package nl.tivek.multiversepowers.creature.animal.monkey.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.monkey.Monkey;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the monkey moves: on all fours as a primate walks, each foot followed by the hand across from it, a quarter of
// a stride apart, every hand and foot kept where it was set down while the body goes over it, a hand rolling up off
// its palm and a foot off its heel as each lifts, its tail held up in an arch and swaying behind; a bounding gallop
// when it runs, hands then feet, its back rocking and the tail streaming lower; the body leaning into its turns; its
// head cocking, its eyes blinking and its tail tip twitching while it stands; and its acts: sitting upright on its rump
// with its knees up, scratching its side with one hand, eating with both hands at its mouth, and standing up on its
// hind legs to hoot, arms thrown up and bouncing. The whole pose follows on springs, the tail loosest.
final class MonkeyPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int HEAD = 4;
    static final int HEAD_YAW = 5;
    static final int HEAD_ROLL = 6;
    static final int JAW = 7;
    // How far the tail is carried: 1 up in its arch, 0 laid out behind it.
    static final int TAIL = 8;
    static final int TAIL_WAG = 9;
    static final int TAIL_SWAY = 10;
    static final int LOOK = 11;
    static final int BLINK = 12;
    // Per limb (right arm, left arm, right leg, left leg): swing, outward, its elbow or knee, and its hand or foot.
    static final int LIMBS = 13;
    static final int PER_LIMB = 4;
    private static final int COUNT = LIMBS + 4 * PER_LIMB;
    private static final int RA = LIMBS;
    private static final int LA = LIMBS + PER_LIMB;
    private static final int RL = LIMBS + 2 * PER_LIMB;
    private static final int LL = LIMBS + 3 * PER_LIMB;
    private static final int[] FEET = { RA, LA, RL, LL };
    // Where in a stride each is set down: walking left foot, right hand, right foot, left hand a quarter apart;
    // galloping the hands close together, then the feet half a stride on.
    private static final float[] WALK_AT = { 0.25F, 0.75F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.5F, 0.6F, 0.0F, 0.1F };
    private static final float WALK_SWEEP = 6.0F;
    private static final float RUN_SWEEP = 13.0F;
    private static final float LEG = 8.0F;
    // From the hips to the shoulders.
    private static final float SPAN = 8.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.65, 0.2, 0.45, TAIL, TAIL_WAG, TAIL_SWAY);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Monkey monkey, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        c[TAIL] = 1.0F;
        int id = monkey.getId();
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.6, 0.9);
        float duty = Gait.duty(run, 0.62F, 0.38F);
        float phase = this.strides.phase(id, age, monkey.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        float turn = Mth.clamp(Mth.wrapDegrees(monkey.yBodyRot - monkey.yBodyRotO), -15.0F, 15.0F);
        c[ROLL] -= 0.012F * turn * a;
        c[HEAD_YAW] += 0.03F * turn;
        c[TAIL_WAG] -= 0.04F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[HEAD_ROLL] += 0.35F * Channels.twitch(age, id, 110, 24.0F);
        c[TAIL_WAG] += 0.4F * Channels.twitch(age, id + 2, 90, 12.0F) * Mth.sin(age * 1.1F);
        c[BOB] += 0.06F * Mth.sin(age * 0.1F);
        for (int kind = Monkey.SIT; kind <= Monkey.HOOT; kind++) {
            float w = monkey.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) monkey.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, monkey.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private void walk(float[] c, float phase, float a, float run, float duty) {
        float walk = a * (1.0F - run);
        float on = Gait.smooth(0.0F, 0.3F, a);
        float[] load = new float[4];
        float[] drop = new float[4];
        for (int k = 0; k < 4; k++) {
            int limb = FEET[k];
            float p = phase - Mth.TWO_PI * Mth.lerp(run, WALK_AT[k], RUN_AT[k]);
            float swing = on * Mth.lerp(run, 0.45F, 0.95F) * Gait.swing(p, duty);
            // An elbow folds forward of the arm, a knee back of the leg.
            float joint = (k < 2 ? -1.0F : 1.1F) * Mth.lerp(run, 0.8F, 1.2F) * on * Gait.fold(p, duty);
            c[limb] = swing;
            c[limb + 2] = joint;
            // The hand or foot kept flat under the limb's lean, rolling up off its heel as it lifts.
            c[limb + 3] = on * Gait.toe(p, duty) - swing - joint;
            drop[k] = LEG * (1.0F - Mth.cos(swing));
            load[k] = Gait.load(p, duty);
        }
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = hind * (1.0F - run) - 0.5F * run * flight;
        c[PITCH] = (fore - hind) / SPAN * (1.0F - run) + 0.15F * run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[ROLL] = 0.06F * walk * (load[0] + load[2] - load[1] - load[3]);
        c[HEAD] = -0.5F * c[PITCH];
        c[TAIL] = 1.0F - 0.5F * run;
        c[TAIL_WAG] = 0.2F * walk * (load[2] - load[3]);
        c[TAIL_SWAY] = 0.5F * a * (load[0] + load[1] - 1.0F);
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        float side = id % 2 == 0 ? 1.0F : -1.0F;
        switch (kind) {
            case Monkey.SIT -> {
                sit(c);
                c[HEAD_YAW] = 0.5F * Mth.sin(t * 0.05F);
                c[LOOK] = 0.8F;
            }
            case Monkey.SCRATCH -> {
                // Sitting, one hand raking quickly at its ribs.
                float rake = Mth.sin(t * 1.8F);
                int arm = side > 0.0F ? RA : LA;
                sit(c);
                limb(c, arm, 0.5F + 0.25F * rake, 0.5F, -2.0F + 0.3F * rake);
                c[arm + 3] = 0.4F + 0.3F * rake;
                c[ROLL] = side * 0.12F;
                c[HEAD] = 0.25F;
                c[HEAD_YAW] = -side * 0.5F;
                c[HEAD_ROLL] = -side * 0.2F;
                c[LOOK] = 0.0F;
            }
            case Monkey.EAT -> {
                // Sitting, both hands at its mouth, turning what it holds and biting at it.
                float bite = Mth.sin(t * 0.9F);
                sit(c);
                limb(c, RA, -1.0F + 0.05F * bite, -0.25F, -2.2F);
                limb(c, LA, -1.0F - 0.05F * bite, -0.25F, -2.2F);
                // Both hands turned in to hold what it eats.
                c[RA + 3] = -0.4F + 0.15F * bite;
                c[LA + 3] = -0.4F - 0.15F * bite;
                c[HEAD] = 0.3F + 0.08F * bite;
                c[JAW] = 0.3F * Math.max(0.0F, bite);
                c[LOOK] = 0.3F;
            }
            case Monkey.HOOT -> {
                // Up on its hind legs, arms thrown up, bouncing at each hoot.
                float up = Trunk.in(t, 0.0, 5.0) * (1.0F - Trunk.in(t, Monkey.HOOT_END - 6.0, Monkey.HOOT_END));
                float hoot = note(t, 2.0) + note(t, 8.0) + note(t, 14.0) + note(t, 20.0) + note(t, 27.0);
                c[PITCH] = -1.3F * up;
                c[BOB] = -1.0F * hoot * up;
                c[ROLL] = 0.0F;
                limb(c, RA, -2.6F * up + 0.3F * hoot, 0.5F * up, -0.5F * up);
                limb(c, LA, -2.6F * up + 0.3F * hoot, 0.5F * up, -0.5F * up);
                c[RA + 3] = c[LA + 3] = 0.3F * hoot;
                limb(c, RL, 0.0F, 0.1F * up, 0.3F * hoot);
                limb(c, LL, 0.0F, 0.1F * up, 0.3F * hoot);
                c[HEAD] = -0.3F * up - 0.15F * hoot;
                c[JAW] = 0.55F * hoot;
                c[TAIL] = 1.0F - 0.5F * up;
                c[LOOK] = 0.5F;
            }
            default -> {
            }
        }
        return p;
    }

    // Upright on its rump: knees drawn up, feet flat before it, hands hanging over its knees, tail laid out behind.
    private static void sit(float[] c) {
        c[PITCH] = -1.15F;
        c[SINK] = 6.0F;
        c[ROLL] = 0.0F;
        c[BOB] *= 0.3F;
        limb(c, RA, -0.5F, 0.1F, -0.5F);
        limb(c, LA, -0.5F, 0.1F, -0.5F);
        limb(c, RL, -2.0F, 0.35F, 1.8F);
        limb(c, LL, -2.0F, 0.35F, 1.8F);
        c[TAIL] = 0.12F;
        c[TAIL_WAG] *= 0.5F;
    }

    // A limb posed whole, its hand or foot flat to the ground (an arm off the ground is given its hand again).
    private static void limb(float[] c, int limb, float swing, float out, float joint) {
        c[limb] = swing;
        c[limb + 1] = out;
        c[limb + 2] = joint;
        c[limb + 3] = -swing - joint;
    }

    // One hoot starting at `from`.
    private static float note(float t, double from) {
        return Trunk.in(t, from, from + 1.5) * (1.0F - Trunk.in(t, from + 2.5, from + 5.0));
    }
}
