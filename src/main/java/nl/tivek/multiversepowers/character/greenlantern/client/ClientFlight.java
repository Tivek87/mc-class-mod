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

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientFlight extends FlightSteering {
    private static final double FAST = 0.51;

    private static final Map<Integer, Motion> MOTIONS = new HashMap<>();

    private ClientFlight() {
    }

    public static final class Motion {
        public Vec3 velocity = Vec3.ZERO;
        Vec3 velocityO = Vec3.ZERO;
        public float bank;
        float lastYaw = Float.NaN;
        final ArrayDeque<Vec3> trail = new ArrayDeque<>();
        double ground = 99.0;
        public int sinceEnd = Integer.MAX_VALUE;
        boolean flew;
        float braceO;
        float brace;
    }

    public static float brace(Entity player, float partialTick) {
        Motion motion = MOTIONS.get(player.getId());
        return motion == null ? 0.0F : Mth.lerp(partialTick, motion.braceO, motion.brace);
    }

    @Nullable
    public static Motion motion(Entity entity) {
        return MOTIONS.get(entity.getId());
    }

    public static boolean dropping(Entity player) {
        return ClientRing.flight(player, 0.0F) < 0.0F && ClientRing.has(player, RingPayload.DIVE);
    }

    public static Vec3 ownVelocity() {
        return steering ? velocity : Vec3.ZERO;
    }

    public static Vec3 ownVelocity(float partialTick) {
        return steering ? velocityO.lerp(velocity, partialTick) : Vec3.ZERO;
    }

    static Vec3 velocity(Entity flyer, float partialTick) {
        if (flyer == Minecraft.getInstance().player) {
            return ownVelocity(partialTick);
        }
        Motion motion = MOTIONS.get(flyer.getId());
        return motion == null ? Vec3.ZERO : motion.velocityO.lerp(motion.velocity, partialTick);
    }

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

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        velocityO = velocity;
        if (player.onGround() && dropping(player) && slam(player, 0.0F) < 0.0F) {
            slamTick = player.tickCount;
        }
        float slammed = slam(player, 0.0F);
        if (slammed >= 0.0F && slammed < SLAM_ROOT && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            Input input = event.getInput();
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.jumping = false;
            input.shiftKeyDown = true;
            velocity = Vec3.ZERO;
            afterMove = null;
            // Keep sinking, not just zero: a slam starting a moment before touchdown must not hang in the air.
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
        boolean onDive = t >= ARISE && ClientRing.has(player, RingPayload.DIVE)
                && !ClientRing.has(player, RingPayload.DESCENT);
        if (impact != null && t >= ARISE && player.onGround() && !ClientRing.has(player, RingPayload.DESCENT)) {
            if (onDive || impact.length() >= fullSpeed() * SLAM_SPEED && -impact.y >= impact.length() * SLAM_DOWN) {
                slamDown(player);
                return;
            }
            dove = -impact.y > DIVE_LAND && player.getXRot() > DIVE_LOOK;
        }
        if (ClientRing.has(player, RingPayload.DESCENT)) {
            velocity = new Vec3(velocity.x * 0.95, Mth.lerp(0.15, velocity.y, -SINK), velocity.z * 0.95);
        } else if (t < ARISE) {
            velocity = arise(player, t, forward, strafe, up, down);
        } else if (onDive) {
            velocity = velocity.lerp(new Vec3(0.0, -Math.max(fullSpeed(), DIVE_SPEED), 0.0), DIVE_TURN);
        } else {
            gainSpeed(player, forward > 0.01F);
            velocity = steer(player, forward, strafe, up, down, 1.0);
        }
        if (!player.onGround()) {
            airborne = true;
        }
        if (dove) {
            velocity = Vec3.ZERO;
        }
        if (!landing && !onDive && (dove || airborne && t > ARISE + 4.0F && player.onGround() && !up
                && velocity.horizontalDistance() < LAND_SPEED && !ClientRing.has(player, RingPayload.DESCENT))) {
            landing = true;
            CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
            if (flight != null) {
                PacketDistributor.sendToServer(new AbilityActionPayload(flight.slot().ordinal(), true, 0));
            }
        }
        // Never fly into a chunk not yet loaded (it would stop you dead there): slow down before its edge instead.
        velocity = ChunkEdge.cap(player.level(), player.position(), velocity, EDGE_LOOK);
        player.setDeltaMovement(velocity);
        player.resetFallDistance();
        if (wind == null || wind.isStopped()) {
            wind = new WindSound(player);
            minecraft.getSoundManager().play(wind);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (steering && event.getEntity() == Minecraft.getInstance().player) {
            afterMove = event.getEntity().getDeltaMovement();
            stoppedDown = event.getEntity().verticalCollisionBelow;
            stoppedUp = event.getEntity().verticalCollision && !stoppedDown;
        }
    }

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
        Iterator<Map.Entry<Integer, Motion>> all = MOTIONS.entrySet().iterator();
        while (all.hasNext()) {
            Map.Entry<Integer, Motion> entry = all.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entry.getValue().sinceEnd > 40 && entry.getValue().sinceEnd != Integer.MAX_VALUE) {
                all.remove();
            }
        }
    }

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

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (!steering || event.getPlayer() != Minecraft.getInstance().player) {
            return;
        }
        double fast = Mth.clamp(velocity.length() / fullSpeed(), 0.0, 1.0);
        float effect = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F + 0.14F * (float) fast * effect));
    }

    private static float scrapeShake(LocalPlayer player) {
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        double speed = velocity.length();
        if (!steering || shield == null || speed < Flight.SCRAPE_PART * fullSpeed()
                || !ClientRing.has(player, RingPayload.SHIELD)
                || !Flight.scraping(player, shield.value("ramGroundBlocks"))) {
            return 0.0F;
        }
        return (float) (ClientSettings.get(ClientSettings.RAM_GROUND_SHAKE) * 0.55
                * Mth.clamp(speed / fullSpeed(), 0.3, 1.0));
    }

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

    static double fast() {
        return FAST * fullSpeed();
    }
}
