package nl.tivek.multiversepowers.creature.animal.duck.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.duck.Duck;

// The duck's body in a duck's own build: a long low boat of a body carried level with a full breast, a short tail
// cocked up, an upright neck with a round head, eyes on its sides that blink and a flat bill whose lower half opens,
// short legs set far back with webbed feet, and wings in two pieces each: laid along its sides at rest (only the inner
// piece shows), out flat and beating in flight. Its texture is painted by scripts/models/animals. DuckPoses moves it.
public final class DuckModel extends HierarchicalModel<Duck> {
    static final float BODY_Y = 19.0F;
    private static final float NECK_REST = 0.15F;
    private static final float TAIL_REST = 0.5F;
    // How far back a folded wing lies along the body.
    private static final float FOLD = 1.5F;
    private static final float YOUNG_HEAD = 1.4F;
    // The bill is a pixel deep in all, the upper half thicker than the lower.
    private static final CubeDeformation UPPER_BILL = new CubeDeformation(0.0F, -0.2F, 0.0F);
    private static final CubeDeformation LOWER_BILL = new CubeDeformation(-0.05F, -0.3F, -0.05F);
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart bill;
    private final ModelPart[] eyes = new ModelPart[2];
    // Right, left: inner, outer.
    private final ModelPart[][] wings = new ModelPart[2][2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final DuckPoses poses = new DuckPoses();

    public DuckModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.bill = this.head.getChild("bill");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "right_" : "left_";
            this.eyes[s] = this.head.getChild(name + "eye");
            ModelPart wing = this.body.getChild(name + "wing");
            this.wings[s] = new ModelPart[] { wing, wing.getChild("outer") };
            this.legs[s] = this.body.getChild(name + "leg");
            this.feet[s] = this.legs[s].getChild("foot");
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2.0F, -3.5F, 5.0F, 4.0F, 7.0F)
                .texOffs(30, 12).addBox(-2.0F, -1.5F, -4.5F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, TAIL_REST, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(34, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -2.5F, NECK_REST, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(42, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(54, 0).addBox(-1.0F, -1.5F, -3.5F, 2.0F, 1.0F, 2.0F, UPPER_BILL),
                PartPose.offset(0.0F, -3.0F, 0.0F));
        // The lower half of the bill, hinged under the upper one so it opens as the duck quacks.
        head.addOrReplaceChild("bill", CubeListBuilder.create()
                .texOffs(54, 3).addBox(-1.0F, -0.3F, -2.0F, 2.0F, 1.0F, 2.0F, LOWER_BILL),
                PartPose.offset(0.0F, -0.7F, -1.5F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            boolean left = s == 1;
            String name = left ? "left_" : "right_";
            // An eye on the side of the head; it shuts by folding down to nothing, as a lid comes up.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(40, 12).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.1F, -1.0F, 0.0F));
            // A wing reaches outward from its shoulder: its span along x, its chord down, thin front to back.
            PartDefinition wing = body.addOrReplaceChild(name + "wing", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 12).addBox(left ? 0.0F : -4.0F, 0.0F, -0.5F, 4.0F, 3.0F, 1.0F),
                    PartPose.offset(side * 2.5F, -1.5F, -2.0F));
            wing.addOrReplaceChild("outer", CubeListBuilder.create().mirror(left)
                    .texOffs(10, 12).addBox(left ? 0.0F : -3.0F, 0.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                    PartPose.offset(side * 4.0F, 0.0F, 0.0F));
            PartDefinition leg = body.addOrReplaceChild(name + "leg", CubeListBuilder.create().mirror(left)
                    .texOffs(18, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.2F, 2.0F, 1.0F));
            leg.addOrReplaceChild("foot", CubeListBuilder.create().mirror(left)
                    .texOffs(22, 12).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 2.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Duck duck, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - duck.tickCount;
        float[] c = this.poses.of(duck, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float spread = c[DuckPoses.SPREAD];
        float pitch = c[DuckPoses.PITCH];
        this.body.y = BODY_Y + c[DuckPoses.BOB] + c[DuckPoses.SINK];
        this.body.xRot = pitch;
        this.body.zRot = c[DuckPoses.ROLL];
        this.tail.xRot = TAIL_REST + c[DuckPoses.TAIL];
        this.tail.yRot = c[DuckPoses.TAIL_WAG];
        float look = c[DuckPoses.LOOK];
        float yaw = c[DuckPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.neck.xRot = NECK_REST + c[DuckPoses.NECK];
        this.neck.yRot = yaw * 0.4F;
        // The head stays level whatever the neck and body do.
        this.head.xRot = -NECK_REST - c[DuckPoses.NECK] - pitch + c[DuckPoses.HEAD]
                + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = yaw * 0.6F;
        this.head.zRot = c[DuckPoses.HEAD_ROLL];
        this.bill.xRot = c[DuckPoses.BILL];
        float open = 1.0F - 0.9F * Mth.clamp(c[DuckPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1.0F : -1.0F;
            ModelPart wing = this.wings[s][0];
            // Folded back along the side on edge, or turned flat, out and beating.
            wing.xRot = spread * Mth.HALF_PI;
            wing.yRot = side * FOLD * (1.0F - spread);
            wing.zRot = side * (c[DuckPoses.BEAT] * spread + c[DuckPoses.WING_R + s]);
            this.wings[s][1].yRot = side * c[DuckPoses.BEAT_OUTER] * spread;
            this.wings[s][1].xScale = spread;
            this.wings[s][1].visible = spread > 0.02F;
            // A leg's swing is told against the ground, and its foot against the leg's.
            this.legs[s].xRot = c[DuckPoses.LEG_R + s] - pitch;
            this.feet[s].xRot = c[DuckPoses.FOOT_R + s] - c[DuckPoses.LEG_R + s];
        }
    }
}
