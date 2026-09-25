package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.ACC;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.MAIN;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.lerpColor;

final class AlchemistCeremonies {
    private AlchemistCeremonies() {
    }

    static final Glyph TRANSMUTER_SEAL = new Glyph().circle(MAIN, 1.0).polygon(MAIN, 4, 1.0, Math.PI / 4)
            .polygon(ACC, 3, 0.7, 0).circle(ACC, 0.35);

    static final int[] BREW_COLORS = {0x5FD0C8, 0xA8FF70, 0xFF70C0, 0xF2C84B, 0xFF8A20, 0xB080E8, 0x5FD0C8};

    static final Item[] INGREDIENTS = {Items.NETHER_WART, Items.GLOWSTONE_DUST, Items.SUGAR,
            Items.GHAST_TEAR, Items.BLAZE_POWDER, Items.GOLDEN_CARROT};

    static void apothecaryRite(Fx fx, double pool, int[] drops, int streamFrom, int peak, int end) {
        int t = fx.age();
        int landed = 0;
        for (int i = 0; i < drops.length; i++) {
            int drop = drops[i];
            double a = hash(i, 31) * 2 * Math.PI;
            double d = hash(i, 32) * pool * 0.6;
            double x = Math.sin(a) * d;
            double z = Math.cos(a) * d;
            ItemParticleOption item = new ItemParticleOption(ParticleTypes.ITEM,
                    new ItemStack(INGREDIENTS[i % INGREDIENTS.length]));
            if (t == drop) {
                fx.fly(item, x, 2.8, z, 0, -1, 0, 0.1);
                fx.fly(item, x, 2.9, z, 0, -1, 0, 0.1);
            }
            if (t == drop + 6) {
                fx.cloud(fx.dust(BREW_COLORS[Math.min(BREW_COLORS.length - 1, i + 1)], 1.5F), x, 0.2, z, 14, 0.3,
                        0.1, 0.3, 0);
                fx.sound(SoundEvents.GENERIC_SPLASH, 0.5F, 1.4F);
            }
            if (t >= drop + 6) {
                landed = i + 1;
            }
        }
        int color = BREW_COLORS[Math.min(BREW_COLORS.length - 1, landed)];
        if (t < streamFrom + 5) {
            for (int i = 0; i < 5; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double r = Math.sqrt(fx.rand()) * pool;
                fx.at(fx.dust(color, 1.5F), Math.sin(a) * r, FLOOR + fx.rand() * 0.3, Math.cos(a) * r);
            }
            if (fx.every(3)) {
                double a = fx.rand() * 2 * Math.PI;
                double r = Math.sqrt(fx.rand()) * pool;
                fx.ringAt(fx.dust(0xFFFFFF, 0.7F), Math.sin(a) * r, FLOOR + 0.1, Math.cos(a) * r, 0.08, 0.04, 1);
            }
            if (fx.every(2)) {
                fx.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.3F, 0.8F + (float) fx.rand() * 0.6F);
            }
        }
        double s = fx.span(streamFrom, peak);
        if (s >= 0) {
            double h = Mth.lerp(Ease.smooth(s / 0.8), 0.1, 2.9);
            for (int k = 0; k < 3; k++) {
                int n = 22;
                for (int i = 0; i <= n; i++) {
                    double f = i / (double) n;
                    double y = f * h;
                    double r = Mth.lerp(f, pool * 0.6, 0.3);
                    double a = f * 4 * Math.PI + t * 0.3 + k * 2 * Math.PI / 3;
                    fx.at(fx.dust(k == 0 ? color : BREW_COLORS[k + 1], 1.2F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
            if (s > 0.4) {
                fx.sphere(fx.dust(color, 1.3F), 0, 2.9, 0, 0.4 * (s - 0.4) / 0.6, 24, 1);
            }
            if (t == streamFrom) {
                fx.sound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.0F);
            }
        }
        if (t == peak) {
            fx.burstItem(Items.SPLASH_POTION, 0, 2.9, 0, 12, 0.18);
            for (int i = 0; i < 36; i++) {
                int c = BREW_COLORS[i % BREW_COLORS.length];
                fx.at(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | c),
                        fx.spread(1.8), 1.8 + fx.rand() * 1.4, fx.spread(1.8));
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.2, 0, 20, 0.9, 0.9, 0.9, 0);
            fx.flash(2.9);
            fx.sound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 1.0F);
        }
        if (t > peak) {
            double keep = fadeOut(t, peak + (end - peak) / 2, end);
            for (int i = 0; i < 5; i++) {
                if (fx.chance(keep)) {
                    double a = fx.rand() * 2 * Math.PI;
                    double r = fx.rand() * 1.8;
                    fx.at(fx.dust(BREW_COLORS[i], 1.1F), Math.sin(a) * r, fx.rand() * 2.6, Math.cos(a) * r);
                }
            }
            double w = (t - peak) / (double) (end - peak);
            for (int k = 0; k < 2; k++) {
                double a = w * 14 + k * Math.PI;
                fx.at(fx.fade(0x5FD0C8, 0xA8FF70, 1.3F), Math.sin(a) * 0.65, Mth.lerp(w, 2.2, 0.1), Math.cos(a) * 0.65);
            }
        }
    }

    static void apothecaryGrand(Fx fx) {
        apothecaryRite(fx, 2.2, new int[] {10, 30, 50, 70, 90, 110}, 115, 150, 200);
    }

    static void apothecaryLight(Fx fx) {
        int t = fx.age();
        int[] colors = {0x5FD0C8, 0xA8FF70, 0xFF70C0};
        for (int k = 0; k < 3; k++) {
            int thrown = k * 4;
            double a = k * 2 * Math.PI / 3 + 0.4;
            double sx = Math.sin(a) * 1.4;
            double sz = Math.cos(a) * 1.4;
            if (t == thrown) {
                fx.sound(SoundEvents.SPLASH_POTION_THROW, 0.6F, 0.9F + k * 0.1F);
            }
            double w = fx.span(thrown, thrown + 6);
            if (w >= 0) {
                double x = sx * w;
                double z = sz * w;
                double y = Mth.lerp(w, 1.4, FLOOR) + 1.0 * Math.sin(Math.PI * w);
                fx.ringAt(fx.dust(0xE0F0FF, 0.7F), x, y, z, 0.1, 0.04, 1);
                fx.at(fx.dust(colors[k], 1.5F), x, y, z);
            }
            if (t == thrown + 6) {
                fx.cloud(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colors[k]), sx, 0.3, sz,
                        10, 0.3, 0.2, 0.3, 0);
                fx.burstItem(Items.SPLASH_POTION, sx, 0.3, sz, 3, 0.1);
                fx.cloud(fx.dust(colors[k], 1.4F), sx, 0.2, sz, 10, 0.3, 0.1, 0.3, 0);
                fx.sound(SoundEvents.SPLASH_POTION_BREAK, 0.6F, 0.9F + k * 0.15F);
            }
            if (t > thrown + 6 && t < 44) {
                double m = (t - thrown - 6) / 24.0;
                double r = 1.4 * (1 - m);
                double aa = a + m * 3;
                if (fx.chance(fadeOut(t, 30, 44))) {
                    fx.at(fx.dust(colors[k], 1.3F), Math.sin(aa) * r, 0.2 + m * 1.6, Math.cos(aa) * r);
                }
            }
        }
        if (t == 20) {
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.0, 0, 12, 0.4, 0.6, 0.4, 0);
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.5F, 1.5F);
        }
    }

    static void plagueRite(Fx fx, double from, int fogEnd, int fliesFrom, int censerFrom, int peak, int end) {
        int t = fx.age();
        ParticleOptions fog = fx.dust(0x4A5A20, 3.2F);
        ParticleOptions fog2 = fx.dust(0x6A8A28, 2.6F);
        if (t < peak) {
            double r = Mth.lerp(Math.min(1, t / (double) fogEnd), from, 0.7);
            for (int i = 0; i < 7; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double d = r * (0.8 + fx.rand() * 0.4);
                fx.at(i % 2 == 0 ? fog : fog2, Math.sin(a) * d, 0.1 + fx.rand() * 0.4, Math.cos(a) * d);
            }
            if (t == 3) {
                fx.sound(SoundEvents.PUFFER_FISH_BLOW_UP, 0.6F, 0.6F);
            }
        }
        if (t >= fliesFrom && t < peak) {
            ParticleOptions fly = fx.dust(0x101010, 0.6F);
            for (int i = 0; i < 8; i++) {
                double a = t * 0.35 * (i % 2 == 0 ? 1 : -1) + i;
                double r = 0.7 + 0.3 * Math.sin(t * 0.3 + i);
                double y = 1.2 + 0.5 * Math.sin(t * 0.47 + i * 2);
                fx.at(fly, Math.sin(a) * r + fx.spread(0.05), y, Math.cos(a) * r + fx.spread(0.05));
            }
            if ((t - fliesFrom) % 7 == 0) {
                fx.sound(SoundEvents.SILVERFISH_AMBIENT, 0.2F, 2.0F);
            }
        }
        if (t >= censerFrom && t < peak) {
            double a = t * 0.3;
            double x = Math.sin(a) * 0.8;
            double z = Math.cos(a) * 0.8;
            double y = 0.9 + 0.2 * Math.sin(t * 0.6);
            fx.at(fx.dust(0xE0B45A, 1.5F), x, y, z);
            fx.line(fx.dust(0x707070, 0.7F), x, y, z, x * 0.5, 1.5, z * 0.5, 0.1, 1);
            fx.fly(ParticleTypes.WHITE_SMOKE, x, y, z, 0, 1, 0, 0.02);
            if ((t - censerFrom) % 5 == 0) {
                fx.sound(SoundEvents.CHAIN_STEP, 0.4F, 1.4F);
            }
        }
        if (t == peak) {
            fx.radial(ParticleTypes.WHITE_SMOKE, 0.4, 28, 0.25);
            fx.radial(ParticleTypes.WHITE_SMOKE, 1.2, 20, 0.2);
            fx.ring(ParticleTypes.END_ROD, 0.6, 1.0, 0.2, 1);
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.0F);
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.6F, 1.4F);
            if (fx.grand()) {
                fx.flash(1.0);
            }
        }
        if (t > peak) {
            double w = (t - peak) / (double) (end - peak);
            if (fx.every(2)) {
                fx.ring(fog, Mth.lerp(w, 0.8, from), 0.3, 0.25, 1 - w);
            }
            if (fx.chance(1 - w)) {
                fx.at(ParticleTypes.WHITE_ASH, fx.spread(1.4), 1.5 + fx.rand(), fx.spread(1.4));
            }
        }
    }

    static void plagueDoctorGrand(Fx fx) {
        plagueRite(fx, 3.4, 80, 30, 90, 150, 200);
    }

    static void plagueDoctorLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.5F, 1.4F);
        }
        for (int k = 0; k < 3; k++) {
            double w = fx.span(k * 5, k * 5 + 18);
            if (w >= 0) {
                fx.ring(fx.fade(0xE8F0E0, 0x86A83C, 1.3F), 0.2 + w * 0.6, 2.0 + w * 1.5, 0.08, 1 - w);
            }
        }
        if (t < 28) {
            double keep = fadeOut(t, 16, 28);
            for (int i = 0; i < 4; i++) {
                double a = t * 0.3 + i * Math.PI / 2;
                if (fx.chance(keep)) {
                    fx.at(fx.dust(0x86A83C, 1.0F), Math.sin(a) * 0.5, 2.0 + 0.1 * Math.sin(t * 0.5 + i),
                            Math.cos(a) * 0.5);
                }
            }
        }
        if (t < 14) {
            for (int i = 0; i < 5; i++) {
                double a = i * 2 * Math.PI / 5 + 0.5;
                double y = Mth.lerp(t / 14.0, 1.6, FLOOR);
                fx.at(fx.dust(0x101010, 0.7F), Math.sin(a) * 1.0, y, Math.cos(a) * 1.0);
            }
        }
        if (t == 14) {
            fx.radial(ParticleTypes.WHITE_SMOKE, 0.3, 14, 0.15);
        }
    }

    static double[] fusePoint(double s, double radius, double turns) {
        double r = Mth.lerp(s, radius, 0.25);
        double a = s * turns * 2 * Math.PI;
        return new double[] {Math.sin(a) * r, Math.cos(a) * r};
    }

    static void bombardierGrand(Fx fx) {
        int t = fx.age();
        double radius = 2.8;
        double turns = 3.0;
        int drawn = 20;
        int lit = 25;
        int flashFrom = 138;
        int peak = 150;
        ParticleOptions fuse = fx.dust(0x505058, 1.1F);
        ParticleOptions ash = fx.dust(0x202020, 0.9F);
        double length = turns * 2 * Math.PI * (radius + 0.25) / 2;
        int points = (int) Math.ceil(length / 0.1);
        double front = t < lit ? 0 : Mth.clamp((t - lit) / (double) (flashFrom - lit), 0, 1);
        double shown = t < drawn ? t / (double) drawn : 1;
        if (t < peak && (t < drawn || fx.every(2))) {
            for (int i = 0; i <= points; i++) {
                double s = i / (double) points;
                if (s > shown) {
                    break;
                }
                double[] p = fusePoint(s, radius, turns);
                if (s >= front) {
                    fx.at(fuse, p[0], FLOOR, p[1]);
                } else if (fx.chance(0.12)) {
                    fx.at(ash, p[0], FLOOR, p[1]);
                }
            }
        }
        if (t == lit) {
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.0F);
            fx.sound(SoundEvents.TNT_PRIMED, 0.6F, 1.2F);
        }
        if (t >= lit && t < flashFrom) {
            double[] p = fusePoint(front, radius, turns);
            fx.at(ParticleTypes.FLAME, p[0], FLOOR + 0.05, p[1]);
            fx.cloud(ParticleTypes.SMALL_FLAME, p[0], FLOOR + 0.05, p[1], 2, 0.03, 0.03, 0.03, 0.02);
            fx.fly(ParticleTypes.SMOKE, p[0], FLOOR + 0.1, p[1], 0, 1, 0, 0.03);
            if (fx.every(3)) {
                fx.at(ParticleTypes.LAVA, p[0], FLOOR + 0.1, p[1]);
                fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.15F, 1.8F);
            }
        }
        if (t >= 45 && t < 128 && t % 4 == 1) {
            double a = hash(t, 41) * 2 * Math.PI;
            double d = 1.0 + hash(t, 42) * 1.8;
            fx.cloud(ParticleTypes.FIREWORK, Math.sin(a) * d, 0.3, Math.cos(a) * d, 6, 0.05, 0.05, 0.05, 0.1);
            fx.cloud(ParticleTypes.CRIT, Math.sin(a) * d, 0.3, Math.cos(a) * d, 4, 0.05, 0.05, 0.05, 0.2);
            fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.3F, 1.5F + (float) hash(t, 43) * 0.4F);
        }
        if (t >= flashFrom && t < peak && fx.every(2)) {
            fx.sphere(fx.dust(0xFFFFFF, 1.4F), 0, 1.0, 0, 0.6, 30, 1);
        }
        if (t == flashFrom) {
            fx.sound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        }
        if (t == peak) {
            fx.at(ParticleTypes.EXPLOSION, 0, 2.4, 0);
            fx.at(ParticleTypes.EXPLOSION, 0.6, 2.0, 0.4);
            fx.at(ParticleTypes.EXPLOSION, -0.5, 1.9, -0.5);
            fx.radial(ParticleTypes.LARGE_SMOKE, 0.4, 24, 0.16);
            fx.burstItem(Items.GUNPOWDER, 0, 2.0, 0, 14, 0.2);
            fx.groundDebris(18, radius);
            fx.cloud(ParticleTypes.LAVA, 0, 2.0, 0, 8, 0.4, 0.3, 0.4, 0);
            fx.flash(2.0);
            fx.sound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.0F);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            if (w < 0.7 && fx.every(2)) {
                fx.ring(fx.dust(0x404048, 2.2F), Mth.lerp(w / 0.7, 0.6, radius), 0.3, 0.2, 1 - w / 0.7);
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.2, 0.6, 1.2, 0);
            }
        }
    }

    static void bombardierLight(Fx fx) {
        int t = fx.age();
        for (int k = 0; k < 6; k++) {
            int thrown = k * 3;
            double a = k * Math.PI / 3 + 0.2;
            double sx = Math.sin(a) * 1.5;
            double sz = Math.cos(a) * 1.5;
            if (t == thrown) {
                fx.sound(SoundEvents.EGG_THROW, 0.5F, 0.6F);
            }
            double w = fx.span(thrown, thrown + 6);
            if (w >= 0) {
                double y = Mth.lerp(w, 1.4, FLOOR + 0.1) + 1.0 * Math.sin(Math.PI * w);
                fx.at(fx.dust(0x303038, 2.2F), sx * w, y, sz * w);
                fx.at(ParticleTypes.SMALL_FLAME, sx * w, y + 0.15, sz * w);
            }
            if (t == thrown + 6) {
                fx.cloud(ParticleTypes.POOF, sx, 0.3, sz, 6, 0.1, 0.1, 0.1, 0.05);
                fx.cloud(ParticleTypes.FLAME, sx, 0.3, sz, 6, 0.1, 0.1, 0.1, 0.06);
                fx.at(ParticleTypes.LAVA, sx, 0.3, sz);
                fx.debrisAt(sx, sz, 3);
                fx.sound(SoundEvents.GENERIC_EXPLODE, 0.25F, 1.6F);
            }
            if (t > thrown + 6 && t < 44 && fx.every(3)) {
                fx.fly(ParticleTypes.SMOKE, sx, 0.2, sz, 0, 1, 0, 0.03);
            }
        }
    }

    static void transmuterGrand(Fx fx) {
        int t = fx.age();
        int traceEnd = 60;
        int collapse = 144;
        int peak = 150;
        double spin = t <= collapse ? 0.02 * t + 0.0016 * t * t : 0.02 * collapse + 0.0016 * collapse * collapse;
        double color = t / (double) collapse;
        int rgb = color < 0.5 ? lerpColor(color * 2, 0x7A7A82, 0xD08050) : lerpColor(color * 2 - 1, 0xD08050, 0xF2C84B);
        ParticleOptions metal = fx.dust(rgb, 1.1F);
        if (t < peak) {
            double shrink = t < collapse ? 1 : 1 - (t - collapse) / (double) (peak - collapse) * 0.85;
            for (int k = 0; k < 3; k++) {
                double r = (1.2 + k * 0.3) * shrink;
                double[] n = switch (k) {
                    case 0 -> new double[] {Math.sin(spin), 0, Math.cos(spin)};
                    case 1 -> new double[] {0, Math.cos(spin * 1.3), Math.sin(spin * 1.3)};
                    default -> new double[] {Math.cos(spin * 0.8), Math.sin(spin * 0.8), 0};
                };
                double part = Mth.clamp((t - k * 20) / 20.0, 0, 1);
                if (part > 0) {
                    fx.ring3(metal, 0, 1.1, 0, r, n[0], n[1], n[2], 0, 2 * Math.PI * part, 0.08, 1);
                }
                if (t > traceEnd && t < collapse) {
                    int[] elements = {0xE04030, 0x4080FF, 0x40C050};
                    double[] p = Fx.ring3Point(0, 1.1, 0, r, n[0], n[1], n[2], t * 0.3 + k * 2);
                    fx.at(fx.dust(elements[k], 2.0F), p[0], p[1], p[2]);
                    fx.at(ParticleTypes.WAX_ON, p[0], p[1], p[2]);
                }
            }
            if (t > traceEnd && t < collapse && t % 6 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.7F + (float) color);
            }
            if (t == 72 || t == 120) {
                fx.sound(SoundEvents.SMITHING_TABLE_USE, 0.6F, 1.2F);
                fx.cloud(fx.dust(rgb, 1.5F), 0, 1.1, 0, 20, 0.8, 0.8, 0.8, 0);
            }
        }
        if (t == peak) {
            fx.burstItem(Items.GOLD_INGOT, 0, 1.2, 0, 12, 0.22);
            fx.burstItem(Items.GOLD_NUGGET, 0, 1.2, 0, 18, 0.22);
            fx.burstItem(Items.EMERALD, 0, 1.2, 0, 5, 0.2);
            fx.cloud(ParticleTypes.WAX_ON, 0, 1.2, 0, 24, 0.6, 0.6, 0.6, 0);
            fx.sphere(fx.dust(0xF2C84B, 1.5F), 0, 1.1, 0, 0.7, 40, 1);
            fx.flash(1.2);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.ANVIL_USE, 0.3F, 1.8F);
            TRANSMUTER_SEAL.draw(fx, ParticleTypes.END_ROD, ParticleTypes.END_ROD, 2.2, FLOOR, 0, 0, 1, 0.6, 0.2);
        }
        if (t > peak && fx.every(2)) {
            TRANSMUTER_SEAL.draw(fx, fx.dust(0xF2C84B, 1.0F), fx.dust(0xB48CFF, 1.0F), 2.2, FLOOR,
                    (t - peak) * 0.01, 0, 1, fadeOut(t, 170, 200), 0.12);
        }
    }

    static void transmuterLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstBlock(Blocks.IRON_BLOCK, 0, 0.2, 0, 10, 0.1);
            fx.sound(SoundEvents.SMITHING_TABLE_USE, 0.7F, 1.3F);
        }
        if (t < 16) {
            double r = Mth.lerp(Ease.smooth(t / 16.0), 0.2, 1.6);
            fx.ring(fx.dust(0xF2C84B, 1.3F), r, FLOOR, 0.08, 1);
            fx.ring(fx.dust(0xB48CFF, 0.9F), r + 0.1, FLOOR, 0.12, 0.6);
            for (int i = 0; i < 10; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double d = Math.sqrt(fx.rand()) * r;
                fx.at(fx.dust(0xF2C84B, 1.0F), Math.sin(a) * d, FLOOR, Math.cos(a) * d);
            }
            for (int i = 0; i < 2; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.burstBlock(Blocks.GOLD_BLOCK, Math.sin(a) * r, 0.1, Math.cos(a) * r, 2, 0.08);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.8F + t * 0.05F);
            }
        }
        if (t == 16) {
            fx.burstItem(Items.GOLD_NUGGET, 0, 0.3, 0, 12, 0.25);
            fx.cloud(ParticleTypes.WAX_ON, 0, 0.8, 0, 16, 0.8, 0.6, 0.8, 0);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.0F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            TRANSMUTER_SEAL.draw(fx, fx.dust(0xF2C84B, 1.0F), fx.dust(0xB48CFF, 1.0F), 1.6, FLOOR, 0, 0, 1,
                    fadeOut(t, 26, 42), 0.12);
        }
    }
}
