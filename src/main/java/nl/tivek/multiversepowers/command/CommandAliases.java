package nl.tivek.multiversepowers.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
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
// already uses is refused, and so is one that would run itself. The built-in short commands may be renamed or taken
// away here too, and every command put in a category (`CommandCategories`).
public final class CommandAliases {
    public static final String FILE = ModConfigs.file("commands");
    public static final int MOST = 100;
    public static final int LONGEST = 256;
    public static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,24}");
    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.ConfigValue<List<? extends String>> ALIASES;
    static final ModConfigSpec.ConfigValue<List<? extends String>> CATEGORIES;
    static final ModConfigSpec.ConfigValue<List<? extends String>> PLACED;
    static final ModConfigSpec.ConfigValue<List<? extends String>> RENAMED;
    static final ModConfigSpec.ConfigValue<List<? extends String>> REMOVED;
    // The command nodes made for aliases and for renamed built-ins: a name stays the kind it was made for until the
    // world starts again, as the game cannot take a command away.
    private static final Set<String> ALIAS_NODES = new HashSet<>();
    private static final Map<String, ShortCommand> BUILT_NODES = new HashMap<>();
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
        CATEGORIES = builder.comment("Categories on the Commands page: \"<id>=<label>\" makes one or renames a built-in"
                + " one (mode, time, weather, world, rules, effects, items, player, move, mobs, custom); \"<id>=\""
                + " takes a built-in one away.")
                .defineListAllowEmpty("categories", List.of(), () -> "fun=Fun", CommandCategories::validEntry);
        PLACED = builder.comment("\"<command>=<category id>\": a command in a category other than its own (a built-in"
                + " command by its own name, even when renamed).")
                .defineListAllowEmpty("placed", List.of(), () -> "gm1=mode", CommandAliases::validPair);
        RENAMED = builder.comment("\"<built-in name>=<new name>\": a built-in short command under another name.")
                .defineListAllowEmpty("renamed", List.of(), () -> "day=morning", CommandAliases::validPair);
        REMOVED = builder.comment("Built-in short commands taken away, by their own name.")
                .defineListAllowEmpty("removed", List.of(), () -> "storm",
                        line -> line instanceof String text && NAME.matcher(text).matches());
        builder.pop();
        SPEC = builder.build();
    }

    // "<key>=<value>" lines as a map, in order; empty while the world's file is not loaded.
    static Map<String, String> pairs(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        Map<String, String> pairs = new LinkedHashMap<>();
        if (SPEC.isLoaded()) {
            for (String line : value.get()) {
                int is = line.indexOf('=');
                if (is > 0) {
                    pairs.put(line.substring(0, is), line.substring(is + 1));
                }
            }
        }
        return pairs;
    }

    private static List<String> lines(Map<String, String> pairs) {
        List<String> lines = new ArrayList<>();
        pairs.forEach((key, value) -> lines.add(key + "=" + value));
        return lines;
    }

    private static boolean validPair(Object line) {
        if (!(line instanceof String text) || text.length() > 64) {
            return false;
        }
        int is = text.indexOf('=');
        return is > 0 && NAME.matcher(text.substring(0, is)).matches()
                && NAME.matcher(text.substring(is + 1)).matches();
    }

    public static Set<String> removed() {
        Set<String> removed = new HashSet<>();
        if (SPEC.isLoaded()) {
            removed.addAll(REMOVED.get());
        }
        return removed;
    }

    // The name a built-in short command goes by in this world.
    public static String nameOf(ShortCommand command) {
        return pairs(RENAMED).getOrDefault(command.name(), command.name());
    }

    // Whether `/literal` runs `command` now: not taken away, and that is the name it goes by.
    static boolean answers(ShortCommand command, String literal) {
        return !removed().contains(command.name()) && nameOf(command).equals(literal);
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

    // Every alias of the world and every built-in's new name that has no command yet becomes one; those taken away
    // stay, but none may use them.
    static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        for (Alias alias : all()) {
            String name = alias.name();
            if (dispatcher.getRoot().getChild(name) != null) {
                continue;
            }
            dispatcher.register(Commands.literal(name)
                    .requires(source -> command(name) != null)
                    .executes(c -> run(c, name, ""))
                    .then(Commands.argument("more", StringArgumentType.greedyString())
                            .executes(c -> run(c, name, StringArgumentType.getString(c, "more")))));
            ALIAS_NODES.add(name);
        }
        for (Map.Entry<String, String> renamed : pairs(RENAMED).entrySet()) {
            ShortCommand command = ShortCommands.find(renamed.getKey());
            String name = renamed.getValue();
            if (command == null || dispatcher.getRoot().getChild(name) != null) {
                continue;
            }
            dispatcher.register(ShortCommandRegistry.build(command, context, name));
            BUILT_NODES.put(name, command);
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
    public static void sync(MinecraftServer server) {
        register(server.getCommands().getDispatcher(), CommandBuildContext.simple(server.registryAccess(),
                server.getWorldData().enabledFeatures()));
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
        ALIAS_NODES.clear();
        BUILT_NODES.clear();
    }

    // A host's change from the Commands page, checked again here; what is refused is told in the chat.
    public static void change(ServerPlayer player, CommandEditPayload edit) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        if (!SPEC.isLoaded()) {
            return;
        }
        MinecraftServer server = player.server;
        String refused = switch (edit.action()) {
            case CommandEditPayload.SAVE -> save(server, edit.a(), edit.b(), edit.c(), edit.d());
            case CommandEditPayload.DELETE -> delete(edit.a());
            case CommandEditPayload.BUILT_IN -> builtIn(server, edit.a(), edit.b(), edit.d());
            case CommandEditPayload.REMOVE -> remove(edit.a(), true);
            case CommandEditPayload.RESTORE -> remove(edit.a(), false);
            case CommandEditPayload.CATEGORY -> category(edit.a(), edit.b());
            case CommandEditPayload.CATEGORY_RESET -> resetCategory(edit.a());
            default -> "bad_name";
        };
        if (refused != null) {
            player.displayClientMessage(Component.translatable(KEY + refused, strip(edit.b()).isEmpty() ? edit.a()
                    : strip(edit.b())), false);
            return;
        }
        WorldSettings.store(FILE);
        sync(server);
    }

    public static String strip(String typed) {
        String text = typed.trim();
        return text.startsWith("/") ? text.substring(1).trim() : text;
    }

    // A custom command made, or changed from `oldName` (empty for a new one).
    @Nullable
    private static String save(MinecraftServer server, String oldName, String rawName, String rawCommand,
            String category) {
        String old = strip(oldName).toLowerCase(Locale.ROOT);
        String name = strip(rawName).toLowerCase(Locale.ROOT);
        String command = strip(rawCommand);
        if (!NAME.matcher(name).matches()) {
            return "bad_name";
        }
        if (command.isEmpty()) {
            return "empty";
        }
        if (command.length() > LONGEST) {
            return "too_long";
        }
        String taken = taken(server, name, old, null);
        if (taken != null) {
            return taken;
        }
        String first = command.split(" ", 2)[0];
        int others = 0;
        for (Alias alias : all()) {
            if (alias.name().equals(name) || alias.name().equals(old)) {
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
        if (others >= MOST) {
            return "full";
        }
        Map<String, String> aliases = pairs(ALIASES);
        Map<String, String> kept = new LinkedHashMap<>();
        boolean put = false;
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            if (alias.getKey().equals(old) || alias.getKey().equals(name)) {
                if (!put) {
                    kept.put(name, command);
                    put = true;
                }
            } else {
                kept.put(alias.getKey(), alias.getValue());
            }
        }
        if (!put) {
            kept.put(name, command);
        }
        ALIASES.set(lines(kept));
        place(old, null);
        place(name, category.equals(CommandCategories.CUSTOM) ? null : category);
        return null;
    }

    @Nullable
    private static String delete(String rawName) {
        String name = strip(rawName).toLowerCase(Locale.ROOT);
        Map<String, String> aliases = pairs(ALIASES);
        if (aliases.remove(name) == null) {
            return "bad_name";
        }
        ALIASES.set(lines(aliases));
        place(name, null);
        return null;
    }

    // A built-in short command renamed (its own name, or none, gives it its name back) and put in `category`.
    @Nullable
    private static String builtIn(MinecraftServer server, String original, String rawName, String category) {
        ShortCommand command = ShortCommands.find(original);
        if (command == null) {
            return "bad_name";
        }
        String name = strip(rawName).toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            name = original;
        }
        if (!NAME.matcher(name).matches()) {
            return "bad_name";
        }
        if (!name.equals(original)) {
            String taken = taken(server, name, null, command);
            if (taken != null) {
                return taken;
            }
        }
        Map<String, String> renamed = pairs(RENAMED);
        if (name.equals(original)) {
            renamed.remove(original);
        } else {
            renamed.put(original, name);
        }
        RENAMED.set(lines(renamed));
        place(original, category.equals(command.group().name().toLowerCase(Locale.ROOT)) ? null : category);
        return null;
    }

    // A built-in taken away (`gone`) or given back; given back with no name, every one comes back.
    @Nullable
    private static String remove(String original, boolean gone) {
        Set<String> removed = new LinkedHashSet<>(REMOVED.get());
        if (!gone && original.isEmpty()) {
            removed.clear();
        } else if (ShortCommands.find(original) == null) {
            return "bad_name";
        } else if (gone) {
            removed.add(original);
        } else {
            removed.remove(original);
        }
        REMOVED.set(new ArrayList<>(removed));
        return null;
    }

    // A category made (no `id`), renamed, or taken away (no label); its commands go back to their own.
    @Nullable
    private static String category(String id, String rawLabel) {
        String label = CommandCategories.clean(rawLabel);
        Map<String, String> entries = CommandCategories.entries();
        boolean builtIn = CommandCategories.builtIn().contains(id);
        if (id.isEmpty()) {
            if (label.isEmpty()) {
                return "bad_label";
            }
            long own = entries.keySet().stream().filter(key -> !CommandCategories.builtIn().contains(key)).count();
            if (own >= CommandCategories.MOST) {
                return "full_categories";
            }
            entries.put(CommandCategories.newId(label), label);
        } else if (builtIn) {
            entries.put(id, label);
        } else if (!entries.containsKey(id)) {
            return "bad_label";
        } else if (label.isEmpty()) {
            entries.remove(id);
        } else {
            entries.put(id, label);
        }
        CATEGORIES.set(lines(entries));
        if (label.isEmpty()) {
            Map<String, String> placed = pairs(PLACED);
            placed.values().removeIf(id::equals);
            PLACED.set(lines(placed));
        }
        return null;
    }

    // A built-in category back as it came: its own name, and there again if it was taken away.
    @Nullable
    private static String resetCategory(String id) {
        Map<String, String> entries = CommandCategories.entries();
        if (!CommandCategories.builtIn().contains(id)) {
            return "bad_label";
        }
        entries.remove(id);
        CATEGORIES.set(lines(entries));
        return null;
    }

    private static void place(String key, @Nullable String category) {
        Map<String, String> placed = pairs(PLACED);
        if (category == null || CommandCategories.find(category) == null) {
            placed.remove(key);
        } else {
            placed.put(key, category);
        }
        PLACED.set(lines(placed));
    }

    // Why `name` cannot be given to an alias (`ownAlias` its name now) or a built-in (`ownBuiltIn`), or null: a
    // built-in's own name stays its own, and a name the game or another mod has is theirs.
    @Nullable
    private static String taken(MinecraftServer server, String name, @Nullable String ownAlias,
            @Nullable ShortCommand ownBuiltIn) {
        for (ShortCommand command : ShortCommands.all()) {
            if (command != ownBuiltIn && (command.name().equals(name) || nameOf(command).equals(name))) {
                return "built_in";
            }
        }
        for (Alias alias : all()) {
            if (alias.name().equals(name) && !alias.name().equals(ownAlias)) {
                return "taken";
            }
        }
        if (server.getCommands().getDispatcher().getRoot().getChild(name) == null) {
            return null;
        }
        if (ALIAS_NODES.contains(name)) {
            return ownBuiltIn == null ? null : "restart";
        }
        ShortCommand made = BUILT_NODES.get(name);
        if (made != null) {
            return made == ownBuiltIn ? null : "restart";
        }
        return "taken";
    }
}
