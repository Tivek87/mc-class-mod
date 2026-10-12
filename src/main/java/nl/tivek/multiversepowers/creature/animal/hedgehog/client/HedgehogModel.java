package nl.tivek.multiversepowers.creature.animal.hedgehog.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;

// The hedgehog's body: a low round trunk on four short legs under a coat of spines that domes over its back and
// sides, a small head with a pointed snout, a black nose and round ears, and a stub of a tail. Rolled up, its head and
// feet are drawn in under the spines and the trunk rounds into a ball. Its texture is painted by
// scripts/models/animals. HedgehogPoses moves it.
public final class HedgehogModel extends HierarchicalModel<Hedgehog> {
    static final float BODY_Y = 21.0F;
    private static final float YOUNG_HEAD = 1.25F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart spines;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart[] ears = new ModelPart[2];
    // Right front, left front, right hind, left hind.
    private final ModelPart[] legs = new ModelPart[4];
    private final HedgehogPoses poses = new HedgehogPoses();

    public HedgehogModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.spines = this.body.getChild("spines");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            this.legs[k] = this.body.getChild(names[k]);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2.0F, -3.5F, 5.0F, 4.0F, 7.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("spines", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 4.0F, 7.0F)
                .texOffs(0, 11).addBox(-2.0F, -3.5F, -2.0F, 4.0F, 1.0F, 5.0F), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(18, 11).addBox(-2.0F, -1.5F, -2.5F, 4.0F, 3.0F, 3.0F)
                .texOffs(32, 11).addBox(-1.0F, -0.5F, -4.5F, 2.0F, 2.0F, 2.0F)
                .texOffs(40, 11).addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, -3.5F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(52, 11).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 1.0F, 3.5F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(44, 11).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.5F, -1.5F, -0.5F));
            for (int k = 0; k < 2; k++) {
                body.addOrReplaceChild(name + (k == 0 ? "front_leg" : "hind_leg"), CubeListBuilder.create()
                        .mirror(s == 1).texOffs(48, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                        PartPose.offset(side * 1.5F, 1.0F, k == 0 ? -2.5F : 2.5F));
            }
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Hedgehog hedgehog, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - hedgehog.tickCount;
        float[] c = this.poses.of(hedgehog, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float curl = c[HedgehogPoses.CURL];
        // Rolled up the trunk rounds: shorter, taller, and down on the ground its feet no longer hold it off.
        this.body.xScale = 1.0F + 0.06F * curl;
        this.body.yScale = 1.0F + 0.25F * curl;
        this.body.zScale = 1.0F - 0.2F * curl;
        this.body.y = BODY_Y + c[HedgehogPoses.BOB] + 0.5F * curl;
        this.body.xRot = c[HedgehogPoses.PITCH];
        this.body.zRot = c[HedgehogPoses.ROLL];
        float bristle = 1.0F + 0.1F * curl + 0.02F * curl * Mth.sin(ageInTicks * 1.7F);
        this.spines.xScale = this.spines.yScale = this.spines.zScale = bristle;
        float look = c[HedgehogPoses.LOOK];
        float young = this.young ? YOUNG_HEAD : 1.0F;
        this.head.xRot = c[HedgehogPoses.HEAD] - c[HedgehogPoses.PITCH] + headPitch * Mth.DEG_TO_RAD * look
                + 1.35F * curl;
        this.head.yRot = c[HedgehogPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look * 0.7F;
        this.head.zRot = c[HedgehogPoses.HEAD_ROLL];
        this.head.y += 1.2F * curl;
        this.head.z += 2.0F * curl;
        this.head.xScale = this.head.yScale = this.head.zScale = young * (1.0F - 0.25F * curl);
        this.ears[0].zRot = -c[HedgehogPoses.EARS];
        this.ears[1].zRot = c[HedgehogPoses.EARS];
        this.tail.visible = curl < 0.5F;
        for (int k = 0; k < 4; k++) {
            int at = HedgehogPoses.LEGS + k * HedgehogPoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            this.legs[k].xRot = c[at] - c[HedgehogPoses.PITCH];
            this.legs[k].zRot = side * c[at + 1];
            this.legs[k].yScale = 1.0F - 0.85F * curl;
        }
    }
}
