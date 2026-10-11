import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

// Builds the stage lights in the game's own style: for each kind the block model of the part that stands still, the
// parts that move (yoke: turns about the mount; head: turns and tilts; lens: the light laid over the head), its
// blockstate and its item (every part at rest), and the 16 pixel pictures they share. Every model stands on the floor
// shining up; the blockstate turns it onto the face it is placed against. From the repository root, with Java 22 or
// newer: java scripts/models/stagelights/StageLightModels.java
public final class StageLightModels {
    private static final String NS = "welcomescreen";

    private record Box(double[] from, double[] to, String texture, String up, double turn) {
        Box(double x0, double y0, double z0, double x1, double y1, double z1, String texture) {
            this(new double[] { x0, y0, z0 }, new double[] { x1, y1, z1 }, texture, texture, 0.0);
        }

        Box top(String up) {
            return new Box(this.from, this.to, this.texture, up, this.turn);
        }

        // Turned about the up axis through the block's middle (the game takes -45, -22.5, 0, 22.5 or 45).
        Box turned(double degrees) {
            return new Box(this.from, this.to, this.texture, this.up, degrees);
        }
    }

    private record Kind(String id, List<Box> still, List<Box> yoke, List<Box> head, List<Box> lens) {
    }

    private StageLightModels() {
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/" + NS);
        Path block = Files.createDirectories(assets.resolve("models/block"));
        Path moving = Files.createDirectories(assets.resolve("models/stage"));
        Path item = Files.createDirectories(assets.resolve("models/item"));
        Path states = Files.createDirectories(assets.resolve("blockstates"));
        Path textures = Files.createDirectories(assets.resolve("textures/block"));
        for (Kind kind : kinds()) {
            Files.writeString(block.resolve(kind.id + ".json"), model(kind.still, true));
            for (String part : new String[] { "yoke", "head", "lens" }) {
                List<Box> boxes = part.equals("yoke") ? kind.yoke : part.equals("head") ? kind.head : kind.lens;
                Path file = moving.resolve(kind.id + "_" + part + ".json");
                if (boxes.isEmpty()) {
                    Files.deleteIfExists(file);
                } else {
                    Files.writeString(file, model(boxes, false));
                }
            }
            List<Box> all = new ArrayList<>(kind.still);
            all.addAll(kind.yoke);
            all.addAll(kind.head);
            Files.writeString(item.resolve(kind.id + ".json"), model(all, true));
            Files.writeString(states.resolve(kind.id + ".json"), blockstate(kind.id));
        }
        paint(textures);
        System.out.println("stage lights written");
    }

    private static List<Kind> kinds() {
        List<Kind> kinds = new ArrayList<>();
        // A moving head: a low base, a yoke and a head with its lens on top.
        kinds.add(new Kind("moving_head",
                List.of(new Box(2, 0, 2, 14, 3, 14, "metal").top("vent")),
                yoke(3, 13, 3, 12),
                List.of(new Box(5, 5, 5, 11, 14, 11, "vent").top("glass"),
                        new Box(4.5, 12, 4.5, 11.5, 14.5, 11.5, "dark").top("glass")),
                List.of(new Box(5.5, 14.5, 5.5, 10.5, 14.6, 10.5, "lens"))));
        // A searchlight: a broad drum on a heavy yoke.
        kinds.add(new Kind("searchlight",
                List.of(new Box(2, 0, 2, 14, 2, 14, "dark").top("metal")),
                yoke(1, 15, 2, 11),
                List.of(new Box(3, 3.5, 3, 13, 14, 13, "vent").top("glass"),
                        new Box(2.5, 13, 2.5, 13.5, 15.5, 13.5, "dark").top("glass")),
                List.of(new Box(3.5, 15.5, 3.5, 12.5, 15.6, 12.5, "lens"))));
        // A laser: a flat box with a small window on top.
        kinds.add(new Kind("laser",
                List.of(new Box(2, 0, 2, 14, 6, 14, "vent").top("laser_top")),
                List.of(), List.of(),
                List.of(new Box(6.5, 6, 6.5, 9.5, 6.1, 9.5, "lens"))));
        // A wash light: a short can on a yoke.
        kinds.add(new Kind("par",
                List.of(new Box(2, 0, 2, 14, 3, 14, "metal").top("vent")),
                yoke(3, 13, 3, 12),
                List.of(new Box(4.5, 4, 4.5, 11.5, 13, 11.5, "metal").top("glass"),
                        new Box(4, 12, 4, 12, 13.5, 12, "dark").top("glass")),
                List.of(new Box(5, 13.5, 5, 11, 13.6, 11, "lens"))));
        // A strobe: a wide panel.
        kinds.add(new Kind("strobe",
                List.of(new Box(1, 0, 1, 15, 5, 15, "vent").top("strobe")),
                List.of(), List.of(),
                List.of(new Box(2, 5, 2, 14, 5.1, 14, "lens"))));
        // A mirror ball on a short stand: the ball turns.
        List<Box> ball = List.of(new Box(3.5, 6, 5, 12.5, 12, 11, "mirror"),
                new Box(5, 6, 3.5, 11, 12, 12.5, "mirror"),
                new Box(3.5, 6, 5, 12.5, 12, 11, "mirror").turned(45),
                new Box(5, 6, 3.5, 11, 12, 12.5, "mirror").turned(45),
                new Box(5, 4.5, 5, 11, 13.5, 11, "mirror"));
        kinds.add(new Kind("disco_ball",
                List.of(new Box(5, 0, 5, 11, 1, 11, "metal"), new Box(7.5, 1, 7.5, 8.5, 4.5, 8.5, "dark")),
                List.of(), ball, List.of()));
        return kinds;
    }

    private static List<Box> yoke(double left, double right, double bottom, double top) {
        double mid = (bottom + top) * 0.5;
        return List.of(new Box(left, bottom, 6.5, left + 1.5, top, 9.5, "dark"),
                new Box(right - 1.5, bottom, 6.5, right, top, 9.5, "dark"),
                new Box(left, bottom, 6.5, right, bottom + 1.5, 9.5, "dark"),
                new Box(left + 0.2, mid - 1.0, 7.0, left + 1.6, mid + 1.0, 9.0, "metal"),
                new Box(right - 1.6, mid - 1.0, 7.0, right - 0.2, mid + 1.0, 9.0, "metal"));
    }

    private static String model(List<Box> boxes, boolean block) {
        StringBuilder out = new StringBuilder("{\n");
        out.append("\t\"credit\": \"Built by scripts/models/stagelights/StageLightModels.java\",\n");
        if (block) {
            out.append("\t\"parent\": \"minecraft:block/block\",\n");
        }
        out.append("\t\"ambientocclusion\": false,\n");
        out.append("\t\"textures\": {\n");
        List<String> used = new ArrayList<>();
        for (Box box : boxes) {
            for (String texture : new String[] { box.texture, box.up }) {
                if (!used.contains(texture)) {
                    used.add(texture);
                }
            }
        }
        if (used.isEmpty()) {
            used.add("metal");
        }
        for (String texture : used) {
            out.append("\t\t\"").append(texture).append("\": \"").append(NS).append(":block/stage_").append(texture)
                    .append("\",\n");
        }
        out.append("\t\t\"particle\": \"").append(NS).append(":block/stage_").append(used.get(0)).append("\"\n");
        out.append("\t},\n\t\"elements\": [\n");
        for (int i = 0; i < boxes.size(); i++) {
            out.append(element(boxes.get(i))).append(i + 1 < boxes.size() ? ",\n" : "\n");
        }
        return out.append("\t]\n}\n").toString();
    }

    private static String element(Box box) {
        StringBuilder out = new StringBuilder("\t\t{\"from\": ").append(list(box.from)).append(", \"to\": ")
                .append(list(box.to));
        if (box.turn != 0.0) {
            out.append(", \"rotation\": {\"angle\": ").append(number(box.turn))
                    .append(", \"axis\": \"y\", \"origin\": [8, 8, 8]}");
        }
        out.append(", \"faces\": {");
        String[] sides = { "north", "east", "south", "west", "up", "down" };
        for (int i = 0; i < sides.length; i++) {
            String texture = sides[i].equals("up") ? box.up : box.texture;
            out.append('"').append(sides[i]).append("\": {\"uv\": ").append(uv(sides[i], box.from, box.to))
                    .append(", \"texture\": \"#").append(texture).append("\"}").append(i + 1 < sides.length ? ", " : "");
        }
        return out.append("}}").toString();
    }

    // The face's patch of its picture as the game works it out for a face with none, moved onto the picture where a
    // box reaches past the block.
    private static String uv(String side, double[] from, double[] to) {
        double[] uv = switch (side) {
            case "north" -> new double[] { 16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1] };
            case "south" -> new double[] { from[0], 16 - to[1], to[0], 16 - from[1] };
            case "east" -> new double[] { 16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1] };
            case "west" -> new double[] { from[2], 16 - to[1], to[2], 16 - from[1] };
            case "up" -> new double[] { from[0], from[2], to[0], to[2] };
            default -> new double[] { from[0], 16 - to[2], to[0], 16 - from[2] };
        };
        for (int i = 0; i < 2; i++) {
            double low = Math.min(uv[i], uv[i + 2]);
            double high = Math.max(uv[i], uv[i + 2]);
            double shift = high - low <= 16.0 ? -16.0 * Math.floor(low / 16.0) : 0.0;
            if (high + shift > 16.0) {
                shift -= high + shift - 16.0;
            }
            uv[i] = Math.max(0.0, Math.min(16.0, uv[i] + shift));
            uv[i + 2] = Math.max(0.0, Math.min(16.0, uv[i + 2] + shift));
        }
        return list(uv);
    }

    // Placed on the floor it shines up; on the ceiling down; on a wall out of it.
    private static String blockstate(String id) {
        String model = NS + ":block/" + id;
        String[][] faces = { { "up", "0", "0" }, { "down", "180", "0" }, { "north", "90", "0" },
                { "south", "90", "180" }, { "west", "90", "270" }, { "east", "90", "90" } };
        StringBuilder out = new StringBuilder("{\n\t\"variants\": {\n");
        for (int i = 0; i < faces.length; i++) {
            out.append("\t\t\"facing=").append(faces[i][0]).append("\": {\"model\": \"").append(model).append('"');
            if (!faces[i][1].equals("0")) {
                out.append(", \"x\": ").append(faces[i][1]);
            }
            if (!faces[i][2].equals("0")) {
                out.append(", \"y\": ").append(faces[i][2]);
            }
            out.append('}').append(i + 1 < faces.length ? ",\n" : "\n");
        }
        return out.append("\t}\n}\n").toString();
    }

    private static String list(double[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            out.append(i > 0 ? ", " : "").append(number(values[i]));
        }
        return out.append(']').toString();
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    // ---- pictures ----

    private static void paint(Path textures) throws Exception {
        write(textures, "metal", (x, y) -> {
            int base = edge(x, y) ? 0x1F2228 : 0x34383F;
            return shade(base, speckle(x, y, 1) * 10 - (y == 1 ? -14 : 0));
        });
        write(textures, "dark", (x, y) -> shade(edge(x, y) ? 0x0E0F12 : 0x191B20, speckle(x, y, 2) * 8));
        write(textures, "vent", (x, y) -> {
            boolean slot = !edge(x, y) && y % 3 == 1 && x > 1 && x < 14;
            return slot ? 0x07080A : shade(edge(x, y) ? 0x1F2228 : 0x2B2E34, speckle(x, y, 3) * 8);
        });
        write(textures, "glass", (x, y) -> {
            double d = Math.hypot(x - 7.5, y - 7.5) / 8.0;
            int glint = (x - y == 3 || x - y == 4) && x > 4 && x < 12 ? 0x4A5A70 : 0;
            return glint != 0 ? glint : shade(0x1B2433, (int) (-d * 18 + speckle(x, y, 4) * 4));
        });
        write(textures, "lens", (x, y) -> {
            double d = Math.min(1.0, Math.hypot(x - 7.5, y - 7.5) / 8.5);
            int v = (int) (255 * (1.0 - 0.55 * d * d));
            return v << 16 | v << 8 | v;
        });
        write(textures, "laser_top", (x, y) -> {
            boolean window = x >= 6 && x <= 9 && y >= 6 && y <= 9;
            return window ? 0x0B0F14 : shade(edge(x, y) ? 0x1F2228 : 0x2E3137, speckle(x, y, 5) * 8);
        });
        write(textures, "strobe", (x, y) -> {
            if (edge(x, y)) {
                return 0x1F2228;
            }
            return x % 2 == 0 ? 0xB9C0C8 : 0xDCE2E8;
        });
        write(textures, "mirror", (x, y) -> {
            if (x % 4 == 3 || y % 4 == 3) {
                return 0x2A2E36;
            }
            int tile = (x / 4) * 7 + (y / 4) * 13;
            int[] tints = { 0xC9D3DE, 0xE6EEF6, 0xA9B8C9, 0xF4F8FF, 0xB7C4D3 };
            return tints[tile % tints.length];
        });
    }

    private interface Painter {
        int at(int x, int y);
    }

    private static void write(Path textures, String name, Painter painter) throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, painter.at(x, y));
            }
        }
        ImageIO.write(image, "png", textures.resolve("stage_" + name + ".png").toFile());
    }

    private static boolean edge(int x, int y) {
        return x == 0 || y == 0 || x == 15 || y == 15;
    }

    private static int speckle(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 2147483647;
        h = (h ^ h >>> 13) * 1274126177;
        return (h >>> 28) % 3 - 1;
    }

    private static int shade(int rgb, int amount) {
        int r = Math.max(0, Math.min(255, (rgb >> 16 & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, (rgb >> 8 & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (rgb & 0xFF) + amount));
        return r << 16 | g << 8 | b;
    }
}
