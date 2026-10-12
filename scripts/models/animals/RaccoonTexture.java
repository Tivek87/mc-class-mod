import java.io.IOException;
import java.nio.file.Path;

// Paints the raccoon's texture (textures/entity/raccoon.png) from scratch, box by box as RaccoonModel lays it out: a
// grizzled grey coat of dark guard hairs and pale tips, browner along the back and buff underneath; the black mask
// across its eyes under white brows, a dark stripe down its forehead to its snout, a white muzzle and fluffy white
// cheeks, round grey ears rimmed in white; dark legs and near-black hand-like paws; and a bushy tail ringed in black
// and buff to its black tip.
// Run from the repository root: java scripts/models/animals/RaccoonTexture.java [assets folder]
public final class RaccoonTexture {
    static final int[] GREY = Pelt.rgb(108, 102, 94);
    static final int[] DARK = Pelt.rgb(66, 62, 58);
    static final int[] TIP = Pelt.rgb(168, 162, 152);
    static final int[] BACK = Pelt.rgb(96, 86, 74);
    static final int[] BELLY = Pelt.rgb(146, 136, 120);
    static final int[] LEG = Pelt.rgb(76, 70, 64);
    static final int[] PAW = Pelt.rgb(42, 38, 36);
    static final int[] MASK = Pelt.rgb(24, 22, 22);
    static final int[] EYE = Pelt.rgb(62, 44, 30);
    static final int[] WHITE = Pelt.rgb(224, 222, 216);
    static final int[] NOSE = Pelt.rgb(18, 16, 16);
    static final int[] RING_DARK = Pelt.rgb(34, 31, 29);
    static final int[] RING_LIGHT = Pelt.rgb(166, 156, 138);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        Pelt p = new Pelt(64, 64, 9133L);
        p.paint(new Pelt.Box("front torso", 0, 0, 6, 5, 5), (f, x, y, z, px, py) -> body(p, f, y, px, py));
        p.paint(new Pelt.Box("hind", 22, 0, 7, 6, 6), (f, x, y, z, px, py) -> body(p, f, y, px, py));
        p.paint(new Pelt.Box("skull", 0, 12, 5, 4, 4), (f, x, y, z, px, py) -> skull(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("cheeks", 18, 12, 7, 2, 2), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? p.fur(GREY, TIP, 0.3, px, py) : p.fur(WHITE, null, 0.0, px, py));
        p.paint(new Pelt.Box("snout", 36, 12, 2, 2, 2), (f, x, y, z, px, py) -> snout(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("ear", 44, 12, 2, 2, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? (y < 0.5 ? WHITE : DARK) : y < 0.5 ? WHITE : p.fur(GREY, null, 0.0, px, py));
        // The tail's rings, counted from its root to its black tip across its three pieces.
        p.paint(new Pelt.Box("tail", 0, 22, 3, 3, 3), (f, x, y, z, px, py) -> ring(p, f, 0 + (int) (z * 3), px,
                py));
        p.paint(new Pelt.Box("tail middle", 12, 22, 3, 3, 3), (f, x, y, z, px, py) -> ring(p, f, 3 + (int) (z * 3),
                px, py));
        p.paint(new Pelt.Box("tail tip", 24, 22, 3, 3, 2), (f, x, y, z, px, py) -> ring(p, f, 6 + (int) (z * 2), px,
                py));
        p.paint(new Pelt.Box("front leg", 0, 30, 2, 4, 2), (f, x, y, z, px, py) -> p.fur(Pelt.mix(GREY, LEG,
                0.4 + 0.6 * y), null, 0.0, px, py));
        p.paint(new Pelt.Box("front paw", 8, 30, 2, 2, 2), (f, x, y, z, px, py) -> paw(p, f, x, px, py));
        p.paint(new Pelt.Box("hind leg", 16, 30, 3, 5, 3), (f, x, y, z, px, py) -> p.fur(Pelt.mix(GREY, LEG,
                0.25 + 0.6 * y), TIP, 0.15 * (1.0 - y), px, py));
        p.paint(new Pelt.Box("hind paw", 28, 30, 2, 2, 3), (f, x, y, z, px, py) -> paw(p, f, x, px, py));
        p.save(folder, "raccoon");
    }

    // Grizzled: dark guard hairs and pale tips through grey, browner on top, buff below.
    static int[] body(Pelt p, Pelt.Face f, double y, int px, int py) {
        double down = f == Pelt.Face.BOTTOM ? 1.0 : f == Pelt.Face.TOP ? 0.0 : y;
        int[] base = Pelt.mix(Pelt.mix(BACK, GREY, Pelt.step(0.0, 0.4, down)), BELLY, Pelt.step(0.65, 1.0, down));
        int[] c = p.fur(base, TIP, 0.2 * (1.0 - down * 0.6), px, py);
        if (p.noise(px, py, 6) < 0.24 * (1.0 - down)) {
            c = Pelt.mix(c, DARK, 0.6);
        }
        return c;
    }

    static int[] skull(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        int[] c = p.fur(GREY, TIP, 0.25, px, py);
        switch (f) {
            case FRONT -> {
                int col = (int) (x * 5);
                int row = (int) (y * 4);
                if (col == 2 && row <= 1) {
                    return Pelt.mix(DARK, MASK, 0.4);
                }
                return switch (row) {
                    // White brows over the mask, its dark stripe between them.
                    case 0 -> WHITE;
                    case 1 -> col == 1 || col == 3 ? EYE : MASK;
                    case 2 -> col == 0 || col == 4 ? MASK : WHITE;
                    default -> WHITE;
                };
            }
            case RIGHT, LEFT -> {
                int row = (int) (y * 4);
                // The mask runs back from the eyes, over white cheeks below it.
                if (row == 1 && z < 0.6) {
                    return MASK;
                }
                if (row == 0 && z < 0.4) {
                    return WHITE;
                }
                if (row >= 2 && z < 0.5) {
                    return WHITE;
                }
                return c;
            }
            case TOP -> {
                return Math.abs(x - 0.5) < 0.12 && z < 0.6 ? Pelt.mix(DARK, MASK, 0.4) : c;
            }
            case BOTTOM -> {
                return WHITE;
            }
            default -> {
                return c;
            }
        }
    }

    static int[] snout(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        return switch (f) {
            case FRONT -> y < 0.5 ? NOSE : Pelt.mix(DARK, MASK, 0.5);
            case TOP -> z < 0.5 ? NOSE : Pelt.mix(DARK, MASK, 0.4);
            default -> z < 0.3 && y < 0.5 ? NOSE : WHITE;
        };
    }

    static int[] ring(Pelt p, Pelt.Face f, int index, int px, int py) {
        if (index >= 7) {
            return p.fur(RING_DARK, null, 0.0, px, py);
        }
        int[] c = index % 2 == 1 ? RING_DARK : RING_LIGHT;
        if (f == Pelt.Face.BOTTOM) {
            // The rings fade out underneath.
            c = Pelt.mix(c, RING_LIGHT, 0.5);
        }
        return p.fur(c, index % 2 == 0 ? TIP : null, 0.25, px, py);
    }

    static int[] paw(Pelt p, Pelt.Face f, double x, int px, int py) {
        if (f == Pelt.Face.FRONT) {
            // Long dark fingers.
            return ((int) (x * 4)) % 2 == 0 ? PAW : Pelt.scale(PAW, 1.4);
        }
        return f == Pelt.Face.BOTTOM ? Pelt.scale(PAW, 0.8) : p.fur(PAW, null, 0.0, px, py);
    }
}
