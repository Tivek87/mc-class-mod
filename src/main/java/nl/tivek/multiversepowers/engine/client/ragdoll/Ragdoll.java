package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.BodyPose;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.Hanging;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.PoseBlend;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import nl.tivek.multiversepowers.engine.physics.joint.Pin;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
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

    // A living creature thrown limp: carried along its flight, then down where it fell, then getting up.
    enum Phase {
        AIR,
        DOWN,
        UP
    }

    static final double TICK = 0.05;
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
    // Lying down, its middle stays within this many blocks (level with the ground) of where its creature really is,
    // or this many once it lies still: any further, the whole body is moved this share of the rest of the way back
    // each tick.
    private static final double SLACK = 0.7;
    private static final double SLACK_STILL = 2.0;
    private static final double DRAW_BACK = 0.3;
    // Lying still, it looks this often (ticks) whether what it lies on is still there.
    private static final int PROBE_EVERY = 10;

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
    private static final BodyPose LIE = new BodyPose();
    private static final BodyPose OWN_POSE = new BodyPose();
    private static final BodyPose OUT = new BodyPose();
    private static final Quaternionf[] TURNS_1 = { new Quaternionf() };
    private static final Quaternionf[] TURNS_2 = { new Quaternionf(), new Quaternionf() };
    private static final Matrix4f TRUNK = new Matrix4f();
    private static final Matrix4f LINK = new Matrix4f();
    private static final Matrix4f LINK_TRUNK = new Matrix4f();
    private static final Matrix4f NONE = new Matrix4f();

    final LivingEntity entity;
    final EntityModel<?> model;
    final List<ModelParts.Part> parts;
    final RigidWorld world = new RigidWorld();
    // Each part's bodies from its near end, -1 where it has none: a long arm or leg is up to three (upper, lower, and
    // the hand or foot past its wrist or ankle), and so is a trunk that bends at its waist and pelvis (chest, belly,
    // pelvis). An arm with a shoulder blade hangs from a small unseen body of its own on the chest (`blade`).
    final int[] body;
    final int[] lower;
    final int[] tip;
    final int[] blade;
    // Each part's joints (ModelBends.chain), which piece of the trunk each part hangs from, and where each part's
    // first piece has its middle, in its own frame (blocks).
    private final ModelBends.Bend[][] chains;
    final int[] hang;
    final double[] center;
    // Every part it moves, the parts that copy them too, and where each sits in the copies layers draw.
    private final ModelPart[] moved;
    private final Set<ModelPart> own = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<EntityModel<?>, ModelPart[]> copies = new IdentityHashMap<>();
    // Which piece of the trunk each part hangs from, and whether it is drawn inside the trunk.
    private final Hanging hanging;
    // Holds the core where its creature is while something carries or throws it.
    final Pin hold;
    final double[] offset = new double[3];
    final double[] target = new double[3];
    private final double[] next = new double[3];
    final int core;
    State state;
    int age;
    int dead = -1;
    int sunk = -1;
    // 1 fully limp, falling to 0 as the creature takes back its own pose.
    double limp = 1.0;
    boolean flew;
    Phase phase = Phase.AIR;
    // Ticks down, how many of them on the ground, and ticks into getting up (-1 before).
    int down;
    int lain;
    int up = -1;
    // Where its boxes lie among every body's this tick (RagdollCrowd).
    int crowdFrom;
    int crowdTo;
    // Dead: ticks it has lain wholly still since it last moved, and how often it was made to give way again as it came
    // to rest still standing (RagdollFalls).
    int rested;
    int slumps;
    // Dead: the push every part was given as it went limp (its creature's own), whether the blow that killed it has
    // pushed it yet or a blast threw it, and the tick it tips over to a side of its own if no word of a blow comes.
    Vec3 built = Vec3.ZERO;
    boolean struck;
    boolean blasted;
    int toppleAt = -1;
    double toppleYaw;
    boolean stiff;
    // The head's part, -1 without one.
    final int head;
    // The body as it lay when it began to get up, the way it faces as it rises, and how it gets up (GetUp: a person by
    // way of its hands and one knee), planned the first time it is drawn getting up.
    @Nullable
    private double[] lay;
    float riseYaw;
    @Nullable
    private GetUp.Rise rise;
    private final boolean person;
    private boolean leapt;
    final double[] was;
    final double[] now;
    // Each body's part and piece of it, and its box's middle in that part's own frame (blocks): where it is drawn
    // getting up (`shown`, the last time it was), to go limp from there.
    final int[] partOf;
    final int[] pieceOf;
    final double[] middle;
    private final double[] shown;
    private boolean shownValid;
    private final double[] scratch = new double[7];

    Ragdoll(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, ModelBends.Bend[][] chains,
            int[] hang, float[][] blades, int core, State state) {
        int n = parts.size();
        this.entity = entity;
        this.model = model;
        this.parts = parts;
        this.chains = chains;
        this.hang = hang;
        this.body = new int[n];
        this.lower = new int[n];
        this.tip = new int[n];
        this.blade = new int[n];
        Arrays.fill(this.lower, -1);
        Arrays.fill(this.tip, -1);
        Arrays.fill(this.blade, -1);
        this.center = new double[n * 3];
        this.core = core;
        this.state = state;
        this.person = GetUp.person(model, parts, chains);
        int headAt = -1;
        for (int i = 0; i < n && headAt < 0; i++) {
            headAt = i != core && parts.get(i).role() == ModelParts.Role.HEAD ? i : -1;
        }
        this.head = headAt;
        boolean[] inside = new boolean[n];
        ModelPart trunk = parts.get(core).part();
        for (int i = 0; i < n; i++) {
            inside[i] = i != core && parts.get(i).parents().contains(trunk);
            this.own.add(parts.get(i).part());
            this.own.addAll(parts.get(i).followers());
        }
        this.hanging = new Hanging(n, core, inside, hang, chains[core]);
        this.hold = new Pin(core, 0.0, 0.0, 0.0, 0.0);
        this.world.gravity = GRAVITY;
        int bodies = n;
        for (int i = 0; i < n; i++) {
            bodies += chains[i].length + (blades[i] != null ? 1 : 0);
        }
        this.partOf = new int[bodies];
        this.pieceOf = new int[bodies];
        this.middle = new double[bodies * 3];
        this.shown = new double[bodies * 7];
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

    static double scaleOf(Matrix4f frame) {
        return Math.sqrt(frame.m00() * frame.m00() + frame.m01() * frame.m01() + frame.m02() * frame.m02());
    }

    static void rotationOf(Matrix4f frame, Quaterniond out) {
        double s = scaleOf(frame);
        Matrix3d m = new Matrix3d(frame.m00() / s, frame.m01() / s, frame.m02() / s, frame.m10() / s,
                frame.m11() / s, frame.m12() / s, frame.m20() / s, frame.m21() / s, frame.m22() / s);
        out.setFromNormalized(m);
    }

    // Body b is the box b (pixels, its part's own frame, before any joint turns it) of part i's piece k.
    void placed(int b, int i, int k, float[] box) {
        this.partOf[b] = i;
        this.pieceOf[b] = k;
        this.middle[b * 3] = (box[0] + box[3]) / 32.0;
        this.middle[b * 3 + 1] = (box[1] + box[4]) / 32.0;
        this.middle[b * 3 + 2] = (box[2] + box[5]) / 32.0;
    }

    // Part i's body for its piece k (0 its first), or its last piece when it has fewer.
    int piece(int i, int k) {
        return k >= 2 && this.tip[i] >= 0 ? this.tip[i] : k >= 1 && this.lower[i] >= 0 ? this.lower[i] : this.body[i];
    }

    // Whether its trunk or head rests on something: lying, not standing, kneeling or sitting up on its limbs.
    boolean slumped() {
        return this.world.touching(this.body[this.core])
                || this.lower[this.core] >= 0 && this.world.touching(this.lower[this.core])
                || this.tip[this.core] >= 0 && this.world.touching(this.tip[this.core])
                || this.head >= 0 && this.world.touching(this.body[this.head]);
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
        this.blasted = true;
        this.world.wake();
    }

    private static double sq(double a) {
        return a * a;
    }

    void die(int tick) {
        this.resume();
        this.hold.release();
        this.state = State.DEAD;
        this.dead = tick;
        this.limp = 1.0;
        this.phase = Phase.DOWN;
        this.lay = null;
        this.up = -1;
        this.world.wake();
    }

    // A body getting up that is thrown again or dies goes limp from the pose it was drawn in last, not from how it lay.
    private void resume() {
        if (this.phase != Phase.UP) {
            return;
        }
        double[] from = this.shownValid ? this.shown : this.lay;
        if (from != null) {
            for (int b = 0; b < this.world.count(); b++) {
                int o = b * 7;
                this.world.place(b, from[o], from[o + 1], from[o + 2], from[o + 3], from[o + 4], from[o + 5],
                        from[o + 6]);
            }
            this.remember(this.now);
            System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        }
        this.rise = null;
        this.shownValid = false;
    }

    // It has come down: no longer held to where its creature is, it falls and lies where the blow left it.
    void fall() {
        this.phase = Phase.DOWN;
        this.hold.release();
        this.down = 0;
        this.lain = 0;
        this.world.wake();
    }

    // Lying down, it slides and rolls as it will but never away from its creature: the body and the creature the
    // server moves are always in one place, so the body gets up where the creature stands. It is drawn back while it
    // still moves, where the pull goes unseen in its fall; lying still it never slides, unless its creature is moved
    // far away.
    void keepNear(LivingEntity entity) {
        Vec3 middle = this.coreAt(1.0);
        double dx = entity.getX() - middle.x;
        double dz = entity.getZ() - middle.z;
        double far = Math.sqrt(dx * dx + dz * dz);
        double slack = this.world.quiet() > 0 ? SLACK_STILL : SLACK;
        if (far > slack) {
            double back = DRAW_BACK * (far - SLACK) / far;
            this.world.shift(dx * back, 0.0, dz * back);
            this.world.wake();
        }
    }

    // Thrown again while down or getting up: carried along with its creature once more, from where it lies now.
    void lift(LivingEntity entity) {
        this.resume();
        this.phase = Phase.AIR;
        this.up = -1;
        this.lay = null;
        this.flew = false;
        this.limp = 1.0;
        System.arraycopy(this.now, this.core * 7, this.target, 0, 3);
        this.offset[0] = this.target[0] - entity.getX();
        this.offset[1] = this.target[1] - entity.getY();
        this.offset[2] = this.target[2] - entity.getZ();
        this.hold.to(this.target[0], this.target[1], this.target[2]);
        this.world.wake();
    }

    // Lain long enough: it gets up from just the way it lies, facing the way it rises (riseYaw).
    void getUp() {
        this.phase = Phase.UP;
        this.up = 0;
        this.lay = this.now.clone();
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        this.riseYaw = rising(this.lay, this.core * 7 + 3, this.entity.yBodyRot);
        this.rise = null;
        this.shownValid = false;
    }

    // The way (the game's degrees) a body lying with its trunk turned by the quaternion at lay[o] faces as it rises:
    // from its front towards where its head lies, from its back towards its feet, from its side the way its chest
    // faces; `own` when it lies no way in particular.
    static float rising(double[] lay, int o, float own) {
        Quaterniond trunk = new Quaterniond(lay[o], lay[o + 1], lay[o + 2], lay[o + 3]);
        Vector3d head = trunk.transform(new Vector3d(0.0, -1.0, 0.0));
        Vector3d chest = trunk.transform(new Vector3d(0.0, 0.0, -1.0));
        double x = chest.y < -0.5 ? head.x : chest.y > 0.5 ? -head.x : chest.x;
        double z = chest.y < -0.5 ? head.z : chest.y > 0.5 ? -head.z : chest.z;
        return x * x + z * z < 0.04 ? own : (float) Math.toDegrees(Math.atan2(-x, z));
    }

    boolean person() {
        return this.person;
    }

    void step(int substeps, Blocks blocks) {
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        if (this.phase == Phase.UP) {
            this.up++;
            this.age++;
            return;
        }
        if (!this.world.sleeping()) {
            this.world.step(TICK, substeps, blocks);
        } else if (this.age % PROBE_EVERY == 0) {
            // Stepped once: kept only if it wakes.
            this.world.probe(TICK, substeps, blocks);
        }
        this.remember(this.now);
        if (this.leapt) {
            System.arraycopy(this.now, 0, this.was, 0, this.now.length);
            this.leapt = false;
        }
        this.age++;
    }

    void remember(double[] into) {
        for (int i = 0; i < this.world.count(); i++) {
            this.world.pose(i, this.scratch);
            System.arraycopy(this.scratch, 0, into, i * 7, 7);
        }
    }

    // Where the trunk's middle is between the last two steps: halfway between its ends when it bends.
    Vec3 coreAt(double partialTick) {
        int a = this.core * 7;
        int b = this.piece(this.core, 2) * 7;
        return new Vec3(middle(a, b, 0, partialTick), middle(a, b, 1, partialTick), middle(a, b, 2, partialTick));
    }

    private double middle(int a, int b, int k, double partialTick) {
        double was = (this.was[a + k] + this.was[b + k]) * 0.5;
        return was + ((this.now[a + k] + this.now[b + k]) * 0.5 - was) * partialTick;
    }

    // Puts every moving part (and the parts that copy it) where its box is this frame, relative to how the model is
    // drawn now, `sink` blocks lower. While a creature takes back its own pose (limp below 1) a part keeps some of it;
    // getting up, it goes from how it lay to its own pose by way of GetUp.
    void pose(Matrix4f drawn, Vec3 camera, double partialTick, double sink, Restore restore) {
        boolean rising = this.phase == Phase.UP && this.lay != null;
        double[] from = rising ? this.lay : this.was;
        double[] to = rising ? this.lay : this.now;
        double t = rising ? 1.0 : partialTick;
        int n = this.parts.size();
        ModelParts.Part trunk = this.parts.get(this.core);
        // The trunk first: a part drawn inside it is placed from where the trunk lies, not from its own pose.
        ModelParts.parentFrame(this.model, drawn, trunk, TRUNK);
        this.lie(this.core, TRUNK, camera, from, to, t, sink);
        ModelPart c = trunk.part();
        TRUNK.translate(LIE.pos[this.core].x / 16.0F, LIE.pos[this.core].y / 16.0F, LIE.pos[this.core].z / 16.0F)
                .rotate(LIE.rot[this.core]).scale(c.xScale, c.yScale, c.zScale);
        this.hanging.trunk.set(c.xScale, c.yScale, c.zScale);
        boolean young = this.model.young;
        if (young) {
            ModelParts.parentFrame(this.model, NONE, trunk, LINK_TRUNK).invert();
        }
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            if (i != this.core) {
                this.lie(i, this.hanging.inside[i] ? TRUNK : ModelParts.parentFrame(this.model, drawn, part, FRAME),
                        camera, from, to, t, sink);
            }
            // A young creature's head is drawn bigger than its body: where it hangs, in the trunk's parent's pixels.
            if (young && !this.hanging.inside[i]) {
                LINK.set(LINK_TRUNK).mul(ModelParts.parentFrame(this.model, NONE, part, FRAME));
                this.hanging.scale[i] = (float) scaleOf(LINK);
                this.hanging.move[i].set(LINK.m30() * 16.0F, LINK.m31() * 16.0F, LINK.m32() * 16.0F);
            } else {
                this.hanging.scale[i] = 1.0F;
                this.hanging.move[i].zero();
            }
            ModelPart target = part.part();
            OWN_POSE.pos[i].set(target.x, target.y, target.z);
            OWN_POSE.rot[i].rotationZYX(target.zRot, target.yRot, target.xRot);
            // A pose that bent it as the creature was drawn (a knee, a waist) is where taking its own pose back ends.
            for (int j = 0; j < BodyPose.JOINTS; j++) {
                Matrix3f drawnBent = j < this.chains[i].length ? BentParts.turn(target, j) : null;
                if (drawnBent == null) {
                    OWN_POSE.joint[j][i].identity();
                } else {
                    drawnBent.getNormalizedRotation(OWN_POSE.joint[j][i]);
                }
            }
        }
        if (rising) {
            if (this.rise == null) {
                this.rise = GetUp.start(this.hanging, this.model, this.parts, this.chains, LIE, this.person);
            }
            float u = Math.min(1.0F, (float) ((this.up + partialTick) / GetUp.ticks(this.person)));
            float turned = (float) Math.toRadians(Mth.wrapDegrees(this.entity.yBodyRot - this.riseYaw));
            this.rise.pose(u, turned, LIE, OWN_POSE, OUT);
        } else {
            float w = (float) Math.max(0.0, Math.min(1.0, this.limp));
            PoseBlend.blend(this.hanging, OWN_POSE, LIE, w, w, OUT);
        }
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            ModelPart target = part.part();
            restore.keep(target);
            MIXED.set(OUT.rot[i].x, OUT.rot[i].y, OUT.rot[i].z, OUT.rot[i].w);
            MIXED.get(ROTATION);
            ROTATION.getEulerAnglesZYX(EULER);
            target.setPos(OUT.pos[i].x, OUT.pos[i].y, OUT.pos[i].z);
            target.setRotation((float) EULER.x, (float) EULER.y, (float) EULER.z);
            List<ModelPart> followers = part.followers();
            for (int k = 0; k < followers.size(); k++) {
                restore.keep(followers.get(k));
                followers.get(k).copyFrom(target);
            }
            ModelBends.Bend[] chain = this.chains[i];
            if (chain.length > 0) {
                // A bent trunk carries what is drawn inside it, all but the parts that move on their own.
                Quaternionf[] turns = chain.length == 1 ? TURNS_1 : TURNS_2;
                for (int j = 0; j < chain.length; j++) {
                    turns[j].set(OUT.joint[j][i]);
                }
                BentParts.bend(target, chain, turns, this.own);
                for (int k = 0; k < followers.size(); k++) {
                    BentParts.bend(followers.get(k), chain, turns, this.own);
                }
            }
        }
        if (rising) {
            this.show(drawn, camera);
        }
    }

    // Where every body is drawn now (the parts just posed), to go limp from there should it be thrown or die.
    private void show(Matrix4f drawn, Vec3 camera) {
        for (int b = 0; b < this.world.count(); b++) {
            ModelParts.Part part = this.parts.get(this.partOf[b]);
            ModelParts.frame(this.model, drawn, part, FRAME);
            if (this.pieceOf[b] > 0) {
                FRAME.scale(1.0F / 16.0F).mul(BentParts.piece(part.part(), this.pieceOf[b], LINK.identity()))
                        .scale(16.0F);
            }
            FRAME.transformPosition((float) this.middle[b * 3], (float) this.middle[b * 3 + 1],
                    (float) this.middle[b * 3 + 2], ORIGIN);
            rotationOf(FRAME, A);
            int o = b * 7;
            this.shown[o] = ORIGIN.x + camera.x;
            this.shown[o + 1] = ORIGIN.y + camera.y;
            this.shown[o + 2] = ORIGIN.z + camera.z;
            this.shown[o + 3] = A.x;
            this.shown[o + 4] = A.y;
            this.shown[o + 5] = A.z;
            this.shown[o + 6] = A.w;
        }
        this.shownValid = true;
    }

    // Where part i lies this frame as a local pose in `parent` (the frame it hangs in): its place and turn in LIE,
    // and the turns at its joints.
    private void lie(int i, Matrix4f parent, Vec3 camera, double[] from, double[] to, double t, double sink) {
        int o = i * 7;
        double px = from[o] + (to[o] - from[o]) * t - camera.x;
        double py = from[o + 1] + (to[o + 1] - from[o + 1]) * t - camera.y - sink;
        double pz = from[o + 2] + (to[o + 2] - from[o + 2]) * t - camera.z;
        A.set(from[o + 3], from[o + 4], from[o + 5], from[o + 6]);
        B.set(to[o + 3], to[o + 4], to[o + 5], to[o + 6]);
        A.slerp(B, t);
        double s = scaleOf(parent);
        rotationOf(parent, PARENT);
        // The part's own turn: its box's turn seen from the part it hangs in.
        OWN.set(PARENT).conjugate().mul(A);
        parent.transformPosition(ORIGIN.set(0.0F, 0.0F, 0.0F));
        LOCAL.set(px - ORIGIN.x, py - ORIGIN.y, pz - ORIGIN.z);
        MIXED.set(PARENT).conjugate().transform(LOCAL);
        LOCAL.div(s);
        MIDDLE.set(this.center[i * 3], this.center[i * 3 + 1], this.center[i * 3 + 2]);
        OWN.transform(MIDDLE);
        LOCAL.sub(MIDDLE);
        LIE.pos[i].set((float) (LOCAL.x * 16.0), (float) (LOCAL.y * 16.0), (float) (LOCAL.z * 16.0));
        LIE.rot[i].set((float) OWN.x, (float) OWN.y, (float) OWN.z, (float) OWN.w);
        // Each piece's turn seen from the piece before it: how far the knee, elbow or waist is bent, then the ankle,
        // wrist or pelvis.
        int before = this.body[i];
        for (int j = 0; j < BodyPose.JOINTS; j++) {
            int piece = j == 0 ? this.lower[i] : this.tip[i];
            if (piece < 0) {
                LIE.joint[j][i].identity();
                continue;
            }
            int a = before * 7;
            int l = piece * 7;
            A.set(from[a + 3], from[a + 4], from[a + 5], from[a + 6]);
            MIXED.set(to[a + 3], to[a + 4], to[a + 5], to[a + 6]);
            A.slerp(MIXED, t);
            B.set(from[l + 3], from[l + 4], from[l + 5], from[l + 6]);
            MIXED.set(to[l + 3], to[l + 4], to[l + 5], to[l + 6]);
            B.slerp(MIXED, t);
            MIXED.set(A).conjugate().mul(B);
            LIE.joint[j][i].set((float) MIXED.x, (float) MIXED.y, (float) MIXED.z, (float) MIXED.w);
            before = piece;
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
