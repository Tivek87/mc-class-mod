package nl.tivek.multiversepowers.creature.wizard.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;

// A ball of fire the Wizard throws from his left hand, in a shallow arc: where it lands it bursts, throwing whoever
// stands near and setting them alight, and (if the world lets creatures grief) starting fires round it. It breaks no
// blocks.
public final class WizardFireball extends ThrowableProjectile {
    private static final double SPEED = 0.8;
    private static final float POWER = 1.6F;
    private static final double BURN_REACH = 3.2;
    private float damage = 9.0F;

    public WizardFireball(EntityType<? extends WizardFireball> type, Level level) {
        super(type, level);
    }

    public static void cast(ServerLevel level, Wizard wizard, LivingEntity target, float damage) {
        WizardFireball ball = new WizardFireball(WizardEntities.FIREBALL.get(), level);
        ball.setOwner(wizard);
        Vec3 from = wizard.palm();
        ball.setPos(from.x, from.y, from.z);
        Vec3 way = target.getBoundingBox().getCenter().subtract(from);
        // Aimed as high above them as gravity takes it down on the way.
        double time = Math.max(1.0, way.length() / SPEED);
        ball.setDeltaMovement(way.x / time, way.y / time + 0.5 * ball.getDefaultGravity() * time, way.z / time);
        ball.damage = damage;
        level.addFreshEntity(ball);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.7F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.012;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position().add(0.0, 0.3, 0.0);
            Vec3 v = this.getDeltaMovement();
            for (int k = 0; k < 3; k++) {
                this.level().addParticle(ParticleTypes.FLAME, at.x + (this.random.nextDouble() - 0.5) * 0.4,
                        at.y + (this.random.nextDouble() - 0.5) * 0.4, at.z + (this.random.nextDouble() - 0.5) * 0.4,
                        -v.x * 0.15, -v.y * 0.15 + 0.01, -v.z * 0.15);
            }
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x - v.x, at.y - v.y, at.z - v.z, 0.0, 0.02, 0.0);
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.LAVA, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
        } else if (this.tickCount > 120) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !Wizard.ally(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = result.getLocation();
        Entity owner = this.getOwner();
        boolean fire = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        for (LivingEntity living : SpellBlast.burst(level, this, owner, at, POWER, fire, BURN_REACH, this.damage,
                DamageTypes.FIREBALL)) {
            living.igniteForSeconds(5.0F);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_DEATH, SoundSource.HOSTILE, 0.6F, 1.4F);
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            this.damage = tag.getFloat("Damage");
        }
    }
}
