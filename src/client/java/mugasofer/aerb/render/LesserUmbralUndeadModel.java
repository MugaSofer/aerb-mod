package mugasofer.aerb.render;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Quadrupedal model for Lesser Umbral Undead.
 * A bulky, monstrous creature composed of many corpses.
 * Body structure similar to a cow but more massive and grotesque.
 */
public class LesserUmbralUndeadModel extends EntityModel<LesserUmbralUndeadRenderState> {
    // Model parts for animation
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;

    public LesserUmbralUndeadModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Model for ~3 block tall creature
        // In Minecraft model coords: Y=0 is feet, negative Y is up
        // 16 pixels = 1 block

        // Legs - pivot at hip (top of leg), extend down to ground
        // Hip at Y=0, legs extend to Y=24 (ground level for this entity)
        root.addChild("left_front_leg",
            ModelPartBuilder.create()
                .uv(112, 0)
                .cuboid(-5F, 0F, -5F, 10F, 24F, 10F),
            ModelTransform.origin(-10F, 0F, -12F));

        root.addChild("right_front_leg",
            ModelPartBuilder.create()
                .uv(112, 0)
                .cuboid(-5F, 0F, -5F, 10F, 24F, 10F),
            ModelTransform.origin(10F, 0F, -12F));

        root.addChild("left_hind_leg",
            ModelPartBuilder.create()
                .uv(112, 34)
                .cuboid(-5F, 0F, -5F, 10F, 24F, 10F),
            ModelTransform.origin(-10F, 0F, 14F));

        root.addChild("right_hind_leg",
            ModelPartBuilder.create()
                .uv(112, 34)
                .cuboid(-5F, 0F, -5F, 10F, 24F, 10F),
            ModelTransform.origin(10F, 0F, 14F));

        // Body - massive torso sitting on top of legs
        // Body bottom at Y=0 (hip), extends up (negative Y)
        root.addChild("body",
            ModelPartBuilder.create()
                .uv(0, 0)
                .cuboid(-16F, -20F, -20F, 32F, 20F, 40F),
            ModelTransform.origin(0F, 0F, 0F));

        // Head - attached at front of body
        root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 60)
                .cuboid(-8F, -14F, -10F, 16F, 14F, 12F),
            ModelTransform.origin(0F, -10F, -22F));

        // 256x128 texture for the larger model
        return TexturedModelData.of(modelData, 256, 128);
    }

    @Override
    public void setAngles(LesserUmbralUndeadRenderState state) {
        // Reset transforms first
        super.setAngles(state);

        // Head follows look direction
        this.head.yaw = state.relativeHeadYaw * 0.017453292F * 0.5F;
        this.head.pitch = state.pitch * 0.017453292F;

        // Walking animation - legs swing
        float limbAngle = state.limbSwingAnimationProgress;
        float limbDistance = state.limbSwingAmplitude;

        // Front legs swing opposite to hind legs (like a trot)
        this.leftFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        this.rightFrontLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;
        this.leftHindLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;
        this.rightHindLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
    }
}
