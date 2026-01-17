package mugasofer.aerb.screen;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Helper to preserve mouse position across async screen transitions.
 */
public class MousePositionHelper {
    private static double savedMouseX = -1;
    private static double savedMouseY = -1;

    /**
     * Save current mouse position before an async screen transition.
     */
    public static void savePosition(MinecraftClient client) {
        savedMouseX = client.mouse.getX();
        savedMouseY = client.mouse.getY();
    }

    /**
     * Restore saved mouse position. Call from new screen's init().
     * Clears the saved position after restoring.
     */
    public static void restorePosition(MinecraftClient client) {
        if (savedMouseX >= 0 && savedMouseY >= 0) {
            double x = savedMouseX;
            double y = savedMouseY;
            savedMouseX = -1;
            savedMouseY = -1;

            // Defer to next tick to ensure screen is fully initialized
            client.execute(() -> {
                GLFW.glfwSetCursorPos(client.getWindow().getHandle(), x, y);
            });
        }
    }
}
