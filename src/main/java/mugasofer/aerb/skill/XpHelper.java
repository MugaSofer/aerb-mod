package mugasofer.aerb.skill;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.command.ModCommands;
import mugasofer.aerb.config.XpConfig;
import mugasofer.aerb.item.AardesTouchItem;
import mugasofer.aerb.item.BloodMagicItem;
import mugasofer.aerb.item.BoneMagicItem;
import mugasofer.aerb.item.PhysicalTappingItem;
import mugasofer.aerb.network.ModNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

/**
 * Centralized XP granting logic for the skill system.
 */
public class XpHelper {

    /**
     * Award XP to a skill, handling level-ups and sync.
     * If the skill is locked, it will be unlocked first.
     * Returns true if a level-up occurred.
     */
    public static boolean awardXp(ServerPlayerEntity player, String skillName, int amount) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);

        int currentLevel = skills.getSkillLevel(skillName);

        // Auto-unlock skill on first XP gain
        if (!skills.isUnlocked(skillName)) {
            skills.setSkillLevel(skillName, 0);
            currentLevel = 0;
            player.sendMessage(Text.literal("Skill unlocked: " + formatSkillName(skillName) + "!"), false);

            // Check for level-0 unlocks (e.g., Aarde's Touch at Blood Magic 0)
            ModCommands.checkSpellUnlocks(player, skillName, 0);
            ModCommands.checkVirtueUnlocks(player, skillName, 0);
        }

        int currentXp = skills.getSkillXp(skillName);
        int newXp = currentXp + amount;

        boolean leveledUp = false;
        int xpNeeded = XpConfig.get().getXpForNextLevel(currentLevel);

        // Check for level-ups (can level up multiple times from one XP gain)
        while (newXp >= xpNeeded && currentLevel < 300) {
            newXp -= xpNeeded;
            currentLevel++;
            leveledUp = true;
            xpNeeded = XpConfig.get().getXpForNextLevel(currentLevel);
        }

        skills.setSkillXp(skillName, newXp);

        if (leveledUp) {
            skills.setSkillLevel(skillName, currentLevel);
            player.sendMessage(Text.literal("Skill increased: " + formatSkillName(skillName) + " lvl " + currentLevel + "!"), false);

            // Check for unlock thresholds (spells/virtues)
            ModCommands.checkSpellUnlocks(player, skillName, currentLevel);
            ModCommands.checkVirtueUnlocks(player, skillName, currentLevel);
        }

        ModNetworking.syncSkillsToClient(player);
        return leveledUp;
    }

    /**
     * Format a skill name for display (e.g., "blood_magic" -> "Blood Magic").
     */
    public static String formatSkillName(String skillName) {
        String[] parts = skillName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return result.toString();
    }

    /**
     * Get the skill associated with a weapon or item.
     * Returns null if not a relevant item.
     */
    public static String getRelevantSkill(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // One-handed weapons: swords and axes
        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)) {
            return PlayerSkills.ONE_HANDED;
        }

        // Blood Magic items
        Item item = stack.getItem();
        if (item instanceof BloodMagicItem || item instanceof AardesTouchItem) {
            return PlayerSkills.BLOOD_MAGIC;
        }

        // Bone Magic items
        if (item instanceof BoneMagicItem || item instanceof PhysicalTappingItem) {
            return PlayerSkills.BONE_MAGIC;
        }

        // Written books - check title and content
        return checkBookForSkill(stack);
    }

    /**
     * Check if a written book grants XP for a skill.
     * Requirements: title contains skill keyword, content has 250+ characters.
     */
    private static String checkBookForSkill(ItemStack stack) {
        if (!stack.isOf(Items.WRITTEN_BOOK)) return null;

        WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (content == null) return null;

        String title = content.title().raw().toLowerCase();

        // Calculate total character count from all pages
        int totalChars = 0;
        for (var page : content.pages()) {
            totalChars += page.raw().getString().length();
        }

        if (totalChars < XpConfig.get().bookMinCharacters) return null;

        // Check for skill keywords in title
        for (Map.Entry<String, String> entry : XpConfig.get().bookSkillKeywords.entrySet()) {
            if (title.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Generate a unique identifier for an item.
     * For weapons: registry ID + custom name (if any)
     * For books: "book:" + title + ":" + author
     */
    public static String getItemIdentifier(ItemStack stack) {
        String baseId = Registries.ITEM.getId(stack.getItem()).toString();

        // For written books, use title and author
        if (stack.isOf(Items.WRITTEN_BOOK)) {
            WrittenBookContentComponent content = stack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                return "book:" + content.title().raw() + ":" + content.author();
            }
        }

        // For named items, include the custom name
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
            if (name != null) {
                return baseId + ":" + name.getString();
            }
        }

        return baseId;
    }

    /**
     * Check if an item is a one-handed weapon (sword or axe).
     */
    public static boolean isOneHandedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }
}
