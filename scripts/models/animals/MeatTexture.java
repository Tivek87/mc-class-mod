import java.io.IOException;
import java.nio.file.Path;

// Paints the wild animals' meat (textures/item/raw_venison.png, cooked_venison.png, raw_bear_meat.png and
// cooked_bear_meat.png) from scratch, as the game's own items are drawn: a 16 pixel cut lying from corner to corner,
// lit from the top left, with a dark outline. Venison is a long lean cut, deep red with a pale edge of skin; bear meat a
// thicker, darker chunk marbled with fat under a cap of it. Cooked, each turns brown with darker grill marks.
// Run from the repository root: java scripts/models/animals/MeatTexture.java [assets folder]
public final class MeatTexture {
    record Cut(String name, double length, double width, int[] meat, int[] grain, int[] edge, int[] outline,
            boolean marbled, boolean fatCap, boolean grilled) {
    }

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "item");
        int[] fat = Pelt.rgb(236, 220, 200);
        int[] cookedFat = Pelt.rgb(206, 164, 104);
        paint(folder, new Cut("raw_venison", 6.9, 3.6, Pelt.rgb(146, 28, 38), Pelt.rgb(104, 16, 26),
                Pelt.rgb(214, 150, 150), Pelt.rgb(58, 10, 14), false, false, false));
        paint(folder, new Cut("cooked_venison", 6.9, 3.6, Pelt.rgb(122, 70, 42), Pelt.rgb(82, 44, 24),
                Pelt.rgb(166, 108, 66), Pelt.rgb(46, 24, 12), false, false, true));
        paint(folder, new Cut("raw_bear_meat", 6.6, 4.8, Pelt.rgb(116, 24, 36), Pelt.rgb(84, 14, 24), fat,
                Pelt.rgb(48, 8, 14), true, true, false));
        paint(folder, new Cut("cooked_bear_meat", 6.6, 4.8, Pelt.rgb(94, 52, 32), Pelt.rgb(62, 32, 18), cookedFat,
                Pelt.rgb(38, 20, 10), true, true, true));
    }

    static void paint(Path folder, Cut cut) throws IOException {
        Pelt p = new Pelt(16, 16, cut.name.hashCode());
        boolean[][] in = new boolean[16][16];
        double c = Math.sqrt(0.5);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - 8.0;
                double dy = y + 0.5 - 8.0;
                // Along the cut (bottom left to top right) and across it.
                double u = (dx - dy) * c;
                double v = (dx + dy) * c;
                double rough = 1.0 + 0.12 * (p.noise(x, y, 1) - 0.5);
                in[y][x] = (u * u) / (cut.length * cut.length) + (v * v) / (cut.width * cut.width) <= rough;
            }
        }
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int[] colour = null;
                if (in[y][x]) {
                    colour = meat(p, cut, x, y, c);
                } else if (near(in, x, y)) {
                    colour = cut.outline;
                }
                if (colour != null) {
                    p.image.setRGB(x, y, Pelt.argb(colour));
                }
            }
        }
        p.save(folder, cut.name);
    }

    static int[] meat(Pelt p, Cut cut, int x, int y, double c) {
        double dx = x + 0.5 - 8.0;
        double dy = y + 0.5 - 8.0;
        double u = (dx - dy) * c;
        double v = (dx + dy) * c;
        double across = v / cut.width;
        int[] colour = cut.meat;
        // The grain running along the cut.
        if (Math.sin(v * 2.4 + 1.3 * p.noise(x, y, 2)) > 0.82) {
            colour = cut.grain;
        }
        if (cut.marbled && p.noise(x, y, 3) < 0.13 && Math.abs(across) < 0.6) {
            colour = Pelt.mix(cut.meat, cut.edge, 0.75);
        }
        if (cut.fatCap && across < -0.55) {
            colour = Pelt.mix(cut.edge, Pelt.rgb(255, 255, 255), 0.1 * p.noise(x, y, 4));
        } else if (!cut.fatCap && across > 0.72) {
            colour = cut.edge;
        }
        if (cut.grilled && Math.abs(across) < 0.5 && ((int) Math.floor(u + 8.0)) % 4 == 0) {
            colour = Pelt.scale(colour, 0.62);
        }
        // Lit from the top left.
        double light = 1.12 - 0.3 * (x + y) / 30.0;
        return Pelt.scale(colour, light * (0.95 + 0.1 * p.noise(x, y, 5)));
    }

    static boolean near(boolean[][] in, int x, int y) {
        for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && in[ny][nx]) {
                return true;
            }
        }
        return false;
    }
}
