package nl.tivek.multiversepowers.update.client;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// What a version is by the end of its name: "-alpha", "-beta", or a release ("-release" or nothing). The update screens
// show it as a tag after a version, a small chip in its colour, only where it tells something: in the list of versions,
// beside the version that is out and beside a server's version.
enum Stage {
    ALPHA(0xFFA45C), BETA(0x7CC4FF), RELEASE(0x6EE7A0);

    static final int HEIGHT = 10;
    private static final int GAP = 4;

    private final int color;

    Stage(int color) {
        this.color = color;
    }

    // Null for an ending that is none of these.
    @Nullable
    static Stage of(String version) {
        int dash = version.indexOf('-');
        String end = dash < 0 ? "" : version.substring(dash + 1).toLowerCase(Locale.ROOT);
        if (end.isEmpty() || end.startsWith("release")) {
            return RELEASE;
        }
        return end.startsWith("alpha") ? ALPHA : end.startsWith("beta") ? BETA : null;
    }

    Component label() {
        return ManagerScreen.text("stage." + this.name().toLowerCase(Locale.ROOT));
    }

    int width(Font font) {
        return font.width(this.label()) + 6;
    }

    // The tag with its top left at (x, y), its corners cut as the manager's other chips; returns its width. Its fill is
    // see-through, so its three strips must not overlap.
    int draw(GuiGraphics graphics, Font font, int x, int y) {
        int width = this.width(font);
        int fill = GuiShapes.fade(this.color, 0.24F);
        graphics.fill(x + 1, y, x + width - 1, y + 1, fill);
        graphics.fill(x, y + 1, x + width, y + HEIGHT - 1, fill);
        graphics.fill(x + 1, y + HEIGHT - 1, x + width - 1, y + HEIGHT, fill);
        graphics.drawString(font, this.label(), x + 3, y + 1, 0xFF000000 | this.color, false);
        return width;
    }

    // A version as the update screens show it, "v0.7.4" and its tag, with the text's top at y; returns its width.
    static int drawVersion(GuiGraphics graphics, Font font, String version, int x, int y, int color) {
        String name = UpdateManagerScreen.name(version);
        graphics.drawString(font, name, x, y, color, false);
        Stage stage = of(version);
        return font.width(name) + (stage == null ? 0 : GAP + stage.draw(graphics, font, x + font.width(name) + GAP,
                y - 1));
    }

    static int versionWidth(Font font, String version) {
        Stage stage = of(version);
        return font.width(UpdateManagerScreen.name(version)) + (stage == null ? 0 : GAP + stage.width(font));
    }
}
