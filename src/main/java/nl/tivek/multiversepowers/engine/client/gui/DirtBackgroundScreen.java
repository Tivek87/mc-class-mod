package nl.tivek.multiversepowers.engine.client.gui;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;

public abstract class DirtBackgroundScreen extends Screen {
    public static final int PANEL_FILL = 0xB0000000;
    public static final int PANEL_BORDER = 0xFF4A4438;
    protected static final int TEXT_COLOR = 0xFFFFFF;
    public static final int MUTED_COLOR = 0xA8A090;
    protected static final int NOTICE_COLOR = 0xF2C84B;
    protected static final int DIVIDER_COLOR = 0x60FFFFFF;

    private static final int NOTICE_FILL = 0xC0302408;
    private static final int NOTICE_PADDING = 5;

    private static final ResourceLocation DIRT_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    private static final int BACKGROUND_TILE = 32;
    private static final float BACKGROUND_DIM = 0.25F;
    private static final int VIGNETTE_TOP = 0x00000000;
    private static final int VIGNETTE_BOTTOM = 0x80000000;

    protected DirtBackgroundScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        guiGraphics.setColor(BACKGROUND_DIM, BACKGROUND_DIM, BACKGROUND_DIM, 1.0F);
        guiGraphics.blit(DIRT_BACKGROUND, 0, 0, 0, 0.0F, 0.0F, this.width, this.height,
                BACKGROUND_TILE, BACKGROUND_TILE);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.fillGradient(0, this.height / 2, this.width, this.height, VIGNETTE_TOP, VIGNETTE_BOTTOM);
    }

    protected static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_FILL);
        guiGraphics.renderOutline(x, y, width, height, borderColor);
    }

    protected void drawBigCenteredString(GuiGraphics guiGraphics, Component text, int centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(this.font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }

    protected int drawWrappedLeft(GuiGraphics guiGraphics, Component text, int x, int y, int width,
                                  int maxLines, int bottom, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        int fitting = Math.max(0, (bottom - y) / this.lineHeight());
        int count = Math.min(lines.size(), Math.min(maxLines, fitting));
        for (int i = 0; i < count; i++) {
            guiGraphics.drawString(this.font, lines.get(i), x, y, color);
            y += this.lineHeight();
        }
        return y;
    }

    protected static Component labeled(Component label, Component text, int labelColor) {
        return Component.empty()
                .append(label.copy().withColor(labelColor))
                .append(" ")
                .append(text);
    }

    protected static void drawDivider(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width, y + 1, DIVIDER_COLOR);
    }

    protected int noticeHeight(@Nullable Component heading, Component body, int width, int maxLines) {
        int lines = Math.min(maxLines, this.font.split(body, width - 2 * NOTICE_PADDING).size());
        int headingLines = heading == null ? 0 : 1;
        return (headingLines + lines) * this.lineHeight() + 2 * NOTICE_PADDING - 1;
    }

    protected void drawNotice(GuiGraphics guiGraphics, @Nullable Component heading, Component body,
                              int x, int y, int width, int maxLines) {
        int height = this.noticeHeight(heading, body, width, maxLines);
        guiGraphics.fill(x, y, x + width, y + height, NOTICE_FILL);
        guiGraphics.renderOutline(x, y, width, height, 0xFF000000 | NOTICE_COLOR);

        int centerX = x + width / 2;
        int textY = y + NOTICE_PADDING;
        int bodyColor = heading == null ? NOTICE_COLOR : TEXT_COLOR;
        if (heading != null) {
            guiGraphics.drawCenteredString(this.font, heading, centerX, textY, 0xFF000000 | NOTICE_COLOR);
            textY += this.lineHeight();
        }
        List<FormattedCharSequence> lines = this.font.split(body, width - 2 * NOTICE_PADDING);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawCenteredString(this.font, lines.get(i), centerX, textY, 0xFF000000 | bodyColor);
            textY += this.lineHeight();
        }
    }

    protected static Component notice(String suffix) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".notice." + suffix);
    }

    protected int lineHeight() {
        return this.font.lineHeight + 1;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
