package nl.tivek.welcomescreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.config.SettingsPages;
import nl.tivek.welcomescreen.client.config.SettingsScreen;

/**
 * The settings of this mod, from the Mods list: first pick what you want to change, the stamina bar or
 * one of the characters. Every one of them has its own page (see {@link SettingsScreen}).
 */
public class ModConfigScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 300;
    private static final String PREFIX = "config." + WelcomeScreenMod.MODID + ".";

    private final Screen lastScreen;

    public ModConfigScreen(Screen lastScreen) {
        super(Component.translatable(PREFIX + "title"));
        this.lastScreen = lastScreen;
    }

    private int panelHeight() {
        return 76 + (GameCharacter.values().length + 1) * 24;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int top = (this.height - this.panelHeight()) / 2 + 46;

        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "stamina"),
                button -> this.minecraft.setScreen(new SettingsScreen(this, SettingsPages.stamina())))
                .bounds(centerX - 110, top, 220, 20)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "stamina.about"))).build());

        int y = top + 24;
        for (GameCharacter character : GameCharacter.values()) {
            this.addRenderableWidget(Button.builder(character.getDisplayName(),
                    button -> this.minecraft.setScreen(new SettingsScreen(this, SettingsPages.character(character))))
                    .bounds(centerX - 110, y, 220, 20)
                    .tooltip(Tooltip.create(Component.translatable(PREFIX + "character.about",
                            character.getDisplayName())))
                    .build());
            y += 24;
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 55, y + 8, 110, 20).build());
    }

    /**
     * The dirt, the panel and its titles. The game draws the background again at the start of every frame, before
     * the buttons, so everything that has to lie under them goes here.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int height = this.panelHeight();
        int top = (this.height - height) / 2;
        drawPanel(graphics, centerX - PANEL_WIDTH / 2, top, PANEL_WIDTH, height, PANEL_BORDER);
        this.drawBigCenteredString(graphics, this.title, centerX, top + 10, 1.2F, 0xFFFFD255);
        graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "pick"), centerX, top + 30,
                MUTED_COLOR);
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
