import java.io.IOException;
import java.nio.file.Path;

// Paints the crocodile's textures (textures/entity/crocodile.png and crocodile_hatchling.png) from scratch, box by box
// as CrocodileModel lays it out. Grown: dull olive above in rows of square scales, crossed by darker bands that
// grow stronger down the tail, paling down its sides to a cream belly; raised scutes down its back and tail lit on
// top; yellow eyes with a dark slit; teeth showing along both jaws, upper and lower fitting between each other, and
// a pale pink mouth inside. The hatchling: yellower, its dark bands bold from neck to tail tip.
// Run from the repository root: java scripts/models/animals/CrocodileTexture.java [assets folder]
public final class CrocodileTexture {
    record Hide(String name, int[] olive, int[] dark, int[] belly, double bands) {
    }

    static final int[] TOOTH = Pelt.rgb(238, 234, 214);
    static final int[] MOUTH = Pelt.rgb(222, 170, 150);
    static final int[] EYE = Pelt.rgb(204, 188, 62);
    static final int[] BLACK = Pelt.rgb(20, 20, 18);
    static final int[] CLAW = Pelt.rgb(40, 38, 32);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Hide("crocodile", Pelt.rgb(92, 104, 64), Pelt.rgb(50, 58, 38), Pelt.rgb(208, 198, 148),
                0.45));
        paint(folder, new Hide("crocodile_hatchling", Pelt.rgb(150, 150, 76), Pelt.rgb(40, 44, 28),
                Pelt.rgb(226, 216, 160), 0.9));
    }

    static void paint(Path folder, Hide k) throws IOException {
        Pelt p = new Pelt(128, 64, k.name.hashCode());
        // Pixels along the animal from the neck, so the bands run on from the body down the tail.
        p.paint(new Pelt.Box("chest", 0, 0, 10, 5, 8), (f, x, y, z, px, py) -> hide(p, k, f, x, y, z * 8.0, px, py));
        p.paint(new Pelt.Box("hips", 36, 0, 9, 5, 8), (f, x, y, z, px, py) -> hide(p, k, f, x, y, 8.0 + z * 8.0, px,
                py));
        p.paint(new Pelt.Box("scutes", 108, 8, 1, 1, 7), (f, x, y, z, px, py) -> scute(k, f, (int) (z * 7)));
        // A yellow-green eye with its lid dark above it.
        p.paint(new Pelt.Box("eyeball", 52, 24, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? Pelt.scale(k.dark, 1.15) : EYE);
        // A row of teeth, a pixel of gum between each.
        p.paint(new Pelt.Box("teeth", 46, 31, 1, 1, 7), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                || f == Pelt.Face.BACK || ((int) (z * 7)) % 2 == 0 ? Pelt.rgb(232, 224, 196) : null);
        p.paint(new Pelt.Box("tail 1", 82, 0, 7, 4, 6), (f, x, y, z, px, py) -> hide(p, k, f, x, y, 16.0 + z * 6.0, px,
                py));
        p.paint(new Pelt.Box("crest", 108, 0, 1, 1, 6), (f, x, y, z, px, py) -> scute(k, f, (int) (z * 6)));
        p.paint(new Pelt.Box("tail 2", 70, 21, 5, 3, 6), (f, x, y, z, px, py) -> hide(p, k, f, x, y, 22.0 + z * 6.0,
                px, py));
        p.paint(new Pelt.Box("tail 3", 92, 21, 3, 2, 6), (f, x, y, z, px, py) -> hide(p, k, f, x, y, 28.0 + z * 6.0,
                px, py));
        p.paint(new Pelt.Box("tail 4", 110, 21, 1, 2, 6), (f, x, y, z, px, py) -> hide(p, k, f, 0.5, y * 0.5,
                34.0 + z * 6.0, px, py));
        p.paint(new Pelt.Box("skull", 0, 21, 8, 4, 5), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM ? k.belly
                : hide(p, k, f, x, y * 0.8, -5.0 + z * 5.0, px, py));
        p.paint(new Pelt.Box("snout", 26, 21, 5, 2, 8), (f, x, y, z, px, py) -> snout(p, k, f, y, z, px, py));
        p.paint(new Pelt.Box("eye bump", 52, 21, 2, 1, 2), (f, x, y, z, px, py) -> Pelt.scale(k.dark, 1.15));
        p.paint(new Pelt.Box("nostrils", 60, 21, 3, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP && z < 0.5
                && Math.abs(x - 0.5) > 0.2 ? BLACK : Pelt.scale(k.olive, 1.08));
        p.paint(new Pelt.Box("jaw", 0, 31, 7, 1, 4), (f, x, y, z, px, py) -> jaw(k, f, x, (int) (z * 4), 7));
        p.paint(new Pelt.Box("jaw front", 22, 31, 4, 1, 8), (f, x, y, z, px, py) -> jaw(k, f, x, (int) (z * 8), 4));
        p.paint(new Pelt.Box("leg", 0, 42, 3, 2, 2), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM ? k.belly
                : p.fur(k.olive, null, 0.0, px, py));
        p.paint(new Pelt.Box("shin", 10, 42, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.olive, k.dark,
                0.3 * y), null, 0.0, px, py));
        p.paint(new Pelt.Box("foot", 18, 42, 3, 1, 3), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                && ((int) (x * 3)) % 2 == 0 ? CLAW : f == Pelt.Face.BOTTOM ? k.belly : Pelt.mix(k.olive, k.dark,
                        0.35));
        p.save(folder, k.name);
    }

    // The armoured hide at `along` pixels from the neck: square scales, a dark band every so often, paler down the
    // side and cream underneath.
    static int[] hide(Pelt p, Hide k, Pelt.Face f, double x, double y, double along, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            // Rows of broad belly scales.
            return Pelt.scale(k.belly, ((int) along) % 2 == 0 ? 1.0 : 0.92);
        }
        double down = f == Pelt.Face.TOP ? 0.0 : y;
        // Bands closer and stronger toward the tail tip, ragged at their edges.
        double period = 6.0;
        double in = (along + 40.0 + 1.2 * (p.noise(px, 0, 4) - 0.5)) % period;
        double band = in < 2.0 ? k.bands * (0.6 + 0.4 * Pelt.step(8.0, 30.0, along)) : 0.0;
        int[] c = Pelt.mix(k.olive, k.dark, band);
        // A scale's edge every other pixel, and the odd scale darker or lighter.
        c = Pelt.scale(c, 0.9 + 0.2 * p.noise(px / 2, py / 2, 2) - (px + py) % 2 * 0.04);
        return Pelt.mix(c, k.belly, 0.75 * Pelt.step(0.45, 1.05, down));
    }

    // A ridge of scutes: each a bump lit on top, dark between.
    static int[] scute(Hide k, Pelt.Face f, int along) {
        boolean bump = along % 2 == 0;
        if (f == Pelt.Face.TOP) {
            return bump ? Pelt.scale(k.olive, 1.3) : k.dark;
        }
        return bump ? Pelt.scale(k.olive, 0.95) : k.dark;
    }

    static int[] snout(Pelt p, Hide k, Pelt.Face f, double y, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return MOUTH;
        }
        boolean side = f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT;
        // The upper teeth, every other pixel along the lip.
        if (side && y > 0.5) {
            return ((int) (z * 8)) % 2 == 0 ? TOOTH : Pelt.mix(k.olive, k.belly, 0.5);
        }
        if (f == Pelt.Face.FRONT && y > 0.5) {
            return ((int) (px)) % 2 == 0 ? TOOTH : Pelt.mix(k.olive, k.belly, 0.5);
        }
        return Pelt.scale(k.olive, 0.94 + 0.14 * p.noise(px / 2, py, 2));
    }

    // The lower jaw: cream below, the mouth's pink inside on top with a tooth at its edge between each two of the
    // upper jaw's.
    static int[] jaw(Hide k, Pelt.Face f, double x, int along, int wide) {
        if (f == Pelt.Face.BOTTOM) {
            return k.belly;
        }
        if (f == Pelt.Face.TOP) {
            int col = (int) (x * wide);
            return (col == 0 || col == wide - 1) && along % 2 == 1 ? TOOTH : MOUTH;
        }
        return along % 2 == 1 || f == Pelt.Face.FRONT ? TOOTH : Pelt.mix(k.belly, k.olive, 0.3);
    }
}
