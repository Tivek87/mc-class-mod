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
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.sky.SkyMoon;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Kind;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Planet;
import nl.tivek.multiversepowers.space.Space;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private static final int EARTH_ROW = 2;
    private static final int PLANET_ROW = 3;
    // Sky units to a block on the way to the moon, and the pictures of the moon's and the Earth's near faces, side
    // by side in one picture, for when they fill the view.
    private static final double UNIT = FAR / (Space.DISTANCE + Space.MOON_HALF);
    private static final int NEAR = 128;
    // The Earth's dark side: dark from near it, but from the moon it always reads as the Earth.
    private static final float EARTH_AMBIENT = 0.28F;
    private static final float FAR_AMBIENT = 0.55F;
    // Across a face, where its haze goes from the rim's to the middle's.
    private static final double[] VEIL = { 0.0, 0.3, 0.7, 1.0 };
    private static final float NEAR_AMBIENT = 0.3F;
    private static final int[] RING_ROW = new int[SkyBodies.PLANETS.size()];
    static final int ROWS;
    private static final int[] ROCK_ROWS;

    private static final CubePainter.Skin MOON = CubePainter.globe(Kind.MOON, 7L, 40.0);
    private static final CubePainter.Skin EARTH = CubePainter.globe(Kind.EARTH, 23L, 40.0);
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
        skins.add(MOON);
        skins.add(EARTH);
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

    // The moon's and the Earth's six faces, painted finer than the sky's own cubes: a row each.
    static NativeImage paintNear() {
        NativeImage image = new NativeImage(NEAR * CubePainter.FACES, NEAR * 2, true);
        IntStream.range(0, CubePainter.FACES * 2).parallel().forEach(n -> CubePainter.face(image,
                n % CubePainter.FACES * NEAR, n / CubePainter.FACES * NEAR, NEAR, n < CubePainter.FACES ? MOON : EARTH,
                n % CubePainter.FACES));
        return image;
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
        Vec3[] axes = moonAxes(Double.NaN, 1.0);
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

    // `tilt`: 1 as the sky shows it, three faces to the eye; 0 with one flat face to it.
    private static Vec3[] moonAxes(double time, double tilt) {
        Vec3[] axes = SkyMoon.tilted(new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, -1.0, 0.0),
                tilt);
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
    // `way`: where the eye is on the way to the moon; `near`: the picture of the two near faces.
    static void draw(Matrix4f sky, ClientLevel level, float partialTick, float night, SkyWay way, int atlas,
            int glow, int near) {
        double time = level.getGameTime() + (double) partialTick;
        // Above the clouds no rain dims them.
        float clear = 1.0F - level.getRainLevel(partialTick) * (float) (1.0 - way.thin());
        List<Body> bodies = new ArrayList<>();
        List<Glow> glows = new ArrayList<>();
        Vec3 sun = SkyBodies.SUN.scale(FAR);
        bodies.add(new Body(sun, spun(poled(SUN_POLE), time * 0.0016), SUN_HALF, SUN_ROW, clear, 1.0F, null, 0.0F,
                null));
        Body moon = LocalSky.moonHidden(level) ? null : moon(level, time, clear, way);
        if (moon != null) {
            float full = 0.3F + 0.7F * (float) (0.5 + 0.5 * moonLight(level).y);
            bodies.add(moon);
            glows.add(halo(moon.at(), moon.half() * 3.0, 0xBFD0FF, 0.3F * full * clear * (0.35F + 0.65F * night)
                    * (float) (1.0 - way.swing())));
        }
        Body earth = way.earthShown() > 0.0 ? earth(way) : null;
        if (earth != null) {
            bodies.add(earth);
            glows.add(halo(earth.at(), earth.half() * 2.2, Surfaces.airColor(Kind.EARTH),
                    0.35F * (float) way.earthTilt()));
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
        Body below = way.onMoon() ? moon : earth;
        RenderSystem.setShaderTexture(0, near);
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        if (moon != null) {
            near(builder, sky, moon, 0, Space.MOON_HALF / way.moonFace());
        }
        if (earth != null) {
            near(builder, sky, earth, 1, Space.EARTH_HALF / Math.max(1.0, way.earthFace()));
        }
        flush(builder);
        if (below != null && Math.max(way.haze(), way.rim()) > 0.003) {
            veil(sky, below, (float) way.haze(), (float) way.rim());
        }
    }

    // The moon: from its place in the sky it swings, as the eye climbs the way to it, to straight ahead on that way,
    // as near as it then is, one face square to the eye, the sun's own light on it.
    private static Body moon(ClientLevel level, double time, float alpha, SkyWay way) {
        Vec3[] axes = moonAxes(time, SkyMoon.cube() ? 1.0 - way.square() : 1.0);
        double turn = way.moonTurn() * way.swing();
        for (int i = 0; i < axes.length; i++) {
            axes[i] = about(axes[i], SKY_X, turn);
        }
        Vec3 light = about(moonLight(level), SKY_X, turn).lerp(SkyBodies.SUN, way.swing()).normalize();
        return new Body(about(SkyBodies.SUN.scale(-1.0), SKY_X, turn).scale((way.moonFace() + Space.MOON_HALF) * UNIT)
                .add(round(way)),
                axes, SkyMoon.half() * FAR, MOON_ROW, alpha, 1.0F, light,
                Mth.lerp((float) way.swing(), MOON_AMBIENT, NEAR_AMBIENT), null);
    }

    // The Earth, straight the other way from where the moon ends up: a flat face to the eye from near, three of them
    // from the moon.
    private static Body earth(SkyWay way) {
        Vec3[] axes = SkyMoon.tilted(new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, -1.0, 0.0),
                way.earthTilt());
        double turn = way.moonTurn() + Math.PI;
        for (int i = 0; i < axes.length; i++) {
            axes[i] = about(axes[i], SKY_X, turn);
        }
        return new Body(about(SkyBodies.SUN.scale(-1.0), SKY_X, turn).scale((way.earthFace() + Space.EARTH_HALF)
                * UNIT).add(round(way)), axes, Space.EARTH_HALF * UNIT, EARTH_ROW, (float) way.earthShown(), 1.0F,
                SkyBodies.SUN, Mth.lerp((float) way.earthTilt(), EARTH_AMBIENT, FAR_AMBIENT), null);
    }

    // The sky's frame turns about the eye as it goes round the world below (SkyWay.orbit), but the moon and the Earth
    // are near: what turns about that world's middle must stand this much further on in the sky's frame.
    private static Vec3 round(SkyWay way) {
        if (way.orbit().w() > 0.9999999F) {
            return Vec3.ZERO;
        }
        Vec3 below = about(SkyBodies.SUN.scale(-1.0), SKY_X, way.moonTurn() + (way.onMoon() ? 0.0 : Math.PI))
                .scale((way.onMoon() ? way.moonFace() + Space.MOON_HALF : way.earthFace() + Space.EARTH_HALF) * UNIT);
        Vector3f back = way.orbit().transformInverse(below.toVector3f(), new Vector3f());
        return new Vec3(back).subtract(below);
    }

    // A body's faces to the eye once more in their finer pictures (row `tile`), the more the bigger it stands
    // (`size`: half its width by how far off it is).
    private static void near(BufferBuilder builder, Matrix4f sky, Body body, int tile, double size) {
        float share = (float) Ease.smooth((size - 0.2) / 0.5);
        if (share <= 0.003F) {
            return;
        }
        Vec3[] corners = corners(body);
        float alpha = body.alpha() * share;
        float inset = 0.5F / NEAR;
        for (int face = 0; face < FACES.length; face++) {
            Vec3 normal = body.axes()[AXIS[face]].scale(SIGN[face]);
            if (normal.dot(body.at().add(normal.scale(body.half()))) >= 0.0) {
                continue;
            }
            float shade = lit(normal, body.light(), body.ambient()) * body.shine();
            float u0 = (face + inset) / CubePainter.FACES;
            float u1 = (face + 1 - inset) / CubePainter.FACES;
            float v0 = (tile + inset) * 0.5F;
            float v1 = (tile + 1 - inset) * 0.5F;
            int[] at = FACES[face];
            vertex(builder, sky, corners[at[0]], u0, v0, shade, shade, shade, alpha);
            vertex(builder, sky, corners[at[1]], u1, v0, shade, shade, shade, alpha);
            vertex(builder, sky, corners[at[2]], u1, v1, shade, shade, shade, alpha);
            vertex(builder, sky, corners[at[3]], u0, v1, shade, shade, shade, alpha);
        }
    }

    // The colour far land fades to, laid over a body's face to the eye: seen from just above it, it is all haze
    // (`share`), and its rim, seen through the most air, stays in it longest (`rim`).
    private static void veil(Matrix4f sky, Body body, float share, float rim) {
        Vec3 normal = body.axes()[AXIS[0]].scale(SIGN[0]);
        if (normal.dot(body.at().add(normal.scale(body.half()))) >= 0.0) {
            return;
        }
        float[] fog = RenderSystem.getShaderFogColor();
        Vec3[] corners = corners(body);
        Vec3 from = corners[FACES[0][0]];
        Vec3 across = corners[FACES[0][1]].subtract(from);
        Vec3 along = corners[FACES[0][3]].subtract(from);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < VEIL.length - 1; i++) {
            for (int j = 0; j < VEIL.length - 1; j++) {
                for (int corner = 0; corner < 4; corner++) {
                    int s = i + (corner == 1 || corner == 2 ? 1 : 0);
                    int t = j + (corner >= 2 ? 1 : 0);
                    boolean edge = s == 0 || t == 0 || s == VEIL.length - 1 || t == VEIL.length - 1;
                    Vec3 p = from.add(across.scale(VEIL[s])).add(along.scale(VEIL[t]));
                    builder.addVertex(sky, (float) p.x, (float) p.y, (float) p.z).setColor(fog[0], fog[1], fog[2],
                            (edge ? Math.max(share, rim) : share) * body.alpha());
                }
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static Vec3[] corners(Body body) {
        Vec3[] axes = body.axes();
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = body.at().add(axes[0].scale(((i & 1) * 2 - 1) * body.half()))
                    .add(axes[1].scale(((i >> 1 & 1) * 2 - 1) * body.half()))
                    .add(axes[2].scale(((i >> 2 & 1) * 2 - 1) * body.half()));
        }
        return corners;
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
                1.0F - level.getRainLevel(partialTick), SkyWay.now(level, partialTick)));
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
        Vec3[] corners = corners(body);
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
