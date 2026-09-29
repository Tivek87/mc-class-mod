package nl.tivek.multiversepowers.engine.client.pose;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.rig.Ik;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person's model posed from where its joints go instead of part by part: the hips, the trunk leaning about them and
// bending at the waist with the head and shoulders riding along, and arms and legs reaching their hands and feet to
// points, bent at the elbows and knees (Limbs). Model space, in pixels: y runs down, -z is ahead, +x the model's own
// left, and the feet stand at y 24. Only the world's own drawing of creatures is posed so (not the first-person arm
// or the inventory), on the render thread, so the scratch below is shared.
public final class Stance {
    public static final float GROUND = 24.0F;
    public static final float HIP_Y = 12.0F;
    public static final float HIP_X = 1.9F;
    public static final float SHOULDER_X = 5.0F;
    public static final float SHOULDER_Y = 2.0F;
    // Pivot to knee and knee to sole; shoulder to elbow and elbow to the end of the hand.
    public static final float THIGH = 6.0F;
    public static final float SHIN = 6.0F;
    public static final float UPPER_ARM = 4.0F;
    public static final float FOREARM = 6.0F;
    private static final float HALF_TRUNK = 6.0F;

    private static final Quaternionf ABDOMEN = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Quaternionf SCRATCH = new Quaternionf();
    private static final Vector3f NECK = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f HIPS = new Vector3f(0.0F, HIP_Y, 0.0F);
    private static final Vector3f V = new Vector3f();
    private static final Vector3f T = new Vector3f();
    private static final Vector3f S = new Vector3f();
    private static final Vector3f P = new Vector3f();
    private static final Vector3f H = new Vector3f();
    private static final Vector3f C = new Vector3f();
    private static final Matrix3f M = new Matrix3f();
    private static final double[] ROOT = new double[3];
    private static final double[] END = new double[3];
    private static final double[] POLE = new double[3];
    private static final double[] MID = new double[3];

    private Stance() {
    }

    // The hips at `hips`, the pelvis and belly turned by `lean` and the chest by `waist` on top of that. The head and
    // arms keep their own turn but move to where the neck and shoulders go; the arms turn with the chest as well.
    public static void trunk(HumanoidModel<?> model, Vector3f hips, Quaternionf lean, Quaternionf waist) {
        trunk(model, hips, lean, waist, true);
    }

    // `carry` false: the arms keep the way they point (a hand aiming a shot) while the shoulders move with the chest.
    public static void trunk(HumanoidModel<?> model, Vector3f hips, Quaternionf lean, Quaternionf waist,
            boolean carry) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ABDOMEN.set(lean);
        CHEST.set(lean).mul(waist);
        HIPS.set(hips);
        V.set(0.0F, -HALF_TRUNK, 0.0F);
        ABDOMEN.transform(V);
        NECK.set(hips).add(V);
        V.set(0.0F, -HALF_TRUNK, 0.0F);
        CHEST.transform(V);
        NECK.add(V);
        place(model.body, NECK, CHEST);
        Limbs.waist(model, SCRATCH.set(waist).conjugate());
        model.head.setPos(NECK.x, NECK.y, NECK.z);
        shoulder(model.rightArm, -1.0F, carry);
        shoulder(model.leftArm, 1.0F, carry);
        for (int side = -1; side <= 1; side += 2) {
            ModelPart leg = side < 0 ? model.rightLeg : model.leftLeg;
            V.set(side * HIP_X, 0.0F, 0.0F);
            ABDOMEN.transform(V);
            leg.setPos(hips.x + V.x, hips.y + V.y, hips.z + V.z);
        }
        followers(model);
    }

    // Where the neck and the chest's turn are after trunk().
    public static Vector3f neck(Vector3f out) {
        return out.set(NECK);
    }

    public static Quaternionf chest(Quaternionf out) {
        return out.set(CHEST);
    }

    private static void shoulder(ModelPart arm, float side, boolean carry) {
        V.set(side * SHOULDER_X, SHOULDER_Y, 0.0F);
        CHEST.transform(V);
        arm.setPos(NECK.x + V.x, NECK.y + V.y, NECK.z + V.z);
        if (!carry) {
            return;
        }
        SCRATCH.rotationZYX(arm.zRot, arm.yRot, arm.xRot);
        SCRATCH.premul(CHEST);
        turn(arm, SCRATCH);
    }

    // Reaches a leg's sole to `foot`, its knee bent out towards `pole` (a direction: ahead for a knee).
    public static void leg(HumanoidModel<?> model, boolean right, Vector3f foot, Vector3f pole) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        reach(model, leg, foot, pole, THIGH, SHIN, false,
                right ? Limbs.Joint.RIGHT_KNEE : Limbs.Joint.LEFT_KNEE);
        followers(model);
    }

    // Reaches an arm's hand to `hand`, its elbow bent out towards `pole` (a direction: back and out for an elbow).
    public static void arm(HumanoidModel<?> model, boolean right, Vector3f hand, Vector3f pole) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ModelPart arm = right ? model.rightArm : model.leftArm;
        reach(model, arm, hand, pole, UPPER_ARM, FOREARM, true,
                right ? Limbs.Joint.RIGHT_ELBOW : Limbs.Joint.LEFT_ELBOW);
        followers(model);
    }

    // Where a limb's far end is as it is posed now, straight.
    public static Vector3f end(ModelPart limb, float length, Vector3f out) {
        SCRATCH.rotationZYX(limb.zRot, limb.yRot, limb.xRot);
        out.set(0.0F, length, 0.0F);
        SCRATCH.transform(out);
        return out.add(limb.x, limb.y, limb.z);
    }

    public static Vector3f foot(HumanoidModel<?> model, boolean right, Vector3f out) {
        return end(right ? model.rightLeg : model.leftLeg, THIGH + SHIN, out);
    }

    public static Vector3f hand(HumanoidModel<?> model, boolean right, Vector3f out) {
        return end(right ? model.rightArm : model.leftArm, UPPER_ARM + FOREARM, out);
    }

    private static void reach(HumanoidModel<?> model, ModelPart limb, Vector3f end, Vector3f pole, float upper,
            float lower, boolean arm, Limbs.Joint joint) {
        ROOT[0] = limb.x;
        ROOT[1] = limb.y;
        ROOT[2] = limb.z;
        END[0] = end.x;
        END[1] = end.y;
        END[2] = end.z;
        POLE[0] = pole.x;
        POLE[1] = pole.y;
        POLE[2] = pole.z;
        Ik.twoBone(ROOT, END, POLE, upper, lower, MID);
        T.set((float) (MID[0] - ROOT[0]), (float) (MID[1] - ROOT[1]), (float) (MID[2] - ROOT[2])).normalize();
        S.set((float) (END[0] - MID[0]), (float) (END[1] - MID[1]), (float) (END[2] - MID[2]));
        if (S.lengthSquared() < 1.0E-8F) {
            S.set(T);
        }
        S.normalize();
        float fold = (float) Math.acos(Mth.clamp(S.dot(T), -1.0F, 1.0F));
        // The way the far half folds, square to the near half: away from the pole when the limb is straight.
        P.set(S).sub(T.x * S.dot(T), T.y * S.dot(T), T.z * S.dot(T));
        if (P.lengthSquared() < 1.0E-6F) {
            P.set(pole).sub(T.x * pole.dot(T), T.y * pole.dot(T), T.z * pole.dot(T)).negate();
        }
        if (P.lengthSquared() < 1.0E-8F) {
            P.set(0.0F, 0.0F, arm ? -1.0F : 1.0F);
        }
        P.normalize();
        // A knee folds about +x (its far half going back), an elbow about -x (going forward).
        T.cross(P, H);
        if (arm) {
            H.negate();
        }
        H.normalize();
        H.cross(T, C);
        M.set(H.x, H.y, H.z, T.x, T.y, T.z, C.x, C.y, C.z);
        M.getNormalizedRotation(SCRATCH);
        turn(limb, SCRATCH);
        Limbs.bend(model, joint, fold);
    }

    private static void place(ModelPart part, Vector3f at, Quaternionf turn) {
        part.setPos(at.x, at.y, at.z);
        turn(part, turn);
    }

    private static void turn(ModelPart part, Quaternionf turn) {
        turn.get(M);
        M.getEulerAnglesZYX(V);
        part.setRotation(V.x, V.y, V.z);
    }

    private static void followers(HumanoidModel<?> model) {
        model.hat.copyFrom(model.head);
        if (model instanceof PlayerModel<?> player) {
            player.jacket.copyFrom(player.body);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }
}
