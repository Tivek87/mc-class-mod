package nl.tivek.multiversepowers.engine.client.fx;

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
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Black holes and the rings of bent light they burst into, laid over the finished picture before the hand: the world
// behind a hole is seen bent round it, a disc of glowing matter whirls round it, and its pull drags the whole picture
// (and the field of view) in towards it. Whoever wants one asks again every frame while drawing the world.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Singularity {
    private static final int MOST = 4;
    // How far round a hole, in its horizons, light is bent.
    private static final double BOUND = 11.0;
    private static final double FOV_PULL = 0.12;
    private static final List<Hole> HOLES = new ArrayList<>();
    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static RenderTarget copy;
    private static double pulled;

    // Axis is square to its disc, inner and outer the disc's edges in horizons, collapse 0 to 1 as it falls in on
    // itself, pull 0 to 1 how hard it drags the picture in; wave the radius of a running ring in blocks, 0 for none.
    private record Hole(Vec3 center, double horizon, Vec3 axis, double inner, double outer, float strength,
            float collapse, int hot, int cool, float pull, double wave, float waveStrength, int seed) {
    }

    private Singularity() {
    }

    public static boolean available() {
        return shader != null;
    }

    public static void hole(Vec3 center, double horizon, Vec3 axis, double inner, double outer, double strength,
            double collapse, int hot, int cool, double pull, int seed) {
        if (shader != null && horizon > 0.01 && strength > 0.001 && HOLES.size() < MOST) {
            HOLES.add(new Hole(center, horizon, axis.normalize(), inner, outer, clamp(strength), clamp(collapse), hot,
                    cool, clamp(pull), 0.0, 0.0F, seed));
        }
    }

    // A ring of bent light running out from center, radius blocks across now.
    public static void wave(Vec3 center, double radius, double strength, int tint) {
        if (shader != null && radius > 0.01 && strength > 0.001 && HOLES.size() < MOST) {
            HOLES.add(new Hole(center, 1.0, new Vec3(0.0, 1.0, 0.0), 0.0, 0.0, 0.0F, 0.0F, tint, tint, 0.0F, radius,
                    clamp(strength), 0));
        }
    }

    private static float clamp(double value) {
        return (float) Math.max(0.0, Math.min(1.0, value));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            HOLES.clear();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        double most = 0.0;
        for (Hole hole : HOLES) {
            most = Math.max(most, hole.pull);
        }
        pulled = most;
        if (HOLES.isEmpty() || shader == null) {
            return;
        }
        try {
            draw(event, shader);
        } finally {
            HOLES.clear();
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (pulled > 0.001) {
            event.setNewFovModifier((float) (event.getNewFovModifier() * (1.0 - FOV_PULL * pulled)));
        }
    }

    private static void draw(RenderLevelStageEvent event, ShaderInstance shine) {
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
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        Matrix4f view = event.getModelViewMatrix();
        Vec3 camera = event.getCamera().getPosition();
        float clock = (minecraft.level == null ? 0.0F : minecraft.level.getGameTime() % 24000L)
                + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        shine.safeGetUniform("Projection").set(projection);
        shine.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        shine.safeGetUniform("Clock").set(clock);
        shine.safeGetUniform("Bound").set((float) BOUND);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        for (Hole hole : HOLES) {
            // Each reads the picture as the ones before it left it.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(false);
            shine.setSampler("SceneSampler", target.getColorTextureId());
            shine.setSampler("DepthSampler", target.getDepthTextureId());
            Vector3f center = view.transformPosition((float) (hole.center.x - camera.x),
                    (float) (hole.center.y - camera.y), (float) (hole.center.z - camera.z), new Vector3f());
            Vector3f axis = view.transformDirection((float) hole.axis.x, (float) hole.axis.y, (float) hole.axis.z,
                    new Vector3f()).normalize();
            shine.safeGetUniform("Center").set(center.x, center.y, center.z);
            shine.safeGetUniform("Horizon").set((float) hole.horizon);
            shine.safeGetUniform("Axis").set(axis.x, axis.y, axis.z);
            shine.safeGetUniform("Inner").set((float) hole.inner);
            shine.safeGetUniform("Outer").set((float) hole.outer);
            shine.safeGetUniform("Strength").set(hole.strength);
            shine.safeGetUniform("Collapse").set(hole.collapse);
            shine.safeGetUniform("Seed").set((float) Math.floorMod(hole.seed, 97));
            shine.safeGetUniform("Hot").set(channel(hole.hot, 16), channel(hole.hot, 8), channel(hole.hot, 0));
            shine.safeGetUniform("Cool").set(channel(hole.cool, 16), channel(hole.cool, 8), channel(hole.cool, 0));
            shine.safeGetUniform("Pull").set(hole.pull);
            shine.safeGetUniform("Wave").set((float) hole.wave);
            shine.safeGetUniform("WaveStrength").set(hole.waveStrength);
            RenderSystem.setShader(() -> shine);
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

    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255.0F;
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // A shader that will not build only takes the holes away (callers draw their own then), never the game.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "singularity");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name, DefaultVertexFormat.POSITION),
                    loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The singularity shader could not be built", e);
        }
    }
}
