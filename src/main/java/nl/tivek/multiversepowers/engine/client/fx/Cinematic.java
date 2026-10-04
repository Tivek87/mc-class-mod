package nl.tivek.multiversepowers.engine.client.fx;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;

// Takes over the camera for a filmed moment: the first director with a shot places it (see CameraMixin).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Cinematic {
    private static final double WALL_GAP = 0.35;
    private static final List<Director> DIRECTORS = new ArrayList<>();
    private static boolean handsHidden;

    // Where the camera stands, which way it looks and how wide it sees, in degrees the way the game counts them; a
    // film keeps its own field of view whatever the player has set.
    public record Shot(Vec3 eye, float yaw, float pitch, float roll, double fov) {
        public static Shot looking(Vec3 eye, Vec3 at, float roll, double fov) {
            Vec3 way = at.subtract(eye);
            double flat = Math.sqrt(way.x * way.x + way.z * way.z);
            return new Shot(eye, (float) Math.toDegrees(Math.atan2(-way.x, way.z)),
                    (float) -Math.toDegrees(Math.atan2(way.y, flat)), roll, fov);
        }
    }

    @FunctionalInterface
    public interface Director {
        @Nullable
        Shot shot(float partialTick);
    }

    private Cinematic() {
    }

    public static void add(Director director) {
        DIRECTORS.add(director);
    }

    @Nullable
    public static Shot shot(float partialTick) {
        for (Director director : DIRECTORS) {
            Shot shot = director.shot(partialTick);
            if (shot != null) {
                return shot;
            }
        }
        return null;
    }

    public static boolean rolling() {
        return shot(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) != null;
    }

    // A camera spot pulled in from behind a wall, towards what it films.
    public static Vec3 clear(Vec3 subject, Vec3 eye) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return eye;
        }
        BlockHitResult hit = level.clip(new ClipContext(subject, eye, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            return eye;
        }
        Vec3 back = subject.subtract(hit.getLocation());
        double far = back.length();
        return far <= WALL_GAP ? subject : hit.getLocation().add(back.scale(WALL_GAP / far));
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        Shot shot = shot((float) event.getPartialTick());
        if (shot != null) {
            event.setFOV(shot.fov());
        }
    }

    // While a film rolls no first-person hand is drawn, whatever would draw one; the switch is only flipped as a film
    // starts and stops, so the game's own use of it (huge screenshots) is left alone.
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        boolean rolling = rolling();
        if (rolling != handsHidden) {
            handsHidden = rolling;
            Minecraft.getInstance().gameRenderer.setRenderHand(!rolling);
        }
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && rolling()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        if (rolling()) {
            event.setCanceled(true);
        }
    }
}
