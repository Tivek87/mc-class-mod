package nl.tivek.multiversepowers.faction.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import nl.tivek.multiversepowers.faction.Standing;

// The world's factions as its settings file holds them, read into one table. Three lists of lines: a faction's own
// words and numbers, "<faction>|<key>=<value>" (a faction is there once it has a line); which faction a kind of
// creature is in, "<creature>=<faction>" (nothing after the = for none); and how one faction stands to another,
// "<faction>><other>=friendly|neutral|hostile" (a faction toward itself is how its members stand to each other).
// The same lists are changed through the `with...` methods, which give a new list each.
public final class FactionTable {
    public static final Pattern ID = Pattern.compile("[A-Za-z0-9_.+-]{1,16}");
    public static final String[] KEYS = {"name", "description", "lore", "color", "attitude", "reputation", "hostileAt",
        "friendlyAt", "help", "open"};
    public static final int NAME = 40;
    public static final int TEXT = 300;
    public static final int[] COLORS = {0xE25C5C, 0xF2994A, 0xF2C84B, 0x8BD45A, 0x4CC9A6, 0x5CB8E2, 0x6F8CF0, 0xA57BEA,
        0xE27BC4, 0xC9A27A, 0xB8C0CC, 0x7A8494};
    public static final String GAME = "game";
    private static final String KIND = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    private static final Pattern FACTION_LINE = Pattern.compile(ID.pattern() + "\\|(" + String.join("|", KEYS)
            + ")=[^\\r\\n]{0," + TEXT + "}");
    private static final Pattern MEMBER_LINE = Pattern.compile(KIND + "=(" + ID.pattern() + ")?");
    private static final Pattern RELATION_LINE = Pattern.compile(ID.pattern() + ">" + ID.pattern()
            + "=(friendly|neutral|hostile)");
    public static final FactionTable EMPTY = new FactionTable(Map.of(), Map.of(), Map.of());

    private final Map<String, Faction> factions;
    private final Map<String, String> members;
    private final Map<String, Standing> relations;

    private FactionTable(Map<String, Faction> factions, Map<String, String> members, Map<String, Standing> relations) {
        this.factions = factions;
        this.members = members;
        this.relations = relations;
    }

    public static boolean validFaction(Object line) {
        return line instanceof String text && FACTION_LINE.matcher(text).matches();
    }

    public static boolean validMember(Object line) {
        return line instanceof String text && text.length() <= 240 && MEMBER_LINE.matcher(text).matches();
    }

    public static boolean validRelation(Object line) {
        return line instanceof String text && RELATION_LINE.matcher(text).matches();
    }

    public static FactionTable parse(List<? extends String> factionLines, List<? extends String> memberLines,
            List<? extends String> relationLines) {
        Map<String, Map<String, String>> words = new LinkedHashMap<>();
        for (String line : factionLines) {
            int bar = line.indexOf('|');
            int is = line.indexOf('=', bar + 1);
            if (bar > 0 && is > bar) {
                words.computeIfAbsent(line.substring(0, bar), id -> new HashMap<>())
                        .put(line.substring(bar + 1, is), line.substring(is + 1));
            }
        }
        Map<String, Faction> factions = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : words.entrySet()) {
            factions.put(entry.getKey(), faction(entry.getKey(), entry.getValue()));
        }
        Map<String, String> members = new HashMap<>();
        for (String line : memberLines) {
            int is = line.indexOf('=');
            if (is > 0) {
                members.put(line.substring(0, is), line.substring(is + 1));
            }
        }
        Map<String, Standing> relations = new HashMap<>();
        for (String line : relationLines) {
            int is = line.indexOf('=');
            Standing standing = is < 0 ? null : standing(line.substring(is + 1));
            if (standing != null) {
                relations.put(line.substring(0, is), standing);
            }
        }
        return new FactionTable(factions, members, relations);
    }

    private static Faction faction(String id, Map<String, String> words) {
        int hostileAt = Math.max(Faction.LOWEST, Math.min(-1, whole(words.get("hostileAt"), Faction.HOSTILE_AT)));
        int friendlyAt = Math.max(1, Math.min(Faction.HIGHEST, whole(words.get("friendlyAt"), Faction.FRIENDLY_AT)));
        String name = words.getOrDefault("name", "");
        return new Faction(id, name.isBlank() ? id : name, words.getOrDefault("description", ""),
                words.getOrDefault("lore", ""), color(id, words.get("color")), standing(words.get("attitude")),
                !"false".equals(words.get("reputation")), hostileAt, friendlyAt, !"false".equals(words.get("help")),
                "true".equals(words.get("open")));
    }

    private static int whole(@Nullable String text, int fallback) {
        try {
            return text == null ? fallback : Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // A faction's colour as written (six hex digits), else one of the palette that stays the same for its id.
    static int color(String id, @Nullable String written) {
        if (written != null && written.matches("[0-9a-fA-F]{6}")) {
            return Integer.parseInt(written, 16);
        }
        return COLORS[Math.floorMod(id.hashCode(), COLORS.length)];
    }

    @Nullable
    public static Standing standing(@Nullable String name) {
        if (name != null) {
            for (Standing standing : Standing.values()) {
                if (standing.name().toLowerCase(Locale.ROOT).equals(name)) {
                    return standing;
                }
            }
        }
        return null;
    }

    public static String name(@Nullable Standing standing) {
        return standing == null ? GAME : standing.name().toLowerCase(Locale.ROOT);
    }

    // Text as a line can hold it: one line, no stray spaces, at most `max` long.
    public static String clean(String text, int max) {
        String one = text.replaceAll("[\\p{Cntrl}]+", " ").replaceAll(" {2,}", " ").strip();
        return one.length() <= max ? one : one.substring(0, max).strip();
    }

    @Nullable
    public Faction get(@Nullable String id) {
        return id == null ? null : this.factions.get(id);
    }

    public Collection<Faction> all() {
        return this.factions.values();
    }

    // The faction a kind of creature is put in: its id, "" for none, or null where no line says.
    @Nullable
    public String member(String kind) {
        return this.members.get(kind);
    }

    // The kinds put in `faction` by a line of their own.
    public List<String> kinds(String faction) {
        List<String> kinds = new ArrayList<>();
        for (Map.Entry<String, String> entry : this.members.entrySet()) {
            if (entry.getValue().equals(faction)) {
                kinds.add(entry.getKey());
            }
        }
        kinds.sort(null);
        return kinds;
    }

    // How `from` stands toward `to` where the world says so; null where it does not.
    @Nullable
    public Standing relation(String from, String to) {
        return this.relations.get(from + ">" + to);
    }

    // Whether `faction` is set hostile toward any faction, so its creatures hunt.
    public boolean hunts(String faction) {
        String prefix = faction + ">";
        for (Map.Entry<String, Standing> entry : this.relations.entrySet()) {
            if (entry.getValue() == Standing.HOSTILE && entry.getKey().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // `lines` with the faction's `key` set to `value`, beside its other lines; a new faction goes last.
    public static List<String> withWord(List<? extends String> lines, String id, String key, String value) {
        String prefix = id + "|" + key + "=";
        List<String> next = new ArrayList<>();
        int last = -1;
        boolean put = false;
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                next.add(prefix + value);
                put = true;
                continue;
            }
            next.add(line);
            if (line.startsWith(id + "|")) {
                last = next.size();
            }
        }
        if (!put) {
            next.add(last < 0 ? next.size() : last, prefix + value);
        }
        return next;
    }

    public static List<String> withoutFaction(List<? extends String> lines, String id) {
        List<String> next = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith(id + "|")) {
                next.add(line);
            }
        }
        return next;
    }

    // `lines` with `kind` put in `faction` ("" for none); null takes its line away, so its own kind decides again.
    public static List<String> withMember(List<? extends String> lines, String kind, @Nullable String faction) {
        List<String> next = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith(kind + "=")) {
                next.add(line);
            }
        }
        if (faction != null) {
            next.add(kind + "=" + faction);
        }
        return next;
    }

    // Every creature of `faction` left with no faction line at all.
    public static List<String> withoutMembers(List<? extends String> lines, String faction) {
        List<String> next = new ArrayList<>();
        for (String line : lines) {
            if (!line.endsWith("=" + faction)) {
                next.add(line);
            }
        }
        return next;
    }

    // `lines` with how `from` stands toward `to`; null takes the line away.
    public static List<String> withRelation(List<? extends String> lines, String from, String to,
            @Nullable Standing standing) {
        String prefix = from + ">" + to + "=";
        List<String> next = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith(prefix)) {
                next.add(line);
            }
        }
        if (standing != null) {
            next.add(prefix + name(standing));
        }
        return next;
    }

    public static List<String> withoutRelations(List<? extends String> lines, String faction) {
        List<String> next = new ArrayList<>();
        for (String line : lines) {
            int arrow = line.indexOf('>');
            int is = line.indexOf('=');
            if (arrow < 0 || is < arrow || !line.substring(0, arrow).equals(faction)
                    && !line.substring(arrow + 1, is).equals(faction)) {
                next.add(line);
            }
        }
        return next;
    }
}
