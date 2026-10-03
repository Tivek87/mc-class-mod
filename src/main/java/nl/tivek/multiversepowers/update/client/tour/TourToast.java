package nl.tivek.multiversepowers.update.client.tour;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.math.Ease;

// What the tour shows besides its cards: a small pill in the corner while its next steps wait somewhere else (Continue
// takes the player there, or it says they show up in game), and a toast once it is done or skipped, saying where to
// replay it.
final class TourToast {
    private static final long TOAST_MS = 3600L;
    private static final int PILL = 18;
    private static final int EDGE = 6;

    private static long toastAt = -TOAST_MS;
    private static boolean cheered;

    // Where the pill and its buttons were drawn.
    record Pill(TourCard.Box all, @Nullable TourCard.Box go, @Nullable TourCard.Box skip) {
    }

    private TourToast() {
    }

    // In the update manager it sits in the middle of its title bar, without Skip; on other screens at the top in the
    // middle; in game, where it has keys, in the corner.
    static Pill pill(GuiGraphics graphics, Component label, boolean reachable, boolean mouse, float alpha,
            double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Component go = Component.translatable(TourCard.PREFIX + "pill.continue");
        Component skip = TourCard.skipLabel();
        Component enter = TourCard.enter();
        ScreenAnchors.Rect window = mouse ? ScreenAnchors.get("window") : null;
        boolean skippable = mouse && window == null;
        int width = 22 + font.width(label) + 8;
        if (reachable) {
            width += mouse ? font.width(TourCard.nextLabel(go)) + 10 + 6 : KeyCap.width(font, enter) + 4
                    + font.width(go) + 6;
        }
        if (skippable) {
            width += font.width(skip) + 8;
        }
        int x = EDGE;
        int y = EDGE;
        if (window != null) {
            x = Math.round(window.x() + (window.width() - width) / 2.0F);
            y = Math.round(window.y()) + 1;
        } else if (mouse) {
            x = (graphics.guiWidth() - width) / 2;
            y = 4;
        }
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        TourCard.panel(graphics, x, y, width, PILL);
        PixelIcons.draw(graphics, PixelIcons.Icon.SPARK, x + 5, y + 4, 1, 0xFF000000 | TourCard.ACCENT, true);
        layer.text(label, x + 22, y + 6, TourCard.TEXT);
        int at = x + 22 + font.width(label) + 8;
        TourCard.Box goBox = null;
        TourCard.Box skipBox = null;
        if (reachable) {
            if (mouse) {
                goBox = TourCard.button(layer, font, TourCard.nextLabel(go), at + font.width(TourCard.nextLabel(go))
                        + 10, y + 3, mouseX, mouseY);
                at += goBox.width() + 6;
            } else {
                int cap = KeyCap.draw(layer, font, enter, at, y + 4, 11);
                layer.text(go, at + cap + 4, y + 6, TourCard.BODY);
            }
        }
        if (skippable) {
            skipBox = TourCard.link(layer, font, skip, at + 2, y + 6, mouseX, mouseY);
        }
        layer.finish();
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return new Pill(new TourCard.Box(x, y, width, PILL), goBox, skipBox);
    }

    static void cheer(long now) {
        toastAt = now;
        cheered = true;
    }

    static void skipped(long now) {
        toastAt = now;
        cheered = false;
    }

    static boolean showing(long now) {
        return now - toastAt < TOAST_MS;
    }

    // Sliding down at the top with a tick (or the tour's spark when skipped), then away.
    static void draw(GuiGraphics graphics, long now) {
        Font font = Minecraft.getInstance().font;
        long age = now - toastAt;
        float in = Mth.clamp(age / 250.0F, 0.0F, 1.0F);
        float out = Mth.clamp((TOAST_MS - age) / 400.0F, 0.0F, 1.0F);
        float shown = (float) Ease.smooth(Math.min(in, out));
        Component head = Component.translatable(TourCard.PREFIX + (cheered ? "caught_up" : "skipped"))
                .withStyle(ChatFormatting.BOLD);
        Component line = Component.translatable(TourCard.PREFIX + "replay_hint");
        int w = Math.max(font.width(head), font.width(line)) + 34;
        int h = 30;
        int x = (graphics.guiWidth() - w) / 2;
        int y = Math.round(-h - 2.0F + (h + 10.0F) * shown);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, shown);
        TourCard.panel(graphics, x, y, w, h);
        PixelIcons.draw(graphics, cheered ? PixelIcons.Icon.CHECK : PixelIcons.Icon.SPARK, x + 8, y + 9, 1,
                0xFF000000 | TourCard.ACCENT, true);
        graphics.drawString(font, head, x + 26, y + 7, TourCard.TEXT, false);
        graphics.drawString(font, line, x + 26, y + 18, TourCard.MUTED, false);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
