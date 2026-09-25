package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import nl.tivek.multiversepowers.classes.ClassGroup;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies.Animation;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FORSAKEN_SHARDS;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;

final class DeathAndLevelUp {
    private DeathAndLevelUp() {
    }

    static Animation death(ClassGroup group) {
        return switch (group) {
            case WARRIORS -> DeathAndLevelUp::deathWarriors;
            case RANGERS -> DeathAndLevelUp::deathRangers;
            case ROGUES -> DeathAndLevelUp::deathRogues;
            case MAGES -> DeathAndLevelUp::deathMages;
            case FAITHFUL -> DeathAndLevelUp::deathFaithful;
            case ALCHEMISTS -> DeathAndLevelUp::deathAlchemists;
            case FORSAKEN -> DeathAndLevelUp::deathForsaken;
        };
    }

    static void deathWarriors(Fx fx) {
        int t = fx.age();
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.2F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.1F);
        if (t == 0) {
            fx.burstBlock(Blocks.IRON_BLOCK, 0, 0.8, 0, 20, 0.25);
            fx.cloud(ParticleTypes.CRIT, 0, 0.8, 0, 14, 0.3, 0.4, 0.3, 0.3);
            fx.sound(SoundEvents.SHIELD_BREAK, 0.9F, 0.8F);
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.6F);
        }
        double fall = Ease.smooth(Math.min(1, t / 10.0));
        double th = fall * Math.PI / 2;
        double keep = fadeOut(t, 12, 20);
        double bx = fx.lx(0, 0.6);
        double bz = fx.lz(0, 0.6);
        double dx = fx.lx(0, Math.sin(th));
        double dz = fx.lz(0, Math.sin(th));
        double dy = Math.cos(th);
        fx.line(gold, bx, FLOOR, bz, bx + dx * 0.3, FLOOR + dy * 0.3, bz + dz * 0.3, 0.07, keep);
        fx.line(steel, bx + dx * 0.3, FLOOR + dy * 0.3, bz + dz * 0.3, bx + dx * 1.4, FLOOR + dy * 1.4,
                bz + dz * 1.4, 0.07, keep);
        if (t == 10) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.4F, 1.6F);
            fx.cloud(ParticleTypes.CRIT, bx + dx, 0.15, bz + dz, 8, 0.4, 0.05, 0.4, 0.15);
        }
        double w = fx.span(0, 12);
        if (w >= 0) {
            fx.ring(steel, Mth.lerp(w, 1.4, 0.3), 0.6, 0.12, 1 - w * 0.5);
        }
    }

    static void deathRangers(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstItem(Items.FEATHER, 0, 0.6, 0, 8, 0.12);
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 1.0, 0, 12, 0.5, 0.4, 0.5, 0);
            fx.sound(SoundEvents.ITEM_BREAK, 0.7F, 0.9F);
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.5F, 0.8F);
        }
        double y = Mth.lerp(t / 20.0, 0.4, 3.0);
        for (int k = 0; k < 3; k++) {
            double a = t * 0.5 + k * 2 * Math.PI / 3;
            double r = 0.6 * (1 - t / 24.0);
            fx.at(fx.dust(0x7FD46B, 1.4F), Math.sin(a) * r, y - k * 0.2, Math.cos(a) * r);
            fx.at(ParticleTypes.COMPOSTER, Math.sin(a) * r, y - k * 0.2, Math.cos(a) * r);
        }
        fx.at(fx.dust(0xE8FFD8, 2.2F), 0, y, 0);
        if (t == 14) {
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, y, 0, 8, 0.2, 0.2, 0.2, 0);
        }
    }

    static void deathRogues(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 0.8, 0, 16, 0.3, 0.4, 0.3, 0.03);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 0.8, 0, 16, 0.12);
            fx.burstItem(Items.GOLD_NUGGET, 0, 0.6, 0, 8, 0.15);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.6F);
        }
        double w = fx.span(0, 14);
        if (w >= 0) {
            fx.ring(fx.dust(0x18181E, 2.4F), Mth.lerp(w, 0.3, 1.5), 0.2, 0.18, 1 - w);
            fx.ring(fx.dust(0x9A3AD8, 1.1F), Mth.lerp(w, 0.2, 1.2), 0.5, 0.14, 1 - w);
        }
        if (t == 8) {
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, 0.6F);
        }
        if (t > 4 && t < 20 && t % 5 == 0) {
            double a = hash(t, 51) * 2 * Math.PI;
            double tx = Math.cos(a) * 0.06;
            double tz = -Math.sin(a) * 0.06;
            double x = Math.sin(a) * 1.1;
            double z = Math.cos(a) * 1.1;
            fx.at(fx.dust(0xC060FF, 1.2F), x + tx, 1.0, z + tz);
            fx.at(fx.dust(0xC060FF, 1.2F), x - tx, 1.0, z - tz);
        }
    }

    static void deathMages(Fx fx) {
        int t = fx.age();
        double w = fx.span(0, 8);
        if (w >= 0) {
            fx.ring(fx.dust(0xA88BE8, 1.2F), Mth.lerp(w, 1.6, 0.1), 0.8, 0.1, 1);
            fx.ring(fx.dust(0x4A8CFF, 1.0F), Mth.lerp(w, 1.2, 0.1), FLOOR, 0.1, 1);
            fx.radialIn(ParticleTypes.ENCHANT, 1.4, 0.8, 6, 0.1);
        }
        if (t == 0) {
            fx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7F, 0.6F);
        }
        if (t == 8) {
            fx.sphere(fx.dust(0xA88BE8, 1.4F), 0, 0.8, 0, 0.5, 30, 1);
            fx.sphereOut(ParticleTypes.WITCH, 0, 0.8, 0, 20, 0.2);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 0.8, 0, 12, 0.1);
            fx.cloud(ParticleTypes.ENCHANT, 0, 1.2, 0, 30, 0.6, 0.6, 0.6, 0.5);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 0.7F);
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.2F);
        }
        if (t > 8) {
            fx.at(ParticleTypes.SMOKE, fx.spread(0.3), 0.8 + fx.rand() * 0.4, fx.spread(0.3));
        }
    }

    static void deathFaithful(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.cloud(fx.dust(0xFFC830, 1.4F), 0, 2.0, 0, 16, 0.3, 0.05, 0.3, 0);
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 0.5F);
            fx.sound(SoundEvents.GLASS_BREAK, 0.4F, 1.5F);
        }
        double y = Mth.lerp(t / 20.0, 0.5, 4.0);
        fx.at(fx.dust(0xFFF4C0, 2.4F), 0, y, 0);
        fx.ring(fx.dust(0xFFC830, 1.0F), 0.3, y - 0.2, 0.1, 1 - t / 20.0);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * 0.5, 0.2, Math.cos(a) * 0.5, 0, 1, 0, 0.12);
        }
        if (t == 10) {
            fx.burstItem(Items.FEATHER, 0, y, 0, 4, 0.08);
        }
    }

    static void deathAlchemists(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstItem(Items.SPLASH_POTION, 0, 0.8, 0, 12, 0.2);
            fx.burstItem(Items.GLASS_BOTTLE, 0, 0.8, 0, 6, 0.15);
            fx.sound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.GLASS_BREAK, 0.6F, 1.0F);
        }
        int[] colors = {0x5FD0C8, 0xA8FF70, 0xFF70C0, 0xF2C84B};
        if (t < 16) {
            double r = Mth.lerp(t / 16.0, 0.3, 1.5);
            for (int i = 0; i < 6; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.at(fx.dust(colors[i % 4], 2.4F), Math.sin(a) * r, 0.2 + fx.rand() * 0.8, Math.cos(a) * r);
            }
        }
        if (t == 4) {
            for (int i = 0; i < 16; i++) {
                fx.at(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colors[i % 4]),
                        fx.spread(0.8), 0.4 + fx.rand(), fx.spread(0.8));
            }
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
        }
    }

    static void deathForsaken(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.6F);
            fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.3F, 0.6F);
        }
        double w = fx.span(0, 10);
        if (w >= 0) {
            fx.sphere(fx.dust(0x18181E, 2.2F), 0, 0.8, 0, Mth.lerp(w, 1.2, 0.05), 40, 1);
            fx.radialIn(ParticleTypes.SMOKE, 1.4, 0.8, 6, 0.12);
        }
        if (t == 10) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 0.8, 0, 16, 0.15);
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.4F);
        }
        double s = fx.span(10, 20);
        if (s >= 0) {
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                double d = Mth.lerp(s, 0.2, 1.8);
                ParticleOptions shard = fx.dust(FORSAKEN_SHARDS[k], 2.0F);
                fx.at(shard, Math.sin(a) * d, 0.8 + s * 0.4, Math.cos(a) * d);
                fx.at(shard, Math.sin(a) * (d - 0.15), 0.8 + s * 0.4, Math.cos(a) * (d - 0.15));
            }
        }
    }

    static final float[] LEVEL_NOTES = {1.0F, 1.26F, 1.5F, 1.68F, 2.0F};

    static void levelUp(Fx fx) {
        int t = fx.age();
        ParticleOptions green = fx.dust(0x7FFF40, 1.2F);
        ParticleOptions gold = fx.dust(0xFFD84A, 1.2F);
        if (t < 20) {
            double top = Mth.lerp(Ease.smooth(t / 20.0), 0.1, 2.4);
            for (int k = 0; k < 2; k++) {
                for (double y = Math.max(0.1, top - 0.8); y <= top; y += 0.1) {
                    double a = y * 4 + k * Math.PI + t * 0.15;
                    fx.at(k == 0 ? green : gold, Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
                double a = top * 4 + k * Math.PI + t * 0.15;
                fx.at(ParticleTypes.END_ROD, Math.sin(a) * 0.7, top, Math.cos(a) * 0.7);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.6F, LEVEL_NOTES[t / 4]);
            }
        }
        if (t == 20) {
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.4, 0, 24, 0.14);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.4, 0, 10, 0.08);
            fx.cloud(gold, 0, 2.4, 0, 16, 0.3, 0.2, 0.3, 0);
            fx.sound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
        }
        double ring = fx.span(20, 32);
        if (ring >= 0) {
            fx.ring(green, Mth.lerp(ring, 0.3, 1.8), FLOOR, 0.12, 1 - ring * 0.6);
            fx.ring(gold, Mth.lerp(ring, 0.2, 1.4), FLOOR, 0.12, 1 - ring * 0.6);
        }
        if (t > 20) {
            double w = (t - 20) / 30.0;
            for (int k = 0; k < 8; k++) {
                double a = t * 0.12 + k * Math.PI / 4;
                if (fx.chance(1 - w)) {
                    fx.at(k % 2 == 0 ? gold : green, Math.sin(a) * 0.5, 2.3 + 0.08 * Math.sin(t * 0.4 + k),
                            Math.cos(a) * 0.5);
                }
            }
            if (fx.chance(1 - w)) {
                double a = fx.rand() * 2 * Math.PI;
                double r = 0.3 + fx.rand() * 0.8;
                fx.at(green, Math.sin(a) * r, 0.4 + fx.rand() * 1.8, Math.cos(a) * r);
            }
        }
    }
}
