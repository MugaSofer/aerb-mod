package mugasofer.aerb.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Prescient Blade - A passive Parry virtue (Parry 20).
 * Halves the penalty for parrying projectiles (arrows, bolts, etc.).
 * Effect applies when in virtue inventory.
 */
public class PrescientBladeItem extends Item implements VirtueItem {

    public PrescientBladeItem(Settings settings) {
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
