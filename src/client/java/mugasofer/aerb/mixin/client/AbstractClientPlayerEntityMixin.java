package mugasofer.aerb.mixin.client;

import mugasofer.aerb.render.TattooTextureManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept player skin texture lookup and return a modified
 * texture with tattoos composited onto it.
 */
@Mixin(PlayerListEntry.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void modifySkinWithTattoos(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures original = cir.getReturnValue();
        PlayerListEntry entry = (PlayerListEntry) (Object) this;

        // Get modified skin textures with tattoos applied
        SkinTextures modified = TattooTextureManager.getModifiedSkinTextures(entry, original);
        if (modified != original) {
            cir.setReturnValue(modified);
        }
    }
}
