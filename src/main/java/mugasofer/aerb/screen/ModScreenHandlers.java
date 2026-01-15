package mugasofer.aerb.screen;

import mugasofer.aerb.Aerb;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<SpellSlotsScreenHandler> SPELL_SLOTS_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(Aerb.MOD_ID, "spell_slots"),
            new ScreenHandlerType<>(SpellSlotsScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
        );

    public static void init() {
        Aerb.LOGGER.info("Registering screen handlers for " + Aerb.MOD_ID);
    }
}
