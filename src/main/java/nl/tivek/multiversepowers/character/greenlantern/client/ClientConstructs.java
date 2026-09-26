package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipSnare;
import nl.tivek.multiversepowers.character.greenlantern.client.Flown.Spot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FirePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FireStream;
import nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.RingSight;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.RAM_OWN_AHEAD;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.beamEnd;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.heldFacing;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.on;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.pane;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.ramWay;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.ringHand;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.where;
import static nl.tivek.multiversepowers.character.greenlantern.client.Track.PLANE_KEEP;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientConstructs extends TrackedConstructs {
    private static final Map<Integer, Broken> BROKEN = new HashMap<>();
    private static final Map<Integer, Broken> BROKEN_HANDS = new HashMap<>();
    private static final int BROKEN_TICKS = 42;

    private record Broken(ConstructPayload construct, double clock, int since) {
    }

    private ClientConstructs() {
    }

    public static void update(ConstructPayload payload) {
        if (payload.solid() < 0.0F) {
            Track going = CONSTRUCTS.get(payload.id());
            if (going != null && going.flown != null) {
                going.flown.strikes(going.clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))
                        - 1.0);
                return;
            }
            letGo(CONSTRUCTS.remove(payload.id()));
            return;
        }
        Track track = CONSTRUCTS.get(payload.id());
        if (track == null) {
            track = new Track(payload);
            CONSTRUCTS.put(payload.id(), track);
            BROKEN_HANDS.remove(payload.id());
            fromAirStrike(track, payload);
            if (payload.shape() == ConstructPayload.BOLT) {
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                BOLTS.merge(payload.owner(), clientTicks + partialTick - (double) payload.age(), Math::max);
            }
        } else {
            track.add(payload);
        }
        if (payload.shape() == ConstructPayload.PLANE) {
            PlanePainter.heard(payload);
        }
        if (track.flown != null) {
            track.flown.add(payload,
                    track.clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) - 1.0);
        }
    }

    private static void fromAirStrike(Track track, ConstructPayload payload) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        int owner = payload.owner();
        switch (payload.shape()) {
            case ConstructPayload.PLANE -> LAUNCHES.keySet().removeIf(key -> (int) (key >> 8) == owner);
            case ConstructPayload.MISSILE -> {
                Track plane = planeOf(owner);
                track.flown = new Flown(payload, plane == null ? null : PlanePainter.path(plane.latest));
                if (plane != null) {
                    track.follow(plane, track.flown.fired);
                }
                if (payload.variant() >= AirStrike.JET_MISSILE) {
                    LAUNCHES.put(((long) owner << 8) | payload.variant(), track.flown.fired);
                }
            }
            case ConstructPayload.BULLET -> {
                Track plane = planeOf(owner);
                if (plane != null) {
                    track.start = clientTicks + partialTick - (plane.clock(partialTick) - payload.charge());
                    PlanePainter.fired(plane.latest, payload);
                } else {
                    CONSTRUCTS.remove(payload.id());
                }
            }
            case ConstructPayload.BLAST -> {
                Track missile = null;
                double nearest = 64.0;
                for (Track other : CONSTRUCTS.values()) {
                    Flown flown = other.flown;
                    if (flown != null && !flown.blasted && flown.ends < Double.POSITIVE_INFINITY
                            && other.latest.owner() == owner) {
                        double far = flown.spots.getLast().at().distanceToSqr(payload.center());
                        if (far < nearest) {
                            nearest = far;
                            missile = other;
                        }
                    }
                }
                if (missile != null) {
                    missile.flown.blasted = true;
                    track.start = clientTicks + partialTick - (missile.clock(partialTick) - missile.flown.ends);
                }
            }
            default -> {
            }
        }
    }

    @Nullable
    private static Track planeOf(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.PLANE && track.latest.owner() == owner) {
                return track;
            }
        }
        return null;
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
            track.retime();
            if (track.flown != null && track.flown.ends < Double.POSITIVE_INFINITY) {
                if (track.clock(0.0F) > track.flown.ends + PlanePainter.MISSILE_BREAKS + 1.0
                        || clientTicks - track.lastSeen > PLANE_KEEP) {
                    tracks.remove();
                }
                continue;
            }
            if (track.timedOut()) {
                tracks.remove();
                if (track.latest.shape() != ConstructPayload.PLANE && track.latest.shape() != ConstructPayload.HAND) {
                    letGo(track);
                }
            } else {
                track.advance();
            }
        }
        BROKEN.values().removeIf(broken -> clientTicks - broken.since() > BROKEN_TICKS);
        BROKEN_HANDS.values().removeIf(broken -> clientTicks - broken.since() > HandPainter.breakTicks());
        BOLTS.values().removeIf(shot -> clientTicks - shot > BOLT_MEMORY);
    }

    @SubscribeEvent
    public static void onClientTickDone(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.shape() == ConstructPayload.HAND) {
                held(minecraft, track);
                continue;
            }
            if (now.shape() == ConstructPayload.WHIP_SNARE) {
                snared(minecraft, now);
                continue;
            }
            if (now.shape() != ConstructPayload.BUBBLE || now.variant() == LightBubble.BREAKING) {
                continue;
            }
            Entity caught = minecraft.level.getEntity(LightBubble.caughtId(now.charge()));
            if (caught == null || caught == minecraft.player) {
                continue;
            }
            caught.setPos(now.center().x, now.center().y - caught.getBbHeight() * 0.5, now.center().z);
            caught.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void held(Minecraft minecraft, Track track) {
        ConstructPayload hand = track.latest;
        if (!hand.held() || minecraft.level == null || HandPose.move(hand.variant()) == HandPose.AXE) {
            return;
        }
        Entity caught = minecraft.level.getEntity(LightBubble.caughtId(hand.charge()));
        if (caught == null || caught == minecraft.player) {
            return;
        }
        double scale = Math.max(0.1, hand.size());
        double reach = Math.sqrt(hand.facing().x * hand.facing().x + hand.facing().z * hand.facing().z) / scale;
        Vec3 grip = HandPose.at(hand.variant(), track.clock(1.0F), reach).place(hand.center(), hand.facing(), scale)
                .at(HandPose.GRIP);
        caught.setPos(grip.x, grip.y - caught.getBbHeight() * 0.5, grip.z);
        caught.setDeltaMovement(Vec3.ZERO);
    }

    // A creature the lasso holds or hauls goes where the server says, every tick, or it would jump along.
    private static void snared(Minecraft minecraft, ConstructPayload snare) {
        if (minecraft.level == null || snare.variant() != WhipSnare.BOUND && snare.variant() != WhipSnare.HAULING) {
            return;
        }
        Entity caught = minecraft.level.getEntity(LightBubble.caughtId(snare.charge()));
        if (caught == null || caught == minecraft.player) {
            return;
        }
        caught.setPos(snare.center().x, snare.center().y - caught.getBbHeight() * 0.5, snare.center().z);
        caught.setDeltaMovement(Vec3.ZERO);
    }

    private static void letGo(@Nullable Track track) {
        if (track == null) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double clock = track.clock(partialTick);
        if (track.latest.shape() == ConstructPayload.HAND) {
            if (clock < HandPose.sinks(track.latest.variant()) && clock > HandPose.ARRIVES) {
                BROKEN_HANDS.put(track.latest.id(), new Broken(track.latest, clock, clientTicks));
            }
            return;
        }
        if (track.latest.shape() == ConstructPayload.PLANE && clock < PlanePainter.path(track.latest).crashTick()) {
            BROKEN.put(track.latest.id(), new Broken(track.latest, clock, clientTicks));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        forgetAll();
    }

    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        forgetAll();
    }

    private static void forgetAll() {
        CONSTRUCTS.clear();
        BROKEN.clear();
        BROKEN_HANDS.clear();
        BOLTS.clear();
        LAUNCHES.clear();
        RingSpot.clear();
        PlanePainter.clear();
        Flattened.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Must draw after water and glass, or light would be hidden behind them instead of showing through.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || CONSTRUCTS.isEmpty() && BROKEN.isEmpty() && BROKEN_HANDS.isEmpty()
                && !BeamCharge.any(level) && FireStream.out() && !Jetpacks.any()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload was = track.previous;
            ConstructPayload now = track.current;
            Entity owner = level.getEntity(now.owner());
            if (owner == null && now.held() && now.shape() == ConstructPayload.FIST) {
                continue;
            }
            Vec3 facing = was.facing().lerp(now.facing(), partialTick);
            if (facing.lengthSqr() < 1.0E-6) {
                facing = now.facing();
            }
            Vec3 way = facing.normalize();
            double size = Mth.lerp(partialTick, was.size(), now.size());
            double solid = Mth.lerp(partialTick, was.solid(), now.solid());
            double charge = Mth.lerp(partialTick, was.charge(), now.charge());
            Vec3 center = where(was, now, owner, partialTick);
            boolean onItsWay = track.path != null && !track.latest.held();
            if (onItsWay) {
                on(track, owner, partialTick);
                center = track.lastCenter;
                way = track.lastWay;
            }
            Vec3 ring = owner == null ? null : ringHand(minecraft, camera, owner, partialTick, event);
            boolean own = owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached();
            if (owner != null) {
                switch (now.shape()) {
                    case ConstructPayload.SHIELD -> {
                        way = owner.getViewVector(partialTick);
                        center = pane(owner, partialTick);
                    }
                    case ConstructPayload.RAM -> {
                        way = ramWay(owner, partialTick);
                        center = own ? owner.getEyePosition(partialTick).add(way.scale(RAM_OWN_AHEAD))
                                : owner.getPosition(partialTick).add(0.0, owner.getBbHeight() * 0.5, 0.0);
                    }
                    case ConstructPayload.DOME -> center = owner.getPosition(partialTick)
                            .add(0.0, owner.getBbHeight() * 0.5, 0.0);
                    case ConstructPayload.BEAM -> way = owner.getViewVector(partialTick);
                    case ConstructPayload.FIST -> {
                        if (now.held() && !onItsWay) {
                            way = heldFacing(owner, partialTick);
                        }
                    }
                    default -> {
                    }
                }
            }
            switch (now.shape()) {
                case ConstructPayload.BOLT -> painter.bolt(center, way, size, solid, ring);
                case ConstructPayload.SHIELD -> painter.shield(center, way, size, solid, charge, ring, own);
                case ConstructPayload.DOME -> painter.dome(center, size, solid, charge, own);
                case ConstructPayload.RAM -> painter.ram(center, way, solid, charge, own);
                case ConstructPayload.SLAM -> SlamPainter.draw(painter, track.latest, track.clock(partialTick), ring,
                        owner == null ? null : owner.getPosition(partialTick));
                case ConstructPayload.SCAN -> {
                    RingSight.wave(painter, now.center(), now.size(), track.clock(partialTick));
                    if (ring != null && now.variant() != ConstructPayload.SCAN_HOSTILE) {
                        RingSight.ringLight(painter, ring, track.clock(partialTick), own);
                    }
                }
                case ConstructPayload.HAND -> HandPainter.draw(painter, track.latest, was.facing().lerp(now.facing(),
                        partialTick), track.clock(partialTick), ring);
                case ConstructPayload.BEAM -> {
                    if (ring != null && owner != null) {
                        int stage = Mth.clamp(now.variant(), 0, LightBeam.STAGE_THICK.length - 1);
                        painter.beamOfLight(ring, beamEnd(level, owner, way, now, partialTick), solid,
                                track.clock(partialTick), LightBeam.STAGE_THICK[stage],
                                (double) stage / (LightBeam.STAGE_THICK.length - 1));
                    }
                }
                case ConstructPayload.PLANE -> PlanePainter.draw(painter, now.id(), track.latest,
                        track.clock(partialTick), ring, partialTick);
                case ConstructPayload.MISSILE -> missile(painter, track, partialTick);
                case ConstructPayload.BULLET -> PlanePainter.bullet(painter, track.latest,
                        sinceSent(track, partialTick));
                case ConstructPayload.BLAST -> PlanePainter.missileBlast(painter, track.latest,
                        sinceSent(track, partialTick));
                case ConstructPayload.BUBBLE -> BubblePainter.draw(painter, now, center, solid,
                        was.variant() == LightBubble.BREAKING ? charge : 0.0, now.held(), track.clock(partialTick), ring,
                        now.age() - 1.0 + partialTick - track.variantSince, now.center().subtract(was.center()));
                case ConstructPayload.POUND -> BubblePainter.pound(painter, track.latest, track.clock(partialTick));
                case ConstructPayload.SWORD -> {
                    if (owner != null && !own) {
                        SwordArms.draw(painter, owner, ring, partialTick);
                    }
                }
                case ConstructPayload.FLAME -> {
                    Flame shown = owner == null ? null : flame(owner.getId(), partialTick);
                    if (shown == null || shown.id() != now.id()) {
                        continue;
                    }
                    if (own && owner instanceof LocalPlayer player) {
                        FlameArms.drawOwn(painter, player, camera, event.getProjectionMatrix(),
                                event.getModelViewMatrix(), partialTick);
                    } else if (owner != null) {
                        FlameArms.draw(painter, owner, ring, partialTick);
                    }
                }
                case ConstructPayload.WHIP -> {
                    Whip shown = owner == null ? null : whip(owner.getId(), partialTick);
                    if (shown == null || shown.id() != now.id()) {
                        continue;
                    }
                    if (own && owner instanceof LocalPlayer player) {
                        WhipArms.drawOwn(painter, player, camera, event.getProjectionMatrix(),
                                event.getModelViewMatrix(), partialTick);
                    } else if (owner != null) {
                        WhipArms.draw(painter, owner, ring, partialTick);
                    }
                }
                case ConstructPayload.WHIP_SNARE -> {
                }
                case ConstructPayload.FLAME_WALL -> FirePainter.wall(painter, level, now.center(), way, size, charge,
                        track.clock(partialTick), solid);
                case ConstructPayload.BURN -> {
                    if (owner != null && !own) {
                        FirePainter.burn(painter, owner.getPosition(partialTick), size, charge, solid, now.id());
                    }
                }
                default -> painter.fist(center, way, size, solid, charge, now.held() && !onItsWay, ring);
            }
        }
        for (Broken broken : BROKEN.values()) {
            double since = clientTicks - broken.since() + partialTick;
            PlanePainter.broken(painter, broken.construct(), broken.clock() + since, since);
        }
        for (Broken broken : BROKEN_HANDS.values()) {
            HandPainter.broken(painter, broken.construct(), broken.clock(),
                    clientTicks - broken.since() + partialTick);
        }
        for (AbstractClientPlayer player : level.players()) {
            if (BeamCharge.charge(player, partialTick) >= 0.0F) {
                BeamCharge.draw(painter, player, ringHand(minecraft, camera, player, partialTick, event), camera,
                        partialTick);
            }
            if (Jetpacks.has(player)) {
                boolean own = player == minecraft.player && camera.getEntity() == player && !camera.isDetached();
                Jetpacks.draw(painter, player, ringHand(minecraft, camera, player, partialTick, event), own,
                        partialTick);
            }
        }
        FireStream.draw(painter, FireStream.now(partialTick));
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    private static void missile(LanternPainter painter, Track track, float partialTick) {
        Flown flown = track.flown;
        if (flown == null || flown.spots.isEmpty()) {
            return;
        }
        double since = track.clock(partialTick);
        if (since <= flown.leaves() && planeOf(track.latest.owner()) != null) {
            return;
        }
        if (since >= flown.ends) {
            Spot last = flown.spots.getLast();
            PlanePainter.missile(painter, flown.small, last.at().add(flown.off(flown.ends - 1.0)), last.nose(),
                    last.up(), -1.0, since - flown.ends);
            return;
        }
        // Drawn a tick after it was sent, so it leaves the plane at the spot it hung a tick ago.
        Spot spot = flown.at(since - 1.0);
        PlanePainter.missile(painter, flown.small, spot.at().add(flown.off(since - 1.0)), spot.nose(), spot.up(),
                since - flown.ignites, -1.0);
    }
}
