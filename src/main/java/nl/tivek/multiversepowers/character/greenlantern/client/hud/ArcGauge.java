package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;

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

    // A padlock of whole pixels, 5 wide and 7 high, its top left corner at (x, y): the shackle over a body with a
    // keyhole.
    static void padlock(GuiGraphics graphics, int x, int y, int rgb, float alpha) {
        int color = GuiShapes.fade(rgb, alpha);
        graphics.fill(x + 1, y, x + 4, y + 1, color);
        graphics.fill(x, y + 1, x + 1, y + 3, color);
        graphics.fill(x + 4, y + 1, x + 5, y + 3, color);
        graphics.fill(x, y + 3, x + 5, y + 4, color);
        graphics.fill(x, y + 4, x + 2, y + 5, color);
        graphics.fill(x + 3, y + 4, x + 5, y + 5, color);
        graphics.fill(x, y + 5, x + 5, y + 7, color);
    }

    // Claims the gauge's band right of the crosshair, out `reach` past its pieces for its numbers and marks, and
    // returns how far out its pieces start: INNER, or past whatever stood there first.
    static float claim(float x, float y, float reach) {
        return HudSpace.ring(x, y, INNER - 1.0F, OUTER - INNER + 2.0F + reach, FROM - 2.0F, FROM + SPAN + 2.0F) + 1.0F;
    }

    // What a gauge held where it is shows under its title: a padlock, LOCKED, and the key that lets it go on.
    record Lock(float throb, @Nullable Component key) {
    }

    // The big title with a line or the lock under it, lined up to the left of the crosshair (the gauge is on its
    // right), moved up or down off whatever is there already; drawn after the arcs are flushed.
    static void label(GuiGraphics graphics, Font font, float x, float y, Component title, int titleColor,
            @Nullable Component line, int lineColor, float appear, @Nullable Lock lock) {
        int alpha = (int) (255 * appear) << 24;
        Component bold = title.copy().withStyle(ChatFormatting.BOLD);
        int titleWidth = Mth.ceil(font.width(bold) * TITLE_SCALE);
        int under = lock != null ? lockWidth(font, lock) : line != null ? font.width(line) : 0;
        int width = Math.max(titleWidth, under);
        int height = lock != null ? 27 : line != null ? 24 : 14;
        HudSpace.Box box = HudSpace.place(Mth.floor(x - LABEL_GAP) - width, Mth.floor(y) - 12, width, height,
                HudSpace.Way.UP, HudSpace.Way.DOWN);
        int right = Mth.floor(box.right());
        int top = Mth.floor(box.y());
        graphics.pose().pushPose();
        graphics.pose().translate(right - font.width(bold) * TITLE_SCALE, top, 0.0F);
        graphics.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        graphics.drawString(font, bold, 0, 0, alpha | titleColor, true);
        graphics.pose().popPose();
        if (lock != null) {
            lockPill(graphics, font, right - lockWidth(font, lock), top + 14, appear, lock);
        } else if (line != null) {
            graphics.drawString(font, line, right - font.width(line), top + 15, alpha | lineColor, true);
        }
    }

    private static int lockWidth(Font font, Lock lock) {
        Component locked = Component.translatable(PREFIX + "beam_locked").withStyle(ChatFormatting.BOLD);
        int width = 4 + 5 + 4 + font.width(locked) + 5;
        if (lock.key() != null) {
            width += KeyCap.width(font, lock.key()) + 3 + font.width(Component.translatable(PREFIX + "beam_unlock"))
                    + 5;
        }
        return width;
    }

    // A dark pill with an amber edge: a padlock and LOCKED in amber, then the key that lets the gauge go on.
    private static void lockPill(GuiGraphics graphics, Font font, int left, int top, float appear, Lock lock) {
        int width = lockWidth(font, lock);
        int edge = GuiShapes.mix(AMBER, 0xFFFFFF, 0.35F * lock.throb());
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        KeyCap.pill(graphics, left, top, width, 13, 0xFF000000 | edge);
        KeyCap.pill(graphics, left + 1, top + 1, width - 2, 11, 0xE6140E04);
        padlock(graphics, left + 4, top + 3, edge, 1.0F);
        Component locked = Component.translatable(PREFIX + "beam_locked").withStyle(ChatFormatting.BOLD);
        int at = left + 4 + 5 + 4;
        layer.text(locked, at, top + 3, 0xFF000000 | edge);
        at += font.width(locked) + 5;
        if (lock.key() != null) {
            at += KeyCap.draw(layer, font, lock.key(), at, top + 1, 11) + 3;
            layer.text(Component.translatable(PREFIX + "beam_unlock"), at, top + 3, 0xFFB9A88A);
        }
        layer.finish();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
