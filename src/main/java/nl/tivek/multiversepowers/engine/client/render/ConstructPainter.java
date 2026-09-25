package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
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
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The engine's painter: draws constructs (solid shapes of energy with bright edges and a glow around them) and light
 * (lines, flares, rings, trails, flames, haze), for any power of any character. What colour the energy has comes from
 * its {@link Material}; a character with shapes of its own adds them in a painter of its own on top of this one (Green
 * Lantern's is {@code LanternPainter}).
 *
 * <p>How to draw with it: make one painter per frame, draw everything of that frame with it, and end with
 * {@link #finish}. Everything is kept in three layers until then, drawn in this order: the solid mass, the bright
 * lines on top of it, and the glow over both.
 *
 * <p>What it draws:
 * <ul>
 * <li>solid shapes: box models ({@link #model}), round and slanted parts ({@link Mesh}, {@link #mesh}) and both
 * together ({@link Shape}, {@link #shape}); breaking up into solid pieces ({@link #shattered}); see-through versions
 * for a construct right in front of its owner's eyes ({@link #seeThrough});</li>
 * <li>light: lines ({@link #edge}, {@link #lightLine}, {@link #glowLine}), sheets ({@link #sheet}), flares
 * ({@link #flare}), rings ({@link #circle}), streaks behind a flyer ({@link #trail}), jet flames ({@link #exhaust}),
 * glowing haze ({@link #haze});</li>
 * <li>chains of solid links ({@link #chain}) and tumbling chunks ({@link #chunk});</li>
 * <li>any of the solid shapes above cut off at a plane, with a seam of light along the cut, for a thing coming out
 * of a surface of light ({@link #clip}, {@link #noClip}).</li>
 * </ul>
 *
 * <p>It is built for big models too (a plane the size of a house has thousands of sides):
 * <ul>
 * <li>every corner is kept as plain numbers in buffers that are used again frame after frame, so drawing makes
 * nothing new for each corner, and nothing new either as they go to the graphics card;</li>
 * <li>a shape that lies wholly outside the view is skipped at once (see {@link #visible}), measured by a ball round
 * it that is worked out once per shape;</li>
 * <li>which edges of a box model have another box against them is worked out once per model, not every frame;</li>
 * <li>far away, where a shape is only a few pixels big, the soft glow along its edges is left out (see
 * {@link #tiny}).</li>
 * </ul>
 */
public class ConstructPainter {
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
    // How far outside a box an edge is looked at to see whether another box sits against it there, in blocks
    // at scale 1.
    private static final double EDGE_OUT = 0.02;
    // Widths of an edge and its glow, in blocks at scale 1. They stop growing at WIDTH_CAP times that, so the
    // edges of a big construct stay lines instead of becoming bars.
    private static final double EDGE_WIDTH = 0.035;
    private static final double HALO_WIDTH = 0.1;
    private static final double WIDTH_CAP = 3.0;
    // A part smaller than this (in blocks at scale 1, see fine) gets finer lines, down to FINEST times the usual.
    private static final double FINE_SIZE = 0.35;
    private static final double FINEST = 0.35;
    /** A cube of one block round its middle: chunks thrown up by a blow. */
    private static final double[][] CUBE = { { -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 1.0 } };
    // How strongly the outline of a see-through shape shows, next to that of a solid one.
    private static final double SEE_THROUGH_EDGE = 0.55;
    // How much wider the seam of light along a cut (see clip) is drawn than an edge, and its glow than an edge's glow.
    private static final double SEAM_WIDTH = 1.5;
    // How near two ends of the lines a solid part was cut along lie (squared, in blocks) to join into one outline of its
    // lid (see lid).
    private static final double LID_JOIN = 1.0E-8;
    // The light on the solid mass (see light): what every side gets, what the sky adds from above, and what the sun
    // adds to the sides that face it, from high over one corner of the world.
    private static final double SKY_FLOOR = 0.42;
    private static final double SKY = 0.3;
    private static final double SUN_LIGHT = 0.3;
    private static final Vec3 SUN = new Vec3(0.45, 0.75, -0.5).normalize();
    /**
     * One link of a chain (see {@link #chain}): a flat oval ring lying round y with its long way along z, one long and
     * three quarters of that wide at scale 1.
     */
    private static final Mesh LINK = Mesh.torus(12, 6, 0.3, 0.075, 1.15).scaled(1.0, 1.0, 1.335);
    // How far apart two links sit, as a part of a link's length: less than one, so they hook into each other.
    private static final double CHAIN_STEP = 0.72;
    // The four corners of each side of a box. A corner is three bits: 1 = far x, 2 = far y, 4 = far z.
    private static final int[][] SIDES = { { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 },
            { 0, 1, 3, 2 }, { 4, 6, 7, 5 } };

    // A shape smaller on screen than this (its size over its distance) gets no soft glow along its edges.
    private static final double TINY = 0.012;

    /**
     * The corners of one kind of quad drawn this frame (the solid sides, the bright lines or the glow), kept as plain
     * numbers until {@link #finish} draws them: where each is, seen from the camera, and its colour with its alpha. A
     * big construct has many thousands of them, so nothing is made for each one; the buffers are handed from one frame
     * to the next (see {@link #SPARE}) and only ever grow.
     */
    private static final class Layer {
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

    /**
     * What a box model needs worked out only once: for every edge of every box whether another box lies against it
     * (see {@link #rim}), and a ball round the whole model, in its own blocks at scale 1, to skip it when it is out of
     * view. Kept per model for as long as the model itself is kept.
     */
    private record ModelInfo(boolean[] covered, double x, double y, double z, double radius) {
    }

    private static final Map<double[][], ModelInfo> MODELS = new WeakHashMap<>();
    // A model with fewer boxes than this is worked out on the spot: those are often made new every frame.
    private static final int CACHED_FROM = 4;

    /**
     * Where a construct is and how it is turned: its middle, its own right, up and forward, and its scale. Its three
     * ways are one long, unless it is squashed or stretched (see {@link #stretched}).
     */
    public record Frame(Vec3 center, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        /** A point of the model (in blocks at scale 1) out in the world. */
        public Vec3 at(double x, double y, double z) {
            // Worked out in one go, in the same order as adding the three ways one by one: the same point, without
            // making six vectors on the way for every corner of every box.
            double sx = x * this.scale;
            double sy = y * this.scale;
            double sz = z * this.scale;
            return new Vec3(this.center.x + this.right.x * sx + this.up.x * sy + this.forward.x * sz,
                    this.center.y + this.right.y * sx + this.up.y * sy + this.forward.y * sz,
                    this.center.z + this.right.z * sx + this.up.z * sy + this.forward.z * sz);
        }

        /** The other way around: where a point of the world lies in the model. */
        public Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.center);
            return new Vec3(way.dot(this.right) / (this.right.lengthSqr() * this.scale),
                    way.dot(this.up) / (this.up.lengthSqr() * this.scale),
                    way.dot(this.forward) / (this.forward.lengthSqr() * this.scale));
        }

        /** The way a side of the model faces (one long, in the model) as a way in the world, one long. */
        public Vec3 normal(Vec3 model) {
            Vec3 way = this.right.scale(model.x / this.right.lengthSqr())
                    .add(this.up.scale(model.y / this.up.lengthSqr()))
                    .add(this.forward.scale(model.z / this.forward.lengthSqr()));
            double length = way.length();
            return length < 1.0E-12 ? Vec3.ZERO : way.scale(1.0 / length);
        }

        /** The same frame with its middle at a point of the model: for a part that turns about its own middle. */
        public Frame moved(double x, double y, double z) {
            return new Frame(this.at(x, y, z), this.right, this.up, this.forward, this.scale);
        }

        /**
         * The same frame turned by {@code angle} (radians) about a line through a point of the model that runs along
         * (ax, ay, az) in the model: a lid on its hinge, a door, a jaw. The angle turns the way it would in the model's
         * own terms (counter-clockwise looking down the line from its tip), also when the frame is a mirror image, as
         * one with his right as its x is.
         */
        public Frame turned(double px, double py, double pz, double ax, double ay, double az, double angle) {
            Vec3 axis = this.right.scale(ax).add(this.up.scale(ay)).add(this.forward.scale(az)).normalize();
            double turn = this.right.cross(this.up).dot(this.forward) < 0.0 ? -angle : angle;
            Vec3 pivot = this.at(px, py, pz);
            return new Frame(pivot.add(Vectors.spin(this.center.subtract(pivot), axis, turn)),
                    Vectors.spin(this.right, axis, turn), Vectors.spin(this.up, axis, turn),
                    Vectors.spin(this.forward, axis, turn), this.scale);
        }

        /** The same frame squashed or stretched along its own right, up and forward (1 = as it is). */
        public Frame stretched(double x, double y, double z) {
            return new Frame(this.center, this.right.scale(x), this.up.scale(y), this.forward.scale(z), this.scale);
        }

        /** How long its longest way is: 1, unless it is stretched. */
        public double stretch() {
            return Math.sqrt(Math.max(this.right.lengthSqr(), Math.max(this.up.lengthSqr(), this.forward.lengthSqr())));
        }

        /**
         * A frame at {@code center} that faces {@code forward}, its up as near {@code up} as it can be and its right
         * worked out from the two, the way every construct stands (see the note on handedness in the project rules).
         */
        public static Frame of(Vec3 center, Vec3 forward, Vec3 up, double scale) {
            Vec3 ahead = forward.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
            Vec3 right = ahead.cross(up);
            if (right.lengthSqr() < 1.0E-8) {
                right = ahead.cross(Math.abs(ahead.x) < 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 0.0, 1.0));
            }
            right = right.normalize();
            return new Frame(center, right, right.cross(ahead).normalize(), ahead, scale);
        }
    }

    /**
     * The shape of a construct, or of one part of it that moves on its own: boxes (see {@link #model}) and round or
     * slanted parts (see {@link Mesh}), in blocks at scale 1.
     */
    public record Shape(double[][] boxes, Mesh... meshes) {
        private static final double[][] NO_BOXES = new double[0][];

        /** A shape of round or slanted parts only. */
        public static Shape of(Mesh... meshes) {
            return new Shape(NO_BOXES, meshes);
        }
    }

    private final Matrix4f matrix;
    private final Vec3 camera;
    private final float time;
    // What is in view, or null to draw everything.
    @Nullable
    private final Frustum frustum;
    // True for constructs in your own hands in first person (see hand): drawn with their own light, never faded out.
    private final boolean hand;
    // The colours everything is drawn in right now.
    private Material material;
    // One corner at a time on its way to the graphics card, turned into the view (see draw).
    private final Vector3f vertex = new Vector3f();
    // The eight corners of the box drawn right now, as x, y, z of corner 0, then of corner 1 and so on (see
    // corners), and the same corners moved a hair towards the camera for its edges.
    private final double[] corner = new double[24];
    private final double[] liftedCorner = new double[24];
    // What this frame draws: the solid sides, the bright lines, and the glow around them.
    private final Layer mass = take();
    private final Layer light = take();
    private final Layer glow = take();
    // Room to work a round part out in, used again for every part: its corners out in the world, the way its sides
    // face, and which of them face the camera.
    private double[] wx = new double[256];
    private double[] wy = new double[256];
    private double[] wz = new double[256];
    private double[] nx = new double[256];
    private double[] ny = new double[256];
    private double[] nz = new double[256];
    private boolean[] facingCamera = new boolean[256];
    // True while a construct's own shape is drawn: then what comes close to the camera fades out.
    private boolean nearFade;
    // Above 0 while a see-through shape is drawn (see seeThrough): how strongly its sides show.
    private double faint;
    // How far the solid shapes drawn right now flare up towards white: a construct the moment it strikes.
    private double glare;
    // How far the pieces of a construct breaking up right now fly, next to how far they usually do (see fling).
    private double fling = 1.0;
    // Light from within added to every solid side drawn right now (see ambient).
    private double ambient;
    // True while everything solid is cut off at a plane (see clip): a point on it, the way to the side that is kept
    // (one long), and how strongly the seam of light along the cut burns.
    private boolean clipping;
    private double clipX;
    private double clipY;
    private double clipZ;
    private double clipNormalX;
    private double clipNormalY;
    private double clipNormalZ;
    private double seam;
    // Room to cut one side in, used again for every side (see cut), as x, y, z one corner after another: its four
    // corners going in and how far each lies in front of the plane, what is left of it (a side of four corners cut by
    // a plane keeps at most six), and where its edges cross the plane (at most four times). And the two ends of a
    // line being cut (see cutLine).
    private final double[] cutIn = new double[12];
    private final double[] cutAhead = new double[4];
    private final double[] cutOut = new double[18];
    private final double[] cutCross = new double[12];
    private final double[] cutEnds = new double[6];
    // The lines the sides of the solid part being drawn were cut along, two ends each (see cut), and how many: the
    // part is closed with a lid there once all its sides are drawn (see lid), so it is never seen hollow.
    private double[] lidCuts = new double[6 * 32];
    private int lidCount;
    // Which outline each of those lines belongs to (the first line of it), and room to walk round one (see lid).
    private int[] lidOutline = new int[32];
    private int[] lidWaiting = new int[32];

    /**
     * A painter that draws everything, wherever it is.
     *
     * @param camera   where the camera is: everything is drawn as seen from there
     * @param time     the time of this frame in ticks (with the part of the tick gone by), for everything that moves
     *                 by itself: ripples, pulses, turning rings
     * @param material the colours to draw in
     */
    public ConstructPainter(PoseStack pose, Vec3 camera, float time, Material material) {
        this(pose, camera, time, null, material);
    }

    /**
     * A painter that skips what is out of view.
     *
     * @param frustum what is in view: shapes wholly outside it are skipped; null draws everything
     */
    public ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material) {
        this(pose, camera, time, frustum, material, false);
    }

    /**
     * @param hand true for a painter of what is held in your own hands in first person (see {@link #hand})
     */
    protected ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
        this.frustum = frustum;
        this.material = material;
        this.hand = hand;
    }

    /**
     * A painter for constructs in your own hands in first person, drawn along with your hands: everything is given in
     * blocks in front of your eyes (x to the right, y up, -z ahead), the camera sits at 0, and nothing close by fades
     * out, since that is exactly where your hands are.
     */
    public static ConstructPainter hand(PoseStack pose, float time, Material material) {
        return new ConstructPainter(pose, Vec3.ZERO, time, null, material, true);
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
    private boolean tiny(Vec3 center, double radius) {
        double away = center.distanceTo(this.camera);
        return away > 1.0 && radius / away < TINY;
    }

    /**
     * Any shape made of boxes, at {@code frame}: seven numbers per box (its low corner, its high corner, and how
     * brightly
     * it burns), in blocks at scale 1. Solid sides, bright lines where the shape ends, and a glow. What comes close to
     * the camera fades out.
     *
     * @param solid  0 = gone, 1 = fully there
     * @param bright how brightly it burns, on top of each box's own brightness
     */
    public void model(double[][] model, Frame frame, double solid, double bright) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0 || model.length == 0) {
            return;
        }
        ModelInfo info = info(model);
        Vec3 middle = frame.at(info.x(), info.y(), info.z());
        double reach = info.radius() * frame.scale() * frame.stretch();
        if (!this.visible(middle, reach)) {
            return;
        }
        boolean halo = !this.tiny(middle, reach);
        Vec3 view = frame.local(this.camera);
        boolean clipped = this.clipping;
        if (clipped) {
            // Cut off at a plane (see clip): a model wholly behind it is left out, one wholly in front of it is drawn
            // whole, without cutting it side by side.
            double ahead = this.ahead(middle.x, middle.y, middle.z);
            if (ahead < -reach) {
                return;
            }
            this.clipping = ahead <= reach;
        }
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            this.corners(frame, box);
            this.box(model, info, b, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * bright,
                    (box[2] + box[5]) * 0.5, 0.0, halo);
        }
        this.nearFade = false;
        if (clipped) {
            this.clipping = true;
        }
    }

    /**
     * A box model the way {@link #model} draws it, for a construct that is being charged up in its maker's hand: the
     * light ripples along it harder and its glow swells the further it is charged. It is always drawn with its glow and
     * never skipped for being out of view: it hangs right beside its maker.
     *
     * @param charge how far it is charged: 0 = not at all, 1 = as far as it goes
     */
    protected void chargedModel(double[][] model, Frame frame, double solid, double bright, double charge) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        ModelInfo info = info(model);
        Vec3 view = frame.local(this.camera);
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            this.corners(frame, box);
            this.box(model, info, b, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * bright,
                    (box[2] + box[5]) * 0.5, charge, true);
        }
        this.nearFade = false;
    }

    /**
     * The eight corners of one box of a model out in the world, into {@link #corner}: the same points
     * {@link Frame#at} gives, worked out the same way, without making a vector for each.
     */
    private void corners(Frame frame, double[] box) {
        Vec3 c = frame.center();
        Vec3 r = frame.right();
        Vec3 u = frame.up();
        Vec3 f = frame.forward();
        double scale = frame.scale();
        double[] at = this.corner;
        for (int i = 0; i < 8; i++) {
            double sx = box[(i & 1) == 0 ? 0 : 3] * scale;
            double sy = box[(i & 2) == 0 ? 1 : 4] * scale;
            double sz = box[(i & 4) == 0 ? 2 : 5] * scale;
            at[3 * i] = c.x + r.x * sx + u.x * sy + f.x * sz;
            at[3 * i + 1] = c.y + r.y * sx + u.y * sy + f.y * sz;
            at[3 * i + 2] = c.z + r.z * sx + u.z * sy + f.z * sz;
        }
    }

    /** What is worked out once for a box model (see {@link ModelInfo}): kept for big ones, made anew for the rest. */
    private static ModelInfo info(double[][] model) {
        if (model.length < CACHED_FROM) {
            return work(model);
        }
        ModelInfo info = MODELS.get(model);
        if (info == null) {
            info = work(model);
            MODELS.put(model, info);
        }
        return info;
    }

    /**
     * Works out, for every edge of every box, whether another box lies against it there (the middle of the edge, a hair
     * outside the box, lies inside another box), and a ball round the whole model.
     */
    private static ModelInfo work(double[][] model) {
        boolean[] covered = new boolean[model.length * 24];
        double[] low = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
        double[] high = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
        double[] at = new double[3];
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            for (int k = 0; k < 3; k++) {
                low[k] = Math.min(low[k], box[k]);
                high[k] = Math.max(high[k], box[3 + k]);
            }
            for (int corner = 0; corner < 8; corner++) {
                for (int axis = 0; axis < 3; axis++) {
                    int bit = 1 << axis;
                    if ((corner & bit) != 0) {
                        continue;
                    }
                    for (int k = 0; k < 3; k++) {
                        int mask = 1 << k;
                        if (mask == bit) {
                            at[k] = (box[k] + box[3 + k]) * 0.5;
                        } else {
                            at[k] = (corner & mask) != 0 ? box[3 + k] + EDGE_OUT : box[k] - EDGE_OUT;
                        }
                    }
                    for (int o = 0; o < model.length; o++) {
                        double[] other = model[o];
                        if (o != b && at[0] > other[0] && at[0] < other[3] && at[1] > other[1] && at[1] < other[4]
                                && at[2] > other[2] && at[2] < other[5]) {
                            covered[b * 24 + corner * 3 + axis] = true;
                            break;
                        }
                    }
                }
            }
        }
        double dx = high[0] - low[0];
        double dy = high[1] - low[1];
        double dz = high[2] - low[2];
        return new ModelInfo(covered, (low[0] + high[0]) * 0.5, (low[1] + high[1]) * 0.5, (low[2] + high[2]) * 0.5,
                Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.5);
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

    /**
     * A shape made of boxes breaking up, still solid: every box flies out from the middle of the shape, tumbling and
     * dropping as it goes, and shrinks away to nothing. Nothing of it fades out: a construct is never see-through.
     *
     * @param apart 0 = still whole, 1 = gone
     */
    public void shattered(double[][] model, Frame frame, double apart, double bright) {
        this.shattered(model, frame, apart, bright, 0);
    }

    /** Boxes breaking up (see above), their pieces counted from {@code seed} on: that sets the way each one flies. */
    private void shattered(double[][] model, Frame frame, double apart, double bright, int seed) {
        double gone = Mth.clamp(apart, 0.0, 1.0);
        double left = 1.0 - gone;
        if (left <= 0.0) {
            return;
        }
        Vec3 view = frame.local(this.camera);
        double size = Math.max(1.0, frame.scale()) * this.fling;
        ModelInfo info = info(model);
        double[] at = this.corner;
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            Vec3 middle = frame.at((box[0] + box[3]) * 0.5, (box[1] + box[4]) * 0.5, (box[2] + box[5]) * 0.5);
            Vec3 out = middle.subtract(frame.center());
            int piece = seed + b;
            Vec3 scatter = Noise.direction(piece, 7);
            Vec3 way = this.awayFromEye(middle,
                    out.lengthSqr() > 1.0E-6 ? out.normalize().add(scatter.scale(0.6)).normalize() : scatter);
            double speed = (1.2 + 1.8 * Noise.of(piece, 7, 3)) * size;
            // Out and up at first, then down: thrown pieces.
            Vec3 moved = middle.add(way.scale(speed * gone)).add(0.0, (1.2 * gone - 2.6 * gone * gone) * size, 0.0);
            Vec3 axis = Noise.direction(piece, 9);
            double turn = gone * (1.5 + 3.0 * Noise.of(piece, 9, 2));
            for (int i = 0; i < 8; i++) {
                Vec3 corner = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4], box[(i & 4) == 0 ? 2 : 5]);
                Vec3 flown = moved.add(Vectors.spin(corner.subtract(middle), axis, turn).scale(left));
                at[3 * i] = flown.x;
                at[3 * i + 1] = flown.y;
                at[3 * i + 2] = flown.z;
            }
            this.box(model, info, b, view, Math.min(frame.scale(), WIDTH_CAP), 1.0, box[6] * bright,
                    (box[2] + box[5]) * 0.5, 0.0, true);
        }
        this.nearFade = false;
    }

    /**
     * The way a piece flies off as its construct breaks up, turned aside where it would come at the camera: the pieces
     * of a construct never fly into your face and fill your view.
     */
    private Vec3 awayFromEye(Vec3 from, Vec3 way) {
        Vec3 toEye = this.camera.subtract(from);
        double length = toEye.length();
        if (length < 1.0E-6) {
            return way;
        }
        toEye = toEye.scale(1.0 / length);
        double at = way.dot(toEye);
        return at > 0.0 ? way.subtract(toEye.scale(1.6 * at)).normalize() : way;
    }

    /** A shape of boxes and round parts at {@code frame}, drawn the way {@link #model} draws boxes. */
    public void shape(Shape shape, Frame frame, double solid, double bright) {
        if (shape.boxes().length > 0) {
            this.model(shape.boxes(), frame, solid, bright);
        }
        for (Mesh mesh : shape.meshes()) {
            this.mesh(mesh, frame, solid, bright);
        }
    }

    /**
     * A shape of boxes and round parts breaking up, still solid: every box and every round part flies off on its own
     * (see {@link #shattered(double[][], Frame, double, double)}).
     */
    public void shattered(Shape shape, Frame frame, double apart, double bright) {
        this.shattered(shape, frame, apart, bright, 0);
    }

    /**
     * A shape breaking up (see above), its pieces flying off the ways of piece {@code seed} and on: give the parts of
     * one thing that break up together (the joints of a finger, each a shape of one round part) seeds far enough apart,
     * or they all fly off the same way, tumbling alike.
     */
    public void shattered(Shape shape, Frame frame, double apart, double bright, int seed) {
        if (shape.boxes().length > 0) {
            this.shattered(shape.boxes(), frame, apart, bright, seed);
        }
        for (int k = 0; k < shape.meshes().length; k++) {
            this.shatteredMesh(shape.meshes()[k], frame, seed + shape.boxes().length + k, apart, bright);
        }
    }

    /** A round or slanted part (see {@link Mesh}) at {@code frame}, drawn the way {@link #model} draws boxes. */
    public void mesh(Mesh mesh, Frame frame, double solid, double bright) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0 || mesh.points.length == 0) {
            return;
        }
        Vec3 middle = frame.at(mesh.boundX, mesh.boundY, mesh.boundZ);
        double reach = mesh.boundRadius * frame.scale() * frame.stretch();
        if (!this.visible(middle, reach)) {
            return;
        }
        boolean clipped = this.clipping;
        if (clipped) {
            // Cut off at a plane (see clip): a part wholly behind it is left out, one wholly in front of it is drawn
            // whole, without cutting it side by side.
            double ahead = this.ahead(middle.x, middle.y, middle.z);
            if (ahead < -reach) {
                return;
            }
            this.clipping = ahead <= reach;
        }
        this.room(mesh.points.length, mesh.sides.length);
        double scale = frame.scale();
        Vec3 c = frame.center();
        Vec3 r = frame.right();
        Vec3 u = frame.up();
        Vec3 f = frame.forward();
        for (int i = 0; i < mesh.px.length; i++) {
            double x = mesh.px[i] * scale;
            double y = mesh.py[i] * scale;
            double z = mesh.pz[i] * scale;
            this.wx[i] = c.x + r.x * x + u.x * y + f.x * z;
            this.wy[i] = c.y + r.y * x + u.y * y + f.y * z;
            this.wz[i] = c.z + r.z * x + u.z * y + f.z * z;
        }
        // The way a side faces, out in the world (see Frame.normal): a stretched frame bends it the other way.
        double r2 = r.lengthSqr();
        double u2 = u.lengthSqr();
        double f2 = f.lengthSqr();
        for (int k = 0; k < mesh.sides.length; k++) {
            double a = mesh.nx[k] / r2;
            double b = mesh.ny[k] / u2;
            double d = mesh.nz[k] / f2;
            double x = r.x * a + u.x * b + f.x * d;
            double y = r.y * a + u.y * b + f.y * d;
            double z = r.z * a + u.z * b + f.z * d;
            double length = Math.sqrt(x * x + y * y + z * z);
            double to = length < 1.0E-12 ? 0.0 : 1.0 / length;
            this.nx[k] = x * to;
            this.ny[k] = y * to;
            this.nz[k] = z * to;
        }
        this.drawMesh(mesh, Math.min(scale, WIDTH_CAP), strength, bright, !this.tiny(middle, reach));
        if (clipped) {
            this.clipping = true;
        }
    }

    /** Makes the room to work out a part of this many corners and sides in. */
    private void room(int points, int sides) {
        if (this.wx.length < points) {
            int size = Math.max(points, this.wx.length * 2);
            this.wx = new double[size];
            this.wy = new double[size];
            this.wz = new double[size];
        }
        if (this.nx.length < sides) {
            int size = Math.max(sides, this.nx.length * 2);
            this.nx = new double[size];
            this.ny = new double[size];
            this.nz = new double[size];
            this.facingCamera = new boolean[size];
        }
    }

    /** One round part flying off as its construct breaks up, the way a box does in {@link #shattered}. */
    private void shatteredMesh(Mesh mesh, Frame frame, int piece, double apart, double bright) {
        double gone = Mth.clamp(apart, 0.0, 1.0);
        double left = 1.0 - gone;
        if (left <= 0.0 || mesh.points.length == 0) {
            return;
        }
        double size = Math.max(1.0, frame.scale()) * this.fling;
        Vec3 middle = frame.at(mesh.middle.x, mesh.middle.y, mesh.middle.z);
        Vec3 out = middle.subtract(frame.center());
        Vec3 scatter = Noise.direction(piece, 7);
        Vec3 way = this.awayFromEye(middle,
                out.lengthSqr() > 1.0E-6 ? out.normalize().add(scatter.scale(0.6)).normalize() : scatter);
        double speed = (1.2 + 1.8 * Noise.of(piece, 7, 3)) * size;
        Vec3 moved = middle.add(way.scale(speed * gone)).add(0.0, (1.2 * gone - 2.6 * gone * gone) * size, 0.0);
        Vec3 axis = Noise.direction(piece, 9);
        double turn = gone * (1.5 + 3.0 * Noise.of(piece, 9, 2));
        this.room(mesh.points.length, mesh.sides.length);
        for (int i = 0; i < mesh.points.length; i++) {
            Vec3 point = mesh.points[i];
            Vec3 at = moved.add(Vectors.spin(frame.at(point.x, point.y, point.z).subtract(middle), axis,
                    turn).scale(left));
            this.wx[i] = at.x;
            this.wy[i] = at.y;
            this.wz[i] = at.z;
        }
        for (int k = 0; k < mesh.sides.length; k++) {
            Vec3 normal = Vectors.spin(frame.normal(mesh.normals[k]), axis, turn);
            this.nx[k] = normal.x;
            this.ny[k] = normal.y;
            this.nz[k] = normal.z;
        }
        this.drawMesh(mesh, Math.min(frame.scale(), WIDTH_CAP), 1.0, bright, true);
    }

    /**
     * Draws a round part whose corners and sides have just been worked out into the room for it (see {@link #room}):
     * solid sides lit like the sides of a box, and a bright line along every edge where its outline runs as you look at
     * it (one side along it faces you and the other faces away), or where it has only one side.
     *
     * @param halo false to leave out the soft glow along its edges: far away it would only blur it
     */
    private void drawMesh(Mesh mesh, double width, double solid, double bright, boolean halo) {
        int count = mesh.sides.length;
        // A see-through shape (see seeThrough) puts its sides with the light, faint, so they hide nothing.
        boolean faint = this.faint > 0.0;
        Layer sides = faint ? this.light : this.mass;
        int body = Colors.alpha(faint ? this.faint * solid : solid);
        double quiet = faint ? SEE_THROUGH_EDGE : 1.0;
        double fine = width * mesh.fine;
        this.nearFade = true;
        this.lidCount = 0;
        for (int s = 0; s < count; s++) {
            double x = this.nx[s];
            double y = this.ny[s];
            double z = this.nz[s];
            if (x * x + y * y + z * z < 0.5) {
                this.facingCamera[s] = false;
                continue;
            }
            int[] side = mesh.sides[s];
            double vx = this.camera.x - (this.wx[side[0]] + this.wx[side[2]]) * 0.5;
            double vy = this.camera.y - (this.wy[side[0]] + this.wy[side[2]]) * 0.5;
            double vz = this.camera.z - (this.wz[side[0]] + this.wz[side[2]]) * 0.5;
            double look = x * vx + y * vy + z * vz;
            this.facingCamera[s] = look > 0.0;
            double away = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double face = away < 1.0E-6 ? 1.0 : Math.abs(look) / away;
            double ripple = 0.9 + 0.06 * Math.sin(this.time * 0.5 - mesh.middleZ[s] * 4.0);
            double light = (light(x, y, z) + this.ambient) * sheen(face) * ripple * bright * mesh.bright[s];
            if (this.clipping) {
                this.cutAt(sides, side, this.mass(light), body, fine, solid * quiet, halo);
            } else {
                this.quadAt(sides, side[0], side[1], side[2], side[3], this.mass(light), body);
            }
        }
        if (this.clipping) {
            this.lid(sides, body, bright);
        }
        int edge = Colors.alpha(EDGE * solid * quiet);
        int glowing = halo ? Colors.alpha(HALO * solid * quiet) : 0;
        double lift = 0.01 + 0.02 * fine;
        for (int e = 0; e < mesh.edgeFrom.length; e++) {
            int left = mesh.edgeLeft[e];
            int right = mesh.edgeRight[e];
            if (left >= 0 && right >= 0 && this.facingCamera[left] == this.facingCamera[right]) {
                continue;
            }
            int from = mesh.edgeFrom[e];
            int to = mesh.edgeTo[e];
            // Both ends moved a hair towards the camera, so the line is not swallowed by the side it lies on.
            double ax = this.wx[from];
            double ay = this.wy[from];
            double az = this.wz[from];
            double bx = this.wx[to];
            double by = this.wy[to];
            double bz = this.wz[to];
            if (this.clipping) {
                // Cut off at the plane (see clip) before it is lifted: the part behind it is not drawn.
                if (!this.cutLine(ax, ay, az, bx, by, bz)) {
                    continue;
                }
                double[] ends = this.cutEnds;
                ax = ends[0];
                ay = ends[1];
                az = ends[2];
                bx = ends[3];
                by = ends[4];
                bz = ends[5];
            }
            double da = Math.sqrt(sq(this.camera.x - ax) + sq(this.camera.y - ay) + sq(this.camera.z - az));
            if (da > 1.0E-6) {
                double k = lift / da;
                ax += (this.camera.x - ax) * k;
                ay += (this.camera.y - ay) * k;
                az += (this.camera.z - az) * k;
            }
            double db = Math.sqrt(sq(this.camera.x - bx) + sq(this.camera.y - by) + sq(this.camera.z - bz));
            if (db > 1.0E-6) {
                double k = lift / db;
                bx += (this.camera.x - bx) * k;
                by += (this.camera.y - by) * k;
                bz += (this.camera.z - bz) * k;
            }
            this.line(this.light, ax, ay, az, bx, by, bz, EDGE_WIDTH * fine, this.material.edge(), edge);
            if (glowing > 0) {
                // Round things have short edges: a glow wider than the edge is long would stick out at every corner.
                double length = Math.sqrt(sq(bx - ax) + sq(by - ay) + sq(bz - az));
                this.line(this.glow, ax, ay, az, bx, by, bz, Math.min(HALO_WIDTH * fine, 0.9 * length),
                        this.material.glow(),
                        glowing);
            }
        }
        this.nearFade = false;
    }

    private static double sq(double value) {
        return value * value;
    }

    /**
     * How much a side's light changes with how it faces you, as a part of it: a little dimmer where you look straight
     * at it and a little brighter where it turns away towards its outline, so round things read round and every shape
     * seems to glow at its edges.
     *
     * @param face 1 when the side faces you straight, 0 when you see it edge-on
     */
    private static double sheen(double face) {
        return 0.88 + 0.24 * (1.0 - Mth.clamp(face, 0.0, 1.0));
    }

    /** A solid cube of the material, turned by {@code angle} about {@code axis}: a chunk thrown up by a blow. */
    public void chunk(Vec3 at, double size, Vec3 axis, double angle, double bright) {
        if (size <= 0.0) {
            return;
        }
        Vec3 forward = Vectors.spin(new Vec3(0, 0, 1), axis, angle);
        Vec3 up = Vectors.spin(Vectors.UP, axis, angle);
        this.model(CUBE, new Frame(at, forward.cross(up), up, forward, size), 1.0, bright);
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
     * The round parts of a shape drawn see-through: their sides as faint light that hides nothing behind it, and their
     * outline quieter than that of a solid construct. Only for a construct right in front of its owner's eyes.
     *
     * @param faint how strongly the sides show, 0 to 1
     */
    public void seeThrough(Shape shape, Frame frame, double faint, double bright) {
        this.faint = Math.max(1.0E-3, faint);
        for (Mesh mesh : shape.meshes()) {
            this.mesh(mesh, frame, 1.0, bright);
        }
        this.faint = 0.0;
    }

    /**
     * The streak of light a fast flyer leaves behind: a glowing ribbon along the way he came, fading out
     * behind him.
     *
     * @param points where the middle of his body was on the last ticks, newest first
     * @param own    true when it is your own and you look from your own eyes: the part at the camera is left out
     */
    public void trail(Vec3 head, Collection<Vec3> points, double strength, boolean own) {
        // Never longer than this, however fast he goes: a streak, not a rope across the sky.
        double reach = 7.0;
        double gone = 0.0;
        Vec3 last = head;
        int i = 0;
        for (Vec3 point : points) {
            if (i++ == 0) {
                continue;
            }
            double step = last.distanceTo(point);
            if (gone + step > reach) {
                point = last.add(point.subtract(last).scale((reach - gone) / Math.max(step, 1.0E-6)));
                step = reach - gone;
            }
            double t0 = gone / reach;
            gone += step;
            double t = gone / reach;
            double fade = strength * Math.pow(1.0 - t, 1.3);
            // Close to the camera it is left out, so it never smears across your screen.
            if (this.camera.distanceTo(point) > (own ? 2.5 : 1.5)) {
                this.line(this.glow, last, point, 0.32 * (1.0 - t0) + 0.05, this.material.glow(),
                        Colors.alpha(0.5 * fade));
                this.line(this.light, last, point, 0.07 * (1.0 - t0) + 0.02, this.material.edge(),
                        Colors.alpha(0.75 * fade));
            }
            if (gone >= reach) {
                return;
            }
            last = point;
        }
    }

    /**
     * The flame out of the back of a jet engine, a turbo booster or a rocket: a white-hot core inside a cone of bright
     * light that flickers, a glow round all of it, bright rings standing in the flame (the shock diamonds of an
     * afterburner) and a flare at the nozzle. Light, not a construct.
     *
     * @param from   the middle of the nozzle
     * @param way    the way the flame blows out, one long
     * @param length how long the flame is at full thrust, in blocks
     * @param radius how wide the nozzle is, in blocks
     * @param thrust 0 = out, 1 = full: the flame grows and brightens with it
     */
    public void exhaust(Vec3 from, Vec3 way, double length, double radius, double thrust) {
        double power = Mth.clamp(thrust, 0.0, 1.0);
        if (power <= 0.01 || way.lengthSqr() < 1.0E-8 || !this.visible(from, length + radius * 4.0)) {
            return;
        }
        Vec3 along = way.normalize();
        double flicker = 0.85 + 0.15 * Math.sin(this.time * 3.1) * Math.sin(this.time * 1.7 + 0.6);
        double reach = length * power * flicker;
        this.line(this.glow, from, from.add(along.scale(reach)), radius * 4.0 * (0.6 + 0.4 * power),
                this.material.glow(),
                Colors.alpha(0.55 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.8)), radius * 1.9, this.material.edge(),
                Colors.alpha(0.75 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.45)), radius * 0.9, this.material.hot(),
                Colors.alpha(0.95 * power));
        Vec3[] across = Vectors.across(along);
        int diamonds = 4;
        for (int k = 1; k <= diamonds; k++) {
            double t = k / (diamonds + 1.0);
            Vec3 at = from.add(along.scale(reach * t * 0.9));
            double ring = radius * (1.0 - 0.6 * t) * (0.9 + 0.1 * Math.sin(this.time * 2.3 + k));
            this.circle(at, across[0], across[1], ring, radius * 0.12, radius * 0.6,
                    Colors.alpha(0.8 * power * (1.0 - t)), Colors.alpha(0.4 * power * (1.0 - t)));
        }
        this.flare(from, radius * 1.6 * (0.7 + 0.3 * power), 0.8 * power);
    }

    /**
     * A chain of solid hard-light links from {@code from} to {@code to}, sagging {@code sag} blocks in its middle:
     * every link a flat oval ring, every other one turned a quarter, so they hook into each other. It runs out from
     * {@code from} link by link as it takes shape.
     *
     * @param link  how long one link is, in blocks
     * @param grown 0 to 1: how much of it has run out from {@code from}
     * @param apart 0 while it holds; above that it is breaking up, every link flying off on its own, gone at 1
     */
    public void chain(Vec3 from, Vec3 to, double sag, double link, double solid, double bright, double grown,
            double apart) {
        double length = from.distanceTo(to);
        if (length < 1.0E-3 || solid <= 0.0 || grown <= 0.0 || apart >= 1.0) {
            return;
        }
        int links = Math.max(2, (int) Math.ceil(length / (link * CHAIN_STEP)));
        int shown = (int) Math.ceil(links * Mth.clamp(grown, 0.0, 1.0));
        Vec3 last = from;
        for (int i = 1; i <= shown; i++) {
            double t = (double) i / links;
            Vec3 point = from.lerp(to, t).add(0.0, -4.0 * sag * t * (1.0 - t), 0.0);
            Vec3 along = point.subtract(last);
            Vec3 side = along.cross(Vectors.UP);
            Vec3 up = i % 2 == 0 || side.lengthSqr() < 1.0E-8 ? Vectors.UP : side;
            Frame frame = Frame.of(last.add(point).scale(0.5), along, up, link);
            if (apart > 0.0) {
                this.shatteredMesh(LINK, frame, i, apart, bright);
            } else {
                this.mesh(LINK, frame, solid, bright);
            }
            last = point;
        }
    }

    /**
     * A spark of light facing you: a soft glow, a bright heart and four short rays turning slowly.
     *
     * @param size how far its glow reaches, in blocks
     */
    public void flare(Vec3 at, double size, double strength) {
        if (strength <= 0.0) {
            return;
        }
        Vec3 view = this.camera.subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        int sides = 14;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2 * i / sides;
            double a1 = Math.PI * 2 * (i + 1) / sides;
            Vec3 r0 = across[0].scale(Math.cos(a0)).add(across[1].scale(Math.sin(a0)));
            Vec3 r1 = across[0].scale(Math.cos(a1)).add(across[1].scale(Math.sin(a1)));
            this.fan(this.glow, at, r0, r1, size, this.material.glow(), Colors.alpha(0.7 * strength));
            this.fan(this.light, at, r0, r1, size * 0.35, this.material.hot(), Colors.alpha(0.9 * strength));
        }
        for (int k = 0; k < 4; k++) {
            double angle = this.time * 0.05 + Math.PI * 0.5 * k;
            Vec3 ray = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle))).scale(size * 1.6);
            this.line(this.light, at.subtract(ray), at.add(ray), size * 0.12, this.material.edge(),
                    Colors.alpha(0.8 * strength));
        }
    }

    /** One slice of a round glow: bright at {@code at}, fading out to its edge. */
    private void fan(Layer layer, Vec3 at, Vec3 r0, Vec3 r1, double size, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        this.put(layer, at.x, at.y, at.z, rgb, alpha);
        this.put(layer, at.x + r0.x * size, at.y + r0.y * size, at.z + r0.z * size, rgb, 0);
        this.put(layer, at.x + r1.x * size, at.y + r1.y * size, at.z + r1.z * size, rgb, 0);
        this.put(layer, at.x, at.y, at.z, rgb, alpha);
    }

    /** A ring of light lying flat around {@code center}, like a line of light round a waist. */
    public void band(Vec3 center, double radius, double strength) {
        this.circle(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, 0.05, 0.3, Colors.alpha(0.95 * strength),
                Colors.alpha(0.45 * strength));
    }

    /** A ring of light around {@code center}, in the flat plane through the unit vectors {@code a} and {@code b}. */
    public void circle(Vec3 center, Vec3 a, Vec3 b, double radius, double width, double glowWidth, int edge,
            int halo) {
        int segments = 24;
        Vec3 last = center.add(a.scale(radius));
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            Vec3 next = center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
            this.line(this.light, last, next, width, this.material.edge(), edge);
            this.line(this.glow, last, next, glowWidth, this.material.glow(), halo);
            last = next;
        }
    }

    /**
     * One box of the fist: solid green sides, lit like a block (the top brightest, the underside darkest).
     * Only the edges where the shape really ends get a bright line (see {@link #rim}); lines over a side you
     * are looking straight at would turn the mass into a cage of wire. The light ripples along the construct
     * from back to front ({@code depth} is how far forward the box sits), harder the further it is charged.
     *
     * @param view   where the camera is, in the model's own space
     * @param width  how thick its edges are drawn, as a scale
     * @param solid  0 = gone, 1 = fully there; nothing else thins the mass out
     * @param bright how brightly this box burns next to the rest of the fist
     * @param charge how far the fist is charged: 0 = not at all, 1 = as far as it goes
     */
    private void box(double[][] model, ModelInfo info, int index, Vec3 view, double width, double solid,
            double bright, double depth, double charge, boolean glowing) {
        // Worked out in plain numbers from its corners (see corners), step by step the way the same sums on vectors
        // go, so it comes out exactly as it did with them: a box is drawn thousands of times a frame.
        double[] at = this.corner;
        Vec3 camera = this.camera;
        double ripple = 0.9 + (0.06 + 0.06 * charge) * Math.sin(this.time * 0.5 - depth * 4.0);
        int body = Colors.alpha(solid);
        // Its middle, to tell which way a side faces.
        double mx = (at[0] + at[21]) * 0.5;
        double my = (at[1] + at[22]) * 0.5;
        double mz = (at[2] + at[23]) * 0.5;
        double[] box = model[index];
        double fine = width * fine(box[3] - box[0], box[4] - box[1], box[5] - box[2]);
        this.lidCount = 0;
        for (int[] side : SIDES) {
            int p0 = 3 * side[0];
            int p1 = 3 * side[1];
            int p2 = 3 * side[2];
            // From the middle of the side to the eye, and the way the side faces (not one long).
            double ex = camera.x - (at[p0] + at[p2]) * 0.5;
            double ey = camera.y - (at[p0 + 1] + at[p2 + 1]) * 0.5;
            double ez = camera.z - (at[p0 + 2] + at[p2 + 2]) * 0.5;
            double ux = at[p1] - at[p0];
            double uy = at[p1 + 1] - at[p0 + 1];
            double uz = at[p1 + 2] - at[p0 + 2];
            double vx = at[p2] - at[p0];
            double vy = at[p2 + 1] - at[p0 + 1];
            double vz = at[p2 + 2] - at[p0 + 2];
            double nx = uy * vz - uz * vy;
            double ny = uz * vx - ux * vz;
            double nz = ux * vy - uy * vx;
            double across = Math.sqrt(nx * nx + ny * ny + nz * nz) * Math.sqrt(ex * ex + ey * ey + ez * ez);
            double face = across < 1.0E-12 ? 1.0 : Math.abs(nx * ex + ny * ey + nz * ez) / across;
            double lit = lit(nx, ny, nz, at[p0] - mx, at[p0 + 1] - my, at[p0 + 2] - mz);
            int rgb = this.mass((lit + this.ambient) * sheen(face) * ripple * bright);
            if (this.clipping) {
                this.cutCorners(this.mass, side, rgb, body, fine, solid, glowing);
            } else {
                this.quadCorners(this.mass, side, rgb, body);
            }
        }
        if (this.clipping) {
            this.lid(this.mass, body, ripple * bright);
        }
        int edge = Colors.alpha(EDGE * ripple * solid);
        int halo = glowing ? Colors.alpha(HALO * (1.0 + 0.4 * charge) * solid) : 0;
        // Every corner moved a hair towards the camera (see lifted), so an edge is not swallowed by its sides.
        double[] lifted = this.liftedCorner;
        double lift = 0.01 + 0.02 * fine;
        for (int i = 0; i < 24; i += 3) {
            double wx = camera.x - at[i];
            double wy = camera.y - at[i + 1];
            double wz = camera.z - at[i + 2];
            double length = Math.sqrt(wx * wx + wy * wy + wz * wz);
            double k = length < 1.0E-6 ? 0.0 : lift / length;
            lifted[i] = length < 1.0E-6 ? at[i] : at[i] + wx * k;
            lifted[i + 1] = length < 1.0E-6 ? at[i + 1] : at[i + 1] + wy * k;
            lifted[i + 2] = length < 1.0E-6 ? at[i + 2] : at[i + 2] + wz * k;
        }
        int edgeRgb = this.material.edge();
        int glowRgb = this.material.glow();
        for (int i = 0; i < 8; i++) {
            for (int axis = 0; axis < 3; axis++) {
                int bit = 1 << axis;
                if ((i & bit) != 0 || !rim(model, info, index, i, axis, view)) {
                    continue;
                }
                int a = 3 * i;
                int b = 3 * (i | bit);
                if (this.clipping) {
                    // Cut off at the plane (see clip) before it is lifted: the part behind it is not drawn.
                    if (this.cutLine(at[a], at[a + 1], at[a + 2], at[b], at[b + 1], at[b + 2])) {
                        double[] ends = this.cutEnds;
                        this.lift(ends, 0, lift);
                        this.lift(ends, 3, lift);
                        this.line(this.light, ends[0], ends[1], ends[2], ends[3], ends[4], ends[5],
                                EDGE_WIDTH * fine, edgeRgb, edge);
                        this.line(this.glow, ends[0], ends[1], ends[2], ends[3], ends[4], ends[5],
                                HALO_WIDTH * fine, glowRgb, halo);
                    }
                    continue;
                }
                this.line(this.light, lifted[a], lifted[a + 1], lifted[a + 2], lifted[b], lifted[b + 1],
                        lifted[b + 2], EDGE_WIDTH * fine, edgeRgb, edge);
                this.line(this.glow, lifted[a], lifted[a + 1], lifted[a + 2], lifted[b], lifted[b + 1],
                        lifted[b + 2], HALO_WIDTH * fine, glowRgb, halo);
            }
        }
    }

    /** A side of a box whose corners were worked out into {@link #corner}, by their numbers. */
    private void quadCorners(Layer layer, int[] side, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double[] at = this.corner;
        for (int k = 0; k < 4; k++) {
            int i = 3 * side[k];
            this.put(layer, at[i], at[i + 1], at[i + 2], rgb, this.faded(at[i], at[i + 1], at[i + 2], alpha));
        }
    }

    /**
     * How thick the lines along a part are drawn, as a part of the usual: a small part (a knob, a coin, a spoke) gets
     * finer lines, so its glow does not swallow it. It goes by the middle one of its three sizes, at scale 1.
     */
    public static double fine(double x, double y, double z) {
        double middle = Math.max(Math.min(x, y), Math.min(Math.max(x, y), z));
        return Mth.clamp(middle / FINE_SIZE, FINEST, 1.0);
    }

    /**
     * Whether an edge of a box is worth a line of light. It is when the shape ends there as you look at it:
     * one of the two sides that meet at the edge faces you and the other faces away. An edge with another
     * box right against it is left out as well, because there the mass simply goes on (worked out once per model, see
     * {@link ModelInfo}).
     *
     * @param corner the corner the edge starts at; {@code axis} is the way it runs (0 = x, 1 = y, 2 = z)
     * @param view   where the camera is, in the model's own space
     */
    private static boolean rim(double[][] model, ModelInfo info, int index, int corner, int axis, Vec3 view) {
        double[] box = model[index];
        int facing = 0;
        for (int other = 0; other < 3; other++) {
            if (other == axis) {
                continue;
            }
            boolean far = (corner & 1 << other) != 0;
            if (far == (axis(view, other) > (far ? box[3 + other] : box[other]))) {
                facing++;
            }
        }
        return facing == 1 && !info.covered()[index * 24 + corner * 3 + axis];
    }

    /** One of the three numbers of a point: 0 = x, 1 = y, 2 = z. */
    private static double axis(Vec3 point, int index) {
        return index == 0 ? point.x : index == 1 ? point.y : point.z;
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
    private static double lit(double nx, double ny, double nz, double dx, double dy, double dz) {
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
    private int mass(double light) {
        int rgb = Colors.shade(this.material.mass(), Math.min(1.0, light));
        return this.glare > 0.0 ? Colors.mix(rgb, this.material.hot(), (float) Math.min(0.75, this.glare
                * Math.min(1.0, light))) : rgb;
    }

    /** How far the solid shapes drawn from now on flare up towards white, 0 to 1 (0 once they are done). */
    public void glare(double amount) {
        this.glare = Mth.clamp(amount, 0.0, 1.0);
    }

    /**
     * How far the pieces of what breaks up from now on fly, next to how far they usually do (1 once it is done): a thing
     * as big as a plane is flung far further apart than a fist.
     */
    public void fling(double amount) {
        this.fling = Math.max(0.0, amount);
    }

    /**
     * How much light from within the solid shapes drawn from now on get on top of the sky's (0 once they are done): hard
     * light glows of itself, so a big construct seen from below (the belly of a plane high over you) is not left a dark
     * shadow, and every part and seam on its underside still reads.
     */
    public void ambient(double amount) {
        this.ambient = Math.max(0.0, amount);
    }

    /**
     * From now on everything solid drawn (the sides and edges of box models, round parts and shapes, whole or breaking
     * up, and see-through ones) is cut off at a plane: only what lies on the side {@code normal} points to is drawn,
     * and where the plane cuts through a solid side a bright line runs along the cut, {@code seam} strong (0 = none),
     * so a thing seems to come out of a surface of light; a solid part is closed with a lid of its own mass where it is
     * cut (see lid), so from behind the plane it is never seen hollow. Light (edge, sheet, flare, circle, trail,
     * haze...) is never cut, and neither are loose sides ({@link #side}, {@link #massQuad}). Lasts until
     * {@link #noClip}.
     *
     * @param point  a point on the plane
     * @param normal the way to the side that is kept, any length (with no length at all nothing is cut)
     */
    public void clip(Vec3 point, Vec3 normal, double seam) {
        double length = normal.length();
        if (length < 1.0E-9) {
            this.noClip();
            return;
        }
        this.clipping = true;
        this.clipX = point.x;
        this.clipY = point.y;
        this.clipZ = point.z;
        this.clipNormalX = normal.x / length;
        this.clipNormalY = normal.y / length;
        this.clipNormalZ = normal.z / length;
        this.seam = Math.max(0.0, seam);
    }

    /** Stops cutting (see {@link #clip}). */
    public void noClip() {
        this.clipping = false;
    }

    /** How far a point lies in front of the plane everything solid is cut off at (see {@link #clip}): below 0 = cut. */
    private double ahead(double x, double y, double z) {
        return (x - this.clipX) * this.clipNormalX + (y - this.clipY) * this.clipNormalY
                + (z - this.clipZ) * this.clipNormalZ;
    }

    /** A side of a round part (see {@link #quadAt}), by the numbers of its corners, cut off at the plane (see cut). */
    private void cutAt(Layer layer, int[] side, int rgb, int alpha, double fine, double solid, boolean halo) {
        double[] in = this.cutIn;
        for (int k = 0; k < 4; k++) {
            int i = side[k];
            in[3 * k] = this.wx[i];
            in[3 * k + 1] = this.wy[i];
            in[3 * k + 2] = this.wz[i];
        }
        this.cut(layer, rgb, alpha, fine, solid, halo);
    }

    /** A side of a box (see {@link #quadCorners}), by the numbers of its corners, cut off at the plane (see cut). */
    private void cutCorners(Layer layer, int[] side, int rgb, int alpha, double fine, double solid, boolean halo) {
        double[] at = this.corner;
        double[] in = this.cutIn;
        for (int k = 0; k < 4; k++) {
            int i = 3 * side[k];
            in[3 * k] = at[i];
            in[3 * k + 1] = at[i + 1];
            in[3 * k + 2] = at[i + 2];
        }
        this.cut(layer, rgb, alpha, fine, solid, halo);
    }

    /**
     * Draws the solid side whose four corners are in {@link #cutIn}, cut off at the plane (see {@link #clip}): whole
     * when it lies wholly in front of it, not at all when wholly behind it, and otherwise only what is in front (its
     * corners in front and the points where its edges cross the plane, walked round in order, as Sutherland and Hodgman
     * cut a polygon), with the seam of light along the cut. A side of three corners (its last one repeated) is cut the
     * same way.
     *
     * @param fine  how thick the lines along the part are drawn, as a scale (see {@link #fine})
     * @param solid how strongly its seam shows, 0 to 1
     * @param halo  false to leave out the soft glow along its seam
     */
    private void cut(Layer layer, int rgb, int alpha, double fine, double solid, boolean halo) {
        if (alpha <= 0) {
            return;
        }
        double[] in = this.cutIn;
        double[] ahead = this.cutAhead;
        boolean front = false;
        boolean behind = false;
        for (int k = 0; k < 4; k++) {
            ahead[k] = this.ahead(in[3 * k], in[3 * k + 1], in[3 * k + 2]);
            if (ahead[k] >= 0.0) {
                front = true;
            } else {
                behind = true;
            }
        }
        if (!front) {
            return;
        }
        if (!behind) {
            for (int i = 0; i < 12; i += 3) {
                this.put(layer, in[i], in[i + 1], in[i + 2], rgb, this.faded(in[i], in[i + 1], in[i + 2], alpha));
            }
            return;
        }
        // What is left: every corner in front, and where an edge runs from one side of the plane to the other, the
        // point where it crosses. A way round four corners crosses the plane an even number of times, at most four,
        // and then only two corners are in front: never more than six corners are left.
        double[] out = this.cutOut;
        double[] cross = this.cutCross;
        int corners = 0;
        int crossings = 0;
        for (int k = 0; k < 4; k++) {
            int next = (k + 1) % 4;
            int i = 3 * k;
            int j = 3 * next;
            double a = ahead[k];
            double b = ahead[next];
            if (a >= 0.0) {
                int o = 3 * corners++;
                out[o] = in[i];
                out[o + 1] = in[i + 1];
                out[o + 2] = in[i + 2];
            }
            if ((a >= 0.0) != (b >= 0.0)) {
                double t = a / (a - b);
                int o = 3 * corners++;
                out[o] = in[i] + (in[j] - in[i]) * t;
                out[o + 1] = in[i + 1] + (in[j + 1] - in[i + 1]) * t;
                out[o + 2] = in[i + 2] + (in[j + 2] - in[i + 2]) * t;
                int c = 3 * crossings++;
                cross[c] = out[o];
                cross[c + 1] = out[o + 1];
                cross[c + 2] = out[o + 2];
            }
        }
        // Drawn as quads fanning out from its first corner: (0, 1, 2, 3), (0, 3, 4, 5), and one left over with a
        // corner too few as a quad with its last corner repeated.
        for (int first = 1; first + 1 < corners; first += 2) {
            int last = first + 2 < corners ? first + 2 : first + 1;
            this.putCut(layer, 0, rgb, alpha);
            this.putCut(layer, first, rgb, alpha);
            this.putCut(layer, first + 1, rgb, alpha);
            this.putCut(layer, last, rgb, alpha);
        }
        for (int c = 0; c + 1 < crossings; c += 2) {
            this.lidCut(3 * c);
        }
        if (this.seam > 0.0) {
            for (int c = 0; c + 1 < crossings; c += 2) {
                this.seamLine(3 * c, fine, solid, halo);
            }
        }
    }

    /** Keeps the line a side was cut along, from the crossing at {@code from} in {@link #cutCross}, for the lid. */
    private void lidCut(int from) {
        if (6 * (this.lidCount + 1) > this.lidCuts.length) {
            this.lidCuts = Arrays.copyOf(this.lidCuts, this.lidCuts.length * 2);
            this.lidOutline = new int[this.lidCuts.length / 6];
            this.lidWaiting = new int[this.lidCuts.length / 6];
        }
        System.arraycopy(this.cutCross, from, this.lidCuts, 6 * this.lidCount, 6);
        this.lidCount++;
    }

    /**
     * Closes a solid part where the plane cut it (see clip) with a lid of its own mass, facing back into the plane, so
     * from behind the plane it is seen solid, never hollow. The lines its sides were cut along join up into outlines (a
     * part of more than one piece cuts into more than one): each is filled with a fan from its own middle to each of its
     * lines.
     *
     * @param bright how brightly the part burns
     */
    private void lid(Layer layer, int alpha, double bright) {
        int count = this.lidCount;
        this.lidCount = 0;
        if (count < 3 || alpha <= 0) {
            return;
        }
        int[] outline = this.lidOutline;
        Arrays.fill(outline, 0, count, -1);
        double nx = -this.clipNormalX;
        double ny = -this.clipNormalY;
        double nz = -this.clipNormalZ;
        double lit = light(nx, ny, nz) + this.ambient;
        for (int first = 0; first < count; first++) {
            if (outline[first] >= 0) {
                continue;
            }
            int lines = this.joinOutline(first, count);
            if (lines >= 3) {
                this.fillOutline(layer, first, count, alpha, lit * bright);
            }
        }
    }

    /**
     * Marks every line joined to line {@code first} (end to end, however far round) as one outline with it (see lid), and
     * tells how many lines it has.
     */
    private int joinOutline(int first, int count) {
        double[] cuts = this.lidCuts;
        int[] outline = this.lidOutline;
        int[] waiting = this.lidWaiting;
        outline[first] = first;
        waiting[0] = first;
        int waits = 1;
        int lines = 0;
        while (waits > 0) {
            int line = waiting[--waits];
            lines++;
            for (int other = 0; other < count; other++) {
                if (outline[other] < 0 && this.touches(cuts, 6 * line, 6 * other)) {
                    outline[other] = first;
                    waiting[waits++] = other;
                }
            }
        }
        return lines;
    }

    /** True when an end of the line at {@code a} in {@code cuts} is (all but) an end of the line at {@code b}. */
    private boolean touches(double[] cuts, int a, int b) {
        for (int i = a; i <= a + 3; i += 3) {
            for (int j = b; j <= b + 3; j += 3) {
                if (sq(cuts[i] - cuts[j]) + sq(cuts[i + 1] - cuts[j + 1]) + sq(cuts[i + 2] - cuts[j + 2]) < LID_JOIN) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Fills the outline of line {@code first} (see lid) with a fan from its middle, lit {@code light}. */
    private void fillOutline(Layer layer, int first, int count, int alpha, double light) {
        double[] cuts = this.lidCuts;
        int[] outline = this.lidOutline;
        double cx = 0.0;
        double cy = 0.0;
        double cz = 0.0;
        int ends = 0;
        for (int line = 0; line < count; line++) {
            if (outline[line] == first) {
                int i = 6 * line;
                cx += cuts[i] + cuts[i + 3];
                cy += cuts[i + 1] + cuts[i + 4];
                cz += cuts[i + 2] + cuts[i + 5];
                ends += 2;
            }
        }
        cx /= ends;
        cy /= ends;
        cz /= ends;
        double ex = this.camera.x - cx;
        double ey = this.camera.y - cy;
        double ez = this.camera.z - cz;
        double away = Math.sqrt(ex * ex + ey * ey + ez * ez);
        double face = away < 1.0E-6 ? 1.0
                : Math.abs(-this.clipNormalX * ex - this.clipNormalY * ey - this.clipNormalZ * ez) / away;
        int rgb = this.mass(light * sheen(face));
        int middle = this.faded(cx, cy, cz, alpha);
        for (int line = 0; line < count; line++) {
            if (outline[line] != first) {
                continue;
            }
            int i = 6 * line;
            // A triangle, as a quad with its last corner repeated.
            this.put(layer, cx, cy, cz, rgb, middle);
            this.put(layer, cuts[i], cuts[i + 1], cuts[i + 2], rgb, this.faded(cuts[i], cuts[i + 1], cuts[i + 2],
                    alpha));
            int end = this.faded(cuts[i + 3], cuts[i + 4], cuts[i + 5], alpha);
            this.put(layer, cuts[i + 3], cuts[i + 4], cuts[i + 5], rgb, end);
            this.put(layer, cuts[i + 3], cuts[i + 4], cuts[i + 5], rgb, end);
        }
    }

    /** One corner of what is left of a side after it is cut (see cut), into a layer, faded like any other. */
    private void putCut(Layer layer, int corner, int rgb, int alpha) {
        double[] out = this.cutOut;
        int i = 3 * corner;
        this.put(layer, out[i], out[i + 1], out[i + 2], rgb, this.faded(out[i], out[i + 1], out[i + 2], alpha));
    }

    /**
     * The seam of light where the plane cuts through a solid side (see {@link #clip}): a bright line from the crossing
     * at {@code from} in {@link #cutCross} to the one after it, with its glow, both a hair towards the camera the way
     * edges are, so the side they lie on does not swallow them.
     */
    private void seamLine(int from, double fine, double solid, boolean halo) {
        double[] ends = this.cutEnds;
        System.arraycopy(this.cutCross, from, ends, 0, 6);
        double lift = 0.01 + 0.02 * fine;
        this.lift(ends, 0, lift);
        this.lift(ends, 3, lift);
        double strength = solid * this.seam;
        this.line(this.light, ends[0], ends[1], ends[2], ends[3], ends[4], ends[5], EDGE_WIDTH * fine * SEAM_WIDTH,
                this.material.edge(), Colors.alpha(EDGE * strength));
        if (halo) {
            // The cut runs over many short sides of a round part: a glow wider than a piece of it is long would stick
            // out at every bend (see drawMesh).
            double length = Math.sqrt(sq(ends[3] - ends[0]) + sq(ends[4] - ends[1]) + sq(ends[5] - ends[2]));
            this.line(this.glow, ends[0], ends[1], ends[2], ends[3], ends[4], ends[5],
                    Math.min(HALO_WIDTH * fine * SEAM_WIDTH, 0.9 * length), this.material.glow(),
                    Colors.alpha(HALO * strength));
        }
    }

    /**
     * The line between two points cut off at the plane (see {@link #clip}), into {@link #cutEnds}: false when it lies
     * wholly behind it, otherwise true, with an end that lies behind it moved to where the line crosses the plane.
     */
    private boolean cutLine(double ax, double ay, double az, double bx, double by, double bz) {
        double[] ends = this.cutEnds;
        ends[0] = ax;
        ends[1] = ay;
        ends[2] = az;
        ends[3] = bx;
        ends[4] = by;
        ends[5] = bz;
        double a = this.ahead(ax, ay, az);
        double b = this.ahead(bx, by, bz);
        boolean frontA = a >= 0.0;
        boolean frontB = b >= 0.0;
        if (frontA && frontB) {
            return true;
        }
        if (!frontA && !frontB) {
            return false;
        }
        // The end behind it slides along the line towards the other one, up to the plane.
        int back = frontA ? 3 : 0;
        int front = 3 - back;
        double t = frontA ? b / (b - a) : a / (a - b);
        for (int k = 0; k < 3; k++) {
            ends[back + k] += (ends[front + k] - ends[back + k]) * t;
        }
        return true;
    }

    /** Moves the point at {@code i} in {@code points} {@code lift} blocks towards the camera, as edges are. */
    private void lift(double[] points, int i, double lift) {
        double x = this.camera.x - points[i];
        double y = this.camera.y - points[i + 1];
        double z = this.camera.z - points[i + 2];
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-6) {
            return;
        }
        double k = lift / length;
        points[i] += x * k;
        points[i + 1] += y * k;
        points[i + 2] += z * k;
    }

    /**
     * A glowing haze filling an egg shape round {@code center}, with {@code a}, {@code b} and {@code c} its three
     * half-axes: light added on top of whatever is behind it, strongest in its middle, where you look through the most of
     * it, and fading out softly towards its rim. The fireball of a blast, a cloud of light. Light, not a construct: it
     * hides nothing, and it is the one thing that may be see-through.
     */
    public void haze(Vec3 center, Vec3 a, Vec3 b, Vec3 c, int rgb, double strength) {
        double reach = Math.max(a.length(), Math.max(b.length(), c.length()));
        if (strength <= 0.0 || reach <= 1.0E-3 || !this.visible(center, reach)) {
            return;
        }
        int rings = 9;
        int slices = 16;
        Vec3[][] points = new Vec3[rings + 1][slices + 1];
        int[][] alphas = new int[rings + 1][slices + 1];
        for (int i = 0; i <= rings; i++) {
            double polar = Math.PI * i / rings;
            for (int j = 0; j <= slices; j++) {
                double around = Math.PI * 2.0 * j / slices;
                Vec3 out = a.scale(Math.sin(polar) * Math.cos(around)).add(b.scale(Math.cos(polar)))
                        .add(c.scale(Math.sin(polar) * Math.sin(around)));
                Vec3 at = center.add(out);
                points[i][j] = at;
                Vec3 toEye = this.camera.subtract(at);
                double away = toEye.length();
                double length = out.length();
                double facing = away < 1.0E-6 || length < 1.0E-9 ? 1.0 : Math.abs(out.dot(toEye)) / (length * away);
                alphas[i][j] = Colors.alpha(strength * Math.pow(facing, 1.6));
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < slices; j++) {
                Vec3 p0 = points[i][j];
                Vec3 p1 = points[i + 1][j];
                Vec3 p2 = points[i + 1][j + 1];
                Vec3 p3 = points[i][j + 1];
                this.put(this.glow, p0.x, p0.y, p0.z, rgb, alphas[i][j]);
                this.put(this.glow, p1.x, p1.y, p1.z, rgb, alphas[i + 1][j]);
                this.put(this.glow, p2.x, p2.y, p2.z, rgb, alphas[i + 1][j + 1]);
                this.put(this.glow, p3.x, p3.y, p3.z, rgb, alphas[i][j + 1]);
            }
        }
    }

    /** A line that always faces the camera: strongest along the middle, fading out to its sides. */
    private void line(Layer layer, Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(layer, a.x, a.y, a.z, b.x, b.y, b.z, width, rgb, alpha);
    }

    private void line(Layer layer, double ax, double ay, double az, double bx, double by, double bz, double width,
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

    /** A quad of four corners worked out into the room for a part (see {@link #room}), by their numbers. */
    private void quadAt(Layer layer, int a, int b, int c, int d, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        this.putAt(layer, a, rgb, alpha);
        this.putAt(layer, b, rgb, alpha);
        this.putAt(layer, c, rgb, alpha);
        this.putAt(layer, d, rgb, alpha);
    }

    private void putAt(Layer layer, int i, int rgb, int alpha) {
        double x = this.wx[i];
        double y = this.wy[i];
        double z = this.wz[i];
        this.put(layer, x, y, z, rgb, this.faded(x, y, z, alpha));
    }

    /** One corner, out in the world, into a layer: kept as where it is seen from the camera. */
    private void put(Layer layer, double x, double y, double z, int rgb, int alpha) {
        layer.add(x - this.camera.x, y - this.camera.y, z - this.camera.z, rgb, alpha);
    }

    /**
     * {@code alpha} at a point: while a construct's own shape is drawn, what comes close to the camera fades out, so a
     * big fist hanging right next to you never fills your screen.
     */
    private int faded(double x, double y, double z, int alpha) {
        if (!this.nearFade || this.hand) {
            return alpha;
        }
        double away = Math.sqrt(sq(x - this.camera.x) + sq(y - this.camera.y) + sq(z - this.camera.z));
        return (int) (alpha * Ease.smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }
}
