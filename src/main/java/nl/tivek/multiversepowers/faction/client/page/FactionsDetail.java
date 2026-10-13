package nl.tivek.multiversepowers.faction.client.page;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientFactions;
import nl.tivek.multiversepowers.faction.client.pick.MobInfo;
import nl.tivek.multiversepowers.faction.client.pick.MobPickerScreen;
import nl.tivek.multiversepowers.faction.net.FactionEditPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// A faction opened: its tabs across the page, one of its creatures turning on the left, and the tab's rows beside
// it: what it is (Overview), which creatures are in it (Members), how it stands to the others (Relations) and what
// a player's reputation does (Reputation).
abstract class FactionsDetail extends FactionsState {
    static final int TAB_HEIGHT = 14;
    private static final String[] TABS = {"overview", "members", "relations", "reputation"};

    // Something outside the scrolling list that a click runs, kept per frame.
    private record Hit(int x, int y, int width, int height, Runnable action) {
    }

    final List<Hit> hits = new ArrayList<>();
    private int cardX;
    private int cardY;
    private int cardWidth;
    private int cardHeight;

    FactionsDetail(Component title, @Nullable Screen root, String page) {
        super(title, root, page);
    }

    void pickTab(int tab) {
        this.stopTyping();
        this.tab = tab;
        this.area.top();
    }

    // The opened faction's tabs and card between `top` and `bottom`; returns where its list starts.
    int profile(GuiGraphics graphics, Faction faction, int top, int bottom, int mouseX, int mouseY) {
        List<MobInfo.Info> members = this.members(faction.id());
        this.tabs(graphics, faction, members.size(), top, mouseX, mouseY);
        top += TAB_HEIGHT + 4;
        if (this.tab == RELATIONS || this.tab == REPUTATION) {
            // Rows of pills and bars take the page's whole width: no model beside them.
            this.cardWidth = 0;
            this.listX = this.contentX;
            this.listWidth = this.contentWidth;
            return top;
        }
        int width = Mth.clamp(this.contentWidth * 3 / 10, 84, 130);
        this.cardX = this.contentX;
        this.cardY = top;
        this.cardWidth = width;
        this.cardHeight = bottom - top;
        card(graphics, this.cardX, top, width, this.cardHeight, faction.color());
        MobInfo.Info kind = members.isEmpty() ? null : members.get(0);
        for (MobInfo.Info info : members) {
            kind = info.type() == this.shownKind ? info : kind;
        }
        List<String> players = this.players(faction);
        String player = this.shownPlayer != null && players.contains(this.shownPlayer) ? this.shownPlayer
                : kind == null && !players.isEmpty() ? players.get(0) : null;
        boolean over = this.overCard(mouseX, mouseY);
        boolean drawn = player != null ? this.preview.drawPlayer(graphics, player, this.cardX + 3, top + 14,
                width - 4, this.cardHeight - 26) : kind != null && this.preview.draw(graphics, kind.type(),
                this.cardX + 3, top + 14, width - 4, this.cardHeight - 26);
        if (!drawn) {
            int size = Math.min(48, width - 30);
            int x = this.cardX + (width - size) / 2 + 1;
            int y = top + (this.cardHeight - size) / 2;
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
            graphics.fill(x, y, x + size, y + size, 0xFF000000 | dim(faction.color()));
            graphics.fill(x, y, x + size, y + 3, 0xFF000000 | faction.color());
            String initial = faction.name().substring(0, 1).toUpperCase(Locale.ROOT);
            graphics.pose().pushPose();
            graphics.pose().translate(x + size / 2.0F, y + size / 2.0F + 1, 0);
            graphics.pose().scale(3.0F, 3.0F, 1.0F);
            graphics.drawString(this.font, initial, -this.font.width(initial) / 2, -4, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        } else if (over || this.preview.held()) {
            graphics.fill(this.cardX + 2, top, this.cardX + width, bottom, 0x0CFFFFFF);
        }
        Component head = player != null ? Component.literal(player) : kind != null ? kind.name()
                : Component.translatable(KEY + "card.none");
        graphics.drawString(this.font, this.fit(head, width - 10), this.cardX + 6, top + 4, MUTED, false);
        if (drawn) {
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "mobs.preview.drag"), width - 10),
                    this.cardX + 6, bottom - 11, over || this.preview.held() ? BODY : DIM, false);
        }
        ScreenAnchors.report("factions.card", this.cardX, top, width, this.cardHeight);
        this.listX = this.cardX + width + 6;
        this.listWidth = this.contentWidth - width - 6;
        return top;
    }

    boolean overCard(double mouseX, double mouseY) {
        return this.picked != null && mouseX >= this.cardX && mouseX < this.cardX + this.cardWidth
                && mouseY >= this.cardY && mouseY < this.cardY + this.cardHeight;
    }

    private int relations(Faction faction) {
        int set = 0;
        for (Faction other : FactionRules.all()) {
            set += FactionRules.relation(faction.id(), other.id()) != null ? 1 : 0;
        }
        return set;
    }

    private void tabs(GuiGraphics graphics, Faction faction, int members, int y, int mouseX, int mouseY) {
        int left = this.contentX;
        int width = this.contentWidth;
        graphics.fill(left, y + TAB_HEIGHT - 1, left + width, y + TAB_HEIGHT, 0x1CFFFFFF);
        int[] counts = {0, members + this.players(faction).size(), this.relations(faction), 0};
        int all = 0;
        for (int i = 0; i < TABS.length; i++) {
            all += this.font.width(Component.translatable(KEY + "tab." + TABS[i])) + 12
                    + (counts[i] > 0 ? this.font.width(String.valueOf(counts[i])) + 10 : 0);
        }
        boolean chips = all <= width;
        int x = left;
        for (int i = 0; i < TABS.length; i++) {
            Component label = Component.translatable(KEY + "tab." + TABS[i]);
            Component chip = chips && counts[i] > 0 ? Component.literal(String.valueOf(counts[i])) : null;
            int each = this.font.width(label) + (chips ? 12 : 8) + (chip == null ? 0 : this.font.width(chip) + 10);
            boolean lit = this.tab == i;
            boolean over = !lit && mouseX >= x && mouseX < x + each && mouseY >= y && mouseY < y + TAB_HEIGHT;
            if (lit) {
                graphics.fill(x, y, x + each, y + TAB_HEIGHT, 0x26FFFFFF);
                graphics.fill(x, y + TAB_HEIGHT - 1, x + each, y + TAB_HEIGHT, 0xFF000000 | faction.color());
            } else if (over) {
                graphics.fill(x, y, x + each, y + TAB_HEIGHT, 0x14FFFFFF);
            }
            graphics.drawString(this.font, label, x + (chips ? 6 : 4), y + 3, lit || over ? TEXT : BODY, false);
            if (chip != null) {
                this.chip(graphics, chip, x + 10 + this.font.width(label), y + 2, 0x2C2C2C, 0xC8C8C8);
            }
            int which = i;
            if (!lit) {
                this.hits.add(new Hit(x, y, each, TAB_HEIGHT, () -> this.pickTab(which)));
            }
            ScreenAnchors.report("factions.tab." + TABS[i], x, y, each, TAB_HEIGHT);
            x += each;
        }
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

    List<Line> detailLines(Faction faction) {
        return switch (this.tab) {
            case MEMBERS -> this.memberLines(faction);
            case RELATIONS -> this.relationLines(faction);
            case REPUTATION -> this.reputationLines(faction);
            default -> this.overviewLines(faction);
        };
    }

    private void word(Faction faction, String key, String value) {
        this.send(FactionEditPayload.WORD, faction.id(), key, value);
    }

    private Line attitude(Faction faction) {
        return this.row(Component.translatable(KEY + "attitude"), Component.translatable(KEY + "attitude.tip"),
                (graphics, right, y, mouseX, mouseY) -> this.options(graphics, this.stances(faction.attitude(),
                        null, standing -> this.word(faction, "attitude", FactionTable.name(standing))), right,
                        y + 3, mouseX, mouseY, this.editable()));
    }

    private List<Line> overviewLines(Faction faction) {
        List<Line> lines = new ArrayList<>();
        lines.add(this.textLine(faction, "name", faction.name(), FactionTable.NAME));
        lines.add(this.textLine(faction, "description", faction.description(), FactionTable.TEXT));
        lines.add(this.textLine(faction, "lore", faction.lore(), FactionTable.TEXT));
        lines.add(this.row(Component.translatable(KEY + "color"), null, (graphics, right, y, mouseX, mouseY) -> {
            int x = right - FactionTable.COLORS.length * 12 + 2;
            int left = x;
            for (int color : FactionTable.COLORS) {
                boolean lit = color == faction.color();
                boolean over = this.editable() && this.area.over(mouseX, mouseY, x, y + 4, 10, 10);
                graphics.fill(x - 1, y + 3, x + 11, y + 15, lit ? 0xFFFFFFFF : over ? 0xFF9A9A9A : 0xFF000000);
                graphics.fill(x, y + 4, x + 10, y + 14, 0xFF000000 | color);
                if (this.editable() && !lit) {
                    this.area.hit(x, y + 4, 10, 10, () -> this.word(faction, "color", String.format("%06x", color)));
                }
                x += 12;
            }
            return left;
        }));
        lines.add(this.attitude(faction));
        lines.add(this.row(Component.translatable(KEY + "you"), Component.translatable(KEY + "you.tip"),
                (graphics, right, y, mouseX, mouseY) -> {
                    Component stand = stand(ClientFactions.toward(faction));
                    right -= this.font.width(stand) + 6;
                    this.chip(graphics, stand, right, y + 4, 0x262626, stand.getStyle().getColor().getValue());
                    if (faction.reputation() && FactionRules.reputation()) {
                        String value = String.valueOf(this.reputation(faction));
                        right -= this.font.width(value) + 6;
                        graphics.drawString(this.font, value, right, y + 5, BODY, false);
                    }
                    if (ClientFactions.pinned(faction)) {
                        Component pinned = Component.translatable(KEY + "pinned");
                        right -= this.font.width(pinned) + 10;
                        this.chip(graphics, pinned, right, y + 4, 0x3A3010, WARN);
                    }
                    return right;
                }));
        lines.add(this.row(Component.translatable(KEY + "help"), Component.translatable(KEY + "help.tip"),
                (graphics, right, y, mouseX, mouseY) -> this.options(graphics, this.yesNo(faction.help(),
                        yes -> this.word(faction, "help", String.valueOf(yes))), right, y + 3, mouseX, mouseY,
                        this.editable())));
        lines.add(this.row(Component.translatable(KEY + "open"), Component.translatable(KEY + "open.tip"),
                (graphics, right, y, mouseX, mouseY) -> this.options(graphics, this.yesNo(faction.open(),
                        yes -> this.word(faction, "open", String.valueOf(yes))), right, y + 3, mouseX, mouseY,
                        this.editable())));
        List<String> players = this.players(faction);
        Component names = players.isEmpty() ? Component.translatable(KEY + "players.none")
                : Component.literal(String.join(", ", players));
        lines.add(this.row(Component.translatable(KEY + "players", players.size()), names,
                (graphics, right, y, mouseX, mouseY) -> {
                    int room = Math.max(40, this.listWidth / 2);
                    int width = Math.min(room, this.font.width(names));
                    graphics.drawString(this.font, this.fit(names, room), right - width, y + 5, MUTED, false);
                    return right - width;
                }));
        lines.add(this.row(Component.translatable(KEY + "id"), Component.translatable(KEY + "id.tip", faction.id()),
                (graphics, right, y, mouseX, mouseY) -> {
                    int width = this.font.width(faction.id());
                    graphics.drawString(this.font, faction.id(), right - width, y + 5, MUTED, false);
                    return right - width;
                }));
        return lines;
    }

    private void pickMembers(Faction faction) {
        Set<String> before = new LinkedHashSet<>();
        this.members(faction.id()).forEach(info -> before.add(info.id()));
        this.minecraft.setScreen(new MobPickerScreen(this, Component.translatable(KEY + "members.pick",
                faction.name()), before, faction.color(), after -> {
                    List<String> added = new ArrayList<>(after);
                    added.removeAll(before);
                    List<String> removed = new ArrayList<>(before);
                    removed.removeAll(after);
                    this.sendKinds(FactionEditPayload.ADD, faction.id(), added);
                    this.sendKinds(FactionEditPayload.REMOVE, "", removed);
                }));
    }

    private void sendKinds(int action, String faction, List<String> kinds) {
        for (int from = 0; from < kinds.size(); from += FactionEditPayload.MOST) {
            List<String> part = kinds.subList(from, Math.min(kinds.size(), from + FactionEditPayload.MOST));
            this.send(action, faction, "", part.toArray(String[]::new));
        }
    }

    private List<Line> memberLines(Faction faction) {
        List<Line> lines = new ArrayList<>();
        List<MobInfo.Info> members = this.members(faction.id());
        lines.add(this.row(Component.translatable(KEY + "members.count", members.size()),
                Component.translatable(KEY + "members.tip"), (graphics, right, y, mouseX, mouseY) -> {
                    if (!this.editable()) {
                        return right;
                    }
                    Component pick = Component.translatable(KEY + "members.edit");
                    right -= this.font.width(pick) + 6;
                    this.button(graphics, pick, right, y + 4, mouseX, mouseY, true, () -> this.pickMembers(faction));
                    ScreenAnchors.report("factions.members.edit", right, y + 4, this.font.width(pick) + 6, 10);
                    return right;
                }));
        for (MobInfo.Info info : members) {
            if (!info.matches(this.query)) {
                continue;
            }
            lines.add(new Line(true, (graphics, y, width, mouseX, mouseY) -> {
                boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
                boolean shown = info.type() == this.shownKind && this.shownPlayer == null;
                if (over || shown) {
                    graphics.fill(this.listX, y, this.listX + width, y + ROW, shown ? 0x22FFFFFF : 0x18FFFFFF);
                }
                graphics.renderItem(info.icon(), this.listX + 2, y + 1);
                int right = this.listX + width - 4;
                if (over && this.editable()) {
                    Component remove = Component.translatable(KEY + "members.remove");
                    right -= this.font.width(remove) + 6;
                    this.button(graphics, remove, right, y + 4, mouseX, mouseY, true,
                            () -> this.send(FactionEditPayload.REMOVE, "", "", info.id()));
                    right -= 4;
                }
                Component tag = FactionRules.listed(info.type()) ? info.modName()
                        : Component.translatable(KEY + "members.sort");
                int tagWidth = Math.min(this.font.width(tag), width / 3);
                graphics.drawString(this.font, this.fit(tag, tagWidth), right - tagWidth, y + 5, DIM, false);
                right -= tagWidth + 6;
                graphics.drawString(this.font, this.fit(info.name(), right - this.listX - 24), this.listX + 22,
                        y + 5, over || shown ? TEXT : BODY, false);
                this.area.hit(this.listX, y, width, ROW, () -> {
                    this.shownKind = info.type();
                    this.shownPlayer = null;
                });
                return over ? info.about() : null;
            }));
        }
        if (members.isEmpty()) {
            lines.add(this.row(Component.translatable(KEY + "members.none").withColor(DIM & 0xFFFFFF), null,
                    (graphics, right, y, mouseX, mouseY) -> right));
        }
        List<String> players = this.players(faction);
        lines.add(this.heading(Component.translatable(KEY + "members.players", players.size())));
        for (String player : players) {
            if (!this.query.isBlank() && !player.toLowerCase(Locale.ROOT).contains(this.query.strip()
                    .toLowerCase(Locale.ROOT))) {
                continue;
            }
            lines.add(new Line(true, (graphics, y, width, mouseX, mouseY) -> {
                boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
                boolean shown = player.equals(this.shownPlayer);
                if (over || shown) {
                    graphics.fill(this.listX, y, this.listX + width, y + ROW, shown ? 0x22FFFFFF : 0x18FFFFFF);
                }
                this.face(graphics, player, this.listX + 5, y + 4, 10);
                boolean on = this.online(player);
                Component state = Component.translatable(KEY + (on ? "members.online" : "members.away"));
                int stateWidth = this.font.width(state);
                graphics.drawString(this.font, state, this.listX + width - 4 - stateWidth, y + 5, on ? MUTED : DIM,
                        false);
                graphics.drawString(this.font, this.fit(Component.literal(player), width - stateWidth - 34),
                        this.listX + 22, y + 5, over || shown ? TEXT : on ? BODY : MUTED, false);
                this.area.hit(this.listX, y, width, ROW, () -> this.shownPlayer = player);
                return null;
            }));
        }
        if (players.isEmpty()) {
            lines.add(this.row(Component.translatable(KEY + "members.no_players").withColor(DIM & 0xFFFFFF), null,
                    (graphics, right, y, mouseX, mouseY) -> right));
        }
        return lines;
    }

    private void relation(Faction faction, Faction other, @Nullable Standing standing) {
        this.send(FactionEditPayload.RELATION, faction.id(), other.id(), FactionTable.name(standing));
        if (hasShiftDown() && faction != other) {
            this.send(FactionEditPayload.RELATION, other.id(), faction.id(), FactionTable.name(standing));
        }
    }

    private List<Line> relationLines(Faction faction) {
        List<Line> lines = new ArrayList<>();
        lines.add(this.heading(Component.translatable(KEY + "relations.own")));
        lines.add(this.row(Component.translatable(KEY + "relations.self"),
                Component.translatable(KEY + "relations.self.tip"), (graphics, right, y, mouseX, mouseY) ->
                        this.options(graphics, this.stances(FactionRules.relation(faction.id(), faction.id()),
                                FactionTable.NATURAL, standing -> this.relation(faction, faction, standing)), right,
                                y + 3, mouseX, mouseY, this.editable())));
        lines.add(this.attitude(faction));
        lines.add(this.heading(Component.translatable(KEY + "relations.others")));
        for (Faction other : FactionRules.all()) {
            if (other == faction || !this.query.isBlank() && !other.name().toLowerCase(Locale.ROOT)
                    .contains(this.query.strip().toLowerCase(Locale.ROOT))) {
                continue;
            }
            lines.add(new Line(true, (graphics, y, width, mouseX, mouseY) -> {
                int right = this.listX + width - 4;
                int left = this.options(graphics, this.stances(FactionRules.relation(faction.id(), other.id()),
                        FactionTable.NATURAL, standing -> this.relation(faction, other, standing)), right, y + 3,
                        mouseX, mouseY, this.editable());
                boolean onPills = this.area.over(mouseX, mouseY, left, y, right - left, ROW);
                Standing back = FactionRules.relation(other.id(), faction.id());
                if (back != null) {
                    Component they = Component.translatable(KEY + "relations.back", stand(back));
                    if (this.font.width(they) + 90 < left - this.listX) {
                        left -= this.font.width(they) + 6;
                        graphics.drawString(this.font, they, left, y + 5, DIM, false);
                    }
                }
                boolean onName = this.area.over(mouseX, mouseY, this.listX, y, left - this.listX - 2, ROW);
                if (onName) {
                    graphics.fill(this.listX, y, left - 2, y + ROW, 0x18FFFFFF);
                }
                graphics.fill(this.listX + 5, y + 6, this.listX + 11, y + 12, 0xFF000000 | other.color());
                graphics.drawString(this.font, this.fit(Component.literal(other.name()), left - this.listX - 22),
                        this.listX + 16, y + 5, onName ? TEXT : BODY, false);
                this.area.hit(this.listX, y, left - this.listX - 2, ROW, () -> this.open(other.id()));
                return onPills && this.editable() ? Component.translatable(KEY + "relations.tip", faction.name(),
                        other.name()) : onName ? Component.translatable(KEY + "relations.open", other.name()) : null;
            }));
        }
        return lines;
    }

    // The scale from -100 to 100 as a bar: red up to where the faction turns hostile, green from where it turns
    // friendly, and a white mark at `value`.
    void scale(GuiGraphics graphics, Faction faction, int value, int x, int y, int width) {
        int hostile = x + Math.round(width * (faction.hostileAt() - Faction.LOWEST) / 200.0F);
        int friendly = x + Math.round(width * (faction.friendlyAt() - Faction.LOWEST) / 200.0F);
        graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0xFF000000);
        graphics.fill(x, y, hostile, y + 4, 0xFF000000 | dim(Standing.HOSTILE.rgb()));
        graphics.fill(hostile, y, friendly, y + 4, 0xFF000000 | dim(Standing.NEUTRAL.rgb()));
        graphics.fill(friendly, y, x + width, y + 4, 0xFF000000 | dim(Standing.FRIENDLY.rgb()));
        int at = x + Math.round(width * (Mth.clamp(value, Faction.LOWEST, Faction.HIGHEST) - Faction.LOWEST)
                / 200.0F);
        graphics.fill(at - 1, y - 2, at + 1, y + 6, 0xFFFFFFFF);
    }

    private List<Line> reputationLines(Faction faction) {
        List<Line> lines = new ArrayList<>();
        boolean world = FactionRules.reputation();
        lines.add(this.row(Component.translatable(KEY + "rep.counts"),
                Component.translatable(KEY + (world ? "rep.counts.tip" : "rep.counts.world")),
                (graphics, right, y, mouseX, mouseY) -> this.options(graphics, this.yesNo(faction.reputation(),
                        yes -> this.word(faction, "reputation", String.valueOf(yes))), right, y + 3, mouseX, mouseY,
                        this.editable())));
        if (!world) {
            lines.add(this.row(Component.translatable(KEY + "rep.world_off").withColor(WARN), null,
                    (graphics, right, y, mouseX, mouseY) -> right));
        }
        int mine = this.reputation(faction);
        lines.add(new Line(true, (graphics, y, width, mouseX, mouseY) -> {
            int x = this.listX + Math.min(LABEL, width / 3);
            graphics.drawString(this.font, this.fit(Component.translatable(KEY + "rep.yours"), x - this.listX - 8),
                    this.listX + 5, y + 5, BODY, false);
            String value = String.valueOf(mine);
            int right = this.listX + width - 4 - this.font.width(value);
            graphics.drawString(this.font, value, right, y + 5, TEXT, false);
            this.scale(graphics, faction, mine, x, y + 7, right - x - 8);
            return this.area.over(mouseX, mouseY, this.listX, y, width, ROW) ? Component.translatable(
                    KEY + "rep.yours.tip", faction.hostileAt(), faction.friendlyAt()) : null;
        }));
        lines.add(this.row(Component.translatable(KEY + "rep.hostile_at"),
                Component.translatable(KEY + "rep.hostile_at.tip"), (graphics, right, y, mouseX, mouseY) ->
                        this.stepper(graphics, faction.id() + "|hostileAt", faction.hostileAt(), Faction.LOWEST, -1,
                                5, right, y, mouseX, mouseY,
                                value -> this.word(faction, "hostileAt", String.valueOf(value)))));
        lines.add(this.row(Component.translatable(KEY + "rep.friendly_at"),
                Component.translatable(KEY + "rep.friendly_at.tip"), (graphics, right, y, mouseX, mouseY) ->
                        this.stepper(graphics, faction.id() + "|friendlyAt", faction.friendlyAt(), 1,
                                Faction.HIGHEST, 5, right, y, mouseX, mouseY,
                                value -> this.word(faction, "friendlyAt", String.valueOf(value)))));
        lines.add(this.row(Component.translatable(KEY + "rep.start"), Component.translatable(KEY + "rep.start.tip"),
                (graphics, right, y, mouseX, mouseY) -> {
                    Component start = Component.literal(faction.start() + "  ").append(stand(faction.zone(0)));
                    right -= this.font.width(start);
                    graphics.drawString(this.font, start, right, y + 5, BODY, false);
                    return right;
                }));
        if (this.editable()) {
            lines.add(this.row(Component.translatable(KEY + "rep.test"), Component.translatable(KEY + "rep.test.tip"),
                    (graphics, right, y, mouseX, mouseY) -> {
                        Component back = Component.translatable(KEY + "rep.test.start");
                        right -= this.font.width(back) + 6;
                        this.button(graphics, back, right, y + 4, mouseX, mouseY, true,
                                () -> this.changeReputation(faction, "set", faction.start()));
                        for (int delta : new int[] {10, -10}) {
                            Component by = Component.literal(delta > 0 ? "+" + delta : String.valueOf(delta));
                            right -= this.font.width(by) + 10;
                            this.button(graphics, by, right, y + 4, mouseX, mouseY, true,
                                    () -> this.changeReputation(faction, "add", delta));
                        }
                        return right;
                    }));
        }
        lines.add(this.row(Component.translatable(KEY + "rep.rules").withColor(MUTED & 0xFFFFFF), null,
                (graphics, right, y, mouseX, mouseY) -> {
                    Component see = Component.translatable(KEY + "rep.rules.see");
                    right -= this.font.width(see) + 6;
                    this.button(graphics, see, right, y + 4, mouseX, mouseY, true, () -> this.open(null));
                    return right;
                }));
        return lines;
    }

    abstract void open(@Nullable String faction);
}
