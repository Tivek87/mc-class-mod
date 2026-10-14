import java.io.IOException;
import java.nio.file.Path;

// Paints the brown bear's texture (textures/entity/brown_bear.png) from scratch, box by box as BearModel lays it out:
// a deep brown coat with grizzled light tips over the hump, shoulders and back, darker legs and belly with shaggy
// fringes hanging under the belly, throat, cheeks and elbows, a lighter tawny muzzle with a black nose, small dark eyes
// under a darker brow, round dark ears, near-black paws with grey soles and long ivory claws, and a pink mouth with
// white teeth that shows when it opens.
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
    static final int[] EYE_SHINE = Pelt.rgb(38, 26, 20);
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
        Pelt p = new Pelt(128, 128, 1871L);
        // The coat: darker toward the belly, grizzled on top, most of all over the hump; long shaggy hair hanging
        // under the belly, the throat, the cheeks and behind the elbows.
        p.paint(new Pelt.Box("chest", 0, 0, 12, 10, 10), (f, x, y, z, px, py) -> body(p, f, y, 0.12, px, py));
        p.paint(new Pelt.Box("hump", 0, 36, 9, 3, 7), (f, x, y, z, px, py) -> body(p, f, y * 0.3, 0.22, px, py));
        p.paint(new Pelt.Box("hump top", 24, 52, 7, 1, 5), (f, x, y, z, px, py) -> body(p, f, 0.0, 0.26, px, py));
        p.paint(new Pelt.Box("belly", 28, 22, 10, 2, 9), (f, x, y, z, px, py) -> shag(p, f, y, Pelt.mix(DARK, BASE,
                0.25), px, py));
        p.paint(new Pelt.Box("shoulder", 112, 36, 1, 7, 6), (f, x, y, z, px, py) -> body(p, f, y, 0.14, px, py));
        p.paint(new Pelt.Box("rump", 44, 0, 11, 9, 13), (f, x, y, z, px, py) -> body(p, f, y, 0.1 * (1.0 - z * 0.5),
                px, py));
        p.paint(new Pelt.Box("rump top", 66, 22, 9, 1, 10), (f, x, y, z, px, py) -> body(p, f, 0.0, 0.16, px, py));
        p.paint(new Pelt.Box("rump belly", 32, 36, 9, 2, 8), (f, x, y, z, px, py) -> shag(p, f, y, Pelt.mix(DARK,
                BASE, 0.2), px, py));
        p.paint(new Pelt.Box("tail", 96, 59, 3, 3, 2), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK,
                0.3 + 0.4 * y), GRIZZLE, 0.1, px, py));
        p.paint(new Pelt.Box("neck", 0, 22, 8, 8, 6), (f, x, y, z, px, py) -> body(p, f, y, 0.12, px, py));
        p.paint(new Pelt.Box("throat", 0, 52, 7, 2, 5), (f, x, y, z, px, py) -> shag(p, f, y, Pelt.mix(BASE, DARK,
                0.5), px, py));
        p.paint(new Pelt.Box("skull", 92, 0, 8, 7, 6), (f, x, y, z, px, py) -> skull(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("brow", 48, 52, 6, 1, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(HEAD, GRIZZLE,
                f == Pelt.Face.TOP ? 0.2 : 0.0), GRIZZLE, 0.25, px, py));
        p.paint(new Pelt.Box("cheek", 0, 59, 1, 4, 4), (f, x, y, z, px, py) -> shag(p, f, y * 0.8, Pelt.mix(HEAD,
                GRIZZLE, 0.15), px, py));
        p.paint(new Pelt.Box("muzzle", 68, 52, 5, 3, 4), (f, x, y, z, px, py) -> muzzle(p, f, y, z, px, py));
        p.paint(new Pelt.Box("bridge", 72, 59, 3, 1, 3), (f, x, y, z, px, py) -> p.fur(Pelt.mix(MUZZLE, HEAD,
                0.3 + 0.5 * z), null, 0.0, px, py));
        p.paint(new Pelt.Box("nose", 0, 67, 3, 2, 1), (f, x, y, z, px, py) -> nose(f, x, y));
        p.paint(new Pelt.Box("eye", 8, 67, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT ? EYE_SHINE : EYE);
        p.paint(new Pelt.Box("ear", 116, 59, 3, 3, 1), (f, x, y, z, px, py) -> ear(p, f, x, y, px, py));
        p.paint(new Pelt.Box("jaw", 42, 59, 4, 1, 4), (f, x, y, z, px, py) -> jaw(p, f, x, z, px, py));
        p.paint(new Pelt.Box("chin", 84, 59, 3, 1, 3), (f, x, y, z, px, py) -> p.fur(Pelt.mix(MUZZLE, DARK, 0.35),
                null, 0.0, px, py));
        // Legs: dark brown, near black at the paws.
        p.paint(new Pelt.Box("front leg", 92, 36, 5, 9, 5), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK,
                0.35 + 0.5 * y), GRIZZLE, 0.18 * (1.0 - y), px, py));
        p.paint(new Pelt.Box("elbow tuft", 106, 59, 4, 3, 1), (f, x, y, z, px, py) -> shag(p, f, y, Pelt.mix(BASE,
                DARK, 0.6), px, py));
        p.paint(new Pelt.Box("forearm", 10, 59, 4, 4, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(DARK, PAW, y),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("paw", 86, 52, 5, 2, 5), (f, x, y, z, px, py) -> paw(p, f, x, z, px, py));
        p.paint(new Pelt.Box("claws", 58, 59, 5, 1, 2), (f, x, y, z, px, py) -> claw(f, x, z, px));
        p.paint(new Pelt.Box("thigh", 66, 36, 6, 9, 7), (f, x, y, z, px, py) -> p.fur(Pelt.mix(BASE, DARK,
                0.25 + 0.55 * y), GRIZZLE, 0.15 * (1.0 - y), px, py));
        p.paint(new Pelt.Box("shin", 26, 59, 4, 4, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(DARK, PAW, y),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("heel", 12, 67, 3, 2, 1), (f, x, y, z, px, py) -> p.fur(PAW, null, 0.0, px, py));
        p.paint(new Pelt.Box("foot", 106, 52, 5, 1, 6), (f, x, y, z, px, py) -> foot(p, f, x, z, px, py));
        p.paint(new Pelt.Box("toes", 20, 67, 5, 1, 1), (f, x, y, z, px, py) -> ((int) (x * 5)) % 2 == 0
                ? Pelt.mix(CLAW_ROOT, CLAW, 0.4) : PAW);
        p.save(folder, "brown_bear");
    }

    // Long hair hanging below a part: its sides end in a ragged fringe, some strands longer than others.
    static int[] shag(Pelt p, Pelt.Face f, double y, int[] base, int px, int py) {
        if (f != Pelt.Face.TOP && f != Pelt.Face.BOTTOM && y > 0.55 && p.noise(px, 0, 7) < 0.4) {
            return null;
        }
        int[] c = p.fur(base, GRIZZLE, f == Pelt.Face.TOP ? 0.0 : 0.12, px, py);
        return Pelt.scale(c, f == Pelt.Face.BOTTOM ? 0.8 : 1.0 - 0.12 * y);
    }

    // A round ear: its upper corners left open, the dark inside facing forward.
    static int[] ear(Pelt p, Pelt.Face f, double x, double y, int px, int py) {
        if ((f == Pelt.Face.FRONT || f == Pelt.Face.BACK) && y < 0.34 && (x < 0.34 || x > 0.66)) {
            return null;
        }
        if (f == Pelt.Face.FRONT) {
            return Pelt.mix(INNER_EAR, DARK, Math.abs(x - 0.5) < 0.2 && y > 0.4 ? 0.3 : 0.1);
        }
        return p.fur(Pelt.mix(BASE, DARK, 0.45), GRIZZLE, 0.1, px, py);
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
            // The lids behind the eyes (each eye a part of its own, shut by drawing it in), under a darker brow.
            if (row == 2 && (col == 1 || col == 6)) {
                return Pelt.mix(c, DARK, 0.3);
            }
            if (row == 1 && (col == 1 || col == 6)) {
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
