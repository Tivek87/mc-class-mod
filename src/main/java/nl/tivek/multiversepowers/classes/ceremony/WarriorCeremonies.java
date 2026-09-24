package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.MAIN;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.fadeOut;

/** The ceremonies of the Warriors: Knight, Berserker, Halberdier and Duelist. */
final class WarriorCeremonies {
    private WarriorCeremonies() {
    }

    static final Glyph DUELIST_ROSE = new Glyph().rose(MAIN, 4);

    /**
     * Knight: eight shields rise one by one around you, a gold ring is traced under them, they circle faster,
     * climb to chest height, slam shut into a shield wall, are thrown outward, and you raise a sword of light.
     */
    static void knightGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.1F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.0F);
        if (t < 185) {
            double spin = t < 60 ? 0 : Math.pow((Math.min(t, 140) - 60) / 80.0, 2) * Math.PI * 3;
            double radius;
            double lift;
            if (t < 100) {
                radius = 2.0;
                lift = 0;
            } else if (t < 140) {
                double w = Ease.smooth((t - 100) / 40.0);
                radius = Mth.lerp(w, 2.0, 1.5);
                lift = w * 0.9;
            } else if (t < 150) {
                double w = (t - 140) / 10.0;
                radius = Mth.lerp(w * w, 1.5, 1.05);
                lift = 0.9;
            } else {
                double w = (t - 150) / 35.0;
                radius = Mth.lerp(w, 1.05, 2.6);
                lift = Mth.lerp(w, 0.9, -1.2);
            }
            for (int i = 0; i < 8; i++) {
                int born = i * 7;
                if (t < born) {
                    continue;
                }
                if (t == born) {
                    fx.sound(SoundEvents.SHIELD_BLOCK, 0.6F, 0.8F + i * 0.06F);
                }
                double base = Mth.lerp(Math.min(1, (t - born) / 10.0), -1.0, 0.15) + lift;
                double angle = i * Math.PI / 4 + spin;
                fx.shield(steel, gold, angle, radius, base, 1.1, fadeOut(t, 160, 185));
                if (t >= 60 && t < 150 && fx.chance(0.25)) {
                    fx.at(ParticleTypes.CRIT, Math.sin(angle) * radius, base + 0.1, Math.cos(angle) * radius);
                }
            }
            if (t >= 60 && t < 140 && t % 8 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 0.6F + (t - 60) / 100F);
            }
        }
        if (t >= 20 && t < 160 && fx.every(2)) {
            double w = Math.min(1, (t - 20) / 40.0);
            fx.arcAt(fx.dust(0xF2C84B, 0.9F), 0, FLOOR, 0, 2.0, 0, 2 * Math.PI * w, 0.1, fadeOut(t, 145, 160));
        }
        if (t == 100) {
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.8F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.8F, 0.9F);
            fx.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F);
            fx.flash(1.4);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.burstBlock(Blocks.IRON_BLOCK, Math.sin(a) * 1.05, 1.4, Math.cos(a) * 1.05, 4, 0.2);
            }
        }
        double w = fx.span(150, 164);
        if (w >= 0) {
            fx.ring(gold, Mth.lerp(w, 1.1, 2.4), 1.4, 0.12, 1 - w * 0.5);
            fx.ring(steel, Mth.lerp(w, 0.8, 2.0), FLOOR, 0.14, 1 - w * 0.5);
        }
        if (t >= 152) {
            double rise = Ease.smooth((t - 152) / 10.0);
            fx.sword(steel, gold, 2.1 + rise * 0.4, 1, 1.4, 0.32, 0.07, fadeOut(t, 185, 200));
            if (t == 160) {
                fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.2F);
                fx.cloud(ParticleTypes.END_ROD, 0, 3.9, 0, 8, 0.05, 0.05, 0.05, 0.05);
            }
            if (fx.chance(fadeOut(t, 170, 200))) {
                fx.at(gold, fx.spread(1.2), 0.3 + fx.rand() * 2.4, fx.spread(1.2));
            }
        }
    }

    /** A straight sword from bottom (bx, by, bz) along unit direction (dx, dy, dz), guard across (tx, tz). */
    static void knightBlade(Fx fx, double bx, double by, double bz, double dx, double dy, double dz,
                                    double tx, double tz, double len, double keep) {
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.1F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.1F);
        double gx = bx + dx * 0.3;
        double gy = by + dy * 0.3;
        double gz = bz + dz * 0.3;
        fx.line(gold, bx, by, bz, gx, gy, gz, 0.08, keep);
        fx.line(gold, gx - tx * 0.22, gy, gz - tz * 0.22, gx + tx * 0.22, gy, gz + tz * 0.22, 0.07, keep);
        fx.line(steel, gx, gy, gz, bx + dx * len, by + dy * len, bz + dz * len, 0.08, keep);
        if (fx.chance(keep)) {
            fx.at(ParticleTypes.END_ROD, bx + dx * len, by + dy * len, bz + dz * len);
        }
    }

    /** Knight respawn: four swords burst up out of the ground around you, clash, and fall outward. */
    static void knightLight(Fx fx) {
        int t = fx.age();
        if (t < 40) {
            double keep = fadeOut(t, 24, 40);
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 + k * Math.PI / 2;
                double ox = Math.sin(a);
                double oz = Math.cos(a);
                double tx = Math.cos(a);
                double tz = -Math.sin(a);
                double bx = ox * 1.0;
                double bz = oz * 1.0;
                if (t < 8) {
                    double by = Mth.lerp(Ease.smooth(t / 6.0), -1.3, 0.1);
                    knightBlade(fx, bx, by, bz, 0, 1, 0, tx, tz, 1.4, 1);
                } else {
                    double th = Math.min(1, (t - 8) / 12.0) * 1.25;
                    knightBlade(fx, bx, 0.1, bz, ox * Math.sin(th), Math.cos(th), oz * Math.sin(th), tx, tz, 1.4,
                            keep);
                }
            }
        }
        if (t == 0) {
            fx.groundDebris(10, 1.2);
        }
        if (t == 6) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.6F, 1.3F);
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.0F);
            fx.cloud(ParticleTypes.CRIT, 0, 1.5, 0, 20, 0.8, 0.1, 0.8, 0.2);
        }
        double pulse = fx.span(6, 14);
        if (pulse >= 0) {
            fx.ring(fx.dust(0xF2C84B, 1.2F), Mth.lerp(pulse, 0.3, 1.2), 1.0, 0.1, 1 - pulse * 0.5);
        }
        if (t == 20) {
            fx.sound(SoundEvents.ANVIL_PLACE, 0.4F, 1.6F);
            fx.cloud(ParticleTypes.CRIT, 0, 0.3, 0, 16, 1.2, 0.05, 1.2, 0.1);
        }
        double w = fx.span(20, 30);
        if (w >= 0) {
            fx.ring(fx.dust(0xC8D0E0, 1.2F), Mth.lerp(w, 1.0, 2.0), FLOOR, 0.14, 1 - w);
        }
    }

    static void berserkerCracks(Fx fx, double len, double keep) {
        ParticleOptions blood = fx.dust(0xC01818, 1.2F);
        ParticleOptions ember = fx.dust(0xFF7A1A, 1.2F);
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3 + 0.3;
            int n = (int) Math.ceil(len / 0.1);
            for (int i = 1; i <= n; i++) {
                double d = i * 0.1;
                double side = Math.sin(i * 1.7 + k * 2.3) * 0.12;
                if (fx.chance(keep)) {
                    fx.at(i % 3 == 0 ? blood : ember, Math.sin(angle) * d + Math.cos(angle) * side, FLOOR,
                            Math.cos(angle) * d - Math.sin(angle) * side);
                }
            }
        }
    }

    static void berserkerJets(Fx fx, double len) {
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3 + 0.3;
            fx.fly(ParticleTypes.FLAME, Math.sin(angle) * len, FLOOR, Math.cos(angle) * len,
                    fx.spread(0.1), 1, fx.spread(0.1), 0.25);
        }
    }

    static final int[] BERSERKER_BEATS = {0, 24, 44, 62, 78, 92, 104, 114, 122, 129, 135, 140, 144, 147};

    /**
     * Berserker: a heartbeat speeds up for seven seconds, red pulses and cracks spread over the ground,
     * rocks lift and blood mist rises, then a roar blows fire out of every crack.
     */
    static void berserkerGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions blood = fx.dust(0xB01818, 1.5F);
        ParticleOptions ember = fx.dust(0xFF7A1A, 1.0F);
        for (int k = 0; k < BERSERKER_BEATS.length; k++) {
            int b = BERSERKER_BEATS[k];
            if (t == b) {
                fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.8F + k * 0.04F);
                fx.cloud(blood, 0, 1.0, 0, 10, 0.25, 0.5, 0.25, 0);
            }
            double w = fx.span(b, b + 6);
            if (w >= 0) {
                fx.ring(blood, Mth.lerp(w, 0.3, 1.0 + k * 0.08), 1.0, 0.12, 1 - w * 0.4);
                fx.ring(ember, Mth.lerp(w, 0.2, 1.3 + k * 0.08), FLOOR, 0.14, 1 - w * 0.4);
            }
        }
        double len = Mth.clamp((t - 50) / 80.0, 0, 1) * 2.4;
        if (t >= 50 && t < 195 && fx.every(2)) {
            berserkerCracks(fx, len, fadeOut(t, 170, 195));
        }
        if (t >= 50 && t < 150 && fx.every(3)) {
            double angle = (int) (fx.rand() * 6) * Math.PI / 3 + 0.3;
            double d = fx.rand() * len;
            fx.fly(ParticleTypes.SMOKE, Math.sin(angle) * d, FLOOR, Math.cos(angle) * d, 0, 1, 0, 0.05);
        }
        if (t >= 100 && t < 150) {
            BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, fx.ground());
            double a = fx.rand() * 2 * Math.PI;
            double d = 0.6 + fx.rand() * 1.6;
            fx.cloud(rock, Math.sin(a) * d, 0.2 + (t - 100) / 60.0, Math.cos(a) * d, 1, 0.05, 0.3, 0.05, 0.02);
            fx.at(fx.dust(0x8A0A0A, 2.0F), Math.sin(a + 1) * 0.5, 0.2 + fx.rand() * 1.8, Math.cos(a + 1) * 0.5);
            if (t % 10 == 0) {
                fx.sound(SoundEvents.RAVAGER_STEP, 0.4F, 0.6F);
            }
        }
        if (t == 150) {
            fx.sound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
            fx.burstBlock(Blocks.NETHERRACK, 0, 0.3, 0, 20, 0.2);
            fx.cloud(ParticleTypes.LAVA, 0, 0.2, 0, 10, 1.0, 0.05, 1.0, 0);
            fx.flash(1.0);
        }
        if (t >= 150 && t < 166) {
            berserkerJets(fx, 2.4);
            berserkerJets(fx, 2.4);
        }
        double dome = fx.span(150, 158);
        if (dome >= 0) {
            fx.sphere(blood, 0, 0.2, 0, Mth.lerp(dome, 0.5, 2.4), 50, 1 - dome * 0.5);
        }
        if (t >= 150 && fx.chance(fadeOut(t, 165, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(blood, Math.sin(a) * 0.45, 0.2 + fx.rand() * 1.6, Math.cos(a) * 0.45);
            fx.at(ParticleTypes.SMALL_FLAME, Math.sin(a + 2) * 0.5, 0.2 + fx.rand() * 1.6, Math.cos(a + 2) * 0.5);
        }
    }

    /** Berserker respawn: you slam the ground, debris flies out, then three wild axe sweeps around you. */
    static void berserkerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions blood = fx.dust(0xB01818, 1.4F);
        double gather = fx.span(0, 6);
        if (gather >= 0) {
            fx.ring(blood, Mth.lerp(gather, 1.2, 0.3), FLOOR, 0.1, 1);
        }
        if (t == 6) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
            fx.sound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.2F);
            fx.groundDebris(16, 1.4);
            fx.radial(new BlockParticleOption(ParticleTypes.BLOCK, fx.ground()), 0.2, 16, 0.3);
            fx.cloud(ParticleTypes.LAVA, 0, 0.2, 0, 4, 0.5, 0.05, 0.5, 0);
        }
        double shock = fx.span(6, 14);
        if (shock >= 0) {
            fx.ring(fx.dust(0xFF7A1A, 1.2F), Mth.lerp(shock, 0.3, 2.0), FLOOR, 0.12, 1 - shock * 0.5);
            fx.ring(blood, Mth.lerp(shock, 0.2, 1.6), FLOOR, 0.12, 1 - shock * 0.5);
        }
        double[] heights = {0.9, 1.4, 1.1};
        for (int k = 0; k < 3; k++) {
            int start = 10 + k * 6;
            double w = fx.span(start, start + 6);
            if (w < 0) {
                continue;
            }
            double dir = k % 2 == 0 ? 1 : -1;
            double a0 = k * 2.1;
            double lead = a0 + dir * w * 1.5 * Math.PI;
            fx.arcAt(blood, 0, heights[k], 0, 1.1, lead - dir * 1.2, lead, 0.09, 1);
            fx.at(ParticleTypes.FLAME, Math.sin(lead) * 1.1, heights[k], Math.cos(lead) * 1.1);
            if (t == start) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 0.7F + k * 0.15F);
            }
        }
        if (t >= 6 && fx.chance(fadeOut(t, 24, 45))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(blood, Math.sin(a) * 0.45, 0.2 + fx.rand() * 1.6, Math.cos(a) * 0.45);
        }
    }

    /** A halberd lying flat at height {@code y}, centred on the player, turned by {@code angle}. */
    static void halberd(Fx fx, double angle, double y, double len, double reveal, double keep) {
        ParticleOptions wood = fx.dust(0x8B6B40, 1.0F);
        ParticleOptions steel = fx.dust(0xD0D8E8, 1.1F);
        ParticleOptions blade = fx.dust(0x6F9FD0, 1.1F);
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        double px = Math.cos(angle);
        double pz = -Math.sin(angle);
        double shown = len * reveal;
        fx.line(wood, -dx * shown, y, -dz * shown, dx * shown, y, dz * shown, 0.1, keep);
        if (reveal < 1) {
            return;
        }
        double[][] head = {{0.72, 0.05}, {0.64, 0.42}, {0.97, 0.46}, {0.9, 0.05}};
        for (int i = 0; i < head.length - 1; i++) {
            fx.line(blade, dx * head[i][0] * len + px * head[i][1], y, dz * head[i][0] * len + pz * head[i][1],
                    dx * head[i + 1][0] * len + px * head[i + 1][1], y,
                    dz * head[i + 1][0] * len + pz * head[i + 1][1], 0.07, keep);
        }
        fx.line(steel, dx * 0.82 * len, y, dz * 0.82 * len, dx * 0.78 * len - px * 0.2, y,
                dz * 0.78 * len - pz * 0.2, 0.07, keep);
        fx.line(steel, dx * len, y, dz * len, dx * (len + 0.3), y, dz * (len + 0.3), 0.07, keep);
        fx.at(ParticleTypes.CRIT, dx * (len + 0.3), y, dz * (len + 0.3));
    }

    static double halberdAngle(int t) {
        if (t <= 140) {
            return 0.04 * t + 0.8 * Math.pow(t, 3) / (3.0 * 140 * 140);
        }
        if (t <= 150) {
            return 42.93 + 0.84 * (t - 140);
        }
        int s = Math.min(t - 150, 25);
        return 51.33 + 0.5 * s - 0.01 * s * s;
    }

    /**
     * Halberdier: a halberd forms and spins around you faster and faster, its blade leaving a trail and
     * carving a circle, it rises overhead, then slams down flat with a 360 degree sweep and shockwave.
     */
    static void halberdierGrand(Fx fx) {
        int t = fx.age();
        double len = 2.0;
        double angle = halberdAngle(t);
        double prev = halberdAngle(Math.max(0, t - 1));
        double y;
        if (t < 110) {
            y = 1.05;
        } else if (t < 147) {
            y = Mth.lerp(Ease.smooth((t - 110) / 37.0), 1.05, 2.6);
        } else if (t < 150) {
            y = Mth.lerp((t - 147) / 3.0, 2.6, 0.3);
        } else {
            y = 0.3;
        }
        halberd(fx, angle, y, len, Math.min(1, t / 30.0), fadeOut(t, 170, 190));
        if (t >= 60 && t < 147) {
            fx.arcAt(fx.dust(0xD0D8E8, 0.7F), 0, y, 0, len + 0.3, angle - 0.9, angle, 0.1, 0.6);
        }
        if (t > 0 && t < 147 && Math.floor(angle / Math.PI) != Math.floor(prev / Math.PI)) {
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.35F, 0.6F + t / 200F);
        }
        ParticleOptions trench = fx.dust(0x6B5030, 1.0F);
        if (t >= 80 && t < 150) {
            fx.debrisAt(Math.sin(angle) * len, Math.cos(angle) * len, 1);
            if (fx.every(2)) {
                fx.ring(trench, len, FLOOR, 0.14, (t - 80) / 70.0);
            }
        }
        if (t == 110) {
            fx.sound(SoundEvents.ARMOR_EQUIP_CHAIN, 0.7F, 0.8F);
        }
        if (t == 150) {
            for (int i = 0; i < 16; i++) {
                double a = 2.0 * Math.PI * i / 16;
                fx.at(ParticleTypes.SWEEP_ATTACK, Math.sin(a) * len, 0.6, Math.cos(a) * len);
                fx.debrisAt(Math.sin(a) * len, Math.cos(a) * len, 3);
            }
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
            fx.groundDebris(12, 2.4);
        }
        double shock = fx.span(150, 165);
        if (shock >= 0) {
            fx.ring(fx.dust(0xD0D8E8, 1.3F), Mth.lerp(shock, 0.5, 2.8), FLOOR + 0.05, 0.14, 1 - shock * 0.6);
        }
        if (t >= 150 && t < 185 && fx.every(2)) {
            fx.ring(fx.dust(0xD0D8E8, 1.2F), len, FLOOR, 0.12, fadeOut(t, 160, 185));
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.CRIT, Math.sin(a) * len, 0.15, Math.cos(a) * len);
        }
    }

    /** Halberdier respawn: four halberds rise around you, lean in to meet above your head and clash. */
    static void halberdierLight(Fx fx) {
        int t = fx.age();
        if (t >= 36) {
            return;
        }
        ParticleOptions wood = fx.dust(0x8B6B40, 1.0F);
        ParticleOptions blade = fx.dust(0x6F9FD0, 1.1F);
        double keep = fadeOut(t, 18, 36);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            double x = Math.sin(a) * 1.2;
            double z = Math.cos(a) * 1.2;
            double tx;
            double ty;
            double tz;
            if (t < 8) {
                tx = x;
                ty = Mth.lerp(Ease.smooth(t / 7.0), -0.2, 2.0);
                tz = z;
            } else {
                double w = Ease.smooth(Math.min(1, (t - 8) / 6.0));
                tx = Mth.lerp(w, x, x * 0.08);
                ty = Mth.lerp(w, 2.0, 2.6);
                tz = Mth.lerp(w, z, z * 0.08);
            }
            fx.line(wood, x, FLOOR, z, tx, ty, tz, 0.1, keep);
            double bx = Math.cos(a) * 0.25;
            double bz = -Math.sin(a) * 0.25;
            double mx = Mth.lerp(0.85, x, tx);
            double my = Mth.lerp(0.85, FLOOR, ty);
            double mz = Mth.lerp(0.85, z, tz);
            fx.line(blade, mx, my, mz, mx + bx, my - 0.15, mz + bz, 0.06, keep);
            if (t == 0) {
                fx.debrisAt(x, z, 4);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.ARMOR_EQUIP_CHAIN, 0.7F, 1.0F);
        }
        if (t == 14) {
            fx.cloud(ParticleTypes.CRIT, 0, 2.6, 0, 20, 0.15, 0.15, 0.15, 0.3);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 1.6F);
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 0.9F);
        }
        double ring = fx.span(14, 22);
        if (ring >= 0) {
            fx.ring(fx.dust(0xD0D8E8, 1.2F), Mth.lerp(ring, 0.2, 1.3), 2.6, 0.1, 1 - ring);
        }
    }

    static final int[] DUELIST_ORDER = {0, 3, 6, 1, 4, 7, 2, 5};

    /** One rapier thrust from the chest outward, visible for ten ticks. */
    static void thrust(Fx fx, double angle, int start, double reach, int index) {
        int t = fx.age();
        double w = fx.span(start, start + 10);
        if (w < 0) {
            return;
        }
        double r = Mth.lerp(Math.min(1, (t - start) / 2.0), 0.4, reach);
        fx.line(fx.dust(0xF0F0F8, 0.8F), Math.sin(angle) * 0.4, 1.35, Math.cos(angle) * 0.4, Math.sin(angle) * r,
                1.35, Math.cos(angle) * r, 0.07, 1 - w);
        if (t == start) {
            fx.cloud(ParticleTypes.ENCHANTED_HIT, Math.sin(angle) * reach, 1.35, Math.cos(angle) * reach, 6, 0.05,
                    0.05, 0.05, 0.15);
            fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.5F, 1.2F + index * 0.04F);
        }
    }

    /**
     * Duelist: a big rose is drawn on the floor, two rounds of eight thrusts form a sixteen-pointed star,
     * the rapier twirls around you, then a flourish of light and cherry petals.
     */
    static void duelistGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions crimson = fx.dust(0xD02848, 0.8F);
        double radius = 2.2;
        if (t < 195) {
            double d = Math.min(1, t / 60.0);
            if (fx.every(2)) {
                DUELIST_ROSE.draw(fx, crimson, crimson, radius, FLOOR, 0, 0, d, fadeOut(t, 170, 195), 0.1);
            } else if (d < 1) {
                DUELIST_ROSE.draw(fx, crimson, crimson, radius, FLOOR, 0, Math.max(0, d - 0.03), d, 1, 0.1);
            }
            if (d < 1) {
                double[] tip = DUELIST_ROSE.point(d);
                fx.glyphPoint(ParticleTypes.CRIT, tip[0], tip[1], radius, FLOOR + 0.05, 0);
                fx.glyphPoint(fx.dust(0xFFFFFF, 1.6F), tip[0], tip[1], radius, FLOOR + 0.05, 0);
                if (fx.every(4)) {
                    fx.sound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.4F, 0.9F + (float) d * 0.8F);
                }
            }
        }
        if (fx.chance(t < 150 ? 0.6 : fadeOut(t, 160, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.6 + fx.rand() * 1.6;
            fx.at(ParticleTypes.CHERRY_LEAVES, Math.sin(a) * r, 1.8 + fx.rand() * 1.2, Math.cos(a) * r);
        }
        for (int i = 0; i < 8; i++) {
            thrust(fx, DUELIST_ORDER[i] * Math.PI / 4, 60 + i * 5, 2.2, i);
            thrust(fx, DUELIST_ORDER[i] * Math.PI / 4 + Math.PI / 8, 100 + i * 5, 2.2, i + 8);
        }
        double twirl = fx.span(140, 150);
        if (twirl >= 0) {
            double a = t * 0.9;
            fx.line(fx.dust(0xF0F0F8, 0.9F), -Math.sin(a) * 1.6, 1.35, -Math.cos(a) * 1.6, Math.sin(a) * 1.6, 1.35,
                    Math.cos(a) * 1.6, 0.08, 1);
            fx.at(ParticleTypes.CRIT, Math.sin(a) * 1.6, 1.35, Math.cos(a) * 1.6);
            fx.at(ParticleTypes.CRIT, -Math.sin(a) * 1.6, 1.35, -Math.cos(a) * 1.6);
            if (t % 3 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.3F, 1.6F);
            }
        }
        if (t == 150) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                fx.line(ParticleTypes.END_ROD, Math.sin(a) * 0.4, 1.35, Math.cos(a) * 0.4, Math.sin(a) * 2.2, 1.35,
                        Math.cos(a) * 2.2, 0.16, 1);
            }
            fx.radial(ParticleTypes.ENCHANTED_HIT, 1.35, 24, 0.3);
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 1.6, 0, 40, 1.4, 0.8, 1.4, 0);
            fx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.2F);
            fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
        }
        double w = fx.span(150, 166);
        if (w >= 0) {
            fx.ring(fx.dust(0xD02848, 1.2F), Mth.lerp(w, 1.2, 2.2), 1.35, 0.1, 1 - w);
        }
    }

    /** Duelist respawn: a small rose spins up from your feet to above your head and bursts into petals. */
    static void duelistLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.6F, 1.2F);
        }
        if (t < 16) {
            double y = Mth.lerp(Ease.smooth(t / 16.0), FLOOR, 2.3);
            ParticleOptions crimson = fx.dust(0xD02848, 1.0F);
            DUELIST_ROSE.draw(fx, crimson, crimson, 1.0, y, t * 0.3, 0, 1, 1, 0.1);
            fx.at(ParticleTypes.CRIT, fx.spread(0.3), y, fx.spread(0.3));
            if (t % 4 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.3F, 1.4F + t * 0.03F);
            }
        }
        if (t == 16) {
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 2.3, 0, 25, 0.8, 0.3, 0.8, 0);
            fx.sphereOut(ParticleTypes.ENCHANTED_HIT, 0, 2.3, 0, 20, 0.25);
            fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
            fx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
        }
        double ring = fx.span(16, 24);
        if (ring >= 0) {
            fx.ring(fx.dust(0xD02848, 1.2F), Mth.lerp(ring, 0.5, 1.4), 2.3, 0.1, 1 - ring);
        }
        if (t > 16 && fx.chance(fadeOut(t, 28, 50))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.CHERRY_LEAVES, Math.sin(a) * 1.1, 2.1, Math.cos(a) * 1.1);
        }
    }
}
