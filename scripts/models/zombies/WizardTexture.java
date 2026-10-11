import java.nio.file.Path;

// Paints the Wizard for the boxes of creature/wizard/client/WizardModel and StaffLayer: wizard.png (a deep violet-blue
// robe and hat sewn with silver stars and trimmed in gold, a long white beard, a hooked nose, a dark staff of twisted
// wood shod with iron and its crystal), wizard_glow.png (his pale eyes, the runes round his hem and sleeves, the stars
// on his hat and the crystal, all glowing) and wizard_orb.png (a soft round glow, white in the middle, for his spells'
// light). Keep the boxes here as the models build them. From the repository root, with Java 22 or newer:
// java scripts/models/zombies/WizardTexture.java
public final class WizardTexture {
    private static final int[] ROBE = { 44, 40, 104 };
    private static final int[] ROBE_DARK = { 30, 26, 74 };
    private static final int[] ROBE_LIGHT = { 62, 58, 134 };
    private static final int[] GOLD = { 214, 172, 66 };
    private static final int[] GOLD_DARK = { 150, 112, 40 };
    private static final int[] SILVER = { 206, 214, 236 };
    private static final int[] SKIN = { 214, 172, 146 };
    private static final int[] SKIN_DARK = { 176, 132, 110 };
    private static final int[] BEARD = { 226, 226, 232 };
    private static final int[] BEARD_DARK = { 184, 184, 196 };
    private static final int[] EYE = { 40, 70, 120 };
    private static final int[] GLOW_EYE = { 150, 210, 255 };
    private static final int[] RUNE = { 190, 150, 255 };
    private static final int[] WOOD = { 72, 50, 34 };
    private static final int[] WOOD_DARK = { 46, 32, 22 };
    private static final int[] IRON = { 120, 120, 130 };
    private static final int[] CRYSTAL = { 186, 128, 255 };
    private static final int[] CRYSTAL_LIGHT = { 236, 214, 255 };
    private static final int[] BOOT = { 54, 40, 32 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 8, 8, 8);
    private static final Paint.Box NOSE = new Paint.Box(32, 0, 2, 3, 2);
    private static final Paint.Box BEARD_BOX = new Paint.Box(40, 0, 6, 7, 2);
    private static final Paint.Box BEARD_TIP = new Paint.Box(56, 0, 4, 4, 2);
    private static final Paint.Box MANTLE = new Paint.Box(68, 0, 10, 3, 6);
    private static final Paint.Box BRIM = new Paint.Box(0, 16, 14, 1, 14);
    private static final Paint.Box HAT1 = new Paint.Box(56, 16, 9, 4, 9);
    private static final Paint.Box HAT2 = new Paint.Box(92, 16, 7, 4, 7);
    private static final Paint.Box HAT3 = new Paint.Box(56, 30, 5, 4, 5);
    private static final Paint.Box HAT4 = new Paint.Box(76, 30, 3, 4, 3);
    private static final Paint.Box HAT5 = new Paint.Box(88, 30, 2, 3, 2);
    private static final Paint.Box TORSO = new Paint.Box(0, 32, 8, 12, 4);
    private static final Paint.Box SKIRT = new Paint.Box(24, 32, 9, 11, 5);
    private static final Paint.Box HEM = new Paint.Box(52, 40, 10, 5, 6);
    private static final Paint.Box ARM = new Paint.Box(0, 48, 3, 12, 3);
    private static final Paint.Box SLEEVE = new Paint.Box(12, 48, 5, 6, 5);
    private static final Paint.Box LEG = new Paint.Box(32, 48, 4, 12, 4);
    private static final Paint.Box SHAFT = new Paint.Box(0, 64, 2, 35, 2);
    private static final Paint.Box KNOT = new Paint.Box(8, 64, 3, 2, 3);
    private static final Paint.Box RING = new Paint.Box(8, 72, 2, 1, 2);
    private static final Paint.Box PRONG = new Paint.Box(20, 64, 1, 5, 1);
    private static final Paint.Box CRYSTAL_BOX = new Paint.Box(24, 64, 2, 5, 2);
    private static final Paint.Box CRYSTAL_TOP = new Paint.Box(24, 72, 1, 1, 1);
    private static final Paint.Box BUTT = new Paint.Box(32, 64, 2, 2, 2);
    private static final Paint.Face[] AROUND = { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
            Paint.Face.BACK };

    private WizardTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        Paint p = new Paint(128, 128, 9091);
        Paint glow = new Paint(128, 128, 9092);
        head(p, glow);
        hat(p, glow);
        robe(p, glow);
        staff(p, glow);
        p.save(folder, "wizard");
        glow.save(folder, "wizard_glow");
        orb(folder);
        System.out.println("painted");
    }

    // A weathered face with bushy white brows over pale eyes, a hooked nose, white hair round the back, and the long
    // white beard in strands.
    private static void head(Paint p, Paint glow) {
        p.all(HEAD, SKIN, 6);
        int[] f = HEAD.face(Paint.Face.FRONT);
        p.row(f, 0, BEARD);
        p.row(f, 1, BEARD_DARK);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            p.dot(f, x, 2, BEARD);
        }
        p.dot(f, 2, 3, EYE);
        p.dot(f, 5, 3, EYE);
        p.dot(f, 1, 3, SKIN_DARK);
        p.dot(f, 6, 3, SKIN_DARK);
        glow.dot(f, 2, 3, GLOW_EYE);
        glow.dot(f, 5, 3, GLOW_EYE);
        p.row(f, 5, SKIN_DARK);
        for (int y = 6; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                p.dot(f, x, y, p.shade(x == 0 || x == 7 ? BEARD_DARK : BEARD, 8));
            }
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.RIGHT, Paint.Face.LEFT, Paint.Face.BACK }) {
            int[] s = HEAD.face(face);
            for (int y = 2; y < 8; y++) {
                for (int x = 0; x < s[2]; x++) {
                    boolean hair = face == Paint.Face.BACK || x >= (face == Paint.Face.RIGHT ? 4 : 0)
                            && x < (face == Paint.Face.RIGHT ? 8 : 4);
                    if (hair) {
                        p.dot(s, x, y, p.shade((x + y) % 3 == 0 ? BEARD_DARK : BEARD, 6));
                    }
                }
            }
        }
        p.all(NOSE, SKIN, 5);
        p.row(NOSE.face(Paint.Face.FRONT), 2, SKIN_DARK);
        for (Paint.Box box : new Paint.Box[] { BEARD_BOX, BEARD_TIP }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] b = box.face(face);
                for (int y = 0; y < b[3]; y++) {
                    for (int x = 0; x < b[2]; x++) {
                        p.dot(b, x, y, p.shade(x % 2 == 0 ? BEARD : BEARD_DARK, 8));
                    }
                }
            }
        }
        int[] tip = BEARD_TIP.face(Paint.Face.FRONT);
        p.clear(tip, 0, 3);
        p.clear(tip, 3, 3);
    }

    // The hat: robe-blue, a gold band above the brim, silver stars and a crescent moon, a little bent and worn.
    private static void hat(Paint p, Paint glow) {
        for (Paint.Box box : new Paint.Box[] { BRIM, HAT1, HAT2, HAT3, HAT4, HAT5 }) {
            for (Paint.Face face : Paint.Face.values()) {
                p.skin(box.face(face), ROBE, ROBE_DARK, ROBE_LIGHT);
            }
        }
        int[] under = BRIM.face(Paint.Face.BOTTOM);
        p.fill(under, ROBE_DARK, 5);
        for (Paint.Face face : AROUND) {
            int[] band = HAT1.face(face);
            p.row(band, 3, GOLD);
            p.row(band, 2, GOLD_DARK);
            int[] mid = HAT2.face(face);
            int x = p.random.nextInt(mid[2]);
            p.dot(mid, x, 1, SILVER);
            glow.dot(mid, x, 1, SILVER);
            int[] top = HAT3.face(face);
            int tx = p.random.nextInt(top[2]);
            p.dot(top, tx, 2, SILVER);
            glow.dot(top, tx, 2, SILVER);
        }
        int[] front = HAT1.face(Paint.Face.FRONT);
        for (int[] d : new int[][] { { 3, 0 }, { 4, 0 }, { 2, 1 }, { 5, 1 }, { 3, 1 } }) {
            p.dot(front, d[0] + 1, d[1], SILVER);
            glow.dot(front, d[0] + 1, d[1], SILVER);
        }
        int[] brim = BRIM.face(Paint.Face.TOP);
        for (int k = 0; k < 14; k++) {
            int bx = p.random.nextInt(14);
            int by = p.random.nextInt(14);
            if (bx < 3 || bx > 10 || by < 3 || by > 10) {
                p.dot(brim, bx, by, ROBE_LIGHT);
            }
        }
    }

    // The robe: deep violet-blue, darker in its folds, a gold trim down its front and round the hem, runes stitched
    // round the hem and the cuffs of the sleeves, a gold-clasped mantle over the shoulders; boots under it.
    private static void robe(Paint p, Paint glow) {
        for (Paint.Box box : new Paint.Box[] { TORSO, SKIRT, HEM, SLEEVE, MANTLE }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] f = box.face(face);
                p.fill(f, ROBE, 6);
                for (int x = 1; x < f[2]; x += 3) {
                    p.column(f, x, ROBE_DARK);
                }
            }
        }
        p.column(TORSO.face(Paint.Face.FRONT), 3, GOLD);
        p.column(TORSO.face(Paint.Face.FRONT), 4, GOLD_DARK);
        p.column(SKIRT.face(Paint.Face.FRONT), 4, GOLD);
        p.row(TORSO.face(Paint.Face.FRONT), 9, GOLD_DARK);
        for (Paint.Face face : AROUND) {
            int[] m = MANTLE.face(face);
            p.row(m, m[3] - 1, GOLD);
            int[] hem = HEM.face(face);
            p.row(hem, hem[3] - 1, GOLD);
            p.row(hem, hem[3] - 2, GOLD_DARK);
            for (int x = 1; x < hem[2] - 1; x += 2) {
                int y = 1 + (x / 2) % 2;
                p.dot(hem, x, y, RUNE);
                glow.dot(hem, x, y, RUNE);
            }
            int[] cuff = SLEEVE.face(face);
            p.row(cuff, cuff[3] - 1, GOLD);
            int rx = 1 + p.random.nextInt(Math.max(1, cuff[2] - 2));
            p.dot(cuff, rx, cuff[3] - 3, RUNE);
            glow.dot(cuff, rx, cuff[3] - 3, RUNE);
        }
        int[] clasp = MANTLE.face(Paint.Face.FRONT);
        p.dot(clasp, 4, 1, GOLD);
        p.dot(clasp, 5, 1, GOLD);
        glow.dot(clasp, 4, 1, GOLD_DARK);
        for (Paint.Face face : Paint.Face.values()) {
            int[] arm = ARM.face(face);
            p.fill(arm, ROBE_DARK, 5);
            for (int y = 10; y < 12; y++) {
                for (int x = 0; x < arm[2]; x++) {
                    p.dot(arm, x, y, p.shade(SKIN, 6));
                }
            }
            int[] leg = LEG.face(face);
            p.fill(leg, ROBE_DARK, 5);
            for (int y = 8; y < 12; y++) {
                for (int x = 0; x < leg[2]; x++) {
                    p.dot(leg, x, y, p.shade(BOOT, 6));
                }
            }
        }
        int[] belt = SKIRT.face(Paint.Face.FRONT);
        p.row(belt, 0, GOLD_DARK);
    }

    // Dark twisted wood with a grain winding round it, iron rings and an iron foot, the claws of its head and the
    // crystal they hold.
    private static void staff(Paint p, Paint glow) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] s = SHAFT.face(face);
            p.fill(s, WOOD, 6);
            for (int y = 0; y < s[3]; y += 3) {
                p.dot(s, (y / 3) % Math.max(1, s[2]), y, WOOD_DARK);
            }
            p.fill(KNOT.face(face), WOOD_DARK, 6);
            p.fill(RING.face(face), IRON, 8);
            p.fill(BUTT.face(face), IRON, 8);
            p.fill(PRONG.face(face), WOOD_DARK, 5);
            p.dot(PRONG.face(face), 0, 0, GOLD);
            for (Paint.Box box : new Paint.Box[] { CRYSTAL_BOX, CRYSTAL_TOP }) {
                int[] c = box.face(face);
                p.fill(c, CRYSTAL, 10);
                glow.fill(c, CRYSTAL, 8);
                for (int y = 0; y < c[3]; y += 2) {
                    p.dot(c, 0, y, CRYSTAL_LIGHT);
                    glow.dot(c, 0, y, CRYSTAL_LIGHT);
                }
            }
        }
    }

    // A soft round glow: white in the middle, falling off to nothing at its edge.
    private static void orb(Path folder) throws Exception {
        Paint o = new Paint(32, 32, 1);
        int[] all = { 0, 0, 32, 32 };
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                double dx = (x + 0.5 - 16.0) / 16.0;
                double dy = (y + 0.5 - 16.0) / 16.0;
                double r = Math.sqrt(dx * dx + dy * dy);
                double v = Math.max(0.0, 1.0 - r);
                v = Math.pow(v, 1.6) * 0.75 + Math.max(0.0, 1.0 - r * 3.0) * 0.4;
                int c = (int) Math.min(255, Math.round(v * 255));
                o.dot(all, x, y, new int[] { c, c, c }, Math.min(255, c * 2));
            }
        }
        o.save(folder, "wizard_orb");
    }
}
