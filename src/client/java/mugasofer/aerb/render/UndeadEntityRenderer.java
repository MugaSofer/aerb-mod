package mugasofer.aerb.render;

import mugasofer.aerb.Aerb;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Renderer for Undead entities - uses zombie model with custom texture.
 *
 * Note: True emissive eyes (like spiders) require the EYES render layer, which was
 * restructured in 1.21.11. For now, use bright red pixels in the base texture.
 * For actual glow effect, use Entity Texture Features mod with undead_e.png overlay.
 */
public class UndeadEntityRenderer extends ZombieEntityRenderer {
    private static final Identifier TEXTURE = Identifier.of(Aerb.MOD_ID, "textures/entity/undead.png");

    public UndeadEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        // TODO: Emissive eyes - the EYES render layer was restructured in 1.21.11
        // For now, use bright red pixels in texture. For true glow, use ETF mod.
    }

    @Override
    public Identifier getTexture(ZombieEntityRenderState state) {
        return TEXTURE;
    }
}
