package nl.tivek.multiversepowers.character.thor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.engine.client.escape.EscapeGames;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The escape game of a player Thor holds by the throat, in his storm's colours: one second and one click to strike the
// gold zone, the time draining off under the bar like a fading spark.
public final class GrabEscape {
    public static final EscapeGames.Rules RULES = new EscapeGames.Rules(1, 1, 0, 0.1F, 0.0F, 2.2F, 20, false);
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
        int edge = view.lost() ? RED : view.won() ? GOLD : view.flash() > 0.0F
                ? GuiShapes.mix(STEEL, view.scored() ? 0xFFFFFF : RED, view.flash()) : STEEL;
        GuiShapes.roundRect(graphics, x - 8.0F, y - 20.0F, width + 16.0F, height + 30.0F, 2.0F,
                GuiShapes.fade(DEEP, 0.85F));
        // A bolt of storm light along the top, as on his cape's clasp.
        float left = x - 8.0F;
        float top = y - 20.0F;
        for (int k = 0; k < 6; k++) {
            float a = left + (width + 16.0F) * k / 6.0F;
            float b = left + (width + 16.0F) * (k + 1) / 6.0F;
            GuiShapes.stroke(graphics, a, top + (k % 2 == 0 ? 0.0F : 1.5F), b, top + (k % 2 == 0 ? 1.5F : 0.0F),
                    1.0F, GuiShapes.fade(BLUE, 0.8F));
        }
        GuiShapes.roundRect(graphics, x - 1.5F, y - 1.5F, width + 3.0F, height + 3.0F, 1.0F,
                GuiShapes.fade(edge, 0.95F));
        GuiShapes.roundRect(graphics, x, y, width, height, 0.5F, GuiShapes.fade(FILL, 1.0F));
        GuiShapes.roundRect(graphics, x + width * view.yellowAt(), y + 1.0F, width * view.rules().yellowWidth(),
                height - 2.0F, 0.5F, GuiShapes.fade(GOLD, 0.95F));
        float stripe = x + width * view.stripe();
        GuiShapes.roundRect(graphics, stripe - 2.5F, y - 3.0F, 5.0F, height + 6.0F, 1.0F, GuiShapes.fade(BLUE, 0.4F));
        GuiShapes.roundRect(graphics, stripe - 1.0F, y - 2.5F, 2.0F, height + 5.0F, 0.5F,
                GuiShapes.fade(0xF4FBFF, 1.0F));
        if (view.timeLeft() >= 0.0F) {
            GuiShapes.roundRect(graphics, x, y + height + 4.0F, width * view.timeLeft(), 2.0F, 1.0F,
                    GuiShapes.fade(BLUE, 0.9F));
        }
        GuiShapes.flush(graphics);
        String state = view.won() ? "free" : view.lost() ? "failed" : "title";
        graphics.drawCenteredString(font, Component.translatable(KEY + state), (int) (x + width / 2.0F),
                (int) (y - 15.0F), view.lost() ? RED : BLUE);
    }
}
