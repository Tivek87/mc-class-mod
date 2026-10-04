package nl.tivek.multiversepowers.update.client.tour;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import org.lwjgl.glfw.GLFW;

// What a tour card holds and how it is drawn: a step's card (what it shows and in which version, how far along the
// tour is, what to do, and Back, Skip and Next, or their keys in game), the card saying how to go on while the next
// steps wait somewhere the tour cannot take the player, and the first ask, all on the same dark box.
final class TourCard {
    static final String PREFIX = "tour." + MultiversePowers.MODID + ".";
    static final int ACCENT = NavScreen.ACCENT;
    static final int LIGHT = 0xC8FFE0;
    static final int TEXT = 0xFFFFFFFF;
    static final int BODY = 0xFFD2D2D2;
    static final int MUTED = 0xFF8C8C8C;
    static final int INK = 0xFF0C2016;
    static final int PAD = 8;
    static final int BUTTON = 14;
    private static final int FILL = 0xF2101310;
    private static final int RIM = 0x22FFFFFF;
    private static final int HEADER = 11;
    private static final int PIPS_UP_TO = 12;
    private static final int HIGHLIGHTS = 4;
    private static final int NOTE_PAD = 6;
    private static final String ARROW = "▶ ";

    // A clickable part as last drawn.
    record Box(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= this.x && px < this.x + this.width && py >= this.y && py < this.y + this.height;
        }
    }

    // A step's words split to the card's width; the prompt beside its key cap, if it has one.
    record Body(List<FormattedCharSequence> title, List<FormattedCharSequence> text,
            List<FormattedCharSequence> prompt, @Nullable Component key) {
        int height() {
            int height = PAD + HEADER + 6 + this.title.size() * 10 + 3 + this.text.size() * 10;
            if (!this.prompt.isEmpty()) {
                height += 6 + this.prompt.size() * 10;
            }
            return height + 8 + 1 + 6 + BUTTON + PAD - 1;
        }
    }

    // Where a step card's buttons were drawn; null for one not there.
    record Buttons(@Nullable Box next, @Nullable Box back, @Nullable Box skip) {
    }

    // Where the first ask's buttons were drawn; `update` only while a newer version is out.
    record Ask(Box show, Box later, Box skip, @Nullable Box update) {
    }

    // Where the card saying how to go on drew its button, Later and Skip; all null in game, where it has keys.
    record Way(@Nullable Box go, @Nullable Box later, @Nullable Box skip) {
    }

    private TourCard() {
    }

    static Body body(Font font, TourStep step, int inner) {
        Component prompt = step.prompt();
        return body(font, step.title(), step.text(), prompt, prompt == null ? null : step.keyName(), inner);
    }

    // The card saying how to go on, with a prompt and the key to press when it has one.
    static Body wayBody(Font font, TourStep way, int inner) {
        String prompt = PREFIX + way.id() + ".prompt";
        return body(font, way.title(), way.text(), I18n.exists(prompt) ? Component.translatable(prompt) : null,
                way.keyName(), inner);
    }

    private static Body body(Font font, Component title, Component text, @Nullable Component prompt,
            @Nullable Component key, int inner) {
        List<FormattedCharSequence> asked = List.of();
        if (prompt != null) {
            int room = inner - font.width(ARROW) - (key == null ? 0 : KeyCap.width(font, key) + 4);
            asked = font.split(prompt, Math.max(40, room));
        }
        return new Body(font.split(title.copy().withStyle(ChatFormatting.BOLD), inner), font.split(text, inner),
                asked, prompt == null ? null : key);
    }

    // How wide the footer needs the card to be.
    static int footerWidth(Font font, boolean mouse, boolean back, Component next) {
        if (mouse) {
            int links = (back ? font.width(backLabel()) + 10 : 0) + font.width(skipLabel());
            return links + 12 + font.width(nextLabel(next)) + 10;
        }
        return keysWidth(font, back, next);
    }

    // How wide the footer of the card saying how to go on needs it to be: its button, Later and Skip, or in game Enter
    // for Later and holding it to skip.
    static int wayFooterWidth(Font font, boolean mouse, Component go) {
        if (mouse) {
            return font.width(laterLabel()) + 10 + font.width(skipLabel()) + 12 + font.width(nextLabel(go)) + 10;
        }
        return keysWidth(font, false, laterLabel());
    }

    // Held, Enter's label turns to Skip tour.
    private static int keysWidth(Font font, boolean back, Component next) {
        Component enter = enter();
        int right = KeyCap.width(font, enter) + 4 + Math.max(font.width(next), font.width(skipLabel()));
        int left = back ? KeyCap.width(font, backspace()) + 4 + font.width(Component.translatable(PREFIX + "back"))
                : font.width(Component.translatable(PREFIX + "hold_skip"));
        return left + 12 + right;
    }

    // The box: a black edge, a dark fill with a faint rim, the accent along its top and a shadow under it.
    static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 2, y + h + 1, x + w + 3, y + h + 3, 0x48000000);
        graphics.fill(x + w + 1, y + 2, x + w + 3, y + h + 1, 0x48000000);
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        graphics.fill(x, y, x + w, y + h, FILL);
        graphics.renderOutline(x, y, w, h, RIM);
        graphics.fill(x, y, x + w, y + 2, 0xFF000000 | ACCENT);
    }

    static Buttons step(KeyCap.Layer layer, Font font, TourStep step, Body body, int x, int y, int w, int h,
            int number, int total, boolean mouse, boolean back, Component next, float hold, double mouseX,
            double mouseY) {
        int left = x + PAD;
        int right = x + w - PAD;
        lines(layer, font, step.chip(), body, left, y);
        pips(layer, font, number, total, right, y + PAD);
        int footer = y + h - PAD - BUTTON;
        layer.graphics().fill(left, footer - 7, right, footer - 6, 0x18FFFFFF);
        if (mouse) {
            Box nextBox = button(layer, font, nextLabel(next), right, footer, mouseX, mouseY);
            int linkX = left;
            Box backBox = null;
            if (back) {
                backBox = link(layer, font, backLabel(), linkX, footer + 3, mouseX, mouseY);
                linkX += font.width(backLabel()) + 10;
            }
            Box skipBox = link(layer, font, skipLabel(), linkX, footer + 3, mouseX, mouseY);
            return new Buttons(nextBox, backBox, skipBox);
        }
        keys(layer, font, next, back, hold, left, right, footer);
        return new Buttons(null, null, null);
    }

    // The card saying how to go on: NEXT PART, what waits and how to get there, and its button with Later and Skip, or
    // in game Enter for Later.
    static Way waypoint(KeyCap.Layer layer, Font font, Body body, int x, int y, int w, int h, boolean mouse,
            Component go, float hold, double mouseX, double mouseY) {
        int left = x + PAD;
        int right = x + w - PAD;
        lines(layer, font, Component.translatable(PREFIX + "next_part"), body, left, y);
        int footer = y + h - PAD - BUTTON;
        layer.graphics().fill(left, footer - 7, right, footer - 6, 0x18FFFFFF);
        if (mouse) {
            Box goBox = button(layer, font, nextLabel(go), right, footer, mouseX, mouseY);
            Box laterBox = link(layer, font, laterLabel(), left, footer + 3, mouseX, mouseY);
            Box skipBox = link(layer, font, skipLabel(), left + font.width(laterLabel()) + 10, footer + 3, mouseX,
                    mouseY);
            return new Way(goBox, laterBox, skipBox);
        }
        keys(layer, font, laterLabel(), false, hold, left, right, footer);
        return new Way(null, null, null);
    }

    // A card's chip, title, text and prompt beside its key cap, from its top.
    private static void lines(KeyCap.Layer layer, Font font, Component chip, Body body, int left, int y) {
        int line = y + PAD;
        KeyCap.chip(layer, font, chip, left, line, 0xFF000000 | ACCENT, INK);
        line += HEADER + 6;
        for (FormattedCharSequence part : body.title()) {
            layer.sequence(part, left, line, TEXT);
            line += 10;
        }
        line += 3;
        for (FormattedCharSequence part : body.text()) {
            layer.sequence(part, left, line, BODY);
            line += 10;
        }
        if (!body.prompt().isEmpty()) {
            line += 6;
            layer.text(Component.literal(ARROW), left, line, 0xFF000000 | ACCENT);
            int textX = left + font.width(ARROW);
            if (body.key() != null) {
                textX += KeyCap.draw(layer, font, body.key(), textX, line - 2, 11) + 4;
            }
            for (FormattedCharSequence part : body.prompt()) {
                layer.sequence(part, textX, line, 0xFF000000 | ACCENT);
                line += 10;
            }
        }
    }

    // In game a card has keys: Enter for `next` (held, it skips the tour, its cap filling), and Backspace for Back.
    private static void keys(KeyCap.Layer layer, Font font, Component next, boolean back, float hold, int left,
            int right, int footer) {
        Component label = hold > 0.0F ? skipLabel() : next;
        int labelX = right - font.width(label);
        layer.text(label, labelX, footer + 3, hold > 0.0F ? TEXT : BODY);
        Component enter = enter();
        int capX = labelX - 4 - KeyCap.width(font, enter);
        KeyCap.draw(layer, font, enter, capX, footer + 1, 11);
        if (hold > 0.0F) {
            int filled = Math.round((KeyCap.width(font, enter) - 2) * Math.min(1.0F, hold));
            layer.graphics().fill(capX + 1, footer + 10, capX + 1 + filled, footer + 12, 0xFF000000 | ACCENT);
        }
        if (back) {
            Component cap = backspace();
            int width = KeyCap.draw(layer, font, cap, left, footer + 1, 11);
            layer.text(Component.translatable(PREFIX + "back"), left + width + 4, footer + 3, MUTED);
        } else {
            layer.text(Component.translatable(PREFIX + "hold_skip"), left, footer + 3, 0xFF5E5E5E);
        }
    }

    // The first ask: how many changes it shows, a few of them, how many steps on how things work follow, and Show me,
    // Later and Skip; while a newer version is out, a note asking to update to it first, and Update in place of Show me.
    static int introHeight(Font font, int inner, int count, int guides, List<Component> highlights,
            @Nullable String newer) {
        int lines = font.split(introText(count), inner).size();
        int shown = Math.min(HIGHLIGHTS, highlights.size());
        int more = highlights.size() > HIGHLIGHTS ? 1 : 0;
        int walk = guides <= 0 || count <= 0 ? 0 : 4 + font.split(guidesText(guides), inner).size() * 10;
        int note = newer == null ? 0 : 6 + font.split(updateText(newer), inner - NOTE_PAD * 2).size() * 10 + 6;
        return PAD + HEADER + 6 + 10 + 4 + lines * 10 + 6 + (shown + more) * 10 + walk + note + 8 + 1 + 6 + BUTTON
                + PAD - 1;
    }

    static Ask intro(KeyCap.Layer layer, Font font, String version, int count, int guides, List<Component> highlights,
            @Nullable String newer, int x, int y, int w, int h, double mouseX, double mouseY) {
        int left = x + PAD;
        int right = x + w - PAD;
        int inner = right - left;
        int line = y + PAD;
        KeyCap.chip(layer, font, Component.translatable(PREFIX + "new_in", version), left, line, 0xFF000000 | ACCENT,
                INK);
        line += HEADER + 6;
        layer.shadowed(Component.translatable(PREFIX + "intro.title").withStyle(ChatFormatting.BOLD), left, line,
                TEXT);
        line += 14;
        for (FormattedCharSequence part : font.split(introText(count), inner)) {
            layer.sequence(part, left, line, BODY);
            line += 10;
        }
        line += 6;
        for (int i = 0; i < Math.min(HIGHLIGHTS, highlights.size()); i++) {
            GuiShapes.roundRect(layer.graphics(), left + 1, line + 3, 3, 3, 0.0F, 0xFF000000 | ACCENT);
            layer.sequence(fit(font, highlights.get(i), inner - 9), left + 9, line, BODY);
            line += 10;
        }
        if (highlights.size() > HIGHLIGHTS) {
            layer.text(Component.translatable(PREFIX + "intro.more", highlights.size() - HIGHLIGHTS), left + 9, line,
                    MUTED);
            line += 10;
        }
        if (guides > 0 && count > 0) {
            line += 4;
            for (FormattedCharSequence part : font.split(guidesText(guides), inner)) {
                layer.sequence(part, left, line, MUTED);
                line += 10;
            }
        }
        int footer = y + h - PAD - BUTTON;
        if (newer != null) {
            List<FormattedCharSequence> note = font.split(updateText(newer), inner - NOTE_PAD * 2);
            int top = line + 6;
            int bottom = top + note.size() * 10 + 6;
            GuiShapes.roundRect(layer.graphics(), left, top, inner, bottom - top, 3.0F,
                    GuiShapes.fade(NavScreen.WARN, 0.18F));
            GuiShapes.roundRect(layer.graphics(), left, top, 2.0F, bottom - top, 1.0F, 0xFF000000 | NavScreen.WARN);
            for (int i = 0; i < note.size(); i++) {
                layer.sequence(note.get(i), left + NOTE_PAD, top + 4 + i * 10, 0xFF000000 | NavScreen.WARN);
            }
        }
        layer.graphics().fill(left, footer - 7, right, footer - 6, 0x18FFFFFF);
        Box update = null;
        Box show;
        int linkX = left;
        if (newer == null) {
            show = button(layer, font, Component.translatable(PREFIX + "intro.show").append(" ▶"), right, footer,
                    mouseX, mouseY);
        } else {
            update = button(layer, font, Component.translatable(PREFIX + "intro.update").append(" ▶"), right, footer,
                    mouseX, mouseY);
            Component anyway = Component.translatable(PREFIX + "intro.anyway");
            show = link(layer, font, anyway, linkX, footer + 3, mouseX, mouseY);
            linkX += font.width(anyway) + 10;
        }
        Component later = Component.translatable(PREFIX + "intro.later");
        Box laterBox = link(layer, font, later, linkX, footer + 3, mouseX, mouseY);
        Box skipBox = link(layer, font, Component.translatable(PREFIX + "intro.skip"), linkX + font.width(later) + 10,
                footer + 3, mouseX, mouseY);
        return new Ask(show, laterBox, skipBox, update);
    }

    private static Component introText(int count) {
        return Component.translatable(PREFIX + "intro." + (count == 0 ? "none" : count == 1 ? "one" : "many"), count);
    }

    private static Component guidesText(int guides) {
        return Component.translatable(PREFIX + "intro.guides" + (guides == 1 ? ".one" : ""), guides);
    }

    private static Component updateText(String newer) {
        return Component.translatable(PREFIX + "intro.newer", newer);
    }

    // How far along the tour is: a pip per step, the done ones lit and the one shown long; past a dozen, a count.
    private static void pips(KeyCap.Layer layer, Font font, int number, int total, int right, int y) {
        if (total > PIPS_UP_TO || total < 2) {
            Component count = Component.translatable(PREFIX + "count", number, total);
            layer.text(count, right - font.width(count), y + 2, MUTED);
            return;
        }
        GuiGraphics graphics = layer.graphics();
        int x = right;
        for (int i = total; i >= 1; i--) {
            int width = i == number ? 8 : 3;
            x -= width;
            int color = i < number ? 0xFF000000 | ACCENT : i == number ? TEXT : 0xFF3A3A3A;
            graphics.fill(x, y + 4, x + width, y + 7, color);
            x -= 2;
        }
    }

    // A small accent button ending at `right`.
    static Box button(KeyCap.Layer layer, Font font, Component label, int right, int y, double mouseX,
            double mouseY) {
        int w = font.width(label) + 10;
        int x = right - w;
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + BUTTON;
        int fill = 0xFF000000 | (hover ? GuiShapes.mix(ACCENT, 0xFFFFFF, 0.3F) : ACCENT);
        KeyCap.pill(layer.graphics(), x, y + 1, w, BUTTON, 0xFF000000 | GuiShapes.mix(ACCENT, 0, 0.6F));
        KeyCap.pill(layer.graphics(), x, y, w, BUTTON, fill);
        layer.text(label, x + 5, y + 3, INK);
        return new Box(x, y, w, BUTTON + 1);
    }

    static Box link(KeyCap.Layer layer, Font font, Component label, int x, int y, double mouseX, double mouseY) {
        int w = font.width(label);
        boolean hover = mouseX >= x - 2 && mouseX < x + w + 2 && mouseY >= y - 3 && mouseY < y + 10;
        layer.text(hover ? label.copy().withStyle(ChatFormatting.UNDERLINE) : label, x, y, hover ? TEXT : MUTED);
        return new Box(x - 2, y - 3, w + 4, 13);
    }

    static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle()).getVisualOrderText();
    }

    static Component nextLabel(Component next) {
        return next.copy().append(" ▶");
    }

    private static Component backLabel() {
        return Component.literal("◀ ").append(Component.translatable(PREFIX + "back"));
    }

    static Component skipLabel() {
        return Component.translatable(PREFIX + "skip");
    }

    private static Component laterLabel() {
        return Component.translatable(PREFIX + "intro.later");
    }

    static Component enter() {
        return InputConstants.getKey(GLFW.GLFW_KEY_ENTER, -1).getDisplayName();
    }

    private static Component backspace() {
        return InputConstants.getKey(GLFW.GLFW_KEY_BACKSPACE, -1).getDisplayName();
    }
}
