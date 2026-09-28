package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;

abstract class PainterSolid extends PainterCut {
    private static final double EDGE_OUT = 0.02;
    private static final double FINE_SIZE = 0.35;
    private static final double FINEST = 0.35;
    private static final double SEE_THROUGH_EDGE = 0.55;
    private static final double CREASE_GLOW = 0.5;
    private static final int[][] SIDES = { { 0, 2, 6, 4 }, { 1, 5, 7, 3 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 },
            { 0, 1, 3, 2 }, { 4, 6, 7, 5 } };

    record ModelInfo(boolean[] covered, double x, double y, double z, double radius) {
    }

    // Arrays have no equals/hashCode, so this caches by identity: the same model array must be reused, not rebuilt.
    private static final Map<double[][], ModelInfo> MODELS = new WeakHashMap<>();
    private static final int CACHED_FROM = 4;

    final double[] corner = new double[24];
    private final double[] liftedCorner = new double[24];
    double[] wx = new double[256];
    double[] wy = new double[256];
    double[] wz = new double[256];
    double[] nx = new double[256];
    double[] ny = new double[256];
    double[] nz = new double[256];
    private boolean[] facingCamera = new boolean[256];
    double faint;
    double creases;
    // Set just before a mesh is drawn when all of it lies too far from the eye to fade: no point needs its distance.
    boolean farFromEye;

    PainterSolid(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

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

    @Override
    void saveState(double[] state) {
        super.saveState(state);
        state[10] = this.faint;
        state[11] = this.creases;
    }

    @Override
    void loadState(double[] state) {
        super.loadState(state);
        this.faint = state[10];
        this.creases = state[11];
    }

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

    void drawMesh(Mesh mesh, double width, double solid, double bright, boolean halo) {
        int count = mesh.sides.length;
        boolean faint = this.faint > 0.0;
        Layer sides = faint ? this.light : this.mass;
        int body = Colors.alpha(faint ? this.faint * solid : solid);
        double quiet = faint ? SEE_THROUGH_EDGE : 1.0;
        double fine = width * mesh.fine;
        // A skin's sides facing away lie behind the body it covers, unless a cut or a see-through look opens it.
        boolean hidden = mesh.skin && !faint && !this.clipping;
        this.nearFade = !this.farFromEye;
        this.farFromEye = false;
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
            if (hidden && look <= 0.0) {
                continue;
            }
            double away = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double face = away < 1.0E-6 ? 1.0 : Math.abs(look) / away;
            double ripple = 0.9 + 0.06 * Math.sin(this.time * 0.5 - mesh.middleZ[s] * 4.0);
            double light = (light(x, y, z) + this.ambient) * sheen(face) * ripple * bright * mesh.bright[s];
            int rgb = this.mass(light);
            if (mesh.glow != null && mesh.glow[s] > 0.0) {
                double glow = mesh.glow[s];
                rgb = Colors.mix(rgb, this.material.edge(), (float) glow);
                if (glow > 1.0) {
                    rgb = Colors.mix(rgb, this.material.hot(), (float) (glow - 1.0));
                }
            }
            if (this.clipping) {
                this.cutAt(sides, side, rgb, body, fine, solid * quiet, halo);
            } else {
                this.quadAt(sides, side[0], side[1], side[2], side[3], rgb, body);
            }
        }
        if (this.clipping) {
            this.lid(sides, body, bright);
        }
        int edge = Colors.alpha(EDGE * solid * quiet);
        int glowing = halo ? Colors.alpha(HALO * solid * quiet) : 0;
        int creaseEdge = Colors.alpha(EDGE * solid * quiet * this.creases);
        int creaseGlow = halo ? Colors.alpha(HALO * solid * quiet * this.creases * CREASE_GLOW) : 0;
        double lift = 0.01 + 0.02 * fine;
        for (int e = 0; e < mesh.edgeFrom.length; e++) {
            int left = mesh.edgeLeft[e];
            int right = mesh.edgeRight[e];
            boolean crease = false;
            if (right < 0 && mesh.skin) {
                continue;
            }
            if (left >= 0 && right >= 0 && this.facingCamera[left] == this.facingCamera[right]) {
                if (mesh.skin || this.creases <= 0.0 || !this.facingCamera[left] || !mesh.edgeCrease[e]) {
                    continue;
                }
                crease = true;
            }
            int from = mesh.edgeFrom[e];
            int to = mesh.edgeTo[e];
            double ax = this.wx[from];
            double ay = this.wy[from];
            double az = this.wz[from];
            double bx = this.wx[to];
            double by = this.wy[to];
            double bz = this.wz[to];
            if (this.clipping) {
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
            int aura = crease ? creaseGlow : glowing;
            double length = aura > 0 ? Math.sqrt(sq(bx - ax) + sq(by - ay) + sq(bz - az)) : 0.0;
            this.linePair(ax, ay, az, bx, by, bz, EDGE_WIDTH * fine, this.material.edge(), crease ? creaseEdge : edge,
                    Math.min(HALO_WIDTH * fine, 0.9 * length), this.material.glow(), aura);
        }
        this.nearFade = false;
    }

    void box(double[][] model, ModelInfo info, int index, Vec3 view, double width, double solid,
            double bright, double depth, double charge, boolean glowing) {
        double[] at = this.corner;
        Vec3 camera = this.camera;
        double ripple = 0.9 + (0.06 + 0.06 * charge) * Math.sin(this.time * 0.5 - depth * 4.0);
        int body = Colors.alpha(solid);
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
        int creaseEdge = Colors.alpha(EDGE * ripple * solid * this.creases);
        int creaseHalo = glowing ? Colors.alpha(HALO * solid * this.creases * CREASE_GLOW) : 0;
        for (int i = 0; i < 8; i++) {
            for (int axis = 0; axis < 3; axis++) {
                int bit = 1 << axis;
                int seen = (i & bit) != 0 ? 0 : rim(model, info, index, i, axis, view);
                if (seen == 0 || seen == 2 && this.creases <= 0.0) {
                    continue;
                }
                int lineAlpha = seen == 2 ? creaseEdge : edge;
                int haloAlpha = seen == 2 ? creaseHalo : halo;
                int a = 3 * i;
                int b = 3 * (i | bit);
                if (this.clipping) {
                    if (this.cutLine(at[a], at[a + 1], at[a + 2], at[b], at[b + 1], at[b + 2])) {
                        double[] ends = this.cutEnds;
                        this.lift(ends, 0, lift);
                        this.lift(ends, 3, lift);
                        this.linePair(ends[0], ends[1], ends[2], ends[3], ends[4], ends[5], EDGE_WIDTH * fine,
                                edgeRgb, lineAlpha, HALO_WIDTH * fine, glowRgb, haloAlpha);
                    }
                    continue;
                }
                this.linePair(lifted[a], lifted[a + 1], lifted[a + 2], lifted[b], lifted[b + 1], lifted[b + 2],
                        EDGE_WIDTH * fine, edgeRgb, lineAlpha, HALO_WIDTH * fine, glowRgb, haloAlpha);
            }
        }
    }

    // How much of the model's creases, the edges between two sides you see both of, to draw (0 = only outlines).
    public void creases(double strength) {
        this.creases = Math.max(0.0, strength);
    }

    private void quadCorners(Layer layer, int[] side, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double[] at = this.corner;
        int i0 = 3 * side[0];
        int i1 = 3 * side[1];
        int i2 = 3 * side[2];
        int i3 = 3 * side[3];
        this.quad4(layer, at[i0], at[i0 + 1], at[i0 + 2], this.faded(at[i0], at[i0 + 1], at[i0 + 2], alpha),
                at[i1], at[i1 + 1], at[i1 + 2], this.faded(at[i1], at[i1 + 1], at[i1 + 2], alpha),
                at[i2], at[i2 + 1], at[i2 + 2], this.faded(at[i2], at[i2 + 1], at[i2 + 2], alpha),
                at[i3], at[i3 + 1], at[i3 + 2], this.faded(at[i3], at[i3 + 1], at[i3 + 2], alpha), rgb);
    }

    public static double fine(double x, double y, double z) {
        double middle = Math.max(Math.min(x, y), Math.min(Math.max(x, y), z));
        return Mth.clamp(middle / FINE_SIZE, FINEST, 1.0);
    }

    // 1 for an outline (one of its two sides faces you), 2 for a crease (both do), 0 for neither or hidden inside.
    private static int rim(double[][] model, ModelInfo info, int index, int corner, int axis, Vec3 view) {
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
        return info.covered()[index * 24 + corner * 3 + axis] ? 0 : facing;
    }

    private static double axis(Vec3 point, int index) {
        return index == 0 ? point.x : index == 1 ? point.y : point.z;
    }

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

    private void quadAt(Layer layer, int a, int b, int c, int d, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double[] x = this.wx;
        double[] y = this.wy;
        double[] z = this.wz;
        this.quad4(layer, x[a], y[a], z[a], this.faded(x[a], y[a], z[a], alpha), x[b], y[b], z[b],
                this.faded(x[b], y[b], z[b], alpha), x[c], y[c], z[c], this.faded(x[c], y[c], z[c], alpha), x[d],
                y[d], z[d], this.faded(x[d], y[d], z[d], alpha), rgb);
    }
}
