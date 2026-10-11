package nl.tivek.multiversepowers.command.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.command.CommandCategories;
import nl.tivek.multiversepowers.command.ShortCommand;
import nl.tivek.multiversepowers.command.ShortCommands;

// Every command the Commands page lists, built-in and the world's own in one list, as the world has them now: found
// by a search, shown in one category or all, and sorted.
final class CommandRows {
    static final String COMMAND = "command." + MultiversePowers.MODID + ".";
    // The heading of the built-in commands taken away, listed last.
    static final String REMOVED = "\u0000removed";

    enum Sort {
        CATEGORY, NAME, CUSTOM_FIRST
    }

    // `key`: a built-in's own name (kept when renamed) or a custom command's name.
    record Entry(String key, String name, @Nullable ShortCommand builtIn, @Nullable String command, String category,
            boolean removed) {
        boolean custom() {
            return this.builtIn == null;
        }

        boolean renamed() {
            return this.builtIn != null && !this.builtIn.name().equals(this.name);
        }

        // How it is typed: "/speed [level] [seconds] [player]", or a custom one's "/name".
        String usage() {
            return this.builtIn == null ? "/" + this.name
                    : "/" + this.name + this.builtIn.usage().substring(1 + this.builtIn.name().length());
        }

        Component about() {
            return this.builtIn != null ? Component.translatable(COMMAND + this.builtIn.name())
                    : Component.literal(this.command);
        }
    }

    record Heading(String category, Component label, int count, boolean open) {
    }

    private CommandRows() {
    }

    static List<Entry> all() {
        List<Entry> all = new ArrayList<>();
        Set<String> removed = CommandAliases.removed();
        for (ShortCommand command : ShortCommands.all()) {
            all.add(new Entry(command.name(), CommandAliases.nameOf(command), command, null,
                    CommandCategories.of(command.name(), command), removed.contains(command.name())));
        }
        for (CommandAliases.Alias alias : CommandAliases.all()) {
            all.add(new Entry(alias.name(), alias.name(), null, alias.command(),
                    CommandCategories.of(alias.name(), null), false));
        }
        return all;
    }

    static Component label(String category) {
        CommandCategories.Category found = CommandCategories.find(category);
        if (found != null && found.label() != null) {
            return Component.literal(found.label());
        }
        return Component.translatable(COMMAND + "group." + category);
    }

    // The rows to list: a heading per category (when sorted by it) that folds in and out, the entries of the open ones,
    // and last the built-in commands taken away under a heading of their own. A search opens every heading.
    static List<Object> rows(List<Entry> all, Sort sort, String query, Set<String> open) {
        boolean searching = !query.isBlank();
        List<Entry> shown = new ArrayList<>();
        List<Entry> removed = new ArrayList<>();
        for (Entry entry : all) {
            if (matches(entry, query)) {
                (entry.removed ? removed : shown).add(entry);
            }
        }
        Comparator<Entry> byName = Comparator.comparing(Entry::name);
        List<Object> rows = new ArrayList<>();
        if (sort != Sort.CATEGORY) {
            shown.sort(sort == Sort.CUSTOM_FIRST ? Comparator.comparing((Entry entry) -> !entry.custom())
                    .thenComparing(byName) : byName);
            rows.addAll(shown);
        } else {
            Map<String, List<Entry>> grouped = new LinkedHashMap<>();
            for (CommandCategories.Category category : CommandCategories.all()) {
                grouped.put(category.id(), new ArrayList<>());
            }
            grouped.put(CommandCategories.NONE, new ArrayList<>());
            for (Entry entry : shown) {
                grouped.computeIfAbsent(entry.category, key -> new ArrayList<>()).add(entry);
            }
            for (Map.Entry<String, List<Entry>> group : grouped.entrySet()) {
                if (group.getValue().isEmpty()) {
                    continue;
                }
                boolean opened = searching || open.contains(group.getKey());
                rows.add(new Heading(group.getKey(), label(group.getKey()), group.getValue().size(), opened));
                if (opened) {
                    rows.addAll(group.getValue());
                }
            }
        }
        if (!removed.isEmpty()) {
            removed.sort(byName);
            boolean opened = searching || open.contains(REMOVED);
            rows.add(new Heading(REMOVED, Component.translatable("config." + MultiversePowers.MODID
                    + ".commands.taken_away.heading"), removed.size(), opened));
            if (opened) {
                rows.addAll(removed);
            }
        }
        return rows;
    }

    // How many each category holds, of the commands not taken away.
    static Map<String, Integer> counts(List<Entry> all) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Entry entry : all) {
            if (!entry.removed) {
                counts.merge(entry.category, 1, Integer::sum);
            }
        }
        return counts;
    }

    static boolean matches(Entry entry, String query) {
        String wanted = query.trim().toLowerCase(Locale.ROOT);
        if (wanted.startsWith("/")) {
            wanted = wanted.substring(1);
        }
        if (wanted.isEmpty()) {
            return true;
        }
        return contains(entry.name, wanted) || contains(entry.key, wanted) || contains(entry.usage(), wanted)
                || contains(entry.about().getString(), wanted) || contains(label(entry.category).getString(), wanted)
                || entry.builtIn != null && contains(entry.builtIn.template(), wanted);
    }

    private static boolean contains(@Nullable String text, String wanted) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(wanted);
    }
}
