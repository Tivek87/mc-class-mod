package nl.tivek.multiversepowers.creature.animal.flamingo.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.flamingo.Flamingo;

// The flamingo's rig, box for box as the Caribbean flamingo was designed (flamingo_demo.html, docs/reference): an ovoid
// body high on stilt legs with a swelling breast, a drooping tail with pointed plumes, layered wings (a covert shield,
// the tertial plume and the black primaries under it), a five-piece S-curved neck, a round head with eyelids that close
// over its eyes, a bill sloping down then bent sharply down at its black hook, a jaw that opens, and legs with a
// swollen joint halfway down that bends back and a webbed foot. The chick: the same body on short thick legs and a
// short neck, no plumes. Sizes and places are the design's own (y up, z forward), turned into the game's (y down, z
// back) as they are built. Its texture, twice as fine as the boxes, is painted by scripts/models/animals; FlamingoPoses
// moves it.
public final class FlamingoModel extends HierarchicalModel<Flamingo> {
    // Where the body sits above the ground at rest, and how far the whole bird is lifted so its feet stand on it.
    static final float REST_Y = 16.0F;
    static final float CHICK_Y = 10.2F;
    private static final float LIFT = 0.825F;
    private static final float CHICK_LIFT = 0.625F;
    private static final float[][] NECK = { { 3.1F, 3.8F, 3.1F, 3.3F }, { 2.7F, 3.8F, 2.7F, 3.3F },
            { 2.5F, 4.0F, 2.5F, 3.5F }, { 2.4F, 3.8F, 2.4F, 3.3F }, { 2.3F, 3.2F, 2.4F, 2.7F } };
    private static final float[][] CHICK_NECK = { { 2.8F, 2.6F, 2.8F, 2.2F }, { 2.5F, 2.4F, 2.5F, 2.0F },
            { 2.3F, 2.2F, 2.3F, 1.8F }, { 2.2F, 2.0F, 2.2F, 1.6F }, { 2.1F, 1.8F, 2.1F, 1.4F } };
    private final boolean chick;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart[] neck = new ModelPart[5];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] lids = new ModelPart[2];
    // Left, right: the wing and its outer piece.
    private final ModelPart[][] wings = new ModelPart[2][2];
    // Left, right: hip, the joint halfway down, foot.
    private final ModelPart[][] legs = new ModelPart[2][3];
    private final FlamingoPoses poses = new FlamingoPoses();

    public FlamingoModel(ModelPart root, boolean chick) {
        this.chick = chick;
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        ModelPart piece = this.body;
        for (int k = 0; k < this.neck.length; k++) {
            piece = piece.getChild("neck");
            this.neck[k] = piece;
        }
        this.head = piece.getChild("head");
        this.jaw = this.head.getChild("bill").getChild("jaw");
        for (int s = 0; s < 2; s++) {
            String name = s == 0 ? "left_" : "right_";
            this.lids[s] = this.head.getChild(name + "lid");
            ModelPart wing = this.body.getChild(name + "wing");
            this.wings[s] = new ModelPart[] { wing, wing.getChild("outer") };
            ModelPart hip = this.body.getChild(name + "leg");
            ModelPart ankle = hip.getChild("ankle");
            this.legs[s] = new ModelPart[] { hip, ankle, ankle.getChild("foot") };
        }
    }

    public static LayerDefinition createLayer() {
        return create(false);
    }

    public static LayerDefinition createChickLayer() {
        return create(true);
    }

    private static LayerDefinition create(boolean chick) {
        MeshDefinition mesh = new MeshDefinition();
        Sheet sheet = new Sheet();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", sheet.box(sheet.box(CubeListBuilder.create(),
                6.6F, 6.0F, 9.6F, 0.0F, 0.0F, 0.0F), 5.8F, 5.0F, 3.2F, 0.0F, -0.1F, 4.4F),
                at(0.0F, (chick ? CHICK_Y : REST_Y) + (chick ? CHICK_LIFT : LIFT) - 24.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", sheet.box(CubeListBuilder.create(), 5.2F, 3.8F, 4.6F,
                0.0F, 0.0F, -1.8F), turned(0.0F, 0.2F, -4.4F, -0.38F, 0.0F, 0.0F));
        // Pointed drooping plume tips; the chick has none.
        tail.addOrReplaceChild("tip", chick ? CubeListBuilder.create() : sheet.box(CubeListBuilder.create(), 3.8F,
                2.2F, 4.4F, 0.0F, -0.8F, -5.0F), turned(0.0F, 0.0F, 0.0F, -0.26F, 0.0F, 0.0F));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1.0F : -1.0F;
            boolean left = s == 0;
            String name = left ? "left_" : "right_";
            PartDefinition wing = body.addOrReplaceChild(name + "wing", sheet.box(CubeListBuilder.create()
                    .mirror(!left), 1.6F, 5.2F, 9.6F, side * 0.5F, -2.0F, -4.0F), at(side * 3.3F, 1.4F, 3.2F));
            // The tertial plume and, under it, the black primaries that flash as the wing opens.
            wing.addOrReplaceChild("outer", chick ? CubeListBuilder.create() : sheet.box(sheet.box(
                    CubeListBuilder.create().mirror(!left), 1.2F, 3.6F, 4.4F, 0.0F, -0.4F, -1.8F),
                    0.7F, 4.0F, 8.6F, -side * 0.25F, -0.6F, 2.2F), at(side * 0.6F, -2.0F, -7.8F));
            if (left) {
                sheet.rewind(chick ? 1 : 3);
            }
        }
        float[][] specs = chick ? CHICK_NECK : NECK;
        PartDefinition piece = body;
        for (int k = 0; k < 5; k++) {
            float[] spec = specs[k];
            piece = piece.addOrReplaceChild("neck", sheet.box(CubeListBuilder.create(), spec[0], spec[1], spec[2],
                    0.0F, spec[1] * 0.42F, 0.0F), k == 0 ? at(0.0F, -1.0F, 5.2F) : at(0.0F, specs[k - 1][3], 0.0F));
        }
        PartDefinition head = piece.addOrReplaceChild("head", sheet.box(CubeListBuilder.create(), 3.0F, 3.0F, 3.6F,
                0.0F, 1.1F, 0.9F), at(0.0F, specs[4][3], 0.2F));
        for (int s = 0; s < 2; s++) {
            boolean left = s == 0;
            // An eyelid over the eye, hung from its top edge: it closes down over it as the eye blinks.
            head.addOrReplaceChild((left ? "left_" : "right_") + "lid", sheet.box(CubeListBuilder.create()
                    .mirror(!left), 0.1F, 0.95F, 0.95F, 0.0F, -0.475F, 0.0F), at(left ? 1.53F : -1.53F, 1.725F, 1.35F));
            if (left) {
                sheet.rewind(1);
            }
        }
        PartDefinition bill = head.addOrReplaceChild("bill", sheet.box(CubeListBuilder.create(), 2.2F, 1.9F, 2.8F,
                0.0F, -0.2F, 1.1F), turned(0.0F, 0.9F, 2.4F, 0.42F, 0.0F, 0.0F));
        bill.addOrReplaceChild("hook", sheet.box(CubeListBuilder.create(), 1.85F, 2.9F, 1.7F, 0.0F, -1.2F, 0.2F),
                turned(0.0F, -0.4F, 2.2F, 0.58F, 0.0F, 0.0F));
        bill.addOrReplaceChild("jaw", sheet.box(CubeListBuilder.create(), 1.65F, 1.1F, 2.3F, 0.0F, -0.3F, 0.9F),
                at(0.0F, -0.6F, 0.4F));
        float upper = chick ? 4.2F : 7.2F;
        float lower = chick ? 4.4F : 7.4F;
        float thick = chick ? 1.25F : 1.05F;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1.0F : -1.0F;
            boolean left = s == 0;
            PartDefinition hip = body.addOrReplaceChild((left ? "left_" : "right_") + "leg", sheet.box(sheet.box(
                    CubeListBuilder.create().mirror(!left), thick, upper, thick, 0.0F, -upper / 2.0F, 0.0F),
                    thick * 1.38F, 1.5F, thick * 1.45F, 0.0F, -upper, -0.1F), at(side * 1.65F, -2.2F, -0.4F));
            PartDefinition ankle = hip.addOrReplaceChild("ankle", sheet.box(CubeListBuilder.create().mirror(!left),
                    thick * 0.92F, lower, thick * 0.92F, 0.0F, -lower / 2.0F, 0.0F), at(0.0F, -upper, 0.0F));
            ankle.addOrReplaceChild("foot", sheet.box(CubeListBuilder.create().mirror(!left), 2.9F, 0.55F, 3.8F,
                    0.0F, 0.25F, 1.3F), at(0.0F, -lower, 0.0F));
            if (left) {
                sheet.rewind(4);
            }
        }
        return LayerDefinition.create(mesh, Sheet.WIDTH, Sheet.WIDTH);
    }

    // A place in the design's frame (y up, z forward) as the game's (y down, z back).
    private static PartPose at(float x, float y, float z) {
        return PartPose.offset(x, -y, -z);
    }

    private static PartPose turned(float x, float y, float z, float rx, float ry, float rz) {
        return PartPose.offsetAndRotation(x, -y, -z, rx, -ry, -rz);
    }

    // Lays the boxes out in the texture in order, row after row, each rounded to half a pixel for its picture (the
    // texture holds two of its pixels to one of the model's) and grown back to its true size; the texture painter lays
    // them out the same way.
    static final class Sheet {
        static final int WIDTH = 128;
        private int u;
        private int v;
        private int row;
        private final int[][] placed = new int[64][];
        private int count;

        CubeListBuilder box(CubeListBuilder builder, float w, float h, float d, float cx, float cy, float cz) {
            float rw = half(w);
            float rh = half(h);
            float rd = half(d);
            int fw = (int) Math.ceil(2.0F * (rw + rd));
            int fh = (int) Math.ceil(rd + rh);
            if (this.u + fw > WIDTH) {
                this.u = 0;
                this.v += this.row;
                this.row = 0;
            }
            this.placed[this.count++] = new int[] { this.u, this.v, this.row };
            builder.texOffs(this.u, this.v).addBox(cx - rw / 2.0F, -cy - rh / 2.0F, -cz - rd / 2.0F, rw, rh, rd,
                    new CubeDeformation((w - rw) / 2.0F, (h - rh) / 2.0F, (d - rd) / 2.0F));
            this.u += fw;
            this.row = Math.max(this.row, fh);
            return builder;
        }

        // The other side's copy of the last `boxes` boxes takes the same place in the texture.
        void rewind(int boxes) {
            int[] first = this.placed[this.count - boxes];
            this.u = first[0];
            this.v = first[1];
            this.row = first[2];
            this.count -= boxes;
        }

        private static float half(float size) {
            return Math.max(0.5F, Math.round(size * 2.0F) / 2.0F);
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(Flamingo flamingo, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float partialTick = ageInTicks - flamingo.tickCount;
        float[] c = this.poses.of(flamingo, limbSwingAmount, ageInTicks, partialTick, this.chick).v;
        this.body.y -= c[FlamingoPoses.BODY_Y];
        this.body.x += c[FlamingoPoses.BODY_X];
        turn(this.body, c, FlamingoPoses.BODY);
        turn(this.tail, c, FlamingoPoses.TAIL);
        for (int k = 0; k < 5; k++) {
            turn(this.neck[k], c, FlamingoPoses.NECK + 3 * k);
        }
        turn(this.head, c, FlamingoPoses.HEAD);
        float look = c[FlamingoPoses.LOOK];
        this.neck[3].yRot += netHeadYaw * Mth.DEG_TO_RAD * look * 0.25F;
        this.neck[4].yRot += netHeadYaw * Mth.DEG_TO_RAD * look * 0.25F;
        this.head.yRot += netHeadYaw * Mth.DEG_TO_RAD * look * 0.5F;
        this.head.xRot += headPitch * Mth.DEG_TO_RAD * look * 0.5F;
        this.jaw.xRot = c[FlamingoPoses.JAW];
        float shut = Mth.clamp(c[FlamingoPoses.BLINK], 0.0F, 1.0F);
        for (int s = 0; s < 2; s++) {
            this.lids[s].visible = shut > 0.02F;
            this.lids[s].yScale = shut;
            int wing = s == 0 ? FlamingoPoses.LEFT_WING : FlamingoPoses.RIGHT_WING;
            turn(this.wings[s][0], c, wing);
            this.wings[s][1].zRot = -c[wing + 3];
            int leg = s == 0 ? FlamingoPoses.LEFT_LEG : FlamingoPoses.RIGHT_LEG;
            ModelPart[] parts = this.legs[s];
            turn(parts[0], c, leg);
            parts[1].xRot = c[leg + 3];
            parts[1].zRot = -c[leg + 4];
            parts[2].xRot = c[leg + 5];
        }
    }

    // A turn told in the design's frame (x, y, z from `at`), given to a part in the game's: y and z turn the other way.
    private static void turn(ModelPart part, float[] c, int at) {
        part.xRot = c[at];
        part.yRot = -c[at + 1];
        part.zRot = -c[at + 2];
    }
}
