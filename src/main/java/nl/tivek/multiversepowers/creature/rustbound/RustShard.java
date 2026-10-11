package nl.tivek.multiversepowers.creature.rustbound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.Bleeding;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A jagged piece of rusted scrap the Rustbound flings: it spins as it flies, almost straight, and whoever it strikes is
// cut and left bleeding; on a block it shatters into flakes of rust.
public final class RustShard extends ThrowableProjectile {
    private float damage = 3.0F;

    public RustShard(EntityType<? extends RustShard> type, Level level) {
        super(type, level);
    }

    RustShard(Level level, LivingEntity owner, float damage) {
        super(Rustbound.SHARD.get(), owner, level);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.015;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive() && this.random.nextInt(3) == 0) {
            Vec3 at = this.position();
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.RED_SAND
                    .defaultBlockState()), at.x, at.y + 0.1, at.z, 0.0, 0.0, 0.0);
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
        DamageSource cut = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(MonsterBlows.SHRAPNEL), this, owner);
        if (living.hurt(cut, this.damage)) {
            Bleeding.tear(living, 100, 1);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = result.getLocation();
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.IRON_BLOCK.defaultBlockState()), at.x, at.y + 0.05, at.z, 6, 0.1, 0.1, 0.1, 0.1);
            ParticleFx.send(level, new BlockParticleOption(
                    ParticleTypes.FALLING_DUST, Blocks.RED_SAND.defaultBlockState()), at.x, at.y + 0.05, at.z, 5, 0.15,
                    0.1, 0.15, 0.0);
            level.playSound(null, BlockPos.containing(at), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 0.9F, 1.4F);
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
