import java.io.IOException;
import java.nio.file.Path;

// Paints the squirrels' textures (textures/entity/squirrel_red.png and squirrel_grey.png) from scratch, box by box as
// SquirrelModel lays it out. The red squirrel: a russet coat, darker along the spine, a white chest and belly, dark
// tufts on its ears and a bushy tail of the same red. The grey: a grizzled grey coat tinged brown along the back and
// on the face and feet, white underneath, its tail grey frosted with white hair tips. Both have a large dark eye
// ringed pale.
// Run from the repository root: java scripts/models/animals/SquirrelTexture.java [assets folder]
public final class SquirrelTexture {
    record Coat(String name, int[] coat, int[] spine, int[] tip, int[] ear, int[] tail, int[] frost, double frosting,
            int[] foot) {
    }

    static final int[] WHITE = Pelt.rgb(240, 233, 220);
    static final int[] EYE = Pelt.rgb(18, 14, 12);
    static final int[] NOSE = Pelt.rgb(74, 46, 42);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("squirrel_red", Pelt.rgb(178, 86, 38), Pelt.rgb(136, 58, 26), Pelt.rgb(214, 128, 62),
                Pelt.rgb(84, 40, 22), Pelt.rgb(158, 70, 30), Pelt.rgb(206, 118, 54), 0.22, Pelt.rgb(190, 110, 62)));
        paint(folder, new Coat("squirrel_grey", Pelt.rgb(128, 124, 118), Pelt.rgb(118, 102, 84),
                Pelt.rgb(184, 180, 172), Pelt.rgb(120, 104, 88), Pelt.rgb(108, 104, 100), Pelt.rgb(232, 230, 224),
                0.42, Pelt.rgb(150, 132, 112)));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(64, 32, k.name.hashCode());
        p.paint(new Pelt.Box("body", 0, 0, 3, 3, 4), (f, x, y, z, px, py) -> body(p, k, f, x, y, px, py, true));
        p.paint(new Pelt.Box("hind", 14, 0, 4, 4, 3), (f, x, y, z, px, py) -> body(p, k, f, x, y, px, py, false));
        p.paint(new Pelt.Box("skull", 28, 0, 3, 3, 3), (f, x, y, z, px, py) -> skull(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("snout", 40, 0, 2, 2, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT && y < 0.5
                || f == Pelt.Face.TOP ? NOSE : WHITE);
        p.paint(new Pelt.Box("ear", 46, 0, 1, 2, 1), (f, x, y, z, px, py) -> y < 0.5 || f == Pelt.Face.TOP ? k.ear
                : p.fur(k.coat, null, 0.0, px, py));
        p.paint(new Pelt.Box("front leg", 50, 0, 1, 3, 1), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.coat, k.foot,
                Pelt.step(0.5, 1.0, y)), null, 0.0, px, py));
        p.paint(new Pelt.Box("thigh", 0, 8, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(k.coat, k.tip, 0.15, px, py));
        p.paint(new Pelt.Box("foot", 8, 8, 1, 1, 3), (f, x, y, z, px, py) -> k.foot);
        p.paint(new Pelt.Box("tail", 16, 8, 2, 2, 3), (f, x, y, z, px, py) -> tail(p, k, 0.3, px, py));
        p.paint(new Pelt.Box("tail middle", 26, 8, 3, 3, 4), (f, x, y, z, px, py) -> tail(p, k, edge(f, x, y), px,
                py));
        p.paint(new Pelt.Box("tail tip", 40, 8, 3, 3, 3), (f, x, y, z, px, py) -> tail(p, k, edge(f, x, y) + 0.15,
                px, py));
        p.save(folder, k.name);
    }

    // Coat above, darker down the spine, white below; the chest white from the front.
    static int[] body(Pelt p, Coat k, Pelt.Face f, double x, double y, int px, int py, boolean chest) {
        if (f == Pelt.Face.BOTTOM || chest && f == Pelt.Face.FRONT && y > 0.34) {
            return p.fur(WHITE, null, 0.0, px, py);
        }
        if (f == Pelt.Face.TOP) {
            return p.fur(Math.abs(x - 0.5) < 0.2 ? k.spine : k.coat, k.tip, 0.12, px, py);
        }
        if (f != Pelt.Face.FRONT && f != Pelt.Face.BACK && y > 0.7) {
            return p.fur(WHITE, null, 0.0, px, py);
        }
        return p.fur(k.coat, k.tip, 0.15, px, py);
    }

    static int[] skull(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        int row = (int) (y * 3);
        switch (f) {
            case FRONT -> {
                int col = (int) (x * 3);
                if (row == 1 && col != 1) {
                    return EYE;
                }
                return row == 2 ? WHITE : p.fur(k.coat, null, 0.0, px, py);
            }
            case RIGHT, LEFT -> {
                if (row == 1 && z < 0.34) {
                    return EYE;
                }
                // A pale ring behind the eye, white cheeks under it.
                if (row == 1 && z < 0.67) {
                    return Pelt.mix(k.coat, WHITE, 0.45);
                }
                return row == 2 && z < 0.67 ? WHITE : p.fur(k.coat, k.tip, 0.1, px, py);
            }
            case BOTTOM -> {
                return WHITE;
            }
            default -> {
                return p.fur(Math.abs(x - 0.5) < 0.2 && f == Pelt.Face.TOP ? k.spine : k.coat, null, 0.0, px, py);
            }
        }
    }

    // How near the outside of the brush a pixel lies, where the long hair tips show.
    static double edge(Pelt.Face f, double x, double y) {
        return f == Pelt.Face.TOP || f == Pelt.Face.BOTTOM ? 2.0 * Math.abs(x - 0.5) * 0.6
                : f == Pelt.Face.FRONT || f == Pelt.Face.BACK ? 0.6 : 2.0 * Math.abs(y - 0.5) * 0.6;
    }

    static int[] tail(Pelt p, Coat k, double edge, int px, int py) {
        int[] c = p.fur(k.tail, null, 0.0, px, py);
        if (p.noise(px, py, 5) < k.frosting * (0.4 + edge)) {
            c = Pelt.mix(c, k.frost, 0.75);
        }
        return c;
    }
}
