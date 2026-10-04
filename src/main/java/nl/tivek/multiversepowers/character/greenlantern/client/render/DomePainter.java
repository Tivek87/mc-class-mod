package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The light dome: a geodesic shell of hard-light plates, hexagons round twelve pentagons, each a raised slab with a
// bright bevelled rim over the seams sunk between them. It grows plate by plate out of the ring's light, a wave
// running over it from the side the ring is on; a blow sends a bright ripple over it from where it struck, the plates
// there giving a little; lowered, the plates break away one after another, flung out tumbling until none is left.
// Seen by its owner, or from inside it, it is see-through.
public final class DomePainter {
    // The plates come from an icosahedron whose every edge is cut into this many parts.
    private static final int CUTS = 3;
    private static final double THICK = 0.07;
    private static final double CUT = 0.03;
    // A plate is a little larger than its cell, so neighbours meet whatever their tilt.
    private static final double OVERLAP = 1.03;
    // How much of the rise the wave takes to run over the shell, and how far a plate rises into its place (radii).
    private static final double SPREAD = 0.6;
    private static final double RISE = 0.2;
    // Breaking: how much of it the plates take to start going, how far one is flung out and falls (radii), and how
    // far it tumbles (radians) as it shrinks away.
    private static final double BREAK_SPREAD = 0.55;
    private static final double FLING = 0.9;
    private static final double DROP = 0.7;
    private static final double TUMBLE = 4.0;
    // A blow's ripple: how wide its bright band is (half turns), how far it runs, and how far the plates it struck give.
    private static final double RIPPLE = 0.22;
    private static final double RIPPLE_RUN = 1.3;
    private static final double GIVE = 0.07;
    private static final Plate[] PLATES = plates();

    // A plate: the way it faces out, how far out its back lies (radii), its own axes and its shape in them.
    private record Plate(Vec3 normal, double depth, Vec3 right, Vec3 up, ConstructPainter.Shape shape) {
    }

    private DomePainter() {
    }

    // `solid` how far it has risen, or breaking how much of it is left; `struck` the way from its middle to the last
    // blow and `flash` how fresh that blow is (1 just struck, 0 long past).
    public static void draw(LanternPainter painter, Vec3 center, double size, double solid, boolean breaking,
            double flash, Vec3 struck, @Nullable Vec3 ring, boolean own) {
        double shown = Mth.clamp(solid, 0.0, 1.0);
        if (shown <= 0.0) {
            return;
        }
        double radius = size * 0.5;
        boolean see = own || painter.camera().distanceTo(center) < radius;
        Vec3 from = ring == null || ring.distanceToSqr(center) < 1.0E-4 ? new Vec3(0.0, -1.0, 0.0)
                : ring.subtract(center).normalize();
        Vec3 hit = struck.lengthSqr() < 1.0E-6 ? Vectors.UP : struck.normalize();
        double fresh = Mth.clamp(flash, 0.0, 1.0);
        double front = (1.0 - fresh) * RIPPLE_RUN;
        painter.batch();
        for (int k = 0; k < PLATES.length; k++) {
            Plate plate = PLATES[k];
            double scale = 1.0;
            double depth = plate.depth();
            double bright = 1.0;
            Vec3 out = Vec3.ZERO;
            double tumble = 0.0;
            if (breaking) {
                double delay = BREAK_SPREAD * (0.65 * (1.0 - plate.normal().y) * 0.5 + 0.35 * Noise.of(k, 3, 1));
                double gone = Mth.clamp((1.0 - shown - delay) / (1.0 - BREAK_SPREAD), 0.0, 1.0);
                if (gone >= 1.0) {
                    continue;
                }
                scale = 1.0 - gone;
                out = plate.normal().scale(FLING * radius * gone).add(0.0, -DROP * radius * gone * gone, 0.0);
                tumble = TUMBLE * gone;
                bright += 0.6 * gone;
            } else {
                double delay = SPREAD * angle(plate.normal(), from);
                double grown = Mth.clamp((shown - delay) / (1.0 - SPREAD), 0.0, 1.0);
                if (grown <= 0.0) {
                    continue;
                }
                double eased = Ease.smooth(grown);
                scale = eased * (1.0 + 0.12 * Math.sin(Math.PI * grown));
                depth -= RISE * (1.0 - eased);
                // Each plate glows bright as it forms out of the light, settling as it sets.
                bright += 1.4 * (1.0 - grown);
            }
            if (fresh > 0.0) {
                double far = angle(plate.normal(), hit);
                double band = Math.max(0.0, 1.0 - Math.abs(far - front) / RIPPLE);
                double near = Math.max(0.0, 1.0 - far / 0.2) * fresh;
                bright += 1.2 * band * fresh + 1.6 * near;
                depth -= GIVE * near;
            }
            if (scale <= 1.0E-3) {
                continue;
            }
            Frame frame = new Frame(center.add(plate.normal().scale(depth * radius)).add(out), plate.right(),
                    plate.up(), plate.normal(), radius * scale);
            if (tumble > 0.0) {
                Vec3 axis = Noise.direction(k, 5);
                frame = frame.turned(0.0, 0.0, THICK * 0.5, axis.x, axis.y, axis.z, tumble);
            }
            if (see) {
                painter.seeThrough(plate.shape(), frame, 0.12 + 0.12 * (bright - 1.0), bright);
            } else {
                painter.shape(plate.shape(), frame, 1.0, bright);
            }
        }
        painter.flush();
        if (!breaking && shown < 1.0 && ring != null) {
            painter.beam(ring, center.add(from.scale(radius)), 1.0 - shown, radius);
        }
    }

    // How far apart two ways are, 0 the same to 1 opposite.
    private static double angle(Vec3 a, Vec3 b) {
        return Math.acos(Mth.clamp(a.dot(b), -1.0, 1.0)) / Math.PI;
    }

    // Twelve pentagons and the hexagons between them on the unit sphere: the cells round each corner of a cut-up
    // icosahedron, each made a plate in its own axes (built as every frame of the painter's is) facing out.
    private static Plate[] plates() {
        double g = (1.0 + Math.sqrt(5.0)) / 2.0;
        double[][] corners = { { -1, g, 0 }, { 1, g, 0 }, { -1, -g, 0 }, { 1, -g, 0 }, { 0, -1, g }, { 0, 1, g },
                { 0, -1, -g }, { 0, 1, -g }, { g, 0, -1 }, { g, 0, 1 }, { -g, 0, -1 }, { -g, 0, 1 } };
        int[][] faces = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 },
                { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 }, { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 },
                { 3, 6, 8 }, { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };
        // Turned so a pentagon crowns it.
        Vec3 top = new Vec3(corners[5][0], corners[5][1], corners[5][2]).normalize();
        double crown = -Math.atan2(top.z, top.y);
        Vec3[] ico = new Vec3[corners.length];
        for (int i = 0; i < corners.length; i++) {
            ico[i] = Vectors.spin(new Vec3(corners[i][0], corners[i][1], corners[i][2]).normalize(),
                    new Vec3(1.0, 0.0, 0.0), crown);
        }
        List<Vec3> points = new ArrayList<>();
        Map<Long, Integer> known = new HashMap<>();
        List<int[]> triangles = new ArrayList<>();
        for (int[] face : faces) {
            Vec3 a = ico[face[0]];
            Vec3 b = ico[face[1]];
            Vec3 c = ico[face[2]];
            int[][] grid = new int[CUTS + 1][CUTS + 1];
            for (int i = 0; i <= CUTS; i++) {
                for (int j = 0; i + j <= CUTS; j++) {
                    Vec3 p = a.add(b.subtract(a).scale((double) i / CUTS)).add(c.subtract(a).scale((double) j / CUTS))
                            .normalize();
                    grid[i][j] = point(p, points, known);
                }
            }
            for (int i = 0; i < CUTS; i++) {
                for (int j = 0; i + j < CUTS; j++) {
                    triangles.add(new int[] { grid[i][j], grid[i + 1][j], grid[i][j + 1] });
                    if (i + j < CUTS - 1) {
                        triangles.add(new int[] { grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1] });
                    }
                }
            }
        }
        List<List<Vec3>> cells = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            cells.add(new ArrayList<>());
        }
        for (int[] t : triangles) {
            Vec3 middle = points.get(t[0]).add(points.get(t[1])).add(points.get(t[2])).normalize();
            for (int corner : t) {
                cells.get(corner).add(middle);
            }
        }
        Plate[] plates = new Plate[points.size()];
        for (int i = 0; i < plates.length; i++) {
            plates[i] = plate(points.get(i), cells.get(i));
        }
        return plates;
    }

    private static int point(Vec3 p, List<Vec3> points, Map<Long, Integer> known) {
        long key = Math.round(p.x * 1.0E4) * 1_000_003L * 1_000_003L + Math.round(p.y * 1.0E4) * 1_000_003L
                + Math.round(p.z * 1.0E4);
        return known.computeIfAbsent(key, k -> {
            points.add(p);
            return points.size() - 1;
        });
    }

    private static Plate plate(Vec3 normal, List<Vec3> cell) {
        Frame axes = Frame.of(Vec3.ZERO, normal, Vectors.UP, 1.0);
        Vec3 right = axes.right();
        Vec3 up = axes.up();
        cell.sort((p, q) -> Double.compare(Math.atan2(p.dot(up), p.dot(right)), Math.atan2(q.dot(up), q.dot(right))));
        double depth = 0.0;
        for (Vec3 corner : cell) {
            depth += corner.dot(normal) / cell.size();
        }
        double[] outline = new double[cell.size() * 2];
        for (int i = 0; i < cell.size(); i++) {
            Vec3 corner = cell.get(i);
            outline[2 * i] = corner.dot(right) * OVERLAP;
            outline[2 * i + 1] = corner.dot(up) * OVERLAP;
        }
        return new Plate(normal, depth, right, up, ConstructPainter.Shape.of(Mesh.plate(0.0, THICK, CUT, 1.0,
                outline)));
    }
}
