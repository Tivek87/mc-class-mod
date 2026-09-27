package nl.tivek.multiversepowers.engine.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Model parts drawn bent at a knee or an elbow for the creature being drawn now: each cube cut across the limb where
// it bends, the far half turned about the knee and the cut itself half as far, so the two halves stay joined as a
// wedge instead of opening a gap. Only the render thread draws, so the scratch below is shared.
public final class BentParts {
    private record Bent(ModelParts.Bend bend, Matrix3f turn, Matrix3f half) {
    }

    private static final Map<ModelPart, Bent> BENT = new IdentityHashMap<>();
    private static final Map<ModelPart.Cube, float[][]> FACES = new WeakHashMap<>();
    private static final float CUT = 1.0E-3F;
    private static final float[] AT = new float[3 * 8];
    private static final float[] UV = new float[2 * 8];
    private static final int[] SIDE = new int[8];
    private static final Vector3f POINT = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Quaternionf HALF = new Quaternionf();

    private BentParts() {
    }

    // Draws `part` bent by `turn` (the far half's turn in the part's own axes) until clear().
    public static void bend(ModelPart part, ModelParts.Bend bend, Quaternionf turn) {
        HALF.identity().slerp(turn, 0.5F);
        BENT.put(part, new Bent(bend, new Matrix3f().set(turn), new Matrix3f().set(HALF)));
    }

    // A copy of the model a layer draws (armour) bends where the model itself does.
    public static void same(ModelPart from, ModelPart to) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(from);
        if (bent != null) {
            BENT.put(to, bent);
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
        ModelParts.Bend bend = bent.bend();
        int axis = bend.axis();
        Matrix4f matrix = pose.pose();
        for (float[][] faces : cubes) {
            for (float[] face : faces) {
                int n = corners(face);
                boolean near = false;
                boolean far = false;
                for (int i = 0; i < n; i++) {
                    float side = (face[3 + 5 * i + axis] - bend.at()) * bend.farSign();
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

    // A face across the knee drawn as its near and its far piece, cut where the limb bends.
    private static void cut(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, Bent bent, float[] face,
            int n, int light, int overlay, int color) {
        ModelParts.Bend bend = bent.bend();
        int axis = bend.axis();
        for (int piece = 0; piece < 2; piece++) {
            float keep = piece == 0 ? -1.0F : 1.0F;
            int count = 0;
            for (int i = 0; i < n && count < 8; i++) {
                int a = 3 + 5 * i;
                int b = 3 + 5 * ((i + 1) % n);
                float da = (face[a + axis] - bend.at()) * bend.farSign() * keep;
                float db = (face[b + axis] - bend.at()) * bend.farSign() * keep;
                if (da >= -CUT) {
                    count = put(count, face[a], face[a + 1], face[a + 2], face[a + 3], face[a + 4],
                            Math.abs(da) <= CUT ? 0 : 1);
                }
                if (da < -CUT && db > CUT || da > CUT && db < -CUT) {
                    float u = da / (da - db);
                    count = put(count, face[a] + (face[b] - face[a]) * u, face[a + 1] + (face[b + 1] - face[a + 1]) * u,
                            face[a + 2] + (face[b + 2] - face[a + 2]) * u, face[a + 3] + (face[b + 3] - face[a + 3]) * u,
                            face[a + 4] + (face[b + 4] - face[a + 4]) * u, 0);
                }
            }
            if (count != 4) {
                // Only a cube's own square faces are cut, and those always into two squares.
                continue;
            }
            boolean farPiece = piece == 1;
            normal(pose, bent, face, farPiece);
            for (int i = 0; i < 4; i++) {
                vertex(buffer, matrix, bent, AT[3 * i], AT[3 * i + 1], AT[3 * i + 2], UV[2 * i], UV[2 * i + 1],
                        SIDE[i] == 0 ? 1 : farPiece ? 2 : 0, light, overlay, color);
            }
        }
    }

    private static int put(int count, float x, float y, float z, float u, float v, int side) {
        AT[3 * count] = x;
        AT[3 * count + 1] = y;
        AT[3 * count + 2] = z;
        UV[2 * count] = u;
        UV[2 * count + 1] = v;
        SIDE[count] = side;
        return count + 1;
    }

    private static void emit(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose pose, Bent bent, float[] face,
            int n, boolean far, int light, int overlay, int color) {
        normal(pose, bent, face, far);
        ModelParts.Bend bend = bent.bend();
        for (int i = 0; i < n; i++) {
            int a = 3 + 5 * i;
            boolean onCut = Math.abs(face[a + bend.axis()] - bend.at()) <= CUT;
            vertex(buffer, matrix, bent, face[a], face[a + 1], face[a + 2], face[a + 3], face[a + 4],
                    onCut ? 1 : far ? 2 : 0, light, overlay, color);
        }
    }

    private static void normal(PoseStack.Pose pose, Bent bent, float[] face, boolean far) {
        NORMAL.set(face[0], face[1], face[2]);
        if (far) {
            bent.turn().transform(NORMAL);
        }
        pose.transformNormal(NORMAL, NORMAL);
    }

    // One corner, in pixels of the part's own frame: turned not at all (0), half as far (1, on the cut) or all the
    // way (2, the far half) about the knee.
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Bent bent, float x, float y, float z, float u,
            float v, int turned, int light, int overlay, int color) {
        POINT.set(x, y, z);
        if (turned > 0) {
            ModelParts.Bend bend = bent.bend();
            POINT.sub(bend.knee()[0], bend.knee()[1], bend.knee()[2]);
            (turned == 1 ? bent.half() : bent.turn()).transform(POINT);
            POINT.add(bend.knee()[0], bend.knee()[1], bend.knee()[2]);
        }
        matrix.transformPosition(POINT.x / 16.0F, POINT.y / 16.0F, POINT.z / 16.0F, POINT);
        buffer.addVertex(POINT.x, POINT.y, POINT.z, color, u, v, overlay, light, NORMAL.x, NORMAL.y, NORMAL.z);
    }
}
