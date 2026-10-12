package nl.tivek.multiversepowers.creature.animal.bear.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the brown bear moves: a heavy, rolling walk in a bear's order of feet (hind, fore, other hind, other fore), its
// head low and swinging, breaking into a bounding gallop when it runs; paddling when it swims; and its acts on top:
// sitting back on its rump, rearing up to sniff the wind, lying down with its head on its paws, shaking itself dry,
// standing up to roar and swinging a paw down in a maul. Every leg's swing is told against the ground (BearModel takes
// off what the body's own lean already gives it).
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
    static final int LOOK = 16;
    // Per leg (right front, left front, right hind, left hind): swing, outward, middle joint, paw.
    static final int LEGS = 17;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final float PACE = 0.55F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(BrownBear bear, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        if (bear.isInWater()) {
            this.swim(c, age);
        } else {
            this.walk(c, limbSwing, Mth.clamp(amount, 0.0F, 1.0F));
        }
        // Breathing, a flick of either ear and a twitch of the tail now and then.
        c[BOB] += 0.12F * Mth.sin(age * 0.09F);
        c[EAR_R] += 0.35F * Channels.twitch(age, bear.getId(), 97, 5.0F);
        c[EAR_L] += 0.35F * Channels.twitch(age, bear.getId() + 3, 131, 5.0F);
        c[TAIL] += 0.08F * Mth.sin(age * 0.05F) + 0.3F * Channels.twitch(age, bear.getId() + 5, 173, 6.0F);
        for (int kind = BrownBear.SIT; kind <= BrownBear.MAUL; kind++) {
            float w = bear.blend(kind, partialTick);
            if (w > 0.0F) {
                float t = (float) bear.actAge(kind, partialTick);
                p.mix(this.act(kind, t, age), w);
            }
        }
        return p;
    }

    private void walk(float[] c, float limbSwing, float a) {
        float phase = limbSwing * PACE;
        float run = Trunk.in(a, 0.62, 0.92);
        float walk = a * (1.0F - run);
        // Walking, each foot a quarter of a stride after the last: left hind, left fore, right hind, right fore.
        float[] walkAt = { 1.5F * Mth.PI, 0.5F * Mth.PI, Mth.PI, 0.0F };
        // Galloping, the forefeet close together and the hind feet half a stride after them.
        float[] runAt = { 0.0F, 0.45F, Mth.PI, Mth.PI + 0.45F };
        int[] legs = { RF, LF, RH, LH };
        for (int k = 0; k < 4; k++) {
            boolean front = k < 2;
            float w = phase + walkAt[k];
            float g = phase + runAt[k];
            float up = Channels.lift(w) * walk + Channels.lift(g) * run * 1.3F;
            c[legs[k]] = 0.42F * walk * Channels.swing(w) + 0.85F * run * Channels.swing(g);
            c[legs[k] + 2] = (front ? 0.95F : 0.75F) * up;
            c[legs[k] + 3] = (front ? 0.7F : 0.5F) * up;
        }
        // The shoulders roll with each forefoot, the hump with them, the head swinging low.
        c[ROLL] = 0.07F * walk * Mth.sin(phase);
        c[HIND_ROLL] = -0.05F * walk * Mth.sin(phase + 0.5F * Mth.PI);
        c[YAW] = 0.03F * walk * Mth.sin(phase + 0.5F * Mth.PI);
        c[BOB] = 0.35F * walk * Mth.cos(2.0F * phase) - 1.1F * run * Math.abs(Mth.sin(phase));
        c[NECK] = 0.12F * a + 0.05F * walk * Mth.cos(2.0F * phase) - 0.12F * run * Mth.sin(phase);
        c[NECK_YAW] = 0.09F * walk * Mth.sin(phase + 0.5F * Mth.PI);
        // Running, the back bends and stretches with each bound.
        c[FLEX] = 0.3F * run * Mth.sin(phase + 0.5F * Mth.PI);
        c[PITCH] = -0.1F * run * Mth.sin(phase);
        c[EAR_R] = c[EAR_L] = 0.25F * run;
    }

    // Paddling with its head held up out of the water.
    private void swim(float[] c, float age) {
        float phase = age * 0.32F;
        float[] at = { 0.0F, Mth.PI, 0.5F * Mth.PI, 1.5F * Mth.PI };
        int[] legs = { RF, LF, RH, LH };
        for (int k = 0; k < 4; k++) {
            float p = phase + at[k];
            c[legs[k]] = 0.65F * Mth.sin(p) - 0.2F;
            c[legs[k] + 2] = 0.8F * Channels.lift(p);
            c[legs[k] + 3] = 0.4F * Channels.lift(p);
        }
        c[PITCH] = -0.18F;
        c[NECK] = -0.25F;
        c[ROLL] = 0.04F * Mth.sin(phase);
    }

    private Channels act(int kind, float t, float age) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case BrownBear.SIT -> {
                c[PITCH] = -0.78F;
                c[SINK] = 4.0F;
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
                c[SINK] = 6.6F;
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
