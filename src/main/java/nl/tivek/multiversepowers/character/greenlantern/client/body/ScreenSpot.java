package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// First-person hands draw with their own projection, so a point on them is found again by where it shows on screen.
final class ScreenSpot {
    private static final long FRESH_MS = 150L;

    private float screenX;
    private float screenY;
    private float distance;
    private long when = Long.MIN_VALUE / 2L;
    private boolean behind;
    private final Vector3f eye = new Vector3f();

    void onHand(PoseStack pose, Vec3 local) {
        Vector4f view = new Vector4f((float) local.x, (float) local.y, (float) local.z, 1.0F).mul(pose.last().pose())
                .mul(RenderSystem.getModelViewMatrix());
        Vector4f clip = new Vector4f(view).mul(RenderSystem.getProjectionMatrix());
        this.when = Util.getMillis();
        // Behind the eyes (the spin carries it round) it shows nowhere on screen: kept as a spot beside you then.
        this.behind = clip.w <= 1.0E-4F;
        if (this.behind) {
            this.eye.set(view.x, view.y, view.z);
            return;
        }
        this.screenX = clip.x / clip.w;
        this.screenY = clip.y / clip.w;
        this.distance = new Vector3f(view.x, view.y, view.z).length();
    }

    @Nullable
    Vec3 world(Camera camera, Matrix4f projection, Matrix4f modelView) {
        if (Util.getMillis() - this.when > FRESH_MS) {
            return null;
        }
        if (this.behind) {
            Vector4f at = new Vector4f(this.eye, 1.0F).mul(new Matrix4f(modelView).invert());
            return camera.getPosition().add(at.x, at.y, at.z);
        }
        Matrix4f back = new Matrix4f(projection).mul(modelView).invert();
        Vector4f near = new Vector4f(this.screenX, this.screenY, -1.0F, 1.0F).mul(back);
        Vector4f far = new Vector4f(this.screenX, this.screenY, 1.0F, 1.0F).mul(back);
        Vec3 a = new Vec3(near.x / near.w, near.y / near.w, near.z / near.w);
        Vec3 b = new Vec3(far.x / far.w, far.y / far.w, far.z / far.w);
        Vec3 way = b.subtract(a);
        if (way.lengthSqr() < 1.0E-12) {
            return null;
        }
        return camera.getPosition().add(way.normalize().scale(Math.max(0.2, this.distance)));
    }

    void forget() {
        this.when = Long.MIN_VALUE / 2L;
    }
}
