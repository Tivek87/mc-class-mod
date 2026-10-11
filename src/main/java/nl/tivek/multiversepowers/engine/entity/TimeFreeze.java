package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.engine.effect.Effects;

// Creatures stopped in time where they are: no AI and no motion, not even falling, until let go or `most` ticks have
// passed. A player moves on his own game, so one is pinned where he stood and slowed to nothing instead.
public final class TimeFreeze {
    private static final Map<LivingEntity, Vec3> FROZEN = new IdentityHashMap<>();
    private static final double DRIFT = 0.01;

    static {
        HeldMobs.addHolder(TimeFreeze::frozen);
    }

    private TimeFreeze() {
    }

    public static boolean frozen(Entity entity) {
        return entity instanceof LivingEntity living && FROZEN.containsKey(living);
    }

    public static boolean freeze(ServerLevel level, LivingEntity living, int most) {
        if (FROZEN.containsKey(living) || !living.isAlive() || living.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        if (living instanceof Mob mob) {
            if (HeldMobs.isHeldByAnyone(mob) || !HeldMobs.hold(mob, false)) {
                return false;
            }
        } else if (!(living instanceof ServerPlayer)) {
            return false;
        }
        FROZEN.put(living, living.position());
        Effects.start(level, (lvl, age) -> {
            if (!FROZEN.containsKey(living)) {
                return false;
            }
            if (age >= most || !living.isAlive() || living.isRemoved() || living.level() != lvl) {
                thaw(living);
                return false;
            }
            hold(living);
            return true;
        });
        return true;
    }

    private static void hold(LivingEntity living) {
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        if (living instanceof ServerPlayer player) {
            Vec3 at = FROZEN.get(living);
            if (player.position().distanceToSqr(at) > DRIFT) {
                player.teleportTo(at.x, at.y, at.z);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 9, false, false));
            player.hurtMarked = true;
        }
    }

    public static void thaw(LivingEntity living) {
        if (FROZEN.remove(living) != null && living instanceof Mob mob) {
            HeldMobs.release(mob);
        }
    }

    public static void thawAll() {
        for (LivingEntity living : new ArrayList<>(FROZEN.keySet())) {
            thaw(living);
        }
    }
}
