package nl.tivek.multiversepowers.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Factions {
    private static final long INVITE_TICKS = 6000L;
    private static final long RESEND_TICKS = 1200L;
    private static final Map<String, Map<UUID, Grudge>> GRUDGES = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private static final Standings.View VIEW = new Standings.View() {
        @Override
        public Standings.Relation relation(Player viewer, String team, String other) {
            MinecraftServer server = viewer.getServer();
            return server == null ? Standings.Relation.NONE : FactionData.get(server).relation(team, other);
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

    public static boolean friendly(ServerPlayer viewer, Entity entity) {
        return standing(viewer, entity) == Standing.FRIENDLY;
    }

    static String key(Player player) {
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
                && friendly(attacker, victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim
                || event.getNewDamage() <= 0.0F) {
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

    static void invite(ServerPlayer player, String team) {
        INVITES.put(player.getUUID(), new Invite(team, now(player.server) + INVITE_TICKS));
    }

    static boolean invited(ServerPlayer player, String team) {
        Invite invite = INVITES.get(player.getUUID());
        if (invite == null || !invite.team().equals(team) || invite.until() < now(player.server)) {
            return false;
        }
        INVITES.remove(player.getUUID());
        return true;
    }

    static void forget(String team) {
        GRUDGES.remove(team);
        INVITES.values().removeIf(invite -> invite.team().equals(team));
    }

    public static void sync(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    static void sync(ServerPlayer player) {
        FactionData data = FactionData.get(player.server);
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
        PacketDistributor.sendToPlayer(player, new StandingsPayload(List.copyOf(data.allies()),
                List.copyOf(data.enemies()), ids, until));
    }

    public static void clear() {
        GRUDGES.clear();
        INVITES.clear();
    }
}
