package nl.tivek.multiversepowers.creature.wizard.special;

import java.util.HashSet;
import java.util.Set;
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
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.creature.wizard.field.FieldSpell;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Archmage's grand sigil, cut into the ground round him as he lands: a great circle of runes that sends PULSES
// rings of force running out along the ground one after another. Whoever stands on the ground as a ring passes is
// struck, slowed and tossed up; a jump in time clears it. Drawn by client/special/SigilRenderer.
public final class GrandSigil extends FieldSpell {
    public static final double REACH = 15.0;
    public static final int PULSES = 3;
    public static final int GAP = 13;
    // Ticks a ring takes to run out to the rim.
    public static final int RUN = 15;
    public static final int LIFE = GAP * (PULSES - 1) + RUN + 12;
    // How high off the ground a foe's feet must be for a ring to pass under it.
    private static final double CLEAR = 0.7;
    private final Set<Long> struck = new HashSet<>();

    public GrandSigil(EntityType<? extends GrandSigil> type, Level level) {
        super(type, level);
    }

    public static void cut(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        GrandSigil sigil = new GrandSigil(WizardEntities.SIGIL.get(), level);
        sigil.setPos(ground.x, ground.y, ground.z);
        sigil.castBy(wizard, damage);
        level.addFreshEntity(sigil);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE,
                2.5F, 0.5F);
    }

    // How far out ring `pulse` has run at `age` (below 0 before it starts, past REACH once done).
    public static double ring(int pulse, float age) {
        float t = age - pulse * GAP;
        if (t < 0.0F) {
            return -1.0;
        }
        float f = t / RUN;
        return f > 1.0F ? REACH + 1.0 : 1.2 + (REACH - 1.2) * (1.0 - (1.0 - f) * (1.0 - f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || this.tickCount > LIFE) {
            this.discard();
            return;
        }
        Vec3 at = this.position();
        for (int pulse = 0; pulse < PULSES; pulse++) {
            double outer = ring(pulse, this.tickCount);
            double inner = ring(pulse, this.tickCount - 1);
            if (outer < 0.0 || inner > REACH) {
                continue;
            }
            if (inner < 0.0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                        SoundSource.HOSTILE, 2.0F, 0.5F + 0.15F * pulse);
                ParticleFx.shockwave(level, Wizard.dust(Wizard.VIOLET, 2.0F), at.add(0.0, 0.3, 0.0), 64, 0.9);
            }
            double from = Math.max(0.0, inner) - 0.5;
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(outer + 1.0, 3.0,
                    outer + 1.0))) {
                double far = Math.hypot(living.getX() - at.x, living.getZ() - at.z);
                double over = living.getY() - at.y;
                long key = (long) living.getId() << 2 | pulse;
                if (far > outer + living.getBbWidth() * 0.5 || far < from - living.getBbWidth() * 0.5
                        || over > CLEAR || over < -2.0 || !this.struck.add(key)) {
                    continue;
                }
                Vec3 away = new Vec3(living.getX() - at.x, 0.0, living.getZ() - at.z);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                if (MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage,
                        WizardKnock.knock(away.scale(0.8).add(0.0, 0.7, 0.0)))) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), wizard);
                    Vec3 hit = living.getBoundingBox().getCenter();
                    ParticleFx.send(level, ParticleTypes.ENCHANT, hit.x, hit.y, hit.z, 20, 0.3, 0.5, 0.3, 0.6);
                }
            }
            if (this.tickCount % 2 == 0 && outer <= REACH) {
                ParticleFx.ring(level, Wizard.dust(Wizard.VIOLET, 1.6F), at.add(0.0, 0.3, 0.0), outer,
                        Mth.ceil(outer * 5.0), this.tickCount * 0.1);
            }
        }
    }
}
