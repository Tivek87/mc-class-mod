package nl.tivek.multiversepowers.creature.animal.deer.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;

// The deer's body in a deer's own build: a slender barrel with a deep chest, the withers standing over the shoulders
// and the croup over the hips, on long thin legs, the forelegs bending at the knee (the wrist) and the hind legs at the
// stifle and the hock that points back, dewclaws behind each fetlock, small cloven hooves; a long neck thick at its
// base, held up, a narrow head with a black nose, eyes on its sides that blink, big ears, a jaw of its own, and on a
// stag branching antlers, a beam curving forward on each side with tines standing up from it. A short tail that lifts
// to show white. Its texture is painted by scripts/models/animals. DeerPoses moves it.
public final class DeerModel extends HierarchicalModel<Deer> {
    static final float BODY_Y = 11.5F;
    static final float NECK_REST = 0.7F;
    static final float THIGH_REST = -0.2F;
    static final float GASKIN_REST = 0.75F;
    static final float CANNON_REST = -0.55F;
    private static final float YOUNG_HEAD = 1.35F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart antlers;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    // Right front, left front: upper, cannon, hoof. Right hind, left hind: thigh, gaskin, cannon, hoof.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final DeerPoses poses = new DeerPoses();

    public DeerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
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
                .texOffs(0, 0).addBox(-3.0F, -3.5F, -8.0F, 6.0F, 7.0F, 15.0F)
                .texOffs(42, 0).addBox(-3.0F, -3.0F, -11.0F, 6.0F, 8.0F, 3.0F)
                .texOffs(60, 0).addBox(-2.0F, -4.5F, -9.0F, 4.0F, 1.0F, 5.0F)
                .texOffs(78, 0).addBox(-2.5F, -4.0F, 6.0F, 5.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(42, 11).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, -3.5F, 9.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-1.5F, -9.0F, -2.0F, 3.0F, 9.0F, 4.0F)
                .texOffs(14, 22).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, -2.5F, -9.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(94, 0).addBox(-2.0F, -3.0F, -3.5F, 4.0F, 4.0F, 5.0F)
                .texOffs(112, 0).addBox(-1.5F, -1.5F, -7.5F, 3.0F, 2.0F, 4.0F)
                .texOffs(70, 11).addBox(-1.0F, -1.5F, -8.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -8.5F, -0.5F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(58, 11).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 0.5F, -3.5F));
        PartDefinition antlers = head.addOrReplaceChild("antlers", CubeListBuilder.create(),
                PartPose.offset(0.0F, -3.0F, -0.5F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(50, 11).addBox(-1.5F, -4.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                    PartPose.offset(side * 1.8F, -2.5F, 0.0F));
            // An eye on the side of the head; it shuts by folding down to nothing, as an upper lid comes down.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(76, 11).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.6F, -1.0F, -2.0F));
            // A beam rising out and back from the skull with a brow tine forward, curving forward and in, two tines
            // standing up from it and its tip turned further in.
            PartDefinition beam = antlers.addOrReplaceChild(name + "beam", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(102, 11).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                    .texOffs(110, 11).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 2.0F),
                    PartPose.offsetAndRotation(side * 1.5F, 0.0F, 0.0F, -0.35F, 0.0F, side * 0.8F));
            PartDefinition main = beam.addOrReplaceChild("main", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(42, 17).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F)
                    .texOffs(106, 11).addBox(-0.5F, -3.5F, -1.0F, 1.0F, 3.0F, 1.0F)
                    .texOffs(106, 11).addBox(-0.5F, -3.5F, -2.5F, 1.0F, 3.0F, 1.0F)
                    .texOffs(106, 11).addBox(-0.5F, -3.0F, -3.9F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.15F, side * 0.4F, -side * 0.8F));
            main.addOrReplaceChild("tip", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(116, 11).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.35F, side * 0.55F, 0.0F));
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(32, 22).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                    PartPose.offset(side * 1.8F, 1.5F, -7.0F));
            PartDefinition cannon = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(62, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                    .texOffs(80, 11).addBox(-0.5F, 3.5F, 1.0F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 5.0F, 0.0F));
            cannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(84, 11).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 5.0F, 0.0F));
            PartDefinition thigh = body.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(44, 22).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 7.0F, 5.0F),
                    PartPose.offsetAndRotation(side * 2.0F, -0.9F, 6.0F, THIGH_REST, 0.0F, 0.0F));
            PartDefinition gaskin = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(92, 11).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, GASKIN_REST, 0.0F, 0.0F));
            PartDefinition hindCannon = gaskin.addOrReplaceChild("cannon", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(70, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F)
                    .texOffs(80, 11).addBox(-0.5F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, CANNON_REST, 0.0F, 0.0F));
            hindCannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(84, 11).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 4.5F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Deer deer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - deer.tickCount;
        float[] c = this.poses.of(deer, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[DeerPoses.PITCH];
        this.body.y = BODY_Y + c[DeerPoses.BOB] + c[DeerPoses.SINK];
        this.body.xRot = pitch;
        this.body.zRot = c[DeerPoses.ROLL];
        this.tail.xRot = 0.3F + c[DeerPoses.TAIL];
        this.tail.zRot = c[DeerPoses.TAIL_SIDE];
        float look = c[DeerPoses.LOOK];
        float yaw = netHeadYaw * Mth.DEG_TO_RAD * look;
        this.neck.xRot = NECK_REST + c[DeerPoses.NECK];
        this.neck.yRot = c[DeerPoses.NECK_YAW] + yaw * 0.4F;
        // The head is held level whatever the neck does, then turned.
        this.head.xRot = -NECK_REST - c[DeerPoses.NECK] - pitch + c[DeerPoses.HEAD]
                + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[DeerPoses.HEAD_YAW] + yaw * 0.6F;
        this.head.zRot = c[DeerPoses.HEAD_ROLL];
        this.jaw.xRot = c[DeerPoses.JAW];
        // Ears out to the sides, turned forward or laid back.
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            this.ears[s].zRot = side * (0.95F + c[DeerPoses.EAR_R + s]);
            this.ears[s].xRot = 0.2F + c[DeerPoses.EARS_TIP];
        }
        float open = 1.0F - 0.9F * Mth.clamp(c[DeerPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        this.antlers.visible = deer.isStag() && !this.young;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = DeerPoses.LEGS + k * DeerPoses.PER_LEG;
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
            leg[0].zRot = side * c[at + 1] - c[DeerPoses.ROLL] * 0.7F;
        }
    }
}
