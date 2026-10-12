package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Planet;
import org.joml.Matrix4f;

public final class CosmicSky {
    private static final float DISTANCE_SCALE = 0.5F;
    private static volatile Pictures painted;
    private static final List<SkyTexture> FACES = new ArrayList<>();
    private static final List<SkyTexture> PLANETS = new ArrayList<>();
    private static SkyTexture galaxy;
    private static SkyTexture sprites;
    private static VertexBuffer stars;
    private static boolean ready;
    private static boolean active;

    private record Pictures(NativeImage[] faces, NativeImage galaxy, NativeImage sprites, NativeImage[] planets,
            float[] stars) {
    }

    private CosmicSky() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofPlatform().daemon().name("cosmic-sky").start(() -> {
            try {
                long start = System.nanoTime();
                NativeImage sprites = StarField.sprites();
                float[] stars = StarField.build();
                NativeImage[] planets = new NativeImage[SkyBodies.PLANETS.size()];
                for (int i = 0; i < planets.length; i++) {
                    planets[i] = PlanetPainter.paint(SkyBodies.PLANETS.get(i));
                }
                NativeImage galaxy = GalaxyPainter.paint();
                NativeImage[] faces = new NativeImage[MilkyWay.FACES.length];
                for (int i = 0; i < faces.length; i++) {
                    faces[i] = MilkyWay.face(i);
                }
                painted = new Pictures(faces, galaxy, sprites, planets, stars);
                MultiversePowers.LOGGER.debug("Cosmic sky painted in {} ms", (System.nanoTime() - start) / 1_000_000L);
            } catch (RuntimeException e) {
                MultiversePowers.LOGGER.error("Could not paint the cosmic sky", e);
            }
        });
    }

    public static boolean replacesStars() {
        return active;
    }

    public static void draw(Matrix4f frustum, Matrix4f projection, float partialTick) {
        active = false;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || !ClientSettings.cosmicSky() || !upload()) {
            return;
        }
        active = true;
        float fade = Mth.clamp(level.getStarBrightness(partialTick) * 2.0F, 0.0F, 1.0F)
                * (1.0F - level.getRainLevel(partialTick));
        if (fade <= 0.002F) {
            return;
        }
        Matrix4f sky = new Matrix4f(frustum).rotateY((float) Math.toRadians(-90.0))
                .rotateX((float) Math.toRadians(level.getTimeOfDay(partialTick) * 360.0F)).scale(DISTANCE_SCALE);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(fade, fade, fade, fade);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        for (int i = 0; i < FACES.size(); i++) {
            Vec3[] face = MilkyWay.FACES[i];
            RenderSystem.setShaderTexture(0, FACES.get(i).getId());
            quad(sky, face[0].scale(100.0), face[1].scale(100.0), face[2].scale(-100.0));
        }
        Vec3[] frame = SkyBodies.frame(SkyBodies.GALAXY);
        double half = 100.0 * Math.tan(Math.toRadians(SkyBodies.GALAXY_DEGREES * 0.5));
        double c = Math.cos(SkyBodies.GALAXY_ROLL) * half;
        double s = Math.sin(SkyBodies.GALAXY_ROLL) * half;
        RenderSystem.setShaderTexture(0, galaxy.getId());
        quad(sky, SkyBodies.GALAXY.scale(100.0), frame[0].scale(c).add(frame[1].scale(s)),
                frame[1].scale(c).subtract(frame[0].scale(s)));
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, sprites.getId());
        stars.bind();
        stars.drawWithShader(sky, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // In twilight a world's dark side would show as a dark disc on the blue sky: it hides less than it shines.
        RenderSystem.setShaderColor(fade, fade, fade, fade * fade);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        for (int i = 0; i < PLANETS.size(); i++) {
            Planet planet = SkyBodies.PLANETS.get(i);
            Vec3[] f = SkyBodies.frame(planet.dir());
            double h = 100.0 * Math.tan(Math.toRadians(planet.degrees() * planet.reach() * 0.5));
            RenderSystem.setShaderTexture(0, PLANETS.get(i).getId());
            quad(sky, planet.dir().scale(100.0), f[0].scale(h), f[1].scale(h));
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
    }

    private static void quad(Matrix4f pose, Vec3 middle, Vec3 right, Vec3 up) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        vertex(builder, pose, middle.subtract(right).add(up), 0.0F, 0.0F);
        vertex(builder, pose, middle.add(right).add(up), 1.0F, 0.0F);
        vertex(builder, pose, middle.add(right).subtract(up), 1.0F, 1.0F);
        vertex(builder, pose, middle.subtract(right).subtract(up), 0.0F, 1.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void vertex(BufferBuilder builder, Matrix4f pose, Vec3 p, float u, float v) {
        builder.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(u, v);
    }

    private static boolean upload() {
        if (ready) {
            return true;
        }
        Pictures pictures = painted;
        if (pictures == null) {
            return false;
        }
        if (sprites == null) {
            sprites = new SkyTexture(pictures.sprites());
            stars = starBuffer(pictures.stars());
        } else if (PLANETS.size() < pictures.planets().length) {
            PLANETS.add(new SkyTexture(pictures.planets()[PLANETS.size()]));
        } else if (galaxy == null) {
            galaxy = new SkyTexture(pictures.galaxy());
        } else if (FACES.size() < pictures.faces().length) {
            FACES.add(new SkyTexture(pictures.faces()[FACES.size()]));
        } else {
            ready = true;
            painted = null;
        }
        return ready;
    }

    private static VertexBuffer starBuffer(float[] data) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < data.length; i += StarField.STRIDE) {
            builder.addVertex(data[i], data[i + 1], data[i + 2]).setUv(data[i + 3], data[i + 4])
                    .setColor(data[i + 5], data[i + 6], data[i + 7], 1.0F);
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
