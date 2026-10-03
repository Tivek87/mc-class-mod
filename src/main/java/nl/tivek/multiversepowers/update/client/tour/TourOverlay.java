package nl.tivek.multiversepowers.update.client.tour;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.math.Ease;

// Draws the tour and keeps where it drew, for clicks: a step's card beside the spotlight on what it points at
// (`TourSpot`), the first ask, the pill saying what waits elsewhere, and what lasts past them (`TourToast`). Everything
// glides: the spotlight from one thing to the next, the card after it, fading in.
final class TourOverlay {
    enum Hit { NONE, BLOCK, NEXT, BACK, SKIP, SHOW, LATER, CONTINUE }

    private enum Side { BELOW, ABOVE, RIGHT, LEFT, NONE }

    private static final Side[] TALL = { Side.RIGHT, Side.LEFT, Side.BELOW, Side.ABOVE };
    private static final Side[] WIDE = { Side.BELOW, Side.ABOVE, Side.RIGHT, Side.LEFT };

    private static final float DIM = 0.55F;
    private static final int CARD = 180;
    private static final int WIDEST = 270;
    private static final int INTRO = 210;
    private static final int GAP = 12;
    private static final int MARGIN = 6;
    private static final int RING = 3;
    private static final float GLIDE = 14.0F;
    private static final long APPEAR_MS = 220L;
    private static final long FRESH_MS = 150L;
    private static final long RESET_MS = 400L;
    private static final long PULSE_MS = 1400L;
    private static final int SECOND_NOTE_TICKS = 3;
    private static final Object ASK = new Object();
    private static final Object PILL = new Object();

    // The frame and the card as shown, gliding to where they go.
    private static float frameX;
    private static float frameY;
    private static float frameWidth;
    private static float frameHeight;
    private static boolean framed;
    private static float cardX;
    private static float cardY;
    private static int cardWidth;
    private static int cardHeight;
    private static boolean placed;
    private static float dim;
    @Nullable
    private static Object was;
    private static long appearedAt;
    private static long lastFrame;
    private static long drawnAt = -RESET_MS;
    // What was drawn last, for clicks.
    @Nullable
    private static TourCard.Buttons buttons;
    @Nullable
    private static TourCard.Ask ask;
    @Nullable
    private static TourToast.Pill pill;
    @Nullable
    private static TourCard.Box card;
    private static int secondNote = -1;

    private TourOverlay() {
    }

    // A step's card; `mouse` while a screen is open (else its keys show), `next` what Next says, `hold` how far a held
    // Enter is to skipping.
    static void step(GuiGraphics graphics, TourStep step, @Nullable ScreenAnchors.Rect target, int number, int total,
            boolean mouse, boolean back, Component next, float hold, double mouseX, double mouseY) {
        long now = Util.getMillis();
        float glide = begin(now, step);
        Font font = Minecraft.getInstance().font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float appear = appear(now);
        dim += ((mouse ? 1.0F : 0.0F) - dim) * glide;

        float tx = 0.0F;
        float ty = 0.0F;
        float tw = 0.0F;
        float th = 0.0F;
        if (target != null) {
            tx = target.x() - RING;
            ty = target.y() - RING;
            tw = target.width() + RING * 2;
            th = target.height() + RING * 2;
            if (!framed) {
                frameX = tx - 16.0F;
                frameY = ty - 16.0F;
                frameWidth = tw + 32.0F;
                frameHeight = th + 32.0F;
                framed = true;
            }
            frameX += (tx - frameX) * glide;
            frameY += (ty - frameY) * glide;
            frameWidth += (tw - frameWidth) * glide;
            frameHeight += (th - frameHeight) * glide;
        } else {
            framed = false;
        }
        TourSpot.shade(graphics, width, height, dim * (framed ? DIM : DIM * 0.7F),
                framed ? new float[] { frameX, frameY, frameWidth, frameHeight } : null);

        // A wider card is a lower one: it widens until it fits beside what it points at.
        int widest = Math.min(WIDEST, width - MARGIN * 2);
        cardWidth = Math.min(widest, Math.max(CARD, TourCard.footerWidth(font, mouse, back, next) + TourCard.PAD * 2));
        TourCard.Body body;
        Side side;
        while (true) {
            body = TourCard.body(font, step, cardWidth - TourCard.PAD * 2);
            cardHeight = body.height();
            side = target == null ? Side.NONE : side(tx, ty, tw, th, width, height);
            if (side != Side.NONE || target == null || cardWidth >= widest) {
                break;
            }
            cardWidth = Math.min(widest, cardWidth + 40);
        }

        float px = (width - cardWidth) / 2.0F;
        float py = (height - cardHeight) / 2.0F;
        if (target != null) {
            float across = Mth.clamp(tx + tw / 2.0F - cardWidth / 2.0F, MARGIN, width - MARGIN - cardWidth);
            float along = Mth.clamp(ty + th / 2.0F - cardHeight / 2.0F, MARGIN, height - MARGIN - cardHeight);
            switch (side) {
                case BELOW -> { px = across; py = ty + th + GAP; }
                case ABOVE -> { px = across; py = ty - GAP - cardHeight; }
                case RIGHT -> { px = tx + tw + GAP; py = along; }
                case LEFT -> { px = tx - GAP - cardWidth; py = along; }
                // No room beside it anywhere: against the screen's edge on its roomier side, above or below.
                default -> { px = across; py = ty > height - ty - th ? MARGIN : height - MARGIN - cardHeight; }
            }
        }
        if (!placed) {
            cardX = px + (side == Side.RIGHT ? 8.0F : side == Side.LEFT ? -8.0F : 0.0F);
            cardY = py + (side == Side.ABOVE ? -8.0F : side == Side.RIGHT || side == Side.LEFT ? 0.0F : 8.0F);
            placed = true;
        }
        cardX += (px - cardX) * glide;
        cardY += (py - cardY) * glide;
        int x = Math.round(cardX);
        int y = Math.round(cardY);

        float pulse = 0.5F + 0.5F * Mth.sin(now % PULSE_MS / (float) PULSE_MS * Mth.TWO_PI);
        if (framed) {
            if (side != Side.NONE) {
                pointer(graphics, side, x, y, appear, pulse, now);
            }
            TourSpot.frame(graphics, frameX, frameY, frameWidth, frameHeight, pulse, appear);
        }

        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        TourCard.panel(graphics, x, y, cardWidth, cardHeight);
        buttons = TourCard.step(layer, font, step, body, x, y, cardWidth, cardHeight, number, total, mouse, back, next,
                hold, mouseX, mouseY);
        layer.finish();
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        card = new TourCard.Box(x, y, cardWidth, cardHeight);
        drawnAt = now;
    }

    // The first ask, in the middle of the menu.
    static void intro(GuiGraphics graphics, String version, int count, List<Component> highlights, double mouseX,
            double mouseY) {
        long now = Util.getMillis();
        float glide = begin(now, ASK);
        Font font = Minecraft.getInstance().font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float appear = appear(now);
        dim += (1.0F - dim) * glide;
        framed = false;
        TourSpot.shade(graphics, width, height, dim * DIM, null);
        cardWidth = Math.min(INTRO, width - MARGIN * 2);
        cardHeight = TourCard.introHeight(font, cardWidth - TourCard.PAD * 2, count, highlights);
        int x = (width - cardWidth) / 2;
        int y = Math.round((height - cardHeight) / 2.0F + 8.0F * (1.0F - appear));
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        TourCard.panel(graphics, x, y, cardWidth, cardHeight);
        ask = TourCard.intro(layer, font, version, count, highlights, x, y, cardWidth, cardHeight, mouseX, mouseY);
        layer.finish();
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        card = new TourCard.Box(x, y, cardWidth, cardHeight);
        drawnAt = now;
    }

    // The pill saying what waits somewhere else.
    static void pill(GuiGraphics graphics, Component label, boolean reachable, boolean mouse, float alpha,
            double mouseX, double mouseY) {
        long now = Util.getMillis();
        begin(now, PILL);
        framed = false;
        dim = 0.0F;
        pill = TourToast.pill(graphics, label, reachable, mouse, appear(now) * alpha, mouseX, mouseY);
        card = pill.all();
        drawnAt = now;
    }

    // What lasts past a step: its box flashing outward once done, and the toast once the tour is done or skipped.
    static void extras(GuiGraphics graphics) {
        long now = Util.getMillis();
        TourSpot.drawFlash(graphics, now);
        if (TourToast.showing(now)) {
            TourToast.draw(graphics, now);
        }
    }

    static Hit hit(double mouseX, double mouseY) {
        if (!up()) {
            return Hit.NONE;
        }
        if (buttons != null) {
            if (inside(buttons.next(), mouseX, mouseY)) {
                return Hit.NEXT;
            }
            if (inside(buttons.back(), mouseX, mouseY)) {
                return Hit.BACK;
            }
            if (inside(buttons.skip(), mouseX, mouseY)) {
                return Hit.SKIP;
            }
        }
        if (ask != null) {
            if (ask.show().contains(mouseX, mouseY)) {
                return Hit.SHOW;
            }
            if (ask.later().contains(mouseX, mouseY)) {
                return Hit.LATER;
            }
            if (ask.skip().contains(mouseX, mouseY)) {
                return Hit.SKIP;
            }
        }
        if (pill != null) {
            if (inside(pill.go(), mouseX, mouseY)) {
                return Hit.CONTINUE;
            }
            if (inside(pill.skip(), mouseX, mouseY)) {
                return Hit.SKIP;
            }
        }
        // What a step points at stays clickable, even where a card on a small screen lies over it.
        if (framed && mouseX >= frameX && mouseX < frameX + frameWidth && mouseY >= frameY
                && mouseY < frameY + frameHeight) {
            return Hit.NONE;
        }
        return ask != null || inside(card, mouseX, mouseY) ? Hit.BLOCK : Hit.NONE;
    }

    // Whether a step done is still flashing or a toast still showing.
    static boolean busy() {
        long now = Util.getMillis();
        return TourSpot.flashing(now) || TourToast.showing(now);
    }

    static boolean introUp() {
        return up() && ask != null;
    }

    static boolean stepUp() {
        return up() && buttons != null;
    }

    static boolean pillUp() {
        return up() && pill != null;
    }

    // A step done: its box flashes outward from where it was, with a tick and a chime that climbs as the tour goes on
    // when the player did what it asked (`acted`), a click when they pressed Next.
    static void done(boolean acted, int number) {
        long now = Util.getMillis();
        if (framed) {
            TourSpot.flash(frameX, frameY, frameWidth, frameHeight, acted, now);
        } else {
            TourSpot.flash(cardX, cardY, cardWidth, cardHeight, acted, now);
        }
        if (acted) {
            pling(Math.min(2.0F, 0.9F + 0.08F * number), 0.5F);
        } else {
            click();
        }
    }

    static void back() {
        sound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8F, 0.5F);
    }

    static void cheer() {
        TourToast.cheer(Util.getMillis());
        pling(1.0F, 0.6F);
        secondNote = SECOND_NOTE_TICKS;
    }

    static void skipped() {
        TourToast.skipped(Util.getMillis());
        click();
    }

    static void click() {
        sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.5F);
    }

    static void tick() {
        if (secondNote >= 0 && secondNote-- == 0) {
            pling(1.5F, 0.6F);
        }
    }

    private static void pling(float pitch, float volume) {
        sound(SoundEvents.NOTE_BLOCK_PLING.value(), pitch, volume);
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    // How far to glide this frame; a tour away for a moment comes back fresh, a new step fades in.
    private static float begin(long now, Object what) {
        if (now - drawnAt > RESET_MS) {
            framed = false;
            placed = false;
            was = null;
            dim = 0.0F;
        }
        if (what != was) {
            was = what;
            appearedAt = now;
            placed = false;
        }
        float seconds = Math.min(0.1F, Math.max(0.0F, (now - lastFrame) / 1000.0F));
        lastFrame = now;
        buttons = null;
        ask = null;
        pill = null;
        card = null;
        return 1.0F - (float) Math.exp(-GLIDE * seconds);
    }

    private static float appear(long now) {
        return (float) Ease.smooth(Mth.clamp((now - appearedAt) / (float) APPEAR_MS, 0.0F, 1.0F));
    }

    private static boolean up() {
        return Util.getMillis() - drawnAt < FRESH_MS;
    }

    // The first side with room for the card: beside a tall thing first, under or over a wide one.
    private static Side side(float x, float y, float w, float h, int width, int height) {
        for (Side side : h > w * 0.8F ? TALL : WIDE) {
            if (fits(side, x, y, w, h, width, height)) {
                return side;
            }
        }
        return Side.NONE;
    }

    private static boolean fits(Side side, float x, float y, float w, float h, int width, int height) {
        return switch (side) {
            case BELOW -> y + h + GAP + cardHeight <= height - MARGIN;
            case ABOVE -> y - GAP - cardHeight >= MARGIN;
            case RIGHT -> x + w + GAP + cardWidth <= width - MARGIN;
            case LEFT -> x - GAP - cardWidth >= MARGIN;
            case NONE -> true;
        };
    }

    // The line from the middle of the card's side facing the frame to the nearest point of the frame's facing side.
    private static void pointer(GuiGraphics graphics, Side side, int x, int y, float appear, float pulse, long now) {
        float ax;
        float ay;
        float bx;
        float by;
        boolean vertical = side == Side.BELOW || side == Side.ABOVE;
        if (vertical) {
            bx = Mth.clamp(x + cardWidth / 2.0F, frameX + 6.0F, frameX + frameWidth - 6.0F);
            ax = Mth.clamp(bx, x + 8.0F, x + cardWidth - 8.0F);
            ay = side == Side.BELOW ? y : y + cardHeight;
            by = side == Side.BELOW ? frameY + frameHeight : frameY;
        } else {
            by = Mth.clamp(y + cardHeight / 2.0F, frameY + 6.0F, frameY + frameHeight - 6.0F);
            ay = Mth.clamp(by, y + 8.0F, y + cardHeight - 8.0F);
            ax = side == Side.RIGHT ? x : x + cardWidth;
            bx = side == Side.RIGHT ? frameX + frameWidth : frameX;
        }
        TourSpot.pointer(graphics, ax, ay, bx, by, vertical, appear, pulse, now % PULSE_MS / (float) PULSE_MS);
    }

    private static boolean inside(@Nullable TourCard.Box box, double x, double y) {
        return box != null && box.contains(x, y);
    }
}
