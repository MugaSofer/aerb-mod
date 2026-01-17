package mugasofer.aerb.item;

import net.minecraft.item.ItemStack;

/**
 * Marker interface for virtue items (blade-bound abilities).
 * Items implementing this interface can only be stored in the virtue inventory,
 * hotbar, or offhand - similar to spells.
 */
public interface VirtueItem {
    /**
     * Check if an ItemStack is a virtue item.
     */
    static boolean isVirtue(ItemStack stack) {
        return stack.getItem() instanceof VirtueItem;
    }
}
