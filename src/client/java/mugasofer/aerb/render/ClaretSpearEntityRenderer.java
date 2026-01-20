package mugasofer.aerb.render;

import mugasofer.aerb.entity.ClaretSpearEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

/**
 * Custom renderer for Claret Spear that extends FlyingItemEntityRenderer.
 *
 * In 1.21.11, the rendering pipeline uses OrderedRenderCommandQueue instead of
 * VertexConsumerProvider, and the ItemRenderState doesn't expose rotation controls.
 * Adding projectile rotation would require deeper API investigation or a mixin.
 *
 * For now, this uses the parent's rendering which shows the item correctly.
 */
public class ClaretSpearEntityRenderer extends FlyingItemEntityRenderer<ClaretSpearEntity> {

    public ClaretSpearEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
}
