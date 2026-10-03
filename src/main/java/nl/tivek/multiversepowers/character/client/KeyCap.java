package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// Key caps, chips and dots for the ability panel and guide, small stone-grey buttons as the game's own. Shapes and text
// sit in buffers the game draws in an order of its own, so a `Layer` flushes the shapes first and draws the text after
// them, always on top.
public final class KeyCap {
    private static final int FACE = 0x3A3A3A;
    private static final int TOP = 0x6B6B6B;
    private static final int SHADOW = 0x121212;
    private static final int LABEL = 0xE6E6E6;
    private static final int PAD = 3;

    private KeyCap() {
    }

    public static int width(Font font, Component text) {
        return font.width(text) + PAD * 2;
    }

    // A key cap `height` tall, its lit top edge and dark bottom edge inside that, with its text centred; returns its
    // width.
    public static int draw(Layer layer, Font font, Component text, int x, int y, int height) {
        int width = width(font, text);
        GuiGraphics graphics = layer.graphics;
        pill(graphics, x, y + 1, width, height - 1, 0xFF000000 | SHADOW);
        pill(graphics, x, y, width, height - 1, 0xFF000000 | TOP);
        pill(graphics, x, y + 1, width, height - 2, 0xFF000000 | FACE);
        layer.text(text, x + PAD, y + (height - 7) / 2, 0xFF000000 | LABEL);
        return width;
    }

    // A box with its corner pixels cut, as the game's own buttons: three rectangles.
    public static void pill(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        GuiShapes.roundRect(graphics, x, y + 1, width, height - 2, 0.0F, argb);
        GuiShapes.roundRect(graphics, x + 1, y, width - 2, 1, 0.0F, argb);
        GuiShapes.roundRect(graphics, x + 1, y + height - 1, width - 2, 1, 0.0F, argb);
    }

    // A flat chip of text with cut corners; returns its width.
    public static int chip(Layer layer, Font font, Component text, int x, int y, int fill, int color) {
        int width = font.width(text) + 8;
        pill(layer.graphics, x, y, width, 11, fill);
        layer.text(text, x + 4, y + 2, color);
        return width;
    }

    public static void dot(GuiGraphics graphics, float x, float y, float radius, int argb) {
        GuiShapes.disc(graphics, x, y, radius, argb);
    }

    // Shapes drawn now, text queued and drawn on `finish`, both inside whatever scissor is on.
    public static final class Layer {
        private final GuiGraphics graphics;
        private final Font font;
        private final List<Runnable> texts = new ArrayList<>();

        public Layer(GuiGraphics graphics, Font font) {
            this.graphics = graphics;
            this.font = font;
        }

        public GuiGraphics graphics() {
            return this.graphics;
        }

        public void text(Component text, int x, int y, int color) {
            this.texts.add(() -> this.graphics.drawString(this.font, text, x, y, color, false));
        }

        public void shadowed(Component text, int x, int y, int color) {
            this.texts.add(() -> this.graphics.drawString(this.font, text, x, y, color, true));
        }

        public void sequence(FormattedCharSequence text, int x, int y, int color) {
            this.texts.add(() -> this.graphics.drawString(this.font, text, x, y, color, false));
        }

        public void finish() {
            GuiShapes.flush(this.graphics);
            for (Runnable text : this.texts) {
                text.run();
            }
            this.texts.clear();
            this.graphics.flush();
        }
    }
}
