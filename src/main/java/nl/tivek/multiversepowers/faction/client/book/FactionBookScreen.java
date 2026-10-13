package nl.tivek.multiversepowers.faction.client.book;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientFactions;
import nl.tivek.multiversepowers.faction.client.FactionTags;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import org.lwjgl.glfw.GLFW;

// The Faction Book: a contents spread of every faction of the world and how it stands to you, then a spread for each:
// its creatures (one shown turning), what it is and its lore, its allies and enemies, the named creatures near and
// its players. The arrows, the arrow keys or a line of the contents turn the pages.
public final class FactionBookScreen extends Screen {
    private static final String KEY = "faction." + MultiversePowers.MODID + ".book.";
    private static final String WORD = "faction." + MultiversePowers.MODID + ".";
    private static final int WIDTH = 360;
    private static final int HEIGHT = 206;
    private static final int MARGIN = 14;
    private static final int PAGE = WIDTH / 2 - MARGIN - 8;
    private static final int LINE = 10;
    private static final int ROW = 11;
    private static final int BOX = 56;
    private static final int NAMED = 12;
    private static final long CYCLE = 4000L;
    private static final int COVER = 0xFF4A2A1C;
    private static final int COVER_EDGE = 0xFF24130B;
    private static final int PAPER = 0xFFF1E6C9;
    private static final int PAPER_EDGE = 0xFFD9C79E;
    private static final int[] GUTTER = { 0xFFE6D8B5, 0xFFDCCBA3, 0xFFCFBC90, 0xFFC0AB7D };
    private static final int INK = 0xFF3A2A1A;
    private static final int MUTED = 0xFF7A6448;
    private static final int LORE = 0xFF5E4A32;
    private static final int HOVER = 0x1C3A2A1A;
    private static final int RULE = 0xFFB8A27A;

    private final MobPreview preview = new MobPreview();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, List<EntityType<?>>> members = new HashMap<>();
    private List<Faction> factions = List.of();
    private int membersStamp = -1;
    private int page;
    private int leftScroll;
    private int rightScroll;
    private int leftHeight;
    private int rightHeight;
    private int left;
    private int top;
    @Nullable
    private EntityType<?> shownKind;
    @Nullable
    private String shownPlayer;

    private record Hit(int x, int y, int width, int height, Runnable action) {
        boolean over(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }
    }

    public FactionBookScreen() {
        super(Component.translatable(KEY + "title"));
        rustle();
    }

    private static void rustle() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
    }

    @Override
    public void removed() {
        this.preview.forget();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.factions = List.copyOf(FactionRules.all());
        this.page = Mth.clamp(this.page, 0, this.factions.size());
        this.hits.clear();
        this.book(graphics);
        if (this.page == 0) {
            this.contents(graphics, mouseX, mouseY);
        } else {
            Faction faction = this.factions.get(this.page - 1);
            this.creatures(graphics, faction, mouseX, mouseY);
            this.story(graphics, faction, mouseX, mouseY);
        }
        this.footer(graphics, mouseX, mouseY);
    }

    private void book(GuiGraphics graphics) {
        int l = this.left;
        int t = this.top;
        int mid = l + WIDTH / 2;
        graphics.fill(l, t, l + WIDTH, t + HEIGHT, COVER_EDGE);
        graphics.fill(l + 2, t + 2, l + WIDTH - 2, t + HEIGHT - 2, COVER);
        graphics.fill(l + 6, t + 6, mid - 1, t + HEIGHT - 4, PAPER_EDGE);
        graphics.fill(mid + 1, t + 6, l + WIDTH - 6, t + HEIGHT - 4, PAPER_EDGE);
        graphics.fill(l + 6, t + 5, mid - 1, t + HEIGHT - 6, PAPER);
        graphics.fill(mid + 1, t + 5, l + WIDTH - 6, t + HEIGHT - 6, PAPER);
        for (int i = 0; i < GUTTER.length; i++) {
            graphics.fill(mid - 2 - i, t + 5, mid - 1 - i, t + HEIGHT - 6, GUTTER[GUTTER.length - 1 - i]);
            graphics.fill(mid + 1 + i, t + 5, mid + 2 + i, t + HEIGHT - 6, GUTTER[GUTTER.length - 1 - i]);
        }
        graphics.fill(mid - 1, t + 3, mid + 1, t + HEIGHT - 3, COVER_EDGE);
    }

    private int leftX() {
        return this.left + MARGIN;
    }

    private int rightX() {
        return this.left + WIDTH / 2 + MARGIN - 4;
    }

    private int pageTop() {
        return this.top + 13;
    }

    private int pageBottom() {
        return this.top + HEIGHT - 22;
    }

    private void contents(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftX();
        int y = this.pageTop();
        Component title = this.title.copy().withStyle(ChatFormatting.BOLD);
        graphics.drawString(this.font, title, x + (PAGE - this.font.width(title)) / 2, y, INK, false);
        y += LINE + 4;
        y = this.text(graphics, Component.translatable(KEY + "intro"), x, y, PAGE, MUTED) + 4;
        Faction mine = ClientFactions.mine();
        y = this.text(graphics, mine == null ? Component.translatable(KEY + "yours.none")
                : Component.translatable(KEY + "yours", this.name(mine)), x, y, PAGE, INK) + 4;
        graphics.fill(x, y, x + PAGE, y + 1, RULE);
        y += 5;
        int first = Math.max(0, (this.pageBottom() - y) / ROW);
        for (int i = 0; i < Math.min(first, this.factions.size()); i++) {
            this.entry(graphics, i, x, y + i * ROW, mouseX, mouseY, this.pageTop(), this.pageBottom());
        }
        int rx = this.rightX();
        int rest = this.factions.size() - first;
        this.rightHeight = Math.max(0, rest) * ROW;
        this.rightScroll = this.scroll(this.rightScroll, this.rightHeight);
        graphics.enableScissor(rx, this.pageTop(), rx + PAGE, this.pageBottom());
        for (int i = first; i < this.factions.size(); i++) {
            this.entry(graphics, i, rx, this.pageTop() + (i - first) * ROW - this.rightScroll, mouseX, mouseY,
                    this.pageTop(), this.pageBottom());
        }
        graphics.disableScissor();
    }

    private void entry(GuiGraphics graphics, int index, int x, int y, int mouseX, int mouseY, int clipTop,
            int clipBottom) {
        Faction faction = this.factions.get(index);
        boolean over = this.hit(x, y, PAGE, ROW, clipTop, clipBottom, () -> this.turnTo(index + 1), mouseX, mouseY);
        if (over) {
            graphics.fill(x - 2, y - 1, x + PAGE + 2, y + ROW - 1, HOVER);
        }
        graphics.fill(x, y + 2, x + 6, y + 8, 0xFF000000 | faction.color());
        graphics.fill(x, y + 7, x + 6, y + 8, 0x40000000);
        Component stand = this.stand(ClientFactions.toward(faction));
        int standWidth = this.font.width(stand);
        graphics.drawString(this.font, this.fit(faction.name(), PAGE - standWidth - 14), x + 9, y + 1,
                ink(faction.color()), false);
        graphics.drawString(this.font, stand, x + PAGE - standWidth, y + 1, MUTED, false);
    }

    private void creatures(GuiGraphics graphics, Faction faction, int mouseX, int mouseY) {
        int x = this.leftX();
        int y = this.pageTop();
        y = this.text(graphics, this.name(faction).withStyle(ChatFormatting.BOLD), x, y, PAGE, INK);
        Standing toward = ClientFactions.toward(faction);
        MutableComponent standing = Component.translatable(KEY + "toward", this.stand(toward));
        if (FactionRules.reputation() && faction.reputation()) {
            standing.append(Component.translatable(KEY + "reputation", ClientFactions.reputation(faction)));
        }
        y = this.text(graphics, standing, x, y + 1, PAGE, MUTED);
        if (faction.equals(ClientFactions.mine())) {
            y = this.text(graphics, Component.translatable(KEY + "mine"), x, y, PAGE, ink(Standing.FRIENDLY.rgb()));
        }
        y += 3;
        List<EntityType<?>> kinds = this.members(faction);
        List<String> players = players(faction);
        EntityType<?> kind = null;
        String player = this.shownPlayer != null && players.contains(this.shownPlayer) ? this.shownPlayer : null;
        if (player == null) {
            kind = this.shownKind != null ? this.shownKind : kinds.isEmpty() ? null
                    : kinds.get((int) (Util.getMillis() / CYCLE % kinds.size()));
            player = kind == null && !players.isEmpty() ? players.get(0) : null;
        }
        graphics.fill(x, y, x + PAGE, y + BOX, PAPER_EDGE);
        graphics.fill(x + 1, y + 1, x + PAGE - 1, y + BOX - 1, 0xFFE9DCB9);
        boolean drawn = player != null ? this.preview.drawPlayer(graphics, player, x + 2, y + 2, PAGE - 4, BOX - 4)
                : kind != null && this.preview.draw(graphics, kind, x + 2, y + 2, PAGE - 4, BOX - 4);
        if (!drawn) {
            Component none = Component.translatable(KEY + "unseen");
            graphics.drawString(this.font, none, x + (PAGE - this.font.width(none)) / 2, y + BOX / 2 - 4, MUTED,
                    false);
        }
        y += BOX + 2;
        Component caption = player != null ? Component.literal(player) : kind != null ? kind.getDescription()
                : Component.empty();
        graphics.drawString(this.font, this.fit(caption.getString(), PAGE), x + (PAGE - Math.min(PAGE,
                this.font.width(caption))) / 2, y, INK, false);
        y += LINE + 2;
        y = this.heading(graphics, Component.translatable(KEY + "creatures", kinds.size()), x, y);
        int listTop = y;
        int bottom = this.pageBottom();
        this.leftHeight = Math.max(1, kinds.size()) * LINE;
        this.leftScroll = this.scroll(this.leftScroll, this.leftHeight, bottom - listTop);
        graphics.enableScissor(x - 2, listTop, x + PAGE + 2, bottom);
        y = listTop - this.leftScroll;
        if (kinds.isEmpty()) {
            graphics.drawString(this.font, Component.translatable(KEY + "creatures.none"), x, y, MUTED, false);
        }
        for (EntityType<?> each : kinds) {
            boolean shown = each == kind;
            boolean over = this.hit(x, y, PAGE, LINE, listTop, bottom, () -> {
                this.shownKind = each;
                this.shownPlayer = null;
            }, mouseX, mouseY);
            if (over || shown) {
                graphics.fill(x - 2, y - 1, x + PAGE + 2, y + LINE - 1, HOVER);
            }
            graphics.drawString(this.font, this.fit(each.getDescription().getString(), PAGE - 6), x + 4, y, shown
                    ? INK : MUTED, false);
            y += LINE;
        }
        graphics.disableScissor();
        if (this.leftScroll < this.leftHeight - (bottom - listTop)) {
            more(graphics, x + PAGE - 3, bottom + 1);
        }
    }

    private void story(GuiGraphics graphics, Faction faction, int mouseX, int mouseY) {
        int x = this.rightX();
        int clipTop = this.pageTop();
        int bottom = this.pageBottom();
        this.rightScroll = this.scroll(this.rightScroll, this.rightHeight);
        graphics.enableScissor(x - 2, clipTop, x + PAGE + 2, bottom);
        int y = clipTop - this.rightScroll;
        int start = y;
        if (!faction.description().isBlank()) {
            y = this.text(graphics, Component.literal(faction.description()), x, y, PAGE, INK) + 5;
        }
        if (!faction.lore().isBlank()) {
            y = this.text(graphics, Component.literal("“" + faction.lore() + "”")
                    .withStyle(ChatFormatting.ITALIC), x, y, PAGE, LORE) + 5;
        }
        y = this.ties(graphics, faction, Standing.FRIENDLY, "allies", x, y);
        y = this.ties(graphics, faction, Standing.HOSTILE, "enemies", x, y);
        List<LivingEntity> named = named(faction);
        y = this.heading(graphics, Component.translatable(KEY + "named"), x, y);
        if (named.isEmpty()) {
            y = this.text(graphics, Component.translatable(KEY + "named.none"), x, y, PAGE, MUTED);
        }
        for (LivingEntity living : named) {
            EntityType<?> type = living.getType();
            boolean over = this.hit(x, y, PAGE, LINE, clipTop, bottom, () -> {
                this.shownKind = type;
                this.shownPlayer = null;
            }, mouseX, mouseY);
            if (over) {
                graphics.fill(x - 2, y - 1, x + PAGE + 2, y + LINE - 1, HOVER);
            }
            String name = living.getCustomName() == null ? "" : living.getCustomName().getString();
            Component kind = Component.literal(" " + type.getDescription().getString()).withColor(MUTED & 0xFFFFFF);
            int kindWidth = Math.min(this.font.width(kind), PAGE / 2);
            graphics.drawString(this.font, this.fit(name, PAGE - kindWidth - 2), x, y, INK, false);
            graphics.drawString(this.font, this.fit(kind.getString(), kindWidth), x + PAGE - kindWidth, y, MUTED,
                    false);
            y += LINE;
        }
        y += 4;
        List<String> players = players(faction);
        y = this.heading(graphics, Component.translatable(KEY + "players", players.size()), x, y);
        if (players.isEmpty()) {
            y = this.text(graphics, Component.translatable(KEY + "players.none"), x, y, PAGE, MUTED);
        }
        Minecraft minecraft = Minecraft.getInstance();
        String self = minecraft.player == null ? "" : minecraft.player.getScoreboardName();
        for (String player : players) {
            boolean shown = player.equals(this.shownPlayer);
            boolean over = this.hit(x, y, PAGE, LINE, clipTop, bottom, () -> this.shownPlayer = player, mouseX,
                    mouseY);
            if (over || shown) {
                graphics.fill(x - 2, y - 1, x + PAGE + 2, y + LINE - 1, HOVER);
            }
            ClientFactions.face(graphics, player, x, y, 8);
            MutableComponent line = Component.literal(player);
            if (player.equals(self)) {
                line.append(Component.translatable(KEY + "you"));
            }
            graphics.drawString(this.font, this.fit(line.getString(), PAGE - 12), x + 11, y,
                    ClientFactions.online(player) ? INK : MUTED, false);
            y += LINE;
        }
        graphics.disableScissor();
        this.rightHeight = y - start;
        if (this.rightScroll < this.rightHeight - (bottom - clipTop)) {
            more(graphics, x + PAGE - 3, bottom + 1);
        }
    }

    // A small arrow under a page whose text goes on below.
    private static void more(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            graphics.fill(x - 2 + row, y + row, x + 3 - row, y + row + 1, MUTED);
        }
    }

    // The factions `faction` holds as `standing`, by name in their colours.
    private int ties(GuiGraphics graphics, Faction faction, Standing standing, String key, int x, int y) {
        MutableComponent names = Component.empty();
        int count = 0;
        for (Faction other : this.factions) {
            if (!other.id().equals(faction.id()) && FactionRules.relation(faction.id(), other.id()) == standing) {
                if (count++ > 0) {
                    names.append(Component.literal(", ").withColor(MUTED & 0xFFFFFF));
                }
                names.append(this.name(other));
            }
        }
        if (count == 0) {
            return y;
        }
        y = this.heading(graphics, Component.translatable(KEY + key), x, y);
        return this.text(graphics, names, x, y, PAGE, INK) + 4;
    }

    private void footer(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = this.top + HEIGHT - 17;
        int total = this.factions.size() + 1;
        Component number = Component.translatable(KEY + "page", this.page + 1, total);
        int rx = this.rightX();
        graphics.drawString(this.font, number, rx + (PAGE - this.font.width(number)) / 2, y, MUTED, false);
        if (this.page > 0) {
            Component back = Component.translatable(KEY + "contents");
            int bx = this.leftX() + (PAGE - this.font.width(back)) / 2;
            boolean over = this.hit(bx - 2, y - 2, this.font.width(back) + 4, LINE + 2, 0, this.height,
                    () -> this.turnTo(0), mouseX, mouseY);
            graphics.drawString(this.font, back, bx, y, over ? INK : MUTED, false);
            this.arrow(graphics, "«", this.leftX() - 4, y, -1, mouseX, mouseY);
        }
        if (this.page < this.factions.size()) {
            this.arrow(graphics, "»", rx + PAGE - 2, y, 1, mouseX, mouseY);
        }
    }

    private void arrow(GuiGraphics graphics, String mark, int x, int y, int step, int mouseX, int mouseY) {
        boolean over = this.hit(x - 4, y - 3, 14, LINE + 4, 0, this.height, () -> this.turnTo(this.page + step),
                mouseX, mouseY);
        graphics.drawString(this.font, mark, x, y, over ? INK : MUTED, false);
    }

    private int heading(GuiGraphics graphics, Component title, int x, int y) {
        graphics.drawString(this.font, title.copy().withStyle(ChatFormatting.BOLD), x, y, INK, false);
        graphics.fill(x, y + LINE - 1, x + PAGE, y + LINE, RULE);
        return y + LINE + 3;
    }

    private int text(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : this.font.split(text, width)) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += LINE;
        }
        return y;
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
    }

    // A click area, kept only where it shows inside its page; whether the mouse is on it.
    private boolean hit(int x, int y, int width, int height, int clipTop, int clipBottom, Runnable action,
            int mouseX, int mouseY) {
        int from = Math.max(y, clipTop);
        int to = Math.min(y + height, clipBottom);
        if (to <= from) {
            return false;
        }
        Hit hit = new Hit(x, from, width, to - from, action);
        this.hits.add(hit);
        return hit.over(mouseX, mouseY);
    }

    private int scroll(int scroll, int content) {
        return this.scroll(scroll, content, this.pageBottom() - this.pageTop());
    }

    private int scroll(int scroll, int content, int view) {
        return Mth.clamp(scroll, 0, Math.max(0, content - view));
    }

    private void turnTo(int page) {
        int to = Mth.clamp(page, 0, this.factions.size());
        if (to != this.page) {
            this.page = to;
            this.leftScroll = 0;
            this.rightScroll = 0;
            this.shownKind = null;
            this.shownPlayer = null;
            rustle();
        }
    }

    private MutableComponent name(Faction faction) {
        return Component.literal(faction.name()).withColor(ink(faction.color()) & 0xFFFFFF);
    }

    private MutableComponent stand(Standing standing) {
        return Component.translatable(WORD + "stand." + standing.name().toLowerCase(Locale.ROOT))
                .withColor(ink(standing.rgb()) & 0xFFFFFF);
    }

    // A colour dark enough to read on the paper.
    private static int ink(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int g = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private List<EntityType<?>> members(Faction faction) {
        if (FactionRules.stamp() != this.membersStamp) {
            this.membersStamp = FactionRules.stamp();
            this.members.clear();
        }
        return this.members.computeIfAbsent(faction.id(), id -> {
            List<EntityType<?>> kinds = new ArrayList<>(FactionRules.members(id));
            kinds.sort(Comparator.comparing(type -> type.getDescription().getString()));
            return kinds;
        });
    }

    private static List<String> players(Faction faction) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerTeam team = minecraft.level == null ? null : minecraft.level.getScoreboard().getPlayerTeam(faction.id());
        List<String> names = team == null ? new ArrayList<>() : new ArrayList<>(team.getPlayers());
        names.sort(null);
        return names;
    }

    // The creatures with a name of their own near the player that are in `faction`, nearest first.
    private static List<LivingEntity> named(Faction faction) {
        Minecraft minecraft = Minecraft.getInstance();
        List<LivingEntity> found = new ArrayList<>();
        if (minecraft.level == null || minecraft.player == null) {
            return found;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && !(living instanceof Player) && living.hasCustomName()
                    && living.isAlive()) {
                Faction of = FactionTags.of(living);
                if (of != null && of.id().equals(faction.id())) {
                    found.add(living);
                }
            }
        }
        Player player = minecraft.player;
        found.sort(Comparator.comparingDouble(player::distanceToSqr));
        return found.size() > NAMED ? found.subList(0, NAMED) : found;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (Hit hit : List.copyOf(this.hits)) {
                if (hit.over(mouseX, mouseY)) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.round(-scrollY * LINE * 2);
        if (mouseX < this.left + WIDTH / 2.0) {
            this.leftScroll = Math.max(0, this.leftScroll + step);
        } else {
            this.rightScroll = Math.max(0, this.rightScroll + step);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.turnTo(this.page - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.turnTo(this.page + 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
