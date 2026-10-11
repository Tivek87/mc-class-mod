package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
import nl.tivek.multiversepowers.engine.client.model.PlainDrawing;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;
import org.joml.Quaternionf;

// The Wizard's body (and his images'), built as a person's: a robe with a mantle and a high collar over his shoulders,
// a belt, a cape from his shoulders, and a long skirt from his hips in two panels, front and back, that part as his legs
// step so they never pass through it; bell sleeves; a hooked nose, bushy brows, a moustache, long white hair down his
// back and at his temples, and a beard in three joints that hangs from his chin; a tall pointed hat whose tip bends back
// in three joints. Beard, hat-tip, skirt and cape trail after him (WizardPoses). His staff is drawn in his right hand on
// its own (StaffLayer). Its texture is made by scripts/models/zombies.
public final class WizardModel<T extends MovingMonster> extends HumanoidModel<T> implements PlainDrawing {
    private static final float HIP_Y = 12.0F;
    private static final float NECK_Y = -12.0F;
    private static final float SHOULDER_X = 6.0F;
    private static final float SHOULDER_Y = -10.0F;
    private static final float LEG_X = 2.0F;
    // The cape hangs this far behind the trunk and this long; the skirt's back this deep and long below the hips.
    private static final float CAPE_Z = 2.2F;
    private static final float CAPE_LONG = 20.0F;
    private static final float SKIRT_DEEP = 3.5F;
    private static final float SKIRT_LONG = 9.0F;
    private final ModelPart beard;
    private final ModelPart beardTip;
    private final ModelPart beardPoint;
    private final ModelPart skirtFront;
    private final ModelPart skirtBack;
    private final ModelPart cape;
    private final ModelPart[] hatJoints;
    private final Quaternionf wrist = new Quaternionf();
    final WizardPoses pose = new WizardPoses();
    float alpha = 1.0F;

    public WizardModel(ModelPart root) {
        super(root);
        this.beard = this.head.getChild("beard");
        this.beardTip = this.beard.getChild("tip");
        this.beardPoint = this.beardTip.getChild("point");
        this.skirtFront = root.getChild("skirt_front");
        this.skirtBack = root.getChild("skirt_back");
        this.cape = this.body.getChild("cape");
        ModelPart mid = this.hat.getChild("mid");
        ModelPart top = mid.getChild("top");
        this.hatJoints = new ModelPart[] { mid, top, top.getChild("tip") };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0).addBox(-1.0F, -4.5F, -5.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(40, 0).addBox(-3.5F, -5.6F, -4.5F, 7.0F, 1.0F, 1.0F)
                .texOffs(40, 2).addBox(-3.5F, -2.4F, -4.6F, 7.0F, 2.0F, 1.0F)
                .texOffs(56, 0).addBox(-4.5F, -7.0F, 3.2F, 9.0F, 10.0F, 2.0F)
                .texOffs(78, 0).addBox(-4.6F, -6.0F, -1.0F, 1.0F, 8.0F, 4.0F)
                .texOffs(78, 0).mirror().addBox(3.6F, -6.0F, -1.0F, 1.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(88, 0)
                .addBox(-3.0F, 0.0F, -1.0F, 6.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, -4.2F, 0.08F,
                        0.0F, 0.0F));
        PartDefinition tip = beard.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(104, 0).addBox(-2.0F,
                0.0F, -1.0F, 4.0F, 5.0F, 2.0F), PartPose.offset(0.0F, 5.6F, 0.0F));
        tip.addOrReplaceChild("point", CubeListBuilder.create().texOffs(116, 0).addBox(-1.0F, 0.0F, -0.5F, 2.0F,
                3.0F, 1.0F), PartPose.offset(0.0F, 4.6F, 0.0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-7.0F, -8.8F, -7.0F, 14.0F, 1.0F, 14.0F)
                .texOffs(56, 16).addBox(-4.5F, -12.6F, -4.5F, 9.0F, 4.0F, 9.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
        PartDefinition mid = hat.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(92, 16)
                .addBox(-3.5F, -4.0F, -3.5F, 7.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, -12.4F, 0.3F,
                        -0.12F, 0.0F, 0.0F));
        PartDefinition top = mid.addOrReplaceChild("top", CubeListBuilder.create().texOffs(32, 5)
                .addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -3.8F, 0.2F,
                        -0.22F, 0.0F, 0.0F));
        top.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(88, 8).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                .texOffs(100, 8).addBox(-0.75F, -6.5F, -0.75F, 1.5F, 3.0F, 1.5F),
                PartPose.offsetAndRotation(0.0F, -3.8F, 0.2F, -0.4F, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(24, 32).addBox(-6.0F, -12.6F, -3.0F, 12.0F, 4.0F, 6.0F)
                .texOffs(60, 32).addBox(-4.5F, -14.2F, 1.6F, 9.0F, 3.0F, 1.0F)
                .texOffs(60, 36).addBox(-4.5F, -2.6F, -2.5F, 9.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(108, 40).addBox(-4.5F, 0.0F, 0.0F, 9.0F,
                CAPE_LONG, 1.0F), PartPose.offsetAndRotation(0.0F, -12.0F, CAPE_Z, 0.08F, 0.0F, 0.0F));
        root.addOrReplaceChild("skirt_front", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 12.0F, 3.0F)
                .texOffs(52, 48).addBox(-5.5F, 8.5F, -3.5F, 11.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        root.addOrReplaceChild("skirt_back", CubeListBuilder.create()
                .texOffs(26, 48).addBox(-5.0F, -1.0F, 0.0F, 10.0F, 12.0F, 3.0F)
                .texOffs(52, 54).addBox(-5.5F, 8.5F, 0.5F, 11.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(80, 44).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                    .texOffs(92, 29).addBox(-2.0F, -2.4F, -2.0F, 4.0F, 7.0F, 4.0F)
                    .texOffs(108, 29).addBox(-2.5F, 4.6F, -2.5F, 5.0F, 4.0F, 5.0F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offset(side * SHOULDER_X,
                    HIP_Y + SHOULDER_Y, 0.0F));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1).texOffs(92, 44).addBox(-2.0F, 0.0F, -2.0F,
                    4.0F, 12.0F, 4.0F);
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return Iterables.concat(super.bodyParts(), ImmutableList.of(this.skirtFront, this.skirtBack));
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
        for (ModelPart part : new ModelPart[] { this.head, this.beard, this.beardTip, this.beardPoint, this.hat,
                this.body, this.cape, this.skirtFront, this.skirtBack, this.rightArm, this.leftArm, this.rightLeg,
                this.leftLeg }) {
            part.resetPose();
        }
        for (ModelPart joint : this.hatJoints) {
            joint.resetPose();
        }
        WizardPoses p = this.pose;
        p.of(caster, limbSwing, limbSwingAmount, ageInTicks, WizardRenderer.floating(caster));
        float[] c = p.c;
        float hipY = HIP_Y + c[SINK] - c[LIFT];
        this.body.y = hipY;
        this.body.xRot = c[LEAN];
        this.body.yRot = c[TWIST];
        this.body.zRot = c[ROLL];
        float[] neck = Trunk.turn(0.0F, NECK_Y, 0.0F, c[LEAN], c[TWIST], c[ROLL]);
        this.head.x = neck[0];
        this.head.y = hipY + neck[1];
        this.head.z = neck[2];
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + c[NOD];
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + c[TWIST] * 0.5F;
        this.head.zRot = c[TILT];
        this.hat.copyFrom(this.head);
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
        this.skirt(c, hipY, p);
        this.beard(c, p);
        this.cape(c, p);
    }

    // The skirt's panels hang from the hips: the front swung forward as far as the leg that steps furthest forward, the
    // back as far as the one furthest back (a bent knee takes its shin further back), both trailing as he moves and
    // flaring as magic bursts out of him.
    private void skirt(float[] c, float hipY, WizardPoses p) {
        float forward = Math.min(c[R_LEG], c[L_LEG]);
        float back = Math.max(c[R_LEG] + 0.45F * c[R_KNEE], c[L_LEG] + 0.45F * c[L_KNEE]);
        float flare = 1.0F + 0.22F * Math.max(0.0F, c[FLARE]);
        float side = c[ROLL] * 0.4F + p.skirtSide;
        for (ModelPart panel : new ModelPart[] { this.skirtFront, this.skirtBack }) {
            panel.y = hipY;
            panel.yRot = c[TWIST] * 0.4F;
            panel.zRot = side;
            panel.xScale = flare;
            panel.zScale = flare;
        }
        this.skirtFront.xRot = Math.min(forward - 0.04F, p.skirt);
        this.skirtBack.xRot = Math.max(back + 0.04F, p.skirt);
    }

    // The beard hangs from the chin as its weight and his motion swing it, never into his chest: in the trunk's own
    // tilt it keeps a little in front of it.
    private void beard(float[] c, WizardPoses p) {
        float hang = Math.min(p.beard, c[LEAN] - 0.06F);
        float turn = hang - this.head.xRot;
        this.beard.xRot = 0.08F + turn * 0.5F;
        this.beardTip.xRot = turn * 0.3F;
        this.beardPoint.xRot = turn * 0.2F;
        this.beard.zRot = p.beardSide - this.head.zRot * 0.5F;
        this.beardTip.zRot = p.beardSide * 0.6F;
        this.beardPoint.zRot = p.beardSide * 0.4F;
    }

    // The cape swings out behind as he moves, but never into the skirt's back panel.
    private void cape(float[] c, WizardPoses p) {
        float clear = (1.1F + SKIRT_LONG * Mth.sin(this.skirtBack.xRot) + SKIRT_DEEP - 3.0F) / CAPE_LONG;
        float least = (float) Math.asin(Mth.clamp(clear, 0.0F, 0.9F)) - c[LEAN];
        this.cape.xRot = Math.max(0.08F + p.cape, least);
        this.cape.zRot = p.capeSide;
    }
}
