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
                new BoneMagicItem.SpellEffect(StatusEffects.REGENERATION, EFFECT_DURATION, 3), // Regeneration IV
                new BoneMagicItem.SpellEffect(StatusEffects.INSTANT_HEALTH, 1, 1),             // Instant Health II (instant)
                new BoneMagicItem.SpellEffect(StatusEffects.RESISTANCE, EFFECT_DURATION, 3),  // Resistance IV
                new BoneMagicItem.SpellEffect(StatusEffects.SATURATION, 1, 1)                 // Saturation II (instant)
            )
        ));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Aerb.MOD_ID, name), item);
    }

    public static void initialize() {
        Aerb.LOGGER.info("Registering items for " + Aerb.MOD_ID);

        // Add to the Tools item group in creative menu
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(PHYSICAL_TAPPING);
            entries.add(POWER_TAPPING);
            entries.add(SPEED_TAPPING);
            entries.add(ENDURANCE_TAPPING);
        });
    }
}
