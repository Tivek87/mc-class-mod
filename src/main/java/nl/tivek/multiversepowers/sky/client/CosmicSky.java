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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Planet;
import org.joml.Matrix4f;

// The cosmic night sky: on the game's own sky, whose colour, sun, moon and stars stay as they are, things of their own
// as the moon is: the Milky Way's band and its stars, nebulae, clusters, a far galaxy and planets, each a picture
// painted in code (never an image file) once on a background thread at client setup and uploaded one a frame. The
// light is added in the stars' frame before the sun and moon, fading with the night; the planets, which hide what is
// behind them, are laid on after the game's stars so none shines through them.
public final class CosmicSky {
    private static final float DISTANCE_SCALE = 0.5F;
    // How far below the horizon (the sun's height, 1 overhead) the sky is wholly out.
    private static final float SUNK = 0.2F;
    private static volatile Pictures painted;
    private static final List<SkyTexture> PLANETS = new ArrayList<>();
    private static final List<SkyTexture> NEBULAE = new ArrayList<>();
    private static final List<SkyTexture> CLUSTERS = new ArrayList<>();
    private static SkyTexture band;
    private static SkyTexture galaxy;
    private static VertexBuffer stars;
    private static boolean ready;
    // This frame's sky, kept for the planets laid on after the game's stars; null when they are not shown.
    private static Matrix4f shown;
    private static float shownFade;

    private record Pictures(NativeImage band, NativeImage galaxy, NativeImage[] planets, NativeImage[] nebulae,
            NativeImage[] clusters, float[] stars) {
    }

    private CosmicSky() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofPlatform().daemon().name("cosmic-sky").start(() -> {
            try {
                long start = System.nanoTime();
                float[] stars = StarField.build();
                NativeImage[] planets = new NativeImage[SkyBodies.PLANETS.size()];
                for (int i = 0; i < planets.length; i++) {
                    planets[i] = PlanetPainter.paint(SkyBodies.PLANETS.get(i));
                }
                NativeImage[] nebulae = new NativeImage[SkyBodies.NEBULAE.size()];
                for (int i = 0; i < nebulae.length; i++) {
                    nebulae[i] = MilkyWay.nebula(SkyBodies.NEBULAE.get(i));
                }
                NativeImage[] clusters = new NativeImage[SkyBodies.CLUSTERS.size()];
                for (int i = 0; i < clusters.length; i++) {
                    clusters[i] = MilkyWay.cluster(SkyBodies.CLUSTERS.get(i));
                }
                painted = new Pictures(MilkyWay.band(), GalaxyPainter.paint(), planets, nebulae, clusters, stars);
                MultiversePowers.LOGGER.debug("Cosmic sky painted in {} ms", (System.nanoTime() - start) / 1_000_000L);
            } catch (RuntimeException e) {
                MultiversePowers.LOGGER.error("Could not paint the cosmic sky", e);
            }
        });
    }

    public static void draw(Matrix4f frustum, Matrix4f projection, float partialTick) {
        shown = null;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                || !ClientSettings.cosmicSky() || !upload()) {
            return;
        }
        // Only once the sun is down, as deep as it sinks in twilight; never while it is up.
        float sunk = Mth.clamp(-Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) / SUNK, 0.0F, 1.0F);
        float fade = sunk * sunk * (3.0F - 2.0F * sunk) * (1.0F - level.getRainLevel(partialTick));
        if (fade <= 0.002F) {
            return;
        }
        Matrix4f sky = new Matrix4f(frustum).rotateY((float) Math.toRadians(-90.0))
                .rotateX((float) Math.toRadians(level.getTimeOfDay(partialTick) * 360.0F)).scale(DISTANCE_SCALE);
        shown = sky;
        shownFade = fade;
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(fade, fade, fade, fade);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, band.getId());
        band(sky);
        for (int i = 0; i < NEBULAE.size(); i++) {
            SkyBodies.Nebula nebula = SkyBodies.NEBULAE.get(i);
            picture(sky, NEBULAE.get(i), nebula.dir(), nebula.degrees() * MilkyWay.NEBULA_REACH, 0.0);
        }
        for (int i = 0; i < CLUSTERS.size(); i++) {
            SkyBodies.Cluster cluster = SkyBodies.CLUSTERS.get(i);
            picture(sky, CLUSTERS.get(i), cluster.dir(), cluster.degrees() * MilkyWay.CLUSTER_REACH, 0.0);
        }
        picture(sky, galaxy, SkyBodies.GALAXY, SkyBodies.GALAXY_DEGREES, SkyBodies.GALAXY_ROLL);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        stars.bind();
        stars.drawWithShader(sky, projection, RenderSystem.getShader());
        VertexBuffer.unbind();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
    }

    // The planets, right after the game's stars: each hides what is behind it.
    public static void planets() {
        Matrix4f sky = shown;
        if (sky == null) {
            return;
        }
        float fade = shownFade;
        float[] was = RenderSystem.getShaderColor().clone();
        ShaderInstance shader = RenderSystem.getShader();
        RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // In twilight a world's dark side would show as a dark disc on the blue sky: it hides less than it shines.
        RenderSystem.setShaderColor(fade, fade, fade, fade * fade);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        for (int i = 0; i < PLANETS.size(); i++) {
            Planet planet = SkyBodies.PLANETS.get(i);
            picture(sky, PLANETS.get(i), planet.dir(), planet.degrees() * planet.reach(), 0.0);
        }
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(was[0], was[1], was[2], was[3]);
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableCull();
    }

    // A square picture `degrees` wide round `dir`, its top towards the sun's side, turned by `roll`.
    private static void picture(Matrix4f sky, SkyTexture texture, Vec3 dir, double degrees, double roll) {
        Vec3[] frame = SkyBodies.frame(dir);
        double half = 100.0 * Math.tan(Math.toRadians(degrees * 0.5));
        double c = Math.cos(roll) * half;
        double s = Math.sin(roll) * half;
        RenderSystem.setShaderTexture(0, texture.getId());
        quad(sky, dir.scale(100.0), frame[0].scale(c).add(frame[1].scale(s)),
                frame[1].scale(c).subtract(frame[0].scale(s)));
    }

    // The Milky Way's strip round the sky, in pieces bent to the sphere.
    private static void band(Matrix4f sky) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        double half = Math.toRadians(MilkyWay.HALF);
        for (int i = 0; i < MilkyWay.PIECES; i++) {
            double lon0 = (2.0 * i / MilkyWay.PIECES - 1.0) * Math.PI;
            double lon1 = (2.0 * (i + 1) / MilkyWay.PIECES - 1.0) * Math.PI;
            float u0 = (float) i / MilkyWay.PIECES;
            float u1 = (float) (i + 1) / MilkyWay.PIECES;
            for (int r = 0; r < MilkyWay.ROWS; r++) {
                double lat0 = half * (1.0 - 2.0 * r / MilkyWay.ROWS);
                double lat1 = half * (1.0 - 2.0 * (r + 1) / MilkyWay.ROWS);
                float v0 = (float) r / MilkyWay.ROWS;
                float v1 = (float) (r + 1) / MilkyWay.ROWS;
                vertex(builder, sky, MilkyWay.along(lon0, lat0).scale(100.0), u0, v0);
                vertex(builder, sky, MilkyWay.along(lon1, lat0).scale(100.0), u1, v0);
                vertex(builder, sky, MilkyWay.along(lon1, lat1).scale(100.0), u1, v1);
                vertex(builder, sky, MilkyWay.along(lon0, lat1).scale(100.0), u0, v1);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
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
        if (stars == null) {
            stars = starBuffer(pictures.stars());
        } else if (PLANETS.size() < pictures.planets().length) {
            PLANETS.add(new SkyTexture(pictures.planets()[PLANETS.size()]));
        } else if (NEBULAE.size() < pictures.nebulae().length) {
            NEBULAE.add(new SkyTexture(pictures.nebulae()[NEBULAE.size()]));
        } else if (CLUSTERS.size() < pictures.clusters().length) {
            CLUSTERS.add(new SkyTexture(pictures.clusters()[CLUSTERS.size()]));
        } else if (galaxy == null) {
            galaxy = new SkyTexture(pictures.galaxy());
        } else if (band == null) {
            band = new SkyTexture(pictures.band());
        } else {
            ready = true;
            painted = null;
        }
        return ready;
    }

    private static VertexBuffer starBuffer(float[] data) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < data.length; i += StarField.STRIDE) {
            builder.addVertex(data[i], data[i + 1], data[i + 2]).setColor(data[i + 3], data[i + 4], data[i + 5],
                    1.0F);
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
