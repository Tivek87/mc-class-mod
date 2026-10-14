import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

// Paints the flamingos' textures (textures/entity/flamingo.png, flamingo_greater.png and flamingo_chick.png) from
// scratch, face by face as the Caribbean flamingo was designed (flamingo_demo.html): two pixels of texture to one of
// the model, every face painted by the design's own painters (layered plumage running from one colour at its top to
// another at its bottom, rows of scalloped coverts, a fine barbule grain, a soft rounded shading, the black flight
// feathers peeking at a wing's lower edge), the head's bare pale lore with a yellow eye and its pupil, a two-tone bill
// with its black hook, pink stilt legs with the swollen coral joint and the webbed foot's ridges. The American flamingo
// in the design's own colours; the greater flamingo paler, almost white, with rose-red wings; the chick in grey down.
// The boxes are laid out in the texture as FlamingoModel lays them out (its Sheet).
// Run from the repository root: java scripts/models/animals/FlamingoTexture.java [assets folder]
public final class FlamingoTexture {
    static final int WIDTH = 128;
    static final int SCALE = 2;

    interface Face {
        void paint(int w, int h, Canvas c);
    }

    // One face's pixels in the design's own orientation.
    interface Canvas {
        void set(int x, int y, String hex);
    }

    record Box(float w, float h, float d, Map<Integer, Face> faces) {
    }

    static Map<String, String> american() {
        Map<String, String> p = new HashMap<>();
        p.put("coralDeep", "#e04f38");
        p.put("coralMid", "#f26649");
        p.put("coralBright", "#fc7e62");
        p.put("wingPale", "#fbd2c6");
        p.put("wingSalmon", "#f89e89");
        p.put("wingCoral", "#ee6146");
        p.put("flightBlack", "#16161a");
        p.put("flightCharcoal", "#27272e");
        p.put("billCream", "#f6efe4");
        p.put("billBlush", "#f59c86");
        p.put("billCoral", "#eb5e45");
        p.put("billBlack", "#141418");
        p.put("billHighlight", "#32323a");
        p.put("eyeYellow", "#fef08a");
        p.put("eyePupil", "#111113");
        p.put("loreWhite", "#f5ebe4");
        p.put("legPink", "#dca09e");
        p.put("legShade", "#bd8180");
        p.put("jointCoral", "#df5a5c");
        p.put("jointDeep", "#ba3f44");
        p.put("chickLight", "#eceae4");
        p.put("chickMid", "#d5d1c8");
        p.put("chickShade", "#b8b3a8");
        p.put("chickLeg", "#d98a8c");
        return p;
    }

    // The greater flamingo: the same bird, pale pink and almost white, its wing coverts rose red.
    static Map<String, String> greater() {
        Map<String, String> p = american();
        p.put("coralDeep", "#eeb0b2");
        p.put("coralMid", "#f4c6c6");
        p.put("coralBright", "#f9d8d6");
        p.put("wingPale", "#fbeae8");
        p.put("wingSalmon", "#f2b4b8");
        p.put("wingCoral", "#dc4c5c");
        p.put("billCream", "#f6e8e8");
        p.put("billBlush", "#f2a8b2");
        p.put("billCoral", "#ec8c9a");
        p.put("legPink", "#e2a6ac");
        p.put("legShade", "#c48a92");
        p.put("jointCoral", "#d8707e");
        p.put("jointDeep", "#b85462");
        return p;
    }

    public static void main(String[] args) throws IOException {
        Path folder = Pelt.textures(args, "entity");
        paint(folder, "flamingo", american(), false);
        paint(folder, "flamingo_greater", greater(), false);
        paint(folder, "flamingo_chick", american(), true);
    }

    static void paint(Path folder, String name, Map<String, String> pal, boolean chick) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH * SCALE, WIDTH * SCALE, BufferedImage.TYPE_INT_ARGB);
        Sheet sheet = new Sheet(image);
        String bodyTop = pal.get(chick ? "chickLight" : "wingPale");
        String bodyMid = pal.get(chick ? "chickMid" : "wingSalmon");
        String bodyDeep = pal.get(chick ? "chickShade" : "coralMid");
        String neckTop = pal.get(chick ? "chickLight" : "coralBright");
        String neckDeep = pal.get(chick ? "chickMid" : "coralDeep");
        // The body: pale on top, deeper below, its sides shading toward the breast's colour at the front.
        Map<Integer, Face> torso = new HashMap<>();
        torso.put(0, (w, h, c) -> plumage(w, h, c, pal, bodyTop, bodyDeep, bodyMid, neckDeep, true, false, 11));
        torso.put(1, (w, h, c) -> plumage(w, h, c, pal, bodyTop, bodyDeep, neckDeep, bodyMid, true, false, 12));
        torso.put(2, (w, h, c) -> plumage(w, h, c, pal, bodyTop, bodyMid, null, null, true, false, 13));
        torso.put(3, (w, h, c) -> plumage(w, h, c, pal, bodyMid, bodyDeep, null, null, false, false, 14));
        torso.put(4, (w, h, c) -> plumage(w, h, c, pal, neckTop, bodyMid, null, null, true, false, 15));
        torso.put(5, (w, h, c) -> plumage(w, h, c, pal, bodyMid, bodyDeep, null, null, true, false, 16));
        sheet.box(new Box(6.6F, 6.0F, 9.6F, torso));
        sheet.box(all(5.8F, 5.0F, 3.2F, (w, h, c) -> plumage(w, h, c, pal, neckTop, neckDeep, null, null, true,
                false, 17)));
        sheet.box(all(5.2F, 3.8F, 4.6F, (w, h, c) -> plumage(w, h, c, pal, pal.get(chick ? "chickMid" : "wingSalmon"),
                pal.get(chick ? "chickShade" : "coralDeep"), null, null, true, false, 21)));
        if (!chick) {
            sheet.box(all(3.8F, 2.2F, 4.4F, (w, h, c) -> plumage(w, h, c, pal, pal.get("coralMid"),
                    pal.get("coralDeep"), null, null, true, false, 22)));
        }
        sheet.box(all(1.6F, 5.2F, 9.6F, (w, h, c) -> plumage(w, h, c, pal, pal.get(chick ? "chickLight"
                : "wingPale"), pal.get(chick ? "chickShade" : "wingCoral"), null, null, true, !chick, 31)));
        if (!chick) {
            sheet.box(all(1.2F, 3.6F, 4.4F, (w, h, c) -> plumage(w, h, c, pal, pal.get("wingSalmon"),
                    pal.get("coralDeep"), null, null, true, true, 33)));
            // The black flight feathers under the plume, a band of coral coverts along their top.
            sheet.box(all(0.7F, 4.0F, 8.6F, (w, h, c) -> {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        boolean covert = y < Math.floor(h * 0.35);
                        c.set(x, y, covert ? pal.get("wingCoral") : (x + y) % 2 == 0 ? pal.get("flightBlack")
                                : pal.get("flightCharcoal"));
                    }
                }
            }));
        }
        float[][] neck = chick ? new float[][] { { 2.8F, 2.6F, 2.8F }, { 2.5F, 2.4F, 2.5F }, { 2.3F, 2.2F, 2.3F },
                { 2.2F, 2.0F, 2.2F }, { 2.1F, 1.8F, 2.1F } }
                : new float[][] { { 3.1F, 3.8F, 3.1F }, { 2.7F, 3.8F, 2.7F }, { 2.5F, 4.0F, 2.5F },
                        { 2.4F, 3.8F, 2.4F }, { 2.3F, 3.2F, 2.4F } };
        for (int k = 0; k < 5; k++) {
            int seed = 40 + k;
            sheet.box(all(neck[k][0], neck[k][1], neck[k][2], (w, h, c) -> plumage(w, h, c, pal, neckTop, neckDeep,
                    null, null, false, false, seed)));
        }
        Map<Integer, Face> cranium = new HashMap<>();
        cranium.put(0, (w, h, c) -> side(w, h, c, pal, neckTop, neckDeep, chick, false));
        cranium.put(1, (w, h, c) -> side(w, h, c, pal, neckTop, neckDeep, chick, true));
        cranium.put(-1, (w, h, c) -> plumage(w, h, c, pal, neckTop, neckDeep, null, null, false, false, 53));
        sheet.box(new Box(3.0F, 3.0F, 3.6F, cranium));
        String lid = pal.get(chick ? "chickMid" : "coralMid");
        sheet.box(all(0.1F, 0.95F, 0.95F, (w, h, c) -> fill(w, h, c, lid)));
        // The bill: a pale ivory ridge over a warm coral cheek, dark where it meets the hook.
        sheet.box(all(2.2F, 1.9F, 2.8F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (chick) {
                        c.set(x, y, y > h * 0.6 ? pal.get("billBlack") : pal.get("chickShade"));
                    } else {
                        String col = pal.get("billCream");
                        if (y > h * 0.45) {
                            col = pal.get("billCoral");
                        }
                        if (x > w * 0.72 && y > h * 0.5) {
                            col = pal.get("billBlack");
                        }
                        c.set(x, y, col);
                    }
                }
            }
        }));
        // The hook: black, an ivory bridge along its top, a sheen down one edge.
        sheet.box(all(1.85F, 2.9F, 1.7F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean bridge = y == 0 && !chick;
                    c.set(x, y, bridge ? pal.get("billCream") : x == 0 ? pal.get("billHighlight")
                            : pal.get("billBlack"));
                }
            }
        }));
        sheet.box(all(1.65F, 1.1F, 2.3F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    c.set(x, y, y < h * 0.4 ? pal.get("billCoral") : pal.get("billBlack"));
                }
            }
        }));
        float thick = chick ? 1.25F : 1.05F;
        float upper = chick ? 4.2F : 7.2F;
        float lower = chick ? 4.4F : 7.4F;
        String legPink = pal.get(chick ? "chickLeg" : "legPink");
        sheet.box(all(thick, upper, thick, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    String base = y >= h - 3 ? pal.get("jointCoral") : legPink;
                    c.set(x, y, x == 0 ? shade(base, 0.9) : base);
                }
            }
        }));
        sheet.box(all(thick * 1.38F, 1.5F, thick * 1.45F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    c.set(x, y, y % 2 == 0 ? pal.get("jointCoral") : pal.get("jointDeep"));
                }
            }
        }));
        sheet.box(all(thick * 0.92F, lower, thick * 0.92F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    String base = y < 2 ? pal.get("jointCoral") : legPink;
                    c.set(x, y, y % 2 == 0 && x == 0 ? pal.get("legShade") : base);
                }
            }
        }));
        sheet.box(all(2.9F, 0.55F, 3.8F, (w, h, c) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean ridge = x == 1 || x == w / 2 || x == w - 2;
                    c.set(x, y, ridge ? pal.get("jointCoral") : pal.get("legPink"));
                }
            }
        }));
        ImageIO.write(image, "png", folder.resolve(name + ".png").toFile());
    }

    static Box all(float w, float h, float d, Face face) {
        Map<Integer, Face> faces = new HashMap<>();
        faces.put(-1, face);
        return new Box(w, h, d, faces);
    }

    static void fill(int w, int h, Canvas c, String hex) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                c.set(x, y, hex);
            }
        }
    }

    // A side of the head: plumage, a patch of bare pale lore toward the bill, and the yellow eye with its pupil.
    static void side(int w, int h, Canvas c, Map<String, String> pal, String top, String deep, boolean chick,
            boolean right) {
        plumage(w, h, c, pal, top, deep, null, null, false, false, right ? 52 : 51);
        int from = right ? 0 : (int) Math.floor(w * 0.62);
        int to = right ? (int) Math.floor(w * 0.38) : w;
        for (int y = (int) Math.floor(h * 0.3); y < Math.floor(h * 0.75); y++) {
            for (int x = from; x < to; x++) {
                c.set(x, y, pal.get("loreWhite"));
            }
        }
        int ex = (int) Math.floor(w * (right ? 0.32 : 0.56));
        int ey = (int) Math.floor(h * 0.38);
        String eye = pal.get("eyeYellow");
        c.set(ex, ey, eye);
        c.set(ex + 1, ey, eye);
        c.set(ex, ey + 1, eye);
        c.set(ex + 1, ey + 1, eye);
        c.set(right ? ex : ex + 1, ey + 1, pal.get("eyePupil"));
    }

    // The design's layered plumage: top colour to bottom colour (blended toward a left to right one where given),
    // rows of U-shaped covert scallops, a fine barbule grain, a soft rounded shading, and the black flight feathers
    // peeking along the lowest row.
    static void plumage(int w, int h, Canvas c, Map<String, String> pal, String topCol, String bottomCol,
            String leftCol, String rightCol, boolean scallops, boolean blackEdge, int seed) {
        for (int y = 0; y < h; y++) {
            double ny = y / (double) Math.max(1, h - 1);
            for (int x = 0; x < w; x++) {
                double nx = x / (double) Math.max(1, w - 1);
                String base = mix(topCol, bottomCol, ny);
                if (leftCol != null && rightCol != null) {
                    base = mix(base, mix(leftCol, rightCol, nx), 0.45);
                }
                if (scallops) {
                    int row = y / 3;
                    int local = y % 3;
                    int off = x + (row % 2) * 2;
                    if (local == 2 && off % 4 != 0) {
                        base = shade(base, 0.91);
                    } else if (local == 0 && off % 4 == 1) {
                        base = mix(base, pal.get("wingPale"), 0.28);
                    }
                }
                double grain = (hash(x, Math.floor(y / 2.0), seed) - 0.5) * 0.06;
                String col = shade(base, 1 + grain);
                double volume = 0.92 + 0.12 * (Math.sin(nx * Math.PI) * 0.65 + Math.sin(ny * Math.PI) * 0.35);
                col = shade(col, volume);
                if (blackEdge && y == h - 1 && x > Math.floor(w * 0.15) && x < Math.floor(w * 0.88)) {
                    col = x % 2 == 0 ? pal.get("flightBlack") : pal.get("flightCharcoal");
                }
                c.set(x, y, col);
            }
        }
    }

    static double hash(double x, double y, int seed) {
        double n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123;
        return n - Math.floor(n);
    }

    static int[] rgb(String hex) {
        int v = Integer.parseInt(hex.substring(1), 16);
        return new int[] { v >> 16 & 255, v >> 8 & 255, v & 255 };
    }

    static String hex(double r, double g, double b) {
        return String.format("#%02x%02x%02x", clamp(r), clamp(g), clamp(b));
    }

    static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    static String mix(String a, String b, double t) {
        int[] x = rgb(a);
        int[] y = rgb(b);
        double k = Math.max(0, Math.min(1, t));
        return hex(x[0] + (y[0] - x[0]) * k, x[1] + (y[1] - x[1]) * k, x[2] + (y[2] - x[2]) * k);
    }

    static String shade(String hex, double f) {
        int[] c = rgb(hex);
        return hex(c[0] * f, c[1] * f, c[2] * f);
    }

    // The boxes laid out as FlamingoModel's Sheet does (each rounded to half a model pixel), and each face painted in
    // the design's orientation: a face's left to right and top to bottom as the design drew it.
    static final class Sheet {
        private final BufferedImage image;
        private int u;
        private int v;
        private int row;
        private final List<int[]> used = new ArrayList<>();

        Sheet(BufferedImage image) {
            this.image = image;
        }

        void box(Box box) {
            float rw = half(box.w);
            float rh = half(box.h);
            float rd = half(box.d);
            int fw = (int) Math.ceil(2.0F * (rw + rd));
            int fh = (int) Math.ceil(rd + rh);
            if (this.u + fw > WIDTH) {
                this.u = 0;
                this.v += this.row;
                this.row = 0;
            }
            int w = Math.round(rw * SCALE);
            int h = Math.round(rh * SCALE);
            int d = Math.round(rd * SCALE);
            int u0 = this.u * SCALE;
            int v0 = this.v * SCALE;
            for (int[] other : this.used) {
                if (u0 < other[2] && other[0] < u0 + 2 * (w + d) && v0 < other[3] && other[1] < v0 + d + h) {
                    throw new IllegalStateException("box " + this.used.size() + " overlaps another");
                }
            }
            this.used.add(new int[] { u0, v0, u0 + 2 * (w + d), v0 + d + h });
            // The design's faces: 0 its left side (+x), 1 its right side, 2 top, 3 bottom, 4 front, 5 back.
            face(box, 0, u0 + d + w, v0 + d, d, h, true, false);
            face(box, 1, u0, v0 + d, d, h, true, false);
            face(box, 2, u0 + d, v0, w, d, false, false);
            face(box, 3, u0 + d + w, v0, w, d, false, true);
            face(box, 4, u0 + d, v0 + d, w, h, false, false);
            face(box, 5, u0 + 2 * d + w, v0 + d, w, h, false, false);
            this.u += fw;
            this.row = Math.max(this.row, fh);
        }

        private void face(Box box, int index, int x0, int y0, int w, int h, boolean flipX, boolean flipY) {
            Face face = box.faces.getOrDefault(index, box.faces.get(-1));
            if (face == null || w <= 0 || h <= 0) {
                return;
            }
            face.paint(w, h, (x, y, hex) -> {
                if (x < 0 || x >= w || y < 0 || y >= h) {
                    return;
                }
                int px = x0 + (flipX ? w - 1 - x : x);
                int py = y0 + (flipY ? h - 1 - y : y);
                int[] c = rgb(hex);
                this.image.setRGB(px, py, 0xFF000000 | shown(c[0]) << 16 | shown(c[1]) << 8 | shown(c[2]));
            });
        }

        // The design's renderer reads its painted colours as linear light and writes sRGB, so it shows each colour
        // brighter and paler than its number: the texture holds the colour as the design shows it.
        private static int shown(int channel) {
            double c = channel / 255.0;
            double s = c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
            return clamp(s * 255);
        }

        private static float half(float size) {
            return Math.max(0.5F, Math.round(size * 2.0F) / 2.0F);
        }
    }
}
