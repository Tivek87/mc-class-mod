package nl.tivek.welcomescreen.client.character.lantern;

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
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.lantern.Flight;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.network.AbilityActionPayload;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * Flying as Green Lantern, on this side. Every game moves its own player, so your own flight is steered here:
 * <ul>
 * <li><b>Taking off</b> (see {@link Flight#ARISE_TICKS}): you stop where you are while your fists come to your
 * chest, then your arms sweep down along your sides and you rise a few blocks, looking up, and from there you
 * fly on without a break.</li>
 * <li><b>Flying</b>: hold forward and you pick up speed the way you look, up to half again as fast as an elytra
 * with rockets; let go and you glide to a hover. Jump and sneak rise and sink, left and right slide sideways.
 * You carry your speed into every turn, so you swing through curves instead of snapping round.</li>
 * <li>The dome works as a brake chute: while it is up your speed is halved.</li>
 * <li>Walls and the ground stop you; knocks from a hit or a blast move you as they would anyone. Sink down onto
 * the ground slowly and you land by yourself.</li>
 * <li>An empty ring lets you sink down gently, with no more steering, until you touch ground.</li>
 * </ul>
 * For everyone who flies, you included, this also keeps how fast they go and how they bank for their poses
 * (see {@link FlightPose}), draws the streak of light behind them at speed, throws up dust and spray where they
 * skim the ground or water, and plays the wind.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientFlight {
    /** The take-off, in ticks: until here the fists come to the chest, then the arms sweep down and you rise. */
    static final float GATHER = 7.0F;
    /** Until here the arms go down along the sides; the rise goes on to the end of the take-off. */
    static final float SWEEP = 13.0F;
    /** The end of the take-off. */
    static final float ARISE = Flight.ARISE_TICKS;

    // Your own steering, in blocks per tick: how fast you slide and rise while hovering, how quickly you pick
    // up speed and lose it again, and how hard the brake bites.
    private static final double HOVER = 0.32;
    private static final double CLIMB = 0.38;
    private static final double SPEED_UP = 0.085;
    private static final double SLOW_DOWN = 0.055;
    private static final double BRAKE = 0.13;
    // How fast you rise at the height of the take-off, and how fast an empty ring lets you sink.
    private static final double RISE = 0.46;
    private static final double SINK = 0.18;
    // You land by yourself when you touch ground slower than this.
    private static final double LAND_SPEED = 0.35;
    // Speed that counts as fast: the streak of light, the dust and the wind start around here.
    private static final double FAST = 0.9;
    // How many ticks of the path behind a flyer the streak of light shows.
    private static final int TRAIL = 12;
    // How far below a flyer the ground still throws up dust, in blocks.
    private static final double SKIM = 3.0;

    // ---- Your own flight ----
    private static boolean steering;
    private static Vec3 velocity = Vec3.ZERO;
    // What the game made of the speed you gave it, right after you moved: a wall stops one part of it, and any
    // change after that and before the next tick is a knock from outside.
    @Nullable
    private static Vec3 afterMove;
    private static boolean airborne;
    private static boolean landing;
    @Nullable
    private static WindSound wind;

    // ---- Everyone who flies, as seen ----
    private static final Map<Integer, Motion> MOTIONS = new HashMap<>();

    private ClientFlight() {
    }

    /** How one flyer moves, smoothed out, for the poses and the light around them. */
    static final class Motion {
        /** How fast and which way, in blocks per tick. */
        Vec3 velocity = Vec3.ZERO;
        /** Lean into a turn, in radians: positive leans to the right. */
        float bank;
        /** The way the body faced last tick, in degrees, to tell how fast it turns. */
        float lastYaw = Float.NaN;
        /** Where the middle of the body was on the last ticks, newest first: the streak of light. */
        final ArrayDeque<Vec3> trail = new ArrayDeque<>();
        /** How far the ground is below, in blocks (up to {@link #SKIM} and a bit). */
        double ground = 99.0;
        /** Ticks since the flight ended, for the landing; very large while the flight goes on. */
        int sinceEnd = Integer.MAX_VALUE;
        boolean flew;
    }

    /** How a flyer moves right now, or null for someone who does not fly (and has not just landed). */
    @Nullable
    static Motion motion(Entity entity) {
        return MOTIONS.get(entity.getId());
    }

    /** Your own speed in blocks per tick while you steer yourself through the air; zero otherwise. */
    static Vec3 ownVelocity() {
        return steering ? velocity : Vec3.ZERO;
    }

    /** Your own top speed right now, in blocks per tick: halved while the dome brakes you. */
    private static double topSpeed(LocalPlayer player) {
        CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
        double top = flight == null ? 2.5 : flight.value("topSpeed") / 20.0;
        return ClientRing.has(player, RingPayload.DOME) ? top * 0.5 : top;
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
        }
        absorb(player);
        if (ClientRing.has(player, RingPayload.DESCENT)) {
            velocity = new Vec3(velocity.x * 0.95, Mth.lerp(0.15, velocity.y, -SINK), velocity.z * 0.95);
        } else if (t < ARISE) {
            velocity = arise(player, t, forward, strafe, up, down);
        } else {
            velocity = steer(player, forward, strafe, up, down, 1.0);
        }
        if (!player.onGround()) {
            airborne = true;
        }
        // Sinking down onto the ground slowly after the take-off: you land by yourself.
        if (!landing && airborne && t > ARISE + 4.0F && player.onGround() && !up
                && velocity.horizontalDistance() < LAND_SPEED && !ClientRing.has(player, RingPayload.DESCENT)) {
            landing = true;
            CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
            if (flight != null) {
                PacketDistributor.sendToServer(new AbilityActionPayload(flight.slot().ordinal(), true, 0));
            }
        }
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
        }
    }

    /**
     * Since the last tick: a wall or the ground that stopped part of your speed takes that part away, and a knock
     * from outside (a hit, a blast) is added to it, as it would be for anyone.
     */
    private static void absorb(LocalPlayer player) {
        Vec3 before = afterMove;
        afterMove = null;
        if (before == null) {
            return;
        }
        Vec3 now = player.getDeltaMovement();
        Vec3 knock = now.subtract(before);
        double x = before.x == 0.0 && Math.abs(velocity.x) > 1.0E-3 ? 0.0 : velocity.x;
        double y = before.y == 0.0 && Math.abs(velocity.y) > 1.0E-3 ? 0.0 : velocity.y;
        double z = before.z == 0.0 && Math.abs(velocity.z) > 1.0E-3 ? 0.0 : velocity.z;
        velocity = new Vec3(x, y, z);
        if (knock.lengthSqr() > 1.0E-4) {
            velocity = velocity.add(knock);
        }
    }

    /**
     * The take-off: first you stop where you are (even in the middle of a fall) while your fists come to your
     * chest; then you shoot up and ease off at the top, and your own steering fades in towards the end, so the
     * rise flows straight into the flight.
     */
    private static Vec3 arise(LocalPlayer player, float t, float forward, float strafe, boolean up, boolean down) {
        if (t < GATHER) {
            return velocity.scale(0.55);
        }
        double x = (t - GATHER) / (ARISE - GATHER);
        double lift = RISE * (x < 0.15 ? x / 0.15 : Math.pow(1.0 - (x - 0.15) / 0.85, 1.2));
        double control = smooth((t - SWEEP) / (ARISE - SWEEP));
        Vec3 steered = control > 0.0 ? steer(player, forward, strafe, up, down, control) : Vec3.ZERO;
        return new Vec3(steered.x * control, Math.max(lift, steered.y * control), steered.z * control);
    }

    /**
     * Flying: forward picks up speed the way you look; without it you hover, sliding and rising with the other
     * keys. Your speed swings round towards where you want to go instead of jumping there.
     *
     * @param grip how much of the steering you have yet, 0 to 1 (it fades in at the end of the take-off)
     */
    private static Vec3 steer(LocalPlayer player, float forward, float strafe, boolean up, boolean down,
            double grip) {
        double top = topSpeed(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double vertical = (up ? 1.0 : 0.0) - (down ? 1.0 : 0.0);
        Vec3 target;
        if (forward > 0.01F) {
            target = look.scale(top * forward).add(right.scale(-strafe * top * 0.25))
                    .add(0.0, vertical * top * 0.25, 0.0);
        } else {
            target = right.scale(-strafe * HOVER).add(flat.scale(forward * HOVER * 0.8))
                    .add(0.0, vertical * CLIMB, 0.0);
        }
        double rate = target.lengthSqr() > velocity.lengthSqr() ? SPEED_UP : SLOW_DOWN;
        if (ClientRing.has(player, RingPayload.DOME) && velocity.length() > top) {
            rate = BRAKE;
        }
        return velocity.lerp(target, rate * grip);
    }

    /** Your flight is over (you landed, turned it off, or are no longer Green Lantern): the game has you again. */
    private static void stop() {
        steering = false;
        afterMove = null;
        landing = false;
    }

    // ---- Everyone who flies, as seen ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : level.players()) {
            boolean flying = ClientRing.flight(player, 0.0F) >= 0.0F;
            Motion motion = MOTIONS.get(player.getId());
            if (motion == null) {
                if (!flying) {
                    continue;
                }
                motion = new Motion();
                MOTIONS.put(player.getId(), motion);
            }
            track(level, player, motion, flying, player == minecraft.player && steering);
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

    /** One tick of how a flyer moves: speed, lean, the streak behind them and what they throw up below. */
    private static void track(ClientLevel level, AbstractClientPlayer player, Motion motion, boolean flying,
            boolean own) {
        Vec3 moved = own ? velocity : new Vec3(player.getX() - player.xo, player.getY() - player.yo,
                player.getZ() - player.zo);
        motion.velocity = motion.velocity.lerp(moved, own ? 0.6 : 0.35);
        float yaw = player.getYRot();
        float turn = Float.isNaN(motion.lastYaw) ? 0.0F : Mth.wrapDegrees(yaw - motion.lastYaw);
        motion.lastYaw = yaw;
        double speed = motion.velocity.length();
        // Turning at speed leans you into the curve, the way a bird banks.
        float lean = flying ? Mth.clamp(turn * 0.045F * (float) Math.min(1.0, speed / FAST), -0.75F, 0.75F) : 0.0F;
        motion.bank = Mth.lerp(0.18F, motion.bank, lean);
        if (flying) {
            motion.flew = true;
            motion.sinceEnd = Integer.MAX_VALUE;
        } else if (motion.sinceEnd == Integer.MAX_VALUE) {
            motion.sinceEnd = 0;
        } else {
            motion.sinceEnd++;
        }
        Vec3 middle = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        motion.trail.addFirst(middle);
        while (motion.trail.size() > TRAIL) {
            motion.trail.removeLast();
        }
        motion.ground = groundBelow(level, player);
        if (flying && speed > FAST * 0.6) {
            skim(level, player, motion, speed);
        }
    }

    /** How far down the ground (or water) is below a flyer's feet, up to a little past {@link #SKIM}. */
    private static double groundBelow(ClientLevel level, Entity player) {
        Vec3 feet = player.position();
        BlockHitResult hit = level.clip(new ClipContext(feet, feet.add(0.0, -SKIM - 1.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? SKIM + 1.0 : feet.y - hit.getLocation().y;
    }

    /** Flying fast low over the ground throws up dust of what lies there; over water it throws up spray. */
    private static void skim(ClientLevel level, Entity player, Motion motion, double speed) {
        if (motion.ground > SKIM) {
            return;
        }
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - motion.ground - 0.2, player.getZ());
        BlockState state = level.getBlockState(below);
        double strength = (1.0 - motion.ground / SKIM) * Math.min(1.0, speed / 2.0);
        int count = (int) (1 + strength * 5);
        double y = below.getY() + 1.05;
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (level.random.nextDouble() - 0.5) * 1.6;
            double z = player.getZ() + (level.random.nextDouble() - 0.5) * 1.6;
            double out = 0.15 + 0.2 * strength;
            if (!state.getFluidState().isEmpty()) {
                level.addParticle(ParticleTypes.SPLASH, x, y, z, (level.random.nextDouble() - 0.5) * out, 0.25,
                        (level.random.nextDouble() - 0.5) * out);
                level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.05, 0.0);
            } else if (state.getRenderShape() != RenderShape.INVISIBLE && !state.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z,
                        -motion.velocity.x * 0.3, 0.15 + 0.2 * strength, -motion.velocity.z * 0.3);
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
        ConstructPainter painter = null;
        for (Map.Entry<Integer, Motion> entry : MOTIONS.entrySet()) {
            Motion motion = entry.getValue();
            Entity entity = level.getEntity(entry.getKey());
            double speed = motion.velocity.length();
            if (entity == null || entity.isInvisible() || motion.trail.size() < 3 || speed < FAST * 0.5) {
                continue;
            }
            if (painter == null) {
                painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
                        (float) (level.getGameTime() % 24000L) + partialTick);
            }
            Vec3 head = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
            painter.trail(head, motion.trail, Mth.clamp((speed - FAST * 0.5) / FAST, 0.0, 1.0),
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
        double fast = Mth.clamp(velocity.length() / 2.5, 0.0, 1.0);
        float effect = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.14F * (float) fast * effect));
    }

    /** As you rise off the ground in first person, your view tips up a little with the head of the body. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getCamera().isDetached() || event.getCamera().getEntity() != player) {
            return;
        }
        float t = ClientRing.flight(player, (float) event.getPartialTick());
        if (t < GATHER || t > ARISE + 6.0F) {
            return;
        }
        float up = (float) Math.sin(Math.PI * Mth.clamp((t - GATHER) / (ARISE + 6.0F - GATHER), 0.0F, 1.0F));
        event.setPitch(event.getPitch() - 9.0F * up);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
        velocity = Vec3.ZERO;
        MOTIONS.clear();
        wind = null;
        FlightPose.clear();
        SuitGlow.clear();
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** The wind of your own flight: louder and higher the faster you go, gone once you land. */
    private static final class WindSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        WindSound(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void tick() {
            if (this.player.isRemoved() || !steering) {
                this.stop();
                return;
            }
            this.x = this.player.getX();
            this.y = this.player.getY();
            this.z = this.player.getZ();
            double speed = velocity.length();
            this.volume = (float) Mth.clamp((speed - 0.3) / 2.2, 0.0, 0.85);
            this.pitch = 0.9F + (float) Math.min(0.5, speed * 0.18);
        }
    }
}
