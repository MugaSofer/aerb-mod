package mugasofer.aerb.screen;

import mugasofer.aerb.item.VirtueItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * A slot that only accepts virtue items.
 */
public class VirtueSlot extends Slot {

    public VirtueSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return VirtueItem.isVirtue(stack);
    }
}
