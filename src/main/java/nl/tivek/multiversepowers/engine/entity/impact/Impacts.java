package nl.tivek.multiversepowers.engine.entity.impact;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;

// Where a blow lands on a creature and which way it goes. A power that knows marks it just before it hurts (at);
// else it is worked out from what struck: a shot where it is, a blow along its striker's look, a blast from its
// middle. A fall, fire or hunger lands nowhere. At the end of the tick, once the game and the power have pushed the
// creature, one that lives takes it (Staggers); one the blow killed keeps it for its body (DeathBlows).
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Impacts {
    // How far a striker's look may reach for the box it hits, in blocks.
    private static final double LOOK = 8.0;

    private static final Map<LivingEntity, Mark> MARKED = new IdentityHashMap<>();
    private static final Map<LivingEntity, Impact> STRUCK = new IdentityHashMap<>();
    // How hard the game's knockback pushed each creature struck this tick (blocks a tick), as it was given: by the
    // tick's end friction has eaten half of it.
    private static final Map<LivingEntity, Double> SHOVED = new IdentityHashMap<>();
    // What of the game's knockback each creature struck this tick did not get (blocks a tick): its hop, or all of it
    // for a blow to the legs. A power that throws it gives it back (Staggers).
    private static final Map<LivingEntity, Vec3> LOST = new IdentityHashMap<>();
    // The game's knockback lifts a creature on the ground by at most this (blocks a tick).
    private static final double HOP = 0.4;

    // Where on the creature the blow landed and the way it went (a unit vector), and what share of its whole health
    // it took.
    public record Impact(Vec3 at, Vec3 way, float share) {
    }

    private record Mark(Vec3 at, @Nullable Vec3 way) {
    }

    static {
        Effects.atTickEnd(Impacts::send);
    }

    private Impacts() {
    }

    // The blow about to hurt `victim` lands at `at`, going `way` (null: from whoever strikes it).
    public static void at(LivingEntity victim, Vec3 at, @Nullable Vec3 way) {
        MARKED.put(victim, new Mark(at, way));
    }

    // How the last blow this tick struck it, if it landed anywhere.
    @Nullable
    public static Impact of(LivingEntity victim) {
        return STRUCK.get(victim);
    }

    // How hard the game's knockback pushed it this tick, as it was given (0 when it did not).
    static double shoved(LivingEntity victim) {
        return SHOVED.getOrDefault(victim, 0.0);
    }

    // What of the game's knockback it did not get this tick (blocks a tick).
    static Vec3 lost(LivingEntity victim) {
        return LOST.getOrDefault(victim, Vec3.ZERO);
    }

    // A blow's knockback, as the game gives it. A creature that takes blows on its feet (Staggers) is shoved back
    // along the ground instead of hopping up into the air: a blow does not lift it, it reels. A blow to its legs does
    // not shove it at all: it sweeps them. What it did not get is kept for a power that throws it.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !STRUCK.containsKey(victim)) {
            return;
        }
        double strength = event.getStrength() * (1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        SHOVED.merge(victim, Math.max(0.0, strength), Math::max);
        if (!(victim instanceof Mob mob) || !mob.onGround() || !PowerRules.staggers() || !Knockdowns.falls(mob)
                || Knockdowns.isDown(mob) || HeldMobs.isHeld(mob) || strength <= 0.0) {
            return;
        }
        Vec3 away = new Vec3(event.getRatioX(), 0.0, event.getRatioZ());
        if (away.lengthSqr() < 1.0E-5) {
            return;
        }
        event.setCanceled(true);
        away = away.normalize().scale(strength);
        Vec3 was = mob.getDeltaMovement();
        // The push the game gives, on top of what it lost this tick already, and the push it gets instead.
        Vec3 game = was.add(lost(mob));
        Vec3 given = new Vec3(game.x / 2.0 - away.x, Math.min(HOP, game.y / 2.0 + strength), game.z / 2.0 - away.z);
        Vec3 now = was;
        AABB box = mob.getBoundingBox();
        if ((STRUCK.get(victim).at().y - box.minY) / Math.max(0.1, box.getYsize()) >= Staggers.LEGS) {
            now = new Vec3(was.x / 2.0 - away.x, was.y, was.z / 2.0 - away.z);
            mob.hasImpulse = true;
            mob.setDeltaMovement(now);
        }
        LOST.put(mob, given.subtract(now));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Mark mark = MARKED.remove(victim);
        if (victim.level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
        }
        Impact impact = locate(victim, event.getSource(), mark,
                event.getNewDamage() / Math.max(1.0F, victim.getMaxHealth()));
        Impact had = STRUCK.get(victim);
        if (impact != null && (had == null || had.share() <= impact.share())) {
            STRUCK.put(victim, impact);
        }
    }

    @Nullable
    private static Impact locate(LivingEntity victim, DamageSource source, @Nullable Mark mark, float share) {
        AABB box = victim.getBoundingBox();
        Entity direct = source.getDirectEntity();
        Entity striker = source.getEntity();
        Vec3 middle = box.getCenter();
        Vec3 at;
        Vec3 way;
        if (mark != null) {
            at = inside(mark.at(), box);
            Entity from = striker != null ? striker : direct;
            way = mark.way() != null ? mark.way() : from != null ? at.subtract(from.getEyePosition()) : Vec3.ZERO;
        } else if (direct instanceof Projectile shot) {
            at = inside(shot.getBoundingBox().getCenter(), box);
            way = shot.getDeltaMovement().lengthSqr() > 1.0E-6 ? shot.getDeltaMovement() : middle.subtract(at);
        } else if (direct != null) {
            Vec3 eye = direct.getEyePosition();
            Optional<Vec3> hit = box.clip(eye, eye.add(direct.getLookAngle().scale(LOOK)));
            at = inside(hit.orElse(eye), box);
            way = at.subtract(eye);
        } else if (source.getSourcePosition() != null) {
            at = inside(source.getSourcePosition(), box);
            way = middle.subtract(source.getSourcePosition());
        } else {
            return null;
        }
        if (way.lengthSqr() < 1.0E-8) {
            way = middle.subtract(at);
        }
        if (way.lengthSqr() < 1.0E-8) {
            way = Vec3.directionFromRotation(0.0F, victim.getYRot()).reverse();
        }
        return new Impact(at, way.normalize(), Mth.clamp(share, 0.0F, 1.0F));
    }

    // The nearest point to p in the box.
    private static Vec3 inside(Vec3 p, AABB box) {
        return new Vec3(Mth.clamp(p.x, box.minX, box.maxX), Mth.clamp(p.y, box.minY, box.maxY),
                Mth.clamp(p.z, box.minZ, box.maxZ));
    }

    private static void send() {
        if (!STRUCK.isEmpty()) {
            for (Map.Entry<LivingEntity, Impact> struck : STRUCK.entrySet()) {
                if (struck.getKey() instanceof Mob mob && mob.isAlive() && !mob.isRemoved()
                        && mob.level() instanceof ServerLevel level) {
                    Staggers.struck(level, mob, struck.getValue());
                }
            }
            STRUCK.clear();
        }
        SHOVED.clear();
        LOST.clear();
        MARKED.clear();
    }

    public static void clear() {
        STRUCK.clear();
        SHOVED.clear();
        LOST.clear();
        MARKED.clear();
    }
}
