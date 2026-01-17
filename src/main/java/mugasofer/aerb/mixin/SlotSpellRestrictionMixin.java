package mugasofer.aerb.mixin;

import mugasofer.aerb.item.SpellItem;
import mugasofer.aerb.item.VirtueItem;
import mugasofer.aerb.spell.SpellInventory;
import mugasofer.aerb.virtue.VirtueInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent spell items from being placed in non-spell slots.
 * Also prevents passive virtues from being placed in hotbar/offhand.
 * Exception: Crafting slots are allowed for spells.
 */
@Mixin(Slot.class)
public class SlotSpellRestrictionMixin {

    @Shadow @Final public Inventory inventory;
    @Shadow @Final private int index;

    /**
     * Prevent inserting spell items into non-spell, non-crafting, non-hotbar slots.
     * Prevent inserting passive virtues into hotbar/offhand.
     */
    @Inject(method = "canInsert(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void restrictSpellInsertion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Handle passive virtues - only allow in VirtueInventory
        if (VirtueItem.isPassiveVirtue(stack)) {
            if (inventory instanceof VirtueInventory) {
                return; // Allow in virtue inventory
            }
            // Block everywhere else (including hotbar and offhand)
            cir.setReturnValue(false);
            return;
        }

        if (SpellItem.isSpell(stack)) {
            // Allow in SpellInventory
            if (inventory instanceof SpellInventory) {
                return; // Let normal logic proceed
            }

            // Allow in crafting grids
            if (inventory instanceof RecipeInputInventory || inventory instanceof CraftingResultInventory) {
                return; // Let normal logic proceed
            }

            // Allow in player hotbar (slots 0-8) and offhand (slot 40)
            if (inventory instanceof PlayerInventory) {
                if (index >= 0 && index <= 8) {
                    return; // Hotbar slots
                }
                if (index == 40) {
                    return; // Offhand slot
                }
            }

            // Block everywhere else (main player inventory, chests, etc.)
            cir.setReturnValue(false);
        }
    }
}
