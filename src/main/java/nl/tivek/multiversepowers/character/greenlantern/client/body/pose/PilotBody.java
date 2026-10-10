package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The mech's pilot posed whole (Stance), key by key (PilotKeys): hips, trunk, knees and elbows, each hand turned at its
// wrist and each foot at its ankle, the head looking where the moment asks. On top of the keys the ring fist is aimed
// at each foot of the mech as it forms high above and falls, every landing, clap, lock and crash jolts through the
// body, the hands tremble while they strain, and the pilot breathes and bobs while hanging in the light.
// Model space, in pixels: y runs down, -z is ahead, +x the pilot's own left.
final class PilotBody {
    private static final int DROP = PilotKeys.DROP;
    private static final int BACK = PilotKeys.BACK;
    private static final int PITCH = PilotKeys.PITCH;
    private static final int WAIST = PilotKeys.WAIST;
    private static final int TWIST = PilotKeys.TWIST;
    private static final int ROLL = PilotKeys.ROLL;
    private static final int FEET = PilotKeys.FEET;
    private static final int HANDS = PilotKeys.HANDS;
    private static final int PALMS = PilotKeys.PALMS;
    private static final int TOES = PilotKeys.TOES;
    private static final int LOOK = PilotKeys.LOOK;
    private static final int VALUES = PilotKeys.VALUES;

    // The ring fist thrown up with the mech's own as it locks, knuckles to the sky, and the lean of each lever pushed.
    private static final Vector3f FIST_UP = new Vector3f(-4.5F, -8.0F, -2.0F);
    private static final Quaternionf FIST_UP_TURN = new Quaternionf().rotationX((float) Math.PI);
    private static final float LEVER_LEAN = 0.12F;
    // Each foot landing, the clap, the arms locking on, the head's crash, the claw closing on it, its throw and its lock
    // jolt through the pilot: when, how far the knees give and how far the trunk rocks (back -).
    private static final float[][] JOLTS = { { MechScript.STOMP, 1.4F, -0.1F }, { MechScript.STOMP2, 1.1F, -0.08F },
            { MechScript.CLAP, 0.8F, -0.06F }, { MechScript.ELBOWS, 0.5F, 0.0F }, { MechScript.CRASH, 1.8F, -0.14F },
            { MechScript.GRAB, 1.0F, 0.1F }, { MechScript.TOSS, 0.8F, -0.12F }, { MechScript.LOCK, 1.0F, -0.08F } };
    private static final double JOLT_TICKS = 7.0;
    // Hanging in the light, the pilot bobs gently; all through the build they breathe.
    private static final float BOB = 0.4F;
    private static final float BREATH_DROP = 0.15F;
    private static final float BREATH_PITCH = 0.015F;
    private static final double BREATH_TICKS = 40.0;
    // Holding a foot up, driving it down and squeezing the clap, the hands tremble.
    private static final float TREMBLE = 0.22F;
    // The aimed ring fist this far from its shoulder (an arm reaches 10), the other hand cupping its wrist from below.
    private static final float AIM_REACH = 9.6F;
    private static final float BRACE_BACK = 2.8F;
    private static final float BRACE_BELOW = 1.3F;
    // Aimed above the shoulders the ring arm stays out on its own side of the head, and the other hand lets go of its
    // wrist rather than reach up across the face.
    private static final float OUTSIDE = -0.45F;
    private static float brace;
    // The eyes above the neck, and how far an aim leads the head.
    private static final float EYES = 4.0F;
    private static final float AIM_LOOK = 0.9F;
    // Sinking back to the ground as the mech breaks up round them.
    private static final float[] LOWERED = PilotKeys.lowered();

    private static final Vector3f KNEE = new Vector3f(0.0F, -0.5F, -1.0F);
    private static final Vector3f RIGHT_ELBOW = new Vector3f(-0.7F, 0.3F, 0.8F);
    private static final Vector3f LEFT_ELBOW = new Vector3f(0.7F, 0.3F, 0.8F);
    private static final Vector3f HIPS = new Vector3f();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Quaternionf PALM = new Quaternionf();
    private static final Quaternionf AIMED = new Quaternionf();
    private static final Quaternionf BRACED = new Quaternionf();
    private static final Quaternionf SOLE = new Quaternionf();
    private static final Vector3f NECK = new Vector3f();
    private static final Vector3f FOOT = new Vector3f();
    private static final Vector3f HAND = new Vector3f();
    private static final Vector3f WAY = new Vector3f();
    private static final Vector3f AIM_HAND = new Vector3f();
    private static final Vector3f AIM_DOWN = new Vector3f();
    private static final Vector3f FINGERS = new Vector3f();
    private static final Vector3f FACING = new Vector3f();
    private static final Vector3f ACROSS = new Vector3f();
    private static final Vector3f THIRD = new Vector3f();
    private static final Matrix3f TURNED = new Matrix3f();

    private PilotBody() {
    }

    // The pose `t` ticks into the build (to SETTLED), or `broke` ticks into breaking up (below 0: not breaking).
    // `grips`: where each hand holds on (right, left) in model space, or null; `onStick` how far each has it (right,
    // left), `raised` how far the ring fist is up, `push` how far the levers lean ahead; `aim`: the point the ring fist
    // is thrown at (model space), or null.
    static void pose(PlayerModel<?> model, double t, double broke, @Nullable Vector3f[] grips, double[] onStick,
            double raised, double push, @Nullable Vector3f aim) {
        float[] v = Keyframes.at(PilotKeys.MOVES, (float) Math.min(t, MechScript.SETTLED));
        double holdRight = onStick[0];
        double holdLeft = onStick[1];
        double aiming = aim == null ? 0.0 : aiming(t);
        if (broke >= 0.0) {
            float u = (float) Ease.smooth(broke / 6.0);
            for (int i = 0; i < VALUES; i++) {
                v[i] = Mth.lerp(u, v[i], LOWERED[i]);
            }
            holdRight *= 1.0 - u;
            holdLeft *= 1.0 - u;
            raised *= 1.0 - u;
            aiming = 0.0;
        }
        float joltDrop = 0.0F;
        float joltBack = 0.0F;
        for (float[] jolt : JOLTS) {
            float k = (float) jolt(t, jolt[0]);
            joltDrop += jolt[1] * k;
            joltBack += jolt[2] * k;
        }
        float bob = (float) (BOB * Math.sin(t * 0.24) * Ease.smooth((t - MechScript.HIPS - 2.0) / 8.0)
                * (1.0 - Ease.smooth((t - MechScript.RISE - 1.0) / 8.0)));
        float breath = broke >= 0.0 ? 0.0F : (float) (Math.sin(2.0 * Math.PI * t / BREATH_TICKS)
                * (1.0 - Ease.smooth((t - MechScript.DONE) / (MechScript.SETTLED - MechScript.DONE))));
        float drop = v[DROP] + joltDrop + bob + BREATH_DROP * breath;
        HIPS.set(0.0F, Stance.HIP_Y + drop, v[BACK]);
        float pitch = v[PITCH] + joltBack + BREATH_PITCH * breath + (float) (LEVER_LEAN * push * holdRight);
        LEAN.rotationZYX(v[ROLL] * 0.6F, v[TWIST] * 0.4F, pitch * 0.6F);
        TURN.rotationZYX(v[ROLL] * 0.4F, v[TWIST] * 0.6F, pitch * 0.4F + v[WAIST]);
        Stance.trunk(model, HIPS, LEAN, TURN, false);
        for (int side = 0; side < 2; side++) {
            int o = FEET + side * 3;
            // The hips bob, the feet hang from them: while hanging in the light nothing stands on the ground.
            FOOT.set(v[o], v[o + 1] + bob, v[o + 2]);
            Stance.leg(model, side == 0, FOOT, KNEE, SOLE.rotationX(v[TOES + side]), 1.0F);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        if (aiming > 0.0 && !aimed(model, aim)) {
            aiming = 0.0;
        }
        float shake = TREMBLE * (float) strain(t);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int o = HANDS + side * 3;
            HAND.set(v[o], v[o + 1], v[o + 2]);
            palm(v, side, PALM);
            if (right && raised > 0.0) {
                HAND.lerp(FIST_UP, (float) raised);
                PALM.slerp(FIST_UP_TURN, (float) raised);
            }
            CHEST.transform(HAND).add(NECK);
            PALM.premul(CHEST);
            if (aiming > 0.0) {
                if (right) {
                    HAND.lerp(AIM_HAND, (float) aiming);
                    PALM.slerp(AIMED, (float) aiming);
                } else {
                    // Cupping the ring arm's wrist from below.
                    FOOT.set(WAY).mul(-BRACE_BACK).add(AIM_HAND).add(AIM_DOWN.x * BRACE_BELOW,
                            AIM_DOWN.y * BRACE_BELOW, AIM_DOWN.z * BRACE_BELOW);
                    HAND.lerp(FOOT, (float) aiming * brace);
                    PALM.slerp(BRACED, (float) aiming * brace);
                }
            }
            double held = right ? holdRight * (1.0 - raised) : holdLeft;
            if (grips != null && grips[side] != null && held > 0.0) {
                HAND.lerp(grips[side], (float) held);
            }
            HAND.add(shake * (float) Math.sin(t * 2.9 + side * 1.3), shake * (float) Math.cos(t * 3.7 + side * 0.7),
                    0.0F);
            Stance.arm(model, right, HAND, right ? RIGHT_ELBOW : LEFT_ELBOW, PALM, 1.0F);
        }
        look(model, v, aim, aiming);
    }

    // How far the ring fist is aimed at the feet as they form and fall: from the wind-up until both are down.
    private static double aiming(double t) {
        return Ease.smooth((t - MechScript.FOOT_FORM + 1.5) / 2.5)
                * (1.0 - Ease.smooth((t - MechScript.STOMP2 - 0.5) / 2.5));
    }

    // How hard the hands strain: gathering the light, holding a foot up and driving it down, squeezing the clap and
    // hauling the head out and round.
    private static double strain(double t) {
        return 0.6 * window(t, 4.0, MechScript.FOOT_FORM - 1.0)
                + window(t, MechScript.FOOT_FORM + 0.5, MechScript.STOMP)
                + window(t, MechScript.FOOT2_FORM + 0.5, MechScript.STOMP2)
                + window(t, MechScript.CLAP + 1.0, MechScript.RELEASE)
                + 0.7 * window(t, MechScript.GRAB - 1.0, MechScript.TOSS);
    }

    private static double window(double t, double from, double to) {
        return Ease.smooth((t - from) / 1.5) * (1.0 - Ease.smooth((t - to) / 1.5));
    }

    // The ring fist out from its shoulder along the way to `aim` (AIM_HAND, WAY), knuckles first and palm down (AIMED,
    // AIM_DOWN: down square to the way); false when there is no way to aim.
    private static boolean aimed(PlayerModel<?> model, Vector3f aim) {
        WAY.set(aim).sub(model.rightArm.x, model.rightArm.y, model.rightArm.z);
        if (WAY.lengthSquared() < 1.0E-4F) {
            return false;
        }
        WAY.normalize();
        float up = (float) Ease.smooth((-WAY.y - 0.2) / 0.5);
        float most = Mth.lerp(up, 1.0F, OUTSIDE);
        if (WAY.x > most) {
            WAY.x = most;
            WAY.normalize();
        }
        brace = 1.0F - (float) Ease.smooth((-WAY.y - 0.1) / 0.4);
        AIM_HAND.set(WAY).mul(AIM_REACH).add(model.rightArm.x, model.rightArm.y, model.rightArm.z);
        AIM_DOWN.set(0.0F, 1.0F, 0.0F).sub(WAY.x * WAY.y, WAY.y * WAY.y, WAY.z * WAY.y);
        if (AIM_DOWN.lengthSquared() < 1.0E-4F) {
            AIM_DOWN.set(0.0F, 0.0F, 1.0F);
        }
        AIM_DOWN.normalize();
        turn(WAY, AIM_DOWN, true, AIMED);
        // The bracing palm turned up under the ring wrist.
        turn(WAY, FACING.set(AIM_DOWN).negate(), false, BRACED);
        return true;
    }

    // A key's turn of one hand at its wrist, in the chest's axes.
    private static void palm(float[] v, int side, Quaternionf out) {
        int o = PALMS + side * 6;
        FINGERS.set(v[o], v[o + 1], v[o + 2]);
        FACING.set(v[o + 3], v[o + 4], v[o + 5]);
        if (FINGERS.lengthSquared() < 1.0E-6F) {
            out.identity();
            return;
        }
        FINGERS.normalize();
        FACING.sub(FINGERS.x * FACING.dot(FINGERS), FINGERS.y * FACING.dot(FINGERS), FINGERS.z * FACING.dot(FINGERS));
        if (FACING.lengthSquared() < 1.0E-6F) {
            out.identity();
            return;
        }
        turn(FINGERS, FACING.normalize(), side == 0, out);
    }

    // The turn of a hand whose fingers run `fingers` and whose palm faces `facing` (both of length 1 and square): an
    // arm's own y runs along its fingers, and its palm is the inner side, +x on the right hand and -x on the left.
    private static void turn(Vector3f fingers, Vector3f facing, boolean right, Quaternionf out) {
        ACROSS.set(facing);
        if (!right) {
            ACROSS.negate();
        }
        TURNED.set(ACROSS, fingers, THIRD.set(ACROSS).cross(fingers));
        out.setFromNormalized(TURNED);
    }

    // The head follows the key's look as far as it leads the player's own, and the aim while the ring fist is thrown.
    private static void look(PlayerModel<?> model, float[] v, @Nullable Vector3f aim, double aiming) {
        float yaw = v[LOOK];
        float pitch = v[LOOK + 1];
        float lead = v[LOOK + 2];
        if (aiming > 0.0 && aim != null) {
            WAY.set(aim).sub(NECK.x, NECK.y - EYES, NECK.z);
            float flat = (float) Math.sqrt(WAY.x * WAY.x + WAY.z * WAY.z);
            float a = (float) aiming;
            yaw = Mth.lerp(a, yaw, (float) Math.atan2(-WAY.x, -WAY.z));
            pitch = Mth.lerp(a, pitch, (float) Math.atan2(WAY.y, flat));
            lead = Mth.lerp(a, lead, AIM_LOOK);
        }
        if (lead <= 0.0F) {
            return;
        }
        model.head.yRot = Mth.lerp(lead, model.head.yRot, yaw);
        model.head.xRot = Mth.lerp(lead, model.head.xRot, pitch);
    }

    // How hard a jolt set off `at` still shakes, rising fast and dying away.
    private static double jolt(double t, double at) {
        return Ease.jolt((t - at) / JOLT_TICKS);
    }

    // Where a point of the world is in the drawn model of `entity`, in pixels as above (see LivingEntityRenderer: the
    // body turned by 180 - its yaw, flipped on x and y, 1.501 blocks down).
    static Vector3f toModel(LivingEntity entity, float partialTick, Vec3 world, Vector3f out) {
        double dx = world.x - Mth.lerp(partialTick, entity.xOld, entity.getX());
        double dy = world.y - Mth.lerp(partialTick, entity.yOld, entity.getY());
        double dz = world.z - Mth.lerp(partialTick, entity.zOld, entity.getZ());
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot) - 180.0F);
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double x = dx * c + dz * s;
        double z = -dx * s + dz * c;
        return out.set((float) (-16.0 * x), (float) (-16.0 * dy + 1.501 * 16.0), (float) (16.0 * z));
    }
}
