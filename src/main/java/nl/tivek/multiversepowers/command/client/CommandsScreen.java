package nl.tivek.multiversepowers.command.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.command.CommandCategories;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import org.lwjgl.glfw.GLFW;

// The settings window's Commands page: every command in one list, the built-in short commands and the world's own (at
// most 100, marked CUSTOM), under their categories or sorted by name, found by a search that suggests as the chat
// does; Run types one in the chat. A host makes, changes, renames and takes away commands and categories here.
public final class CommandsScreen extends CommandsBase {
    private static final int ROW = 22;
    private static final int GROUP = 16;
    private static final int CATEGORY_ROW = 16;

    private final ScrollArea area = new ScrollArea();

    public CommandsScreen(@Nullable Screen root) {
        super(root);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<CommandRows.Entry> all = CommandRows.all();
        if (this.searchSuggestions != null) {
            this.searchSuggestions.update(all);
        }
        this.bar(graphics, mouseX, mouseY);
        int top = this.contentY + BAR + 2;
        if (this.mode == Mode.EDIT && this.editable()) {
            this.editorCard(graphics, top);
            top += EDITOR + 4;
        }
        int bottom = this.contentY + this.contentHeight - (this.mode == Mode.CATEGORIES && this.editable() ? 34 : 12);
        ScreenAnchors.report("commands.list", this.contentX, top, this.contentWidth, bottom - top);
        Component tip = this.mode == Mode.CATEGORIES ? this.categoryList(graphics, all, top, bottom, mouseX, mouseY)
                : this.commands(graphics, all, top, bottom, mouseX, mouseY);
        if (this.mode != Mode.CATEGORIES || !this.editable()) {
            this.foot(graphics, all, bottom + 3, mouseX, mouseY);
        }
        if (tip != null && !this.suggesting()) {
            graphics.renderTooltip(this.font, this.font.split(tip, 240), mouseX, mouseY);
        }
    }

    // The game's suggestions over everything else on the page.
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.commandInput != null) {
            this.commandInput.render(graphics, mouseX, mouseY, this.windowX, this.windowX + this.windowWidth);
        }
        if (this.searchSuggestions != null) {
            this.searchSuggestions.render(graphics, this.font, mouseX, mouseY);
        }
    }

    private boolean suggesting() {
        return this.commandInput != null && this.commandInput.showing()
                || this.searchSuggestions != null && this.searchSuggestions.showing();
    }

    // Right of the search: how the list is sorted, and for a host New command and Categories.
    private void bar(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.contentY + 2;
        int x = this.contentX + this.contentWidth;
        boolean editable = this.editable();
        if (editable) {
            Component categories = Component.translatable(PREFIX + "categories");
            x -= this.font.width(categories) + 6;
            if (this.mode == Mode.CATEGORIES) {
                this.chip(graphics, categories, x, y, COLOR, 0x101010);
            } else {
                this.smallButton(graphics, categories, x, y, mouseX, mouseY, true, this::categories);
            }
            Component create = Component.translatable(PREFIX + "new");
            x -= this.font.width(create) + 6 + 4;
            this.smallButton(graphics, create, x, y, mouseX, mouseY, this.mode != Mode.EDIT, this::create);
        }
        Component sort = Component.translatable(PREFIX + "sort", Component.translatable(PREFIX + "sort."
                + this.sort.name().toLowerCase(Locale.ROOT)));
        x -= this.font.width(sort) + 6 + 4;
        int left = this.search == null ? this.contentX : this.search.getX() + this.search.getWidth() + 4;
        if (x >= left) {
            this.smallButton(graphics, sort, x, y, mouseX, mouseY, true, () -> {
                CommandRows.Sort[] all = CommandRows.Sort.values();
                this.sort = all[Math.floorMod(this.sort.ordinal() + (hasShiftDown() ? -1 : 1), all.length)];
                this.area.top();
            });
        }
    }

    // Under the list: how many custom commands there are, or what was just done; at the right a way to open or close
    // every category at once.
    private void foot(GuiGraphics graphics, List<CommandRows.Entry> all, int y, int mouseX, int mouseY) {
        int right = this.contentX + this.contentWidth;
        if (this.sort == CommandRows.Sort.CATEGORY && this.query.isBlank() && this.mode == Mode.LIST) {
            boolean anyOpen = !OPEN.isEmpty();
            Component fold = Component.translatable(PREFIX + (anyOpen ? "close_all" : "open_all"));
            right -= this.font.width(fold);
            this.link(graphics, fold, right, y, mouseX, mouseY, MUTED, () -> {
                if (anyOpen) {
                    OPEN.clear();
                } else {
                    for (CommandCategories.Category category : CommandCategories.all()) {
                        OPEN.add(category.id());
                    }
                    OPEN.add(CommandCategories.NONE);
                }
                this.area.top();
            });
            right -= 8;
        }
        long custom = all.stream().filter(CommandRows.Entry::custom).count();
        Component foot = this.status != null ? this.status
                : Component.translatable(PREFIX + "count", custom, CommandAliases.MOST, CommandCategories.all().size());
        graphics.drawString(this.font, this.fit(foot, right - this.contentX), this.contentX, y,
                this.status != null ? 0xFF000000 | this.statusColor : MUTED, false);
    }

    // The card round the editor's fields: what is being made or changed, and the names of its fields.
    private void editorCard(GuiGraphics graphics, int top) {
        card(graphics, this.contentX, top, this.contentWidth, EDITOR, COLOR);
        CommandRows.Entry entry = this.editing;
        Component title = entry == null ? Component.translatable(PREFIX + "editor.new")
                : Component.translatable(entry.custom() ? PREFIX + "editor.custom" : PREFIX + "editor.built_in",
                        "/" + entry.name());
        graphics.drawString(this.font, title.copy().withStyle(ChatFormatting.BOLD), this.contentX + 6, top + 4, TEXT,
                false);
        int x = this.contentX + 6;
        graphics.drawString(this.font, Component.translatable(PREFIX + "name"), x, top + 17, BODY, false);
        boolean builtIn = entry != null && !entry.custom();
        graphics.drawString(this.font, Component.translatable(builtIn ? PREFIX + "runs_label" : PREFIX + "command"),
                x, top + 35, BODY, false);
        if (builtIn) {
            String runs = entry.builtIn().template() == null ? Component.translatable(PREFIX + "own").getString()
                    : String.join(" , ", entry.builtIn().template().split(";"));
            graphics.drawString(this.font, this.fit(Component.literal(runs), this.contentWidth - 64), x + 52,
                    top + 35, MUTED, false);
        }
        Component hint = Component.translatable(builtIn ? PREFIX + "editor.built_in.hint" : PREFIX + "editor.hint");
        this.wrap(graphics, hint, x, top + 53, this.contentWidth - 124, DIM, 2);
    }

    // The list: a heading per category (when sorted by it) that a click folds in or out, and per command how it is
    // typed, CUSTOM on the world's own and what it does; Run, Edit, Copy and Delete (Restore on one taken away) show
    // only on the row under the mouse, so the list stays calm.
    @Nullable
    private Component commands(GuiGraphics graphics, List<CommandRows.Entry> all, int top, int bottom, int mouseX,
            int mouseY) {
        List<Object> rows = CommandRows.rows(all, this.sort, this.query, OPEN);
        if (rows.isEmpty()) {
            this.nothing(graphics, top, "config.welcomescreen.search.none");
            return null;
        }
        int height = 0;
        for (Object row : rows) {
            height += row instanceof CommandRows.Heading ? GROUP : ROW;
        }
        Component tip = null;
        int y = this.area.begin(graphics, this.contentX, top, this.contentWidth, bottom - top, height);
        int width = this.area.rowWidth();
        boolean editable = this.editable();
        for (Object row : rows) {
            if (row instanceof CommandRows.Heading heading) {
                if (this.area.shows(y, GROUP)) {
                    this.heading(graphics, heading, y, width, mouseX, mouseY);
                }
                y += GROUP;
                continue;
            }
            CommandRows.Entry entry = (CommandRows.Entry) row;
            if (this.area.shows(y, ROW)) {
                boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, ROW - 2);
                if (over) {
                    graphics.fill(this.contentX, y, this.contentX + width, y + ROW - 2, 0x1CFFFFFF);
                }
                int right = this.contentX + width - 4;
                if (over) {
                    if (entry.removed()) {
                        right = this.rowButton(graphics, PREFIX + "restore", right, y, mouseX, mouseY, editable,
                                () -> this.restore(entry));
                    } else {
                        right = this.rowButton(graphics, PREFIX + "delete", right, y, mouseX, mouseY, editable,
                                () -> this.delete(entry));
                        if (!entry.custom()) {
                            right = this.rowButton(graphics, PREFIX + "copy", right, y, mouseX, mouseY, editable,
                                    () -> this.copy(entry));
                        }
                        right = this.rowButton(graphics, PREFIX + "edit", right, y, mouseX, mouseY, editable,
                                () -> this.edit(entry));
                        right = this.rowButton(graphics, PREFIX + "run", right, y, mouseX, mouseY,
                                this.minecraft.level != null, () -> this.type(entry));
                    }
                }
                this.entry(graphics, entry, y, right - this.contentX - 8, over);
                if (over && mouseX < right) {
                    tip = this.tip(entry);
                }
            }
            y += ROW;
        }
        this.area.end(graphics);
        return tip;
    }

    // A category's heading: folded or open, its name and how many it holds; a click on it folds it in or out.
    private void heading(GuiGraphics graphics, CommandRows.Heading heading, int y, int width, int mouseX,
            int mouseY) {
        boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, GROUP - 2);
        graphics.fill(this.contentX, y + 1, this.contentX + width, y + GROUP - 1, over ? 0x26FFFFFF : 0x14FFFFFF);
        int color = CommandRows.REMOVED.equals(heading.category()) ? MUTED : 0xFF000000 | COLOR;
        graphics.drawString(this.font, heading.open() ? "▾" : "▸", this.contentX + 4, y + 4, color, false);
        Component text = Component.empty().append(heading.label().copy().withStyle(ChatFormatting.BOLD))
                .append(Component.literal("  " + heading.count()).withStyle(ChatFormatting.DARK_GRAY));
        graphics.drawString(this.font, this.fit(text, width - 20), this.contentX + 14, y + 4, color, false);
        if (this.query.isBlank()) {
            this.area.hit(this.contentX, y, width, GROUP - 2, () -> {
                if (!OPEN.remove(heading.category())) {
                    OPEN.add(heading.category());
                }
            });
        }
    }

    // How it is typed, CUSTOM on the world's own, and under it what it does (with its old name if renamed).
    private void entry(GuiGraphics graphics, CommandRows.Entry entry, int y, int room, boolean over) {
        int x = this.contentX + 4;
        Component tag = Component.translatable(PREFIX + "custom_tag");
        int tagWidth = entry.custom() ? this.font.width(tag) + 10 : 0;
        Component usage = Component.literal(entry.usage());
        int usageWidth = Math.min(this.font.width(usage), room - tagWidth);
        graphics.drawString(this.font, this.fit(usage, room - tagWidth), x, y + 2, over ? TEXT : 0xFFFFE2C2, false);
        if (entry.custom()) {
            this.chip(graphics, tag, x + usageWidth + 4, y + 1, CUSTOM & 0xFFFFFF, 0x101010);
        }
        Component about = entry.renamed() ? Component.translatable(PREFIX + "was", "/" + entry.key())
                .append(" · ").append(entry.about()) : entry.about();
        graphics.drawString(this.font, this.fit(about, room), x, y + 11, MUTED, false);
    }

    private Component tip(CommandRows.Entry entry) {
        Component runs;
        if (entry.custom()) {
            runs = Component.translatable(PREFIX + "runs", entry.command());
        } else if (entry.builtIn().template() != null) {
            runs = Component.translatable(PREFIX + "runs", String.join(" , ", entry.builtIn().template().split(";")));
        } else {
            runs = Component.translatable(PREFIX + "own");
        }
        return Component.empty().append(runs).append("\n").append(Component.translatable(PREFIX + "category.is",
                CommandRows.label(entry.category())).withStyle(ChatFormatting.GRAY));
    }

    // The categories: each with how many commands it holds, Show, and for a host Rename and Delete (a built-in one
    // also Reset); the built-in ones taken away, to bring back.
    @Nullable
    private Component categoryList(GuiGraphics graphics, List<CommandRows.Entry> all, int top, int bottom, int mouseX,
            int mouseY) {
        Map<String, Integer> counts = CommandRows.counts(all);
        List<CommandCategories.Category> categories = CommandCategories.all();
        List<String> takenAway = new ArrayList<>();
        for (String id : CommandCategories.builtIn()) {
            if (CommandCategories.find(id) == null) {
                takenAway.add(id);
            }
        }
        long removed = all.stream().filter(CommandRows.Entry::removed).count();
        int height = (categories.size() + (removed > 0 ? 1 : 0)) * CATEGORY_ROW + (takenAway.isEmpty() ? 0
                : GROUP + takenAway.size() * CATEGORY_ROW);
        int y = this.area.begin(graphics, this.contentX, top, this.contentWidth, bottom - top, height);
        int width = this.area.rowWidth();
        boolean editable = this.editable();
        if (removed > 0) {
            if (this.area.shows(y, CATEGORY_ROW)) {
                graphics.fill(this.contentX, y, this.contentX + width, y + CATEGORY_ROW - 2, 0x10FFFFFF);
                int right = this.rowButtonAt(graphics, PREFIX + "categories.show", this.contentX + width - 4, y + 2,
                        mouseX, mouseY, true, () -> {
                            OPEN.add(CommandRows.REMOVED);
                            this.list();
                        });
                graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "removed_count", removed),
                        right - this.contentX - 8), this.contentX + 4, y + 3, MUTED, false);
            }
            y += CATEGORY_ROW;
        }
        for (CommandCategories.Category category : categories) {
            if (this.area.shows(y, CATEGORY_ROW)) {
                boolean renaming = category.id().equals(this.renaming);
                graphics.fill(this.contentX, y, this.contentX + width, y + CATEGORY_ROW - 2, renaming ? 0x30FFB86B
                        : 0x10FFFFFF);
                int right = this.contentX + width - 4;
                int buttonY = y + 2;
                right = this.rowButtonAt(graphics, PREFIX + "delete", right, buttonY, mouseX, mouseY, editable,
                        () -> this.deleteCategory(category));
                if (category.builtIn() && category.label() != null) {
                    right = this.rowButtonAt(graphics, PREFIX + "categories.reset", right, buttonY, mouseX, mouseY,
                            editable, () -> this.resetCategory(category.id()));
                }
                right = this.rowButtonAt(graphics, PREFIX + "categories.rename", right, buttonY, mouseX, mouseY,
                        editable, () -> this.rename(category));
                right = this.rowButtonAt(graphics, PREFIX + "categories.show", right, buttonY, mouseX, mouseY, true,
                        () -> {
                            OPEN.add(category.id());
                            this.list();
                        });
                Component text = Component.empty().append(CommandRows.label(category.id())).append(Component.literal(
                        "  " + counts.getOrDefault(category.id(), 0)).withStyle(ChatFormatting.DARK_GRAY));
                if (category.builtIn()) {
                    text = text.copy().append(Component.literal("  ")).append(Component.translatable(
                            PREFIX + "categories.built_in").withStyle(ChatFormatting.DARK_GRAY));
                }
                graphics.drawString(this.font, this.fit(text, right - this.contentX - 8), this.contentX + 4, y + 3,
                        BODY, false);
            }
            y += CATEGORY_ROW;
        }
        if (!takenAway.isEmpty()) {
            if (this.area.shows(y, GROUP)) {
                graphics.drawString(this.font, Component.translatable(PREFIX + "categories.taken_away")
                        .withStyle(ChatFormatting.BOLD), this.contentX + 2, y + 5, 0xFF000000 | COLOR, false);
            }
            y += GROUP;
            for (String id : takenAway) {
                if (this.area.shows(y, CATEGORY_ROW)) {
                    graphics.fill(this.contentX, y, this.contentX + width, y + CATEGORY_ROW - 2, 0x10FFFFFF);
                    int right = this.rowButtonAt(graphics, PREFIX + "restore", this.contentX + width - 4, y + 2,
                            mouseX, mouseY, editable, () -> this.resetCategory(id));
                    graphics.drawString(this.font, this.fit(Component.translatable(CommandRows.COMMAND + "group."
                            + id), right - this.contentX - 8), this.contentX + 4, y + 3, MUTED, false);
                }
                y += CATEGORY_ROW;
            }
        }
        this.area.end(graphics);
        return null;
    }

    // A small button at the right of a row, left of `right`; returns its own left edge.
    private int rowButton(GuiGraphics graphics, String key, int right, int y, int mouseX, int mouseY, boolean active,
            Runnable action) {
        return this.rowButtonAt(graphics, key, right, y + 5, mouseX, mouseY, active, action);
    }

    private int rowButtonAt(GuiGraphics graphics, String key, int right, int y, int mouseX, int mouseY,
            boolean active, Runnable action) {
        Component text = Component.translatable(key);
        int width = this.font.width(text) + 6;
        int x = right - width;
        boolean over = active && this.area.over(mouseX, mouseY, x, y, width, 10);
        this.chip(graphics, text, x, y, over ? 0x505050 : 0x343434, !active ? 0x6A6A6A : over ? 0xFFFFFF : 0xC8C8C8);
        if (active) {
            this.area.hit(x, y, width, 10, action);
        }
        return x - 4;
    }

    private void nothing(GuiGraphics graphics, int top, String key) {
        Component none = Component.translatable(key);
        graphics.drawString(this.font, none, this.contentX + (this.contentWidth - this.font.width(none)) / 2, top + 10,
                MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandInput != null && this.commandInput.mouseClicked(mouseX, mouseY, button)
                || this.searchSuggestions != null && this.searchSuggestions.mouseClicked(mouseX, mouseY, this.font)) {
            return true;
        }
        if (button == 0 && this.area.press(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.area.click(mouseX, mouseY)) {
            this.click(1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.area.drag(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.area.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.commandInput != null && this.commandInput.mouseScrolled(scrollY)
                || this.searchSuggestions != null && this.searchSuggestions.mouseScrolled(scrollY)) {
            return true;
        }
        this.area.scroll(scrollY, ROW);
        return true;
    }

    // The suggestions take Tab, the arrows and Enter first; then Enter in the editor saves and in the label box adds,
    // and Escape leaves the editor or the categories before it closes the window.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandInput != null && this.commandInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.search != null && this.search.isFocused() && this.searchSuggestions != null
                && this.searchSuggestions.keyPressed(keyCode)) {
            return true;
        }
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (enter && this.nameBox != null && (this.getFocused() == this.nameBox
                || this.getFocused() == this.commandBox)) {
            this.save();
            return true;
        }
        if (enter && this.labelBox != null && this.getFocused() == this.labelBox) {
            this.saveCategory();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.mode != Mode.LIST) {
            this.list();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
