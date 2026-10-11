package nl.tivek.multiversepowers.creature.wizard.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A spectral familiar the Wizard calls up in his second phase: a little bird of violet light with a long glowing tail
// that flies through walls as a vex does, circles its master and dives at his foes. It fades when he falls or after
// half a minute; a few blows put it out. Drawn by client/fx/FamiliarRenderer.
public final class ArcaneFamiliar extends Vex {
    private static final int LIFE = 600;
    private static final double OWNED = 48.0;
    // Whether a wizard called it (one from its egg has no master to outlive).
    private boolean bound;

    public ArcaneFamiliar(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static void summon(ServerLevel level, Wizard wizard, int count) {
        for (int i = 0; i < count; i++) {
            ArcaneFamiliar familiar = WizardEntities.FAMILIAR.get().create(level);
            if (familiar == null) {
                return;
            }
            double angle = i * Mth.TWO_PI / count + wizard.getRandom().nextDouble() * 0.5;
            Vec3 at = wizard.position().add(Math.cos(angle) * 1.4, 2.4 + wizard.hover(), Math.sin(angle) * 1.4);
            familiar.moveTo(at.x, at.y, at.z, wizard.getYRot(), 0.0F);
            familiar.setOwner(wizard);
            familiar.bound = true;
            familiar.setBoundOrigin(wizard.blockPosition());
            familiar.setLimitedLife(LIFE + wizard.getRandom().nextInt(200));
            familiar.setTarget(wizard.getTarget());
            level.addFreshEntity(familiar);
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.5F), at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.12);
        }
        Vec3 at = wizard.position();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 1.6F, 0.6F);
    }

    // How many familiars of `wizard`'s are about.
    public static int owned(Wizard wizard) {
        return wizard.level().getEntitiesOfClass(ArcaneFamiliar.class, wizard.getBoundingBox().inflate(OWNED),
                familiar -> familiar.getOwner() == wizard).size();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(Wizard.dust(Wizard.VIOLET, 0.7F), this.getX(), this.getY() + 0.25,
                        this.getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.bound && (this.getOwner() == null || !this.getOwner().isAlive())) {
            this.fade();
        }
    }

    private void fade() {
        if (this.level() instanceof ServerLevel level) {
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.2F), this.getX(), this.getY() + 0.25, this.getZ(), 16,
                    0.2, 0.2, 0.2, 0.0);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Wizard || source.getEntity() instanceof ArcaneFamiliar) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public float getVoicePitch() {
        return 1.5F + this.random.nextFloat() * 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.bound && super.shouldBeSaved();
    }
}
