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

/**
 * Where every ring really is on the screen: measured each time the ring itself is drawn, on a body seen from
 * outside and on your own hand in first person. The beam to a construct, the bolts and the beam of light all
 * leave the stone from here, so they always come out of the ring and not out of thin air next to it.
 *
 * <p>Your own hand is drawn with a view of its own, narrower than the world around it (the game always draws
 * it as if your field of view were 70), and after the world. So for your own ring this keeps where on the screen
 * the stone showed up last frame, and finds the point of the world that shows up on that same spot.
 */
public final class RingSpot {
    // A spot older than this is not trusted any more (the ring went out of view, or the hand was not drawn).
    private static final long FRESH_MS = 150L;
    // How far in front of the camera your own ring's light starts, as a part of how far the hand really is.
    private static final float NEAR = 0.5F;

    private record Seen(Vec3 at, long when) {
    }

    private static final Map<Integer, Seen> BODIES = new HashMap<>();
    // Your own ring in first person: where on the screen it was (-1 to 1 both ways), how far away, and when.
    private static float screenX;
    private static float screenY;
    private static float distance;
    private static long handWhen = Long.MIN_VALUE / 2L;

    private RingSpot() {
    }

    /**
     * The ring was just drawn on a body in the world. {@code pose} stands on the arm, as the ring drew it;
     * {@code local} is the middle of the stone in the arm's own blocks.
     */
    static void onBody(Entity owner, PoseStack pose, Vector3f local) {
        Vector3f at = pose.last().pose().transformPosition(local, new Vector3f());
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        BODIES.put(owner.getId(), new Seen(camera.getPosition().add(at.x, at.y, at.z), Util.getMillis()));
    }

    /** Your own ring was just drawn on your hand in first person: remember where on the screen it showed up. */
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

    /**
     * Where the ring of this player is, as a point in the world that shows up exactly on the stone, or null when
     * it was not drawn lately.
     *
     * @param projection the world's projection this frame, and {@code modelView} its view turn
     */
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

    /** Forgets everyone (you left the world). */
    public static void clear() {
        BODIES.clear();
        handWhen = Long.MIN_VALUE / 2L;
    }
}
