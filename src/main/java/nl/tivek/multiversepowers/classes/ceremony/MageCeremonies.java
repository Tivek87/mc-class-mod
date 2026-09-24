package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.ACC;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.MAIN;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;

/** The ceremonies of the Mages: Wizard, Sorcerer, Warlock and Necromancer. */
final class MageCeremonies {
    private MageCeremonies() {
    }

    static final Glyph WIZARD_SIGIL = new Glyph().circle(MAIN, 1.0).circle(MAIN, 0.89)
            .star(ACC, 5, 2, 0.89, 0).circle(ACC, 0.34).rays(MAIN, 24, 0.89, 1.0, 0);

    /**
     * Wizard: a huge thin magic circle is traced line by line (outer circle, second circle, pentagram,
     * inner ring, runes), runes pour into it while a second circle turns around your waist, then the
     * whole circle lifts up to your head as a crown and bursts into stars.
     */
    static void wizardGrand(Fx fx) {
        int t = fx.age();
        double radius = 3.0;
        int drawEnd = 90;
        int riseStart = 120;
        int peak = 150;
        ParticleOptions azure = fx.dust(0x4A8CFF, 0.8F);
        ParticleOptions gold = fx.dust(0xF0C850, 0.8F);
        Glyph g = WIZARD_SIGIL;
        if (t < riseStart) {
            double d = Math.min(1, t / (double) drawEnd);
            double prev = Math.min(1, Math.max(0, t - 1) / (double) drawEnd);
            if (t <= drawEnd && t > 0) {
                g.draw(fx, ParticleTypes.END_ROD, ParticleTypes.END_ROD, radius, FLOOR, 0, prev, d, 1, 0.12);
                double[] tip = g.point(d);
                fx.glyphPoint(fx.dust(0xFFFFFF, 1.8F), tip[0], tip[1], radius, FLOOR + 0.05, 0);
                fx.glyphPoint(ParticleTypes.ENCHANT, tip[0], tip[1], radius, FLOOR + 0.3, 0);
                if (fx.every(3)) {
                    fx.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.3F, 0.9F + (float) d * 0.6F);
                }
                if (g.strokeAt(d) != g.strokeAt(prev)) {
                    fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.9F + g.strokeAt(d) * 0.15F);
                }
            }
            if (fx.every(2)) {
                g.draw(fx, azure, gold, radius, FLOOR + 0.01, 0, 0, d, 0.6, 0.25);
            }
            if (t > drawEnd * 0.4) {
                for (int i = 0; i < 4; i++) {
                    double a = fx.rand() * 2 * Math.PI;
                    fx.fly(ParticleTypes.ENCHANT, Math.sin(a) * radius, FLOOR + 0.1, Math.cos(a) * radius,
                            fx.spread(1.2), 1.0 + fx.rand(), fx.spread(1.2), 1.0);
                }
            }
            if (t == drawEnd) {
                fx.sound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.8F, 1.0F);
            }
            if (t > drawEnd) {
                double keep = Math.min(1, (t - drawEnd) / 8.0);
                g.draw(fx, fx.dust(0xA070FF, 0.9F), gold, 1.2, 1.0, t * 0.08, 0, 1, keep, 0.15);
                for (int i = 0; i < 4; i++) {
                    double[] p = g.point(fx.rand());
                    double right = p[0] * radius;
                    double fwd = p[1] * radius;
                    fx.fly(ParticleTypes.END_ROD, fx.lx(fwd, right), FLOOR, fx.lz(fwd, right), 0, 1, 0, 0.04);
                }
                if ((t - drawEnd) % 6 == 0) {
                    fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 0.8F + (t - drawEnd) / 40F);
                }
            }
        } else if (t < peak) {
            double q = (t - riseStart) / (double) (peak - riseStart);
            double e = Ease.smooth(q);
            double y = Mth.lerp(e, FLOOR, 2.2);
            double rr = Mth.lerp(e, radius, 0.6);
            double rot = q * q * Math.PI * 1.4;
            g.draw(fx, fx.dust(0x4A8CFF, 1.1F), fx.dust(0xF0C850, 1.1F), rr, y, rot, 0, 1, 1, 0.25);
            g.draw(fx, fx.dust(0xA070FF, 0.9F), gold, Mth.lerp(e, 1.2, 0.4), Mth.lerp(e, 1.0, 2.2), -rot, 0, 1,
                    1 - e, 0.15);
            if (fx.every(3)) {
                fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.5F, 0.8F + (float) q * 0.6F);
            }
        }
        if (t == peak) {
            fx.flash(2.2);
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.2, 0, 50, 0.16);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.2, 0, 20, 0.1);
            fx.sound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            if (fx.every(2)) {
                g.draw(fx, fx.dust(0x4A8CFF, 1.1F), fx.dust(0xF0C850, 1.1F), 0.6, 2.2, Math.PI * 1.4 + w * 3, 0, 1,
                        1 - w, 0.12);
            }
            if (w < 0.3) {
                fx.ring(azure, Mth.lerp(w / 0.3, 0.6, 2.2), 2.2, 0.12, 1 - w * 2);
            }
            for (int k = 0; k < 5; k++) {
                double a = w * 9 + k * 2 * Math.PI / 5;
                if (fx.chance(1 - w)) {
                    fx.at(fx.dust(k % 2 == 0 ? 0xF0C850 : 0xA070FF, 1.6F), Math.sin(a) * 0.9, 2.2, Math.cos(a) * 0.9);
                }
            }
        }
    }

    /** Wizard respawn: three rune rings orbit you, spiralling up to your head, and burst into stars. */
    static void wizardLight(Fx fx) {
        int t = fx.age();
        int[] colors = {0x4A8CFF, 0xF0C850, 0xA070FF};
        if (t == 0) {
            fx.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7F, 1.0F);
        }
        if (t < 18) {
            double w = Ease.smooth(t / 18.0);
            double orbit = Mth.lerp(w, 1.0, 0.3);
            double y = Mth.lerp(w, 0.4, 2.2);
            for (int k = 0; k < 3; k++) {
                double a = t * 0.35 + k * 2 * Math.PI / 3;
                double cx = Math.sin(a) * orbit;
                double cz = Math.cos(a) * orbit;
                double cy = y + 0.2 * Math.sin(t * 0.4 + k);
                fx.ring3(fx.dust(colors[k], 1.0F), cx, cy, cz, 0.28, Math.cos(a + t * 0.2), 0.3, Math.sin(a + t * 0.2),
                        0, 2 * Math.PI, 0.06, 1);
                fx.at(ParticleTypes.ENCHANT, cx, cy, cz);
                fx.at(fx.dust(0xFFFFFF, 1.4F), cx, cy, cz);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.0F + t * 0.04F);
            }
        }
        if (t == 18) {
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.2, 0, 24, 0.15);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.2, 0, 10, 0.1);
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.3F);
        }
        if (t > 18) {
            double w = (t - 18) / 28.0;
            for (int k = 0; k < 5; k++) {
                double a = w * 7 + k * 2 * Math.PI / 5;
                if (fx.chance(1 - w)) {
                    fx.at(fx.dust(colors[k % 3], 1.5F), Math.sin(a) * 0.7, 2.2, Math.cos(a) * 0.7);
                }
            }
        }
    }

    /** Storm cloud hovering above the player. */
    static void stormCloud(Fx fx, double y, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = fx.rand() * 2 * Math.PI;
            double d = Math.sqrt(fx.rand()) * radius;
            fx.at(fx.dust(fx.chance(0.5) ? 0x505868 : 0x383C48, 3.0F), Math.sin(a) * d, y + fx.spread(0.3),
                    Math.cos(a) * d);
        }
        if (fx.every(4) && count > 4) {
            fx.cloud(fx.dust(0x5CE8FF, 1.2F), 0, y, 0, 3, radius * 0.5, 0.1, radius * 0.5, 0);
            fx.cloud(ParticleTypes.ELECTRIC_SPARK, 0, y, 0, 3, radius * 0.5, 0.1, radius * 0.5, 0.02);
        }
    }

    /** A lightning strike from the cloud onto a fixed spot, flickering for three ticks, leaving a scorch mark. */
    static void sorcererStrike(Fx fx, int i, int at, double cloudY, double minDist, double maxDist,
                                       int scorchEnd) {
        int t = fx.age();
        double a = hash(i, 21) * 2 * Math.PI;
        double d = minDist + hash(i, 22) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        if (t >= at && t < at + 3) {
            fx.bolt(0x5CE8FF, x * 0.4, cloudY, z * 0.4, x, FLOOR, z);
        }
        if (t == at) {
            fx.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.2F + (float) hash(i, 23) * 0.4F);
            fx.cloud(ParticleTypes.ELECTRIC_SPARK, x, 0.2, z, 8, 0.1, 0.1, 0.1, 0.2);
            fx.debrisAt(x, z, 3);
        }
        if (t >= at && t < scorchEnd && fx.every(2)) {
            double keep = fadeOut(t, scorchEnd - 12, scorchEnd);
            fx.ringAt(fx.dust(0x202020, 1.2F), x, FLOOR, z, 0.22, 0.07, keep);
            fx.at(fx.dust(0x202020, 1.6F), x, FLOOR, z);
        }
    }

    static void bodyArcs(Fx fx) {
        double a1 = fx.rand() * 2 * Math.PI;
        double a2 = a1 + 1 + fx.rand() * 2;
        fx.zigzag(ParticleTypes.ELECTRIC_SPARK, Math.sin(a1) * 0.45, 0.3 + fx.rand() * 1.4, Math.cos(a1) * 0.45,
                Math.sin(a2) * 0.45, 0.3 + fx.rand() * 1.4, Math.cos(a2) * 0.45, 3, 0.1, 0.1);
    }

    /**
     * Sorcerer: a wide storm cloud gathers above you, twelve bolts strike the ground around you, static
     * crawls over your body, then six bolts hit a ring around you at once and the last one hits you.
     */
    static void sorcererGrand(Fx fx) {
        int t = fx.age();
        if (t < 185) {
            double r = t < 80 ? Mth.lerp(Ease.smooth(t / 80.0), 0.3, 2.2) : 2.2;
            stormCloud(fx, 5.5, r, (int) (18 * fadeOut(t, 160, 185)));
        }
        if (t == 10 || t == 40 || t == 70) {
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.25F, 0.5F + t / 200F);
        }
        for (int i = 0; i < 12; i++) {
            sorcererStrike(fx, i, 80 + i * 5, 5.2, 1.2, 2.6, 185);
        }
        if (t >= 135 && t < 150) {
            bodyArcs(fx);
            fx.fly(ParticleTypes.ELECTRIC_SPARK, fx.spread(0.3), fx.rand() * 1.8, fx.spread(0.3), 0, 1, 0, 0.1);
            if (t == 135) {
                fx.sound(SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.6F);
            }
        }
        if (t >= 150 && t < 153) {
            fx.bolt(0x5CE8FF, 0, 5.2, 0, 0, 2.0, 0);
            fx.bolt(0xB48CFF, 0, 2.0, 0, 0, 0.1, 0);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                fx.bolt(0xB48CFF, Math.sin(a) * 0.8, 5.2, Math.cos(a) * 0.8, Math.sin(a) * 2.0, FLOOR,
                        Math.cos(a) * 2.0);
            }
        }
        if (t == 150) {
            fx.flash(2.0);
            fx.sphereOut(ParticleTypes.ELECTRIC_SPARK, 0, 2.0, 0, 40, 0.2);
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.1F);
            fx.groundDebris(12, 2.0);
        }
        if (t > 150 && fx.every(2) && fx.chance(fadeOut(t, 170, 200))) {
            bodyArcs(fx);
        }
    }

    /** Sorcerer respawn: ball lightning grows in your hands, rises over your head and fires bolts all around. */
    static void sorcererLight(Fx fx) {
        int t = fx.age();
        double y = t < 14 ? 1.2 : Mth.lerp(Math.min(1, (t - 14) / 4.0), 1.2, 2.6);
        if (t < 18) {
            double r = t < 14 ? Mth.lerp(t / 14.0, 0.1, 0.45) : 0.45;
            fx.sphere(fx.dust(0x5CE8FF, 1.1F), 0, y, 0, r, 22, 0.8);
            for (int i = 0; i < 3; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.at(ParticleTypes.ELECTRIC_SPARK, Math.sin(a) * r, y + fx.spread(r), Math.cos(a) * r);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.15F, 2.0F);
            }
        }
        if (t >= 18 && t < 21) {
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + 0.3;
                fx.bolt(0x5CE8FF, 0, 2.6, 0, Math.sin(a) * 1.5, FLOOR, Math.cos(a) * 1.5);
            }
        }
        if (t == 18) {
            fx.sphereOut(ParticleTypes.ELECTRIC_SPARK, 0, 2.6, 0, 24, 0.2);
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35F, 1.4F);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + 0.3;
                fx.debrisAt(Math.sin(a) * 1.5, Math.cos(a) * 1.5, 2);
            }
        }
        if (t > 18 && fx.every(2) && fx.chance(fadeOut(t, 30, 45))) {
            bodyArcs(fx);
        }
    }

    /** Warlock rite: five soul candles, a pact seal between them, chains to the chest that shatter. */
    static void warlockRite(Fx fx, double radius, int candleStep, int sealFrom, int sealTo, int chainFrom,
                                    int peak, int eyeFrom, int end) {
        int t = fx.age();
        ParticleOptions wax = fx.dust(0xE8DCC0, 1.1F);
        ParticleOptions green = fx.dust(0x50FF90, 1.0F);
        ParticleOptions violet = fx.dust(0x9A40D8, 0.9F);
        double[][] tops = new double[5][];
        for (int c = 0; c < 5; c++) {
            double a = c * 2 * Math.PI / 5;
            double x = Math.sin(a) * radius;
            double z = Math.cos(a) * radius;
            tops[c] = new double[] {x, 0.45, z};
            int born = c * candleStep;
            if (t < born || t >= end - 2) {
                continue;
            }
            double h = Math.min(1, (t - born) / 5.0) * 0.45;
            tops[c][1] = h;
            if (fx.every(2)) {
                fx.line(wax, x, FLOOR, z, x, h, z, 0.08, 1);
            }
            if (t == born + 5) {
                fx.sound(SoundEvents.SOUL_ESCAPE, 0.5F, 1.0F + c * 0.1F);
            }
            if (t >= born + 5) {
                if (t < end - 15) {
                    fx.at(ParticleTypes.SOUL_FIRE_FLAME, x, h + 0.08, z);
                    fx.at(green, x, h + 0.05, z);
                } else if (fx.every(3)) {
                    fx.fly(ParticleTypes.SMOKE, x, h + 0.05, z, 0, 1, 0, 0.03);
                }
            }
        }
        if (t >= sealFrom && t < end - 5 && fx.every(2)) {
            double w = Math.min(1, (t - sealFrom) / (double) (sealTo - sealFrom));
            double keep = fadeOut(t, end - 20, end - 5);
            for (int c = 0; c < 5; c++) {
                double[] a = tops[c];
                double[] b = tops[(c + 1) % 5];
                double part = Mth.clamp(w * 5 - c, 0, 1);
                if (part > 0) {
                    fx.line(violet, a[0], FLOOR, a[2], Mth.lerp(part, a[0], b[0]), FLOOR, Mth.lerp(part, a[2], b[2]),
                            0.1, keep);
                }
                fx.line(green, a[0], FLOOR, a[2], a[0] * (1 - w), FLOOR, a[2] * (1 - w), 0.12, keep * 0.6);
                if (w >= 1 && t < peak) {
                    double f = (t * 0.07 + c * 0.2) % 1.0;
                    fx.at(fx.dust(0x50FF90, 1.5F), a[0] * (1 - f), FLOOR + 0.02, a[2] * (1 - f));
                }
            }
        }
        if (t >= chainFrom && t < peak) {
            double sag = 0.4 * (1 - (t - chainFrom) / (double) (peak - chainFrom));
            ParticleOptions linkA = fx.dust(0x3A1850, 1.2F);
            ParticleOptions linkB = fx.dust(0x9A40D8, 1.2F);
            for (double[] top : tops) {
                for (int i = 0; i <= 14; i++) {
                    double f = i / 14.0;
                    fx.at(i % 2 == 0 ? linkA : linkB, Mth.lerp(f, top[0], 0), Mth.lerp(f, top[1], 1.1)
                            - Math.sin(f * Math.PI) * sag, Mth.lerp(f, top[2], 0));
                }
            }
            if ((t - chainFrom) % 6 == 0) {
                fx.sound(SoundEvents.CHAIN_STEP, 0.6F, 0.8F);
            }
        }
        if (t == peak) {
            for (double[] top : tops) {
                double mx = top[0] / 2;
                double mz = top[2] / 2;
                fx.cloud(ParticleTypes.CRIT, mx, 0.8, mz, 6, 0.1, 0.1, 0.1, 0.2);
                fx.burstItem(Items.CHAIN, mx, 0.8, mz, 3, 0.15);
            }
            fx.sphereOut(ParticleTypes.REVERSE_PORTAL, 0, 1.2, 0, 40, 0.15);
            fx.cloud(ParticleTypes.DRAGON_BREATH, 0, 1.2, 0, 16, 0.4, 0.4, 0.4, 0.02);
            fx.flash(1.2);
            fx.sound(SoundEvents.CHAIN_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.5F);
        }
        if (t >= peak && t < peak + 12) {
            for (double[] top : tops) {
                fx.fly(ParticleTypes.SOUL_FIRE_FLAME, top[0], top[1], top[2], 0, 1, 0, 0.2);
            }
        }
        if (t >= eyeFrom && t < end && fx.every(2)) {
            int mid = (eyeFrom + end) / 2;
            double open = t < mid ? Ease.smooth((t - eyeFrom) / (double) (mid - eyeFrom)) : fadeOut(t, end - 15, end);
            fx.eye(fx.dust(0x9A40D8, 1.1F), fx.dust(0x50FF90, 2.4F), 2.7, 0.65, open, t * 0.05);
        }
    }

    /**
     * Warlock: five soul candles light up one by one far around you, a pact seal links them, chains rise
     * from the candles and pull tight on your chest while a great eye opens above you, then they shatter.
     */
    static void warlockGrand(Fx fx) {
        if (fx.age() == 0) {
            fx.sound(SoundEvents.EVOKER_PREPARE_SUMMON, 0.6F, 0.8F);
        }
        warlockRite(fx, 2.2, 8, 44, 90, 80, 150, 110, 200);
    }

    /** Warlock respawn: soul fire spirals up around you and a seven-pointed pact seal flares above your head. */
    static void warlockLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.SOUL_ESCAPE, 0.8F, 0.8F);
        }
        if (t < 16) {
            double h = Mth.lerp(t / 16.0, 0.3, 2.4);
            for (int k = 0; k < 2; k++) {
                for (double y = 0; y <= h; y += 0.15) {
                    double a = y * 3.5 + k * Math.PI + t * 0.3;
                    fx.at(fx.dust(k == 0 ? 0x50FF90 : 0x9A40D8, 1.1F), Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
                double a = h * 3.5 + k * Math.PI + t * 0.3;
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * 0.7, h, Math.cos(a) * 0.7);
            }
        }
        if (t == 16) {
            fx.sphereOut(ParticleTypes.REVERSE_PORTAL, 0, 2.4, 0, 20, 0.12);
            fx.sound(SoundEvents.EVOKER_CAST_SPELL, 0.8F, 0.7F);
        }
        if (t >= 16 && t < 42) {
            double keep = fadeOut(t, 28, 42);
            double rot = t * 0.08;
            ParticleOptions green = fx.dust(0x50FF90, 1.0F);
            fx.ring(fx.dust(0x9A40D8, 1.0F), 0.75, 2.4, 0.08, keep);
            for (int i = 0; i < 7; i++) {
                double a1 = rot + i * 2 * Math.PI / 7;
                double a2 = rot + (i + 3) * 2 * Math.PI / 7;
                fx.line(green, Math.sin(a1) * 0.72, 2.4, Math.cos(a1) * 0.72, Math.sin(a2) * 0.72, 2.4,
                        Math.cos(a2) * 0.72, 0.08, keep);
            }
            if (fx.chance(keep)) {
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, fx.spread(0.6), 2.4, fx.spread(0.6));
            }
        }
    }

    /** Skull outline above the player, slowly turning so everyone sees it. */
    static void skull(Fx fx, double y, double keep, double jaw) {
        double angle = fx.age() * 0.06;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.0F);
        ParticleOptions glow = fx.dust(0x40E0A0, 1.6F);
        fx.panelEllipse(bone, 0, 0, tx, tz, 0, y, 0.3, 0.3, -0.5, Math.PI + 0.5, 0.06, keep);
        fx.panelEllipse(bone, 0, 0, tx, tz, -0.11, y - 0.02, 0.07, 0.07, 0, 2 * Math.PI, 0.04, keep);
        fx.panelEllipse(bone, 0, 0, tx, tz, 0.11, y - 0.02, 0.07, 0.07, 0, 2 * Math.PI, 0.04, keep);
        fx.panelLine(glow, 0, 0, tx, tz, -0.11, y - 0.02, -0.11, y - 0.02, 0.1, keep);
        fx.panelLine(glow, 0, 0, tx, tz, 0.11, y - 0.02, 0.11, y - 0.02, 0.1, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.03, y - 0.12, 0.03, y - 0.12, 0.03, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.18, y - 0.2 - jaw, -0.12, y - 0.32 - jaw, 0.04, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.12, y - 0.32 - jaw, 0.12, y - 0.32 - jaw, 0.04, keep);
        fx.panelLine(bone, 0, 0, tx, tz, 0.12, y - 0.32 - jaw, 0.18, y - 0.2 - jaw, 0.04, keep);
    }

    /** Grave outlines around the player, souls spiralling from them into the chest. */
    static void necroGraves(Fx fx, int graves, double dist, double offset, int drawEnd, int handFrom,
                                    int soulFrom, int soulTo, double soulSpeed, int fadeFrom, int fadeTo) {
        int t = fx.age();
        ParticleOptions soil = fx.dust(0x3A2A1A, 1.2F);
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.0F);
        ParticleOptions soul = fx.dust(0x40E0A0, 1.4F);
        double keep = fadeOut(t, fadeFrom, fadeTo);
        for (int g = 0; g < graves; g++) {
            double a = offset + g * 2 * Math.PI / graves;
            double rx = Math.sin(a);
            double rz = Math.cos(a);
            double tx = Math.cos(a);
            double tz = -Math.sin(a);
            double cx = rx * dist;
            double cz = rz * dist;
            if (t < fadeTo && (t <= drawEnd || fx.every(2))) {
                double w = Math.min(1, t / (double) drawEnd);
                double[][] c = {{0.45, 0.25}, {0.45, -0.25}, {-0.45, -0.25}, {-0.45, 0.25}, {0.45, 0.25}};
                for (int i = 0; i < 4; i++) {
                    double part = Mth.clamp(w * 4 - i, 0, 1);
                    if (part <= 0) {
                        break;
                    }
                    double x1 = cx + rx * c[i][0] + tx * c[i][1];
                    double z1 = cz + rz * c[i][0] + tz * c[i][1];
                    double x2 = cx + rx * c[i + 1][0] + tx * c[i + 1][1];
                    double z2 = cz + rz * c[i + 1][0] + tz * c[i + 1][1];
                    fx.line(soil, x1, FLOOR, z1, Mth.lerp(part, x1, x2), FLOOR, Mth.lerp(part, z1, z2), 0.08, keep);
                }
            }
            if (t < drawEnd + 8 && t % 4 == g % 4) {
                fx.debrisAt(cx, cz, 2);
                if (g == 0) {
                    fx.sound(SoundEvents.ROOTED_DIRT_BREAK, 0.5F, 0.8F);
                }
            }
            if (t >= handFrom && t < fadeTo && fx.every(2)) {
                double grow = Math.min(1, (t - handFrom) / 12.0) * keep;
                for (int hand = -1; hand <= 1; hand += 2) {
                    double hx = cx + tx * hand * 0.12;
                    double hz = cz + tz * hand * 0.12;
                    double top = 0.5 * grow;
                    fx.line(bone, hx, FLOOR, hz, hx, top, hz, 0.08, 1);
                    for (int finger = -1; finger <= 1; finger++) {
                        fx.line(bone, hx, top, hz, hx + tx * finger * 0.08, top + 0.13 * grow, hz + tz * finger * 0.08,
                                0.05, 1);
                    }
                }
            }
            if (t >= soulFrom && t < soulTo) {
                for (int j = 0; j < 4; j++) {
                    double s = ((t - soulFrom) * soulSpeed + j * 0.25) % 1.0;
                    double sa = a + s * Math.PI * 1.2;
                    double sr = dist * (1 - s);
                    fx.at(soul, Math.sin(sa) * sr, FLOOR + s * 1.1 + 0.1 * Math.sin(t * 0.3 + j), Math.cos(sa) * sr);
                }
                if (fx.chance(0.5)) {
                    fx.fly(ParticleTypes.SOUL, cx, FLOOR + 0.1, cz, 0, 1, 0, 0.03);
                }
            }
        }
    }

    /**
     * Necromancer: six graves crack open in a wide ring, bony hands claw up, souls spiral out of them into you,
     * soul mist rises, a skull forms above your head and screams.
     */
    static void necromancerGrand(Fx fx) {
        int t = fx.age();
        necroGraves(fx, 6, 2.4, 0, 30, 40, 60, 145, 0.03, 165, 195);
        if (t >= 60 && t < 145 && (t - 60) % 8 == 0) {
            fx.sound(SoundEvents.SOUL_ESCAPE, 0.6F, 0.8F + (t - 60) / 170F);
        }
        if (t >= 100 && t < 150) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * 2.4;
            fx.fly(ParticleTypes.SOUL, Math.sin(a) * r, FLOOR, Math.cos(a) * r, 0, 1, 0, 0.04);
        }
        if (t >= 90 && t < 192) {
            double keep = Math.min(1, (t - 90) / 12.0) * fadeOut(t, 178, 192);
            skull(fx, 2.7, keep, t >= 150 && t < 162 ? 0.1 : 0);
        }
        if (t == 150) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 2.5, 0, 40, 0.16);
            fx.burstItem(Items.BONE, 0, 2.5, 0, 14, 0.2);
            fx.flash(2.5);
            fx.sound(SoundEvents.WITHER_AMBIENT, 0.6F, 1.3F);
            fx.sound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.8F);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            for (int k = 0; k < 4; k++) {
                double a = w * 4 * Math.PI + k * Math.PI / 2;
                double d = 0.6 + w * 1.2;
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * d, 1.0 + w * 2.5, Math.cos(a) * d);
            }
        }
    }

    /** Necromancer respawn: bones burst out of the ground in a ring and whirl up into a vortex of souls. */
    static void necromancerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.1F);
        if (t == 0) {
            fx.sound(SoundEvents.SKELETON_AMBIENT, 0.7F, 0.8F);
            fx.groundDebris(12, 1.3);
        }
        if (t < 20) {
            double w = t < 6 ? 0 : Ease.smooth((t - 6) / 12.0);
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI / 5 + w * 3;
                double r = Mth.lerp(w, 1.2, 0.2);
                double base = t < 6 ? Mth.lerp(t / 6.0, -0.3, 0.2) : Mth.lerp(w, 0.2, 2.4);
                double x = Math.sin(a) * r;
                double z = Math.cos(a) * r;
                fx.line(bone, x, Math.max(FLOOR, base), z, x + Math.cos(a) * 0.12, base + 0.3, z - Math.sin(a) * 0.12,
                        0.06, 1);
                if (t >= 6) {
                    fx.at(ParticleTypes.SOUL, x, base, z);
                }
            }
            if (t == 6) {
                fx.burstItem(Items.BONE, 0, 0.3, 0, 8, 0.15);
                fx.sound(SoundEvents.SOUL_ESCAPE, 0.7F, 1.0F);
            }
        }
        if (t == 20) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 2.4, 0, 22, 0.15);
            fx.sound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.7F);
            fx.sound(SoundEvents.WITHER_SKELETON_AMBIENT, 0.5F, 1.2F);
        }
        if (t > 20) {
            double w = (t - 20) / 26.0;
            for (int k = 0; k < 3; k++) {
                double a = w * 4 * Math.PI + k * 2 * Math.PI / 3;
                if (fx.chance(1 - w)) {
                    fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * (0.5 + w), 2.0 + w, Math.cos(a) * (0.5 + w));
                }
            }
        }
    }
}
