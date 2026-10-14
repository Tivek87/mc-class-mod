package nl.tivek.multiversepowers.creature.animal.lynx.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the lynx moves: a long-legged cat's walk, a foot at a time a quarter of a stride apart, each paw kept where it
// was set down while the body goes over it, the forepaw flicking back at the wrist and the hind leg folding at the
// hock as each lifts, the head held low and steady; a bounding gallop when it runs, hind feet then forefeet, the body
// rocking and in the air between; the body leaning into its turns; ears turning, eyes blinking and the stub of a tail
// flicking while it stands; and its acts: sitting on its haunches, washing a raised forepaw, the stretch (forelegs out
// ahead, chest to the ground, rump up), lying with its paws out before it, the hiss (crouched, ears flat, mouth open,
// tail lashing) and the swipe of a forepaw. Paws stand flat wherever a pose puts them. The whole pose follows on
// springs, ears and tail loosest.
final class LynxPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int LUNGE = 4;
    static final int HEAD = 5;
    static final int HEAD_YAW = 6;
    static final int HEAD_ROLL = 7;
    static final int JAW = 8;
    // How far out each ear lies (right, then left), and how far both are laid back.
    static final int EAR_R = 9;
    static final int EAR_L = 10;
    static final int EARS_BACK = 11;
    static final int TAIL = 12;
    static final int TAIL_WAG = 13;
    static final int LOOK = 14;
    static final int BLINK = 15;
    static final int TAIL_TIP = 16;
    // Per leg (right front, left front, right hind, left hind): swing, the joint below it (elbow or stifle), the hock
    // (hind legs only), and the paw.
    static final int LEGS = 17;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down: walking left hind, left fore, right hind, right fore a quarter apart;
    // galloping the hind feet close together, then the forefeet half a stride on.
    private static final float[] WALK_AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.6F, 0.5F, 0.08F, 0.0F };
    private static final float WALK_SWEEP = 7.8F;
    private static final float RUN_SWEEP = 16.0F;
    private static final float LEG = 10.0F;
    // From the hips to the shoulders.
    private static final float SPAN = 9.5F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.6, 0.2, 0.45, EAR_R, EAR_L, EARS_BACK, TAIL,
            TAIL_WAG, TAIL_TIP);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Lynx lynx, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        int id = lynx.getId();
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.6, 0.9);
        float duty = Gait.duty(run, 0.62F, 0.35F);
        float phase = this.strides.phase(id, age, lynx.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        float turn = Mth.clamp(Mth.wrapDegrees(lynx.yBodyRot - lynx.yBodyRotO), -15.0F, 15.0F);
        c[ROLL] -= 0.012F * turn * a;
        c[HEAD_YAW] += 0.03F * turn;
        c[TAIL_WAG] -= 0.03F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.08F * Mth.sin(age * 0.08F);
        c[EAR_R] += 0.35F * Channels.twitch(age, id, 83, 5.0F);
        c[EAR_L] += 0.35F * Channels.twitch(age, id + 2, 119, 5.0F);
        c[TAIL_WAG] += 0.7F * Channels.twitch(age, id + 4, 70, 10.0F) * Mth.sin(age * 1.3F);
        for (int kind = Lynx.SIT; kind <= Lynx.SWIPE; kind++) {
            float w = lynx.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) lynx.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, lynx.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private void walk(float[] c, float phase, float a, float run, float duty) {
        float walk = a * (1.0F - run);
        float on = Gait.smooth(0.0F, 0.3F, a);
        float[] load = new float[4];
        float[] drop = new float[4];
        for (int k = 0; k < 4; k++) {
            int leg = FEET[k];
            float p = phase - Mth.TWO_PI * Mth.lerp(run, WALK_AT[k], RUN_AT[k]);
            float fold = on * Gait.fold(p, duty);
            float toe = on * Gait.toe(p, duty);
            if (k < 2) {
                float swing = on * Mth.lerp(run, 0.45F, 0.95F) * Gait.swing(p, duty);
                float wrist = Mth.lerp(run, 0.8F, 1.2F) * fold;
                c[leg] = swing;
                c[leg + 1] = wrist;
                // The paw kept flat under the leg's lean, flicked back as it lifts.
                c[leg + 3] = 1.1F * toe - swing - wrist;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            } else {
                float swing = on * Mth.lerp(run, 0.4F, 0.9F) * Gait.swing(p, duty);
                float stifle = Mth.lerp(run, 0.5F, 0.8F) * fold;
                float hock = -Mth.lerp(run, 0.8F, 1.2F) * fold;
                c[leg] = swing;
                c[leg + 1] = stifle;
                c[leg + 2] = hock;
                c[leg + 3] = 0.9F * toe - swing - stifle - hock;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            }
            load[k] = Gait.load(p, duty);
        }
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = hind * (1.0F - run) - 1.4F * run * flight;
        c[PITCH] = (fore - hind) / SPAN * (1.0F - run) + 0.14F * run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[ROLL] = 0.04F * walk * (load[0] + load[2] - load[1] - load[3]);
        // The head held low and steady over its prey, whatever the shoulders do.
        c[HEAD] = 0.15F * walk - 0.6F * c[PITCH] * run;
        c[EARS_BACK] = 0.5F * run;
        c[TAIL] = 0.3F * run;
        c[TAIL_TIP] = 0.3F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Lynx.SIT -> {
                sit(c);
                c[HEAD_YAW] = 0.3F * Mth.sin(t * 0.04F);
                c[LOOK] = 0.8F;
            }
            case Lynx.GROOM -> {
                // Sitting, a forepaw lifted to its mouth and licked.
                float lick = Mth.sin(t * 0.7F);
                sit(c);
                stand(c, RF, -2.1F + 0.06F * lick, 2.0F, 0.0F);
                c[HEAD] = 0.65F + 0.1F * lick;
                c[HEAD_YAW] = 0.3F;
                c[HEAD_ROLL] = 0.15F;
                c[JAW] = 0.15F * Math.max(0.0F, lick);
                c[LOOK] = 0.0F;
            }
            case Lynx.STRETCH -> {
                float down = Trunk.in(t, 0.0, 10.0) * (1.0F - Trunk.in(t, 30.0, Lynx.STRETCH_END));
                c[PITCH] = 0.45F * down;
                stand(c, RF, -1.0F * down, 0.0F, 0.0F);
                stand(c, LF, -1.0F * down, 0.0F, 0.0F);
                stand(c, RH, 0.0F, 0.0F, 0.0F);
                stand(c, LH, 0.0F, 0.0F, 0.0F);
                c[HEAD] = -0.4F * down;
                c[JAW] = 0.5F * Trunk.in(t, 12.0, 18.0) * (1.0F - Trunk.in(t, 24.0, 30.0));
                c[TAIL] = 0.9F * down;
                c[EARS_BACK] = 0.5F * down;
                c[LOOK] = 0.2F;
            }
            case Lynx.LIE -> {
                c[SINK] = 7.0F;
                c[PITCH] = c[ROLL] = 0.0F;
                c[BOB] = 0.12F * Mth.sin(t * 0.07F);
                // Forepaws out ahead, hind legs folded under its flanks.
                stand(c, RF, -1.45F, 0.0F, 0.0F);
                stand(c, LF, -1.45F, 0.0F, 0.0F);
                stand(c, RH, -1.15F, 1.9F, -2.3F);
                stand(c, LH, -1.15F, 1.9F, -2.3F);
                c[HEAD] = -0.05F;
                c[TAIL] = 0.2F;
                c[LOOK] = 0.8F;
            }
            case Lynx.HISS -> {
                // Crouched and drawn back, ears flat, mouth wide, the tail lashing.
                c[SINK] = 1.5F;
                c[PITCH] = -0.08F;
                c[LUNGE] = -1.0F;
                stand(c, RF, -0.25F, 0.5F, 0.0F);
                stand(c, LF, -0.25F, 0.5F, 0.0F);
                stand(c, RH, -0.2F, 0.4F, -0.2F);
                stand(c, LH, -0.2F, 0.4F, -0.2F);
                c[HEAD] = 0.15F;
                c[JAW] = 0.6F + 0.05F * Mth.sin(t * 1.5F);
                c[EARS_BACK] = 1.2F;
                c[EAR_R] = c[EAR_L] = 0.4F;
                c[TAIL_WAG] = 0.7F * Mth.sin(t * 0.9F);
                c[LOOK] = 1.0F;
            }
            case Lynx.SWIPE -> {
                float back = Trunk.in(t, 0.0, 2.5);
                float strike = Trunk.in(t, 2.5, Lynx.SWIPE_HIT);
                float home = Trunk.in(t, 7.0, Lynx.SWIPE_END);
                float out = strike * (1.0F - home);
                c[LUNGE] = 2.0F * out;
                c[PITCH] = -0.1F * back * (1.0F - strike) + 0.08F * out;
                // The right paw drawn back and up, then thrown forward and down.
                stand(c, RF, 0.5F * back * (1.0F - strike) - 1.35F * out, 1.3F * back * (1.0F - strike) + 0.2F * out,
                        0.0F);
                c[HEAD] = 0.1F * out;
                c[JAW] = 0.4F * out;
                c[EARS_BACK] = 1.0F;
                c[LOOK] = 1.0F;
            }
            default -> {
            }
        }
        return p;
    }

    // On its haunches: rump down, forelegs straight, hind legs folded with the feet flat ahead of them.
    private static void sit(float[] c) {
        c[PITCH] = -0.7F;
        c[SINK] = 6.5F;
        c[ROLL] = 0.0F;
        c[BOB] *= 0.3F;
        stand(c, RF, 0.0F, 0.0F, 0.0F);
        stand(c, LF, 0.0F, 0.0F, 0.0F);
        stand(c, RH, -1.1F, 1.75F, -2.2F);
        stand(c, LH, -1.1F, 1.75F, -2.2F);
        c[TAIL] = 0.5F;
    }

    // A leg posed whole, its paw turned flat to the ground.
    private static void stand(float[] c, int leg, float swing, float knee, float hock) {
        c[leg] = swing;
        c[leg + 1] = knee;
        c[leg + 2] = hock;
        c[leg + 3] = -swing - knee - hock;
    }
}
