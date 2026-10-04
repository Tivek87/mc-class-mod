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
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;
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
    // What the toast says (`<head>` over `<line>`), and whether it shows a tick rather than the tour's spark.
    private static String head = "caught_up";
    private static String line = "replay_hint";
    private static boolean cheered;

    // Where the pill and its buttons were drawn.
    record Pill(TourCard.Box all, @Nullable TourCard.Box go, @Nullable TourCard.Box skip) {
    }

    private TourToast() {
    }

    // In the update manager it sits in the free middle of its title bar, without Skip, saying only `brief` (or nothing
    // but its button) where the whole label does not fit; on other screens at the top in the middle; in game, where it
    // has keys, in the corner.
    static Pill pill(GuiGraphics graphics, Component label, Component brief, boolean reachable, boolean mouse,
            float alpha, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Component go = Component.translatable(TourCard.PREFIX + "pill.continue");
        Component skip = TourCard.skipLabel();
        Component enter = TourCard.enter();
        ScreenAnchors.Rect bar = mouse ? ScreenAnchors.get("window.bar") : null;
        boolean skippable = mouse && bar == null;
        int extra = 0;
        if (reachable) {
            extra += mouse ? font.width(TourCard.nextLabel(go)) + 10 + 6 : KeyCap.width(font, enter) + 4
                    + font.width(go) + 6;
        }
        if (skippable) {
            extra += font.width(skip) + 8;
        }
        if (bar != null && 22 + font.width(label) + 8 + extra > bar.width()) {
            label = 22 + font.width(brief) + 8 + extra <= bar.width() ? brief : Component.empty();
        }
        int width = 22 + (label.getString().isEmpty() ? 0 : font.width(label) + 8) + extra;
        int x = EDGE;
        int y = EDGE;
        if (bar != null) {
            x = Math.round(bar.x() + (bar.width() - width) / 2.0F);
            y = Math.round(bar.y()) + 1;
        } else if (mouse) {
            x = (graphics.guiWidth() - width) / 2;
            y = 4;
        } else {
            y = Mth.floor(HudSpace.place(x, y, width, PILL, HudSpace.Way.DOWN).y());
        }
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        TourCard.panel(graphics, x, y, width, PILL);
        PixelIcons.draw(graphics, PixelIcons.Icon.SPARK, x + 5, y + 4, 1, 0xFF000000 | TourCard.ACCENT, true);
        layer.text(label, x + 22, y + 6, TourCard.TEXT);
        int at = x + width - extra;
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
        toast(now, "caught_up", "replay_hint", true);
    }

    static void skipped(long now) {
        toast(now, "skipped", "replay_hint", false);
    }

    // The update manager's Tour on a version with nothing to point at.
    static void nothing(long now) {
        toast(now, "nothing", "nothing.hint", false);
    }

    private static void toast(long now, String head, String line, boolean cheered) {
        toastAt = now;
        TourToast.head = head;
        TourToast.line = line;
        TourToast.cheered = cheered;
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
        Component head = Component.translatable(TourCard.PREFIX + TourToast.head).withStyle(ChatFormatting.BOLD);
        Component line = Component.translatable(TourCard.PREFIX + TourToast.line);
        int w = Math.max(font.width(head), font.width(line)) + 34;
        int h = 30;
        int x = (graphics.guiWidth() - w) / 2;
        // In game it comes to rest under the boss bars.
        int rest = Minecraft.getInstance().screen == null
                ? Mth.floor(HudSpace.place(x, 8, w, h, HudSpace.Way.DOWN).y()) : 8;
        int y = Math.round(-h - 2.0F + (rest + h + 2.0F) * shown);
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
