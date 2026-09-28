package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.BallJoint;
import nl.tivek.multiversepowers.engine.physics.HingeJoint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import nl.tivek.multiversepowers.engine.physics.SelfContact;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3f;

// Builds a ragdoll's bodies and joints from the pose its creature was drawn in.
final class RagdollBuild {
    private static final double DENSITY = 100.0;
    // How deep a limb may already lie in another part as the body is built before it is no longer kept out of it.
    private static final double TOUCHING = 0.01;

    private RagdollBuild() {
    }

    // Built the first time the creature is drawn after something made it limp: `model` is its model as posed this
    // frame, `drawn` the matrix from model space to the camera, `camera` where the camera stands.
    static Ragdoll build(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, Matrix4f drawn,
            Vec3 camera, float partialTick, Ragdoll.State state, boolean stiff, RagdollProfiles.Profile profile,
            Vec3 velocity) {
        int n = parts.size();
        int[] body = new int[n];
        double[] center = new double[n * 3];
        Matrix4f[] frames = new Matrix4f[n];
        int core = 0;
        double biggest = -1.0;
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = parts.get(i);
            float[] b = part.bounds();
            double volume = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
            boolean better = part.role() == ModelParts.Role.BODY && parts.get(core).role() != ModelParts.Role.BODY
                    || (part.role() == ModelParts.Role.BODY) == (parts.get(core).role() == ModelParts.Role.BODY)
                            && volume > biggest;
            if (i == 0 || better) {
                core = i;
                biggest = volume;
            }
        }
        ModelParts.Bend[] bends = new ModelParts.Bend[n];
        for (int i = 0; i < n && !stiff; i++) {
            bends[i] = i == core ? null : ModelParts.bend(parts.get(i));
        }
        Ragdoll ragdoll = new Ragdoll(entity, model, parts, body, bends, center, core, state);
        RigidWorld world = ragdoll.world;
        world.friction = 0.8;
        world.angularDamping = 1.6;
        Quaterniond turn = new Quaterniond();
        double[] vel = { velocity.x, velocity.y, velocity.z };
        double[][] kneeHang = new double[n][];
        float[][][] halves = new float[n][][];
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = parts.get(i);
            Matrix4f frame = ModelParts.frame(model, drawn, part, new Matrix4f());
            frames[i] = frame;
            ModelParts.Bend bend = bends[i];
            float[] b = part.bounds();
            float[] near = b;
            float[] far = null;
            if (bend != null) {
                near = b.clone();
                far = b.clone();
                int a = bend.axis();
                near[bend.farSign() > 0.0F ? a + 3 : a] = bend.at();
                far[bend.farSign() > 0.0F ? a : a + 3] = bend.at();
            }
            center[i * 3] = (near[0] + near[3]) / 32.0;
            center[i * 3 + 1] = (near[1] + near[4]) / 32.0;
            center[i * 3 + 2] = (near[2] + near[5]) / 32.0;
            double scale = Ragdoll.scaleOf(frame);
            double heft = heft(part.role()) * profile.part(part.name()).mass().orElse(1.0);
            Ragdoll.rotationOf(frame, turn);
            body[i] = box(ragdoll, frame, near, scale, heft, camera, turn, vel);
            halves[i] = far == null ? null : new float[][] { near, far };
        }
        // The far halves after every part's own body, so a part's index stays its body's.
        for (int i = 0; i < n; i++) {
            if (halves[i] != null) {
                ModelParts.Bend bend = bends[i];
                float[] near = halves[i][0];
                float[] far = halves[i][1];
                double scale = Ragdoll.scaleOf(frames[i]);
                double heft = heft(parts.get(i).role()) * profile.part(parts.get(i).name()).mass().orElse(1.0);
                Ragdoll.rotationOf(frames[i], turn);
                ragdoll.lower[i] = box(ragdoll, frames[i], far, scale, heft, camera, turn, vel);
                double[] anchorA = new double[3];
                double[] anchorB = new double[3];
                for (int k = 0; k < 3; k++) {
                    anchorA[k] = (bend.knee()[k] - (near[k] + near[k + 3]) * 0.5) / 16.0 * scale;
                    anchorB[k] = (bend.knee()[k] - (far[k] + far[k + 3]) * 0.5) / 16.0 * scale;
                }
                double[] hinge = { bend.hinge()[0], bend.hinge()[1], bend.hinge()[2] };
                double[] bone = new double[3];
                bone[bend.axis()] = bend.farSign();
                world.add(new HingeJoint(body[i], anchorA, hinge, bone, ragdoll.lower[i], anchorB, hinge, bone,
                        bend.min(), bend.max()));
                kneeHang[i] = anchorB;
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
        double[][] hangs = new double[n][];
        world.pose(body[core], coreAt);
        for (int i = 0; i < n; i++) {
            if (i == core) {
                continue;
            }
            Vector3f origin = frames[i].transformPosition(new Vector3f(), new Vector3f());
            pivot[0] = origin.x + camera.x;
            pivot[1] = origin.y + camera.y;
            pivot[2] = origin.z + camera.z;
            world.pose(body[i], limbAt);
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
            local(coreAt, away[0], away[1], away[2], axisA);
            local(limbAt, away[0], away[1], away[2], axisB);
            local(coreAt, across[0], across[1], across[2], refA);
            local(limbAt, across[0], across[1], across[2], refB);
            ModelParts.Role role = parts.get(i).role();
            RagdollProfiles.Tuning tuning = profile.part(parts.get(i).name());
            double swing = stiff ? 0.02 : tuning.swing().map(Math::toRadians).orElse(switch (role) {
                case HEAD -> 0.9;
                case ARM -> 2.4;
                case LEG -> 1.3;
                default -> 1.1;
            });
            double twist = stiff ? 0.02 : tuning.twist().map(Math::toRadians).orElse(switch (role) {
                case HEAD -> 0.9;
                case ARM -> 1.4;
                case LEG -> 0.4;
                default -> 0.6;
            });
            world.add(new BallJoint(body[core], aLocal, axisA, refA, body[i], bLocal, axisB, refB, swing, -twist,
                    twist));
            hangs[i] = bLocal.clone();
            if (!stiff && role != ModelParts.Role.HEAD) {
                apart(ragdoll, i, hangs[i], kneeHang[i], core);
            }
        }
        // A pair of legs side by side stays apart; four legs stand too far from each other to meet.
        int firstLeg = -1;
        int legs = 0;
        int lastLeg = -1;
        for (int i = 0; i < n; i++) {
            if (parts.get(i).role() == ModelParts.Role.LEG && i != core) {
                legs++;
                firstLeg = firstLeg < 0 ? i : firstLeg;
                lastLeg = i;
            }
        }
        if (!stiff && legs == 2) {
            apart(ragdoll, lastLeg, hangs[lastLeg], kneeHang[lastLeg], firstLeg);
        }
        // Arms keep out of the legs and the head, legs out of the head, and the head out of the trunk.
        for (int i = 0; i < n && !stiff; i++) {
            ModelParts.Role role = parts.get(i).role();
            if (i == core) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                ModelParts.Role other = parts.get(j).role();
                boolean apart = role == ModelParts.Role.ARM && (other == ModelParts.Role.LEG
                        || other == ModelParts.Role.HEAD) || role == ModelParts.Role.LEG && other == ModelParts.Role.HEAD;
                if (j != i && j != core && apart) {
                    apart(ragdoll, i, hangs[i], kneeHang[i], j);
                }
            }
            if (role == ModelParts.Role.HEAD) {
                apart(ragdoll, i, hangs[i], kneeHang[i], core);
            }
        }
        world.add(ragdoll.hold);
        ragdoll.remember(ragdoll.now);
        System.arraycopy(ragdoll.now, 0, ragdoll.was, 0, ragdoll.now.length);
        // Where the core sits from the creature's feet as it is drawn now, kept while something carries it.
        Vec3 feet = entity.getPosition(partialTick);
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

    // Part i (both halves of a bent limb) kept out of part j (both its halves as well).
    private static void apart(Ragdoll ragdoll, int i, double[] hang, @Nullable double[] kneeHang, int j) {
        for (int to : new int[] { ragdoll.body[j], ragdoll.lower[j] }) {
            if (to < 0) {
                continue;
            }
            keepOut(ragdoll, ragdoll.body[i], hang, to);
            if (ragdoll.lower[i] >= 0) {
                keepOut(ragdoll, ragdoll.lower[i], kneeHang, to);
            }
        }
    }

    // A limb's far part (its capsule along its longest side, away from where it hangs) kept out of another body.
    private static void keepOut(Ragdoll ragdoll, int b, double[] hang, int other) {
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
        for (int tries = 0; tries < 5; tries++) {
            near[longest] = sign * reach * (tries * 0.25 - 0.3);
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
