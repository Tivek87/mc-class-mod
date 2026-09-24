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

/**
 * Where the hands of every Green Lantern with the sword and shield really are: measured each time his body is drawn
 * (see {@link GreenLanternSuitLayer}), so the sword sits in his fist and the shield on his forearm wherever his arms go,
 * also when he flies, crouches or turns. Next to the grip and the forearm it keeps how his whole body stands, to turn the
 * ways a pose gives (seen from his upper body) into ways in the world.
 */
final class SwordSpot {
    // A spot older than this is not trusted any more (his body went out of view).
    private static final long FRESH_MS = 150L;

    /**
     * How one body stood when it was drawn last: its own three ways out in the world (x to its left, y down, z back, as
     * the game's models have them), the grip of its right fist, the outside of its left forearm, and when.
     */
    record Spot(Vec3 x, Vec3 y, Vec3 z, Vec3 grip, Vec3 mount, long when) {
        /**
         * A way seen from his upper body (x to his right, y up, z ahead), with the upper body turned {@code twist} to
         * his right, as a way out in the world.
         */
        Vec3 world(Vec3 way, float twist) {
            double cos = Mth.cos(twist);
            double sin = Mth.sin(twist);
            double mx = -way.x;
            double my = -way.y;
            double mz = -way.z;
            double tx = mx * cos + mz * sin;
            double tz = -mx * sin + mz * cos;
            return this.x.scale(tx).add(this.y.scale(my)).add(this.z.scale(tz));
        }
    }

    private static final class Measured {
        Vec3 x = new Vec3(-1.0, 0.0, 0.0);
        Vec3 y = new Vec3(0.0, -1.0, 0.0);
        Vec3 z = new Vec3(0.0, 0.0, -1.0);
        Vec3 grip = Vec3.ZERO;
        Vec3 mount = Vec3.ZERO;
        long root = Long.MIN_VALUE / 2L;
        long right = Long.MIN_VALUE / 2L;
        long left = Long.MIN_VALUE / 2L;
    }

    private static final Map<Integer, Measured> BODIES = new HashMap<>();

    private SwordSpot() {
    }

    /** His body is about to be drawn: {@code pose} stands on the model as a whole. */
    static void onRoot(Entity owner, PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        Measured measured = BODIES.computeIfAbsent(owner.getId(), id -> new Measured());
        measured.x = way(matrix, 1.0F, 0.0F, 0.0F);
        measured.y = way(matrix, 0.0F, 1.0F, 0.0F);
        measured.z = way(matrix, 0.0F, 0.0F, 1.0F);
        measured.root = Util.getMillis();
    }

    /**
     * One of his arms was just posed for drawing: {@code pose} stands on it. For the right arm this keeps the grip of the
     * fist, for the left one the outside of the forearm, where the shield sits.
     */
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

    /** Where this body stood when it was drawn last, or null when it was not drawn lately. */
    @Nullable
    static Spot of(Entity owner) {
        Measured measured = BODIES.get(owner.getId());
        long now = Util.getMillis();
        if (measured == null || now - measured.root > FRESH_MS || now - measured.right > FRESH_MS
                || now - measured.left > FRESH_MS) {
            return null;
        }
        return new Spot(measured.x, measured.y, measured.z, measured.grip, measured.mount, measured.root);
    }

    /** Forgets everyone (you left the world). */
    static void clear() {
        BODIES.clear();
    }

    private static Vec3 way(Matrix4f matrix, float x, float y, float z) {
        Vector3f way = matrix.transformDirection(x, y, z, new Vector3f());
        float length = way.length();
        return length < 1.0E-6F ? new Vec3(x, y, z) : new Vec3(way.x / length, way.y / length, way.z / length);
    }
}
