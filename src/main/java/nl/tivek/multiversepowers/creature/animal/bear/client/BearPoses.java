package nl.tivek.multiversepowers.creature.animal.bear.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the brown bear moves: a heavy, rolling walk in a bear's order of feet (left hind, left fore, right hind, right
// fore), each paw set down flat and kept where it was set while the body goes over it, rolled up off its heel and
// folded as it lifts; the shoulders and hips dropping on the side whose foot is up, the head low and swinging, and
// leaning into its turns; breaking into a rotary gallop when it runs, the back bending and stretching with each bound;
// paddling when it swims; and its acts on top: sitting back on its rump, rearing up to sniff the wind, lying down with
// its head on its paws, shaking itself dry, standing up to roar and swinging a paw down in a maul. Every leg's swing is
// told against the ground (BearModel takes off what the body's own lean already gives it). The whole pose follows on
// springs, ears and tail slower and swinging past, and the eyes blink.
final class BearPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int YAW = 2;
    static final int BOB = 3;
    static final int SINK = 4;
    static final int FLEX = 5;
    static final int HIND_ROLL = 6;
    static final int NECK = 7;
    static final int NECK_YAW = 8;
    static final int HEAD = 9;
    static final int HEAD_YAW = 10;
    static final int HEAD_ROLL = 11;
    static final int JAW = 12;
    static final int EAR_R = 13;
    static final int EAR_L = 14;
    static final int TAIL = 15;
    static final int TAIL_YAW = 16;
    static final int LOOK = 17;
    static final int BLINK = 18;
    // Per leg (right front, left front, right hind, left hind): swing, outward, middle joint, paw.
    static final int LEGS = 19;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down, as a share of it: walking, left hind first, then left fore, right hind
    // and right fore; galloping, the hind feet close together and the forefeet half a stride after them.
    private static final float[] WALK_AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.55F, 0.45F, 0.0F, 0.1F };
    // How far a paw sweeps back under it (pixels) walking and running: a little longer than the legs show, so a
    // stride keeps pace with the ground.
    private static final float WALK_SWEEP = 11.0F;
    private static final float RUN_SWEEP = 22.0F;
    private static final float FORE_LEG = 10.0F;
    private static final float HIND_LEG = 9.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.45, 0.16, 0.45, EAR_R, EAR_L, TAIL,
            TAIL_YAW, HEAD_ROLL);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(BrownBear bear, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.62, 0.92);
        float duty = Gait.duty(run, 0.68F, 0.4F);
        float rate = bear.isInWater() ? 0.3F : Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP));
        float phase = this.strides.phase(bear.getId(), age, bear.level().getGameTime(), rate);
        if (bear.isInWater()) {
            this.swim(c, phase);
        } else {
            this.walk(c, phase, a, run, duty);
        }
        // Leaning into a turn, the head leading it.
        float turn = Mth.clamp(Mth.wrapDegrees(bear.yBodyRot - bear.yBodyRotO), -15.0F, 15.0F);
        c[ROLL] -= 0.012F * turn * a;
        c[NECK_YAW] += 0.02F * turn;
        c[TAIL_YAW] -= 0.03F * turn;
        // Breathing, a flick of either ear and a twitch of the tail now and then.
        c[BOB] += 0.12F * Mth.sin(age * 0.09F);
        c[EAR_R] += 0.35F * Channels.twitch(age, bear.getId(), 97, 5.0F);
        c[EAR_L] += 0.35F * Channels.twitch(age, bear.getId() + 3, 131, 5.0F);
        c[TAIL] += 0.08F * Mth.sin(age * 0.05F) + 0.3F * Channels.twitch(age, bear.getId() + 5, 173, 6.0F);
        c[BLINK] = Channels.blink(age, bear.getId());
        for (int kind = BrownBear.SIT; kind <= BrownBear.MAUL; kind++) {
            float w = bear.blend(kind, partialTick);
            if (w > 0.0F) {
                float t = (float) bear.actAge(kind, partialTick);
                p.mix(this.act(kind, t, age), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(bear.getId(), c, age, bear.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private void walk(float[] c, float phase, float a, float run, float duty) {
        float walk = a * (1.0F - run);
        float on = Gait.smooth(0.0F, 0.3F, a);
        float[] load = new float[4];
        float[] drop = new float[4];
        for (int k = 0; k < 4; k++) {
            boolean front = k < 2;
            int leg = FEET[k];
            float p = phase - Mth.TWO_PI * Mth.lerp(run, WALK_AT[k], RUN_AT[k]);
            float reach = on * (front ? Mth.lerp(run, 0.36F, 0.75F) : Mth.lerp(run, 0.32F, 0.7F));
            float swing = reach * Gait.swing(p, duty);
            float fold = on * Gait.fold(p, duty) * (front ? Mth.lerp(run, 0.85F, 1.3F) : Mth.lerp(run, 0.7F, 1.1F));
            c[leg] = swing;
            c[leg + 1] = 0.04F * on;
            c[leg + 2] = fold;
            // The paw kept flat on the ground under the leg's lean, folding only as the foot rolls off and lifts.
            c[leg + 3] = on * (front ? 1.1F : 0.8F) * Gait.toe(p, duty) - swing - fold;
            load[k] = Gait.load(p, duty);
            drop[k] = (front ? FORE_LEG : HIND_LEG) * (1.0F - Mth.cos(swing));
        }
        // The shoulders sit as high as the legs under them stand, dropping on the side whose foot is up, and the hips
        // with them; the hump rolls with every step.
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        // BearModel keeps the hips where the hind legs hold them, so the body sinks by the hips' drop and its pitch
        // brings the shoulders down to theirs.
        c[BOB] = hind * (1.0F - run) - 0.9F * run * (1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]));
        c[PITCH] = (fore - hind) / 15.0F * (1.0F - run);
        c[ROLL] = 0.09F * walk * (load[0] - load[1]);
        c[HIND_ROLL] = 0.08F * walk * (load[2] - load[3]) - c[ROLL];
        c[YAW] = 0.04F * walk * (load[0] - load[1]);
        // The head low and swinging toward the forefoot that is down.
        c[NECK] = 0.12F * a + 0.06F * walk * (load[0] + load[1] - 1.0F);
        c[NECK_YAW] = 0.1F * walk * (load[1] - load[0]);
        c[HEAD] = -0.6F * c[PITCH];
        // Galloping, the back bends and stretches with each bound and the head is held steady against it.
        float bound = run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[FLEX] = 0.22F * bound;
        c[PITCH] += -0.12F * run * Mth.sin(phase - Mth.TWO_PI * 0.05F);
        c[NECK] += -0.15F * bound;
        c[HEAD] += 0.12F * run * Mth.sin(phase - Mth.TWO_PI * 0.05F);
        c[EAR_R] += 0.25F * run;
        c[EAR_L] += 0.25F * run;
        c[TAIL] += 0.25F * run;
    }

    // Paddling with its head held up out of the water, each leg pulling back folded and reaching forward again.
    private void swim(float[] c, float phase) {
        float[] at = { 0.0F, 0.5F, 0.25F, 0.75F };
        for (int k = 0; k < 4; k++) {
            int leg = FEET[k];
            float p = phase - Mth.TWO_PI * at[k];
            c[leg] = 0.65F * Gait.swing(p, 0.5F) - 0.2F;
            c[leg + 2] = 0.8F * Gait.fold(p, 0.5F);
            c[leg + 3] = 0.5F * Gait.toe(p, 0.5F);
        }
        c[PITCH] = -0.18F;
        c[NECK] = -0.25F;
        c[HEAD] = 0.15F;
        c[ROLL] = 0.04F * Mth.sin(phase);
    }

    private Channels act(int kind, float t, float age) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case BrownBear.SIT -> {
                c[PITCH] = -0.78F;
                c[SINK] = 5.5F;
                c[FLEX] = 0.12F;
                c[BOB] = 0.12F * Mth.sin(age * 0.09F);
                c[ROLL] = c[HIND_ROLL] = c[YAW] = 0.0F;
                stand(c, RF, -0.08F, 0.0F, 0.0F, 0.0F);
                stand(c, LF, -0.08F, 0.0F, 0.0F, 0.0F);
                // The hind legs folded forward under it, thighs along the ground, soles flat.
                stand(c, RH, -1.45F, 0.3F, 1.5F, -0.05F);
                stand(c, LH, -1.45F, 0.3F, 1.5F, -0.05F);
                c[NECK] = 0.5F;
                c[NECK_YAW] = 0.3F * Mth.sin(t * 0.025F);
                c[HEAD] = 0.08F;
            }
            case BrownBear.REAR -> {
                float rise = Trunk.in(t, 0.0, 10.0);
                c[PITCH] = -1.3F * rise;
                c[FLEX] = 0.12F * rise;
                c[ROLL] = c[HIND_ROLL] = c[YAW] = 0.0F;
                c[BOB] = 0.1F * Mth.sin(age * 0.11F);
                // Forepaws hanging limp before its chest.
                stand(c, RF, -0.3F, 0.08F, 0.6F, 0.7F);
                stand(c, LF, -0.3F, 0.08F, 0.6F, 0.7F);
                stand(c, RH, 0.05F, 0.08F, 0.2F, -0.2F);
                stand(c, LH, 0.05F, 0.08F, 0.2F, -0.2F);
                // Nose up into the wind, sniffing, turning slowly one way and the other.
                c[NECK] = 0.95F * rise;
                c[NECK_YAW] = 0.4F * Mth.sin(t * 0.07F);
                c[HEAD] = -0.25F + 0.05F * Mth.sin(t * 1.4F);
                c[EAR_R] = c[EAR_L] = -0.15F;
                c[LOOK] = 0.3F;
            }
            case BrownBear.REST -> {
                c[SINK] = 7.8F;
                c[PITCH] = c[FLEX] = c[ROLL] = c[HIND_ROLL] = c[YAW] = 0.0F;
                c[BOB] = 0.25F * Mth.sin(age * 0.055F);
                // Forelegs stretched out before it, hind legs out behind.
                stand(c, RF, -1.5F, 0.12F, 0.05F, 0.1F);
                stand(c, LF, -1.5F, 0.12F, 0.05F, 0.1F);
                stand(c, RH, 1.45F, 0.3F, 0.1F, 0.25F);
                stand(c, LH, 1.45F, 0.3F, 0.1F, 0.25F);
                // Its chin on its paws, now and then lifting its head to look round.
                float looks = Channels.twitch(t, 0, 260, 60.0F);
                c[NECK] = 0.55F - 0.45F * looks;
                c[NECK_YAW] = 0.2F * looks * Mth.sin(t * 0.08F);
                c[HEAD] = 0.15F;
                c[EAR_R] += 0.15F;
                c[EAR_L] += 0.15F;
                c[TAIL] = 0.0F;
                c[LOOK] = 0.4F * looks;
                // Dozing, its eyes open only while it looks round.
                c[BLINK] = Math.max(c[BLINK], 0.85F * (1.0F - looks));
            }
            case BrownBear.SHAKE -> {
                float on = Trunk.in(t, 0.0, 5.0) * (1.0F - Trunk.in(t, BrownBear.SHAKE_END - 8, BrownBear.SHAKE_END));
                float s = t * 2.3F;
                c[ROLL] = 0.38F * on * Mth.sin(s);
                c[HIND_ROLL] = 0.3F * on * Mth.sin(s - 0.9F);
                c[NECK_YAW] = 0.25F * on * Mth.sin(s + 0.6F);
                c[HEAD_ROLL] = -0.3F * on * Mth.sin(s);
                c[EAR_R] = 0.5F * on * Mth.sin(s + 1.0F);
                c[EAR_L] = -c[EAR_R];
                c[BOB] = 0.5F * on;
                for (int leg : new int[] { RF, LF, RH, LH }) {
                    c[leg + 1] = 0.15F * on;
                }
                c[TAIL] = 0.5F * on * Mth.sin(s + 2.0F);
                c[JAW] = 0.25F * on;
                c[BLINK] = Math.max(c[BLINK], 0.7F * on);
            }
            case BrownBear.ROAR -> {
                float rise = Trunk.in(t, 0.0, 8.0) * (1.0F - Trunk.in(t, 22.0, BrownBear.ROAR_END));
                float open = Trunk.in(t, 5.0, 9.0) * (1.0F - Trunk.in(t, 20.0, 26.0));
                c[PITCH] = -1.0F * rise;
                c[FLEX] = 0.1F * rise;
                stand(c, RF, -0.65F * rise, 0.15F * rise, 0.7F * rise, 0.5F * rise);
                stand(c, LF, -0.65F * rise, 0.15F * rise, 0.7F * rise, 0.5F * rise);
                stand(c, RH, 0.0F, 0.12F * rise, 0.15F, -0.15F);
                stand(c, LH, 0.0F, 0.12F * rise, 0.15F, -0.15F);
                c[NECK] = 0.55F * rise;
                c[HEAD] = -0.4F * open;
                c[JAW] = 0.95F * open;
                c[HEAD_ROLL] = 0.12F * open * Mth.sin(t * 1.7F);
                c[HEAD_YAW] = 0.1F * open * Mth.sin(t * 1.1F);
                c[EAR_R] = c[EAR_L] = 0.4F * open;
                c[LOOK] = 0.5F;
                c[BLINK] = 0.0F;
            }
            case BrownBear.MAUL -> {
                float raise = Trunk.in(t, 0.0, 6.0);
                float strike = Trunk.in(t, 6.0, BrownBear.MAUL_HIT + 1.0F);
                float back = Trunk.in(t, 11.0, BrownBear.MAUL_END);
                float up = raise * (1.0F - strike);
                float down = strike * (1.0F - back);
                c[PITCH] = -0.55F * up + 0.12F * down;
                c[FLEX] = 0.1F * up;
                // The right paw lifted high and out, then raked down before it.
                stand(c, RF, -2.1F * up + 0.35F * down, 0.35F * up, 0.9F * up + 0.2F * down, 0.6F * up);
                stand(c, LF, -0.3F * up, 0.1F * up, 0.4F * up, 0.3F * up);
                c[NECK] = c[NECK] + 0.4F * up - 0.25F * down;
                c[HEAD] = -0.3F * up + 0.3F * down;
                c[JAW] = 0.85F * Trunk.in(t, 3.0, 7.0) * (1.0F - Trunk.in(t, 10.0, 16.0));
                c[ROLL] = 0.12F * up - 0.1F * down;
                c[EAR_R] = c[EAR_L] = 0.45F;
                c[LOOK] = 0.6F;
            }
            default -> {
            }
        }
        return p;
    }

    private static void stand(float[] c, int leg, float swing, float out, float middle, float paw) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = middle;
        c[leg + 3] = paw;
    }
}
