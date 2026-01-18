package mugasofer.aerb.mixin;

import mugasofer.aerb.config.XpConfig;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.skill.XpHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to award XP when a player kills an entity with a weapon.
 * Awards One-Handed XP for kills with swords/axes.
 */
@Mixin(LivingEntity.class)
public class KillXpMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onEntityDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;

        // Only award XP for killing hostile entities
        if (!(victim instanceof HostileEntity)) {
            return;
        }

        // Check if the attacker is a player
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {
            ItemStack weapon = player.getMainHandStack();

            // Award One-Handed XP for kills with swords/axes
            if (XpHelper.isOneHandedWeapon(weapon)) {
                XpHelper.awardXp(player, PlayerSkills.ONE_HANDED, XpConfig.get().xpPerKill);
            }
        }
    }
}
