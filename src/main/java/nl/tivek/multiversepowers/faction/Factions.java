package nl.tivek.multiversepowers.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.mob.MobDefaults;
import nl.tivek.multiversepowers.faction.net.FactionStatePayload;
import nl.tivek.multiversepowers.faction.net.StandingsPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Factions {
    private static final long INVITE_TICKS = 6000L;
    private static final long RESEND_TICKS = 1200L;
    private static final long HUNT_EVERY = 20L;
    private static final Map<String, Map<UUID, Grudge>> GRUDGES = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    // The factions with someone to attack on sight now: another faction, or a player they are hostile to.
    private static final Set<String> HUNTING = new HashSet<>();
    private static long huntingAt = -1L;

    private static final Standings.View VIEW = new Standings.View() {
        @Override
        @Nullable
        public Standing stand(Mob mob, Player viewer) {
            return FactionStands.of(mob, viewer, Reputation.SOURCE);
        }

        @Override
        public boolean grudge(Player viewer, UUID entity) {
            MinecraftServer server = viewer.getServer();
            return server != null && red(key(viewer), entity, now(server));
        }

        @Override
        public boolean angry(Mob mob, Player viewer) {
            return mob.getTarget() == viewer;
        }

        @Override
        @Nullable
        public Player player(Player viewer, UUID id) {
            MinecraftServer server = viewer.getServer();
            return server == null ? null : server.getPlayerList().getPlayer(id);
        }
    };

    private static final class Grudge {
        private int hits;
        private long last;
        private long sent;
        private boolean red;
    }

    private record Invite(String team, long until) {
    }

    private Factions() {
    }

    public static Standing standing(ServerPlayer viewer, Entity entity) {
        return Standings.of(viewer, entity, VIEW);
    }

    public static boolean hostile(ServerPlayer viewer, Entity entity) {
        return standing(viewer, entity) == Standing.HOSTILE;
    }

    // The faction a player is in: the one named as their team, if the world has it.
    @Nullable
    public static Faction of(Player player) {
        PlayerTeam team = player.getTeam();
        return team == null ? null : FactionRules.get(team.getName());
    }

    // Whether `mob` attacks `player` on sight: by its faction or the Mobs page, else by the game's own.
    public static boolean hostileTo(Mob mob, ServerPlayer player) {
        Standing stand = FactionStands.of(mob, player, Reputation.SOURCE);
        return (stand != null ? stand : MobDefaults.of(player.serverLevel(), mob.getType(), EntityType.PLAYER))
                == Standing.HOSTILE;
    }

    // Whether `mob`'s faction or its own behaviour gives it someone to attack on sight.
    public static boolean hunts(Mob mob) {
        if (MobFactions.stance(mob) == Standing.HOSTILE) {
            return true;
        }
        Faction faction = MobFactions.of(mob);
        MinecraftServer server = mob.getServer();
        if (faction == null || server == null) {
            return false;
        }
        long now = now(server);
        if (huntingAt < 0L || now - huntingAt >= HUNT_EVERY || now < huntingAt) {
            huntingAt = now;
            HUNTING.clear();
            for (Faction each : FactionRules.all()) {
                boolean hunts = FactionRules.table().hunts(each.id());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    hunts |= Reputation.toward(each, player) == Standing.HOSTILE;
                }
                if (hunts) {
                    HUNTING.add(each.id());
                }
            }
        }
        return HUNTING.contains(faction.id());
    }

    // Anyone may be hit, whatever their standing, but never by yourself and never a player of your own team.
    public static boolean mayHit(ServerPlayer attacker, Entity target) {
        return target != attacker && !sameTeam(attacker, target);
    }

    private static boolean sameTeam(Player player, Entity other) {
        PlayerTeam team = player.getTeam();
        return team != null && other instanceof Player them && them.getTeam() == team;
    }

    public static String key(Player player) {
        PlayerTeam team = player.getTeam();
        return team != null ? team.getName() : "@" + player.getUUID();
    }

    static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static boolean red(String key, UUID entity, long now) {
        Map<UUID, Grudge> held = GRUDGES.get(key);
        Grudge grudge = held == null ? null : held.get(entity);
        return grudge != null && grudge.red && now - grudge.last <= Standings.GRUDGE_TICKS;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim
                && sameTeam(attacker, victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        // A creature killed by the blow turns on no one (a player comes back, and keeps the grudge).
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim
                || event.getNewDamage() <= 0.0F || !(victim instanceof Player) && victim.isDeadOrDying()) {
            return;
        }
        long now = now(attacker.server);
        Map<UUID, Grudge> mine = GRUDGES.computeIfAbsent(key(attacker), k -> new HashMap<>());
        Grudge grudge = mine.get(victim.getUUID());
        if (grudge == null && standing(attacker, victim) != Standing.NEUTRAL) {
            return;
        }
        if (grudge == null || now - grudge.last > Standings.GRUDGE_TICKS) {
            grudge = new Grudge();
            mine.put(victim.getUUID(), grudge);
        }
        grudge.hits++;
        grudge.last = now;
        boolean turned = !grudge.red && grudge.hits >= Standings.GRUDGE_HITS;
        grudge.red |= turned;
        if (!grudge.red) {
            return;
        }
        boolean resend = turned || now - grudge.sent > RESEND_TICKS;
        grudge.sent = resend ? now : grudge.sent;
        if (victim instanceof ServerPlayer other) {
            Map<UUID, Grudge> theirs = GRUDGES.computeIfAbsent(key(other), k -> new HashMap<>());
            String side = key(attacker);
            for (ServerPlayer member : attacker.server.getPlayerList().getPlayers()) {
                if (!key(member).equals(side)) {
                    continue;
                }
                Grudge back = theirs.computeIfAbsent(member.getUUID(), id -> new Grudge());
                resend |= !back.red || now - back.sent > RESEND_TICKS;
                back.red = true;
                back.hits = Math.max(back.hits, Standings.GRUDGE_HITS);
                back.last = now;
                back.sent = resend ? now : back.sent;
            }
            if (turned) {
                other.displayClientMessage(Component.translatable("faction." + MultiversePowers.MODID + ".turned_on",
                        attacker.getDisplayName()), true);
            }
        }
        if (turned) {
            attacker.displayClientMessage(Component.translatable("faction." + MultiversePowers.MODID + ".turned",
                    victim.getDisplayName()), true);
        }
        if (resend) {
            sync(attacker.server);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) {
            return;
        }
        boolean held = false;
        for (Map<UUID, Grudge> grudges : GRUDGES.values()) {
            held |= grudges.remove(dead.getUUID()) != null;
        }
        if (held && dead.getServer() != null) {
            sync(dead.getServer());
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    public static void invite(ServerPlayer player, String team) {
        INVITES.put(player.getUUID(), new Invite(team, now(player.server) + INVITE_TICKS));
    }

    public static boolean invited(ServerPlayer player, String team) {
        Invite invite = INVITES.get(player.getUUID());
        if (invite == null || !invite.team().equals(team) || invite.until() < now(player.server)) {
            return false;
        }
        INVITES.remove(player.getUUID());
        return true;
    }

    public static void forget(MinecraftServer server, String team) {
        GRUDGES.remove(team);
        INVITES.values().removeIf(invite -> invite.team().equals(team));
        FactionData.get(server).forget(team);
    }

    // The game's own standings read before anyone plays, and the teams, allies and enemies of a world from before
    // factions had a settings file moved into it, once.
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        MobDefaults.table(server.overworld());
        FactionData data = FactionData.get(server);
        if (FactionRules.SPEC.isLoaded() && !data.adopted()) {
            for (PlayerTeam team : server.getScoreboard().getPlayerTeams()) {
                FactionEdits.adopt(team.getName());
            }
            for (String pair : data.allies()) {
                FactionEdits.adopt(pair, Standing.FRIENDLY);
            }
            for (String pair : data.enemies()) {
                FactionEdits.adopt(pair, Standing.HOSTILE);
            }
            data.adopt();
            FactionEdits.store();
        }
        FactionUpgrade.run(server, data);
    }

    // The factions or someone's place among them changed: who hunts whom is worked out again, and everyone told.
    public static void changed(MinecraftServer server) {
        huntingAt = -1L;
        sync(server);
    }

    static void told(ServerPlayer player) {
        huntingAt = -1L;
        sync(player);
    }

    public static void sync(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    static void sync(ServerPlayer player) {
        FactionData data = FactionData.get(player.server);
        Map<String, Integer> pins = new HashMap<>();
        data.pins(player.getUUID()).forEach((faction, standing) -> pins.put(faction, standing.ordinal()));
        PacketDistributor.sendToPlayer(player, new FactionStatePayload(new HashMap<>(data.earned(player.getUUID())),
                pins, FactionEdits.steps()));
        long now = now(player.server);
        List<UUID> ids = new ArrayList<>();
        List<Long> until = new ArrayList<>();
        Map<UUID, Grudge> held = GRUDGES.get(key(player));
        if (held != null) {
            held.values().removeIf(grudge -> now - grudge.last > Standings.GRUDGE_TICKS);
            for (Map.Entry<UUID, Grudge> entry : held.entrySet()) {
                if (entry.getValue().red) {
                    ids.add(entry.getKey());
                    until.add(entry.getValue().last + Standings.GRUDGE_TICKS);
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new StandingsPayload(ids, until));
    }

    public static void clear() {
        GRUDGES.clear();
        INVITES.clear();
        HUNTING.clear();
        huntingAt = -1L;
        Reputation.clear();
        FactionHelp.clear();
        FactionEdits.forget();
    }
}
