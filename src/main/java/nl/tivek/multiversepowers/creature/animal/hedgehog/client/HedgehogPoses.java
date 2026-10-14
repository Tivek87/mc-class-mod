package nl.tivek.multiversepowers.creature.animal.hedgehog.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;
import nl.tivek.multiversepowers.creature.client.PoseSprings;

// How the hedgehog moves: a quick patter of short steps, a diagonal pair of feet at a time, each paw set down and kept
// where it was set and flicked up as it lifts, the body bobbing and rocking over them; its nose forever twitching, its
// eyes blinking and its stub of a tail wagging as it scurries; and its acts: rolled up in a ball, nose lifted to sniff
// the air, snuffling and rooting through the ground, and scratching its flank with a hind foot. The whole pose follows
// on springs.
final class HedgehogPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int HEAD = 3;
    static final int HEAD_YAW = 4;
    static final int HEAD_ROLL = 5;
    static final int EARS = 6;
    static final int CURL = 7;
    static final int LOOK = 8;
    static final int TAIL = 9;
    static final int BLINK = 10;
    // Per leg (right front, left front, right hind, left hind): swing, outward, paw.
    static final int LEGS = 11;
    static final int PER_LEG = 3;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // A trot: right fore with left hind, then left fore with right hind half a stride on.
    private static final float[] AT = { 0.0F, 0.5F, 0.5F, 0.0F };
    private static final float SWEEP = 2.6F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 1.0, 0.3, 0.5, EARS, TAIL);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Hedgehog hedgehog, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float duty = 0.55F;
        float phase = this.strides.phase(hedgehog.getId(), age, hedgehog.level().getGameTime(),
                Strides.rate(a, duty, SWEEP));
        float on = Gait.smooth(0.0F, 0.25F, a);
        c[LOOK] = 1.0F;
        for (int k = 0; k < 4; k++) {
            float at = phase - Mth.TWO_PI * AT[k];
            float swing = 0.6F * on * Gait.swing(at, duty);
            c[FEET[k]] = swing;
            c[FEET[k] + 2] = on * 1.1F * Gait.toe(at, duty) - swing;
        }
        // Up as each pair of feet stands under it, rocking toward the side whose forefoot is down.
        float rf = Gait.load(phase, duty);
        float lf = Gait.load(phase - Mth.PI, duty);
        c[BOB] = -0.25F * on * Math.abs(rf - lf) + 0.05F * Mth.sin(age * 0.18F);
        c[ROLL] = 0.06F * on * (rf - lf);
        c[TAIL] = 0.4F * on * Mth.sin(phase * 2.0F);
        int id = hedgehog.getId();
        float turn = Mth.clamp(Mth.wrapDegrees(hedgehog.yBodyRot - hedgehog.yBodyRotO), -20.0F, 20.0F);
        c[HEAD_YAW] += 0.02F * turn;
        // The nose never rests.
        c[HEAD] = 0.05F * Mth.sin(age * 1.5F) * (0.4F + Channels.twitch(age, id, 50, 22.0F));
        c[EARS] = 0.3F * Channels.twitch(age, id + 1, 97, 4.0F);
        c[BLINK] = Channels.blink(age, id);
        for (int kind = Hedgehog.CURL; kind <= Hedgehog.SCRATCH; kind++) {
            float w = hedgehog.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) hedgehog.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, hedgehog.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Hedgehog.CURL -> {
                c[CURL] = 1.0F;
                c[PITCH] = c[ROLL] = c[HEAD] = c[HEAD_YAW] = 0.0F;
                c[RF] = c[LF] = -0.6F;
                c[RH] = c[LH] = 0.6F;
                c[BOB] = 0.0F;
                c[LOOK] = 0.0F;
                c[TAIL] = 0.0F;
                c[BLINK] = 1.0F;
            }
            case Hedgehog.SNIFF -> {
                // Front lifted a little, nose high, testing the air this way and that.
                c[PITCH] = -0.28F;
                c[BOB] = -0.4F;
                c[HEAD] = -0.55F + 0.09F * Mth.sin(t * 1.9F);
                c[HEAD_YAW] = 0.45F * Mth.sin(t * 0.09F);
                c[RF] = c[LF] = 0.15F;
                c[LOOK] = 0.3F;
            }
            case Hedgehog.FORAGE -> {
                float root = Mth.sin(t * 1.1F);
                c[PITCH] = 0.14F;
                c[HEAD] = 0.5F + 0.12F * root;
                c[HEAD_YAW] = 0.4F * Mth.sin(t * 0.08F);
                c[RF] = 0.3F * Math.max(0.0F, root);
                c[LF] = 0.3F * Math.max(0.0F, -root);
                c[LOOK] = 0.0F;
            }
            case Hedgehog.SCRATCH -> {
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                int leg = side > 0 ? RH : LH;
                c[ROLL] = -0.22F * side;
                c[leg] = -1.1F + 0.3F * Mth.sin(t * 2.6F);
                c[leg + 1] = 0.6F;
                c[leg + 2] = 0.6F;
                c[BLINK] = Math.max(c[BLINK], 0.6F);
                c[HEAD_ROLL] = 0.25F * side;
                c[HEAD_YAW] = 0.35F * side;
                c[LOOK] = 0.0F;
            }
            default -> {
            }
        }
        return p;
    }
}
