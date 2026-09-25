package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

abstract class FlameHits implements Effect {
    private static final double CHEST = 0.62;
    private static final double TALL = 1.8;
    private static final double CLOSE = 1.0;
    private static final double SWEEP_SLACK = 8.0;
    private static final double STREAM_NEAR = 0.35;
    private static final double STREAM_FAR = 1.45;
    private static final double VORTEX_TALL = 3.4;
    private static final double FLYING = 0.25;

    final ServerPlayer owner;
    final Set<Integer> swept = new HashSet<>();
    int age;
    FlameMove move = FlameMove.EQUIP;
    int moveStart;

    FlameHits(ServerPlayer owner) {
        this.owner = owner;
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    void sweep(ServerLevel level, int t) {
        CharacterAbility wheel = wheel();
        double a = FlameMove.sweepYaw(this.move, t - 2.0);
        double b = FlameMove.sweepYaw(this.move, t);
        double low = Math.min(a, b) - SWEEP_SLACK;
        double high = Math.max(a, b) + SWEEP_SLACK;
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 look = flat(this.owner.getLookAngle());
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        double reach = wheel.value("sweepReach");
        double side = this.move == FlameMove.SWEEP_BACK ? 1.0 : -1.0;
        int struck = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin)
                .inflate(reach + 1.5), this::fair)) {
            if (this.swept.contains(target.getId())) {
                continue;
            }
            Vec3 middle = target.getBoundingBox().getCenter();
            Vec3 to = middle.subtract(origin);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z) - target.getBbWidth() * 0.5;
            if (flat > reach || Math.abs(to.y) > TALL + target.getBbHeight() * 0.5) {
                continue;
            }
            double angle = Math.toDegrees(Math.atan2(to.dot(right), to.dot(look)));
            if (flat > CLOSE && (angle < low || angle > high)) {
                continue;
            }
            if (this.blocked(level, origin, middle)) {
                continue;
            }
            this.swept.add(target.getId());
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) wheel.value("sweepDamage"));
            Vec3 away = new Vec3(to.x, 0.0, to.z);
            away = away.lengthSqr() < 1.0E-4 ? look : away.normalize();
            shove(target, away.scale(0.6).add(right.scale(side * 0.5)), 0.45, 0.12);
            FlameBurn.ignite(level, target);
            ParticleFx.cloud(level, ParticleFx.fade(0xE4FFEA, PowerRing.GREEN, 1.2F), middle, 8, 0.3, 0.06);
            struck++;
        }
        if (struck > 0) {
            this.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.1F);
            this.sound(SoundEvents.FIRECHARGE_USE, 0.5F, 1.6F);
        }
    }

    void pour(ServerLevel level) {
        CharacterAbility wheel = wheel();
        Vec3 eye = this.owner.getEyePosition();
        Vec3 look = this.owner.getLookAngle();
        double range = wheel.value("infernoRange");
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(eye, eye.add(look.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.owner));
        double reach = wall.getType() == HitResult.Type.MISS ? range : eye.distanceTo(wall.getLocation());
        Vec3 end = eye.add(look.scale(reach));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end)
                .inflate(STREAM_FAR + 1.0), this::fair)) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double along = middle.subtract(eye).dot(look);
            if (along < 0.3 || along > reach + target.getBbWidth()) {
                continue;
            }
            double radius = Mth.lerp(Mth.clamp(along / range, 0.0, 1.0), STREAM_NEAR, STREAM_FAR);
            Vec3 closest = eye.add(look.scale(along));
            double off = middle.subtract(closest).length() - Math.max(target.getBbWidth(), target.getBbHeight())
                    * 0.5;
            if (off > radius) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) wheel.value("infernoDamage"));
            shove(target, look, 0.16, 0.02);
            FlameBurn.ignite(level, target);
            ParticleFx.cloud(level, ParticleFx.fade(0xE4FFEA, PowerRing.GREEN, 1.0F), middle, 3, 0.3, 0.04);
        }
        if (wall.getType() != HitResult.Type.MISS) {
            ParticleFx.cloud(level, ParticleFx.fade(0xE4FFEA, PowerRing.GREEN, 1.3F), wall.getLocation(), 4, 0.35,
                    0.05);
        }
    }

    void swirl(ServerLevel level, boolean blast) {
        CharacterAbility wheel = wheel();
        double radius = wheel.value("vortexRadius") * (blast ? 1.35 : 1.0);
        Vec3 at = this.owner.position();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius
                + 1.0, VORTEX_TALL, radius + 1.0), this::hostile)) {
            Vec3 to = target.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > radius + target.getBbWidth() * 0.5 || to.y < -1.5 || to.y > VORTEX_TALL) {
                continue;
            }
            Vec3 away = flat < 1.0E-3 ? flat(this.owner.getLookAngle()) : new Vec3(to.x / flat, 0.0, to.z / flat);
            Vec3 round = new Vec3(-away.z, 0.0, away.x);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner),
                    (float) (wheel.value("vortexDamage") * (blast ? 1.5 : 1.0)));
            shove(target, blast ? away : away.scale(0.7).add(round.scale(0.6)), blast ? 1.1 : 0.45, blast ? 0.4
                    : 0.2);
            FlameBurn.ignite(level, target);
        }
    }

    void scorch(ServerLevel level) {
        double radius = wheel().value("vortexRadius") + 0.6;
        Vec3 at = this.owner.position();
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(radius,
                VORTEX_TALL * 0.5, radius).move(0.0, VORTEX_TALL * 0.5, 0.0))) {
            if (!burnsUp(this.owner, shot)) {
                continue;
            }
            Vec3 spot = shot.position();
            Vec3 next = spot.add(shot.getDeltaMovement());
            if (spot.subtract(at).horizontalDistance() > radius && next.subtract(at).horizontalDistance() > radius) {
                continue;
            }
            burnUp(level, shot);
        }
    }

    // Only what hostiles shoot, or what nobody shot, burns up, and only while it flies: arrows in the ground stay.
    static boolean burnsUp(ServerPlayer owner, Projectile shot) {
        Entity shooter = shot.getOwner();
        return shooter != owner && (shooter == null || Factions.hostile(owner, shooter))
                && shot.getDeltaMovement().lengthSqr() > FLYING * FLYING;
    }

    // A trident is someone's weapon: the fire stops it dead instead of burning it up.
    static void burnUp(ServerLevel level, Projectile shot) {
        Vec3 spot = shot.position();
        if (shot instanceof ThrownTrident) {
            shot.setDeltaMovement(Vec3.ZERO);
            shot.hurtMarked = true;
        } else {
            shot.discard();
        }
        ParticleFx.cloud(level, ParticleFx.dust(0xD8FFE2, 1.0F), spot, 6, 0.15, 0.05);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private boolean blocked(ServerLevel level, Vec3 from, Vec3 to) {
        return LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this.owner)).getType() != HitResult.Type.MISS;
    }

    private static void shove(LivingEntity target, Vec3 way, double strength, double lift) {
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double keep = 1.0 - resist;
        target.setDeltaMovement(target.getDeltaMovement().add(way.scale(strength * keep)).add(0.0, lift * keep,
                0.0));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private boolean fair(LivingEntity living) {
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
