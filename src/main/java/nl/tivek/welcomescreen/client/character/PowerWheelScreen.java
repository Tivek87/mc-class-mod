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
import nl.tivek.welcomescreen.client.spell.ClientSpellCooldowns;
import nl.tivek.welcomescreen.network.CastSpellPayload;
import nl.tivek.welcomescreen.network.TransformPayload;
import nl.tivek.welcomescreen.spell.MagicSchool;
import nl.tivek.welcomescreen.spell.Spell;
import org.lwjgl.glfw.GLFW;

/**
 * The power screen you get while you hold the wheel key.
 *
 * <p>
 * The screen is split in two clearly separated halves:
 * </p>
 * <ul>
 * <li>the characters you can turn into, side by side in their own bar at the
 * top;</li>
 * <li>the schools of magic well below them, as a table five cards across.</li>
 * </ul>
 *
 * <p>
 * Nothing needs to be clicked. Keep the mouse still on a school for a moment
 * and that school opens
 * as a page of its own, with its spells as the same kind of cards. Let the key
 * go over a spell to cast
 * it, or over a character to turn into them. Right-click or Escape goes back a
 * page.
 * </p>
 */
public class PowerWheelScreen extends Screen {
    private static final String KEY = "screen." + WelcomeScreenMod.MODID + ".spell_wheel.";

    /** How long the mouse has to stay on a card before it opens by itself. */
    private static final long HOLD_MS = 305L;

    // The bar of characters at the top.
    private static final int CHAR_CARD_W = 120;
    private static final int CHAR_CARD_H = 22;
    private static final int CHAR_CARD_GAP = 12;
    private static final int CHAR_Y = 22;

    // The table of cards underneath, well clear of the characters.
    private static final int COLS = 5;
    private static final int GRID_TOP = 80;
    private static final int GRID_GAP_X = 6;
    private static final int GRID_GAP_Y = 8;
    private static final int CARD_H = 26;
    private static final int CARD_W_MIN = 58;
    private static final int CARD_W_MAX = 84;
    // A page of spells has fewer, wider cards, so their names fit.
    private static final int CARD_W_WIDE = 116;

    private static final int DIM = 0x55000000;
    private static final int CARD_FILL = 0xC8000000;
    private static final int CARD_FILL_HOVER = 0xEA2A2A2A;
    private static final int LINE = 0x44FFFFFF;

    private static final int MUTED_COLOR = 0xFFA8A090;
    private static final int COOLDOWN_COLOR = 0xFFE06050;
    private static final int ACTIVE_COLOR = 0xFF7FD46B;

    private final GameCharacter[] characters;
    private final MagicSchool[] schools;

    /**
     * Which school's page is open, as an index in {@link #schools}, or -1 for the
     * page of schools.
     */
    private int open = -1;
    /** Which card on this page the mouse is on, or -1 for none. */
    private int hovered = -1;
    private long hoverSince;

    @Nullable
    private GameCharacter hoveredCharacter;
    @Nullable
    private Spell hoveredSpell;
    @Nullable
    private Spell inspectedSpell;
    private boolean clickedToInspect;

    private boolean keyWasReleased;

    public PowerWheelScreen() {
        super(Component.translatable(KEY + "title"));
        this.characters = GameCharacter.values();
        // Schools that already hold spells come first, so the top row is the one you
        // really use and
        // the ones still being filled sit together in the row below.
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

    /**
     * Letting the wheel key go does whatever the mouse is pointing at, and
     * otherwise just closes.
     */
    private void handleRelease() {
        if (this.clickedToInspect) {
            // Player clicked to read the explanation; do not cast on release!
            this.onClose();
            return;
        }
        if (this.hoveredCharacter != null) {
            this.selectCharacter(this.hoveredCharacter);
        } else if (this.hoveredSpell != null) {
            this.castSpell(this.hoveredSpell);
        } else {
            this.onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.open >= 0) {
            this.goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (this.hoveredCharacter != null) {
                this.selectCharacter(this.hoveredCharacter);
                return true;
            }
            if (this.hoveredSpell != null) {
                this.inspectedSpell = this.hoveredSpell;
                this.clickedToInspect = true;
                this.click(1.2F);
                return true;
            }
            if (this.open >= 0 && this.hovered == 0) {
                this.goBack();
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (this.open >= 0) {
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

    private void openSchool(int index) {
        this.open = index;
        this.hovered = -1;
        this.inspectedSpell = null;
        this.clickedToInspect = false;
        this.hoverSince = Util.getMillis();
        this.click(1.3F);
    }

    private void goBack() {
        this.open = -1;
        this.hovered = -1;
        this.inspectedSpell = null;
        this.clickedToInspect = false;
        this.hoverSince = Util.getMillis();
        this.click(0.9F);
    }

    private void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    // ---- What is on this page, and where it sits
    // ------------------------------------------

    /**
     * The spells of the school whose page is open; empty on the page of schools.
     */
    private List<Spell> spells() {
        return this.open < 0 ? List.of() : this.schools[this.open].getSpells();
    }

    /**
     * How many cards this page has: the schools, or a back card plus that school's
     * spells.
     */
    private int cardCount() {
        return this.open < 0 ? this.schools.length : this.spells().size() + 1;
    }

    private int cardWidth() {
        int cols = Math.min(COLS, Math.max(1, this.cardCount()));
        int fits = (this.width - 40 - (cols - 1) * GRID_GAP_X) / cols;
        return Math.max(CARD_W_MIN, Math.min(this.open < 0 ? CARD_W_MAX : CARD_W_WIDE, fits));
    }

    /**
     * Every row is centred on its own, so a row that is not full does not hang to
     * the left.
     */
    private int cardX(int index, int cardW) {
        int row = index / COLS;
        int inRow = Math.min(COLS, this.cardCount() - row * COLS);
        int width = inRow * cardW + (inRow - 1) * GRID_GAP_X;
        return (this.width - width) / 2 + (index % COLS) * (cardW + GRID_GAP_X);
    }

    private int cardY(int index) {
        return GRID_TOP + (index / COLS) * (CARD_H + GRID_GAP_Y);
    }

    private int charX(int index) {
        int total = this.characters.length * CHAR_CARD_W + (this.characters.length - 1) * CHAR_CARD_GAP;
        return (this.width - total) / 2 + index * (CHAR_CARD_W + CHAR_CARD_GAP);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void updateHover(double mouseX, double mouseY) {
        this.hoveredCharacter = null;
        this.hoveredSpell = null;
        int was = this.hovered;
        this.hovered = -1;

        for (int i = 0; i < this.characters.length; i++) {
            if (inside(mouseX, mouseY, this.charX(i), CHAR_Y, CHAR_CARD_W, CHAR_CARD_H)) {
                this.hoveredCharacter = this.characters[i];
                // Characters have their own numbers, well away from the cards below.
                this.hovered = -2 - i;
                break;
            }
        }
        if (this.hoveredCharacter == null) {
            int cardW = this.cardWidth();
            for (int i = 0; i < this.cardCount(); i++) {
                if (inside(mouseX, mouseY, this.cardX(i, cardW), this.cardY(i), cardW, CARD_H)) {
                    this.hovered = i;
                    break;
                }
            }
        }
        if (this.hovered != was) {
            this.hoverSince = Util.getMillis();
        }
        if (this.open >= 0 && this.hovered > 0) {
            this.hoveredSpell = this.spells().get(this.hovered - 1);
        }
    }

    /**
     * How far the mouse is through the wait on the card it is resting on: 0 to 1.
     */
    private double held() {
        return Math.min(1.0, (Util.getMillis() - this.hoverSince) / (double) HOLD_MS);
    }

    /**
     * Resting on a school opens it; resting on the back card goes back. Nothing is
     * ever clicked.
     */
    private void followHover() {
        if (this.hovered < 0 || this.held() < 1.0) {
            return;
        }
        if (this.open < 0) {
            this.openSchool(this.hovered);
        } else if (this.hovered == 0) {
            this.goBack();
        }
    }

    // ---- Drawing
    // ---------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, DIM);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.updateHover(mouseX, mouseY);
        this.followHover();

        this.renderCharacterBar(guiGraphics);
        if (this.open < 0) {
            this.renderSchools(guiGraphics);
        } else {
            this.renderSpells(guiGraphics);
        }

        Component hint = Component.translatable(
                KEY + (this.clickedToInspect ? "inspected_hint" : (this.open < 0 ? "hint_schools" : "hint")),
                AbilityKeys.SPELL_WHEEL.getTranslatedKeyMessage());
        guiGraphics.drawCenteredString(this.font, hint, this.width / 2, this.height - 18, MUTED_COLOR);
    }

    private void renderCharacterBar(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(this.font, Component.translatable(KEY + "characters"), this.width / 2,
                CHAR_Y - 12, MUTED_COLOR);

        for (int i = 0; i < this.characters.length; i++) {
            GameCharacter character = this.characters[i];
            int x = this.charX(i);
            boolean hovered = character == this.hoveredCharacter;
            boolean active = ClientCharacter.active() == character;
            int color = 0xFF000000 | character.getColor();

            guiGraphics.fill(x, CHAR_Y, x + CHAR_CARD_W, CHAR_Y + CHAR_CARD_H, hovered ? CARD_FILL_HOVER : CARD_FILL);
            guiGraphics.renderOutline(x, CHAR_Y, CHAR_CARD_W, CHAR_CARD_H,
                    hovered ? 0xFFFFFFFF : (active ? ACTIVE_COLOR : color));
            guiGraphics.fill(x + 1, CHAR_Y + 1, x + 4, CHAR_Y + CHAR_CARD_H - 1, color);

            int textX = x + 8;
            guiGraphics.drawString(this.font, character.getDisplayName(), textX, CHAR_Y + 2, color);
            Component status = Component.translatable(KEY + (active ? "active" : "become"));
            guiGraphics.drawString(this.font, status, textX, CHAR_Y + 12, active ? ACTIVE_COLOR : MUTED_COLOR);
        }
    }

    /** The line and the heading between the characters and the cards under them. */
    private void header(GuiGraphics guiGraphics, Component title, int color, int cardW) {
        int left = this.cardX(0, cardW);
        int right = left + Math.min(COLS, this.cardCount()) * cardW
                + (Math.min(COLS, this.cardCount()) - 1) * GRID_GAP_X;
        int lineY = (CHAR_Y + CHAR_CARD_H + GRID_TOP) / 2 - 8;
        guiGraphics.fill(Math.min(left, this.width / 2 - 100), lineY, Math.max(right, this.width / 2 + 100),
                lineY + 1, LINE);
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, lineY + 7, color);
    }

    private void renderSchools(GuiGraphics guiGraphics) {
        int cardW = this.cardWidth();
        this.header(guiGraphics, Component.translatable(KEY + "schools"), MUTED_COLOR, cardW);

        for (int i = 0; i < this.schools.length; i++) {
            MagicSchool school = this.schools[i];
            int color = 0xFF000000 | school.getColor();
            int count = school.getSpells().size();
            Component line = switch (count) {
                case 0 -> Component.translatable(KEY + "empty_school");
                case 1 -> Component.translatable(KEY + "spells_one");
                default -> Component.translatable(KEY + "spells_count", count);
            };
            this.card(guiGraphics, i, cardW, school.getDisplayName(), color, line,
                    count == 0 ? MUTED_COLOR : ACTIVE_COLOR);
        }
    }

    private void renderSpells(GuiGraphics guiGraphics) {
        MagicSchool school = this.schools[this.open];
        int cardW = this.cardWidth();
        this.header(guiGraphics, school.getDisplayName(), 0xFF000000 | school.getColor(), cardW);

        this.card(guiGraphics, 0, cardW, Component.translatable(KEY + "back"), MUTED_COLOR,
                Component.translatable(KEY + "back_hint"), MUTED_COLOR);

        List<Spell> spells = this.spells();
        for (int i = 0; i < spells.size(); i++) {
            Spell spell = spells.get(i);
            int left = ClientSpellCooldowns.remaining(spell);
            Component status = left > 0
                    ? Component.translatable(KEY + "cooldown", seconds(left))
                    : Component.translatable(KEY + "ready");
            this.card(guiGraphics, i + 1, cardW, spell.getDisplayName(), 0xFF000000 | spell.getColor(),
                    status, left > 0 ? COOLDOWN_COLOR : ACTIVE_COLOR);
        }
        if (spells.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable(KEY + "empty_school"),
                    this.width / 2, GRID_TOP + CARD_H + GRID_GAP_Y + 8, MUTED_COLOR);
        }

        // Selected spell explanation box (max 2 sentences, clean & readable)
        if (this.inspectedSpell != null) {
            int boxW = Math.min(350, this.width - 40);
            int boxH = 48;
            int boxX = (this.width - boxW) / 2;
            int boxY = this.height - 72;

            int spellColor = 0xFF000000 | this.inspectedSpell.getColor();
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xEE121218);
            guiGraphics.renderOutline(boxX, boxY, boxW, boxH, spellColor);
            guiGraphics.fill(boxX + 1, boxY + 1, boxX + 4, boxY + boxH - 1, spellColor);

            Component title = Component.literal(this.inspectedSpell.getDisplayName().getString() + "  ")
                    .append(Component.literal("(" + seconds(this.inspectedSpell.getCooldown()) + "s)")
                            .withColor(MUTED_COLOR));
            guiGraphics.drawString(this.font, title, boxX + 10, boxY + 5, spellColor);
            guiGraphics.drawWordWrap(this.font, this.inspectedSpell.getDescription(), boxX + 10, boxY + 18, boxW - 18,
                    0xFFE0E0E0);
        }
    }

    /**
     * One card: a name, a colour strip, a line under it, and the wait bar while you
     * rest on it.
     */
    private void card(GuiGraphics guiGraphics, int index, int cardW, Component name, int color,
            Component line, int lineColor) {
        int x = this.cardX(index, cardW);
        int y = this.cardY(index);
        boolean hovered = index == this.hovered;
        boolean selected = this.open >= 0 && index > 0 && (index - 1) < this.spells().size()
                && this.spells().get(index - 1) == this.inspectedSpell;

        guiGraphics.fill(x, y, x + cardW, y + CARD_H, hovered || selected ? CARD_FILL_HOVER : CARD_FILL);
        guiGraphics.renderOutline(x, y, cardW, CARD_H, selected ? 0xFF55FFFF : (hovered ? 0xFFFFFFFF : color));
        guiGraphics.fill(x + 1, y + 1, x + 3, y + CARD_H - 1, color);

        int textX = x + 6;
        guiGraphics.drawString(this.font, name, textX, y + 4, color);
        guiGraphics.drawString(this.font, line, textX, y + 15, lineColor);

        // The bar that fills while the mouse rests here: it says the page is about to
        // open by itself.
        boolean waits = hovered && (this.open < 0 || index == 0);
        if (waits) {
            int width = (int) ((cardW - 2) * this.held());
            guiGraphics.fill(x + 1, y + CARD_H - 2, x + 1 + width, y + CARD_H - 1, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
