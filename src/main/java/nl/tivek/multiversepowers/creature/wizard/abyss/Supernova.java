package nl.tivek.multiversepowers.creature.wizard.abyss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.field.FieldSpell;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The supernova, his last ultimate: high in the air over his raised staff a star kindles and grows, drawing the light
// in, brighter and brighter, as every player near is warned to take cover. Then it bursts: whoever can see it (nothing
// solid between them and the star) is struck by its light, burnt and thrown; whoever hides behind a wall is spared.
// Drawn by client/abyss/SupernovaRenderer.
public final class Supernova extends FieldSpell {
    public static final int BURST = WizardMove.release(WizardMove.SUPERNOVA) - WizardMove.NOVA_RISE;
    public static final int LIFE = BURST + 34;
    public static final double REACH = 48.0;
    // How far over his staff's crystal it burns.
    public static final double ABOVE = 2.6;

    public Supernova(EntityType<? extends Supernova> type, Level level) {
        super(type, level);
    }

    public static void kindle(ServerLevel level, Wizard wizard, float damage) {
        Supernova star = new Supernova(WizardEntities.SUPERNOVA.get(), level);
        Vec3 at = place(wizard);
        star.setPos(at.x, at.y, at.z);
        star.castBy(wizard, damage);
        level.addFreshEntity(star);
        Component warning = Component.translatable("boss." + MultiversePowers.MODID + ".wizard.supernova");
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(REACH))) {
            player.displayClientMessage(warning, true);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 1.4F);
    }

    // Over his staff, as it follows him.
    private static Vec3 place(Wizard wizard) {
        return wizard.staffTip().add(0.0, ABOVE, 0.0);
    }

    // How far grown (0 to 1) `age` ticks in.
    public static float grown(float age) {
        float f = Mth.clamp(age / BURST, 0.0F, 1.0F);
        return f * f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (this.tickCount > LIFE || wizard == null && this.tickCount < BURST) {
            this.discard();
            return;
        }
        if (wizard != null && this.tickCount < BURST) {
            Vec3 at = place(wizard);
            this.setPos(at.x, at.y, at.z);
        }
        Vec3 at = this.position();
        if (this.tickCount < BURST) {
            float grown = grown(this.tickCount);
            if (this.tickCount % 2 == 0) {
                ParticleFx.implosion(level, Wizard.dust(Wizard.STARLIGHT, 1.6F), at, 6.0 + 6.0 * grown, 8, 0.5);
                ParticleFx.implosion(level, ParticleTypes.END_ROD, at, 8.0, 4, 0.6);
            }
            if (this.tickCount % 20 == 0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F,
                        0.5F + grown);
            }
            if (this.tickCount == BURST - 16) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0F,
                        0.6F);
            }
            return;
        }
        if (this.tickCount == BURST && wizard != null) {
            this.burst(level, wizard, at);
        }
    }

    // Everyone who can see it is struck by its light; whoever hides behind something solid is spared.
    private void burst(ServerLevel level, Wizard wizard, Vec3 at) {
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(REACH))) {
            double far = living.getEyePosition().distanceTo(at);
            if (far > REACH || Wizard.ally(living) || !(this.sees(level, at, living.getEyePosition())
                    || this.sees(level, at, living.getBoundingBox().getCenter()))) {
                continue;
            }
            Vec3 away = living.position().subtract(at).normalize();
            float harm = (float) (this.damage * (1.0 - 0.55 * far / REACH));
            if (MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.NOVA_BURST), harm,
                    away.scale(1.7).add(0.0, 0.6, 0.0))) {
                living.igniteForSeconds(6.0F);
            }
        }
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 60, 1.0);
        ParticleFx.sphereOut(level, Wizard.dust(0xFFF6D8, 3.0F), at, 160, 1.4);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, at, 120, 1.1);
        ParticleFx.sphereOut(level, ParticleTypes.FLAME, at, 80, 0.9);
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 3, 1.0, 1.0, 1.0, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.4F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 1.2F);
    }

    private boolean sees(ServerLevel level, Vec3 from, Vec3 to) {
        return LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this)).getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0 * 256.0;
    }
}
