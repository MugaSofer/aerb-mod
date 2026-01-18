package mugasofer.aerb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mugasofer.aerb.Aerb;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for spell and virtue descriptions.
 * Descriptions appear as lore text on items.
 */
public class DescriptionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("aerb_descriptions.json");

    private static DescriptionConfig INSTANCE;

    // Map of item ID (e.g., "aardes_touch") to description lines
    public Map<String, List<String>> descriptions = new HashMap<>();

    public static DescriptionConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, DescriptionConfig.class);
                Aerb.LOGGER.info("Loaded description config from " + CONFIG_PATH);
            } catch (IOException e) {
                Aerb.LOGGER.error("Failed to load description config, using defaults", e);
                INSTANCE = createDefaults();
                save();
            }
        } else {
            INSTANCE = createDefaults();
            save();
            Aerb.LOGGER.info("Created default description config at " + CONFIG_PATH);
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            Aerb.LOGGER.error("Failed to save description config", e);
        }
    }

    /**
     * Get description lines for an item, or empty list if none defined.
     */
    public List<String> getDescription(String itemId) {
        return descriptions.getOrDefault(itemId, List.of());
    }

    private static DescriptionConfig createDefaults() {
        DescriptionConfig config = new DescriptionConfig();

        // Blood Magic Spells
        config.descriptions.put("aardes_touch", List.of(
            "Channels the warmth of your blood to ignite your fingertips.",
			"Does not burn you, but objects set on fire may still harm you.",
			"Named for the god of warmth and life."
        ));
        config.descriptions.put("crimson_fist", List.of(
            "Channels the force of your blood to add kinetic energy to your attacks.",
			"Costs a small amount of blood."
        ));
        config.descriptions.put("sanguine_surge", List.of(
            "Channels the force of your blood to gain kinetic energy in the form of a leap.",
			"Costs a small amount of blood."
        ));

        // Bone Magic Spells
        config.descriptions.put("physical_tapping", List.of(
            "Taps into the physical abilites of a bone's owner.",
			"Burns one bone."
        ));
        config.descriptions.put("power_tapping", List.of(
            "Taps into the physical strength of a bone's owner.",
			"Burns one bone."
        ));
        config.descriptions.put("speed_tapping", List.of(
            "Taps into the speed of a bone's owner.",
			"Burns one bone."
        ));
        config.descriptions.put("endurance_tapping", List.of(
            "Taps into the toughness and vitality of a bone's owner.",
			"Burns one bone.",
			"The primary form of Bone Magic healing."
        ));

        // Blood Magic Virtues
        config.descriptions.put("hypertension", List.of(
            "Passive",
            "Doubles blood volume.",
			"2x max HP, but extra HP is only effective vs bleeding."
        ));

        // Parry Virtues
        config.descriptions.put("prescient_blade", List.of(
            "Passive",
            "Halves the projectile modifier for parry rolls."
        ));
        config.descriptions.put("prophetic_blade", List.of(
            "Active (when in hotbar)",
            "Always parrying, from any direction.",
            "Auto-switches to best weapon on parry."
        ));

        // One-Handed Weapons Virtues
        config.descriptions.put("riposter", List.of(
            "Active (when in hotbar)",
            "On successful parry with sword or axe,",
            "immediately counterattack."
        ));

        return config;
    }
}
