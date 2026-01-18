package mugasofer.aerb.mixin;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.combat.ParryHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to check for parry when player takes damage.
 * If player is in attack swing and hit from the front, attempt parry roll.
 */
@Mixin(ServerPlayerEntity.class)
public class ParryDamageMixin {

    /**
     * Check for parry conditions when player takes damage.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Check if player is holding a parryable weapon
        if (!ParryHandler.isParryableWeapon(player.getMainHandStack())) {
            return;
        }

        // Check if player is in attack swing
        if (!ParryHandler.isInAttackSwing(player)) {
            return;
        }

        // Check if damage is parryable (skip environmental damage)
        if (!isParryableDamage(source)) {
            return;
        }

        // Check if attack is from the front
        if (!ParryHandler.isFrontalAttack(player, source)) {
            Aerb.LOGGER.debug("Attack not frontal, no parry attempt");
            return;
        }

        // Attempt parry!
        ParryHandler.ParryResult result = ParryHandler.attemptParry(player, source, amount);

        if (result.success()) {
            // Parry succeeded - cancel the damage
            cir.setReturnValue(false);
        }
        // If parry failed, damage proceeds normally
    }

    /**
     * Check if a damage type can be parried.
     * Parryable: melee attacks, projectiles
     * Not parryable: environmental, magic, etc.
     */
    private boolean isParryableDamage(DamageSource source) {
        String damageTypeId = getDamageTypeId(source);

        // Parryable damage types (melee and projectiles)
        if (damageTypeId.contains("player_attack") ||
            damageTypeId.contains("mob_attack") ||
            damageTypeId.contains("arrow") ||
            damageTypeId.contains("trident")) {
            return true;
        }

        // Non-parryable damage types (environmental, magic, etc.)
        if (damageTypeId.contains("fall") ||
            damageTypeId.contains("drown") ||
            damageTypeId.contains("fire") ||
            damageTypeId.contains("lava") ||
            damageTypeId.contains("magic") ||
            damageTypeId.contains("wither") ||
            damageTypeId.contains("starve") ||
            damageTypeId.contains("void") ||
            damageTypeId.contains("generic") ||
            damageTypeId.contains("explosion") ||
            damageTypeId.contains("cactus") ||
            damageTypeId.contains("cramming")) {
            return false;
        }

        // Default to parryable for unknown attack types (mobs, etc.)
        return true;
    }

    /**
     * Get the damage type identifier string from a DamageSource.
     */
    private String getDamageTypeId(DamageSource source) {
        var typeKey = source.getTypeRegistryEntry().getKey();
        if (typeKey.isPresent()) {
            return typeKey.get().getValue().toString();
        }
        return "minecraft:generic";
    }
}
