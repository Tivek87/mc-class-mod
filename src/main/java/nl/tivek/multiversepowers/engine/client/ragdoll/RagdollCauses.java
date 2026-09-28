package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Matrix4f;

// What throws a creature limp and how hard: a blow that sends it flying, or a blast near it.
final class RagdollCauses {
    // How fast a blow must send a creature to throw it limp, in blocks per tick: along the ground, and upward.
    private static final double THROWN = 0.9;
    private static final double TOSSED = 0.6;
    // A creature bigger than this (width squared times height) is too heavy for a blow to throw it limp.
    private static final double HEAVY = 3.5;
    // A blast pushing a creature this hard (blocks a tick, as the game counts a blast's push) sends it flying limp.
    private static final double BLOWN = 0.25;
    // How long a blast is remembered: a creature it kills goes limp a tick or two later and is thrown then.
    private static final int BLAST_TICKS = 3;
    // A blast's middle is often still in the blocks it breaks: what shields a body is looked for from this far out.
    private static final double BLAST_CORE = 0.8;
    private static final double FASTEST = 40.0;

    private static final Int2IntOpenHashMap BLOWN_AT = new Int2IntOpenHashMap();
    private static final List<Blast> BLASTS = new ArrayList<>();

    private RagdollCauses() {
    }

    private record Blast(Vec3 center, double power, int tick) {
    }

    // Remembers a blast at tick `now` and marks the creatures it pushes hard enough to fly limp (`free`: not limp yet).
    static void blast(ClientLevel level, Vec3 center, double power, int now, IntPredicate free) {
        BLASTS.add(new Blast(center, power, now));
        double reach = power * 2.0;
        for (Entity entity : level.getEntities((Entity) null, new AABB(center, center).inflate(reach),
                entity -> entity instanceof Mob)) {
            Mob mob = (Mob) entity;
            Vec3 middle = mob.getBoundingBox().getCenter();
            double near = 1.0 - middle.distanceTo(center) / reach;
            if (mob.isAlive() && free.test(mob.getId()) && near * seen(level, center, middle) >= BLOWN
                    && mayFly(mob)) {
                BLOWN_AT.put(mob.getId(), now);
            }
        }
    }

    // How much of a blast reaches what stands at `to`: nothing past a wall, a little round its corner.
    static double seen(ClientLevel level, Vec3 center, Vec3 to) {
        Vec3 way = to.subtract(center);
        double far = way.length();
        if (far <= BLAST_CORE) {
            return 1.0;
        }
        Vec3 from = center.add(way.scale(BLAST_CORE / far));
        int clear = 0;
        for (int k = -1; k <= 1; k++) {
            Vec3 aim = to.add(0.0, k * 0.4, 0.0);
            if (level.clip(new ClipContext(from, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty())).getType() == HitResult.Type.MISS) {
                clear++;
            }
        }
        return clear == 0 ? 0.1 : clear / 3.0;
    }

    static boolean blown(int entity, int now) {
        return BLOWN_AT.containsKey(entity) && now - BLOWN_AT.get(entity) <= BLAST_TICKS;
    }

    // Whether a blast just went off near enough to throw this creature by itself.
    static boolean nearBlast(Entity entity) {
        for (Blast blast : BLASTS) {
            if (entity.getBoundingBox().getCenter().distanceTo(blast.center()) < blast.power() * 2.0) {
                return true;
            }
        }
        return false;
    }

    // A creature a blast just hit is thrown by the blast itself (by its weight), not by the push the game gave it.
    static void throwByBlasts(Ragdoll doll, ClientLevel level, RandomSource random) {
        for (Blast blast : BLASTS) {
            doll.blast(blast.center(), blast.power(), seen(level, blast.center(), doll.coreAt(1.0)), random);
        }
    }

    static void forget(int now) {
        BLASTS.removeIf(blast -> now - blast.tick() > BLAST_TICKS);
        BLOWN_AT.int2IntEntrySet().removeIf(entry -> now - entry.getIntValue() > BLAST_TICKS);
    }

    static void clear() {
        BLASTS.clear();
        BLOWN_AT.clear();
    }

    // Only a creature drawn at its true size and shape can go limp: not one squashed or stretched by some effect.
    static boolean rigid(Matrix4f m) {
        float a = m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02();
        float b = m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12();
        float c = m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22();
        if (a < 1.0E-6F || Math.abs(a - b) > 0.1F * a || Math.abs(a - c) > 0.1F * a) {
            return false;
        }
        float ab = m.m00() * m.m10() + m.m01() * m.m11() + m.m02() * m.m12();
        float ac = m.m00() * m.m20() + m.m01() * m.m21() + m.m02() * m.m22();
        float bc = m.m10() * m.m20() + m.m11() * m.m21() + m.m12() * m.m22();
        return Math.abs(ab) < 0.05F * a && Math.abs(ac) < 0.05F * a && Math.abs(bc) < 0.05F * a
                && m.determinant3x3() > 0.0F;
    }

    static Vec3 velocity(LivingEntity entity) {
        Vec3 moved = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        Vec3 push = entity.getDeltaMovement();
        Vec3 velocity = (push.lengthSqr() > moved.lengthSqr() ? push : moved).scale(20.0);
        double speed = velocity.length();
        return speed > FASTEST ? velocity.scale(FASTEST / speed) : velocity;
    }

    // A creature a blow or a blast sends flying: just hurt and given a hard push (the server tells its speed at once,
    // while where it is only follows a few ticks later), not one that flies by itself or is too heavy.
    static boolean thrown(Mob mob) {
        if (mob.hurtTime <= 0) {
            return false;
        }
        Vec3 push = mob.getDeltaMovement();
        if (push.horizontalDistanceSqr() <= THROWN * THROWN && push.y <= TOSSED) {
            return false;
        }
        return mayFly(mob);
    }

    // Not one that flies by itself, rides or is ridden, swims, or is too heavy.
    private static boolean mayFly(Mob mob) {
        return !mob.isNoGravity() && !mob.isPassenger() && !mob.isVehicle() && !mob.isInWater() && !mob.isInLava()
                && !(mob instanceof FlyingMob) && !(mob instanceof FlyingAnimal) && !(mob instanceof Bat)
                && mob.getBbWidth() * mob.getBbWidth() * mob.getBbHeight() <= HEAVY;
    }

    // Whether something solid is right under the creature. A client only learns a creature's own onGround() when it
    // moves, so one that stood still since it came into view would never seem to stand.
    static boolean grounded(Entity entity) {
        return !entity.level().noCollision(entity, entity.getBoundingBox().move(0.0, -0.06, 0.0));
    }
}
