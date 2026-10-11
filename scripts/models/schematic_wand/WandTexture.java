import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

// Paints the Schematic Wand's 16 pixel item picture (textures/item/schematic_wand.png): a dark wooden staff with a
// blue grip and a steel collar, holding up a glowing hologram cube, a few sparks around it. From the repository root:
// java scripts/models/schematic_wand/WandTexture.java
public final class WandTexture {
    private static final int SIZE = 16;
    private static final int HANDLE = 0xFF5A4030;
    private static final int HANDLE_LIGHT = 0xFF86633F;
    private static final int GRIP = 0xFF2F4A6E;
    private static final int GRIP_LIGHT = 0xFF4A6FA0;
    private static final int METAL = 0xFF8E9BAA;
    private static final int METAL_LIGHT = 0xFFD4DCE4;
    private static final int TOP = 0xFFC8F4FF;
    private static final int LEFT = 0xFF5CCBF2;
    private static final int RIGHT = 0xFF2E8CC4;
    private static final int EDGE = 0xFFF2FDFF;
    private static final int OUTLINE = 0xFF141C26;
    private static final int GLOW = 0xFF1B5A86;
    private static final int SPARK = 0xFFE8FBFF;

    private WandTexture() {
    }

    public static void main(String[] args) throws Exception {
        int[][] pixels = new int[SIZE][SIZE];
        boolean[][] staff = new boolean[SIZE][SIZE];
        boolean[][] cube = new boolean[SIZE][SIZE];
        for (int k = 0; k <= 7; k++) {
            int x = 1 + k;
            int y = 14 - k;
            boolean grip = k >= 1 && k <= 3;
            boolean collar = k >= 6;
            set(pixels, staff, x, y, collar ? METAL : grip ? GRIP : HANDLE);
            set(pixels, staff, x + 1, y, collar ? METAL_LIGHT : grip ? GRIP_LIGHT : HANDLE_LIGHT);
        }
        fill(pixels, cube, new double[][] { { 11, 0.5 }, { 14.6, 2.3 }, { 11, 4.1 }, { 7.4, 2.3 } }, TOP);
        fill(pixels, cube, new double[][] { { 7.4, 2.3 }, { 11, 4.1 }, { 11, 8.0 }, { 7.4, 6.2 } }, LEFT);
        fill(pixels, cube, new double[][] { { 11, 4.1 }, { 14.6, 2.3 }, { 14.6, 6.2 }, { 11, 8.0 } }, RIGHT);
        for (int y = 2; y <= 7; y++) {
            set(pixels, cube, 11, y, EDGE);
        }
        set(pixels, cube, 11, 1, EDGE);
        set(pixels, cube, 8, 2, EDGE);
        set(pixels, cube, 14, 2, EDGE);
        int[][] outlined = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                outlined[y][x] = pixels[y][x];
                if (pixels[y][x] != 0) {
                    continue;
                }
                if (touches(staff, x, y)) {
                    outlined[y][x] = OUTLINE;
                } else if (touches(cube, x, y)) {
                    outlined[y][x] = GLOW;
                }
            }
        }
        int[][] sparks = { { 15, 0 }, { 5, 1 }, { 14, 9 }, { 3, 6 } };
        for (int[] spark : sparks) {
            if (outlined[spark[1]][spark[0]] == 0) {
                outlined[spark[1]][spark[0]] = SPARK;
            }
        }
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setRGB(x, y, outlined[y][x]);
            }
        }
        Path out = Path.of(args.length > 0 ? args[0]
                : "src/main/resources/assets/welcomescreen/textures/item/schematic_wand.png");
        Files.createDirectories(out.getParent());
        ImageIO.write(image, "png", out.toFile());
        System.out.println("Wrote " + out);
    }

    private static void set(int[][] pixels, boolean[][] part, int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
            pixels[y][x] = color;
            part[y][x] = true;
        }
    }

    // Every pixel whose middle lies inside the outline.
    private static void fill(int[][] pixels, boolean[][] part, double[][] corners, int color) {
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo(corners[0][0], corners[0][1]);
        for (int k = 1; k < corners.length; k++) {
            shape.lineTo(corners[k][0], corners[k][1]);
        }
        shape.closePath();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (shape.contains(x + 0.5, y + 0.5)) {
                    set(pixels, part, x, y, color);
                }
            }
        }
    }

    private static boolean touches(boolean[][] part, int x, int y) {
        int[][] around = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] step : around) {
            int nx = x + step[0];
            int ny = y + step[1];
            if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && part[ny][nx]) {
                return true;
            }
        }
        return false;
    }
}
