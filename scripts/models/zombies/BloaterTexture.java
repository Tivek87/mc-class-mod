import java.nio.file.Path;

// Paints the Bloater for the boxes of creature/bloater/client/BloaterModel: bloater.png (pale swollen green skin
// stretched tight over the belly with purple stretch marks, inflamed patches and veins, a heavy brow over tiny eyes, a
// slack jaw of broken teeth, torn trousers), bloater_eyes.png (its eyes glowing), bloater_glow.png (its boils and the
// veins round them glowing acid green, drawn over it as bright as it swells) and, for creature/bloater/client/
// SpitRenderer, bloater_spit.png (a slime core in a see-through shell). Keep the boxes here as the models build them.
// From the repository root, with Java 22 or newer: java scripts/models/zombies/BloaterTexture.java
public final class BloaterTexture {
    private static final int[] SKIN = { 128, 150, 96 };
    private static final int[] DARK = { 98, 120, 74 };
    private static final int[] ROT = { 120, 104, 70 };
    private static final int[] STRETCH = { 150, 116, 130 };
    private static final int[] SORE = { 168, 92, 74 };
    private static final int[] VEIN = { 84, 120, 70 };
    private static final int[] SOCKET = { 40, 44, 30 };
    private static final int[] GAP = { 40, 24, 22 };
    private static final int[] TOOTH = { 206, 196, 150 };
    private static final int[] NAIL = { 70, 64, 44 };
    private static final int[] CLOTH = { 74, 68, 58 };
    private static final int[] CLOTH_DARK = { 52, 48, 40 };
    private static final int[] BOIL = { 186, 212, 86 };
    private static final int[] BOIL_TOP = { 236, 242, 168 };
    private static final int[] BOIL_BASE = { 150, 82, 64 };
    private static final int[] EYE = { 230, 240, 90 };
    private static final int[] GLOW = { 150, 255, 70 };
    private static final int[] GLOW_CORE = { 230, 255, 170 };
    private static final int[] SLIME = { 140, 224, 56 };
    private static final int[] SLIME_LIGHT = { 200, 250, 120 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 8, 7, 8);
    private static final Paint.Box JAW = new Paint.Box(64, 0, 8, 3, 7);
    private static final Paint.Box BELLY = new Paint.Box(0, 16, 16, 12, 13);
    private static final Paint.Box CHEST = new Paint.Box(0, 42, 14, 7, 10);
    private static final Paint.Box BOIL_SMALL = new Paint.Box(96, 0, 3, 3, 3);
    private static final Paint.Box BOIL_BIG = new Paint.Box(96, 8, 4, 4, 4);
    private static final Paint.Box UPPER = new Paint.Box(0, 62, 6, 7, 6);
    private static final Paint.Box FOREARM = new Paint.Box(24, 62, 5, 6, 5);
    private static final Paint.Box HAND = new Paint.Box(44, 62, 5, 3, 6);
    private static final Paint.Box THIGH = new Paint.Box(0, 78, 6, 6, 6);
    private static final Paint.Box SHIN = new Paint.Box(24, 78, 5, 4, 5);
    private static final Paint.Box FOOT = new Paint.Box(44, 78, 6, 2, 7);
    private static final Paint.Box CORE = new Paint.Box(0, 0, 4, 4, 4);
    private static final Paint.Box SHELL = new Paint.Box(0, 8, 6, 6, 6);

    private BloaterTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        Paint body = new Paint(128, 128, 5150);
        body(body);
        body.save(folder, "bloater");
        Paint eyes = new Paint(128, 128, 1);
        int[] f = HEAD.face(Paint.Face.FRONT);
        eyes.dot(f, 2, 3, EYE);
        eyes.dot(f, 5, 3, EYE);
        eyes.save(folder, "bloater_eyes");
        Paint glow = new Paint(128, 128, 77);
        glow(glow);
        glow.save(folder, "bloater_glow");
        Paint spit = new Paint(32, 32, 9);
        spit.all(CORE, SLIME, 14);
        for (Paint.Face face : Paint.Face.values()) {
            int[] c = CORE.face(face);
            spit.dot(c, 1, 1, SLIME_LIGHT);
            spit.dot(c, 2, 1, SLIME_LIGHT);
            int[] s = SHELL.face(face);
            for (int y = 0; y < s[3]; y++) {
                for (int x = 0; x < s[2]; x++) {
                    boolean edge = x == 0 || y == 0 || x == s[2] - 1 || y == s[3] - 1;
                    spit.dot(s, x, y, spit.shade(edge ? SLIME : SLIME_LIGHT, 10), edge ? 150 : 90);
                }
            }
        }
        spit.save(folder, "bloater_spit");
        System.out.println("painted");
    }

    private static void body(Paint p) {
        for (Paint.Box box : new Paint.Box[] { HEAD, JAW, BELLY, CHEST, UPPER, FOREARM, HAND, SHIN }) {
            p.skin(box, SKIN, DARK, ROT);
        }
        // Its face: a heavy brow, tiny sunken eyes, two holes for a nose and a lip hanging over the jaw.
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 1, DARK);
        for (int x : new int[] { 1, 2, 3, 4, 5, 6 }) {
            p.dot(f, x, 2, DARK);
        }
        for (int x : new int[] { 1, 2, 5, 6 }) {
            p.dot(f, x, 3, SOCKET);
        }
        p.dot(f, 3, 5, SOCKET);
        p.dot(f, 4, 5, SOCKET);
        p.row(f, 6, ROT);
        p.dot(f, 0, 4, SORE);
        p.dot(f, 7, 1, SORE);
        int[] top = HEAD.face(Paint.Face.TOP);
        for (int k = 0; k < 6; k++) {
            p.dot(top, p.random.nextInt(8), p.random.nextInt(8), SORE);
        }
        // The slack jaw: broken teeth along its top and the dark of the mouth.
        int[] mouth = JAW.face(Paint.Face.TOP);
        p.fill(mouth, GAP, 6);
        int[] lip = JAW.face(Paint.Face.FRONT);
        for (int x = 0; x < 8; x++) {
            p.dot(lip, x, 0, x % 3 == 1 ? GAP : TOOTH);
            p.dot(mouth, x, 0, x % 3 == 1 ? GAP : TOOTH);
        }
        p.dot(lip, 2, 2, ROT);
        p.dot(lip, 6, 1, DARK);
        // The belly stretched tight: stretch marks round it, sores, veins, a navel pulled wide.
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT }) {
            int[] b = BELLY.face(face);
            p.veins(b, 3, VEIN);
            for (int k = 0; k < 5; k++) {
                int x = p.random.nextInt(b[2]);
                int y = 2 + p.random.nextInt(b[3] - 4);
                p.dot(b, x, y, STRETCH);
                p.dot(b, x + 1, y + 1, STRETCH);
                p.dot(b, x + 2, y + 1, STRETCH);
            }
            for (int k = 0; k < 3; k++) {
                sore(p, b, p.random.nextInt(b[2]), p.random.nextInt(b[3]));
            }
        }
        int[] belly = BELLY.face(Paint.Face.FRONT);
        p.dot(belly, 7, 8, GAP);
        p.dot(belly, 8, 8, SOCKET);
        p.dot(belly, 7, 9, SOCKET);
        int[] back = BELLY.face(Paint.Face.BACK);
        p.veins(back, 4, VEIN);
        int[] chest = CHEST.face(Paint.Face.FRONT);
        p.veins(chest, 2, VEIN);
        p.row(chest, 6, DARK);
        // Hands with dark broken nails.
        int[] hand = HAND.face(Paint.Face.FRONT);
        for (int x = 0; x < 5; x += 2) {
            p.dot(hand, x, 2, NAIL);
        }
        p.fill(HAND.face(Paint.Face.BOTTOM), DARK, 6);
        // Torn trousers stretched over the thighs, bare swollen shins, dirty feet.
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(THIGH.face(face), CLOTH, 7);
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                Paint.Face.BACK }) {
            int[] t = THIGH.face(face);
            p.row(t, 0, CLOTH_DARK);
            p.row(t, t[3] - 1, CLOTH_DARK);
            p.dot(t, 1 + p.random.nextInt(4), 2 + p.random.nextInt(3), SKIN);
            p.dot(t, 1 + p.random.nextInt(4), 2 + p.random.nextInt(3), SKIN);
        }
        p.skin(FOOT, DARK, ROT, SOCKET);
        // The boils: yellow-green pustules with a pale head and a raw base.
        for (Paint.Box boil : new Paint.Box[] { BOIL_SMALL, BOIL_BIG }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] b = boil.face(face);
                p.fill(b, BOIL, 10);
                p.row(b, b[3] - 1, BOIL_BASE);
                p.dot(b, b[2] / 2, b[3] / 2 - 1, BOIL_TOP);
            }
        }
    }

    private static void sore(Paint p, int[] f, int x, int y) {
        p.dot(f, x, y, SORE);
        p.dot(f, x + 1, y, p.shade(SORE, 10));
        p.dot(f, x, y + 1, p.shade(BOIL_BASE, 8));
    }

    // Only what glows: the boils, brightest at their heads, and the veins on the belly feeding them.
    private static void glow(Paint p) {
        for (Paint.Box boil : new Paint.Box[] { BOIL_SMALL, BOIL_BIG }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] b = boil.face(face);
                for (int y = 0; y < b[3]; y++) {
                    for (int x = 0; x < b[2]; x++) {
                        p.dot(b, x, y, p.shade(GLOW, 12), 200);
                    }
                }
                p.dot(b, b[2] / 2, b[3] / 2 - 1, GLOW_CORE, 255);
            }
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT }) {
            int[] b = BELLY.face(face);
            for (int k = 0; k < 3; k++) {
                int x = p.random.nextInt(b[2]);
                for (int y = p.random.nextInt(3); y < b[3] - 1; y++) {
                    p.dot(b, x, y, GLOW, 110);
                    x = Math.max(0, Math.min(b[2] - 1, x + p.random.nextInt(3) - 1));
                }
            }
        }
    }
}
