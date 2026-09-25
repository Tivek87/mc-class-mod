package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The ground floor of the engine's painter (see {@link ConstructPainter}): the three layers a frame is kept in until
 * {@link #finish} draws them, the camera, time and colours everything is drawn with, how brightly a solid side is lit,
 * and the pieces everything is drawn from (lines that face the camera, quads, single corners), faded out close to the
 * camera while a construct's own shape is drawn.
 */
abstract class PainterCore {
    /**
     * The bright lines on a construct: its edges, beams, rays and sparks. They
     * are seen from both sides and never hide each other, and keep their colour even against a bright sky.
     */
    private static final RenderType LIGHT = RenderType.create("welcomescreen_hard_light",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false));
    /**
     * The solid mass a construct is made of: it hides what is behind it and its own far sides, so a
     * construct reads as a thing you could knock on, not as a cloud of light.
     */
    private static final RenderType MASS = RenderType.create("welcomescreen_hard_light_mass",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** The glow around them: added on top of what is behind, like light. Shows most at night. */
    private static final RenderType GLOW = RenderType.create("welcomescreen_hard_light_glow",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false));
    /**
     * The bright lines and the glow of constructs in your own hands in first person: those are drawn after the world,
     * straight into the picture (the target the world's light goes to has already been put on screen by then).
     */
    private static final RenderType HAND_LIGHT = RenderType.create("welcomescreen_hard_light_hand",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType HAND_GLOW = RenderType.create("welcomescreen_hard_light_hand_glow",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    // A construct's shape fades out closer to the camera than NEAR_CLEAR blocks, and is gone at NEAR_GONE: only
    // so that one sweeping right past your eyes never fills your screen.
    private static final double NEAR_GONE = 0.2;
    private static final double NEAR_CLEAR = 0.75;
    /** How strongly the edges of a construct show, and the soft glow around them. */
    protected static final double EDGE = 0.95;
    protected static final double HALO = 0.28;
    // Widths of an edge and its glow, in blocks at scale 1. They stop growing at WIDTH_CAP times that, so the
    // edges of a big construct stay lines instead of becoming bars.
    static final double EDGE_WIDTH = 0.035;
    static final double HALO_WIDTH = 0.1;
    static final double WIDTH_CAP = 3.0;
    // The light on the solid mass (see light): what every side gets, what the sky adds from above, and what the sun
    // adds to the sides that face it, from high over one corner of the world.
    private static final double SKY_FLOOR = 0.42;
    private static final double SKY = 0.3;
    private static final double SUN_LIGHT = 0.3;
    private static final Vec3 SUN = new Vec3(0.45, 0.75, -0.5).normalize();

    // A shape smaller on screen than this (its size over its distance) gets no soft glow along its edges.
    private static final double TINY = 0.012;

    /**
     * The corners of one kind of quad drawn this frame (the solid sides, the bright lines or the glow), kept as plain
     * numbers until {@link #finish} draws them: where each is, seen from the camera, and its colour with its alpha. A
     * big construct has many thousands of them, so nothing is made for each one; the buffers are handed from one frame
     * to the next (see {@link #SPARE}) and only ever grow.
     */
    static final class Layer {
        private float[] at = new float[3 * 1024];
        private int[] color = new int[1024];
        private int count;

        void add(double x, double y, double z, int rgb, int alpha) {
            if (this.count == this.color.length) {
                this.at = Arrays.copyOf(this.at, this.at.length * 2);
                this.color = Arrays.copyOf(this.color, this.color.length * 2);
            }
            int i = this.count++;
            this.at[3 * i] = (float) x;
            this.at[3 * i + 1] = (float) y;
            this.at[3 * i + 2] = (float) z;
            this.color[i] = alpha << 24 | rgb & 0xFFFFFF;
        }
    }

    /** Buffers a finished frame gave back, for the next painter to fill. Only the render thread draws. */
    private static final ArrayDeque<Layer> SPARE = new ArrayDeque<>();

    private static Layer take() {
        Layer layer = SPARE.poll();
        return layer == null ? new Layer() : layer;
    }

    private final Matrix4f matrix;
    final Vec3 camera;
    final float time;
    // What is in view, or null to draw everything.
    @Nullable
    private final Frustum frustum;
    // True for constructs in your own hands in first person (see hand): drawn with their own light, never faded out.
    private final boolean hand;
    // The colours everything is drawn in right now.
    Material material;
    // One corner at a time on its way to the graphics card, turned into the view (see draw).
    private final Vector3f vertex = new Vector3f();
    // What this frame draws: the solid sides, the bright lines, and the glow around them.
    final Layer mass = take();
    final Layer light = take();
    final Layer glow = take();
    // True while a construct's own shape is drawn: then what comes close to the camera fades out.
    boolean nearFade;
    // How far the solid shapes drawn right now flare up towards white: a construct the moment it strikes.
    private double glare;
    // Light from within added to every solid side drawn right now (see ambient).
    double ambient;

    /** See the constructors of {@link ConstructPainter}. */
    PainterCore(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material, boolean hand) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
        this.frustum = frustum;
        this.material = material;
        this.hand = hand;
    }

    /** The colours everything is drawn in right now. */
    public Material material() {
        return this.material;
    }

    /** Draws everything from now on in these colours: for the constructs of another power in the same frame. */
    public void material(Material material) {
        this.material = material;
    }

    /** Draws everything of this frame: the solid shapes first, then their lines and the glow on top. */
    public void finish(MultiBufferSource.BufferSource buffers) {
        this.draw(buffers, MASS, this.mass);
        this.draw(buffers, this.hand ? HAND_LIGHT : LIGHT, this.light);
        this.draw(buffers, this.hand ? HAND_GLOW : GLOW, this.glow);
    }

    private void draw(MultiBufferSource.BufferSource buffers, RenderType type, Layer layer) {
        if (layer.count > 0) {
            VertexConsumer buffer = buffers.getBuffer(type);
            float[] at = layer.at;
            int[] color = layer.color;
            Vector3f vertex = this.vertex;
            for (int i = 0; i < layer.count; i++) {
                int argb = color[i];
                // Turned into the view the way the game's own addVertex(matrix, ...) does, but into one vector used
                // again for every corner instead of a new one each time.
                this.matrix.transformPosition(at[3 * i], at[3 * i + 1], at[3 * i + 2], vertex);
                buffer.addVertex(vertex.x, vertex.y, vertex.z).setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF,
                        argb & 0xFF, argb >>> 24);
            }
            buffers.endBatch(type);
        }
        layer.count = 0;
        SPARE.push(layer);
    }

    /**
     * Whether any of a ball round {@code center} of this {@code radius} (in blocks) can be in view. Anything that is
     * not need not be drawn at all.
     */
    public boolean visible(Vec3 center, double radius) {
        return this.frustum == null || this.frustum.isVisible(new AABB(center.x - radius, center.y - radius,
                center.z - radius, center.x + radius, center.y + radius, center.z + radius));
    }

    /** True when a ball this big round {@code center} is only a few pixels on screen: its soft glow is left out. */
    boolean tiny(Vec3 center, double radius) {
        double away = center.distanceTo(this.camera);
        return away > 1.0 && radius / away < TINY;
    }

    /** A solid side of a construct, from four corners (for shapes that are not boxes). */
    public void side(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, double solid) {
        this.quad(this.mass, p0, p1, p2, p3, rgb, Colors.alpha(solid));
    }

    // ---- The three layers, one piece at a time: for shapes of a power's own ----

    /**
     * A line of bright light from {@code a} to {@code b} that always faces the camera, strongest along its middle and
     * fading out to its sides. It hides nothing, and never hides another line.
     *
     * @param width how wide it is, in blocks
     * @param alpha how strongly it shows, 0 to 255 (see {@link Colors#alpha})
     */
    public void lightLine(Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(this.light, a, b, width, rgb, alpha);
    }

    /** A line like {@link #lightLine}, in the glow: added on top of what is behind it, strongest against the dark. */
    public void glowLine(Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(this.glow, a, b, width, rgb, alpha);
    }

    /** A solid side from four corners, in one colour: it hides what is behind it. */
    public void massQuad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        this.quad(this.mass, p0, p1, p2, p3, rgb, alpha);
    }

    /** A side of faint light from four corners: it hides nothing (a see-through pane). */
    public void lightQuad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        this.quad(this.light, p0, p1, p2, p3, rgb, alpha);
    }

    /**
     * While on, what is drawn fades out where it comes closer to the camera than about half a block: for a construct's
     * own shape, so one sweeping right past your eyes never fills your screen. Turn it off again when done.
     */
    protected void nearFade(boolean on) {
        this.nearFade = on;
    }

    /** A bright line of light from {@code a} to {@code b}, with its glow around it. */
    public void edge(Vec3 a, Vec3 b, double width, double strength) {
        this.line(this.light, a, b, width, this.material.edge(), Colors.alpha(EDGE * strength));
        this.line(this.glow, a, b, width * 3.0, this.material.glow(), Colors.alpha(HALO * strength));
    }

    /**
     * A sheet of light between four corners, each as strong as given (0 to 1): the streak a swung blade leaves in the
     * air. Light, not a construct: it hides nothing.
     */
    public void sheet(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double a0, double a1, double a2, double a3) {
        this.put(this.glow, p0.x, p0.y, p0.z, this.material.glow(), Colors.alpha(0.6 * a0));
        this.put(this.glow, p1.x, p1.y, p1.z, this.material.glow(), Colors.alpha(0.6 * a1));
        this.put(this.glow, p2.x, p2.y, p2.z, this.material.glow(), Colors.alpha(0.6 * a2));
        this.put(this.glow, p3.x, p3.y, p3.z, this.material.glow(), Colors.alpha(0.6 * a3));
        this.put(this.light, p0.x, p0.y, p0.z, this.material.edge(), Colors.alpha(0.3 * a0));
        this.put(this.light, p1.x, p1.y, p1.z, this.material.edge(), Colors.alpha(0.3 * a1));
        this.put(this.light, p2.x, p2.y, p2.z, this.material.edge(), Colors.alpha(0.3 * a2));
        this.put(this.light, p3.x, p3.y, p3.z, this.material.edge(), Colors.alpha(0.3 * a3));
    }

    static double sq(double value) {
        return value * value;
    }

    /**
     * How much a side's light changes with how it faces you, as a part of it: a little dimmer where you look straight
     * at it and a little brighter where it turns away towards its outline, so round things read round and every shape
     * seems to glow at its edges.
     *
     * @param face 1 when the side faces you straight, 0 when you see it edge-on
     */
    static double sheen(double face) {
        return 0.88 + 0.24 * (1.0 - Mth.clamp(face, 0.0, 1.0));
    }

    /** The camera, where everything is drawn from. */
    public Vec3 camera() {
        return this.camera;
    }

    /** The time of this frame, in ticks, for everything that ripples. */
    public float time() {
        return this.time;
    }

    /**
     * How brightly one side of a box is lit, as a part of its colour (see {@link #light}).
     *
     * @param middle the middle of the box, to tell which way the side faces
     */
    public static double lit(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 middle) {
        Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
        return lit(normal.x, normal.y, normal.z, p0.x - middle.x, p0.y - middle.y, p0.z - middle.z);
    }

    /**
     * {@link #lit(Vec3, Vec3, Vec3, Vec3)} in plain numbers: the way the side faces (any length) and the way from the
     * middle of the box to one of its corners. The same sums as normalising a vector and turning it round, in the same
     * order, so it comes out exactly the same.
     */
    static double lit(double nx, double ny, double nz, double dx, double dy, double dz) {
        double lengthSqr = nx * nx + ny * ny + nz * nz;
        if (lengthSqr < 1.0E-12) {
            return 0.8;
        }
        // As Vec3.normalize: too short to have a way comes out as no way at all.
        double length = Math.sqrt(lengthSqr);
        double x = length < 1.0E-4 ? 0.0 : nx / length;
        double y = length < 1.0E-4 ? 0.0 : ny / length;
        double z = length < 1.0E-4 ? 0.0 : nz / length;
        if (x * dx + y * dy + z * dz < 0.0) {
            x *= -1.0;
            y *= -1.0;
            z *= -1.0;
        }
        return light(x, y, z);
    }

    /**
     * How brightly a side facing {@code normal} (one long, in the world) is lit, as a part of its colour: the side
     * facing up catches the most light and the one facing down the least, the way a block's sides do, and the sides
     * facing the sun, high over one corner of the world, more than those facing away from it. So every side of a
     * shape comes out a shade of its own and its corners and folds read at a glance, as a solid thing does.
     */
    public static double light(Vec3 normal) {
        return light(normal.x, normal.y, normal.z);
    }

    /** {@link #light(Vec3)}, for a way given as three numbers. */
    public static double light(double x, double y, double z) {
        return SKY_FLOOR + SKY * (y * 0.5 + 0.5) + SUN_LIGHT * Math.max(0.0, x * SUN.x + y * SUN.y + z * SUN.z);
    }

    /**
     * The colour of the solid mass at this much light: the material's mass colour, darker in the shade, never brighter
     * than that colour itself (past that it would wash out to white), except while it glares as it strikes.
     */
    int mass(double light) {
        int rgb = Colors.shade(this.material.mass(), Math.min(1.0, light));
        return this.glare > 0.0 ? Colors.mix(rgb, this.material.hot(), (float) Math.min(0.75, this.glare
                * Math.min(1.0, light))) : rgb;
    }

    /** How far the solid shapes drawn from now on flare up towards white, 0 to 1 (0 once they are done). */
    public void glare(double amount) {
        this.glare = Mth.clamp(amount, 0.0, 1.0);
    }

    /**
     * How much light from within the solid shapes drawn from now on get on top of the sky's (0 once they are done): hard
     * light glows of itself, so a big construct seen from below (the belly of a plane high over you) is not left a dark
     * shadow, and every part and seam on its underside still reads.
     */
    public void ambient(double amount) {
        this.ambient = Math.max(0.0, amount);
    }

    /** A line that always faces the camera: strongest along the middle, fading out to its sides. */
    void line(Layer layer, Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(layer, a.x, a.y, a.z, b.x, b.y, b.z, width, rgb, alpha);
    }

    void line(Layer layer, double ax, double ay, double az, double bx, double by, double bz, double width,
            int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double tx = this.camera.x - (ax + bx) * 0.5;
        double ty = this.camera.y - (ay + by) * 0.5;
        double tz = this.camera.z - (az + bz) * 0.5;
        double sx = dy * tz - dz * ty;
        double sy = dz * tx - dx * tz;
        double sz = dx * ty - dy * tx;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1.0E-6) {
            return;
        }
        double k = width * 0.5 / length;
        sx *= k;
        sy *= k;
        sz *= k;
        int alphaA = this.faded(ax, ay, az, alpha);
        int alphaB = this.faded(bx, by, bz, alpha);
        this.put(layer, ax, ay, az, rgb, alphaA);
        this.put(layer, bx, by, bz, rgb, alphaB);
        this.put(layer, bx + sx, by + sy, bz + sz, rgb, 0);
        this.put(layer, ax + sx, ay + sy, az + sz, rgb, 0);
        this.put(layer, ax, ay, az, rgb, alphaA);
        this.put(layer, bx, by, bz, rgb, alphaB);
        this.put(layer, bx - sx, by - sy, bz - sz, rgb, 0);
        this.put(layer, ax - sx, ay - sy, az - sz, rgb, 0);
    }

    private void quad(Layer layer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        this.put(layer, p0.x, p0.y, p0.z, rgb, this.faded(p0.x, p0.y, p0.z, alpha));
        this.put(layer, p1.x, p1.y, p1.z, rgb, this.faded(p1.x, p1.y, p1.z, alpha));
        this.put(layer, p2.x, p2.y, p2.z, rgb, this.faded(p2.x, p2.y, p2.z, alpha));
        this.put(layer, p3.x, p3.y, p3.z, rgb, this.faded(p3.x, p3.y, p3.z, alpha));
    }

    /** One corner, out in the world, into a layer: kept as where it is seen from the camera. */
    void put(Layer layer, double x, double y, double z, int rgb, int alpha) {
        layer.add(x - this.camera.x, y - this.camera.y, z - this.camera.z, rgb, alpha);
    }

    /**
     * {@code alpha} at a point: while a construct's own shape is drawn, what comes close to the camera fades out, so a
     * big fist hanging right next to you never fills your screen.
     */
    int faded(double x, double y, double z, int alpha) {
        if (!this.nearFade || this.hand) {
            return alpha;
        }
        double away = Math.sqrt(sq(x - this.camera.x) + sq(y - this.camera.y) + sq(z - this.camera.z));
        return (int) (alpha * Ease.smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }
}
