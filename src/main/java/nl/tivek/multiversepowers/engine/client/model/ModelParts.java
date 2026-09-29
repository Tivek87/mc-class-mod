package nl.tivek.multiversepowers.engine.client.model;

import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.ColorableAgeableListModel;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import nl.tivek.multiversepowers.mixin.client.render.AgeableHierarchicalModelAccess;
import nl.tivek.multiversepowers.mixin.client.render.AgeableListModelAccess;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// The parts of a creature's model a ragdoll moves: its top parts (each with the parts that only copy it, such as a
// hat or a sleeve) and the parts they hang in. Works for every model built the vanilla way from ModelParts.
public final class ModelParts {
    // One moving part: the chain of parts it hangs in (outermost first), the parts that copy it, how big it is in its
    // own frame (pixels: minX, minY, minZ, maxX, maxY, maxZ), whether it is a head or an arm or leg of a person,
    // which of a young model's two groups it is drawn in, and its name (for a profile to tune it by).
    public record Part(ModelPart part, List<ModelPart> parents, List<ModelPart> followers, float[] bounds, Role role,
            int group, String name) {
    }

    public static final int ONE_GROUP = 0;
    public static final int HEADS = 1;
    public static final int BODIES = 2;
    private static final int MOST = 24;
    private static final float SAME = 1.0E-4F;

    // Models whose way of drawing a ragdoll knows: those that draw their parts as the vanilla bases do. One that
    // draws them some way of its own (another mod's) is left to die as in the plain game.
    private static final ClassValue<Boolean> KNOWN = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                if (c == AgeableListModel.class || c == HierarchicalModel.class || c == ListModel.class) {
                    return true;
                }
                boolean vanilla = c == AgeableHierarchicalModel.class || c == CamelModel.class
                        || c == ColorableAgeableListModel.class || c == ColorableHierarchicalModel.class
                        || c == TurtleModel.class;
                if (!vanilla && draws(c)) {
                    return false;
                }
            }
            return false;
        }
    };

    private static boolean draws(Class<?> type) {
        try {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("renderToBuffer")) {
                    return true;
                }
            }
            return false;
        } catch (LinkageError | SecurityException e) {
            return true;
        }
    }

    // Two models are of one kind when they share a family below the vanilla bases (a sheep and its wool, a pig and
    // its saddle), or are both people.
    public static boolean kin(EntityModel<?> a, EntityModel<?> b) {
        if (a.getClass() == b.getClass() || a instanceof HumanoidModel<?> && b instanceof HumanoidModel<?>) {
            return true;
        }
        for (Class<?> c = a.getClass(); c != null; c = c.getSuperclass()) {
            if (c == AgeableListModel.class || c == HierarchicalModel.class || c == ListModel.class
                    || c == AgeableHierarchicalModel.class || c == ColorableAgeableListModel.class
                    || c == ColorableHierarchicalModel.class || c == EntityModel.class) {
                return false;
            }
            if (c.isInstance(b)) {
                return true;
            }
        }
        return false;
    }

    public enum Role {
        HEAD,
        BODY,
        ARM,
        LEG,
        OTHER
    }

    // A part's frame in the model as it was built (each part's first pose), in pixels.
    public static Matrix4f rest(Part part, Matrix4f out) {
        out.identity();
        for (ModelPart parent : part.parents()) {
            first(out, parent);
        }
        return first(out, part.part());
    }

    static Matrix4f first(Matrix4f frame, ModelPart part) {
        PartPose pose = part.getInitialPose();
        frame.translate(pose.x, pose.y, pose.z);
        if (pose.xRot != 0.0F || pose.yRot != 0.0F || pose.zRot != 0.0F) {
            frame.rotateZYX(pose.zRot, pose.yRot, pose.xRot);
        }
        return frame;
    }

    private ModelParts() {
    }

    public static boolean known(EntityModel<?> model) {
        return KNOWN.get(model.getClass());
    }

    // null when this is not a model a ragdoll can move.
    @Nullable
    public static List<Part> of(EntityModel<?> model) {
        if (!known(model)) {
            return null;
        }
        List<Part> parts = new ArrayList<>();
        if (model instanceof HumanoidModel<?> humanoid) {
            boolean player = model instanceof PlayerModel<?>;
            PlayerModel<?> p = player ? (PlayerModel<?>) model : null;
            add(parts, humanoid.head, List.of(), List.of(humanoid.hat), Role.HEAD, HEADS, "head");
            add(parts, humanoid.body, List.of(), player ? List.of(p.jacket) : List.of(), Role.BODY, BODIES, "body");
            add(parts, humanoid.rightArm, List.of(), player ? List.of(p.rightSleeve) : List.of(), Role.ARM, BODIES,
                    "right_arm");
            add(parts, humanoid.leftArm, List.of(), player ? List.of(p.leftSleeve) : List.of(), Role.ARM, BODIES,
                    "left_arm");
            add(parts, humanoid.rightLeg, List.of(), player ? List.of(p.rightPants) : List.of(), Role.LEG, BODIES,
                    "right_leg");
            add(parts, humanoid.leftLeg, List.of(), player ? List.of(p.leftPants) : List.of(), Role.LEG, BODIES,
                    "left_leg");
        } else if (model instanceof AgeableListModel<?> ageable) {
            AgeableListModelAccess lists = (AgeableListModelAccess) ageable;
            Map<ModelPart, String> names = fieldNames(model);
            addAll(parts, lists.welcomescreen$headParts(), Role.HEAD, HEADS, names);
            addAll(parts, lists.welcomescreen$bodyParts(), Role.OTHER, BODIES, names);
        } else if (model instanceof HierarchicalModel<?> hierarchical) {
            List<ModelPart> chain = new ArrayList<>();
            ModelPart node = hierarchical.root();
            String name = "root";
            // Past the empty wrappers (a root holding one part that holds the rest).
            while (node.cubes.isEmpty() && node.children.size() == 1) {
                chain.add(node);
                Map.Entry<String, ModelPart> only = node.children.entrySet().iterator().next();
                name = only.getKey();
                node = only.getValue();
            }
            if (!node.cubes.isEmpty()) {
                add(parts, node, List.copyOf(chain), List.of(), Role.BODY, ONE_GROUP, name);
            }
            chain.add(node);
            for (Map.Entry<String, ModelPart> entry : node.children.entrySet()) {
                add(parts, entry.getValue(), List.copyOf(chain), List.of(), roleOf(entry.getKey(), Role.OTHER),
                        ONE_GROUP, entry.getKey());
            }
        } else if (model instanceof ListModel<?> list) {
            addAll(parts, list.parts(), Role.OTHER, ONE_GROUP, fieldNames(model));
        } else {
            return null;
        }
        return parts.size() >= 2 && parts.size() <= MOST ? parts : null;
    }

    private static Role roleOf(String name, Role otherwise) {
        return name.contains("head") ? Role.HEAD : name.contains("arm") ? Role.ARM : name.contains("leg") ? Role.LEG
                : name.equals("body") ? Role.BODY : otherwise;
    }

    // A part that sits exactly where an earlier one of its group does, turned the same way (a chicken's beak on its
    // head), only copies that one.
    private static void addAll(List<Part> parts, Iterable<ModelPart> list, Role role, int group,
            Map<ModelPart, String> names) {
        int index = 0;
        for (ModelPart part : list) {
            Part leader = null;
            for (Part earlier : parts) {
                if (earlier.group() == group && same(earlier.part(), part)) {
                    leader = earlier;
                    break;
                }
            }
            String name = names.getOrDefault(part, "part_" + index);
            index++;
            if (leader == null) {
                add(parts, part, List.of(), new ArrayList<>(), role == Role.HEAD ? Role.HEAD : roleOf(name, role),
                        group, name);
            } else if (part.visible) {
                leader.followers().add(part);
            }
        }
    }

    // What the model calls each of its parts (its field names, rightHindLeg as right_hind_leg), for profiles to name
    // a part by: models built from lists keep no names of their own.
    private static Map<ModelPart, String> fieldNames(EntityModel<?> model) {
        Map<ModelPart, String> names = new IdentityHashMap<>();
        for (Class<?> c = model.getClass(); c != null && c != EntityModel.class; c = c.getSuperclass()) {
            try {
                for (Field field : c.getDeclaredFields()) {
                    if (field.getType() != ModelPart.class || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (field.get(model) instanceof ModelPart part) {
                        names.putIfAbsent(part, snake(field.getName()));
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // A field that cannot be read keeps the part nameless: it is still moved, only not tuned by name.
            }
        }
        return names;
    }

    private static String snake(String name) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                out.append('_');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static boolean same(ModelPart a, ModelPart b) {
        return Math.abs(a.x - b.x) < SAME && Math.abs(a.y - b.y) < SAME && Math.abs(a.z - b.z) < SAME
                && Math.abs(a.xRot - b.xRot) < SAME && Math.abs(a.yRot - b.yRot) < SAME
                && Math.abs(a.zRot - b.zRot) < SAME;
    }

    private static void add(List<Part> parts, ModelPart part, List<ModelPart> parents, List<ModelPart> followers,
            Role role, int group, String name) {
        if (!part.visible) {
            return;
        }
        float[] bounds = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
        grow(part, new Matrix4f(), bounds, new Vector3f());
        if (bounds[0] > bounds[3]) {
            return;
        }
        parts.add(new Part(part, parents, followers, bounds, role, group, name));
    }

    // The frame of a part as drawn this frame: the model matrix, a young model's smaller drawing, each part it hangs
    // in, then its own pose.
    public static Matrix4f frame(EntityModel<?> model, Matrix4f drawn, Part part, Matrix4f out) {
        parentFrame(model, drawn, part, out);
        apply(out, part.part());
        return out;
    }

    public static Matrix4f parentFrame(EntityModel<?> model, Matrix4f drawn, Part part, Matrix4f out) {
        out.set(drawn);
        young(model, part.group(), out);
        List<ModelPart> parents = part.parents();
        for (int i = 0; i < parents.size(); i++) {
            apply(out, parents.get(i));
        }
        return out;
    }

    // What ModelPart.translateAndRotate does to a pose stack, on a plain matrix.
    public static void apply(Matrix4f frame, ModelPart part) {
        frame.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
        if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
            frame.rotateZYX(part.zRot, part.yRot, part.xRot);
        }
        if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
            frame.scale(part.xScale, part.yScale, part.zScale);
        }
    }

    // The move a young model makes before it draws a group of its parts: its head and body drawn smaller.
    public static void young(EntityModel<?> model, int group, Matrix4f frame) {
        if (!model.young) {
            return;
        }
        if (model instanceof CamelModel<?>) {
            // CamelModel draws its young this way, in its own renderToBuffer.
            frame.scale(0.45F);
            frame.translate(0.0F, 1.834375F, 0.0F);
        } else if (model instanceof AgeableHierarchicalModel<?> ageable) {
            AgeableHierarchicalModelAccess young = (AgeableHierarchicalModelAccess) ageable;
            frame.scale(young.welcomescreen$youngScaleFactor());
            frame.translate(0.0F, young.welcomescreen$bodyYOffset() / 16.0F, 0.0F);
        } else if (model instanceof AgeableListModel<?> ageable) {
            AgeableListModelAccess young = (AgeableListModelAccess) ageable;
            if (group == HEADS) {
                if (young.welcomescreen$scaleHead()) {
                    frame.scale(1.5F / young.welcomescreen$babyHeadScale());
                }
                frame.translate(0.0F, young.welcomescreen$babyYHeadOffset() / 16.0F,
                        young.welcomescreen$babyZHeadOffset() / 16.0F);
            } else {
                frame.scale(1.0F / young.welcomescreen$babyBodyScale());
                frame.translate(0.0F, young.welcomescreen$bodyYOffset() / 16.0F, 0.0F);
            }
        }
    }

    // The part of another model of the same kind (the copy a layer draws, such as a sheep's wool) that stands where
    // `part` stands in `original`; null when the copy has no such part.
    @Nullable
    public static ModelPart counterpart(EntityModel<?> original, EntityModel<?> copy, ModelPart part) {
        if (!kin(original, copy)) {
            return null;
        }
        if (original instanceof HumanoidModel<?> from && copy instanceof HumanoidModel<?> to) {
            ModelPart[] a = humanoid(from);
            ModelPart[] b = humanoid(to);
            for (int i = 0; i < a.length && i < b.length; i++) {
                if (a[i] == part) {
                    return b[i];
                }
            }
            return null;
        }
        if (original instanceof AgeableListModel<?> from && copy instanceof AgeableListModel<?> to) {
            List<ModelPart> a = listed((AgeableListModelAccess) from);
            List<ModelPart> b = listed((AgeableListModelAccess) to);
            int i = a.indexOf(part);
            return i >= 0 && a.size() == b.size() ? b.get(i) : null;
        }
        if (original instanceof HierarchicalModel<?> from && copy instanceof HierarchicalModel<?> to) {
            List<String> path = new ArrayList<>();
            return find(from.root(), part, path) ? follow(to.root(), path) : null;
        }
        return null;
    }

    private static ModelPart[] humanoid(HumanoidModel<?> model) {
        if (model instanceof PlayerModel<?> player) {
            return new ModelPart[] { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg,
                    model.leftLeg, player.jacket, player.rightSleeve, player.leftSleeve, player.rightPants,
                    player.leftPants };
        }
        return new ModelPart[] { model.head, model.hat, model.body, model.rightArm, model.leftArm, model.rightLeg,
                model.leftLeg };
    }

    private static List<ModelPart> listed(AgeableListModelAccess model) {
        List<ModelPart> all = new ArrayList<>();
        model.welcomescreen$headParts().forEach(all::add);
        model.welcomescreen$bodyParts().forEach(all::add);
        return all;
    }

    private static boolean find(ModelPart node, ModelPart part, List<String> path) {
        if (node == part) {
            return true;
        }
        for (Map.Entry<String, ModelPart> child : node.children.entrySet()) {
            path.add(child.getKey());
            if (find(child.getValue(), part, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    @Nullable
    private static ModelPart follow(ModelPart node, List<String> path) {
        for (String name : path) {
            node = node.children.get(name);
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    // Everything a part draws, in its own frame: its cubes and those of the parts on it where they sit now.
    static void grow(ModelPart part, Matrix4f frame, float[] bounds, Vector3f corner) {
        for (ModelPart.Cube cube : part.cubes) {
            for (int c = 0; c < 8; c++) {
                corner.set((c & 1) == 0 ? cube.minX : cube.maxX, (c & 2) == 0 ? cube.minY : cube.maxY,
                        (c & 4) == 0 ? cube.minZ : cube.maxZ);
                frame.transformPosition(corner);
                bounds[0] = Math.min(bounds[0], corner.x);
                bounds[1] = Math.min(bounds[1], corner.y);
                bounds[2] = Math.min(bounds[2], corner.z);
                bounds[3] = Math.max(bounds[3], corner.x);
                bounds[4] = Math.max(bounds[4], corner.y);
                bounds[5] = Math.max(bounds[5], corner.z);
            }
        }
        for (ModelPart child : part.children.values()) {
            if (!child.visible) {
                continue;
            }
            Matrix4f inner = new Matrix4f(frame).translate(child.x, child.y, child.z)
                    .rotate(Axis.ZP.rotation(child.zRot)).rotate(Axis.YP.rotation(child.yRot))
                    .rotate(Axis.XP.rotation(child.xRot)).scale(child.xScale, child.yScale, child.zScale);
            grow(child, inner, bounds, corner);
        }
    }
}
