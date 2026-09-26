package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// The body of a construct that moves the whole player (the flamethrower, the whip): the torso bends, turns and tips
// at the hips, the knees bend (drawn by KneelLegs) and the whole model sinks until the lowest foot or knee is on the
// ground. Model space: pixels, y down, -z ahead, +x to the player's left. Body space: blocks, x right, y up from the
// shoulders, z ahead.
final class FlameBody {
    static final float BEND = 0.5F;
    private static final double THIGH = 6.0;
    private static final double SHIN = 6.0;
    private static final double LEG = THIGH + SHIN;
    private static final double HIP_ACROSS = 1.9;
    private static final double SQUAT_THIGH = 1.05;
    private static final double SPREAD = 0.35;
    private static final Vec3 HIP = new Vec3(0.0, 12.0, 0.0);
    private static final Vec3 FIST = new Vec3(-1.0, 9.4, 0.0);
    private static final Vec3 LEFT_FIST = new Vec3(1.0, 9.4, 0.0);

    record Torso(float bend, float twist, float roll) {
        Vec3 turn(Vec3 v) {
            return FlameBody.turn(v, this.bend, this.twist, -this.roll);
        }

        Vec3 neck() {
            return HIP.add(this.turn(new Vec3(0.0, -12.0, 0.0)));
        }

        Vec3 shoulder(boolean right) {
            return HIP.add(this.turn(new Vec3(right ? -5.0 : 5.0, -10.0, 0.0)));
        }
    }

    // Thighs (xRot, forward when negative), knees (the shin bends back) and the spread (zRot, outward), right then
    // left; drop is how far the whole model sinks, in pixels.
    record Legs(float rightThigh, float rightKnee, float leftThigh, float leftKnee, float spread, float drop) {
    }

    private FlameBody() {
    }

    static Torso torso(FlameCurves.Pose pose, float ours) {
        return torso(pose.lean(), pose.twist(), pose.roll(), ours);
    }

    static Torso torso(float lean, float twist, float roll, float ours) {
        return new Torso(BEND * Mth.clamp(lean, -0.35F, 1.0F) * ours, twist * ours,
                Mth.clamp(roll, -0.6F, 0.6F) * ours);
    }

    static Legs legs(FlameCurves.Pose pose, float ours) {
        return legs(pose.squat(), pose.step(), pose.kneel(), pose.wide(), pose.hop(), ours);
    }

    static Legs legs(float squatting, float stepping, float kneeling, float wide, float hop, float ours) {
        double squat = Mth.clamp(squatting, 0.0F, 1.0F) * SQUAT_THIGH;
        double step = Mth.clamp(stepping, -1.0F, 1.0F);
        double front = Math.max(0.0, step);
        double back = Math.max(0.0, -step);
        double rightThigh = -squat + 0.42 * front - 0.52 * back;
        double rightKnee = 2.0 * squat + 0.12 * front + 0.28 * back;
        double leftThigh = -squat - 0.52 * front + 0.42 * back;
        double leftKnee = 2.0 * squat + 0.28 * front + 0.12 * back;
        double kneel = Mth.clamp(kneeling, 0.0F, 1.0F);
        rightThigh = Mth.lerp(kneel, rightThigh, -0.05);
        rightKnee = Mth.lerp(kneel, rightKnee, 1.62);
        leftThigh = Mth.lerp(kneel, leftThigh, -1.55);
        leftKnee = Mth.lerp(kneel, leftKnee, 1.55);
        double spread = SPREAD * Mth.clamp(wide, 0.0F, 1.0F);
        float rt = (float) (rightThigh * ours);
        float rk = (float) (rightKnee * ours);
        float lt = (float) (leftThigh * ours);
        float lk = (float) (leftKnee * ours);
        float sp = (float) (spread * ours);
        double reach = Math.max(low(rt, rk), low(lt, lk)) * Math.cos(sp);
        return new Legs(rt, rk, lt, lk, sp, (float) (LEG - reach - Math.max(0.0F, hop) * 16.0 * ours));
    }

    // How far below the hip a leg reaches: its knee or its foot, whichever is lower.
    private static double low(double thigh, double knee) {
        double kneeAt = THIGH * Math.cos(thigh);
        return Math.max(kneeAt, kneeAt + SHIN * Math.cos(thigh + knee));
    }

    static float[] knees(Legs legs) {
        return new float[] { legs.rightKnee(), legs.leftKnee() };
    }

    static boolean bent(Legs legs) {
        return legs.rightKnee() > 0.02F || legs.leftKnee() > 0.02F;
    }

    // The arm's xRot and yRot that point it from its shoulder at the target, all in model space.
    static float[] reach(Vec3 shoulder, Vec3 target, float otherwise) {
        Vec3 to = target.subtract(shoulder);
        double length = to.length();
        if (length < 1.0E-4) {
            return new float[] { 0.0F, otherwise };
        }
        Vec3 d = to.scale(1.0 / length);
        float xRot = (float) -Math.acos(Mth.clamp(d.y, -1.0, 1.0));
        float yRot = Math.abs(Math.sin(xRot)) < 1.0E-3 ? otherwise : (float) Mth.atan2(-d.x, -d.z);
        return new float[] { xRot, yRot };
    }

    static Vec3 fist(Vec3 shoulder, float[] rot, boolean right) {
        return shoulder.add(turn(right ? FIST : LEFT_FIST, rot[0], rot[1], 0.0));
    }

    static Vec3 model(Vec3 body) {
        return new Vec3(-16.0 * body.x, 2.0 - 16.0 * body.y, -16.0 * body.z);
    }

    static Vec3 body(Vec3 model) {
        return new Vec3(-model.x / 16.0, (2.0 - model.y) / 16.0, -model.z / 16.0);
    }

    static Vec3 hip(boolean right) {
        return new Vec3(right ? -HIP_ACROSS : HIP_ACROSS, HIP.y, 0.0);
    }

    // x first, then y, then z, as a ModelPart turns.
    static Vec3 turn(Vec3 v, double xRot, double yRot, double zRot) {
        double cx = Math.cos(xRot);
        double sx = Math.sin(xRot);
        double y1 = v.y * cx - v.z * sx;
        double z1 = v.y * sx + v.z * cx;
        double cy = Math.cos(yRot);
        double sy = Math.sin(yRot);
        double x2 = v.x * cy + z1 * sy;
        double z2 = -v.x * sy + z1 * cy;
        double cz = Math.cos(zRot);
        double sz = Math.sin(zRot);
        return new Vec3(x2 * cz - y1 * sz, x2 * sz + y1 * cz, z2);
    }
}
