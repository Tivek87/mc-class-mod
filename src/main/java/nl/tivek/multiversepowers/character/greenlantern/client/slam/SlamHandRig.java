package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;

// The slam hands' fingers as bones, every joint within a real hand's range: a knuckle gives a little backwards, the
// joints past it hardly.
final class SlamHandRig {
    // Per finger: where its knuckle sits across the palm, its thickness and its three bones' lengths.
    static final double[][] FINGERS = {
            { 0.30, 0.085, 0.36, 0.24, 0.20 },
            { 0.10, 0.088, 0.40, 0.27, 0.21 },
            { -0.10, 0.085, 0.37, 0.25, 0.20 },
            { -0.29, 0.075, 0.29, 0.20, 0.17 } };
    // The thumb's thickness and its three bones' lengths.
    static final double[] THUMB = { 0.10, 0.26, 0.22, 0.18 };
    static final int THUMB_FINGER = 4;
    private static final Joint.Turn[] FINGER_BENDS = { Joint.z(-0.6, 1.6), Joint.z(-0.1, 1.9), Joint.z(-0.2, 1.4) };
    private static final Joint.Turn[] THUMB_BENDS = { Joint.z(-0.4, 1.0), Joint.z(-0.2, 1.1), Joint.z(-0.3, 1.4) };
    private static final double THUMB_OUT = Math.toRadians(40.0);
    private static final double THUMB_ACROSS = Math.toRadians(15.0);
    static final Rig RIG = rig();

    private SlamHandRig() {
    }

    static int bone(int finger, int b) {
        return finger * 3 + b;
    }

    static double length(int finger, int b) {
        return finger < THUMB_FINGER ? FINGERS[finger][2 + b] : THUMB[1 + b];
    }

    static double thickness(int finger) {
        return finger < THUMB_FINGER ? FINGERS[finger][1] : THUMB[0];
    }

    // The thumb's first bone sits turned out of the palm before it bends, so its bend is its third turn.
    private static int bendTurn(int finger, int b) {
        return finger == THUMB_FINGER && b == 0 ? 2 : 0;
    }

    private static Rig rig() {
        Rig.Builder builder = Rig.builder();
        for (int f = 0; f < THUMB_FINGER; f++) {
            int knuckle = builder.bone("finger" + f, -1, 0.0, 0.30, FINGERS[f][0], new Joint(FINGER_BENDS[0]));
            int middle = builder.bone("finger" + f + "_middle", knuckle, 0.0, length(f, 0), 0.0,
                    new Joint(FINGER_BENDS[1]));
            builder.bone("finger" + f + "_tip", middle, 0.0, length(f, 1), 0.0, new Joint(FINGER_BENDS[2]));
        }
        int root = builder.bone("thumb", -1, -0.04, -0.22, 0.40, new Joint(Joint.x(THUMB_OUT, THUMB_OUT),
                Joint.z(THUMB_ACROSS, THUMB_ACROSS), THUMB_BENDS[0]));
        int middle = builder.bone("thumb_middle", root, 0.0, length(THUMB_FINGER, 0), 0.0, new Joint(THUMB_BENDS[1]));
        builder.bone("thumb_tip", middle, 0.0, length(THUMB_FINGER, 1), 0.0, new Joint(THUMB_BENDS[2]));
        return builder.build();
    }

    // Towards the palm, -x, a finger turns about z and its middle joint bends furthest; bent back, a finger gives
    // mostly at its knuckle.
    static RigPose pose(double[] curl) {
        RigPose pose = new RigPose(RIG);
        for (int f = 0; f <= THUMB_FINGER; f++) {
            for (int b = 0; b < 3; b++) {
                double share = curl[f] < 0.0 ? (b == 0 ? 1.2 : 0.25) : (b == 1 ? 1.2 : 0.9);
                pose.set(bone(f, b), bendTurn(f, b), curl[f] * share);
            }
        }
        return pose;
    }

    static Frame[] frames(Frame palm, double[] curl) {
        Frame[] frames = new Frame[RIG.size()];
        RigFrames.pose(RIG, pose(curl), palm, frames, true);
        return frames;
    }
}
