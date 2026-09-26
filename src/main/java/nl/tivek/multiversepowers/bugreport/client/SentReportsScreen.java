package nl.tivek.multiversepowers.bugreport.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

final class SentReportsScreen extends DirtBackgroundScreen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final String ELLIPSIS = "...";

    private final Screen parent;
    private final BugReporter.Kind kind;
    private int left;
    private int top;
    private int panelWidth;

    SentReportsScreen(Screen parent, BugReporter.Kind kind) {
        super(BugReportScreen.text(kind.key("history.title")));
        this.parent = parent;
        this.kind = kind;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(BugReportScreen.PANEL_WIDTH, this.width - 20);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(6, (this.height - BugReportScreen.PANEL_HEIGHT) / 2);
        this.addRenderableWidget(Button.builder(BugReportScreen.text("back"), button -> this.onClose())
                .bounds(this.left + 10, this.top + 196, this.panelWidth - 20, 20).build());
        ReportStore.check(this.kind);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.left, this.top, this.panelWidth, BugReportScreen.PANEL_HEIGHT, PANEL_BORDER);
        int x = this.left + 10;
        int right = this.left + this.panelWidth - 10;
        graphics.drawString(this.font, this.title, x, this.top + 10, TEXT_COLOR);
        drawDivider(graphics, x, this.top + 23, right - x);
        List<ReportStore.Sent> sent = ReportStore.sent(this.kind);
        int y = this.top + 30;
        if (sent.isEmpty()) {
            graphics.drawString(this.font, BugReportScreen.text(this.kind.key("history.empty")), x, y, MUTED_COLOR);
        }
        for (int i = 0; i < sent.size(); i++) {
            if (i > 0) {
                drawDivider(graphics, x, y + 2, right - x);
                y += 6;
            }
            y = this.drawEntry(graphics, sent.get(i), x, right, y);
        }
        this.drawStatus(graphics, x, this.top + 181);
    }

    private int drawEntry(GuiGraphics graphics, ReportStore.Sent entry, int x, int right, int y) {
        Component priority = BugReportScreen.text("priority." + entry.priority().id());
        int priorityWidth = this.font.width(priority);
        graphics.drawString(this.font, this.fit(entry.name(), right - x - priorityWidth - 8), x, y, TEXT_COLOR);
        graphics.drawString(this.font, priority, right - priorityWidth, y, entry.priority().color);
        y += this.lineHeight();
        Component state = BugReportScreen.text(this.stateKey(entry.state())).withColor(color(entry.state()));
        graphics.drawString(this.font, BugReportScreen.text("history.line", entry.issue(),
                DATE.format(Instant.ofEpochMilli(entry.time())), state), x, y, MUTED_COLOR);
        y += this.lineHeight();
        if (!entry.title().isEmpty() && !entry.title().equals(entry.name())) {
            y = this.drawWrappedLeft(graphics, BugReportScreen.text("history.github", entry.title()), x, y, right - x,
                    2, this.top + 178, MUTED_COLOR);
        }
        return y;
    }

    private String stateKey(BugReporter.State state) {
        return state == BugReporter.State.DONE ? this.kind.key("status.done") : "status." + state.id();
    }

    private static int color(BugReporter.State state) {
        return switch (state) {
            case OPEN -> NOTICE_COLOR;
            case DONE -> BugReportScreen.SENT_COLOR;
            case DECLINED -> BugReportScreen.ERROR_COLOR;
            case DUPLICATE, CLOSED, GONE -> TEXT_COLOR;
        };
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width(ELLIPSIS))) + ELLIPSIS;
    }

    private void drawStatus(GuiGraphics graphics, int x, int y) {
        if (ReportStore.checking(this.kind)) {
            graphics.drawString(this.font, BugReportScreen.text("history.checking"), x, y, TEXT_COLOR);
        } else if (ReportStore.checkFailed(this.kind)) {
            graphics.drawString(this.font, BugReportScreen.text("history.offline"), x, y, BugReportScreen.ERROR_COLOR);
        } else {
            graphics.drawString(this.font, BugReportScreen.text("history.kept", ReportStore.KEPT), x, y, MUTED_COLOR);
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
