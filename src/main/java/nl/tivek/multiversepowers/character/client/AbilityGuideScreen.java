package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import org.lwjgl.glfw.GLFW;

// Every ability of the character you play: a list on the left (with a dot for whether it can be used now) and the
// chosen one on the right, with its keys and what it does in plain words.
final class AbilityGuideScreen extends Screen {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    private static final int MAX_WIDTH = 520;
    private static final int MAX_HEIGHT = 320;
    private static final int RADIUS = 6;
    private static final int ROW = 16;
    private static final int HEADING = 16;
    private static final int FOOTER = 20;
    private static final int WINDOW = 0xF50E1118;
    private static final int EDGE = 0xFF2A303C;
    private static final int LIST = 0xFF090C11;
    private static final int HOVER = 0x14FFFFFF;
    private static final int LINE = 0x22FFFFFF;
    private static final int TEXT = 0xFFE6EAF0;
    private static final int SOFT = 0xFFB9C0CA;
    private static final int MUTED = 0xFF8B94A1;
    private static final int DIM = 0xFF5E6672;
    private static final int SCROLLBAR = 0x50FFFFFF;

    private final GameCharacter character;
    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private Component passive;
    private int count;
    private int selected = -1;
    private double listScroll;
    private double detailScroll;
    private int detailHeight;
    private float shownY = Float.NaN;
    private long lastFrame = Util.getMillis();
    private int left;
    private int top;
    private int windowWidth;
    private int windowHeight;
    private int header;
    private int listX;
    private int listY;
    private int listWidth;
    private int bodyHeight;
    private int detailX;
    private int detailWidth;

    // An ability, a mode, or a heading when it is neither.
    private record Row(@Nullable CharacterAbility ability, @Nullable GuideMode mode, Component title) {
        boolean heading() {
            return this.ability == null && this.mode == null;
        }

        int height() {
            return this.heading() ? HEADING : ROW;
        }
    }

    AbilityGuideScreen(GameCharacter character) {
        super(Component.translatable(PREFIX + "title", character.getDisplayName()));
        this.character = character;
    }

    @Override
    protected void init() {
        this.rows.clear();
        String passive = "character." + MultiversePowers.MODID + "." + this.character.getId() + ".passive";
        this.passive = Language.getInstance().has(passive) ? Component.translatable(passive) : null;
        boolean split = false;
        for (CharacterAbility ability : this.character.abilities()) {
            split |= !ability.isPlaceholder() && ability.when() == CharacterAbility.When.FLYING;
        }
        this.count = 0;
        List<GuideMode> modes = AbilityGuide.modes(this.character);
        this.group(split ? Component.translatable(PREFIX + "ground")
                : modes.isEmpty() ? null : Component.translatable(PREFIX + "abilities"), false);
        if (split) {
            this.group(Component.translatable(PREFIX + "flying"), true);
        }
        if (!modes.isEmpty()) {
            this.rows.add(new Row(null, null, Component.translatable(PREFIX + "modes")));
            for (GuideMode mode : modes) {
                this.rows.add(new Row(null, mode, mode.title()));
            }
        }
        this.windowWidth = Math.min(this.width - 16, MAX_WIDTH);
        this.windowHeight = Math.min(this.height - 16, MAX_HEIGHT);
        this.left = (this.width - this.windowWidth) / 2;
        this.top = (this.height - this.windowHeight) / 2;
        this.header = this.passive == null ? 36 : 50;
        this.listX = this.left + 8;
        this.listY = this.top + this.header + 6;
        this.listWidth = Mth.clamp((int) (this.windowWidth * 0.36F), 110, 170);
        this.bodyHeight = this.top + this.windowHeight - FOOTER - this.listY;
        this.detailX = this.listX + this.listWidth + 12;
        this.detailWidth = this.left + this.windowWidth - 12 - this.detailX;
        if (this.selected < 0 || this.selected >= this.rows.size()) {
            this.selected = this.next(-1, 1);
        }
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    private void group(@Nullable Component heading, boolean flying) {
        if (heading != null) {
            this.rows.add(new Row(null, null, heading));
        }
        for (CharacterAbility ability : this.character.abilities()) {
            if (!ability.isPlaceholder() && (ability.when() == CharacterAbility.When.FLYING) == flying) {
                this.rows.add(new Row(ability, null, ability.getDisplayName()));
                this.count++;
            }
        }
    }

    // The next row that is no heading from `from` going `step`, or `from` when there is none.
    private int next(int from, int step) {
        for (int i = from + step; i >= 0 && i < this.rows.size(); i += step) {
            if (!this.rows.get(i).heading()) {
                return i;
            }
        }
        return from;
    }

    private void select(int index) {
        if (index < 0 || index >= this.rows.size() || this.rows.get(index).heading()) {
            return;
        }
        this.selected = index;
        this.detailScroll = 0.0;
        int start = this.offset(index);
        if (index > 0 && this.rows.get(index - 1).heading()) {
            start -= HEADING;
        }
        int end = this.offset(index) + ROW + 4;
        if (start < this.listScroll) {
            this.listScroll = start;
        } else if (end > this.listScroll + this.bodyHeight) {
            this.listScroll = end - this.bodyHeight;
        }
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    // A row's top within the list before scrolling.
    private int offset(int index) {
        int y = 4;
        for (int i = 0; i < index; i++) {
            y += this.rows.get(i).height();
        }
        return y;
    }

    private double maxListScroll() {
        return Math.max(0, this.offset(this.rows.size()) + 4 - this.bodyHeight);
    }

    private double maxDetailScroll() {
        return Math.max(0, this.detailHeight - (this.bodyHeight - GuideDetail.STATUS));
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < this.listX || mouseX >= this.listX + this.listWidth || mouseY < this.listY
                || mouseY >= this.listY + this.bodyHeight) {
            return -1;
        }
        double y = this.listY - this.listScroll;
        for (int i = 0; i < this.rows.size(); i++) {
            double rowTop = y + this.offset(i);
            if (mouseY >= rowTop && mouseY < rowTop + this.rows.get(i).height()) {
                return i;
            }
        }
        return -1;
    }

    private FormattedCharSequence fit(Component text, int width) {
        if (this.font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = this.font.plainSubstrByWidth(text.getString(), Math.max(0, width - this.font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle()).getVisualOrderText();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return;
        }
        Font font = this.font;
        int color = this.character.getColor();
        int accent = 0xFF000000 | color;
        int right = this.left + this.windowWidth;
        int bottom = this.top + this.windowHeight;

        KeyCap.Layer frame = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, this.left - 1, this.top - 1, this.windowWidth + 2, this.windowHeight + 2,
                RADIUS + 1, EDGE);
        GuiShapes.roundRect(graphics, this.left, this.top, this.windowWidth, this.windowHeight, RADIUS, WINDOW);
        GuiShapes.roundRect(graphics, this.left, this.top + 3, this.windowWidth, 22, 0.0F,
                GuiShapes.fade(color, 0.06F));
        frame.shadowed(this.title.copy().withStyle(ChatFormatting.BOLD), this.left + 14, this.top + 12, accent);
        frame.text(Component.translatable(PREFIX + "subtitle"), this.left + 14, this.top + 24, MUTED);
        Component counted = Component.translatable(PREFIX + "count", this.count);
        KeyCap.chip(frame, font, counted, right - 14 - font.width(counted) - 8, this.top + 12, 0xFF1C222C, MUTED);
        if (this.passive != null) {
            int y = this.top + 38;
            GuiShapes.disc(graphics, this.left + 18.5F, y + 4.0F, 4.5F, GuiShapes.fade(color, 0.45F));
            frame.text(Component.literal("i"), this.left + 18, y, TEXT);
            frame.sequence(this.fit(this.passive, this.windowWidth - 42), this.left + 28, y, SOFT);
        }
        GuiShapes.roundRect(graphics, this.left + 8, this.top + this.header, this.windowWidth - 16, 1, 0.0F, LINE);
        GuiShapes.roundRect(graphics, this.listX, this.listY, this.listWidth, this.bodyHeight, 4.0F, LIST);
        this.footer(frame, font, bottom, right);
        frame.finish();

        graphics.enableScissor(this.left, this.top, right, this.top + 3);
        GuiShapes.roundRect(graphics, this.left, this.top, this.windowWidth, RADIUS * 2, RADIUS, accent);
        GuiShapes.flush(graphics);
        graphics.disableScissor();

        this.list(graphics, font, player, mouseX, mouseY, color);

        Row chosen = this.selected < 0 ? null : this.rows.get(this.selected);
        if (chosen != null && !chosen.heading()) {
            this.detailHeight = chosen.ability() != null
                    ? GuideDetail.draw(graphics, font, chosen.ability(), player, this.detailX, this.listY,
                            this.detailWidth, this.bodyHeight, this.detailScroll)
                    : GuideDetail.draw(graphics, font, chosen.mode(), player, this.detailX, this.listY,
                            this.detailWidth, this.bodyHeight, this.detailScroll);
            this.detailScroll = Mth.clamp(this.detailScroll, 0.0, this.maxDetailScroll());
            this.scrollbar(graphics, this.detailX + this.detailWidth + 4, this.listY,
                    this.bodyHeight - GuideDetail.STATUS, this.detailScroll, this.maxDetailScroll());
        }
    }

    private void list(GuiGraphics graphics, Font font, LocalPlayer player, int mouseX, int mouseY, int color) {
        long now = Util.getMillis();
        float seconds = Math.min(0.1F, (now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        float target = this.offset(Math.max(0, this.selected));
        this.shownY = Float.isNaN(this.shownY) ? target
                : this.shownY + (target - this.shownY) * (1.0F - (float) Math.exp(-seconds * 20.0F));

        graphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.bodyHeight);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        float base = (float) (this.listY - this.listScroll);
        if (this.selected >= 0) {
            float y = base + this.shownY;
            GuiShapes.roundRect(graphics, this.listX + 3, y, this.listWidth - 6, ROW, 3.0F,
                    GuiShapes.fade(color, 0.20F));
            GuiShapes.roundRect(graphics, this.listX + 3, y + 3, 2, ROW - 6, 1.0F, 0xFF000000 | color);
        }
        int hover = this.rowAt(mouseX, mouseY);
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int y = Math.round(base + this.offset(i));
            if (y + row.height() < this.listY || y > this.listY + this.bodyHeight) {
                continue;
            }
            if (row.heading()) {
                layer.text(Component.literal(row.title().getString().toUpperCase(Locale.ROOT)), this.listX + 9,
                        y + 6, DIM);
                continue;
            }
            if (i == hover && i != this.selected) {
                GuiShapes.roundRect(graphics, this.listX + 3, y, this.listWidth - 6, ROW, 3.0F, HOVER);
            }
            if (row.ability() != null) {
                KeyCap.dot(graphics, this.listX + 12.0F, y + ROW * 0.5F, 2.5F,
                        GuideDetail.status(row.ability(), player).color());
            } else {
                boolean on = row.mode().active().test(player);
                GuiShapes.ring(graphics, this.listX + 12.0F, y + ROW * 0.5F, 2.4F, 1.2F,
                        on ? GuideDetail.GREEN : 0xFF000000 | color);
            }
            layer.sequence(this.fit(row.title(), this.listWidth - 28), this.listX + 19, y + 4,
                    i == this.selected ? TEXT : SOFT);
        }
        layer.finish();
        graphics.disableScissor();
        this.scrollbar(graphics, this.listX + this.listWidth - 4, this.listY, this.bodyHeight, this.listScroll,
                this.maxListScroll());
    }

    private void scrollbar(GuiGraphics graphics, int x, int y, int height, double scroll, double max) {
        if (max <= 0.0) {
            return;
        }
        int track = height - 8;
        float bar = Math.max(14.0F, track * track / (float) (track + max));
        float barTop = y + 4 + (float) ((track - bar) * (scroll / max));
        GuiShapes.roundRect(graphics, x, barTop, 2, bar, 1.0F, SCROLLBAR);
        GuiShapes.flush(graphics);
    }

    private void footer(KeyCap.Layer layer, Font font, int bottom, int right) {
        GuiGraphics graphics = layer.graphics();
        int y = bottom - FOOTER + 5;
        GuiShapes.roundRect(graphics, this.left + 8, bottom - FOOTER, this.windowWidth - 16, 1, 0.0F, LINE);
        int x = this.left + 12;
        x += KeyCap.draw(layer, font, Component.literal("↑"), x, y, 11) + 2;
        x += KeyCap.draw(layer, font, Component.literal("↓"), x, y, 11) + 4;
        Component choose = Component.translatable(PREFIX + "choose");
        layer.text(choose, x, y + 2, MUTED);
        Component close = Component.translatable(PREFIX + "close");
        Component esc = Component.literal("Esc");
        Component key = PowerInputs.keyName(AbilityGuide.KEY);
        int end = right - 12 - font.width(close);
        layer.text(close, end, y + 2, MUTED);
        end -= 4 + KeyCap.width(font, esc);
        KeyCap.draw(layer, font, esc, end, y, 11);
        if (!AbilityGuide.KEY.isUnbound()) {
            end -= 2 + KeyCap.width(font, key);
            KeyCap.draw(layer, font, key, end, y, 11);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.rowAt(mouseX, mouseY);
        if (button == 0 && index >= 0 && !this.rows.get(index).heading()) {
            this.select(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overDetail = mouseX >= this.detailX && mouseX < this.detailX + this.detailWidth
                && mouseY >= this.listY && mouseY < this.listY + this.bodyHeight;
        if (overDetail && this.maxDetailScroll() > 0.0) {
            this.detailScroll = Mth.clamp(this.detailScroll - scrollY * 12.0, 0.0, this.maxDetailScroll());
        } else {
            this.listScroll = Mth.clamp(this.listScroll - scrollY * ROW * 1.5, 0.0, this.maxListScroll());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AbilityGuide.KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        int index = switch (keyCode) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> this.next(this.selected, -1);
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> this.next(this.selected, 1);
            case GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_PAGE_UP -> this.next(-1, 1);
            case GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_PAGE_DOWN -> this.next(this.rows.size(), -1);
            default -> -1;
        };
        if (index >= 0) {
            this.select(index);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
