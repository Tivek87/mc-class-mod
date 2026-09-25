package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;

abstract class PainterCut extends PainterLight {
    private static final double SEAM_WIDTH = 1.5;
    private static final double LID_JOIN = 1.0E-8;

    boolean clipping;
    private double clipX;
    private double clipY;
    private double clipZ;
    private double clipNormalX;
    private double clipNormalY;
    private double clipNormalZ;
    private double seam;
    final double[] cutIn = new double[12];
    private final double[] cutAhead = new double[4];
    // Sized for the clip's own invariant: a 4-corner side cut by one plane keeps at most 6 corners.
    private final double[] cutOut = new double[18];
    private final double[] cutCross = new double[12];
    final double[] cutEnds = new double[6];
    private double[] lidCuts = new double[6 * 32];
    int lidCount;
    private int[] lidOutline = new int[32];
    private int[] lidWaiting = new int[32];

    PainterCut(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material, boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

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

    public void noClip() {
        this.clipping = false;
    }

    double ahead(double x, double y, double z) {
        return (x - this.clipX) * this.clipNormalX + (y - this.clipY) * this.clipNormalY
                + (z - this.clipZ) * this.clipNormalZ;
    }

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

    private void lidCut(int from) {
        if (6 * (this.lidCount + 1) > this.lidCuts.length) {
            this.lidCuts = Arrays.copyOf(this.lidCuts, this.lidCuts.length * 2);
            this.lidOutline = new int[this.lidCuts.length / 6];
            this.lidWaiting = new int[this.lidCuts.length / 6];
        }
        System.arraycopy(this.cutCross, from, this.lidCuts, 6 * this.lidCount, 6);
        this.lidCount++;
    }

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
            this.put(layer, cx, cy, cz, rgb, middle);
            this.put(layer, cuts[i], cuts[i + 1], cuts[i + 2], rgb, this.faded(cuts[i], cuts[i + 1], cuts[i + 2],
                    alpha));
            int end = this.faded(cuts[i + 3], cuts[i + 4], cuts[i + 5], alpha);
            this.put(layer, cuts[i + 3], cuts[i + 4], cuts[i + 5], rgb, end);
            this.put(layer, cuts[i + 3], cuts[i + 4], cuts[i + 5], rgb, end);
        }
    }

    private void putCut(Layer layer, int corner, int rgb, int alpha) {
        double[] out = this.cutOut;
        int i = 3 * corner;
        this.put(layer, out[i], out[i + 1], out[i + 2], rgb, this.faded(out[i], out[i + 1], out[i + 2], alpha));
    }

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
            double length = Math.sqrt(sq(ends[3] - ends[0]) + sq(ends[4] - ends[1]) + sq(ends[5] - ends[2]));
            this.line(this.glow, ends[0], ends[1], ends[2], ends[3], ends[4], ends[5],
                    Math.min(HALO_WIDTH * fine * SEAM_WIDTH, 0.9 * length), this.material.glow(),
                    Colors.alpha(HALO * strength));
        }
    }

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
        int back = frontA ? 3 : 0;
        int front = 3 - back;
        double t = frontA ? b / (b - a) : a / (a - b);
        for (int k = 0; k < 3; k++) {
            ends[back + k] += (ends[front + k] - ends[back + k]) * t;
        }
        return true;
    }

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
