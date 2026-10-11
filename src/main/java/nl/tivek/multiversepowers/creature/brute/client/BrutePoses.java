package nl.tivek.multiversepowers.creature.brute.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.brute.BruteMoves;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;
import nl.tivek.multiversepowers.engine.math.Ease;

// How the brute stands this frame, as turns on top of its model's rest (BruteModel): a heavy rolling walk, breathing
// and a slack jaw at rest, its arms grasping for what it hunts, and each of its moves on the server's timeline.
// Arms and legs turn forward with a negative angle; a positive lean bends the trunk further forward.
final class BrutePoses {
    float lean;
    float twist;
    float roll;
    float sink;
    float nod;
    float shake;
    float jaw;
    float legSpread;
    final float[] arm = new float[2];
    final float[] spread = new float[2];
    final float[] armTwist = new float[2];
    final float[] leg = new float[2];

    void of(ZombieBrute brute, float limbSwing, float amount, float age) {
        this.lean = 0.0F;
        this.twist = 0.0F;
        this.roll = 0.0F;
        this.sink = 0.0F;
        this.nod = 0.0F;
        this.shake = 0.0F;
        this.jaw = 0.0F;
        this.legSpread = 0.0F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = 0.0F;
            this.spread[s] = 0.0F;
            this.armTwist[s] = 0.0F;
            this.leg[s] = 0.0F;
        }
        this.walk(limbSwing, amount);
        this.idle(age, brute.getId());
        int move = brute.move();
        if (move == 0 && brute.isAggressive()) {
            this.grasp(age);
        }
        double t = brute.moveAge(age - brute.tickCount);
        switch (move) {
            case BruteMoves.SMASH -> this.smash(t);
            case BruteMoves.LEAP -> this.leap(t);
            case BruteMoves.SLAM -> this.slam(t);
            case BruteMoves.ROAR -> this.roar(t, age);
            case BruteMoves.CHARGE -> this.charge(t, age);
            case BruteMoves.STUNNED -> this.stunned(t, age);
            default -> {
            }
        }
        BruteSmooth.follow(brute.getId(), this, age, brute.level().getGameTime());
    }

    private static float in(double t, double from, double to) {
        return (float) Ease.smooth((t - from) / (to - from));
    }

    // Heavy steps: each leg swings far, the arms against them, its weight rolling over the leg it stands on and its
    // head dipping as each foot comes down.
    private void walk(float limbSwing, float amount) {
        float phase = limbSwing * 0.55F;
        float w = Math.min(1.0F, amount * 1.3F);
        float c = Mth.cos(phase);
        float s = Mth.sin(phase);
        this.leg[0] = 0.75F * w * c;
        this.leg[1] = -this.leg[0];
        this.arm[0] = -0.5F * w * c;
        this.arm[1] = -this.arm[0];
        this.roll = 0.07F * w * s;
        this.twist = 0.13F * w * s;
        this.lean += 0.08F * w;
        this.nod += 0.06F * w * Math.abs(c);
    }

    // At rest it breathes deep, its arms dangle and sway and its jaw hangs and twitches. No two are in step.
    private void idle(float age, int id) {
        float breath = Mth.sin(age * 0.08F + id);
        this.lean += 0.025F * breath;
        this.nod += 0.03F * breath;
        this.jaw = 0.12F + 0.06F * Mth.sin(age * 0.05F + id * 1.7F);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.03F * (1.0F + breath);
            this.arm[s] += 0.04F * Mth.sin(age * 0.06F + id + s * 2.0F);
        }
    }

    // Hunting, its hands reach out ahead of it, grasping by turns, its mouth open.
    private void grasp(float age) {
        this.lean += 0.06F;
        this.jaw += 0.15F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] -= 0.45F + 0.1F * Mth.sin(age * 0.3F + s * Mth.PI);
        }
    }

    // Both fists rise high overhead as it rears back, then come down together in front of it as it bends deep into
    // the blow, a foot forward.
    private void smash(double t) {
        float up = in(t, 0.0, 7.0) * (1.0F - in(t, BruteMoves.HITS - 2.5, BruteMoves.HITS - 0.5));
        float down = in(t, BruteMoves.HITS - 2.5, BruteMoves.HITS - 0.2)
                * (1.0F - in(t, BruteMoves.SMASH_END - 8.0, BruteMoves.SMASH_END));
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(up, this.arm[s], -2.85F);
            this.arm[s] = Mth.lerp(down, this.arm[s], -0.35F);
            this.spread[s] = Mth.lerp(up, this.spread[s], -0.22F);
            this.spread[s] = Mth.lerp(down, this.spread[s], 0.05F);
        }
        this.lean += -0.3F * up + 0.75F * down;
        this.nod += -0.35F * up + 0.25F * down;
        this.jaw = Math.max(this.jaw, 0.55F * up + 0.3F * down);
        this.legSpread += 0.12F * down;
        this.leg[0] += -0.25F * down;
        this.leg[1] += 0.2F * down;
    }

    // It crouches with its arms swung back, then springs: in the air its fists are raised for the slam, its legs
    // striding and its mouth wide.
    private void leap(double t) {
        float crouch = in(t, 0.0, 7.0) * (1.0F - in(t, BruteMoves.SPRING - 1.0, BruteMoves.SPRING + 1.0));
        float air = in(t, BruteMoves.SPRING - 1.0, BruteMoves.SPRING + 3.0);
        this.lean += 0.4F * crouch - 0.15F * air;
        this.jaw = Math.max(this.jaw, 0.2F * crouch + 0.8F * air);
        this.nod -= 0.3F * air;
        for (int s = 0; s < 2; s++) {
            this.leg[s] -= 0.55F * crouch;
            this.arm[s] = Mth.lerp(crouch, this.arm[s], 0.65F);
            this.arm[s] = Mth.lerp(air, this.arm[s], -2.7F);
            this.spread[s] = Mth.lerp(air, this.spread[s], -0.1F);
        }
        this.leg[0] -= 0.6F * air;
        this.leg[1] += 0.35F * air;
    }

    // Landing, both fists pound the ground in front of it, its legs braced wide, and it rises out of the blow.
    private void slam(double t) {
        float raised = 1.0F - in(t, 0.0, 1.5);
        float hit = in(t, -1.0, 1.5) * (1.0F - in(t, 5.0, BruteMoves.SLAM_END));
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(raised, this.arm[s], -2.7F);
            this.arm[s] = Mth.lerp(hit, this.arm[s], -0.3F);
            this.spread[s] = Mth.lerp(hit, this.spread[s], 0.3F);
        }
        this.lean += 0.8F * hit;
        this.nod += 0.3F * hit;
        this.jaw = Math.max(this.jaw, 0.6F * hit);
        this.legSpread += 0.2F * hit;
        this.sink += 1.2F * hit;
    }

    // It rears up, throws its head back and its arms wide and roars, shaking.
    private void roar(double t, float age) {
        float r = in(t, 0.0, 4.0) * (1.0F - in(t, BruteMoves.ROAR_END - 3.0, BruteMoves.ROAR_END));
        this.lean -= 0.4F * r;
        this.nod -= 0.75F * r;
        this.jaw = Math.max(this.jaw, r);
        this.shake += 0.05F * r * Mth.sin(age * 3.0F);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += r;
            this.arm[s] = Mth.lerp(r, this.arm[s], -0.35F) + 0.06F * r * Mth.sin(age * 2.2F + s * Mth.PI);
            this.armTwist[s] += 0.4F * r;
        }
    }

    // Head down and forward it runs, its arms pumping back at its sides.
    private void charge(double t, float age) {
        float c = in(t, 0.0, 3.0) * (1.0F - in(t, BruteMoves.RUN - 3.0, BruteMoves.RUN));
        this.lean += 0.5F * c;
        this.nod -= 0.35F * c;
        this.jaw = Math.max(this.jaw, 0.35F * c);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(c, this.arm[s], 0.55F + 0.35F * Mth.sin(age * 0.9F + s * Mth.PI));
            this.spread[s] += 0.15F * c;
        }
    }

    // Run into a wall it stands dazed: rocking, its head lolling, its arms hanging loose.
    private void stunned(double t, float age) {
        float st = in(t, 0.0, 3.0) * (1.0F - in(t, BruteMoves.STUN - 8.0, BruteMoves.STUN));
        this.lean -= 0.25F * st;
        this.roll += 0.12F * st * Mth.sin(age * 0.35F);
        this.shake += 0.45F * st * Mth.sin(age * 0.5F);
        this.nod += 0.35F * st;
        this.jaw = Math.max(this.jaw, 0.5F * st);
        this.legSpread += 0.1F * st;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(st, this.arm[s], 0.15F + 0.1F * Mth.sin(age * 0.4F + s));
            this.spread[s] += 0.2F * st;
        }
    }
}
