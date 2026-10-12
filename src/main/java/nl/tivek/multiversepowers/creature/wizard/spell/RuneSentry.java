package nl.tivek.multiversepowers.creature.wizard.spell;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// A rune sentry, his second phase: a floating eye of runes he raises beside him that turns to the nearest foe it sees
// and flicks a spark at it every so often, until its time runs out or a few blows break it. Drawn by
// client/fx/SentryRenderer.
public final class RuneSentry extends Entity {
    public static final int LIFE = 240;
    // Ticks between its sparks, and the tick it turns to aim before each.
    public static final int EVERY = 22;
    private static final double REACH = 24.0;
    private static final float MOST_HEALTH = 6.0F;
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(RuneSentry.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(RuneSentry.class,
            EntityDataSerializers.FLOAT);
    private float damage;

    public RuneSentry(EntityType<? extends RuneSentry> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // One sentry over his shoulder, a little to the side he is not facing his foe with.
    public static void raise(ServerLevel level, Wizard wizard, float damage) {
        RuneSentry sentry = new RuneSentry(WizardEntities.SENTRY.get(), level);
        sentry.entityData.set(OWNER, wizard.getId());
        sentry.damage = damage;
        double side = owned(wizard) % 2 == 0 ? 1.0 : -1.0;
        Vec3 at = wizard.at(side * 2.4, 2.6, -0.6);
        sentry.moveTo(at.x, at.y, at.z, wizard.getYRot(), 0.0F);
        level.addFreshEntity(sentry);
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.light(wizard.phase()), 1.5F), at, 24, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.8F);
    }

    // How many of his sentries still watch.
    public static int owned(Wizard wizard) {
        return wizard.level().getEntitiesOfClass(RuneSentry.class, wizard.getBoundingBox().inflate(32.0),
                sentry -> sentry.ownerId() == wizard.getId()).size();
    }

    public int ownerId() {
        return this.entityData.get(OWNER);
    }

    @Nullable
    public Wizard owner() {
        return this.level().getEntity(this.ownerId()) instanceof Wizard wizard ? wizard : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(HEALTH, MOST_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner();
        if (wizard == null || !wizard.isAlive() || this.tickCount > LIFE) {
            this.fade(level);
            this.discard();
            return;
        }
        LivingEntity foe = this.foe(level, wizard);
        if (foe != null) {
            Vec3 to = foe.getEyePosition().subtract(this.position());
            this.setYRot((float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG));
            this.setXRot((float) (-Mth.atan2(to.y, Math.hypot(to.x, to.z)) * Mth.RAD_TO_DEG));
        }
        int beat = this.tickCount % EVERY;
        if (foe != null && beat == EVERY - 6) {
            ParticleFx.implosion(level, Wizard.dust(Wizard.light(wizard.phase()), 1.1F), this.position(), 1.0, 8,
                    0.2);
        }
        if (foe != null && beat == 0 && this.tickCount > 10) {
            ArcaneBolt.spark(level, wizard, this.position(), foe, this.damage);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 1.4F, 1.7F);
        }
    }

    // The nearest of his foes it sees.
    @Nullable
    private LivingEntity foe(ServerLevel level, Wizard wizard) {
        List<LivingEntity> foes = MonsterBlows.foes(wizard, this.getBoundingBox().inflate(REACH));
        foes.removeIf(living -> !Wizard.prey(living) || living.distanceTo(this) > REACH || !this.sees(level, living));
        LivingEntity target = wizard.getTarget();
        if (target != null && foes.contains(target)) {
            return target;
        }
        return foes.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean sees(ServerLevel level, LivingEntity living) {
        return LoadedWorld.clip(level, new ClipContext(this.position(), living.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (this.isInvulnerableTo(source) || Wizard.ally(by) || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypeTags.IS_FALL) || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        float left = this.entityData.get(HEALTH) - amount;
        if (left > 0.0F) {
            this.entityData.set(HEALTH, left);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.HOSTILE, 1.4F, 1.4F);
            return true;
        }
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.VIOLET, 1.5F), this.position(), 30, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.position(), 16, 0.35);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 1.8F, 1.0F);
        this.discard();
        return true;
    }

    private void fade(ServerLevel level) {
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.STARLIGHT, 1.2F), this.position(), 14, 0.15);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
