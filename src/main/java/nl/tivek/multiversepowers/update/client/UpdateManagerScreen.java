package nl.tivek.multiversepowers.update.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.lwjgl.glfw.GLFW;

// The manager's updates page: on top whether you are up to date (or which version is out, or how its download goes);
// under it the last ten versions as the game's launcher lists them, beside them the chosen one's notes (a newer one's
// with those of every version between it and yours); under those a button that switches to the chosen version and one
// that updates once you quit, or else looks for updates. A switch downloads that version's jar, closes the game and
// swaps it in (UpdateInstaller); the player starts the game again.
final class UpdateManagerScreen extends ManagerScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final int ERROR_COLOR = 0xFF7B7B;
    static final int GRAY = 0x8A8A8A;
    private static final int CARD = 32;
    private static final int SHOWN = 10;
    private static final int GAP = 5;
    private static final int PADDING = 7;
    private static final int ROW = 25;
    private static final int SCROLLBAR = 6;
    private static final int SCROLL_STEP = 20;

    @Nullable
    private Release release;
    private List<Release> versions = List.of();
    @Nullable
    private Release chosen;
    private List<ChangelogLayout.Block> notes = List.of();
    private boolean complete;
    private int top;
    private int bottom;
    private int listWidth;
    private int notesX;
    private int notesWidth;
    private int notesContent;
    private double scroll;
    private double listScroll;
    @Nullable
    private Button use;
    @Nullable
    private Button second;
    @Nullable
    private Button retry;
    // Whether the second button updates once you quit (else it looks for updates), so its tooltip is set once.
    @Nullable
    private Boolean secondLater;

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
        UpdateChecker.loadReleases();
        int x = this.contentX;
        int buttonsY = this.contentY + this.contentHeight - 20;
        this.top = this.contentY + CARD + GAP;
        this.bottom = buttonsY - GAP - (UpdateInstaller.canInstall() ? 0 : 12);
        // Room for a two-digit day's date and the widest chip on one line.
        this.listWidth = Mth.clamp(this.contentWidth * 3 / 8, 126, 150);
        this.notesX = x + this.listWidth + GAP;
        this.notesWidth = x + this.contentWidth - this.notesX;
        int half = (this.contentWidth - 4) / 2;
        this.use = this.addRenderableWidget(Button.builder(text("versions.use"), button -> this.switchTo())
                .tooltip(tip(text("versions.tip"))).bounds(x, buttonsY, half, 20).build());
        this.second = this.addRenderableWidget(Button.builder(text("check"), button -> this.second())
                .bounds(x + half + 4, buttonsY, this.contentWidth - half - 4, 20).build());
        this.secondLater = null;
        this.retry = this.addRenderableWidget(Button.builder(text("versions.retry"), button -> {
            this.complete = false;
            UpdateChecker.reloadReleases();
        }).bounds(x + 6, this.top + 38, this.listWidth - 12, 20).build());
        this.layout();
        this.refreshButtons();
    }

    @Override
    public void tick() {
        Release latest = UpdateChecker.latest();
        if (latest != this.release) {
            // A version just came out: it is the one shown.
            this.release = latest;
            this.chosen = null;
            this.layout();
        } else if (!this.complete && (UpdateChecker.releasesIfLoaded() != null || UpdateChecker.releasesFailed())) {
            this.layout();
        }
        this.refreshButtons();
    }

    // The ten newest versions, and yours under them when it is older; the chosen one's notes, a newer one's with those
    // of every version between it and yours, so they say all that switching to it brings.
    private void layout() {
        List<Release> all = UpdateChecker.releasesIfLoaded();
        this.complete = all != null || UpdateChecker.releasesFailed();
        Component note = null;
        List<Release> shown = List.of();
        if (all == null) {
            this.versions = List.of();
            this.chosen = null;
        } else {
            List<Release> listed = new ArrayList<>(all.subList(0, Math.min(SHOWN, all.size())));
            Release own = UpdateChecker.installedRelease();
            if (own != null && !listed.contains(own)) {
                listed.add(own);
            }
            this.versions = listed;
            if (this.chosen == null || !listed.contains(this.chosen)) {
                this.chosen = listed.isEmpty() ? null : listed.get(0);
            }
            if (own == null) {
                note = text("changelog.not_listed", "v" + UpdateChecker.installed());
            }
            Release pick = this.chosen;
            if (pick != null) {
                shown = !newer(pick) ? List.of(pick) : all.stream()
                        .filter(release -> newer(release) && Release.compare(release.version(), pick.version()) <= 0)
                        .toList();
            }
        }
        Release newest = newest(false);
        this.notes = ChangelogLayout.build(this.font, shown, UpdateChecker.installed(),
                newest == null ? "" : newest.version(), note, this.notesWidth - 2 * PADDING - SCROLLBAR, false);
        this.notesContent = this.notes.stream().mapToInt(ChangelogLayout.Block::height).sum() + 2 * PADDING;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    // The latest release (`installable`: the latest that has a jar to install).
    @Nullable
    private static Release newest(boolean installable) {
        List<Release> all = UpdateChecker.releasesIfLoaded();
        if (all == null) {
            return null;
        }
        return all.stream().filter(release -> !installable || release.jar() != null).findFirst().orElse(null);
    }

    private static boolean installed(Release release) {
        return Release.compare(release.version(), UpdateChecker.installed()) == 0;
    }

    private static boolean newer(Release release) {
        return Release.compare(release.version(), UpdateChecker.installed()) > 0;
    }

    private void choose(Release release) {
        if (release == this.chosen) {
            return;
        }
        this.chosen = release;
        this.scroll = 0.0;
        this.layout();
        this.refreshButtons();
    }

    private void switchTo() {
        Release pick = this.chosen;
        if (pick != null && !installed(pick)) {
            UpdateInstaller.updateNow(pick);
        }
    }

    // A newer chosen version is put in once the game closes; otherwise this looks for updates.
    private void second() {
        Release pick = this.chosen;
        if (pick != null && newer(pick)) {
            UpdateInstaller.updateLater(pick);
        } else {
            UpdateChecker.checkNow();
        }
    }

    private double maxScroll() {
        return Math.max(0, this.notesContent - (this.bottom - this.top));
    }

    private double maxListScroll() {
        return Math.max(0, this.versions.size() * ROW + 4 - (this.bottom - this.top));
    }

    private void refreshButtons() {
        Release pick = this.chosen;
        boolean own = pick != null && installed(pick);
        boolean ahead = pick != null && newer(pick);
        this.use.active = pick != null && !own && UpdateInstaller.canSwitch(pick);
        this.use.setMessage(pick == null ? text("versions.use") : own ? text("versions.installed")
                : this.quits(text(ahead ? "versions.update_to" : "versions.use_this", "v" + pick.version()),
                        this.use.getWidth()));
        if (!Boolean.valueOf(ahead).equals(this.secondLater)) {
            this.secondLater = ahead;
            this.second.setTooltip(tip(ahead ? text("later.tip") : text("auto",
                    UpdatePopup.OPEN_KEY.getTranslatedKeyMessage())));
        }
        if (ahead) {
            boolean scheduled = UpdateInstaller.scheduled(pick);
            this.second.active = !scheduled && UpdateInstaller.canSwitch(pick);
            this.second.setMessage(text(scheduled ? "versions.on_quit" : "later"));
        } else {
            this.second.active = !UpdateChecker.checking();
            this.second.setMessage(text(UpdateChecker.checking() ? "checking" : "check"));
        }
        this.retry.visible = UpdateChecker.releasesIfLoaded() == null && UpdateChecker.releasesFailed();
    }

    // A switch's label says the game quits when the button has room for it.
    private Component quits(Component label, int width) {
        Component full = Component.empty().append(label).append(text("quits"));
        return this.font.width(full) <= width - 8 ? full : label;
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
            PixelIcons.spinner(graphics, x + 15, y + 11, status.color(), Util.getMillis());
        } else {
            PixelIcons.draw(graphics, status.icon(), x + 8, y + 4, 2, 0xFF000000 | status.color(), true);
        }
        int textX = x + 40;
        int room = this.contentWidth - 48;
        graphics.drawString(this.font, firstLine(this.font, status.head(), room), textX, y + 7,
                0xFF000000 | status.color(), true);
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            bar(graphics, textX, y + 20, room, UpdateInstaller.progress(), ACCENT);
        } else {
            graphics.drawString(this.font, firstLine(this.font, status.detail(), room), textX, y + 18, MUTED, false);
        }
        int height = this.bottom - this.top;
        card(graphics, x, this.top, this.listWidth, height, 0);
        card(graphics, this.notesX, this.top, this.notesWidth, height, 0);
        ScreenAnchors.report("versions.list", x, this.top, this.listWidth, height);
        ScreenAnchors.report("manager.news", this.notesX, this.top, this.notesWidth, height);
        ScreenAnchors.report("versions.buttons", this.use.getX(), this.use.getY(),
                this.second.getRight() - this.use.getX(), this.use.getHeight());
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawVersions(graphics, mouseX, mouseY);
        graphics.enableScissor(this.notesX + 1, this.top + 1, this.notesX + this.notesWidth - 1, this.bottom - 1);
        int y = this.top + PADDING - (int) this.scroll;
        for (ChangelogLayout.Block block : this.notes) {
            if (y + block.height() >= this.top && y <= this.bottom) {
                block.draw(graphics, this.notesX + PADDING, y);
            }
            y += block.height();
        }
        graphics.flush();
        graphics.disableScissor();
        this.drawScrollbar(graphics, this.notesX + this.notesWidth - SCROLLBAR, this.scroll, this.maxScroll());
        if (!UpdateInstaller.canInstall()) {
            graphics.drawString(this.font, firstLine(this.font, text("test"), this.contentWidth), this.contentX,
                    this.bottom + 3, 0xFF000000 | WARN, false);
        }
    }

    // One row a version: its number, a chip for yours and the latest, and its date under it.
    private void drawVersions(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.contentX + 1;
        int width = this.listWidth - 2;
        graphics.enableScissor(x, this.top + 1, x + width, this.bottom - 1);
        if (this.versions.isEmpty()) {
            Component note = text(UpdateChecker.releasesFailed() ? "versions.failed" : "versions.loading");
            this.wrap(graphics, note, x + 6, this.top + 6, width - 12, MUTED, 3);
        }
        int hover = this.versionAt(mouseX, mouseY);
        Release newest = newest(false);
        for (int i = 0; i < this.versions.size(); i++) {
            Release release = this.versions.get(i);
            int y = this.top + 2 + i * ROW - (int) this.listScroll;
            boolean chosen = release == this.chosen;
            if (i > 0) {
                graphics.fill(x + 6, y, x + width - 6, y + 1, 0x14FFFFFF);
            }
            if (chosen) {
                graphics.fill(x, y, x + width, y + ROW, 0x38FFFFFF);
                graphics.fill(x, y, x + width, y + 1, 0x20FFFFFF);
                graphics.fill(x, y, x + 2, y + ROW, 0xFF000000 | UpdatePopup.ACCENT);
            } else if (i == hover) {
                graphics.fill(x, y, x + width, y + ROW, 0x18FFFFFF);
            }
            graphics.drawString(this.font, "v" + release.version(), x + 7, y + 4, chosen ? 0xFFFFFFFF : 0xFFD8D8D8,
                    chosen);
            graphics.drawString(this.font, DATE.format(release.published()), x + 7, y + 14,
                    chosen ? 0xFFA8A090 : 0xFF000000 | GRAY, false);
            boolean own = installed(release);
            Component chip = own ? text("versions.yours")
                    : newest != null && release.version().equals(newest.version()) ? text("versions.newest") : null;
            if (chip != null) {
                this.chip(graphics, chip, x + width - SCROLLBAR - 4 - this.font.width(chip) - 6, y + 13,
                        own ? 0x5A6270 : UpdatePopup.ACCENT, own ? 0xE8E8E8 : 0x0E1A12);
            }
        }
        graphics.disableScissor();
        this.drawScrollbar(graphics, this.contentX + this.listWidth - SCROLLBAR, this.listScroll,
                this.maxListScroll());
    }

    private int versionAt(double mouseX, double mouseY) {
        if (mouseX < this.contentX || mouseX >= this.contentX + this.listWidth || mouseY < this.top
                || mouseY >= this.bottom) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - this.top - 2 + this.listScroll) / ROW);
        return index >= 0 && index < this.versions.size() ? index : -1;
    }

    private void drawScrollbar(GuiGraphics graphics, int x, double scroll, double max) {
        if (max <= 0) {
            return;
        }
        int view = this.bottom - this.top - 4;
        int thumb = Math.max(16, (int) (view * (double) view / (view + max)));
        int thumbY = this.top + 2 + (int) ((view - thumb) * (scroll / max));
        graphics.fill(x, this.top + 2, x + 3, this.top + 2 + view, 0x40FFFFFF);
        graphics.fill(x, thumbY, x + 3, thumbY + thumb, 0xC0FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.versionAt(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            this.choose(this.versions.get(index));
            this.click(1.0F);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY < this.top || mouseY >= this.bottom) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (mouseX < this.contentX + this.listWidth) {
            this.listScroll = Mth.clamp(this.listScroll - scrollY * SCROLL_STEP, 0.0, this.maxListScroll());
        } else {
            this.scrollBy(-scrollY * SCROLL_STEP);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int page = this.bottom - this.top - SCROLL_STEP;
        int at = this.versions.indexOf(this.chosen);
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> this.step(at - 1);
            case GLFW.GLFW_KEY_DOWN -> this.step(at + 1);
            case GLFW.GLFW_KEY_PAGE_UP -> this.scrollBy(-page);
            case GLFW.GLFW_KEY_PAGE_DOWN -> this.scrollBy(page);
            case GLFW.GLFW_KEY_HOME -> this.scrollBy(-this.notesContent);
            case GLFW.GLFW_KEY_END -> this.scrollBy(this.notesContent);
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    // Chooses the version at `index` in the list and keeps its row in view.
    private void step(int index) {
        if (index < 0 || index >= this.versions.size()) {
            return;
        }
        this.choose(this.versions.get(index));
        int view = this.bottom - this.top - 4;
        this.listScroll = Mth.clamp(this.listScroll, index * ROW + ROW - view, index * ROW);
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    private void scrollBy(double amount) {
        this.scroll = Mth.clamp(this.scroll + amount, 0.0, this.maxScroll());
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
