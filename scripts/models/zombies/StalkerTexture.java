import java.nio.file.Path;

// Paints the Stalker for the boxes of creature/stalker/client/StalkerModel: stalker.png (skin dark and cracked like
// old bark, grown over with grey-green lichen, its ribs showing; a long skull with deep black sockets and a lipless
// mouth of thin teeth; horns like dead twigs; a ridge of bark plates down its spine; long bone-white claws) and
// stalker_eyes.png (two small pale eyes far back in the sockets). Keep the boxes here as the model builds them. From
// the repository root, with Java 22 or newer: java scripts/models/zombies/StalkerTexture.java
public final class StalkerTexture {
    private static final int[] SKIN = { 60, 54, 47 };
    private static final int[] DARK = { 38, 34, 30 };
    private static final int[] LICHEN = { 84, 98, 74 };
    private static final int[] CRACK = { 22, 19, 17 };
    private static final int[] BARK = { 92, 78, 62 };
    private static final int[] RIB = { 86, 80, 70 };
    private static final int[] SOCKET = { 6, 6, 8 };
    private static final int[] MOUTH = { 52, 14, 16 };
    private static final int[] TOOTH = { 132, 124, 104 };
    private static final int[] BONE = { 224, 218, 198 };
    private static final int[] BONE_DARK = { 168, 160, 140 };
    private static final int[] TWIG = { 70, 58, 46 };
    private static final int[] EYE = { 120, 190, 160 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 6, 7, 6);
    private static final Paint.Box JAW = new Paint.Box(24, 0, 4, 2, 4);
    private static final Paint.Box HORN = new Paint.Box(40, 0, 1, 6, 1);
    private static final Paint.Box TWIG_BOX = new Paint.Box(44, 0, 1, 3, 1);
    private static final Paint.Box CHEST = new Paint.Box(0, 16, 7, 8, 3);
    private static final Paint.Box WAIST = new Paint.Box(20, 16, 5, 5, 2);
    private static final Paint.Box SPINE = new Paint.Box(34, 16, 1, 10, 1);
    private static final Paint.Box ARM = new Paint.Box(0, 32, 2, 17, 2);
    private static final Paint.Box HAND = new Paint.Box(8, 32, 3, 2, 3);
    private static final Paint.Box CLAW = new Paint.Box(20, 32, 1, 5, 1);
    private static final Paint.Box LEG = new Paint.Box(24, 32, 2, 15, 2);
    private static final Paint.Box THIGH = new Paint.Box(32, 32, 3, 6, 3);
    private static final Paint.Box SHOULDER = new Paint.Box(44, 32, 3, 3, 3);
    private static final Paint.Face[] AROUND = { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
            Paint.Face.BACK };

    private StalkerTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        Paint p = new Paint(128, 64, 4417);
        for (Paint.Box box : new Paint.Box[] { HEAD, JAW, CHEST, WAIST, ARM, HAND, LEG, THIGH, SHOULDER }) {
            p.skin(box, SKIN, DARK, LICHEN);
            for (Paint.Face face : AROUND) {
                int[] f = box.face(face);
                p.veins(f, Math.max(1, f[2] / 2), CRACK);
            }
        }
        head(p);
        trunk(p);
        limbs(p);
        p.save(folder, "stalker");
        Paint eyes = new Paint(128, 64, 1);
        int[] f = HEAD.face(Paint.Face.FRONT);
        eyes.dot(f, 1, 2, EYE);
        eyes.dot(f, 4, 2, EYE);
        eyes.save(folder, "stalker_eyes");
        System.out.println("painted");
    }

    // Deep black sockets under a heavy brow, no nose but two slits, a lipless mouth of thin teeth; the jaw under it
    // the same, and horns of dead twigs.
    private static void head(Paint p) {
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 1, DARK);
        for (int x : new int[] { 0, 1, 4, 5 }) {
            p.dot(f, x, 2, SOCKET);
            p.dot(f, x, 3, SOCKET);
        }
        p.dot(f, 1, 4, DARK);
        p.dot(f, 4, 4, DARK);
        p.dot(f, 2, 4, CRACK);
        p.dot(f, 3, 4, CRACK);
        for (int x = 0; x < 6; x++) {
            p.dot(f, x, 5, x % 2 == 1 ? TOOTH : MOUTH);
            p.dot(f, x, 6, MOUTH);
        }
        int[] under = HEAD.face(Paint.Face.BOTTOM);
        p.fill(under, MOUTH, 6);
        int[] chin = JAW.face(Paint.Face.FRONT);
        for (int x = 0; x < 4; x++) {
            p.dot(chin, x, 0, TOOTH);
        }
        p.fill(JAW.face(Paint.Face.TOP), MOUTH, 6);
        for (int x = 0; x < 4; x++) {
            p.dot(JAW.face(Paint.Face.TOP), x, 3, TOOTH);
        }
        int[] top = HEAD.face(Paint.Face.TOP);
        for (int k = 0; k < 8; k++) {
            p.dot(top, p.random.nextInt(6), p.random.nextInt(6), p.shade(LICHEN, 8));
        }
        for (Paint.Box box : new Paint.Box[] { HORN, TWIG_BOX }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] h = box.face(face);
                p.fill(h, TWIG, 10);
                if (h[3] > 2) {
                    p.dot(h, 0, p.random.nextInt(h[3]), CRACK);
                }
            }
            p.fill(box.face(Paint.Face.TOP), p.shade(BARK, 0), 6);
        }
    }

    // Ribs showing through cracked skin, lichen in its hollows, and the bark plates down its spine.
    private static void trunk(Paint p) {
        int[] front = CHEST.face(Paint.Face.FRONT);
        for (int y = 1; y < 7; y += 2) {
            for (int x = 0; x < front[2]; x++) {
                if (x != 3) {
                    p.dot(front, x, y, p.shade(RIB, 6));
                }
            }
        }
        p.column(front, 3, DARK);
        for (Paint.Face side : new Paint.Face[] { Paint.Face.RIGHT, Paint.Face.LEFT }) {
            int[] s = CHEST.face(side);
            for (int y = 1; y < 7; y += 2) {
                p.row(s, y, RIB);
            }
        }
        int[] belly = WAIST.face(Paint.Face.FRONT);
        p.column(belly, 2, CRACK);
        for (int k = 0; k < 4; k++) {
            p.dot(belly, p.random.nextInt(5), p.random.nextInt(5), p.shade(LICHEN, 10));
        }
        for (Paint.Face face : Paint.Face.values()) {
            int[] s = SPINE.face(face);
            p.fill(s, BARK, 10);
            for (int y = 1; y < s[3]; y += 3) {
                p.row(s, y, CRACK);
            }
        }
        int[] back = CHEST.face(Paint.Face.BACK);
        for (int y = 0; y < back[3]; y++) {
            p.dot(back, 3, y, p.shade(BARK, 8));
        }
    }

    // Lichen creeping up the shins and forearms, the knuckles dark and the claws bone-white, darker at the root.
    private static void limbs(Paint p) {
        for (Paint.Face face : AROUND) {
            int[] arm = ARM.face(face);
            for (int k = 0; k < 6; k++) {
                p.dot(arm, p.random.nextInt(arm[2]), 9 + p.random.nextInt(7), p.shade(LICHEN, 10));
            }
            int[] leg = LEG.face(face);
            for (int k = 0; k < 6; k++) {
                p.dot(leg, p.random.nextInt(leg[2]), 7 + p.random.nextInt(8), p.shade(LICHEN, 10));
            }
            p.row(leg, leg[3] - 1, CRACK);
            p.row(HAND.face(face), 1, CRACK);
            int[] claw = CLAW.face(face);
            p.fill(claw, BONE, 6);
            p.dot(claw, 0, 0, BONE_DARK);
            p.dot(claw, 0, 1, BONE_DARK);
            int[] shoulder = SHOULDER.face(face);
            p.fill(shoulder, BARK, 10);
            p.row(shoulder, 1, CRACK);
            int[] thigh = THIGH.face(face);
            p.veins(thigh, 2, CRACK);
        }
        p.fill(CLAW.face(Paint.Face.TOP), BONE_DARK, 4);
        p.fill(CLAW.face(Paint.Face.BOTTOM), BONE, 4);
        p.fill(SHOULDER.face(Paint.Face.TOP), BARK, 10);
    }
}
