package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;

// Your own Thor on his feet: the dash, the super jump and the leap and drop of a grab, and the state his flight in
// `ThorMotion` shares with them.
abstract class ThorGroundMotion {
    // A dash: one tick sinking into it, then eight covering the ground (fast out, easing in), then the stop.
    static final int DASH_WAIT = 1;
    static final int DASH_MOVE = 8;
    static final int DASH_ALL = 15;
    // A super jump from the ground sinks three ticks first; from the air (the first press already jumped) one.
    private static final int CROUCH = 3;
    private static final int TUCK = 1;
    private static final int GRAVITY_BACK = 12;
    private static final double FLOAT_SINK = -0.025;
    private static final double DRIFT = 0.24;
    private static final double GRAB_DASH = 9.5;
    private static final double HOIST = 9.0;
    private static final int HOIST_HANG = 8;
    private static final double DROP_SPEED = -2.4;
    private static final double DROP_STEER = 1.5;
    private static final RandomSource RANDOM = RandomSource.create();

    static int dashAge = -1;
    private static Vec3 dashWay = Vec3.ZERO;
    private static double dashFar;
    static int jumpAge = -1;
    private static int jumpWait;
    private static double launch;
    static int floatAge = -1;
    private static int floatTicks;
    static boolean dropping;
    private static int dropOnto = -1;
    static boolean flying;
    static boolean lightning;

    public static boolean flying() {
        return flying;
    }

    public static boolean lightning() {
        return flying && lightning;
    }

    // Floating at the top of a super jump.
    public static boolean floating() {
        return floatAge >= 0 && floatAge < floatTicks;
    }

    static int flags() {
        int flags = flying ? ThorStatePayload.FLYING : 0;
        flags |= floating() ? ThorStatePayload.FLOATING : 0;
        flags |= lightning() ? ThorStatePayload.LIGHTNING : 0;
        return flags;
    }

    static void still(Input input) {
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
    }

    // The way he was moving, or ahead when standing: packed with how far into the bits the server reads.
    static int dash(LocalPlayer player, CharacterAbility ability) {
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        double yaw = Math.toRadians(player.getYRot());
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 way = ahead.scale(forward).add(left.scale(strafe));
        dashWay = way.lengthSqr() < 1.0E-4 ? ahead : way.normalize();
        double shortest = ability.value("shortestBlocks");
        double longest = Math.max(shortest, ability.value("longestBlocks"));
        dashFar = shortest + (longest - shortest) * RANDOM.nextDouble();
        dashAge = 0;
        int angle = Math.floorMod(Math.round((float) Math.toDegrees(Math.atan2(-dashWay.x, dashWay.z)) / 360.0F
                * 256.0F), 256);
        int tenths = Mth.clamp((int) Math.round(dashFar * 10.0), 0, 255);
        ClientThor.predict(player, ThorStatePayload.DASH, angle | tenths << 8, flags());
        return angle | tenths << 8;
    }

    // A running grab: straight ahead the way he looks, as far as it reaches, packed as a dash is.
    static int grabDash(LocalPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        dashWay = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        dashFar = GRAB_DASH;
        dashAge = 0;
        int angle = Math.floorMod(Math.round(player.getYRot() / 360.0F * 256.0F), 256);
        int tenths = (int) Math.round(GRAB_DASH * 10.0);
        ClientThor.predict(player, ThorStatePayload.DASH, angle | tenths << 8, flags());
        return angle | tenths << 8;
    }

    // His grab caught what he dashed at: he stops there.
    static void grabbed() {
        if (dashAge >= 0 && dashAge < DASH_WAIT + DASH_MOVE) {
            dashAge = DASH_WAIT + DASH_MOVE;
        }
    }

    static void jump(LocalPlayer player, CharacterAbility ability) {
        jumpAge = 0;
        jumpWait = player.onGround() ? CROUCH : TUCK;
        launch = launchFor(ability.value("heightBlocks"));
        floatTicks = (int) Math.round(ability.value("floatSeconds") * 20.0);
        floatAge = -1;
        dashAge = -1;
        dropping = false;
        ClientThor.predict(player, ThorStatePayload.JUMP, 0, flags());
    }

    // Up with what he grabbed over his head: a high leap with a short hang at the top.
    static void hoist(LocalPlayer player) {
        jumpAge = 0;
        jumpWait = TUCK;
        launch = launchFor(HOIST);
        floatTicks = HOIST_HANG;
        floatAge = -1;
        dashAge = -1;
        dropping = false;
    }

    // Back down, hard: straight, or onto the creature `onto` (an entity id, -1 for none) wherever it lies.
    static void drop(int onto) {
        if (jumpAge >= 0) {
            dropping = true;
            dropOnto = onto;
        }
    }

    // The speed up that rises `height` blocks as the game's own gravity and air drag slow it.
    private static double launchFor(double height) {
        double low = 0.0;
        double high = 6.0;
        for (int i = 0; i < 40; i++) {
            double v = (low + high) * 0.5;
            double y = 0.0;
            double speed = v;
            while (speed > 0.0) {
                y += speed;
                speed = (speed - 0.08) * 0.98;
            }
            if (y < height) {
                low = v;
            } else {
                high = v;
            }
        }
        return (low + high) * 0.5;
    }

    // How far along a dash is after `u` of its moving part: quick off the mark, easing into the stop.
    static double dashEase(double u) {
        double k = Mth.clamp(u, 0.0, 1.0);
        return 1.0 - (1.0 - k) * (1.0 - k);
    }

    static void dashing(LocalPlayer player, Input input) {
        still(input);
        int t = dashAge++;
        Vec3 v = player.getDeltaMovement();
        if (t >= DASH_WAIT && t < DASH_WAIT + DASH_MOVE) {
            double step = dashFar * (dashEase((t + 1.0 - DASH_WAIT) / DASH_MOVE)
                    - dashEase((double) (t - DASH_WAIT) / DASH_MOVE));
            player.setDeltaMovement(dashWay.x * step, v.y, dashWay.z * step);
            if (player.horizontalCollision && t > DASH_WAIT) {
                dashAge = DASH_WAIT + DASH_MOVE;
            }
        } else if (t >= DASH_WAIT + DASH_MOVE) {
            player.setDeltaMovement(v.x * 0.35, v.y, v.z * 0.35);
        } else {
            player.setDeltaMovement(v.x * 0.3, v.y, v.z * 0.3);
        }
        if (dashAge >= DASH_ALL) {
            dashAge = -1;
        }
    }

    static void jumping(LocalPlayer player, Input input) {
        int t = jumpAge++;
        Vec3 v = player.getDeltaMovement();
        if (t < jumpWait) {
            still(input);
            player.setDeltaMovement(v.x * 0.4, jumpWait == TUCK ? Math.max(v.y, 0.0) : v.y, v.z * 0.4);
            return;
        }
        if (t == jumpWait) {
            still(input);
            player.setDeltaMovement(v.x, launch, v.z);
            player.resetFallDistance();
            return;
        }
        if (dropping) {
            still(input);
            Entity onto = dropOnto < 0 ? null : player.level().getEntity(dropOnto);
            double x = v.x * 0.5;
            double z = v.z * 0.5;
            if (onto != null && onto.isAlive()) {
                Vec3 to = onto.position().subtract(player.position());
                double ticks = Math.max(1.0, to.y / DROP_SPEED);
                x = Mth.clamp(to.x / ticks, -DROP_STEER, DROP_STEER);
                z = Mth.clamp(to.z / ticks, -DROP_STEER, DROP_STEER);
            }
            player.setDeltaMovement(x, DROP_SPEED, z);
            player.resetFallDistance();
            if (player.onGround()) {
                dropping = false;
                jumpAge = -1;
                floatAge = -1;
                ClientThor.predict(player, ThorStatePayload.TOUCH_DOWN, 0, flags());
            }
            return;
        }
        if (floatAge < 0 && v.y <= 0.02) {
            floatAge = 0;
            ClientThor.flags(player, flags());
        }
        if (floatAge >= 0) {
            int f = floatAge++;
            float forward = input.forwardImpulse;
            float strafe = input.leftImpulse;
            still(input);
            double yaw = Math.toRadians(player.getYRot());
            Vec3 wish = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).scale(forward)
                    .add(new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(strafe));
            if (wish.lengthSqr() > 1.0) {
                wish = wish.normalize();
            }
            wish = wish.scale(DRIFT);
            double vx = Mth.lerp(0.12, v.x, wish.x);
            double vz = Mth.lerp(0.12, v.z, wish.z);
            double vy;
            if (f < floatTicks) {
                vy = Mth.lerp(0.25, v.y, FLOAT_SINK);
            } else {
                double back = Math.min(1.0, (f - floatTicks + 1.0) / GRAVITY_BACK);
                vy = v.y - 0.08 * back * back;
            }
            player.setDeltaMovement(vx, vy, vz);
            player.resetFallDistance();
            if (f == floatTicks) {
                ClientThor.flags(player, flags());
            }
        }
        if (t > jumpWait + 2 && player.onGround()) {
            jumpAge = -1;
            floatAge = -1;
            ClientThor.predict(player, ThorStatePayload.TOUCH_DOWN, 0, flags());
        }
    }

    // Where the dash is along its way now, for the pose: 0 before it moves, 1 at its stop.
    static float dashDone(float age) {
        return (float) dashEase((age - DASH_WAIT) / DASH_MOVE);
    }

    @Nullable
    static Vec3 dashWay() {
        return dashAge >= 0 ? dashWay : null;
    }
}
