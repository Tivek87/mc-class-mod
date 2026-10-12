package nl.tivek.multiversepowers.creature.animal.deer.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;

// How the deer moves: a light walk, a foot at a time a quarter of a stride apart, its head nodding; bounding away when
// it runs, forefeet together and hind feet together, its back flexing, ears laid back and white tail raised; ears and
// tail flicking and its jaw chewing the cud while it stands; and its acts: grazing with its muzzle in the grass, standing
// alert with its ears forward and stamping a forefoot, grooming its flank and lying down with its legs folded under it.
final class DeerPoses {
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
    // How far out each ear lies (right, then left), and how far both tip forward.
    static final int EAR_R = 10;
    static final int EAR_L = 11;
    static final int EARS_TIP = 12;
    static final int TAIL = 13;
    static final int TAIL_SIDE = 14;
    static final int LOOK = 15;
    // Per leg (right front, left front, right hind, left hind): swing, outward, the joint below it (knee or stifle),
    // the hock (hind legs only), and the hoof.
    static final int LEGS = 16;
    static final int PER_LEG = 5;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final float PACE = 0.5F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Deer deer, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        this.walk(c, limbSwing, Mth.clamp(amount, 0.0F, 1.0F));
        int id = deer.getId();
        c[BOB] += 0.06F * Mth.sin(age * 0.1F);
        c[EAR_R] += 0.4F * Channels.twitch(age, id, 83, 4.0F);
        c[EAR_L] += 0.4F * Channels.twitch(age, id + 2, 109, 4.0F);
        c[EARS_TIP] -= 0.3F * Channels.twitch(age, id + 4, 151, 7.0F);
        c[TAIL_SIDE] += 0.35F * Channels.twitch(age, id + 6, 71, 6.0F) * Mth.sin(age * 1.5F);
        c[JAW] += 0.1F * Channels.twitch(age, id + 8, 420, 120.0F) * Math.max(0.0F, Mth.sin(age * 0.7F));
        for (int kind = Deer.GRAZE; kind <= Deer.BED; kind++) {
            float w = deer.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) deer.actAge(kind, partialTick), id), w);
            }
        }
        return p;
    }

    private void walk(float[] c, float limbSwing, float a) {
        float phase = limbSwing * PACE;
        float run = Trunk.in(a, 0.55, 0.85);
        float walk = a * (1.0F - run);
        float[] walkAt = { 1.5F * Mth.PI, 0.5F * Mth.PI, Mth.PI, 0.0F };
        // Bounding: the forefeet land together, the hind feet half a stride later.
        float[] runAt = { Mth.PI + 0.3F, Mth.PI, 0.3F, 0.0F };
        int[] legs = { RF, LF, RH, LH };
        for (int k = 0; k < 4; k++) {
            float w = phase + walkAt[k];
            float g = phase + runAt[k];
            float up = Channels.lift(w) * walk + Channels.lift(g) * run * 1.25F;
            int leg = legs[k];
            if (k < 2) {
                c[leg] = 0.38F * walk * Channels.swing(w) + 0.95F * run * Channels.swing(g);
                c[leg + 2] = 1.4F * up;
                c[leg + 4] = 0.4F * up;
            } else {
                c[leg] = 0.32F * walk * Channels.swing(w) + 0.85F * run * Channels.swing(g);
                c[leg + 2] = 0.45F * up;
                c[leg + 3] = -0.9F * up;
                c[leg + 4] = 0.5F * up;
            }
        }
        c[BOB] = 0.25F * walk * Mth.cos(2.0F * phase) - 1.8F * run * (0.5F + 0.5F * Mth.sin(phase + 0.5F * Mth.PI));
        c[PITCH] = 0.16F * run * Mth.sin(phase);
        c[ROLL] = 0.03F * walk * Mth.sin(phase);
        c[NECK] = 0.08F * walk * Mth.cos(2.0F * phase) + 0.35F * run;
        c[HEAD] = -0.15F * run;
        c[TAIL] = -2.0F * run;
        c[EARS_TIP] = -0.5F * run;
        c[EAR_R] = c[EAR_L] = -0.2F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Deer.GRAZE -> {
                // Now and then the head comes up to look round, still chewing.
                float up = Channels.twitch(t, id, 90, 32.0F);
                c[PITCH] = 0.1F * (1.0F - up);
                c[NECK] = 1.75F - 1.35F * up;
                c[NECK_YAW] = 0.15F * Mth.sin(t * 0.05F);
                c[HEAD] = 1.0F - 0.85F * up;
                c[JAW] = 0.18F * Math.max(0.0F, Mth.sin(t * 0.9F));
                stand(c, RF, -0.12F, 0.06F, 0.0F, 0.0F, 0.0F);
                stand(c, LF, -0.12F, 0.06F, 0.0F, 0.0F, 0.0F);
                c[LOOK] = 0.8F * up;
            }
            case Deer.ALERT -> {
                float lift = Trunk.in(t, 6.0, Deer.STAMP - 2.0) * (1.0F - Trunk.in(t, Deer.STAMP - 2.0, Deer.STAMP));
                c[NECK] = -0.4F;
                c[HEAD] = -0.1F;
                c[EAR_R] = c[EAR_L] = -0.55F;
                c[EARS_TIP] = 0.35F;
                c[TAIL] = -0.9F;
                c[BOB] -= 0.2F;
                stand(c, RF, -0.6F * lift, 0.0F, 1.2F * lift, 0.0F, 0.3F * lift);
                c[LOOK] = 1.0F;
            }
            case Deer.GROOM -> {
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                c[NECK] = 0.9F;
                c[NECK_YAW] = side * 1.25F;
                c[HEAD_YAW] = side * 0.8F;
                c[HEAD] = 0.7F + 0.12F * Mth.sin(t * 1.6F);
                c[HEAD_ROLL] = side * 0.3F;
                c[EARS_TIP] = -0.3F;
                c[LOOK] = 0.0F;
            }
            case Deer.BED -> {
                c[SINK] = 8.0F;
                c[PITCH] = c[ROLL] = 0.0F;
                c[BOB] = 0.12F * Mth.sin(t * 0.07F);
                // Forelegs folded at the knee under the chest; hind legs folded beside the body.
                stand(c, RF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, LF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, RH, -1.25F, 0.45F, 2.2F, -2.45F, 0.3F);
                stand(c, LH, -1.25F, 0.45F, 2.2F, -2.45F, 0.3F);
                c[NECK] = -0.15F;
                c[HEAD] = 0.05F;
                c[JAW] = 0.12F * Math.max(0.0F, Mth.sin(t * 0.6F));
                c[EAR_R] += 0.15F;
                c[EAR_L] += 0.15F;
                c[TAIL] = 0.0F;
                c[LOOK] = 0.7F;
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
