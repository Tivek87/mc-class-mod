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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.fx.Thuds;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.Hanging;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import nl.tivek.multiversepowers.engine.physics.joint.Pin;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import org.joml.Quaterniond;
import org.joml.Vector3d;

// A creature gone limp: one box per part of its model, joined where the parts turn, falling, bumping into blocks and
// hanging from whatever holds it; carried along by its creature, thrown, lying, getting up (Ragdoll draws it).
abstract class RagdollBody {
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
    private static final int THUD_EVERY = 5;
    private static final int SETTLING = 30;

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
    final ModelBends.Bend[][] chains;
    final int[] hang;
    final double[] center;
    // Every part it moves, the parts that copy them too, and where each sits in the copies layers draw.
    final ModelPart[] moved;
    final Set<ModelPart> own = Collections.newSetFromMap(new IdentityHashMap<>());
    final Map<EntityModel<?>, ModelPart[]> copies = new IdentityHashMap<>();
    // Which piece of the trunk each part hangs from, and whether it is drawn inside the trunk.
    final Hanging hanging;
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
    // Thrown, the way it was last seen flying along the ground; slumping down a wall, the way into it and ticks into
    // the slide (-1 when not).
    double wayX;
    double wayZ;
    double slumpX;
    double slumpZ;
    int slumpAge = -1;
    // The head's part, -1 without one.
    final int head;
    // The body as it lay when it began to get up, the way it faces as it rises, and how it gets up (GetUp: a person by
    // way of its hands and one knee), planned the first time it is drawn getting up.
    @Nullable
    double[] lay;
    float riseYaw;
    @Nullable
    GetUp.Rise rise;
    final boolean person;
    private boolean leapt;
    // How hard a part hit a block this step (blocks a second), and the last thud: when, and how hard.
    private double hit;
    private int thudAt = Integer.MIN_VALUE / 2;
    private double thudSpeed;
    final double[] was;
    final double[] now;
    // Each body's part and piece of it, and its box's middle in that part's own frame (blocks): where it is drawn
    // getting up (`shown`, the last time it was), to go limp from there.
    final int[] partOf;
    final int[] pieceOf;
    final double[] middle;
    final double[] shown;
    boolean shownValid;
    private final double[] scratch = new double[7];

    RagdollBody(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, ModelBends.Bend[][] chains,
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
        double push = BLAST * middle * seen * face / mass * ClientSettings.ragdollForce();
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
        this.slumpAge = -1;
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
        this.slumpAge = -1;
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
        this.hit = 0.0;
        if (this.phase == Phase.UP) {
            this.up++;
            this.age++;
            return;
        }
        if (!this.world.sleeping()) {
            this.world.step(TICK, substeps, blocks);
            this.hit = this.world.impact();
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

    // The step's hardest hit on a block thuds and throws up the block's dust, a limb softer than the trunk at the same
    // speed (by the root of their weights). A body still tumbling thuds again only after THUD_EVERY ticks unless it
    // hits harder, and while it settles only as hard as half its last thud.
    void thud(int tick, RandomSource random) {
        int part = this.world.impactBody();
        if (this.hit <= 0.0 || part < 0 || !(this.entity.level() instanceof ClientLevel level)) {
            return;
        }
        double hard = this.hit * Math.sqrt(this.world.mass(part) / this.world.mass(this.body[this.core]));
        int since = tick - this.thudAt;
        if (hard < Thuds.SOFT || since < THUD_EVERY && hard < this.thudSpeed * 1.5
                || since < SETTLING && hard < this.thudSpeed * 0.5) {
            return;
        }
        this.thudAt = tick;
        this.thudSpeed = hard;
        double[] at = new double[6];
        this.world.impact(at);
        Thuds.hit(level, at[0], at[1], at[2], at[3], at[4], at[5], hard, this.entity.getSoundSource(), random);
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
}
