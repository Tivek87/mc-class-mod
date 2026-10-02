package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import javax.annotation.Nullable;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.pose.Shoulders;
import nl.tivek.multiversepowers.engine.rig.Ik;
import nl.tivek.multiversepowers.engine.rig.Limits;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person's body as it gets up: its trunk, head, arms and legs measured from its model as it was built, posed from
// the pelvis out by the turn at every joint (a Pose), its arms and legs reaching their hands and feet to points, and
// written into a BodyPose. In the pixels of the frame the trunk hangs in: y runs down, -z ahead, +x the person's own
// left; at rest its feet stand on `ground`. Render thread only.
final class Skeleton {
    static final int HEAD = 0;
    static final int BODY = 1;
    // Its four limbs: the right arm and left arm, the right leg and left leg (roles 2 to 5).
    static final int LIMBS = 4;

    // A pose of the whole body: where the hips' middle is and how the pelvis is turned (in the trunk's parent's frame);
    // the belly's turn in the chest's axes (the waist) and the pelvis's in the belly's (the low back); the head's turn
    // from its rest in the chest's axes; each shoulder blade's turn in the chest's axes; each limb's turn from its rest
    // (an arm in its blade's axes, a leg in the pelvis's), and the turn at its elbow or knee and its wrist or ankle.
    static final class Pose {
        final Vector3f hips = new Vector3f();
        final Quaternionf pelvis = new Quaternionf();
        final Quaternionf waist = new Quaternionf();
        final Quaternionf low = new Quaternionf();
        final Quaternionf head = new Quaternionf();
        final Quaternionf[] blade = { new Quaternionf(), new Quaternionf() };
        final Quaternionf[] limb = turns();
        final Quaternionf[] mid = turns();
        final Quaternionf[] end = turns();

        private static Quaternionf[] turns() {
            return new Quaternionf[] { new Quaternionf(), new Quaternionf(), new Quaternionf(), new Quaternionf() };
        }

        // Every turn in one list, in order, for the pose's numbers to be read and written as one.
        Quaternionf[] all() {
            return new Quaternionf[] { this.pelvis, this.waist, this.low, this.head, this.blade[0], this.blade[1],
                    this.limb[0], this.limb[1], this.limb[2], this.limb[3], this.mid[0], this.mid[1], this.mid[2],
                    this.mid[3], this.end[0], this.end[1], this.end[2], this.end[3] };
        }

        Pose set(Pose other) {
            this.hips.set(other.hips);
            Quaternionf[] to = this.all();
            Quaternionf[] from = other.all();
            for (int i = 0; i < to.length; i++) {
                to[i].set(from[i]);
            }
            return this;
        }
    }

    final Hanging body;
    final int[] roles;
    private final ModelBends.Bend[][] chains;
    // The trunk's joints (a waist, a pelvis), where its hips' middle is and each of its pieces' boxes, in its own
    // pixels; how it lies in its parent's frame at rest.
    private final ModelBends.Bend[] trunk;
    final Vector3f hipsAt = new Vector3f();
    private final float[][] trunkPieces;
    private final Matrix4f trunkRest = new Matrix4f();
    // Where each part hangs and how it is turned at rest in the trunk's own frame; each part's pieces' boxes (its own
    // pixels); each arm's shoulder blade where it meets the spine, null without one.
    private final Vector3f[] hangAt = new Vector3f[6];
    private final Quaternionf[] restTurn = new Quaternionf[6];
    private final float[][][] pieces = new float[6][][];
    private final Vector3f[] blade = new Vector3f[2];
    // Where each limb's far end is in its own pixels (a fingertip's or a sole's middle), and each limb's lengths from
    // its pivot to its middle joint, on to its end joint and on to its far end; how thick it is, halved.
    final Vector3f[] tipAt = new Vector3f[LIMBS];
    final float[][] length = new float[LIMBS][3];
    final float[] half = new float[LIMBS];
    // The ground its feet stand on at rest, how tall it stands from the ground to its hips, and where its hips' middle
    // is as it stands at rest (the trunk's parent's pixels).
    final float ground;
    final float legLength;
    final Vector3f stands = new Vector3f();

    // Scratch.
    final Matrix4f chest = new Matrix4f();
    final Matrix4f belly = new Matrix4f();
    final Matrix4f pelvis = new Matrix4f();
    final Matrix4f head = new Matrix4f();
    final Matrix4f[][] limbs = new Matrix4f[LIMBS][3];
    private final Quaternionf turn = new Quaternionf();
    private final Quaternionf other = new Quaternionf();
    private final Vector3f v = new Vector3f();
    private final Vector3f w = new Vector3f();
    private final double[] root = new double[3];
    private final double[] target = new double[3];
    private final double[] pole = new double[3];
    private final double[] middle = new double[3];

    // `roles`: the parts that are its head, trunk, right and left arm and right and left leg; an arm -1 for a body that
    // gets up without its hands (its arms folded as one part, carried by its chest).
    Skeleton(Hanging body, List<ModelParts.Part> parts, ModelBends.Bend[][] chains, int[] roles) {
        this.body = body;
        this.roles = roles;
        this.chains = chains;
        ModelParts.Part trunkPart = parts.get(roles[BODY]);
        this.trunk = chains[roles[BODY]];
        ModelParts.rest(trunkPart, this.trunkRest);
        Matrix4f into = new Matrix4f(this.trunkRest).invert();
        this.trunkPieces = pieces(trunkPart.bounds(), this.trunk);
        float[] b = trunkPart.bounds();
        for (int r = 0; r < 6; r++) {
            if (roles[r] < 0) {
                continue;
            }
            ModelParts.Part part = parts.get(roles[r]);
            Matrix4f rest = new Matrix4f(into).mul(ModelParts.rest(part, new Matrix4f()));
            this.hangAt[r] = rest.transformPosition(new Vector3f());
            this.restTurn[r] = rest.getNormalizedRotation(new Quaternionf());
            this.pieces[r] = pieces(part.bounds(), r == BODY ? this.trunk : chains[roles[r]]);
        }
        // The hips' middle: on its spine level with where its legs hang (short of the hem of a robe).
        this.hipsAt.set((b[0] + b[3]) * 0.5F, (this.hangAt[4].y + this.hangAt[5].y) * 0.5F,
                (b[2] + b[5]) * 0.5F);
        int[] hang = body.hang;
        for (int s = 0; s < 2; s++) {
            float[] inner = roles[2 + s] < 0 ? null : ModelBends.shoulder(parts, roles[BODY], roles[2 + s], hang);
            this.blade[s] = inner == null ? null : new Vector3f(inner[0], inner[1], inner[2]);
        }
        for (int l = 0; l < LIMBS; l++) {
            if (!this.present(l)) {
                continue;
            }
            ModelParts.Part part = parts.get(roles[2 + l]);
            float[] box = part.bounds();
            ModelBends.Bend[] chain = chains[roles[2 + l]];
            int axis = chain[0].axis();
            float sign = chain[0].farSign();
            float far = sign > 0.0F ? box[axis + 3] : box[axis];
            this.tipAt[l] = new Vector3f((box[0] + box[3]) * 0.5F, (box[1] + box[4]) * 0.5F, (box[2] + box[5]) * 0.5F);
            this.tipAt[l].setComponent(axis, far);
            float knee = chain[0].at();
            float ankle = chain.length > 1 ? chain[1].at() : far;
            this.length[l][0] = Math.abs(knee);
            this.length[l][1] = Math.abs(ankle - knee);
            this.length[l][2] = Math.abs(far - ankle);
            float thick = 0.0F;
            for (int a = 0; a < 3; a++) {
                thick = a == axis ? thick : Math.max(thick, box[a + 3] - box[a]);
            }
            this.half[l] = thick * 0.5F;
        }
        for (int l = 0; l < LIMBS; l++) {
            for (int p = 0; p < 3; p++) {
                this.limbs[l][p] = new Matrix4f();
            }
        }
        Pose rest = new Pose();
        this.trunkRest.transformPosition(rest.hips.set(this.hipsAt));
        this.trunkRest.getNormalizedRotation(rest.pelvis);
        this.place(rest);
        this.ground = this.lowest();
        this.legLength = this.ground - rest.hips.y;
        this.stands.set(rest.hips);
    }

    // A part's box (pixels, its own frame) cut at its joints into its pieces, near to far.
    private static float[][] pieces(float[] b, ModelBends.Bend[] chain) {
        float[][] pieces = new float[chain.length + 1][];
        float[] rest = b;
        for (int k = 0; k < chain.length; k++) {
            ModelBends.Bend bend = chain[k];
            int a = bend.axis();
            float[] near = rest.clone();
            float[] far = rest.clone();
            near[bend.farSign() > 0.0F ? a + 3 : a] = bend.at();
            far[bend.farSign() > 0.0F ? a : a + 3] = bend.at();
            pieces[k] = near;
            rest = far;
        }
        pieces[chain.length] = rest;
        return pieces;
    }

    // A turn about a joint's middle, onto `frame`; `back` its inverse.
    private Matrix4f about(Matrix4f frame, float[] k, Quaternionf turn, boolean back) {
        this.other.set(turn);
        if (back) {
            this.other.conjugate();
        }
        return frame.translate(k[0], k[1], k[2]).rotate(this.other).translate(-k[0], -k[1], -k[2]);
    }

    // Every piece's frame for `pose` (pixels: from each piece's own, before its joints turn it, to the trunk's
    // parent's): the trunk's three, the head's and each limb's up to three.
    void place(Pose pose) {
        this.pelvis.translation(pose.hips).rotate(pose.pelvis).translate(-this.hipsAt.x, -this.hipsAt.y,
                -this.hipsAt.z);
        this.belly.set(this.pelvis);
        if (this.trunk.length > 1) {
            this.about(this.belly, this.trunk[1].knee(), pose.low, true);
        }
        this.chest.set(this.belly);
        if (this.trunk.length > 0) {
            this.about(this.chest, this.trunk[0].knee(), pose.waist, true);
        }
        Vector3f at = this.hangAt[HEAD];
        this.head.set(this.chest).translate(at.x, at.y, at.z).rotate(this.restTurn[HEAD]).rotate(pose.head);
        for (int l = 0; l < LIMBS; l++) {
            Matrix4f frame = this.limbs[l][0];
            int r = 2 + l;
            if (!this.present(l)) {
                for (int p = 0; p < 3; p++) {
                    this.limbs[l][p].set(this.chest);
                }
                continue;
            }
            at = this.hangAt[r];
            if (l < 2) {
                frame.set(this.chest);
                Vector3f inner = this.blade[l];
                if (inner != null) {
                    frame.translate(inner.x, inner.y, inner.z).rotate(pose.blade[l]).translate(at.x - inner.x,
                            at.y - inner.y, at.z - inner.z);
                } else {
                    frame.translate(at.x, at.y, at.z);
                }
            } else {
                frame.set(this.pelvis).translate(at.x, at.y, at.z);
            }
            frame.rotate(this.restTurn[r]).rotate(pose.limb[l]);
            ModelBends.Bend[] chain = this.chains[this.roles[r]];
            for (int p = 1; p < 3; p++) {
                Matrix4f piece = this.limbs[l][p].set(this.limbs[l][p - 1]);
                if (p <= chain.length) {
                    this.about(piece, chain[p - 1].knee(), p == 1 ? pose.mid[l] : pose.end[l], false);
                }
            }
        }
    }

    // The lowest point of the body as last placed (the largest y: y runs down), every piece's corners.
    float lowest() {
        float low = Float.NEGATIVE_INFINITY;
        Matrix4f[] trunkFrames = { this.chest, this.belly, this.pelvis };
        for (int p = 0; p < this.trunkPieces.length; p++) {
            low = Math.max(low, lowest(trunkFrames[Math.min(p, 2)], this.trunkPieces[p]));
        }
        low = Math.max(low, lowest(this.head, this.pieces[HEAD][0]));
        for (int l = 0; l < LIMBS; l++) {
            if (this.present(l)) {
                low = Math.max(low, this.lowestOf(l));
            }
        }
        return low;
    }

    // The lowest point of limb l as last placed.
    float lowestOf(int l) {
        float low = Float.NEGATIVE_INFINITY;
        float[][] own = this.pieces[2 + l];
        for (int p = 0; p < own.length; p++) {
            low = Math.max(low, lowest(this.limbs[l][p], own[p]));
        }
        return low;
    }

    private float lowest(Matrix4f frame, float[] box) {
        float low = Float.NEGATIVE_INFINITY;
        for (int c = 0; c < 8; c++) {
            frame.transformPosition(this.v.set((c & 1) == 0 ? box[0] : box[3], (c & 2) == 0 ? box[1] : box[4],
                    (c & 4) == 0 ? box[2] : box[5]));
            low = Math.max(low, this.v.y);
        }
        return low;
    }

    // Limb l's far end (a fingertip, a sole) as last placed.
    Vector3f tip(int l, Vector3f out) {
        return this.limbs[l][2].transformPosition(out.set(this.tipAt[l]));
    }

    // Limb l's middle joint (an elbow, a knee) as last placed.
    Vector3f joint(int l, Vector3f out) {
        float[] k = this.chains[this.roles[2 + l]][0].knee();
        return this.limbs[l][0].transformPosition(out.set(k[0], k[1], k[2]));
    }

    // Where limb l hangs (its shoulder or hip) as last placed.
    Vector3f pivot(int l, Vector3f out) {
        return this.limbs[l][0].transformPosition(out.set(0.0F, 0.0F, 0.0F));
    }

    // Which way limb l's piece p runs as last placed, from its near end to its far end.
    Vector3f along(int l, int p, Vector3f out) {
        return this.limbs[l][p].transformDirection(this.bone(l, out)).normalize();
    }

    // The way limb l's elbow or knee faces as last placed: the back of an arm, the front of a leg.
    Vector3f outer(int l, Vector3f out) {
        return this.limbs[l][0].transformDirection(out.set(0.0F, 0.0F, l < 2 ? 1.0F : -1.0F)).normalize();
    }

    // The axis a joint of the trunk (0 the waist, 1 the pelvis) or of limb l (0 its elbow or knee, 1 its wrist or
    // ankle) folds about, in its own axes; null when it has no such joint.
    @Nullable
    float[] hinge(int role, int j) {
        if (this.roles[role] < 0) {
            return null;
        }
        ModelBends.Bend[] chain = role == BODY ? this.trunk : this.chains[this.roles[role]];
        return j < chain.length ? chain[j].hinge() : null;
    }

    // Where arm `side`'s shoulder blade would put its arm turned by `turn`: the blade's own way from where it meets
    // the spine (trunk's own axes), or null without a blade.
    @Nullable
    Vector3f bladeWay(int side, Vector3f out) {
        Vector3f inner = this.blade[side];
        return inner == null ? null : out.set(this.hangAt[2 + side]).sub(inner).normalize();
    }

    // The way arm `side` points as `pose` turns it (from its shoulder to its hand, its blade included), in the
    // chest's own axes.
    Vector3f armWay(Pose pose, int side, Vector3f out) {
        this.turn.set(pose.blade[side]).mul(this.restTurn[2 + side]).mul(pose.limb[side]);
        return this.turn.transform(this.bone(side, out)).normalize();
    }

    // Arm `side`'s shoulder blade turned by `turn` (in the chest's axes about where it meets the spine), the arm
    // keeping the way it points: only its shoulder moves.
    void shrug(Pose pose, int side, Quaternionf turn) {
        Quaternionf rest = this.restTurn[2 + side];
        this.other.set(rest).mul(pose.limb[side]);
        this.other.premul(pose.blade[side]);
        pose.blade[side].set(turn);
        pose.limb[side].set(rest).conjugate().mul(this.turn.set(turn).conjugate()).mul(this.other);
    }

    // Limb l reaching its far end to `tip` (the trunk's parent's pixels) along `way` (its hand's or foot's way, from
    // the wrist or ankle to the far end), its elbow or knee out towards `toward`, the rest of the pose as it is: sets the
    // pose's turns of the limb, its middle joint and its end joint. The pose must have been placed.
    void reach(Pose pose, int l, Vector3f tip, Vector3f way, Vector3f toward) {
        boolean arm = l < 2;
        float upper = this.length[l][0];
        float lower = this.length[l][1];
        float last = this.length[l][2];
        if (arm && this.blade[l] != null) {
            // Its shoulder blade follows the arm reaching there, as Shoulders turns it under any arm.
            Vector3f to = new Vector3f(tip).sub(this.pivot(l, new Vector3f()));
            float stretch = Math.min(1.0F, to.length() / (upper + lower + last));
            this.chest.getNormalizedRotation(new Quaternionf()).transformInverse(to);
            if (to.lengthSquared() > 1.0E-6F) {
                this.shrug(pose, l, Shoulders.turn(l == 0, to.normalize(), stretch, new Quaternionf()));
                this.place(pose);
            }
        }
        Vector3f pivot = this.pivot(l, new Vector3f());
        Vector3f wrist = new Vector3f(way).normalize().mul(-last).add(tip);
        this.root[0] = pivot.x;
        this.root[1] = pivot.y;
        this.root[2] = pivot.z;
        this.target[0] = wrist.x;
        this.target[1] = wrist.y;
        this.target[2] = wrist.z;
        this.pole[0] = toward.x;
        this.pole[1] = toward.y;
        this.pole[2] = toward.z;
        Ik.twoBone(this.root, this.target, this.pole, upper, lower, this.middle);
        Vector3f a = new Vector3f((float) this.middle[0], (float) this.middle[1], (float) this.middle[2]).sub(pivot);
        Vector3f b = new Vector3f(wrist).sub((float) this.middle[0], (float) this.middle[1], (float) this.middle[2]);
        if (a.lengthSquared() < 1.0E-8F) {
            return;
        }
        a.normalize();
        b = b.lengthSquared() < 1.0E-8F ? new Vector3f(a) : b.normalize();
        // The joint's outer side faces the pole: an elbow's is its back (+z of the arm), a knee's its front (-z).
        Vector3f side = new Vector3f(toward).sub(new Vector3f(a).mul(toward.dot(a)));
        if (side.lengthSquared() < 1.0E-6F) {
            side.set(b).sub(new Vector3f(a).mul(b.dot(a))).negate();
        }
        if (side.lengthSquared() < 1.0E-6F) {
            side.set(0.0F, 0.0F, arm ? 1.0F : -1.0F);
        }
        side.normalize();
        Vector3f z = arm ? side : side.negate();
        Vector3f x = new Vector3f(a).cross(z).normalize();
        z.set(x).cross(a);
        // The limb's own axes with its bone along a (a person's limbs run down their own y).
        Quaternionf own = new Quaternionf().setFromNormalized(new Matrix3f(x, a, z));
        Vector3f bone = this.bone(l, new Vector3f());
        own.mul(new Quaternionf().rotationTo(bone, new Vector3f(0.0F, 1.0F, 0.0F)));
        // Into the pose's own terms: an arm's turn from its rest in its blade's axes, a leg's in the pelvis's.
        Matrix4f parent = new Matrix4f(this.limbs[l][0]).rotate(new Quaternionf(pose.limb[l]).conjugate());
        parent.getNormalizedRotation(this.turn);
        pose.limb[l].set(this.turn.conjugate()).mul(own);
        // Its elbow or knee and its wrist or ankle each bend only as far and as the joint can.
        ModelBends.Bend[] chain = this.chains[this.roles[2 + l]];
        float[] along = { 0.0F, 0.0F, 0.0F };
        along[chain[0].axis()] = chain[0].farSign();
        this.aim(own.transformInverse(new Vector3f(b)), along, chain[0], pose.mid[l]);
        Quaternionf lowerTurn = new Quaternionf(own).mul(pose.mid[l]);
        Vector3f hand = lowerTurn.transformInverse(new Vector3f(way).normalize());
        if (chain.length > 1) {
            this.aim(hand, along, chain[1], pose.end[l]);
        } else {
            pose.end[l].identity();
        }
        this.place(pose);
    }

    private void aim(Vector3f way, float[] bone, ModelBends.Bend bend, Quaternionf out) {
        Limits.aim(way, bone, bend.hinge(), (float) bend.min(), (float) bend.max(), (float) bend.lean(), out);
    }

    // As above, its last piece turned wholly `turn` (the trunk's parent's axes), twist and all, not just its way: as
    // far as its wrist or ankle goes.
    void reach(Pose pose, int l, Vector3f tip, Quaternionf turn, Vector3f toward) {
        this.reach(pose, l, tip, turn.transform(this.bone(l, new Vector3f())), toward);
        Quaternionf lower = this.limbs[l][1].getNormalizedRotation(new Quaternionf());
        pose.end[l].set(lower.conjugate()).mul(turn);
        ModelBends.Bend[] chain = this.chains[this.roles[2 + l]];
        if (chain.length > 1) {
            chain[1].keep(pose.end[l]);
        } else {
            pose.end[l].identity();
        }
        this.place(pose);
    }

    // Limb l's bone in its own axes: the way from its pivot to its far end.
    Vector3f bone(int l, Vector3f out) {
        ModelBends.Bend bend = this.chains[this.roles[2 + l]][0];
        return out.zero().setComponent(bend.axis(), bend.farSign());
    }

    // The pose a BodyPose (parts where they hang in the trunk's parent's frame, with their joints) puts the body in.
    void read(BodyPose from, Pose out) {
        int core = this.roles[BODY];
        this.chest.translation(from.pos[core]).rotate(from.rot[core]);
        out.waist.set(this.trunk.length > 0 ? from.joint[0][core] : this.turn.identity());
        out.low.set(this.trunk.length > 1 ? from.joint[1][core] : this.turn.identity());
        Matrix4f pelvis = new Matrix4f(this.chest);
        if (this.trunk.length > 0) {
            this.about(pelvis, this.trunk[0].knee(), out.waist, false);
        }
        if (this.trunk.length > 1) {
            this.about(pelvis, this.trunk[1].knee(), out.low, false);
        }
        pelvis.transformPosition(out.hips.set(this.hipsAt));
        pelvis.getNormalizedRotation(out.pelvis);
        Quaternionf chestTurn = this.chest.getNormalizedRotation(new Quaternionf());
        out.head.set(chestTurn).mul(this.restTurn[HEAD]).conjugate().mul(from.rot[this.roles[HEAD]]);
        for (int l = 0; l < LIMBS; l++) {
            int part = this.roles[2 + l];
            if (part < 0) {
                continue;
            }
            Quaternionf parent;
            if (l < 2) {
                Vector3f inner = this.blade[l];
                out.blade[l].identity();
                if (inner != null) {
                    // The blade turned as far as it takes the arm from where it hangs at rest to where it hangs now.
                    this.linked(part, from, this.v);
                    new Matrix4f(this.chest).invert().transformPosition(this.v).sub(inner);
                    this.w.set(this.hangAt[2 + l]).sub(inner);
                    out.blade[l].rotationTo(this.w, this.v);
                }
                parent = new Quaternionf(chestTurn).mul(out.blade[l]);
            } else {
                parent = pelvis.getNormalizedRotation(new Quaternionf());
            }
            parent.mul(this.restTurn[2 + l]);
            out.limb[l].set(parent).conjugate().mul(from.rot[part]);
            out.mid[l].set(this.chains[part].length > 0 ? from.joint[0][part] : this.turn.identity());
            out.end[l].set(this.chains[part].length > 1 ? from.joint[1][part] : this.turn.identity());
        }
    }

    // Where part i hangs in the trunk's parent's pixels, as a pose has it.
    private Vector3f linked(int i, BodyPose pose, Vector3f out) {
        return out.set(pose.pos[i]).mul(this.body.scale[i]).add(this.body.move[i]);
    }

    // The body as placed last (place()) and the joints of `pose`, into a BodyPose.
    void write(Pose pose, BodyPose out) {
        int core = this.roles[BODY];
        this.put(core, this.chest, out);
        out.joint[0][core].set(pose.waist);
        out.joint[1][core].set(pose.low);
        this.put(this.roles[HEAD], this.head, out);
        for (int l = 0; l < LIMBS; l++) {
            int part = this.roles[2 + l];
            if (part < 0) {
                continue;
            }
            this.put(part, this.limbs[l][0], out);
            out.joint[0][part].set(pose.mid[l]);
            out.joint[1][part].set(pose.end[l]);
        }
    }

    // Part i where `frame` puts it in the trunk's parent's frame, into its own parent's (a young head's is bigger).
    private void put(int i, Matrix4f frame, BodyPose out) {
        frame.transformPosition(out.pos[i].zero()).sub(this.body.move[i]).div(this.body.scale[i]);
        frame.getNormalizedRotation(out.rot[i]);
    }

    // Whether part `role` has its joint j (0 the elbow or knee, 1 the wrist or ankle; the trunk's waist and pelvis).
    boolean has(int role, int j) {
        return this.roles[role] >= 0 && j < this.chains[this.roles[role]].length;
    }

    // Whether limb l is there to move (an arm folded as one part is not: its chest carries it).
    boolean present(int l) {
        return this.roles[2 + l] >= 0;
    }

    @Nullable
    Vector3f blade(int side) {
        return this.blade[side];
    }
}
