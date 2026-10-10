package nl.tivek.multiversepowers.character.thor.client.motion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
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
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorPowers;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.blow.ThorCombo;
import nl.tivek.multiversepowers.character.thor.hammer.HammerRules;
import nl.tivek.multiversepowers.engine.client.world.ChunkEdge;
import org.joml.Vector3f;

// Moves your own Thor: his game, not the server, moves a player, so every move of his is made here the moment its
// button is pressed (the server shows it to the others and hits what it hits). Speeds are in blocks per tick.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorMotion extends ThorGroundMotion {
    private static final int LIFT_TICKS = 12;
    private static final double LIFT = 0.5;
    private static final int BLINK_TICKS = 3;
    private static final double BLINK = 15.0;
    private static final double DIVE_SPEED = 2.1;
    private static final double DIVE_REACH = 32.0;
    private static final int DIVE_LONGEST = 80;
    private static final int EDGE_LOOK = 30;
    // At lightning speed he lands by himself once the ground comes this close below him while he sinks.
    private static final double LAND_BELOW = 2.5;
    // His hammer gone from his hand in flight (a Storm Throw), he hangs, sinking this fast.
    private static final double HANG_SINK = -0.03;
    // Called into his hand to fly, the hammer is waited for this long at most; a throw the server says nothing of
    // this long was refused.
    private static final int AWAIT_LONGEST = 40;
    private static final int GUESS_LONGEST = 20;
    // Space pressed twice within this many ticks in flight: he hops up and lets himself fall.
    private static final int DOUBLE_TAP = 7;
    private static final double HOP = 0.55;
    private static final double SINK = 0.8;
    private static int lastTap = -100;
    private static boolean jumpWas;

    private static int flightAge;
    private static Vec3 velocity = Vec3.ZERO;
    private static int blinkAge = -1;
    private static Vec3 blinkFrom = Vec3.ZERO;
    private static Vec3 blinkTo = Vec3.ZERO;
    private static int diveAge = -1;
    private static Vec3 diveAt = Vec3.ZERO;
    private static int lightningLeft;
    // Ticks into Throw and Follow's draw (from its start), and into the wait for the hammer called to fly; -1 when not.
    private static int drawAge = -1;
    private static int awaitAge = -1;
    // Ticks since a throw of his own game that the server has not spoken of yet; -1 once it has.
    private static int guessAge = -1;
    // The way he was last steering in flight: ahead, aside, up (+1, 0, -1 each).
    private static float steerForward;
    private static float steerStrafe;
    private static float steerUp;

    static {
        ClientCharacter.local(GameCharacter.THOR, ThorMotion::act);
        ClientCharacter.flying(GameCharacter.THOR, player -> flying);
        ClientCharacter.state(GameCharacter.THOR, ThorMotion::state);
    }

    // With the hammer in hand or not, running or not, the hammer on him or away: which of his gestures are his now.
    private static int state(LocalPlayer player) {
        ClientThor.View view = ClientThor.view(player);
        boolean away = away(player);
        boolean armed = view != null && view.has(ThorStatePayload.ARMED) && !away;
        return (armed ? ThorPowers.ARMED : ThorPowers.UNARMED)
                | (player.isSprinting() ? ThorPowers.SPRINTING : ThorPowers.WALKING)
                | (away ? ThorPowers.AWAY : ThorPowers.HOME);
    }

    // His hammer out of his hands: thrown, resting or coming back.
    public static boolean away(LocalPlayer player) {
        return ClientThor.has(player, ThorStatePayload.THROWN);
    }

    private ThorMotion() {
    }

    private static int act(LocalPlayer player, CharacterAbility ability, boolean on, int data) {
        boolean held = (data & Characters.HOLD) != 0;
        boolean slam = (data & Characters.SLAM) != 0;
        // Through the lightning bomb he does nothing else but call his storm's bolts.
        if (on && ThorRise.active() && !ability.id().equals("storm")) {
            return -1;
        }
        switch (ability.id()) {
            case "combo" -> {
                return on ? ThorCombo.act(player, ability, data) : -1;
            }
            case "dash" -> {
                if (!on || flying || dashAge >= 0) {
                    return on ? -1 : data;
                }
                return data | dash(player, ability) << Characters.MOVE_SHIFT;
            }
            case "grab_dash" -> {
                if (!on || !held || flying || dashAge >= 0) {
                    return on ? -1 : data;
                }
                return data | grabDash(player) << Characters.MOVE_SHIFT;
            }
            case "mjolnir" -> {
                ClientThor.View view = ClientThor.view(player);
                if (on && !flying && (view == null || !view.has(ThorStatePayload.THROWN))) {
                    ClientThor.flip(player, ThorStatePayload.ARMED);
                }
            }
            case "hammer_uppercut" -> {
                if (on && held && !flying) {
                    ClientThor.predictBlow(player, ThorBlow.HAMMER_UPPERCUT.ordinal());
                }
            }
            case "hammer_throw" -> {
                if (on && !flying && !away(player)) {
                    thrown(player, ThorBlow.HAMMER_THROW);
                }
            }
            // Held: drawn back over his shoulder; let go: thrown as far as it was drawn, in tenths of a block.
            case "hammer_leap" -> {
                if (on) {
                    if (held && !flying && !away(player)) {
                        drawAge = 0;
                    }
                    return data;
                }
                if (drawAge < 0) {
                    return -1;
                }
                double far = HammerRules.drawn(HammerRules.DRAW_FROM + drawAge, ability.value("throwBlocks"));
                drawAge = -1;
                thrown(player, ThorBlow.HAMMER_THROW);
                return data | Mth.clamp((int) Math.round(far * 10.0), 0, 255) << Characters.MOVE_SHIFT;
            }
            // Shown only once the server starts it: it may find nothing to aim at, or no room over him to toss it.
            case "storm_throw" -> {
                if (!on || away(player)) {
                    return -1;
                }
            }
            case "air_shockwave", "air_bolt" -> {
                if (on && flying && away(player)) {
                    return -1;
                }
            }
            case "super_jump" -> {
                if (on && !flying) {
                    jump(player, ability);
                }
            }
            // He flies only with the hammer: away, he waits for it to come into his raised left hand.
            case "flight" -> {
                if (!on) {
                    return -1;
                }
                if (held && !slam && !flying) {
                    if (away(player)) {
                        awaitAge = 0;
                    } else {
                        takeOff(player);
                    }
                }
            }
            case "air_blink" -> {
                if (!on || !flying || blinkAge >= 0 || diveAge >= 0 || away(player)) {
                    return -1;
                }
                return data | blink(player) << Characters.MOVE_SHIFT;
            }
            case "grab_dash_dive" -> {
                if (!on || held && !slam && away(player)) {
                    return -1;
                }
                if (held && !slam && flying && diveAge < 0) {
                    dive(player);
                }
            }
            case "lightning_flight" -> {
                if (!on || !held || !flying || lightning || away(player)) {
                    return -1;
                }
                lightning = true;
                lightningLeft = (int) Math.round(ability.value("seconds") * 20.0);
                ClientThor.flags(player, flags());
            }
            case "lightning_bomb" -> {
                if (!on || flying || ThorRise.active() || player.isPassenger()
                        || ClientThor.has(player, ThorStatePayload.CARRYING)) {
                    return -1;
                }
                jumpAge = -1;
                floatAge = -1;
                dashAge = -1;
                dropping = false;
                ThorRise.start(player);
                ClientThor.predict(player, ThorStatePayload.BOMB, 0, flags());
            }
            default -> {
            }
        }
        return data;
    }

    // A throw leaves his hand: shown at once, and the hammer counts as away, so its buttons are those for it gone. Not
    // heard of from the server in time (it refused), the hammer is his again.
    private static void thrown(LocalPlayer player, ThorBlow blow) {
        ClientThor.predictBlow(player, blow.ordinal());
        ClientThor.set(player, ThorStatePayload.THROWN, true);
        guessAge = 0;
    }

    // How far into Throw and Follow's draw he is, from its start to full (0 to 1), or -1 while not drawing.
    public static float drawn(float partialTick) {
        return drawAge < 0 ? -1.0F : Math.min(1.0F, (drawAge + partialTick)
                / (HammerRules.DRAW_FULL - HammerRules.DRAW_FROM));
    }

    // Whether he stands waiting, his left hand raised, for the hammer he called to fly with.
    public static boolean awaiting() {
        return awaitAge >= 0;
    }

    // The server says the hammer he called to fly with is in his hand: he takes off.
    public static void liftOff(LocalPlayer player) {
        if (awaitAge >= 0 && !flying) {
            awaitAge = -1;
            takeOff(player);
        }
    }

    // Arrived at his hammer up in the air: he catches it in his left hand and flies on, keeping `velocity`.
    public static void flyOn(LocalPlayer player, Vec3 velocity) {
        flying = true;
        flightAge = LIFT_TICKS + 1;
        ThorMotion.velocity = velocity;
        lightning = false;
        jumpAge = -1;
        floatAge = -1;
        dashAge = -1;
        ClientThor.predict(player, ThorStatePayload.CATCH, ThorStatePayload.LEFT_HAND, flags());
    }

    private static void takeOff(LocalPlayer player) {
        flying = true;
        flightAge = 0;
        lastTap = -100;
        jumpWas = true;
        velocity = player.getDeltaMovement();
        lightning = false;
        jumpAge = -1;
        floatAge = -1;
        dashAge = -1;
        ClientThor.predict(player, ThorStatePayload.TAKE_OFF, 0, flags());
    }

    // Fifteen blocks the way he was steering (ahead along his look, back, aside or up), or along his look: packed as
    // yaw and pitch in 256ths for the server.
    private static int blink(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 wish = look.scale(steerForward).add(right.scale(-steerStrafe)).add(0.0, steerUp, 0.0);
        if (wish.lengthSqr() > 1.0E-4) {
            look = wish.normalize();
        }
        int yawByte = Math.floorMod(Math.round((float) Math.toDegrees(Math.atan2(-look.x, look.z)) / 360.0F
                * 256.0F), 256);
        int pitchByte = Mth.clamp(Math.round(((float) Math.toDegrees(-Math.asin(Mth.clamp(look.y, -1.0, 1.0)))
                + 90.0F) / 180.0F * 255.0F), 0, 255);
        // The server's own way from the bytes, so both blink the same.
        look = ThorMoves.blinkWay(yawByte, pitchByte);
        blinkFrom = player.position();
        Vec3 from = blinkFrom.add(0.0, 0.9 * player.getScale(), 0.0);
        HitResult hit = player.level().clip(new ClipContext(from, from.add(look.scale(BLINK)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double far = hit.getType() == HitResult.Type.MISS ? BLINK
                : Math.max(0.0, hit.getLocation().distanceTo(from) - 0.6 * player.getScale());
        blinkTo = blinkFrom.add(look.scale(far));
        blinkAge = 0;
        ClientThor.predict(player, ThorStatePayload.BLINK, 0, flags());
        return yawByte | pitchByte << 8;
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

    // Let go of a knockdown still in the air: he flies on from where he is, with no lift.
    public static void caught(LocalPlayer player) {
        if (flying) {
            return;
        }
        takeOff(player);
        flightAge = LIFT_TICKS + 1;
    }

    // The server's word on your own Thor: knocked out of the sky, he falls; out of a pull, he drops; the hammer put
    // away or gone, a draw ends.
    public static void told(ClientThor.View view) {
        guessAge = -1;
        if (flying && flightAge > LIFT_TICKS && !view.has(ThorStatePayload.FLYING)) {
            flying = false;
            lightning = false;
            diveAge = -1;
            blinkAge = -1;
        }
        ThorPull.told(view.has(ThorStatePayload.PULLING));
        if (drawAge > 2 && !view.has(ThorStatePayload.COCKED) && !view.has(ThorStatePayload.THROWN)) {
            drawAge = -1;
        }
    }

    // What the server said about his lightning speed wins once it had time to hear of it.
    public static void toldLightning(boolean on, int sinceStart) {
        if (lightning && !on && sinceStart > 10) {
            lightning = false;
        }
    }

    public static void stop() {
        dashAge = -1;
        jumpAge = -1;
        floatAge = -1;
        flying = false;
        lightning = false;
        blinkAge = -1;
        diveAge = -1;
        dropping = false;
        lightningLeft = 0;
        velocity = Vec3.ZERO;
        drawAge = -1;
        awaitAge = -1;
        ThorPull.stop();
        ThorRise.stop();
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        if (ClientCharacter.active() != GameCharacter.THOR || player.isPassenger() || player.isSpectator()
                || player.getAbilities().flying || !player.isAlive()) {
            if (flying || jumpAge >= 0 || dashAge >= 0 || drawAge >= 0 || awaitAge >= 0 || ThorRise.active()) {
                stop();
            }
            return;
        }
        Input input = event.getInput();
        if (ThorRise.active()) {
            ThorRise.tick(player, input);
            return;
        }
        if (drawAge >= 0) {
            drawing(player);
        }
        if (awaitAge >= 0 && (++awaitAge > AWAIT_LONGEST || !away(player))) {
            awaitAge = -1;
        }
        if (guessAge >= 0 && ++guessAge > GUESS_LONGEST) {
            guessAge = -1;
            ClientThor.set(player, ThorStatePayload.THROWN, false);
        }
        if (flying) {
            fly(player, input);
            return;
        }
        if (ThorPull.pulling(player, input)) {
            return;
        }
        if (awaitAge >= 0) {
            still(input);
            return;
        }
        if (dashAge >= 0) {
            dashing(player, input);
        }
        if (jumpAge >= 0) {
            jumping(player, input);
        }
    }

    private static void fly(LocalPlayer player, Input input) {
        float forward = input.forwardImpulse;
        float strafe = input.leftImpulse;
        boolean up = input.jumping;
        steerForward = forward;
        steerStrafe = strafe;
        // Sneaking sinks him, unless its key is the one held for lightning speed (by default both are shift).
        boolean down = !up && input.shiftKeyDown
                && !Minecraft.getInstance().options.keyShift.same(PowerInputs.HOLD_SHIFT);
        steerUp = up ? 1.0F : down ? -1.0F : 0.0F;
        still(input);
        input.shiftKeyDown = false;
        if (up && !jumpWas) {
            if (flightAge - lastTap <= DOUBLE_TAP && flightAge > LIFT_TICKS && blinkAge < 0 && diveAge < 0) {
                jumpWas = true;
                hop(player);
                return;
            }
            lastTap = flightAge;
        }
        jumpWas = up;
        flightAge++;
        if (lightning && --lightningLeft <= 0) {
            lightning = false;
            ClientThor.flags(player, flags());
        }
        CharacterAbility flight = GameCharacter.THOR.byName("flight");
        CharacterAbility fast = GameCharacter.THOR.byName("lightning_flight");
        double speed = (lightning && fast != null ? fast.value("speed") : flight == null ? 18.0
                : flight.value("speed")) / 20.0;
        if (blinkAge >= 0) {
            int t = blinkAge++;
            if (t < BLINK_TICKS) {
                velocity = blinkFrom.lerp(blinkTo, (t + 1.0) / BLINK_TICKS).subtract(player.position());
            } else {
                blinkAge = -1;
                Vec3 on = blinkTo.subtract(blinkFrom);
                velocity = on.lengthSqr() < 1.0E-6 ? Vec3.ZERO : on.normalize().scale(speed * 0.6);
            }
        } else if (diveAge >= 0) {
            if (diving(player)) {
                return;
            }
        } else if (away(player)) {
            // His hammer hurled from his hand, he hangs in the air, arms out, sinking slowly, until it is back.
            velocity = velocity.lerp(new Vec3(0.0, HANG_SINK, 0.0), 0.2);
        } else {
            Vec3 look = player.getLookAngle();
            double yaw = Math.toRadians(player.getYRot());
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 wish = look.scale(forward).add(right.scale(-strafe)).add(0.0, up ? 0.8 : down ? -SINK : 0.0, 0.0);
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
        boolean landing = player.onGround() || lightning && velocity.y < -0.2 && groundWithin(player, LAND_BELOW);
        if (flightAge > LIFT_TICKS && landing && velocity.y <= 0.02 && diveAge < 0 && blinkAge < 0) {
            touchDown(player);
        }
    }

    // Drawing the hammer back for Throw and Follow: in his own view a small crackling mark shows where it would stop
    // (the first block or creature along his look, else as far as it is drawn); held this long, it goes by itself.
    private static void drawing(LocalPlayer player) {
        CharacterAbility leap = GameCharacter.THOR.byName("hammer_leap");
        if (leap == null || away(player) || flying) {
            drawAge = -1;
            return;
        }
        drawAge++;
        if (HammerRules.DRAW_FROM + drawAge >= HammerRules.DRAW_LONGEST) {
            ClientCharacter.sendAction(leap, false, 0);
            return;
        }
        double far = HammerRules.drawn(HammerRules.DRAW_FROM + drawAge, leap.value("throwBlocks"));
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(far));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult creature = ProjectileUtil.getEntityHitResult(player, eye, stop,
                player.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator(),
                eye.distanceToSqr(stop));
        if (creature != null) {
            stop = creature.getLocation();
        }
        Level level = player.level();
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, stop.x, stop.y, stop.z, 0.0, 0.0, 0.0);
        if (drawAge % 2 == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.62F, 0.91F, 1.0F), 0.9F), stop.x, stop.y, stop.z,
                    0.0, 0.0, 0.0);
        }
    }

    private static boolean groundWithin(LocalPlayer player, double below) {
        Vec3 feet = player.position();
        return player.level().clip(new ClipContext(feet, feet.add(0.0, -below, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player)).getType() != HitResult.Type.MISS;
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

    // Double space in flight: a hop up, then he falls like anyone jumping.
    private static void hop(LocalPlayer player) {
        flying = false;
        lightning = false;
        velocity = Vec3.ZERO;
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x * 0.6, HOP, v.z * 0.6);
        CharacterAbility flight = GameCharacter.THOR.byName("flight");
        if (flight != null) {
            ClientCharacter.sendAction(flight, true, Characters.SLAM | ThorPowers.DROP << Characters.MOVE_SHIFT);
        }
        ClientThor.predict(player, ThorStatePayload.NONE, 0, flags());
    }

    // Steers the dive: at what he grabs, then straight down with it. True once he slammed into the ground.
    private static boolean diving(LocalPlayer player) {
        int t = diveAge++;
        ClientThor.View view = ClientThor.view(player);
        boolean carrying = view != null && view.has(ThorStatePayload.CARRYING);
        Vec3 at = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
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

}
