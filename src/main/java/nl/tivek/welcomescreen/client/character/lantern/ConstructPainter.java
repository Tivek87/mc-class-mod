package nl.tivek.welcomescreen.client.character.lantern;

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
import org.joml.Matrix4f;

/**
 * Draws Green Lantern's hard-light constructs: solid green shapes with bright edges and a glow around them,
 * and the beam of light from the ring that feeds them. A fist is built out of boxes (palm, fingers,
 * knuckles, thumb, wrist, forearm), the way the ring would shape it.
 *
 * <p>The engine under it is built for big models too (a jet the size of a house has thousands of sides):
 * <ul>
 * <li>every corner is kept as plain numbers in buffers that are used again frame after frame, so drawing makes
 * nothing new for each corner;</li>
 * <li>a shape that lies wholly outside the view is skipped at once (see {@link #visible}), measured by a ball round
 * it that is worked out once per shape;</li>
 * <li>which edges of a box model have another box against them is worked out once per model, not every frame;</li>
 * <li>far away, where a shape is only a few pixels big, the soft glow along its edges is left out (see
 * {@link #tiny}).</li>
 * </ul>
 * Besides boxes and the round parts of {@link Mesh} (with lofted bodies and wings for aircraft), it draws exhaust
 * flames ({@link #exhaust}) and chains of solid links ({@link #chain}).
 */
final class ConstructPainter {
    /**
     * The bright lines on a construct: its edges, the beam from the ring and the specks feeding it. They
     * are seen from both sides and never hide each other, and stay green even against a bright sky.
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
     * The solid green mass a construct is made of: it hides what is behind it and its own far sides, so a
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
    static final RenderType GLOW = RenderType.create("welcomescreen_hard_light_glow",
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

    static final Vec3 UP = new Vec3(0, 1, 0);
    // The ring's green, the bright green of the edges, and the almost white heart of fresh light.
    static final int GREEN = 0x3CE86A;
    static final int BRIGHT = 0x6CFF8E;
    static final int HOT = 0xE4FFEA;
    // The green the mass itself is made of: a shade brighter than the light around it, so it reads as
    // something you could knock on rather than as a shadow.
    static final int MASS_GREEN = 0x4BEF78;
    // How wide the fist model below is, from the thumb to the little finger: a fist of size 1 is one block wide.
    private static final double MODEL_WIDTH = 1.23;
    // Specks of light that keep landing on the fist while it charges.
    private static final int SPECKS = 12;
    // A construct's shape fades out closer to the camera than NEAR_CLEAR blocks, and is gone at NEAR_GONE: only
    // so that one sweeping right past your eyes never fills your screen.
    private static final double NEAR_GONE = 0.2;
    private static final double NEAR_CLEAR = 0.75;
    // How strongly the edges and the soft glow around them show.
    private static final double EDGE = 0.95;
    private static final double HALO = 0.28;
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
    // The beam from the ring is drawn in this many pieces, and a piece this close to the camera is left
    // out because it cannot be turned to face it.
    private static final int BEAM_STEPS = 16;
    private static final double BEAM_NEAR = 0.25;
    // How wide its bright core and its glow are where it meets a construct of scale 1, in blocks, and from
    // how far away from the camera on it is drawn at its full width.
    private static final double BEAM_CORE = 0.05;
    private static final double BEAM_HALO = 0.19;
    private static final double BEAM_FULL = 1.6;
    // A bolt keeps its beam back to the ring only while it is still this near the hand, in blocks.
    private static final double BOLT_BEAM = 6.0;
    // How long the beam of the attack button takes to shoot out of the ring, and how long the flash lasts as it breaks
    // loose, in ticks.
    private static final double BEAM_SHOOT = 2.5;
    private static final double BEAM_BURST = 6.0;

    /**
     * The fist, box by box, in blocks at scale 1: x to the right, y up, z the way it punches. A right
     * hand, palm down, so the thumb is on the left. The last number is how brightly that box glows.
     */
    private static final double[][] FIST = {
            // Back of the hand, with the four tendons standing out on it
            { -0.52, -0.30, -0.42, 0.52, 0.28, 0.16, 1.0 },
            { -0.46, 0.28, -0.36, -0.30, 0.32, 0.08, 1.05 },
            { -0.20, 0.28, -0.36, -0.06, 0.32, 0.08, 1.05 },
            { 0.06, 0.28, -0.36, 0.20, 0.32, 0.08, 1.05 },
            { 0.31, 0.26, -0.36, 0.45, 0.30, 0.08, 1.05 },
            // The fingers, from the index finger (next to the thumb) to the little finger. The first bone lies
            // forward on top (the face it punches with), the second curls under it a little further back, and the
            // tip tucks into the palm.
            { -0.53, -0.02, 0.16, -0.29, 0.28, 0.53, 1.0 },
            { -0.53, -0.32, 0.12, -0.29, -0.02, 0.47, 0.92 },
            { -0.51, -0.37, -0.06, -0.31, -0.28, 0.13, 0.85 },
            { -0.27, -0.02, 0.16, -0.02, 0.28, 0.57, 1.0 },
            { -0.27, -0.32, 0.12, -0.02, -0.02, 0.51, 0.92 },
            { -0.25, -0.37, -0.06, -0.04, -0.28, 0.13, 0.85 },
            { 0.00, -0.02, 0.16, 0.25, 0.28, 0.54, 1.0 },
            { 0.00, -0.32, 0.12, 0.25, -0.02, 0.48, 0.92 },
            { 0.02, -0.37, -0.06, 0.23, -0.28, 0.13, 0.85 },
            { 0.27, -0.01, 0.16, 0.49, 0.24, 0.48, 1.0 },
            { 0.27, -0.28, 0.12, 0.49, -0.01, 0.43, 0.92 },
            { 0.29, -0.33, -0.06, 0.47, -0.25, 0.13, 0.85 },
            // Knuckles
            { -0.50, 0.28, 0.08, -0.32, 0.38, 0.30, 1.1 },
            { -0.24, 0.28, 0.08, -0.05, 0.39, 0.33, 1.1 },
            { 0.03, 0.28, 0.08, 0.22, 0.38, 0.31, 1.1 },
            { 0.30, 0.24, 0.08, 0.46, 0.33, 0.27, 1.1 },
            // Thumb: out of the side of the hand, folded across the front of the first two fingers
            { -0.72, -0.28, -0.30, -0.52, 0.06, 0.10, 1.0 },
            { -0.74, -0.34, 0.08, -0.50, -0.06, 0.42, 1.0 },
            { -0.64, -0.40, 0.42, -0.08, -0.16, 0.62, 1.05 },
            { -0.30, -0.36, 0.62, -0.10, -0.20, 0.635, 1.25 },
            // Wrist, a bright cuff, and the forearm fading out towards the ring
            { -0.38, -0.27, -0.72, 0.38, 0.25, -0.42, 0.9 },
            { -0.44, -0.33, -0.86, 0.44, 0.31, -0.72, 1.2 },
            { -0.34, -0.25, -1.25, 0.34, 0.23, -0.86, 0.6 } };
    // Where the forearm ends: the beam from the ring comes in there.
    private static final double BACK = -1.25;
    /**
     * His ring, on the middle finger of the fist (only a right fist wears it): a band round the finger just past its
     * knuckle, a setting on top and the gem in it.
     */
    static final Shape FIST_RING = new Shape(new double[][] { { -0.285, 0.28, 0.36, -0.005, 0.31, 0.44, 1.2 } },
            Mesh.cylinder(10, 0.08, 0.30, 0.335, 1.3).moved(-0.145, 0.0, 0.40),
            Mesh.ball(10, 6, 0.065, 1.6).scaled(1.0, 0.6, 1.0).moved(-0.145, 0.34, 0.40));
    // How wide the bolt (see BOLT_SHAPE) is at its widest, so a bolt of size 1 is one block wide.
    private static final double BOLT_WIDTH = 0.64;
    /** A cube of one block round its middle: chunks thrown up by a blow. */
    private static final double[][] CUBE = { { -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 1.0 } };
    // The dome: how many rings and slices its sphere is cut into.
    private static final int DOME_RINGS = 12;
    private static final int DOME_SLICES = 24;
    // The ram cone: how many sides it has, and its shape as rings along its length (how far ahead of his middle,
    // and how wide there, in blocks).
    private static final int RAM_SIDES = 16;
    private static final double[][] RAM = { { -0.45, 0.92 }, { 0.35, 0.8 }, { 1.05, 0.52 }, { 1.55, 0.2 },
            { 1.8, 0.0 } };
    // How strongly the outline of a see-through shape shows, next to that of a solid one.
    private static final double SEE_THROUGH_EDGE = 0.55;
    // The light on the solid mass (see light): what every side gets, what the sky adds from above, and what the sun
    // adds to the sides that face it, from high over one corner of the world.
    private static final double SKY_FLOOR = 0.42;
    private static final double SKY = 0.3;
    private static final double SUN_LIGHT = 0.3;
    private static final Vec3 SUN = new Vec3(0.45, 0.75, -0.5).normalize();
    /**
     * The shield, round its middle with its face towards +z and a radius of 1 at scale 1: a body that bulges a little
     * to the front, a round rim, a groove turned into its face, the lantern emblem raised in its middle, and a grip on
     * its back.
     */
    private static final Shape SHIELD = Shape.of(
            Mesh.lathe(40, 1.0, 0.0, -0.07, 0.90, -0.07, 0.97, -0.03, 0.97, 0.03, 0.90, 0.05, 0.60, 0.11, 0.30, 0.14,
                    0.0, 0.15).alongZ(),
            Mesh.torus(48, 8, 0.96, 0.06, 1.2).alongZ(),
            Mesh.torus(48, 6, 0.70, 0.025, 1.3).alongZ().moved(0.0, 0.0, 0.095),
            Mesh.torus(32, 6, 0.24, 0.045, 1.45).alongZ().moved(0.0, 0.0, 0.155),
            Mesh.box(-0.30, 0.31, 0.10, 0.30, 0.39, 0.20, 1.45),
            Mesh.box(-0.30, -0.39, 0.10, 0.30, -0.31, 0.20, 1.45),
            Mesh.box(-0.08, -0.30, -0.13, 0.08, 0.30, -0.07, 1.0));
    /** The rivets round the face of the shield, drawn on their own so the ring of them can turn slowly. */
    private static final Shape SHIELD_RIVETS = Shape.of(rivets());
    /** The bolt the ring shoots, along z: a pointed nose, a round body, and a tail narrowing behind it. */
    private static final Shape BOLT_SHAPE = Shape.of(Mesh.lathe(10, 1.15, 0.0, -0.75, 0.18, -0.75, 0.24, -0.55,
            0.32, -0.40, 0.32, 0.20, 0.24, 0.40, 0.10, 0.55, 0.0, 0.60).alongZ());
    /**
     * One link of a chain (see {@link #chain}): a flat oval ring lying round y with its long way along z, one long and
     * three quarters of that wide at scale 1.
     */
    private static final Mesh LINK = Mesh.torus(12, 6, 0.3, 0.075, 1.15).scaled(1.0, 1.0, 1.335);
    // How far apart two links sit, as a part of a link's length: less than one, so they hook into each other.
    private static final double CHAIN_STEP = 0.72;
    // Where the bolt's nose and tail end, along z at scale 1.
    private static final double BOLT_NOSE = 0.60;
    private static final double BOLT_TAIL = -0.75;
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
    record Frame(Vec3 center, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        /** A point of the model (in blocks at scale 1) out in the world. */
        Vec3 at(double x, double y, double z) {
            return this.center.add(this.right.scale(x * this.scale)).add(this.up.scale(y * this.scale))
                    .add(this.forward.scale(z * this.scale));
        }

        /** The other way around: where a point of the world lies in the model. */
        Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.center);
            return new Vec3(way.dot(this.right) / (this.right.lengthSqr() * this.scale),
                    way.dot(this.up) / (this.up.lengthSqr() * this.scale),
                    way.dot(this.forward) / (this.forward.lengthSqr() * this.scale));
        }

        /** The way a side of the model faces (one long, in the model) as a way in the world, one long. */
        Vec3 normal(Vec3 model) {
            Vec3 way = this.right.scale(model.x / this.right.lengthSqr())
                    .add(this.up.scale(model.y / this.up.lengthSqr()))
                    .add(this.forward.scale(model.z / this.forward.lengthSqr()));
            double length = way.length();
            return length < 1.0E-12 ? Vec3.ZERO : way.scale(1.0 / length);
        }

        /** The same frame with its middle at a point of the model: for a part that turns about its own middle. */
        Frame moved(double x, double y, double z) {
            return new Frame(this.at(x, y, z), this.right, this.up, this.forward, this.scale);
        }

        /**
         * The same frame turned by {@code angle} (radians) about a line through a point of the model that runs along
         * (ax, ay, az) in the model: a lid on its hinge, a door, a jaw. The angle turns the way it would in the model's
         * own terms (counter-clockwise looking down the line from its tip), also when the frame is a mirror image, as
         * one with his right as its x is.
         */
        Frame turned(double px, double py, double pz, double ax, double ay, double az, double angle) {
            Vec3 axis = this.right.scale(ax).add(this.up.scale(ay)).add(this.forward.scale(az)).normalize();
            double turn = this.right.cross(this.up).dot(this.forward) < 0.0 ? -angle : angle;
            Vec3 pivot = this.at(px, py, pz);
            return new Frame(pivot.add(spin(this.center.subtract(pivot), axis, turn)), spin(this.right, axis, turn),
                    spin(this.up, axis, turn), spin(this.forward, axis, turn), this.scale);
        }

        /** The same frame squashed or stretched along its own right, up and forward (1 = as it is). */
        Frame stretched(double x, double y, double z) {
            return new Frame(this.center, this.right.scale(x), this.up.scale(y), this.forward.scale(z), this.scale);
        }

        /** How long its longest way is: 1, unless it is stretched. */
        double stretch() {
            return Math.sqrt(Math.max(this.right.lengthSqr(), Math.max(this.up.lengthSqr(), this.forward.lengthSqr())));
        }

        /**
         * A frame at {@code center} that faces {@code forward}, its up as near {@code up} as it can be and its right
         * worked out from the two, the way every construct stands (see the note on handedness in the project rules).
         */
        static Frame of(Vec3 center, Vec3 forward, Vec3 up, double scale) {
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
    record Shape(double[][] boxes, Mesh... meshes) {
        private static final double[][] NO_BOXES = new double[0][];

        /** A shape of round or slanted parts only. */
        static Shape of(Mesh... meshes) {
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

    ConstructPainter(PoseStack pose, Vec3 camera, float time) {
        this(pose, camera, time, null);
    }

    /**
     * @param frustum what is in view: shapes wholly outside it are skipped; null draws everything
     */
    ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum) {
        this(pose, camera, time, frustum, false);
    }

    private ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, boolean hand) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
        this.frustum = frustum;
        this.hand = hand;
    }

    /**
     * A painter for constructs in your own hands in first person, drawn along with your hands: everything is given in
     * blocks in front of your eyes (x to the right, y up, -z ahead), the camera sits at 0, and nothing close by fades
     * out, since that is exactly where your hands are.
     */
    static ConstructPainter hand(PoseStack pose, float time) {
        return new ConstructPainter(pose, Vec3.ZERO, time, null, true);
    }

    /** Draws everything of this frame: the solid shapes first, then their lines and the glow on top. */
    void finish(MultiBufferSource.BufferSource buffers) {
        this.draw(buffers, MASS, this.mass);
        this.draw(buffers, this.hand ? HAND_LIGHT : LIGHT, this.light);
        this.draw(buffers, this.hand ? HAND_GLOW : GLOW, this.glow);
    }

    private void draw(MultiBufferSource.BufferSource buffers, RenderType type, Layer layer) {
        if (layer.count > 0) {
            VertexConsumer buffer = buffers.getBuffer(type);
            float[] at = layer.at;
            int[] color = layer.color;
            for (int i = 0; i < layer.count; i++) {
                int argb = color[i];
                buffer.addVertex(this.matrix, at[3 * i], at[3 * i + 1], at[3 * i + 2]).setColor(argb >> 16 & 0xFF,
                        argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
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
    boolean visible(Vec3 center, double radius) {
        return this.frustum == null || this.frustum.isVisible(new AABB(center.x - radius, center.y - radius,
                center.z - radius, center.x + radius, center.y + radius, center.z + radius));
    }

    /** True when a ball this big round {@code center} is only a few pixels on screen: its soft glow is left out. */
    private boolean tiny(Vec3 center, double radius) {
        double away = center.distanceTo(this.camera);
        return away > 1.0 && radius / away < TINY;
    }

    /**
     * Draws a fist punching along {@code facing}, and the beam from the ring that feeds it. It has no build-up
     * of its own: it simply fades in where it is. While it charges, lumps of light run down the beam into it
     * and specks land all over it; once it is fully charged it throbs.
     *
     * @param size   how wide it is, in blocks
     * @param solid  0 = gone, 1 = fully there
     * @param charge how far it is charged: 0 = not at all, 1 = as far as it goes
     * @param held   true while it charges beside you, false once it flies
     * @param ring   where the ring of the one who made it is, or null when they are out of sight
     */
    void fist(Vec3 center, Vec3 facing, double size, double solid, double charge, boolean held,
            @Nullable Vec3 ring) {
        Vec3 right = facing.cross(UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Frame frame = new Frame(center, right, right.cross(facing), facing, size / MODEL_WIDTH);
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double grown = held ? Mth.clamp(charge, 0.0, 1.0) : 0.0;
        boolean full = grown >= 0.99;
        double throb = full ? 0.85 + 0.25 * Math.sin(this.time * 0.6) : 1.0;
        Vec3[] corners = new Vec3[8];
        Vec3 view = frame.local(this.camera);
        ModelInfo info = info(FIST);
        this.nearFade = true;
        for (int b = 0; b < FIST.length; b++) {
            double[] box = FIST[b];
            for (int i = 0; i < 8; i++) {
                corners[i] = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4],
                        box[(i & 4) == 0 ? 2 : 5]);
            }
            this.box(FIST, info, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * throb,
                    (box[2] + box[5]) * 0.5, grown, true);
        }
        this.nearFade = false;
        this.shape(FIST_RING, frame, strength, throb);
        if (ring == null) {
            return;
        }
        Vec3 end = frame.at(0.0, 0.0, BACK);
        this.beam(ring, end, strength, frame.scale());
        if (held && !full) {
            this.feed(frame);
        }
    }

    /**
     * Any shape made of boxes, the way the fist is ({@link #FIST}: six numbers per box and how brightly it burns),
     * at {@code frame}: solid green sides, bright lines where the shape ends, and a glow. What comes close to the
     * camera fades out.
     *
     * @param solid  0 = gone, 1 = fully there
     * @param bright how brightly it burns, on top of each box's own brightness
     */
    void model(double[][] model, Frame frame, double solid, double bright) {
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
        Vec3[] corners = new Vec3[8];
        Vec3 view = frame.local(this.camera);
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            for (int i = 0; i < 8; i++) {
                corners[i] = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4],
                        box[(i & 4) == 0 ? 2 : 5]);
            }
            this.box(model, info, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * bright,
                    (box[2] + box[5]) * 0.5, 0.0, halo);
        }
        this.nearFade = false;
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

    /** A solid side of hard light, from four corners (for shapes that are not boxes). */
    void side(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, double solid) {
        this.quad(this.mass, p0, p1, p2, p3, rgb, alpha(solid));
    }

    /** A bright line of light from {@code a} to {@code b}, with its glow around it. */
    void edge(Vec3 a, Vec3 b, double width, double strength) {
        this.line(this.light, a, b, width, BRIGHT, alpha(EDGE * strength));
        this.line(this.glow, a, b, width * 3.0, GREEN, alpha(HALO * strength));
    }

    /**
     * A sheet of light between four corners, each as strong as given (0 to 1): the streak a swung blade leaves in the
     * air. Light, not a construct: it hides nothing.
     */
    void sheet(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double a0, double a1, double a2, double a3) {
        this.put(this.glow, p0.x, p0.y, p0.z, GREEN, alpha(0.6 * a0));
        this.put(this.glow, p1.x, p1.y, p1.z, GREEN, alpha(0.6 * a1));
        this.put(this.glow, p2.x, p2.y, p2.z, GREEN, alpha(0.6 * a2));
        this.put(this.glow, p3.x, p3.y, p3.z, GREEN, alpha(0.6 * a3));
        this.put(this.light, p0.x, p0.y, p0.z, BRIGHT, alpha(0.3 * a0));
        this.put(this.light, p1.x, p1.y, p1.z, BRIGHT, alpha(0.3 * a1));
        this.put(this.light, p2.x, p2.y, p2.z, BRIGHT, alpha(0.3 * a2));
        this.put(this.light, p3.x, p3.y, p3.z, BRIGHT, alpha(0.3 * a3));
    }

    /**
     * A shape made of boxes breaking up, still solid: every box flies out from the middle of the shape, tumbling and
     * dropping as it goes, and shrinks away to nothing. Nothing of it fades out: a construct is never see-through.
     *
     * @param apart 0 = still whole, 1 = gone
     */
    void shattered(double[][] model, Frame frame, double apart, double bright) {
        double gone = Mth.clamp(apart, 0.0, 1.0);
        double left = 1.0 - gone;
        if (left <= 0.0) {
            return;
        }
        Vec3[] corners = new Vec3[8];
        Vec3 view = frame.local(this.camera);
        double size = Math.max(1.0, frame.scale()) * this.fling;
        ModelInfo info = info(model);
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            Vec3 middle = frame.at((box[0] + box[3]) * 0.5, (box[1] + box[4]) * 0.5, (box[2] + box[5]) * 0.5);
            Vec3 out = middle.subtract(frame.center());
            Vec3 scatter = direction(b, 7);
            Vec3 way = this.awayFromEye(middle,
                    out.lengthSqr() > 1.0E-6 ? out.normalize().add(scatter.scale(0.6)).normalize() : scatter);
            double speed = (1.2 + 1.8 * noise(b, 7, 3)) * size;
            // Out and up at first, then down: thrown pieces.
            Vec3 moved = middle.add(way.scale(speed * gone)).add(0.0, (1.2 * gone - 2.6 * gone * gone) * size, 0.0);
            Vec3 axis = direction(b, 9);
            double turn = gone * (1.5 + 3.0 * noise(b, 9, 2));
            for (int i = 0; i < 8; i++) {
                Vec3 corner = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4], box[(i & 4) == 0 ? 2 : 5]);
                corners[i] = moved.add(spin(corner.subtract(middle), axis, turn).scale(left));
            }
            this.box(model, info, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), 1.0, box[6] * bright,
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
    void shape(Shape shape, Frame frame, double solid, double bright) {
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
    void shattered(Shape shape, Frame frame, double apart, double bright) {
        if (shape.boxes().length > 0) {
            this.shattered(shape.boxes(), frame, apart, bright);
        }
        for (int k = 0; k < shape.meshes().length; k++) {
            this.shatteredMesh(shape.meshes()[k], frame, shape.boxes().length + k, apart, bright);
        }
    }

    /** A round or slanted part (see {@link Mesh}) at {@code frame}, drawn the way {@link #model} draws boxes. */
    void mesh(Mesh mesh, Frame frame, double solid, double bright) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0 || mesh.points.length == 0) {
            return;
        }
        Vec3 middle = frame.at(mesh.boundX, mesh.boundY, mesh.boundZ);
        double reach = mesh.boundRadius * frame.scale() * frame.stretch();
        if (!this.visible(middle, reach)) {
            return;
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
        Vec3 scatter = direction(piece, 7);
        Vec3 way = this.awayFromEye(middle,
                out.lengthSqr() > 1.0E-6 ? out.normalize().add(scatter.scale(0.6)).normalize() : scatter);
        double speed = (1.2 + 1.8 * noise(piece, 7, 3)) * size;
        Vec3 moved = middle.add(way.scale(speed * gone)).add(0.0, (1.2 * gone - 2.6 * gone * gone) * size, 0.0);
        Vec3 axis = direction(piece, 9);
        double turn = gone * (1.5 + 3.0 * noise(piece, 9, 2));
        this.room(mesh.points.length, mesh.sides.length);
        for (int i = 0; i < mesh.points.length; i++) {
            Vec3 point = mesh.points[i];
            Vec3 at = moved.add(spin(frame.at(point.x, point.y, point.z).subtract(middle), axis, turn).scale(left));
            this.wx[i] = at.x;
            this.wy[i] = at.y;
            this.wz[i] = at.z;
        }
        for (int k = 0; k < mesh.sides.length; k++) {
            Vec3 normal = spin(frame.normal(mesh.normals[k]), axis, turn);
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
        int body = alpha(faint ? this.faint * solid : solid);
        this.nearFade = true;
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
            double light = light(x, y, z) * sheen(face) * ripple * bright * mesh.bright[s];
            this.quadAt(sides, side[0], side[1], side[2], side[3], this.mass(light), body);
        }
        double quiet = faint ? SEE_THROUGH_EDGE : 1.0;
        int edge = alpha(EDGE * solid * quiet);
        int glowing = halo ? alpha(HALO * solid * quiet) : 0;
        double fine = width * mesh.fine;
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
            this.line(this.light, ax, ay, az, bx, by, bz, EDGE_WIDTH * fine, BRIGHT, edge);
            if (glowing > 0) {
                // Round things have short edges: a glow wider than the edge is long would stick out at every corner.
                double length = Math.sqrt(sq(bx - ax) + sq(by - ay) + sq(bz - az));
                this.line(this.glow, ax, ay, az, bx, by, bz, Math.min(HALO_WIDTH * fine, 0.9 * length), GREEN,
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

    /** A solid cube of hard light, turned by {@code angle} about {@code axis}: a chunk thrown up by a blow. */
    void chunk(Vec3 at, double size, Vec3 axis, double angle, double bright) {
        if (size <= 0.0) {
            return;
        }
        Vec3 forward = spin(new Vec3(0, 0, 1), axis, angle);
        Vec3 up = spin(UP, axis, angle);
        this.model(CUBE, new Frame(at, forward.cross(up), up, forward, size), 1.0, bright);
    }

    /** The boxes of the fist, for other constructs that are made of fists. */
    static double[][] fistModel() {
        return FIST;
    }

    /** The camera, where everything is drawn from. */
    Vec3 camera() {
        return this.camera;
    }

    /** The time of this frame, in ticks, for everything that ripples. */
    float time() {
        return this.time;
    }

    /**
     * One bolt of hard light on its way: a small solid bullet with its nose along {@code facing}, and the
     * beam back to the ring for as long as you can still see both.
     *
     * @param size  how wide it is, in blocks
     * @param solid 0 = gone, 1 = fully there
     * @param ring  where the ring that shot it is, or null when its owner is out of sight
     */
    void bolt(Vec3 center, Vec3 facing, double size, double solid, @Nullable Vec3 ring) {
        Vec3 right = facing.cross(UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        // The solid heart of it is small; most of what you see is the light around it and the streak behind it,
        // so it reads as a shot of energy and never as a green block flying past your eyes.
        Frame frame = new Frame(center, right, right.cross(facing), facing, size * 0.5 / BOLT_WIDTH);
        double strength = Mth.clamp(solid, 0.0, 1.0);
        this.shape(BOLT_SHAPE, frame, strength, 1.0);
        this.nearFade = true;
        Vec3 nose = frame.at(0.0, 0.0, BOLT_NOSE + 0.15);
        Vec3 tail = frame.at(0.0, 0.0, BOLT_TAIL);
        this.line(this.light, tail, nose, size * 0.3, HOT, alpha(0.9 * strength));
        this.line(this.glow, tail, nose, size * 1.3, GREEN, alpha(0.6 * strength));
        // The streak it leaves, fading out behind it.
        Vec3 last = tail;
        for (int i = 1; i <= 4; i++) {
            Vec3 next = tail.subtract(facing.scale(0.35 * i));
            double fade = 1.0 - i / 5.0;
            this.line(this.glow, last, next, size * (0.9 * fade + 0.2), GREEN, alpha(0.45 * fade * strength));
            this.line(this.light, last, next, size * 0.18 * fade, BRIGHT, alpha(0.6 * fade * strength));
            last = next;
        }
        this.nearFade = false;
        // Only while it is still near the hand: a beam stretching across half the world behind every bolt
        // would turn a burst of them into a fan of stripes.
        if (ring != null && ring.distanceToSqr(center) < BOLT_BEAM * BOLT_BEAM) {
            this.beam(ring, frame.at(0.0, 0.0, BOLT_TAIL), strength * 0.7, frame.scale());
        }
    }

    /**
     * The shield: a round shield of hard light standing across {@code facing} (see {@link #SHIELD}), as solid as any
     * construct. The ring of rivets round its face turns slowly, a glint sweeps over it now and then, and a hit sends a
     * ripple of light out over its face. Only from the eyes of the one holding it is it left see-through, because it
     * hangs right in front of them.
     *
     * @param size  how wide it is, in blocks
     * @param solid 0 = gone, 1 = fully there; it also opens out from the middle as this grows
     * @param flash 1 right after a hit landed on it, 0 otherwise
     * @param ring  where the ring holding it is, or null when its owner is out of sight
     * @param own   true when it is your own and you look out through it
     */
    void shield(Vec3 center, Vec3 facing, double size, double solid, double flash, @Nullable Vec3 ring,
            boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        Vec3 forward = facing.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : facing.normalize();
        Vec3 right = forward.cross(UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward);
        double radius = size * 0.5 * (0.55 + 0.45 * strength);
        Frame frame = new Frame(center, right, up, forward, radius);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        double burn = 1.0 + 0.5 * hit;
        Frame rivets = frame.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, this.time * 0.02);
        if (own) {
            // Your own shield stays up while you walk and fight, right in front of your eyes: you see it by its
            // outline and the faint glow of its face, and look straight through it. Everyone else sees it solid.
            this.seeThrough(SHIELD, frame, (0.1 + 0.12 * hit) * strength, burn);
            this.seeThrough(SHIELD_RIVETS, rivets, (0.1 + 0.12 * hit) * strength, burn);
        } else {
            this.shape(SHIELD, frame, 1.0, burn);
            this.shape(SHIELD_RIVETS, rivets, 1.0, burn);
        }
        Vec3 face = frame.at(0.0, 0.0, 0.17);
        double quiet = own ? 0.5 : 1.0;
        if (hit > 0.0) {
            this.circle(face, right, up, radius * (0.2 + 0.8 * (1.0 - hit)), 0.05, 0.25, alpha(hit * quiet),
                    alpha(0.5 * hit * quiet));
        }
        // Now and then a glint sweeps across its face, from its top left to its bottom right.
        double sweep = Mth.frac(this.time / 70.0) * 4.0 - 1.2;
        if (Math.abs(sweep) < 1.0) {
            double reach = Math.sqrt(1.0 - sweep * sweep) * 0.85;
            Vec3 across = right.add(up).normalize();
            Vec3 along = right.subtract(up).normalize();
            Vec3 middle = face.add(along.scale(sweep * radius * 0.85));
            this.line(this.light, middle.subtract(across.scale(reach * radius)), middle.add(across.scale(reach
                    * radius)), radius * 0.03, HOT, alpha(0.5 * strength * quiet * (1.0 - Math.abs(sweep))));
        }
        if (ring != null) {
            this.beam(ring, frame.at(0.0, 0.0, -0.13), strength, radius);
        }
    }

    /** The rivets round the face of the shield. */
    private static Mesh[] rivets() {
        Mesh[] rivets = new Mesh[12];
        for (int i = 0; i < rivets.length; i++) {
            double angle = Math.PI * 2.0 * i / rivets.length;
            rivets[i] = Mesh.ball(6, 4, 0.035, 1.4).moved(0.84 * Math.cos(angle), 0.84 * Math.sin(angle), 0.065);
        }
        return rivets;
    }

    /**
     * The round parts of a shape drawn see-through: their sides as faint light that hides nothing behind it, and their
     * outline quieter than that of a solid construct. Only for a construct right in front of its owner's eyes.
     *
     * @param faint how strongly the sides show, 0 to 1
     */
    private void seeThrough(Shape shape, Frame frame, double faint, double bright) {
        this.faint = Math.max(1.0E-3, faint);
        for (Mesh mesh : shape.meshes()) {
            this.mesh(mesh, frame, 1.0, bright);
        }
        this.faint = 0.0;
    }

    /** While the fist grows in your hand, specks of light keep flying in and landing all over it. */
    private void feed(Frame frame) {
        for (int k = 0; k < SPECKS; k++) {
            double cycle = this.time / 14.0 + noise(k, 11, 0);
            int round = Mth.floor(cycle);
            double[] box = FIST[(int) (noise(k, round, 1) * FIST.length)];
            Vec3 target = frame.at(Mth.lerp(noise(k, round, 2), box[0], box[3]),
                    Mth.lerp(noise(k, round, 3), box[1], box[4]), Mth.lerp(noise(k, round, 4), box[2], box[5]));
            this.speck(target, direction(k, round), frame.forward(), frame.scale() * 0.8, cycle - round,
                    frame.scale());
        }
    }

    /**
     * One speck of light flying in onto {@code target} from {@code direction}, swirling around {@code axis}:
     * fast at first, slowing down as it lands.
     *
     * @param t how far along it is: 0 = it sets off, 1 = it lands
     */
    private void speck(Vec3 target, Vec3 direction, Vec3 axis, double distance, double t, double scale) {
        double before = Math.max(0.0, t - 0.12);
        Vec3 at = target.add(spin(direction, axis, 2.0 * t).scale(distance * (1.0 - t) * (1.0 - t)));
        Vec3 tail = target.add(spin(direction, axis, 2.0 * before)
                .scale(distance * (1.0 - before) * (1.0 - before)));
        double fade = Math.min(1.0, t * 4.0);
        this.line(this.light, tail, at, 0.025 * scale, BRIGHT, alpha(0.9 * fade));
        this.line(this.glow, tail, at, 0.1 * scale, GREEN, alpha(0.5 * fade));
    }

    /** The same number between 0 and 1 every frame for the same three numbers. */
    static double noise(int a, int b, int c) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }

    /** A direction of its own for every speck (or piece, or crack). */
    static Vec3 direction(int a, int b) {
        double yaw = noise(a, b, 0) * Math.PI * 2;
        double y = noise(a, b, 1) * 2.0 - 1.0;
        double flat = Math.sqrt(1.0 - y * y);
        return new Vec3(Math.cos(yaw) * flat, y, Math.sin(yaw) * flat);
    }

    /**
     * The beam of light from the ring to what it made: a white-hot thread leaving the stone with a spark of light
     * on it, swelling to a cord of green light where it meets the construct, with light running down it into the
     * construct and two fine strands winding around it. It stays as long as the construct does: the light always
     * hangs on the ring that wills it.
     */
    void beam(Vec3 ring, Vec3 end, double solid, double scale) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double length = ring.distanceTo(end);
        if (strength <= 0.0 || length < 1.0E-3) {
            return;
        }
        // How wide it is where it meets the construct: a cord that suits what hangs on it, never a bar.
        double wide = Mth.clamp(scale, 0.4, 2.0);
        Vec3 axis = end.subtract(ring).scale(1.0 / length);
        Vec3[] across = Basis.of(axis);
        // Cut into short pieces, closer together near the ring. One long piece is turned to face the camera from
        // its middle only, so a beam that starts at your own hand and runs far out would end up edge-on near you
        // and seem to stop in mid-air.
        Vec3 last = ring;
        Vec3 lastA = ring;
        Vec3 lastB = ring;
        for (int i = 1; i <= BEAM_STEPS; i++) {
            double t = Math.pow((double) i / BEAM_STEPS, 1.5);
            Vec3 next = ring.lerp(end, t);
            Vec3 middle = last.add(next).scale(0.5);
            double away = this.camera.distanceTo(middle);
            // Thin where it leaves the stone, full where it meets the construct, and never fat on screen: a piece
            // right in front of your eye is drawn as thin as it looks from far off.
            double near = Mth.clamp(away / BEAM_FULL, 0.18, 1.0);
            double thick = wide * (0.25 + 0.75 * t) * near;
            // Light runs down the beam into the construct.
            double run = Mth.frac(this.time * 0.05 - t * 0.9);
            double pulse = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 6.0);
            // Two fine strands winding around it.
            double turn = length * t * 3.0 - this.time * 0.35;
            double radius = BEAM_HALO * thick * 0.55;
            Vec3 a = next.add(across[0].scale(Math.cos(turn) * radius)).add(across[1].scale(Math.sin(turn) * radius));
            Vec3 b = next.subtract(across[0].scale(Math.cos(turn) * radius))
                    .subtract(across[1].scale(Math.sin(turn) * radius));
            // A piece almost on top of the camera cannot be turned towards it at all: skip it.
            if (away >= BEAM_NEAR) {
                this.line(this.light, last, next, BEAM_CORE * thick * (1.0 + 0.8 * pulse), HOT,
                        alpha(strength));
                this.line(this.light, last, next, BEAM_CORE * 2.2 * thick, BRIGHT, alpha(0.55 * strength));
                this.line(this.glow, last, next, BEAM_HALO * thick * (1.0 + 0.5 * pulse), GREEN,
                        alpha((0.4 + 0.3 * pulse) * strength));
                if (i > 1) {
                    this.line(this.light, lastA, a, BEAM_CORE * 0.6 * thick, BRIGHT, alpha(0.6 * strength));
                    this.line(this.light, lastB, b, BEAM_CORE * 0.6 * thick, BRIGHT, alpha(0.6 * strength));
                }
            }
            last = next;
            lastA = a;
            lastB = b;
        }
        this.flare(ring, 0.03 * Mth.clamp(this.camera.distanceTo(ring) / 0.5, 0.6, 3.0), strength);
    }

    /**
     * The beam that pours out of the ring while the attack button is held. It shoots out of the ring in a blink, with
     * a flash and a ring of light bursting out of the fist, and then roars on: a white-hot core in a cord of bright
     * light and a thick, breathing glow of green, surges of light racing along it away from the ring, three strands
     * winding round it, rings of light running down it and sparks of it crackling off its sides. At the ring a lens of
     * light turns; where it strikes it splashes: a hot flare, ripples running out and sparks spraying back.
     *
     * @param solid how far it is there, 0 to 1 (it dies down when he lets go)
     * @param age   ticks since it broke loose
     * @param thick how thick it is: 1 for the beam of the attack button, more for the air strike's pillar of light
     */
    void beamOfLight(Vec3 from, Vec3 target, double solid, double age, double thick) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double full = from.distanceTo(target);
        if (strength <= 0.0 || full < 0.05) {
            return;
        }
        Vec3 axis = target.subtract(from).scale(1.0 / full);
        Vec3[] across = Basis.of(axis);
        // It shoots out of the ring in a blink.
        double out = Mth.clamp(age / BEAM_SHOOT, 0.0, 1.0);
        Vec3 to = from.lerp(target, 1.0 - (1.0 - out) * (1.0 - out));
        double length = from.distanceTo(to);
        double flicker = 0.9 + 0.1 * Math.sin(this.time * 2.7) * Math.sin(this.time * 1.3 + 1.0);
        int steps = Mth.clamp((int) (length / 0.5), 10, 90);
        Vec3 last = from;
        Vec3[] strands = { from, from, from };
        for (int i = 1; i <= steps && length > 0.02; i++) {
            double t = Math.pow((double) i / steps, 1.3);
            Vec3 next = from.lerp(to, t);
            double away = this.camera.distanceTo(last.add(next).scale(0.5));
            // Thin where it is right in front of your eye, so your own beam never fills your screen.
            double near = Mth.clamp(away / 1.2, 0.22, 1.0) * thick;
            double along = t * length;
            // Surges of light racing along it, and its glow breathing in and out along its length.
            double run = Mth.frac(along * 0.2 - this.time * 0.45);
            double surge = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 5.0);
            double breath = 1.0 + 0.25 * Math.sin(along * 1.3 - this.time * 0.9);
            if (away >= BEAM_NEAR) {
                this.line(this.light, last, next, 0.09 * near * flicker * (1.0 + 0.5 * surge), HOT, alpha(strength));
                this.line(this.light, last, next, 0.24 * near * flicker, BRIGHT, alpha((0.6 + 0.3 * surge) * strength));
                this.line(this.glow, last, next, 0.8 * near * breath, GREEN, alpha((0.45 + 0.25 * surge) * strength));
                if (away > 2.5) {
                    this.line(this.glow, last, next, 1.7 * near * breath, GREEN, alpha(0.14 * strength));
                }
            }
            // Three strands winding round it.
            for (int k = 0; k < 3; k++) {
                double turn = along * 2.4 + this.time * 0.8 + k * Math.PI * 2.0 / 3.0;
                double radius = 0.15 * near * (1.0 + 0.2 * surge);
                Vec3 strand = next.add(across[0].scale(Math.cos(turn) * radius))
                        .add(across[1].scale(Math.sin(turn) * radius));
                if (i > 1 && away >= BEAM_NEAR) {
                    this.line(this.light, strands[k], strand, 0.035 * near, BRIGHT, alpha(0.6 * strength));
                }
                strands[k] = strand;
            }
            last = next;
        }
        // Rings of light running down it, away from the ring; none right in front of your eye.
        double spacing = 2.4 * thick;
        for (double d = this.time * 0.7 % spacing; d < length; d += spacing) {
            Vec3 at = from.add(axis.scale(d));
            double away = this.camera.distanceTo(at);
            if (away < 1.4) {
                continue;
            }
            double near = Mth.clamp(away / 1.2, 0.22, 1.0) * thick;
            double swell = 0.5 + 0.5 * Math.sin(d * 0.9 - this.time * 0.3);
            this.circle(at, across[0], across[1], (0.26 + 0.08 * swell) * near, 0.03 * near, 0.16 * near,
                    alpha(0.75 * strength), alpha(0.35 * strength));
        }
        // Sparks of it crackling off its sides, a new few every other tick.
        int flick = (int) (this.time / 2.0);
        for (int k = 0; k < 5; k++) {
            Vec3 base = from.add(axis.scale(noise(flick, k, 21) * length));
            if (this.camera.distanceTo(base) < 1.4) {
                continue;
            }
            double angle = noise(flick, k, 22) * Math.PI * 2.0;
            Vec3 side = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            Vec3 middle = base.add(side.scale(0.25 * thick)).add(axis.scale(0.15 * thick));
            Vec3 tip = base.add(side.scale((0.45 + 0.3 * noise(flick, k, 23)) * thick))
                    .subtract(axis.scale(0.1 * thick));
            this.edge(base, middle, 0.03 * thick, 0.9 * strength);
            this.edge(middle, tip, 0.025 * thick, 0.7 * strength);
        }
        // At the ring a lens of light turns.
        double muzzle = Mth.clamp(this.camera.distanceTo(from) / 0.5, 0.6, 3.0);
        this.flare(from, 0.06 * muzzle * thick, strength);
        double lens = this.time * 0.25;
        Vec3 lensA = across[0].scale(Math.cos(lens)).add(across[1].scale(Math.sin(lens)));
        Vec3 lensB = axis.cross(lensA);
        this.circle(from.add(axis.scale(0.04 * muzzle)), lensA, lensB, 0.05 * muzzle * thick, 0.008 * muzzle,
                0.04 * muzzle, alpha(0.8 * strength), alpha(0.4 * strength));
        // It broke loose just now: a flash and a ring of light bursting out of the fist, never so big that it fills
        // your own screen.
        if (age < BEAM_BURST) {
            double u = Math.max(0.0, age) / BEAM_BURST;
            double fade = (1.0 - u) * strength;
            double burst = Mth.clamp(this.camera.distanceTo(from) / 1.5, 0.25, 1.2) * thick;
            this.flare(from, (0.12 + 0.2 * (1.0 - u)) * muzzle * thick, fade);
            double wide = (0.1 + 0.6 * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u))) * burst;
            this.circle(from.add(axis.scale(0.05 * muzzle)), across[0], across[1], wide, 0.02 * burst,
                    0.12 * burst, alpha(fade), alpha(0.5 * fade));
        }
        if (out < 1.0) {
            // The head of the beam on its way out.
            this.flare(to, 0.25 * thick, strength);
            return;
        }
        // Where it strikes: a hot flare, ripples running out and sparks spraying back.
        double splash = 0.42 * thick * (0.85 + 0.15 * Math.sin(this.time * 1.9));
        this.flare(to, splash, strength);
        Vec3 face = to.subtract(axis.scale(0.05));
        for (int k = 0; k < 3; k++) {
            double ripple = Mth.frac(this.time / 9.0 + k / 3.0);
            this.circle(face, across[0], across[1], (0.15 + 1.0 * ripple) * thick, 0.03 * thick, 0.16 * thick,
                    alpha(0.8 * (1.0 - ripple) * strength), alpha(0.4 * (1.0 - ripple) * strength));
        }
        for (int k = 0; k < 8; k++) {
            double phase = this.time / 6.0 + noise(k, 31, 0);
            int round = (int) Math.floor(phase);
            double cycle = phase - round;
            // Back towards the ring, spread out, and dropping as they go.
            Vec3 way = direction(k, 31 + round);
            way = way.subtract(axis.scale(1.4 + way.dot(axis))).normalize();
            Vec3 head = face.add(way.scale((0.2 + 1.3 * cycle) * thick)).add(0.0, -0.3 * cycle * cycle * thick, 0.0);
            this.edge(head.subtract(way.scale(0.3 * thick)), head, 0.03 * thick, 1.0 - cycle);
        }
    }

    /**
     * The dome: a bubble of hard light around its owner, with a web of seams over it and a ripple running round it.
     * Seen from outside it is as solid as any construct. Seen from under it, it is faint where you look straight
     * through it and bright where it curves away (its outline), so it still lets you see out. Hits make it flare.
     *
     * @param inside true when you are the one under it, looking out (anyone whose eyes are inside it counts too)
     */
    void dome(Vec3 center, double size, double solid, double flash, boolean inside) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        double radius = size * 0.5 * (0.45 + 0.55 * strength);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        boolean under = inside || this.camera.distanceTo(center) < radius;
        double base = under ? 0.04 : 0.1;
        double edge = under ? 0.16 : 0.42;
        Vec3[][] points = new Vec3[DOME_RINGS + 1][DOME_SLICES + 1];
        for (int i = 0; i <= DOME_RINGS; i++) {
            double polar = Math.PI * i / DOME_RINGS;
            for (int j = 0; j <= DOME_SLICES; j++) {
                double around = Math.PI * 2 * j / DOME_SLICES;
                points[i][j] = center.add(Math.sin(polar) * Math.cos(around) * radius, Math.cos(polar) * radius,
                        Math.sin(polar) * Math.sin(around) * radius);
            }
        }
        double ripple = Mth.frac(this.time * 0.03);
        for (int i = 0; i < DOME_RINGS; i++) {
            for (int j = 0; j < DOME_SLICES; j++) {
                Vec3 p = points[i][j].add(points[i + 1][j + 1]).scale(0.5);
                Vec3 normal = p.subtract(center).normalize();
                Vec3 view = this.camera.subtract(p);
                double facing = view.lengthSqr() < 1.0E-8 ? 1.0 : Math.abs(normal.dot(view.normalize()));
                double rim = (1.0 - facing) * (1.0 - facing);
                double band = Math.max(0.0, 1.0 - Math.abs((double) i / DOME_RINGS - ripple) * 8.0);
                if (!under) {
                    // A solid shell, lit like a block: brightest on top.
                    double lit = (0.62 + 0.38 * (normal.y * 0.5 + 0.5)) * (0.9 + 0.2 * band) * (1.0 + 0.3 * hit);
                    this.quad(this.mass, points[i][j], points[i + 1][j], points[i + 1][j + 1], points[i][j + 1],
                            shade(MASS_GREEN, Math.min(1.0, lit)), 255);
                    continue;
                }
                double a = (base + edge * rim + 0.12 * band + 0.25 * hit) * strength;
                this.quad(this.light, points[i][j], points[i + 1][j], points[i + 1][j + 1], points[i][j + 1],
                        shade(MASS_GREEN, 0.85 + 0.25 * band), alpha(a));
            }
        }
        // The seams, like the joints of a wall of stones: a ring round it every other row, and in every band between
        // two of them an upright joint every fourth slice, shifted half a stone from one band to the next.
        int seam = alpha((0.5 + 0.4 * hit) * strength);
        int seamGlow = alpha((0.25 + 0.3 * hit) * strength);
        for (int i = 2; i < DOME_RINGS; i += 2) {
            for (int j = 0; j < DOME_SLICES; j++) {
                this.line(this.light, points[i][j], points[i][j + 1], 0.025, BRIGHT, seam);
                this.line(this.glow, points[i][j], points[i][j + 1], 0.12, GREEN, seamGlow);
            }
        }
        for (int band = 0; band < DOME_RINGS; band += 2) {
            for (int j = (band / 2 % 2) * 2; j < DOME_SLICES; j += 4) {
                for (int i = band; i < band + 2; i++) {
                    this.line(this.light, points[i][j], points[i + 1][j], 0.025, BRIGHT, seam);
                    this.line(this.glow, points[i][j], points[i + 1][j], 0.12, GREEN, seamGlow);
                }
            }
        }
        // A bright band round its middle, where it meets the ground, and a crown of light on its top.
        int band = alpha((0.8 + 0.2 * hit) * strength);
        int bandGlow = alpha((0.4 + 0.3 * hit) * strength);
        int middle = DOME_RINGS / 2;
        for (int j = 0; j < DOME_SLICES; j++) {
            this.line(this.light, points[middle][j], points[middle][j + 1], 0.06, BRIGHT, band);
            this.line(this.glow, points[middle][j], points[middle][j + 1], 0.25, GREEN, bandGlow);
            this.line(this.light, points[1][j], points[1][j + 1], 0.04, BRIGHT, band);
        }
        this.flare(points[0][0], 0.35 * (1.0 + hit), 0.8 * strength);
    }

    /**
     * The shield while its owner flies: a pointed, streamlined cone of solid hard light out in front of him, its
     * tip where he is going, with bright ridges running to the tip and a glowing rim round its open end. Hits and
     * rams make it flare.
     *
     * @param own true when it is your own and you look out through it: only then is it left see-through, like the
     *            shield
     */
    void ram(Vec3 center, Vec3 way, double solid, double flash, boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        Vec3 forward = way.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : way.normalize();
        Vec3[] across = Basis.of(forward);
        double grow = 0.4 + 0.6 * strength;
        double hit = Mth.clamp(flash, 0.0, 1.0);
        Vec3[][] rings = new Vec3[RAM.length][RAM_SIDES + 1];
        for (int k = 0; k < RAM.length; k++) {
            Vec3 middle = center.add(forward.scale(RAM[k][0] * grow));
            double radius = RAM[k][1] * grow;
            for (int s = 0; s <= RAM_SIDES; s++) {
                double angle = Math.PI * 2 * s / RAM_SIDES + this.time * 0.08;
                rings[k][s] = middle.add(across[0].scale(Math.cos(angle) * radius))
                        .add(across[1].scale(Math.sin(angle) * radius));
            }
        }
        double burn = 1.0 + 0.5 * hit;
        this.nearFade = true;
        for (int k = 0; k + 1 < RAM.length; k++) {
            for (int s = 0; s < RAM_SIDES; s++) {
                Vec3 p0 = rings[k][s];
                Vec3 p1 = rings[k + 1][s];
                Vec3 p2 = rings[k + 1][s + 1];
                Vec3 p3 = rings[k][s + 1];
                int rgb = shade(MASS_GREEN, Math.min(1.0, lit(p0, p1, p2, center.add(forward.scale(RAM[k][0])))
                        * burn * (0.85 + 0.15 * k / RAM.length)));
                if (own) {
                    this.quad(this.light, p0, p1, p2, p3, rgb, alpha(0.16 * strength));
                } else {
                    this.quad(this.mass, p0, p1, p2, p3, rgb, 255);
                }
            }
        }
        // Looking out through your own cone, its lines run past your eyes: they are kept quieter then.
        double quiet = own ? 0.3 : 1.0;
        // Ridges that wind round it towards the tip, like the thread of a drill; the cone turns as it flies.
        for (int s = 0; s < RAM_SIDES; s += 4) {
            for (int k = 0; k + 1 < RAM.length; k++) {
                Vec3 from = rings[k][(s + 2 * k) % RAM_SIDES];
                Vec3 to = rings[k + 1][(s + 2 * k + 2) % RAM_SIDES];
                this.line(this.light, from, to, 0.04, BRIGHT, alpha(EDGE * burn * strength * quiet));
                this.line(this.glow, from, to, 0.16, GREEN, alpha(HALO * burn * strength * quiet));
            }
        }
        // A second bright ring a little way up it; from your own eyes it would frame your whole view, so not there.
        if (!own) {
            for (int s = 0; s < RAM_SIDES; s++) {
                this.line(this.light, rings[1][s], rings[1][s + 1], 0.035, BRIGHT, alpha(0.7 * burn * strength));
            }
        }
        for (int s = 0; s < RAM_SIDES; s++) {
            this.line(this.light, rings[0][s], rings[0][s + 1], 0.05, BRIGHT, alpha(EDGE * burn * strength * quiet));
            this.line(this.glow, rings[0][s], rings[0][s + 1], 0.22, GREEN, alpha(HALO * burn * strength * quiet));
        }
        this.nearFade = false;
        this.flare(rings[RAM.length - 1][0], 0.2 * (1.0 + hit), strength);
    }

    /**
     * The streak of hard light a fast flyer leaves behind: a glowing ribbon along the way he came, fading out
     * behind him.
     *
     * @param points where the middle of his body was on the last ticks, newest first
     * @param own    true when it is your own and you look from your own eyes: the part at the camera is left out
     */
    void trail(Vec3 head, Collection<Vec3> points, double strength, boolean own) {
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
                this.line(this.glow, last, point, 0.32 * (1.0 - t0) + 0.05, GREEN, alpha(0.5 * fade));
                this.line(this.light, last, point, 0.07 * (1.0 - t0) + 0.02, BRIGHT, alpha(0.75 * fade));
            }
            if (gone >= reach) {
                return;
            }
            last = point;
        }
    }

    /**
     * The flame out of the back of a jet engine, a turbo booster or a rocket: a white-hot core inside a cone of bright
     * green light that flickers, a glow round all of it, bright rings standing in the flame (the shock diamonds of an
     * afterburner) and a flare at the nozzle. Light, not a construct.
     *
     * @param from   the middle of the nozzle
     * @param way    the way the flame blows out, one long
     * @param length how long the flame is at full thrust, in blocks
     * @param radius how wide the nozzle is, in blocks
     * @param thrust 0 = out, 1 = full: the flame grows and brightens with it
     */
    void exhaust(Vec3 from, Vec3 way, double length, double radius, double thrust) {
        double power = Mth.clamp(thrust, 0.0, 1.0);
        if (power <= 0.01 || way.lengthSqr() < 1.0E-8 || !this.visible(from, length + radius * 4.0)) {
            return;
        }
        Vec3 along = way.normalize();
        double flicker = 0.85 + 0.15 * Math.sin(this.time * 3.1) * Math.sin(this.time * 1.7 + 0.6);
        double reach = length * power * flicker;
        this.line(this.glow, from, from.add(along.scale(reach)), radius * 4.0 * (0.6 + 0.4 * power), GREEN,
                alpha(0.55 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.8)), radius * 1.9, BRIGHT, alpha(0.75 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.45)), radius * 0.9, HOT, alpha(0.95 * power));
        Vec3[] across = Basis.of(along);
        int diamonds = 4;
        for (int k = 1; k <= diamonds; k++) {
            double t = k / (diamonds + 1.0);
            Vec3 at = from.add(along.scale(reach * t * 0.9));
            double ring = radius * (1.0 - 0.6 * t) * (0.9 + 0.1 * Math.sin(this.time * 2.3 + k));
            this.circle(at, across[0], across[1], ring, radius * 0.12, radius * 0.6,
                    alpha(0.8 * power * (1.0 - t)), alpha(0.4 * power * (1.0 - t)));
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
    void chain(Vec3 from, Vec3 to, double sag, double link, double solid, double bright, double grown,
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
            Vec3 side = along.cross(UP);
            Vec3 up = i % 2 == 0 || side.lengthSqr() < 1.0E-8 ? UP : side;
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
     * An easing that shoots a little past the end and settles back: 0 at 0, a hair over 1 on the way, 1 from 1 on. The
     * way a construct pops into shape.
     */
    static double backOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0) - 1.0;
        return 1.0 + c * c * (2.70158 * c + 1.70158);
    }

    /**
     * A spark of light facing you: a soft glow, a bright heart and four short rays turning slowly.
     *
     * @param size how far its glow reaches, in blocks
     */
    void flare(Vec3 at, double size, double strength) {
        if (strength <= 0.0) {
            return;
        }
        Vec3 view = this.camera.subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Basis.of(view.normalize());
        int sides = 14;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2 * i / sides;
            double a1 = Math.PI * 2 * (i + 1) / sides;
            Vec3 r0 = across[0].scale(Math.cos(a0)).add(across[1].scale(Math.sin(a0)));
            Vec3 r1 = across[0].scale(Math.cos(a1)).add(across[1].scale(Math.sin(a1)));
            this.fan(this.glow, at, r0, r1, size, GREEN, alpha(0.7 * strength));
            this.fan(this.light, at, r0, r1, size * 0.35, HOT, alpha(0.9 * strength));
        }
        for (int k = 0; k < 4; k++) {
            double angle = this.time * 0.05 + Math.PI * 0.5 * k;
            Vec3 ray = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle))).scale(size * 1.6);
            this.line(this.light, at.subtract(ray), at.add(ray), size * 0.12, BRIGHT, alpha(0.8 * strength));
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

    /** Two directions square to {@code axis} and to each other, to lay things out around it. */
    private static final class Basis {
        private Basis() {
        }

        static Vec3[] of(Vec3 axis) {
            Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(UP) : axis.cross(new Vec3(1, 0, 0));
            side = side.normalize();
            return new Vec3[] { side, side.cross(axis).normalize() };
        }
    }

    /** A ring of light lying flat around {@code center}, like the line that puts on the uniform. */
    void band(Vec3 center, double radius, double strength) {
        this.circle(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, 0.05, 0.3, alpha(0.95 * strength),
                alpha(0.45 * strength));
    }

    /** A ring of light around {@code center}, in the flat plane through the unit vectors {@code a} and {@code b}. */
    void circle(Vec3 center, Vec3 a, Vec3 b, double radius, double width, double glowWidth, int edge,
            int halo) {
        int segments = 24;
        Vec3 last = center.add(a.scale(radius));
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            Vec3 next = center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
            this.line(this.light, last, next, width, BRIGHT, edge);
            this.line(this.glow, last, next, glowWidth, GREEN, halo);
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
    private void box(double[][] model, ModelInfo info, int index, Vec3[] corners, Vec3 view, double width,
            double solid, double bright, double depth, double charge, boolean glowing) {
        double ripple = 0.9 + (0.06 + 0.06 * charge) * Math.sin(this.time * 0.5 - depth * 4.0);
        int body = alpha(solid);
        Vec3 middle = corners[0].add(corners[7]).scale(0.5);
        for (int[] side : SIDES) {
            Vec3 p0 = corners[side[0]];
            Vec3 p1 = corners[side[1]];
            Vec3 p2 = corners[side[2]];
            Vec3 toEye = this.camera.subtract(p0.add(p2).scale(0.5));
            Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
            double across = normal.length() * toEye.length();
            double face = across < 1.0E-12 ? 1.0 : Math.abs(normal.dot(toEye)) / across;
            this.quad(this.mass, p0, p1, p2, corners[side[3]],
                    this.mass(lit(p0, p1, p2, middle) * sheen(face) * ripple * bright), body);
        }
        int edge = alpha(EDGE * ripple * solid);
        int halo = glowing ? alpha(HALO * (1.0 + 0.4 * charge) * solid) : 0;
        double[] box = model[index];
        double fine = width * fine(box[3] - box[0], box[4] - box[1], box[5] - box[2]);
        for (int i = 0; i < 8; i++) {
            for (int axis = 0; axis < 3; axis++) {
                int bit = 1 << axis;
                if ((i & bit) != 0 || !rim(model, info, index, i, axis, view)) {
                    continue;
                }
                Vec3 a = this.lifted(corners[i], fine);
                Vec3 b = this.lifted(corners[i | bit], fine);
                this.line(this.light, a, b, EDGE_WIDTH * fine, BRIGHT, edge);
                this.line(this.glow, a, b, HALO_WIDTH * fine, GREEN, halo);
            }
        }
    }

    /**
     * How thick the lines along a part are drawn, as a part of the usual: a small part (a knob, a coin, a spoke) gets
     * finer lines, so its glow does not swallow it. It goes by the middle one of its three sizes, at scale 1.
     */
    static double fine(double x, double y, double z) {
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
    static double lit(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 middle) {
        Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
        if (normal.lengthSqr() < 1.0E-12) {
            return 0.8;
        }
        normal = normal.normalize();
        if (normal.dot(p0.subtract(middle)) < 0.0) {
            normal = normal.scale(-1.0);
        }
        return light(normal);
    }

    /**
     * How brightly a side facing {@code normal} (one long, in the world) is lit, as a part of its colour: the side
     * facing up catches the most light and the one facing down the least, the way a block's sides do, and the sides
     * facing the sun, high over one corner of the world, more than those facing away from it. So every side of a
     * shape comes out a shade of its own and its corners and folds read at a glance, as a solid thing does.
     */
    static double light(Vec3 normal) {
        return light(normal.x, normal.y, normal.z);
    }

    /** {@link #light(Vec3)}, for a way given as three numbers. */
    static double light(double x, double y, double z) {
        return SKY_FLOOR + SKY * (y * 0.5 + 0.5) + SUN_LIGHT * Math.max(0.0, x * SUN.x + y * SUN.y + z * SUN.z);
    }

    /**
     * The colour of the solid mass at this much light: the green of hard light, darker in the shade, never brighter
     * than the green itself (past that it would wash out to white), except while it glares as it strikes.
     */
    private int mass(double light) {
        int rgb = shade(MASS_GREEN, Math.min(1.0, light));
        return this.glare > 0.0 ? Ring.mix(rgb, HOT, (float) Math.min(0.75, this.glare * Math.min(1.0, light))) : rgb;
    }

    /** How far the solid shapes drawn from now on flare up towards white, 0 to 1 (0 once they are done). */
    void glare(double amount) {
        this.glare = Mth.clamp(amount, 0.0, 1.0);
    }

    /**
     * How far the pieces of what breaks up from now on fly, next to how far they usually do (1 once it is done): a thing
     * as big as a plane is flung far further apart than a fist.
     */
    void fling(double amount) {
        this.fling = Math.max(0.0, amount);
    }

    /** The same colour, darker or lighter. */
    static int shade(int rgb, double amount) {
        int red = (int) Mth.clamp((rgb >> 16 & 0xFF) * amount, 0.0, 255.0);
        int green = (int) Mth.clamp((rgb >> 8 & 0xFF) * amount, 0.0, 255.0);
        int blue = (int) Mth.clamp((rgb & 0xFF) * amount, 0.0, 255.0);
        return red << 16 | green << 8 | blue;
    }

    /** A point moved a hair towards the camera, so a bright edge is not swallowed by the side it lies on. */
    private Vec3 lifted(Vec3 point, double width) {
        Vec3 way = this.camera.subtract(point);
        double length = way.length();
        return length < 1.0E-6 ? point : point.add(way.scale((0.01 + 0.02 * width) / length));
    }

    /** {@code v} turned by {@code angle} (radians) around the unit vector {@code axis}. */
    static Vec3 spin(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
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
        return (int) (alpha * smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }

    static int alpha(double value) {
        return (int) (255 * Mth.clamp(value, 0.0, 1.0));
    }
}
