package mugasofer.aerb.screen;

import mugasofer.aerb.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class VirtuesScreen extends HandledScreen<VirtuesScreenHandler> {

    private final PlayerInventory playerInventory;

    // Tab dimensions - consistent across all screens
    private static final int TAB_WIDTH = 40;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 24;

    public VirtuesScreen(VirtuesScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal("Virtues"));
        this.playerInventory = inventory;
        // Virtue slots (3 rows) + hotbar
        this.backgroundHeight = 100;
        this.backgroundWidth = 176;
    }

    @Override
    protected void init() {
        super.init();

        // Restore mouse position if switching from another handled screen
        MousePositionHelper.restorePosition(this.client);

        // Calculate panel position (using our actual background dimensions)
        int panelX = (this.width - this.backgroundWidth) / 2;
        int panelY = (this.height - this.backgroundHeight) / 2;

        // Add title as TextWidget (absolute screen coordinates)
        int titleWidth = this.textRenderer.getWidth(this.title);
        this.addDrawableChild(new TextWidget(
            panelX + (this.backgroundWidth - titleWidth) / 2, panelY + 6,
            titleWidth, 10, this.title, this.textRenderer));

        // Position tabs to match inventory screen (left of 176-wide panel)
        int tabX = (this.width - 176) / 2 - TAB_WIDTH - 4;
        int tabY = (this.height - 166) / 2;

        // Inventory tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Inv"), button -> {
            switchScreen(new InventoryScreen(this.playerInventory.player));
        }).dimensions(tabX, tabY, TAB_WIDTH, TAB_HEIGHT).build());

        // Stats tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stats"), button -> {
            switchScreen(new CharacterSheetScreen(this.playerInventory.player));
        }).dimensions(tabX, tabY + TAB_SPACING, TAB_WIDTH, TAB_HEIGHT).build());

        // Spells tab - server will close this screen when opening the new one
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Spells"), button -> {
            MousePositionHelper.savePosition(this.client);
            ClientPlayNetworking.send(new ModNetworking.OpenSpellInventoryPayload());
        }).dimensions(tabX, tabY + TAB_SPACING * 2, TAB_WIDTH, TAB_HEIGHT).build());

        // Virtues tab (current - disabled/highlighted)
        ButtonWidget virtuesTab = ButtonWidget.builder(Text.literal("Virtues"), button -> {
            // Already on this screen
        }).dimensions(tabX, tabY + TAB_SPACING * 3, TAB_WIDTH, TAB_HEIGHT).build();
        virtuesTab.active = false;
        this.addDrawableChild(virtuesTab);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw dark background panel
        context.fill(x - 2, y - 2, x + this.backgroundWidth + 2, y + this.backgroundHeight + 2, 0xFF333333);
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);

        // Draw slot backgrounds for virtue inventory (27 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 17 + row * 18;
                context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF373737);
                context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            }
        }

        // Draw hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            int slotX = x + 7 + col * 18;
            int slotY = y + 75;
            context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF373737);
            context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Title is handled by TextWidget added in init()
    }

    /**
     * Switch to another screen while preserving mouse position.
     * Closes current handler to avoid sync issues, then restores mouse.
     */
    private void switchScreen(Screen newScreen) {
        // Save mouse position
        double mouseX = this.client.mouse.getX();
        double mouseY = this.client.mouse.getY();
        long windowHandle = this.client.getWindow().getHandle();

        // Close current handler to avoid sync ID issues
        this.close();

        // Open new screen
        this.client.setScreen(newScreen);

        // Restore mouse position on next tick (after screen fully initializes)
        this.client.execute(() -> {
            GLFW.glfwSetCursorPos(windowHandle, mouseX, mouseY);
        });
    }
}
