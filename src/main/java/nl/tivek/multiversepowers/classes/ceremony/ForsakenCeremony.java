package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FLOOR;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.FORSAKEN_SHARDS;
import static nl.tivek.multiversepowers.classes.ceremony.Fx.hash;

/** The ceremony of The Forsaken. */
final class ForsakenCeremony {
    private ForsakenCeremony() {
    }

    static final double[][][] FORSAKEN_CRACKS = forsakenCracks();

    static double[] sphereAt(double lat, double lon, double radius) {
        return new double[] {Math.cos(lat) * Math.sin(lon) * radius, Math.sin(lat) * radius,
                Math.cos(lat) * Math.cos(lon) * radius};
    }

    static final int[] FORSAKEN_BEATS = {0, 20, 38, 54, 68, 80, 90, 99, 107, 114, 120, 125, 129, 133, 136,
            139, 142, 145, 147};

    /**
     * Forsaken: a dark shell slowly closes around you from far away while your heartbeat speeds up, cracks of
     * light spread over it, six shards in the group colours glow inside, it trembles and shatters with a sonic boom.
     */
    static void forsakenGrand(Fx fx) {
        int t = fx.age();
        double cy = 1.0;
        double radius = 1.6;
        int shellEnd = 80;
        int crackEnd = 135;
        int peak = 150;
        for (int b : FORSAKEN_BEATS) {
            if (t == b) {
                fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
            }
        }
        if (t < peak) {
            double r = Mth.lerp(Ease.smooth(t / (double) shellEnd), 3.0, radius);
            double maxY = t < shellEnd ? Mth.lerp(t / (double) shellEnd, -1, 1) : 1;
            double jitter = t >= crackEnd ? 0.08 : 0;
            for (int i = 0; i < 80; i++) {
                double yy = fx.rand() * (maxY + 1) - 1;
                double ring = Math.sqrt(1 - yy * yy);
                double a = fx.rand() * 2 * Math.PI;
                double py = cy + yy * r;
                if (py < 0.02) {
                    continue;
                }
                fx.at(fx.dust(i % 3 == 0 ? 0x3A3A48 : 0x18181E, i % 3 == 0 ? 1.4F : 2.0F),
                        Math.sin(a) * ring * r + fx.spread(jitter), py, Math.cos(a) * ring * r + fx.spread(jitter));
            }
            if (t >= shellEnd) {
                int n = (int) Math.min(14, 1 + 14 * (t - shellEnd) / (double) (crackEnd - shellEnd));
                ParticleOptions crack = fx.dust(0xC8C8D8, 1.0F);
                for (double[][] path : FORSAKEN_CRACKS) {
                    for (int i = 1; i < n; i++) {
                        double[] p1 = sphereAt(path[i - 1][0], path[i - 1][1], r * 1.01);
                        double[] p2 = sphereAt(path[i][0], path[i][1], r * 1.01);
                        fx.line(crack, p1[0], cy + p1[1], p1[2], p2[0], cy + p2[1], p2[2], 0.08, 1);
                    }
                    if (n < 14 && fx.chance(0.3)) {
                        double[] tip = sphereAt(path[n - 1][0], path[n - 1][1], r);
                        fx.at(ParticleTypes.END_ROD, tip[0], cy + tip[1], tip[2]);
                    }
                }
                for (int k = 0; k < 6; k++) {
                    double a = t * 0.12 + k * Math.PI / 3;
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 2.0F), Math.sin(a) * 0.6, 1.6, Math.cos(a) * 0.6);
                }
            }
            if (t >= crackEnd) {
                fx.radialIn(ParticleTypes.SMOKE, r, cy, 8, 0.1);
                if (t == crackEnd) {
                    fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.4F, 0.8F);
                }
            }
        }
        if (t == peak) {
            for (int i = 0; i < 50; i++) {
                double[] d = Fx.sphereDirection(i, 50);
                fx.fly(ParticleTypes.SCULK_SOUL, d[0] * radius, cy + d[1] * radius, d[2] * radius, d[0], d[1], d[2],
                        0.2);
            }
            fx.at(ParticleTypes.SONIC_BOOM, 0, 1.2, 0);
            fx.flash(1.2);
            fx.sound(SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 1.0F);
        }
        double s = fx.span(peak, peak + 10);
        if (s >= 0) {
            fx.sphere(fx.dust(0x18181E, 1.8F), 0, cy, 0, Mth.lerp(s, radius, radius + 1.2), 60, 1 - s);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            double d = w < 0.3 ? Mth.lerp(w / 0.3, 0.6, 2.0) : 2.0;
            for (int k = 0; k < 6; k++) {
                double a = (w < 0.3 ? peak * 0.12 : peak * 0.12 + (t - peak - 15) * 0.08) + k * Math.PI / 3;
                if (fx.chance(1 - w)) {
                    ParticleOptions shard = fx.dust(FORSAKEN_SHARDS[k], 2.0F);
                    fx.at(shard, Math.sin(a) * d, 1.6, Math.cos(a) * d);
                    fx.at(shard, Math.sin(a - 0.08) * d, 1.6, Math.cos(a - 0.08) * d);
                }
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.4, 0.6, 1.4, 0);
            }
        }
    }

    /** Forsaken respawn: six shards in the group colours spiral in and slam into you with a dark pulse. */
    static void forsakenLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.2F);
        }
        if (t < 14) {
            double w = Ease.smooth(t / 14.0);
            for (int k = 0; k < 6; k++) {
                for (int m = 0; m < 4; m++) {
                    double ww = Math.max(0, w - m * 0.05);
                    double a = k * Math.PI / 3 + ww * 3;
                    double r = Mth.lerp(ww, 2.5, 0.2);
                    double y = Mth.lerp(ww, 2.2, 1.2);
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 2.0F - m * 0.4F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
        }
        if (t == 14) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 1.2, 0, 24, 0.15);
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.8F);
            fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.3F, 1.2F);
        }
        double pulse = fx.span(14, 24);
        if (pulse >= 0) {
            fx.ring(fx.dust(0x18181E, 2.2F), Mth.lerp(pulse, 0.3, 2.0), 1.2, 0.15, 1 - pulse * 0.5);
            fx.ring(fx.dust(0x6A6A78, 1.4F), Mth.lerp(pulse, 0.2, 1.6), FLOOR, 0.15, 1 - pulse * 0.5);
        }
        if (t > 14) {
            double w = (t - 14) / 32.0;
            for (int k = 0; k < 6; k++) {
                double a = t * 0.15 + k * Math.PI / 3;
                if (fx.chance((1 - w) * 0.6)) {
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 1.4F), Math.sin(a) * 0.7, 1.6, Math.cos(a) * 0.7);
                }
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 2, 1.0, 0.5, 1.0, 0);
            }
        }
    }

    /** Six crack paths over a sphere, as {latitude, longitude} points, the same every run. */
    static double[][][] forsakenCracks() {
        double[][][] cracks = new double[6][14][2];
        for (int c = 0; c < 6; c++) {
            double lat = (hash(c, 3) - 0.5) * 1.2;
            double lon = c * Math.PI / 3 + hash(c, 4) * 0.5;
            for (int i = 0; i < 14; i++) {
                cracks[c][i][0] = lat;
                cracks[c][i][1] = lon;
                lat = Mth.clamp(lat + (hash(c * 20 + i, 5) - 0.5) * 0.45, -1.3, 1.3);
                lon += 0.16 + (hash(c * 20 + i, 6) - 0.5) * 0.2;
            }
        }
        return cracks;
    }
}
