package mugasofer.aerb.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Prophetic Blade - An active Parry virtue (Parry 40).
 * When in hotbar: always parrying (no swing needed), parry from any direction.
 * On successful parry: auto-switches to best weapon in hotbar.
 */
public class PropheticBladeItem extends Item implements VirtueItem {

    public PropheticBladeItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Enchanted glow to indicate it's special
    }

    @Override
    public boolean isPassive() {
        return false; // Must be in hotbar to work
    }
}
