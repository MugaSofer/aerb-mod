package mugasofer.aerb.tattoo;

/**
 * Positions where a tattoo can be placed on the body.
 * Used for the "tattoos can move" mechanic from Worth the Candle.
 */
public enum BodyPosition {
    // Arms
    LEFT_UPPER_ARM,
    LEFT_FOREARM,
    RIGHT_UPPER_ARM,
    RIGHT_FOREARM,

    // Torso
    CHEST,
    BACK,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,

    // Legs
    LEFT_THIGH,
    LEFT_CALF,
    RIGHT_THIGH,
    RIGHT_CALF,

    // Head/Neck
    FACE,
    NECK,

    // Special - tattoo can be anywhere / position doesn't matter
    ANY;

    /**
     * Check if this position is on exposed skin for the default Steve/Alex skins.
     * Used as a fallback when no custom mask is available.
     */
    public boolean isExposedDefault() {
        return switch (this) {
            case LEFT_FOREARM, RIGHT_FOREARM, FACE, NECK -> true;
            default -> false;
        };
    }
}
