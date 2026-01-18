package mugasofer.aerb.mixin;

import mugasofer.aerb.config.XpConfig;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.skill.XpHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect when a player equips a new item in their main hand.
 * Awards XP for relevant items that haven't been equipped before.
 */
@Mixin(ServerPlayerEntity.class)
public class ItemEquipMixin {

    @Unique
    private ItemStack lastMainHandStack = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ItemStack currentMainHand = player.getMainHandStack();

        // Check if main hand item changed
        if (!ItemStack.areItemsEqual(currentMainHand, lastMainHandStack)) {
            lastMainHandStack = currentMainHand.copy();

            // Skip empty hands
            if (currentMainHand.isEmpty()) {
                return;
            }

            // Get the relevant skill for this item
            String relevantSkill = XpHelper.getRelevantSkill(currentMainHand);
            if (relevantSkill == null) {
                return;
            }

            // Get the item identifier for tracking
            String itemId = XpHelper.getItemIdentifier(currentMainHand);
            if (itemId == null) {
                return;
            }

            // Check if this is a new item (not discovered before)
            PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
            if (!skills.hasDiscoveredItem(itemId)) {
                // Mark as discovered
                skills.discoverItem(itemId);

                // Award XP if the skill is unlocked
                XpHelper.awardXp(player, relevantSkill, XpConfig.get().xpPerNewItemEquip);
            }
        }
    }
}
