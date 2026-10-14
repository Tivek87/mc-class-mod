import java.io.IOException;
import java.nio.file.Path;

// Paints the snakes' textures (textures/entity/snake_grass.png and snake_adder.png) from scratch, piece by piece as
// SnakeModel lays it out, each piece's markings counted on from the one before so they run unbroken from neck to
// tail. The grass snake: olive green, flecked darker, a yellow collar edged black behind its head, short black bars
// down its sides, a pale chequered belly and pale lips. The adder: grey-brown with a dark zigzag down its back, a
// row of dark spots along each side, a dark V on its head, a red eye and a dark belly. Both have a pink mouth and a
// dark forked tongue.
// Run from the repository root: java scripts/models/animals/SnakeTexture.java [assets folder]
public final class SnakeTexture {
    static final int[] MOUTH = Pelt.rgb(216, 132, 132);
    static final int[] TONGUE = Pelt.rgb(58, 16, 22);
    static final int[] BLACK = Pelt.rgb(22, 20, 18);
    // Pixels along the body, from the neck: ten pieces of two, then the thin tail.
    static final int PIECES = 10;

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, "snake_grass", false);
        paint(folder, "snake_adder", true);
    }

    static void paint(Path folder, String name, boolean adder) throws IOException {
        Pelt p = new Pelt(64, 32, name.hashCode());
        p.paint(new Pelt.Box("skull", 0, 0, 3, 1, 3), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM ? MOUTH
                : head(p, adder, f, x, z, px, py, true));
        p.paint(new Pelt.Box("jaw", 12, 0, 3, 1, 3), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH
                : head(p, adder, f, x, z, px, py, false));
        p.paint(new Pelt.Box("tongue", 24, 0, 1, 0, 2), (f, x, y, z, px, py) -> TONGUE);
        p.paint(new Pelt.Box("snout", 30, 0, 2, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM ? MOUTH
                : head(p, adder, f, x, 0.0, px, py, true));
        p.paint(new Pelt.Box("chin", 36, 0, 2, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH
                : head(p, adder, f, x, 0.0, px, py, false));
        for (int k = 0; k < PIECES; k++) {
            int piece = k;
            p.paint(new Pelt.Box("piece " + k, k % 8 * 8, 4 + k / 8 * 4, 2, 2, 2), (f, x, y, z, px, py) -> body(p,
                    adder, f, x, y, piece * 2 + (int) (z * 2), px, py));
        }
        for (int k = 0; k < 3; k++) {
            int piece = k;
            p.paint(new Pelt.Box("tail " + k, 16 + k * 6, 8, 1, 1, 2), (f, x, y, z, px, py) -> body(p, adder, f, 0.5,
                    y, PIECES * 2 + piece * 2 + (int) (z * 2), px, py));
        }
        p.save(folder, name);
    }

    static int[] ground(boolean adder) {
        return adder ? Pelt.rgb(152, 134, 104) : Pelt.rgb(98, 110, 56);
    }

    static int[] dark(boolean adder) {
        return adder ? Pelt.rgb(44, 32, 24) : Pelt.rgb(58, 68, 34);
    }

    static int[] belly(boolean adder) {
        return adder ? Pelt.rgb(74, 68, 62) : Pelt.rgb(214, 208, 162);
    }

    // `along` counts pixels from the neck.
    static int[] body(Pelt p, boolean adder, Pelt.Face f, double x, double y, int along, int px, int py) {
        int[] scales = Pelt.scale(ground(adder), 0.93 + 0.14 * p.noise(px, py, 3));
        if (f == Pelt.Face.BOTTOM) {
            // The grass snake's belly is chequered black.
            return !adder && (along + (x < 0.5 ? 0 : 1)) % 3 == 0 ? BLACK : belly(adder);
        }
        if (adder) {
            if (f == Pelt.Face.TOP) {
                // The zigzag: two pixels to one side, then two to the other.
                return (along / 2 % 2 == 0) == (x < 0.5) ? dark(true) : scales;
            }
            return y > 0.5 && along % 2 == 1 ? Pelt.mix(scales, dark(true), 0.75) : y > 0.5
                    ? Pelt.mix(scales, belly(true), 0.25) : scales;
        }
        if (along == 0 && f != Pelt.Face.FRONT) {
            return Pelt.rgb(230, 204, 72);
        }
        if (along == 1) {
            return BLACK;
        }
        if (f == Pelt.Face.TOP) {
            return p.noise(px, py, 4) < 0.2 ? Pelt.mix(scales, dark(false), 0.7) : scales;
        }
        return y > 0.5 && along % 2 == 0 ? BLACK : y > 0.5 ? Pelt.mix(scales, belly(false), 0.35) : scales;
    }

    static int[] head(Pelt p, boolean adder, Pelt.Face f, double x, double z, int px, int py, boolean skull) {
        int[] scales = Pelt.scale(Pelt.mix(ground(adder), dark(adder), adder ? 0.0 : 0.25), 0.94 + 0.12
                * p.noise(px, py, 3));
        int[] lip = adder ? Pelt.mix(ground(true), Pelt.rgb(226, 214, 190), 0.55) : Pelt.rgb(226, 220, 176);
        if (!skull) {
            return f == Pelt.Face.BOTTOM ? Pelt.mix(lip, belly(adder), adder ? 0.5 : 0.2) : lip;
        }
        if (f == Pelt.Face.TOP) {
            int col = (int) (x * 3);
            int row = (int) (z * 3);
            // The adder's V, its point toward the snout.
            if (adder && (row == 2 && col != 1 || row == 1 && col == 1)) {
                return dark(true);
            }
            return scales;
        }
        if (f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT) {
            int at = (int) (z * 3);
            return at == 1 ? adder ? Pelt.rgb(168, 52, 30) : BLACK : scales;
        }
        return scales;
    }
}
