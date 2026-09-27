package nl.tivek.multiversepowers.engine.rig;

// Bends a joint back from the angle it wants, just as far as it takes for what it moves to stop going into something:
// a finger's tip comes to rest on the palm or on the skin of what the hand holds instead of sinking in.
public final class Settle {
    // How finely the way back is searched for the first free angle before that one is sharpened.
    private static final int SCAN = 16;

    private Settle() {
    }

    @FunctionalInterface
    public interface Depth {
        // How deep something goes in (0 or less when clear), given the rig's bones in RigSpace form.
        double of(double[] space);
    }

    // Moves the turn from where the pose has it towards `clear` and stops at the first angle where the depth is at
    // most the tolerance, the way a finger opens only until it is out; returns that angle. When no angle up to
    // `clear` is free this joint cannot help and the pose is left as it was.
    public static double back(Rig rig, RigPose pose, int bone, int turn, double clear, double[] space, Depth depth,
            double tolerance, int steps) {
        double wanted = pose.get(bone, turn);
        RigSpace.pose(rig, pose, space);
        if (depth.of(space) <= tolerance) {
            return wanted;
        }
        double target = rig.joint(bone).turn(turn).clamp(clear);
        double stuck = wanted;
        for (int i = 1; i <= SCAN; i++) {
            double angle = wanted + (target - wanted) * i / SCAN;
            pose.set(bone, turn, angle);
            RigSpace.pose(rig, pose, space);
            if (depth.of(space) <= tolerance) {
                double free = angle;
                for (int s = 0; s < steps; s++) {
                    double middle = (free + stuck) * 0.5;
                    pose.set(bone, turn, middle);
                    RigSpace.pose(rig, pose, space);
                    if (depth.of(space) <= tolerance) {
                        free = middle;
                    } else {
                        stuck = middle;
                    }
                }
                pose.set(bone, turn, free);
                RigSpace.pose(rig, pose, space);
                return free;
            }
            stuck = angle;
        }
        pose.set(bone, turn, wanted);
        RigSpace.pose(rig, pose, space);
        return wanted;
    }

    // Eases several turns (bones[i], turns[i]) towards their open angles together, each by the same share of the way,
    // and stops at the first share where the depth is within the tolerance: a curled finger that cannot get out joint
    // by joint relaxes as a whole. Returns the share of the wanted bend kept (1 all of it, 0 fully open).
    public static double ease(Rig rig, RigPose pose, int[] bones, int[] turns, double[] opens, double[] space,
            Depth depth, double tolerance, int steps) {
        RigSpace.pose(rig, pose, space);
        if (depth.of(space) <= tolerance) {
            return 1.0;
        }
        double[] wanted = new double[bones.length];
        for (int i = 0; i < bones.length; i++) {
            wanted[i] = pose.get(bones[i], turns[i]);
        }
        double stuck = 1.0;
        for (int i = 1; i <= SCAN; i++) {
            double keep = 1.0 - (double) i / SCAN;
            if (clear(rig, pose, bones, turns, opens, wanted, keep, space, depth, tolerance)) {
                double free = keep;
                for (int s = 0; s < steps; s++) {
                    double middle = (free + stuck) * 0.5;
                    if (clear(rig, pose, bones, turns, opens, wanted, middle, space, depth, tolerance)) {
                        free = middle;
                    } else {
                        stuck = middle;
                    }
                }
                clear(rig, pose, bones, turns, opens, wanted, free, space, depth, tolerance);
                return free;
            }
            stuck = keep;
        }
        clear(rig, pose, bones, turns, opens, wanted, 1.0, space, depth, tolerance);
        return 1.0;
    }

    private static boolean clear(Rig rig, RigPose pose, int[] bones, int[] turns, double[] opens, double[] wanted,
            double keep, double[] space, Depth depth, double tolerance) {
        for (int i = 0; i < bones.length; i++) {
            pose.set(bones[i], turns[i], opens[i] + (wanted[i] - opens[i]) * keep);
        }
        RigSpace.pose(rig, pose, space);
        return depth.of(space) <= tolerance;
    }

    // For what no single joint can undo: nudges the given turns (bones[i], turns[i]) one at a time, keeping every
    // nudge that makes it less deep and halving the nudge when none does, until the depth is within the tolerance.
    // Returns the depth left.
    public static double relax(Rig rig, RigPose pose, int[] bones, int[] turns, double[] space, Depth depth,
            double tolerance, int rounds) {
        RigSpace.pose(rig, pose, space);
        double now = depth.of(space);
        double step = 0.2;
        for (int round = 0; round < rounds && now > tolerance && step > 1.0E-4; round++) {
            boolean better = false;
            for (int i = 0; i < bones.length && now > tolerance; i++) {
                double was = pose.get(bones[i], turns[i]);
                for (int sign = -1; sign <= 1; sign += 2) {
                    pose.set(bones[i], turns[i], was + sign * step);
                    RigSpace.pose(rig, pose, space);
                    double tried = depth.of(space);
                    if (tried < now - 1.0E-9) {
                        now = tried;
                        better = true;
                        break;
                    }
                    pose.set(bones[i], turns[i], was);
                }
            }
            if (!better) {
                step *= 0.5;
            }
        }
        RigSpace.pose(rig, pose, space);
        return now;
    }
}
