package nl.tivek.multiversepowers.creature.animal.monkey.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.monkey.Monkey;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the monkey moves: on all fours, a hand and the foot across from it together, its tail held up in an arch; a
// bounding gallop when it runs; its head cocking and its tail tip twitching while it stands; and its acts: sitting
// upright on its rump with its knees up, scratching its side with one hand, eating with both hands at its mouth, and
// standing up on its hind legs to hoot, arms thrown up and bouncing.
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
    static final int LOOK = 10;
    // Per limb (right arm, left arm, right leg, left leg): swing, outward, and its elbow or knee.
    static final int LIMBS = 11;
    static final int PER_LIMB = 3;
    private static final int COUNT = LIMBS + 4 * PER_LIMB;
    private static final int RA = LIMBS;
    private static final int LA = LIMBS + PER_LIMB;
    private static final int RL = LIMBS + 2 * PER_LIMB;
    private static final int LL = LIMBS + 3 * PER_LIMB;
    private static final float PACE = 1.2F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Monkey monkey, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        c[TAIL] = 1.0F;
        this.walk(c, limbSwing, Mth.clamp(amount, 0.0F, 1.0F));
        int id = monkey.getId();
        c[HEAD_ROLL] += 0.35F * Channels.twitch(age, id, 110, 24.0F);
        c[TAIL_WAG] += 0.4F * Channels.twitch(age, id + 2, 90, 12.0F) * Mth.sin(age * 1.1F);
        c[BOB] += 0.06F * Mth.sin(age * 0.1F);
        for (int kind = Monkey.SIT; kind <= Monkey.HOOT; kind++) {
            float w = monkey.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) monkey.actAge(kind, partialTick), id), w);
            }
        }
        return p;
    }

    private void walk(float[] c, float limbSwing, float a) {
        float phase = limbSwing * PACE;
        float run = Trunk.in(a, 0.6, 0.9);
        float walk = a * (1.0F - run);
        float[] walkAt = { 0.0F, Mth.PI, Mth.PI, 0.0F };
        // The gallop: both hands, then both feet.
        float[] runAt = { 0.0F, 0.5F, Mth.PI, Mth.PI + 0.5F };
        int[] limbs = { RA, LA, RL, LL };
        for (int k = 0; k < 4; k++) {
            float w = phase + walkAt[k];
            float g = phase + runAt[k];
            float up = Channels.lift(w) * walk + Channels.lift(g) * run * 1.3F;
            int limb = limbs[k];
            c[limb] = 0.6F * walk * Channels.swing(w) + 1.0F * run * Channels.swing(g);
            // An elbow folds forward of the arm, a knee back of the leg.
            c[limb + 2] = (k < 2 ? -0.9F : 0.9F) * up;
        }
        c[BOB] = 0.25F * walk * Mth.cos(2.0F * phase) + 0.8F * run * Mth.cos(phase);
        c[PITCH] = 0.15F * run * Mth.sin(phase);
        c[ROLL] = 0.05F * walk * Mth.sin(phase);
        c[TAIL_WAG] = 0.15F * walk * Mth.sin(phase);
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

    private static void limb(float[] c, int limb, float swing, float out, float joint) {
        c[limb] = swing;
        c[limb + 1] = out;
        c[limb + 2] = joint;
    }

    // One hoot starting at `from`.
    private static float note(float t, double from) {
        return Trunk.in(t, from, from + 1.5) * (1.0F - Trunk.in(t, from + 2.5, from + 5.0));
    }
}
