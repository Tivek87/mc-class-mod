package nl.tivek.multiversepowers.faction.client.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.faction.MobFactions;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientFactions;
import nl.tivek.multiversepowers.faction.client.pick.MobInfo;
import nl.tivek.multiversepowers.faction.net.FactionEditPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionRules.Rule;
import nl.tivek.multiversepowers.faction.rules.FactionTable;
import org.lwjgl.glfw.GLFW;

// The settings window's Factions page, open to every player of a world: its factions in a list to search, each
// with how it stands to you and your reputation, and under them the world's rules of reputation. A click opens a
// faction (`FactionsDetail`). Only the host, an owner or (where the world lets them) an operator changes anything:
// a new faction, a step back, a reset, and the faction or behaviour of the one creature they stood before. Anyone
// joins a faction that is open, or leaves theirs.
public final class FactionsScreen extends FactionsDetail {
    private static final int SEARCH = 110;
    private static final int TIP_WIDTH = 220;
    private static final long ARMED_MS = 3000L;
    @Nullable
    private EditBox search;
    private long deleteArmed;
    private long resetArmed;

    public FactionsScreen(@Nullable Screen root) {
        super(Component.translatable(PREFIX + "factions").withColor(COLOR), root, SettingsPages.FACTIONS);
        if (Minecraft.getInstance().crosshairPickEntity instanceof Mob mob) {
            this.looked = mob;
        }
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
        return Component.translatable(KEY + (this.editable() ? "about" : "about.locked"));
    }

    @Override
    protected void initPage() {
        this.stopTyping();
        int top = this.windowY + TITLE_BAR + 8;
        int room = this.contentWidth - this.font.width(this.title.copy().withStyle(ChatFormatting.BOLD)) - 12;
        int width = Math.max(60, Math.min(SEARCH, room));
        this.search = new EditBox(this.font, this.contentX + this.contentWidth - width, top - 3, width, 14,
                Component.translatable(PREFIX + "search"));
        this.search.setHint(Component.translatable(KEY + "search").withStyle(ChatFormatting.DARK_GRAY));
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

    @Override
    void open(@Nullable String faction) {
        this.stopTyping();
        this.picked = faction;
        this.tab = OVERVIEW;
        this.shownKind = null;
        this.deleteArmed = 0L;
        this.area.top();
        this.query = "";
        if (this.search != null) {
            this.search.setValue("");
        }
    }

    private boolean matches(Faction faction) {
        String wanted = this.query.strip().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty() || faction.name().toLowerCase(Locale.ROOT).contains(wanted)
                || faction.id().toLowerCase(Locale.ROOT).contains(wanted)) {
            return true;
        }
        for (MobInfo.Info info : this.members(faction.id())) {
            if (info.matches(wanted)) {
                return true;
            }
        }
        return false;
    }

    private List<Line> listLines() {
        List<Line> lines = new ArrayList<>();
        if (this.typing(NEW)) {
            lines.add(new Line(false, (graphics, y, width, mouseX, mouseY) -> {
                Component label = Component.translatable(KEY + "new.name");
                graphics.fill(this.listX, y + 1, this.listX + width, y + ROW - 1, 0x2AE8B04A);
                graphics.drawString(this.font, label, this.listX + 5, y + 5, TEXT, false);
                int x = this.listX + this.font.width(label) + 12;
                graphics.fill(x - 3, y + 2, this.listX + width - 3, y + 16, 0xFF000000);
                this.typingAt(x - 2, y + 3, this.listX + width - 4 - x);
                return null;
            }));
        }
        if (this.looked != null && this.looked.isAlive() && this.editable()) {
            this.lookedLines(lines, this.looked);
        }
        lines.add(this.heading(Component.translatable(KEY + "list", FactionRules.all().size())));
        Faction mine = ClientFactions.mine();
        for (Faction faction : FactionRules.all()) {
            if (this.matches(faction)) {
                lines.add(this.factionLine(faction, faction == mine));
            }
        }
        lines.add(this.heading(Component.translatable(KEY + "rules")));
        for (Rule rule : Rule.values()) {
            Component name = Component.translatable(WORD + "rule." + rule.key);
            Component tip = Component.translatable(WORD + "rule." + rule.key + ".desc");
            lines.add(this.row(name, tip, (graphics, right, y, mouseX, mouseY) -> rule.toggle()
                    ? this.options(graphics, this.yesNo(rule.get() > 0, yes -> this.send(FactionEditPayload.RULE,
                            "", rule.key, yes ? "1" : "0")), right, y + 3, mouseX, mouseY, this.editable())
                    : this.stepper(graphics, "rule|" + rule.key, rule.get(), 0, rule.most, 1, right, y, mouseX,
                            mouseY, value -> this.send(FactionEditPayload.RULE, "", rule.key, String.valueOf(value)))));
        }
        return lines;
    }

    // The creature the player stood before: the faction it is in, with a way to another, and how it acts to players.
    private void lookedLines(List<Line> lines, Mob mob) {
        String entity = String.valueOf(mob.getId());
        MobInfo.Info info = MobInfo.of(mob.getType());
        lines.add(new Line(false, (graphics, y, width, mouseX, mouseY) -> {
            graphics.fill(this.listX, y + 1, this.listX + width, y + ROW, 0x2A7FD8FF);
            graphics.renderItem(info.icon(), this.listX + 2, y + 1);
            int right = this.listX + width - 4;
            Component change = Component.translatable(KEY + (this.choosing ? "looked.cancel" : "looked.change"));
            right -= this.font.width(change) + 6;
            this.button(graphics, change, right, y + 4, mouseX, mouseY, true, () -> this.choosing = !this.choosing);
            Faction faction = MobFactions.of(mob);
            MobFactions.Own own = MobFactions.own(mob);
            Component in = faction == null ? Component.translatable(KEY + "looked.none") : name(faction);
            right -= this.font.width(in) + 12;
            this.chip(graphics, in, right, y + 4, 0x262626, faction == null ? 0x9A9A9A : faction.color());
            if (own != null && !own.faction().isEmpty()) {
                Component its = Component.translatable(KEY + "looked.own");
                right -= this.font.width(its) + 10;
                this.chip(graphics, its, right, y + 4, 0x3A3010, WARN);
            }
            graphics.drawString(this.font, this.fit(Component.translatable(KEY + "looked", mob.getDisplayName()),
                    right - this.listX - 28), this.listX + 22, y + 5, TEXT, false);
            return this.area.over(mouseX, mouseY, this.listX, y, right - this.listX, ROW)
                    ? Component.translatable(KEY + "looked.tip") : null;
        }));
        lines.add(this.row(Component.translatable(KEY + "looked.stance"),
                Component.translatable(KEY + "looked.stance.tip"), (graphics, right, y, mouseX, mouseY) -> {
                    graphics.fill(this.listX, y, this.listX + this.area.rowWidth(), y + ROW, 0x147FD8FF);
                    return this.options(graphics, this.stances(MobFactions.stance(mob), "faction",
                            standing -> this.send(FactionEditPayload.MOB_STANCE, "", entity,
                                    FactionTable.name(standing))), right, y + 3, mouseX, mouseY, true);
                }));
        if (this.choosing) {
            lines.add(this.row(Component.translatable(KEY + "looked.choose").withColor(WARN), null,
                    (graphics, right, y, mouseX, mouseY) -> {
                        Component kind = Component.translatable(KEY + "looked.kind");
                        right -= this.font.width(kind) + 6;
                        this.button(graphics, kind, right, y + 4, mouseX, mouseY, true,
                                () -> this.put(entity, ""));
                        Component none = Component.translatable(KEY + "looked.none");
                        right -= this.font.width(none) + 10;
                        this.button(graphics, none, right, y + 4, mouseX, mouseY, true,
                                () -> this.put(entity, MobFactions.NONE));
                        return right;
                    }));
        }
    }

    private void put(String entity, String faction) {
        this.send(FactionEditPayload.MOB_FACTION, faction, entity);
        this.choosing = false;
    }

    // A faction in the list: its colour and name, how many kinds and players are in it, and how it stands to you.
    private Line factionLine(Faction faction, boolean mine) {
        return new Line(true, (graphics, y, width, mouseX, mouseY) -> {
            boolean over = this.area.over(mouseX, mouseY, this.listX, y, width, ROW);
            if (over) {
                graphics.fill(this.listX, y, this.listX + width, y + ROW, this.choosing ? 0x2A7FD8FF : 0x18FFFFFF);
            }
            graphics.fill(this.listX + 4, y + 5, this.listX + 12, y + 13, 0xFF000000);
            graphics.fill(this.listX + 5, y + 6, this.listX + 11, y + 12, 0xFF000000 | faction.color());
            int right = this.listX + width - 4;
            graphics.drawString(this.font, "›", right - 4, y + 5, over ? TEXT : MUTED, false);
            right -= 10;
            Standing toward = ClientFactions.toward(faction);
            Component stand = stand(toward);
            int chip = this.font.width(stand) + 6;
            right -= chip;
            this.chip(graphics, stand, right, y + 4, 0x262626, toward == null ? 0xC8C8C8 : toward.rgb());
            if (faction.reputation() && FactionRules.reputation()) {
                int value = this.reputation(faction);
                right -= 46;
                this.scale(graphics, faction, value, right, y + 7, 40);
                String shown = String.valueOf(value);
                right -= this.font.width(shown) + 5;
                graphics.drawString(this.font, shown, right, y + 5, BODY, false);
            }
            Component name = name(faction).withStyle(style -> style.withBold(mine));
            int nameWidth = Math.min(this.font.width(name), right - this.listX - 24);
            graphics.drawString(this.font, this.fit(name, nameWidth), this.listX + 17, y + 5, TEXT, false);
            int x = this.listX + 17 + nameWidth + 6;
            if (mine) {
                Component yours = Component.translatable(KEY + "yours");
                if (x + this.font.width(yours) + 8 < right) {
                    x += this.chip(graphics, yours, x, y + 4, 0x1C3A24, ACCENT) + 6;
                }
            }
            Component count = Component.translatable(KEY + "count", this.members(faction.id()).size(),
                    this.players(faction).size());
            if (x + 30 < right) {
                graphics.drawString(this.font, this.fit(count, right - x - 4), x, y + 5, DIM, false);
            }
            if (this.choosing && this.looked != null) {
                String entity = String.valueOf(this.looked.getId());
                this.area.hit(this.listX, y, width, ROW, () -> this.put(entity, faction.id()));
                return over ? Component.translatable(KEY + "looked.put", this.looked.getDisplayName(),
                        faction.name()) : null;
            }
            this.area.hit(this.listX, y, width, ROW, () -> this.open(faction.id()));
            return over && !faction.description().isEmpty() ? Component.literal(faction.description()) : null;
        });
    }

    // Over the list: a new faction, a step back and a reset of them all; or the opened faction with a way back, a
    // way in or out of it for the player, and its end.
    private void header(GuiGraphics graphics, @Nullable Faction faction, int mouseX, int mouseY) {
        int y = this.contentY + 3;
        boolean editable = this.editable();
        long now = Util.getMillis();
        int right = this.contentX + this.contentWidth;
        if (faction == null) {
            if (editable) {
                boolean armed = now - this.resetArmed < ARMED_MS;
                Component reset = Component.translatable(KEY + (armed ? "reset.sure" : "reset"));
                right -= this.font.width(reset) + 6;
                this.smallButton(graphics, armed ? reset.copy().withColor(ERROR) : reset, right, y - 1, mouseX,
                        mouseY, true, () -> {
                            if (armed) {
                                this.send(FactionEditPayload.RESET, "", "");
                                this.resetArmed = 0L;
                            } else {
                                this.resetArmed = now;
                            }
                        });
                if (ClientFactions.steps() > 0) {
                    Component undo = Component.translatable(PREFIX + "mobs.undo", ClientFactions.steps());
                    right -= this.font.width(undo) + 10;
                    this.smallButton(graphics, undo, right, y - 1, mouseX, mouseY, true,
                            () -> this.send(FactionEditPayload.UNDO, "", ""));
                }
                Component add = Component.translatable(KEY + "new");
                right -= this.font.width(add) + 10;
                this.smallButton(graphics, add, right, y - 1, mouseX, mouseY, true, () -> {
                    this.area.top();
                    this.startTyping(NEW, "", "", FactionTable.NAME);
                });
                ScreenAnchors.report("factions.new", right, y - 1, this.font.width(add) + 6, 10);
            }
            graphics.drawString(this.font, this.fit(Component.translatable(KEY + "pick"),
                    right - this.contentX - 8), this.contentX, y, MUTED, false);
            return;
        }
        int x = this.contentX;
        x += this.link(graphics, Component.translatable(KEY + "back"), x, y, mouseX, mouseY, MUTED,
                () -> this.open(null)) + 8;
        if (editable) {
            boolean armed = now - this.deleteArmed < ARMED_MS;
            Component delete = Component.translatable(KEY + (armed ? "delete.sure" : "delete"));
            right -= this.font.width(delete) + 6;
            this.smallButton(graphics, armed ? delete.copy().withColor(ERROR) : delete, right, y - 1, mouseX, mouseY,
                    true, () -> {
                        if (armed) {
                            this.send(FactionEditPayload.DELETE, faction.id(), "");
                            this.open(null);
                        } else {
                            this.deleteArmed = now;
                        }
                    });
            right -= 4;
        }
        boolean mine = ClientFactions.mine() == faction;
        boolean welcome = editable || faction.open() && ClientFactions.toward(faction) != Standing.HOSTILE;
        Component join = Component.translatable(KEY + (mine ? "leave" : welcome ? "join" : faction.open()
                ? "join.unwelcome" : "join.invite"));
        right -= this.font.width(join) + 6;
        this.smallButton(graphics, join, right, y - 1, mouseX, mouseY, mine || welcome,
                () -> this.send(mine ? FactionEditPayload.LEAVE : FactionEditPayload.JOIN, faction.id(), ""));
        ScreenAnchors.report("factions.join", right, y - 1, this.font.width(join) + 6, 10);
        graphics.drawString(this.font, this.fit(name(faction).withStyle(ChatFormatting.BOLD), right - x - 8), x, y,
                TEXT, false);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hits.clear();
        this.beginTyping();
        // A page of the settings window, as the tour knows it.
        ScreenAnchors.report("settings", this.contentX, this.contentY, this.contentWidth, this.contentHeight);
        if (this.wanted != null && FactionRules.get(this.wanted) != null) {
            this.open(this.wanted);
            this.wanted = null;
        }
        Faction faction = this.faction();
        if (this.picked != null && faction == null) {
            this.open(null);
        }
        if (this.search != null) {
            this.search.visible = faction == null || this.tab == MEMBERS || this.tab == RELATIONS;
        }
        this.header(graphics, faction, mouseX, mouseY);
        int top = this.contentY + HEADER + 4;
        int bottom = this.contentY + this.contentHeight;
        this.listX = this.contentX;
        this.listWidth = this.contentWidth;
        if (faction != null) {
            top = this.profile(graphics, faction, top, bottom, mouseX, mouseY);
        }
        int height = bottom - top;
        List<Line> lines = faction == null ? this.listLines() : this.detailLines(faction);
        ScreenAnchors.report("factions.list", this.listX, top, this.listWidth, height);
        Component tip = null;
        int start = this.area.begin(graphics, this.listX, top, this.listWidth, height, lines.size() * ROW);
        int width = this.area.rowWidth();
        for (int i = 0; i < lines.size(); i++) {
            int y = start + i * ROW;
            if (!this.area.shows(y, ROW)) {
                continue;
            }
            Line line = lines.get(i);
            if (line.striped() && i % 2 == 0) {
                graphics.fill(this.listX, y, this.listX + width, y + ROW, 0x10FFFFFF);
            }
            Component about = line.drawer().draw(graphics, y, width, mouseX, mouseY);
            tip = about != null ? about : tip;
        }
        this.area.end(graphics);
        this.placeTyping();
        if (tip != null) {
            // Split into its own lines: a line break inside one tooltip line shows as a glyph.
            graphics.renderTooltip(this.font, this.font.split(tip, TIP_WIDTH), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A click away from an open box takes what was typed.
        if (this.typing != null && !(this.typing.visible && this.typing.isMouseOver(mouseX, mouseY))) {
            this.applyTyping();
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.area.press(mouseX, mouseY)) {
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

    // Esc first leaves a box, then a creature being placed, then goes back from a faction to the list.
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.typing != null && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            this.applyTyping();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.typing != null) {
            this.stopTyping();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.choosing) {
            this.choosing = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.picked != null) {
            this.open(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
