package nl.tivek.multiversepowers.creature.animal.raccoon.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;

// The raccoon's body in a raccoon's own build: hunched, its rump higher than its shoulders on longer hind legs, a broad
// masked head with fluffy cheeks, a short pointed snout with a black nose, eyes in the mask that blink and round ears,
// legs with an elbow or knee and a wrist or ankle each: short forelegs ending in hand-like paws with long fingers, flat
// hind feet with a heel, and a bushy tail in four pieces that sways and curls. Its texture is painted by
// scripts/models/animals. RaccoonPoses moves it.
public final class RaccoonModel extends HierarchicalModel<Raccoon> {
    static final float BODY_Y = 17.0F;
    // The hips in the body's own frame, through the rump.
    static final float HIP_Y = 1.5F;
    static final float HIP_Z = 3.5F;
    private static final float TAIL_REST = -0.5F;
    private static final float YOUNG_HEAD = 1.35F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart hind;
    private final ModelPart head;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[4];
    // Right front, left front, right hind, left hind: each its upper bone, its middle one and its paw.
    private final ModelPart[][] legs = new ModelPart[4][3];
    private final RaccoonPoses poses = new RaccoonPoses();

    public RaccoonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.hind = this.body.getChild("hind");
        this.head = this.body.getChild("head");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        this.tail[0] = this.hind.getChild("tail");
        this.tail[1] = this.tail[0].getChild("middle");
        this.tail[2] = this.tail[1].getChild("outer");
        this.tail[3] = this.tail[2].getChild("tip");
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = (k < 2 ? this.body : this.hind).getChild(names[k]);
            ModelPart middle = upper.getChild("middle");
            this.legs[k] = new ModelPart[] { upper, middle, middle.getChild("paw") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 5.0F)
                .texOffs(36, 12).addBox(-2.5F, -3.5F, -4.5F, 5.0F, 1.0F, 4.0F)
                .texOffs(0, 20).addBox(-2.5F, 2.5F, -4.5F, 5.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition hind = body.addOrReplaceChild("hind", CubeListBuilder.create()
                .texOffs(22, 0).addBox(-3.5F, -3.5F, 0.0F, 7.0F, 6.0F, 6.0F)
                .texOffs(18, 20).addBox(-2.5F, -4.5F, 1.0F, 5.0F, 1.0F, 4.0F), PartPose.ZERO);
        PartDefinition tail = hind.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(36, 20).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, 6.0F));
        PartDefinition middle = tail.addOrReplaceChild("middle", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition outer = middle.addOrReplaceChild("outer", CubeListBuilder.create()
                .texOffs(48, 20).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        outer.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(54, 28).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2.5F, -2.5F, -3.5F, 5.0F, 4.0F, 4.0F)
                .texOffs(18, 12).addBox(-3.5F, -0.5F, -2.5F, 7.0F, 2.0F, 2.0F)
                .texOffs(18, 16).addBox(-1.5F, -0.5F, -5.5F, 3.0F, 2.0F, 2.0F)
                .texOffs(54, 7).addBox(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -0.5F, -5.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(48, 7).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.8F, -2.5F, -1.5F));
            // It shuts by folding down to nothing, as an upper lid comes down over it.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(58, 7).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.0F, -0.5F, -3.1F));
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(12, 26).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(side * 1.8F, 2.0F, -3.5F));
            PartDefinition forearm = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(20, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 2.0F, 0.0F));
            forearm.addOrReplaceChild("paw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(46, 26).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 2.0F)
                    .texOffs(54, 26).addBox(-1.0F, 0.0F, -2.5F, 2.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 2.0F, 0.0F));
            PartDefinition thigh = hind.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 26).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 4.0F, 3.0F),
                    PartPose.offset(side * 2.2F, HIP_Y, HIP_Z));
            PartDefinition shin = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(28, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 2.5F, 0.0F));
            shin.addOrReplaceChild("paw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(36, 26).addBox(-1.0F, 0.0F, -2.5F, 2.0F, 1.0F, 3.0F),
                    PartPose.offset(0.0F, 2.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Raccoon raccoon, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - raccoon.tickCount;
        float[] c = this.poses.of(raccoon, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[RaccoonPoses.PITCH];
        float flex = c[RaccoonPoses.FLEX];
        float roll = c[RaccoonPoses.ROLL];
        // The body turns about its shoulders, then comes back so its hips stay where its hind legs stand.
        float hy = HIP_Y * Mth.cos(flex) - HIP_Z * Mth.sin(flex);
        float hz = HIP_Y * Mth.sin(flex) + HIP_Z * Mth.cos(flex);
        float wy = hy * Mth.cos(pitch) - hz * Mth.sin(pitch);
        float wz = hy * Mth.sin(pitch) + hz * Mth.cos(pitch);
        this.body.y = BODY_Y + c[RaccoonPoses.BOB] + (HIP_Y + c[RaccoonPoses.SINK] - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = roll;
        this.hind.xRot = flex;
        this.hind.zRot = c[RaccoonPoses.HIND_ROLL];
        float look = c[RaccoonPoses.LOOK];
        this.head.xRot = c[RaccoonPoses.HEAD] - pitch + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[RaccoonPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.head.zRot = c[RaccoonPoses.HEAD_ROLL];
        this.ears[0].zRot = -c[RaccoonPoses.EARS];
        this.ears[1].zRot = c[RaccoonPoses.EARS];
        this.tail[0].xRot = TAIL_REST + c[RaccoonPoses.TAIL];
        this.tail[0].yRot = c[RaccoonPoses.TAIL_SIDE];
        this.tail[1].xRot = -0.15F + c[RaccoonPoses.TAIL_CURL];
        this.tail[1].yRot = c[RaccoonPoses.TAIL_SIDE] * 0.8F;
        this.tail[2].xRot = -0.1F + c[RaccoonPoses.TAIL_CURL];
        this.tail[2].yRot = c[RaccoonPoses.TAIL_SIDE] * 0.6F + c[RaccoonPoses.TAIL_WAVE];
        this.tail[3].xRot = -0.1F + c[RaccoonPoses.TAIL_CURL] * 0.8F;
        this.tail[3].yRot = c[RaccoonPoses.TAIL_SIDE] * 0.4F + c[RaccoonPoses.TAIL_WAVE] * 1.4F;
        float open = 1.0F - 0.9F * Mth.clamp(c[RaccoonPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = RaccoonPoses.LEGS + k * RaccoonPoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            boolean front = k < 2;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            leg[0].xRot = c[at] - pitch - (front ? 0.0F : flex);
            leg[0].zRot = side * c[at + 1] - (front ? roll : roll + c[RaccoonPoses.HIND_ROLL]) * 0.7F;
            leg[1].xRot = c[at + 2];
            leg[2].xRot = c[at + 3];
        }
    }
}
