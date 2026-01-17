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
import java.util.Set;

/**
 * Configuration for Hypertension damage multipliers.
 *
 * Damage types are categorized as:
 * - Pointy: Piercing damage (arrows, thorns) - small multiplier
 * - Blunt: Impact damage (fall, fly into wall) - moderate multiplier
 * - Other: Non-physical damage (fire, magic) - high multiplier (extra blood doesn't help)
 */
public class HypertensionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aerb_hypertension.json");

    private static HypertensionConfig INSTANCE;

    // Default multipliers for each category
    public double pointyMultiplier = 1.25;  // 25% extra damage
    public double bluntMultiplier = 1.5;    // 50% extra damage
    public double otherMultiplier = 2.0;    // 100% extra damage (doubled)

    // Override multipliers for specific damage types (damage type ID -> multiplier)
    // Set to 1.0 for no change, or any other value to override category default
    public Map<String, Double> damageTypeOverrides = new HashMap<>();

    // Damage types in each category (can be modified by user)
    public Set<String> pointyDamageTypes = Set.of(
        "minecraft:arrow",
        "minecraft:cactus",
        "minecraft:sting",
        "minecraft:thorns",
        "minecraft:trident",
        "minecraft:sweet_berry_bush"
    );

    public Set<String> bluntDamageTypes = Set.of(
        "minecraft:fall",
        "minecraft:fly_into_wall",
        "minecraft:falling_block",
        "minecraft:falling_anvil",
        "minecraft:falling_stalactite",
        "minecraft:mob_attack",
        "minecraft:player_attack",
        "minecraft:cramming"
    );

    // Everything else uses otherMultiplier (fire, drowning, magic, explosion, etc.)

    public static HypertensionConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, HypertensionConfig.class);
                Aerb.LOGGER.info("Loaded Hypertension config from " + CONFIG_PATH);
            } catch (IOException e) {
                Aerb.LOGGER.error("Failed to load Hypertension config, using defaults", e);
                INSTANCE = new HypertensionConfig();
                save();
            }
        } else {
            INSTANCE = new HypertensionConfig();
            save();
            Aerb.LOGGER.info("Created default Hypertension config at " + CONFIG_PATH);
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            Aerb.LOGGER.error("Failed to save Hypertension config", e);
        }
    }

    /**
     * Get the damage multiplier for a specific damage type.
     */
    public double getMultiplier(String damageTypeId) {
        // Check for specific override first
        if (damageTypeOverrides.containsKey(damageTypeId)) {
            return damageTypeOverrides.get(damageTypeId);
        }

        // Check category
        if (pointyDamageTypes.contains(damageTypeId)) {
            return pointyMultiplier;
        }
        if (bluntDamageTypes.contains(damageTypeId)) {
            return bluntMultiplier;
        }

        // Default to "other" category
        return otherMultiplier;
    }
}
