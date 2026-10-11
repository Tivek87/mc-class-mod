package nl.tivek.multiversepowers.creature.lurker.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.lurker.Crawler;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;

// A crawler's body (the Lurker, and smaller the Screamer's ferals), built as a tall, gaunt person standing (head with a
// hanging jaw, a narrow trunk with its ribs and spine showing, long thin arms ending in bone claws, long legs), so it
// goes limp and gets up as one. Every frame it is set down on all fours: its trunk leant far forward with its hips high
// at the back, its legs folded under them at the knees, its arms reaching down to the ground ahead, bent a little at
// the elbows, and its head held up to look ahead. Its texture is made by scripts/models/zombies. It poses itself
// (LurkerPoses) on top of that.
public final class LurkerModel<T extends MovingMonster & Crawler> extends HumanoidModel<T> {
    static final float LEAN = 1.25F;
    private static final float HIP_Y = 1.0F;
    private static final float CRAWL_HIP_Y = 10.0F;
    private static final float CRAWL_HIP_Z = 7.0F;
    private static final float NECK_Y = -16.0F;
    private static final float SHOULDER_X = 5.0F;
    private static final float SHOULDER_Y = -14.5F;
    private static final float LEG_X = 2.5F;
    static final float ARM = -0.55F;
    static final float ELBOW = 0.5F;
    static final float LEG = -1.0F;
    static final float KNEE = 1.85F;
    private final ModelPart jaw;
    private final LurkerPoses pose = new LurkerPoses();

    public LurkerModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -8.0F, -4.0F, 7.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(30, 0).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 2.0F,
                6.0F), PartPose.offset(0.0F, -2.0F, 2.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -16.0F, -2.5F, 8.0F, 9.0F, 5.0F)
                .texOffs(26, 16).addBox(-3.0F, -7.0F, -2.0F, 6.0F, 7.0F, 4.0F)
                .texOffs(46, 16).addBox(-1.0F, -15.0F, 2.0F, 2.0F, 14.0F, 1.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 32).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F)
                    .texOffs(12, 32).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 9.0F, 2.0F)
                    .texOffs(20, 32).addBox(-1.5F, 19.0F, -1.5F, 3.0F, 2.0F, 3.0F)
                    .texOffs(32, 32).addBox(-1.5F, 21.0F, -1.6F, 0.8F, 5.0F, 0.8F)
                    .texOffs(32, 32).addBox(-0.4F, 21.0F, -1.8F, 0.8F, 6.0F, 0.8F)
                    .texOffs(32, 32).addBox(0.7F, 21.0F, -1.6F, 0.8F, 5.0F, 0.8F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offset(side * SHOULDER_X,
                    HIP_Y + SHOULDER_Y, 0.0F));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(40, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F)
                    .texOffs(56, 32).addBox(-1.5F, 11.0F, -1.5F, 3.0F, 10.0F, 3.0F)
                    .texOffs(68, 32).addBox(-1.5F, 21.0F, -3.5F, 3.0F, 2.0F, 5.0F);
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(T crawler, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        LurkerPoses p = this.pose;
        p.of(crawler, limbSwing, limbSwingAmount, ageInTicks);
        float lean = p.lean;
        float hipY = CRAWL_HIP_Y + p.sink;
        float hipZ = CRAWL_HIP_Z + p.shift;
        this.body.y = hipY;
        this.body.z = hipZ;
        this.body.xRot = lean;
        this.body.yRot = p.twist;
        this.body.zRot = p.roll;
        float[] neck = Trunk.turn(0.0F, NECK_Y, 0.0F, lean, p.twist, p.roll);
        this.head.x = neck[0];
        this.head.y = hipY + neck[1];
        this.head.z = hipZ + neck[2];
        this.head.xRot = Mth.clamp(headPitch * Mth.DEG_TO_RAD, -0.6F, 0.6F) + p.nod;
        this.head.yRot = Mth.clamp(netHeadYaw * Mth.DEG_TO_RAD, -0.9F, 0.9F) * 0.8F + p.twist * 0.6F;
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
            arm.z = hipZ + at[2];
            arm.xRot = p.arm[s];
            arm.yRot = p.twist;
            arm.zRot = -side * (0.12F + p.armOut[s]) + p.roll;
            Limbs.bend(this, elbows[s], p.elbow[s]);
            float[] hip = Trunk.turn(side * LEG_X, 0.0F, 0.0F, 0.0F, p.twist, p.roll);
            ModelPart leg = legs[s];
            leg.x = hip[0];
            leg.y = hipY + hip[1];
            leg.z = hipZ + hip[2];
            leg.xRot = p.leg[s];
            leg.zRot = -side * (0.1F + p.legOut);
            Limbs.bend(this, knees[s], p.knee[s]);
        }
    }
}
