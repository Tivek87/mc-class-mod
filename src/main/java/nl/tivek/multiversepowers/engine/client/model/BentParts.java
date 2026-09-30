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

// Model parts drawn bent at their joints for the creature being drawn now: an arm at its elbow and wrist, a leg at its
// knee and ankle, a trunk at its waist and pelvis (FoldChain), each cube cut across the part where it bends and each
// piece past a cut turned about that joint. What a bent trunk carries as parts of its own (a robe, a tail) bends with
// it where it lies.
public final class BentParts {
    // A part drawn bent: its joints; for a part drawn inside the bent one, how its pixels lie in the bent part's and
    // back, and so its faces.
    private record Bent(FoldChain chain, @Nullable Matrix4f into, @Nullable Matrix4f back,
            @Nullable Matrix3f intoFaces, @Nullable Matrix3f backFaces) {
    }

    private static final Map<ModelPart, Bent> BENT = new IdentityHashMap<>();
    private static final Map<ModelPart.Cube, float[][]> FACES = new WeakHashMap<>();

    private BentParts() {
    }

    // Draws `part` bent by `turn` (the far half's turn in the part's own axes) until clear().
    public static void bend(ModelPart part, ModelBends.Bend bend, Quaternionf turn) {
        bend(part, bend, turn, Set.of());
    }

    // As above, and the parts drawn inside it bend along with it, all but those in `apart` (parts that are posed on
    // their own, and all that is drawn inside them).
    public static void bend(ModelPart part, ModelBends.Bend bend, Quaternionf turn, Set<ModelPart> apart) {
        bend(part, new ModelBends.Bend[] { bend }, new Quaternionf[] { turn }, apart);
    }

    // Draws `part` bent at each of `bends` (its joints from its near end, ModelBends.chain) by `turns`, each the turn of
    // the piece past that joint in the axes of the piece before it; the parts drawn inside it bend along, all but
    // those in `apart`.
    public static void bend(ModelPart part, ModelBends.Bend[] bends, Quaternionf[] turns, Set<ModelPart> apart) {
        if (bends.length == 0) {
            BENT.remove(part);
            return;
        }
        FoldChain chain = FoldChain.of(part, bends, turns);
        BENT.put(part, new Bent(chain, null, null, null, null));
        if (!part.children.isEmpty()) {
            inner(part, chain, new Matrix4f(), apart);
        }
    }

    // Every part drawn inside a bent one, `into` taking its parent's pixels to the bent part's: the same joints, seen
    // from its own frame.
    private static void inner(ModelPart parent, FoldChain chain, Matrix4f into, Set<ModelPart> apart) {
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
            BENT.put(child, new Bent(chain, own, back, own.normal(new Matrix3f()), back.normal(new Matrix3f())));
            if (!child.children.isEmpty()) {
                inner(child, chain, own, apart);
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

    // The far half's turn a part is drawn with now (past its first joint), or null when it is drawn straight.
    @Nullable
    public static Matrix3f turn(ModelPart part) {
        return turn(part, 0);
    }

    // The turn of the piece past joint `joint` (0 the first, a knee or waist; 1 the next, an ankle or pelvis) in the
    // axes of the piece before it, or null when the part is not drawn bent there.
    @Nullable
    public static Matrix3f turn(ModelPart part, int joint) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        return bent == null || joint >= bent.chain().joints() ? null : bent.chain().turn(joint);
    }

    // The frame (pixels, in the part's own) of the piece past `piece` joints as a whole, every joint before it turned:
    // `out` multiplied by it; left as it is when the part is not drawn bent that far.
    public static Matrix4f piece(ModelPart part, int piece, Matrix4f out) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        return bent == null || piece == 0 || bent.into() != null || piece > bent.chain().joints() ? out
                : out.mul(bent.chain().frame(piece));
    }

    // Where a point of a part (pixels, in its own frame) is drawn when the part is bent: with the piece it lies in,
    // twisted as the part's own faces are there (something drawn on the skin, a line).
    public static Vector3f place(ModelPart part, float x, float y, float z, Vector3f out) {
        out.set(x, y, z);
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent == null) {
            return out;
        }
        if (bent.into() != null) {
            bent.into().transformPosition(out);
        }
        bent.chain().place(out);
        return bent.back() == null ? out : bent.back().transformPosition(out);
    }

    // Something held at a bent limb's far end (an item in the hand) moves along with its last piece.
    public static void farHalf(ModelPart part, PoseStack pose) {
        Bent bent = BENT.isEmpty() ? null : BENT.get(part);
        if (bent != null) {
            bent.chain().farEnd(pose);
        }
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
            bent.chain().draw(buffer, pose, faces, bent.into(), bent.back(), bent.intoFaces(), bent.backFaces(), light,
                    overlay, color);
        }
        return true;
    }

    // A cube's faces as plain numbers, read from the game's own once: its faces' types are hidden inside ModelPart. A
    // face: its normal, then each corner's x, y, z (pixels) and u, v.
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

}
