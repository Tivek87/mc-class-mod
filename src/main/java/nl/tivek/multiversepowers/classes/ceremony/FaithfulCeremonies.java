package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.ACC;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.MAIN;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.lerpColor;

final class FaithfulCeremonies {
    private FaithfulCeremonies() {
    }

    static final Glyph INQUISITOR_BRAND = new Glyph().polygon(MAIN, 3, 1.0, 0).circle(ACC, 0.5);

    static void lightBeam(Fx fx, int from, int to, double top) {
        int t = fx.age();
        double q = fx.span(from, to);
        if (q < 0) {
            return;
        }
        double front = Mth.lerp(q, top, 0);
        double prev = Mth.lerp(Math.max(0, (t - 1 - from) / (double) (to - from)), top, 0);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            double x = Math.sin(a) * 0.55;
            double z = Math.cos(a) * 0.55;
            fx.line(ParticleTypes.END_ROD, x, prev, z, x, front, z, 0.25, 1);
        }
        fx.line(fx.dust(0xFFF4C0, 2.0F), 0, front, 0, 0, top, 0, 0.3, 0.5);
    }

    static final float[] CLERIC_MELODY = {1.0F, 1.26F, 1.5F, 1.33F, 1.68F, 1.5F, 1.26F, 1.5F, 1.68F, 2.0F};

    static void clericGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions gold = fx.dust(0xFFC830, 1.2F);
        if (t < 80 && fx.every(2)) {
            fx.ring(fx.dust(0xFFC830, 0.9F), Math.min(1, t / 30.0) * 1.4, 7.5, 0.1, fadeOut(t, 60, 80));
            fx.ring(fx.dust(0xFFF4C0, 0.9F), Math.min(1, t / 40.0) * 0.9, 7.5, 0.1, fadeOut(t, 60, 80));
            if (fx.chance(0.6)) {
                fx.at(ParticleTypes.END_ROD, fx.spread(1.4), 7.5, fx.spread(1.4));
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_ACTIVATE, 0.6F, 1.4F);
        }
        lightBeam(fx, 40, 70, 7.5);
        lightBeam(fx, 110, 124, 7.5);
        if (t >= 70 && t < 150 && fx.every(2)) {
            fx.cloud(fx.dust(0xFFF4C0, 1.4F), 0, 1.8, 0, 6, 0.25, 1.4, 0.25, 0);
        }
        for (int k = 0; k < 10; k++) {
            int start = 70 + k * 8;
            double w = fx.span(start, start + 14);
            if (w >= 0) {
                fx.ring(fx.fade(0xFFC830, 0xFFF4C0, 1.2F), Mth.lerp(w, 0.3, 2.6), FLOOR, 0.14, 1 - w * 0.6);
            }
            if (t == start) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, CLERIC_MELODY[k]);
            }
        }
        if (t >= 70 && t < 150 && t % 5 == 0) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.HEART, Math.sin(a) * 0.8, 1.0 + fx.rand() * 0.8, Math.cos(a) * 0.8);
        }
        if (t >= 70 && t < 150 && fx.every(3)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.5 + fx.rand() * 1.2;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * r, 4.0, Math.cos(a) * r, 0, -1, 0, 0.05);
        }
        if (t == 150) {
            fx.ring(ParticleTypes.END_ROD, 0.45, 2.25, 0.12, 1);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.8, 1.9, Math.cos(a) * 0.8);
            }
            fx.cloud(gold, 0, 2.0, 0, 30, 0.6, 0.4, 0.6, 0);
            fx.flash(2.2);
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, 1.5F);
            fx.sound(SoundEvents.BELL_RESONATE, 0.6F, 1.5F);
        }
        if (t >= 150) {
            fx.ring(fx.dust(0xFFC830, 1.5F), 0.45, 2.25, 0.07, fadeOut(t, 180, 200));
            if (fx.chance(fadeOut(t, 160, 200))) {
                fx.at(fx.dust(0xFFF4C0, 1.2F), fx.spread(1.5), 0.3 + fx.rand() * 2.5, fx.spread(1.5));
            }
        }
    }

    static void clericLight(Fx fx) {
        int t = fx.age();
        for (int k = 0; k < 6; k++) {
            int at = k * 2;
            double a = k * Math.PI / 3;
            double x = Math.sin(a) * 1.3;
            double z = Math.cos(a) * 1.3;
            if (t == at) {
                fx.line(ParticleTypes.END_ROD, x, FLOOR, z, x, 1.2, z, 0.12, 1);
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, CLERIC_MELODY[k]);
            }
            if (t >= at && t < 34 && fx.every(2)) {
                fx.line(fx.dust(0xFFF4C0, 1.3F), x, FLOOR, z, x, 1.2, z, 0.15, fadeOut(t, 20, 34));
            }
        }
        double w = fx.span(14, 26);
        if (w >= 0) {
            fx.ring(fx.fade(0xFFC830, 0xFFF4C0, 1.3F), Mth.lerp(w, 0.3, 1.8), FLOOR, 0.12, 1 - w * 0.6);
        }
        if (t == 14) {
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.6, 1.8, Math.cos(a) * 0.6);
            }
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 1.6F);
        }
        if (t >= 14 && t < 44) {
            fx.ring(fx.dust(0xFFC830, 1.5F), 0.4, 2.2, 0.07, fadeOut(t, 30, 44));
        }
    }

    static void holyCross(Fx fx, double len, double keep) {
        ParticleOptions gold = fx.dust(0xF2D060, 1.2F);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            double dx = Math.sin(a);
            double dz = Math.cos(a);
            fx.line(gold, dx * 0.3, FLOOR, dz * 0.3, dx * len, FLOOR, dz * len, 0.09, keep);
            fx.line(gold, dx * len + dz * 0.25, FLOOR, dz * len - dx * 0.25, dx * len - dz * 0.25, FLOOR,
                    dz * len + dx * 0.25, 0.07, keep);
        }
    }

    static void paladinGrand(Fx fx) {
        int t = fx.age();
        int land = 80;
        ParticleOptions blade = fx.dust(0xE8F0FF, 1.2F);
        ParticleOptions hilt = fx.dust(0xF2D060, 1.3F);
        if (t < 150) {
            double tip = t < land ? Mth.lerp(Math.pow(t / (double) land, 3), 14.0, 0) : 0;
            double keep = t < 140 ? 1 : fadeOut(t, 140, 150);
            fx.sword(blade, hilt, tip + 3.2, -1, 3.2, 0.8, 0.15, keep);
            if (t < land) {
                fx.at(ParticleTypes.END_ROD, 0, tip, 0);
            } else if (fx.chance(0.6)) {
                fx.fly(ParticleTypes.END_ROD, 0, fx.rand() * 3.2, 0, 0, 1, 0, t < 140 ? 0.01 : 0.1);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_ACTIVATE, 0.6F, 1.0F);
        }
        if (t == 50) {
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.7F);
        }
        if (t == land) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.9F, 0.7F);
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 0.6F);
            fx.groundDebris(16, 1.0);
            fx.cloud(ParticleTypes.CRIT, 0, 0.2, 0, 24, 0.5, 0.1, 0.5, 0.3);
        }
        if (t >= land && t < 190 && fx.every(2)) {
            double w = Math.min(1, (t - land) / 30.0);
            holyCross(fx, Mth.lerp(w, 0.3, 3.0), fadeOut(t, 172, 190));
        }
        if (t >= 110 && t < 150 && fx.every(2)) {
            double a = (int) (fx.rand() * 4) * Math.PI / 2;
            double d = 0.4 + fx.rand() * 2.6;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * d, FLOOR, Math.cos(a) * d, 0, 1, 0, 0.05);
        }
        if (t == 110 || t == 130) {
            fx.sound(SoundEvents.BELL_BLOCK, 0.5F, 0.8F + (t - 110) / 100F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, 1.0F);
            fx.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.5F, 1.6F);
            fx.cloud(hilt, 0, 1.5, 0, 30, 0.6, 0.6, 0.6, 0);
            fx.flash(1.6);
        }
        if (t >= 150 && fx.every(2)) {
            double spread = Math.min(1, (t - 150) / 12.0);
            fx.wings(fx.dust(0xF2D060, 1.6F), fx.dust(0xFFFFFF, 1.3F), spread, 1.3, fadeOut(t, 182, 200));
            if (fx.chance(0.7)) {
                fx.local(ParticleTypes.END_ROD, -0.4, 1.2 + fx.rand() * 1.0, fx.spread(1.6));
            }
        }
    }

    static void paladinLight(Fx fx) {
        int t = fx.age();
        ParticleOptions gold = fx.dust(0xF2D060, 1.0F);
        double radius = 1.3;
        if (t < 18) {
            double eMax = Math.min(1, t / 10.0) * Math.PI / 2;
            double keep = t < 10 ? 1 : 0.9;
            for (int m = 0; m < 8; m++) {
                double a = m * Math.PI / 4;
                for (double e = 0; e <= eMax; e += 0.1) {
                    if (fx.chance(keep)) {
                        fx.at(gold, Math.sin(a) * Math.cos(e) * radius, FLOOR + Math.sin(e) * radius,
                                Math.cos(a) * Math.cos(e) * radius);
                    }
                }
            }
            for (double e = 0.35; e < eMax; e += 0.4) {
                fx.ring(gold, Math.cos(e) * radius, FLOOR + Math.sin(e) * radius, 0.1, keep);
            }
            if (t >= 10 && t % 3 == 0) {
                fx.ring(fx.dust(0xFFFFFF, 1.3F), radius, FLOOR, 0.1, 1);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.BELL_BLOCK, 0.7F, 1.2F);
        }
        if (t == 18) {
            for (int i = 0; i < 40; i++) {
                double[] d = Fx.sphereDirection(i, 40);
                if (d[1] < 0) {
                    continue;
                }
                fx.fly(ParticleTypes.END_ROD, d[0] * radius, FLOOR + d[1] * radius, d[2] * radius, d[0], d[1], d[2],
                        0.12);
            }
            fx.sound(SoundEvents.GLASS_BREAK, 0.7F, 1.3F);
            fx.sound(SoundEvents.BELL_BLOCK, 0.8F, 1.6F);
        }
        if (t > 18 && fx.chance(fadeOut(t, 26, 46))) {
            fx.at(fx.dust(0xF2D060, 1.3F), fx.spread(1.3), 0.3 + fx.rand() * 1.8, fx.spread(1.3));
        }
    }

    static void inquisitorRite(Fx fx, double brand, int brandEnd, double wallRadius, int wallFrom,
                                       double wallHeight, int peak, int end) {
        int t = fx.age();
        ParticleOptions orange = fx.dust(0xFF8A20, 1.1F);
        ParticleOptions scorch = fx.dust(0x3A2A20, 1.2F);
        if (t < end - 5) {
            double d = Math.min(1, t / (double) brandEnd);
            ParticleOptions ink = t < peak ? orange : scorch;
            if (fx.every(2)) {
                INQUISITOR_BRAND.draw(fx, ink, ink, brand, FLOOR, 0, 0, d, fadeOut(t, end - 25, end - 5), 0.1);
            }
            if (d < 1) {
                double[] tip = INQUISITOR_BRAND.point(d);
                fx.glyphPoint(ParticleTypes.FLAME, tip[0], tip[1], brand, FLOOR + 0.05, 0);
                fx.glyphPoint(ParticleTypes.LAVA, tip[0], tip[1], brand, FLOOR + 0.05, 0);
                if (fx.every(5)) {
                    fx.sound(SoundEvents.FIRE_AMBIENT, 0.5F, 1.0F);
                }
            }
            if (t < peak && fx.chance(0.5)) {
                double[] p = INQUISITOR_BRAND.point(fx.rand() * d);
                fx.glyphPoint(ParticleTypes.SMALL_FLAME, p[0], p[1], brand, FLOOR + 0.05, 0);
            }
        }
        if (t == 0 || t == brandEnd / 3 || t == brandEnd * 2 / 3) {
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.7F, 0.9F);
        }
        double w = fx.span(wallFrom, peak);
        if (w >= 0) {
            double h = Ease.smooth(w / 0.9) * wallHeight;
            for (int i = 0; i < 20; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.FLAME, Math.sin(a) * wallRadius, fx.rand() * h, Math.cos(a) * wallRadius, 0, 1, 0,
                        0.02);
            }
            for (double y = 0.3; y < h; y += 0.5) {
                fx.ring(orange, wallRadius, y, 0.15, 0.35);
            }
            if ((t - wallFrom) % 6 == 0) {
                fx.sound(SoundEvents.BLAZE_BURN, 0.5F, 0.8F);
            }
        }
        if (t == peak) {
            for (double y = 0.5; y < wallHeight; y += 0.6) {
                fx.radialIn(ParticleTypes.FLAME, wallRadius, y, 18, 0.18);
            }
            if (fx.grand()) {
                fx.flash(1.5);
            }
            fx.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
            fx.sound(SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
        }
        if (t >= peak && t < peak + 14) {
            for (int i = 0; i < 5; i++) {
                fx.fly(ParticleTypes.FLAME, fx.spread(0.15), 0.1, fx.spread(0.15), fx.spread(0.1), 1, fx.spread(0.1),
                        0.4);
            }
        }
        if (t > peak + 8 && fx.every(2)) {
            fx.cloud(ParticleTypes.WHITE_ASH, 0, 2.0, 0, 5, 1.4, 0.8, 1.4, 0);
            fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.4, 0.8, 1.4, 0);
        }
        int eyeFrom = wallFrom + (peak - wallFrom) / 2;
        if (t >= eyeFrom && t < end && fx.every(2)) {
            double open = Math.min(1, (t - eyeFrom) / 10.0) * fadeOut(t, end - 12, end);
            double angle = t * 0.06;
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            double up = wallHeight + 0.4;
            double s = 0.45;
            ParticleOptions cream = fx.dust(0xFFF0B0, 1.0F);
            fx.panelLine(cream, 0, 0, tx, tz, 0, up + s, -s * 0.9, up - s * 0.5, 0.07, open);
            fx.panelLine(cream, 0, 0, tx, tz, -s * 0.9, up - s * 0.5, s * 0.9, up - s * 0.5, 0.07, open);
            fx.panelLine(cream, 0, 0, tx, tz, s * 0.9, up - s * 0.5, 0, up + s, 0.07, open);
            fx.eye(fx.dust(0xFF8A20, 1.0F), fx.dust(0xFF8A20, 2.0F), up - 0.05, 0.22, open, angle);
        }
    }

    static void inquisitorGrand(Fx fx) {
        int t = fx.age();
        inquisitorRite(fx, 2.4, 60, 1.8, 60, 3.0, 150, 200);
        if (t >= 60 && t < 150 && fx.every(2)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * 1.6;
            fx.fly(ParticleTypes.SMALL_FLAME, Math.sin(a) * r, 4.0, Math.cos(a) * r, 0, -1, 0, 0.08);
        }
    }

    static void inquisitorLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double front = Mth.lerp(t / 14.0, 2.4, 0);
            for (double y = 2.4; y >= Math.max(0, front); y -= 0.12) {
                double a = y * 4;
                if (fx.chance(0.7)) {
                    fx.at(fx.dust(0xFF8A20, 1.1F), Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
            }
            double a = Math.max(0, front) * 4;
            fx.at(ParticleTypes.FLAME, Math.sin(a) * 0.7, Math.max(0, front), Math.cos(a) * 0.7);
            if (t % 4 == 0) {
                fx.sound(SoundEvents.FIRE_AMBIENT, 0.6F, 1.2F);
            }
        }
        if (t == 16) {
            fx.radial(ParticleTypes.FLAME, 0.2, 16, 0.15);
            fx.sound(SoundEvents.BLAZE_SHOOT, 0.8F, 0.8F);
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.7F, 0.8F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            int rgb = lerpColor((t - 16) / 14.0, 0xFF8A20, 0x3A2A20);
            ParticleOptions ink = fx.dust(rgb, 1.2F);
            INQUISITOR_BRAND.draw(fx, ink, ink, 1.1, FLOOR, 0, 0, 1, fadeOut(t, 30, 42), 0.1);
        }
        if (t > 20 && fx.every(2) && fx.chance(fadeOut(t, 30, 48))) {
            fx.cloud(ParticleTypes.WHITE_ASH, 0, 2.0, 0, 3, 1.0, 0.6, 1.0, 0);
        }
    }

    static final float[] MONK_NOTES = {0.9F, 1.0F, 1.12F, 1.35F, 1.5F, 1.8F, 1.5F, 1.35F};

    static void lotusPetal(Fx fx, double angle, double grow, double length, int rgb, double keep) {
        ParticleOptions edge = fx.dust(rgb, 1.0F);
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        double px = Math.cos(angle);
        double pz = -Math.sin(angle);
        int steps = (int) Math.ceil(length * 9);
        for (int i = 0; i <= steps; i++) {
            double s = i / (double) steps * grow;
            double dist = Mth.lerp(s, 0.35, length);
            double half = 0.28 * length / 1.6 * Math.sin(Math.PI * s / Math.max(grow, 0.01)) * grow;
            for (int side = -1; side <= 1; side += 2) {
                if (fx.chance(keep)) {
                    fx.at(edge, dx * dist + px * half * side, FLOOR, dz * dist + pz * half * side);
                }
            }
        }
    }

    static void yinYang(Fx fx, double y, double radius, double rot) {
        ParticleOptions white = fx.dust(0xFFFFFF, 1.0F);
        ParticleOptions orange = fx.dust(0xF2A640, 1.0F);
        fx.ring(white, radius, y, 0.07, 1);
        double half = radius / 2;
        double cx = Math.sin(rot) * half;
        double cz = Math.cos(rot) * half;
        fx.arcAt(orange, cx, y, cz, half, rot + Math.PI, rot + 2 * Math.PI, 0.06, 1);
        fx.arcAt(orange, -cx, y, -cz, half, rot, rot + Math.PI, 0.06, 1);
        fx.at(fx.dust(0xF2A640, 1.6F), cx, y, cz);
        fx.at(fx.dust(0xFFFFFF, 1.6F), -cx, y, -cz);
    }

    static void monkGrand(Fx fx) {
        int t = fx.age();
        if (t < 100 && fx.every(2)) {
            double r = 0.7 + 0.7 * (0.5 - 0.5 * Math.cos(2 * Math.PI * t / 20.0));
            for (int i = 0; i < 40; i++) {
                double[] d = Fx.sphereDirection(i, 40);
                double a = t * 0.05;
                double x = d[0] * Math.cos(a) - d[2] * Math.sin(a);
                double z = d[0] * Math.sin(a) + d[2] * Math.cos(a);
                fx.at(fx.dust(i % 5 == 0 ? 0x5FD0A0 : 0xFFFFFF, 0.9F), x * r, 1.0 + d[1] * r, z * r);
            }
        }
        if (t < 100 && t % 20 == 10) {
            fx.sound(SoundEvents.NOTE_BLOCK_FLUTE, 0.5F, 0.7F + t / 300F);
        }
        for (int k = 0; k < 16; k++) {
            boolean inner = k < 8;
            int born = inner ? 10 + k * 6 : 56 + (k - 8) * 6;
            if (t < born || t >= 195) {
                continue;
            }
            if (t == born) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.5F, MONK_NOTES[k % 8] * (inner ? 1.0F : 0.75F));
            }
            if (t < born + 5 || fx.every(2)) {
                double angle = k * Math.PI / 4 + (inner ? 0 : Math.PI / 8);
                lotusPetal(fx, angle, Math.min(1, (t - born) / 6.0), inner ? 1.3 : 2.2,
                        inner ? 0xF090B0 : 0xF2A640, fadeOut(t, 170, 195));
            }
        }
        double yy = fx.span(100, 150);
        if (yy >= 0) {
            yinYang(fx, Mth.lerp(Ease.smooth(yy), 0.2, 1.1), 0.8, t * (0.1 + yy * 0.3));
            fx.at(fx.dust(0xFFFFFF, 1.5F), fx.spread(0.3), Mth.lerp(yy, 0.2, 2.0), fx.spread(0.3));
            if (t % 10 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.4F, 0.8F + (float) yy);
            }
        }
        if (t == 150 || t == 160) {
            fx.radial(ParticleTypes.CLOUD, 1.1, 32, 0.3);
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, t == 150 ? 0.5F : 0.6F);
        }
        for (int k = 0; k < 2; k++) {
            double w = fx.span(150 + k * 10, 164 + k * 10);
            if (w >= 0) {
                fx.ring(fx.dust(0xFFFFFF, 1.2F), Mth.lerp(w, 0.5, 2.6), 1.1, 0.12, 1 - w * 0.5);
            }
        }
        if (t > 150 && fx.chance(fadeOut(t, 165, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(fx.dust(fx.chance(0.5) ? 0xFFFFFF : 0xF2A640, 1.3F), Math.sin(a) * 0.6, 0.2 + (t - 150) * 0.05,
                    Math.cos(a) * 0.6);
        }
    }

    static void monkLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double y = Mth.lerp(Ease.smooth(t / 16.0), 0.1, 2.1);
            for (int k = 0; k < 2; k++) {
                for (int m = 0; m < 5; m++) {
                    double yy = Math.max(0.1, y - m * 0.1);
                    double a = yy * 5 + k * Math.PI;
                    fx.at(fx.dust(k == 0 ? 0xFFFFFF : 0xF2A640, 1.8F - m * 0.3F), Math.sin(a) * 0.5, yy,
                            Math.cos(a) * 0.5);
                }
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.4F, MONK_NOTES[t / 4]);
            }
        }
        if (t == 16) {
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.1, 0, 12, 0.08);
            fx.radial(ParticleTypes.CLOUD, 1.1, 14, 0.2);
            fx.sound(SoundEvents.BELL_BLOCK, 0.6F, 0.8F);
        }
        double w = fx.span(16, 26);
        if (w >= 0) {
            fx.ring(fx.dust(0xFFFFFF, 1.2F), Mth.lerp(w, 0.4, 1.6), 1.1, 0.12, 1 - w * 0.5);
        }
        if (t > 16 && fx.chance(fadeOut(t, 26, 46))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(fx.dust(0xF2A640, 1.2F), Math.sin(a) * 0.5, 1.0 + (t - 16) * 0.05, Math.cos(a) * 0.5);
        }
    }
}
