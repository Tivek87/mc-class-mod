package nl.tivek.multiversepowers.update.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;
import org.lwjgl.glfw.GLFW;

/** Every version newer than the one running, with its notes laid out by {@link ChangelogLayout}; scrolls. */
final class ChangelogScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_TOP = 44;
    private static final int BOTTOM_SPACE = 34;
    private static final int PADDING = 10;
    private static final int SCROLLBAR = 6;
    private static final int SCROLL_STEP = 20;

    private final Screen parent;
    private List<ChangelogLayout.Block> blocks = List.of();
    private int contentHeight;
    private double scroll;
    private int left;
    private int panelWidth;
    private int panelBottom;

    ChangelogScreen(Screen parent) {
        super(UpdateManagerScreen.text("changelog.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_WIDTH, this.width - 24);
        this.left = (this.width - this.panelWidth) / 2;
        this.panelBottom = Math.max(PANEL_TOP + 40, this.height - BOTTOM_SPACE);
        this.blocks = ChangelogLayout.build(this.font, UpdateChecker.newer(),
                this.panelWidth - 2 * PADDING - SCROLLBAR);
        this.contentHeight = this.blocks.stream().mapToInt(ChangelogLayout.Block::height).sum() + 2 * PADDING;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
        this.addRenderableWidget(Button.builder(UpdateManagerScreen.text("back"), button -> this.onClose())
                .bounds(this.width / 2 - 60, this.height - 27, 120, 20).build());
    }

    private double maxScroll() {
        return Math.max(0, this.contentHeight - (this.panelBottom - PANEL_TOP));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.drawBigCenteredString(graphics, this.title, this.width / 2, 10, 1.5F, TEXT_COLOR);
        int count = UpdateChecker.newer().size();
        Component since = UpdateManagerScreen.text(count == 1 ? "changelog.since_one" : "changelog.since_many",
                count, "v" + UpdateChecker.installed());
        graphics.drawCenteredString(this.font, since, this.width / 2, 27, MUTED_COLOR);
        drawPanel(graphics, this.left, PANEL_TOP, this.panelWidth, this.panelBottom - PANEL_TOP, PANEL_BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.enableScissor(this.left + 1, PANEL_TOP + 1, this.left + this.panelWidth - 1, this.panelBottom - 1);
        int y = PANEL_TOP + PADDING - (int) this.scroll;
        for (ChangelogLayout.Block block : this.blocks) {
            if (y + block.height() >= PANEL_TOP && y <= this.panelBottom) {
                block.draw(graphics, this.left + PADDING, y);
            }
            y += block.height();
        }
        graphics.flush();
        graphics.disableScissor();
        this.drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        double max = this.maxScroll();
        if (max <= 0) {
            return;
        }
        int view = this.panelBottom - PANEL_TOP - 4;
        int x = this.left + this.panelWidth - SCROLLBAR;
        int thumb = Math.max(16, (int) (view * (double) view / (view + max)));
        int thumbY = PANEL_TOP + 2 + (int) ((view - thumb) * (this.scroll / max));
        graphics.fill(x, PANEL_TOP + 2, x + 3, PANEL_TOP + 2 + view, 0x40FFFFFF);
        graphics.fill(x, thumbY, x + 3, thumbY + thumb, 0xC0FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollBy(-scrollY * SCROLL_STEP);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int page = this.panelBottom - PANEL_TOP - SCROLL_STEP;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> this.scrollBy(-SCROLL_STEP);
            case GLFW.GLFW_KEY_DOWN -> this.scrollBy(SCROLL_STEP);
            case GLFW.GLFW_KEY_PAGE_UP -> this.scrollBy(-page);
            case GLFW.GLFW_KEY_PAGE_DOWN -> this.scrollBy(page);
            case GLFW.GLFW_KEY_HOME -> this.scrollBy(-this.contentHeight);
            case GLFW.GLFW_KEY_END -> this.scrollBy(this.contentHeight);
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    private void scrollBy(double amount) {
        this.scroll = Mth.clamp(this.scroll + amount, 0.0, this.maxScroll());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
