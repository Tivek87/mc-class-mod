package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;

// How the Wizard holds himself this frame (WizardModel, StaffLayer), on his channels (WizardKeys): upright and a
// little stooped, breathing slow, his staff upright before him in his right hand; walking he plants the staff with
// every other step, his robe swinging; floating (from his second phase) he drifts with his legs hanging and his robe
// trailing. At rest now and then he strokes his beard or taps his staff; struck, he flinches. On top his moves'
// keyframes, and what no keyframe holds: the staff swung round in circles, the trembling as great magic passes through
// him, the kick of each bolt, the circle his hand draws for the time warp, the sway of a stunned head. All of it
// followed on springs, so he never snaps from one pose into the next; beard, hat-tip and robe trail after.
final class WizardPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.3, 0.8);

    final float[] c = new float[CHANNELS];
    float beard;
    float beardSide;
    float hat;
    float hatSide;
    float skirt;
    float skirtSide;

    void of(MovingMonster caster, float limbSwing, float amount, float age, boolean floating) {
        float breath = Mth.sin(age * 0.07F);
        java.util.Arrays.fill(this.c, 0.0F);
        this.c[LEAN] = 0.06F;
        this.c[SINK] = 0.15F * breath;
        this.c[NOD] = 0.05F;
        this.c[STAFF] = 0.08F;
        this.c[R_ARM] = -0.2F;
        this.c[R_OUT] = 0.05F;
        this.c[R_ELBOW] = 1.25F + 0.03F * breath;
        this.c[L_ARM] = 0.05F;
        this.c[L_OUT] = 0.06F;
        this.c[L_ELBOW] = 0.3F;
        this.c[R_KNEE] = 0.05F;
        this.c[L_KNEE] = 0.05F;
        if (floating) {
            // Hanging in the air: legs loose, toes down, a slow bob.
            this.c[R_LEG] = 0.12F;
            this.c[L_LEG] = -0.06F;
            this.c[R_KNEE] = 0.3F;
            this.c[L_KNEE] = 0.22F;
            this.c[LIFT] = 0.8F * Mth.sin(age * 0.09F + caster.getId());
            this.c[FLARE] = 0.15F;
        }
        int move = caster.move();
        double t = caster.moveAge(age - caster.tickCount);
        this.walk(limbSwing, amount, floating);
        if (move == 0) {
            this.idle(age + caster.getId() * 37, amount);
        }
        float[] keys = WizardKeys.at(move, t);
        if (keys != null) {
            for (int i = 0; i < CHANNELS; i++) {
                this.c[i] += keys[i];
            }
        }
        this.extras(move, t, age);
        if (caster.hurtTime > 0) {
            float f = caster.hurtTime / 10.0F;
            this.c[LEAN] -= 0.25F * f;
            this.c[NOD] -= 0.3F * f;
            this.c[R_ARM] += 0.2F * f;
            this.c[L_ARM] += 0.35F * f;
            this.c[L_OUT] += 0.2F * f;
        }
        SPRINGS.follow(caster.getId(), this.c, age, caster.level().getGameTime());
        this.c[GLOW] = Math.max(0.0F, this.c[GLOW]);
        this.c[HAND] = Math.max(0.0F, this.c[HAND]);
        this.c[R_ELBOW] = Math.max(0.0F, this.c[R_ELBOW]);
        this.c[L_ELBOW] = Math.max(0.0F, this.c[L_ELBOW]);
        this.c[R_KNEE] = Math.max(0.0F, this.c[R_KNEE]);
        this.c[L_KNEE] = Math.max(0.0F, this.c[L_KNEE]);
        this.trail(caster, age, amount, floating);
    }

    // A measured walk: short steps under the robe, a gentle bob, the staff swung forward and planted every other step;
    // floating, he leans into where he drifts and his legs and robe trail behind.
    private void walk(float limbSwing, float amount, boolean floating) {
        float w = Math.min(1.0F, amount * 1.8F);
        if (floating) {
            this.c[LEAN] += 0.25F * w;
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
        this.c[SINK] += 0.4F * w * Math.abs(sin);
        this.c[ROLL] += 0.03F * w * cos;
        float plant = Mth.cos(phase * 0.5F);
        this.c[R_ARM] += -0.25F * w * plant;
        this.c[R_ELBOW] -= 0.1F * w * plant;
        this.c[R_WRIST] += 0.15F * w * plant;
        this.c[L_ARM] += 0.25F * w * cos;
        this.c[L_ELBOW] += 0.1F * w * Math.abs(cos);
        this.c[TWIST] += 0.06F * w * cos;
        this.c[LEAN] += 0.05F * w;
    }

    // Now and then at rest: a slow stroke of his beard, a tap of his staff on the ground.
    private void idle(float age, float amount) {
        if (amount > 0.1F) {
            return;
        }
        float cycle = age % 300.0F;
        float stroke = bump(cycle, 10.0F, 70.0F);
        if (stroke > 0.0F) {
            float pull = 0.5F + 0.5F * Mth.sin(age * 0.25F);
            this.c[L_ARM] += -1.3F * stroke;
            this.c[L_IN] += 0.55F * stroke;
            this.c[L_ELBOW] += 1.75F * stroke;
            this.c[L_WRIST] += (0.3F + 0.3F * pull) * stroke;
            this.c[NOD] += 0.12F * stroke;
            this.c[SINK] += -0.2F * pull * stroke;
        }
        float tap = bump(cycle, 170.0F, 190.0F);
        if (tap > 0.0F) {
            float knock = Mth.sin((cycle - 170.0F) / 20.0F * Mth.PI * 2.0F);
            this.c[R_ARM] += -0.12F * tap + 0.08F * knock * tap;
            this.c[STAFF] += 0.06F * knock * tap;
            this.c[NOD] += -0.08F * tap;
        }
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
            case WizardMove.UNBIND, WizardMove.ASCEND -> {
                float shake = Trunk.in(t, 8.0, 44.0) * (1.0F - Trunk.in(t, 46.0, 50.0))
                        * (move == WizardMove.UNBIND ? 1.0F : 0.4F);
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

    // Beard, hat-tip and robe trail after his turns, leans and steps, and wave in their own slow way.
    private void trail(MovingMonster caster, float age, float amount, boolean floating) {
        float sway = Mth.sin(age * 0.11F + caster.getId());
        this.beard = 0.06F * sway - 0.6F * this.c[LEAN] - 0.25F * this.c[NOD];
        this.beardSide = -0.5F * this.c[ROLL] - 0.3F * this.c[TWIST] + 0.04F * Mth.sin(age * 0.09F);
        this.hat = 0.05F * sway - 0.3F * this.c[LEAN];
        this.hatSide = -0.6F * this.c[ROLL] + 0.05F * Mth.cos(age * 0.08F);
        float knees = Math.max(this.c[R_KNEE], this.c[L_KNEE]);
        float stride = Math.max(-Math.min(this.c[R_LEG], this.c[L_LEG]), 0.0F);
        this.skirt = -0.85F * this.c[LEAN] - 0.35F * knees - 0.4F * stride
                + (floating ? 0.25F * Math.min(1.0F, amount * 1.8F) : 0.0F) + 0.03F * sway;
        this.skirtSide = -0.7F * this.c[ROLL] + 0.03F * Mth.sin(age * 0.13F);
    }

    // 0 outside `from`..`to`, rising and falling smoothly between.
    private static float bump(float x, float from, float to) {
        if (x <= from || x >= to) {
            return 0.0F;
        }
        float f = (x - from) / (to - from);
        float s = Mth.sin(f * Mth.PI);
        return s * s;
    }
}
