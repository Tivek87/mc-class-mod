import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.imageio.ImageIO;

// Builds Green Lantern's Power Battery in the game's own style: boxes in the vanilla block model format (lantern.json,
// which Blockbench opens too) on a 128 pixel picture (lantern.png), in the proportions of the lantern it was drawn from:
// a gold ring and knob on top, gold and stone bands at both ends, ribbed green glass above and below its body, two gold
// hoops round its middle and the emblem on its front and back. Its glass's light is two more models of the glass alone
// (lantern_glass.json, lantern_glass_b.json), each with its own cracks of light, laid over it in turn so it crackles,
// over the glass's even light (lantern_light.json).
// One unit is half a model pixel; the lantern stands 26 units tall on x and z 8. From the repository root:
// java scripts/models/lantern/LanternModel.java
public final class LanternModel {
    private static final String TEXTURE = "welcomescreen:greenlantern/lantern";
    private static final int SIZE = 128;
    private static final int GOLD = 0xD8B04C;
    private static final int GOLD_TOP = 0xEDCD6E;
    private static final int GOLD_LIGHT = 0xF5DA84;
    private static final int GOLD_DARK = 0xA8842F;
    private static final int GOLD_UNDER = 0x8A6A27;
    private static final int STONE = 0x9A9A94;
    private static final int STONE_DARK = 0x72726C;
    private static final int GLASS = 0x1B5A2F;
    private static final int GLASS_TOP = 0x164C27;
    private static final int RIB = 0x2B7C45;
    private static final int SHINE = 0x3F9160;
    private static final int SHINE_SOFT = 0x2F7A4C;
    private static final int GLASS_EDGE = 0x113A1F;
    private static final int DISC = 0x1E6436;
    private static final int PLATE = 0xE8C55C;
    private static final int SIGN = 0x103A1E;
    private static final int CRACK = 0x4CFF84;
    private static final int CORE = 0xDFFFE6;
    private static final int SPARK = 0x9CFFB8;

    enum Side { NORTH, EAST, SOUTH, WEST, UP, DOWN }

    enum Paint { GOLD, STONE, NECK, BODY, FOOT, EMBLEM, GOLD_PLAIN, LIT, GLOW_A, GLOW_B }

    record Rect(int x, int y, int w, int h) {
    }

    // One box: from and to in units, its paint, and the picture each side shows.
    static final class Cube {
        final String name;
        final double[] from;
        final double[] to;
        final Paint paint;
        final Rect[] shown = new Rect[6];

        Cube(String name, double x1, double y1, double z1, double x2, double y2, double z2, Paint paint) {
            this.name = name;
            this.from = new double[] { x1, y1, z1 };
            this.to = new double[] { x2, y2, z2 };
            this.paint = paint;
        }

        int width(Side side) {
            return (int) Math.round(switch (side) {
                case NORTH, SOUTH, UP, DOWN -> this.to[0] - this.from[0];
                case EAST, WEST -> this.to[2] - this.from[2];
            });
        }

        int height(Side side) {
            return (int) Math.round(switch (side) {
                case NORTH, SOUTH, EAST, WEST -> this.to[1] - this.from[1];
                case UP, DOWN -> this.to[2] - this.from[2];
            });
        }

        Cube grown(String name, double by, Paint paint) {
            return new Cube(name, this.from[0] - by, this.from[1] - by, this.from[2] - by, this.to[0] + by,
                    this.to[1] + by, this.to[2] + by, paint);
        }
    }

    private static final BufferedImage SHEET = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    private static int shelfX;
    private static int shelfY;
    private static int shelfH;

    private LanternModel() {
    }

    public static void main(String[] args) throws IOException {
        Path assets = Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/welcomescreen");
        Path models = Files.createDirectories(assets.resolve("models/greenlantern"));
        Path textures = Files.createDirectories(assets.resolve("textures/greenlantern"));
        // Wholly opaque, as every picture of the mod: what no face shows is black.
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                SHEET.setRGB(x, y, 0xFF000000);
            }
        }
        List<Cube> body = body();
        List<Cube> lit = new ArrayList<>();
        List<Cube> glassA = new ArrayList<>();
        List<Cube> glassB = new ArrayList<>();
        for (Cube cube : body) {
            if (cube.paint == Paint.NECK || cube.paint == Paint.BODY || cube.paint == Paint.FOOT) {
                lit.add(cube.grown(cube.name + "_light", 0.02, Paint.LIT));
                glassA.add(cube.grown(cube.name + "_light", 0.04, Paint.GLOW_A));
                glassB.add(cube.grown(cube.name + "_light", 0.04, Paint.GLOW_B));
            }
        }
        for (List<Cube> cubes : List.of(body, lit, glassA, glassB)) {
            for (Cube cube : cubes) {
                for (Side side : Side.values()) {
                    boolean glow = cube.paint == Paint.LIT || cube.paint == Paint.GLOW_A || cube.paint == Paint.GLOW_B;
                    if (glow && (side == Side.UP || side == Side.DOWN)) {
                        continue;
                    }
                    cube.shown[side.ordinal()] = place(cube, side);
                }
            }
        }
        Files.writeString(models.resolve("lantern.json"), model(body));
        Files.writeString(models.resolve("lantern_light.json"), model(lit));
        Files.writeString(models.resolve("lantern_glass.json"), model(glassA));
        Files.writeString(models.resolve("lantern_glass_b.json"), model(glassB));
        ImageIO.write(SHEET, "png", textures.resolve("lantern.png").toFile());
        System.out.printf("%d cubes, sheet filled to row %d of %d%n", body.size(), shelfY + shelfH, SIZE);
    }

    private static List<Cube> body() {
        List<Cube> cubes = new ArrayList<>();
        // The ring on top, a rounded bail on the knob.
        cubes.add(new Cube("ring_top", 5, 25, 7.5, 11, 26, 8.5, Paint.GOLD_PLAIN));
        cubes.add(new Cube("ring_left", 4, 21, 7.5, 5, 25, 8.5, Paint.GOLD_PLAIN));
        cubes.add(new Cube("ring_right", 11, 21, 7.5, 12, 25, 8.5, Paint.GOLD_PLAIN));
        cubes.add(new Cube("ring_left_low", 5, 20, 7.5, 6, 21, 8.5, Paint.GOLD_PLAIN));
        cubes.add(new Cube("ring_right_low", 10, 20, 7.5, 11, 21, 8.5, Paint.GOLD_PLAIN));
        cubes.add(new Cube("knob", 7, 17, 7, 9, 21, 9, Paint.GOLD));
        // The cap: gold, stone, gold.
        cubes.add(new Cube("cap_top", 5, 17, 5, 11, 18, 11, Paint.GOLD));
        cubes.add(new Cube("cap_band", 4, 16, 4, 12, 17, 12, Paint.STONE));
        cubes.add(new Cube("cap_low", 4, 15, 4, 12, 16, 12, Paint.GOLD));
        // The glass: ribbed neck, body, ribbed foot.
        cubes.add(new Cube("neck", 5, 12, 5, 11, 15, 11, Paint.NECK));
        cubes.add(new Cube("body", 4, 5, 4, 12, 12, 12, Paint.BODY));
        cubes.add(new Cube("foot_glass", 5, 3, 5, 11, 5, 11, Paint.FOOT));
        // The two gold hoops round the middle, wider to the sides than to the front.
        cubes.add(new Cube("hoop_high", 3, 10, 3.5, 13, 11, 12.5, Paint.GOLD));
        cubes.add(new Cube("hoop_low", 3, 7, 3.5, 13, 8, 12.5, Paint.GOLD));
        // The emblem on the front and the back.
        cubes.add(new Cube("emblem_front", 5, 6, 12.5, 11, 12, 13.5, Paint.EMBLEM));
        cubes.add(new Cube("emblem_back", 5, 6, 2.5, 11, 12, 3.5, Paint.EMBLEM));
        // The foot: gold, stone, gold, each a little wider.
        cubes.add(new Cube("foot_top", 4, 2, 4, 12, 3, 12, Paint.GOLD));
        cubes.add(new Cube("foot_band", 3.5, 1, 3.5, 12.5, 2, 12.5, Paint.STONE));
        cubes.add(new Cube("foot_base", 3, 0, 3, 13, 1, 13, Paint.GOLD));
        return cubes;
    }

    // A place on the sheet for one side, painted.
    private static Rect place(Cube cube, Side side) {
        int w = cube.width(side);
        int h = cube.height(side);
        if (shelfX + w > SIZE) {
            shelfX = 0;
            shelfY += shelfH;
            shelfH = 0;
        }
        if (shelfY + h > SIZE) {
            throw new IllegalStateException("sheet full at " + cube.name + " " + side);
        }
        Rect rect = new Rect(shelfX, shelfY, w, h);
        shelfX += w;
        shelfH = Math.max(shelfH, h);
        paint(rect, cube, side);
        return rect;
    }

    private static void paint(Rect rect, Cube cube, Side side) {
        Random random = new Random(cube.name.hashCode() * 31L + side.ordinal());
        boolean upright = side != Side.UP && side != Side.DOWN;
        for (int j = 0; j < rect.h(); j++) {
            for (int i = 0; i < rect.w(); i++) {
                int rgb = switch (cube.paint) {
                    case GOLD, GOLD_PLAIN -> gold(side, i, j, rect, cube.paint == Paint.GOLD_PLAIN, random);
                    case STONE -> random.nextInt(5) == 0 ? STONE_DARK : STONE;
                    case NECK, FOOT -> upright ? (j % 2 == 0 ? RIB : GLASS) : GLASS_TOP;
                    case BODY -> upright ? body(i, rect.w()) : GLASS_TOP;
                    case EMBLEM -> side == Side.SOUTH || side == Side.NORTH ? emblem(i, j) : GOLD_DARK;
                    case LIT -> lit(cube, i, j, rect);
                    case GLOW_A, GLOW_B -> 0x000000;
                };
                int noise = cube.paint == Paint.LIT || cube.paint == Paint.GLOW_A || cube.paint == Paint.GLOW_B || cube.paint == Paint.EMBLEM
                        ? 0 : random.nextInt(11) - 5;
                SHEET.setRGB(rect.x() + i, rect.y() + j, 0xFF000000 | shade(rgb, noise));
            }
        }
        if (cube.paint == Paint.GLOW_A || cube.paint == Paint.GLOW_B) {
            cracks(rect, new Random((cube.paint == Paint.GLOW_A ? 7L : 1013L) + cube.name.hashCode() * 17L
                    + side.ordinal()));
        }
    }

    private static int gold(Side side, int i, int j, Rect rect, boolean plain, Random random) {
        if (side == Side.UP) {
            return GOLD_TOP;
        }
        if (side == Side.DOWN) {
            return GOLD_UNDER;
        }
        if (plain || rect.h() == 1) {
            return random.nextInt(4) == 0 ? GOLD_LIGHT : GOLD;
        }
        int rgb = j == 0 ? GOLD_LIGHT : j == rect.h() - 1 ? GOLD_DARK : GOLD;
        return i == 0 || i == rect.w() - 1 ? mix(rgb, GOLD_UNDER, 0.35) : rgb;
    }

    // The glass lit from inside, near white so the light's colour comes from how it is drawn: brightest down its middle,
    // the ribs of the neck and foot in bands.
    private static int lit(Cube cube, int i, int j, Rect rect) {
        if (!cube.name.startsWith("body")) {
            return j % 2 == 0 ? 0xFFFFFF : 0xA8D8B4;
        }
        double off = Math.abs(i + 0.5 - rect.w() / 2.0) / (rect.w() / 2.0);
        double up = Math.abs(j + 0.5 - rect.h() / 2.0) / (rect.h() / 2.0);
        return mix(0xFFFFFF, 0x9CC8A8, Math.min(1.0, 0.7 * off * off + 0.3 * up * up));
    }

    private static int body(int i, int w) {
        if (i == 1) {
            return SHINE;
        }
        if (i == 2) {
            return SHINE_SOFT;
        }
        return i == 0 || i == w - 1 ? GLASS_EDGE : GLASS;
    }

    // The emblem's face, six by six: the lantern's sign in gold on a dark green disc, a circle between two bars.
    private static int emblem(int i, int j) {
        String[] rows = { "eppppe", "ddppdd", "dpsspd", "dpsspd", "ddppdd", "eppppe" };
        return switch (rows[j].charAt(i)) {
            case 'g' -> GOLD;
            case 'p' -> PLATE;
            case 's' -> SIGN;
            case 'e' -> GLASS_EDGE;
            default -> DISC;
        };
    }

    // Cracks of light over black (black adds nothing where it is laid): a few jagged lines, their middles white-hot,
    // and sparks.
    private static void cracks(Rect rect, Random random) {
        int lines = Math.max(1, rect.w() * rect.h() / 14);
        for (int k = 0; k < lines; k++) {
            double x = random.nextInt(rect.w());
            double y = random.nextInt(rect.h());
            double angle = random.nextDouble() * Math.PI * 2.0;
            int length = 2 + random.nextInt(4);
            for (int s = 0; s < length; s++) {
                int px = (int) Math.round(x);
                int py = (int) Math.round(y);
                if (px >= 0 && py >= 0 && px < rect.w() && py < rect.h()) {
                    SHEET.setRGB(rect.x() + px, rect.y() + py, 0xFF000000 | (s == length / 2 ? CORE : CRACK));
                }
                angle += (random.nextDouble() - 0.5) * 1.4;
                x += Math.cos(angle);
                y += Math.sin(angle);
            }
        }
        for (int k = 0; k < Math.max(1, rect.w() * rect.h() / 20); k++) {
            SHEET.setRGB(rect.x() + random.nextInt(rect.w()), rect.y() + random.nextInt(rect.h()), 0xFF000000 | SPARK);
        }
    }

    private static int shade(int rgb, int by) {
        int r = Math.max(0, Math.min(255, (rgb >> 16 & 0xFF) + by));
        int g = Math.max(0, Math.min(255, (rgb >> 8 & 0xFF) + by));
        int b = Math.max(0, Math.min(255, (rgb & 0xFF) + by));
        return r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
        int g = (int) Math.round((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
        int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    private static String model(List<Cube> cubes) {
        StringBuilder out = new StringBuilder("{\n");
        out.append("\t\"credit\": \"Built by scripts/models/lantern/LanternModel.java\",\n");
        out.append("\t\"texture_size\": [").append(SIZE).append(", ").append(SIZE).append("],\n");
        out.append("\t\"textures\": {\n\t\t\"lantern\": \"").append(TEXTURE).append("\",\n\t\t\"particle\": \"")
                .append(TEXTURE).append("\"\n\t},\n");
        out.append("\t\"elements\": [\n");
        for (int c = 0; c < cubes.size(); c++) {
            Cube cube = cubes.get(c);
            out.append("\t\t{\n\t\t\t\"name\": \"").append(cube.name).append("\",\n");
            out.append("\t\t\t\"from\": ").append(list(cube.from)).append(",\n");
            out.append("\t\t\t\"to\": ").append(list(cube.to)).append(",\n");
            out.append("\t\t\t\"faces\": {\n");
            boolean first = true;
            for (Side side : Side.values()) {
                Rect rect = cube.shown[side.ordinal()];
                if (rect == null) {
                    continue;
                }
                double unit = 16.0 / SIZE;
                out.append(first ? "" : ",\n").append("\t\t\t\t\"").append(side.name().toLowerCase(Locale.ROOT))
                        .append("\": {\"uv\": ").append(list(rect.x() * unit, rect.y() * unit,
                                (rect.x() + rect.w()) * unit, (rect.y() + rect.h()) * unit))
                        .append(", \"texture\": \"#lantern\"}");
                first = false;
            }
            out.append("\n\t\t\t}\n\t\t}").append(c + 1 < cubes.size() ? ",\n" : "\n");
        }
        return out.append("\t]\n}\n").toString();
    }

    private static String list(double... values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            out.append(i == 0 ? "" : ", ").append(v == Math.rint(v) ? String.valueOf((long) v)
                    : String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", ""));
        }
        return out.append("]").toString();
    }
}
