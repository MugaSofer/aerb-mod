package mugasofer.aerb.tattoo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Represents a single tattoo instance on a player.
 * Players can have multiple instances of the same tattoo type at different positions.
 *
 * @param tattooId The type of tattoo (e.g., "fall_rune", "icy_devil")
 * @param position Where on the body this tattoo is located
 * @param cooldownUntil World time when cooldown expires (0 = no cooldown)
 */
public record TattooInstance(
        String tattooId,
        BodyPosition position,
        long cooldownUntil
) {
    public static final Codec<TattooInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("tattoo_id").forGetter(TattooInstance::tattooId),
                    Codec.STRING.fieldOf("position").forGetter(ti -> ti.position.name()),
                    Codec.LONG.fieldOf("cooldown_until").forGetter(TattooInstance::cooldownUntil)
            ).apply(instance, (id, posName, cooldown) ->
                    new TattooInstance(id, BodyPosition.valueOf(posName), cooldown))
    );

    /**
     * Create a new tattoo instance with no cooldown.
     */
    public static TattooInstance create(String tattooId, BodyPosition position) {
        return new TattooInstance(tattooId, position, 0);
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
        return new TattooInstance(tattooId, position, until);
    }

    /**
     * Create a new instance at a different position.
     */
    public TattooInstance movedTo(BodyPosition newPosition) {
        return new TattooInstance(tattooId, newPosition, cooldownUntil);
    }
}
