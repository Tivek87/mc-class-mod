package nl.tivek.multiversepowers.creature.stalker.client;

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
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;

// The Stalker's body, built as a person's but far too long: a narrow, eyeless-looking skull with two branching horns
// like dead twigs and a jaw of its own, a starved trunk with a ridge of bark plates down its spine, arms that hang
// past its knees ending in three long bone-white claws, and thin legs. Its texture is made by scripts/models/zombies.
// It poses itself (StalkerPoses): creeping, pressed to a trunk and peeking round it, curled up in a bush, and its moves
// on the server's timeline.
public final class StalkerModel extends HumanoidModel<Stalker> {
    static final float LEAN = 0.18F;
    private static final float HIP_Y = 9.0F;
    private static final float NECK_Y = -13.0F;
    private static final float NECK_Z = -0.5F;
    private static final float SHOULDER_X = 4.2F;
    private static final float SHOULDER_Y = -12.0F;
    private static final float LEG_X = 1.6F;
    private final ModelPart jaw;
    private final StalkerPoses pose = new StalkerPoses();

    public StalkerModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, NECK_Z));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F,
                4.0F), PartPose.offset(0.0F, -1.5F, 1.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            PartDefinition horn = head.addOrReplaceChild(s == 0 ? "right_horn" : "left_horn", CubeListBuilder.create()
                    .mirror(s == 1).texOffs(40, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                    PartPose.offsetAndRotation(side * 2.2F, -6.5F, 0.5F, -0.35F, 0.0F, side * 0.45F));
            horn.addOrReplaceChild("twig", CubeListBuilder.create().mirror(s == 1).texOffs(44, 0).addBox(-0.5F, -3.0F,
                    -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, 0.0F, 0.0F, side * 0.7F));
        }
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.5F, -13.0F, -1.5F, 7.0F, 8.0F, 3.0F)
                .texOffs(20, 16).addBox(-2.5F, -5.0F, -1.0F, 5.0F, 5.0F, 2.0F)
                .texOffs(34, 16).addBox(-0.5F, -13.0F, 1.2F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, HIP_Y, 0.0F, LEAN, 0.0F, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 32).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 17.0F, 2.0F)
                    .texOffs(8, 32).addBox(-1.25F, 14.0F, -1.25F, 2.5F, 2.0F, 2.5F)
                    .texOffs(44, 32).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.1F));
            for (float x : new float[] { -1.1F, -0.25F, 0.6F }) {
                arm.texOffs(20, 32).addBox(x, 16.0F, -1.0F, 0.5F, 5.0F, 0.5F);
            }
            float[] at = Trunk.turn(side * SHOULDER_X, SHOULDER_Y, 0.0F, LEAN, 0.0F, 0.0F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offset(at[0], HIP_Y + at[1],
                    at[2]));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(24, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F)
                    .texOffs(32, 32).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.05F));
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(Stalker stalker, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        StalkerPoses p = this.pose;
        p.of(stalker, limbSwing, limbSwingAmount, ageInTicks);
        float lean = p.lean;
        float hipY = HIP_Y + p.sink;
        this.body.y = hipY;
        this.body.z = p.ahead;
        this.body.xRot = lean;
        this.body.yRot = p.twist;
        this.body.zRot = p.roll;
        float[] neck = Trunk.turn(0.0F, NECK_Y, NECK_Z, lean, p.twist, p.roll);
        this.head.x = neck[0];
        this.head.y = hipY + neck[1];
        this.head.z = neck[2] + p.ahead;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + p.nod;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * p.follow + p.twist * 0.5F + p.shake;
        this.head.zRot = p.tilt;
        this.jaw.xRot = p.jaw;
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
            arm.z = at[2] + p.ahead;
            arm.xRot = p.arm[s];
            arm.yRot = p.twist + side * p.armIn[s];
            arm.zRot = -side * (0.06F + p.armOut[s]) + p.roll;
            Limbs.bend(this, elbows[s], p.elbow[s]);
            ModelPart leg = legs[s];
            leg.y = hipY;
            leg.z = p.ahead;
            leg.xRot = p.leg[s];
            leg.zRot = -side * (0.05F + p.legOut[s]);
            Limbs.bend(this, knees[s], p.knee[s]);
        }
    }
}
