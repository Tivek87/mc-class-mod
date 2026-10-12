package nl.tivek.multiversepowers.creature.animal.crab.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;

// The crab's body: a shell wider than it is long over a paler underside, two eyes on stalks at its front edge, two
// arms that reach forward and bend inward to heavy claws, each with a finger that opens and shuts, and four walking
// legs a side, each rising to a knee and coming down to a point. It faces the way its eyes look; CrabRenderer turns
// it side on to the way it goes. Its texture is painted by scripts/models/animals. CrabPoses moves it.
public final class CrabModel extends HierarchicalModel<Crab> {
    static final float BODY_Y = 21.5F;
    private static final float ARM_OUT = 0.5F;
    private static final float CLAW_IN = 1.1F;
    private static final float KNEE_UP = 0.5F;
    private static final float KNEE_BEND = 1.7F;
    private static final float[] LEG_Z = { -1.8F, -0.3F, 1.2F, 2.7F };
    private static final float[] LEG_FAN = { -0.45F, -0.15F, 0.2F, 0.55F };
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] eyes = new ModelPart[2];
    // Right, left: arm, claw, finger.
    private final ModelPart[][] arms = new ModelPart[2][3];
    // Right legs front to back, then left: upper and lower.
    private final ModelPart[][] legs = new ModelPart[8][2];
    private final CrabPoses poses = new CrabPoses();

    public CrabModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "right_" : "left_";
            this.eyes[s] = this.body.getChild(name + "eye");
            ModelPart arm = this.body.getChild(name + "arm");
            ModelPart claw = arm.getChild("claw");
            this.arms[s] = new ModelPart[] { arm, claw, claw.getChild("finger") };
            for (int k = 0; k < 4; k++) {
                ModelPart upper = this.body.getChild(name + "leg" + k);
                this.legs[s * 4 + k] = new ModelPart[] { upper, upper.getChild("lower") };
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -2.0F, -3.0F, 9.0F, 3.0F, 6.0F)
                .texOffs(30, 0).addBox(-3.0F, 1.0F, -2.0F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            body.addOrReplaceChild(name + "eye", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(52, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.5F, -2.0F, -2.5F));
            PartDefinition arm = body.addOrReplaceChild(name + "arm", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 9).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
                    PartPose.offsetAndRotation(side * 3.5F, 0.5F, -3.0F, 0.0F, -side * ARM_OUT, 0.0F));
            PartDefinition claw = arm.addOrReplaceChild("claw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(10, 9).addBox(-1.0F, -1.5F, -2.0F, 2.0F, 3.0F, 2.0F)
                    .texOffs(18, 9).addBox(-1.0F, 0.5F, -4.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, side * CLAW_IN, 0.0F));
            claw.addOrReplaceChild("finger", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(26, 9).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, -0.5F, -2.0F));
            for (int k = 0; k < 4; k++) {
                // Out and up to the knee, then down to the ground.
                PartDefinition upper = body.addOrReplaceChild(name + "leg" + k, CubeListBuilder.create()
                        .mirror(s == 1).texOffs(34, 9).addBox(s == 0 ? -3.0F : 0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 4.5F, 0.2F, LEG_Z[k], 0.0F, -side * LEG_FAN[k],
                                -side * KNEE_UP));
                upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(s == 1)
                        .texOffs(42, 9).addBox(s == 0 ? -4.0F : 0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, side * KNEE_BEND));
            }
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Crab crab, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - crab.tickCount;
        float[] c = this.poses.of(crab, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        this.body.y = BODY_Y + c[CrabPoses.BOB] + c[CrabPoses.SINK];
        this.body.z = c[CrabPoses.LUNGE];
        this.body.xRot = c[CrabPoses.PITCH];
        this.body.zRot = c[CrabPoses.ROLL];
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            int arm = CrabPoses.ARMS + s * CrabPoses.PER_ARM;
            this.eyes[s].zRot = side * c[CrabPoses.EYES] + c[CrabPoses.EYES_SIDE];
            this.eyes[s].xRot = c[CrabPoses.EYES_BACK];
            this.arms[s][0].yRot = -side * (ARM_OUT + c[arm]);
            this.arms[s][0].xRot = c[arm + 1];
            this.arms[s][1].yRot = side * (CLAW_IN + c[arm + 2]);
            this.arms[s][2].xRot = -c[arm + 3];
            for (int k = 0; k < 4; k++) {
                int leg = CrabPoses.LEGS + (s * 4 + k) * CrabPoses.PER_LEG;
                this.legs[s * 4 + k][0].zRot = -side * (KNEE_UP + c[leg]);
                this.legs[s * 4 + k][1].zRot = side * (KNEE_BEND - c[leg + 1]);
            }
        }
    }
}
