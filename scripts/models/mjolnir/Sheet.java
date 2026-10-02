import java.awt.image.BufferedImage;

final class Sheet {
    static final int SIZE = 64;

    record Rect(int x, int y, int w, int h) {
        Rect sub(int dx, int dy, int w, int h) {
            return new Rect(this.x + dx, this.y + dy, w, h);
        }
    }

    // Top row first: 'r' rune, '.' steel, 'g' gold under the gold pieces, ' ' beside the roof's steps.
    static final String[] FACE = {
        "          .r.          ",
        "       ..r.r.r..       ",
        "    ......rrr......    ",
        ".r..r..g.r.r.r.g..r..r.",
        "r.rrrr..r..r..r..rrrr.r",
        ".r..r..r..ggg..r..r..r.",
        "..r.r..r..ggg..r..r.r..",
        "...r...r.ggggg.r...r...",
        "....gggggg.r.gggggg....",
        "gggg.....g.r.g.....gggg",
    };

    static final Rect FRONT = new Rect(0, 0, 23, 10);
    static final Rect GLOW = new Rect(0, 10, 23, 10);
    static final Rect TOP = new Rect(0, 20, 23, 6);
    static final Rect BOTTOM = new Rect(0, 26, 23, 6);
    static final Rect END = new Rect(23, 0, 6, 7);
    static final Rect RISER = new Rect(23, 7, 6, 1);
    static final Rect COLLAR = new Rect(29, 0, 5, 3);
    static final Rect COLLAR_SIDE = new Rect(29, 3, 5, 3);
    static final Rect COLLAR_DOWN = new Rect(29, 6, 5, 5);
    static final Rect HANDLE = new Rect(34, 0, 3, 15);
    static final Rect HANDLE_SIDE = new Rect(37, 0, 3, 15);
    static final Rect HANDLE_DOWN = new Rect(40, 0, 3, 3);
    static final Rect MEDAL = new Rect(40, 3, 3, 3);
    static final Rect STUD = new Rect(43, 3, 1, 1);
    static final Rect HORN = new Rect(23, 8, 6, 1);
    static final Rect CURL = new Rect(43, 0, 2, 2);
    static final Rect SCROLL = new Rect(45, 0, 1, 3);
    static final Rect SNAKE = new Rect(46, 0, 2, 2);
    static final Rect SNAKE_SIDE = new Rect(48, 0, 2, 2);
    static final Rect SNAKE_DOWN = new Rect(50, 0, 2, 2);
    static final Rect TIP = new Rect(52, 0, 5, 3);
    static final Rect GOLD = new Rect(57, 0, 6, 6);

    private static final int STEEL_HIGH = 0xE3E8EC;
    private static final int STEEL_LIGHT = 0xC7CED4;
    private static final int STEEL = 0xADB5BD;
    private static final int STEEL_SHADE = 0x929AA3;
    private static final int STEEL_DARK = 0x737B84;
    private static final int RUNE = 0x2D3239;
    private static final int GOLD_HIGH = 0xFBE6A0;
    private static final int GOLD_LIGHT = 0xE8BE58;
    private static final int GOLD_MID = 0xC99A3B;
    private static final int GOLD_SHADE = 0x9D7228;
    private static final int GOLD_DARK = 0x6A4818;
    private static final int IRON_LIGHT = 0x7A8189;
    private static final int IRON = 0x5F666E;
    private static final int IRON_DARK = 0x464C53;
    private static final int LEATHER_LIGHT = 0x974832;
    private static final int LEATHER = 0x7A3626;
    private static final int STRAP_LIGHT = 0x5C5552;
    private static final int STRAP = 0x3F3A38;
    private static final int STRAP_DARK = 0x2C2827;

    private final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

    static BufferedImage paint() {
        Sheet sheet = new Sheet();
        sheet.face();
        sheet.glow();
        sheet.steel(TOP, 11, STEEL_LIGHT, STEEL, STEEL_SHADE);
        sheet.steel(BOTTOM, 12, STEEL, STEEL_SHADE, STEEL_DARK);
        sheet.steel(END, 13, STEEL_LIGHT, STEEL, STEEL_SHADE);
        sheet.row(END, 0, STEEL_HIGH);
        sheet.row(END, END.h() - 1, STEEL_SHADE);
        sheet.row(RISER, 0, STEEL_HIGH);
        sheet.art(COLLAR, "YcccY", "cYcYc", "ddZdd");
        sheet.art(COLLAR_SIDE, "ccccc", "dyyyd", "ddddd");
        sheet.art(COLLAR_DOWN, "ddddd", "deeed", "deeed", "deeed", "ddddd");
        sheet.art(HANDLE, "lsl", "sZs", "sls", "lsl", "sZs", "sls", "lsl", "sZs", "sls",
                "YlY", "YsY", "lYl", "YlY", "YlY", "yyy");
        sheet.art(HANDLE_SIDE, "sls", "lsl", "sls", "lsl", "sls", "lsl", "sls", "lsl", "sls",
                "lsl", "sls", "lsl", "sls", "lsl", "yyy");
        sheet.art(HANDLE_DOWN, "yyy", "ywy", "yyy");
        sheet.art(MEDAL, "wYw", "YZY", "wYw");
        sheet.art(STUD, "Z");
        sheet.art(HORN, "YyYyYy");
        sheet.art(CURL, "Yy", "wv");
        sheet.art(SCROLL, "Z", "Y", "y");
        sheet.art(SNAKE, "vY", "yZ");
        sheet.art(SNAKE_SIDE, "yY", "wy");
        sheet.art(SNAKE_DOWN, "wy", "yw");
        sheet.art(TIP, "YY YY", " YZY ", "  y  ");
        sheet.art(GOLD, "YYYYYy", "YYyyyy", "Yyyyyw", "yyyyww", "yyywww", "ywwwwv");
        return sheet.image;
    }

    private void face() {
        for (int y = 0; y < FRONT.h(); y++) {
            for (int x = 0; x < FRONT.w(); x++) {
                char c = FACE[y].charAt(x);
                if (c == ' ') {
                    continue;
                }
                boolean rim = y == 0 || FACE[y - 1].charAt(x) == ' ' || x == 0 || x == FRONT.w() - 1;
                int color = switch (c) {
                    case 'r' -> RUNE;
                    case 'g' -> GOLD_MID;
                    default -> rim ? STEEL_LIGHT : y == FRONT.h() - 1 ? STEEL_SHADE
                            : speckle(x, y, 7, STEEL_LIGHT, STEEL, STEEL_SHADE);
                };
                this.set(FRONT, x, y, color);
            }
        }
    }

    // Light added over the runes, so black adds nothing.
    private void glow() {
        for (int y = 0; y < GLOW.h(); y++) {
            for (int x = 0; x < GLOW.w(); x++) {
                char c = FACE[y].charAt(x);
                int level = 0;
                if (c == 'r') {
                    level = 255;
                } else if (c == '.' && (rune(x - 1, y) || rune(x + 1, y) || rune(x, y - 1) || rune(x, y + 1))) {
                    level = 36;
                }
                this.set(GLOW, x, y, level << 16 | level << 8 | level);
            }
        }
    }

    private static boolean rune(int x, int y) {
        return y >= 0 && y < FACE.length && x >= 0 && x < FACE[y].length() && FACE[y].charAt(x) == 'r';
    }

    private void steel(Rect r, int seed, int light, int base, int shade) {
        for (int y = 0; y < r.h(); y++) {
            for (int x = 0; x < r.w(); x++) {
                this.set(r, x, y, speckle(x, y, seed, light, base, shade));
            }
        }
    }

    private void row(Rect r, int y, int color) {
        for (int x = 0; x < r.w(); x++) {
            this.set(r, x, y, color);
        }
    }

    // One letter a pixel: gold (Z brightest, Y, y, w, v darkest), iron (c, d, e), leather (l), straps (s), ' ' clear.
    private void art(Rect r, String... rows) {
        for (int y = 0; y < r.h(); y++) {
            for (int x = 0; x < r.w(); x++) {
                char c = rows[y].charAt(x);
                if (c != ' ') {
                    this.set(r, x, y, color(c, x, y));
                }
            }
        }
    }

    private static int color(char c, int x, int y) {
        return switch (c) {
            case 'Z' -> GOLD_HIGH;
            case 'Y' -> GOLD_LIGHT;
            case 'y' -> GOLD_MID;
            case 'w' -> GOLD_SHADE;
            case 'v' -> GOLD_DARK;
            case 'c' -> IRON_LIGHT;
            case 'd' -> IRON;
            case 'e' -> IRON_DARK;
            case 'l' -> speckle(x, y, 5, LEATHER_LIGHT, LEATHER, LEATHER);
            case 's' -> speckle(x, y, 9, STRAP_LIGHT, STRAP, STRAP_DARK);
            default -> throw new IllegalArgumentException("no colour for " + c);
        };
    }

    private static int speckle(int x, int y, int seed, int light, int base, int shade) {
        int h = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791);
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        int roll = (h & 0xFF);
        return roll < 36 ? light : roll > 219 ? shade : base;
    }

    private void set(Rect r, int x, int y, int rgb) {
        this.image.setRGB(r.x() + x, r.y() + y, 0xFF000000 | rgb);
    }
}
