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

// Model parts drawn bent at a knee, an elbow or a waist for the creature being drawn now: each cube cut across the
// part where it bends, the far half turned about the knee and the cut itself half as far, so the two halves stay
// joined as a wedge instead of opening a gap. What a bent trunk carries as parts of its own (a robe, a tail) bends
// with it where it lies. Only the render thread draws, so the scratch below is shared.
public final class BentParts {
    // A part drawn bent: the bend and the far half's turn in the bent part's own axes; the cut in this part's own
    // pixels (a point lies past it, in the far half, where plane . (x, y, z) + plane[3] is above 0); where its points
    // go, past the cut and on it; and how the far half's faces turn.
    private record Bent(ModelParts.Bend bend, Matrix3f turn, float[] plane, Matrix4f far, Matrix4f half,
            Matrix3f farFaces) {
    }

    private static final Map<ModelPart, Bent> BENT = new IdentityHashMap<>();
    private static final Map<ModelPart.Cube, float[][]> FACES = new WeakHashMap<>();
    private static final float CUT = 1.0E-3F;
    private static final float[] AT = new float[3 * 8];
    private static final float[] UV = new float[2 * 8];
    private static final int[] TURNED = new int[8];
    private static final Vector3f POINT = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Quaternionf HALF = new Quaternionf();
    private static final Quaternionf FAR_TURN = new Quaternionf();

    private BentParts() {
    }

    // Draws `part` bent by `turn` (the far half's turn in the part's own axes) until clear().
    public static void bend(ModelPart part, ModelParts.Bend bend, Quaternionf turn) {
        bend(part, bend, turn, Set.of());
    }

    // As above, and the parts drawn inside it bend along with it, all but those in `apart` (parts that are posed on
    // their own, and all that is drawn inside them).
    public static void bend(ModelPart part, ModelParts.Bend bend, Quaternionf turn, Set<ModelPart> apart) {
        HALF.identity().slerp(turn, 0.5F);
        Matrix3f whole = new Matrix3f().set(turn);
        float[] k = bend.knee();
        Matrix4f far = new Matrix4f().translation(k[0], k[1], k[2]).mul(new Matrix4f().set(whole))
                .translate(-k[0], -k[1], -k[2]);
        Matrix4f half = new Matrix4f().translation(k[0], k[1], k[2]).mul(new Matrix4f().set(HALF))
                .translate(-k[0], -k[1], -k[2]);
        float[] plane = new float[4];
        plane[bend.axis()] = bend.farSign();
        plane[3] = -bend.at() * bend.farSign();
        BENT.put(part, new Bent(bend, whole, plane, far, half, whole));
        if (!part.children.isEmpty()) {
            inner(part, bend, whole, plane, far, half, new Matrix4f(), apart);
        }
    }

    // Every part drawn inside a bent one, `into` taking its parent's pixels to the bent part's: the same cut and the
    // same moves, seen from its own frame.
    private static void inner(ModelPart parent, ModelParts.Bend bend, Matrix3f turn, float[] plane, Matrix4f far,
            Matrix4f half, Matrix4f into, Set<ModelPart> apart) {
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
            float[] cut = { plane[0] * own.m00() + plane[1] * own.m01() + plane[2] * own.m02(),
                    plane[0] * own.m10() + plane[1] * own.m11() + plane[2] * own.m12(),
                    plane[0] * own.m20() + plane[1] * own.m21() + plane[2] * own.m22(),
                    plane[0] * own.m30() + plane[1] * own.m31() + plane[2] * own.m32() + plane[3] };
            Matrix4f moved = new Matrix4f(back).mul(far).mul(own);
            BENT.put(child, new Bent(bend, turn, cut, moved, new Matrix4f(back).mul(half).mul(own),
                    moved.normal(new Matrix3f())));
            if (!child.children.isEmpty()) {
                inner(child, bend, turn, plane, far, half, own, apart);
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

    // Where a point of a part (pixels, in its own frame) is drawn when the part is bent: past the cut turned with the
    // far half, on the cut half as far, as the part's own faces are (something drawn on the skin, such as a line).
    public static Vector3f place(ModelPart part, float x, float y, float z, Vector3f out) {
        out.set(x, y, z);
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent == null) {
            return out;
        }
        float side = side(bent.plane(), x, y, z);
        if (side < -CUT) {
            return out;
        }
        return (side <= CUT ? bent.half() : bent.far()).transformPosition(out);
    }

    private static float side(float[] plane, float x, float y, float z) {
        return plane[0] * x + plane[1] * y + plane[2] * z + plane[3];
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
        Matrix4f matrix = pose.pose();
        for (float[][] faces : cubes) {
            for (float[] face : faces) {
                int n = corners(face);
                boolean near = false;
                boolean far = false;
                for (int i = 0; i < n; i++) {
                    float side = side(bent.plane(), face, 3 + 5 * i);
                    near |= side < -CUT;
                    far |= side > CUT;
                }
                if (!far) {
                    emit(buffer, matrix, pose, bent, face, n, false, light, overlay, color);
                } else if (!near) {
                    emit(buffer, matrix, pose, bent, face, n, true, light, overlay, color);
                } else {
                    cut(buffer, matrix, pose, bent, face, n, light, overlay, color);
                }
            }
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

    // A face across the cut drawn as its near and its far piece. A cube's face square to the cut splits into two
    // squares; one cut aslant (a part turned inside a bent trunk) can leave a triangle and a five-sided piece, drawn
    // as squares with a corner doubled.
    private static void cut(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, Bent bent, float[] face,
            int n, int light, int overlay, int color) {
        for (int piece = 0; piece < 2; piece++) {
            float keep = piece == 0 ? -1.0F : 1.0F;
            int count = 0;
            for (int i = 0; i < n && count < 7; i++) {
                int a = 3 + 5 * i;
                int b = 3 + 5 * ((i + 1) % n);
                float da = side(bent.plane(), face, a) * keep;
                float db = side(bent.plane(), face, b) * keep;
                if (da >= -CUT) {
                    count = put(count, face[a], face[a + 1], face[a + 2], face[a + 3], face[a + 4],
                            Math.abs(da) <= CUT ? 1 : piece == 1 ? 2 : 0);
                }
                if (da < -CUT && db > CUT || da > CUT && db < -CUT) {
                    float u = da / (da - db);
                    count = put(count, face[a] + (face[b] - face[a]) * u, face[a + 1] + (face[b + 1] - face[a + 1]) * u,
                            face[a + 2] + (face[b + 2] - face[a + 2]) * u, face[a + 3] + (face[b + 3] - face[a + 3]) * u,
                            face[a + 4] + (face[b + 4] - face[a + 4]) * u, 1);
                }
            }
            if (count < 3) {
                continue;
            }
            normal(pose, bent, face, piece == 1);
            for (int k = 1; k + 1 < count; k += 2) {
                corner(buffer, matrix, bent, 0, light, overlay, color);
                corner(buffer, matrix, bent, k, light, overlay, color);
                corner(buffer, matrix, bent, k + 1, light, overlay, color);
                corner(buffer, matrix, bent, Math.min(k + 2, count - 1), light, overlay, color);
            }
        }
    }

    private static int put(int count, float x, float y, float z, float u, float v, int turned) {
        AT[3 * count] = x;
        AT[3 * count + 1] = y;
        AT[3 * count + 2] = z;
        UV[2 * count] = u;
        UV[2 * count + 1] = v;
        TURNED[count] = turned;
        return count + 1;
    }

    private static void corner(VertexConsumer buffer, Matrix4f matrix, Bent bent, int i, int light, int overlay,
            int color) {
        vertex(buffer, matrix, bent, AT[3 * i], AT[3 * i + 1], AT[3 * i + 2], UV[2 * i], UV[2 * i + 1], TURNED[i],
                light, overlay, color);
    }

    private static void emit(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, Bent bent, float[] face,
            int n, boolean far, int light, int overlay, int color) {
        normal(pose, bent, face, far);
        for (int i = 0; i < n; i++) {
            int a = 3 + 5 * i;
            boolean onCut = Math.abs(side(bent.plane(), face, a)) <= CUT;
            vertex(buffer, matrix, bent, face[a], face[a + 1], face[a + 2], face[a + 3], face[a + 4],
                    onCut ? 1 : far ? 2 : 0, light, overlay, color);
        }
    }

    // How far past the cut a face's corner (at face[a]) lies.
    private static float side(float[] plane, float[] face, int a) {
        return side(plane, face[a], face[a + 1], face[a + 2]);
    }

    private static void normal(PoseStack.Pose pose, Bent bent, float[] face, boolean far) {
        NORMAL.set(face[0], face[1], face[2]);
        if (far) {
            bent.farFaces().transform(NORMAL).normalize();
        }
        pose.transformNormal(NORMAL, NORMAL);
    }

    // One corner, in pixels of the part's own frame: moved not at all (0), as the cut is (1) or as the far half (2).
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Bent bent, float x, float y, float z, float u,
            float v, int turned, int light, int overlay, int color) {
        POINT.set(x, y, z);
        if (turned > 0) {
            (turned == 1 ? bent.half() : bent.far()).transformPosition(POINT);
        }
        matrix.transformPosition(POINT.x / 16.0F, POINT.y / 16.0F, POINT.z / 16.0F, POINT);
        buffer.addVertex(POINT.x, POINT.y, POINT.z, color, u, v, overlay, light, NORMAL.x, NORMAL.y, NORMAL.z);
    }
}
