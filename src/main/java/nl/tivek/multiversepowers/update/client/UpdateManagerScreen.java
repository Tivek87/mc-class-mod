package nl.tivek.multiversepowers.update.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.bugreport.client.BugReportScreen;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

final class UpdateManagerScreen extends DirtBackgroundScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final int PANEL_WIDTH = 280;
    private static final int ROW = 13;
    private static final int ERROR_COLOR = 0xFF7B7B;

    @Nullable
    private final Screen parent;
    @Nullable
    private Release release;
    @Nullable
    private Button later;
    @Nullable
    private Button now;
    @Nullable
    private Button check;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    UpdateManagerScreen(@Nullable Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    static Component text(String key, Object... args) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".update." + key, args);
    }

    @Override
    protected void init() {
        this.release = UpdateChecker.latest();
        this.later = null;
        this.now = null;
        this.check = null;
        this.panelWidth = Math.min(PANEL_WIDTH, this.width - 20);
        this.panelHeight = 156;
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(6, (this.height - this.panelHeight) / 2);
        int x = this.left + 10;
        int inner = this.panelWidth - 20;
        int half = (inner - 6) / 2;
        int y = this.top + 78;
        this.addRenderableWidget(Button.builder(text("whats_new"),
                button -> this.minecraft.setScreen(new ChangelogScreen(this))).bounds(x, y, half, 20).build());
        this.addRenderableWidget(Button.builder(text("report"),
                button -> this.minecraft.setScreen(new BugReportScreen(this)))
                .bounds(x + half + 6, y, inner - half - 6, 20).build());
        if (this.release != null) {
            Release target = this.release;
            this.later = this.addRenderableWidget(Button.builder(text("later"),
                    button -> UpdateInstaller.updateLater(target)).bounds(x, y + 24, half, 20).build());
            boolean restart = Relaunch.possible();
            this.now = this.addRenderableWidget(Button.builder(text(restart ? "restart_now" : "close_now"),
                    button -> UpdateInstaller.updateAndRestart(target))
                    .tooltip(Tooltip.create(text(restart ? "restart_now.tip" : "close_now.tip")))
                    .bounds(x + half + 6, y + 24, inner - half - 6, 20).build());
        } else {
            this.check = this.addRenderableWidget(Button.builder(text("check"), button -> UpdateChecker.checkNow())
                    .bounds(x, y + 24, inner, 20).build());
        }
        y += 48;
        this.addRenderableWidget(Button.builder(text("close"), button -> this.onClose()).bounds(x, y, inner, 20).build());
        this.refreshButtons();
    }

    @Override
    public void tick() {
        if ((UpdateChecker.latest() == null) != (this.release == null)) {
            this.rebuildWidgets();
        }
        this.refreshButtons();
    }

    private void refreshButtons() {
        boolean busy = UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING;
        if (this.release != null) {
            boolean usable = UpdateInstaller.canInstall() && this.release.jar() != null;
            boolean scheduled = UpdateInstaller.scheduled(this.release);
            this.later.active = usable && !busy && !scheduled;
            this.now.active = usable && !busy;
        }
        if (this.check != null) {
            this.check.active = !UpdateChecker.checking();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.left, this.top, this.panelWidth, this.panelHeight, PANEL_BORDER);
        int x = this.left + 10;
        int right = this.left + this.panelWidth - 10;
        graphics.drawString(this.font, this.title, x, this.top + 10, TEXT_COLOR);
        drawDivider(graphics, x, this.top + 23, this.panelWidth - 20);

        int y = this.top + 30;
        this.row(graphics, "installed", Component.literal("v" + UpdateChecker.installed()), TEXT_COLOR, x, right, y);
        boolean known = this.release != null || (UpdateChecker.lastCheck() != 0L && !UpdateChecker.lastFailed());
        Component newest = Component.literal(!known ? "-"
                : "v" + (this.release == null ? UpdateChecker.installed() : this.release.version()));
        this.row(graphics, "newest", newest, this.release == null ? MUTED_COLOR : UpdatePopup.ACCENT, x, right, y + ROW);
        this.status(graphics, x, right, y + 2 * ROW);
    }

    private void row(GuiGraphics graphics, String label, Component value, int color, int x, int right, int y) {
        graphics.drawString(this.font, text("row." + label), x, y, MUTED_COLOR);
        graphics.drawString(this.font, value, right - this.font.width(value), y, color);
    }

    private void status(GuiGraphics graphics, int x, int right, int y) {
        if (this.release == null) {
            if (UpdateChecker.checking()) {
                this.row(graphics, "status", text("checking"), TEXT_COLOR, x, right, y);
            } else if (UpdateChecker.lastFailed()) {
                this.row(graphics, "status", text("error.network"), ERROR_COLOR, x, right, y);
            } else if (UpdateChecker.lastCheck() == 0L) {
                this.row(graphics, "status", text("not_checked"), MUTED_COLOR, x, right, y);
            } else {
                this.row(graphics, "status", text("up_to_date", ago(UpdateChecker.lastCheck())), UpdatePopup.ACCENT,
                        x, right, y);
            }
            return;
        }
        if (!UpdateInstaller.canInstall()) {
            this.row(graphics, "status", text("dev"), ERROR_COLOR, x, right, y);
            return;
        }
        switch (UpdateInstaller.state()) {
            case DOWNLOADING -> {
                float progress = UpdateInstaller.progress();
                this.row(graphics, "status", text("downloading", Math.round(progress * 100.0F)), TEXT_COLOR, x, right, y);
                int barWidth = right - x;
                GuiShapes.roundRect(graphics, x, y + 12, barWidth, 3, 1.5F, 0xFF2A2F36);
                GuiShapes.roundRect(graphics, x, y + 12, Math.max(3.0F, barWidth * progress), 3, 1.5F,
                        0xFF000000 | UpdatePopup.ACCENT);
                GuiShapes.flush(graphics);
            }
            case READY -> this.row(graphics, "status", UpdateInstaller.scheduled(this.release) ? text("ready_later")
                    : this.released(), UpdateInstaller.scheduled(this.release) ? NOTICE_COLOR : MUTED_COLOR, x, right, y);
            case FAILED -> {
                Component error = UpdateInstaller.error();
                this.row(graphics, "status", error == null ? text("error.network") : error, ERROR_COLOR, x, right, y);
            }
            case IDLE -> this.row(graphics, "status", this.released(), MUTED_COLOR, x, right, y);
        }
    }

    private Component released() {
        String size = this.release.jar() == null ? "?" : String.format(Locale.ENGLISH, "%.1f MB",
                this.release.jar().size() / 1_048_576.0);
        return text("released", DATE.format(this.release.published()), size);
    }

    private static Component ago(long time) {
        long minutes = (System.currentTimeMillis() - time) / 60_000L;
        if (minutes < 1) {
            return text("ago.now");
        }
        return minutes < 60 ? text("ago.minutes", minutes) : text("ago.hours", minutes / 60);
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
