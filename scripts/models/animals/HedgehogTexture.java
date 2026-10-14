import java.io.IOException;
import java.nio.file.Path;

// Paints the hedgehog's texture (textures/entity/hedgehog.png) from scratch, box by box as HedgehogModel lays it out:
// a coat of spines, each dark at its root and banded cream toward its tip, so the coat is a dense speckle of dark and
// pale, paler low on the flanks; soft grey-brown fur on the face, chest and belly, a darker mask round the eyes and
// down the snout to a black nose, small round ears and dark feet.
// Run from the repository root: java scripts/models/animals/HedgehogTexture.java [assets folder]
public final class HedgehogTexture {
    static final int[] ROOT = Pelt.rgb(58, 44, 34);
    static final int[] BAND = Pelt.rgb(138, 112, 84);
    static final int[] TIP = Pelt.rgb(222, 204, 168);
    static final int[] FUR = Pelt.rgb(132, 110, 88);
    static final int[] BELLY = Pelt.rgb(176, 156, 128);
    static final int[] FACE = Pelt.rgb(164, 140, 110);
    static final int[] MASK = Pelt.rgb(88, 68, 52);
    static final int[] EYE = Pelt.rgb(16, 14, 14);
    static final int[] NOSE = Pelt.rgb(22, 18, 18);
    static final int[] EAR = Pelt.rgb(150, 118, 100);
    static final int[] FOOT = Pelt.rgb(78, 62, 52);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        Pelt p = new Pelt(64, 32, 4127L);
        p.paint(new Pelt.Box("body", 0, 0, 5, 4, 7), (f, x, y, z, px, py) -> body(p, f, y, z, px, py));
        p.paint(new Pelt.Box("spines", 24, 0, 6, 4, 7), (f, x, y, z, px, py) -> ragged(p, f, y, px, py));
        p.paint(new Pelt.Box("crest", 0, 11, 4, 1, 5), (f, x, y, z, px, py) -> spines(p, Pelt.Face.TOP, 0.0, px,
                py));
        p.paint(new Pelt.Box("brow", 46, 15, 4, 1, 1), (f, x, y, z, px, py) -> spines(p, Pelt.Face.TOP, 0.0, px,
                py));
        p.paint(new Pelt.Box("rump", 0, 17, 5, 3, 1), (f, x, y, z, px, py) -> ragged(p, f, y, px, py));
        p.paint(new Pelt.Box("eye", 56, 11, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? Pelt.scale(EYE, 2.6) : EYE);
        p.paint(new Pelt.Box("paw", 40, 15, 1, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? Pelt.mix(FOOT, TIP, 0.35) : f == Pelt.Face.BOTTOM ? Pelt.scale(FOOT, 0.8) : FOOT);
        p.paint(new Pelt.Box("skull", 18, 11, 4, 3, 3), (f, x, y, z, px, py) -> skull(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("snout", 32, 11, 2, 2, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? p.fur(Pelt.mix(MASK, FACE, z), null, 0.0, px, py) : p.fur(Pelt.mix(FACE, BELLY, y), null, 0.0, px,
                        py));
        p.paint(new Pelt.Box("nose", 40, 11, 1, 1, 1), (f, x, y, z, px, py) -> NOSE);
        p.paint(new Pelt.Box("ear", 44, 11, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? Pelt.scale(EAR, 0.8) : EAR);
        p.paint(new Pelt.Box("leg", 48, 11, 1, 1, 1), (f, x, y, z, px, py) -> Pelt.mix(FUR, FOOT, 0.4 + 0.4 * y));
        p.paint(new Pelt.Box("tail", 52, 11, 1, 1, 1), (f, x, y, z, px, py) -> FOOT);
        p.save(folder, "hedgehog");
    }

    // What shows of the trunk under the spines: its face end, and its belly.
    static int[] body(Pelt p, Pelt.Face f, double y, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return p.fur(BELLY, null, 0.0, px, py);
        }
        return p.fur(Pelt.mix(FUR, BELLY, Pelt.step(0.5, 1.0, y)), null, 0.0, px, py);
    }

    // Spines whose lowest row on the sides is ragged, single spines standing out past the others.
    static int[] ragged(Pelt p, Pelt.Face f, double y, int px, int py) {
        if (f != Pelt.Face.TOP && f != Pelt.Face.BOTTOM && y > 0.7 && p.noise(px, py, 9) < 0.4) {
            return null;
        }
        return spines(p, f, y, px, py);
    }

    static int[] spines(Pelt p, Pelt.Face f, double y, int px, int py) {
        double low = f == Pelt.Face.TOP ? 0.0 : y;
        double pick = p.noise(px, py, 7);
        // More pale tips show low on the flanks, where the spines lie outward.
        double tips = 0.4 + 0.2 * low;
        int[] c = pick < tips ? TIP : pick < tips + 0.25 ? BAND : ROOT;
        return Pelt.scale(c, 0.92 + 0.16 * p.noise(px, py, 8));
    }

    static int[] skull(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        switch (f) {
            case FRONT -> {
                int col = (int) (x * 4);
                int row = (int) (y * 3);
                // The lids behind the eyes, which are parts of their own.
                if (row == 1 && (col == 0 || col == 3)) {
                    return Pelt.scale(MASK, 0.8);
                }
                return row == 2 ? p.fur(FACE, null, 0.0, px, py) : p.fur(MASK, null, 0.0, px, py);
            }
            case RIGHT, LEFT -> {
                int row = (int) (y * 3);
                if (row == 1 && z < 0.34) {
                    return Pelt.scale(MASK, 0.8);
                }
                return p.fur(row == 2 ? FACE : Pelt.mix(MASK, FUR, z), null, 0.0, px, py);
            }
            case BOTTOM -> {
                return p.fur(BELLY, null, 0.0, px, py);
            }
            default -> {
                return p.fur(Pelt.mix(MASK, FUR, z), null, 0.0, px, py);
            }
        }
    }
}
