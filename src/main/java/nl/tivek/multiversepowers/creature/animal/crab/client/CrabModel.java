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

// The crab's body: a shell wider than it is long, domed in the middle, a spine at each front corner, over a paler
// underside, with mouthparts that flutter under its face; two eyes on stalks at its front edge; two arms that reach
// forward and fold inward at the wrist to heavy claws, each with a finger that opens and shuts against a fixed one;
// and four walking legs a side, each rising to a knee, coming down and ending in a pointed tip set on the ground. It
// faces the way its eyes look; CrabRenderer turns it side on to the way it goes. Its texture is painted by
// scripts/models/animals. CrabPoses moves it.
public final class CrabModel extends HierarchicalModel<Crab> {
    static final float BODY_Y = 21.0F;
    private static final float ARM_OUT = 0.35F;
    private static final float WRIST_IN = 1.0F;
    private static final float HAND_IN = 0.3F;
    private static final float KNEE_UP = 0.5F;
    private static final float KNEE_BEND = 1.4F;
    private static final float TIP_BEND = 0.45F;
    private static final float[] LEG_Z = { -1.8F, -0.3F, 1.2F, 2.7F };
    private static final float[] LEG_FAN = { -0.45F, -0.15F, 0.2F, 0.55F };
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart mouth;
    private final ModelPart[] eyes = new ModelPart[2];
    // Right, left: arm, wrist, hand, finger.
    private final ModelPart[][] arms = new ModelPart[2][4];
    // Right legs front to back, then left: upper, lower and tip.
    private final ModelPart[][] legs = new ModelPart[8][3];
    private final CrabPoses poses = new CrabPoses();

    public CrabModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.mouth = this.body.getChild("mouth");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "right_" : "left_";
            this.eyes[s] = this.body.getChild(name + "eye");
            ModelPart arm = this.body.getChild(name + "arm");
            ModelPart wrist = arm.getChild("wrist");
            ModelPart hand = wrist.getChild("hand");
            this.arms[s] = new ModelPart[] { arm, wrist, hand, hand.getChild("finger") };
            for (int k = 0; k < 4; k++) {
                ModelPart upper = this.body.getChild(name + "leg" + k);
                ModelPart lower = upper.getChild("lower");
                this.legs[s * 4 + k] = new ModelPart[] { upper, lower, lower.getChild("tip") };
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -2.0F, -3.0F, 9.0F, 3.0F, 6.0F)
                .texOffs(30, 0).addBox(-3.0F, 1.0F, -2.0F, 6.0F, 1.0F, 5.0F)
                .texOffs(0, 14).addBox(-3.5F, -3.0F, -2.0F, 7.0F, 1.0F, 4.0F)
                .texOffs(56, 0).addBox(-5.5F, -1.5F, -2.5F, 1.0F, 1.0F, 2.0F)
                .mirror().texOffs(56, 0).addBox(4.5F, -1.5F, -2.5F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("mouth", CubeListBuilder.create()
                .texOffs(52, 3).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -0.5F, -3.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            body.addOrReplaceChild(name + "eye", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(52, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.5F, -2.0F, -2.5F));
            PartDefinition arm = body.addOrReplaceChild(name + "arm", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 9).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
                    PartPose.offsetAndRotation(side * 3.5F, 0.5F, -3.0F, 0.0F, -side * ARM_OUT, 0.0F));
            PartDefinition wrist = arm.addOrReplaceChild("wrist", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(34, 11).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, side * WRIST_IN, 0.0F));
            // The hand with its fixed finger below; the moving finger shuts down onto it.
            PartDefinition hand = wrist.addOrReplaceChild("hand", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(22, 14).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 2.0F)
                    .texOffs(34, 15).addBox(-1.0F, 0.5F, -4.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, 0.0F, side * HAND_IN, 0.0F));
            hand.addOrReplaceChild("finger", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(42, 15).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, -0.5F, -2.0F));
            for (int k = 0; k < 4; k++) {
                // Out and up to the knee, then down, the tip set on the ground.
                PartDefinition upper = body.addOrReplaceChild(name + "leg" + k, CubeListBuilder.create()
                        .mirror(s == 1).texOffs(34, 9).addBox(s == 0 ? -3.0F : 0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 4.5F, 0.2F, LEG_Z[k], 0.0F, -side * LEG_FAN[k],
                                -side * KNEE_UP));
                PartDefinition lower = upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(s == 1)
                        .texOffs(42, 9).addBox(s == 0 ? -3.0F : 0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, side * KNEE_BEND));
                lower.addOrReplaceChild("tip", CubeListBuilder.create().mirror(s == 1)
                        .texOffs(50, 9).addBox(s == 0 ? -2.0F : 0.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, side * TIP_BEND));
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
        this.mouth.xRot = c[CrabPoses.MOUTH];
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            int arm = CrabPoses.ARMS + s * CrabPoses.PER_ARM;
            this.eyes[s].zRot = side * c[CrabPoses.EYES] + c[CrabPoses.EYES_SIDE];
            this.eyes[s].xRot = c[CrabPoses.EYES_BACK];
            this.arms[s][0].yRot = -side * (ARM_OUT + c[arm]);
            this.arms[s][0].xRot = c[arm + 1];
            // Folding in is shared by the wrist and the hand, the wrist taking most.
            this.arms[s][1].yRot = side * (WRIST_IN + 0.6F * c[arm + 2]);
            this.arms[s][2].yRot = side * (HAND_IN + 0.4F * c[arm + 2]);
            this.arms[s][2].zRot = side * c[arm + 4];
            this.arms[s][3].xRot = -c[arm + 3];
            for (int k = 0; k < 4; k++) {
                int leg = CrabPoses.LEGS + (s * 4 + k) * CrabPoses.PER_LEG;
                ModelPart[] parts = this.legs[s * 4 + k];
                parts[0].zRot = -side * (KNEE_UP + c[leg]);
                parts[1].zRot = side * (KNEE_BEND - c[leg + 1]);
                parts[2].zRot = side * (TIP_BEND - c[leg + 2]);
            }
        }
    }
}
