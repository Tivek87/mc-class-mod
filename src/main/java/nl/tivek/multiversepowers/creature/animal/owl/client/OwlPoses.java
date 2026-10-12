package nl.tivek.multiversepowers.creature.animal.owl.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the owl moves: perched upright and still but for a blink and a slow breath, waddling when it walks; in flight
// lying forward with its wings out, beating slow and deep, the outer wing following the inner, feet tucked back and
// tail spread; and its acts: asleep with its eyes shut, feathers fluffed and head sunk into its shoulders, hooting
// with its throat swelling at each note, turning its head far round to look behind it, and preening a wing.
final class OwlPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int PUFF = 3;
    static final int HEAD = 4;
    static final int HEAD_YAW = 5;
    static final int HEAD_ROLL = 6;
    static final int HEAD_SINK = 7;
    static final int LIDS = 8;
    static final int TAIL = 9;
    // How far its wings are out (0 folded, 1 in flight), their beat and the outer wing's own bend.
    static final int SPREAD = 10;
    static final int BEAT = 11;
    static final int BEAT_OUTER = 12;
    // A folded wing lifted off the body (right, then left).
    static final int WING_R = 13;
    static final int WING_L = 14;
    static final int LEG_R = 15;
    static final int LEG_L = 16;
    static final int LOOK = 17;
    private static final int COUNT = 18;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Owl owl, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float spread = Mth.lerp(partialTick, owl.oSpread, owl.spread);
        float flap = Mth.lerp(partialTick, owl.oFlap, owl.flap);
        float a = Mth.clamp(amount, 0.0F, 1.0F) * (1.0F - spread);
        int id = owl.getId();
        c[LOOK] = 1.0F;
        c[SPREAD] = spread;
        c[PITCH] = 1.15F * spread;
        // The downstroke is quicker than the upstroke, and the outer wing lags behind the inner.
        c[BEAT] = 0.55F * Mth.sin(flap) + 0.1F;
        c[BEAT_OUTER] = 0.4F * Mth.sin(flap - 0.9F);
        c[BOB] = -0.6F * spread * Mth.cos(flap) + 0.05F * Mth.sin(age * 0.08F);
        c[TAIL] = 0.1F * spread;
        // On foot it waddles from one leg to the other.
        c[LEG_R] = 0.55F * a * Mth.sin(limbSwing * 1.6F);
        c[LEG_L] = -0.55F * a * Mth.sin(limbSwing * 1.6F);
        c[ROLL] = 0.12F * a * Mth.sin(limbSwing * 1.6F);
        c[LIDS] = Channels.twitch(age, id, 73, 4.0F);
        c[HEAD_ROLL] = 0.25F * Channels.twitch(age, id + 2, 190, 40.0F) * (1.0F - spread);
        for (int kind = Owl.SLEEP; kind <= Owl.PREEN; kind++) {
            float w = owl.blend(kind, partialTick) * (1.0F - spread);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) owl.actAge(kind, partialTick), id), w);
            }
        }
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        c[HEAD_ROLL] = 0.0F;
        switch (kind) {
            case Owl.SLEEP -> {
                float breath = Mth.sin(t * 0.07F);
                c[PUFF] = 1.0F + 0.12F * breath;
                c[BOB] = 0.8F;
                c[HEAD] = 0.22F;
                c[HEAD_SINK] = 1.0F + 0.1F * breath;
                c[LIDS] = 1.0F;
                c[LEG_R] = c[LEG_L] = 0.0F;
                c[LOOK] = 0.0F;
            }
            case Owl.HOOT -> {
                // Hoo, then hu, hu-hooo: the throat swells and the body dips at each note.
                float note = note(t, 2.0, 11.0) + 0.7F * note(t, 19.0, 23.0) + 0.7F * note(t, 25.0, 29.0)
                        + note(t, 31.0, 44.0);
                c[PITCH] = 0.22F * note;
                c[PUFF] = 0.7F * note;
                c[HEAD] = -0.2F * note;
                c[BOB] = 0.4F * note;
                c[LIDS] = note > 0.6F ? 1.0F : 0.0F;
                c[LOOK] = 0.6F;
            }
            case Owl.TURN -> {
                // Round almost to its back one way, held, then round the other.
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                float first = Trunk.in(t, 0.0, 9.0) * (1.0F - Trunk.in(t, 30.0, 40.0));
                float second = Trunk.in(t, 40.0, 50.0);
                c[HEAD_YAW] = side * 2.5F * (first - second);
                c[HEAD_ROLL] = side * 0.3F * Trunk.in(t, 12.0, 18.0) * (1.0F - Trunk.in(t, 24.0, 30.0));
                c[LOOK] = 0.0F;
            }
            case Owl.PREEN -> {
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                float nibble = Mth.sin(t * 1.3F);
                c[HEAD_YAW] = side * 1.7F;
                c[HEAD] = 0.75F + 0.1F * nibble;
                c[HEAD_ROLL] = side * 0.2F;
                // The wing its head has turned to lifts off its side.
                c[side > 0 ? WING_R : WING_L] = -0.35F + 0.05F * nibble;
                c[PUFF] = 0.4F;
                c[LIDS] = 0.0F;
                c[LOOK] = 0.0F;
            }
            default -> {
            }
        }
        return p;
    }

    // A note sung from `from` to `to`: swelling in and dying away.
    private static float note(float t, double from, double to) {
        return Trunk.in(t, from, from + 2.5) * (1.0F - Trunk.in(t, to - 2.5, to));
    }
}
