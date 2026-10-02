package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import nl.tivek.multiversepowers.engine.client.pose.Shoulders;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person getting up as a person does, from how it lies (RiseMoments): face down it draws its hands in under its
// shoulders, pushes up onto its hands and knees, steps one foot up and kneels with a hand on that knee, then rises over
// it; face up it curls up to sit, turns onto a hand and a knee and goes on from kneeling (on its side, whichever it lies
// nearer; sitting up, it starts from curling up). Its hands lie flat and its feet stand flat where they are put on the
// ground while its weight moves over them, and step when they move; its shoulder blades follow its arms. Every joint
// moves on one smooth curve through the moments, the trunk, the head, each arm and each leg on a curve of its own a
// little ahead of or behind the others, so it never stops dead and never moves as one block. It fades in from just how
// it lay and out into its own pose; the moments are laid out along its way from where it lay to where it stands.
final class PersonRise implements GetUp.Rise {
    // How a limb rests (RiseMoments.how): its far end flat on the ground, its knee down, its hand on its own knee, its
    // foot on its toes.
    private static final int FLAT = 1;
    private static final int KNEE = 2;
    private static final int ON_KNEE = 3;
    private static final int TOES = 4;
    // Which group (RiseMoments) each turn of Pose.all moves with.
    private static final int[] GROUP = { 0, 0, 0, 1, 2, 3, 2, 3, 4, 5, 2, 3, 4, 5, 2, 3, 4, 5 };
    private static final float PI = (float) Math.PI;
    // Its pelvis this near upright (the cosine to straight up), it sits.
    static final float SITTING = 0.6F;
    // Varied, each moment comes up to this much sooner or later, each group's lead is this share more or less, and
    // the head turns up to this far (radians) from the third moment on.
    private static final float SOONER = 0.012F;
    private static final float LEAD_SPREAD = 0.35F;
    private static final float LOOK = 0.18F;

    // The pose fades in from how it lay over this much of getting up, and into its own pose from OWN_FROM on; its head
    // follows its trunk this far behind as well; it moves along its way to where it stands from DRIFT_FROM to DRIFT_TO.
    private static final float LIE_FADE = 0.06F;
    private static final float OWN_FROM = 0.84F;
    private static final float HEAD_LAG = 0.02F;
    private static final float DRIFT_FROM = 0.1F;
    private static final float DRIFT_TO = 0.9F;
    // A resting end lifting off is held over this first share of the time before the next moment; one landing is set
    // down over this last share; one moving on the ground is lifted this high (pixels) halfway, a foot stepping higher
    // and a knee sliding less.
    private static final float LIFT = 0.3F;
    private static final float LAND = 0.35F;
    private static final float STEP = 2.5F;
    private static final float STRIDE = 4.0F;
    private static final float SLIDE = 0.4F;
    // A flat hand's fingers turn this far out (radians); they point on from its shoulder as much as the hand is out
    // from under it, against the way the body faces counted this far (pixels). A hand on its knee rests this far
    // (pixels) over the thigh's top.
    private static final float FINGERS_OUT = 0.25F;
    private static final float REACH_OUT = 6.0F;
    private static final float ON_THIGH = 0.5F;
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
    // Whether it gets up with its hands (its arms not folded as one part), and the parts it does not move itself (the
    // folded arms), which its trunk carries.
    private final boolean hands;
    private final int[] extras;
    // The weapon it leans on getting up (null without one), at which moments, and where its tip rests then.
    @Nullable
    private final Brace brace;
    private final boolean[] braced;
    private final Vector3f[] braceAt;
    // When each group reaches each moment, [group][moment].
    private final float[][] times;
    private final Skeleton.Pose[] keys;
    // Each moment's turns (Pose.all), and the pose's own.
    private final Quaternionf[][] turns;
    private final int[] rests;
    // Where each resting end rests at each moment and the way its hand or foot runs there, [moment][limb].
    private final Vector3f[][] contacts;
    private final Vector3f[][] ways;
    private final Skeleton.Pose pose = new Skeleton.Pose();
    private final Quaternionf[] poseTurns = this.pose.all();
    private final Skeleton.Pose reached = new Skeleton.Pose();
    private final float[] weights = new float[Skeleton.LIMBS];
    private final float[] p = new float[4];
    private final float[] result = new float[4];
    private final BodyPose track = new BodyPose();
    private final BodyPose faded = new BodyPose();
    private final Vector3f a = new Vector3f();
    private final Vector3f b = new Vector3f();
    private final Vector3f c = new Vector3f();
    private final Quaternionf q = new Quaternionf();
    private final Quaternionf q2 = new Quaternionf();

    // `vary`: each moment a little sooner or later, each group a little more or less ahead or behind and the head
    // turned a little this way or that, by `seed` (never for the same body twice), so no two get up alike.
    PersonRise(Skeleton body, BodyPose lie, @Nullable Brace brace, boolean vary, long seed) {
        this.body = body;
        this.brace = brace;
        this.extras = extras(body);
        RandomSource random = vary ? RandomSource.create(seed) : null;
        Skeleton.Pose lying = new Skeleton.Pose();
        body.read(lie, lying);
        body.place(lying);
        // Face down its pelvis's front (-z) points at the ground (+y: y runs down); sitting, its trunk (-y) points up.
        boolean sitting = lying.pelvis.transform(this.a.set(0.0F, -1.0F, 0.0F)).y < -SITTING;
        boolean front = !sitting && lying.pelvis.transform(this.a.set(0.0F, 0.0F, -1.0F)).y > 0.0F;
        // With its arms folded as one part it gets up without its hands.
        this.hands = body.present(0) && body.present(1);
        RiseMoments.Moment[] plan = this.hands
                ? sitting ? RiseMoments.SEATED : front ? RiseMoments.FRONT : RiseMoments.BACK
                : sitting ? RiseMoments.SEATED_FOLDED : front ? RiseMoments.FRONT_FOLDED : RiseMoments.BACK_FOLDED;
        // Face down it steps through with the leg whose knee lies further ahead; face up it turns onto the side it
        // lies nearer: its front turned to its left, it lies on its right.
        boolean mirror = front ? body.joint(3, this.a).z < body.joint(2, this.b).z
                : lying.pelvis.transform(this.a.set(0.0F, 0.0F, -1.0F)).x > 0.0F;
        int n = plan.length + 1;
        this.times = new float[RiseMoments.GROUPS][n];
        this.keys = new Skeleton.Pose[n];
        this.rests = new int[n];
        this.contacts = new Vector3f[n][Skeleton.LIMBS];
        this.ways = new Vector3f[n][Skeleton.LIMBS];
        this.braced = new boolean[n];
        this.braceAt = new Vector3f[n];
        this.keys[0] = lying;
        this.rests[0] = this.resting(lying);
        for (int l = 0; l < Skeleton.LIMBS; l++) {
            boolean there = body.present(l);
            this.contacts[0][l] = there ? this.end(l, RiseMoments.how(l, this.rests[0]), new Vector3f())
                    : new Vector3f();
            this.ways[0][l] = there ? body.along(l, 2, new Vector3f()) : new Vector3f(0.0F, 1.0F, 0.0F);
        }
        Vector3f anchor = new Vector3f(lying.hips);
        Skeleton.Pose rest = new Skeleton.Pose();
        for (int k = 1; k < n; k++) {
            RiseMoments.Moment key = mirror ? RiseMoments.mirrored(plan[k - 1]) : plan[k - 1];
            float sooner = random == null ? 0.0F : (random.nextFloat() * 2.0F - 1.0F) * SOONER;
            float pace = random == null ? 1.0F : 1.0F + (random.nextFloat() * 2.0F - 1.0F) * LEAD_SPREAD;
            float look = random == null || k < 3 ? 0.0F : (random.nextFloat() * 2.0F - 1.0F) * LOOK;
            for (int g = 0; g < RiseMoments.GROUPS; g++) {
                // Each group in order, never at a moment before the one before it.
                this.times[g][k] = Math.max(key.u() + sooner - key.lead()[g] * pace, this.times[g][k - 1] + 0.03F);
            }
            Skeleton.Pose made = this.made(key, rest, look);
            float drift = drift(key.u());
            made.hips.x = anchor.x + (body.stands.x - anchor.x) * drift + key.side();
            made.hips.z = anchor.z + (body.stands.z - anchor.z) * drift - key.ahead();
            made.hips.y = anchor.y;
            body.place(made);
            made.hips.y += body.ground - body.lowest();
            body.place(made);
            this.rests[k] = key.rests();
            if (brace != null) {
                // The hand holding a weapon leans on it, not on its knee.
                this.rests[k] &= ~(brace.limb == 0 ? RiseMoments.ON_KNEE_R : RiseMoments.ON_KNEE_L);
            }
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
            if (!this.body.present(l)) {
                continue;
            }
            if (this.body.tip(l, this.a).y >= this.body.ground - ON_GROUND) {
                rests |= l < 2 ? 1 << l : l == 2 ? RiseMoments.FOOT_R : RiseMoments.FOOT_L;
            } else if (l >= 2 && this.body.joint(l, this.a).y >= this.body.ground - this.body.half[l] - ON_GROUND) {
                rests |= l == 2 ? RiseMoments.KNEE_R : RiseMoments.KNEE_L;
            }
        }
        return rests;
    }

    // Limb l's resting point as the body was placed last: its knee's middle when it kneels, else its far end.
    private Vector3f end(int l, int how, Vector3f out) {
        return how == KNEE ? this.body.joint(l, out) : this.body.tip(l, out);
    }

    // How high (y, pixels) limb l's resting point is as it rests `how`: a knee's or a flat hand's middle half its
    // thickness over the ground, a sole on it.
    private float height(int l, int how) {
        return how == KNEE || how == FLAT && l < 2 ? this.body.ground - this.body.half[l] : this.body.ground;
    }

    // Two ways of resting that keep an end where it is: the same, or a foot going from flat to its toes.
    private static boolean alike(int a, int b) {
        return a == b || (a == FLAT || a == TOES) && (b == FLAT || b == TOES);
    }

    // Moment k's resting ends put on the ground: where they rested the moment before when they have not gone far,
    // else where the moment puts them, set down on the ground; each limb reaching there. A hand on its knee is put
    // there as the knee moves.
    private void plant(Skeleton.Pose made, int k) {
        for (int l = 2; l < Skeleton.LIMBS + 2; l++) {
            int limb = l % Skeleton.LIMBS;
            if (!this.body.present(limb)) {
                this.contacts[k][limb] = new Vector3f();
                this.ways[k][limb] = new Vector3f(0.0F, 1.0F, 0.0F);
                continue;
            }
            int how = RiseMoments.how(limb, this.rests[k]);
            Vector3f at = this.end(limb, how, new Vector3f());
            this.contacts[k][limb] = at;
            if (how == ON_KNEE) {
                this.onKnee(made, limb);
            } else if (how != 0) {
                at.y = this.height(limb, how);
                Vector3f before = this.contacts[k - 1][limb];
                int was = RiseMoments.how(limb, this.rests[k - 1]);
                if (was != 0 && was != ON_KNEE && alike(was, how) && before.distance(at) < STAY) {
                    at.set(before);
                }
                this.reach(made, limb, at, how, this.way(made, limb, at, how));
            }
            this.ways[k][limb] = this.body.along(limb, 2, new Vector3f());
        }
        this.body.place(made);
    }

    // The way limb l's hand or foot runs resting `how` at `at`, the body placed: a hand lying flat, a foot standing
    // flat or on its toes, a kneeling leg's foot as it is.
    private Vector3f way(Skeleton.Pose pose, int l, Vector3f at, int how) {
        if (how == KNEE) {
            return this.body.along(l, 2, new Vector3f());
        }
        if (l < 2) {
            return this.fingers(l, at, this.body.pivot(l, new Vector3f()));
        }
        if (how == TOES) {
            // On its toes the foot leans halfway with its shin, its heel off the ground.
            return this.body.along(l, 1, new Vector3f()).add(0.0F, 1.0F, 0.0F).normalize();
        }
        return new Vector3f(0.0F, 1.0F, 0.0F);
    }

    // Limb l of a placed pose reaching its resting point `at`, resting `how`, its hand or foot running `way` (a
    // kneeling leg's shin and foot keep their shape from the knee). Its elbow or knee goes out the way it faces now
    // and the way it is bent, so the limb never twists round as it reaches.
    private void reach(Skeleton.Pose pose, int l, Vector3f at, int how, Vector3f way) {
        Vector3f tip = this.body.tip(l, new Vector3f());
        Vector3f joint = this.body.joint(l, new Vector3f());
        Vector3f pivot = this.body.pivot(l, new Vector3f());
        Vector3f toward = new Vector3f(joint).sub(new Vector3f(pivot).add(tip).mul(0.5F));
        float bent = toward.length();
        if (bent > 1.0E-3F) {
            toward.mul(Math.min(1.0F, bent) / bent);
        }
        toward.add(this.body.outer(l, this.c));
        if (how == KNEE) {
            tip.sub(joint).add(at);
            toward.add(0.0F, 2.0F, 0.0F);
        } else {
            tip.set(at);
        }
        this.body.reach(pose, l, tip, way, toward);
    }

    // The way a flat hand's fingers point: on from its shoulder, the further the hand is out from under it, else the
    // way the body faces as it gets up; turned a little out to the hand's own side.
    private Vector3f fingers(int l, Vector3f at, Vector3f shoulder) {
        Vector3f way = new Vector3f(at.x - shoulder.x, 0.0F, at.z - shoulder.z).add(0.0F, 0.0F, -REACH_OUT);
        if (way.lengthSquared() < 1.0E-4F) {
            way.set(0.0F, 0.0F, -1.0F);
        }
        way.normalize();
        // Turned out: the right hand to the person's right (-x), the left to its left.
        float out = l == 0 ? FINGERS_OUT : -FINGERS_OUT;
        return way.rotateY(way.z <= 0.0F ? out : -out);
    }

    // Arm l of a placed pose with its hand on its own side's knee: on the thigh's top just short of the knee, its
    // fingers over the knee, its elbow out to its side.
    private void onKnee(Skeleton.Pose pose, int l) {
        int leg = l + 2;
        Vector3f knee = this.body.joint(leg, new Vector3f());
        Vector3f thigh = new Vector3f(knee).sub(this.body.pivot(leg, new Vector3f())).normalize();
        Vector3f up = new Vector3f(0.0F, -1.0F, 0.0F).sub(new Vector3f(thigh).mul(-thigh.y));
        if (up.lengthSquared() < 1.0E-4F) {
            up.set(0.0F, 0.0F, -1.0F);
        }
        up.normalize();
        Vector3f tip = new Vector3f(knee).add(up.mul(this.body.half[leg] + ON_THIGH));
        Vector3f way = new Vector3f(thigh).add(0.0F, 0.8F, 0.0F).normalize();
        Vector3f toward = pose.pelvis.transform(new Vector3f(l == 0 ? -1.0F : 1.0F, 0.0F, 0.6F));
        this.body.reach(pose, l, tip, way, toward);
    }

    // Moment k with a weapon in hand: low, up on its knees or feet and that hand free, it leans on the weapon, its tip
    // planted where it was the moment before when that is near, else ahead of its shoulder.
    private void lean(Skeleton.Pose made, int k) {
        Brace brace = this.brace;
        int feet = RiseMoments.KNEE_R | RiseMoments.KNEE_L | RiseMoments.FOOT_R | RiseMoments.FOOT_L
                | RiseMoments.TOES_R | RiseMoments.TOES_L;
        if (brace == null || RiseMoments.how(brace.limb, this.rests[k]) != 0 || (this.rests[k] & feet) == 0
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

    // The pose of a moment from its numbers: every turn, the hips where the caller puts them; each shoulder blade
    // following its arm as well (Shoulders), the arm keeping the way it points.
    private Skeleton.Pose made(RiseMoments.Moment key, Skeleton.Pose rest, float look) {
        Skeleton.Pose made = new Skeleton.Pose().set(rest);
        made.pelvis.rotationY(key.yaw()).rotateX(key.pitch()).rotateZ(key.roll());
        fold(made.waist, this.body.hinge(Skeleton.BODY, 0), key.waist());
        fold(made.low, this.body.hinge(Skeleton.BODY, 1), key.low());
        made.head.rotationZYX(0.0F, key.shake() + look, key.nod());
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
        for (int s = 0; s < 2; s++) {
            if (this.body.blade(s) != null) {
                float stretch = 1.0F - made.mid[s].angle() / PI;
                Shoulders.turn(s == 0, this.body.armWay(made, s, this.a), stretch, this.q);
                this.body.shrug(made, s, this.q.premul(made.blade[s]));
            }
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
    public void pose(float u, BodyPose lie, BodyPose own, BodyPose out) {
        this.at(u);
        this.body.write(this.pose, this.track);
        this.carry(u, lie, own);
        float toOwn = (float) Ease.smoother((u - OWN_FROM) / (1.0F - OWN_FROM));
        PoseBlend.blend(this.body.body, this.track, own, toOwn, toOwn, this.faded);
        float fromLie = 1.0F - (float) Ease.smoother(u / LIE_FADE);
        PoseBlend.blend(this.body.body, this.faded, lie, fromLie, fromLie, out);
    }

    // Every part that is none of its head, trunk, arms and legs.
    private static int[] extras(Skeleton body) {
        int n = body.body.n;
        int[] extras = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            boolean role = false;
            for (int r : body.roles) {
                role |= r == i;
            }
            if (!role) {
                extras[count++] = i;
            }
        }
        return Arrays.copyOf(extras, count);
    }

    // The parts it does not move itself go along with the piece of its trunk they hang from, from how they lay there to
    // how they hang as it stands.
    private void carry(float u, BodyPose lie, BodyPose own) {
        float w = (float) Ease.smoother(u);
        Hanging hanging = this.body.body;
        for (int i : this.extras) {
            PoseBlend.inTrunk(hanging, i, lie, this.a, this.q);
            PoseBlend.inTrunk(hanging, i, own, this.b, this.q2);
            this.a.lerp(this.b, w);
            this.q.slerp(this.q2, w);
            PoseBlend.fromTrunk(hanging, i, this.a, this.q, this.track);
            for (int j = 0; j < BodyPose.JOINTS; j++) {
                this.track.joint[j][i].set(lie.joint[j][i]).slerp(own.joint[j][i], w);
            }
        }
    }

    // The body `u` of the way up, placed: every joint on its curve through the moments, the body kept off the ground,
    // and its resting ends where they rest: the legs first, so a hand on a knee finds the knee where it is.
    void at(float u) {
        this.curve(u);
        this.body.place(this.pose);
        float low = this.body.lowest();
        if (low > this.body.ground) {
            this.pose.hips.y -= low - this.body.ground;
            this.body.place(this.pose);
        }
        this.rest(u, 2);
        if (this.hands) {
            this.rest(u, 0);
        }
    }

    // Limbs l and l + 1 reaching where they rest at `u`, each by the moments of its own curve.
    private void rest(float u, int from) {
        this.reached.set(this.pose);
        for (int l = from; l < from + 2; l++) {
            this.weights[l] = this.contact(u, l);
            this.body.place(this.pose);
        }
        for (int l = from; l < from + 2; l++) {
            float w = this.weights[l];
            if (w > 0.0F) {
                this.pose.limb[l].slerp(this.reached.limb[l], w);
                this.pose.mid[l].slerp(this.reached.mid[l], w);
                this.pose.end[l].slerp(this.reached.end[l], w);
                if (l < 2) {
                    // A hand put on the ground or a knee brings its shoulder blade along.
                    this.pose.blade[l].slerp(this.reached.blade[l], w);
                }
            }
        }
        this.body.place(this.pose);
    }

    // Limb l of `reached` where it rests at `u`, between its moments k and k + 1: held where it rests (moved over the
    // ground, lifted halfway, when it is put down anew), lifted off over LIFT and set down over LAND. How much of the
    // limb rests so.
    private float contact(float u, int l) {
        float[] times = this.times[RiseMoments.ARM_R + l];
        int k = segment(times, u);
        float s = Math.max(0.0F, Math.min(1.0F, (u - times[k]) / (times[k + 1] - times[k])));
        if (this.brace != null && l == this.brace.limb && (this.braced[k] || this.braced[k + 1])) {
            float lean = this.leaning(k, s);
            if (lean > 0.0F) {
                return lean;
            }
        }
        int before = RiseMoments.how(l, this.rests[k]);
        int after = RiseMoments.how(l, this.rests[k + 1]);
        Vector3f from = this.contacts[k][l];
        Vector3f to = this.contacts[k + 1][l];
        float w;
        int how;
        Vector3f way = new Vector3f();
        if (before != 0 && after != 0 && alike(before, after)) {
            w = 1.0F;
            how = s < 0.5F ? before : after;
            float e = (float) Ease.smoother(s);
            float far = from.distance(to);
            this.c.set(from).lerp(to, e);
            way.set(this.ways[k][l]).lerp(this.ways[k + 1][l], e);
            if (far > 0.5F) {
                float lift = how == KNEE ? SLIDE : l >= 2 ? Math.min(STRIDE, far * 0.5F) : Math.min(STEP, far * 0.4F);
                this.c.y -= lift * (float) Math.sin(Math.PI * s);
            }
        } else if (before != 0 && s < LIFT) {
            w = 1.0F - (float) Ease.smoother(s / LIFT);
            how = before;
            this.c.set(from);
            way.set(this.ways[k][l]);
        } else if (after != 0 && s > 1.0F - LAND) {
            w = (float) Ease.smoother((s - (1.0F - LAND)) / LAND);
            how = after;
            this.c.set(to);
            way.set(this.ways[k + 1][l]);
        } else {
            return 0.0F;
        }
        if (how == ON_KNEE) {
            this.onKnee(this.reached, l);
        } else {
            if (l < 2 && how == FLAT) {
                // A resting hand lies level, never pointing into the ground.
                way.y = 0.0F;
            }
            if (how == KNEE || way.lengthSquared() < 1.0E-6F) {
                this.body.along(l, 2, way);
            }
            this.reach(this.reached, l, new Vector3f(this.c), how, way.normalize());
        }
        return w;
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

    // Every number of the pose `u` of the way, on a curve through the moments that passes each at the pace it moves
    // there (its pace from the moment before to the one after), still at the first and the last: each group by its
    // own times, the head a little behind on top of that.
    private void curve(float u) {
        float[] trunk = this.times[RiseMoments.TRUNK];
        int k = segment(trunk, u);
        for (int c = 0; c < 3; c++) {
            for (int j = 0; j < 4; j++) {
                this.p[j] = this.keys[this.clamp(k - 1 + j)].hips.get(c);
            }
            this.pose.hips.setComponent(c, hermite(trunk, k, u, this.p));
        }
        for (int i = 0; i < this.poseTurns.length; i++) {
            int group = GROUP[i];
            float at = group == RiseMoments.HEAD ? Math.max(0.0F, u - HEAD_LAG) : u;
            float[] times = this.times[group];
            this.curveTurn(times, segment(times, at), at, i, this.poseTurns[i]);
        }
    }

    private int clamp(int k) {
        return Math.max(0, Math.min(this.keys.length - 1, k));
    }

    // Turn i of the pose (in Pose.all's order) on its curve, as four numbers then made a turn again.
    private void curveTurn(float[] times, int k, float u, int i, Quaternionf out) {
        for (int c = 0; c < 4; c++) {
            for (int j = 0; j < 4; j++) {
                Quaternionf turn = this.turns[this.clamp(k - 1 + j)][i];
                this.p[j] = c == 0 ? turn.x : c == 1 ? turn.y : c == 2 ? turn.z : turn.w;
            }
            this.result[c] = hermite(times, k, u, this.p);
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
}
