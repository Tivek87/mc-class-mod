package nl.tivek.multiversepowers.creature.animal.crab.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the crab moves: scuttling, its legs lifting and reaching in two sets of four by turns; eye stalks twitching and
// claws working a little while it stands; and its acts: picking at the ground with one claw and then the other and
// bringing each to its mouth, waving a raised claw, rearing up with both claws spread and snapping, digging itself
// into the sand with a shuffle until only its eyes and the top of its shell show, and the pinch: a claw thrown
// forward open and shut on what it reaches.
final class CrabPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int LUNGE = 4;
    // The eye stalks: leaning apart, both to one side, and laid back.
    static final int EYES = 5;
    static final int EYES_SIDE = 6;
    static final int EYES_BACK = 7;
    // Per arm (right, left): further out, lifted (negative) or dipped, the claw turned further in, its finger open.
    static final int ARMS = 8;
    static final int PER_ARM = 4;
    // Per leg (right legs front to back, then left): its knee lifted, its foot reaching out.
    static final int LEGS = ARMS + 2 * PER_ARM;
    static final int PER_LEG = 2;
    private static final int COUNT = LEGS + 8 * PER_LEG;
    private static final int RA = ARMS;
    private static final int LA = ARMS + PER_ARM;
    private static final float PACE = 2.2F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Crab crab, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float a = Mth.clamp(amount * 1.5F, 0.0F, 1.0F);
        float phase = limbSwing * PACE;
        int id = crab.getId();
        for (int i = 0; i < 8; i++) {
            // Neighbours step by turns, and the two sides by turns.
            float at = phase + ((i % 4 + i / 4) % 2 == 0 ? 0.0F : Mth.PI);
            c[LEGS + i * PER_LEG] = 0.4F * a * Math.max(0.0F, Mth.sin(at));
            c[LEGS + i * PER_LEG + 1] = 0.35F * a * Mth.cos(at);
        }
        c[BOB] = 0.2F * a * Mth.sin(2.0F * phase) + 0.05F * Mth.sin(age * 0.11F);
        c[ROLL] = 0.04F * a * Mth.sin(phase);
        c[EYES] = 0.12F + 0.25F * Channels.twitch(age, id, 61, 5.0F);
        c[EYES_SIDE] = 0.3F * Channels.twitch(age, id + 4, 113, 14.0F) * Mth.sin(age * 0.5F);
        c[RA + 3] = 0.35F * Channels.twitch(age, id + 1, 87, 10.0F);
        c[LA + 3] = 0.35F * Channels.twitch(age, id + 2, 131, 10.0F);
        for (int kind = Crab.WAVE; kind <= Crab.PINCH; kind++) {
            float w = crab.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) crab.actAge(kind, partialTick), id), w);
            }
        }
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        int near = id % 2 == 0 ? RA : LA;
        switch (kind) {
            case Crab.WAVE -> {
                float wave = Mth.sin(t * 0.45F);
                arm(c, near, 0.7F + 0.3F * wave, -1.25F, -0.7F + 0.3F * wave, 0.7F);
                c[ROLL] = (near == RA ? -0.08F : 0.08F);
                c[EYES] = 0.3F;
            }
            case Crab.SNAP -> {
                // Reared back, both claws spread wide and high, shutting with a snap every half second.
                float open = 1.0F - Trunk.in(t % 9.0F, 5.0, 7.0) + Trunk.in(t % 9.0F, 7.5, 9.0);
                c[PITCH] = -0.32F;
                c[BOB] = -0.6F;
                arm(c, RA, 0.6F, -0.95F, -0.75F, 0.85F * open);
                arm(c, LA, 0.6F, -0.95F, -0.75F, 0.85F * open);
                c[EYES] = 0.35F;
            }
            case Crab.BURROW -> {
                float shuffle = 1.0F - Trunk.in(t, 14.0, 26.0);
                c[SINK] = 2.6F * Trunk.in(t, 0.0, 22.0);
                c[ROLL] = 0.16F * shuffle * Mth.sin(t * 1.4F);
                c[BOB] = 0.0F;
                arm(c, RA, -0.35F, 0.25F, 0.35F, 0.0F);
                arm(c, LA, -0.35F, 0.25F, 0.35F, 0.0F);
                for (int i = 0; i < 8; i++) {
                    c[LEGS + i * PER_LEG] = 0.35F + 0.2F * shuffle * Mth.sin(t * 1.4F + i);
                    c[LEGS + i * PER_LEG + 1] = -0.3F;
                }
                c[EYES] = 0.05F + 0.2F * Channels.twitch(t, id, 70, 8.0F);
            }
            case Crab.FEED -> {
                // One claw down to the ground while the other is at its mouth, and back.
                float turn = Mth.sin(t * 0.32F);
                float right = Math.max(0.0F, turn);
                float left = Math.max(0.0F, -turn);
                arm(c, RA, -0.2F * left, 0.55F * right - 0.1F * left, 0.45F * left - 0.3F * right, 0.6F * right);
                arm(c, LA, -0.2F * right, 0.55F * left - 0.1F * right, 0.45F * right - 0.3F * left, 0.6F * left);
                c[PITCH] = 0.08F;
                c[EYES_BACK] = -0.15F;
            }
            case Crab.PINCH -> {
                float reach = Trunk.in(t, 0.0, Crab.PINCH_HIT) * (1.0F - Trunk.in(t, Crab.PINCH_HIT + 2.0,
                        Crab.PINCH_END));
                float shut = Trunk.in(t, Crab.PINCH_HIT - 1.0, Crab.PINCH_HIT + 0.5);
                c[LUNGE] = -2.5F * reach;
                c[PITCH] = 0.12F * reach;
                arm(c, near, -0.3F * reach, -0.25F * reach, -0.85F * reach, 0.95F * (1.0F - shut));
            }
            default -> {
            }
        }
        return p;
    }

    private static void arm(float[] c, int arm, float out, float lift, float in, float grip) {
        c[arm] = out;
        c[arm + 1] = lift;
        c[arm + 2] = in;
        c[arm + 3] = grip;
    }
}
