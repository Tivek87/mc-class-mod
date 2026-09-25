package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The boxes of the cartoon landing-slam constructs (see {@link SlamCartoon}): raised letters made of pixels, lists of
 * boxes joined into one, and boxes turned to face every side.
 */
final class SlamLetters {
    private SlamLetters() {
    }

    /** Letters five pixels tall, row by row from the top: '#' is a pixel, anything else is empty. */
    private static final Map<Character, String[]> FONT = Map.of(
            '1', new String[] { ".#.", "##.", ".#.", ".#.", "###" },
            'T', new String[] { "###", ".#.", ".#.", ".#.", ".#." },
            'O', new String[] { "###", "#.#", "#.#", "#.#", "###" },
            'N', new String[] { "#..#", "##.#", "#.##", "#..#", "#..#" },
            ' ', new String[] { ".", ".", ".", ".", "." });

    /**
     * Raised letters made of pixels, standing up in the plane of x and y: {@code text} centred on x = 0 with its foot
     * at y = {@code bottom}, each pixel {@code pixel} wide and sticking out from z = {@code back} to z = {@code front}.
     * The letters run along +x, his right, so they read from left to right on a side that faces him (-z).
     */
    static double[][] text(String text, double bottom, double pixel, double back, double front,
            double bright) {
        int width = -1;
        for (char c : text.toCharArray()) {
            width += FONT.get(c)[0].length() + 1;
        }
        List<double[]> boxes = new ArrayList<>();
        double x = -0.5 * width * pixel;
        for (char c : text.toCharArray()) {
            String[] glyph = FONT.get(c);
            boolean[][] used = new boolean[glyph.length][glyph[0].length()];
            for (int row = 0; row < glyph.length; row++) {
                String line = glyph[row];
                for (int from = 0; from < line.length(); from++) {
                    if (line.charAt(from) != '#' || used[row][from]) {
                        continue;
                    }
                    int to = from;
                    while (to < line.length() && line.charAt(to) == '#' && !used[row][to]) {
                        to++;
                    }
                    // As few boxes as it takes: a run of pixels goes on down as long as the rows below have it too.
                    int last = row;
                    while (last + 1 < glyph.length && filled(glyph[last + 1], used[last + 1], from, to)) {
                        last++;
                    }
                    for (int r = row; r <= last; r++) {
                        for (int i = from; i < to; i++) {
                            used[r][i] = true;
                        }
                    }
                    double y = bottom + (glyph.length - 1 - last) * pixel;
                    boxes.add(new double[] { x + from * pixel, y, Math.min(back, front), x + to * pixel,
                            y + (last - row + 1) * pixel, Math.max(back, front), bright });
                    from = to - 1;
                }
            }
            x += (glyph[0].length() + 1) * pixel;
        }
        return boxes.toArray(double[][]::new);
    }

    /** Whether a row of a letter has a pixel from {@code from} up to {@code to} that no box has taken yet. */
    private static boolean filled(String line, boolean[] used, int from, int to) {
        for (int i = from; i < to; i++) {
            if (line.charAt(i) != '#' || used[i]) {
                return false;
            }
        }
        return true;
    }

    /** Boxes one after the other in one list. */
    static double[][] join(double[][]... parts) {
        int count = 0;
        for (double[][] part : parts) {
            count += part.length;
        }
        double[][] all = new double[count][];
        int n = 0;
        for (double[][] part : parts) {
            for (double[] box : part) {
                all[n++] = box;
            }
        }
        return all;
    }

    /**
     * Boxes four times over: as they are and turned a quarter, a half and three quarters round the y axis, so what
     * is on the side facing him is on every side.
     */
    static double[][] quarters(double[][] boxes) {
        double[][] all = new double[boxes.length * 4][];
        for (int b = 0; b < boxes.length; b++) {
            double[] box = boxes[b];
            double x0 = box[0];
            double z0 = box[2];
            double x1 = box[3];
            double z1 = box[5];
            for (int q = 0; q < 4; q++) {
                all[4 * b + q] = new double[] { x0, box[1], z0, x1, box[4], z1, box[6] };
                // A quarter turn takes (x, z) to (z, -x).
                double nx0 = z0;
                double nx1 = z1;
                double nz0 = -x1;
                double nz1 = -x0;
                x0 = nx0;
                x1 = nx1;
                z0 = nz0;
                z1 = nz1;
            }
        }
        return all;
    }
}
