package nl.tivek.multiversepowers.registry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Void Instability (Void Instabiliteit):
 * A hazardous cosmic status effect inflicted by overstaying in deep space /
 * cosmic voids.
 * Tears at the victim's dimensional cohesion, dealing escalating void damage
 * that bypasses all armor,
 * distorting their movement, and emitting unstable cosmic particles.
 */
public class VoidInstabilityEffect extends MobEffect {
    public VoidInstabilityEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A0072); // Deep cosmic void purple
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel level) {
            if (entity.tickCount % 25 == 0) { // 25% slower interval (every 1.25s)
                float damage = 0.75F + (amplifier * 0.45F); // Increased by 50%
                entity.invulnerableTime = 0;
                entity.hurt(level.damageSources().fellOutOfWorld(), damage);

                // Void instability particles
                double x = entity.getX();
                double y = entity.getY() + entity.getBbHeight() * 0.5;
                double z = entity.getZ();

                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 12, 0.4, 0.4, 0.4, 0.4);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 6, 0.3, 0.3, 0.3, 0.1);

                if (entity.tickCount % 50 == 0) {
                    level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.8F, 0.7F);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
