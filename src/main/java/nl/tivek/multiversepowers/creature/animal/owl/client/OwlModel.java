package nl.tivek.multiversepowers.creature.animal.owl.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;

// The owl's body: an upright trunk with fluffed feathers over the tops of its short legs, broad feet with dark talons,
// a big round head wider than its body with a flat face, a small hooked beak and lids that slide down over its eyes,
// a short tail with a ragged end, and broad wings in two pieces each, their flight feathers ragged at the edge: folded
// they lie along its sides, their tips crossing behind it; spread, the outer half unfurls and they beat in slow, deep
// strokes while the body lies forward into its flight and the head stays level. Its texture is painted by
// scripts/models/animals. OwlPoses moves it.
public final class OwlModel extends HierarchicalModel<Owl> {
    static final float BODY_Y = 19.0F;
    // How far back a folded wing lies along the body.
    private static final float FOLD = 1.45F;
    private static final float TAIL_REST = 0.45F;
    private static final float YOUNG_HEAD = 1.2F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart[] lids = new ModelPart[2];
    // Right, left: the inner half and the outer.
    private final ModelPart[][] wings = new ModelPart[2][2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final OwlPoses poses = new OwlPoses();

    public OwlModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "right_" : "left_";
            this.lids[s] = this.head.getChild(name + "lid");
            ModelPart wing = this.body.getChild(name + "wing");
            this.wings[s] = new ModelPart[] { wing, wing.getChild("outer") };
            this.legs[s] = this.body.getChild(name + "leg");
            this.feet[s] = this.legs[s].getChild("foot");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -3.5F, -2.0F, 5.0F, 7.0F, 4.0F)
                .texOffs(36, 12).addBox(-2.0F, 3.0F, -1.5F, 4.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(18, 0).addBox(-3.5F, -5.0F, -2.5F, 7.0F, 5.0F, 5.0F)
                .texOffs(42, 0).addBox(-0.5F, -3.0F, -3.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -3.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(52, 0).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 2.0F, TAIL_REST, 0.0F, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            // A lid hangs from the top of its eye and slides down over it as it is drawn longer.
            head.addOrReplaceChild(name + "lid", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(46, 0).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.5F, -4.0F, -2.2F));
            float from = s == 0 ? -5.0F : 0.0F;
            PartDefinition wing = body.addOrReplaceChild(name + "wing", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 12).addBox(from, 0.0F, -0.5F, 5.0F, 6.0F, 1.0F),
                    PartPose.offset(side * 2.5F, -3.0F, -1.5F));
            wing.addOrReplaceChild("outer", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(12, 12).addBox(from, 0.0F, -0.5F, 5.0F, 6.0F, 1.0F),
                    PartPose.offset(side * 5.0F, 0.0F, 0.0F));
            PartDefinition leg = body.addOrReplaceChild(name + "leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(24, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.2F, 3.0F, 0.0F));
            leg.addOrReplaceChild("foot", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(28, 12).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 2.0F)
                    .texOffs(36, 16).addBox(-1.0F, 0.0F, -2.5F, 2.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 1.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Owl owl, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - owl.tickCount;
        float[] c = this.poses.of(owl, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float spread = c[OwlPoses.SPREAD];
        float pitch = c[OwlPoses.PITCH];
        float puff = 1.0F + 0.14F * c[OwlPoses.PUFF];
        this.body.y = BODY_Y + c[OwlPoses.BOB];
        this.body.xRot = pitch;
        this.body.zRot = c[OwlPoses.ROLL];
        this.body.xScale = this.body.zScale = puff;
        float look = c[OwlPoses.LOOK];
        float young = this.young ? YOUNG_HEAD : 1.0F;
        // The head stays level whatever the body does.
        this.head.xRot = c[OwlPoses.HEAD] - pitch + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[OwlPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.head.zRot = c[OwlPoses.HEAD_ROLL];
        this.head.y += c[OwlPoses.HEAD_SINK];
        this.head.x += c[OwlPoses.PEER];
        this.head.xScale = this.head.zScale = young / puff;
        this.head.yScale = young;
        float shut = Mth.clamp(c[OwlPoses.LIDS], 0.0F, 1.0F);
        for (ModelPart lid : this.lids) {
            lid.visible = shut > 0.05F;
            lid.yScale = shut;
        }
        this.tail.xRot = TAIL_REST * (1.0F - spread) + c[OwlPoses.TAIL];
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1.0F : -1.0F;
            // Folded along the side, or out and beating.
            this.wings[s][0].yRot = side * (Mth.lerp(spread, FOLD, c[OwlPoses.BEAT]) + c[OwlPoses.WING_R + s]);
            this.wings[s][0].zRot = side * 0.12F * spread;
            this.wings[s][1].yRot = side * c[OwlPoses.BEAT_OUTER] * spread;
            this.wings[s][1].xScale = spread;
            this.wings[s][1].visible = spread > 0.02F;
            this.legs[s].xRot = c[OwlPoses.LEG_R + s] - pitch * (1.0F - spread) + 0.5F * spread;
            this.feet[s].xRot = c[OwlPoses.FOOT_R + s] + 0.6F * spread;
        }
    }
}
