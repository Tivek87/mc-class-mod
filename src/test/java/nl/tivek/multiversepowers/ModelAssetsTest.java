package nl.tivek.multiversepowers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

// The mod's block models are drawn with a cutout shader that culls back faces: a clear pixel on a face, an open side,
// or a clear pixel anywhere in a model's picture (its smaller copies for far away blend face edges with it) shows
// straight through the model. Every model is closed and its pictures wholly opaque; an overlay drawn over another
// model's faces may be open, never clear.
class ModelAssetsTest {
    private static final String MOD = MultiversePowers.MODID;
    private static final Set<String> OVERLAYS = Set.of("thor/mjolnir_runes");
    private static final String[] SIDES = { "north", "east", "south", "west", "up", "down" };
    // How far outside an open side its points are looked for in another element, in model pixels.
    private static final double OUT = 0.05;
    private static final int SAMPLES = 4;

    private record Element(String name, double[] from, double[] to, String axis, double angle, double[] origin,
            JsonObject faces) {
    }

    @Test
    void everyModelIsClosedAndOpaque() throws IOException, URISyntaxException {
        Path assets = assets();
        Path folder = assets.resolve("models");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty(), "no models found under " + folder);
        Map<String, BufferedImage> pictures = new HashMap<>();
        for (Path file : files) {
            String name = folder.relativize(file).toString().replace('\\', '/').replaceAll("\\.json$", "");
            JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            // An item model that only names a parent (a spawn egg) has no boxes of its own.
            if (!model.has("elements")) {
                continue;
            }
            JsonObject textures = model.getAsJsonObject("textures");
            List<Element> elements = elements(model);
            for (Element element : elements) {
                for (int side = 0; side < SIDES.length; side++) {
                    JsonObject face = element.faces().getAsJsonObject(SIDES[side]);
                    if (face != null) {
                        BufferedImage picture = picture(assets, textures, face.get("texture").getAsString(), pictures);
                        if (picture != null) {
                            opaqueFace(name, element, SIDES[side], face.getAsJsonArray("uv"), picture);
                        }
                    } else if (!OVERLAYS.contains(name)) {
                        closed(name, element, side, elements);
                    }
                }
            }
        }
        assertFalse(pictures.isEmpty(), "no model pictures found");
        pictures.forEach(ModelAssetsTest::opaquePicture);
    }

    private static Path assets() throws URISyntaxException {
        URL url = ModelAssetsTest.class.getResource("/assets/" + MOD + "/models");
        assertNotNull(url, "the mod's models are not on the test path");
        return Path.of(url.toURI()).getParent();
    }

    private static List<Element> elements(JsonObject model) {
        List<Element> out = new ArrayList<>();
        for (JsonElement each : model.getAsJsonArray("elements")) {
            JsonObject element = each.getAsJsonObject();
            String axis = null;
            double angle = 0.0;
            double[] origin = new double[3];
            if (element.has("rotation")) {
                JsonObject rotation = element.getAsJsonObject("rotation");
                axis = rotation.get("axis").getAsString();
                angle = rotation.get("angle").getAsDouble();
                origin = numbers(rotation.getAsJsonArray("origin"));
            }
            out.add(new Element(element.has("name") ? element.get("name").getAsString() : "element " + out.size(),
                    numbers(element.getAsJsonArray("from")), numbers(element.getAsJsonArray("to")), axis, angle,
                    origin, element.getAsJsonObject("faces")));
        }
        return out;
    }

    private static double[] numbers(JsonArray array) {
        double[] out = new double[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsDouble();
        }
        return out;
    }

    // The mod's own picture behind a face's texture reference, or null for one of another namespace.
    private static BufferedImage picture(Path assets, JsonObject textures, String reference,
            Map<String, BufferedImage> pictures) throws IOException {
        String id = reference;
        for (int hops = 0; id.startsWith("#") && hops < 8; hops++) {
            JsonElement next = textures.get(id.substring(1));
            assertNotNull(next, "no texture " + id);
            id = next.getAsString();
        }
        int colon = id.indexOf(':');
        if (colon < 0 || !id.substring(0, colon).equals(MOD)) {
            return null;
        }
        String path = id.substring(colon + 1);
        BufferedImage cached = pictures.get(path);
        if (cached != null) {
            return cached;
        }
        Path file = assets.resolve("textures/" + path + ".png");
        assertTrue(Files.exists(file), "no picture " + file);
        BufferedImage picture = ImageIO.read(file.toFile());
        pictures.put(path, picture);
        return picture;
    }

    private static void opaqueFace(String model, Element element, String side, JsonArray uv, BufferedImage picture) {
        double scale = picture.getWidth() / 16.0;
        int x1 = (int) Math.round(Math.min(uv.get(0).getAsDouble(), uv.get(2).getAsDouble()) * scale);
        int x2 = (int) Math.round(Math.max(uv.get(0).getAsDouble(), uv.get(2).getAsDouble()) * scale);
        int y1 = (int) Math.round(Math.min(uv.get(1).getAsDouble(), uv.get(3).getAsDouble()) * scale);
        int y2 = (int) Math.round(Math.max(uv.get(1).getAsDouble(), uv.get(3).getAsDouble()) * scale);
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                assertTrue(picture.getRGB(x, y) >>> 24 == 255, model + ": " + element.name() + "'s " + side
                        + " face shows a clear pixel at " + x + "," + y);
            }
        }
    }

    private static void opaquePicture(String path, BufferedImage picture) {
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                assertTrue(picture.getRGB(x, y) >>> 24 == 255, path + ".png has a clear pixel at " + x + "," + y);
            }
        }
    }

    // A side with no face must lie wholly inside other elements, or it is a hole into the box.
    private static void closed(String model, Element element, int side, List<Element> elements) {
        int axis = side == 1 || side == 3 ? 0 : side == 4 || side == 5 ? 1 : 2;
        boolean high = side == 1 || side == 2 || side == 4;
        int u = axis == 0 ? 2 : 0;
        int v = axis == 1 ? 2 : 1;
        for (int i = 0; i < SAMPLES; i++) {
            for (int j = 0; j < SAMPLES; j++) {
                double[] local = new double[3];
                local[axis] = high ? element.to()[axis] + OUT : element.from()[axis] - OUT;
                local[u] = element.from()[u] + (element.to()[u] - element.from()[u]) * (i + 0.5) / SAMPLES;
                local[v] = element.from()[v] + (element.to()[v] - element.from()[v]) * (j + 0.5) / SAMPLES;
                double[] point = turn(local, element, 1.0);
                boolean inside = false;
                for (Element other : elements) {
                    if (other != element && holds(other, turn(point, other, -1.0))) {
                        inside = true;
                        break;
                    }
                }
                assertTrue(inside, model + ": " + element.name() + " has no " + SIDES[side]
                        + " face, and nothing covers it near " + local[0] + "," + local[1] + "," + local[2]);
            }
        }
    }

    private static boolean holds(Element element, double[] point) {
        for (int k = 0; k < 3; k++) {
            if (point[k] <= element.from()[k] || point[k] >= element.to()[k]) {
                return false;
            }
        }
        return true;
    }

    // A point turned as the game turns an element about its origin (way 1), or back again (way -1).
    private static double[] turn(double[] point, Element element, double way) {
        if (element.axis() == null || element.angle() == 0.0) {
            return point.clone();
        }
        double rad = Math.toRadians(element.angle()) * way;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double[] o = element.origin();
        double x = point[0] - o[0];
        double y = point[1] - o[1];
        double z = point[2] - o[2];
        return switch (element.axis()) {
            case "x" -> new double[] { o[0] + x, o[1] + y * cos - z * sin, o[2] + y * sin + z * cos };
            case "y" -> new double[] { o[0] + x * cos + z * sin, o[1] + y, o[2] - x * sin + z * cos };
            default -> new double[] { o[0] + x * cos - y * sin, o[1] + x * sin + y * cos, o[2] + z };
        };
    }
}
