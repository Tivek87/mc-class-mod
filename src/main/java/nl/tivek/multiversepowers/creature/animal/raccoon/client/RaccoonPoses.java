package nl.tivek.multiversepowers.creature.animal.raccoon.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the raccoon moves: a hunched, waddling walk on flat feet, each paw set down and kept where it was set while the
// body goes over it, rolled off its heel and folded at the wrist or ankle as it lifts, its rump swaying and its tail
// swinging the other way in a wave down its length, bounding when it runs; leaning into its turns; its nose forever
// sniffing and its eyes blinking; and its acts: sitting up on its haunches to look round, rubbing its forepaws together,
// nosing and digging at the ground, and scratching behind its ear with a hind foot. Every leg's swing is told against
// the ground (RaccoonModel takes off what the body's own lean already gives it). The whole pose follows on springs, ears
// and tail slower and swinging past.
final class RaccoonPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int FLEX = 4;
    static final int HIND_ROLL = 5;
    static final int HEAD = 6;
    static final int HEAD_YAW = 7;
    static final int HEAD_ROLL = 8;
    static final int EARS = 9;
    static final int TAIL = 10;
    static final int TAIL_SIDE = 11;
    static final int TAIL_CURL = 12;
    static final int LOOK = 13;
    static final int TAIL_WAVE = 14;
    static final int BLINK = 15;
    // Per leg (right front, left front, right hind, left hind): swing, outward, middle joint, paw.
    static final int LEGS = 16;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down: walking left hind, left fore, right hind, right fore a quarter apart;
    // bounding the forefeet close together, the hind feet half a stride after them.
    private static final float[] WALK_AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.0F, 0.08F, 0.5F, 0.58F };
    private static final float WALK_SWEEP = 6.0F;
    private static final float RUN_SWEEP = 13.0F;
    private static final float LEG = 6.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.5, 0.2, 0.45, EARS, TAIL, TAIL_SIDE,
            TAIL_CURL, TAIL_WAVE);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Raccoon raccoon, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.6, 0.9);
        float duty = Gait.duty(run, 0.65F, 0.4F);
        float phase = this.strides.phase(raccoon.getId(), age, raccoon.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        int id = raccoon.getId();
        float turn = Mth.clamp(Mth.wrapDegrees(raccoon.yBodyRot - raccoon.yBodyRotO), -15.0F, 15.0F);
        c[ROLL] -= 0.012F * turn * a;
        c[HEAD_YAW] += 0.02F * turn;
        c[TAIL_SIDE] -= 0.04F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.08F * Mth.sin(age * 0.14F);
        c[HEAD] += 0.035F * Mth.sin(age * 1.3F) * Channels.twitch(age, id, 60, 25.0F);
        c[EARS] += 0.3F * Channels.twitch(age, id + 1, 89, 4.0F);
        c[TAIL_SIDE] += 0.12F * Mth.sin(age * 0.06F);
        for (int kind = Raccoon.SIT_UP; kind <= Raccoon.SCRATCH; kind++) {
            float w = raccoon.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) raccoon.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, raccoon.level().getGameTime());
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
            float swing = on * Mth.lerp(run, 0.5F, 0.95F) * Gait.swing(p, duty);
            float fold = on * Gait.fold(p, duty) * (front ? Mth.lerp(run, 0.9F, 1.3F) : Mth.lerp(run, 0.8F, 1.2F));
            c[leg] = swing;
            c[leg + 1] = 0.05F * on;
            c[leg + 2] = fold;
            // The paw kept flat under the leg's lean, folding only as it rolls off and lifts.
            c[leg + 3] = on * 0.9F * Gait.toe(p, duty) - swing - fold;
            load[k] = Gait.load(p, duty);
            drop[k] = LEG * (1.0F - Mth.cos(swing));
        }
        // RaccoonModel keeps the hips where the hind legs hold them: the body sinks by the hips' drop and its pitch
        // brings the shoulders down to theirs. The rump sways toward the hind foot that is down, the tail against it.
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = hind * (1.0F - run) - 0.4F * run * flight;
        c[PITCH] = (fore - hind) / 7.0F * (1.0F - run) - 0.1F * run * Mth.sin(phase - Mth.TWO_PI * 0.2F);
        c[HIND_ROLL] = 0.14F * walk * (load[2] - load[3]);
        c[ROLL] = -0.05F * walk * (load[2] - load[3]);
        c[TAIL_SIDE] = -0.3F * walk * (load[2] - load[3]);
        c[TAIL_WAVE] = 0.25F * walk * Mth.sin(phase - 1.4F);
        c[FLEX] = 0.3F * run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[HEAD] = 0.12F * walk + 0.05F * walk * (load[0] + load[1] - 1.0F) - 0.5F * c[PITCH];
        c[TAIL] = 0.25F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        c[ROLL] = c[HIND_ROLL] = c[FLEX] = 0.0F;
        switch (kind) {
            case Raccoon.SIT_UP -> {
                c[PITCH] = -1.25F;
                c[SINK] = 1.0F;
                // Haunches on the ground, forepaws held up before its chest, looking round.
                stand(c, RF, -0.4F, 0.1F, 0.8F);
                stand(c, LF, -0.4F, 0.1F, 0.8F);
                stand(c, RH, -1.3F, 0.3F, 1.3F);
                stand(c, LH, -1.3F, 0.3F, 1.3F);
                c[HEAD] = 0.05F;
                c[HEAD_YAW] = 0.5F * Mth.sin(t * 0.06F);
                c[TAIL] = 1.1F;
                c[TAIL_CURL] = 0.0F;
                c[LOOK] = 0.6F;
            }
            case Raccoon.WASH -> {
                float rub = Mth.sin(t * 0.9F);
                c[PITCH] = -0.9F;
                c[SINK] = 1.0F;
                // Forepaws together before it, rubbing one over the other; eyes on its paws.
                stand(c, RF, -0.95F + 0.18F * rub, -0.3F, 0.5F);
                stand(c, LF, -0.95F - 0.18F * rub, -0.3F, 0.5F);
                stand(c, RH, -1.1F, 0.3F, 1.1F);
                stand(c, LH, -1.1F, 0.3F, 1.1F);
                c[HEAD] = 0.55F + 0.04F * rub;
                c[TAIL] = 0.8F;
                c[LOOK] = 0.0F;
            }
            case Raccoon.FORAGE -> {
                float dig = Mth.sin(t * 0.8F);
                c[PITCH] = 0.15F;
                c[HEAD] = 0.75F + 0.06F * Mth.sin(t * 2.0F);
                c[HEAD_YAW] = 0.3F * Mth.sin(t * 0.07F);
                stand(c, RF, -0.35F + 0.3F * dig, 0.05F, 0.5F * Math.max(0.0F, dig));
                stand(c, LF, -0.35F - 0.3F * dig, 0.05F, 0.5F * Math.max(0.0F, -dig));
                c[TAIL] = 0.35F;
                c[LOOK] = 0.0F;
            }
            case Raccoon.SCRATCH -> {
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                int leg = side > 0 ? RH : LH;
                c[PITCH] = -0.45F;
                c[SINK] = 1.0F;
                stand(c, RH, -0.9F, 0.3F, 0.9F);
                stand(c, LH, -0.9F, 0.3F, 0.9F);
                // One hind foot up behind its ear, scratching fast.
                stand(c, leg, -1.6F + 0.25F * Mth.sin(t * 2.3F), 0.55F, 0.6F);
                c[HEAD_ROLL] = 0.35F * side;
                c[HEAD_YAW] = 0.3F * side;
                c[HEAD] = 0.15F;
                c[EARS] = 0.3F;
                c[LOOK] = 0.0F;
            }
            default -> {
            }
        }
        return p;
    }

    // A leg held still: its swing, how far out, and how far it bends at its middle joint, the paw turned back a little
    // against that bend to lie flatter.
    private static void stand(float[] c, int leg, float swing, float out, float bend) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = bend;
        c[leg + 3] = -0.35F * bend;
    }
}
