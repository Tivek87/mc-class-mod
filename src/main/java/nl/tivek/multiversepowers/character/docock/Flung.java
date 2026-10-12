package nl.tivek.multiversepowers.character.docock;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.faction.Factions;

// A creature Doctor Octopus's tentacles hurled: it flies on and is hurt where it hits the ground or a wall, or, hurled
// at a creature, it bends its way to it and both are hurt as they meet.
final class Flung implements Effect {
    private static final int LONGEST = 60;
    // Ticks a hurled creature keeps its speed over the ground before the air slows it.
    private static final int CARRIED = 8;
    private static final double STEER = 0.3;

    private final ServerPlayer thrower;
    private final Mob mob;
    @Nullable
    private final LivingEntity at;
    private final float damage;
    private final double speed;
    private final double flat;

    Flung(ServerPlayer thrower, Mob mob, @Nullable LivingEntity at, float damage) {
        this.thrower = thrower;
        this.mob = mob;
        this.at = at;
        this.damage = damage;
        this.speed = mob.getDeltaMovement().length();
        this.flat = mob.getDeltaMovement().horizontalDistance();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (age > LONGEST || !this.mob.isAlive() || this.mob.isRemoved() || this.mob.level() != level) {
            return false;
        }
        if (this.at != null && this.at.isAlive() && !this.at.isRemoved()) {
            Vec3 to = this.at.position().add(0.0, this.at.getBbHeight() * 0.5, 0.0)
                    .subtract(this.mob.position().add(0.0, this.mob.getBbHeight() * 0.5, 0.0));
            if (this.mob.getBoundingBox().inflate(0.3).intersects(this.at.getBoundingBox())) {
                this.hit(level, this.at);
                this.hit(level, this.mob);
                return false;
            }
            if (to.lengthSqr() > 1.0E-6) {
                Vec3 want = to.normalize().scale(this.speed);
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().lerp(want, STEER));
                this.mob.hurtMarked = true;
            }
            return true;
        }
        if (age > 2 && (this.mob.horizontalCollision || this.mob.onGround())) {
            this.hit(level, this.mob);
            return false;
        }
        if (age < CARRIED) {
            // The air slows a flying creature at once: hurled, it keeps its speed over the ground a while first.
            Vec3 motion = this.mob.getDeltaMovement();
            double flat = motion.horizontalDistance();
            if (flat > 1.0E-4 && flat < this.flat) {
                double keep = this.flat / flat;
                this.mob.setDeltaMovement(motion.x * keep, motion.y, motion.z * keep);
                this.mob.hurtMarked = true;
            }
        }
        return true;
    }

    private void hit(ServerLevel level, LivingEntity entity) {
        if (this.damage <= 0.0F || !Factions.mayHit(this.thrower, entity)) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.hurt(this.thrower.damageSources().playerAttack(this.thrower), this.damage);
        entity.setDeltaMovement(motion.scale(0.3));
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
