package mugasofer.aerb.screen;

import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.ClientSkillCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    // Panel size to match vanilla inventory
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;

    // Tab dimensions - consistent across all screens
    private static final int TAB_WIDTH = 40;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_SPACING = 24;

    private final PlayerEntity player;

    // TextWidgets for dynamic stat display
    private TextWidget phyWidget;
    private TextWidget powWidget;
    private TextWidget spdWidget;
    private TextWidget endWidget;

    // TextWidgets for dynamic skill display
    private TextWidget bloodMagicWidget;
    private TextWidget boneMagicWidget;
    private TextWidget oneHandedWidget;
    private TextWidget parryWidget;

    public CharacterSheetScreen(PlayerEntity player) {
        super(Text.literal("Character Sheet"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        // Center panel like vanilla inventory
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int lineHeight = 14;

        int y = panelY + 10;

        // PHYSICAL section
        this.addDrawableChild(new TextWidget(panelX + 10, y, 160, lineHeight,
            Text.literal("PHYSICAL").withColor(0xFFAA00), this.textRenderer));
        y += lineHeight;

        // Dynamic stat widgets (updated each frame in render())
        this.phyWidget = new TextWidget(panelX + 20, y, 150, lineHeight,
            Text.literal("PHY: ?"), this.textRenderer);
        this.addDrawableChild(this.phyWidget);
        y += lineHeight;

        this.powWidget = new TextWidget(panelX + 30, y, 140, lineHeight,
            Text.literal("POW: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.powWidget);
        y += lineHeight;

        this.spdWidget = new TextWidget(panelX + 30, y, 140, lineHeight,
            Text.literal("SPD: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.spdWidget);
        y += lineHeight;

        this.endWidget = new TextWidget(panelX + 30, y, 140, lineHeight,
            Text.literal("END: ?").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.endWidget);

        // SKILLS section
        y += lineHeight + 8;
        this.addDrawableChild(new TextWidget(panelX + 10, y, 160, lineHeight,
            Text.literal("SKILLS").withColor(0xFFAA00), this.textRenderer));
        y += lineHeight;

        this.bloodMagicWidget = new TextWidget(panelX + 20, y, 150, lineHeight,
            Text.literal("Blood Magic: 0").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.bloodMagicWidget);
        y += lineHeight;

        this.boneMagicWidget = new TextWidget(panelX + 20, y, 150, lineHeight,
            Text.literal("Bone Magic: 0").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.boneMagicWidget);
        y += lineHeight;

        this.oneHandedWidget = new TextWidget(panelX + 20, y, 150, lineHeight,
            Text.literal("One-Handed: 0").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.oneHandedWidget);
        y += lineHeight;

        this.parryWidget = new TextWidget(panelX + 20, y, 150, lineHeight,
            Text.literal("Parry: 0").withColor(0xAAAAAA), this.textRenderer);
        this.addDrawableChild(this.parryWidget);

        // Navigation tabs on the left side (consistent position across screens)
        int tabX = panelX - TAB_WIDTH - 4;
        int tabY = panelY;

        // Inventory tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Inv"), button -> {
            this.client.setScreen(new InventoryScreen(player));
        }).dimensions(tabX, tabY, TAB_WIDTH, TAB_HEIGHT).build());

        // Stats tab (current - disabled)
        ButtonWidget statsTab = ButtonWidget.builder(Text.literal("Stats"), button -> {
            // Already on this screen
        }).dimensions(tabX, tabY + TAB_SPACING, TAB_WIDTH, TAB_HEIGHT).build();
        statsTab.active = false;
        this.addDrawableChild(statsTab);

        // Spells tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Spells"), button -> {
            ClientPlayNetworking.send(new ModNetworking.OpenSpellInventoryPayload());
        }).dimensions(tabX, tabY + TAB_SPACING * 2, TAB_WIDTH, TAB_HEIGHT).build());

        // Virtues tab
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Virtues"), button -> {
            ClientPlayNetworking.send(new ModNetworking.OpenVirtueInventoryPayload());
        }).dimensions(tabX, tabY + TAB_SPACING * 3, TAB_WIDTH, TAB_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay (like vanilla screens)
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        // Dark panel behind stats (centered like inventory)
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        context.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF333333);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF000000);

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

        // Update skill widgets (from client cache, synced from server)
        // Locked skills (-1) show greyed out, unlocked skills (0+) show level
        int bloodLevel = ClientSkillCache.getBloodMagic();
        int boneLevel = ClientSkillCache.getBoneMagic();
        int oneHandedLevel = ClientSkillCache.getOneHanded();
        int parryLevel = ClientSkillCache.getParry();
        this.bloodMagicWidget.setMessage(formatSkill("Blood Magic", bloodLevel));
        this.boneMagicWidget.setMessage(formatSkill("Bone Magic", boneLevel));
        this.oneHandedWidget.setMessage(formatSkill("One-Handed", oneHandedLevel));
        this.parryWidget.setMessage(formatSkill("Parry", parryLevel));

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

    private Text formatSkill(String name, int level) {
        if (level < 0) {
            // Locked - show greyed out
            return Text.literal(name + ": Locked").withColor(0x555555);
        } else {
            // Unlocked - show level
            return Text.literal(name + ": " + level).withColor(0xAAAAAA);
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
