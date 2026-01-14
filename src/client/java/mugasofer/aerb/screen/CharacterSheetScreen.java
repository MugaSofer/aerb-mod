package mugasofer.aerb.screen;

import mugasofer.aerb.spell.SpellSlots;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class CharacterSheetScreen extends Screen {
    private static final int BASE_POW = 2;
    private static final int BASE_SPD = 2;
    private static final int BASE_END = 2;

    private final PlayerEntity player;

    // TextWidgets for dynamic stat display
    private TextWidget phyWidget;
    private TextWidget powWidget;
    private TextWidget spdWidget;
    private TextWidget endWidget;

    // Spell slot positions for click detection
    private int spellSlotsX;
    private int spellSlotsY;

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

        // Dynamic stat widgets (updated each frame in render())
        this.phyWidget = new TextWidget(panelX + 20, y, 170, lineHeight,
            Text.literal("PHY: ?"), this.textRenderer);
        this.addDrawableChild(this.phyWidget);
        y += lineHeight;

        this.powWidget = new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("POW: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.powWidget);
        y += lineHeight;

        this.spdWidget = new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("SPD: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.spdWidget);
        y += lineHeight;

        this.endWidget = new TextWidget(panelX + 30, y, 160, lineHeight,
            Text.literal("END: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.endWidget);

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

        // SPELL SLOTS section
        y += lineHeight + 8;
        this.addDrawableChild(new TextWidget(panelX + 10, y, 180, lineHeight,
            Text.literal("SPELL SLOTS").withColor(0xFFAA00), this.textRenderer));
        y += lineHeight;

        // Store position for rendering slots
        this.spellSlotsX = panelX + 20;
        this.spellSlotsY = y;

        // Back to inventory button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Inventory"), button -> {
            this.client.setScreen(new InventoryScreen(player));
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay (like vanilla screens)
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Dark panel behind stats (taller to fit spell slots)
        int panelX = this.width / 2 - 100;
        int panelY = 20;
        context.fill(panelX - 2, panelY - 2, panelX + 202, panelY + 222, 0xFF333333);
        context.fill(panelX, panelY, panelX + 200, panelY + 220, 0xFF000000);

        // Update dynamic stat widgets
        int pow = calculatePOW();
        int spd = calculateSPD();
        int end = calculateEND();
        int phy = Math.min(Math.min(pow, spd), end) + 1;
        int basePhy = Math.min(Math.min(BASE_POW, BASE_SPD), BASE_END) + 1;

        this.phyWidget.setMessage(formatStat("PHY", basePhy, phy));
        this.powWidget.setMessage(formatStat("POW", BASE_POW, pow));
        this.spdWidget.setMessage(formatStat("SPD", BASE_SPD, spd));
        this.endWidget.setMessage(formatStat("END", BASE_END, end));

        // Render spell slots (always show, even if empty)
        SpellSlots spellSlots = player.getAttached(SpellSlots.ATTACHMENT);
        for (int i = 0; i < SpellSlots.MAX_SLOTS; i++) {
            int slotX = spellSlotsX + (i * 20);
            int slotY = spellSlotsY;

            // Draw slot background
            context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF222222);
            context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF3C3C3C);

            // Draw equipped item if any
            if (spellSlots != null) {
                ItemStack stack = spellSlots.getSlot(i);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, slotX + 1, slotY + 1);
                }
            }
        }

        // Render all widgets (TextWidgets and buttons)
        super.render(context, mouseX, mouseY, delta);
    }

    private Text formatStat(String name, int base, int current) {
        if (current > base) {
            // Show base value with green bonus
            return Text.literal(name + ": " + base).withColor(0xAAAAAA)
                .append(Text.literal(" (+" + (current - base) + ")").withColor(0x55FF55));
        } else {
            return Text.literal(name + ": " + current).withColor(0xAAAAAA);
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
