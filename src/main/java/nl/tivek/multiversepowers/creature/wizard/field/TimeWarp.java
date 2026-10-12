package nl.tivek.multiversepowers.creature.wizard.field;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The time warp, an ultimate of his third phase: a great clock face of golden light spreads over the ground round him,
// its hands turning, ticking loud. Inside it time drags for his foes (slowed in step and swing, the world gone golden
// at the edges of their sight, client/fx/WarpRenderer) and races for him: his spells come round twice as fast. It
// lasts eight seconds.
public final class TimeWarp extends FieldSpell {
    public static final double RADIUS = 10.0;
    public static final int GROWS = 12;
    public static final int LIFE = 160;
    public static final int FADES = 16;

    public TimeWarp(EntityType<? extends TimeWarp> type, Level level) {
        super(type, level);
    }

    // Spread on the ground between him and his foe, so both stand in it.
    public static void cast(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        TimeWarp warp = new TimeWarp(WizardEntities.WARP.get(), level);
        Vec3 middle = target == null ? wizard.position() : wizard.position().lerp(target.position(), 0.5);
        Vec3 at = Wizard.groundUnder(level, middle);
        warp.setPos(at.x, at.y, at.z);
        warp.castBy(wizard, 0.0F);
        level.addFreshEntity(warp);
        level.playSound(null, wizard.getX(), wizard.getY(), wizard.getZ(), SoundEvents.BELL_RESONATE,
                SoundSource.HOSTILE, 2.5F, 0.6F);
        level.playSound(null, wizard.getX(), wizard.getY(), wizard.getZ(), SoundEvents.BELL_BLOCK,
                SoundSource.HOSTILE, 2.5F, 0.5F);
    }

    // How strong it holds now: spreading at first, fading at the end.
    public float strength(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(Math.min(age / GROWS, (LIFE - age) / FADES), 0.0F, 1.0F);
    }

    // Whether `at` lies inside it.
    public boolean holds(Vec3 at) {
        return Math.hypot(at.x - this.getX(), at.z - this.getZ()) < RADIUS && Math.abs(at.y - this.getY()) < 6.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        if (this.tickCount % 10 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.HOSTILE, 2.0F, this.tickCount % 20 == 0 ? 0.5F : 0.7F);
            wizard.haste(20);
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(this.position(), this.position())
                    .inflate(RADIUS, 6.0, RADIUS))) {
                if (this.holds(living.position())) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false), wizard);
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 25, 2, false, false), wizard);
                }
            }
        }
        if (this.tickCount % 4 == 0) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            Vec3 at = this.position().add(Math.cos(angle) * RADIUS, 0.2, Math.sin(angle) * RADIUS);
            ParticleFx.send(level, Wizard.dust(Wizard.TIME, 1.4F), at.x, at.y, at.z, 2, 0.1, 0.4, 0.1, 0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.03);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }
}
