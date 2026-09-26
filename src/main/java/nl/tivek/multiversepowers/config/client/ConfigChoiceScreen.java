package nl.tivek.multiversepowers.config.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

public class ConfigChoiceScreen extends DirtBackgroundScreen {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".choice.";
    private static final int CARD_WIDTH = 170;
    private static final int CARD_HEIGHT = 34;
    private static final int GAP = 12;
    private static final int ALLOWED = 0x7CE07C;

    @Nullable
    private final Screen lastScreen;
    private int cardsTop;
    private int clientX;
    private int serverX;
    private boolean stacked;

    public ConfigChoiceScreen(@Nullable Screen lastScreen) {
        super(Component.translatable("config." + MultiversePowers.MODID + ".title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.stacked = this.width < 2 * CARD_WIDTH + GAP + 24;
        int center = this.width / 2;
        if (this.stacked) {
            this.clientX = center - CARD_WIDTH / 2;
            this.serverX = this.clientX;
            this.cardsTop = Math.max(40, this.height / 2 - CARD_HEIGHT - 40);
        } else {
            this.clientX = center - CARD_WIDTH - GAP / 2;
            this.serverX = center + GAP / 2;
            this.cardsTop = Math.max(40, this.height / 2 - 40);
        }
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "client"), pressed ->
                this.minecraft.setScreen(new SettingsScreen(this, SettingsScreen.Kind.CLIENT, 0)))
                .bounds(this.clientX, this.cardsTop, CARD_WIDTH, CARD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "server"), pressed ->
                this.minecraft.setScreen(new SettingsScreen(this, SettingsScreen.Kind.SERVER, 0)))
                .bounds(this.serverX, this.serverTop(), CARD_WIDTH, CARD_HEIGHT).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, pressed -> this.onClose())
                .bounds(center - 60, this.height - 28, 120, 20).build());
    }

    private int serverTop() {
        return this.stacked ? this.cardsTop + CARD_HEIGHT + 44 : this.cardsTop;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderTransparentBackground(graphics);
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
        int left = Math.min(this.clientX, this.serverX) - 10;
        int right = Math.max(this.clientX, this.serverX) + CARD_WIDTH + 10;
        drawPanel(graphics, left, 8, right - left, this.serverTop() + CARD_HEIGHT + 48 - 8, PANEL_BORDER);
        this.drawBigCenteredString(graphics, this.title, this.width / 2, 16, 1.3F, 0xFFFFD255);
        graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "pick"), this.width / 2, 30,
                MUTED_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.caption(graphics, Component.translatable(PREFIX + "client.desc"), this.clientX,
                this.cardsTop + CARD_HEIGHT + 4, MUTED_COLOR);
        this.caption(graphics, Component.translatable(PREFIX + "server.desc"), this.serverX,
                this.serverTop() + CARD_HEIGHT + 4, MUTED_COLOR);
        Component access;
        int color;
        if (this.minecraft.level == null) {
            access = Component.translatable(PREFIX + "server.closed");
            color = MUTED_COLOR;
        } else if (SettingsPages.serverEditable()) {
            access = Component.translatable(PREFIX + "server.allowed");
            color = ALLOWED;
        } else {
            access = Component.translatable(PREFIX + "server.locked");
            color = NOTICE_COLOR;
        }
        this.caption(graphics, access, this.serverX, this.serverTop() + CARD_HEIGHT + 26, color);
    }

    private void caption(GuiGraphics graphics, Component text, int x, int y, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, CARD_WIDTH);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawCenteredString(this.font, lines.get(i), x + CARD_WIDTH / 2, y + i * 10, color);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
