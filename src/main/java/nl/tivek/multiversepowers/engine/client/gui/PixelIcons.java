package nl.tivek.multiversepowers.engine.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

// Small 12 by 12 pixel icons for menus, drawn as runs of filled pixels in one colour, with the game's own text shadow
// under them when asked.
public final class PixelIcons {
    public static final int SIZE = 12;
    private static final int SHADOW = 0x3F3F3F;

    public enum Icon {
        DOWNLOAD(
                ".....##.....",
                ".....##.....",
                ".....##.....",
                ".....##.....",
                "..########..",
                "...######...",
                "....####....",
                ".....##.....",
                "............",
                "#..........#",
                "#..........#",
                "############"),
        BUG(
                "...#....#...",
                "....#..#....",
                "...######...",
                "#.###..###.#",
                ".####..####.",
                "..###..###..",
                "#.###..###.#",
                ".####..####.",
                "..###..###..",
                "#.###..###.#",
                "...######...",
                "............"),
        IDEA(
                "....####....",
                "..##....##..",
                ".#........#.",
                ".#...##...#.",
                ".#....#...#.",
                "..#...#..#..",
                "...#.....#..",
                "....#####...",
                "....#####...",
                "....#####...",
                ".....###....",
                "............"),
        INBOX(
                "..########..",
                "..#......#..",
                "..#.####.#..",
                "..#......#..",
                "..#.###..#..",
                "#.#......#.#",
                "#.########.#",
                "#..........#",
                "####....####",
                "#..######..#",
                "#..........#",
                "############"),
        SPARK(
                ".....##.....",
                ".....##.....",
                "....####....",
                "....####....",
                "..########..",
                "############",
                "############",
                "..########..",
                "....####....",
                "....####....",
                ".....##.....",
                ".....##....."),
        CHECK(
                "............",
                "...........#",
                "..........##",
                ".........##.",
                "........##..",
                "#......##...",
                "##....##....",
                ".##..##.....",
                "..####......",
                "...##.......",
                "............",
                "............"),
        WARNING(
                ".....##.....",
                "....####....",
                "....#..#....",
                "...##..##...",
                "...#.##.#...",
                "..##.##.##..",
                "..#..##..#..",
                ".##..##..##.",
                ".#........#.",
                "##...##...##",
                "#..........#",
                "############"),
        CLOSE(
                "............",
                ".##......##.",
                "..##....##..",
                "...##..##...",
                "....####....",
                ".....##.....",
                "....####....",
                "...##..##...",
                "..##....##..",
                ".##......##.",
                "............",
                "............"),
        PLAY(
                "............",
                "..##........",
                "..####......",
                "..######....",
                "..########..",
                "..#########.",
                "..#########.",
                "..########..",
                "..######....",
                "..####......",
                "..##........",
                "............"),
        SEND(
                "............",
                "##..........",
                "#####.......",
                "#..#####....",
                "#.....#####.",
                "##.........#",
                "##.........#",
                "#.....#####.",
                "#..#####....",
                "#####.......",
                "##..........",
                "............"),
        GEAR(
                "....#..#....",
                "..#.####.#..",
                ".##########.",
                "..###..###..",
                ".###....###.",
                "####....####",
                "####....####",
                ".###....###.",
                "..###..###..",
                ".##########.",
                "..#.####.#..",
                "....#..#...."),
        SCREEN(
                "............",
                "############",
                "#..........#",
                "#..........#",
                "#..........#",
                "#..........#",
                "#..........#",
                "############",
                ".....##.....",
                "....####....",
                "..########..",
                "............"),
        GLOBE(
                "....####....",
                "..##.##.##..",
                ".#..#..#..#.",
                "#...#..#...#",
                "############",
                "#...#..#...#",
                "#...#..#...#",
                "############",
                "#...#..#...#",
                ".#..#..#..#.",
                "..##.##.##..",
                "....####...."),
        BATTERY(
                "............",
                "............",
                "##########..",
                "#........#..",
                "#.#####..###",
                "#.#####..###",
                "#.#####..###",
                "#........#..",
                "##########..",
                "............",
                "............",
                "............"),
        PERSON(
                "....####....",
                "...######...",
                "...######...",
                "...######...",
                "....####....",
                "............",
                "..########..",
                ".##########.",
                ".##########.",
                ".##########.",
                ".##########.",
                "............"),
        LOCK(
                "............",
                "....####....",
                "...#....#...",
                "...#....#...",
                "...#....#...",
                "..########..",
                "..########..",
                "..###..###..",
                "..###..###..",
                "..########..",
                "..########..",
                "............");

        // Each row as runs: start, length, start, length...
        private final int[][] runs;

        Icon(String... rows) {
            this.runs = new int[rows.length][];
            for (int y = 0; y < rows.length; y++) {
                String row = rows[y];
                int[] found = new int[SIZE];
                int count = 0;
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) == '#' && (x == 0 || row.charAt(x - 1) != '#')) {
                        int end = x;
                        while (end < row.length() && row.charAt(end) == '#') {
                            end++;
                        }
                        found[count++] = x;
                        found[count++] = end - x;
                    }
                }
                this.runs[y] = java.util.Arrays.copyOf(found, count);
            }
        }
    }

    private PixelIcons() {
    }

    // At `scale` screen units a pixel; `shadow` puts the game's dark text shadow one pixel down and right.
    public static void draw(GuiGraphics graphics, Icon icon, int x, int y, int scale, int argb, boolean shadow) {
        if (shadow) {
            fill(graphics, icon, x + scale, y + scale, scale, (argb & 0xFF000000) | SHADOW);
        }
        fill(graphics, icon, x, y, scale, argb);
    }

    private static void fill(GuiGraphics graphics, Icon icon, int x, int y, int scale, int argb) {
        for (int row = 0; row < icon.runs.length; row++) {
            int[] runs = icon.runs[row];
            for (int i = 0; i < runs.length; i += 2) {
                int left = x + runs[i] * scale;
                int top = y + row * scale;
                graphics.fill(left, top, left + runs[i + 1] * scale, top + scale, argb);
            }
        }
    }

    // Eight dots round a ring, the brightest going round once a second: something is busy.
    public static void spinner(GuiGraphics graphics, int x, int y, int rgb, long millis) {
        int lead = (int) (millis / 125L % 8L);
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8.0F;
            int px = x + 5 + Math.round(Mth.cos(angle) * 4.0F);
            int py = y + 5 + Math.round(Mth.sin(angle) * 4.0F);
            int age = (lead - i + 8) % 8;
            int alpha = Math.max(40, 255 - age * 34);
            graphics.fill(px, py, px + 2, py + 2, alpha << 24 | rgb & 0xFFFFFF);
        }
    }
}
