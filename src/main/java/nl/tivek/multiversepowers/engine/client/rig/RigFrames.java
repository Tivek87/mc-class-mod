package nl.tivek.multiversepowers.engine.client.rig;

import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.Socket;

// The frame of every bone, for a painter to draw on.
public final class RigFrames {
    private RigFrames() {
    }

    public static ConstructPainter.Frame[] pose(Rig rig, RigPose pose, ConstructPainter.Frame root) {
        ConstructPainter.Frame[] frames = new ConstructPainter.Frame[rig.size()];
        pose(rig, pose, root, frames);
        return frames;
    }

    // Each joint turns about itself and then the frame moves onto it, the way hand-built chains do it, so a rig gives
    // the very same frames, bit for bit.
    public static void pose(Rig rig, RigPose pose, ConstructPainter.Frame root, ConstructPainter.Frame[] out) {
        pose(rig, pose, root, out, false);
    }

    // With moveFirst the frame moves onto the joint first and turns there: the same bones, but the way chains built
    // like that round their numbers.
    public static void pose(Rig rig, RigPose pose, ConstructPainter.Frame root, ConstructPainter.Frame[] out,
            boolean moveFirst) {
        if (pose.rig() != rig) {
            throw new IllegalArgumentException("A pose of another rig");
        }
        for (int b = 0; b < rig.size(); b++) {
            int parent = rig.parent(b);
            ConstructPainter.Frame frame = parent < 0 ? root : out[parent];
            double x = rig.offsetX(b);
            double y = rig.offsetY(b);
            double z = rig.offsetZ(b);
            Joint joint = rig.joint(b);
            if (moveFirst) {
                frame = frame.moved(x, y, z);
                for (int t = 0; t < joint.size(); t++) {
                    Joint.Axis axis = joint.turn(t).axis();
                    frame = frame.turned(0.0, 0.0, 0.0, axis.x, axis.y, axis.z, pose.get(b, t));
                }
                out[b] = frame;
                continue;
            }
            for (int t = 0; t < joint.size(); t++) {
                Joint.Axis axis = joint.turn(t).axis();
                frame = frame.turned(x, y, z, axis.x, axis.y, axis.z, pose.get(b, t));
            }
            out[b] = frame.moved(x, y, z);
        }
    }

    public static ConstructPainter.Frame at(Socket socket, ConstructPainter.Frame[] frames) {
        return frames[socket.bone()].moved(socket.x(), socket.y(), socket.z());
    }
}
