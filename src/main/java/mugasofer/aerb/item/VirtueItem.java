package mugasofer.aerb.item;

import net.minecraft.item.ItemStack;

/**
 * Marker interface for virtue items (blade-bound abilities).
 * Items implementing this interface can only be stored in the virtue inventory,
 * hotbar, or offhand - similar to spells.
 *
 * Passive virtues (isPassive() returns true) can ONLY be stored in the virtue
 * inventory - not hotbar or offhand.
 */
public interface VirtueItem {
    /**
     * Check if an ItemStack is a virtue item.
     */
    static boolean isVirtue(ItemStack stack) {
        return stack.getItem() instanceof VirtueItem;
    }

    /**
     * Check if an ItemStack is a passive virtue (can't go in hotbar/offhand).
     */
    static boolean isPassiveVirtue(ItemStack stack) {
        if (stack.getItem() instanceof VirtueItem virtue) {
            return virtue.isPassive();
        }
        return false;
    }

    /**
     * Whether this virtue is passive (only stored in virtue inventory, not hotbar/offhand).
     * Override to return true for passive virtues.
     */
    default boolean isPassive() {
        return false;
    }
}
