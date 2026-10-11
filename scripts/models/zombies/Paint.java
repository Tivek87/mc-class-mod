import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

// What the zombies' texture painters share: a picture, the faces of a model's box in it, and brushes that paint a face
// in jittered colours (fill, row, dot, mottled skin, winding veins).
final class Paint {
    enum Face { TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK }

    // A box as the model builds it: its corner in the picture and its size in pixels (rounded up where it is not whole).
    record Box(int u, int v, int w, int h, int d) {
        // The face's corner in the picture and its size: across (x, or z on the sides) and down (y, or z on top).
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

    final BufferedImage image;
    final Random random;

    Paint(int width, int height, long seed) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.random = new Random(seed);
    }

    static Path textures(String[] args, String folder) throws IOException {
        return Files.createDirectories(Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/welcomescreen")
                .resolve("textures/" + folder));
    }

    void save(Path folder, String name) throws IOException {
        ImageIO.write(this.image, "png", folder.resolve(name + ".png").toFile());
    }

    void all(Box box, int[] colour, int jitter) {
        for (Face face : Face.values()) {
            this.fill(box.face(face), colour, jitter);
        }
    }

    // Every face of the box mottled: `base` with blotches of `dark` and, rarely, `rot`.
    void skin(Box box, int[] base, int[] dark, int[] rot) {
        for (Face face : Face.values()) {
            this.skin(box.face(face), base, dark, rot);
        }
    }

    void skin(int[] f, int[] base, int[] dark, int[] rot) {
        this.fill(f, base, 8);
        int blotches = Math.max(1, f[2] * f[3] / 10);
        for (int k = 0; k < blotches; k++) {
            int[] colour = this.random.nextInt(5) == 0 ? rot : dark;
            this.dot(f, this.random.nextInt(f[2]), this.random.nextInt(f[3]), this.shade(colour, 6));
        }
    }

    // Veins winding down a face.
    void veins(int[] f, int count, int[] colour) {
        for (int k = 0; k < count; k++) {
            int x = this.random.nextInt(f[2]);
            for (int y = this.random.nextInt(3); y < f[3] - 1; y++) {
                this.dot(f, x, y, colour);
                x = Math.max(0, Math.min(f[2] - 1, x + this.random.nextInt(3) - 1));
            }
        }
    }

    void fill(int[] f, int[] colour, int jitter) {
        for (int y = 0; y < f[3]; y++) {
            for (int x = 0; x < f[2]; x++) {
                this.dot(f, x, y, this.shade(colour, jitter));
            }
        }
    }

    void row(int[] f, int y, int[] colour) {
        for (int x = 0; x < f[2]; x++) {
            this.dot(f, x, y, this.shade(colour, 6));
        }
    }

    void column(int[] f, int x, int[] colour) {
        for (int y = 0; y < f[3]; y++) {
            this.dot(f, x, y, this.shade(colour, 6));
        }
    }

    // Holes torn in a face (cloth): see-through.
    void tear(int[] f, int count) {
        for (int k = 0; k < count; k++) {
            int x = this.random.nextInt(f[2]);
            int y = this.random.nextInt(f[3]);
            this.clear(f, x, y);
            if (this.random.nextBoolean()) {
                this.clear(f, x + 1, y);
            }
        }
    }

    // A ragged bottom edge: each column torn off a little higher or lower.
    void hem(int[] f, int most) {
        for (int x = 0; x < f[2]; x++) {
            int cut = this.random.nextInt(most + 1);
            for (int y = f[3] - cut; y < f[3]; y++) {
                this.clear(f, x, y);
            }
        }
    }

    int[] shade(int[] colour, int jitter) {
        int d = jitter == 0 ? 0 : this.random.nextInt(2 * jitter + 1) - jitter;
        return new int[] { colour[0] + d, colour[1] + d, colour[2] + d };
    }

    void dot(int[] f, int x, int y, int[] colour) {
        if (x < 0 || y < 0 || x >= f[2] || y >= f[3]) {
            return;
        }
        this.image.setRGB(f[0] + x, f[1] + y, argb(colour, 255));
    }

    void dot(int[] f, int x, int y, int[] colour, int alpha) {
        if (x < 0 || y < 0 || x >= f[2] || y >= f[3]) {
            return;
        }
        this.image.setRGB(f[0] + x, f[1] + y, argb(colour, alpha));
    }

    void clear(int[] f, int x, int y) {
        if (x < 0 || y < 0 || x >= f[2] || y >= f[3]) {
            return;
        }
        this.image.setRGB(f[0] + x, f[1] + y, 0);
    }

    static int argb(int[] c, int alpha) {
        return clamp(alpha) << 24 | clamp(c[0]) << 16 | clamp(c[1]) << 8 | clamp(c[2]);
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
