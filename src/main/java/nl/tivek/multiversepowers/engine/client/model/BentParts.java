package nl.tivek.multiversepowers.engine.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Model parts drawn bent at a knee, an elbow or a waist for the creature being drawn now, each cube cut across the
// part where it bends and its far half turned about the knee. The halves stay whole and square: each reaches on past
// the cut and is trimmed where it would pass into the other, on the plane halfway between them, so up to a square bend
// they meet in a mitred corner and past it each ends flat, as two blocks would; a sharp fold neither thins nor
// stretches. A twist about the bone is shared out along both halves, so the knee itself only folds. What a bent trunk
// carries as parts of its own (a robe, a tail) bends with it where it lies. Only the render thread draws, so the
// scratch is shared.
public final class BentParts {
    // A part drawn bent: its bend and the far half's whole turn in the bent part's own axes, and the fold; for a part
    // drawn inside the bent one, how its pixels lie in the bent part's and back, and so its faces.
    private record Bent(ModelBends.Bend bend, Matrix3f turn, Fold fold, @Nullable Matrix4f into,
            @Nullable Matrix4f back, @Nullable Matrix3f intoFaces, @Nullable Matrix3f backFaces) {
    }

    // A fold in the bent part's pixels: the cut across it (a point lies past it, in the far half, where
    // cut . (x, y, z) + cut[3] is above 0); the bone, the way from the near half to the far; how far the part reaches
    // along it either way from the cut; its twist about the bone, none of it at the near end, half at the cut and all
    // at the far end. Seen with the cut turned by half the twist the knee only folds: the planes halfway between the
    // halves as the near half lies and as the far half lay before it turned (each half keeps what lies on its own
    // side); the way into the fold; how far past the cut each half reaches for every pixel a cube stands out on the
    // outside of the fold, never further than square; whether the fold is sharper than square, so the halves end flat;
    // and how each half is moved from there, and so its faces.
    private record Fold(float[] cut, float[] bone, float low, float high, float twist, float[] near, float[] far,
            float[] in, float reach, boolean sharp, Matrix4f nearMove, Matrix4f farMove, Matrix3f nearFaces,
            Matrix3f farFaces) {
    }

    private static final Map<ModelPart, Bent> BENT = new IdentityHashMap<>();
    private static final Map<ModelPart.Cube, float[][]> FACES = new WeakHashMap<>();
    private static final float CUT = 1.0E-3F;
    // A face as it is trimmed, one plane after another.
    private static final float[][] ROUND_AT = { new float[3 * 10], new float[3 * 10], new float[3 * 10] };
    private static final float[][] ROUND_UV = { new float[2 * 10], new float[2 * 10], new float[2 * 10] };
    // Where a cube ends at the knee: the near half's reach past the cut and the far half's.
    private static final float[] NEAR_END = new float[4];
    private static final float[] FAR_END = new float[4];
    private static final Vector3f POINT = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector3f FACING = new Vector3f();
    private static final Quaternionf SPIN = new Quaternionf();
    private static final Quaternionf FAR_TURN = new Quaternionf();

    private BentParts() {
    }

    // Draws `part` bent by `turn` (the far half's turn in the part's own axes) until clear().
    public static void bend(ModelPart part, ModelBends.Bend bend, Quaternionf turn) {
        bend(part, bend, turn, Set.of());
    }

    // As above, and the parts drawn inside it bend along with it, all but those in `apart` (parts that are posed on
    // their own, and all that is drawn inside them).
    public static void bend(ModelPart part, ModelBends.Bend bend, Quaternionf turn, Set<ModelPart> apart) {
        Matrix3f whole = new Matrix3f().set(turn);
        Fold fold = fold(part, bend, turn);
        BENT.put(part, new Bent(bend, whole, fold, null, null, null, null));
        if (!part.children.isEmpty()) {
            inner(part, bend, whole, fold, new Matrix4f(), apart);
        }
    }

    // How `part` folds by `turn` about its knee.
    private static Fold fold(ModelPart part, ModelBends.Bend bend, Quaternionf turn) {
        float[] k = bend.knee();
        int axis = bend.axis();
        Vector3f bone = new Vector3f();
        bone.setComponent(axis, bend.farSign());
        // How far the part reaches along its bone either way from the cut, a pixel at least.
        float low = -1.0F;
        float high = 1.0F;
        for (ModelPart.Cube cube : part.cubes) {
            float a = bend.farSign() * ((axis == 0 ? cube.minX : axis == 1 ? cube.minY : cube.minZ) - bend.at());
            float b = bend.farSign() * ((axis == 0 ? cube.maxX : axis == 1 ? cube.maxY : cube.maxZ) - bend.at());
            low = Math.min(low, Math.min(a, b));
            high = Math.max(high, Math.max(a, b));
        }
        Quaternionf q = new Quaternionf(turn);
        if (q.w < 0.0F) {
            q.set(-q.x, -q.y, -q.z, -q.w);
        }
        float twist = 2.0F * (float) Math.atan2(q.x * bone.x + q.y * bone.y + q.z * bone.z, q.w);
        // The turn with its twist taken out, seen from the cut turned by half the twist: there the knee only folds.
        Quaternionf half = new Quaternionf().fromAxisAngleRad(bone, twist * 0.5F);
        Quaternionf swing = new Quaternionf(q).mul(new Quaternionf().fromAxisAngleRad(bone, -twist));
        Quaternionf seen = new Quaternionf(half).conjugate().mul(swing).mul(half);
        if (seen.w < 0.0F) {
            seen.set(-seen.x, -seen.y, -seen.z, -seen.w);
        }
        Vector3f hinge = new Vector3f(seen.x, seen.y, seen.z);
        float angle = 2.0F * (float) Math.acos(Math.min(1.0F, seen.w));
        if (hinge.lengthSquared() < 1.0E-12F) {
            hinge.set(bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]);
            angle = 0.0F;
        }
        hinge.normalize();
        // Towards the inside of the fold, where the far half goes.
        Vector3f in = new Vector3f(hinge).cross(bone).normalize();
        float c = (float) Math.cos(angle * 0.5F);
        float s = (float) Math.sin(angle * 0.5F);
        Matrix3f nearFaces = new Matrix3f().set(half);
        Matrix3f farFaces = new Matrix3f().set(new Quaternionf(swing).mul(half));
        return new Fold(cutPlane(bend), new float[] { bone.x, bone.y, bone.z }, low, high, twist,
                through(k, new Vector3f(bone).mul(c).add(new Vector3f(in).mul(s))),
                through(k, new Vector3f(bone).mul(c).sub(new Vector3f(in).mul(s))), new float[] { in.x, in.y, in.z },
                Math.min(1.0F, s / c), s > c, about(k, nearFaces), about(k, farFaces), nearFaces, farFaces);
    }

    // A turn about the knee, in pixels.
    private static Matrix4f about(float[] k, Matrix3f turn) {
        return new Matrix4f().translation(k[0], k[1], k[2]).mul(new Matrix4f().set(turn)).translate(-k[0], -k[1],
                -k[2]);
    }

    // The cut across a bent part, in its own pixels.
    private static float[] cutPlane(ModelBends.Bend bend) {
        float[] plane = new float[4];
        plane[bend.axis()] = bend.farSign();
        plane[3] = -bend.at() * bend.farSign();
        return plane;
    }

    // The plane through the knee square to `normal`.
    private static float[] through(float[] k, Vector3f normal) {
        return new float[] { normal.x, normal.y, normal.z, -(normal.x * k[0] + normal.y * k[1] + normal.z * k[2]) };
    }

    // Every part drawn inside a bent one, `into` taking its parent's pixels to the bent part's: the same fold, seen from
    // its own frame.
    private static void inner(ModelPart parent, ModelBends.Bend bend, Matrix3f turn, Fold fold, Matrix4f into,
            Set<ModelPart> apart) {
        for (ModelPart child : parent.children.values()) {
            if (apart.contains(child)) {
                continue;
            }
            Matrix4f own = new Matrix4f(into).translate(child.x, child.y, child.z);
            if (child.xRot != 0.0F || child.yRot != 0.0F || child.zRot != 0.0F) {
                own.rotateZYX(child.zRot, child.yRot, child.xRot);
            }
            if (child.xScale != 1.0F || child.yScale != 1.0F || child.zScale != 1.0F) {
                own.scale(child.xScale, child.yScale, child.zScale);
            }
            Matrix4f back = new Matrix4f(own).invert();
            BENT.put(child, new Bent(bend, turn, fold, own, back, own.normal(new Matrix3f()),
                    back.normal(new Matrix3f())));
            if (!child.children.isEmpty()) {
                inner(child, bend, turn, fold, own, apart);
            }
        }
    }

    // A copy of the model a layer draws (armour, a sheep's wool) bends where the model itself does, and so do the
    // parts drawn inside it.
    public static void same(ModelPart from, ModelPart to) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(from);
        if (bent == null) {
            return;
        }
        BENT.put(to, bent);
        for (Map.Entry<String, ModelPart> child : from.children.entrySet()) {
            ModelPart other = to.children.get(child.getKey());
            if (other != null && other != child.getValue()) {
                same(child.getValue(), other);
            }
        }
    }

    public static void clear() {
        BENT.clear();
    }

    // The far half's turn a part is drawn with now, or null when it is drawn straight.
    @Nullable
    public static Matrix3f turn(ModelPart part) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        return bent == null ? null : bent.turn();
    }

    // Where a point of a part (pixels, in its own frame) is drawn when the part is bent: up to the cut with the near
    // half, past it with the far one, twisted as the part's own faces are there (something drawn on the skin, a line).
    public static Vector3f place(ModelPart part, float x, float y, float z, Vector3f out) {
        out.set(x, y, z);
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent == null) {
            return out;
        }
        Fold fold = bent.fold();
        if (bent.into() != null) {
            bent.into().transformPosition(out);
        }
        float along = side(fold.cut(), out.x, out.y, out.z);
        boolean near = along <= 0.0F;
        spin(bent.bend().knee(), fold.bone(), out, twisted(fold, along, near));
        (near ? fold.nearMove() : fold.farMove()).transformPosition(out);
        return bent.back() == null ? out : bent.back().transformPosition(out);
    }

    private static float side(float[] plane, float x, float y, float z) {
        return plane[0] * x + plane[1] * y + plane[2] * z + plane[3];
    }

    // How far a point `along` the bone from the cut is twisted, seen from the cut: not at all past it, and the more the
    // nearer its half's end, back by half the twist at the near end and on by half at the far end.
    private static float twisted(Fold fold, float along, boolean near) {
        float half = 0.5F * fold.twist();
        float share = near ? along / fold.low() : along / fold.high();
        return (near ? -half : half) * Math.max(0.0F, Math.min(1.0F, share));
    }

    // Turns a point about the bone through the knee, or a direction when there is no knee.
    private static void spin(@Nullable float[] k, float[] bone, Vector3f point, float angle) {
        if (angle == 0.0F) {
            return;
        }
        SPIN.fromAxisAngleRad(bone[0], bone[1], bone[2], angle);
        if (k == null) {
            SPIN.transform(point);
            return;
        }
        SPIN.transform(point.sub(k[0], k[1], k[2])).add(k[0], k[1], k[2]);
    }

    // Something held at a bent limb's far end (an item in the hand) moves along with its far half.
    public static void farHalf(ModelPart part, PoseStack pose) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent == null) {
            return;
        }
        float[] knee = bent.bend().knee();
        pose.translate(knee[0] / 16.0F, knee[1] / 16.0F, knee[2] / 16.0F);
        pose.mulPose(bent.turn().getNormalizedRotation(FAR_TURN));
        pose.translate(-knee[0] / 16.0F, -knee[1] / 16.0F, -knee[2] / 16.0F);
    }

    // Draws a bent part's cubes in place of ModelPart.compile; false when the part is not bent.
    public static boolean draw(ModelPart part, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
            int color) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent == null) {
            return false;
        }
        List<float[][]> cubes = new ArrayList<>(part.cubes.size());
        for (ModelPart.Cube cube : part.cubes) {
            float[][] faces = faces(cube);
            if (faces == null) {
                return false;
            }
            cubes.add(faces);
        }
        for (float[][] faces : cubes) {
            folded(buffer, pose, bent, faces, light, overlay, color);
        }
        return true;
    }

    // A face: its normal, then each corner's x, y, z (pixels) and u, v.
    private static int corners(float[] face) {
        return (face.length - 3) / 5;
    }

    // A cube's faces as plain numbers, read from the game's own once: its faces' types are hidden inside ModelPart.
    @Nullable
    private static float[][] faces(ModelPart.Cube cube) {
        if (FACES.containsKey(cube)) {
            return FACES.get(cube);
        }
        float[][] faces = null;
        try {
            Field polygons = ModelPart.Cube.class.getDeclaredField("polygons");
            polygons.setAccessible(true);
            Object[] all = (Object[]) polygons.get(cube);
            faces = new float[all.length][];
            for (int k = 0; k < all.length; k++) {
                Class<?> polygon = all[k].getClass();
                Field vertices = polygon.getDeclaredField("vertices");
                Field normal = polygon.getDeclaredField("normal");
                vertices.setAccessible(true);
                normal.setAccessible(true);
                Object[] corners = (Object[]) vertices.get(all[k]);
                Vector3f facing = (Vector3f) normal.get(all[k]);
                float[] face = new float[3 + 5 * corners.length];
                face[0] = facing.x;
                face[1] = facing.y;
                face[2] = facing.z;
                for (int i = 0; i < corners.length; i++) {
                    Class<?> vertex = corners[i].getClass();
                    Field pos = vertex.getDeclaredField("pos");
                    Field u = vertex.getDeclaredField("u");
                    Field v = vertex.getDeclaredField("v");
                    pos.setAccessible(true);
                    u.setAccessible(true);
                    v.setAccessible(true);
                    Vector3f at = (Vector3f) pos.get(corners[i]);
                    face[3 + 5 * i] = at.x;
                    face[4 + 5 * i] = at.y;
                    face[5 + 5 * i] = at.z;
                    face[6 + 5 * i] = u.getFloat(corners[i]);
                    face[7 + 5 * i] = v.getFloat(corners[i]);
                }
                faces[k] = face;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A game whose cubes cannot be read draws its limbs straight.
            faces = null;
        }
        FACES.put(cube, faces);
        return faces;
    }

    // A cube folded: each face's near piece and far piece, both reaching on past the cut as far as the fold needs,
    // twisted as far as they lie along the bone and trimmed on the plane halfway between the halves. Folded sharper than
    // square, each half ends flat at the knee, closed with the cube's own end: the far end's face for the near half,
    // the near end's for the far one.
    private static void folded(VertexConsumer buffer, PoseStack.Pose pose, Bent bent, float[][] faces, int light,
            int overlay, int color) {
        Fold fold = bent.fold();
        float[] cut = fold.cut();
        float[] k = bent.bend().knee();
        float[] in = fold.in();
        float low = Float.POSITIVE_INFINITY;
        float high = Float.NEGATIVE_INFINITY;
        float out = 0.0F;
        for (float[] face : faces) {
            for (int i = 0; i < corners(face); i++) {
                corner(bent, face, i, POINT);
                float along = side(cut, POINT.x, POINT.y, POINT.z);
                low = Math.min(low, along);
                high = Math.max(high, along);
                out = Math.max(out, -(in[0] * (POINT.x - k[0]) + in[1] * (POINT.y - k[1])
                        + in[2] * (POINT.z - k[2])));
            }
        }
        // Only a cube across the knee reaches on past it.
        boolean across = low < -CUT && high > CUT;
        float reach = across ? out * fold.reach() : 0.0F;
        System.arraycopy(cut, 0, NEAR_END, 0, 4);
        System.arraycopy(cut, 0, FAR_END, 0, 4);
        NEAR_END[3] -= reach;
        FAR_END[3] += reach;
        for (float[] face : faces) {
            int n = corners(face);
            FACING.set(face[0], face[1], face[2]);
            if (bent.intoFaces() != null) {
                bent.intoFaces().transform(FACING).normalize();
            }
            load(bent, face, n, Float.NaN);
            piece(buffer, pose, bent, true, 1, clip(0, n, NEAR_END, 1.0F, 1), light, overlay, color);
            piece(buffer, pose, bent, false, 1, clip(0, n, FAR_END, -1.0F, 1), light, overlay, color);
            if (!across || !fold.sharp()) {
                continue;
            }
            boolean atLow = true;
            boolean atHigh = true;
            for (int i = 0; i < n; i++) {
                float along = side(cut, ROUND_AT[0][3 * i], ROUND_AT[0][3 * i + 1], ROUND_AT[0][3 * i + 2]);
                atLow &= Math.abs(along - low) <= CUT;
                atHigh &= Math.abs(along - high) <= CUT;
            }
            if (atHigh) {
                load(bent, face, n, reach);
                piece(buffer, pose, bent, true, 0, n, light, overlay, color);
            } else if (atLow) {
                load(bent, face, n, -reach);
                piece(buffer, pose, bent, false, 0, n, light, overlay, color);
            }
        }
    }

    // Corner i of a face, in the bent part's pixels.
    private static void corner(Bent bent, float[] face, int i, Vector3f out) {
        out.set(face[3 + 5 * i], face[4 + 5 * i], face[5 + 5 * i]);
        if (bent.into() != null) {
            bent.into().transformPosition(out);
        }
    }

    // A face's corners into the first round of the scratch, in the bent part's pixels, slid along the bone onto `at`
    // past the cut unless NaN.
    private static void load(Bent bent, float[] face, int n, float at) {
        float[] cut = bent.fold().cut();
        for (int i = 0; i < n; i++) {
            corner(bent, face, i, POINT);
            if (!Float.isNaN(at)) {
                float slide = side(cut, POINT.x, POINT.y, POINT.z) - at;
                POINT.sub(cut[0] * slide, cut[1] * slide, cut[2] * slide);
            }
            ROUND_AT[0][3 * i] = POINT.x;
            ROUND_AT[0][3 * i + 1] = POINT.y;
            ROUND_AT[0][3 * i + 2] = POINT.z;
            System.arraycopy(face, 6 + 5 * i, ROUND_UV[0], 2 * i, 2);
        }
    }

    // One half's piece of a face (`count` corners in round `from`): each corner twisted as far as it lies along the
    // bone, the piece trimmed on the plane halfway between the halves and drawn where its half is moved, as squares with
    // a corner doubled where needed.
    private static void piece(VertexConsumer buffer, PoseStack.Pose pose, Bent bent, boolean near, int from,
            int count, int light, int overlay, int color) {
        if (count < 3) {
            return;
        }
        Fold fold = bent.fold();
        float[] at = ROUND_AT[from];
        for (int i = 0; i < count; i++) {
            POINT.set(at[3 * i], at[3 * i + 1], at[3 * i + 2]);
            spin(bent.bend().knee(), fold.bone(), POINT,
                    twisted(fold, side(fold.cut(), POINT.x, POINT.y, POINT.z), near));
            at[3 * i] = POINT.x;
            at[3 * i + 1] = POINT.y;
            at[3 * i + 2] = POINT.z;
        }
        int trimmed = clip(from, count, near ? fold.near() : fold.far(), near ? 1.0F : -1.0F, 2);
        for (int k = 1; k + 1 < trimmed; k += 2) {
            point(buffer, pose, bent, near, 0, light, overlay, color);
            point(buffer, pose, bent, near, k, light, overlay, color);
            point(buffer, pose, bent, near, k + 1, light, overlay, color);
            point(buffer, pose, bent, near, Math.min(k + 2, trimmed - 1), light, overlay, color);
        }
    }

    // Keeps what lies where keep * (plane . p + plane[3]) is at most 0, from one round of the scratch into another.
    private static int clip(int from, int count, float[] plane, float keep, int to) {
        float[] at = ROUND_AT[from];
        float[] uv = ROUND_UV[from];
        float[] outAt = ROUND_AT[to];
        float[] outUv = ROUND_UV[to];
        int out = 0;
        for (int i = 0; i < count && out < 9; i++) {
            int j = (i + 1) % count;
            float di = keep * side(plane, at[3 * i], at[3 * i + 1], at[3 * i + 2]);
            float dj = keep * side(plane, at[3 * j], at[3 * j + 1], at[3 * j + 2]);
            if (di <= CUT) {
                System.arraycopy(at, 3 * i, outAt, 3 * out, 3);
                System.arraycopy(uv, 2 * i, outUv, 2 * out, 2);
                out++;
            }
            if (di < -CUT && dj > CUT || di > CUT && dj < -CUT) {
                float u = di / (di - dj);
                for (int a = 0; a < 3; a++) {
                    outAt[3 * out + a] = at[3 * i + a] + (at[3 * j + a] - at[3 * i + a]) * u;
                }
                for (int a = 0; a < 2; a++) {
                    outUv[2 * out + a] = uv[2 * i + a] + (uv[2 * j + a] - uv[2 * i + a]) * u;
                }
                out++;
            }
        }
        return out;
    }

    // One corner of a trimmed piece (round 2) where its half is moved, with its face's normal twisted as it is.
    private static void point(VertexConsumer buffer, PoseStack.Pose pose, Bent bent, boolean near, int i, int light,
            int overlay, int color) {
        Fold fold = bent.fold();
        POINT.set(ROUND_AT[2][3 * i], ROUND_AT[2][3 * i + 1], ROUND_AT[2][3 * i + 2]);
        NORMAL.set(FACING);
        spin(null, fold.bone(), NORMAL, twisted(fold, side(fold.cut(), POINT.x, POINT.y, POINT.z), near));
        (near ? fold.nearMove() : fold.farMove()).transformPosition(POINT);
        (near ? fold.nearFaces() : fold.farFaces()).transform(NORMAL);
        if (bent.back() != null) {
            bent.back().transformPosition(POINT);
            bent.backFaces().transform(NORMAL);
        }
        pose.transformNormal(NORMAL.normalize(), NORMAL);
        pose.pose().transformPosition(POINT.x / 16.0F, POINT.y / 16.0F, POINT.z / 16.0F, POINT);
        buffer.addVertex(POINT.x, POINT.y, POINT.z, color, ROUND_UV[2][2 * i], ROUND_UV[2][2 * i + 1], overlay, light,
                NORMAL.x, NORMAL.y, NORMAL.z);
    }
}
