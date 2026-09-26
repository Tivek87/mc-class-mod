package nl.tivek.multiversepowers.engine.client.gui;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;

public final class WipTag {
    public static final int AMBER = 0xFFB81C;
    private static final int INK = 0x1C1400;
    private static final int STRIPE = 6;
    private static final int LINE = 10;

    private WipTag() {
    }

    public static Component title() {
        return Component.translatable("gui." + MultiversePowers.MODID + ".wip").withStyle(ChatFormatting.BOLD);
    }

    public static Component about() {
        return Component.translatable("gui." + MultiversePowers.MODID + ".wip.about");
    }

    public static int chipWidth(Font font) {
        return font.width(title()) + 10;
    }

    public static void chip(GuiGraphics graphics, Font font, int x, int y) {
        int width = chipWidth(font);
        GuiShapes.roundRect(graphics, x, y, width, 12, 3.0F, 0xFF000000 | AMBER);
        GuiShapes.flush(graphics);
        graphics.drawString(font, title(), x + 5, y + 2, 0xFF000000 | INK, false);
    }

    public static int bannerHeight(Font font, int width) {
        return 22 + font.split(about(), width - 16).size() * LINE;
    }

    public static void banner(GuiGraphics graphics, Font font, int x, int y, int width) {
        int height = bannerHeight(font, width);
        GuiShapes.roundRect(graphics, x, y, width, height, 4.0F, GuiShapes.fade(AMBER, 0.16F));
        float right = x + width;
        for (int s = -STRIPE; s < width + 5; s += 2 * STRIPE) {
            GuiShapes.quad(graphics, Math.clamp(x + s, x, right), y + 5, Math.clamp(x + s + 5, x, right), y,
                    Math.clamp(x + s + 5 + STRIPE, x, right), y, Math.clamp(x + s + STRIPE, x, right), y + 5,
                    0xFF000000 | AMBER);
        }
        GuiShapes.flush(graphics);
        graphics.drawString(font, title(), x + 8, y + 9, 0xFF000000 | AMBER, true);
        List<FormattedCharSequence> lines = font.split(about(), width - 16);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + 8, y + 20 + i * LINE, 0xFFE9E1CF, false);
        }
    }
}
