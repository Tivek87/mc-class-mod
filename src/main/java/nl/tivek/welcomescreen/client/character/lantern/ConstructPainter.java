package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws Green Lantern's hard-light constructs: solid green shapes with bright edges and a glow around them,
 * and the beam of light from the ring that feeds them. A fist is built out of boxes (palm, fingers,
 * knuckles, thumb, wrist, forearm), the way the ring would shape it.
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
            // Wrist, a bright cuff, and the forearm fading out towards the ring
            { -0.38, -0.27, -0.72, 0.38, 0.25, -0.42, 0.9 },
            { -0.44, -0.33, -0.86, 0.44, 0.31, -0.72, 1.2 },
            { -0.34, -0.25, -1.25, 0.34, 0.23, -0.86, 0.6 } };
    // Where the forearm ends: the beam from the ring comes in there.
    private static final double BACK = -1.25;
    /**
     * The bolt the ring shoots: a small bullet of light, in the same three numbers per corner. A pointed
     * nose, a body, and a tail that burns lower behind it.
     */
    private static final double[][] BOLT = {
            { -0.20, -0.20, -0.75, 0.20, 0.20, -0.40, 0.7 },
            { -0.32, -0.32, -0.40, 0.32, 0.32, 0.25, 1.0 },
            { -0.20, -0.20, 0.25, 0.20, 0.20, 0.55, 1.3 } };
    // How wide that bullet is at its widest, so a bolt of size 1 is one block wide.
    private static final double BOLT_WIDTH = 0.64;
    // The dome: how many rings and slices its sphere is cut into.
    private static final int DOME_RINGS = 12;
    private static final int DOME_SLICES = 24;
    // The ram cone: how many sides it has, and its shape as rings along its length (how far ahead of his middle,
    // and how wide there, in blocks).
    private static final int RAM_SIDES = 16;
    private static final double[][] RAM = { { -0.45, 0.92 }, { 0.35, 0.8 }, { 1.05, 0.52 }, { 1.55, 0.2 },
            { 1.8, 0.0 } };
    // How many corners the round pane of the shield is cut into.
    private static final int SHIELD_SIDES = 16;
    // The four corners of each side of a box. A corner is three bits: 1 = far x, 2 = far y, 4 = far z.
    private static final int[][] SIDES = { { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 },
            { 0, 1, 3, 2 }, { 4, 6, 7, 5 } };

    /** One corner of a quad, kept until {@link #finish} draws them all. */
    private record Corner(Vec3 at, int rgb, int alpha) {
    }

    /** Where a construct is and how it is turned: its middle, its own right, up and forward, and its scale. */
    record Frame(Vec3 center, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        /** A point of the model (in blocks at scale 1) out in the world. */
        Vec3 at(double x, double y, double z) {
            return this.center.add(this.right.scale(x * this.scale)).add(this.up.scale(y * this.scale))
                    .add(this.forward.scale(z * this.scale));
        }

        /** The other way around: where a point of the world lies in the model. */
        Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.center);
            return new Vec3(way.dot(this.right) / this.scale, way.dot(this.up) / this.scale,
                    way.dot(this.forward) / this.scale);
        }
    }

    private final Matrix4f matrix;
    private final Vec3 camera;
    private final float time;
    // What this frame draws: the solid sides, the bright lines, and the glow around them.
    private final List<Corner> mass = new ArrayList<>();
    private final List<Corner> light = new ArrayList<>();
    private final List<Corner> glow = new ArrayList<>();
    // True while a construct's own shape is drawn: then what comes close to the camera fades out.
    private boolean nearFade;

    ConstructPainter(PoseStack pose, Vec3 camera, float time) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
    }

    /** Draws everything of this frame: the solid shapes first, then their lines and the glow on top. */
    void finish(MultiBufferSource.BufferSource buffers) {
        this.draw(buffers, MASS, this.mass);
        this.draw(buffers, LIGHT, this.light);
        this.draw(buffers, GLOW, this.glow);
    }

    private void draw(MultiBufferSource.BufferSource buffers, RenderType type, List<Corner> corners) {
        if (corners.isEmpty()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(type);
        for (Corner corner : corners) {
            Vec3 p = corner.at();
            int rgb = corner.rgb();
            buffer.addVertex(this.matrix, (float) (p.x - this.camera.x), (float) (p.y - this.camera.y),
                    (float) (p.z - this.camera.z)).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF,
                            corner.alpha());
        }
        corners.clear();
        buffers.endBatch(type);
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
        this.nearFade = true;
        for (int b = 0; b < FIST.length; b++) {
            double[] box = FIST[b];
            for (int i = 0; i < 8; i++) {
                corners[i] = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4],
                        box[(i & 4) == 0 ? 2 : 5]);
            }
            this.box(FIST, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * throb,
                    (box[2] + box[5]) * 0.5, grown);
        }
        this.nearFade = false;
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
        if (strength <= 0.0) {
            return;
        }
        Vec3[] corners = new Vec3[8];
        Vec3 view = frame.local(this.camera);
        this.nearFade = true;
        for (int b = 0; b < model.length; b++) {
            double[] box = model[b];
            for (int i = 0; i < 8; i++) {
                corners[i] = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4],
                        box[(i & 4) == 0 ? 2 : 5]);
            }
            this.box(model, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * bright,
                    (box[2] + box[5]) * 0.5, 0.0);
        }
        this.nearFade = false;
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
        Vec3[] corners = new Vec3[8];
        Vec3 view = frame.local(this.camera);
        this.nearFade = true;
        for (int b = 0; b < BOLT.length; b++) {
            double[] box = BOLT[b];
            for (int i = 0; i < 8; i++) {
                corners[i] = frame.at(box[(i & 1) == 0 ? 0 : 3], box[(i & 2) == 0 ? 1 : 4],
                        box[(i & 4) == 0 ? 2 : 5]);
            }
            this.box(BOLT, b, corners, view, Math.min(frame.scale(), WIDTH_CAP), strength, box[6] * 1.15,
                    (box[2] + box[5]) * 0.5, 0.0);
        }
        Vec3 nose = frame.at(0.0, 0.0, BOLT[2][5] + 0.2);
        Vec3 tail = frame.at(0.0, 0.0, BOLT[0][2]);
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
            this.beam(ring, frame.at(0.0, 0.0, BOLT[0][2]), strength * 0.7, frame.scale());
        }
    }

    /**
     * The shield: a round pane of hard light standing across {@code facing}, thick enough to read as
     * something solid and bright all around its edge. It is left a little see-through on purpose, because it
     * hangs right in front of the eyes of the one holding it.
     *
     * @param size  how wide the pane is, in blocks
     * @param solid 0 = gone, 1 = fully there; it also folds out from the middle as this grows
     * @param flash 1 right after a hit landed on it, 0 otherwise
     * @param ring  where the ring holding it is, or null when its owner is out of sight
     */
    void shield(Vec3 center, Vec3 facing, double size, double solid, double flash, @Nullable Vec3 ring,
            boolean own) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        // Your own shield stays up while you walk and fight, right in front of your eyes: you see it by its
        // rim and ribs, and look straight through the pane.
        double pane = own ? 0.22 : 1.0;
        Vec3 forward = facing.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : facing.normalize();
        Vec3 right = forward.cross(UP);
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(forward);
        double radius = size * 0.5 * (0.55 + 0.45 * strength);
        double thick = Math.max(0.02, radius * 0.09);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        double burn = 1.0 + 0.5 * hit;
        double ripple = 0.92 + 0.08 * Math.sin(this.time * 0.5);
        int face = alpha((0.5 + 0.18 * hit) * strength * pane);
        int faceRgb = shade(MASS_GREEN, Math.min(1.0, 0.8 * burn * ripple));
        int edgeRgb = shade(MASS_GREEN, Math.min(1.0, burn));
        // Your own pane is drawn as light only, so it never hides anything behind it.
        List<Corner> faces = own ? this.light : this.mass;
        Vec3 front = center.add(forward.scale(thick));
        Vec3 back = center.subtract(forward.scale(thick));
        Vec3 lastOut = null;
        for (int i = 0; i <= SHIELD_SIDES; i++) {
            double angle = Math.PI * 2 * i / SHIELD_SIDES;
            Vec3 out = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            if (lastOut != null) {
                // The two flat faces, each as a wedge from the middle, and the strip of edge between them.
                this.quad(faces, front, front.add(lastOut), front.add(out), front, faceRgb, face);
                this.quad(faces, back, back.add(out), back.add(lastOut), back, faceRgb, face);
                this.quad(faces, front.add(lastOut), back.add(lastOut), back.add(out), front.add(out),
                        edgeRgb, alpha((own ? 0.55 : 0.9) * strength));
                Vec3 a = center.add(lastOut);
                Vec3 b = center.add(out);
                this.line(this.light, a, b, radius * 0.07, BRIGHT, alpha(EDGE * burn * strength));
                this.line(this.glow, a, b, radius * 0.24, GREEN, alpha(HALO * burn * strength));
            }
            // Ribs out of the middle, every fourth corner: it reads as something shaped, not as a green plate.
            if (i % 4 == 0 && i < SHIELD_SIDES) {
                this.line(this.light, center, center.add(out), radius * 0.045, BRIGHT,
                        alpha(0.55 * burn * strength));
            }
            lastOut = out;
        }
        if (ring != null) {
            this.beam(ring, center.subtract(forward.scale(thick + 0.02)), strength, radius);
        }
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
    private static double noise(int a, int b, int c) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }

    /** A direction of its own for every speck. */
    private static Vec3 direction(int a, int b) {
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
     * The beam that pours out of the ring while the attack button is held: a white-hot core in a thick glow of
     * green, flickering, with light rushing along it away from the ring and two strands winding around it, a
     * spark at the ring and a splash of light where it strikes.
     */
    void beamOfLight(Vec3 from, Vec3 to, double solid) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        double length = from.distanceTo(to);
        if (strength <= 0.0 || length < 0.05) {
            return;
        }
        Vec3 axis = to.subtract(from).scale(1.0 / length);
        Vec3[] across = Basis.of(axis);
        double flicker = 0.9 + 0.1 * Math.sin(this.time * 2.7) * Math.sin(this.time * 1.3 + 1.0);
        int steps = Mth.clamp((int) (length / 0.6), 10, 72);
        Vec3 last = from;
        Vec3 lastA = from;
        Vec3 lastB = from;
        for (int i = 1; i <= steps; i++) {
            double t = Math.pow((double) i / steps, 1.4);
            Vec3 next = from.lerp(to, t);
            double away = this.camera.distanceTo(last.add(next).scale(0.5));
            double near = Mth.clamp(away / 1.2, 0.22, 1.0);
            double run = Mth.frac(t * length * 0.25 - this.time * 0.3);
            double pulse = Math.max(0.0, 1.0 - Math.abs(run - 0.5) * 5.0);
            double turn = length * t * 2.2 + this.time * 0.6;
            double radius = 0.1 * near;
            Vec3 a = next.add(across[0].scale(Math.cos(turn) * radius)).add(across[1].scale(Math.sin(turn) * radius));
            Vec3 b = next.subtract(across[0].scale(Math.cos(turn) * radius))
                    .subtract(across[1].scale(Math.sin(turn) * radius));
            if (away >= BEAM_NEAR) {
                this.line(this.light, last, next, 0.07 * near * flicker, HOT, alpha(strength));
                this.line(this.light, last, next, 0.17 * near * flicker, BRIGHT, alpha((0.6 + 0.3 * pulse) * strength));
                this.line(this.glow, last, next, 0.6 * near * flicker, GREEN, alpha((0.4 + 0.25 * pulse) * strength));
                if (i > 1) {
                    this.line(this.light, lastA, a, 0.035 * near, BRIGHT, alpha(0.55 * strength));
                    this.line(this.light, lastB, b, 0.035 * near, BRIGHT, alpha(0.55 * strength));
                }
            }
            last = next;
            lastA = a;
            lastB = b;
        }
        this.flare(from, 0.05 * Mth.clamp(this.camera.distanceTo(from) / 0.5, 0.6, 3.0), strength);
        double splash = 0.32 * (0.85 + 0.15 * Math.sin(this.time * 1.9));
        this.flare(to, splash, strength);
        this.circle(to.subtract(axis.scale(0.05)), across[0], across[1], splash * 1.2, 0.03, 0.18,
                alpha(0.8 * strength), alpha(0.4 * strength));
    }

    /**
     * The dome: a bubble of hard light around its owner. Faint where you look straight through it and bright
     * where it curves away (its outline), with a web of seams over it and a ripple running round it, so it reads
     * as a shell and still lets you see out. Hits make it flare.
     *
     * @param inside true when you are the one under it, looking out: then it is kept fainter still
     */
    void dome(Vec3 center, double size, double solid, double flash, boolean inside) {
        double strength = Mth.clamp(solid, 0.0, 1.0);
        if (strength <= 0.0) {
            return;
        }
        double radius = size * 0.5 * (0.45 + 0.55 * strength);
        double hit = Mth.clamp(flash, 0.0, 1.0);
        double base = inside ? 0.04 : 0.1;
        double edge = inside ? 0.16 : 0.42;
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
                double a = (base + edge * rim + 0.12 * band + 0.25 * hit) * strength;
                this.quad(this.light, points[i][j], points[i + 1][j], points[i + 1][j + 1], points[i][j + 1],
                        shade(MASS_GREEN, 0.85 + 0.25 * band), alpha(a));
            }
        }
        // The seams: every other ring and every third slice.
        int seam = alpha((0.5 + 0.4 * hit) * strength);
        int seamGlow = alpha((0.25 + 0.3 * hit) * strength);
        for (int i = 2; i < DOME_RINGS; i += 2) {
            for (int j = 0; j < DOME_SLICES; j++) {
                this.line(this.light, points[i][j], points[i][j + 1], 0.025, BRIGHT, seam);
                this.line(this.glow, points[i][j], points[i][j + 1], 0.12, GREEN, seamGlow);
            }
        }
        for (int j = 0; j < DOME_SLICES; j += 3) {
            for (int i = 0; i < DOME_RINGS; i++) {
                this.line(this.light, points[i][j], points[i + 1][j], 0.025, BRIGHT, seam);
                this.line(this.glow, points[i][j], points[i + 1][j], 0.12, GREEN, seamGlow);
            }
        }
    }

    /**
     * The shield while its owner flies: a pointed, streamlined cone of hard light out in front of him, its tip
     * where he is going, with bright ridges running to the tip and a glowing rim round its open end. Hits and
     * rams make it flare.
     *
     * @param own true when it is your own and you look out through it: then it is left see-through, like the shield
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
                double angle = Math.PI * 2 * s / RAM_SIDES + this.time * 0.02;
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
                    this.quad(this.mass, p0, p1, p2, p3, rgb, alpha(0.62 * strength));
                }
            }
        }
        // Looking out through your own cone, its lines run past your eyes: they are kept quieter then.
        double quiet = own ? 0.45 : 1.0;
        for (int s = 0; s < RAM_SIDES; s += 4) {
            for (int k = 0; k + 1 < RAM.length; k++) {
                this.line(this.light, rings[k][s], rings[k + 1][s], 0.04, BRIGHT,
                        alpha(EDGE * burn * strength * quiet));
                this.line(this.glow, rings[k][s], rings[k + 1][s], 0.16, GREEN,
                        alpha(HALO * burn * strength * quiet));
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
    private void fan(List<Corner> layer, Vec3 at, Vec3 r0, Vec3 r1, double size, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        layer.add(new Corner(at, rgb, alpha));
        layer.add(new Corner(at.add(r0.scale(size)), rgb, 0));
        layer.add(new Corner(at.add(r1.scale(size)), rgb, 0));
        layer.add(new Corner(at, rgb, alpha));
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
    private void box(double[][] model, int index, Vec3[] corners, Vec3 view, double width, double solid,
            double bright, double depth, double charge) {
        double ripple = 0.9 + (0.06 + 0.06 * charge) * Math.sin(this.time * 0.5 - depth * 4.0);
        int body = alpha(solid);
        Vec3 middle = corners[0].add(corners[7]).scale(0.5);
        for (int[] side : SIDES) {
            Vec3 p0 = corners[side[0]];
            Vec3 p1 = corners[side[1]];
            Vec3 p2 = corners[side[2]];
            // Never brighter than the green itself: past that the mass washes out to white.
            this.quad(this.mass, p0, p1, p2, corners[side[3]],
                    shade(MASS_GREEN, Math.min(1.0, lit(p0, p1, p2, middle) * ripple * bright)), body);
        }
        int edge = alpha(EDGE * ripple * solid);
        int halo = alpha(HALO * (1.0 + 0.4 * charge) * solid);
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                if ((i & bit) != 0 || !rim(model, index, i, bit, view)) {
                    continue;
                }
                Vec3 a = this.lifted(corners[i], width);
                Vec3 b = this.lifted(corners[i | bit], width);
                this.line(this.light, a, b, EDGE_WIDTH * width, BRIGHT, edge);
                this.line(this.glow, a, b, HALO_WIDTH * width, GREEN, halo);
            }
        }
    }

    /**
     * Whether an edge of a box is worth a line of light. It is when the shape ends there as you look at it:
     * one of the two sides that meet at the edge faces you and the other faces away. An edge with another
     * box right against it is left out as well, because there the mass simply goes on.
     *
     * @param corner the corner the edge starts at; {@code bit} is the way it runs (1 = x, 2 = y, 4 = z)
     * @param view   where the camera is, in the model's own space
     */
    private static boolean rim(double[][] model, int index, int corner, int bit, Vec3 view) {
        double[] box = model[index];
        double[] outward = { 0.0, 0.0, 0.0 };
        int facing = 0;
        for (int axis = 0; axis < 3; axis++) {
            int mask = 1 << axis;
            if (mask == bit) {
                continue;
            }
            boolean far = (corner & mask) != 0;
            if (far == (axis(view, axis) > (far ? box[3 + axis] : box[axis]))) {
                facing++;
            }
            outward[axis] = far ? EDGE_OUT : -EDGE_OUT;
        }
        if (facing != 1) {
            return false;
        }
        double x = along(box, corner, bit, 0) + outward[0];
        double y = along(box, corner, bit, 1) + outward[1];
        double z = along(box, corner, bit, 2) + outward[2];
        for (int b = 0; b < model.length; b++) {
            double[] other = model[b];
            if (b != index && x > other[0] && x < other[3] && y > other[1] && y < other[4] && z > other[2]
                    && z < other[5]) {
                return false;
            }
        }
        return true;
    }

    /** One coordinate of the middle of an edge: along the edge the middle, across it the corner's own. */
    private static double along(double[] box, int corner, int bit, int axis) {
        int mask = 1 << axis;
        if (mask == bit) {
            return (box[axis] + box[3 + axis]) * 0.5;
        }
        return (corner & mask) != 0 ? box[3 + axis] : box[axis];
    }

    /** One of the three numbers of a point: 0 = x, 1 = y, 2 = z. */
    private static double axis(Vec3 point, int index) {
        return index == 0 ? point.x : index == 1 ? point.y : point.z;
    }

    /**
     * How brightly one side of a box is lit, as a part of its colour: the side facing up catches the most
     * light and the one facing down the least, the way a block's sides do.
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
        return 0.62 + 0.38 * (normal.y * 0.5 + 0.5);
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
    private static Vec3 spin(Vec3 v, Vec3 axis, double angle) {
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
    private void line(List<Corner> layer, Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        Vec3 side = b.subtract(a).cross(this.camera.subtract(a.add(b).scale(0.5)));
        double length = side.length();
        if (length < 1.0E-6) {
            return;
        }
        side = side.scale(width * 0.5 / length);
        int alphaA = this.faded(a, alpha);
        int alphaB = this.faded(b, alpha);
        layer.add(new Corner(a, rgb, alphaA));
        layer.add(new Corner(b, rgb, alphaB));
        layer.add(new Corner(b.add(side), rgb, 0));
        layer.add(new Corner(a.add(side), rgb, 0));
        layer.add(new Corner(a, rgb, alphaA));
        layer.add(new Corner(b, rgb, alphaB));
        layer.add(new Corner(b.subtract(side), rgb, 0));
        layer.add(new Corner(a.subtract(side), rgb, 0));
    }

    private void quad(List<Corner> layer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        layer.add(new Corner(p0, rgb, this.faded(p0, alpha)));
        layer.add(new Corner(p1, rgb, this.faded(p1, alpha)));
        layer.add(new Corner(p2, rgb, this.faded(p2, alpha)));
        layer.add(new Corner(p3, rgb, this.faded(p3, alpha)));
    }

    /**
     * {@code alpha} at {@code point}: while a construct's own shape is drawn, what comes close to the
     * camera fades out, so a big fist hanging right next to you never fills your screen.
     */
    private int faded(Vec3 point, int alpha) {
        if (!this.nearFade) {
            return alpha;
        }
        return (int) (alpha * smooth((point.distanceTo(this.camera) - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }

    static int alpha(double value) {
        return (int) (255 * Mth.clamp(value, 0.0, 1.0));
    }
}
