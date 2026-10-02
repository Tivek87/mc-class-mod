package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import javax.annotation.Nullable;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person getting up as a person does, from how it lies. Face down it props itself on its forearms, pushes up onto its
// hands and knees, steps one foot through and kneels, then stands; face up it curls up to sit, turns onto a hand and a
// knee, kneels and stands (on its side, whichever it lies nearer; sitting up, it starts from curling up). Its hands,
// knees and feet stay where they are put on the ground while its weight moves over them, and step when they move. Every
// joint moves on one smooth curve through the moments below, so it never stops dead between them, its head a moment
// behind its trunk. It fades in from just how it lay and out into its own pose; the moments are laid out along its way
// from where it lay to where it stands.
final class PersonRise implements GetUp.Rise {
    // The ends that rest on the ground at a moment.
    private static final int HAND_R = 1;
    private static final int HAND_L = 2;
    private static final int KNEE_R = 4;
    private static final int KNEE_L = 8;
    private static final int FOOT_R = 16;
    private static final int FOOT_L = 32;

    // A moment of getting up, for a body that steps through with its right foot (and posts its left hand, face up);
    // mirrored for the other side: when (u); the pelvis's turn (yaw, pitch ahead: 1.57 face down, -1.57 face up, roll);
    // where its hips are from its way to where it stands (sideways, ahead; how high comes from the ground); the waist's
    // and low back's fold (ahead +); the head's nod (down +) and turn; the shoulder blades' shrug (up +) and roll
    // (ahead +), right then left; each limb's turn as a model part turns (x, y, z) and its middle and end joints' folds
    // (an elbow ahead, a knee back, a wrist ahead, an ankle pointing), right arm, left arm, right leg, left leg; and
    // the ends resting on the ground.
    private record Key(float u, float yaw, float pitch, float roll, float side, float ahead, float waist, float low,
            float nod, float shake, float[] blades, float[][] limbs, int rests) {
        // The same moment, at another time.
        Key at(float when) {
            return new Key(when, this.yaw, this.pitch, this.roll, this.side, this.ahead, this.waist, this.low, this.nod,
                    this.shake, this.blades, this.limbs, this.rests);
        }
    }

    private static final float PI = (float) Math.PI;
    private static final Key[] FRONT = {
            // Up on its forearms, head raised.
            new Key(0.14F, 0.0F, PI / 2.0F, 0.0F, 0.0F, 0.0F, -0.25F, -0.15F, -0.55F, 0.0F,
                    new float[] { 0.1F, 0.05F, 0.1F, 0.05F },
                    new float[][] { { -1.5F, 0.0F, 0.35F, 1.45F, 0.1F }, { -1.5F, 0.0F, -0.35F, 1.45F, 0.1F },
                            { 0.02F, 0.0F, 0.06F, 0.15F, 0.05F }, { 0.02F, 0.0F, -0.06F, 0.1F, 0.05F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // Pushing its chest up on its hands.
            new Key(0.3F, 0.0F, 1.3F, 0.0F, 0.0F, 1.0F, -0.4F, -0.25F, -0.45F, 0.0F,
                    new float[] { 0.2F, 0.1F, 0.2F, 0.1F },
                    new float[][] { { -1.15F, 0.0F, 0.3F, 0.5F, -0.9F }, { -1.15F, 0.0F, -0.3F, 0.5F, -0.9F },
                            { 0.05F, 0.0F, 0.06F, 0.2F, 0.05F }, { 0.05F, 0.0F, -0.06F, 0.2F, 0.05F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // On its hands and knees.
            new Key(0.44F, 0.0F, 1.35F, 0.0F, 0.0F, -3.0F, 0.1F, 0.1F, -0.2F, 0.0F,
                    new float[] { 0.05F, 0.15F, 0.05F, 0.15F },
                    new float[][] { { -1.45F, 0.0F, 0.12F, 0.08F, -1.3F }, { -1.45F, 0.0F, -0.12F, 0.08F, -1.3F },
                            { -1.35F, 0.0F, 0.05F, 1.57F, 0.0F }, { -1.35F, 0.0F, -0.05F, 1.57F, 0.0F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // Its right foot stepped through between its hands.
            new Key(0.56F, 0.0F, 0.9F, 0.0F, 0.0F, 0.0F, 0.2F, 0.1F, -0.15F, 0.0F,
                    new float[] { 0.1F, 0.1F, 0.1F, 0.1F },
                    new float[][] { { -1.1F, 0.0F, 0.1F, 0.25F, -0.9F }, { -1.1F, 0.0F, -0.1F, 0.25F, -0.9F },
                            { -2.25F, 0.0F, 0.1F, 2.0F, -0.4F }, { -0.9F, 0.0F, -0.05F, 1.57F, 0.0F } },
                    HAND_R | HAND_L | KNEE_L | FOOT_R),
            // Kneeling upright on its left knee.
            new Key(0.68F, 0.0F, 0.12F, 0.0F, 0.0F, 0.0F, 0.05F, 0.05F, 0.05F, 0.0F,
                    new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { -0.45F, 0.0F, 0.1F, 0.5F, 0.0F }, { 0.15F, 0.0F, -0.1F, 0.3F, 0.0F },
                            { -1.6F, 0.0F, 0.1F, 1.6F, -0.05F }, { -0.1F, 0.0F, -0.05F, 1.6F, 0.0F } },
                    KNEE_L | FOOT_R),
            // Rising over its right foot, its left on its toes.
            new Key(0.8F, 0.0F, 0.35F, 0.0F, 0.0F, 2.5F, 0.08F, 0.05F, -0.05F, 0.0F,
                    new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { 0.35F, 0.0F, 0.1F, 0.4F, 0.0F }, { -0.45F, 0.0F, -0.1F, 0.4F, 0.0F },
                            { -0.75F, 0.0F, 0.05F, 0.95F, -0.2F }, { 0.45F, 0.0F, -0.05F, 0.7F, 0.65F } },
                    FOOT_R),
            // Standing, its left foot brought up beside its right.
            new Key(0.92F, 0.0F, 0.02F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { 0.08F, 0.0F, 0.08F, 0.15F, 0.0F }, { -0.05F, 0.0F, -0.08F, 0.15F, 0.0F },
                            { 0.0F, 0.0F, 0.0F, 0.04F, 0.0F }, { 0.0F, 0.0F, 0.0F, 0.04F, 0.0F } },
                    FOOT_R | FOOT_L) };
    private static final Key[] BACK = {
            // On its back, knees drawn up, chin tucked.
            new Key(0.12F, 0.0F, -PI / 2.0F, 0.0F, 0.0F, 0.0F, 0.2F, 0.1F, 0.5F, 0.0F,
                    new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { 0.25F, 0.0F, 0.25F, 0.3F, 0.0F }, { 0.25F, 0.0F, -0.25F, 0.3F, 0.0F },
                            { -1.1F, 0.0F, 0.05F, 2.0F, -0.35F }, { -1.1F, 0.0F, -0.05F, 2.0F, -0.35F } },
                    HAND_R | HAND_L | FOOT_R | FOOT_L),
            // Curled up to sit, its left hand reaching back for the ground.
            new Key(0.28F, 0.0F, -0.85F, 0.0F, 0.0F, -1.0F, 0.5F, 0.3F, 0.3F, 0.0F,
                    new float[] { 0.1F, 0.1F, 0.0F, -0.1F },
                    new float[][] { { -1.2F, 0.0F, 0.1F, 0.3F, 0.0F }, { 0.55F, 0.0F, -0.45F, 0.2F, -0.2F },
                            { -1.1F, 0.0F, 0.05F, 2.0F, -0.35F }, { -1.1F, 0.0F, -0.05F, 2.0F, -0.35F } },
                    FOOT_R | FOOT_L),
            // Turned onto its left hand and knee, its right foot planted ahead.
            new Key(0.44F, -0.35F, 0.45F, 0.2F, 1.0F, -1.0F, 0.25F, 0.15F, 0.1F, 0.0F,
                    new float[] { 0.0F, 0.1F, 0.2F, 0.0F },
                    new float[][] { { -0.6F, 0.0F, 0.1F, 0.6F, 0.0F }, { -0.25F, 0.0F, -0.45F, 0.1F, -1.1F },
                            { -1.3F, 0.0F, 0.05F, 1.6F, -0.2F }, { -0.45F, 0.0F, -0.1F, 2.0F, 0.0F } },
                    HAND_L | KNEE_L | FOOT_R),
            FRONT[4].at(0.58F), FRONT[5].at(0.74F), FRONT[6].at(0.88F) };
    // Sitting up (slumped against a wall): it curls up from there, as face up once it has sat up.
    private static final Key[] SEATED = { BACK[1].at(0.16F), BACK[2].at(0.36F), FRONT[4].at(0.54F),
            FRONT[5].at(0.72F), FRONT[6].at(0.88F) };
    // Its pelvis this near upright (the cosine to straight up), it sits.
    private static final float SITTING = 0.6F;

    // The pose fades in from how it lay over this much of getting up, and into its own pose from OWN_FROM on; its head
    // follows its trunk this far behind; it moves along its way to where it stands from DRIFT_FROM to DRIFT_TO.
    private static final float LIE_FADE = 0.06F;
    private static final float OWN_FROM = 0.84F;
    private static final float HEAD_LAG = 0.035F;
    private static final float DRIFT_FROM = 0.1F;
    private static final float DRIFT_TO = 0.9F;
    // A resting end lifting off is held over this first share of the time before the next moment; one landing is set
    // down over this last share; one moving on the ground is lifted this high (pixels) halfway, a knee sliding less.
    private static final float LIFT = 0.3F;
    private static final float LAND = 0.35F;
    private static final float STEP = 2.5F;
    private static final float SLIDE = 0.4F;
    // A resting end this near (pixels) where it rested the moment before stays there.
    private static final float STAY = 5.0F;
    // An end this near the ground (pixels) as the body lies rests on it.
    private static final float ON_GROUND = 1.5F;
    // Leaning on a weapon: at the moments its hips are lower than this share of standing and its trunk is up (the
    // cosine to straight up), its tip planted this far (pixels) ahead of its shoulder and out to the side.
    private static final float LOW = 0.95F;
    private static final float UP = 0.5F;
    private static final float BRACE_AHEAD = 7.0F;
    private static final float BRACE_OUT = 1.5F;

    private final Skeleton body;
    // The weapon it leans on getting up (null without one), at which moments, and where its tip rests then.
    @Nullable
    private final Brace brace;
    private final boolean[] braced;
    private final Vector3f[] braceAt;
    private final float[] times;
    private final Skeleton.Pose[] keys;
    // Each moment's turns (Pose.all), and the pose's own.
    private final Quaternionf[][] turns;
    private final int[] rests;
    // Where each resting end rests at each moment, [moment][limb].
    private final Vector3f[][] contacts;
    private final Skeleton.Pose pose = new Skeleton.Pose();
    private final Quaternionf[] poseTurns = this.pose.all();
    private final Skeleton.Pose reached = new Skeleton.Pose();
    private final float[] p = new float[4];
    private final float[] result = new float[4];
    private final BodyPose track = new BodyPose();
    private final BodyPose faded = new BodyPose();
    private final Vector3f a = new Vector3f();
    private final Vector3f b = new Vector3f();
    private final Vector3f c = new Vector3f();
    private final Quaternionf q = new Quaternionf();

    PersonRise(Skeleton body, BodyPose lie, @Nullable Brace brace) {
        this.body = body;
        this.brace = brace;
        Skeleton.Pose lying = new Skeleton.Pose();
        body.read(lie, lying);
        body.place(lying);
        // Face down its pelvis's front (-z) points at the ground (+y: y runs down); sitting, its trunk (-y) points up.
        boolean sitting = lying.pelvis.transform(this.a.set(0.0F, -1.0F, 0.0F)).y < -SITTING;
        boolean front = !sitting && lying.pelvis.transform(this.a.set(0.0F, 0.0F, -1.0F)).y > 0.0F;
        Key[] plan = sitting ? SEATED : front ? FRONT : BACK;
        // Face down it steps through with the leg whose knee lies further ahead; face up it turns onto the side it
        // lies nearer: its front turned to its left, it lies on its right.
        boolean mirror = front ? body.joint(3, this.a).z < body.joint(2, this.b).z
                : lying.pelvis.transform(this.a.set(0.0F, 0.0F, -1.0F)).x > 0.0F;
        int n = plan.length + 1;
        this.times = new float[n];
        this.keys = new Skeleton.Pose[n];
        this.rests = new int[n];
        this.contacts = new Vector3f[n][Skeleton.LIMBS];
        this.braced = new boolean[n];
        this.braceAt = new Vector3f[n];
        this.keys[0] = lying;
        this.rests[0] = this.resting(lying);
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            this.contacts[0][l] = this.end(l, this.rests[0], new Vector3f());
        }
        Vector3f anchor = new Vector3f(lying.hips);
        Skeleton.Pose rest = new Skeleton.Pose();
        this.times[0] = 0.0F;
        for (int k = 1; k < n; k++) {
            Key key = mirror ? mirrored(plan[k - 1]) : plan[k - 1];
            this.times[k] = key.u();
            Skeleton.Pose made = this.made(key, rest);
            float drift = drift(key.u());
            made.hips.x = anchor.x + (body.stands.x - anchor.x) * drift + key.side();
            made.hips.z = anchor.z + (body.stands.z - anchor.z) * drift - key.ahead();
            made.hips.y = anchor.y;
            body.place(made);
            made.hips.y += body.ground - body.lowest();
            body.place(made);
            this.rests[k] = key.rests();
            this.plant(made, k);
            this.lean(made, k);
            this.keys[k] = made;
        }
        align(this.keys);
        this.turns = new Quaternionf[n][];
        for (int k = 0; k < n; k++) {
            this.turns[k] = this.keys[k].all();
        }
    }

    // The ends that touch the ground as the body lies: a fingertip or a sole, else a knee.
    private int resting(Skeleton.Pose lying) {
        int rests = 0;
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            if (this.body.tip(l, this.a).y >= this.body.ground - ON_GROUND) {
                rests |= l < 2 ? 1 << l : l == 2 ? FOOT_R : FOOT_L;
            } else if (l >= 2 && this.body.joint(l, this.a).y >= this.body.ground - this.body.half[l] - ON_GROUND) {
                rests |= l == 2 ? KNEE_R : KNEE_L;
            }
        }
        return rests;
    }

    // Limb l's resting point as the body was placed last: its knee's middle when it kneels, else its far end.
    private Vector3f end(int l, int rests, Vector3f out) {
        return kneels(l, rests) ? this.body.joint(l, out) : this.body.tip(l, out);
    }

    private static boolean kneels(int l, int rests) {
        return l == 2 && (rests & KNEE_R) != 0 || l == 3 && (rests & KNEE_L) != 0;
    }

    private static boolean rests(int l, int rests) {
        int bits = l == 0 ? HAND_R : l == 1 ? HAND_L : l == 2 ? KNEE_R | FOOT_R : KNEE_L | FOOT_L;
        return (rests & bits) != 0;
    }

    // Moment k's resting ends put on the ground: where they rested the moment before when they have not gone far,
    // else where the moment puts them, set down on the ground; each limb reaching there.
    private void plant(Skeleton.Pose made, int k) {
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            if (!rests(l, this.rests[k])) {
                this.contacts[k][l] = this.end(l, this.rests[k], new Vector3f());
                continue;
            }
            boolean knee = kneels(l, this.rests[k]);
            Vector3f at = this.end(l, this.rests[k], new Vector3f());
            at.y = knee ? this.body.ground - this.body.half[l] : this.body.ground;
            Vector3f before = this.contacts[k - 1][l];
            if (rests(l, this.rests[k - 1]) && knee == kneels(l, this.rests[k - 1]) && before.distance(at) < STAY) {
                at.set(before);
            }
            this.contacts[k][l] = at;
            this.reach(made, l, at, knee);
        }
    }

    // Limb l of a placed pose reaching its resting point `at` (its knee there, when it kneels), bent the way it is.
    private void reach(Skeleton.Pose pose, int l, Vector3f at, boolean knee) {
        Vector3f tip = this.body.tip(l, new Vector3f());
        Vector3f way = this.body.along(l, 2, new Vector3f());
        Vector3f joint = this.body.joint(l, new Vector3f());
        Vector3f pivot = this.body.pivot(l, new Vector3f());
        // The elbow or knee goes out the way it is bent now: from the line between its ends towards where it is.
        Vector3f toward = new Vector3f(joint).sub(pivot.add(tip).mul(0.5F));
        if (toward.lengthSquared() < 0.01F) {
            toward.set(l < 2 ? 0.0F : 0.0F, l < 2 ? 0.0F : 1.0F, l < 2 ? 1.0F : -1.0F);
        }
        if (knee) {
            // Kneeling, its shin and foot keep their shape from the knee, which goes down to the ground.
            tip.sub(joint).add(at);
            toward.add(0.0F, 2.0F, 0.0F);
        } else {
            tip.set(at);
        }
        this.body.reach(pose, l, tip, way, toward);
    }

    // Moment k with a weapon in hand: low, up on its knees or feet and that hand free, it leans on the weapon, its tip
    // planted where it was the moment before when that is near, else ahead of its shoulder.
    private void lean(Skeleton.Pose made, int k) {
        Brace brace = this.brace;
        int feet = KNEE_R | KNEE_L | FOOT_R | FOOT_L;
        if (brace == null || rests(brace.limb, this.rests[k]) || (this.rests[k] & feet) == 0
                || this.body.ground - made.hips.y > LOW * this.body.legLength
                || made.pelvis.transform(this.a.set(0.0F, -1.0F, 0.0F)).y > -UP) {
            return;
        }
        Vector3f ahead = ahead(made, new Vector3f());
        float side = brace.limb == 0 ? -1.0F : 1.0F;
        Vector3f at = this.body.pivot(brace.limb, new Vector3f()).add(ahead.x * BRACE_AHEAD, 0.0F,
                ahead.z * BRACE_AHEAD).add(-ahead.z * side * BRACE_OUT, 0.0F, ahead.x * side * BRACE_OUT);
        at.y = this.body.ground;
        if (this.braced[k - 1] && this.braceAt[k - 1].distance(at) < STAY) {
            at.set(this.braceAt[k - 1]);
        }
        this.braced[k] = true;
        this.braceAt[k] = at;
        brace.hold(this.body, made, at, ahead, elbow(ahead, side, new Vector3f()));
    }

    // The way a placed pose faces, level: its pelvis's front.
    private static Vector3f ahead(Skeleton.Pose pose, Vector3f out) {
        pose.pelvis.transform(out.set(0.0F, 0.0F, -1.0F));
        out.y = 0.0F;
        return out.lengthSquared() < 1.0E-6F ? out.set(0.0F, 0.0F, -1.0F) : out.normalize();
    }

    // An elbow leaning on a weapon goes out to its side (`side` -1 the right, +1 the left) and back.
    private static Vector3f elbow(Vector3f ahead, float side, Vector3f out) {
        return out.set(-ahead.z * side - ahead.x * 0.5F, 0.0F, ahead.x * side - ahead.z * 0.5F);
    }

    // The pose of a moment from its numbers: every turn, the hips where the caller puts them.
    private Skeleton.Pose made(Key key, Skeleton.Pose rest) {
        Skeleton.Pose made = new Skeleton.Pose().set(rest);
        made.pelvis.rotationY(key.yaw()).rotateX(key.pitch()).rotateZ(key.roll());
        fold(made.waist, this.body.hinge(Skeleton.BODY, 0), key.waist());
        fold(made.low, this.body.hinge(Skeleton.BODY, 1), key.low());
        made.head.rotationZYX(0.0F, key.shake(), key.nod());
        for (int s = 0; s < 2; s++) {
            made.blade[s].identity();
            Vector3f way = this.body.bladeWay(s, this.a);
            if (way != null) {
                // Up the trunk is -y and ahead -z, in its own axes.
                Vector3f lift = new Vector3f(way).cross(0.0F, -1.0F, 0.0F).normalize();
                Vector3f roll = new Vector3f(way).cross(0.0F, 0.0F, -1.0F).normalize();
                made.blade[s].rotationAxis(key.blades()[s * 2 + 1], roll).rotateAxis(key.blades()[s * 2], lift);
            }
        }
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            float[] limb = key.limbs()[l];
            made.limb[l].rotationZYX(limb[2], limb[1], limb[0]);
            fold(made.mid[l], this.body.hinge(2 + l, 0), limb[3]);
            fold(made.end[l], this.body.hinge(2 + l, 1), limb[4]);
        }
        return made;
    }

    private static void fold(Quaternionf out, float[] hinge, float angle) {
        if (hinge == null) {
            out.identity();
        } else {
            out.setAngleAxis(angle, hinge[0], hinge[1], hinge[2]);
        }
    }

    // The same moment for a body stepping through with its left foot: left and right changed over.
    private static Key mirrored(Key key) {
        float[] blades = key.blades();
        float[][] limbs = key.limbs();
        float[][] swapped = new float[4][];
        for (int l = 0; l < 4; l++) {
            float[] from = limbs[l ^ 1];
            swapped[l] = new float[] { from[0], -from[1], -from[2], from[3], from[4] };
        }
        int rests = key.rests();
        int flipped = 0;
        for (int bit = 0; bit < 6; bit += 2) {
            flipped |= (rests >> bit & 1) << bit + 1 | (rests >> bit + 1 & 1) << bit;
        }
        return new Key(key.u(), -key.yaw(), key.pitch(), -key.roll(), -key.side(), key.ahead(), key.waist(),
                key.low(), key.nod(), -key.shake(), new float[] { blades[2], blades[3], blades[0], blades[1] }, swapped,
                flipped);
    }

    // Each turn kept on the side of the one before it, so the curve through them goes the short way round.
    private static void align(Skeleton.Pose[] keys) {
        for (int k = 1; k < keys.length; k++) {
            Quaternionf[] now = keys[k].all();
            Quaternionf[] before = keys[k - 1].all();
            for (int i = 0; i < now.length; i++) {
                if (now[i].dot(before[i]) < 0.0F) {
                    now[i].set(-now[i].x, -now[i].y, -now[i].z, -now[i].w);
                }
            }
        }
    }

    private static float drift(float u) {
        return (float) Ease.smoother((u - DRIFT_FROM) / (DRIFT_TO - DRIFT_FROM));
    }

    @Override
    public void pose(float u, float turned, BodyPose lie, BodyPose own, BodyPose out) {
        this.at(u);
        this.body.write(this.pose, this.track);
        this.turn(this.track, -turned);
        float toOwn = (float) Ease.smoother((u - OWN_FROM) / (1.0F - OWN_FROM));
        PoseBlend.blend(this.body.body, this.track, own, toOwn, toOwn, this.faded);
        float fromLie = 1.0F - (float) Ease.smoother(u / LIE_FADE);
        PoseBlend.blend(this.body.body, this.faded, lie, fromLie, fromLie, out);
    }

    // The body `u` of the way up, placed: every joint on its curve through the moments, the body kept off the ground,
    // and its resting ends where they rest.
    void at(float u) {
        int k = segment(this.times, u);
        float s = (u - this.times[k]) / (this.times[k + 1] - this.times[k]);
        this.curve(k, u);
        // The head a moment behind its trunk.
        float late = Math.max(0.0F, u - HEAD_LAG);
        this.curveTurn(segment(this.times, late), late, 3, this.pose.head);
        this.body.place(this.pose);
        float low = this.body.lowest();
        if (low > this.body.ground) {
            this.pose.hips.y -= low - this.body.ground;
            this.body.place(this.pose);
        }
        this.reached.set(this.pose);
        float[] weights = new float[Skeleton.LIMBS];
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            if (this.brace != null && l == this.brace.limb && (this.braced[k] || this.braced[k + 1])) {
                float lean = this.leaning(k, s);
                if (lean > 0.0F) {
                    weights[l] = lean;
                    continue;
                }
            }
            boolean before = rests(l, this.rests[k]);
            boolean after = rests(l, this.rests[k + 1]);
            boolean same = before && after && kneels(l, this.rests[k]) == kneels(l, this.rests[k + 1]);
            Vector3f from = this.contacts[k][l];
            Vector3f to = this.contacts[k + 1][l];
            float w;
            boolean knee;
            if (same) {
                w = 1.0F;
                knee = kneels(l, this.rests[k]);
                float e = (float) Ease.smoother(s);
                this.c.set(from).lerp(to, e);
                float far = from.distance(to);
                if (far > 0.5F) {
                    this.c.y -= (knee ? SLIDE : Math.min(STEP, far * 0.4F)) * (float) Math.sin(Math.PI * s);
                }
            } else if (before && s < LIFT) {
                w = 1.0F - (float) Ease.smoother(s / LIFT);
                knee = kneels(l, this.rests[k]);
                this.c.set(from);
            } else if (after && s > 1.0F - LAND) {
                w = (float) Ease.smoother((s - (1.0F - LAND)) / LAND);
                knee = kneels(l, this.rests[k + 1]);
                this.c.set(to);
            } else {
                continue;
            }
            weights[l] = w;
            this.reach(this.reached, l, new Vector3f(this.c), knee);
            this.body.place(this.pose);
        }
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            if (weights[l] > 0.0F) {
                this.pose.limb[l].slerp(this.reached.limb[l], weights[l]);
                this.pose.mid[l].slerp(this.reached.mid[l], weights[l]);
                this.pose.end[l].slerp(this.reached.end[l], weights[l]);
            }
        }
        this.body.place(this.pose);
    }

    // The weapon arm between moments k and k + 1, `s` of the way: its tip held where it is planted (moved over the
    // ground when it is planted anew), lifted off over LIFT and set down over LAND. How much of the arm leans so.
    private float leaning(int k, float s) {
        Brace brace = this.brace;
        float w;
        if (this.braced[k] && this.braced[k + 1]) {
            w = 1.0F;
            this.c.set(this.braceAt[k]).lerp(this.braceAt[k + 1], (float) Ease.smoother(s));
        } else if (this.braced[k] && s < LIFT) {
            w = 1.0F - (float) Ease.smoother(s / LIFT);
            this.c.set(this.braceAt[k]);
        } else if (this.braced[k + 1] && s > 1.0F - LAND) {
            w = (float) Ease.smoother((s - (1.0F - LAND)) / LAND);
            this.c.set(this.braceAt[k + 1]);
        } else {
            return 0.0F;
        }
        Vector3f ahead = ahead(this.pose, new Vector3f());
        brace.hold(this.body, this.reached, new Vector3f(this.c), ahead,
                elbow(ahead, brace.limb == 0 ? -1.0F : 1.0F, new Vector3f()));
        this.body.place(this.pose);
        return w;
    }

    private static int segment(float[] times, float u) {
        int k = 0;
        while (k < times.length - 2 && u >= times[k + 1]) {
            k++;
        }
        return k;
    }

    // Every number of the pose `u` of the way (between moments k and k + 1), on a curve through the moments that
    // passes each at the pace it moves there (its pace from the moment before to the one after), still at the first
    // and the last.
    private void curve(int k, float u) {
        for (int c = 0; c < 3; c++) {
            for (int j = 0; j < 4; j++) {
                this.p[j] = this.keys[this.clamp(k - 1 + j)].hips.get(c);
            }
            this.pose.hips.setComponent(c, hermite(this.times, k, u, this.p));
        }
        for (int i = 0; i < this.poseTurns.length; i++) {
            this.curveTurn(k, u, i, this.poseTurns[i]);
        }
    }

    private int clamp(int k) {
        return Math.max(0, Math.min(this.keys.length - 1, k));
    }

    // Turn i of the pose (in Pose.all's order) on its curve, as four numbers then made a turn again.
    private void curveTurn(int k, float u, int i, Quaternionf out) {
        for (int c = 0; c < 4; c++) {
            for (int j = 0; j < 4; j++) {
                Quaternionf turn = this.turns[this.clamp(k - 1 + j)][i];
                this.p[j] = c == 0 ? turn.x : c == 1 ? turn.y : c == 2 ? turn.z : turn.w;
            }
            this.result[c] = hermite(this.times, k, u, this.p);
        }
        out.set(this.result[0], this.result[1], this.result[2], this.result[3]).normalize();
    }

    // The value between moments k and k + 1 (p[1] and p[2], p[0] and p[3] the ones either side, as clamped at the
    // ends) at u: a cubic through them whose slope at each is the slope from its neighbour before to its neighbour
    // after, flat at the first and last moments.
    private static float hermite(float[] times, int k, float u, float[] p) {
        int last = times.length - 1;
        float t0 = times[k];
        float t1 = times[k + 1];
        float span = t1 - t0;
        float s = Math.max(0.0F, Math.min(1.0F, (u - t0) / span));
        float m0 = k == 0 ? 0.0F : (p[2] - p[0]) / (t1 - times[k - 1]) * span;
        float m1 = k + 1 >= last ? 0.0F : (p[3] - p[1]) / (times[k + 2] - t0) * span;
        float s2 = s * s;
        float s3 = s2 * s;
        return (2.0F * s3 - 3.0F * s2 + 1.0F) * p[1] + (s3 - 2.0F * s2 + s) * m0 + (-2.0F * s3 + 3.0F * s2) * p[2]
                + (s3 - s2) * m1;
    }

    // The whole pose turned `angle` about the upright line through the model's middle (as the creature turns).
    private void turn(BodyPose pose, float angle) {
        if (Math.abs(angle) < 1.0E-5F) {
            return;
        }
        this.q.rotationY(angle);
        Hanging hanging = this.body.body;
        for (int i = 0; i < hanging.n; i++) {
            if (hanging.inside[i]) {
                continue;
            }
            Vector3f at = pose.pos[i].mul(hanging.scale[i]).add(hanging.move[i]);
            this.q.transform(at).sub(hanging.move[i]).div(hanging.scale[i]);
            pose.rot[i].premul(this.q);
        }
    }
}
