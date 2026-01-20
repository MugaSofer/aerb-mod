package mugasofer.aerb.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Undead from the Risen Lands - zombie variant with glowing red eyes.
 * Takes 4x damage when hit in the heart (chest area).
 */
public class UndeadEntity extends ZombieEntity {
    // Heart is roughly in the chest area - 40% to 70% up the body
    private static final double HEART_MIN_HEIGHT = 0.4;
    private static final double HEART_MAX_HEIGHT = 0.7;
    private static final float HEART_DAMAGE_MULTIPLIER = 4.0f;

    public UndeadEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Check if this is a hit to the heart area
        if (isHeartHit(source)) {
            amount *= HEART_DAMAGE_MULTIPLIER;
        }

        return super.damage(world, source, amount);
    }

    /**
     * Determines if a damage source hit the heart area (chest).
     */
    private boolean isHeartHit(DamageSource source) {
        Vec3d hitPos = source.getPosition();

        // For melee attacks, getPosition() returns the attacker's position, not the hit location
        // We need to ray-trace from the attacker's eyes to find where they actually hit
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity livingAttacker && hitPos != null) {
            // Check if this looks like a melee attack (attacker position is far from this entity's body)
            double distToAttacker = hitPos.distanceTo(new Vec3d(this.getX(), this.getY(), this.getZ()));
            if (distToAttacker > 1.0) {
                // This is likely a melee attack - ray trace from attacker's eyes
                hitPos = calculateMeleeHitPosition(livingAttacker);
            }
        }

        if (hitPos == null) {
            return false;
        }

        // Calculate the relative height of the hit on this entity
        double entityBottom = this.getY();
        double entityHeight = this.getHeight();
        double hitHeight = hitPos.y;

        // Calculate where on the body the hit landed (0 = feet, 1 = head)
        double relativeHitHeight = (hitHeight - entityBottom) / entityHeight;

        // Check if hit is in the heart zone
        return relativeHitHeight >= HEART_MIN_HEIGHT && relativeHitHeight <= HEART_MAX_HEIGHT;
    }

    /**
     * Calculate where a melee attack from the given attacker would hit this entity
     * by ray-tracing from their eye position along their look direction.
     */
    private Vec3d calculateMeleeHitPosition(LivingEntity attacker) {
        Vec3d eyePos = attacker.getEyePos();
        Vec3d lookVec = attacker.getRotationVec(1.0f);

        // Extend the look vector to cover melee range (about 4-5 blocks)
        Vec3d rayEnd = eyePos.add(lookVec.multiply(5.0));

        // Get this entity's bounding box
        Box boundingBox = this.getBoundingBox();

        // Find where the ray intersects the bounding box
        Optional<Vec3d> intersection = boundingBox.raycast(eyePos, rayEnd);

        return intersection.orElse(null);
    }

    @Override
    public boolean collidesWith(Entity other) {
        // Don't collide with Lesser Umbral Undead - allows smooth formation and absorption
        if (other instanceof LesserUmbralUndeadEntity) {
            return false;
        }
        return super.collidesWith(other);
    }

    @Override
    protected void pushAway(Entity entity) {
        // Don't push Lesser Umbral Undead - allows smooth formation and absorption
        if (entity instanceof LesserUmbralUndeadEntity) {
            return;
        }
        super.pushAway(entity);
    }

    @Override
    public boolean isBaby() {
        return false; // Undead don't have baby variants
    }

    @Override
    public boolean canPickUpLoot() {
        return false; // Undead don't pick up items from the ground
    }

    @Override
    protected void initEquipment(net.minecraft.util.math.random.Random random, net.minecraft.world.LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);
        // Clear any weapons that zombie spawning logic may have added (just clear, don't drop during init)
        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        // Continuously ensure undead aren't holding anything (handles command spawning, etc.)
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            dropHandItems(serverWorld);
        }
    }

    private void dropHandItems(ServerWorld world) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = this.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                this.dropStack(world, stack);
                this.equipStack(slot, ItemStack.EMPTY);
            }
        }
    }
}
