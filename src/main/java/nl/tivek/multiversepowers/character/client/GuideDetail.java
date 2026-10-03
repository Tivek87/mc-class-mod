package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
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
import nl.tivek.multiversepowers.config.Unit;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The guide's right side: a mode's overview (how you get in and out, what you cannot use there, what is good to know),
// or one control of it (its key, what it does in this mode in one short line, its cooldown and cost, what else sits on
// its key, and folded under that the full explanation of the ability it fires), with whether it can be used right now
// pinned under it.
final class GuideDetail {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    private static final String[] OWN_COSTS = { "powerCost", "staminaCost", "staminaPerTick" };
    static final int GREEN = 0xFF72D96E;
    static final int AMBER = 0xFFE9B44C;
    static final int RED = 0xFFFF6A50;
    static final int GRAY = 0xFF6E6E6E;
    private static final int BLUE = 0xFF7DB8F0;
    // Per overview section (`GuideMode.SECTIONS`): get in, get out, can't use here, good to know.
    private static final int[] SECTION_COLORS = { GREEN, AMBER, RED, BLUE };
    private static final int TEXT = 0xFFFFFFFF;
    private static final int BODY = 0xFFD8D8D8;
    private static final int MUTED = 0xFF9A9A9A;
    private static final int CHIP = 0xFF2A2A2A;
    private static final int LINE = 0x22FFFFFF;
    private static final Component AGAIN = Component.translatable(PREFIX + "again");
    static final int STATUS = 20;
    static final int FOLD = 12;

    record Status(Component text, int color) {
    }

    // What was drawn: its full height, and where its fold line starts within it (-1 when it has none).
    record Drawn(int height, int fold) {
    }

    private GuideDetail() {
    }

    // Draws the scrolled part clipped to the box above the status line.
    static Drawn overview(GuiGraphics graphics, Font font, GuideMode mode, LocalPlayer player, int x, int y, int width,
            int height, double scroll) {
        int bottom = y + height - STATUS;
        graphics.enableScissor(x, y, x + width, bottom);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int top = y - (int) Math.round(scroll);
        int cy = top + 2;
        layer.shadowed(mode.title().copy().withStyle(ChatFormatting.BOLD), x, cy, TEXT);
        cy += 14;
        for (int i = 0; i < GuideMode.SECTIONS.length; i++) {
            List<String> lines = mode.section(GuideMode.SECTIONS[i]);
            if (lines.isEmpty()) {
                continue;
            }
            int color = SECTION_COLORS[i];
            cy = heading(layer, Component.translatable(PREFIX + "section." + GuideMode.SECTIONS[i]), x, cy, color);
            for (String line : lines) {
                GuiShapes.roundRect(graphics, x + 1, cy + 3, 3, 3, 0.0F, color);
                cy = paragraph(layer, font, Component.literal(line), x + 8, cy, width - 8, BODY) + 2;
            }
            cy += 5;
        }
        layer.finish();
        graphics.disableScissor();
        boolean on = mode.active().test(player);
        foot(graphics, font, new Status(Component.translatable(PREFIX + (on ? "mode_on" : "mode_off")),
                on ? GREEN : GRAY), x, bottom, width);
        return new Drawn(cy - top + 4, -1);
    }

    // `more`: the ability's full explanation unfolded under the short line; `hover` whether the mouse is on the fold.
    static Drawn control(GuiGraphics graphics, Font font, GuideMode mode, GuideMode.Control control,
            LocalPlayer player, int x, int y, int width, int height, double scroll, int accent, boolean more,
            boolean hover) {
        int bottom = y + height - STATUS;
        graphics.enableScissor(x, y, x + width, bottom);
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int top = y - (int) Math.round(scroll);
        int cy = top + 2;
        GuideMode.Control parent = mode.parent(control);
        if (parent != null) {
            layer.text(Component.empty().append(mode.name(parent)).append(" »"), x, cy, MUTED);
            cy += 11;
        }
        layer.shadowed(mode.name(control).copy().withStyle(ChatFormatting.BOLD), x, cy, TEXT);
        cy += 14;
        Supplier<Component> key = control.key();
        int after = x + (key == null ? 0 : KeyCap.draw(layer, font, key.get(), x, cy, 13) + 6);
        Component inMode = Component.translatable(PREFIX + "in_mode", mode.title());
        if (control.again()) {
            layer.text(AGAIN, after, cy + 3, accent);
            after += font.width(AGAIN) + 4;
            inMode = Component.literal("· ").append(inMode);
        }
        layer.text(inMode, after, cy + 3, MUTED);
        cy += 19;
        Component does = mode.does(control);
        if (does != null) {
            cy = paragraph(layer, font, does, x, cy, width, BODY) + 3;
        }
        CharacterAbility ability = mode.ability(control);
        int cx = x;
        for (Component tag : tags(mode, control, ability)) {
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
        List<GuideMode.Control> children = mode.children(control);
        if (!children.isEmpty()) {
            cy = heading(layer, Component.translatable(PREFIX + "same_key"), x, cy + 1, MUTED) + 1;
            for (GuideMode.Control child : children) {
                Supplier<Component> childKey = child.key();
                int at = x + (childKey == null ? 0 : KeyCap.draw(layer, font, childKey.get(), x, cy, 11) + 5);
                if (child.again()) {
                    layer.text(AGAIN, at, cy + 2, MUTED);
                    at += font.width(AGAIN) + 5;
                }
                layer.text(mode.name(child), at, cy + 2, BODY);
                cy += 14;
            }
            cy += 2;
        }
        Component about = ability == null || !control.describes() ? null : AbilityGuide.about(ability);
        int fold = -1;
        if (about != null) {
            cy += 2;
            GuiShapes.roundRect(graphics, x, cy, width, 1, 0.0F, LINE);
            cy += 5;
            fold = cy - top;
            Component label = more ? Component.translatable(PREFIX + "less")
                    : Component.translatable(PREFIX + "more", mode.name(control));
            layer.text(Component.literal(more ? "▼ " : "▶ ").append(hover
                    ? label.copy().withStyle(ChatFormatting.UNDERLINE) : label), x, cy + 2, accent);
            cy += FOLD + 2;
            if (more) {
                cy = paragraph(layer, font, about, x, cy, width, BODY);
            }
        }
        layer.finish();
        graphics.disableScissor();
        foot(graphics, font, status(mode, control, player), x, bottom, width);
        return new Drawn(cy - top + 4, fold);
    }

    private static int heading(KeyCap.Layer layer, Component text, int x, int y, int color) {
        layer.text(Component.literal(text.getString().toUpperCase(Locale.ROOT)), x, y, color);
        return y + 11;
    }

    private static int paragraph(KeyCap.Layer layer, Font font, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            layer.sequence(line, x, y, color);
            y += 11;
        }
        return y;
    }

    private static void foot(GuiGraphics graphics, Font font, Status status, int x, int bottom, int width) {
        KeyCap.Layer foot = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, x, bottom + 3, width, 1, 0.0F, LINE);
        KeyCap.dot(graphics, x + 3.5F, bottom + 12.5F, 3.0F, status.color());
        foot.text(status.text(), x + 11, bottom + 9, status.color());
        foot.finish();
    }

    // Its cooldown when it is the ability's own move, and what it costs.
    private static List<Component> tags(GuideMode mode, GuideMode.Control control, @Nullable CharacterAbility ability) {
        List<Component> tags = new ArrayList<>();
        if (ability != null && control.describes() && ability.usesCooldown()) {
            double seconds = CharacterConfig.cooldown(ability) * PowerRules.cooldowns() / 20.0;
            if (seconds >= 0.05) {
                tags.add(Component.translatable(PREFIX + "cooldown", Unit.number(seconds)));
            }
        }
        Component cost = cost(mode, control, ability);
        if (cost != null) {
            tags.add(cost);
        }
        return tags;
    }

    // The setting the control names, else, for the ability's own move, its own power or stamina cost.
    @Nullable
    private static Component cost(GuideMode mode, GuideMode.Control control, @Nullable CharacterAbility ability) {
        GuideMode.Cost named = control.cost();
        CharacterAbility owner = named != null ? mode.character().byName(named.ability())
                : control.describes() ? ability : null;
        if (owner == null) {
            return null;
        }
        for (CharacterAbility.Setting setting : owner.settings()) {
            boolean wanted = named != null ? setting.key().equals(named.setting())
                    : Arrays.asList(OWN_COSTS).contains(setting.key());
            if (wanted) {
                double value = owner.value(setting.key());
                return value <= 0.0 ? Component.translatable(PREFIX + "free")
                        : Component.translatable(PREFIX + "cost", setting.unit().describe(value));
            }
        }
        return null;
    }

    // Whether the control works this moment, and if not, why not.
    static Status status(GuideMode mode, GuideMode.Control control, LocalPlayer player) {
        if (!mode.active().test(player)) {
            return new Status(Component.translatable(PREFIX + "status.mode_off"), GRAY);
        }
        CharacterAbility ability = mode.ability(control);
        if (ability == null) {
            return new Status(Component.translatable(PREFIX + "status.works"), GREEN);
        }
        boolean mouse = ability.input() == CharacterAbility.Input.LEFT
                || ability.input() == CharacterAbility.Input.RIGHT;
        if (mouse && !Gestures.takesMouse(player)) {
            return new Status(Component.translatable(PREFIX + "status.hands"), GRAY);
        }
        return status(ability, control.hold(), player);
    }

    // Whether the ability (`hold`: its hold version) can be used this moment, and if not, why not, in the order the
    // game checks.
    static Status status(CharacterAbility ability, boolean hold, LocalPlayer player) {
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
        if (ClientCharacter.tired(ability)) {
            return new Status(Component.translatable(PREFIX + "status.tired"), AMBER);
        }
        if (!AbilityPanel.affords(ability, hold, player)) {
            return new Status(Component.translatable(PREFIX + "status.power"), RED);
        }
        return new Status(Component.translatable(PREFIX + "status.ready"), GREEN);
    }
}
