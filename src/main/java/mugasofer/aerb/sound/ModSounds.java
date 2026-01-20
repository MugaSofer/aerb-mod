package mugasofer.aerb.sound;

import mugasofer.aerb.Aerb;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Custom sound events for the mod, allowing specific sound file selection.
 */
public class ModSounds {
    // Umbral Undead sounds - specific crunchy bone/meat sounds
    public static final SoundEvent UMBRAL_CRUNCH = register("umbral_crunch");
    public static final SoundEvent UMBRAL_SMASH = register("umbral_smash");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(Aerb.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
        Aerb.LOGGER.info("Registering mod sounds");
    }
}
