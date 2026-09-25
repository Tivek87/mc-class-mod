package nl.tivek.multiversepowers.classes.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

public class ClassCardWidget extends AbstractButton {
    private static final int NAME_Y = 6;
    private static final int TAGLINE_Y = 18;
    private static final int TEXT_PADDING = 4;
    private static final int SELECTED_FILL = 0xD0241C10;
    private static final int HOVER_BORDER = 0xFFFFFF;

    @Nullable
    private final Component tagline;
    private final int color;
    private final Runnable onPress;

    private boolean selected;

    public ClassCardWidget(int x, int y, int width, int height, Component name, @Nullable Component tagline,
                           int color, Runnable onPress) {
        super(x, y, width, height, name);
        this.tagline = tagline;
        this.color = color;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int border = this.selected ? this.color
                : (this.isHoveredOrFocused() ? HOVER_BORDER : DirtBackgroundScreen.PANEL_BORDER);

        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                this.selected ? SELECTED_FILL : DirtBackgroundScreen.PANEL_FILL);
        guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFF000000 | border);

        int centerX = this.getX() + this.width / 2;
        int textWidth = this.width - 2 * TEXT_PADDING;
        int nameY = this.tagline == null ? (this.height - font.lineHeight) / 2 + 1 : NAME_Y;
        guiGraphics.drawCenteredString(font, firstLine(font, this.getMessage(), textWidth), centerX,
                this.getY() + nameY, 0xFF000000 | this.color);
        if (this.tagline != null) {
            guiGraphics.drawCenteredString(font, firstLine(font, this.tagline, textWidth), centerX,
                    this.getY() + TAGLINE_Y, 0xFF000000 | DirtBackgroundScreen.MUTED_COLOR);
        }
    }

    private static FormattedCharSequence firstLine(Font font, Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
