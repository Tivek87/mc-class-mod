package nl.tivek.multiversepowers.creature.wizard.special;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

// The Unbound's solar flare: he burns like a sun and WAVES walls of fire roll out from under him one after another,
// each a little faster, low enough to jump. Whoever one runs over is struck and set burning. Drawn by
// client/special/WavesRenderer.
public final class SolarWaves extends FieldSpell {
    public static final double REACH = 18.0;
    public static final int WAVES = 3;
    public static final int GAP = 12;
    // The wall's height: a foe whose feet are above it is not struck.
    public static final double TALL = 1.2;
    private static final double[] SPEED = { 0.7, 0.8, 0.9 };
    public static final int LIFE = GAP * (WAVES - 1) + 26;
    private final Set<Long> struck = new HashSet<>();

    public SolarWaves(EntityType<? extends SolarWaves> type, Level level) {
        super(type, level);
    }

    public static void roll(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        SolarWaves waves = new SolarWaves(WizardEntities.WAVES.get(), level);
        waves.setPos(ground.x, ground.y, ground.z);
        waves.castBy(wizard, damage);
        level.addFreshEntity(waves);
    }

    // How far out wave `wave` has run at `age` (below 0 before it starts).
    public static double radius(int wave, float age) {
        float t = age - wave * GAP;
        return t < 0.0F ? -1.0 : 1.0 + t * SPEED[Mth.clamp(wave, 0, WAVES - 1)];
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
        for (int wave = 0; wave < WAVES; wave++) {
            double outer = radius(wave, this.tickCount);
            double inner = radius(wave, this.tickCount - 1);
            if (outer < 0.0 || inner > REACH) {
                continue;
            }
            if (inner < 0.0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 0.4F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
                ParticleFx.sphereOut(level, ParticleTypes.FLAME, at.add(0.0, 1.5, 0.0), 50, 0.45);
            }
            double from = Math.max(0.0, inner) - 0.6;
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(outer + 1.0, 3.0,
                    outer + 1.0))) {
                double far = Math.hypot(living.getX() - at.x, living.getZ() - at.z);
                double over = living.getY() - at.y;
                long key = (long) living.getId() << 2 | wave;
                if (far > outer + living.getBbWidth() * 0.5 || far < from - living.getBbWidth() * 0.5 || over > TALL
                        || over < -2.0 || !this.struck.add(key)) {
                    continue;
                }
                Vec3 away = new Vec3(living.getX() - at.x, 0.0, living.getZ() - at.z);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                if (MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.INFERNO), this.damage,
                        WizardKnock.knock(away.scale(1.0).add(0.0, 0.55, 0.0)))) {
                    living.igniteForSeconds(5.0F);
                }
            }
            if (this.tickCount % 2 == 0 && outer <= REACH) {
                ParticleFx.ring(level, ParticleTypes.FLAME, at.add(0.0, 0.3, 0.0), outer, Mth.ceil(outer * 4.0),
                        this.tickCount * 0.2);
                if (this.tickCount % 4 == 0) {
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.4F,
                            0.6F);
                }
            }
        }
    }
}
