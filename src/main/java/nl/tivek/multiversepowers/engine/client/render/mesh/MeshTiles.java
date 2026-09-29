package nl.tivek.multiversepowers.engine.client.render.mesh;

import java.util.Arrays;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh.Builder;
import nl.tivek.multiversepowers.engine.math.Noise;

// Builds Mesh.tiled: a Surface cut into cells, the cells joined into tiles of one to three, each tile a curved plate
// standing on the surface with a glowing bevelled rim, the surface itself dark in the gaps between them.
final class MeshTiles {
    private static final double BEND = Math.toRadians(14.0);
    private static final double BASE_BEND = Math.toRadians(24.0);
    private static final int MOST = 8;
    private static final double BASE_DARK = 0.55;
    private static final double RIM_BRIGHT = 1.12;
    private static final double SINK = 0.3;
    private static final double BASE_DEPTH = 0.6;
    private static final double TOO_SMALL = 0.45;
    private static final double SMALLEST = 2.2;
    private static final double INSET = 0.3;

    private MeshTiles() {
    }

    static Mesh build(Surface surface, boolean round, boolean base, Mesh.Tiles tiles, double bright) {
        Builder builder = new Builder();
        builder.skin = true;
        int across = Math.max(1, tiles.across());
        int along = Math.max(1, tiles.along());
        if (base) {
            // Sunk below the tiles' feet, so its flat pieces never cut through their rims.
            Surface under = surface.offset(-tiles.rise() * BASE_DEPTH, round);
            grid(builder, under, round, across * split(surface, round, across, along, true, BASE_BEND),
                    along * split(surface, round, across, along, false, BASE_BEND), bright * BASE_DARK);
        }
        double half = tiles.gap() * 0.5;
        double least = SMALLEST * (tiles.gap() + 2.0 * tiles.cut());
        boolean[][] taken = new boolean[along][across];
        int count = 0;
        // A round surface closing to a point gets one round cap there instead of a ring of wedges meeting in a cross.
        for (int end = 0; end < 2 && round && along > 2; end++) {
            double pole = end;
            if (!pole(surface, pole)) {
                continue;
            }
            int row = end == 0 ? 0 : along - 1;
            Arrays.fill(taken[row], true);
            double edge = end == 0 ? 1.0 / along : 1.0 - 1.0 / along;
            cap(builder, surface, pole, edge, Math.max(8, Math.min(48, across * 2)), tiles.rise(), half,
                    tiles.cut(), bright * (0.88 + 0.24 * Noise.of(tiles.seed(), end, 16)), tiles.glow());
        }
        for (int i = 0; i < along; i++) {
            double v0 = (double) i / along;
            double v1 = (double) (i + 1) / along;
            int start = round ? (int) (Noise.of(tiles.seed(), i, 11) * across) : 0;
            for (int c = 0; c < across; c++) {
                int j = (start + c) % across;
                if (taken[i][j]) {
                    continue;
                }
                double roll = Noise.of(tiles.seed(), i * 131 + j, 12);
                int wide = roll < tiles.merge() * 0.3 ? 3 : roll < tiles.merge() ? 2 : 1;
                int room = 0;
                while (room < across - c && !taken[i][(j + room) % across] && (round || j + room < across)) {
                    room++;
                }
                int most = round ? Math.min(room, Math.max(1, across / 2)) : room;
                wide = Math.min(wide, most);
                double vMiddle = (v0 + v1) * 0.5;
                while (wide < most && width(surface, j, wide, across, vMiddle) < least) {
                    wide++;
                }
                int tall = 1;
                if (i + 1 < along && Noise.of(tiles.seed(), i * 131 + j, 13) < tiles.merge() * 0.3) {
                    boolean free = true;
                    for (int k = 0; k < wide; k++) {
                        free &= !taken[i + 1][(j + k) % across];
                    }
                    tall = free ? 2 : 1;
                }
                for (int a = 0; a < tall; a++) {
                    for (int k = 0; k < wide; k++) {
                        taken[i + a][(j + k) % across] = true;
                    }
                }
                double u0 = (double) j / across;
                double u1 = (double) (j + wide) / across;
                double top = (double) (i + tall) / along;
                // Only a sliver right at a pole is left bare; anything bigger gets a tile, its rim cut down to fit.
                if (width(surface, j, wide, across, (v0 + top) * 0.5) < least * TOO_SMALL
                        || surface.at((u0 + u1) * 0.5, v0).distanceTo(surface.at((u0 + u1) * 0.5, top))
                                < least * TOO_SMALL) {
                    continue;
                }
                double rise = tiles.rise() * (0.7 + 0.6 * Noise.of(tiles.seed(), count, 14));
                double shade = bright * (0.88 + 0.24 * Noise.of(tiles.seed(), count, 15));
                tile(builder, surface, round, u0, u1, v0, top, rise, half, tiles.cut(), shade, tiles.glow());
                count++;
            }
        }
        return builder.build();
    }

    // The surface as a sheet so thick, its inner side offset inwards and its open ends closed by rims.
    static Mesh sheet(Surface surface, boolean round, int across, int along, double thick, double bright) {
        Builder builder = new Builder();
        int columns = round ? across : across + 1;
        int[][] out = new int[along + 1][columns];
        int[][] in = new int[along + 1][columns];
        for (int i = 0; i <= along; i++) {
            double v = (double) i / along;
            for (int j = 0; j < columns; j++) {
                double u = (double) j / across;
                Vec3 at = surface.at(u, v);
                out[i][j] = builder.point(at);
                in[i][j] = builder.point(at.subtract(normal(surface, round, u, v).scale(thick)));
            }
        }
        for (int i = 0; i < along; i++) {
            for (int j = 0; j < across; j++) {
                int k = round ? (j + 1) % across : j + 1;
                builder.side(out[i][j], out[i + 1][j], out[i + 1][k], out[i][k], bright);
                builder.side(in[i][k], in[i + 1][k], in[i + 1][j], in[i][j], bright);
            }
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : along;
            int next = end == 0 ? 1 : along - 1;
            for (int j = 0; j < across; j++) {
                int k = round ? (j + 1) % across : j + 1;
                Vec3 inside = middle(builder, out[next][j], out[next][k], in[next][j], in[next][k]);
                builder.outward(inside, bright, out[i][j], out[i][k], in[i][k], in[i][j]);
            }
        }
        if (!round) {
            for (int end = 0; end < 2; end++) {
                int j = end == 0 ? 0 : across;
                int next = end == 0 ? 1 : across - 1;
                for (int i = 0; i < along; i++) {
                    Vec3 inside = middle(builder, out[i][next], out[i + 1][next], in[i][next], in[i + 1][next]);
                    builder.outward(inside, bright, out[i][j], out[i + 1][j], in[i + 1][j], in[i][j]);
                }
            }
        }
        return builder.build();
    }

    private static Vec3 middle(Builder builder, int a, int b, int c, int d) {
        return builder.at(a).add(builder.at(b)).add(builder.at(c)).add(builder.at(d)).scale(0.25);
    }

    private static double width(Surface surface, int from, int cells, int across, double v) {
        double length = 0.0;
        Vec3 before = surface.at((double) from / across, v);
        for (int k = 1; k <= cells; k++) {
            Vec3 now = surface.at((double) (from + k) / across, v);
            length += now.distanceTo(before);
            before = now;
        }
        return length;
    }

    // How many pieces each cell needs across (or along) so no piece bends more than so much.
    private static int split(Surface surface, boolean round, int across, int along, boolean acrossWay, double bend) {
        int most = 1;
        for (int i = 0; i <= along; i++) {
            for (int j = 0; j < across; j++) {
                double u = (double) j / across;
                double v = (double) i / along;
                Vec3 a = normal(surface, round, u, v);
                Vec3 b = acrossWay ? normal(surface, round, (double) (j + 1) / across, v)
                        : normal(surface, round, u, Math.min(1.0, (double) (i + 1) / along));
                double angle = Math.acos(Math.max(-1.0, Math.min(1.0, a.dot(b))));
                most = Math.max(most, (int) Math.ceil(angle / bend));
            }
        }
        return Math.min(MOST, most);
    }

    private static void grid(Builder builder, Surface surface, boolean round, int across, int along,
            double bright) {
        int columns = round ? across : across + 1;
        int[][] at = new int[along + 1][columns];
        for (int i = 0; i <= along; i++) {
            double v = (double) i / along;
            Vec3 first = surface.at(0.0, v);
            boolean pole = true;
            for (int j = 1; j < columns && pole; j++) {
                pole = surface.at((double) j / across, v).distanceToSqr(first) < 1.0E-12;
            }
            for (int j = 0; j < columns; j++) {
                at[i][j] = pole && j > 0 ? at[i][0] : builder.point(surface.at((double) j / across, v));
            }
        }
        for (int i = 0; i < along; i++) {
            for (int j = 0; j < across; j++) {
                int k = round ? (j + 1) % across : j + 1;
                builder.side(at[i][j], at[i + 1][j], at[i + 1][k], at[i][k], bright);
            }
        }
    }

    private static void tile(Builder builder, Surface surface, boolean round, double u0, double u1, double v0,
            double v1, double rise, double half, double cut, double bright, double glow) {
        double du = u1 - u0;
        double dv = v1 - v0;
        double uc = (u0 + u1) * 0.5;
        double vc = (v0 + v1) * 0.5;
        double lu = surface.at(u1, vc).distanceTo(surface.at(u0, vc)) / du;
        double lv = surface.at(uc, v1).distanceTo(surface.at(uc, v0)) / dv;
        if (lu < 1.0E-9 || lv < 1.0E-9) {
            return;
        }
        double tu = Math.min((half + cut) / lu, INSET * du);
        double tv = Math.min((half + cut) / lv, INSET * dv);
        double bu = Math.min(half / lu, tu * 0.6);
        double bv = Math.min(half / lv, tv * 0.6);
        int su = pieces(surface, round, u0 + tu, vc, u1 - tu, vc);
        int sv = pieces(surface, round, uc, v0 + tv, uc, v1 - tv);
        int[][] top = new int[sv + 1][su + 1];
        for (int a = 0; a <= sv; a++) {
            for (int b = 0; b <= su; b++) {
                double u = u0 + tu + (du - 2.0 * tu) * b / su;
                double v = v0 + tv + (dv - 2.0 * tv) * a / sv;
                top[a][b] = builder.point(surface.at(u, v).add(normal(surface, round, u, v).scale(rise)));
            }
        }
        int ring = 2 * (su + sv);
        int[] rim = new int[ring];
        int[] foot = new int[ring];
        int k = 0;
        for (int b = 0; b < su; b++) {
            rim[k] = top[0][b];
            foot[k++] = footAt(builder, surface, round, u0 + bu + (du - 2.0 * bu) * b / su, v0 + bv, rise);
        }
        for (int a = 0; a < sv; a++) {
            rim[k] = top[a][su];
            foot[k++] = footAt(builder, surface, round, u1 - bu, v0 + bv + (dv - 2.0 * bv) * a / sv, rise);
        }
        for (int b = su; b > 0; b--) {
            rim[k] = top[sv][b];
            foot[k++] = footAt(builder, surface, round, u0 + bu + (du - 2.0 * bu) * b / su, v1 - bv, rise);
        }
        for (int a = sv; a > 0; a--) {
            rim[k] = top[a][0];
            foot[k++] = footAt(builder, surface, round, u0 + bu, v0 + bv + (dv - 2.0 * bv) * a / sv, rise);
        }
        for (int a = 0; a < sv; a++) {
            for (int b = 0; b < su; b++) {
                builder.side(top[a][b], top[a + 1][b], top[a + 1][b + 1], top[a][b + 1], bright);
            }
        }
        Vec3 inside = surface.at(uc, vc).subtract(normal(surface, round, uc, vc)
                .scale(rise + 0.5 * Math.min(lu * du, lv * dv)));
        for (int r = 0; r < ring; r++) {
            int next = (r + 1) % ring;
            builder.outward(inside, bright * RIM_BRIGHT, glow, rim[r], rim[next], foot[next], foot[r]);
        }
    }

    private static boolean pole(Surface surface, double v) {
        Vec3 first = surface.at(0.0, v);
        for (int k = 1; k < 8; k++) {
            if (surface.at(k / 8.0, v).distanceToSqr(first) > 1.0E-10) {
                return false;
            }
        }
        return true;
    }

    // A round plate over a pole, from the pole out to the ring at v edge, raised and rimmed like a tile.
    private static void cap(Builder builder, Surface surface, double pole, double edge, int around, double rise,
            double half, double cut, double bright, double glow) {
        double lv = surface.at(0.0, edge).distanceTo(surface.at(0.0, pole)) / Math.abs(edge - pole);
        if (lv < 1.0E-9) {
            return;
        }
        double toward = Math.signum(pole - edge);
        double tv = Math.min((half + cut) / lv, INSET * Math.abs(edge - pole));
        double bv = Math.min(half / lv, tv * 0.6);
        double rimV = edge + toward * tv;
        int steps = Math.max(1, Math.min(MOST, pieces(surface, true, 0.0, pole, 0.0, rimV)));
        Vec3 poleNormal = normal(surface, true, 0.0, pole);
        int middle = builder.point(surface.at(0.0, pole).add(poleNormal.scale(rise)));
        int[][] ring = new int[steps][around];
        for (int a = 0; a < steps; a++) {
            double v = pole + (rimV - pole) * (a + 1.0) / steps;
            for (int k = 0; k < around; k++) {
                double u = (double) k / around;
                ring[a][k] = builder.point(surface.at(u, v).add(normal(surface, true, u, v).scale(rise)));
            }
        }
        int[] foot = new int[around];
        for (int k = 0; k < around; k++) {
            foot[k] = footAt(builder, surface, true, (double) k / around, edge + toward * bv, rise);
        }
        double reach = surface.at(0.0, rimV).distanceTo(surface.at(0.5, rimV)) * 0.5;
        Vec3 inside = surface.at(0.0, pole).subtract(poleNormal.scale(rise + reach));
        for (int k = 0; k < around; k++) {
            int next = (k + 1) % around;
            builder.outward(inside, bright, middle, ring[0][k], ring[0][next], ring[0][next]);
            for (int a = 0; a + 1 < steps; a++) {
                builder.outward(inside, bright, ring[a][k], ring[a + 1][k], ring[a + 1][next], ring[a][next]);
            }
            builder.outward(inside, bright * RIM_BRIGHT, glow, ring[steps - 1][k], ring[steps - 1][next], foot[next],
                    foot[k]);
        }
    }

    private static int footAt(Builder builder, Surface surface, boolean round, double u, double v, double rise) {
        return builder.point(surface.at(u, v).subtract(normal(surface, round, u, v).scale(rise * SINK)));
    }

    private static int pieces(Surface surface, boolean round, double ua, double va, double ub, double vb) {
        Vec3 a = normal(surface, round, ua, va);
        Vec3 b = normal(surface, round, ub, vb);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, a.dot(b))));
        return Math.max(1, Math.min(MOST, (int) Math.ceil(angle / BEND)));
    }

    private static Vec3 normal(Surface surface, boolean round, double u, double v) {
        return surface.outward(u, v, round);
    }
}
