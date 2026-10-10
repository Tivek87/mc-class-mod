package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import org.joml.Vector3f;

// Green Lantern's fist blows as keyframes, ticks from a blow's start. A hand goes through named spots, each where it
// is seen from outside (the chest's frame in pixels: +x his left, y down, -z ahead; with the way its elbow bends out)
// and where his own eyes see it (view space: x right, y up, -z ahead; with the point its arm reaches from), written
// for the right hand and mirrored for the left. The trunk turns (+ to his right), leans (+ forward), rolls (+ to his
// left) and sinks (pixels).
final class FistKeys {
    enum Spot {
        // Every fist stays clear of his head and chest.
        GUARD(-2.4F, -0.6F, -6.8F, -0.6F, 0.6F, 0.8F, 0.30F, -0.30F, -0.62F, 0.62F, -1.0F, -0.2F),
        CHAMBER(-6.0F, 5.5F, 1.0F, -0.3F, 0.2F, 1.0F, 0.55F, -0.70F, -0.30F, 0.70F, -1.1F, 0.05F),
        STRAIGHT(-1.0F, -0.5F, -10.5F, -0.5F, 0.5F, 1.0F, 0.04F, 0.10F, -1.35F, 0.45F, -0.75F, -0.25F),
        HOOK_WIDE(-10.0F, -1.0F, -4.0F, -1.0F, -0.2F, 0.2F, 0.85F, -0.10F, -0.55F, 1.10F, -0.35F, 0.0F),
        HOOK(-0.5F, -1.5F, -7.5F, -1.0F, -0.2F, 0.2F, -0.10F, 0.08F, -0.95F, 0.45F, -0.20F, -0.55F),
        LOW_HOOK(-0.5F, 5.5F, -7.0F, -1.0F, 0.3F, 0.2F, -0.08F, -0.25F, -0.95F, 0.45F, -0.55F, -0.55F),
        UPPER_LOW(-3.5F, 7.0F, -4.5F, -0.3F, 0.5F, 1.0F, 0.30F, -0.80F, -0.58F, 0.55F, -1.2F, -0.30F),
        UPPER(-1.0F, -3.0F, -7.0F, -0.4F, 1.0F, 0.1F, 0.04F, 0.18F, -1.00F, 0.25F, -0.75F, -0.65F),
        OVERHEAD(-6.5F, -8.5F, 0.5F, -0.5F, -0.2F, 1.0F, 0.45F, 0.45F, -0.40F, 0.70F, -0.40F, 0.05F),
        ELBOW_UP(-7.0F, -3.5F, -1.0F, -0.6F, -0.3F, -0.8F, 0.35F, 0.0F, -0.30F, 0.75F, -0.50F, -0.40F),
        ELBOW_ACROSS(2.0F, 0.5F, -6.5F, 0.0F, 0.0F, -1.0F, -0.25F, -0.12F, -0.40F, 0.25F, -0.25F, -0.75F),
        BACKFIST(-3.5F, -1.5F, -10.0F, -1.0F, 0.0F, 0.3F, 0.26F, 0.10F, -1.25F, 0.80F, -0.40F, -0.35F),
        BALANCE(-14.0F, 3.0F, 1.0F, -0.3F, 0.5F, 1.0F, 0.95F, -0.55F, -0.15F, 1.20F, -0.75F, 0.10F),
        TUCK(-2.0F, 2.0F, -5.0F, -0.5F, 0.5F, 1.0F, 0.25F, -0.45F, -0.45F, 0.55F, -1.0F, -0.15F),
        WIND(-9.0F, 0.0F, 5.0F, 0.0F, 0.2F, 1.0F, 0.90F, -0.40F, -0.05F, 1.10F, -0.90F, 0.20F),
        // The heavies': both fists clasped high over the head and brought down together before him; and an arm swung
        // straight out to the side for a spin.
        CLASP_UP(-1.2F, -11.0F, -2.0F, -0.6F, 0.0F, 0.8F, 0.14F, 0.62F, -0.72F, 0.80F, 0.12F, 0.05F),
        CLASP_DOWN(-1.0F, 7.5F, -8.5F, -0.5F, -0.3F, 0.8F, 0.08F, -0.45F, -1.05F, 0.35F, 0.05F, -0.50F),
        SPIN_OUT(-13.5F, -1.0F, -2.5F, -0.2F, 0.6F, 1.0F, 0.95F, -0.20F, -0.55F, 1.10F, -0.60F, 0.10F);

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

    private static final Key[][] SCRIPTS = new Key[FistMoves.KINDS][];

    static {
        SCRIPTS[FistMoves.JAB] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.STRAIGHT, 0.32F, 0.1F, 0.02F, 0.5F),
                k(4, Spot.GUARD, Spot.GUARD, 0.1F, 0.03F, 0, 0.3F),
                k(7, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.CROSS] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.GUARD, 0.22F, 0, 0.03F, 0.5F),
                k(3, Spot.STRAIGHT, Spot.GUARD, -0.78F, 0.2F, -0.07F, 1.1F),
                k(5, Spot.GUARD, Spot.GUARD, -0.25F, 0.06F, 0, 0.5F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.HOOK] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.HOOK_WIDE, -0.38F, 0.03F, -0.07F, 0.7F),
                k(4, Spot.GUARD, Spot.HOOK, 0.78F, 0.1F, 0.12F, 1.1F),
                k(6, Spot.GUARD, Spot.GUARD, 0.26F, 0.03F, 0.03F, 0.5F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.UPPERCUT] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.UPPER_LOW, Spot.GUARD, 0.28F, 0.16F, 0.05F, 2.2F),
                k(5, Spot.UPPER, Spot.GUARD, -0.55F, -0.14F, -0.06F, -0.6F),
                k(7, Spot.GUARD, Spot.GUARD, -0.18F, -0.03F, 0, 0.2F),
                k(10, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.BODY] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.GUARD, Spot.HOOK_WIDE, -0.32F, 0.2F, -0.07F, 2.2F),
                k(4, Spot.GUARD, Spot.LOW_HOOK, 0.62F, 0.36F, 0.1F, 3.4F),
                k(6, Spot.GUARD, Spot.GUARD, 0.2F, 0.12F, 0, 1.3F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.OVERHAND] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(2, Spot.OVERHEAD, Spot.GUARD, 0.38F, -0.1F, 0.07F, 0.4F),
                k(5, Spot.STRAIGHT, Spot.GUARD, -0.82F, 0.4F, -0.16F, 2.2F),
                k(8, Spot.GUARD, Spot.GUARD, -0.25F, 0.1F, -0.04F, 0.8F),
                k(11, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.BACKFIST] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.GUARD, Spot.ELBOW_ACROSS, -0.3F, 0, -0.04F, 0.3F),
                k(3, Spot.GUARD, Spot.BACKFIST, 0.45F, 0.05F, 0.07F, 0.4F),
                k(5, Spot.GUARD, Spot.GUARD, 0.14F, 0, 0, 0.2F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.ELBOW] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(1, Spot.ELBOW_UP, Spot.GUARD, 0.32F, 0, 0.05F, 0.4F),
                k(3, Spot.ELBOW_ACROSS, Spot.GUARD, -0.88F, 0.08F, -0.09F, 0.8F),
                k(5, Spot.GUARD, Spot.GUARD, -0.26F, 0.02F, 0, 0.4F),
                k(8, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.DOUBLE] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(3, Spot.CHAMBER, Spot.CHAMBER, 0, -0.14F, 0, 1.6F),
                k(6, Spot.STRAIGHT, Spot.STRAIGHT, 0, 0.34F, 0, 1.6F),
                k(9, Spot.GUARD, Spot.GUARD, 0, 0.1F, 0, 0.6F),
                k(12, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.FINISHER] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(4, Spot.WIND, Spot.GUARD, 0.82F, -0.06F, 0.07F, 1.5F),
                k(7, Spot.WIND, Spot.BALANCE, 0.9F, -0.02F, 0.07F, 1.8F),
                k(9, Spot.STRAIGHT, Spot.BALANCE, -0.95F, 0.4F, -0.12F, 1.9F),
                k(12, Spot.GUARD, Spot.GUARD, -0.25F, 0.1F, 0, 0.7F),
                k(16, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.HAMMER] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(5, Spot.CLASP_UP, Spot.CLASP_UP, 0, -0.25F, 0, -0.5F),
                k(11, Spot.CLASP_UP, Spot.CLASP_UP, 0, -0.32F, 0, -0.8F),
                k(14, Spot.CLASP_DOWN, Spot.CLASP_DOWN, 0, 0.6F, 0, 3.8F),
                k(18, Spot.CLASP_DOWN, Spot.CLASP_DOWN, 0, 0.5F, 0, 3.2F),
                k(24, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.RISING] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(6, Spot.UPPER_LOW, Spot.TUCK, 0.3F, 0.3F, 0.05F, 3.5F),
                k(10, Spot.UPPER_LOW, Spot.TUCK, 0.35F, 0.36F, 0.05F, 4.2F),
                k(12, Spot.UPPER, Spot.BALANCE, -0.55F, -0.28F, -0.05F, -1.0F),
                k(17, Spot.UPPER, Spot.GUARD, -0.3F, -0.15F, 0, -0.5F),
                k(24, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.PISTON] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(6, Spot.CHAMBER, Spot.GUARD, 0.45F, 0, 0.04F, 1.0F),
                k(9, Spot.CHAMBER, Spot.GUARD, 0.52F, 0, 0.05F, 1.2F),
                k(11, Spot.STRAIGHT, Spot.BALANCE, -0.72F, 0.25F, -0.08F, 1.4F),
                k(16, Spot.STRAIGHT, Spot.BALANCE, -0.62F, 0.2F, -0.06F, 1.2F),
                k(22, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
        SCRIPTS[FistMoves.SPIN] = keys(
                k(0, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0),
                k(4, Spot.SPIN_OUT, Spot.SPIN_OUT, 0.5F, 0, 0, 1.0F),
                k(16, Spot.SPIN_OUT, Spot.SPIN_OUT, -0.3F, 0, 0, 1.0F),
                k(20, Spot.GUARD, Spot.GUARD, 0, 0, 0, 0));
    }

    private FistKeys() {
    }

    static Key[] of(int move) {
        return SCRIPTS[Math.floorMod(move, FistMoves.KINDS)];
    }

    private static Key k(float t, Spot right, Spot left, float twist, float pitch, float roll, float drop) {
        return new Key(t, right, left, twist, pitch, roll, drop);
    }

    private static Key[] keys(Key... keys) {
        return keys;
    }
}
