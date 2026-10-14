package nl.tivek.multiversepowers.creature.animal.snake.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;

// The snake's body: a chain of short pieces jointed end to end, built outward from the middle one (which lies where
// the creature is) so that a bend at any joint carries what lies beyond it: four pieces forward to a flat, wider head
// with a rounded snout and chin, whose skull and jaw open apart and whose forked tongue flicks out, and eight back, the
// last three a thin tail. Its texture is painted by scripts/models/animals. SnakePoses bends it.
public final class SnakeModel extends HierarchicalModel<Snake> {
    // Joints from the head back: the head's, four before the middle piece, eight behind it.
    static final int FRONT = 5;
    static final int JOINTS = 13;
    // The thick pieces from the neck to the start of the tail, the middle one among them, then the tail's.
    static final int PIECES = 10;
    static final int TAIL = 3;
    private static final float BODY_Y = 23.0F;
    private final ModelPart root;
    private final ModelPart middle;
    private final ModelPart[] joints = new ModelPart[JOINTS];
    private final ModelPart skull;
    private final ModelPart jaw;
    private final ModelPart tongue;
    private final SnakePoses poses = new SnakePoses();

    public SnakeModel(ModelPart root) {
        this.root = root;
        this.middle = root.getChild("middle");
        ModelPart part = this.middle;
        for (int j = FRONT - 1; j >= 0; j--) {
            part = part.getChild("front");
            this.joints[j] = part;
        }
        this.skull = part.getChild("skull");
        this.jaw = part.getChild("jaw");
        this.tongue = part.getChild("tongue");
        part = this.middle;
        for (int j = FRONT; j < JOINTS; j++) {
            part = part.getChild("back");
            this.joints[j] = part;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int neck = FRONT - 1;
        PartDefinition middle = root.addOrReplaceChild("middle", CubeListBuilder.create()
                .texOffs(u(neck), v(neck)).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, BODY_Y, 0.0F));
        PartDefinition part = middle;
        for (int k = 0; k < neck; k++) {
            // Counted from the head along the body, as the texture lays the pieces out.
            int piece = neck - 1 - k;
            part = part.addOrReplaceChild("front", CubeListBuilder.create()
                    .texOffs(u(piece), v(piece)).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, k == 0 ? -1.0F : -2.0F));
        }
        PartDefinition head = part.addOrReplaceChild("front", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        head.addOrReplaceChild("skull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 1.0F, 3.0F)
                .texOffs(30, 0).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 1.0F, 1.0F), PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(12, 0).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 3.0F)
                .texOffs(36, 0).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 1.0F, 1.0F), PartPose.ZERO);
        head.addOrReplaceChild("tongue", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 0.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        part = middle;
        for (int k = 0; k < PIECES - FRONT; k++) {
            int piece = FRONT + k;
            part = part.addOrReplaceChild("back", CubeListBuilder.create()
                    .texOffs(u(piece), v(piece)).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, k == 0 ? 1.0F : 2.0F));
        }
        for (int k = 0; k < TAIL; k++) {
            part = part.addOrReplaceChild("back", CubeListBuilder.create()
                    .texOffs(16 + k * 6, 8).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                    PartPose.offset(0.0F, 0.0F, 2.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    // Where the thick piece `piece` (counted from the head) lies in the texture: eight to a row.
    static int u(int piece) {
        return piece % 8 * 8;
    }

    static int v(int piece) {
        return 4 + piece / 8 * 4;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Snake snake, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - snake.tickCount;
        float[] c = this.poses.of(snake, limbSwing, limbSwingAmount, ageInTicks, partialTick).v;
        this.middle.z = c[SnakePoses.LUNGE];
        float look = c[SnakePoses.LOOK];
        // A bend to one side is the same bend at any joint, but the pieces before the middle hang from it the other
        // way round. The head then turns back by what the neck's bends add up to, so it keeps facing ahead.
        float neck = 0.0F;
        for (int j = 1; j < JOINTS; j++) {
            float bend = c[SnakePoses.BEND + j];
            if (j < FRONT) {
                this.joints[j].yRot = -bend;
                this.joints[j].xRot = c[SnakePoses.RISE + j];
                neck += bend;
            } else {
                this.joints[j].yRot = bend;
            }
        }
        ModelPart head = this.joints[0];
        head.yRot = neck * 0.85F - c[SnakePoses.BEND] + netHeadYaw * Mth.DEG_TO_RAD * look;
        head.xRot = c[SnakePoses.RISE] + headPitch * Mth.DEG_TO_RAD * look * 0.5F;
        this.skull.xRot = -c[SnakePoses.GAPE] * 0.6F;
        this.jaw.xRot = c[SnakePoses.GAPE] * 0.7F;
        float flick = c[SnakePoses.TONGUE];
        this.tongue.visible = flick > 0.05F;
        this.tongue.zScale = flick;
        this.tongue.xRot = 0.35F * Mth.sin(ageInTicks * 2.6F) * flick;
    }
}
