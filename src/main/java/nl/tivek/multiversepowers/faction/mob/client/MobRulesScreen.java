package nl.tivek.multiversepowers.faction.mob.client;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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
// game's own first) that folds in and out; every group and the list as shown take a standing for all at once, and over
// the list are everyone hostile, a step back and a reset of it all. A click on a creature opens it: its model on the
// left, and in tabs what it is (how it stands to players and to every creature, its health, armor, damage, speed and
// more), its numbers to change (`MobStats`), and how it stands to each creature and the player, a line under the
// game's own. A change goes to the server at once (`MobRules`).
public final class MobRulesScreen extends MobRulesProfile {
    private static final int SEARCH = 110;
    private static final int TIP_WIDTH = 220;
    private boolean asked;
    @Nullable
    private EditBox search;
    // A number's box opened to type into, where its row lies this frame.
    @Nullable
    private EditBox typing;
    @Nullable
    private MobStats.Stat typingStat;
    @Nullable
    private Row typingCreature;
    private int typingX;
    private int typingY;
    private int typingWidth;
    private boolean typingShown;
    // The relation whose three choices are open: from the mouse reaching its pill until it leaves them.
    @Nullable
    private Row choosing;

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

    @Override
    public void removed() {
        super.removed();
        this.preview.forget();
    }

    private void startTyping(MobStats.Stat stat, double now) {
        this.stopTyping();
        EditBox box = new EditBox(this.font, 0, 0, 34, 12, Component.translatable(PREFIX + "mobs.stat." + stat.key));
        box.setMaxLength(12);
        box.setFilter(text -> text.matches("[0-9.,x×*/÷+%\\- ]*"));
        box.setValue(value(stat, now).getString().replaceAll("[^0-9.\\-]", ""));
        box.moveCursorToEnd(false);
        box.setHighlightPos(0);
        box.visible = false;
        this.typing = box;
        this.typingStat = stat;
        this.typingCreature = this.picked;
        this.addRenderableWidget(box);
        this.setFocused(box);
    }

    private void stopTyping() {
        if (this.typing != null) {
            this.removeWidget(this.typing);
        }
        this.typing = null;
        this.typingStat = null;
        this.typingCreature = null;
    }

    private void applyTyping() {
        if (this.typing != null && this.typingStat != null && this.typingCreature != null) {
            Row creature = this.typingCreature;
            MobStats.Stat stat = this.typingStat;
            double now = this.stat(creature, stat);
            OptionalDouble value = typed(this.typing.getValue(), stat, now);
            if (value.isPresent()) {
                this.stat(creature, stat, value.getAsDouble());
            }
        }
        this.stopTyping();
    }

    // The open box over its number's row, hidden while the row is scrolled out of the list.
    private void placeTyping() {
        if (this.typing == null) {
            return;
        }
        if (this.picked != this.typingCreature || this.tab != NUMBERS) {
            this.stopTyping();
            return;
        }
        this.typing.visible = this.typingShown;
        this.typing.setX(this.typingX);
        this.typing.setY(this.typingY);
        this.typing.setWidth(this.typingWidth);
    }

    // What was typed into a number's box: its value as the box shows it, *2 or /2 to multiply or divide what it is
    // now. An x before the number reads as the box's own × on a multiplier and as *, multiplying, on anything else.
    static OptionalDouble typed(String text, MobStats.Stat stat, double now) {
        String clean = text.trim().toLowerCase(Locale.ROOT).replace(',', '.').replace('÷', '/').replace(" ", "")
                .replace("/s", "").replace("%", "").replace("+", "");
        boolean times = clean.startsWith("*") || clean.endsWith("x")
                || (clean.startsWith("x") || clean.startsWith("×")) && stat.kind != MobStats.Kind.TIMES;
        boolean divide = clean.startsWith("/");
        clean = clean.replaceAll("^[x×*/]+|x$", "");
        try {
            double number = Double.parseDouble(clean);
            if (!Double.isFinite(number) || divide && number <= 0.0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(times ? now * number : divide ? now / number : number);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    private void pick(@Nullable Row row) {
        this.stopTyping();
        this.choosing = null;
        this.picked = row;
        this.tab = OVERVIEW;
        this.area.top();
        this.query = "";
        if (this.search != null) {
            this.search.setValue("");
        }
    }

    // Friendly, Neutral and Hostile for every one of `rows` at once, right-aligned to `right`; the lit one how they all
    // stand, if they do alike. Returns its left edge.
    private int allPills(GuiGraphics graphics, List<Row> rows, int right, int y, int mouseX, int mouseY) {
        return this.standingPills(graphics, this.common(rows), right, y, mouseX, mouseY,
                standing -> this.all(rows, standing));
    }

    // All shown at once: the creatures the list shows now, the player too while one is picked. Returns what it does
    // while the mouse is on its name.
    @Nullable
    private Component allLine(GuiGraphics graphics, Line line, int y, int width, int mouseX, int mouseY) {
        graphics.fill(this.listX, y + 1, this.listX + width, y + ROW - 1, 0x2A6EE7A0);
        int left = this.allPills(graphics, line.members(), this.listX + width - 4, y + 3, mouseX, mouseY);
        Component title = Component.translatable(PREFIX + (this.picked != null ? "mobs.all.toward" : "mobs.all"),
                line.members().size()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(title, left - this.listX - 10), this.listX + 5, y + 5,
                0xFF000000 | ACCENT, false);
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, left - this.listX, ROW);
        return over ? Component.translatable(PREFIX + (this.picked != null ? "mobs.all.toward.tip" : "mobs.all.tip"))
                : null;
    }

    // One of the picked creature's numbers: its name and what it makes of the creature, then - value + and a way back
    // to the game's own.
    @Nullable
    private Component numberLine(GuiGraphics graphics, MobStats.Stat stat, int y, int width, int mouseX,
            int mouseY) {
        Row creature = this.picked;
        double now = this.stat(creature, stat);
        boolean editable = this.editable();
        int right = this.listX + width - 4;
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
            Component twice = Component.literal("×2");
            right -= this.font.width(twice) + 6;
            this.button(graphics, twice, right, y + 4, mouseX, mouseY, editable && now > 0.0 && now < stat.most,
                    () -> this.stat(creature, stat, now * 2.0));
            Component plus = Component.literal("+");
            right -= this.font.width(plus) + 8;
            this.button(graphics, plus, right, y + 4, mouseX, mouseY, editable && now < stat.most,
                    () -> this.stat(creature, stat, now + stat.step * (hasShiftDown() ? 10.0 : 1.0)));
            Component shown = value(stat, now);
            int box = Math.max(34, this.font.width(shown) + 8);
            right -= box + 2;
            boolean typing = this.typing != null && this.typingStat == stat;
            boolean overBox = editable && this.area.over(mouseX, mouseY, right, y + 3, box, 12);
            graphics.fill(right, y + 3, right + box, y + 15, overBox && !typing ? 0xFF2A2A2A : 0xFF161616);
            if (typing) {
                this.typingX = right;
                this.typingY = y + 3;
                this.typingWidth = box;
                this.typingShown = this.area.whole(y + 3, 12);
            } else {
                graphics.drawString(this.font, shown, right + (box - this.font.width(shown)) / 2, y + 5,
                        now != stat.normal ? 0xFF000000 | WARN : BODY, false);
                if (editable) {
                    this.area.hit(right, y + 3, box, 12, () -> this.startTyping(stat, now));
                }
            }
            Component minus = Component.literal("-");
            right -= this.font.width(minus) + 8;
            this.button(graphics, minus, right, y + 4, mouseX, mouseY, editable && now > stat.least,
                    () -> this.stat(creature, stat, now - stat.step * (hasShiftDown() ? 10.0 : 1.0)));
            Component half = Component.literal("÷2");
            right -= this.font.width(half) + 8;
            this.button(graphics, half, right, y + 4, mouseX, mouseY, editable && now > stat.least,
                    () -> this.stat(creature, stat, now / 2.0));
            if (overBox && !typing) {
                return Component.translatable(PREFIX + "mobs.type.tip");
            }
        }
        LivingEntity living = this.preview.of(creature.type());
        Component result = MobFacts.result(stat, living, this.stats(creature));
        Component name = Component.translatable(PREFIX + "mobs.stat." + stat.key);
        // What it makes of the creature only where the name still fits whole beside it.
        if (result != null && this.font.width(name) + this.font.width(result) + 18 <= right - this.listX) {
            right -= this.font.width(result) + 6;
            graphics.drawString(this.font, result, right, y + 5, MUTED, false);
        }
        graphics.drawString(this.font, this.fit(name, right - this.listX - 10), this.listX + 5, y + 5,
                now != stat.normal ? TEXT : BODY, false);
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, right - this.listX - 4, ROW);
        return over ? Component.translatable(PREFIX + "mobs.stat." + stat.key + ".desc") : null;
    }

    // A group's head: all of it at once, and a click elsewhere on it folds the group in or out.
    private void groupLine(GuiGraphics graphics, Line line, int y, int width, int mouseX, int mouseY) {
        boolean folded = FOLDED.contains(line.group()) && this.query.isBlank();
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
        graphics.fill(this.listX, y + 1, this.listX + width, y + ROW - 1, over ? 0x30FFFFFF : 0x22FFFFFF);
        graphics.drawString(this.font, folded ? "▸" : "▾", this.listX + 5, y + 5, 0xFF000000 | COLOR, false);
        String count = String.valueOf(line.members().size());
        int countX = this.listX + width - 6 - this.font.width(count);
        graphics.drawString(this.font, count, countX, y + 5, MUTED, false);
        // The whole group at once, only while the mouse is on its head, so the list stays calm.
        int left = over && this.editable() ? this.allPills(graphics, line.members(), countX - 8, y + 3, mouseX, mouseY)
                : countX;
        Component title = groupName(line.group()).copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(title, left - this.listX - 22), this.listX + 16, y + 5,
                0xFF000000 | COLOR, false);
        this.area.hit(this.listX, y, width, ROW, () -> {
            if (!FOLDED.remove(line.group())) {
                FOLDED.add(line.group());
            }
        });
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hits.clear();
        this.typingShown = false;
        // A page of the settings window, as the tour knows it.
        ScreenAnchors.report("settings", this.contentX, this.contentY, this.contentWidth, this.contentHeight);
        if (this.search != null) {
            this.search.visible = this.picked == null || this.tab == RELATIONS;
        }
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
        int bottom = this.contentY + this.contentHeight;
        this.listX = this.contentX;
        this.listWidth = this.contentWidth;
        if (this.picked != null) {
            top = this.profile(graphics, top, bottom, mouseX, mouseY);
        }
        int height = bottom - top;
        List<Line> rows = this.shownLines();
        ScreenAnchors.report("mobs.list", this.listX, top, this.listWidth, height);
        if (rows.isEmpty()) {
            Component none = Component.translatable(PREFIX + "search.none");
            graphics.drawString(this.font, none, this.listX + (this.listWidth - this.font.width(none)) / 2,
                    top + 8, MUTED, false);
            return;
        }
        Component tip = null;
        int start = this.area.begin(graphics, this.listX, top, this.listWidth, height, rows.size() * ROW);
        int width = this.area.rowWidth();
        for (int i = 0; i < rows.size(); i++) {
            int y = start + i * ROW;
            if (!this.area.shows(y, ROW)) {
                continue;
            }
            Component about = this.line(graphics, rows.get(i), i, y, width, mouseX, mouseY);
            tip = about != null ? about : tip;
        }
        this.area.end(graphics);
        this.placeTyping();
        if (tip != null) {
            // Split into its own lines: a line break inside one tooltip line shows as a glyph.
            graphics.renderTooltip(this.font, this.font.split(tip, TIP_WIDTH), mouseX, mouseY);
        }
    }

    // One line of the list; what it tells while the mouse is on it.
    @Nullable
    private Component line(GuiGraphics graphics, Line line, int i, int y, int width, int mouseX, int mouseY) {
        switch (line.kind()) {
            case GROUP -> {
                this.groupLine(graphics, line, y, width, mouseX, mouseY);
                return null;
            }
            case ALL -> {
                return this.allLine(graphics, line, y, width, mouseX, mouseY);
            }
            case HEADING -> {
                this.headingLine(graphics, line.title(), y, width);
                return null;
            }
            case NUMBER -> {
                if (i % 2 == 0) {
                    graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x10FFFFFF);
                }
                return this.numberLine(graphics, line.stat(), y, width, mouseX, mouseY);
            }
            case FACT -> {
                return this.factLine(graphics, line.fact(), y, width, mouseX, mouseY, i % 2 == 0);
            }
            case TOWARD_PLAYERS -> {
                return this.towardPlayersLine(graphics, y, width, mouseX, mouseY);
            }
            case TOWARD_ALL -> {
                return this.towardAllLine(graphics, y, width, mouseX, mouseY);
            }
            case SUMMARY -> {
                this.summaryLine(graphics, y, width, mouseX, mouseY);
                return null;
            }
            default -> {
                return this.creatureLine(graphics, line.row(), i, y, width, mouseX, mouseY);
            }
        }
    }

    // A creature: in the list a way into it, with what this world changed of it; in a picked one's relations how
    // the picked one stands to it.
    @Nullable
    private Component creatureLine(GuiGraphics graphics, Row row, int i, int y, int width, int mouseX, int mouseY) {
        Component tip = null;
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
        if (i % 2 == 0) {
            graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x10FFFFFF);
        }
        graphics.renderItem(row.icon(), this.listX + 2, y + 1);
        int right = this.listX + width - 4;
        if (this.picked == null) {
            if (over) {
                graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x18FFFFFF);
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
            Standing toPlayers = this.shown(row, this.player);
            graphics.fill(right - 6, y + 6, right - 1, y + 11, 0xFF000000 | toPlayers.rgb());
            if (this.area.over(mouseX, mouseY, right - 8, y, 9, ROW)) {
                tip = Component.translatable(PREFIX + "mobs.dot", label(toPlayers));
            }
            right -= 10;
            this.area.hit(this.listX, y, width, ROW, () -> this.pick(row));
        } else {
            // The choices open once the mouse reaches the pill and close when it leaves them, so the name beside
            // them stays a way into its own page.
            boolean named = row != this.player && row != this.picked;
            if (over && mouseX >= right - this.pillWidth()) {
                this.choosing = row;
            } else if (this.choosing == row && !(over && mouseX >= this.pillsLeft(right))) {
                this.choosing = null;
            }
            Tip pills = this.pills(graphics, row, right, y + 3, mouseX, mouseY,
                    over && (!named || this.choosing == row));
            right = pills.left() - 4;
            if (pills.tip() != null) {
                tip = pills.tip();
            }
            if (this.shown(this.picked, row) != ClientMobTable.of(this.picked.index(), row.index())) {
                graphics.drawString(this.font, "•", right - 4, y + 5, 0xFF000000 | WARN, false);
                right -= 8;
            }
            // Another creature's name opens its own page.
            if (named) {
                boolean overName = this.area.over(mouseX, mouseY, this.listX, y, right - this.listX, ROW);
                if (overName) {
                    graphics.fill(this.listX, y, right, y + ROW, 0x18FFFFFF);
                    tip = tip != null ? tip : Component.translatable(PREFIX + "mobs.open", row.name());
                }
                this.area.hit(this.listX, y, right - this.listX, ROW, () -> this.pick(row));
                graphics.drawString(this.font, this.fit(row.name(), right - this.listX - 26), this.listX + 22,
                        y + 5, overName ? TEXT : BODY, false);
                return tip;
            }
        }
        graphics.drawString(this.font, this.fit(row.name(), right - this.listX - 26), this.listX + 22,
                y + 5, over && this.picked == null ? TEXT : BODY, false);
        return tip;
    }

    // Over the list: what to do with everyone hostile, a step back and a reset of the whole page; or the picked
    // creature with a way back, whether it spawns and its reset.
    private void header(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.contentY + 3;
        boolean editable = this.editable();
        if (this.picked == null) {
            int right = this.contentX + this.contentWidth;
            Component reset = Component.translatable(PREFIX + "mobs.reset_all");
            right -= this.font.width(reset) + 6;
            this.smallButton(graphics, reset, right, y - 1, mouseX, mouseY, editable, () -> this.page(
                    MobPageActionPayload.RESET));
            if (ClientMobTable.steps() > 0) {
                Component undo = Component.translatable(PREFIX + "mobs.undo", ClientMobTable.steps());
                right -= this.font.width(undo) + 10;
                this.smallButton(graphics, undo, right, y - 1, mouseX, mouseY, editable,
                        () -> this.page(MobPageActionPayload.UNDO));
            }
            Component everyone = Component.translatable(PREFIX + "mobs.everyone."
                    + (MobRules.everyoneHostile() ? "on" : "off"));
            right -= this.font.width(everyone) + 10;
            this.smallButton(graphics, everyone, right, y - 1, mouseX, mouseY, editable,
                    () -> this.page(MobPageActionPayload.EVERYONE));
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "mobs.pick"),
                    right - this.contentX - 8), this.contentX, y, MUTED, false);
            return;
        }
        Row actor = this.picked;
        int x = this.contentX;
        x += this.link(graphics, Component.translatable(PREFIX + "mobs.back"), x, y, mouseX, mouseY, MUTED,
                () -> this.pick(null)) + 8;
        Component reset = Component.translatable(PREFIX + "mobs.reset");
        int resetX = this.contentX + this.contentWidth - this.font.width(reset) - 6;
        this.smallButton(graphics, reset, resetX, y - 1, mouseX, mouseY, editable && this.changes(actor) > 0,
                () -> this.reset(actor));
        boolean spawns = this.spawns(actor);
        Component spawning = Component.translatable(PREFIX + "mobs.spawns." + (spawns ? "on" : "off"));
        int spawnX = resetX - this.font.width(spawning) - 14;
        this.smallButton(graphics, spawning, spawnX, y - 1, mouseX, mouseY, editable,
                () -> this.spawning(actor, !spawns));
        Component name = actor.name().copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, this.fit(name, spawnX - x - 8), x, y, 0xFF000000 | COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A click away from an open number box takes what was typed.
        if (this.typing != null && !(this.typing.visible && this.typing.isMouseOver(mouseX, mouseY))) {
            this.applyTyping();
        }
        if (button == 0 && this.area.press(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.clickHit(mouseX, mouseY)) {
            this.click(1.0F);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.overCard(mouseX, mouseY)) {
            this.preview.hold(true);
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
        if (this.preview.held()) {
            this.preview.drag(dragX);
            return true;
        }
        return this.area.drag(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.preview.hold(false);
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
        if (this.typing != null && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            this.applyTyping();
            return true;
        }
        if (this.typing != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.stopTyping();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.picked != null) {
            this.pick(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
