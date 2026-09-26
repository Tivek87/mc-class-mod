package nl.tivek.multiversepowers.spell.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.spell.SpellFxPayload;

// Particles every game makes for itself round a drawn spell effect, so none of them cross the network.
final class Particles {
    private Particles() {
    }

    static void tick(ClientLevel level, SpellFx.Fx fx, double age) {
        RandomSource random = level.getRandom();
        Vec3 from = fx.said.from();
        switch (fx.kind()) {
            case SpellFxPayload.FIREBALL -> {
                if (fx.lost == 0) {
                    for (int k = 0; k < 3; k++) {
                        puff(level, random, random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                                fx.at, 0.2, 0.02);
                    }
                    puff(level, random, ParticleTypes.SMOKE, fx.was, 0.15, 0.01);
                }
            }
            case SpellFxPayload.FIRE_BURST -> {
                if (age < 1.0) {
                    for (int k = 0; k < 40; k++) {
                        puff(level, random, ParticleTypes.FLAME, from, 0.4, 0.25);
                    }
                } else if (age < 30.0 && random.nextFloat() < 0.6) {
                    puff(level, random, ParticleTypes.SMALL_FLAME, from.add(0.0, 0.1, 0.0), 1.3, 0.03);
                }
            }
            case SpellFxPayload.STORM -> {
                for (int k = 0; k < 2; k++) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double reach = Math.sqrt(random.nextDouble()) * 2.2;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, from.x + Math.cos(angle) * reach, from.y + 0.1,
                            from.z + Math.sin(angle) * reach, 0.0, 0.15, 0.0);
                }
            }
            case SpellFxPayload.BOLT -> {
                if (age < 1.0) {
                    level.setSkyFlashTime(2);
                    for (int k = 0; k < 50; k++) {
                        puff(level, random, ParticleTypes.ELECTRIC_SPARK, fx.said.to(), 0.4, 0.6);
                    }
                    for (int k = 0; k < 12; k++) {
                        puff(level, random, ParticleTypes.LARGE_SMOKE, fx.said.to(), 0.6, 0.05);
                    }
                }
            }
            case SpellFxPayload.POISON -> {
                double radius = fx.said.to().x;
                if (random.nextFloat() < 0.5) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double reach = Math.sqrt(random.nextDouble()) * radius;
                    level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, from.x + Math.cos(angle) * reach,
                            from.y + 2.2, from.z + Math.sin(angle) * reach, 0.0, 0.0, 0.0);
                }
            }
            case SpellFxPayload.GUST -> {
                if (age < 8.0) {
                    Vec3 way = fx.said.to();
                    Vec3 front = from.add(way.scale(age + 1.0));
                    for (int k = 0; k < 3; k++) {
                        level.addParticle(ParticleTypes.CLOUD, front.x + (random.nextDouble() - 0.5) * 3.0,
                                front.y + (random.nextDouble() - 0.5), front.z + (random.nextDouble() - 0.5) * 3.0,
                                way.x * 0.3, way.y * 0.3, way.z * 0.3);
                    }
                }
            }
            case SpellFxPayload.CLAP -> {
                Vec3 feet = fx.said.to();
                if (age < 1.0) {
                    level.addParticle(ParticleTypes.FLASH, from.x, from.y, from.z, 0.0, 0.0, 0.0);
                    for (int k = 0; k < 60; k++) {
                        puff(level, random, ParticleTypes.ELECTRIC_SPARK, from, 0.3, 0.7);
                    }
                    for (int k = 0; k < 24; k++) {
                        double angle = Math.PI * 2.0 * k / 24.0;
                        level.addParticle(ParticleTypes.CLOUD, feet.x, feet.y + 0.2, feet.z, Math.cos(angle) * 0.5,
                                0.0, Math.sin(angle) * 0.5);
                    }
                }
                int sparkles = (int) (10.0 * (1.0 - age / StormFx.CLAP)) + 1;
                for (int k = 0; k < sparkles; k++) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double reach = Math.sqrt(random.nextDouble()) * StormFx.CLAP_REACH;
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, feet.x + Math.cos(angle) * reach,
                            feet.y + 0.1 + random.nextDouble() * 1.5, feet.z + Math.sin(angle) * reach, 0.0, 0.08,
                            0.0);
                }
            }
            case SpellFxPayload.VOID_IN, SpellFxPayload.VOID_OUT -> {
                if (age < 10.0) {
                    for (int k = 0; k < 6; k++) {
                        puff(level, random, ParticleTypes.REVERSE_PORTAL, from.add(0.0, 1.0, 0.0), 0.8, 0.15);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void puff(ClientLevel level, RandomSource random, ParticleOptions particle, Vec3 at,
            double spread, double speed) {
        level.addParticle(particle, at.x + (random.nextDouble() - 0.5) * spread,
                at.y + (random.nextDouble() - 0.5) * spread, at.z + (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * speed * 2.0, (random.nextDouble() - 0.5) * speed * 2.0,
                (random.nextDouble() - 0.5) * speed * 2.0);
    }
}
