package nl.tivek.multiversepowers.creature.wizard.spell;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A bolt of arcane light from the Wizard's staff: it flies fast and bends after its mark a little each tick, so it
// can be stepped aside from late but not outrun; it strikes with the harm of pure magic and bursts in violet sparks.
// Unspent, it fizzles out after a few seconds. A spark (WizardEntities.SPARK) is its small quick cousin, flicked
// between his big spells: faster, straight, weaker.
public final class ArcaneBolt extends ThrowableProjectile {
    private static final double SPEED = 0.95;
    private static final double SPARK_SPEED = 1.4;
    private static final double TURN = 0.09;
    private static final int LIFE = 70;
    private static final int SPARK_LIFE = 35;
    private float damage = 6.0F;
    @Nullable
    private LivingEntity mark;

    public ArcaneBolt(EntityType<? extends ArcaneBolt> type, Level level) {
        super(type, level);
    }

    public static void cast(ServerLevel level, Wizard wizard, LivingEntity target, float damage) {
        ArcaneBolt bolt = new ArcaneBolt(WizardEntities.BOLT.get(), level);
        bolt.setOwner(wizard);
        Vec3 from = wizard.staffTip();
        bolt.setPos(from.x, from.y, from.z);
        Vec3 way = target.getBoundingBox().getCenter().subtract(from).normalize();
        bolt.setDeltaMovement(way.scale(SPEED));
        bolt.damage = damage;
        bolt.mark = target;
        level.addFreshEntity(bolt);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F,
                1.6F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F,
                0.7F);
    }

    // A quick spark from `from` at where `target` will be, led a little.
    public static void spark(ServerLevel level, Wizard wizard, Vec3 from, LivingEntity target, float damage) {
        ArcaneBolt spark = new ArcaneBolt(WizardEntities.SPARK.get(), level);
        spark.setOwner(wizard);
        spark.setPos(from.x, from.y, from.z);
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 lead = target.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(from.distanceTo(to) / SPARK_SPEED);
        spark.setDeltaMovement(to.add(lead).subtract(from).normalize().scale(SPARK_SPEED));
        spark.damage = damage;
        level.addFreshEntity(spark);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.2F,
                1.8F);
    }

    private boolean isSpark() {
        return this.getType() == WizardEntities.SPARK.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position();
            Vec3 v = this.getDeltaMovement();
            for (int k = 0; k < 2; k++) {
                double s = k * 0.5;
                this.level().addParticle(Wizard.dust(Wizard.VIOLET, 1.1F), at.x - v.x * s, at.y + 0.2 - v.y * s,
                        at.z - v.z * s, 0.0, 0.0, 0.0);
            }
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, at.x, at.y + 0.2, at.z, -v.x * 0.05, -v.y * 0.05,
                        -v.z * 0.05);
            }
            return;
        }
        LivingEntity target = this.mark;
        if (target != null && target.isAlive()) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize();
            Vec3 v = this.getDeltaMovement().normalize();
            this.setDeltaMovement(v.add(want.subtract(v).scale(TURN)).normalize().scale(SPEED));
        }
        if (this.tickCount > (this.isSpark() ? SPARK_LIFE : LIFE)) {
            this.burst(this.position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof Wizard) && !(entity instanceof WizardDecoy) && !(entity instanceof ArcaneFamiliar)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity living) {
            Vec3 push = this.getDeltaMovement().normalize().scale(0.4);
            if (living.hurt(Wizard.source(this, this.getOwner(), WizardEntities.ARCANE), this.damage)) {
                living.push(push.x, 0.15, push.z);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.burst(result.getLocation());
        }
    }

    private void burst(Vec3 at) {
        if (this.level() instanceof ServerLevel level) {
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.3F), at.x, at.y + 0.2, at.z, 16, 0.25, 0.25, 0.25,
                    0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 0.2, at.z, 8, 0.05, 0.05, 0.05, 0.12);
            ParticleFx.send(level, ParticleTypes.WITCH, at.x, at.y + 0.2, at.z, 6, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 0.9F,
                    1.4F);
        }
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
