package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class RingSpot {
    private static final long FRESH_MS = 150L;
    private static final float NEAR = 0.5F;

    private record Seen(Vec3 at, long when) {
    }

    private static final Map<Integer, Seen> BODIES = new HashMap<>();
    private static float screenX;
    private static float screenY;
    private static float distance;
    private static long handWhen = Long.MIN_VALUE / 2L;

    private RingSpot() {
    }

    static void onBody(Entity owner, PoseStack pose, Vector3f local) {
        Vector3f at = pose.last().pose().transformPosition(local, new Vector3f());
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        BODIES.put(owner.getId(), new Seen(camera.getPosition().add(at.x, at.y, at.z), Util.getMillis()));
    }

    public static void onHand(PoseStack pose, Vector3f local) {
        Vector4f view = new Vector4f(local, 1.0F).mul(pose.last().pose()).mul(RenderSystem.getModelViewMatrix());
        Vector4f clip = new Vector4f(view).mul(RenderSystem.getProjectionMatrix());
        if (clip.w <= 1.0E-4F) {
            return;
        }
        screenX = clip.x / clip.w;
        screenY = clip.y / clip.w;
        distance = new Vector3f(view.x, view.y, view.z).length();
        handWhen = Util.getMillis();
    }

    @Nullable
    public static Vec3 of(Entity owner, Camera camera, Matrix4f projection, Matrix4f modelView) {
        long now = Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        if (owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached()) {
            if (now - handWhen > FRESH_MS) {
                return null;
            }
            Matrix4f back = new Matrix4f(projection).mul(modelView).invert();
            Vector4f near = new Vector4f(screenX, screenY, -1.0F, 1.0F).mul(back);
            Vector4f far = new Vector4f(screenX, screenY, 1.0F, 1.0F).mul(back);
            Vec3 a = new Vec3(near.x / near.w, near.y / near.w, near.z / near.w);
            Vec3 b = new Vec3(far.x / far.w, far.y / far.w, far.z / far.w);
            Vec3 way = b.subtract(a);
            if (way.lengthSqr() < 1.0E-12) {
                return null;
            }
            return camera.getPosition().add(way.normalize().scale(Math.max(0.2, distance * NEAR)));
        }
        Seen seen = BODIES.get(owner.getId());
        return seen == null || now - seen.when() > FRESH_MS || !(owner instanceof LivingEntity) ? null : seen.at();
    }

    public static void clear() {
        BODIES.clear();
        handWhen = Long.MIN_VALUE / 2L;
    }
}
