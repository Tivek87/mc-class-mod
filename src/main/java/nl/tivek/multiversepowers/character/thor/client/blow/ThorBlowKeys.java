package nl.tivek.multiversepowers.character.thor.client.blow;

import java.util.EnumMap;
import java.util.Map;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import org.joml.Vector3f;

// Thor's blows as keyframes, ticks from a blow's start. A hand goes through named spots, each where it is seen from
// outside (the chest's frame in pixels: +x his left, y down, -z ahead; with the way its elbow bends out) and where his
// own eyes see it (view space: x right, y up, -z ahead; with the point its arm reaches from), written for the right
// hand and mirrored for the left. The trunk turns (+ to his right), leans (+ forward), rolls (+ to his left) and sinks
// (pixels). A kick moves one foot (the model's own space, feet at y 24; and view space) and plants the other. A
// hammer blow is thrown by the right hand, which holds the hammer.
final class ThorBlowKeys {
    enum Spot {
        // Every fist stays clear of his head (8 pixels across round the neck) and chest (4 deep): its middle at
        // least 2 pixels outside them.
        GUARD(-2.4F, -0.6F, -6.8F, -0.6F, 0.6F, 0.8F, 0.30F, -0.30F, -0.62F, 0.62F, -1.0F, -0.2F),
        CHAMBER(-6.0F, 5.5F, 1.0F, -0.3F, 0.2F, 1.0F, 0.55F, -0.70F, -0.30F, 0.70F, -1.1F, 0.05F),
        STRAIGHT(-1.0F, -0.5F, -10.5F, -0.5F, 0.5F, 1.0F, 0.04F, 0.10F, -1.35F, 0.45F, -0.75F, -0.25F),
        LOW_STRAIGHT(-1.5F, 5.0F, -10.0F, -0.5F, 0.5F, 1.0F, 0.06F, -0.22F, -1.25F, 0.50F, -0.95F, -0.30F),
        HOOK_WIDE(-10.0F, -1.0F, -4.0F, -1.0F, -0.2F, 0.2F, 0.85F, -0.10F, -0.55F, 1.10F, -0.35F, 0.0F),
        HOOK(-0.5F, -1.5F, -7.5F, -1.0F, -0.2F, 0.2F, -0.10F, 0.08F, -0.95F, 0.45F, -0.20F, -0.55F),
        LOW_HOOK(-0.5F, 5.5F, -7.0F, -1.0F, 0.3F, 0.2F, -0.08F, -0.25F, -0.95F, 0.45F, -0.55F, -0.55F),
        UPPER_LOW(-3.5F, 7.0F, -4.5F, -0.3F, 0.5F, 1.0F, 0.30F, -0.80F, -0.58F, 0.55F, -1.2F, -0.30F),
        UPPER(-1.0F, -3.0F, -7.0F, -0.4F, 1.0F, 0.1F, 0.04F, 0.18F, -1.00F, 0.25F, -0.75F, -0.65F),
        OVERHEAD(-6.5F, -8.5F, 0.5F, -0.5F, -0.2F, 1.0F, 0.45F, 0.45F, -0.40F, 0.70F, -0.40F, 0.05F),
        DOWN(-1.5F, 8.0F, -7.5F, -0.5F, -0.3F, 0.8F, 0.05F, -0.40F, -1.10F, 0.30F, 0.10F, -0.55F),
        ELBOW_UP(-7.0F, -3.5F, -1.0F, -0.6F, -0.3F, -0.8F, 0.35F, 0.0F, -0.30F, 0.75F, -0.50F, -0.40F),
        ELBOW_ACROSS(2.0F, 0.5F, -6.5F, 0.0F, 0.0F, -1.0F, -0.25F, -0.12F, -0.40F, 0.25F, -0.25F, -0.75F),
        ELBOW_RISE(-6.5F, -7.0F, -0.5F, -0.2F, -1.0F, -0.6F, 0.18F, 0.20F, -0.25F, 0.25F, -0.25F, -0.75F),
        PALM(-1.5F, 1.0F, -10.2F, -0.5F, 0.5F, 1.0F, 0.06F, -0.02F, -1.30F, 0.45F, -0.80F, -0.25F),
        BACKFIST(-3.5F, -1.5F, -10.0F, -1.0F, 0.0F, 0.3F, 0.26F, 0.10F, -1.25F, 0.80F, -0.40F, -0.35F),
        CHOP_HIGH(-7.0F, -7.5F, -2.0F, -1.0F, -0.2F, 0.2F, 0.60F, 0.35F, -0.50F, 0.95F, -0.20F, -0.10F),
        CHOP(0.5F, 3.5F, -8.5F, -1.0F, 0.3F, 0.2F, -0.08F, -0.22F, -1.05F, 0.40F, -0.25F, -0.45F),
        BALANCE(-14.0F, 3.0F, 1.0F, -0.3F, 0.5F, 1.0F, 0.95F, -0.55F, -0.15F, 1.20F, -0.75F, 0.10F),
        TUCK(-2.0F, 2.0F, -5.0F, -0.5F, 0.5F, 1.0F, 0.25F, -0.45F, -0.45F, 0.55F, -1.0F, -0.15F),
        WIND(-9.0F, 0.0F, 5.0F, 0.0F, 0.2F, 1.0F, 0.90F, -0.40F, -0.05F, 1.10F, -0.90F, 0.20F),
        // A grab's: holding a creature up by the throat at arm's length (ThorPoses holds it there), lifted high beside
        // his head, hurled out ahead, and slammed down onto the ground before him.
        HELD(-2.5F, -1.0F, -8.5F, -0.5F, 0.6F, 0.8F, 0.50F, -0.41F, -1.30F, 0.60F, -0.70F, 0.0F),
        HOIST(-6.5F, -11.0F, -3.0F, -0.6F, 0.2F, 0.6F, 0.40F, 0.55F, -0.75F, 0.70F, -0.50F, -0.10F),
        HURL(-2.0F, -3.0F, -11.0F, -0.5F, 0.5F, 1.0F, 0.10F, 0.15F, -1.30F, 0.45F, -0.70F, -0.25F),
        SLAMMED(-2.0F, 12.0F, -9.0F, -0.5F, -0.3F, 0.8F, 0.05F, -0.55F, -1.10F, 0.30F, 0.10F, -0.55F),
        // A hammer throw's: drawn back high over the shoulder, its head behind him, out of his own sight; let go and
        // followed through as HURL and STRAIGHT are, but seen off to the throwing hand's side, so the arm never hides
        // where the hammer flies.
        THROW_BACK(-6.0F, -10.0F, 4.0F, -0.5F, -0.2F, 1.0F, 0.62F, 0.40F, -0.10F, 0.80F, -0.35F, 0.15F),
        LET_GO(-2.0F, -3.0F, -11.0F, -0.5F, 0.5F, 1.0F, 0.34F, -0.02F, -1.15F, 0.70F, -0.65F, -0.20F),
        FOLLOW(-1.0F, -0.5F, -10.5F, -0.5F, 0.5F, 1.0F, 0.38F, -0.30F, -1.00F, 0.70F, -0.95F, -0.20F),
        // A Storm Throw's toss: swung low behind the hip, then flung straight up overhead.
        TOSS_LOW(-7.0F, 9.0F, 4.0F, -0.4F, 0.3F, 1.0F, 0.55F, -0.85F, -0.35F, 0.70F, -1.10F, 0.05F),
        TOSS_UP(-4.5F, -12.0F, -5.0F, -0.6F, 0.0F, 0.8F, 0.30F, 0.60F, -0.85F, 0.55F, -0.55F, -0.25F);

        final Vector3f hand;
        final Vector3f pole;
        final Vector3f seen;
        final Vector3f from;

        Spot(float x, float y, float z, float px, float py, float pz, float sx, float sy, float sz, float fx,
                float fy, float fz) {
            this.hand = new Vector3f(x, y, z);
            this.pole = new Vector3f(px, py, pz);
            this.seen = new Vector3f(sx, sy, sz);
            this.from = new Vector3f(fx, fy, fz);
        }
    }

    record Key(float t, Spot right, Spot left, float twist, float pitch, float roll, float drop) {
    }

    record Foot(float t, Vector3f at, Vector3f seen) {
    }

    // One blow: its keys, and for a kick the kicking foot's (right or left).
    record Script(Key[] keys, Foot[] feet, boolean rightFoot) {
    }

    private static final Map<ThorBlow, Script> SCRIPTS = new EnumMap<>(ThorBlow.class);

    static {
        hands(ThorBlow.JAB,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.STRAIGHT, 0.32F, 0.1F, 0.02F, 0.5F),
                k(4, Spot.GUARD, Spot.GUARD, 0.1F, 0.03F, 0, 0.3F),
                k(7, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.CROSS,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.GUARD, 0.22F, 0, 0.03F, 0.5F),
                k(3, Spot.STRAIGHT, Spot.GUARD, -0.78F, 0.2F, -0.07F, 1.1F),
                k(5, Spot.GUARD, Spot.GUARD, -0.25F, 0.06F, 0, 0.5F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.LEAD_HOOK,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.HOOK_WIDE, -0.38F, 0.03F, -0.07F, 0.7F),
                k(4, Spot.GUARD, Spot.HOOK, 0.78F, 0.1F, 0.12F, 1.1F),
                k(6, Spot.GUARD, Spot.GUARD, 0.26F, 0.03F, 0.03F, 0.5F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.REAR_HOOK,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.HOOK_WIDE, Spot.GUARD, 0.38F, 0.03F, 0.07F, 0.7F),
                k(4, Spot.HOOK, Spot.GUARD, -0.82F, 0.1F, -0.12F, 1.1F),
                k(6, Spot.GUARD, Spot.GUARD, -0.26F, 0.03F, -0.03F, 0.5F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.OVERHAND,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.OVERHEAD, Spot.GUARD, 0.38F, -0.1F, 0.07F, 0.4F),
                k(5, Spot.STRAIGHT, Spot.GUARD, -0.82F, 0.4F, -0.16F, 2.2F),
                k(8, Spot.GUARD, Spot.GUARD, -0.25F, 0.1F, -0.04F, 0.8F),
                k(11, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.BACKFIST,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.ELBOW_ACROSS, -0.3F, 0, -0.04F, 0.3F),
                k(3, Spot.GUARD, Spot.BACKFIST, 0.45F, 0.05F, 0.07F, 0.4F),
                k(5, Spot.GUARD, Spot.GUARD, 0.14F, 0, 0, 0.2F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_FIST,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.OVERHEAD, Spot.GUARD, 0.26F, -0.16F, 0.04F, 0.0F),
                k(6, Spot.DOWN, Spot.GUARD, -0.36F, 0.52F, -0.06F, 3.2F),
                k(8, Spot.GUARD, Spot.GUARD, -0.12F, 0.16F, 0, 1.0F),
                k(11, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.ELBOW,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.ELBOW_UP, Spot.GUARD, 0.32F, 0, 0.05F, 0.4F),
                k(3, Spot.ELBOW_ACROSS, Spot.GUARD, -0.88F, 0.08F, -0.09F, 0.8F),
                k(5, Spot.GUARD, Spot.GUARD, -0.26F, 0.02F, 0, 0.4F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.LEAD_ELBOW,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.ELBOW_UP, -0.32F, 0, -0.05F, 0.4F),
                k(3, Spot.GUARD, Spot.ELBOW_ACROSS, 0.88F, 0.08F, 0.09F, 0.8F),
                k(5, Spot.GUARD, Spot.GUARD, 0.26F, 0.02F, 0, 0.4F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.RISING_ELBOW,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.TUCK, Spot.GUARD, 0.12F, 0.18F, 0, 2.4F),
                k(4, Spot.ELBOW_RISE, Spot.GUARD, -0.36F, -0.22F, -0.05F, -0.8F),
                k(6, Spot.GUARD, Spot.GUARD, -0.12F, -0.05F, 0, 0.2F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.PALM_STRIKE,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.CHAMBER, Spot.GUARD, 0.3F, 0, 0.03F, 0.7F),
                k(4, Spot.PALM, Spot.GUARD, -0.58F, 0.22F, -0.05F, 1.1F),
                k(6, Spot.GUARD, Spot.GUARD, -0.18F, 0.06F, 0, 0.4F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.DOUBLE_PALM,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.CHAMBER, Spot.CHAMBER, 0, -0.14F, 0, 1.6F),
                k(6, Spot.PALM, Spot.PALM, 0, 0.34F, 0, 1.6F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0.1F, 0, 0.6F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.BODY_HOOK,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.HOOK_WIDE, -0.32F, 0.2F, -0.07F, 2.2F),
                k(4, Spot.GUARD, Spot.LOW_HOOK, 0.62F, 0.36F, 0.1F, 3.4F),
                k(6, Spot.GUARD, Spot.GUARD, 0.2F, 0.12F, 0, 1.3F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.BODY_SHOT,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.GUARD, 0.14F, 0.08F, 0, 1.5F),
                k(4, Spot.LOW_STRAIGHT, Spot.GUARD, -0.62F, 0.38F, -0.06F, 3.6F),
                k(6, Spot.GUARD, Spot.GUARD, -0.2F, 0.12F, 0, 1.3F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.SUPERMAN_PUNCH,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.CHAMBER, Spot.GUARD, 0.3F, -0.05F, 0.04F, 2.2F),
                k(5, Spot.WIND, Spot.BALANCE, 0.36F, 0.12F, 0, -1.2F),
                k(8, Spot.STRAIGHT, Spot.BALANCE, -0.82F, 0.38F, -0.1F, 0.2F),
                k(10, Spot.GUARD, Spot.GUARD, -0.25F, 0.12F, 0, 1.2F),
                k(14, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.KNIFE_HAND,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.CHOP_HIGH, -0.28F, -0.06F, -0.05F, 0.3F),
                k(4, Spot.GUARD, Spot.CHOP, 0.56F, 0.22F, 0.08F, 1.3F),
                k(6, Spot.GUARD, Spot.GUARD, 0.18F, 0.06F, 0, 0.4F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.SHOVEL_HOOK,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.UPPER_LOW, Spot.GUARD, 0.28F, 0.16F, 0.05F, 2.2F),
                k(5, Spot.HOOK, Spot.GUARD, -0.68F, -0.04F, -0.08F, 0.2F),
                k(7, Spot.GUARD, Spot.GUARD, -0.2F, 0, 0, 0.2F),
                k(10, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.LEAD_STRAIGHT,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.GUARD, -0.08F, 0.03F, 0, 0.8F),
                k(4, Spot.GUARD, Spot.STRAIGHT, 0.55F, 0.26F, 0.05F, 1.5F),
                k(6, Spot.GUARD, Spot.GUARD, 0.18F, 0.08F, 0, 0.6F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAYMAKER,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.WIND, Spot.GUARD, 0.78F, -0.12F, 0.1F, 0.8F),
                k(7, Spot.HOOK, Spot.GUARD, -1.0F, 0.26F, -0.15F, 1.5F),
                k(10, Spot.GUARD, Spot.GUARD, -0.35F, 0.08F, -0.04F, 0.6F),
                k(13, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.THUNDER_PUNCH,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(4, Spot.WIND, Spot.GUARD, 0.82F, -0.06F, 0.07F, 1.5F),
                k(6, Spot.WIND, Spot.GUARD, 0.88F, -0.02F, 0.07F, 1.7F),
                k(8, Spot.STRAIGHT, Spot.GUARD, -0.92F, 0.38F, -0.12F, 1.9F),
                k(11, Spot.GUARD, Spot.GUARD, -0.25F, 0.1F, 0, 0.7F),
                k(15, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        // A kick is the whole body's: the knee drawn up first, the leg driven out above the hip, the trunk leaning
        // back from it and the arms out for balance, then the knee drawn back before the foot comes down.
        kick(ThorBlow.FRONT_KICK, true, new Key[] {
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.GUARD, Spot.GUARD, 0.06F, -0.16F, 0, 0.4F),
                k(5, Spot.BALANCE, Spot.TUCK, 0.12F, -0.34F, 0, 0.8F),
                k(8, Spot.GUARD, Spot.GUARD, 0.06F, -0.14F, 0, 0.4F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0) },
                f(1, -1.9F, 24.0F, 0.0F, 0.25F, -1.3F, -0.2F),
                f(3, -1.9F, 14.0F, -4.5F, 0.18F, -0.50F, -0.60F),
                f(5, -1.9F, 9.0F, -12.5F, 0.08F, -0.05F, -1.25F),
                f(8, -1.9F, 14.5F, -5.0F, 0.18F, -0.50F, -0.60F),
                f(11, -1.9F, 24.0F, -0.5F, 0.25F, -1.3F, -0.2F));
        kick(ThorBlow.ROUNDHOUSE, true, new Key[] {
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.GUARD, 0.36F, -0.06F, 0.12F, 0.5F),
                k(6, Spot.BALANCE, Spot.GUARD, -1.0F, -0.12F, 0.42F, 0.4F),
                k(9, Spot.GUARD, Spot.GUARD, -0.35F, -0.06F, 0.12F, 0.4F),
                k(13, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0) },
                f(1, -1.9F, 24.0F, 0.0F, 0.30F, -1.3F, -0.2F),
                f(2, -5.0F, 15.0F, 2.0F, 0.85F, -0.70F, -0.30F),
                f(4, -10.5F, 8.5F, -5.0F, 0.70F, -0.10F, -0.70F),
                f(6, -2.0F, 5.0F, -11.5F, -0.05F, 0.08F, -1.10F),
                f(9, -4.5F, 15.0F, -1.0F, 0.30F, -0.80F, -0.40F),
                f(11, -1.9F, 24.0F, 0.0F, 0.30F, -1.3F, -0.2F));
        kick(ThorBlow.SIDE_KICK, false, new Key[] {
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.GUARD, 0.42F, -0.06F, -0.18F, 0.5F),
                k(6, Spot.GUARD, Spot.BALANCE, 0.85F, -0.12F, -0.45F, 0.4F),
                k(9, Spot.GUARD, Spot.GUARD, 0.35F, -0.06F, -0.18F, 0.4F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0) },
                f(1, 1.9F, 24.0F, 0.0F, -0.30F, -1.3F, -0.2F),
                f(2, 4.0F, 14.5F, -2.0F, -0.30F, -0.80F, -0.40F),
                f(6, 2.0F, 9.0F, -12.5F, -0.05F, -0.05F, -1.25F),
                f(9, 4.0F, 15.0F, -2.0F, -0.30F, -0.80F, -0.40F),
                f(11, 1.9F, 24.0F, 0.0F, -0.30F, -1.3F, -0.2F));
        // A grab's endings: the right fist holds the creature throughout. Hurled: drawn back high over his shoulder
        // and flung out ahead, letting go at the front. Slammed: lifted high, then driven down to the ground before
        // him as he drops to a crouch.
        hands(ThorBlow.GRAB_HURL,
                k(0, Spot.HELD, Spot.GUARD, 0, 0, 0, 0),
                k(5, Spot.HOIST, Spot.BALANCE, 0.7F, -0.18F, 0.1F, 0.6F),
                k(9, Spot.HURL, Spot.GUARD, -0.75F, 0.3F, -0.1F, 1.2F),
                k(11, Spot.STRAIGHT, Spot.GUARD, -0.4F, 0.18F, -0.04F, 0.8F),
                k(14, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.GRAB_SLAM,
                k(0, Spot.HELD, Spot.GUARD, 0, 0, 0, 0),
                k(5, Spot.HOIST, Spot.GUARD, 0.15F, -0.22F, 0.04F, -0.6F),
                k(7, Spot.HOIST, Spot.BALANCE, 0.18F, -0.24F, 0.04F, -0.8F),
                k(10, Spot.SLAMMED, Spot.BALANCE, -0.2F, 0.62F, -0.05F, 5.0F),
                k(13, Spot.SLAMMED, Spot.GUARD, -0.1F, 0.5F, 0, 4.0F),
                k(16, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_SWING,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.WIND, Spot.GUARD, 0.65F, -0.08F, 0.08F, 0.8F),
                k(6, Spot.HOOK, Spot.GUARD, -0.9F, 0.2F, -0.12F, 1.2F),
                k(9, Spot.GUARD, Spot.GUARD, -0.3F, 0.06F, -0.03F, 0.5F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_BACKHAND,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.ELBOW_ACROSS, Spot.GUARD, -0.5F, 0.02F, -0.05F, 0.4F),
                k(5, Spot.BACKFIST, Spot.GUARD, 0.55F, 0.06F, 0.06F, 0.6F),
                k(8, Spot.GUARD, Spot.GUARD, 0.15F, 0.02F, 0, 0.3F),
                k(11, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_THRUST,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.CHAMBER, Spot.GUARD, 0.2F, 0, 0.02F, 0.5F),
                k(4, Spot.STRAIGHT, Spot.GUARD, -0.5F, 0.18F, -0.04F, 0.9F),
                k(7, Spot.GUARD, Spot.GUARD, -0.15F, 0.05F, 0, 0.3F),
                k(10, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_SMASH,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(4, Spot.OVERHEAD, Spot.GUARD, 0.1F, -0.2F, 0.03F, -0.4F),
                k(7, Spot.DOWN, Spot.GUARD, -0.2F, 0.45F, -0.05F, 3.2F),
                k(10, Spot.GUARD, Spot.GUARD, -0.05F, 0.12F, 0, 1.0F),
                k(14, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.HAMMER_UPPERCUT,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.UPPER_LOW, Spot.GUARD, 0.25F, 0.2F, 0.05F, 2.2F),
                k(5, Spot.UPPER, Spot.GUARD, -0.5F, -0.14F, -0.05F, -0.5F),
                k(8, Spot.GUARD, Spot.GUARD, -0.15F, -0.03F, 0, 0.2F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        // The hammer's throws: drawn back over the shoulder, hurled out ahead as the body turns into it (it leaves
        // the hand as the arm comes through), following through and back to the guard, the hand empty. On the ground
        // the right hand throws; in flight the left, which held it.
        hands(ThorBlow.HAMMER_THROW,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1.5F, Spot.THROW_BACK, Spot.BALANCE, 0.55F, -0.12F, 0.05F, 0.6F),
                k(2.5F, Spot.LET_GO, Spot.GUARD, -0.75F, 0.3F, -0.08F, 1.2F),
                k(5, Spot.FOLLOW, Spot.GUARD, -0.4F, 0.2F, -0.04F, 0.8F),
                k(11, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        // A Storm Throw: in the air the left hand first holds the hammer high over his head while the storm gathers
        // in it, then hurls it down ahead of him. On the ground it starts with the toss: swung low behind him and
        // flung high up, the body rising after it.
        hands(ThorBlow.STORM_THROW,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(4, Spot.GUARD, Spot.HOIST, 0.15F, -0.2F, 0.03F, 0.0F),
                k(9, Spot.GUARD, Spot.HOIST, 0.2F, -0.25F, 0.04F, 0.0F),
                k(10, Spot.GUARD, Spot.THROW_BACK, -0.5F, -0.15F, -0.05F, 0.0F),
                k(11, Spot.GUARD, Spot.LET_GO, 0.7F, 0.45F, 0.08F, 0.0F),
                k(14, Spot.GUARD, Spot.FOLLOW, 0.3F, 0.25F, 0.03F, 0.0F),
                k(18, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        hands(ThorBlow.STORM_TOSS,
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2.5F, Spot.TOSS_LOW, Spot.BALANCE, 0.45F, 0.25F, 0.05F, 2.4F),
                k(4, Spot.TOSS_UP, Spot.GUARD, -0.35F, -0.25F, -0.04F, -0.6F),
                k(7, Spot.TOSS_UP, Spot.GUARD, -0.2F, -0.15F, -0.02F, -0.3F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
    }

    private ThorBlowKeys() {
    }

    static Script of(ThorBlow blow) {
        return SCRIPTS.get(blow);
    }

    private static Key k(float t, Spot right, Spot left, float twist, float pitch, float roll, float drop) {
        return new Key(t, right, left, twist, pitch, roll, drop);
    }

    private static Foot f(float t, float x, float y, float z, float sx, float sy, float sz) {
        return new Foot(t, new Vector3f(x, y, z), new Vector3f(sx, sy, sz));
    }

    private static void hands(ThorBlow blow, Key... keys) {
        SCRIPTS.put(blow, new Script(keys, new Foot[0], true));
    }

    private static void kick(ThorBlow blow, boolean rightFoot, Key[] keys, Foot... feet) {
        SCRIPTS.put(blow, new Script(keys, feet, rightFoot));
    }
}
