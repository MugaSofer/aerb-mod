package mugasofer.aerb.render;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.item.TattooDesignItem;
import mugasofer.aerb.tattoo.ClientTattooCache;
import mugasofer.aerb.tattoo.PlayerTattoos;
import mugasofer.aerb.tattoo.TattooInstance;
import net.fabricmc.loader.api.FabricLoader;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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

    // Mapping of tattoo ID to grid size (width, height in grid cells)
    // This should match the sizes defined in ModItems
    private static final Map<String, int[]> TATTOO_SIZES = Map.of(
        PlayerTattoos.FALL_RUNE, new int[]{2, 2},   // 2x2 grid cells = 8x8 pixels
        PlayerTattoos.ICY_DEVIL, new int[]{3, 3}    // 3x3 grid cells = 12x12 pixels
    );

    // Skin masks - define which areas are exposed skin vs clothed
    private static final Identifier MASK_STEVE = Identifier.of(Aerb.MOD_ID, "textures/entity/skin_mask_steve.png");
    private static final Identifier MASK_ALEX = Identifier.of(Aerb.MOD_ID, "textures/entity/skin_mask_alex.png");

    // Cache of modified skin textures per player UUID
    // Key includes a hash of active tattoo IDs so cache invalidates when tattoos change
    private static final Map<UUID, CachedTattooSkin> skinCache = new HashMap<>();

    // Cached tattoo overlay images (loaded on demand)
    private static final Map<String, NativeImage> tattooImages = new HashMap<>();

    // Cached mask images (default Steve/Alex)
    private static NativeImage maskSteve = null;
    private static NativeImage maskAlex = null;
    private static boolean defaultMasksLoadAttempted = false;

    // Cached custom masks per player name (loaded from config folder)
    private static final Map<String, NativeImage> customMasks = new HashMap<>();
    private static final Set<String> customMaskLoadAttempted = new java.util.HashSet<>();

    // Path to custom skin masks folder
    private static Path getCustomMasksFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve("aerb").resolve("skin_masks");
    }

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

        // Get all tattoo instances from client cache
        List<TattooInstance> tattoos = ClientTattooCache.getAllTattoos();
        if (tattoos.isEmpty()) {
            // No tattoos - return original and clear any cached version
            skinCache.remove(playerId);
            return original;
        }

        // Create a cache key that includes the tattoos and their positions
        // This ensures we regenerate when tattoos change or move
        int tattoosHash = computeTattoosHash(tattoos);

        // Check cache - use body().texturePath() to get the skin texture identifier
        CachedTattooSkin cached = skinCache.get(playerId);
        Identifier originalBodyTexture = original.body().texturePath();
        if (cached != null && cached.originalTexture.equals(originalBodyTexture) && cached.tattoosHash == tattoosHash) {
            // Cache hit - return cached modified textures
            return cached.modifiedTextures;
        }

        // Need to create/update the modified texture
        try {
            SkinTextures modified = createModifiedSkinTextures(entry, original, tattoos);
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
     * Compute a hash that includes all tattoo instances and their positions.
     */
    private static int computeTattoosHash(List<TattooInstance> tattoos) {
        int hash = 0;
        for (TattooInstance instance : tattoos) {
            hash = 31 * hash + instance.tattooId().hashCode();
            hash = 31 * hash + instance.gridX();
            hash = 31 * hash + instance.gridY();
        }
        return hash;
    }

    /**
     * Creates a new SkinTextures with tattoos composited onto the skin.
     */
    private static SkinTextures createModifiedSkinTextures(PlayerListEntry entry, SkinTextures original, List<TattooInstance> tattoos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getTextureManager() == null) {
            return null;
        }

        // Load default skin masks if not already loaded
        if (!defaultMasksLoadAttempted) {
            defaultMasksLoadAttempted = true;
            maskSteve = loadMask(MASK_STEVE);
            maskAlex = loadMask(MASK_ALEX);
            LOGGER.info("[TATTOO] Loaded default skin masks - Steve: {}, Alex: {}",
                maskSteve != null, maskAlex != null);
        }

        // Get the original skin texture as a NativeImage
        Identifier bodyTextureId = original.body().texturePath();
        NativeImage baseSkin = loadTextureAsImage(bodyTextureId);
        if (baseSkin == null) {
            LOGGER.warn("[TATTOO] Could not load base skin texture: {}", bodyTextureId);
            return null;
        }

        // Try to load custom mask for this player, fall back to Steve/Alex
        String playerName = entry.getProfile().name();
        NativeImage mask = getCustomMask(playerName);
        if (mask == null) {
            // Fall back to default mask based on skin model type (slim = Alex, wide = Steve)
            mask = (original.model() == PlayerSkinType.SLIM) ? maskAlex : maskSteve;
        }

        // Composite all tattoos onto the skin at their grid positions
        NativeImage compositedSkin = baseSkin;
        for (TattooInstance instance : tattoos) {
            NativeImage tattooImage = getTattooImage(instance.tattooId());
            if (tattooImage != null) {
                int[] size = TATTOO_SIZES.getOrDefault(instance.tattooId(), new int[]{2, 2});
                NativeImage newComposite = compositeTattooAtGridPosition(
                    compositedSkin, tattooImage, mask,
                    instance.gridX(), instance.gridY(),
                    size[0], size[1]
                );
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

        LOGGER.info("[TATTOO] Created modified skin texture with {} tattoos: {}", tattoos.size(), registrationId);

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
     * Load a skin mask texture from resources.
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
     * Get a custom mask for a player from the config folder.
     * Looks for: config/aerb/skin_masks/<playername>.png (case-insensitive)
     * Returns null if no custom mask exists.
     */
    private static NativeImage getCustomMask(String playerName) {
        String lowerName = playerName.toLowerCase();

        // Check cache first
        if (customMasks.containsKey(lowerName)) {
            return customMasks.get(lowerName); // May be null if we tried and failed
        }

        // Only attempt to load once per player name
        if (customMaskLoadAttempted.contains(lowerName)) {
            return null;
        }
        customMaskLoadAttempted.add(lowerName);

        // Try to load from config folder
        Path masksFolder = getCustomMasksFolder();
        if (!Files.exists(masksFolder)) {
            // Create the folder so users know where to put masks
            try {
                Files.createDirectories(masksFolder);
                LOGGER.info("[TATTOO] Created skin masks folder: {}", masksFolder);
            } catch (Exception e) {
                LOGGER.warn("[TATTOO] Could not create skin masks folder: {}", e.getMessage());
            }
            return null;
        }

        // Look for mask file (try exact name first, then lowercase)
        Path maskPath = masksFolder.resolve(playerName + ".png");
        if (!Files.exists(maskPath)) {
            maskPath = masksFolder.resolve(lowerName + ".png");
        }

        if (Files.exists(maskPath)) {
            try (InputStream stream = Files.newInputStream(maskPath)) {
                NativeImage mask = NativeImage.read(stream);
                customMasks.put(lowerName, mask);
                LOGGER.info("[TATTOO] Loaded custom mask for {}: {}", playerName, maskPath);
                return mask;
            } catch (Exception e) {
                LOGGER.error("[TATTOO] Error loading custom mask for {}: {}", playerName, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Composite a tattoo at a specific grid position.
     * The tattoo image is scaled/placed to fit the target UV region.
     *
     * @param baseSkin Base skin image to composite onto
     * @param tattoo Tattoo image to apply
     * @param mask Skin mask (white = exposed skin, black = clothed)
     * @param gridX Grid X position (0-15)
     * @param gridY Grid Y position (0-15)
     * @param gridWidth Tattoo width in grid cells
     * @param gridHeight Tattoo height in grid cells
     */
    private static NativeImage compositeTattooAtGridPosition(
            NativeImage baseSkin, NativeImage tattoo, NativeImage mask,
            int gridX, int gridY, int gridWidth, int gridHeight) {
        int width = baseSkin.getWidth();
        int height = baseSkin.getHeight();

        // Create output image as copy of base
        NativeImage result = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setColorArgb(x, y, baseSkin.getColorArgb(x, y));
            }
        }

        // Get UV region for this grid position
        SkinUVMap.UVRegion region = SkinUVMap.getRegionForGrid(gridX, gridY, gridWidth, gridHeight);

        // Apply tattoo to the region
        applyTattooToRegion(result, tattoo, mask, region);

        return result;
    }

    /**
     * Apply a tattoo image to a specific UV region on the skin.
     * Scales the tattoo to fit the region.
     */
    private static void applyTattooToRegion(NativeImage skin, NativeImage tattoo, NativeImage mask, SkinUVMap.UVRegion region) {
        int tattooW = tattoo.getWidth();
        int tattooH = tattoo.getHeight();

        for (int dy = 0; dy < region.height(); dy++) {
            for (int dx = 0; dx < region.width(); dx++) {
                int skinX = region.x() + dx;
                int skinY = region.y() + dy;

                // Check mask at this skin position
                if (mask != null && skinX < mask.getWidth() && skinY < mask.getHeight()) {
                    int maskValue = mask.getColorArgb(skinX, skinY) & 0xFF;
                    if (maskValue < 128) {
                        // Masked out - don't apply tattoo here
                        continue;
                    }
                }

                // Sample from tattoo using nearest-neighbor scaling
                int tattooX = dx * tattooW / region.width();
                int tattooY = dy * tattooH / region.height();

                if (tattooX < tattooW && tattooY < tattooH) {
                    int tattooColor = tattoo.getColorArgb(tattooX, tattooY);
                    int tattooAlpha = (tattooColor >> 24) & 0xFF;

                    if (tattooAlpha > 0) {
                        int baseColor = skin.getColorArgb(skinX, skinY);
                        int blended = alphaBlend(baseColor, tattooColor);
                        skin.setColorArgb(skinX, skinY, blended);
                    }
                }
            }
        }
    }

    /**
     * Composite the tattoo overlay onto the base skin, using mask to limit to exposed areas.
     * Returns a new NativeImage with the result.
     * @param mask If non-null, white (255) = show tattoo, black (0) = hide tattoo
     * @deprecated Use compositeTattooAtPosition for position-based placement
     */
    @Deprecated
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
