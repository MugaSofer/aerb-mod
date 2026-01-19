package mugasofer.aerb.item;

import net.minecraft.item.ItemStack;

/**
 * Marker interface for items that have descriptions loaded from the config file.
 * The client uses this to determine which items should show configurable tooltips.
 */
public interface DescribedItem {
    /**
     * Check if an ItemStack has config-based descriptions.
     */
    static boolean hasDescription(ItemStack stack) {
        return stack.getItem() instanceof DescribedItem;
    }
}
