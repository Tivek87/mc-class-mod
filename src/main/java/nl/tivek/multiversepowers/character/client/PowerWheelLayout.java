package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.spell.MagicSchool;
import nl.tivek.multiversepowers.spell.Spell;

abstract class PowerWheelLayout extends Screen {
    static final String KEY = "screen." + MultiversePowers.MODID + ".spell_wheel.";

    private static final long HOLD_MS = 381L;

    private static final int COLS = 5;
    static final int CARD_H = 26;
    private static final int GAP_X = 6;
    static final int GAP_Y = 8;
    private static final int CARD_W_MIN = 58;
    private static final int CARD_W_MAX = 96;
    private static final int CARD_W_WIDE = 116;
    static final float RADIUS = 3.0F;
    static final int FRANCHISE_Y = 26;
    static final int SCHOOL_Y = 86;
    static final int PAGE_Y = 36;

    static final int DIM = 0x88000000;
    static final int CARD_FILL = 0xE0141418;
    static final int CARD_FILL_HOVER = 0xF02A2A32;
    static final int CARD_FILL_OFF = 0x9A0E0E10;
    static final int LINE = 0x44FFFFFF;
    static final int INSPECTED = 0x55FFFF;

    static final int MUTED_COLOR = 0xFFA8A090;
    static final int OFF_COLOR = 0xFF6A6A6A;
    static final int COOLDOWN_COLOR = 0xFFE06050;
    static final int ACTIVE_COLOR = 0xFF7FD46B;

    enum Page {
        HOME,
        FRANCHISE,
        SCHOOL
    }

    enum Kind {
        FRANCHISE,
        SCHOOL,
        BACK,
        CHARACTER,
        SPELL
    }

    record Card(Kind kind, int index, int x, int y, int w) {
        boolean same(@Nullable Card other) {
            return other != null && other.kind == this.kind && other.index == this.index;
        }
    }

    final Roster.Franchise[] franchises = Roster.Franchise.values();
    final MagicSchool[] schools;

    Page page = Page.HOME;
    int opened;
    List<Card> cards = List.of();
    @Nullable
    Card hovered;
    long hoverSince;
    private boolean firstFrame = true;
    double stillX = Double.NaN;
    double stillY;
    double mouseX;
    double mouseY;

    @Nullable
    Spell inspectedSpell;
    boolean clickedToInspect;

    boolean keyWasReleased;

    PowerWheelLayout() {
        super(Component.translatable(KEY + "title"));
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

    List<Roster.Entry> roster() {
        return this.page == Page.FRANCHISE ? Roster.of(this.franchises[this.opened]) : List.of();
    }

    List<Spell> spells() {
        return this.page == Page.SCHOOL ? this.schools[this.opened].getSpells() : List.of();
    }

    List<Card> layout() {
        List<Card> cards = new ArrayList<>();
        if (this.page == Page.HOME) {
            int cardW = this.cardWidth(COLS, CARD_W_MAX);
            this.grid(cards, slots(null, Kind.FRANCHISE, this.franchises.length), FRANCHISE_Y, cardW);
            this.grid(cards, slots(null, Kind.SCHOOL, this.schools.length), SCHOOL_Y, cardW);
        } else {
            List<Card> slots = this.page == Page.FRANCHISE
                    ? slots(Kind.BACK, Kind.CHARACTER, this.roster().size())
                    : slots(Kind.BACK, Kind.SPELL, this.spells().size());
            this.grid(cards, slots, PAGE_Y, this.cardWidth(slots.size(), CARD_W_WIDE));
        }
        return cards;
    }

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

    boolean pickable(Card card) {
        return card.kind() != Kind.CHARACTER || this.roster().get(card.index()).available();
    }

    @Nullable
    GameCharacter hoveredCharacter() {
        return this.hovered != null && this.hovered.kind() == Kind.CHARACTER
                ? this.roster().get(this.hovered.index()).character() : null;
    }

    @Nullable
    Spell hoveredSpell() {
        return this.hovered != null && this.hovered.kind() == Kind.SPELL ? this.spells().get(this.hovered.index())
                : null;
    }

    void updateHover(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (this.firstFrame) {
            this.firstFrame = false;
            this.stillX = mouseX;
            this.stillY = mouseY;
        }
        if (!Double.isNaN(this.stillX) && Math.abs(mouseX - this.stillX) + Math.abs(mouseY - this.stillY) > 2.0) {
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

    double held() {
        return Math.min(1.0, (Util.getMillis() - this.hoverSince) / (double) HOLD_MS);
    }

    private static boolean opens(Card card) {
        return card.kind() == Kind.FRANCHISE || card.kind() == Kind.SCHOOL || card.kind() == Kind.BACK;
    }

    boolean waiting() {
        return this.hovered != null && opens(this.hovered) && Double.isNaN(this.stillX);
    }
}
