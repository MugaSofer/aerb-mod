package mugasofer.aerb.tattoo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * State of a single tattoo on a player.
 *
 * @param charges Number of uses remaining. -1 = unlimited, 0 = depleted, 1+ = charges left
 * @param cooldownUntil World time when cooldown expires. 0 = no cooldown active
 * @param position Where on the body the tattoo is located
 */
public record TattooState(
        int charges,
        long cooldownUntil,
        BodyPosition position
) {
    public static final Codec<TattooState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("charges").forGetter(TattooState::charges),
                    Codec.LONG.fieldOf("cooldown_until").forGetter(TattooState::cooldownUntil),
                    Codec.STRING.fieldOf("position").forGetter(ts -> ts.position.name())
            ).apply(instance, (charges, cooldown, posName) ->
                    new TattooState(charges, cooldown, BodyPosition.valueOf(posName)))
    );

    // Convenience constants
    public static final int UNLIMITED = -1;
    public static final int DEPLETED = 0;

    /**
     * Create a single-use tattoo at the default position.
     */
    public static TattooState singleUse() {
        return new TattooState(1, 0, BodyPosition.ANY);
    }

    /**
     * Create a single-use tattoo at a specific position.
     */
    public static TattooState singleUse(BodyPosition position) {
        return new TattooState(1, 0, position);
    }

    /**
     * Create an unlimited-use tattoo.
     */
    public static TattooState unlimited() {
        return new TattooState(UNLIMITED, 0, BodyPosition.ANY);
    }

    /**
     * Create a tattoo with specific charges.
     */
    public static TattooState withCharges(int charges) {
        return new TattooState(charges, 0, BodyPosition.ANY);
    }

    /**
     * Check if this tattoo has charges remaining.
     */
    public boolean hasCharges() {
        return charges == UNLIMITED || charges > 0;
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
        return hasCharges() && !isOnCooldown(currentTime);
    }

    /**
     * Create a new state with one charge consumed.
     */
    public TattooState useCharge() {
        if (charges == UNLIMITED) {
            return this;
        }
        return new TattooState(Math.max(0, charges - 1), cooldownUntil, position);
    }

    /**
     * Create a new state with cooldown set.
     */
    public TattooState withCooldown(long until) {
        return new TattooState(charges, until, position);
    }

    /**
     * Create a new state at a different position.
     */
    public TattooState movedTo(BodyPosition newPosition) {
        return new TattooState(charges, cooldownUntil, newPosition);
    }
}
