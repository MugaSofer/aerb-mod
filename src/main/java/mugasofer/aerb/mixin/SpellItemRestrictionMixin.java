package mugasofer.aerb.mixin;

import mugasofer.aerb.item.SpellItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent spell items from being dropped.
 * Targets Entity.dropStack which is called by all drop paths.
 */
@Mixin(Entity.class)
public class SpellItemRestrictionMixin {

    /**
     * Prevent dropping spell items by intercepting dropStack.
     */
    @Inject(method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void preventSpellDrop(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
        if (SpellItem.isSpell(stack)) {
            cir.setReturnValue(null);
        }
    }
}
