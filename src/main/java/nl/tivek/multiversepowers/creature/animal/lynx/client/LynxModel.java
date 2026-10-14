package nl.tivek.multiversepowers.creature.animal.lynx.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;

// The lynx's body in a lynx's own build: a short deep trunk on long legs, the forelegs bending at the elbow and the
// wrist, the hind legs longer and bent at the stifle, the hock and the ankle, big paws; a round head with a short
// muzzle, eyes facing forward that blink, a jaw of its own, a ruff hanging from each cheek and upright ears ending in a
// long tuft; and the stub of a tail in two pieces, black at its tip. Its texture is painted by scripts/models/animals.
// LynxPoses moves it.
public final class LynxModel extends HierarchicalModel<Lynx> {
    static final float BODY_Y = 13.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 1.0F;
    static final float HIP_Z = 4.5F;
    private static final float THIGH_REST = -0.35F;
    private static final float SHANK_REST = 0.9F;
    private static final float FOOT_REST = -0.55F;
    private static final float TAIL_REST = -0.7F;
    private static final float YOUNG_HEAD = 1.3F;
    private static final CubeDeformation TUFT = new CubeDeformation(-0.25F, -0.3F, -0.25F);
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    // Right front, left front: upper, lower, paw. Right hind, left hind: thigh, shank, foot, paw.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final LynxPoses poses = new LynxPoses();

    public LynxModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tip");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            ModelPart middle = upper.getChild("middle");
            if (k < 2) {
                this.legs[k] = new ModelPart[] { upper, middle, middle.getChild("paw") };
            } else {
                ModelPart foot = middle.getChild("foot");
                this.legs[k] = new ModelPart[] { upper, middle, foot, foot.getChild("paw") };
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 13.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(36, 19).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 6.0F, TAIL_REST, 0.0F, 0.0F));
        tail.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(38, 9).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 0.0F, 2.8F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(38, 0).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 5.0F, 4.0F)
                .texOffs(0, 19).addBox(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 2.0F)
                .texOffs(10, 19).addBox(-3.5F, -0.5F, -3.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(10, 19).mirror().addBox(2.5F, -0.5F, -3.0F, 1.0F, 3.0F, 2.0F).mirror(false),
                PartPose.offset(0.0F, -2.5F, -6.5F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(26, 19).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.0F, -4.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            boolean left = s == 1;
            String name = left ? "left_" : "right_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(left)
                    .texOffs(16, 19).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F)
                    .texOffs(22, 19).addBox(-0.5F, -4.7F, -0.5F, 1.0F, 2.0F, 1.0F, TUFT),
                    PartPose.offset(side * 1.6F, -2.5F, -1.5F));
            // An eye in the face; it shuts by folding down to nothing, as an upper lid comes down.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(56, 0).addBox(-0.5F, -1.0F, -0.6F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.0F, -1.0F, -3.5F));
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 28).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                    PartPose.offset(side * 1.8F, 2.0F, -5.0F));
            PartDefinition forearm = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(12, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(0.0F, 5.0F, 0.0F));
            forearm.addOrReplaceChild("paw", CubeListBuilder.create().mirror(left)
                    .texOffs(20, 28).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 3.0F),
                    PartPose.offset(0.0F, 3.0F, 0.0F));
            PartDefinition thigh = body.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(32, 28).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 6.0F, 4.0F),
                    PartPose.offsetAndRotation(side * 1.8F, HIP_Y, HIP_Z, THIGH_REST, 0.0F, 0.0F));
            PartDefinition shank = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(46, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, SHANK_REST, 0.0F, 0.0F));
            PartDefinition foot = shank.addOrReplaceChild("foot", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 40).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, FOOT_REST, 0.0F, 0.0F));
            foot.addOrReplaceChild("paw", CubeListBuilder.create().mirror(left)
                    .texOffs(20, 28).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 3.0F),
                    PartPose.offset(0.0F, 1.5F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Lynx lynx, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - lynx.tickCount;
        float[] c = this.poses.of(lynx, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[LynxPoses.PITCH];
        // The body turns about its middle, then comes back so its hips stay where its hind legs stand.
        float wy = HIP_Y * Mth.cos(pitch) - HIP_Z * Mth.sin(pitch);
        float wz = HIP_Y * Mth.sin(pitch) + HIP_Z * Mth.cos(pitch);
        this.body.y = BODY_Y + c[LynxPoses.BOB] + c[LynxPoses.SINK] + (HIP_Y - wy);
        this.body.z = HIP_Z - wz - c[LynxPoses.LUNGE];
        this.body.xRot = pitch;
        this.body.zRot = c[LynxPoses.ROLL];
        this.tail.xRot = TAIL_REST + c[LynxPoses.TAIL] - pitch * 0.5F;
        this.tail.yRot = c[LynxPoses.TAIL_WAG];
        this.tailTip.xRot = c[LynxPoses.TAIL_TIP];
        this.tailTip.yRot = 0.6F * c[LynxPoses.TAIL_WAG];
        float open = 1.0F - 0.9F * Mth.clamp(c[LynxPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        float look = c[LynxPoses.LOOK];
        // The head stays level whatever the body does, then is turned.
        this.head.xRot = c[LynxPoses.HEAD] - pitch + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[LynxPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.head.zRot = c[LynxPoses.HEAD_ROLL];
        this.jaw.xRot = c[LynxPoses.JAW];
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            this.ears[s].zRot = side * (0.2F + c[LynxPoses.EAR_R + s]);
            this.ears[s].xRot = -0.9F * c[LynxPoses.EARS_BACK];
        }
        for (int k = 0; k < 4; k++) {
            int at = LynxPoses.LEGS + k * LynxPoses.PER_LEG;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            if (k < 2) {
                leg[0].xRot = c[at] - pitch;
                leg[1].xRot = c[at + 1];
                leg[2].xRot = c[at + 3];
            } else {
                leg[0].xRot = THIGH_REST + c[at] - pitch;
                leg[1].xRot = SHANK_REST + c[at + 1];
                leg[2].xRot = FOOT_REST + c[at + 2];
                leg[3].xRot = c[at + 3];
            }
        }
    }
}
