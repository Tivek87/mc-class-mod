package nl.tivek.welcomescreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.character.CharacterConfigScreen;
import nl.tivek.welcomescreen.client.stamina.StaminaConfigScreen;

/**
 * The settings of this mod, from the Mods list: first pick what you want to change, the stamina bar or
 * one of the characters. Every character has their own screen with their own numbers.
 */
public class ModConfigScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 300;
    private static final String PREFIX = "screen." + WelcomeScreenMod.MODID + ".config.";

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
                button -> this.minecraft.setScreen(new StaminaConfigScreen(this)))
                .bounds(centerX - 110, top, 220, 20).build());

        int y = top + 24;
        for (GameCharacter character : GameCharacter.values()) {
            this.addRenderableWidget(Button.builder(character.getDisplayName(),
                    button -> this.minecraft.setScreen(new CharacterConfigScreen(this, character)))
                    .bounds(centerX - 110, y, 220, 20).build());
            y += 24;
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 55, y + 8, 110, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int height = this.panelHeight();
        int top = (this.height - height) / 2;
        drawPanel(graphics, centerX - PANEL_WIDTH / 2, top, PANEL_WIDTH, height, PANEL_BORDER);
        this.drawBigCenteredString(graphics, this.title, centerX, top + 10, 1.2F, 0xFFFFD255);
        graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "pick"), centerX, top + 30,
                MUTED_COLOR);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
