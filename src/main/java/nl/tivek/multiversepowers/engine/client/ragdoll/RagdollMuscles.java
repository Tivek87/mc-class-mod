package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import nl.tivek.multiversepowers.engine.physics.joint.Muscle;
import nl.tivek.multiversepowers.engine.physics.joint.Pin;
import org.joml.Matrix4f;

// The muscles across a ragdoll's joints and how hard each holds, tick by tick. A body struck dead holds the pose it was
// struck in a moment, its knees giving first, then its hips, back and neck, so it crumples where the blow pushed it
// instead of folding at once; a creature thrown alive braces, its arms out ahead when it falls on its face and wide
// when it falls back, its head held up, and goes slack as it lands; one hanging from a ledge reaches up to it and kicks
// its legs. Limp otherwise.
final class RagdollMuscles {
    enum Group {
        NECK,
        SHOULDER,
        ELBOW,
        WRIST,
        BLADE,
        HIP,
        KNEE,
        ANKLE,
        WAIST,
        PELVIS,
        LOOSE
    }

    // Dying: how hard each group holds as the creature dies and in how many ticks it is gone, square fading.
    private static final double[] DYING_TONE = { 0.8, 0.35, 0.35, 0.3, 1.0, 0.8, 0.85, 0.5, 0.9, 0.9, 0.3 };
    private static final double[] DYING_TICKS = { 12.0, 4.0, 4.0, 6.0, 14.0, 8.0, 5.0, 6.0, 10.0, 10.0, 6.0 };
    // Bracing: the arms' tone and the rest's, and how fast it goes slack once down (a share each tick).
    private static final double BRACE_ARMS = 0.35;
    private static final double BRACE_HEAD = 0.5;
    private static final double BRACE_REST = 0.15;
    private static final double SLACKEN = 0.6;
    // Hanging: how hard the arms reach up to the hands' hold, the legs' kick (radians, and radians a tick).
    private static final double HANG_ARMS = 0.6;
    private static final double HANG_LEGS = 0.3;
    private static final double KICK = 0.55;
    private static final double KICK_PACE = 0.35;

    // A limb hung this far or less (pixels) to the side of the middle is neither a left nor a right one.
    private static final float MIDDLE = 1.0F;

    private final List<Muscle> muscles = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    // Per muscle: the side of the body its limb is on (+1 its own left, -1 its right, 0 the middle) and the way its
    // limb points from where it hangs, in the limb's own axes.
    private final List<double[]> bones = new ArrayList<>();
    private final List<Integer> sides = new ArrayList<>();
    private final double[] t = new double[8];

    // A muscle across every joint, holding to begin with the pose the body went limp in (RagdollMuscles), and a grip
    // in each of a person's hands for an edge it may catch hold of. `hung`: where each part's pieces hang, in their own
    // axes; `from`: the body each part hangs from.
    static void build(Ragdoll ragdoll, List<ModelParts.Part> parts, double[][][] hung, int[] from, int core) {
        RigidWorld world = ragdoll.world;
        for (int i = 0; i < parts.size(); i++) {
            ModelParts.Part part = parts.get(i);
            ModelParts.Role role = part.role();
            boolean crossed = ModelBends.crossed(part);
            float x = ModelParts.rest(part, new Matrix4f()).m30();
            int side = x > MIDDLE ? 1 : x < -MIDDLE ? -1 : 0;
            boolean arm = role == ModelParts.Role.ARM;
            boolean leg = role == ModelParts.Role.LEG;
            if (i != core && from[i] >= 0 && !crossed) {
                ragdoll.muscles.add(world, from[i], ragdoll.body[i], arm ? Group.SHOULDER
                        : leg ? Group.HIP : role == ModelParts.Role.HEAD ? Group.NECK
                        : Group.LOOSE, side, away(hung[i][0]));
            }
            if (ragdoll.lower[i] >= 0) {
                ragdoll.muscles.add(world, ragdoll.body[i], ragdoll.lower[i], i == core ? Group.WAIST
                        : arm ? Group.ELBOW : leg ? Group.KNEE
                        : Group.LOOSE, side, away(hung[i][1]));
            }
            if (ragdoll.tip[i] >= 0) {
                ragdoll.muscles.add(world, ragdoll.lower[i], ragdoll.tip[i], i == core ? Group.PELVIS
                        : arm ? Group.WRIST : leg ? Group.ANKLE
                        : Group.LOOSE, side, away(hung[i][2]));
            }
            if (ragdoll.blade[i] >= 0) {
                ragdoll.muscles.add(world, ragdoll.body[core], ragdoll.blade[i], Group.BLADE, side,
                        new double[] { 0.0, 1.0, 0.0 });
            }
            if (arm && side != 0 && !crossed) {
                Pin grip = new Pin(ragdoll.piece(i, 2), 0.0, 0.0, 0.0, 0.0);
                world.add(grip);
                ragdoll.grips[side < 0 ? 0 : 1] = grip;
            }
        }
    }

    // The way a piece points from where it hangs (its anchor, in its own axes) to its middle.
    private static double[] away(double[] anchor) {
        return anchor == null ? new double[] { 0.0, 1.0, 0.0 } : new double[] { -anchor[0], -anchor[1], -anchor[2] };
    }

    void add(RigidWorld world, int a, int b, Group group, int side, double[] bone) {
        double[] rest = new double[4];
        Quat.relative(world.q, a * 4, world.q, b * 4, rest, 0);
        Muscle muscle = new Muscle(a, b, rest);
        world.addFirst(muscle);
        this.muscles.add(muscle);
        this.groups.add(group);
        this.sides.add(side);
        double length = Math.sqrt(bone[0] * bone[0] + bone[1] * bone[1] + bone[2] * bone[2]);
        this.bones.add(length < 1.0E-6 ? new double[] { 0.0, 1.0, 0.0 }
                : new double[] { bone[0] / length, bone[1] / length, bone[2] / length });
    }

    boolean any() {
        return !this.muscles.isEmpty();
    }

    // Every muscle's own pose becomes the one the body is in now: it holds that from here on.
    void rebase(RigidWorld world) {
        for (Muscle muscle : this.muscles) {
            Quat.relative(world.q, muscle.first() * 4, world.q, muscle.second() * 4, muscle.rest(), 0);
            muscle.relax();
        }
    }

    // Every muscle slack, holding nothing.
    void slack() {
        for (Muscle muscle : this.muscles) {
            muscle.tone(0.0);
            muscle.relax();
        }
    }

    // `ticks` since it died: every muscle still holds the pose it died in, fading.
    void dying(int ticks) {
        for (int m = 0; m < this.muscles.size(); m++) {
            int g = this.groups.get(m).ordinal();
            double left = Math.max(0.0, 1.0 - ticks / DYING_TICKS[g]);
            Muscle muscle = this.muscles.get(m);
            muscle.relax();
            muscle.tone(DYING_TONE[g] * left * left);
        }
    }

    // Thrown alive and still in the air: `forward` whether it falls on its face; `share` how much it still braces.
    void brace(boolean forward, double share) {
        for (int m = 0; m < this.muscles.size(); m++) {
            Muscle muscle = this.muscles.get(m);
            Group group = this.groups.get(m);
            int side = this.sides.get(m);
            switch (group) {
                case SHOULDER -> {
                    // Out ahead and a little down to catch itself, or out wide to the sides and up.
                    if (forward) {
                        this.reach(m, side * 0.35, 0.45, -0.85);
                    } else {
                        this.reach(m, side * 0.9, -0.3, -0.2);
                    }
                    muscle.tone(BRACE_ARMS * share);
                }
                case ELBOW, WRIST -> {
                    muscle.target(0.0, 0.0, 0.0, 1.0);
                    muscle.tone(BRACE_ARMS * 0.6 * share);
                }
                case NECK, BLADE -> {
                    muscle.relax();
                    muscle.tone(BRACE_HEAD * share);
                }
                default -> {
                    muscle.relax();
                    muscle.tone(BRACE_REST * share);
                }
            }
        }
    }

    // Down after a flight: whatever tone is left goes out of it.
    void slacken() {
        for (Muscle muscle : this.muscles) {
            muscle.tone(muscle.tone() * SLACKEN);
        }
    }

    // Hanging from its hands `ticks` into the hang: arms up and straight, legs kicking.
    void hang(int ticks) {
        for (int m = 0; m < this.muscles.size(); m++) {
            Muscle muscle = this.muscles.get(m);
            int side = this.sides.get(m);
            switch (this.groups.get(m)) {
                case SHOULDER -> {
                    this.reach(m, side * 0.2, -1.0, 0.1);
                    muscle.tone(HANG_ARMS);
                }
                case ELBOW, WRIST -> {
                    muscle.target(0.0, 0.0, 0.0, 1.0);
                    muscle.tone(HANG_ARMS);
                }
                case HIP -> {
                    double kick = Math.sin(ticks * KICK_PACE + side * 1.7) * KICK;
                    this.reach(m, side * 0.15, 1.0, -kick);
                    muscle.tone(HANG_LEGS);
                }
                case KNEE -> {
                    muscle.relax();
                    muscle.tone(HANG_LEGS * 0.5);
                }
                default -> {
                    muscle.relax();
                    muscle.tone(BRACE_REST);
                }
            }
        }
    }

    // Muscle m pulls its limb to point the way (x, y, z) seen from what it hangs from (that body's own axes: a person's
    // model, +x its left, +y down, -z ahead), turned the least way from how it was built.
    private void reach(int m, double x, double y, double z) {
        Muscle muscle = this.muscles.get(m);
        double[] rest = muscle.rest();
        double[] bone = this.bones.get(m);
        Quat.rotate(rest, 0, bone[0], bone[1], bone[2], this.t, 0);
        double length = Math.sqrt(x * x + y * y + z * z);
        double dx = x / length;
        double dy = y / length;
        double dz = z / length;
        double cx = this.t[1] * dz - this.t[2] * dy;
        double cy = this.t[2] * dx - this.t[0] * dz;
        double cz = this.t[0] * dy - this.t[1] * dx;
        double cos = this.t[0] * dx + this.t[1] * dy + this.t[2] * dz;
        // The shortest turn from the bone to the way: (1 + cos, c) normalised, as a quaternion.
        double w = 1.0 + cos;
        if (w < 1.0E-6) {
            muscle.relax();
            return;
        }
        this.t[4] = cx;
        this.t[5] = cy;
        this.t[6] = cz;
        this.t[7] = w;
        Quat.normalize(this.t, 4);
        Quat.multiply(this.t, 4, rest, 0, this.t, 0);
        muscle.target(this.t[0], this.t[1], this.t[2], this.t[3]);
    }
}
