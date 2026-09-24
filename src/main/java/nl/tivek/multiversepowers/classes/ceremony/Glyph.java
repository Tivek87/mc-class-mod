package nl.tivek.multiversepowers.classes.ceremony;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;

/**
 * A floor drawing made of strokes in unit coordinates: x = right, z = forward, outer radius 1.
 * Strokes are traced in the order they were added.
 */
final class Glyph {
    private final List<Stroke> strokes = new ArrayList<>();
    private double total;

    private record Stroke(double[] xs, double[] zs, boolean accent, double start, double length) {
    }

    private Glyph path(boolean accent, double... xz) {
        int n = xz.length / 2;
        double[] xs = new double[n];
        double[] zs = new double[n];
        double length = 0;
        for (int i = 0; i < n; i++) {
            xs[i] = xz[2 * i];
            zs[i] = xz[2 * i + 1];
            if (i > 0) {
                length += Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
            }
        }
        this.strokes.add(new Stroke(xs, zs, accent, this.total, length));
        this.total += length;
        return this;
    }

    /** Arc around (cx, cz); angle 0 points forward, positive turns to the right. */
    private Glyph arc(boolean accent, double cx, double cz, double radius, double from, double to) {
        int n = Math.max(4, (int) Math.ceil(Math.abs(to - from) / (Math.PI / 32)));
        double[] xz = new double[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double angle = from + (to - from) * i / n;
            xz[2 * i] = cx + Math.sin(angle) * radius;
            xz[2 * i + 1] = cz + Math.cos(angle) * radius;
        }
        return this.path(accent, xz);
    }

    Glyph circle(boolean accent, double radius) {
        return this.arc(accent, 0, 0, radius, 0, 2.0 * Math.PI);
    }

    Glyph polygon(boolean accent, int sides, double radius, double rot) {
        double[] xz = new double[(sides + 1) * 2];
        for (int i = 0; i <= sides; i++) {
            double angle = rot + 2.0 * Math.PI * i / sides;
            xz[2 * i] = Math.sin(angle) * radius;
            xz[2 * i + 1] = Math.cos(angle) * radius;
        }
        return this.path(accent, xz);
    }

    /** Star polygon {n/k}; splits into several strokes when n and k share a divisor. */
    Glyph star(boolean accent, int n, int k, double radius, double rot) {
        int groups = gcd(n, k);
        int per = n / groups;
        for (int j = 0; j < groups; j++) {
            double[] xz = new double[(per + 1) * 2];
            for (int i = 0; i <= per; i++) {
                double angle = rot + 2.0 * Math.PI * ((j + i * k) % n) / n;
                xz[2 * i] = Math.sin(angle) * radius;
                xz[2 * i + 1] = Math.cos(angle) * radius;
            }
            this.path(accent, xz);
        }
        return this;
    }

    Glyph rays(boolean accent, int n, double r1, double r2, double rot) {
        for (int i = 0; i < n; i++) {
            double angle = rot + 2.0 * Math.PI * i / n;
            this.path(accent, Math.sin(angle) * r1, Math.cos(angle) * r1, Math.sin(angle) * r2,
                    Math.cos(angle) * r2);
        }
        return this;
    }

    /** Rose curve r = cos(k * angle): 2k petals for even k. */
    Glyph rose(boolean accent, int k) {
        int n = 240;
        double[] xz = new double[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double angle = 2.0 * Math.PI * i / n;
            double radius = Math.cos(k * angle);
            xz[2 * i] = Math.sin(angle) * radius;
            xz[2 * i + 1] = Math.cos(angle) * radius;
        }
        return this.path(accent, xz);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    int strokeAt(double progress) {
        double at = Mth.clamp(progress, 0, 1) * this.total;
        for (int i = 0; i < this.strokes.size(); i++) {
            Stroke stroke = this.strokes.get(i);
            if (at <= stroke.start() + stroke.length()) {
                return i;
            }
        }
        return this.strokes.size() - 1;
    }

    /** Unit point {x, z} at a fraction of the total pen length. */
    double[] point(double progress) {
        double at = Mth.clamp(progress, 0, 1) * this.total;
        for (Stroke stroke : this.strokes) {
            if (at > stroke.start() + stroke.length()) {
                continue;
            }
            double[] xs = stroke.xs();
            double[] zs = stroke.zs();
            double pos = stroke.start();
            for (int i = 1; i < xs.length; i++) {
                double seg = Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
                if (at <= pos + seg || i == xs.length - 1) {
                    double f = seg > 0 ? Mth.clamp((at - pos) / seg, 0, 1) : 0;
                    return new double[] {Mth.lerp(f, xs[i - 1], xs[i]), Mth.lerp(f, zs[i - 1], zs[i])};
                }
                pos += seg;
            }
        }
        Stroke last = this.strokes.get(this.strokes.size() - 1);
        return new double[] {last.xs()[last.xs().length - 1], last.zs()[last.zs().length - 1]};
    }

    /** Draws the part between two fractions of the pen length, one particle every {@code step} blocks. */
    void draw(Fx fx, ParticleOptions main, ParticleOptions accent, double radius, double dy, double rot,
                      double from, double to, double keep, double step) {
        double lo = from * this.total;
        double hi = to * this.total;
        for (Stroke stroke : this.strokes) {
            if (stroke.start() > hi || stroke.start() + stroke.length() < lo) {
                continue;
            }
            ParticleOptions particle = stroke.accent() ? accent : main;
            double[] xs = stroke.xs();
            double[] zs = stroke.zs();
            double pos = stroke.start();
            for (int i = 1; i < xs.length; i++) {
                double seg = Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
                double a = Math.max(lo, pos);
                double b = Math.min(hi, pos + seg);
                if (b > a && seg > 0) {
                    int n = Math.max(1, (int) Math.ceil((b - a) * radius / step));
                    for (int k = 0; k < n; k++) {
                        if (keep < 1 && !fx.chance(keep)) {
                            continue;
                        }
                        double f = (a - pos + (b - a) * k / n) / seg;
                        fx.glyphPoint(particle, Mth.lerp(f, xs[i - 1], xs[i]), Mth.lerp(f, zs[i - 1], zs[i]),
                                radius, dy, rot);
                    }
                }
                pos += seg;
            }
        }
    }
}
