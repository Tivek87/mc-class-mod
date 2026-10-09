package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Glow that runs into a block or a creature fades out over its last stretch before the line where they meet, never cut
// off there in a hard line: its shader finds the surface behind each pixel in a copy of the depth, taken once a stage
// before the first painter with glow draws, so a construct's glow never fades against the constructs' own masses.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SoftGlow {
    // At most this many blocks of a wide glow fade out before the line where it runs into what lies behind it.
    private static final float SOFTNESS = 1.0F;
    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static RenderTarget copy;
    private static boolean inLevel;
    private static boolean stale = true;

    static final RenderStateShard.ShaderStateShard SHADER = new RenderStateShard.ShaderStateShard(
            () -> shader != null ? shader : GameRenderer.getRendertypeLightningShader());

    private SoftGlow() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onStageStart(RenderLevelStageEvent event) {
        stale = true;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            inLevel = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onStageEnd(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            inLevel = false;
        }
    }

    // Before a painter with glow draws anything: the first since the stage began copies the depth drawn so far.
    static void before() {
        if (!inLevel || !stale || shader == null) {
            return;
        }
        stale = false;
        // Making or resizing the copy binds and unbinds framebuffers of its own: whatever is drawn into goes back after.
        int bound = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget target = copy;
        if (target == null) {
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            copy = target;
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        if (main.isStencilEnabled() && !target.isStencilEnabled()) {
            target.enableStencil();
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, bound);
    }

    // Just before a glow layer draws: where to read the depth and how far to fade; outside the world nothing fades.
    static void prepare() {
        ShaderInstance glow = shader;
        if (glow == null) {
            return;
        }
        RenderTarget target = copy;
        boolean soft = inLevel && target != null;
        glow.safeGetUniform("Softness").set(soft ? SOFTNESS : 0.0F);
        if (soft) {
            glow.setSampler("DepthSampler", target.getDepthTextureId());
            glow.safeGetUniform("InverseProjection").set(new Matrix4f(RenderSystem.getProjectionMatrix()).invert());
            glow.safeGetUniform("SceneSize").set((float) target.width, (float) target.height);
        }
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // A shader that will not build only leaves glow cut off where it meets a block, never takes the game down.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "soft_glow");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name,
                    DefaultVertexFormat.POSITION_COLOR), loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The soft glow shader could not be built", e);
        }
    }
}
