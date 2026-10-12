package nl.tivek.multiversepowers.creature.animal.giraffe.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.giraffe.Giraffe;

// The giraffe's body in a giraffe's own build: a short deep trunk, highest at the withers and sloping down to the
// rump, on very long legs, the forelegs bending at the knee and the hind legs at the stifle and the hock; a neck of
// three pieces as long as its legs, leaning forward, with a short mane down the back of it; a long narrow head with
// a jaw of its own, a dark tongue it can put out, two blunt horns and ears held out to the sides; and a thin tail
// ending in a dark tuft. Its texture is painted by scripts/models/animals. GiraffePoses moves it.
public final class GiraffeModel extends HierarchicalModel<Giraffe> {
    static final float BODY_Y = -9.0F;
    // The hips in the body's own frame.
    static final float HIP_Y = 1.8F;
    static final float HIP_Z = 6.5F;
    static final float NECK_REST = 0.45F;
    static final float HEAD_REST = 0.25F;
    static final float THIGH_REST = -0.2F;
    static final float GASKIN_REST = 0.6F;
    static final float CANNON_REST = -0.4F;
    // How much of a turn of the head each neck piece takes.
    private static final float[] TURN = { 0.15F, 0.2F, 0.25F };
    private static final float YOUNG_HEAD = 1.25F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart[] neck = new ModelPart[3];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tongue;
    private final ModelPart[] ears = new ModelPart[2];
    // Right front, left front: upper, cannon, hoof. Right hind, left hind: thigh, gaskin, cannon, hoof.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final GiraffePoses poses = new GiraffePoses();

    public GiraffeModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        ModelPart piece = this.body;
        for (int k = 0; k < this.neck.length; k++) {
            piece = piece.getChild("neck");
            this.neck[k] = piece;
        }
        this.head = piece.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tongue = this.head.getChild("tongue");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
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
                .texOffs(0, 0).addBox(-5.0F, -7.0F, -9.0F, 10.0F, 13.0F, 9.0F)
                .texOffs(38, 0).addBox(-4.5F, -4.0F, 0.0F, 9.0F, 10.0F, 9.0F)
                .texOffs(74, 0).addBox(-3.5F, -9.0F, -8.5F, 7.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(110, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F)
                .texOffs(114, 0).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, -3.0F, 9.0F));
        PartDefinition neck1 = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.5F, -11.0F, -3.0F, 5.0F, 11.0F, 6.0F)
                .texOffs(56, 24).addBox(-0.5F, -11.0F, 3.0F, 1.0F, 11.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, NECK_REST, 0.0F, 0.0F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(22, 24).addBox(-2.0F, -9.0F, -2.5F, 4.0F, 9.0F, 5.0F)
                .texOffs(60, 24).addBox(-0.5F, -9.0F, 2.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offset(0.0F, -10.0F, 0.0F));
        PartDefinition neck3 = neck2.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(40, 24).addBox(-2.0F, -8.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(64, 24).addBox(-0.5F, -8.0F, 2.0F, 1.0F, 8.0F, 1.0F),
                PartPose.offset(0.0F, -8.5F, 0.0F));
        PartDefinition head = neck3.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(68, 24).addBox(-2.0F, -2.5F, -4.0F, 4.0F, 5.0F, 5.0F)
                .texOffs(86, 24).addBox(-1.5F, -0.5F, -9.0F, 3.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -7.5F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(102, 24).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 1.5F, -4.0F));
        head.addOrReplaceChild("tongue", CubeListBuilder.create()
                .texOffs(100, 0).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 1.5F, -5.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            boolean left = s == 1;
            String name = left ? "left_" : "right_";
            head.addOrReplaceChild(name + "horn", CubeListBuilder.create().mirror(left)
                    .texOffs(118, 24).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(side * 1.2F, -2.5F, -0.5F, -0.3F, 0.0F, side * 0.12F));
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(left)
                    .texOffs(122, 24).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                    PartPose.offset(side * 2.0F, -1.5F, 0.5F));
            PartDefinition front = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(0, 44).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                    PartPose.offset(side * 3.0F, 4.0F, -6.0F));
            PartDefinition cannon = front.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(16, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F),
                    PartPose.offset(0.0F, 13.0F, 0.0F));
            cannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(left)
                    .texOffs(28, 44).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(0.0F, 14.0F, 0.0F));
            PartDefinition thigh = body.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(left)
                    .texOffs(42, 44).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 12.0F, 6.0F),
                    PartPose.offsetAndRotation(side * 2.5F, HIP_Y, HIP_Z, THIGH_REST, 0.0F, 0.0F));
            PartDefinition gaskin = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(left)
                    .texOffs(64, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, GASKIN_REST, 0.0F, 0.0F));
            PartDefinition hindCannon = gaskin.addOrReplaceChild("cannon", CubeListBuilder.create().mirror(left)
                    .texOffs(76, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, CANNON_REST, 0.0F, 0.0F));
            hindCannon.addOrReplaceChild("hoof", CubeListBuilder.create().mirror(left)
                    .texOffs(28, 44).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 4.0F),
                    PartPose.offset(0.0F, 12.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Giraffe giraffe, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - giraffe.tickCount;
        float[] c = this.poses.of(giraffe, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[GiraffePoses.PITCH];
        // The body turns about its middle, then comes back so its hips stay where its hind legs stand.
        float wy = HIP_Y * Mth.cos(pitch) - HIP_Z * Mth.sin(pitch);
        float wz = HIP_Y * Mth.sin(pitch) + HIP_Z * Mth.cos(pitch);
        this.body.y = BODY_Y + c[GiraffePoses.BOB] + c[GiraffePoses.SINK] + (HIP_Y - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = c[GiraffePoses.ROLL];
        this.tail.xRot = 0.12F + c[GiraffePoses.TAIL];
        this.tail.zRot = c[GiraffePoses.TAIL_SIDE];
        float look = c[GiraffePoses.LOOK];
        float yaw = netHeadYaw * Mth.DEG_TO_RAD * look;
        float lean = 0.0F;
        for (int k = 0; k < this.neck.length; k++) {
            float bend = (k == 0 ? NECK_REST : 0.0F) + c[GiraffePoses.NECK + k];
            this.neck[k].xRot = bend;
            this.neck[k].yRot = (c[GiraffePoses.NECK_YAW] + yaw) * TURN[k];
            lean += bend;
        }
        // The head keeps its own slope whatever the neck does, then is turned.
        this.head.xRot = HEAD_REST - lean - pitch + c[GiraffePoses.HEAD] + headPitch * Mth.DEG_TO_RAD * look * 0.6F;
        this.head.yRot = c[GiraffePoses.HEAD_YAW] + yaw * 0.4F;
        this.head.zRot = c[GiraffePoses.HEAD_ROLL];
        this.jaw.xRot = c[GiraffePoses.JAW];
        this.jaw.yRot = c[GiraffePoses.JAW_SIDE] * 0.3F;
        this.jaw.x = c[GiraffePoses.JAW_SIDE];
        float out = c[GiraffePoses.TONGUE];
        // The tongue lies inside the muzzle until it is put out, and curls up at its tip.
        this.tongue.visible = out > 0.05F;
        this.tongue.z = -5.0F - 3.5F * out;
        this.tongue.xRot = -0.5F * out;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            this.ears[s].zRot = side * (1.35F + c[GiraffePoses.EAR_R + s]);
            this.ears[s].xRot = 0.1F;
        }
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = GiraffePoses.LEGS + k * GiraffePoses.PER_LEG;
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
            leg[0].zRot = side * c[at + 1] - c[GiraffePoses.ROLL] * 0.7F;
        }
    }
}
