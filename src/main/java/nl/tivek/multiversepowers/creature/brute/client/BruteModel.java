package nl.tivek.multiversepowers.creature.brute.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;

// The brute's body, built as a person's (head, trunk, two arms, two legs, so it goes limp and gets up as one) but
// bigger and stronger than a zombie: a head jutting from a broad, slightly hunched chest, bone spikes along its back,
// long muscled arms ending in heavy fists, thick legs. Its texture is made by scripts/models/zombie_brute. It poses itself (never the
// person's own swing): a heavy, rolling walk, a slack jaw, and each of its moves (BrutePoses) on the timeline the
// server keeps.
public final class BruteModel extends HumanoidModel<ZombieBrute> {
    // How far its trunk leans forward at rest, and where its hips, neck and shoulders are (in pixels, y down, -z
    // ahead; the neck and shoulders in the trunk's own frame).
    static final float LEAN = 0.2F;
    private static final float HIP_Y = 12.0F;
    private static final float HIP_Z = 1.0F;
    private static final float NECK_Y = -14.5F;
    private static final float NECK_Z = -3.0F;
    private static final float SHOULDER_X = 9.0F;
    private static final float SHOULDER_Y = -12.5F;
    private static final float LEG_X = 4.2F;
    private static final float LEG = 11.0F;
    private final ModelPart jaw;
    private final BrutePoses pose = new BrutePoses();

    public BruteModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float[] neck = trunk(0.0F, NECK_Y, NECK_Z, LEAN);
        float[] shoulder = trunk(0.0F, SHOULDER_Y, 0.0F, LEAN);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -7.0F, -6.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0).addBox(-4.5F, -6.0F, -6.8F, 9.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, HIP_Y + neck[1], HIP_Z + neck[2]));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(54, 0).addBox(-3.5F, 0.0F, -5.5F, 7.0F, 3.0F,
                6.0F), PartPose.offset(0.0F, 0.5F, -1.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-8.0F, -15.0F, -5.0F, 16.0F, 10.0F, 10.0F)
                .texOffs(52, 16).addBox(-6.0F, -6.0F, -4.0F, 12.0F, 6.0F, 8.0F)
                .texOffs(92, 16).addBox(-5.0F, -16.0F, 3.0F, 10.0F, 4.0F, 3.0F)
                .texOffs(124, 16).addBox(-3.5F, -18.0F, 4.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(124, 16).addBox(-0.5F, -18.5F, 4.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(124, 16).addBox(2.5F, -18.0F, 4.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, HIP_Y, HIP_Z, LEAN, 0.0F, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 36).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 9.0F, 8.0F)
                    .texOffs(36, 36).addBox(-3.5F, 6.0F, -3.5F, 7.0F, 8.0F, 7.0F)
                    .texOffs(64, 36).addBox(-3.5F, 14.0F, -4.0F, 7.0F, 5.0F, 8.0F)
                    .texOffs(98, 36).addBox(-1.0F + side * 1.5F, -5.5F, -2.5F, 2.0F, 3.0F, 2.0F)
                    .texOffs(98, 36).addBox(-1.0F + side * 1.5F, -5.0F, 1.0F, 2.0F, 3.0F, 2.0F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offsetAndRotation(side
                    * SHOULDER_X, HIP_Y + shoulder[1], HIP_Z + shoulder[2], 0.0F, 0.0F, -side * 0.08F));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 56).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 11.0F, 6.0F)
                    .texOffs(24, 56).addBox(-3.5F, 9.0F, -5.5F, 7.0F, 3.0F, 9.0F);
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    HIP_Z + 0.5F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    // A point in the trunk's frame (pivot at the hips), leaning `lean`, as an offset from the hips.
    private static float[] trunk(float x, float y, float z, float lean) {
        return turn(x, y, z, lean, 0.0F, 0.0F);
    }

    // A model part turns about z, then y, then x, applied to the point in that order from the inside out.
    private static float[] turn(float x, float y, float z, float xRot, float yRot, float zRot) {
        float c = Mth.cos(xRot);
        float s = Mth.sin(xRot);
        float y1 = y * c - z * s;
        float z1 = y * s + z * c;
        c = Mth.cos(yRot);
        s = Mth.sin(yRot);
        float x2 = x * c + z1 * s;
        float z2 = -x * s + z1 * c;
        c = Mth.cos(zRot);
        s = Mth.sin(zRot);
        return new float[] { x2 * c - y1 * s, x2 * s + y1 * c, z2 };
    }

    @Override
    public void setupAnim(ZombieBrute brute, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        BrutePoses p = this.pose;
        p.of(brute, limbSwing, limbSwingAmount, ageInTicks);
        // The more upright leg carries it: its hips drop as far as that leg's foot would lift.
        float spread = Mth.cos(p.legSpread);
        float sink = p.sink + LEG * (1.0F - Math.max(Mth.cos(p.leg[0]), Mth.cos(p.leg[1])) * spread);
        float lean = LEAN + p.lean;
        this.body.xRot = lean;
        this.body.yRot = p.twist;
        this.body.zRot = p.roll;
        this.body.y += sink;
        float[] neck = turn(0.0F, NECK_Y, NECK_Z, lean, p.twist, p.roll);
        this.head.x = neck[0];
        this.head.y = HIP_Y + sink + neck[1];
        this.head.z = HIP_Z + neck[2];
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + p.nod;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + p.twist * 0.5F + p.shake;
        this.head.zRot = p.roll * 0.5F;
        this.jaw.xRot = p.jaw;
        ModelPart[] arms = { this.rightArm, this.leftArm };
        ModelPart[] legs = { this.rightLeg, this.leftLeg };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float[] at = turn(side * SHOULDER_X, SHOULDER_Y, 0.0F, lean, p.twist, p.roll);
            ModelPart arm = arms[s];
            arm.x = at[0];
            arm.y = HIP_Y + sink + at[1];
            arm.z = HIP_Z + at[2];
            arm.xRot = p.arm[s];
            arm.yRot = p.twist + side * p.armTwist[s];
            arm.zRot = -side * (0.08F + p.spread[s]) + p.roll;
            ModelPart leg = legs[s];
            leg.y += sink;
            leg.xRot = p.leg[s];
            leg.zRot = -side * p.legSpread;
        }
    }
}
