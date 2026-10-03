package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

// Every ability of the character you play: its name, its key or button, and what it does in plain words.
final class AbilityGuideScreen extends Screen {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    private static final int MAX_WIDTH = 380;
    private static final int PAD = 10;
    private static final int HEADER = 40;
    private static final int FOOTER = 18;
    private static final int PANEL = 0xE0101418;
    private static final int LINE = 0x40FFFFFF;
    private static final int NAME = 0xFFFFFFFF;
    private static final int KEY = 0xFFE8D9A8;
    private static final int TEXT = 0xFFB4BCC6;
    private static final int MUTED = 0xFF7C8590;
    private static final int SCROLL = 0x80FFFFFF;

    private final GameCharacter character;
    private final List<Entry> entries = new ArrayList<>();
    private int contentHeight;
    private double scroll;

    private record Entry(Component name, Component keys, List<FormattedCharSequence> lines, int height,
            boolean heading) {
    }

    AbilityGuideScreen(GameCharacter character) {
        super(Component.translatable(PREFIX + "title", character.getDisplayName()));
        this.character = character;
    }

    @Override
    protected void init() {
        this.entries.clear();
        int inner = this.panelWidth() - PAD * 2 - 6;
        Font font = this.font;
        String passive = "character." + MultiversePowers.MODID + "." + this.character.getId() + ".passive";
        if (Language.getInstance().has(passive)) {
            this.add(font, Component.translatable(PREFIX + "always"), Component.empty(),
                    Component.translatable(passive), inner);
        }
        boolean split = false;
        for (CharacterAbility ability : this.character.abilities()) {
            split |= ability.when() == CharacterAbility.When.FLYING;
        }
        if (split) {
            this.heading(font, Component.translatable(PREFIX + "ground"));
        }
        for (CharacterAbility ability : this.character.abilities()) {
            if (!ability.isPlaceholder() && ability.when() != CharacterAbility.When.FLYING) {
                this.ability(font, ability, inner);
            }
        }
        if (split) {
            this.heading(font, Component.translatable(PREFIX + "flying"));
            for (CharacterAbility ability : this.character.abilities()) {
                if (!ability.isPlaceholder() && ability.when() == CharacterAbility.When.FLYING) {
                    this.ability(font, ability, inner);
                }
            }
        }
        int height = 0;
        for (Entry entry : this.entries) {
            height += entry.height();
        }
        this.contentHeight = height;
        this.scroll = Mth.clamp(this.scroll, 0.0, this.maxScroll());
    }

    private void ability(Font font, CharacterAbility ability, int inner) {
        Component about = AbilityGuide.about(ability);
        this.add(font, ability.getDisplayName(), keys(ability),
                about == null ? Component.translatable(PREFIX + "none") : about, inner);
    }

    private static Component keys(CharacterAbility ability) {
        boolean mouse = ability.input() == CharacterAbility.Input.LEFT
                || ability.input() == CharacterAbility.Input.RIGHT;
        if (mouse && ability.holdTicks() > 0 && ability.tapWhen() != CharacterAbility.Tap.NEVER) {
            return Component.empty().append(PowerInputs.keyName(PowerInputs.clickKey(ability.input()))).append(" / ")
                    .append(PowerInputs.holdLabel(ability.input()));
        }
        return PowerInputs.label(ability);
    }

    private void add(Font font, Component name, Component keys, Component about, int inner) {
        List<FormattedCharSequence> lines = font.split(about, inner);
        int height = font.lineHeight + 3 + lines.size() * (font.lineHeight + 1) + 8;
        this.entries.add(new Entry(name, keys, lines, height, false));
    }

    private void heading(Font font, Component title) {
        this.entries.add(new Entry(title, Component.empty(), List.of(), font.lineHeight + 10, true));
    }

    private int panelWidth() {
        return Math.min(MAX_WIDTH, this.width - 32);
    }

    private int listTop() {
        return 16 + HEADER;
    }

    private int listBottom() {
        return this.height - 16 - FOOTER;
    }

    private double maxScroll() {
        return Math.max(0, this.contentHeight - (this.listBottom() - this.listTop()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Font font = this.font;
        int width = this.panelWidth();
        int left = (this.width - width) / 2;
        int right = left + width;
        int top = 16;
        int bottom = this.height - 16;
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 2, 0xFF000000 | this.character.getColor());

        graphics.drawString(font, this.title.copy().withStyle(ChatFormatting.BOLD), left + PAD, top + 10,
                0xFF000000 | this.character.getColor());
        graphics.drawString(font, Component.translatable(PREFIX + "subtitle"), left + PAD, top + 23, MUTED);
        graphics.fill(left + PAD, this.listTop() - 4, right - PAD, this.listTop() - 3, LINE);

        int listTop = this.listTop();
        int listBottom = this.listBottom();
        graphics.enableScissor(left, listTop, right, listBottom);
        int y = listTop - (int) Math.round(this.scroll);
        int textRight = right - PAD - 6;
        for (Entry entry : this.entries) {
            if (y + entry.height() >= listTop && y <= listBottom) {
                this.draw(graphics, font, entry, left + PAD, textRight, y);
            }
            y += entry.height();
        }
        graphics.disableScissor();

        double max = this.maxScroll();
        if (max > 0) {
            int track = listBottom - listTop;
            int bar = Math.max(16, track * track / this.contentHeight);
            int barTop = listTop + (int) Math.round((track - bar) * (this.scroll / max));
            graphics.fill(right - PAD + 2, barTop, right - PAD + 4, barTop + bar, SCROLL);
        }
        graphics.fill(left + PAD, listBottom + 3, right - PAD, listBottom + 4, LINE);
        Component close = Component.translatable(max > 0 ? PREFIX + "close_scroll" : PREFIX + "close",
                PowerInputs.keyName(AbilityGuide.KEY));
        graphics.drawCenteredString(font, close, this.width / 2, listBottom + 8, MUTED);
    }

    private void draw(GuiGraphics graphics, Font font, Entry entry, int left, int right, int y) {
        if (entry.heading()) {
            graphics.drawString(font, entry.name().copy().withStyle(ChatFormatting.BOLD), left, y + 2,
                    0xFF000000 | this.character.getColor());
            return;
        }
        int keysWidth = font.width(entry.keys());
        graphics.drawString(font, entry.keys(), right - keysWidth, y, KEY);
        graphics.drawString(font, entry.name(), left, y, NAME);
        int line = y + font.lineHeight + 3;
        for (FormattedCharSequence text : entry.lines()) {
            graphics.drawString(font, text, left, line, TEXT);
            line += font.lineHeight + 1;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Mth.clamp(this.scroll - scrollY * 24.0, 0.0, this.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (AbilityGuide.KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
