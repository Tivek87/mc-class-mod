package nl.tivek.multiversepowers.creature.wizard.client;

import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// The Wizard's moves as keyframes on his channels (WizardPoses): each key what is added to his pose at rest at that
// tick, every channel it does not name at rest; between keys the pose runs smoothly through them. Every move winds up
// before its release (a pull back, a coil, a breath in), strikes past where it settles and follows through. Each
// channel runs a little behind or ahead of the trunk (LAG): legs lead, the arm follows the trunk, the elbow the arm, the
// wrist and staff the elbow, the head last, so a move rolls through his body as a whip does instead of all at once.
final class WizardKeys {
    static final int LEAN = 0;
    static final int SINK = 1;
    static final int TWIST = 2;
    static final int ROLL = 3;
    static final int NOD = 4;
    static final int TILT = 5;
    static final int STAFF = 6;
    static final int SPIN = 7;
    static final int GLOW = 8;
    static final int HAND = 9;
    static final int R_ARM = 10;
    static final int L_ARM = 11;
    static final int R_OUT = 12;
    static final int L_OUT = 13;
    static final int R_IN = 14;
    static final int L_IN = 15;
    static final int R_ELBOW = 16;
    static final int L_ELBOW = 17;
    static final int R_WRIST = 18;
    static final int L_WRIST = 19;
    static final int R_LEG = 20;
    static final int L_LEG = 21;
    static final int R_KNEE = 22;
    static final int L_KNEE = 23;
    static final int LIFT = 24;
    static final int FLARE = 25;
    static final int CHANNELS = 26;

    private static final Key[][] MOVES = new Key[WizardMove.KINDS + 1][];
    // How many ticks each channel runs behind the trunk (ahead, below 0).
    private static final float[] LAG = { 0.0F, 0.0F, 0.3F, 0.5F, 1.2F, 1.5F, 1.6F, 1.6F, 0.0F, 0.0F, 0.4F, 0.4F, 0.5F,
            0.5F, 0.5F, 0.5F, 0.9F, 0.9F, 1.4F, 1.4F, -0.5F, -0.5F, -0.2F, -0.2F, 0.0F, 1.0F };

    private WizardKeys() {
    }

    // A key at `tick` setting each named channel (channel, value, channel, value ...).
    private static Key k(float tick, float... pairs) {
        float[] values = new float[CHANNELS];
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values[(int) pairs[i]] = pairs[i + 1];
        }
        return new Key(tick, false, values);
    }

    // A key the pose comes to rest on before going on.
    private static Key hold(float tick, float... pairs) {
        Key key = k(tick, pairs);
        return new Key(key.tick(), true, key.values());
    }

    // What the move adds to his pose `t` ticks in, each channel at its own lag; null when it has no keys.
    static float[] at(int move, double t) {
        Key[] keys = move >= 0 && move < MOVES.length ? MOVES[move] : null;
        if (keys == null) {
            return null;
        }
        float[] out = new float[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            out[i] = Keyframes.at(keys, Math.max(0.0F, (float) t - LAG[i]))[i];
        }
        return out;
    }

    static {
        // A thrust of the staff, the weight on the front foot, then a wide sweep round from the coiled shoulder.
        MOVES[WizardMove.STRIKE] = new Key[] { k(0),
                k(5, R_ARM, 0.35F, R_ELBOW, 0.8F, STAFF, -0.5F, TWIST, 0.4F, LEAN, -0.1F, SINK, 0.8F, L_LEG, -0.3F,
                        L_KNEE, 0.3F, R_LEG, 0.2F, L_ARM, 0.2F, L_OUT, 0.2F),
                k(8, R_ARM, -1.5F, R_ELBOW, -1.15F, STAFF, 1.4F, R_WRIST, 0.25F, TWIST, -0.5F, LEAN, 0.32F, SINK,
                        1.7F, L_LEG, -0.6F, L_KNEE, 0.6F, R_LEG, 0.45F, L_ARM, 0.35F, L_OUT, 0.4F),
                k(11, R_ARM, -1.15F, R_ELBOW, -0.85F, STAFF, 1.2F, TWIST, -0.3F, LEAN, 0.2F, SINK, 1.3F, L_LEG,
                        -0.45F, L_KNEE, 0.45F, R_LEG, 0.3F),
                k(14, R_ARM, -0.9F, R_OUT, 0.9F, R_IN, -0.5F, R_ELBOW, -0.6F, STAFF, 1.0F, TWIST, 0.65F, LEAN,
                        0.05F, SINK, 1.0F, L_ARM, -0.4F, L_OUT, 0.3F, R_LEG, -0.2F, L_LEG, 0.2F, R_KNEE, 0.3F),
                k(17, R_ARM, -1.25F, R_OUT, 0.35F, R_IN, 1.25F, R_ELBOW, -0.9F, STAFF, 1.35F, R_WRIST, -0.3F, TWIST,
                        -0.85F, LEAN, 0.28F, ROLL, 0.12F, SINK, 1.5F, L_ARM, 0.45F, L_OUT, 0.55F, R_LEG, -0.45F,
                        R_KNEE, 0.5F, L_LEG, 0.35F),
                k(21, R_ARM, -0.9F, R_IN, 1.45F, R_ELBOW, -0.5F, STAFF, 1.0F, TWIST, -0.95F, LEAN, 0.2F, ROLL, 0.08F,
                        SINK, 1.1F, L_ARM, 0.3F, L_OUT, 0.4F, R_LEG, -0.3F, R_KNEE, 0.35F),
                k(26) };
        // A flick of the staff: the wrist cocked back, then snapped forward.
        MOVES[WizardMove.SPARK] = new Key[] { k(0),
                k(4, R_ARM, -0.6F, R_ELBOW, -0.3F, STAFF, 0.4F, R_WRIST, -0.55F, TWIST, 0.15F, GLOW, 0.6F),
                k(6, R_ARM, -1.0F, R_ELBOW, -0.7F, STAFF, 1.3F, R_WRIST, 0.45F, TWIST, -0.2F, LEAN, 0.12F, GLOW,
                        1.0F, L_ARM, 0.15F),
                k(9, R_ARM, -0.8F, R_ELBOW, -0.5F, STAFF, 1.0F, R_WRIST, 0.2F, TWIST, -0.1F, LEAN, 0.05F),
                k(12) };
        // The staff levelled at them, his left palm turned out beside it; it kicks with each bolt.
        MOVES[WizardMove.BOLT] = new Key[] { k(0),
                k(4, R_ARM, -0.35F, STAFF, -0.3F, L_ARM, -0.6F, L_ELBOW, 1.1F, L_IN, 0.5F, LEAN, -0.06F, TWIST, 0.2F,
                        GLOW, 0.5F, HAND, 0.5F),
                k(9, R_ARM, -1.2F, R_ELBOW, -0.95F, STAFF, 1.3F, L_ARM, -1.4F, L_OUT, 0.15F, L_IN, 0.35F, L_ELBOW,
                        -0.2F, L_WRIST, -0.85F, LEAN, 0.12F, TWIST, -0.18F, GLOW, 1.3F, HAND, 1.0F, L_LEG, -0.25F,
                        R_LEG, 0.15F, SINK, 0.6F),
                hold(24, R_ARM, -1.15F, R_ELBOW, -0.9F, STAFF, 1.25F, L_ARM, -1.3F, L_OUT, 0.15F, L_IN, 0.3F,
                        L_ELBOW, -0.1F, L_WRIST, -0.7F, LEAN, 0.1F, TWIST, -0.15F, GLOW, 1.0F, HAND, 0.8F, L_LEG,
                        -0.25F, R_LEG, 0.15F, SINK, 0.6F),
                k(32) };
        // Fire gathered at his belt, drawn back high over his shoulder, hurled with a step and a turn of his body.
        MOVES[WizardMove.FIREBALL] = new Key[] { k(0),
                k(6, L_ARM, -0.55F, L_ELBOW, 1.7F, L_IN, 0.6F, L_WRIST, -0.6F, HAND, 0.6F, LEAN, 0.12F, NOD, 0.2F),
                k(13, L_ARM, -2.55F, L_OUT, 0.4F, L_ELBOW, 1.5F, L_WRIST, 0.3F, TWIST, 0.55F, LEAN, -0.18F, R_ARM,
                        0.25F, R_OUT, 0.3F, STAFF, -0.3F, HAND, 1.0F, SINK, 0.5F, NOD, -0.1F, R_LEG, 0.15F),
                k(17, L_ARM, -1.2F, L_ELBOW, 0.05F, L_WRIST, 0.55F, TWIST, -0.55F, LEAN, 0.38F, L_LEG, -0.55F,
                        L_KNEE, 0.45F, R_LEG, 0.35F, SINK, 1.5F, HAND, 0.8F, R_ARM, 0.15F, NOD, 0.1F),
                k(21, L_ARM, -0.6F, L_IN, 0.7F, L_ELBOW, 0.3F, TWIST, -0.65F, LEAN, 0.3F, L_LEG, -0.45F, L_KNEE,
                        0.4F, R_LEG, 0.3F, SINK, 1.2F),
                k(28) };
        // The staff raised and swung round over his head, a leap up onto his toes, then driven down into the ground as
        // he drops onto one knee.
        MOVES[WizardMove.BLIZZARD] = new Key[] { k(0),
                k(5, R_ARM, -2.6F, R_ELBOW, -0.9F, STAFF, -0.1F, L_ARM, -1.2F, L_OUT, 0.6F, NOD, -0.3F, GLOW, 0.8F),
                k(17, R_ARM, -2.6F, R_ELBOW, -0.9F, STAFF, -0.1F, L_ARM, -1.3F, L_OUT, 0.7F, NOD, -0.35F, GLOW,
                        1.2F),
                k(19, R_ARM, -2.95F, R_ELBOW, -0.7F, LEAN, -0.25F, SINK, -0.6F, NOD, -0.45F, L_ARM, -1.6F, L_OUT,
                        0.8F, GLOW, 1.4F),
                k(22, R_ARM, -0.7F, R_ELBOW, -0.3F, STAFF, 0.4F, LEAN, 0.45F, SINK, 3.5F, R_LEG, -1.1F, R_KNEE, 1.2F,
                        L_LEG, 0.3F, L_KNEE, 1.7F, NOD, 0.15F, L_ARM, -0.6F, L_OUT, 0.9F, GLOW, 2.0F, FLARE, 0.6F),
                hold(28, R_ARM, -0.7F, R_ELBOW, -0.3F, STAFF, 0.4F, LEAN, 0.4F, SINK, 3.3F, R_LEG, -1.1F, R_KNEE,
                        1.2F, L_LEG, 0.3F, L_KNEE, 1.7F, NOD, 0.1F, L_ARM, -0.5F, L_OUT, 0.8F, GLOW, 1.2F),
                k(34) };
        // Hunched in round his staff as the dark folds in on him; flung open as he is there again.
        MOVES[WizardMove.BLINK] = new Key[] { k(0),
                k(6, LEAN, 0.4F, SINK, 1.6F, NOD, 0.45F, R_ARM, -0.25F, R_IN, 0.5F, L_ARM, -0.9F, L_IN, 0.85F,
                        L_ELBOW, 1.4F, R_KNEE, 0.45F, L_KNEE, 0.45F, R_LEG, -0.2F, L_LEG, -0.2F, GLOW, 1.0F),
                k(9, LEAN, -0.15F, NOD, -0.25F, R_ARM, -0.4F, R_OUT, 0.5F, L_ARM, -1.4F, L_OUT, 1.1F, L_WRIST,
                        -0.6F, FLARE, 0.8F, GLOW, 1.4F),
                k(16) };
        // His left hand clawed down at the ground and torn upward, dragging the chains up out of it.
        MOVES[WizardMove.CHAINS] = new Key[] { k(0),
                k(7, L_ARM, -0.4F, L_ELBOW, 1.4F, L_WRIST, 0.8F, LEAN, 0.22F, SINK, 1.0F, NOD, 0.3F, R_ARM, 0.1F,
                        STAFF, -0.2F, HAND, 0.6F),
                k(12, L_ARM, -0.5F, L_ELBOW, 1.6F, L_WRIST, 1.0F, LEAN, 0.3F, SINK, 1.7F, NOD, 0.35F, HAND, 1.0F,
                        L_KNEE, 0.4F, R_KNEE, 0.3F),
                k(14, L_ARM, -2.3F, L_ELBOW, 0.6F, L_WRIST, -0.5F, LEAN, -0.15F, NOD, -0.2F, TWIST, 0.2F, HAND,
                        1.2F),
                hold(20, L_ARM, -2.0F, L_ELBOW, 0.7F, L_WRIST, -0.3F, LEAN, -0.08F, NOD, -0.1F, TWIST, 0.15F, HAND,
                        0.8F),
                k(30) };
        // The staff raised crackling to the sky on tiptoe, then thrust at them as the lightning leaps.
        MOVES[WizardMove.LIGHTNING] = new Key[] { k(0),
                k(8, R_ARM, -2.7F, R_ELBOW, -0.6F, STAFF, 0.2F, NOD, -0.4F, L_ARM, -1.0F, L_OUT, 0.8F, L_WRIST,
                        -0.6F, GLOW, 1.5F, SINK, -0.3F),
                k(12, R_ARM, -2.95F, R_ELBOW, -0.5F, LEAN, -0.25F, TWIST, 0.3F, NOD, -0.45F, L_ARM, -1.1F, L_OUT,
                        0.9F, GLOW, 1.8F, SINK, -0.5F),
                k(14, R_ARM, -1.3F, R_ELBOW, -0.9F, STAFF, 1.45F, LEAN, 0.38F, TWIST, -0.4F, L_ARM, 0.3F, L_OUT,
                        0.45F, SINK, 1.3F, L_LEG, -0.4F, L_KNEE, 0.4F, R_LEG, 0.3F, GLOW, 2.2F),
                k(19, R_ARM, -1.15F, R_ELBOW, -0.8F, STAFF, 1.3F, LEAN, 0.25F, TWIST, -0.3F, L_ARM, 0.2F, L_OUT,
                        0.4F, SINK, 1.0F, L_LEG, -0.35F, L_KNEE, 0.35F, R_LEG, 0.25F, GLOW, 1.0F),
                k(26) };
        // Both arms and the staff raised to the sky, his head thrown back, as the rift tears open over them.
        MOVES[WizardMove.STARFALL] = new Key[] { k(0),
                k(10, R_ARM, -2.6F, R_OUT, 0.12F, R_ELBOW, -1.0F, STAFF, 1.3F, L_ARM, -2.4F, L_OUT, 0.5F, L_ELBOW,
                        -0.1F, L_WRIST, -0.5F, NOD, -0.5F, LEAN, -0.12F, SINK, -0.2F, GLOW, 1.2F),
                k(18, R_ARM, -2.8F, R_OUT, 0.12F, R_ELBOW, -1.05F, STAFF, 1.4F, L_ARM, -2.6F, L_OUT, 0.55F, NOD,
                        -0.65F, LEAN, -0.16F, SINK, -0.4F, GLOW, 1.6F, LIFT, 1.0F),
                k(20, R_ARM, -2.75F, R_OUT, 0.12F, R_ELBOW, -1.05F, STAFF, 1.4F, L_ARM, -2.55F, L_OUT, 0.75F, NOD,
                        -0.6F, LEAN, -0.14F, SINK, 0.6F, GLOW, 2.4F, FLARE, 0.5F),
                hold(32, R_ARM, -2.75F, R_OUT, 0.12F, R_ELBOW, -1.05F, STAFF, 1.4F, L_ARM, -2.5F, L_OUT, 0.6F, NOD,
                        -0.55F, LEAN, -0.14F, SINK, -0.3F, GLOW, 1.4F),
                k(42) };
        // The staff spun in his hand, swung high and planted hard, his left palm pressed down at the ground.
        MOVES[WizardMove.RUNES] = new Key[] { k(0),
                k(6, R_ARM, -1.0F, R_IN, 0.5F, L_ARM, -0.8F, L_OUT, 0.6F, GLOW, 0.6F, SPIN, 3.0F),
                k(15, R_ARM, -2.0F, R_ELBOW, -0.5F, STAFF, -0.3F, LEAN, -0.1F, SINK, -0.4F, L_ARM, -0.9F, L_OUT,
                        0.7F, GLOW, 1.0F, SPIN, 9.0F),
                k(18, R_ARM, -0.6F, R_ELBOW, 0.3F, STAFF, 0.25F, LEAN, 0.32F, SINK, 2.0F, L_ARM, -1.3F, L_OUT, 0.9F,
                        L_WRIST, -0.8F, NOD, 0.3F, GLOW, 2.0F, SPIN, 9.42F, R_KNEE, 0.5F, L_KNEE, 0.5F),
                hold(24, R_ARM, -0.6F, R_ELBOW, 0.3F, STAFF, 0.25F, LEAN, 0.28F, SINK, 1.8F, L_ARM, -1.2F, L_OUT,
                        0.85F, L_WRIST, -0.7F, NOD, 0.25F, GLOW, 1.0F, SPIN, 9.42F, R_KNEE, 0.45F, L_KNEE, 0.45F),
                k(32, SPIN, 9.42F) };
        // His left hand rising palm up as they rise, held high, trembling; then his fist clenched and driven down.
        MOVES[WizardMove.LIFT] = new Key[] { k(0),
                k(8, L_ARM, -0.6F, L_ELBOW, 1.2F, L_WRIST, -0.9F, L_OUT, 0.4F, SINK, 0.8F, LEAN, 0.05F, HAND, 0.6F),
                k(14, L_ARM, -2.4F, L_ELBOW, 0.4F, L_WRIST, -0.4F, L_OUT, 0.3F, NOD, -0.35F, LEAN, -0.15F, SINK,
                        -0.2F, R_ARM, -0.6F, STAFF, 0.3F, HAND, 1.0F),
                k(30, L_ARM, -2.7F, L_ELBOW, 0.3F, L_WRIST, -0.3F, L_OUT, 0.3F, NOD, -0.45F, LEAN, -0.18F, R_ARM,
                        -0.65F, STAFF, 0.3F, HAND, 1.2F, LIFT, 2.0F),
                k(32, L_ARM, -2.95F, L_ELBOW, 0.5F, L_WRIST, 0.9F, NOD, -0.5F, LEAN, -0.22F, SINK, -0.6F, R_ARM,
                        -0.7F, STAFF, 0.3F, HAND, 1.4F, LIFT, 3.0F),
                k(34, L_ARM, -0.55F, L_ELBOW, 0.2F, L_WRIST, 0.6F, LEAN, 0.5F, SINK, 2.4F, NOD, 0.35F, TWIST, -0.2F,
                        R_ARM, 0.2F, HAND, 1.6F, R_KNEE, 0.6F, L_KNEE, 0.7F, FLARE, 0.5F),
                hold(38, L_ARM, -0.6F, L_ELBOW, 0.25F, L_WRIST, 0.55F, LEAN, 0.45F, SINK, 2.2F, NOD, 0.3F, TWIST,
                        -0.15F, R_ARM, 0.15F, HAND, 0.8F, R_KNEE, 0.55F, L_KNEE, 0.6F),
                k(44) };
        // The staff raised high, his left hand cupped beneath it, then both flung wide as the familiars burst free.
        MOVES[WizardMove.FAMILIARS] = new Key[] { k(0),
                k(10, R_ARM, -2.4F, STAFF, 0.2F, L_ARM, -1.0F, L_IN, 0.7F, L_ELBOW, 1.2F, L_WRIST, -0.6F, NOD,
                        -0.25F, GLOW, 0.8F, HAND, 0.6F),
                k(20, R_ARM, -2.7F, STAFF, 0.25F, L_ARM, -1.6F, L_IN, 0.9F, L_ELBOW, 1.0F, L_WRIST, -0.7F, NOD,
                        -0.4F, LIFT, 1.5F, GLOW, 1.2F, HAND, 1.0F),
                k(22, R_ARM, -2.0F, R_OUT, 0.6F, STAFF, 0.4F, L_ARM, -2.0F, L_OUT, 1.2F, L_IN, -0.3F, NOD, -0.5F,
                        LEAN, -0.2F, GLOW, 1.8F, HAND, 1.5F, FLARE, 0.7F),
                hold(28, R_ARM, -1.9F, R_OUT, 0.55F, STAFF, 0.4F, L_ARM, -1.9F, L_OUT, 1.1F, L_IN, -0.2F, NOD,
                        -0.4F, LEAN, -0.15F, GLOW, 1.0F),
                k(36) };
        // Braced wide, both hands on the staff levelled before him, leaning into the torrent and shaking with it.
        MOVES[WizardMove.BEAM] = new Key[] { k(0),
                k(10, R_ARM, -0.8F, STAFF, 0.5F, L_ARM, -0.9F, L_IN, 0.9F, L_ELBOW, 1.3F, SINK, 1.2F, L_LEG, -0.45F,
                        L_KNEE, 0.4F, R_LEG, 0.45F, R_KNEE, 0.3F, LEAN, 0.15F, GLOW, 1.0F),
                k(24, R_ARM, -1.3F, R_ELBOW, -0.8F, STAFF, 1.3F, L_ARM, -1.3F, L_IN, 1.0F, L_ELBOW, 0.6F, LEAN,
                        0.25F, SINK, 1.7F, L_LEG, -0.5F, L_KNEE, 0.45F, R_LEG, 0.5F, R_KNEE, 0.35F, GLOW, 2.2F),
                k(26, R_ARM, -1.15F, R_ELBOW, -0.75F, STAFF, 1.2F, L_ARM, -1.2F, L_IN, 1.0F, L_ELBOW, 0.6F, LEAN,
                        0.05F, SINK, 1.5F, L_LEG, -0.5F, L_KNEE, 0.45F, R_LEG, 0.5F, R_KNEE, 0.35F, GLOW, 3.0F,
                        FLARE, 0.6F),
                hold(78, R_ARM, -1.25F, R_ELBOW, -0.8F, STAFF, 1.3F, L_ARM, -1.25F, L_IN, 1.0F, L_ELBOW, 0.6F,
                        LEAN, 0.28F, SINK, 1.8F, L_LEG, -0.55F, L_KNEE, 0.5F, R_LEG, 0.55F, R_KNEE, 0.4F, GLOW, 3.0F,
                        FLARE, 0.4F),
                k(82, R_ARM, -0.6F, STAFF, 0.6F, L_ARM, -0.3F, LEAN, 0.4F, SINK, 2.0F, NOD, 0.3F, R_KNEE, 0.4F,
                        L_KNEE, 0.4F),
                k(90) };
        // Both hands and the staff thrust at the sky, rising off his feet, then dragged down as the meteor comes.
        MOVES[WizardMove.METEOR] = new Key[] { k(0),
                k(10, R_ARM, -2.8F, R_ELBOW, -0.5F, STAFF, 0.1F, L_ARM, -2.6F, L_IN, 0.6F, NOD, -0.5F, LEAN, -0.2F,
                        SINK, -0.3F, GLOW, 1.5F),
                k(22, R_ARM, -2.9F, R_ELBOW, -0.4F, STAFF, 0.1F, L_ARM, -2.75F, L_IN, 0.6F, NOD, -0.6F, LEAN, -0.25F,
                        GLOW, 2.0F, LIFT, 3.0F, FLARE, 0.5F),
                k(26, R_ARM, -1.0F, R_ELBOW, -0.4F, STAFF, 1.0F, L_ARM, -0.9F, L_OUT, 0.6F, L_WRIST, 0.6F, LEAN,
                        0.38F, SINK, 1.7F, NOD, 0.25F, GLOW, 2.4F, R_KNEE, 0.4F, L_KNEE, 0.5F),
                hold(34, R_ARM, -0.9F, R_ELBOW, -0.35F, STAFF, 0.9F, L_ARM, -0.8F, L_OUT, 0.55F, L_WRIST, 0.5F, LEAN,
                        0.3F, SINK, 1.4F, NOD, 0.2F, GLOW, 1.2F, R_KNEE, 0.35F, L_KNEE, 0.45F),
                k(50) };
        // The staff planted at his side, his left hand drawing a circle in the air and turning palm up to hold it.
        MOVES[WizardMove.WARP] = new Key[] { k(0),
                k(6, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.3F, L_ELBOW, 0.6F, HAND, 0.6F),
                k(16, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.4F, L_ELBOW, 0.5F, HAND, 1.0F, GLOW, 0.8F),
                k(20, R_ARM, -0.35F, STAFF, -0.2F, L_ARM, -1.6F, L_WRIST, -1.0F, L_OUT, 0.5F, NOD, -0.1F, GLOW, 1.4F,
                        SINK, -0.3F, HAND, 1.6F, FLARE, 0.4F),
                hold(27, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.5F, L_WRIST, -0.9F, L_OUT, 0.45F, GLOW, 1.0F, HAND,
                        1.0F),
                k(34) };
        // Arms raised, rising off the ground, then flung wide as the images burst out of him.
        MOVES[WizardMove.MIRRORS] = new Key[] { k(0),
                k(12, R_ARM, -2.5F, L_ARM, -2.3F, L_OUT, 0.7F, NOD, -0.3F, SINK, -0.4F, GLOW, 1.2F),
                k(20, R_ARM, -2.8F, L_ARM, -2.6F, L_OUT, 0.6F, NOD, -0.4F, LIFT, 3.0F, GLOW, 2.0F),
                k(22, R_ARM, -1.4F, R_OUT, 1.0F, L_ARM, -1.4F, L_OUT, 1.2F, LEAN, -0.25F, NOD, -0.3F, FLARE, 1.0F,
                        GLOW, 2.4F),
                k(30) };
        // Down on a knee, curled; rising into the air, arms opening; curled again, then flung up as the phase bursts.
        MOVES[WizardMove.ASCEND] = new Key[] { k(0),
                k(10, SINK, 3.0F, LEAN, 0.5F, NOD, 0.5F, R_ARM, -0.4F, L_ARM, -0.4F, L_IN, 0.6F, L_ELBOW, 1.4F,
                        R_LEG, -1.0F, R_KNEE, 1.3F, L_LEG, 0.3F, L_KNEE, 1.6F, GLOW, 1.0F),
                k(30, LIFT, 8.0F, LEAN, -0.1F, NOD, -0.2F, R_ARM, -1.2F, R_OUT, 0.6F, L_ARM, -1.2F, L_OUT, 0.8F,
                        R_LEG, 0.15F, L_LEG, -0.1F, R_KNEE, 0.3F, L_KNEE, 0.3F, FLARE, 0.6F, GLOW, 1.5F),
                k(44, LIFT, 10.0F, LEAN, 0.4F, NOD, 0.45F, R_ARM, -0.6F, R_IN, 0.6F, L_ARM, -0.6F, L_IN, 0.8F,
                        L_ELBOW, 1.2F, R_KNEE, 0.8F, L_KNEE, 0.8F, R_LEG, -0.5F, L_LEG, -0.5F, GLOW, 2.0F),
                k(46, LIFT, 9.0F, LEAN, -0.35F, NOD, -0.65F, R_ARM, -2.6F, R_OUT, 0.8F, L_ARM, -2.6F, L_OUT, 1.0F,
                        L_WRIST, -0.5F, FLARE, 1.6F, GLOW, 3.0F),
                hold(58, LIFT, 8.0F, LEAN, -0.2F, NOD, -0.4F, R_ARM, -2.2F, R_OUT, 0.7F, L_ARM, -2.2F, L_OUT, 0.9F,
                        FLARE, 1.0F, GLOW, 1.8F),
                k(70) };
        // Doubled over, clutching himself as the magic tears at him, shaking harder; then torn open wide.
        MOVES[WizardMove.UNBIND] = new Key[] { k(0),
                k(8, LEAN, 0.6F, NOD, 0.6F, SINK, 1.5F, R_ARM, -0.8F, R_IN, 0.8F, R_ELBOW, 0.6F, L_ARM, -0.9F, L_IN,
                        1.0F, L_ELBOW, 1.6F, R_KNEE, 0.5F, L_KNEE, 0.5F, GLOW, 1.0F),
                k(30, LEAN, 0.5F, NOD, 0.5F, SINK, 0.5F, LIFT, 4.0F, R_ARM, -0.9F, R_IN, 0.9F, R_ELBOW, 0.5F, L_ARM,
                        -1.0F, L_IN, 1.0F, L_ELBOW, 1.7F, R_KNEE, 0.6F, L_KNEE, 0.6F, GLOW, 1.6F),
                k(44, LEAN, 0.75F, NOD, 0.7F, SINK, 1.0F, LIFT, 5.0F, R_ARM, -0.7F, R_IN, 1.0F, R_ELBOW, 0.7F, L_ARM,
                        -0.8F, L_IN, 1.1F, L_ELBOW, 1.9F, R_KNEE, 1.0F, L_KNEE, 1.0F, R_LEG, -0.6F, L_LEG, -0.6F,
                        GLOW, 2.0F),
                k(46, LEAN, -0.35F, NOD, -0.75F, LIFT, 5.0F, R_ARM, -1.5F, R_OUT, 1.3F, L_ARM, -1.5F, L_OUT, 1.4F,
                        L_WRIST, -0.6F, FLARE, 2.0F, GLOW, 3.0F),
                hold(58, LEAN, -0.2F, NOD, -0.5F, LIFT, 4.0F, R_ARM, -1.3F, R_OUT, 1.1F, L_ARM, -1.3F, L_OUT, 1.2F,
                        FLARE, 1.2F, GLOW, 2.0F),
                k(70) };
        // Dazed on one knee, leaning on his staff, head hanging.
        MOVES[WizardMove.STUNNED] = new Key[] { k(0),
                k(5, SINK, 3.2F, LEAN, 0.45F, NOD, 0.55F, TILT, 0.3F, R_LEG, -0.9F, R_KNEE, 1.4F, L_LEG, 0.4F,
                        L_KNEE, 1.6F, R_ARM, -0.5F, STAFF, 0.3F, L_ARM, -0.2F, L_OUT, 0.3F),
                hold(42, SINK, 3.0F, LEAN, 0.4F, NOD, 0.5F, TILT, 0.25F, R_LEG, -0.9F, R_KNEE, 1.4F, L_LEG, 0.4F,
                        L_KNEE, 1.6F, R_ARM, -0.5F, STAFF, 0.3F, L_ARM, -0.15F, L_OUT, 0.3F),
                k(50) };
        // A quick glide: a dip to push off, swept along with his robe and cape streaming, a dip as he stops.
        MOVES[WizardMove.SKIP] = new Key[] { k(0),
                k(2, SINK, 1.4F, LEAN, 0.25F, R_KNEE, 0.5F, L_KNEE, 0.5F, R_LEG, -0.2F, L_LEG, -0.2F, L_ARM, -0.3F,
                        L_OUT, 0.3F),
                k(5, LEAN, 0.3F, LIFT, 2.0F, R_LEG, 0.35F, L_LEG, 0.2F, R_KNEE, 0.35F, L_KNEE, 0.5F, R_ARM, 0.3F,
                        STAFF, -0.5F, L_ARM, 0.4F, L_OUT, 0.7F, L_WRIST, -0.4F, FLARE, 0.9F, NOD, -0.1F),
                k(8, SINK, 1.0F, LEAN, -0.1F, R_KNEE, 0.4F, L_KNEE, 0.4F, L_ARM, 0.2F, L_OUT, 0.4F, FLARE, 0.3F),
                k(10) };
        // Called down out of his rift: sinking slowly, arms spread palms down, head bowed, legs hanging and robe
        // spread; gathering himself; landing on one knee with the staff driven into the ground; rising slowly and
        // levelling his hand at them.
        MOVES[WizardMove.ARRIVE] = new Key[] { k(0, R_ARM, -0.4F, R_OUT, 0.9F, STAFF, 0.3F, L_ARM, -0.4F, L_OUT, 1.1F,
                L_WRIST, 0.6F, NOD, 0.5F, R_LEG, 0.1F, L_LEG, 0.05F, R_KNEE, 0.25F, L_KNEE, 0.3F, FLARE, 0.7F,
                GLOW, 1.0F),
                k(40, R_ARM, -0.6F, R_OUT, 1.0F, STAFF, 0.4F, L_ARM, -0.6F, L_OUT, 1.2F, L_WRIST, 0.5F, NOD, 0.4F,
                        R_LEG, 0.15F, L_LEG, 0.05F, R_KNEE, 0.3F, L_KNEE, 0.35F, FLARE, 0.8F, GLOW, 1.4F),
                k(56, R_ARM, -2.2F, R_ELBOW, -0.4F, STAFF, 0.1F, L_ARM, -1.6F, L_OUT, 0.8F, NOD, -0.1F, R_LEG,
                        -0.5F, R_KNEE, 0.9F, L_LEG, 0.2F, L_KNEE, 1.0F, LEAN, 0.15F, FLARE, 0.5F, GLOW, 2.0F),
                k(62, R_ARM, -0.6F, R_ELBOW, 0.2F, STAFF, 0.3F, L_ARM, -0.7F, L_OUT, 1.3F, L_WRIST, -0.6F, NOD,
                        0.45F, LEAN, 0.5F, SINK, 5.5F, R_LEG, -1.3F, R_KNEE, 1.5F, L_LEG, 0.25F, L_KNEE, 1.9F,
                        FLARE, 1.4F, GLOW, 3.0F),
                hold(70, R_ARM, -0.6F, R_ELBOW, 0.2F, STAFF, 0.3F, L_ARM, -0.6F, L_OUT, 1.2F, L_WRIST, -0.5F, NOD,
                        0.4F, LEAN, 0.45F, SINK, 5.3F, R_LEG, -1.3F, R_KNEE, 1.5F, L_LEG, 0.25F, L_KNEE, 1.9F,
                        GLOW, 1.5F),
                k(80, R_ARM, -0.4F, STAFF, 0.2F, L_ARM, -0.3F, L_OUT, 0.4F, NOD, 0.1F, LEAN, 0.2F, SINK, 2.0F,
                        R_LEG, -0.4F, R_KNEE, 0.5F, L_KNEE, 0.6F, GLOW, 1.0F),
                k(88, L_ARM, -1.45F, L_IN, 0.25F, L_ELBOW, 0.15F, L_WRIST, -0.5F, NOD, -0.15F, LEAN, -0.05F,
                        TWIST, 0.2F, R_ARM, -0.3F, STAFF, 0.25F, GLOW, 1.4F, HAND, 1.0F),
                k(96) };
        // His death: reeling from the blow, staggering, sinking to his knees leaning on his staff, looking up as light
        // breaks out of him, arms and head thrown back, lifted off the ground as it pours out of him.
        float[] lifted = { NOD, -0.75F, LEAN, -0.3F, R_ARM, -1.8F, R_OUT, 1.15F, STAFF, 0.4F, L_ARM, -1.8F, L_OUT,
                1.2F, L_WRIST, -0.5F, R_LEG, 0.2F, L_LEG, 0.1F, R_KNEE, 0.4F, L_KNEE, 0.5F, FLARE, 1.4F, GLOW, 3.0F,
                LIFT, 22.0F };
        MOVES[WizardMove.DEATH] = new Key[] { k(0),
                k(4, LEAN, -0.35F, NOD, -0.5F, TWIST, 0.25F, SINK, -0.3F, R_ARM, -1.2F, R_OUT, 0.6F, STAFF, 0.6F,
                        L_ARM, -0.8F, L_OUT, 0.8F, R_LEG, -0.2F, L_LEG, 0.25F),
                k(12, LEAN, 0.35F, NOD, 0.3F, TWIST, -0.1F, SINK, 1.4F, R_ARM, -0.5F, STAFF, 0.4F, L_ARM, -0.6F,
                        L_IN, 0.6F, L_ELBOW, 1.5F, R_KNEE, 0.5F, L_KNEE, 0.6F, R_LEG, -0.3F, L_LEG, 0.2F),
                k(22, LEAN, 0.3F, NOD, 0.55F, TILT, 0.15F, SINK, 6.0F, R_ARM, -0.75F, R_ELBOW, -0.3F, STAFF, 0.25F,
                        L_ARM, -0.2F, L_OUT, 0.25F, L_ELBOW, 0.4F, R_LEG, -0.1F, R_KNEE, 1.57F, L_LEG, -0.05F, L_KNEE,
                        1.57F),
                hold(38, LEAN, 0.32F, NOD, 0.6F, TILT, 0.1F, SINK, 6.0F, R_ARM, -0.75F, R_ELBOW, -0.3F, STAFF, 0.25F,
                        L_ARM, -0.15F, L_OUT, 0.2F, L_ELBOW, 0.5F, R_LEG, -0.1F, R_KNEE, 1.57F, L_LEG, -0.05F,
                        L_KNEE, 1.57F, GLOW, 0.4F),
                k(46, LEAN, 0.0F, NOD, -0.35F, SINK, 6.0F, R_ARM, -1.0F, R_OUT, 0.5F, STAFF, 0.3F, L_ARM, -1.0F,
                        L_OUT, 0.6F, R_LEG, -0.1F, R_KNEE, 1.57F, L_LEG, -0.05F, L_KNEE, 1.57F, GLOW, 1.5F, FLARE,
                        0.4F),
                k(52, NOD, -0.8F, LEAN, -0.35F, SINK, 5.0F, R_ARM, -1.9F, R_OUT, 1.2F, STAFF, 0.4F, L_ARM, -1.9F,
                        L_OUT, 1.25F, L_WRIST, -0.6F, R_KNEE, 1.3F, L_KNEE, 1.3F, FLARE, 1.6F, GLOW, 3.0F),
                k(74, lifted), k(90, lifted) };
        // Flying round in the circle of mirrors: leaning forward, legs trailing, staff swept back.
        float[] glide = { LEAN, 0.35F, R_LEG, 0.4F, L_LEG, 0.25F, R_KNEE, 0.4F, L_KNEE, 0.5F, R_ARM, 0.35F, STAFF,
                -0.6F, L_ARM, 0.45F, L_OUT, 0.55F, FLARE, 0.8F, LIFT, 2.0F, NOD, -0.15F };
        MOVES[WizardMove.GLIDE] = new Key[] { k(0, glide), k(400, glide) };
        // A dash in out of the circle: coiled, then the staff swung through.
        MOVES[WizardMove.DASH] = new Key[] { k(0, glide),
                k(4, LEAN, 0.5F, R_ARM, 0.55F, R_OUT, 0.3F, TWIST, 0.5F, STAFF, -0.3F, L_ARM, 0.3F, L_OUT, 0.5F,
                        R_LEG, 0.5F, L_LEG, 0.3F, R_KNEE, 0.5F, L_KNEE, 0.5F, LIFT, 2.0F, FLARE, 1.0F),
                k(6, LEAN, 0.6F, R_ARM, -1.4F, R_IN, 1.2F, R_ELBOW, -0.8F, STAFF, 1.25F, TWIST, -0.7F, L_ARM, 0.5F,
                        L_OUT, 0.6F, R_LEG, 0.5F, L_LEG, 0.3F, LIFT, 2.0F, FLARE, 1.2F),
                k(12, glide) };
    }
}
