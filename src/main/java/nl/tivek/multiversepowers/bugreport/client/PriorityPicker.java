package nl.tivek.multiversepowers.bugreport.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

// Low, medium and high side by side, the chosen one lit in its own colour; a click or the arrow keys pick one.
final class PriorityPicker extends AbstractWidget {
    private static final int GAP = 3;
    private static final BugReporter.Priority[] ALL = BugReporter.Priority.values();

    private final Font font;
    private final Consumer<BugReporter.Priority> picked;
    private BugReporter.Priority value;

    PriorityPicker(Font font, int x, int y, int width, int height, BugReporter.Priority value,
            Consumer<BugReporter.Priority> picked) {
        super(x, y, width, height, BugReportScreen.words("priority"));
        this.font = font;
        this.value = value;
        this.picked = picked;
    }

    private int segment() {
        return (this.getWidth() - GAP * (ALL.length - 1)) / ALL.length;
    }

    private int left(int index) {
        return this.getX() + index * (this.segment() + GAP);
    }

    private int width(int index) {
        return index == ALL.length - 1 ? this.getX() + this.getWidth() - this.left(index) : this.segment();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int y = this.getY();
        int height = this.getHeight();
        for (int i = 0; i < ALL.length; i++) {
            BugReporter.Priority priority = ALL[i];
            int x = this.left(i);
            int width = this.width(i);
            boolean chosen = priority == this.value;
            boolean over = this.isHovered() && mouseX >= x && mouseX < x + width;
            int fill = chosen ? 0xFF000000 | priority.color : over ? 0xFF3C3C3C : 0xFF262626;
            graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF000000);
            graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, chosen ? 0x60FFFFFF : 0x18FFFFFF);
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0x40000000);
            if (chosen && this.isFocused()) {
                graphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
            }
            Component label = BugReportScreen.words("priority." + priority.id());
            int color = chosen ? 0xFF101010 : 0xFF000000 | priority.color;
            graphics.drawString(this.font, label, x + (width - this.font.width(label)) / 2, y + (height - 8) / 2,
                    color, false);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        for (int i = 0; i < ALL.length; i++) {
            if (mouseX >= this.left(i) && mouseX < this.left(i) + this.width(i)) {
                this.pick(ALL[i]);
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.active || !this.visible) {
            return false;
        }
        int step = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : 0;
        if (step == 0) {
            return false;
        }
        int next = Math.max(0, Math.min(ALL.length - 1, this.value.ordinal() + step));
        this.pick(ALL[next]);
        return true;
    }

    private void pick(BugReporter.Priority priority) {
        if (priority != this.value) {
            this.value = priority;
            this.picked.accept(priority);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty().append(this.getMessage()).append(": ")
                .append(BugReportScreen.words("priority." + this.value.id())));
    }
}
