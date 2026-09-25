package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlightPose;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.world.ChunkEdge;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlyerTracker.track;

/**
 * Flying as Green Lantern, on this side. Every game moves its own player, so your own flight is steered here:
 * <ul>
 * <li><b>Taking off</b> (see {@link Flight#ARISE_TICKS}), with the flight key or by tapping jump twice: you stop
 * where you are while your fists come to your chest, then your arms sweep down along your sides and you rise a few
 * blocks, looking up, and from there you fly on without a break.</li>
 * <li><b>Flying</b>: hold forward and you pick up speed the way you look: within a moment you are up to cruising
 * speed, and from there you keep gaining until a few seconds later you reach the top speed; let go and you glide to a
 * hover. Jump and sneak rise and sink, left and right slide sideways. You carry your speed
 * into every turn, so you swing through curves instead of snapping round.</li>
 * <li>The dome works as a brake chute: while it is up your speed is halved.</li>
 * <li>Walls and the ground stop you; knocks from a hit or a blast move you as they would anyone. Sink down onto
 * the ground slowly and you land by yourself, and fly into it looking down and you land as well. Dive into it at full
 * speed and you land with a slam: just before the ground you swing upright, feet first, with your ring fist cocked,
 * and you come down on one knee with that fist smashed into the ground while the ring throws up a construct (see
 * LandingSlam). The shockwave key does that dive for you: straight down at full speed, into a slam.</li>
 * <li>An empty ring lets you sink down gently, with no more steering, until you touch ground.</li>
 * <li>You never stop dead at the edge of the world your own game has: the server makes it ready round you and far
 * ahead (see Flight), and should you still catch up with the edge, you slow down smoothly before it and fly on once
 * the world is there (see {@link ChunkEdge}).</li>
 * </ul>
 * For everyone who flies, you included, this also keeps how fast they go and how they bank for their poses
 * (see {@link FlightPose}), draws the streak of light behind them at speed, throws up dust and spray where they
 * skim the ground or water, and plays the wind. Your own steering is worked out in {@link FlightSteering}, which
 * this builds on, and every flyer is followed from tick to tick by {@link FlyerTracker}.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientFlight extends FlightSteering {
    // Part of the top speed that counts as fast: the streak of light, the dust and the wind start around here.
    private static final double FAST = 0.51;

    // ---- Everyone who flies, as seen ----
    private static final Map<Integer, Motion> MOTIONS = new HashMap<>();

    private ClientFlight() {
    }

    /** How one flyer moves, smoothed out, for the poses and the light around them. */
    public static final class Motion {
        /** How fast and which way, in blocks per tick; and the same one tick earlier. */
        public Vec3 velocity = Vec3.ZERO;
        Vec3 velocityO = Vec3.ZERO;
        /** Lean into a turn, in radians: positive leans to the right. */
        public float bank;
        /** The way the body faced last tick, in degrees, to tell how fast it turns. */
        float lastYaw = Float.NaN;
        /** Where the middle of the body was on the last ticks, newest first: the streak of light. */
        final ArrayDeque<Vec3> trail = new ArrayDeque<>();
        /** How far the ground is below, in blocks (up to {@link FlyerTracker#SKIM} and a bit). */
        double ground = 99.0;
        /** Ticks since the flight ended, for the landing; very large while the flight goes on. */
        public int sinceEnd = Integer.MAX_VALUE;
        boolean flew;
        /** 0 to 1: how far he has swung upright for a slam into the ground just ahead; last tick and now. */
        float braceO;
        float brace;
    }

    /**
     * 0 to 1: how far this flyer has swung upright for a slam, feet first and ring fist cocked, because the ground
     * is only a few ticks away along his dive at full speed.
     */
    public static float brace(Entity player, float partialTick) {
        Motion motion = MOTIONS.get(player.getId());
        return motion == null ? 0.0F : Mth.lerp(partialTick, motion.braceO, motion.brace);
    }

    /** How a flyer moves right now, or null for someone who does not fly (and has not just landed). */
    @Nullable
    public static Motion motion(Entity entity) {
        return MOTIONS.get(entity.getId());
    }

    /**
     * True while this player drops straight down to a slam: the shockwave key, used jumping or falling rather than
     * flying. The ring drives him down, fist cocked.
     */
    public static boolean dropping(Entity player) {
        return ClientRing.flight(player, 0.0F) < 0.0F && ClientRing.has(player, RingPayload.DIVE);
    }

    /** Your own speed in blocks per tick while you steer yourself through the air; zero otherwise. */
    public static Vec3 ownVelocity() {
        return steering ? velocity : Vec3.ZERO;
    }

    /** The same, but gliding from last tick's speed to this tick's, for what is drawn in between. */
    public static Vec3 ownVelocity(float partialTick) {
        return steering ? velocityO.lerp(velocity, partialTick) : Vec3.ZERO;
    }

    /** How fast a flyer moves right now, gliding between two ticks; zero for someone who does not fly. */
    static Vec3 velocity(Entity flyer, float partialTick) {
        if (flyer == Minecraft.getInstance().player) {
            return ownVelocity(partialTick);
        }
        Motion motion = MOTIONS.get(flyer.getId());
        return motion == null ? Vec3.ZERO : motion.velocityO.lerp(motion.velocity, partialTick);
    }

    /**
     * How many ticks ago this player hit the ground with a landing slam (with the part of a tick), or -1 when he
     * did not just now. Your own is known the moment you land; anyone else's once the server's construct arrives.
     * Counted at the pace the constructs were made for, like the timeline of {@link LandingSlam}: your own at the pace
     * of your own settings until the server's construct tells its own.
     */
    public static float slam(Entity player, float partialTick) {
        float seen = ClientConstructs.slamAge(player.getId(), partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && slamTick != Integer.MIN_VALUE) {
            CharacterAbility shockwave = GameCharacter.GREEN_LANTERN.byName("shockwave");
            double pace = shockwave == null ? 1.0 : shockwave.value("slowMotion");
            float own = (float) ((player.tickCount - slamTick + partialTick) / pace);
            if (own >= 0.0F && own < LandingSlam.END_TICK) {
                seen = Math.max(seen, own);
            }
        }
        return seen;
    }

    // ---- Steering yourself ----

    /**
     * Your keys, right after the game read them and right before it moves you. While you fly the keys are yours:
     * the game itself gets none of them (so it never walks, jumps or crouches), and your speed is set from them.
     */
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        velocityO = velocity;
        // Dropping down to a slam and on the ground now: it is known the moment you touch it (the server throws up
        // the construct).
        if (player.onGround() && dropping(player) && slam(player, 0.0F) < 0.0F) {
            slamTick = player.tickCount;
        }
        // Right after a slam you stay down on your fist a moment: crouched, and going nowhere.
        float slammed = slam(player, 0.0F);
        if (slammed >= 0.0F && slammed < SLAM_ROOT && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            Input input = event.getInput();
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.jumping = false;
            input.shiftKeyDown = true;
            velocity = Vec3.ZERO;
            afterMove = null;
            // No sliding on, but still falling: a slam that begins a moment before you touch down never leaves you
            // hanging in the air.
            player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
            return;
        }
        float t = ClientRing.flight(player, 0.0F);
        if (t < 0.0F || ClientCharacter.active() != GameCharacter.GREEN_LANTERN || player.isPassenger()
                || player.isSpectator() || player.getAbilities().flying) {
            stop();
            return;
        }
        Input input = event.getInput();
        float forward = input.forwardImpulse;
        float strafe = input.leftImpulse;
        boolean up = input.jumping;
        boolean down = input.shiftKeyDown;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
        if (!steering) {
            steering = true;
            velocity = player.getDeltaMovement();
            afterMove = null;
            airborne = false;
            landing = false;
            momentum = 0.0;
        }
        absorb(player);
        Vec3 impact = landedWith;
        landedWith = null;
        boolean dove = false;
        // On a dive for a slam (the shockwave key): your keys wait until you hit the ground.
        boolean onDive = t >= ARISE && ClientRing.has(player, RingPayload.DIVE)
                && !ClientRing.has(player, RingPayload.DESCENT);
        if (impact != null && t >= ARISE && player.onGround() && !ClientRing.has(player, RingPayload.DESCENT)) {
            // Flown into the ground at full speed, diving, or at the end of a dive for a slam: a slam instead of a
            // landing.
            if (onDive || impact.length() >= fullSpeed() * SLAM_SPEED && -impact.y >= impact.length() * SLAM_DOWN) {
                slamDown(player);
                return;
            }
            // Flown into it slower but on purpose, looking down at it: you land all the same.
            dove = -impact.y > DIVE_LAND && player.getXRot() > DIVE_LOOK;
        }
        if (ClientRing.has(player, RingPayload.DESCENT)) {
            velocity = new Vec3(velocity.x * 0.95, Mth.lerp(0.15, velocity.y, -SINK), velocity.z * 0.95);
        } else if (t < ARISE) {
            velocity = arise(player, t, forward, strafe, up, down);
        } else if (onDive) {
            // Straight down at dive speed, swinging round into it out of whatever way you flew.
            velocity = velocity.lerp(new Vec3(0.0, -Math.max(fullSpeed(), DIVE_SPEED), 0.0), DIVE_TURN);
        } else {
            gainSpeed(player, forward > 0.01F);
            velocity = steer(player, forward, strafe, up, down, 1.0);
        }
        if (!player.onGround()) {
            airborne = true;
        }
        if (dove) {
            // Down on your feet where you hit the ground, not sliding on over it.
            velocity = Vec3.ZERO;
        }
        // Sinking down onto the ground slowly after the take-off, or flown into it: you land by yourself. A dive that
        // starts on the ground slams into it on the next tick instead.
        if (!landing && !onDive && (dove || airborne && t > ARISE + 4.0F && player.onGround() && !up
                && velocity.horizontalDistance() < LAND_SPEED && !ClientRing.has(player, RingPayload.DESCENT))) {
            landing = true;
            CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
            if (flight != null) {
                PacketDistributor.sendToServer(new AbilityActionPayload(flight.slot().ordinal(), true, 0));
            }
        }
        // Never on into a chunk your own game does not have yet (it would stop you dead there until it comes in): you
        // slow down smoothly before its edge instead, and fly on once it is there.
        velocity = ChunkEdge.cap(player.level(), player.position(), velocity, EDGE_LOOK);
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        if (wind == null || wind.isStopped()) {
            wind = new WindSound(player);
            minecraft.getSoundManager().play(wind);
        }
    }

    /** Right after the game moved you: what it made of your speed, to spot walls and knocks next tick. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (steering && event.getEntity() == Minecraft.getInstance().player) {
            afterMove = event.getEntity().getDeltaMovement();
            stoppedDown = event.getEntity().verticalCollisionBelow;
            stoppedUp = event.getEntity().verticalCollision && !stoppedDown;
        }
    }

    // ---- Everyone who flies, as seen ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.player != null) {
            doubleJump(minecraft, minecraft.player);
        }
        for (AbstractClientPlayer player : level.players()) {
            boolean flying = ClientRing.flight(player, 0.0F) >= 0.0F;
            boolean dropping = dropping(player);
            Motion motion = MOTIONS.get(player.getId());
            if (motion == null) {
                if (!flying && !dropping) {
                    continue;
                }
                motion = new Motion();
                MOTIONS.put(player.getId(), motion);
            }
            track(level, player, motion, flying, dropping, player == minecraft.player && steering);
        }
        // A flyer that went out of sight, or landed a while ago, is forgotten.
        Iterator<Map.Entry<Integer, Motion>> all = MOTIONS.entrySet().iterator();
        while (all.hasNext()) {
            Map.Entry<Integer, Motion> entry = all.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entry.getValue().sinceEnd > 40 && entry.getValue().sinceEnd != Integer.MAX_VALUE) {
                all.remove();
            }
        }
    }

    // ---- What you see and hear of it ----

    /** The streak of hard light that trails behind every fast flyer. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || MOTIONS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        LanternPainter painter = null;
        for (Map.Entry<Integer, Motion> entry : MOTIONS.entrySet()) {
            Motion motion = entry.getValue();
            Entity entity = level.getEntity(entry.getKey());
            double speed = motion.velocity.length();
            if (entity == null || entity.isInvisible() || motion.trail.size() < 3 || speed < fast() * 0.5) {
                continue;
            }
            if (painter == null) {
                painter = new LanternPainter(event.getPoseStack(), camera.getPosition(),
                        (float) (level.getGameTime() % 24000L) + partialTick);
            }
            Vec3 head = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
            painter.trail(head, motion.trail, Mth.clamp((speed - fast() * 0.5) / fast(), 0.0, 1.0),
                    entity == minecraft.player && !camera.isDetached());
        }
        if (painter != null) {
            painter.finish(minecraft.renderBuffers().bufferSource());
        }
    }

    /** At speed the world widens a little around you, the way it does when you sprint. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!steering || event.getPlayer() != Minecraft.getInstance().player) {
            return;
        }
        double fast = Mth.clamp(velocity.length() / fullSpeed(), 0.0, 1.0);
        float effect = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.14F * (float) fast * effect));
    }

    /**
     * How hard your view shakes because you fly with the ram cone low along the ground (see {@link Flight#scraping}):
     * harder the faster you go, as hard as your own setting {@code ramGroundShake} makes it; 0 when you do not.
     */
    private static float scrapeShake(LocalPlayer player) {
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        double speed = velocity.length();
        if (!steering || shield == null || speed < Flight.SCRAPE_SPEED || !ClientRing.has(player, RingPayload.SHIELD)
                || !Flight.scraping(player, shield.value("ramGroundBlocks"))) {
            return 0.0F;
        }
        return (float) (ClientSettings.get(ClientSettings.RAM_GROUND_SHAKE) * 0.55
                * Mth.clamp(speed / fullSpeed(), 0.3, 1.0));
    }

    /**
     * A landing slam shakes the view: your own landing a little, and the shockwave of any slam nearby hard. In first
     * person your own slam also dips your view for a moment, down to your fist in the ground; then it looks up at the
     * construct taking shape in the air before you and follows it down as it strikes (see {@link SlamPainter#look}).
     * As you rise off the ground, your view tips up a little with the head of the body.
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float shake = Math.max(ClientConstructs.shake(event.getCamera().getPosition(), partialTick),
                scrapeShake(player));
        float slammed = slam(player, partialTick);
        if (slammed >= 0.0F && slammed < 5.0F) {
            shake = Math.max(shake, 0.6F * (1.0F - slammed / 5.0F));
        }
        shake *= ClientSettings.cameraShake();
        if (shake > 0.0F) {
            float time = player.tickCount + partialTick;
            event.setPitch(event.getPitch() + 1.8F * shake * Mth.sin(time * 2.9F));
            event.setYaw(event.getYaw() + 1.3F * shake * Mth.sin(time * 3.7F + 1.0F));
            event.setRoll(event.getRoll() + 1.5F * shake * Mth.sin(time * 4.3F + 2.0F));
        }
        if (event.getCamera().isDetached() || event.getCamera().getEntity() != player) {
            return;
        }
        if (slammed >= 0.0F && slammed < LandingSlam.IMPACT_TICK
                && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            float dip = (float) (Ease.smooth(slammed / 1.5) * (1.0 - Ease.smooth((slammed - 2.5) / 3.0)));
            float look = SlamPainter.look(ClientConstructs.slamVariant(player.getId()));
            float up = (float) (Ease.smooth((slammed - 3.0) / 3.0)
                    * (1.0 - Ease.smooth((slammed - LandingSlam.HANG_TICKS)
                    / (LandingSlam.IMPACT_TICK - LandingSlam.HANG_TICKS))));
            event.setPitch(event.getPitch() + 20.0F * dip - look * up);
        }
        float t = ClientRing.flight(player, partialTick);
        if (t < GATHER || t > ARISE + 6.0F) {
            return;
        }
        float up = (float) Math.sin(Math.PI * Mth.clamp((t - GATHER) / (ARISE + 6.0F - GATHER), 0.0F, 1.0F));
        event.setPitch(event.getPitch() - 9.0F * up);
    }

    /**
     * You respawned or went to another world, as a new player whose ticks count from zero again: a slam or a jump of the
     * old one must not count for the new one, many ticks later.
     */
    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        slamTick = Integer.MIN_VALUE;
        lastJump = Integer.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        slamTick = Integer.MIN_VALUE;
        lastJump = Integer.MIN_VALUE;
        stop();
        velocity = Vec3.ZERO;
        velocityO = Vec3.ZERO;
        MOTIONS.clear();
        wind = null;
        FlightPose.clear();
        SuitGlow.clear();
    }

    /** The speed that counts as fast, in blocks per tick: about half the top speed. */
    static double fast() {
        return FAST * fullSpeed();
    }
}
