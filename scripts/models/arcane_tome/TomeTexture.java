import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

// Paints the Arcane Tome's 16 pixel item picture (textures/item/arcane_tome.png): a thick violet leather book seen a
// little from its spine side, gold corners and clasp, its pages edged in cream, a glowing four-pointed star on the
// cover and a few sparks round it. From the repository root: java scripts/models/arcane_tome/TomeTexture.java
public final class TomeTexture {
    private static final int SIZE = 16;
    private static final int OUTLINE = 0xFF1A0E26;
    private static final int COVER = 0xFF4B2470;
    private static final int COVER_LIGHT = 0xFF6A36A0;
    private static final int COVER_DARK = 0xFF331850;
    private static final int SPINE = 0xFF2A1240;
    private static final int SPINE_BAND = 0xFFB8862E;
    private static final int PAGES = 0xFFEDE3C8;
    private static final int PAGES_SHADE = 0xFFC9BB98;
    private static final int GOLD = 0xFFE2B33F;
    private static final int GOLD_LIGHT = 0xFFFFE08A;
    private static final int STAR = 0xFFE6F0FF;
    private static final int STAR_GLOW = 0xFFB46CFF;
    private static final int SPARK = 0xFFD9C2FF;

    private TomeTexture() {
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
        // Gold corners and the clasp on the right edge.
        int[][] corners = { { 3, 2 }, { 4, 2 }, { 3, 3 }, { 13, 2 }, { 12, 2 }, { 13, 3 }, { 3, 13 }, { 4, 13 },
                { 3, 12 }, { 13, 13 }, { 12, 13 }, { 13, 12 } };
        for (int[] c : corners) {
            p[c[1]][c[0]] = GOLD;
        }
        p[2][3] = GOLD_LIGHT;
        p[7][13] = GOLD;
        p[8][13] = GOLD_LIGHT;
        p[7][14] = GOLD;
        p[8][14] = GOLD;
        // The star: a glowing violet halo, a pale four-pointed star in it.
        int cx = 8;
        int cy = 7;
        int[][] halo = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 }, { 0, -2 }, { 0, 2 }, { -2, 0 }, { 2, 0 } };
        for (int[] h : halo) {
            p[cy + h[1]][cx + h[0]] = STAR_GLOW;
        }
        int[][] star = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 }, { 0, -3 }, { 0, 3 }, { -3, 0 }, { 3, 0 } };
        for (int[] s : star) {
            p[cy + s[1]][cx + s[0]] = s[0] == 0 && s[1] == 0 ? 0xFFFFFFFF : STAR;
        }
        // A gold frame line round the star.
        for (int x = 5; x <= 11; x++) {
            p[12][x] = x % 2 == 1 ? GOLD : p[12][x];
        }
        int[][] out = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                out[y][x] = p[y][x] != 0 ? p[y][x] : touches(p, x, y) ? OUTLINE : 0;
            }
        }
        int[][] sparks = { { 15, 1 }, { 1, 0 }, { 0, 15 } };
        for (int[] s : sparks) {
            if (out[s[1]][s[0]] == 0) {
                out[s[1]][s[0]] = SPARK;
            }
        }
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, out[y][x]);
            }
        }
        Path file = Path.of(args.length > 0 ? args[0]
                : "src/main/resources/assets/welcomescreen/textures/item/arcane_tome.png");
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
