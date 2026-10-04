package nl.tivek.multiversepowers.bugreport.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.update.client.ManagerScreen;
import nl.tivek.multiversepowers.update.client.UpdateChecker;

// The feedback page's part of the bug reports and ideas you sent, newest first, each with how it stands on GitHub
// (looked up again every few minutes, or now with Refresh); a click opens it on GitHub.
final class SentReportsScreen extends ManagerScreen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final int ROW = 30;
    private static final int SCROLL_STEP = 15;
    private static final int BUG_COLOR = 0xFF9A7A;
    private static final int IDEA_COLOR = 0xF2C84B;

    private record Entry(BugReporter.Kind kind, ReportStore.Sent sent) {
    }

    private List<Entry> entries = List.of();
    private double scroll;
    private int listHeight;

    SentReportsScreen(@Nullable Screen root) {
        super(BugReportScreen.words("history.title"), root, FEEDBACK);
    }

    @Override
    protected List<Tab> tabs() {
        return this.feedbackTabs(REPORTS);
    }

    @Override
    protected Component subtitle() {
        return BugReportScreen.words("history.subtitle");
    }

    @Override
    protected void initPage() {
        this.listHeight = this.contentHeight - 26;
        this.addRenderableWidget(Button.builder(BugReportScreen.words("history.refresh"), button -> {
            ReportStore.refresh(BugReporter.Kind.BUG);
            ReportStore.refresh(BugReporter.Kind.IDEA);
        }).bounds(this.contentX + this.contentWidth - 80, this.contentY + this.contentHeight - 20, 80, 20).build());
        ReportStore.check(BugReporter.Kind.BUG);
        ReportStore.check(BugReporter.Kind.IDEA);
        this.gather();
    }

    @Override
    public void tick() {
        this.gather();
        ReportStore.tick();
    }

    private void gather() {
        List<Entry> all = new ArrayList<>();
        for (BugReporter.Kind kind : BugReporter.Kind.values()) {
            for (ReportStore.Sent sent : ReportStore.sent(kind)) {
                all.add(new Entry(kind, sent));
            }
        }
        all.sort(Comparator.comparingLong((Entry entry) -> entry.sent().time()).reversed());
        this.entries = all;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
    }

    private double maxScroll() {
        return Math.max(0, this.entries.size() * ROW - this.listHeight);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.contentX;
        int top = this.contentY;
        ScreenAnchors.report("reports", x, top, this.contentWidth, this.contentHeight);
        if (this.entries.isEmpty()) {
            PixelIcons.draw(graphics, PixelIcons.Icon.INBOX, x + 4, top + 6, 2, DIM, false);
            graphics.drawString(this.font, BugReportScreen.words("history.empty"), x + 36, top + 8, BODY, false);
            int linkX = x + 36;
            linkX += this.link(graphics, BugReportScreen.words("history.to_bug"), linkX, top + 20, mouseX, mouseY,
                    0xFF000000 | ACCENT, () -> open(this.root, BUG)) + 12;
            this.link(graphics, BugReportScreen.words("history.to_idea"), linkX, top + 20, mouseX, mouseY,
                    0xFF000000 | ACCENT, () -> open(this.root, IDEA));
        }
        graphics.enableScissor(x, top, x + this.contentWidth, top + this.listHeight);
        int hover = this.rowAt(mouseX, mouseY);
        for (int i = 0; i < this.entries.size(); i++) {
            int y = top + i * ROW - (int) this.scroll;
            if (y + ROW >= top && y <= top + this.listHeight) {
                this.drawEntry(graphics, this.entries.get(i), x, y, i == hover);
            }
        }
        graphics.disableScissor();
        if (hover >= 0) {
            this.setTooltipForNextRenderPass(BugReportScreen.words("history.open"));
        }
        this.drawStatus(graphics, x, top + this.contentHeight - 14);
    }

    private void drawEntry(GuiGraphics graphics, Entry entry, int x, int y, boolean hover) {
        ReportStore.Sent sent = entry.sent();
        int width = this.contentWidth;
        card(graphics, x, y, width, ROW - 3, 0);
        if (hover) {
            graphics.fill(x, y, x + width, y + ROW - 3, 0x1CFFFFFF);
        }
        boolean bug = entry.kind() == BugReporter.Kind.BUG;
        PixelIcons.draw(graphics, bug ? PixelIcons.Icon.BUG : PixelIcons.Icon.IDEA, x + 6, y + 7, 1,
                0xFF000000 | (bug ? BUG_COLOR : IDEA_COLOR), true);
        Component state = BugReportScreen.words(stateKey(entry.kind(), sent.state()));
        int chip = this.font.width(state) + 6;
        this.chip(graphics, state, x + width - 6 - chip, y + 4, color(sent.state()), 0x101010);
        Component number = Component.literal("#" + sent.issue() + "  ").withColor(0x8E8E8E);
        graphics.drawString(this.font, this.fit(Component.empty().append(number).append(sent.name()),
                width - 34 - chip - 10), x + 24, y + 4, TEXT, false);
        Component line = BugReportScreen.words("history.line", DATE.format(Instant.ofEpochMilli(sent.time())),
                BugReportScreen.words("priority." + sent.priority().id()).withColor(sent.priority().color));
        if (!sent.title().isEmpty() && !sent.title().equals(sent.name())) {
            line = Component.empty().append(line).append(BugReportScreen.words("history.github", sent.title()));
        }
        graphics.drawString(this.font, this.fit(line, width - 34), x + 24, y + 15, MUTED, false);
    }

    private static String stateKey(BugReporter.Kind kind, BugReporter.State state) {
        return state == BugReporter.State.DONE ? kind.key("status.done") : "status." + state.id();
    }

    private static int color(BugReporter.State state) {
        return switch (state) {
            case OPEN -> WARN;
            case DONE -> BugReportScreen.SENT_COLOR;
            case DECLINED -> BugReportScreen.ERROR_COLOR;
            case DUPLICATE, CLOSED, GONE -> 0xA0A0A0;
        };
    }

    private void drawStatus(GuiGraphics graphics, int x, int y) {
        boolean checking = ReportStore.checking(BugReporter.Kind.BUG) || ReportStore.checking(BugReporter.Kind.IDEA);
        boolean offline = ReportStore.checkFailed(BugReporter.Kind.BUG) || ReportStore.checkFailed(BugReporter.Kind.IDEA);
        Component text = checking ? BugReportScreen.words("history.checking")
                : offline ? BugReportScreen.words("history.offline")
                : BugReportScreen.words("history.kept", ReportStore.KEPT);
        int color = checking ? TEXT : offline ? 0xFF000000 | BugReportScreen.ERROR_COLOR : DIM;
        graphics.drawString(this.font, this.fit(text, this.contentWidth - 90), x, y, color, false);
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < this.contentX || mouseX >= this.contentX + this.contentWidth || mouseY < this.contentY
                || mouseY >= this.contentY + this.listHeight) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - this.contentY + this.scroll) / ROW);
        double inRow = mouseY - this.contentY + this.scroll - index * ROW;
        return index >= 0 && index < this.entries.size() && inRow < ROW - 3 ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.rowAt(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            this.click(1.0F);
            ConfirmLinkScreen.confirmLinkNow(this, "https://github.com/" + UpdateChecker.REPO + "/issues/"
                    + this.entries.get(index).sent().issue());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Mth.clamp(this.scroll - scrollY * SCROLL_STEP, 0.0, this.maxScroll());
        return true;
    }
}
