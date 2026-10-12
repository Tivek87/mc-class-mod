import java.io.IOException;
import java.nio.file.Path;

// Paints the owls' textures (textures/entity/owl_tawny.png, owl_snowy.png and owl_owlet.png) from scratch, box by box
// as OwlModel lays it out. The tawny owl: mottled rufous brown above with dark streaks and pale spots, buff below
// with dark streaks down the breast, a pale flat face ringed dark, black eyes, a pale beak and barred wings and tail.
// The snowy owl: white, barred sparsely with dark brown on its back, wings and breast, its face clean white, eyes
// yellow, beak black. The owlet: soft grey down, barred faintly, with a darker face.
// Run from the repository root: java scripts/models/animals/OwlTexture.java [assets folder]
public final class OwlTexture {
    record Plumage(String name, int[] back, int[] streak, int[] spot, double streaks, double spots, int[] breast,
            int[] bar, int[] face, int[] rim, int[] eye, int[] pupil, int[] beak, int[] lid, int[] foot) {
    }

    static final int[] TALON = Pelt.rgb(34, 30, 28);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Plumage("owl_tawny", Pelt.rgb(116, 80, 50), Pelt.rgb(58, 40, 26), Pelt.rgb(214, 188, 146),
                0.22, 0.12, Pelt.rgb(216, 192, 152), Pelt.rgb(82, 56, 36), Pelt.rgb(204, 188, 162),
                Pelt.rgb(84, 58, 38), Pelt.rgb(22, 17, 14), Pelt.rgb(22, 17, 14), Pelt.rgb(198, 190, 128),
                Pelt.rgb(170, 152, 126), Pelt.rgb(206, 184, 148)));
        paint(folder, new Plumage("owl_snowy", Pelt.rgb(238, 238, 232), Pelt.rgb(74, 64, 56),
                Pelt.rgb(255, 255, 255), 0.16, 0.0, Pelt.rgb(242, 242, 238), Pelt.rgb(92, 80, 70),
                Pelt.rgb(246, 246, 242), Pelt.rgb(226, 226, 220), Pelt.rgb(234, 196, 40), Pelt.rgb(16, 14, 12),
                Pelt.rgb(34, 30, 28), Pelt.rgb(214, 214, 208), Pelt.rgb(236, 236, 230)));
        paint(folder, new Plumage("owl_owlet", Pelt.rgb(188, 184, 176), Pelt.rgb(146, 142, 134),
                Pelt.rgb(220, 218, 212), 0.2, 0.1, Pelt.rgb(206, 203, 196), Pelt.rgb(160, 156, 148),
                Pelt.rgb(124, 114, 104), Pelt.rgb(98, 90, 82), Pelt.rgb(22, 17, 14), Pelt.rgb(22, 17, 14),
                Pelt.rgb(180, 176, 150), Pelt.rgb(112, 104, 96), Pelt.rgb(196, 192, 184)));
    }

    static void paint(Path folder, Plumage k) throws IOException {
        Pelt p = new Pelt(64, 32, k.name.hashCode());
        p.paint(new Pelt.Box("body", 0, 0, 5, 7, 4), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                || f == Pelt.Face.BOTTOM ? breast(p, k, px, py) : back(p, k, px, py));
        p.paint(new Pelt.Box("head", 18, 0, 7, 5, 5), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? face(p, k, (int) (x * 7), (int) (y * 5), px, py) : f == Pelt.Face.BOTTOM ? breast(p, k, px, py)
                        : back(p, k, px, py));
        p.paint(new Pelt.Box("beak", 42, 0, 1, 2, 1), (f, x, y, z, px, py) -> Pelt.scale(k.beak, y < 0.5 ? 1.0
                : 0.82));
        p.paint(new Pelt.Box("lid", 46, 0, 2, 2, 1), (f, x, y, z, px, py) -> Pelt.scale(k.lid, y < 0.5 ? 1.0
                : 0.86));
        p.paint(new Pelt.Box("tail", 52, 0, 3, 3, 1), (f, x, y, z, px, py) -> barred(p, k, (int) (y * 3), px, py));
        p.paint(new Pelt.Box("wing", 0, 12, 5, 5, 1), (f, x, y, z, px, py) -> wing(p, k, f, x, y, px, py, false));
        p.paint(new Pelt.Box("outer wing", 12, 12, 5, 5, 1), (f, x, y, z, px, py) -> wing(p, k, f, x, y, px, py,
                true));
        p.paint(new Pelt.Box("leg", 24, 12, 1, 2, 1), (f, x, y, z, px, py) -> p.fur(k.foot, null, 0.0, px, py));
        p.paint(new Pelt.Box("foot", 28, 12, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                || f == Pelt.Face.TOP && z < 0.5 ? TALON : k.foot);
        p.save(folder, k.name);
    }

    // Mottled: dark streaks and pale spots through the ground colour.
    static int[] back(Pelt p, Plumage k, int px, int py) {
        int[] c = p.fur(k.back, null, 0.0, px, py);
        double pick = p.noise(px, py / 2, 9);
        if (pick < k.streaks) {
            return Pelt.mix(c, k.streak, 0.75);
        }
        return p.noise(px, py, 10) < k.spots ? Pelt.mix(c, k.spot, 0.8) : c;
    }

    // Pale, with dark streaks running down it.
    static int[] breast(Pelt p, Plumage k, int px, int py) {
        int[] c = p.fur(k.breast, null, 0.0, px, py);
        return p.noise(px, py / 3, 11) < k.streaks * 1.1 ? Pelt.mix(c, k.bar, 0.7) : c;
    }

    // Bars across a feather: every other row dark.
    static int[] barred(Pelt p, Plumage k, int row, int px, int py) {
        int[] c = p.fur(k.back, null, 0.0, px, py);
        return row % 2 == 1 ? Pelt.mix(c, k.streak, k.spots > 0.0 ? 0.6 : 0.5 * p.noise(px, py, 12) + 0.25) : c;
    }

    static int[] wing(Pelt p, Plumage k, Pelt.Face f, double x, double y, int px, int py, boolean outer) {
        if (f != Pelt.Face.FRONT && f != Pelt.Face.BACK) {
            return p.fur(Pelt.mix(k.back, k.streak, 0.35), null, 0.0, px, py);
        }
        int row = (int) (y * 5);
        // The coverts near the shoulder mottled like the back, the flight feathers below them barred.
        if (row == 0 && !outer) {
            return back(p, k, px, py);
        }
        int[] c = barred(p, k, row, px, py);
        // A line of pale spots across the shoulder.
        return row == 1 && k.spots > 0.0 && ((int) (x * 5)) % 2 == 0 ? Pelt.mix(c, k.spot, 0.8) : c;
    }

    // The flat face, seven wide and five tall: a pale disc inside a dark rim, an eye two wide each side of the beak.
    static int[] face(Pelt p, Plumage k, int col, int row, int px, int py) {
        boolean edge = col == 0 || col == 6;
        if (row == 0) {
            return col == 3 ? k.rim : edge ? back(p, k, px, py) : Pelt.mix(k.rim, k.face, 0.35);
        }
        if (row == 4) {
            return col >= 2 && col <= 4 ? Pelt.mix(k.face, k.rim, 0.25) : k.rim;
        }
        if (edge) {
            return k.rim;
        }
        if ((row == 1 || row == 2) && col != 3) {
            // The pupil sits low and toward the beak.
            boolean inner = col == 2 || col == 4;
            return row == 2 && inner ? k.pupil : k.eye;
        }
        if (col == 3 && row == 1) {
            return Pelt.mix(k.rim, k.face, 0.3);
        }
        return p.fur(k.face, null, 0.0, px, py);
    }
}
