package nl.tivek.multiversepowers.update.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

// In place of NeoForge's refusal when a server runs another version of the mod: which version it runs and which you
// have, then back to the main menu, or switch to the server's version and quit, so the next start can join it.
final class VersionMismatchScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 146;
    private static final int ROW = 13;
    private static final int ERROR_COLOR = 0xFF7B7B;

    private final String server;
    @Nullable
    private Button change;
    private boolean reloaded;
    private int left;
    private int top;
    private int panelWidth;

    VersionMismatchScreen(String server) {
        super(text("title"));
        this.server = Release.stripV(server);
    }

    static Component text(String key, Object... args) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".mismatch." + key, args);
    }

    @Override
    protected void init() {
        UpdateChecker.loadReleases();
        this.panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(6, (this.height - PANEL_HEIGHT) / 2 - 8);
        int x = this.left + 10;
        int inner = this.panelWidth - 20;
        int y = this.top + PANEL_HEIGHT - 54;
        this.change = this.addRenderableWidget(Button.builder(text("switch", "v" + this.server), button -> {
            Release release = this.release();
            if (release != null) {
                UpdateInstaller.updateNow(release);
            }
        }).tooltip(Tooltip.create(UpdateManagerScreen.text("versions.tip"))).bounds(x, y, inner, 20).build());
        this.addRenderableWidget(Button.builder(text("back"), button -> this.minecraft.setScreen(new TitleScreen()))
                .bounds(x, y + 24, inner, 20).build());
        this.refresh();
    }

    // The server's version on the release page, with a jar to install.
    @Nullable
    private Release release() {
        List<Release> all = UpdateChecker.releasesIfLoaded();
        return all == null ? null : all.stream()
                .filter(release -> Release.compare(release.version(), this.server) == 0 && release.jar() != null)
                .findFirst().orElse(null);
    }

    @Override
    public void tick() {
        // A version newer than the list loaded earlier: load it once more.
        if (!this.reloaded && UpdateChecker.releasesIfLoaded() != null && this.release() == null) {
            this.reloaded = true;
            UpdateChecker.reloadReleases();
        }
        this.refresh();
    }

    private void refresh() {
        this.change.active = UpdateInstaller.canSwitch(this.release());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.left, this.top, this.panelWidth, PANEL_HEIGHT, PANEL_BORDER);
        int x = this.left + 10;
        int right = this.left + this.panelWidth - 10;
        graphics.drawString(this.font, this.title, x, this.top + 10, NOTICE_COLOR);
        drawDivider(graphics, x, this.top + 23, this.panelWidth - 20);
        int y = this.top + 30;
        this.row(graphics, text("server"), "v" + this.server, UpdatePopup.ACCENT, x, right, y);
        this.row(graphics, text("yours"), "v" + UpdateChecker.installed(), TEXT_COLOR, x, right, y + ROW);
        this.drawWrappedLeft(graphics, text("how"), x, y + 2 * ROW + 5, this.panelWidth - 20, 3,
                this.top + PANEL_HEIGHT - 56, MUTED_COLOR);
        this.status(graphics, this.top + PANEL_HEIGHT + 6);
    }

    private void row(GuiGraphics graphics, Component label, String value, int color, int x, int right, int y) {
        graphics.drawString(this.font, label, x, y, MUTED_COLOR);
        graphics.drawString(this.font, value, right - this.font.width(value), y, color);
    }

    // Under the panel: the download while it runs, else why the switch cannot be made yet.
    private void status(GuiGraphics graphics, int y) {
        int center = this.width / 2;
        String version = "v" + this.server;
        Component text;
        int color = MUTED_COLOR;
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            float progress = UpdateInstaller.progress();
            text = UpdateManagerScreen.text("versions.downloading", version, Math.round(progress * 100.0F));
            color = TEXT_COLOR;
            int barWidth = Math.min(200, this.panelWidth);
            drawBar(graphics, center - barWidth / 2, y + 11, barWidth, progress, UpdatePopup.ACCENT);
        } else if (UpdateInstaller.state() == UpdateInstaller.State.CHECKED) {
            text = UpdateManagerScreen.text("versions.checked", version);
            color = NOTICE_COLOR;
        } else if (UpdateInstaller.state() == UpdateInstaller.State.FAILED) {
            Component error = UpdateInstaller.error();
            text = error == null ? UpdateManagerScreen.text("error.network") : error;
            color = ERROR_COLOR;
        } else if (UpdateChecker.releasesIfLoaded() == null) {
            text = UpdateChecker.releasesFailed() ? text("offline") : text("looking", version);
            color = UpdateChecker.releasesFailed() ? ERROR_COLOR : MUTED_COLOR;
        } else if (this.release() == null) {
            text = this.reloaded ? text("missing", version) : text("looking", version);
        } else if (!UpdateInstaller.canInstall()) {
            text = UpdateManagerScreen.text("test");
            color = NOTICE_COLOR;
        } else {
            text = UpdateManagerScreen.text("versions.hint");
        }
        graphics.drawCenteredString(this.font, text, center, y, color);
    }
}
