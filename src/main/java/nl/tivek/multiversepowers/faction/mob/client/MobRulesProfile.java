package nl.tivek.multiversepowers.faction.mob.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobStats;

// The Mobs page's picked creature under `MobRulesScreen`: its model turning in a card on the left (dragged to turn
// it), its tabs over the list on the right, and the lines of its overview: how it stands to players and to every
// creature, how many it is hostile to, and what it is (`MobFacts`). Also the Friendly, Neutral and Hostile pills the
// whole page uses.
abstract class MobRulesProfile extends MobRulesState {
    static final int TAB_HEIGHT = 14;
    private static final String[] TABS = { "overview", "numbers", "relations" };

    // Something outside the scrolling list that a click runs, kept per frame.
    private record Hit(int x, int y, int width, int height, Runnable action) {
    }

    record Tip(int left, @Nullable Component tip) {
    }

    final List<Hit> hits = new ArrayList<>();
    // Where the list lies this frame: the whole page, or right of the picked creature's model.
    int listX;
    int listWidth;
    private int cardX;
    private int cardY;
    private int cardWidth;
    private int cardHeight;

    MobRulesProfile(Component title, @Nullable Screen root, String page) {
        super(title, root, page);
    }

    void pickTab(int tab) {
        this.tab = tab;
        this.area.top();
    }

    // The picked creature's card and tabs between `top` and `bottom`; returns where its list starts.
    int profile(GuiGraphics graphics, int top, int bottom, int mouseX, int mouseY) {
        Row actor = this.picked;
        int width = Mth.clamp(this.contentWidth * 3 / 10, 84, 130);
        this.cardX = this.contentX;
        this.cardY = top;
        this.cardWidth = width;
        this.cardHeight = bottom - top;
        card(graphics, this.cardX, top, width, this.cardHeight, COLOR);
        boolean over = this.overCard(mouseX, mouseY);
        if (over || this.preview.held()) {
            graphics.fill(this.cardX + 2, top, this.cardX + width, bottom, 0x0CFFFFFF);
        }
        if (!this.preview.draw(graphics, actor.type(), this.cardX + 3, top + 14, width - 4, this.cardHeight - 26)) {
            graphics.pose().pushPose();
            graphics.pose().translate(this.cardX + width / 2.0F - 16, top + this.cardHeight / 2.0F - 16, 0);
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            graphics.renderItem(actor.icon(), 0, 0);
            graphics.pose().popPose();
        }
        graphics.drawString(this.font, this.fit(groupName(actor.group()), width - 10), this.cardX + 6, top + 4,
                MUTED, false);
        Component hint = Component.translatable(PREFIX + "mobs.preview.drag");
        graphics.drawString(this.font, this.fit(hint, width - 10), this.cardX + 6, bottom - 11,
                over || this.preview.held() ? BODY : DIM, false);
        ScreenAnchors.report("mobs.preview", this.cardX, top, width, this.cardHeight);
        this.listX = this.cardX + width + 6;
        this.listWidth = this.contentWidth - width - 6;
        this.tabs(graphics, actor, this.listX, top, this.listWidth, mouseX, mouseY);
        return top + TAB_HEIGHT + 4;
    }

    boolean overCard(double mouseX, double mouseY) {
        return this.picked != null && mouseX >= this.cardX && mouseX < this.cardX + this.cardWidth
                && mouseY >= this.cardY && mouseY < this.cardY + this.cardHeight;
    }

    private void tabs(GuiGraphics graphics, Row actor, int left, int y, int width, int mouseX, int mouseY) {
        graphics.fill(left, y + TAB_HEIGHT - 1, left + width, y + TAB_HEIGHT, 0x1CFFFFFF);
        int x = left;
        for (int i = 0; i < TABS.length; i++) {
            Component label = Component.translatable(PREFIX + "mobs.tab." + TABS[i]);
            int count = i == NUMBERS ? this.numbersChanged(actor) : i == RELATIONS ? this.relationsChanged(actor) : 0;
            Component chip = count > 0 ? Component.literal(String.valueOf(count)) : null;
            int each = this.font.width(label) + 12 + (chip == null ? 0 : this.font.width(chip) + 10);
            boolean lit = this.tab == i;
            boolean over = !lit && mouseX >= x && mouseX < x + each && mouseY >= y && mouseY < y + TAB_HEIGHT;
            if (lit) {
                graphics.fill(x, y, x + each, y + TAB_HEIGHT, 0x26FFFFFF);
                graphics.fill(x, y + TAB_HEIGHT - 1, x + each, y + TAB_HEIGHT, 0xFF000000 | COLOR);
            } else if (over) {
                graphics.fill(x, y, x + each, y + TAB_HEIGHT, 0x14FFFFFF);
            }
            graphics.drawString(this.font, label, x + 6, y + 3, lit || over ? TEXT : BODY, false);
            if (chip != null) {
                this.chip(graphics, chip, x + 10 + this.font.width(label), y + 2, 0x3A3010, WARN);
            }
            int which = i;
            if (!lit) {
                this.hits.add(new Hit(x, y, each, TAB_HEIGHT, () -> this.pickTab(which)));
            }
            ScreenAnchors.report("mobs.tab." + TABS[i], x, y, each, TAB_HEIGHT);
            x += each;
        }
    }

    int numbersChanged(Row actor) {
        int changed = 0;
        for (MobStats.Stat stat : MobStats.Stat.values()) {
            changed += Math.abs(this.stat(actor, stat) - stat.normal) > 1.0E-9 ? 1 : 0;
        }
        return changed;
    }

    int relationsChanged(Row actor) {
        return Math.max(0, this.changes(actor) - MobStats.changed(actor.type()));
    }

    // Runs what was clicked outside the list; whether anything was.
    boolean clickHit(double mouseX, double mouseY) {
        for (Hit hit : List.copyOf(this.hits)) {
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.width() && mouseY >= hit.y()
                    && mouseY < hit.y() + hit.height()) {
                hit.action().run();
                return true;
            }
        }
        return false;
    }

    // A section's title with a line under it.
    void headingLine(GuiGraphics graphics, Component title, int y, int width) {
        graphics.drawString(this.font, title.copy().withStyle(ChatFormatting.BOLD), this.listX + 3, y + 7,
                0xFF000000 | COLOR, false);
        graphics.fill(this.listX + 3, y + ROW - 2, this.listX + width - 3, y + ROW - 1, 0x26FFFFFF);
    }

    // How the picked creature stands to players, to pick from.
    @Nullable
    Component towardPlayersLine(GuiGraphics graphics, int y, int width, int mouseX, int mouseY) {
        Tip pills = this.pills(graphics, this.player, this.listX + width - 4, y + 3, mouseX, mouseY, true);
        graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "mobs.toward.players"),
                pills.left() - this.listX - 9), this.listX + 5, y + 5, BODY, false);
        return pills.tip();
    }

    // How the picked creature stands to every other kind at once, lit while one standing holds for all.
    @Nullable
    Component towardAllLine(GuiGraphics graphics, int y, int width, int mouseX, int mouseY) {
        Row actor = this.picked;
        List<Row> others = new ArrayList<>(this.creatures);
        others.remove(actor);
        Standing now = this.common(others);
        int left = this.standingPills(graphics, now, this.listX + width - 4, y + 3, mouseX, mouseY,
                standing -> this.everyCreature(actor, standing));
        Component name = Component.translatable(PREFIX + "mobs.toward.all");
        graphics.drawString(this.font, this.fit(name, left - this.listX - 9), this.listX + 5, y + 5, BODY, false);
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, left - this.listX, ROW);
        return over ? Component.translatable(PREFIX + "mobs.toward.all.tip") : null;
    }

    // How many it is hostile, neutral and friendly to; a click opens who.
    void summaryLine(GuiGraphics graphics, int y, int width, int mouseX, int mouseY) {
        int[] counts = this.counts(this.picked);
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
        if (over) {
            graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x18FFFFFF);
        }
        MutableComponent text = Component.empty();
        Standing[] order = { Standing.HOSTILE, Standing.NEUTRAL, Standing.FRIENDLY };
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                text.append(Component.literal(" · ").withColor(DIM));
            }
            text.append(Component.translatable(PREFIX + "mobs.count." + order[i].name().toLowerCase(Locale.ROOT),
                    Component.literal(String.valueOf(counts[order[i].ordinal()])).withColor(order[i].rgb())));
        }
        graphics.drawString(this.font, "›", this.listX + width - 8, y + 5, over ? TEXT : MUTED, false);
        graphics.drawString(this.font, this.fit(text, width - 18), this.listX + 5, y + 5, BODY, false);
        this.area.hit(this.listX, y, width, ROW, () -> this.pickTab(RELATIONS));
    }

    // One fact: its name, and what it is now on the right, marked while this world changed it; a number leads to its
    // place on the Numbers tab.
    @Nullable
    Component factLine(GuiGraphics graphics, MobFacts.Fact fact, int y, int width, int mouseX, int mouseY,
            boolean even) {
        boolean leads = fact.stat() != null && this.editable();
        boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
        if (even) {
            graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x10FFFFFF);
        }
        if (over && leads) {
            graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x18FFFFFF);
        }
        int right = this.listX + width - 5;
        if (leads) {
            graphics.drawString(this.font, "›", right - 3, y + 5, over ? TEXT : DIM, false);
            right -= 10;
        }
        int valueWidth = Math.min(this.font.width(fact.value()), width * 3 / 5);
        graphics.drawString(this.font, this.fit(fact.value(), valueWidth), right - valueWidth, y + 5,
                fact.changed() ? 0xFF000000 | WARN : TEXT, false);
        if (fact.changed()) {
            graphics.drawString(this.font, "•", right - valueWidth - 7, y + 5, 0xFF000000 | WARN, false);
        }
        graphics.drawString(this.font, this.fit(fact.name(), right - valueWidth - this.listX - 16), this.listX + 5,
                y + 5, BODY, false);
        if (leads) {
            this.area.hit(this.listX, y, width, ROW, () -> this.pickTab(NUMBERS));
        }
        if (!over || fact.tip() == null) {
            return null;
        }
        return leads ? fact.tip().copy().append("\n").append(Component.translatable(PREFIX + "mobs.fact.change")
                .withStyle(ChatFormatting.GRAY)) : fact.tip();
    }

    // Friendly, Neutral and Hostile side by side, right-aligned to `right`, `now` lit; returns their left edge.
    int standingPills(GuiGraphics graphics, @Nullable Standing now, int right, int y, int mouseX, int mouseY,
            Consumer<Standing> choose) {
        int each = pillWidth();
        int x = right - each * ORDER.length - 2 * (ORDER.length - 1);
        int left = x;
        boolean editable = this.editable();
        for (Standing standing : ORDER) {
            boolean lit = standing == now;
            boolean over = editable && this.area.over(mouseX, mouseY, x, y, each, 12);
            graphics.fill(x, y, x + each, y + 12, lit ? 0xFF000000 | dim(standing.rgb()) : over ? 0xFF404040
                    : 0xFF262626);
            Component text = label(standing);
            graphics.drawString(this.font, text, x + (each - this.font.width(text)) / 2, y + 2,
                    lit ? 0xFFFFFFFF : over ? 0xFFE0E0E0 : 0xFF8A8A8A, false);
            if (editable && !lit) {
                this.area.hit(x, y, each, 12, () -> choose.accept(standing));
            }
            x += each + 2;
        }
        return left;
    }

    int pillWidth() {
        int each = 0;
        for (Standing standing : ORDER) {
            each = Math.max(each, this.font.width(label(standing)) + 6);
        }
        return each;
    }

    // How the picked creature stands to one target, right-aligned to `right`: only that while the mouse is elsewhere;
    // on the row Friendly, Neutral and Hostile to pick from, the lit one how it stands, a line under the game's own.
    Tip pills(GuiGraphics graphics, Row target, int right, int y, int mouseX, int mouseY, boolean open) {
        Row actor = this.picked;
        Standing now = this.shown(actor, target);
        Standing own = ClientMobTable.of(actor.index(), target.index());
        int each = this.pillWidth();
        int x = right - each * ORDER.length - 2 * (ORDER.length - 1);
        int left = x;
        Component tip = null;
        boolean editable = this.editable();
        if (!open || !editable) {
            int at = right - each;
            graphics.fill(at, y, at + each, y + 12, 0xFF000000 | dim(now.rgb()));
            Component text = label(now);
            graphics.drawString(this.font, text, at + (each - this.font.width(text)) / 2, y + 2, 0xFFFFFFFF, false);
            return new Tip(at, null);
        }
        for (Standing standing : ORDER) {
            boolean lit = standing == now;
            boolean over = this.area.over(mouseX, mouseY, x, y, each, 12);
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
            if (!lit) {
                this.area.hit(x, y, each, 12, () -> this.choose(actor, target, standing));
            }
            x += each + 2;
        }
        return new Tip(left, tip);
    }

    // A chip inside the list that scrolls with it.
    void button(GuiGraphics graphics, Component text, int x, int y, int mouseX, int mouseY, boolean active,
            Runnable action) {
        int width = this.font.width(text) + 6;
        boolean over = active && this.area.over(mouseX, mouseY, x, y, width, 10);
        this.chip(graphics, text, x, y, over ? 0x505050 : 0x343434, !active ? 0x6A6A6A : over ? 0xFFFFFF : 0xC8C8C8);
        if (active) {
            this.area.hit(x, y, width, 10, action);
        }
    }
}
