package nl.tivek.multiversepowers.creature.rustbound.client;

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
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;

// The Rustbound's body, built as a person's: a skull with a riveted plate on one side and a bolt through it, a jaw
// wired shut; a gaunt trunk in a torn coat, plates of rusted scrap riveted down its back and rods of rebar sticking out
// of it, the magnet core sunk in its chest; on its right arm a heavy fist of scrap with a pauldron on the shoulder and
// spikes on the knuckles, its left arm thin and wound with wire. Its texture is made by scripts/models/zombies. It poses
// itself (RustboundPoses): a heavy, lopsided lurch, the core pulsing, and its moves on the server's timeline.
public final class RustboundModel extends HumanoidModel<Rustbound> {
    static final float LEAN = 0.2F;
    private static final float HIP_Y = 12.0F;
    private static final float NECK_Y = -12.5F;
    private static final float NECK_Z = -0.5F;
    // The heavy right arm hangs a little wider than the thin left one.
    private static final float[] SHOULDER_X = { 5.5F, 4.8F };
    private static final float SHOULDER_Y = -11.0F;
    private static final float LEG_X = 2.0F;
    private final ModelPart jaw;
    private final ModelPart core;
    private final RustboundPoses pose = new RustboundPoses();
    // How bright the core of the one just posed glows, 0 to 1, for the glow drawn over it.
    float glow;

    public RustboundModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
        this.core = this.body.getChild("core");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, HIP_Y + NECK_Y, NECK_Z));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(28, 0).addBox(-3.0F, 0.0F, -4.5F, 6.0F, 2.0F,
                5.0F), PartPose.offset(0.0F, -1.0F, 0.5F));
        head.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, -3.0F, -3.0F, 1.0F, 5.0F,
                5.0F), PartPose.offsetAndRotation(3.4F, -4.0F, 0.2F, 0.0F, 0.0F, -0.1F));
        head.addOrReplaceChild("bolt", CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F,
                3.0F), PartPose.offsetAndRotation(-2.0F, -5.5F, -3.0F, 0.2F, 0.5F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -12.0F, -2.5F, 8.0F, 8.0F, 5.0F)
                .texOffs(26, 16).addBox(-3.5F, -4.0F, -2.0F, 7.0F, 4.0F, 4.0F)
                .texOffs(48, 16).addBox(-4.0F, -12.0F, -2.5F, 8.0F, 11.0F, 5.0F, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, HIP_Y, 0.0F, LEAN, 0.0F, 0.0F));
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(76, 16).addBox(-1.5F, -1.5F, -1.0F, 3.0F,
                3.0F, 1.0F), PartPose.offset(0.5F, -8.0F, -2.6F));
        plate(body, "plate0", 0, -3.0F, -2.5F, 6, 5, 0.0F, -9.0F, 2.7F, -0.15F, 0.0F, 0.12F);
        plate(body, "plate1", 14, -2.5F, -2.0F, 5, 4, -1.5F, -4.5F, 2.4F, 0.05F, 0.0F, -0.2F);
        plate(body, "plate2", 26, -2.0F, -3.0F, 4, 6, -3.0F, -10.0F, 2.1F, 0.0F, 0.5F, 0.3F);
        plate(body, "plate3", 38, -3.5F, -1.0F, 7, 2, 0.5F, -12.2F, 2.3F, -0.4F, 0.0F, 0.0F);
        body.addOrReplaceChild("rebar0", CubeListBuilder.create().texOffs(56, 96).addBox(-0.5F, -7.0F, -0.5F, 1.0F,
                7.0F, 1.0F), PartPose.offsetAndRotation(2.5F, -11.0F, 1.5F, -0.5F, 0.0F, 0.35F));
        body.addOrReplaceChild("rebar1", CubeListBuilder.create().texOffs(56, 96).addBox(-0.5F, -7.0F, -0.5F, 1.0F,
                7.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, -6.0F, 2.4F, -1.0F, 0.0F, -0.25F));
        body.addOrReplaceChild("rebar2", CubeListBuilder.create().texOffs(60, 96).addBox(-0.5F, -4.0F, -0.5F, 1.0F,
                4.0F, 1.0F), PartPose.offsetAndRotation(2.8F, -3.5F, 1.8F, -0.6F, 0.0F, 0.9F));
        float[] right = Trunk.turn(-SHOULDER_X[0], SHOULDER_Y, 0.0F, LEAN, 0.0F, 0.0F);
        PartDefinition fist = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(12, 32).addBox(-2.5F, 4.0F, -2.5F, 5.0F, 7.0F, 5.0F)
                .texOffs(32, 32).addBox(-2.5F, 11.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.offset(right[0], HIP_Y + right[1], right[2]));
        fist.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(52, 32).addBox(-3.0F, -3.5F, -3.0F, 6.0F,
                3.0F, 6.0F), PartPose.offsetAndRotation(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
        for (int k = 0; k < 2; k++) {
            fist.addOrReplaceChild("spike" + k, CubeListBuilder.create().texOffs(76, 32).addBox(-0.5F, 0.0F, -0.5F,
                    1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(k == 0 ? -1.3F : 1.3F, 13.5F, -2.3F, -1.5F, 0.0F,
                            0.0F));
        }
        float[] left = Trunk.turn(SHOULDER_X[1], SHOULDER_Y, 0.0F, LEAN, 0.0F, 0.0F);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F)
                .texOffs(8, 48).addBox(-1.0F, 12.0F, -1.5F, 2.0F, 3.0F, 2.0F)
                .texOffs(16, 48).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.2F))
                .texOffs(24, 48).addBox(-1.0F, 7.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.2F)),
                PartPose.offset(left[0], HIP_Y + left[1], left[2]));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F)
                    .texOffs(12, 64).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.25F));
            if (s == 0) {
                leg.texOffs(28, 64).addBox(-2.0F, 5.0F, -2.2F, 4.0F, 4.0F, 1.0F);
            }
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    0.0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    // A riveted plate of scrap on its back: `u` its column in the texture's plate row.
    private static void plate(PartDefinition body, String name, int u, float x0, float y0, int w, int h, float x,
            float y, float z, float xRot, float yRot, float zRot) {
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, 96).addBox(x0, y0, -0.5F, w, h, 1.0F),
                PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
    }

    @Override
    public void setupAnim(Rustbound rustbound, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.core.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        RustboundPoses p = this.pose;
        p.of(rustbound, limbSwing, limbSwingAmount, ageInTicks);
        this.glow = Mth.clamp(p.glow + (rustbound.hurtTime > 0 ? 0.2F : 0.0F), 0.0F, 1.0F);
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
        this.jaw.xRot = p.jaw;
        // The core swells a little as it flares.
        float swell = 1.0F + 0.25F * Math.max(0.0F, p.glow - 0.5F);
        this.core.xScale = swell;
        this.core.yScale = swell;
        ModelPart[] arms = { this.rightArm, this.leftArm };
        ModelPart[] legs = { this.rightLeg, this.leftLeg };
        Limbs.Joint[] elbows = { Limbs.Joint.RIGHT_ELBOW, Limbs.Joint.LEFT_ELBOW };
        Limbs.Joint[] knees = { Limbs.Joint.RIGHT_KNEE, Limbs.Joint.LEFT_KNEE };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float[] at = Trunk.turn(side * SHOULDER_X[s], SHOULDER_Y, 0.0F, lean, p.twist, p.roll);
            ModelPart arm = arms[s];
            arm.x = at[0];
            arm.y = hipY + at[1];
            arm.z = at[2];
            arm.xRot = p.arm[s];
            arm.yRot = p.twist;
            arm.zRot = -side * (0.06F + p.armOut[s]) + p.roll;
            Limbs.bend(this, elbows[s], p.elbow[s]);
            ModelPart leg = legs[s];
            leg.y = hipY;
            leg.xRot = p.leg[s];
            leg.zRot = -side * (0.04F + p.legOut);
            Limbs.bend(this, knees[s], p.knee[s]);
        }
    }
}
