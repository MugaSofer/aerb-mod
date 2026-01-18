package mugasofer.aerb.combat;

/**
 * Shared cache for parry skill level used by the animation.
 * This is in main so both client and server code can access it.
 * Client networking code updates this when skills are synced.
 */
public class ParrySkillCache {
    private static int clientParryLevel = 0;

    /**
     * Get the cached parry level for animation purposes.
     * Used by SwordParryMixin on the client side.
     */
    public static int getParryLevel() {
        return clientParryLevel;
    }

    /**
     * Update the cached parry level.
     * Called by client networking when skills are synced.
     */
    public static void setParryLevel(int level) {
        clientParryLevel = Math.max(0, level);
    }

    /**
     * Reset the cache (called on disconnect).
     */
    public static void reset() {
        clientParryLevel = 0;
    }
}
