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
import nl.tivek.multiversepowers.engine.client.pose.Shoulders;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The Wizard's body (and his images'), built as a person's: robe with a mantle over his shoulders, a belt, a cape from
// his shoulders, and a long skirt from his hips in two panels, front and back, that part as his legs step so they never
// pass through it; bell sleeves; a hooked nose, bushy brows, a moustache parted over his mouth, white hair at his
// temples and the back of his head that ends above his shoulders and hangs on down his back over the cape, and a beard
// in three joints, narrowing to a point, that hangs on his chest from under his chin (a part of the trunk, so a turning
// head never swings it into the mantle; it lies over an arm crossing his chest); a tall hat, its tip bending back
// in three joints. Beard, hat-tip, skirt and cape trail after him (WizardPoses). Grander each phase: pauldrons on his
// shoulders from his second, a crown of spikes round his hat's brim from his third, curling horns through his hat and a
// longer cape from his fourth (the rest of each phase's dress is its texture and RegaliaLayer). His staff is drawn in
// his right hand on its own (StaffLayer). Its textures are made by scripts/models/zombies.
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
    static final int SPIKES = 7;
    // How much longer his cape hangs from his fourth phase.
    private static final float CAPE_LONGER = 1.15F;
    // The beard's root never lies further back than this on the chest: its back stays clear of the mantle's front.
    private static final float BEARD_FRONT = -4.0F;
    private static final float BEARD_LONG = 14.0F;
    // An arm as the beard meets it: points along it, its upper arm's length to the elbow, its whole length to the
    // fingers, and how thick its sleeve is round its middle line.
    private static final int ARM_SAMPLES = 6;
    private static final float UPPER_ARM = 5.0F;
    private static final float ARM_LONG = 11.0F;
    private static final float SLEEVE = 2.0F;
    private final ModelPart beard;
    private final ModelPart beardTip;
    private final ModelPart beardPoint;
    private final ModelPart skirtFront;
    private final ModelPart skirtBack;
    private final ModelPart cape;
    private final ModelPart[] hatJoints;
    private final ModelPart[] pauldrons;
    private final ModelPart[] spikes;
    private final ModelPart[] horns;
    private final Quaternionf wrist = new Quaternionf();
    private final Quaternionf turn = new Quaternionf();
    private final Vector3f chin = new Vector3f();
    private final Quaternionf bodyTurn = new Quaternionf();
    private final Vector3f point = new Vector3f();
    final WizardPoses pose = new WizardPoses();
    float alpha = 1.0F;

    public WizardModel(ModelPart root) {
        super(root);
        this.beard = this.body.getChild("beard");
        this.beardTip = this.beard.getChild("tip");
        this.beardPoint = this.beardTip.getChild("point");
        this.skirtFront = root.getChild("skirt_front");
        this.skirtBack = root.getChild("skirt_back");
        this.cape = this.body.getChild("cape");
        ModelPart mid = this.hat.getChild("mid");
        ModelPart top = mid.getChild("top");
        this.hatJoints = new ModelPart[] { mid, top, top.getChild("tip") };
        this.pauldrons = new ModelPart[] { this.rightArm.getChild("pauldron"), this.leftArm.getChild("pauldron") };
        this.spikes = new ModelPart[SPIKES];
        for (int k = 0; k < SPIKES; k++) {
            this.spikes[k] = this.hat.getChild("spike" + k);
        }
        this.horns = new ModelPart[] { this.hat.getChild("right_horn"), this.hat.getChild("left_horn") };
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The face: a nose high enough to leave the mouth showing between the moustache's halves; brows over the eyes;
        // hair at the back and temples that ends above his shoulders (the rest hangs down his back from the cape).
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0).addBox(-1.0F, -5.3F, -5.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(40, 0).addBox(-3.5F, -6.3F, -4.6F, 7.0F, 1.0F, 1.0F)
                .texOffs(40, 2).addBox(-3.5F, -2.5F, -4.7F, 7.0F, 2.0F, 1.0F)
                .texOffs(56, 0).addBox(-4.5F, -7.6F, 3.2F, 9.0F, 7.0F, 2.0F)
                .texOffs(78, 0).addBox(-4.6F, -6.7F, -1.0F, 1.0F, 6.0F, 4.0F)
                .texOffs(78, 0).mirror().addBox(3.6F, -6.7F, -1.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, 0.0F));
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
        // The crown round the brim, its middle spike over his brow the tallest, each leaning out a little.
        for (int k = 0; k < SPIKES; k++) {
            float angle = -Mth.HALF_PI + k * Mth.TWO_PI / SPIKES;
            float tall = k == 0 ? 1.5F : 1.0F;
            hat.addOrReplaceChild("spike" + k, CubeListBuilder.create().texOffs(64, 64).addBox(-0.5F, -3.0F * tall,
                    -0.5F, 1.0F, 3.0F * tall, 1.0F), PartPose.offsetAndRotation(Mth.cos(angle) * 5.6F, -8.6F,
                    Mth.sin(angle) * 5.6F, Mth.sin(angle) * 0.3F, 0.0F, -Mth.cos(angle) * 0.3F));
        }
        // Horns through the hat's band, sweeping out and back and curling up at their points.
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            PartDefinition horn = hat.addOrReplaceChild(s == 0 ? "right_horn" : "left_horn", CubeListBuilder.create()
                    .mirror(s == 1).texOffs(72, 64).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(side * 4.2F, -10.4F, 0.6F, 0.25F, 0.0F, side * 1.05F));
            PartDefinition bend = horn.addOrReplaceChild("mid", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(82, 64).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -3.8F, 0.0F, 0.35F, 0.0F, -side * 0.6F));
            bend.addOrReplaceChild("tip", CubeListBuilder.create().mirror(s == 1).texOffs(88, 64).addBox(-0.5F,
                    -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.8F, 0.0F, 0.3F, 0.0F,
                    -side * 0.55F));
        }
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(24, 32).addBox(-6.0F, -12.0F, -3.0F, 12.0F, 4.0F, 6.0F)
                .texOffs(60, 36).addBox(-4.5F, -2.6F, -2.5F, 9.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, HIP_Y, 0.0F));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(108, 40).addBox(-4.5F,
                0.0F, 0.0F, 9.0F, CAPE_LONG, 1.0F), PartPose.offsetAndRotation(0.0F, -12.0F, CAPE_Z, 0.08F, 0.0F,
                0.0F));
        cape.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(88, 80).addBox(-4.0F, 0.0F, 0.0F, 8.0F,
                10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 1.0F));
        // The beard hangs on the chest from where the chin is (placed every frame), never into the mantle.
        PartDefinition beard = body.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(40, 80)
                .addBox(-3.5F, 0.0F, -1.6F, 7.0F, 5.0F, 2.0F), PartPose.offset(0.0F, -12.0F, -4.4F));
        PartDefinition tip = beard.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(60, 80).addBox(-2.5F,
                0.0F, -1.4F, 5.0F, 5.0F, 2.0F), PartPose.offset(0.0F, 4.6F, 0.0F));
        tip.addOrReplaceChild("point", CubeListBuilder.create().texOffs(76, 80).addBox(-1.5F, 0.0F, -0.9F, 3.0F,
                4.0F, 1.0F), PartPose.offset(0.0F, 4.6F, 0.0F));
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
            PartDefinition limb = root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm,
                    PartPose.offset(side * SHOULDER_X, HIP_Y + SHOULDER_Y, 0.0F));
            limb.addOrReplaceChild("pauldron", CubeListBuilder.create().mirror(s == 1).texOffs(40, 64)
                    .addBox(-3.0F, -3.3F, -2.5F, 6.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(side * 0.4F, 0.0F,
                    0.0F, 0.0F, 0.0F, side * 0.28F));
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
        int phase = WizardRenderer.phase(caster);
        for (ModelPart pauldron : this.pauldrons) {
            pauldron.visible = phase >= 2;
        }
        for (ModelPart spike : this.spikes) {
            spike.visible = phase >= 3;
        }
        for (ModelPart horn : this.horns) {
            horn.visible = phase >= 4;
        }
        this.cape.yScale = phase >= 4 ? CAPE_LONGER : 1.0F;
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
        // His neck turns only so far: further, the brim, hair and beard would cut into his shoulders.
        this.head.xRot = Mth.clamp(headPitch, -35.0F, 25.0F) * Mth.DEG_TO_RAD + c[NOD];
        this.head.yRot = Mth.clamp(netHeadYaw, -40.0F, 40.0F) * Mth.DEG_TO_RAD + c[TWIST] * 0.5F;
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
        Limbs.Joint[] ankles = { Limbs.Joint.RIGHT_ANKLE, Limbs.Joint.LEFT_ANKLE };
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
            // The foot stands flat on the ground however the leg swings and the knee bends, then turns from there.
            float shin = c[R_LEG + s] + Math.max(0.0F, c[R_KNEE + s]);
            Limbs.turn(this, ankles[s], this.wrist.rotationX(-shin + c[R_FOOT + s]));
        }
        // The spine bends rather than tipping whole: the chest takes the lean and twist, the belly under it turned back
        // part of the way, so the hips stay nearer upright and turn less than the shoulders.
        Limbs.waist(this, this.wrist.rotationYXZ(-0.4F * c[TWIST], -0.3F * c[LEAN], -0.35F * c[ROLL]));
        // The shoulder blades follow the arms: up as an arm rises overhead, ahead as it reaches.
        Shoulders.follow(this, 1.0F);
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

    // The beard hangs from under the chin, wherever the head has turned it, as its weight and his motion swing it: on
    // the chest, it keeps in front of the mantle and the robe whatever the head does, turning with the head only a
    // little, and never swings back into him.
    private void beard(float[] c, WizardPoses p) {
        this.turn.rotationZYX(this.head.zRot, this.head.yRot, this.head.xRot)
                .transform(this.chin.set(0.0F, -0.8F, -4.4F))
                .add(this.head.x - this.body.x, this.head.y - this.body.y, this.head.z - this.body.z);
        this.turn.rotationZYX(this.body.zRot, this.body.yRot, this.body.xRot).conjugate().transform(this.chin);
        this.beard.x = Mth.clamp(this.chin.x, -2.0F, 2.0F);
        this.beard.y = this.chin.y;
        this.beard.z = Math.min(this.chin.z, BEARD_FRONT);
        float hang = Math.min(p.beard - c[LEAN], -0.06F);
        // An arm across his chest lifts the beard over it rather than passing through.
        hang = Math.min(hang, this.overArms(c, hang));
        this.beard.xRot = hang * 0.6F;
        this.beardTip.xRot = hang * 0.25F;
        this.beardPoint.xRot = hang * 0.15F;
        this.beard.yRot = Mth.clamp((this.head.yRot - this.body.yRot) * 0.35F, -0.3F, 0.3F);
        this.beard.zRot = p.beardSide * 0.7F;
        this.beardTip.zRot = p.beardSide * 0.5F;
        this.beardPoint.zRot = p.beardSide * 0.3F;
    }

    // The steepest the beard may hang (radians, trunk frame, below 0 forward) to lie in front of both arms where
    // they cross before his chest: points along each upper arm and forearm (the elbow folding the forearm ahead) in the
    // trunk's frame, each pushing the beard out by the sleeve's thickness. The beard's three joints bend 1 in all.
    private float overArms(float[] c, float hang) {
        float most = 0.0F;
        this.bodyTurn.rotationZYX(this.body.zRot, this.body.yRot, this.body.xRot).conjugate();
        ModelPart[] arms = { this.rightArm, this.leftArm };
        for (int s = 0; s < 2; s++) {
            ModelPart arm = arms[s];
            this.turn.rotationZYX(arm.zRot, arm.yRot, arm.xRot);
            float elbow = Math.max(0.0F, c[R_ELBOW + s]);
            for (int k = 1; k <= ARM_SAMPLES; k++) {
                float along = ARM_LONG * k / ARM_SAMPLES;
                if (along <= UPPER_ARM) {
                    this.point.set(0.0F, along, 0.0F);
                } else {
                    float fore = along - UPPER_ARM;
                    this.point.set(0.0F, UPPER_ARM + fore * Mth.cos(elbow), -fore * Mth.sin(elbow));
                }
                this.turn.transform(this.point).add(arm.x - this.body.x, arm.y - this.body.y, arm.z - this.body.z);
                this.bodyTurn.transform(this.point);
                float down = this.point.y - this.beard.y;
                if (down < 1.0F || down > BEARD_LONG || Math.abs(this.point.x - this.beard.x) > 3.5F + SLEEVE) {
                    continue;
                }
                // An arm reaching out well in front of the beard leaves it be.
                if (this.point.z + SLEEVE < this.beard.z - 2.1F + down * Mth.sin(hang)) {
                    continue;
                }
                float room = this.point.z - SLEEVE - this.beard.z - 0.4F;
                most = Math.min(most, (float) Math.asin(Mth.clamp(room / down, -0.95F, 0.0F)));
            }
        }
        return most == 0.0F ? 0.0F : most / 0.6F;
    }

    // The cape swings out behind as he moves, but never into the skirt's back panel.
    private void cape(float[] c, WizardPoses p) {
        float clear = (1.1F + SKIRT_LONG * Mth.sin(this.skirtBack.xRot) + SKIRT_DEEP - 3.0F) / CAPE_LONG;
        float least = (float) Math.asin(Mth.clamp(clear, 0.0F, 0.9F)) - c[LEAN];
        this.cape.xRot = Math.max(0.08F + p.cape, least);
        this.cape.zRot = p.capeSide;
    }
}
