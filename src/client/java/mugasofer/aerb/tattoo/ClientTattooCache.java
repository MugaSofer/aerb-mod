package mugasofer.aerb.tattoo;

import mugasofer.aerb.render.TattooTextureManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side cache of the local player's tattoos.
 * Updated via network sync from server.
 */
public class ClientTattooCache {
    private static final List<TattooInstance> tattoos = new ArrayList<>();

    /**
     * Update the cache with data from server.
     * Also invalidates the texture cache to regenerate tattooed skin.
     */
    public static void update(List<TattooInstance> newTattoos) {
        tattoos.clear();
        tattoos.addAll(newTattoos);

        // Invalidate texture cache for the local player so their tattooed skin regenerates
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            TattooTextureManager.invalidateCache(client.player.getUuid());
        }
    }

    /**
     * Check if the player has at least one instance of a specific tattoo.
     */
    public static boolean hasTattoo(String tattooId) {
        return tattoos.stream().anyMatch(t -> t.tattooId().equals(tattooId));
    }

    /**
     * Get all instances of a specific tattoo type.
     */
    public static List<TattooInstance> getTattooInstances(String tattooId) {
        return tattoos.stream()
                .filter(t -> t.tattooId().equals(tattooId))
                .toList();
    }

    /**
     * Get all unique tattoo IDs.
     */
    public static Set<String> getActiveTattooIds() {
        Set<String> ids = new HashSet<>();
        for (TattooInstance t : tattoos) {
            ids.add(t.tattooId());
        }
        return ids;
    }

    /**
     * Get all tattoo instances.
     */
    public static List<TattooInstance> getAllTattoos() {
        return new ArrayList<>(tattoos);
    }

    /**
     * Clear the cache (e.g., on disconnect).
     */
    public static void clear() {
        tattoos.clear();
    }
}
