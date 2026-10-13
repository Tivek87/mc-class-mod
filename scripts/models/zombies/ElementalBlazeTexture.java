import java.nio.file.Path;

// Paints the Elemental Blaze for the boxes of creature/elemental/client/ElementalBlazeModel: elemental_blaze.png (a
// blaze's golden head with five small marks of the elements on its brow, and its rods: stone with a boulder knot,
// burning fire with a tongue of flame, ice with an icicle, pale wind with a ring of air, dark storm with forking
// sparks), elemental_blaze_glow.png (what of them glows: fire, sparks, the ice's shine, the earth's embers, drawn over
// it in full light) and elemental_blaze_eyes.png (its eyes in white, tinted in game by the element it uses). Then the
// items: the five rods it drops (earth_rod, ember_rod, frost_rod, gale_rod, storm_rod), the Elemental Staff in each of
// its five elements (elemental_staff_<element>) and the Frozen effect's icon (mob_effect/frozen.png). Keep the boxes
// here as the model builds them.
// From the repository root, with Java 22 or newer: java scripts/models/zombies/ElementalBlazeTexture.java
public final class ElementalBlazeTexture {
    private static final String[] ELEMENTS = { "earth", "fire", "ice", "wind", "storm" };
    private static final String[] RODS = { "earth_rod", "ember_rod", "frost_rod", "gale_rod", "storm_rod" };
    private static final int[] GOLD = { 226, 168, 44 };
    private static final int[] GOLD_LIGHT = { 255, 216, 96 };
    private static final int[] GOLD_DARK = { 176, 104, 22 };
    private static final int[] EYE = { 56, 26, 12 };
    private static final int[] WHITE = { 255, 255, 255 };
    // Each element's base, dark, light and glowing colours.
    private static final int[][][] TONES = {
        { { 118, 96, 70 }, { 76, 60, 44 }, { 150, 128, 96 }, { 255, 140, 40 } },
        { { 232, 96, 24 }, { 168, 40, 16 }, { 255, 200, 80 }, { 255, 170, 60 } },
        { { 150, 208, 238 }, { 92, 150, 200 }, { 230, 248, 255 }, { 110, 170, 205 } },
        { { 206, 212, 220 }, { 150, 158, 170 }, { 244, 248, 252 }, { 90, 96, 108 } },
        { { 70, 70, 92 }, { 42, 42, 58 }, { 120, 120, 150 }, { 255, 228, 70 } },
    };

    // Each element's gem on the staff and mark on the brow: green earth, orange fire, blue ice, white wind, yellow
    // storm.
    private static final int[][] GEMS = {
        { 110, 196, 80 }, { 255, 120, 30 }, { 160, 226, 255 }, { 244, 248, 255 }, { 255, 228, 70 },
    };

    private ElementalBlazeTexture() {
    }

    public static void main(String[] args) throws Exception {
        Path entity = Paint.textures(args, "entity");
        Paint p = new Paint(64, 64, 80);
        Paint glow = new Paint(64, 64, 81);
        Paint eyes = new Paint(64, 64, 82);
        head(p, eyes);
        for (int e = 0; e < 5; e++) {
            rods(p, glow, e, false);
            rods(p, glow, e, true);
        }
        p.save(entity, "elemental_blaze");
        glow.save(entity, "elemental_blaze_glow");
        eyes.save(entity, "elemental_blaze_eyes");
        Path items = Paint.textures(args, "item");
        for (int e = 0; e < 5; e++) {
            rodItem(e).save(items, RODS[e]);
            staffItem(e).save(items, "elemental_staff_" + ELEMENTS[e]);
        }
        frozenIcon().save(Paint.textures(args, "mob_effect"), "frozen");
    }

    // The head: mottled gold, darker at the bottom, two dark eyes and a mouth on its face, a mark of each element
    // across its brow.
    private static void head(Paint p, Paint eyes) {
        Paint.Box head = new Paint.Box(0, 0, 8, 8, 8);
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = head.face(face);
            p.fill(f, GOLD, 10);
            for (int k = 0; k < f[2] * f[3] / 5; k++) {
                int[] colour = p.random.nextBoolean() ? GOLD_LIGHT : GOLD_DARK;
                p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), p.shade(colour, 8));
            }
            if (face != Paint.Face.TOP && face != Paint.Face.BOTTOM) {
                p.row(f, f[3] - 1, GOLD_DARK);
            }
        }
        int[] front = head.face(Paint.Face.FRONT);
        for (int x : new int[] { 1, 2, 5, 6 }) {
            p.dot(front, x, 4, EYE);
            eyes.dot(front, x, 4, WHITE);
        }
        p.dot(front, 1, 3, GOLD_DARK);
        p.dot(front, 6, 3, GOLD_DARK);
        for (int x = 2; x < 6; x++) {
            p.dot(front, x, 6, GOLD_DARK);
        }
        int[] marks = { 1, 2, 4, 5, 6 };
        for (int e = 0; e < 5; e++) {
            p.dot(front, marks[e], 1, GEMS[e]);
        }
    }

    // One element's rods: the long one of the upper rings, or the short one of the lowest, and its piece of look.
    private static void rods(Paint p, Paint glow, int e, boolean low) {
        int u = e * 12;
        int[][] tone = TONES[e];
        Paint.Box rod = low ? new Paint.Box(u, 34, 2, 6, 2) : new Paint.Box(u, 16, 2, 8, 2);
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = rod.face(face);
            p.fill(f, tone[0], 8);
            switch (e) {
                case 0 -> {
                    for (int k = 0; k < f[2] * f[3] / 4; k++) {
                        int x = p.random.nextInt(f[2]);
                        int y = p.random.nextInt(f[3]);
                        p.dot(f, x, y, p.shade(tone[1], 6));
                        if (p.random.nextInt(3) == 0) {
                            glow.dot(f, x, y, tone[3]);
                        }
                    }
                    if (p.random.nextBoolean()) {
                        p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), new int[] { 90, 120, 60 });
                    }
                }
                case 1 -> {
                    for (int y = 0; y < f[3]; y++) {
                        for (int x = 0; x < f[2]; x++) {
                            int[] c = (y + x * 3 + p.random.nextInt(3)) % 4 == 0 ? tone[2]
                                    : y > f[3] * 0.6 ? tone[1] : tone[0];
                            p.dot(f, x, y, p.shade(c, 10));
                            glow.dot(f, x, y, p.shade(y < f[3] * 0.6 ? tone[3] : tone[1], 10));
                        }
                    }
                }
                case 2 -> {
                    for (int y = 0; y < f[3]; y++) {
                        for (int x = 0; x < f[2]; x++) {
                            boolean streak = (x + y) % 3 == 0;
                            p.dot(f, x, y, p.shade(streak ? tone[2] : y % 4 == 3 ? tone[1] : tone[0], 6));
                            if (streak) {
                                glow.dot(f, x, y, p.shade(tone[3], 6), 255);
                            }
                        }
                    }
                }
                case 3 -> {
                    for (int y = 0; y < f[3]; y++) {
                        for (int x = 0; x < f[2]; x++) {
                            boolean swirl = (y - x * 2 + 8) % 4 == 0;
                            p.dot(f, x, y, p.shade(swirl ? tone[2] : (y + x) % 4 == 2 ? tone[1] : tone[0], 5));
                            if (swirl && y % 2 == 0) {
                                glow.dot(f, x, y, p.shade(tone[3], 4));
                            }
                        }
                    }
                }
                default -> {
                    if (face == Paint.Face.TOP || face == Paint.Face.BOTTOM) {
                        break;
                    }
                    int x = p.random.nextInt(f[2]);
                    for (int y = p.random.nextInt(2); y < f[3]; y += 1 + p.random.nextInt(2)) {
                        if (p.random.nextInt(3) != 0) {
                            p.dot(f, x, y, tone[3]);
                            glow.dot(f, x, y, tone[3]);
                        }
                        x = Math.max(0, Math.min(f[2] - 1, x + p.random.nextInt(3) - 1));
                    }
                }
            }
        }
        int w = low ? 42 : 26;
        switch (e) {
            case 0 -> {
                Paint.Box knot = low ? new Paint.Box(u, w, 2, 2, 2) : new Paint.Box(u, w, 3, 3, 3);
                for (Paint.Face face : Paint.Face.values()) {
                    int[] f = knot.face(face);
                    p.fill(f, new int[] { 128, 124, 118 }, 12);
                    p.dot(f, p.random.nextInt(f[2]), p.random.nextInt(f[3]), new int[] { 70, 66, 62 });
                    int x = p.random.nextInt(f[2]);
                    int y = p.random.nextInt(f[3]);
                    glow.dot(f, x, y, new int[] { 200, 90, 30 });
                }
            }
            case 1 -> piece(p, glow, new Paint.Box(u, w, 1, 2, 1), new int[] { 255, 236, 140 },
                    new int[] { 255, 250, 200 });
            case 2 -> piece(p, glow, new Paint.Box(u, w, 1, low ? 2 : 3, 1), new int[] { 236, 250, 255 },
                    new int[] { 200, 240, 255 });
            case 3 -> {
                Paint.Box ring = new Paint.Box(u, w, 3, 1, 3);
                for (Paint.Face face : Paint.Face.values()) {
                    int[] f = ring.face(face);
                    p.fill(f, new int[] { 238, 244, 250 }, 6);
                    glow.fill(f, new int[] { 150, 160, 180 }, 6);
                }
            }
            default -> {
                piece(p, glow, new Paint.Box(u, w, 1, 2, 1), TONES[4][3], new int[] { 255, 240, 120 });
                piece(p, glow, new Paint.Box(u + 4, w, 1, 2, 1), TONES[4][3], new int[] { 255, 240, 120 });
            }
        }
    }

    // A small piece in one colour that glows in another.
    private static void piece(Paint p, Paint glow, Paint.Box box, int[] colour, int[] light) {
        for (Paint.Face face : Paint.Face.values()) {
            int[] f = box.face(face);
            p.fill(f, colour, 6);
            glow.fill(f, light, 6);
        }
    }

    // A rod lying corner to corner as a blaze rod does, in its element's colours, with its own mark: a knot of stone,
    // a flame, an icicle, a swirl, a fork of lightning.
    private static Paint rodItem(int e) {
        Paint p = new Paint(16, 16, 90 + e);
        int[] all = { 0, 0, 16, 16 };
        int[][] tone = TONES[e];
        for (int k = 2; k <= 13; k++) {
            int x = k;
            int y = 15 - k;
            p.dot(all, x, y, p.shade(tone[0], 8));
            p.dot(all, x - 1, y, p.shade(tone[1], 6));
            p.dot(all, x, y - 1, p.shade(tone[2], 6));
        }
        switch (e) {
            case 0 -> {
                int[] stone = { 128, 124, 118 };
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        p.dot(all, 7 + dx, 8 + dy, p.shade(Math.abs(dx) + Math.abs(dy) == 2 ? tone[1] : stone, 10));
                    }
                }
                p.dot(all, 7, 8, tone[3]);
            }
            case 1 -> {
                p.dot(all, 14, 1, tone[2]);
                p.dot(all, 13, 0, new int[] { 255, 240, 160 });
                p.dot(all, 15, 0, tone[3]);
                p.dot(all, 14, 0, new int[] { 255, 250, 210 });
            }
            case 2 -> {
                p.dot(all, 1, 15, tone[2]);
                p.dot(all, 0, 15, WHITE);
                p.dot(all, 14, 1, tone[2]);
                p.dot(all, 15, 0, WHITE);
            }
            case 3 -> {
                for (int a = 0; a < 8; a++) {
                    double t = a / 8.0 * Math.PI * 2.0;
                    p.dot(all, 8 + (int) Math.round(Math.cos(t) * 3.0), 7 + (int) Math.round(Math.sin(t) * 2.0),
                            new int[] { 244, 248, 252 });
                }
            }
            default -> {
                int[][] fork = { { 9, 4 }, { 10, 5 }, { 11, 4 }, { 12, 3 }, { 4, 10 }, { 5, 11 }, { 4, 12 } };
                for (int[] at : fork) {
                    p.dot(all, at[0], at[1], tone[3]);
                }
            }
        }
        return p;
    }

    // The staff: a dark gold shaft corner to corner, a ring at its head holding a small gem of each element, the gem of
    // the element it is set to big and bright at the top.
    private static Paint staffItem(int chosen) {
        Paint p = new Paint(16, 16, 100 + chosen);
        int[] all = { 0, 0, 16, 16 };
        int[] shaft = { 120, 76, 28 };
        int[] shaftDark = { 80, 46, 16 };
        int[] shaftLight = { 196, 140, 56 };
        for (int k = 1; k <= 10; k++) {
            int x = k;
            int y = 15 - k;
            p.dot(all, x, y, p.shade(shaft, 6));
            p.dot(all, x - 1, y, shaftDark);
            if (k % 3 == 0) {
                p.dot(all, x, y - 1, shaftLight);
            }
        }
        int cx = 11;
        int cy = 4;
        for (int a = 0; a < 12; a++) {
            double t = a / 12.0 * Math.PI * 2.0;
            p.dot(all, cx + (int) Math.round(Math.cos(t) * 3.0), cy + (int) Math.round(Math.sin(t) * 3.0),
                    a % 2 == 0 ? GOLD : GOLD_DARK);
        }
        int[][] spots = { { 8, 4 }, { 11, 7 }, { 14, 4 }, { 9, 2 }, { 13, 2 } };
        for (int e = 0; e < 5; e++) {
            p.dot(all, spots[e][0], spots[e][1], GEMS[e]);
        }
        int[][] tone = TONES[chosen];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (Math.abs(dx) + Math.abs(dy) < 2) {
                    p.dot(all, cx + dx, cy + dy, dx == 0 && dy == 0 ? WHITE : GEMS[chosen]);
                }
            }
        }
        p.dot(all, cx + 1, cy + 1, tone[1]);
        p.dot(all, cx, cy - 3, tone[2]);
        return p;
    }

    // A pale blue snowflake: six arms, each with a pair of twigs, on a faint frost.
    private static Paint frozenIcon() {
        Paint p = new Paint(18, 18, 18);
        int[] all = { 0, 0, 18, 18 };
        int[] edge = { 40, 90, 140 };
        int[] ice = { 150, 220, 255 };
        int[] bright = { 236, 250, 255 };
        for (int arm = 0; arm < 6; arm++) {
            double t = arm * Math.PI / 3.0;
            for (int r = 0; r <= 7; r++) {
                int x = 9 + (int) Math.round(Math.cos(t) * r - 0.5);
                int y = 9 + (int) Math.round(Math.sin(t) * r - 0.5);
                p.dot(all, x, y, r > 5 ? edge : r < 2 ? bright : ice);
            }
            for (int side = -1; side <= 1; side += 2) {
                double twig = t + side * 0.7;
                for (int r = 1; r <= 2; r++) {
                    int x = 9 + (int) Math.round(Math.cos(t) * 4.5 + Math.cos(twig) * r - 0.5);
                    int y = 9 + (int) Math.round(Math.sin(t) * 4.5 + Math.sin(twig) * r - 0.5);
                    p.dot(all, x, y, ice);
                }
            }
        }
        p.dot(all, 8, 8, WHITE);
        p.dot(all, 9, 9, WHITE);
        return p;
    }
}
