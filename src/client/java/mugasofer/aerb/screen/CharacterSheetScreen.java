package mugasofer.aerb.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class CharacterSheetScreen extends Screen {
    private static final int BASE_POW = 2;
    private static final int BASE_SPD = 2;
    private static final int BASE_END = 2;

    private final PlayerEntity player;

    public CharacterSheetScreen(PlayerEntity player) {
        super(Text.literal("Character Sheet"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        int panelX = this.width / 2 - 100;
        int panelY = 20;
        int lineHeight = 14;

        // Title - centered in panel
        this.addDrawableChild(new TextWidget(panelX, panelY + 8, 200, lineHeight,
            Text.literal("CHARACTER SHEET"), this.textRenderer));

        // PHYSICAL section
        int y = panelY + 32;
        this.addDrawableChild(new TextWidget(panelX + 10, y, 180, lineHeight,
            Text.literal("PHYSICAL").withColor(0xFFAA00), this.textRenderer));
        y += lineHeight;

        int phy = Math.min(Math.min(BASE_POW, BASE_SPD), BASE_END) + 1;
        this.addDrawableChild(new TextWidget(panelX + 20, y, 170, lineHeight,
            Text.literal("PHY: " + phy), this.textRenderer));
        y += lineHeight;

        this.addDrawableChild(new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("POW: " + BASE_POW).withColor(0xAAAAAA), this.textRenderer));
        y += lineHeight;

        this.addDrawableChild(new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("SPD: " + BASE_SPD).withColor(0xAAAAAA), this.textRenderer));
        y += lineHeight;

        this.addDrawableChild(new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("END: " + BASE_END).withColor(0xAAAAAA), this.textRenderer));

        // SKILLS section
        y += lineHeight + 8;
        this.addDrawableChild(new TextWidget(panelX + 10, y, 180, lineHeight,
            Text.literal("SKILLS").withColor(0xFFAA00), this.textRenderer));
        y += lineHeight;

        this.addDrawableChild(new TextWidget(panelX + 20, y, 170, lineHeight,
            Text.literal("Blood Magic: 0").withColor(0xAAAAAA), this.textRenderer));
        y += lineHeight;

        this.addDrawableChild(new TextWidget(panelX + 20, y, 170, lineHeight,
            Text.literal("Bone Magic: 0").withColor(0xAAAAAA), this.textRenderer));

        // Back to inventory button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Inventory"), button -> {
            this.client.setScreen(new InventoryScreen(player));
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay (like vanilla screens)
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Dark panel behind stats
        int panelX = this.width / 2 - 100;
        int panelY = 20;
        context.fill(panelX - 2, panelY - 2, panelX + 202, panelY + 182, 0xFF333333);
        context.fill(panelX, panelY, panelX + 200, panelY + 180, 0xFF000000);

        // Render all widgets (TextWidgets and buttons)
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, int x, int y, String name, int base, int current) {
        if (current > base) {
            // Base value in gray, bonus in green
            String baseText = name + ": " + base;
            String bonusText = " (+" + (current - base) + ")";
            context.drawTextWithShadow(this.textRenderer, Text.literal(baseText), x, y, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.literal(bonusText),
                x + this.textRenderer.getWidth(baseText), y, 0x55FF55);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal(name + ": " + current), x, y, 0xAAAAAA);
        }
    }

    private int calculatePOW() {
        int pow = BASE_POW;
        if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
            pow += player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            pow += player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1;
        }
        return pow;
    }

    private int calculateSPD() {
        int spd = BASE_SPD;
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            spd += player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.HASTE)) {
            spd += player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1;
        }
        return spd;
    }

    private int calculateEND() {
        int end = BASE_END;
        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            end += player.getStatusEffect(StatusEffects.REGENERATION).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            end += player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
        }
        return end;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
