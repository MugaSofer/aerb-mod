package mugasofer.aerb.item;

import net.minecraft.item.ItemStack;

/**
 * Marker interface for spell items.
 * Items implementing this interface can only be stored in the spell inventory,
 * not in regular inventory, chests, or dropped. They CAN be placed in crafting grids.
 */
public interface SpellItem extends DescribedItem {

    /**
     * Check if an ItemStack contains a spell item.
     */
    static boolean isSpell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpellItem;
    }
}
