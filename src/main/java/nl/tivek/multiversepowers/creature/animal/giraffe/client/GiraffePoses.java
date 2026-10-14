package nl.tivek.multiversepowers.creature.animal.giraffe.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.giraffe.Giraffe;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the giraffe moves: a slow pacing walk, both legs of one side nearly together, the hind a little first, each hoof
// kept where it was set down while the body goes over it, the foreleg folding at the knee and the hind leg at the hock
// as it lifts, the body swaying toward the side that bears it and its long neck swinging forward and back with every
// step; a rocking gallop when it runs, hind feet then forefeet, neck pumping once a stride; the body leaning into its
// turns; ears flicking, eyes blinking, tail swishing and neck swaying while it stands; and its acts: browsing with its
// neck stretched up, its dark tongue
// curling out round the leaves and its head jerking them free; its head down to the ground, forelegs spread wide
// and its front let down between them; standing chewing, its jaw going from side to side; and the kick: weight thrown
// onto its forelegs, both hind hooves lashing out behind. The whole pose follows on springs, the neck, ears and tail
// slower and swinging past.
final class GiraffePoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    // The neck's three pieces, from the shoulders up.
    static final int NECK = 4;
    static final int NECK_YAW = 7;
    static final int HEAD = 8;
    static final int HEAD_YAW = 9;
    static final int HEAD_ROLL = 10;
    static final int JAW = 11;
    static final int JAW_SIDE = 12;
    // How far the tongue is out, 0 to 1.
    static final int TONGUE = 13;
    static final int EAR_R = 14;
    static final int EAR_L = 15;
    static final int TAIL = 16;
    static final int TAIL_SIDE = 17;
    static final int LOOK = 18;
    static final int BLINK = 19;
    // Per leg (right front, left front, right hind, left hind): swing, outward, the joint below it (knee or stifle),
    // the hock (hind legs only), and the hoof.
    static final int LEGS = 20;
    static final int PER_LEG = 5;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down: pacing, each hind foot just before the forefoot of its side; galloping
    // the hind feet close together, then the forefeet half a stride on.
    private static final float[] WALK_AT = { 0.08F, 0.58F, 0.0F, 0.5F };
    private static final float[] RUN_AT = { 0.55F, 0.45F, 0.0F, 0.08F };
    private static final float WALK_SWEEP = 17.0F;
    private static final float RUN_SWEEP = 34.0F;
    private static final float LEG = 29.0F;
    // From the hips to the shoulders.
    private static final float SPAN = 12.5F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.35, 0.16, 0.45, NECK, NECK + 1, NECK + 2,
            HEAD, EAR_R, EAR_L, TAIL, TAIL_SIDE);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Giraffe giraffe, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        int id = giraffe.getId();
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.55, 0.85);
        float duty = Gait.duty(run, 0.65F, 0.38F);
        float phase = this.strides.phase(id, age, giraffe.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        float turn = Mth.clamp(Mth.wrapDegrees(giraffe.yBodyRot - giraffe.yBodyRotO), -10.0F, 10.0F);
        c[ROLL] -= 0.008F * turn * a;
        c[NECK_YAW] += 0.04F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[NECK] += 0.02F * Mth.sin(age * 0.05F);
        c[NECK + 2] += 0.02F * Mth.sin(age * 0.05F + 1.0F);
        c[EAR_R] += 0.6F * Channels.twitch(age, id, 83, 5.0F);
        c[EAR_L] += 0.6F * Channels.twitch(age, id + 2, 109, 5.0F);
        float swish = Channels.twitch(age, id + 4, 70, 20.0F);
        c[TAIL_SIDE] += 0.7F * swish * Mth.sin(age * 0.6F);
        c[TAIL] += 0.15F * swish;
        for (int kind = Giraffe.BROWSE; kind <= Giraffe.KICK; kind++) {
            float w = giraffe.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) giraffe.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, giraffe.level().getGameTime());
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
                float swing = on * Mth.lerp(run, 0.3F, 0.6F) * Gait.swing(p, duty);
                float knee = Mth.lerp(run, 1.2F, 1.6F) * fold;
                c[leg] = swing;
                c[leg + 2] = knee;
                c[leg + 4] = 0.7F * toe - swing - knee;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            } else {
                float swing = on * Mth.lerp(run, 0.28F, 0.55F) * Gait.swing(p, duty);
                float stifle = Mth.lerp(run, 0.4F, 0.6F) * fold;
                float hock = -Mth.lerp(run, 0.85F, 1.2F) * fold;
                c[leg] = swing;
                c[leg + 1] = 0.12F * run;
                c[leg + 2] = stifle;
                c[leg + 3] = hock;
                c[leg + 4] = 0.6F * toe - swing - stifle - hock;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            }
            load[k] = Gait.load(p, duty);
        }
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = hind * (1.0F - run) - 1.4F * run * flight;
        c[PITCH] = (fore - hind) / SPAN * (1.0F - run) + 0.09F * run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        // Pacing, the body sways over the side whose legs bear it.
        c[ROLL] = 0.06F * walk * (load[0] + load[2] - load[1] - load[3]);
        // The neck goes forward as the weight comes onto a forefoot, and back again; at the gallop once a stride.
        float nod = 0.1F * walk * (load[0] + load[1] - 1.0F) + 0.2F * run * Mth.sin(phase + 1.2F);
        c[NECK] = nod + 0.12F * run;
        c[NECK + 1] = 0.5F * nod;
        c[NECK + 2] = -0.3F * nod;
        c[HEAD] = -0.4F * nod;
        c[TAIL] = 0.1F * walk + 0.7F * run;
        c[TAIL_SIDE] = 0.15F * walk * (load[2] - load[3]);
        c[EAR_R] = c[EAR_L] = -0.3F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Giraffe.BROWSE -> {
                // Neck stretched up, nose lifted; the tongue goes out and curls, the head jerks the leaves free, the
                // jaw works, and again.
                float at = t % 44.0F;
                float out = Trunk.in(at, 0.0, 8.0) * (1.0F - Trunk.in(at, 14.0, 19.0));
                float jerk = Trunk.in(at, 12.0, 15.0) * (1.0F - Trunk.in(at, 15.0, 22.0));
                float chew = Trunk.in(at, 18.0, 22.0) * (1.0F - Trunk.in(at, 40.0, 44.0));
                c[NECK] = -0.2F;
                c[NECK + 1] = -0.12F;
                c[NECK + 2] = -0.1F + 0.12F * jerk;
                c[NECK_YAW] = 0.15F * Mth.sin(t * 0.03F);
                c[HEAD] = -0.75F + 0.35F * jerk;
                c[TONGUE] = out;
                c[JAW] = 0.3F * out + 0.12F * chew * Math.max(0.0F, Mth.sin(at * 0.9F));
                c[JAW_SIDE] = 0.25F * chew * Mth.sin(at * 0.45F);
                c[EAR_R] -= 0.2F;
                c[EAR_L] -= 0.2F;
                c[LOOK] = 0.2F;
            }
            case Giraffe.DRINK -> {
                // Too tall to reach the ground standing: forelegs spread wide to both sides, its front let down
                // between them and its neck hung down before its chest. Now and then it throws its head up to look.
                float up = Channels.twitch(t, id, 120, 30.0F);
                float down = 1.0F - up;
                c[PITCH] = 0.6F * down;
                c[ROLL] = 0.0F;
                c[BOB] *= 0.3F;
                stand(c, RF, 0.0F, 0.75F * down, 0.0F, 0.0F, 0.0F);
                stand(c, LF, 0.0F, 0.75F * down, 0.0F, 0.0F, 0.0F);
                c[NECK] = 1.3F * down - 0.1F * up;
                c[NECK + 1] = 0.25F * down;
                c[NECK + 2] = 0.1F * down;
                c[NECK_YAW] = 0.0F;
                c[HEAD] = 1.2F * down;
                c[JAW] = 0.15F * down * Math.max(0.0F, Mth.sin(t * 0.7F));
                c[TONGUE] = 0.5F * down * Math.max(0.0F, Mth.sin(t * 0.7F));
                c[LOOK] = 0.8F * up;
            }
            case Giraffe.CHEW -> {
                c[NECK + 2] = -0.05F;
                c[HEAD] = -0.1F;
                c[HEAD_ROLL] = 0.06F * Mth.sin(t * 0.4F);
                c[JAW] = 0.1F + 0.06F * Mth.sin(t * 0.8F);
                c[JAW_SIDE] = 0.3F * Mth.sin(t * 0.4F);
                c[LOOK] = 0.8F;
            }
            case Giraffe.KICK -> {
                float load = Trunk.in(t, 0.0, 3.0);
                float lash = Trunk.in(t, 3.0, Giraffe.KICK_HIT);
                float back = Trunk.in(t, Giraffe.KICK_HIT + 2.0, Giraffe.KICK_END);
                float coil = load * (1.0F - lash);
                float out = lash * (1.0F - back);
                c[PITCH] = 0.12F * coil + 0.3F * out;
                c[SINK] = -3.0F * out;
                c[ROLL] = 0.0F;
                // Hind legs drawn up under the belly, then thrown straight out behind.
                stand(c, RH, -0.5F * coil + 1.3F * out, 0.08F, 0.6F * coil - 0.5F * out, -1.0F * coil + 0.3F * out,
                        0.3F * out);
                stand(c, LH, -0.5F * coil + 1.2F * out, 0.08F, 0.6F * coil - 0.5F * out, -1.0F * coil + 0.3F * out,
                        0.3F * out);
                stand(c, RF, 0.12F * out, 0.05F, 0.0F, 0.0F, 0.0F);
                stand(c, LF, 0.12F * out, 0.05F, 0.0F, 0.0F, 0.0F);
                c[NECK] = -0.25F * out;
                c[NECK + 1] = -0.1F * out;
                c[HEAD] = 0.2F * out;
                c[TAIL] = 1.0F * out;
                c[EAR_R] = c[EAR_L] = -0.5F;
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
