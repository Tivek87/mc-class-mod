package nl.tivek.multiversepowers.creature.animal.elephant.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.elephant.Elephant;

// The elephant's body in an elephant's own build: a long barrel of a trunk, high at the withers, its back dipping
// before the croup and its belly sagging, on
// four legs like pillars that bend at the knee, each on a round foot of its own that stays flat on the ground; a great
// head with a domed brow, small eyes that blink, a lower lip of its own under it, and at each side an ear as big as a
// door, hinged at its front edge so it can stand out or lie back along the shoulder, its back half hinged again so it
// trails and ripples as the ear moves;
// a trunk of five pieces that reaches the ground; tusks out of the face beside it, short on a cow, long and curving
// up on a bull, none on a calf; and a thin tail. Its texture is painted by scripts/models/animals. ElephantPoses
// moves it.
public final class ElephantModel extends HierarchicalModel<Elephant> {
    static final float BODY_Y = -12.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 6.0F;
    static final float HIP_Z = 12.0F;
    // How far an ear stands out from the head when it is held still.
    private static final float EAR_REST = 0.5F;
    private static final float YOUNG_HEAD = 1.15F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart lip;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] flaps = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] tusks = new ModelPart[2];
    private final ModelPart[] longTusks = new ModelPart[2];
    private final ModelPart[] trunk = new ModelPart[ElephantPoses.PIECES];
    // Right front, left front, right hind, left hind: upper, lower, foot.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final ElephantPoses poses = new ElephantPoses();

    public ElephantModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = this.body.getChild("head");
        this.lip = this.head.getChild("lip");
        ModelPart piece = this.head;
        for (int k = 0; k < this.trunk.length; k++) {
            piece = piece.getChild("trunk");
            this.trunk[k] = piece;
        }
        String[] sides = { "right_", "left_" };
        for (int s = 0; s < 2; s++) {
            this.ears[s] = this.head.getChild(sides[s] + "ear");
            this.flaps[s] = this.ears[s].getChild("flap");
            this.eyes[s] = this.head.getChild(sides[s] + "eye");
            this.tusks[s] = this.head.getChild(sides[s] + "tusk");
            this.longTusks[s] = this.tusks[s].getChild("long");
        }
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            ModelPart lower = upper.getChild("lower");
            this.legs[k] = new ModelPart[] { upper, lower, lower.getChild("foot") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A long barrel, deepest and highest at the withers, the back dipping before the croup, the belly sagging.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11.0F, -13.0F, -21.0F, 22.0F, 26.0F, 20.0F)
                .texOffs(84, 0).addBox(-10.5F, -12.0F, -2.0F, 21.0F, 24.0F, 22.0F)
                .texOffs(0, 50).addBox(-9.5F, 12.0F, -16.0F, 19.0F, 2.0F, 28.0F)
                .texOffs(94, 50).addBox(-7.0F, -15.0F, -19.0F, 14.0F, 2.0F, 14.0F)
                .texOffs(94, 66).addBox(-6.0F, -14.0F, 6.0F, 12.0F, 2.0F, 12.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(244, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                PartPose.offset(0.0F, -9.0F, 20.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(170, 0).addBox(-8.0F, -12.0F, -12.0F, 16.0F, 18.0F, 12.0F)
                .texOffs(170, 30).addBox(-6.0F, -14.0F, -11.0F, 12.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, -7.0F, -21.0F));
        head.addOrReplaceChild("lip", CubeListBuilder.create()
                .texOffs(170, 41).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 6.0F, -6.0F));
        float[][] pieces = { { 8.0F, 10.0F }, { 7.0F, 9.0F }, { 6.0F, 8.0F }, { 5.0F, 8.0F }, { 4.0F, 6.0F } };
        int[] u = { 150, 182, 210, 234, 150 };
        int[] v = { 52, 52, 52, 52, 70 };
        PartDefinition trunk = head;
        for (int k = 0; k < pieces.length; k++) {
            float wide = pieces[k][0];
            // Each piece starts half a pixel inside the one above it, so a bend opens no gap.
            trunk = trunk.addOrReplaceChild("trunk", CubeListBuilder.create()
                    .texOffs(u[k], v[k]).addBox(-wide / 2.0F, 0.0F, -wide / 2.0F, wide, pieces[k][1], wide),
                    k == 0 ? PartPose.offset(0.0F, 0.0F, -11.0F) : PartPose.offset(0.0F, pieces[k - 1][1] - 0.5F,
                            0.0F));
        }
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            boolean left = s == 1;
            String name = left ? "left_" : "right_";
            // An ear is hinged at its front edge and lies back from there; its back half hinged again.
            PartDefinition ear = head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(left)
                    .texOffs(226, 0).addBox(left ? 0.0F : -1.0F, -5.0F, 0.0F, 1.0F, 18.0F, 8.0F),
                    PartPose.offset(side * 8.0F, -9.0F, -8.0F));
            ear.addOrReplaceChild("flap", CubeListBuilder.create().mirror(left)
                    .texOffs(226, 26).addBox(left ? 0.0F : -1.0F, -5.0F, 0.0F, 1.0F, 18.0F, 7.0F),
                    PartPose.offset(0.0F, 0.0F, 8.0F));
            // An eye low on the side of the head; it shuts by folding down to nothing, as an upper lid comes down.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(252, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 7.6F, -1.0F, -9.5F));
            PartDefinition tusk = head.addOrReplaceChild(name + "tusk", CubeListBuilder.create().mirror(left)
                    .texOffs(166, 70).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(side * 5.0F, 4.0F, -11.0F, 0.75F, side * 0.1F, 0.0F));
            PartDefinition more = tusk.addOrReplaceChild("long", CubeListBuilder.create().mirror(left)
                    .texOffs(182, 70).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, -0.4F, 0.0F, 0.0F));
            more.addOrReplaceChild("point", CubeListBuilder.create().mirror(left)
                    .texOffs(194, 41).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, -0.4F, 0.0F, 0.0F));
            for (int end = 0; end < 2; end++) {
                boolean front = end == 0;
                PartDefinition upper = body.addOrReplaceChild(name + (front ? "front_leg" : "hind_leg"),
                        CubeListBuilder.create().mirror(left)
                                .texOffs(0, 80).addBox(-4.5F, -3.0F, -4.5F, 9.0F, 17.0F, 9.0F),
                        PartPose.offset(side * 6.5F, HIP_Y, front ? -13.0F : HIP_Z));
                PartDefinition lower = upper.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                        .texOffs(36, 80).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F),
                        PartPose.offset(0.0F, 14.0F, 0.0F));
                lower.addOrReplaceChild("foot", CubeListBuilder.create().mirror(left)
                        .texOffs(68, 80).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 2.0F, 9.0F),
                        PartPose.offset(0.0F, 14.0F, 0.0F));
            }
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Elephant elephant, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - elephant.tickCount;
        float[] c = this.poses.of(elephant, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[ElephantPoses.PITCH];
        // The body turns about its middle, then comes back so its hips stay where its hind legs stand.
        float wy = HIP_Y * Mth.cos(pitch) - HIP_Z * Mth.sin(pitch);
        float wz = HIP_Y * Mth.sin(pitch) + HIP_Z * Mth.cos(pitch);
        this.body.y = BODY_Y + c[ElephantPoses.BOB] + c[ElephantPoses.SINK] + (HIP_Y - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = c[ElephantPoses.ROLL];
        this.tail.xRot = 0.1F + c[ElephantPoses.TAIL];
        this.tail.zRot = c[ElephantPoses.TAIL_SIDE];
        float look = c[ElephantPoses.LOOK];
        float nod = c[ElephantPoses.HEAD] + headPitch * Mth.DEG_TO_RAD * look * 0.35F;
        this.head.xRot = nod - pitch;
        this.head.yRot = c[ElephantPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look * 0.6F;
        this.head.zRot = c[ElephantPoses.HEAD_ROLL];
        this.lip.xRot = c[ElephantPoses.JAW];
        for (int k = 0; k < this.trunk.length; k++) {
            // The trunk hangs by its own weight, whatever way the head is tipped.
            this.trunk[k].xRot = c[ElephantPoses.TRUNK + k] - (k == 0 ? nod * 0.8F : 0.0F);
            this.trunk[k].zRot = c[ElephantPoses.TRUNK_SIDE] * (0.4F + 0.3F * k);
        }
        boolean grown = !this.young;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            this.ears[s].yRot = side * (EAR_REST + c[ElephantPoses.EAR_R + s]);
            // The back half trails behind how far the ear has moved.
            this.flaps[s].yRot = side * (0.1F + 0.9F * (c[ElephantPoses.FLAP_R + s] - c[ElephantPoses.EAR_R + s]));
            this.eyes[s].yScale = 1.0F - 0.9F * Mth.clamp(c[ElephantPoses.BLINK], 0.0F, 1.0F);
            this.tusks[s].visible = grown;
            this.longTusks[s].visible = elephant.isBull();
        }
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = ElephantPoses.LEGS + k * ElephantPoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            leg[0].xRot = c[at] - pitch;
            leg[0].zRot = side * c[at + 1] - c[ElephantPoses.ROLL] * 0.7F;
            leg[1].xRot = c[at + 2];
            leg[2].xRot = c[at + 3];
        }
    }
}
