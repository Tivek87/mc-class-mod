package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.docock.TentacleReach.COUNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.docock.TentacleReach;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// Doctor Octopus's crosshair: a steel ring round a point, like the lens of his goggles, and his four tentacles round
// it, each an arc at the corner where it sits on his back (1 top left, 2 top right, 3 bottom left, 4 bottom right) in
// the colour of what it does: free pale steel, on the ground sunk outward with a foot, holding amber and drawn in,
// squeezing red and throbbing. An arc kicks as its tentacle changes, eases where it goes, and in Selection Mode the
// picked ones light up blue with their numbers. It widens as he strikes or runs; on a creature four lens arcs turn in
// and lock round it, the point in its colour, a thin arc of its health round them. A short message shows under it for
// a moment when a pick or a key cannot be done. It reacts as every crosshair does (Crosshairs.Feel).
public final class OctoCrosshair {
    private static final String PREFIX = "octopus." + MultiversePowers.MODID + ".";
    private static final int STEEL = 0xE6ECF2;
    private static final int AMBER = 0xFFB547;
    private static final int GROUND = 0xC09A6E;
    private static final int FREE = 0xD8DEE6;
    private static final int HOLDING = 0xFFB547;
    private static final int SQUEEZING = 0xFF4A3A;
    private static final int PICKED = 0x59E1FF;
    private static final int MESSAGE_TICKS = 40;
    private static final float KICK_TICKS = 7.0F;

    // Per tentacle: its state last frame, when it last changed (ticks), and how far out (`reach`), how thick and how
    // picked its arc is drawn now, each eased towards where its state puts it.
    private static final int[] state = new int[COUNT];
    private static final float[] changed = new float[COUNT];
    private static final float[] reach = new float[COUNT];
    private static final float[] thick = new float[COUNT];
    private static final float[] lit = new float[COUNT];
    private static float lastTime = Float.NaN;
    private static float selecting;
    private static Component message = Component.empty();
    private static float messageAt = Float.NEGATIVE_INFINITY;
    private static boolean messageNew;

    private OctoCrosshair() {
    }

    static void tell(String key, Object... args) {
        message = Component.translatable(PREFIX + "hud." + key, args);
        messageNew = true;
    }

    public static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        float time = feel.time();
        float step = Float.isNaN(lastTime) ? 1.0F : Mth.clamp(time - lastTime, 0.0F, 5.0F);
        lastTime = time;
        if (messageNew) {
            messageAt = time;
            messageNew = false;
        }
        float aim = feel.aim();
        float ring = feel.radius(2.6F + 2.0F * feel.spread() - 0.4F * aim);
        float rx = cx + feel.swayX();
        float ry = cy + feel.swayY();
        boolean choosing = OctoControls.selecting();
        selecting = ease(selecting, choosing ? 1.0F : 0.0F, step, 0.45F);
        int steel = GuiShapes.mix(Crosshairs.tint(STEEL, feel, 0.0F), PICKED, 0.6F * selecting);
        Crosshairs.ring(graphics, rx, ry, ring, 0.9F + feel.thick(), steel, 0.9F);
        Crosshairs.dot(graphics, cx, cy, 0.6F + 0.3F * aim, Crosshairs.tint(AMBER, feel, 1.0F), 0.95F);
        float outer = ring;
        if (aim >= 0.05F) {
            float lens = ring + 3.0F - 1.0F * aim;
            Crosshairs.health(graphics, rx, ry, lens + 2.2F, feel);
            float turn = 45.0F * (1.0F - aim) + feel.turn() * Mth.RAD_TO_DEG;
            int color = Crosshairs.tint(STEEL, feel, 0.7F);
            for (int arc = 0; arc < 4; arc++) {
                float from = arc * 90.0F + 20.0F + turn;
                GuiShapes.arc(graphics, rx, ry, lens - 1.0F, lens + 1.0F, from, from + 50.0F,
                        GuiShapes.fade(0x000000, 0.35F * aim));
                GuiShapes.arc(graphics, rx, ry, lens - 0.45F, lens + 0.45F, from, from + 50.0F,
                        GuiShapes.fade(color, 0.9F * aim));
            }
            outer = lens + 2.2F;
        }
        ClientOcto.View view = ClientOcto.of(player);
        if (view != null) {
            outer = Math.max(outer, tentacles(graphics, view, rx, ry, outer, feel, time, step, choosing));
        }
        say(graphics, cx, cy + outer + 6.0F, time);
        return outer;
    }

    // The four tentacles' arcs round `inner`; says how far out they reach.
    private static float tentacles(GuiGraphics graphics, ClientOcto.View view, float cx, float cy, float inner,
            Crosshairs.Feel feel, float time, float step, boolean choosing) {
        int picked = OctoControls.picked();
        float base = inner + 3.5F;
        float far = base;
        ScreenAnchors.report("game.tentacles", (int) (cx - base - 8), (int) (cy - base - 8), (int) (2 * base + 16),
                (int) (2 * base + 16));
        for (int i = 0; i < COUNT; i++) {
            int now = view.ground(i) ? 1 : view.squeezing(i) ? 3 : view.holding(i) ? 2 : 0;
            if (now != state[i]) {
                state[i] = now;
                changed[i] = time;
            }
            float throb = now == 3 ? 0.5F + 0.5F * Mth.sin(time * 0.9F + i) : 0.0F;
            reach[i] = ease(reach[i], now == 1 ? 2.2F : now >= 2 ? -1.2F : 0.0F, step, 0.4F);
            thick[i] = ease(thick[i], now == 1 ? 1.1F : now == 2 ? 1.5F : now == 3 ? 1.8F : 1.1F, step, 0.4F);
            boolean on = choosing && (picked & 1 << i) != 0;
            lit[i] = ease(lit[i], on ? 1.0F : 0.0F, step, 0.5F);
            float kick = 1.0F - Mth.clamp((time - changed[i]) / KICK_TICKS, 0.0F, 1.0F);
            kick *= kick;
            int color = now == 1 ? GROUND : now == 3 ? SQUEEZING : now == 2 ? HOLDING : FREE;
            color = GuiShapes.mix(Crosshairs.tint(color, feel, 0.0F), PICKED, lit[i]);
            boolean left = TentacleReach.side(i) > 0;
            float middle = (TentacleReach.upper(i) ? left ? 315.0F : 45.0F : left ? 225.0F : 135.0F)
                    + feel.turn() * Mth.RAD_TO_DEG;
            float half = 17.0F + 10.0F * kick + 5.0F * lit[i] - (now == 2 ? 5.0F : 0.0F);
            float r = base + reach[i] + 2.0F * kick + 0.6F * throb + feel.radius(0.0F);
            float w = (thick[i] + 0.9F * lit[i] + 0.6F * throb + 0.8F * kick) * 0.5F;
            GuiShapes.arc(graphics, cx, cy, r - w - 0.6F, r + w + 0.6F, middle - half - 2.0F, middle + half + 2.0F,
                    GuiShapes.fade(0x000000, 0.4F));
            GuiShapes.arc(graphics, cx, cy, r - w, r + w, middle - half, middle + half, GuiShapes.fade(color, 0.95F));
            float sin = Mth.sin(middle * Mth.DEG_TO_RAD);
            float cos = -Mth.cos(middle * Mth.DEG_TO_RAD);
            if (now == 1) {
                // A foot planted outward: a short tick out from the arc's middle.
                Crosshairs.stroke(graphics, cx + sin * (r + w), cy + cos * (r + w), cx + sin * (r + w + 2.5F),
                        cy + cos * (r + w + 2.5F), 0.9F, color, 0.95F);
            }
            if (now >= 2) {
                // Holding: a claw shut on the arc's middle, pointing in.
                Crosshairs.stroke(graphics, cx + sin * (r - w), cy + cos * (r - w), cx + sin * (r - w - 1.8F),
                        cy + cos * (r - w - 1.8F), 1.0F + 0.5F * throb, color, 0.95F);
            }
            far = Math.max(far, r + w + (now == 1 ? 2.5F : 0.0F));
            if (lit[i] > 0.05F || selecting > 0.05F) {
                float alpha = Math.max(lit[i], 0.55F * selecting);
                number(graphics, i + 1, cx + sin * (r + w + 4.0F), cy + cos * (r + w + 4.0F),
                        GuiShapes.mix(FREE, PICKED, lit[i]), alpha);
            }
        }
        return far;
    }

    private static void number(GuiGraphics graphics, int n, float x, float y, int rgb, float alpha) {
        int a = Mth.clamp((int) (255 * alpha), 0, 255);
        if (a < 8) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        String text = Integer.toString(n);
        GuiShapes.flush(graphics);
        graphics.pose().pushPose();
        graphics.pose().translate(x - font.width(text) * 0.25F, y - 2.0F, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, text, 0, 0, a << 24 | rgb, true);
        graphics.pose().popPose();
    }

    // Selection Mode's name while it is on, and the last message while it lasts, under the crosshair.
    private static void say(GuiGraphics graphics, float cx, float y, float time) {
        Font font = Minecraft.getInstance().font;
        if (selecting > 0.05F) {
            y = line(graphics, font, Component.translatable(PREFIX + "selection"), cx, y, PICKED, selecting);
        }
        float age = time - messageAt;
        if (age >= 0.0F && age < MESSAGE_TICKS) {
            line(graphics, font, message, cx, y, 0xFFFFFF, Math.min(1.0F, (MESSAGE_TICKS - age) / 10.0F));
        }
    }

    private static float line(GuiGraphics graphics, Font font, Component text, float cx, float y, int rgb,
            float alpha) {
        int a = Mth.clamp((int) (255 * alpha), 0, 255);
        if (a >= 8) {
            GuiShapes.flush(graphics);
            graphics.pose().pushPose();
            graphics.pose().translate(cx - font.width(text) * 0.375F, y, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(font, text, 0, 0, a << 24 | rgb, true);
            graphics.pose().popPose();
        }
        return y + 8.0F;
    }

    // Eases `from` towards `to` by `rate` a tick, the same at any frame rate.
    private static float ease(float from, float to, float step, float rate) {
        return to + (from - to) * (float) Math.pow(1.0F - rate, step);
    }
}
