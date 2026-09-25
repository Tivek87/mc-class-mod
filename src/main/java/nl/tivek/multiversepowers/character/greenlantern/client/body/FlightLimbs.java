package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlightPose.Blend;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlightPose.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.FlightPose.KNEEL_LEAN;

/**
 * Where each limb goes in the flight pose (see {@link FlightPose}): the arms, legs and head in flight and during the
 * take-off, the ring arm with the beam, and the body swinging upright for a slam and landing on one knee.
 */
final class FlightLimbs {
    // The ring arm with the beam (see BeamArm), in radians: how far it trembles at its hardest, how far the beam
    // breaking loose kicks it up, and how far it swings in towards the middle of his chest once the other hand braces
    // it. That hand then comes across (turned in towards the ring arm) and a little lower, under its wrist.
    private static final float BEAM_TREMBLE = 0.05F;
    private static final float BEAM_KICK = 0.42F;
    private static final float BRACE_IN = 0.2F;
    private static final float BRACE_ACROSS = 0.72F;
    private static final float BRACE_DROP = 0.14F;
    // Where his limbs point in the world while he kneels, in radians from straight down, positive swinging back: the
    // right thigh straight down to the knee on the ground, the left thigh out in front almost flat, the ring arm
    // straight down into the ground, the other arm flung back and up.
    private static final float DOWN_THIGH = 0.05F;
    private static final float STEP_THIGH = -1.4F;
    private static final float FIST_ARM = -0.1F;
    private static final float BACK_ARM = 2.15F;

    private FlightLimbs() {
    }

    /**
     * The beam (see {@link BeamArm}): the ring arm points where he looks, trembling and kicking with the light; once
     * the other hand braces it, the ring arm swings in a little towards the middle of his chest and the other arm
     * comes across under it, its hand at the wrist, trembling and kicking along.
     */
    static void beam(HumanoidModel<?> model, ModelPart limb, boolean right, Blend blend, LivingEntity entity,
            float partialTick, float time) {
        float tremble = BEAM_TREMBLE * BeamArm.tremble(entity, partialTick);
        float lift = model.head.xRot - BEAM_KICK * BeamArm.kick(entity, partialTick)
                + tremble * BeamArm.shake(time, 0);
        float turn = model.head.yRot + tremble * BeamArm.shake(time, 1);
        if (right) {
            limb.xRot = Mth.lerp(blend.beam, limb.xRot, -Mth.HALF_PI + lift);
            limb.yRot = Mth.lerp(blend.beam, limb.yRot, -0.08F - BRACE_IN * blend.brace + turn);
            limb.zRot = Mth.lerp(blend.beam, limb.zRot, 0.0F);
        } else if (blend.brace > 0.0F) {
            limb.xRot = Mth.lerp(blend.brace, limb.xRot, -Mth.HALF_PI + BRACE_DROP + lift);
            limb.yRot = Mth.lerp(blend.brace, limb.yRot, BRACE_ACROSS + turn);
            limb.zRot = Mth.lerp(blend.brace, limb.zRot, 0.0F);
        }
    }

    /**
     * Swinging upright for a slam into the ground just ahead: the ring fist cocked high over his head, the other arm
     * out for balance, one knee drawn up and the other leg back, looking down at where he will hit.
     */
    static void brace(HumanoidModel<?> model, ModelPart limb, boolean right, float weight) {
        if (right) {
            // Raised, an arm swings out the other way round than hanging down, hence the minus.
            limb.xRot = Mth.lerp(weight, limb.xRot, -2.9F);
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(weight, limb.zRot, -0.12F);
            // Once for both legs and the head (this runs for each arm).
            model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, 0.55F);
            model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, -0.95F);
            model.head.xRot = Mth.clamp(model.head.xRot + 0.4F * weight, -1.35F, 1.1F);
        } else {
            limb.xRot = Mth.lerp(weight, limb.xRot, -0.3F);
            limb.yRot = Mth.lerp(weight, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(weight, limb.zRot, -1.25F);
        }
    }

    /**
     * The landing slam, the way heroes land: his body drops and leans forward over his hips (see {@link FlightPose#pre}),
     * his right knee comes down on the ground with the shin lying back along it, his left foot is planted out in front,
     * the ring fist comes down out of its cocked position straight into the ground before him, and the other arm is
     * flung back and up. He stays down there while the construct strikes; then he rises again. The legs bend at the
     * knee, which the game's own legs cannot: their thighs are posed here and their shins hung on them in
     * {@link KneelLegs}.
     *
     * <p>Every angle is set where the limb has to point in the world, less the lean of his body, since the limbs lean
     * along with it.
     *
     * @param age   ticks since he hit the ground
     * @param kneel 0 to 1: how far down he is
     */
    static void slam(HumanoidModel<?> model, ModelPart limb, boolean right, float age, float kneel) {
        float lean = KNEEL_LEAN * kneel;
        if (right) {
            // The fist: from cocked high over his head, straight down into the ground.
            float strike = (float) Ease.smooth(age / 1.4);
            float shudder = age < 5.0F ? 0.05F * (1.0F - age / 5.0F) * Mth.sin(age * 8.0F) : 0.0F;
            limb.xRot = Mth.lerp(kneel, limb.xRot, Mth.lerp(strike, -2.9F, FIST_ARM - lean) + shudder);
            limb.yRot = Mth.lerp(kneel, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(kneel, limb.zRot, Mth.lerp(strike, -0.12F, 0.1F));
            // Once for both legs and the head (this runs for each arm).
            model.rightLeg.xRot = Mth.lerp(kneel, model.rightLeg.xRot, DOWN_THIGH - lean);
            model.rightLeg.yRot = Mth.lerp(kneel, model.rightLeg.yRot, 0.0F);
            model.rightLeg.zRot = Mth.lerp(kneel, model.rightLeg.zRot, 0.04F);
            model.leftLeg.xRot = Mth.lerp(kneel, model.leftLeg.xRot, STEP_THIGH - lean);
            model.leftLeg.yRot = Mth.lerp(kneel, model.leftLeg.yRot, -0.12F);
            model.leftLeg.zRot = Mth.lerp(kneel, model.leftLeg.zRot, -0.1F);
            // Looking ahead, a little down, over the fist in the ground.
            model.head.xRot = Mth.lerp(kneel, model.head.xRot, Mth.clamp(0.25F - lean, -1.35F, 1.1F));
            model.head.yRot = Mth.lerp(kneel, model.head.yRot, 0.0F);
        } else {
            // Flung back and up as the fist comes down, and out to the side for balance.
            float fling = (float) Ease.smooth(age / 2.0);
            limb.xRot = Mth.lerp(kneel, limb.xRot, Mth.lerp(fling, 0.4F, BACK_ARM) - lean);
            limb.yRot = Mth.lerp(kneel, limb.yRot, 0.0F);
            limb.zRot = Mth.lerp(kneel, limb.zRot, -0.45F);
        }
    }

    /**
     * Where one arm goes in flight, as xRot, yRot and zRot: the take-off (fists to the chest, then swept down
     * along the sides), hovering (a little out, drifting) or flying fast (back along the sides), or up in the air
     * while an empty ring lets him sink.
     *
     * @param side 1 for the right arm, -1 for the left
     */
    static float[] flightArm(Frame f, float side) {
        float time = f.time();
        if (f.sinking()) {
            // Arms up and wide, like someone hanging from a chute, with a slow sway. Raised, an arm swings out
            // the other way round than hanging down, hence the minus.
            float sway = 0.12F * Mth.sin(time * 0.2F + side);
            return new float[] { -2.95F + sway, 0.0F, -side * (0.6F + sway) };
        }
        float hoverX = -0.12F + 0.07F * Mth.sin(time * 0.08F + side);
        float hoverZ = side * (0.3F + 0.05F * Mth.sin(time * 0.06F));
        float x = Mth.lerp(f.fast(), hoverX, 0.1F);
        float z = Mth.lerp(f.fast(), hoverZ, side * 0.09F);
        float y = 0.0F;
        float t = f.t();
        if (t >= 0.0F && t < ClientFlight.ARISE + 6.0F) {
            // Fists to the chest ...
            float gather = (float) Ease.smooth(t / ClientFlight.GATHER);
            // ... then down along the sides, a little back and out ...
            float sweep = (float) Ease.smooth((t - ClientFlight.GATHER) / (ClientFlight.SWEEP - ClientFlight.GATHER));
            // ... and from there into the flight.
            float fly = (float) Ease.smooth(
                    (t - ClientFlight.SWEEP) / (ClientFlight.ARISE + 6.0F - ClientFlight.SWEEP));
            float ax = Mth.lerp(sweep, Mth.lerp(gather, hoverX, -1.28F), 0.42F);
            float ay = Mth.lerp(sweep, gather * -side * 0.78F, 0.0F);
            float az = Mth.lerp(sweep, gather * side * 0.05F, side * 0.32F);
            x = Mth.lerp(fly, ax, x);
            y = Mth.lerp(fly, ay, y);
            z = Mth.lerp(fly, az, z);
        }
        return new float[] { x, y, z };
    }

    /** The legs: hanging loose and drifting while he hovers, straight together and fluttering when fast. */
    static void legs(HumanoidModel<?> model, Frame f, float weight) {
        float time = f.time();
        float rightX;
        float leftX;
        float spread;
        if (f.sinking()) {
            rightX = 0.2F * Mth.sin(time * 0.2F);
            leftX = -rightX;
            spread = 0.12F;
        } else {
            float flutter = 0.05F * Mth.sin(time * 0.7F) * f.fast();
            rightX = Mth.lerp(f.fast(), -0.22F + 0.09F * Mth.sin(time * 0.07F), 0.1F + flutter);
            leftX = Mth.lerp(f.fast(), 0.14F + 0.09F * Mth.sin(time * 0.07F + 2.0F), 0.1F - flutter);
            spread = Mth.lerp(f.fast(), 0.08F, 0.025F);
            float t = f.t();
            if (t >= 0.0F && t < ClientFlight.ARISE + 6.0F) {
                // Together and pointed down as he shoots up.
                float rise = (float) (Ease.smooth((t - ClientFlight.GATHER) / 4.0)
                        * (1.0 - Ease.smooth((t - ClientFlight.ARISE) / 6.0)));
                rightX = Mth.lerp(rise, rightX, 0.12F);
                leftX = Mth.lerp(rise, leftX, 0.12F);
                spread = Mth.lerp(rise, spread, 0.02F);
            }
        }
        model.rightLeg.xRot = Mth.lerp(weight, model.rightLeg.xRot, rightX);
        model.leftLeg.xRot = Mth.lerp(weight, model.leftLeg.xRot, leftX);
        model.rightLeg.yRot = Mth.lerp(weight, model.rightLeg.yRot, 0.0F);
        model.leftLeg.yRot = Mth.lerp(weight, model.leftLeg.yRot, 0.0F);
        model.rightLeg.zRot = Mth.lerp(weight, model.rightLeg.zRot, spread);
        model.leftLeg.zRot = Mth.lerp(weight, model.leftLeg.zRot, -spread);
    }

    /** The head during the take-off: down while the fists come to the chest, then up as he rises. */
    static float headLift(Frame f) {
        float t = f.t();
        if (t < 0.0F || t > ClientFlight.ARISE + 6.0F) {
            return 0.0F;
        }
        float gather = (float) (Ease.smooth(t / ClientFlight.GATHER)
                * (1.0 - Ease.smooth((t - ClientFlight.GATHER) / 3.0)));
        float up = (float) (Ease.smooth((t - ClientFlight.GATHER) / 4.0)
                * (1.0 - Ease.smooth((t - ClientFlight.ARISE) / 6.0)));
        return 0.3F * gather - 0.45F * up;
    }
}
