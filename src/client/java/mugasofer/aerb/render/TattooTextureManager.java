package mugasofer.aerb.render;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.tattoo.ClientTattooCache;
import mugasofer.aerb.tattoo.PlayerTattoos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.resource.Resource;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages player skin textures with tattoos composited onto them.
 * Caches modified textures per player and regenerates when tattoos change.
 */
public class TattooTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("TattooTextureManager");

    // Mapping of tattoo ID to texture path
    private static final Map<String, Identifier> TATTOO_TEXTURES = Map.of(
        PlayerTattoos.FALL_RUNE, Identifier.of(Aerb.MOD_ID, "textures/entity/tattoo_fall_rune.png"),
        PlayerTattoos.ICY_DEVIL, Identifier.of(Aerb.MOD_ID, "textures/entity/tattoo_icy_devil.png")
    );

    // Skin masks - define which areas are exposed skin vs clothed
    private static final Identifier MASK_STEVE = Identifier.of(Aerb.MOD_ID, "textures/entity/skin_mask_steve.png");
    private static final Identifier MASK_ALEX = Identifier.of(Aerb.MOD_ID, "textures/entity/skin_mask_alex.png");

    // Cache of modified skin textures per player UUID
    // Key includes a hash of active tattoo IDs so cache invalidates when tattoos change
    private static final Map<UUID, CachedTattooSkin> skinCache = new HashMap<>();

    // Cached tattoo overlay images (loaded on demand)
    private static final Map<String, NativeImage> tattooImages = new HashMap<>();

    // Cached mask images
    private static NativeImage maskSteve = null;
    private static NativeImage maskAlex = null;
    private static boolean masksLoadAttempted = false;

    /**
     * Get modified skin textures with tattoos applied for a player.
     * Returns the original if no tattoos or on error.
     */
    public static SkinTextures getModifiedSkinTextures(PlayerListEntry entry, SkinTextures original) {
        MinecraftClient client = MinecraftClient.getInstance();
        UUID playerId = entry.getProfile().id();

        // Only apply tattoos to the local player (we only sync local player's tattoos for now)
        if (client.player == null || !client.player.getUuid().equals(playerId)) {
            return original;
        }

        // Get active tattoos from client cache
        Set<String> activeTattoos = ClientTattooCache.getActiveTattooIds();
        if (activeTattoos.isEmpty()) {
            // No tattoos - return original and clear any cached version
            skinCache.remove(playerId);
            return original;
        }

        // Create a cache key that includes the set of active tattoos
        // This ensures we regenerate when tattoos change
        int tattoosHash = activeTattoos.hashCode();

        // Check cache - use body().texturePath() to get the skin texture identifier
        CachedTattooSkin cached = skinCache.get(playerId);
        Identifier originalBodyTexture = original.body().texturePath();
        if (cached != null && cached.originalTexture.equals(originalBodyTexture) && cached.tattoosHash == tattoosHash) {
            // Cache hit - return cached modified textures
            return cached.modifiedTextures;
        }

        // Need to create/update the modified texture
        try {
            SkinTextures modified = createModifiedSkinTextures(entry, original, activeTattoos);
            if (modified != null) {
                skinCache.put(playerId, new CachedTattooSkin(originalBodyTexture, modified, tattoosHash));
                return modified;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create tattoo skin for player {}: {}", playerId, e.getMessage());
        }

        return original;
    }

    /**
     * Creates a new SkinTextures with tattoos composited onto the skin.
     */
    private static SkinTextures createModifiedSkinTextures(PlayerListEntry entry, SkinTextures original, Set<String> activeTattoos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getTextureManager() == null) {
            return null;
        }

        // Load skin masks if not already loaded
        if (!masksLoadAttempted) {
            masksLoadAttempted = true;
            maskSteve = loadMask(MASK_STEVE);
            maskAlex = loadMask(MASK_ALEX);
            LOGGER.info("[TATTOO] Loaded skin masks - Steve: {}, Alex: {}",
                maskSteve != null, maskAlex != null);
        }

        // Get the original skin texture as a NativeImage
        Identifier bodyTextureId = original.body().texturePath();
        NativeImage baseSkin = loadTextureAsImage(bodyTextureId);
        if (baseSkin == null) {
            LOGGER.warn("[TATTOO] Could not load base skin texture: {}", bodyTextureId);
            return null;
        }

        // Select mask based on skin model type (slim = Alex, wide = Steve)
        NativeImage mask = (original.model() == PlayerSkinType.SLIM) ? maskAlex : maskSteve;

        // Composite all active tattoos onto the skin
        NativeImage compositedSkin = baseSkin;
        for (String tattooId : activeTattoos) {
            NativeImage tattooImage = getTattooImage(tattooId);
            if (tattooImage != null) {
                NativeImage newComposite = compositeTattoo(compositedSkin, tattooImage, mask);
                if (compositedSkin != baseSkin) {
                    compositedSkin.close(); // Close intermediate images
                }
                compositedSkin = newComposite;
            }
        }

        if (compositedSkin == baseSkin) {
            // No tattoos were actually composited
            baseSkin.close();
            return null;
        }

        // Register the composited texture
        // The system looks for "textures/<path>.png", so register with that full path
        String playerHash = entry.getProfile().id().toString().replace("-", "");
        Identifier registrationId = Identifier.of(Aerb.MOD_ID, "textures/tattoo_skin/" + playerHash + ".png");

        // Create and register the texture with the full path including .png
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> registrationId.toString(), compositedSkin);
        client.getTextureManager().registerTexture(registrationId, texture);

        LOGGER.info("[TATTOO] Created modified skin texture with {} tattoos: {}", activeTattoos.size(), registrationId);

        // Return new SkinTextures with our modified texture
        // SkinTextures(body, cape, elytra, model, secure)
        // Create a TextureAssetInfo - pass the path that when "textures/" and ".png" are added matches our registration
        Identifier assetId = Identifier.of(Aerb.MOD_ID, "tattoo_skin/" + playerHash);
        AssetInfo.TextureAssetInfo modifiedBodyAsset = new AssetInfo.TextureAssetInfo(assetId);
        return new SkinTextures(
                modifiedBodyAsset,
                original.cape(),
                original.elytra(),
                original.model(),
                original.secure()
        );
    }

    /**
     * Get a tattoo image by ID, loading and caching it if needed.
     */
    private static NativeImage getTattooImage(String tattooId) {
        // Check cache first
        NativeImage cached = tattooImages.get(tattooId);
        if (cached != null) {
            return cached;
        }

        // Look up the texture path for this tattoo
        Identifier textureId = TATTOO_TEXTURES.get(tattooId);
        if (textureId == null) {
            LOGGER.warn("[TATTOO] No texture defined for tattoo: {}", tattooId);
            return null;
        }

        // Load the texture
        NativeImage image = loadTattooTexture(textureId);
        if (image != null) {
            tattooImages.put(tattooId, image);
            LOGGER.info("[TATTOO] Loaded tattoo texture: {} -> {}", tattooId, textureId);
        }
        return image;
    }

    /**
     * Load a tattoo texture from resources.
     */
    private static NativeImage loadTattooTexture(Identifier textureId) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Optional<Resource> resource = client.getResourceManager().getResource(textureId);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    return NativeImage.read(stream);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[TATTOO] Error loading tattoo texture {}: {}", textureId, e.getMessage());
        }
        return null;
    }

    /**
     * Load a texture from the resource manager or texture manager as a NativeImage.
     */
    private static NativeImage loadTextureAsImage(Identifier textureId) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            LOGGER.info("[TATTOO] Attempting to load texture: {}", textureId);

            // Try to get from resource manager first (for mod textures)
            Optional<Resource> resource = client.getResourceManager().getResource(textureId);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    LOGGER.info("[TATTOO] Loaded from resource manager");
                    return NativeImage.read(stream);
                }
            }

            // For player skins (downloaded textures), try to get from texture manager
            var texture = client.getTextureManager().getTexture(textureId);
            LOGGER.info("[TATTOO] Texture from manager: {} (type: {})",
                texture != null ? "found" : "null",
                texture != null ? texture.getClass().getSimpleName() : "N/A");

            if (texture instanceof NativeImageBackedTexture nativeTexture) {
                NativeImage original = nativeTexture.getImage();
                if (original != null) {
                    LOGGER.info("[TATTOO] Got NativeImage from texture, size: {}x{}",
                        original.getWidth(), original.getHeight());
                    // Copy the image since we don't want to modify the original
                    NativeImage copy = new NativeImage(original.getWidth(), original.getHeight(), true);
                    copy.copyFrom(original);
                    return copy;
                }
            }

            // For other texture types, we may need to read from GPU
            // For now, create a blank 64x64 skin if we can't load it
            LOGGER.warn("[TATTOO] Could not load texture {}, using blank", textureId);
            return new NativeImage(64, 64, true);

        } catch (Exception e) {
            LOGGER.error("[TATTOO] Error loading texture {}: {}", textureId, e.getMessage());
            return null;
        }
    }

    /**
     * Load a skin mask texture.
     */
    private static NativeImage loadMask(Identifier maskId) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Optional<Resource> resource = client.getResourceManager().getResource(maskId);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().getInputStream()) {
                    return NativeImage.read(stream);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[TATTOO] Error loading mask {}: {}", maskId, e.getMessage());
        }
        return null;
    }

    /**
     * Composite the tattoo overlay onto the base skin, using mask to limit to exposed areas.
     * Returns a new NativeImage with the result.
     * @param mask If non-null, white (255) = show tattoo, black (0) = hide tattoo
     */
    private static NativeImage compositeTattoo(NativeImage baseSkin, NativeImage tattoo, NativeImage mask) {
        int width = baseSkin.getWidth();
        int height = baseSkin.getHeight();

        // Create output image
        NativeImage result = new NativeImage(width, height, true);

        // Copy base skin
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setColorArgb(x, y, baseSkin.getColorArgb(x, y));
            }
        }

        // Overlay tattoo (alpha blend) with masking
        int tattooWidth = Math.min(width, tattoo.getWidth());
        int tattooHeight = Math.min(height, tattoo.getHeight());

        for (int y = 0; y < tattooHeight; y++) {
            for (int x = 0; x < tattooWidth; x++) {
                // Check mask - if mask exists and pixel is dark, skip this pixel
                if (mask != null && x < mask.getWidth() && y < mask.getHeight()) {
                    int maskValue = mask.getColorArgb(x, y) & 0xFF; // Get blue channel (grayscale)
                    if (maskValue < 128) {
                        // Masked out (clothed area) - don't apply tattoo here
                        continue;
                    }
                }

                int tattooColor = tattoo.getColorArgb(x, y);
                int tattooAlpha = (tattooColor >> 24) & 0xFF;

                if (tattooAlpha > 0) {
                    int baseColor = result.getColorArgb(x, y);

                    // Alpha blend
                    int blended = alphaBlend(baseColor, tattooColor);
                    result.setColorArgb(x, y, blended);
                }
            }
        }

        return result;
    }

    /**
     * Alpha blend two ARGB colors (tattoo over base).
     */
    private static int alphaBlend(int base, int overlay) {
        int oA = (overlay >> 24) & 0xFF;
        int oR = (overlay >> 16) & 0xFF;
        int oG = (overlay >> 8) & 0xFF;
        int oB = overlay & 0xFF;

        int bA = (base >> 24) & 0xFF;
        int bR = (base >> 16) & 0xFF;
        int bG = (base >> 8) & 0xFF;
        int bB = base & 0xFF;

        float alpha = oA / 255.0f;
        float invAlpha = 1.0f - alpha;

        int rR = (int) (oR * alpha + bR * invAlpha);
        int rG = (int) (oG * alpha + bG * invAlpha);
        int rB = (int) (oB * alpha + bB * invAlpha);
        int rA = Math.max(oA, bA);

        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }

    /**
     * Invalidate the cached tattoo skin for a player (call when tattoos change).
     */
    public static void invalidateCache(UUID playerId) {
        CachedTattooSkin removed = skinCache.remove(playerId);
        if (removed != null) {
            // Could also unregister the texture here if needed
            LOGGER.info("[TATTOO] Invalidated cache for player {}", playerId);
        }
    }

    /**
     * Clear all cached tattoo skins.
     */
    public static void clearCache() {
        skinCache.clear();
        LOGGER.info("[TATTOO] Cleared all tattoo skin cache");
    }

    /**
     * Cache entry for a player's tattooed skin.
     * Includes tattoosHash to invalidate cache when the set of active tattoos changes.
     */
    private record CachedTattooSkin(Identifier originalTexture, SkinTextures modifiedTextures, int tattoosHash) {
    }
}
