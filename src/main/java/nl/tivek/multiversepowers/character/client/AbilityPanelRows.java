package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

// The ability panel's rows: what it lists right now, in its groups, each with its key, its name and what it says.
final class AbilityPanelRows {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".character.";
    private static final int GREEN = 0x92E08C;
    private static final int AMBER = 0xE9C46A;
    private static final int RED = 0xEF7B6E;

    // A mouse button with a click and a hold gets a row for each (`split`); `hold` says which. `prefix` is the word
    // before its key: "hold", "2×".
    private record Row(CharacterAbility ability, @Nullable Component prefix, Component key, Component name,
            boolean split, boolean hold) {
        int id() {
            return this.ability.slot().ordinal() * 2 + (this.hold ? 1 : 0);
        }
    }

    // `ready` when it can be used now; `status` what it is doing or why it waits; `busy` while the move is held or
    // running, which keeps the panel up.
    record Line(int id, int group, @Nullable Component prefix, Component key, Component name, boolean ready,
            @Nullable Component status, int statusColor, boolean busy) {
    }

    private AbilityPanelRows() {
    }

    static List<Line> lines(GameCharacter now, LocalPlayer player, AbilityPanel.Rules rules) {
        List<Line> lines = new ArrayList<>();
        for (Row row : rows(now, player, rules)) {
            lines.add(line(row, rules, player));
        }
        return lines;
    }

    private static boolean onMouse(CharacterAbility ability) {
        return ability.input() == CharacterAbility.Input.LEFT || ability.input() == CharacterAbility.Input.RIGHT;
    }

    static boolean split(CharacterAbility ability) {
        return onMouse(ability) && ability.holdTicks() > 0 && ability.tapWhen() != CharacterAbility.Tap.NEVER;
    }

    private static List<Row> rows(GameCharacter now, LocalPlayer player, AbilityPanel.Rules rules) {
        List<Row> rows = new ArrayList<>();
        for (CharacterAbility ability : now.abilities()) {
            if (ability.isPlaceholder() || !PowerInputs.bound(ability) || !Gestures.active(ability, player)
                    || onMouse(ability) && !Gestures.takesMouse(player)
                    || ClientCharacter.refused(ability, player) != null || !rules.lists(ability, player)) {
                continue;
            }
            CharacterAbility.Input input = ability.input();
            if (split(ability)) {
                rows.add(new Row(ability, null, AbilityPanel.brief(PowerInputs.keyName(PowerInputs.clickKey(input))),
                        name(rules, ability, false, true, player), true, false));
                if (!PowerInputs.holdKey(input).isUnbound()) {
                    rows.add(new Row(ability, word("hold"),
                            AbilityPanel.brief(PowerInputs.keyName(PowerInputs.holdKey(input))),
                            name(rules, ability, true, true, player), true, true));
                }
                continue;
            }
            boolean hold = ability.tapWhen() == CharacterAbility.Tap.NEVER;
            rows.add(new Row(ability, prefix(ability), AbilityPanel.brief(key(ability)),
                    name(rules, ability, hold, false, player), false, hold));
        }
        rows.sort(Comparator.comparingInt(row -> group(row.ability())));
        return rows;
    }

    // The mouse buttons first, then the ability keys, then the space and shift gestures.
    private static int group(CharacterAbility ability) {
        return switch (ability.input()) {
            case LEFT, RIGHT, SCROLL -> 0;
            case KEY -> 1;
            case SPACE, SHIFT -> 2;
        };
    }

    // The key alone: a hold or a double press says so in a word before it.
    private static Component key(CharacterAbility ability) {
        CharacterAbility.Input input = ability.input();
        if (input == CharacterAbility.Input.KEY) {
            KeyMapping key = AbilityKeys.of(ability.slot());
            return key == null ? Component.literal("-") : PowerInputs.keyName(key);
        }
        return PowerInputs.keyName(ability.tapWhen() == CharacterAbility.Tap.NEVER ? PowerInputs.holdKey(input)
                : PowerInputs.clickKey(input));
    }

    @Nullable
    private static Component prefix(CharacterAbility ability) {
        if (ability.input() == CharacterAbility.Input.KEY) {
            return null;
        }
        return switch (ability.tapWhen()) {
            case NEVER -> word("hold");
            case DOUBLE -> word("double");
            default -> null;
        };
    }

    private static Component word(String id) {
        return Component.translatable(PREFIX + "key." + id);
    }

    private static Component name(AbilityPanel.Rules rules, CharacterAbility ability, boolean hold, boolean split,
            LocalPlayer player) {
        Component own = rules.name(ability, hold, player);
        if (own != null) {
            return own;
        }
        String held = "screen." + MultiversePowers.MODID + ".hold." + ability.id();
        return split && hold && Language.getInstance().has(held) ? Component.translatable(held)
                : ability.getDisplayName();
    }

    private static Line line(Row row, AbilityPanel.Rules rules, LocalPlayer player) {
        CharacterAbility ability = row.ability();
        AbilitySlot slot = ability.slot();
        int cooldown = ClientCharacter.cooldownLeft(slot);
        Component running = row.split() ? null : rules.running(ability, player);
        boolean on = row.split() ? row.hold() && MouseHold.holding(ability.input())
                : ability.isHeld() && ClientCharacter.isHeld(slot);
        if (running != null) {
            return line(row, true, running, GREEN, true);
        }
        if (on) {
            return line(row, true, Component.translatable(PREFIX + "holding"), GREEN, true);
        }
        if (cooldown > 0) {
            return line(row, false, Component.literal((cooldown + 19) / 20 + "s"), AMBER, false);
        }
        if (ClientCharacter.tired(ability)) {
            return line(row, false, Component.translatable(PREFIX + "tired"), AMBER, false);
        }
        if (!rules.affords(ability, row.hold(), player)) {
            return line(row, false, Component.translatable(PREFIX + "no_power"), RED, false);
        }
        return line(row, true, null, 0, false);
    }

    private static Line line(Row row, boolean ready, @Nullable Component status, int statusColor, boolean busy) {
        return new Line(row.id(), group(row.ability()), row.prefix(), row.key(), row.name(), ready, status,
                statusColor, busy);
    }
}
