package nl.tivek.multiversepowers.creature.wizard.field;

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
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The frost nova, his second phase: his staff struck down, a ring of ice bursts out from under him along the ground,
// a wall of jagged ice spikes running outward. Whoever it runs over (not over the top of it: a jump in time clears it)
// is struck, frozen to the bone and slowed, and thrown outward. The spikes stand a moment and crumble. Drawn by
// client/field/NovaRenderer.
public final class FrostNova extends FieldSpell {
    public static final double REACH = 10.0;
    public static final int GROW = 14;
    public static final int LIFE = 34;
    // How high the ice wall stands over the ground: a foe whose feet are above it is not struck.
    public static final double TALL = 1.1;
    private final Set<Integer> struck = new HashSet<>();

    public FrostNova(EntityType<? extends FrostNova> type, Level level) {
        super(type, level);
    }

    public static void burst(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        FrostNova nova = new FrostNova(WizardEntities.NOVA.get(), level);
        nova.setPos(ground.x, ground.y, ground.z);
        nova.castBy(wizard, damage);
        level.addFreshEntity(nova);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 2.5F,
                0.6F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.5F,
                0.5F);
    }

    // How far out the ring has run `age` ticks in.
    public static double radius(float age) {
        float f = Mth.clamp(age / GROW, 0.0F, 1.0F);
        return REACH * (1.0 - (1.0 - f) * (1.0 - f));
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
        if (this.tickCount > GROW + 1) {
            return;
        }
        double outer = radius(this.tickCount);
        double inner = Math.max(0.0, radius(this.tickCount - 1) - 0.6);
        Vec3 at = this.position();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(outer + 1.0, 3.0,
                outer + 1.0))) {
            double far = Math.sqrt(living.distanceToSqr(at.x, living.getY(), at.z));
            double over = living.getY() - at.y;
            if (far > outer + living.getBbWidth() || far < inner - living.getBbWidth() || over > TALL || over < -2.0
                    || !this.struck.add(living.getId())) {
                continue;
            }
            Vec3 away = new Vec3(living.getX() - at.x, 0.0, living.getZ() - at.z);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            if (MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.FROST), this.damage,
                    away.scale(0.9).add(0.0, 0.5, 0.0))) {
                living.setTicksFrozen(Math.max(living.getTicksFrozen(), 260));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), wizard);
                Vec3 hit = living.getBoundingBox().getCenter();
                ParticleFx.send(level, ParticleTypes.SNOWFLAKE, hit.x, hit.y, hit.z, 20, 0.3, 0.5, 0.3, 0.05);
            }
        }
        if (this.tickCount % 2 == 0) {
            ParticleFx.ring(level, ParticleTypes.SNOWFLAKE, at.add(0.0, 0.2, 0.0), outer, 36, this.tickCount * 0.3);
            ParticleFx.ring(level, Wizard.dust(Wizard.ICE, 1.6F), at.add(0.0, 0.5, 0.0), outer, 28, 0.0);
        }
    }
}
