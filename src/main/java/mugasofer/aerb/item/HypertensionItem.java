package mugasofer.aerb.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Hypertension - A passive Blood Magic virtue that doubles max HP.
 * Effect applies when in virtue inventory.
 */
public class HypertensionItem extends Item implements VirtueItem {

    public HypertensionItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Enchanted glow to indicate it's special
    }

    @Override
    public boolean isPassive() {
        return true; // Cannot be placed in hotbar or offhand
    }
}
