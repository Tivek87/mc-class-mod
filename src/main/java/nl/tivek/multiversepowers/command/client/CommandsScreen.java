package nl.tivek.multiversepowers.command.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.command.CommandAliasEditPayload;
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.command.ShortCommand;
import nl.tivek.multiversepowers.command.ShortCommands;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import org.lwjgl.glfw.GLFW;

// The settings window's Commands page: every short command, searchable, under its group, and a click types it in the
// chat; and the world's own (`CommandAliases`), at most 25, each a name for one of the game's commands, added, changed
// and taken away here.
public final class CommandsScreen extends NavScreen {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".commands.";
    private static final String COMMAND = "command." + MultiversePowers.MODID + ".";
    private static final int COLOR = 0xFFB86B;
    private static final int ROW = 22;
    private static final int GROUP = 16;
    private static final int BAR = 18;

    private enum Tab {
        BUILT_IN,
        YOURS
    }

    private final ScrollArea area = new ScrollArea();
    private Tab tab = Tab.BUILT_IN;
    private String query = "";
    private String typedName = "";
    private String typedCommand = "";
    @Nullable
    private Component status;
    private int statusColor;
    @Nullable
    private EditBox name;
    @Nullable
    private EditBox command;

    public CommandsScreen(@Nullable Screen root) {
        super(Component.translatable("config." + MultiversePowers.MODID + ".commands").withColor(COLOR), root,
                SettingsPages.COMMANDS);
    }

    @Override
    protected List<Item> items() {
        return SettingsScreen.sidebar(SettingsPages.all(),
                page -> this.minecraft.setScreen(SettingsScreen.create(this.root, page.id())));
    }

    @Override
    protected Component brand() {
        return Component.translatable("config." + MultiversePowers.MODID + ".brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.GEAR;
    }

    @Override
    protected int accent() {
        return COLOR;
    }

    @Override
    protected Component subtitle() {
        if (!this.editable()) {
            return Component.translatable("config." + MultiversePowers.MODID + ".locked").withColor(WARN);
        }
        return Component.translatable(PREFIX + "about");
    }

    private boolean editable() {
        return SettingsPages.worldEditable(CommandAliases.SPEC);
    }

    // The search on the bar over the list; on Yours, under it, a name, its command and Save.
    @Override
    protected void initPage() {
        int width = Math.min(160, this.contentWidth / 2);
        EditBox search = new EditBox(this.font, this.contentX, this.contentY, width, 14,
                Component.translatable(PREFIX + "search"));
        search.setHint(Component.translatable(PREFIX + "search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(40);
        search.setValue(this.query);
        search.setResponder(text -> {
            this.query = text;
            this.area.top();
        });
        this.addRenderableWidget(search);
        this.name = null;
        this.command = null;
        if (this.tab != Tab.YOURS || !this.editable()) {
            return;
        }
        int y = this.contentY + BAR;
        this.name = this.addRenderableWidget(new EditBox(this.font, this.contentX, y, 70, 16,
                Component.translatable(PREFIX + "name")));
        this.name.setHint(Component.translatable(PREFIX + "name").withStyle(ChatFormatting.DARK_GRAY));
        this.name.setMaxLength(25);
        this.name.setValue(this.typedName);
        this.name.setResponder(text -> this.typedName = text);
        int save = 46;
        this.command = this.addRenderableWidget(new EditBox(this.font, this.contentX + 74, y,
                this.contentWidth - 74 - save - 4, 16, Component.translatable(PREFIX + "command")));
        this.command.setHint(Component.translatable(PREFIX + "command").withStyle(ChatFormatting.DARK_GRAY));
        this.command.setMaxLength(CommandAliases.LONGEST + 1);
        this.command.setValue(this.typedCommand);
        this.command.setResponder(text -> this.typedCommand = text);
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"), button -> this.save())
                .bounds(this.contentX + this.contentWidth - save, y - 1, save, 18).build());
    }

    private void show(Tab tab) {
        this.tab = tab;
        this.query = "";
        this.status = null;
        this.area.top();
        this.rebuildWidgets();
    }

    private void save() {
        String alias = CommandAliases.strip(this.typedName).toLowerCase(Locale.ROOT);
        String line = CommandAliases.strip(this.typedCommand);
        String refused = this.refusal(alias, line);
        if (refused != null) {
            this.say(Component.translatable(PREFIX + "error." + refused, alias), ERROR);
            return;
        }
        PacketDistributor.sendToServer(new CommandAliasEditPayload(alias, line));
        this.say(Component.translatable(PREFIX + "sent", alias), ACCENT);
        this.typedName = "";
        this.typedCommand = "";
        this.rebuildWidgets();
    }

    private void remove(CommandAliases.Alias alias) {
        PacketDistributor.sendToServer(new CommandAliasEditPayload(alias.name(), ""));
        this.say(Component.translatable(PREFIX + "removed", alias.name()), ACCENT);
    }

    private void edit(CommandAliases.Alias alias) {
        this.typedName = alias.name();
        this.typedCommand = alias.command();
        this.status = null;
        this.rebuildWidgets();
    }

    private void say(Component text, int color) {
        this.status = text;
        this.statusColor = color;
    }

    // As the server checks it again: a key under `<prefix>error.`, or null.
    @Nullable
    private String refusal(String alias, String line) {
        if (!CommandAliases.NAME.matcher(alias).matches()) {
            return "bad_name";
        }
        if (line.isEmpty()) {
            return "empty";
        }
        if (line.length() > CommandAliases.LONGEST) {
            return "too_long";
        }
        List<CommandAliases.Alias> all = CommandAliases.all();
        boolean known = all.stream().anyMatch(other -> other.name().equals(alias));
        boolean taken = ShortCommands.find(alias) != null || this.minecraft.getConnection() != null
                && this.minecraft.getConnection().getCommands().getRoot().getChild(alias) != null;
        if (taken && !known) {
            return "taken";
        }
        String first = line.split(" ", 2)[0];
        for (CommandAliases.Alias other : all) {
            if (!other.name().equals(alias) && (other.name().equals(first)
                    || other.command().split(" ", 2)[0].equals(alias))) {
                return "loop";
            }
        }
        if (first.equals(alias)) {
            return "loop";
        }
        return !known && all.size() >= CommandAliases.MOST ? "full" : null;
    }

    private boolean matches(String... texts) {
        String wanted = this.query.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return true;
        }
        for (String text : texts) {
            if (text != null && text.toLowerCase(Locale.ROOT).contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.tabs(graphics, mouseX, mouseY);
        int top = this.contentY + (this.name != null ? BAR * 2 : BAR) + 2;
        int bottom = this.contentY + this.contentHeight - (this.tab == Tab.YOURS ? 12 : 0);
        ScreenAnchors.report("commands.list", this.contentX, top, this.contentWidth, bottom - top);
        Component tip = this.tab == Tab.BUILT_IN ? this.builtIn(graphics, top, bottom, mouseX, mouseY)
                : this.yours(graphics, top, bottom, mouseX, mouseY);
        if (this.tab == Tab.YOURS) {
            Component foot = this.status != null ? this.status
                    : Component.translatable(PREFIX + "count", CommandAliases.all().size(), CommandAliases.MOST);
            graphics.drawString(this.font, this.fit(foot, this.contentWidth), this.contentX, bottom + 3,
                    this.status != null ? 0xFF000000 | this.statusColor : MUTED, false);
        }
        if (tip != null) {
            graphics.renderTooltip(this.font, this.font.split(tip, 240), mouseX, mouseY);
        }
    }

    // Built-in and Yours at the right of the search, the open one lit.
    private void tabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.contentY + 2;
        int x = this.contentX + this.contentWidth;
        Component yours = Component.translatable(PREFIX + "tab.yours", CommandAliases.all().size());
        Component builtIn = Component.translatable(PREFIX + "tab.built_in", ShortCommands.all().size());
        x -= this.font.width(yours) + 6;
        this.tab(graphics, yours, x, y, mouseX, mouseY, Tab.YOURS);
        x -= this.font.width(builtIn) + 6 + 4;
        this.tab(graphics, builtIn, x, y, mouseX, mouseY, Tab.BUILT_IN);
    }

    private void tab(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, Tab tab) {
        if (this.tab == tab) {
            this.chip(graphics, text, x, y, COLOR, 0x101010);
        } else {
            this.smallButton(graphics, text, x, y, mouseX, mouseY, true, () -> this.show(tab));
        }
    }

    @Nullable
    private Component builtIn(GuiGraphics graphics, int top, int bottom, int mouseX, int mouseY) {
        List<Object> rows = new ArrayList<>();
        ShortCommand.Group group = null;
        int height = 0;
        for (ShortCommand command : ShortCommands.all()) {
            Component what = Component.translatable(COMMAND + command.name());
            Component under = Component.translatable(COMMAND + "group." + command.group().name()
                    .toLowerCase(Locale.ROOT));
            if (!this.matches(command.usage(), what.getString(), under.getString(), command.template())) {
                continue;
            }
            if (command.group() != group) {
                group = command.group();
                rows.add(under);
                height += GROUP;
            }
            rows.add(command);
            height += ROW;
        }
        if (rows.isEmpty()) {
            this.nothing(graphics, top);
            return null;
        }
        Component tip = null;
        int y = this.area.begin(graphics, this.contentX, top, this.contentWidth, bottom - top, height);
        int width = this.area.rowWidth();
        for (Object row : rows) {
            if (row instanceof Component heading) {
                if (this.area.shows(y, GROUP)) {
                    graphics.drawString(this.font, heading.copy().withStyle(ChatFormatting.BOLD), this.contentX + 2,
                            y + 5, 0xFF000000 | COLOR, false);
                }
                y += GROUP;
                continue;
            }
            ShortCommand command = (ShortCommand) row;
            if (this.area.shows(y, ROW)) {
                boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, ROW - 2);
                graphics.fill(this.contentX, y, this.contentX + width, y + ROW - 2, over ? 0x22FFFFFF : 0x10FFFFFF);
                graphics.drawString(this.font, this.fit(Component.literal(command.usage()), width - 8),
                        this.contentX + 4, y + 2, over ? TEXT : 0xFFFFE2C2, false);
                graphics.drawString(this.font, this.fit(Component.translatable(COMMAND + command.name()), width - 8),
                        this.contentX + 4, y + 11, MUTED, false);
                if (over) {
                    tip = this.tip(command);
                }
                this.area.hit(this.contentX, y, width, ROW - 2, () -> this.type(command));
            }
            y += ROW;
        }
        this.area.end(graphics);
        return tip;
    }

    private Component tip(ShortCommand command) {
        Component runs = command.template() != null
                ? Component.translatable(PREFIX + "runs", String.join(" , ", command.template().split(";")))
                : Component.translatable(PREFIX + "own");
        return Component.empty().append(runs).append("\n").append(Component.translatable(PREFIX + "click")
                .withStyle(ChatFormatting.GRAY));
    }

    // Closes the window into the chat with the command typed, ready for its arguments.
    private void type(ShortCommand command) {
        if (this.minecraft.level != null) {
            this.minecraft.setScreen(new ChatScreen("/" + command.name() + " "));
        }
    }

    @Nullable
    private Component yours(GuiGraphics graphics, int top, int bottom, int mouseX, int mouseY) {
        List<CommandAliases.Alias> rows = new ArrayList<>();
        for (CommandAliases.Alias alias : CommandAliases.all()) {
            if (this.matches(alias.name(), alias.command())) {
                rows.add(alias);
            }
        }
        if (rows.isEmpty()) {
            Component none = Component.translatable(CommandAliases.all().isEmpty() ? PREFIX + "none"
                    : "config." + MultiversePowers.MODID + ".search.none");
            graphics.drawString(this.font, none, this.contentX + (this.contentWidth - this.font.width(none)) / 2,
                    top + 10, MUTED, false);
            return null;
        }
        int y = this.area.begin(graphics, this.contentX, top, this.contentWidth, bottom - top, rows.size() * ROW);
        int width = this.area.rowWidth();
        boolean editable = this.editable();
        for (CommandAliases.Alias alias : rows) {
            if (this.area.shows(y, ROW)) {
                graphics.fill(this.contentX, y, this.contentX + width, y + ROW - 2, 0x10FFFFFF);
                int right = this.contentX + width - 4;
                Component remove = Component.translatable(PREFIX + "remove");
                Component edit = Component.translatable(PREFIX + "edit");
                right -= this.font.width(remove) + 6;
                this.button(graphics, remove, right, y + 5, mouseX, mouseY, editable, () -> this.remove(alias));
                right -= this.font.width(edit) + 6 + 4;
                this.button(graphics, edit, right, y + 5, mouseX, mouseY, editable, () -> this.edit(alias));
                graphics.drawString(this.font, this.fit(Component.literal("/" + alias.name()), right - this.contentX
                        - 8), this.contentX + 4, y + 2, 0xFFFFE2C2, false);
                graphics.drawString(this.font, this.fit(Component.literal(alias.command()), right - this.contentX - 8),
                        this.contentX + 4, y + 11, MUTED, false);
            }
            y += ROW;
        }
        this.area.end(graphics);
        return null;
    }

    // A small button inside the list, clicked through the list so it never fires outside its box.
    private void button(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, boolean active,
            Runnable action) {
        int width = this.font.width(text) + 6;
        boolean over = active && this.area.over(mouseX, mouseY, x, y, width, 10);
        this.chip(graphics, text, x, y, over ? 0x505050 : 0x343434, !active ? 0x6A6A6A : over ? 0xFFFFFF : 0xC8C8C8);
        if (active) {
            this.area.hit(x, y, width, 10, action);
        }
    }

    private void nothing(GuiGraphics graphics, int top) {
        Component none = Component.translatable("config." + MultiversePowers.MODID + ".search.none");
        graphics.drawString(this.font, none, this.contentX + (this.contentWidth - this.font.width(none)) / 2, top + 10,
                MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.area.scroll(scrollY, ROW);
        return true;
    }

    // Enter in the name or command saves.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.name != null
                && (this.getFocused() == this.name || this.getFocused() == this.command)) {
            this.save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
