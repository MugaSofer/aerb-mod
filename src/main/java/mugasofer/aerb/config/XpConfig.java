package mugasofer.aerb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mugasofer.aerb.Aerb;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for skill XP system.
 */
public class XpConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aerb_xp.json");

    private static XpConfig INSTANCE;

    // XP amounts per action type
    public int xpPerDamageDealt = 1;
    public int xpPerParry = 1;
    public int xpPerSpellCast = 1;
    public int xpPerNewItemEquip = 1;
    public int xpPerKill = 1;

    // Level-up formula: XP needed = level^exponent
    public double levelExponent = 2.0;

    // Book detection settings
    // NOTE: Vanilla Minecraft book titles have a very low character limit (~15 chars),
    // making this system impractical for most use cases. Consider using unique
    // "Commoner's Guide" items instead for skill unlocking. This system is kept
    // for potential use with mods/datapacks that extend book title limits.
    public int bookMinCharacters = 250;

    // Skill name mappings for book detection (case-insensitive matching)
    public Map<String, String> bookSkillKeywords = new HashMap<>();

    public XpConfig() {
        // Initialize default book keywords
        bookSkillKeywords.put("blood magic", "blood_magic");
        bookSkillKeywords.put("bone magic", "bone_magic");
        bookSkillKeywords.put("one-handed", "one_handed");
        bookSkillKeywords.put("parry", "parry");
        bookSkillKeywords.put("parrying", "parry");
        bookSkillKeywords.put("swordsmanship", "one_handed");
        bookSkillKeywords.put("fencing", "one_handed");
    }

    public static XpConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, XpConfig.class);
                Aerb.LOGGER.info("Loaded XP config from " + CONFIG_PATH);
            } catch (Exception e) {
                Aerb.LOGGER.error("Failed to load XP config, using defaults", e);
                INSTANCE = new XpConfig();
                save();
            }
        } else {
            INSTANCE = new XpConfig();
            save();
            Aerb.LOGGER.info("Created default XP config at " + CONFIG_PATH);
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            Aerb.LOGGER.error("Failed to save XP config", e);
        }
    }

    /**
     * Get XP required to reach the next level from current level.
     * Formula: (nextLevel + 1)^exponent
     * This means: 0->1 needs 4 XP, 1->2 needs 9 XP, 2->3 needs 16 XP, etc.
     */
    public int getXpForNextLevel(int currentLevel) {
        int nextLevel = currentLevel + 1;
        return (int) Math.pow(nextLevel + 1, levelExponent);
    }
}
