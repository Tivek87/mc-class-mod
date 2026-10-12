package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.sky.SkyMoon;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Kind;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Planet;
import org.joml.Matrix4f;

// Everything solid in the cosmic sky is a cube: the sun, the moon, every planet (two with a square ring), the moons
// going round them, a belt of tumbling rocks and a comet. Each is drawn from the eye in the sky's own frame, the
// furthest first, only the faces turned to the eye, each face lit by where the sun stands; the moon's faces by its
// phase, and it always keeps the same ones to the world. Their glows, the comet's tail and the shooting stars are
// light added before them.
final class SkyCubes {
    static final double FAR = 100.0;
    private static final double SUN_HALF = 8.5;
    // Corners by their bits (x lowest, then y, then z) and each face's four, as TexturedBox has them.
    private static final int[][] FACES = { { 0, 1, 3, 2 }, { 4, 5, 7, 6 }, { 0, 4, 6, 2 }, { 1, 5, 7, 3 },
            { 0, 1, 5, 4 }, { 2, 3, 7, 6 } };
    private static final int[] AXIS = { 2, 2, 0, 0, 1, 1 };
    private static final int[] SIGN = { -1, 1, -1, 1, -1, 1 };
    private static final float AMBIENT = 0.22F;
    private static final float MOON_AMBIENT = 0.1F;
    private static final int RING_PIECES = 6;
    private static final Vec3 SUN_POLE = new Vec3(0.5, 0.6, 0.62).normalize();
    private static final Vec3 SKY_X = new Vec3(1.0, 0.0, 0.0);

    static final int SUN_ROW = 0;
    static final int MOON_ROW = 1;
    private static final int PLANET_ROW = 2;
    private static final int[] RING_ROW = new int[SkyBodies.PLANETS.size()];
    static final int ROWS;
    private static final int[] ROCK_ROWS;

    private static final Vec3[][] PLANET_AXES = new Vec3[SkyBodies.PLANETS.size()][];
    private static final List<Rock> ROCKS = new ArrayList<>();
    private static double struckAt = Double.NEGATIVE_INFINITY;

    private record Rock(Vec3 dir, double half, Vec3 pole, double spin, int row) {
    }

    private record Ring(double in, double out, int row) {
    }

    private record Body(Vec3 at, Vec3[] axes, double half, int row, float alpha, float shine, @Nullable Vec3 light,
            float ambient, @Nullable Ring ring) {
    }

    private record Glow(Vec3 at, Vec3 right, Vec3 up, int rgb, float alpha, boolean streak) {
    }

    static {
        int row = PLANET_ROW + SkyBodies.PLANETS.size();
        List<Integer> rocks = new ArrayList<>();
        for (int i = 0; i < SkyBodies.PLANETS.size(); i++) {
            Planet planet = SkyBodies.PLANETS.get(i);
            RING_ROW[i] = planet.ringed() ? row++ : -1;
            Kind kind = planet.kind();
            if (kind == Kind.MOON || kind == Kind.ROCK || kind == Kind.ICE_MOON || kind == Kind.LAVA) {
                rocks.add(PLANET_ROW + i);
            }
            PLANET_AXES[i] = planetAxes(planet);
        }
        ROWS = row;
        ROCK_ROWS = rocks.stream().mapToInt(Integer::intValue).toArray();
        SplittableRandom random = new SplittableRandom(0xA57E201DL);
        Vec3 from = SkyBodies.BELT_FROM;
        Vec3 across = SkyBodies.BELT_FROM.cross(SkyBodies.BELT_TO).normalize();
        Vec3 along = across.cross(from);
        for (int i = 0; i < SkyBodies.BELT_ROCKS; i++) {
            double a = random.nextDouble() * SkyBodies.BELT_ARC;
            double off = random.nextGaussian() * 0.035;
            Vec3 dir = from.scale(Math.cos(a)).add(along.scale(Math.sin(a))).scale(Math.cos(off))
                    .add(across.scale(Math.sin(off))).normalize();
            Vec3 pole = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            double half = 0.22 + 0.75 * Math.pow(random.nextDouble(), 2.5);
            ROCKS.add(new Rock(dir, half, pole, (0.01 + 0.03 * random.nextDouble()) * (random.nextBoolean() ? 1 : -1),
                    ROCK_ROWS[random.nextInt(ROCK_ROWS.length)]));
        }
    }

    private SkyCubes() {
    }

    // Every skin, in the atlas's rows: painted once, off the render thread.
    static NativeImage paint() {
        List<CubePainter.Skin> skins = new ArrayList<>();
        skins.add(CubePainter.sun());
        skins.add(CubePainter.globe(Kind.MOON, 7L, 40.0));
        for (Planet planet : SkyBodies.PLANETS) {
            skins.add(CubePainter.globe(planet.kind(), planet.seed(), planet.degrees()));
        }
        for (Planet planet : SkyBodies.PLANETS) {
            if (planet.ringed()) {
                skins.add(CubePainter.ring(planet.kind() == Kind.SATURN ? 0xE8D6A8 : 0x9CE8E4, planet.seed()));
            }
        }
        return CubePainter.atlas(skins);
    }

    // The moon rings as something strikes it: it rocks on its place and comes to rest.
    static void strike(double now) {
        struckAt = now;
    }

    static float[][] moonSquares() {
        float[][] squares = new float[CubePainter.FACES][];
        for (int face = 0; face < squares.length; face++) {
            squares[face] = CubePainter.square(MOON_ROW, ROWS, face);
        }
        return squares;
    }

    // How bright each of the moon's faces stands in the sky now, by its phase.
    static float[] moonShades(ClientLevel level) {
        Vec3[] axes = moonAxes(Double.NaN);
        Vec3 light = moonLight(level);
        float[] shades = new float[CubePainter.FACES];
        for (int face = 0; face < shades.length; face++) {
            shades[face] = lit(axes[AXIS[face]].scale(SIGN[face]), light, MOON_AMBIENT);
        }
        return shades;
    }

    private static Vec3 moonLight(ClientLevel level) {
        double phase = level.getMoonPhase() * Math.PI / 4.0;
        return new Vec3(0.0, Math.cos(phase), Math.sin(phase));
    }

    private static Vec3[] moonAxes(double time) {
        Vec3[] axes = SkyMoon.tilted(new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, -1.0, 0.0));
        double since = time - struckAt;
        if (!Double.isNaN(time) && since >= 0.0 && since < 200.0) {
            double rock = 0.28 * Math.exp(-since / 28.0) * Math.sin(since * 0.42);
            for (int i = 0; i < axes.length; i++) {
                axes[i] = about(axes[i], SKY_X, rock);
            }
        }
        return axes;
    }

    private static float lit(Vec3 normal, Vec3 light, float ambient) {
        return ambient + (1.0F - ambient) * (float) Math.max(0.0, normal.dot(light));
    }

    // A planet's own axes in the sky's frame: its pole from its roll and tilt as it is seen, the others square to it.
    private static Vec3[] planetAxes(Planet planet) {
        Vec3[] frame = SkyBodies.frame(planet.dir());
        double r = planet.roll();
        double t = planet.tilt() + 0.45;
        Vec3 pole = frame[0].scale(-Math.sin(r) * Math.cos(t)).add(frame[1].scale(Math.cos(r) * Math.cos(t)))
                .subtract(planet.dir().scale(Math.sin(t))).normalize();
        return poled(pole);
    }

    private static Vec3[] poled(Vec3 pole) {
        Vec3 side = Math.abs(pole.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : SKY_X;
        Vec3 x = side.cross(pole).normalize();
        return new Vec3[] { x, pole, x.cross(pole) };
    }

    // `axes` turned about their own pole (their second) by `angle`.
    private static Vec3[] spun(Vec3[] axes, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3[] { axes[0].scale(c).add(axes[2].scale(s)), axes[1], axes[2].scale(c).subtract(
                axes[0].scale(s)) };
    }

    // `v` turned about the unit `axis` by `angle`.
    private static Vec3 about(Vec3 v, Vec3 axis, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return v.scale(c).add(axis.cross(v).scale(s)).add(axis.scale(axis.dot(v) * (1.0 - c)));
    }

    // `night`: how far the night's own things are out, 0 to 1; the sun and moon only fade with the rain.
    static void draw(Matrix4f sky, ClientLevel level, float partialTick, float night, int atlas, int glow) {
        double time = level.getGameTime() + (double) partialTick;
        float clear = 1.0F - level.getRainLevel(partialTick);
        List<Body> bodies = new ArrayList<>();
        List<Glow> glows = new ArrayList<>();
        Vec3 sun = SkyBodies.SUN.scale(FAR);
        bodies.add(new Body(sun, spun(poled(SUN_POLE), time * 0.0016), SUN_HALF, SUN_ROW, clear, 1.0F, null, 0.0F,
                null));
        if (!LocalSky.moonHidden(level)) {
            Body moon = moon(level, time, clear);
            float full = 0.3F + 0.7F * (float) (0.5 + 0.5 * moon.light().y);
            bodies.add(moon);
            glows.add(halo(moon.at(), moon.half() * 3.0, 0xBFD0FF, 0.3F * full * clear * (0.35F + 0.65F * night)));
        }
        if (night > 0.002F) {
            ofTheNight(time, night, bodies, glows);
        }
        if (!glows.isEmpty()) {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderTexture(0, glow);
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_TEX_COLOR);
            for (Glow one : glows) {
                glow(builder, sky, one);
            }
            flush(builder);
        }
        bodies.sort(Comparator.comparingDouble((Body body) -> -body.at().lengthSqr()));
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, atlas);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Body body : bodies) {
            if (body.ring() != null) {
                ring(builder, sky, body, true);
            }
            cube(builder, sky, body);
            if (body.ring() != null) {
                ring(builder, sky, body, false);
            }
        }
        flush(builder);
    }

    private static Body moon(ClientLevel level, double time, float alpha) {
        return new Body(SkyBodies.SUN.scale(-FAR), moonAxes(time), SkyMoon.half() * FAR, MOON_ROW, alpha, 1.0F,
                moonLight(level), MOON_AMBIENT, null);
    }

    // The moon alone, once more, with whatever depth test is set: over something drawn after the sky that stands
    // behind it. `pose` turns as the sky does, the eye at its origin.
    static void moonAlone(Matrix4f pose, ClientLevel level, float partialTick, int atlas) {
        if (LocalSky.moonHidden(level)) {
            return;
        }
        RenderSystem.setShaderTexture(0, atlas);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        cube(builder, pose, moon(level, level.getGameTime() + (double) partialTick,
                1.0F - level.getRainLevel(partialTick)));
        flush(builder);
    }

    // What only the night shows: the planets and their moons, the belt, the comet, a shooting star.
    private static void ofTheNight(double time, float night, List<Body> bodies, List<Glow> glows) {
        // In twilight a world's dark side would show dark on the blue sky: it hides less than it shines.
        float alpha = night * night;
        Vec3 light = SkyBodies.SUN;
        for (int i = 0; i < SkyBodies.PLANETS.size(); i++) {
            Planet planet = SkyBodies.PLANETS.get(i);
            Vec3 at = planet.dir().scale(FAR);
            double half = FAR * Math.tan(Math.toRadians(planet.degrees() * 0.5)) * 0.5;
            Vec3[] axes = spun(PLANET_AXES[i], time * (0.004 + 0.0025 * (planet.seed() % 5)) / (1.0 + half * 0.25));
            Ring ring = planet.ringed() ? new Ring(planet.ringIn() * 1.25, planet.ringOut() * 1.25, RING_ROW[i])
                    : null;
            bodies.add(new Body(at, axes, half, PLANET_ROW + i, alpha, night, light, AMBIENT, ring));
            double air = Surfaces.air(planet.kind());
            if (air > 0.0) {
                glows.add(halo(at, half * 2.4, Surfaces.airColor(planet.kind()), (float) (0.3 * air) * night));
            }
            for (SkyBodies.Moon moon : SkyBodies.MOONS) {
                if (moon.planet() == i) {
                    double turn = moon.phase() + time * Math.PI * 2.0 / moon.period();
                    Vec3 out = PLANET_AXES[i][0].scale(Math.cos(turn)).add(PLANET_AXES[i][2].scale(Math.sin(turn)));
                    Vec3 where = at.add(out.scale(half * moon.away()));
                    bodies.add(new Body(where, spun(poled(out), turn * 3.0), half * moon.size(),
                            ROCK_ROWS[moon.skin() % ROCK_ROWS.length], alpha, night, light, AMBIENT, null));
                }
            }
        }
        for (Rock rock : ROCKS) {
            bodies.add(new Body(rock.dir().scale(FAR), spun(poled(rock.pole()), time * rock.spin()), rock.half(),
                    rock.row(), alpha, night, light, AMBIENT, null));
        }
        comet(time, night, alpha, bodies, glows);
        shootingStar(time, night, glows);
    }

    // The comet: an icy cube going slowly round the sky, its two tails of light blown away from the sun.
    private static void comet(double time, float night, float alpha, List<Body> bodies, List<Glow> glows) {
        Vec3 from = SkyBodies.COMET_FROM;
        Vec3 along = SkyBodies.COMET_ACROSS.cross(from).normalize();
        double turn = time * Math.PI * 2.0 / SkyBodies.COMET_PERIOD;
        Vec3 dir = from.scale(Math.cos(turn)).add(along.scale(Math.sin(turn))).normalize();
        Vec3 at = dir.scale(FAR);
        Vec3 away = at.subtract(SkyBodies.SUN.scale(FAR * 4.0));
        away = away.subtract(dir.scale(away.dot(dir))).normalize();
        Vec3 side = away.cross(dir).normalize();
        bodies.add(new Body(at, spun(poled(side), time * 0.02), 1.1, ROCK_ROWS[2 % ROCK_ROWS.length], alpha, night,
                SkyBodies.SUN, 0.45F, null));
        glows.add(halo(at, 3.4, 0xBFE8FF, 0.55F * night));
        glows.add(new Glow(at, away.scale(22.0), side.scale(2.6), 0xA8DCFF, 0.5F * night, true));
        glows.add(new Glow(at, away.add(side.scale(0.28)).normalize().scale(15.0), side.scale(1.4), 0xFFF0D0,
                0.35F * night, true));
    }

    // Now and then a shooting star: a streak of light for a blink, where and which way its own moment says.
    private static void shootingStar(double time, float night, List<Glow> glows) {
        long slot = (long) Math.floor(time / SkyBodies.SHOOTING_EVERY);
        SplittableRandom random = new SplittableRandom(slot * 0x9E3779B97F4A7C15L + 77L);
        double start = random.nextDouble() * (SkyBodies.SHOOTING_EVERY - SkyBodies.SHOOTING_TICKS);
        double age = time - slot * SkyBodies.SHOOTING_EVERY - start;
        if (random.nextDouble() > 0.7 || age < 0.0 || age > SkyBodies.SHOOTING_TICKS) {
            return;
        }
        Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
        Vec3[] frame = SkyBodies.frame(dir);
        double roll = random.nextDouble() * Math.PI * 2.0;
        Vec3 way = frame[0].scale(Math.cos(roll)).add(frame[1].scale(Math.sin(roll)));
        double u = age / SkyBodies.SHOOTING_TICKS;
        Vec3 at = dir.add(way.scale(Math.tan(Math.toRadians(22.0)) * (u - 0.5))).normalize().scale(FAR);
        float fade = (float) Math.sin(Math.PI * u);
        glows.add(new Glow(at, way.scale(-9.0 * (0.4 + 0.6 * fade)), way.cross(dir).scale(0.45), 0xF4F8FF,
                0.9F * fade * night, true));
        glows.add(halo(at, 0.9, 0xFFFFFF, fade * night));
    }

    // A round glow facing the eye, `size` from its middle to its edge.
    private static Glow halo(Vec3 at, double size, int rgb, float alpha) {
        Vec3[] frame = SkyBodies.frame(at.normalize());
        return new Glow(at, frame[0].scale(size), frame[1].scale(size), rgb, alpha, false);
    }

    private static void glow(BufferBuilder builder, Matrix4f sky, Glow glow) {
        if (glow.alpha() <= 0.003F) {
            return;
        }
        float r = (float) SkyColors.red(glow.rgb());
        float g = (float) SkyColors.green(glow.rgb());
        float b = (float) SkyColors.blue(glow.rgb());
        Vec3 at = glow.at();
        if (glow.streak()) {
            // From its head (the glow's middle) along `right` to nothing, `up` to either side.
            Vec3 end = at.add(glow.right());
            vertex(builder, sky, at.add(glow.up()), 0.5F, 0.0F, r, g, b, glow.alpha());
            vertex(builder, sky, end.add(glow.up()), 1.0F, 0.0F, r, g, b, glow.alpha());
            vertex(builder, sky, end.subtract(glow.up()), 1.0F, 1.0F, r, g, b, glow.alpha());
            vertex(builder, sky, at.subtract(glow.up()), 0.5F, 1.0F, r, g, b, glow.alpha());
            return;
        }
        vertex(builder, sky, at.subtract(glow.right()).add(glow.up()), 0.0F, 0.0F, r, g, b, glow.alpha());
        vertex(builder, sky, at.add(glow.right()).add(glow.up()), 1.0F, 0.0F, r, g, b, glow.alpha());
        vertex(builder, sky, at.add(glow.right()).subtract(glow.up()), 1.0F, 1.0F, r, g, b, glow.alpha());
        vertex(builder, sky, at.subtract(glow.right()).subtract(glow.up()), 0.0F, 1.0F, r, g, b, glow.alpha());
    }

    private static void cube(BufferBuilder builder, Matrix4f sky, Body body) {
        if (body.alpha() <= 0.003F) {
            return;
        }
        Vec3[] axes = body.axes();
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = body.at().add(axes[0].scale(((i & 1) * 2 - 1) * body.half()))
                    .add(axes[1].scale(((i >> 1 & 1) * 2 - 1) * body.half()))
                    .add(axes[2].scale(((i >> 2 & 1) * 2 - 1) * body.half()));
        }
        for (int face = 0; face < FACES.length; face++) {
            Vec3 normal = axes[AXIS[face]].scale(SIGN[face]);
            if (normal.dot(body.at().add(normal.scale(body.half()))) >= 0.0) {
                continue;
            }
            // The sun gives its own light: its faces differ only so its edges show.
            float shade = body.light() == null ? 0.84F + 0.16F * (float) Math.abs(normal.dot(SUN_POLE))
                    : lit(normal, body.light(), body.ambient());
            shade *= body.shine();
            float[] square = CubePainter.square(body.row(), ROWS, face);
            int[] at = FACES[face];
            vertex(builder, sky, corners[at[0]], square[0], square[1], shade, shade, shade, body.alpha());
            vertex(builder, sky, corners[at[1]], square[2], square[1], shade, shade, shade, body.alpha());
            vertex(builder, sky, corners[at[2]], square[2], square[3], shade, shade, shade, body.alpha());
            vertex(builder, sky, corners[at[3]], square[0], square[3], shade, shade, shade, body.alpha());
        }
    }

    // A square ring round the cube's middle, flat across its pole: the pieces behind the cube, or those before it.
    private static void ring(BufferBuilder builder, Matrix4f sky, Body body, boolean behind) {
        Ring ring = body.ring();
        float[] square = CubePainter.square(ring.row(), ROWS, 0);
        float shade = (0.55F + 0.45F * (float) Math.abs(body.axes()[1].dot(SkyBodies.SUN))) * body.shine();
        double[][] corner = { { -1.0, -1.0 }, { 1.0, -1.0 }, { 1.0, 1.0 }, { -1.0, 1.0 } };
        for (int side = 0; side < 4; side++) {
            double[] a = corner[side];
            double[] b = corner[(side + 1) % 4];
            for (int piece = 0; piece < RING_PIECES; piece++) {
                double t0 = (double) piece / RING_PIECES;
                double t1 = (double) (piece + 1) / RING_PIECES;
                Vec3 from = flat(body, a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0);
                Vec3 to = flat(body, a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1);
                Vec3 middle = from.add(to).scale(0.5);
                if (middle.dot(body.at()) > 0.0 != behind) {
                    continue;
                }
                double in = ring.in() * body.half();
                double out = ring.out() * body.half();
                vertex(builder, sky, body.at().add(from.scale(in)), square[0], square[1], shade, shade, shade,
                        body.alpha());
                vertex(builder, sky, body.at().add(from.scale(out)), square[2], square[1], shade, shade, shade,
                        body.alpha());
                vertex(builder, sky, body.at().add(to.scale(out)), square[2], square[3], shade, shade, shade,
                        body.alpha());
                vertex(builder, sky, body.at().add(to.scale(in)), square[0], square[3], shade, shade, shade,
                        body.alpha());
            }
        }
    }

    // A point of the ring's own plane, across the cube's pole.
    private static Vec3 flat(Body body, double x, double z) {
        return body.axes()[0].scale(x).add(body.axes()[2].scale(z));
    }

    private static void vertex(BufferBuilder builder, Matrix4f sky, Vec3 p, float u, float v, float r, float g,
            float b, float a) {
        builder.addVertex(sky, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(r, g, b, a);
    }

    private static void flush(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            BufferUploader.drawWithShader(mesh);
        }
    }
}
