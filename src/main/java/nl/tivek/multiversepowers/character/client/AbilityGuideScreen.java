package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import org.lwjgl.glfw.GLFW;

// The guide to the character you play: who they are in one short line on top, which unfolds into a page of its own, then
// mode by mode a tab for each state its keys work in (the one you are in has a green dot), the binds that do something
// there on the left, each with a dot for whether it works now, and the chosen one on the right: what it does there in
// one line, what it costs, and folded under that the ability's full explanation.
final class AbilityGuideScreen extends Screen {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    private static final int MAX_WIDTH = 540;
    private static final int MAX_HEIGHT = 330;
    private static final int ROW = 15;
    private static final int HEADING = 14;
    private static final int TAB = 15;
    private static final int TAB_GAP = 3;
    private static final int FOOTER = 20;
    private static final int WINDOW = 0xF0101010;
    private static final int EDGE = 0xFF000000;
    private static final int RIM = 0x1EFFFFFF;
    private static final int LIST = 0xFF080808;
    private static final int HOVER = 0x18FFFFFF;
    private static final int TAB_FILL = 0xFF2A2A2A;
    private static final int LINE = 0x22FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SOFT = 0xFFC8C8C8;
    private static final int MUTED = 0xFF9A9A9A;
    private static final int DIM = 0xFF6E6E6E;
    private static final int SCROLLBAR = 0x50FFFFFF;

    private final GameCharacter character;
    private final List<GuideMode> modes;
    // The open tab's rows: null is its overview, then its controls and headings in order.
    private final List<GuideMode.Control> rows = new ArrayList<>();
    private int[] tabX = new int[0];
    private int[] tabY = new int[0];
    private int[] tabWidth = new int[0];
    private final GuideAbout about;
    // Whether the chosen control's full explanation is unfolded, and where its fold line was drawn.
    private boolean more;
    private int fold = -1;
    private int tab;
    private int selected;
    private double listScroll;
    private double detailScroll;
    private int detailHeight;
    private float shownY = Float.NaN;
    private float shownTabX = Float.NaN;
    private float shownTabY;
    private float shownTabWidth;
    private long lastFrame = Util.getMillis();
    private int left;
    private int top;
    private int windowWidth;
    private int windowHeight;
    private int listX;
    private int listY;
    private int listWidth;
    private int bodyHeight;
    private int detailX;
    private int detailWidth;

    AbilityGuideScreen(GameCharacter character) {
        super(Component.translatable(PREFIX + "title", character.getDisplayName()));
        this.character = character;
        this.modes = AbilityGuide.modes(character);
        this.about = new GuideAbout(character);
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; player != null && i < this.modes.size(); i++) {
            if (this.modes.get(i).active().test(player)) {
                this.tab = i;
                break;
            }
        }
        this.fill();
    }

    private void fill() {
        this.rows.clear();
        if (this.tab < this.modes.size()) {
            this.rows.add(null);
            this.rows.addAll(this.modes.get(this.tab).controls());
        }
    }

    @Override
    protected void init() {
        this.windowWidth = Math.min(this.width - 16, MAX_WIDTH);
        this.windowHeight = Math.min(this.height - 16, MAX_HEIGHT);
        this.left = (this.width - this.windowWidth) / 2;
        this.top = (this.height - this.windowHeight) / 2;
        int lines = this.about.place(this.font, this.left + 14, this.left + this.windowWidth - 14, this.top + 26);
        int count = this.modes.size();
        this.tabX = new int[count];
        this.tabY = new int[count];
        this.tabWidth = new int[count];
        int x = this.left + 10;
        int y = this.top + (lines == 0 ? 30 : 32 + 10 * lines);
        for (int i = 0; i < count; i++) {
            int width = 19 + this.font.width(this.modes.get(i).title());
            if (x > this.left + 10 && x + width > this.left + this.windowWidth - 10) {
                x = this.left + 10;
                y += TAB + TAB_GAP;
            }
            this.tabX[i] = x;
            this.tabY[i] = y;
            this.tabWidth[i] = width;
            x += width + TAB_GAP;
        }
        this.listX = this.left + 8;
        this.listY = y + TAB + 9;
        // Wide enough for every tab's longest row, name and key cap side by side, so switching tabs never moves it.
        int rows = 0;
        for (GuideMode mode : this.modes) {
            for (GuideMode.Control control : mode.controls()) {
                if (!control.heading() && control.key() != null) {
                    rows = Math.max(rows, 19 + this.font.width(mode.name(control)) + 6
                            + KeyCap.width(this.font, AbilityPanel.brief(control.key().get())) + 8);
                }
            }
        }
        this.listWidth = Mth.clamp(Math.max(Math.min((int) (this.windowWidth * 0.4F), 200), rows), 130,
                Math.max(130, this.windowWidth / 2));
        this.bodyHeight = this.top + this.windowHeight - FOOTER - this.listY;
        this.detailX = this.listX + this.listWidth + 12;
        this.detailWidth = this.left + this.windowWidth - 12 - this.detailX;
        this.selected = Mth.clamp(this.selected, 0, Math.max(0, this.rows.size() - 1));
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    private void openAbout(boolean open) {
        if (this.about.open(open)) {
            this.click(open ? 1.2F : 1.0F);
        }
    }

    private void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.3F));
        }
    }

    private boolean onFold(double mouseX, double mouseY) {
        if (this.about.open() || this.fold < 0 || mouseX < this.detailX || mouseX >= this.detailX + this.detailWidth) {
            return false;
        }
        double foldTop = this.listY - this.detailScroll + this.fold;
        return mouseY >= foldTop && mouseY < foldTop + GuideDetail.FOLD && mouseY >= this.listY
                && mouseY < this.listY + this.bodyHeight - GuideDetail.STATUS;
    }

    // Opens another tab on the same control when it has one, else on its overview.
    private void open(int index) {
        if (index < 0 || index >= this.modes.size() || index == this.tab) {
            return;
        }
        GuideMode.Control was = this.rows.isEmpty() ? null : this.rows.get(this.selected);
        this.tab = index;
        this.fill();
        int found = 0;
        for (int i = 1; was != null && i < this.rows.size(); i++) {
            if (!this.heading(i) && this.rows.get(i).id().equals(was.id())) {
                found = i;
                break;
            }
        }
        this.listScroll = 0.0;
        this.selected = -1;
        this.select(found);
        this.click(1.4F);
    }

    private boolean heading(int index) {
        GuideMode.Control row = this.rows.get(index);
        return row != null && row.heading();
    }

    private int height(int index) {
        return this.heading(index) ? HEADING : ROW;
    }

    // The next row that is no heading from `from` going `step`, or `from` when there is none.
    private int next(int from, int step) {
        for (int i = from + step; i >= 0 && i < this.rows.size(); i += step) {
            if (!this.heading(i)) {
                return i;
            }
        }
        return from;
    }

    private void select(int index) {
        if (index < 0 || index >= this.rows.size() || this.heading(index)) {
            return;
        }
        if (index != this.selected) {
            this.detailScroll = 0.0;
        }
        this.selected = index;
        int start = this.offset(index);
        if (index > 0 && this.heading(index - 1)) {
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
            y += this.height(i);
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
            if (mouseY >= rowTop && mouseY < rowTop + this.height(i)) {
                return i;
            }
        }
        return -1;
    }

    private int tabAt(double mouseX, double mouseY) {
        for (int i = 0; i < this.tabX.length; i++) {
            if (mouseX >= this.tabX[i] && mouseX < this.tabX[i] + this.tabWidth[i] && mouseY >= this.tabY[i]
                    && mouseY < this.tabY[i] + TAB) {
                return i;
            }
        }
        return -1;
    }

    static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("...")));
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
        long now = Util.getMillis();
        float seconds = Math.min(0.1F, (now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        float ease = 1.0F - (float) Math.exp(-seconds * 20.0F);

        KeyCap.Layer frame = new KeyCap.Layer(graphics, font);
        graphics.fill(this.left - 1, this.top - 1, right + 1, bottom + 1, EDGE);
        graphics.fill(this.left, this.top, right, bottom, WINDOW);
        graphics.renderOutline(this.left, this.top, this.windowWidth, this.windowHeight, RIM);
        graphics.fill(this.left, this.top, right, this.top + 2, accent);
        frame.shadowed(this.title.copy().withStyle(ChatFormatting.BOLD), this.left + 14, this.top + 11, accent);
        Component subtitle = Component.translatable(PREFIX + "subtitle");
        frame.text(subtitle, right - 14 - font.width(subtitle), this.top + 11, DIM);
        this.about.drawTop(frame, font, mouseX, mouseY, accent);
        this.tabs(frame, font, player, mouseX, mouseY, color, ease);
        graphics.fill(this.left + 8, this.listY - 5, right - 8, this.listY - 4, LINE);
        this.footer(frame, font, bottom, right);
        frame.finish();

        if (this.about.open()) {
            this.about.drawPage(graphics, font, this.listX, this.listY, right - 8 - this.listX, this.bodyHeight,
                    accent);
            return;
        }
        graphics.fill(this.listX - 1, this.listY - 1, this.listX + this.listWidth + 1, this.listY + this.bodyHeight + 1,
                EDGE);
        graphics.fill(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.bodyHeight, LIST);
        if (this.rows.isEmpty()) {
            graphics.drawString(font, Component.translatable(PREFIX + "none"), this.detailX, this.listY + 2, MUTED,
                    false);
            return;
        }
        GuideMode mode = this.modes.get(this.tab);
        this.list(graphics, font, player, mode, mouseX, mouseY, color, ease);
        GuideMode.Control chosen = this.rows.get(this.selected);
        GuideDetail.Drawn drawn = chosen == null
                ? GuideDetail.overview(graphics, font, mode, player, this.detailX, this.listY, this.detailWidth,
                        this.bodyHeight, this.detailScroll, accent)
                : GuideDetail.control(graphics, font, mode, chosen, player, this.detailX, this.listY,
                        this.detailWidth, this.bodyHeight, this.detailScroll, accent, this.more,
                        this.onFold(mouseX, mouseY));
        this.detailHeight = drawn.height();
        this.fold = drawn.fold();
        this.detailScroll = Mth.clamp(this.detailScroll, 0.0, this.maxDetailScroll());
        scrollbar(graphics, this.detailX + this.detailWidth + 4, this.listY,
                this.bodyHeight - GuideDetail.STATUS, this.detailScroll, this.maxDetailScroll());
    }

    // A pill per mode, a green dot on the one you are in, and the open one's bar sliding under it.
    private void tabs(KeyCap.Layer layer, Font font, LocalPlayer player, int mouseX, int mouseY, int color,
            float ease) {
        if (this.modes.isEmpty()) {
            return;
        }
        GuiGraphics graphics = layer.graphics();
        if (Float.isNaN(this.shownTabX)) {
            this.shownTabX = this.tabX[this.tab];
            this.shownTabY = this.tabY[this.tab];
            this.shownTabWidth = this.tabWidth[this.tab];
        } else {
            this.shownTabX += (this.tabX[this.tab] - this.shownTabX) * ease;
            this.shownTabY += (this.tabY[this.tab] - this.shownTabY) * ease;
            this.shownTabWidth += (this.tabWidth[this.tab] - this.shownTabWidth) * ease;
        }
        int hover = this.tabAt(mouseX, mouseY);
        for (int i = 0; i < this.modes.size(); i++) {
            GuideMode mode = this.modes.get(i);
            boolean open = i == this.tab;
            int x = this.tabX[i];
            int y = this.tabY[i];
            KeyCap.pill(graphics, x, y, this.tabWidth[i], TAB, EDGE);
            KeyCap.pill(graphics, x + 1, y + 1, this.tabWidth[i] - 2, TAB - 2, open
                    ? 0xFF000000 | GuiShapes.mix(TAB_FILL, color, 0.28F) : i == hover ? 0xFF3A3A3A : TAB_FILL);
            graphics.fill(x + 2, y + 1, x + this.tabWidth[i] - 2, y + 2, HOVER);
            if (mode.active().test(player)) {
                KeyCap.dot(graphics, x + 8.0F, y + TAB * 0.5F, 2.2F, GuideDetail.GREEN);
            }
            layer.shadowed(mode.title(), x + 13, y + 4, open || i == hover ? TEXT : SOFT);
        }
        GuiShapes.roundRect(graphics, this.shownTabX + 3.0F, this.shownTabY + TAB - 3.0F, this.shownTabWidth - 6.0F,
                2.0F, 0.0F, 0xFF000000 | color);
    }

    private void list(GuiGraphics graphics, Font font, LocalPlayer player, GuideMode mode, int mouseX, int mouseY,
            int color, float ease) {
        float target = this.offset(this.selected);
        this.shownY = Float.isNaN(this.shownY) ? target : this.shownY + (target - this.shownY) * ease;

        graphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.bodyHeight);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        float base = (float) (this.listY - this.listScroll);
        float shown = base + this.shownY;
        GuiShapes.roundRect(graphics, this.listX + 2, shown, this.listWidth - 4, ROW, 0.0F,
                GuiShapes.fade(color, 0.20F));
        GuiShapes.roundRect(graphics, this.listX + 2, shown, 2, ROW, 0.0F, 0xFF000000 | color);
        int hover = this.rowAt(mouseX, mouseY);
        for (int i = 0; i < this.rows.size(); i++) {
            GuideMode.Control row = this.rows.get(i);
            int y = Math.round(base + this.offset(i));
            if (y + this.height(i) < this.listY || y > this.listY + this.bodyHeight) {
                continue;
            }
            if (row != null && row.heading()) {
                layer.text(Component.literal(mode.name(row).getString().toUpperCase(Locale.ROOT)), this.listX + 9,
                        y + 5, DIM);
                continue;
            }
            if (i == hover && i != this.selected) {
                GuiShapes.roundRect(graphics, this.listX + 2, y, this.listWidth - 4, ROW, 0.0F, HOVER);
            }
            int text = i == this.selected ? TEXT : SOFT;
            if (row == null) {
                GuiShapes.ring(graphics, this.listX + 12.0F, y + ROW * 0.5F, 2.6F, 1.2F, 0xFF000000 | color);
                layer.text(Component.translatable(PREFIX + "how"), this.listX + 19, y + 4, text);
                continue;
            }
            KeyCap.dot(graphics, this.listX + 12.0F, y + ROW * 0.5F, 2.5F,
                    GuideDetail.status(mode, row, player).color());
            Component key = AbilityPanel.brief(row.key().get());
            Component name = mode.name(row);
            int cap = KeyCap.width(font, key);
            int room = this.listWidth - 19 - 8;
            // The cap stands beside the name while the name keeps at least half the row; a longer one is left to the
            // detail.
            if (cap + 6 <= room - Math.min(font.width(name), room / 2)) {
                room -= cap + 6;
                KeyCap.draw(layer, font, key, this.listX + this.listWidth - 7 - cap, y + 2, 11);
            }
            layer.sequence(fit(font, name, room), this.listX + 19, y + 4, text);
        }
        layer.finish();
        graphics.disableScissor();
        scrollbar(graphics, this.listX + this.listWidth - 4, this.listY, this.bodyHeight, this.listScroll,
                this.maxListScroll());
    }

    static void scrollbar(GuiGraphics graphics, int x, int y, int height, double scroll, double max) {
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
        if (this.modes.size() > 1) {
            x += KeyCap.draw(layer, font, Component.literal("←"), x, y, 11) + 2;
            x += KeyCap.draw(layer, font, Component.literal("→"), x, y, 11) + 4;
            Component mode = Component.translatable(PREFIX + "mode");
            layer.text(mode, x, y + 2, MUTED);
            x += font.width(mode) + 10;
        }
        x += KeyCap.draw(layer, font, Component.literal("↑"), x, y, 11) + 2;
        x += KeyCap.draw(layer, font, Component.literal("↓"), x, y, 11) + 4;
        Component choose = Component.translatable(PREFIX + "choose");
        layer.text(choose, x, y + 2, MUTED);
        x += font.width(choose) + 10;
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
        Component enter = Component.literal("Enter");
        Component more = Component.translatable(PREFIX + "details");
        if (!this.about.open() && this.fold >= 0 && x + KeyCap.width(font, enter) + 4 + font.width(more) < end - 10) {
            x += KeyCap.draw(layer, font, enter, x, y, 11) + 4;
            layer.text(more, x, y + 2, MUTED);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.about.onToggle(this.font, mouseX, mouseY)) {
                this.openAbout(!this.about.open());
                return true;
            }
            int tab = this.tabAt(mouseX, mouseY);
            if (tab >= 0) {
                this.openAbout(false);
                this.open(tab);
                return true;
            }
            if (this.about.open()) {
                return true;
            }
            if (this.onFold(mouseX, mouseY)) {
                this.unfold();
                return true;
            }
            int index = this.rowAt(mouseX, mouseY);
            if (index >= 0 && !this.heading(index)) {
                this.select(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void unfold() {
        this.more = !this.more;
        this.click(this.more ? 1.2F : 1.0F);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.about.open()) {
            this.about.scroll(-scrollY * 12.0, this.bodyHeight);
            return true;
        }
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
        if (this.about.open() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.openAbout(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            if (this.about.open()) {
                this.openAbout(false);
            } else if (this.fold >= 0) {
                this.unfold();
            }
            return true;
        }
        if (this.about.open()) {
            int page = this.bodyHeight - 24;
            double step = switch (keyCode) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> -12.0;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> 12.0;
                case GLFW.GLFW_KEY_PAGE_UP -> -page;
                case GLFW.GLFW_KEY_PAGE_DOWN -> page;
                default -> 0.0;
            };
            if (step != 0.0) {
                this.about.scroll(step, this.bodyHeight);
                return true;
            }
        }
        int count = this.modes.size();
        int tab = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> this.tab - 1;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> this.tab + 1;
            case GLFW.GLFW_KEY_TAB -> count == 0 ? -1 : Math.floorMod(this.tab + (hasShiftDown() ? -1 : 1), count);
            default -> keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9 ? keyCode - GLFW.GLFW_KEY_1 : -1;
        };
        if (tab >= 0 && tab < count) {
            this.openAbout(false);
            this.open(tab);
            return true;
        }
        if (this.about.open()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
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
