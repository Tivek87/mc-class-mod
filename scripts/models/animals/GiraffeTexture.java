import java.io.IOException;
import java.nio.file.Path;

// Paints the giraffe's textures (textures/entity/giraffe.png and giraffe_calf.png) from scratch, box by box as
// GiraffeModel lays it out. Its coat is a net of cream lines round chestnut patches, each patch a little darker at
// its heart and no two the same shade: big on the trunk and neck, smaller on the head and legs, fading out below the
// knees and hocks into plain cream shins over dark hooves, and fainter under the belly. A short red-brown mane, a
// pale face darkening to a grey-brown nose, dark eyes, blunt horns tipped black, a dark tongue and a black tail
// tuft. The calf: the same coat, paler.
// Run from the repository root: java scripts/models/animals/GiraffeTexture.java [assets folder]
public final class GiraffeTexture {
    record Coat(String name, int[] line, int[] patch, int[] heart, int[] mane, int[] nose) {
    }

    static final int[] HOOF = Pelt.rgb(46, 36, 30);
    static final int[] EYE = Pelt.rgb(20, 14, 12);
    static final int[] TIP = Pelt.rgb(40, 30, 26);
    static final int[] TONGUE = Pelt.rgb(70, 54, 84);
    static final int[] EAR_INSIDE = Pelt.rgb(206, 176, 150);

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, new Coat("giraffe", Pelt.rgb(236, 212, 162), Pelt.rgb(164, 96, 46), Pelt.rgb(118, 62, 28),
                Pelt.rgb(132, 72, 36), Pelt.rgb(96, 74, 62)));
        paint(folder, new Coat("giraffe_calf", Pelt.rgb(240, 222, 182), Pelt.rgb(190, 130, 76),
                Pelt.rgb(156, 98, 52), Pelt.rgb(150, 96, 54), Pelt.rgb(112, 90, 76)));
    }

    static void paint(Path folder, Coat k) throws IOException {
        Pelt p = new Pelt(128, 64, k.name.hashCode());
        p.paint(new Pelt.Box("chest", 0, 0, 10, 13, 9), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.8,
                belly(f, y)));
        p.paint(new Pelt.Box("rump", 38, 0, 9, 10, 9), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.8,
                belly(f, y)));
        p.paint(new Pelt.Box("withers", 74, 0, 7, 2, 6), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.8, 0.0));
        p.paint(new Pelt.Box("tongue", 100, 0, 1, 1, 4), (f, x, y, z, px, py) -> Pelt.scale(TONGUE, f == Pelt.Face.TOP
                ? 1.15 : 0.95));
        p.paint(new Pelt.Box("tail", 110, 0, 1, 9, 1), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.line, k.patch, 0.35),
                null, 0.0, px, py));
        p.paint(new Pelt.Box("tuft", 114, 0, 2, 3, 2), (f, x, y, z, px, py) -> p.fur(TIP, null, 0.0, px, py));
        p.paint(new Pelt.Box("neck 1", 0, 24, 5, 11, 6), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.5, 0.0));
        p.paint(new Pelt.Box("neck 2", 22, 24, 4, 9, 5), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.3, 0.0));
        p.paint(new Pelt.Box("neck 3", 40, 24, 4, 8, 4), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.1, 0.0));
        for (int piece = 0; piece < 3; piece++) {
            p.paint(new Pelt.Box("mane " + piece, 56 + 4 * piece, 24, 1, new int[] { 11, 9, 8 }[piece], 1),
                    (f, x, y, z, px, py) -> Pelt.scale(k.mane, 0.85 + 0.3 * p.noise(px, py, 5)));
        }
        p.paint(new Pelt.Box("skull", 68, 24, 4, 5, 5), (f, x, y, z, px, py) -> skull(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("muzzle", 86, 24, 3, 2, 5), (f, x, y, z, px, py) -> muzzle(p, k, f, x, y, z, px, py));
        p.paint(new Pelt.Box("jaw", 102, 24, 3, 1, 5), (f, x, y, z, px, py) -> p.fur(Pelt.mix(k.nose, k.line,
                f == Pelt.Face.TOP ? 0.2 : Pelt.step(0.0, 0.8, z)), null, 0.0, px, py));
        p.paint(new Pelt.Box("horn", 118, 24, 1, 3, 1), (f, x, y, z, px, py) -> f == Pelt.Face.TOP || y < 0.34 ? TIP
                : p.fur(Pelt.mix(k.line, k.patch, 0.45), null, 0.0, px, py));
        p.paint(new Pelt.Box("ear", 122, 24, 2, 3, 1), (f, x, y, z, px, py) -> f == Pelt.Face.FRONT && y > 0.3
                ? EAR_INSIDE : p.fur(Pelt.mix(k.line, k.patch, 0.3), null, 0.0, px, py));
        p.paint(new Pelt.Box("front leg", 0, 44, 4, 14, 4), (f, x, y, z, px, py) -> hide(p, k, px, py, 2.9,
                0.55 * Pelt.step(0.3, 1.0, y)));
        p.paint(new Pelt.Box("front cannon", 16, 44, 3, 14, 3), (f, x, y, z, px, py) -> shin(p, k, y, px, py));
        p.paint(new Pelt.Box("hoof", 28, 44, 3, 2, 4), (f, x, y, z, px, py) -> Pelt.scale(HOOF, y < 0.5 ? 1.3 : 1.0));
        p.paint(new Pelt.Box("thigh", 42, 44, 5, 12, 6), (f, x, y, z, px, py) -> hide(p, k, px, py, 3.3,
                0.3 * Pelt.step(0.5, 1.0, y)));
        p.paint(new Pelt.Box("gaskin", 64, 44, 3, 9, 3), (f, x, y, z, px, py) -> hide(p, k, px, py, 2.7,
                0.3 + 0.45 * y));
        p.paint(new Pelt.Box("hind cannon", 76, 44, 3, 12, 3), (f, x, y, z, px, py) -> shin(p, k, y, px, py));
        p.save(folder, k.name);
    }

    // How far the patches fade into the cream on a trunk box: not at all on the back, more low on the flank, most
    // under the belly.
    static double belly(Pelt.Face f, double y) {
        return f == Pelt.Face.TOP ? 0.0 : f == Pelt.Face.BOTTOM ? 0.7 : 0.45 * Pelt.step(0.7, 1.0, y);
    }

    // The coat at a pixel: a cream line where two patches meet, else its patch, darker at its heart.
    static int[] hide(Pelt p, Coat k, int px, int py, double size, double fade) {
        int[] cream = Pelt.scale(k.line, 0.94 + 0.1 * p.noise(px, py, 2));
        double cx = px + 0.5;
        double cy = py + 0.5;
        int gx = (int) Math.floor(cx / size);
        int gy = (int) Math.floor(cy / size);
        double near = Double.MAX_VALUE;
        double next = Double.MAX_VALUE;
        double shade = 0.0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int ix = gx + dx;
                int iy = gy + dy;
                double sx = (ix + 0.15 + 0.7 * hash(ix, iy, 1)) * size;
                double sy = (iy + 0.15 + 0.7 * hash(ix, iy, 2)) * size;
                double d = Math.hypot(cx - sx, cy - sy);
                if (d < near) {
                    next = near;
                    near = d;
                    shade = hash(ix, iy, 3);
                } else if (d < next) {
                    next = d;
                }
            }
        }
        if (next - near < 0.85) {
            return cream;
        }
        int[] patch = Pelt.mix(k.patch, k.heart, 0.25 + 0.5 * shade + 0.25 * Pelt.step(0.6, 0.0, near / size));
        return Pelt.mix(Pelt.scale(patch, 0.95 + 0.1 * p.noise(px, py, 4)), cream, fade);
    }

    // Below the knee and the hock: the last small spots, then plain cream down to the hoof.
    static int[] shin(Pelt p, Coat k, double y, int px, int py) {
        return hide(p, k, px, py, 2.3, 0.7 + 0.3 * Pelt.step(0.0, 0.45, y));
    }

    static int[] skull(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        boolean side = f == Pelt.Face.RIGHT || f == Pelt.Face.LEFT;
        int row = (int) (y * 5);
        if (side && row == 1 && (int) (z * 5) == 1) {
            return EYE;
        }
        // Small faint patches on the cheeks and behind the ears; the brow and forehead plain and a little tawny.
        if (f == Pelt.Face.FRONT || f == Pelt.Face.TOP && z < 0.5) {
            return p.fur(Pelt.mix(k.line, k.patch, 0.3), null, 0.0, px, py);
        }
        return hide(p, k, px, py, 2.5, f == Pelt.Face.BOTTOM ? 0.8 : 0.35);
    }

    static int[] muzzle(Pelt p, Coat k, Pelt.Face f, double x, double y, double z, int px, int py) {
        if (f == Pelt.Face.FRONT) {
            // Two nostrils set wide apart.
            boolean nostril = (int) (y * 2) == 0 && ((int) (x * 3) == 0 || (int) (x * 3) == 2);
            return nostril ? EYE : p.fur(k.nose, null, 0.0, px, py);
        }
        return p.fur(Pelt.mix(k.nose, Pelt.mix(k.line, k.patch, 0.25), Pelt.step(0.15, 0.7, z)), null, 0.0, px, py);
    }

    static double hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }
}
