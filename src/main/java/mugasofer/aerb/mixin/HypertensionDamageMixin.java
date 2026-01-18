package mugasofer.aerb.mixin;

import mugasofer.aerb.config.HypertensionConfig;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.virtue.VirtueInventory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to increase damage taken when player has Hypertension virtue.
 * Extra blood means more blood to lose!
 */
@Mixin(ServerPlayerEntity.class)
public class HypertensionDamageMixin {

    /**
     * Modify the damage amount based on Hypertension and damage type.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void modifyDamageForHypertension(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (!hasHypertension(player)) {
            return; // No modification needed
        }

        // Only apply extra damage when above 50% HP (you have the "extra blood" to lose)
        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent <= 0.5f) {
            return; // Below half health, no extra damage
        }

        // Get the damage type identifier
        String damageTypeId = aerb$getHypertensionDamageTypeId(source);

        // Get multiplier from config
        double multiplier = HypertensionConfig.get().getMultiplier(damageTypeId);

        if (multiplier != 1.0) {
            // Cancel this call and re-call with modified damage
            // We need to call the super implementation with modified damage
            float modifiedDamage = (float) (amount * multiplier);

            // Temporarily disable this mixin to avoid recursion
            // Use a flag to track if we're already processing
            if (isProcessingDamage.get()) {
                return;
            }

            isProcessingDamage.set(true);
            try {
                boolean result = player.damage(world, source, modifiedDamage);
                cir.setReturnValue(result);
            } finally {
                isProcessingDamage.set(false);
            }
        }
    }

    // Flag to prevent recursion when calling damage with modified amount
    private static final ThreadLocal<Boolean> isProcessingDamage = ThreadLocal.withInitial(() -> false);

    /**
     * Check if player has Hypertension in virtue inventory.
     * (Hypertension is passive, so it can only be in virtue inventory)
     */
    private boolean hasHypertension(ServerPlayerEntity player) {
        // Check virtue inventory only (Hypertension is passive)
        VirtueInventory virtueInv = player.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        for (int i = 0; i < virtueInv.size(); i++) {
            if (virtueInv.getStack(i).isOf(ModItems.HYPERTENSION)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the damage type identifier string from a DamageSource.
     */
    private String aerb$getHypertensionDamageTypeId(DamageSource source) {
        // The damage type is stored in the registry key
        var typeKey = source.getTypeRegistryEntry().getKey();
        if (typeKey.isPresent()) {
            return typeKey.get().getValue().toString();
        }
        return "minecraft:generic";
    }
}
