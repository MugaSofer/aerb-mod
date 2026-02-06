package mugasofer.aerb.screen;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.item.TattooDesignItem;
import mugasofer.aerb.item.TattooInkItem;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.ClientSkillCache;
import mugasofer.aerb.tattoo.ClientTattooCache;
import mugasofer.aerb.tattoo.TattooInstance;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.Click;

import java.util.*;

/**
 * Paper doll UI for applying tattoos.
 * Shows body regions with a grid system for tattoo placement.
 * Click a region, then click a design, then click Apply.
 */
public class TattooApplicationScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 220;

    // Paper doll area (shows front, back, and side views)
    private static final int DOLL_AREA_WIDTH = 220;
    private static final int DOLL_AREA_HEIGHT = 180;

    private int panelX;
    private int panelY;
    private int dollX;
    private int dollY;

    // Player skin texture
    private Identifier skinTexture;

    // Currently selected region and design
    private int selectedRegionIndex = -1;
    private TattooDesignItem selectedDesign = null;
    private int selectedDesignSlot = -1;

    // Hover tracking
    private int hoveredRegionIndex = -1;

    // Available designs from player's inventory
    private final List<DesignEntry> availableDesigns = new ArrayList<>();

    private record DesignEntry(TattooDesignItem item, int slot, ItemStack stack) {}

    // Buttons (region selection is handled via mouseClicked, not buttons)
    private ButtonWidget applyButton;
    private final List<ButtonWidget> designButtons = new ArrayList<>();

    // Text widgets
    private TextWidget positionLabel;
    private TextWidget positionValue;
    private TextWidget designsLabel;
    private TextWidget inkStatus;
    private TextWidget instructionText;

    public TattooApplicationScreen() {
        super(Text.literal("Tattoo Application"));
    }

    @Override
    protected void init() {
        super.init();

        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        dollX = panelX + 10;
        dollY = panelY + 30;

        // Load player skin texture
        loadPlayerSkin();

        // Scan inventory for available designs
        scanForDesigns();

        // Body regions are rendered manually and clicked via mouseClicked override
        // (no buttons for regions - they would draw on top of our skin textures)

        // Create clickable buttons for designs (right side)
        designButtons.clear();
        int infoX = panelX + DOLL_AREA_WIDTH + 20;
        int infoY = panelY + 65;
        for (int i = 0; i < availableDesigns.size() && i < 5; i++) {
            final int index = i;
            DesignEntry entry = availableDesigns.get(index);
            String name = formatTattooName(entry.item.getTattooId());

            ButtonWidget btn = ButtonWidget.builder(Text.literal(name), button -> selectDesign(index))
                .dimensions(infoX, infoY + i * 22, 110, 20)
                .build();

            // Disable if player doesn't meet level requirement
            int required = entry.item.getRequiredSkinMagicLevel();
            if (ClientSkillCache.getSkinMagic() < required) {
                btn.active = false;
            }

            designButtons.add(btn);
            addDrawableChild(btn);
        }

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> applyTattoo())
            .dimensions(panelX + PANEL_WIDTH - 70, panelY + PANEL_HEIGHT - 28, 60, 20)
            .build();
        applyButton.active = false;
        addDrawableChild(applyButton);

        // Text widgets
        int textY = panelY + 30;

        positionLabel = new TextWidget(infoX, textY, 100, 10, Text.literal("Position:"), textRenderer);
        addDrawableChild(positionLabel);

        positionValue = new TextWidget(infoX, textY + 10, 100, 10, Text.literal("None"), textRenderer);
        addDrawableChild(positionValue);

        textY += 28;
        designsLabel = new TextWidget(infoX, textY, 100, 10, Text.literal("Designs:"), textRenderer);
        addDrawableChild(designsLabel);

        inkStatus = new TextWidget(infoX, panelY + PANEL_HEIGHT - 55, 100, 10, Text.literal("Ink: ?"), textRenderer);
        addDrawableChild(inkStatus);

        instructionText = new TextWidget(infoX, panelY + PANEL_HEIGHT - 42, 120, 10,
            Text.literal("Select region & design"), textRenderer);
        addDrawableChild(instructionText);
    }

    private void scanForDesigns() {
        availableDesigns.clear();
        if (client == null || client.player == null) return;

        var inventory = client.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof TattooDesignItem design) {
                availableDesigns.add(new DesignEntry(design, i, stack));
            }
        }
    }

    private void loadPlayerSkin() {
        if (client == null || client.player == null) return;

        // Get skin texture from player list entry
        // Note: This returns the tattooed skin if TattooTextureManager has modified it
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (entry != null && entry.getSkinTextures().body() != null) {
            skinTexture = entry.getSkinTextures().body().texturePath();
        } else {
            // Fallback to Steve
            skinTexture = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Calculate hovered region
        updateHoveredRegion(mouseX, mouseY);

        // Dark overlay background
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Panel background
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC222222);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 8, 0xFFFFFF);

        // Paper doll area background
        context.fill(dollX, dollY, dollX + DOLL_AREA_WIDTH, dollY + DOLL_AREA_HEIGHT, 0xFF333333);
        drawBorder(context, dollX, dollY, DOLL_AREA_WIDTH, DOLL_AREA_HEIGHT, 0xFF555555);

        // Draw region backgrounds with skin texture
        renderRegionBackgrounds(context);

        // Render tattoo preview on hovered region
        renderTattooPreview(context);

        // TODO: Render existing tattoos indicators (disabled for now - they show on the actual skin anyway)
        // renderExistingTattoos(context);

        // Update text widgets
        updateTextWidgets();

        // Render all widgets
        super.render(context, mouseX, mouseY, delta);
    }

    private void updateHoveredRegion(int mouseX, int mouseY) {
        hoveredRegionIndex = -1;

        // Convert to doll area coordinates
        int localX = mouseX - dollX;
        int localY = mouseY - dollY;

        // Check if within doll area
        if (localX >= 0 && localX < DOLL_AREA_WIDTH && localY >= 0 && localY < DOLL_AREA_HEIGHT) {
            List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
            for (int i = 0; i < regions.size(); i++) {
                PaperDollMapper.VisualRegion region = regions.get(i);
                int rx = region.visualX();
                int ry = region.visualY();
                int rw = region.visualWidth();
                int rh = region.visualHeight();

                if (localX >= rx && localX < rx + rw && localY >= ry && localY < ry + rh) {
                    hoveredRegionIndex = i;
                    break;
                }
            }
        }
    }

    private void renderTattooPreview(DrawContext context) {
        // Only show preview if a design is selected and hovering over a region
        if (selectedDesign == null || hoveredRegionIndex < 0) {
            return;
        }

        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
        if (hoveredRegionIndex >= regions.size()) {
            return;
        }

        PaperDollMapper.VisualRegion region = regions.get(hoveredRegionIndex);
        PaperDollMapper.BodyFace face = region.face();

        // Get tattoo texture
        Identifier tattooTexture = Identifier.of(Aerb.MOD_ID, "textures/entity/tattoo_" + selectedDesign.getTattooId() + ".png");

        // Calculate tattoo size in pixels (at the region's scale)
        int tattooGridW = selectedDesign.getGridWidth();
        int tattooGridH = selectedDesign.getGridHeight();

        // Scale factor: region visual size / face UV size
        float scaleX = (float) region.visualWidth() / face.uvWidth();
        float scaleY = (float) region.visualHeight() / face.uvHeight();

        // Tattoo size on screen
        int tattooW = (int) (tattooGridW * PaperDollMapper.CELL_SIZE * scaleX);
        int tattooH = (int) (tattooGridH * PaperDollMapper.CELL_SIZE * scaleY);

        // Center the tattoo in the region
        int rx = dollX + region.visualX();
        int ry = dollY + region.visualY();
        int tattooX = rx + (region.visualWidth() - tattooW) / 2;
        int tattooY = ry + (region.visualHeight() - tattooH) / 2;

        // Tattoo texture size in pixels
        int texW = tattooGridW * PaperDollMapper.CELL_SIZE;
        int texH = tattooGridH * PaperDollMapper.CELL_SIZE;

        // Draw tattoo texture preview
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            tattooTexture,
            tattooX, tattooY,      // Screen position
            0, 0,                   // UV start
            tattooW, tattooH,       // Size on screen
            texW, texH,             // Region size in texture
            texW, texH              // Full texture dimensions
        );

        // Draw border around preview
        drawBorder(context, tattooX, tattooY, tattooW, tattooH, 0xFFAA00AA);
    }

    private void renderRegionBackgrounds(DrawContext context) {
        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();

        for (int i = 0; i < regions.size(); i++) {
            PaperDollMapper.VisualRegion region = regions.get(i);
            PaperDollMapper.BodyFace face = region.face();
            int rx = dollX + region.visualX();
            int ry = dollY + region.visualY();
            int rw = region.visualWidth();
            int rh = region.visualHeight();

            // Draw skin texture if available, otherwise fallback to colored background
            if (skinTexture != null) {
                // Draw the skin texture portion for this face
                // 12-param version: drawTexture(pipeline, texture, x, y, u, v, screenWidth, screenHeight, regionWidth, regionHeight, texWidth, texHeight)
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    skinTexture,
                    rx, ry,                          // Screen position
                    (float) face.uvX(), (float) face.uvY(),  // UV start in texture
                    rw, rh,                          // Size on screen
                    face.uvWidth(), face.uvHeight(), // Size of region in texture (pixels)
                    64, 64                           // Full texture dimensions
                );
            } else {
                // Fallback: draw colored background
                int bgColor = (i == selectedRegionIndex) ? 0xFF446644 : 0xFF554433;
                context.fill(rx, ry, rx + rw, ry + rh, bgColor);

                // Draw label on fallback
                String name = getShortName(region.name());
                int textWidth = textRenderer.getWidth(name);
                if (textWidth < rw - 4) {
                    context.drawTextWithShadow(textRenderer, name,
                        rx + (rw - textWidth) / 2, ry + (rh - 8) / 2, 0xCCCCCC);
                }
            }

            // Draw grid overlay
            drawGridOverlay(context, rx, ry, rw, rh, face.gridWidth(), face.gridHeight());

            // Draw border - green if selected, yellow if hovered, white otherwise
            int borderColor;
            if (i == selectedRegionIndex) {
                borderColor = 0xFF00FF00;  // Green for selected
            } else if (i == hoveredRegionIndex && selectedDesign != null) {
                borderColor = 0xFFFFFF00;  // Yellow for hovered (when design selected)
            } else {
                borderColor = 0x88FFFFFF;  // White for normal
            }
            drawBorder(context, rx, ry, rw, rh, borderColor);
        }
    }

    private void drawGridOverlay(DrawContext context, int x, int y, int w, int h, int gridW, int gridH) {
        int cellW = w / gridW;
        int cellH = h / gridH;

        // Vertical lines
        for (int i = 1; i < gridW; i++) {
            int lx = x + i * cellW;
            context.fill(lx, y, lx + 1, y + h, 0x44FFFFFF);
        }

        // Horizontal lines
        for (int i = 1; i < gridH; i++) {
            int ly = y + i * cellH;
            context.fill(x, ly, x + w, ly + 1, 0x44FFFFFF);
        }
    }

    private void renderExistingTattoos(DrawContext context) {
        List<TattooInstance> tattoos = ClientTattooCache.getAllTattoos();
        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();

        for (TattooInstance tattoo : tattoos) {
            // Find which visual region this tattoo is on
            PaperDollMapper.BodyFace face = PaperDollMapper.getFaceAtGrid(tattoo.gridX(), tattoo.gridY());
            if (face == null) continue;

            // Find the visual region for this face
            for (PaperDollMapper.VisualRegion region : regions) {
                if (region.face().partName().equals(face.partName()) &&
                    region.face().faceName().equals(face.faceName())) {

                    // Calculate position within the visual region
                    int localGridX = tattoo.gridX() - face.gridX();
                    int localGridY = tattoo.gridY() - face.gridY();

                    int cellW = region.visualWidth() / face.gridWidth();
                    int cellH = region.visualHeight() / face.gridHeight();

                    int tx = dollX + region.visualX() + localGridX * cellW;
                    int ty = dollY + region.visualY() + localGridY * cellH;

                    // Get tattoo size
                    int[] size = getTattooSizeForId(tattoo.tattooId());
                    int tw = size[0] * cellW;
                    int th = size[1] * cellH;

                    // Draw tattoo indicator (purple tint)
                    context.fill(tx, ty, tx + tw, ty + th, 0x66AA00AA);
                    drawBorder(context, tx, ty, tw, th, 0xFFAA00AA);

                    break;
                }
            }
        }
    }

    private void updateTextWidgets() {
        // Position
        String posName = "None";
        if (selectedRegionIndex >= 0) {
            List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
            if (selectedRegionIndex < regions.size()) {
                posName = regions.get(selectedRegionIndex).name();
            }
        }
        positionValue.setMessage(Text.literal(posName));

        // Ink status
        boolean hasInk = hasInkInInventory();
        inkStatus.setMessage(Text.literal(hasInk ? "Ink: Ready" : "Ink: None!").withColor(hasInk ? 0x00FF00 : 0xFF4444));

        // Update apply button
        applyButton.active = selectedRegionIndex >= 0 && selectedDesign != null && hasInk && hasNeedleInHand();

        // Highlight selected design button
        for (int i = 0; i < designButtons.size(); i++) {
            ButtonWidget btn = designButtons.get(i);
            if (i < availableDesigns.size()) {
                DesignEntry entry = availableDesigns.get(i);
                String name = formatTattooName(entry.item.getTattooId());

                if (entry.item == selectedDesign) {
                    name = "> " + name + " <";
                }

                int required = entry.item.getRequiredSkinMagicLevel();
                if (ClientSkillCache.getSkinMagic() < required) {
                    name += " (Lv" + required + ")";
                }

                btn.setMessage(Text.literal(name));
            }
        }
    }

    // ========== Button Handlers ==========

    private void selectRegion(int index) {
        selectedRegionIndex = index;
    }

    private void selectDesign(int index) {
        if (index >= 0 && index < availableDesigns.size()) {
            DesignEntry entry = availableDesigns.get(index);
            int required = entry.item.getRequiredSkinMagicLevel();
            if (ClientSkillCache.getSkinMagic() >= required) {
                selectedDesign = entry.item;
                selectedDesignSlot = entry.slot;
            }
        }
    }

    private void applyTattoo() {
        if (selectedDesign == null || selectedRegionIndex < 0) return;
        if (!hasInkInInventory() || !hasNeedleInHand()) return;

        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
        if (selectedRegionIndex >= regions.size()) return;

        PaperDollMapper.VisualRegion region = regions.get(selectedRegionIndex);
        PaperDollMapper.BodyFace face = region.face();

        // Place at center of face
        int[] size = new int[] { selectedDesign.getGridWidth(), selectedDesign.getGridHeight() };
        int gridX = face.gridX() + (face.gridWidth() - size[0]) / 2;
        int gridY = face.gridY() + (face.gridHeight() - size[1]) / 2;

        ClientPlayNetworking.send(new ModNetworking.ApplyTattooPayload(
            selectedDesign.getTattooId(),
            gridX,
            gridY
        ));

        // Reset selection
        selectedDesign = null;
        selectedDesignSlot = -1;
    }

    // ========== Helper Methods ==========

    private String getShortName(String name) {
        return switch(name) {
            case "Left Arm" -> "L.Arm";
            case "Right Arm" -> "R.Arm";
            case "Left Leg" -> "L.Leg";
            case "Right Leg" -> "R.Leg";
            default -> name;
        };
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private String formatTattooName(String tattooId) {
        return tattooId.replace("_", " ");
    }

    private boolean hasInkInInventory() {
        if (client == null || client.player == null) return false;
        var inventory = client.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).getItem() instanceof TattooInkItem) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNeedleInHand() {
        if (client == null || client.player == null) return false;
        ItemStack mainHand = client.player.getMainHandStack();
        return mainHand.getItem() instanceof mugasofer.aerb.item.TattooNeedleItem;
    }

    private int[] getTattooSizeForId(String tattooId) {
        for (DesignEntry entry : availableDesigns) {
            if (entry.item.getTattooId().equals(tattooId)) {
                return new int[] { entry.item.getGridWidth(), entry.item.getGridHeight() };
            }
        }
        return new int[] { 2, 2 };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // Only handle left clicks
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        // Convert to doll area coordinates
        int localX = (int) click.x() - dollX;
        int localY = (int) click.y() - dollY;

        // Check if click is within doll area
        if (localX >= 0 && localX < DOLL_AREA_WIDTH && localY >= 0 && localY < DOLL_AREA_HEIGHT) {
            // Check each visual region
            List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
            for (int i = 0; i < regions.size(); i++) {
                PaperDollMapper.VisualRegion region = regions.get(i);
                int rx = region.visualX();
                int ry = region.visualY();
                int rw = region.visualWidth();
                int rh = region.visualHeight();

                if (localX >= rx && localX < rx + rw && localY >= ry && localY < ry + rh) {
                    // Clicked on this region
                    selectRegion(i);
                    return true;
                }
            }
        }

        // Let widgets handle the click if not on a region
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
