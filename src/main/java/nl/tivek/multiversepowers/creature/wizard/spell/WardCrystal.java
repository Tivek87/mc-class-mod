package nl.tivek.multiversepowers.creature.wizard.spell;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// One of the three crystals that hold the Wizard's ward (WizardWard) while they circle him: each game puts it on the
// same ring round him by the world's clock, so it never lags behind him. Anything may strike it, arrows and blasts too;
// a few blows break it, and the ward is weaker for it. Drawn by client/fx/CrystalRenderer, a thread of light to him.
public final class WardCrystal extends Entity {
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(WardCrystal.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(WardCrystal.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(WardCrystal.class,
            EntityDataSerializers.FLOAT);
    public static final float MOST_HEALTH = 16.0F;
    private static final double RADIUS = 2.5;
    private static final double HEIGHT = 1.3;

    public WardCrystal(EntityType<? extends WardCrystal> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // `count` crystals round him, evenly spaced.
    public static void ring(ServerLevel level, Wizard wizard, int count) {
        for (int slot = 0; slot < count; slot++) {
            WardCrystal crystal = new WardCrystal(WizardEntities.CRYSTAL.get(), level);
            crystal.entityData.set(OWNER, wizard.getId());
            crystal.entityData.set(SLOT, slot);
            Vec3 at = place(wizard, slot, level.getGameTime());
            crystal.setPos(at.x, at.y, at.z);
            level.addFreshEntity(crystal);
            ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.5F), at.x, at.y + 0.4, at.z, 16,
                    0.2, 0.3, 0.2, 0.0);
        }
    }

    // Every crystal of his: burst into shards, or quietly gone.
    public static void dropAll(ServerLevel level, Wizard wizard, boolean burst) {
        for (WardCrystal crystal : level.getEntitiesOfClass(WardCrystal.class, wizard.getBoundingBox().inflate(16.0),
                crystal -> crystal.ownerId() == wizard.getId())) {
            if (burst) {
                crystal.shards(level, Wizard.light(wizard.phase()));
            }
            crystal.discard();
        }
    }

    // Where crystal `slot` of his is at `time` (the world's clock, ticks and a share of one).
    public static Vec3 place(Entity wizard, int slot, double time) {
        double angle = time * 0.045 + slot * Mth.TWO_PI / 3.0;
        double bob = 0.25 * Math.sin(time * 0.09 + slot * 2.1);
        double hover = wizard instanceof Wizard caster ? caster.hover() : 0.0;
        return wizard.position().add(Math.cos(angle) * RADIUS, HEIGHT + hover + bob, Math.sin(angle) * RADIUS);
    }

    public int ownerId() {
        return this.entityData.get(OWNER);
    }

    public int slot() {
        return this.entityData.get(SLOT);
    }

    public float health() {
        return this.entityData.get(HEALTH);
    }

    @Nullable
    public Wizard owner() {
        return this.level().getEntity(this.ownerId()) instanceof Wizard wizard ? wizard : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(SLOT, 0);
        builder.define(HEALTH, MOST_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();
        Wizard wizard = this.owner();
        if (wizard != null) {
            Vec3 at = place(wizard, this.slot(), this.level().getGameTime());
            this.setPos(at.x, at.y, at.z);
        }
        if (!this.level().isClientSide && (wizard == null || !wizard.isAlive() || wizard.ward() <= 0.0F)) {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (this.isInvulnerableTo(source) || by instanceof Wizard || by instanceof WizardDecoy
                || by instanceof ArcaneFamiliar || source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL)
                || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        float left = this.health() - amount;
        Vec3 at = this.position().add(0.0, 0.45, 0.0);
        Wizard wizard = this.owner();
        int light = wizard == null ? Wizard.VIOLET : Wizard.light(wizard.phase());
        if (left > 0.0F) {
            this.entityData.set(HEALTH, left);
            ParticleFx.send(level, Wizard.dust(light, 1.2F), at.x, at.y, at.z, 8, 0.15, 0.2, 0.15, 0.0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.4F, 1.2F);
            return true;
        }
        this.shards(level, light);
        this.discard();
        if (wizard != null) {
            wizard.crystalBroke();
        }
        return true;
    }

    private void shards(ServerLevel level, int light) {
        Vec3 at = this.position().add(0.0, 0.45, 0.0);
        ParticleFx.sphereOut(level, Wizard.dust(light, 1.4F), at, 28, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, at, 10, 0.2);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.35);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.8F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 1.3F);
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
