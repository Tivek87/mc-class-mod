package nl.tivek.multiversepowers.faction.mob.client;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobPageActionPayload;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.mob.MobStats;
import nl.tivek.multiversepowers.faction.mob.MobTableAskPayload;
import org.lwjgl.glfw.GLFW;

// The settings window's Mobs page: every creature of the game, modded ones too, searchable, in a group per mod (the
// game's own first) that folds in and out; a click on one lists every creature and the player again, each with
// Friendly, Neutral and Hostile, the one lit being how the first stands to it, and whether it spawns by itself, and its
// numbers (`MobStats`). A line under one marks the game's own. Every group and the list as shown take a standing for
// all at once (in the list: toward every creature); over the list, everyone hostile, a step back and a reset of it all.
// A change goes to the server at once (`MobRules`).
public final class MobRulesScreen extends MobRulesState {
    private static final int SEARCH = 110;
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

    private void pick(@Nullable Row row) {
        this.picked = row;
        this.numbers = false;
        this.area.top();
        this.query = "";
        if (this.search != null) {
            this.search.setValue("");
        }
    }

    // Friendly, Neutral and Hostile for every one of `rows` at once, right-aligned to `right`; the lit one how they all
    // stand, if they do alike. Returns its left edge.
    private int allPills(GuiGraphics graphics, List<Row> rows, int right, int y, int mouseX, int mouseY) {
        Standing now = this.common(rows);
        int each = 0;
        for (Standing standing : ORDER) {
            each = Math.max(each, this.font.width(label(standing)) + 8);
        }
        int x = right - each * ORDER.length - 2 * (ORDER.length - 1);
        int left = x;
        boolean editable = this.editable();
        for (Standing standing : ORDER) {
            boolean lit = standing == now;
            boolean over = editable && this.area.over(mouseX, mouseY, x, y, each, 12);
            graphics.fill(x, y, x + each, y + 12, lit ? 0xFF000000 | dim(standing.rgb()) : over ? 0xFF404040
                    : 0xFF1E1E1E);
            graphics.renderOutline(x, y, each, 12, 0xFF000000 | dim(dim(standing.rgb())));
            Component text = label(standing);
            graphics.drawString(this.font, text, x + (each - this.font.width(text)) / 2, y + 2,
                    lit ? 0xFFFFFFFF : over ? 0xFFE0E0E0 : 0xFF9A9A9A, false);
            if (editable) {
                this.area.hit(x, y, each, 12, () -> this.all(rows, standing));
            }
            x += each + 2;
        }
        return left;
    }

    // All shown at once: the creatures the list shows now, the player too while one is picked. Returns what it does
    // while the mouse is on its name.
    @Nullable
    private Component allLine(GuiGraphics graphics, Line line, int y, int width, int mouseX, int mouseY) {
        graphics.fill(this.contentX, y + 1, this.contentX + width, y + ROW - 1, 0x2A6EE7A0);
        int left = this.allPills(graphics, line.members(), this.contentX + width - 4, y + 3, mouseX, mouseY);
        Component title = Component.translatable(PREFIX + (this.picked != null ? "mobs.all.toward" : "mobs.all"),
                line.members().size()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(title, left - this.contentX - 10), this.contentX + 5, y + 5,
                0xFF000000 | ACCENT, false);
        boolean over = this.area.over(mouseX, mouseY, this.contentX, y, left - this.contentX, ROW);
        return over ? Component.translatable(PREFIX + (this.picked != null ? "mobs.all.toward.tip" : "mobs.all.tip"))
                : null;
    }

    // The way to the picked creature's numbers.
    private void numbersLine(GuiGraphics graphics, int y, int width, int mouseX, int mouseY) {
        boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, ROW);
        graphics.fill(this.contentX, y, this.contentX + width, y + ROW, over ? 0x28FFFFFF : 0x14FFFFFF);
        graphics.renderItem(new ItemStack(Items.COMPARATOR), this.contentX + 2, y + 1);
        int right = this.contentX + width - 4;
        graphics.drawString(this.font, "›", right - 4, y + 5, over ? TEXT : MUTED, false);
        right -= 10;
        int changed = MobStats.changed(this.picked.type());
        if (changed > 0) {
            Component count = Component.translatable(PREFIX + "mobs.changed", changed);
            right -= this.font.width(count) + 6;
            this.chip(graphics, count, right, y + 4, 0x3A3010, WARN);
        }
        graphics.drawString(this.font, Component.translatable(PREFIX + "mobs.numbers"), this.contentX + 22, y + 5,
                over ? TEXT : BODY, false);
        this.area.hit(this.contentX, y, width, ROW, () -> {
            this.numbers = true;
            this.area.top();
        });
    }

    // One of the picked creature's numbers: its name, then - value + and a way back to the game's own.
    @Nullable
    private Component numberLine(GuiGraphics graphics, MobStats.Stat stat, int y, int width, int mouseX,
            int mouseY) {
        Row creature = this.picked;
        double now = this.stat(creature, stat);
        boolean editable = this.editable();
        int right = this.contentX + width - 4;
        Component back = Component.translatable(PREFIX + "mobs.reset");
        right -= this.font.width(back) + 6;
        if (now != stat.normal) {
            this.button(graphics, back, right, y + 4, mouseX, mouseY, editable, () -> this.stat(creature, stat,
                    stat.normal));
        }
        right -= 6;
        if (stat.kind == MobStats.Kind.SWITCH) {
            Component flip = value(stat, now);
            right -= this.font.width(flip) + 6;
            this.button(graphics, flip, right, y + 4, mouseX, mouseY, editable, () -> this.stat(creature, stat,
                    now >= 0.5 ? 0.0 : 1.0));
        } else {
            Component plus = Component.literal("+");
            right -= this.font.width(plus) + 6;
            this.button(graphics, plus, right, y + 4, mouseX, mouseY, editable && now < stat.most,
                    () -> this.stat(creature, stat, now + stat.step * (hasShiftDown() ? 10.0 : 1.0)));
            Component shown = value(stat, now);
            int box = Math.max(44, this.font.width(shown) + 8);
            right -= box + 2;
            graphics.fill(right, y + 3, right + box, y + 15, 0xFF161616);
            graphics.drawString(this.font, shown, right + (box - this.font.width(shown)) / 2, y + 5,
                    now != stat.normal ? 0xFF000000 | WARN : BODY, false);
            Component minus = Component.literal("-");
            right -= this.font.width(minus) + 8;
            this.button(graphics, minus, right, y + 4, mouseX, mouseY, editable && now > stat.least,
                    () -> this.stat(creature, stat, now - stat.step * (hasShiftDown() ? 10.0 : 1.0)));
        }
        Component name = Component.translatable(PREFIX + "mobs.stat." + stat.key);
        graphics.drawString(this.font, this.fit(name, right - this.contentX - 12), this.contentX + 5, y + 5,
                now != stat.normal ? TEXT : BODY, false);
        boolean over = this.area.over(mouseX, mouseY, this.contentX, y, right - this.contentX - 4, ROW);
        return over ? Component.translatable(PREFIX + "mobs.stat." + stat.key + ".desc") : null;
    }

    // A chip inside the list that scrolls with it.
    private void button(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, boolean active,
            Runnable action) {
        int width = this.font.width(text) + 6;
        boolean over = active && this.area.over(mouseX, mouseY, x, y, width, 10);
        this.chip(graphics, text, x, y, over ? 0x505050 : 0x343434, !active ? 0x6A6A6A : over ? 0xFFFFFF : 0xC8C8C8);
        if (active) {
            this.area.hit(x, y, width, 10, action);
        }
    }

    // A group's head: all of it at once, and a click elsewhere on it folds the group in or out.
    private void groupLine(GuiGraphics graphics, Line line, int y, int width, int mouseX, int mouseY) {
        boolean folded = FOLDED.contains(line.group()) && this.query.isBlank();
        boolean over = this.area.over(mouseX, mouseY, this.contentX, y, width, ROW);
        graphics.fill(this.contentX, y + 1, this.contentX + width, y + ROW - 1, over ? 0x30FFFFFF : 0x22FFFFFF);
        graphics.drawString(this.font, folded ? "▸" : "▾", this.contentX + 5, y + 5, 0xFF000000 | COLOR, false);
        String count = String.valueOf(line.members().size());
        int countX = this.contentX + width - 6 - this.font.width(count);
        graphics.drawString(this.font, count, countX, y + 5, MUTED, false);
        int left = this.allPills(graphics, line.members(), countX - 8, y + 3, mouseX, mouseY);
        Component title = groupName(line.group()).copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(title, left - this.contentX - 22), this.contentX + 16, y + 5,
                0xFF000000 | COLOR, false);
        this.area.hit(this.contentX, y, width, ROW, () -> {
            if (!FOLDED.remove(line.group())) {
                FOLDED.add(line.group());
            }
        });
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
        int stamp = MobRules.stamp();
        if (stamp != this.stamp) {
            this.stamp = stamp;
            this.sent.clear();
            this.sentSpawn.clear();
            this.sentAny.clear();
            this.sentStat.clear();
        }
        this.header(graphics, mouseX, mouseY);
        int top = this.contentY + HEADER + 4;
        int height = this.contentY + this.contentHeight - top;
        List<Line> rows = this.shownLines();
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
            Line line = rows.get(i);
            if (line.kind() == GROUP) {
                this.groupLine(graphics, line, y, width, mouseX, mouseY);
                continue;
            }
            if (line.kind() == ALL) {
                Component about = this.allLine(graphics, line, y, width, mouseX, mouseY);
                tip = about != null ? about : tip;
                continue;
            }
            if (line.kind() == NUMBERS) {
                this.numbersLine(graphics, y, width, mouseX, mouseY);
                continue;
            }
            if (line.kind() == NUMBER) {
                if (i % 2 == 0) {
                    graphics.fill(this.contentX, y, this.contentX + width, y + ROW, 0x10FFFFFF);
                }
                Component about = this.numberLine(graphics, line.stat(), y, width, mouseX, mouseY);
                tip = about != null ? about : tip;
                continue;
            }
            Row row = line.row();
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
                if (!this.spawns(row)) {
                    Component off = Component.translatable(PREFIX + "mobs.no_spawn");
                    right -= this.font.width(off) + 6;
                    this.chip(graphics, off, right, y + 4, 0x3A1414, 0xFF7A7A);
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

    // Over the list: what to do with everyone hostile, a step back and a reset of the whole page; or the picked
    // creature with a way back and its reset; or its numbers with a way back to who it attacks.
    private void header(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.contentY + 3;
        boolean editable = this.editable();
        if (this.picked == null) {
            int right = this.contentX + this.contentWidth;
            Component reset = Component.translatable(PREFIX + "mobs.reset_all");
            right -= this.font.width(reset) + 6;
            this.smallButton(graphics, reset, right, y - 1, mouseX, mouseY, editable, () -> this.page(
                    MobPageActionPayload.RESET));
            Component undo = Component.translatable(PREFIX + "mobs.undo", ClientMobTable.steps());
            right -= this.font.width(undo) + 10;
            this.smallButton(graphics, undo, right, y - 1, mouseX, mouseY, editable && ClientMobTable.steps() > 0,
                    () -> this.page(MobPageActionPayload.UNDO));
            Component everyone = Component.translatable(PREFIX + "mobs.everyone."
                    + (MobRules.everyoneHostile() ? "on" : "off"));
            right -= this.font.width(everyone) + 10;
            this.smallButton(graphics, everyone, right, y - 1, mouseX, mouseY, editable,
                    () -> this.page(MobPageActionPayload.EVERYONE));
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "mobs.pick", this.creatures.size()),
                    right - this.contentX - 8), this.contentX, y, MUTED, false);
            return;
        }
        Row actor = this.picked;
        int x = this.contentX;
        if (this.numbers) {
            x += this.link(graphics, Component.translatable(PREFIX + "mobs.numbers.back"), x, y, mouseX, mouseY, MUTED,
                    () -> {
                        this.numbers = false;
                        this.area.top();
                    }) + 8;
            Component reset = Component.translatable(PREFIX + "mobs.reset");
            int resetX = this.contentX + this.contentWidth - this.font.width(reset) - 6;
            this.smallButton(graphics, reset, resetX, y - 1, mouseX, mouseY,
                    editable && MobStats.changed(actor.type()) > 0, () -> this.resetNumbers(actor));
            Component name = Component.translatable(PREFIX + "mobs.numbers.of", actor.name())
                    .withStyle(ChatFormatting.BOLD);
            graphics.drawString(this.font, this.fit(name, resetX - x - 8), x, y, 0xFF000000 | COLOR, false);
            return;
        }
        x += this.link(graphics, Component.translatable(PREFIX + "mobs.back"), x, y, mouseX, mouseY, MUTED,
                () -> this.pick(null)) + 8;
        Component reset = Component.translatable(PREFIX + "mobs.reset");
        int resetX = this.contentX + this.contentWidth - this.font.width(reset) - 6;
        this.smallButton(graphics, reset, resetX, y - 1, mouseX, mouseY, this.editable() && this.changes(actor) > 0,
                () -> this.reset(actor));
        boolean spawns = this.spawns(actor);
        Component spawning = Component.translatable(PREFIX + "mobs.spawns." + (spawns ? "on" : "off"));
        int spawnX = resetX - this.font.width(spawning) - 14;
        this.smallButton(graphics, spawning, spawnX, y - 1, mouseX, mouseY, this.editable(),
                () -> this.spawning(actor, !spawns));
        Component name = Component.translatable(PREFIX + "mobs.toward", actor.name()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(name, spawnX - x - 8), x, y, 0xFF000000 | COLOR, false);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        this.area.scroll(scrollY, ROW);
        return true;
    }

    // Esc first goes back from a creature to the list.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.picked != null) {
            if (this.numbers) {
                this.numbers = false;
                this.area.top();
            } else {
                this.pick(null);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
