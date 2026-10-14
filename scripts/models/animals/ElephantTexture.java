import java.io.IOException;
import java.nio.file.Path;

// Paints the elephant's textures (textures/entity/elephant.png and elephant_calf.png) from scratch, box by box as
// ElephantModel lays it out. Grey skin, a little brown with the dust it throws over itself and browner along its
// back, creased all over with fine wrinkles that run across the body, round the legs and round the trunk in rings;
// darker under the belly and in the folds; the ears thinner and paler toward their ragged edges, pinker inside;
// small dark eyes in a pale fold; pale toenails round each foot; a pink tip to the trunk and a pink mouth; ivory
// tusks, stained where they leave the face; a dark tuft at the end of the tail. The calf: browner and downier.
// Run from the repository root: java scripts/models/animals/ElephantTexture.java [assets folder]
public final class ElephantTexture {
    record Coat(String name, int[] skin, int[] dust, int[] fold, int[] pale, int[] pink) {
    }

    static final int[] EYE = Pelt.rgb(18, 14, 12);
    static final int[] NAIL = Pelt.rgb(214, 200, 176);
    static final int[] IVORY = Pelt.rgb(236, 226, 196);
    static final int[] STAIN = Pelt.rgb(176, 152, 116);
    static final int[] TUFT = Pelt.rgb(34, 28, 26);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("elephant", Pelt.rgb(128, 120, 114), Pelt.rgb(146, 124, 104), Pelt.rgb(88, 82, 78),
                Pelt.rgb(156, 148, 140), Pelt.rgb(176, 136, 128)));
        paint(folder, new Coat("elephant_calf", Pelt.rgb(138, 122, 110), Pelt.rgb(150, 122, 98),
                Pelt.rgb(100, 88, 80), Pelt.rgb(164, 150, 138), Pelt.rgb(184, 142, 132)));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(256, 128, k.name.hashCode());
        p.paint(new Pelt.Box("forequarters", 0, 0, 22, 26, 20), (f, x, y, z, px, py) -> trunkBody(p, k, f, y, px,
                py));
        p.paint(new Pelt.Box("hindquarters", 84, 0, 21, 24, 22), (f, x, y, z, px, py) -> trunkBody(p, k, f, y, px,
                py));
        p.paint(new Pelt.Box("belly", 0, 50, 19, 2, 28), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.skin, k.fold,
                f == Pelt.Face.BOTTOM ? 0.6 : 0.35), k, px, py, true));
        p.paint(new Pelt.Box("withers", 94, 50, 14, 2, 14), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.skin, k.dust,
                0.7), k, px, py, true));
        p.paint(new Pelt.Box("croup", 94, 66, 12, 2, 12), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.skin, k.dust,
                0.7), k, px, py, true));
        p.paint(new Pelt.Box("head", 170, 0, 16, 18, 12), (f, x, y, z, px, py) -> head(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("dome", 170, 30, 12, 2, 9), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.skin, k.dust,
                0.5), k, px, py, true));
        // The ear in its two halves, its markings laid across both as across one.
        p.paint(new Pelt.Box("ear", 226, 0, 1, 18, 8), (f, x, y, z, px, py) -> ear(p, k, f == Pelt.Face.BACK
                ? Pelt.Face.LEFT : f, y, z * 8.0 / 15.0, px, py));
        p.paint(new Pelt.Box("ear flap", 226, 26, 1, 18, 7), (f, x, y, z, px, py) -> ear(p, k,
                f == Pelt.Face.FRONT ? Pelt.Face.LEFT : f, y, (8.0 + z * 7.0) / 15.0, px, py));
        // A brown eye, its pupil and the dark lid line toward the front.
        p.paint(new Pelt.Box("eye", 252, 0, 1, 1, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? k.fold
                : f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT ? Pelt.rgb(64, 40, 26) : EYE);
        p.paint(new Pelt.Box("tail", 244, 0, 2, 16, 2), (f, x, y, z, px, py) -> f == Pelt.Face.BOTTOM || y > 0.78
                ? Pelt.scale(TUFT, 0.9 + 0.3 * p.noise(px, py, 6)) : skin(p, k.skin, k, px, py, true));
        int[] wide = { 8, 7, 6, 5, 4 };
        int[] tall = { 10, 9, 8, 8, 6 };
        int[] u = { 150, 182, 210, 234, 150 };
        int[] v = { 52, 52, 52, 52, 70 };
        for (int piece = 0; piece < 5; piece++) {
            boolean tip = piece == 4;
            double pale = 0.08 * piece;
            p.paint(new Pelt.Box("trunk " + piece, u[piece], v[piece], wide[piece], tall[piece], wide[piece]),
                    (f, x, y, z, px, py) -> {
                        if (tip && (f == Pelt.Face.BOTTOM || y > 0.84)) {
                            return Pelt.scale(k.pink, f == Pelt.Face.BOTTOM ? 0.8 : 1.0);
                        }
                        // The underside of the trunk is paler and smoother than its ringed front.
                        int[] base = Pelt.mix(k.skin, k.pale, f == Pelt.Face.BACK ? 0.6 : pale);
                        return skin(p, base, k, px, py, f != Pelt.Face.TOP && f != Pelt.Face.BOTTOM);
                    });
        }
        p.paint(new Pelt.Box("tusk", 166, 70, 2, 2, 6), (f, x, y, z, px, py) -> ivory(p, f, z, 0.9, px, py));
        p.paint(new Pelt.Box("long tusk", 182, 70, 2, 2, 6), (f, x, y, z, px, py) -> ivory(p, f, z, 0.0, px, py));
        p.paint(new Pelt.Box("tusk point", 194, 41, 1, 1, 4), (f, x, y, z, px, py) -> Pelt.scale(IVORY, 1.04));
        p.paint(new Pelt.Box("leg", 0, 80, 9, 17, 9), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.dust, k.skin,
                Pelt.step(0.0, 0.5, y)), k, px, py, true));
        p.paint(new Pelt.Box("shin", 36, 80, 8, 14, 8), (f, x, y, z, px, py) -> skin(p, Pelt.mix(k.skin, k.fold,
                0.3 * Pelt.step(0.6, 1.0, y)), k, px, py, true));
        p.paint(new Pelt.Box("foot", 68, 80, 9, 2, 9), (f, x, y, z, px, py) -> foot(p, k, f, x, y, px, py));
        p.paint(new Pelt.Box("lip", 170, 41, 6, 2, 6), (f, x, y, z, px, py) -> f == Pelt.Face.TOP ? Pelt.scale(k.pink,
                0.8) : skin(p, Pelt.mix(k.skin, k.pink, 0.35), k, px, py, false));
        p.save(folder, k.name);
    }

    // Skin at a pixel: fine grain, and every few pixels a crease: a darker line, broken here and there, with a paler
    // one just under it.
    static int[] skin(Pelt p, int[] base, Coat k, int px, int py, boolean creased) {
        int[] c = Pelt.scale(base, 0.93 + 0.14 * p.noise(px, py, 1));
        if (!creased) {
            return c;
        }
        int row = py + (int) (2.0 * p.noise(px / 5, 0, 7));
        boolean broken = p.noise(px / 2, py, 8) < 0.3;
        if (row % 4 == 0 && !broken) {
            return Pelt.mix(c, k.fold, 0.45);
        }
        if (row % 4 == 1 && !broken) {
            return Pelt.mix(c, k.pale, 0.2);
        }
        return c;
    }

    // Dusty along the back, grey down the flank, darker under the belly.
    static int[] trunkBody(Pelt p, Coat k, Pelt.Face f, double y, int px, int py) {
        double down = f == Pelt.Face.TOP ? 0.0 : f == Pelt.Face.BOTTOM ? 1.0 : y;
        int[] base = Pelt.mix(Pelt.mix(k.dust, k.skin, Pelt.step(0.0, 0.4, down)), k.fold,
                0.5 * Pelt.step(0.82, 1.0, down));
        return skin(p, base, k, px, py, true);
    }

    static int[] head(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        boolean side = f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT;
        int row = (int) (y * 18);
        int col = (int) (z * 12);
        if (side && col == 2) {
            // The eye sits low and forward, in a fold of paler skin.
            // Behind the eye, which is a part of its own: its lid, seen as it shuts.
            if (row == 10) {
                return Pelt.mix(k.pale, k.fold, 0.5);
            }
            if (row == 9 || row == 11) {
                return Pelt.mix(k.pale, k.skin, 0.3);
            }
        }
        if (side && row == 10 && (col == 1 || col == 3)) {
            return Pelt.mix(k.fold, k.skin, 0.4);
        }
        double top = f == Pelt.Face.TOP ? 0.0 : f == Pelt.Face.BOTTOM ? 1.0 : y;
        int[] base = Pelt.mix(Pelt.mix(k.dust, k.skin, Pelt.step(0.0, 0.35, top)), k.fold,
                0.4 * Pelt.step(0.85, 1.0, top));
        // The temples are hollow: a darker dip above and behind each eye.
        if (side && row >= 4 && row <= 7 && col >= 3 && col <= 6) {
            base = Pelt.mix(base, k.fold, 0.3);
        }
        return skin(p, base, k, px, py, f != Pelt.Face.FRONT || row > 5);
    }

    // The ear's two broad faces: creased skin near the head, thinner and paler toward the ragged edge behind and
    // below, where veins show; its hind edge darker.
    static int[] ear(Pelt p, Coat k, Pelt.Face f, double y, double z, int px, int py) {
        if (f != Pelt.Face.RIGHT && f != Pelt.Face.LEFT) {
            return skin(p, Pelt.mix(k.skin, k.fold, 0.35), k, px, py, false);
        }
        double edge = Math.max(Pelt.step(0.45, 1.0, z), Pelt.step(0.6, 1.0, y) * 0.8);
        int[] base = Pelt.mix(k.skin, Pelt.mix(k.pale, k.pink, 0.35), 0.75 * edge);
        // Veins branching back from the head.
        boolean vein = edge > 0.2 && Math.abs(Math.sin(y * 9.0 + z * 4.0 + 3.0 * p.noise(px / 3, py / 3, 9))) < 0.16;
        if (vein) {
            base = Pelt.mix(base, k.fold, 0.3);
        }
        if (z > 0.9 || y > 0.93) {
            base = Pelt.mix(base, k.fold, p.noise(px, py, 10) < 0.5 ? 0.5 : 0.15);
        }
        return Pelt.scale(base, 0.94 + 0.12 * p.noise(px, py, 1));
    }

    // The round foot: creased skin with a rim of pale toenails across the front and round the sides, over a dark sole.
    static int[] foot(Pelt p, Coat k, Pelt.Face f, double x, double y, int px, int py) {
        if (f == Pelt.Face.BOTTOM) {
            return Pelt.scale(k.fold, 0.75 + 0.2 * p.noise(px, py, 3));
        }
        if (f == Pelt.Face.TOP) {
            return skin(p, Pelt.mix(k.skin, k.fold, 0.3), k, px, py, false);
        }
        int col = (int) (x * 9);
        if (y > 0.5 && (f == Pelt.Face.FRONT && col % 2 == 1 || (f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT)
                && px % 3 == 0)) {
            return NAIL;
        }
        return skin(p, Pelt.mix(k.skin, k.fold, 0.35), k, px, py, false);
    }

    static int[] ivory(Pelt p, Pelt.Face f, double z, double stain, int px, int py) {
        double root = f == Pelt.Face.BACK ? 1.0 : f == Pelt.Face.FRONT ? 0.0 : z;
        int[] c = Pelt.mix(IVORY, STAIN, stain * Pelt.step(0.3, 1.0, root));
        return Pelt.scale(c, (f == Pelt.Face.BOTTOM ? 0.88 : 0.97) + 0.06 * p.noise(px, py, 5));
    }
}
