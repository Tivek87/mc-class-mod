package nl.tivek.multiversepowers.command.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

// The commands a search finds, under the search field as the chat shows suggestions: the arrow keys pick one, Tab or
// Enter (or a click) puts it in the field, Escape hides them.
final class SearchSuggestions {
    private static final int LINES = 7;
    private static final int LINE = 12;
    private final EditBox box;
    private final List<CommandRows.Entry> found = new ArrayList<>();
    private int picked;
    private int first;
    private boolean hidden;
    private String searched = "";

    SearchSuggestions(EditBox box) {
        this.box = box;
    }

    // The commands matching what is typed, those whose name starts with it first.
    void update(List<CommandRows.Entry> all) {
        String query = this.box.getValue();
        if (query.equals(this.searched)) {
            return;
        }
        this.searched = query;
        this.hidden = false;
        this.found.clear();
        this.picked = 0;
        this.first = 0;
        String wanted = query.trim().toLowerCase(Locale.ROOT).replaceFirst("^/", "");
        if (wanted.isEmpty()) {
            return;
        }
        for (CommandRows.Entry entry : all) {
            if (!entry.removed() && CommandRows.matches(entry, query) && !entry.name().equals(wanted)) {
                this.found.add(entry);
            }
        }
        this.found.sort(Comparator.comparing((CommandRows.Entry entry) -> !entry.name().startsWith(wanted))
                .thenComparing(CommandRows.Entry::name));
    }

    boolean showing() {
        return this.box.isFocused() && !this.hidden && !this.found.isEmpty();
    }

    private int top() {
        return this.box.getY() + this.box.getHeight() + 1;
    }

    private int width(Font font) {
        return Math.max(this.box.getWidth(), 220);
    }

    void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!this.showing()) {
            return;
        }
        int x = this.box.getX();
        int y = this.top();
        int width = this.width(font);
        int lines = Math.min(LINES, this.found.size());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.fill(x - 1, y - 1, x + width + 1, y + lines * LINE + 1, 0xFF505050);
        graphics.fill(x, y, x + width, y + lines * LINE, 0xFF101010);
        for (int i = 0; i < lines; i++) {
            CommandRows.Entry entry = this.found.get(this.first + i);
            int lineY = y + i * LINE;
            boolean over = mouseX >= x && mouseX < x + width && mouseY >= lineY && mouseY < lineY + LINE;
            boolean lit = this.first + i == this.picked;
            if (lit || over) {
                graphics.fill(x, lineY, x + width, lineY + LINE, lit ? 0x40FFFFFF : 0x20FFFFFF);
            }
            String name = "/" + entry.name();
            graphics.drawString(font, name, x + 2, lineY + 2, lit ? 0xFFFFFF55 : 0xFFE0E0E0, false);
            int about = x + 6 + font.width(name);
            graphics.drawString(font, font.plainSubstrByWidth(entry.about().getString(), x + width - about - 2),
                    about, lineY + 2, 0xFF8E8E8E, false);
        }
        if (this.found.size() > LINES) {
            Component more = Component.literal((this.first + 1) + "-" + (this.first + lines) + " / "
                    + this.found.size());
            int moreX = x + width - font.width(more) - 4;
            graphics.fill(moreX - 2, y + lines * LINE, x + width, y + lines * LINE + 11, 0xFF101010);
            graphics.drawString(font, more, moreX, y + lines * LINE + 2, 0xFF8E8E8E, false);
        }
        graphics.pose().popPose();
    }

    boolean keyPressed(int keyCode) {
        if (!this.showing()) {
            return false;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> this.move(1);
            case GLFW.GLFW_KEY_UP -> this.move(-1);
            case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> this.use(this.picked);
            case GLFW.GLFW_KEY_ESCAPE -> this.hidden = true;
            default -> {
                return false;
            }
        }
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY, Font font) {
        if (!this.showing()) {
            return false;
        }
        int x = this.box.getX();
        int y = this.top();
        int lines = Math.min(LINES, this.found.size());
        if (mouseX < x || mouseX >= x + this.width(font) || mouseY < y || mouseY >= y + lines * LINE) {
            return false;
        }
        this.use(this.first + (int) ((mouseY - y) / LINE));
        return true;
    }

    boolean mouseScrolled(double scrollY) {
        if (!this.showing()) {
            return false;
        }
        this.move(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void move(int step) {
        this.picked = Math.floorMod(this.picked + step, this.found.size());
        if (this.picked < this.first) {
            this.first = this.picked;
        } else if (this.picked >= this.first + LINES) {
            this.first = this.picked - LINES + 1;
        }
    }

    private void use(int index) {
        if (index >= 0 && index < this.found.size()) {
            this.box.setValue("/" + this.found.get(index).name());
            this.box.moveCursorToEnd(false);
            this.searched = this.box.getValue();
            this.found.clear();
        }
    }
}
