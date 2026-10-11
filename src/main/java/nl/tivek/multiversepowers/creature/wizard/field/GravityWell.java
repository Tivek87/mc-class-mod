package nl.tivek.multiversepowers.creature.wizard.field;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.spell.SpellBlast;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The gravity lift, his second phase: rings of deep blue light close round his foe, and they are torn off the ground
// and carried up (RISE) as his hands rise; then he clenches his fist and drives them down (SLAM), and where they land
// the ground bursts. Drawn by client/fx/WellRenderer round whoever it lifts.
public final class GravityWell extends FieldSpell {
    public static final int RISE = 6;
    public static final int SLAM = WizardMove.SLAM - WizardMove.release(WizardMove.LIFT);
    private static final int LONGEST = 50;
    private static final float POWER = 1.4F;
    private static final double REACH = 2.8;
    private static final EntityDataAccessor<Integer> HELD = SynchedEntityData.defineId(GravityWell.class,
            EntityDataSerializers.INT);
    private boolean landed;

    public GravityWell(EntityType<? extends GravityWell> type, Level level) {
        super(type, level);
    }

    public static void lift(ServerLevel level, Wizard wizard, LivingEntity target, float damage) {
        GravityWell well = new GravityWell(WizardEntities.WELL.get(), level);
        well.setPos(target.getX(), target.getY(), target.getZ());
        well.castBy(wizard, damage);
        well.entityData.set(HELD, target.getId());
        level.addFreshEntity(well);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 1.6F, 0.5F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HELD, -1);
    }

    @Nullable
    public Entity held() {
        return this.level().getEntity(this.entityData.get(HELD));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.held() instanceof LivingEntity living) || !living.isAlive()) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }
        this.setPos(living.getX(), living.getY(), living.getZ());
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || this.tickCount > LONGEST) {
            living.removeEffect(MobEffects.LEVITATION);
            this.discard();
            return;
        }
        if (this.tickCount == RISE) {
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, SLAM - RISE, 6, false, false), wizard);
            living.setDeltaMovement(living.getDeltaMovement().add(0.0, 0.5, 0.0));
            living.hurtMarked = true;
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.HOSTILE, 1.4F, 0.5F);
        }
        if (this.tickCount == SLAM) {
            living.removeEffect(MobEffects.LEVITATION);
            living.setDeltaMovement(0.0, -2.4, 0.0);
            living.hurtMarked = true;
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT,
                    SoundSource.HOSTILE, 1.6F, 0.6F);
        }
        if (this.tickCount > SLAM && !this.landed && this.standing(living)) {
            this.landed = true;
            Vec3 at = living.position();
            SpellBlast.burst(level, this, wizard, at, POWER, false, REACH, this.damage, WizardEntities.STARFALL);
            ParticleFx.shockwave(level, Wizard.dust(Wizard.GRAVITY, 2.2F), at.add(0.0, 0.2, 0.0), 48, 0.7);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, at.x, at.y + 0.2, at.z, 4, 0.8, 0.1, 0.8, 0.0);
            this.discard();
        }
    }

    // On the ground now (the server's own onGround runs a tick ahead for players).
    private boolean standing(LivingEntity living) {
        return !this.level().noCollision(living, living.getBoundingBox().move(0.0, -0.2, 0.0));
    }
}
