package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

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
 *
 * <p>It is built up floor by floor, each on top of the one before: {@link PainterCore} (the three layers, the light on
 * a solid side, and the lines, quads and corners everything is drawn from), {@link PainterLight} (light),
 * {@link PainterCut} (cutting off at a plane) and {@link PainterSolid} (one solid box or round part at a time). This
 * class adds where a shape stands ({@link Frame}), the shapes themselves ({@link Shape}) and drawing them whole or
 * breaking up.
 */
public class ConstructPainter extends PainterSolid {
    /** A cube of one block round its middle: chunks thrown up by a blow. */
    private static final double[][] CUBE = { { -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 1.0 } };
    /**
     * One link of a chain (see {@link #chain}): a flat oval ring lying round y with its long way along z, one long and
     * three quarters of that wide at scale 1.
     */
    private static final Mesh LINK = Mesh.torus(12, 6, 0.3, 0.075, 1.15).scaled(1.0, 1.0, 1.335);
    // How far apart two links sit, as a part of a link's length: less than one, so they hook into each other.
    private static final double CHAIN_STEP = 0.72;

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

    // How far the pieces of a construct breaking up right now fly, next to how far they usually do (see fling).
    private double fling = 1.0;

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
        super(pose, camera, time, frustum, material, hand);
    }

    /**
     * A painter for constructs in your own hands in first person, drawn along with your hands: everything is given in
     * blocks in front of your eyes (x to the right, y up, -z ahead), the camera sits at 0, and nothing close by fades
     * out, since that is exactly where your hands are.
     */
    public static ConstructPainter hand(PoseStack pose, float time, Material material) {
        return new ConstructPainter(pose, Vec3.ZERO, time, null, material, true);
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

    /** A solid cube of the material, turned by {@code angle} about {@code axis}: a chunk thrown up by a blow. */
    public void chunk(Vec3 at, double size, Vec3 axis, double angle, double bright) {
        if (size <= 0.0) {
            return;
        }
        Vec3 forward = Vectors.spin(new Vec3(0, 0, 1), axis, angle);
        Vec3 up = Vectors.spin(Vectors.UP, axis, angle);
        this.model(CUBE, new Frame(at, forward.cross(up), up, forward, size), 1.0, bright);
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
     * How far the pieces of what breaks up from now on fly, next to how far they usually do (1 once it is done): a thing
     * as big as a plane is flung far further apart than a fist.
     */
    public void fling(double amount) {
        this.fling = Math.max(0.0, amount);
    }
}
