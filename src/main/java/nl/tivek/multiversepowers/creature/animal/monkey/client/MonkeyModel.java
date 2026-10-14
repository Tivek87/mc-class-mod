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

// The monkey's body in a capuchin's own build: a slim trunk, deep at the chest and narrow at the loins, thin arms and
// legs that bend at the elbow and the knee and end in hands and long feet that bend at the wrist and the ankle; a round
// head with a tufted cap, a short bare muzzle and a chin of its own, pale fur framing the face, small ears set back,
// and two eyes over the muzzle that blink; and a long tail of six pieces, thick at the root, carried up in an arch, its
// tip hooked down. Its texture is painted by scripts/models/animals. MonkeyPoses moves it.
public final class MonkeyModel extends HierarchicalModel<Monkey> {
    static final float BODY_Y = 16.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 0.5F;
    static final float HIP_Z = 4.0F;
    // How each tail piece turns from the one before it when the tail is carried: up in an arch, its tip hooked down.
    private static final float[] CURL = { 0.55F, 0.35F, 0.15F, -0.45F, -0.75F, -0.65F };
    private static final CubeDeformation THIN = new CubeDeformation(-0.3F, 0.0F, -0.3F);
    private static final CubeDeformation THIGH = new CubeDeformation(-0.15F, 0.0F, -0.2F);
    // Small ears, flat against the head.
    private static final CubeDeformation EAR = new CubeDeformation(-0.25F, -0.15F, -0.1F);
    private static final float YOUNG_HEAD = 1.35F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[6];
    // Right arm, left arm, right leg, left leg: upper, lower, hand or foot.
    private final ModelPart[][] limbs = new ModelPart[4][];
    private final MonkeyPoses poses = new MonkeyPoses();

    public MonkeyModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        ModelPart piece = this.body;
        for (int k = 0; k < this.tail.length; k++) {
            piece = piece.getChild("tail");
            this.tail[k] = piece;
        }
        String[] names = { "right_arm", "left_arm", "right_leg", "left_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            ModelPart lower = upper.getChild("lower");
            this.limbs[k] = new ModelPart[] { upper, lower, lower.getChild("end") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A slim trunk: a deep chest, narrower loins, a rounded rump a little higher than the shoulders.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2.5F, -5.5F, 5.0F, 5.0F, 5.0F)
                .texOffs(20, 0).addBox(-2.0F, -2.3F, -0.5F, 4.0F, 4.0F, 3.0F)
                .texOffs(34, 0).addBox(-2.5F, -2.8F, 2.5F, 5.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        // A round skull with a tufted cap on top, a short bare muzzle and small round ears set back on its sides.
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-2.5F, -4.5F, -4.0F, 5.0F, 5.0F, 4.0F)
                .texOffs(18, 10).addBox(-1.5F, -1.5F, -5.0F, 3.0F, 2.0F, 1.0F)
                .texOffs(36, 10).addBox(-1.5F, -5.5F, -3.5F, 3.0F, 1.0F, 3.0F)
                .texOffs(48, 10).addBox(-3.2F, -3.5F, -1.5F, 1.0F, 2.0F, 1.0F, EAR)
                .texOffs(48, 10).mirror().addBox(2.2F, -3.5F, -1.5F, 1.0F, 2.0F, 1.0F, EAR).mirror(false),
                PartPose.offset(0.0F, -1.5F, -5.5F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(26, 10).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.5F, -3.0F));
        for (int s = 0; s < 2; s++) {
            // An eye in the face, set apart over the muzzle; it shuts by folding down to nothing, as a lid comes down.
            head.addOrReplaceChild((s == 0 ? "right_" : "left_") + "eye", CubeListBuilder.create()
                    .texOffs(54, 10).addBox(-0.5F, -1.0F, -0.6F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(s == 0 ? -1.0F : 1.0F, -2.5F, -3.5F));
        }
        PartDefinition tail = body;
        for (int k = 0; k < 6; k++) {
            // Thicker at its root, thinning toward the tip.
            CubeDeformation thick = new CubeDeformation(k < 2 ? 0.2F : k < 4 ? 0.05F : -0.1F);
            tail = tail.addOrReplaceChild("tail", CubeListBuilder.create()
                    .texOffs(54, 20).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, thick),
                    k == 0 ? PartPose.offset(0.0F, -1.8F, 5.3F) : PartPose.offset(0.0F, 0.0F, 2.9F));
        }
        for (int k = 0; k < 4; k++) {
            boolean left = k % 2 == 1;
            float side = left ? 1.0F : -1.0F;
            boolean arm = k < 2;
            String name = (left ? "left_" : "right_") + (arm ? "arm" : "leg");
            PartDefinition upper = body.addOrReplaceChild(name, CubeListBuilder.create().mirror(left)
                    .texOffs(arm ? 0 : 24, 20).addBox(-1.0F, -1.0F, arm ? -1.0F : -1.5F, 2.0F, arm ? 4.0F : 5.0F,
                            arm ? 2.0F : 3.0F, arm ? THIN : THIGH),
                    PartPose.offset(side * (arm ? 2.2F : 1.8F), arm ? 1.0F : HIP_Y, arm ? -4.0F : HIP_Z));
            PartDefinition lower = upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                    .texOffs(arm ? 8 : 34, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, THIN),
                    PartPose.offset(0.0F, arm ? 3.0F : 3.5F, 0.0F));
            // A hand, or a long foot reaching further forward.
            lower.addOrReplaceChild("end", CubeListBuilder.create().mirror(left)
                    .texOffs(arm ? 16 : 42, 20).addBox(-1.0F, 0.0F, arm ? -1.6F : -2.6F, 2.0F, 1.0F,
                            arm ? 2.0F : 4.0F, THIN),
                    PartPose.offset(0.0F, 3.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
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
        float open = 1.0F - 0.9F * Mth.clamp(c[MonkeyPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        float curl = c[MonkeyPoses.TAIL];
        for (int k = 0; k < this.tail.length; k++) {
            // The tail's root is told against the ground, so it lies out behind a sitting monkey.
            this.tail[k].xRot = CURL[k] * curl - (k == 0 ? pitch : 0.0F) + c[MonkeyPoses.TAIL_SWAY] * k * 0.12F;
            this.tail[k].yRot = c[MonkeyPoses.TAIL_WAG] * (k + 1) * 0.3F;
        }
        for (int k = 0; k < 4; k++) {
            int at = MonkeyPoses.LIMBS + k * MonkeyPoses.PER_LIMB;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            ModelPart[] limb = this.limbs[k];
            // A limb's swing is told against the ground, whatever the body does.
            limb[0].xRot = c[at] - pitch;
            limb[0].zRot = side * c[at + 1];
            limb[1].xRot = c[at + 2];
            limb[2].xRot = c[at + 3];
        }
    }
}
