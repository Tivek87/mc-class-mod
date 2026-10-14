package nl.tivek.multiversepowers.creature.animal.client;

import net.minecraft.util.Mth;

// How a foot goes through a stride, as a real one does: set down reached forward, it stays planted while the body
// goes over it, so the leg sweeps back at an even pace for `duty` of the stride (more of it walking, less running);
// then it lifts, folding most early in its swing, and is carried forward faster to be set down again. A stride is one
// turn of the phase (2π), starting as the foot is set down. A leg's swing is positive back, as a model part's xRot.
public final class Gait {
    private Gait() {
    }

    // Where in its stride the foot is, 0 to 1.
    public static float of(float phase) {
        float u = phase / Mth.TWO_PI;
        return u - Mth.floor(u);
    }

    // The leg's swing: -1 reached forward as it is set down, +1 pushed back as it lifts.
    public static float swing(float phase, float duty) {
        float u = of(phase);
        if (u < duty) {
            float s = u / duty;
            // Even through the middle of the stance, easing only at its very ends.
            return -1.0F + 2.0F * (0.9F * s + 0.1F * s * s * (3.0F - 2.0F * s));
        }
        float s = (u - duty) / (1.0F - duty);
        return 1.0F - 2.0F * s * s * (3.0F - 2.0F * s);
    }

    // How high the foot is: 0 on the ground, 1 at the top of its swing, which comes early, as the leg folds first.
    public static float lift(float phase, float duty) {
        float u = of(phase);
        if (u < duty) {
            return 0.0F;
        }
        float s = (u - duty) / (1.0F - duty);
        return Mth.sin(Mth.PI * (float) Math.sqrt(s));
    }

    // The fold of the middle joint (knee, hock, wrist), a little ahead of the lift: it folds as the foot leaves the
    // ground and opens out again before it is set down.
    public static float fold(float phase, float duty) {
        float u = of(phase);
        if (u < duty) {
            // Giving a little as the weight comes onto it just after it is set down.
            float s = u / duty;
            return 0.12F * Mth.sin(Mth.PI * Math.min(1.0F, s * 3.0F));
        }
        float s = (u - duty) / (1.0F - duty);
        return Mth.sin(Mth.PI * (float) Math.pow(s, 0.7));
    }

    // The foot's own fold at the end of the leg, as a share of how far it folds: its heel rolling up off the ground as
    // the stance ends, folded most early in the swing (toes hanging), then reached out a little past flat to be set down.
    public static float toe(float phase, float duty) {
        float u = of(phase);
        if (u < duty) {
            return 0.5F * smooth(0.7F, 1.0F, u / duty);
        }
        float s = (u - duty) / (1.0F - duty);
        return 0.5F + 0.5F * Mth.sin(Mth.PI * smooth(0.0F, 0.6F, s)) - 0.5F * smooth(0.3F, 0.9F, s)
                - 0.15F * Mth.sin(Mth.PI * smooth(0.6F, 1.0F, s));
    }

    // 1 while the foot bears weight, 0 while it is in the air, eased at the edges: what the body rests on.
    public static float load(float phase, float duty) {
        float u = of(phase);
        float edge = 0.06F;
        if (u < duty) {
            return smooth(0.0F, edge, u) * (1.0F - smooth(duty - edge, duty, u));
        }
        return 0.0F;
    }

    // From the walk's share of stance at a slow walk to the run's at full speed.
    public static float duty(float run, float walkDuty, float runDuty) {
        return Mth.lerp(run, walkDuty, runDuty);
    }

    public static float smooth(float from, float to, float v) {
        float x = Mth.clamp((v - from) / (to - from), 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }
}
