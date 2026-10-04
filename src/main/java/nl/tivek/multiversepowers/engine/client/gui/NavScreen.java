package nl.tivek.multiversepowers.engine.client.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

// A window over the game's own menu background: a title bar, a column of pages on the left and the open page on the
// right. Every page is a screen of its own built on this, opened in place of the last, so the window stays put and only
// its right side changes; the highlight in the column glides from the page left to the one opened. Closing returns to
// `root`. Pages switch with a click, Ctrl+Tab, or their number while no text field has the keys.
public abstract class NavScreen extends Screen {
    public static final int ACCENT = 0x6EE7A0;
    protected static final int TEXT = 0xFFFFFFFF;
    protected static final int BODY = 0xFFD2D2D2;
    protected static final int MUTED = 0xFF8E8E8E;
    protected static final int DIM = 0xFF5E5E5E;
    public static final int WARN = 0xF2C84B;
    protected static final int ERROR = 0xFF7B7B;
    private static final int EDGE = 0xFF000000;
    private static final int FILL = 0xEE121212;
    private static final int BAR = 0xFF1B1B1B;
    private static final int SIDE = 0x70000000;
    private static final int RIM = 0x1CFFFFFF;
    private static final int HOVER = 0x14FFFFFF;
    private static final int PICKED = 0x26FFFFFF;
    private static final int MARGIN = 8;
    private static final int MAX_WIDTH = 520;
    private static final int MAX_HEIGHT = 304;
    protected static final int TITLE_BAR = 20;
    private static final int ITEM = 20;
    private static final int GAP = 9;
    private static final int COMPACT = 26;
    protected static final int PAD = 10;
    private static final int HEADER = 28;
    private static final int BARE_HEADER = 18;
    private static final int TAB = 14;
    private static final int MIN_CONTENT = 250;
    private static final float GLIDE = 18.0F;

    // Where the highlight was drawn last, kept from page to page so it glides to the one opened.
    private static float shownPick = Float.NaN;
    private static long lastFrame;

    public record Item(String id, Component label, PixelIcons.Icon icon, Runnable open, boolean gap) {
    }

    // A small mark on an item: a dot when `text` is null, else a chip.
    public record Badge(@Nullable Component text, int rgb) {
    }

    // One part of a page, switched to at the right of its title; `count` is a chip after its name.
    public record Tab(String id, Component label, @Nullable Component count, boolean picked, Runnable open) {
    }

    // Text drawn this frame that does something when clicked.
    private record Link(int x, int y, int width, int height, Runnable action) {
    }

    @Nullable
    protected final Screen root;
    private final String page;
    private final List<Link> links = new ArrayList<>();
    private List<Item> items = List.of();
    protected int windowX;
    protected int windowY;
    protected int windowWidth;
    protected int windowHeight;
    protected int sideWidth;
    // The page's own room, under its title.
    protected int contentX;
    protected int contentY;
    protected int contentWidth;
    protected int contentHeight;
    private boolean compact;

    protected NavScreen(Component title, @Nullable Screen root, String page) {
        super(title);
        this.root = root;
        this.page = page;
    }

    protected abstract List<Item> items();

    protected abstract Component brand();

    protected abstract PixelIcons.Icon brandIcon();

    // A chip at the right of the title bar, such as a version, and what clicking it does.
    @Nullable
    protected Component brandTag() {
        return null;
    }

    protected void clickedBrandTag() {
    }

    @Nullable
    protected Badge badge(String id) {
        return null;
    }

    @Nullable
    protected Component subtitle() {
        return null;
    }

    // The parts this page is made of, if any.
    protected List<Tab> tabs() {
        return List.of();
    }

    protected abstract void initPage();

    // Under the page's widgets.
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    // Over the page's widgets.
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected final void init() {
        this.items = this.items();
        this.windowWidth = Math.min(this.width - MARGIN * 2, MAX_WIDTH);
        this.windowHeight = Math.min(this.height - MARGIN * 2, MAX_HEIGHT);
        this.windowX = (this.width - this.windowWidth) / 2;
        this.windowY = (this.height - this.windowHeight) / 2;
        int widest = 0;
        for (Item item : this.items) {
            widest = Math.max(widest, this.font.width(item.label()));
        }
        this.sideWidth = widest + 36;
        this.compact = this.windowWidth - this.sideWidth - PAD * 2 < MIN_CONTENT;
        if (this.compact) {
            this.sideWidth = COMPACT;
        }
        this.contentX = this.windowX + this.sideWidth + PAD;
        this.contentWidth = this.windowX + this.windowWidth - PAD - this.contentX;
        this.contentY = this.windowY + TITLE_BAR + 8 + (this.subtitle() == null ? BARE_HEADER : HEADER);
        this.contentHeight = this.windowY + this.windowHeight - 8 - this.contentY;
        this.initPage();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int right = this.windowX + this.windowWidth;
        int bottom = this.windowY + this.windowHeight;
        graphics.fill(this.windowX - 1, this.windowY - 1, right + 1, bottom + 1, EDGE);
        graphics.fill(this.windowX, this.windowY, right, bottom, FILL);
        this.titleBar(graphics, mouseX, mouseY);
        this.sidebar(graphics, mouseX, mouseY);
        int top = this.windowY + TITLE_BAR + 8;
        int tabs = this.tabs(graphics, top - 3, mouseX, mouseY);
        graphics.drawString(this.font, fit(this.title.copy().withStyle(ChatFormatting.BOLD),
                this.contentWidth - (tabs > 0 ? tabs + 8 : 0)), this.contentX, top, TEXT, true);
        Component subtitle = this.subtitle();
        if (subtitle != null) {
            graphics.drawString(this.font, fit(subtitle, this.contentWidth), this.contentX, top + 12, MUTED, false);
        }
        graphics.fill(this.contentX, this.contentY - 5, this.contentX + this.contentWidth, this.contentY - 4, RIM);
        ScreenAnchors.report("window", this.windowX, this.windowY, this.windowWidth, this.windowHeight);
        this.renderPageBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.links.clear();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderPage(graphics, mouseX, mouseY, partialTick);
    }

    private void titleBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int right = this.windowX + this.windowWidth;
        graphics.fill(this.windowX, this.windowY, right, this.windowY + TITLE_BAR, BAR);
        graphics.fill(this.windowX, this.windowY, right, this.windowY + 1, RIM);
        graphics.fill(this.windowX, this.windowY + TITLE_BAR - 1, right, this.windowY + TITLE_BAR, 0x50000000);
        graphics.fill(this.windowX, this.windowY + 1, this.windowX + 2, this.windowY + TITLE_BAR - 1, 0xFF000000 | ACCENT);
        PixelIcons.draw(graphics, this.brandIcon(), this.windowX + 7, this.windowY + 4, 1, 0xFF000000 | ACCENT, true);
        graphics.drawString(this.font, this.brand(), this.windowX + 24, this.windowY + 6, TEXT, true);
        int closeX = right - 17;
        boolean overClose = this.overClose(mouseX, mouseY);
        if (overClose) {
            graphics.fill(closeX - 2, this.windowY + 2, closeX + 14, this.windowY + TITLE_BAR - 2, 0x30FFFFFF);
        }
        PixelIcons.draw(graphics, PixelIcons.Icon.CLOSE, closeX, this.windowY + 4, 1, overClose ? TEXT : 0xFFB0B0B0,
                false);
        ScreenAnchors.report("close", closeX - 2, this.windowY + 2, 16, TITLE_BAR - 4);
        Component tag = this.brandTag();
        int free = closeX - 2;
        if (tag != null) {
            int width = this.font.width(tag) + 8;
            int x = closeX - 8 - width;
            boolean over = mouseX >= x && mouseX < x + width && mouseY >= this.windowY + 4
                    && mouseY < this.windowY + 16;
            chip(graphics, tag, x, this.windowY + 5, over ? 0x505050 : 0x343434, over ? 0xFFFFFF : 0xC8C8C8);
            this.links.add(new Link(x, this.windowY + 4, width, 12, this::clickedBrandTag));
            ScreenAnchors.report("window.tag", x, this.windowY + 4, width, 12);
            free = x;
        }
        // The bar's free middle, between the brand and the tag, for what lies on it.
        int left = this.windowX + 24 + this.font.width(this.brand()) + 6;
        ScreenAnchors.report("window.bar", left, this.windowY, Math.max(0, free - 6 - left), TITLE_BAR);
    }

    private void sidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = this.windowY + TITLE_BAR;
        int bottom = this.windowY + this.windowHeight;
        graphics.fill(this.windowX, top, this.windowX + this.sideWidth, bottom, SIDE);
        graphics.fill(this.windowX + this.sideWidth, top, this.windowX + this.sideWidth + 1, bottom, RIM);
        ScreenAnchors.report("nav", this.windowX, top, this.sideWidth, bottom - top);
        long now = Util.getMillis();
        float seconds = Math.min(0.1F, Math.max(0.0F, (now - lastFrame) / 1000.0F));
        lastFrame = now;
        int pickY = -1;
        int y = top + 6;
        for (Item item : this.items) {
            if (item.gap()) {
                graphics.fill(this.windowX + 8, y + GAP / 2, this.windowX + this.sideWidth - 8, y + GAP / 2 + 1, RIM);
                y += GAP;
            }
            if (item.id().equals(this.page)) {
                pickY = y;
            }
            y += ITEM;
        }
        if (pickY >= 0) {
            shownPick = Float.isNaN(shownPick) || Math.abs(shownPick - pickY) > 400.0F ? pickY
                    : shownPick + (pickY - shownPick) * (1.0F - (float) Math.exp(-GLIDE * seconds));
            int shown = Math.round(shownPick);
            graphics.fill(this.windowX, shown, this.windowX + this.sideWidth, shown + ITEM, PICKED);
            graphics.fill(this.windowX, shown, this.windowX + 2, shown + ITEM, 0xFF000000 | ACCENT);
        }
        y = top + 6;
        for (Item item : this.items) {
            if (item.gap()) {
                y += GAP;
            }
            boolean picked = item.id().equals(this.page);
            boolean over = this.over(item, y, mouseX, mouseY);
            if (over && !picked) {
                graphics.fill(this.windowX, y, this.windowX + this.sideWidth, y + ITEM, HOVER);
            }
            int iconColor = picked ? 0xFF000000 | ACCENT : over ? TEXT : 0xFFA8A8A8;
            PixelIcons.draw(graphics, item.icon(), this.windowX + 7, y + 4, 1, iconColor, false);
            if (!this.compact) {
                graphics.drawString(this.font, item.label(), this.windowX + 25, y + 6, picked || over ? TEXT : BODY,
                        picked);
            } else if (over) {
                this.setTooltipForNextRenderPass(item.label());
            }
            Badge badge = this.badge(item.id());
            if (badge != null) {
                this.drawBadge(graphics, badge, y);
            }
            ScreenAnchors.report("nav." + item.id(), this.windowX, y, this.sideWidth, ITEM);
            y += ITEM;
        }
    }

    // The page's parts side by side at the right of its title, the open one lit; returns how wide they are.
    private int tabs(GuiGraphics graphics, int y, int mouseX, int mouseY) {
        List<Tab> tabs = this.tabs();
        int total = 0;
        for (Tab tab : tabs) {
            total += this.tabWidth(tab);
        }
        int right = this.contentX + this.contentWidth;
        int x = right - total;
        if (!tabs.isEmpty()) {
            graphics.fill(x, y, right, y + TAB, 0x50000000);
            graphics.fill(x, y + TAB - 1, right, y + TAB, RIM);
        }
        for (Tab tab : tabs) {
            int width = this.tabWidth(tab);
            boolean over = !tab.picked() && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + TAB;
            if (tab.picked()) {
                graphics.fill(x, y, x + width, y + TAB, PICKED);
                graphics.fill(x, y + TAB - 1, x + width, y + TAB, 0xFF000000 | ACCENT);
            } else if (over) {
                graphics.fill(x, y, x + width, y + TAB, HOVER);
            }
            graphics.drawString(this.font, tab.label(), x + 6, y + 3, tab.picked() || over ? TEXT : BODY, false);
            if (tab.count() != null) {
                chip(graphics, tab.count(), x + 10 + this.font.width(tab.label()), y + 2, 0x4A4A4A, 0xE0E0E0);
            }
            if (!tab.picked()) {
                this.links.add(new Link(x, y, width, TAB, tab.open()));
            }
            ScreenAnchors.report("tab." + tab.id(), x, y, width, TAB);
            x += width;
        }
        return total;
    }

    private int tabWidth(Tab tab) {
        int width = this.font.width(tab.label()) + 12;
        return tab.count() == null ? width : width + this.font.width(tab.count()) + 10;
    }

    private void drawBadge(GuiGraphics graphics, Badge badge, int y) {
        int right = this.windowX + this.sideWidth - 7;
        if (badge.text() == null || this.compact) {
            int x = this.compact ? this.windowX + 17 : right - 4;
            int top = this.compact ? y + 3 : y + 8;
            graphics.fill(x - 1, top - 1, x + 5, top + 5, 0xFF000000);
            graphics.fill(x, top, x + 4, top + 4, 0xFF000000 | badge.rgb());
            return;
        }
        int width = this.font.width(badge.text()) + 6;
        chip(graphics, badge.text(), right - width, y + 5, badge.rgb(), 0x101010);
    }

    private boolean over(Item item, int y, double mouseX, double mouseY) {
        return mouseX >= this.windowX && mouseX < this.windowX + this.sideWidth && mouseY >= y && mouseY < y + ITEM;
    }

    private boolean overClose(double mouseX, double mouseY) {
        int closeX = this.windowX + this.windowWidth - 17;
        return mouseX >= closeX - 2 && mouseX < closeX + 14 && mouseY >= this.windowY + 2
                && mouseY < this.windowY + TITLE_BAR - 2;
    }

    @Nullable
    private Item itemAt(double mouseX, double mouseY) {
        int y = this.windowY + TITLE_BAR + 6;
        for (Item item : this.items) {
            if (item.gap()) {
                y += GAP;
            }
            if (this.over(item, y, mouseX, mouseY)) {
                return item;
            }
            y += ITEM;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (this.overClose(mouseX, mouseY)) {
                this.click(1.0F);
                this.onClose();
                return true;
            }
            Item item = this.itemAt(mouseX, mouseY);
            if (item != null) {
                this.openItem(item);
                return true;
            }
            for (Link link : List.copyOf(this.links)) {
                if (mouseX >= link.x() && mouseX < link.x() + link.width() && mouseY >= link.y()
                        && mouseY < link.y() + link.height()) {
                    this.click(1.0F);
                    link.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openItem(Item item) {
        this.click(item.id().equals(this.page) ? 0.9F : 1.15F);
        if (!item.id().equals(this.page) || item.gap()) {
            item.open().run();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = this.getFocused() instanceof EditBox || this.getFocused() instanceof MultiLineEditBox;
        if (keyCode == GLFW.GLFW_KEY_TAB && hasControlDown()) {
            this.step(hasShiftDown() ? -1 : 1);
            return true;
        }
        if (!typing && modifiers == 0 && keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < this.items.size()) {
                this.openItem(this.items.get(index));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // The next page that is no action of its own (`gap` items, such as a tour, are left out).
    private void step(int direction) {
        List<Item> pages = this.items.stream().filter(item -> !item.gap()).toList();
        int at = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id().equals(this.page)) {
                at = i;
            }
        }
        this.openItem(pages.get(Math.floorMod(at + direction, pages.size())));
    }

    protected void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.4F));
        }
    }

    // Whether a text field has the keys, so letters and numbers go there.
    protected boolean typing() {
        GuiEventListener focused = this.getFocused();
        return focused instanceof EditBox || focused instanceof MultiLineEditBox;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.root);
        }
    }

    // A sunken box with a stripe of `accent` down its left side (0 for none).
    public static void card(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, 0x5A000000);
        graphics.fill(x, y, x + width, y + 1, 0x70000000);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x12FFFFFF);
        if (accent != 0) {
            graphics.fill(x, y, x + 2, y + height, 0xFF000000 | accent);
        }
    }

    // A small tag of text with cut corners, 10 high; returns its width.
    protected int chip(GuiGraphics graphics, Component text, int x, int y, int fill, int color) {
        int width = this.font.width(text) + 6;
        int argb = 0xFF000000 | fill;
        graphics.fill(x + 1, y, x + width - 1, y + 10, argb);
        graphics.fill(x, y + 1, x + width, y + 9, argb);
        graphics.drawString(this.font, text, x + 3, y + 1, 0xFF000000 | color, false);
        return width;
    }

    // A thin progress bar with a black edge.
    public static void bar(GuiGraphics graphics, int x, int y, int width, float progress, int rgb) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xFF000000);
        graphics.fill(x, y, x + width, y + 3, 0xFF2A2A2A);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 3, 0xFF000000 | rgb);
            graphics.fill(x, y, x + filled, y + 1, 0x40FFFFFF);
        }
    }

    protected static void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, RIM);
    }

    // Lines of `text` from `y`, at most `maxLines`; returns the y under the last.
    protected int wrap(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            graphics.drawString(this.font, lines.get(i), x, y, color, false);
            y += 10;
        }
        return y;
    }

    // Clickable text, underlined while the pointer is on it; returns its width.
    protected int link(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, int color,
            Runnable action) {
        int width = this.font.width(text);
        boolean over = mouseX >= x - 1 && mouseX < x + width + 1 && mouseY >= y - 2 && mouseY < y + 10;
        graphics.drawString(this.font, over ? text.copy().withStyle(ChatFormatting.UNDERLINE) : text, x, y,
                over ? TEXT : color, false);
        this.links.add(new Link(x - 1, y - 2, width + 2, 12, action));
        return width;
    }

    // `text` cut to `width` with "..." at its end, each part keeping its own colour.
    protected FormattedCharSequence fit(Component text, int width) {
        if (this.font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText cut = this.font.substrByWidth(text, Math.max(0, width - this.font.width("...")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...",
                text.getStyle())));
    }

    protected static Tooltip tip(Component text) {
        return Tooltip.create(text);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
