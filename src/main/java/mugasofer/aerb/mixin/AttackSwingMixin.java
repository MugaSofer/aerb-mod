package mugasofer.aerb.mixin;

import mugasofer.aerb.combat.ParryHandler;
import mugasofer.aerb.config.XpConfig;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.skill.XpHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect when a player swings their weapon.
 * Records the swing for the parry system (works for hits AND misses).
 */
@Mixin(ServerPlayerEntity.class)
public class AttackSwingMixin {

    /**
     * Called when the player swings their arm (main hand or off hand).
     * This fires for ALL swings, not just when hitting entities.
     */
    @Inject(method = "swingHand", at = @At("HEAD"))
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        if (hand != Hand.MAIN_HAND) {
            return; // Only track main hand swings for parry
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Record the swing with the main hand weapon
        ParryHandler.recordAttack(player, player.getMainHandStack());
    }

    /**
     * Also track when player attacks an entity (backup for parry, and XP award).
     */
    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack weapon = player.getMainHandStack();

        // Record the attack with the main hand weapon (for parry system)
        ParryHandler.recordAttack(player, weapon);

        // Award One-Handed XP if attacking a living entity with sword/axe
        if (target instanceof LivingEntity && XpHelper.isOneHandedWeapon(weapon)) {
            XpHelper.awardXp(player, PlayerSkills.ONE_HANDED, XpConfig.get().xpPerDamageDealt);
        }
    }
}
