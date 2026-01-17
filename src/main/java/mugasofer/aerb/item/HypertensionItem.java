package mugasofer.aerb.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Hypertension - A passive Blood Magic virtue that doubles max HP.
 * Effect applies when in virtue inventory, hotbar, or offhand.
 */
public class HypertensionItem extends Item implements VirtueItem {

    public HypertensionItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Enchanted glow to indicate it's special
    }
}
