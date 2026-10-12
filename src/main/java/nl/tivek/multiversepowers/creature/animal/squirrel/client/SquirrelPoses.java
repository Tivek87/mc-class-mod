package nl.tivek.multiversepowers.creature.animal.squirrel.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;

// How the squirrel moves: always in bounds, forefeet together and hind feet together, its back arching and its tail
// streaming out the faster it goes; whiskers and tail tip twitching while it stands; and its acts: sitting up to look
// round in quick turns of the head, nibbling at something held to its mouth, digging with its forepaws, and flicking
// its tail in alarm. Every leg's swing is told against the ground (SquirrelModel takes off the body's own lean).
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
    // Per leg (right front, left front, right hind, left hind): swing, outward, foot (hind legs only).
    static final int LEGS = 14;
    static final int PER_LEG = 3;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final float PACE = 1.1F;
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);

    Channels of(Squirrel squirrel, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        this.bound(c, limbSwing, Mth.clamp(amount * 1.4F, 0.0F, 1.0F));
        int id = squirrel.getId();
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
        return p;
    }

    private void bound(float[] c, float limbSwing, float a) {
        float phase = limbSwing * PACE;
        // Forefeet reach and land together, the hind feet half a bound later, swinging up past them.
        float front = Mth.sin(phase);
        float back = Mth.sin(phase + Mth.PI);
        c[RF] = 0.9F * a * front;
        c[LF] = 0.9F * a * Mth.sin(phase + 0.25F);
        c[RH] = 1.0F * a * back;
        c[LH] = 1.0F * a * Mth.sin(phase + Mth.PI + 0.25F);
        c[RH + 2] = -0.5F * c[RH] + 0.5F * a * Channels.lift(phase + Mth.PI);
        c[LH + 2] = -0.5F * c[LH] + 0.5F * a * Channels.lift(phase + Mth.PI + 0.25F);
        c[BOB] = -1.3F * a * Math.abs(Mth.sin(phase * 0.5F + 0.6F));
        c[FLEX] = 0.4F * a * Mth.sin(phase + 0.5F * Mth.PI);
        c[PITCH] = -0.18F * a * Mth.sin(phase);
        c[HEAD] = 0.1F * a;
        c[TAIL] = -1.15F * a;
        c[TAIL_MID] = -0.45F * a + 0.12F * a * Mth.sin(phase - 1.0F);
        c[TAIL_TIP] = 0.9F * a + 0.15F * a * Mth.sin(phase - 2.0F);
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

    private static void stand(float[] c, int leg, float swing, float out, float foot) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = foot;
    }
}
