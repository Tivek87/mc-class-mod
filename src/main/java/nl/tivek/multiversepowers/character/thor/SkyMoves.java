package nl.tivek.multiversepowers.character.thor;

import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.spell.SpellTargets;

// What Thor does in the sky besides moving: the shockwave that knocks all round him out of the air, the bolt he calls
// down on what he aims at, the bolts that strike what he passes near at lightning speed, and the strike where he
// lands from it.
final class SkyMoves {
    private static final double SHOCK_RADIUS = 3.5;
    // Everything the shockwave hits drops for this long, its flight gone.
    private static final int STUN = 60;
    private static final double FALL = -0.6;
    private static final double BOLT_REACH = 64.0;
    private static final double NEAR = 5.0;
    private static final int STRIKE_AGAIN = 20;
    private static final double LANDING_RADIUS = 3.5;

    private SkyMoves() {
    }

    // Lightning flows down his arm into the hammer and bursts out round him in a dome.
    static boolean shockwave(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(ThorMoves.GLOW, 1.6F), center, 70, 0.55);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, center, 50, 0.45);
        ParticleFx.sphere(level, ParticleFx.dust(ThorMoves.DEEP, 1.2F), center, SHOCK_RADIUS, 60, 0.0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                1.2F, 1.4F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS,
                1.4F, 0.7F);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(SHOCK_RADIUS), entity -> SpellTargets.hits(player, entity))) {
            if (target.getBoundingBox().getCenter().distanceTo(center) > SHOCK_RADIUS + 0.5) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(player), damage);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 12, 0.3, 0.2);
            ground(level, target);
        }
        return true;
    }

    // Its flight is gone for a while: it drops, and whoever flies as Thor lands.
    private static void ground(ServerLevel level, LivingEntity target) {
        if (target instanceof ServerPlayer other) {
            ThorMoves.land(other, false);
        }
        UUID id = target.getUUID();
        Effects.start(level, (lvl, age) -> {
            if (!(lvl.getEntity(id) instanceof LivingEntity held) || !held.isAlive() || age > STUN) {
                return false;
            }
            Vec3 v = held.getDeltaMovement();
            held.setDeltaMovement(v.x * 0.6, Math.min(v.y, FALL), v.z * 0.6);
            held.hurtMarked = true;
            if (age % 4 == 0) {
                ParticleFx.cloud(lvl, ParticleTypes.ELECTRIC_SPARK, held.getBoundingBox().getCenter(), 3, 0.3, 0.1);
            }
            return !held.onGround() || age < 10;
        });
    }

    // A plain bolt on the creature he aims at; aimed at nothing, nothing happens.
    static boolean bolt(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = Targeting.aimLiving(player, level, BOLT_REACH);
        if (target == null) {
            return false;
        }
        strike(level, player, target, damage);
        return true;
    }

    private static void strike(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        Vec3 at = target.position();
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(player), damage);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 20, 0.4, 0.3);
    }

    // At lightning speed a bolt leaps to each foe he passes near, once a second each.
    static void strikeNear(ServerPlayer player, Map<UUID, Long> struck, float damage) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        struck.values().removeIf(at -> now - at > STRIKE_AGAIN);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(NEAR), entity -> Factions.hostile(player, entity)
                        && Targeting.mayStrike(player, entity) && !struck.containsKey(entity.getUUID()))) {
            struck.put(target.getUUID(), now);
            strike(level, player, target, damage);
        }
    }

    // The bolt he has become comes down where he lands.
    static void landingStrike(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(center.x, center.y, center.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(ThorMoves.GLOW, 1.5F), center.add(0.0, 0.15, 0.0), 40, 0.7);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, center.add(0.0, 0.5, 0.0), 40, 1.2, 0.35);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(LANDING_RADIUS), entity -> SpellTargets.hits(player, entity))) {
            Vec3 away = near.position().subtract(center);
            double far = away.length();
            if (far > LANDING_RADIUS) {
                continue;
            }
            near.invulnerableTime = 0;
            near.hurt(level.damageSources().playerAttack(player), damage * (float) (1.0 - 0.5 * far / LANDING_RADIUS));
            Vec3 way = far < 1.0E-3 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(away.x, 0.0, away.z).normalize();
            SpellTargets.push(near, way, 1.0, 0.5);
        }
    }
}
