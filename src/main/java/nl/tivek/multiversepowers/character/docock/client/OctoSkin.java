package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.LitShapes.Skin;
import nl.tivek.multiversepowers.engine.client.render.PaintedTexture;

// The metal of Doctor Octopus's harness and tentacles, painted in code in sharp pixels as the game's own textures
// are: brushed steel, dark gunmetal, the gold rings of his tentacles, the black rubber between them and the red lamp
// of each claw. Each is a row of the texture.
final class OctoSkin {
    private static final int SIZE = 32;
    static final PaintedTexture TEXTURE = new PaintedTexture(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "docock/metal"), SIZE, SIZE, OctoSkin::paint);

    static final Skin STEEL = row(0, 8);
    static final Skin DARK = row(8, 16);
    static final Skin GOLD = row(16, 24);
    static final Skin RUBBER = row(24, 28);
    static final Skin LAMP = row(28, 32);

    private OctoSkin() {
    }

    private static Skin row(int from, int to) {
        return new Skin(0.0F, (float) from / SIZE, 1.0F, (float) to / SIZE);
    }

    private static int paint(int x, int y) {
        float grain = noise(x, y) - 0.5F;
        if (y < 8) {
            // Brushed steel: streaks along u, a bright edge at the top, a darker one at the bottom.
            float streak = noise(x / 3, 99) - 0.5F;
            float edge = y == 0 ? 0.25F : y == 7 ? -0.22F : y == 1 ? 0.1F : 0.0F;
            return shade(0xB4BCC6, 0.08F * grain + 0.12F * streak + edge);
        }
        if (y < 16) {
            int r = y - 8;
            float edge = r == 0 ? 0.18F : r == 7 ? -0.18F : 0.0F;
            float rivet = (x % 8 == 3 && r == 3) ? 0.3F : 0.0F;
            return shade(0x30343B, 0.12F * grain + edge + rivet);
        }
        if (y < 24) {
            // Gold rings: bright in the middle row, shadowed at their edges, a fine groove round them.
            int r = y - 16;
            float bulge = 0.22F - 0.09F * Math.abs(r - 3.5F);
            float groove = r == 2 || r == 5 ? -0.12F : 0.0F;
            return shade(0xC89B34, bulge + groove + 0.07F * grain);
        }
        if (y < 28) {
            int r = y - 24;
            return shade(0x17181C, (r == 1 ? 0.25F : 0.0F) + 0.15F * grain);
        }
        // The lamp: white-hot in its middle, deep red at its rim.
        int r = y - 28;
        float hot = 1.0F - Math.abs(r - 1.5F) / 2.0F;
        return mix(0xFF9A1E14, 0xFFFFE0C8, hot * hot);
    }

    private static float noise(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535.0F;
    }

    private static int shade(int rgb, float amount) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        float k = 1.0F + amount;
        return 0xFF000000 | channel(r * k) << 16 | channel(g * k) << 8 | channel(b * k);
    }

    private static int mix(int from, int to, float t) {
        int r = channel(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = channel(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = channel(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int channel(float v) {
        return Mth.clamp(Math.round(v), 0, 255);
    }
}
