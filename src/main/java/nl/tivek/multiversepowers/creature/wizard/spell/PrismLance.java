package nl.tivek.multiversepowers.creature.wizard.spell;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import org.joml.Vector3f;

// A prism lance, his second phase: a long crystal of storm-blue light grows in the air beside him, turns to follow its
// mark with a thin line of light showing where it points, holds still a moment (the time to step aside) and shoots
// along that line too fast to outrun, through everyone in its way, shattering on the first block it meets. Drawn by
// client/fx/LanceRenderer.
public final class PrismLance extends ThrowableProjectile {
    private static final EntityDataAccessor<Vector3f> AIM = SynchedEntityData.defineId(PrismLance.class,
            EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> LAUNCH = SynchedEntityData.defineId(PrismLance.class,
            EntityDataSerializers.INT);
    private static final double SPEED = 1.9;
    // It stops following its mark this long before it shoots.
    public static final int LOCK = 7;
    private static final int LIFE = 40;
    private float damage = 8.0F;
    @Nullable
    private LivingEntity mark;
    private final Set<Integer> struck = new HashSet<>();

    public PrismLance(EntityType<? extends PrismLance> type, Level level) {
        super(type, level);
    }

    // `count` lances fanned out over and behind his shoulders, each shooting `every` ticks after the one before.
    public static void fan(ServerLevel level, Wizard wizard, LivingEntity target, float damage, int count, int first,
            int every) {
        for (int k = 0; k < count; k++) {
            double share = count == 1 ? 0.0 : k / (double) (count - 1) - 0.5;
            Vec3 at = wizard.at(share * 3.4, 2.6 + 0.6 * (1.0 - 4.0 * share * share), -0.8);
            PrismLance lance = new PrismLance(WizardEntities.LANCE.get(), level);
            lance.setOwner(wizard);
            lance.setPos(at.x, at.y, at.z);
            lance.damage = damage;
            lance.mark = target;
            lance.entityData.set(LAUNCH, first + k * every);
            lance.entityData.set(AIM, target.getBoundingBox().getCenter().toVector3f());
            level.addFreshEntity(lance);
            ParticleFx.send(level, Wizard.dust(Wizard.STORM, 1.3F), at.x, at.y, at.z, 10, 0.15, 0.15, 0.15, 0.0);
        }
        level.playSound(null, wizard.getX(), wizard.getY() + 2.0, wizard.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // Where it points, and when it shoots (its own ticks).
    public Vec3 aim() {
        return new Vec3(this.entityData.get(AIM));
    }

    public int launch() {
        return this.entityData.get(LAUNCH);
    }

    public boolean flying() {
        return this.tickCount >= this.launch();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AIM, new Vector3f());
        builder.define(LAUNCH, 20);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        if (!this.flying()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide) {
                LivingEntity target = this.mark;
                if (target != null && target.isAlive() && this.tickCount < this.launch() - LOCK) {
                    this.entityData.set(AIM, target.getBoundingBox().getCenter().toVector3f());
                }
                if (this.tickCount == this.launch() - 1) {
                    Vec3 way = this.aim().subtract(this.position());
                    if (way.lengthSqr() < 1.0E-4) {
                        way = new Vec3(0.0, -1.0, 0.0);
                    }
                    this.setDeltaMovement(way.normalize().scale(SPEED));
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TRIDENT_THROW,
                            SoundSource.HOSTILE, 1.6F, 1.6F);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0F, 0.5F);
                }
            }
            this.baseTick();
            return;
        }
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position();
            this.level().addParticle(Wizard.dust(Wizard.STORM, 1.0F), at.x, at.y, at.z, 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        } else if (this.tickCount > this.launch() + LIFE) {
            this.shatter(this.position());
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !Wizard.ally(entity) && !this.struck.contains(entity.getId()) && super.canHitEntity(entity);
    }

    // Through everyone in its way: each is struck once and it flies on.
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        this.struck.add(entity.getId());
        if (entity instanceof LivingEntity living && this.level() instanceof ServerLevel level) {
            Vec3 push = this.getDeltaMovement().normalize().scale(0.6);
            if (living.hurt(Wizard.source(this, this.getOwner(), WizardEntities.LIGHTNING), this.damage)) {
                living.push(push.x, 0.2, push.z);
            }
            Vec3 at = living.getBoundingBox().getCenter();
            ParticleFx.send(level, Wizard.dust(Wizard.STORM, 1.4F), at.x, at.y, at.z, 14, 0.2, 0.3, 0.2, 0.0);
            ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.3);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.shatter(result.getLocation());
        }
    }

    private void shatter(Vec3 at) {
        if (this.level() instanceof ServerLevel level) {
            ParticleFx.sphereOut(level, Wizard.dust(Wizard.STORM, 1.3F), at, 18, 0.25);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.05, 0.05, 0.05, 0.12);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4F,
                    1.2F + 0.2F * Mth.sin(this.tickCount));
        }
        this.discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
