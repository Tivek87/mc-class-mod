import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

// Paints the Zombie Brute's 128 pixel picture (zombie_brute.png) and its glowing eyes (zombie_brute_eyes.png) for the
// boxes of creature/brute/client/BruteModel: rotting grey-green skin with veins, bruises and stitches, ribs bare down
// its left side, a torn shirt over its back, torn trousers held up with a rope, rags wound round its forearms, bone
// spikes, heavy knuckles and bare feet. Keep the boxes here as the model builds them. From the repository root, with
// Java 22 or newer: java scripts/models/zombie_brute/BruteTexture.java
public final class BruteTexture {
    private static final int SIZE = 128;
    private static final int[] SKIN = { 86, 118, 72 };
    private static final int[] SKIN_DARK = { 60, 86, 52 };
    private static final int[] ROT = { 84, 74, 54 };
    private static final int[] RAW = { 122, 58, 56 };
    private static final int[] BONE = { 214, 206, 178 };
    private static final int[] BONE_DARK = { 150, 140, 114 };
    private static final int[] CLOTH = { 58, 62, 98 };
    private static final int[] CLOTH_DARK = { 40, 42, 70 };
    private static final int[] SHIRT = { 52, 112, 120 };
    private static final int[] RAG = { 168, 156, 124 };
    private static final int[] ROPE = { 112, 84, 50 };
    private static final int[] GAP = { 34, 22, 20 };
    private static final int[] SOCKET = { 28, 34, 24 };
    private static final int[] STITCH = { 42, 30, 30 };
    private static final int[] VEIN = { 56, 82, 70 };
    private static final int[] BRUISE = { 92, 74, 92 };
    private static final int[] EYE = { 255, 74, 30 };
    private static final int[] EYE_CORE = { 255, 178, 96 };

    private enum Face { TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK }

    private record Box(int u, int v, int w, int h, int d) {
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

    private static final Box HEAD = new Box(0, 0, 8, 8, 8);
    private static final Box BROW = new Box(32, 0, 9, 2, 2);
    private static final Box JAW = new Box(54, 0, 7, 3, 6);
    private static final Box CHEST = new Box(0, 16, 16, 10, 10);
    private static final Box BELLY = new Box(52, 16, 12, 6, 8);
    private static final Box HUMP = new Box(92, 16, 10, 4, 3);
    private static final Box SPIKE = new Box(124, 16, 1, 3, 1);
    private static final Box UPPER = new Box(0, 36, 8, 9, 8);
    private static final Box FOREARM = new Box(36, 36, 7, 8, 7);
    private static final Box FIST = new Box(64, 36, 7, 5, 8);
    private static final Box HORN = new Box(98, 36, 2, 3, 2);
    private static final Box LEG = new Box(0, 56, 6, 11, 6);
    private static final Box FOOT = new Box(24, 56, 7, 3, 9);

    private final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    private final Random random = new Random(4242);

    private BruteTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path textures = Files.createDirectories(Path.of(args.length > 0 ? args[0]
                : "src/main/resources/assets/welcomescreen").resolve("textures/entity"));
        BruteTexture body = new BruteTexture();
        body.paint();
        ImageIO.write(body.image, "png", textures.resolve("zombie_brute.png").toFile());
        ImageIO.write(eyes(), "png", textures.resolve("zombie_brute_eyes.png").toFile());
        System.out.println("painted");
    }

    private void paint() {
        for (Box box : new Box[] { HEAD, BROW, JAW, CHEST, BELLY, HUMP, UPPER, FOREARM, FIST, LEG, FOOT }) {
            for (Face face : Face.values()) {
                this.skin(box.face(face));
            }
        }
        for (Box box : new Box[] { SPIKE, HORN }) {
            for (Face face : Face.values()) {
                int[] f = box.face(face);
                this.fill(f, BONE, 10);
                this.row(f, 0, face == Face.TOP ? BONE : BONE_DARK);
            }
            this.fill(box.face(Face.BOTTOM), BONE_DARK, 6);
        }
        this.head();
        this.jaw();
        this.trunk();
        this.arms();
        this.legs();
    }

    private void head() {
        int[] top = HEAD.face(Face.TOP);
        for (int k = 0; k < 14; k++) {
            this.dot(top, this.random.nextInt(8), this.random.nextInt(8), SKIN_DARK);
        }
        this.dot(top, 2, 5, RAW);
        this.dot(top, 3, 5, RAW);
        int[] f = HEAD.face(Face.FRONT);
        this.row(f, 2, SKIN_DARK);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            this.dot(f, x, 3, SOCKET);
            this.dot(f, x, 4, SKIN_DARK);
        }
        this.dot(f, 3, 5, SOCKET);
        this.dot(f, 4, 5, SOCKET);
        this.row(f, 6, SKIN_DARK);
        for (int x = 0; x < 8; x++) {
            this.dot(f, x, 7, x % 2 == 0 || x == 7 ? GAP : BONE);
        }
        this.dot(f, 6, 1, RAW);
        this.dot(f, 7, 2, RAW);
        int[] side = HEAD.face(Face.RIGHT);
        for (int k = 0; k < 5; k++) {
            this.dot(side, 1 + k, 2 + k / 2, STITCH);
        }
        this.dot(HEAD.face(Face.LEFT), 3, 4, ROT);
        this.dot(HEAD.face(Face.LEFT), 4, 4, ROT);
        this.fill(BROW.face(Face.FRONT), SKIN_DARK, 8);
        this.row(BROW.face(Face.FRONT), 1, SOCKET);
    }

    private void jaw() {
        this.fill(JAW.face(Face.TOP), GAP, 6);
        int[] top = JAW.face(Face.TOP);
        for (int x = 0; x < 7; x++) {
            this.dot(top, x, 0, x % 2 == 0 ? BONE : GAP);
        }
        int[] f = JAW.face(Face.FRONT);
        for (int x = 0; x < 7; x++) {
            this.dot(f, x, 0, x % 2 == 1 ? BONE : GAP);
        }
        this.dot(f, 1, 2, ROT);
        this.fill(JAW.face(Face.BOTTOM), SKIN_DARK, 8);
    }

    private void trunk() {
        int[] f = CHEST.face(Face.FRONT);
        this.row(f, 0, SKIN, 18);
        this.row(f, 4, SKIN_DARK);
        for (int y = 2; y <= 8; y++) {
            for (int x = 10; x <= 14; x++) {
                this.dot(f, x, y, y % 2 == 0 ? (x == 10 || x == 14 ? BONE_DARK : BONE) : GAP);
            }
        }
        for (int x = 2; x <= 6; x++) {
            this.dot(f, x, 6, STITCH);
        }
        for (int x = 2; x <= 6; x += 2) {
            this.dot(f, x, 5, STITCH);
            this.dot(f, x, 7, STITCH);
        }
        this.dot(f, 7, 8, RAW);
        this.dot(f, 8, 8, RAW);
        int[] left = CHEST.face(Face.LEFT);
        for (int y = 2; y <= 8; y += 2) {
            for (int x = 0; x < 4; x++) {
                this.dot(left, x, y, BONE);
            }
        }
        int[] back = CHEST.face(Face.BACK);
        this.shirt(back, 0.18);
        this.shirt(CHEST.face(Face.RIGHT), 0.45);
        int[] belly = BELLY.face(Face.FRONT);
        this.dot(belly, 6, 2, SKIN_DARK);
        this.dot(belly, 3, 1, ROT);
        this.dot(belly, 4, 1, ROT);
        for (Face face : new Face[] { Face.FRONT, Face.RIGHT, Face.LEFT, Face.BACK }) {
            int[] g = BELLY.face(face);
            this.row(g, g[3] - 1, ROPE);
            this.row(g, g[3] - 2, CLOTH_DARK);
        }
        this.shirt(BELLY.face(Face.BACK), 0.3);
        int[] hump = HUMP.face(Face.TOP);
        for (int x = 1; x < 10; x += 3) {
            this.dot(hump, x, 1, BONE_DARK);
        }
        int[] hb = HUMP.face(Face.BACK);
        for (int y = 0; y < 4; y += 2) {
            this.dot(hb, 4, y, BONE_DARK);
            this.dot(hb, 5, y, BONE_DARK);
        }
        this.shirt(hb, 0.35);
    }

    private void arms() {
        for (Face face : new Face[] { Face.FRONT, Face.RIGHT, Face.LEFT, Face.BACK }) {
            this.veins(UPPER.face(face), 2);
            int[] g = FOREARM.face(face);
            this.veins(g, 1);
            for (int y : new int[] { 2, 3, 6 }) {
                this.row(g, y, RAG, 14);
            }
            this.row(g, 1, SKIN_DARK);
            for (int x = 0; x < g[2]; x += 3) {
                this.dot(g, x, 4, RAG);
            }
            int[] fist = FIST.face(face);
            if (this.random.nextBoolean()) {
                this.dot(fist, 1 + this.random.nextInt(Math.max(1, fist[2] - 2)), 2 + this.random.nextInt(3), BRUISE);
            }
        }
        this.dot(UPPER.face(Face.FRONT), 4, 5, RAW);
        this.dot(UPPER.face(Face.FRONT), 5, 6, RAW);
        int[] knuckles = FIST.face(Face.FRONT);
        this.row(knuckles, 0, SKIN, 22);
        for (int x : new int[] { 1, 3, 5 }) {
            for (int y = 1; y < 5; y++) {
                this.dot(knuckles, x, y, GAP);
            }
        }
        this.row(knuckles, 4, ROT);
        int[] under = FIST.face(Face.BOTTOM);
        this.fill(under, SKIN_DARK, 8);
        for (int x = 1; x < 8; x += 2) {
            this.dot(under, x, 0, ROT);
        }
    }

    private void legs() {
        for (Face face : new Face[] { Face.FRONT, Face.RIGHT, Face.LEFT, Face.BACK }) {
            int[] g = LEG.face(face);
            for (int x = 0; x < g[2]; x++) {
                int ends = 5 + this.random.nextInt(3);
                for (int y = 0; y < ends; y++) {
                    this.dot(g, x, y, this.shade(y == ends - 1 ? CLOTH_DARK : CLOTH, 8));
                }
            }
            if (face == Face.FRONT) {
                this.dot(g, 2, 2, SKIN);
                this.dot(g, 3, 2, SKIN_DARK);
            }
            this.dot(g, 1 + this.random.nextInt(4), 8 + this.random.nextInt(2), BRUISE);
        }
        this.fill(LEG.face(Face.TOP), CLOTH_DARK, 6);
        int[] toes = FOOT.face(Face.FRONT);
        this.fill(toes, SKIN_DARK, 8);
        for (int x = 1; x < 7; x += 2) {
            this.dot(toes, x, 0, GAP);
            this.dot(toes, x, 1, GAP);
        }
        this.row(toes, 2, ROT);
        int[] top = FOOT.face(Face.TOP);
        for (int x = 1; x < 7; x += 2) {
            this.dot(top, x, 7, GAP);
            this.dot(top, x, 8, GAP);
        }
        this.fill(FOOT.face(Face.BOTTOM), ROT, 6);
    }

    // A torn shirt over a face: cloth with holes where the skin shows through, `holes` of it.
    private void shirt(int[] f, double holes) {
        for (int y = 0; y < f[3]; y++) {
            for (int x = 0; x < f[2]; x++) {
                if (this.random.nextDouble() >= holes) {
                    this.dot(f, x, y, this.shade(y % 3 == 2 ? new int[] { 36, 82, 90 } : SHIRT, 8));
                }
            }
        }
    }

    // Veins winding down a face.
    private void veins(int[] f, int count) {
        for (int k = 0; k < count; k++) {
            int x = this.random.nextInt(f[2]);
            for (int y = this.random.nextInt(3); y < f[3] - 1; y++) {
                this.dot(f, x, y, VEIN);
                x = Math.max(0, Math.min(f[2] - 1, x + this.random.nextInt(3) - 1));
            }
        }
    }

    // Skin, mottled: darker blotches, a few patches of rot.
    private void skin(int[] f) {
        this.fill(f, SKIN, 9);
        int blotches = f[2] * f[3] / 12;
        for (int k = 0; k < blotches; k++) {
            int[] colour = this.random.nextInt(5) == 0 ? ROT : SKIN_DARK;
            this.dot(f, this.random.nextInt(f[2]), this.random.nextInt(f[3]), this.shade(colour, 6));
        }
    }

    private void fill(int[] f, int[] colour, int jitter) {
        for (int y = 0; y < f[3]; y++) {
            for (int x = 0; x < f[2]; x++) {
                this.dot(f, x, y, this.shade(colour, jitter));
            }
        }
    }

    private void row(int[] f, int y, int[] colour) {
        this.row(f, y, colour, 6);
    }

    private void row(int[] f, int y, int[] colour, int jitter) {
        for (int x = 0; x < f[2]; x++) {
            this.dot(f, x, y, this.shade(colour, jitter));
        }
    }

    private int[] shade(int[] colour, int jitter) {
        int d = jitter == 0 ? 0 : this.random.nextInt(2 * jitter + 1) - jitter;
        return new int[] { colour[0] + d, colour[1] + d, colour[2] + d };
    }

    private void dot(int[] f, int x, int y, int[] colour) {
        if (x < 0 || y < 0 || x >= f[2] || y >= f[3]) {
            return;
        }
        this.image.setRGB(f[0] + x, f[1] + y, argb(colour));
    }

    private static int argb(int[] c) {
        return 0xFF000000 | clamp(c[0]) << 16 | clamp(c[1]) << 8 | clamp(c[2]);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // Only its eyes, glowing in the sockets of its face.
    private static BufferedImage eyes() {
        BufferedImage eyes = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int[] f = HEAD.face(Face.FRONT);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            boolean inner = x == 2 || x == 5;
            eyes.setRGB(f[0] + x, f[1] + 3, argb(inner ? EYE_CORE : EYE));
        }
        return eyes;
    }
}
