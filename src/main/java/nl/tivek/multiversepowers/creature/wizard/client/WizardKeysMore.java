package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// The keyframes of the moves his later phases brought (WizardKeys holds the rest and how they are read): the spells of
// all five phases added since, the moves that carry him into his fourth and fifth, and his blink in the air.
final class WizardKeysMore {
    private WizardKeysMore() {
    }

    static void fill(Key[][] moves) {
        // The staff raised and twirled over his head, his left hand drawing the orbs round him; then levelled at them.
        moves[WizardMove.ORBIT] = new Key[] { k(0),
                k(8, R_ARM, -2.5F, R_ELBOW, -0.6F, STAFF, 0.2F, L_ARM, -1.8F, L_OUT, 0.6F, L_ELBOW, 0.3F, NOD, -0.3F,
                        LEAN, -0.1F, GLOW, 1.0F, SPIN, 3.0F),
                k(16, R_ARM, -2.7F, R_ELBOW, -0.7F, STAFF, 0.25F, L_ARM, -2.2F, L_OUT, 0.9F, NOD, -0.35F, LEAN,
                        -0.12F, GLOW, 1.6F, SPIN, 9.0F, LIFT, 1.0F),
                k(19, R_ARM, -1.2F, R_ELBOW, -0.6F, STAFF, 1.2F, L_ARM, -1.3F, L_OUT, 1.1F, L_WRIST, -0.6F, LEAN,
                        0.2F, SINK, 0.9F, NOD, 0.05F, GLOW, 2.0F, FLARE, 0.6F, SPIN, 12.56F, L_LEG, -0.3F, L_KNEE,
                        0.3F),
                hold(30, R_ARM, -1.1F, R_ELBOW, -0.5F, STAFF, 1.1F, L_ARM, -1.2F, L_OUT, 1.0F, L_WRIST, -0.5F, LEAN,
                        0.15F, SINK, 0.7F, GLOW, 1.2F, SPIN, 12.56F, L_LEG, -0.25F, L_KNEE, 0.25F),
                k(40, SPIN, 12.56F) };
        // His left hand raised high, palm up, calling the lances out of the air behind him; then swept down at them.
        moves[WizardMove.LANCES] = new Key[] { k(0),
                k(8, L_ARM, -2.4F, L_OUT, 0.9F, L_WRIST, -0.6F, L_ELBOW, 0.2F, R_ARM, -0.5F, STAFF, 0.4F, NOD, -0.25F,
                        LEAN, -0.12F, TWIST, 0.25F, HAND, 1.0F, GLOW, 0.8F, SINK, -0.2F),
                k(15, L_ARM, -2.6F, L_OUT, 1.0F, L_WRIST, -0.8F, NOD, -0.3F, LEAN, -0.15F, TWIST, 0.3F, HAND, 1.4F,
                        GLOW, 1.2F, LIFT, 0.8F),
                k(18, L_ARM, -1.45F, L_OUT, 0.1F, L_IN, 0.3F, L_ELBOW, 0.05F, L_WRIST, 0.3F, LEAN, 0.25F, TWIST,
                        -0.35F, SINK, 0.8F, HAND, 1.6F, NOD, 0.1F, L_LEG, -0.35F, L_KNEE, 0.35F, R_LEG, 0.2F, FLARE,
                        0.5F),
                hold(28, L_ARM, -1.4F, L_IN, 0.3F, L_WRIST, 0.2F, LEAN, 0.2F, TWIST, -0.3F, SINK, 0.6F, HAND, 1.0F,
                        L_LEG, -0.3F, L_KNEE, 0.3F, R_LEG, 0.15F),
                k(34) };
        // The staff raised overhead in both hands, a leap, and its foot driven into the ground as he lands crouched.
        float[] driven = { R_ARM, -0.5F, R_ELBOW, 0.1F, STAFF, 0.0F, L_ARM, -0.6F, L_IN, 0.7F, L_ELBOW, 0.6F, LEAN,
                0.5F, SINK, 3.6F, NOD, 0.35F, R_LEG, -1.0F, R_KNEE, 1.3F, L_LEG, 0.25F, L_KNEE, 1.8F, GLOW, 2.4F,
                FLARE, 1.0F };
        moves[WizardMove.NOVA] = new Key[] { k(0),
                k(6, R_ARM, -2.7F, R_ELBOW, -0.6F, STAFF, -0.2F, L_ARM, -2.5F, L_IN, 0.6F, L_ELBOW, 0.5F, NOD, -0.3F,
                        SINK, 0.6F, GLOW, 1.0F, R_KNEE, 0.4F, L_KNEE, 0.4F),
                k(12, R_ARM, -2.95F, R_ELBOW, -0.5F, STAFF, -0.25F, L_ARM, -2.8F, L_IN, 0.6F, L_ELBOW, 0.4F, NOD,
                        -0.45F, SINK, -0.8F, LIFT, 1.5F, LEAN, -0.15F, GLOW, 1.6F, FLARE, 0.4F),
                k(16, driven), hold(22, driven), k(30) };
        // His left hand circling palm down, stirring up the wind, his body winding round with it; then flung out.
        moves[WizardMove.TORNADO] = new Key[] { k(0),
                k(6, L_ARM, -1.3F, L_OUT, 0.5F, L_WRIST, 0.5F, TWIST, 0.4F, R_ARM, -0.4F, STAFF, 0.3F, HAND, 0.6F,
                        LEAN, 0.05F),
                k(22, L_ARM, -1.6F, L_OUT, 0.8F, L_WRIST, 0.6F, TWIST, 0.55F, LEAN, 0.1F, HAND, 1.2F, SINK, 0.5F,
                        GLOW, 1.0F, FLARE, 0.5F),
                k(25, L_ARM, -1.5F, L_IN, 0.4F, L_OUT, -0.1F, L_ELBOW, 0.05F, L_WRIST, -0.4F, TWIST, -0.5F, LEAN,
                        0.3F, SINK, 1.0F, HAND, 1.6F, FLARE, 0.9F, L_LEG, -0.4F, L_KNEE, 0.4F),
                hold(32, L_ARM, -1.4F, L_IN, 0.35F, L_WRIST, -0.3F, TWIST, -0.4F, LEAN, 0.25F, SINK, 0.8F, HAND,
                        1.0F, FLARE, 0.5F),
                k(40) };
        // A crouch and a spring up; hanging over them, leaning in with the staff aimed down; a dive head first in
        // flames; the landing on one knee, the staff driven down; rising.
        float[] diving = { LEAN, 1.2F, NOD, 0.7F, R_ARM, -2.0F, R_ELBOW, -0.4F, STAFF, 1.6F, L_ARM, 0.6F, L_OUT, 0.5F,
                R_LEG, 0.9F, L_LEG, 0.8F, R_KNEE, 0.5F, L_KNEE, 0.6F, FLARE, 1.4F, GLOW, 2.4F };
        float[] aiming = { LEAN, 0.8F, NOD, 0.55F, R_ARM, -1.7F, R_ELBOW, -0.6F, STAFF, 1.7F, L_ARM, -0.5F, L_OUT,
                1.0F, R_LEG, 0.7F, L_LEG, 0.5F, R_KNEE, 0.9F, L_KNEE, 1.1F, FLARE, 0.7F, GLOW, 2.0F, LIFT, 1.0F };
        float[] kneel = { LEAN, 0.5F, SINK, 4.0F, NOD, 0.4F, R_ARM, -0.6F, R_ELBOW, 0.2F, STAFF, 0.3F, L_ARM, -0.6F,
                L_OUT, 1.2F, L_WRIST, -0.6F, R_LEG, -1.2F, R_KNEE, 1.5F, L_LEG, 0.25F, L_KNEE, 1.9F, FLARE, 1.6F, GLOW,
                2.6F };
        moves[WizardMove.COMET] = new Key[] { k(0),
                k(5, SINK, 2.0F, LEAN, 0.4F, R_KNEE, 0.9F, L_KNEE, 0.9F, R_LEG, -0.5F, L_LEG, -0.5F, R_ARM, 0.3F,
                        L_ARM, 0.4F, L_OUT, 0.4F, NOD, 0.2F),
                k(10, SINK, -1.0F, LIFT, 2.0F, LEAN, -0.2F, R_ARM, -2.6F, R_ELBOW, -0.5F, STAFF, 0.2F, L_ARM, -2.4F,
                        L_OUT, 0.6F, R_LEG, 0.3F, L_LEG, 0.1F, R_KNEE, 0.6F, L_KNEE, 0.9F, NOD, -0.4F, FLARE, 0.8F,
                        GLOW, 1.2F),
                k(16, aiming), hold(25, aiming), k(28, diving), hold(34, diving), k(38, kneel), hold(46, kneel),
                k(56) };
        // Arms raised to the sky as the fire climbs his hands, then both thrust down and out as the ring leaps up.
        float[] thrust = { L_ARM, -1.1F, L_OUT, 1.3F, L_WRIST, 0.7F, R_ARM, -1.0F, R_OUT, 1.1F, STAFF, 0.8F, LEAN, 0.35F,
                SINK, 1.8F, NOD, 0.3F, HAND, 2.0F, GLOW, 2.2F, FLARE, 1.0F, R_KNEE, 0.5F, L_KNEE, 0.5F };
        moves[WizardMove.INFERNO] = new Key[] { k(0),
                k(8, L_ARM, -0.9F, L_OUT, 0.7F, L_WRIST, -0.9F, L_ELBOW, 0.8F, R_ARM, -0.6F, R_OUT, 0.4F, STAFF, 0.2F,
                        SINK, 0.8F, LEAN, 0.1F, HAND, 0.8F, GLOW, 0.8F),
                k(18, L_ARM, -2.6F, L_OUT, 1.0F, L_WRIST, -0.6F, L_ELBOW, 0.2F, R_ARM, -2.5F, R_OUT, 0.6F, STAFF,
                        0.1F, NOD, -0.45F, LEAN, -0.2F, SINK, -0.3F, HAND, 1.5F, GLOW, 1.8F, LIFT, 0.8F),
                k(21, thrust), hold(28, thrust), k(36) };
        // Hunched as the dark folds round him; gone, and over them, the staff raised high in both hands, arched back;
        // swung down with all his weight onto them; crouched where he lands.
        float[] raised = { LEAN, -0.35F, NOD, -0.45F, R_ARM, -3.0F, R_ELBOW, -0.3F, STAFF, -0.5F, L_ARM, -2.8F, L_IN,
                0.6F, L_ELBOW, 0.35F, R_LEG, 0.35F, L_LEG, -0.25F, R_KNEE, 1.0F, L_KNEE, 0.7F, FLARE, 0.8F, GLOW, 2.0F };
        float[] slammed = { LEAN, 0.6F, SINK, 3.0F, NOD, 0.4F, R_ARM, -0.4F, R_ELBOW, 0.1F, STAFF, 0.6F, L_ARM, -0.5F,
                L_IN, 0.5F, L_ELBOW, 0.5F, R_LEG, -1.1F, R_KNEE, 1.4F, L_LEG, 0.3F, L_KNEE, 1.8F, FLARE, 1.2F, GLOW,
                1.6F };
        moves[WizardMove.PHANTOM] = new Key[] { k(0),
                k(5, LEAN, 0.4F, SINK, 1.4F, NOD, 0.45F, R_ARM, -0.3F, R_IN, 0.5F, L_ARM, -0.9F, L_IN, 0.8F, L_ELBOW,
                        1.4F, R_KNEE, 0.5F, L_KNEE, 0.5F, GLOW, 1.0F),
                k(8, raised), hold(17, raised),
                k(20, LEAN, 0.7F, NOD, 0.4F, R_ARM, -0.4F, R_ELBOW, 0.1F, STAFF, 0.6F, L_ARM, -0.5F, L_IN, 0.5F,
                        L_ELBOW, 0.5F, R_LEG, -0.6F, L_LEG, 0.4F, R_KNEE, 0.8F, L_KNEE, 1.2F, SINK, 2.0F, FLARE, 1.6F,
                        GLOW, 2.6F),
                k(23, slammed), hold(28, slammed), k(34) };
        // Clawing at the ground with his left hand, crouched low, shaking with the effort; then his fist torn upward
        // as the tendrils burst free.
        moves[WizardMove.TENDRILS] = new Key[] { k(0),
                k(8, L_ARM, -0.5F, L_ELBOW, 1.3F, L_WRIST, 0.9F, L_OUT, 0.4F, R_ARM, -0.2F, STAFF, 0.1F, LEAN, 0.35F,
                        SINK, 1.4F, NOD, 0.45F, HAND, 0.8F, R_KNEE, 0.4F, L_KNEE, 0.5F),
                k(18, L_ARM, -0.35F, L_ELBOW, 1.6F, L_WRIST, 1.1F, L_OUT, 0.5F, LEAN, 0.45F, SINK, 2.0F, NOD, 0.55F,
                        HAND, 1.2F, R_KNEE, 0.6F, L_KNEE, 0.7F, GLOW, 1.2F, TILT, 0.1F),
                k(21, L_ARM, -2.0F, L_ELBOW, 1.2F, L_WRIST, -0.2F, L_OUT, 0.3F, LEAN, -0.2F, SINK, 0.3F, NOD, -0.35F,
                        TWIST, 0.25F, HAND, 1.8F, GLOW, 2.0F, FLARE, 0.8F),
                hold(28, L_ARM, -1.9F, L_ELBOW, 1.1F, L_WRIST, -0.1F, L_OUT, 0.3F, LEAN, -0.15F, SINK, 0.4F, NOD,
                        -0.25F, TWIST, 0.2F, HAND, 1.2F, GLOW, 1.4F),
                k(36) };
        // The staff held across his chest in both hands as the dark gathers at its crystal, drawn back, then thrust
        // forward to send the orb on its way.
        float[] pushed = { R_ARM, -1.6F, R_ELBOW, -0.9F, R_IN, 0.3F, STAFF, 1.5F, L_ARM, -1.6F, L_IN, 0.6F, L_ELBOW,
                0.1F, L_WRIST, -0.8F, LEAN, 0.35F, SINK, 1.2F, GLOW, 2.8F, HAND, 2.0F, FLARE, 1.0F, L_LEG, -0.45F,
                L_KNEE, 0.45F, R_LEG, 0.3F };
        moves[WizardMove.VOID_ORB] = new Key[] { k(0),
                k(10, R_ARM, -1.0F, R_ELBOW, 0.2F, R_IN, 0.6F, STAFF, 1.1F, L_ARM, -1.1F, L_IN, 0.9F, L_ELBOW, 1.2F,
                        LEAN, -0.05F, SINK, 0.6F, NOD, 0.2F, GLOW, 1.0F, HAND, 0.8F),
                k(24, R_ARM, -0.8F, R_ELBOW, 0.5F, R_IN, 0.8F, STAFF, 0.9F, L_ARM, -0.9F, L_IN, 1.0F, L_ELBOW, 1.5F,
                        LEAN, -0.15F, SINK, 1.0F, NOD, 0.3F, GLOW, 2.0F, HAND, 1.4F),
                k(27, pushed), hold(36, pushed), k(44) };
        // His left hand held out at them, clawed, his head tipped back as their life runs into him.
        float[] drinking = { L_ARM, -1.5F, L_OUT, 0.25F, L_WRIST, -1.0F, LEAN, -0.25F, NOD, -0.4F, TILT, 0.2F, HAND,
                1.4F, GLOW, 1.2F, FLARE, 0.3F, LIFT, 0.8F };
        moves[WizardMove.DRAIN] = new Key[] { k(0),
                k(10, L_ARM, -1.5F, L_OUT, 0.3F, L_ELBOW, 0.1F, L_WRIST, -0.9F, R_ARM, -0.3F, STAFF, -0.2F, LEAN,
                        -0.05F, HAND, 1.0F, GLOW, 0.8F, NOD, 0.1F),
                k(18, L_ARM, -1.55F, L_OUT, 0.25F, L_WRIST, -1.1F, LEAN, -0.2F, NOD, -0.3F, TILT, 0.15F, HAND, 1.6F,
                        GLOW, 1.4F, FLARE, 0.4F, SINK, -0.3F, LIFT, 0.5F),
                hold(62, drinking), k(70) };
        // Rising with his arms spread wide and the staff high, turning as the stars spiral out of him.
        float[] spread = { R_ARM, -2.4F, R_OUT, 0.9F, L_ARM, -1.4F, L_OUT, 1.6F, L_WRIST, -0.6F, NOD, -0.2F, LIFT, 3.0F,
                GLOW, 2.2F, FLARE, 1.3F };
        moves[WizardMove.SPIRAL] = new Key[] { k(0),
                k(10, R_ARM, -2.6F, R_OUT, 0.4F, STAFF, 0.2F, L_ARM, -1.5F, L_OUT, 1.4F, L_WRIST, -0.6F, NOD, -0.3F,
                        LIFT, 1.5F, GLOW, 1.5F, FLARE, 0.8F),
                k(14, spread), hold(56, spread), k(64) };
        // The staff raised high in both hands, the star kindling over it, holding it up as it grows, trembling more and
        // more, head thrown back; braced as it is about to burst; arms flung wide as it does.
        float[] holding = { R_ARM, -2.95F, R_ELBOW, -0.4F, STAFF, 0.1F, L_ARM, -2.85F, L_OUT, 0.3F, L_ELBOW, 0.3F,
                L_WRIST, -0.7F, NOD, -0.6F, LEAN, -0.15F, GLOW, 2.0F, FLARE, 0.8F, LIFT, 2.0F };
        float[] flung = { R_ARM, -1.8F, R_OUT, 1.3F, STAFF, 0.6F, L_ARM, -1.8F, L_OUT, 1.4F, L_WRIST, -0.6F, NOD, -0.8F,
                LEAN, -0.4F, SINK, -0.5F, GLOW, 3.5F, FLARE, 2.0F, LIFT, 3.0F };
        moves[WizardMove.SUPERNOVA] = new Key[] { k(0),
                k(12, R_ARM, -2.8F, R_ELBOW, -0.5F, STAFF, 0.1F, L_ARM, -2.6F, L_IN, 0.5F, L_ELBOW, 0.6F, NOD, -0.4F,
                        LEAN, -0.1F, GLOW, 1.5F, FLARE, 0.6F, LIFT, 1.0F),
                k(24, holding),
                k(96, R_ARM, -3.0F, R_ELBOW, -0.3F, STAFF, 0.1F, L_ARM, -2.95F, L_OUT, 0.35F, L_ELBOW, 0.2F, L_WRIST,
                        -0.8F, NOD, -0.75F, LEAN, -0.25F, GLOW, 3.0F, FLARE, 1.2F, LIFT, 2.5F, SINK, 0.4F),
                k(102, R_ARM, -2.6F, R_ELBOW, -0.6F, STAFF, 0.2F, L_ARM, -2.4F, L_OUT, 0.5F, NOD, -0.4F, LEAN, 0.1F,
                        SINK, 1.6F, GLOW, 3.0F, R_KNEE, 0.6F, L_KNEE, 0.6F, LIFT, 2.0F),
                k(105, flung),
                hold(118, R_ARM, -1.6F, R_OUT, 1.2F, STAFF, 0.5F, L_ARM, -1.6F, L_OUT, 1.3F, NOD, -0.5F, LEAN, -0.25F,
                        GLOW, 1.6F, FLARE, 1.0F, LIFT, 2.0F),
                k(132) };
        // The void waking in him: his left hand clutching his face, doubled over and shaking, lifted off the ground;
        // then thrown open as it bursts out of him.
        moves[WizardMove.AWAKEN] = new Key[] { k(0),
                k(10, LEAN, 0.5F, NOD, 0.7F, SINK, 1.2F, L_ARM, -2.3F, L_IN, 1.0F, L_ELBOW, 2.0F, R_ARM, -0.4F, R_IN,
                        0.4F, STAFF, -0.3F, R_KNEE, 0.4F, L_KNEE, 0.5F, GLOW, 0.8F, TILT, 0.2F),
                k(30, LEAN, 0.6F, NOD, 0.8F, SINK, 0.8F, LIFT, 3.0F, L_ARM, -2.4F, L_IN, 1.0F, L_ELBOW, 2.1F, R_ARM,
                        -0.5F, R_IN, 0.5F, STAFF, -0.3F, R_KNEE, 0.8F, L_KNEE, 0.9F, R_LEG, -0.4F, L_LEG, -0.4F, GLOW,
                        1.6F, TILT, -0.2F),
                k(44, LEAN, 0.8F, NOD, 0.9F, SINK, 1.2F, LIFT, 4.0F, L_ARM, -2.2F, L_IN, 1.1F, L_ELBOW, 2.2F, R_ARM,
                        -0.6F, R_IN, 0.7F, R_KNEE, 1.1F, L_KNEE, 1.1F, R_LEG, -0.7F, L_LEG, -0.7F, GLOW, 2.2F),
                k(47, LEAN, -0.45F, NOD, -0.85F, LIFT, 5.0F, R_ARM, -1.7F, R_OUT, 1.4F, STAFF, 0.5F, L_ARM, -1.7F,
                        L_OUT, 1.5F, L_WRIST, -0.8F, R_LEG, 0.2F, L_LEG, -0.1F, FLARE, 2.2F, GLOW, 3.2F),
                hold(60, LEAN, -0.25F, NOD, -0.5F, LIFT, 4.0F, R_ARM, -1.4F, R_OUT, 1.2F, L_ARM, -1.4F, L_OUT, 1.3F,
                        FLARE, 1.2F, GLOW, 2.0F),
                k(70) };
        // His last phase: lifted slowly and high, arms opening palms up, face to the sky; drawn in a moment, then
        // flung wide as the light takes him.
        moves[WizardMove.APOTHEOSIS] = new Key[] { k(0),
                k(16, LIFT, 3.0F, NOD, -0.2F, R_ARM, -0.6F, R_OUT, 0.8F, STAFF, 0.3F, L_ARM, -0.6F, L_OUT, 0.9F,
                        L_WRIST, -1.0F, R_LEG, 0.1F, L_LEG, 0.1F, R_KNEE, 0.2F, L_KNEE, 0.2F, GLOW, 1.2F, FLARE, 0.5F),
                k(40, LIFT, 8.0F, NOD, -0.5F, LEAN, -0.1F, R_ARM, -1.3F, R_OUT, 1.2F, STAFF, 0.4F, L_ARM, -1.3F,
                        L_OUT, 1.3F, L_WRIST, -1.1F, R_LEG, 0.15F, L_LEG, 0.15F, R_KNEE, 0.3F, L_KNEE, 0.3F, GLOW, 2.0F,
                        FLARE, 0.9F),
                k(44, LIFT, 7.5F, NOD, 0.3F, LEAN, 0.35F, R_ARM, -0.5F, R_IN, 0.6F, L_ARM, -0.6F, L_IN, 0.8F, L_ELBOW,
                        1.3F, R_KNEE, 0.9F, L_KNEE, 0.9F, GLOW, 2.5F),
                k(47, LIFT, 9.0F, NOD, -0.8F, LEAN, -0.4F, R_ARM, -2.7F, R_OUT, 0.9F, STAFF, 0.3F, L_ARM, -2.7F,
                        L_OUT, 1.0F, L_WRIST, -0.6F, FLARE, 2.4F, GLOW, 3.5F),
                hold(66, LIFT, 8.0F, NOD, -0.5F, LEAN, -0.2F, R_ARM, -2.3F, R_OUT, 0.8F, L_ARM, -2.3F, L_OUT, 0.9F,
                        FLARE, 1.4F, GLOW, 2.2F),
                k(84) };
        // Folded in on himself for a breath, and flung open somewhere else in the air.
        moves[WizardMove.AIRSTEP] = new Key[] { k(0),
                k(2, LEAN, 0.3F, SINK, 1.0F, NOD, 0.3F, R_ARM, -0.3F, R_IN, 0.5F, L_ARM, -0.7F, L_IN, 0.8F, L_ELBOW,
                        1.2F, R_KNEE, 0.6F, L_KNEE, 0.6F, GLOW, 1.0F),
                k(5, LEAN, -0.2F, NOD, -0.2F, R_ARM, 0.2F, R_OUT, 0.4F, L_ARM, -0.6F, L_OUT, 1.0F, L_WRIST, -0.5F,
                        FLARE, 1.2F, GLOW, 1.4F),
                k(10) };
    }
}
