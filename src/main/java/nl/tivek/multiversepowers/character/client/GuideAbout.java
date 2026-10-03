package nl.tivek.multiversepowers.character.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// The guide's top: the character in a line or two, and Read more, the page those lines unfold into over the guide's
// list and detail.
final class GuideAbout {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    private static final int EDGE = 0xFF000000;
    private static final int PAGE = 0xFF080808;
    private static final int SOFT = 0xFFC8C8C8;

    private final GameCharacter character;
    @Nullable
    private final Component summary;
    @Nullable
    private final Component about;
    private List<FormattedCharSequence> lines = List.of();
    private boolean open;
    private double scroll;
    private int height;
    private int left;
    private int right;
    private int y;

    GuideAbout(GameCharacter character) {
        String own = "character." + MultiversePowers.MODID + "." + character.getId() + ".";
        this.character = character;
        this.summary = Language.getInstance().has(own + "summary") ? Component.translatable(own + "summary") : null;
        this.about = Language.getInstance().has(own + "about") ? Component.translatable(own + "about") : null;
    }

    // Lays the summary out from `left` at `y`, its toggle ending at `right`; returns how many lines it takes.
    int place(Font font, int left, int right, int y) {
        this.left = left;
        this.right = right;
        this.y = y;
        int toggle = this.about == null ? 0 : Math.max(font.width(label(false)), font.width(label(true))) + 8;
        this.lines = this.summary == null ? List.of() : wrap(font, this.summary, right - left - toggle);
        return this.lines.size();
    }

    boolean open() {
        return this.open;
    }

    // Opens or closes the page; false when there is none or it already was.
    boolean open(boolean open) {
        if (open == this.open || this.about == null) {
            return false;
        }
        this.open = open;
        this.scroll = 0.0;
        return true;
    }

    boolean onToggle(Font font, double mouseX, double mouseY) {
        if (this.about == null) {
            return false;
        }
        int width = font.width(label(this.open));
        return mouseX >= this.right - width - 2 && mouseX < this.right + 2 && mouseY >= this.y - 2
                && mouseY < this.y + 10;
    }

    void drawTop(KeyCap.Layer layer, Font font, double mouseX, double mouseY, int accent) {
        for (int i = 0; i < this.lines.size(); i++) {
            layer.sequence(this.lines.get(i), this.left, this.y + 10 * i, SOFT);
        }
        if (!this.lines.isEmpty() || this.about != null) {
            ScreenAnchors.report("guide.summary", this.left - 2, this.y - 2, this.right - this.left + 4,
                    Math.max(1, this.lines.size()) * 10 + 2);
        }
        if (this.about != null) {
            Component toggle = label(this.open);
            layer.text(this.onToggle(font, mouseX, mouseY) ? toggle.copy().withStyle(ChatFormatting.UNDERLINE)
                    : toggle, this.right - font.width(toggle), this.y, accent);
        }
    }

    // The page, `width` by `height` from `x`, `top`.
    void drawPage(GuiGraphics graphics, Font font, int x, int top, int width, int height, int accent) {
        if (this.about == null) {
            return;
        }
        graphics.fill(x - 1, top - 1, x + width + 1, top + height + 1, EDGE);
        graphics.fill(x, top, x + width, top + height, PAGE);
        graphics.enableScissor(x, top, x + width, top + height);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int start = top - (int) Math.round(this.scroll);
        int y = start + 8;
        layer.shadowed(Component.translatable(PREFIX + "about", this.character.getDisplayName())
                .withStyle(ChatFormatting.BOLD), x + 10, y, accent);
        y += 16;
        for (FormattedCharSequence line : font.split(this.about, width - 20)) {
            layer.sequence(line, x + 10, y, SOFT);
            y += 11;
        }
        layer.finish();
        graphics.disableScissor();
        this.height = y - start + 6;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.most(height));
        AbilityGuideScreen.scrollbar(graphics, x + width - 5, top, height, this.scroll, this.most(height));
    }

    // Scrolls the page by `by`, shown `height` tall.
    void scroll(double by, int height) {
        this.scroll = Mth.clamp(this.scroll + by, 0.0, this.most(height));
    }

    private double most(int height) {
        return Math.max(0, this.height - height);
    }

    private static Component label(boolean open) {
        return Component.literal(open ? "▼ " : "▶ ").append(Component.translatable(PREFIX + (open ? "less" : "read")));
    }

    // At most two lines, the second cut short when the text runs on past it.
    private static List<FormattedCharSequence> wrap(Font font, Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(1, width));
        if (lines.size() <= 2) {
            return lines;
        }
        String first = font.getSplitter().splitLines(text, Math.max(1, width), text.getStyle()).get(0).getString();
        String rest = text.getString().substring(Math.min(first.length(), text.getString().length())).trim();
        return List.of(lines.get(0),
                AbilityGuideScreen.fit(font, Component.literal(rest).withStyle(text.getStyle()), width));
    }
}
