package nl.tivek.multiversepowers.update.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;

// In place of NeoForge's refusal when a server runs another version of the mod: which version it runs and which you
// have, then back to the main menu, or switch to the server's version and quit, so the next start can join it. Drawn
// as the update manager's window is, without its pages.
final class VersionMismatchScreen extends Screen {
    private static final int MAX_WIDTH = 320;
    private static final int HEIGHT = 160;
    private static final int TITLE_BAR = 20;
    private static final int PAD = 10;
    private static final int WARN = 0xF2C84B;
    private static final int ERROR_COLOR = 0xFF7B7B;
    private static final int MUTED = 0xFF8E8E8E;

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
        this.panelWidth = Math.min(MAX_WIDTH, this.width - 16);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(8, (this.height - HEIGHT) / 2);
        int x = this.left + PAD;
        int inner = this.panelWidth - PAD * 2;
        int half = (inner - 4) / 2;
        int y = this.top + HEIGHT - PAD - 20;
        this.addRenderableWidget(Button.builder(text("back"), button -> this.minecraft.setScreen(new TitleScreen()))
                .bounds(x, y, half, 20).build());
        Component label = text("switch", UpdateManagerScreen.name(this.server));
        this.change = this.addRenderableWidget(Button.builder(label, button -> {
            Release release = this.release();
            if (release != null) {
                UpdateInstaller.updateNow(release);
            }
        }).tooltip(Tooltip.create(UpdateManagerScreen.text("versions.tip"))).bounds(x + half + 4, y, inner - half - 4,
                20).build());
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
        int right = this.left + this.panelWidth;
        int bottom = this.top + HEIGHT;
        graphics.fill(this.left - 1, this.top - 1, right + 1, bottom + 1, 0xFF000000);
        graphics.fill(this.left, this.top, right, bottom, 0xEE121212);
        graphics.fill(this.left, this.top, right, this.top + TITLE_BAR, 0xFF1B1B1B);
        graphics.fill(this.left, this.top, right, this.top + 1, 0x1CFFFFFF);
        graphics.fill(this.left, this.top + TITLE_BAR - 1, right, this.top + TITLE_BAR, 0x50000000);
        graphics.fill(this.left, this.top + 1, this.left + 2, this.top + TITLE_BAR - 1, 0xFF000000 | WARN);
        PixelIcons.draw(graphics, PixelIcons.Icon.WARNING, this.left + 7, this.top + 4, 1, 0xFF000000 | WARN, true);
        graphics.drawString(this.font, this.title, this.left + 24, this.top + 6, 0xFFFFFFFF, true);
        int x = this.left + PAD;
        int inner = this.panelWidth - PAD * 2;
        int y = this.top + TITLE_BAR + 8;
        NavScreen.card(graphics, x, y, inner, 30, UpdatePopup.ACCENT);
        int end = x + inner - 8;
        graphics.drawString(this.font, text("server"), x + 8, y + 6, MUTED, false);
        Stage.drawVersion(graphics, this.font, this.server, end - Stage.versionWidth(this.font, this.server), y + 6,
                0xFF000000 | UpdatePopup.ACCENT);
        String yours = UpdateManagerScreen.name(UpdateChecker.installed());
        graphics.drawString(this.font, text("yours"), x + 8, y + 17, MUTED, false);
        graphics.drawString(this.font, yours, end - this.font.width(yours), y + 17, 0xFFFFFFFF, false);
        y += 38;
        List<FormattedCharSequence> lines = this.font.split(text("how"), inner);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            graphics.drawString(this.font, lines.get(i), x, y + i * 10, 0xFFD2D2D2, false);
        }
        this.status(graphics, x, inner, this.top + HEIGHT - PAD - 20 - 17);
    }

    // Above the buttons: the download while it runs, else why the switch cannot be made yet.
    private void status(GuiGraphics graphics, int x, int width, int y) {
        String version = UpdateManagerScreen.name(this.server);
        Component text;
        int color = MUTED;
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            float progress = UpdateInstaller.progress();
            text = UpdateManagerScreen.text("versions.downloading", version, Math.round(progress * 100.0F));
            color = 0xFFFFFFFF;
            NavScreen.bar(graphics, x, y + 11, width, progress, UpdatePopup.ACCENT);
        } else if (UpdateInstaller.state() == UpdateInstaller.State.CHECKED) {
            text = UpdateManagerScreen.text("versions.checked", version);
            color = 0xFF000000 | WARN;
        } else if (UpdateInstaller.state() == UpdateInstaller.State.FAILED) {
            Component error = UpdateInstaller.error();
            text = error == null ? UpdateManagerScreen.text("error.network") : error;
            color = 0xFF000000 | ERROR_COLOR;
        } else if (UpdateChecker.releasesIfLoaded() == null) {
            text = UpdateChecker.releasesFailed() ? text("offline") : text("looking", version);
            color = UpdateChecker.releasesFailed() ? 0xFF000000 | ERROR_COLOR : MUTED;
        } else if (this.release() == null) {
            text = this.reloaded ? text("missing", version) : text("looking", version);
        } else if (!UpdateInstaller.canInstall()) {
            text = UpdateManagerScreen.text("test");
            color = 0xFF000000 | WARN;
        } else {
            text = UpdateManagerScreen.text("versions.hint");
        }
        graphics.drawString(this.font, UpdateManagerScreen.firstLine(this.font, text, width), x, y, color, false);
    }
}
