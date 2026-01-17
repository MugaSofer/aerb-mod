package mugasofer.aerb.mixin;

import mugasofer.aerb.command.ModCommands;
import mugasofer.aerb.item.SpellItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to detect when spell items are added to player inventory
 * and trigger discovery messages.
 */
@Mixin(PlayerInventory.class)
public class SpellDiscoveryMixin {

    @Shadow @Final public PlayerEntity player;

    /**
     * Check for spell discovery when items are inserted into inventory.
     */
    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void onInsertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        checkSpellDiscovery(stack);
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void onInsertStackWithSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        checkSpellDiscovery(stack);
    }

    private void checkSpellDiscovery(ItemStack stack) {
        if (SpellItem.isSpell(stack) && player instanceof ServerPlayerEntity serverPlayer) {
            String spellId = Registries.ITEM.getId(stack.getItem()).toString();
            // Get a readable name from the item
            String spellName = stack.getName().getString();
            ModCommands.sendDiscoveryMessage(serverPlayer, spellId, spellName);
        }
    }
}
