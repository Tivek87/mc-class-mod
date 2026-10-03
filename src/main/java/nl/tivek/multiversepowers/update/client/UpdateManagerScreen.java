package nl.tivek.multiversepowers.update.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.bugreport.client.BugReportScreen;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

final class UpdateManagerScreen extends DirtBackgroundScreen {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    static final int ERROR_COLOR = 0xFF7B7B;
    static final int GRAY = 0x8A8A8A;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 190;
    private static final int CARD_TOP = 31;
    private static final int CARD = 42;
    private static final int BUTTONS = CARD_TOP + CARD + 10;

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
        this.left = (this.width - this.panelWidth) / 2;
        this.top = Math.max(8, (this.height - PANEL_HEIGHT) / 2 - 6);
        int x = this.left + 10;
        int inner = this.panelWidth - 20;
        int half = (inner - 4) / 2;
        int y = this.top + BUTTONS;
        if (this.release != null) {
            Release target = this.release;
            this.later = this.addRenderableWidget(Button.builder(text("later"),
                    button -> UpdateInstaller.updateLater(target)).bounds(x, y, half, 20).build());
            this.now = this.addRenderableWidget(Button.builder(text("now"), button -> UpdateInstaller.updateNow(target))
                    .tooltip(Tooltip.create(text("now.tip"))).bounds(x + half + 4, y, inner - half - 4, 20).build());
        } else {
            this.check = this.addRenderableWidget(Button.builder(text("check"), button -> UpdateChecker.checkNow())
                    .bounds(x, y, inner, 20).build());
        }
        y += 24;
        this.addRenderableWidget(Button.builder(text("whats_new"),
                button -> this.minecraft.setScreen(new ChangelogScreen(this))).bounds(x, y, inner, 20).build());
        y += 24;
        this.addRenderableWidget(Button.builder(text("report"),
                button -> this.minecraft.setScreen(BugReportScreen.bug(this))).bounds(x, y, half, 20).build());
        this.addRenderableWidget(Button.builder(text("idea"),
                button -> this.minecraft.setScreen(BugReportScreen.idea(this)))
                .bounds(x + half + 4, y, inner - half - 4, 20).build());
        y += 28;
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.left, this.top, this.panelWidth, PANEL_HEIGHT, PANEL_BORDER);
        int x = this.left + 10;
        int inner = this.panelWidth - 20;
        graphics.drawString(this.font, this.title, x, this.top + 10, TEXT_COLOR);
        Component own = Component.literal("v" + UpdateChecker.installed());
        graphics.drawString(this.font, own, x + inner - this.font.width(own), this.top + 10, MUTED_COLOR, false);
        drawDivider(graphics, x, this.top + 23, inner);

        Status status = this.status();
        int y = this.top + CARD_TOP;
        drawInset(graphics, x, y, inner, CARD, status.color());
        graphics.drawString(this.font, firstLine(this.font, status.head(), inner - 18), x + 9, y + 8,
                0xFF000000 | status.color());
        graphics.drawString(this.font, firstLine(this.font, status.detail(), inner - 18), x + 9, y + 21,
                0xFF000000 | MUTED_COLOR, false);
        if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
            drawBar(graphics, x + 9, y + CARD - 8, inner - 18, UpdateInstaller.progress(), UpdatePopup.ACCENT);
        }
        if (!UpdateInstaller.canInstall()) {
            graphics.drawCenteredString(this.font, text("test"), this.width / 2, this.top + PANEL_HEIGHT + 6,
                    NOTICE_COLOR);
        }
    }

    static FormattedCharSequence firstLine(Font font, Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    // What the card says: a headline in the state's colour and a line under it.
    private record Status(Component head, Component detail, int color) {
    }

    private Status status() {
        Release target = UpdateInstaller.target();
        String aimed = target == null ? "?" : "v" + target.version();
        Component yours = text("detail.yours", "v" + UpdateChecker.installed());
        switch (UpdateInstaller.state()) {
            case DOWNLOADING -> {
                return new Status(text("versions.downloading", aimed,
                        Math.round(UpdateInstaller.progress() * 100.0F)), yours, UpdatePopup.ACCENT);
            }
            case READY -> {
                return new Status(text("versions.ready", aimed), yours, NOTICE_COLOR);
            }
            case CHECKED -> {
                return new Status(text("head.checked", aimed), text("detail.test"), NOTICE_COLOR);
            }
            case FAILED -> {
                Component error = UpdateInstaller.error();
                return new Status(error == null ? text("error.network") : error, yours, ERROR_COLOR);
            }
            case IDLE -> {
            }
        }
        if (this.release != null) {
            return new Status(text("head.available", "v" + this.release.version()), this.released(), NOTICE_COLOR);
        }
        if (UpdateChecker.checking()) {
            return new Status(text("checking"), yours, GRAY);
        }
        if (UpdateChecker.lastFailed()) {
            return new Status(text("error.network"), yours, ERROR_COLOR);
        }
        if (UpdateChecker.lastCheck() == 0L) {
            return new Status(text("not_checked"), yours, GRAY);
        }
        return new Status(text("head.up_to_date"),
                text("detail.checked", "v" + UpdateChecker.installed(), ago(UpdateChecker.lastCheck())),
                UpdatePopup.ACCENT);
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

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
