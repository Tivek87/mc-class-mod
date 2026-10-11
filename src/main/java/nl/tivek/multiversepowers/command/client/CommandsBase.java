package nl.tivek.multiversepowers.command.client;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.command.CommandCategories;
import nl.tivek.multiversepowers.command.CommandEditPayload;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;

// The Commands page's state and what its buttons do: the search, the sort and the category shown, the command being
// made or changed, and the category being made or renamed. CommandsScreen draws it.
abstract class CommandsBase extends NavScreen {
    static final String PREFIX = "config." + MultiversePowers.MODID + ".commands.";
    static final int COLOR = 0xFFB86B;
    static final int CUSTOM = 0xFF9F43;
    static final int BAR = 18;
    static final int EDITOR = 74;

    enum Mode {
        LIST, EDIT, CATEGORIES
    }

    // The categories opened in the list, kept while the game runs; all folded at first.
    static final Set<String> OPEN = new HashSet<>();

    String query = "";
    CommandRows.Sort sort = CommandRows.Sort.CATEGORY;
    Mode mode = Mode.LIST;
    // The command being changed, or null while a new one is made.
    @Nullable
    CommandRows.Entry editing;
    String typedName = "";
    String typedCommand = "";
    String typedCategory = CommandCategories.CUSTOM;
    String typedLabel = "";
    // The category being renamed, or null while a new one is made.
    @Nullable
    String renaming;
    @Nullable
    Component status;
    int statusColor;
    @Nullable
    EditBox search;
    @Nullable
    SearchSuggestions searchSuggestions;
    @Nullable
    EditBox nameBox;
    @Nullable
    EditBox commandBox;
    @Nullable
    CommandInput commandInput;
    @Nullable
    EditBox labelBox;

    CommandsBase(@Nullable Screen root) {
        super(Component.translatable("config." + MultiversePowers.MODID + ".commands").withColor(COLOR), root,
                SettingsPages.COMMANDS);
    }

    @Override
    protected List<Item> items() {
        return SettingsScreen.sidebar(SettingsPages.all(),
                page -> this.minecraft.setScreen(SettingsScreen.create(this.root(), page.id())));
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

    boolean editable() {
        return SettingsPages.worldEditable(CommandAliases.SPEC);
    }

    // The search on the bar over the list; in the editor a name, its command (with the game's suggestions) and its
    // category; on the categories page a label.
    @Override
    protected void initPage() {
        int buttons = this.font.width(Component.translatable(PREFIX + "sort", Component.translatable(PREFIX
                + "sort.custom_first"))) + 10;
        if (this.editable()) {
            buttons += this.font.width(Component.translatable(PREFIX + "new")) + 10
                    + this.font.width(Component.translatable(PREFIX + "categories")) + 10;
        }
        int width = Math.max(60, Math.min(150, this.contentWidth - buttons - 6));
        this.search = new EditBox(this.font, this.contentX, this.contentY, width, 14,
                Component.translatable(PREFIX + "search"));
        this.search.setHint(Component.translatable(PREFIX + "search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(40);
        this.search.setValue(this.query);
        this.search.setResponder(text -> this.query = text);
        this.addRenderableWidget(this.search);
        this.searchSuggestions = new SearchSuggestions(this.search);
        this.nameBox = null;
        this.commandBox = null;
        this.commandInput = null;
        this.labelBox = null;
        if (this.mode == Mode.EDIT && this.editable()) {
            this.editor(this.contentY + BAR + 2);
        } else if (this.mode == Mode.CATEGORIES && this.editable()) {
            this.categoryBox(this.contentY + this.contentHeight - 30);
        }
    }

    private void editor(int top) {
        int x = this.contentX + 6;
        int right = this.contentX + this.contentWidth - 6;
        int y = top + 14;
        int label = 52;
        this.nameBox = this.addRenderableWidget(new EditBox(this.font, x + label, y, 90, 14,
                Component.translatable(PREFIX + "name")));
        this.nameBox.setHint(Component.translatable(PREFIX + "name.hint").withStyle(ChatFormatting.DARK_GRAY));
        this.nameBox.setMaxLength(25);
        this.nameBox.setValue(this.typedName);
        this.nameBox.setResponder(text -> this.typedName = text);
        int categoryX = x + label + 96;
        Button category = Button.builder(Component.translatable(PREFIX + "category",
                CommandRows.label(this.typedCategory)), button -> this.nextCategory())
                .bounds(categoryX, y - 1, right - categoryX, 16)
                .tooltip(tip(Component.translatable(PREFIX + "category.desc"))).build();
        this.addRenderableWidget(category);
        y += 18;
        boolean builtIn = this.editing != null && !this.editing.custom();
        if (!builtIn) {
            this.commandBox = this.addRenderableWidget(new EditBox(this.font, x + label, y, right - x - label, 14,
                    Component.translatable(PREFIX + "command")));
            this.commandBox.setHint(Component.translatable(PREFIX + "command.hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            this.commandBox.setMaxLength(CommandAliases.LONGEST + 1);
            this.commandBox.setValue(this.typedCommand);
            if (this.minecraft.player != null) {
                this.commandInput = new CommandInput(this.minecraft, this, this.commandBox);
            }
            this.commandBox.setResponder(text -> {
                this.typedCommand = text;
                if (this.commandInput != null) {
                    this.commandInput.edited();
                }
            });
        }
        y += 18;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"), button -> this.save())
                .bounds(right - 104, y, 50, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "cancel"), button -> this.list())
                .bounds(right - 50, y, 50, 16).build());
    }

    private void categoryBox(int y) {
        int right = this.contentX + this.contentWidth;
        int button = 70;
        this.labelBox = this.addRenderableWidget(new EditBox(this.font, this.contentX, y, this.contentWidth - button * 2
                - 8, 14, Component.translatable(PREFIX + "categories.label")));
        this.labelBox.setHint(Component.translatable(this.renaming == null ? PREFIX + "categories.new"
                : PREFIX + "categories.rename_hint").withStyle(ChatFormatting.DARK_GRAY));
        this.labelBox.setMaxLength(CommandCategories.LONGEST_LABEL);
        this.labelBox.setValue(this.typedLabel);
        this.labelBox.setResponder(text -> this.typedLabel = text);
        this.addRenderableWidget(Button.builder(Component.translatable(this.renaming == null ? PREFIX + "categories.add"
                : PREFIX + "categories.save"), b -> this.saveCategory()).bounds(right - button * 2 - 4, y - 1, button,
                16).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "categories.done"), b -> this.list())
                .bounds(right - button, y - 1, button, 16).build());
    }

    // ---- what the buttons do ----

    void list() {
        this.mode = Mode.LIST;
        this.editing = null;
        this.renaming = null;
        this.typedLabel = "";
        this.rebuildWidgets();
    }

    void create() {
        this.mode = Mode.EDIT;
        this.editing = null;
        this.typedName = "";
        this.typedCommand = "";
        this.typedCategory = CommandCategories.CUSTOM;
        this.status = null;
        this.rebuildWidgets();
    }

    void edit(CommandRows.Entry entry) {
        this.mode = Mode.EDIT;
        this.editing = entry;
        this.typedName = entry.name();
        this.typedCommand = entry.command() == null ? "" : entry.command();
        this.typedCategory = entry.category();
        this.status = null;
        this.rebuildWidgets();
    }

    // A built-in command copied as a custom one of its own, ready to change: what it runs, its arguments left to type.
    void copy(CommandRows.Entry entry) {
        String template = entry.builtIn() == null ? null : entry.builtIn().template();
        this.mode = Mode.EDIT;
        this.editing = null;
        this.typedName = entry.name() + "2";
        this.typedCommand = template == null ? entry.name() : template.split(";")[0].replaceAll(" ?\\$\\w+", "");
        this.typedCategory = entry.category();
        this.rebuildWidgets();
    }

    void categories() {
        this.mode = Mode.CATEGORIES;
        this.renaming = null;
        this.typedLabel = "";
        this.rebuildWidgets();
    }

    void rename(CommandCategories.Category category) {
        this.renaming = category.id();
        this.typedLabel = CommandRows.label(category.id()).getString();
        this.rebuildWidgets();
    }

    private void nextCategory() {
        List<CommandCategories.Category> all = CommandCategories.all();
        int at = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(this.typedCategory)) {
                at = i;
            }
        }
        this.typedCategory = all.isEmpty() ? CommandCategories.CUSTOM
                : all.get(Math.floorMod(at + (hasShiftDown() ? -1 : 1), all.size())).id();
        this.rebuildWidgets();
    }

    void save() {
        CommandRows.Entry entry = this.editing;
        if (entry != null && !entry.custom()) {
            String name = CommandAliases.strip(this.typedName).toLowerCase(Locale.ROOT);
            if (!CommandAliases.NAME.matcher(name).matches()) {
                this.say(Component.translatable(PREFIX + "error.bad_name", name), ERROR);
                return;
            }
            this.send(new CommandEditPayload(CommandEditPayload.BUILT_IN, entry.key(), name, "", this.typedCategory));
            this.say(Component.translatable(PREFIX + "sent", name), ACCENT);
            this.list();
            return;
        }
        String alias = CommandAliases.strip(this.typedName).toLowerCase(Locale.ROOT);
        String line = CommandAliases.strip(this.typedCommand);
        String refused = this.refusal(alias, line, entry == null ? "" : entry.key());
        if (refused != null) {
            this.say(Component.translatable(PREFIX + "error." + refused, alias), ERROR);
            return;
        }
        this.send(new CommandEditPayload(CommandEditPayload.SAVE, entry == null ? "" : entry.key(), alias, line,
                this.typedCategory));
        this.say(Component.translatable(PREFIX + "sent", alias), ACCENT);
        this.list();
    }

    void delete(CommandRows.Entry entry) {
        this.send(CommandEditPayload.of(entry.custom() ? CommandEditPayload.DELETE : CommandEditPayload.REMOVE,
                entry.key()));
        this.say(Component.translatable(PREFIX + (entry.custom() ? "removed" : "taken_away"), entry.name()), ACCENT);
    }

    void restore(@Nullable CommandRows.Entry entry) {
        this.send(CommandEditPayload.of(CommandEditPayload.RESTORE, entry == null ? "" : entry.key()));
        this.say(Component.translatable(PREFIX + "restored", entry == null ? "*" : entry.name()), ACCENT);
    }

    void deleteCategory(CommandCategories.Category category) {
        this.send(new CommandEditPayload(CommandEditPayload.CATEGORY, category.id(), "", "", ""));
        OPEN.remove(category.id());
    }

    void resetCategory(String id) {
        this.send(CommandEditPayload.of(CommandEditPayload.CATEGORY_RESET, id));
    }

    void saveCategory() {
        String label = CommandCategories.clean(this.typedLabel);
        if (label.isEmpty()) {
            this.say(Component.translatable(PREFIX + "error.bad_label"), ERROR);
            return;
        }
        this.send(new CommandEditPayload(CommandEditPayload.CATEGORY, this.renaming == null ? "" : this.renaming,
                label, "", ""));
        this.renaming = null;
        this.typedLabel = "";
        this.rebuildWidgets();
    }

    // Closes the window into the chat with the command typed, ready for its arguments.
    void type(CommandRows.Entry entry) {
        if (this.minecraft.level != null) {
            this.minecraft.setScreen(new ChatScreen("/" + entry.name() + " "));
        }
    }

    private void send(CommandEditPayload edit) {
        if (this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(edit);
        }
    }

    void say(Component text, int color) {
        this.status = text;
        this.statusColor = color;
    }

    // As the server checks it again: a key under `<prefix>error.`, or null.
    @Nullable
    private String refusal(String alias, String line, String old) {
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
        for (CommandRows.Entry entry : CommandRows.all()) {
            if (!entry.custom() && (entry.key().equals(alias) || entry.name().equals(alias))) {
                return "built_in";
            }
        }
        boolean mine = alias.equals(old) || all.stream().noneMatch(other -> other.name().equals(alias));
        boolean game = this.minecraft.getConnection() != null
                && this.minecraft.getConnection().getCommands().getRoot().getChild(alias) != null;
        if (!mine || game && all.stream().noneMatch(other -> other.name().equals(alias))) {
            return "taken";
        }
        String first = line.split(" ", 2)[0];
        for (CommandAliases.Alias other : all) {
            if (!other.name().equals(alias) && !other.name().equals(old) && (other.name().equals(first)
                    || other.command().split(" ", 2)[0].equals(alias))) {
                return "loop";
            }
        }
        if (first.equals(alias)) {
            return "loop";
        }
        long others = all.stream().filter(other -> !other.name().equals(alias) && !other.name().equals(old)).count();
        return others >= CommandAliases.MOST ? "full" : null;
    }
}
