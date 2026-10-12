package nl.tivek.multiversepowers.creature.animal.hedgehog.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;

// How the hedgehog moves: a quick patter of short steps, a diagonal pair of feet at a time, its nose forever
// twitching; and its acts: rolled up in a ball, nose lifted to sniff the air, snuffling and rooting through the
// ground, and scratching its flank with a hind foot.
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
    // Per leg (right front, left front, right hind, left hind): swing, outward.
    static final int LEGS = 9;
    static final int PER_LEG = 2;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final float PACE = 2.4F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Hedgehog hedgehog, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float phase = limbSwing * PACE;
        c[LOOK] = 1.0F;
        c[RF] = c[LH] = 0.7F * a * Mth.sin(phase);
        c[LF] = c[RH] = -0.7F * a * Mth.sin(phase);
        c[BOB] = 0.15F * a * Mth.cos(2.0F * phase) + 0.05F * Mth.sin(age * 0.18F);
        c[ROLL] = 0.05F * a * Mth.sin(phase);
        int id = hedgehog.getId();
        // The nose never rests.
        c[HEAD] = 0.05F * Mth.sin(age * 1.5F) * (0.4F + Channels.twitch(age, id, 50, 22.0F));
        c[EARS] = 0.3F * Channels.twitch(age, id + 1, 97, 4.0F);
        for (int kind = Hedgehog.CURL; kind <= Hedgehog.SCRATCH; kind++) {
            float w = hedgehog.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) hedgehog.actAge(kind, partialTick), id), w);
            }
        }
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
