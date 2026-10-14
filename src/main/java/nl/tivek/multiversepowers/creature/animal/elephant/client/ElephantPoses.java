package nl.tivek.multiversepowers.creature.animal.elephant.client;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.elephant.Elephant;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the elephant moves: a slow, rolling walk, a foot at a time a quarter of a stride apart, each foot kept flat
// where it was set down while the great body goes over it, the leg folding at the knee as it swings, the shoulders and
// hips rising and falling over the legs that bear them, its head nodding and its trunk and ears swinging behind; the
// same walk longer and faster when it charges, as an elephant never leaves the ground; the body leaning into its
// turns; ears fanning, eyes blinking, trunk swaying and tail swishing while it stands; and its acts: its trunk reached
// up into the leaves and curled back to its mouth, its trunk sweeping the ground for grass and curled back to its
// mouth, dust drawn up and thrown back over its shoulders, the threat (head high, ears spread wide, trunk thrown up,
// rocking on its forefeet), and the strike: head swung down and then up, tusks first, its trunk flung forward. The
// whole pose follows on springs, the trunk, ears and tail slow and swinging past.
final class ElephantPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int HEAD = 4;
    static final int HEAD_YAW = 5;
    static final int HEAD_ROLL = 6;
    static final int JAW = 7;
    // How far each ear stands out from the head (right, then left).
    static final int EAR_R = 8;
    static final int EAR_L = 9;
    // The trunk's five pieces from the face down, each against the one above it, and its swing to the side.
    static final int TRUNK = 10;
    static final int PIECES = 5;
    static final int TRUNK_SIDE = 15;
    static final int TAIL = 16;
    static final int TAIL_SIDE = 17;
    static final int LOOK = 18;
    // Where each ear's back half is (right, then left): its ear's own place, followed more loosely.
    static final int FLAP_R = 19;
    static final int FLAP_L = 20;
    static final int BLINK = 21;
    // Per leg (right front, left front, right hind, left hind): swing, outward, its knee, and its foot.
    static final int LEGS = 22;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    // Where in a stride each foot is set down: left hind, left fore, right hind, right fore a quarter apart.
    private static final float[] AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float WALK_SWEEP = 18.0F;
    private static final float FAST_SWEEP = 28.0F;
    private static final float LEG = 30.0F;
    // From the hips to the shoulders.
    private static final float SPAN = 25.0F;
    private static final double[][] SPRINGS = springs();
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);
    // Hanging, its tip curled a little back.
    private static final float[] REST = { -0.12F, 0.05F, 0.1F, 0.15F, 0.3F };
    // Hung forward and curled back up, so its tip comes to the mouth.
    private static final float[] TO_MOUTH = { -0.5F, -0.1F, 1.4F, 1.6F, 0.8F };
    // Reached up and forward into the leaves, its tip hooked over them.
    private static final float[] UP = { -2.1F, -0.35F, -0.2F, 0.3F, 0.8F };
    // Let down to the ground, its tip turned forward along it.
    private static final float[] DOWN = { -0.08F, 0.0F, -0.05F, -0.35F, -0.9F };
    // Thrown back over the head.
    private static final float[] BACK = { -2.5F, -0.6F, -0.5F, -0.4F, -0.2F };
    // Thrown up in an S before the face.
    private static final float[] RAISED = { -1.9F, -0.7F, 0.2F, 0.7F, 0.6F };
    // What a strike adds to how it hangs: tucked back under the chin, then flung up and forward.
    private static final float[] TUCKED = { 0.5F, 0.6F, 0.7F, 0.6F, 0.5F };
    private static final float[] FLUNG = { -1.6F, -0.5F, -0.2F, 0.2F, 0.4F };
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Elephant elephant, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        System.arraycopy(REST, 0, c, TRUNK, PIECES);
        int id = elephant.getId();
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float fast = Trunk.in(a, 0.5, 0.9);
        float duty = Gait.duty(fast, 0.72F, 0.55F);
        float phase = this.strides.phase(id, age, elephant.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(fast, WALK_SWEEP, FAST_SWEEP)));
        this.walk(c, phase, a, fast, duty);
        float turn = Mth.clamp(Mth.wrapDegrees(elephant.yBodyRot - elephant.yBodyRotO), -10.0F, 10.0F);
        c[ROLL] -= 0.008F * turn * a;
        c[HEAD_YAW] += 0.02F * turn;
        c[TRUNK_SIDE] -= 0.04F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.12F * Mth.sin(age * 0.05F);
        // It fans itself: both ears out and back together, for a while, then held still.
        float fan = Channels.twitch(age, id, 190, 110.0F) * (0.5F + 0.5F * Mth.sin(age * 0.22F));
        c[EAR_R] += 0.7F * fan;
        c[EAR_L] += 0.7F * fan;
        float sway = Mth.sin(age * 0.045F);
        c[TRUNK_SIDE] += 0.1F * sway;
        c[TRUNK + 3] += 0.12F * Mth.sin(age * 0.07F + 1.0F);
        c[TRUNK + 4] += 0.2F * Mth.sin(age * 0.07F);
        float swish = Channels.twitch(age, id + 4, 80, 24.0F);
        c[TAIL_SIDE] += 0.6F * swish * Mth.sin(age * 0.5F);
        for (int kind = Elephant.REACH; kind <= Elephant.STRIKE; kind++) {
            float w = elephant.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) elephant.actAge(kind, partialTick), id), w);
            }
        }
        c[FLAP_R] = c[EAR_R];
        c[FLAP_L] = c[EAR_L];
        float blink = c[BLINK];
        this.springs.follow(id, c, age, elephant.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private static double[][] springs() {
        int[] loose = new int[PIECES + 7];
        for (int k = 0; k < PIECES; k++) {
            loose[k] = TRUNK + k;
        }
        int[] more = { TRUNK_SIDE, EAR_R, EAR_L, TAIL, TAIL_SIDE, FLAP_R, FLAP_L };
        System.arraycopy(more, 0, loose, PIECES, more.length);
        double[][] s = Channels.springs(COUNT, 0.35, 0.16, 0.45, loose);
        // The ears' back halves lag furthest.
        s[0][FLAP_R] = s[0][FLAP_L] = 0.1;
        s[1][FLAP_R] = s[1][FLAP_L] = 0.35;
        return s;
    }

    private void walk(float[] c, float phase, float a, float fast, float duty) {
        float on = Gait.smooth(0.0F, 0.3F, a);
        float[] load = new float[4];
        float[] drop = new float[4];
        for (int k = 0; k < 4; k++) {
            int leg = LEGS + k * PER_LEG;
            float p = phase - Mth.TWO_PI * AT[k];
            float swing = on * Mth.lerp(fast, 0.3F, 0.45F) * Gait.swing(p, duty);
            float knee = Mth.lerp(fast, 0.7F, 0.95F) * on * Gait.fold(p, duty);
            c[leg] = swing;
            c[leg + 2] = knee;
            // The foot kept flat, its heel rolling up only a little as it lifts.
            c[leg + 3] = 0.35F * on * Gait.toe(p, duty) - swing - knee;
            drop[k] = LEG * (1.0F - Mth.cos(swing));
            load[k] = Gait.load(p, duty);
        }
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        c[BOB] = hind;
        c[PITCH] = (fore - hind) / SPAN;
        // The great body rolls toward the side whose legs bear it.
        c[ROLL] = 0.05F * a * (load[0] + load[2] - load[1] - load[3]);
        float nod = load[0] + load[1] - 1.0F;
        c[HEAD] = 0.06F * a * nod - 0.12F * fast - c[PITCH];
        c[TRUNK_SIDE] = 0.2F * a * (load[1] - load[0]);
        c[TRUNK + 1] += 0.1F * a * nod;
        c[TRUNK + 3] += 0.15F * fast;
        c[TRUNK + 4] += 0.3F * fast;
        c[EAR_R] = c[EAR_L] = 0.9F * fast;
        c[EAR_R] += 0.12F * a * load[0];
        c[EAR_L] += 0.12F * a * load[1];
        c[TAIL] = 0.5F * fast;
        c[TAIL_SIDE] = 0.2F * a * (load[2] - load[3]);
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Elephant.REACH -> {
                // Up into the leaves, a pull, then the trunk curls back to the mouth and the jaw works.
                float at = t % 70.0F;
                float feed = Trunk.in(at, 28.0, 40.0) * (1.0F - Trunk.in(at, 60.0, 70.0));
                float pull = Trunk.in(at, 20.0, 24.0) * (1.0F - Trunk.in(at, 24.0, 30.0));
                trunk(c, UP, TO_MOUTH, feed);
                c[TRUNK] += 0.25F * pull;
                c[TRUNK_SIDE] = 0.12F * Mth.sin(t * 0.03F) * (1.0F - feed);
                c[HEAD] = -0.3F * (1.0F - feed) + 0.08F * pull;
                c[JAW] = 0.3F * feed * (0.5F + 0.5F * Mth.sin(at * 0.7F));
                c[LOOK] = 0.2F;
            }
            case Elephant.GRAZE -> {
                // The trunk's tip sweeps the ground, wraps a tuft and tears it loose, then carries it to the mouth.
                float at = t % 64.0F;
                float feed = Trunk.in(at, 30.0, 40.0) * (1.0F - Trunk.in(at, 56.0, 64.0));
                float tear = Trunk.in(at, 22.0, 26.0) * (1.0F - Trunk.in(at, 26.0, 31.0));
                trunk(c, DOWN, TO_MOUTH, feed);
                c[TRUNK + 4] += 0.9F * tear * (1.0F - feed);
                c[TRUNK_SIDE] = 0.3F * Mth.sin(at * 0.2F) * (1.0F - feed) * (1.0F - tear);
                c[HEAD] = 0.22F * (1.0F - feed) - 0.1F * tear;
                c[JAW] = 0.3F * feed * (0.5F + 0.5F * Mth.sin(at * 0.7F));
                c[LOOK] = 0.3F;
            }
            case Elephant.DUST -> {
                // Trunk to the ground drawing dust up, then swung up and back and blown out over its shoulders.
                float draw = Trunk.in(t, 0.0, 10.0);
                float swing = Trunk.in(t, 16.0, Elephant.DUST_THROW);
                float home = Trunk.in(t, Elephant.DUST_OUT + 2.0, Elephant.DUST_END);
                float blow = Trunk.in(t, Elephant.DUST_THROW, Elephant.DUST_THROW + 3.0) * (1.0F - home);
                for (int k = 0; k < PIECES; k++) {
                    float down = Mth.lerp(draw, c[TRUNK + k], DOWN[k]);
                    c[TRUNK + k] = Mth.lerp(home, Mth.lerp(swing, down, BACK[k]), c[TRUNK + k]);
                }
                c[TRUNK + 4] += 0.4F * blow * Mth.sin(t * 1.5F);
                c[TRUNK_SIDE] = 0.3F * swing * (1.0F - home) * (id % 2 == 0 ? 1.0F : -1.0F);
                c[HEAD] = 0.15F * draw * (1.0F - swing) - 0.25F * swing * (1.0F - home);
                c[EAR_R] = c[EAR_L] = 0.9F * blow * (0.5F + 0.5F * Mth.sin(t * 0.8F));
                c[TAIL_SIDE] = 0.5F * blow * Mth.sin(t * 0.7F);
                c[LOOK] = 0.0F;
            }
            case Elephant.THREAT -> {
                float on = Trunk.in(t, 0.0, 6.0);
                float rock = on * Mth.sin(t * 0.4F);
                trunk(c, null, RAISED, on);
                c[TRUNK + 4] += 0.25F * rock;
                c[TRUNK_SIDE] = 0.1F * rock;
                c[HEAD] = -0.35F * on + 0.06F * rock;
                c[HEAD_ROLL] = 0.08F * rock;
                c[EAR_R] = c[EAR_L] = 1.25F * on;
                c[PITCH] = -0.04F * on + 0.02F * rock;
                c[RF] = -0.2F * on * Math.max(0.0F, rock);
                c[RF + 2] = 0.7F * on * Math.max(0.0F, rock);
                c[LF] = -0.2F * on * Math.max(0.0F, -rock);
                c[LF + 2] = 0.7F * on * Math.max(0.0F, -rock);
                c[RF + 3] = -c[RF] - c[RF + 2];
                c[LF + 3] = -c[LF] - c[LF + 2];
                c[JAW] = 0.4F * on;
                c[TAIL] = 0.9F * on;
                c[LOOK] = 1.0F;
            }
            case Elephant.STRIKE -> {
                // Head down and trunk tucked, then both thrown up and forward.
                float low = Trunk.in(t, 0.0, 5.0);
                float toss = Trunk.in(t, 5.5, Elephant.STRIKE_HIT);
                float back = Trunk.in(t, 12.0, Elephant.STRIKE_END);
                float down = low * (1.0F - toss);
                float up = toss * (1.0F - back);
                c[HEAD] = 0.45F * down - 0.55F * up;
                c[PITCH] = 0.06F * down - 0.08F * up;
                c[BOB] = 1.0F * down;
                for (int k = 0; k < PIECES; k++) {
                    c[TRUNK + k] = REST[k] + TUCKED[k] * down + FLUNG[k] * up;
                }
                c[TRUNK_SIDE] = 0.0F;
                c[EAR_R] = c[EAR_L] = 1.2F;
                c[TAIL] = 0.8F;
                c[LOOK] = 0.5F;
            }
            default -> {
            }
        }
        return p;
    }

    // The trunk between two shapes; with no first shape, from how it hangs now.
    private static void trunk(float[] c, @Nullable float[] from, float[] to, float t) {
        for (int k = 0; k < PIECES; k++) {
            c[TRUNK + k] = Mth.lerp(t, from == null ? c[TRUNK + k] : from[k], to[k]);
        }
    }
}
