import java.nio.file.Path;

// Paints the Screamer for the boxes of creature/screamer/client/ScreamerModel: screamer.png (grey, starved skin
// stretched over the skull and ribs, a long jaw of yellow teeth, the raw throat behind it, a torn and bloodied shirt,
// rags round the shoulders and ragged trousers, see-through where they are torn) and screamer_eyes.png (small red eyes
// glowing in the sockets). Keep the boxes here as the model builds them. From the repository root, with Java 22 or
// newer: java scripts/models/zombies/ScreamerTexture.java
public final class ScreamerTexture {
    private static final int[] SKIN = { 132, 124, 106 };
    private static final int[] DARK = { 100, 92, 78 };
    private static final int[] ROT = { 112, 88, 72 };
    private static final int[] RIB = { 156, 148, 128 };
    private static final int[] GAP = { 26, 16, 16 };
    private static final int[] TOOTH = { 214, 200, 140 };
    private static final int[] RAW = { 128, 34, 38 };
    private static final int[] RAW_DARK = { 72, 18, 22 };
    private static final int[] SHIRT = { 168, 158, 134 };
    private static final int[] SHIRT_DARK = { 128, 118, 98 };
    private static final int[] BLOOD = { 104, 26, 26 };
    private static final int[] PANTS = { 82, 68, 54 };
    private static final int[] PANTS_DARK = { 60, 50, 40 };
    private static final int[] EYE = { 255, 66, 52 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 7, 6, 7);
    private static final Paint.Box PALATE = new Paint.Box(28, 0, 6, 2, 4);
    private static final Paint.Box JAW = new Paint.Box(48, 0, 6, 2, 5);
    private static final Paint.Box THROAT = new Paint.Box(72, 0, 5, 1, 4);
    private static final Paint.Box CHEST = new Paint.Box(0, 16, 7, 7, 4);
    private static final Paint.Box WAIST = new Paint.Box(22, 16, 6, 5, 3);
    private static final Paint.Box SHIRT_BOX = new Paint.Box(40, 16, 7, 10, 4);
    private static final Paint.Box ARM = new Paint.Box(0, 32, 2, 13, 2);
    private static final Paint.Box HAND = new Paint.Box(8, 32, 2, 3, 2);
    private static final Paint.Box SLEEVE = new Paint.Box(16, 32, 3, 5, 3);
    private static final Paint.Box LEG = new Paint.Box(0, 48, 3, 12, 3);
    private static final Paint.Box PANTS_BOX = new Paint.Box(12, 48, 3, 7, 3);
    private static final Paint.Face[] AROUND = { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
            Paint.Face.BACK };

    private ScreamerTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        Paint p = new Paint(128, 64, 7337);
        for (Paint.Box box : new Paint.Box[] { HEAD, PALATE, JAW, CHEST, WAIST, ARM, HAND, LEG }) {
            p.skin(box, SKIN, DARK, ROT);
        }
        head(p);
        trunk(p);
        clothes(p);
        p.save(folder, "screamer");
        Paint eyes = new Paint(128, 64, 1);
        int[] f = HEAD.face(Paint.Face.FRONT);
        eyes.dot(f, 2, 2, EYE);
        eyes.dot(f, 4, 2, EYE);
        eyes.save(folder, "screamer_eyes");
        System.out.println("painted");
    }

    // Sunken sockets, a gash of a nose, the upper teeth bared; under the skull the palate, behind the jaw's teeth the
    // dark of the mouth, and the throat raw red.
    private static void head(Paint p) {
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 1, DARK);
        for (int x : new int[] { 1, 2, 4, 5 }) {
            p.dot(f, x, 2, GAP);
        }
        p.dot(f, 3, 3, GAP);
        p.dot(f, 1, 4, DARK);
        p.dot(f, 5, 4, DARK);
        for (int x = 0; x < 7; x++) {
            p.dot(f, x, 5, x == 0 || x == 6 ? DARK : x % 2 == 1 ? TOOTH : p.shade(TOOTH, 20));
        }
        int[] under = HEAD.face(Paint.Face.BOTTOM);
        p.fill(under, RAW_DARK, 6);
        for (int x = 0; x < 7; x++) {
            p.dot(under, x, 0, TOOTH);
        }
        p.fill(PALATE.face(Paint.Face.BOTTOM), RAW_DARK, 6);
        p.fill(PALATE.face(Paint.Face.FRONT), RAW, 8);
        int[] mouth = JAW.face(Paint.Face.TOP);
        p.fill(mouth, RAW_DARK, 6);
        int[] chin = JAW.face(Paint.Face.FRONT);
        for (int x = 0; x < 6; x++) {
            p.dot(chin, x, 0, x % 2 == 0 ? TOOTH : GAP);
            p.dot(mouth, x, 0, x % 2 == 0 ? TOOTH : GAP);
        }
        for (Paint.Face side : new Paint.Face[] { Paint.Face.RIGHT, Paint.Face.LEFT }) {
            int[] j = JAW.face(side);
            p.dot(j, 0, 0, TOOTH);
            p.dot(j, 1, 0, TOOTH);
        }
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(THROAT.face(face), RAW, 10);
        }
        p.dot(THROAT.face(Paint.Face.TOP), 2, 1, RAW_DARK);
        int[] top = HEAD.face(Paint.Face.TOP);
        for (int k = 0; k < 6; k++) {
            p.dot(top, p.random.nextInt(7), p.random.nextInt(7), ROT);
        }
    }

    private static void trunk(Paint p) {
        for (Paint.Face face : AROUND) {
            int[] c = CHEST.face(face);
            for (int y = 1; y < c[3]; y += 2) {
                p.row(c, y, RIB);
            }
        }
        p.column(WAIST.face(Paint.Face.FRONT), 3, DARK);
    }

    // A torn shirt hanging off it, bloodied down the front; rags round the shoulders; trousers torn off above the
    // knee.
    private static void clothes(Paint p) {
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(SHIRT_BOX.face(face), SHIRT, 9);
            p.fill(SLEEVE.face(face), SHIRT_DARK, 9);
            p.fill(PANTS_BOX.face(face), PANTS, 8);
        }
        for (Paint.Face face : AROUND) {
            int[] s = SHIRT_BOX.face(face);
            p.hem(s, 3);
            p.tear(s, s[2] * s[3] / 9);
            p.row(s, 0, SHIRT_DARK);
            int[] sleeve = SLEEVE.face(face);
            p.hem(sleeve, 2);
            p.tear(sleeve, 2);
            int[] pants = PANTS_BOX.face(face);
            p.row(pants, 0, PANTS_DARK);
            p.hem(pants, 2);
            p.tear(pants, 2);
        }
        int[] front = SHIRT_BOX.face(Paint.Face.FRONT);
        for (int k = 0; k < 9; k++) {
            int x = 1 + p.random.nextInt(4);
            int y = p.random.nextInt(6);
            if ((p.image.getRGB(front[0] + x, front[1] + y) >>> 24) != 0) {
                p.dot(front, x, y, p.shade(BLOOD, 10));
            }
        }
        p.fill(SHIRT_BOX.face(Paint.Face.TOP), SHIRT_DARK, 8);
        int[] top = SHIRT_BOX.face(Paint.Face.TOP);
        for (int x = 2; x < 5; x++) {
            for (int y = 1; y < 3; y++) {
                p.clear(top, x, y);
            }
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.TOP, Paint.Face.BOTTOM }) {
            int[] b = SHIRT_BOX.face(face);
            if (face == Paint.Face.BOTTOM) {
                for (int y = 0; y < b[3]; y++) {
                    for (int x = 0; x < b[2]; x++) {
                        p.clear(b, x, y);
                    }
                }
            }
            int[] pb = PANTS_BOX.face(face);
            for (int y = 0; y < pb[3]; y++) {
                for (int x = 0; x < pb[2]; x++) {
                    p.clear(pb, x, y);
                }
            }
            int[] sb = SLEEVE.face(face);
            for (int y = 0; y < sb[3]; y++) {
                for (int x = 0; x < sb[2]; x++) {
                    p.clear(sb, x, y);
                }
            }
        }
        int[] hand = HAND.face(Paint.Face.FRONT);
        p.row(hand, 2, DARK);
    }
}
