package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.physics.BallJoint;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import nl.tivek.multiversepowers.engine.physics.HingeJoint;
import nl.tivek.multiversepowers.engine.physics.Pin;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import nl.tivek.multiversepowers.engine.physics.SelfContact;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

// A creature gone limp: one box per part of its model, joined where the parts turn, falling, bumping into blocks and
// hanging from whatever holds it. Built from the pose the creature was drawn in, so it never jumps.
final class Ragdoll {
    enum State {
        HELD,
        FLYING,
        DEAD
    }

    static final double TICK = 0.05;
    private static final double DENSITY = 100.0;
    // How deep a limb may already lie in another part as the body is built before it is no longer kept out of it.
    private static final double TOUCHING = 0.01;
    // Carried further than this in one step (a teleport), the whole body goes along at once instead of stretching.
    private static final double LEAP = 4.0;
    // How much of the gap to where a thrown creature really is its body closes each tick, flying on by the speed the
    // server gave it in between (where a thrown creature is drawn only catches up every few ticks, in jolts).
    private static final double CATCH_UP = 0.35;
    // As the game's own creatures fall: 0.08 blocks a tick faster every tick.
    private static final double GRAVITY = -32.0;
    // How hard a blast throws a body, per unit of face turned to it per unit of mass, in blocks per second at its
    // middle: a zombie a blast of 4 (TNT) goes off 2 blocks from flies off at about 15 blocks a second, as it would
    // in the game.
    private static final double BLAST = 490.0;
    // A blast throws up as well as away, and sets each part turning a little its own way.
    private static final double LIFT = 0.35;
    private static final double SPIN = 0.25;
    // How much faster or slower than its body a part may be thrown (a part nearer the blast flies first).
    private static final double UNEVEN = 0.5;

    // Scratch for drawing, which only ever happens on the render thread.
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Quaterniond A = new Quaterniond();
    private static final Quaterniond B = new Quaterniond();
    private static final Quaterniond PARENT = new Quaterniond();
    private static final Quaterniond OWN = new Quaterniond();
    private static final Quaterniond MIXED = new Quaterniond();
    private static final Vector3d EULER = new Vector3d();
    private static final Vector3d LOCAL = new Vector3d();
    private static final Vector3d MIDDLE = new Vector3d();
    private static final Vector3f ORIGIN = new Vector3f();
    private static final Matrix3d ROTATION = new Matrix3d();
    private static final Quaternionf BENT = new Quaternionf();

    final LivingEntity entity;
    final EntityModel<?> model;
    final List<ModelParts.Part> parts;
    final RigidWorld world = new RigidWorld();
    // Each part's body; a long arm or leg is two, its near half in `body` and its far half past the knee in `lower`.
    private final int[] body;
    private final int[] lower;
    private final ModelParts.Bend[] bends;
    private final double[] center;
    // Every part it moves, the parts that copy them too, and where each sits in the copies layers draw.
    private final ModelPart[] moved;
    private final Map<EntityModel<?>, ModelPart[]> copies = new IdentityHashMap<>();
    // Holds the core where its creature is while something carries or throws it.
    private final Pin hold;
    private final double[] offset = new double[3];
    private final double[] target = new double[3];
    private final double[] next = new double[3];
    final int core;
    State state;
    int age;
    int still;
    int dead = -1;
    int sunk = -1;
    // 1 fully limp, falling to 0 as the creature takes back its own pose.
    double limp = 1.0;
    boolean ending;
    boolean flew;
    private boolean leapt;
    private final double[] was;
    private final double[] now;
    private final double[] scratch = new double[7];

    private Ragdoll(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, int[] body,
            ModelParts.Bend[] bends, double[] center, int core, State state) {
        this.entity = entity;
        this.model = model;
        this.parts = parts;
        this.body = body;
        this.bends = bends;
        this.lower = new int[parts.size()];
        Arrays.fill(this.lower, -1);
        this.center = center;
        this.core = core;
        this.state = state;
        this.hold = new Pin(core, 0.0, 0.0, 0.0, 0.0);
        this.world.gravity = GRAVITY;
        int bodies = parts.size();
        for (ModelParts.Bend bend : bends) {
            bodies += bend != null ? 1 : 0;
        }
        this.was = new double[bodies * 7];
        this.now = new double[bodies * 7];
        int count = parts.size();
        for (ModelParts.Part part : parts) {
            count += part.followers().size();
        }
        this.moved = new ModelPart[count];
        int k = parts.size();
        for (int i = 0; i < parts.size(); i++) {
            this.moved[i] = parts.get(i).part();
            for (ModelPart follower : parts.get(i).followers()) {
                this.moved[k++] = follower;
            }
        }
    }

    // Built the first time the creature is drawn after something made it limp: `model` is its model as posed this
    // frame, `drawn` the matrix from model space to the camera, `camera` where the camera stands.
    static Ragdoll build(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, Matrix4f drawn,
            Vec3 camera, float partialTick, State state, boolean stiff, RagdollProfiles.Profile profile,
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
            double scale = scaleOf(frame);
            double heft = heft(part.role()) * profile.part(part.name()).mass().orElse(1.0);
            rotationOf(frame, turn);
            body[i] = ragdoll.box(frame, near, scale, heft, camera, turn, vel);
            halves[i] = far == null ? null : new float[][] { near, far };
        }
        // The far halves after every part's own body, so a part's index stays its body's.
        for (int i = 0; i < n; i++) {
            if (halves[i] != null) {
                ModelParts.Bend bend = bends[i];
                float[] near = halves[i][0];
                float[] far = halves[i][1];
                double scale = scaleOf(frames[i]);
                double heft = heft(parts.get(i).role()) * profile.part(parts.get(i).name()).mass().orElse(1.0);
                rotationOf(frames[i], turn);
                ragdoll.lower[i] = ragdoll.box(frames[i], far, scale, heft, camera, turn, vel);
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
                ragdoll.apart(i, hangs[i], kneeHang[i], core);
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
            ragdoll.apart(lastLeg, hangs[lastLeg], kneeHang[lastLeg], firstLeg);
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
                    ragdoll.apart(i, hangs[i], kneeHang[i], j);
                }
            }
            if (role == ModelParts.Role.HEAD) {
                ragdoll.apart(i, hangs[i], kneeHang[i], core);
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
        if (state != State.DEAD) {
            ragdoll.hold.to(ragdoll.target[0], ragdoll.target[1], ragdoll.target[2]);
        }
        return ragdoll;
    }

    // One body for the box b (pixels, in the part's frame) of a part drawn in `frame`, placed and moving with it.
    private int box(Matrix4f frame, float[] b, double scale, double heft, Vec3 camera, Quaterniond turn,
            double[] vel) {
        double hx = Math.max(0.02, (b[3] - b[0]) / 32.0 * scale);
        double hy = Math.max(0.02, (b[4] - b[1]) / 32.0 * scale);
        double hz = Math.max(0.02, (b[5] - b[2]) / 32.0 * scale);
        int made = this.world.add(DENSITY * heft * 8.0 * hx * hy * hz, hx, hy, hz);
        Vector3f c = frame.transformPosition(new Vector3f((b[0] + b[3]) / 32.0F, (b[1] + b[4]) / 32.0F,
                (b[2] + b[5]) / 32.0F), new Vector3f());
        this.world.place(made, c.x + camera.x, c.y + camera.y, c.z + camera.z, turn.x, turn.y, turn.z, turn.w);
        this.world.velocity(made, vel[0], vel[1], vel[2], 0.0, 0.0, 0.0);
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
    private void apart(int i, double[] hang, @Nullable double[] kneeHang, int j) {
        for (int to : new int[] { this.body[j], this.lower[j] }) {
            if (to < 0) {
                continue;
            }
            this.keepOut(this.body[i], hang, to);
            if (this.lower[i] >= 0) {
                this.keepOut(this.lower[i], kneeHang, to);
            }
        }
    }

    // A limb's far part (its capsule along its longest side, away from where it hangs) kept out of another body.
    private void keepOut(int b, double[] hang, int other) {
        int longest = 0;
        for (int a = 1; a < 3; a++) {
            if (this.world.half(b, a) > this.world.half(b, longest)) {
                longest = a;
            }
        }
        double radius = Double.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            if (a != longest) {
                radius = Math.min(radius, this.world.half(b, a));
            }
        }
        double reach = Math.max(0.0, this.world.half(b, longest) - radius);
        double sign = hang[longest] > 0.0 ? -1.0 : 1.0;
        double[] far = new double[3];
        double[] near = new double[3];
        far[longest] = sign * reach;
        // A model can be built with a limb partly inside its body (a horse's legs start in its belly): the capsule
        // then starts further out, or is left off, or it would shove the two apart every step and fling the body.
        for (int tries = 0; tries < 5; tries++) {
            near[longest] = sign * reach * (tries * 0.25 - 0.3);
            SelfContact contact = new SelfContact(b, near, far, radius, other);
            if (contact.deepest(this.world) < TOUCHING) {
                this.world.add(contact);
                return;
            }
        }
    }

    private static void local(double[] pose, double x, double y, double z, double[] out) {
        Quat.unrotate(pose, 3, x, y, z, out, 0);
    }

    static double scaleOf(Matrix4f frame) {
        return Math.sqrt(frame.m00() * frame.m00() + frame.m01() * frame.m01() + frame.m02() * frame.m02());
    }

    static void rotationOf(Matrix4f frame, Quaterniond out) {
        double s = scaleOf(frame);
        Matrix3d m = new Matrix3d(frame.m00() / s, frame.m01() / s, frame.m02() / s, frame.m10() / s,
                frame.m11() / s, frame.m12() / s, frame.m20() / s, frame.m21() / s, frame.m22() / s);
        out.setFromNormalized(m);
    }

    // Sets every part turning by (wx, wy, wz) radians per second about a line through `pivot`, as one rigid body would.
    void tip(double wx, double wy, double wz, Vec3 pivot) {
        double[] v = new double[6];
        double[] at = new double[7];
        for (int i = 0; i < this.world.count(); i++) {
            this.world.velocity(i, v);
            this.world.pose(i, at);
            double rx = at[0] - pivot.x;
            double ry = at[1] - pivot.y;
            double rz = at[2] - pivot.z;
            this.world.velocity(i, v[0] + wy * rz - wz * ry, v[1] + wz * rx - wx * rz,
                    v[2] + wx * ry - wy * rx, v[3] + wx, v[4] + wy, v[5] + wz);
        }
    }

    // As it dies its limbs give way, folding the way the body falls (about the level line (ax, az)), each a little
    // differently: legs standing straight under a body would hold it up like a table's.
    void giveWay(RandomSource random, double ax, double az, double speed) {
        double[] v = new double[6];
        for (int i = 0; i < this.world.count(); i++) {
            if (i == this.body[this.core]) {
                continue;
            }
            double along = speed * (0.7 + 0.6 * random.nextDouble());
            double across = speed * 0.25 * random.nextGaussian();
            this.world.velocity(i, v);
            this.world.velocity(i, v[0], v[1], v[2], v[3] + ax * along - az * across, v[4],
                    v[5] + az * along + ax * across);
        }
    }

    // Carries the core along with its creature over the coming step, as the creature moves this tick; `smooth`
    // glides it along the creature's own speed instead of every jolt of where it is drawn.
    void follow(LivingEntity entity, boolean smooth) {
        this.next[0] = entity.getX() + this.offset[0];
        this.next[1] = entity.getY() + this.offset[1];
        this.next[2] = entity.getZ() + this.offset[2];
        if (smooth) {
            Vec3 push = entity.getDeltaMovement();
            double gx = this.target[0] + push.x;
            double gy = this.target[1] + push.y;
            double gz = this.target[2] + push.z;
            this.next[0] = gx + (this.next[0] - gx) * CATCH_UP;
            this.next[1] = gy + (this.next[1] - gy) * CATCH_UP;
            this.next[2] = gz + (this.next[2] - gz) * CATCH_UP;
        }
        double dx = this.next[0] - this.target[0];
        double dy = this.next[1] - this.target[1];
        double dz = this.next[2] - this.target[2];
        double far = dx * dx + dy * dy + dz * dz;
        if (far > LEAP * LEAP) {
            this.world.shift(dx, dy, dz);
            this.hold.to(this.next[0], this.next[1], this.next[2]);
            this.leapt = true;
        } else {
            this.hold.sweep(this.target, this.next, TICK);
        }
        System.arraycopy(this.next, 0, this.target, 0, 3);
        if (far > 1.0E-8) {
            this.world.wake();
        }
    }

    // Thrown by a blast of the given power at center (it reaches twice its power in blocks, as the game counts it),
    // seen: how much of the body the blast reaches past walls (0 to 1). How far the whole body flies goes by its face
    // to its weight, so small light creatures fly far and big heavy ones hardly; each part flies by how near it is.
    void blast(Vec3 center, double power, double seen, RandomSource random) {
        double reach = power * 2.0;
        double[] at = new double[7];
        double[] v = new double[6];
        double face = 0.0;
        double mass = 0.0;
        for (int b = 0; b < this.world.count(); b++) {
            double hx = this.world.half(b, 0);
            double hy = this.world.half(b, 1);
            double hz = this.world.half(b, 2);
            face += 2.0 * (hx * hy + hy * hz + hx * hz);
            mass += this.world.mass(b);
        }
        this.world.pose(this.body[this.core], at);
        double middle = 1.0 - Math.sqrt(sq(at[0] - center.x) + sq(at[1] - center.y) + sq(at[2] - center.z)) / reach;
        if (middle <= 0.0 || mass <= 0.0) {
            return;
        }
        double push = BLAST * middle * seen * face / mass;
        for (int b = 0; b < this.world.count(); b++) {
            this.world.pose(b, at);
            double dx = at[0] - center.x;
            double dy = at[1] - center.y;
            double dz = at[2] - center.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 1.0E-4) {
                dx = 0.0;
                dy = 1.0;
                dz = 0.0;
                d = 1.0;
            }
            double near = Math.max(0.0, 1.0 - d / reach);
            double own = push * Math.max(1.0 - UNEVEN, Math.min(1.0 + UNEVEN, near / middle));
            this.world.velocity(b, v);
            this.world.velocity(b, v[0] + dx / d * own, v[1] + (dy / d + LIFT) * own, v[2] + dz / d * own,
                    v[3] + random.nextGaussian() * own * SPIN, v[4] + random.nextGaussian() * own * SPIN,
                    v[5] + random.nextGaussian() * own * SPIN);
        }
        this.world.wake();
    }

    private static double sq(double a) {
        return a * a;
    }

    void die(int tick) {
        this.hold.release();
        this.state = State.DEAD;
        this.dead = tick;
        this.limp = 1.0;
        this.ending = false;
        this.world.wake();
    }

    void step(int substeps, Blocks blocks) {
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        this.world.step(TICK, substeps, blocks);
        this.remember(this.now);
        if (this.leapt) {
            System.arraycopy(this.now, 0, this.was, 0, this.now.length);
            this.leapt = false;
        }
        this.age++;
    }

    private void remember(double[] into) {
        for (int i = 0; i < this.world.count(); i++) {
            this.world.pose(i, this.scratch);
            System.arraycopy(this.scratch, 0, into, i * 7, 7);
        }
    }

    // Where the core is between the last two steps.
    Vec3 coreAt(double partialTick) {
        int o = this.core * 7;
        return new Vec3(this.was[o] + (this.now[o] - this.was[o]) * partialTick,
                this.was[o + 1] + (this.now[o + 1] - this.was[o + 1]) * partialTick,
                this.was[o + 2] + (this.now[o + 2] - this.was[o + 2]) * partialTick);
    }

    // Puts every moving part (and the parts that copy it) where its box is this frame, relative to how the model is
    // drawn now, `sink` blocks lower. A part keeps `1 - limp` of its own animated pose, so a creature getting up
    // blends back smoothly.
    void pose(Matrix4f drawn, Vec3 camera, double partialTick, double sink, Restore restore) {
        for (int i = 0; i < this.parts.size(); i++) {
            ModelParts.Part part = this.parts.get(i);
            int o = i * 7;
            double t = partialTick;
            double px = this.was[o] + (this.now[o] - this.was[o]) * t - camera.x;
            double py = this.was[o + 1] + (this.now[o + 1] - this.was[o + 1]) * t - camera.y - sink;
            double pz = this.was[o + 2] + (this.now[o + 2] - this.was[o + 2]) * t - camera.z;
            A.set(this.was[o + 3], this.was[o + 4], this.was[o + 5], this.was[o + 6]);
            B.set(this.now[o + 3], this.now[o + 4], this.now[o + 5], this.now[o + 6]);
            A.slerp(B, t);
            ModelParts.parentFrame(this.model, drawn, part, FRAME);
            double s = scaleOf(FRAME);
            rotationOf(FRAME, PARENT);
            // The part's own turn: its box's turn seen from the part it hangs in.
            OWN.set(PARENT).conjugate().mul(A);
            FRAME.transformPosition(ORIGIN.set(0.0F, 0.0F, 0.0F));
            LOCAL.set(px - ORIGIN.x, py - ORIGIN.y, pz - ORIGIN.z);
            MIXED.set(PARENT).conjugate().transform(LOCAL);
            LOCAL.div(s);
            MIDDLE.set(this.center[i * 3], this.center[i * 3 + 1], this.center[i * 3 + 2]);
            OWN.transform(MIDDLE);
            LOCAL.sub(MIDDLE);
            ModelPart target = part.part();
            restore.keep(target);
            float x = (float) (LOCAL.x * 16.0);
            float y = (float) (LOCAL.y * 16.0);
            float z = (float) (LOCAL.z * 16.0);
            double w = Math.max(0.0, Math.min(1.0, this.limp));
            if (w < 1.0) {
                MIXED.rotationZYX(target.zRot, target.yRot, target.xRot).slerp(OWN, w);
                x = (float) (target.x + (x - target.x) * w);
                y = (float) (target.y + (y - target.y) * w);
                z = (float) (target.z + (z - target.z) * w);
            } else {
                MIXED.set(OWN);
            }
            MIXED.get(ROTATION);
            ROTATION.getEulerAnglesZYX(EULER);
            target.setPos(x, y, z);
            target.setRotation((float) EULER.x, (float) EULER.y, (float) EULER.z);
            List<ModelPart> followers = part.followers();
            for (int k = 0; k < followers.size(); k++) {
                restore.keep(followers.get(k));
                followers.get(k).copyFrom(target);
            }
            if (this.lower[i] >= 0) {
                // The far half's turn seen from the near half: how far the knee or elbow is bent.
                int l = this.lower[i] * 7;
                B.set(this.was[l + 3], this.was[l + 4], this.was[l + 5], this.was[l + 6]);
                MIXED.set(this.now[l + 3], this.now[l + 4], this.now[l + 5], this.now[l + 6]);
                B.slerp(MIXED, t);
                MIXED.set(A).conjugate().mul(B);
                if (w < 1.0) {
                    MIXED.set(B.identity().slerp(MIXED, w));
                }
                BENT.set((float) MIXED.x, (float) MIXED.y, (float) MIXED.z, (float) MIXED.w);
                BentParts.bend(target, this.bends[i], BENT);
                for (int k = 0; k < followers.size(); k++) {
                    BentParts.bend(followers.get(k), this.bends[i], BENT);
                }
            }
        }
    }

    // A layer's own copy of the model (a sheep's wool, a saddle) takes the pose just given to the model itself.
    void copyTo(EntityModel<?> copy, Restore restore) {
        ModelPart[] to = this.copies.get(copy);
        if (to == null) {
            to = new ModelPart[this.moved.length];
            for (int i = 0; i < this.moved.length; i++) {
                to[i] = ModelParts.counterpart(this.model, copy, this.moved[i]);
            }
            this.copies.put(copy, to);
        }
        for (int i = 0; i < to.length; i++) {
            if (to[i] != null && to[i] != this.moved[i]) {
                restore.keep(to[i]);
                to[i].copyFrom(this.moved[i]);
                BentParts.same(this.moved[i], to[i]);
            }
        }
    }
}
