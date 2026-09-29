package nl.tivek.multiversepowers.engine.client.ragdoll;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// How a dead body goes down: the blow that killed it pushes it hardest where it struck, so it folds and falls away
// from it; one that dies with no word of a blow tips over to a side as its limbs give way; and one that comes to rest
// still standing, kneeling or sitting up on its limbs gives way again until it lies.
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

    // After every step of a dead body: it tips over when no blow came in time, counts how long it has lain, and gives
    // way again when it would come to rest not lying.
    static void settle(Ragdoll doll, int now) {
        if (doll.toppleAt >= 0 && now >= doll.toppleAt) {
            doll.toppleAt = -1;
            if (!doll.struck && !doll.blasted) {
                topple(doll);
            }
        }
        if (doll.slumped()) {
            doll.rested++;
        } else if (doll.toppleAt < 0 && doll.slumps < MOST_SLUMPS && doll.world.quiet() >= QUIET_BEFORE_SLUMP) {
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
        double hard = Mth.clamp(push.horizontalDistance() * 20.0 * BLOW_SHARE, LEAST_BLOW, MOST_BLOW);
        double lift = Mth.clamp(push.y * 20.0 * BLOW_LIFT, 0.0, hard * 0.6);
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
