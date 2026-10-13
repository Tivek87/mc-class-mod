package nl.tivek.multiversepowers.creature.elemental.spell;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The fire a scorch ball leaves where it bursts: a patch of flames on the ground for four seconds that sets alight
// and burns whoever stands in it. Only the flames' light: the blocks stay as they are.
final class ScorchPatch implements Effect {
    private static final int LIFE = 80;
    private static final double RADIUS = 1.8;
    private final Vec3 at;
    @Nullable
    private final Entity owner;

    ScorchPatch(Vec3 at, @Nullable Entity owner) {
        this.at = at;
        this.owner = owner;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (!level.isLoaded(BlockPos.containing(this.at))) {
            return false;
        }
        double fade = 1.0 - age / (double) LIFE;
        if (age % 3 == 0) {
            ParticleFx.send(level, ParticleTypes.FLAME, this.at.x, this.at.y + 0.1, this.at.z,
                    (int) (3 + 6 * fade), RADIUS * 0.5, 0.05, RADIUS * 0.5, 0.01);
            ParticleFx.send(level, ParticleTypes.SMALL_FLAME, this.at.x, this.at.y + 0.2, this.at.z, 2, RADIUS * 0.4,
                    0.1, RADIUS * 0.4, 0.02);
        }
        if (age % 7 == 0) {
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, this.at.x, this.at.y + 0.3, this.at.z, 1, RADIUS * 0.4,
                    0.1, RADIUS * 0.4, 0.01);
        }
        if (age % 20 == 0) {
            level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE,
                    1.0F, 0.8F + level.random.nextFloat() * 0.3F);
        }
        if (age % 10 == 5) {
            Entity caster = this.owner != null && this.owner.isRemoved() ? null : this.owner;
            AABB box = new AABB(this.at.x - RADIUS, this.at.y - 0.5, this.at.z - RADIUS, this.at.x + RADIUS,
                    this.at.y + 1.5, this.at.z + RADIUS);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                    living -> BlazeCasts.hits(caster, living) && !living.fireImmune())) {
                living.igniteForSeconds(3.0F);
                // Fire on the ground burns, it throws no one back.
                Vec3 motion = living.getDeltaMovement();
                living.hurt(caster == null ? level.damageSources().inFire() : BlazeCasts.source(caster, caster,
                        Element.FIRE), 1.0F);
                living.setDeltaMovement(motion);
            }
        }
        return age < LIFE;
    }
}
