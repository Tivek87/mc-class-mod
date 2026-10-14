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

// The crocodile's body in a crocodile's own build: a broad flat trunk in two halves that bend at the spine, with two
// ridges of scutes down its back, a wide skull with raised eyes that blink running into a long narrow snout with the
// nostrils on a bump at its tip, jagged teeth along both jaws, a lower jaw that drops open, a tail of four pieces
// tapering to a thin blade with a crest along it, and four short legs that reach out sideways from the body before
// they go down, as a reptile's do, each on a broad foot of its own. Its texture is painted by scripts/models/animals.
// CrocodilePoses moves it.
public final class CrocodileModel extends HierarchicalModel<Crocodile> {
    static final float BODY_Y = 18.5F;
    // How each tail piece slopes down, from the body out, for the tail to lie on the ground behind it.
    private static final float[] DROOP = { -0.25F, -0.15F, 0.2F, 0.2F };
    private static final float YOUNG_HEAD = 1.15F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart hips;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[CrocodilePoses.TAIL_PIECES];
    // Right front, left front, right hind, left hind: upper, lower, foot.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final CrocodilePoses poses = new CrocodilePoses();

    public CrocodileModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.hips = this.body.getChild("hips");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart piece = this.hips;
        for (int k = 0; k < this.tail.length; k++) {
            piece = piece.getChild("tail");
            this.tail[k] = piece;
        }
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = (k < 2 ? this.body : this.hips).getChild(names[k]);
            ModelPart lower = upper.getChild("lower");
            this.legs[k] = new ModelPart[] { upper, lower, lower.getChild("foot") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The trunk in two halves that bend at the spine between them, each with its two rows of scutes.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -2.5F, -8.0F, 10.0F, 5.0F, 8.0F)
                .texOffs(108, 8).addBox(-3.0F, -3.5F, -7.0F, 1.0F, 1.0F, 7.0F)
                .texOffs(108, 8).addBox(2.0F, -3.5F, -7.0F, 1.0F, 1.0F, 7.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition hips = body.addOrReplaceChild("hips", CubeListBuilder.create()
                .texOffs(36, 0).addBox(-4.5F, -2.5F, -0.5F, 9.0F, 5.0F, 8.0F)
                .texOffs(108, 8).addBox(-3.0F, -3.5F, 0.0F, 1.0F, 1.0F, 7.0F)
                .texOffs(108, 8).addBox(2.0F, -3.5F, 0.0F, 1.0F, 1.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 0.5F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 21).addBox(-4.0F, -2.5F, -5.0F, 8.0F, 4.0F, 5.0F)
                .texOffs(26, 21).addBox(-2.5F, -0.5F, -13.0F, 5.0F, 2.0F, 8.0F)
                .texOffs(52, 21).addBox(-3.5F, -3.5F, -3.5F, 2.0F, 1.0F, 2.0F)
                .texOffs(52, 21).mirror().addBox(1.5F, -3.5F, -3.5F, 2.0F, 1.0F, 2.0F).mirror(false)
                .texOffs(60, 21).addBox(-1.5F, -1.5F, -13.0F, 3.0F, 1.0F, 2.0F)
                // The upper teeth along each edge of the snout, jagged where the picture leaves gaps.
                .texOffs(46, 31).addBox(-2.6F, 1.5F, -12.5F, 1.0F, 1.0F, 7.0F)
                .texOffs(46, 31).mirror().addBox(1.6F, 1.5F, -12.5F, 1.0F, 1.0F, 7.0F).mirror(false),
                PartPose.offset(0.0F, 0.0F, -8.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 31).addBox(-3.5F, 0.0F, -4.0F, 7.0F, 1.0F, 4.0F)
                .texOffs(22, 31).addBox(-2.0F, 0.0F, -12.0F, 4.0F, 1.0F, 8.0F)
                .texOffs(46, 31).addBox(-2.0F, -1.0F, -11.5F, 1.0F, 1.0F, 7.0F)
                .texOffs(46, 31).mirror().addBox(1.0F, -1.0F, -11.5F, 1.0F, 1.0F, 7.0F).mirror(false),
                PartPose.offset(0.0F, 1.5F, -1.0F));
        for (int s = 0; s < 2; s++) {
            // An eye on the outer face of its bump; it blinks by sinking into the bump.
            head.addOrReplaceChild((s == 0 ? "right_" : "left_") + "eye", CubeListBuilder.create()
                    .texOffs(52, 24).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(s == 0 ? -3.1F : 3.1F, -2.6F, -2.5F));
        }
        PartDefinition tail = hips.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(82, 0).addBox(-3.5F, -2.0F, 0.0F, 7.0F, 4.0F, 6.0F)
                .texOffs(108, 0).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, -0.5F, 7.5F));
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
            boolean front = k < 2;
            PartDefinition upper = (front ? body : hips).addOrReplaceChild(name, CubeListBuilder.create()
                    .mirror(left).texOffs(0, 42).addBox(left ? 0.0F : -3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                    PartPose.offset(side * (front ? 5.0F : 4.5F), 1.0F, front ? -5.5F : 5.0F));
            PartDefinition lower = upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                    .texOffs(10, 42).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offset(side * 3.0F, 0.0F, 0.0F));
            // A broad foot with its toes spread forward, on an ankle of its own.
            lower.addOrReplaceChild("foot", CubeListBuilder.create().mirror(left)
                    .texOffs(18, 42).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 1.0F, 3.0F),
                    PartPose.offset(0.0F, 3.5F, 0.0F));
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
        this.hips.yRot = c[CrocodilePoses.HIPS];
        float open = 1.0F - 0.9F * Mth.clamp(c[CrocodilePoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
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
            // The foot keeps pointing ahead while the leg swings round above it, and lies flat on the ground.
            leg[2].yRot = -side * 0.8F * c[at] - (k < 2 ? 0.0F : c[CrocodilePoses.HIPS]);
            leg[2].xRot = c[at + 2];
        }
    }
}
