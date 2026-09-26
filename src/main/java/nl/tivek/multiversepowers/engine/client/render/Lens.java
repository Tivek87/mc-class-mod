package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Bubbles of bent light: the world behind one is seen as through a ball of glass. Whoever wants one asks again every
// frame while drawing the world; they are all laid over the finished picture, before the hand, by copying it and
// reading the copy back bent.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Lens {
    private static final int MOST = 16;
    private static final List<Bubble> BUBBLES = new ArrayList<>();
    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static RenderTarget copy;

    private record Bubble(Vec3 center, double radius, float strength, int tint) {
    }

    private Lens() {
    }

    public static boolean available() {
        return shader != null;
    }

    public static void bubble(Vec3 center, double radius, double strength, int tint) {
        if (shader != null && radius > 0.01 && strength > 0.001 && BUBBLES.size() < MOST) {
            BUBBLES.add(new Bubble(center, radius, (float) Math.min(1.0, strength), tint));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            BUBBLES.clear();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || BUBBLES.isEmpty() || shader == null) {
            return;
        }
        try {
            draw(event, shader);
        } finally {
            BUBBLES.clear();
        }
    }

    private static void draw(RenderLevelStageEvent event, ShaderInstance lens) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget target = copy;
        if (target == null) {
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
            copy = target;
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
        if (main.isStencilEnabled() && !target.isStencilEnabled()) {
            target.enableStencil();
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);

        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f view = event.getModelViewMatrix();
        Vec3 camera = event.getCamera().getPosition();
        float clock = (minecraft.level == null ? 0.0F : minecraft.level.getGameTime() % 24000L)
                + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        lens.setSampler("SceneSampler", target.getColorTextureId());
        lens.setSampler("DepthSampler", target.getDepthTextureId());
        lens.safeGetUniform("Projection").set(projection);
        lens.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        lens.safeGetUniform("Clock").set(clock);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> lens);
        for (Bubble bubble : BUBBLES) {
            Vector3f center = view.transformPosition((float) (bubble.center.x - camera.x),
                    (float) (bubble.center.y - camera.y), (float) (bubble.center.z - camera.z), new Vector3f());
            lens.safeGetUniform("Center").set(center.x, center.y, center.z);
            lens.safeGetUniform("Radius").set((float) bubble.radius);
            lens.safeGetUniform("Strength").set(bubble.strength);
            lens.safeGetUniform("Tint").set(((bubble.tint >> 16) & 0xFF) / 255.0F, ((bubble.tint >> 8) & 0xFF) / 255.0F,
                    (bubble.tint & 0xFF) / 255.0F);
            BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            quad.addVertex(0.0F, 0.0F, 0.0F);
            quad.addVertex(1.0F, 0.0F, 0.0F);
            quad.addVertex(1.0F, 1.0F, 0.0F);
            quad.addVertex(0.0F, 1.0F, 0.0F);
            BufferUploader.drawWithShader(quad.buildOrThrow());
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // A shader that will not build only takes the bubbles away (callers draw their own then), never the game.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "lens");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name, DefaultVertexFormat.POSITION),
                    loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The lens shader could not be built", e);
        }
    }
}
