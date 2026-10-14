package nl.tivek.multiversepowers.creature.animal.crab.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the crab moves: scuttling sideways, its legs in two sets of four stepping by turns (the first and third of one
// side with the second and fourth of the other), each set a little ahead of the next from front to back; a foot set
// down on the side it goes toward is reached out and pulls the body over, one on the other side is set down close
// and pushes it away, and each lifts and tucks its tip as it swings; the shell dips a little whenever a set is in the
// air. Standing, its legs shift now and then, its eye stalks twitch, its mouthparts flutter and its claws work a
// little. Its acts: picking at the ground with one claw and then the other and bringing each to its mouth, waving a
// raised claw, rearing up with both claws spread and snapping, digging itself into the sand with a shuffle until only
// its eyes and the top of its shell show, and the pinch: a claw thrown forward open and shut on what it reaches. The
// whole pose follows on springs, the eye stalks loosest.
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
    static final int MOUTH = 8;
    // Per arm (right, left): further out, lifted (negative) or dipped, folded further in at the wrist, its finger
    // open, the hand rolled.
    static final int ARMS = 9;
    static final int PER_ARM = 5;
    // Per leg (right legs front to back, then left): its knee lifted, its lower leg opened out, its tip opened out.
    static final int LEGS = ARMS + 2 * PER_ARM;
    static final int PER_LEG = 3;
    private static final int COUNT = LEGS + 8 * PER_LEG;
    private static final int RA = ARMS;
    private static final int LA = ARMS + PER_ARM;
    // How far a foot travels across the ground in a stride, in pixels.
    private static final float SWEEP = 2.7F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 1.2, 0.35, 0.45, EYES, EYES_SIDE, EYES_BACK);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Crab crab, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        int id = crab.getId();
        float a = Mth.clamp(amount * 1.5F, 0.0F, 1.0F);
        float duty = Gait.duty(Trunk.in(a, 0.5, 0.9), 0.6F, 0.5F);
        float phase = this.strides.phase(id, age, crab.level().getGameTime(),
                Strides.rate(Mth.clamp(amount, 0.0F, 1.0F), duty, SWEEP));
        float on = Gait.smooth(0.0F, 0.25F, a);
        int lead = crab.leftward() ? 1 : 0;
        float air = 0.0F;
        for (int i = 0; i < 8; i++) {
            int s = i / 4;
            int k = i % 4;
            float at = phase + ((k + s) % 2 == 0 ? 0.0F : Mth.PI) - k * 0.35F;
            float lift = Gait.lift(at, duty);
            float reach = (s == lead ? -0.8F : 0.8F) * Gait.swing(at, duty);
            int leg = LEGS + i * PER_LEG;
            // Reaching out lowers the knee and opens the leg below it, so the tip stays on the ground.
            float shift = (1.0F - on) * 0.3F * Channels.twitch(age, id + i * 13, 97, 6.0F);
            c[leg] = on * (0.45F * lift - 0.3F * reach) + shift;
            c[leg + 1] = on * (0.6F * reach - 0.3F * lift) - 0.2F * shift;
            c[leg + 2] = on * (-0.2F * lift) - 0.3F * shift;
            air += 1.0F - Gait.load(at, duty);
        }
        c[BOB] = on * 0.35F * air / 8.0F + 0.05F * Mth.sin(age * 0.11F);
        c[ROLL] = on * 0.05F * Mth.sin(2.0F * phase);
        c[MOUTH] = 0.15F + 0.15F * Mth.sin(age * 1.7F) * Mth.sin(age * 0.23F);
        c[EYES] = 0.12F + 0.25F * Channels.twitch(age, id, 61, 5.0F);
        c[EYES_SIDE] = 0.3F * Channels.twitch(age, id + 4, 113, 14.0F) * Mth.sin(age * 0.5F);
        c[RA + 3] = 0.35F * Channels.twitch(age, id + 1, 87, 10.0F);
        c[LA + 3] = 0.35F * Channels.twitch(age, id + 2, 131, 10.0F);
        for (int kind = Crab.WAVE; kind <= Crab.PINCH; kind++) {
            float w = crab.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) crab.actAge(kind, partialTick), age, id), w);
            }
        }
        float mouth = c[MOUTH];
        this.springs.follow(id, c, age, crab.level().getGameTime());
        c[MOUTH] = mouth;
        return p;
    }

    private Channels act(int kind, float t, float age, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        int near = id % 2 == 0 ? RA : LA;
        switch (kind) {
            case Crab.WAVE -> {
                // A raised claw beckoning in time, the hand rolling with it, the body leaning away.
                float wave = Mth.sin(t * 0.45F);
                arm(c, near, 0.7F + 0.3F * wave, -1.25F, -0.7F + 0.3F * wave, 0.7F, 0.3F * wave);
                c[ROLL] = (near == RA ? -0.08F : 0.08F);
                c[EYES] = 0.3F;
            }
            case Crab.SNAP -> {
                // Reared back, both claws spread wide and high, shutting with a snap every half second.
                float open = 1.0F - Trunk.in(t % 9.0F, 5.0, 7.0) + Trunk.in(t % 9.0F, 7.5, 9.0);
                float jolt = Trunk.in(t % 9.0F, 6.0, 7.0) * (1.0F - Trunk.in(t % 9.0F, 7.0, 8.5));
                c[PITCH] = -0.32F;
                c[BOB] = -0.6F - 0.15F * jolt;
                arm(c, RA, 0.6F, -0.95F - 0.1F * jolt, -0.75F, 0.85F * open, 0.0F);
                arm(c, LA, 0.6F, -0.95F - 0.1F * jolt, -0.75F, 0.85F * open, 0.0F);
                for (int i = 0; i < 8; i++) {
                    // The hind legs braced, the front ones lifted as it rears.
                    float front = 1.0F - (i % 4) / 3.0F;
                    c[LEGS + i * PER_LEG] = 0.25F * front - 0.1F;
                    c[LEGS + i * PER_LEG + 1] = 0.2F - 0.25F * front;
                }
                c[EYES] = 0.35F;
            }
            case Crab.BURROW -> {
                float shuffle = 1.0F - Trunk.in(t, 14.0, 26.0);
                c[SINK] = 2.6F * Trunk.in(t, 0.0, 22.0);
                c[ROLL] = 0.16F * shuffle * Mth.sin(t * 1.4F);
                c[BOB] = 0.0F;
                arm(c, RA, -0.35F, 0.25F, 0.35F, 0.0F, 0.0F);
                arm(c, LA, -0.35F, 0.25F, 0.35F, 0.0F, 0.0F);
                for (int i = 0; i < 8; i++) {
                    // Legs splayed and raking, tips working the sand.
                    float rake = Mth.sin(t * 1.4F + i);
                    c[LEGS + i * PER_LEG] = 0.35F + 0.2F * shuffle * rake;
                    c[LEGS + i * PER_LEG + 1] = -0.3F;
                    c[LEGS + i * PER_LEG + 2] = 0.3F * shuffle * Mth.cos(t * 1.4F + i);
                }
                c[EYES] = 0.05F + 0.2F * Channels.twitch(t, id, 70, 8.0F);
            }
            case Crab.FEED -> {
                // One claw down to the ground while the other is at its mouth, and back, the mouth working.
                float turn = Mth.sin(t * 0.32F);
                float right = Math.max(0.0F, turn);
                float left = Math.max(0.0F, -turn);
                arm(c, RA, -0.2F * left, 0.55F * right - 0.1F * left, 0.45F * left - 0.3F * right, 0.6F * right,
                        0.35F * left);
                arm(c, LA, -0.2F * right, 0.55F * left - 0.1F * right, 0.45F * right - 0.3F * left, 0.6F * left,
                        0.35F * right);
                c[PITCH] = 0.08F;
                c[EYES_BACK] = -0.15F;
                c[MOUTH] = 0.3F + 0.25F * Mth.sin(age * 2.3F);
            }
            case Crab.PINCH -> {
                float reach = Trunk.in(t, 0.0, Crab.PINCH_HIT) * (1.0F - Trunk.in(t, Crab.PINCH_HIT + 2.0,
                        Crab.PINCH_END));
                float shut = Trunk.in(t, Crab.PINCH_HIT - 1.0, Crab.PINCH_HIT + 0.5);
                c[LUNGE] = -2.5F * reach;
                c[PITCH] = 0.12F * reach;
                arm(c, near, -0.3F * reach, -0.25F * reach, -0.85F * reach, 0.95F * (1.0F - shut), 0.0F);
            }
            default -> {
            }
        }
        return p;
    }

    private static void arm(float[] c, int arm, float out, float lift, float in, float grip, float roll) {
        c[arm] = out;
        c[arm + 1] = lift;
        c[arm + 2] = in;
        c[arm + 3] = grip;
        c[arm + 4] = roll;
    }
}
