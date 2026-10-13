package nl.tivek.multiversepowers.creature.elemental.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Scorch Ball: a fireball from the fire rod, bigger and slower than a blaze's, trailing flame and smoke. It sets
// whoever it strikes alight and bursts into a patch of fire on the ground that burns a few seconds (ScorchPatch); the
// Blaze's also lights real fire round it where the world lets creatures change it. As any fireball, a blow turns it
// back the other way.
public final class ScorchBall extends AbstractHurtingProjectile {
    private float damage = 5.0F;

    public ScorchBall(EntityType<? extends ScorchBall> type, Level level) {
        super(type, level);
    }

    public ScorchBall(Level level, LivingEntity owner, Vec3 from, Vec3 way, float damage) {
        super(ElementalBlaze.SCORCH_BALL.get(), owner, way, level);
        this.accelerationPower = 0.06;
        this.setPos(from);
        this.setDeltaMovement(way.scale(0.6));
        this.damage = damage;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            Vec3 at = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
            Vec3 back = this.getDeltaMovement().scale(-0.15);
            this.level().addParticle(ParticleTypes.SMALL_FLAME, at.x + (this.random.nextDouble() - 0.5) * 0.3,
                    at.y + (this.random.nextDouble() - 0.5) * 0.3, at.z + (this.random.nextDouble() - 0.5) * 0.3,
                    back.x, back.y, back.z);
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, back.x, back.y + 0.01, back.z);
            }
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.LAVA, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && BlazeCasts.hits(this.getOwner(), target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel) || !(result.getEntity() instanceof LivingEntity living)) {
            return;
        }
        int burning = living.getRemainingFireTicks();
        living.igniteForSeconds(5.0F);
        if (!living.hurt(BlazeCasts.source(this, this.getOwner(), Element.FIRE), this.damage)) {
            living.setRemainingFireTicks(burning);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Entity owner = this.getOwner();
        if (this.level().isClientSide || !(owner instanceof Mob) || !EventHooks.canEntityGrief(this.level(), owner)) {
            return;
        }
        BlockPos hit = result.getBlockPos().relative(result.getDirection());
        for (BlockPos pos : new BlockPos[] { hit, hit.east(), hit.west(), hit.north(), hit.south() }) {
            if (this.level().isEmptyBlock(pos) && BaseFireBlock.canBePlacedAt(this.level(), pos,
                    result.getDirection()) && this.random.nextInt(pos == hit ? 1 : 3) == 0) {
                this.level().setBlockAndUpdate(pos, BaseFireBlock.getState(this.level(), pos));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = result.getLocation();
            ParticleFx.sphereOut(level, ParticleTypes.FLAME, at, 30, 0.18);
            ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y, at.z, 8, 0.3, 0.2, 0.3, 0.0);
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, 0.4, 0.3, 0.4, 0.03);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F,
                    1.6F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.6F);
            Effects.start(level, new ScorchPatch(at, this.getOwner()));
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
