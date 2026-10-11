package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.HashMap;
import java.util.Map;
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
import nl.tivek.multiversepowers.creature.wizard.spell.SpellBlast;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The inferno ring, his third phase: a wall of fire leaps up in a wide ring round his foe and closes in on them. Who
// touches the wall burns; who is still inside when it has closed is caught in the pillar of fire it bursts into. The way
// out is through the wall, early, and burning for it. Drawn by client/field/InfernoRenderer.
public final class InfernoRing extends FieldSpell {
    public static final double WIDE = 7.5;
    public static final double NARROW = 1.4;
    public static final int RISE = 10;
    public static final int CLOSE = 60;
    public static final int LIFE = CLOSE + 16;
    public static final double TALL = 2.4;
    private static final double THICK = 0.7;
    private static final int SCORCH_EVERY = 10;
    private final Map<Integer, Integer> scorched = new HashMap<>();

    public InfernoRing(EntityType<? extends InfernoRing> type, Level level) {
        super(type, level);
    }

    public static void kindle(ServerLevel level, Wizard wizard, Vec3 ground, float damage) {
        InfernoRing ring = new InfernoRing(WizardEntities.RING_OF_FIRE.get(), level);
        ring.setPos(ground.x, ground.y, ground.z);
        ring.castBy(wizard, damage);
        level.addFreshEntity(ring);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F,
                0.6F);
    }

    // How far out the wall stands, and how high, `age` ticks in.
    public static double radius(float age) {
        float f = Mth.clamp((age - RISE) / (CLOSE - RISE), 0.0F, 1.0F);
        return Mth.lerp(f * f * (3.0F - 2.0F * f), WIDE, NARROW);
    }

    public static double height(float age) {
        if (age > CLOSE) {
            return TALL * Math.max(0.0, 1.0 - (age - CLOSE) / 6.0);
        }
        return TALL * Mth.clamp(age / RISE, 0.0F, 1.0F);
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
        double radius = radius(this.tickCount);
        if (this.tickCount <= CLOSE) {
            double tall = height(this.tickCount);
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(radius + 1.0, 3.0,
                    radius + 1.0))) {
                double far = Math.sqrt(living.distanceToSqr(at.x, living.getY(), at.z));
                double over = living.getY() - at.y;
                if (Math.abs(far - radius) > THICK + living.getBbWidth() * 0.5 || over > tall || over < -1.5) {
                    continue;
                }
                int now = this.tickCount;
                Integer last = this.scorched.get(living.getId());
                if (last == null || now - last >= SCORCH_EVERY) {
                    this.scorched.put(living.getId(), now);
                    living.igniteForSeconds(5.0F);
                    living.hurt(Wizard.source(this, wizard, WizardEntities.INFERNO), this.damage * 0.35F);
                }
            }
            if (this.tickCount % 2 == 0) {
                int points = (int) (radius * 7.0);
                ParticleFx.ring(level, ParticleTypes.FLAME, at.add(0.0, 0.2 + this.random.nextDouble() * tall, 0.0),
                        radius, points, this.tickCount * 0.2);
                ParticleFx.ring(level, ParticleTypes.LARGE_SMOKE, at.add(0.0, tall + 0.3, 0.0), radius, points / 3,
                        0.0);
            }
            if (this.tickCount % 15 == 0) {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
        }
        if (this.tickCount == CLOSE) {
            SpellBlast.burst(level, this, wizard, at.add(0.0, 0.5, 0.0), 2.0F, false, NARROW + 2.2, this.damage * 1.6F,
                    WizardEntities.INFERNO);
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(NARROW + 2.2, 4.0,
                    NARROW + 2.2))) {
                living.igniteForSeconds(8.0F);
            }
            ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y + 2.0, at.z, 120, 0.8, 2.5, 0.8, 0.12);
            ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 20, 1.0, 0.2, 1.0, 0.0);
            ParticleFx.shockwave(level, ParticleTypes.FLAME, at.add(0.0, 0.3, 0.0), 48, 0.6);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F,
                    0.7F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 0.4F);
        }
    }
}
