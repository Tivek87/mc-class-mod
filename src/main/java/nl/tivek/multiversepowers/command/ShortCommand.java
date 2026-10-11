package nl.tivek.multiversepowers.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;

// One short command: `/name` and its arguments, each left out from the end taking its fallback. It runs either
// `template`, the game's own commands with `$<argument>` filled in and split on ";", or `action`, code of its own.
public record ShortCommand(String name, Group group, int permission, List<Arg> args, @Nullable String template,
        @Nullable Action action) {

    public enum Group {
        MODE,
        TIME,
        WEATHER,
        WORLD,
        RULES,
        EFFECTS,
        ITEMS,
        PLAYER,
        MOVE,
        MOBS
    }

    // What an argument takes: a whole number, an effect's level from 1 (the game's amplifier is one less), seconds,
    // on or off, players (a name or a selector), an item, an effect, an enchantment, a creature, or any text.
    public enum Kind {
        NUMBER,
        LEVEL,
        SECONDS,
        SWITCH,
        PLAYERS,
        ITEM,
        EFFECT,
        ENCHANTMENT,
        ENTITY,
        TEXT
    }

    // `fallback` null: the argument must be given.
    public record Arg(String name, Kind kind, @Nullable String fallback) {
    }

    @FunctionalInterface
    public interface Action {
        // `args` holds every argument as typed, or its fallback.
        int run(CommandSourceStack source, Map<String, String> args) throws CommandSyntaxException;
    }

    // How it is typed, such as "/speed [level] [seconds] [player]".
    public String usage() {
        StringBuilder usage = new StringBuilder("/").append(this.name);
        for (Arg arg : this.args) {
            usage.append(arg.fallback() == null ? " <" : " [").append(arg.name()).append(arg.fallback() == null ? ">"
                    : "]");
        }
        return usage.toString();
    }
}
