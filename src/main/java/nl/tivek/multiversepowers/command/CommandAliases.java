package nl.tivek.multiversepowers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettings;

// The world's own short commands, at most `MOST`: each a name standing for one of the game's commands, which runs with
// whatever is typed after the name added to it. A world setting, "<name>=<command>"; a name the game or another mod
// already uses is refused, and so is one that would run itself.
public final class CommandAliases {
    public static final String FILE = ModConfigs.file("commands");
    public static final int MOST = 25;
    public static final int LONGEST = 256;
    public static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,24}");
    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.ConfigValue<List<? extends String>> ALIASES;
    // The command nodes made for aliases, so a name can be taken over again by one of them.
    private static final Set<CommandNode<CommandSourceStack>> NODES = Collections.newSetFromMap(
            new IdentityHashMap<>());
    private static final String KEY = "command." + MultiversePowers.MODID + ".alias.";

    public record Alias(String name, String command) {
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "This world's own short commands.",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "Easiest changed in the game: settings window > Commands.").push("commands");
        ALIASES = builder.comment("At most " + MOST + ", one per line: \"<name>=<command>\", the command without its /,"
                + " for example \"gm1=gamemode creative\" makes /gm1. Words typed after the name are added to the"
                + " command.")
                .defineListAllowEmpty("aliases", List.of(), () -> "gm1=gamemode creative", CommandAliases::valid);
        builder.pop();
        SPEC = builder.build();
    }

    private CommandAliases() {
    }

    public static List<Alias> all() {
        List<Alias> all = new ArrayList<>();
        if (!SPEC.isLoaded()) {
            return all;
        }
        for (String line : ALIASES.get()) {
            int is = line.indexOf('=');
            if (is > 0 && all.size() < MOST) {
                all.add(new Alias(line.substring(0, is), line.substring(is + 1)));
            }
        }
        return all;
    }

    @Nullable
    private static String command(String name) {
        for (Alias alias : all()) {
            if (alias.name().equals(name)) {
                return alias.command();
            }
        }
        return null;
    }

    private static boolean valid(Object line) {
        if (!(line instanceof String text) || text.length() > LONGEST + 30) {
            return false;
        }
        int is = text.indexOf('=');
        return is > 0 && NAME.matcher(text.substring(0, is)).matches() && is < text.length() - 1;
    }

    // Every alias of the world that has no command yet becomes one; those taken away stay, but none may use them.
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Alias alias : all()) {
            String name = alias.name();
            if (dispatcher.getRoot().getChild(name) != null) {
                continue;
            }
            CommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal(name)
                    .requires(source -> command(name) != null)
                    .executes(context -> run(context, name, ""))
                    .then(Commands.argument("more", StringArgumentType.greedyString())
                            .executes(context -> run(context, name, StringArgumentType.getString(context, "more")))));
            NODES.add(node);
        }
    }

    private static int run(CommandContext<CommandSourceStack> context, String name, String more) {
        String command = command(name);
        if (command == null) {
            return 0;
        }
        context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(),
                more.isEmpty() ? command : command + " " + more);
        return 1;
    }

    // The aliases as the file has them now, made commands, and every player's list of commands sent again.
    static void sync(MinecraftServer server) {
        register(server.getCommands().getDispatcher());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    public static void onConfig(ModConfigEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && event.getConfig().getSpec() == SPEC && event.getConfig().getLoadedConfig() != null) {
            server.execute(() -> sync(server));
        }
    }

    static void clear() {
        NODES.clear();
    }

    // A host's change from the Commands page: an empty `command` takes the alias away.
    public static void edit(ServerPlayer player, String rawName, String rawCommand) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        String name = strip(rawName).toLowerCase(Locale.ROOT);
        String command = strip(rawCommand);
        String refused = refusal(player.server, name, command);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(KEY + refused, name), false);
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Alias alias : all()) {
            if (!alias.name().equals(name)) {
                lines.add(alias.name() + "=" + alias.command());
            }
        }
        if (!command.isEmpty()) {
            lines.add(name + "=" + command);
        }
        ALIASES.set(lines);
        WorldSettings.store(FILE);
        sync(player.server);
        player.displayClientMessage(Component.translatable(KEY + (command.isEmpty() ? "removed" : "saved"), name),
                false);
    }

    public static String strip(String typed) {
        String text = typed.trim();
        return text.startsWith("/") ? text.substring(1).trim() : text;
    }

    // Why the alias cannot be made (a key under `command.<mod>.alias.`), or null.
    @Nullable
    private static String refusal(MinecraftServer server, String name, String command) {
        if (!NAME.matcher(name).matches() || !SPEC.isLoaded()) {
            return "bad_name";
        }
        if (command.isEmpty()) {
            return null;
        }
        if (command.length() > LONGEST) {
            return "too_long";
        }
        CommandNode<CommandSourceStack> taken = server.getCommands().getDispatcher().getRoot().getChild(name);
        if (taken != null && !NODES.contains(taken)) {
            return "taken";
        }
        String first = command.split(" ", 2)[0];
        int others = 0;
        for (Alias alias : all()) {
            if (alias.name().equals(name)) {
                continue;
            }
            others++;
            if (alias.name().equals(first) || alias.command().split(" ", 2)[0].equals(name)) {
                return "loop";
            }
        }
        if (first.equals(name)) {
            return "loop";
        }
        return others >= MOST ? "full" : null;
    }
}
