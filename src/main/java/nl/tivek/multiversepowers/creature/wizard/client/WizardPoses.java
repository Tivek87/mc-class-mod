package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// How the Wizard holds himself this frame (WizardModel, StaffLayer), on his channels (WizardKeys): upright and a
// little stooped, breathing slow, his staff upright before him in his right hand; walking he plants the staff with
// every other step, his robe swinging; flying (from his second phase) he drifts with his legs hanging and his robe
// trailing, leaning into where he flies, arms lifting as he drops and trailing as he climbs. At rest with no foe about
// his habits and pastimes come round (WizardRest); standing, his weight shifts from foot to foot, and his feet roll
// heel to toe as he walks; struck, he flinches. On top his moves'
// keyframes, and what no keyframe holds: the staff swung round in circles, the trembling as great magic passes through
// him, the kick of each bolt, the circle his hand draws for the time warp and the tornado, the turning of the star
// spiral, the clutch of a draining hand, the sway of a stunned head. All of it
// followed on springs, each channel its own speed, so he never snaps from one pose into the next; beard, hat-tip and
// robe trail after.
final class WizardPoses {
    // Each channel on a spring of its own weight: the trunk and hips heavy and slow, swinging a little past; the head
    // and the arms quicker; elbows, wrists and the staff quicker again; legs and feet firm, so they never slide.
    private static final double[] SPEED = { 0.22, 0.24, 0.22, 0.22, 0.26, 0.26, 0.34, 0.34, 0.36, 0.36, 0.28, 0.28,
            0.28, 0.28, 0.28, 0.28, 0.32, 0.32, 0.38, 0.38, 0.34, 0.34, 0.34, 0.34, 0.2, 0.3, 0.36, 0.36 };
    private static final double[] DAMPING = { 0.68, 0.72, 0.66, 0.66, 0.62, 0.62, 0.72, 0.8, 0.9, 0.9, 0.72, 0.72,
            0.72, 0.72, 0.72, 0.72, 0.7, 0.7, 0.68, 0.68, 0.85, 0.85, 0.85, 0.85, 0.7, 0.8, 0.8, 0.8 };
    private static final PoseSprings SPRINGS = new PoseSprings(SPEED, DAMPING);
    private static final PoseSprings CLOTH = new PoseSprings(0.09, 0.32);
    // The whirl turns him round this many times between its first and last blow.
    private static final float WHIRL_TURNS = 3.0F;

    final float[] c = new float[CHANNELS];
    private final float[] cloth = new float[8];
    float cape;
    float capeSide;
    float beard;
    float beardSide;
    float hat;
    float hatSide;
    float skirt;
    float skirtSide;
    // How fast he climbs (blocks a tick, below 0 as he drops), for his flight.
    private float rise;

    void of(MovingMonster caster, float limbSwing, float amount, float age, boolean floating) {
        int move = caster.move();
        double t = caster.moveAge(age - caster.tickCount);
        // Utterly still while he holds time still: no breath, no sway.
        boolean still = move == WizardMove.JUDGMENT && t > 18.0 && t < 58.0;
        float breath = still ? 0.0F : Mth.sin(age * 0.07F + caster.getId());
        // His weight shifting slowly from one foot to the other as he stands.
        float shift = still ? 0.0F : Mth.sin(age * 0.021F + caster.getId() * 1.7F);
        java.util.Arrays.fill(this.c, 0.0F);
        this.c[LEAN] = 0.06F - 0.02F * breath;
        this.c[SINK] = 0.15F * breath;
        this.c[NOD] = 0.05F + 0.015F * breath;
        this.c[STAFF] = 0.08F;
        this.c[R_ARM] = -0.2F;
        this.c[R_OUT] = 0.05F + 0.01F * breath;
        this.c[R_ELBOW] = 1.25F + 0.03F * breath;
        this.c[L_ARM] = 0.05F;
        this.c[L_OUT] = 0.06F + 0.015F * breath;
        this.c[L_ELBOW] = 0.3F + 0.04F * breath;
        this.c[R_KNEE] = 0.05F;
        this.c[L_KNEE] = 0.05F;
        if (floating) {
            // Hanging in the air: legs loose, toes pointed down, a slow bob on two waves so it never repeats plainly.
            this.c[R_LEG] = 0.12F;
            this.c[L_LEG] = -0.06F;
            this.c[R_KNEE] = 0.3F;
            this.c[L_KNEE] = 0.22F;
            this.c[R_FOOT] = 0.55F;
            this.c[L_FOOT] = 0.45F;
            float bob = Mth.sin(age * 0.09F + caster.getId()) + 0.35F * Mth.sin(age * 0.157F + caster.getId() * 0.3F);
            this.c[LIFT] = still ? 0.0F : 0.6F * bob;
            this.c[R_LEG] += 0.04F * bob;
            this.c[L_LEG] -= 0.03F * bob;
            this.c[FLARE] = 0.15F;
        } else {
            // Standing, his weight on one foot: that knee straight, the other a little bent, his hips rolled over.
            this.c[ROLL] += 0.03F * shift;
            this.c[R_KNEE] += 0.1F * Math.max(0.0F, shift);
            this.c[L_KNEE] += 0.1F * Math.max(0.0F, -shift);
            this.c[TILT] -= 0.02F * shift;
        }
        if (!floating && MobPreview.drawing(caster)) {
            this.planted();
        }
        this.rise = Mth.clamp((float) (caster.getY() - caster.yo), -0.5F, 0.5F);
        this.walk(limbSwing, amount, floating);
        if (floating) {
            this.bank(caster);
        }
        if (move == 0 && amount < 0.1F && !caster.isAggressive()) {
            WizardRest.idle(this.c, age + caster.getId() * 37);
        } else if (WizardMove.pastime(move)) {
            WizardRest.pastime(this.c, move, t, age);
        }
        float[] keys = WizardKeys.at(move, t);
        if (keys != null) {
            for (int i = 0; i < CHANNELS; i++) {
                this.c[i] += keys[i];
            }
        }
        this.extras(move, t, age);
        this.closeExtras(move, t, age);
        if (caster.hurtTime > 0) {
            // Struck: the blow snaps his head and chest back first, his arms fly up after, his knees give a little.
            float f = caster.hurtTime / 10.0F;
            float snap = f * f;
            this.c[LEAN] -= 0.25F * snap;
            this.c[NOD] -= 0.35F * snap;
            this.c[TILT] += 0.08F * snap;
            this.c[SINK] += 0.5F * f;
            this.c[R_ARM] += 0.2F * f;
            this.c[L_ARM] += 0.35F * f;
            this.c[L_OUT] += 0.25F * f;
            this.c[L_ELBOW] += 0.3F * f;
            this.c[R_KNEE] += 0.15F * f;
            this.c[L_KNEE] += 0.15F * f;
        }
        SPRINGS.follow(caster.getId(), this.c, age, caster.level().getGameTime());
        this.c[GLOW] = Math.max(0.0F, this.c[GLOW]);
        this.c[HAND] = Math.max(0.0F, this.c[HAND]);
        this.c[R_ELBOW] = Math.max(0.0F, this.c[R_ELBOW]);
        this.c[L_ELBOW] = Math.max(0.0F, this.c[L_ELBOW]);
        this.c[R_KNEE] = Math.max(0.0F, this.c[R_KNEE]);
        this.c[L_KNEE] = Math.max(0.0F, this.c[L_KNEE]);
        this.trail(caster, age, amount, floating, still);
    }

    // How far the whirl has turned his whole body round at `t` (radians), easing in and out of the spin.
    static float whirl(int move, double t) {
        if (move != WizardMove.WHIRL) {
            return 0.0F;
        }
        double from = WizardMove.release(WizardMove.WHIRL) - 2.0;
        double to = WizardMove.WHIRL_STOP + 3.0;
        double f = Mth.clamp((t - from) / (to - from), 0.0, 1.0);
        double eased = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
        return (float) (-eased * WHIRL_TURNS * Mth.TWO_PI);
    }

    // Flying, he banks into his turns as a bird does, leans into his speed and lifts his chest as he slows.
    private void bank(MovingMonster caster) {
        float turn = Mth.wrapDegrees(caster.yBodyRot - caster.yBodyRotO);
        float yaw = caster.yBodyRot * Mth.DEG_TO_RAD;
        double dx = caster.getX() - caster.xo;
        double dz = caster.getZ() - caster.zo;
        float ahead = (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw));
        float across = (float) (dx * Mth.cos(yaw) + dz * Mth.sin(yaw));
        this.c[ROLL] += Mth.clamp(-turn * 0.025F + across * 0.6F, -0.35F, 0.35F);
        this.c[LEAN] += Mth.clamp(ahead * 0.9F, -0.25F, 0.35F);
        this.c[TWIST] += Mth.clamp(-turn * 0.012F, -0.15F, 0.15F);
        this.c[R_LEG] += Mth.clamp(ahead * 0.8F, 0.0F, 0.4F);
        this.c[L_LEG] += Mth.clamp(ahead * 0.6F, 0.0F, 0.35F);
    }

    // What no keyframe holds of his moves up close and his specials: the frost cone's hand sweeping after them, the
    // rune drawn for a sentry, the push down with each ring of the sigil and wave of the flare, the arms sweeping down
    // with each shaft of the rain, the fist shaking as the eclipse is dragged down, the shudder of each staff blow.
    private void closeExtras(int move, double t, float age) {
        switch (move) {
            case WizardMove.FROST_CONE -> {
                float pour = Trunk.in(t, 9.0, 11.0) * (1.0F - Trunk.in(t, 23.0, 26.0));
                this.c[L_IN] += 0.18F * pour * Mth.sin((float) t * 0.5F);
                this.c[L_ARM] += 0.06F * pour * Mth.sin((float) t * 0.9F);
                this.c[L_WRIST] += 0.08F * pour * Mth.sin(age * 2.1F);
                this.c[TILT] += 0.03F * pour * Mth.sin(age * 1.7F);
            }
            case WizardMove.SENTRY -> {
                float draw = Trunk.in(t, 5.0, 7.0) * (1.0F - Trunk.in(t, 16.0, 19.0));
                float round = (float) ((t - 6.0) / 11.0 * Mth.TWO_PI);
                this.c[L_IN] += 0.35F * draw * Mth.sin(round);
                this.c[L_ARM] += 0.3F * draw * Mth.cos(round);
            }
            case WizardMove.SIGIL, WizardMove.FLARE -> {
                int gap = move == WizardMove.SIGIL ? 13 : 12;
                float from = WizardMove.release(move);
                float tremble = Trunk.in(t, 8.0, from) * (1.0F - Trunk.in(t, from, from + 2.0));
                this.c[TILT] += 0.05F * tremble * Mth.sin(age * 5.3F);
                this.c[ROLL] += 0.03F * tremble * Mth.sin(age * 6.1F);
                this.c[R_ARM] += 0.05F * tremble * Mth.sin(age * 7.3F);
                this.c[L_ARM] += 0.05F * tremble * Mth.sin(age * 6.7F);
                for (int beat = 0; beat < 3; beat++) {
                    double at = from + beat * gap;
                    float push = Trunk.in(t, at - 0.5, at + 0.5) * (1.0F - Trunk.in(t, at + 1.0, at + 6.0));
                    this.c[SINK] += 1.2F * push;
                    this.c[LEAN] += (move == WizardMove.SIGIL ? 0.12F : -0.1F) * push;
                    this.c[NOD] += 0.15F * push;
                    this.c[GLOW] += 1.2F * push;
                    this.c[FLARE] += 0.6F * push;
                }
            }
            case WizardMove.RAIN -> {
                float conduct = Trunk.in(t, 18.0, 21.0) * (1.0F - Trunk.in(t, 58.0, 62.0));
                float beat = (float) ((t - 18.0) / 3.0 * Mth.PI);
                float right = Math.max(0.0F, Mth.sin(beat));
                float left = Math.max(0.0F, -Mth.sin(beat));
                this.c[R_ARM] += 0.9F * conduct * right;
                this.c[L_ARM] += 0.9F * conduct * left;
                this.c[TWIST] += 0.12F * conduct * (right - left);
                this.c[HAND] += 0.6F * conduct * (right + left);
            }
            case WizardMove.ECLIPSE -> {
                float strain = Trunk.in(t, 22.0, 25.0) * (1.0F - Trunk.in(t, 46.0, 49.0));
                this.c[L_WRIST] += 0.08F * strain * Mth.sin(age * 4.7F);
                this.c[L_ELBOW] += 0.06F * strain * Mth.sin(age * 5.9F);
                this.c[SINK] += 0.25F * strain * Mth.sin(age * 0.5F);
                this.c[TILT] += 0.04F * strain * Mth.sin(age * 0.37F);
            }
            case WizardMove.COMBO -> {
                for (int blow : new int[] { WizardMove.COMBO_SMASH, WizardMove.COMBO_SWEEP, WizardMove.COMBO_THRUST }) {
                    float jar = Trunk.in(t, blow - 0.5, blow + 0.5) * (1.0F - Trunk.in(t, blow + 0.5, blow + 4.0));
                    this.c[R_WRIST] -= 0.25F * jar;
                    this.c[STAFF] -= 0.18F * jar;
                    this.c[NOD] += 0.08F * jar;
                    this.c[SINK] += 0.3F * jar;
                }
            }
            case WizardMove.WHIRL -> {
                float spin = Trunk.in(t, 9.0, 11.0) * (1.0F - Trunk.in(t, 28.0, 31.0));
                this.c[LIFT] += 0.35F * spin * Mth.sin((float) t * 1.05F);
                this.c[ROLL] += 0.05F * spin * Mth.sin((float) t * 0.52F);
            }
            default -> {
            }
        }
    }

    // Standing for his picture (the Mobs page): the staff planted upright at his side, its foot on the ground, his
    // hand resting on it.
    private void planted() {
        // The arm straight, as the staff hangs from the arm and not the bent hand (StaffLayer), and out so far that
        // the staff's foot, a block under the grip, meets the ground.
        // Turned out about the upright rather than lifted out sideways, which would tip the staff.
        this.c[R_ARM] = -0.84F;
        this.c[R_OUT] = 0.0F;
        this.c[R_IN] = -0.35F;
        this.c[R_ELBOW] = 0.0F;
        this.c[STAFF] = -Mth.HALF_PI - this.c[R_ARM];
    }

    // A measured walk: short steps under the robe, a gentle bob, the staff swung forward and planted every other step;
    // floating, he leans into where he drifts and his legs and robe trail behind.
    private void walk(float limbSwing, float amount, boolean floating) {
        float w = Math.min(1.0F, amount * 1.8F);
        if (floating) {
            this.c[LEAN] += 0.25F * w;
            this.c[R_ARM] += this.rise * 2.0F;
            this.c[L_ARM] += this.rise * 2.4F;
            this.c[L_OUT] += Math.max(0.0F, -this.rise) * 2.0F;
            this.c[FLARE] += Math.abs(this.rise) * 1.5F;
            this.c[R_LEG] += 0.3F * w;
            this.c[L_LEG] += 0.22F * w;
            this.c[R_KNEE] += 0.2F * w;
            this.c[L_KNEE] += 0.3F * w;
            this.c[FLARE] += 0.4F * w;
            this.c[L_ARM] += 0.25F * w;
            this.c[L_OUT] += 0.2F * w;
            return;
        }
        float phase = limbSwing * 0.6F;
        float cos = Mth.cos(phase);
        float sin = Mth.sin(phase);
        this.c[R_LEG] += 0.5F * w * cos;
        this.c[L_LEG] -= 0.5F * w * cos;
        this.c[R_KNEE] += 0.45F * w * Math.max(0.0F, -sin);
        this.c[L_KNEE] += 0.45F * w * Math.max(0.0F, sin);
        // Heel to toe: the trailing foot pushes off on its toes, the leading one comes down heel first.
        this.c[R_FOOT] += w * (0.35F * Math.max(0.0F, cos) - 0.2F * Math.max(0.0F, -cos) * Math.max(0.0F, sin));
        this.c[L_FOOT] += w * (0.35F * Math.max(0.0F, -cos) - 0.2F * Math.max(0.0F, cos) * Math.max(0.0F, -sin));
        this.c[SINK] += 0.4F * w * Math.abs(sin);
        this.c[ROLL] += 0.03F * w * cos;
        this.c[NOD] -= 0.04F * w * Math.abs(sin);
        float plant = Mth.cos(phase * 0.5F);
        this.c[R_ARM] += -0.25F * w * plant;
        this.c[R_ELBOW] -= 0.1F * w * plant;
        this.c[R_WRIST] += 0.15F * w * plant;
        this.c[L_ARM] += 0.25F * w * cos;
        this.c[L_ELBOW] += 0.1F * w * Math.abs(cos);
        this.c[TWIST] += 0.06F * w * cos;
        this.c[LEAN] += 0.05F * w;
    }

    // What no keyframe holds.
    private void extras(int move, double t, float age) {
        switch (move) {
            case WizardMove.BOLT -> {
                for (int n = 0; n < 3; n++) {
                    double at = WizardMove.release(WizardMove.BOLT) + n * WizardMove.BOLT_AGAIN;
                    float kick = Trunk.in(t, at - 0.5, at + 0.5) * (1.0F - Trunk.in(t, at + 1.0, at + 4.0));
                    this.c[R_ARM] += 0.28F * kick;
                    this.c[STAFF] -= 0.28F * kick;
                    this.c[LEAN] -= 0.12F * kick;
                    this.c[GLOW] += 0.9F * kick;
                }
            }
            case WizardMove.BLIZZARD -> {
                float swing = Trunk.in(t, 3.0, 6.0) * (1.0F - Trunk.in(t, 16.0, 19.0));
                float round = (float) (t * 0.65);
                this.c[R_IN] += 0.55F * swing * Mth.sin(round);
                this.c[R_OUT] += 0.35F * swing * (0.5F + 0.5F * Mth.cos(round));
                this.c[TWIST] += 0.25F * swing * Mth.sin(round);
                this.c[SPIN] += (float) t * 0.4F * swing;
            }
            case WizardMove.STARFALL, WizardMove.LIGHTNING, WizardMove.METEOR -> {
                float end = WizardMove.release(move);
                float tremble = Trunk.in(t, 4.0, end) * (1.0F - Trunk.in(t, end + 4.0, end + 12.0));
                this.c[TILT] += 0.035F * tremble * Mth.sin(age * 5.3F);
                this.c[ROLL] += 0.018F * tremble * Mth.sin(age * 6.1F);
                this.c[R_ARM] += 0.04F * tremble * Mth.sin(age * 7.3F);
            }
            case WizardMove.BEAM -> {
                float shake = Trunk.in(t, 24.0, 28.0) * (1.0F - Trunk.in(t, 78.0, 82.0));
                this.c[TWIST] += 0.035F * shake * Mth.sin(age * 3.1F);
                this.c[ROLL] += 0.025F * shake * Mth.sin(age * 2.3F);
                this.c[R_ARM] += 0.05F * shake * Mth.sin(age * 4.7F);
                this.c[L_ARM] += 0.05F * shake * Mth.sin(age * 5.3F);
                this.c[SINK] += 0.25F * shake * Mth.sin(age * 3.7F);
            }
            case WizardMove.UNBIND, WizardMove.ASCEND, WizardMove.AWAKEN, WizardMove.APOTHEOSIS -> {
                float shake = Trunk.in(t, 8.0, 44.0) * (1.0F - Trunk.in(t, 46.0, 50.0))
                        * (move == WizardMove.UNBIND || move == WizardMove.AWAKEN ? 1.0F : 0.4F);
                this.c[LEAN] += 0.06F * shake * Mth.sin(age * 4.3F);
                this.c[ROLL] += 0.06F * shake * Mth.sin(age * 5.9F);
                this.c[TILT] += 0.08F * shake * Mth.sin(age * 3.7F);
                this.c[R_ARM] += 0.08F * shake * Mth.sin(age * 6.7F);
                this.c[L_ARM] += 0.08F * shake * Mth.sin(age * 7.1F);
            }
            case WizardMove.WARP -> {
                float draw = Trunk.in(t, 5.0, 7.0) * (1.0F - Trunk.in(t, 17.0, 20.0));
                float round = (float) ((t - 6.0) / 12.0 * Mth.TWO_PI);
                this.c[L_IN] += 0.5F * draw * Mth.sin(round);
                this.c[L_ARM] += 0.4F * draw * Mth.cos(round);
            }
            case WizardMove.TORNADO -> {
                float stir = Trunk.in(t, 5.0, 8.0) * (1.0F - Trunk.in(t, 20.0, 23.0));
                float round = (float) (t * 0.7);
                this.c[L_IN] += 0.6F * stir * Mth.sin(round);
                this.c[L_ARM] += 0.4F * stir * Mth.cos(round);
                this.c[TWIST] += 0.15F * stir * Mth.sin(round);
            }
            case WizardMove.TENDRILS, WizardMove.VOID_ORB -> {
                float end = WizardMove.release(move);
                float tremble = Trunk.in(t, 6.0, end) * (1.0F - Trunk.in(t, end, end + 3.0));
                this.c[TILT] += 0.04F * tremble * Mth.sin(age * 5.7F);
                this.c[ROLL] += 0.02F * tremble * Mth.sin(age * 6.3F);
                this.c[L_ARM] += 0.06F * tremble * Mth.sin(age * 7.9F);
                this.c[R_ARM] += 0.04F * tremble * Mth.sin(age * 8.3F);
            }
            case WizardMove.DRAIN -> {
                float draw = Trunk.in(t, 16.0, 20.0) * (1.0F - Trunk.in(t, 62.0, 66.0));
                this.c[L_WRIST] += 0.18F * draw * Mth.sin(age * 0.6F);
                this.c[L_ELBOW] += 0.12F * draw * (0.5F + 0.5F * Mth.sin(age * 0.6F));
                this.c[SINK] += 0.3F * draw * Mth.sin(age * 0.3F);
                this.c[TILT] += 0.05F * draw * Mth.sin(age * 0.17F);
            }
            case WizardMove.SPIRAL -> {
                float turn = Trunk.in(t, 12.0, 16.0) * (1.0F - Trunk.in(t, 54.0, 58.0));
                this.c[TWIST] += 0.55F * turn * Mth.sin(age * 0.45F);
                this.c[ROLL] += 0.06F * turn * Mth.sin(age * 0.3F);
                this.c[SPIN] += (float) t * 0.5F * turn;
                this.c[LIFT] += 0.5F * turn * Mth.sin(age * 0.2F);
            }
            case WizardMove.SUPERNOVA -> {
                float tremble = Trunk.in(t, 24.0, 100.0) * (1.0F - Trunk.in(t, 104.0, 110.0));
                this.c[TILT] += 0.05F * tremble * Mth.sin(age * 5.3F);
                this.c[ROLL] += 0.03F * tremble * Mth.sin(age * 6.1F);
                this.c[R_ARM] += 0.06F * tremble * Mth.sin(age * 7.3F);
                this.c[L_ARM] += 0.06F * tremble * Mth.sin(age * 6.7F);
                this.c[SINK] += 0.3F * tremble * Mth.sin(age * 4.1F);
            }
            case WizardMove.ARRIVE -> {
                // Turning over slowly in his cocoon as it sinks.
                float sink = 1.0F - Trunk.in(t, 44.0, 56.0);
                this.c[ROLL] += 0.12F * sink * Mth.sin(age * 0.07F);
                this.c[LEAN] += 0.1F * sink * Mth.sin(age * 0.05F);
                this.c[SINK] += 0.4F * sink * Mth.sin(age * 0.09F);
            }
            case WizardMove.STUNNED -> {
                this.c[TILT] += 0.16F * Mth.sin(age * 0.15F);
                this.c[NOD] += 0.08F * Mth.sin(age * 0.11F);
            }
            case WizardMove.GLIDE, WizardMove.DASH -> {
                this.c[ROLL] += 0.06F * Mth.sin(age * 0.2F);
                this.c[LIFT] += 0.6F * Mth.sin(age * 0.3F);
            }
            default -> {
            }
        }
    }

    // Beard, hat-tip, skirt and cape: cloth and hair on soft springs of their own, swung back by his speed, out to the
    // side as he turns, up as he drops and down as he rises, shaken by bursts of magic, and waving slowly at rest. The
    // beard's angle is in the world's tilt (WizardModel hangs it from his chin), the others in his trunk's.
    private void trail(MovingMonster caster, float age, float amount, boolean floating, boolean still) {
        float sway = still ? 0.0F : Mth.sin(age * 0.11F + caster.getId());
        float yaw = caster.yBodyRot * Mth.DEG_TO_RAD;
        double dx = caster.getX() - caster.xo;
        double dz = caster.getZ() - caster.zo;
        float ahead = (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw));
        float across = (float) (dx * Mth.cos(yaw) + dz * Mth.sin(yaw));
        float rise = (float) (caster.getY() - caster.yo);
        float turn = Mth.wrapDegrees(caster.yBodyRot - caster.yBodyRotO) * Mth.DEG_TO_RAD;
        float burst = Math.max(0.0F, this.c[FLARE]);
        float rush = Mth.clamp(ahead * 4.0F, -0.8F, 1.6F);
        float[] cloth = this.cloth;
        cloth[0] = 0.04F * sway + 0.35F * rush - 0.8F * rise;
        cloth[1] = -0.5F * this.c[ROLL] - 1.2F * turn - 0.4F * across * 4.0F + 0.04F * Mth.sin(age * 0.09F);
        cloth[2] = 0.05F * sway - 0.3F * this.c[LEAN] + 0.3F * rush;
        cloth[3] = -0.6F * this.c[ROLL] - 1.5F * turn + 0.05F * Mth.cos(age * 0.08F);
        cloth[4] = 0.03F * sway + 0.3F * rush - 0.6F * rise * 4.0F + (floating ? 0.12F : 0.0F) + 0.15F * burst;
        cloth[5] = -0.4F * this.c[ROLL] - 0.8F * turn - 0.3F * across * 4.0F + 0.03F * Mth.sin(age * 0.13F);
        cloth[6] = 0.04F * sway + 0.55F * rush - 0.9F * rise * 4.0F + 0.25F * burst + (floating ? 0.1F : 0.0F);
        cloth[7] = -1.0F * turn - 0.5F * across * 4.0F + 0.04F * Mth.sin(age * 0.07F + 1.0F);
        CLOTH.follow(caster.getId(), cloth, age, caster.level().getGameTime());
        this.beard = cloth[0];
        this.beardSide = cloth[1];
        this.hat = cloth[2];
        this.hatSide = cloth[3];
        this.skirt = cloth[4];
        this.skirtSide = cloth[5];
        this.cape = Math.max(0.0F, cloth[6]);
        this.capeSide = cloth[7];
    }
}
