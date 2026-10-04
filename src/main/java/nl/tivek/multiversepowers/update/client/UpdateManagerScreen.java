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

// The manager's updates page, in two parts. What's new: whether you are up to date (or which version is out, or how
// its download goes), what the newer versions bring or else what yours brought, and only while a newer version is out a
// button that updates now and one that updates once you quit. Versions: the last ten versions as the game's launcher
// lists them, the chosen one's notes beside them (a newer one's with those of every version between it and yours) and a
// button that switches to it. A switch downloads that version's jar, closes the game and swaps it in
// (UpdateInstaller); the player starts the game again.
final class UpdateManagerScreen extends ManagerScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final int ERROR_COLOR = 0xFF7B7B;
    static final int GRAY = 0x8A8A8A;
    private static final int CARD = 32;
    private static final int SHOWN = 10;
    private static final int GAP = 5;
    private static final int PADDING = 7;
    private static final int ROW = 16;
    private static final int SCROLLBAR = 6;
    private static final int SCROLL_STEP = 20;

    private final boolean news;
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
    private Button later;
    @Nullable
    private Button retry;

    UpdateManagerScreen(@Nullable Screen root, String part) {
        super(text(part.equals(VERSIONS) ? "title.versions" : "title"), root, UPDATES);
        this.news = !part.equals(VERSIONS);
    }

    @Override
    protected List<Tab> tabs() {
        return this.updatesTabs(this.news ? NEWS : VERSIONS);
    }

    @Override
    protected void initPage() {
        this.release = UpdateChecker.latest();
        UpdateChecker.loadReleases();
        int x = this.contentX;
        int end = this.contentY + this.contentHeight;
        boolean buttons = !this.news || this.release != null;
        int buttonsY = end - 20;
        this.top = this.news ? this.contentY + CARD + GAP : this.contentY;
        this.bottom = buttons ? buttonsY - GAP - (UpdateInstaller.canInstall() ? 0 : 12) : end;
        // Room for a version, its tag and a short date (or "yours") on one line.
        this.listWidth = this.news ? 0 : Mth.clamp(this.contentWidth * 3 / 8, 136, 144);
        this.notesX = this.news ? x : x + this.listWidth + GAP;
        this.notesWidth = x + this.contentWidth - this.notesX;
        int half = (this.contentWidth - 4) / 2;
        this.use = this.addRenderableWidget(Button.builder(text("versions.use"), button -> this.switchTo())
                .tooltip(tip(text("versions.tip"))).bounds(x, buttonsY, half, 20).build());
        this.later = this.addRenderableWidget(Button.builder(text("later"), button -> this.later())
                .tooltip(tip(text("later.tip"))).bounds(x + half + 4, buttonsY, this.contentWidth - half - 4, 20)
                .build());
        int retryX = this.news ? this.notesX : x;
        int retryWidth = this.news ? Math.min(120, this.notesWidth) : this.listWidth;
        this.retry = this.addRenderableWidget(Button.builder(text("versions.retry"), button -> {
            this.complete = false;
            UpdateChecker.reloadReleases();
        }).bounds(retryX + 6, this.top + 38, retryWidth - 12, 20).build());
        this.layout();
        this.refreshButtons();
    }

    @Override
    public void tick() {
        if (UpdateChecker.latest() != this.release) {
            // A version just came out: What's new shows it, with the buttons that get it.
            this.chosen = null;
            this.rebuildWidgets();
            return;
        }
        if (!this.complete && (UpdateChecker.releasesIfLoaded() != null || UpdateChecker.releasesFailed())) {
            this.layout();
        }
        this.refreshButtons();
    }

    // What's new: every version newer than yours, newest first, so the notes say all that updating brings; else yours.
    // Versions: the ten newest and yours under them when it is older, and the chosen one's notes, a newer one's with
    // those of every version between it and yours.
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
                note = text("changelog.not_listed", name(UpdateChecker.installed()));
            }
            Release pick = this.news ? newest(false) : this.chosen;
            if (pick != null && newer(pick)) {
                shown = all.stream()
                        .filter(release -> newer(release) && Release.compare(release.version(), pick.version()) <= 0)
                        .toList();
            } else if (!this.news && pick != null) {
                shown = List.of(pick);
            } else if (own != null) {
                shown = List.of(own);
            }
        }
        this.notes = ChangelogLayout.build(this.font, shown, UpdateChecker.installed(), note,
                this.notesWidth - 2 * PADDING - SCROLLBAR, false);
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

    // A version as the update screens show it: "v0.7.3", without the ending its tag shows (`Stage`).
    static String name(String version) {
        int dash = version.indexOf('-');
        return "v" + (dash < 0 || Stage.of(version) == null ? version : version.substring(0, dash));
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

    // What the buttons switch to: the chosen version, or on What's new the newest one with a jar to install.
    @Nullable
    private Release target() {
        return this.news ? newest(true) : this.chosen;
    }

    private void switchTo() {
        Release pick = this.target();
        if (pick != null && !installed(pick)) {
            UpdateInstaller.updateNow(pick);
        }
    }

    // A newer version is put in once the game closes.
    private void later() {
        Release pick = this.target();
        if (pick != null && newer(pick)) {
            UpdateInstaller.updateLater(pick);
        }
    }

    private double maxScroll() {
        return Math.max(0, this.notesContent - (this.bottom - this.top));
    }

    private double maxListScroll() {
        return Math.max(0, this.versions.size() * ROW + 4 - (this.bottom - this.top));
    }

    // What's new shows its buttons only while a newer version is out; Update on quit only for a newer version, the
    // other button taking the whole row while it is away.
    private void refreshButtons() {
        Release pick = this.target();
        boolean own = pick != null && installed(pick);
        boolean ahead = pick != null && newer(pick);
        this.use.visible = !this.news || this.release != null;
        this.later.visible = this.use.visible && ahead;
        this.use.setWidth(this.later.visible ? this.later.getX() - 4 - this.use.getX() : this.contentWidth);
        this.use.active = pick != null && !own && UpdateInstaller.canSwitch(pick);
        this.use.setMessage(pick == null ? text("versions.use") : own ? text("versions.installed")
                : this.quits(text(ahead ? "versions.update_to" : "versions.use_this", name(pick.version())),
                        this.use.getWidth()));
        if (ahead) {
            boolean scheduled = UpdateInstaller.scheduled(pick);
            this.later.active = !scheduled && UpdateInstaller.canSwitch(pick);
            this.later.setMessage(text(scheduled ? "versions.on_quit" : "later"));
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
        int height = this.bottom - this.top;
        if (this.news) {
            this.drawStatus(graphics, mouseX, mouseY);
        } else {
            card(graphics, this.contentX, this.top, this.listWidth, height, 0);
            ScreenAnchors.report("versions.list", this.contentX, this.top, this.listWidth, height);
        }
        card(graphics, this.notesX, this.top, this.notesWidth, height, 0);
        ScreenAnchors.report("manager.news", this.notesX, this.top, this.notesWidth, height);
        if (this.use.visible) {
            Button last = this.later.visible ? this.later : this.use;
            ScreenAnchors.report("versions.buttons", this.use.getX(), this.use.getY(),
                    last.getRight() - this.use.getX(), this.use.getHeight());
        }
    }

    // The card on top of What's new: a headline in the state's colour (with the tag of a version that is out), a line
    // under it, its icon (null: a spinner) and, while nothing is out or on its way, Check now.
    private void drawStatus(GuiGraphics graphics, int mouseX, int mouseY) {
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
        if (this.release == null && !UpdateChecker.checking()
                && UpdateInstaller.state() == UpdateInstaller.State.IDLE) {
            Component check = text("check");
            int width = this.font.width(check);
            int linkX = x + this.contentWidth - 8 - width;
            this.link(graphics, check, linkX, y + 12, mouseX, mouseY, 0xFF000000 | ACCENT, UpdateChecker::checkNow);
            ScreenAnchors.report("manager.check", linkX - 2, y + 9, width + 4, 14);
            if (mouseX >= linkX - 1 && mouseX < linkX + width + 1 && mouseY >= y + 10 && mouseY < y + 22) {
                this.setTooltipForNextRenderPass(text("auto", UpdatePopup.OPEN_KEY.getTranslatedKeyMessage()));
            }
            room = linkX - 10 - textX;
        }
        FormattedCharSequence head = firstLine(this.font, status.head(), room);
        graphics.drawString(this.font, head, textX, y + 7, 0xFF000000 | status.color(), true);
        int tagX = textX + this.font.width(head) + 5;
        if (status.stage() != null && tagX + status.stage().width(this.font) <= textX + room) {
            status.stage().draw(graphics, this.font, tagX, y + 6);
        }
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            bar(graphics, textX, y + 20, room, UpdateInstaller.progress(), ACCENT);
        } else {
            graphics.drawString(this.font, firstLine(this.font, status.detail(), room), textX, y + 18, MUTED, false);
        }
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.news) {
            this.drawVersions(graphics, mouseX, mouseY);
        }
        graphics.enableScissor(this.notesX + 1, this.top + 1, this.notesX + this.notesWidth - 1, this.bottom - 1);
        if (this.news && this.notes.isEmpty()) {
            Component note = text(UpdateChecker.releasesFailed() ? "versions.failed" : "versions.loading");
            this.wrap(graphics, note, this.notesX + PADDING, this.top + PADDING, this.notesWidth - 2 * PADDING, MUTED,
                    3);
        }
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
        if (!UpdateInstaller.canInstall() && this.use.visible) {
            graphics.drawString(this.font, firstLine(this.font, text("test"), this.contentWidth), this.contentX,
                    this.bottom + 3, 0xFF000000 | WARN, false);
        }
    }

    // One row a version: its number and tag, and at the right the day it came out, or "yours" on your own.
    private void drawVersions(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.contentX + 1;
        int width = this.listWidth - 2;
        graphics.enableScissor(x, this.top + 1, x + width, this.bottom - 1);
        if (this.versions.isEmpty()) {
            Component note = text(UpdateChecker.releasesFailed() ? "versions.failed" : "versions.loading");
            this.wrap(graphics, note, x + 6, this.top + 6, width - 12, MUTED, 3);
        }
        int hover = this.versionAt(mouseX, mouseY);
        int dayRight = x + width - SCROLLBAR - 4;
        for (int i = 0; i < this.versions.size(); i++) {
            Release release = this.versions.get(i);
            int y = this.top + 2 + i * ROW - (int) this.listScroll;
            boolean chosen = release == this.chosen;
            if (chosen) {
                graphics.fill(x, y, x + width, y + ROW, 0x30FFFFFF);
                graphics.fill(x, y, x + 2, y + ROW, 0xFF000000 | UpdatePopup.ACCENT);
            } else if (i == hover) {
                graphics.fill(x, y, x + width, y + ROW, 0x16FFFFFF);
            }
            Stage.drawVersion(graphics, this.font, release.version(), x + 7, y + 4, chosen ? TEXT : 0xFFD0D0D0);
            boolean own = installed(release);
            Component right = own ? text("versions.yours") : Component.literal(DAY.format(release.published()));
            graphics.drawString(this.font, right, dayRight - this.font.width(right), y + 4,
                    0xFF000000 | (own ? UpdatePopup.ACCENT : GRAY), false);
            if (Stage.of(release.version()) != null && !ScreenAnchors.shown("versions.stage") && y >= this.top
                    && y + ROW <= this.bottom) {
                ScreenAnchors.report("versions.stage", x, y, width, ROW);
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
            case GLFW.GLFW_KEY_UP -> {
                if (this.news) {
                    this.scrollBy(-SCROLL_STEP);
                } else {
                    this.step(at - 1);
                }
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (this.news) {
                    this.scrollBy(SCROLL_STEP);
                } else {
                    this.step(at + 1);
                }
            }
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

    // What the card says: a headline in the state's colour, a line under it, its icon (null: a spinner), and the stage
    // of the version it names.
    private record Status(Component head, Component detail, int color, @Nullable PixelIcons.Icon icon,
            @Nullable Stage stage) {
        Status(Component head, Component detail, int color, @Nullable PixelIcons.Icon icon) {
            this(head, detail, color, icon, null);
        }
    }

    private Status status() {
        Release target = UpdateInstaller.target();
        String aimed = target == null ? "?" : name(target.version());
        Component yours = text("detail.yours", name(UpdateChecker.installed()));
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
            return new Status(text("head.available", name(this.release.version())), this.released(), WARN,
                    PixelIcons.Icon.DOWNLOAD, Stage.of(this.release.version()));
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
        return new Status(text("head.up_to_date"), text("detail.checked", ago(UpdateChecker.lastCheck())), ACCENT,
                PixelIcons.Icon.CHECK);
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
