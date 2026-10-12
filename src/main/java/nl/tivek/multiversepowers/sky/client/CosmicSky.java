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
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.sky.SkyMoon;
import nl.tivek.multiversepowers.space.Space;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

// The cosmic sky: on the game's own sky, whose colour and stars stay as they are, things of their own: by night the
// Milky Way's band and its stars, nebulae, clusters and a far galaxy, each a picture whose light is added, and before
// them all the sky's cubes (SkyCubes): the sun and the moon in place of the game's flat ones, by day too, and by night
// the planets, their moons, a belt of rocks and a comet. Every picture is painted in code (never an image file) once
// on a background thread at client setup and uploaded one a frame; until all are up the game's own sun and moon show.
// Drawn right after the game's stars in the stars' frame, the cubes last, so nothing shines through them.
public final class CosmicSky {
    private static final float DISTANCE_SCALE = 0.5F;
    // How far below the horizon (the sun's height, 1 overhead) the sky is wholly out.
    private static final float SUNK = 0.2F;
    private static final int GLOW = 32;
    private static final ResourceLocation CUBES = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "sky/cubes");
    private static volatile Pictures painted;
    private static final List<SkyTexture> NEBULAE = new ArrayList<>();
    private static final List<SkyTexture> CLUSTERS = new ArrayList<>();
    private static SkyTexture band;
    private static SkyTexture galaxy;
    private static SkyTexture cubes;
    private static SkyTexture near;
    private static SkyTexture glow;
    private static VertexBuffer stars;
    private static boolean ready;

    private record Pictures(NativeImage band, NativeImage galaxy, NativeImage cubes, NativeImage near,
            NativeImage glow, NativeImage[] nebulae, NativeImage[] clusters, float[] stars) {
    }

    private CosmicSky() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofPlatform().daemon().name("cosmic-sky").start(() -> {
            try {
                long start = System.nanoTime();
                float[] stars = StarField.build();
                NativeImage[] nebulae = new NativeImage[SkyBodies.NEBULAE.size()];
                for (int i = 0; i < nebulae.length; i++) {
                    nebulae[i] = MilkyWay.nebula(SkyBodies.NEBULAE.get(i));
                }
                NativeImage[] clusters = new NativeImage[SkyBodies.CLUSTERS.size()];
                for (int i = 0; i < clusters.length; i++) {
                    clusters[i] = MilkyWay.cluster(SkyBodies.CLUSTERS.get(i));
                }
                painted = new Pictures(MilkyWay.band(), GalaxyPainter.paint(), SkyCubes.paint(),
                        SkyCubes.paintNear(), glow(), nebulae, clusters, stars);
                MultiversePowers.LOGGER.debug("Cosmic sky painted in {} ms", (System.nanoTime() - start) / 1_000_000L);
            } catch (RuntimeException e) {
                MultiversePowers.LOGGER.error("Could not paint the cosmic sky", e);
            }
        });
    }

    // A soft round glow, white, fading from its middle to nothing at its edge.
    private static NativeImage glow() {
        NativeImage image = new NativeImage(GLOW, GLOW, false);
        for (int y = 0; y < GLOW; y++) {
            for (int x = 0; x < GLOW; x++) {
                double r = Math.hypot(x + 0.5 - GLOW * 0.5, y + 0.5 - GLOW * 0.5) / (GLOW * 0.5);
                image.setPixelRGBA(x, y, SkyColors.channel(Math.pow(Math.max(0.0, 1.0 - r), 2.2)) << 24 | 0xFFFFFF);
            }
        }
        return image;
    }

    // Whether this sky's own sun and moon are drawn now, in place of the game's.
    public static boolean bodies() {
        ClientLevel level = Minecraft.getInstance().level;
        return ready && level != null && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL
                && ClientSettings.cosmicSky();
    }

    // The moon's cube, for what takes it out of the sky: the picture its faces are in (null while the game's own
    // moon shows), each face's square of it and how bright each stands now.
    @Nullable
    public static ResourceLocation moonTexture() {
        return bodies() ? CUBES : null;
    }

    public static float[][] moonSquares() {
        return SkyCubes.moonSquares();
    }

    public static float[] moonShades(ClientLevel level) {
        return SkyCubes.moonShades(level);
    }

    // Something struck the moon at this game time: its cube rocks where it stands and comes to rest.
    public static void strikeMoon(double gameTime) {
        SkyCubes.strike(gameTime);
    }

    // The moon's cube once more, with depth: for what is drawn after the sky and stands partly behind the moon, so
    // the moon hides that part. `eye` holds the eye at its origin, the world's ways along its axes, at the scale the
    // moon stands 100 away in.
    public static void moonOver(Matrix4f eye, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!bodies()) {
            return;
        }
        Matrix4f sky = sky(eye, level, partialTick, SkyWay.now(level, partialTick));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyCubes.moonAlone(sky, level, partialTick, cubes.getId());
    }

    public static void draw(Matrix4f frustum, Matrix4f projection, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        upload();
        boolean on = bodies();
        SkyMoon.cube(on);
        if (!on) {
            return;
        }
        SkyWay way = SkyWay.now(level, partialTick);
        float thin = (float) way.thin();
        // The night's own only once the sun is down, as deep as it sinks in twilight; never while it is up. Where the
        // air is gone they are out by day too, and no rain hides them.
        float sunk = Mth.clamp(-Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) / SUNK, 0.0F, 1.0F);
        float fade = Math.max(thin, sunk * sunk * (3.0F - 2.0F * sunk) * (1.0F - level.getRainLevel(partialTick)
                * (1.0F - thin))) * LocalSky.stars(level, 1.0F);
        Matrix4f sky = sky(frustum, level, partialTick, way).scale(DISTANCE_SCALE);
        RenderSystem.disableCull();
        if (way.black() > 0.002) {
            space(sky, (float) way.black());
        }
        if (fade > 0.002F) {
            // Added light shows pale on a sky still blue: it comes later in the dusk than the cubes do.
            lights(sky, projection, fade * fade);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        SkyCubes.draw(sky, level, partialTick, fade, way, cubes.getId(), glow.getId(), near.getId());
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.enableCull();
    }

    // The sky's frame from the eye's: the stars' own turn through the day, then the eye's way round the world below.
    private static Matrix4f sky(Matrix4f eye, ClientLevel level, float partialTick, SkyWay way) {
        return new Matrix4f(eye).rotate(turn(level, partialTick)).rotate(way.orbit());
    }

    // The stars' own turn through the day, from the world's axes to the sky's. The moon's world lies turned over
    // about the x axis to the one below it (Space), so its sky is the very same one, seen from there.
    static Quaternionf turn(ClientLevel level, float partialTick) {
        Quaternionf turn = new Quaternionf();
        if (Space.onMoon(level)) {
            turn.rotateX((float) Math.PI);
        }
        return turn.rotateY((float) Math.toRadians(-90.0)).rotateX(hour(level, partialTick) * Mth.TWO_PI);
    }

    // How far the sky has turned, 0 to 1 (0 at noon): on the moon too by the clock of the world below it, as the
    // moon's own runs half a day on and the game's hours are not all as long.
    static float hour(ClientLevel level, float partialTick) {
        return Space.onMoon(level) ? level.dimensionType().timeOfDay(level.getLevelData().getDayTime())
                : level.getTimeOfDay(partialTick);
    }

    // Black all round, over the game's own sky, its stars and its haze at the horizon.
    private static void space(Matrix4f sky, float share) {
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        float far = 90.0F;
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int corner = 0; corner < 4; corner++) {
                    float a = corner == 0 || corner == 3 ? -far : far;
                    float b = corner < 2 ? -far : far;
                    float c = side * far;
                    builder.addVertex(sky, axis == 0 ? c : a, axis == 1 ? c : axis == 0 ? a : b, axis == 2 ? c : b)
                            .setColor(0.0F, 0.0F, 0.0F, share);
                }
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    // The night's light, only added to what is behind it: the band, the nebulae, the clusters, the galaxy, the stars.
    private static void lights(Matrix4f sky, Matrix4f projection, float fade) {
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
        } else if (cubes == null) {
            cubes = new SkyTexture(pictures.cubes());
            Minecraft.getInstance().getTextureManager().register(CUBES, cubes);
        } else if (near == null) {
            near = new SkyTexture(pictures.near());
        } else if (glow == null) {
            glow = new SkyTexture(pictures.glow(), true);
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
