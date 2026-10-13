package nl.tivek.multiversepowers.bugreport.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.bugreport.client.form.Field;
import nl.tivek.multiversepowers.bugreport.client.form.Form;
import nl.tivek.multiversepowers.bugreport.client.form.FormView;
import nl.tivek.multiversepowers.bugreport.client.form.Values;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.faction.client.pick.MobPickerScreen;
import nl.tivek.multiversepowers.update.client.ManagerScreen;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import org.lwjgl.glfw.GLFW;

// The feedback page's part for a bug report or an idea: a form in folding sections (an idea's by its category), kept
// as a draft until submitted. Check & submit shows the report as it will be sent first, and Submit there sends it;
// what is still missing is said under the form, a click on it going there. Ctrl+Enter does either; Clear asks a second
// click.
public final class BugReportScreen extends ManagerScreen {
    static final int SENT_COLOR = 0x6EE7A0;
    static final int ERROR_COLOR = 0xFF7B7B;
    private static final long CLEAR_MS = 2500L;
    private static final long SAVING_MS = 1000L;
    private static final long SAVED_MS = 3000L;

    private final BugReporter.Kind kind;
    @Nullable
    private BugReporter.Result shown;
    @Nullable
    private Button send;
    @Nullable
    private Button clear;
    @Nullable
    private Button closeAll;
    @Nullable
    private Button openAll;
    @Nullable
    private FormView view;
    @Nullable
    private ReportPreview preview;
    private Map<String, String> values = new LinkedHashMap<>();
    private Form form = FeedbackForms.BUG;
    private boolean previewing;
    @Nullable
    private Component sendTip;
    private long editedAt;
    private long clearAskedAt = -CLEAR_MS;
    private int statusY;

    private BugReportScreen(@Nullable Screen root, BugReporter.Kind kind) {
        super(words(kind.key("title")), root, FEEDBACK);
        this.kind = kind;
        this.shown = ReportStore.result(kind);
    }

    @Override
    protected List<Tab> tabs() {
        return this.feedbackTabs(this.kind == BugReporter.Kind.BUG ? BUG : IDEA);
    }

    public static BugReportScreen bug(@Nullable Screen root) {
        return new BugReportScreen(root, BugReporter.Kind.BUG);
    }

    public static BugReportScreen idea(@Nullable Screen root) {
        return new BugReportScreen(root, BugReporter.Kind.IDEA);
    }

    // Whether the idea part (or else the bug part) is open now: where the tour's steps on them wait.
    public static boolean showing(boolean idea) {
        return Minecraft.getInstance().screen instanceof BugReportScreen screen
                && screen.kind == (idea ? BugReporter.Kind.IDEA : BugReporter.Kind.BUG);
    }

    public static Screen reports(@Nullable Screen root) {
        return new SentReportsScreen(root);
    }

    public static int sentCount() {
        return ReportStore.sent(BugReporter.Kind.BUG).size() + ReportStore.sent(BugReporter.Kind.IDEA).size();
    }

    static MutableComponent words(String key, Object... args) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".bugreport." + key, args);
    }

    @Override
    protected void initPage() {
        int x = this.contentX;
        int width = this.contentWidth;
        ReportStore.Draft draft = ReportStore.draft(this.kind);
        this.values = new LinkedHashMap<>(draft.values());
        this.form = this.kind == BugReporter.Kind.BUG ? FeedbackForms.BUG : FeedbackForms.idea(draft.category());
        int buttonsY = this.contentY + this.contentHeight - 20;
        this.statusY = buttonsY - 13;
        int boxHeight = this.statusY - 5 - this.contentY;
        this.view = null;
        this.preview = null;
        this.sendTip = null;
        if (this.previewing) {
            this.preview = new ReportPreview(this.font, this.submission(), x, this.contentY, width, boxHeight);
            this.addRenderableWidget(Button.builder(words("preview.back"), button -> this.preview(false))
                    .tooltip(tip(words("preview.back.tip"))).bounds(x, buttonsY, 76, 20).build());
            this.send = this.addRenderableWidget(Button.builder(words("send"), button -> this.submit())
                    .tooltip(tip(words("send.tip"))).bounds(x + width - 104, buttonsY, 104, 20).build());
            this.clear = null;
            this.closeAll = null;
            this.openAll = null;
            this.refreshButtons();
            return;
        }
        this.view = new FormView(this.form, this.values, this.font, new Host(), x, this.contentY, width, boxHeight);
        this.clear = this.addRenderableWidget(Button.builder(words("clear"), button -> this.clear())
                .tooltip(tip(words("clear.tip"))).bounds(x, buttonsY, 60, 20).build());
        this.closeAll = this.addRenderableWidget(Button.builder(words("close_all"), button -> this.foldAll(true))
                .tooltip(tip(words("close_all.tip"))).bounds(x + 64, buttonsY, 60, 20).build());
        this.openAll = this.addRenderableWidget(Button.builder(words("open_all"), button -> this.foldAll(false))
                .tooltip(tip(words("open_all.tip"))).bounds(x + 128, buttonsY, 60, 20).build());
        this.send = this.addRenderableWidget(Button.builder(words("check"), button -> this.preview(true))
                .bounds(x + width - 104, buttonsY, 104, 20).build());
        this.refreshButtons();
    }

    private final class Host implements FormView.Host {
        @Override
        public void add(AbstractWidget widget) {
            BugReportScreen.this.addWidget(widget);
        }

        @Nullable
        @Override
        public GuiEventListener focused() {
            return BugReportScreen.this.getFocused();
        }

        @Override
        public void focus(@Nullable GuiEventListener widget) {
            BugReportScreen.this.setFocused(widget);
        }

        @Override
        public void tooltip(List<FormattedCharSequence> lines) {
            BugReportScreen.this.setTooltipForNextRenderPass(lines);
        }

        @Override
        public void changed() {
            BugReportScreen.this.changed();
        }

        @Override
        public void pickMobs(Field field, List<String> picked, Consumer<List<String>> done) {
            BugReportScreen.this.minecraft.setScreen(new MobPickerScreen(BugReportScreen.this, field.label(), picked,
                    ACCENT, done));
        }
    }

    private Values answers() {
        return this.view != null ? this.view.values() : new Values(this.form, this.values);
    }

    private String category() {
        return this.kind == BugReporter.Kind.IDEA ? this.values.getOrDefault(FeedbackForms.CATEGORY, "") : "";
    }

    private BugReporter.Submission submission() {
        Values answers = this.answers();
        String category = this.category();
        Field field = this.form.field(FeedbackForms.CATEGORY);
        String categoryName = category.isEmpty() || field == null ? ""
                : field.option(category).getString();
        return new BugReporter.Submission(this.form.title(answers),
                BugReporter.Priority.of(this.form.priority(answers)), category, categoryName,
                this.form.blocks(answers));
    }

    // Keeps the draft, and on a new category opens its form with the category folded away.
    private void changed() {
        String before = ReportStore.draft(this.kind).category();
        ReportStore.draft(this.kind, new ReportStore.Draft(this.category(), this.values));
        this.editedAt = Util.getMillis();
        this.clearAskedAt = -CLEAR_MS;
        if (!this.category().equals(before)) {
            Form next = FeedbackForms.idea(this.category());
            FormView.fold(next, "idea.category", next != FeedbackForms.PICK);
            this.rebuildWidgets();
        }
    }

    private void foldAll(boolean folded) {
        if (this.view != null) {
            this.view.foldAll(folded);
        }
    }

    private void preview(boolean open) {
        this.previewing = open;
        this.rebuildWidgets();
    }

    private void submit() {
        if (this.send != null && this.send.active) {
            ReportStore.send(this.kind, this.submission(), this.form);
        }
    }

    // The first click asks, the second within a few seconds empties this form's answers (an idea keeps its category).
    private void clear() {
        long now = Util.getMillis();
        if (now - this.clearAskedAt > CLEAR_MS) {
            this.clearAskedAt = now;
            return;
        }
        this.clearAskedAt = -CLEAR_MS;
        this.values.keySet().removeIf(key -> this.form.owns(key) && !key.equals(FeedbackForms.CATEGORY));
        ReportStore.draft(this.kind, new ReportStore.Draft(this.category(), this.values));
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        BugReporter.Result result = ReportStore.result(this.kind);
        if (result != this.shown) {
            this.shown = result;
            if (result != null && result.outcome() == BugReporter.Outcome.SENT) {
                this.previewing = false;
                this.rebuildWidgets();
            }
        }
        ReportStore.tick();
        this.refreshButtons();
    }

    private void refreshButtons() {
        if (this.send != null) {
            Component why = this.why();
            this.send.active = why == null;
            Component tip = why != null ? why : words(this.previewing ? "send.tip" : "check.tip");
            if (!tip.equals(this.sendTip)) {
                this.sendTip = tip;
                this.send.setTooltip(Tooltip.create(tip));
            }
        }
        if (this.clear != null) {
            boolean asking = Util.getMillis() - this.clearAskedAt <= CLEAR_MS;
            this.clear.setMessage(words(asking ? "clear.sure" : "clear"));
            this.clear.active = this.values.entrySet().stream().anyMatch(entry -> this.form.owns(entry.getKey())
                    && !entry.getKey().equals(FeedbackForms.CATEGORY) && !entry.getValue().isBlank());
        }
        if (this.closeAll != null && this.openAll != null && this.view != null) {
            this.closeAll.active = !this.view.allFolded();
            this.openAll.active = !this.view.allOpen();
        }
    }

    // Why the report cannot go yet, or null when it can.
    @Nullable
    private Component why() {
        if (ReportStore.sending(this.kind)) {
            return words("sending");
        }
        if (this.form == FeedbackForms.PICK) {
            return words("send.pick");
        }
        List<Field> missing = this.form.missing(this.answers());
        if (!missing.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < Math.min(4, missing.size()); i++) {
                names.append(i == 0 ? "" : ", ").append(missing.get(i).label().getString());
            }
            if (missing.size() > 4) {
                names.append(", ...");
            }
            return words("send.missing", names.toString());
        }
        return ReportStore.duplicate(this.kind, this.submission()) ? words("send.duplicate") : null;
    }

    @Override
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenAnchors.report("report.form", this.contentX, this.contentY, this.contentWidth, this.contentHeight);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.preview != null) {
            this.preview.render(graphics);
        } else if (this.view != null) {
            this.view.render(graphics, mouseX, mouseY, partialTick);
        }
        int right = this.contentX + this.contentWidth;
        long since = Util.getMillis() - this.editedAt;
        int room = this.contentWidth;
        if (this.editedAt > 0 && since < SAVING_MS + SAVED_MS) {
            Component saved = words(since < SAVING_MS ? "draft.saving" : "draft.saved");
            graphics.drawString(this.font, saved, right - this.font.width(saved), this.statusY, DIM, false);
            room -= this.font.width(saved) + 8;
        }
        this.drawStatus(graphics, mouseX, mouseY, this.contentX, this.statusY, room);
    }

    private void drawStatus(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int room) {
        if (ReportStore.sending(this.kind)) {
            PixelIcons.spinner(graphics, x, y - 1, 0xFFFFFF, Util.getMillis());
            graphics.drawString(this.font, words("sending"), x + 15, y, TEXT, false);
            return;
        }
        BugReporter.Result result = ReportStore.result(this.kind);
        if (result != null) {
            switch (result.outcome()) {
                case SENT -> {
                    Component sent = words(this.kind.key("sent"), result.issue());
                    graphics.drawString(this.font, sent, x, y, 0xFF000000 | SENT_COLOR, false);
                    this.link(graphics, words("see_reports"), x + this.font.width(sent) + 8, y, mouseX, mouseY,
                            0xFF000000 | ACCENT, () -> open(this.root, REPORTS));
                }
                case LIMITED -> this.error(graphics, words(this.kind.key("error.limited")), x, y, room);
                case REJECTED -> this.error(graphics, words("error.rejected"), x, y, room);
                case FAILED -> this.error(graphics, words("error.network"), x, y, room);
            }
            return;
        }
        List<Field> missing = this.form.missing(this.answers());
        if (this.previewing) {
            graphics.drawString(this.font, this.fit(words("preview.status"), room), x, y, MUTED, false);
        } else if (this.form == FeedbackForms.PICK) {
            graphics.drawString(this.font, this.fit(words("status.pick"), room), x, y, 0xFF000000 | WARN, false);
        } else if (!missing.isEmpty() && this.view != null) {
            Component left = words("status.missing", missing.size());
            graphics.drawString(this.font, left, x, y, 0xFF000000 | ERROR_COLOR, false);
            FormView view = this.view;
            this.link(graphics, words("status.show"), x + this.font.width(left) + 6, y, mouseX, mouseY,
                    0xFF000000 | ACCENT, () -> view.reveal(missing.get(0)));
        } else {
            Component as = words("as", BugReporter.username(), UpdateChecker.installed());
            graphics.drawString(this.font, this.fit(as, room), x, y, MUTED, false);
        }
    }

    private void error(GuiGraphics graphics, Component text, int x, int y, int room) {
        graphics.drawString(this.font, this.fit(text, room), x, y, 0xFF000000 | ERROR_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.view != null) {
            if (this.view.mouseClicked(mouseX, mouseY, button)) {
                this.click(1.0F);
                return true;
            }
            if (!this.view.inside(mouseX, mouseY)) {
                this.view.hideWidgets();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.preview != null && this.preview.mouseScrolled(mouseX, mouseY, scrollY)
                || this.view != null && this.view.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasControlDown()) {
            if (this.send != null && this.send.active) {
                this.click(1.0F);
                if (this.previewing) {
                    this.submit();
                } else {
                    this.preview(true);
                }
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.previewing) {
            this.preview(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        ReportStore.closed(this.kind);
    }
}
