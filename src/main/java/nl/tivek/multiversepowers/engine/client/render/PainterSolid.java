package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;

/**
 * The engine's painter (see {@link ConstructPainter}) drawing solid shapes, one part at a time: a box of a box model
 * whose corners are worked out, or a round part (see {@link Mesh}) whose corners and sides are, as solid sides lit
 * like a block with bright lines only where the shape ends as you look at it, cut off at a plane while asked to (see
 * {@link #clip}). What a box model needs worked out only once is kept here too.
 */
abstract class PainterSolid extends PainterCut {
    // How far outside a box an edge is looked at to see whether another box sits against it there, in blocks
    // at scale 1.
    private static final double EDGE_OUT = 0.02;
    // A part smaller than this (in blocks at scale 1, see fine) gets finer lines, down to FINEST times the usual.
    private static final double FINE_SIZE = 0.35;
    private static final double FINEST = 0.35;
    // How strongly the outline of a see-through shape shows, next to that of a solid one.
    private static final double SEE_THROUGH_EDGE = 0.55;
    // The four corners of each side of a box. A corner is three bits: 1 = far x, 2 = far y, 4 = far z.
    private static final int[][] SIDES = { { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 },
            { 0, 1, 3, 2 }, { 4, 6, 7, 5 } };

    /**
     * What a box model needs worked out only once: for every edge of every box whether another box lies against it
     * (see {@link #rim}), and a ball round the whole model, in its own blocks at scale 1, to skip it when it is out of
     * view. Kept per model for as long as the model itself is kept.
     */
    record ModelInfo(boolean[] covered, double x, double y, double z, double radius) {
    }

    private static final Map<double[][], ModelInfo> MODELS = new WeakHashMap<>();
    // A model with fewer boxes than this is worked out on the spot: those are often made new every frame.
    private static final int CACHED_FROM = 4;

    // The eight corners of the box drawn right now, as x, y, z of corner 0, then of corner 1 and so on (see
    // corners), and the same corners moved a hair towards the camera for its edges.
    final double[] corner = new double[24];
    private final double[] liftedCorner = new double[24];
    // Room to work a round part out in, used again for every part: its corners out in the world, the way its sides
    // face, and which of them face the camera.
    double[] wx = new double[256];
    double[] wy = new double[256];
    double[] wz = new double[256];
    double[] nx = new double[256];
    double[] ny = new double[256];
    double[] nz = new double[256];
    private boolean[] facingCamera = new boolean[256];
    // Above 0 while a see-through shape is drawn (see seeThrough): how strongly its sides show.
    double faint;

    /** See the constructors of {@link ConstructPainter}. */
    PainterSolid(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

    /** What is worked out once for a box model (see {@link ModelInfo}): kept for big ones, made anew for the rest. */
    static ModelInfo info(double[][] model) {
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

    /** Makes the room to work out a part of this many corners and sides in. */
    void room(int points, int sides) {
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

    /**
     * Draws a round part whose corners and sides have just been worked out into the room for it (see {@link #room}):
     * solid sides lit like the sides of a box, and a bright line along every edge where its outline runs as you look at
     * it (one side along it faces you and the other faces away), or where it has only one side.
     *
     * @param halo false to leave out the soft glow along its edges: far away it would only blur it
     */
    void drawMesh(Mesh mesh, double width, double solid, double bright, boolean halo) {
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
    void box(double[][] model, ModelInfo info, int index, Vec3 view, double width, double solid,
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
}
