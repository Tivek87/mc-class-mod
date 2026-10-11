package nl.tivek.multiversepowers.creature.screamer.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;

// The Screamer's body, built as a person's: a narrow skull whose long jaw hangs from it on its own (and drops,
// unhinged, down to the chest as it screams, showing the raw throat behind it), a starved trunk with its ribs showing
// through a torn shirt, long thin arms and legs in rags. Its texture is made by scripts/models/zombies. It poses
// itself (ScreamerPoses): hunched, twitching, its teeth clicking, and its moves on the server's timeline.
public final class ScreamerModel extends HumanoidModel<Screamer> {
    static final float LEAN = 0.3F;
    private static final float HIP_Y = 12.0F;
    private static final float NECK_Y = -12.0F;
    private static final float NECK_Z = -0.5F;
    private static final float SHOULDER_X = 4.5F;
    private static final float SHOULDER_Y = -11.0F;
    private static final float LEG_X = 1.9F;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ScreamerPoses pose = new ScreamerPoses();

    public ScreamerModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
        this.throat = this.head.getChild("throat");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -8.0F, -3.5F, 7.0F, 6.0F, 7.0F)
                .texOffs(28, 0).addBox(-3.0F, -2.0F, -1.0F, 6.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, NECK_Z));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 0).addBox(-3.0F, 0.0F, -5.0F, 6.0F, 2.0F,
                5.0F), PartPose.offset(0.0F, -2.0F, 1.5F));
        head.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(72, 0).addBox(-2.5F, 0.0F, -3.0F, 5.0F,
                1.0F, 4.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.5F, -12.0F, -2.0F, 7.0F, 7.0F, 4.0F)
                .texOffs(22, 16).addBox(-3.0F, -5.0F, -1.5F, 6.0F, 5.0F, 3.0F)
                .texOffs(40, 16).addBox(-3.5F, -12.0F, -2.0F, 7.0F, 10.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, HIP_Y, 0.0F, LEAN, 0.0F, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 32).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F)
                    .texOffs(8, 32).addBox(-1.0F, 12.0F, -1.5F, 2.0F, 3.0F, 2.0F)
                    .texOffs(16, 32).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.1F));
            float[] at = Trunk.turn(side * SHOULDER_X, SHOULDER_Y, 0.0F, LEAN, 0.0F, 0.0F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offset(at[0], HIP_Y + at[1],
                    at[2]));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 48).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                    .texOffs(12, 48).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.25F));
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Screamer screamer, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.throat.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        ScreamerPoses p = this.pose;
        p.of(screamer, limbSwing, limbSwingAmount, ageInTicks);
        float lean = p.lean;
        float hipY = HIP_Y + p.sink;
        this.body.y = hipY;
        this.body.xRot = lean;
        this.body.yRot = p.twist;
        this.body.zRot = p.roll;
        float[] neck = Trunk.turn(0.0F, NECK_Y, NECK_Z, lean, p.twist, p.roll);
        this.head.x = neck[0];
        this.head.y = hipY + neck[1];
        this.head.z = neck[2];
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + p.nod;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + p.twist * 0.5F + p.shake;
        this.head.zRot = p.tilt;
        // Unhinged, the jaw slides down out of its sockets as it drops open, the raw throat showing behind it.
        this.jaw.xRot = p.jaw;
        this.jaw.y += p.drop;
        this.jaw.z += p.drop * 0.35F;
        this.throat.visible = p.drop > 0.3F;
        this.throat.y += p.drop * 0.45F;
        ModelPart[] arms = { this.rightArm, this.leftArm };
        ModelPart[] legs = { this.rightLeg, this.leftLeg };
        Limbs.Joint[] elbows = { Limbs.Joint.RIGHT_ELBOW, Limbs.Joint.LEFT_ELBOW };
        Limbs.Joint[] knees = { Limbs.Joint.RIGHT_KNEE, Limbs.Joint.LEFT_KNEE };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float[] at = Trunk.turn(side * SHOULDER_X, SHOULDER_Y, 0.0F, lean, p.twist, p.roll);
            ModelPart arm = arms[s];
            arm.x = at[0];
            arm.y = hipY + at[1];
            arm.z = at[2];
            arm.xRot = p.arm[s];
            arm.yRot = p.twist;
            arm.zRot = -side * (0.08F + p.armOut[s]) + p.roll;
            Limbs.bend(this, elbows[s], p.elbow[s]);
            ModelPart leg = legs[s];
            leg.y = hipY;
            leg.xRot = p.leg[s];
            leg.zRot = -side * 0.04F;
            Limbs.bend(this, knees[s], p.knee[s]);
        }
    }
}
