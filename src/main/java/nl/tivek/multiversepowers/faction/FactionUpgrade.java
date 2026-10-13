package nl.tivek.multiversepowers.faction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import nl.tivek.multiversepowers.faction.rules.FactionDefaults;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// A world made with the first factions (Villagers, Undead and the rest, each going by the game) gets the peoples of
// today in their place, once: their players, reputation and pinned standings move to the faction that took over,
// creatures and relations a player set on factions of their own stay, and an attitude of "game" becomes neutral. Each
// raise of VERSION also gives the kinds of creature added since (the Elemental Blaze) their faction.
final class FactionUpgrade {
    static final int VERSION = 3;
    private static final String GAME = "|attitude=game";
    private static final Map<String, String> RENAMED = Map.ofEntries(Map.entry("villagers", "hearthbound"),
            Map.entry("illagers", "covenant"), Map.entry("undead", "legion"), Map.entry("piglins", "horde"),
            Map.entry("nether", "cinder"), Map.entry("end", "endwatch"), Map.entry("monsters", "creeping"),
            Map.entry("wildlife", "wildkin"), Map.entry("farm", "meadowfolk"), Map.entry("sea", "tidefolk"),
            Map.entry("arcane", "conclave"));

    private FactionUpgrade() {
    }

    static void run(MinecraftServer server, FactionData data) {
        if (data.version() >= VERSION || !FactionRules.SPEC.isLoaded()) {
            return;
        }
        List<String> factions = new ArrayList<>(FactionRules.FACTIONS.get());
        boolean first = factions.stream().anyMatch(line -> RENAMED.containsKey(id(line)) && line.endsWith(GAME));
        List<String> kept = new ArrayList<>();
        for (String line : factions) {
            if (!first || !RENAMED.containsKey(id(line))) {
                kept.add(line.endsWith(GAME) ? line.substring(0, line.length() - GAME.length()) + "|attitude=neutral"
                        : line);
            }
        }
        if (first) {
            Set<String> have = new HashSet<>();
            kept.forEach(line -> have.add(id(line)));
            for (String line : FactionDefaults.factions()) {
                if (!have.contains(id(line))) {
                    kept.add(line);
                }
            }
            FactionRules.MEMBERS.set(members());
            FactionRules.RELATIONS.set(relations());
            teams(server.getScoreboard());
            RENAMED.forEach(data::rename);
        }
        joinNew();
        if (!kept.equals(factions)) {
            FactionRules.FACTIONS.set(kept);
        }
        data.version(VERSION);
        FactionEdits.store();
    }

    private static String id(String line) {
        int bar = line.indexOf('|');
        return bar < 0 ? line : line.substring(0, bar);
    }

    // Today's members, but where a line put a kind in no faction or in one of a player's own; a kind today's lines
    // leave out stays in the faction that took over from its old one.
    private static List<String> members() {
        Map<String, String> members = new LinkedHashMap<>();
        for (String line : FactionDefaults.members()) {
            int is = line.indexOf('=');
            members.put(line.substring(0, is), line.substring(is + 1));
        }
        for (String line : FactionRules.MEMBERS.get()) {
            int is = line.indexOf('=');
            if (is <= 0) {
                continue;
            }
            String kind = line.substring(0, is);
            String faction = line.substring(is + 1);
            if (!RENAMED.containsKey(faction)) {
                members.put(kind, faction);
            } else if (!members.containsKey(kind)) {
                members.put(kind, RENAMED.get(faction));
            }
        }
        List<String> lines = new ArrayList<>();
        members.forEach((kind, faction) -> lines.add(kind + "=" + faction));
        return lines;
    }

    // Relations of a player's own factions (toward an old one: toward the one that took over), then today's where no
    // line of theirs says otherwise.
    private static List<String> relations() {
        List<String> lines = new ArrayList<>();
        Set<String> pairs = new HashSet<>();
        for (String line : FactionRules.RELATIONS.get()) {
            int arrow = line.indexOf('>');
            int is = line.indexOf('=');
            if (arrow <= 0 || is <= arrow) {
                continue;
            }
            String from = line.substring(0, arrow);
            String to = line.substring(arrow + 1, is);
            if (RENAMED.containsKey(from) && RENAMED.containsKey(to)) {
                continue;
            }
            String pair = RENAMED.getOrDefault(from, from) + ">" + RENAMED.getOrDefault(to, to);
            if (pairs.add(pair)) {
                lines.add(pair + line.substring(is));
            }
        }
        for (String line : FactionDefaults.relations()) {
            if (!pairs.contains(line.substring(0, line.indexOf('=')))) {
                lines.add(line);
            }
        }
        return lines;
    }

    // Kinds of creature added since the world's lists were written (no line of their own yet) join the faction
    // today's lines give them; a kind a player put somewhere keeps its line.
    private static void joinNew() {
        List<String> members = new ArrayList<>(FactionRules.MEMBERS.get());
        Set<String> listed = new HashSet<>();
        for (String line : members) {
            int is = line.indexOf('=');
            listed.add(is < 0 ? line : line.substring(0, is));
        }
        boolean added = false;
        for (String line : FactionDefaults.members()) {
            if (listed.add(line.substring(0, line.indexOf('=')))) {
                members.add(line);
                added = true;
            }
        }
        if (added) {
            FactionRules.MEMBERS.set(members);
        }
    }

    // Every player of a faction that was renamed into the team of the one that took over.
    private static void teams(ServerScoreboard scoreboard) {
        RENAMED.forEach((from, to) -> {
            PlayerTeam old = scoreboard.getPlayerTeam(from);
            if (old == null) {
                return;
            }
            PlayerTeam team = scoreboard.getPlayerTeam(to);
            if (team == null) {
                team = scoreboard.addPlayerTeam(to);
                team.setAllowFriendlyFire(false);
                team.setSeeFriendlyInvisibles(true);
            }
            for (String player : List.copyOf(old.getPlayers())) {
                scoreboard.addPlayerToTeam(player, team);
            }
            scoreboard.removePlayerTeam(old);
        });
    }
}
