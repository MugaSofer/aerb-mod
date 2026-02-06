package mugasofer.aerb.screen;

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
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Paper doll UI for applying tattoos.
 * Shows a body diagram with clickable regions that map to grid coordinates.
 *
 * NOTE: This is a temporary implementation for Phase 1.
 * Phase 4 will replace this with a full grid-based paper doll UI
 * that shows the player's skin texture and supports drag & drop.
 */
public class TattooApplicationScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 200;

    // Paper doll dimensions (left half of panel)
    private static final int DOLL_WIDTH = 80;
    private static final int DOLL_HEIGHT = 150;

    private int panelX;
    private int panelY;

    /**
     * Body region with visual bounds and corresponding grid coordinates.
     */
    private record BodyRegion(String name, int[] visualBounds, int gridX, int gridY) {}

    // Clickable body regions with grid coordinate mapping
    private final List<BodyRegion> bodyRegions = new ArrayList<>();

    // Currently selected region index and design
    private int selectedRegionIndex = -1;
    private TattooDesignItem selectedDesign = null;
    private int selectedDesignSlot = -1;

    // Available designs from player's inventory
    private final List<DesignEntry> availableDesigns = new ArrayList<>();

    private record DesignEntry(TattooDesignItem item, int slot, ItemStack stack) {}

    // Buttons
    private ButtonWidget applyButton;
    private final List<ButtonWidget> bodyButtons = new ArrayList<>();
    private final List<ButtonWidget> designButtons = new ArrayList<>();

    // Text widgets
    private TextWidget positionLabel;
    private TextWidget positionValue;
    private TextWidget designsLabel;
    private TextWidget inkStatus;
    private final TextWidget[] designTexts = new TextWidget[5];

    public TattooApplicationScreen() {
        super(Text.literal("Tattoo Application"));
        initBodyRegions();
    }

    private void initBodyRegions() {
        // Define clickable rectangles for each body region
        // Format: name, {x, y, width, height} relative to paper doll top-left, gridX, gridY
        // Grid coordinates map to approximate UV positions on 16x16 grid

        // Head/Neck (head UV is around 8,8 on 64x64 = grid 2,2)
        bodyRegions.add(new BodyRegion("Face", new int[]{28, 5, 24, 20}, 2, 2));
        bodyRegions.add(new BodyRegion("Neck", new int[]{32, 25, 16, 10}, 2, 4));

        // Torso (body UV is around 20,20 on 64x64 = grid 5,5)
        bodyRegions.add(new BodyRegion("Left Shoulder", new int[]{10, 35, 18, 15}, 4, 5));
        bodyRegions.add(new BodyRegion("Right Shoulder", new int[]{52, 35, 18, 15}, 7, 5));
        bodyRegions.add(new BodyRegion("Chest", new int[]{25, 35, 30, 25}, 5, 5));
        bodyRegions.add(new BodyRegion("Back", new int[]{25, 60, 30, 20}, 8, 5));

        // Arms (arm UV varies)
        bodyRegions.add(new BodyRegion("Left Upper Arm", new int[]{5, 50, 15, 20}, 11, 5));
        bodyRegions.add(new BodyRegion("Left Forearm", new int[]{0, 70, 15, 25}, 11, 7));
        bodyRegions.add(new BodyRegion("Right Upper Arm", new int[]{60, 50, 15, 20}, 9, 5));
        bodyRegions.add(new BodyRegion("Right Forearm", new int[]{65, 70, 15, 25}, 9, 7));

        // Legs
        bodyRegions.add(new BodyRegion("Left Thigh", new int[]{22, 85, 16, 30}, 1, 5));
        bodyRegions.add(new BodyRegion("Left Calf", new int[]{20, 115, 16, 30}, 1, 8));
        bodyRegions.add(new BodyRegion("Right Thigh", new int[]{42, 85, 16, 30}, 0, 5));
        bodyRegions.add(new BodyRegion("Right Calf", new int[]{44, 115, 16, 30}, 0, 8));
    }

    @Override
    protected void init() {
        super.init();

        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        int dollX = panelX + 10;
        int dollY = panelY + 25;

        // Create invisible buttons for body regions
        bodyButtons.clear();
        for (int i = 0; i < bodyRegions.size(); i++) {
            final int index = i;
            BodyRegion region = bodyRegions.get(i);
            int[] rect = region.visualBounds();
            ButtonWidget btn = ButtonWidget.builder(Text.empty(), button -> selectBodyRegion(index))
                .dimensions(dollX + rect[0], dollY + rect[1], rect[2], rect[3])
                .build();
            bodyButtons.add(btn);
            addDrawableChild(btn);
        }

        // Scan inventory for available designs first
        scanForDesigns();

        // Create buttons for designs
        designButtons.clear();
        int infoX = panelX + DOLL_WIDTH + 20;
        int infoY = panelY + 25 + 35 + 12;
        for (int i = 0; i < availableDesigns.size() && i < 5; i++) {
            final int index = i;
            ButtonWidget btn = ButtonWidget.builder(Text.empty(), button -> selectDesign(index))
                .dimensions(infoX, infoY + i * 12, 100, 12)
                .build();
            designButtons.add(btn);
            addDrawableChild(btn);
        }

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply Tattoo"), button -> applyTattoo())
            .dimensions(panelX + PANEL_WIDTH - 80, panelY + PANEL_HEIGHT - 25, 70, 20)
            .build();
        applyButton.active = false;
        addDrawableChild(applyButton);

        // Text widgets for labels and values
        int textY = panelY + 25;

        positionLabel = new TextWidget(infoX, textY, 100, 12, Text.literal("Position:"), textRenderer);
        addDrawableChild(positionLabel);

        positionValue = new TextWidget(infoX, textY + 12, 100, 12, Text.literal("None"), textRenderer);
        addDrawableChild(positionValue);

        textY += 35;
        designsLabel = new TextWidget(infoX, textY, 100, 12, Text.literal("Designs:"), textRenderer);
        addDrawableChild(designsLabel);

        textY += 12;
        for (int i = 0; i < 5; i++) {
            designTexts[i] = new TextWidget(infoX, textY + i * 12, 100, 12, Text.empty(), textRenderer);
            addDrawableChild(designTexts[i]);
        }

        inkStatus = new TextWidget(infoX, panelY + PANEL_HEIGHT - 50, 100, 12, Text.literal("Ink: ?"), textRenderer);
        addDrawableChild(inkStatus);
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay background (don't call renderBackground - it causes double blur)
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Panel background
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC222222);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 5, 0xFFFFFF);

        // Paper doll area
        int dollX = panelX + 10;
        int dollY = panelY + 25;

        // Draw paper doll outline (placeholder - just a rectangle for now)
        context.fill(dollX, dollY, dollX + DOLL_WIDTH, dollY + DOLL_HEIGHT, 0x44AAAAAA);
        drawBorder(context, dollX, dollY, DOLL_WIDTH, DOLL_HEIGHT, 0xFF777777);

        // Get existing tattoos for indicator
        List<TattooInstance> existingTattoos = ClientTattooCache.getAllTattoos();

        // Draw body regions
        for (int i = 0; i < bodyRegions.size(); i++) {
            BodyRegion region = bodyRegions.get(i);
            int[] rect = region.visualBounds();
            int rx = dollX + rect[0];
            int ry = dollY + rect[1];
            int rw = rect[2];
            int rh = rect[3];

            // Check if this region has an existing tattoo (approximate by grid proximity)
            boolean hasTattoo = existingTattoos.stream().anyMatch(t ->
                Math.abs(t.gridX() - region.gridX()) <= 1 &&
                Math.abs(t.gridY() - region.gridY()) <= 1
            );

            // Highlight selected region (green)
            if (i == selectedRegionIndex) {
                context.fill(rx, ry, rx + rw, ry + rh, 0x6600FF00);
            } else if (hasTattoo) {
                // Existing tattoo indicator (purple/magenta)
                context.fill(rx, ry, rx + rw, ry + rh, 0x66AA00AA);
            } else if (isMouseOver(mouseX, mouseY, rx, ry, rw, rh)) {
                context.fill(rx, ry, rx + rw, ry + rh, 0x44FFFFFF);
            }
            drawBorder(context, rx, ry, rw, rh, 0x88FFFFFF);
        }

        // Update text widgets with current values
        String posName = selectedRegionIndex >= 0 ? bodyRegions.get(selectedRegionIndex).name() : "None";
        positionValue.setMessage(Text.literal(posName));

        // Update design texts
        if (availableDesigns.isEmpty()) {
            designTexts[0].setMessage(Text.literal("(none found)").withColor(0x888888));
            for (int i = 1; i < 5; i++) {
                designTexts[i].setMessage(Text.empty());
            }
        } else {
            for (int i = 0; i < 5; i++) {
                if (i < availableDesigns.size()) {
                    DesignEntry entry = availableDesigns.get(i);
                    String name = formatTattooName(entry.item.getTattooId());
                    int color = (entry.item == selectedDesign) ? 0x00FF00 : 0xFFFFFF;

                    int required = entry.item.getRequiredSkinMagicLevel();
                    int playerLevel = ClientSkillCache.getSkinMagic();
                    if (playerLevel < required) {
                        color = 0xFF4444;
                        name += " (Lv " + required + ")";
                    }

                    designTexts[i].setMessage(Text.literal(name).withColor(color));
                } else {
                    designTexts[i].setMessage(Text.empty());
                }
            }
        }

        // Update ink status
        boolean hasInk = hasInkInInventory();
        String inkText = hasInk ? "Ink: Ready" : "Ink: None!";
        int inkColor = hasInk ? 0x00FF00 : 0xFF4444;
        inkStatus.setMessage(Text.literal(inkText).withColor(inkColor));

        // Render all widgets
        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * Draw a 1-pixel border around a rectangle.
     */
    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        // Top
        context.fill(x, y, x + w, y + 1, color);
        // Bottom
        context.fill(x, y + h - 1, x + w, y + h, color);
        // Left
        context.fill(x, y, x + 1, y + h, color);
        // Right
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private String formatTattooName(String tattooId) {
        return tattooId.replace("_", " ");
    }

    /**
     * Select a body region.
     */
    private void selectBodyRegion(int index) {
        selectedRegionIndex = index;
        updateApplyButton();
    }

    /**
     * Select a design.
     */
    private void selectDesign(int index) {
        if (index >= 0 && index < availableDesigns.size()) {
            DesignEntry entry = availableDesigns.get(index);
            int required = entry.item.getRequiredSkinMagicLevel();
            int playerLevel = ClientSkillCache.getSkinMagic();
            if (playerLevel >= required) {
                selectedDesign = entry.item;
                selectedDesignSlot = entry.slot;
                updateApplyButton();
            }
        }
    }

    private void updateApplyButton() {
        boolean canApply = selectedRegionIndex >= 0 &&
                          selectedDesign != null &&
                          hasInkInInventory() &&
                          hasNeedleInHand();
        applyButton.active = canApply;
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

    private void applyTattoo() {
        if (selectedDesign == null || selectedRegionIndex < 0) return;

        BodyRegion region = bodyRegions.get(selectedRegionIndex);

        // Send packet to server with grid coordinates
        ClientPlayNetworking.send(new ModNetworking.ApplyTattooPayload(
            selectedDesign.getTattooId(),
            region.gridX(),
            region.gridY()
        ));

        // Close screen
        close();
    }
}
