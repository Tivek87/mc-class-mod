package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;
import org.joml.Quaternionf;

// The Wizard's body (and his images'), built as a person's: a robe from his shoulders with a mantle over them, its
// long skirt hanging from his hips on its own (swinging with his steps, trailing as he floats, flaring as magic bursts
// out of him), wide sleeves, a hooked nose and a long white beard that swings from his chin, and a tall pointed hat,
// its tip bending back in three joints that sway as he moves. His staff is drawn in his right hand on its own
// (StaffLayer). Its texture is made by scripts/models/zombies. It poses itself (WizardPoses).
public final class WizardModel<T extends MovingMonster> extends HumanoidModel<T> {
    private static final float HIP_Y = 12.0F;
    private static final float NECK_Y = -12.0F;
    private static final float SHOULDER_X = 5.0F;
    private static final float SHOULDER_Y = -10.0F;
    private static final float LEG_X = 2.0F;
    private final ModelPart beard;
    private final ModelPart beardTip;
    private final ModelPart skirt;
    private final ModelPart[] hatJoints;
    private final Quaternionf wrist = new Quaternionf();
    final WizardPoses pose = new WizardPoses();
    float alpha = 1.0F;

    public WizardModel(ModelPart root) {
        super(root);
        this.beard = this.head.getChild("beard");
        this.beardTip = this.beard.getChild("tip");
        this.skirt = this.body.getChild("skirt");
        ModelPart mid = this.hat.getChild("mid");
        ModelPart top = mid.getChild("top");
        this.hatJoints = new ModelPart[] { mid, top, top.getChild("tip") };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0).addBox(-1.0F, -4.5F, -5.5F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-3.0F, 0.0F, -1.0F, 6.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -2.0F, -4.0F, 0.12F,
                        0.0F, 0.0F));
        beard.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(56, 0).addBox(-2.0F, 0.0F, -0.75F, 4.0F,
                4.0F, 1.5F), PartPose.offset(0.0F, 6.5F, 0.0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-7.0F, -8.8F, -7.0F, 14.0F, 1.0F, 14.0F)
                .texOffs(56, 16).addBox(-4.5F, -12.6F, -4.5F, 9.0F, 4.0F, 9.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
        PartDefinition mid = hat.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(92, 16)
                .addBox(-3.5F, -4.0F, -3.5F, 7.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -12.4F, 0.3F,
                        -0.12F, 0.0F, 0.0F));
        PartDefinition top = mid.addOrReplaceChild("top", CubeListBuilder.create().texOffs(56, 30)
                .addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -3.8F, 0.2F,
                        -0.22F, 0.0F, 0.0F));
        top.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(76, 30).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                .texOffs(88, 30).addBox(-0.75F, -6.5F, -0.75F, 1.5F, 3.0F, 1.5F),
                PartPose.offsetAndRotation(0.0F, -3.8F, 0.2F, -0.4F, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(68, 0).addBox(-5.0F, -12.5F, -3.0F, 10.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(24, 32).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 11.0F, 5.0F)
                .texOffs(52, 40).addBox(-5.0F, 6.5F, -3.0F, 10.0F, 5.0F, 6.0F, new CubeDeformation(0.05F)),
                PartPose.ZERO);
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 48).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                    .texOffs(12, 48).addBox(-2.3F, 3.5F, -2.3F, 4.6F, 6.0F, 4.6F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offset(side * SHOULDER_X,
                    HIP_Y + SHOULDER_Y, 0.0F));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1).texOffs(32, 48).addBox(-2.0F, 0.0F, -2.0F,
                    4.0F, 12.0F, 4.0F);
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        if (this.alpha < 1.0F) {
            color = (int) ((color >>> 24) * this.alpha) << 24 | color & 0xFFFFFF;
        }
        super.renderToBuffer(pose, buffer, light, overlay, color);
    }

    @Override
    public void setupAnim(T caster, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.head.resetPose();
        this.beard.resetPose();
        this.beardTip.resetPose();
        this.hat.resetPose();
        for (ModelPart joint : this.hatJoints) {
            joint.resetPose();
        }
        this.body.resetPose();
        this.skirt.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        WizardPoses p = this.pose;
        p.of(caster, limbSwing, limbSwingAmount, ageInTicks, WizardRenderer.floating(caster));
        float[] c = p.c;
        float hipY = HIP_Y + c[SINK] - c[LIFT];
        this.body.y = hipY;
        this.body.xRot = c[LEAN];
        this.body.yRot = c[TWIST];
        this.body.zRot = c[ROLL];
        this.skirt.xRot = p.skirt;
        this.skirt.zRot = p.skirtSide;
        float flare = 1.0F + 0.22F * Math.max(0.0F, c[FLARE]);
        this.skirt.xScale = flare;
        this.skirt.zScale = flare;
        float[] neck = Trunk.turn(0.0F, NECK_Y, 0.0F, c[LEAN], c[TWIST], c[ROLL]);
        this.head.x = neck[0];
        this.head.y = hipY + neck[1];
        this.head.z = neck[2];
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + c[NOD];
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + c[TWIST] * 0.5F;
        this.head.zRot = c[TILT];
        this.hat.copyFrom(this.head);
        this.beard.xRot += p.beard;
        this.beard.zRot += p.beardSide;
        this.beardTip.xRot += p.beard * 0.8F;
        this.beardTip.zRot += p.beardSide * 0.8F;
        for (int k = 0; k < this.hatJoints.length; k++) {
            this.hatJoints[k].xRot += p.hat * (0.6F + 0.4F * k);
            this.hatJoints[k].zRot += p.hatSide * (0.6F + 0.4F * k);
        }
        ModelPart[] arms = { this.rightArm, this.leftArm };
        ModelPart[] legs = { this.rightLeg, this.leftLeg };
        Limbs.Joint[] elbows = { Limbs.Joint.RIGHT_ELBOW, Limbs.Joint.LEFT_ELBOW };
        Limbs.Joint[] wrists = { Limbs.Joint.RIGHT_WRIST, Limbs.Joint.LEFT_WRIST };
        Limbs.Joint[] knees = { Limbs.Joint.RIGHT_KNEE, Limbs.Joint.LEFT_KNEE };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float[] at = Trunk.turn(side * SHOULDER_X, SHOULDER_Y, 0.0F, c[LEAN], c[TWIST], c[ROLL]);
            ModelPart arm = arms[s];
            arm.x = at[0];
            arm.y = hipY + at[1];
            arm.z = at[2];
            arm.xRot = c[R_ARM + s];
            arm.yRot = c[TWIST] + side * c[R_IN + s];
            arm.zRot = -side * (0.04F + c[R_OUT + s]) + c[ROLL];
            Limbs.bend(this, elbows[s], c[R_ELBOW + s]);
            Limbs.turn(this, wrists[s], this.wrist.rotationX(c[R_WRIST + s]));
            ModelPart leg = legs[s];
            leg.y = hipY;
            leg.xRot = c[R_LEG + s];
            leg.zRot = -side * 0.03F;
            Limbs.bend(this, knees[s], c[R_KNEE + s]);
        }
    }
}
