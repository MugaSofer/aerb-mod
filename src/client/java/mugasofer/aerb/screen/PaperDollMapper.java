package mugasofer.aerb.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps between paper doll visual coordinates and skin UV coordinates.
 *
 * The Minecraft skin is a 64x64 texture with body parts laid out in a specific arrangement.
 * This class provides the translation between:
 * - Visual paper doll layout (intuitive body arrangement for UI)
 * - UV coordinates on the actual skin texture
 * - Grid coordinates (16x16 grid where each cell = 4x4 pixels)
 *
 * Skin UV Layout Reference (64x64):
 *
 * HEAD (8x8 face):
 *   Front (face): UV 8,8
 *   Back: UV 24,8
 *   Right: UV 0,8
 *   Left: UV 16,8
 *
 * BODY (8x12):
 *   Front (chest): UV 20,20
 *   Back: UV 32,20
 *   Right side: UV 16,20 (4x12)
 *   Left side: UV 28,20 (4x12)
 *
 * RIGHT ARM (4x12):
 *   Front: UV 44,20
 *   Back: UV 52,20
 *   Outer: UV 40,20
 *   Inner: UV 48,20
 *
 * LEFT ARM (4x12):
 *   Front: UV 36,52
 *   Back: UV 44,52
 *   Inner: UV 32,52
 *   Outer: UV 40,52
 *
 * RIGHT LEG (4x12):
 *   Front: UV 4,20
 *   Back: UV 12,20
 *   Outer: UV 0,20
 *   Inner: UV 8,20
 *
 * LEFT LEG (4x12):
 *   Front: UV 20,52
 *   Back: UV 28,52
 *   Inner: UV 16,52
 *   Outer: UV 24,52
 */
public class PaperDollMapper {

    // Grid cell size in pixels
    public static final int CELL_SIZE = 4;

    // Total grid dimensions (64 / 4 = 16)
    public static final int GRID_SIZE = 16;

    /**
     * A face of a body part with its UV position and size.
     */
    public record BodyFace(
        String partName,      // e.g., "head", "body", "left_arm"
        String faceName,      // e.g., "front", "back", "outer"
        int uvX, int uvY,     // Top-left UV pixel coordinate
        int uvWidth, int uvHeight  // Size in pixels
    ) {
        /** Get grid X coordinate (top-left) */
        public int gridX() { return uvX / CELL_SIZE; }

        /** Get grid Y coordinate (top-left) */
        public int gridY() { return uvY / CELL_SIZE; }

        /** Get width in grid cells */
        public int gridWidth() { return uvWidth / CELL_SIZE; }

        /** Get height in grid cells */
        public int gridHeight() { return uvHeight / CELL_SIZE; }

        /** Check if a UV pixel position is within this face */
        public boolean containsUV(int x, int y) {
            return x >= uvX && x < uvX + uvWidth && y >= uvY && y < uvY + uvHeight;
        }

        /** Check if a grid position is within this face */
        public boolean containsGrid(int gx, int gy) {
            return gx >= gridX() && gx < gridX() + gridWidth()
                && gy >= gridY() && gy < gridY() + gridHeight();
        }
    }

    /**
     * Visual region on the paper doll display.
     * Maps a visual rectangle to a body face.
     */
    public record VisualRegion(
        String name,          // Display name
        int visualX, int visualY,  // Position on paper doll (in pixels, relative to doll origin)
        int visualWidth, int visualHeight,  // Visual size
        BodyFace face         // The body face this region represents
    ) {}

    // All body faces indexed by part name
    private static final Map<String, List<BodyFace>> BODY_PARTS = new HashMap<>();

    // All body faces in a flat list
    private static final List<BodyFace> ALL_FACES = new ArrayList<>();

    // Visual regions for paper doll display
    private static final List<VisualRegion> VISUAL_REGIONS = new ArrayList<>();

    // Connection map for wrapping (which faces connect to which)
    private static final Map<String, List<String>> FACE_CONNECTIONS = new HashMap<>();

    static {
        initBodyParts();
        initVisualRegions();
        initConnections();
    }

    private static void initBodyParts() {
        // HEAD
        addFace("head", "front", 8, 8, 8, 8);      // Face
        addFace("head", "back", 24, 8, 8, 8);
        addFace("head", "right", 0, 8, 8, 8);
        addFace("head", "left", 16, 8, 8, 8);
        addFace("head", "top", 8, 0, 8, 8);
        addFace("head", "bottom", 16, 0, 8, 8);

        // BODY
        addFace("body", "front", 20, 20, 8, 12);   // Chest
        addFace("body", "back", 32, 20, 8, 12);
        addFace("body", "right", 16, 20, 4, 12);
        addFace("body", "left", 28, 20, 4, 12);

        // RIGHT ARM
        addFace("right_arm", "front", 44, 20, 4, 12);
        addFace("right_arm", "back", 52, 20, 4, 12);
        addFace("right_arm", "outer", 40, 20, 4, 12);
        addFace("right_arm", "inner", 48, 20, 4, 12);

        // LEFT ARM
        addFace("left_arm", "front", 36, 52, 4, 12);
        addFace("left_arm", "back", 44, 52, 4, 12);
        addFace("left_arm", "inner", 32, 52, 4, 12);
        addFace("left_arm", "outer", 40, 52, 4, 12);

        // RIGHT LEG
        addFace("right_leg", "front", 4, 20, 4, 12);
        addFace("right_leg", "back", 12, 20, 4, 12);
        addFace("right_leg", "outer", 0, 20, 4, 12);
        addFace("right_leg", "inner", 8, 20, 4, 12);

        // LEFT LEG
        addFace("left_leg", "front", 20, 52, 4, 12);
        addFace("left_leg", "back", 28, 52, 4, 12);
        addFace("left_leg", "inner", 16, 52, 4, 12);
        addFace("left_leg", "outer", 24, 52, 4, 12);
    }

    private static void addFace(String part, String face, int uvX, int uvY, int w, int h) {
        BodyFace bf = new BodyFace(part, face, uvX, uvY, w, h);
        ALL_FACES.add(bf);
        BODY_PARTS.computeIfAbsent(part, k -> new ArrayList<>()).add(bf);
    }

    /**
     * Initialize visual regions for paper doll display.
     * This defines how body parts are arranged on the UI.
     *
     * Layout (approximately 80x150 pixels):
     *          [HEAD]           <- centered at top
     *     [LA] [BODY] [RA]      <- arms beside body
     *          [LL][RL]         <- legs below body
     */
    private static void initVisualRegions() {
        // Scale factor from UV to visual (makes it easier to see)
        int scale = 3;

        // Center X for the body column
        int centerX = 40;

        // HEAD - show front face, centered at top
        BodyFace headFront = getFace("head", "front");
        int headW = headFront.uvWidth() * scale;
        int headH = headFront.uvHeight() * scale;
        VISUAL_REGIONS.add(new VisualRegion("Face", centerX - headW/2, 5, headW, headH, headFront));

        // BODY - show front face below head
        BodyFace bodyFront = getFace("body", "front");
        int bodyW = bodyFront.uvWidth() * scale;
        int bodyH = bodyFront.uvHeight() * scale;
        int bodyY = 5 + headH + 2;
        VISUAL_REGIONS.add(new VisualRegion("Chest", centerX - bodyW/2, bodyY, bodyW, bodyH, bodyFront));

        // BODY BACK - show back face (offset slightly or toggled)
        BodyFace bodyBack = getFace("body", "back");
        // For now, don't show back in main view - could be a toggle

        // LEFT ARM - to the left of body
        BodyFace leftArmOuter = getFace("left_arm", "outer");
        int armW = leftArmOuter.uvWidth() * scale;
        int armH = leftArmOuter.uvHeight() * scale;
        int armY = bodyY;
        VISUAL_REGIONS.add(new VisualRegion("Left Arm", centerX - bodyW/2 - armW - 2, armY, armW, armH, leftArmOuter));

        // RIGHT ARM - to the right of body
        BodyFace rightArmOuter = getFace("right_arm", "outer");
        VISUAL_REGIONS.add(new VisualRegion("Right Arm", centerX + bodyW/2 + 2, armY, armW, armH, rightArmOuter));

        // LEFT LEG - below body, left side
        BodyFace leftLegFront = getFace("left_leg", "front");
        int legW = leftLegFront.uvWidth() * scale;
        int legH = leftLegFront.uvHeight() * scale;
        int legY = bodyY + bodyH + 2;
        VISUAL_REGIONS.add(new VisualRegion("Left Leg", centerX - legW, legY, legW, legH, leftLegFront));

        // RIGHT LEG - below body, right side
        BodyFace rightLegFront = getFace("right_leg", "front");
        VISUAL_REGIONS.add(new VisualRegion("Right Leg", centerX, legY, legW, legH, rightLegFront));
    }

    /**
     * Initialize connections between faces for wrapping.
     * When a tattoo extends past the edge of one face, it can continue on a connected face.
     */
    private static void initConnections() {
        // Head face connections (wraps around)
        connect("head:front", "head:right", "head:left");
        connect("head:right", "head:front", "head:back");
        connect("head:back", "head:right", "head:left");
        connect("head:left", "head:front", "head:back");

        // Body connections
        connect("body:front", "body:right", "body:left");
        connect("body:right", "body:front", "body:back", "right_arm:inner");
        connect("body:left", "body:front", "body:back", "left_arm:inner");
        connect("body:back", "body:right", "body:left");

        // Arm connections (wrap around arm, connect to body)
        connect("right_arm:outer", "right_arm:front", "right_arm:back");
        connect("right_arm:front", "right_arm:outer", "right_arm:inner");
        connect("right_arm:inner", "right_arm:front", "right_arm:back", "body:right");
        connect("right_arm:back", "right_arm:outer", "right_arm:inner");

        connect("left_arm:outer", "left_arm:front", "left_arm:back");
        connect("left_arm:front", "left_arm:outer", "left_arm:inner");
        connect("left_arm:inner", "left_arm:front", "left_arm:back", "body:left");
        connect("left_arm:back", "left_arm:outer", "left_arm:inner");

        // Leg connections
        connect("right_leg:front", "right_leg:outer", "right_leg:inner");
        connect("right_leg:outer", "right_leg:front", "right_leg:back");
        connect("right_leg:inner", "right_leg:front", "right_leg:back");
        connect("right_leg:back", "right_leg:outer", "right_leg:inner");

        connect("left_leg:front", "left_leg:outer", "left_leg:inner");
        connect("left_leg:outer", "left_leg:front", "left_leg:back");
        connect("left_leg:inner", "left_leg:front", "left_leg:back");
        connect("left_leg:back", "left_leg:outer", "left_leg:inner");
    }

    private static void connect(String face, String... connections) {
        FACE_CONNECTIONS.put(face, List.of(connections));
    }

    // ========== Public API ==========

    /**
     * Get a specific body face.
     */
    public static BodyFace getFace(String partName, String faceName) {
        List<BodyFace> faces = BODY_PARTS.get(partName);
        if (faces != null) {
            for (BodyFace f : faces) {
                if (f.faceName().equals(faceName)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Get all faces for a body part.
     */
    public static List<BodyFace> getFaces(String partName) {
        return BODY_PARTS.getOrDefault(partName, List.of());
    }

    /**
     * Get all body faces.
     */
    public static List<BodyFace> getAllFaces() {
        return ALL_FACES;
    }

    /**
     * Get the visual regions for paper doll display.
     */
    public static List<VisualRegion> getVisualRegions() {
        return VISUAL_REGIONS;
    }

    /**
     * Find which body face contains a given grid position.
     * Returns null if the position is in an unused area of the skin texture.
     */
    public static BodyFace getFaceAtGrid(int gridX, int gridY) {
        for (BodyFace face : ALL_FACES) {
            if (face.containsGrid(gridX, gridY)) {
                return face;
            }
        }
        return null;
    }

    /**
     * Find which body face contains a given UV pixel position.
     */
    public static BodyFace getFaceAtUV(int uvX, int uvY) {
        for (BodyFace face : ALL_FACES) {
            if (face.containsUV(uvX, uvY)) {
                return face;
            }
        }
        return null;
    }

    /**
     * Convert a visual region click to grid coordinates.
     *
     * @param region The visual region that was clicked
     * @param localX X position within the visual region (0 to region width)
     * @param localY Y position within the visual region (0 to region height)
     * @return Grid coordinates [x, y] on the skin texture
     */
    public static int[] visualToGrid(VisualRegion region, int localX, int localY) {
        BodyFace face = region.face();

        // Convert local position to UV position within the face
        float ratioX = (float) localX / region.visualWidth();
        float ratioY = (float) localY / region.visualHeight();

        int uvX = face.uvX() + (int)(ratioX * face.uvWidth());
        int uvY = face.uvY() + (int)(ratioY * face.uvHeight());

        // Convert UV to grid
        return new int[] { uvX / CELL_SIZE, uvY / CELL_SIZE };
    }

    /**
     * Get the visual region and local position for a grid coordinate.
     * Used for displaying existing tattoos on the paper doll.
     *
     * @param gridX Grid X coordinate
     * @param gridY Grid Y coordinate
     * @return Array of [visualRegionIndex, localX, localY] or null if not displayable
     */
    public static int[] gridToVisual(int gridX, int gridY) {
        // Find which face contains this grid position
        BodyFace face = getFaceAtGrid(gridX, gridY);
        if (face == null) return null;

        // Find the visual region for this face
        for (int i = 0; i < VISUAL_REGIONS.size(); i++) {
            VisualRegion region = VISUAL_REGIONS.get(i);
            if (region.face().equals(face)) {
                // Calculate local position within the visual region
                int localGridX = gridX - face.gridX();
                int localGridY = gridY - face.gridY();

                float ratioX = (float) localGridX / face.gridWidth();
                float ratioY = (float) localGridY / face.gridHeight();

                int localX = (int)(ratioX * region.visualWidth());
                int localY = (int)(ratioY * region.visualHeight());

                return new int[] { i, localX, localY };
            }
        }

        return null;
    }

    /**
     * Get connected faces for wrapping.
     *
     * @param partName Body part name (e.g., "head")
     * @param faceName Face name (e.g., "front")
     * @return List of connected face keys (e.g., ["head:right", "head:left"])
     */
    public static List<String> getConnectedFaces(String partName, String faceName) {
        String key = partName + ":" + faceName;
        return FACE_CONNECTIONS.getOrDefault(key, List.of());
    }

    /**
     * Check if a grid position is on a valid body part (not empty space on the texture).
     */
    public static boolean isValidPosition(int gridX, int gridY) {
        return getFaceAtGrid(gridX, gridY) != null;
    }

    /**
     * Get the display name for a grid position.
     */
    public static String getPositionName(int gridX, int gridY) {
        BodyFace face = getFaceAtGrid(gridX, gridY);
        if (face == null) return "Unknown";

        String partDisplay = switch(face.partName()) {
            case "head" -> "Head";
            case "body" -> "Body";
            case "left_arm" -> "Left Arm";
            case "right_arm" -> "Right Arm";
            case "left_leg" -> "Left Leg";
            case "right_leg" -> "Right Leg";
            default -> face.partName();
        };

        String faceDisplay = switch(face.faceName()) {
            case "front" -> face.partName().equals("head") ? "" : " (front)";
            case "back" -> " (back)";
            case "outer" -> " (outer)";
            case "inner" -> " (inner)";
            case "left" -> " (left)";
            case "right" -> " (right)";
            default -> "";
        };

        return partDisplay + faceDisplay;
    }
}
