import java.io.IOException;
import java.nio.file.Path;

// Paints the deer's textures (textures/entity/deer.png and deer_fawn.png) from scratch, box by box as DeerModel lays
// them out: a white-tailed deer in its summer coat, red-brown with a darker line down the back, a white belly, throat
// patch, chin, eye rings and band behind the black nose, white under the tail and round the rump, greyer head and tan
// legs with black hooves and a dark gland on each hind leg, big ears pale inside with dark rims, and antlers dark at
// the base and pale at the tips. The fawn's coat is brighter, with rows of white spots along its back and sides.
// Run from the repository root: java scripts/models/animals/DeerTexture.java [assets folder]
public final class DeerTexture {
    static final int[] COAT = Pelt.rgb(160, 104, 60);
    static final int[] FAWN = Pelt.rgb(178, 112, 58);
    static final int[] DORSAL = Pelt.rgb(116, 74, 42);
    static final int[] GREY = Pelt.rgb(138, 104, 74);
    static final int[] TAN = Pelt.rgb(152, 120, 88);
    static final int[] WHITE = Pelt.rgb(236, 232, 222);
    static final int[] SPOT = Pelt.rgb(240, 230, 210);
    static final int[] HOOF = Pelt.rgb(38, 34, 32);
    static final int[] NOSE = Pelt.rgb(24, 21, 20);
    static final int[] EYE = Pelt.rgb(16, 12, 11);
    static final int[] EAR_INNER = Pelt.rgb(224, 208, 198);
    static final int[] RIM = Pelt.rgb(96, 72, 52);
    static final int[] GLAND = Pelt.rgb(86, 58, 36);
    static final int[] MOUTH = Pelt.rgb(156, 94, 94);
    static final int[] ANTLER = Pelt.rgb(150, 126, 90);
    static final int[] ANTLER_ROOT = Pelt.rgb(94, 74, 50);
    static final int[] ANTLER_TIP = Pelt.rgb(228, 216, 188);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(false).save(folder, "deer");
        paint(true).save(folder, "deer_fawn");
    }

    static Pelt paint(boolean fawn) {
        Pelt p = new Pelt(128, 64, fawn ? 5521L : 4410L);
        int[] coat = fawn ? FAWN : COAT;
        p.paint(new Pelt.Box("torso", 0, 0, 6, 7, 15), (f, x, y, z, px, py) -> torso(p, coat, fawn, f, x, y, z, px,
                py));
        p.paint(new Pelt.Box("chest", 42, 0, 6, 8, 3), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM
                ? WHITE : p.fur(Pelt.mix(Pelt.mix(coat, GREY, 0.4), WHITE, Pelt.step(0.6, 1.0, y)
                        * (f == Pelt.Face.FRONT ? 0.8 : 0.3)), null, 0.0, px, py));
        p.paint(new Pelt.Box("withers", 60, 0, 4, 1, 5), (f, x, y, z, px, py) -> p.fur(Pelt.mix(coat, DORSAL,
                f == Pelt.Face.TOP ? 0.6 : 0.3), null, 0.0, px, py));
        p.paint(new Pelt.Box("croup", 78, 0, 5, 6, 3), (f, x, y, z, px, py) -> croup(p, coat, f, x, y, px, py));
        p.paint(new Pelt.Box("tail", 42, 11, 3, 4, 1), (f, x, y, z, px, py) -> tail(p, coat, f, x, y, px, py));
        p.paint(new Pelt.Box("neck", 0, 22, 3, 9, 4), (f, x, y, z, px, py) -> neck(p, coat, f, x, y, px, py));
        p.paint(new Pelt.Box("neck base", 14, 22, 4, 4, 5), (f, x, y, z, px, py) -> neck(p, coat, f, x,
                0.6 + 0.4 * y, px, py));
        p.paint(new Pelt.Box("skull", 94, 0, 4, 4, 5), (f, x, y, z, px, py) -> skull(p, fawn, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 112, 0, 3, 2, 4), (f, x, y, z, px, py) -> muzzle(p, f, x, y, z, px, py));
        p.paint(new Pelt.Box("nose", 70, 11, 2, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? Pelt.scale(NOSE, 1.6) : NOSE);
        p.paint(new Pelt.Box("eye", 76, 11, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.RIGHT
                || f == Pelt.Face.LEFT ? Pelt.scale(EYE, 2.2) : EYE);
        p.paint(new Pelt.Box("jaw", 58, 11, 2, 1, 4), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? MOUTH
                : f == Pelt.Face.FRONT ? Pelt.mix(WHITE, RIM, 0.5) : p.fur(WHITE, null, 0.0, px, py));
        p.paint(new Pelt.Box("ear", 50, 11, 3, 4, 1), (f, x, y, z, px, py) -> ear(p, f, x, y, px, py));
        p.paint(new Pelt.Box("front leg", 32, 22, 3, 7, 3), (f, x, y, z, px, py) -> leg(p, coat, f, y, 0.0, px, py));
        p.paint(new Pelt.Box("front cannon", 62, 22, 2, 5, 2), (f, x, y, z, px, py) -> cannon(p, f, y, false, px,
                py));
        p.paint(new Pelt.Box("dewclaw", 80, 11, 1, 1, 1), (f, x, y, z, px, py) -> HOOF);
        p.paint(new Pelt.Box("hoof", 84, 11, 2, 1, 2), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM
                ? Pelt.scale(HOOF, 0.7) : (f == Pelt.Face.FRONT || f == Pelt.Face.TOP) && x > 0.4 && x < 0.6
                        ? Pelt.scale(HOOF, 0.55) : HOOF);
        p.paint(new Pelt.Box("thigh", 44, 22, 4, 7, 5), (f, x, y, z, px, py) -> leg(p, coat, f, y,
                f == Pelt.Face.BACK || f == Pelt.Face.LEFT ? 0.55 : 0.0, px, py));
        p.paint(new Pelt.Box("gaskin", 92, 11, 3, 4, 2), (f, x, y, z, px, py) -> p.fur(Pelt.mix(Pelt.mix(coat, TAN,
                0.6), WHITE, f == Pelt.Face.LEFT ? 0.4 : 0.0), null, 0.0, px, py));
        p.paint(new Pelt.Box("hind cannon", 70, 22, 2, 5, 2), (f, x, y, z, px, py) -> cannon(p, f, y, true, px, py));
        if (!fawn) {
            p.paint(new Pelt.Box("beam", 102, 11, 1, 3, 1), (f, x, y, z, px, py) -> antler(p, Pelt.mix(ANTLER_ROOT,
                    ANTLER, 1.0 - y), px, py));
            p.paint(new Pelt.Box("brow tine", 110, 11, 1, 1, 2), (f, x, y, z, px, py) -> antler(p, Pelt.mix(ANTLER,
                    ANTLER_TIP, 1.0 - z), px, py));
            p.paint(new Pelt.Box("main beam", 42, 17, 1, 1, 4), (f, x, y, z, px, py) -> antler(p, Pelt.mix(ANTLER,
                    ANTLER_TIP, 0.5 - 0.5 * z), px, py));
            p.paint(new Pelt.Box("tine", 106, 11, 1, 3, 1), (f, x, y, z, px, py) -> antler(p, Pelt.mix(ANTLER,
                    ANTLER_TIP, 1.0 - y), px, py));
            p.paint(new Pelt.Box("tip", 116, 11, 1, 1, 2), (f, x, y, z, px, py) -> antler(p, Pelt.mix(ANTLER,
                    ANTLER_TIP, 0.6 + 0.4 * (1.0 - z)), px, py));
        }
        return p;
    }

    // The croup over the hips: the coat on top and sides, the white rump patch round the tail behind.
    static int[] croup(Pelt p, int[] coat, Pelt.Face f, double x, double y, int px, int py) {
        int[] c = p.fur(Pelt.mix(coat, DORSAL, f == Pelt.Face.TOP ? 0.45 : 0.1), null, 0.0, px, py);
        if (f == Pelt.Face.BACK) {
            return Pelt.mix(c, WHITE, Pelt.step(0.15, 0.4, y) * Pelt.step(0.48, 0.25, Math.abs(x - 0.5)));
        }
        return Pelt.scale(c, 1.03 - 0.06 * y);
    }

    static int[] torso(Pelt p, int[] coat, boolean fawn, Pelt.Face f, double x, double y, double z, int px,
            int py) {
        if (f == Pelt.Face.BOTTOM) {
            return p.fur(WHITE, null, 0.0, px, py);
        }
        double belly = f == Pelt.Face.TOP ? 0.0 : Pelt.step(0.7, 0.92, y);
        double spine = f == Pelt.Face.TOP ? Pelt.step(0.3, 0.05, Math.abs(x - 0.5)) : Pelt.step(0.2, 0.0, y) * 0.5;
        int[] c = p.fur(Pelt.mix(coat, DORSAL, spine * 0.8), null, 0.0, px, py);
        if (f == Pelt.Face.FRONT) {
            // The chest, greyer, white low down between the forelegs.
            c = Pelt.mix(Pelt.mix(c, GREY, 0.35), WHITE, Pelt.step(0.65, 0.95, y) * Pelt.step(0.35, 0.1,
                    Math.abs(x - 0.5)));
        }
        if (f == Pelt.Face.BACK) {
            // The rump: white below the tail and down the backs of the thighs.
            double patch = Pelt.step(0.2, 0.45, y) * Pelt.step(0.38, 0.18, Math.abs(x - 0.5));
            c = Pelt.mix(c, WHITE, patch);
        }
        c = Pelt.mix(c, WHITE, belly);
        if (fawn && belly < 0.3 && f != Pelt.Face.FRONT && f != Pelt.Face.BACK) {
            // Two rows of spots either side of the spine, more scattered over the flanks.
            boolean row = f == Pelt.Face.TOP ? Math.abs(Math.abs(x - 0.5) - 0.3) < 0.09 && px % 2 == 0
                    : (py % 3 == 0 && px % 2 == (py / 3) % 2 && y < 0.6);
            boolean scatter = p.noise(px, py, 7) < 0.07 && y < 0.65;
            if ((row && p.noise(px, py, 8) < 0.8) || scatter) {
                c = SPOT;
            }
        }
        return Pelt.scale(c, 1.04 - 0.08 * y);
    }

    static int[] tail(Pelt p, int[] coat, Pelt.Face f, double x, double y, int px, int py) {
        return switch (f) {
            // The underside, turned out when the tail is raised: all white.
            case FRONT, BOTTOM -> WHITE;
            case BACK -> p.fur(Pelt.mix(coat, DORSAL, Math.abs(x - 0.5) < 0.2 ? 0.7 : 0.2), null, 0.0, px, py);
            case TOP -> p.fur(coat, null, 0.0, px, py);
            default -> y < 0.3 ? p.fur(coat, null, 0.0, px, py) : WHITE;
        };
    }

    static int[] neck(Pelt p, int[] coat, Pelt.Face f, double x, double y, int px, int py) {
        int[] c = p.fur(Pelt.mix(coat, GREY, 0.3 + 0.3 * (1.0 - y)), null, 0.0, px, py);
        if (f == Pelt.Face.FRONT && y < 0.3 && Math.abs(x - 0.5) < 0.4) {
            // The white throat patch just under the chin.
            return WHITE;
        }
        if (f == Pelt.Face.BACK) {
            return Pelt.mix(c, DORSAL, 0.5);
        }
        return c;
    }

    static int[] skull(Pelt p, boolean fawn, Pelt.Face f, double x, double y, double z, int px, int py) {
        int[] c = p.fur(GREY, null, 0.0, px, py);
        switch (f) {
            case BOTTOM -> {
                return WHITE;
            }
            case FRONT -> {
                // The forehead, darker in its middle.
                return Pelt.mix(c, DORSAL, Pelt.step(0.4, 0.1, Math.abs(x - 0.5)) * (fawn ? 0.3 : 0.6));
            }
            case TOP -> {
                return Pelt.mix(c, DORSAL, 0.3);
            }
            case RIGHT, LEFT -> {
                int col = (int) (z * 5);
                int row = (int) (y * 4);
                // The lid behind the eye (a part of its own, shut by drawing it in) in a ring of white, a little
                // forward of the middle.
                if (col == 1 && row == 1) {
                    return Pelt.mix(c, DORSAL, 0.35);
                }
                if (Math.abs(col - 1) <= 1 && Math.abs(row - 1) <= 1) {
                    return Pelt.mix(c, WHITE, 0.75);
                }
                return Pelt.mix(c, WHITE, Pelt.step(0.6, 1.0, y) * 0.5);
            }
            default -> {
                return Pelt.mix(c, DORSAL, 0.4);
            }
        }
    }

    static int[] muzzle(Pelt p, Pelt.Face f, double x, double y, double z, int px, int py) {
        int[] c = p.fur(Pelt.mix(GREY, TAN, 0.3), null, 0.0, px, py);
        switch (f) {
            case FRONT -> {
                // The black nose, white lips at its lower corners.
                return y > 0.5 && Math.abs(x - 0.5) > 0.3 ? WHITE : NOSE;
            }
            case BOTTOM -> {
                return WHITE;
            }
            default -> {
                if (z < 0.25) {
                    return f == Pelt.Face.TOP ? NOSE : (y < 0.5 ? NOSE : WHITE);
                }
                // The white band behind the nose.
                if (z < 0.5) {
                    return WHITE;
                }
                return c;
            }
        }
    }

    static int[] ear(Pelt p, Pelt.Face f, double x, double y, int px, int py) {
        // A thin dark rim along its outer edge and tip.
        boolean rim = x < 0.2 || y < 0.15;
        if (f == Pelt.Face.FRONT) {
            // Inside: pale, with long white hairs down its middle.
            return rim ? RIM : Math.abs(x - 0.5) < 0.2 && y > 0.4 ? WHITE : EAR_INNER;
        }
        if (f == Pelt.Face.BACK) {
            return rim ? RIM : p.fur(GREY, null, 0.0, px, py);
        }
        return RIM;
    }

    static int[] leg(Pelt p, int[] coat, Pelt.Face f, double y, double inner, int px, int py) {
        int[] c = p.fur(Pelt.mix(coat, TAN, 0.25 + 0.5 * y), null, 0.0, px, py);
        return Pelt.mix(c, WHITE, inner * (0.4 + 0.6 * y));
    }

    static int[] cannon(Pelt p, Pelt.Face f, double y, boolean hind, int px, int py) {
        int[] c = p.fur(TAN, null, 0.0, px, py);
        if (f == Pelt.Face.LEFT) {
            c = Pelt.mix(c, WHITE, 0.45);
        }
        if (f == Pelt.Face.FRONT) {
            c = Pelt.mix(c, GREY, 0.4);
        }
        if (hind && f == Pelt.Face.RIGHT && y > 0.3 && y < 0.55) {
            // The dark tuft of the gland on the outside of each hind leg.
            return GLAND;
        }
        return c;
    }

    static int[] antler(Pelt p, int[] c, int px, int py) {
        // Rough, pearled bone.
        return Pelt.scale(c, 0.92 + 0.16 * p.noise(px, py, 5));
    }
}
