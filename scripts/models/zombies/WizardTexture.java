import java.nio.file.Path;

// Paints the Wizard for the boxes of creature/wizard/client/WizardModel and StaffLayer, once for each of his five
// phases: wizard.png (a deep violet-blue robe, mantle, cape and hat sewn with silver stars and trimmed in gold, long
// white hair, brows, moustache and beard, a hooked nose, a dark staff of twisted wood shod with iron and its crystal,
// the pauldrons, crown spikes and horns his later phases wear) and wizard_glow.png (his pale eyes, the runes round his
// hem and cuffs, the stars on his hat, robe and cape, and the crystal, all glowing); wizard_2 to wizard_5 (and their
// glows) the same in the colours of each later phase, grander each time: frost-blue and silver with frost on the hem,
// crimson and gold with flames licking up the skirt, the void's black and magenta cracked with light, white and gold
// strewn with stars. wizard_orb.png is a soft round glow, white in the middle, for his spells' light. Keep the boxes
// here as the models build them. From the repository root, with Java 22 or newer:
// java scripts/models/zombies/WizardTexture.java
public final class WizardTexture {
    private static int[] ROBE = { 44, 40, 104 };
    private static int[] ROBE_DARK = { 30, 26, 74 };
    private static int[] ROBE_LIGHT = { 62, 58, 134 };
    private static int[] GOLD = { 214, 172, 66 };
    private static int[] GOLD_DARK = { 150, 112, 40 };
    private static int[] SILVER = { 206, 214, 236 };
    private static final int[] SKIN = { 214, 172, 146 };
    private static final int[] SKIN_DARK = { 176, 132, 110 };
    private static final int[] BEARD = { 226, 226, 232 };
    private static final int[] BEARD_DARK = { 184, 184, 196 };
    private static final int[] EYE = { 40, 70, 120 };
    private static final int[] GLOW_EYE = { 150, 210, 255 };
    private static int[] RUNE = { 190, 150, 255 };
    private static final int[] WOOD = { 72, 50, 34 };
    private static final int[] WOOD_DARK = { 46, 32, 22 };
    private static final int[] IRON = { 120, 120, 130 };
    private static int[] CRYSTAL = { 186, 128, 255 };
    private static int[] CRYSTAL_LIGHT = { 236, 214, 255 };
    private static final int[] BOOT = { 54, 40, 32 };
    private static final int[] BOOT_DARK = { 34, 24, 20 };
    private static final int[] WHITE = { 236, 236, 240 };
    private static final int[] CHEEK = { 204, 140, 126 };
    private static final int[] BEARD_LIGHT = { 246, 246, 250 };
    private static int[] STAR_GLOW = { 150, 160, 200 };
    private static int[] GOLD_LIGHT = { 252, 220, 120 };
    private static int[] LEATHER = { 74, 48, 32 };
    private static int[] RUNE_DIM = { 130, 96, 200 };
    private static int[] PLUM = { 78, 30, 72 };

    private static final Paint.Box HEAD = new Paint.Box(0, 0, 8, 8, 8);
    private static final Paint.Box NOSE = new Paint.Box(32, 0, 2, 3, 2);
    private static final Paint.Box BROWS = new Paint.Box(40, 0, 7, 1, 1);
    private static final Paint.Box MUSTACHE = new Paint.Box(40, 2, 7, 2, 1);
    private static final Paint.Box HAIR = new Paint.Box(56, 0, 9, 10, 2);
    private static final Paint.Box LOCK = new Paint.Box(78, 0, 1, 8, 4);
    private static final Paint.Box BEARD_BOX = new Paint.Box(88, 0, 6, 6, 2);
    private static final Paint.Box BEARD_TIP = new Paint.Box(104, 0, 4, 5, 2);
    private static final Paint.Box BEARD_POINT = new Paint.Box(116, 0, 2, 3, 1);
    private static final Paint.Box BRIM = new Paint.Box(0, 16, 14, 1, 14);
    private static final Paint.Box HAT1 = new Paint.Box(56, 16, 9, 4, 9);
    private static final Paint.Box HAT2 = new Paint.Box(92, 16, 7, 4, 7);
    private static final Paint.Box HAT3 = new Paint.Box(32, 5, 5, 4, 5);
    private static final Paint.Box HAT4 = new Paint.Box(88, 8, 3, 4, 3);
    private static final Paint.Box HAT5 = new Paint.Box(100, 8, 2, 3, 2);
    private static final Paint.Box TORSO = new Paint.Box(0, 32, 8, 12, 4);
    private static final Paint.Box MANTLE = new Paint.Box(24, 32, 12, 4, 6);
    private static final Paint.Box COLLAR = new Paint.Box(60, 32, 9, 3, 1);
    private static final Paint.Box BELT = new Paint.Box(60, 36, 9, 2, 5);
    private static final Paint.Box SKIRT_FRONT = new Paint.Box(0, 48, 10, 12, 3);
    private static final Paint.Box SKIRT_BACK = new Paint.Box(26, 48, 10, 12, 3);
    private static final Paint.Box HEM_FRONT = new Paint.Box(52, 48, 11, 3, 3);
    private static final Paint.Box HEM_BACK = new Paint.Box(52, 54, 11, 3, 3);
    private static final Paint.Box ARM = new Paint.Box(80, 44, 3, 12, 3);
    private static final Paint.Box SLEEVE = new Paint.Box(92, 29, 4, 7, 4);
    private static final Paint.Box CUFF = new Paint.Box(108, 29, 5, 4, 5);
    private static final Paint.Box LEG = new Paint.Box(92, 44, 4, 12, 4);
    private static final Paint.Box CAPE = new Paint.Box(108, 40, 9, 20, 1);
    private static final Paint.Box SHAFT = new Paint.Box(0, 64, 2, 35, 2);
    private static final Paint.Box KNOT = new Paint.Box(8, 64, 3, 2, 3);
    private static final Paint.Box RING = new Paint.Box(8, 72, 2, 1, 2);
    private static final Paint.Box PRONG = new Paint.Box(20, 64, 1, 5, 1);
    private static final Paint.Box CRYSTAL_BOX = new Paint.Box(24, 64, 2, 5, 2);
    private static final Paint.Box CRYSTAL_TOP = new Paint.Box(24, 72, 1, 1, 1);
    private static final Paint.Box BUTT = new Paint.Box(32, 64, 2, 2, 2);
    private static final Paint.Box PAULDRON = new Paint.Box(40, 64, 6, 3, 5);
    private static final Paint.Box SPIKE = new Paint.Box(64, 64, 1, 5, 1);
    private static final Paint.Box HORN = new Paint.Box(72, 64, 2, 4, 2);
    private static final Paint.Box HORN_MID = new Paint.Box(82, 64, 1, 3, 1);
    private static final Paint.Box HORN_TIP = new Paint.Box(88, 64, 1, 2, 1);
    private static final int[] HORN_DARK = { 30, 18, 40 };
    private static final int[] HORN_LIGHT = { 64, 44, 80 };
    private static final Paint.Face[] AROUND = { Paint.Face.FRONT, Paint.Face.RIGHT, Paint.Face.LEFT,
            Paint.Face.BACK };

    private WizardTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "entity");
        for (int phase = 1; phase <= 5; phase++) {
            look(phase);
            // The same seeds each phase: the same folds, stars and strands, in other colours.
            Paint p = new Paint(128, 128, 9091);
            Paint glow = new Paint(128, 128, 9092);
            head(p, glow);
            hat(p, glow);
            robe(p, glow);
            staff(p, glow);
            regalia(p, glow);
            extras(p, glow, phase);
            String end = phase == 1 ? "" : "_" + phase;
            p.save(folder, "wizard" + end);
            glow.save(folder, "wizard_glow" + end);
        }
        orb(folder);
        System.out.println("painted");
    }

    // The colours of each phase's robe, trim, runes, lining, stars and crystal.
    private static void look(int phase) {
        int[][] c = switch (phase) {
            case 2 -> new int[][] { { 40, 78, 132 }, { 24, 50, 92 }, { 84, 132, 184 }, { 200, 224, 244 },
                    { 130, 160, 196 }, { 240, 250, 255 }, { 255, 255, 255 }, { 140, 220, 255 }, { 90, 160, 220 },
                    { 22, 44, 84 }, { 150, 210, 250 }, { 60, 70, 90 }, { 130, 210, 255 } };
            case 3 -> new int[][] { { 120, 28, 26 }, { 72, 14, 16 }, { 164, 52, 38 }, { 236, 170, 56 },
                    { 170, 104, 30 }, { 255, 214, 110 }, { 255, 200, 120 }, { 255, 160, 60 }, { 200, 100, 40 },
                    { 36, 10, 10 }, { 240, 140, 60 }, { 50, 26, 20 }, { 255, 180, 80 } };
            case 4 -> new int[][] { { 26, 16, 40 }, { 12, 6, 20 }, { 56, 30, 80 }, { 170, 80, 230 },
                    { 100, 40, 150 }, { 226, 150, 255 }, { 230, 170, 255 }, { 220, 90, 255 }, { 150, 60, 200 },
                    { 70, 10, 80 }, { 190, 90, 255 }, { 30, 20, 36 }, { 220, 80, 255 } };
            case 5 -> new int[][] { { 228, 224, 240 }, { 186, 180, 210 }, { 250, 248, 255 }, { 244, 200, 90 },
                    { 196, 150, 56 }, { 255, 236, 160 }, { 255, 246, 210 }, { 255, 230, 150 }, { 220, 190, 110 },
                    { 24, 28, 70 }, { 255, 240, 190 }, { 200, 170, 110 }, { 255, 248, 220 } };
            default -> new int[][] { { 44, 40, 104 }, { 30, 26, 74 }, { 62, 58, 134 }, { 214, 172, 66 },
                    { 150, 112, 40 }, { 252, 220, 120 }, { 206, 214, 236 }, { 190, 150, 255 }, { 130, 96, 200 },
                    { 78, 30, 72 }, { 150, 160, 200 }, { 74, 48, 32 }, { 186, 128, 255 } };
        };
        ROBE = c[0];
        ROBE_DARK = c[1];
        ROBE_LIGHT = c[2];
        GOLD = c[3];
        GOLD_DARK = c[4];
        GOLD_LIGHT = c[5];
        SILVER = c[6];
        RUNE = c[7];
        RUNE_DIM = c[8];
        PLUM = c[9];
        STAR_GLOW = c[10];
        LEATHER = c[11];
        CRYSTAL = c[12];
        CRYSTAL_LIGHT = phase == 1 ? new int[] { 236, 214, 255 } : mix(c[12], new int[] { 255, 255, 255 }, 0.65);
    }

    private static int[] mix(int[] a, int[] b, double t) {
        return new int[] { (int) Math.round(a[0] + (b[0] - a[0]) * t), (int) Math.round(a[1] + (b[1] - a[1]) * t),
                (int) Math.round(a[2] + (b[2] - a[2]) * t) };
    }

    // What his later phases wear (WizardModel shows each from its phase): pauldrons in the trim's metal with a rune,
    // crown spikes of the trim tipped with light, horns of dark polished stone veined with the runes' light.
    private static void regalia(Paint p, Paint glow) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = PAULDRON.face(face);
            p.fill(f, GOLD_DARK, 6);
            p.row(f, 0, GOLD_LIGHT);
            p.row(f, f[3] - 1, GOLD);
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                int x = f[2] / 2;
                p.dot(f, x, 1, RUNE);
                glow.dot(f, x, 1, RUNE);
            } else if (face == Paint.Face.TOP) {
                p.fill(f, GOLD, 6);
                for (int x = 1; x < f[2]; x += 2) {
                    p.dot(f, x, f[3] / 2, GOLD_LIGHT);
                }
            }
            int[] spike = SPIKE.face(face);
            p.fill(spike, GOLD, 4);
            p.dot(spike, 0, 0, GOLD_LIGHT);
            glow.dot(spike, 0, 0, RUNE);
            glow.dot(spike, 0, 1, RUNE_DIM);
            for (Paint.Box box : new Paint.Box[] { HORN, HORN_MID, HORN_TIP }) {
                int[] h = box.face(face);
                p.fill(h, HORN_DARK, 5);
                for (int y = 0; y < h[3]; y += 2) {
                    p.dot(h, y % Math.max(1, h[2]), y, HORN_LIGHT);
                }
            }
            int[] horn = HORN.face(face);
            for (int y = 0; y < horn[3]; y++) {
                if ((y + face.ordinal()) % 2 == 0) {
                    p.dot(horn, y % horn[2], y, RUNE);
                    glow.dot(horn, y % horn[2], y, RUNE);
                }
            }
            int[] tip = HORN_TIP.face(face);
            p.dot(tip, 0, 0, RUNE);
            glow.dot(tip, 0, 0, RUNE);
        }
    }

    // What makes each phase grander than the last: frost riming the hem and cuffs; flames licking up the skirt and
    // cape; cracks of the void's light across the robe; stars strewn all over him, glowing.
    private static void extras(Paint p, Paint glow, int phase) {
        Paint.Box[] cloth = { TORSO, MANTLE, SKIRT_FRONT, SKIRT_BACK, HEM_FRONT, HEM_BACK, SLEEVE, CUFF, CAPE, HAT1,
                HAT2, HAT3, BRIM };
        switch (phase) {
            case 2 -> {
                int[] frost = { 232, 246, 255 };
                for (Paint.Box box : new Paint.Box[] { SKIRT_FRONT, SKIRT_BACK, HEM_FRONT, HEM_BACK, CUFF, CAPE }) {
                    for (Paint.Face face : AROUND) {
                        int[] f = box.face(face);
                        for (int x = 0; x < f[2]; x++) {
                            int deep = 1 + p.random.nextInt(box == CAPE ? 4 : 3);
                            for (int y = f[3] - deep; y < f[3]; y++) {
                                p.dot(f, x, y, p.shade(frost, 8));
                            }
                            if (p.random.nextInt(4) == 0) {
                                glow.dot(f, x, f[3] - 1, RUNE_DIM);
                            }
                        }
                    }
                }
            }
            case 3 -> {
                int[] yellow = { 255, 220, 110 };
                int[] orange = { 255, 130, 40 };
                for (Paint.Box box : new Paint.Box[] { SKIRT_FRONT, SKIRT_BACK, CAPE, SLEEVE }) {
                    for (Paint.Face face : AROUND) {
                        int[] f = box.face(face);
                        for (int x = 0; x < f[2]; x++) {
                            int tall = 2 + (int) Math.round(Math.abs(Math.sin(x * 1.7 + box.u())) * (f[3] * 0.45));
                            for (int k = 0; k < tall; k++) {
                                int y = f[3] - 1 - k;
                                int[] c = k < tall / 3 ? yellow : k < tall * 2 / 3 ? orange : RUNE_DIM;
                                p.dot(f, x, y, c);
                                if (k < tall * 2 / 3) {
                                    glow.dot(f, x, y, c);
                                }
                            }
                        }
                    }
                }
            }
            case 4 -> {
                for (Paint.Box box : cloth) {
                    for (Paint.Face face : AROUND) {
                        int[] f = box.face(face);
                        int x = p.random.nextInt(f[2]);
                        for (int y = 0; y < f[3]; y++) {
                            if (p.random.nextInt(3) == 0) {
                                x = Math.max(0, Math.min(f[2] - 1, x + p.random.nextInt(3) - 1));
                            }
                            if (p.random.nextInt(5) != 0) {
                                p.dot(f, x, y, RUNE);
                                glow.dot(f, x, y, RUNE);
                            }
                        }
                    }
                }
            }
            case 5 -> {
                for (Paint.Box box : cloth) {
                    for (Paint.Face face : Paint.Face.values()) {
                        int[] f = box.face(face);
                        int stars = Math.max(1, f[2] * f[3] / 9);
                        for (int k = 0; k < stars; k++) {
                            int x = p.random.nextInt(f[2]);
                            int y = p.random.nextInt(f[3]);
                            int[] c = k % 3 == 0 ? GOLD_LIGHT : SILVER;
                            p.dot(f, x, y, c);
                            glow.dot(f, x, y, STAR_GLOW);
                        }
                    }
                }
                int[] lining = CAPE.face(Paint.Face.FRONT);
                for (int k = 0; k < 30; k++) {
                    int x = p.random.nextInt(lining[2]);
                    int y = p.random.nextInt(lining[3]);
                    p.dot(lining, x, y, SILVER);
                    glow.dot(lining, x, y, STAR_GLOW);
                }
            }
            default -> {
            }
        }
    }

    // A weathered face: lined brow, pale blue eyes that glow, ruddy cheeks; bushy white brows, a drooping moustache, long
    // white hair down his back and at his temples, and the long beard in strands that fray at its point.
    private static void head(Paint p, Paint glow) {
        p.all(HEAD, SKIN, 5);
        int[] f = HEAD.face(Paint.Face.FRONT);
        for (int x = 0; x < 8; x++) {
            p.dot(f, x, 0, p.shade(x % 3 == 0 ? BEARD_DARK : BEARD, 6));
        }
        p.dot(f, 0, 1, BEARD);
        p.dot(f, 7, 1, BEARD);
        for (int x = 2; x < 6; x++) {
            p.dot(f, x, 1, p.shade(x % 2 == 0 ? SKIN_DARK : SKIN, 4));
        }
        p.dot(f, 0, 2, BEARD_DARK);
        p.dot(f, 7, 2, BEARD_DARK);
        p.dot(f, 1, 3, WHITE);
        p.dot(f, 2, 3, EYE);
        p.dot(f, 5, 3, EYE);
        p.dot(f, 6, 3, WHITE);
        glow.dot(f, 2, 3, GLOW_EYE);
        glow.dot(f, 5, 3, GLOW_EYE);
        p.dot(f, 0, 3, BEARD_DARK);
        p.dot(f, 7, 3, BEARD_DARK);
        p.dot(f, 1, 4, SKIN_DARK);
        p.dot(f, 6, 4, SKIN_DARK);
        p.dot(f, 1, 5, CHEEK);
        p.dot(f, 6, 5, CHEEK);
        for (int y = 4; y < 8; y++) {
            p.dot(f, 0, y, p.shade(BEARD, 6));
            p.dot(f, 7, y, p.shade(BEARD, 6));
        }
        for (int y = 6; y < 8; y++) {
            for (int x = 1; x < 7; x++) {
                p.dot(f, x, y, p.shade((x + y) % 2 == 0 ? BEARD : BEARD_DARK, 6));
            }
        }
        for (Paint.Face face : new Paint.Face[] { Paint.Face.RIGHT, Paint.Face.LEFT, Paint.Face.BACK }) {
            int[] s = HEAD.face(face);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < s[2]; x++) {
                    boolean front = face == Paint.Face.RIGHT ? x < 3 : face == Paint.Face.LEFT && x > 4;
                    if (!front || y < 1 || y > 5) {
                        p.dot(s, x, y, strand(p, x, y));
                    }
                }
            }
            if (face != Paint.Face.BACK) {
                int ear = face == Paint.Face.RIGHT ? 2 : 5;
                p.dot(s, ear, 3, SKIN_DARK);
                p.dot(s, ear, 4, SKIN_DARK);
            }
        }
        p.fill(HEAD.face(Paint.Face.TOP), BEARD_DARK, 6);
        p.all(NOSE, SKIN, 4);
        int[] nose = NOSE.face(Paint.Face.FRONT);
        p.dot(nose, 0, 0, SKIN_DARK);
        p.dot(nose, 1, 0, SKIN_DARK);
        p.row(nose, 2, CHEEK);
        p.row(NOSE.face(Paint.Face.BOTTOM), 0, SKIN_DARK);
        p.row(NOSE.face(Paint.Face.BOTTOM), 1, SKIN_DARK);
        for (Paint.Box box : new Paint.Box[] { BROWS, MUSTACHE, HAIR, LOCK, BEARD_BOX, BEARD_TIP, BEARD_POINT }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] b = box.face(face);
                for (int y = 0; y < b[3]; y++) {
                    for (int x = 0; x < b[2]; x++) {
                        p.dot(b, x, y, strand(p, x, y));
                    }
                }
            }
        }
        int[] brows = BROWS.face(Paint.Face.FRONT);
        p.dot(brows, 3, 0, SKIN_DARK);
        p.dot(brows, 0, 0, BEARD_DARK);
        p.dot(brows, 6, 0, BEARD_DARK);
        int[] stache = MUSTACHE.face(Paint.Face.FRONT);
        p.clear(stache, 0, 0);
        p.clear(stache, 6, 0);
        p.dot(stache, 3, 0, SKIN_DARK);
        p.dot(stache, 3, 1, BEARD_DARK);
        for (Paint.Face face : Paint.Face.values()) {
            int[] tip = BEARD_POINT.face(face);
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                p.clear(tip, 0, tip[3] - 1);
            }
            int[] end = BEARD_TIP.face(face);
            if (face == Paint.Face.FRONT || face == Paint.Face.BACK) {
                p.clear(end, 0, end[3] - 1);
                p.clear(end, end[2] - 1, end[3] - 1);
            }
            int[] hair = HAIR.face(face);
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                for (int x = 0; x < hair[2]; x += 2) {
                    p.clear(hair, x, hair[3] - 1);
                }
            }
            int[] lock = LOCK.face(face);
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                p.clear(lock, 0, lock[3] - 1);
            }
        }
    }

    // White hair in strands: lighter and darker lines, now and then a grey one.
    private static int[] strand(Paint p, int x, int y) {
        int k = (x * 7 + y / 3) % 5;
        int[] c = k == 0 ? BEARD_DARK : k == 3 ? BEARD_LIGHT : BEARD;
        return p.shade(c, 5);
    }

    // The hat: robe-blue, darker under the brim, a gold band, silver stars and a crescent moon on its crown, patches of
    // wear, and a few stars higher up its bent tip, which glow.
    private static void hat(Paint p, Paint glow) {
        for (Paint.Box box : new Paint.Box[] { BRIM, HAT1, HAT2, HAT3, HAT4, HAT5 }) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] f = box.face(face);
                p.fill(f, ROBE, 5);
                for (int x = 1; x < f[2]; x += 3) {
                    for (int y = 0; y < f[3]; y++) {
                        if (p.random.nextInt(3) == 0) {
                            p.dot(f, x, y, p.shade(ROBE_DARK, 4));
                        }
                    }
                }
                if (face == Paint.Face.TOP) {
                    p.fill(f, ROBE_LIGHT, 5);
                }
            }
        }
        p.fill(BRIM.face(Paint.Face.BOTTOM), ROBE_DARK, 4);
        int[] brim = BRIM.face(Paint.Face.TOP);
        p.fill(brim, ROBE, 5);
        for (int x = 0; x < 14; x++) {
            p.dot(brim, x, 0, ROBE_LIGHT);
            p.dot(brim, x, 13, ROBE_DARK);
            p.dot(brim, 0, x, ROBE_LIGHT);
            p.dot(brim, 13, x, ROBE_DARK);
        }
        for (int[] d : new int[][] { { 2, 3 }, { 11, 2 }, { 3, 11 }, { 10, 11 }, { 1, 7 }, { 12, 6 } }) {
            p.dot(brim, d[0], d[1], SILVER);
            glow.dot(brim, d[0], d[1], STAR_GLOW);
        }
        for (Paint.Face face : AROUND) {
            int[] band = HAT1.face(face);
            p.row(band, 3, GOLD_DARK);
            p.row(band, 2, GOLD);
            for (Paint.Box box : new Paint.Box[] { HAT2, HAT3 }) {
                int[] up = box.face(face);
                int x = p.random.nextInt(up[2]);
                int y = p.random.nextInt(Math.max(1, up[3] - 1));
                p.dot(up, x, y, SILVER);
                glow.dot(up, x, y, STAR_GLOW);
            }
        }
        int[] front = HAT1.face(Paint.Face.FRONT);
        for (int[] d : new int[][] { { 2, 0 }, { 3, 0 }, { 1, 1 }, { 4, 1 }, { 7, 0 } }) {
            p.dot(front, d[0], d[1], SILVER);
            glow.dot(front, d[0], d[1], STAR_GLOW);
        }
        p.dot(front, 4, 2, GOLD_LIGHT);
        int[] tip = HAT5.face(Paint.Face.FRONT);
        p.dot(tip, 0, 2, GOLD);
        p.dot(tip, 1, 2, GOLD_DARK);
    }

    // The robe: deep violet-blue with darker folds and silver stars; a gold trim down its front, a mantle with a gold
    // edge and clasp, a high collar, a leather belt with a gold buckle; the skirt in two panels (front and back) with a
    // gold hem stitched with glowing runes; bell sleeves trimmed in gold; boots; a cape lined in plum with a silver
    // moon and star on its back.
    private static void robe(Paint p, Paint glow) {
        Paint.Box[] cloth = { TORSO, MANTLE, COLLAR, SKIRT_FRONT, SKIRT_BACK, HEM_FRONT, HEM_BACK, SLEEVE, CUFF, CAPE };
        for (Paint.Box box : cloth) {
            for (Paint.Face face : Paint.Face.values()) {
                int[] f = box.face(face);
                p.fill(f, ROBE, 5);
                for (int x = 1; x < f[2]; x += 3) {
                    p.column(f, x, ROBE_DARK);
                }
                if (f[2] > 3) {
                    for (int y = 0; y < f[3]; y += 4) {
                        p.dot(f, (y * 3 + 2) % f[2], y, ROBE_LIGHT);
                    }
                }
                if (face == Paint.Face.TOP) {
                    p.fill(f, ROBE_LIGHT, 4);
                } else if (face == Paint.Face.BOTTOM) {
                    p.fill(f, ROBE_DARK, 4);
                }
            }
        }
        int[] chest = TORSO.face(Paint.Face.FRONT);
        p.column(chest, 3, GOLD);
        p.column(chest, 4, GOLD_DARK);
        p.dot(chest, 1, 5, SILVER);
        p.dot(chest, 6, 7, SILVER);
        glow.dot(chest, 1, 5, STAR_GLOW);
        glow.dot(chest, 6, 7, STAR_GLOW);
        for (Paint.Face face : AROUND) {
            int[] m = MANTLE.face(face);
            p.row(m, m[3] - 1, GOLD);
            p.row(m, m[3] - 2, GOLD_DARK);
            int[] c = COLLAR.face(face);
            p.row(c, 0, GOLD);
            int[] belt = BELT.face(face);
            p.fill(belt, LEATHER, 5);
            p.row(belt, 0, p.shade(LEATHER, 0));
            for (Paint.Box hemBox : new Paint.Box[] { HEM_FRONT, HEM_BACK }) {
                int[] hem = hemBox.face(face);
                p.row(hem, 0, ROBE_DARK);
                p.row(hem, hem[3] - 1, GOLD);
                for (int x = 0; x < hem[2]; x += 2) {
                    int y = 1;
                    int[] rune = (x / 2) % 2 == 0 ? RUNE : RUNE_DIM;
                    p.dot(hem, x, y, rune);
                    glow.dot(hem, x, y, rune);
                }
            }
            int[] cuff = CUFF.face(face);
            p.row(cuff, cuff[3] - 1, GOLD);
            p.row(cuff, cuff[3] - 2, GOLD_DARK);
            int rx = cuff[2] / 2;
            p.dot(cuff, rx, 1, RUNE);
            glow.dot(cuff, rx, 1, RUNE);
        }
        int[] clasp = MANTLE.face(Paint.Face.FRONT);
        p.dot(clasp, 5, 2, GOLD_LIGHT);
        p.dot(clasp, 6, 2, GOLD_LIGHT);
        p.dot(clasp, 5, 1, GOLD);
        p.dot(clasp, 6, 1, GOLD);
        glow.dot(clasp, 5, 2, CRYSTAL);
        for (int[] d : new int[][] { { 1, 0 }, { 10, 1 }, { 3, 2 }, { 8, 0 } }) {
            p.dot(clasp, d[0], d[1], SILVER);
        }
        int[] buckle = BELT.face(Paint.Face.FRONT);
        p.dot(buckle, 4, 0, GOLD_LIGHT);
        p.dot(buckle, 4, 1, GOLD);
        p.dot(buckle, 3, 0, GOLD);
        p.dot(buckle, 5, 0, GOLD);
        p.dot(buckle, 3, 1, GOLD_DARK);
        p.dot(buckle, 5, 1, GOLD_DARK);
        int[] skirt = SKIRT_FRONT.face(Paint.Face.FRONT);
        p.column(skirt, 4, GOLD);
        p.column(skirt, 5, GOLD_DARK);
        for (Paint.Box box : new Paint.Box[] { SKIRT_FRONT, SKIRT_BACK }) {
            for (Paint.Face face : AROUND) {
                int[] s = box.face(face);
                for (int y = s[3] - 3; y < s[3]; y++) {
                    for (int x = 0; x < s[2]; x++) {
                        if ((x + y) % 2 == 0) {
                            p.dot(s, x, y, p.shade(ROBE_DARK, 4));
                        }
                    }
                }
                int sx = p.random.nextInt(s[2]);
                int sy = 1 + p.random.nextInt(Math.max(1, s[3] - 5));
                p.dot(s, sx, sy, SILVER);
                glow.dot(s, sx, sy, STAR_GLOW);
            }
        }
        int[] lining = CAPE.face(Paint.Face.FRONT);
        p.fill(lining, PLUM, 5);
        int[] back = CAPE.face(Paint.Face.BACK);
        for (int y = 0; y < back[3]; y++) {
            p.dot(back, 0, y, GOLD_DARK);
            p.dot(back, back[2] - 1, y, GOLD_DARK);
        }
        p.row(back, back[3] - 1, GOLD);
        p.row(back, 0, GOLD_DARK);
        int[][] moon = { { 3, 5 }, { 4, 4 }, { 5, 4 }, { 3, 6 }, { 3, 7 }, { 4, 8 }, { 5, 8 }, { 2, 6 } };
        for (int[] d : moon) {
            p.dot(back, d[0], d[1], SILVER);
            glow.dot(back, d[0], d[1], STAR_GLOW);
        }
        for (int[] d : new int[][] { { 6, 6 }, { 5, 11 }, { 3, 14 }, { 6, 16 }, { 2, 10 } }) {
            p.dot(back, d[0], d[1], SILVER);
            glow.dot(back, d[0], d[1], STAR_GLOW);
        }
        for (Paint.Face face : Paint.Face.values()) {
            int[] arm = ARM.face(face);
            p.fill(arm, ROBE_DARK, 5);
            for (int y = 10; y < 12; y++) {
                for (int x = 0; x < arm[2]; x++) {
                    p.dot(arm, x, y, p.shade(SKIN, 5));
                }
            }
            int[] leg = LEG.face(face);
            p.fill(leg, ROBE_DARK, 5);
            for (int y = 8; y < 12; y++) {
                for (int x = 0; x < leg[2]; x++) {
                    p.dot(leg, x, y, p.shade(y == 11 ? BOOT_DARK : BOOT, 5));
                }
            }
        }
        p.fill(ARM.face(Paint.Face.BOTTOM), SKIN, 5);
        p.fill(LEG.face(Paint.Face.BOTTOM), BOOT_DARK, 4);
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
