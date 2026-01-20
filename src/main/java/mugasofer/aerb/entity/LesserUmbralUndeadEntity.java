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
import net.minecraft.server.world.ServerWorld;
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

    // Constants
    public static final int MIN_CORPSES = 5;  // Minimum during formation
    public static final int FULL_SIZE_CORPSES = 20;  // "Normal" size reference for scaling
    public static final int MAX_CORPSES = 40;
    // Base dimensions at FULL_SIZE_CORPSES (20)
    public static final float BASE_WIDTH = 2.75f;
    public static final float BASE_HEIGHT = 3.25f;
    public static final float HEALTH_PER_CORPSE = 10.0f;
    public static final float MAX_BREAKABLE_HARDNESS = 6.0f;
    public static final int BLOCK_BREAK_INTERVAL = 10;
    public static final int ABSORB_SEARCH_RADIUS = 4;
    public static final float MOVEMENT_SPEED = 0.2f;
    public static final float DEATH_SURVIVAL_RATE_MIN = 0.3f;
    public static final float DEATH_SURVIVAL_RATE_MAX = 0.5f;

    // Absorption settings
    private static final int ABSORB_INTERVAL = 100; // 5 seconds between looking for new target
    private static final double SUCK_IN_SPEED = 0.15; // How fast absorbed undead fly toward us (slower)
    private static final double ABSORB_DISTANCE = 1.5; // Distance at which they're actually absorbed

    // Instance state
    private int blockBreakCooldown = 0;
    private int absorbCooldown = 0;
    private LivingEntity beingAbsorbed = null; // Entity currently being sucked in

    public LesserUmbralUndeadEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CORPSE_COUNT, MIN_CORPSES);
        builder.add(REARING_UP, false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, MIN_CORPSES * HEALTH_PER_CORPSE)
            .add(EntityAttributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(EntityAttributes.ATTACK_DAMAGE, 8.0)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(EntityAttributes.FOLLOW_RANGE, 40.0)
            .add(EntityAttributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
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
        updateHealthForCorpseCount();
    }

    private void updateHealthForCorpseCount() {
        float newMaxHealth = getCorpseCount() * HEALTH_PER_CORPSE;
        var healthAttr = this.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(newMaxHealth);
            if (this.getHealth() > newMaxHealth) {
                this.setHealth(newMaxHealth);
            }
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        if (damaged && !this.isDead()) {
            // Check if health has dropped enough to lose corpses
            updateCorpseCountFromHealth();
        }
        return damaged;
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
    public float calculateScale() {
        int corpses = getCorpseCount();
        return (float) (Math.cbrt(corpses) / Math.cbrt(FULL_SIZE_CORPSES));
    }

    @Override
    public EntityDimensions getBaseDimensions(EntityPose pose) {
        return EntityDimensions.fixed(BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();

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

        // Check blocks in a column toward the target
        int minY = (int) Math.floor(boundingBox.minY);
        int maxY = (int) Math.ceil(boundingBox.maxY);

        // Check blocks from the edge of our hitbox out to 3 blocks ahead
        for (int y = minY; y <= maxY; y++) {
            for (double dist = boxHalfWidth + 0.5; dist <= boxHalfWidth + 3.0; dist += 0.5) {
                double checkX = this.getX() + dirX * dist;
                double checkZ = this.getZ() + dirZ * dist;

                BlockPos pos = new BlockPos((int) Math.floor(checkX), y, (int) Math.floor(checkZ));
                BlockState state = world.getBlockState(pos);

                if (!state.isAir() && canBreakBlock(world, pos, state)) {
                    world.breakBlock(pos, true, this);
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

    @Override
    public void onDeath(DamageSource source) {
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            spawnSurvivingUndead(serverWorld);
        }
        super.onDeath(source);
    }

    private void spawnSurvivingUndead(ServerWorld world) {
        int corpses = getCorpseCount();
        // 30-50% survival rate
        float survivalRate = DEATH_SURVIVAL_RATE_MIN +
            this.random.nextFloat() * (DEATH_SURVIVAL_RATE_MAX - DEATH_SURVIVAL_RATE_MIN);
        int survivors = (int) (corpses * survivalRate);

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

    // Note: CORPSE_COUNT is tracked via DataTracker which handles persistence automatically
}
