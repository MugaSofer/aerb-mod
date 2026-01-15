package mugasofer.aerb.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class SpellSlotsScreen extends HandledScreen<SpellSlotsScreenHandler> {

    private final PlayerInventory playerInventory;

    // Tab dimensions - consistent across all screens
    private static final int TAB_WIDTH = 40;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 24;

    public SpellSlotsScreen(SpellSlotsScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal("Spells Known"));
        this.playerInventory = inventory;
        // Spell slots (3 rows) + offhand + hotbar
        this.backgroundHeight = 122;
        this.backgroundWidth = 176;
    }

    @Override
    protected void init() {
        super.init();

        // Position tabs to match inventory screen (left of 176-wide panel)
        int tabX = (this.width - 176) / 2 - TAB_WIDTH - 4;
        int tabY = (this.height - 166) / 2;

        // Inventory tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Inv"), button -> {
            this.client.setScreen(new InventoryScreen(this.playerInventory.player));
        }).dimensions(tabX, tabY, TAB_WIDTH, TAB_HEIGHT).build());

        // Stats tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stats"), button -> {
            this.client.setScreen(new CharacterSheetScreen(this.playerInventory.player));
        }).dimensions(tabX, tabY + TAB_SPACING, TAB_WIDTH, TAB_HEIGHT).build());

        // Spells tab (current - disabled/highlighted)
        ButtonWidget spellsTab = ButtonWidget.builder(Text.literal("Spells"), button -> {
            // Already on this screen
        }).dimensions(tabX, tabY + TAB_SPACING * 2, TAB_WIDTH, TAB_HEIGHT).build();
        spellsTab.active = false;
        this.addDrawableChild(spellsTab);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw dark background panel
        context.fill(x - 2, y - 2, x + this.backgroundWidth + 2, y + this.backgroundHeight + 2, 0xFF333333);
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xFFC6C6C6);

        // Draw slot backgrounds for spell inventory (27 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 17 + row * 18;
                context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF373737);
                context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF8B8B8B);
            }
        }

        // Draw offhand slot background (centered between spell grid and hotbar)
        int offhandX = x + 79;
        int offhandY = y + 75;
        context.fill(offhandX, offhandY, offhandX + 18, offhandY + 18, 0xFF373737);
        context.fill(offhandX + 1, offhandY + 1, offhandX + 17, offhandY + 17, 0xFF8B8B8B);

        // Draw hotbar slot backgrounds
        for (int col = 0; col < 9; col++) {
            int slotX = x + 7 + col * 18;
            int slotY = y + 97;
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
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        // Offhand label (centered above the slot)
        context.drawText(this.textRenderer, Text.literal("Off"), 82, 66, 0x404040, false);
    }
}
