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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

    // Scrolling for entire content
    private static final int LINE_HEIGHT = 14;
    private static final int VISIBLE_LINES = 10; // Number of lines visible at once
    private static final int CONTENT_PADDING = 10; // Padding from panel edges
    private int scrollOffset = 0;

    private final PlayerEntity player;

    // Content entries - each provides text dynamically
    private final List<ContentEntry> contentEntries = new ArrayList<>();

    // TextWidgets for visible content slots (updated dynamically based on scroll)
    private final TextWidget[] slotWidgets = new TextWidget[VISIBLE_LINES];

    // Content area bounds for scroll detection
    private int contentAreaTop;
    private int contentAreaBottom;
    private int contentAreaLeft;
    private int contentAreaRight;

    // Scroll bar bounds
    private int scrollBarX;
    private int scrollBarTop;
    private int scrollBarHeight;

    // Scroll buttons
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;

    // Entry types for different indentation/styling
    private record ContentEntry(Supplier<Text> textSupplier, int indent) {}

    public CharacterSheetScreen(PlayerEntity player) {
        super(Text.literal("Character Sheet"));
        this.player = player;

        // Build content list - this defines the full scrollable content
        buildContentList();
    }

    private void buildContentList() {
        contentEntries.clear();

        // PHYSICAL section
        contentEntries.add(new ContentEntry(
            () -> Text.literal("PHYSICAL").withColor(0xFFAA00), 0));
        contentEntries.add(new ContentEntry(
            () -> formatStat("PHY", getBasePhy(), calculatePHY()), 10));
        contentEntries.add(new ContentEntry(
            () -> formatStat("POW", BASE_POW, calculatePOW()), 20));
        contentEntries.add(new ContentEntry(
            () -> formatStat("SPD", BASE_SPD, calculateSPD()), 20));
        contentEntries.add(new ContentEntry(
            () -> formatStat("END", BASE_END, calculateEND()), 20));

        // Spacer
        contentEntries.add(new ContentEntry(() -> Text.empty(), 0));

        // SKILLS section
        contentEntries.add(new ContentEntry(
            () -> Text.literal("SKILLS").withColor(0xFFAA00), 0));

        // Add all skills
        addSkillEntry("Blood Magic", ClientSkillCache::getBloodMagic, ClientSkillCache::getBloodMagicXp);
        addSkillEntry("Bone Magic", ClientSkillCache::getBoneMagic, ClientSkillCache::getBoneMagicXp);
        addSkillEntry("One-Handed", ClientSkillCache::getOneHanded, ClientSkillCache::getOneHandedXp);
        addSkillEntry("Parry", ClientSkillCache::getParry, ClientSkillCache::getParryXp);
        addSkillEntry("Horticulture", ClientSkillCache::getHorticulture, ClientSkillCache::getHorticultureXp);
        addSkillEntry("Art", ClientSkillCache::getArt, ClientSkillCache::getArtXp);
        addSkillEntry("Skin Magic", ClientSkillCache::getSkinMagic, ClientSkillCache::getSkinMagicXp);
    }

    private void addSkillEntry(String name, Supplier<Integer> levelGetter, Supplier<Integer> xpGetter) {
        contentEntries.add(new ContentEntry(
            () -> formatSkill(name, levelGetter.get(), xpGetter.get()), 10));
    }

    private int getBasePhy() {
        return Math.min(Math.min(BASE_POW, BASE_SPD), BASE_END) + 1;
    }

    private int calculatePHY() {
        return Math.min(Math.min(calculatePOW(), calculateSPD()), calculateEND()) + 1;
    }

    @Override
    protected void init() {
        super.init();

        // Center panel like vanilla inventory
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Content area bounds (with padding, leaving room for scroll bar)
        this.contentAreaTop = panelY + CONTENT_PADDING;
        this.contentAreaBottom = panelY + PANEL_HEIGHT - CONTENT_PADDING;
        this.contentAreaLeft = panelX + CONTENT_PADDING;
        this.contentAreaRight = panelX + PANEL_WIDTH - CONTENT_PADDING - 8; // Leave room for scroll bar

        // Scroll bar bounds (on the right edge of the panel)
        this.scrollBarX = panelX + PANEL_WIDTH - 6;
        this.scrollBarTop = contentAreaTop;
        this.scrollBarHeight = VISIBLE_LINES * LINE_HEIGHT;

        // Create TextWidgets for visible slots (content updated in render())
        int y = contentAreaTop;
        for (int i = 0; i < VISIBLE_LINES; i++) {
            slotWidgets[i] = new TextWidget(contentAreaLeft, y, PANEL_WIDTH - (CONTENT_PADDING * 2), LINE_HEIGHT,
                Text.literal(""), this.textRenderer);
            this.addDrawableChild(slotWidgets[i]);
            y += LINE_HEIGHT;
        }

        // Ensure scroll offset is valid
        int maxScroll = Math.max(0, contentEntries.size() - VISIBLE_LINES);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Add scroll buttons if content is scrollable
        if (contentEntries.size() > VISIBLE_LINES) {
            // Scroll up button (above scroll track)
            this.scrollUpButton = ButtonWidget.builder(Text.literal("\u25B2"), button -> {
                scrollOffset = Math.max(0, scrollOffset - 1);
            }).dimensions(scrollBarX - 4, scrollBarTop - 14, 12, 12).build();
            this.addDrawableChild(scrollUpButton);

            // Scroll down button (below scroll track)
            this.scrollDownButton = ButtonWidget.builder(Text.literal("\u25BC"), button -> {
                int max = Math.max(0, contentEntries.size() - VISIBLE_LINES);
                scrollOffset = Math.min(max, scrollOffset + 1);
            }).dimensions(scrollBarX - 4, scrollBarTop + scrollBarHeight + 2, 12, 12).build();
            this.addDrawableChild(scrollDownButton);
        }

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

        // Dark panel behind content (centered like inventory)
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        context.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, 0xFF333333);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF000000);

        // Update slot widgets based on scroll offset
        for (int i = 0; i < VISIBLE_LINES; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex < contentEntries.size()) {
                ContentEntry entry = contentEntries.get(entryIndex);
                slotWidgets[i].setMessage(entry.textSupplier.get());
                slotWidgets[i].setX(contentAreaLeft + entry.indent);
                slotWidgets[i].visible = true;
            } else {
                slotWidgets[i].setMessage(Text.empty());
                slotWidgets[i].visible = false;
            }
        }

        // Render all widgets (TextWidgets and buttons)
        super.render(context, mouseX, mouseY, delta);

        // Render scroll indicator if needed (using context.fill which should work)
        renderScrollIndicator(context);
    }

    private void renderScrollIndicator(DrawContext context) {
        int maxScroll = Math.max(0, contentEntries.size() - VISIBLE_LINES);
        if (maxScroll > 0) {
            // Track background
            context.fill(scrollBarX, scrollBarTop, scrollBarX + 4, scrollBarTop + scrollBarHeight, 0xFF333333);

            // Scroll thumb position
            float scrollProgress = (float) scrollOffset / maxScroll;
            int thumbHeight = Math.max(15, scrollBarHeight * VISIBLE_LINES / contentEntries.size());
            int thumbY = scrollBarTop + (int) ((scrollBarHeight - thumbHeight) * scrollProgress);

            context.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, 0xFF888888);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Allow scrolling anywhere on the panel
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH &&
            mouseY >= panelY && mouseY <= panelY + PANEL_HEIGHT) {
            int maxScroll = Math.max(0, contentEntries.size() - VISIBLE_LINES);
            if (verticalAmount > 0) {
                // Scroll up
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (verticalAmount < 0) {
                // Scroll down
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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

    private Text formatSkill(String name, int level, int xp) {
        if (level < 0) {
            // Locked - show greyed out
            return Text.literal(name + ": Locked").withColor(0x555555);
        } else {
            // Unlocked - show level with XP progress
            int xpNeeded = getXpForNextLevel(level);
            return Text.literal(name + ": " + level).withColor(0xAAAAAA)
                .append(Text.literal(" (" + xp + "/" + xpNeeded + " XP)").withColor(0x777777));
        }
    }

    /**
     * Calculate XP needed for next level (mirrors server-side XpConfig formula).
     * Formula: (nextLevel + 1)^2
     * This means: 0->1 needs 4 XP, 1->2 needs 9 XP, 2->3 needs 16 XP, etc.
     */
    private int getXpForNextLevel(int currentLevel) {
        int nextLevel = currentLevel + 1;
        return (nextLevel + 1) * (nextLevel + 1);
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
