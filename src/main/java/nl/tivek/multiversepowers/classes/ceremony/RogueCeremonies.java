package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;

/** The ceremonies of the Rogues: Assassin, Thief, Highwayman and Infiltrator. */
final class RogueCeremonies {
    private RogueCeremonies() {
    }

    /** X-shaped slash close to the player on the side at {@code angle}. */
    static void crossSlash(Fx fx, double angle, double dist, double y, double half) {
        double cx = Math.sin(angle) * dist;
        double cz = Math.cos(angle) * dist;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        for (int s = -1; s <= 1; s += 2) {
            fx.panelLine(fx.dust(0x9A3AD8, 1.1F), cx, cz, tx, tz, -half, y + s * half, half, y - s * half, 0.07, 1);
            fx.panelLine(ParticleTypes.CRIT, cx, cz, tx, tz, -half, y + s * half, half, y - s * half, 0.2, 1);
        }
    }

    /** A shadow clone at {@code angle} that dashes into the player at tick {@code dash}. */
    static void shadowClone(Fx fx, double angle, double dist, int appear, int dash) {
        int t = fx.age();
        if (t < appear || t > dash + 3) {
            return;
        }
        ParticleOptions shadow = fx.dust(0x201828, 1.3F);
        ParticleOptions violet = fx.dust(0x9A3AD8, 0.9F);
        double d = t < dash ? dist : Mth.lerp((t - dash) / 3.0, dist, 0.35);
        double x = Math.sin(angle) * d;
        double z = Math.cos(angle) * d;
        if (t < dash + 3 && (t < dash || fx.every(1))) {
            double keep = Math.min(1, (t - appear) / 8.0) * (fx.chance(0.15) ? 0.3 : 1);
            if (t >= dash || fx.every(2)) {
                fx.figure(shadow, x, z, keep * 0.9);
                fx.figure(violet, x, z, keep * 0.3);
            }
        }
        if (t >= dash) {
            fx.line(violet, Math.sin(angle) * dist, 1.0, Math.cos(angle) * dist, x, 1.0, z, 0.1, 1);
        }
        if (t == dash + 3) {
            crossSlash(fx, angle, 0.4, 1.1, 0.35);
            fx.cloud(ParticleTypes.SMOKE, x, 1.0, z, 6, 0.1, 0.3, 0.1, 0.02);
            fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.6F, 1.3F);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.8F);
        }
    }

    static void glintingEyes(Fx fx, int from, int to) {
        int t = fx.age();
        if (t < from || t >= to || t % 6 >= 4) {
            return;
        }
        int pair = t / 6;
        double a = hash(pair, 7) * 2 * Math.PI;
        double r = 1.0 + hash(pair, 8) * 0.8;
        double x = Math.sin(a) * r;
        double z = Math.cos(a) * r;
        double tx = Math.cos(a) * 0.06;
        double tz = -Math.sin(a) * 0.06;
        ParticleOptions eye = fx.dust(0xC060FF, 1.2F);
        fx.at(eye, x + tx, 1.5, z + tz);
        fx.at(eye, x - tx, 1.5, z - tz);
    }

    /**
     * Assassin: darkness closes in from far around you, eight shadow clones step out of it in two rings,
     * they dash through you one by one, then you vanish in smoke and ink and eyes glint in the dark.
     */
    static void assassinGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions black = fx.dust(0x18181E, 2.5F);
        ParticleOptions violet = fx.dust(0x9A3AD8, 1.2F);
        if (t == 0) {
            fx.sound(SoundEvents.PHANTOM_AMBIENT, 0.4F, 0.6F);
        }
        if (t < 150) {
            double r = t < 50 ? Mth.lerp(Ease.smooth(t / 50.0), 4.0, 2.8) : 2.8;
            if (t < 50 || fx.every(2)) {
                fx.ring(black, r, 0.3, 0.2, t < 50 ? 0.7 : 0.45);
                fx.ring(violet, r * 0.97, 0.9, 0.2, t < 50 ? 0.4 : 0.2);
            }
            if (t < 50 && fx.every(4)) {
                fx.cloud(ParticleTypes.SQUID_INK, 0, 0.5, 0, 3, r * 0.5, 0.2, r * 0.5, 0.01);
            }
        }
        for (int k = 0; k < 8; k++) {
            boolean inner = k % 2 == 0;
            double angle = inner ? Math.PI / 4 + (k / 2) * Math.PI / 2 : (k / 2) * Math.PI / 2;
            shadowClone(fx, angle, inner ? 1.8 : 2.4, inner ? 50 : 80, 100 + k * 6);
        }
        if (t == 50 || t == 80) {
            fx.sound(SoundEvents.ENDERMAN_AMBIENT, 0.3F, 0.6F);
        }
        if (t == 150) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 24, 0.4, 0.7, 0.4, 0.02);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 1.0, 0, 28, 0.14);
            fx.cloud(ParticleTypes.REVERSE_PORTAL, 0, 1.0, 0, 30, 0.3, 0.6, 0.3, 0.05);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.9F, 0.6F);
            for (int k = 0; k < 8; k++) {
                crossSlash(fx, k * Math.PI / 4, k % 2 == 0 ? 1.0 : 1.5, 1.2, 0.5);
            }
        }
        if (t > 150 && t < 190 && fx.every(3)) {
            fx.at(ParticleTypes.SMOKE, fx.spread(1.0), 0.2 + fx.rand() * 1.6, fx.spread(1.0));
        }
        glintingEyes(fx, 154, 198);
    }

    /** Assassin respawn: you blink between three points around you, slashing a triangle, then back with an X. */
    static void assassinLight(Fx fx) {
        int t = fx.age();
        ParticleOptions violet = fx.dust(0x9A3AD8, 1.1F);
        if (t == 0) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 8, 0.2, 0.5, 0.2, 0.02);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.2F);
        }
        double[][] points = new double[4][];
        points[0] = new double[] {0, 0};
        for (int k = 0; k < 3; k++) {
            double a = k * 2 * Math.PI / 3;
            points[k + 1] = new double[] {Math.sin(a) * 1.3, Math.cos(a) * 1.3};
        }
        for (int k = 1; k <= 3; k++) {
            int at = 3 + (k - 1) * 4;
            double[] p = points[k];
            if (t == at) {
                fx.cloud(ParticleTypes.REVERSE_PORTAL, p[0], 1.0, p[1], 12, 0.15, 0.5, 0.15, 0.05);
                fx.ringAt(violet, p[0], 0.1, p[1], 0.3, 0.07, 1);
                fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.6F + k * 0.1F);
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.5F);
            }
            if (k > 1 && t >= at && t < 34 && (t == at || fx.every(2))) {
                double[] q = points[k - 1];
                double keep = fadeOut(t, 20, 34);
                fx.line(violet, q[0], 1.1, q[1], p[0], 1.1, p[1], 0.07, keep);
                fx.line(ParticleTypes.CRIT, q[0], 1.1, q[1], p[0], 1.1, p[1], 0.25, keep);
            }
        }
        if (t >= 11 && t < 34 && fx.every(2)) {
            fx.line(violet, points[3][0], 1.1, points[3][1], points[1][0], 1.1, points[1][1], 0.07,
                    fadeOut(t, 20, 34));
        }
        if (t == 15) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 10, 0.3, 0.6, 0.3, 0.02);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 1.0, 0, 12, 0.12);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.7F);
            for (int k = 0; k < 4; k++) {
                crossSlash(fx, Math.PI / 4 + k * Math.PI / 2, 0.7, 1.2, 0.4);
            }
        }
        glintingEyes(fx, 20, 48);
    }

    /** A spinning coin standing on its edge at (x, y, z). */
    static void coin(Fx fx, double x, double y, double z, double spin, double radius) {
        fx.ring3(fx.dust(0xF0C040, 0.9F), x, y, z, radius, Math.cos(spin), 0, Math.sin(spin), 0, 2 * Math.PI, 0.05,
                1);
    }

    /** Coin {@code i}: pops out of the ground, spins, then flies in an arc into the player's belt. */
    static void thiefCoin(Fx fx, int i, int pop, int hop, int flyStart, int flyTime, double minDist,
                                  double maxDist) {
        int t = fx.age();
        if (t < pop || t > flyStart + flyTime) {
            return;
        }
        double a = hash(i, 11) * 2 * Math.PI;
        double d = minDist + hash(i, 12) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        double spin = t * 0.6 + i;
        if (t == pop) {
            fx.sound(SoundEvents.ITEM_PICKUP, 0.4F, 0.8F + (i % 10) * 0.08F);
            fx.debrisAt(x, z, 2);
        }
        if (t < flyStart) {
            double y = 0.2 + 0.6 * Math.sin(Math.PI * Math.min(1, (t - pop) / (double) hop));
            coin(fx, x, y, z, spin, 0.12);
            return;
        }
        double w = (t - flyStart) / (double) flyTime;
        double px = Mth.lerp(w, x, 0);
        double pz = Mth.lerp(w, z, 0);
        double py = Mth.lerp(w, 0.2, 1.0) + 1.2 * Math.sin(Math.PI * w);
        coin(fx, px, py, pz, spin, 0.12);
        fx.at(ParticleTypes.WAX_ON, px, py, pz);
        if (t == flyStart + flyTime) {
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4F, 1.0F + (i % 10) * 0.06F);
        }
    }

    static void coinFountain(Fx fx, int nuggets, int emeralds) {
        fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), 0, 2.3, 0, nuggets,
                0.2, 0.1, 0.2, 0.3);
        if (emeralds > 0) {
            fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)), 0, 2.3, 0, emeralds,
                    0.2, 0.1, 0.2, 0.3);
            fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.DIAMOND)), 0, 2.3, 0,
                    emeralds / 2, 0.2, 0.1, 0.2, 0.3);
        }
        fx.cloud(fx.dust(0xF0C040, 1.5F), 0, 2.3, 0, 24, 0.5, 0.3, 0.5, 0);
        fx.sound(SoundEvents.BUNDLE_DROP_CONTENTS, 1.0F, 1.0F);
    }

    static void coinRain(Fx fx, double keep, double radius) {
        if (fx.chance(keep)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * radius;
            fx.at(fx.dust(0xF0C040, 1.3F), Math.sin(a) * r, 0.3 + fx.rand() * 2.2, Math.cos(a) * r);
            if (fx.chance(0.4)) {
                fx.at(ParticleTypes.WAX_ON, fx.spread(radius), 0.3 + fx.rand() * 2, fx.spread(radius));
            }
        }
    }

    /**
     * Thief: twenty coins pop out of the ground around you and fly into your pocket, a padlock appears
     * above you, you pick it click by click, it springs open and a fountain of gold and gems pours out.
     */
    static void thiefGrand(Fx fx) {
        int t = fx.age();
        for (int i = 0; i < 20; i++) {
            thiefCoin(fx, i, 2 + i * 4, 8, (int) (60 + i * 3.5), 10, 0.9, 2.6);
        }
        if (t >= 110 && t < 172) {
            double keep = Math.min(1, (t - 110) / 8.0) * fadeOut(t, 160, 172);
            double angle = t * 0.07;
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            double lift = t >= 150 ? 0.14 : 0;
            ParticleOptions gold = fx.dust(0xF0C040, 1.1F);
            ParticleOptions violet = fx.dust(0xB080E8, 1.0F);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.3, 0.25, 2.3, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.7, 0.25, 2.7, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.3, -0.25, 2.7, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, 0.25, 2.3, 0.25, 2.7, 0.06, keep);
            fx.panelEllipse(violet, 0, 0, tx, tz, 0, 2.7 + lift, 0.16, 0.2, 0, Math.PI, 0.05, keep);
            fx.panelEllipse(violet, 0, 0, tx, tz, 0, 2.55, 0.05, 0.05, 0, 2 * Math.PI, 0.03, keep);
            fx.panelLine(violet, 0, 0, tx, tz, 0, 2.5, 0, 2.38, 0.04, keep);
        }
        if (t >= 128 && t < 150 && t % 3 == 0) {
            fx.cloud(ParticleTypes.WAX_ON, 0, 2.5, 0, 3, 0.05, 0.05, 0.05, 0.05);
            fx.sound(SoundEvents.TRIPWIRE_CLICK_OFF, 0.5F, 1.4F + (t - 128) / 30F);
        }
        if (t == 146) {
            fx.sound(SoundEvents.CHEST_LOCKED, 0.6F, 1.2F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.CHEST_OPEN, 0.7F, 1.4F);
            coinFountain(fx, 30, 8);
            fx.flash(2.5);
        }
        if (t > 150) {
            coinRain(fx, fadeOut(t, 165, 200), 2.0);
            coinRain(fx, fadeOut(t, 165, 200), 2.0);
        }
    }

    /** Thief respawn: a coin flips high over your head, you catch it, and five coins circle you. */
    static void thiefLight(Fx fx) {
        int t = fx.age();
        if (t < 18) {
            double w = t / 18.0;
            double y = 1.3 + 1.7 * Math.sin(Math.PI * w);
            coin(fx, 0, y, 0, t * 0.9, 0.18);
            fx.at(ParticleTypes.WAX_ON, fx.spread(0.1), y - 0.2, fx.spread(0.1));
        }
        if (t == 0) {
            fx.sound(SoundEvents.ARMOR_EQUIP_GOLD, 0.7F, 1.6F);
        }
        if (t == 18) {
            fx.cloud(ParticleTypes.WAX_ON, 0, 1.3, 0, 12, 0.2, 0.2, 0.2, 0.05);
            fx.cloud(fx.dust(0xF0C040, 1.5F), 0, 1.3, 0, 10, 0.2, 0.2, 0.2, 0);
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
        }
        if (t > 18 && t < 46) {
            double keep = fadeOut(t, 34, 46);
            for (int k = 0; k < 5; k++) {
                double a = t * 0.2 + k * 2 * Math.PI / 5;
                if (fx.chance(keep)) {
                    coin(fx, Math.sin(a) * 0.8, 1.0 + 0.1 * Math.sin(t * 0.3 + k), Math.cos(a) * 0.8, t * 0.6 + k,
                            0.1);
                }
            }
        }
    }

    /** Revolver cylinder hovering above the head: six chambers, {@code loaded} of them filled. */
    static void revolver(Fx fx, double y, double rot, int loaded, int fired, double keep) {
        ParticleOptions leather = fx.dust(0x8B5A30, 1.0F);
        ParticleOptions steel = fx.dust(0xB8B8C0, 0.9F);
        fx.ring(leather, 0.55, y, 0.08, keep);
        for (int c = 0; c < 6; c++) {
            double a = rot + c * Math.PI / 3;
            double cx = Math.sin(a) * 0.36;
            double cz = Math.cos(a) * 0.36;
            fx.ringAt(steel, cx, y, cz, 0.1, 0.05, keep);
            if (c < loaded && c >= fired) {
                fx.at(fx.dust(0xF0C040, 1.5F), cx, y, cz);
            }
        }
    }

    static void gunshot(Fx fx, double angle, double muzzle, double end) {
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        fx.line(ParticleTypes.CRIT, dx * muzzle, 1.25, dz * muzzle, dx * end, 1.3, dz * end, 0.15, 1);
        fx.line(fx.dust(0xE8D8A8, 0.9F), dx * muzzle, 1.25, dz * muzzle, dx * end, 1.3, dz * end, 0.1, 1);
        fx.cloud(ParticleTypes.FLAME, dx * muzzle, 1.25, dz * muzzle, 5, 0.05, 0.05, 0.05, 0.04);
        fx.cloud(ParticleTypes.SMOKE, dx * muzzle, 1.25, dz * muzzle, 3, 0.05, 0.05, 0.05, 0.02);
        fx.cloud(ParticleTypes.CRIT, dx * end, 1.3, dz * end, 6, 0.1, 0.1, 0.1, 0.2);
        fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5F, 0.8F);
        fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.4F, 0.6F);
    }

    /**
     * Highwayman: a horse gallops two and a half laps around you, kicking up dust, a revolver loads chamber
     * by chamber above your head, spins, and six shots fan out all around you, then gunsmoke rings rise.
     */
    static void highwaymanGrand(Fx fx) {
        int t = fx.age();
        int step = t / 2;
        if (t == 0) {
            fx.sound(SoundEvents.HORSE_AMBIENT, 0.6F, 1.0F);
        }
        if (t < 100 && fx.every(2)) {
            ParticleOptions hoof = fx.dust(0x3A2A1A, 1.1F);
            double keep = fadeOut(t, 80, 100);
            for (int s = Math.max(0, step - 14); s <= Math.min(step, 40); s++) {
                double a = s * 0.4;
                for (int side = -1; side <= 1; side += 2) {
                    if (fx.chance(keep)) {
                        double r = 2.2 + side * 0.08;
                        fx.at(hoof, Math.sin(a - side * 0.03) * r, FLOOR, Math.cos(a - side * 0.03) * r);
                    }
                }
            }
            if (step <= 40) {
                double a = step * 0.4;
                fx.debrisAt(Math.sin(a) * 2.2, Math.cos(a) * 2.2, 3);
                fx.cloud(ParticleTypes.POOF, Math.sin(a) * 2.2, 0.2, Math.cos(a) * 2.2, 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
        if (t < 80 && t % 5 == 0) {
            fx.sound(SoundEvents.HORSE_GALLOP, 0.5F, 1.0F);
        }
        if (t >= 70 && t < 178) {
            int loaded = t < 80 ? 0 : Math.min(6, (t - 80) / 9 + 1);
            int fired = t < 150 ? 0 : Math.min(6, (t - 150) / 3 + 1);
            double rot = t < 125 ? Math.PI / 3 * Mth.clamp((t - 80) / 9.0, 0, 5)
                    : 5 * Math.PI / 3 + Math.pow(Math.min(t, 150) - 125, 2) * 0.012 + Math.max(0, t - 150) * 0.3;
            double keep = Math.min(1, (t - 70) / 6.0) * fadeOut(t, 170, 178);
            revolver(fx, 2.5, rot, loaded, fired, keep);
            if (t >= 80 && t < 130 && (t - 80) % 9 == 0) {
                fx.sound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.5F, 1.4F);
            }
            if (t >= 125 && t < 150 && fx.every(2)) {
                fx.sound(SoundEvents.LEVER_CLICK, 0.3F, 1.5F + (t - 125) / 50F);
            }
        }
        for (int i = 0; i < 6; i++) {
            if (t == 150 + i * 3) {
                gunshot(fx, i * Math.PI / 3, 0.45, 2.2);
            }
        }
        for (int k = 0; k < 2; k++) {
            double w = fx.span(170 + k * 8, 200);
            if (w >= 0) {
                fx.ring(fx.dust(0x9A9AA0, 1.4F), 0.3 + w * 0.7, 2.0 + w * 1.2, 0.12, 1 - w);
            }
        }
    }

    /** Highwayman respawn: a whinny, then two pistol shots straight up into the air and rising smoke rings. */
    static void highwaymanLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.HORSE_AMBIENT, 0.4F, 1.1F);
        }
        for (int k = 0; k < 2; k++) {
            int at = 4 + k * 6;
            double side = k == 0 ? 0.35 : -0.35;
            double mx = fx.lx(0, side);
            double mz = fx.lz(0, side);
            if (t == at) {
                fx.line(ParticleTypes.CRIT, mx, 1.7, mz, mx, 4.2, mz, 0.15, 1);
                fx.line(fx.dust(0xE8D8A8, 0.9F), mx, 1.7, mz, mx, 4.2, mz, 0.1, 1);
                fx.cloud(ParticleTypes.FLAME, mx, 1.7, mz, 6, 0.05, 0.05, 0.05, 0.05);
                fx.cloud(ParticleTypes.CRIT, mx, 4.2, mz, 6, 0.1, 0.1, 0.1, 0.2);
                fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.7F, 0.8F + k * 0.1F);
                fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.5F, 0.6F);
            }
            double w = fx.span(at, at + 30);
            if (w >= 0) {
                fx.ringAt(fx.dust(0x9A9AA0, 1.3F), mx, 1.8 + w * 1.6, mz, 0.12 + w * 0.35, 0.08, 1 - w);
            }
        }
        if (t >= 10 && t < 30 && fx.every(3)) {
            fx.debrisAt(fx.spread(1.2), fx.spread(1.2), 2);
        }
    }

    static void infiltratorScan(Fx fx, double y) {
        fx.ring(fx.dust(0x40D8B0, 1.2F), 0.55, y, 0.08, 1);
        fx.ring(fx.dust(0x40D8B0, 0.7F), 0.62, y - 0.04, 0.12, 0.6);
    }

    /** Body turned into jittering pixels; {@code amount} 0..1 is how far they are displaced. */
    static void infiltratorGlitch(Fx fx, int count, double amount) {
        int[] colors = {0x40D8B0, 0x202830, 0x9AA8C0};
        for (int i = 0; i < count; i++) {
            double a = fx.rand() * 2 * Math.PI;
            double r = Math.sqrt(fx.rand()) * 0.3;
            fx.at(fx.dust(colors[i % 3], 1.4F), Math.sin(a) * r + fx.spread(0.35 * amount), 0.1 + fx.rand() * 1.8,
                    Math.cos(a) * r + fx.spread(0.35 * amount));
        }
        if (fx.every(5)) {
            double y = 0.2 + fx.rand() * 1.6;
            fx.ringAt(fx.dust(0x40D8B0, 1.0F), fx.spread(0.3), y, fx.spread(0.3), 0.35, 0.08, 1);
        }
    }

    /**
     * Infiltrator: a wide laser grid appears under you and a scanner sweeps over you three times,
     * you glitch apart into pixels that scatter, then snap back together and a watching eye opens.
     */
    static void infiltratorGrand(Fx fx) {
        int t = fx.age();
        if (t < 135 && fx.every(3)) {
            ParticleOptions grid = fx.dust(0x40D8B0, 0.7F);
            double keep = Math.min(1, t / 15.0) * 0.6 * fadeOut(t, 120, 135);
            for (int k = -3; k <= 3; k++) {
                fx.line(grid, k * 0.8, FLOOR, -2.4, k * 0.8, FLOOR, 2.4, 0.12, keep);
                fx.line(grid, -2.4, FLOOR, k * 0.8, 2.4, FLOOR, k * 0.8, 0.12, keep);
            }
        }
        if (t < 60) {
            int phase = t % 20;
            double y = phase < 10 ? Mth.lerp(phase / 10.0, FLOOR, 2.0) : Mth.lerp((phase - 10) / 10.0, 2.0, FLOOR);
            infiltratorScan(fx, y);
            if (fx.every(3)) {
                fx.sound(SoundEvents.NOTE_BLOCK_BIT, 0.3F, 1.2F + (float) y * 0.3F);
            }
        }
        double g = fx.span(60, 120);
        if (g >= 0) {
            infiltratorGlitch(fx, (int) (10 + 35 * g), g);
            if (fx.every(4)) {
                fx.sound(SoundEvents.SCULK_CLICKING, 0.4F, 1.3F + (float) g * 0.5F);
            }
        }
        double s = fx.span(120, 140);
        if (s >= 0) {
            infiltratorGlitch(fx, (int) (45 * (1 - s)), 1);
            for (int i = 0; i < 3; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.ELECTRIC_SPARK, 0, 0.3 + fx.rand() * 1.5, 0, Math.sin(a), 0, Math.cos(a), 0.2);
            }
            if (t == 120) {
                fx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.0F);
            }
        }
        if (t >= 140 && t < 150 && fx.every(2)) {
            for (double y = 0.2; y < 1.9; y += 0.35) {
                fx.ring(fx.dust(0x40D8B0, 0.8F), 0.42, y, 0.14, 0.15);
            }
        }
        if (t == 150) {
            for (double y = 0.4; y < 2.0; y += 0.5) {
                fx.radialIn(ParticleTypes.ELECTRIC_SPARK, 1.5, y, 12, 0.35);
            }
            fx.cloud(fx.dust(0x40D8B0, 1.2F), 0, 1.0, 0, 40, 0.25, 0.6, 0.25, 0);
            fx.flash(1.0);
            fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.3F);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.4F, 1.6F);
        }
        if (t > 150 && fx.every(2)) {
            double open = Math.sin(Math.min(1, (t - 150) / 10.0) * Math.PI / 2) * fadeOut(t, 185, 200);
            fx.eye(fx.dust(0x40D8B0, 1.1F), fx.dust(0x9AA8C0, 2.2F), 2.5, 0.5, open, t * 0.06);
            for (double y = 0.2; y < 1.9; y += 0.35) {
                fx.ring(fx.dust(0x40D8B0, 0.8F), 0.42, y, 0.14, 0.2 * fadeOut(t, 170, 200));
            }
        }
    }

    /** Infiltrator respawn: a hologram of you builds up ring by ring from head to feet, flickers, and turns solid. */
    static void infiltratorLight(Fx fx) {
        int t = fx.age();
        ParticleOptions holo = fx.dust(0x40D8B0, 1.0F);
        if (t < 30) {
            int rings = Math.min(14, t + 1);
            double keep = t < 16 ? (fx.chance(0.2) ? 0.3 : 1) : fadeOut(t, 16, 30);
            for (int i = 0; i < rings; i++) {
                double y = 2.0 - i * 0.14;
                double r = y > 1.4 ? 0.2 : 0.33;
                fx.ring(holo, r, y, 0.1, keep * 0.8);
            }
            if (t < 14) {
                fx.ring(fx.dust(0xFFFFFF, 1.0F), 0.45, 2.0 - t * 0.14, 0.08, 1);
                if (t % 2 == 0) {
                    fx.sound(SoundEvents.NOTE_BLOCK_BIT, 0.3F, 2.0F - t * 0.06F);
                }
            }
        }
        if (t < 16 && fx.every(3)) {
            fx.ring(fx.dust(0x40D8B0, 0.7F), 0.9, FLOOR, 0.12, 0.6);
            fx.ring(fx.dust(0x40D8B0, 0.7F), 1.3, FLOOR, 0.12, 0.4);
        }
        if (t == 16) {
            fx.radialIn(ParticleTypes.ELECTRIC_SPARK, 1.2, 1.0, 14, 0.3);
            fx.cloud(holo, 0, 1.0, 0, 24, 0.25, 0.6, 0.25, 0);
            fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.4F);
        }
    }
}
