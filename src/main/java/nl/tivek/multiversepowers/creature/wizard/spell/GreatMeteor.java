package nl.tivek.multiversepowers.creature.wizard.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.field.StarRift;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import org.joml.Vector3f;

// The great meteor, an ultimate of his third phase: a rift tears open high over his foe and a burning rock as wide as
// a door drops out of it, slowly at first. A ring of runes on the ground below glows where it will land, closing in as
// it comes (client/fx/MeteorRenderer); whoever is still inside when it strikes is thrown and burned, the most in the
// middle, and burning stars fly out of the crater. It breaks no blocks.
public final class GreatMeteor extends ThrowableProjectile {
    public static final double REACH = 6.5;
    private static final double HEIGHT = 30.0;
    private static final float POWER = 3.5F;
    private static final int SHARDS = 8;
    private static final EntityDataAccessor<Vector3f> TARGET = SynchedEntityData.defineId(GreatMeteor.class,
            EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> START = SynchedEntityData.defineId(GreatMeteor.class,
            EntityDataSerializers.FLOAT);
    private float damage = 20.0F;

    public GreatMeteor(EntityType<? extends GreatMeteor> type, Level level) {
        super(type, level);
    }

    public static void call(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        double height = HEIGHT;
        for (int up = 3; up <= HEIGHT; up++) {
            if (!level.getBlockState(BlockPos.containing(ground.x, ground.y + up, ground.z)).isAir()) {
                height = Math.max(6.0, up - 2.0);
                break;
            }
        }
        GreatMeteor meteor = new GreatMeteor(WizardEntities.METEOR.get(), level);
        meteor.setOwner(wizard);
        Vec3 from = ground.add(0.0, height, 0.0);
        meteor.setPos(from.x, from.y, from.z);
        meteor.setDeltaMovement(0.0, -0.15, 0.0);
        meteor.damage = damage;
        meteor.entityData.set(TARGET, new Vector3f((float) ground.x, (float) ground.y, (float) ground.z));
        meteor.entityData.set(START, (float) from.y);
        level.addFreshEntity(meteor);
        StarRift.portal(level, wizard, ground, height + 1.0);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, 0.4F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET, new Vector3f());
        builder.define(START, 0.0F);
    }

    // Where it will land, and how far it has fallen there (0 to 1).
    public Vec3 target() {
        Vector3f target = this.entityData.get(TARGET);
        return new Vec3(target.x, target.y, target.z);
    }

    public float fallen(float partialTick) {
        float start = this.entityData.get(START);
        double y = Mth.lerp(partialTick, this.yo, this.getY());
        double ground = this.entityData.get(TARGET).y;
        return start - ground < 1.0E-3 ? 1.0F : Mth.clamp((float) ((start - y) / (start - ground)), 0.0F, 1.0F);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            Vec3 at = this.position().add(0.0, 0.8, 0.0);
            for (int k = 0; k < 4; k++) {
                this.level().addParticle(ParticleTypes.FLAME, at.x + (this.random.nextDouble() - 0.5) * 1.4,
                        at.y + (this.random.nextDouble() - 0.5) * 1.4, at.z + (this.random.nextDouble() - 0.5) * 1.4,
                        0.0, 0.12, 0.0);
            }
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.8, at.z, 0.0, 0.15, 0.0);
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.LAVA, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
        } else if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !Wizard.ally(entity) && !(entity instanceof FallingStar) && super.canHitEntity(entity);
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
        for (LivingEntity living : SpellBlast.burst(level, this, owner, at, POWER, fire, REACH, this.damage,
                WizardEntities.STARFALL)) {
            living.igniteForSeconds(4.0F);
        }
        ParticleFx.at(level, ParticleTypes.EXPLOSION_EMITTER, at);
        ParticleFx.shockwave(level, ParticleTypes.FLAME, at.add(0.0, 0.3, 0.0), 80, 0.9);
        ParticleFx.shockwave(level, Wizard.dust(Wizard.FIRE, 2.5F), at.add(0.0, 0.5, 0.0), 60, 0.7);
        ParticleFx.sphereOut(level, ParticleTypes.LAVA, at, 30, 0.5);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5, at.z, 30, 2.0, 0.5, 2.0, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
        if (owner != null) {
            for (int i = 0; i < SHARDS; i++) {
                double angle = i * Mth.TWO_PI / SHARDS + this.random.nextDouble() * 0.4;
                double far = 5.0 + this.random.nextDouble() * 4.0;
                Vec3 to = at.add(Math.cos(angle) * far, 0.0, Math.sin(angle) * far);
                FallingStar.toss(level, owner, at.add(0.0, 1.0, 0.0), to, this.damage * 0.25F);
            }
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

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }
}
