package nl.tivek.multiversepowers.config.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.WorldSettingsEditPayload;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// The settings: one window in the update manager's look, its pages down the left: under Client your own game, under
// Server the world's power rules and stamina, then each character. The world's pages are only for its host or a listed
// owner, in that world (the server checks again): anyone else sees one shut line there. Each page has its colour. A
// change waits, marked, until Save or until the window closes; Undo drops what waits.
public final class SettingsScreen extends NavScreen {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    private static final int SEARCH = 110;
    private static final int FOOTER = 24;

    // What one visit to the window keeps from page to page.
    private static final class Visit {
        private final List<SettingsPages.Page> pages = SettingsPages.all();
        private final Map<ConfigNumber, SettingsPages.Page> pageOf = new IdentityHashMap<>();
        private final Map<ConfigNumber, Double> edits = new IdentityHashMap<>();
        private final Set<String> collapsed = new HashSet<>();
        private String query = "";

        private Visit() {
            for (SettingsPages.Page page : this.pages) {
                for (SettingsPages.Section section : page.sections()) {
                    for (SettingsPages.Group group : section.groups()) {
                        for (ConfigNumber number : group.numbers()) {
                            this.pageOf.put(number, page);
                        }
                    }
                }
            }
        }

        private SettingsPages.Page page(String id) {
            return this.pages.stream().filter(page -> page.id().equals(id)).findFirst().orElse(this.pages.get(0));
        }
    }

    private final Visit visit;
    private final SettingsPages.Page page;
    private SettingsList list;
    private Button defaults;
    private Button undo;
    private Button save;
    private boolean rebuildLater;
    private boolean keepScrollLater;

    private SettingsScreen(@Nullable Screen root, Visit visit, SettingsPages.Page page) {
        super(page.title().copy().withColor(page.color()), root, page.id());
        this.visit = visit;
        this.page = page;
    }

    // The window on `page` (an id of `SettingsPages` or a character's); closing it returns to `root`.
    public static Screen create(@Nullable Screen root, String page) {
        Visit visit = new Visit();
        return new SettingsScreen(root, visit, visit.page(page));
    }

    // Client over your own page, Server over the world's (each character's in its colour), or over one shut line.
    @Override
    protected List<Item> items() {
        List<Item> items = new ArrayList<>();
        Heading client = new Heading("client", Component.translatable(PREFIX + "side.client"));
        Heading server = new Heading("server", Component.translatable(PREFIX + "side.server"));
        SettingsPages.Page last = null;
        for (SettingsPages.Page page : this.visit.pages) {
            boolean starts = last == null || page.world() != last.world();
            boolean line = last != null && (starts || (page.character() == null) != (last.character() == null));
            items.add(new Item(page.id(), page.title(), page.icon(), () -> this.open(page), false, line,
                    starts ? page.world() ? server : client : null, page.character() == null ? 0 : page.color(),
                    null));
            last = page;
        }
        if (last == null || !last.world()) {
            items.add(new Item("host_only", Component.translatable(PREFIX + "host_only"), PixelIcons.Icon.LOCK, null,
                    false, last != null, server, 0, Component.translatable(PREFIX + "host_only.desc")));
        }
        return items;
    }

    private void open(SettingsPages.Page page) {
        this.visit.query = "";
        this.minecraft.setScreen(new SettingsScreen(this.root, this.visit, page));
    }

    @Override
    protected Component brand() {
        return Component.translatable(PREFIX + "brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.GEAR;
    }

    // A page with changes not saved yet.
    @Override
    @Nullable
    protected Badge badge(String id) {
        boolean waiting = this.visit.edits.keySet().stream()
                .anyMatch(number -> this.visit.pageOf.get(number).id().equals(id));
        return waiting ? new Badge(null, WARN) : null;
    }

    @Override
    protected int accent() {
        return this.page.color();
    }

    // Whom the page is for, or why it cannot be changed now.
    @Override
    protected Component subtitle() {
        if (!this.page.editable()) {
            return Component.translatable(PREFIX + "locked").withColor(WARN);
        }
        return Component.translatable(PREFIX + (this.page.world() ? "about.world" : "about.game"));
    }

    // Open all and Close all at the right of the subtitle, but not while searching every page.
    @Override
    protected int subtitleRoom() {
        return this.folds() ? this.font.width(this.openAll()) + this.font.width(this.closeAll()) + 24 : 0;
    }

    private boolean folds() {
        return this.visit.query.isBlank() && !this.page.sections().isEmpty();
    }

    private Component openAll() {
        return Component.translatable(PREFIX + "open_all");
    }

    private Component closeAll() {
        return Component.translatable(PREFIX + "close_all");
    }

    // Search at the right of the page's title; under the list Defaults, and Undo and Save for what waits.
    @Override
    protected void initPage() {
        int top = this.windowY + TITLE_BAR + 8;
        int room = this.contentWidth - this.font.width(this.title.copy().withStyle(ChatFormatting.BOLD)) - 12;
        int width = Math.max(60, Math.min(SEARCH, room));
        EditBox search = new EditBox(this.font, this.contentX + this.contentWidth - width, top - 3, width, 14,
                Component.translatable(PREFIX + "search"));
        search.setHint(Component.translatable(PREFIX + "search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(40);
        search.setValue(this.visit.query);
        search.setResponder(text -> {
            this.visit.query = text;
            this.rebuildLater(false);
        });
        this.addRenderableWidget(search);
        int y = this.contentY + this.contentHeight - 20;
        int right = this.contentX + this.contentWidth;
        this.defaults = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "defaults"),
                button -> this.defaults()).tooltip(tip(Component.translatable(PREFIX + "defaults.desc")))
                .bounds(this.contentX, y, 70, 20).build());
        this.undo = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "undo"),
                button -> this.undo()).tooltip(tip(Component.translatable(PREFIX + "undo.desc")))
                .bounds(right - 132, y, 64, 20).build());
        this.save = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"),
                button -> this.apply()).tooltip(tip(Component.translatable(PREFIX + "save.desc")))
                .bounds(right - 64, y, 64, 20).build());
        this.rebuild(true);
    }

    private void rebuild(boolean keepScroll) {
        double scroll = this.list == null ? 0.0 : this.list.getScrollAmount();
        if (this.list != null) {
            this.removeWidget(this.list);
        }
        this.list = new SettingsList(this.minecraft, this, this.blocks(), this.contentX, this.contentY,
                this.contentWidth, this.contentHeight - FOOTER);
        this.addRenderableWidget(this.list);
        if (keepScroll) {
            this.list.setClampedScrollAmount(scroll);
        }
        this.refreshButtons();
    }

    // The page's parts; while searching, what matches on every page.
    private List<SettingsList.Block> blocks() {
        List<SettingsList.Block> blocks = new ArrayList<>();
        String wanted = this.visit.query.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            for (int i = 0; i < this.page.sections().size(); i++) {
                SettingsPages.Section section = this.page.sections().get(i);
                String key = this.key(i);
                blocks.add(new SettingsList.Block(key, section.title(), section.hint(), section.about(),
                        this.page.color(), true, this.visit.collapsed.contains(key), section.groups()));
            }
            return blocks;
        }
        for (SettingsPages.Page page : this.visit.pages) {
            for (SettingsPages.Section section : page.sections()) {
                boolean all = matches(section.title(), wanted);
                List<SettingsPages.Group> groups = new ArrayList<>();
                for (SettingsPages.Group group : section.groups()) {
                    boolean whole = all || group.title() != null && matches(group.title(), wanted);
                    List<ConfigNumber> numbers = new ArrayList<>();
                    for (ConfigNumber number : group.numbers()) {
                        if (whole || matches(number.label(), wanted) || matches(number.description(), wanted)) {
                            numbers.add(number);
                        }
                    }
                    if (!numbers.isEmpty()) {
                        groups.add(new SettingsPages.Group(group.title(), numbers));
                    }
                }
                if (!groups.isEmpty()) {
                    Component title = Component.empty().append(page.title()).append(" › ").append(section.title());
                    blocks.add(new SettingsList.Block("search", title, section.hint(), section.about(), page.color(),
                            false, false, groups));
                }
            }
        }
        return blocks;
    }

    private static boolean matches(Component text, String wanted) {
        return text.getString().toLowerCase(Locale.ROOT).contains(wanted);
    }

    private String key(int section) {
        return this.page.id() + ":" + section;
    }

    void toggle(String key) {
        if (!this.visit.collapsed.remove(key)) {
            this.visit.collapsed.add(key);
        }
        this.rebuildLater(true);
    }

    // Every part of the page folded shut, or every one open, from the top.
    private void fold(boolean shut) {
        for (int i = 0; i < this.page.sections().size(); i++) {
            if (shut) {
                this.visit.collapsed.add(this.key(i));
            } else {
                this.visit.collapsed.remove(this.key(i));
            }
        }
        this.rebuildLater(false);
    }

    // Whether any part of the page is folded `shut` (or, false, open).
    private boolean anyFolded(boolean shut) {
        for (int i = 0; i < this.page.sections().size(); i++) {
            if (this.visit.collapsed.contains(this.key(i)) == shut) {
                return true;
            }
        }
        return false;
    }

    // Deferred: a click still walks the widget list, which a rebuild now would change under it.
    private void rebuildLater(boolean keepScroll) {
        this.keepScrollLater = this.rebuildLater ? this.keepScrollLater && keepScroll : keepScroll;
        this.rebuildLater = true;
    }

    double value(ConfigNumber number) {
        Double edited = this.visit.edits.get(number);
        return edited != null ? edited : number.clamp(number.stored().getAsDouble());
    }

    void set(ConfigNumber number, double value) {
        if (Math.abs(value - number.clamp(number.stored().getAsDouble())) < 1.0E-9) {
            this.visit.edits.remove(number);
        } else {
            this.visit.edits.put(number, value);
        }
        this.refreshButtons();
    }

    boolean editable(ConfigNumber number) {
        SettingsPages.Page page = this.visit.pageOf.get(number);
        return page != null && page.editable();
    }

    // Changed but not saved yet.
    boolean waiting(ConfigNumber number) {
        return this.visit.edits.containsKey(number);
    }

    private void refreshButtons() {
        boolean waiting = !this.visit.edits.isEmpty();
        this.undo.active = waiting;
        this.save.active = waiting;
        this.defaults.active = this.list != null && this.list.numbers().stream().anyMatch(this::editable);
    }

    // Every setting in the list back to the mod's own value, waiting like any change.
    private void defaults() {
        for (ConfigNumber number : this.list.numbers()) {
            if (this.editable(number)) {
                this.set(number, number.defaultValue());
            }
        }
        this.rebuildLater(true);
    }

    private void undo() {
        this.visit.edits.clear();
        this.rebuildLater(true);
        this.refreshButtons();
    }

    private void apply() {
        Set<SettingsPages.Page> touched = new HashSet<>();
        List<WorldSettingsEditPayload.Entry> sent = new ArrayList<>();
        boolean remote = this.minecraft == null || !this.minecraft.hasSingleplayerServer();
        for (Map.Entry<ConfigNumber, Double> edit : this.visit.edits.entrySet()) {
            ConfigNumber number = edit.getKey();
            SettingsPages.Page page = this.visit.pageOf.get(number);
            if (page == null || !page.editable()) {
                continue;
            }
            number.store().accept(edit.getValue());
            // The server owns its files: it saves them and sends everyone the new copy.
            if (page.world() && remote) {
                sent.add(new WorldSettingsEditPayload.Entry(number.file(), number.path(), edit.getValue()));
            } else {
                touched.add(page);
            }
        }
        for (SettingsPages.Page page : touched) {
            page.save().run();
        }
        if (!sent.isEmpty()) {
            PacketDistributor.sendToServer(new WorldSettingsEditPayload(sent));
        }
        this.visit.edits.clear();
        this.rebuildLater(true);
        this.refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.rebuildLater) {
            this.rebuildLater = false;
            this.rebuild(this.keepScrollLater);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderPageBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenAnchors.report("settings", this.contentX, this.contentY, this.contentWidth, this.contentHeight);
    }

    // Open all and Close all; between the buttons, how many changes wait; over an empty list, that nothing matches.
    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.folds()) {
            int y = this.windowY + TITLE_BAR + 19;
            int x = this.contentX + this.contentWidth - this.font.width(this.closeAll()) - 6;
            int width = this.smallButton(graphics, this.closeAll(), x, y, mouseX, mouseY, this.anyFolded(false),
                    () -> this.fold(true));
            ScreenAnchors.report("settings.close_all", x, y, width, 10);
            x -= this.font.width(this.openAll()) + 10;
            width = this.smallButton(graphics, this.openAll(), x, y, mouseX, mouseY, this.anyFolded(true),
                    () -> this.fold(false));
            ScreenAnchors.report("settings.open_all", x, y, width, 10);
        }
        int count = this.visit.edits.size();
        if (count > 0) {
            int left = this.contentX + 74;
            int room = this.contentWidth - 74 - 136;
            Component waiting = Component.translatable(PREFIX + "unsaved", count);
            graphics.drawString(this.font, this.fit(waiting, room), left + Math.max(0,
                    (room - this.font.width(waiting)) / 2), this.contentY + this.contentHeight - 14, 0xFF000000 | WARN,
                    false);
        }
        if (this.list != null && this.list.children().isEmpty() && !this.visit.query.isBlank()) {
            Component none = Component.translatable(PREFIX + "search.none");
            graphics.drawString(this.font, none, this.contentX + (this.contentWidth - this.font.width(none)) / 2,
                    this.contentY + 12, MUTED, false);
        }
    }

    // Closing keeps what waits, as the game's own options do.
    @Override
    public void onClose() {
        this.apply();
        super.onClose();
    }
}
