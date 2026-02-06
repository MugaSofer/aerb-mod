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
    private final int gridWidth;
    private final int gridHeight;

    public TattooDesignItem(Settings settings, String tattooId, int requiredSkinMagicLevel, int gridWidth, int gridHeight) {
        super(settings);
        this.tattooId = tattooId;
        this.requiredSkinMagicLevel = requiredSkinMagicLevel;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
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

    /**
     * Get the width of this tattoo in grid cells (1-4).
     * Each cell corresponds to 4x4 skin pixels.
     */
    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * Get the height of this tattoo in grid cells (1-4).
     * Each cell corresponds to 4x4 skin pixels.
     */
    public int getGridHeight() {
        return gridHeight;
    }
}
