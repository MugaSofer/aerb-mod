package mugasofer.aerb.screen;

import mugasofer.aerb.item.SpellItem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * A slot that only accepts spell items.
 */
public class SpellSlot extends Slot {

    public SpellSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return SpellItem.isSpell(stack);
    }
}
