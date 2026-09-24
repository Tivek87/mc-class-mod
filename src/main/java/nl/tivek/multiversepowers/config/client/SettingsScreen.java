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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

/**
 * The settings of this mod in the game. A tab for every page (the stamina bar, and every character); on a page a part
 * for every ability that folds open and shut; a search box that looks through every page at once; and a bar at the
 * bottom that says what the number you point at does. Changes on every tab are kept until you save them, all at once,
 * into the same settings files you could open by hand; cancel throws them away.
 */
public class SettingsScreen extends DirtBackgroundScreen {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    private static final int MAX_WIDTH = 520;
    private static final int TABS_Y = 22;
    private static final int SEARCH_Y = 44;
    private static final int LIST_TOP = 64;
    // Room below the list: the bar that explains, and the buttons.
    private static final int HELP_HEIGHT = 32;
    private static final int FOOTER = 30;
    private static final int CHANGED = 0xF2C84B;

    @Nullable
    private final Screen lastScreen;
    private final List<SettingsPages.Page> pages;
    // Every number on every page, and the page it is on.
    private final Map<ConfigNumber, SettingsPages.Page> pageOf = new IdentityHashMap<>();
    // What was changed and not saved yet: a number and its new value.
    private final Map<ConfigNumber, Double> edits = new IdentityHashMap<>();
    // The parts that are folded shut, by page and part.
    private final Set<String> collapsed = new HashSet<>();
    private final List<Button> tabs = new ArrayList<>();
    private int tab;
    private String query = "";
    private SettingsList list;
    @Nullable
    private ConfigNumber pointed;
    private int panelLeft;
    private int panelWidth;
    // What to redo at the start of the next frame (see rebuildLater): the list, and whether to keep where it was
    // scrolled to; or the whole screen, for another tab.
    private boolean rebuildLater;
    private boolean keepScrollLater;
    private boolean retabLater;

    /** The settings, open on the first tab. */
    public SettingsScreen(@Nullable Screen lastScreen) {
        this(lastScreen, 0);
    }

    /** The settings, open on this tab (see {@link SettingsPages#all}). */
    public SettingsScreen(@Nullable Screen lastScreen, int tab) {
        super(Component.translatable(PREFIX + "title"));
        this.lastScreen = lastScreen;
        this.pages = SettingsPages.all();
        this.tab = Mth.clamp(tab, 0, this.pages.size() - 1);
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

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 12, MAX_WIDTH);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        int inner = this.panelWidth - 12;
        int left = this.panelLeft + 6;
        int right = left + inner;

        this.tabs.clear();
        int count = this.pages.size();
        int tabWidth = (inner - (count - 1) * 2) / count;
        for (int i = 0; i < count; i++) {
            int index = i;
            Button button = Button.builder(this.tabLabel(i), pressed -> this.selectTab(index))
                    .bounds(left + i * (tabWidth + 2), TABS_Y, tabWidth, 18).build();
            this.tabs.add(this.addRenderableWidget(button));
        }

        EditBox search = new EditBox(this.font, left + 1, SEARCH_Y + 1, inner - 2 * 64 - 6, 16,
                Component.translatable(PREFIX + "search"));
        search.setHint(Component.translatable(PREFIX + "search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(40);
        search.setValue(this.query);
        search.setResponder(text -> {
            this.query = text;
            this.rebuildLater(false);
        });
        this.addRenderableWidget(search);
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "expand"), pressed -> {
            this.collapsed.clear();
            this.rebuildLater(true);
        }).bounds(right - 2 * 64 - 2, SEARCH_Y, 64, 18)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "expand.desc"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "collapse"), pressed -> {
            SettingsPages.Page page = this.pages.get(this.tab);
            for (int i = 0; i < page.sections().size(); i++) {
                this.collapsed.add(this.key(this.tab, i));
            }
            this.rebuildLater(false);
        }).bounds(right - 64, SEARCH_Y, 64, 18)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "collapse.desc"))).build());

        int buttonsY = this.height - 26;
        int buttonWidth = Math.min(100, (inner - 3 * 4) / 4);
        int start = this.width / 2 - (4 * buttonWidth + 3 * 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "defaults"), pressed -> this.defaults())
                .bounds(start, buttonsY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "defaults.desc"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "apply"), pressed -> this.apply())
                .bounds(start + buttonWidth + 4, buttonsY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "apply.desc"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"), pressed -> {
            this.apply();
            this.onClose();
        }).bounds(start + 2 * (buttonWidth + 4), buttonsY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, pressed -> this.onClose())
                .bounds(start + 3 * (buttonWidth + 4), buttonsY, buttonWidth, 20).build());
        this.rebuild(true);
    }

    /** The list made anew for the tab, the search and what is folded shut; scrolled back where it was if asked. */
    private void rebuild(boolean keepScroll) {
        double scroll = this.list == null ? 0.0 : this.list.getScrollAmount();
        if (this.list != null) {
            this.removeWidget(this.list);
        }
        int left = this.panelLeft + 6;
        int bottom = this.height - FOOTER - HELP_HEIGHT;
        this.list = new SettingsList(this.minecraft, this, this.blocks(), left, LIST_TOP, this.panelWidth - 12,
                bottom - LIST_TOP);
        this.addRenderableWidget(this.list);
        if (keepScroll) {
            this.list.setClampedScrollAmount(scroll);
        }
        for (int i = 0; i < this.tabs.size(); i++) {
            this.tabs.get(i).setMessage(this.tabLabel(i));
        }
    }

    /**
     * What the list shows: the parts of the page of the open tab, or, while something is searched for, every number of
     * every page that matches it, under the name of its page and part.
     */
    private List<SettingsList.Block> blocks() {
        List<SettingsList.Block> blocks = new ArrayList<>();
        String wanted = this.query.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            SettingsPages.Page page = this.pages.get(this.tab);
            for (int i = 0; i < page.sections().size(); i++) {
                SettingsPages.Section section = page.sections().get(i);
                String key = this.key(this.tab, i);
                blocks.add(new SettingsList.Block(key, section.title(), section.hint(), page.color(), true,
                        this.collapsed.contains(key), section.groups()));
            }
            return blocks;
        }
        for (int p = 0; p < this.pages.size(); p++) {
            SettingsPages.Page page = this.pages.get(p);
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
                    blocks.add(new SettingsList.Block("search", title, section.hint(), page.color(), false, false,
                            groups));
                }
            }
        }
        return blocks;
    }

    private static boolean matches(Component text, String wanted) {
        return text.getString().toLowerCase(Locale.ROOT).contains(wanted);
    }

    private String key(int page, int section) {
        return page + ":" + section;
    }

    /** A part folded open or shut, by its title in the list. */
    void toggle(String key) {
        if (!this.collapsed.remove(key)) {
            this.collapsed.add(key);
        }
        this.rebuildLater(true);
    }

    private void selectTab(int index) {
        this.tab = index;
        this.query = "";
        this.retabLater = true;
    }

    /**
     * The list made anew at the start of the next frame: a click or a key asks for it while the screen still goes
     * through its buttons, which must not change under its hands.
     */
    private void rebuildLater(boolean keepScroll) {
        this.keepScrollLater = this.rebuildLater ? this.keepScrollLater && keepScroll : keepScroll;
        this.rebuildLater = true;
    }

    /** The name on a tab: the open one stands out, and a dot shows a page with changes not saved yet. */
    private Component tabLabel(int index) {
        SettingsPages.Page page = this.pages.get(index);
        boolean changed = this.edits.keySet().stream().anyMatch(number -> this.pageOf.get(number) == page);
        Component label = changed ? Component.empty().append(page.title()).append(" •") : page.title();
        return index == this.tab && this.query.isBlank() ? label.copy().withStyle(ChatFormatting.YELLOW,
                ChatFormatting.UNDERLINE) : label;
    }

    // ---- The numbers, as changed on this screen ----

    /** What a number is right now on this screen: changed and not saved yet, or what its file holds. */
    double value(ConfigNumber number) {
        Double edited = this.edits.get(number);
        return edited != null ? edited : number.clamp(number.stored().getAsDouble());
    }

    /** A number changed on this screen: kept until it is saved, or forgotten when it is back to what the file has. */
    void set(ConfigNumber number, double value) {
        if (Math.abs(value - number.clamp(number.stored().getAsDouble())) < 1.0E-9) {
            this.edits.remove(number);
        } else {
            this.edits.put(number, value);
        }
        for (int i = 0; i < this.tabs.size(); i++) {
            this.tabs.get(i).setMessage(this.tabLabel(i));
        }
    }

    /** Whether this number can be changed: its page's settings file is open. */
    boolean editable(ConfigNumber number) {
        SettingsPages.Page page = this.pageOf.get(number);
        return page != null && page.editable();
    }

    /** The number the mouse points at, for the bar that explains it. */
    void pointAt(@Nullable ConfigNumber number) {
        this.pointed = number;
    }

    /** Every number in the list back to what the mod itself has; nothing is saved yet. */
    private void defaults() {
        for (ConfigNumber number : this.list.numbers()) {
            this.set(number, number.defaultValue());
        }
        this.rebuildLater(true);
    }

    /** Puts every change into its settings file and writes the files; the screen stays open. */
    private void apply() {
        Set<SettingsPages.Page> touched = new HashSet<>();
        for (Map.Entry<ConfigNumber, Double> edit : this.edits.entrySet()) {
            SettingsPages.Page page = this.pageOf.get(edit.getKey());
            if (page != null && page.editable()) {
                edit.getKey().store().accept(edit.getValue());
                touched.add(page);
            }
        }
        for (SettingsPages.Page page : touched) {
            page.save().run();
        }
        this.edits.clear();
        this.rebuildLater(true);
    }

    // ---- Drawing ----

    /**
     * The background, the panel and its title. The game draws the background again at the start of every frame,
     * before the buttons and the list, so everything that has to lie under them goes here.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderTransparentBackground(graphics);
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
        drawPanel(graphics, this.panelLeft, 4, this.panelWidth, this.height - 8, PANEL_BORDER);
        this.drawBigCenteredString(graphics, this.title, this.width / 2, 8, 1.1F, 0xFFFFD255);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.retabLater) {
            this.retabLater = false;
            this.rebuildLater = false;
            // Another page: start at its top.
            this.list = null;
            this.rebuildWidgets();
        } else if (this.rebuildLater) {
            this.rebuildLater = false;
            this.rebuild(this.keepScrollLater);
        }
        // The list says which number the mouse points at while it draws its rows; the bar below it tells about it.
        this.pointed = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderHelp(graphics);
    }

    /** The bar under the list: what the number pointed at does, or what to do here, and how much is not saved. */
    private void renderHelp(GuiGraphics graphics) {
        int center = this.width / 2;
        int left = this.panelLeft + 8;
        int width = this.panelWidth - 16;
        int top = this.height - FOOTER - HELP_HEIGHT + 3;
        drawDivider(graphics, left - 2, top - 2, width + 4);
        Component line;
        int color;
        if (this.pointed != null) {
            line = this.pointed.description().getString().isEmpty() ? this.pointed.label()
                    : this.pointed.description();
            color = TEXT_COLOR;
        } else if (!this.pages.get(this.tab).editable()) {
            line = Component.translatable(PREFIX + "locked");
            color = NOTICE_COLOR;
        } else if (this.minecraft != null && this.minecraft.level != null
                && !this.minecraft.hasSingleplayerServer()) {
            // On someone else's server their own file decides; this only changes your own copy.
            line = Component.translatable(PREFIX + "server_decides");
            color = NOTICE_COLOR;
        } else {
            line = Component.translatable(PREFIX + "help");
            color = MUTED_COLOR;
        }
        List<FormattedCharSequence> lines = this.font.split(line, width);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(this.font, lines.get(i), left, top + i * 10, color);
        }
        if (!this.edits.isEmpty()) {
            Component changed = Component.translatable(PREFIX + "unsaved", this.edits.size());
            graphics.drawString(this.font, changed, left + width - this.font.width(changed), top + 20, CHANGED);
        }
        if (this.list != null && this.list.children().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "search.none"), center,
                    LIST_TOP + 12, MUTED_COLOR);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
