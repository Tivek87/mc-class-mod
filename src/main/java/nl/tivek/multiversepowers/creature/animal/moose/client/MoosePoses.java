package nl.tivek.multiversepowers.creature.animal.moose.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.moose.Moose;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the moose moves: a long, slow walk, a foot at a time a quarter of a stride apart, each hoof kept where it was set
// down while the body goes over it, the foreleg folding at the knee and the hind leg at the hock as it lifts high over
// the ground, its head nodding as each forefoot takes the weight and its bell swinging behind; a ground-covering trot
// when it runs, diagonal legs together, the body floating level, head held out and ears back; the body leaning into
// its turns; ears turning, eyes blinking and jaw chewing while it stands; and its acts: browsing with its head reached
// up, its head down to the ground between spread forelegs, lying with its legs folded under it, shaking the water out
// of its coat, the threat (head low, ears flat back, a forefoot pawing the ground), and the stomp: reared up, both
// forefeet drawn in and struck down together. The whole pose follows on springs, ears, bell and tail loosest.
final class MoosePoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int NECK = 4;
    static final int NECK_YAW = 5;
    static final int HEAD = 6;
    static final int HEAD_YAW = 7;
    static final int HEAD_ROLL = 8;
    static final int JAW = 9;
    // How far out each ear lies (right, then left), and how far both are laid back.
    static final int EAR_R = 10;
    static final int EAR_L = 11;
    static final int EARS_BACK = 12;
    static final int BELL = 13;
    static final int BELL_SIDE = 14;
    static final int TAIL = 15;
    static final int LOOK = 16;
    static final int BLINK = 17;
    // Per leg (right front, left front, right hind, left hind): swing, outward, the joint below it (knee or stifle),
    // the hock (hind legs only), and the hoof.
    static final int LEGS = 18;
    static final int PER_LEG = 5;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down: walking left hind, left fore, right hind, right fore a quarter apart;
    // trotting each forefoot with the hind foot across from it.
    private static final float[] WALK_AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.0F, 0.5F, 0.5F, 0.0F };
    private static final float WALK_SWEEP = 10.7F;
    private static final float RUN_SWEEP = 18.0F;
    private static final float LEG = 17.0F;
    // From the hips to the shoulders.
    private static final float SPAN = 14.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.4, 0.15, 0.4, EAR_R, EAR_L, EARS_BACK, BELL,
            BELL_SIDE, TAIL);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Moose moose, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        int id = moose.getId();
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.55, 0.85);
        float duty = Gait.duty(run, 0.65F, 0.45F);
        float phase = this.strides.phase(id, age, moose.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        float turn = Mth.clamp(Mth.wrapDegrees(moose.yBodyRot - moose.yBodyRotO), -12.0F, 12.0F);
        c[ROLL] -= 0.01F * turn * a;
        c[NECK_YAW] += 0.02F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.1F * Mth.sin(age * 0.07F);
        c[EAR_R] += 0.4F * Channels.twitch(age, id, 97, 6.0F);
        c[EAR_L] += 0.4F * Channels.twitch(age, id + 2, 131, 6.0F);
        c[EARS_BACK] += 0.4F * Channels.twitch(age, id + 4, 211, 12.0F);
        c[TAIL] += 0.5F * Channels.twitch(age, id + 6, 89, 8.0F) * Mth.sin(age * 1.2F);
        c[BELL] += 0.05F * Mth.sin(age * 0.13F);
        c[JAW] += 0.12F * Channels.twitch(age, id + 8, 380, 140.0F) * Math.max(0.0F, Mth.sin(age * 0.55F));
        for (int kind = Moose.BROWSE; kind <= Moose.STOMP; kind++) {
            float w = moose.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) moose.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, moose.level().getGameTime());
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
                float swing = on * Mth.lerp(run, 0.32F, 0.55F) * Gait.swing(p, duty);
                // A moose lifts its forefeet high, folding the knee far.
                float knee = Mth.lerp(run, 1.6F, 1.8F) * fold;
                c[leg] = swing;
                c[leg + 2] = knee;
                c[leg + 4] = 0.7F * toe - swing - knee;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            } else {
                float swing = on * Mth.lerp(run, 0.3F, 0.5F) * Gait.swing(p, duty);
                float stifle = Mth.lerp(run, 0.5F, 0.7F) * fold;
                float hock = -Mth.lerp(run, 1.0F, 1.25F) * fold;
                c[leg] = swing;
                c[leg + 2] = stifle;
                c[leg + 3] = hock;
                c[leg + 4] = 0.6F * toe - swing - stifle - hock;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            }
            load[k] = Gait.load(p, duty);
        }
        // The hips as high as the hind legs stand, the body pitching to where the forelegs hold the shoulders.
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = hind - 0.8F * run * flight;
        c[PITCH] = (fore - hind) / SPAN;
        c[ROLL] = 0.04F * walk * (load[0] + load[2] - load[1] - load[3]);
        // The head nodding down as each forefoot takes the weight; held out level at the trot.
        c[NECK] = 0.08F * walk * (load[0] + load[1] - 1.0F) - 0.15F * run;
        c[HEAD] = 0.06F * walk * (load[0] + load[1] - 1.0F) - 0.15F * run - c[PITCH];
        c[BELL] = 0.25F * a * (load[0] + load[1] - 1.0F);
        c[BELL_SIDE] = 0.15F * a * (load[0] - load[1]);
        c[EARS_BACK] = 0.6F * run;
        c[TAIL] = 0.2F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Moose.BROWSE -> {
                // Head reached up into the leaves, lips pulling at them, jaw working.
                float pull = Mth.sin(t * 0.5F);
                c[NECK] = -0.7F + 0.05F * pull;
                c[NECK_YAW] = 0.2F * Mth.sin(t * 0.045F);
                c[HEAD] = -0.55F + 0.1F * pull;
                c[JAW] = 0.2F * Math.max(0.0F, Mth.sin(t * 0.8F));
                c[BELL] = 0.1F * pull;
                c[EAR_R] = c[EAR_L] = -0.2F;
                c[LOOK] = 0.3F;
            }
            case Moose.GRAZE -> {
                // Too tall to reach the ground standing: forelegs spread, one forward and one back, front let down.
                float up = Channels.twitch(t, id, 110, 36.0F);
                float down = 1.0F - up;
                c[PITCH] = 0.2F * down;
                c[NECK] = 1.15F * down - 0.1F * up;
                c[NECK_YAW] = 0.12F * Mth.sin(t * 0.05F);
                c[HEAD] = 1.05F * down;
                c[JAW] = 0.2F * Math.max(0.0F, Mth.sin(t * 0.8F));
                stand(c, RF, -0.45F * down, 0.35F * down, 0.0F, 0.0F, 0.2F * down);
                stand(c, LF, 0.45F * down, 0.35F * down, 0.0F, 0.0F, -0.2F * down);
                c[LOOK] = 0.8F * up;
            }
            case Moose.BED -> {
                c[SINK] = 14.5F;
                c[PITCH] = c[ROLL] = 0.0F;
                c[BOB] = 0.15F * Mth.sin(t * 0.06F);
                // Forelegs folded at the knee under the chest; hind legs folded beside the body.
                stand(c, RF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, LF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, RH, -1.25F, 0.4F, 2.2F, -2.45F, 0.3F);
                stand(c, LH, -1.25F, 0.4F, 2.2F, -2.45F, 0.3F);
                c[NECK] = -0.3F;
                c[HEAD] = 0.05F;
                c[JAW] = 0.14F * Math.max(0.0F, Mth.sin(t * 0.5F));
                c[TAIL] = 0.0F;
                c[LOOK] = 0.7F;
            }
            case Moose.SHAKE -> {
                float on = Trunk.in(t, 0.0, 5.0) * (1.0F - Trunk.in(t, Moose.SHAKE_END - 8, Moose.SHAKE_END));
                float shake = on * Mth.sin(t * 1.9F);
                c[ROLL] = 0.1F * shake;
                c[NECK] = 0.25F * on;
                c[HEAD_ROLL] = -0.5F * on * Mth.sin(t * 1.9F - 0.8F);
                c[EAR_R] = 0.5F * shake;
                c[EAR_L] = -0.5F * shake;
                c[BELL_SIDE] = 0.6F * on * Mth.sin(t * 1.9F - 1.6F);
                c[TAIL] = 0.5F * shake;
                stand(c, RF, 0.0F, 0.12F * on, 0.0F, 0.0F, 0.0F);
                stand(c, LF, 0.0F, 0.12F * on, 0.0F, 0.0F, 0.0F);
                c[LOOK] = 0.0F;
            }
            case Moose.THREAT -> {
                // Head low, ears flat back, and a forefoot pawing at the ground.
                float paw = Trunk.in(t, 6.0, 12.0) * (0.5F + 0.5F * Mth.sin(t * 0.55F));
                c[NECK] = 0.5F;
                c[HEAD] = 0.35F + 0.05F * Mth.sin(t * 0.9F);
                c[EARS_BACK] = 1.1F;
                c[EAR_R] = c[EAR_L] = 0.3F;
                c[PITCH] = 0.05F;
                stand(c, RF, -0.55F * paw, 0.0F, 1.0F * paw, 0.0F, 0.5F * paw);
                c[TAIL] = 0.4F;
                c[LOOK] = 1.0F;
            }
            case Moose.STOMP -> {
                float rear = Trunk.in(t, 0.0, 6.0);
                float strike = Trunk.in(t, 6.5, Moose.STOMP_HIT);
                float back = Trunk.in(t, 12.0, Moose.STOMP_END);
                float up = rear * (1.0F - strike);
                float down = strike * (1.0F - back);
                c[PITCH] = -0.5F * up + 0.08F * down;
                c[BOB] = 0.8F * down;
                // Forefeet drawn up under the chest, then thrown out and down.
                stand(c, RF, -0.8F * up - 0.35F * down, 0.05F, 1.7F * up + 0.1F * down, 0.0F, 0.4F * up);
                stand(c, LF, -0.95F * up - 0.25F * down, 0.05F, 1.5F * up + 0.1F * down, 0.0F, 0.4F * up);
                c[NECK] = -0.3F * up + 0.35F * down;
                c[HEAD] = 0.3F * up + 0.2F * down;
                c[EARS_BACK] = 1.0F;
                c[BELL] = 0.5F * down - 0.3F * up;
                c[LOOK] = 0.5F;
            }
            default -> {
            }
        }
        return p;
    }

    private static void stand(float[] c, int leg, float swing, float out, float knee, float hock, float hoof) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = knee;
        c[leg + 3] = hock;
        c[leg + 4] = hoof;
    }
}
