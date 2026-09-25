package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;

/**
 * The engine's painter (see {@link ConstructPainter}) cutting everything solid off at a plane (see {@link #clip}):
 * a side that runs through the plane keeps only what lies in front of it, a seam of light runs along the cut, and a
 * lid of the part's own mass closes it there, so a thing seems to come out of a surface of light.
 */
abstract class PainterCut extends PainterLight {
    // How much wider the seam of light along a cut (see clip) is drawn than an edge, and its glow than an edge's glow.
    private static final double SEAM_WIDTH = 1.5;
    // How near two ends of the lines a solid part was cut along lie (squared, in blocks) to join into one outline of its
    // lid (see lid).
    private static final double LID_JOIN = 1.0E-8;

    // True while everything solid is cut off at a plane (see clip): a point on it, the way to the side that is kept
    // (one long), and how strongly the seam of light along the cut burns.
    boolean clipping;
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
    final double[] cutIn = new double[12];
    private final double[] cutAhead = new double[4];
    private final double[] cutOut = new double[18];
    private final double[] cutCross = new double[12];
    final double[] cutEnds = new double[6];
    // The lines the sides of the solid part being drawn were cut along, two ends each (see cut), and how many: the
    // part is closed with a lid there once all its sides are drawn (see lid), so it is never seen hollow.
    private double[] lidCuts = new double[6 * 32];
    int lidCount;
    // Which outline each of those lines belongs to (the first line of it), and room to walk round one (see lid).
    private int[] lidOutline = new int[32];
    private int[] lidWaiting = new int[32];

    /** See the constructors of {@link ConstructPainter}. */
    PainterCut(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material, boolean hand) {
        super(pose, camera, time, frustum, material, hand);
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
    double ahead(double x, double y, double z) {
        return (x - this.clipX) * this.clipNormalX + (y - this.clipY) * this.clipNormalY
                + (z - this.clipZ) * this.clipNormalZ;
    }

    /**
     * Draws the solid side whose four corners are in {@link #cutIn}, cut off at the plane (see {@link #clip}): whole
     * when it lies wholly in front of it, not at all when wholly behind it, and otherwise only what is in front (its
     * corners in front and the points where its edges cross the plane, walked round in order, as Sutherland and Hodgman
     * cut a polygon), with the seam of light along the cut. A side of three corners (its last one repeated) is cut the
     * same way.
     *
     * @param fine  how thick the lines along the part are drawn, as a scale (see {@link ConstructPainter#fine})
     * @param solid how strongly its seam shows, 0 to 1
     * @param halo  false to leave out the soft glow along its seam
     */
    void cut(Layer layer, int rgb, int alpha, double fine, double solid, boolean halo) {
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
    void lid(Layer layer, int alpha, double bright) {
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
    boolean cutLine(double ax, double ay, double az, double bx, double by, double bz) {
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
    void lift(double[] points, int i, double lift) {
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
}
