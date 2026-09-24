package nl.tivek.multiversepowers.engine.client.gui;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Base screen with the classic opaque dirt background, plus the shared panel and title styling.
 *
 * <p>Vanilla's own menu background is only 25% black, so the world would stay visible behind it.
 * This draws a solid base first and tiles the dirt block texture over it instead.
 */
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

        // Darker towards the bottom, so the buttons stand out from the pattern.
        guiGraphics.fillGradient(0, this.height / 2, this.width, this.height, VIGNETTE_TOP, VIGNETTE_BOTTOM);
    }

    /** A dark rounded-looking panel with a one pixel border. */
    protected static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_FILL);
        guiGraphics.renderOutline(x, y, width, height, borderColor);
    }

    /** Draws text centred and enlarged, for screen titles. */
    protected void drawBigCenteredString(GuiGraphics guiGraphics, Component text, int centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(this.font, text, 0, 0, color);
        guiGraphics.pose().popPose();
    }

    /**
     * Draws text left-aligned and wrapped to {@code width}. Stops at {@code maxLines}, and never
     * draws a line that would pass {@code bottom}, so a panel can never overflow.
     *
     * @return the y just below the last drawn line
     */
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

    /** "Label: text", with only the label in the given colour. */
    protected static Component labeled(Component label, Component text, int labelColor) {
        return Component.empty()
                .append(label.copy().withColor(labelColor))
                .append(" ")
                .append(text);
    }

    /** A thin horizontal line that separates sections inside a panel. */
    protected static void drawDivider(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width, y + 1, DIVIDER_COLOR);
    }

    /** How tall {@link #drawNotice} will draw, so layouts can reserve the space up front. */
    protected int noticeHeight(@Nullable Component heading, Component body, int width, int maxLines) {
        int lines = Math.min(maxLines, this.font.split(body, width - 2 * NOTICE_PADDING).size());
        int headingLines = heading == null ? 0 : 1;
        return (headingLines + lines) * this.lineHeight() + 2 * NOTICE_PADDING - 1;
    }

    /**
     * A gold-bordered box for things every player must read, such as "your class locks nothing".
     * Centred text; with a heading, the heading is gold and the body white, without it the body is gold.
     */
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
