package nl.tivek.multiversepowers.engine.client.pose;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import nl.tivek.multiversepowers.engine.math.Segments;
import org.joml.Matrix3f;
import org.joml.Vector3f;

// Keeps a person's arms out of their chest and their legs out of each other, whatever pose they were given, by
// turning the limb out about its own forward line as little as it takes. A pose that clears already stays exactly as
// it was.
public final class PoseGuard {
    // How far a limb may sink into the chest or the other leg before it is turned out, in pixels: vanilla's own
    // poses (a bow drawn, a crossbow held) brush the chest a little.
    private static final float SKIN = 0.75F;
    // The limb is looked at as a round rod a little thinner than its square block, whose edges may touch.
    private static final float ROUND = 0.8F;
    // The most a limb is turned out, in radians.
    private static final float FURTHEST = 1.4F;
    private static final int SCAN = 8;
    private static final int REFINE = 6;
    private static final int SAMPLES = 5;
    // The part of a limb that is looked at: from this far down it (the shoulder or hip may touch the body) to its end.
    private static final float FROM = 0.45F;

    private static final Map<ModelPart, float[]> BOUNDS = new WeakHashMap<>();
    private static final Matrix3f TURN = new Matrix3f();
    private static final Matrix3f BODY = new Matrix3f();
    private static final Vector3f POINT = new Vector3f();
    private static final double[] A0 = new double[3];
    private static final double[] A1 = new double[3];
    private static final double[] B0 = new double[3];
    private static final double[] B1 = new double[3];
    private static final double[] ST = new double[2];

    private PoseGuard() {
    }

    public static void guard(HumanoidModel<?> model) {
        out(model.rightArm, model.body);
        out(model.leftArm, model.body);
        apart(model.rightLeg, model.leftLeg);
    }

    // Turns the limb out of the body about its z axis, the smallest turn either way that clears it.
    static void out(ModelPart limb, ModelPart body) {
        if (!limb.visible || !body.visible || depth(limb, body) <= SKIN) {
            return;
        }
        float base = limb.zRot;
        float best = Float.NaN;
        float leastDepth = Float.POSITIVE_INFINITY;
        float leastTurn = 0.0F;
        for (int sign = -1; sign <= 1; sign += 2) {
            float cleared = Float.NaN;
            float before = 0.0F;
            for (int k = 1; k <= SCAN && Float.isNaN(cleared); k++) {
                float turn = FURTHEST * k / SCAN;
                limb.zRot = base + sign * turn;
                float d = depth(limb, body);
                if (d <= SKIN) {
                    float low = before;
                    float high = turn;
                    for (int r = 0; r < REFINE; r++) {
                        float mid = (low + high) * 0.5F;
                        limb.zRot = base + sign * mid;
                        if (depth(limb, body) <= SKIN) {
                            high = mid;
                        } else {
                            low = mid;
                        }
                    }
                    cleared = high;
                } else if (d < leastDepth) {
                    leastDepth = d;
                    leastTurn = sign * turn;
                }
                before = turn;
            }
            if (!Float.isNaN(cleared) && (Float.isNaN(best) || cleared < Math.abs(best))) {
                best = sign * cleared;
            }
        }
        limb.zRot = base + (Float.isNaN(best) ? leastTurn : best);
    }

    // Turns two legs apart, both the same amount each their own way, till they no longer pass into each other.
    static void apart(ModelPart right, ModelPart left) {
        if (!right.visible || !left.visible || overlap(right, left) <= SKIN) {
            return;
        }
        float rightBase = right.zRot;
        float leftBase = left.zRot;
        for (int sign = -1; sign <= 1; sign += 2) {
            for (int k = 1; k <= SCAN; k++) {
                float turn = FURTHEST * 0.5F * k / SCAN;
                right.zRot = rightBase + sign * turn;
                left.zRot = leftBase - sign * turn;
                if (overlap(right, left) <= SKIN) {
                    return;
                }
            }
        }
        right.zRot = rightBase;
        left.zRot = leftBase;
    }

    // How deep the limb's lower part is inside the body's box, in pixels (0 or less when it is out).
    static float depth(ModelPart limb, ModelPart body) {
        float[] lb = bounds(limb);
        float[] bb = bounds(body);
        float radius = Math.min(lb[3] - lb[0], lb[5] - lb[2]) * 0.5F * ROUND;
        float cx = (lb[0] + lb[3]) * 0.5F;
        float cz = (lb[2] + lb[5]) * 0.5F;
        float top = lb[1] + (lb[4] - lb[1]) * FROM;
        float bottom = lb[4] - radius;
        float hx = (bb[3] - bb[0]) * 0.5F;
        float hy = (bb[4] - bb[1]) * 0.5F;
        float hz = (bb[5] - bb[2]) * 0.5F;
        TURN.rotationZYX(limb.zRot, limb.yRot, limb.xRot);
        BODY.rotationZYX(body.zRot, body.yRot, body.xRot).transpose();
        float deepest = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < SAMPLES; i++) {
            float y = top + (bottom - top) * i / (SAMPLES - 1.0F);
            TURN.transform(POINT.set(cx, y, cz));
            POINT.add(limb.x - body.x, limb.y - body.y, limb.z - body.z);
            BODY.transform(POINT);
            float dx = Math.abs(POINT.x - (bb[0] + bb[3]) * 0.5F) - hx;
            float dy = Math.abs(POINT.y - (bb[1] + bb[4]) * 0.5F) - hy;
            float dz = Math.abs(POINT.z - (bb[2] + bb[5]) * 0.5F) - hz;
            float outside = (float) Math.sqrt(Math.max(dx, 0.0F) * Math.max(dx, 0.0F)
                    + Math.max(dy, 0.0F) * Math.max(dy, 0.0F) + Math.max(dz, 0.0F) * Math.max(dz, 0.0F));
            float distance = outside + Math.min(Math.max(dx, Math.max(dy, dz)), 0.0F);
            deepest = Math.max(deepest, radius - distance);
        }
        return deepest;
    }

    // How far two legs' lower parts pass into each other, in pixels.
    static float overlap(ModelPart a, ModelPart b) {
        float ra = axis(a, A0, A1);
        float rb = axis(b, B0, B1);
        double gap = Math.sqrt(Segments.closest(A0, A1, B0, B1, ST));
        return (float) (ra + rb - gap);
    }

    // The limb's lower middle line in model space; returns its radius.
    private static float axis(ModelPart limb, double[] from, double[] to) {
        float[] lb = bounds(limb);
        float radius = Math.min(lb[3] - lb[0], lb[5] - lb[2]) * 0.5F * ROUND;
        float cx = (lb[0] + lb[3]) * 0.5F;
        float cz = (lb[2] + lb[5]) * 0.5F;
        TURN.rotationZYX(limb.zRot, limb.yRot, limb.xRot);
        TURN.transform(POINT.set(cx, lb[1] + (lb[4] - lb[1]) * FROM, cz));
        from[0] = POINT.x + limb.x;
        from[1] = POINT.y + limb.y;
        from[2] = POINT.z + limb.z;
        TURN.transform(POINT.set(cx, lb[4] - radius, cz));
        to[0] = POINT.x + limb.x;
        to[1] = POINT.y + limb.y;
        to[2] = POINT.z + limb.z;
        return radius;
    }

    // A part's own cubes, as one box in its own frame (pixels).
    static float[] bounds(ModelPart part) {
        float[] bounds = BOUNDS.get(part);
        if (bounds == null) {
            bounds = new float[] { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
            for (ModelPart.Cube cube : part.cubes) {
                bounds[0] = Math.min(bounds[0], cube.minX);
                bounds[1] = Math.min(bounds[1], cube.minY);
                bounds[2] = Math.min(bounds[2], cube.minZ);
                bounds[3] = Math.max(bounds[3], cube.maxX);
                bounds[4] = Math.max(bounds[4], cube.maxY);
                bounds[5] = Math.max(bounds[5], cube.maxZ);
            }
            if (bounds[0] > bounds[3]) {
                bounds = new float[6];
            }
            BOUNDS.put(part, bounds);
        }
        return bounds;
    }
}
