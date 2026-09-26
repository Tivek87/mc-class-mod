package nl.tivek.multiversepowers.bugreport.client;

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
    static final int PANEL_WIDTH = 300;
    static final int PANEL_HEIGHT = 226;
    static final int SENT_COLOR = 0x6EE7A0;
    static final int ERROR_COLOR = 0xFF7B7B;

    @Nullable
    private final Screen parent;
    private final BugReporter.Kind kind;
    @Nullable
    private BugReporter.Result shown;
    @Nullable
    private Button send;
    private int left;
    private int top;
    private int panelWidth;

    private BugReportScreen(@Nullable Screen parent, BugReporter.Kind kind) {
        super(text(kind.key("title")));
        this.parent = parent;
        this.kind = kind;
        this.shown = ReportStore.result(kind);
    }

    public static BugReportScreen bug(@Nullable Screen parent) {
        return new BugReportScreen(parent, BugReporter.Kind.BUG);
    }

    public static BugReportScreen idea(@Nullable Screen parent) {
        return new BugReportScreen(parent, BugReporter.Kind.IDEA);
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
        int third = (inner - 12) / 3;
        ReportStore.Draft draft = this.draft();

        EditBox nameBox = new EditBox(this.font, x, this.top + 40, inner, 20, text("name"));
        nameBox.setMaxLength(BugReporter.TITLE_MAX);
        nameBox.setHint(text(this.kind.key("name.hint")));
        nameBox.setValue(draft.name());
        nameBox.setResponder(value -> ReportStore.draft(this.kind, this.draft().withName(value)));
        this.addRenderableWidget(nameBox);

        MultiLineEditBox descriptionBox = new MultiLineEditBox(this.font, x, this.top + 77, inner, 72,
                text(this.kind.key("description.hint")), text("description"));
        descriptionBox.setCharacterLimit(BugReporter.DESCRIPTION_MAX);
        descriptionBox.setValue(draft.description());
        descriptionBox.setValueListener(value -> ReportStore.draft(this.kind, this.draft().withDescription(value)));
        this.addRenderableWidget(descriptionBox);

        this.addRenderableWidget(CycleButton.<BugReporter.Priority>builder(
                        value -> text("priority." + value.id()).withColor(value.color))
                .withValues(BugReporter.Priority.values())
                .withInitialValue(draft.priority())
                .create(x, this.top + 155, inner, 20, text("priority"),
                        (button, value) -> ReportStore.draft(this.kind, this.draft().withPriority(value))));

        this.addRenderableWidget(Button.builder(text("back"), button -> this.onClose())
                .bounds(x, this.top + 196, third, 20).build());
        this.addRenderableWidget(Button.builder(text("history", ReportStore.sent(this.kind).size()),
                        button -> this.minecraft.setScreen(new SentReportsScreen(this, this.kind)))
                .bounds(x + third + 6, this.top + 196, third, 20).build());
        this.send = this.addRenderableWidget(Button.builder(text("send"), button -> ReportStore.send(this.kind))
                .bounds(x + 2 * (third + 6), this.top + 196, inner - 2 * (third + 6), 20).build());
        this.setInitialFocus(nameBox);
        this.refreshSend();
    }

    private ReportStore.Draft draft() {
        return ReportStore.draft(this.kind);
    }

    @Override
    public void tick() {
        BugReporter.Result result = ReportStore.result(this.kind);
        if (result != this.shown) {
            this.shown = result;
            if (result != null && result.outcome() == BugReporter.Outcome.SENT) {
                this.rebuildWidgets();
            }
        }
        ReportStore.tick();
        this.refreshSend();
    }

    private void refreshSend() {
        if (this.send != null) {
            this.send.active = !ReportStore.sending(this.kind) && this.draft().ready();
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
        if (ReportStore.sending(this.kind)) {
            graphics.drawString(this.font, text("sending"), x, y, TEXT_COLOR);
            return;
        }
        BugReporter.Result result = ReportStore.result(this.kind);
        if (result == null) {
            Component as = text("as", BugReporter.username(), UpdateChecker.installed());
            graphics.drawString(this.font, as, x, y, MUTED_COLOR);
            return;
        }
        switch (result.outcome()) {
            case SENT -> graphics.drawString(this.font, text(this.kind.key("sent"), result.issue()), x, y, SENT_COLOR);
            case LIMITED -> graphics.drawString(this.font, text(this.kind.key("error.limited")), x, y, ERROR_COLOR);
            case REJECTED -> graphics.drawString(this.font, text("error.rejected"), x, y, ERROR_COLOR);
            case FAILED -> graphics.drawString(this.font, text("error.network"), x, y, ERROR_COLOR);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void removed() {
        ReportStore.closed(this.kind);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
