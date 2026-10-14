import java.io.IOException;
import java.nio.file.Path;

// Paints the crab's texture (textures/entity/crab.png) and its meat (textures/item/raw_crab.png and cooked_crab.png)
// from scratch. The crab, box by box as CrabModel lays it out: a red-brown shell mottled darker and lighter, a paler
// scalloped rim round its edge, a raised middle with its H-shaped groove, dark-tipped spines at its front corners,
// cream underneath and across its face over brownish mouthparts; black eyes on pale stalks; brick-red claws,
// paler below, their fingers tipped black; and orange-brown legs darkening to their points. The meat is a claw: raw,
// dull grey-brown with its black tip; cooked, bright orange-red.
// Run from the repository root: java scripts/models/animals/CrabTexture.java [assets folder]
public final class CrabTexture {
    static final int[] SHELL = Pelt.rgb(152, 62, 38);
    static final int[] SHELL_DARK = Pelt.rgb(108, 42, 28);
    static final int[] SHELL_LIGHT = Pelt.rgb(190, 96, 54);
    static final int[] RIM = Pelt.rgb(206, 132, 86);
    static final int[] CREAM = Pelt.rgb(230, 210, 170);
    static final int[] MOUTH = Pelt.rgb(150, 110, 84);
    static final int[] CLAW = Pelt.rgb(174, 70, 40);
    static final int[] LEG = Pelt.rgb(176, 96, 54);
    static final int[] POINT = Pelt.rgb(88, 42, 28);
    static final int[] BLACK = Pelt.rgb(26, 22, 22);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        Pelt p = new Pelt(64, 32, 7321L);
        p.paint(new Pelt.Box("shell", 0, 0, 9, 3, 6), (f, x, y, z, px, py) -> shell(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("underside", 30, 0, 6, 1, 5), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM
                && ((int) (z * 5)) % 2 == 1 ? Pelt.scale(CREAM, 0.88) : CREAM);
        p.paint(new Pelt.Box("eye", 52, 0, 1, 2, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP || y < 0.5 ? BLACK
                : Pelt.mix(CREAM, SHELL_LIGHT, 0.4));
        p.paint(new Pelt.Box("dome", 0, 14, 7, 1, 4), (f, x, y, z, px, py) -> dome(p, f, x, z, px, py));
        // The spine's point lies at its front.
        p.paint(new Pelt.Box("spine", 56, 0, 1, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT || z < 0.5
                ? POINT : Pelt.mix(RIM, SHELL, 0.4));
        p.paint(new Pelt.Box("mouth", 52, 3, 3, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                && (int) (x * 3) == 1 ? Pelt.scale(MOUTH, 0.7) : Pelt.mix(MOUTH, CREAM, 0.3));
        p.paint(new Pelt.Box("arm", 0, 9, 2, 2, 3), (f, x, y, z, px, py) -> claw(p, f, y, px, py));
        p.paint(new Pelt.Box("wrist", 34, 11, 2, 2, 2), (f, x, y, z, px, py) -> claw(p, f, y, px, py));
        p.paint(new Pelt.Box("hand", 22, 14, 3, 3, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                && p.noise(px, py, 4) < 0.25 ? Pelt.scale(CLAW, 0.8) : claw(p, f, y, px, py));
        p.paint(new Pelt.Box("thumb", 34, 15, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT || z < 0.5
                ? BLACK : Pelt.mix(CLAW, CREAM, 0.35));
        p.paint(new Pelt.Box("finger", 42, 15, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT || z < 0.5
                ? BLACK : p.fur(CLAW, null, 0.0, px, py));
        p.paint(new Pelt.Box("leg", 34, 9, 3, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM
                ? Pelt.mix(LEG, CREAM, 0.4) : p.fur(LEG, null, 0.0, px, py));
        p.paint(new Pelt.Box("lower leg", 42, 9, 3, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM
                ? Pelt.mix(LEG, CREAM, 0.3) : Pelt.scale(p.fur(LEG, null, 0.0, px, py), 0.9));
        // The tip's point lies at the box's outer end, the animal's right on the unmirrored piece.
        p.paint(new Pelt.Box("tip", 50, 9, 2, 1, 1), (f, x, y, z, px, py) -> Pelt.mix(POINT, LEG,
                Pelt.step(0.1, 0.9, f == Pelt.Face.RIGHT ? 0.0 : f == Pelt.Face.LEFT ? 1.0 : x)));
        p.save(folder, "crab");
        Path items = Pelt.textures(args, "item");
        meat(items, "raw_crab", Pelt.rgb(120, 104, 92), Pelt.rgb(88, 74, 66), Pelt.rgb(190, 176, 150),
                Pelt.rgb(44, 36, 32));
        meat(items, "cooked_crab", Pelt.rgb(222, 92, 42), Pelt.rgb(176, 58, 28), Pelt.rgb(246, 214, 170),
                Pelt.rgb(84, 28, 16));
    }

    static int[] shell(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return CREAM;
        }
        if (f == Pelt.Face.FRONT) {
            // Its face: shell above, cream below, the mouth dark in the middle.
            if (y < 0.34) {
                return p.fur(SHELL, null, 0.0, px, py);
            }
            return Math.abs(x - 0.5) < 0.17 ? MOUTH : y > 0.67 ? CREAM : Pelt.mix(SHELL_LIGHT, CREAM, 0.5);
        }
        if (f != Pelt.Face.TOP) {
            return y > 0.67 ? Pelt.mix(SHELL_LIGHT, CREAM, 0.45) : p.fur(SHELL_LIGHT, null, 0.0, px, py);
        }
        int col = (int) (x * 9);
        int row = (int) (z * 6);
        boolean rim = col == 0 || col == 8 || row == 0 || row == 5;
        if (rim) {
            // Scalloped, every other pixel paler.
            return (col + row) % 2 == 0 ? RIM : Pelt.mix(RIM, SHELL, 0.5);
        }
        double mottle = p.noise(px, py, 5);
        int[] c = mottle < 0.22 ? SHELL_DARK : mottle > 0.82 ? SHELL_LIGHT : SHELL;
        // Two darker dimples in the middle of the back.
        if (row == 2 && (col == 3 || col == 5)) {
            c = SHELL_DARK;
        }
        return Pelt.scale(c, 0.95 + 0.1 * p.noise(px, py, 6));
    }

    // The raised middle of the back: mottled like the shell, its edges paler where it rises.
    static int[] dome(Pelt p, Pelt.Face f, double x, double z, int px, int py) {
        if (f != Pelt.Face.TOP) {
            return Pelt.mix(SHELL, RIM, 0.25);
        }
        double mottle = p.noise(px, py, 5);
        int[] c = mottle < 0.25 ? SHELL_DARK : mottle > 0.8 ? SHELL_LIGHT : SHELL;
        // The H-shaped groove of a crab's back.
        int col = (int) (x * 7);
        int row = (int) (z * 4);
        if ((col == 2 || col == 4) && row != 0 || row == 2 && col == 3) {
            c = SHELL_DARK;
        }
        return Pelt.scale(c, 0.95 + 0.1 * p.noise(px, py, 6));
    }

    static int[] claw(Pelt p, Pelt.Face f, double y, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return Pelt.mix(CLAW, CREAM, 0.55);
        }
        int[] c = f == Pelt.Face.TOP ? CLAW : Pelt.mix(CLAW, CREAM, 0.4 * Pelt.step(0.5, 1.0, y));
        return Pelt.scale(c, 0.94 + 0.12 * p.noise(px, py, 5));
    }

    // A claw lying from the bottom left to the top right: the arm, the swollen hand, and two fingers parted at
    // their tips.
    static void meat(Path folder, String name, int[] shell, int[] shade, int[] pale, int[] outline)
            throws IOException {
        Pelt p = new Pelt(16, 16, name.hashCode());
        String[] rows = {
            "................",
            "..........tt....",
            ".........tsst...",
            "........sssstt..",
            ".......ssss.st..",
            "......sssss.....",
            ".....ssssssstt..",
            "....sssssssssst.",
            "....ssssssssstt.",
            "...ppsssssss....",
            "...pppsssss.....",
            "..ppppsss.......",
            "..pppps.........",
            "..ppp...........",
            "................",
            "................",
        };
        boolean[][] in = new boolean[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                in[y][x] = rows[y].charAt(x) != '.';
            }
        }
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                char at = rows[y].charAt(x);
                int[] colour = null;
                if (at == 't') {
                    colour = BLACK;
                } else if (at == 'p') {
                    colour = Pelt.scale(pale, 1.05 - 0.02 * (x + y));
                } else if (at == 's') {
                    // Lit from the top left, shaded along its lower edge.
                    boolean low = y + 1 < 16 && !in[y + 1][x] || x + 1 < 16 && !in[y][x + 1] && y > 6;
                    colour = Pelt.scale(low ? shade : shell, 1.1 - 0.012 * (x + y) + 0.08 * p.noise(x, y, 2));
                } else if (MeatTexture.near(in, x, y)) {
                    colour = outline;
                }
                if (colour != null) {
                    p.image.setRGB(x, y, Pelt.argb(colour));
                }
            }
        }
        p.save(folder, name);
    }
}
