package nl.tivek.multiversepowers.update.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// The manager's first page: whether you are up to date (or which version is out, or how its download goes) with the
// buttons to update or check, and under them the notes of every version newer than yours, or of your own.
final class UpdateManagerScreen extends ManagerScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final int ERROR_COLOR = 0xFF7B7B;
    static final int GRAY = 0x8A8A8A;
    private static final int CARD = 40;
    private static final int SCROLL_STEP = 20;

    @Nullable
    private Release release;
    @Nullable
    private Button later;
    @Nullable
    private Button now;
    @Nullable
    private Button check;
    private List<ChangelogLayout.Block> notes = List.of();
    // What the notes were laid out from, so they are laid out again once the versions arrive.
    @Nullable
    private Object notesFrom;
    private int notesTop;
    private int notesHeight;
    private int notesContent;
    private double scroll;

    UpdateManagerScreen(@Nullable Screen root) {
        super(text("title"), root, UPDATES);
    }

    @Override
    protected Component subtitle() {
        return text("subtitle.updates");
    }

    @Override
    protected void initPage() {
        this.release = UpdateChecker.latest();
        this.later = null;
        this.now = null;
        this.check = null;
        UpdateChecker.loadReleases();
        int x = this.contentX;
        int half = (this.contentWidth - 4) / 2;
        int y = this.contentY + CARD + 6;
        if (this.release != null) {
            Release target = this.release;
            this.now = this.addRenderableWidget(Button.builder(text("now"), button -> UpdateInstaller.updateNow(target))
                    .tooltip(tip(text("now.tip"))).bounds(x, y, half, 20).build());
            this.later = this.addRenderableWidget(Button.builder(text("later"),
                    button -> UpdateInstaller.updateLater(target)).tooltip(tip(text("later.tip")))
                    .bounds(x + half + 4, y, this.contentWidth - half - 4, 20).build());
        } else {
            this.check = this.addRenderableWidget(Button.builder(text("check"), button -> UpdateChecker.checkNow())
                    .bounds(x, y, half, 20).build());
        }
        this.notesTop = y + 20 + 20;
        this.notesHeight = this.contentY + this.contentHeight - 14 - this.notesTop;
        this.notesFrom = null;
        this.layoutNotes();
        this.refreshButtons();
    }

    @Override
    public void tick() {
        if ((UpdateChecker.latest() == null) != (this.release == null)) {
            this.rebuildWidgets();
        }
        this.layoutNotes();
        this.refreshButtons();
    }

    // The newer versions' notes while you are behind, else your own version's.
    private void layoutNotes() {
        List<Release> all = UpdateChecker.releasesIfLoaded();
        List<Release> newer = UpdateChecker.newer();
        Object from = all == null ? (Object) UpdateChecker.releasesFailed() : List.of(all, newer);
        if (from.equals(this.notesFrom)) {
            return;
        }
        this.notesFrom = from;
        List<Release> shown = !newer.isEmpty() ? newer
                : UpdateChecker.installedRelease() == null ? List.of() : List.of(UpdateChecker.installedRelease());
        Component note = all == null ? text(UpdateChecker.releasesFailed() ? "versions.failed" : "versions.loading")
                : shown.isEmpty() ? text("changelog.not_listed", "v" + UpdateChecker.installed()) : null;
        String newest = all == null || all.isEmpty() ? "" : all.get(0).version();
        this.notes = ChangelogLayout.build(this.font, shown, UpdateChecker.installed(), newest, note,
                this.contentWidth - 14, false);
        this.notesContent = this.notes.stream().mapToInt(ChangelogLayout.Block::height).sum() + 8;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
    }

    private double maxScroll() {
        return Math.max(0, this.notesContent - this.notesHeight);
    }

    private void refreshButtons() {
        boolean busy = UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING;
        if (this.release != null) {
            boolean usable = this.release.jar() != null;
            boolean scheduled = UpdateInstaller.scheduled(this.release);
            this.later.active = usable && !busy && !scheduled;
            this.now.active = usable && !busy;
        }
        if (this.check != null) {
            this.check.active = !UpdateChecker.checking();
        }
    }

    @Override
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenAnchors.report("manager", this.contentX, this.contentY, this.contentWidth, this.contentHeight);
        Status status = this.status();
        int x = this.contentX;
        int y = this.contentY;
        card(graphics, x, y, this.contentWidth, CARD, status.color());
        ScreenAnchors.report("manager.status", x, y, this.contentWidth, CARD);
        if (status.icon() == null) {
            PixelIcons.spinner(graphics, x + 15, y + 14, status.color(), Util.getMillis());
        } else {
            PixelIcons.draw(graphics, status.icon(), x + 10, y + 8, 2, 0xFF000000 | status.color(), true);
        }
        int textX = x + 44;
        int room = this.contentWidth - 54;
        graphics.drawString(this.font, firstLine(this.font, status.head(), room), textX, y + 9,
                0xFF000000 | status.color(), true);
        graphics.drawString(this.font, firstLine(this.font, status.detail(), room), textX, y + 21, MUTED, false);
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            bar(graphics, textX, y + 32, room, UpdateInstaller.progress(), ACCENT);
        }
        int boxTop = this.notesTop - 4;
        card(graphics, x, boxTop, this.contentWidth, this.notesHeight + 8, 0);
        ScreenAnchors.report("manager.news", x, this.notesTop - 16, this.contentWidth, this.notesHeight + 20);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.contentX;
        int newer = UpdateChecker.newer().size();
        Component heading = newer > 0 ? text(newer == 1 ? "news.newer_one" : "news.newer_many", newer)
                : text("news.yours", "v" + UpdateChecker.installed());
        graphics.drawString(this.font, heading, x, this.notesTop - 15, BODY, false);
        Component all = text("news.all");
        this.link(graphics, all, x + this.contentWidth - this.font.width(all), this.notesTop - 15, mouseX, mouseY,
                0xFF000000 | ACCENT, () -> open(this.root, VERSIONS));
        graphics.enableScissor(x + 1, this.notesTop - 3, x + this.contentWidth - 1, this.notesTop + this.notesHeight + 3);
        int y = this.notesTop + 2 - (int) this.scroll;
        for (ChangelogLayout.Block block : this.notes) {
            if (y + block.height() >= this.notesTop - 4 && y <= this.notesTop + this.notesHeight) {
                block.draw(graphics, x + 7, y);
            }
            y += block.height();
        }
        graphics.flush();
        graphics.disableScissor();
        double max = this.maxScroll();
        if (max > 0) {
            int track = this.notesHeight;
            int thumb = Math.max(14, (int) (track * (double) track / (track + max)));
            int thumbY = this.notesTop + (int) ((track - thumb) * (this.scroll / max));
            graphics.fill(x + this.contentWidth - 4, this.notesTop, x + this.contentWidth - 2, this.notesTop + track,
                    0x30FFFFFF);
            graphics.fill(x + this.contentWidth - 4, thumbY, x + this.contentWidth - 2, thumbY + thumb, 0xA0FFFFFF);
        }
        int footY = this.contentY + this.contentHeight - 9;
        if (!UpdateInstaller.canInstall()) {
            graphics.drawString(this.font, firstLine(this.font, text("test"), this.contentWidth), x, footY,
                    0xFF000000 | WARN, false);
        } else {
            graphics.drawString(this.font, firstLine(this.font, text("auto", UpdatePopup.OPEN_KEY
                    .getTranslatedKeyMessage()), this.contentWidth), x, footY, DIM, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= this.notesTop - 4 && mouseY < this.notesTop + this.notesHeight + 4) {
            this.scroll = Mth.clamp(this.scroll - scrollY * SCROLL_STEP, 0.0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    static FormattedCharSequence firstLine(Font font, Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    // What the card says: a headline in the state's colour, a line under it, and its icon (null: a spinner).
    private record Status(Component head, Component detail, int color, @Nullable PixelIcons.Icon icon) {
    }

    private Status status() {
        Release target = UpdateInstaller.target();
        String aimed = target == null ? "?" : "v" + target.version();
        Component yours = text("detail.yours", "v" + UpdateChecker.installed());
        switch (UpdateInstaller.state()) {
            case DOWNLOADING -> {
                return new Status(text("versions.downloading", aimed,
                        Math.round(UpdateInstaller.progress() * 100.0F)), yours, ACCENT, null);
            }
            case READY -> {
                return new Status(text("versions.ready", aimed), yours, WARN, PixelIcons.Icon.CHECK);
            }
            case CHECKED -> {
                return new Status(text("head.checked", aimed), text("detail.test"), WARN, PixelIcons.Icon.CHECK);
            }
            case FAILED -> {
                Component error = UpdateInstaller.error();
                return new Status(error == null ? text("error.network") : error, yours, ERROR_COLOR,
                        PixelIcons.Icon.WARNING);
            }
            case IDLE -> {
            }
        }
        if (this.release != null) {
            return new Status(text("head.available", "v" + this.release.version()), this.released(), WARN,
                    PixelIcons.Icon.DOWNLOAD);
        }
        if (UpdateChecker.checking()) {
            return new Status(text("checking"), yours, GRAY, null);
        }
        if (UpdateChecker.lastFailed()) {
            return new Status(text("error.network"), yours, ERROR_COLOR, PixelIcons.Icon.WARNING);
        }
        if (UpdateChecker.lastCheck() == 0L) {
            return new Status(text("not_checked"), yours, GRAY, PixelIcons.Icon.DOWNLOAD);
        }
        return new Status(text("head.up_to_date"),
                text("detail.checked", "v" + UpdateChecker.installed(), ago(UpdateChecker.lastCheck())),
                ACCENT, PixelIcons.Icon.CHECK);
    }

    private Component released() {
        String size = this.release.jar() == null ? "?" : String.format(Locale.ENGLISH, "%.1f MB",
                this.release.jar().size() / 1_048_576.0);
        return text("released", DATE_TIME.format(this.release.published()), size);
    }

    private static Component ago(long time) {
        long minutes = (System.currentTimeMillis() - time) / 60_000L;
        if (minutes < 1) {
            return text("ago.now");
        }
        return minutes < 60 ? text("ago.minutes", minutes) : text("ago.hours", minutes / 60);
    }
}
