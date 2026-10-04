package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.spell.SpellFxPayload;

// The Storm Throw's strike: where the hammer, hurled from the sky, stops, a ring of lightning flashes round it on the
// ground for a moment, and lightning leaps from what it struck to the nearest foe inside the ring, and on from there to
// the next, until every foe in it is struck once (or it has leapt its most).
final class StormThrow {
    private static final int LEAP_EVERY = 2;
    private static final int RING_TICKS = 6;
    private static final int SHOCKED = 30;
    // How far up and down from the ring a foe still stands in it.
    private static final double RING_HEIGHT = 3.0;
    private static final double GROUND_LOOK = 6.0;

    private StormThrow() {
    }

    static void strike(ServerLevel level, ServerPlayer owner, Vec3 at, @Nullable LivingEntity struck) {
        CharacterAbility ability = GameCharacter.THOR.byName("storm_throw");
        double ring = ability == null ? 4.0 : ability.value("ringBlocks");
        int leaps = ability == null ? 10 : (int) Math.round(ability.value("leaps"));
        float damage = ability == null ? 4.0F : (float) ability.value("chainDamage");
        Vec3 ground = ground(level, at);
        flash(level, ground, ring);
        List<LivingEntity> done = new ArrayList<>();
        if (struck != null) {
            done.add(struck);
        }
        Vec3[] from = { struck != null ? struck.getBoundingBox().getCenter() : at };
        UUID id = owner.getUUID();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (age < RING_TICKS) {
                ParticleFx.ring(lvl, ParticleFx.dust(ThorMoves.GLOW, 1.3F), ground.add(0.0, 0.15, 0.0), ring,
                        (int) (ring * 10), age * 0.4);
            }
            if (thor == null || thor.level() != lvl || from[0] == null) {
                return age < RING_TICKS;
            }
            int leap = age / LEAP_EVERY;
            if (age % LEAP_EVERY == 0 && leap < leaps) {
                from[0] = leap(lvl, thor, from[0], ground, ring, done, damage);
            }
            return from[0] != null && leap < leaps || age < RING_TICKS;
        });
    }

    // The ring lies on the ground under where it stopped (or level with it, high above the ground).
    private static Vec3 ground(ServerLevel level, Vec3 at) {
        BlockHitResult floor = LoadedWorld.clip(level, new ClipContext(at, at.add(0.0, -GROUND_LOOK, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return floor.getType() == HitResult.Type.MISS ? at : floor.getLocation();
    }

    // A ring of lightning flashes round it with a crack of thunder.
    private static void flash(ServerLevel level, Vec3 center, double ring) {
        Vec3 low = center.add(0.0, 0.15, 0.0);
        int arcs = Math.max(6, (int) Math.round(ring * 2.5));
        for (int i = 0; i < arcs; i++) {
            double a = Math.PI * 2.0 * i / arcs;
            double b = Math.PI * 2.0 * (i + 1) / arcs;
            Vec3 from = low.add(Math.cos(a) * ring, 0.0, Math.sin(a) * ring);
            Vec3 to = low.add(Math.cos(b) * ring, 0.0, Math.sin(b) * ring);
            ParticleFx.zigzag(level, ParticleFx.dust(ThorMoves.GLOW, 1.2F), from, to, 3, 0.25, 0.25);
        }
        ParticleFx.ring(level, ParticleFx.dust(ThorMoves.DEEP, 1.6F), low, ring, (int) (ring * 12), 0.0);
        ParticleFx.shockwave(level, ParticleFx.dust(ThorMoves.GLOW, 1.4F), low, 36, 0.55);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, center.add(0.0, 0.5, 0.0), 40, ring * 0.4, 0.3);
        ParticleFx.at(level, ParticleTypes.FLASH, center.add(0.0, 0.5, 0.0));
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                1.0F, 1.5F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS,
                1.2F, 1.1F);
    }

    // One leap: on to the nearest foe inside the ring not struck yet; null once there is none left.
    @Nullable
    private static Vec3 leap(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 center, double ring,
            List<LivingEntity> done, float damage) {
        LivingEntity next = Targeting.nextInChain(level, owner, from, ring * 2.0, done,
                entity -> inside(entity, center, ring) && Factions.hostile(owner, entity)
                        && Targeting.mayStrike(owner, entity));
        if (next == null) {
            return null;
        }
        done.add(next);
        Vec3 to = next.getBoundingBox().getCenter();
        SpellFxPayload.send(level, SpellFxPayload.ARC, from, to, -1, 0);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, to, 14, 0.3, 0.2);
        ParticleFx.at(level, ParticleTypes.FLASH, to);
        DeathStyles.mark(next, DeathStyles.Style.ASH);
        next.invulnerableTime = 0;
        next.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT, owner), damage);
        next.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHOCKED, 2), owner);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F,
                1.5F + ParticleFx.RANDOM.nextFloat() * 0.3F);
        return to;
    }

    private static boolean inside(LivingEntity entity, Vec3 center, double ring) {
        Vec3 feet = entity.position();
        double dx = feet.x - center.x;
        double dz = feet.z - center.z;
        return dx * dx + dz * dz <= ring * ring && Math.abs(feet.y - center.y) <= RING_HEIGHT;
    }
}
