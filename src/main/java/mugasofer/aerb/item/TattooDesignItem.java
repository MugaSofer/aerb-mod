package mugasofer.aerb.item;

import net.minecraft.item.Item;

/**
 * A reusable tattoo design item.
 * Players need this item in their inventory to apply the corresponding tattoo.
 * Not consumed on use - designs are permanent once obtained.
 */
public class TattooDesignItem extends Item implements DescribedItem {
    private final String tattooId;
    private final int requiredSkinMagicLevel;

    public TattooDesignItem(Settings settings, String tattooId, int requiredSkinMagicLevel) {
        super(settings);
        this.tattooId = tattooId;
        this.requiredSkinMagicLevel = requiredSkinMagicLevel;
    }

    /**
     * Get the tattoo ID this design represents.
     */
    public String getTattooId() {
        return tattooId;
    }

    /**
     * Get the minimum Skin Magic level required to apply this tattoo.
     */
    public int getRequiredSkinMagicLevel() {
        return requiredSkinMagicLevel;
    }
}
