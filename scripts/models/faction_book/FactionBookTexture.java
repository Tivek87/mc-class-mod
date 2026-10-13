import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

// Paints the Faction Book's 16 pixel item picture (textures/item/faction_book.png): a brown leather book seen a little
// from its spine side, iron corners, its pages edged in cream, a shield on the cover split in three faction colours
// (red, green, gold) under an iron rim, and a cloth bookmark hanging from the bottom.
// From the repository root: java scripts/models/faction_book/FactionBookTexture.java
public final class FactionBookTexture {
    private static final int SIZE = 16;
    private static final int OUTLINE = 0xFF1C110A;
    private static final int COVER = 0xFF6B3E24;
    private static final int COVER_LIGHT = 0xFF8A5432;
    private static final int COVER_DARK = 0xFF4A2A18;
    private static final int SPINE = 0xFF3A2012;
    private static final int SPINE_BAND = 0xFFA9B0B8;
    private static final int PAGES = 0xFFEDE3C8;
    private static final int PAGES_SHADE = 0xFFC9BB98;
    private static final int IRON = 0xFFB8C0CC;
    private static final int IRON_DARK = 0xFF7D8590;
    private static final int RED = 0xFFC8443A;
    private static final int GREEN = 0xFF4CA66A;
    private static final int GOLD = 0xFFE2B33F;
    private static final int RIBBON = 0xFF9E2A2A;

    private FactionBookTexture() {
    }

    public static void main(String[] args) throws Exception {
        int[][] p = new int[SIZE][SIZE];
        // The cover, x 3..13, y 2..13; the spine down its left, the pages showing along its right and bottom.
        for (int y = 2; y <= 13; y++) {
            for (int x = 3; x <= 13; x++) {
                p[y][x] = x == 3 || y == 2 ? COVER_LIGHT : x == 13 || y == 13 ? COVER_DARK : COVER;
            }
        }
        for (int y = 3; y <= 14; y++) {
            p[y][14] = y % 2 == 0 ? PAGES : PAGES_SHADE;
        }
        for (int x = 4; x <= 14; x++) {
            p[14][x] = x % 2 == 0 ? PAGES : PAGES_SHADE;
        }
        for (int y = 2; y <= 14; y++) {
            p[y][2] = SPINE;
        }
        p[4][2] = SPINE_BAND;
        p[11][2] = SPINE_BAND;
        int[][] corners = { { 3, 2 }, { 4, 2 }, { 3, 3 }, { 13, 2 }, { 12, 2 }, { 13, 3 }, { 3, 13 }, { 4, 13 },
                { 3, 12 }, { 13, 13 }, { 12, 13 }, { 13, 12 } };
        for (int[] c : corners) {
            p[c[1]][c[0]] = IRON_DARK;
        }
        p[2][3] = IRON;
        // The shield: x 5..11, y 4..11, its sides drawing in to a point at the bottom.
        int[] inset = { 0, 0, 0, 0, 0, 1, 2, 3 };
        for (int row = 0; row < inset.length; row++) {
            int y = 4 + row;
            for (int x = 5 + inset[row]; x <= 11 - inset[row]; x++) {
                boolean rim = row == 0 || x == 5 + inset[row] || x == 11 - inset[row] || row == inset.length - 1;
                int field = row < 3 ? (x <= 8 ? RED : GREEN) : GOLD;
                p[y][x] = rim ? (row == 0 || x <= 8 ? IRON : IRON_DARK) : field;
            }
        }
        p[5][8] = IRON_DARK;
        p[6][8] = IRON_DARK;
        int[][] out = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                out[y][x] = p[y][x] != 0 ? p[y][x] : touches(p, x, y) ? OUTLINE : 0;
            }
        }
        // The bookmark hangs below the pages.
        out[14][10] = RIBBON;
        out[15][10] = RIBBON;
        out[15][11] = OUTLINE;
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, out[y][x]);
            }
        }
        Path file = Path.of(args.length > 0 ? args[0]
                : "src/main/resources/assets/welcomescreen/textures/item/faction_book.png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("Wrote " + file);
    }

    private static boolean touches(int[][] p, int x, int y) {
        int[][] around = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] step : around) {
            int nx = x + step[0];
            int ny = y + step[1];
            if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && p[ny][nx] != 0) {
                return true;
            }
        }
        return false;
    }
}
