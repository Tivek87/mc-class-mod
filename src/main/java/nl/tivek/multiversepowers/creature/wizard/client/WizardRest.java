package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;

// How the Wizard holds himself at rest, on his channels (WizardPoses adds it): with no foe about, standing still, one
// of twelve small habits comes round in turn (IDLES: stroking his beard, looking round, tapping his staff, a stretch,
// peering into his crystal, dusting off his robe, settling his hat, a flame on his fingertip, a yawn, twisting his
// staff, muttering a charm, gazing up at the sky); and the poses of his pastimes (WizardPastimes), each faded in and
// out over its first and last moments: reading, sitting on his staff with his legs swinging, juggling, meditating
// cross-legged, walking through his rifts, stirring his cauldron, pointing after the wisp, drawing his circle,
// practising his spells.
final class WizardRest {
    static final int STROKE = 0;
    static final int LOOK = 1;
    static final int TAP = 2;
    static final int STRETCH = 3;
    static final int PEER = 4;
    static final int DUST = 5;
    static final int HAT = 6;
    static final int FLAME = 7;
    static final int YAWN = 8;
    static final int TWIRL = 9;
    static final int CHARM = 10;
    static final int STARGAZE = 11;
    static final int IDLES = 12;
    // Each habit has a slot of this many ticks and lasts HABIT of them.
    private static final float SLOT = 150.0F;
    private static final float HABIT = 90.0F;

    private WizardRest() {
    }

    // The habit he is in at `age` (ticks, his own) and how far through it (0 to 1); -1 between them.
    static int habit(float age) {
        float at = age % (SLOT * IDLES);
        return at % SLOT < HABIT ? (int) (at / SLOT) : -1;
    }

    static float through(float age) {
        return (age % (SLOT * IDLES)) % SLOT / HABIT;
    }

    static void idle(float[] c, float age) {
        int habit = habit(age);
        if (habit < 0) {
            return;
        }
        float f = through(age);
        float w = bump(f);
        float tick = f * HABIT;
        switch (habit) {
            case STROKE -> {
                float pull = 0.5F + 0.5F * Mth.sin(tick * 0.25F);
                c[L_ARM] += -1.3F * w;
                c[L_IN] += 0.55F * w;
                c[L_ELBOW] += 1.75F * w;
                c[L_WRIST] += (0.3F + 0.3F * pull) * w;
                c[NOD] += 0.12F * w;
                c[SINK] += -0.2F * pull * w;
            }
            case LOOK -> {
                float side = Mth.sin(f * Mth.TWO_PI);
                c[TWIST] += 0.3F * side * w;
                c[TILT] += 0.06F * side * w;
                c[NOD] += -0.05F * w;
            }
            case TAP -> {
                float knock = Mth.sin(f * Mth.PI * 6.0F);
                c[R_ARM] += (-0.12F + 0.08F * knock) * w;
                c[STAFF] += 0.06F * knock * w;
                c[NOD] += -0.08F * w;
            }
            case STRETCH -> {
                c[L_ARM] += -2.7F * w;
                c[L_OUT] += 0.25F * w;
                c[R_ARM] += -0.9F * w;
                c[R_OUT] += 0.5F * w;
                c[LEAN] += -0.18F * w;
                c[NOD] += -0.3F * w;
                c[LIFT] += 0.4F * w;
                c[R_FOOT] += 0.25F * w;
                c[L_FOOT] += 0.25F * w;
            }
            case PEER -> {
                c[R_ARM] += -0.45F * w;
                c[R_IN] += 0.4F * w;
                c[R_ELBOW] += 0.35F * w;
                c[STAFF] += -0.25F * w;
                c[NOD] += -0.12F * w;
                c[TILT] += 0.14F * w;
                c[TWIST] += 0.08F * w;
                c[GLOW] += 0.7F * w;
            }
            case DUST -> {
                float brush = Mth.sin(tick * 0.6F);
                c[L_ARM] += (-1.0F + 0.35F * brush) * w;
                c[L_IN] += 0.95F * w;
                c[L_ELBOW] += (1.5F - 0.3F * brush) * w;
                c[L_WRIST] += 0.4F * brush * w;
                c[NOD] += 0.2F * w;
                c[TWIST] += 0.1F * w;
            }
            case HAT -> {
                c[L_ARM] += -2.35F * w;
                c[L_IN] += 0.45F * w;
                c[L_ELBOW] += 1.5F * w;
                c[L_WRIST] += 0.3F * w;
                c[NOD] += 0.08F * w;
                c[TILT] += -0.06F * w;
            }
            case FLAME -> {
                c[L_ARM] += -1.1F * w;
                c[L_IN] += 0.3F * w;
                c[L_ELBOW] += 1.3F * w;
                c[L_WRIST] += -0.5F * w;
                c[HAND] += (1.0F + 0.3F * Mth.sin(tick * 0.9F)) * w;
                c[NOD] += 0.15F * w;
                c[TILT] += 0.1F * w;
            }
            case YAWN -> {
                float wide = bump(Mth.clamp(f * 1.6F - 0.1F, 0.0F, 1.0F));
                c[L_ARM] += -1.5F * wide;
                c[L_IN] += 0.6F * wide;
                c[L_ELBOW] += 2.0F * wide;
                c[NOD] += -0.35F * wide;
                c[LEAN] += -0.08F * wide;
                c[R_ARM] += -0.2F * w;
            }
            case TWIRL -> {
                // Turned to and fro where it stands, never wound round and run back.
                c[SPIN] += 0.7F * Mth.sin(f * Mth.TWO_PI * 2.0F) * w;
                c[R_ARM] += -0.5F * w;
                c[R_ELBOW] += 0.2F * w;
                c[STAFF] += 0.15F * w;
                c[R_WRIST] += 0.2F * Mth.sin(tick * 0.8F) * w;
                c[NOD] += 0.1F * w;
            }
            case CHARM -> {
                float round = tick * 0.4F;
                c[L_ARM] += (-0.9F + 0.12F * Mth.sin(round)) * w;
                c[L_IN] += (0.5F + 0.12F * Mth.cos(round)) * w;
                c[L_ELBOW] += 1.2F * w;
                c[L_WRIST] += 0.35F * Mth.sin(round * 1.3F) * w;
                c[HAND] += 0.5F * w;
                c[NOD] += 0.06F * w;
            }
            default -> {
                c[NOD] += -0.55F * w;
                c[LEAN] += -0.12F * w;
                c[L_ARM] += 0.2F * w;
                c[TILT] += 0.05F * Mth.sin(tick * 0.1F) * w;
            }
        }
    }

    static void pastime(float[] c, int move, double t, float age) {
        int release = WizardMove.release(move);
        int length = WizardMove.length(move);
        float w = Mth.clamp((float) t / 12.0F, 0.0F, 1.0F) * Mth.clamp((float) (length - t) / 14.0F, 0.0F, 1.0F);
        float on = (float) Mth.clamp((t - release) / 10.0, 0.0, 1.0) * w;
        float s = (float) t;
        switch (move) {
            case WizardMove.READ -> {
                c[L_ARM] += -1.0F * w;
                c[L_IN] += 0.45F * w;
                c[L_ELBOW] += 1.2F * w;
                c[L_WRIST] += -0.3F * w;
                c[NOD] += 0.38F * w;
                c[TILT] += 0.05F * Mth.sin(s * 0.03F) * w;
                float turn = bump(Mth.clamp(((s % 70.0F) - 20.0F) / 25.0F, 0.0F, 1.0F));
                c[L_IN] += 0.3F * turn * on;
                c[L_WRIST] += 0.4F * turn * on;
            }
            case WizardMove.PERCH -> {
                float reach = w - on;
                c[R_ARM] += -1.0F * reach - 0.4F * on;
                c[STAFF] += 1.4F * reach;
                c[R_OUT] += 0.5F * on;
                c[SINK] += 1.8F * on;
                c[R_LEG] += -1.45F * on;
                c[L_LEG] += -1.35F * on;
                c[R_KNEE] += (1.5F + 0.25F * Mth.sin(age * 0.12F)) * on;
                c[L_KNEE] += (1.4F + 0.25F * Mth.sin(age * 0.12F + Mth.PI)) * on;
                c[L_ARM] += -0.9F * on;
                c[L_ELBOW] += 1.3F * on;
                c[HAND] += (0.6F + 0.3F * Mth.sin(age * 0.2F)) * on;
                c[LEAN] += -0.05F * on;
                c[NOD] += 0.1F * on;
            }
            case WizardMove.JUGGLE -> {
                float set = w - on;
                c[R_ARM] += -0.6F * set;
                c[R_OUT] += 0.6F * set;
                c[SINK] += 0.6F * set;
                float toss = (float) ((t - release) / 22.0 * Mth.TWO_PI);
                c[R_ARM] += -0.5F * on;
                c[L_ARM] += -0.5F * on;
                c[R_ELBOW] += 1.0F * on;
                c[L_ELBOW] += 1.0F * on;
                c[R_WRIST] += 0.3F * Mth.sin(toss) * on;
                c[L_WRIST] += 0.3F * Mth.sin(toss + Mth.PI) * on;
                c[R_ARM] += 0.1F * Mth.sin(toss) * on;
                c[L_ARM] += 0.1F * Mth.sin(toss + Mth.PI) * on;
                c[NOD] += -0.28F * on;
                c[TILT] += 0.05F * Mth.sin(toss * 0.5F) * on;
            }
            case WizardMove.MEDITATE -> {
                float breath = Mth.sin(age * 0.05F);
                c[R_LEG] += -1.5F * on;
                c[L_LEG] += -1.5F * on;
                c[R_KNEE] += 2.3F * on;
                c[L_KNEE] += 2.3F * on;
                c[SINK] += 2.0F * on;
                c[R_ARM] += -0.7F * on;
                c[L_ARM] += -0.7F * on;
                c[R_OUT] += 0.35F * on;
                c[L_OUT] += 0.35F * on;
                c[R_ELBOW] += 0.5F * on;
                c[L_ELBOW] += 0.5F * on;
                c[HAND] += (0.5F + 0.3F * breath) * on;
                c[NOD] += 0.15F * on;
                c[LIFT] += 0.2F * breath * on;
                c[R_ARM] += -0.5F * (w - on);
                c[STAFF] += 1.0F * (w - on);
            }
            case WizardMove.PORTALS -> {
                float open = bump(Mth.clamp((s - 6.0F) / 16.0F, 0.0F, 1.0F));
                c[L_ARM] += -1.6F * open;
                c[R_ARM] += -1.2F * open;
                c[L_OUT] += 0.7F * open;
                c[R_OUT] += 0.6F * open;
                c[HAND] += 1.0F * open;
                c[GLOW] += 0.8F * open;
                for (float into : new float[] { 50.0F, 172.0F }) {
                    float walk = bump(Mth.clamp((s - into + 10.0F) / 18.0F, 0.0F, 1.0F));
                    float step = Mth.sin((s - into) * 0.6F);
                    c[R_LEG] += -0.4F * step * walk;
                    c[L_LEG] += 0.4F * step * walk;
                    c[LEAN] += 0.12F * walk;
                }
                float toss = bump(Mth.clamp((s - 100.0F) / 16.0F, 0.0F, 1.0F));
                c[L_ARM] += -1.4F * toss;
                c[L_ELBOW] += (0.6F - 0.6F * Mth.clamp((s - 108.0F) / 4.0F, 0.0F, 1.0F)) * toss;
                c[HAND] += 1.0F * toss;
            }
            case WizardMove.BREW -> {
                float stir = age * 0.25F;
                c[R_ARM] += -0.9F * w;
                c[R_ELBOW] += 0.9F * w;
                c[STAFF] += 1.0F * w;
                c[R_IN] += 0.25F * Mth.sin(stir) * on;
                c[R_OUT] += 0.15F * Mth.cos(stir) * on;
                c[LEAN] += 0.25F * w;
                c[NOD] += 0.35F * w;
                for (float drop : new float[] { 80.0F, 150.0F, 220.0F }) {
                    float add = bump(Mth.clamp((s - drop + 10.0F) / 14.0F, 0.0F, 1.0F));
                    c[L_ARM] += -1.3F * add;
                    c[L_IN] += 0.2F * add;
                    c[L_WRIST] += 0.5F * add;
                }
                float sniff = bump(Mth.clamp((s - length + 56.0F) / 24.0F, 0.0F, 1.0F));
                c[NOD] += -0.35F * sniff;
                c[LEAN] += -0.3F * sniff;
                c[TILT] += 0.12F * sniff;
            }
            case WizardMove.WISP -> {
                float point = age * 0.075F;
                c[L_ARM] += (-1.3F + 0.3F * Mth.sin(point)) * on;
                c[L_OUT] += (0.3F + 0.3F * Mth.cos(point)) * on;
                c[L_ELBOW] += 0.3F * on;
                c[HAND] += 0.5F * on;
                c[SINK] += 0.3F * Mth.sin(age * 0.2F) * on;
            }
            case WizardMove.SCRIBE -> {
                c[R_ARM] += -0.9F * on;
                c[R_ELBOW] += 0.4F * on;
                c[STAFF] += -1.2F * on;
                c[LEAN] += 0.3F * on;
                c[NOD] += 0.4F * on;
                float step = Mth.sin(age * 0.35F);
                c[R_LEG] += 0.18F * step * on;
                c[L_LEG] += -0.18F * step * on;
            }
            case WizardMove.PRACTICE -> {
                double at = (t - release) % 36.0;
                boolean staff = ((int) ((t - release) / 36.0)) % 2 == 0;
                float wind = bump(Mth.clamp((float) at / 12.0F, 0.0F, 1.0F)) * on;
                float cast = bump(Mth.clamp((float) (at - 10.0) / 14.0F, 0.0F, 1.0F)) * on;
                if (staff) {
                    c[R_ARM] += 0.25F * wind - 1.3F * cast;
                    c[STAFF] += -0.3F * wind + 1.3F * cast;
                    c[GLOW] += 0.6F * wind + 1.0F * cast;
                } else {
                    c[L_ARM] += -0.6F * wind - 1.5F * cast;
                    c[L_ELBOW] += 1.6F * wind;
                    c[HAND] += 0.8F * wind + 1.4F * cast;
                }
                c[TWIST] += (staff ? -0.3F : 0.3F) * cast;
                c[LEAN] += 0.15F * cast;
                c[SINK] += 0.6F * cast;
            }
            default -> {
            }
        }
    }

    // 0 at 0 and 1, rising and falling smoothly between.
    static float bump(float f) {
        if (f <= 0.0F || f >= 1.0F) {
            return 0.0F;
        }
        float s = Mth.sin(f * Mth.PI);
        return s * s;
    }
}
