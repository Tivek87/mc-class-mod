package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.BoneView;
import nl.tivek.multiversepowers.engine.math.Vectors;

// A walking mech's legs: each foot where its walk put it, the knee bent forward between the ankle and the hip.
final class MechLegs {
    static final double REACH = 0.999;

    private MechLegs() {
    }

    static void leg(LanternPainter painter, MechPose pose, boolean right, double apart, int seed) {
        int side = right ? 0 : 1;
        Vec3 toes = pose.toes[side];
        Vec3 hip = pose.hips().point(MechPainter.side(MechScript.HIP, right));
        Vec3 ankle = reached(hip, pose.ankle[side]);
        Vec3 knee = knee(ankle, hip, toes);
        Vec3 shin = knee.subtract(ankle).normalize();
        Vec3 thigh = hip.subtract(knee).normalize();
        Frame foot = Frame.of(ankle, toes, Vectors.UP, 1.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -pose.tip[side]);
        MechParts.draw(painter, right ? MechLegShapes.FOOT : MechLegShapes.FOOT_LEFT, foot, 1.0, apart, seed);
        MechParts.draw(painter, right ? MechLegShapes.SHIN_PART : MechLegShapes.SHIN_LEFT,
                MechPainter.limb(ankle, shin, toes), 1.0, apart, seed + 20);
        MechParts.draw(painter, right ? MechLegShapes.KNEE : MechLegShapes.KNEE_LEFT,
                MechPainter.limb(knee, shin.add(thigh).normalize(), toes), 1.0, apart, seed + 40);
        MechParts.draw(painter, right ? MechLegShapes.THIGH_PART : MechLegShapes.THIGH_LEFT,
                MechPainter.limb(knee, thigh, pose.hips().ahead()), 1.0, apart, seed + 50);
        if (BoneView.shown()) {
            BoneView.bone(hip, knee, BoneView.CONSTRUCT);
            BoneView.bone(knee, ankle, BoneView.CONSTRUCT);
            BoneView.bone(ankle, foot.at(0.0, 0.0, 1.4), BoneView.CONSTRUCT);
        }
    }

    // The ankle as far towards where its foot is put as the leg reaches: a foot put further off hangs short of it on
    // the leg, never drawn off it.
    static Vec3 reached(Vec3 hip, Vec3 ankle) {
        Vec3 to = ankle.subtract(hip);
        double most = (MechLegShapes.SHIN + MechLegShapes.THIGH) * REACH;
        double far = to.length();
        return far <= most ? ankle : hip.add(to.scale(most / far));
    }

    // The knee two fixed lengths from the ankle and the hip, bent towards forward.
    static Vec3 knee(Vec3 ankle, Vec3 hip, Vec3 forward) {
        double shin = MechLegShapes.SHIN;
        double thigh = MechLegShapes.THIGH;
        Vec3 to = hip.subtract(ankle);
        double far = Mth.clamp(to.length(), Math.abs(shin - thigh) + 1.0E-3, (shin + thigh) * REACH);
        Vec3 along = to.lengthSqr() < 1.0E-8 ? Vectors.UP : to.normalize();
        double a = (shin * shin - thigh * thigh + far * far) / (2.0 * far);
        double h = Math.sqrt(Math.max(0.0, shin * shin - a * a));
        Vec3 bend = forward.subtract(along.scale(forward.dot(along)));
        bend = bend.lengthSqr() < 1.0E-8 ? Vectors.across(along)[0] : bend.normalize();
        return ankle.add(along.scale(a)).add(bend.scale(h));
    }
}
