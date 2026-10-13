package nl.tivek.multiversepowers.bugreport.client.form;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

// A form's text area, which says whether the wheel can still scroll its own text that way: else the form scrolls.
final class FormArea extends MultiLineEditBox {
    FormArea(Font font, int width, int height, Component placeholder, Component name) {
        super(font, 0, 0, width, height, placeholder, name);
    }

    // The game's text area takes a click and keys while hidden: one in a folded section would steal them.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.visible && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.visible && super.keyPressed(keyCode, scanCode, modifiers);
    }

    boolean scrolls(double scrollY) {
        return this.scrollbarVisible()
                && (scrollY > 0 ? this.scrollAmount() > 0 : this.scrollAmount() < this.getMaxScrollAmount());
    }
}
