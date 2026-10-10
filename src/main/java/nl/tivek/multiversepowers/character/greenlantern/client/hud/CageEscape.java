package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.engine.client.escape.EscapeGames;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The escape game of a player caught in a Light Cage, in the ring's colours: a bar of hard light with a yellow and a
// smaller red zone; five hits in the yellow or three in the red break the cage. Five tries a round, then it starts
// over.
public final class CageEscape {
    public static final EscapeGames.Rules RULES = new EscapeGames.Rules(5, 5, 3, 0.15F, 0.05F, 1.7F, 0, true);
    private static final int GREEN = 0x6CFF8E;
    private static final int DEEP = 0x07170C;
    private static final int FILL = 0x0C2E17;
    private static final int YELLOW = 0xFFE066;
    private static final int RED = 0xFF4A4A;
    private static final int STRIPE = 0xF0FFF3;
    private static final String KEY = "escape." + MultiversePowers.MODID + ".cage.";

    private CageEscape() {
    }

    public static void register() {
        EscapeGames.register(LightBubble.ESCAPE, RULES, CageEscape::draw);
    }

    static void draw(GuiGraphics graphics, float x, float y, float width, float height, EscapeGames.View view) {
        Font font = Minecraft.getInstance().font;
        int edge = view.flash() > 0.0F ? GuiShapes.mix(GREEN, view.scored() ? 0xFFFFFF : RED, view.flash()) : GREEN;
        GuiShapes.roundRect(graphics, x - 8.0F, y - 20.0F, width + 16.0F, height + 36.0F, 4.0F,
                GuiShapes.fade(DEEP, 0.82F));
        GuiShapes.roundRect(graphics, x - 8.0F, y - 20.0F, width + 16.0F, 1.0F, 0.0F, GuiShapes.fade(GREEN, 0.7F));
        GuiShapes.roundRect(graphics, x - 1.5F, y - 1.5F, width + 3.0F, height + 3.0F, 2.0F,
                GuiShapes.fade(edge, 0.95F));
        GuiShapes.roundRect(graphics, x, y, width, height, 1.5F, GuiShapes.fade(FILL, 1.0F));
        GuiShapes.roundRect(graphics, x + width * view.yellowAt(), y + 1.0F, width * view.rules().yellowWidth(),
                height - 2.0F, 1.0F, GuiShapes.fade(YELLOW, 0.9F));
        GuiShapes.roundRect(graphics, x + width * view.redAt(), y + 1.0F, width * view.rules().redWidth(),
                height - 2.0F, 1.0F, GuiShapes.fade(RED, 0.95F));
        float stripe = x + width * view.stripe();
        GuiShapes.roundRect(graphics, stripe - 2.5F, y - 3.0F, 5.0F, height + 6.0F, 2.0F, GuiShapes.fade(GREEN, 0.35F));
        GuiShapes.roundRect(graphics, stripe - 1.0F, y - 2.5F, 2.0F, height + 5.0F, 1.0F, GuiShapes.fade(STRIPE, 1.0F));
        // The score: yellow hits on the left, red on the right, and the tries left in the middle.
        float pips = y + height + 7.0F;
        for (int k = 0; k < view.rules().yellow(); k++) {
            pip(graphics, x + 4.0F + k * 7.0F, pips, k < view.yellowHits(), YELLOW);
        }
        for (int k = 0; k < view.rules().red(); k++) {
            pip(graphics, x + width - 4.0F - k * 7.0F, pips, k < view.redHits(), RED);
        }
        float middle = x + width / 2.0F;
        for (int k = 0; k < view.rules().tries(); k++) {
            float at = middle + (k - (view.rules().tries() - 1) / 2.0F) * 6.0F;
            GuiShapes.disc(graphics, at, pips, 1.4F, GuiShapes.fade(k < view.tries() ? 0x3A5A44 : GREEN, 0.95F));
        }
        GuiShapes.flush(graphics);
        Component title = Component.translatable(KEY + (view.won() ? "free" : "title"));
        graphics.drawCenteredString(font, title, (int) middle, (int) (y - 15.0F), GREEN);
        graphics.drawCenteredString(font, Component.translatable(KEY + "hint"), (int) middle,
                (int) (pips + 4.0F), 0xB8D8C0);
    }

    private static void pip(GuiGraphics graphics, float x, float y, boolean lit, int rgb) {
        GuiShapes.disc(graphics, x, y, 2.4F, GuiShapes.fade(0x000000, 0.5F));
        GuiShapes.disc(graphics, x, y, 1.9F, GuiShapes.fade(lit ? rgb : 0x2A3A30, 1.0F));
    }
}
