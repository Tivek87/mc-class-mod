package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class WhipPainter {
    // Where the fist holds the handle, and where the lash leaves it, along the handle from that grip.
    private static final double GRIP = 0.17;
    public static final double TIP = 0.1 + GRIP;
    private static final ConstructPainter.Shape HANDLE = handle();
    private static final double RADIUS = 0.032;
    private static final double TIP_THIN = 0.72;
    private static final int ROUND = 6;
    private static final Mesh BAND = Mesh.torus(12, 4, 1.0, 0.16, 1.9);
    private static final Mesh CRACKER = Mesh.cone(8, 0.017, 0.0, 0.0, 0.1, 1.9);
    private static final Mesh KNOT = Mesh.ball(8, 5, 0.019, 1.6);
    private static final int BANDS = 6;
    private static final int PIECES = 7;
    private static final double CRACK_TICKS = 5.0;
    private static final double FLING = 1.2;
    // In your own view the lash leaves out what passes this close to your eyes, or it would fill the view.
    private static final double NEAR_EYE = 0.7;
    private static final ConstructPainter.Frame WORLD = new ConstructPainter.Frame(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0),
            new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);

    private WhipPainter() {
    }

    private static ConstructPainter.Shape handle() {
        List<Mesh> parts = new ArrayList<>();
        for (Mesh part : WeaponShapes.whipHandle()) {
            parts.add(part.moved(0.0, 0.0, GRIP));
        }
        return ConstructPainter.Shape.of(parts.toArray(Mesh[]::new));
    }

    public static ConstructPainter.Frame held(Vec3 grip, Vec3 forward, Vec3 up, double scale) {
        Vec3 ahead = forward.normalize();
        Vec3 top = up.subtract(ahead.scale(up.dot(ahead)));
        top = top.lengthSqr() < 1.0E-8 ? ConstructPainter.Frame.of(grip, ahead, Vectors.UP, 1.0).up() : top.normalize();
        return new ConstructPainter.Frame(grip, ahead.cross(top).normalize(), top, ahead, scale);
    }

    // The handle grows out of the fist both ways, white-hot, and cools to green.
    public static void handle(LanternPainter painter, ConstructPainter.Frame frame, double grown, double apart) {
        if (grown <= 0.01) {
            return;
        }
        double wide = Math.min(1.0, grown * 1.6);
        ConstructPainter.Frame shown = grown < 1.0 ? frame.stretched(wide, wide, grown) : frame;
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(HANDLE, shown, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        painter.ambient(0.12);
        painter.shape(HANDLE, shown, 1.0, 1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (grown < 1.0) {
            painter.flare(frame.center(), 0.3 * frame.scale() * (1.0 - grown), 1.0 - grown);
        }
    }

    public static double radius(double share, double scale) {
        return RADIUS * scale * (1.0 - TIP_THIN * Math.pow(Mth.clamp(share, 0.0, 1.0), 0.85));
    }

    // The lash: one solid tapering line of hard light with glowing bands along it and a tuft at the tip. Hot is how
    // white it still glows as it forms; eye is where your own eyes are when it is your own lash.
    public static void lash(LanternPainter painter, Vec3[] points, Vec3 normal, double scale, double hot,
            double apart, @Nullable Vec3 eye) {
        int n = points.length;
        if (n < 2 || length(points) < 1.0E-3) {
            return;
        }
        double[] along = new double[n];
        for (int i = 1; i < n; i++) {
            along[i] = along[i - 1] + points[i].distanceTo(points[i - 1]);
        }
        double total = Math.max(1.0E-4, along[n - 1]);
        double[] radius = new double[n];
        for (int i = 0; i < n; i++) {
            radius[i] = radius(along[i] / total, scale);
        }
        if (apart > 0.0) {
            shatter(painter, points, normal, radius, apart);
            return;
        }
        painter.glare(Mth.clamp(hot, 0.0, 1.0) * 0.7);
        painter.ambient(0.12);
        pieces(painter, points, normal, radius, eye);
        for (int k = 1; k < BANDS; k++) {
            double at = total * k / BANDS;
            int i = 1;
            while (i < n - 1 && along[i] < at) {
                i++;
            }
            Vec3 way = points[i].subtract(points[i - 1]);
            if (way.lengthSqr() < 1.0E-10) {
                continue;
            }
            double u = (at - along[i - 1]) / Math.max(1.0E-6, along[i] - along[i - 1]);
            Vec3 spot = points[i - 1].lerp(points[i], Mth.clamp(u, 0.0, 1.0));
            double size = radius(at / total, scale) + 0.006 * scale;
            if (clear(spot, eye)) {
                painter.mesh(BAND, placed(spot, way, size), 1.0, 1.0);
            }
        }
        Vec3 tip = points[n - 1];
        Vec3 last = tip.subtract(points[n - 2]);
        if (last.lengthSqr() > 1.0E-10 && clear(tip, eye)) {
            painter.mesh(CRACKER, placed(tip.subtract(last.normalize().scale(0.02 * scale)), last, scale), 1.0, 1.0);
            painter.mesh(KNOT, placed(tip, last, scale), 1.0, 1.0);
        }
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    private static void pieces(LanternPainter painter, Vec3[] points, Vec3 normal, double[] radius,
            @Nullable Vec3 eye) {
        if (eye == null) {
            painter.mesh(Mesh.taper(ROUND, 1.35, normal, radius, points), WORLD, 1.0, 1.0);
            return;
        }
        int from = -1;
        for (int i = 0; i <= points.length; i++) {
            boolean shown = i < points.length && clear(points[i], eye);
            if (shown && from < 0) {
                from = i;
            } else if (!shown && from >= 0) {
                if (i - from >= 2) {
                    painter.mesh(Mesh.taper(ROUND, 1.35, normal, Arrays.copyOfRange(radius, from, i),
                            Arrays.copyOfRange(points, from, i)), WORLD, 1.0, 1.0);
                }
                from = -1;
            }
        }
    }

    private static boolean clear(Vec3 at, @Nullable Vec3 eye) {
        return eye == null || at.distanceToSqr(eye) >= NEAR_EYE * NEAR_EYE;
    }

    // The first bit of the lash, drawn with the hand in first person to cover where the two join.
    public static void stub(LanternPainter painter, Vec3 from, Vec3 to, Vec3 normal, double scale) {
        painter.ambient(0.12);
        painter.mesh(Mesh.taper(ROUND, 1.35, normal, new double[] { radius(0.0, scale), radius(0.03, scale) }, from,
                to), WORLD, 1.0, 1.0);
        painter.ambient(0.0);
    }

    private static void shatter(LanternPainter painter, Vec3[] points, Vec3 normal, double[] radius, double apart) {
        int n = points.length;
        Vec3 middle = points[n / 2];
        List<Mesh> pieces = new ArrayList<>();
        for (int k = 0; k < PIECES; k++) {
            int from = k * (n - 1) / PIECES;
            int to = (k + 1) * (n - 1) / PIECES;
            if (to <= from) {
                continue;
            }
            Vec3[] piece = new Vec3[to - from + 1];
            double[] thick = new double[piece.length];
            for (int i = from; i <= to; i++) {
                piece[i - from] = points[i].subtract(middle);
                thick[i - from] = radius[i];
            }
            pieces.add(Mesh.taper(ROUND, 1.35, normal, thick, piece));
        }
        painter.fling(FLING);
        painter.shattered(ConstructPainter.Shape.of(pieces.toArray(Mesh[]::new)), new ConstructPainter.Frame(middle,
                WORLD.right(), WORLD.up(), WORLD.forward(), 1.0), apart, 1.2);
        painter.fling(1.0);
    }

    private static ConstructPainter.Frame placed(Vec3 at, Vec3 along, double scale) {
        Vec3 up = along.normalize();
        Vec3 side = Math.abs(up.y) < 0.9 ? up.cross(Vectors.UP) : up.cross(new Vec3(1.0, 0.0, 0.0));
        return ConstructPainter.Frame.of(at, side.normalize(), up, scale);
    }

    private static double length(Vec3[] points) {
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += points[i].distanceTo(points[i - 1]);
        }
        return total;
    }

    // A faint glow at the tip, and white heat where the lash is still pouring out.
    public static void ember(LanternPainter painter, Vec3 tip, double scale, double strength) {
        if (strength > 0.01) {
            painter.flare(tip, 0.07 * scale * (0.7 + 0.6 * strength), strength);
        }
    }

    // The tip breaks the sound barrier: a flash, a cone of shock rings running on ahead, and sparks.
    public static void crack(LanternPainter painter, Vec3 at, Vec3 way, double since, double strength) {
        if (since < 0.0 || since > CRACK_TICKS || strength <= 0.0) {
            return;
        }
        double u = since / CRACK_TICKS;
        double fade = (1.0 - u) * (1.0 - u);
        Vec3 ahead = way.lengthSqr() < 1.0E-8 ? Vectors.UP : way.normalize();
        Vec3[] across = Vectors.across(ahead);
        painter.flare(at, 0.42 * strength * (0.6 + 0.8 * u), fade * Math.min(1.0, strength * 1.3));
        for (int k = 0; k < 3; k++) {
            double ring = u - k * 0.12;
            if (ring <= 0.0) {
                continue;
            }
            double reach = strength * (0.12 + 0.9 * Math.sqrt(ring)) * (1.0 - 0.2 * k);
            Vec3 center = at.add(ahead.scale(0.35 * strength * ring - 0.18 * k * ring));
            double alpha = fade * (1.0 - 0.28 * k);
            painter.circle(center, across[0], across[1], reach, 0.035, 0.22, Colors.alpha(0.9 * alpha),
                    Colors.alpha(0.4 * alpha));
        }
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * k / 8.0 + 0.4;
            Vec3 out = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle))).add(ahead.scale(0.6))
                    .normalize();
            double reach = strength * (0.1 + 0.55 * Math.sqrt(u)) * (0.75 + 0.07 * ((k * 5) % 7));
            painter.edge(at.add(out.scale(reach * 0.45)), at.add(out.scale(reach)), 0.02, fade);
        }
    }

    public static void trail(LanternPainter painter, List<Vec3> tips, List<Vec3> roots, double strength) {
        SwordPainter.trail(painter, tips, roots, Mth.clamp(strength, 0.0, 1.0));
    }

    // The whirl: a ring of light where the tip goes round.
    public static void ring(LanternPainter painter, Vec3 center, double radius, double strength) {
        if (strength <= 0.01) {
            return;
        }
        painter.circle(center, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius, 0.04, 0.32,
                Colors.alpha(0.55 * strength), Colors.alpha(0.3 * strength));
        painter.circle(center.add(0.0, -0.06, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius * 0.93,
                0.02, 0.18, Colors.alpha(0.3 * strength), Colors.alpha(0.15 * strength));
    }

    // The spinning shield: a faint disc of light over the lash going round, with a bright rim. Faint when it is
    // your own, so you still see through it.
    public static void disc(LanternPainter painter, Vec3 center, Vec3 normal, double radius, double spin,
            double strength, boolean own) {
        if (strength <= 0.01 || radius <= 1.0E-3) {
            return;
        }
        Vec3[] across = Vectors.across(normal.normalize());
        double show = strength * (own ? 0.4 : 1.0);
        painter.circle(center, across[0], across[1], radius, 0.035, 0.26, Colors.alpha(0.75 * show),
                Colors.alpha(0.35 * show));
        int slices = 16;
        for (int k = 0; k < slices; k++) {
            double a0 = spin + Math.PI * 2.0 * k / slices;
            double a1 = spin + Math.PI * 2.0 * (k + 1) / slices;
            Vec3 r0 = across[0].scale(Math.cos(a0)).add(across[1].scale(Math.sin(a0))).scale(radius);
            Vec3 r1 = across[0].scale(Math.cos(a1)).add(across[1].scale(Math.sin(a1))).scale(radius);
            double shine = 0.08 + 0.1 * Ease.bump(((k + spin * 1.5) % slices) / 3.0 - 1.0);
            painter.sheet(center, center.add(r0.scale(0.5)), center.add(r1.scale(0.5)), center, 0.0,
                    shine * 0.5 * show, shine * 0.5 * show, 0.0);
            painter.sheet(center.add(r0.scale(0.5)), center.add(r0), center.add(r1), center.add(r1.scale(0.5)),
                    shine * 0.5 * show, shine * show, shine * show, shine * 0.5 * show);
        }
    }
}
