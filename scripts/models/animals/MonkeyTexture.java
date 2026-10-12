import java.io.IOException;
import java.nio.file.Path;

// Paints the monkeys' textures (textures/entity/monkey_white_faced.png and monkey_brown.png) from scratch, box by box
// as MonkeyModel lays it out. The white-faced capuchin: black, with cream over its shoulders, chest, upper arms and
// round its face, a black cap coming to a point on its brow, and a bare pink face. The brown capuchin: brown with a
// paler chest, its cap, limbs and tail much darker, and a tan face. Both have dark eyes set close together.
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
        Pelt p = new Pelt(64, 32, k.name.hashCode());
        p.paint(new Pelt.Box("body", 0, 0, 4, 4, 7), (f, x, y, z, px, py) -> {
            boolean chest = f == Pelt.Face.FRONT || f == Pelt.Face.BOTTOM && z < 0.6
                    || k.shoulders && f != Pelt.Face.BACK && z < 0.3;
            return p.fur(chest ? k.front : k.body, null, 0.0, px, py);
        });
        p.paint(new Pelt.Box("head", 22, 0, 4, 4, 4), (f, x, y, z, px, py) -> head(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 38, 0, 2, 1, 1), (f, x, y, z, px, py) -> Pelt.scale(k.skin, 0.86));
        p.paint(new Pelt.Box("ear", 44, 0, 1, 2, 1), (f, x, y, z, px, py) -> Pelt.scale(k.skin, f == Pelt.Face.FRONT
                ? 0.8 : 0.92));
        p.paint(new Pelt.Box("jaw", 48, 0, 2, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH
                : Pelt.scale(k.skin, 0.92));
        p.paint(new Pelt.Box("arm", 0, 11, 2, 5, 2), (f, x, y, z, px, py) -> p.fur(k.shoulders && y < 0.5 ? k.front
                : k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("forearm", 8, 11, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("hand", 16, 11, 2, 1, 2), (f, x, y, z, px, py) -> Pelt.scale(k.limb, 0.8));
        p.paint(new Pelt.Box("thigh", 24, 11, 2, 5, 2), (f, x, y, z, px, py) -> p.fur(y < 0.3 ? k.body : k.limb, null,
                0.0, px, py));
        p.paint(new Pelt.Box("shin", 32, 11, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(k.limb, null, 0.0, px, py));
        p.paint(new Pelt.Box("tail", 40, 11, 1, 1, 4), (f, x, y, z, px, py) -> Pelt.scale(k.limb, 0.92
                + 0.16 * p.noise(px, py, 3)));
        p.save(folder, k.name);
    }

    // The face, four wide and four tall: the cap's point on the brow, the eyes close together, bare skin under them
    // and pale fur down each cheek.
    static int[] head(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        int col = (int) (x * 4);
        int row = (int) (y * 4);
        int[] cap = p.fur(k.cap, null, 0.0, px, py);
        int[] cheek = p.fur(k.cheek, null, 0.0, px, py);
        if (f == Pelt.Face.TOP) {
            return cap;
        }
        if (f == Pelt.Face.BOTTOM) {
            return cheek;
        }
        if (f == Pelt.Face.FRONT) {
            boolean middle = col == 1 || col == 2;
            if (row == 0) {
                return middle ? cap : cheek;
            }
            if (!middle) {
                return cheek;
            }
            return row == 1 ? EYE : k.skin;
        }
        if (f == Pelt.Face.BACK) {
            return row < 2 ? cap : p.fur(k.shoulders ? k.cheek : k.body, null, 0.0, px, py);
        }
        // The side: the cap above and behind, the cheek's pale fur below it toward the face.
        return row == 0 || z > 0.6 && row < 3 ? cap : cheek;
    }
}
