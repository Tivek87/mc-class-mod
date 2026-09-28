package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.client.world.ChunkEdge;

// Moves your own Thor: his game, not the server, moves a player, so every move of his is made here the moment its
// button is pressed (the server shows it to the others and hits what it hits). Speeds are in blocks per tick.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorMotion {
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
    private static final int LIFT_TICKS = 12;
    private static final double LIFT = 0.5;
    private static final int BLINK_TICKS = 3;
    private static final double DIVE_SPEED = 2.1;
    private static final double DIVE_REACH = 32.0;
    private static final int DIVE_LONGEST = 80;
    private static final int EDGE_LOOK = 30;
    private static final RandomSource RANDOM = RandomSource.create();

    private static int dashAge = -1;
    private static Vec3 dashWay = Vec3.ZERO;
    private static double dashFar;
    private static int jumpAge = -1;
    private static int jumpWait;
    private static double launch;
    private static int floatAge = -1;
    private static int floatTicks;
    private static boolean flying;
    private static int flightAge;
    private static Vec3 velocity = Vec3.ZERO;
    private static boolean lightning;
    private static int blinkAge = -1;
    private static Vec3 blinkFrom = Vec3.ZERO;
    private static Vec3 blinkTo = Vec3.ZERO;
    private static int diveAge = -1;
    private static Vec3 diveAt = Vec3.ZERO;

    static {
        ClientCharacter.local(GameCharacter.THOR, ThorMotion::act);
        ClientCharacter.flying(GameCharacter.THOR, player -> flying);
    }

    private ThorMotion() {
    }

    public static boolean flying() {
        return flying;
    }

    public static boolean lightning() {
        return flying && lightning;
    }

    // Floating at the top of a super jump, 0 to 1 as it fades.
    public static boolean floating() {
        return floatAge >= 0 && floatAge < floatTicks;
    }

    private static int flags() {
        int flags = flying ? ThorStatePayload.FLYING : 0;
        flags |= floating() ? ThorStatePayload.FLOATING : 0;
        flags |= lightning() ? ThorStatePayload.LIGHTNING : 0;
        return flags;
    }

    private static int act(LocalPlayer player, CharacterAbility ability, boolean on, int data) {
        boolean held = (data & Characters.HOLD) != 0;
        boolean slam = (data & Characters.SLAM) != 0;
        switch (ability.id()) {
            case "dash" -> {
                if (!on || flying || dashAge >= 0) {
                    return on ? -1 : data;
                }
                return data | dash(player, ability) << Characters.MOVE_SHIFT;
            }
            case "super_jump" -> {
                if (on && !flying) {
                    jump(player, ability);
                }
            }
            case "flight" -> {
                if (!on) {
                    return -1;
                }
                if (held && !slam && !flying) {
                    takeOff(player);
                }
            }
            case "air_blink" -> {
                if (on && flying && blinkAge < 0 && diveAge < 0) {
                    blink(player);
                }
            }
            case "grab_dash_dive" -> {
                if (!on) {
                    return -1;
                }
                if (held && !slam && flying && diveAge < 0) {
                    dive(player);
                }
            }
            case "lightning_flight" -> {
                lightning = on && flying;
                ClientThor.flags(player, flags());
            }
            default -> {
            }
        }
        return data;
    }

    // The way he was moving, or ahead when standing: packed with how far into the bits the server reads.
    private static int dash(LocalPlayer player, CharacterAbility ability) {
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

    private static void jump(LocalPlayer player, CharacterAbility ability) {
        jumpAge = 0;
        jumpWait = player.onGround() ? CROUCH : TUCK;
        launch = launchFor(ability.value("heightBlocks"));
        floatTicks = (int) Math.round(ability.value("floatSeconds") * 20.0);
        floatAge = -1;
        dashAge = -1;
        ClientThor.predict(player, ThorStatePayload.JUMP, 0, flags());
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

    private static void takeOff(LocalPlayer player) {
        flying = true;
        flightAge = 0;
        velocity = player.getDeltaMovement();
        lightning = false;
        jumpAge = -1;
        floatAge = -1;
        dashAge = -1;
        ClientThor.predict(player, ThorStatePayload.TAKE_OFF, 0, flags());
    }

    private static void blink(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        blinkFrom = player.position();
        Vec3 from = blinkFrom.add(0.0, 0.9, 0.0);
        double blink = GameCharacter.THOR.byName("air_blink").value("distanceBlocks");
        HitResult hit = player.level().clip(new ClipContext(from, from.add(look.scale(blink)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double far = hit.getType() == HitResult.Type.MISS ? blink : Math.max(0.0, hit.getLocation().distanceTo(from) - 0.6);
        blinkTo = blinkFrom.add(look.scale(far));
        blinkAge = 0;
        ClientThor.predict(player, ThorStatePayload.BLINK, 0, flags());
    }

    private static void dive(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(look.scale(DIVE_REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        diveAt = hit.getLocation();
        diveAge = 0;
        lightning = false;
        ClientThor.predict(player, ThorStatePayload.DIVE, 0, flags());
    }

    // The server's word on your own Thor: knocked out of the sky, he falls.
    static void told(ClientThor.View view) {
        if (flying && flightAge > LIFT_TICKS && !view.has(ThorStatePayload.FLYING)) {
            flying = false;
            lightning = false;
            diveAge = -1;
            blinkAge = -1;
        }
    }

    static void stop() {
        dashAge = -1;
        jumpAge = -1;
        floatAge = -1;
        flying = false;
        lightning = false;
        blinkAge = -1;
        diveAge = -1;
        velocity = Vec3.ZERO;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        if (ClientCharacter.active() != GameCharacter.THOR || player.isPassenger() || player.isSpectator()
                || player.getAbilities().flying || !player.isAlive()) {
            if (flying || jumpAge >= 0 || dashAge >= 0) {
                stop();
            }
            return;
        }
        Input input = event.getInput();
        if (flying) {
            fly(player, input);
            return;
        }
        if (dashAge >= 0) {
            dashing(player, input);
        }
        if (jumpAge >= 0) {
            jumping(player, input);
        }
    }

    private static void still(Input input) {
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
    }

    // How far along a dash is after `u` of its moving part: quick off the mark, easing into the stop.
    static double dashEase(double u) {
        double k = Mth.clamp(u, 0.0, 1.0);
        return 1.0 - (1.0 - k) * (1.0 - k);
    }

    private static void dashing(LocalPlayer player, Input input) {
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

    private static void jumping(LocalPlayer player, Input input) {
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

    private static void fly(LocalPlayer player, Input input) {
        float forward = input.forwardImpulse;
        float strafe = input.leftImpulse;
        boolean up = input.jumping;
        boolean down = input.shiftKeyDown;
        still(input);
        input.shiftKeyDown = false;
        flightAge++;
        CharacterAbility flight = GameCharacter.THOR.byName("flight");
        CharacterAbility fast = GameCharacter.THOR.byName("lightning_flight");
        double speed = (lightning && fast != null ? fast.value("speed") : flight == null ? 18.0
                : flight.value("speed")) / 20.0;
        if (blinkAge >= 0) {
            int t = blinkAge++;
            Vec3 goal = blinkFrom.lerp(blinkTo, Math.min(1.0, (t + 1.0) / BLINK_TICKS));
            velocity = goal.subtract(player.position());
            if (blinkAge >= BLINK_TICKS) {
                blinkAge = -1;
                Vec3 on = blinkTo.subtract(blinkFrom);
                velocity = on.lengthSqr() < 1.0E-6 ? Vec3.ZERO : on.normalize().scale(speed * 0.6);
            }
        } else if (diveAge >= 0) {
            if (diving(player)) {
                return;
            }
        } else {
            Vec3 look = player.getLookAngle();
            double yaw = Math.toRadians(player.getYRot());
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 wish = look.scale(forward).add(right.scale(-strafe)).add(0.0, (up ? 0.8 : 0.0) - (down ? 0.8 : 0.0),
                    0.0);
            if (wish.lengthSqr() > 1.0) {
                wish = wish.normalize();
            }
            wish = wish.scale(speed);
            if (flightAge < LIFT_TICKS) {
                wish = wish.add(0.0, LIFT * (1.0 - (double) flightAge / LIFT_TICKS), 0.0);
            }
            double pull = wish.lengthSqr() < 1.0E-6 ? 0.18 : lightning ? 0.22 : 0.14;
            velocity = velocity.lerp(wish, pull);
        }
        velocity = ChunkEdge.cap(player.level(), player.position(), velocity, EDGE_LOOK);
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        if (flightAge > LIFT_TICKS && player.onGround() && velocity.y <= 0.02 && diveAge < 0 && blinkAge < 0) {
            touchDown(player);
        }
    }

    private static void touchDown(LocalPlayer player) {
        flying = false;
        lightning = false;
        velocity = Vec3.ZERO;
        CharacterAbility flight = GameCharacter.THOR.byName("flight");
        if (flight != null) {
            ClientCharacter.sendAction(flight, true, Characters.SLAM);
        }
        ClientThor.predict(player, ThorStatePayload.TOUCH_DOWN, 0, flags());
    }

    // Steers the dive: at what he grabs, then straight down with it. True once he slammed into the ground.
    private static boolean diving(LocalPlayer player) {
        int t = diveAge++;
        ClientThor.View view = ClientThor.view(player);
        boolean carrying = view != null && view.has(ThorStatePayload.CARRYING);
        Vec3 at = player.position().add(0.0, 0.9, 0.0);
        Vec3 goal;
        if (carrying) {
            Vec3 look = player.getLookAngle();
            goal = new Vec3(look.x * 0.25, -1.0, look.z * 0.25);
        } else {
            Entity target = view == null || view.carried < 0 ? null : player.level().getEntity(view.carried);
            Vec3 aim = target != null && target.isAlive() ? target.getBoundingBox().getCenter() : diveAt;
            goal = aim.subtract(at);
            if (goal.lengthSqr() < 2.25) {
                goal = new Vec3(0.0, -1.0, 0.0);
            }
        }
        velocity = velocity.lerp(goal.normalize().scale(DIVE_SPEED), 0.35);
        if (player.onGround() && t > 1 || player.verticalCollisionBelow) {
            slam(player);
            return true;
        }
        if (t > DIVE_LONGEST) {
            diveAge = -1;
        }
        return false;
    }

    private static void slam(LocalPlayer player) {
        CharacterAbility dive = GameCharacter.THOR.byName("grab_dash_dive");
        diveAge = -1;
        flying = false;
        lightning = false;
        velocity = Vec3.ZERO;
        player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
        if (dive != null) {
            ClientCharacter.sendAction(dive, true, Characters.SLAM);
        }
        ClientThor.predict(player, ThorStatePayload.SLAM, 0, flags());
    }

    // Where the dash is along its way now, for the pose: 0 before it moves, 1 at its stop.
    static float dashDone(float age) {
        return (float) dashEase((age - DASH_WAIT) / DASH_MOVE);
    }

    @Nullable
    static Vec3 dashWay() {
        return dashAge >= 0 ? dashWay : null;
    }

    static Vec3 flightVelocity() {
        return velocity;
    }

}
