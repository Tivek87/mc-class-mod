package nl.tivek.multiversepowers.creature.wizard;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The light of the Wizard's spells as they gather, made in each game that sees him (or one of his images): motes drawn
// spiralling into his crystal, fire winding round his left hand, snow whirling, lightning crackling, stars streaming up
// from the raised staff, the light of the beam falling in on the crystal, his phase's glow at rest (motes under him as
// he floats, violet-gold fire licking off him once he is unbound) and stars round his head while he is stunned.
final class WizardSparks {
    private WizardSparks() {
    }

    static void of(MovingMonster body, int phase, Vec3 crystal, Vec3 hand) {
        Level level = body.level();
        RandomSource random = body.getRandom();
        int move = body.move();
        double t = body.moveAge(0.0F);
        float age = body.tickCount;
        int release = WizardMove.release(move);
        boolean gathering = move != 0 && t < release;
        Vec3 feet = body.position();
        Vec3 middle = feet.add(0.0, 1.0 + (phase > 1 ? Wizard.HOVER : 0.0), 0.0);
        if (move == 0 && random.nextInt(5) == 0) {
            level.addParticle(Wizard.dust(Wizard.light(phase), 0.6F), crystal.x + spread(random, 0.3),
                    crystal.y + spread(random, 0.3), crystal.z + spread(random, 0.3), 0.0, 0.0, 0.0);
        }
        if (phase > 1 && random.nextInt(3) == 0) {
            level.addParticle(Wizard.dust(Wizard.light(phase), 0.9F), feet.x + spread(random, 0.6), feet.y + 0.1
                    + random.nextDouble() * 0.5, feet.z + spread(random, 0.6), 0.0, -0.02, 0.0);
        }
        if (phase > 2 && random.nextInt(2) == 0) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME,
                    middle.x + spread(random, 0.7), middle.y + spread(random, 1.2), middle.z + spread(random, 0.7),
                    0.0, 0.04, 0.0);
        }
        switch (move) {
            case WizardMove.SPARK, WizardMove.BOLT -> {
                if (gathering) {
                    spiral(level, crystal, 0.8 - Math.min(0.6, t * 0.06), age, Wizard.dust(Wizard.VIOLET, 0.8F), 3);
                    level.addParticle(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, spread(random, 0.04),
                            spread(random, 0.04), spread(random, 0.04));
                }
            }
            case WizardMove.FIREBALL -> {
                if (gathering) {
                    double grow = Math.min(1.0, t / release);
                    spiral(level, hand, 0.15 + 0.35 * grow, age * 1.6F, ParticleTypes.FLAME, 2);
                    level.addParticle(ParticleTypes.SMALL_FLAME, hand.x, hand.y, hand.z, 0.0, 0.02, 0.0);
                }
            }
            case WizardMove.BLIZZARD -> {
                if (gathering) {
                    spiral(level, crystal, 0.6, age * 2.2F, ParticleTypes.SNOWFLAKE, 3);
                    spiral(level, middle, 1.4, -age * 1.4F, Wizard.dust(Wizard.ICE, 1.0F), 2);
                }
            }
            case WizardMove.BLINK -> {
                if (gathering) {
                    for (int k = 0; k < 4; k++) {
                        level.addParticle(ParticleTypes.REVERSE_PORTAL, middle.x + spread(random, 1.6),
                                middle.y + spread(random, 2.2), middle.z + spread(random, 1.6), 0.0, 0.0, 0.0);
                    }
                }
            }
            case WizardMove.CHAINS, WizardMove.RUNES -> {
                if (gathering) {
                    spiral(level, feet.add(0.0, 0.15, 0.0), 1.4, age, ParticleTypes.ENCHANT, 4);
                    level.addParticle(Wizard.dust(Wizard.VIOLET, 1.0F), hand.x, hand.y, hand.z, 0.0, 0.03, 0.0);
                }
            }
            case WizardMove.LIGHTNING -> {
                if (gathering) {
                    for (int k = 0; k < 3; k++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, crystal.x + spread(random, 0.6),
                                crystal.y + spread(random, 0.6), crystal.z + spread(random, 0.6), 0.0, 0.0, 0.0);
                    }
                    level.addParticle(Wizard.dust(Wizard.STORM, 0.9F), crystal.x, crystal.y, crystal.z, 0.0, 0.0,
                            0.0);
                }
            }
            case WizardMove.STARFALL, WizardMove.METEOR -> {
                if (t < WizardMove.length(move) - 6) {
                    ParticleOptions rising = move == WizardMove.METEOR ? ParticleTypes.FLAME : ParticleTypes.END_ROD;
                    for (int k = 0; k < 2; k++) {
                        level.addParticle(rising, crystal.x, crystal.y, crystal.z, spread(random, 0.08),
                                0.45 + random.nextDouble() * 0.3, spread(random, 0.08));
                    }
                    spiral(level, feet.add(0.0, 0.1 + (t % 20) * 0.1, 0.0), 1.2, age,
                            Wizard.dust(WizardMove.colour(move), 1.0F), 4);
                }
            }
            case WizardMove.LIFT -> {
                if (t < WizardMove.SLAM) {
                    spiral(level, hand.add(0.0, (t % 10) * 0.08, 0.0), 0.5, age * 1.4F,
                            Wizard.dust(Wizard.GRAVITY, 1.0F), 3);
                }
            }
            case WizardMove.FAMILIARS -> {
                if (gathering) {
                    spiral(level, middle.add(0.0, 1.2, 0.0), 1.6, age * 1.2F, Wizard.dust(Wizard.VIOLET, 1.2F), 3);
                }
            }
            case WizardMove.BEAM -> {
                if (gathering) {
                    double r = 2.4 * (1.0 - t / release) + 0.3;
                    spiral(level, crystal, r, age * 1.8F, Wizard.dust(Wizard.VIOLET, 1.3F), 4);
                    level.addParticle(ParticleTypes.END_ROD, crystal.x + spread(random, r), crystal.y
                            + spread(random, r), crystal.z + spread(random, r), 0.0, 0.0, 0.0);
                }
            }
            case WizardMove.WARP -> {
                if (gathering) {
                    spiral(level, middle, 1.5, age * 0.7F, Wizard.dust(Wizard.TIME, 1.2F), 4);
                }
            }
            case WizardMove.ASCEND, WizardMove.UNBIND -> {
                if (gathering) {
                    int light = Wizard.light(move == WizardMove.ASCEND ? 2 : 3);
                    spiral(level, feet.add(0.0, (t % 30) * 0.08, 0.0), 1.8 - t * 0.02, age * 2.0F,
                            Wizard.dust(light, 1.4F), 6);
                }
            }
            case WizardMove.STUNNED -> {
                Vec3 head = middle.add(0.0, 1.2, 0.0);
                spiral(level, head, 0.45, age * 2.5F, ParticleTypes.CRIT, 2);
            }
            case WizardMove.GLIDE, WizardMove.DASH -> {
                level.addParticle(Wizard.dust(Wizard.STARLIGHT, move == WizardMove.DASH ? 1.4F : 0.8F),
                        middle.x + spread(random, 0.3), middle.y + spread(random, 0.8), middle.z + spread(random, 0.3),
                        0.0, 0.0, 0.0);
            }
            default -> {
            }
        }
    }

    private static double spread(RandomSource random, double range) {
        return (random.nextDouble() - 0.5) * range;
    }

    // Particles on a ring round `at` that turns with `angle`, moving in towards it.
    private static void spiral(Level level, Vec3 at, double radius, float angle, ParticleOptions particle,
            int count) {
        for (int k = 0; k < count; k++) {
            float a = angle * 0.35F + k * Mth.TWO_PI / count;
            double x = Mth.cos(a) * radius;
            double z = Mth.sin(a) * radius;
            level.addParticle(particle, at.x + x, at.y + (k % 2 == 0 ? 0.05 : -0.05), at.z + z, -x * 0.12, 0.0,
                    -z * 0.12);
        }
    }

    // Where he blinks out and where he comes back: a burst of violet and the dark folding in.
    static void blink(ServerLevel level, Vec3 from, Vec3 to) {
        for (Vec3 at : new Vec3[] { from, to }) {
            ParticleFx.send(level, ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0, at.z, 40, 0.4, 0.8, 0.4, 0.4);
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.4F), at.x, at.y + 1.0, at.z, 20, 0.4, 0.9, 0.4, 0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 8, 0.3, 0.6, 0.3, 0.08);
        }
    }
}
