package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The one creature the mech builds itself over: held on its spot from the first stomp to the end of the build.
final class MechTarget {
    private static final double CONE = Math.cos(Math.toRadians(65.0));
    private static final double OFF_AIM = 6.0;

    @Nullable
    private LivingEntity creature;
    private boolean holding;

    MechTarget(@Nullable LivingEntity creature) {
        this.creature = creature;
        if (creature instanceof Mob mob) {
            this.holding = HeldMobs.hold(mob);
        }
    }

    // The nearest creature out to hurt the owner in front of them, weighed against how far off their aim it stands.
    @Nullable
    static LivingEntity pick(ServerPlayer owner, ServerLevel level, double reach) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        flatLook = flatLook.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flatLook.normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(reach),
                living -> GiantHands.fair(owner, living) && !HeldMobs.isHeldByAnyone(living)
                        && !living.getType().is(Tags.EntityTypes.BOSSES))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            Vec3 to = middle.subtract(eye);
            double far = to.length();
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (far > reach || flat.lengthSqr() < 1.0E-6) {
                continue;
            }
            double aim = flat.normalize().dot(flatLook);
            if (aim < CONE || LoadedWorld.clip(level, new ClipContext(eye, middle, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty())).getType() != HitResult.Type.MISS) {
                continue;
            }
            double score = far + OFF_AIM * (1.0 - aim);
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }

    @Nullable
    LivingEntity creature() {
        return this.creature != null && this.creature.isAlive() && !this.creature.isRemoved() ? this.creature : null;
    }

    void place(Vec3 at) {
        LivingEntity creature = this.creature();
        if (creature == null) {
            this.release();
            return;
        }
        creature.setDeltaMovement(Vec3.ZERO);
        creature.resetFallDistance();
        if (creature instanceof ServerPlayer player) {
            // A server that does not allow flying must not think he hangs in the air on his own.
            player.teleportTo(at.x, at.y, at.z);
            player.connection.aboveGroundTickCount = 0;
        } else {
            creature.setPos(at.x, at.y, at.z);
        }
    }

    // A playerAttack knocks back and hops by itself; the held creature stays where the mech has it.
    void hit(ServerLevel level, ServerPlayer owner, double damage) {
        LivingEntity creature = this.creature();
        if (creature == null || damage <= 0.0) {
            return;
        }
        creature.invulnerableTime = 0;
        creature.hurt(level.damageSources().playerAttack(owner), (float) damage);
        creature.setDeltaMovement(Vec3.ZERO);
        creature.hurtMarked = true;
    }

    void release() {
        if (this.holding && this.creature instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.holding = false;
        this.creature = null;
    }
}
