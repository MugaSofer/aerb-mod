package mugasofer.aerb.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Riposter virtue (One-Handed Weapons 40).
 * Active virtue - must be in hotbar to function.
 * When you successfully parry with a one-handed weapon (sword or axe),
 * you immediately counterattack without cooldown penalty.
 */
public class RiposterItem extends Item implements VirtueItem {

    public RiposterItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean isPassive() {
        return false; // Active virtue - must be in hotbar
    }
}
