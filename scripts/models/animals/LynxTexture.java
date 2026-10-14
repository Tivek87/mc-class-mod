import java.io.IOException;
import java.nio.file.Path;

// Paints the lynx's textures (textures/entity/lynx.png and lynx_pale.png) from scratch, box by box as LynxModel lays
// it out. A tawny, red-buff coat spotted dark brown over its back, flanks and legs, darker along the spine, paling to
// cream on its chest, belly and the insides of its legs; a face with dark lines up the brow, yellow eyes under darker
// lids, white
// round the eyes and muzzle, a pink nose, and a pale ruff barred black at its ends; ears black behind with a pale
// spot, pale inside, their long tufts black; the tip of the stubby tail black. The pale coat is its winter one: grey
// and thinly spotted.
// Run from the repository root: java scripts/models/animals/LynxTexture.java [assets folder]
public final class LynxTexture {
    record Coat(String name, int[] base, int[] spot, int[] pale, double spots) {
    }

    static final int[] BLACK = Pelt.rgb(24, 20, 18);
    static final int[] EYE = Pelt.rgb(204, 180, 74);
    static final int[] NOSE = Pelt.rgb(198, 122, 112);
    static final int[] MOUTH = Pelt.rgb(206, 132, 128);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("lynx", Pelt.rgb(192, 142, 88), Pelt.rgb(92, 62, 40), Pelt.rgb(236, 226, 206), 0.13));
        paint(folder, new Coat("lynx_pale", Pelt.rgb(184, 176, 160), Pelt.rgb(122, 110, 98), Pelt.rgb(240, 238, 232),
                0.07));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(64, 64, k.name.hashCode());
        p.paint(new Pelt.Box("body", 0, 0, 6, 6, 13), (f, x, y, z, px, py) -> {
            if (f == Pelt.Face.BOTTOM || f == Pelt.Face.FRONT && y > 0.4) {
                return p.fur(k.pale, null, 0.0, px, py);
            }
            double down = f == Pelt.Face.TOP ? 0.0 : y;
            int[] c = spotted(p, k, px, py, f == Pelt.Face.TOP && Math.abs(x - 0.5) < 0.17 ? 0.22 : 0.0);
            return Pelt.mix(c, k.pale, 0.85 * Pelt.step(0.6, 1.0, down));
        });
        p.paint(new Pelt.Box("head", 38, 0, 5, 5, 4), (f, x, y, z, px, py) -> head(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 0, 19, 3, 2, 2), (f, x, y, z, px, py) -> {
            if (f == Pelt.Face.FRONT) {
                boolean middle = (int) (x * 3) == 1;
                return middle ? y < 0.5 ? NOSE : Pelt.scale(k.pale, 0.7) : k.pale;
            }
            return f == Pelt.Face.TOP ? Pelt.mix(k.base, k.pale, 0.5) : k.pale;
        });
        p.paint(new Pelt.Box("ruff", 10, 19, 1, 3, 2), (f, x, y, z, px, py) -> y > 0.66 ? BLACK : y > 0.33
                && z > 0.5 ? Pelt.scale(k.pale, 0.82) : k.pale);
        p.paint(new Pelt.Box("ear", 16, 19, 2, 3, 1), (f, x, y, z, px, py) -> {
            if (f == Pelt.Face.TOP || f != Pelt.Face.BACK && y < 0.34) {
                return BLACK;
            }
            if (f == Pelt.Face.BACK) {
                return (int) (y * 3) == 1 ? Pelt.mix(k.pale, k.base, 0.4) : BLACK;
            }
            return f == Pelt.Face.FRONT ? Pelt.mix(k.pale, k.base, 0.25) : k.base;
        });
        p.paint(new Pelt.Box("tuft", 22, 19, 1, 2, 1), (f, x, y, z, px, py) -> BLACK);
        p.paint(new Pelt.Box("jaw", 26, 19, 3, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH : k.pale);
        p.paint(new Pelt.Box("tail", 36, 19, 2, 2, 3), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM ? k.pale
                : spotted(p, k, px, py, 0.0));
        p.paint(new Pelt.Box("tail tip", 38, 9, 2, 2, 2), (f, x, y, z, px, py) -> f == Pelt.Face.BACK || z > 0.4
                ? BLACK : f == Pelt.Face.BOTTOM ? k.pale : spotted(p, k, px, py, 0.0));
        p.paint(new Pelt.Box("eye", 56, 0, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT ? EYE
                : Pelt.scale(EYE, 0.6));
        p.paint(new Pelt.Box("front leg", 0, 28, 3, 6, 3), (f, x, y, z, px, py) -> leg(p, k, f, px, py, true));
        p.paint(new Pelt.Box("front lower", 12, 28, 2, 3, 2), (f, x, y, z, px, py) -> leg(p, k, f, px, py, false));
        p.paint(new Pelt.Box("paw", 20, 28, 3, 1, 3), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                && (int) (x * 3) == 1 ? Pelt.scale(k.base, 0.7) : p.fur(Pelt.mix(k.base, k.pale, 0.35), null, 0.0, px,
                        py));
        p.paint(new Pelt.Box("thigh", 32, 28, 3, 6, 4), (f, x, y, z, px, py) -> leg(p, k, f, px, py, true));
        p.paint(new Pelt.Box("shank", 46, 28, 2, 4, 2), (f, x, y, z, px, py) -> leg(p, k, f, px, py, false));
        p.paint(new Pelt.Box("hind foot", 0, 40, 2, 2, 2), (f, x, y, z, px, py) -> leg(p, k, f, px, py, false));
        p.save(folder, k.name);
    }

    // Fur with a spot here and there; `dark` shades it toward the spots' colour (the line along the spine).
    static int[] spotted(Pelt p, Coat k, int px, int py, double dark) {
        int[] c = p.fur(Pelt.mix(k.base, k.spot, dark), k.pale, 0.05, px, py);
        return p.noise(px, py, 5) < k.spots ? Pelt.mix(c, k.spot, 0.85) : c;
    }

    // A leg: spotted outside, cream on the inner side (the animal's left on the unmirrored right leg).
    static int[] leg(Pelt p, Coat k, Pelt.Face f, int px, int py, boolean spots) {
        if (f == Pelt.Face.LEFT) {
            return p.fur(k.pale, null, 0.0, px, py);
        }
        return spots ? spotted(p, k, px, py, 0.0) : p.fur(Pelt.mix(k.base, k.pale, 0.2), null, 0.0, px, py);
    }

    static int[] head(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        int col = (int) (x * 5);
        int row = (int) (y * 5);
        if (f == Pelt.Face.FRONT) {
            if (row == 0) {
                // Dark lines running up the brow from each eye.
                return col == 1 || col == 3 ? Pelt.mix(k.base, k.spot, 0.8) : p.fur(k.base, null, 0.0, px, py);
            }
            if (row == 1) {
                // Behind the eyes, which are parts of their own: the lids, seen as they shut.
                return col == 1 || col == 3 ? Pelt.mix(k.base, k.spot, 0.5) : p.fur(k.base, null, 0.0, px, py);
            }
            return row == 2 && col == 2 ? p.fur(k.base, null, 0.0, px, py) : k.pale;
        }
        if (f == Pelt.Face.BOTTOM) {
            return k.pale;
        }
        if (f == Pelt.Face.TOP) {
            return col == 1 || col == 3 ? Pelt.mix(k.base, k.spot, 0.6) : p.fur(k.base, null, 0.0, px, py);
        }
        if (f == Pelt.Face.BACK) {
            return spotted(p, k, px, py, 0.0);
        }
        // The cheek: tawny above, white below, a dark bar across it.
        int back = (int) (z * 4);
        if (row == 3 && back >= 1 && back <= 2) {
            return Pelt.mix(k.base, k.spot, 0.8);
        }
        return row >= 3 ? k.pale : p.fur(k.base, null, 0.0, px, py);
    }
}
