package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

abstract class WhipHits implements Effect {
    private static final int SEGMENTS = 12;
    private static final int SAMPLES = 4;
    private static final double LASH_WIDE = 0.3;
    // The last part of the lash moves fastest and hurts most.
    private static final double TIP_SHARE = 0.7;
    private static final double TIP_BONUS = 1.3;
    private static final double WHIRL_CLOSE = 0.6;
    private static final double WHIRL_LOW = -1.2;
    private static final double WHIRL_HIGH = 2.8;
    private static final double CHEST = 0.62;

    final ServerPlayer owner;
    final Set<Integer> struck = new HashSet<>();
    int age;
    WhipMove move = WhipMove.EQUIP;
    int moveStart;
    // How long the whirl or spin ran that the current move ends.
    double before;
    int stroke = -1;

    WhipHits(ServerPlayer owner) {
        this.owner = owner;
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    static double length() {
        return wheel().value("whipLength");
    }

    WhipLash.Look look() {
        return WhipLash.Look.of(this.owner.getYRot(), this.owner.getXRot());
    }

    // Roughly where the handle ends: the server knows no arms, so it takes the right hand before the chest.
    Vec3 root(WhipLash.Look look) {
        return look.at(this.owner.getEyePosition(), 0.34, -0.42, 0.32);
    }

    Vec3[] lashAt(WhipLash.Look look, double t) {
        return WhipLash.shape(this.root(look), null, look, length(), at -> this.move.aim(Math.max(0.0, at),
                this.before), t, SEGMENTS, null);
    }

    void lash(ServerLevel level, int t) {
        WhipMove.Strike strike = this.move.strike();
        if (strike == null) {
            return;
        }
        float[][] windows = strike.windows();
        for (int w = 0; w < windows.length; w++) {
            if (t < windows[w][0] || t > windows[w][1]) {
                continue;
            }
            if (w != this.stroke) {
                this.stroke = w;
                this.struck.clear();
                this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.55F, 1.45F + 0.2F * this.owner.getRandom().nextFloat());
            }
            this.sweep(level, t, strike);
        }
        for (WhipMove.Crack crack : this.move.cracks()) {
            if (t - 1 < crack.tick() && crack.tick() <= t) {
                Vec3[] points = this.lashAt(this.look(), crack.tick());
                this.crack(level, points[points.length - 1], crack.strength());
            }
        }
    }

    private void sweep(ServerLevel level, int t, WhipMove.Strike strike) {
        WhipLash.Look look = this.look();
        Vec3[][] shapes = new Vec3[SAMPLES + 1][];
        AABB area = null;
        for (int k = 0; k <= SAMPLES; k++) {
            shapes[k] = this.lashAt(look, t - 1.0 + (double) k / SAMPLES);
            for (Vec3 point : shapes[k]) {
                area = area == null ? new AABB(point, point) : area.minmax(new AABB(point, point));
            }
        }
        if (area == null) {
            return;
        }
        Vec3 eye = this.owner.getEyePosition();
        CharacterAbility wheel = wheel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area.inflate(1.0), this::fair)) {
            if (this.struck.contains(target.getId())) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(LASH_WIDE);
            int where = -1;
            int when = -1;
            for (int k = 0; k <= SAMPLES && where < 0; k++) {
                Vec3[] points = shapes[k];
                for (int i = 0; i + 1 < points.length; i++) {
                    if (touches(box, points[i], points[i + 1])) {
                        where = i + 1;
                        when = k;
                        break;
                    }
                }
                if (where < 0 && k > 0 && touches(box, shapes[k - 1][SEGMENTS], points[SEGMENTS])) {
                    where = SEGMENTS;
                    when = k;
                }
            }
            if (where < 0) {
                continue;
            }
            Vec3 middle = target.getBoundingBox().getCenter();
            if (LoadedWorld.clip(level, new ClipContext(eye, middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    this.owner)).getType() != HitResult.Type.MISS) {
                continue;
            }
            this.struck.add(target.getId());
            boolean cracked = where >= TIP_SHARE * SEGMENTS;
            double damage = this.move == WhipMove.WHIRL_CRACK ? wheel.value("whirlCrackDamage")
                    : wheel.value("whipDamage") * strike.power();
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (cracked ? damage * TIP_BONUS
                    : damage));
            Vec3 tip = shapes[when][SEGMENTS];
            Vec3 was = shapes[Math.max(0, when - 1)][SEGMENTS];
            this.shove(target, strike, tip.subtract(was));
            if (strike.slow() > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, strike.slow(), 1));
            }
            Vec3 at = shapes[when][Math.min(where, SEGMENTS)];
            ParticleFx.cloud(level, cracked ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT, at, cracked ? 6 : 4,
                    0.15, 0.12);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), at, 3, 0.2, 0.03);
            this.sound(cracked ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, 0.9F,
                    cracked ? 1.1F : 1.3F);
            this.sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, cracked ? 1.7F : 1.35F);
        }
    }

    private static boolean touches(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) || box.contains(to) || box.clip(from, to).isPresent();
    }

    private void shove(LivingEntity target, WhipMove.Strike strike, Vec3 swept) {
        Vec3 to = target.position().subtract(this.owner.position());
        Vec3 away = new Vec3(to.x, 0.0, to.z);
        away = away.lengthSqr() < 1.0E-4 ? flat(this.owner.getLookAngle()) : away.normalize();
        Vec3 along = new Vec3(swept.x, 0.0, swept.z);
        along = along.lengthSqr() < 1.0E-6 ? Vec3.ZERO : along.normalize();
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double keep = 1.0 - resist;
        Vec3 push = away.scale(strike.away()).add(along.scale(strike.side())).add(0.0, strike.lift(), 0.0);
        target.setDeltaMovement(target.getDeltaMovement().add(push.scale(keep)));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    void crack(ServerLevel level, Vec3 tip, float strength) {
        level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                0.45F + 0.55F * strength, 1.75F + 0.2F * this.owner.getRandom().nextFloat());
        if (strength > 0.6F) {
            level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6F,
                    1.9F);
        }
        ParticleFx.cloud(level, ParticleFx.dust(0xE4FFEA, 0.8F + 0.6F * strength), tip, 4 + (int) (6 * strength),
                0.12, 0.08);
    }

    // The whirl hits every hostile creature round the owner the spinning lash reaches.
    void whirlHits(ServerLevel level) {
        double reach = length() * 0.98;
        Vec3 at = this.owner.position();
        Vec3 chest = at.add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        double damage = wheel().value("whirlDamage");
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach + 1.0,
                WHIRL_HIGH + 0.5, reach + 1.0), this::hostile)) {
            Vec3 to = target.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat < WHIRL_CLOSE || flat > reach + target.getBbWidth() * 0.5 || to.y < WHIRL_LOW
                    || to.y > WHIRL_HIGH) {
                continue;
            }
            Vec3 middle = target.getBoundingBox().getCenter();
            if (LoadedWorld.clip(level, new ClipContext(chest, middle, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this.owner)).getType() != HitResult.Type.MISS) {
                continue;
            }
            Vec3 away = new Vec3(to.x / flat, 0.0, to.z / flat);
            // The lash turns to the left, so it sweeps what it hits round that way.
            Vec3 round = new Vec3(away.z, 0.0, -away.x);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) damage);
            double keep = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(0.35 * keep))
                    .add(round.scale(0.3 * keep)).add(0.0, 0.12 * keep, 0.0));
            target.hasImpulse = true;
            target.hurtMarked = true;
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), middle, 4, 0.2, 0.05);
        }
    }

    // Shots flying at the spinning lash from the front bounce off it and fly back, now the owner's.
    void deflect(ServerLevel level) {
        Vec3 look = this.owner.getLookAngle();
        Vec3 center = this.owner.getEyePosition().add(look.scale(0.9)).add(0.0, -0.25, 0.0);
        double radius = length() * WhipMove.SPIN_REACH + 0.5;
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(center, center)
                .inflate(radius + 1.5))) {
            Vec3 velocity = shot.getDeltaMovement();
            if (!FlameHits.burnsUp(this.owner, shot) || velocity.dot(look) >= 0.0) {
                continue;
            }
            if (!inDisc(shot.position(), center, look, radius) && !inDisc(shot.position().add(velocity), center, look,
                    radius)) {
                continue;
            }
            Vec3 back = velocity.subtract(look.scale(2.0 * velocity.dot(look))).scale(0.7);
            Vec3 scatter = new Vec3(shot.getRandom().nextGaussian(), shot.getRandom().nextGaussian(),
                    shot.getRandom().nextGaussian()).scale(0.08 * back.length());
            Vec3 out = back.add(scatter);
            shot.deflect((projectile, by, random) -> {
                projectile.setDeltaMovement(out);
                projectile.hasImpulse = true;
            }, this.owner, this.owner, true);
            shot.hurtMarked = true;
            Vec3 spot = shot.position();
            ParticleFx.cloud(level, ParticleTypes.CRIT, spot, 5, 0.1, 0.2);
            ParticleFx.cloud(level, ParticleFx.dust(0xE4FFEA, 0.7F), spot, 3, 0.1, 0.05);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 1.6F);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F,
                    1.9F);
        }
    }

    private static boolean inDisc(Vec3 spot, Vec3 center, Vec3 look, double radius) {
        Vec3 to = spot.subtract(center);
        double along = to.dot(look);
        return along > -0.7 && along < 1.6 && to.subtract(look.scale(along)).lengthSqr() <= radius * radius;
    }

    boolean fair(LivingEntity living) {
        return PowerRing.canHit(this.owner, living)
                && !(living instanceof OwnableEntity pet && pet.getOwner() == this.owner);
    }

    private boolean hostile(LivingEntity living) {
        return this.fair(living) && Factions.hostile(this.owner, living);
    }

    static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    void sound(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(null, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
