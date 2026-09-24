package nl.tivek.multiversepowers.character.docock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of the robot arms and tech portals (Iron Tentacle, Portal Grab): each tick a spell
 * works out the shape of its arms and portals and sends it here; nearby clients draw them as 3D
 * models (see ClientArms).
 */
public final class RobotArm {
    private static final double VIEW_RANGE = 128.0;
    // Word that one is gone reaches a little further, to everyone who may still be drawing it.
    private static final double GONE_RANGE = VIEW_RANGE + 32.0;
    // An arm or portal that has not changed is not sent again every tick, only this often: well within
    // the time after which a client stops drawing one it hears nothing about (see ClientArms).
    private static final int RESEND = 4;
    private static final int MIN_POINTS = 12;
    private static int nextId;
    // Per arm or portal: what was last sent, where it was, and on which server tick.
    private static final Map<Integer, Sent> SENT = new HashMap<>();

    private record Sent(CustomPacketPayload payload, Vec3 start, Vec3 end, int tick) {
    }

    private RobotArm() {
    }

    /**
     * A fresh id for one arm or portal; send it every tick, and remove it when done.
     */
    public static int newId() {
        nextId++;
        return nextId;
    }

    /** Starts describing arm {@code id} for this tick; finish with {@link Shape#send}. */
    public static Shape arm(int id, List<Vec3> path) {
        return new Shape(id, path);
    }

    public static void remove(ServerLevel level, int id) {
        gone(level, id, ArmPayload.remove(id));
    }

    /** @param open 0 = shut, 1 = fully open (see PortalPayload) */
    public static void portal(ServerLevel level, int id, Vec3 center, Vec3 normal, double size, double open) {
        disc(level, id, center, normal, size, open, PortalPayload.STYLE_PORTAL);
    }

    /** A glowing energy shield, without a ring around it. */
    public static void shield(ServerLevel level, int id, Vec3 center, Vec3 normal, double size, double open) {
        disc(level, id, center, normal, size, open, PortalPayload.STYLE_SHIELD);
    }

    private static void disc(ServerLevel level, int id, Vec3 center, Vec3 normal, double size, double open,
            int style) {
        broadcast(level, id, center, center, new PortalPayload(id, center, normal.normalize(), (float) size,
                (float) open, style));
    }

    public static void removePortal(ServerLevel level, int id) {
        gone(level, id, PortalPayload.remove(id));
    }

    /** Forgets everything that was sent (the server stops). */
    public static void clear() {
        SENT.clear();
    }

    /** Sends this tick's shape to the players near it, unless it is exactly what they already have. */
    private static void broadcast(ServerLevel level, int id, Vec3 start, Vec3 end, CustomPacketPayload payload) {
        int tick = level.getServer().getTickCount();
        Sent last = SENT.get(id);
        if (last != null && tick - last.tick() < RESEND && last.payload().equals(payload)) {
            return;
        }
        SENT.put(id, new Sent(payload, start, end, tick));
        send(level, start, end, VIEW_RANGE, payload);
    }

    /** Tells the players around where an arm or portal was last sent that it is gone. */
    private static void gone(ServerLevel level, int id, CustomPacketPayload payload) {
        Sent last = SENT.remove(id);
        // One that was never sent is on no client, so there is nobody to tell.
        if (last != null) {
            send(level, last.start(), last.end(), GONE_RANGE, payload);
        }
    }

    private static void send(ServerLevel level, Vec3 start, Vec3 end, double range, CustomPacketPayload payload) {
        double rangeSqr = range * range;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(start) < rangeSqr || player.distanceToSqr(end) < rangeSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** One tick's shape of one arm. See {@link ArmPayload} for what each part means. */
    public static final class Shape {
        private final int id;
        private final List<Vec3> path;
        private double claw = -1.0;
        private double thickness = 1.0;
        private int anchorId = -1;
        private Vec3 anchorPos = Vec3.ZERO;
        private float anchorYaw;
        private double anchorBlend;
        private double tipOffset;
        private int heldId = -1;
        private int cut;
        private Vec3 clipPoint = Vec3.ZERO;
        private Vec3 clipNormal = Vec3.ZERO;
        private int lamps = ArmPayload.LAMPS_NORMAL;
        private double spike;
        private double thrust;
        private List<ArmPayload.Carried> carried = List.of();

        private Shape(int id, List<Vec3> path) {
            this.id = id;
            this.path = path;
        }

        /** How wide the claw is open in blocks; below 0 for no claw. */
        public Shape claw(double open) {
            this.claw = open;
            return this;
        }

        public Shape thickness(double thickness) {
            this.thickness = thickness;
            return this;
        }

        /** Mounted on {@code player}; {@code blend} 0 = moves along whole, 1 = the tip stays put. */
        public Shape anchor(ServerPlayer player, double blend) {
            this.anchorId = player.getId();
            this.anchorPos = player.position();
            this.anchorYaw = player.getYRot();
            this.anchorBlend = blend;
            return this;
        }

        public Shape tipOffset(double length) {
            this.tipOffset = length;
            return this;
        }

        public Shape holding(@Nullable Entity entity) {
            this.heldId = entity == null ? -1 : entity.getId();
            return this;
        }

        public Shape cut(int cut) {
            this.cut = cut;
            return this;
        }

        /** Nothing past the plane through {@code point}, on the side {@code normal} points to, is drawn. */
        public Shape clip(Vec3 point, Vec3 normal) {
            this.clipPoint = point;
            this.clipNormal = normal.normalize();
            return this;
        }

        public Shape lamps(int lamps) {
            this.lamps = lamps;
            return this;
        }

        /** The Portal ability's tools at the tip: the sharp point, and the thrusters. */
        public Shape tools(double spike, double thrust) {
            this.spike = spike;
            this.thrust = thrust;
            return this;
        }

        /** Blocks clamped in the claw, each one offset from the tip. */
        public Shape carrying(List<ArmPayload.Carried> carried) {
            this.carried = carried;
            return this;
        }

        public void send(ServerLevel level) {
            if (this.path.isEmpty()) {
                return;
            }
            // About three points per block: long arms keep their curves, short ones stay small.
            int count = Mth.clamp((int) (length(this.path) * 3) + 2, MIN_POINTS, ArmPayload.MAX_POINTS);
            List<Vec3> points = resample(this.path, count);
            broadcast(level, this.id, points.get(0), points.get(points.size() - 1), new ArmPayload(this.id, points,
                    (float) this.claw, (float) this.thickness, this.anchorId, this.anchorPos,
                    this.anchorYaw, (float) this.anchorBlend, (float) this.tipOffset, this.heldId, this.cut,
                    this.clipPoint, this.clipNormal, this.lamps, (float) this.spike, (float) this.thrust,
                    this.carried));
        }
    }

    /**
     * The same line with exactly {@code count} evenly spaced points, so the client
     * can smoothly blend
     * one tick's shape into the next.
     */
    public static List<Vec3> resample(List<Vec3> path, int count) {
        List<Vec3> result = new ArrayList<>(count);
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i - 1).distanceTo(path.get(i));
        }
        if (path.size() < 2 || total < 1.0E-4) {
            for (int i = 0; i < count; i++) {
                result.add(path.get(0));
            }
            return result;
        }
        int segment = 1;
        double walked = 0;
        for (int i = 0; i < count; i++) {
            double wanted = total * i / (count - 1);
            while (segment < path.size() - 1
                    && walked + path.get(segment - 1).distanceTo(path.get(segment)) < wanted) {
                walked += path.get(segment - 1).distanceTo(path.get(segment));
                segment++;
            }
            Vec3 from = path.get(segment - 1);
            Vec3 to = path.get(segment);
            double length = from.distanceTo(to);
            result.add(length < 1.0E-6 ? to : from.lerp(to, Math.min(1.0, (wanted - walked) / length)));
        }
        return result;
    }

    /**
     * Points on a smooth curve from {@code a} to {@code b}, bent towards the two
     * control points.
     */
    public static List<Vec3> curve(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, int steps) {
        List<Vec3> points = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1.0 - t;
            points.add(a.scale(u * u * u).add(c1.scale(3 * u * u * t)).add(c2.scale(3 * u * t * t))
                    .add(b.scale(t * t * t)));
        }
        return points;
    }

    public static double length(List<Vec3> path) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i - 1).distanceTo(path.get(i));
        }
        return total;
    }

    /**
     * The first part of a line, {@code length} blocks long (the whole line if it is
     * shorter).
     */
    public static List<Vec3> firstPart(List<Vec3> path, double length) {
        List<Vec3> result = new ArrayList<>();
        result.add(path.get(0));
        double walked = 0;
        for (int i = 1; i < path.size(); i++) {
            double step = path.get(i - 1).distanceTo(path.get(i));
            if (walked + step >= length) {
                result.add(path.get(i - 1).lerp(path.get(i), step < 1.0E-6 ? 1.0 : (length - walked) / step));
                return result;
            }
            walked += step;
            result.add(path.get(i));
        }
        return result;
    }

    /**
     * Flat sideways (right-hand) direction of a player, for placing arms on their
     * back.
     */
    public static Vec3 right(ServerPlayer player) {
        Vec3 forward = forward(player);
        return new Vec3(-forward.z, 0, forward.x);
    }

    /** Flat forward direction of a player. */
    public static Vec3 forward(ServerPlayer player) {
        float yaw = (float) Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }
}
