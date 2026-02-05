package mugasofer.aerb.tattoo;

import mugasofer.aerb.render.TattooTextureManager;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client-side cache of the local player's tattoos.
 * Updated via network sync from server.
 */
public class ClientTattooCache {
    private static final Map<String, TattooState> tattoos = new HashMap<>();

    /**
     * Update the cache with data from server.
     * Also invalidates the texture cache to regenerate tattooed skin.
     */
    public static void update(Map<String, TattooState> newTattoos) {
        tattoos.clear();
        tattoos.putAll(newTattoos);

        // Invalidate texture cache for the local player so their tattooed skin regenerates
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            TattooTextureManager.invalidateCache(client.player.getUuid());
        }
    }

    /**
     * Check if the player has a specific tattoo.
     */
    public static boolean hasTattoo(String tattooId) {
        TattooState state = tattoos.get(tattooId);
        return state != null && state.hasCharges();
    }

    /**
     * Get the state of a tattoo, or null if not present.
     */
    public static TattooState getTattoo(String tattooId) {
        return tattoos.get(tattooId);
    }

    /**
     * Get all active tattoo IDs.
     */
    public static Set<String> getActiveTattooIds() {
        Set<String> active = new java.util.HashSet<>();
        for (Map.Entry<String, TattooState> entry : tattoos.entrySet()) {
            if (entry.getValue().hasCharges()) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    /**
     * Get all tattoos.
     */
    public static Map<String, TattooState> getAllTattoos() {
        return new HashMap<>(tattoos);
    }

    /**
     * Clear the cache (e.g., on disconnect).
     */
    public static void clear() {
        tattoos.clear();
    }
}
