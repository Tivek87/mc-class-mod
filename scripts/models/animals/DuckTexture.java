import java.io.IOException;
import java.nio.file.Path;

// Paints the ducks' textures (textures/entity/duck_drake.png, duck_hen.png and duckling.png) and their meat
// (textures/item/raw_duck.png and cooked_duck.png) from scratch, box by box as DuckModel lays it out. The drake: a
// glossy bottle-green head, a white collar, a chestnut breast, pale grey flanks under a grey-brown back, a black rump
// and a white tail, a yellow bill and orange feet. The hen: buff scalloped with dark brown all over, a dark crown
// and a dark line through the eye, an orange bill with a dark saddle. Both carry the blue wing patch edged white.
// The duckling: yellow down, brown over its crown, back and rump, a dark line through the eye. The meat is a drumstick
// on its bone.
// Run from the repository root: java scripts/models/animals/DuckTexture.java [assets folder]
public final class DuckTexture {
    static final int[] EYE = Pelt.rgb(18, 16, 16);
    static final int[] WHITE = Pelt.rgb(236, 236, 230);
    static final int[] BLUE = Pelt.rgb(46, 84, 172);
    static final int[] ORANGE = Pelt.rgb(226, 122, 36);
    static final int[] GREEN = Pelt.rgb(22, 88, 54);
    static final int[] CHESTNUT = Pelt.rgb(112, 60, 42);
    static final int[] FLANK = Pelt.rgb(192, 192, 186);
    static final int[] BACK = Pelt.rgb(122, 110, 98);
    static final int[] BLACK = Pelt.rgb(30, 30, 32);
    static final int[] BUFF = Pelt.rgb(180, 144, 100);
    static final int[] SCALLOP = Pelt.rgb(94, 66, 42);
    static final int[] DOWN = Pelt.rgb(234, 206, 96);
    static final int[] DOWN_DARK = Pelt.rgb(106, 84, 50);

    enum Kind { DRAKE, HEN, DUCKLING }

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, "duck_drake", Kind.DRAKE);
        paint(folder, "duck_hen", Kind.HEN);
        paint(folder, "duckling", Kind.DUCKLING);
        Path items = Pelt.textures(args, "item");
        meat(items, "raw_duck", Pelt.rgb(226, 150, 140), Pelt.rgb(188, 108, 104), Pelt.rgb(240, 226, 204),
                Pelt.rgb(74, 34, 34));
        meat(items, "cooked_duck", Pelt.rgb(176, 104, 48), Pelt.rgb(132, 70, 30), Pelt.rgb(232, 214, 184),
                Pelt.rgb(60, 32, 14));
    }

    static void paint(Path folder, String name, Kind k) throws IOException {
        Pelt p = new Pelt(64, 32, name.hashCode());
        p.paint(new Pelt.Box("body", 0, 0, 5, 4, 7), (f, x, y, z, px, py) -> body(p, k, f, y, z, px, py));
        p.paint(new Pelt.Box("tail", 24, 0, 3, 2, 2), (f, x, y, z, px, py) -> tail(p, k, f, x, px, py));
        p.paint(new Pelt.Box("neck", 34, 0, 2, 4, 2), (f, x, y, z, px, py) -> neck(p, k, f, y, px, py));
        p.paint(new Pelt.Box("head", 42, 0, 3, 3, 3), (f, x, y, z, px, py) -> head(p, k, f, y, z, px, py));
        p.paint(new Pelt.Box("bill", 54, 0, 2, 1, 2), (f, x, y, z, px, py) -> bill(k, f, z));
        p.paint(new Pelt.Box("lower bill", 54, 3, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? Pelt.rgb(150, 72, 66) : Pelt.scale(bill(k, Pelt.Face.BOTTOM, z), 0.88));
        // The breast swells out of the body's front, coloured as the chest.
        p.paint(new Pelt.Box("breast", 30, 12, 4, 3, 1), (f, x, y, z, px, py) -> body(p, k, f == Pelt.Face.TOP
                ? Pelt.Face.TOP : Pelt.Face.FRONT, y, 0.0, px, py));
        p.paint(new Pelt.Box("eye", 40, 12, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? head(p, k, Pelt.Face.TOP, 0.0, 0.5, px, py) : EYE);
        p.paint(new Pelt.Box("wing", 0, 12, 4, 3, 1), (f, x, y, z, px, py) -> wing(p, k, f, x, y, px, py, false));
        p.paint(new Pelt.Box("outer wing", 10, 12, 3, 3, 1), (f, x, y, z, px, py) -> wing(p, k, f, x, y, px, py,
                true));
        int[] leg = k == Kind.DUCKLING ? Pelt.rgb(124, 98, 60) : ORANGE;
        p.paint(new Pelt.Box("leg", 18, 12, 1, 2, 1), (f, x, y, z, px, py) -> leg);
        p.paint(new Pelt.Box("foot", 22, 12, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                && ((int) (x * 2) + (int) (z * 2)) % 2 == 1 ? Pelt.scale(leg, 0.82) : leg);
        p.save(folder, name);
    }

    // The hen's coat: buff, each feather's dark centre a scallop.
    static int[] mottled(Pelt p, int px, int py, double dark) {
        int[] c = p.fur(BUFF, null, 0.0, px, py);
        return p.noise(px, py, 5) < dark ? Pelt.mix(c, SCALLOP, 0.8) : c;
    }

    static int[] body(Pelt p, Kind k, Pelt.Face f, double y, double z, int px, int py) {
        boolean top = f == Pelt.Face.TOP;
        boolean under = f == Pelt.Face.BOTTOM;
        double back = f == Pelt.Face.FRONT ? 0.0 : f == Pelt.Face.BACK ? 1.0 : z;
        boolean high = top || !under && y < 0.3;
        if (k == Kind.HEN) {
            return under ? p.fur(Pelt.mix(BUFF, WHITE, 0.3), null, 0.0, px, py) : mottled(p, px, py,
                    high ? 0.5 : 0.36);
        }
        if (k == Kind.DUCKLING) {
            return high && back > 0.12 || back > 0.85 && !under ? p.fur(DOWN_DARK, DOWN, 0.08, px, py)
                    : p.fur(DOWN, null, 0.0, px, py);
        }
        if (back < 0.26) {
            return p.fur(CHESTNUT, null, 0.0, px, py);
        }
        if (back > 0.8) {
            return BLACK;
        }
        if (high) {
            return p.fur(BACK, null, 0.0, px, py);
        }
        // Fine dark lines across the pale flank.
        return Pelt.scale(FLANK, 0.94 + 0.1 * p.noise(px, py / 2, 6));
    }

    static int[] tail(Pelt p, Kind k, Pelt.Face f, double x, int px, int py) {
        if (k == Kind.DUCKLING) {
            return p.fur(DOWN_DARK, null, 0.0, px, py);
        }
        if (k == Kind.HEN) {
            return Math.abs(x - 0.5) > 0.3 ? Pelt.mix(BUFF, WHITE, 0.35) : mottled(p, px, py, 0.4);
        }
        // White, with the black curled feathers down its middle.
        return f == Pelt.Face.TOP && Math.abs(x - 0.5) < 0.2 ? BLACK : WHITE;
    }

    static int[] neck(Pelt p, Kind k, Pelt.Face f, double y, int px, int py) {
        if (k == Kind.HEN) {
            return mottled(p, px, py, 0.22);
        }
        if (k == Kind.DUCKLING) {
            return f == Pelt.Face.BACK ? p.fur(DOWN_DARK, null, 0.0, px, py) : p.fur(DOWN, null, 0.0, px, py);
        }
        return y < 0.5 ? gloss(p, px, py, 0.0) : y < 0.75 ? WHITE : p.fur(CHESTNUT, null, 0.0, px, py);
    }

    // The drake's green, with its sheen.
    static int[] gloss(Pelt p, int px, int py, double light) {
        return Pelt.scale(Pelt.mix(GREEN, Pelt.rgb(60, 150, 96), light), 0.9 + 0.2 * p.noise(px, py, 7));
    }

    static int[] head(Pelt p, Kind k, Pelt.Face f, double y, double z, int px, int py) {
        boolean side = f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT;
        int row = (int) (y * 3);
        int col = (int) (z * 3);
        if (side && row == 1 && col == 1) {
            return EYE;
        }
        boolean crown = f == Pelt.Face.TOP || f != Pelt.Face.BOTTOM && row == 0;
        // A dark line from the bill through the eye.
        boolean stripe = side && row == 1;
        if (k == Kind.HEN) {
            return crown || stripe ? p.fur(SCALLOP, BUFF, 0.15, px, py) : p.fur(Pelt.mix(BUFF, WHITE, 0.15), null,
                    0.0, px, py);
        }
        if (k == Kind.DUCKLING) {
            return crown || stripe && col == 2 ? p.fur(DOWN_DARK, null, 0.0, px, py) : p.fur(DOWN, null, 0.0, px,
                    py);
        }
        return gloss(p, px, py, f == Pelt.Face.TOP ? 0.45 : 0.0);
    }

    static int[] bill(Kind k, Pelt.Face f, double z) {
        int[] c = k == Kind.DRAKE ? Pelt.rgb(216, 186, 54) : k == Kind.HEN ? Pelt.rgb(208, 128, 52)
                : Pelt.rgb(96, 76, 64);
        if (k == Kind.HEN && f == Pelt.Face.TOP) {
            return Pelt.rgb(70, 52, 40);
        }
        // The dark nail at its tip.
        return f == Pelt.Face.FRONT || f == Pelt.Face.TOP && z < 0.5 ? Pelt.scale(c, 0.78) : c;
    }

    // Both broad faces alike: grey-brown coverts over the flight feathers, and on the inner wing the blue patch
    // with its white edge, nearest the body. x runs from the wing's tip (0) to its shoulder (1).
    static int[] wing(Pelt p, Kind k, Pelt.Face f, double x, double y, int px, int py, boolean outer) {
        if (k == Kind.DUCKLING) {
            return p.fur(DOWN_DARK, DOWN, 0.1, px, py);
        }
        int[] covert = k == Kind.HEN ? mottled(p, px, py, 0.42) : p.fur(BACK, null, 0.0, px, py);
        int[] flight = Pelt.scale(k == Kind.HEN ? SCALLOP : Pelt.rgb(84, 76, 70), 0.92 + 0.14 * p.noise(px, py, 8));
        if (f != Pelt.Face.FRONT && f != Pelt.Face.BACK) {
            return flight;
        }
        int row = (int) (y * 3);
        if (outer) {
            return row == 0 ? covert : flight;
        }
        int col = (int) (x * 4);
        if (row == 2) {
            return col == 0 ? flight : col == 1 ? WHITE : BLUE;
        }
        return row == 1 && col == 0 ? flight : covert;
    }

    // A drumstick lying from the bottom left to the top right: the bone's knob, the bone, the meat.
    static void meat(Path folder, String name, int[] flesh, int[] shade, int[] bone, int[] outline)
            throws IOException {
        Pelt p = new Pelt(16, 16, name.hashCode());
        String[] rows = {
            "................",
            "..........mmm...",
            "........mmmmmm..",
            ".......mmmmmmmm.",
            "......mmmmmmmmm.",
            "......mmmmmmmmm.",
            "......mmmmmmmm..",
            ".....mmmmmmmm...",
            "....bmmmmmmm....",
            "...bbbmmmm......",
            "..bbb...........",
            ".bbbb...........",
            ".bb.b...........",
            "..b.............",
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
                if (at == 'b') {
                    colour = Pelt.scale(bone, 1.04 - 0.015 * (x + y));
                } else if (at == 'm') {
                    // Lit from the top left, shaded along its lower edge.
                    boolean low = !in[y + 1][x] || !in[y][x + 1] && y > 4;
                    colour = Pelt.scale(low ? shade : flesh, 1.12 - 0.012 * (x + y) + 0.08 * p.noise(x, y, 2));
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
