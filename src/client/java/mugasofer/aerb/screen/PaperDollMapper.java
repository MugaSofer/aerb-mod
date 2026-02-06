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
        addFace("head", "bottom", 16, 0, 8, 8);    // Full bottom (for compatibility)
        // Split head bottom: front half (chin) and back half (connects to neck)
        addFace("head", "bottom_front", 16, 0, 8, 4);  // Chin - front half
        addFace("head", "bottom_back", 16, 4, 8, 4);   // Back of jaw - connects to body top

        // BODY
        addFace("body", "front", 20, 20, 8, 12);   // Chest
        addFace("body", "back", 32, 20, 8, 12);
        addFace("body", "right", 16, 20, 4, 12);
        addFace("body", "left", 28, 20, 4, 12);
        addFace("body", "top", 20, 16, 8, 4);      // Neck/collar area

        // RIGHT ARM
        addFace("right_arm", "front", 44, 20, 4, 12);
        addFace("right_arm", "back", 52, 20, 4, 12);
        addFace("right_arm", "outer", 40, 20, 4, 12);
        addFace("right_arm", "inner", 48, 20, 4, 12);
        addFace("right_arm", "top", 44, 16, 4, 4);    // Shoulder
        addFace("right_arm", "bottom", 48, 16, 4, 4); // Hand

        // LEFT ARM
        addFace("left_arm", "front", 36, 52, 4, 12);
        addFace("left_arm", "back", 44, 52, 4, 12);
        addFace("left_arm", "inner", 32, 52, 4, 12);
        addFace("left_arm", "outer", 40, 52, 4, 12);
        addFace("left_arm", "top", 36, 48, 4, 4);     // Shoulder
        addFace("left_arm", "bottom", 40, 48, 4, 4);  // Hand

        // RIGHT LEG
        addFace("right_leg", "front", 4, 20, 4, 12);
        addFace("right_leg", "back", 12, 20, 4, 12);
        addFace("right_leg", "outer", 0, 20, 4, 12);
        addFace("right_leg", "inner", 8, 20, 4, 12);
        addFace("right_leg", "bottom", 8, 16, 4, 4);  // Sole

        // LEFT LEG
        addFace("left_leg", "front", 20, 52, 4, 12);
        addFace("left_leg", "back", 28, 52, 4, 12);
        addFace("left_leg", "inner", 16, 52, 4, 12);
        addFace("left_leg", "outer", 24, 52, 4, 12);
        addFace("left_leg", "bottom", 24, 48, 4, 4);  // Sole
    }

    private static void addFace(String part, String face, int uvX, int uvY, int w, int h) {
        BodyFace bf = new BodyFace(part, face, uvX, uvY, w, h);
        ALL_FACES.add(bf);
        BODY_PARTS.computeIfAbsent(part, k -> new ArrayList<>()).add(bf);
    }

    /**
     * Initialize visual regions for paper doll display.
     * Layout: Center paper doll at 3x scale, surrounding detail clusters at 2x scale.
     *
     *                    [HEAD cluster]
     *         [NECK]     [  FRONT   ]     [TORSO]
     *     [RIGHT ARM]    [  VIEW    ]     [LEFT ARM]
     *     [RIGHT LEG]    [  3x      ]     [LEFT LEG]
     *                    [LEG BACKS ]
     */
    // Doll area dimensions (should match TattooApplicationScreen)
    private static final int DOLL_AREA_HEIGHT = 180;

    private static void initVisualRegions() {
        int mainScale = 3;
        int sideScale = 2;
        int gap = 2;

        // Center position for main paper doll
        int centerX = 110;

        // ========== CENTER PAPER DOLL (3x scale) ==========
        // Calculate total height for vertical centering
        int headH3 = 8 * mainScale;  // 24
        int bodyH3 = 12 * mainScale; // 36
        int legH3 = 12 * mainScale;  // 36
        int totalPaperDollHeight = headH3 + gap + bodyH3 + gap + legH3; // 100

        // Center the paper doll vertically in the doll area
        int headY = (DOLL_AREA_HEIGHT - totalPaperDollHeight) / 2;

        // Head front
        int headW3 = 8 * mainScale;  // 24
        int headX = centerX - headW3 / 2;
        VISUAL_REGIONS.add(new VisualRegion("Face", headX, headY, headW3, headH3, getFace("head", "front")));

        // Body front
        int bodyW3 = 8 * mainScale;  // 24
        int bodyX = centerX - bodyW3 / 2;
        int bodyY = headY + headH3 + gap;
        VISUAL_REGIONS.add(new VisualRegion("Chest", bodyX, bodyY, bodyW3, bodyH3, getFace("body", "front")));

        // Arms (3x) - flanking body
        int armW3 = 4 * mainScale;  // 12
        int armH3 = 12 * mainScale; // 36
        VISUAL_REGIONS.add(new VisualRegion("R.Arm", bodyX - armW3 - 1, bodyY, armW3, armH3, getFace("right_arm", "front")));
        VISUAL_REGIONS.add(new VisualRegion("L.Arm", bodyX + bodyW3 + 1, bodyY, armW3, armH3, getFace("left_arm", "front")));

        // Legs (3x) - below body
        int legW3 = 4 * mainScale;  // 12
        int legY = bodyY + bodyH3 + gap;
        VISUAL_REGIONS.add(new VisualRegion("R.Leg", centerX - legW3, legY, legW3, legH3, getFace("right_leg", "front")));
        VISUAL_REGIONS.add(new VisualRegion("L.Leg", centerX, legY, legW3, legH3, getFace("left_leg", "front")));

        // ========== HEAD CLUSTER (top center, 2x) ==========
        int headW2 = 8 * sideScale;  // 16
        int headH2 = 8 * sideScale;  // 16
        int headHalfH2 = 4 * sideScale; // 8 (for split bottom)
        int headClusterX = centerX - (headW2 * 3 + gap * 2) / 2; // Center 3 heads
        int headClusterY = headY - headH2 - gap - 4; // Above the paper doll head

        // Row: back, left, right, top, chin (front half of bottom)
        VISUAL_REGIONS.add(new VisualRegion("Head Back", headClusterX, headClusterY, headW2, headH2, getFace("head", "back")));
        VISUAL_REGIONS.add(new VisualRegion("Head Left", headClusterX + headW2 + gap, headClusterY, headW2, headH2, getFace("head", "left")));
        VISUAL_REGIONS.add(new VisualRegion("Head Right", headClusterX + (headW2 + gap) * 2, headClusterY, headW2, headH2, getFace("head", "right")));
        VISUAL_REGIONS.add(new VisualRegion("Head Top", headClusterX + (headW2 + gap) * 3, headClusterY, headW2, headH2, getFace("head", "top")));
        VISUAL_REGIONS.add(new VisualRegion("Chin", headClusterX + (headW2 + gap) * 4, headClusterY, headW2, headHalfH2, getFace("head", "bottom_front")));

        // ========== NECK CLUSTER (left of center head, 2x) ==========
        // Back half of head bottom + body top (conceptually the "neck" connection)
        int bodyTopW2 = 8 * sideScale;  // 16
        int bodyTopH2 = 4 * sideScale;  // 8
        int neckX = headX - bodyTopW2 - 20;
        int neckY = headY;
        // Back of jaw (back half of head bottom) - connects to body top
        VISUAL_REGIONS.add(new VisualRegion("Jaw Back", neckX, neckY, headW2, headHalfH2, getFace("head", "bottom_back")));
        // Body top below it
        VISUAL_REGIONS.add(new VisualRegion("Neck Top", neckX, neckY + headHalfH2 + gap, bodyTopW2, bodyTopH2, getFace("body", "top")));

        // ========== TORSO CLUSTER (right of center, 2x) ==========
        int bodyW2 = 8 * sideScale;   // 16
        int bodyH2 = 12 * sideScale;  // 24
        int bodySideW2 = 4 * sideScale; // 8
        int torsoX = bodyX + bodyW3 + armW3 + 15;
        int torsoY = headY;  // Align with head/neck level

        // Back and sides
        VISUAL_REGIONS.add(new VisualRegion("Back", torsoX, torsoY, bodyW2, bodyH2, getFace("body", "back")));
        VISUAL_REGIONS.add(new VisualRegion("Body L", torsoX + bodyW2 + gap, torsoY, bodySideW2, bodyH2, getFace("body", "left")));
        VISUAL_REGIONS.add(new VisualRegion("Body R", torsoX + bodyW2 + gap + bodySideW2 + gap, torsoY, bodySideW2, bodyH2, getFace("body", "right")));

        // ========== ARM CLUSTERS (both vertically centered) ==========
        int armW2 = 4 * sideScale;   // 8
        int armH2 = 12 * sideScale;  // 24
        int armTopH2 = 4 * sideScale; // 8
        int armClusterY = (DOLL_AREA_HEIGHT - armH2) / 2;  // Vertically centered

        // RIGHT ARM CLUSTER (left side, 2x)
        int rArmX = 5;

        // Outer, inner, back (3 tall faces)
        VISUAL_REGIONS.add(new VisualRegion("R.Arm Out", rArmX, armClusterY, armW2, armH2, getFace("right_arm", "outer")));
        VISUAL_REGIONS.add(new VisualRegion("R.Arm In", rArmX + armW2 + gap, armClusterY, armW2, armH2, getFace("right_arm", "inner")));
        VISUAL_REGIONS.add(new VisualRegion("R.Arm Bk", rArmX + (armW2 + gap) * 2, armClusterY, armW2, armH2, getFace("right_arm", "back")));

        // Shoulder and hand (stacked)
        int rArmSmallX = rArmX + (armW2 + gap) * 3 + gap;
        VISUAL_REGIONS.add(new VisualRegion("R.Shoulder", rArmSmallX, armClusterY, armW2, armTopH2, getFace("right_arm", "top")));
        VISUAL_REGIONS.add(new VisualRegion("R.Hand", rArmSmallX, armClusterY + armTopH2 + gap, armW2, armTopH2, getFace("right_arm", "bottom")));

        // LEFT ARM CLUSTER (right side, 2x)
        int lArmX = torsoX;

        // Outer, inner, back
        VISUAL_REGIONS.add(new VisualRegion("L.Arm Out", lArmX, armClusterY, armW2, armH2, getFace("left_arm", "outer")));
        VISUAL_REGIONS.add(new VisualRegion("L.Arm In", lArmX + armW2 + gap, armClusterY, armW2, armH2, getFace("left_arm", "inner")));
        VISUAL_REGIONS.add(new VisualRegion("L.Arm Bk", lArmX + (armW2 + gap) * 2, armClusterY, armW2, armH2, getFace("left_arm", "back")));

        // Shoulder and hand
        int lArmSmallX = lArmX + (armW2 + gap) * 3 + gap;
        VISUAL_REGIONS.add(new VisualRegion("L.Shoulder", lArmSmallX, armClusterY, armW2, armTopH2, getFace("left_arm", "top")));
        VISUAL_REGIONS.add(new VisualRegion("L.Hand", lArmSmallX, armClusterY + armTopH2 + gap, armW2, armTopH2, getFace("left_arm", "bottom")));

        // ========== LEG CLUSTERS (top aligned with bottom of paper doll legs) ==========
        int legW2 = 4 * sideScale;   // 8
        int legH2 = 12 * sideScale;  // 24
        int legBottomH2 = 4 * sideScale; // 8
        int legClusterY = legY + legH3;  // Top of clusters at bottom of paper doll legs
        int lLegX = torsoX;

        // ========== RIGHT LEG CLUSTER (bottom left, 2x) ==========
        int rLegX = 5;

        // Sole (small square first)
        VISUAL_REGIONS.add(new VisualRegion("R.Sole", rLegX, legClusterY, legW2, legBottomH2, getFace("right_leg", "bottom")));

        // Outer, inner, back
        int rLegFacesX = rLegX + legW2 + gap + 4;
        VISUAL_REGIONS.add(new VisualRegion("R.Leg Out", rLegFacesX, legClusterY, legW2, legH2, getFace("right_leg", "outer")));
        VISUAL_REGIONS.add(new VisualRegion("R.Leg In", rLegFacesX + legW2 + gap, legClusterY, legW2, legH2, getFace("right_leg", "inner")));
        VISUAL_REGIONS.add(new VisualRegion("R.Leg Bk", rLegFacesX + (legW2 + gap) * 2, legClusterY, legW2, legH2, getFace("right_leg", "back")));

        // ========== LEFT LEG CLUSTER (bottom right, 2x) ==========
        // Outer, inner, back
        VISUAL_REGIONS.add(new VisualRegion("L.Leg Out", lLegX, legClusterY, legW2, legH2, getFace("left_leg", "outer")));
        VISUAL_REGIONS.add(new VisualRegion("L.Leg In", lLegX + legW2 + gap, legClusterY, legW2, legH2, getFace("left_leg", "inner")));
        VISUAL_REGIONS.add(new VisualRegion("L.Leg Bk", lLegX + (legW2 + gap) * 2, legClusterY, legW2, legH2, getFace("left_leg", "back")));

        // Sole
        int lLegSmallX = lLegX + (legW2 + gap) * 3 + gap;
        VISUAL_REGIONS.add(new VisualRegion("L.Sole", lLegSmallX, legClusterY, legW2, legBottomH2, getFace("left_leg", "bottom")));
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
