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
    private static final double CLOSE = 1.0;
    private static final int SAMPLES = 8;
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

    // Everything the flame passed over in the last two ticks: the aim swept from one way to the next, and the fire
    // reaches a little beside and above it, more for a big target.
    void spray(ServerLevel level, FlameMove.Stroke stroke, int t) {
        CharacterAbility wheel = wheel();
        FlameMove.Fire fire = this.move.fire();
        double was = Mth.clamp(t - 2.0, stroke.from(), stroke.to());
        double now = Mth.clamp((double) t, stroke.from(), stroke.to());
        Vec3 look = this.owner.getLookAngle();
        Vec3[] ways = new Vec3[SAMPLES];
        for (int s = 0; s < SAMPLES; s++) {
            ways[s] = FlameMove.way(look, stroke.aim(Mth.lerp(s / (SAMPLES - 1.0), was, now)));
        }
        double turning = Math.signum(stroke.aim(now).yaw() - stroke.aim(was).yaw());
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 ahead = flat(look);
        double reach = wheel.value("sweepReach") * this.move.reach();
        int struck = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin)
                .inflate(reach + 1.5), this::fair)) {
            if (this.swept.contains(target.getId())) {
                continue;
            }
            Vec3 middle = target.getBoundingBox().getCenter();
            Vec3 to = middle.subtract(origin);
            double far = to.length();
            double flat = Math.sqrt(to.x * to.x + to.z * to.z) - target.getBbWidth() * 0.5;
            if (far - target.getBbWidth() * 0.5 > reach) {
                continue;
            }
            if (flat > CLOSE && !inFlame(ways, to, fire, target, far)) {
                continue;
            }
            if (this.blocked(level, origin, middle)) {
                continue;
            }
            this.swept.add(target.getId());
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner),
                    (float) (wheel.value("sweepDamage") * this.move.power()));
            Vec3 away = new Vec3(to.x, 0.0, to.z);
            away = away.lengthSqr() < 1.0E-4 ? ahead : away.normalize();
            Vec3 along = new Vec3(-away.z, 0.0, away.x).scale(turning);
            shove(target, away.scale(this.move.away()).add(along.scale(this.move.side())), 1.0, this.move.lift());
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

    // Within the fire round one of the ways the aim took: an oval, wider for a bigger or nearer target.
    private static boolean inFlame(Vec3[] ways, Vec3 to, FlameMove.Fire fire, LivingEntity target, double far) {
        double wide = fire.across + Math.toDegrees(Math.atan2(target.getBbWidth() * 0.5, Math.max(0.5, far)));
        double tall = fire.high + Math.toDegrees(Math.atan2(target.getBbHeight() * 0.5, Math.max(0.5, far)));
        Vec3 dir = to.normalize();
        for (Vec3 way : ways) {
            Vec3 aim = way.normalize();
            double ahead = dir.dot(aim);
            if (ahead <= 0.0) {
                continue;
            }
            Vec3 side = aim.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = side.cross(aim);
            double across = Math.toDegrees(Math.atan2(dir.dot(side), ahead)) / wide;
            double high = Math.toDegrees(Math.atan2(dir.dot(up), ahead)) / tall;
            if (across * across + high * high <= 1.0) {
                return true;
            }
        }
        return false;
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
