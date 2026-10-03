package nl.tivek.multiversepowers.update.client.tour;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.math.Ease;

// The spotlight of a step: the screen dimmed round what it points at, through a hole with round corners and a soft
// green glow at its edge; a frame breathing round it with pixel corners standing off it; the line from the card; and a
// step done flashing outward, with a tick popping up when the player did what it asked.
final class TourSpot {
    static final float RADIUS = 4.0F;
    private static final int STEPS = 6;
    private static final long FLASH_MS = 320L;
    private static final long TICK_MS = 650L;

    private static final float[] FLASHED = new float[4];
    private static long flashedAt = -TICK_MS;
    private static boolean ticked;

    private TourSpot() {
    }

    // The screen dimmed by `alpha`, but for `hole` (x, y, width, height) when there is one.
    static void shade(GuiGraphics graphics, int width, int height, float alpha, @Nullable float[] hole) {
        if (alpha < 0.01F) {
            return;
        }
        int color = GuiShapes.fade(0, alpha);
        if (hole == null) {
            GuiShapes.roundRect(graphics, 0, 0, width, height, 0.0F, color);
            return;
        }
        float x = hole[0];
        float y = hole[1];
        float right = x + hole[2];
        float bottom = y + hole[3];
        GuiShapes.roundRect(graphics, 0, 0, width, Math.max(0.0F, y), 0.0F, color);
        GuiShapes.roundRect(graphics, 0, bottom, width, Math.max(0.0F, height - bottom), 0.0F, color);
        GuiShapes.roundRect(graphics, 0, y, Math.max(0.0F, x), hole[3], 0.0F, color);
        GuiShapes.roundRect(graphics, right, y, Math.max(0.0F, width - right), hole[3], 0.0F, color);
        float r = Math.min(RADIUS, Math.min(hole[2], hole[3]) * 0.5F);
        corner(graphics, x, y, x + r, y + r, r, 270.0F, color);
        corner(graphics, right, y, right - r, y + r, r, 0.0F, color);
        corner(graphics, right, bottom, right - r, bottom - r, r, 90.0F, color);
        corner(graphics, x, bottom, x + r, bottom - r, r, 180.0F, color);
    }

    // The part of a corner's square outside its quarter circle, so the dimmed hole has round corners.
    private static void corner(GuiGraphics graphics, float cornerX, float cornerY, float cx, float cy, float r,
            float from, int color) {
        for (int i = 0; i < STEPS; i++) {
            float a = (from + 90.0F * i / STEPS) * Mth.DEG_TO_RAD;
            float b = (from + 90.0F * (i + 1) / STEPS) * Mth.DEG_TO_RAD;
            GuiShapes.quad(graphics, cornerX, cornerY, cornerX, cornerY, cx + Mth.sin(a) * r, cy - Mth.cos(a) * r,
                    cx + Mth.sin(b) * r, cy - Mth.cos(b) * r, color);
        }
    }

    // A round-cornered outline `thick` wide, inside the box.
    static void outline(GuiGraphics graphics, float x, float y, float w, float h, float radius, float thick,
            int color) {
        float r = Math.max(thick, Math.min(radius, Math.min(w, h) * 0.5F));
        GuiShapes.roundRect(graphics, x + r, y, w - r * 2.0F, thick, 0.0F, color);
        GuiShapes.roundRect(graphics, x + r, y + h - thick, w - r * 2.0F, thick, 0.0F, color);
        GuiShapes.roundRect(graphics, x, y + r, thick, h - r * 2.0F, 0.0F, color);
        GuiShapes.roundRect(graphics, x + w - thick, y + r, thick, h - r * 2.0F, 0.0F, color);
        GuiShapes.arc(graphics, x + r, y + r, r - thick, r, 270.0F, 360.0F, color);
        GuiShapes.arc(graphics, x + w - r, y + r, r - thick, r, 0.0F, 90.0F, color);
        GuiShapes.arc(graphics, x + w - r, y + h - r, r - thick, r, 90.0F, 180.0F, color);
        GuiShapes.arc(graphics, x + r, y + h - r, r - thick, r, 180.0F, 270.0F, color);
    }

    // A glow fading out round the hole, a thin line on its edge breathing with `pulse`, and bright pixel corners
    // standing a little off it.
    static void frame(GuiGraphics graphics, float x, float y, float w, float h, float pulse, float appear) {
        for (int i = 1; i <= 3; i++) {
            float out = i;
            outline(graphics, x - out, y - out, w + out * 2.0F, h + out * 2.0F, RADIUS + out, 1.0F,
                    GuiShapes.fade(TourCard.ACCENT, (0.2F - 0.05F * i) * (0.6F + 0.4F * pulse) * appear));
        }
        outline(graphics, x, y, w, h, RADIUS, 1.0F, GuiShapes.fade(TourCard.ACCENT, (0.5F + 0.4F * pulse) * appear));
        float off = 2.0F + pulse;
        float arm = 6.0F;
        int corner = GuiShapes.fade(GuiShapes.mix(TourCard.ACCENT, 0xFFFFFF, 0.35F), appear);
        float left = x - off;
        float top = y - off;
        float right = x + w + off;
        float bottom = y + h + off;
        GuiShapes.roundRect(graphics, left, top, arm, 2.0F, 0.0F, corner);
        GuiShapes.roundRect(graphics, left, top, 2.0F, arm, 0.0F, corner);
        GuiShapes.roundRect(graphics, right - arm, top, arm, 2.0F, 0.0F, corner);
        GuiShapes.roundRect(graphics, right - 2.0F, top, 2.0F, arm, 0.0F, corner);
        GuiShapes.roundRect(graphics, left, bottom - 2.0F, arm, 2.0F, 0.0F, corner);
        GuiShapes.roundRect(graphics, left, bottom - arm, 2.0F, arm, 0.0F, corner);
        GuiShapes.roundRect(graphics, right - arm, bottom - 2.0F, arm, 2.0F, 0.0F, corner);
        GuiShapes.roundRect(graphics, right - 2.0F, bottom - arm, 2.0F, arm, 0.0F, corner);
    }

    // A line with one bend from (ax, ay) on the card to (bx, by) on the frame, drawing itself out as the card fades
    // in, a dot where it lands and a ring rippling out of it.
    static void pointer(GuiGraphics graphics, float ax, float ay, float bx, float by, boolean vertical, float appear,
            float pulse, float ripple) {
        float[] points = vertical
                ? new float[] { ax, ay, ax, (ay + by) / 2.0F, bx, (ay + by) / 2.0F, bx, by }
                : new float[] { ax, ay, (ax + bx) / 2.0F, ay, (ax + bx) / 2.0F, by, bx, by };
        float length = 0.0F;
        for (int i = 2; i < points.length; i += 2) {
            length += Math.abs(points[i] - points[i - 2]) + Math.abs(points[i + 1] - points[i - 1]);
        }
        float left = length * appear;
        int shadow = GuiShapes.fade(0, 0.5F);
        int color = GuiShapes.fade(TourCard.ACCENT, 0.95F);
        for (int pass = 0; pass < 2; pass++) {
            float rest = left;
            for (int i = 2; i < points.length && rest > 0.0F; i += 2) {
                float x0 = points[i - 2];
                float y0 = points[i - 1];
                float piece = Math.abs(points[i] - x0) + Math.abs(points[i + 1] - y0);
                float part = piece <= 0.0F ? 0.0F : Math.min(1.0F, rest / piece);
                float x1 = x0 + (points[i] - x0) * part;
                float y1 = y0 + (points[i + 1] - y0) * part;
                float grow = pass == 0 ? 1.0F : 0.5F;
                GuiShapes.roundRect(graphics, Math.min(x0, x1) - grow, Math.min(y0, y1) - grow,
                        Math.abs(x1 - x0) + grow * 2.0F, Math.abs(y1 - y0) + grow * 2.0F, 0.0F,
                        pass == 0 ? shadow : color);
                rest -= piece;
            }
        }
        if (appear >= 1.0F) {
            GuiShapes.ring(graphics, bx, by, 2.5F + 5.0F * ripple, 1.0F,
                    GuiShapes.fade(TourCard.ACCENT, 0.7F * (1.0F - ripple)));
            GuiShapes.disc(graphics, bx, by, 2.5F, 0xFF000000);
            GuiShapes.disc(graphics, bx, by, 1.5F + 0.5F * pulse, 0xFF000000 | TourCard.LIGHT);
        }
    }

    // The box of the step just done flashes outward from where it was; `tick` pops a tick up in its middle.
    static void flash(float x, float y, float w, float h, boolean tick, long now) {
        FLASHED[0] = x;
        FLASHED[1] = y;
        FLASHED[2] = w;
        FLASHED[3] = h;
        flashedAt = now;
        ticked = tick;
    }

    static boolean flashing(long now) {
        return now - flashedAt < (ticked ? TICK_MS : FLASH_MS);
    }

    static void drawFlash(GuiGraphics graphics, long now) {
        long age = now - flashedAt;
        if (age < FLASH_MS) {
            float t = age / (float) FLASH_MS;
            float grow = 2.0F + 10.0F * (float) Ease.smooth(t);
            outline(graphics, FLASHED[0] - grow, FLASHED[1] - grow, FLASHED[2] + grow * 2.0F,
                    FLASHED[3] + grow * 2.0F, RADIUS + grow, 1.0F, GuiShapes.fade(TourCard.LIGHT, (1.0F - t) * 0.9F));
        }
        if (ticked && age < TICK_MS) {
            float t = age / (float) TICK_MS;
            float size = 1.0F + (float) Ease.backOut(Math.min(1.0F, t * 2.5F));
            float alpha = t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F;
            float cx = FLASHED[0] + FLASHED[2] / 2.0F;
            float cy = FLASHED[1] + FLASHED[3] / 2.0F;
            GuiShapes.disc(graphics, cx, cy, 6.5F * size, GuiShapes.fade(0x0C2016, 0.85F * alpha));
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0.0F);
            graphics.pose().scale(size, size, 1.0F);
            PixelIcons.draw(graphics, PixelIcons.Icon.CHECK, -PixelIcons.SIZE / 2, -PixelIcons.SIZE / 2, 1,
                    GuiShapes.fade(TourCard.ACCENT, alpha), false);
            graphics.pose().popPose();
        }
    }
}
