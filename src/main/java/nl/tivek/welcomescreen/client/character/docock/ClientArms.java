package nl.tivek.welcomescreen.client.character.docock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.network.ArmPayload;
import nl.tivek.welcomescreen.network.PortalPayload;

/**
 * Keeps the robot arms and tech portals the server sends, and draws them (see ArmPainter) smoothly:
 * <ul>
 * <li>Updates are played back one per client tick and blended between, so updates that arrive
 * unevenly over the network still move evenly.</li>
 * <li>An arm's mount is glued to where this client draws its owner (in first person: exactly where
 * you are and where you look), so it never trails behind.</li>
 * <li>A mob in a claw is put exactly at the claw's tip each tick, so claw and mob move as one.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientArms {
    // An arm or portal the server stopped updating (out of range, or a lost packet) disappears.
    private static final int TIMEOUT = 10;
    // Updates waiting beyond this many are skipped, so a network hiccup never leaves things lagging.
    private static final int MAX_WAITING = 2;

    private static final Map<Integer, Arm> ARMS = new HashMap<>();
    private static final Map<Integer, Track<PortalPayload>> PORTALS = new HashMap<>();
    private static int clientTicks;

    private ClientArms() {
    }

    /** The server updates of one arm or portal, played back one per client tick. */
    private static class Track<T> {
        private final ArrayDeque<T> waiting = new ArrayDeque<>();
        protected T previous;
        protected T current;
        private int lastSeen;

        Track(T first) {
            this.previous = first;
            this.current = first;
            this.lastSeen = clientTicks;
        }

        void add(T update) {
            this.waiting.add(update);
            this.lastSeen = clientTicks;
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

    private static final class Arm extends Track<ArmPayload> {
        // The previous shape with as many points as the current one, so the two can be blended.
        private List<Vec3> previousPoints;

        Arm(ArmPayload first) {
            super(first);
            this.previousPoints = first.points();
        }

        /** @return true when the arm jumped (through a portal) and must not be blended */
        boolean step() {
            this.advance();
            boolean jumped = this.previous.cut() != this.current.cut();
            if (jumped) {
                this.previous = this.current;
            }
            this.previousPoints = sameCount(this.previous.points(), this.current.points().size());
            return jumped;
        }
    }

    public static void update(ArmPayload payload) {
        if (payload.points().isEmpty()) {
            ARMS.remove(payload.id());
            return;
        }
        Arm arm = ARMS.get(payload.id());
        if (arm == null) {
            ARMS.put(payload.id(), new Arm(payload));
        } else {
            arm.add(payload);
        }
    }

    public static void updatePortal(PortalPayload payload) {
        if (payload.open() < 0.0F) {
            PORTALS.remove(payload.id());
            return;
        }
        Track<PortalPayload> portal = PORTALS.get(payload.id());
        if (portal == null) {
            PORTALS.put(payload.id(), new Track<>(payload));
        } else {
            portal.add(payload);
        }
    }

    // Before the world ticks: the held mob's new place must be set before it moves this tick.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        Iterator<Arm> arms = ARMS.values().iterator();
        while (arms.hasNext()) {
            Arm arm = arms.next();
            if (arm.timedOut()) {
                arms.remove();
                continue;
            }
            boolean jumped = arm.step();
            hold(level, arm.current, jumped);
        }
        Iterator<Track<PortalPayload>> portals = PORTALS.values().iterator();
        while (portals.hasNext()) {
            Track<PortalPayload> portal = portals.next();
            if (portal.timedOut()) {
                portals.remove();
                continue;
            }
            portal.advance();
            sparks(level, portal.current);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ARMS.clear();
        PORTALS.clear();
    }

    /**
     * Puts the mob in the claw exactly at the tip, where it stays for this whole tick. Vanilla then
     * draws it blended from its last place, just like the arm, so the two never drift apart.
     */
    private static void hold(ClientLevel level, ArmPayload arm, boolean jumped) {
        if (arm.heldId() < 0) {
            return;
        }
        Entity entity = level.getEntity(arm.heldId());
        if (entity == null || entity == Minecraft.getInstance().player) {
            return;
        }
        Vec3 tip = arm.points().get(arm.points().size() - 1);
        double y = tip.y - entity.getBbHeight() / 2;
        if (jumped) {
            entity.moveTo(tip.x, y, tip.z, entity.getYRot(), entity.getXRot());
        }
        entity.lerpTo(tip.x, y, tip.z, entity.getYRot(), entity.getXRot(), 1);
    }

    /** Now and then a spark flying off a portal's ring. */
    private static void sparks(ClientLevel level, PortalPayload portal) {
        RandomSource random = level.random;
        // Only once the energy is open; more while it tears open. Shields throw none.
        float chance = portal.style() != PortalPayload.STYLE_PORTAL || portal.open() < 0.55F ? 0.0F
                : portal.open() < 0.999F ? 0.9F : 0.6F;
        if (random.nextFloat() >= chance) {
            return;
        }
        Vec3 n = portal.normal();
        Vec3 e1 = ArmPainter.anySquare(n);
        Vec3 e2 = n.cross(e1);
        double a = random.nextDouble() * Math.PI * 2;
        Vec3 out = e1.scale(Math.cos(a)).add(e2.scale(Math.sin(a)));
        Vec3 at = portal.center().add(out.scale(portal.size()));
        Vec3 v = out.scale(0.1).add(n.scale(random.nextGaussian() * 0.05));
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, v.x, v.y, v.z);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || (ARMS.isEmpty() && PORTALS.isEmpty())) {
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
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ArmPainter painter = new ArmPainter(event.getPoseStack(), buffers, minecraft.getBlockRenderer(), level,
                event.getCamera().getPosition(), minecraft.options.getCameraType().isFirstPerson(), time);
        for (Arm arm : ARMS.values()) {
            ArmPayload before = arm.previous;
            ArmPayload now = arm.current;
            List<Vec3> points = new ArrayList<>(now.points().size());
            for (int i = 0; i < now.points().size(); i++) {
                points.add(arm.previousPoints.get(i).lerp(now.points().get(i), partialTick));
            }
            Vec3 reference = anchor(level, before, now, points, partialTick);
            float claw = before.claw() < 0.0F || now.claw() < 0.0F ? now.claw()
                    : Mth.lerp(partialTick, before.claw(), now.claw());
            painter.clip(now.clips() ? now.clipPoint() : null, now.clipNormal());
            painter.arm(points, reference, claw, now.thickness(),
                    Mth.lerp(partialTick, before.tipOffset(), now.tipOffset()),
                    now.lamps() == ArmPayload.LAMPS_RAGE,
                    Mth.lerp(partialTick, before.spike(), now.spike()),
                    Mth.lerp(partialTick, before.thrust(), now.thrust()),
                    now.carried());
        }
        painter.clip(null, Vec3.ZERO);
        for (Track<PortalPayload> portal : PORTALS.values()) {
            PortalPayload was = portal.previous;
            PortalPayload now = portal.current;
            // Where it is and which way it faces are blended between ticks as well, so a shield that
            // follows your head moves as smoothly as you turn. A real jump (a portal that opens
            // somewhere else) is too far to blend, so that one is simply drawn where it is now.
            boolean near = was.center().distanceToSqr(now.center()) < 16.0;
            Vec3 center = near ? was.center().lerp(now.center(), partialTick) : now.center();
            Vec3 normal = near ? was.normal().lerp(now.normal(), partialTick) : now.normal();
            if (normal.lengthSqr() < 1.0E-6) {
                normal = now.normal();
            }
            painter.portal(center, normal.normalize(), Mth.lerp(partialTick, was.size(), now.size()),
                    Mth.lerp(partialTick, was.open(), now.open()), now.style());
        }
        painter.finish();
        buffers.endBatch();
    }

    /**
     * Moves the arm's points from where the server's copy of its owner stood to where this client
     * draws the owner, turned along with the owner's yaw; fully at the mount, less towards the tip
     * as much as the server asks. Returns the owner's right-hand side, which keeps the arm's pieces
     * from rolling when the owner turns (null without an owner).
     */
    @Nullable
    private static Vec3 anchor(ClientLevel level, ArmPayload before, ArmPayload now, List<Vec3> points,
            float partialTick) {
        if (now.anchorId() < 0) {
            return null;
        }
        boolean same = before.anchorId() == now.anchorId();
        Vec3 serverPos = same ? before.anchorPos().lerp(now.anchorPos(), partialTick) : now.anchorPos();
        float serverYaw = same ? Mth.rotLerp(partialTick, before.anchorYaw(), now.anchorYaw()) : now.anchorYaw();
        Entity owner = level.getEntity(now.anchorId());
        float yaw = serverYaw;
        if (owner != null) {
            yaw = owner.getViewYRot(partialTick);
            Vec3 shift = owner.getPosition(partialTick).subtract(serverPos);
            double turn = Math.toRadians(Mth.wrapDegrees(yaw - serverYaw));
            float blend = same ? Mth.lerp(partialTick, before.anchorBlend(), now.anchorBlend()) : now.anchorBlend();
            int n = points.size();
            for (int i = 0; i < n; i++) {
                double t = n < 2 ? 0.0 : (double) i / (n - 1);
                double weight = 1.0 - blend * t * t * (3 - 2 * t);
                Vec3 offset = turn(points.get(i).subtract(serverPos), turn * weight);
                points.set(i, serverPos.add(offset).add(shift.scale(weight)));
            }
        }
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.cos(radians), 0, -Math.sin(radians));
    }

    /** {@code v} turned {@code angle} radians around the vertical axis, the way yaw turns. */
    private static Vec3 turn(Vec3 v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** The same line with {@code count} evenly spaced points. */
    private static List<Vec3> sameCount(List<Vec3> points, int count) {
        if (points.size() == count) {
            return points;
        }
        ArmPainter.Frames frames = new ArmPainter.Frames(points, null);
        List<Vec3> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(frames.pointAt(count < 2 ? 0.0 : frames.length * i / (count - 1)));
        }
        return result;
    }
}
