package nl.tivek.welcomescreen.client.character.lantern;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.lantern.ConstructPath;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.character.lantern.LightShield;
import nl.tivek.welcomescreen.network.ConstructPayload;
import org.joml.Vector3f;

/**
 * Keeps the hard-light constructs the server sends and draws them (see ConstructPainter), blended between
 * ticks so they fly smoothly. Updates are played back one per client tick, so updates that arrive
 * unevenly over the network still move evenly.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientConstructs {
    // A construct the server stopped updating (out of range, or a lost packet) disappears.
    private static final int TIMEOUT = 10;
    // Updates waiting beyond this many are skipped, so a network hiccup never leaves one lagging behind.
    private static final int MAX_WAITING = 2;
    // How long the shockwave of a slam shakes the view, in ticks.
    private static final double SHAKE_TICKS = 8.0;

    // The beam starts on the line from your eye through your own hand, but this much nearer than the hand
    // itself: on screen that is the same spot, and it keeps the beam from starting inside a wall.
    private static final double RING_NEAR = 0.45;

    private static final Map<Integer, Track> CONSTRUCTS = new HashMap<>();
    private static int clientTicks;

    private ClientConstructs() {
    }

    /**
     * The server updates of one construct, played back one per client tick. A construct that moves by itself (a
     * fist or bolt on its way, a landing slam) also runs on a clock of its own: the server says how long ago it
     * set off, and from then on the client counts on by itself, so it moves at an even pace however unevenly the
     * updates come in.
     */
    private static final class Track {
        private final ArrayDeque<ConstructPayload> waiting = new ArrayDeque<>();
        private ConstructPayload previous;
        private ConstructPayload current;
        // The newest update, the moment it arrives.
        private ConstructPayload latest;
        private int lastSeen;
        // The client time it set off, the most the server told it has aged, and the way it flies (null: none).
        private double start = Double.NaN;
        private int told = -1;
        @Nullable
        private ConstructPath path;

        Track(ConstructPayload first) {
            this.previous = first;
            this.current = first;
            this.latest = first;
            this.lastSeen = clientTicks;
            this.time(first);
        }

        void add(ConstructPayload update) {
            this.waiting.add(update);
            this.latest = update;
            this.lastSeen = clientTicks;
            this.time(update);
        }

        private void time(ConstructPayload update) {
            if (update.path() == null && update.shape() != ConstructPayload.SLAM) {
                return;
            }
            if (update.path() != null) {
                this.path = update.path();
            }
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            double setOff = clientTicks + partialTick - update.age();
            // The update that came through quickest tells best when it really set off.
            this.start = Double.isNaN(this.start) ? setOff : Math.min(this.start, setOff);
            this.told = Math.max(this.told, update.age());
        }

        /** Ticks since it set off by the client's own clock: smooth, and never far ahead of what the server told. */
        double clock(float partialTick) {
            if (Double.isNaN(this.start)) {
                return Math.max(0, this.told);
            }
            return Mth.clamp(clientTicks + partialTick - this.start, 0.0, this.told + 1.5);
        }

        void advance() {
            this.previous = this.current;
            while (this.waiting.size() > MAX_WAITING) {
                this.waiting.poll();
            }
            if (!this.waiting.isEmpty()) {
                this.current = this.waiting.poll();
            }
        }

        boolean timedOut() {
            return clientTicks - this.lastSeen > TIMEOUT;
        }
    }

    /**
     * A construct someone is holding beside them: where it hangs, how far it has come in, and which shape it
     * is (the hand that holds it depends on that: the ring hand attacks, the other one defends).
     */
    public record Held(Vec3 center, float strength, int shape) {
        /** True while this is something the hand that defends holds up, not something the ring hand shapes. */
        public boolean defends() {
            return this.shape == ConstructPayload.SHIELD;
        }
    }

    /**
     * The construct this player holds out with one hand, or null when they hold none: a fist they charge, or the
     * shield in front of them. (The dome, the ram cone and the beam are posed with the flight, see FlightPose.)
     */
    @Nullable
    public static Held heldBy(int owner) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(owner);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.owner() != owner || !now.held()) {
                continue;
            }
            if (now.shape() == ConstructPayload.FIST) {
                Vec3 center = entity == null ? now.center() : hung(entity, now.center(), partialTick);
                return new Held(center, now.solid(), now.shape());
            }
            if (now.shape() == ConstructPayload.SHIELD) {
                Vec3 center = entity == null ? now.center() : pane(entity, partialTick);
                return new Held(center, now.solid(), now.shape());
            }
        }
        return null;
    }

    /**
     * How hard the constructs of this player make the ring work right now, for the glow of the uniform: a fist
     * that charges (more as it grows), a fist or a bolt on its way.
     */
    static float working(int owner) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.owner() != owner || now.solid() <= 0.0F) {
                continue;
            }
            float here = switch (now.shape()) {
                case ConstructPayload.FIST -> now.held() ? 0.6F + 0.4F * now.charge() : 0.5F;
                case ConstructPayload.BOLT -> 0.7F;
                default -> 0.0F;
            };
            most = Math.max(most, here * now.solid());
        }
        return most;
    }

    /** How many ticks ago this player's landing slam began, by the client's own clock, or -1 when there is none. */
    static float slamAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    /**
     * How hard the shockwave of a slam nearby shakes a view from {@code from}: 1 right next to it as it strikes,
     * fading with distance and over the next few ticks, 0 when there is none.
     */
    static float shake(Vec3 from, float partialTick) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload slam = track.latest;
            if (slam.shape() != ConstructPayload.SLAM) {
                continue;
            }
            double since = track.clock(partialTick) - LandingSlam.IMPACT_TICK;
            double near = 1.0 - from.distanceTo(slam.center()) / (slam.size() * 3.0 + 4.0);
            if (since < 0.0 || since >= SHAKE_TICKS || near <= 0.0) {
                continue;
            }
            double fade = 1.0 - since / SHAKE_TICKS;
            most = Math.max(most, (float) (fade * fade * Math.min(1.0, near * 1.5)));
        }
        return most;
    }

    /**
     * Where a held fist hangs: the spot around its owner's eyes the server gave (x to his right, y up, z ahead),
     * turned with where he faces right now, so it keeps up with him however fast he turns or flies.
     */
    private static Vec3 hung(Entity owner, Vec3 spot, float partialTick) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, owner.getViewYRot(partialTick));
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return owner.getEyePosition(partialTick).add(right.scale(spot.x)).add(0.0, spot.y, 0.0)
                .add(ahead.scale(spot.z));
    }

    /** Where the shield stands: in front of its owner's eyes, in the way he looks right now. */
    private static Vec3 pane(Entity owner, float partialTick) {
        return owner.getEyePosition(partialTick).add(owner.getViewVector(partialTick).scale(LightShield.AHEAD));
    }

    public static void update(ConstructPayload payload) {
        if (payload.solid() < 0.0F) {
            CONSTRUCTS.remove(payload.id());
            return;
        }
        Track track = CONSTRUCTS.get(payload.id());
        if (track == null) {
            CONSTRUCTS.put(payload.id(), new Track(payload));
        } else {
            track.add(payload);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        Iterator<Track> tracks = CONSTRUCTS.values().iterator();
        while (tracks.hasNext()) {
            Track track = tracks.next();
            if (track.timedOut()) {
                tracks.remove();
            } else {
                track.advance();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CONSTRUCTS.clear();
        RingSpot.clear();
    }

    // After water and glass: light never hides what is behind it, so it has to come after them.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || CONSTRUCTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        // The same blend between ticks that entities are drawn with.
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        Camera camera = event.getCamera();
        ConstructPainter painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(), time);
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload was = track.previous;
            ConstructPayload now = track.current;
            Entity owner = level.getEntity(now.owner());
            Vec3 facing = was.facing().lerp(now.facing(), partialTick);
            if (facing.lengthSqr() < 1.0E-6) {
                facing = now.facing();
            }
            Vec3 way = facing.normalize();
            double size = Mth.lerp(partialTick, was.size(), now.size());
            double solid = Mth.lerp(partialTick, was.solid(), now.solid());
            double charge = Mth.lerp(partialTick, was.charge(), now.charge());
            Vec3 center = where(was, now, owner, partialTick);
            // A fist or bolt on its way glides along its path by the client's own clock. Once it stops (it hit
            // something, or falls apart at the end of its way) it stays where the server says it stopped.
            boolean onItsWay = track.path != null && !track.latest.held();
            if (onItsWay) {
                boolean moving = track.latest.path() != null;
                double travelled = track.path.travelled(moving ? track.clock(partialTick) : track.told);
                center = moving ? track.path.along(travelled) : track.latest.center();
                way = track.path.way(travelled);
            }
            Vec3 ring = owner == null ? null : ringHand(minecraft, camera, owner, partialTick, event);
            boolean own = owner == minecraft.player && !camera.isDetached();
            // What hangs on its owner is worked out here from how he stands right now, so it moves with him
            // without dragging a tick behind, however fast he turns or flies.
            if (owner != null) {
                switch (now.shape()) {
                    case ConstructPayload.SHIELD -> {
                        way = owner.getViewVector(partialTick);
                        center = pane(owner, partialTick);
                    }
                    case ConstructPayload.RAM, ConstructPayload.DOME -> center = owner.getPosition(partialTick)
                            .add(0.0, owner.getBbHeight() * 0.5, 0.0);
                    case ConstructPayload.BEAM -> way = owner.getViewVector(partialTick);
                    case ConstructPayload.FIST -> {
                        if (now.held() && !onItsWay) {
                            way = heldFacing(owner, partialTick);
                        }
                    }
                    default -> {
                        // A bolt flies on by itself.
                    }
                }
            }
            switch (now.shape()) {
                case ConstructPayload.BOLT -> painter.bolt(center, way, size, solid, ring);
                case ConstructPayload.SHIELD -> painter.shield(center, way, size, solid, charge, ring, own);
                case ConstructPayload.DOME -> painter.dome(center, size, solid, charge, own);
                case ConstructPayload.RAM -> painter.ram(center, ramWay(owner, way), solid, charge, own);
                case ConstructPayload.SLAM -> SlamPainter.draw(painter, track.latest, track.clock(partialTick), ring);
                case ConstructPayload.BEAM -> {
                    if (ring != null && owner != null) {
                        painter.beamOfLight(ring, beamEnd(level, owner, way, now, partialTick), solid);
                    }
                }
                default -> painter.fist(center, way, size, solid, charge, now.held() && !onItsWay, ring);
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /**
     * Where a construct is between two updates: a held fist hangs on its owner (see {@link #hung}), anything else
     * is blended from one update to the next. (A fist or bolt on its way follows its path instead.)
     */
    private static Vec3 where(ConstructPayload was, ConstructPayload now, @Nullable Entity owner, float partialTick) {
        if (now.held() && now.shape() == ConstructPayload.FIST) {
            Vec3 spot = was.held() ? was.center().lerp(now.center(), partialTick) : now.center();
            return owner == null ? now.center() : hung(owner, spot, partialTick);
        }
        return was.center().lerp(now.center(), partialTick);
    }

    /** The way a held fist points: where its owner looks, but tipped up or down no further than the server lets it. */
    private static Vec3 heldFacing(Entity owner, float partialTick) {
        return Vec3.directionFromRotation(Mth.clamp(owner.getViewXRot(partialTick), -25.0F, 25.0F),
                owner.getViewYRot(partialTick));
    }

    /** The way the ram cone points: the way its owner flies, or the way he looks while he hardly moves. */
    private static Vec3 ramWay(@Nullable Entity owner, Vec3 fallback) {
        ClientFlight.Motion motion = owner == null ? null : ClientFlight.motion(owner);
        if (motion == null) {
            return fallback;
        }
        Vec3 moving = owner == Minecraft.getInstance().player ? ClientFlight.ownVelocity() : motion.velocity;
        return moving.lengthSqr() > 0.04 ? moving.normalize() : fallback;
    }

    /**
     * Where the beam stops: at the first wall along the crosshair. For your own beam that is worked out here, so
     * it sits exactly on what you aim at; anyone else's beam is as long as the server says.
     */
    private static Vec3 beamEnd(ClientLevel level, Entity owner, Vec3 way, ConstructPayload now, float partialTick) {
        Vec3 eye = owner.getEyePosition(partialTick);
        if (owner == Minecraft.getInstance().player) {
            CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
            double range = bolt == null ? 40.0 : bolt.value("beamRangeBlocks");
            Vec3 far = eye.add(way.scale(range));
            BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            return hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
        }
        return eye.add(way.scale(Math.max(0.5, now.size())));
    }

    /**
     * Where the ring is: measured where it was really drawn (see {@link RingSpot}), so the light leaves the stone
     * itself; worked out from the body when it was not drawn lately.
     */
    private static Vec3 ringHand(Minecraft minecraft, Camera camera, Entity owner, float partialTick,
            RenderLevelStageEvent event) {
        Vec3 seen = RingSpot.of(owner, camera, event.getProjectionMatrix(), event.getModelViewMatrix());
        if (seen != null) {
            return seen;
        }
        if (owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached()) {
            // Your own ring in first person: straight at the ring on the hand the game draws low on the right
            // of your screen, but close to the camera, so the beam leaves the stone itself.
            Vector3f hand = LanternArms.handPoint(minecraft.player, partialTick);
            Vec3 forward = new Vec3(camera.getLookVector());
            Vec3 up = new Vec3(camera.getUpVector());
            Vec3 left = new Vec3(camera.getLeftVector());
            return camera.getPosition().add(forward.scale(-hand.z() * RING_NEAR))
                    .subtract(left.scale(hand.x() * RING_NEAR)).add(up.scale(hand.y() * RING_NEAR));
        }
        // Seen from outside: the end of the right arm, exactly where the body draws it.
        if (owner instanceof LivingEntity living) {
            return LanternArms.ringPoint(living, partialTick);
        }
        double yaw = Math.toRadians(owner.getViewYRot(partialTick));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.72, 0).add(forward.scale(0.3));
    }
}
