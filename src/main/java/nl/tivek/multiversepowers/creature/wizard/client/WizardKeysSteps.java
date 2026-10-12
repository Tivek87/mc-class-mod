package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// The keyframes of his ways of going, his staff's moves and his comings and goings between air and ground (WizardKeys
// holds the rest and how they are read): landing on bent knees with his arms out, a crouch and a spring into the air,
// a still watch with his hand at his chin, the staff drawn back and hurled as a spear, his open hand calling it home
// and the jolt as he catches it, the staff tossed up spinning to fight on its own, the flicks of the flicker, a rift
// swept open and walked into, the crouch of the veil and the blow out of nowhere, and rifts called open round his foe.
final class WizardKeysSteps {
    private WizardKeysSteps() {
    }

    static void fill(Key[][] moves) {
        // Legs reaching down, arms out for balance; touching down on bent knees, then rising.
        moves[WizardMove.ALIGHT] = new Key[] { k(0),
                k(8, L_OUT, 0.5F, R_OUT, 0.3F, R_LEG, -0.2F, L_LEG, 0.1F, R_KNEE, 0.3F, L_KNEE, 0.4F, FLARE, 0.6F,
                        NOD, 0.15F),
                k(17, L_OUT, 0.55F, R_OUT, 0.3F, R_LEG, -0.1F, R_KNEE, 0.15F, L_KNEE, 0.2F, FLARE, 0.9F, NOD, 0.2F,
                        R_FOOT, 0.2F, L_FOOT, 0.2F),
                k(20, SINK, 2.4F, R_KNEE, 0.9F, L_KNEE, 1.0F, LEAN, 0.25F, L_OUT, 0.6F, R_OUT, 0.35F, NOD, 0.2F,
                        FLARE, 0.8F),
                k(24) };
        // A crouch with the crystal kindling, then a spring up, arms flung up and out, toes pointed.
        moves[WizardMove.TAKEOFF] = new Key[] { k(0),
                hold(6, SINK, 2.2F, R_KNEE, 0.9F, L_KNEE, 0.9F, LEAN, 0.3F, R_ARM, 0.3F, L_ARM, 0.4F, L_OUT, 0.3F,
                        NOD, 0.2F, GLOW, 0.6F),
                k(9, SINK, -0.8F, LIFT, 0.8F, LEAN, -0.15F, R_ARM, -0.6F, L_ARM, -1.2F, L_OUT, 0.9F, R_FOOT, 0.6F,
                        L_FOOT, 0.6F, FLARE, 1.2F, NOD, -0.2F, GLOW, 1.0F),
                k(14, LIFT, 0.3F, L_OUT, 0.5F, FLARE, 0.6F),
                k(18) };
        // He stops and only watches them, weighing them: his hand at his chin, his head tipped.
        moves[WizardMove.WATCH] = new Key[] { k(0),
                hold(8, NOD, -0.05F, TILT, 0.12F, L_ARM, -1.1F, L_IN, 0.6F, L_ELBOW, 1.9F, L_WRIST, 0.3F, LEAN, -0.04F,
                        STAFF, -0.1F),
                hold(30, NOD, 0.03F, TILT, -0.08F, L_ARM, -1.05F, L_IN, 0.62F, L_ELBOW, 1.95F, L_WRIST, 0.35F,
                        LEAN, -0.02F, STAFF, -0.1F),
                k(40) };
        // The staff drawn back high over his shoulder like a spear, then hurled with a step and a turn of his body.
        moves[WizardMove.STAFF_THROW] = new Key[] { k(0),
                k(6, R_ARM, -2.3F, R_OUT, 0.3F, R_ELBOW, 1.3F, STAFF, 1.4F, TWIST, 0.6F, LEAN, -0.15F, SINK, 0.8F,
                        L_ARM, -1.2F, L_OUT, 0.3F, R_LEG, 0.3F, L_LEG, -0.2F, GLOW, 0.8F),
                hold(12, R_ARM, -2.6F, R_OUT, 0.35F, R_ELBOW, 1.5F, STAFF, 1.5F, TWIST, 0.75F, LEAN, -0.2F, SINK,
                        1.0F, L_ARM, -1.4F, L_OUT, 0.35F, R_LEG, 0.35F, L_LEG, -0.25F, GLOW, 1.3F),
                k(15, R_ARM, -1.1F, R_ELBOW, 0.0F, R_WRIST, 0.4F, TWIST, -0.6F, LEAN, 0.45F, SINK, 1.6F, L_ARM, 0.4F,
                        L_OUT, 0.4F, L_LEG, -0.6F, L_KNEE, 0.5F, R_LEG, 0.4F, R_FOOT, 0.4F, FLARE, 0.8F),
                k(20, R_ARM, -0.7F, TWIST, -0.4F, LEAN, 0.25F, SINK, 1.0F, L_LEG, -0.4F, L_KNEE, 0.35F, R_LEG, 0.25F),
                k(26) };
        // His open hand held out to his staff, calling it; the jolt as it slaps into his palm.
        moves[WizardMove.STAFF_CALL] = new Key[] { k(0),
                k(5, R_ARM, -1.6F, R_OUT, 0.4F, R_ELBOW, 0.1F, R_WRIST, -0.6F, TWIST, 0.2F, LEAN, -0.05F, NOD, -0.05F,
                        GLOW, 0.6F),
                hold(WizardMove.CALL_CATCH - 2, R_ARM, -1.7F, R_OUT, 0.45F, R_ELBOW, 0.05F, R_WRIST, -0.7F, GLOW,
                        1.0F),
                k(WizardMove.CALL_CATCH + 1, R_ARM, -1.0F, R_ELBOW, 0.6F, R_OUT, 0.2F, SINK, 1.0F, LEAN, -0.1F,
                        STAFF, 0.4F, GLOW, 1.4F),
                k(24) };
        // The staff raised overhead and set spinning, then tossed up free, both his arms flung up to send it.
        moves[WizardMove.STAFF_FREE] = new Key[] { k(0),
                k(8, R_ARM, -2.7F, R_ELBOW, 0.3F, STAFF, -0.2F, L_ARM, -1.0F, L_OUT, 0.6F, NOD, -0.3F, GLOW, 1.2F,
                        SPIN, 2.0F),
                hold(16, R_ARM, -2.9F, R_ELBOW, 0.2F, L_ARM, -1.2F, L_OUT, 0.7F, NOD, -0.35F, GLOW, 1.6F, SPIN, 6.0F,
                        LIFT, 0.3F),
                k(19, R_ARM, -2.4F, R_OUT, 0.9F, R_ELBOW, 0.0F, L_ARM, -2.4F, L_OUT, 0.9F, NOD, -0.4F, FLARE, 1.0F,
                        HAND, 1.0F),
                hold(26, R_ARM, -1.3F, R_OUT, 0.8F, L_ARM, -1.3F, L_OUT, 0.8F, NOD, -0.1F, HAND, 0.6F),
                k(30) };
        // Three quick hops, each landing in a crouch with a flick of the staff at them.
        moves[WizardMove.FLICKER] = new Key[] { k(0),
                k(4, SINK, 0.8F, LEAN, 0.2F, R_ARM, -0.6F, STAFF, 0.6F, GLOW, 0.8F),
                k(7, R_ARM, -1.0F, STAFF, 1.2F, R_WRIST, 0.4F, TWIST, -0.25F, GLOW, 1.2F, HAND, 0.6F),
                k(10, SINK, 0.8F, LEAN, 0.2F, R_ARM, -0.6F, STAFF, 0.6F, GLOW, 0.8F),
                k(13, R_ARM, -1.0F, STAFF, 1.2F, R_WRIST, 0.4F, TWIST, 0.25F, GLOW, 1.2F),
                k(16, SINK, 0.8F, LEAN, 0.2F, R_ARM, -0.6F, STAFF, 0.6F, GLOW, 0.8F),
                k(19, R_ARM, -1.0F, STAFF, 1.2F, R_WRIST, 0.4F, TWIST, -0.2F, GLOW, 1.2F),
                k(24) };
        // A sweep of his left hand tears the rift open; he walks into it, and steps out of the other with a flourish.
        moves[WizardMove.RIFT_STEP] = new Key[] { k(0),
                k(6, L_ARM, -1.5F, L_OUT, 0.6F, L_IN, -0.3F, L_WRIST, -0.6F, HAND, 1.0F, TWIST, 0.3F, GLOW, 0.6F),
                k(10, L_ARM, -1.4F, L_IN, 0.7F, HAND, 1.2F, TWIST, -0.2F),
                k(14, R_LEG, -0.45F, L_LEG, 0.3F, LEAN, 0.15F, L_ARM, -0.4F, HAND, 0.5F),
                k(17, R_LEG, 0.25F, L_LEG, -0.45F, L_KNEE, 0.4F, LEAN, 0.2F),
                k(20, R_LEG, -0.4F, L_LEG, 0.3F, LEAN, 0.25F),
                hold(WizardMove.RIFT_OUT - 1, R_LEG, -0.35F, L_LEG, 0.25F, LEAN, 0.2F),
                k(31, R_LEG, 0.2F, L_LEG, -0.2F, L_ARM, -1.2F, L_OUT, 0.9F, R_ARM, -0.6F, R_OUT, 0.5F, FLARE, 0.8F,
                        NOD, -0.1F),
                k(40) };
        // He folds into himself and is gone; behind them he is coiled low, and swings round with his staff.
        moves[WizardMove.VEIL] = new Key[] { k(0),
                k(6, SINK, 1.2F, LEAN, 0.3F, R_ARM, 0.2F, L_ARM, -0.6F, L_IN, 0.8F, L_ELBOW, 1.5F, NOD, 0.3F),
                hold(WizardMove.VEIL_BACK - 2, SINK, 1.6F, LEAN, 0.4F, R_ARM, 0.4F, R_OUT, 0.4F, STAFF, -0.4F, TWIST,
                        0.8F, R_KNEE, 0.5F, L_KNEE, 0.6F),
                k(WizardMove.VEIL_BACK + 1, R_ARM, -0.6F, R_OUT, 0.9F, R_IN, -0.6F, R_ELBOW, 0.3F, STAFF, 1.0F, TWIST,
                        0.9F, LEAN, 0.1F, SINK, 1.2F),
                k(WizardMove.VEIL_STRIKE, R_ARM, -1.1F, R_IN, 1.4F, R_OUT, 0.3F, R_ELBOW, 0.6F, STAFF, 1.3F, TWIST,
                        -0.9F, LEAN, 0.35F, SINK, 1.5F, L_LEG, -0.5F, L_KNEE, 0.4F, FLARE, 0.6F),
                k(40) };
        // Both arms raised wide, calling the rifts open round them, then thrust at them as the sparks fly.
        moves[WizardMove.RIFT_VOLLEY] = new Key[] { k(0),
                k(8, L_ARM, -2.2F, L_OUT, 0.8F, R_ARM, -2.0F, R_OUT, 0.6F, NOD, -0.25F, GLOW, 1.0F, HAND, 1.0F, SINK,
                        0.4F),
                hold(15, L_ARM, -2.5F, L_OUT, 0.9F, R_ARM, -2.2F, R_OUT, 0.7F, NOD, -0.3F, GLOW, 1.4F, HAND, 1.4F),
                k(18, L_ARM, -1.5F, L_OUT, 0.2F, L_ELBOW, 0.0F, R_ARM, -1.3F, STAFF, 1.2F, NOD, 0.0F, LEAN, 0.2F,
                        HAND, 1.6F, GLOW, 1.6F, FLARE, 0.9F),
                hold(34, L_ARM, -1.4F, L_OUT, 0.2F, R_ARM, -1.2F, STAFF, 1.1F, LEAN, 0.15F, HAND, 1.0F, GLOW, 1.0F),
                k(40) };
    }
}
