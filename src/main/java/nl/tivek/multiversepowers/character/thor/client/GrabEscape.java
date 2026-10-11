package nl.tivek.multiversepowers.character.thor.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.engine.client.escape.EscapeGames;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The escape game of a player Thor holds by the throat, in his storm's colours: one second and one click to strike the
// gold zone, the time draining off under the bar like a fading spark.
public final class GrabEscape {
    public static final EscapeGames.Rules RULES = new EscapeGames.Rules(1, 1, 0, 0.1F, 0.0F, 1.1F, 20, false);
    private static final int STEEL = 0xC9D6E2;
    private static final int BLUE = 0x9FD8FF;
    private static final int DEEP = 0x070C14;
    private static final int FILL = 0x101C2C;
    private static final int GOLD = 0xFFD54A;
    private static final int RED = 0xFF5A4A;
    private static final String KEY = "escape." + MultiversePowers.MODID + ".grab.";

    private GrabEscape() {
    }

    public static void register() {
        EscapeGames.register(ThorGrab.ESCAPE, RULES, GrabEscape::draw);
    }

    static void draw(GuiGraphics graphics, float x, float y, float width, float height, EscapeGames.View view) {
        Font font = Minecraft.getInstance().font;
        long ms = Util.getMillis();
        float breath = 0.5F + 0.5F * Mth.sin(ms / 200.0F);
        float flash = view.flash();
        int edge = view.lost() ? RED : view.won() ? GOLD : flash > 0.0F
                ? GuiShapes.mix(STEEL, view.scored() ? 0xFFFFFF : RED, flash) : STEEL;
        float panelX = x - 12.0F;
        float panelY = y - 24.0F;
        float panelWidth = width + 24.0F;
        float panelHeight = height + 40.0F;
        GuiShapes.glow(graphics, panelX, panelY, panelWidth, panelHeight, 6.0F, view.lost() ? RED : BLUE,
                0.35F + 0.25F * breath);
        GuiShapes.roundRect(graphics, panelX, panelY, panelWidth, panelHeight, 3.0F, GuiShapes.fade(DEEP, 0.9F));
        GuiShapes.gradient(graphics, panelX + 2.0F, panelY + 2.0F, panelWidth - 4.0F, panelHeight * 0.45F,
                GuiShapes.fade(BLUE, 0.10F), GuiShapes.fade(BLUE, 0.0F));
        // A bolt of storm light along the top, as on his cape's clasp, flickering.
        for (int k = 0; k < 8; k++) {
            float a = panelX + panelWidth * k / 8.0F;
            float b = panelX + panelWidth * (k + 1) / 8.0F;
            float flicker = 0.6F + 0.4F * Mth.sin(ms / 70.0F + k * 1.3F);
            GuiShapes.stroke(graphics, a, panelY + (k % 2 == 0 ? 0.0F : 2.0F), b, panelY + (k % 2 == 0 ? 2.0F : 0.0F),
                    1.2F, GuiShapes.fade(BLUE, 0.9F * flicker));
        }
        GuiShapes.roundRect(graphics, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 2.0F,
                GuiShapes.fade(edge, 0.95F));
        GuiShapes.roundRect(graphics, x, y, width, height, 1.0F, GuiShapes.fade(FILL, 1.0F));
        GuiShapes.gradient(graphics, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, GuiShapes.fade(0x000000, 0.45F),
                GuiShapes.fade(BLUE, 0.08F));
        for (int k = 1; k < 10; k++) {
            float mark = x + width * k / 10.0F;
            GuiShapes.roundRect(graphics, mark - 0.5F, y + height - 4.0F, 1.0F, 3.0F, 0.0F,
                    GuiShapes.fade(STEEL, 0.25F));
        }
        float goldX = x + width * view.yellowAt();
        float goldWidth = width * view.rules().yellowWidth();
        float hot = view.scored() ? flash : 0.0F;
        GuiShapes.glow(graphics, goldX, y + 1.0F, goldWidth, height - 2.0F, 4.0F, GOLD, 0.5F + 0.25F * breath + hot);
        GuiShapes.gradient(graphics, goldX, y + 1.0F, goldWidth, height - 2.0F,
                GuiShapes.fade(GuiShapes.mix(GOLD, 0xFFFFFF, 0.35F + 0.5F * hot), 1.0F), GuiShapes.fade(0xD49A1A, 1.0F));
        float[] trail = view.trail();
        for (int k = trail.length - 1; k >= 0; k--) {
            float at = x + width * trail[k];
            float fade = 0.35F * (1.0F - (k + 1.0F) / (trail.length + 1.0F));
            GuiShapes.roundRect(graphics, at - 1.5F, y - 1.0F, 3.0F, height + 2.0F, 1.0F, GuiShapes.fade(BLUE, fade));
        }
        float stripe = x + width * view.stripe();
        GuiShapes.glow(graphics, stripe - 1.0F, y - 3.0F, 2.0F, height + 6.0F, 4.0F, BLUE, 0.9F);
        GuiShapes.roundRect(graphics, stripe - 1.5F, y - 3.5F, 3.0F, height + 7.0F, 1.0F,
                GuiShapes.fade(0xF4FBFF, 1.0F));
        if (flash > 0.0F) {
            float clicked = x + width * view.clickedAt();
            GuiShapes.ring(graphics, clicked, y + height / 2.0F, 4.0F + 12.0F * (1.0F - flash), 1.5F,
                    GuiShapes.fade(view.scored() ? 0xFFFFFF : RED, flash));
        }
        // The time draining off under the bar, from blue to red as it runs out.
        if (view.timeLeft() >= 0.0F) {
            float left = view.timeLeft();
            GuiShapes.roundRect(graphics, x, y + height + 6.0F, width, 3.0F, 1.5F, GuiShapes.fade(0x000000, 0.5F));
            GuiShapes.roundRect(graphics, x, y + height + 6.0F, width * left, 3.0F, 1.5F,
                    GuiShapes.fade(GuiShapes.mix(RED, BLUE, left), 0.95F));
        }
        GuiShapes.flush(graphics);
        String state = view.won() ? "free" : view.lost() ? "failed" : "title";
        graphics.drawCenteredString(font, Component.translatable(KEY + state), (int) (x + width / 2.0F),
                (int) (y - 17.0F), view.lost() ? RED : view.won() ? GOLD : BLUE);
    }
}
