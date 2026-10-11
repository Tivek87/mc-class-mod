package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.engine.client.escape.EscapeGames;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The escape game of a player caught in a Light Cage, in the ring's colours: a glowing bar of hard light with a yellow
// zone and a smaller red one in its middle; five hits in the yellow or three in the red break the cage. Five tries a
// round, then it starts over.
public final class CageEscape {
    public static final EscapeGames.Rules RULES = new EscapeGames.Rules(5, 5, 3, 0.15F, 0.05F, 0.85F, 0, true);
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
        long ms = Util.getMillis();
        float breath = 0.5F + 0.5F * Mth.sin(ms / 260.0F);
        float flash = view.flash();
        boolean missed = flash > 0.0F && !view.scored();
        // A miss shakes the bar a moment.
        float shake = missed ? Mth.sin(ms / 18.0F) * 2.5F * flash : 0.0F;
        x += shake;
        float panelX = x - 12.0F;
        float panelY = y - 24.0F;
        float panelWidth = width + 24.0F;
        float panelHeight = height + 50.0F;
        int edge = flash > 0.0F ? GuiShapes.mix(GREEN, view.scored() ? 0xFFFFFF : RED, flash) : GREEN;
        GuiShapes.glow(graphics, panelX, panelY, panelWidth, panelHeight, 6.0F, edge, 0.35F + 0.25F * breath);
        GuiShapes.roundRect(graphics, panelX, panelY, panelWidth, panelHeight, 5.0F, GuiShapes.fade(DEEP, 0.88F));
        GuiShapes.gradient(graphics, panelX + 2.0F, panelY + 2.0F, panelWidth - 4.0F, panelHeight * 0.45F,
                GuiShapes.fade(GREEN, 0.10F), GuiShapes.fade(GREEN, 0.0F));
        GuiShapes.roundRect(graphics, panelX + 6.0F, panelY, panelWidth - 12.0F, 1.0F, 0.0F,
                GuiShapes.fade(GREEN, 0.85F));
        // The bar: a groove of hard light with marks every tenth.
        GuiShapes.roundRect(graphics, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 3.0F,
                GuiShapes.fade(edge, 0.95F));
        GuiShapes.roundRect(graphics, x, y, width, height, 2.0F, GuiShapes.fade(FILL, 1.0F));
        GuiShapes.gradient(graphics, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, GuiShapes.fade(0x000000, 0.45F),
                GuiShapes.fade(GREEN, 0.08F));
        for (int k = 1; k < 10; k++) {
            float mark = x + width * k / 10.0F;
            GuiShapes.roundRect(graphics, mark - 0.5F, y + height - 4.0F, 1.0F, 3.0F, 0.0F,
                    GuiShapes.fade(GREEN, 0.25F));
        }
        // The yellow zone and the red one in its middle, glowing, brighter the moment one is struck.
        float yellowX = x + width * view.yellowAt();
        float yellowWidth = width * view.rules().yellowWidth();
        float hot = view.scored() ? flash : 0.0F;
        GuiShapes.glow(graphics, yellowX, y + 1.0F, yellowWidth, height - 2.0F, 4.0F, YELLOW,
                0.45F + 0.25F * breath + hot);
        GuiShapes.gradient(graphics, yellowX, y + 1.0F, yellowWidth, height - 2.0F,
                GuiShapes.fade(GuiShapes.mix(YELLOW, 0xFFFFFF, 0.35F + 0.5F * hot), 1.0F), GuiShapes.fade(0xE0A82A, 1.0F));
        if (view.rules().red() > 0) {
            float redX = x + width * view.redAt();
            float redWidth = width * view.rules().redWidth();
            GuiShapes.glow(graphics, redX, y + 1.0F, redWidth, height - 2.0F, 3.0F, RED, 0.6F + 0.3F * breath);
            GuiShapes.gradient(graphics, redX, y + 1.0F, redWidth, height - 2.0F,
                    GuiShapes.fade(GuiShapes.mix(RED, 0xFFFFFF, 0.3F), 1.0F), GuiShapes.fade(0xB01E1E, 1.0F));
        }
        // The stripe: a fading trail behind it, its glow, its bright core and a mark over and under the bar.
        float[] trail = view.trail();
        for (int k = trail.length - 1; k >= 0; k--) {
            float at = x + width * trail[k];
            float fade = 0.35F * (1.0F - (k + 1.0F) / (trail.length + 1.0F));
            GuiShapes.roundRect(graphics, at - 1.5F, y - 1.0F, 3.0F, height + 2.0F, 1.0F, GuiShapes.fade(GREEN, fade));
        }
        float stripe = x + width * view.stripe();
        GuiShapes.glow(graphics, stripe - 1.0F, y - 3.0F, 2.0F, height + 6.0F, 4.0F, GREEN, 0.9F);
        GuiShapes.roundRect(graphics, stripe - 1.5F, y - 3.5F, 3.0F, height + 7.0F, 1.5F, GuiShapes.fade(STRIPE, 1.0F));
        GuiShapes.quad(graphics, stripe - 3.5F, y - 7.0F, stripe + 3.5F, y - 7.0F, stripe, y - 3.0F, stripe,
                y - 3.0F, GuiShapes.fade(STRIPE, 1.0F));
        GuiShapes.quad(graphics, stripe - 3.5F, y + height + 7.0F, stripe, y + height + 3.0F, stripe,
                y + height + 3.0F, stripe + 3.5F, y + height + 7.0F, GuiShapes.fade(STRIPE, 1.0F));
        // Where the last click struck: a ring opening out, white if it scored, red if not.
        if (flash > 0.0F) {
            float clicked = x + width * view.clickedAt();
            GuiShapes.ring(graphics, clicked, y + height / 2.0F, 4.0F + 12.0F * (1.0F - flash), 1.5F,
                    GuiShapes.fade(view.scored() ? 0xFFFFFF : RED, flash));
        }
        // The score: yellow hits on the left, red on the right, and the tries left in the middle.
        float pips = y + height + 13.0F;
        for (int k = 0; k < view.rules().yellow(); k++) {
            pip(graphics, x + 5.0F + k * 9.0F, pips, k < view.yellowHits(), YELLOW);
        }
        for (int k = 0; k < view.rules().red(); k++) {
            pip(graphics, x + width - 5.0F - k * 9.0F, pips, k < view.redHits(), RED);
        }
        float middle = x + width / 2.0F;
        for (int k = 0; k < view.rules().tries(); k++) {
            float at = middle + (k - (view.rules().tries() - 1) / 2.0F) * 8.0F;
            boolean left = k >= view.tries();
            GuiShapes.disc(graphics, at, pips, 2.0F, GuiShapes.fade(left ? GREEN : 0x24402C, 0.95F));
        }
        GuiShapes.flush(graphics);
        Component title = Component.translatable(KEY + (view.won() ? "free" : "title"));
        graphics.drawCenteredString(font, title, (int) middle, (int) (y - 17.0F), view.won() ? 0xFFFFFF : GREEN);
        graphics.drawCenteredString(font, Component.translatable(KEY + "hint"), (int) middle, (int) (pips + 6.0F),
                0xB8D8C0);
    }

    private static void pip(GuiGraphics graphics, float x, float y, boolean lit, int rgb) {
        if (lit) {
            GuiShapes.disc(graphics, x, y, 4.5F, GuiShapes.fade(rgb, 0.25F));
        }
        GuiShapes.disc(graphics, x, y, 3.2F, GuiShapes.fade(0x000000, 0.6F));
        GuiShapes.disc(graphics, x, y, 2.6F, GuiShapes.fade(lit ? rgb : 0x2A3A30, 1.0F));
        if (lit) {
            GuiShapes.disc(graphics, x - 0.7F, y - 0.7F, 0.9F, GuiShapes.fade(0xFFFFFF, 0.8F));
        }
    }
}
