package nl.tivek.multiversepowers.bugreport.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.update.client.ManagerScreen;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import org.lwjgl.glfw.GLFW;

// The feedback page's part for a bug report or an idea: a name, a description and a priority, kept as a draft until
// submitted, with what goes along (your name and version) said above Submit. Ctrl+Enter submits; Clear asks a second
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
    private EditBox name;
    @Nullable
    private MultiLineEditBox description;
    private long editedAt;
    private long clearAskedAt = -CLEAR_MS;
    private int descriptionY;
    private int priorityY;
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
        ReportStore.Draft draft = this.draft();
        int bottom = this.contentY + this.contentHeight;
        int buttonsY = bottom - 20;
        this.statusY = buttonsY - 13;
        this.priorityY = this.statusY - 23;

        this.name = new EditBox(this.font, x, this.contentY + 11, width, 20, words("name"));
        this.name.setMaxLength(BugReporter.TITLE_MAX);
        this.name.setHint(words(this.kind.key("name.hint")));
        this.name.setValue(draft.name());
        this.name.setResponder(value -> this.edited(this.draft().withName(value)));
        this.addRenderableWidget(this.name);

        this.descriptionY = this.contentY + 11 + 20 + 17;
        this.description = new MultiLineEditBox(this.font, x, this.descriptionY, width,
                Math.max(28, this.priorityY - 6 - this.descriptionY), words(this.kind.key("description.hint")),
                words("description"));
        // The box's own limit would draw a second counter under it, over the priority: the limit is kept here instead.
        this.description.setValue(cap(draft.description()));
        this.description.setValueListener(value -> {
            if (value.length() > BugReporter.DESCRIPTION_MAX) {
                this.description.setValue(cap(value));
                return;
            }
            this.edited(this.draft().withDescription(value));
        });
        this.addRenderableWidget(this.description);

        int label = this.font.width(words("priority")) + 8;
        this.addRenderableWidget(new PriorityPicker(this.font, x + label, this.priorityY, width - label, 18,
                draft.priority(), priority -> this.edited(this.draft().withPriority(priority))));

        this.clear = this.addRenderableWidget(Button.builder(words("clear"), button -> this.clear())
                .bounds(x, buttonsY, 76, 20).build());
        this.send = this.addRenderableWidget(Button.builder(words("send"), button -> ReportStore.send(this.kind))
                .tooltip(tip(words("send.tip"))).bounds(x + width - 104, buttonsY, 104, 20).build());
        this.setInitialFocus(this.name);
        this.refreshButtons();
    }

    private ReportStore.Draft draft() {
        return ReportStore.draft(this.kind);
    }

    private static String cap(String text) {
        return text.length() <= BugReporter.DESCRIPTION_MAX ? text : text.substring(0, BugReporter.DESCRIPTION_MAX);
    }

    private void edited(ReportStore.Draft draft) {
        if (!draft.equals(this.draft())) {
            ReportStore.draft(this.kind, draft);
            this.editedAt = Util.getMillis();
        }
    }

    // The first click asks, the second within a few seconds empties the name and description.
    private void clear() {
        long now = Util.getMillis();
        if (now - this.clearAskedAt > CLEAR_MS) {
            this.clearAskedAt = now;
            return;
        }
        this.clearAskedAt = -CLEAR_MS;
        this.name.setValue("");
        this.description.setValue("");
        this.setFocused(this.name);
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
        this.refreshButtons();
    }

    private void refreshButtons() {
        if (this.send != null) {
            this.send.active = !ReportStore.sending(this.kind) && this.draft().ready();
        }
        if (this.clear != null) {
            boolean asking = Util.getMillis() - this.clearAskedAt <= CLEAR_MS;
            this.clear.setMessage(words(asking ? "clear.sure" : "clear"));
            this.clear.active = !this.draft().name().isEmpty() || !this.draft().description().isEmpty();
        }
    }

    @Override
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.contentX;
        graphics.drawString(this.font, words("name"), x, this.contentY, BODY, false);
        graphics.drawString(this.font, words("description"), x, this.descriptionY - 11, BODY, false);
        graphics.drawString(this.font, words("priority"), x, this.priorityY + 5, BODY, false);
        ScreenAnchors.report("report.form", x, this.contentY, this.contentWidth, this.contentHeight);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.contentX;
        int right = x + this.contentWidth;
        int length = this.description == null ? 0 : this.description.getValue().length();
        Component count = Component.literal(length + " / " + BugReporter.DESCRIPTION_MAX);
        int countColor = length > BugReporter.DESCRIPTION_MAX * 9 / 10 ? 0xFF000000 | WARN : DIM;
        graphics.drawString(this.font, count, right - this.font.width(count), this.descriptionY - 11, countColor, false);
        long since = Util.getMillis() - this.editedAt;
        if (this.editedAt > 0 && since < SAVING_MS + SAVED_MS) {
            Component saved = words(since < SAVING_MS ? "draft.saving" : "draft.saved");
            graphics.drawString(this.font, saved, right - this.font.width(saved), this.contentY, DIM, false);
        }
        this.drawStatus(graphics, mouseX, mouseY, x, this.statusY);
    }

    private void drawStatus(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        if (ReportStore.sending(this.kind)) {
            PixelIcons.spinner(graphics, x, y - 1, 0xFFFFFF, Util.getMillis());
            graphics.drawString(this.font, words("sending"), x + 15, y, TEXT, false);
            return;
        }
        BugReporter.Result result = ReportStore.result(this.kind);
        if (result == null) {
            Component as = words("as", BugReporter.username(), UpdateChecker.installed());
            graphics.drawString(this.font, this.fit(as, this.contentWidth), x, y, MUTED, false);
            return;
        }
        switch (result.outcome()) {
            case SENT -> {
                Component sent = words(this.kind.key("sent"), result.issue());
                graphics.drawString(this.font, sent, x, y, 0xFF000000 | SENT_COLOR, false);
                this.link(graphics, words("see_reports"), x + this.font.width(sent) + 8, y, mouseX, mouseY,
                        0xFF000000 | ACCENT, () -> open(this.root, REPORTS));
            }
            case LIMITED -> this.error(graphics, words(this.kind.key("error.limited")), x, y);
            case REJECTED -> this.error(graphics, words("error.rejected"), x, y);
            case FAILED -> this.error(graphics, words("error.network"), x, y);
        }
    }

    private void error(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(this.font, this.fit(text, this.contentWidth), x, y, 0xFF000000 | ERROR_COLOR, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasControlDown()) {
            if (this.send != null && this.send.active) {
                this.click(1.0F);
                ReportStore.send(this.kind);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        ReportStore.closed(this.kind);
    }
}
