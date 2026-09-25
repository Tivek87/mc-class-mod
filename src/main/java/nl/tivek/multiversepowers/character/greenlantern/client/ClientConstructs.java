package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
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
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.client.Flown.Spot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
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

/**
 * Keeps the hard-light constructs the server sends and draws them (see ConstructPainter), blended between
 * ticks so they fly smoothly. Updates are played back one per client tick, so updates that arrive
 * unevenly over the network still move evenly. Each construct is kept as a {@link Track}; what the rest of the
 * client asks about them is answered in {@link TrackedConstructs}.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientConstructs extends TrackedConstructs {
    // Planes whose maker let go of them in the air: they break up where they were. By id: the last word about it, the
    // clock it had then, and the client tick it was let go on.
    private static final Map<Integer, Broken> BROKEN = new HashMap<>();
    // Giant hands whose maker let go of them before they were done: they break up where they were. By id, as above.
    private static final Map<Integer, Broken> BROKEN_HANDS = new HashMap<>();
    // How long a plane that was let go takes to break up and be gone, in ticks.
    private static final int BROKEN_TICKS = 42;

    /** A plane or giant hand let go of before it was done, breaking up. */
    private record Broken(ConstructPayload construct, double clock, int since) {
    }

    private ClientConstructs() {
    }

    public static void update(ConstructPayload payload) {
        if (payload.solid() < 0.0F) {
            Track going = CONSTRUCTS.get(payload.id());
            if (going != null && going.flown != null) {
                // A missile that struck (or was let go of) is seen to get there first, then breaks up (see missile).
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
            // Heard of again: whatever of it was breaking up is whole after all.
            BROKEN_HANDS.remove(payload.id());
            fromAirStrike(track, payload);
            if (payload.shape() == ConstructPayload.BOLT) {
                // Seen first on its way (it came into range late), it still left the ring as long ago as it has flown.
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                BOLTS.merge(payload.owner(), clientTicks + partialTick - (double) payload.age(), Math::max);
            }
        } else {
            track.add(payload);
        }
        if (track.flown != null) {
            track.flown.add(payload,
                    track.clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) - 1.0);
        }
    }

    /**
     * Something new of an air strike: it keeps the time of its plane. A missile runs on the plane's clock (see
     * Track#follow) and a round flies out as its gun fired it by that clock, so they leave the plane just where it is
     * drawn; the gun swings to where the round says it is to point next; a missile's blast bursts as the missile gets
     * there on your screen; and a new plane forgets when the last one's jets fired.
     */
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
                    // A round of a plane this client does not see: there is no gun for it to fly out of.
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
                // Nothing of an air strike.
            }
        }
    }

    /** The track of this player's air strike's plane, or null when this client has none. */
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
                // A missile that struck is gone once it has got there and broken up.
                if (track.clock(0.0F) > track.flown.ends + PlanePainter.MISSILE_BREAKS + 1.0
                        || clientTicks - track.lastSeen > PLANE_KEEP) {
                    tracks.remove();
                }
                continue;
            }
            if (track.timedOut()) {
                tracks.remove();
                // A plane or giant hand not heard of a while is out of reach or the server hitches: it did not break up
                // (the server says so when it does).
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

    /**
     * A creature caught in a Light Bubble stays right in its middle on your screen too, and one a giant hand holds right
     * in its fist. The game itself only tells where it is every few ticks and glides it there, well behind a bubble that
     * is smashed down or a hand that throws.
     */
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

    /**
     * What a giant hand holds sits in its fist, where the hand is by the client's own clock. A pair of hands with an
     * axe never holds anything.
     */
    private static void held(Minecraft minecraft, Track track) {
        ConstructPayload hand = track.latest;
        if (!hand.held() || minecraft.level == null || HandPose.move(hand.variant()) == HandPose.AXE) {
            return;
        }
        // The id comes whole in the charge's bits (see LightBubble#caught).
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

    /**
     * A construct is gone. A plane still in the air when it goes was let go of by its maker, and so was a giant hand
     * that had not yet sunk back into the ground, or a pair of them whose axe had not yet broken up: they break into
     * solid pieces where they were instead of simply vanishing, as every construct does.
     */
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

    /**
     * Into another dimension, or back to life: what was drawn where you were is gone (the server tells again of what
     * is still round you).
     */
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

    // After water and glass: light never hides what is behind it, so it has to come after them.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || CONSTRUCTS.isEmpty() && BROKEN.isEmpty() && BROKEN_HANDS.isEmpty()
                && !BeamCharge.any(level)) {
            return;
        }
        // The same blend between ticks that entities are drawn with.
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload was = track.previous;
            ConstructPayload now = track.current;
            Entity owner = level.getEntity(now.owner());
            // A fist held beside its owner is placed round him: with him not here (too far to be seen) there is
            // nowhere to put it.
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
            // A fist or bolt on its way glides along its path by the client's own clock; a fist stays on its owner's
            // line of sight as he looks right now, so your own is always right under your crosshair. Once it stops
            // (it hit something, or falls apart at the end of its way) a bolt stays where the server says it stopped,
            // and a fist where it was drawn last.
            boolean onItsWay = track.path != null && !track.latest.held();
            if (onItsWay) {
                on(track, owner, partialTick);
                center = track.lastCenter;
                way = track.lastWay;
            }
            Vec3 ring = owner == null ? null : ringHand(minecraft, camera, owner, partialTick, event);
            // Seen from your own eyes: not from behind, and not while the camera looks out of something else.
            boolean own = owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached();
            // What hangs on its owner is worked out here from how he stands right now, so it moves with him
            // without dragging a tick behind, however fast he turns or flies.
            if (owner != null) {
                switch (now.shape()) {
                    case ConstructPayload.SHIELD -> {
                        way = owner.getViewVector(partialTick);
                        center = pane(owner, partialTick);
                    }
                    case ConstructPayload.RAM -> {
                        way = ramWay(owner, partialTick);
                        // Seen from your own eyes it hangs in front of them, its open end just before the camera, so
                        // none of its sides ever sweeps through your view; seen from outside it is round his body.
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
                        // A bolt flies on by itself.
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
                    // The ring he holds out shines while it reads (the plane's scans shine out of its own sensor).
                    if (ring != null && now.variant() != ConstructPayload.SCAN_HOSTILE) {
                        RingSight.ringLight(painter, ring, track.clock(partialTick), own);
                    }
                }
                case ConstructPayload.HAND -> HandPainter.draw(painter, track.latest, was.facing().lerp(now.facing(),
                        partialTick), track.clock(partialTick), ring);
                case ConstructPayload.BEAM -> {
                    if (ring != null && owner != null) {
                        painter.beamOfLight(ring, beamEnd(level, owner, way, now, partialTick), solid,
                                track.clock(partialTick), 1.0);
                    }
                }
                case ConstructPayload.PLANE -> PlanePainter.draw(painter, now.id(), track.latest,
                        track.clock(partialTick), ring, partialTick);
                case ConstructPayload.MISSILE -> missile(painter, track, partialTick);
                case ConstructPayload.BULLET -> PlanePainter.bullet(painter, track.latest,
                        sinceSent(track, partialTick));
                case ConstructPayload.BLAST -> PlanePainter.missileBlast(painter, track.latest,
                        sinceSent(track, partialTick));
                // Until it breaks up its charge is the creature inside, not a time. A pound is timed by the updates as
                // they are drawn, so every slam squashes it the moment it is drawn on the ground.
                case ConstructPayload.BUBBLE -> BubblePainter.draw(painter, now, center, solid,
                        was.variant() == LightBubble.BREAKING ? charge : 0.0, now.held(), track.clock(partialTick), ring,
                        now.age() - 1.0 + partialTick - track.variantSince, now.center().subtract(was.center()));
                case ConstructPayload.POUND -> BubblePainter.pound(painter, track.latest, track.clock(partialTick));
                case ConstructPayload.SWORD -> {
                    // Your own in first person are drawn with your hands (see SwordArms).
                    if (owner != null && !own) {
                        SwordArms.draw(painter, owner, ring, partialTick);
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
        // The light every ring gathers for the beam.
        for (AbstractClientPlayer player : level.players()) {
            if (BeamCharge.charge(player, partialTick) >= 0.0F) {
                BeamCharge.draw(painter, player, ringHand(minecraft, camera, player, partialTick, event), camera,
                        partialTick);
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /**
     * A missile of an air strike on its way, drawn by its own clock (which keeps its plane's time) where it was on the
     * ticks told of, smooth between them (see Flown), and breaking into solid pieces once it has got where it struck.
     * While it still falls off the plane or a jet's wing with its motor dead, the plane draws it (see
     * {@link PlanePainter}), from the very spot it hung.
     */
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
        // Sent on the tick it was let go of, it had already moved one tick on: drawn a tick later, so it leaves the
        // plane from where it hung.
        Spot spot = flown.at(since - 1.0);
        PlanePainter.missile(painter, flown.small, spot.at().add(flown.off(since - 1.0)), spot.nose(), spot.up(),
                since - flown.ignites, -1.0);
    }
}
