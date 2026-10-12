package nl.tivek.multiversepowers.creature.animal.raccoon.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;

// How the raccoon moves: a hunched, waddling walk, its rump swaying and its tail swinging the other way, bounding when
// it runs; its nose forever sniffing; and its acts: sitting up on its haunches to look round, rubbing its forepaws
// together, nosing and digging at the ground, and scratching behind its ear with a hind foot. Every leg's swing is told
// against the ground (RaccoonModel takes off what the body's own lean already gives it).
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
    // Per leg (right front, left front, right hind, left hind): swing, outward, paw.
    static final int LEGS = 14;
    static final int PER_LEG = 3;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final float PACE = 0.9F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Raccoon raccoon, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        this.walk(c, limbSwing, Mth.clamp(amount, 0.0F, 1.0F));
        int id = raccoon.getId();
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
        return p;
    }

    private void walk(float[] c, float limbSwing, float a) {
        float phase = limbSwing * PACE;
        float run = Trunk.in(a, 0.6, 0.9);
        float walk = a * (1.0F - run);
        float[] walkAt = { 1.5F * Mth.PI, 0.5F * Mth.PI, Mth.PI, 0.0F };
        float[] runAt = { 0.0F, 0.4F, Mth.PI, Mth.PI + 0.4F };
        int[] legs = { RF, LF, RH, LH };
        for (int k = 0; k < 4; k++) {
            float w = phase + walkAt[k];
            float g = phase + runAt[k];
            float up = Channels.lift(w) * walk + Channels.lift(g) * run * 1.2F;
            c[legs[k]] = 0.55F * walk * Channels.swing(w) + 0.95F * run * Channels.swing(g);
            c[legs[k] + 2] = 0.7F * up;
        }
        // The rump sways with each hind foot, the tail swinging against it.
        c[HIND_ROLL] = 0.1F * walk * Mth.sin(phase);
        c[ROLL] = -0.04F * walk * Mth.sin(phase);
        c[TAIL_SIDE] = -0.3F * walk * Mth.sin(phase);
        c[BOB] = 0.2F * walk * Mth.cos(2.0F * phase) - 0.9F * run * Math.abs(Mth.sin(phase));
        c[FLEX] = 0.3F * run * Mth.sin(phase + 0.5F * Mth.PI);
        c[PITCH] = -0.1F * run * Mth.sin(phase);
        c[HEAD] = 0.12F * walk + 0.04F * walk * Mth.cos(2.0F * phase);
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

    private static void stand(float[] c, int leg, float swing, float out, float paw) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = paw;
    }
}
