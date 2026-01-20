package mugasofer.aerb.render;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.AerbClient;
import mugasofer.aerb.entity.LesserUmbralUndeadEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Renderer for Lesser Umbral Undead with dynamic scaling based on corpse count.
 * Uses custom quadrupedal model.
 */
public class LesserUmbralUndeadRenderer extends MobEntityRenderer<LesserUmbralUndeadEntity, LesserUmbralUndeadRenderState, LesserUmbralUndeadModel> {
    private static final Identifier TEXTURE = Identifier.of(Aerb.MOD_ID, "textures/entity/lesser_umbral_undead.png");

    public LesserUmbralUndeadRenderer(EntityRendererFactory.Context context) {
        super(context, new LesserUmbralUndeadModel(context.getPart(AerbClient.LESSER_UMBRAL_UNDEAD_LAYER)), 1.5f);
    }

    @Override
    public LesserUmbralUndeadRenderState createRenderState() {
        return new LesserUmbralUndeadRenderState();
    }

    @Override
    public void updateRenderState(LesserUmbralUndeadEntity entity, LesserUmbralUndeadRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.scale = entity.calculateScale();
        state.corpseCount = entity.getCorpseCount();
        state.rearingUp = entity.isRearingUp();
    }

    @Override
    protected void scale(LesserUmbralUndeadRenderState state, MatrixStack matrices) {
        float scale = state.scale;
        matrices.scale(scale, scale, scale);

        // Also scale the shadow
        this.shadowRadius = 1.5f * scale;
    }

    @Override
    public Identifier getTexture(LesserUmbralUndeadRenderState state) {
        return TEXTURE;
    }
}
