package nl.tivek.multiversepowers.faction.client.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientFactions;
import nl.tivek.multiversepowers.faction.client.pick.MobInfo;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;
import nl.tivek.multiversepowers.faction.net.FactionEditPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// What the Factions page knows and the parts its lists are built from: the faction opened, the creature the player
// stood before, the box a word is typed in, and rows, pills and steppers that scroll with the list. Nothing is kept
// here of the factions themselves: every change goes to the server and comes back with the world's settings.
abstract class FactionsState extends NavScreen {
    static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    static final String KEY = PREFIX + "factions.";
    static final String WORD = "faction." + MultiversePowers.MODID + ".";
    public static final int COLOR = 0xE8B04A;
    static final int ROW = 18;
    static final int HEADER = 16;
    static final int LABEL = 96;
    static final int OVERVIEW = 0;
    static final int MEMBERS = 1;
    static final int RELATIONS = 2;
    static final int REPUTATION = 3;
    static final String NEW = "new";
    private static final long PENDING_MS = 3000L;

    // One row of a list: draws itself at `y`, and returns what to tell while the pointer is on it.
    @FunctionalInterface
    interface Drawer {
        @Nullable
        Component draw(GuiGraphics graphics, int y, int width, int mouseX, int mouseY);
    }

    record Line(boolean striped, Drawer drawer) {
    }

    record Option(Component label, int rgb, boolean lit, Runnable choose) {
    }

    // A number just set here that the server has not answered yet: `from` is what it was.
    private record Pending(int value, int from, long at) {
    }

    final ScrollArea area = new ScrollArea();
    final MobPreview preview = new MobPreview();
    private final Map<String, List<MobInfo.Info>> members = new HashMap<>();
    // Numbers set here, by faction and key, shown at once and stepped on from, so quick clicks add up while the
    // server's answer is on its way.
    private final Map<String, Pending> pending = new HashMap<>();
    private int membersStamp;
    int listX;
    int listWidth;
    @Nullable
    String picked;
    // A faction just made here, opened once the world's settings come back with it.
    @Nullable
    String wanted;
    int tab = OVERVIEW;
    String query = "";
    @Nullable
    EntityType<?> shownKind;
    // The player of the faction shown on its card instead of a creature.
    @Nullable
    String shownPlayer;
    // The creature the player stood before when the page opened, to give a faction or a way of its own.
    @Nullable
    Mob looked;
    boolean choosing;
    @Nullable
    EditBox typing;
    @Nullable
    String typingKey;
    String typingFaction = "";
    private int typingX;
    private int typingY;
    private int typingWidth;
    private boolean typingShown;

    FactionsState(Component title, @Nullable Screen root, String page) {
        super(title, root, page);
    }

    boolean editable() {
        return SettingsPages.worldEditable(FactionRules.SPEC);
    }

    void send(int action, String faction, String key, String... values) {
        PacketDistributor.sendToServer(new FactionEditPayload(action, faction, key, Arrays.asList(values)));
    }

    @Nullable
    Faction faction() {
        return this.picked == null ? null : FactionRules.get(this.picked);
    }

    // The kinds of creature in a faction, by name; worked out again when the world's settings change.
    List<MobInfo.Info> members(String faction) {
        int stamp = FactionRules.stamp();
        if (stamp != this.membersStamp) {
            this.membersStamp = stamp;
            this.members.clear();
        }
        return this.members.computeIfAbsent(faction, id -> {
            List<MobInfo.Info> infos = new ArrayList<>();
            for (MobInfo.Info info : MobInfo.all()) {
                Faction of = FactionRules.of(info.type());
                if (of != null && of.id().equals(id)) {
                    infos.add(info);
                }
            }
            return infos;
        });
    }

    List<String> players(Faction faction) {
        PlayerTeam team = this.minecraft.level == null ? null
                : this.minecraft.level.getScoreboard().getPlayerTeam(faction.id());
        List<String> names = team == null ? new ArrayList<>() : new ArrayList<>(team.getPlayers());
        names.sort(null);
        return names;
    }

    void face(GuiGraphics graphics, String player, int x, int y, int size) {
        ClientFactions.face(graphics, player, x, y, size);
    }

    boolean online(String player) {
        return ClientFactions.online(player);
    }

    static MutableComponent name(Faction faction) {
        return Component.literal(faction.name()).withColor(faction.color());
    }

    // How a faction stands, in its colour; null is each creature as its nature has it.
    static MutableComponent stand(@Nullable Standing standing) {
        return Component.translatable(WORD + "stand." + (standing == null ? FactionTable.NATURAL
                : standing.name().toLowerCase(Locale.ROOT))).withColor(standing == null ? 0xC8C8C8 : standing.rgb());
    }

    static int dim(int rgb) {
        return (rgb >> 17 & 0x7F) << 16 | (rgb >> 9 & 0x7F) << 8 | rgb >> 1 & 0x7F;
    }

    // A name a new faction goes by in commands and files, made from what the player called it and not yet taken.
    static String slug(String name) {
        String id = name.strip().toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_.+-]", "");
        id = id.length() > 16 ? id.substring(0, 16) : id;
        if (id.isEmpty() || id.equals("-")) {
            id = "faction";
        }
        String free = id;
        for (int i = 2; FactionRules.get(free) != null; i++) {
            String number = String.valueOf(i);
            free = id.substring(0, Math.min(id.length(), 16 - number.length())) + number;
        }
        return free;
    }

    void startTyping(String key, String faction, String initial, int max) {
        this.stopTyping();
        EditBox box = new EditBox(this.font, 0, 0, 80, 12, Component.translatable(KEY + "word." + key));
        box.setMaxLength(max);
        box.setValue(initial);
        box.moveCursorToEnd(false);
        box.setHighlightPos(0);
        box.visible = false;
        this.typing = box;
        this.typingKey = key;
        this.typingFaction = faction;
        this.addRenderableWidget(box);
        this.setFocused(box);
    }

    void stopTyping() {
        if (this.typing != null) {
            this.removeWidget(this.typing);
        }
        this.typing = null;
        this.typingKey = null;
    }

    void applyTyping() {
        if (this.typing != null && this.typingKey != null) {
            String text = FactionTable.clean(this.typing.getValue(), FactionTable.TEXT);
            if (this.typingKey.equals(NEW)) {
                if (!text.isEmpty()) {
                    String id = slug(text);
                    this.send(FactionEditPayload.CREATE, id, text);
                    this.wanted = id;
                }
            } else if (!text.isEmpty() || !this.typingKey.equals("name")) {
                this.send(FactionEditPayload.WORD, this.typingFaction, this.typingKey, text);
            }
        }
        this.stopTyping();
    }

    void beginTyping() {
        this.typingShown = false;
    }

    // The open box over its row, hidden while the row is scrolled out of the list or its faction is no longer shown.
    void placeTyping() {
        if (this.typing == null) {
            return;
        }
        if (!NEW.equals(this.typingKey) && !this.typingFaction.equals(this.picked)) {
            this.stopTyping();
            return;
        }
        this.typing.visible = this.typingShown;
        this.typing.setX(this.typingX);
        this.typing.setY(this.typingY);
        this.typing.setWidth(this.typingWidth);
    }

    boolean typing(String key) {
        return this.typing != null && key.equals(this.typingKey);
    }

    void typingAt(int x, int y, int width) {
        this.typingX = x;
        this.typingY = y;
        this.typingWidth = width;
        this.typingShown = this.area.whole(y, 12);
    }

    Line heading(Component title) {
        return new Line(false, (graphics, y, width, mouseX, mouseY) -> {
            graphics.drawString(this.font, title.copy().withStyle(style -> style.withBold(true)), this.listX + 2,
                    y + 7, 0xFF000000 | COLOR, false);
            graphics.fill(this.listX, y + ROW - 1, this.listX + width, y + ROW, 0x1CFFFFFF);
            return null;
        });
    }

    // A row with its name on the left; `right` draws what it holds, right-aligned to the x it is given, and returns
    // its left edge.
    Line row(Component label, @Nullable Component tip, RightSide right) {
        return new Line(true, (graphics, y, width, mouseX, mouseY) -> {
            int left = right.draw(graphics, this.listX + width - 4, y, mouseX, mouseY);
            graphics.drawString(this.font, this.fit(label, left - this.listX - 10), this.listX + 5, y + 5, BODY,
                    false);
            return tip != null && this.area.over(mouseX, mouseY, this.listX, y, left - this.listX - 2, ROW) ? tip
                    : null;
        });
    }

    @FunctionalInterface
    interface RightSide {
        int draw(GuiGraphics graphics, int right, int y, int mouseX, int mouseY);
    }

    // One of a faction's words, typed into where it stands after a click.
    Line textLine(Faction faction, String key, String value, int max) {
        Component label = Component.translatable(KEY + "word." + key);
        return new Line(true, (graphics, y, width, mouseX, mouseY) -> {
            int x = this.listX + Math.min(LABEL, width / 3);
            int room = this.listX + width - 4 - x;
            graphics.drawString(this.font, this.fit(label, x - this.listX - 8), this.listX + 5, y + 5, MUTED, false);
            if (this.typing(key)) {
                graphics.fill(x - 3, y + 2, x + room + 1, y + 16, 0xFF000000);
                this.typingAt(x - 2, y + 3, room + 2);
                return null;
            }
            boolean editable = this.editable();
            boolean over = this.area.over(mouseX, mouseY, x - 3, y, room + 4, ROW);
            if (editable && over) {
                graphics.fill(x - 3, y + 2, x + room + 1, y + 16, 0xFF2A2A2A);
            }
            Component shown = value.isEmpty() ? Component.translatable(KEY + (editable ? "word.write" : "word.none"))
                    .withColor(DIM & 0xFFFFFF) : Component.literal(value);
            graphics.drawString(this.font, this.fit(shown, room), x, y + 5, value.isEmpty() ? DIM : TEXT, false);
            if (editable) {
                this.area.hit(x - 3, y, room + 4, ROW, () -> this.startTyping(key, faction.id(), value, max));
            }
            return over && this.font.width(shown) > room ? shown
                    : over && editable ? Component.translatable(KEY + "word.tip") : null;
        });
    }

    // A chip inside the list that scrolls with it; returns its width.
    int button(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, boolean active,
            Runnable action) {
        int width = this.font.width(text) + 6;
        boolean over = active && this.area.over(mouseX, mouseY, x, y, width, 10);
        this.chip(graphics, text, x, y, over ? 0x505050 : 0x343434, !active ? 0x6A6A6A : over ? 0xFFFFFF : 0xC8C8C8);
        if (active) {
            this.area.hit(x, y, width, 10, action);
        }
        return width;
    }

    // Choices side by side, right-aligned to `right`, the lit one how it is now; returns their left edge.
    int options(GuiGraphics graphics, List<Option> options, int right, int y, int mouseX, int mouseY,
            boolean active) {
        int all = -2;
        for (Option option : options) {
            all += this.font.width(option.label()) + 10;
        }
        int x = right - all;
        int left = x;
        for (Option option : options) {
            int each = this.font.width(option.label()) + 8;
            boolean over = active && this.area.over(mouseX, mouseY, x, y, each, 12);
            graphics.fill(x, y, x + each, y + 12, option.lit() ? 0xFF000000 | dim(option.rgb()) : over ? 0xFF404040
                    : 0xFF262626);
            graphics.drawString(this.font, option.label(), x + (each - this.font.width(option.label())) / 2, y + 2,
                    option.lit() ? 0xFFFFFFFF : over ? 0xFFE0E0E0 : 0xFF8A8A8A, false);
            if (active && !option.lit()) {
                this.area.hit(x, y, each, 12, option.choose());
            }
            x += each + 2;
        }
        return left;
    }

    // Friendly, neutral and hostile, and before them `first` (null: none) for neither: as its nature has it, or as
    // its faction does.
    List<Option> stances(@Nullable Standing now, @Nullable String first, Consumer<Standing> choose) {
        List<Option> options = new ArrayList<>();
        if (first != null) {
            options.add(new Option(Component.translatable(KEY + "pill." + first), 0x9A9A9A, now == null,
                    () -> choose.accept(null)));
        }
        for (Standing standing : new Standing[] {Standing.FRIENDLY, Standing.NEUTRAL, Standing.HOSTILE}) {
            options.add(new Option(Component.translatable(WORD + "stand."
                    + standing.name().toLowerCase(Locale.ROOT)), standing.rgb(), standing == now,
                    () -> choose.accept(standing)));
        }
        return options;
    }

    List<Option> yesNo(boolean now, Consumer<Boolean> choose) {
        return List.of(new Option(Component.translatable(KEY + "pill.yes"), 0x4CFF6E, now,
                        () -> choose.accept(true)),
                new Option(Component.translatable(KEY + "pill.no"), 0xFF3A30, !now, () -> choose.accept(false)));
    }

    // The number as last set here until the server has it (or a few seconds went by), else as the server has it.
    int shown(String key, int server) {
        Pending set = this.pending.get(key);
        if (set == null) {
            return server;
        }
        if (server == set.value() || server != set.from() || Util.getMillis() - set.at() > PENDING_MS) {
            this.pending.remove(key);
            return server;
        }
        return set.value();
    }

    void expect(String key, int server, int value) {
        this.pending.put(key, new Pending(value, server, Util.getMillis()));
    }

    // The player's reputation with `faction`, a change just asked for shown at once.
    int reputation(Faction faction) {
        return this.shown("rep|" + faction.id(), ClientFactions.reputation(faction));
    }

    void changeReputation(Faction faction, String how, int number) {
        int now = ClientFactions.reputation(faction);
        int to = how.equals("set") ? number : this.reputation(faction) + number;
        this.expect("rep|" + faction.id(), now, Mth.clamp(to, Faction.LOWEST, Faction.HIGHEST));
        this.send(FactionEditPayload.REPUTATION, faction.id(), how, String.valueOf(number));
    }

    // - value + right-aligned to `right`; a step is 5, 1 with shift held. Returns its left edge.
    int stepper(GuiGraphics graphics, String key, int server, int least, int most, int step, int right, int y,
            int mouseX, int mouseY, Consumer<Integer> set) {
        boolean editable = this.editable();
        int value = this.shown(key, server);
        int by = hasShiftDown() ? 1 : step;
        Consumer<Integer> change = to -> {
            this.expect(key, server, to);
            set.accept(to);
        };
        Component plus = Component.literal("+");
        right -= this.font.width(plus) + 6;
        this.button(graphics, plus, right, y + 4, mouseX, mouseY, editable && value < most,
                () -> change.accept(Math.min(most, value + by)));
        String shown = String.valueOf(value);
        right -= 30;
        graphics.fill(right, y + 3, right + 28, y + 15, 0xFF161616);
        graphics.drawString(this.font, shown, right + (28 - this.font.width(shown)) / 2, y + 5, TEXT, false);
        Component minus = Component.literal("-");
        right -= this.font.width(minus) + 8;
        this.button(graphics, minus, right, y + 4, mouseX, mouseY, editable && value > least,
                () -> change.accept(Math.max(least, value - by)));
        return right;
    }
}
