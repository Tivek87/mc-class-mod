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
// he flies, fire licking off him once he is unbound, the void smoking off him once he has woken it, stars streaming
// from him at the last), stars round his head while he is stunned, and the shards of his ward as it breaks.
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
        double hover = body instanceof Wizard wizard ? wizard.hover()
                : body instanceof WizardDecoy decoy ? decoy.hover() : 0.0;
        Vec3 middle = feet.add(0.0, 1.0 + hover, 0.0);
        if (move == 0 && random.nextInt(5) == 0) {
            level.addParticle(Wizard.dust(Wizard.light(phase), 0.6F), crystal.x + spread(random, 0.3),
                    crystal.y + spread(random, 0.3), crystal.z + spread(random, 0.3), 0.0, 0.0, 0.0);
        }
        if (hover > 0.0 && random.nextInt(3) == 0) {
            level.addParticle(Wizard.dust(Wizard.light(phase), 0.9F), feet.x + spread(random, 0.6), feet.y + 0.1
                    + random.nextDouble() * 0.5, feet.z + spread(random, 0.6), 0.0, -0.02, 0.0);
        }
        if (phase == 3 && random.nextInt(2) == 0) {
            level.addParticle(random.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                    middle.x + spread(random, 0.7), middle.y + spread(random, 1.2), middle.z + spread(random, 0.7),
                    0.0, 0.04, 0.0);
        }
        if (phase == 4) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.SQUID_INK, middle.x + spread(random, 0.8), middle.y + spread(random,
                        1.4), middle.z + spread(random, 0.8), 0.0, 0.02, 0.0);
            }
            level.addParticle(Wizard.dust(Wizard.VOID, 0.8F), middle.x + spread(random, 0.9),
                    middle.y + spread(random, 1.6), middle.z + spread(random, 0.9), 0.0, 0.0, 0.0);
        }
        if (phase == 5) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.END_ROD, middle.x + spread(random, 0.8), middle.y + spread(random,
                        1.4), middle.z + spread(random, 0.8), 0.0, -0.02, 0.0);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.FIREWORK, middle.x + spread(random, 1.4), middle.y + 0.8 + spread(random,
                        1.0), middle.z + spread(random, 1.4), 0.0, 0.0, 0.0);
            }
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
            case WizardMove.ASCEND, WizardMove.UNBIND, WizardMove.AWAKEN, WizardMove.APOTHEOSIS -> {
                if (gathering) {
                    int light = Wizard.light(WizardMove.carries(move));
                    spiral(level, feet.add(0.0, (t % 30) * 0.08, 0.0), 1.8 - t * 0.02, age * 2.0F,
                            Wizard.dust(light, 1.4F), 6);
                    spiral(level, middle.add(0.0, 1.0 - (t % 20) * 0.1, 0.0), 1.2, -age * 2.6F,
                            Wizard.dust(0xFFFFFF, 0.9F), 3);
                }
            }
            case WizardMove.ORBIT, WizardMove.LANCES, WizardMove.SPIRAL -> {
                if (gathering) {
                    int light = WizardMove.colour(move);
                    spiral(level, crystal, 1.0 - Math.min(0.7, t * 0.05), age * 1.7F, Wizard.dust(light, 1.0F), 3);
                    level.addParticle(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, spread(random, 0.05),
                            spread(random, 0.05), spread(random, 0.05));
                }
            }
            case WizardMove.NOVA -> {
                if (gathering) {
                    spiral(level, crystal, 0.7, age * 2.4F, ParticleTypes.SNOWFLAKE, 3);
                    spiral(level, feet.add(0.0, 0.2, 0.0), 2.2 - t * 0.1, -age * 1.6F, Wizard.dust(Wizard.ICE, 1.2F),
                            4);
                }
            }
            case WizardMove.TORNADO -> {
                if (gathering) {
                    spiral(level, hand.add(0.0, (t % 8) * 0.1, 0.0), 0.6, age * 3.0F, Wizard.dust(Wizard.VIOLET, 1.0F),
                            3);
                    level.addParticle(ParticleTypes.CLOUD, hand.x, hand.y, hand.z, spread(random, 0.1), 0.05,
                            spread(random, 0.1));
                }
            }
            case WizardMove.INFERNO, WizardMove.COMET -> {
                if (move == WizardMove.COMET || gathering) {
                    spiral(level, hand, 0.4, age * 2.0F, ParticleTypes.FLAME, 2);
                    level.addParticle(ParticleTypes.FLAME, middle.x + spread(random, 0.8), middle.y + spread(random,
                            1.6), middle.z + spread(random, 0.8), 0.0, 0.06, 0.0);
                }
            }
            case WizardMove.PHANTOM -> {
                for (int k = 0; k < 2; k++) {
                    level.addParticle(ParticleTypes.REVERSE_PORTAL, middle.x + spread(random, 1.0),
                            middle.y + spread(random, 2.0), middle.z + spread(random, 1.0), 0.0, 0.0, 0.0);
                }
            }
            case WizardMove.TENDRILS, WizardMove.VOID_ORB, WizardMove.DRAIN -> {
                if (gathering) {
                    spiral(level, move == WizardMove.DRAIN ? hand : crystal, 0.9 - Math.min(0.6, t * 0.04),
                            age * 1.9F, Wizard.dust(Wizard.VOID, 1.1F), 3);
                    level.addParticle(ParticleTypes.SQUID_INK, hand.x, hand.y, hand.z, 0.0, -0.02, 0.0);
                }
            }
            case WizardMove.SUPERNOVA -> {
                if (t < WizardMove.release(move)) {
                    level.addParticle(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, spread(random, 0.1),
                            0.3 + random.nextDouble() * 0.2, spread(random, 0.1));
                }
            }
            case WizardMove.AIRSTEP -> {
                for (int k = 0; k < 4; k++) {
                    level.addParticle(Wizard.dust(Wizard.light(phase), 1.0F), middle.x + spread(random, 0.8),
                            middle.y + spread(random, 1.8), middle.z + spread(random, 0.8), 0.0, 0.0, 0.0);
                }
            }
            case WizardMove.STUNNED -> {
                Vec3 head = middle.add(0.0, 1.2, 0.0);
                spiral(level, head, 0.45, age * 2.5F, ParticleTypes.CRIT, 2);
            }
            case WizardMove.STRIKE -> {
                // The crystal's arc as the staff swings through.
                if (Math.abs(t - release) < 4.0) {
                    level.addParticle(Wizard.dust(Wizard.light(phase), 1.1F), crystal.x, crystal.y, crystal.z, 0.0,
                            0.0, 0.0);
                    level.addParticle(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, spread(random, 0.05),
                            spread(random, 0.05), spread(random, 0.05));
                }
            }
            case WizardMove.SKIP -> {
                // A streak of violet after-images where he glides from, folding into his robe's hem.
                for (int k = 0; k < 3; k++) {
                    level.addParticle(Wizard.dust(Wizard.VIOLET, 0.9F + 0.3F * k), middle.x + spread(random, 0.5),
                            middle.y + spread(random, 1.6), middle.z + spread(random, 0.5), 0.0, 0.0, 0.0);
                }
                level.addParticle(ParticleTypes.REVERSE_PORTAL, feet.x, feet.y + 0.1, feet.z, spread(random, 0.2),
                        0.02, spread(random, 0.2));
            }
            case WizardMove.ARRIVE -> {
                // Coming down: glyphs whirl round him and light rains past him to the ground.
                if (t < WizardMove.LAND) {
                    spiral(level, middle, 1.3, age * 1.8F, ParticleTypes.ENCHANT, 3);
                    spiral(level, middle.add(0.0, 0.8, 0.0), 0.9, -age * 2.4F, Wizard.dust(Wizard.light(phase),
                            1.0F), 2);
                    level.addParticle(ParticleTypes.END_ROD, middle.x + spread(random, 1.8), middle.y + 2.0
                            + random.nextDouble() * 2.0, middle.z + spread(random, 1.8), 0.0, -0.18, 0.0);
                }
            }
            case WizardMove.DEATH -> {
                // Motes of his light leaving him, more and more, and drawn back in before he is gone.
                if (t < WizardMove.GONE - 14) {
                    if (random.nextFloat() < 0.3F + (float) t / WizardMove.GONE) {
                        level.addParticle(Wizard.dust(Wizard.light(phase), 0.7F), middle.x + spread(random, 0.7),
                                middle.y + spread(random, 1.4), middle.z + spread(random, 0.7), 0.0, 0.03, 0.0);
                    }
                } else if (t < WizardMove.GONE) {
                    spiral(level, middle.add(0.0, 0.6, 0.0), 1.6 - (t - WizardMove.GONE + 14) * 0.1, age * 3.0F,
                            ParticleTypes.ENCHANT, 3);
                }
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

    // His ward breaks, as this game sees it: shards of it flying out from all round him, falling as they go.
    static void shatter(Wizard wizard) {
        Level level = wizard.level();
        RandomSource random = wizard.getRandom();
        Vec3 middle = wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
        int light = Wizard.light(wizard.phase());
        for (int k = 0; k < 70; k++) {
            Vec3 way = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = middle.add(way.scale(1.7));
            Vec3 fly = way.scale(0.25 + random.nextDouble() * 0.25);
            level.addParticle(k % 3 == 0 ? ParticleTypes.END_ROD : Wizard.dust(light, 1.4F), at.x, at.y, at.z, fly.x,
                    fly.y, fly.z);
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
