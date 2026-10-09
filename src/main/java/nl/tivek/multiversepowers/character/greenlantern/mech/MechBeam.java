package nl.tivek.multiversepowers.character.greenlantern.mech;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// Where the mech's eyes fire from and where their light goes, worked out alike on the server and in every game: from
// the middle of its visor at the spot its pilot's crosshair finds, and on until a block stops it.
public final class MechBeam {
    public static final double EYE_RANGE = 48.0;
    // How far the missile arm aims, and its missiles fly.
    public static final double AIM_RANGE = 64.0;
    public static final double GLARE_RANGE = 40.0;
    public static final double GLARE_RADIUS = 0.9;
    // The middle of its visor, in the torso's places (where its shapes draw it).
    private static final Vec3 VISOR = MechScript.NECK.add(0.0, MechScript.HEAD_UP + 0.05 * MechScript.HEAD_SCALE,
            0.62 * MechScript.HEAD_SCALE);
    private static final double BODY = 0.3;

    private MechBeam() {
    }

    public static Vec3 visor(MechScript.Stage torso) {
        return torso.point(VISOR);
    }

    // What the pilot's crosshair rests on within `range`: a creature, a block, or the end of its reach.
    public static Vec3 aim(Level level, Entity pilot, double range) {
        Vec3 eye = pilot.getEyePosition();
        Vec3 end = block(level, eye, eye.add(pilot.getLookAngle().scale(range)), pilot);
        LivingEntity first = first(level, pilot, eye, end);
        if (first == null) {
            return end;
        }
        return first.getBoundingBox().inflate(BODY).clip(eye, end).orElse(first.getBoundingBox().getCenter());
    }

    // From `from` past `at` on until a block stops it, `range` at most.
    public static Vec3 reach(Level level, Vec3 from, Vec3 at, double range, @Nullable Entity pilot) {
        Vec3 way = at.subtract(from);
        if (way.lengthSqr() < 1.0E-6) {
            return at;
        }
        return block(level, from, from.add(way.normalize().scale(range)), pilot);
    }

    // The first creature, the pilot left out, whose body the line from `from` to `to` passes through.
    @Nullable
    public static LivingEntity first(Level level, @Nullable Entity pilot, Vec3 from, Vec3 to) {
        LivingEntity best = null;
        double bestFar = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(pilot, new AABB(from, to).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3> on = entity.getBoundingBox().inflate(BODY).clip(from, to);
            double far = on.map(from::distanceToSqr).orElse(Double.MAX_VALUE);
            if (far < bestFar) {
                bestFar = far;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    private static Vec3 block(Level level, Vec3 from, Vec3 to, @Nullable Entity pilot) {
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, pilot == null ? CollisionContext.empty() : CollisionContext.of(pilot)));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }
}
