package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.joint.BallJoint;
import nl.tivek.multiversepowers.engine.physics.joint.HingeJoint;
import nl.tivek.multiversepowers.engine.physics.joint.LimbJoint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import nl.tivek.multiversepowers.engine.physics.joint.SelfContact;
import nl.tivek.multiversepowers.engine.physics.joint.SpineJoint;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Builds a ragdoll's bodies from the pose its creature was drawn in, and its joints from how its model was built: each
// limb reaches as far as a body's does from where it hangs at rest, whatever pose it went limp in. A part bends at its
// joints (ModelBends.chain): an arm is its upper arm, forearm and hand, a leg its thigh, shin and foot, a trunk its
// chest, belly and pelvis, each a body of its own; an arm hangs from a shoulder blade, a small unseen body on the chest.
final class RagdollBuild {
    private static final double DENSITY = 100.0;
    // How much faster a part stops turning while it touches the ground or another body (per second).
    private static final double CONTACT_DAMPING = 25.0;
    // How deep a limb may already lie in another part as the body is built before it is no longer kept out of it.
    private static final double TOUCHING = 0.01;
    // How far a limp trunk leans out to its side at the waist and twists there, either way (radians).
    private static final double WAIST_SIDE = 0.35;
    private static final double WAIST_TWIST = 0.45;
    // With a pelvis, the waist takes this share of the trunk's reach and the pelvis this share.
    private static final double WAIST_SHARE = 0.65;
    private static final double PELVIS_SHARE = 0.4;
    // A limp hand folds either way, leans to its sides and twists this far; a foot points its toes down further than
    // up; the tip of a leg standing out sideways folds down more than up (radians).
    private static final double WRIST = 1.0;
    private static final double WRIST_SIDE = 0.35;
    private static final double WRIST_TWIST = 0.3;
    private static final double TOES_UP = 0.35;
    private static final double TOES_DOWN = 0.75;
    private static final double ANKLE_SIDE = 0.25;
    private static final double ANKLE_TWIST = 0.15;
    private static final double TIP_UP = 0.4;
    private static final double TIP_DOWN = 0.8;
    // A shoulder blade: how thick its body is (pixels) and what share of its length that body runs each way from its
    // middle, how much heavier than its size, how far it swings ahead (and back) and up (and down) about its middle
    // way, that middle way ahead of and above how it lies at rest, and its twist (radians).
    private static final float BLADE_THICK = 2.0F;
    private static final float BLADE_SHARE = 0.3F;
    private static final double BLADE_HEFT = 2.0;
    private static final double BLADE_SWING = 0.2;
    private static final double BLADE_LIFT = 0.18;
    private static final double BLADE_AHEAD = 0.05;
    private static final double BLADE_RAISED = 0.1;
    private static final double BLADE_TWIST = 0.08;
    private static final String WAIST = "waist";
    // A part hung this far or less (pixels) to the side of the middle is neither a left nor a right one.
    private static final float MIDDLE = 1.0F;
    // A limb's bone pointing this much along a way it bends is turned the next way instead (a head held ahead nods).
    private static final float ALONG = 0.8F;

    // How far a limb swings from where it hangs as the model was built, in radians: back and forth, in towards the
    // middle and out, and its twist either way.
    private record Reach(double back, double fore, double in, double out, double twist) {
    }

    private static final Reach SHOULDER = new Reach(0.9, 2.9, 0.5, 2.9, 1.2);
    private static final Reach HIP = new Reach(0.35, 1.9, 0.2, 0.7, 0.4);
    private static final Reach PAW = new Reach(0.9, 0.9, 0.35, 0.35, 0.3);
    private static final Reach NECK = new Reach(0.7, 0.9, 0.6, 0.6, 1.1);
    private static final Reach LOOSE = new Reach(1.1, 1.1, 1.1, 1.1, 0.6);

    private RagdollBuild() {
    }

    // A person's arm or leg, a pair of legs or four, a head; anything else swings loose. A resource pack's swing
    // makes it the same every way.
    private static Reach reach(ModelParts.Role role, int legs, RagdollProfiles.Tuning tuning) {
        Reach own = switch (role) {
            case ARM -> SHOULDER;
            case LEG -> legs == 2 ? HIP : legs == 4 ? PAW : LOOSE;
            case HEAD -> NECK;
            default -> LOOSE;
        };
        double twist = tuning.twist().map(Math::toRadians).orElse(own.twist());
        return tuning.swing().map(Math::toRadians).map(swing -> new Reach(swing, swing, swing, swing, twist))
                .orElse(new Reach(own.back(), own.fore(), own.in(), own.out(), twist));
    }

    // A limb's joint on the trunk, from how the model was built: its bone (from where it hangs to its middle) swings
    // about a middle way between how far it reaches ahead and behind, out and in, and twists about itself from how it
    // is turned there. `trunk` and `limb` are the bodies, their anchors in their own axes.
    private static LimbJoint limb(int trunk, double[] anchorA, int limb, double[] anchorB, ModelParts.Part part,
            Quaternionf coreRest, Reach reach) {
        Matrix4f rest = ModelParts.rest(part, new Matrix4f());
        Quaternionf back = new Quaternionf(coreRest).conjugate();
        // The limb's own axes into the trunk's, as built.
        Quaternionf into = new Quaternionf(back).mul(rest.getNormalizedRotation(new Quaternionf()));
        float[] b = part.bounds();
        Vector3f own = new Vector3f((b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F);
        if (own.lengthSquared() < 0.25F) {
            int longest = b[4] - b[1] > b[3] - b[0] ? 1 : 0;
            longest = b[5] - b[2] > b[longest + 3] - b[longest] ? 2 : longest;
            own.zero().setComponent(longest, 1.0F);
        }
        own.normalize();
        Vector3f bone = into.transform(new Vector3f(own));
        Vector3f ahead = back.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        if (Math.abs(bone.dot(ahead)) > ALONG) {
            back.transform(ahead.set(0.0F, 1.0F, 0.0F));
        }
        // Turning about `bend` takes the bone ahead, about `spread` out to its own side (across the bone and ahead).
        Vector3f bend = new Vector3f(bone).cross(ahead).normalize();
        float x = rest.m30();
        Vector3f side = new Vector3f(bend);
        if (side.dot(back.transform(new Vector3f(x < 0.0F ? -1.0F : 1.0F, 0.0F, 0.0F))) < 0.0F) {
            side.negate();
        }
        Vector3f spread = new Vector3f(bone).cross(side).normalize();
        double in = Math.abs(x) <= MIDDLE ? Math.min(reach.in(), reach.out()) : reach.in();
        double outMost = Math.abs(x) <= MIDDLE ? in : reach.out();
        double forth = (reach.fore() - reach.back()) * 0.5;
        double apart = (outMost - in) * 0.5;
        Vector3f turn = new Vector3f(bend).mul((float) forth).add(new Vector3f(spread).mul((float) apart));
        Quaternionf toMiddle = turn.lengthSquared() < 1.0E-12F ? new Quaternionf()
                : new Quaternionf().fromAxisAngleRad(new Vector3f(turn).normalize(), turn.length());
        Vector3f middle = toMiddle.transform(new Vector3f(bone));
        Vector3f across = toMiddle.transform(new Vector3f(bend));
        // Its twist is read from the way ahead as it stands across the bone at rest.
        Vector3f aheadA = new Vector3f(ahead).sub(new Vector3f(bone).mul(bone.dot(ahead))).normalize();
        Vector3f aheadB = new Quaternionf(into).conjugate().transform(new Vector3f(aheadA));
        return new LimbJoint(trunk, anchorA, vector(middle), vector(across), vector(bone), vector(aheadA), limb,
                anchorB, vector(own), vector(aheadB), (reach.fore() + reach.back()) * 0.5, (outMost + in) * 0.5,
                -reach.twist(), reach.twist());
    }

    private static double[] vector(Vector3f v) {
        return new double[] { v.x, v.y, v.z };
    }

    // Built the first time the creature is drawn after something made it limp: `model` is its model as posed this
    // frame, `drawn` the matrix from model space to the camera, `camera` where the camera stands, `feet` where the
    // creature is drawn.
    static Ragdoll build(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, Matrix4f drawn,
            Vec3 camera, Vec3 feet, Ragdoll.State state, boolean stiff, RagdollProfiles.Profile profile,
            Vec3 velocity) {
        int n = parts.size();
        int core = ModelBends.core(parts);
        RagdollProfiles.Tuning waist = profile.part(WAIST);
        ModelBends.Bend[][] chains = new ModelBends.Bend[n][];
        for (int i = 0; i < n; i++) {
            chains[i] = stiff || i == core && waist.swing().orElse(1.0) <= 0.0 ? ModelBends.NONE
                    : ModelBends.chain(parts, core, i);
        }
        int[] hang = ModelBends.hang(parts, core, chains[core]);
        float[][] blades = new float[n][];
        for (int i = 0; i < n && !stiff; i++) {
            blades[i] = ModelBends.shoulder(parts, core, i, hang);
        }
        fit(chains, blades, n, core);
        Ragdoll ragdoll = new Ragdoll(entity, model, parts, chains, hang, blades, core, state);
        RigidWorld world = ragdoll.world;
        world.friction = 0.8;
        world.angularDamping = 1.6;
        world.contactDamping = CONTACT_DAMPING;
        Quaterniond turn = new Quaterniond();
        double[] vel = { velocity.x, velocity.y, velocity.z };
        Matrix4f[] frames = new Matrix4f[n];
        float[][][] pieces = new float[n][][];
        for (int i = 0; i < n; i++) {
            frames[i] = ModelParts.frame(model, drawn, parts.get(i), new Matrix4f());
            pieces[i] = pieces(parts.get(i).bounds(), chains[i]);
            float[] near = pieces[i][0];
            ragdoll.center[i * 3] = (near[0] + near[3]) / 32.0;
            ragdoll.center[i * 3 + 1] = (near[1] + near[4]) / 32.0;
            ragdoll.center[i * 3 + 2] = (near[2] + near[5]) / 32.0;
        }
        // Every part's first piece before any other, so a part's index stays its body's; each piece as bent as it is
        // drawn now, so a creature posed with a bent knee or waist goes limp from just that pose.
        double[][][] hung = new double[n][3][];
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < n; i++) {
                if (k >= pieces[i].length) {
                    continue;
                }
                ModelParts.Part part = parts.get(i);
                Matrix4f frame = k == 0 ? frames[i] : new Matrix4f(frames[i]).scale(1.0F / 16.0F)
                        .mul(BentParts.piece(part.part(), k, new Matrix4f())).scale(16.0F);
                double scale = Ragdoll.scaleOf(frames[i]);
                double heft = heft(part.role()) * profile.part(part.name()).mass().orElse(1.0);
                Ragdoll.rotationOf(frame, turn);
                int made = box(ragdoll, frame, pieces[i][k], scale, heft, camera, turn, vel);
                ragdoll.placed(made, i, k, pieces[i][k]);
                if (k == 0) {
                    ragdoll.body[i] = made;
                    continue;
                }
                (k == 1 ? ragdoll.lower : ragdoll.tip)[i] = made;
                hung[i][k] = join(ragdoll, i, k, chains[i], pieces[i], scale, part.role(), waist);
            }
        }
        double[] pivot = new double[3];
        double[] away = new double[3];
        double[] across = new double[3];
        double[] aLocal = new double[3];
        double[] bLocal = new double[3];
        double[] axisA = new double[3];
        double[] axisB = new double[3];
        double[] refA = new double[3];
        double[] refB = new double[3];
        double[] coreAt = new double[7];
        double[] limbAt = new double[7];
        int legs = 0;
        for (int i = 0; i < n; i++) {
            legs += i != core && parts.get(i).role() == ModelParts.Role.LEG ? 1 : 0;
        }
        Quaternionf coreRest = ModelParts.rest(parts.get(core), new Matrix4f()).getNormalizedRotation(
                new Quaternionf());
        for (int i = 0; i < n; i++) {
            if (blades[i] != null) {
                blade(ragdoll, i, blades[i], frames, pieces[core][0], Ragdoll.scaleOf(frames[core]), camera, vel);
            }
        }
        for (int i = 0; i < n; i++) {
            if (i == core) {
                continue;
            }
            int trunk = ragdoll.piece(core, hang[i]);
            int blade = ragdoll.blade[i];
            world.pose(blade >= 0 ? blade : trunk, coreAt);
            Vector3f origin = frames[i].transformPosition(new Vector3f(), new Vector3f());
            pivot[0] = origin.x + camera.x;
            pivot[1] = origin.y + camera.y;
            pivot[2] = origin.z + camera.z;
            world.pose(ragdoll.body[i], limbAt);
            away[0] = limbAt[0] - pivot[0];
            away[1] = limbAt[1] - pivot[1];
            away[2] = limbAt[2] - pivot[2];
            double far = Math.sqrt(away[0] * away[0] + away[1] * away[1] + away[2] * away[2]);
            if (far < 1.0E-6) {
                away[0] = 0.0;
                away[1] = -1.0;
                away[2] = 0.0;
            } else {
                away[0] /= far;
                away[1] /= far;
                away[2] /= far;
            }
            across[0] = Math.abs(away[1]) < 0.9 ? away[2] : 0.0;
            across[1] = Math.abs(away[1]) < 0.9 ? 0.0 : -away[2];
            across[2] = Math.abs(away[1]) < 0.9 ? -away[0] : away[1];
            double al = Math.sqrt(across[0] * across[0] + across[1] * across[1] + across[2] * across[2]);
            for (int k = 0; k < 3; k++) {
                across[k] /= al;
            }
            local(coreAt, pivot[0] - coreAt[0], pivot[1] - coreAt[1], pivot[2] - coreAt[2], aLocal);
            local(limbAt, pivot[0] - limbAt[0], pivot[1] - limbAt[1], pivot[2] - limbAt[2], bLocal);
            ModelParts.Role role = parts.get(i).role();
            local(coreAt, away[0], away[1], away[2], axisA);
            local(limbAt, away[0], away[1], away[2], axisB);
            local(coreAt, across[0], across[1], across[2], refA);
            local(limbAt, across[0], across[1], across[2], refB);
            if (stiff) {
                // Held as it was drawn.
                world.add(new BallJoint(trunk, aLocal, axisA, refA, ragdoll.body[i], bLocal, axisB, refB, 0.02, -0.02,
                        0.02));
            } else if (blade >= 0) {
                // An arm hangs from its shoulder blade and turns within its reach of the chest, as without one: the
                // blade only moves where it hangs.
                world.add(new BallJoint(blade, aLocal, axisA, refA, ragdoll.body[i], bLocal, axisB, refB, Math.PI,
                        -Math.PI, Math.PI));
                world.add(limb(trunk, aLocal, ragdoll.body[i], bLocal, parts.get(i), coreRest,
                        reach(role, legs, profile.part(parts.get(i).name()))).turnsOnly());
            } else {
                world.add(limb(trunk, aLocal, ragdoll.body[i], bLocal, parts.get(i), coreRest,
                        reach(role, legs, profile.part(parts.get(i).name()))));
            }
            hung[i][0] = bLocal.clone();
        }
        // Every limb keeps out of the trunk and the head out of it; arms out of each other, the legs and the head,
        // and legs out of each other and the head. Many legs (a spider's) only keep out of the trunk. A hand or a foot
        // only keeps out of the trunk.
        for (int i = 0; i < n && !stiff; i++) {
            ModelParts.Role role = parts.get(i).role();
            if (i == core) {
                continue;
            }
            apart(ragdoll, i, hung[i], core);
            for (int j = 0; j < n; j++) {
                ModelParts.Role other = parts.get(j).role();
                // One way per pair: two limbs pushing each other out of their own boxes push along two lines, and
                // the difference would slide them over the ground.
                boolean apart = role == ModelParts.Role.ARM && (other == ModelParts.Role.ARM && i > j
                        || other == ModelParts.Role.LEG || other == ModelParts.Role.HEAD)
                        || role == ModelParts.Role.LEG && (other == ModelParts.Role.HEAD
                                || other == ModelParts.Role.LEG && i > j && legs <= 4);
                if (j != i && j != core && apart) {
                    apart(ragdoll, i, hung[i], j);
                }
            }
        }
        world.add(ragdoll.hold);
        ragdoll.remember(ragdoll.now);
        System.arraycopy(ragdoll.now, 0, ragdoll.was, 0, ragdoll.now.length);
        // Where the core sits from the creature's feet as it is drawn now, kept while something carries it.
        System.arraycopy(ragdoll.now, core * 7, ragdoll.target, 0, 3);
        ragdoll.offset[0] = ragdoll.target[0] - feet.x;
        ragdoll.offset[1] = ragdoll.target[1] - feet.y;
        ragdoll.offset[2] = ragdoll.target[2] - feet.z;
        if (state != Ragdoll.State.DEAD) {
            ragdoll.hold.to(ragdoll.target[0], ragdoll.target[1], ragdoll.target[2]);
        }
        return ragdoll;
    }

    // One body for the box b (pixels, in the part's frame) of a part drawn in `frame`, placed and moving with it.
    private static int box(Ragdoll ragdoll, Matrix4f frame, float[] b, double scale, double heft, Vec3 camera, Quaterniond turn,
            double[] vel) {
        double hx = Math.max(0.02, (b[3] - b[0]) / 32.0 * scale);
        double hy = Math.max(0.02, (b[4] - b[1]) / 32.0 * scale);
        double hz = Math.max(0.02, (b[5] - b[2]) / 32.0 * scale);
        int made = ragdoll.world.add(DENSITY * heft * 8.0 * hx * hy * hz, hx, hy, hz);
        Vector3f c = frame.transformPosition(new Vector3f((b[0] + b[3]) / 32.0F, (b[1] + b[4]) / 32.0F,
                (b[2] + b[5]) / 32.0F), new Vector3f());
        ragdoll.world.place(made, c.x + camera.x, c.y + camera.y, c.z + camera.z, turn.x, turn.y, turn.z, turn.w);
        ragdoll.world.velocity(made, vel[0], vel[1], vel[2], 0.0, 0.0, 0.0);
        return made;
    }

    // A block head is mostly air and a trunk mostly flesh: weighed by volume alone, a held creature would hang by its
    // head.
    private static double heft(ModelParts.Role role) {
        return switch (role) {
            case HEAD -> 0.35;
            case BODY -> 1.5;
            case LEG -> 1.2;
            default -> 1.0;
        };
    }

    // Part i (its first two pieces; its hand or foot only for the trunk) kept out of part j (its first two pieces, or
    // all three of the trunk's). `hung`: where each of part i's pieces hangs, in its own axes. Only where a limb hangs
    // on the trunk (j) is its end there left free.
    private static void apart(Ragdoll ragdoll, int i, double[][] hung, int j) {
        boolean trunk = j == ragdoll.core;
        for (int to : new int[] { ragdoll.body[j], ragdoll.lower[j], trunk ? ragdoll.tip[j] : -1 }) {
            if (to < 0) {
                continue;
            }
            keepOut(ragdoll, ragdoll.body[i], hung[0], to, !trunk);
            if (ragdoll.lower[i] >= 0) {
                keepOut(ragdoll, ragdoll.lower[i], hung[1], to, true);
            }
            if (ragdoll.tip[i] >= 0 && trunk) {
                keepOut(ragdoll, ragdoll.tip[i], hung[2], to, true);
            }
        }
    }

    // A body has at most RigidWorld.MOST parts: past that its arms go without shoulder blades, then its limbs without
    // hands and feet, its trunk without a pelvis, and last its limbs without knees.
    private static void fit(ModelBends.Bend[][] chains, float[][] blades, int n, int core) {
        for (int drop = 0; drop < 4 && count(chains, blades, n) > RigidWorld.MOST; drop++) {
            for (int i = 0; i < n; i++) {
                if (drop == 0) {
                    blades[i] = null;
                } else if (drop == 1 && i != core || drop == 2 && i == core) {
                    chains[i] = chains[i].length > 1 ? new ModelBends.Bend[] { chains[i][0] } : chains[i];
                } else if (drop == 3 && i != core) {
                    chains[i] = ModelBends.NONE;
                }
            }
        }
    }

    private static int count(ModelBends.Bend[][] chains, float[][] blades, int n) {
        int count = n;
        for (int i = 0; i < n; i++) {
            count += chains[i].length + (blades[i] != null ? 1 : 0);
        }
        return count;
    }

    // A part's box (pixels, its own frame) cut at its joints into its pieces, near to far.
    private static float[][] pieces(float[] b, ModelBends.Bend[] chain) {
        float[][] pieces = new float[chain.length + 1][];
        float[] rest = b;
        for (int k = 0; k < chain.length; k++) {
            ModelBends.Bend bend = chain[k];
            int a = bend.axis();
            float[] near = rest.clone();
            float[] far = rest.clone();
            near[bend.farSign() > 0.0F ? a + 3 : a] = bend.at();
            far[bend.farSign() > 0.0F ? a : a + 3] = bend.at();
            pieces[k] = near;
            rest = far;
        }
        pieces[chain.length] = rest;
        return pieces;
    }

    // The joint between part i's pieces k - 1 and k: a knee or an elbow, then an ankle or a wrist; a waist, then a
    // pelvis, the trunk's reach shared out between them. Where piece k hangs from it, in its own axes.
    private static double[] join(Ragdoll ragdoll, int i, int k, ModelBends.Bend[] chain, float[][] pieces,
            double scale, ModelParts.Role role, RagdollProfiles.Tuning waist) {
        ModelBends.Bend bend = chain[k - 1];
        float[] near = pieces[k - 1];
        float[] far = pieces[k];
        double[] anchorA = new double[3];
        double[] anchorB = new double[3];
        for (int a = 0; a < 3; a++) {
            anchorA[a] = (bend.knee()[a] - (near[a] + near[a + 3]) * 0.5) / 16.0 * scale;
            anchorB[a] = (bend.knee()[a] - (far[a] + far[a + 3]) * 0.5) / 16.0 * scale;
        }
        double[] hinge = { bend.hinge()[0], bend.hinge()[1], bend.hinge()[2] };
        double[] bone = new double[3];
        bone[bend.axis()] = bend.farSign();
        int a = k == 1 ? ragdoll.body[i] : ragdoll.lower[i];
        int b = k == 1 ? ragdoll.lower[i] : ragdoll.tip[i];
        if (i == ragdoll.core) {
            ModelBends.Bend first = chain[0];
            double share = chain.length == 1 ? 1.0 : k == 1 ? WAIST_SHARE : PELVIS_SHARE;
            double fold = waist.swing().map(Math::toRadians).orElse(first.max()) * share;
            double twist = waist.twist().map(Math::toRadians).orElse(WAIST_TWIST) * share;
            ragdoll.world.add(new SpineJoint(a, anchorA, hinge, bone, b, anchorB, hinge, bone, first.min() * share, fold,
                    WAIST_SIDE * share, twist));
        } else if (k == 1) {
            ragdoll.world.add(new HingeJoint(a, anchorA, hinge, bone, b, anchorB, hinge, bone, bend.min(), bend.max()));
        } else if (role == ModelParts.Role.ARM) {
            ragdoll.world.add(new SpineJoint(a, anchorA, hinge, bone, b, anchorB, hinge, bone, -WRIST, WRIST,
                    WRIST_SIDE, WRIST_TWIST));
        } else {
            boolean hanging = bend.axis() == 1;
            ragdoll.world.add(new SpineJoint(a, anchorA, hinge, bone, b, anchorB, hinge, bone,
                    hanging ? -TOES_UP : -TIP_UP, hanging ? TOES_DOWN : TIP_DOWN, ANKLE_SIDE, ANKLE_TWIST));
        }
        return anchorB;
    }

    // Arm i's shoulder blade: a small unseen body on the chest from where the blade meets the spine (`inner`, the
    // trunk's pixels) to where the arm hangs, joined to the chest at the spine; it swings a little ahead and up, as a
    // shoulder shrugs and rolls. `chest` is the trunk's first piece, as its body was built.
    private static void blade(Ragdoll ragdoll, int i, float[] inner, Matrix4f[] frames, float[] chest, double scale,
            Vec3 camera, double[] vel) {
        Matrix4f trunk = frames[ragdoll.core];
        Vector3f pivot = new Matrix4f(trunk).invert().transformPosition(frames[i].transformPosition(new Vector3f(),
                new Vector3f())).mul(16.0F);
        Vector3f bone = new Vector3f(pivot).sub(inner[0], inner[1], inner[2]);
        // Up the trunk and ahead, in its own axes: a person's, whose shoulders these are.
        Vector3f up = new Vector3f(0.0F, -1.0F, 0.0F);
        Vector3f ahead = new Vector3f(0.0F, 0.0F, -1.0F);
        if (bone.lengthSquared() < 1.0E-6F) {
            return;
        }
        bone.normalize();
        Vector3f lift = new Vector3f(bone).cross(up);
        Vector3f fore = new Vector3f(bone).cross(ahead);
        if (lift.lengthSquared() < 0.1F || fore.lengthSquared() < 0.1F) {
            return;
        }
        lift.normalize();
        fore.normalize();
        // Its box lies about the blade's middle well inside the chest, so it never touches what the chest lies on.
        float[] box = new float[6];
        for (int a = 0; a < 3; a++) {
            float mid = (inner[a] + pivot.get(a)) * 0.5F;
            float half = Math.max(BLADE_THICK * 0.5F, Math.abs(pivot.get(a) - inner[a]) * BLADE_SHARE);
            box[a] = mid - half;
            box[a + 3] = mid + half;
        }
        Quaterniond turn = new Quaterniond();
        Ragdoll.rotationOf(trunk, turn);
        int made = box(ragdoll, trunk, box, scale, BLADE_HEFT, camera, turn, vel);
        ragdoll.placed(made, ragdoll.core, 0, box);
        ragdoll.world.ghost(made);
        ragdoll.blade[i] = made;
        Quaternionf toMiddle = new Quaternionf().fromAxisAngleRad(fore, (float) BLADE_AHEAD)
                .mul(new Quaternionf().fromAxisAngleRad(lift, (float) BLADE_RAISED));
        Vector3f middle = toMiddle.transform(new Vector3f(bone));
        Vector3f across = new Vector3f(fore).sub(new Vector3f(middle).mul(fore.dot(middle))).normalize();
        Vector3f reference = new Vector3f(ahead).sub(new Vector3f(bone).mul(ahead.dot(bone))).normalize();
        double[] anchorA = new double[3];
        double[] anchorB = new double[3];
        for (int a = 0; a < 3; a++) {
            anchorA[a] = (inner[a] - (chest[a] + chest[a + 3]) * 0.5) / 16.0 * scale;
            anchorB[a] = (inner[a] - (box[a] + box[a + 3]) * 0.5) / 16.0 * scale;
        }
        ragdoll.world.add(new LimbJoint(ragdoll.body[ragdoll.core], anchorA, vector(middle), vector(across),
                vector(bone), vector(reference), made, anchorB, vector(bone), vector(reference), BLADE_SWING,
                BLADE_LIFT, -BLADE_TWIST, BLADE_TWIST));
    }

    // A limb's part (its capsule along its longest side, end to end when `whole`, else away from where it hangs) kept
    // out of another body.
    private static void keepOut(Ragdoll ragdoll, int b, double[] hang, int other, boolean whole) {
        int longest = 0;
        for (int a = 1; a < 3; a++) {
            if (ragdoll.world.half(b, a) > ragdoll.world.half(b, longest)) {
                longest = a;
            }
        }
        double radius = Double.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            if (a != longest) {
                radius = Math.min(radius, ragdoll.world.half(b, a));
            }
        }
        double reach = Math.max(0.0, ragdoll.world.half(b, longest) - radius);
        double sign = hang[longest] > 0.0 ? -1.0 : 1.0;
        double[] far = new double[3];
        double[] near = new double[3];
        far[longest] = sign * reach;
        // A model can be built with a limb partly inside its body (a horse's legs start in its belly): the capsule
        // then starts further out, or is left off, or it would shove the two apart every step and fling the body.
        double from = whole ? -1.0 : -0.3;
        for (int tries = 0; tries < 5; tries++) {
            near[longest] = sign * reach * (from + tries * 0.25);
            SelfContact contact = new SelfContact(b, near, far, radius, other);
            if (contact.deepest(ragdoll.world) < TOUCHING) {
                ragdoll.world.add(contact);
                return;
            }
        }
    }

    private static void local(double[] pose, double x, double y, double z, double[] out) {
        Quat.unrotate(pose, 3, x, y, z, out, 0);
    }
}
