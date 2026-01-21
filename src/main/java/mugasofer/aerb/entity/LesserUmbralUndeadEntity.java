package mugasofer.aerb.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import mugasofer.aerb.sound.ModSounds;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Lesser Umbral Undead - a large quadrupedal undead composed of 20-40 corpses.
 * Size scales with cube root of corpse count.
 * Can smash through walls, absorb nearby undead, and spawns survivors on death.
 */
public class LesserUmbralUndeadEntity extends HostileEntity {
    // Tracked data for client sync
    private static final TrackedData<Integer> CORPSE_COUNT = DataTracker.registerData(
        LesserUmbralUndeadEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> REARING_UP = DataTracker.registerData(
        LesserUmbralUndeadEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_REACHED_FULL_SIZE = DataTracker.registerData(
        LesserUmbralUndeadEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Constants
    public static final int MIN_CORPSES = 5;  // Minimum during formation
    public static final int DEFAULT_CORPSES = 30;  // Default when summoned
    public static final int FULL_SIZE_CORPSES = 20;  // "Normal" size reference for scaling
    public static final int MAX_CORPSES = 255;
    // Base dimensions at FULL_SIZE_CORPSES (20) - slightly smaller hitbox than visual
    public static final float BASE_WIDTH = 2.2f;
    public static final float BASE_HEIGHT = 2.8f;
    public static final float HEALTH_PER_CORPSE = 10.0f;
    public static final float MAX_BREAKABLE_HARDNESS = 6.0f;
    public static final int BLOCK_BREAK_INTERVAL = 10;
    public static final int ABSORB_SEARCH_RADIUS = 4;
    public static final float MOVEMENT_SPEED = 0.2f;

    // Absorption settings
    private static final int ABSORB_INTERVAL = 100; // 5 seconds between looking for new target
    private static final double SUCK_IN_SPEED = 0.15; // How fast absorbed undead fly toward us (slower)
    private static final double ABSORB_DISTANCE = 1.5; // Distance at which they're actually absorbed

    // Damage shedding settings
    private static final float DAMAGE_PER_SHED = 10.0f; // Shed a body every 10 damage
    private static final double SHED_BODY_SPEED = 0.5; // How fast shed bodies fly away
    private static final float SHED_SURVIVAL_CHANCE = 0.05f; // 5% chance shed body survives

    // Turn speed limit (degrees per tick) - large creature turns slowly
    private static final float MAX_TURN_SPEED = 3.0f;

    // Instance state
    private int blockBreakCooldown = 0;
    private int absorbCooldown = 0;
    private LivingEntity beingAbsorbed = null; // Entity currently being sucked in
    private float accumulatedDamage = 0; // Tracks damage for shedding bodies

    public LesserUmbralUndeadEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CORPSE_COUNT, DEFAULT_CORPSES);
        builder.add(REARING_UP, false);
        builder.add(HAS_REACHED_FULL_SIZE, true);  // Summoned entities start at full size
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, DEFAULT_CORPSES * HEALTH_PER_CORPSE)
            .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(EntityAttributes.ATTACK_DAMAGE, 8.0)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(EntityAttributes.FOLLOW_RANGE, 40.0)
            .add(EntityAttributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new UmbralMeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Custom melee attack goal with attack range that scales with hitbox.
     * Allows attacking ~1.5 blocks beyond their hitbox edge.
     */
    private static class UmbralMeleeAttackGoal extends MeleeAttackGoal {
        private final LesserUmbralUndeadEntity umbral;
        private static final double REACH_BEYOND_HITBOX = 1.5;

        public UmbralMeleeAttackGoal(LesserUmbralUndeadEntity mob, double speed, boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
            this.umbral = mob;
        }

        @Override
        protected boolean canAttack(LivingEntity target) {
            // Attack range = half our width + reach + half target width
            // This lets us hit things ~1.5 blocks from our hitbox edge
            double ourHalfWidth = umbral.getWidth() / 2.0;
            double targetHalfWidth = target.getWidth() / 2.0;
            double maxRange = ourHalfWidth + REACH_BEYOND_HITBOX + targetHalfWidth;
            double distSq = umbral.squaredDistanceTo(target);
            return distSq <= maxRange * maxRange && super.canAttack(target);
        }
    }

    @Override
    public boolean collidesWith(Entity other) {
        // Don't collide with regular undead - allows smooth absorption and formation
        if (other instanceof UndeadEntity || other instanceof ZombieEntity) {
            return false;
        }
        return super.collidesWith(other);
    }

    @Override
    protected void pushAway(Entity entity) {
        // Don't get pushed by regular undead - prevents being shoved out of formations
        if (entity instanceof UndeadEntity || entity instanceof ZombieEntity) {
            return;
        }
        super.pushAway(entity);
    }

    // Corpse count management
    public int getCorpseCount() {
        return this.dataTracker.get(CORPSE_COUNT);
    }

    public void setCorpseCount(int count) {
        int clamped = Math.max(MIN_CORPSES, Math.min(MAX_CORPSES, count));
        this.dataTracker.set(CORPSE_COUNT, clamped);
        // Once we reach full size, lock minimum size at 20
        if (clamped >= FULL_SIZE_CORPSES) {
            this.dataTracker.set(HAS_REACHED_FULL_SIZE, true);
        }
        updateHealthForCorpseCount();
    }

    public boolean hasReachedFullSize() {
        return this.dataTracker.get(HAS_REACHED_FULL_SIZE);
    }

    /**
     * Used by formation handler to spawn a small, growing Umbral.
     * Resets the full size flag so it can grow from small.
     */
    public void setCorpseCountForFormation(int count) {
        this.dataTracker.set(HAS_REACHED_FULL_SIZE, false);
        setCorpseCount(count);
    }

    private void updateHealthForCorpseCount() {
        float newMaxHealth = getCorpseCount() * HEALTH_PER_CORPSE;
        var healthAttr = this.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            float oldMax = (float) healthAttr.getBaseValue();
            float currentHealth = this.getHealth();
            healthAttr.setBaseValue(newMaxHealth);
            // Never heal a dead/dying entity
            if (currentHealth <= 0) {
                return;
            }
            // If max health increased, heal to full
            // If max health decreased, cap current health at new max
            if (newMaxHealth > oldMax) {
                this.setHealth(newMaxHealth);
            } else if (currentHealth > newMaxHealth) {
                this.setHealth(newMaxHealth);
            }
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        // Only process shedding if still alive (health > 0 check catches death before isDead flag is set)
        if (damaged && !this.isDead() && this.getHealth() > 0) {
            // Track damage for body shedding
            accumulatedDamage += amount;

            // Shed bodies based on accumulated damage
            while (accumulatedDamage >= DAMAGE_PER_SHED && getCorpseCount() > MIN_CORPSES) {
                accumulatedDamage -= DAMAGE_PER_SHED;
                shedBody(world, source);
            }

            // Update corpse count based on remaining health
            updateCorpseCountFromHealth();
        }
        return damaged;
    }

    private void shedBody(ServerWorld world, DamageSource source) {
        // Spawn an undead that flies away from the damage source
        UndeadEntity undead = ModEntities.UNDEAD.create(world, SpawnReason.MOB_SUMMONED);
        if (undead == null) return;

        // Spawn at our position
        double x = this.getX();
        double y = this.getY() + 1.0;
        double z = this.getZ();
        undead.refreshPositionAndAngles(x, y, z, this.random.nextFloat() * 360, 0);

        // Calculate direction away from damage source
        Vec3d awayDir;
        if (source.getPosition() != null) {
            awayDir = new Vec3d(x, y, z).subtract(source.getPosition()).normalize();
        } else {
            // Random direction if no source position
            double angle = this.random.nextDouble() * Math.PI * 2;
            awayDir = new Vec3d(Math.cos(angle), 0.3, Math.sin(angle)).normalize();
        }

        // Launch the undead away
        undead.setVelocity(
            awayDir.x * SHED_BODY_SPEED,
            0.3 + this.random.nextDouble() * 0.2,
            awayDir.z * SHED_BODY_SPEED
        );

        world.spawnEntity(undead);

        // 95% of the time, the shed body dies immediately (visual effect)
        // 5% chance it survives
        if (this.random.nextFloat() > SHED_SURVIVAL_CHANCE) {
            undead.damage(world, this.getDamageSources().generic(), 1000f);
        }

        // Crunchy bone/meat tearing sound
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
            ModSounds.UMBRAL_CRUNCH, SoundCategory.HOSTILE,
            1.5f, 0.8f + this.random.nextFloat() * 0.3f);

        // Reduce corpse count
        setCorpseCount(getCorpseCount() - 1);
    }

    private void updateCorpseCountFromHealth() {
        float currentHealth = this.getHealth();
        // Calculate what corpse count our current health supports
        int healthBasedCorpses = (int) Math.ceil(currentHealth / HEALTH_PER_CORPSE);
        healthBasedCorpses = Math.max(MIN_CORPSES, healthBasedCorpses);

        // If we have more corpses than our health supports, shed some
        if (getCorpseCount() > healthBasedCorpses) {
            setCorpseCount(healthBasedCorpses);
        }
    }

    // Rearing animation state
    public boolean isRearingUp() {
        return this.dataTracker.get(REARING_UP);
    }

    public void setRearingUp(boolean rearing) {
        this.dataTracker.set(REARING_UP, rearing);
    }

    // Scale based on corpse count using cube root formula
    // At 5 corpses: ~0.63x, at 20 corpses: 1.0x, at 40 corpses: ~1.26x
    // Once full size (20) is reached, minimum scale is locked at 1.0x
    public float calculateScale() {
        int corpses = getCorpseCount();
        // If we've ever reached full size, don't shrink below that
        if (hasReachedFullSize() && corpses < FULL_SIZE_CORPSES) {
            corpses = FULL_SIZE_CORPSES;
        }
        return (float) (Math.cbrt(corpses) / Math.cbrt(FULL_SIZE_CORPSES));
    }

    @Override
    public EntityDimensions getBaseDimensions(EntityPose pose) {
        // Scale hitbox with corpse count
        float scale = calculateScale();
        return EntityDimensions.fixed(BASE_WIDTH * scale, BASE_HEIGHT * scale);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        // Recalculate bounding box when corpse count or size lock changes
        if (CORPSE_COUNT.equals(data) || HAS_REACHED_FULL_SIZE.equals(data)) {
            this.calculateDimensions();
        }
    }



    @Override
    public void tick() {
        // Capture yaw before super.tick() changes it
        float prevBodyYaw = this.bodyYaw;
        float prevHeadYaw = this.headYaw;

        super.tick();

        // Limit turn speed - large creature turns slowly
        float bodyYawDelta = this.bodyYaw - prevBodyYaw;
        // Normalize to -180 to 180
        while (bodyYawDelta > 180) bodyYawDelta -= 360;
        while (bodyYawDelta < -180) bodyYawDelta += 360;
        if (Math.abs(bodyYawDelta) > MAX_TURN_SPEED) {
            this.bodyYaw = prevBodyYaw + Math.signum(bodyYawDelta) * MAX_TURN_SPEED;
        }

        float headYawDelta = this.headYaw - prevHeadYaw;
        while (headYawDelta > 180) headYawDelta -= 360;
        while (headYawDelta < -180) headYawDelta += 360;
        if (Math.abs(headYawDelta) > MAX_TURN_SPEED) {
            this.headYaw = prevHeadYaw + Math.signum(headYawDelta) * MAX_TURN_SPEED;
        }

        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            // Block breaking while chasing
            if (blockBreakCooldown > 0) {
                blockBreakCooldown--;
            } else if (this.getTarget() != null) {
                // Break blocks toward target if we have one
                tryBreakBlocksTowardTarget(serverWorld);
                blockBreakCooldown = BLOCK_BREAK_INTERVAL;
            }

            // Handle entity being sucked in
            if (beingAbsorbed != null) {
                if (beingAbsorbed.isRemoved() || beingAbsorbed.isDead()) {
                    beingAbsorbed = null;
                } else {
                    // Pull it toward us
                    double dx = this.getX() - beingAbsorbed.getX();
                    double dy = (this.getY() + 1.0) - beingAbsorbed.getY();
                    double dz = this.getZ() - beingAbsorbed.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (dist <= ABSORB_DISTANCE) {
                        // Close enough - absorb it
                        float healAmount = beingAbsorbed.getMaxHealth() * 0.5f;
                        this.heal(healAmount);
                        setCorpseCount(getCorpseCount() + 1);
                        beingAbsorbed.discard();
                        beingAbsorbed = null;

                        // Crunchy absorption sound
                        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.UMBRAL_CRUNCH, SoundCategory.HOSTILE,
                            1.5f, 0.6f + this.random.nextFloat() * 0.2f);
                    } else {
                        // Pull it in
                        double speed = SUCK_IN_SPEED;
                        beingAbsorbed.setVelocity(
                            dx / dist * speed,
                            dy / dist * speed,
                            dz / dist * speed
                        );
                    }
                }
            }
            // Look for new absorption target
            else if (absorbCooldown > 0) {
                absorbCooldown--;
            } else {
                tryAbsorbNearbyUndead(serverWorld);
                absorbCooldown = ABSORB_INTERVAL;
            }

            // Check if should rear up for elevated target
            updateRearingState();
        }
    }

    private void tryBreakBlocksTowardTarget(ServerWorld world) {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        // Calculate direction toward target
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.1) {
            return; // Already at target
        }

        // Normalize direction
        double dirX = dx / distance;
        double dirZ = dz / distance;

        Box boundingBox = this.getBoundingBox();
        double boxHalfWidth = (boundingBox.maxX - boundingBox.minX) / 2;

        int feetY = (int) Math.floor(this.getY());
        int headY = (int) Math.ceil(boundingBox.maxY);
        double stepHeight = this.getStepHeight();

        // Check blocks from the edge of our hitbox out to 2 blocks ahead
        for (double dist = boxHalfWidth + 0.3; dist <= boxHalfWidth + 2.0; dist += 0.5) {
            double checkX = this.getX() + dirX * dist;
            double checkZ = this.getZ() + dirZ * dist;
            int blockX = (int) Math.floor(checkX);
            int blockZ = (int) Math.floor(checkZ);

            // Check if this is a wall (blocks body) vs terrain (can step over)
            // Count solid blocks in the column from feet to head
            int solidCount = 0;
            int lowestSolid = headY + 1;
            for (int y = feetY; y <= headY; y++) {
                BlockPos pos = new BlockPos(blockX, y, blockZ);
                BlockState state = world.getBlockState(pos);
                if (!state.isAir() && canBreakBlock(world, pos, state)) {
                    solidCount++;
                    if (y < lowestSolid) lowestSolid = y;
                }
            }

            // If lowest solid is within step height and there's only 1-2 blocks, skip (terrain)
            // Otherwise it's a wall - break it
            boolean isTerrain = (lowestSolid <= feetY + stepHeight) && solidCount <= 2;

            if (!isTerrain && solidCount > 0) {
                // This is a wall - break blocks from feet to head
                for (int y = feetY; y <= headY; y++) {
                    BlockPos pos = new BlockPos(blockX, y, blockZ);
                    BlockState state = world.getBlockState(pos);

                    if (!state.isAir() && canBreakBlock(world, pos, state)) {
                        // Spawn extra block particles for dramatic effect
                        world.spawnParticles(
                            new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            15, 0.3, 0.3, 0.3, 0.05
                        );

                        // Crashing/smashing sound
                        world.playSound(null, pos,
                            ModSounds.UMBRAL_SMASH, SoundCategory.HOSTILE,
                            1.5f, 0.8f + this.random.nextFloat() * 0.2f);

                        world.breakBlock(pos, true, this);
                    }
                }
            }
        }
    }

    private boolean canBreakBlock(ServerWorld world, BlockPos pos, BlockState state) {
        float hardness = state.getHardness(world, pos);
        // Negative hardness means unbreakable (bedrock, etc.)
        return hardness >= 0 && hardness <= MAX_BREAKABLE_HARDNESS;
    }

    private void tryAbsorbNearbyUndead(ServerWorld world) {
        if (getCorpseCount() >= MAX_CORPSES) {
            return;
        }

        // Already absorbing something
        if (beingAbsorbed != null) {
            return;
        }

        Box searchBox = this.getBoundingBox().expand(ABSORB_SEARCH_RADIUS);
        List<LivingEntity> nearbyUndead = world.getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != this && !entity.isRemoved() && isAbsorbableUndead(entity)
        );

        // Find closest undead to absorb
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity undead : nearbyUndead) {
            double dist = this.squaredDistanceTo(undead);
            if (dist < closestDist) {
                closest = undead;
                closestDist = dist;
            }
        }

        // Start sucking it in
        if (closest != null) {
            beingAbsorbed = closest;
        }
    }

    private boolean isAbsorbableUndead(LivingEntity entity) {
        // Absorb our custom Undead entities
        if (entity instanceof UndeadEntity) {
            return true;
        }
        // Absorb vanilla zombie types
        if (entity instanceof ZombieEntity) {
            return true;
        }
        return false;
    }

    private void updateRearingState() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            double heightDiff = target.getY() - this.getY();
            // Rear up if target is 2+ blocks above
            setRearingUp(heightDiff >= 2.0);
        } else {
            setRearingUp(false);
        }
    }

    private void spawnSurvivingUndead(ServerWorld world) {
        // Spawn 5-15 undead survivors, but never more than corpse count
        int corpses = getCorpseCount();
        int minSurvivors = Math.min(5, corpses);
        int maxSurvivors = Math.min(15, corpses);
        int survivors = minSurvivors + this.random.nextInt(maxSurvivors - minSurvivors + 1);

        // Spawn in a wider area around the death location to avoid crowding
        double centerX = this.getX();
        double centerZ = this.getZ();
        double spawnRadius = 3.0;

        for (int i = 0; i < survivors; i++) {
            // Random position in circle around death location
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = this.random.nextDouble() * spawnRadius;
            double x = centerX + Math.cos(angle) * dist;
            double z = centerZ + Math.sin(angle) * dist;
            // Spawn slightly above ground to avoid suffocation
            double y = this.getY() + 0.5;

            UndeadEntity undead = ModEntities.UNDEAD.create(world, SpawnReason.MOB_SUMMONED);
            if (undead != null) {
                undead.refreshPositionAndAngles(x, y, z, this.random.nextFloat() * 360, 0);
                world.spawnEntity(undead);
            }
        }
    }


    // === Sound overrides for meaty/crunchy sounds ===

    @Override
    protected SoundEvent getAmbientSound() {
        // Crunchy bone/meat sounds for ambient
        return ModSounds.UMBRAL_CRUNCH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        // Crunchy bone/meat sounds for hurt
        return ModSounds.UMBRAL_CRUNCH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        // Return null - we handle death sounds manually in onDeath
        return null;
    }

    @Override
    public void onDeath(DamageSource source) {
        // Play multiple layered crunches as the body falls apart
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            // Play 3-5 crunches with slight pitch/timing variation
            int crunches = 3 + this.random.nextInt(3);
            for (int i = 0; i < crunches; i++) {
                float pitch = 0.7f + this.random.nextFloat() * 0.4f;
                float volume = 1.2f + this.random.nextFloat() * 0.3f;
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.UMBRAL_CRUNCH, SoundCategory.HOSTILE,
                    volume, pitch);
            }
            spawnSurvivingUndead(serverWorld);
        }
        super.onDeath(source);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Crunchy bone/meat sounds for steps
        this.playSound(ModSounds.UMBRAL_CRUNCH, 1.0f, 0.8f + this.random.nextFloat() * 0.2f);
    }
}
