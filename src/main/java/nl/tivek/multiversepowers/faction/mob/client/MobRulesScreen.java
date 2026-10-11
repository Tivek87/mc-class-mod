package nl.tivek.multiversepowers.faction.mob.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobRuleEditPayload;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.mob.MobTableAskPayload;
import org.lwjgl.glfw.GLFW;

// The settings window's Mobs page: every creature of the game, modded ones too, searchable; a click on one lists every
// creature and the player again, each with Friendly, Neutral and Hostile, the one lit being how the first stands to it.
// A line under one marks the game's own. A change goes to the server at once (`MobRules`).
public final class MobRulesScreen extends NavScreen {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    private static final int COLOR = 0x7FD8FF;
    private static final int ROW = 18;
    private static final int HEADER = 16;
    private static final int SEARCH = 110;
    private static final Standing[] ORDER = {Standing.FRIENDLY, Standing.NEUTRAL, Standing.HOSTILE};

    // A creature (`index` in the table's order) or the player (-1).
    private record Row(int index, EntityType<?> type, Component name, ItemStack icon) {
    }

    private final ScrollArea area = new ScrollArea();
    // Sent to the server, maybe not back yet, by "<creature>><target>": null for the game's own.
    private final Map<String, Standing> sent = new HashMap<>();
    private List<Row> creatures = List.of();
    @Nullable
    private Row player;
    @Nullable
    private Row picked;
    private String query = "";
    private boolean asked;
    @Nullable
    private EditBox search;

    public MobRulesScreen(@Nullable Screen root) {
        super(Component.translatable(PREFIX + "mobs").withColor(COLOR), root, SettingsPages.MOBS);
    }

    @Override
    protected List<Item> items() {
        return SettingsScreen.sidebar(SettingsPages.all(),
                page -> this.minecraft.setScreen(SettingsScreen.create(this.root, page.id())));
    }

    @Override
    protected Component brand() {
        return Component.translatable(PREFIX + "brand");
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
            return Component.translatable(PREFIX + "locked").withColor(WARN);
        }
        return Component.translatable(PREFIX + "mobs.about");
    }

    private boolean editable() {
        return SettingsPages.worldEditable(MobRules.SPEC);
    }

    @Override
    protected void initPage() {
        if (!this.asked) {
            this.asked = true;
            ClientMobTable.forget();
            PacketDistributor.sendToServer(new MobTableAskPayload());
        }
        int top = this.windowY + TITLE_BAR + 8;
        int room = this.contentWidth - this.font.width(this.title.copy().withStyle(ChatFormatting.BOLD)) - 12;
        int width = Math.max(60, Math.min(SEARCH, room));
        this.search = new EditBox(this.font, this.contentX + this.contentWidth - width, top - 3, width, 14,
                Component.translatable(PREFIX + "search"));
        this.search.setHint(Component.translatable(PREFIX + "mobs.search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(40);
        this.search.setValue(this.query);
        this.search.setResponder(text -> {
            this.query = text;
            this.area.top();
        });
        this.addRenderableWidget(this.search);
    }

    private void build() {
        List<Row> rows = new ArrayList<>();
        List<EntityType<?>> types = ClientMobTable.types();
        for (int i = 0; i < types.size(); i++) {
            EntityType<?> type = types.get(i);
            if (type != null) {
                SpawnEggItem egg = SpawnEggItem.byId(type);
                rows.add(new Row(i, type, type.getDescription(),
                        egg != null ? new ItemStack(egg) : new ItemStack(Items.NAME_TAG)));
            }
        }
        rows.sort(Comparator.comparing(row -> row.name().getString()));
        this.creatures = rows;
        this.player = new Row(-1, EntityType.PLAYER, Component.translatable(PREFIX + "mobs.player"),
                new ItemStack(Items.PLAYER_HEAD));
    }

    // How `actor` stands to `target` now: what was just sent, else what the world sets, else the game's own.
    private Standing shown(Row actor, Row target) {
        Standing own = ClientMobTable.of(actor.index(), target.index());
        String key = this.sent.isEmpty() ? null : key(actor, target);
        if (key != null && this.sent.containsKey(key)) {
            Standing chosen = this.sent.get(key);
            return chosen != null ? chosen : own;
        }
        Standing set = MobRules.set(actor.type(), target.type());
        return set != null ? set : own;
    }

    private static String key(Row actor, Row target) {
        return MobRules.id(actor.type()) + ">" + MobRules.id(target.type());
    }

    private int changes(Row actor) {
        int changed = this.shown(actor, this.player) != ClientMobTable.of(actor.index(), -1) ? 1 : 0;
        for (Row target : this.creatures) {
            changed += this.shown(actor, target) != ClientMobTable.of(actor.index(), target.index()) ? 1 : 0;
        }
        return changed;
    }

    private void choose(Row actor, Row target, Standing standing) {
        boolean own = standing == ClientMobTable.of(actor.index(), target.index());
        this.sent.put(key(actor, target), own ? null : standing);
        PacketDistributor.sendToServer(new MobRuleEditPayload(MobRules.id(actor.type()), MobRules.id(target.type()),
                own ? -1 : standing.ordinal()));
    }

    private void reset(Row actor) {
        this.sent.put(key(actor, this.player), null);
        for (Row target : this.creatures) {
            this.sent.put(key(actor, target), null);
        }
        PacketDistributor.sendToServer(new MobRuleEditPayload(MobRules.id(actor.type()), "", -1));
    }

    private void pick(@Nullable Row row) {
        this.picked = row;
        this.area.top();
        this.query = "";
        if (this.search != null) {
            this.search.setValue("");
        }
    }

    private List<Row> shownRows() {
        String wanted = this.query.trim().toLowerCase(Locale.ROOT);
        List<Row> all = new ArrayList<>();
        if (this.picked != null) {
            all.add(this.player);
        }
        all.addAll(this.creatures);
        if (wanted.isEmpty()) {
            return all;
        }
        List<Row> found = new ArrayList<>();
        for (Row row : all) {
            if (row.name().getString().toLowerCase(Locale.ROOT).contains(wanted)
                    || MobRules.id(row.type()).contains(wanted)) {
                found.add(row);
            }
        }
        return found;
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientMobTable.ready()) {
            Component waiting = Component.translatable(PREFIX + "mobs.loading");
            graphics.drawString(this.font, waiting, this.contentX + (this.contentWidth - this.font.width(waiting)) / 2,
                    this.contentY + 12, MUTED, false);
            return;
        }
        if (this.player == null) {
            this.build();
        }
        this.header(graphics, mouseX, mouseY);
        int top = this.contentY + HEADER + 4;
        int height = this.contentY + this.contentHeight - top;
        List<Row> rows = this.shownRows();
        ScreenAnchors.report("mobs.list", this.contentX, top, this.contentWidth, height);
        if (rows.isEmpty()) {
            Component none = Component.translatable(PREFIX + "search.none");
            graphics.drawString(this.font, none, this.contentX + (this.contentWidth - this.font.width(none)) / 2,
                    top + 8, MUTED, false);
            return;
        }
        Component tip = null;
        int start = this.area.begin(graphics, this.contentX, top, this.contentWidth, height, rows.size() * ROW);
        int width = this.area.rowWidth();
        for (int i = 0; i < rows.size(); i++) {
            int y = start + i * ROW;
            if (!this.area.shows(y, ROW)) {
                continue;
            }
            Row row = rows.get(i);
            boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, ROW);
            if (i % 2 == 0) {
                graphics.fill(this.contentX, y, this.contentX + width, y + ROW, 0x10FFFFFF);
            }
            graphics.renderItem(row.icon(), this.contentX + 2, y + 1);
            int right = this.contentX + width - 4;
            if (this.picked == null) {
                if (over) {
                    graphics.fill(this.contentX, y, this.contentX + width, y + ROW, 0x18FFFFFF);
                }
                graphics.drawString(this.font, "›", right - 4, y + 5, over ? TEXT : MUTED, false);
                right -= 10;
                int changed = this.changes(row);
                if (changed > 0) {
                    Component count = Component.translatable(PREFIX + "mobs.changed", changed);
                    right -= this.font.width(count) + 6;
                    this.chip(graphics, count, right, y + 4, 0x3A3010, WARN);
                    right -= 4;
                }
                this.area.hit(this.contentX, y, width, ROW, () -> this.pick(row));
            } else {
                Tip pills = this.pills(graphics, row, right, y + 3, mouseX, mouseY);
                right = pills.left() - 4;
                if (pills.tip() != null) {
                    tip = pills.tip();
                }
                if (this.shown(this.picked, row) != ClientMobTable.of(this.picked.index(), row.index())) {
                    graphics.drawString(this.font, "•", right - 4, y + 5, 0xFF000000 | WARN, false);
                    right -= 8;
                }
            }
            graphics.drawString(this.font, this.fit(row.name(), right - this.contentX - 26), this.contentX + 22,
                    y + 5, over && this.picked == null ? TEXT : BODY, false);
        }
        this.area.end(graphics);
        if (tip != null) {
            graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    // Over the list: what to do, or the picked creature with a way back and its reset.
    private void header(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.contentY + 3;
        if (this.picked == null) {
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "mobs.pick", this.creatures.size()),
                    this.contentWidth), this.contentX, y, MUTED, false);
            return;
        }
        Row actor = this.picked;
        int x = this.contentX;
        x += this.link(graphics, Component.translatable(PREFIX + "mobs.back"), x, y, mouseX, mouseY, MUTED,
                () -> this.pick(null)) + 8;
        Component reset = Component.translatable(PREFIX + "mobs.reset");
        int resetX = this.contentX + this.contentWidth - this.font.width(reset) - 6;
        this.smallButton(graphics, reset, resetX, y - 1, mouseX, mouseY, this.editable() && this.changes(actor) > 0,
                () -> this.reset(actor));
        Component name = Component.translatable(PREFIX + "mobs.toward", actor.name()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(name, resetX - x - 8), x, y, 0xFF000000 | COLOR, false);
    }

    private record Tip(int left, @Nullable Component tip) {
    }

    // Friendly, Neutral and Hostile for one target, right-aligned to `right`, the lit one how the picked creature
    // stands to it, a line under the game's own.
    private Tip pills(GuiGraphics graphics, Row target, int right, int y, int mouseX, int mouseY) {
        Row actor = this.picked;
        Standing now = this.shown(actor, target);
        Standing own = ClientMobTable.of(actor.index(), target.index());
        int each = 0;
        for (Standing standing : ORDER) {
            each = Math.max(each, this.font.width(label(standing)) + 8);
        }
        int x = right - each * ORDER.length - 2 * (ORDER.length - 1);
        int left = x;
        Component tip = null;
        boolean editable = this.editable();
        for (Standing standing : ORDER) {
            boolean lit = standing == now;
            boolean over = editable && this.area.over(mouseX, mouseY, x, y, each, 12);
            int fill = lit ? 0xFF000000 | dim(standing.rgb()) : over ? 0xFF404040 : 0xFF262626;
            graphics.fill(x, y, x + each, y + 12, fill);
            Component text = label(standing);
            int color = lit ? 0xFFFFFFFF : over ? 0xFFE0E0E0 : 0xFF8A8A8A;
            graphics.drawString(this.font, text, x + (each - this.font.width(text)) / 2, y + 2, color, false);
            if (standing == own) {
                graphics.fill(x + 2, y + 11, x + each - 2, y + 12, 0xFF000000 | standing.rgb());
            }
            if (over) {
                tip = Component.translatable(PREFIX + "mobs.tip." + standing.name().toLowerCase(Locale.ROOT),
                        actor.name(), target.name());
            }
            if (editable && !lit) {
                this.area.hit(x, y, each, 12, () -> this.choose(actor, target, standing));
            }
            x += each + 2;
        }
        return new Tip(left, tip);
    }

    private static Component label(Standing standing) {
        return Component.translatable(PREFIX + "mobs." + standing.name().toLowerCase(Locale.ROOT));
    }

    private static int dim(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int g = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return r << 16 | g << 8 | b;
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

    // Esc first goes back from a creature to the list.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.picked != null) {
            this.pick(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
