package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

public class ConstructPainter extends PainterSolid {
    private static final double[][] CUBE = { { -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 1.0 } };
    private static final Mesh LINK = Mesh.torus(12, 6, 0.3, 0.075, 1.15).scaled(1.0, 1.0, 1.335);
    private static final double CHAIN_STEP = 0.72;

    public record Frame(Vec3 center, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        public Vec3 at(double x, double y, double z) {
            double sx = x * this.scale;
            double sy = y * this.scale;
            double sz = z * this.scale;
            return new Vec3(this.center.x + this.right.x * sx + this.up.x * sy + this.forward.x * sz,
                    this.center.y + this.right.y * sx + this.up.y * sy + this.forward.y * sz,
                    this.center.z + this.right.z * sx + this.up.z * sy + this.forward.z * sz);
        }

        public Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.center);
            return new Vec3(way.dot(this.right) / (this.right.lengthSqr() * this.scale),
                    way.dot(this.up) / (this.up.lengthSqr() * this.scale),
                    way.dot(this.forward) / (this.forward.lengthSqr() * this.scale));
        }

        public Vec3 normal(Vec3 model) {
            Vec3 way = this.right.scale(model.x / this.right.lengthSqr())
                    .add(this.up.scale(model.y / this.up.lengthSqr()))
                    .add(this.forward.scale(model.z / this.forward.lengthSqr()));
            double length = way.length();
            return length < 1.0E-12 ? Vec3.ZERO : way.scale(1.0 / length);
        }

        public Frame moved(double x, double y, double z) {
            return new Frame(this.at(x, y, z), this.right, this.up, this.forward, this.scale);
        }

        public Frame turned(double px, double py, double pz, double ax, double ay, double az, double angle) {
            Vec3 axis = this.right.scale(ax).add(this.up.scale(ay)).add(this.forward.scale(az)).normalize();
            // A left-handed frame (right = forward x up) needs the angle flipped to turn the way it looks.
            double turn = this.right.cross(this.up).dot(this.forward) < 0.0 ? -angle : angle;
            Vec3 pivot = this.at(px, py, pz);
            return new Frame(pivot.add(Vectors.spin(this.center.subtract(pivot), axis, turn)),
                    Vectors.spin(this.right, axis, turn), Vectors.spin(this.up, axis, turn),
                    Vectors.spin(this.forward, axis, turn), this.scale);
        }

        public Frame stretched(double x, double y, double z) {
            return new Frame(this.center, this.right.scale(x), this.up.scale(y), this.forward.scale(z), this.scale);
        }

        public double stretch() {
            return Math.sqrt(Math.max(this.right.lengthSqr(), Math.max(this.up.lengthSqr(), this.forward.lengthSqr())));
        }

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

    public record Shape(double[][] boxes, Mesh... meshes) {
        private static final double[][] NO_BOXES = new double[0][];

        public static Shape of(Mesh... meshes) {
            return new Shape(NO_BOXES, meshes);
        }
    }

    private double fling = 1.0;

    public ConstructPainter(PoseStack pose, Vec3 camera, float time, Material material) {
        this(pose, camera, time, null, material);
    }

    public ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material) {
        this(pose, camera, time, frustum, material, false);
    }

    protected ConstructPainter(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

    public static ConstructPainter hand(PoseStack pose, float time, Material material) {
        return new ConstructPainter(pose, Vec3.ZERO, time, null, material, true);
    }

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

    public void shattered(double[][] model, Frame frame, double apart, double bright) {
        this.shattered(model, frame, apart, bright, 0);
    }

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

    public void shape(Shape shape, Frame frame, double solid, double bright) {
        if (shape.boxes().length > 0) {
            this.model(shape.boxes(), frame, solid, bright);
        }
        for (Mesh mesh : shape.meshes()) {
            this.mesh(mesh, frame, solid, bright);
        }
    }

    public void shattered(Shape shape, Frame frame, double apart, double bright) {
        this.shattered(shape, frame, apart, bright, 0);
    }

    public void shattered(Shape shape, Frame frame, double apart, double bright, int seed) {
        if (shape.boxes().length > 0) {
            this.shattered(shape.boxes(), frame, apart, bright, seed);
        }
        for (int k = 0; k < shape.meshes().length; k++) {
            this.shatteredMesh(shape.meshes()[k], frame, seed + shape.boxes().length + k, apart, bright);
        }
    }

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

    public void chunk(Vec3 at, double size, Vec3 axis, double angle, double bright) {
        if (size <= 0.0) {
            return;
        }
        Vec3 forward = Vectors.spin(new Vec3(0, 0, 1), axis, angle);
        Vec3 up = Vectors.spin(Vectors.UP, axis, angle);
        this.model(CUBE, new Frame(at, forward.cross(up), up, forward, size), 1.0, bright);
    }

    public void seeThrough(Shape shape, Frame frame, double faint, double bright) {
        this.faint = Math.max(1.0E-3, faint);
        for (Mesh mesh : shape.meshes()) {
            this.mesh(mesh, frame, 1.0, bright);
        }
        this.faint = 0.0;
    }

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

    public void fling(double amount) {
        this.fling = Math.max(0.0, amount);
    }
}
