import java.nio.file.Path;

// Paints the crawlers for the boxes of creature/lurker/client/LurkerModel: lurker.png (ash-black skin, grey where it
// flakes, ribs and spine pressing through, sunken black sockets, long bone claws) with lurker_eyes.png (white pupils
// glowing in them), and the Screamer's ferals in the same boxes, feral.png (starved grey-green skin, torn rags,
// broken nails) with feral_eyes.png (dim yellow). Keep the boxes here as the model builds them. From the repository
// root, with Java 22 or newer: java scripts/models/zombies/LurkerTexture.java
public final class LurkerTexture {
    private static final Paint.Box HEAD = new Paint.Box(0, 0, 7, 6, 8);
    private static final Paint.Box JAW = new Paint.Box(30, 0, 6, 2, 6);
    private static final Paint.Box CHEST = new Paint.Box(0, 16, 8, 9, 5);
    private static final Paint.Box WAIST = new Paint.Box(26, 16, 6, 7, 4);
    private static final Paint.Box SPINE = new Paint.Box(46, 16, 2, 14, 1);
    private static final Paint.Box UPPER = new Paint.Box(0, 32, 3, 11, 3);
    private static final Paint.Box FOREARM = new Paint.Box(12, 32, 2, 9, 2);
    private static final Paint.Box HAND = new Paint.Box(20, 32, 3, 2, 3);
    private static final Paint.Box CLAW = new Paint.Box(32, 32, 1, 6, 1);
    private static final Paint.Box THIGH = new Paint.Box(40, 32, 4, 11, 4);
    private static final Paint.Box SHIN = new Paint.Box(56, 32, 3, 10, 3);
    private static final Paint.Box FOOT = new Paint.Box(68, 32, 3, 2, 5);
    private static final Paint.Box[] SKIN = { HEAD, JAW, CHEST, WAIST, UPPER, FOREARM, HAND, THIGH, SHIN, FOOT };

    private static final int[] BONE = { 222, 214, 190 };
    private static final int[] BONE_DARK = { 160, 150, 124 };
    private static final int[] GAP = { 14, 10, 10 };

    // A crawler's colours: skin, its dark blotches, where it flakes or rots, ribs and veins, rags, and its eyes.
    private record Look(int[] skin, int[] dark, int[] rot, int[] rib, int[] rag, int[] eye, boolean ragged) {
    }

    private static final Look LURKER = new Look(new int[] { 36, 34, 34 }, new int[] { 22, 20, 21 },
            new int[] { 66, 62, 60 }, new int[] { 74, 70, 68 }, null, new int[] { 255, 255, 255 }, false);
    private static final Look FERAL = new Look(new int[] { 104, 116, 86 }, new int[] { 80, 92, 66 },
            new int[] { 112, 96, 70 }, new int[] { 136, 140, 112 }, new int[] { 92, 82, 66 },
            new int[] { 232, 210, 120 }, true);

    private LurkerTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        paint(folder, "lurker", LURKER, 6606);
        paint(folder, "feral", FERAL, 3131);
        System.out.println("painted");
    }

    private static void paint(Path folder, String name, Look look, long seed) throws Exception {
        Paint p = new Paint(128, 64, seed);
        for (Paint.Box box : SKIN) {
            p.skin(box, look.skin, look.dark, look.rot);
        }
        face(p, look);
        trunk(p, look);
        limbs(p, look);
        p.save(folder, name);
        Paint eyes = new Paint(128, 64, 1);
        int[] f = HEAD.face(Paint.Face.FRONT);
        eyes.dot(f, 2, 2, look.eye);
        eyes.dot(f, 4, 2, look.eye);
        eyes.save(folder, name + "_eyes");
    }

    // Deep black sockets under a jutting brow, a slit for a nose, a lipless row of teeth; the jaw's teeth and the dark
    // of the mouth.
    private static void face(Paint p, Look look) {
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 1, look.dark);
        for (int x : new int[] { 1, 2, 4, 5 }) {
            p.dot(f, x, 2, GAP);
        }
        p.dot(f, 3, 3, GAP);
        p.dot(f, 2, 4, look.dark);
        p.dot(f, 4, 4, look.dark);
        for (int x = 0; x < 7; x++) {
            p.dot(f, x, 5, x % 2 == 0 ? BONE : GAP);
        }
        int[] jaw = JAW.face(Paint.Face.FRONT);
        int[] mouth = JAW.face(Paint.Face.TOP);
        p.fill(mouth, GAP, 4);
        for (int x = 0; x < 6; x++) {
            p.dot(jaw, x, 0, x % 2 == 1 ? BONE : GAP);
            p.dot(mouth, x, 0, x % 2 == 1 ? BONE : GAP);
        }
        int[] top = HEAD.face(Paint.Face.TOP);
        for (int k = 0; k < 5; k++) {
            p.dot(top, p.random.nextInt(7), p.random.nextInt(8), look.rot);
        }
        for (Paint.Face side : new Paint.Face[] { Paint.Face.RIGHT, Paint.Face.LEFT }) {
            int[] s = HEAD.face(side);
            p.dot(s, 2, 2, look.dark);
            p.dot(s, 3, 3, look.dark);
        }
    }

    // Ribs pressing through the chest and sides, a sunken belly, vertebrae down its back; a feral wears a torn rag.
    private static void trunk(Paint p, Look look) {
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                Paint.Face.BACK }) {
            int[] c = CHEST.face(face);
            for (int y = 2; y < c[3] - 1; y += 2) {
                for (int x = 0; x < c[2]; x++) {
                    boolean middle = face == Paint.Face.FRONT && (x == c[2] / 2 - 1 || x == c[2] / 2);
                    p.dot(c, x, y, middle ? look.dark : p.shade(look.rib, 6));
                }
            }
        }
        int[] belly = WAIST.face(Paint.Face.FRONT);
        p.column(belly, 2, look.dark);
        p.column(belly, 3, look.dark);
        for (Paint.Face face : Paint.Face.values()) {
            p.fill(SPINE.face(face), BONE_DARK, 8);
        }
        int[] spine = SPINE.face(Paint.Face.BACK);
        for (int y = 0; y < 14; y += 2) {
            p.dot(spine, 0, y, BONE);
            p.dot(spine, 1, y, BONE);
        }
        if (look.ragged) {
            for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                    Paint.Face.BACK }) {
                int[] w = WAIST.face(face);
                for (int y = 2; y < w[3]; y++) {
                    p.row(new int[] { w[0], w[1] + y, w[2], 1 }, 0, look.rag);
                }
                p.dot(w, p.random.nextInt(w[2]), w[3] - 1, look.skin);
            }
        }
    }

    // Thin limbs, knobby at the joints; bone claws past the hands; long-toed, clawed feet.
    private static void limbs(Paint p, Look look) {
        for (Paint.Face face : new Paint.Face[] { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
                Paint.Face.BACK }) {
            p.row(UPPER.face(face), 10, look.rib);
            p.row(THIGH.face(face), 10, look.rib);
            p.veins(FOREARM.face(face), 1, look.dark);
            p.veins(SHIN.face(face), 1, look.dark);
            if (look.ragged) {
                int[] t = THIGH.face(face);
                for (int y = 0; y < 6; y++) {
                    p.row(new int[] { t[0], t[1] + y, t[2], 1 }, 0, look.rag);
                }
                p.dot(t, p.random.nextInt(t[2]), 5, look.skin);
            }
        }
        for (Paint.Face face : Paint.Face.values()) {
            int[] c = CLAW.face(face);
            p.fill(c, BONE, 8);
            p.dot(c, 0, 0, BONE_DARK);
            p.dot(c, 0, c[3] - 1, look.ragged ? look.dark : BONE_DARK);
        }
        int[] hand = HAND.face(Paint.Face.FRONT);
        p.row(hand, 1, look.ragged ? look.dark : BONE_DARK);
        int[] foot = FOOT.face(Paint.Face.TOP);
        for (int x = 0; x < 3; x++) {
            p.dot(foot, x, 0, BONE_DARK);
        }
    }
}
