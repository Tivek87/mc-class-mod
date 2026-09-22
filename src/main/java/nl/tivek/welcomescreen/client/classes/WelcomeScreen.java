package nl.tivek.welcomescreen.client.classes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.client.DirtBackgroundScreen;

/**
 * First screen after joining: greets the player by name and sends them on to the class picker.
 */
public class WelcomeScreen extends DirtBackgroundScreen {
    private static final int PANEL_HEIGHT = 74;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int SECTION_GAP = 10;
    private static final int MARGIN = 8;
    private static final int NOTICE_WIDTH = 300;
    private static final int NOTICE_LINES = 5;

    private final String playerName;

    public WelcomeScreen(String playerName) {
        super(Component.translatable("screen." + WelcomeScreenMod.MODID + ".title"));
        this.playerName = playerName;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH / 2;
        int top = this.noticeTop() + this.noticeHeight() + SECTION_GAP;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + WelcomeScreenMod.MODID + ".continue"),
                button -> this.openClassSelection()
        ).bounds(left, top, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui." + WelcomeScreenMod.MODID + ".leave"),
                button -> this.leaveGame()
        ).bounds(left, top + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int panelTop = this.panelTop();

        drawPanel(guiGraphics, centerX - this.noticeWidth() / 2, panelTop, this.noticeWidth(), PANEL_HEIGHT, PANEL_BORDER);

        this.drawBigCenteredString(guiGraphics, this.title, centerX, panelTop + 14, 1.8F, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.playerName, centerX, panelTop + 36, 0xFFFFD98A);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + WelcomeScreenMod.MODID + ".greeting"),
                centerX, panelTop + 52, 0xFF000000 | MUTED_COLOR);

        this.drawNotice(guiGraphics, notice("title"), notice("body"), centerX - this.noticeWidth() / 2,
                this.noticeTop(), this.noticeWidth(), NOTICE_LINES);
    }

    // Panel, notice and buttons as one block, centred on the screen.
    private int panelTop() {
        int buttonsHeight = 2 * BUTTON_HEIGHT + BUTTON_GAP;
        int total = PANEL_HEIGHT + SECTION_GAP + this.noticeHeight() + SECTION_GAP + buttonsHeight;
        return Math.max(MARGIN, (this.height - total) / 2);
    }

    private int noticeTop() {
        return this.panelTop() + PANEL_HEIGHT + SECTION_GAP;
    }

    private int noticeWidth() {
        return Math.min(NOTICE_WIDTH, this.width - 2 * MARGIN);
    }

    private int noticeHeight() {
        return this.noticeHeight(notice("title"), notice("body"), this.noticeWidth(), NOTICE_LINES);
    }

    private void openClassSelection() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(new GroupSelectionScreen(this));
        }
    }

    private void leaveGame() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            return;
        }

        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        minecraft.disconnect();
        minecraft.setScreen(new TitleScreen());
    }
}
