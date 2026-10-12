import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// What the wild animals' texture painters share: a picture, the boxes of a model in it, and a way to colour every
// pixel of a box by where it lies on the box (x from the animal's right side to its left, y from top to bottom, z from
// front to back, each 0 to 1), so markings run on across faces as they do on a coat; and fur: a grain of short strands
// and lighter tips.
final class Pelt {
    enum Face { TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK }

    // A box as the model builds it: its corner in the picture and its size in pixels.
    record Box(String name, int u, int v, int w, int h, int d) {
        int[] face(Face face) {
            return switch (face) {
                case TOP -> new int[] { u + d, v, w, d };
                case BOTTOM -> new int[] { u + d + w, v, w, d };
                case RIGHT -> new int[] { u, v + d, d, h };
                case FRONT -> new int[] { u + d, v + d, w, h };
                case LEFT -> new int[] { u + d + w, v + d, d, h };
                case BACK -> new int[] { u + 2 * d + w, v + d, w, h };
            };
        }
    }

    // The colour of one pixel: its face, where it lies on the box, and its place in the picture.
    interface Coat {
        int[] at(Face face, double x, double y, double z, int px, int py);
    }

    final BufferedImage image;
    private final long seed;
    private final List<int[]> used = new ArrayList<>();

    Pelt(int width, int height, long seed) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.seed = seed;
    }

    static Path textures(String[] args, String folder) throws IOException {
        return Files.createDirectories(Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/welcomescreen")
                .resolve("textures/" + folder));
    }

    void save(Path folder, String name) throws IOException {
        ImageIO.write(this.image, "png", folder.resolve(name + ".png").toFile());
    }

    // Paints every face of `box`; a box that runs into another's place in the picture, or out of it, is refused.
    void paint(Box box, Coat coat) {
        int[] whole = { box.u, box.v, 2 * (box.d + box.w), box.d + box.h };
        if (whole[0] + whole[2] > this.image.getWidth() || whole[1] + whole[3] > this.image.getHeight()) {
            throw new IllegalStateException(box.name + " runs out of the picture");
        }
        for (int[] other : this.used) {
            if (whole[0] < other[0] + other[2] && other[0] < whole[0] + whole[2] && whole[1] < other[1] + other[3]
                    && other[1] < whole[1] + whole[3] && other[4] != (box.u * 31 + box.v)) {
                throw new IllegalStateException(box.name + " overlaps another box in the picture");
            }
        }
        this.used.add(new int[] { whole[0], whole[1], whole[2], whole[3], box.u * 31 + box.v });
        for (Face face : Face.values()) {
            int[] f = box.face(face);
            for (int py = 0; py < f[3]; py++) {
                for (int px = 0; px < f[2]; px++) {
                    double s = (px + 0.5) / f[2];
                    double t = (py + 0.5) / f[3];
                    double x;
                    double y;
                    double z;
                    switch (face) {
                        case TOP -> { x = s; y = 0.0; z = 1.0 - t; }
                        case BOTTOM -> { x = s; y = 1.0; z = 1.0 - t; }
                        case RIGHT -> { x = 0.0; y = t; z = 1.0 - s; }
                        case LEFT -> { x = 1.0; y = t; z = s; }
                        case FRONT -> { x = s; y = t; z = 0.0; }
                        default -> { x = 1.0 - s; y = t; z = 1.0; }
                    }
                    int[] c = coat.at(face, x, y, z, f[0] + px, f[1] + py);
                    if (c != null) {
                        this.image.setRGB(f[0] + px, f[1] + py, argb(c));
                    }
                }
            }
        }
    }

    // A steady random number from 0 to 1 for a pixel (and a channel `k`), the same on every run.
    double noise(int px, int py, int k) {
        long h = this.seed * 0x9E3779B97F4A7C15L + px * 0xC2B2AE3D27D4EB4FL + py * 0x165667B19E3779F9L
                + k * 0xD6E8FEB86659FD93L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    // Fur over `base`: each pixel a little lighter or darker, in strands two pixels long down the coat, and here and
    // there (at `tips`, 0 to 1) a light tip of `tip`'s colour.
    int[] fur(int[] base, int[] tip, double tips, int px, int py) {
        double strand = this.noise(px, py / 2, 1) * 0.16 - 0.08;
        double grain = this.noise(px, py, 2) * 0.1 - 0.05;
        int[] c = scale(base, 1.0 + strand + grain);
        if (tip != null && this.noise(px, py, 3) < tips) {
            c = mix(c, tip, 0.3 + 0.3 * this.noise(px, py, 4));
        }
        return c;
    }

    static int[] rgb(int r, int g, int b) {
        return new int[] { r, g, b };
    }

    static int[] mix(int[] a, int[] b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        return new int[] { (int) Math.round(a[0] + (b[0] - a[0]) * k), (int) Math.round(a[1] + (b[1] - a[1]) * k),
                (int) Math.round(a[2] + (b[2] - a[2]) * k) };
    }

    static int[] scale(int[] c, double f) {
        return new int[] { (int) Math.round(c[0] * f), (int) Math.round(c[1] * f), (int) Math.round(c[2] * f) };
    }

    // 0 below `from`, 1 above `to`, smooth between.
    static double step(double from, double to, double v) {
        double t = Math.max(0.0, Math.min(1.0, (v - from) / (to - from)));
        return t * t * (3.0 - 2.0 * t);
    }

    static int argb(int[] c) {
        return 0xFF000000 | clamp(c[0]) << 16 | clamp(c[1]) << 8 | clamp(c[2]);
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
