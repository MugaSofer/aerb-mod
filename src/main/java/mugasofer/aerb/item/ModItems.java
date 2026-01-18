package mugasofer.aerb.item;

import mugasofer.aerb.Aerb;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.List;

public class ModItems {
    private static final int EFFECT_DURATION = 120; // 6 seconds

    public static final Item PHYSICAL_TAPPING = register("physical_tapping",
        new PhysicalTappingItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "physical_tapping")))));

    public static final Item POWER_TAPPING = register("power_tapping",
        new BoneMagicItem(
            new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "power_tapping"))),
            List.of(
                new BoneMagicItem.SpellEffect(StatusEffects.STRENGTH, EFFECT_DURATION, 3),      // Strength IV
                new BoneMagicItem.SpellEffect(StatusEffects.JUMP_BOOST, EFFECT_DURATION, 3)    // Jump Boost IV
            )
        ));

    public static final Item SPEED_TAPPING = register("speed_tapping",
        new BoneMagicItem(
            new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "speed_tapping"))),
            List.of(
                new BoneMagicItem.SpellEffect(StatusEffects.SPEED, EFFECT_DURATION, 3),        // Speed IV
                new BoneMagicItem.SpellEffect(StatusEffects.HASTE, EFFECT_DURATION, 3)         // Haste IV
            )
        ));

    public static final Item ENDURANCE_TAPPING = register("endurance_tapping",
        new BoneMagicItem(
            new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "endurance_tapping"))),
            List.of(
                new BoneMagicItem.SpellEffect(StatusEffects.REGENERATION, EFFECT_DURATION, 1), // Regeneration II (~3 hearts over 6s)
                new BoneMagicItem.SpellEffect(StatusEffects.RESISTANCE, EFFECT_DURATION, 3),  // Resistance IV
                new BoneMagicItem.SpellEffect(StatusEffects.SATURATION, 1, 1)                 // Saturation II (instant)
            )
        ));

    // Blood Magic
    public static final Item AARDES_TOUCH = register("aardes_touch",
        new AardesTouchItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "aardes_touch")))));

    public static final Item CRIMSON_FIST = register("crimson_fist",
        new BloodMagicItem(
            new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "crimson_fist"))),
            List.of(
                new BloodMagicItem.SpellEffect(StatusEffects.STRENGTH, EFFECT_DURATION, 3)  // Strength IV
            )
        ));

    public static final Item SANGUINE_SURGE = register("sanguine_surge",
        new BloodMagicItem(
            new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "sanguine_surge"))),
            List.of(
                new BloodMagicItem.SpellEffect(StatusEffects.JUMP_BOOST, EFFECT_DURATION, 3)  // Jump Boost IV
            )
        ));

    // Virtues (Blood Magic)
    public static final Item HYPERTENSION = register("hypertension",
        new HypertensionItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "hypertension")))));

    // Virtues (Parry)
    public static final Item PRESCIENT_BLADE = register("prescient_blade",
        new PrescientBladeItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "prescient_blade")))));

    public static final Item PROPHETIC_BLADE = register("prophetic_blade",
        new PropheticBladeItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "prophetic_blade")))));

    // Virtues (One-Handed Weapons)
    public static final Item RIPOSTER = register("riposter",
        new RiposterItem(new Item.Settings()
            .maxCount(1)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Aerb.MOD_ID, "riposter")))));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Aerb.MOD_ID, name), item);
    }

    public static void initialize() {
        Aerb.LOGGER.info("Registering items for " + Aerb.MOD_ID);

        // Add to the Tools item group in creative menu
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            // Spells
            entries.add(PHYSICAL_TAPPING);
            entries.add(POWER_TAPPING);
            entries.add(SPEED_TAPPING);
            entries.add(ENDURANCE_TAPPING);
            entries.add(AARDES_TOUCH);
            entries.add(CRIMSON_FIST);
            entries.add(SANGUINE_SURGE);
            // Virtues
            entries.add(HYPERTENSION);
            entries.add(PRESCIENT_BLADE);
            entries.add(PROPHETIC_BLADE);
            entries.add(RIPOSTER);
        });
    }
}
