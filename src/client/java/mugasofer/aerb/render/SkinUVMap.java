package mugasofer.aerb.render;

import mugasofer.aerb.tattoo.BodyPosition;

/**
 * Maps BodyPosition and grid coordinates to UV coordinates on the 64x64 Minecraft skin texture.
 * Each body part has a base layer region and an outer layer region.
 *
 * Minecraft skin UV layout reference (64x64):
 * - Base layer contains the actual skin
 * - Outer layer (offset) contains overlay elements (hat, jacket, sleeves, pants)
 *
 * Grid system: 16x16 grid where each cell = 4x4 skin pixels.
 * Grid coordinates (0-15) map to different body parts based on which UV region they fall into.
 */
public class SkinUVMap {

    // Grid cell size in pixels (64 / 16 = 4)
    public static final int GRID_CELL_SIZE = 4;

    /**
     * UV region on the skin texture.
     * @param x Top-left X coordinate
     * @param y Top-left Y coordinate
     * @param width Region width
     * @param height Region height
     */
    public record UVRegion(int x, int y, int width, int height) {
        public boolean contains(int px, int py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    /**
     * Pair of UV regions for base and outer layer.
     */
    public record LayeredRegion(UVRegion base, UVRegion outer) {}

    /**
     * Convert grid coordinates to UV pixel coordinates.
     * Each grid cell is 4x4 pixels.
     *
     * @param gridX Grid X (0-15)
     * @param gridY Grid Y (0-15)
     * @return UV pixel coordinates [x, y]
     */
    public static int[] gridToUV(int gridX, int gridY) {
        return new int[] {
            gridX * GRID_CELL_SIZE,
            gridY * GRID_CELL_SIZE
        };
    }

    /**
     * Convert UV pixel coordinates to grid coordinates.
     *
     * @param uvX UV pixel X (0-63)
     * @param uvY UV pixel Y (0-63)
     * @return Grid coordinates [x, y]
     */
    public static int[] uvToGrid(int uvX, int uvY) {
        return new int[] {
            uvX / GRID_CELL_SIZE,
            uvY / GRID_CELL_SIZE
        };
    }

    /**
     * Get UV region for placing a tattoo at grid position with given size.
     *
     * @param gridX Grid X position (0-15)
     * @param gridY Grid Y position (0-15)
     * @param gridWidth Tattoo width in grid cells
     * @param gridHeight Tattoo height in grid cells
     * @return UV region for base layer placement
     */
    public static UVRegion getRegionForGrid(int gridX, int gridY, int gridWidth, int gridHeight) {
        int[] uv = gridToUV(gridX, gridY);
        return new UVRegion(
            uv[0],
            uv[1],
            gridWidth * GRID_CELL_SIZE,
            gridHeight * GRID_CELL_SIZE
        );
    }

    /**
     * Get the UV regions for a body position.
     * Returns regions for both base and outer layer where the tattoo should be placed.
     */
    public static LayeredRegion getRegion(BodyPosition position) {
        return switch (position) {
            // Head/Face - front of head
            case FACE -> new LayeredRegion(
                new UVRegion(8, 8, 8, 8),    // Base: head front
                new UVRegion(40, 8, 8, 8)    // Outer: hat front
            );

            // Neck - bottom of head (not really visible, but included)
            case NECK -> new LayeredRegion(
                new UVRegion(8, 8, 8, 2),    // Base: top of head front (approximate)
                new UVRegion(40, 8, 8, 2)    // Outer: top of hat front
            );

            // Torso front (chest)
            case CHEST -> new LayeredRegion(
                new UVRegion(20, 20, 8, 12), // Base: body front
                new UVRegion(20, 36, 8, 12)  // Outer: jacket front
            );

            // Torso back
            case BACK -> new LayeredRegion(
                new UVRegion(32, 20, 8, 12), // Base: body back
                new UVRegion(32, 36, 8, 12)  // Outer: jacket back
            );

            // Shoulders (top of arms near body)
            case LEFT_SHOULDER -> new LayeredRegion(
                new UVRegion(36, 52, 4, 4),  // Base: left arm top area
                new UVRegion(52, 52, 4, 4)   // Outer: left sleeve top
            );
            case RIGHT_SHOULDER -> new LayeredRegion(
                new UVRegion(44, 20, 4, 4),  // Base: right arm top area
                new UVRegion(44, 36, 4, 4)   // Outer: right sleeve top
            );

            // Upper arms (top half of arm length)
            case LEFT_UPPER_ARM -> new LayeredRegion(
                new UVRegion(36, 52, 4, 6),  // Base: left arm outer face, upper
                new UVRegion(52, 52, 4, 6)   // Outer: left sleeve outer, upper
            );
            case RIGHT_UPPER_ARM -> new LayeredRegion(
                new UVRegion(44, 20, 4, 6),  // Base: right arm outer face, upper
                new UVRegion(44, 36, 4, 6)   // Outer: right sleeve outer, upper
            );

            // Forearms (bottom half of arm length) - typically exposed
            case LEFT_FOREARM -> new LayeredRegion(
                new UVRegion(36, 58, 4, 6),  // Base: left arm outer face, lower
                new UVRegion(52, 58, 4, 6)   // Outer: left sleeve outer, lower
            );
            case RIGHT_FOREARM -> new LayeredRegion(
                new UVRegion(44, 26, 4, 6),  // Base: right arm outer face, lower
                new UVRegion(44, 42, 4, 6)   // Outer: right sleeve outer, lower
            );

            // Thighs (upper leg)
            case LEFT_THIGH -> new LayeredRegion(
                new UVRegion(20, 52, 4, 6),  // Base: left leg front, upper
                new UVRegion(4, 52, 4, 6)    // Outer: left pants front, upper
            );
            case RIGHT_THIGH -> new LayeredRegion(
                new UVRegion(4, 20, 4, 6),   // Base: right leg front, upper
                new UVRegion(4, 36, 4, 6)    // Outer: right pants front, upper
            );

            // Calves (lower leg)
            case LEFT_CALF -> new LayeredRegion(
                new UVRegion(20, 58, 4, 6),  // Base: left leg front, lower
                new UVRegion(4, 58, 4, 6)    // Outer: left pants front, lower
            );
            case RIGHT_CALF -> new LayeredRegion(
                new UVRegion(4, 26, 4, 6),   // Base: right leg front, lower
                new UVRegion(4, 42, 4, 6)    // Outer: right pants front, lower
            );

            // ANY - use a default visible area (forearm)
            case ANY -> new LayeredRegion(
                new UVRegion(44, 26, 4, 6),  // Default to right forearm
                new UVRegion(44, 42, 4, 6)
            );
        };
    }

    /**
     * Get just the base layer region for a position.
     */
    public static UVRegion getBaseRegion(BodyPosition position) {
        return getRegion(position).base();
    }

    /**
     * Get just the outer layer region for a position.
     */
    public static UVRegion getOuterRegion(BodyPosition position) {
        return getRegion(position).outer();
    }
}
