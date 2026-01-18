package mugasofer.aerb.combat;

/**
 * Tracks state for Prophetic Blade's instant weapon switch.
 * When skip frames > 0, the HeldItemRenderer mixin will skip the equip animation.
 */
public class ParryAnimationState {
    // Number of frames to keep skipping the equip animation
    private static int skipEquipAnimationFrames = 0;

    public static void requestSkipEquipAnimation() {
        // Skip for several frames to ensure we catch the right moment
        skipEquipAnimationFrames = 5;
    }

    public static boolean shouldSkipEquipAnimation() {
        if (skipEquipAnimationFrames > 0) {
            skipEquipAnimationFrames--;
            return true;
        }
        return false;
    }
}
