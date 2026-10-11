package nl.tivek.multiversepowers.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

// The categories the Commands page sorts commands under: one for each group of built-in commands, "custom" for the
// world's own, and any a host makes. A world setting like the commands themselves: "<id>=<label>" names or renames
// one ("<id>=" takes a built-in one away), and "<command>=<id>" puts a command in one other than its own.
public final class CommandCategories {
    public static final String CUSTOM = "custom";
    // Where a command goes when its category was taken away.
    public static final String NONE = "none";
    public static final int MOST = 32;
    public static final int LONGEST_LABEL = 24;

    // `label` null: the game's own name for a built-in one, from en_us.json.
    public record Category(String id, @Nullable String label, boolean builtIn) {
    }

    private CommandCategories() {
    }

    // Every category in order: the built-in ones that are left, then the host's own.
    public static List<Category> all() {
        Map<String, String> entries = entries();
        List<Category> all = new ArrayList<>();
        for (String id : builtIn()) {
            String label = entries.get(id);
            if (label == null || !label.isEmpty()) {
                all.add(new Category(id, label, true));
            }
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (!builtIn().contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                all.add(new Category(entry.getKey(), entry.getValue(), false));
            }
        }
        return all;
    }

    public static List<String> builtIn() {
        List<String> ids = new ArrayList<>();
        for (ShortCommand.Group group : ShortCommand.Group.values()) {
            ids.add(group.name().toLowerCase(Locale.ROOT));
        }
        ids.add(CUSTOM);
        return ids;
    }

    @Nullable
    public static Category find(String id) {
        for (Category category : all()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return null;
    }

    // The category of a command: `key` is a built-in's own name (it keeps it when renamed) or a custom one's name.
    public static String of(String key, @Nullable ShortCommand builtIn) {
        String placed = CommandAliases.pairs(CommandAliases.PLACED).get(key);
        if (placed != null && find(placed) != null) {
            return placed;
        }
        String own = builtIn == null ? CUSTOM : builtIn.group().name().toLowerCase(Locale.ROOT);
        return find(own) != null ? own : NONE;
    }

    static Map<String, String> entries() {
        return CommandAliases.pairs(CommandAliases.CATEGORIES);
    }

    // A label as a host may give one: one line, at most LONGEST_LABEL long.
    public static String clean(String label) {
        StringBuilder kept = new StringBuilder();
        for (char c : label.trim().toCharArray()) {
            if (!Character.isISOControl(c)) {
                kept.append(c);
            }
        }
        String text = kept.toString().trim();
        return text.length() > LONGEST_LABEL ? text.substring(0, LONGEST_LABEL).trim() : text;
    }

    // A new id from `label`, unlike any other: "fun_stuff", "fun_stuff_2".
    static String newId(String label) {
        String base = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "category";
        }
        base = base.length() > 20 ? base.substring(0, 20) : base;
        Map<String, String> entries = entries();
        String id = base;
        for (int n = 2; entries.containsKey(id) || builtIn().contains(id) || id.equals(NONE); n++) {
            id = base + "_" + n;
        }
        return id;
    }

    static boolean validEntry(Object line) {
        return line instanceof String text && text.length() <= 64 && text.indexOf('=') > 0
                && CommandAliases.NAME.matcher(text.substring(0, text.indexOf('='))).matches();
    }
}
