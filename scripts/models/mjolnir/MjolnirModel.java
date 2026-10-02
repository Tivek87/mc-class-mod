import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

// Builds Mjolnir after the God of War Ragnarok hammer, in the game's own style: a model of boxes in the vanilla block
// model format (mjolnir.json, which Blockbench opens too), the faces its runes glow on (mjolnir_runes.json) and its
// 64 pixel picture (mjolnir.png). From the repository root, with Java 22 or newer:
// java scripts/models/mjolnir/MjolnirModel.java
public final class MjolnirModel {
    private static final String TEXTURE = "welcomescreen:thor/mjolnir";

    private MjolnirModel() {
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args.length > 0 ? args[0] : "src/main/resources/assets/welcomescreen");
        Path models = Files.createDirectories(assets.resolve("models/thor"));
        Path textures = Files.createDirectories(assets.resolve("textures/thor"));
        List<Cube> body = Hammer.body();
        Files.writeString(models.resolve("mjolnir.json"), model(body));
        Files.writeString(models.resolve("mjolnir_runes.json"), model(Hammer.runes()));
        ImageIO.write(Sheet.paint(), "png", textures.resolve("mjolnir.png").toFile());
        System.out.printf("%d cubes%n", body.size());
    }

    private static String model(List<Cube> cubes) {
        StringBuilder out = new StringBuilder("{\n");
        out.append("\t\"credit\": \"Built by scripts/models/mjolnir/MjolnirModel.java\",\n");
        out.append("\t\"texture_size\": [").append(Sheet.SIZE).append(", ").append(Sheet.SIZE).append("],\n");
        out.append("\t\"textures\": {\n\t\t\"hammer\": \"").append(TEXTURE).append("\",\n\t\t\"particle\": \"")
                .append(TEXTURE).append("\"\n\t},\n");
        out.append("\t\"elements\": [\n");
        for (int i = 0; i < cubes.size(); i++) {
            out.append(cubes.get(i).json("hammer")).append(i + 1 < cubes.size() ? ",\n" : "\n");
        }
        return out.append("\t]\n}\n").toString();
    }
}
