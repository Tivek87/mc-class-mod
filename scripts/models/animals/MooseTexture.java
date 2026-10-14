import java.io.IOException;
import java.nio.file.Path;

// Paints the moose's textures (textures/entity/moose.png and moose_calf.png) from scratch, box by box as MooseModel
// lays it out. The grown moose: a dark chocolate-brown coat, almost black over the hump, neck and shoulders, a little
// browner on the rump and flanks; long legs paling from the knee and hock down to grey-white stockings over black
// cloven hooves and dewclaws; a near-black mane along the top of the neck; a greyer, lighter muzzle ending in a dark
// nose with two nostrils; a small dark eye under a lid of the coat's colour; pale inside the ears;
// and antlers the colour of old bone, darker at the burr and palest at the tips. The calf: a light red-brown all
// over, its legs only a little paler.
// Run from the repository root: java scripts/models/animals/MooseTexture.java [assets folder]
public final class MooseTexture {
    record Coat(String name, int[] coat, int[] dark, int[] flank, int[] tip, int[] stocking, int[] muzzle,
            int[] nose) {
    }

    static final int[] HOOF = Pelt.rgb(28, 24, 22);
    static final int[] EYE = Pelt.rgb(14, 12, 12);
    static final int[] EAR_INSIDE = Pelt.rgb(172, 150, 128);
    static final int[] BONE = Pelt.rgb(190, 162, 118);
    static final int[] BURR = Pelt.rgb(132, 104, 72);
    static final int[] POINT = Pelt.rgb(222, 204, 168);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("moose", Pelt.rgb(66, 46, 33), Pelt.rgb(40, 28, 22), Pelt.rgb(80, 57, 40),
                Pelt.rgb(110, 84, 60), Pelt.rgb(164, 150, 132), Pelt.rgb(110, 90, 74), Pelt.rgb(58, 48, 42)));
        paint(folder, new Coat("moose_calf", Pelt.rgb(156, 102, 62), Pelt.rgb(132, 84, 50), Pelt.rgb(166, 112, 70),
                Pelt.rgb(196, 146, 100), Pelt.rgb(178, 132, 92), Pelt.rgb(122, 86, 60), Pelt.rgb(60, 44, 36)));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(128, 64, k.name.hashCode());
        p.paint(new Pelt.Box("chest", 0, 0, 10, 13, 11), (f, x, y, z, px, py) -> trunk(p, k, f, y, 0.35, px, py));
        p.paint(new Pelt.Box("rump", 42, 0, 9, 11, 10), (f, x, y, z, px, py) -> trunk(p, k, f, y, 0.0, px, py));
        p.paint(new Pelt.Box("hump", 80, 0, 8, 2, 9), (f, x, y, z, px, py) -> p.fur(k.dark, k.tip, 0.06, px, py));
        p.paint(new Pelt.Box("neck", 0, 24, 6, 9, 7), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.dark, k.coat,
                0.3 + 0.4 * (1.0 - y)), k.tip, 0.05, px, py));
        p.paint(new Pelt.Box("skull", 26, 24, 6, 6, 7), (f, x, y, z, px, py) -> skull(p, k, f, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 52, 24, 5, 5, 7), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.muzzle, k.coat,
                Pelt.step(0.4, 1.0, z)), null, 0.0, px, py));
        p.paint(new Pelt.Box("nose", 76, 24, 6, 5, 3), (f, x, y, z, px, py) -> nose(p, k, f, x, y, px, py));
        p.paint(new Pelt.Box("jaw", 94, 24, 4, 1, 6), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.muzzle, k.dark, 0.3),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("ear", 114, 24, 2, 5, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT && y > 0.2
                ? EAR_INSIDE : p.fur(k.coat, null, 0.0, px, py));
        p.paint(new Pelt.Box("bell", 120, 24, 2, 5, 2), (f, x, y, z, px, py) -> p.fur(k.dark, null, 0.0, px, py));
        p.paint(new Pelt.Box("front leg", 0, 40, 4, 9, 4), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.coat,
                k.stocking, Pelt.step(0.55, 1.1, y)), null, 0.0, px, py));
        p.paint(new Pelt.Box("front cannon", 16, 40, 3, 8, 3), (f, x, y, z, px, py) -> stocking(p, k, y, px, py));
        // Cloven: a dark split down the middle of its front.
        p.paint(new Pelt.Box("hoof", 28, 40, 3, 2, 4), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT
                && (int) (x * 3) == 1 ? HOOF : Pelt.scale(HOOF, y < 0.5 ? 1.9 : 1.5));
        p.paint(new Pelt.Box("dewclaw", 120, 0, 1, 1, 1), (f, x, y, z, px, py) -> Pelt.scale(HOOF, 1.4));
        p.paint(new Pelt.Box("eye", 114, 0, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP
                ? Pelt.mix(k.dark, k.coat, 0.5) : EYE);
        p.paint(new Pelt.Box("mane", 114, 4, 2, 8, 1), (f, x, y, z, px, py) -> p.fur(Pelt.scale(k.dark, 0.85),
                k.tip, 0.05, px, py));
        p.paint(new Pelt.Box("thigh", 42, 40, 5, 9, 6), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.flank, k.coat,
                Pelt.step(0.3, 0.9, y)), k.tip, 0.05, px, py));
        p.paint(new Pelt.Box("gaskin", 64, 40, 3, 6, 3), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.coat, k.stocking,
                Pelt.step(0.2, 1.0, y)), null, 0.0, px, py));
        p.paint(new Pelt.Box("hind cannon", 76, 40, 3, 8, 3), (f, x, y, z, px, py) -> stocking(p, k, y, px, py));
        p.paint(new Pelt.Box("tail", 88, 40, 2, 3, 1), (f, x, y, z, px, py) -> p.fur(k.dark, null, 0.0, px, py));
        // Antlers: old bone, darker where each piece grows from, palest at its far end.
        p.paint(new Pelt.Box("beam", 0, 56, 4, 1, 1), (f, x, y, z, px, py) -> bone(p, 1.0 - reach(f, x), 0.0, px,
                py));
        p.paint(new Pelt.Box("palm", 10, 56, 7, 1, 7), (f, x, y, z, px, py) -> bone(p, 0.5, 0.35 * (1.0
                - reach(f, x)), px, py));
        p.paint(new Pelt.Box("front tine", 38, 56, 1, 1, 3), (f, x, y, z, px, py) -> bone(p, 0.0, f == Pelt.Face.FRONT
                ? 1.0 : 1.0 - z, px, py));
        p.paint(new Pelt.Box("outer tine", 46, 56, 3, 1, 1), (f, x, y, z, px, py) -> bone(p, 0.0, 1.0 - reach(f, x),
                px, py));
        p.paint(new Pelt.Box("brow tine", 54, 56, 1, 1, 4), (f, x, y, z, px, py) -> bone(p, 0.2, f == Pelt.Face.FRONT
                ? 1.0 : 1.0 - z, px, py));
        p.save(folder, k.name);
    }

    // How far a pixel lies from the animal's right end of a box (the outer end of a right-hand antler piece), 0 to 1.
    static double reach(Pelt.Face f, double x) {
        return f == Pelt.Face.RIGHT ? 0.0 : f == Pelt.Face.LEFT ? 1.0 : x;
    }

    static int[] bone(Pelt p, double burr, double point, int px, int py) {
        int[] c = Pelt.mix(Pelt.mix(BONE, BURR, burr), POINT, point);
        return Pelt.scale(c, 0.92 + 0.16 * p.noise(px, py / 2, 9));
    }

    // Dark along the back (darker the more of the shoulder a box takes in), browner down the flank, dark again
    // under the belly.
    static int[] trunk(Pelt p, Coat k, Pelt.Face f, double y, double shoulder, int px, int py) {
        double down = f == Pelt.Face.TOP ? 0.0 : f == Pelt.Face.BOTTOM ? 1.0 : y;
        int[] base = Pelt.mix(Pelt.mix(k.dark, k.coat, Pelt.step(0.0, 0.35 + shoulder, down)), k.flank,
                Pelt.step(0.35 + shoulder, 0.75, down) * (1.0 - Pelt.step(0.82, 1.0, down)));
        return p.fur(Pelt.mix(base, k.dark, 0.6 * Pelt.step(0.88, 1.0, down)), k.tip, 0.07 * (1.0 - down), px, py);
    }

    static int[] stocking(Pelt p, Coat k, double y, int px, int py) {
        return p.fur(Pelt.mix(k.stocking, Pelt.mix(k.stocking, k.coat, 0.35), Pelt.step(0.75, 1.0, y)), null, 0.0, px,
                py);
    }

    static int[] skull(Pelt p, Coat k, Pelt.Face f, double y, double z, int px, int py) {
        // Behind the eye, which is a part of its own: its lid, seen as it shuts.
        if ((f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT) && (int) (y * 6) == 2 && (int) (z * 7) == 2) {
            return Pelt.mix(k.dark, k.coat, 0.4);
        }
        // Lighter toward the muzzle, darker on the brow and behind the ears.
        double front = f == Pelt.Face.FRONT ? 0.0 : f == Pelt.Face.BACK ? 1.0 : z;
        return p.fur(Pelt.mix(Pelt.mix(k.coat, k.muzzle, 0.5 * (1.0 - front)), k.dark, 0.35 * front), k.tip, 0.04, px,
                py);
    }

    static int[] nose(Pelt p, Coat k, Pelt.Face f, double x, double y, int px, int py) {
        if (f == Pelt.Face.FRONT) {
            int col = (int) (x * 6);
            int row = (int) (y * 5);
            // Two wide nostrils over a soft, paler upper lip.
            if (row == 2 && (col == 1 || col == 4)) {
                return EYE;
            }
            return row >= 4 ? Pelt.mix(k.nose, k.muzzle, 0.6) : p.fur(k.nose, null, 0.0, px, py);
        }
        return p.fur(Pelt.mix(k.nose, k.muzzle, f == Pelt.Face.TOP ? 0.5 : 0.25), null, 0.0, px, py);
    }
}
