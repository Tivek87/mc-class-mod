package nl.tivek.multiversepowers.command.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

// The game's own command suggestions under a text field anywhere on a screen, as the chat shows them: what may come
// next, Tab and the arrow keys to pick, and how the command is typed. The game draws them at a fixed height (72), so
// they are drawn moved down under the field, and the mouse is moved back the same way.
final class CommandInput {
    private static final int GAME_TOP = 72;
    private final CommandSuggestions suggestions;
    private final EditBox box;

    CommandInput(Minecraft minecraft, Screen screen, EditBox box) {
        this.box = box;
        this.suggestions = new CommandSuggestions(minecraft, screen, box, minecraft.font, true, false, 0, 7, false,
                0xE0000000);
        this.suggestions.setAllowHiding(false);
        this.suggestions.setAllowSuggestions(true);
        this.suggestions.updateCommandInfo();
    }

    // Each time the field's text changes.
    void edited() {
        this.suggestions.setAllowSuggestions(true);
        this.suggestions.updateCommandInfo();
    }

    private int down() {
        return this.box.getY() + this.box.getHeight() + 2 - GAME_TOP;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, int clipLeft, int clipRight) {
        if (!this.box.isFocused()) {
            return;
        }
        graphics.enableScissor(clipLeft, 0, clipRight, graphics.guiHeight());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, this.down(), 400.0F);
        this.suggestions.render(graphics, mouseX, mouseY - this.down());
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.box.isFocused() && this.suggestions.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.box.isFocused() && this.suggestions.mouseClicked(mouseX, mouseY - this.down(), button);
    }

    boolean mouseScrolled(double scrollY) {
        return this.box.isFocused() && this.suggestions.mouseScrolled(scrollY);
    }

    boolean showing() {
        return this.box.isFocused() && this.suggestions.isVisible();
    }
}
