package nl.tivek.multiversepowers.character.thor;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.spell.SpellTargets;

// The grab-dash dive: in flight Thor dives at what he aims at, grabs it on the way and drives it into the ground,
// where the slam ends his flight. Aimed at nothing, he dives at the ground he looks at and slams there.
final class GrabDive {
    private static final double REACH = 32.0;
    private static final double GRAB = 2.2;
    private static final int LONGEST = 80;
    private static final double SLAM_RADIUS = 4.5;

    private final ThorMoves moves;
    private final float damage;
    @Nullable
    private LivingEntity target;
    private boolean carried;
    private boolean done;
    private int age;

    private GrabDive(ThorMoves moves, @Nullable LivingEntity target, float damage) {
        this.moves = moves;
        this.target = target;
        this.damage = damage;
    }

    static boolean start(ServerPlayer player, float damage) {
        ThorMoves moves = ThorMoves.find(player);
        if (moves == null || !ThorMoves.flying(player) || moves.dive != null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        LivingEntity target = Targeting.aimLiving(player, level, REACH);
        GrabDive dive = new GrabDive(moves, target, damage);
        moves.dive = dive;
        moves.sync(ThorStatePayload.DIVE, target == null ? 0 : target.getId() + 1);
        moves.sound(level, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0F, 0.9F);
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4F, 1.9F);
        Effects.start(level, (lvl, age) -> dive.tick(lvl));
        return true;
    }

    boolean carrying() {
        return this.carried;
    }

    private boolean tick(ServerLevel level) {
        if (this.done) {
            return false;
        }
        ServerPlayer owner = this.moves.owner();
        if (++this.age > LONGEST || !owner.isAlive() || owner.level() != level) {
            this.end();
            return false;
        }
        LivingEntity held = this.target;
        if (held != null && (!held.isAlive() || held.level() != level)) {
            this.let(held);
            this.target = null;
            held = null;
        }
        if (held == null) {
            return true;
        }
        if (!this.carried) {
            if (owner.getBoundingBox().getCenter().distanceTo(held.getBoundingBox().getCenter()) < GRAB) {
                this.grab(level, owner, held);
            }
            return true;
        }
        Vec3 at = hand(owner, held);
        held.setPos(at.x, at.y, at.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.resetFallDistance();
        return true;
    }

    // Where a grabbed creature hangs: from his right hand, a little ahead of him.
    static Vec3 hand(LivingEntity thor, LivingEntity held) {
        Vec3 look = Vec3.directionFromRotation(0.0F, thor.getYRot());
        Vec3 right = look.cross(Vectors.UP).normalize();
        double size = thor.getScale();
        return thor.position().add(look.scale(0.7 * size)).add(right.scale(0.45 * size))
                .add(0.0, 0.5 * size - held.getBbHeight() * 0.55, 0.0);
    }

    private void grab(ServerLevel level, ServerPlayer owner, LivingEntity held) {
        if (held instanceof Mob mob && HeldMobs.hold(mob)) {
            this.carried = true;
        } else {
            held.invulnerableTime = 0;
            held.hurt(level.damageSources().playerAttack(owner), this.damage * 0.5F);
            this.target = null;
        }
        Vec3 at = held.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 18, 0.4, 0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
        this.moves.sync(ThorStatePayload.NONE, 0);
    }

    // His game says he hit the ground in the dive: the slam, and his flight is over.
    void slam(ServerLevel level) {
        if (this.done) {
            return;
        }
        ServerPlayer owner = this.moves.owner();
        Vec3 center = owner.position();
        Set<LivingEntity> hit = new HashSet<>();
        LivingEntity held = this.carried ? this.target : null;
        if (held != null) {
            this.let(held);
            held.invulnerableTime = 0;
            held.hurt(level.damageSources().playerAttack(owner), this.damage);
            SpellTargets.push(held, Vec3.directionFromRotation(0.0F, owner.getYRot()), 0.9, 0.55);
            hit.add(held);
        }
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center)
                .inflate(SLAM_RADIUS), entity -> Targeting.isTargetable(owner, entity) && !hit.contains(entity))) {
            Vec3 away = near.position().subtract(center);
            double far = away.length();
            if (far > SLAM_RADIUS) {
                continue;
            }
            near.invulnerableTime = 0;
            near.hurt(level.damageSources().playerAttack(owner), this.damage * (float) (0.6 - 0.3 * far
                    / SLAM_RADIUS));
            Vec3 way = far < 1.0E-3 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(away.x, 0.0, away.z).normalize();
            SpellTargets.push(near, way, 1.2 * (1.0 - 0.5 * far / SLAM_RADIUS), 0.5);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(center.x, center.y, center.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(ThorMoves.GLOW, 1.6F), center.add(0.0, 0.15, 0.0), 40, 0.7);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, center.add(0.0, 0.2, 0.0), 24, 0.35);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, center.add(0.0, 0.5, 0.0), 40, 1.2, 0.35);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS,
                1.4F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                1.2F, 1.0F);
        this.moves.sync(ThorStatePayload.SLAM, 0);
        this.end();
        ThorMoves.land(owner, false);
    }

    private void let(LivingEntity held) {
        if (this.carried && held instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.carried = false;
    }

    void end() {
        if (this.done) {
            return;
        }
        this.done = true;
        if (this.target != null) {
            this.let(this.target);
        }
        if (this.moves.dive == this) {
            this.moves.dive = null;
            this.moves.sync(ThorStatePayload.NONE, 0);
        }
    }
}
