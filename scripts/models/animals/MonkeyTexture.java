import java.io.IOException;
import java.nio.file.Path;

// Paints the monkeys' textures (textures/entity/monkey_white_faced.png and monkey_brown.png) from scratch, box by box
// as MonkeyModel lays it out. The white-faced capuchin: black, with cream over its shoulders, chest, upper arms and
// round its face, a black cap coming to a point on its brow, and a bare pink face. The brown capuchin: brown with a
// paler chest, its cap, limbs and tail much darker, and a tan face. Both have dark eyes set close together under
// darker lids, and bare palms and soles.
// Run from the repository root: java scripts/models/animals/MonkeyTexture.java [assets folder]
public final class MonkeyTexture {
    record Coat(String name, int[] body, int[] front, int[] limb, int[] cap, int[] cheek, int[] skin,
            boolean shoulders) {
    }

    static final int[] EYE = Pelt.rgb(40, 24, 16);
    static final int[] MOUTH = Pelt.rgb(120, 50, 50);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("monkey_white_faced", Pelt.rgb(34, 31, 30), Pelt.rgb(234, 216, 172),
                Pelt.rgb(30, 28, 27), Pelt.rgb(26, 24, 23), Pelt.rgb(238, 222, 184), Pelt.rgb(222, 172, 150),
                true));
        paint(folder, new Coat("monkey_brown", Pelt.rgb(128, 94, 60), Pelt.rgb(170, 132, 88), Pelt.rgb(70, 50, 34),
                Pelt.rgb(46, 34, 26), Pelt.rgb(150, 112, 74), Pelt.rgb(204, 166, 138), false));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(64, 64, k.name.hashCode());
        p.paint(new Pelt.Box("chest", 0, 0, 5, 5, 5), (f, x, y, z, px, py) -> {
            boolean pale = f == Pelt.Face.FRONT || f == Pelt.Face.BOTTOM
                    || k.shoulders && f != Pelt.Face.BACK && (z < 0.6 || f != Pelt.Face.TOP && y > 0.5);
            return p.fur(pale ? k.front : k.body, null, 0.0, px, py);
        });
        p.paint(new Pelt.Box("loins", 20, 0, 4, 4, 3), (f, x, y, z, px, py) -> p.fur(f == Pelt.Face.BOTTOM
                ? Pelt.mix(k.body, k.front, 0.35) : k.body, null, 0.0, px, py));
        p.paint(new Pelt.Box("rump", 34, 0, 5, 5, 3), (f, x, y, z, px, py) -> p.fur(k.body, null, 0.0, px, py));
        p.paint(new Pelt.Box("head", 0, 10, 5, 5, 4), (f, x, y, z, px, py) -> head(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 18, 10, 3, 2, 1), (f, x, y, z, px, py) -> muzzle(k, f, x, y));
        p.paint(new Pelt.Box("chin", 26, 10, 3, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH
                : f == Pelt.Face.BOTTOM ? p.fur(k.cheek, null, 0.0, px, py) : Pelt.scale(k.skin, 0.9));
        // The cap: darker fur standing up in a tuft.
        p.paint(new Pelt.Box("cap", 36, 10, 3, 1, 3), (f, x, y, z, px, py) -> p.fur(Pelt.scale(k.cap, f
                == Pelt.Face.TOP ? 1.15 : 1.0), k.body, 0.1, px, py));
        // The ear: bare and darker in its hollow, furred round its rim.
        p.paint(new Pelt.Box("ear", 48, 10, 1, 2, 1), (f, x, y, z, px, py) -> f == Pelt.Face.RIGHT
                || f == Pelt.Face.LEFT ? Pelt.mix(k.skin, k.cap, 0.45) : p.fur(k.cheek, null, 0.0, px, py));
        // A dark brown eye, the lid's line over it.
        p.paint(new Pelt.Box("eye", 54, 10, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT ? EYE
                : f == Pelt.Face.TOP ? Pelt.scale(k.skin, 0.55) : Pelt.scale(EYE, 0.6));
        p.paint(new Pelt.Box("arm", 0, 20, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(k.shoulders && y < 0.6 ? k.front
                : k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("forearm", 8, 20, 2, 3, 2), (f, x, y, z, px, py) -> p.fur(k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("hand", 16, 20, 2, 1, 2), (f, x, y, z, px, py) -> paw(p, k, f, x, px, py));
        p.paint(new Pelt.Box("thigh", 24, 20, 2, 5, 3), (f, x, y, z, px, py) -> p.fur(y < 0.3 ? k.body : k.limb,
                null, 0.0, px, py));
        p.paint(new Pelt.Box("shin", 34, 20, 2, 3, 2), (f, x, y, z, px, py) -> p.fur(k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("foot", 42, 20, 2, 1, 4), (f, x, y, z, px, py) -> paw(p, k, f, x, px, py));
        p.paint(new Pelt.Box("tail", 54, 20, 1, 1, 3), (f, x, y, z, px, py) -> Pelt.scale(k.limb, 0.92
                + 0.16 * p.noise(px, py, 3)));
        p.save(folder, k.name);
    }

    // Hands and feet: dark fur on top, bare palms and soles below, the fingers or toes parted on their front.
    static int[] paw(Pelt p, Coat k, Pelt.Face f, double x, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return Pelt.scale(k.skin, 0.55);
        }
        if (f == Pelt.Face.FRONT) {
            return Pelt.scale(k.skin, (int) (x * 4) % 2 == 0 ? 0.5 : 0.62);
        }
        return Pelt.scale(k.limb, 0.85 + 0.1 * p.noise(px, py, 4));
    }

    // The muzzle: a small dark nose with its nostrils on top, the upper lip below, the mouth's line under it.
    static int[] muzzle(Coat k, Pelt.Face f, double x, double y) {
        if (f == Pelt.Face.BOTTOM) {
            return MOUTH;
        }
        if (f == Pelt.Face.FRONT) {
            int col = (int) (x * 3);
            if (y < 0.5) {
                return col == 1 ? Pelt.scale(k.skin, 0.45) : Pelt.scale(k.skin, 0.88);
            }
            return Pelt.scale(k.skin, 0.95);
        }
        return Pelt.scale(k.skin, f == Pelt.Face.TOP ? 0.8 : 0.9);
    }

    // The face, five wide and five tall: the cap's point on the brow, the eyes set apart behind their lids, a bare
    // nose bridge between them, and pale fur framing it all; the muzzle covers the middle of the lowest two rows.
    static int[] head(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        int col = (int) (x * 5);
        int row = (int) (y * 5);
        int[] cap = p.fur(k.cap, null, 0.0, px, py);
        int[] cheek = p.fur(k.cheek, null, 0.0, px, py);
        if (f == Pelt.Face.TOP) {
            return z < 0.3 && (col == 0 || col == 4) ? cheek : cap;
        }
        if (f == Pelt.Face.BOTTOM) {
            return cheek;
        }
        if (f == Pelt.Face.FRONT) {
            if (row == 0) {
                return col == 2 ? cap : col == 1 || col == 3 ? Pelt.mix(cap, cheek, 0.5) : cheek;
            }
            if (col == 0 || col == 4) {
                return cheek;
            }
            if (row == 1) {
                // Behind the eyes, which are parts of their own: the lids, seen as they shut.
                return col == 2 ? Pelt.scale(k.skin, 0.85) : Pelt.scale(k.skin, 0.6);
            }
            return row == 2 ? Pelt.scale(k.skin, col == 2 ? 0.9 : 1.0) : k.skin;
        }
        if (f == Pelt.Face.BACK) {
            return row < 2 ? cap : p.fur(k.shoulders ? k.cheek : k.body, null, 0.0, px, py);
        }
        // The side: the cap above and behind, the pale fur below it toward the face.
        return row == 0 || z > 0.6 && row < 3 ? cap : cheek;
    }
}
