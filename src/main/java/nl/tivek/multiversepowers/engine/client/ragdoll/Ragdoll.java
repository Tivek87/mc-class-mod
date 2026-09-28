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
import nl.tivek.multiversepowers.engine.physics.Blocks;
import nl.tivek.multiversepowers.engine.physics.Pin;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
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
    private static final Vector3f[] LIE_POS = GetUp.vectors();
    private static final Quaternionf[] LIE_ROT = GetUp.turns();
    private static final Quaternionf[] LIE_KNEE = GetUp.turns();
    private static final Vector3f[] OWN_POS = GetUp.vectors();
    private static final Quaternionf[] OWN_ROT = GetUp.turns();
    private static final Vector3f[] OUT_POS = GetUp.vectors();
    private static final Quaternionf[] OUT_ROT = GetUp.turns();
    private static final Quaternionf[] OUT_KNEE = GetUp.turns();

    final LivingEntity entity;
    final EntityModel<?> model;
    final List<ModelParts.Part> parts;
    final RigidWorld world = new RigidWorld();
    // Each part's body; a long arm or leg is two, its near half in `body` and its far half past the knee in `lower`.
    final int[] body;
    final int[] lower;
    private final ModelParts.Bend[] bends;
    private final double[] center;
    // Every part it moves, the parts that copy them too, and where each sits in the copies layers draw.
    private final ModelPart[] moved;
    private final Map<EntityModel<?>, ModelPart[]> copies = new IdentityHashMap<>();
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
    // Ticks lying still, ticks down in all, and ticks into getting up (-1 before).
    int rest;
    int down;
    int up = -1;
    // The body as it lay when it began to get up; a person gets up by way of one knee (GetUp).
    @Nullable
    private double[] lay;
    private final boolean person;
    private boolean leapt;
    final double[] was;
    final double[] now;
    private final double[] scratch = new double[7];

    Ragdoll(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, int[] body,
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
        this.person = GetUp.person(model, parts);
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
        this.phase = Phase.DOWN;
        this.lay = null;
        this.up = -1;
        this.world.wake();
    }

    // It has come down: no longer held to where its creature is, it falls and lies where the blow left it.
    void fall() {
        this.phase = Phase.DOWN;
        this.hold.release();
        this.rest = 0;
        this.down = 0;
        this.world.wake();
    }

    // Thrown again while down or getting up: carried along with its creature once more, from where it lies now.
    void lift(LivingEntity entity) {
        if (this.phase == Phase.UP && this.lay != null) {
            System.arraycopy(this.lay, 0, this.now, 0, this.now.length);
            System.arraycopy(this.lay, 0, this.was, 0, this.now.length);
        }
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

    // Lain long enough: it gets up from just the way it lies.
    void getUp() {
        this.phase = Phase.UP;
        this.up = 0;
        this.lay = this.now.clone();
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
    }

    boolean person() {
        return this.person;
    }

    // Lying all but still: a last twitch of a limb does not keep it down longer.
    boolean resting() {
        return this.world.quiet() >= 3;
    }

    void step(int substeps, Blocks blocks) {
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        if (this.phase == Phase.UP) {
            this.up++;
            this.age++;
            return;
        }
        this.world.step(TICK, substeps, blocks);
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

    // Where the core is between the last two steps.
    Vec3 coreAt(double partialTick) {
        int o = this.core * 7;
        return new Vec3(this.was[o] + (this.now[o] - this.was[o]) * partialTick,
                this.was[o + 1] + (this.now[o + 1] - this.was[o + 1]) * partialTick,
                this.was[o + 2] + (this.now[o + 2] - this.was[o + 2]) * partialTick);
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
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            int o = i * 7;
            double px = from[o] + (to[o] - from[o]) * t - camera.x;
            double py = from[o + 1] + (to[o + 1] - from[o + 1]) * t - camera.y - sink;
            double pz = from[o + 2] + (to[o + 2] - from[o + 2]) * t - camera.z;
            A.set(from[o + 3], from[o + 4], from[o + 5], from[o + 6]);
            B.set(to[o + 3], to[o + 4], to[o + 5], to[o + 6]);
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
            LIE_POS[i].set((float) (LOCAL.x * 16.0), (float) (LOCAL.y * 16.0), (float) (LOCAL.z * 16.0));
            LIE_ROT[i].set((float) OWN.x, (float) OWN.y, (float) OWN.z, (float) OWN.w);
            LIE_KNEE[i].identity();
            if (this.lower[i] >= 0) {
                // The far half's turn seen from the near half: how far the knee or elbow is bent.
                int l = this.lower[i] * 7;
                B.set(from[l + 3], from[l + 4], from[l + 5], from[l + 6]);
                MIXED.set(to[l + 3], to[l + 4], to[l + 5], to[l + 6]);
                B.slerp(MIXED, t);
                MIXED.set(A).conjugate().mul(B);
                LIE_KNEE[i].set((float) MIXED.x, (float) MIXED.y, (float) MIXED.z, (float) MIXED.w);
            }
            ModelPart target = part.part();
            OWN_POS[i].set(target.x, target.y, target.z);
            OWN_ROT[i].rotationZYX(target.zRot, target.yRot, target.xRot);
        }
        if (rising && this.person) {
            float u = Math.min(1.0F, (float) ((this.up + partialTick) / GetUp.PERSON_TICKS));
            GetUp.person(this.parts, this.core, this.bends, u, LIE_POS, LIE_ROT, LIE_KNEE, OWN_POS, OWN_ROT, OUT_POS,
                    OUT_ROT, OUT_KNEE);
        } else {
            float w = rising ? GetUp.limp((float) Math.min(1.0, (this.up + partialTick) / GetUp.OTHER_TICKS))
                    : (float) Math.max(0.0, Math.min(1.0, this.limp));
            for (int i = 0; i < n; i++) {
                OUT_POS[i].set(OWN_POS[i]).lerp(LIE_POS[i], w);
                OUT_ROT[i].set(OWN_ROT[i]).slerp(LIE_ROT[i], w);
                OUT_KNEE[i].identity().slerp(LIE_KNEE[i], w);
            }
        }
        for (int i = 0; i < n; i++) {
            ModelParts.Part part = this.parts.get(i);
            ModelPart target = part.part();
            restore.keep(target);
            MIXED.set(OUT_ROT[i].x, OUT_ROT[i].y, OUT_ROT[i].z, OUT_ROT[i].w);
            MIXED.get(ROTATION);
            ROTATION.getEulerAnglesZYX(EULER);
            target.setPos(OUT_POS[i].x, OUT_POS[i].y, OUT_POS[i].z);
            target.setRotation((float) EULER.x, (float) EULER.y, (float) EULER.z);
            List<ModelPart> followers = part.followers();
            for (int k = 0; k < followers.size(); k++) {
                restore.keep(followers.get(k));
                followers.get(k).copyFrom(target);
            }
            if (this.lower[i] >= 0) {
                BentParts.bend(target, this.bends[i], OUT_KNEE[i]);
                for (int k = 0; k < followers.size(); k++) {
                    BentParts.bend(followers.get(k), this.bends[i], OUT_KNEE[i]);
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
