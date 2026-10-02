package nl.tivek.multiversepowers.engine.client.ragdoll;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.world.Solid;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;
import org.joml.Quaterniond;
import org.joml.Vector3d;

// How a body goes down: the blow that killed it pushes it hardest where it struck, so it folds and falls away from
// it; one that dies with no word of a blow tips over to a side as its limbs give way; and one that comes to rest still
// standing, kneeling or sitting up on its limbs gives way again until it lies. A person knocked back (dead, or thrown
// alive) against a wall right behind it slams its back into it and slides down it into a slump, sitting against it.
final class RagdollFalls {
    // Tipping over, a body turns this fast (radians per second), a stiff one as one piece; its limbs give way this fast.
    private static final double TOPPLE = 2.0;
    private static final double TOPPLE_STIFF = 2.4;
    private static final double GIVE_WAY = 1.5;
    // Knocked away harder than this (blocks per second) as it dies, it falls the way it was knocked.
    private static final double CALM = 2.0;
    // With no word yet of the blow that killed it, a body waits this many ticks for one before it tips over by itself.
    private static final int WAIT_FOR_BLOW = 2;
    // A blow pushes the parts where it struck at this share of its push (blocks per second), never less than LEAST
    // nor more than MOST, the parts furthest from there at a quarter of that; it lifts at this share of its lift.
    private static final double BLOW_SHARE = 0.85;
    private static final double LEAST_BLOW = 3.5;
    private static final double MOST_BLOW = 22.0;
    private static final double BLOW_LIFT = 0.5;
    private static final double NEAREST = 1.3;
    private static final double FURTHEST = 0.25;
    // How hard its limbs flail, as a share of the blow; a push along the ground slower than NO_WAY (blocks a tick)
    // tells no way to fall.
    private static final double FLAIL = 0.3;
    private static final double NO_WAY = 0.03;
    // Nearly still for this many steps and still not lying, it gives way again, at most MOST_SLUMPS times.
    private static final int QUIET_BEFORE_SLUMP = 4;
    private static final int MOST_SLUMPS = 6;
    // A wall counts from its back out to this far (blocks), at its chest and at its hips, looked for in WALL_LOOKS
    // steps. Its back slams into it this fast (blocks a second) at the most and the least. Then, for SLIDE_TICKS, its
    // chest is pressed to it at PRESS, the hips slide out from it at HIPS_OUT of how fast they come down, the trunk
    // loses HOLD_SIDE of its speed along the wall every tick, and once its feet are down its trunk and head come down
    // no faster than SLIDE; a throw does not lift it again for SLAM_TICKS.
    private static final double WALL_GAP = 0.55;
    private static final int WALL_LOOKS = 5;
    private static final double MOST_SLAM = 6.0;
    private static final double LEAST_SLAM = 2.5;
    private static final double PRESS = 0.8;
    private static final double SLIDE = 0.8;
    private static final double HIPS_OUT = 0.5;
    private static final double HOLD_SIDE = 0.8;
    private static final int SLIDE_TICKS = 60;
    private static final int SLAM_TICKS = 12;
    private static final RandomSource RANDOM = RandomSource.create();

    private RagdollFalls() {
    }

    // It has just gone limp dead, drawn facing `yaw` (the game's degrees), every part given `velocity` (its creature's
    // own push); `blow`: what the server told of the blow that killed it, if that came first.
    static void die(Ragdoll doll, float yaw, Vec3 velocity, boolean stiff, int now,
            @Nullable RagdollCauses.Blow blow) {
        doll.stiff = stiff;
        doll.built = velocity;
        doll.toppleYaw = Math.toRadians(yaw);
        if (doll.blasted || blow != null && strike(doll, blow)) {
            return;
        }
        if (velocity.horizontalDistance() < CALM) {
            doll.toppleAt = now + WAIT_FOR_BLOW;
            return;
        }
        if (!stiff) {
            // Knocked away: it folds the way it goes.
            double length = velocity.horizontalDistance();
            giveWay(doll, RANDOM, velocity.z / length, -velocity.x / length, GIVE_WAY);
        }
    }

    // After every step of a dead body: it tips over when no blow came in time, counts how long it has lain wholly
    // still (back to 0 whenever it moves), and gives way again when it would come to rest not lying.
    static void settle(Ragdoll doll, int now) {
        slide(doll);
        if (doll.toppleAt >= 0 && now >= doll.toppleAt) {
            doll.toppleAt = -1;
            if (!doll.struck && !doll.blasted) {
                topple(doll);
            }
        }
        doll.rested = doll.world.sleeping() ? doll.rested + 1 : 0;
        if (!doll.slumped() && doll.toppleAt < 0 && doll.slumps < MOST_SLUMPS
                && doll.world.quiet() >= QUIET_BEFORE_SLUMP) {
            doll.slumps++;
            collapse(doll);
        }
    }

    // The blow that killed it, from `from` (null: from nowhere in particular) pushing `push` (blocks a tick): the
    // parts at the height it struck go hardest and the feet least, so the body folds and falls away from it instead
    // of flying off as one piece. What every part was given as it went limp is taken back first. False when the blow
    // tells no way to fall.
    static boolean strike(Ragdoll doll, RagdollCauses.Blow blow) {
        if (doll.struck || doll.blasted) {
            return false;
        }
        RigidWorld world = doll.world;
        Vec3 core = doll.coreAt(1.0);
        Vec3 push = blow.push();
        double fx = push.x;
        double fz = push.z;
        if (fx * fx + fz * fz < NO_WAY * NO_WAY && blow.from() != null) {
            fx = core.x - blow.from().x;
            fz = core.z - blow.from().z;
        }
        double flat = Math.sqrt(fx * fx + fz * fz);
        if (flat < 1.0E-4) {
            return false;
        }
        fx /= flat;
        fz /= flat;
        double force = ClientSettings.ragdollForce();
        double hard = Mth.clamp(push.horizontalDistance() * 20.0 * BLOW_SHARE, LEAST_BLOW, MOST_BLOW) * force;
        double lift = Mth.clamp(push.y * 20.0 * BLOW_LIFT, 0.0, hard * 0.6);
        if (slump(doll, fx, fz, hard)) {
            doll.built = Vec3.ZERO;
            doll.struck = true;
            doll.toppleAt = -1;
            return true;
        }
        double[] at = new double[7];
        double[] v = new double[6];
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int b = 0; b < world.count(); b++) {
            world.pose(b, at);
            double reach = Math.max(world.half(b, 0), Math.max(world.half(b, 1), world.half(b, 2)));
            low = Math.min(low, at[1] - reach);
            high = Math.max(high, at[1] + reach);
        }
        double tall = Math.max(0.3, high - low);
        // A blow from someone strikes at the height it came from, a blow from nowhere at the chest.
        double hit = blow.from() != null ? Mth.clamp(blow.from().y, low + 0.35 * tall, high) : low + 0.65 * tall;
        Vec3 was = doll.built;
        for (int b = 0; b < world.count(); b++) {
            world.pose(b, at);
            world.velocity(b, v);
            double near = Mth.clamp(NEAREST - Math.abs(at[1] - hit) / tall * 1.6, FURTHEST, NEAREST);
            double flail = b == doll.body[doll.core] ? 0.0 : hard * FLAIL;
            world.velocity(b, v[0] - was.x + fx * hard * near, v[1] - was.y + lift * near,
                    v[2] - was.z + fz * hard * near, v[3] + RANDOM.nextGaussian() * flail,
                    v[4] + RANDOM.nextGaussian() * flail, v[5] + RANDOM.nextGaussian() * flail);
        }
        doll.built = Vec3.ZERO;
        doll.struck = true;
        doll.toppleAt = -1;
        if (!doll.stiff) {
            giveWay(doll, RANDOM, fz, -fx, GIVE_WAY);
        }
        world.wake();
        return true;
    }

    // Whether a creature thrown alive, still upright, has its back to a wall right behind it the way it is thrown: then
    // it slams into the wall and slumps down it.
    static boolean pinned(Ragdoll doll) {
        double flat = Math.sqrt(doll.wayX * doll.wayX + doll.wayZ * doll.wayZ);
        return flat > 1.0E-3 && slump(doll, doll.wayX / flat, doll.wayZ / flat, LEAST_SLAM);
    }

    // Whether the body, a person upright with its back to a wall just behind it (the way (fx, fz) it is knocked),
    // slams into the wall `hard` (blocks a second) and slumps down it; if so, it is set going.
    static boolean slump(Ragdoll doll, double fx, double fz, double hard) {
        if (!doll.person() || !ClientSettings.wallSlump() || !(doll.entity.level() instanceof Level level)) {
            return false;
        }
        RigidWorld world = doll.world;
        int chest = doll.body[doll.core];
        int hips = doll.piece(doll.core, 2);
        double[] top = new double[7];
        double[] low = new double[7];
        world.pose(chest, top);
        world.pose(hips, low);
        double dx = top[0] - low[0];
        double dy = top[1] - low[1];
        double dz = top[2] - low[2];
        double tall = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vector3d front = new Quaterniond(top[3], top[4], top[5], top[6]).transform(new Vector3d(0.0, 0.0, -1.0));
        if (tall < 1.0E-3 || dy < 0.75 * tall || front.x * fx + front.z * fz > -0.2) {
            return false;
        }
        double back = doll.entity.getBbWidth() * 0.5;
        if (!wall(level, top, fx, fz, back) || !wall(level, low, fx, fz, back)) {
            return false;
        }
        double slam = Mth.clamp(hard, LEAST_SLAM, MOST_SLAM);
        for (int b = 0; b < world.count(); b++) {
            int part = doll.partOf[b];
            ModelParts.Role role = doll.parts.get(part).role();
            double share = b == chest ? 1.0 : b == hips ? 0.5 : part == doll.core ? 0.75 : part == doll.head ? 1.1
                    : role == ModelParts.Role.ARM ? 0.8 : role == ModelParts.Role.LEG ? 0.1 : 0.6;
            double flail = role == ModelParts.Role.ARM ? slam * FLAIL : 0.0;
            world.velocity(b, fx * slam * share, 0.0, fz * slam * share, RANDOM.nextGaussian() * flail,
                    RANDOM.nextGaussian() * flail, RANDOM.nextGaussian() * flail);
        }
        doll.slumpX = fx;
        doll.slumpZ = fz;
        doll.slumpAge = 0;
        return true;
    }

    // Whether it slammed into a wall these last SLAM_TICKS: the blow that did it no longer throws it.
    static boolean slamming(Ragdoll doll) {
        return doll.slumpAge >= 0 && doll.slumpAge < SLAM_TICKS;
    }

    // Whether a block stands within WALL_GAP of a back `back` blocks behind the middle `at`, the way (fx, fz).
    private static boolean wall(Level level, double[] at, double fx, double fz, double back) {
        for (int k = 0; k < WALL_LOOKS; k++) {
            double far = back + 0.05 + (WALL_GAP - 0.05) * k / (WALL_LOOKS - 1);
            if (Solid.at(level, at[0] + fx * far, at[1], at[2] + fz * far)) {
                return true;
            }
        }
        return false;
    }

    // A tick of a slump: the chest pressed to the wall and the hips sliding out from it as they come down; once its
    // feet are on the ground the trunk and head come down no faster than SLIDE. The trunk is held from sliding or
    // rolling along the wall, so it sits against it instead of toppling over sideways.
    static void slide(Ragdoll doll) {
        if (doll.slumpAge < 0) {
            return;
        }
        if (++doll.slumpAge > SLIDE_TICKS) {
            doll.slumpAge = -1;
            return;
        }
        RigidWorld world = doll.world;
        int chest = doll.body[doll.core];
        int hips = doll.piece(doll.core, 2);
        boolean standing = false;
        for (int i = 0; i < doll.parts.size() && !standing; i++) {
            standing = doll.parts.get(i).role() == ModelParts.Role.LEG && world.touching(doll.piece(i, 2));
        }
        double fx = doll.slumpX;
        double fz = doll.slumpZ;
        double[] v = new double[6];
        for (int b = 0; b < world.count(); b++) {
            int part = doll.partOf[b];
            if (part != doll.core && part != doll.head) {
                continue;
            }
            world.velocity(b, v);
            double down = standing ? Math.max(v[1], -SLIDE) : v[1];
            double along = v[0] * fx + v[2] * fz;
            double want = b == chest ? PRESS : b == hips ? -HIPS_OUT * Math.max(0.0, -down) : along;
            double side = part == doll.core ? (v[2] * fx - v[0] * fz) * HOLD_SIDE : 0.0;
            double roll = part == doll.core ? (v[3] * fx + v[5] * fz) * HOLD_SIDE : 0.0;
            world.velocity(b, v[0] + fx * (want - along) + fz * side, down, v[2] + fz * (want - along) - fx * side,
                    v[3] - fx * roll, v[4], v[5] - fz * roll);
        }
    }

    // Died with no blow to go by: it tips over sideways about the line it faces along, its limbs giving way.
    private static void topple(Ragdoll doll) {
        double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
        double ax = -Math.sin(doll.toppleYaw) * side;
        double az = Math.cos(doll.toppleYaw) * side;
        double spin = doll.stiff ? TOPPLE_STIFF : TOPPLE;
        tip(doll, ax * spin, az * spin, feet(doll));
        if (!doll.stiff) {
            giveWay(doll, RANDOM, ax, az, GIVE_WAY);
        }
    }

    // Come to rest with its trunk and head off the ground: it tips the way it leans over what it stands on (any way
    // when it stands straight) and its limbs give way again.
    private static void collapse(Ragdoll doll) {
        RigidWorld world = doll.world;
        double[] at = new double[7];
        double sx = 0.0;
        double sz = 0.0;
        int standing = 0;
        for (int b = 0; b < world.count(); b++) {
            if (world.touching(b)) {
                world.pose(b, at);
                sx += at[0];
                sz += at[2];
                standing++;
            }
        }
        if (standing == 0) {
            return;
        }
        Vec3 core = doll.coreAt(1.0);
        double dx = core.x - sx / standing;
        double dz = core.z - sz / standing;
        double lean = Math.sqrt(dx * dx + dz * dz);
        if (lean < 0.05) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
            lean = 1.0;
        }
        double ax = dz / lean;
        double az = -dx / lean;
        tip(doll, ax * TOPPLE, az * TOPPLE, feet(doll));
        if (!doll.stiff) {
            giveWay(doll, RANDOM, ax, az, GIVE_WAY);
        }
        world.wake();
    }

    // Under its middle, at the height of its lowest part.
    private static Vec3 feet(Ragdoll doll) {
        RigidWorld world = doll.world;
        double[] at = new double[7];
        double low = Double.POSITIVE_INFINITY;
        for (int b = 0; b < world.count(); b++) {
            world.pose(b, at);
            low = Math.min(low, at[1] - world.half(b, 1));
        }
        Vec3 core = doll.coreAt(1.0);
        return new Vec3(core.x, low, core.z);
    }

    // Sets every part turning by (wx, 0, wz) radians per second about a line through `pivot`, as one rigid body would.
    static void tip(Ragdoll doll, double wx, double wz, Vec3 pivot) {
        RigidWorld world = doll.world;
        double[] v = new double[6];
        double[] at = new double[7];
        for (int i = 0; i < world.count(); i++) {
            world.velocity(i, v);
            world.pose(i, at);
            double rx = at[0] - pivot.x;
            double ry = at[1] - pivot.y;
            double rz = at[2] - pivot.z;
            world.velocity(i, v[0] - wz * ry, v[1] + wz * rx - wx * rz, v[2] + wx * ry, v[3] + wx, v[4], v[5] + wz);
        }
    }

    // Its limbs give way, folding the way the body falls (about the level line (ax, az)), each a little differently:
    // legs standing straight under a body would hold it up like a table's.
    static void giveWay(Ragdoll doll, RandomSource random, double ax, double az, double speed) {
        RigidWorld world = doll.world;
        double[] v = new double[6];
        for (int i = 0; i < world.count(); i++) {
            if (i == doll.body[doll.core]) {
                continue;
            }
            double along = speed * (0.7 + 0.6 * random.nextDouble());
            double across = speed * 0.25 * random.nextGaussian();
            world.velocity(i, v);
            world.velocity(i, v[0], v[1], v[2], v[3] + ax * along - az * across, v[4],
                    v[5] + az * along + ax * across);
        }
    }
}
