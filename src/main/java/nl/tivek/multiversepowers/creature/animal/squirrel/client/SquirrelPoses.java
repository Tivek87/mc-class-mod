package nl.tivek.multiversepowers.creature.animal.squirrel.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;
import nl.tivek.multiversepowers.creature.client.PoseSprings;

// How the squirrel moves: always in bounds, forefeet landing together and the hind feet swinging up past them, each
// foot planted while the body goes over it and folded as it lifts, its back arching and stretching, its body rising
// clear of the ground between bounds and its tail streaming out the faster it goes in a wave down its length; ears,
// whiskers and tail tip twitching and eyes blinking while it stands; and its acts: sitting up to look round in quick
// turns of the head, nibbling at something held to its mouth, digging with its forepaws, and flicking its tail in
// alarm. Every leg's swing is told against the ground (SquirrelModel takes off the body's own lean). The whole pose
// follows on springs, the tail slower and swinging past.
final class SquirrelPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int FLEX = 4;
    static final int HEAD = 5;
    static final int HEAD_YAW = 6;
    static final int HEAD_ROLL = 7;
    static final int EARS = 8;
    static final int TAIL = 9;
    static final int TAIL_MID = 10;
    static final int TAIL_TIP = 11;
    static final int TAIL_SIDE = 12;
    static final int LOOK = 13;
    static final int TAIL_WAVE = 14;
    static final int BLINK = 15;
    // Per leg (right front, left front, right hind, left hind): swing, outward, knee (hind legs only), paw or foot.
    static final int LEGS = 16;
    static final int PER_LEG = 4;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a bound each foot is set down: the forefeet together, the hind feet half a bound after them.
    private static final float[] AT = { 0.0F, 0.06F, 0.5F, 0.56F };
    private static final float SWEEP = 6.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.7, 0.22, 0.45, EARS, TAIL, TAIL_MID, TAIL_TIP,
            TAIL_SIDE, TAIL_WAVE);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Squirrel squirrel, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        float a = Mth.clamp(amount * 1.4F, 0.0F, 1.0F);
        float phase = this.strides.phase(squirrel.getId(), age, squirrel.level().getGameTime(),
                Strides.rate(Mth.clamp(amount, 0.0F, 1.0F), 0.35F, SWEEP));
        this.bound(c, phase, a);
        int id = squirrel.getId();
        float turn = Mth.clamp(Mth.wrapDegrees(squirrel.yBodyRot - squirrel.yBodyRotO), -20.0F, 20.0F);
        c[ROLL] -= 0.01F * turn * a;
        c[TAIL_SIDE] -= 0.04F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.05F * Mth.sin(age * 0.2F);
        c[EARS] += 0.3F * Channels.twitch(age, id, 71, 3.0F);
        c[TAIL_TIP] += 0.3F * Channels.twitch(age, id + 3, 47, 5.0F);
        c[TAIL_SIDE] += 0.1F * Mth.sin(age * 0.09F);
        c[HEAD] += 0.04F * Mth.sin(age * 2.1F) * Channels.twitch(age, id + 5, 40, 14.0F);
        for (int kind = Squirrel.SIT_UP; kind <= Squirrel.FLICK; kind++) {
            float w = squirrel.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) squirrel.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, squirrel.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private void bound(float[] c, float phase, float a) {
        float duty = 0.35F;
        float on = Gait.smooth(0.0F, 0.3F, a);
        float load = 0.0F;
        for (int k = 0; k < 4; k++) {
            boolean front = k < 2;
            int leg = FEET[k];
            float p = phase - Mth.TWO_PI * AT[k];
            float swing = on * (front ? 0.9F : 1.0F) * Gait.swing(p, duty);
            float fold = on * Gait.fold(p, duty);
            c[leg] = swing;
            if (front) {
                c[leg + 3] = on * 1.0F * Gait.toe(p, duty) - swing;
            } else {
                c[leg + 2] = 1.1F * fold;
                c[leg + 3] = on * 0.8F * Gait.toe(p, duty) - swing - 1.1F * fold;
            }
            load += Gait.load(p, duty);
        }
        // Clear of the ground between landings, the back gathering up as the hind feet swing through and stretching
        // out as they push off.
        c[BOB] = -1.3F * a * (1.0F - Math.min(1.0F, load));
        c[FLEX] = 0.4F * a * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[PITCH] = -0.18F * a * Mth.sin(phase - Mth.TWO_PI * 0.1F);
        c[HEAD] = 0.1F * a - 0.5F * c[PITCH];
        c[TAIL] = -1.15F * a;
        c[TAIL_MID] = -0.45F * a + 0.12F * a * Mth.sin(phase - 1.0F);
        c[TAIL_TIP] = 0.9F * a + 0.15F * a * Mth.sin(phase - 2.0F);
        c[TAIL_WAVE] = 0.12F * Mth.sin(phase * 0.5F - 1.0F) * a;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        c[ROLL] = c[FLEX] = 0.0F;
        switch (kind) {
            case Squirrel.SIT_UP -> {
                // A look held, then a quick turn to the next.
                float turn = Math.round(Mth.sin(t * 0.11F) * 1.4F) * 0.45F;
                c[PITCH] = -1.2F;
                c[SINK] = 0.5F;
                stand(c, RF, -0.2F, 0.05F, 0.0F);
                stand(c, LF, -0.2F, 0.05F, 0.0F);
                stand(c, RH, -1.25F, 0.3F, 1.25F);
                stand(c, LH, -1.25F, 0.3F, 1.25F);
                c[HEAD] = 0.1F;
                c[HEAD_YAW] = turn;
                c[TAIL] = 0.15F;
                c[LOOK] = 0.5F;
            }
            case Squirrel.NIBBLE -> {
                float bite = Mth.sin(t * 3.1F);
                c[PITCH] = -1.0F;
                c[SINK] = 0.5F;
                // Forepaws together at its mouth, turning what they hold.
                stand(c, RF, -1.25F + 0.05F * bite, -0.3F, 0.0F);
                stand(c, LF, -1.25F - 0.05F * bite, -0.3F, 0.0F);
                stand(c, RH, -1.1F, 0.3F, 1.1F);
                stand(c, LH, -1.1F, 0.3F, 1.1F);
                c[HEAD] = 0.4F + 0.05F * bite;
                c[TAIL] = 0.1F;
                c[LOOK] = 0.0F;
            }
            case Squirrel.DIG -> {
                float dig = Mth.sin(t * 1.7F);
                c[PITCH] = 0.3F;
                c[HEAD] = 0.5F + 0.05F * dig;
                stand(c, RF, -0.3F + 0.55F * dig, 0.0F, 0.0F);
                stand(c, LF, -0.3F - 0.55F * dig, 0.0F, 0.0F);
                c[TAIL] = 0.25F + 0.1F * Mth.sin(t * 0.9F);
                c[LOOK] = 0.0F;
            }
            case Squirrel.FLICK -> {
                // Bursts of flicks, the whole tail whipping over its back.
                float burst = 0.5F + 0.5F * Mth.sin(t * 0.35F);
                float flick = burst * Mth.sin(t * 2.2F);
                c[PITCH] = -0.25F;
                stand(c, RF, 0.1F, 0.1F, 0.0F);
                stand(c, LF, 0.1F, 0.1F, 0.0F);
                c[HEAD] = -0.15F;
                c[EARS] = 0.2F;
                c[TAIL] = 0.35F * flick;
                c[TAIL_MID] = 0.4F * flick;
                c[TAIL_TIP] = -0.5F * flick;
                c[TAIL_SIDE] = 0.3F * burst * Mth.sin(t * 2.2F + 1.2F);
                c[LOOK] = 1.0F;
            }
            default -> {
            }
        }
        return p;
    }

    // A leg held still: its swing, how far out, and how far a hind leg folds at its knee (its foot then lying flat).
    private static void stand(float[] c, int leg, float swing, float out, float knee) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = knee;
        c[leg + 3] = 0.0F;
    }
}
