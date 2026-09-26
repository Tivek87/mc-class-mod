package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// A gauge of pieces on an arc right of the crosshair, with a big label beside it: the beam's stages, the gun's heat.
final class ArcGauge {
    static final String PREFIX = "screen." + MultiversePowers.MODID + ".hold.";
    static final int GREEN = 0x3CE86A;
    static final int BRIGHT = 0xCFFFDC;
    static final int WHITE = 0xE6FFEC;
    static final int AMBER = 0xFFC23A;
    static final int DARK = 0x0E3A1E;
    static final float FROM = 35.0F;
    static final float SPAN = 110.0F;
    static final float INNER = 21.0F;
    static final float OUTER = 27.0F;
    private static final float LABEL_GAP = 14.0F;
    private static final float TITLE_SCALE = 1.5F;

    private ArcGauge() {
    }

    static float pieceFrom(int k, int count, float gap) {
        return FROM + k * (piece(count, gap) + gap);
    }

    static float piece(int count, float gap) {
        return (SPAN - gap * (count - 1)) / count;
    }

    static void empty(GuiGraphics graphics, float x, float y, float inner, float outer, float from, float to,
            float appear) {
        GuiShapes.arc(graphics, x, y, inner - 1.0F, outer + 1.0F, from, to, GuiShapes.fade(0x000000, 0.5F * appear));
        GuiShapes.arc(graphics, x, y, inner, outer, from, to, GuiShapes.fade(DARK, 0.85F * appear));
    }

    static void filled(GuiGraphics graphics, float x, float y, float inner, float outer, float from, float to,
            float fill, int full, int filling, float appear) {
        if (fill <= 0.0F) {
            return;
        }
        if (fill >= 1.0F) {
            GuiShapes.arc(graphics, x, y, inner - 0.5F, outer + 0.5F, from, to, GuiShapes.fade(full, appear));
            // A pale line along the inside edge, so each lit piece reads as its own block of light.
            GuiShapes.arc(graphics, x, y, inner - 0.5F, inner + 1.0F, from, to, GuiShapes.fade(0xFFFFFF, 0.35F * appear));
            return;
        }
        float edge = from + (to - from) * fill;
        GuiShapes.arc(graphics, x, y, inner, outer, from, edge, GuiShapes.fade(filling, 0.95F * appear));
        GuiShapes.arc(graphics, x, y, inner - 1.0F, outer + 1.0F, Math.max(from, edge - 3.0F), edge,
                GuiShapes.fade(WHITE, appear));
    }

    static void flash(GuiGraphics graphics, float x, float y, float outer, float from, float to, float flash) {
        if (flash < 1.0F) {
            float out = outer + 3.0F + 8.0F * flash;
            GuiShapes.arc(graphics, x, y, out - 2.5F, out, from, to, GuiShapes.fade(WHITE, 1.0F - flash));
        }
    }

    // A number just outside a piece, at its middle.
    static void number(GuiGraphics graphics, Font font, float x, float y, float radius, float angle, String text,
            int argb) {
        double rad = Math.toRadians(angle);
        float px = x + (float) Math.sin(rad) * radius;
        float py = y - (float) Math.cos(rad) * radius;
        graphics.drawString(font, text, Mth.floor(px - font.width(text) * 0.5F + 0.5F), Mth.floor(py - 3.5F), argb,
                true);
    }

    static void padlock(GuiGraphics graphics, float x, float y, int rgb, float alpha) {
        int color = GuiShapes.fade(rgb, alpha);
        GuiShapes.arc(graphics, x, y - 2.0F, 1.6F, 2.8F, 270.0F, 360.0F, color);
        GuiShapes.arc(graphics, x, y - 2.0F, 1.6F, 2.8F, 0.0F, 90.0F, color);
        GuiShapes.quad(graphics, x - 2.8F, y - 2.0F, x - 1.6F, y - 2.0F, x - 1.6F, y, x - 2.8F, y, color);
        GuiShapes.quad(graphics, x + 1.6F, y - 2.0F, x + 2.8F, y - 2.0F, x + 2.8F, y, x + 1.6F, y, color);
        GuiShapes.roundRect(graphics, x - 3.8F, y - 0.5F, 7.6F, 5.5F, 1.0F, color);
    }

    // The words go on the other side of the crosshair from the gauge, lined up to it: the character panel fills the
    // right of the screen.
    static int labelRight(float x) {
        return Mth.floor(x - LABEL_GAP);
    }

    // The big title with a line under it; drawn after the arcs are flushed.
    static void label(GuiGraphics graphics, Font font, float x, float y, Component title, int titleColor,
            Component line, int lineColor, float appear) {
        int alpha = (int) (255 * appear) << 24;
        int right = labelRight(x);
        Component bold = title.copy().withStyle(ChatFormatting.BOLD);
        graphics.pose().pushPose();
        graphics.pose().translate(right - font.width(bold) * TITLE_SCALE, Mth.floor(y) - 12, 0.0F);
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        graphics.drawString(font, bold, 0, 0, alpha | titleColor, true);
        graphics.pose().popPose();
        if (line != null) {
            graphics.drawString(font, line, right - font.width(line), Mth.floor(y) + 3, alpha | lineColor, true);
        }
    }

    // A chip with a padlock, for a gauge that is held where it is.
    static void lockChip(GuiGraphics graphics, Font font, float x, float y, float appear, float throb) {
        Component text = Component.translatable(PREFIX + "beam_locked").withStyle(ChatFormatting.BOLD);
        int width = font.width(text) + 20;
        int left = labelRight(x) - width + 1;
        int top = Mth.floor(y) + 2;
        GuiShapes.roundRect(graphics, left - 1, top - 1, width, 12, 3.0F,
                GuiShapes.fade(GuiShapes.mix(AMBER, 0xFFFFFF, 0.3F * throb), appear));
        padlock(graphics, left + 6.0F, top + 4.0F, 0x2A1A00, appear);
        GuiShapes.flush(graphics);
        graphics.drawString(font, text, left + 13, top + 1, (int) (255 * appear) << 24 | 0x2A1A00, false);
    }
}
