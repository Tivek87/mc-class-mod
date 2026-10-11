package nl.tivek.multiversepowers.creature.bloater;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A ball of acid slime the Bloater spits: it arcs, dripping, and bursts on what it hits; whoever it strikes is hurt,
// poisoned 4 seconds and slowed by the sticky goo.
public final class BloaterSpit extends ThrowableProjectile {
    private float damage = 5.0F;

    public BloaterSpit(EntityType<? extends BloaterSpit> type, Level level) {
        super(type, level);
    }

    BloaterSpit(Level level, LivingEntity owner, float damage) {
        super(Bloater.SPIT.get(), owner, level);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.035;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            Vec3 at = this.position();
            this.level().addParticle(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.15, at.z, 0.0, 0.0, 0.0);
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleFx.dust(Bloater.ACID, 0.9F), at.x + ParticleFx.spread(0.15), at.y
                        + 0.15 + ParticleFx.spread(0.15), at.z + ParticleFx.spread(0.15), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity owner = this.getOwner();
        if (hit == owner || !(hit instanceof LivingEntity living)) {
            return;
        }
        if (living.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity shooter ? shooter
                : null), this.damage)) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1), owner);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = result.getLocation();
            ParticleFx.send(level, ParticleTypes.ITEM_SLIME, at.x, at.y + 0.1, at.z, 18, 0.25, 0.2, 0.25, 0.2);
            ParticleFx.send(level, ParticleFx.dust(Bloater.ACID, 1.4F), at.x, at.y + 0.1, at.z, 12, 0.3, 0.2, 0.3,
                    0.0);
            ParticleFx.send(level, ParticleTypes.SNEEZE, at.x, at.y + 0.1, at.z, 6, 0.2, 0.1, 0.2, 0.02);
            level.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.8F);
            level.playSound(null, BlockPos.containing(at), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 0.8F,
                    0.7F);
            this.discard();
        }
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
