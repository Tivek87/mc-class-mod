package nl.tivek.multiversepowers.creature.wizard.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A star fallen out of the Wizard's rift: a white-hot core in a violet glow trailing sparks, faster as it falls. It
// bursts where it lands in a flash and a ring of sparks, throwing and burning whoever stands near.
public final class FallingStar extends ThrowableProjectile {
    private static final float POWER = 1.2F;
    private static final double REACH = 2.6;
    private float damage = 7.0F;

    public FallingStar(EntityType<? extends FallingStar> type, Level level) {
        super(type, level);
    }

    public static void fall(ServerLevel level, Entity owner, Vec3 from, Vec3 to, float damage) {
        FallingStar star = new FallingStar(WizardEntities.STAR.get(), level);
        star.setOwner(owner);
        star.setPos(from.x, from.y, from.z);
        Vec3 way = to.subtract(from);
        double down = Math.max(1.0, from.y - to.y);
        // Starting at 0.35 a tick down and gaining gravity: the ticks it takes to drop, and how fast it must go across.
        double g = star.getDefaultGravity();
        double v0 = 0.35;
        double time = (-v0 + Math.sqrt(v0 * v0 + 2.0 * g * down)) / g;
        star.setDeltaMovement(way.x / time, -v0, way.z / time);
        star.damage = damage;
        level.addFreshEntity(star);
    }

    // A star thrown up out of `from` in an arc that comes down on `to` (out of the great meteor's crater).
    public static void toss(ServerLevel level, Entity owner, Vec3 from, Vec3 to, float damage) {
        FallingStar star = new FallingStar(WizardEntities.STAR.get(), level);
        star.setOwner(owner);
        star.setPos(from.x, from.y, from.z);
        double g = star.getDefaultGravity();
        double time = 18.0;
        Vec3 way = to.subtract(from);
        star.setDeltaMovement(way.x / time, (way.y + 0.5 * g * time * time) / time, way.z / time);
        star.damage = damage;
        level.addFreshEntity(star);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position().add(0.0, 0.25, 0.0);
            Vec3 v = this.getDeltaMovement();
            for (int k = 0; k < 3; k++) {
                double s = k / 3.0;
                this.level().addParticle(Wizard.dust(k == 0 ? Wizard.STARLIGHT : Wizard.VIOLET, 1.4F),
                        at.x - v.x * s, at.y - v.y * s, at.z - v.z * s, 0.0, 0.0, 0.0);
            }
            this.level().addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, (this.random.nextDouble() - 0.5) * 0.05,
                    0.02, (this.random.nextDouble() - 0.5) * 0.05);
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.FIREWORK, at.x, at.y, at.z, -v.x * 0.1, -v.y * 0.1,
                        -v.z * 0.1);
            }
        } else if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof Wizard) && !(entity instanceof WizardDecoy) && !(entity instanceof FallingStar)
                && !(entity instanceof ArcaneFamiliar)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = result.getLocation();
        SpellBlast.burst(level, this, this.getOwner(), at, POWER, false, REACH, this.damage, WizardEntities.STARFALL);
        ParticleFx.send(level, ParticleTypes.FLASH, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 0.3, at.z, 24, 0.1, 0.1, 0.1, 0.35);
        ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 2.0F), at.x, at.y + 0.4, at.z, 24, 0.9, 0.3, 0.9, 0.0);
        ParticleFx.send(level, ParticleTypes.FIREWORK, at.x, at.y + 0.3, at.z, 16, 0.2, 0.1, 0.2, 0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 1.4F,
                0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
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
