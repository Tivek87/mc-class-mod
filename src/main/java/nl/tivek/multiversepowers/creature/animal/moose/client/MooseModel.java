package nl.tivek.multiversepowers.creature.animal.moose.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.moose.Moose;

// The moose's body in a moose's own build: a deep chest under a high shoulder hump, a lower rump, and very long
// legs, the forelegs bending at the knee and the hind legs at the stifle and the hock, dewclaws behind each fetlock
// over broad cloven hooves; a short thick neck carried forward with a dark mane along its top, a long head with a
// drooping muzzle and a broad overhanging nose, small eyes on its sides that blink, a jaw of its own, big ears, a bell
// of skin under the throat, and on a bull broad antlers: a beam out to each side widening into a flat palm cupped upward,
// with tines along its front and outer edges and a brow tine forward. Its texture is painted by
// scripts/models/animals. MoosePoses moves it.
public final class MooseModel extends HierarchicalModel<Moose> {
    static final float BODY_Y = 2.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 1.3F;
    static final float HIP_Z = 6.5F;
    static final float NECK_REST = 1.15F;
    static final float HEAD_REST = 0.3F;
    static final float THIGH_REST = -0.2F;
    static final float GASKIN_REST = 0.7F;
    static final float CANNON_REST = -0.5F;
    private static final float YOUNG_HEAD = 1.2F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart bell;
    private final ModelPart antlers;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    // Right front, left front: upper, cannon, hoof. Right hind, left hind: thigh, gaskin, cannon, hoof.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final MoosePoses poses = new MoosePoses();

    public MooseModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.bell = this.head.getChild("bell");
        this.antlers = this.head.getChild("antlers");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        String[] names = { "right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg" };
        for (int k = 0; k < 4; k++) {
            ModelPart upper = this.body.getChild(names[k]);
            ModelPart middle = upper.getChild("middle");
            if (k < 2) {
                this.legs[k] = new ModelPart[] { upper, middle, middle.getChild("hoof") };
            } else {
                ModelPart cannon = middle.getChild("cannon");
                this.legs[k] = new ModelPart[] { upper, middle, cannon, cannon.getChild("hoof") };
            }
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -6.5F, -11.0F, 10.0F, 13.0F, 11.0F)
                .texOffs(42, 0).addBox(-4.5F, -5.5F, 0.0F, 9.0F, 11.0F, 10.0F)
                .texOffs(80, 0).addBox(-4.0F, -8.5F, -10.5F, 8.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(88, 40).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, -4.5F, 10.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.0F, -9.0F, -3.5F, 6.0F, 9.0F, 7.0F)
                .texOffs(114, 4).addBox(-1.0F, -9.0F, 3.5F, 2.0F, 8.0F, 1.0F),
                PartPose.offset(0.0F, -3.5F, -10.5F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(26, 24).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 7.0F)
                .texOffs(52, 24).addBox(-2.5F, -1.5F, -13.0F, 5.0F, 5.0F, 7.0F)
                .texOffs(76, 24).addBox(-3.0F, -0.5F, -15.0F, 6.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, -8.5F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(94, 24).addBox(-2.0F, 0.0F, -6.0F, 4.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 3.0F, -6.0F));
        head.addOrReplaceChild("bell", CubeListBuilder.create()
                .texOffs(120, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 2.5F, -3.0F));
        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(),
                PartPose.offset(0.0F, -3.0F, -1.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            boolean left = s == 1;
            String name = left ? "left_" : "right_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(left)
                    .texOffs(114, 24).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.0F),
                    PartPose.offset(side * 3.0F, -2.5F, 0.0F));
            // An eye on the side of the head; it shuts by folding down to nothing, as an upper lid comes down.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(114, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 2.6F, -0.5F, -4.0F));
            // Every antler box reaches outward from its own joint: to -x on the right, +x on the left.
            PartDefinition beam = antlers.addOrReplaceChild(name + "beam", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 56).addBox(left ? 0.0F : -4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(side * 2.5F, 0.0F, 0.0F, 0.0F, 0.0F, -side * 0.35F));
            beam.addOrReplaceChild("brow", CubeListBuilder.create().mirror(left)
                    .texOffs(54, 56).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
                    PartPose.offsetAndRotation(side * 2.0F, 0.0F, 0.0F, -0.5F, -side * 0.35F, 0.0F));
            PartDefinition palm = beam.addOrReplaceChild("palm", CubeListBuilder.create().mirror(left)
                    .texOffs(10, 56).addBox(left ? 0.0F : -7.0F, -0.5F, -3.5F, 7.0F, 1.0F, 7.0F),
                    PartPose.offsetAndRotation(side * 4.0F, 0.0F, 0.0F, 0.0F, -side * 0.25F, -side * 0.4F));
            for (int k = 0; k < 3; k++) {
                palm.addOrReplaceChild("front_tine" + k, CubeListBuilder.create().mirror(left)
                        .texOffs(38, 56).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                        PartPose.offsetAndRotation(side * (1.5F + 2.0F * k), 0.0F, -3.5F, -0.45F - 0.1F * k,
                                -side * (0.1F + 0.15F * k), 0.0F));
                palm.addOrReplaceChild("outer_tine" + k, CubeListBuilder.create().mirror(left)
                        .texOffs(46, 56).addBox(left ? 0.0F : -3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(side * 7.0F, 0.0F, -2.0F + 2.5F * k, 0.0F, -side * 0.3F * (k - 1),
                                -side * 0.5F));
            }
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 40).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 9.0F, 4.0F),
                    PartPose.offset(side * 3.0F, 4.5F, -7.5F));
            PartDefinition cannon = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(16, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F)
                    .texOffs(120, 0).addBox(-0.5F, 6.5F, 1.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 7.5F, 0.0F));
            cannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(left)
                    .texOffs(28, 40).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
            PartDefinition thigh = body.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(42, 40).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 9.0F, 6.0F),
                    PartPose.offsetAndRotation(side * 3.0F, HIP_Y, HIP_Z, THIGH_REST, 0.0F, 0.0F));
            PartDefinition gaskin = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(64, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, GASKIN_REST, 0.0F, 0.0F));
            PartDefinition hindCannon = gaskin.addOrReplaceChild("cannon", CubeListBuilder.create().mirror(left)
                    .texOffs(76, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F)
                    .texOffs(120, 0).addBox(-0.5F, 6.5F, 1.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, CANNON_REST, 0.0F, 0.0F));
            hindCannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(left)
                    .texOffs(28, 40).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Moose moose, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - moose.tickCount;
        float[] c = this.poses.of(moose, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[MoosePoses.PITCH];
        // The body turns about its middle, then comes back so its hips stay where its hind legs stand.
        float wy = HIP_Y * Mth.cos(pitch) - HIP_Z * Mth.sin(pitch);
        float wz = HIP_Y * Mth.sin(pitch) + HIP_Z * Mth.cos(pitch);
        this.body.y = BODY_Y + c[MoosePoses.BOB] + c[MoosePoses.SINK] + (HIP_Y - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = c[MoosePoses.ROLL];
        this.tail.xRot = 0.25F + c[MoosePoses.TAIL];
        float look = c[MoosePoses.LOOK];
        float yaw = netHeadYaw * Mth.DEG_TO_RAD * look;
        this.neck.xRot = NECK_REST + c[MoosePoses.NECK];
        this.neck.yRot = c[MoosePoses.NECK_YAW] + yaw * 0.5F;
        // The head keeps its own slope whatever the neck does, then is turned.
        this.head.xRot = HEAD_REST - NECK_REST - c[MoosePoses.NECK] - pitch + c[MoosePoses.HEAD]
                + headPitch * Mth.DEG_TO_RAD * look * 0.6F;
        this.head.yRot = c[MoosePoses.HEAD_YAW] + yaw * 0.5F;
        this.head.zRot = c[MoosePoses.HEAD_ROLL];
        this.jaw.xRot = c[MoosePoses.JAW];
        // The bell hangs straight down from the throat and swings.
        this.bell.xRot = -HEAD_REST - c[MoosePoses.HEAD] + c[MoosePoses.BELL];
        this.bell.zRot = -c[MoosePoses.HEAD_ROLL] + c[MoosePoses.BELL_SIDE];
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            this.ears[s].zRot = side * (1.0F + c[MoosePoses.EAR_R + s]);
            this.ears[s].xRot = 0.15F + c[MoosePoses.EARS_BACK];
        }
        float open = 1.0F - 0.9F * Mth.clamp(c[MoosePoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        this.antlers.visible = moose.isBull() && !this.young;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = MoosePoses.LEGS + k * MoosePoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            if (k < 2) {
                leg[0].xRot = c[at] - pitch;
                leg[1].xRot = c[at + 2];
                leg[2].xRot = c[at + 4];
            } else {
                leg[0].xRot = THIGH_REST + c[at] - pitch;
                leg[1].xRot = GASKIN_REST + c[at + 2];
                leg[2].xRot = CANNON_REST + c[at + 3];
                leg[3].xRot = c[at + 4];
            }
            leg[0].zRot = side * c[at + 1] - c[MoosePoses.ROLL] * 0.7F;
        }
    }
}
