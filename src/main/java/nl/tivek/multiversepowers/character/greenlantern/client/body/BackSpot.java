package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BackSpot {
    private static final long FRESH_MS = 150L;
    // The middle of the back on the suit, in the torso's own space: y runs down, +z out of the back.
    private static final Vector3f BACK = new Vector3f(0.0F, 6.0F / 16.0F, 2.3F / 16.0F);

    private record Seen(ConstructPainter.Frame frame, long when) {
    }

    private static final Map<Integer, Seen> BODIES = new HashMap<>();

    private BackSpot() {
    }

    static void onBody(Entity owner, PoseStack pose) {
        Matrix4f matrix = pose.last().pose();
        Vector3f at = matrix.transformPosition(BACK, new Vector3f());
        Vector3f out = matrix.transformDirection(0.0F, 0.0F, 1.0F, new Vector3f());
        Vector3f up = matrix.transformDirection(0.0F, -1.0F, 0.0F, new Vector3f());
        float scale = out.length();
        if (scale < 1.0E-6F || up.lengthSquared() < 1.0E-12F) {
            return;
        }
        Vec3 center = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(at.x, at.y, at.z);
        BODIES.put(owner.getId(), new Seen(ConstructPainter.Frame.of(center, new Vec3(out.x, out.y, out.z),
                new Vec3(up.x, up.y, up.z), scale), Util.getMillis()));
    }

    // The back as last drawn: forward points out of it, up along the spine.
    @Nullable
    public static ConstructPainter.Frame of(Entity owner) {
        Seen seen = BODIES.get(owner.getId());
        return seen == null || Util.getMillis() - seen.when() > FRESH_MS ? null : seen.frame();
    }

    public static void clear() {
        BODIES.clear();
    }
}
