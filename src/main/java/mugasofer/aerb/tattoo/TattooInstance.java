package mugasofer.aerb.tattoo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Represents a single tattoo instance on a player.
 * Players can have multiple instances of the same tattoo type at different positions.
 *
 * @param tattooId The type of tattoo (e.g., "fall_rune", "icy_devil")
 * @param gridX Grid X coordinate (0-15, where each cell is 4x4 skin pixels)
 * @param gridY Grid Y coordinate (0-15, where each cell is 4x4 skin pixels)
 * @param cooldownUntil World time when cooldown expires (0 = no cooldown)
 */
public record TattooInstance(
        String tattooId,
        int gridX,
        int gridY,
        long cooldownUntil
) {
    public static final Codec<TattooInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("tattoo_id").forGetter(TattooInstance::tattooId),
                    Codec.INT.fieldOf("grid_x").forGetter(TattooInstance::gridX),
                    Codec.INT.fieldOf("grid_y").forGetter(TattooInstance::gridY),
                    Codec.LONG.fieldOf("cooldown_until").forGetter(TattooInstance::cooldownUntil)
            ).apply(instance, TattooInstance::new)
    );

    /**
     * Create a new tattoo instance with no cooldown.
     */
    public static TattooInstance create(String tattooId, int gridX, int gridY) {
        return new TattooInstance(tattooId, gridX, gridY, 0);
    }

    /**
     * Check if this tattoo is on cooldown.
     */
    public boolean isOnCooldown(long currentTime) {
        return cooldownUntil > currentTime;
    }

    /**
     * Check if this tattoo can be used right now.
     */
    public boolean canUse(long currentTime) {
        return !isOnCooldown(currentTime);
    }

    /**
     * Create a new instance with cooldown set.
     */
    public TattooInstance withCooldown(long until) {
        return new TattooInstance(tattooId, gridX, gridY, until);
    }

    /**
     * Create a new instance at a different position.
     */
    public TattooInstance movedTo(int newGridX, int newGridY) {
        return new TattooInstance(tattooId, newGridX, newGridY, cooldownUntil);
    }
}
