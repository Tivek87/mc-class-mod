package nl.tivek.multiversepowers.faction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.scores.PlayerTeam;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.WorldSettings;
import nl.tivek.multiversepowers.faction.net.FactionEditPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionDefaults;
import nl.tivek.multiversepowers.faction.rules.FactionRules.Rule;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// Every change to the world's factions, from the Factions page (`page`, which asks who may) or `/faction`: each
// writes the settings file, sends it to every player and can be taken back, the last 25 one at a time.
public final class FactionEdits {
    private static final String KEY = "faction." + MultiversePowers.MODID + ".";
    private static final int UNDO = 25;
    private static final double REACH = 64.0;

    private record Step(List<String> factions, List<String> members, List<String> relations) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();

    private FactionEdits() {
    }

    public static int steps() {
        return STEPS.size();
    }

    static void forget() {
        STEPS.clear();
    }

    private static List<String> factions() {
        return new ArrayList<>(FactionRules.FACTIONS.get());
    }

    private static List<String> members() {
        return new ArrayList<>(FactionRules.MEMBERS.get());
    }

    private static List<String> relations() {
        return new ArrayList<>(FactionRules.RELATIONS.get());
    }

    private static boolean ready() {
        return FactionRules.SPEC.isLoaded();
    }

    // The three lists as given (null: as it is), kept to step back to, stored and told to everyone.
    private static boolean commit(MinecraftServer server, @Nullable List<String> factions,
            @Nullable List<String> members, @Nullable List<String> relations) {
        boolean same = (factions == null || factions.equals(FactionRules.FACTIONS.get()))
                && (members == null || members.equals(FactionRules.MEMBERS.get()))
                && (relations == null || relations.equals(FactionRules.RELATIONS.get()));
        if (same || factions != null && !fits(factions) || members != null && !fits(members)
                || relations != null && !fits(relations)) {
            return false;
        }
        STEPS.addLast(new Step(factions(), members(), relations()));
        while (STEPS.size() > UNDO) {
            STEPS.pollFirst();
        }
        set(server, factions, members, relations);
        return true;
    }

    private static boolean fits(List<String> lines) {
        return lines.size() <= 4000;
    }

    private static void set(MinecraftServer server, @Nullable List<String> factions, @Nullable List<String> members,
            @Nullable List<String> relations) {
        if (factions != null) {
            FactionRules.FACTIONS.set(factions);
        }
        if (members != null) {
            FactionRules.MEMBERS.set(members);
        }
        if (relations != null) {
            FactionRules.RELATIONS.set(relations);
        }
        store();
        Factions.changed(server);
    }

    static void store() {
        WorldSettings.store(FactionRules.FILE);
    }

    // A new faction: neutral to players, its members at peace with each other. Null, or why not (a lang key).
    @Nullable
    public static String create(MinecraftServer server, String id, String name) {
        if (!ready() || !FactionTable.ID.matcher(id).matches() || id.equals(MobFactions.NONE)) {
            return "bad_name";
        }
        if (FactionRules.get(id) != null) {
            return "exists";
        }
        String shown = FactionTable.clean(name, FactionTable.NAME);
        List<String> lines = factions();
        lines.add(id + "|name=" + (shown.isEmpty() ? id : shown));
        lines.add(id + "|color=" + String.format("%06x", freeColor()));
        lines.add(id + "|attitude=" + FactionTable.name(Standing.NEUTRAL));
        return commit(server, lines, null, FactionTable.withRelation(relations(), id, id, Standing.FRIENDLY)) ? null
                : "bad_name";
    }

    private static int freeColor() {
        for (int color : FactionTable.COLORS) {
            if (FactionRules.all().stream().noneMatch(faction -> faction.color() == color)) {
                return color;
            }
        }
        return FactionTable.COLORS[FactionRules.all().size() % FactionTable.COLORS.length];
    }

    // The faction gone: its creatures back with their sort, its players out, no one's reputation with it kept.
    public static boolean delete(MinecraftServer server, String id) {
        if (!ready() || FactionRules.get(id) == null) {
            return false;
        }
        commit(server, FactionTable.withoutFaction(factions(), id), FactionTable.withoutMembers(members(), id),
                FactionTable.withoutRelations(relations(), id));
        PlayerTeam team = server.getScoreboard().getPlayerTeam(id);
        if (team != null) {
            server.getScoreboard().removePlayerTeam(team);
        }
        Factions.forget(server, id);
        Factions.changed(server);
        return true;
    }

    // One of a faction's words or numbers (`FactionTable.KEYS`); whether the value was one it takes.
    public static boolean word(MinecraftServer server, String id, String key, String value) {
        Faction faction = ready() ? FactionRules.get(id) : null;
        if (faction == null) {
            return false;
        }
        String kept;
        switch (key) {
            case "name" -> {
                kept = FactionTable.clean(value, FactionTable.NAME);
                if (kept.isEmpty()) {
                    return false;
                }
            }
            case "description", "lore" -> kept = FactionTable.clean(value, FactionTable.TEXT);
            case "color" -> {
                kept = value.replace("#", "").toLowerCase(Locale.ROOT);
                if (!kept.matches("[0-9a-f]{6}")) {
                    return false;
                }
            }
            case "attitude" -> {
                kept = value;
                if (!value.equals(FactionTable.GAME) && FactionTable.standing(value) == null) {
                    return false;
                }
            }
            case "reputation", "help", "open" -> {
                kept = value;
                if (!value.equals("true") && !value.equals("false")) {
                    return false;
                }
            }
            case "hostileAt", "friendlyAt" -> {
                int number;
                try {
                    number = Integer.parseInt(value.strip());
                } catch (NumberFormatException e) {
                    return false;
                }
                boolean low = key.equals("hostileAt");
                kept = String.valueOf(low ? Math.max(Faction.LOWEST, Math.min(-1, number))
                        : Math.max(1, Math.min(Faction.HIGHEST, number)));
            }
            default -> {
                return false;
            }
        }
        commit(server, FactionTable.withWord(factions(), id, key, kept), null, null);
        return true;
    }

    // Kinds of creature put in the faction `id` ("" for in none); how many of them were kinds a faction can hold.
    public static int members(MinecraftServer server, String id, List<String> kinds) {
        if (!ready() || !id.isEmpty() && FactionRules.get(id) == null) {
            return 0;
        }
        List<String> lines = members();
        int done = 0;
        for (String kind : kinds) {
            EntityType<?> type = FactionRules.type(kind);
            if (type != null && FactionRules.kinds().contains(type)) {
                lines = FactionTable.withMember(lines, FactionRules.id(type), id);
                done++;
            }
        }
        commit(server, null, lines, null);
        return done;
    }

    // How `from` stands toward `to` (itself: its members to each other); null for as the game has it.
    public static boolean relation(MinecraftServer server, String from, String to, @Nullable Standing standing) {
        if (!ready() || FactionRules.get(from) == null || FactionRules.get(to) == null) {
            return false;
        }
        commit(server, null, null, FactionTable.withRelation(relations(), from, to, standing));
        return true;
    }

    public static boolean rule(MinecraftServer server, Rule rule, int value) {
        if (!ready()) {
            return false;
        }
        int kept = Math.max(0, Math.min(rule.most, value));
        if (kept != rule.get()) {
            rule.value().set(kept);
            store();
            Factions.changed(server);
        }
        return true;
    }

    public static boolean undo(MinecraftServer server) {
        Step step = STEPS.pollLast();
        if (step == null || !ready()) {
            return false;
        }
        set(server, step.factions(), step.members(), step.relations());
        return true;
    }

    // Every faction, member and relation back to what a new world starts with.
    public static void reset(MinecraftServer server) {
        if (ready()) {
            commit(server, FactionDefaults.factions(), FactionDefaults.members(), FactionDefaults.relations());
        }
    }

    // A team from before factions had a settings file made a faction, where it is none yet. Stored by the caller.
    static void adopt(String id) {
        if (!FactionTable.ID.matcher(id).matches() || id.equals(MobFactions.NONE) || FactionRules.get(id) != null) {
            return;
        }
        List<String> factions = factions();
        factions.add(id + "|name=" + id);
        factions.add(id + "|attitude=" + FactionTable.name(Standing.NEUTRAL));
        FactionRules.FACTIONS.set(factions);
        FactionRules.RELATIONS.set(FactionTable.withRelation(relations(), id, id, Standing.FRIENDLY));
    }

    // A pair "a|b" of such teams: both made factions, each set to stand `standing` to the other.
    static void adopt(String pair, Standing standing) {
        int bar = pair.indexOf('|');
        if (bar <= 0) {
            return;
        }
        String one = pair.substring(0, bar);
        String other = pair.substring(bar + 1);
        adopt(one);
        adopt(other);
        if (FactionRules.get(one) != null && FactionRules.get(other) != null) {
            FactionRules.RELATIONS.set(FactionTable.withRelation(FactionTable.withRelation(relations(), one, other,
                    standing), other, one, standing));
        }
    }

    // The player into the faction's team, made if it is not there yet.
    public static void join(ServerPlayer player, Faction faction) {
        ServerScoreboard scoreboard = player.server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(faction.id());
        if (team == null) {
            team = scoreboard.addPlayerTeam(faction.id());
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        Factions.changed(player.server);
    }

    public static boolean leave(ServerPlayer player) {
        boolean left = player.server.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
        if (left) {
            Factions.changed(player.server);
        }
        return left;
    }

    // Something done on the Factions page: only the host, an owner or (where the world lets them) an operator changes
    // the factions; any player joins one that is open, or leaves theirs.
    public static void page(ServerPlayer player, FactionEditPayload payload) {
        MinecraftServer server = player.server;
        Faction faction = FactionRules.get(payload.faction());
        if (payload.action() == FactionEditPayload.LEAVE) {
            leave(player);
            return;
        }
        boolean may = WorldSettings.mayEdit(player);
        if (payload.action() == FactionEditPayload.JOIN) {
            if (faction != null && (may || faction.open()
                    && Reputation.toward(faction, player) != Standing.HOSTILE)) {
                join(player, faction);
                player.sendSystemMessage(Component.translatable(KEY + "joined", player.getDisplayName(),
                        Reputation.name(faction)));
            }
            return;
        }
        if (!may) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        switch (payload.action()) {
            case FactionEditPayload.CREATE -> create(server, payload.faction(), payload.key());
            case FactionEditPayload.DELETE -> delete(server, payload.faction());
            case FactionEditPayload.WORD -> word(server, payload.faction(), payload.key(), payload.value());
            case FactionEditPayload.ADD -> members(server, payload.faction(), payload.values());
            case FactionEditPayload.REMOVE -> members(server, "", payload.values());
            case FactionEditPayload.RELATION -> relation(server, payload.faction(), payload.key(),
                    FactionTable.standing(payload.value()));
            case FactionEditPayload.RULE -> {
                Rule rule = Rule.of(payload.key());
                try {
                    if (rule != null) {
                        rule(server, rule, Integer.parseInt(payload.value()));
                    }
                } catch (NumberFormatException ignored) {
                    // Not a number: nothing changes.
                }
            }
            case FactionEditPayload.UNDO -> undo(server);
            case FactionEditPayload.RESET -> reset(server);
            case FactionEditPayload.MOB_FACTION, FactionEditPayload.MOB_STANCE -> mob(player, payload);
            case FactionEditPayload.REPUTATION -> {
                try {
                    int number = Integer.parseInt(payload.value());
                    if (faction != null && payload.key().equals("set")) {
                        Reputation.set(player, faction, number);
                    } else if (faction != null) {
                        Reputation.add(player, faction, number);
                    }
                } catch (NumberFormatException ignored) {
                    // Not a number: nothing changes.
                }
            }
            default -> {
            }
        }
    }

    private static void mob(ServerPlayer player, FactionEditPayload payload) {
        int id;
        try {
            id = Integer.parseInt(payload.key());
        } catch (NumberFormatException e) {
            return;
        }
        if (!(player.serverLevel().getEntity(id) instanceof Mob mob) || mob.distanceToSqr(player) > REACH * REACH
                || !MobFactions.free(mob)) {
            return;
        }
        if (payload.action() == FactionEditPayload.MOB_STANCE) {
            MobFactions.stance(mob, FactionTable.standing(payload.value()));
        } else if (payload.faction().isEmpty() || payload.faction().equals(MobFactions.NONE)
                || FactionRules.get(payload.faction()) != null) {
            MobFactions.faction(mob, payload.faction());
        }
    }
}
