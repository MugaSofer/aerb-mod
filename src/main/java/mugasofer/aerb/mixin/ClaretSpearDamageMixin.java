package mugasofer.aerb.mixin;

import mugasofer.aerb.item.ClaretSpearItem;
import mugasofer.aerb.skill.PlayerSkills;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to apply Blood Magic-scaled damage when hitting with Claret Spear in melee.
 */
@Mixin(ServerPlayerEntity.class)
public class ClaretSpearDamageMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onClaretSpearAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack weapon = player.getMainHandStack();

        // Only handle Claret Spear attacks
        if (!(weapon.getItem() instanceof ClaretSpearItem)) {
            return;
        }

        // Only damage living entities
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        // Get Blood Magic level and calculate damage
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        int bloodMagicLevel = Math.max(0, skills.getSkillLevel(PlayerSkills.BLOOD_MAGIC));
        float damage = ClaretSpearItem.getDamage(bloodMagicLevel);

        // Create damage source and apply damage
        DamageSource damageSource = player.getDamageSources().playerAttack(player);
        if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
            livingTarget.damage(serverWorld, damageSource, damage);
        }

        // Swing the hand for visual feedback
        player.swingHand(player.getActiveHand(), true);

        // Cancel the default attack to prevent double damage
        ci.cancel();
    }
}
