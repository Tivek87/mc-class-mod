package nl.tivek.multiversepowers.engine.client.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

// A held blade swung into a wall or a raised shield stops against it instead of passing through: the blade turns about
// its grip only as far as the way is clear (back towards where it last was clear, or up), and where it struck is told
// for sparks. Only what is drawn; the blow itself is the server's.
public final class BladeStop {
    // Of the blade, the part from the grip out to here is the hilt and the hand, which may touch.
    private static final double HILT = 0.3;
    private static final int REFINE = 7;
    private static final double SHIELD_AHEAD = 0.55;
    private static final double SHIELD_DOWN = 0.45;
    private static final Vec3 SHIELD_HALF = new Vec3(0.45, 0.6, 0.12);
    private static final int FORGET = 100;

    // The way to draw the blade, and the point it struck this frame (null when it swings free).
    public record Stop(Vec3 blade, @Nullable Vec3 struck) {
    }

    private static final class Memory {
        Vec3 clear;
        int seen;
    }

    private static final Int2ObjectOpenHashMap<Memory> MEMORY = new Int2ObjectOpenHashMap<>();

    private BladeStop() {
    }

    // `owner` holds the blade at `grip`, pointing it along the unit `wanted`, `length` blocks long; `tick` counts
    // frames or ticks, only to forget blades no longer drawn.
    public static Stop stop(Level level, LivingEntity owner, Vec3 grip, Vec3 wanted, double length, int tick) {
        Memory memory = MEMORY.get(owner.getId());
        if (memory == null) {
            memory = new Memory();
            MEMORY.put(owner.getId(), memory);
        }
        memory.seen = tick;
        if (tick % 200 == 0) {
            MEMORY.values().removeIf(old -> tick - old.seen > FORGET);
        }
        Vec3 struck = struck(level, owner, grip, wanted, length);
        if (struck == null) {
            memory.clear = wanted;
            return new Stop(wanted, null);
        }
        Vec3[] ways = memory.clear == null ? new Vec3[] { new Vec3(0.0, 1.0, 0.0) }
                : new Vec3[] { memory.clear, new Vec3(0.0, 1.0, 0.0) };
        for (Vec3 back : ways) {
            if (struck(level, owner, grip, back, length) != null) {
                continue;
            }
            double low = 0.0;
            double high = 1.0;
            for (int i = 0; i < REFINE; i++) {
                double mid = (low + high) * 0.5;
                if (struck(level, owner, grip, turn(wanted, back, mid), length) == null) {
                    high = mid;
                } else {
                    low = mid;
                }
            }
            Vec3 blade = turn(wanted, back, high);
            memory.clear = blade;
            return new Stop(blade, struck(level, owner, grip, turn(wanted, back, low), length));
        }
        return new Stop(wanted, struck);
    }

    // The unit way `u` of the turn from `from` to `to` (0 is `from`), along the shorter arc.
    private static Vec3 turn(Vec3 from, Vec3 to, double u) {
        Vec3 mixed = from.scale(1.0 - u).add(to.scale(u));
        double length = mixed.length();
        return length < 1.0E-6 ? to : mixed.scale(1.0 / length);
    }

    // Where the blade pointing along `way` first meets a solid block or someone else's raised shield; null if nowhere.
    @Nullable
    private static Vec3 struck(Level level, LivingEntity owner, Vec3 grip, Vec3 way, double length) {
        Vec3 from = grip.add(way.scale(length * HILT));
        Vec3 to = grip.add(way.scale(length));
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        Vec3 best = hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
        double nearest = best == null ? Double.POSITIVE_INFINITY : best.distanceToSqr(from);
        AABB around = new AABB(grip, grip).inflate(length + 1.0);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, around,
                living -> living != owner && living.isBlocking())) {
            Vec3 at = shield(other, from, to);
            if (at != null && at.distanceToSqr(from) < nearest) {
                nearest = at.distanceToSqr(from);
                best = at;
            }
        }
        return best;
    }

    // Where the segment enters the shield someone raises before them: a board at chest height a little in front.
    @Nullable
    private static Vec3 shield(LivingEntity holder, Vec3 from, Vec3 to) {
        Vec3 look = holder.getViewVector(1.0F);
        Vec3 ahead = new Vec3(look.x, 0.0, look.z);
        ahead = ahead.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        Vec3 center = holder.getEyePosition().subtract(0.0, SHIELD_DOWN, 0.0).add(ahead.scale(SHIELD_AHEAD));
        double[] a = local(from.subtract(center), right, ahead);
        double[] b = local(to.subtract(center), right, ahead);
        double enter = 0.0;
        double leave = 1.0;
        double[] half = { SHIELD_HALF.x, SHIELD_HALF.y, SHIELD_HALF.z };
        for (int axis = 0; axis < 3; axis++) {
            double d = b[axis] - a[axis];
            if (Math.abs(d) < 1.0E-9) {
                if (Math.abs(a[axis]) > half[axis]) {
                    return null;
                }
                continue;
            }
            double t0 = (-half[axis] - a[axis]) / d;
            double t1 = (half[axis] - a[axis]) / d;
            enter = Math.max(enter, Math.min(t0, t1));
            leave = Math.min(leave, Math.max(t0, t1));
            if (enter > leave) {
                return null;
            }
        }
        return from.lerp(to, enter);
    }

    private static double[] local(Vec3 v, Vec3 right, Vec3 ahead) {
        return new double[] { v.dot(right), v.y, v.dot(ahead) };
    }
}
