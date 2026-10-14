package nl.tivek.multiversepowers.creature.animal.owl.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the owl moves: perched upright and still but for a slow breath, a slow blink of its lids, its head snapping round
// to look one way and holding there before it snaps to the next, and now and then bobbing side to side to size up what
// it sees; waddling when it walks, each foot set down flat and kept there; in flight lying forward with its wings out,
// beating slow and deep, the outer wing following the inner, feet tucked back with their talons curled and tail
// spread; and its acts: asleep with its eyes shut, feathers fluffed and head sunk into its shoulders, hooting with its
// throat swelling at each note, turning its head far round to look behind it, and preening a wing. The whole pose
// follows on springs, the tail slower.
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
    // The head moved sideways (pixels), peering; each foot's turn at the end of its leg.
    static final int PEER = 18;
    static final int FOOT_R = 19;
    static final int FOOT_L = 20;
    private static final int COUNT = 21;
    private static final float SWEEP = 2.5F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.6, 0.2, 0.5, TAIL);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Owl owl, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float spread = Mth.lerp(partialTick, owl.oSpread, owl.spread);
        float flap = Mth.lerp(partialTick, owl.oFlap, owl.flap);
        float a = Mth.clamp(amount, 0.0F, 1.0F) * (1.0F - spread);
        int id = owl.getId();
        float perched = 1.0F - spread;
        c[LOOK] = 1.0F;
        c[SPREAD] = spread;
        c[PITCH] = 1.15F * spread;
        // The downstroke is quicker than the upstroke, and the outer wing lags behind the inner.
        c[BEAT] = 0.55F * Mth.sin(flap) + 0.1F;
        c[BEAT_OUTER] = 0.4F * Mth.sin(flap - 0.9F);
        c[BOB] = -0.6F * spread * Mth.cos(flap) + 0.05F * Mth.sin(age * 0.08F);
        c[TAIL] = 0.1F * spread;
        c[FOOT_R] = c[FOOT_L] = 0.8F * spread;
        // On foot it waddles from one leg to the other, rocking over the foot that is down.
        float duty = 0.6F;
        float phase = this.strides.phase(id, age, owl.level().getGameTime(), Strides.rate(a, duty, SWEEP));
        float on = Gait.smooth(0.0F, 0.25F, a);
        for (int s = 0; s < 2; s++) {
            float leg = phase - Mth.PI * s;
            float swing = 0.6F * on * Gait.swing(leg, duty);
            c[LEG_R + s] = swing;
            c[FOOT_R + s] += on * 0.8F * Gait.toe(leg, duty) - swing;
        }
        c[ROLL] = 0.14F * on * (Gait.load(phase, duty) - Gait.load(phase - Mth.PI, duty));
        c[BOB] -= 0.3F * on * Math.abs(Gait.lift(phase, duty) - Gait.lift(phase - Mth.PI, duty));
        // A slow blink now and then; the head snapping to a new way to look every so often and holding it.
        c[LIDS] = Channels.twitch(age, id, 73, 7.0F);
        int glance = Mth.floor((age + id * 37) / 46.0F);
        float pick = (float) ((glance * 0x9E3779B9L + id * 0x7F4A7C15L) >>> 8 & 0xFFFF) / 65535.0F;
        c[HEAD_YAW] = perched * (pick - 0.5F) * 1.6F;
        c[HEAD_ROLL] = 0.25F * Channels.twitch(age, id + 2, 190, 40.0F) * perched;
        // Peering: the head swaying side to side and bobbing to size up what it looks at.
        float peer = Channels.twitch(age, id + 5, 260, 36.0F) * perched;
        c[PEER] = 0.9F * peer * Mth.sin(age * 0.55F);
        c[HEAD_SINK] = 0.4F * peer * Mth.sin(age * 1.1F);
        for (int kind = Owl.SLEEP; kind <= Owl.PREEN; kind++) {
            float w = owl.blend(kind, partialTick) * perched;
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) owl.actAge(kind, partialTick), id), w);
            }
        }
        float lids = c[LIDS];
        this.springs.follow(id, c, age, owl.level().getGameTime());
        c[LIDS] = lids;
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        c[HEAD_ROLL] = 0.0F;
        c[HEAD_YAW] = 0.0F;
        c[PEER] = 0.0F;
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
                c[LIDS] = 0.7F * note;
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
