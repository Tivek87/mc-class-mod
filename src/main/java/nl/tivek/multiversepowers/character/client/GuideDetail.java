package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterConfig;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The guide's right side: one ability's name, keys, tags and explanation, and whether it can be used right now.
final class GuideDetail {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    static final int GREEN = 0xFF72D96E;
    static final int AMBER = 0xFFE9B44C;
    static final int RED = 0xFFFF6A50;
    static final int GRAY = 0xFF5D6571;
    private static final int TEXT = 0xFFE6EAF0;
    private static final int BODY = 0xFFC3CAD3;
    private static final int MUTED = 0xFF8B94A1;
    private static final int CHIP = 0xFF1C222C;
    private static final int LINE = 0x22FFFFFF;
    static final int STATUS = 20;

    record Status(Component text, int color) {
    }

    private GuideDetail() {
    }

    // Draws the scrolled part (name to explanation) clipped to the box above the status line; returns its full height.
    static int draw(GuiGraphics graphics, Font font, CharacterAbility ability, LocalPlayer player, int x, int y,
            int width, int height, double scroll) {
        int bottom = y + height - STATUS;
        graphics.enableScissor(x, y, x + width, bottom);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int top = y - (int) Math.round(scroll);
        int cy = top + 2;
        layer.text(ability.getDisplayName().copy().withStyle(ChatFormatting.BOLD), x, cy, TEXT);
        cy += 15;
        for (Binding binding : bindings(ability, player)) {
            int cap = KeyCap.draw(layer, font, binding.key(), x, cy, 13);
            if (binding.does() != null) {
                layer.text(binding.does(), x + cap + 6, cy + 3, MUTED);
            }
            cy += 17;
        }
        int cx = x;
        for (Component tag : tags(ability)) {
            int tagWidth = font.width(tag) + 8;
            if (cx > x && cx + tagWidth > x + width) {
                cx = x;
                cy += 14;
            }
            cx += KeyCap.chip(layer, font, tag, cx, cy, CHIP, MUTED) + 4;
        }
        if (cx > x) {
            cy += 16;
        }
        GuiShapes.roundRect(graphics, x, cy, width, 1, 0.0F, LINE);
        cy += 7;
        Component about = AbilityGuide.about(ability);
        List<FormattedCharSequence> lines = font.split(about == null
                ? Component.translatable(PREFIX + "none") : about, width);
        for (FormattedCharSequence line : lines) {
            layer.sequence(line, x, cy, BODY);
            cy += 11;
        }
        layer.finish();
        graphics.disableScissor();

        KeyCap.Layer foot = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, x, bottom + 3, width, 1, 0.0F, LINE);
        Status status = status(ability, player);
        KeyCap.dot(graphics, x + 3.5F, bottom + 12.5F, 3.0F, status.color());
        foot.text(status.text(), x + 11, bottom + 9, status.color());
        foot.finish();
        return cy - top + 4;
    }

    // A mode: how it starts and ends, a key cap for each control with what it does beside it, and what is off.
    static int draw(GuiGraphics graphics, Font font, GuideMode mode, LocalPlayer player, int x, int y, int width,
            int height, double scroll) {
        int bottom = y + height - STATUS;
        graphics.enableScissor(x, y, x + width, bottom);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int top = y - (int) Math.round(scroll);
        int cy = top + 2;
        layer.text(mode.title().copy().withStyle(ChatFormatting.BOLD), x, cy, TEXT);
        cy += 14;
        for (FormattedCharSequence line : font.split(mode.when(), width)) {
            layer.sequence(line, x, cy, MUTED);
            cy += 11;
        }
        cy += 3;
        GuiShapes.roundRect(graphics, x, cy, width, 1, 0.0F, LINE);
        cy += 7;
        List<Component> keys = new ArrayList<>();
        int column = 0;
        for (GuideMode.Control control : mode.controls()) {
            Component key = control.key().get();
            keys.add(key);
            column = Math.max(column, KeyCap.width(font, key));
        }
        boolean beside = column <= width * 0.42F;
        for (int i = 0; i < keys.size(); i++) {
            KeyCap.draw(layer, font, keys.get(i), x, cy, 13);
            int textX = beside ? x + column + 6 : x + 4;
            int textY = beside ? cy + 3 : cy + 16;
            List<FormattedCharSequence> lines = font.split(mode.does(mode.controls().get(i)), x + width - textX);
            for (FormattedCharSequence line : lines) {
                layer.sequence(line, textX, textY, BODY);
                textY += 11;
            }
            cy = Math.max(cy + 17, textY + 4);
        }
        Component off = mode.off();
        if (off != null) {
            cy += 2;
            for (FormattedCharSequence line : font.split(Component.translatable(PREFIX + "off", off), width)) {
                layer.sequence(line, x, cy, AMBER);
                cy += 11;
            }
        }
        layer.finish();
        graphics.disableScissor();

        KeyCap.Layer foot = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, x, bottom + 3, width, 1, 0.0F, LINE);
        boolean on = mode.active().test(player);
        int color = on ? GREEN : GRAY;
        KeyCap.dot(graphics, x + 3.5F, bottom + 12.5F, 3.0F, color);
        foot.text(Component.translatable(PREFIX + (on ? "mode_on" : "mode_off")), x + 11, bottom + 9, color);
        foot.finish();
        return cy - top + 4;
    }

    private record Binding(Component key, @Nullable Component does) {
    }

    // A mouse button with a click and a hold shows both, each with what it does.
    private static List<Binding> bindings(CharacterAbility ability, LocalPlayer player) {
        boolean mouse = ability.input() == CharacterAbility.Input.LEFT
                || ability.input() == CharacterAbility.Input.RIGHT;
        if (mouse && ability.holdTicks() > 0 && ability.tapWhen() != CharacterAbility.Tap.NEVER) {
            return List.of(
                    new Binding(PowerInputs.keyName(PowerInputs.clickKey(ability.input())),
                            AbilityPanel.rowName(ability, false, player)),
                    new Binding(PowerInputs.holdLabel(ability.input()), AbilityPanel.rowName(ability, true, player)));
        }
        return List.of(new Binding(PowerInputs.label(ability), null));
    }

    private static List<Component> tags(CharacterAbility ability) {
        List<Component> tags = new ArrayList<>();
        if (ability.when() == CharacterAbility.When.GROUND) {
            tags.add(Component.translatable(PREFIX + "ground"));
        } else if (ability.when() == CharacterAbility.When.FLYING) {
            tags.add(Component.translatable(PREFIX + "flying"));
        }
        if (ability.usesCooldown()) {
            double seconds = CharacterConfig.cooldown(ability) * PowerRules.cooldowns() / 20.0;
            if (seconds >= 0.05) {
                tags.add(Component.translatable(PREFIX + "cooldown", seconds(seconds)));
            }
        }
        if (ability.has(ClientCharacter.POWER_COST)) {
            tags.add(Component.translatable(PREFIX + "power"));
        }
        return tags;
    }

    private static String seconds(double seconds) {
        double tenths = Math.round(seconds * 10.0) / 10.0;
        return tenths == Math.rint(tenths) ? String.valueOf((long) tenths) : String.valueOf(tenths);
    }

    // Whether the ability can be used this moment, and if not, why not, in the order the game checks.
    static Status status(CharacterAbility ability, LocalPlayer player) {
        if (!PowerInputs.bound(ability)) {
            return new Status(Component.translatable(PREFIX + "status.unbound"), RED);
        }
        Component why = ClientCharacter.refused(ability, player);
        if (why != null) {
            return new Status(why, GRAY);
        }
        if (!ClientCharacter.inPlay(ability, player)) {
            boolean flies = ClientCharacter.flies(ability.character(), player);
            if (ability.when() == CharacterAbility.When.GROUND && flies
                    || ability.when() == CharacterAbility.When.FLYING && !flies) {
                return new Status(Component.translatable(PREFIX + "status." + (flies ? "ground" : "flying")), GRAY);
            }
            Component state = AbilityPanel.unavailable(ability, player);
            return new Status(state == null ? Component.translatable(PREFIX + "status.not_now") : state, GRAY);
        }
        int cooldown = ClientCharacter.cooldownLeft(ability.slot());
        if (cooldown > 0) {
            return new Status(Component.translatable(PREFIX + "status.cooldown", (cooldown + 19) / 20), AMBER);
        }
        if (!ClientCharacter.canPay(player, ability)) {
            return new Status(Component.translatable(PREFIX + "status.power"), RED);
        }
        return new Status(Component.translatable(PREFIX + "status.ready"), GREEN);
    }
}
