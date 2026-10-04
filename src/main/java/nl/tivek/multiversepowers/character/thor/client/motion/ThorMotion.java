package nl.tivek.multiversepowers.character.thor.client.motion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
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
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorPowers;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.blow.ThorCombo;
import nl.tivek.multiversepowers.engine.client.world.ChunkEdge;

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

    private static int flightAge;
    private static Vec3 velocity = Vec3.ZERO;
    private static int blinkAge = -1;
    private static Vec3 blinkFrom = Vec3.ZERO;
    private static Vec3 blinkTo = Vec3.ZERO;
    private static int diveAge = -1;
    private static Vec3 diveAt = Vec3.ZERO;
    private static int lightningLeft;
    // The way he was last steering in flight: ahead, aside, up (+1, 0, -1 each).
    private static float steerForward;
    private static float steerStrafe;
    private static float steerUp;

    static {
        ClientCharacter.local(GameCharacter.THOR, ThorMotion::act);
        ClientCharacter.flying(GameCharacter.THOR, player -> flying);
        ClientCharacter.state(GameCharacter.THOR, ThorMotion::state);
    }

    // With the hammer in hand or not, running or not: which of his gestures are his now.
    private static int state(LocalPlayer player) {
        ClientThor.View view = ClientThor.view(player);
        boolean armed = view != null && view.has(ThorStatePayload.ARMED) && !view.has(ThorStatePayload.THROWN);
        return (armed ? ThorPowers.ARMED : ThorPowers.UNARMED)
                | (player.isSprinting() ? ThorPowers.SPRINTING : ThorPowers.WALKING);
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
                if (!on || !flying || blinkAge >= 0 || diveAge >= 0) {
                    return -1;
                }
                return data | blink(player) << Characters.MOVE_SHIFT;
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
                if (!on || !held || !flying || lightning) {
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

    // The server's word on your own Thor: knocked out of the sky, he falls.
    public static void told(ClientThor.View view) {
        if (flying && flightAge > LIFT_TICKS && !view.has(ThorStatePayload.FLYING)) {
            flying = false;
            lightning = false;
            diveAge = -1;
            blinkAge = -1;
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
            if (flying || jumpAge >= 0 || dashAge >= 0 || ThorRise.active()) {
                stop();
            }
            return;
        }
        Input input = event.getInput();
        if (ThorRise.active()) {
            ThorRise.tick(player, input);
            return;
        }
        if (flying) {
            fly(player, input);
            return;
        }
        if (ThorPull.pulling(player)) {
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
        steerUp = up ? 1.0F : 0.0F;
        still(input);
        // Sneaking never sinks him in flight: by default its key is shift, held there for lightning speed.
        input.shiftKeyDown = false;
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
        } else {
            Vec3 look = player.getLookAngle();
            double yaw = Math.toRadians(player.getYRot());
            Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 wish = look.scale(forward).add(right.scale(-strafe)).add(0.0, up ? 0.8 : 0.0, 0.0);
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
