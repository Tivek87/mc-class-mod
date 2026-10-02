package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.BodyPose;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.PoseBlend;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

// A creature gone limp, drawn: built from the pose the creature was drawn in, so it never jumps, every part it moves
// is put where its box is each frame; getting up, it goes from how it lay to its own pose by way of GetUp.
final class Ragdoll extends RagdollBody {
    // Scratch for drawing, which only ever happens on the render thread.
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Quaterniond A = new Quaterniond();
    private static final Quaterniond B = new Quaterniond();
    private static final Quaterniond PARENT = new Quaterniond();
    private static final Quaterniond OWN = new Quaterniond();
    private static final Quaterniond MIXED = new Quaterniond();
    private static final Vector3d EULER = new Vector3d();
    private static final Vector3d LOCAL = new Vector3d();
    private static final Vector3d MIDDLE = new Vector3d();
    private static final Vector3f ORIGIN = new Vector3f();
    private static final Matrix3d ROTATION = new Matrix3d();
    private static final BodyPose LIE = new BodyPose();
    private static final BodyPose OWN_POSE = new BodyPose();
    private static final BodyPose OUT = new BodyPose();
    private static final Quaternionf[] TURNS_1 = { new Quaternionf() };
    private static final Quaternionf[] TURNS_2 = { new Quaternionf(), new Quaternionf() };
    private static final Matrix4f TRUNK = new Matrix4f();
    private static final Matrix4f LINK = new Matrix4f();
    private static final Matrix4f LINK_TRUNK = new Matrix4f();
    private static final Matrix4f NONE = new Matrix4f();

    Ragdoll(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, ModelBends.Bend[][] chains,
            int[] hang, float[][] blades, int core, State state) {
        super(entity, model, parts, chains, hang, blades, core, state);
    }

    static double scaleOf(Matrix4f frame) {
        return Math.sqrt(frame.m00() * frame.m00() + frame.m01() * frame.m01() + frame.m02() * frame.m02());
    }

    static void rotationOf(Matrix4f frame, Quaterniond out) {
        double s = scaleOf(frame);
        Matrix3d m = new Matrix3d(frame.m00() / s, frame.m01() / s, frame.m02() / s, frame.m10() / s,
                frame.m11() / s, frame.m12() / s, frame.m20() / s, frame.m21() / s, frame.m22() / s);
        out.setFromNormalized(m);
    }

    // Puts every moving part (and the parts that copy it) where its box is this frame, relative to how the model is
    // drawn now, `sink` blocks lower. While a creature takes back its own pose (limp below 1) a part keeps some of it;
    // getting up, it goes from how it lay to its own pose by way of GetUp.
    void pose(Matrix4f drawn, Vec3 camera, double partialTick, double sink, Restore restore) {
        boolean rising = this.phase == Phase.UP && this.lay != null;
        double[] from = rising ? this.lay : this.was;
        double[] to = rising ? this.lay : this.now;
        double t = rising ? 1.0 : partialTick;
        int n = this.parts.size();
        ModelParts.Part trunk = this.parts.get(this.core);
        // The trunk first: a part drawn inside it is placed from where the trunk lies, not from its own pose.
        ModelParts.parentFrame(this.model, drawn, trunk, TRUNK);
        this.lie(this.core, TRUNK, camera, from, to, t, sink);
        ModelPart c = trunk.part();
        TRUNK.translate(LIE.pos[this.core].x / 16.0F, LIE.pos[this.core].y / 16.0F, LIE.pos[this.core].z / 16.0F)
                .rotate(LIE.rot[this.core]).scale(c.xScale, c.yScale, c.zScale);
        this.hanging.trunk.set(c.xScale, c.yScale, c.zScale);
        boolean young = this.model.young;
        if (young) {
            ModelParts.parentFrame(this.model, NONE, trunk, LINK_TRUNK).invert();
        }
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            if (i != this.core) {
                this.lie(i, this.hanging.inside[i] ? TRUNK : ModelParts.parentFrame(this.model, drawn, part, FRAME),
                        camera, from, to, t, sink);
            }
            // A young creature's head is drawn bigger than its body: where it hangs, in the trunk's parent's pixels.
            if (young && !this.hanging.inside[i]) {
                LINK.set(LINK_TRUNK).mul(ModelParts.parentFrame(this.model, NONE, part, FRAME));
                this.hanging.scale[i] = (float) scaleOf(LINK);
                this.hanging.move[i].set(LINK.m30() * 16.0F, LINK.m31() * 16.0F, LINK.m32() * 16.0F);
            } else {
                this.hanging.scale[i] = 1.0F;
                this.hanging.move[i].zero();
            }
            ModelPart target = part.part();
            OWN_POSE.pos[i].set(target.x, target.y, target.z);
            OWN_POSE.rot[i].rotationZYX(target.zRot, target.yRot, target.xRot);
            // A pose that bent it as the creature was drawn (a knee, a waist) is where taking its own pose back ends.
            for (int j = 0; j < BodyPose.JOINTS; j++) {
                Matrix3f drawnBent = j < this.chains[i].length ? BentParts.turn(target, j) : null;
                if (drawnBent == null) {
                    OWN_POSE.joint[j][i].identity();
                } else {
                    drawnBent.getNormalizedRotation(OWN_POSE.joint[j][i]);
                }
            }
        }
        if (rising) {
            if (this.rise == null) {
                this.rise = GetUp.start(this.hanging, this.model, this.parts, this.chains, LIE, this.person,
                        this.entity);
            }
            float u = Math.min(1.0F, (float) ((this.up + partialTick) / GetUp.ticks(this.person)));
            this.rise.pose(u, LIE, OWN_POSE, OUT);
        } else {
            float w = (float) Math.max(0.0, Math.min(1.0, this.limp));
            PoseBlend.blend(this.hanging, OWN_POSE, LIE, w, w, OUT);
        }
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            ModelPart target = part.part();
            restore.keep(target);
            MIXED.set(OUT.rot[i].x, OUT.rot[i].y, OUT.rot[i].z, OUT.rot[i].w);
            MIXED.get(ROTATION);
            ROTATION.getEulerAnglesZYX(EULER);
            target.setPos(OUT.pos[i].x, OUT.pos[i].y, OUT.pos[i].z);
            target.setRotation((float) EULER.x, (float) EULER.y, (float) EULER.z);
            List<ModelPart> followers = part.followers();
            for (int k = 0; k < followers.size(); k++) {
                restore.keep(followers.get(k));
                followers.get(k).copyFrom(target);
            }
            ModelBends.Bend[] chain = this.chains[i];
            if (chain.length > 0) {
                // A bent trunk carries what is drawn inside it, all but the parts that move on their own.
                Quaternionf[] turns = chain.length == 1 ? TURNS_1 : TURNS_2;
                for (int j = 0; j < chain.length; j++) {
                    turns[j].set(OUT.joint[j][i]);
                }
                BentParts.bend(target, chain, turns, this.own);
                for (int k = 0; k < followers.size(); k++) {
                    BentParts.bend(followers.get(k), chain, turns, this.own);
                }
            }
        }
        if (rising) {
            this.show(drawn, camera);
        }
    }

    // Where every body is drawn now (the parts just posed), to go limp from there should it be thrown or die.
    private void show(Matrix4f drawn, Vec3 camera) {
        for (int b = 0; b < this.world.count(); b++) {
            ModelParts.Part part = this.parts.get(this.partOf[b]);
            ModelParts.frame(this.model, drawn, part, FRAME);
            if (this.pieceOf[b] > 0) {
                FRAME.scale(1.0F / 16.0F).mul(BentParts.piece(part.part(), this.pieceOf[b], LINK.identity()))
                        .scale(16.0F);
            }
            FRAME.transformPosition((float) this.middle[b * 3], (float) this.middle[b * 3 + 1],
                    (float) this.middle[b * 3 + 2], ORIGIN);
            rotationOf(FRAME, A);
            int o = b * 7;
            this.shown[o] = ORIGIN.x + camera.x;
            this.shown[o + 1] = ORIGIN.y + camera.y;
            this.shown[o + 2] = ORIGIN.z + camera.z;
            this.shown[o + 3] = A.x;
            this.shown[o + 4] = A.y;
            this.shown[o + 5] = A.z;
            this.shown[o + 6] = A.w;
        }
        this.shownValid = true;
    }

    // Where part i lies this frame as a local pose in `parent` (the frame it hangs in): its place and turn in LIE,
    // and the turns at its joints.
    private void lie(int i, Matrix4f parent, Vec3 camera, double[] from, double[] to, double t, double sink) {
        int o = i * 7;
        double px = from[o] + (to[o] - from[o]) * t - camera.x;
        double py = from[o + 1] + (to[o + 1] - from[o + 1]) * t - camera.y - sink;
        double pz = from[o + 2] + (to[o + 2] - from[o + 2]) * t - camera.z;
        A.set(from[o + 3], from[o + 4], from[o + 5], from[o + 6]);
        B.set(to[o + 3], to[o + 4], to[o + 5], to[o + 6]);
        A.slerp(B, t);
        double s = scaleOf(parent);
        rotationOf(parent, PARENT);
        // The part's own turn: its box's turn seen from the part it hangs in.
        OWN.set(PARENT).conjugate().mul(A);
        parent.transformPosition(ORIGIN.set(0.0F, 0.0F, 0.0F));
        LOCAL.set(px - ORIGIN.x, py - ORIGIN.y, pz - ORIGIN.z);
        MIXED.set(PARENT).conjugate().transform(LOCAL);
        LOCAL.div(s);
        MIDDLE.set(this.center[i * 3], this.center[i * 3 + 1], this.center[i * 3 + 2]);
        OWN.transform(MIDDLE);
        LOCAL.sub(MIDDLE);
        LIE.pos[i].set((float) (LOCAL.x * 16.0), (float) (LOCAL.y * 16.0), (float) (LOCAL.z * 16.0));
        LIE.rot[i].set((float) OWN.x, (float) OWN.y, (float) OWN.z, (float) OWN.w);
        // Each piece's turn seen from the piece before it: how far the knee, elbow or waist is bent, then the ankle,
        // wrist or pelvis.
        int before = this.body[i];
        for (int j = 0; j < BodyPose.JOINTS; j++) {
            int piece = j == 0 ? this.lower[i] : this.tip[i];
            if (piece < 0) {
                LIE.joint[j][i].identity();
                continue;
            }
            int a = before * 7;
            int l = piece * 7;
            A.set(from[a + 3], from[a + 4], from[a + 5], from[a + 6]);
            MIXED.set(to[a + 3], to[a + 4], to[a + 5], to[a + 6]);
            A.slerp(MIXED, t);
            B.set(from[l + 3], from[l + 4], from[l + 5], from[l + 6]);
            MIXED.set(to[l + 3], to[l + 4], to[l + 5], to[l + 6]);
            B.slerp(MIXED, t);
            MIXED.set(A).conjugate().mul(B);
            LIE.joint[j][i].set((float) MIXED.x, (float) MIXED.y, (float) MIXED.z, (float) MIXED.w);
            before = piece;
        }
    }

    // A layer's own copy of the model (a sheep's wool, a saddle) takes the pose just given to the model itself.
    void copyTo(EntityModel<?> copy, Restore restore) {
        ModelPart[] to = this.copies.get(copy);
        if (to == null) {
            to = new ModelPart[this.moved.length];
            for (int i = 0; i < this.moved.length; i++) {
                to[i] = ModelParts.counterpart(this.model, copy, this.moved[i]);
            }
            this.copies.put(copy, to);
        }
        for (int i = 0; i < to.length; i++) {
            if (to[i] != null && to[i] != this.moved[i]) {
                restore.keep(to[i]);
                to[i].copyFrom(this.moved[i]);
                BentParts.same(this.moved[i], to[i]);
            }
        }
    }
}
