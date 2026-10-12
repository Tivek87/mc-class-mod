package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// The keyframes of his moves up close (WizardKeys holds the rest and how they are read): the three short spells, the
// staff's combo, whirl and plant, and the tether that drags a foe in to them. Each gathers with a pull back or a coil,
// strikes with his weight stepping into it, and settles through a follow-through rather than snapping back.
final class WizardKeysClose {
    private WizardKeysClose() {
    }

    static void fill(Key[][] moves) {
        // His left palm drawn in to his chest, his weight sinking back as the force gathers in it; then thrust out
        // with a step, the staff swung wide for balance, his robe flaring with the blast.
        moves[WizardMove.REPULSE] = new Key[] { k(0),
                k(5, L_ARM, -0.7F, L_IN, 0.9F, L_ELBOW, 2.0F, L_WRIST, -0.4F, LEAN, -0.12F, SINK, 1.3F, TWIST, 0.35F,
                        R_ARM, -0.3F, R_OUT, 0.25F, R_KNEE, 0.35F, L_KNEE, 0.25F, HAND, 0.9F, GLOW, 0.4F, NOD, 0.1F),
                hold(8, L_ARM, -0.75F, L_IN, 0.95F, L_ELBOW, 2.1F, L_WRIST, -0.5F, LEAN, -0.15F, SINK, 1.5F, TWIST,
                        0.4F, R_ARM, -0.3F, R_OUT, 0.3F, R_KNEE, 0.4F, L_KNEE, 0.3F, HAND, 1.3F, GLOW, 0.6F),
                k(10, L_ARM, -1.55F, L_IN, 0.25F, L_ELBOW, 0.0F, L_WRIST, -1.0F, LEAN, 0.32F, SINK, 1.6F, TWIST,
                        -0.35F, R_ARM, -0.6F, R_OUT, 1.0F, STAFF, 0.5F, L_LEG, -0.6F, L_KNEE, 0.55F, R_LEG, 0.4F,
                        HAND, 2.0F, GLOW, 1.4F, FLARE, 1.2F, NOD, -0.1F),
                hold(14, L_ARM, -1.5F, L_IN, 0.2F, L_ELBOW, 0.05F, L_WRIST, -0.9F, LEAN, 0.26F, SINK, 1.4F, TWIST,
                        -0.3F, R_ARM, -0.5F, R_OUT, 0.9F, STAFF, 0.4F, L_LEG, -0.55F, L_KNEE, 0.5F, R_LEG, 0.35F,
                        HAND, 1.0F, FLARE, 0.5F),
                k(20) };
        // His left hand opened at them, the other braced back on the staff, his feet set wide; the frost pours out of
        // his palm as he leans into it and his hand sweeps a little after them (WizardPoses).
        float[] breathe = { L_ARM, -1.5F, L_IN, 0.2F, L_ELBOW, 0.15F, L_WRIST, -0.75F, LEAN, 0.24F, SINK, 1.5F, TWIST,
                -0.25F, R_ARM, 0.15F, R_OUT, 0.45F, STAFF, -0.4F, L_LEG, -0.5F, L_KNEE, 0.5F, R_LEG, 0.45F, R_KNEE,
                0.3F, HAND, 1.5F, NOD, 0.05F };
        moves[WizardMove.FROST_CONE] = new Key[] { k(0),
                k(6, L_ARM, -1.0F, L_IN, 0.7F, L_ELBOW, 1.5F, L_WRIST, 0.4F, LEAN, -0.08F, SINK, 0.7F, TWIST, 0.3F,
                        R_ARM, 0.1F, R_OUT, 0.3F, HAND, 0.8F, NOD, 0.15F),
                k(10, breathe), hold(23, breathe),
                k(26, L_ARM, -0.9F, L_ELBOW, 0.6F, L_WRIST, -0.2F, LEAN, 0.1F, SINK, 0.9F, TWIST, -0.1F, L_LEG, -0.2F,
                        L_KNEE, 0.25F, HAND, 0.3F),
                k(30) };
        // The staff drawn up high over his right shoulder, his body wound round to the right; then whipped down and
        // across to his left, the lash of fire trailing from the crystal, and through past where it struck.
        moves[WizardMove.FLAME_LASH] = new Key[] { k(0),
                k(6, R_ARM, -2.3F, R_OUT, 0.8F, R_ELBOW, 0.9F, STAFF, -0.6F, R_WRIST, -0.5F, TWIST, 0.7F, LEAN, -0.15F,
                        SINK, 0.4F, L_ARM, -0.8F, L_OUT, 0.6F, R_LEG, 0.3F, L_LEG, -0.15F, GLOW, 1.0F, NOD, -0.1F),
                hold(10, R_ARM, -2.45F, R_OUT, 0.85F, R_ELBOW, 1.0F, STAFF, -0.7F, R_WRIST, -0.6F, TWIST, 0.8F, LEAN,
                        -0.18F, SINK, 0.5F, L_ARM, -0.85F, L_OUT, 0.65F, R_LEG, 0.3F, L_LEG, -0.15F, GLOW, 1.4F),
                k(12, R_ARM, -1.25F, R_OUT, 0.2F, R_IN, 0.9F, R_ELBOW, -0.4F, STAFF, 1.35F, R_WRIST, 0.3F, TWIST,
                        -0.4F, LEAN, 0.3F, SINK, 1.5F, L_ARM, 0.2F, L_OUT, 0.5F, L_LEG, -0.55F, L_KNEE, 0.5F, R_LEG,
                        0.45F, GLOW, 2.2F, FLARE, 0.6F),
                k(15, R_ARM, -0.8F, R_IN, 1.5F, R_ELBOW, -0.2F, STAFF, 1.2F, R_WRIST, 0.4F, TWIST, -0.95F, LEAN, 0.35F,
                        ROLL, 0.1F, SINK, 1.6F, L_ARM, 0.4F, L_OUT, 0.6F, L_LEG, -0.55F, L_KNEE, 0.55F, R_LEG, 0.4F,
                        R_KNEE, 0.3F, GLOW, 1.2F),
                k(19, R_ARM, -0.7F, R_IN, 1.3F, STAFF, 1.0F, TWIST, -0.7F, LEAN, 0.22F, ROLL, 0.06F, SINK, 1.1F, L_ARM,
                        0.2F, L_OUT, 0.4F, L_LEG, -0.3F, L_KNEE, 0.3F),
                k(24) };
        // Three blows: the staff hoisted overhead in both hands on tiptoe and brought down in a smash; wound across his
        // body and swept back out with the back of it; then drawn low and driven up into a rising thrust whose crystal
        // bursts, lifting him off his heels.
        float[] smash = { R_ARM, -0.95F, R_ELBOW, -0.2F, STAFF, 1.45F, R_WRIST, 0.3F, L_ARM, -1.05F, L_IN, 0.85F,
                L_ELBOW, 0.35F, LEAN, 0.48F, SINK, 2.3F, NOD, 0.2F, L_LEG, -0.55F, L_KNEE, 0.65F, R_LEG, 0.45F, R_KNEE,
                0.2F, GLOW, 1.4F, FLARE, 0.4F };
        moves[WizardMove.COMBO] = new Key[] { k(0),
                k(5, R_ARM, -2.9F, R_ELBOW, 0.7F, STAFF, -0.7F, R_WRIST, -0.4F, L_ARM, -2.6F, L_IN, 0.8F, L_ELBOW,
                        0.9F, LEAN, -0.22F, SINK, -0.4F, NOD, -0.35F, R_LEG, 0.2F, GLOW, 0.8F),
                k(9, smash), hold(11, smash),
                k(15, R_ARM, -1.05F, R_IN, 1.35F, R_ELBOW, 0.5F, STAFF, 1.0F, R_WRIST, -0.3F, TWIST, -0.75F, LEAN,
                        0.18F, SINK, 1.3F, L_ARM, 0.25F, L_OUT, 0.6F, NOD, 0.05F, R_LEG, 0.25F, L_LEG, -0.2F),
                k(19, R_ARM, -1.25F, R_OUT, 1.15F, R_IN, -0.55F, R_ELBOW, -0.3F, STAFF, 1.25F, R_WRIST, 0.35F, TWIST,
                        0.85F, LEAN, 0.2F, ROLL, -0.08F, SINK, 1.5F, L_ARM, 0.3F, L_OUT, 0.7F, R_LEG, -0.4F, R_KNEE,
                        0.45F, L_LEG, 0.3F, GLOW, 1.2F, FLARE, 0.3F),
                k(22, R_ARM, -1.0F, R_OUT, 1.2F, R_IN, -0.7F, STAFF, 1.1F, TWIST, 0.95F, LEAN, 0.12F, ROLL, -0.06F,
                        SINK, 1.2F, L_ARM, 0.2F, L_OUT, 0.6F, R_LEG, -0.35F, R_KNEE, 0.4F),
                k(27, R_ARM, 0.35F, R_OUT, 0.2F, R_ELBOW, 1.5F, STAFF, 0.4F, R_WRIST, -0.3F, TWIST, 0.55F, LEAN, 0.05F,
                        SINK, 2.2F, L_ARM, -0.7F, L_OUT, 0.5F, L_ELBOW, 0.6F, R_LEG, 0.35F, R_KNEE, 0.7F, L_LEG, -0.35F,
                        L_KNEE, 0.6F, GLOW, 1.6F, NOD, 0.15F),
                k(31, R_ARM, -2.05F, R_ELBOW, -0.3F, STAFF, 1.6F, R_WRIST, 0.35F, TWIST, -0.45F, LEAN, -0.12F, SINK,
                        -0.4F, LIFT, 1.0F, L_ARM, 0.35F, L_OUT, 0.7F, R_LEG, 0.5F, L_LEG, -0.4F, L_KNEE, 0.2F, GLOW,
                        2.8F, FLARE, 1.0F, NOD, -0.25F),
                hold(35, R_ARM, -1.95F, R_ELBOW, -0.2F, STAFF, 1.55F, TWIST, -0.4F, LEAN, -0.08F, SINK, -0.2F, LIFT,
                        0.6F, L_ARM, 0.3F, L_OUT, 0.6F, R_LEG, 0.4F, L_LEG, -0.3F, GLOW, 1.4F, FLARE, 0.4F, NOD,
                        -0.2F),
                k(42) };
        // The staff held out flat at arm's length, his body coiled the other way and sinking; then he spins round and
        // round on the spot (the turn is WizardPoses.whirl), the staff sweeping all round him, leaning out against it,
        // and unwinds to a stop.
        float[] spin = { R_ARM, -1.45F, R_OUT, 1.35F, R_ELBOW, -0.2F, STAFF, 1.55F, R_WRIST, 0.2F, L_ARM, -0.9F, L_OUT,
                1.25F, LEAN, 0.12F, ROLL, -0.1F, SINK, 1.5F, R_KNEE, 0.35F, L_KNEE, 0.35F, GLOW, 1.4F, FLARE, 1.3F,
                NOD, -0.1F };
        moves[WizardMove.WHIRL] = new Key[] { k(0),
                k(6, R_ARM, -1.2F, R_OUT, 0.6F, R_IN, 0.9F, R_ELBOW, 0.4F, STAFF, 1.3F, TWIST, -0.9F, L_ARM, -0.6F,
                        L_OUT, 0.9F, LEAN, 0.2F, SINK, 1.8F, R_KNEE, 0.5F, L_KNEE, 0.5F, GLOW, 0.8F),
                k(10, spin), hold(28, spin),
                k(32, R_ARM, -1.0F, R_OUT, 0.9F, STAFF, 1.0F, L_ARM, -0.4F, L_OUT, 0.8F, LEAN, 0.05F, ROLL, 0.08F,
                        SINK, 1.2F, R_KNEE, 0.25F, L_KNEE, 0.3F, FLARE, 0.4F),
                k(36) };
        // The staff hoisted upright in both hands on his toes; then driven foot first into the ground at his side as
        // he drops into a crouch over it, both hands on it; held while the ground shakes, then he straightens.
        float[] driven = { R_ARM, -0.7F, R_IN, -0.35F, R_ELBOW, -1.25F, STAFF, -0.75F, L_ARM, -0.75F, L_IN, 0.75F,
                L_ELBOW, 0.5F, LEAN, 0.3F, SINK, 2.2F, NOD, 0.18F, R_KNEE, 0.5F, L_KNEE, 0.6F, L_LEG, -0.35F,
                R_LEG, 0.15F, GLOW, 1.8F, FLARE, 0.8F };
        moves[WizardMove.STAFF_PLANT] = new Key[] { k(0),
                k(7, R_ARM, -2.2F, R_ELBOW, -0.65F, STAFF, 0.75F, L_ARM, -2.1F, L_IN, 0.6F, L_ELBOW, 0.6F, LEAN,
                        -0.15F, SINK, -0.3F, LIFT, 0.3F, NOD, -0.2F, GLOW, 0.8F),
                hold(10, R_ARM, -2.35F, R_ELBOW, -0.7F, STAFF, 0.85F, L_ARM, -2.25F, L_IN, 0.6F, L_ELBOW, 0.55F, LEAN,
                        -0.18F, SINK, -0.4F, LIFT, 0.4F, NOD, -0.25F, GLOW, 1.1F),
                k(13, driven), hold(19, driven),
                k(25, R_ARM, -0.4F, R_IN, -0.2F, R_ELBOW, -0.8F, STAFF, -0.45F, L_ARM, -0.3F, L_IN, 0.3F, LEAN, 0.1F,
                        SINK, 0.8F, R_KNEE, 0.2F, L_KNEE, 0.2F),
                k(30) };
        // The staff thrust at them as the chain flies out of its crystal; then wrenched back as he braces against the
        // pull, his left hand reaching out after it, his foe dragged in.
        moves[WizardMove.TETHER] = new Key[] { k(0),
                k(6, R_ARM, -0.4F, R_ELBOW, 1.0F, STAFF, 0.1F, R_WRIST, -0.4F, TWIST, 0.35F, LEAN, -0.08F, SINK, 0.6F,
                        GLOW, 0.8F),
                k(11, R_ARM, -1.55F, R_ELBOW, -0.4F, STAFF, 1.5F, R_WRIST, 0.3F, TWIST, -0.3F, LEAN, 0.3F, SINK, 1.2F,
                        L_LEG, -0.45F, L_KNEE, 0.45F, R_LEG, 0.35F, GLOW, 1.8F, FLARE, 0.4F),
                k(15, R_ARM, 0.2F, R_ELBOW, 1.6F, STAFF, 0.6F, R_WRIST, -0.2F, TWIST, 0.55F, LEAN, -0.3F, SINK, 1.5F,
                        L_ARM, -1.3F, L_OUT, 0.2F, L_WRIST, -0.6F, R_LEG, 0.5F, R_KNEE, 0.5F, L_LEG, -0.35F, L_KNEE,
                        0.2F, GLOW, 1.4F, HAND, 1.0F, NOD, -0.1F),
                hold(21, R_ARM, 0.1F, R_ELBOW, 1.5F, STAFF, 0.55F, TWIST, 0.45F, LEAN, -0.2F, SINK, 1.3F, L_ARM,
                        -1.2F, L_WRIST, -0.5F, R_LEG, 0.45F, R_KNEE, 0.45F, L_LEG, -0.3F, GLOW, 1.0F, HAND, 0.7F),
                k(30) };
    }
}
