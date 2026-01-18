package mugasofer.aerb.mixin.client;

import mugasofer.aerb.combat.ParryAnimationState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to skip the equip animation when doing a Prophetic Blade instant weapon switch.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Shadow
    private ItemStack mainHand;

    @Shadow
    private float equipProgressMainHand;

    /**
     * Intercept the updateHeldItems method to skip equip animation when requested.
     * Inject at TAIL to override whatever the method set.
     */
    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void onUpdateHeldItems(CallbackInfo ci) {
        if (ParryAnimationState.shouldSkipEquipAnimation()) {
            // Get the player's current main hand item
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                // Update cached item to current, so no change is detected on next frame
                this.mainHand = player.getMainHandStack().copy();
                // Force equip progress to full to skip animation
                this.equipProgressMainHand = 1.0f;
            }
        }
    }
}
