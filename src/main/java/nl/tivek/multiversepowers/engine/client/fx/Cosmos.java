package nl.tivek.multiversepowers.engine.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.lwjgl.opengl.GL11;

// Windows into a deep sky of stars and slow clouds, cut in a surface: what is seen through one lies far behind it and
// shifts against the eye as true depth does. A window is drawn at once, into the world as drawn so far, and writes its
// depth, so whatever is drawn after it behind its surface stays hidden. Outside the world's drawing none is drawn.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Cosmos {
    // Lifted off its surface this far, so the surface never shows through it.
    private static final double LIFT = 0.01;
    @Nullable
    private static ShaderInstance shader;
    private static boolean inLevel;

    private Cosmos() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            inLevel = true;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            inLevel = false;
        }
    }

    // The outline runs round the window, about center in the plane square to normal, along lying in that plane. Depth
    // is how far behind the surface its nearest stars lie, deep and light its darkest and brightest colours, and glow
    // how bright it burns (1 as it is).
    public static void window(Vec3 center, Vec3 normal, Vec3 along, List<Vec3> outline, double depth, int deep,
            int light, double glow, int seed) {
        ShaderInstance sky = shader;
        if (sky == null || !inLevel || outline.size() < 3) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 n = normal.normalize();
        Vec3 a = along.subtract(n.scale(along.dot(n))).normalize();
        Vec3 b = n.cross(a);
        Vec3 lift = n.scale(LIFT);
        Vec3 middle = center.add(lift).subtract(camera);
        float clock = (minecraft.level == null ? 0.0F : minecraft.level.getGameTime() % 24000L)
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        sky.safeGetUniform("Center").set((float) middle.x, (float) middle.y, (float) middle.z);
        sky.safeGetUniform("AxisA").set((float) a.x, (float) a.y, (float) a.z);
        sky.safeGetUniform("AxisB").set((float) b.x, (float) b.y, (float) b.z);
        sky.safeGetUniform("Normal").set((float) n.x, (float) n.y, (float) n.z);
        sky.safeGetUniform("Depth").set((float) depth);
        sky.safeGetUniform("Clock").set(clock);
        sky.safeGetUniform("Deep").set(channel(deep, 16), channel(deep, 8), channel(deep, 0));
        sky.safeGetUniform("Light").set(channel(light, 16), channel(light, 8), channel(light, 0));
        sky.safeGetUniform("Glow").set((float) glow);
        sky.safeGetUniform("Seed").set((float) Math.floorMod(seed, 997));
        // A fan from the middle: its colour's alpha runs from 1 there to 0 on the outline, how near the edge it is.
        BufferBuilder fan = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < outline.size(); i++) {
            Vec3 from = outline.get(i).add(lift).subtract(camera);
            Vec3 to = outline.get((i + 1) % outline.size()).add(lift).subtract(camera);
            fan.addVertex((float) middle.x, (float) middle.y, (float) middle.z).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            fan.addVertex((float) from.x, (float) from.y, (float) from.z).setColor(1.0F, 1.0F, 1.0F, 0.0F);
            fan.addVertex((float) to.x, (float) to.y, (float) to.z).setColor(1.0F, 1.0F, 1.0F, 0.0F);
        }
        RenderSystem.setShader(() -> sky);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -10.0F);
        BufferUploader.drawWithShader(fan.buildOrThrow());
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
    }

    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255.0F;
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // A shader that will not build only leaves the windows out (callers draw their edges anyway), never the game.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "cosmos");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name,
                    DefaultVertexFormat.POSITION_COLOR), loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The cosmos shader could not be built", e);
        }
    }
}
