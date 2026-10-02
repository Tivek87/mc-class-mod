package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.BoneView;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech as its build poses it (MechBuild) and its legs as the build makes them: each lower leg grown out of the
// light high above, slammed down and stepping off heel and toe, swung from where its hip will be; then the thighs grown
// up to the hips, the knees bending between the hips and the feet as the body's weight moves, as a walking mech's do.
final class MechBuilding {
    private static final double SEAM = 1.0;

    private MechBuilding() {
    }

    static MechPose pose(MechScript.Stage stage, double t) {
        MechBuild.Body body = MechBuild.body(t);
        MechScript.Stage hips = MechBuild.hips(stage, body);
        MechPose pose = new MechPose(stage, hips, MechBuild.torso(hips, body));
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            pose.ankle[side] = stage.point(MechScript.ankle(right, stage, t));
            pose.tip[side] = MechBuild.tip(right, t);
            pose.shrug[side] = body.shrug(right);
        }
        pose.levers(MechBuild.lever(true, t), MechBuild.lever(false, t));
        return pose;
    }

    // Where a leg's knee is: bent between the hip and the ankle once there is a thigh, before that at the top of the
    // lower leg, leaning the way it steps.
    static Vec3 knee(MechPose pose, boolean right, double t) {
        int side = right ? 0 : 1;
        MechScript.Stage stage = pose.stage();
        Vec3 ankle = pose.ankle[side];
        if (t >= MechScript.THIGHS) {
            Vec3 hip = pose.hips().point(MechPainter.side(MechScript.HIP, right));
            return MechLegs.knee(MechLegs.reached(hip, ankle), hip, pose.toes[side]);
        }
        // Standing, its shin would run to where a walking leg's knee is.
        Vec3 stand = MechPainter.side(MechScript.ANKLE, right);
        Vec3 rest = stage.local(MechLegs.knee(stage.point(stand), stage.point(MechPainter.side(MechScript.HIP, right)),
                stage.ahead())).subtract(stand);
        Vec3 shin = stage.dir(rest);
        double lean = MechBuild.lean(right, t);
        if (lean != 0.0) {
            Vec3 way = stage.point(stand).subtract(stage.point(MechScript.ankle(right, stage, MechScript.STEPS[
                    side])));
            way = new Vec3(way.x, 0.0, way.z);
            if (way.lengthSqr() > 1.0E-6) {
                shin = Vectors.spin(shin, way.normalize().cross(Vectors.UP).scale(-1.0), lean);
            }
        }
        return ankle.add(shin);
    }

    static void leg(LanternPainter painter, MechPose pose, boolean right, double t, double apart, int seed) {
        int side = right ? 0 : 1;
        double form = right ? MechScript.FOOT_FORM : MechScript.FOOT2_FORM;
        if (t < form) {
            return;
        }
        boolean thighs = t >= MechScript.THIGHS;
        Vec3 toes = pose.toes[side];
        Vec3 hip = pose.hips().point(MechPainter.side(MechScript.HIP, right));
        Vec3 ankle = thighs ? MechLegs.reached(hip, pose.ankle[side]) : pose.ankle[side];
        Vec3 knee = knee(pose, right, t);
        Vec3 shin = knee.subtract(ankle).normalize();
        Vec3 bend = thighs ? MechLegs.bend(hip, ankle, knee, toes) : toes;
        double grown = MechScript.grown(t, form);
        if (grown < 1.0 && apart < 0.0) {
            // It grows down out of the light, from the open top of the shin to the sole.
            double y = Mth.lerp(Ease.smooth(grown), MechLegShapes.SHIN + 0.1, MechLegShapes.SOLE - 0.05);
            painter.clip(ankle.add(0.0, y, 0.0), Vectors.UP, SEAM);
        }
        Frame foot = Frame.of(ankle, toes, Vectors.UP, 1.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -pose.tip[side]);
        MechParts.draw(painter, right ? MechLegShapes.FOOT : MechLegShapes.FOOT_LEFT, foot, 1.0, apart, seed);
        MechParts.draw(painter, right ? MechLegShapes.SHIN_PART : MechLegShapes.SHIN_LEFT,
                MechPainter.limb(ankle, shin, bend), 1.0, apart, seed + 20);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(knee, ankle, BoneView.CONSTRUCT);
            BoneView.bone(ankle, foot.at(0.0, 0.0, 1.4), BoneView.CONSTRUCT);
        }
        if (!thighs) {
            return;
        }
        Vec3 thigh = hip.subtract(knee).normalize();
        MechPainter.rising(painter, pose.stage(), MechScript.KNEE.y - 0.7, MechScript.HIP.y + 0.6,
                MechScript.grown(t, MechScript.THIGHS), apart);
        MechParts.draw(painter, right ? MechLegShapes.KNEE : MechLegShapes.KNEE_LEFT,
                MechPainter.limb(knee, shin.add(thigh).normalize(), bend), 1.0, apart, seed + 40);
        MechParts.draw(painter, right ? MechLegShapes.THIGH_PART : MechLegShapes.THIGH_LEFT,
                MechPainter.limb(knee, thigh, bend), 1.0, apart, seed + 50);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(hip, knee, BoneView.CONSTRUCT);
        }
    }
}
