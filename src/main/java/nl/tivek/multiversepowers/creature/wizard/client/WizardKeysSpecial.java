package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// The keyframes of his newer great spells (WizardKeys holds the rest and how they are read): glacial spikes, flame
// pillars and the rune sentry, and the special each phase opens with: the grand sigil, the rain of light, the solar
// flare, the eclipse and the judgment. The specials are slower and grander than his other moves: a long gathering, a
// pose held as the magic pours out of him, and the beats WizardPoses adds as each ring, wave or shaft goes.
final class WizardKeysSpecial {
    private WizardKeysSpecial() {
    }

    static void fill(Key[][] moves) {
        // The staff hoisted overhead in both hands; driven point first into the ground before him as he drops to a
        // knee, his left hand sweeping out along the ground after the spikes.
        float[] driven = { R_ARM, -0.75F, R_ELBOW, 0.2F, STAFF, 0.15F, L_ARM, -1.25F, L_OUT, 0.15F, L_ELBOW, 0.1F,
                L_WRIST, 0.65F, LEAN, 0.5F, SINK, 3.2F, NOD, 0.25F, R_LEG, -1.0F, R_KNEE, 1.35F, L_LEG, 0.3F, L_KNEE,
                1.75F, GLOW, 2.2F, HAND, 1.2F, FLARE, 0.8F };
        moves[WizardMove.GLACIAL] = new Key[] { k(0),
                k(8, R_ARM, -2.7F, R_ELBOW, -0.4F, STAFF, -0.2F, L_ARM, -2.5F, L_IN, 0.7F, L_ELBOW, 0.5F, NOD, -0.35F,
                        LEAN, -0.15F, SINK, -0.3F, GLOW, 1.0F),
                k(12, R_ARM, -2.9F, R_ELBOW, -0.3F, STAFF, -0.3F, L_ARM, -2.75F, L_IN, 0.7F, L_ELBOW, 0.4F, NOD, -0.45F,
                        LEAN, -0.2F, SINK, -0.6F, LIFT, 1.2F, GLOW, 1.4F),
                k(15, driven), hold(22, driven),
                k(30) };
        // Crouched low, both palms turned up as if lifting a weight out of the ground; rising, arms and head lifting
        // to the sky as the marks burn; then both fists clenched as the pillars burst up.
        moves[WizardMove.PILLARS] = new Key[] { k(0),
                k(8, L_ARM, -0.8F, L_OUT, 0.5F, L_ELBOW, 1.3F, L_WRIST, -0.9F, R_ARM, -0.4F, R_OUT, 0.4F, STAFF, 0.3F,
                        LEAN, 0.32F, SINK, 2.2F, NOD, 0.3F, R_KNEE, 0.7F, L_KNEE, 0.8F, R_LEG, -0.3F, L_LEG, -0.25F,
                        HAND, 0.8F, GLOW, 0.6F),
                k(16, L_ARM, -2.5F, L_OUT, 0.9F, L_ELBOW, 0.3F, L_WRIST, -0.6F, R_ARM, -2.3F, R_OUT, 0.5F, STAFF,
                        0.2F, LEAN, -0.18F, SINK, -0.5F, NOD, -0.45F, LIFT, 1.0F, HAND, 1.4F, GLOW, 1.8F),
                k(18, L_ARM, -2.2F, L_OUT, 0.8F, L_ELBOW, 0.7F, L_WRIST, 0.4F, R_ARM, -2.1F, R_OUT, 0.5F, STAFF, 0.3F,
                        LEAN, -0.1F, SINK, 0.4F, NOD, -0.3F, HAND, 2.2F, GLOW, 2.4F, FLARE, 0.9F),
                hold(26, L_ARM, -2.1F, L_OUT, 0.75F, L_ELBOW, 0.75F, L_WRIST, 0.35F, R_ARM, -2.0F, R_OUT, 0.45F,
                        STAFF, 0.3F, LEAN, -0.08F, SINK, 0.3F, NOD, -0.25F, HAND, 1.2F, GLOW, 1.4F, FLARE, 0.4F),
                k(32) };
        // The staff planted at his side; his left hand drawing a rune in the air before him (WizardPoses circles it),
        // then pushing it out to his side, where the sentry opens.
        moves[WizardMove.SENTRY] = new Key[] { k(0),
                k(6, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.6F, L_OUT, 0.35F, L_ELBOW, 0.7F, L_WRIST, -0.3F, NOD, 0.05F,
                        HAND, 0.8F, GLOW, 0.5F),
                k(17, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.7F, L_OUT, 0.4F, L_ELBOW, 0.6F, L_WRIST, -0.4F, HAND, 1.2F,
                        GLOW, 0.9F, TWIST, 0.15F),
                k(20, R_ARM, -0.35F, STAFF, -0.2F, L_ARM, -1.35F, L_OUT, 1.25F, L_ELBOW, 0.05F, L_WRIST, -0.85F, TWIST,
                        0.35F, LEAN, -0.05F, HAND, 1.8F, GLOW, 1.2F, FLARE, 0.5F),
                hold(26, R_ARM, -0.3F, STAFF, -0.2F, L_ARM, -1.3F, L_OUT, 1.2F, L_WRIST, -0.7F, TWIST, 0.3F, HAND,
                        1.0F),
                k(30) };
        // The grand sigil: the staff lifted in both hands high over his head as he rises onto his toes, his head
        // thrown back, trembling with the gathered power; then its foot driven into the ground at his side, his left
        // arm flung out low over the ground, as the circle burns in; a sharp push down with each ring (WizardPoses).
        float[] cut = { R_ARM, -0.55F, R_OUT, 0.35F, R_ELBOW, 0.1F, STAFF, 0.1F, L_ARM, -0.9F, L_OUT, 1.35F, L_WRIST,
                0.55F, LEAN, 0.22F, SINK, 1.4F, NOD, 0.2F, L_LEG, -0.35F, L_KNEE, 0.5F, R_LEG, 0.3F, R_KNEE, 0.3F,
                GLOW, 2.4F, HAND, 1.4F, FLARE, 1.4F };
        moves[WizardMove.SIGIL] = new Key[] { k(0),
                k(10, R_ARM, -2.85F, R_ELBOW, -0.3F, STAFF, 0.0F, L_ARM, -2.7F, L_IN, 0.75F, L_ELBOW, 0.5F, NOD, -0.6F,
                        LEAN, -0.22F, SINK, -0.6F, LIFT, 1.5F, GLOW, 1.6F),
                hold(19, R_ARM, -2.95F, R_ELBOW, -0.25F, STAFF, 0.0F, L_ARM, -2.8F, L_IN, 0.75F, L_ELBOW, 0.45F, NOD,
                        -0.7F, LEAN, -0.25F, SINK, -0.8F, LIFT, 2.0F, GLOW, 2.2F),
                k(22, cut), hold(46, cut),
                k(56) };
        // The rain of light: both arms spread to the sky, his head back, lifted off the ground as he calls it down;
        // then, as each shaft falls, one hand or the other swept down to point it (WizardPoses), over and over.
        float[] call = { R_ARM, -2.55F, R_OUT, 0.85F, R_ELBOW, -0.2F, STAFF, 0.3F, L_ARM, -2.6F, L_OUT, 0.95F, L_WRIST,
                -0.5F, NOD, -0.55F, LEAN, -0.18F, SINK, -0.3F, LIFT, 2.0F, GLOW, 2.2F, HAND, 1.4F, FLARE, 0.6F };
        moves[WizardMove.RAIN] = new Key[] { k(0),
                k(10, R_ARM, -1.6F, R_OUT, 0.5F, STAFF, 0.4F, L_ARM, -1.5F, L_OUT, 0.6F, NOD, -0.2F, SINK, 0.6F, GLOW,
                        0.8F, HAND, 0.6F),
                k(18, call), hold(58, call),
                k(64) };
        // The solar flare: curled in on himself, arms crossed over his chest, shaking as the fire builds in him; then
        // burst open, arms flung wide and down, head thrown back, a sun; braced with each wave (WizardPoses).
        float[] blaze = { R_ARM, -0.75F, R_OUT, 1.4F, R_ELBOW, -0.2F, STAFF, 0.6F, L_ARM, -0.7F, L_OUT, 1.45F, L_WRIST,
                -0.6F, NOD, -0.6F, LEAN, -0.3F, SINK, -0.4F, LIFT, 1.5F, GLOW, 3.0F, HAND, 2.0F, FLARE, 2.0F };
        moves[WizardMove.FLARE] = new Key[] { k(0),
                k(10, R_ARM, -1.0F, R_IN, 1.0F, R_ELBOW, 1.2F, STAFF, -0.3F, L_ARM, -1.1F, L_IN, 1.1F, L_ELBOW, 1.9F,
                        LEAN, 0.5F, NOD, 0.55F, SINK, 1.8F, R_KNEE, 0.6F, L_KNEE, 0.6F, R_LEG, -0.3F, L_LEG, -0.3F,
                        GLOW, 1.2F, HAND, 1.0F),
                hold(20, R_ARM, -1.05F, R_IN, 1.05F, R_ELBOW, 1.3F, STAFF, -0.3F, L_ARM, -1.15F, L_IN, 1.15F, L_ELBOW,
                        2.0F, LEAN, 0.6F, NOD, 0.65F, SINK, 2.2F, R_KNEE, 0.8F, L_KNEE, 0.8F, R_LEG, -0.4F, L_LEG,
                        -0.4F, GLOW, 2.0F, HAND, 1.6F),
                k(22, blaze), hold(52, blaze),
                k(60) };
        // The eclipse: his left hand raised slowly to the sky, fingers spread, then clenched and dragged down to his
        // chest, pulling the dark across the sun, bent under its weight; the staff struck down as the void erupts again.
        float[] dragged = { L_ARM, -0.9F, L_IN, 0.85F, L_ELBOW, 2.05F, L_WRIST, 0.3F, R_ARM, -0.4F, R_OUT, 0.3F,
                STAFF, 0.3F, LEAN, 0.35F, SINK, 1.4F, NOD, 0.35F, R_KNEE, 0.4F, L_KNEE, 0.5F, HAND, 1.8F, GLOW, 1.6F,
                FLARE, 0.6F };
        moves[WizardMove.ECLIPSE] = new Key[] { k(0),
                k(14, L_ARM, -2.85F, L_OUT, 0.25F, L_ELBOW, 0.1F, L_WRIST, -0.5F, R_ARM, -0.3F, STAFF, 0.2F, NOD,
                        -0.55F, LEAN, -0.15F, SINK, -0.2F, LIFT, 1.0F, HAND, 1.2F, GLOW, 1.2F),
                hold(21, L_ARM, -2.95F, L_OUT, 0.25F, L_WRIST, -0.4F, R_ARM, -0.3F, STAFF, 0.2F, NOD, -0.6F, LEAN,
                        -0.18F, LIFT, 1.2F, HAND, 1.6F, GLOW, 1.6F),
                k(24, dragged), hold(46, dragged),
                k(50, R_ARM, -0.9F, R_ELBOW, -0.2F, STAFF, 1.3F, L_ARM, -0.8F, L_IN, 0.7F, L_ELBOW, 1.8F, LEAN, 0.45F,
                        SINK, 2.0F, NOD, 0.3F, L_LEG, -0.4F, L_KNEE, 0.5F, GLOW, 2.0F, FLARE, 0.8F),
                hold(56, R_ARM, -0.85F, STAFF, 1.25F, L_ARM, -0.7F, L_IN, 0.6F, L_ELBOW, 1.6F, LEAN, 0.4F, SINK, 1.8F,
                        NOD, 0.25F, L_LEG, -0.35F, L_KNEE, 0.45F, GLOW, 1.2F),
                k(64) };
        // The judgment: his staff raised upright high over his head, his left palm laid flat on the air, lifted off
        // the ground and utterly still while time is (WizardPoses stills his breath and cloth); then the staff swung
        // down at them as the lances fall.
        float[] still = { R_ARM, -2.85F, R_ELBOW, -0.1F, STAFF, 0.05F, L_ARM, -1.5F, L_OUT, 0.35F, L_ELBOW, 0.1F,
                L_WRIST, 0.45F, NOD, -0.2F, LEAN, -0.08F, LIFT, 3.0F, GLOW, 2.6F, HAND, 1.6F, FLARE, 0.5F };
        moves[WizardMove.JUDGMENT] = new Key[] { k(0),
                k(10, R_ARM, -2.2F, R_ELBOW, 0.4F, STAFF, 0.2F, L_ARM, -1.0F, L_OUT, 0.6F, L_ELBOW, 0.5F, NOD, -0.3F,
                        SINK, 0.5F, GLOW, 1.0F),
                k(18, still), hold(58, still),
                k(62, R_ARM, -1.3F, R_ELBOW, -0.3F, STAFF, 1.5F, L_ARM, -0.6F, L_OUT, 0.9F, LEAN, 0.35F, SINK, 0.8F,
                        NOD, 0.15F, LIFT, 1.5F, GLOW, 3.0F, FLARE, 1.4F, L_LEG, -0.3F, R_LEG, 0.3F),
                hold(72, R_ARM, -1.25F, STAFF, 1.4F, L_ARM, -0.5F, L_OUT, 0.8F, LEAN, 0.28F, SINK, 0.6F, LIFT, 1.0F,
                        GLOW, 1.6F, FLARE, 0.6F),
                k(84) };
    }
}
