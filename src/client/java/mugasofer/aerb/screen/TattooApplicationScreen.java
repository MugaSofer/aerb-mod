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
 * Shows a body diagram with clickable regions that map to grid coordinates
 * using the PaperDollMapper for proper UV coordinate translation.
 *
 * NOTE: This is an intermediate implementation for Phase 3.
 * Phase 4 will add drag-and-drop and show the actual player skin texture.
 */
public class TattooApplicationScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 220;

    // Paper doll dimensions (left side of panel)
    private static final int DOLL_WIDTH = 100;
    private static final int DOLL_HEIGHT = 170;

    private int panelX;
    private int panelY;
    private int dollX;
    private int dollY;

    // Currently selected region and design
    private int selectedRegionIndex = -1;
    private TattooDesignItem selectedDesign = null;
    private int selectedDesignSlot = -1;

    // Available designs from player's inventory
    private final List<DesignEntry> availableDesigns = new ArrayList<>();

    private record DesignEntry(TattooDesignItem item, int slot, ItemStack stack) {}

    // Buttons
    private ButtonWidget applyButton;
    private final List<ButtonWidget> regionButtons = new ArrayList<>();
    private final List<ButtonWidget> designButtons = new ArrayList<>();

    // Text widgets
    private TextWidget positionLabel;
    private TextWidget positionValue;
    private TextWidget designsLabel;
    private TextWidget inkStatus;
    private final TextWidget[] designTexts = new TextWidget[5];

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

        // Create buttons for visual regions from PaperDollMapper
        regionButtons.clear();
        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
        for (int i = 0; i < regions.size(); i++) {
            final int index = i;
            PaperDollMapper.VisualRegion region = regions.get(i);
            ButtonWidget btn = ButtonWidget.builder(Text.empty(), button -> selectRegion(index))
                .dimensions(
                    dollX + region.visualX(),
                    dollY + region.visualY(),
                    region.visualWidth(),
                    region.visualHeight()
                )
                .build();
            regionButtons.add(btn);
            addDrawableChild(btn);
        }

        // Scan inventory for available designs
        scanForDesigns();

        // Create buttons for designs
        designButtons.clear();
        int infoX = panelX + DOLL_WIDTH + 25;
        int infoY = panelY + 30 + 35 + 12;
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
            .dimensions(panelX + PANEL_WIDTH - 85, panelY + PANEL_HEIGHT - 28, 75, 20)
            .build();
        applyButton.active = false;
        addDrawableChild(applyButton);

        // Text widgets for labels and values
        int textY = panelY + 30;

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

        inkStatus = new TextWidget(infoX, panelY + PANEL_HEIGHT - 55, 100, 12, Text.literal("Ink: ?"), textRenderer);
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
        // Dark overlay background
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Panel background
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC222222);
        drawBorder(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        // Title
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 8, 0xFFFFFF);

        // Paper doll area background
        context.fill(dollX, dollY, dollX + DOLL_WIDTH, dollY + DOLL_HEIGHT, 0x44AAAAAA);
        drawBorder(context, dollX, dollY, DOLL_WIDTH, DOLL_HEIGHT, 0xFF777777);

        // Get existing tattoos for indicator
        List<TattooInstance> existingTattoos = ClientTattooCache.getAllTattoos();

        // Draw visual regions from PaperDollMapper
        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
        for (int i = 0; i < regions.size(); i++) {
            PaperDollMapper.VisualRegion region = regions.get(i);
            int rx = dollX + region.visualX();
            int ry = dollY + region.visualY();
            int rw = region.visualWidth();
            int rh = region.visualHeight();

            // Check if this region has an existing tattoo
            PaperDollMapper.BodyFace face = region.face();
            boolean hasTattoo = existingTattoos.stream().anyMatch(t -> {
                PaperDollMapper.BodyFace tattooFace = PaperDollMapper.getFaceAtGrid(t.gridX(), t.gridY());
                return tattooFace != null &&
                    tattooFace.partName().equals(face.partName()) &&
                    tattooFace.faceName().equals(face.faceName());
            });

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

            // Draw region name
            String shortName = getShortName(region.name());
            int textWidth = textRenderer.getWidth(shortName);
            if (textWidth < rw - 4) {
                context.drawTextWithShadow(textRenderer, shortName,
                    rx + (rw - textWidth) / 2, ry + (rh - 8) / 2, 0xCCCCCC);
            }
        }

        // Update text widgets with current values
        String posName = "None";
        if (selectedRegionIndex >= 0 && selectedRegionIndex < regions.size()) {
            posName = regions.get(selectedRegionIndex).name();
        }
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

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private String formatTattooName(String tattooId) {
        return tattooId.replace("_", " ");
    }

    private void selectRegion(int index) {
        selectedRegionIndex = index;
        updateApplyButton();
    }

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

        List<PaperDollMapper.VisualRegion> regions = PaperDollMapper.getVisualRegions();
        if (selectedRegionIndex >= regions.size()) return;

        PaperDollMapper.VisualRegion region = regions.get(selectedRegionIndex);
        PaperDollMapper.BodyFace face = region.face();

        // Place tattoo at the center of the selected body face
        int gridX = face.gridX() + face.gridWidth() / 2;
        int gridY = face.gridY() + face.gridHeight() / 2;

        // Adjust for tattoo size to center it
        int[] tattooSize = getTattooSize(selectedDesign.getTattooId());
        gridX -= tattooSize[0] / 2;
        gridY -= tattooSize[1] / 2;

        // Clamp to face bounds
        gridX = Math.max(face.gridX(), Math.min(gridX, face.gridX() + face.gridWidth() - tattooSize[0]));
        gridY = Math.max(face.gridY(), Math.min(gridY, face.gridY() + face.gridHeight() - tattooSize[1]));

        // Send packet to server with grid coordinates
        ClientPlayNetworking.send(new ModNetworking.ApplyTattooPayload(
            selectedDesign.getTattooId(),
            gridX,
            gridY
        ));

        // Close screen
        close();
    }

    private int[] getTattooSize(String tattooId) {
        // Get size from design item if possible
        for (DesignEntry entry : availableDesigns) {
            if (entry.item.getTattooId().equals(tattooId)) {
                return new int[] { entry.item.getGridWidth(), entry.item.getGridHeight() };
            }
        }
        // Default size
        return new int[] { 2, 2 };
    }
}
