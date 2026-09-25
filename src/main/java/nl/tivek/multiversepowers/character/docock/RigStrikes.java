package nl.tivek.multiversepowers.character.docock;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

/**
 * The tentacles that hit: Tentacle Strike, Multi-Tentacle and Octopus Rampage, and whom they pick
 * to hit.
 */
abstract class RigStrikes extends RigPoses {
    RigStrikes(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    // ---- Tentacle Strike and Multi-Tentacle ----

    /**
     * A melee hit: a free tentacle lashes out and the hit lands when its claw arrives.
     *
     * @return false when this hit is not taken over (it is then a normal hit)
     */
    boolean meleeStrike(LivingEntity target) {
        if (this.delivering || this.folding || this.unfold < 1.0 || this.climbing) {
            return false;
        }
        // Upper tentacles in turns; the legs join in only when both are busy.
        int[] order = this.nextArm++ % 2 == 0 ? new int[] { 0, 1, 2, 3 } : new int[] { 1, 0, 3, 2 };
        for (int i : order) {
            Arm arm = this.arms[i];
            if (arm.free()) {
                this.strike(arm, target, Hit.MELEE, 0);
                this.sound(arm.tip, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.6F);
                return true;
            }
        }
        return false;
    }

    private void strike(Arm arm, LivingEntity target, Hit hit, int delay) {
        arm.job = Job.STRIKE;
        arm.age = 0;
        arm.target = target;
        arm.hit = hit;
        arm.delay = delay;
        arm.struckAt = -1;
    }

    /** Every free tentacle hits the target you aim at (or the nearest enemy in front), in turns. */
    boolean multiStrike(ServerLevel level) {
        if (this.searchedJustNow("multi_tentacle")) {
            return false;
        }
        double range = ability("multi_tentacle").value("rangeBlocks");
        LivingEntity target = Targeting.aimLiving(this.caster, level, range);
        if (target == null) {
            target = this.nearest(level, range, true);
        }
        if (target == null) {
            Targeting.noTarget(this.caster);
            return this.foundNothing("multi_tentacle");
        }
        int delay = 0;
        for (Arm arm : this.arms) {
            if (arm.free() || arm.job == Job.STRIKE) {
                this.strike(arm, target, Hit.MULTI, delay);
                delay += MULTI_GAP;
            }
        }
        if (delay == 0) {
            return this.foundNothing("multi_tentacle");
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 1.3F);
        this.sound(this.caster.position(), SoundEvents.CHAIN_PLACE, 1.0F, 1.4F);
        return true;
    }

    void deliver(ServerLevel level, Arm arm, LivingEntity target) {
        Vec3 at = target.getBoundingBox().getCenter();
        switch (arm.hit) {
            case MELEE -> {
                // The normal hit, with everything it brings: enchantments, crits, sweeping.
                this.delivering = true;
                this.caster.attack(target);
                this.delivering = false;
            }
            case MULTI -> this.hurt(target, damageOf("multi_tentacle"), 0.5);
            case RAMPAGE -> this.hurt(target, damageOf("rampage"), 0.3);
        }
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 8, 0.25, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 6, 0.2);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 0.6F, 1.4F);
    }

    /** Damage that counts as your attack; hits in quick succession all land. */
    boolean hurt(LivingEntity target, float damage, double knockback) {
        DamageSource source = this.caster.serverLevel().damageSources().playerAttack(this.caster);
        target.invulnerableTime = 0;
        if (!target.hurt(source, damage)) {
            return false;
        }
        Vec3 away = target.position().subtract(this.caster.position());
        if (knockback > 0.0 && away.horizontalDistanceSqr() > 1.0E-4) {
            target.knockback(knockback, -away.x, -away.z);
        }
        return true;
    }

    /**
     * Every enemy around you worth grabbing, nearest first: monsters, other players you are allowed to
     * hurt, anything that is after you, and whoever hit you last. Peaceful animals are left alone; you
     * grab those by aiming at them.
     */
    List<LivingEntity> threats(ServerLevel level, double range) {
        Vec3 eye = this.caster.getEyePosition();
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.caster.getBoundingBox().inflate(range),
                entity -> Targeting.isTargetable(this.caster, entity))) {
            boolean threat = living instanceof Enemy || living instanceof Player
                    || (living instanceof Mob mob && mob.getTarget() == this.caster)
                    || living == this.caster.getLastHurtByMob();
            Vec3 center = living.getBoundingBox().getCenter();
            if (threat && center.distanceTo(eye) <= range
                    && Targeting.clearPath(level, eye, center, this.caster)) {
                found.add(living);
            }
        }
        found.sort((a, b) -> Double.compare(a.distanceToSqr(this.caster), b.distanceToSqr(this.caster)));
        return found;
    }

    /**
     * The nearest creature to hit: enemies, players you are allowed to hurt, anything after you, or
     * whoever hit you last.
     */
    @Nullable
    private LivingEntity nearest(ServerLevel level, double range, boolean inFront) {
        Vec3 eye = this.caster.getEyePosition();
        Vec3 look = this.caster.getLookAngle();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB box = this.caster.getBoundingBox().inflate(range);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> Targeting.isTargetable(this.caster, entity))) {
            boolean threat = living instanceof Enemy || living instanceof Player
                    || (living instanceof Mob mob && mob.getTarget() == this.caster)
                    || living == this.caster.getLastHurtByMob();
            Vec3 center = living.getBoundingBox().getCenter();
            double distance = center.distanceTo(eye);
            if (!threat || distance > range || (inFront && look.dot(center.subtract(eye).normalize()) < 0.5)
                    || !Targeting.clearPath(level, eye, center, this.caster)) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    // ---- Octopus Rampage ----

    /** For 10 seconds: double attack speed, and all four tentacles attack nearby enemies by themselves. */
    boolean rampage(ServerLevel level) {
        this.rampage = ability("rampage").intValue("durationTicks");
        OctopusArms.modifier(this.caster, Attributes.ATTACK_SPEED, OctopusArms.RAMPAGE_ID, 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        Vec3 at = this.caster.position().add(0, 1.0, 0);
        ParticleFx.sphereOut(level, ParticleTypes.FLAME, at, 40, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.5);
        this.sound(at, SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
        this.sound(at, SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.6F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + ".rampage.start"), true);
        return true;
    }

    void tickRampage(ServerLevel level) {
        if (this.rampage <= 0) {
            return;
        }
        this.rampage--;
        if (this.rampage == 0) {
            this.endRampage();
            OctopusArms.sync(this.caster);
            return;
        }
        for (Arm arm : this.arms) {
            if (arm.free() && !this.isBlocking() && (this.rampage + arm.index * 3) % RAMPAGE_EVERY == 0) {
                LivingEntity target = this.nearest(level, RAMPAGE_RANGE, false);
                if (target != null) {
                    this.strike(arm, target, Hit.RAMPAGE, 0);
                }
            }
        }
        if (this.rampage % 20 == 0) {
            this.sound(this.caster.position(), SoundEvents.BLAZE_BURN, 0.5F, 0.6F);
        }
    }

    void endRampage() {
        this.rampage = 0;
        OctopusArms.modifier(this.caster, Attributes.ATTACK_SPEED, OctopusArms.RAMPAGE_ID, 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
    }
}
