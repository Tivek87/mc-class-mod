package nl.tivek.multiversepowers.creature.animal.crocodile.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;

// The crocodile's body in a crocodile's own build: a broad flat trunk with two ridges of scutes down its back, a
// wide skull with raised eyes running into a long narrow snout with the nostrils on a bump at its tip, a lower jaw
// that drops open, a tail of four pieces tapering to a thin blade with a crest along it, and four short legs that
// reach out sideways from the body before they go down, as a reptile's do. Its texture is painted by
// scripts/models/animals. CrocodilePoses moves it.
public final class CrocodileModel extends HierarchicalModel<Crocodile> {
    static final float BODY_Y = 18.5F;
    // How each tail piece slopes down, from the body out, for the tail to lie on the ground behind it.
    private static final float[] DROOP = { -0.25F, -0.15F, 0.2F, 0.2F };
    private static final float YOUNG_HEAD = 1.15F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] tail = new ModelPart[CrocodilePoses.TAIL_PIECES];
    // Right front, left front, right hind, left hind: upper, lower.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final CrocodilePoses poses = new CrocodilePoses();

    public CrocodileModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart piece = this.body;
        for (int k = 0; k < this.tail.length; k++) {
            piece = piece.getChild("tail");
            this.tail[k] = piece;
        }
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            this.legs[k] = new ModelPart[] { upper, upper.getChild("lower") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -2.5F, -8.0F, 10.0F, 5.0F, 16.0F)
                .texOffs(52, 0).addBox(-3.0F, -3.5F, -7.0F, 1.0F, 1.0F, 14.0F)
                .texOffs(52, 0).addBox(2.0F, -3.5F, -7.0F, 1.0F, 1.0F, 14.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 21).addBox(-4.0F, -2.5F, -5.0F, 8.0F, 4.0F, 5.0F)
                .texOffs(26, 21).addBox(-2.5F, -0.5F, -13.0F, 5.0F, 2.0F, 8.0F)
                .texOffs(52, 21).addBox(-3.5F, -3.5F, -3.5F, 2.0F, 1.0F, 2.0F)
                .texOffs(52, 21).mirror().addBox(1.5F, -3.5F, -3.5F, 2.0F, 1.0F, 2.0F).mirror(false)
                .texOffs(60, 21).addBox(-1.5F, -1.5F, -13.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 31).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 1.0F, 4.0F)
                .texOffs(22, 31).addBox(-2.0F, 0.0F, -12.0F, 4.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 1.5F, -1.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(82, 0).addBox(-3.5F, -2.0F, 0.0F, 7.0F, 4.0F, 6.0F)
                .texOffs(108, 0).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, -0.5F, 8.0F));
        tail = tail.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(70, 21).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 6.0F)
                .texOffs(108, 0).addBox(-0.5F, -2.5F, 0.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 6.0F));
        tail = tail.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(92, 21).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 6.0F)
                .texOffs(108, 0).addBox(-0.5F, -2.0F, 0.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 6.0F));
        tail.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(110, 21).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 6.0F));
        for (int k = 0; k < 4; k++) {
            boolean left = k % 2 == 1;
            float side = left ? 1.0F : -1.0F;
            String name = (left ? "left_" : "right_") + (k < 2 ? "front_leg" : "hind_leg");
            PartDefinition upper = body.addOrReplaceChild(name, CubeListBuilder.create().mirror(left)
                    .texOffs(0, 42).addBox(left ? 0.0F : -3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                    PartPose.offset(side * 5.0F, 1.0F, k < 2 ? -5.5F : 5.5F));
            upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                    .texOffs(10, 42).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F)
                    .texOffs(18, 42).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 3.0F),
                    PartPose.offset(side * 3.0F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Crocodile crocodile, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - crocodile.tickCount;
        float[] c = this.poses.of(crocodile, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[CrocodilePoses.PITCH];
        this.body.y = BODY_Y + c[CrocodilePoses.BOB] + c[CrocodilePoses.SINK];
        this.body.z = -c[CrocodilePoses.LUNGE];
        this.body.xRot = pitch;
        this.body.yRot = c[CrocodilePoses.SWAY];
        this.body.zRot = c[CrocodilePoses.ROLL];
        float look = c[CrocodilePoses.LOOK];
        this.head.xRot = c[CrocodilePoses.HEAD] - pitch * 0.6F + headPitch * Mth.DEG_TO_RAD * look * 0.5F;
        this.head.yRot = c[CrocodilePoses.HEAD_YAW] - c[CrocodilePoses.SWAY]
                + Mth.clamp(netHeadYaw, -40.0F, 40.0F) * Mth.DEG_TO_RAD * look;
        this.jaw.xRot = c[CrocodilePoses.JAW];
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        float droop = c[CrocodilePoses.DROOP];
        for (int k = 0; k < this.tail.length; k++) {
            this.tail[k].xRot = DROOP[k] * droop - (k == 0 ? pitch : 0.0F);
            this.tail[k].yRot = c[CrocodilePoses.TAIL + k];
        }
        for (int k = 0; k < 4; k++) {
            int at = CrocodilePoses.LEGS + k * CrocodilePoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            ModelPart[] leg = this.legs[k];
            // A leg swings about the upright, its knee lifting as it comes forward; the shin stays upright.
            leg[0].yRot = side * c[at];
            leg[0].zRot = side * c[at + 1];
            leg[1].zRot = -side * c[at + 1];
            leg[1].xRot = -pitch;
        }
    }
}
