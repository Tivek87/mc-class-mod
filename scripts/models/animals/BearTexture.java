import java.io.IOException;
import java.nio.file.Path;

// Paints the brown bear's texture (textures/entity/brown_bear.png) from scratch, box by box as BearModel lays it out:
// a deep brown coat with grizzled light tips over the hump, shoulders and back, darker legs and belly, a lighter
// tawny muzzle with a black nose, small dark eyes, round dark ears, near-black paws with grey soles and long ivory
// claws, and a pink mouth with white teeth that shows when it opens.
// Run from the repository root: java scripts/models/animals/BearTexture.java [assets folder]
public final class BearTexture {
    static final int[] DARK = Pelt.rgb(52, 34, 21);
    static final int[] BASE = Pelt.rgb(98, 64, 38);
    static final int[] HEAD = Pelt.rgb(112, 77, 48);
    static final int[] GRIZZLE = Pelt.rgb(172, 132, 86);
    static final int[] MUZZLE = Pelt.rgb(134, 101, 68);
    static final int[] NOSE = Pelt.rgb(40, 35, 33);
    static final int[] NOSTRIL = Pelt.rgb(12, 10, 10);
    static final int[] EYE = Pelt.rgb(14, 10, 8);
    static final int[] INNER_EAR = Pelt.rgb(76, 54, 38);
    static final int[] PAW = Pelt.rgb(38, 26, 18);
    static final int[] SOLE = Pelt.rgb(46, 42, 40);
    static final int[] CLAW = Pelt.rgb(216, 206, 182);
    static final int[] CLAW_ROOT = Pelt.rgb(124, 110, 90);
    static final int[] MOUTH = Pelt.rgb(116, 50, 52);
    static final int[] TONGUE = Pelt.rgb(176, 92, 96);
    static final int[] TEETH = Pelt.rgb(234, 228, 212);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        Pelt p = new Pelt(128, 64, 1871L);
        // The coat: darker toward the belly, grizzled on top, most of all over the hump.
        p.paint(new Pelt.Box("chest", 0, 0, 12, 10, 11), (f, x, y, z, px, py) -> body(p, f, y, 0.3, px, py));
        p.paint(new Pelt.Box("hump", 46, 0, 9, 3, 7), (f, x, y, z, px, py) -> body(p, f, y * 0.3, 0.5, px, py));
        p.paint(new Pelt.Box("rump", 0, 21, 11, 9, 13), (f, x, y, z, px, py) -> body(p, f, y, 0.22 * (1.0 - z * 0.5),
                px, py));
        p.paint(new Pelt.Box("tail", 78, 0, 3, 3, 2), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK, 0.3 + 0.4 * y),
                GRIZZLE, 0.1, px, py));
        p.paint(new Pelt.Box("neck", 48, 21, 8, 8, 6), (f, x, y, z, px, py) -> body(p, f, y, 0.3, px, py));
        p.paint(new Pelt.Box("skull", 76, 21, 8, 7, 6), (f, x, y, z, px, py) -> skull(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 88, 0, 5, 3, 4), (f, x, y, z, px, py) -> muzzle(p, f, y, z, px, py));
        p.paint(new Pelt.Box("nose", 106, 0, 3, 2, 1), (f, x, y, z, px, py) -> nose(f, x, y));
        p.paint(new Pelt.Box("ear", 114, 0, 3, 2, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                ? Pelt.mix(INNER_EAR, DARK, Math.abs(x - 0.5) < 0.3 && y > 0.4 ? 0.6 : 0.0)
                : p.fur(Pelt.mix(BASE, DARK, 0.45), null, 0.0, px, py));
        p.paint(new Pelt.Box("jaw", 88, 7, 4, 1, 4), (f, x, y, z, px, py) -> jaw(p, f, x, z, px, py));
        // Legs: dark brown, near black at the paws.
        p.paint(new Pelt.Box("front leg", 0, 43, 5, 8, 5), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK,
                0.35 + 0.5 * y), GRIZZLE, 0.18 * (1.0 - y), px, py));
        p.paint(new Pelt.Box("forearm", 20, 43, 4, 4, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(DARK, PAW, y),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("paw", 36, 43, 5, 2, 5), (f, x, y, z, px, py) -> paw(p, f, x, z, px, py));
        p.paint(new Pelt.Box("claws", 56, 43, 5, 1, 2), (f, x, y, z, px, py) -> claw(f, x, z, px));
        p.paint(new Pelt.Box("thigh", 70, 43, 5, 7, 6), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK,
                0.25 + 0.55 * y), GRIZZLE, 0.15 * (1.0 - y), px, py));
        p.paint(new Pelt.Box("shin", 92, 43, 4, 4, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(DARK, PAW, y),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("foot", 36, 50, 5, 1, 6), (f, x, y, z, px, py) -> foot(p, f, x, z, px, py));
        p.save(folder, "brown_bear");
    }

    static int[] body(Pelt p, Pelt.Face f, double y, double grizzle, int px, int py) {
        double down = f == Pelt.Face.BOTTOM ? 1.0 : f == Pelt.Face.TOP ? 0.0 : y;
        int[] base = Pelt.mix(BASE, DARK, Pelt.step(0.35, 1.0, down) * 0.85);
        int[] c = p.fur(base, GRIZZLE, grizzle * (1.0 - down), px, py);
        return Pelt.scale(c, 1.05 - 0.1 * down);
    }

    static int[] skull(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        int[] c = p.fur(Pelt.mix(HEAD, BASE, y * 0.6), GRIZZLE, 0.22 * (1.0 - y), px, py);
        if (f == Pelt.Face.FRONT) {
            int col = (int) (x * 8);
            int row = (int) (y * 7);
            // Small dark eyes set wide above the muzzle, under a darker brow.
            if (row == 3 && (col == 1 || col == 6)) {
                return EYE;
            }
            if (row == 2 && (col == 1 || col == 6)) {
                return Pelt.mix(c, DARK, 0.45);
            }
            // The dished brow between the eyes runs into the muzzle a little lighter.
            if (row >= 3 && col >= 2 && col <= 5) {
                return Pelt.mix(c, MUZZLE, 0.5);
            }
        }
        return c;
    }

    static int[] muzzle(Pelt p, Pelt.Face f, double y, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            // The roof of the mouth, teeth along its front.
            return z < 0.3 ? TEETH : MOUTH;
        }
        int[] c = p.fur(MUZZLE, null, 0.0, px, py);
        if (f == Pelt.Face.FRONT && y > 0.66) {
            return Pelt.mix(c, DARK, 0.55);
        }
        // Lighter toward the nose, a little darker back toward the eyes.
        return Pelt.mix(c, HEAD, z * 0.4);
    }

    static int[] nose(Pelt.Face f, double x, double y) {
        if (f == Pelt.Face.FRONT && y > 0.5 && Math.abs(x - 0.5) > 0.2) {
            return NOSTRIL;
        }
        return f == Pelt.Face.TOP ? Pelt.mix(NOSE, Pelt.rgb(96, 90, 88), 0.4) : NOSE;
    }

    static int[] jaw(Pelt p, Pelt.Face f, double x, double z, int px, int py) {
        if (f == Pelt.Face.TOP) {
            // The tongue, with the lower teeth along the front and the long canines at its corners.
            if (z < 0.3) {
                return TEETH;
            }
            if (z < 0.55 && (x < 0.25 || x > 0.75)) {
                return TEETH;
            }
            return TONGUE;
        }
        if (f == Pelt.Face.FRONT) {
            return Pelt.mix(MUZZLE, DARK, 0.5);
        }
        return p.fur(Pelt.mix(MUZZLE, BASE, 0.4), null, 0.0, px, py);
    }

    static int[] paw(Pelt p, Pelt.Face f, double x, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            // The bare sole with its toe pads.
            return z < 0.25 && ((int) (x * 5)) % 2 == 0 ? Pelt.scale(SOLE, 0.8) : SOLE;
        }
        return p.fur(PAW, null, 0.0, px, py);
    }

    static int[] claw(Pelt.Face f, double x, double z, int px) {
        int[] c = Pelt.mix(CLAW_ROOT, CLAW, 1.0 - z);
        // Five claws side by side, a dark gap's shading between each.
        return ((int) (x * 5)) % 2 == 1 ? Pelt.scale(c, 0.86) : c;
    }

    static int[] foot(Pelt p, Pelt.Face f, double x, double z, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return z < 0.2 && ((int) (x * 5)) % 2 == 0 ? Pelt.scale(SOLE, 0.8) : SOLE;
        }
        if (f == Pelt.Face.FRONT) {
            // Short claws at the toes.
            return ((int) (x * 5)) % 2 == 0 ? Pelt.mix(CLAW_ROOT, CLAW, 0.4) : PAW;
        }
        return p.fur(PAW, null, 0.0, px, py);
    }
}
