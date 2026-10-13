package nl.tivek.multiversepowers.faction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.mob.MobDefaults;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.net.ReputationNotePayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules.Rule;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// What a player's deeds do to how factions see them. Striking or killing a member that was no danger to you (not
// hostile to you, not come for you) costs reputation with its faction, and a share of it with the factions friendly
// to that one; killing a foe of a faction, or what hunts its members, earns some with it; a trade, a tamed or a bred
// animal earns a little. Every day in the game it
// drifts back toward where it started. A member whose own faction turns hostile to them is put out of it.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Reputation {
    private static final String KEY = "faction." + MultiversePowers.MODID + ".";
    private static final int HIT_EVERY = 40;
    private static final int DAY = 24000;
    private static final int REMEMBERED = 2048;
    // The player who struck a creature before it had come for them, by creature.
    private static final Map<UUID, UUID> WRONGED = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, UUID> eldest) {
            return this.size() > REMEMBERED;
        }
    };
    // When a creature's last counted blow fell, by creature.
    private static final Map<UUID, Long> STRUCK = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
            return this.size() > REMEMBERED;
        }
    };
    // The factions each kind of creature hunts a member of, until the settings change.
    private static final Map<EntityType<?>, Set<String>> HUNTED = new HashMap<>();
    private static int huntedStamp;

    public static final FactionStands.Source SOURCE = new FactionStands.Source() {
        @Override
        public int earned(Player player, String faction) {
            MinecraftServer server = player.getServer();
            return server == null ? 0 : FactionData.get(server).earned(player.getUUID(), faction);
        }

        @Override
        @Nullable
        public Standing pin(Player player, String faction) {
            MinecraftServer server = player.getServer();
            return server == null ? null : FactionData.get(server).pin(player.getUUID(), faction);
        }
    };

    private Reputation() {
    }

    // A player's reputation with `faction` as its scale shows it.
    public static int of(ServerPlayer player, Faction faction) {
        return faction.reputation(FactionRules.reputation() ? SOURCE.earned(player, faction.id()) : 0);
    }

    // How `faction` stands to `player` now; null where each of its creatures does as the game has it.
    @Nullable
    public static Standing toward(Faction faction, ServerPlayer player) {
        return FactionStands.toward(faction, player, SOURCE);
    }

    // The player's reputation with `faction` set to `value` on its scale.
    public static void set(ServerPlayer player, Faction faction, int value) {
        int now = SOURCE.earned(player, faction.id());
        change(player, faction, faction.clamp(value - faction.start()) - now, true);
    }

    public static void add(ServerPlayer player, Faction faction, int delta) {
        change(player, faction, delta, true);
    }

    private static void earn(ServerPlayer player, @Nullable Faction faction, int delta) {
        if (faction != null && faction.reputation() && FactionRules.reputation() && !player.isCreative()
                && !player.isSpectator()) {
            change(player, faction, delta, false);
        }
    }

    private static void change(ServerPlayer player, Faction faction, int delta, boolean forced) {
        FactionData data = FactionData.get(player.server);
        int before = data.earned(player.getUUID(), faction.id());
        int after = faction.clamp(before + delta);
        if (after == before) {
            return;
        }
        Standing was = toward(faction, player);
        data.earned(player.getUUID(), faction.id(), after);
        Standing now = toward(faction, player);
        boolean turned = was != now;
        PacketDistributor.sendToPlayer(player, new ReputationNotePayload(faction.id(), after - before,
                !turned ? -1 : now == null ? Standing.values().length : now.ordinal()));
        if (turned) {
            player.sendSystemMessage(Component.translatable(KEY + "turned." + (now == null ? "game"
                    : now.name().toLowerCase(Locale.ROOT)), name(faction)));
        }
        PlayerTeam team = player.getTeam();
        if (!forced && team != null && team.getName().equals(faction.id()) && faction.zone(after) == Standing.HOSTILE) {
            player.server.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
            player.sendSystemMessage(Component.translatable(KEY + "expelled", name(faction)));
        }
        Factions.told(player);
    }

    public static Component name(Faction faction) {
        return Component.literal(faction.name()).withColor(faction.color());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof Mob victim
                && victim.getTarget() != attacker && !WRONGED.containsKey(victim.getUUID())
                && !Factions.hostileTo(victim, attacker)) {
            // Creatures that left the world unseen are never taken out one by one.
            if (WRONGED.size() > 4096) {
                WRONGED.clear();
                STRUCK.clear();
            }
            WRONGED.put(victim.getUUID(), attacker.getUUID());
        }
    }

    private static boolean wronged(LivingEntity victim, ServerPlayer by) {
        return by.getUUID().equals(WRONGED.get(victim.getUUID()));
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || !(victim instanceof Mob)
                || event.getNewDamage() <= 0.0F || victim.isDeadOrDying() || !wronged(victim, attacker)) {
            return;
        }
        long now = attacker.server.overworld().getGameTime();
        Long last = STRUCK.get(victim.getUUID());
        if (last != null && now - last < HIT_EVERY) {
            return;
        }
        STRUCK.put(victim.getUUID(), now);
        lose(attacker, MobFactions.of(victim), Rule.HIT_LOSS.get());
    }

    // The loss with `faction`, and its share with every faction friendly to it.
    private static void lose(ServerPlayer player, @Nullable Faction faction, int loss) {
        if (faction == null || loss <= 0) {
            return;
        }
        earn(player, faction, -loss);
        int share = loss * Rule.ALLY_SHARE.get() / 100;
        if (share <= 0) {
            return;
        }
        for (Faction other : FactionRules.all()) {
            if (other != faction && FactionRules.relation(other.id(), faction.id()) == Standing.FRIENDLY) {
                earn(player, other, -share);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || victim == killer
                || !(victim.level() instanceof ServerLevel level)) {
            WRONGED.remove(victim.getUUID());
            STRUCK.remove(victim.getUUID());
            return;
        }
        Faction own = victim instanceof Player player ? Factions.of(player)
                : victim instanceof Mob ? MobFactions.of(victim) : null;
        if (own != null && (victim instanceof Player || wronged(victim, killer))) {
            lose(killer, own, Rule.KILL_LOSS.get());
        }
        WRONGED.remove(victim.getUUID());
        STRUCK.remove(victim.getUUID());
        int gain = Rule.ENEMY_GAIN.get();
        if (gain <= 0 || !FactionRules.reputation()) {
            return;
        }
        Set<String> hunted = victim instanceof Mob mob && MobFactions.free(mob) ? hunted(level, mob.getType())
                : Set.of();
        for (Faction faction : FactionRules.all()) {
            if (faction == own || own != null && FactionRules.relation(faction.id(), own.id()) == Standing.FRIENDLY) {
                continue;
            }
            boolean foe = own != null && (FactionRules.relation(faction.id(), own.id()) == Standing.HOSTILE
                    || FactionRules.relation(own.id(), faction.id()) == Standing.HOSTILE);
            if (foe || hunted.contains(faction.id())) {
                earn(killer, faction, gain);
            }
        }
    }

    // The factions with a member that `kind` attacks on sight, by the Mobs page or else the game's own.
    private static Set<String> hunted(ServerLevel level, EntityType<?> kind) {
        int stamp = 31 * FactionRules.stamp() + MobRules.stamp();
        if (stamp != huntedStamp) {
            huntedStamp = stamp;
            HUNTED.clear();
        }
        return HUNTED.computeIfAbsent(kind, key -> {
            Set<String> factions = new HashSet<>();
            for (EntityType<?> prey : FactionRules.kinds()) {
                Faction faction = prey == key ? null : FactionRules.of(prey);
                if (faction == null || factions.contains(faction.id())) {
                    continue;
                }
                Standing set = MobRules.set(key, prey);
                if ((set != null ? set : MobDefaults.of(level, key, prey)) == Standing.HOSTILE) {
                    factions.add(faction.id());
                }
            }
            return factions;
        });
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            earn(player, MobFactions.of(event.getAbstractVillager()), Rule.DEED_GAIN.get());
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            earn(player, FactionRules.of(event.getAnimal().getType()), Rule.DEED_GAIN.get() * 2);
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            earn(player, MobFactions.of(event.getParentA()), Rule.DEED_GAIN.get());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 200 != 0) {
            return;
        }
        FactionData data = FactionData.get(server);
        long day = server.overworld().getDayTime() / DAY;
        if (day == data.day()) {
            return;
        }
        long days = data.day() < 0 || day < data.day() ? 0 : Math.min(30, day - data.day());
        data.day(day);
        int drift = (int) days * Rule.DAILY_DRIFT.get();
        if (drift <= 0) {
            return;
        }
        for (UUID player : data.players()) {
            for (Map.Entry<String, Integer> entry : Map.copyOf(data.earned(player)).entrySet()) {
                int now = entry.getValue();
                data.earned(player, entry.getKey(), now > 0 ? Math.max(0, now - drift) : Math.min(0, now + drift));
            }
        }
        Factions.changed(server);
    }

    public static void clear() {
        WRONGED.clear();
        STRUCK.clear();
        HUNTED.clear();
    }
}
