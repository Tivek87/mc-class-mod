package nl.tivek.multiversepowers.update.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import org.lwjgl.glfw.GLFW;

// The manager's What's new & versions page: the last ten versions listed as the game's launcher lists them, the chosen
// one's notes beside them, and under them a button that switches to the chosen version and one to the latest. A switch
// downloads that version's jar, closes the game and swaps it in (UpdateInstaller); the player starts the game again.
final class ChangelogScreen extends ManagerScreen {
    private static final int SHOWN = 10;
    private static final int GAP = 6;
    private static final int PADDING = 8;
    private static final int ROW = 25;
    private static final int SCROLLBAR = 6;
    private static final int SCROLL_STEP = 20;

    private int top;
    private List<Release> versions = List.of();
    @Nullable
    private Release chosen;
    private List<ChangelogLayout.Block> blocks = List.of();
    private int notesContent;
    private double scroll;
    private double listScroll;
    private int left;
    private int panelWidth;
    private int panelBottom;
    private int listWidth;
    private int notesX;
    private int notesWidth;
    private boolean complete;
    @Nullable
    private Button use;
    @Nullable
    private Button latest;
    @Nullable
    private Button retry;

    ChangelogScreen(@Nullable Screen root) {
        super(text("changelog.title"), root, VERSIONS);
    }

    @Override
    protected Component subtitle() {
        int count = UpdateChecker.newer().size();
        return count == 0 ? text("changelog.up_to_date", "v" + UpdateChecker.installed())
                : text(count == 1 ? "changelog.since_one" : "changelog.since_many", count,
                        "v" + UpdateChecker.installed());
    }

    @Override
    protected void initPage() {
        this.panelWidth = this.contentWidth;
        this.left = this.contentX;
        this.top = this.contentY;
        this.panelBottom = this.contentY + this.contentHeight - 38;
        this.listWidth = Mth.clamp(this.panelWidth * 3 / 8, 112, 140);
        this.notesX = this.left + this.listWidth + GAP;
        this.notesWidth = this.left + this.panelWidth - this.notesX;
        UpdateChecker.loadReleases();
        int half = (this.panelWidth - 4) / 2;
        int y = this.contentY + this.contentHeight - 20;
        this.use = this.addRenderableWidget(Button.builder(text("versions.use"),
                button -> this.switchTo(this.chosen)).tooltip(tip(text("versions.tip")))
                .bounds(this.left, y, half, 20).build());
        this.latest = this.addRenderableWidget(Button.builder(text("versions.latest"),
                button -> this.switchTo(newest(true))).tooltip(tip(text("versions.tip")))
                .bounds(this.left + half + 4, y, this.panelWidth - half - 4, 20).build());
        this.retry = this.addRenderableWidget(Button.builder(text("versions.retry"), button -> {
            this.complete = false;
            UpdateChecker.reloadReleases();
        }).bounds(this.left + 6, this.top + 46, this.listWidth - 12, 20).build());
        this.layout();
        this.refreshButtons();
    }

    // The ten newest versions, and yours under them when it is older; the chosen one's notes.
    private void layout() {
        List<Release> all = UpdateChecker.releasesIfLoaded();
        this.complete = all != null || UpdateChecker.releasesFailed();
        Component note = null;
        if (all == null) {
            this.versions = List.of();
            this.chosen = null;
        } else {
            List<Release> shown = new ArrayList<>(all.subList(0, Math.min(SHOWN, all.size())));
            Release own = UpdateChecker.installedRelease();
            if (own != null && !shown.contains(own)) {
                shown.add(own);
            }
            this.versions = shown;
            if (this.chosen == null || !shown.contains(this.chosen)) {
                this.chosen = shown.isEmpty() ? null : shown.get(0);
            }
            if (own == null) {
                note = UpdateManagerScreen.text("changelog.not_listed", "v" + UpdateChecker.installed());
            }
        }
        Release newest = newest(false);
        this.blocks = ChangelogLayout.build(this.font, this.chosen == null ? List.of() : List.of(this.chosen),
                UpdateChecker.installed(), newest == null ? "" : newest.version(), note,
                this.notesWidth - 2 * PADDING - SCROLLBAR, true);
        this.notesContent = this.blocks.stream().mapToInt(ChangelogLayout.Block::height).sum() + 2 * PADDING;
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

    private void choose(Release release) {
        if (release == this.chosen) {
            return;
        }
        this.chosen = release;
        this.scroll = 0.0;
        this.layout();
        this.refreshButtons();
    }

    private void switchTo(@Nullable Release release) {
        if (release != null && !installed(release)) {
            UpdateInstaller.updateNow(release);
        }
    }

    @Override
    public void tick() {
        if (!this.complete && (UpdateChecker.releasesIfLoaded() != null || UpdateChecker.releasesFailed())) {
            this.layout();
        }
        this.refreshButtons();
    }

    private void refreshButtons() {
        Release chosen = this.chosen;
        boolean own = chosen != null && installed(chosen);
        this.use.active = !own && UpdateInstaller.canSwitch(chosen);
        this.use.setMessage(chosen == null ? UpdateManagerScreen.text("versions.use")
                : own ? UpdateManagerScreen.text("versions.installed")
                : UpdateManagerScreen.text("versions.use_this", "v" + chosen.version()));
        Release newest = newest(true);
        boolean onLatest = newest != null && Release.compare(UpdateChecker.installed(), newest.version()) >= 0;
        this.latest.active = !onLatest && UpdateInstaller.canSwitch(newest);
        this.latest.setMessage(UpdateManagerScreen.text(onLatest ? "versions.on_latest" : "versions.latest"));
        this.retry.visible = UpdateChecker.releasesIfLoaded() == null && UpdateChecker.releasesFailed();
    }

    private double maxScroll() {
        return Math.max(0, this.notesContent - (this.panelBottom - this.top));
    }

    private double maxListScroll() {
        return Math.max(0, this.versions.size() * ROW + 4 - (this.panelBottom - this.top));
    }

    @Override
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        card(graphics, this.left, this.top, this.listWidth, this.panelBottom - this.top, 0);
        card(graphics, this.notesX, this.top, this.notesWidth, this.panelBottom - this.top, 0);
        ScreenAnchors.report("versions", this.left, this.top, this.panelWidth, this.panelBottom - this.top);
        ScreenAnchors.report("versions.list", this.left, this.top, this.listWidth, this.panelBottom - this.top);
        ScreenAnchors.report("versions.buttons", this.use.getX(), this.use.getY(),
                this.latest.getRight() - this.use.getX(), this.use.getHeight());
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.drawVersions(graphics, mouseX, mouseY);
        graphics.enableScissor(this.notesX + 1, this.top + 1, this.notesX + this.notesWidth - 1, this.panelBottom - 1);
        int y = this.top + PADDING - (int) this.scroll;
        for (ChangelogLayout.Block block : this.blocks) {
            if (y + block.height() >= this.top && y <= this.panelBottom) {
                block.draw(graphics, this.notesX + PADDING, y);
            }
            y += block.height();
        }
        graphics.flush();
        graphics.disableScissor();
        this.drawScrollbar(graphics, this.notesX + this.notesWidth - SCROLLBAR, this.scroll, this.maxScroll());
        this.drawStatus(graphics);
    }

    // One row a version: its number, a chip for yours and the latest, and its date under it.
    private void drawVersions(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.left + 1;
        int width = this.listWidth - 2;
        graphics.enableScissor(x, this.top + 1, x + width, this.panelBottom - 1);
        if (this.versions.isEmpty()) {
            Component note = UpdateManagerScreen.text(UpdateChecker.releasesFailed() ? "versions.failed"
                    : "versions.loading");
            this.wrap(graphics, note, x + 6, this.top + 8, width - 12, MUTED, 4);
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
            graphics.drawString(this.font, UpdateManagerScreen.DATE_TIME.format(release.published()), x + 7, y + 14,
                    chosen ? 0xFFA8A090 : 0xFF000000 | UpdateManagerScreen.GRAY, false);
            boolean own = installed(release);
            Component chip = own ? UpdateManagerScreen.text("versions.yours")
                    : newest != null && release.version().equals(newest.version())
                    ? UpdateManagerScreen.text("versions.newest") : null;
            if (chip != null) {
                this.chip(graphics, chip, x + width - 8 - this.font.width(chip) - 6, y + 3,
                        own ? 0x5A6270 : UpdatePopup.ACCENT, own ? 0xE8E8E8 : 0x0E1A12);
            }
        }
        graphics.disableScissor();
        this.drawScrollbar(graphics, this.left + this.listWidth - SCROLLBAR, this.listScroll, this.maxListScroll());
    }

    private int versionAt(double mouseX, double mouseY) {
        if (mouseX < this.left || mouseX >= this.left + this.listWidth || mouseY < this.top
                || mouseY >= this.panelBottom) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - this.top - 2 + this.listScroll) / ROW);
        return index >= 0 && index < this.versions.size() ? index : -1;
    }

    // Under the panels: the download while it runs, why switching failed or cannot work here, else what switching does.
    private void drawStatus(GuiGraphics graphics) {
        int y = this.panelBottom + 5;
        int x = this.left;
        Release target = UpdateInstaller.target();
        String aimed = target == null ? "?" : "v" + target.version();
        switch (UpdateInstaller.state()) {
            case DOWNLOADING -> {
                float progress = UpdateInstaller.progress();
                Component text = UpdateManagerScreen.text("versions.downloading", aimed, Math.round(progress * 100.0F));
                graphics.drawString(this.font, text, x, y, TEXT, false);
                int barX = x + this.font.width(text) + 8;
                bar(graphics, barX, y + 2, Math.max(20, x + this.panelWidth - barX), progress, ACCENT);
            }
            case FAILED -> {
                Component error = UpdateInstaller.error();
                this.status(graphics, error == null ? UpdateManagerScreen.text("error.network") : error, y, ERROR);
            }
            case READY -> this.status(graphics, UpdateManagerScreen.text("versions.ready", aimed), y, WARN);
            case CHECKED -> this.status(graphics, UpdateManagerScreen.text("versions.checked", aimed), y, WARN);
            case IDLE -> this.status(graphics, UpdateInstaller.canInstall() ? UpdateManagerScreen.text("versions.hint")
                    : UpdateManagerScreen.text("test"), y, UpdateInstaller.canInstall() ? 0x8E8E8E : WARN);
        }
    }

    private void status(GuiGraphics graphics, Component text, int y, int rgb) {
        graphics.drawString(this.font, this.fit(text, this.panelWidth), this.left, y, 0xFF000000 | rgb, false);
    }

    private void drawScrollbar(GuiGraphics graphics, int x, double scroll, double max) {
        if (max <= 0) {
            return;
        }
        int view = this.panelBottom - this.top - 4;
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
        if (mouseX < this.left + this.listWidth) {
            this.listScroll = Mth.clamp(this.listScroll - scrollY * SCROLL_STEP, 0.0, this.maxListScroll());
        } else {
            this.scrollBy(-scrollY * SCROLL_STEP);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int page = this.panelBottom - this.top - SCROLL_STEP;
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
        int view = this.panelBottom - this.top - 4;
        this.listScroll = Mth.clamp(this.listScroll, index * ROW + ROW - view, index * ROW);
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    private void scrollBy(double amount) {
        this.scroll = Mth.clamp(this.scroll + amount, 0.0, this.maxScroll());
    }
}
