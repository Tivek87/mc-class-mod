package nl.tivek.multiversepowers.creature.animal.bear.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;

// The brown bear's body in a bear's own build: a deep chest under the muscle hump over its shoulders, heavy shoulder
// muscles and a shaggy belly, a lower rump with big hams that bends from it, a broad head on a thick neck with a ruff
// at the throat, fur standing out at the cheeks, a high brow over a dished face, a nose bridge down to the nose, eyes
// that blink, round ears and a jaw with a chin of its own, and legs with an elbow or knee and a wrist or ankle each:
// a tuft at each elbow, a heel at each hock, broad paws, the front ones with long pale claws. Its texture is painted
// by scripts/models/animals. BearPoses moves it.
public final class BearModel extends HierarchicalModel<BrownBear> {
    static final float BODY_Y = 11.5F;
    static final float BODY_Z = -1.0F;
    // The hips in the body's own frame, through the rump.
    static final float HIND_Y = 0.5F;
    static final float HIP_Y = 1.5F;
    static final float HIP_Z = 8.0F;
    private static final float YOUNG_HEAD = 1.45F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart hind;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    // Right front, left front, right hind, left hind: each its upper bone, its middle one and its paw.
    private final ModelPart[][] legs = new ModelPart[4][3];
    private final BearPoses poses = new BearPoses();

    public BearModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.hind = this.body.getChild("hind");
        this.tail = this.hind.getChild("tail");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
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
                .texOffs(0, 0).addBox(-6.0F, -6.0F, -10.0F, 12.0F, 10.0F, 10.0F)
                .texOffs(0, 36).addBox(-4.5F, -8.5F, -9.0F, 9.0F, 3.0F, 7.0F)
                .texOffs(24, 52).addBox(-3.5F, -9.5F, -8.0F, 7.0F, 1.0F, 5.0F)
                .texOffs(28, 22).addBox(-5.0F, 3.5F, -9.0F, 10.0F, 2.0F, 9.0F)
                .texOffs(112, 36).addBox(-7.0F, -5.0F, -9.0F, 1.0F, 7.0F, 6.0F)
                .mirror().texOffs(112, 36).addBox(6.0F, -5.0F, -9.0F, 1.0F, 7.0F, 6.0F).mirror(false),
                PartPose.offset(0.0F, BODY_Y, BODY_Z));
        PartDefinition hind = body.addOrReplaceChild("hind", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-5.5F, -5.5F, 0.0F, 11.0F, 9.0F, 13.0F)
                .texOffs(66, 22).addBox(-4.5F, -6.5F, 1.0F, 9.0F, 1.0F, 10.0F)
                .texOffs(32, 36).addBox(-4.5F, 3.5F, 0.0F, 9.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, HIND_Y, 0.0F));
        hind.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(96, 59).addBox(-1.5F, -1.0F, -0.5F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -4.0F, 13.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 8.0F, 6.0F)
                .texOffs(0, 52).addBox(-3.5F, 3.0F, -4.5F, 7.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -2.0F, -10.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F)
                .texOffs(48, 52).addBox(-3.0F, -5.0F, -5.0F, 6.0F, 1.0F, 4.0F)
                .texOffs(68, 52).addBox(-2.5F, -0.5F, -10.0F, 5.0F, 3.0F, 4.0F)
                .texOffs(72, 59).addBox(-1.5F, -1.5F, -9.0F, 3.0F, 1.0F, 3.0F)
                .texOffs(0, 67).addBox(-1.5F, -1.0F, -10.6F, 3.0F, 2.0F, 1.0F)
                .texOffs(0, 59).addBox(-5.0F, -1.0F, -4.5F, 1.0F, 4.0F, 4.0F)
                .mirror().texOffs(0, 59).addBox(4.0F, -1.0F, -4.5F, 1.0F, 4.0F, 4.0F).mirror(false),
                PartPose.offset(0.0F, 0.5F, -4.5F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(42, 59).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 1.0F, 4.0F)
                .texOffs(84, 59).addBox(-1.5F, 1.0F, -3.5F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 2.5F, -6.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(116, 59).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                    PartPose.offset(side * 3.0F, -4.0F, -2.0F));
            // It shuts by folding down to nothing toward its bottom, as an upper lid comes down over it.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(8, 67).addBox(-0.5F, -1.0F, -0.6F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 2.5F, -1.0F, -5.5F));
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(92, 36).addBox(-2.5F, -3.0F, -2.5F, 5.0F, 9.0F, 5.0F)
                    .texOffs(106, 59).addBox(-2.0F, 2.0F, 2.5F, 4.0F, 3.0F, 1.0F),
                    PartPose.offset(side * 3.7F, 1.0F, -6.0F));
            PartDefinition forearm = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(10, 59).addBox(-2.0F, -0.5F, -2.0F, 4.0F, 4.0F, 4.0F),
                    PartPose.offset(0.0F, 6.5F, 0.0F));
            forearm.addOrReplaceChild("paw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(86, 52).addBox(-2.5F, -0.5F, -3.0F, 5.0F, 2.0F, 5.0F)
                    .texOffs(58, 59).addBox(-2.5F, 0.5F, -4.5F, 5.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 3.5F, 0.0F));
            PartDefinition thigh = hind.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(66, 36).addBox(-3.0F, -2.5F, -3.5F, 6.0F, 9.0F, 7.0F),
                    PartPose.offset(side * 3.6F, HIP_Y, HIP_Z));
            PartDefinition shin = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(26, 59).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                    .texOffs(12, 67).addBox(-1.5F, 1.0F, 1.5F, 3.0F, 2.0F, 1.0F),
                    PartPose.offset(0.0F, 6.5F, 0.0F));
            shin.addOrReplaceChild("paw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(106, 52).addBox(-2.5F, 0.0F, -4.0F, 5.0F, 1.0F, 6.0F)
                    .texOffs(20, 67).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 3.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(BrownBear bear, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - bear.tickCount;
        float[] c = this.poses.of(bear, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[BearPoses.PITCH];
        float flex = c[BearPoses.FLEX];
        float roll = c[BearPoses.ROLL];
        // The body turns about its shoulders, then comes back so its hips stay where its hind legs stand.
        float hy = HIND_Y + HIP_Y * Mth.cos(flex) - HIP_Z * Mth.sin(flex);
        float hz = HIP_Y * Mth.sin(flex) + HIP_Z * Mth.cos(flex);
        float wy = hy * Mth.cos(pitch) - hz * Mth.sin(pitch);
        float wz = hy * Mth.sin(pitch) + hz * Mth.cos(pitch);
        this.body.y = BODY_Y + c[BearPoses.BOB] + (HIND_Y + HIP_Y + c[BearPoses.SINK] - wy);
        this.body.z = BODY_Z + (HIP_Z - wz);
        this.body.xRot = pitch;
        this.body.yRot = c[BearPoses.YAW];
        this.body.zRot = roll;
        this.hind.xRot = flex;
        this.hind.zRot = c[BearPoses.HIND_ROLL];
        this.tail.xRot = 0.35F + c[BearPoses.TAIL];
        this.tail.yRot = c[BearPoses.TAIL_YAW];
        float look = c[BearPoses.LOOK];
        float yaw = netHeadYaw * Mth.DEG_TO_RAD * look;
        this.neck.xRot = c[BearPoses.NECK];
        this.neck.yRot = c[BearPoses.NECK_YAW] + yaw * 0.45F;
        this.head.xRot = c[BearPoses.HEAD] + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[BearPoses.HEAD_YAW] + yaw * 0.55F;
        this.head.zRot = c[BearPoses.HEAD_ROLL];
        this.jaw.xRot = c[BearPoses.JAW];
        this.ears[0].zRot = -c[BearPoses.EAR_R];
        this.ears[1].zRot = c[BearPoses.EAR_L];
        float open = 1.0F - 0.9F * Mth.clamp(c[BearPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = BearPoses.LEGS + k * BearPoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            boolean front = k < 2;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            leg[0].xRot = c[at] - pitch - (front ? 0.0F : flex);
            leg[0].zRot = side * c[at + 1] - (front ? roll : roll + c[BearPoses.HIND_ROLL]) * 0.7F;
            leg[1].xRot = c[at + 2];
            leg[2].xRot = c[at + 3];
        }
    }
}
