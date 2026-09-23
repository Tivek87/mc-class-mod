package nl.tivek.welcomescreen.client.character;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.GuiShapes;
import nl.tivek.welcomescreen.client.spell.ClientSpellCooldowns;
import nl.tivek.welcomescreen.network.CastSpellPayload;
import nl.tivek.welcomescreen.network.TransformPayload;
import nl.tivek.welcomescreen.spell.MagicSchool;
import nl.tivek.welcomescreen.spell.Spell;
import org.lwjgl.glfw.GLFW;

/**
 * The power screen you get while you hold the wheel key: who you can turn into, and the spells you can cast.
 *
 * <p>The first page has two parts, with the same cards: the franchises the characters come from (Marvel, DC, Disney,
 * Warner Bros. and the rest, see {@link Roster}), and under them the schools of magic.
 *
 * <p>Nothing needs to be clicked. Keep the mouse still on a franchise or a school for a moment and it opens as a
 * page of its own, with its characters or its spells. Let the key go over a character to turn into them, or over a
 * spell to cast it; click a spell to read what it does. Characters that are not in the game yet say they are coming
 * soon, and the mouse passes over them. Resting on the back card, right-click or Escape goes back a page.
 */
public class PowerWheelScreen extends Screen {
    private static final String KEY = "screen." + WelcomeScreenMod.MODID + ".spell_wheel.";

    /** How long the mouse has to stay on a card before it opens by itself. */
    private static final long HOLD_MS = 381L;

    private static final int COLS = 5;
    private static final int CARD_H = 26;
    private static final int GAP_X = 6;
    private static final int GAP_Y = 8;
    private static final int CARD_W_MIN = 58;
    private static final int CARD_W_MAX = 96;
    // A page of characters or spells has wider cards, so their names fit.
    private static final int CARD_W_WIDE = 116;
    private static final float RADIUS = 3.0F;
    // Where things sit: the franchises and the schools on the first page, and the cards of an opened page.
    private static final int FRANCHISE_Y = 26;
    private static final int SCHOOL_Y = 86;
    private static final int PAGE_Y = 36;

    private static final int DIM = 0x88000000;
    private static final int CARD_FILL = 0xE0141418;
    private static final int CARD_FILL_HOVER = 0xF02A2A32;
    private static final int CARD_FILL_OFF = 0x9A0E0E10;
    private static final int LINE = 0x44FFFFFF;
    private static final int INSPECTED = 0x55FFFF;

    private static final int MUTED_COLOR = 0xFFA8A090;
    private static final int OFF_COLOR = 0xFF6A6A6A;
    private static final int COOLDOWN_COLOR = 0xFFE06050;
    private static final int ACTIVE_COLOR = 0xFF7FD46B;

    /** Which page is showing. */
    private enum Page {
        HOME,
        FRANCHISE,
        SCHOOL
    }

    /** What a card on the page stands for. */
    private enum Kind {
        FRANCHISE,
        SCHOOL,
        BACK,
        CHARACTER,
        SPELL
    }

    /**
     * One card as it is laid out right now.
     *
     * @param index which franchise, school, character or spell it is, in the list of its page
     */
    private record Card(Kind kind, int index, int x, int y, int w) {
        boolean same(@Nullable Card other) {
            return other != null && other.kind == this.kind && other.index == this.index;
        }
    }

    private final Roster.Franchise[] franchises = Roster.Franchise.values();
    private final MagicSchool[] schools;

    private Page page = Page.HOME;
    /** Which franchise or school is open, as an index in {@link #franchises} or {@link #schools}. */
    private int opened;
    private List<Card> cards = List.of();
    /** The card the mouse is on, or null for none (or one that cannot be picked). */
    @Nullable
    private Card hovered;
    private long hoverSince;
    // When the screen opens (the mouse jumps to the middle) or the page changes, the mouse may rest on a card it
    // never went to (the back card sits where the franchise or school was). Such a card only waits once the mouse
    // has moved; until then this is where the mouse was, in gui points. The first frame fills it in.
    private boolean firstFrame = true;
    private double stillX = Double.NaN;
    private double stillY;
    // Where the mouse was when the page was last drawn, in gui points.
    private double mouseX;
    private double mouseY;

    @Nullable
    private Spell inspectedSpell;
    private boolean clickedToInspect;

    private boolean keyWasReleased;

    public PowerWheelScreen() {
        super(Component.translatable(KEY + "title"));
        // Schools that already hold spells come first, so the top row is the one you really use and the ones still
        // being filled sit together in the row below.
        List<MagicSchool> order = new ArrayList<>();
        for (MagicSchool school : MagicSchool.values()) {
            if (!school.getSpells().isEmpty()) {
                order.add(school);
            }
        }
        for (MagicSchool school : MagicSchool.values()) {
            if (school.getSpells().isEmpty()) {
                order.add(school);
            }
        }
        this.schools = order.toArray(new MagicSchool[0]);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isWheelKeyDown() && !this.keyWasReleased) {
            this.handleRelease();
            this.keyWasReleased = true;
        }
    }

    private boolean isWheelKeyDown() {
        if (this.minecraft == null) {
            return false;
        }
        long window = this.minecraft.getWindow().getWindow();
        InputConstants.Key key = AbilityKeys.SPELL_WHEEL.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, key.getValue());
    }

    /** Letting the wheel key go does whatever the mouse is pointing at, and otherwise just closes. */
    private void handleRelease() {
        if (this.clickedToInspect) {
            // The spell was clicked to read about it: letting go must not cast it.
            this.onClose();
            return;
        }
        GameCharacter character = this.hoveredCharacter();
        Spell spell = this.hoveredSpell();
        if (character != null) {
            this.selectCharacter(character);
        } else if (spell != null) {
            this.castSpell(spell);
        } else {
            this.onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.page != Page.HOME) {
            this.goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.hovered != null) {
            GameCharacter character = this.hoveredCharacter();
            Spell spell = this.hoveredSpell();
            if (character != null) {
                this.selectCharacter(character);
                return true;
            }
            if (spell != null) {
                this.inspectedSpell = spell;
                this.clickedToInspect = true;
                this.click(1.2F);
                return true;
            }
            if (this.hovered.kind() == Kind.BACK) {
                this.goBack();
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (this.page != Page.HOME) {
                this.goBack();
            } else {
                this.onClose();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (AbilityKeys.SPELL_WHEEL.matchesMouse(button)) {
            this.handleRelease();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectCharacter(GameCharacter character) {
        if (this.minecraft != null && this.minecraft.player != null) {
            PacketDistributor.sendToServer(new TransformPayload(character.getId()));
        }
        this.onClose();
    }

    private void castSpell(Spell spell) {
        if (this.minecraft != null && this.minecraft.player != null) {
            int left = ClientSpellCooldowns.remaining(spell);
            if (left > 0) {
                this.minecraft.player.displayClientMessage(Component.translatable(KEY + "on_cooldown",
                        spell.getDisplayName(), seconds(left)), true);
            } else {
                PacketDistributor.sendToServer(new CastSpellPayload(spell.getId()));
            }
        }
        this.onClose();
    }

    private void open(Page page, int index) {
        this.page = page;
        this.opened = index;
        this.turned();
        this.click(1.3F);
    }

    private void goBack() {
        this.page = Page.HOME;
        this.turned();
        this.click(0.9F);
    }

    /** A new page: nothing on it is hovered or read yet, and it waits for the mouse to move (see stillX). */
    private void turned() {
        this.hovered = null;
        this.inspectedSpell = null;
        this.clickedToInspect = false;
        this.hoverSince = Util.getMillis();
        this.stillX = this.mouseX;
        this.stillY = this.mouseY;
    }

    private void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    // ---- What is on this page, and where it sits ----

    /** The characters of the open franchise; empty on any other page. */
    private List<Roster.Entry> roster() {
        return this.page == Page.FRANCHISE ? Roster.of(this.franchises[this.opened]) : List.of();
    }

    /** The spells of the open school; empty on any other page. */
    private List<Spell> spells() {
        return this.page == Page.SCHOOL ? this.schools[this.opened].getSpells() : List.of();
    }

    /** Every card on this page, where it sits right now. */
    private List<Card> layout() {
        List<Card> cards = new ArrayList<>();
        if (this.page == Page.HOME) {
            int cardW = this.cardWidth(COLS, CARD_W_MAX);
            this.grid(cards, slots(null, Kind.FRANCHISE, this.franchises.length), FRANCHISE_Y, cardW);
            this.grid(cards, slots(null, Kind.SCHOOL, this.schools.length), SCHOOL_Y, cardW);
        } else {
            // The back card comes first, the characters or spells after it on the same rows.
            List<Card> slots = this.page == Page.FRANCHISE
                    ? slots(Kind.BACK, Kind.CHARACTER, this.roster().size())
                    : slots(Kind.BACK, Kind.SPELL, this.spells().size());
            this.grid(cards, slots, PAGE_Y, this.cardWidth(slots.size(), CARD_W_WIDE));
        }
        return cards;
    }

    /** Cards that still have to be put in place: maybe one of {@code first}, then {@code count} of {@code kind}. */
    private static List<Card> slots(@Nullable Kind first, Kind kind, int count) {
        List<Card> slots = new ArrayList<>();
        if (first != null) {
            slots.add(new Card(first, 0, 0, 0, 0));
        }
        for (int i = 0; i < count; i++) {
            slots.add(new Card(kind, i, 0, 0, 0));
        }
        return slots;
    }

    private int cardWidth(int count, int widest) {
        int cols = Math.min(COLS, Math.max(1, count));
        int fits = (this.width - 40 - (cols - 1) * GAP_X) / cols;
        return Math.max(CARD_W_MIN, Math.min(widest, fits));
    }

    /**
     * Puts cards in rows of {@link #COLS} from {@code top} down, every row centred on its own, so a row that is not
     * full does not hang to the left.
     */
    private void grid(List<Card> cards, List<Card> slots, int top, int cardW) {
        for (int i = 0; i < slots.size(); i++) {
            int row = i / COLS;
            int inRow = Math.min(COLS, slots.size() - row * COLS);
            int rowWidth = inRow * cardW + (inRow - 1) * GAP_X;
            int x = (this.width - rowWidth) / 2 + (i % COLS) * (cardW + GAP_X);
            Card slot = slots.get(i);
            cards.add(new Card(slot.kind(), slot.index(), x, top + row * (CARD_H + GAP_Y), cardW));
        }
    }

    /** False for a character that is not in the game yet: the mouse passes over it. */
    private boolean pickable(Card card) {
        return card.kind() != Kind.CHARACTER || this.roster().get(card.index()).available();
    }

    @Nullable
    private GameCharacter hoveredCharacter() {
        return this.hovered != null && this.hovered.kind() == Kind.CHARACTER
                ? this.roster().get(this.hovered.index()).character() : null;
    }

    @Nullable
    private Spell hoveredSpell() {
        return this.hovered != null && this.hovered.kind() == Kind.SPELL ? this.spells().get(this.hovered.index())
                : null;
    }

    private void updateHover(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (this.firstFrame) {
            this.firstFrame = false;
            this.stillX = mouseX;
            this.stillY = mouseY;
        }
        if (!Double.isNaN(this.stillX) && Math.abs(mouseX - this.stillX) + Math.abs(mouseY - this.stillY) > 2.0) {
            // The mouse moved since the page changed: whatever it rests on from now on starts its wait afresh.
            this.stillX = Double.NaN;
            this.hoverSince = Util.getMillis();
        }
        Card was = this.hovered;
        this.hovered = null;
        for (Card card : this.cards) {
            if (mouseX >= card.x() && mouseX < card.x() + card.w() && mouseY >= card.y()
                    && mouseY < card.y() + CARD_H && this.pickable(card)) {
                this.hovered = card;
                break;
            }
        }
        if (this.hovered == null ? was != null : !this.hovered.same(was)) {
            this.hoverSince = Util.getMillis();
        }
    }

    /** How far the mouse is through the wait on the card it is resting on: 0 to 1. */
    private double held() {
        return Math.min(1.0, (Util.getMillis() - this.hoverSince) / (double) HOLD_MS);
    }

    /** True for a card that does something by itself once the mouse has rested on it long enough. */
    private static boolean opens(Card card) {
        return card.kind() == Kind.FRANCHISE || card.kind() == Kind.SCHOOL || card.kind() == Kind.BACK;
    }

    /** True while the card under the mouse may wait to open: not right after the page changed under it. */
    private boolean waiting() {
        return this.hovered != null && opens(this.hovered) && Double.isNaN(this.stillX);
    }

    /** Resting on a franchise or a school opens it; resting on the back card goes back. */
    private void followHover() {
        if (!this.waiting() || this.held() < 1.0) {
            return;
        }
        switch (this.hovered.kind()) {
            case FRANCHISE -> this.open(Page.FRANCHISE, this.hovered.index());
            case SCHOOL -> this.open(Page.SCHOOL, this.hovered.index());
            default -> this.goBack();
        }
    }

    // ---- Drawing ----

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, DIM);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.cards = this.layout();
        this.updateHover(mouseX, mouseY);
        this.followHover();
        // Following the mouse can open another page: lay that one out before it is drawn.
        this.cards = this.layout();
        if (this.hovered != null && this.cards.stream().noneMatch(card -> card.same(this.hovered))) {
            this.hovered = null;
        }

        switch (this.page) {
            case HOME -> {
                this.heading(guiGraphics, Component.translatable(KEY + "characters"), MUTED_COLOR, FRANCHISE_Y - 14);
                this.heading(guiGraphics, Component.translatable(KEY + "schools"), MUTED_COLOR, SCHOOL_Y - 14);
            }
            case FRANCHISE -> {
                Roster.Franchise franchise = this.franchises[this.opened];
                this.heading(guiGraphics, franchise.getDisplayName(), 0xFF000000 | franchise.getColor(), PAGE_Y - 16);
            }
            case SCHOOL -> {
                MagicSchool school = this.schools[this.opened];
                this.heading(guiGraphics, school.getDisplayName(), 0xFF000000 | school.getColor(), PAGE_Y - 16);
            }
        }
        // The shapes of every card first, then all the text on top of them.
        for (Card card : this.cards) {
            this.cardShape(guiGraphics, card);
        }
        GuiShapes.flush(guiGraphics);
        for (Card card : this.cards) {
            this.cardText(guiGraphics, card);
        }
        if (this.page == Page.SCHOOL && this.spells().isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable(KEY + "empty_school"),
                    this.width / 2, PAGE_Y + CARD_H + GAP_Y + 8, MUTED_COLOR);
        }
        if (this.page == Page.SCHOOL && this.inspectedSpell != null) {
            this.renderInspected(guiGraphics, this.inspectedSpell);
        }

        String hint = this.clickedToInspect ? "inspected_hint"
                : switch (this.page) {
                    case HOME -> "hint_home";
                    case FRANCHISE -> "hint_characters";
                    case SCHOOL -> "hint";
                };
        guiGraphics.drawCenteredString(this.font, Component.translatable(KEY + hint,
                AbilityKeys.SPELL_WHEEL.getTranslatedKeyMessage()), this.width / 2, this.height - 18, MUTED_COLOR);
    }

    /** A title in the middle, with a thin line out to either side of it. */
    private void heading(GuiGraphics guiGraphics, Component title, int color, int y) {
        int middle = this.width / 2;
        int half = Math.min(250, middle - 20);
        int textHalf = this.font.width(title) / 2 + 6;
        guiGraphics.fill(middle - half, y + 4, middle - textHalf, y + 5, LINE);
        guiGraphics.fill(middle + textHalf, y + 4, middle + half, y + 5, LINE);
        guiGraphics.drawCenteredString(this.font, title, middle, y, color);
    }

    /** The colour a card is drawn in: its franchise, school, character or spell. */
    private int color(Card card) {
        return switch (card.kind()) {
            case FRANCHISE -> this.franchises[card.index()].getColor();
            case SCHOOL -> this.schools[card.index()].getColor();
            case BACK -> MUTED_COLOR & 0xFFFFFF;
            case CHARACTER -> {
                Roster.Entry entry = this.roster().get(card.index());
                GameCharacter character = entry.character();
                yield character != null ? character.getColor() : entry.franchise().getColor();
            }
            case SPELL -> this.spells().get(card.index()).getColor();
        };
    }

    /**
     * The body of a card: a rim in its colour (white while the mouse is on it), a colour strip on its left, and while
     * the mouse rests on a card that opens by itself, a bar along its bottom that fills up until it does.
     */
    private void cardShape(GuiGraphics guiGraphics, Card card) {
        boolean pickable = this.pickable(card);
        boolean hovered = card.same(this.hovered);
        boolean inspected = card.kind() == Kind.SPELL && this.spells().get(card.index()) == this.inspectedSpell;
        int color = this.color(card);
        float x = card.x();
        float y = card.y();
        float w = card.w();
        int rim = inspected ? INSPECTED : hovered ? 0xFFFFFF : color;
        float rimAlpha = !pickable ? 0.22F : hovered || inspected ? 1.0F : 0.55F;
        GuiShapes.roundRect(guiGraphics, x, y, w, CARD_H, RADIUS, GuiShapes.fade(rim, rimAlpha));
        GuiShapes.roundRect(guiGraphics, x + 1.0F, y + 1.0F, w - 2.0F, CARD_H - 2.0F, RADIUS - 1.0F,
                !pickable ? CARD_FILL_OFF : hovered || inspected ? CARD_FILL_HOVER : CARD_FILL);
        GuiShapes.roundRect(guiGraphics, x + 3.0F, y + 4.0F, 2.0F, CARD_H - 8.0F, 1.0F,
                GuiShapes.fade(color, pickable ? 1.0F : 0.3F));
        if (hovered && this.waiting()) {
            float filled = (float) ((w - 8.0F) * this.held());
            if (filled > 0.5F) {
                GuiShapes.roundRect(guiGraphics, x + 4.0F, y + CARD_H - 4.0F, filled, 1.5F, 0.75F,
                        GuiShapes.fade(0xFFFFFF, 0.9F));
            }
        }
    }

    /** The name on a card, and the line under it. */
    private void cardText(GuiGraphics guiGraphics, Card card) {
        Component name;
        Component line;
        int nameColor = 0xFF000000 | this.color(card);
        int lineColor = MUTED_COLOR;
        switch (card.kind()) {
            case FRANCHISE -> {
                Roster.Franchise franchise = this.franchises[card.index()];
                int playable = Roster.available(franchise);
                name = franchise.getDisplayName();
                line = playable == 0 ? Component.translatable(KEY + "coming_soon")
                        : Component.translatable(KEY + "playable", playable, Roster.of(franchise).size());
                lineColor = playable == 0 ? MUTED_COLOR : ACTIVE_COLOR;
            }
            case SCHOOL -> {
                MagicSchool school = this.schools[card.index()];
                int count = school.getSpells().size();
                name = school.getDisplayName();
                line = switch (count) {
                    case 0 -> Component.translatable(KEY + "empty_school");
                    case 1 -> Component.translatable(KEY + "spells_one");
                    default -> Component.translatable(KEY + "spells_count", count);
                };
                lineColor = count == 0 ? MUTED_COLOR : ACTIVE_COLOR;
            }
            case BACK -> {
                name = Component.translatable(KEY + "back");
                line = Component.translatable(KEY + "back_hint");
                nameColor = MUTED_COLOR;
            }
            case CHARACTER -> {
                Roster.Entry entry = this.roster().get(card.index());
                boolean active = entry.character() != null && ClientCharacter.active() == entry.character();
                name = entry.getDisplayName();
                if (!entry.available()) {
                    line = Component.translatable(KEY + "coming_soon");
                    nameColor = OFF_COLOR;
                    lineColor = OFF_COLOR;
                } else {
                    line = Component.translatable(KEY + (active ? "active" : "become"));
                    lineColor = active ? ACTIVE_COLOR : MUTED_COLOR;
                }
            }
            case SPELL -> {
                Spell spell = this.spells().get(card.index());
                int left = ClientSpellCooldowns.remaining(spell);
                name = spell.getDisplayName();
                line = left > 0 ? Component.translatable(KEY + "cooldown", seconds(left))
                        : Component.translatable(KEY + "ready");
                lineColor = left > 0 ? COOLDOWN_COLOR : ACTIVE_COLOR;
            }
            default -> throw new IllegalStateException();
        }
        int textX = card.x() + 8;
        int room = card.w() - 12;
        guiGraphics.drawString(this.font, this.fit(name, room), textX, card.y() + 4, nameColor);
        guiGraphics.drawString(this.font, this.fit(line, room), textX, card.y() + 15, lineColor);
    }

    /** Text cut short with an ellipsis when it does not fit, so it never runs out of its card. */
    private Component fit(Component text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        String cut = this.font.plainSubstrByWidth(text.getString(), Math.max(0, width - this.font.width("…")));
        return Component.literal(cut + "…").withStyle(text.getStyle());
    }

    /** The spell you clicked: its name, its cooldown and what it does, in a box at the bottom. */
    private void renderInspected(GuiGraphics guiGraphics, Spell spell) {
        int boxW = Math.min(350, this.width - 40);
        int boxH = 48;
        int boxX = (this.width - boxW) / 2;
        int boxY = this.height - 72;
        int spellColor = spell.getColor();
        GuiShapes.roundRect(guiGraphics, boxX, boxY, boxW, boxH, 4.0F, GuiShapes.fade(spellColor, 0.9F));
        GuiShapes.roundRect(guiGraphics, boxX + 1.0F, boxY + 1.0F, boxW - 2.0F, boxH - 2.0F, 3.0F, 0xF0121218);
        GuiShapes.roundRect(guiGraphics, boxX + 4.0F, boxY + 5.0F, 2.0F, boxH - 10.0F, 1.0F,
                GuiShapes.fade(spellColor, 1.0F));
        GuiShapes.flush(guiGraphics);
        Component title = Component.literal(spell.getDisplayName().getString() + "  ")
                .append(Component.literal("(" + seconds(spell.getCooldown()) + "s)").withColor(MUTED_COLOR));
        guiGraphics.drawString(this.font, title, boxX + 11, boxY + 6, 0xFF000000 | spellColor);
        guiGraphics.drawWordWrap(this.font, spell.getDescription(), boxX + 11, boxY + 19, boxW - 20, 0xFFE0E0E0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
