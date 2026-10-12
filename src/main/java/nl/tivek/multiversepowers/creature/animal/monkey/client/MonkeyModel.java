package nl.tivek.multiversepowers.creature.animal.monkey.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.monkey.Monkey;

// The monkey's body in a capuchin's own build: a small trunk, long thin arms and legs that bend at the elbow and the
// knee and end in hands and feet, a round head with a short bare face, a jaw of its own and ears at its sides, and a
// long thin tail of four pieces carried up in an arch, its tip hooked down. Its texture is painted by
// scripts/models/animals. MonkeyPoses moves it.
public final class MonkeyModel extends HierarchicalModel<Monkey> {
    static final float BODY_Y = 16.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 0.0F;
    static final float HIP_Z = 2.5F;
    // How each tail piece turns from the one before it when the tail is carried: up in an arch, its tip hooked down.
    private static final float[] CURL = { 0.8F, 0.4F, -0.7F, -1.3F };
    private static final CubeDeformation THIN = new CubeDeformation(-0.3F, 0.0F, -0.3F);
    private static final float YOUNG_HEAD = 1.35F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] tail = new ModelPart[4];
    // Right arm, left arm, right leg, left leg: upper, lower.
    private final ModelPart[][] limbs = new ModelPart[4][];
    private final MonkeyPoses poses = new MonkeyPoses();

    public MonkeyModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart piece = this.body;
        for (int k = 0; k < this.tail.length; k++) {
            piece = piece.getChild("tail");
            this.tail[k] = piece;
        }
        String[] names = { "right_arm", "left_arm", "right_leg", "left_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            this.limbs[k] = new ModelPart[] { upper, upper.getChild("lower") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(22, 0).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(38, 0).addBox(-1.0F, -2.0F, -4.0F, 2.0F, 1.0F, 1.0F)
                .texOffs(44, 0).addBox(-3.0F, -3.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(44, 0).mirror().addBox(2.0F, -3.0F, -1.0F, 1.0F, 2.0F, 1.0F).mirror(false),
                PartPose.offset(0.0F, -1.5F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -3.0F));
        PartDefinition tail = body;
        for (int k = 0; k < 4; k++) {
            tail = tail.addOrReplaceChild("tail", CubeListBuilder.create()
                    .texOffs(40, 11).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                    k == 0 ? PartPose.offset(0.0F, -1.5F, 3.0F) : PartPose.offset(0.0F, 0.0F, 4.0F));
        }
        for (int k = 0; k < 4; k++) {
            boolean left = k % 2 == 1;
            float side = left ? 1.0F : -1.0F;
            boolean arm = k < 2;
            String name = (left ? "left_" : "right_") + (arm ? "arm" : "leg");
            PartDefinition upper = body.addOrReplaceChild(name, CubeListBuilder.create().mirror(left)
                    .texOffs(arm ? 0 : 24, 11).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, THIN),
                    PartPose.offset(side * (arm ? 2.3F : 1.6F), 0.0F, arm ? -3.0F : HIP_Z));
            upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                    .texOffs(arm ? 8 : 32, 11).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, THIN)
                    .texOffs(16, 11).addBox(-1.0F, 3.0F, -2.0F, 2.0F, 1.0F, 2.0F, THIN),
                    PartPose.offset(0.0F, 4.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Monkey monkey, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - monkey.tickCount;
        float[] c = this.poses.of(monkey, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[MonkeyPoses.PITCH];
        // The body turns about its middle, then comes back so its hips stay where its legs stand.
        float wy = HIP_Y * Mth.cos(pitch) - HIP_Z * Mth.sin(pitch);
        float wz = HIP_Y * Mth.sin(pitch) + HIP_Z * Mth.cos(pitch);
        this.body.y = BODY_Y + c[MonkeyPoses.BOB] + c[MonkeyPoses.SINK] + (HIP_Y - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = c[MonkeyPoses.ROLL];
        float look = c[MonkeyPoses.LOOK];
        // The head stays level whatever the body does, then is turned.
        this.head.xRot = c[MonkeyPoses.HEAD] - pitch + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[MonkeyPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.head.zRot = c[MonkeyPoses.HEAD_ROLL];
        this.jaw.xRot = c[MonkeyPoses.JAW];
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        float curl = c[MonkeyPoses.TAIL];
        for (int k = 0; k < this.tail.length; k++) {
            // The tail's root is told against the ground, so it lies out behind a sitting monkey.
            this.tail[k].xRot = CURL[k] * curl - (k == 0 ? pitch : 0.0F);
            this.tail[k].yRot = c[MonkeyPoses.TAIL_WAG] * (k + 1) * 0.5F;
        }
        for (int k = 0; k < 4; k++) {
            int at = MonkeyPoses.LIMBS + k * MonkeyPoses.PER_LIMB;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            ModelPart[] limb = this.limbs[k];
            // A limb's swing is told against the ground, whatever the body does.
            limb[0].xRot = c[at] - pitch;
            limb[0].zRot = side * c[at + 1];
            limb[1].xRot = c[at + 2];
        }
    }
}
