package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class SwordSpot {
    private static final long FRESH_MS = 150L;

    record Spot(Vec3 x, Vec3 y, Vec3 z, Vec3 grip, Vec3 mount, long when, Vec3 origin, double scale) {
        Vec3 world(Vec3 way, float twist) {
            double cos = Mth.cos(twist);
            double sin = Mth.sin(twist);
            // the model's own axes are mirrored (left/down/back) from the pose's (right/up/ahead)
            double mx = -way.x;
            double my = -way.y;
            double mz = -way.z;
            double tx = mx * cos + mz * sin;
            double tz = -mx * sin + mz * cos;
            return this.x.scale(tx).add(this.y.scale(my)).add(this.z.scale(tz));
        }

        Vec3 at(Vec3 place) {
            return this.origin.add(this.world(place.subtract(0.0, SHOULDERS, 0.0), 0.0F).scale(this.scale));
        }
    }

    private static final double SHOULDERS = 2.0 / 16.0;

    private static final class Measured {
        Vec3 x = new Vec3(-1.0, 0.0, 0.0);
        Vec3 y = new Vec3(0.0, -1.0, 0.0);
        Vec3 z = new Vec3(0.0, 0.0, -1.0);
        Vec3 grip = Vec3.ZERO;
        Vec3 mount = Vec3.ZERO;
        Vec3 origin = Vec3.ZERO;
        double scale = 1.0;
        long root = Long.MIN_VALUE / 2L;
        long right = Long.MIN_VALUE / 2L;
        long left = Long.MIN_VALUE / 2L;
    }

    private static final Map<Integer, Measured> BODIES = new HashMap<>();

    private SwordSpot() {
    }

    static void onRoot(Entity owner, PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        Measured measured = BODIES.computeIfAbsent(owner.getId(), id -> new Measured());
        measured.x = way(matrix, 1.0F, 0.0F, 0.0F);
        measured.y = way(matrix, 0.0F, 1.0F, 0.0F);
        measured.z = way(matrix, 0.0F, 0.0F, 1.0F);
        Vector3f origin = matrix.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
        measured.origin = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(origin.x, origin.y,
                origin.z);
        measured.scale = matrix.transformDirection(1.0F, 0.0F, 0.0F, new Vector3f()).length();
        measured.root = Util.getMillis();
    }

    static void onArm(Entity owner, PoseStack pose, boolean right, boolean slim) {
        Measured measured = BODIES.computeIfAbsent(owner.getId(), id -> new Measured());
        Vector3f local = right ? new Vector3f((slim ? -0.5F : -1.0F) / 16.0F, 9.4F / 16.0F, 0.0F)
                : new Vector3f((slim ? 2.4F : 3.4F) / 16.0F, 6.4F / 16.0F, 0.0F);
        Vector3f at = pose.last().pose().transformPosition(local, new Vector3f());
        Vec3 world = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(at.x, at.y, at.z);
        if (right) {
            measured.grip = world;
            measured.right = Util.getMillis();
        } else {
            measured.mount = world;
            measured.left = Util.getMillis();
        }
    }

    @Nullable
    static Spot of(Entity owner) {
        Measured measured = BODIES.get(owner.getId());
        long now = Util.getMillis();
        if (measured == null || now - measured.root > FRESH_MS || now - measured.right > FRESH_MS
                || now - measured.left > FRESH_MS) {
            return null;
        }
        return new Spot(measured.x, measured.y, measured.z, measured.grip, measured.mount, measured.root,
                measured.origin, measured.scale);
    }

    static void clear() {
        BODIES.clear();
    }

    private static Vec3 way(Matrix4f matrix, float x, float y, float z) {
        Vector3f way = matrix.transformDirection(x, y, z, new Vector3f());
        float length = way.length();
        return length < 1.0E-6F ? new Vec3(x, y, z) : new Vec3(way.x / length, way.y / length, way.z / length);
    }
}
