package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.BallJoint;
import nl.tivek.multiversepowers.engine.physics.HingeJoint;
import nl.tivek.multiversepowers.engine.physics.LimbJoint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import nl.tivek.multiversepowers.engine.physics.SelfContact;
import nl.tivek.multiversepowers.engine.physics.SpineJoint;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Builds a ragdoll's bodies from the pose its creature was drawn in, and its joints from how its model was built: each
// limb reaches as far as a body's does from where it hangs at rest, whatever pose it went limp in.
final class RagdollBuild {
    private static final double DENSITY = 100.0;
    // How deep a limb may already lie in another part as the body is built before it is no longer kept out of it.
    private static final double TOUCHING = 0.01;
    // How far a limp trunk leans out to its side at the waist and twists there, either way (radians).
    private static final double WAIST_SIDE = 0.35;
    private static final double WAIST_TWIST = 0.45;
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
        int[] body = new int[n];
        double[] center = new double[n * 3];
        Matrix4f[] frames = new Matrix4f[n];
        int core = ModelParts.core(parts);
        ModelParts.Bend[] bends = new ModelParts.Bend[n];
        RagdollProfiles.Tuning waist = profile.part(WAIST);
        for (int i = 0; i < n && !stiff; i++) {
            bends[i] = i != core ? ModelParts.bend(parts.get(i))
                    : waist.swing().orElse(1.0) > 0.0 ? ModelParts.waist(parts, core) : null;
        }
        // Which parts hang from the trunk's far half (legs, a tail): joined to it, not to the near half.
        boolean[] hangsLow = bends[core] == null ? new boolean[n] : ModelParts.far(parts, core, bends[core]);
        Ragdoll ragdoll = new Ragdoll(entity, model, parts, body, bends, hangsLow, center, core, state);
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
        // The far halves after every part's own body, so a part's index stays its body's; each as bent as it is drawn
        // now, so a creature posed with a bent knee or waist goes limp from just that pose.
        for (int i = 0; i < n; i++) {
            if (halves[i] != null) {
                ModelParts.Bend bend = bends[i];
                float[] near = halves[i][0];
                float[] far = halves[i][1];
                double scale = Ragdoll.scaleOf(frames[i]);
                double heft = heft(parts.get(i).role()) * profile.part(parts.get(i).name()).mass().orElse(1.0);
                Matrix4f bent = new Matrix4f(frames[i]);
                Matrix3f drawnBent = BentParts.turn(parts.get(i).part());
                if (drawnBent != null) {
                    float[] k = bend.knee();
                    bent.translate(k[0] / 16.0F, k[1] / 16.0F, k[2] / 16.0F).mul(new Matrix4f().set(drawnBent))
                            .translate(-k[0] / 16.0F, -k[1] / 16.0F, -k[2] / 16.0F);
                }
                Ragdoll.rotationOf(bent, turn);
                ragdoll.lower[i] = box(ragdoll, bent, far, scale, heft, camera, turn, vel);
                double[] anchorA = new double[3];
                double[] anchorB = new double[3];
                for (int k = 0; k < 3; k++) {
                    anchorA[k] = (bend.knee()[k] - (near[k] + near[k + 3]) * 0.5) / 16.0 * scale;
                    anchorB[k] = (bend.knee()[k] - (far[k] + far[k + 3]) * 0.5) / 16.0 * scale;
                }
                double[] hinge = { bend.hinge()[0], bend.hinge()[1], bend.hinge()[2] };
                double[] bone = new double[3];
                bone[bend.axis()] = bend.farSign();
                if (i == core) {
                    double fold = waist.swing().map(Math::toRadians).orElse(bend.max());
                    double twist = waist.twist().map(Math::toRadians).orElse(WAIST_TWIST);
                    world.add(new SpineJoint(body[i], anchorA, hinge, bone, ragdoll.lower[i], anchorB, hinge, bone,
                            bend.min(), fold, WAIST_SIDE, twist));
                } else {
                    world.add(new HingeJoint(body[i], anchorA, hinge, bone, ragdoll.lower[i], anchorB, hinge, bone,
                            bend.min(), bend.max()));
                }
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
        int legs = 0;
        for (int i = 0; i < n; i++) {
            legs += i != core && parts.get(i).role() == ModelParts.Role.LEG ? 1 : 0;
        }
        Quaternionf coreRest = ModelParts.rest(parts.get(core), new Matrix4f()).getNormalizedRotation(
                new Quaternionf());
        for (int i = 0; i < n; i++) {
            if (i == core) {
                continue;
            }
            int trunk = hangsLow[i] ? ragdoll.lower[core] : body[core];
            world.pose(trunk, coreAt);
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
            ModelParts.Role role = parts.get(i).role();
            if (stiff) {
                // Held as it was drawn.
                local(coreAt, away[0], away[1], away[2], axisA);
                local(limbAt, away[0], away[1], away[2], axisB);
                local(coreAt, across[0], across[1], across[2], refA);
                local(limbAt, across[0], across[1], across[2], refB);
                world.add(new BallJoint(trunk, aLocal, axisA, refA, body[i], bLocal, axisB, refB, 0.02, -0.02, 0.02));
            } else {
                world.add(limb(trunk, aLocal, body[i], bLocal, parts.get(i), coreRest,
                        reach(role, legs, profile.part(parts.get(i).name()))));
            }
            hangs[i] = bLocal.clone();
        }
        // Every limb keeps out of the trunk and the head out of it; arms out of each other, the legs and the head,
        // and legs out of each other and the head. Many legs (a spider's) only keep out of the trunk.
        for (int i = 0; i < n && !stiff; i++) {
            ModelParts.Role role = parts.get(i).role();
            if (i == core) {
                continue;
            }
            apart(ragdoll, i, hangs[i], kneeHang[i], core);
            for (int j = 0; j < n; j++) {
                ModelParts.Role other = parts.get(j).role();
                // One way per pair: two limbs pushing each other out of their own boxes push along two lines, and
                // the difference would slide them over the ground.
                boolean apart = role == ModelParts.Role.ARM && (other == ModelParts.Role.ARM && i > j
                        || other == ModelParts.Role.LEG || other == ModelParts.Role.HEAD)
                        || role == ModelParts.Role.LEG && (other == ModelParts.Role.HEAD
                                || other == ModelParts.Role.LEG && i > j && legs <= 4);
                if (j != i && j != core && apart) {
                    apart(ragdoll, i, hangs[i], kneeHang[i], j);
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

    // Part i (both halves of a bent limb) kept out of part j (both its halves as well). Only where a limb hangs on the
    // trunk (j) is its end there left free.
    private static void apart(Ragdoll ragdoll, int i, double[] hang, @Nullable double[] kneeHang, int j) {
        for (int to : new int[] { ragdoll.body[j], ragdoll.lower[j] }) {
            if (to < 0) {
                continue;
            }
            keepOut(ragdoll, ragdoll.body[i], hang, to, j != ragdoll.core);
            if (ragdoll.lower[i] >= 0) {
                keepOut(ragdoll, ragdoll.lower[i], kneeHang, to, true);
            }
        }
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
