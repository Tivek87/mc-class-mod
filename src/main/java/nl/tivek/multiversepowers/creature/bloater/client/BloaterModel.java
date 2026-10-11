package nl.tivek.multiversepowers.creature.bloater.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.client.Trunk;

// The Bloater's body, built as a person's (head, trunk, two arms, two legs, so it goes limp and gets up as one): a
// small head with a heavy jaw on a huge round belly under a fat chest, short thick arms held out by the belly, short
// legs, and boils all over its trunk, each a part of its own. Its texture is made by scripts/models/zombies. It poses
// itself (BloaterPoses): a waddle, heaving breath, boils that pulse and crawl, and its moves on the server's timeline;
// rushing it swells and its boils swell with it.
public final class BloaterModel extends HumanoidModel<Bloater> {
    static final float LEAN = 0.08F;
    private static final float HIP_Y = 13.0F;
    private static final float HIP_Z = 0.5F;
    private static final float NECK_Y = -17.0F;
    private static final float NECK_Z = -1.0F;
    private static final float SHOULDER_X = 9.5F;
    private static final float SHOULDER_Y = -15.5F;
    private static final float LEG_X = 4.5F;
    private final ModelPart jaw;
    private final ModelPart[] boils = new ModelPart[Bloater.BOILS];
    private final BloaterPoses pose = new BloaterPoses();
    // How bright the boils of the one just posed glow, 0 to 1, for the glow drawn over it.
    float glow;

    public BloaterModel(ModelPart root) {
        super(root);
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < Bloater.BOILS; i++) {
            this.boils[i] = this.body.getChild("boil" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float[] neck = Trunk.turn(0.0F, NECK_Y, NECK_Z, LEAN, 0.0F, 0.0F);
        float[] shoulder = Trunk.turn(0.0F, SHOULDER_Y, 0.0F, LEAN, 0.0F, 0.0F);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -7.0F, -4.5F, 8.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, HIP_Y + neck[1], HIP_Z + neck[2]));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, 0.0F, -5.0F, 8.0F, 3.0F,
                7.0F), PartPose.offset(0.0F, -1.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-8.0F, -12.0F, -7.0F, 16.0F, 12.0F, 13.0F)
                .texOffs(0, 42).addBox(-7.0F, -18.0F, -5.0F, 14.0F, 7.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, HIP_Y, HIP_Z, LEAN, 0.0F, 0.0F));
        for (int i = 0; i < Bloater.BOILS; i++) {
            float[] at = Bloater.BOIL_AT[i];
            float size = at[3];
            body.addOrReplaceChild("boil" + i, CubeListBuilder.create().texOffs(96, size > 3.5F ? 8 : 0)
                    .addBox(-size / 2.0F, -size / 2.0F, -size / 2.0F, size, size, size), PartPose.offset(at[0], at[1],
                            at[2]));
        }
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            CubeListBuilder arm = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 62).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 7.0F, 6.0F)
                    .texOffs(24, 62).addBox(-2.5F, 5.0F, -2.5F, 5.0F, 6.0F, 5.0F)
                    .texOffs(44, 62).addBox(-2.5F, 11.0F, -3.0F, 5.0F, 3.0F, 6.0F);
            root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm", arm, PartPose.offsetAndRotation(side
                    * SHOULDER_X, HIP_Y + shoulder[1], HIP_Z + shoulder[2], 0.0F, 0.0F, -side * 0.3F));
            CubeListBuilder leg = CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 78).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                    .texOffs(24, 78).addBox(-2.5F, 5.0F, -2.5F, 5.0F, 4.0F, 5.0F)
                    .texOffs(44, 78).addBox(-3.0F, 9.0F, -4.0F, 6.0F, 2.0F, 7.0F);
            root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * LEG_X, HIP_Y,
                    HIP_Z));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(Bloater bloater, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.resetPose();
        this.jaw.resetPose();
        this.hat.resetPose();
        this.body.resetPose();
        this.rightArm.resetPose();
        this.leftArm.resetPose();
        this.rightLeg.resetPose();
        this.leftLeg.resetPose();
        BloaterPoses p = this.pose;
        p.of(bloater, limbSwing, limbSwingAmount, ageInTicks);
        float swell = Mth.clamp(p.swell, 0.0F, 1.2F);
        this.glow = Mth.clamp(0.18F + 0.1F * Mth.sin(ageInTicks * 0.2F) + 0.85F * swell + (bloater.hurtTime > 0
                ? 0.35F : 0.0F), 0.0F, 1.0F);
        float wide = 1.0F + 0.16F * swell;
        float tall = 1.0F + 0.07F * swell;
        float lean = LEAN + p.lean;
        this.body.xRot = lean;
        this.body.yRot = p.twist;
        this.body.zRot = p.roll;
        this.body.y += p.sink;
        this.body.xScale = wide;
        this.body.zScale = wide;
        this.body.yScale = tall;
        float[] neck = Trunk.turn(0.0F, NECK_Y * tall, NECK_Z, lean, p.twist, p.roll);
        this.head.x = neck[0];
        this.head.y = HIP_Y + p.sink + neck[1];
        this.head.z = HIP_Z + neck[2];
        this.head.xRot = headPitch * Mth.DEG_TO_RAD + p.nod;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + p.twist * 0.5F + p.shake;
        this.head.zRot = p.roll * 0.6F;
        this.jaw.xRot = p.jaw;
        ModelPart[] arms = { this.rightArm, this.leftArm };
        ModelPart[] legs = { this.rightLeg, this.leftLeg };
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            float[] at = Trunk.turn(side * SHOULDER_X * wide, SHOULDER_Y * tall, 0.0F, lean, p.twist, p.roll);
            ModelPart arm = arms[s];
            arm.x = at[0];
            arm.y = HIP_Y + p.sink + at[1];
            arm.z = HIP_Z + at[2];
            arm.xRot = p.arm[s];
            arm.yRot = p.twist;
            arm.zRot = -side * (0.3F + 0.25F * swell + p.spread[s]) + p.roll;
            ModelPart leg = legs[s];
            leg.xRot = p.leg[s];
            leg.zRot = -side * (0.05F + p.legSpread);
        }
        // The boils pulse each in its own time and crawl a little under the skin; rushing, they swell.
        for (int i = 0; i < Bloater.BOILS; i++) {
            ModelPart boil = this.boils[i];
            boil.resetPose();
            boil.visible = bloater.boil(i);
            float phase = i * 1.7F + bloater.getId() * 0.3F;
            float size = 1.0F + 0.13F * Mth.sin(ageInTicks * 0.21F + phase) + swell * (0.6F + 0.12F
                    * Mth.sin(ageInTicks * 0.9F + phase));
            boil.xScale = size;
            boil.yScale = size;
            boil.zScale = size;
            boil.x += 0.3F * Mth.sin(ageInTicks * 0.07F + phase);
            boil.y += 0.25F * Mth.cos(ageInTicks * 0.09F + phase * 1.3F);
        }
    }
}
