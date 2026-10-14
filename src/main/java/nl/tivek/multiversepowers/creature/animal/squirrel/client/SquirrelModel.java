package nl.tivek.multiversepowers.creature.animal.squirrel.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;

// The squirrel's body: a slight chest on thin forelegs bending at the elbow and ending in small grasping paws, heavy
// haunches on long hind legs that bend at the knee and the ankle, a rounded head with a short snout, big dark eyes
// that blink and upright ears with a tuft at their tips, and a bushy tail in four pieces, as long as the rest of it,
// carried up over its back in an S and streaming out behind when it runs. Its texture is painted by
// scripts/models/animals. SquirrelPoses moves it.
public final class SquirrelModel extends HierarchicalModel<Squirrel> {
    static final float BODY_Y = 20.0F;
    // The hips in the body's own frame, through the haunches.
    static final float HIP_Y = 0.5F;
    static final float HIP_Z = 2.5F;
    private static final float[] TAIL_REST = { 1.5F, 0.5F, -0.9F, -0.5F };
    private static final float YOUNG_HEAD = 1.3F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart hind;
    private final ModelPart head;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[4];
    // Right front, left front: the leg and its paw. Right hind, left hind: thigh, shank and foot.
    private final ModelPart[][] legs = new ModelPart[4][];
    private final SquirrelPoses poses = new SquirrelPoses();

    public SquirrelModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.hind = this.body.getChild("hind");
        this.head = this.body.getChild("head");
        this.ears[0] = this.head.getChild("right_ear");
        this.ears[1] = this.head.getChild("left_ear");
        this.eyes[0] = this.head.getChild("right_eye");
        this.eyes[1] = this.head.getChild("left_eye");
        this.tail[0] = this.hind.getChild("tail");
        this.tail[1] = this.tail[0].getChild("middle");
        this.tail[2] = this.tail[1].getChild("outer");
        this.tail[3] = this.tail[2].getChild("tip");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "right_" : "left_";
            ModelPart arm = this.body.getChild(name + "front_leg");
            this.legs[s] = new ModelPart[] { arm, arm.getChild("paw") };
            ModelPart thigh = this.hind.getChild(name + "hind_leg");
            ModelPart shank = thigh.getChild("middle");
            this.legs[2 + s] = new ModelPart[] { thigh, shank, shank.getChild("foot") };
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition hind = body.addOrReplaceChild("hind", CubeListBuilder.create()
                .texOffs(14, 0).addBox(-2.0F, -2.0F, 1.0F, 4.0F, 4.0F, 3.0F), PartPose.ZERO);
        PartDefinition tail = hind.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(16, 8).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -1.0F, 3.5F));
        PartDefinition middle = tail.addOrReplaceChild("middle", CubeListBuilder.create()
                .texOffs(26, 8).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 2.5F));
        PartDefinition outer = middle.addOrReplaceChild("outer", CubeListBuilder.create()
                .texOffs(40, 8).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 3.5F));
        outer.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(52, 8).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 2.5F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(28, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 3.0F, 3.0F)
                .texOffs(40, 0).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -2.5F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1.0F : 1.0F;
            String name = s == 0 ? "right_" : "left_";
            head.addOrReplaceChild(name + "ear", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(46, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                    .texOffs(54, 2).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.0F, -2.0F, -0.5F));
            // A big eye at the front corner of the head; it shuts by folding down to nothing.
            head.addOrReplaceChild(name + "eye", CubeListBuilder.create()
                    .texOffs(58, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(side * 1.1F, 0.0F, -2.6F));
            PartDefinition arm = body.addOrReplaceChild(name + "front_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(50, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                    PartPose.offset(side * 1.0F, 1.0F, -2.0F));
            arm.addOrReplaceChild("paw", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(54, 0).addBox(-0.5F, 0.0F, -0.7F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 2.0F, 0.0F));
            PartDefinition thigh = hind.addOrReplaceChild(name + "hind_leg", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 8).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offset(side * 1.5F, HIP_Y, HIP_Z));
            PartDefinition shank = thigh.addOrReplaceChild("middle", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(0, 13).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, 1.5F, 0.0F));
            shank.addOrReplaceChild("foot", CubeListBuilder.create().mirror(s == 1)
                    .texOffs(8, 8).addBox(-0.5F, 0.0F, -2.5F, 1.0F, 1.0F, 3.0F),
                    PartPose.offset(0.0F, 1.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Squirrel squirrel, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - squirrel.tickCount;
        float[] c = this.poses.of(squirrel, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        float pitch = c[SquirrelPoses.PITCH];
        float flex = c[SquirrelPoses.FLEX];
        // The body turns about its shoulders, then comes back so its hips stay where its hind legs stand.
        float hy = HIP_Y * Mth.cos(flex) - HIP_Z * Mth.sin(flex);
        float hz = HIP_Y * Mth.sin(flex) + HIP_Z * Mth.cos(flex);
        float wy = hy * Mth.cos(pitch) - hz * Mth.sin(pitch);
        float wz = hy * Mth.sin(pitch) + hz * Mth.cos(pitch);
        this.body.y = BODY_Y + c[SquirrelPoses.BOB] + (HIP_Y + c[SquirrelPoses.SINK] - wy);
        this.body.z = HIP_Z - wz;
        this.body.xRot = pitch;
        this.body.zRot = c[SquirrelPoses.ROLL];
        this.hind.xRot = flex;
        float look = c[SquirrelPoses.LOOK];
        this.head.xRot = c[SquirrelPoses.HEAD] - pitch + headPitch * Mth.DEG_TO_RAD * look;
        this.head.yRot = c[SquirrelPoses.HEAD_YAW] + netHeadYaw * Mth.DEG_TO_RAD * look;
        this.head.zRot = c[SquirrelPoses.HEAD_ROLL];
        this.ears[0].zRot = -c[SquirrelPoses.EARS];
        this.ears[1].zRot = c[SquirrelPoses.EARS];
        // The tail keeps its curve against the ground, whatever the body does.
        this.tail[0].xRot = TAIL_REST[0] + c[SquirrelPoses.TAIL] - pitch - flex;
        this.tail[0].yRot = c[SquirrelPoses.TAIL_SIDE];
        this.tail[1].xRot = TAIL_REST[1] + c[SquirrelPoses.TAIL_MID];
        this.tail[1].yRot = c[SquirrelPoses.TAIL_SIDE] * 0.7F;
        this.tail[2].xRot = TAIL_REST[2] + c[SquirrelPoses.TAIL_TIP];
        this.tail[2].yRot = c[SquirrelPoses.TAIL_WAVE];
        this.tail[3].xRot = TAIL_REST[3] + c[SquirrelPoses.TAIL_TIP] * 0.6F;
        this.tail[3].yRot = c[SquirrelPoses.TAIL_WAVE] * 1.3F;
        float open = 1.0F - 0.9F * Mth.clamp(c[SquirrelPoses.BLINK], 0.0F, 1.0F);
        this.eyes[0].yScale = open;
        this.eyes[1].yScale = open;
        if (this.young) {
            this.head.xScale = this.head.yScale = this.head.zScale = YOUNG_HEAD;
        }
        for (int k = 0; k < 4; k++) {
            int at = SquirrelPoses.LEGS + k * SquirrelPoses.PER_LEG;
            float side = k % 2 == 0 ? 1.0F : -1.0F;
            boolean front = k < 2;
            ModelPart[] leg = this.legs[k];
            // A leg's swing is told against the ground, whatever the body does.
            leg[0].xRot = c[at] - pitch - (front ? 0.0F : flex);
            leg[0].zRot = side * c[at + 1];
            if (front) {
                leg[1].xRot = c[at + 3];
            } else {
                leg[1].xRot = c[at + 2];
                leg[2].xRot = c[at + 3];
            }
        }
    }
}
