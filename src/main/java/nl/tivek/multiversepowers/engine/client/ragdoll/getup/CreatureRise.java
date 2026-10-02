package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Anything but a person getting up, the way its kind does (GetUp.Kind). It turns upright while it still lies low (a
// four-legged one rolling onto its chest about its spine), its legs folding in under it, then unfolds them and pushes
// up into its own pose, its limbs coming under it a little behind its trunk: front legs first, sitting up like a dog
// with its nose high before its hind legs heave it up (a horse, a pig); hind legs first, kneeling on its front knees
// with its rump up before it steps up in front (cattle, sheep); both at once; or a bird flapping its wings, its feet
// scrambling under it. It stays on the ground all the way: what would sink into it or hang over it is moved onto it.
final class CreatureRise implements GetUp.Rise {
    // Its way of getting up: upright by `upright` of the way; rising onto its legs from riseFrom to riseTo; its legs
    // folding in at most TUCK more at the knee from FOLD_FROM over FOLD_OVER and unfolding again over `unfold`, its
    // front legs from frontFrom, its hind legs from hindFrom; the trunk pitched `pitch` (radians, nose up; below 0 the
    // nose goes down, the hind end being up first) from pitchFrom to pitchTo; its wings flapping `flaps` times until
    // `flapTo`.
    private record Style(float upright, float riseFrom, float riseTo, float frontFrom, float hindFrom, float unfold,
            float pitch, float pitchFrom, float pitchTo, float flaps, float flapTo) {
    }

    private static final Style EVEN = new Style(0.5F, 0.25F, 0.9F, 0.42F, 0.49F, 0.38F, 0.0F, 0.0F, 1.0F, 0.0F,
            0.0F);
    private static final Style FRONT_FIRST = new Style(0.38F, 0.36F, 0.9F, 0.38F, 0.6F, 0.24F, 0.42F, 0.38F, 0.86F,
            0.0F, 0.0F);
    private static final Style HIND_FIRST = new Style(0.38F, 0.36F, 0.9F, 0.62F, 0.38F, 0.24F, -0.3F, 0.38F, 0.88F,
            0.0F, 0.0F);
    private static final Style BIRD = new Style(0.36F, 0.3F, 0.86F, 0.42F, 0.42F, 0.34F, 0.0F, 0.0F, 1.0F, 3.0F,
            0.62F);
    // Its limbs come under it from LIMBS_FROM until a little after their legs unfold.
    private static final float LIMBS_FROM = 0.1F;
    private static final float LIMBS_PAST = 0.04F;
    private static final float TUCK = 1.2F;
    private static final float FOLD_FROM = 0.05F;
    private static final float FOLD_OVER = 0.3F;
    // How much of the ankle's fold the knee's undoes, keeping a foot under the leg.
    private static final float ANKLE_BACK = 0.5F;
    // It is kept on the ground from GROUND_IN on and let go of it again over the last GROUND_IN, so it starts just as
    // it lay and ends just in its own pose.
    private static final float GROUND_IN = 0.1F;
    // How far a wing flaps up and down (radians); the pitch comes and goes over this share of its time each.
    private static final float FLAP = 0.9F;
    private static final float PITCH_RAMP = 0.45F;
    // It has rolled over onto its belly by this share of the time it takes to come upright.
    private static final float ROLL_SHARE = 0.85F;
    private static final Quaternionf FOLD = new Quaternionf();

    private final Hanging body;
    private final Style style;
    // Each part's knee, where it is a leg that has one (null otherwise); its ankle, where it has one; whether it is a
    // hind leg, a wing (+1 the right, -1 the left, 0 none); each part's pieces' boxes (pixels, its own frame) and its
    // joints; which way is ahead along the model's z.
    private final ModelBends.Bend[] knees;
    private final ModelBends.Bend[] ankles;
    private final boolean[] hind;
    private final float[] wing;
    private final float[][][] pieces;
    private final ModelBends.Bend[][] chains;
    private final float[] limbs;
    private final float ahead;
    // A body lying along the ground: its spine and its belly's way in its trunk's own axes, and how far it rolls
    // about its spine to bring its belly down (planned the first time), about which way.
    private final boolean along;
    private final Vector3f spine = new Vector3f();
    private final Vector3f belly = new Vector3f();
    private final Vector3f rollAxis = new Vector3f();
    private float roll = Float.NaN;
    private final BodyPose rolled = new BodyPose();
    private final Matrix4f frame = new Matrix4f();
    private final Matrix4f trunk = new Matrix4f();
    private final Vector3f v = new Vector3f();
    private final Quaternionf turn = new Quaternionf();

    CreatureRise(Hanging body, List<ModelParts.Part> parts, ModelBends.Bend[][] chains, GetUp.Kind kind) {
        this.body = body;
        this.chains = chains;
        this.style = kind == GetUp.Kind.FRONT_FIRST ? FRONT_FIRST : kind == GetUp.Kind.HIND_FIRST ? HIND_FIRST
                : kind == GetUp.Kind.BIRD ? BIRD : EVEN;
        int n = body.n;
        this.knees = new ModelBends.Bend[n];
        this.ankles = new ModelBends.Bend[n];
        this.hind = new boolean[n];
        this.wing = new float[n];
        this.pieces = new float[n][][];
        this.limbs = new float[n];
        // Its front is the way its head is from its trunk as its model was built (else -z, ahead).
        Matrix4f rest = new Matrix4f();
        float coreZ = ModelParts.rest(parts.get(body.core), rest).m32();
        float front = -1.0F;
        boolean named = false;
        for (int i = 0; i < n; i++) {
            if (i != body.core && parts.get(i).role() == ModelParts.Role.HEAD) {
                front = Math.signum(ModelParts.rest(parts.get(i), rest).m32() - coreZ);
            }
            named |= ModelBends.hind(parts.get(i)) || ModelBends.front(parts.get(i));
        }
        this.ahead = front == 0.0F ? -1.0F : front;
        this.along = GetUp.lying(parts, body.core);
        Quaternionf back = ModelParts.rest(parts.get(body.core), rest).getNormalizedRotation(new Quaternionf())
                .conjugate();
        back.transform(this.spine.set(0.0F, 0.0F, this.ahead));
        back.transform(this.belly.set(0.0F, 1.0F, 0.0F));
        for (int i = 0; i < n; i++) {
            ModelBends.Bend[] chain = chains[i];
            ModelParts.Part part = parts.get(i);
            this.pieces[i] = pieces(part.bounds(), chain);
            boolean leg = i != body.core && part.role() == ModelParts.Role.LEG;
            this.knees[i] = leg && chain.length > 0 ? chain[0] : null;
            this.ankles[i] = leg && chain.length > 1 ? chain[1] : null;
            // Its hind legs by their names (a cat's are its back legs), else by where they hang.
            this.hind[i] = leg && (named ? ModelBends.hind(part)
                    : (ModelParts.rest(part, rest).m32() - coreZ) * this.ahead < 0.0F);
            if (i != body.core && part.name().contains("wing")) {
                this.wing[i] = part.name().contains("left") ? -1.0F : 1.0F;
            }
        }
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

    @Override
    public void pose(float u, BodyPose lie, BodyPose own, BodyPose out) {
        Style s = this.style;
        float upright = (float) Ease.smoother(u / s.upright());
        float rise = (float) Ease.smoother((u - s.riseFrom()) / (s.riseTo() - s.riseFrom()));
        for (int i = 0; i < this.body.n; i++) {
            float under = (this.hind[i] ? s.hindFrom() : s.frontFrom()) + s.unfold() + LIMBS_PAST;
            this.limbs[i] = (float) Ease.smoother((u - LIMBS_FROM) / (under - LIMBS_FROM));
        }
        PoseBlend.blend(this.body, this.rolled(u, lie), own, upright, rise, this.limbs, out);
        for (int i = 0; i < this.body.n; i++) {
            ModelBends.Bend knee = this.knees[i];
            float from = this.hind[i] ? s.hindFrom() : s.frontFrom();
            float tuck = TUCK * (float) (Ease.smoother((u - FOLD_FROM) / FOLD_OVER)
                    - Ease.smoother((u - from) / s.unfold()));
            if (knee == null || tuck < 1.0E-3F) {
                continue;
            }
            float[] h = knee.hinge();
            float fold = Math.min(tuck, (float) knee.max());
            knee.keep(out.joint[0][i].premul(FOLD.setAngleAxis(fold, h[0], h[1], h[2])));
            ModelBends.Bend ankle = this.ankles[i];
            if (ankle != null) {
                float back = Math.max(-fold * ANKLE_BACK, (float) ankle.min());
                ankle.keep(out.joint[1][i].premul(FOLD.setAngleAxis(back, h[0], h[1], h[2])));
            }
        }
        // One end up before the other: the whole body pitched about its trunk.
        float span = (s.pitchTo() - s.pitchFrom()) * PITCH_RAMP;
        float pitch = s.pitch() * (float) (Ease.smoother((u - s.pitchFrom()) / span)
                - Ease.smoother((u - s.pitchTo() + span) / span));
        if (Math.abs(pitch) > 1.0E-4F) {
            this.pitch(out, pitch * this.ahead);
        }
        if (s.flaps() > 0.0F && u < s.flapTo()) {
            float beat = (float) Math.sin(u / s.flapTo() * s.flaps() * 2.0 * Math.PI);
            float strength = FLAP * (float) (1.0 - Ease.smoother((u - s.flapTo() * 0.6F) / (s.flapTo() * 0.4F)));
            for (int i = 0; i < this.body.n; i++) {
                if (this.wing[i] != 0.0F) {
                    out.rot[i].premul(this.turn.rotationZ(this.wing[i] * beat * strength));
                }
            }
        }
        float hold = (float) (Ease.smoother(u / GROUND_IN) * (1.0 - Ease.smoother((u - 1.0F + GROUND_IN) / GROUND_IN)));
        if (hold > 0.0F) {
            float sink = (this.lowest(out) - this.lowest(own)) * hold;
            for (int i = 0; i < this.body.n; i++) {
                if (!this.body.inside[i]) {
                    out.pos[i].y -= i == this.body.core ? sink : sink / this.body.scale[i];
                }
            }
        }
    }

    // How it lay, rolled as one about its spine by as much of the way to its belly down as it has rolled at `u`: a
    // body lying along the ground rolls over that way first (from its back that is half round, which a plain turn to
    // its own pose could take any way, up on end as well).
    private BodyPose rolled(float u, BodyPose lie) {
        int core = this.body.core;
        if (!this.along) {
            return lie;
        }
        if (Float.isNaN(this.roll)) {
            Quaternionf lay = lie.rot[core];
            lay.transform(this.rollAxis.set(this.spine)).normalize();
            Vector3f down = lay.transform(new Vector3f(this.belly));
            Vector3f want = new Vector3f(0.0F, 1.0F, 0.0F);
            down.sub(new Vector3f(this.rollAxis).mul(down.dot(this.rollAxis)));
            want.sub(new Vector3f(this.rollAxis).mul(want.dot(this.rollAxis)));
            this.roll = (float) Math.atan2(this.rollAxis.dot(new Vector3f(down).cross(want)), down.dot(want));
        }
        this.rolled.set(lie, this.body.n);
        float share = (float) Ease.smoother(u / (this.style.upright() * ROLL_SHARE));
        this.turn(this.rolled, this.turn.setAngleAxis(this.roll * share, this.rollAxis.x, this.rollAxis.y,
                this.rollAxis.z));
        return this.rolled;
    }

    // Turns the whole pose about the trunk's place by `angle` about the model's x axis (its left), as one: nose up for
    // a body facing -z when the angle is below 0.
    private void pitch(BodyPose pose, float angle) {
        this.turn(pose, this.turn.rotationX(angle));
    }

    // Turns the whole pose about the trunk's place by `by` (the trunk's parent's axes), as one.
    private void turn(BodyPose pose, Quaternionf by) {
        Hanging body = this.body;
        int core = body.core;
        Vector3f pivot = new Vector3f(pose.pos[core]);
        pose.rot[core].premul(by);
        for (int i = 0; i < body.n; i++) {
            if (i == core || body.inside[i]) {
                continue;
            }
            this.v.set(pose.pos[i]).mul(body.scale[i]).add(body.move[i]).sub(pivot);
            by.transform(this.v).add(pivot).sub(body.move[i]).div(body.scale[i]);
            pose.pos[i].set(this.v);
            pose.rot[i].premul(by);
        }
    }

    // The lowest point (the largest y: y runs down) of a pose, every piece's corners, in the trunk's parent's pixels.
    private float lowest(BodyPose pose) {
        Hanging body = this.body;
        int core = body.core;
        this.trunk.translation(pose.pos[core]).rotate(pose.rot[core]).scale(body.trunk);
        float low = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < body.n; i++) {
            if (i == core) {
                this.frame.set(this.trunk);
            } else if (body.inside[i]) {
                this.frame.set(this.trunk).translate(pose.pos[i]).rotate(pose.rot[i]);
            } else {
                Vector3f at = this.v.set(pose.pos[i]).mul(body.scale[i]).add(body.move[i]);
                this.frame.translation(at).rotate(pose.rot[i]).scale(body.scale[i]);
            }
            float[][] boxes = this.pieces[i];
            ModelBends.Bend[] chain = this.chains[i];
            for (int k = 0; k < boxes.length; k++) {
                if (k > 0) {
                    float[] knee = chain[k - 1].knee();
                    this.frame.translate(knee[0], knee[1], knee[2]).rotate(pose.joint[k - 1][i])
                            .translate(-knee[0], -knee[1], -knee[2]);
                }
                float[] b = boxes[k];
                for (int c = 0; c < 8; c++) {
                    this.frame.transformPosition(this.v.set((c & 1) == 0 ? b[0] : b[3], (c & 2) == 0 ? b[1] : b[4],
                            (c & 4) == 0 ? b[2] : b[5]));
                    low = Math.max(low, this.v.y);
                }
            }
        }
        return low;
    }
}
