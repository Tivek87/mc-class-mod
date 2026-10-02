package nl.tivek.multiversepowers.engine.client.pose;

import javax.annotation.Nullable;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.rig.Ik;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A person's model posed from where its joints go instead of part by part: the hips, the trunk leaning about them and
// bending at the low back and the waist with the head and shoulders riding along, and arms and legs reaching their
// hands and feet to points, bent at the elbows and knees, the feet and hands turned at the ankles and wrists and the
// shoulder blades following the arms (Limbs). Model space, in pixels: y runs down, -z is ahead, +x the model's own
// left, and the feet stand at y 24. Only the world's own drawing of creatures is posed so (not the first-person arm
// or the inventory), on the render thread, so the scratch below is shared.
public final class Stance {
    public static final float GROUND = 24.0F;
    public static final float HIP_Y = 12.0F;
    public static final float HIP_X = 1.9F;
    public static final float SHOULDER_X = 5.0F;
    public static final float SHOULDER_Y = 2.0F;
    // Pivot to knee and knee to sole; shoulder to elbow and elbow to the end of the hand; of those, the foot past the
    // ankle and the hand past the wrist (ModelBends.end).
    public static final float THIGH = 6.0F;
    public static final float SHIN = 6.0F;
    public static final float UPPER_ARM = 4.0F;
    public static final float FOREARM = 6.0F;
    public static final float FOOT = 2.0F;
    public static final float HAND = 3.0F;
    // The trunk from the neck down: the chest to the waist, the belly to the pelvis, the pelvis to the hips
    // (ModelBends.waist and pelvis).
    private static final float CHEST_LENGTH = 6.0F;
    private static final float BELLY = 3.0F;
    private static final float PELVIS_LENGTH = 3.0F;
    // How much of the lean the pelvis takes; the low back bends the rest.
    private static final float HIPS_SHARE = 0.6F;
    // How far an ankle turns a foot and a wrist a hand from in line with the limb, at most.
    public static final float ANKLE_MOST = 1.0F;
    private static final float WRIST_MOST = 1.3F;

    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final Quaternionf PELVIS = new Quaternionf();
    private static final Quaternionf ABDOMEN = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Quaternionf SCRATCH = new Quaternionf();
    private static final Quaternionf BLADE = new Quaternionf();
    private static final Quaternionf OWN = new Quaternionf();
    private static final Quaternionf HINGE = new Quaternionf();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf LAST = new Quaternionf();
    private static final Quaternionf SHARE = new Quaternionf();
    private static final Quaternionf FOOT_WAY = new Quaternionf();
    private static final Vector3f J = new Vector3f();
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

    // The hips at `hips`, the belly turned by `lean` and the chest by `waist` on top of that; the pelvis, which the legs
    // hang from, takes HIPS_SHARE of the lean and the low back bends the rest. The head and arms keep their own turn
    // but move to where the neck and shoulders go; the arms turn with the chest as well.
    public static void trunk(HumanoidModel<?> model, Vector3f hips, Quaternionf lean, Quaternionf waist) {
        trunk(model, hips, lean, waist, true);
    }

    // `carry` false: the arms keep the way they point (a hand aiming a shot) while the shoulders move with the chest.
    public static void trunk(HumanoidModel<?> model, Vector3f hips, Quaternionf lean, Quaternionf waist,
            boolean carry) {
        if (!EntityPass.inWorld()) {
            return;
        }
        PELVIS.identity().slerp(lean, HIPS_SHARE);
        ABDOMEN.set(lean);
        CHEST.set(lean).mul(waist);
        HIPS.set(hips);
        NECK.set(hips).add(PELVIS.transform(V.set(0.0F, -PELVIS_LENGTH, 0.0F)));
        NECK.add(ABDOMEN.transform(V.set(0.0F, -BELLY, 0.0F)));
        NECK.add(CHEST.transform(V.set(0.0F, -CHEST_LENGTH, 0.0F)));
        place(model.body, NECK, CHEST);
        Limbs.waist(model, SCRATCH.set(waist).conjugate());
        Limbs.pelvis(model, SCRATCH.set(lean).conjugate().mul(PELVIS));
        model.head.setPos(NECK.x, NECK.y, NECK.z);
        shoulder(model.rightArm, -1.0F, carry);
        shoulder(model.leftArm, 1.0F, carry);
        for (int side = -1; side <= 1; side += 2) {
            ModelPart leg = side < 0 ? model.rightLeg : model.leftLeg;
            PELVIS.transform(V.set(side * HIP_X, 0.0F, 0.0F));
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

    // Reaches a leg's sole to `foot`, its knee bent out towards `pole` (a direction: ahead for a knee). The foot keeps
    // the way it pointed (as the game or a pose turned the leg), as far as its ankle turns: a sole that stood flat
    // stays flat as the knee bends.
    public static void leg(HumanoidModel<?> model, boolean right, Vector3f foot, Vector3f pole) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        // The way the foot points now: its leg's turn, and its knee's and ankle's when a pose bent them already.
        FOOT_WAY.rotationZYX(leg.zRot, leg.yRot, leg.xRot);
        for (int joint = 0; joint < 2; joint++) {
            Matrix3f turn = BentParts.turn(leg, joint);
            if (turn != null) {
                FOOT_WAY.mul(turn.getNormalizedRotation(SCRATCH));
            }
        }
        reach(model, leg, foot, pole, THIGH, SHIN - FOOT, FOOT, FOOT_WAY, 1.0F, ANKLE_MOST, false,
                right ? Limbs.Joint.RIGHT_KNEE : Limbs.Joint.LEFT_KNEE,
                right ? Limbs.Joint.RIGHT_ANKLE : Limbs.Joint.LEFT_ANKLE);
        followers(model);
    }

    // For a pose that turned a person's leg and bent its knee itself: the foot turns at the ankle towards standing flat
    // on the model's ground, heading as the leg does, `weight` of the way and as far as the ankle turns.
    public static void flat(HumanoidModel<?> model, boolean right, float weight) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ModelPart leg = right ? model.rightLeg : model.leftLeg;
        lower(leg, Limbs.bent(model, right ? Limbs.Joint.RIGHT_KNEE : Limbs.Joint.LEFT_KNEE), false, OWN);
        TURN.set(OWN).conjugate().mul(LAST.rotationY(leg.yRot));
        share(TURN, weight);
        float angle = angle(TURN);
        if (angle > ANKLE_MOST) {
            share(TURN, ANKLE_MOST / angle);
        }
        Limbs.turn(model, right ? Limbs.Joint.RIGHT_ANKLE : Limbs.Joint.LEFT_ANKLE, TURN);
    }

    // Reaches an arm's hand to `hand`, its elbow bent out towards `pole` (a direction: back and out for an elbow), the
    // hand in line with the forearm and the shoulder blade following the arm (Shoulders).
    public static void arm(HumanoidModel<?> model, boolean right, Vector3f hand, Vector3f pole) {
        arm(model, right, hand, pole, null, 0.0F);
    }

    // As above, the hand turned `weight` of the way to `palm` (its turn in model space, as an arm's own: y along the
    // fingers, its inner side +x for the right hand and -x for the left), as far as its wrist turns.
    public static void arm(HumanoidModel<?> model, boolean right, Vector3f hand, Vector3f pole,
            @Nullable Quaternionf palm, float weight) {
        if (!EntityPass.inWorld()) {
            return;
        }
        ModelPart arm = right ? model.rightArm : model.leftArm;
        blade(model, arm, right, hand);
        reach(model, arm, hand, pole, UPPER_ARM, FOREARM - HAND, HAND, palm, weight, WRIST_MOST, true,
                right ? Limbs.Joint.RIGHT_ELBOW : Limbs.Joint.LEFT_ELBOW,
                right ? Limbs.Joint.RIGHT_WRIST : Limbs.Joint.LEFT_WRIST);
        followers(model);
    }

    // The shoulder blade turned for the way the hand goes, once a drawing; the arm then reaches from where the shoulder
    // went.
    private static void blade(HumanoidModel<?> model, ModelPart arm, boolean right, Vector3f hand) {
        if (Limbs.shrugged(model, right)) {
            return;
        }
        V.set(hand).sub(arm.x, arm.y, arm.z);
        float length = V.length();
        if (length < 1.0E-3F) {
            return;
        }
        SCRATCH.rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot).transformInverse(V.div(length));
        Limbs.shoulder(model, right, Shoulders.turn(right, V, length / (UPPER_ARM + FOREARM), BLADE));
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

    // Reaches a limb's far end to `end`, bent at its middle joint towards `pole`, its last piece (`last` long, past its
    // end joint) turned `weight` of the way to `way` (model space) as far as `most` from in line with the piece before
    // it, or in line when `way` is null.
    private static void reach(HumanoidModel<?> model, ModelPart limb, Vector3f end, Vector3f pole, float upper,
            float lower, float last, @Nullable Quaternionf way, float weight, float most, boolean arm,
            Limbs.Joint middle, Limbs.Joint tip) {
        float fold = solve(limb, end, pole, upper, lower + last, arm);
        if (way == null || weight <= 0.0F) {
            Limbs.bend(model, middle, fold);
            Limbs.turn(model, tip, IDENTITY);
            return;
        }
        // How the last piece would lie in line, the turn it takes from there and where its joint goes for it.
        lower(limb, fold, arm, OWN);
        TURN.set(OWN).conjugate().mul(way);
        share(TURN, weight);
        float angle = angle(TURN);
        if (angle > most) {
            share(TURN, most / angle);
        }
        // The middle joint folds only so far (a deep crouch) and the limb reaches only so far (stretched out): the last
        // piece gives up as much of its turn as it takes for the limb to reach its end joint.
        double shut = Limbs.most(model, middle);
        float least = (float) Math.sqrt(upper * upper + lower * lower + 2.0 * upper * lower * Math.cos(shut));
        if (!fits(limb, end, last, 1.0F, least, upper + lower)) {
            float low = 0.0F;
            float high = 1.0F;
            boolean inLine = fits(limb, end, last, 0.0F, least, upper + lower);
            for (int i = 0; i < 10 && inLine; i++) {
                float mid = (low + high) * 0.5F;
                if (fits(limb, end, last, mid, least, upper + lower)) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            share(TURN, low);
        }
        joint(end, last, 1.0F);
        Limbs.bend(model, middle, solve(limb, J, pole, upper, lower, arm));
        lower(limb, Limbs.bent(model, middle), arm, OWN);
        Limbs.turn(model, tip, TURN.set(OWN).conjugate().mul(LAST));
    }

    // Whether the limb reaches its end joint, with the last piece turned `share` of TURN, folding no further than its
    // middle joint can and no straighter than straight.
    private static boolean fits(ModelPart limb, Vector3f end, float last, float share, float least, float most) {
        float distance = joint(end, last, share).distance(limb.x, limb.y, limb.z);
        return distance >= least && distance <= most;
    }

    // Where the end joint goes (J) for the last piece to end at `end`, turned `share` of TURN from in line (OWN); the
    // last piece's turn left in LAST.
    private static Vector3f joint(Vector3f end, float last, float share) {
        LAST.set(OWN).mul(SHARE.identity().slerp(TURN, share));
        return LAST.transform(J.set(0.0F, -last, 0.0F)).add(end);
    }

    // The turn in model space of the piece past a limb's middle joint, folded `fold` about its hinge (a knee about +x,
    // an elbow about -x).
    private static Quaternionf lower(ModelPart limb, float fold, boolean arm, Quaternionf out) {
        return out.rotationZYX(limb.zRot, limb.yRot, limb.xRot)
                .mul(HINGE.setAngleAxis(fold, arm ? -1.0F : 1.0F, 0.0F, 0.0F));
    }

    // `share` of the turn, from none.
    private static void share(Quaternionf turn, float share) {
        turn.set(SHARE.identity().slerp(turn, share));
    }

    private static float angle(Quaternionf turn) {
        return 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(turn.w)));
    }

    // Turns the limb about its pivot so its far end, `upper` past the pivot and `lower` past the middle joint, reaches
    // `end` bent towards `pole`; how far the middle joint folds.
    private static float solve(ModelPart limb, Vector3f end, Vector3f pole, float upper, float lower, boolean arm) {
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
        return fold;
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
