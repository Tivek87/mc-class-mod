package nl.tivek.multiversepowers.bugreport.client;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;
import nl.tivek.multiversepowers.update.client.UpdateChecker;

public final class BugReportScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 226;
    private static final int SENT_COLOR = 0x6EE7A0;
    private static final int ERROR_COLOR = 0xFF7B7B;

    @Nullable
    private final Screen parent;
    private String name = "";
    private String description = "";
    private BugReporter.Priority priority = BugReporter.Priority.MEDIUM;
    @Nullable
    private CompletableFuture<BugReporter.Result> pending;
    @Nullable
    private BugReporter.Result result;
    @Nullable
    private Button send;
    private int left;
    private int top;
    private int panelWidth;

    public BugReportScreen(@Nullable Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    static MutableComponent text(String key, Object... args) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".bugreport." + key, args);
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(6, (this.height - PANEL_HEIGHT) / 2);
        int x = this.left + 10;
        int inner = this.panelWidth - 20;
        int half = (inner - 6) / 2;

        EditBox nameBox = new EditBox(this.font, x, this.top + 40, inner, 20, text("name"));
        nameBox.setMaxLength(BugReporter.TITLE_MAX);
        nameBox.setHint(text("name.hint"));
        nameBox.setValue(this.name);
        nameBox.setResponder(value -> this.name = value);
        this.addRenderableWidget(nameBox);

        MultiLineEditBox descriptionBox = new MultiLineEditBox(this.font, x, this.top + 77, inner, 72,
                text("description.hint"), text("description"));
        descriptionBox.setCharacterLimit(BugReporter.DESCRIPTION_MAX);
        descriptionBox.setValue(this.description);
        descriptionBox.setValueListener(value -> this.description = value);
        this.addRenderableWidget(descriptionBox);

        this.addRenderableWidget(CycleButton.<BugReporter.Priority>builder(
                        value -> text("priority." + value.id()).withColor(value.color))
                .withValues(BugReporter.Priority.values())
                .withInitialValue(this.priority)
                .create(x, this.top + 155, inner, 20, text("priority"), (button, value) -> this.priority = value));

        this.addRenderableWidget(Button.builder(text("back"), button -> this.onClose())
                .bounds(x, this.top + 196, half, 20).build());
        this.send = this.addRenderableWidget(Button.builder(text("send"), button -> this.send())
                .bounds(x + half + 6, this.top + 196, inner - half - 6, 20).build());
        this.setInitialFocus(nameBox);
        this.refreshSend();
    }

    private void send() {
        if (this.pending != null || this.name.isBlank() || this.description.isBlank()) {
            return;
        }
        this.result = null;
        this.pending = BugReporter.send(this.name, this.description, this.priority);
        this.refreshSend();
    }

    @Override
    public void tick() {
        if (this.pending != null && this.pending.isDone()) {
            this.result = this.pending.join();
            this.pending = null;
            if (this.result.outcome() == BugReporter.Outcome.SENT) {
                this.name = "";
                this.description = "";
                this.priority = BugReporter.Priority.MEDIUM;
                this.rebuildWidgets();
            }
        }
        this.refreshSend();
    }

    private void refreshSend() {
        if (this.send != null) {
            this.send.active = this.pending == null && !this.name.isBlank() && !this.description.isBlank();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.left, this.top, this.panelWidth, PANEL_HEIGHT, PANEL_BORDER);
        int x = this.left + 10;
        graphics.drawString(this.font, this.title, x, this.top + 10, TEXT_COLOR);
        drawDivider(graphics, x, this.top + 23, this.panelWidth - 20);
        graphics.drawString(this.font, text("name"), x, this.top + 29, MUTED_COLOR);
        graphics.drawString(this.font, text("description"), x, this.top + 66, MUTED_COLOR);
        this.drawStatus(graphics, x, this.top + 181);
    }

    private void drawStatus(GuiGraphics graphics, int x, int y) {
        if (this.pending != null) {
            graphics.drawString(this.font, text("sending"), x, y, TEXT_COLOR);
            return;
        }
        if (this.result == null) {
            Component as = text("as", BugReporter.username(), UpdateChecker.installed());
            graphics.drawString(this.font, as, x, y, MUTED_COLOR);
            return;
        }
        switch (this.result.outcome()) {
            case SENT -> graphics.drawString(this.font, text("sent", this.result.issue()), x, y, SENT_COLOR);
            case LIMITED -> graphics.drawString(this.font, text("error.limited"), x, y, ERROR_COLOR);
            case REJECTED -> graphics.drawString(this.font, text("error.rejected"), x, y, ERROR_COLOR);
            case FAILED -> graphics.drawString(this.font, text("error.network"), x, y, ERROR_COLOR);
        }
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
