package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;

final class RangerCeremonies {
    private RangerCeremonies() {
    }

    static void archerArrow(Fx fx, int i, int land, double minDist, double maxDist, int fadeFrom,
                                    int fadeTo) {
        int t = fx.age();
        double a = hash(i, 1) * 2 * Math.PI;
        double d = minDist + hash(i, 2) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        ParticleOptions straw = fx.dust(0xF0E0A0, 0.9F);
        if (t >= land - 8 && t < land) {
            double y = FLOOR + (land - t) * 0.45;
            fx.line(straw, x, y, z, x, y + 0.7, z, 0.1, 1);
            fx.at(ParticleTypes.END_ROD, x, y, z);
            fx.at(fx.dust(0xD04040, 1.2F), x, y + 0.7, z);
        }
        if (t == land) {
            fx.sound(SoundEvents.ARROW_HIT, 0.5F, 0.9F + (float) hash(i, 3) * 0.4F);
            fx.cloud(ParticleTypes.CRIT, x, 0.2, z, 4, 0.1, 0.1, 0.1, 0.1);
            fx.debrisAt(x, z, 2);
        }
        if (t >= land && t < fadeTo && fx.every(2)) {
            double keep = fadeOut(t, fadeFrom, fadeTo);
            fx.ringAt(fx.dust(0xD04040, 1.0F), x, FLOOR, z, 0.36, 0.08, keep);
            fx.ringAt(fx.dust(0xF0F0F0, 1.0F), x, FLOOR, z, 0.24, 0.08, keep);
            fx.ringAt(fx.dust(0xD04040, 1.0F), x, FLOOR, z, 0.1, 0.08, keep);
            fx.line(straw, x, FLOOR, z, x + Math.sin(a) * 0.12, 0.5, z + Math.cos(a) * 0.12, 0.08, keep);
        }
    }

    static void archerTarget(Fx fx, int rings, double step, double keep) {
        for (int k = 1; k <= rings; k++) {
            fx.ring(fx.dust(k % 2 == 1 ? 0xD04040 : 0xF0F0F0, 1.3F), k * step, FLOOR, 0.1, keep);
        }
    }

    static void archerGrand(Fx fx) {
        int t = fx.age();
        if (t == 4) {
            fx.sound(SoundEvents.CROSSBOW_LOADING_END, 0.7F, 0.9F);
        }
        if (t < 12 && fx.every(2)) {
            fx.radialIn(ParticleTypes.END_ROD, 1.2, 1.4, 8, 0.1);
        }
        for (int i = 0; i < 12; i++) {
            int launch = 12 + i * 5;
            if (t >= launch && t < launch + 8) {
                double a = i * Math.PI / 6;
                double x = Math.sin(a) * 0.35;
                double z = Math.cos(a) * 0.35;
                double y = 1.5 + (t - launch) * 0.8;
                fx.line(fx.dust(0xF0E0A0, 0.9F), x, y, z, x, y + 0.7, z, 0.1, 1);
                fx.at(ParticleTypes.END_ROD, x, y + 0.7, z);
                if (t == launch) {
                    fx.sound(SoundEvents.ARROW_SHOOT, 0.6F, 0.8F + i * 0.04F);
                }
            }
        }
        for (int i = 0; i < 30; i++) {
            archerArrow(fx, i, 80 + i * 2, 0.8, 2.6, 170, 190);
        }
        double fall = fx.span(138, 150);
        if (fall >= 0) {
            double y = Mth.lerp(fall * fall, 9.0, FLOOR);
            fx.line(fx.dust(0xF2C84B, 1.2F), 0, y, 0, 0, y + 1.0, 0, 0.08, 1);
            fx.at(ParticleTypes.END_ROD, 0, y, 0);
            fx.at(fx.dust(0xFFFFFF, 1.6F), 0, y + 1.0, 0);
            if (t == 138) {
                fx.sound(SoundEvents.ARROW_SHOOT, 1.0F, 0.5F);
            }
        }
        if (t == 150) {
            fx.sound(SoundEvents.ARROW_HIT_PLAYER, 1.0F, 1.0F);
            fx.burstItem(Items.FEATHER, 0, 2.0, 0, 14, 0.15);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 1.2, 0, 24, 0.14);
            fx.flash(1.0);
        }
        if (t >= 150 && t < 185 && fx.every(2)) {
            archerTarget(fx, 4, 0.6, fadeOut(t, 165, 185));
        }
    }

    static void archerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions straw = fx.dust(0xF0E0A0, 0.9F);
        if (t == 2) {
            fx.sound(SoundEvents.ARROW_SHOOT, 0.8F, 1.0F);
            fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.5F, 1.2F);
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            double sx = Math.sin(a);
            double sz = Math.cos(a);
            double w = fx.span(2, 10);
            if (w >= 0) {
                double d = Mth.lerp(w, 0.3, 1.6);
                double y = Mth.lerp(w, 1.4, FLOOR) + 0.8 * Math.sin(Math.PI * w);
                double pw = Math.max(0, w - 0.15);
                double pd = Mth.lerp(pw, 0.3, 1.6);
                double py = Mth.lerp(pw, 1.4, FLOOR) + 0.8 * Math.sin(Math.PI * pw);
                fx.line(straw, sx * pd, py, sz * pd, sx * d, y, sz * d, 0.08, 1);
                fx.at(ParticleTypes.END_ROD, sx * d, y, sz * d);
            }
            if (t >= 10 && t < 44 && fx.every(2)) {
                double keep = fadeOut(t, 26, 44);
                fx.line(straw, sx * 1.6, FLOOR, sz * 1.6, sx * 1.35, 0.45, sz * 1.35, 0.08, keep);
                fx.ringAt(fx.dust(0xD04040, 1.0F), sx * 1.6, FLOOR, sz * 1.6, 0.2, 0.07, keep);
            }
            if (t == 10) {
                fx.cloud(ParticleTypes.CRIT, sx * 1.6, 0.2, sz * 1.6, 3, 0.05, 0.05, 0.05, 0.1);
            }
        }
        if (t == 10) {
            fx.sound(SoundEvents.ARROW_HIT, 0.7F, 1.0F);
        }
        if (t == 12) {
            fx.sound(SoundEvents.ARROW_HIT_PLAYER, 0.8F, 1.3F);
            fx.burstItem(Items.FEATHER, 0, 1.6, 0, 6, 0.12);
        }
    }

    static void pawPrint(Fx fx, int type, double x, double z, double heading, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        ParticleOptions mark = fx.dust(type == 1 ? 0xE8E0C8 : 0x5A3A1C, 1.0F);
        double fx1 = Math.sin(heading);
        double fz1 = Math.cos(heading);
        double sx = Math.cos(heading);
        double sz = -Math.sin(heading);
        switch (type) {
            case 1 -> {
                for (int k = -1; k <= 1; k++) {
                    fx.line(mark, x, FLOOR, z, x + fx1 * 0.16 + sx * k * 0.1, FLOOR, z + fz1 * 0.16 + sz * k * 0.1,
                            0.05, 1);
                }
                fx.line(mark, x, FLOOR, z, x - fx1 * 0.07, FLOOR, z - fz1 * 0.07, 0.05, 1);
            }
            case 2 -> {
                for (int k = -1; k <= 1; k += 2) {
                    fx.line(mark, x - fx1 * 0.08 + sx * k * 0.05, FLOOR, z - fz1 * 0.08 + sz * k * 0.05,
                            x + fx1 * 0.08 + sx * k * 0.02, FLOOR, z + fz1 * 0.08 + sz * k * 0.02, 0.04, 1);
                }
            }
            default -> {
                double size = type == 3 ? 1.4 : 1.0;
                int toes = type == 3 ? 5 : 4;
                fx.at(mark, x, FLOOR, z);
                fx.at(mark, x + sx * 0.05 * size, FLOOR, z + sz * 0.05 * size);
                fx.at(mark, x - sx * 0.05 * size, FLOOR, z - sz * 0.05 * size);
                for (int k = 0; k < toes; k++) {
                    double side = (k - (toes - 1) / 2.0) * 0.07 * size;
                    double ahead = (0.14 - Math.abs(side) * 0.4) * size;
                    fx.at(mark, x + fx1 * ahead + sx * side, FLOOR, z + fz1 * ahead + sz * side);
                }
            }
        }
    }

    static void beastTracks(Fx fx, int steps, int first, int interval, double from, double to,
                                    double keep) {
        int t = fx.age();
        for (int k = 0; k < 4; k++) {
            double a0 = Math.PI / 4 + k * Math.PI / 2;
            for (int j = 0; j < steps; j++) {
                int at = first + j * interval;
                if (t < at) {
                    break;
                }
                if (t == at && k == 0) {
                    fx.sound(SoundEvents.GRASS_STEP, 0.4F, 0.9F + j * 0.03F);
                }
                double dist = Mth.lerp(j / (double) (steps - 1), from, to);
                double angle = a0 + 0.35 * Math.sin(j * 0.9 + k);
                double side = (j % 2 == 0 ? 0.1 : -0.1);
                double x = Math.sin(angle) * dist + Math.cos(angle) * side;
                double z = Math.cos(angle) * dist - Math.sin(angle) * side;
                pawPrint(fx, k, x, z, angle + Math.PI, keep);
            }
        }
    }

    static void spiritOrb(Fx fx, int rgb, double angle, double radius, double y, double dir, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        for (int m = 0; m < 6; m++) {
            double a = angle - dir * 0.12 * m;
            fx.at(fx.dust(rgb, 2.0F - m * 0.28F), Math.sin(a) * radius, y, Math.cos(a) * radius);
        }
    }

    static void beastmasterGrand(Fx fx) {
        int t = fx.age();
        if (t < 130 && fx.every(2)) {
            beastTracks(fx, 16, 2, 5, 4.0, 0.9, fadeOut(t, 100, 130));
        }
        double in = fx.span(70, 150);
        if (in >= 0) {
            double keep = Math.min(1, (t - 70) / 10.0);
            double pull = t < 144 ? 1 : 1 - (t - 144) / 6.0;
            double birdY = t < 100 ? 3.2 : Mth.lerp(Math.min(1, (t - 100) / 20.0), 3.2, 2.5);
            spiritOrb(fx, 0x9EE07A, t * 0.25, 1.4 * pull, 0.45 + 0.1 * Math.abs(Math.sin(t * 0.5)), 1, keep);
            spiritOrb(fx, 0xF0E0A0, 1 - t * 0.18, 1.8 * pull, 1.0, -1, keep);
            spiritOrb(fx, 0xE08A3A, 3 - t * 0.3, 1.0 * pull, 0.35, -1, keep);
            spiritOrb(fx, 0xFFFFFF, 2 + t * 0.3, 0.9 * pull, birdY + 0.2 * Math.sin(t * 0.3), 1, keep);
        }
        if (t == 80) {
            fx.sound(SoundEvents.WOLF_AMBIENT, 0.5F, 1.0F);
        }
        if (t == 95) {
            fx.sound(SoundEvents.FOX_AMBIENT, 0.5F, 1.1F);
        }
        if (t == 110) {
            fx.sound(SoundEvents.PARROT_AMBIENT, 0.5F, 1.0F);
        }
        if (t == 125) {
            fx.sound(SoundEvents.WOLF_PANT, 0.5F, 1.0F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.WOLF_HOWL, 1.0F, 1.0F);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.7, 2.1, Math.cos(a) * 0.7);
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.0, 0, 30, 0.8, 0.8, 0.8, 0);
            fx.burstItem(Items.FEATHER, 0, 1.8, 0, 10, 0.15);
            fx.flash(1.0);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            spiritOrb(fx, 0xFFFFFF, t * 0.2, 0.8, 2.3 + w * 2.5, 1, 1 - w);
            if (fx.every(2)) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.COMPOSTER, Math.sin(a) * 0.9, 0.3, Math.cos(a) * 0.9, 0, 1, 0, 0.04);
            }
        }
    }

    static void beastmasterLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double h = Mth.lerp(t / 16.0, 0.2, 2.2);
            for (int k = 0; k < 3; k++) {
                for (double y = 0; y <= h; y += 0.12) {
                    double a = y * 3 + k * 2 * Math.PI / 3 + t * 0.25;
                    double r = 0.9 - y * 0.2;
                    fx.at(fx.dust(k == 1 ? 0x7A5028 : 0x9EE07A, 1.1F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
            fx.at(ParticleTypes.COMPOSTER, fx.spread(0.4), h, fx.spread(0.4));
            if (t % 5 == 0) {
                fx.sound(SoundEvents.GRASS_STEP, 0.5F, 1.2F);
            }
        }
        if (t == 16) {
            fx.sound(SoundEvents.WOLF_HOWL, 0.9F, 1.1F);
            for (int i = 0; i < 5; i++) {
                double a = i * 2 * Math.PI / 5;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.5, 2.1, Math.cos(a) * 0.5);
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.4, 0, 16, 0.6, 0.6, 0.6, 0);
        }
        if (t > 16 && fx.chance(fadeOut(t, 26, 48))) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.4 + fx.rand() * 1.0;
            fx.at(ParticleTypes.COMPOSTER, Math.sin(a) * r, 0.5 + fx.rand() * 1.8, Math.cos(a) * r);
        }
    }

    static void trapJaws(Fx fx, double radius, double close, double keep) {
        ParticleOptions iron = fx.dust(0xA0A0A8, 1.1F);
        double phi = close * Math.PI / 2;
        int count = (int) Math.ceil(Math.PI * radius / 0.08);
        for (int k = -1; k <= 1; k += 2) {
            for (int i = 0; i <= count; i++) {
                if (!fx.chance(keep)) {
                    continue;
                }
                double th = Math.PI * i / count;
                double along = Math.cos(th) * radius;
                double out = Math.sin(th) * radius;
                double fwd = k * Math.cos(phi) * out;
                double up = FLOOR + Math.sin(phi) * out;
                fx.local(iron, fwd, up, along);
                if (i % 5 == 2) {
                    fx.localLine(iron, fwd, up, along, fwd * 0.82, FLOOR + (up - FLOOR) * 0.82, along * 0.82, 0.05, 1);
                }
            }
        }
    }

    static void trapSpikes(Fx fx, int count, double radius, int first, int fadeFrom, int fadeTo) {
        int t = fx.age();
        ParticleOptions iron = fx.dust(0x8A8A92, 1.0F);
        for (int j = 0; j < count; j++) {
            int born = first + j;
            if (t < born || t >= fadeTo) {
                continue;
            }
            double a = 2.0 * Math.PI * j / count;
            double x = Math.sin(a) * radius;
            double z = Math.cos(a) * radius;
            double h = Math.min(1, (t - born) / 3.0) * 0.35;
            if (t == born) {
                fx.at(ParticleTypes.CRIT, x, h + 0.1, z);
                if (j % 2 == 0) {
                    fx.sound(SoundEvents.TRIPWIRE_CLICK_ON, 0.5F, 1.0F + j * 0.03F);
                }
            }
            if (t < born + 4 || fx.every(2)) {
                fx.line(iron, x, FLOOR, z, x, FLOOR + h, z, 0.07, fadeOut(t, fadeFrom, fadeTo));
            }
        }
    }

    static void trapperGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions rope = fx.dust(0x9B7B48, 1.1F);
        double cy;
        double r;
        if (t < 50) {
            cy = 2.6;
            r = 0.9;
        } else if (t < 70) {
            double w = (t - 50) / 20.0;
            cy = Mth.lerp(w, 2.6, FLOOR);
            r = Mth.lerp(w, 0.9, 2.4);
        } else {
            cy = FLOOR;
            r = Mth.lerp(Ease.smooth((t - 70) / 18.0), 2.4, 0.45);
        }
        if (t < 88 || t < 185 && fx.every(2)) {
            fx.ring(rope, r, cy, 0.1, fadeOut(t, 160, 185));
        }
        if (t < 70) {
            double a = t * 0.6;
            fx.at(fx.dust(0x9B7B48, 1.8F), Math.sin(a) * r, cy, Math.cos(a) * r);
            if (t < 50) {
                fx.line(rope, Math.sin(a) * r, cy, Math.cos(a) * r, 0, 1.5, 0, 0.1, 1);
            }
            if (t % 6 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.4F, 1.1F + t / 200F);
            }
        }
        if (t == 88) {
            fx.sound(SoundEvents.LEASH_KNOT_PLACE, 0.8F, 0.8F);
        }
        trapSpikes(fx, 16, 1.4, 90, 170, 190);
        trapSpikes(fx, 20, 2.2, 106, 170, 190);
        if (t >= 126 && t < 175 && fx.every(2)) {
            ParticleOptions wire = fx.dust(0xE8E8E0, 0.7F);
            double keep = Math.min(1, (t - 126) / 8.0) * fadeOut(t, 150, 175);
            for (int k = 0; k < 5; k++) {
                double a1 = k * 2 * Math.PI / 5;
                double a2 = a1 + 4 * Math.PI / 5;
                fx.line(wire, Math.sin(a1) * 2.2, 0.22, Math.cos(a1) * 2.2, Math.sin(a2) * 2.2, 0.22,
                        Math.cos(a2) * 2.2, 0.12, keep);
            }
            if (t < 136 && t % 3 == 0) {
                fx.sound(SoundEvents.TRIPWIRE_ATTACH, 0.4F, 1.2F);
            }
        }
        double close = fx.span(135, 150);
        if (close >= 0 || t >= 150 && t < 185) {
            trapJaws(fx, 1.3, close >= 0 ? close * close : 1, fadeOut(t, 162, 185));
        }
        if (t == 135) {
            fx.sound(SoundEvents.CHAIN_PLACE, 0.6F, 0.7F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.0F, 0.7F);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 1.8F);
            fx.burstItem(Items.IRON_NUGGET, 0, 1.3, 0, 14, 0.2);
            fx.cloud(ParticleTypes.CRIT, 0, 1.3, 0, 24, 0.3, 0.1, 0.3, 0.3);
        }
    }

    static void trapperLight(Fx fx) {
        int t = fx.age();
        if (t >= 38) {
            return;
        }
        ParticleOptions rope = fx.dust(0x9B7B48, 1.1F);
        double fall = Math.min(1, t / 10.0);
        double cy = Mth.lerp(fall * fall, 2.8, 0.15);
        double rad = t < 10 ? 1.2 : Mth.lerp(Ease.smooth((t - 10) / 10.0), 1.2, 0.8);
        double keep = fadeOut(t, 22, 38);
        if (t < 12 || fx.every(2)) {
            for (int k = 0; k < 10; k++) {
                double angle = k * Math.PI / 5;
                for (int i = 0; i <= 7; i++) {
                    double lat = i / 7.0 * Math.PI / 2;
                    if (fx.chance(keep)) {
                        fx.at(rope, Math.sin(angle) * Math.cos(lat) * rad, cy + Math.sin(lat) * 1.6,
                                Math.cos(angle) * Math.cos(lat) * rad);
                    }
                }
            }
            for (double h = 0.35; h < 1.0; h += 0.3) {
                fx.ring(rope, rad * Math.cos(h * Math.PI / 2), cy + Math.sin(h * Math.PI / 2) * 1.6, 0.12, keep);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.7F, 0.8F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.LEASH_KNOT_PLACE, 0.9F, 0.8F);
            fx.sound(SoundEvents.CHAIN_PLACE, 0.5F, 1.0F);
            fx.groundDebris(8, 1.2);
        }
    }

    static void scoutWind(Fx fx, int layers, double keep) {
        int t = fx.age();
        ParticleOptions wind = fx.dust(0xE8FAFF, 0.9F);
        ParticleOptions sky = fx.dust(0xB8F0FF, 0.8F);
        for (int l = 0; l < layers; l++) {
            double y = 0.2 + l * 0.5;
            double r = 0.6 + l * 0.22;
            for (int j = 0; j < 3; j++) {
                double phase = t * 0.45 * (1 + l * 0.1) + j * 2 * Math.PI / 3 + l;
                fx.arcAt(j == 0 ? sky : wind, 0, y, 0, r, phase, phase + 0.8, 0.1, keep);
            }
            if (fx.chance(0.3 * keep)) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.WHITE_SMOKE, Math.sin(a) * r, y, Math.cos(a) * r, Math.cos(a), 0.1, -Math.sin(a),
                        0.12);
            }
        }
    }

    static void scoutCompass(Fx fx, double len, double keep) {
        ParticleOptions pointer = fx.dust(0x7FD46B, 1.1F);
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 2;
            double dx = Math.sin(angle);
            double dz = Math.cos(angle);
            fx.line(pointer, dx * 0.3, FLOOR, dz * 0.3, dx * len, FLOOR, dz * len, 0.1, keep);
            fx.line(pointer, dx * len, FLOOR, dz * len, dx * (len - 0.22) + dz * 0.16, FLOOR,
                    dz * (len - 0.22) - dx * 0.16, 0.08, keep);
            fx.line(pointer, dx * len, FLOOR, dz * len, dx * (len - 0.22) - dz * 0.16, FLOOR,
                    dz * (len - 0.22) + dx * 0.16, 0.08, keep);
            fx.at(ParticleTypes.END_ROD, dx * len, FLOOR + 0.03, dz * len);
        }
    }

    static void hawk(Fx fx, double angle, double radius, double y, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        ParticleOptions body = fx.dust(0x7A5028, 1.4F);
        ParticleOptions tip = fx.dust(0xF0F0F0, 1.1F);
        double x = Math.sin(angle) * radius;
        double z = Math.cos(angle) * radius;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        double ox = Math.sin(angle);
        double oz = Math.cos(angle);
        double flap = 0.22 * Math.sin(fx.age() * 0.9);
        fx.at(body, x, y, z);
        fx.at(body, x - tx * 0.2, y, z - tz * 0.2);
        for (int side = -1; side <= 1; side += 2) {
            double mx = x + ox * side * 0.25;
            double mz = z + oz * side * 0.25;
            double ex = x + ox * side * 0.5;
            double ez = z + oz * side * 0.5;
            fx.line(body, x, y, z, mx, y + flap * 0.5, mz, 0.06, 1);
            fx.line(body, mx, y + flap * 0.5, mz, ex - tx * 0.1, y + flap, ez - tz * 0.1, 0.06, 1);
            fx.at(tip, ex - tx * 0.1, y + flap, ez - tz * 0.1);
        }
    }

    static void scoutGrand(Fx fx) {
        int t = fx.age();
        if (t < 170) {
            scoutWind(fx, 8, t < 150 ? Math.min(1, t / 60.0) : fadeOut(t, 150, 170));
        }
        if (t < 150 && t % 10 == 0) {
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.4F, 1.2F + t / 300F);
        }
        double q = fx.span(20, 150);
        if (q >= 0) {
            double e = Ease.smooth(q);
            hawk(fx, t * 0.12, Mth.lerp(e, 2.8, 0.6), Mth.lerp(e, 6.0, 1.9), 1);
            if (fx.chance(0.05)) {
                fx.burstItem(Items.FEATHER, Math.sin(t * 0.12) * 2, 3, Math.cos(t * 0.12) * 2, 1, 0.02);
            }
        }
        if (t == 150) {
            fx.at(ParticleTypes.GUST, 0, 1.6, 0);
            fx.radial(ParticleTypes.CLOUD, 0.3, 24, 0.3);
            fx.radial(ParticleTypes.CLOUD, 1.4, 16, 0.25);
            fx.burstItem(Items.FEATHER, 0, 1.8, 0, 14, 0.15);
            fx.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 1.0F);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            hawk(fx, t * 0.12, Mth.lerp(w, 0.6, 1.6), Mth.lerp(w, 1.9, 6.5), 1 - w);
        }
        if (t >= 152 && t < 190 && fx.every(2)) {
            scoutCompass(fx, 0.4 + Math.min(1, (t - 152) / 8.0) * 1.4, fadeOut(t, 175, 190));
        }
    }

    static void scoutLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.6F, 1.5F);
        }
        if (t < 16) {
            double a = t * 0.8;
            double y = 0.9 + 0.3 * Math.sin(t * 0.4);
            spiritOrb(fx, 0x7FD46B, a, 1.1, y, 1, 1);
            fx.at(ParticleTypes.WHITE_SMOKE, Math.sin(a - 0.4) * 1.1, y, Math.cos(a - 0.4) * 1.1);
            if (t % 4 == 0) {
                fx.debrisAt(Math.sin(a) * 1.1, Math.cos(a) * 1.1, 2);
                fx.sound(SoundEvents.GRASS_STEP, 0.4F, 1.4F);
            }
        }
        if (t == 16) {
            fx.burstItem(Items.FEATHER, 0, 1.6, 0, 8, 0.15);
            fx.radial(ParticleTypes.CLOUD, 0.3, 12, 0.2);
            fx.sound(SoundEvents.PHANTOM_SWOOP, 0.6F, 1.4F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            scoutCompass(fx, 0.4 + Math.min(1, (t - 16) / 5.0) * 0.9, fadeOut(t, 30, 42));
        }
    }
}
