package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class MechPainter {
    private static final double GLOWS = 0.24;
    private static final double SEAM = 1.0;
    private static final double FLING = 2.0;
    private static final double REACH = 16.0;
    private static final int PIECES = 40;
    private static final double[] BENDS = { 0.95, 1.15, 0.9 };
    private static final double STOMP_SHAKE = 0.55;
    private static final double STEP_SHAKE = 0.28;
    private static final double CLAP_SHAKE = 0.7;
    private static final double LOCK_SHAKE = 0.3;
    private static final double DRILL_SHAKE = 0.9;
    private static final double SHAKE_RANGE = 40.0;

    private MechPainter() {
    }

    public static void draw(LanternPainter painter, ConstructPayload was, ConstructPayload now, double clock,
            float partialTick, @Nullable Vec3 ring, boolean own) {
        MechScript.Stage stage = MechScript.Stage.of(now.center(), now.facing());
        boolean breaking = now.variant() == MechScript.BREAKING;
        double t = breaking ? now.age() : clock;
        double apart = breaking ? Mth.clamp(Mth.lerp(partialTick, Math.max(0.0F, was.charge()), now.charge())
                / MechScript.BREAK_TICKS, 0.0, 1.0) : -1.0;
        if (!painter.visible(stage.point(0.0, 6.0, 0.0), REACH)) {
            return;
        }
        parts(painter, stage, t, apart, own);
        if (apart < 0.0) {
            MechLight.lights(painter, stage, t, ring, own);
        }
    }

    public static boolean breaks(ConstructPayload mech, double clock) {
        return mech.variant() == MechScript.BUILDING && clock > MechScript.BOOT_FORM;
    }

    public static void broken(LanternPainter painter, ConstructPayload mech, double clock, double since) {
        double apart = since / MechScript.BREAK_TICKS;
        if (apart >= 1.0) {
            return;
        }
        parts(painter, MechScript.Stage.of(mech.center(), mech.facing()), clock, apart, false);
    }

    private static void parts(LanternPainter painter, MechScript.Stage stage, double t, double apart, boolean own) {
        painter.ambient(GLOWS);
        painter.fling(FLING);
        int seed = 0;
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            seed += PIECES * 8;
            boot(painter, stage, right, t, apart, seed);
            leg(painter, stage, right, t, apart, seed + PIECES);
            arm(painter, stage, right, t, apart, seed + PIECES * 3);
            if (t >= MechScript.SHOULDERS) {
                shoulder(painter, stage, right, t, apart, seed + PIECES * 5);
            }
        }
        Frame body = Frame.of(stage.base(), stage.ahead(), Vectors.UP, 1.0);
        if (t >= MechScript.HIPS) {
            double grown = Mth.clamp((t - MechScript.HIPS) / (MechScript.HIPS_LOCK - MechScript.HIPS), 0.0, 1.0);
            rising(painter, stage.point(0.0, 4.55 + grown * 1.45, 0.0), grown < 1.0 && apart < 0.0);
            HandPainter.part(painter, MechShapes.PELVIS, body, 1.0, apart, PIECES * 20);
            painter.noClip();
        }
        if (t >= MechScript.TORSO) {
            double grown = Mth.clamp((t - MechScript.TORSO) / MechScript.TORSO_TICKS, 0.0, 1.0);
            rising(painter, stage.point(0.0, 5.4 + grown * 3.7, 0.0), grown < 1.0 && apart < 0.0);
            HandPainter.part(painter, MechShapes.TORSO, body, 1.0, apart, PIECES * 21);
            HandPainter.part(painter, MechShapes.COCKPIT, body, 1.0, apart, PIECES * 23);
            painter.noClip();
        }
        if (t >= MechScript.GLASS && apart < 0.0) {
            double grown = Ease.backOut((t - MechScript.GLASS) / 6.0);
            Frame glass = body.moved(MechShapes.GLASS_AT.x, MechShapes.GLASS_AT.y, MechShapes.GLASS_AT.z)
                    .stretched(Math.max(0.01, grown), Math.max(0.01, grown), 1.0);
            painter.seeThrough(MechShapes.GLASS, glass, own ? 0.07 : 0.14, 1.3);
        }
        if (t >= MechScript.HEAD_FORM) {
            head(painter, stage, t, apart);
        }
        painter.fling(1.0);
        painter.ambient(0.0);
    }

    // Grows up out of the light: only what lies below the rising plane shows, cut with a bright seam.
    private static void rising(LanternPainter painter, Vec3 at, boolean growing) {
        if (growing) {
            painter.clip(at, new Vec3(0.0, -1.0, 0.0), SEAM);
        }
    }

    private static void boot(LanternPainter painter, MechScript.Stage stage, boolean right, double t, double apart,
            int seed) {
        MechScript.Foot foot = MechScript.foot(right, t);
        if (foot.grown() <= 0.0) {
            return;
        }
        Vec3 at = stage.point(foot.at());
        Frame frame = Frame.of(at, stage.ahead(), Vectors.UP, MechScript.FOOT_SCALE).turned(0.0, 0.0, 0.0, 0.0, 1.0,
                0.0, foot.yaw());
        rising(painter, at.add(0.0, foot.grown() * 1.9, 0.0), foot.grown() < 1.0 && apart < 0.0);
        HandPainter.part(painter, right ? MechShapes.BOOT : MechShapes.BOOT_LEFT, frame, 1.0, apart, seed);
        painter.noClip();
    }

    private static void leg(LanternPainter painter, MechScript.Stage stage, boolean right, double t, double apart,
            int seed) {
        int i = right ? 0 : 1;
        Vec3 ankle = stage.point(side(MechScript.ANKLE, right));
        Vec3 knee = stage.point(side(MechScript.KNEE, right));
        Vec3 hip = stage.point(side(MechScript.HIP, right));
        if (t >= MechScript.SHINS[i]) {
            double grown = MechScript.grown(t, MechScript.SHINS[i]);
            Frame shin = limb(ankle, knee, stage.ahead(), MechScript.LEG_WIDTH);
            growing(painter, shin, grown * MechShapes.SHIN, grown < 1.0 && apart < 0.0);
            HandPainter.part(painter, MechShapes.SHIN_PART, shin, 1.0, apart, seed);
            painter.noClip();
        }
        double kneeIn = t - MechScript.KNEES[i];
        if (kneeIn >= -3.0) {
            double slide = 0.9 * (1.0 - Ease.backOut(Mth.clamp((kneeIn + 3.0) / 3.0, 0.0, 1.0)));
            Frame cap = Frame.of(knee, stage.ahead(), Vectors.UP, MechScript.LEG_WIDTH).moved(0.0, 0.0, slide);
            HandPainter.part(painter, MechShapes.KNEE_PART, cap, 1.0, apart, seed + 10);
        }
        if (t >= MechScript.THIGHS[i]) {
            double grown = MechScript.grown(t, MechScript.THIGHS[i]);
            Frame thigh = limb(knee, hip, stage.ahead(), MechScript.LEG_WIDTH);
            growing(painter, thigh, grown * MechShapes.THIGH, grown < 1.0 && apart < 0.0);
            HandPainter.part(painter, MechShapes.THIGH_PART, thigh, 1.0, apart, seed + 20);
            painter.noClip();
        }
    }

    private static void growing(LanternPainter painter, Frame limb, double reached, boolean growing) {
        if (growing) {
            painter.clip(limb.at(0.0, reached, 0.0), limb.up().scale(-1.0), SEAM);
        }
    }

    static Vec3 side(Vec3 local, boolean right) {
        return right ? local : new Vec3(-local.x, local.y, local.z);
    }

    static Frame limb(Vec3 from, Vec3 to, Vec3 facing, double width) {
        Vec3 up = to.subtract(from).normalize();
        Vec3 ahead = facing.subtract(up.scale(facing.dot(up)));
        ahead = ahead.lengthSqr() < 1.0E-8 ? Vectors.across(up)[0] : ahead.normalize();
        return new Frame(from, ahead.cross(up).normalize().scale(width), up, ahead.scale(width), 1.0);
    }

    static Frame armFrame(MechScript.Stage stage, MechScript.Arm arm) {
        return Frame.of(stage.point(arm.elbow()), stage.dir(arm.palm()), stage.dir(arm.way()), MechScript.ARM_SCALE);
    }

    private static void arm(LanternPainter painter, MechScript.Stage stage, boolean right, double t, double apart,
            int seed) {
        MechScript.Arm arm = MechScript.arm(right, t);
        if (arm.grown() <= 0.0) {
            return;
        }
        Frame frame = armFrame(stage, arm);
        growing(painter, frame, arm.grown() * 3.8, arm.grown() < 1.0 && apart < 0.0);
        HandPainter.part(painter, right ? MechShapes.FOREARM : MechShapes.FOREARM_LEFT, frame, 1.0, apart, seed);
        for (int k = 0; k < 4; k++) {
            Frame joint = frame.moved(MechShapes.FINGER_X[k], MechShapes.FINGER_ROOT, 0.0);
            for (int j = 0; j < 3; j++) {
                double length = MechShapes.FINGER_LENGTHS[k][j];
                joint = joint.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, arm.curl() * BENDS[j]);
                Frame drawn = joint.stretched(1.0, length / MechShapes.FINGER_LENGTHS[0][j], 1.0);
                HandPainter.part(painter, MechShapes.FINGER[j], drawn, 1.0, apart, seed + 10 + k * 3 + j);
                joint = joint.moved(0.0, length, 0.0);
            }
        }
        Frame thumb = frame.moved(MechShapes.THUMB_ROOT.x, MechShapes.THUMB_ROOT.y, MechShapes.THUMB_ROOT.z)
                .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -0.95);
        for (int j = 0; j < 2; j++) {
            thumb = thumb.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.3 + arm.curl() * 0.8);
            HandPainter.part(painter, MechShapes.THUMB[j], thumb, 1.0, apart, seed + 30 + j);
            thumb = thumb.moved(0.0, MechShapes.THUMB_LENGTHS[j], 0.0);
        }
        painter.noClip();
    }

    private static void shoulder(LanternPainter painter, MechScript.Stage stage, boolean right, double t,
            double apart, int seed) {
        double grown = Mth.clamp((t - MechScript.SHOULDERS) / 10.0, 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            Vec3 out = stage.right().scale(right ? 1.0 : -1.0);
            painter.clip(stage.point(right ? 1.6 + grown * 2.0 : -1.6 - grown * 2.0, 0.0, 0.0), out.scale(-1.0), SEAM);
        }
        Frame body = Frame.of(stage.base(), stage.ahead(), Vectors.UP, 1.0);
        HandPainter.part(painter, right ? MechShapes.SHOULDER : MechShapes.SHOULDER_LEFT, body, 1.0, apart, seed);
        Frame upper = limb(stage.point(side(MechScript.ELBOW, right)), stage.point(side(MechScript.SHOULDER, right)),
                stage.ahead(), 1.25);
        HandPainter.part(painter, right ? MechShapes.UPPER : MechShapes.UPPER_LEFT, upper, 1.0, apart, seed + 10);
        painter.noClip();
    }

    private static void head(LanternPainter painter, MechScript.Stage stage, double t, double apart) {
        Vec3 at = stage.point(MechScript.head(t));
        Frame frame = Frame.of(at, stage.ahead(), Vectors.UP, 1.0).turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                MechScript.headSpin(t));
        double grown = Mth.clamp((t - MechScript.HEAD_FORM) / (MechScript.HEAD_AHEAD - MechScript.HEAD_FORM), 0.0,
                1.0);
        rising(painter, at.add(0.0, -1.2 + grown * 2.1, 0.0), grown < 1.0 && apart < 0.0);
        HandPainter.part(painter, MechShapes.HEAD, frame, 1.0, apart, PIECES * 30);
        painter.noClip();
    }

    public static float shake(ConstructPayload mech, double clock, Vec3 from) {
        if (mech.variant() == MechScript.BREAKING) {
            return 0.0F;
        }
        MechScript.Stage stage = MechScript.Stage.of(mech.center(), mech.facing());
        double near = 1.0 - from.distanceTo(stage.point(0.0, 3.0, 1.5)) / SHAKE_RANGE;
        if (near <= 0.0) {
            return 0.0F;
        }
        double most = 0.0;
        for (int side = 0; side < 2; side++) {
            double landed = MechScript.landed(side == 0, clock);
            boolean stomp = clock < MechScript.STEPS[side] - MechScript.STEP_TICKS;
            most = Math.max(most, jolt(landed, stomp ? 9.0 : 6.0, stomp ? STOMP_SHAKE : STEP_SHAKE));
        }
        most = Math.max(most, jolt(clock - MechScript.CLAP, 9.0, CLAP_SHAKE));
        most = Math.max(most, jolt(clock - MechScript.ARMS_LOCK, 6.0, LOCK_SHAKE));
        most = Math.max(most, jolt(clock - MechScript.IMPACT, 12.0, DRILL_SHAKE));
        if (clock > MechScript.IMPACT && clock < MechScript.GRIND) {
            most = Math.max(most, 0.35);
        }
        most = Math.max(most, jolt(clock - MechScript.CLACK, 7.0, LOCK_SHAKE * 1.4));
        return (float) Math.min(1.0, most * Math.min(1.0, near * 1.3));
    }

    private static double jolt(double since, double ticks, double hard) {
        if (since < 0.0 || since >= ticks) {
            return 0.0;
        }
        double fade = 1.0 - since / ticks;
        return hard * fade * fade;
    }
}
