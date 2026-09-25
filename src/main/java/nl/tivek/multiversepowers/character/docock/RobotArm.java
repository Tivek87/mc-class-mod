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

public final class RobotArm {
    private static final double VIEW_RANGE = 128.0;
    private static final double GONE_RANGE = VIEW_RANGE + 32.0;
    // Must stay well under ClientArms.TIMEOUT or idle arms would vanish.
    private static final int RESEND = 4;
    private static final int MIN_POINTS = 12;
    private static int nextId;
    private static final Map<Integer, Sent> SENT = new HashMap<>();

    private record Sent(CustomPacketPayload payload, Vec3 start, Vec3 end, int tick) {
    }

    private RobotArm() {
    }

    public static int newId() {
        nextId++;
        return nextId;
    }

    public static Shape arm(int id, List<Vec3> path) {
        return new Shape(id, path);
    }

    public static void remove(ServerLevel level, int id) {
        gone(level, id, ArmPayload.remove(id));
    }

    public static void portal(ServerLevel level, int id, Vec3 center, Vec3 normal, double size, double open) {
        disc(level, id, center, normal, size, open, PortalPayload.STYLE_PORTAL);
    }

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

    public static void clear() {
        SENT.clear();
    }

    private static void broadcast(ServerLevel level, int id, Vec3 start, Vec3 end, CustomPacketPayload payload) {
        int tick = level.getServer().getTickCount();
        Sent last = SENT.get(id);
        if (last != null && tick - last.tick() < RESEND && last.payload().equals(payload)) {
            return;
        }
        SENT.put(id, new Sent(payload, start, end, tick));
        send(level, start, end, VIEW_RANGE, payload);
    }

    private static void gone(ServerLevel level, int id, CustomPacketPayload payload) {
        Sent last = SENT.remove(id);
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

        public Shape claw(double open) {
            this.claw = open;
            return this;
        }

        public Shape thickness(double thickness) {
            this.thickness = thickness;
            return this;
        }

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

        public Shape clip(Vec3 point, Vec3 normal) {
            this.clipPoint = point;
            this.clipNormal = normal.normalize();
            return this;
        }

        public Shape lamps(int lamps) {
            this.lamps = lamps;
            return this;
        }

        public Shape tools(double spike, double thrust) {
            this.spike = spike;
            this.thrust = thrust;
            return this;
        }

        public Shape carrying(List<ArmPayload.Carried> carried) {
            this.carried = carried;
            return this;
        }

        public void send(ServerLevel level) {
            if (this.path.isEmpty()) {
                return;
            }
            int count = Mth.clamp((int) (length(this.path) * 3) + 2, MIN_POINTS, ArmPayload.MAX_POINTS);
            List<Vec3> points = resample(this.path, count);
            broadcast(level, this.id, points.get(0), points.get(points.size() - 1), new ArmPayload(this.id, points,
                    (float) this.claw, (float) this.thickness, this.anchorId, this.anchorPos,
                    this.anchorYaw, (float) this.anchorBlend, (float) this.tipOffset, this.heldId, this.cut,
                    this.clipPoint, this.clipNormal, this.lamps, (float) this.spike, (float) this.thrust,
                    this.carried));
        }
    }

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

    public static Vec3 right(ServerPlayer player) {
        Vec3 forward = forward(player);
        return new Vec3(-forward.z, 0, forward.x);
    }

    public static Vec3 forward(ServerPlayer player) {
        float yaw = (float) Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }
}
