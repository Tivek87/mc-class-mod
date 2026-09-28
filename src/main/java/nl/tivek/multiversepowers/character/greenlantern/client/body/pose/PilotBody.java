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
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The mech's pilot posed whole, hips, trunk, knees and elbows (Stance): braced with the ring arm out while its feet
// come down, a crouch and a leap into its chest with one knee drawn up and the ring fist high, a catch in the light
// there, their own arms working the mech's (a flare, a spread, a clap, a lift), then down onto the seat, feet on the
// rest, and forward to take the sticks. Model space, in pixels: y runs down, -z is ahead, +x the pilot's own left;
// hands in the chest's own axes from the neck, feet from the ground under the hips.
final class PilotBody {
    // Hips, trunk (lean ahead, waist, twist, roll), right and left foot, right and left hand.
    private static final int DROP = 0;
    private static final int BACK = 1;
    private static final int PITCH = 2;
    private static final int WAIST = 3;
    private static final int TWIST = 4;
    private static final int ROLL = 5;
    private static final int FEET = 6;
    private static final int HANDS = 12;
    private static final int VALUES = 18;

    private static final float[] STAND = pose(0.8F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F,
            -2.4F, 24.0F, -0.8F, 2.4F, 24.0F, 0.8F, -5.8F, 11.5F, 0.5F, 5.8F, 11.5F, 0.5F);
    // The ring fist thrust at where the feet come down, the other hand bracing its wrist.
    private static final float[] CAST = pose(1.8F, 0.0F, 0.1F, 0.0F, -0.25F, 0.0F,
            -3.0F, 24.0F, -2.6F, 3.2F, 24.0F, 2.4F, -1.5F, 2.8F, -8.8F, -1.0F, 4.0F, -6.2F);
    private static final float[] WIND_UP = pose(5.0F, 1.0F, 0.55F, 0.2F, 0.0F, 0.0F,
            -2.6F, 24.0F, -1.0F, 2.6F, 24.0F, 0.6F, -6.5F, 9.5F, 5.5F, 6.5F, 9.5F, 5.5F);
    private static final float[] LAUNCH = pose(-0.5F, 0.0F, -0.1F, -0.1F, 0.0F, 0.0F,
            -2.0F, 24.5F, 1.5F, 2.0F, 24.5F, 2.5F, -4.2F, -8.0F, -1.5F, 4.0F, -4.5F, -6.5F);
    // In the air one knee drawn up, the other leg trailing, the ring fist high and the free arm out.
    private static final float[] HERO = pose(0.5F, 0.0F, 0.12F, 0.05F, 0.0F, 0.0F,
            -2.3F, 17.5F, -4.5F, 2.2F, 23.0F, 3.8F, -4.5F, -7.5F, -2.5F, 13.0F, 2.5F, -2.0F);
    private static final float[] REACH_DOWN = pose(1.5F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F,
            -2.4F, 22.5F, -1.8F, 2.4F, 22.5F, -0.8F, -12.0F, 5.0F, -3.0F, 12.0F, 5.0F, -3.0F);
    // Caught by the light in the chest: a crouch in the air.
    private static final float[] CATCH = pose(3.5F, 0.0F, 0.35F, 0.0F, 0.0F, 0.0F,
            -2.5F, 22.0F, -3.0F, 2.5F, 22.0F, -2.0F, -4.5F, 9.0F, -6.5F, 4.5F, 9.0F, -6.5F);
    private static final float[] FLARE = pose(0.3F, 0.0F, -0.22F, -0.1F, 0.0F, 0.0F,
            -2.8F, 24.0F, 1.0F, 2.8F, 24.0F, 1.0F, -13.0F, -2.0F, -1.5F, 13.0F, -2.0F, -1.5F);
    private static final float[] REACH_UP = pose(0.8F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F,
            -2.3F, 23.5F, 0.3F, 2.3F, 23.2F, -0.3F, -3.5F, -3.0F, -7.5F, 3.5F, -3.0F, -7.5F);
    private static final float[] PUSH_DOWN = pose(1.2F, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F,
            -2.3F, 23.2F, -0.3F, 2.3F, 23.5F, 0.3F, -3.0F, 6.0F, -7.0F, 3.0F, 6.0F, -7.0F);
    private static final float[] SPREAD = pose(0.6F, 0.0F, -0.05F, 0.0F, 0.0F, 0.0F,
            -2.6F, 23.6F, 0.2F, 2.6F, 23.6F, 0.2F, -14.0F, 2.5F, -2.5F, 14.0F, 2.5F, -2.5F);
    private static final float[] CLAP_BACK = pose(0.6F, 0.0F, -0.08F, 0.0F, 0.0F, 0.0F,
            -2.6F, 23.6F, 0.4F, 2.6F, 23.6F, 0.4F, -12.5F, 2.0F, -6.0F, 12.5F, 2.0F, -6.0F);
    private static final float[] CLAP = pose(1.8F, 0.0F, 0.28F, 0.1F, 0.0F, 0.0F,
            -2.3F, 22.5F, -2.2F, 2.3F, 22.3F, -1.8F, -0.7F, 3.2F, -8.2F, 0.7F, 3.2F, -8.2F);
    private static final float[] RELEASE = pose(0.8F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            -2.4F, 23.5F, 0.0F, 2.4F, 23.5F, 0.0F, -9.0F, 3.0F, -6.5F, 9.0F, 3.0F, -6.5F);
    private static final float[] LIFT = pose(0.5F, 0.0F, -0.12F, 0.0F, 0.0F, 0.0F,
            -2.4F, 23.8F, 0.4F, 2.4F, 23.8F, 0.4F, -8.5F, -6.5F, -3.0F, 8.5F, -6.5F, -3.0F);
    // On the seat, feet on the rest before it.
    private static final float[] SEATED = pose(0.4F, 1.2F, -0.02F, 0.0F, 0.0F, 0.0F,
            -2.4F, 18.4F, -10.0F, 2.4F, 18.4F, -10.0F, -14.0F, 2.5F, -1.5F, 14.0F, 2.5F, -1.5F);
    private static final float[] SEATED_AHEAD = pose(0.4F, 1.2F, 0.12F, 0.0F, 0.0F, 0.0F,
            -2.4F, 18.4F, -10.0F, 2.4F, 18.4F, -10.0F, -5.5F, 3.0F, -8.0F, 5.5F, 3.0F, -8.0F);
    // Sinking back to the ground as the mech breaks up round them.
    private static final float[] LOWERED = pose(0.8F, 0.0F, 0.05F, 0.0F, 0.0F, 0.0F,
            -2.5F, 24.0F, -0.4F, 2.5F, 24.0F, 0.6F, -8.0F, 9.5F, -1.5F, 8.0F, 9.5F, -1.5F);

    private static final Keyframes.Key[] MOVES = {
            key(0, true, STAND), key(5, true, CAST), key(20, true, CAST), key(26, true, WIND_UP),
            key(29, true, WIND_UP), key(32, false, LAUNCH), key(37, true, HERO), key(43, true, HERO),
            key(47, false, REACH_DOWN), key(51, true, CATCH), key(58, true, FLARE), key(64, true, FLARE),
            key(68, false, REACH_UP), key(72, false, PUSH_DOWN), key(76, true, SPREAD), key(80, true, SPREAD),
            key(84, false, CLAP_BACK), key(86, true, CLAP), key(96, true, CLAP), key(100, false, RELEASE),
            key(107, true, LIFT), key(114, true, SEATED), key(MechScript.GRIP - 2, true, SEATED),
            key(MechScript.GRIP + 4, true, SEATED_AHEAD) };
    // The ring fist thrown up with the mech's own as it locks, and the lean of each lever pushed.
    private static final Vector3f FIST_UP = new Vector3f(-4.5F, -8.0F, -2.0F);
    private static final float LEVER_LEAN = 0.12F;
    // Each foot of the mech landing jolts through the pilot: knees give, the trunk rocks back.
    private static final float JOLT_DROP = 1.4F;
    private static final float JOLT_BACK = -0.1F;
    private static final double JOLT_TICKS = 7.0;
    // Hanging in the light, the pilot bobs gently.
    private static final float BOB = 0.4F;

    private static final Vector3f KNEE = new Vector3f(0.0F, -0.5F, -1.0F);
    private static final Vector3f RIGHT_ELBOW = new Vector3f(-0.7F, 0.3F, 0.8F);
    private static final Vector3f LEFT_ELBOW = new Vector3f(0.7F, 0.3F, 0.8F);
    private static final Vector3f HIPS = new Vector3f();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Vector3f NECK = new Vector3f();
    private static final Vector3f FOOT = new Vector3f();
    private static final Vector3f HAND = new Vector3f();

    private PilotBody() {
    }

    private static float[] pose(float... values) {
        if (values.length != VALUES) {
            throw new IllegalArgumentException("A pilot pose has " + VALUES + " values");
        }
        return values;
    }

    private static Keyframes.Key key(int t, boolean stop, float[] pose) {
        return new Keyframes.Key(t, stop, pose);
    }

    // The pose `t` ticks into the build (to SETTLED), or `broke` ticks into breaking up (below 0: not breaking).
    // `grips`: where each hand holds on (right, left) in model space, or null; `onStick` how far they have it,
    // `raised` how far the ring fist is up, `push` how far the levers lean ahead.
    static void pose(PlayerModel<?> model, double t, double broke, @Nullable Vector3f[] grips, double onStick,
            double raised, double push) {
        float[] v = Keyframes.at(MOVES, (float) Math.min(t, MechScript.SETTLED));
        if (broke >= 0.0) {
            float u = (float) Ease.smooth(broke / 6.0);
            for (int i = 0; i < VALUES; i++) {
                v[i] = Mth.lerp(u, v[i], LOWERED[i]);
            }
            onStick *= 1.0 - u;
            raised *= 1.0 - u;
        }
        float jolt = (float) (jolt(t, MechScript.STOMP) + jolt(t, MechScript.STOMP2));
        float bob = (float) (BOB * Math.sin(t * 0.24) * Ease.smooth((t - 56.0) / 8.0)
                * (1.0 - Ease.smooth((t - 104.0) / 8.0)));
        float drop = v[DROP] + JOLT_DROP * jolt + bob;
        HIPS.set(0.0F, Stance.HIP_Y + drop, v[BACK]);
        float pitch = v[PITCH] + JOLT_BACK * jolt + (float) (LEVER_LEAN * push * onStick);
        LEAN.rotationZYX(v[ROLL] * 0.6F, v[TWIST] * 0.4F, pitch * 0.6F);
        TURN.rotationZYX(v[ROLL] * 0.4F, v[TWIST] * 0.6F, pitch * 0.4F + v[WAIST]);
        Stance.trunk(model, HIPS, LEAN, TURN, false);
        for (int side = 0; side < 2; side++) {
            int o = FEET + side * 3;
            // The hips bob, the feet hang from them: while hanging in the light nothing stands on the ground.
            FOOT.set(v[o], v[o + 1] + bob, v[o + 2]);
            Stance.leg(model, side == 0, FOOT, KNEE);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int o = HANDS + side * 3;
            HAND.set(v[o], v[o + 1], v[o + 2]);
            if (right && raised > 0.0) {
                HAND.lerp(FIST_UP, (float) raised);
            }
            CHEST.transform(HAND).add(NECK);
            if (grips != null && grips[side] != null) {
                float hold = (float) (right ? onStick * (1.0 - raised) : onStick);
                HAND.lerp(grips[side], hold);
            }
            Stance.arm(model, right, HAND, right ? RIGHT_ELBOW : LEFT_ELBOW);
        }
    }

    // How hard a foot landing `at` still jolts, rising fast and dying away.
    private static double jolt(double t, double at) {
        double since = t - at;
        if (since < 0.0 || since > JOLT_TICKS) {
            return 0.0;
        }
        return Math.sin(Math.PI * Math.sqrt(since / JOLT_TICKS));
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
