package nl.tivek.multiversepowers.faction.mob;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettings;
import nl.tivek.multiversepowers.faction.Standing;

// How one kind of creature stands to another in this world, where the host set it apart from the game's own: a world
// setting, one line per pair, "<creature>><target>=friendly|neutral|hostile". The player is the target minecraft:player;
// the target * is every creature but its own kind (never the player), and *>* every creature toward every other. A pair's
// own line goes before its creature's *, and that before *>*. Beside it the creatures that never spawn by themselves
// here and each kind's own numbers (`MobStats`). The last 25 changes can be taken back, one at a time.
public final class MobRules {
    public static final String FILE = ModConfigs.file("mobs");
    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.ConfigValue<List<? extends String>> RELATIONS;
    static final ModConfigSpec.ConfigValue<List<? extends String>> NO_SPAWN;
    static final ModConfigSpec.ConfigValue<List<? extends String>> STATS;
    public static final String ANY = "*";
    private static final String EVERYONE = ANY + ">" + ANY + "=";
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern LINE = Pattern.compile(
            "([a-z0-9_.-]+:[a-z0-9_./-]+|\\*)>([a-z0-9_.-]+:[a-z0-9_./-]+|\\*)=(friendly|neutral|hostile)");
    private static final int MOST = 20000;
    private static final int UNDO = 25;
    // Changes to the same number this close together (ticks) are one step back.
    private static final int SAME_STEP = 60;

    private record Rules(Map<EntityType<?>, Map<EntityType<?>, Standing>> pairs, Map<EntityType<?>, Standing> any,
            @Nullable Standing everyone) {
    }

    // The Mobs page as it was before a change, and what changed (one number's key, else empty) when.
    private record Step(List<String> relations, List<String> noSpawn, List<String> stats, String key, int at) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    @Nullable
    private static List<? extends String> parsedFrom;
    private static Rules rules = new Rules(Map.of(), Map.of(), null);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "How creatures stand to each other and to players in this world.",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "Easiest changed in the game: settings window > Mobs.").push("mobs");
        RELATIONS = builder.comment("Only where it differs from the game's own, one line per pair:"
                + " \"<creature>><target>=friendly|neutral|hostile\", for example"
                + " \"minecraft:iron_golem>minecraft:player=hostile\". Friendly never attacks, neutral only hits back,"
                + " hostile attacks on sight.")
                .defineListAllowEmpty("relations", List.of(), () -> "minecraft:zombie>minecraft:cow=hostile",
                        MobRules::valid);
        NO_SPAWN = builder.comment("Creatures that never spawn by themselves in this world (as the world spawns"
                + " creatures, in new land, in structures, on patrols): spawn eggs, spawners and commands still make"
                + " them. For example \"minecraft:phantom\".")
                .defineListAllowEmpty("noSpawn", List.of(), () -> "minecraft:phantom", MobRules::validId);
        STATS = builder.comment("Each kind's own numbers, only where they differ from the game's own, one line each:"
                + " \"<creature>|<number>=<value>\", for example \"minecraft:zombie|health=2\". Numbers: health, damage,"
                + " speed, follow, size, jump, gravity and xp (times the creature's own), armor, toughness, steady"
                + " (knockback resistance, 0 to 1) and push (its blows' knockback) added to it, regen (health a second),"
                + " drops (1 or 0) and spawnChance (0 to 100, of spawning by itself).")
                .defineListAllowEmpty("stats", List.of(), () -> "minecraft:zombie|health=2", MobStats::valid);
        builder.pop();
        SPEC = builder.build();
    }

    private MobRules() {
    }

    // What this world sets `actor` to be toward `target`, or null where it keeps the game's own.
    @Nullable
    public static Standing set(EntityType<?> actor, EntityType<?> target) {
        Rules now = rules();
        Map<EntityType<?>, Standing> row = now.pairs().get(actor);
        Standing own = row == null ? null : row.get(target);
        return own != null ? own : wide(now, actor, target);
    }

    // What `actor`'s * or *>* sets toward `target`, past a line of the pair's own.
    @Nullable
    private static Standing wide(Rules now, EntityType<?> actor, EntityType<?> target) {
        if (target == EntityType.PLAYER || target == actor || actor == EntityType.PLAYER) {
            return null;
        }
        Standing any = now.any().get(actor);
        return any != null ? any : now.everyone();
    }

    // What `actor`'s * line sets, or null.
    @Nullable
    public static Standing any(EntityType<?> actor) {
        return rules().any().get(actor);
    }

    public static boolean everyoneHostile() {
        return rules().everyone() == Standing.HOSTILE;
    }

    // Whether `actor` is set hostile toward anything, so it hunts.
    public static boolean hunts(EntityType<?> actor) {
        Rules now = rules();
        Map<EntityType<?>, Standing> row = now.pairs().get(actor);
        Standing any = now.any().getOrDefault(actor, now.everyone());
        return row != null && row.containsValue(Standing.HOSTILE) || any == Standing.HOSTILE;
    }

    private static synchronized Rules rules() {
        if (!SPEC.isLoaded()) {
            return new Rules(Map.of(), Map.of(), null);
        }
        List<? extends String> lines = RELATIONS.get();
        if (lines != parsedFrom) {
            parsedFrom = lines;
            rules = parse(lines);
        }
        return rules;
    }

    private static Rules parse(List<? extends String> lines) {
        Map<EntityType<?>, Map<EntityType<?>, Standing>> pairs = new HashMap<>();
        Map<EntityType<?>, Standing> any = new HashMap<>();
        Standing everyone = null;
        for (String line : lines) {
            int arrow = line.indexOf('>');
            int is = line.indexOf('=');
            if (arrow < 0 || is < arrow) {
                continue;
            }
            String actorId = line.substring(0, arrow);
            String targetId = line.substring(arrow + 1, is);
            Standing standing = standing(line.substring(is + 1));
            if (standing == null) {
                continue;
            }
            if (actorId.equals(ANY)) {
                everyone = targetId.equals(ANY) ? standing : everyone;
                continue;
            }
            EntityType<?> actor = type(actorId);
            // A creature of a mod no longer there keeps its line, but sets nothing.
            if (actor == null) {
                continue;
            }
            if (targetId.equals(ANY)) {
                any.put(actor, standing);
                continue;
            }
            EntityType<?> target = type(targetId);
            if (target != null) {
                pairs.computeIfAbsent(actor, key -> new HashMap<>()).put(target, standing);
            }
        }
        return new Rules(pairs, any, everyone);
    }

    @Nullable
    public static EntityType<?> type(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
    }

    public static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static String name(Standing standing) {
        return standing.name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static Standing standing(String name) {
        for (Standing standing : Standing.values()) {
            if (standing.name().toLowerCase(Locale.ROOT).equals(name)) {
                return standing;
            }
        }
        return null;
    }

    // Whether `type` spawns by itself in this world.
    public static boolean spawns(EntityType<?> type) {
        return !SPEC.isLoaded() || !NO_SPAWN.get().contains(id(type));
    }

    private static boolean validId(Object line) {
        return line instanceof String text && text.length() <= 200 && ID.matcher(text).matches();
    }

    private static boolean valid(Object line) {
        return line instanceof String text && text.length() <= 300 && LINE.matcher(text).matches();
    }

    private static boolean allowed(ServerPlayer player) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return false;
        }
        return SPEC.isLoaded();
    }

    // A host's change from the settings window: `standing` -1 puts the pair back to the game's own, and an empty
    // `target` puts every pair of `actor` back, its * too.
    public static void edit(ServerPlayer player, String actorId, String targetId, int standing) {
        EntityType<?> actor = type(actorId);
        EntityType<?> target = targetId.isEmpty() ? null : type(targetId);
        if (!allowed(player) || actor == null || !targetId.isEmpty() && target == null
                || standing >= Standing.values().length) {
            return;
        }
        List<String> lines = new ArrayList<>(RELATIONS.get());
        if (target == null) {
            lines.removeIf(line -> line.startsWith(id(actor) + ">"));
        } else {
            pair(lines, player.serverLevel(), actor, target, standing < 0 ? null : Standing.values()[standing]);
        }
        commit(player, lines);
    }

    // Many at once from the Mobs page: each of `actorIds` toward each of `targetIds` (* for every creature, its own
    // kind and players left out), never a creature toward its own kind.
    public static void bulk(ServerPlayer player, List<String> actorIds, List<String> targetIds, int standing) {
        if (!allowed(player) || standing < 0 || standing >= Standing.values().length) {
            return;
        }
        Standing chosen = Standing.values()[standing];
        List<String> lines = new ArrayList<>(RELATIONS.get());
        for (String actorId : actorIds) {
            EntityType<?> actor = type(actorId);
            if (actor == null || actor == EntityType.PLAYER) {
                continue;
            }
            for (String targetId : targetIds) {
                if (targetId.equals(ANY)) {
                    String own = id(actor) + ">";
                    lines.removeIf(line -> line.startsWith(own) && !line.startsWith(own + id(EntityType.PLAYER) + "="));
                    lines.add(own + ANY + "=" + name(chosen));
                    continue;
                }
                EntityType<?> target = type(targetId);
                if (target != null && target != actor) {
                    pair(lines, player.serverLevel(), actor, target, chosen);
                }
            }
        }
        commit(player, lines);
    }

    // Every creature hostile toward every other kind, or back as it was set: switched on it takes every line between
    // creatures away (a step back brings them again); toward players nothing changes.
    public static void everyone(ServerPlayer player) {
        if (!allowed(player)) {
            return;
        }
        boolean on = everyoneHostile();
        String toPlayer = ">" + id(EntityType.PLAYER) + "=";
        List<String> lines = new ArrayList<>();
        for (String line : RELATIONS.get()) {
            if (!line.startsWith(EVERYONE) && (on || line.contains(toPlayer))) {
                lines.add(line);
            }
        }
        if (!on) {
            lines.add(EVERYONE + name(Standing.HOSTILE));
        }
        commit(player, lines);
    }

    // The whole Mobs page back to the game's own: relations, spawning and numbers.
    public static void reset(ServerPlayer player) {
        if (!allowed(player) || RELATIONS.get().isEmpty() && NO_SPAWN.get().isEmpty() && STATS.get().isEmpty()) {
            return;
        }
        remember(player, "");
        RELATIONS.set(List.of());
        NO_SPAWN.set(List.of());
        STATS.set(List.of());
        saved(player.server);
        MobStats.applyAll(player.server, null);
    }

    // The last change taken back.
    public static void undo(ServerPlayer player) {
        if (!allowed(player)) {
            return;
        }
        Step step = STEPS.pollLast();
        if (step == null) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".mobs.nothing"),
                    true);
            return;
        }
        RELATIONS.set(step.relations());
        NO_SPAWN.set(step.noSpawn());
        STATS.set(step.stats());
        saved(player.server);
        MobStats.applyAll(player.server, null);
    }

    public static int steps() {
        return STEPS.size();
    }

    // Changes whenever the page's lists do (a copy from the server came in): what was just sent shows them no longer.
    public static int stamp() {
        return SPEC.isLoaded() ? 31 * (31 * System.identityHashCode(RELATIONS.get())
                + System.identityHashCode(NO_SPAWN.get())) + System.identityHashCode(STATS.get()) : 0;
    }

    public static void forget() {
        STEPS.clear();
    }

    // The pair's line in `lines` set to `chosen` (null: the game's own): none where that is what applies anyway.
    private static void pair(List<String> lines, ServerLevel level, EntityType<?> actor, EntityType<?> target,
            @Nullable Standing chosen) {
        String prefix = id(actor) + ">" + id(target) + "=";
        lines.removeIf(line -> line.startsWith(prefix));
        Standing wide = wide(rules(), actor, target);
        Standing without = wide != null ? wide : MobDefaults.of(level, actor, target);
        Standing wanted = chosen != null ? chosen : MobDefaults.of(level, actor, target);
        if (wanted != without) {
            lines.add(prefix + name(wanted));
        }
    }

    private static void commit(ServerPlayer player, List<String> lines) {
        if (lines.size() > MOST || lines.equals(RELATIONS.get())) {
            return;
        }
        remember(player, "");
        RELATIONS.set(lines);
        saved(player.server);
    }

    // The page as it is, kept to step back to before a change; a number changed again soon after keeps the first.
    static void remember(ServerPlayer player, String key) {
        int now = player.server.getTickCount();
        Step last = STEPS.peekLast();
        if (!key.isEmpty() && last != null && last.key().equals(key) && now - last.at() < SAME_STEP) {
            STEPS.pollLast();
            STEPS.addLast(new Step(last.relations(), last.noSpawn(), last.stats(), key, now));
            return;
        }
        STEPS.addLast(new Step(List.copyOf(RELATIONS.get()), List.copyOf(NO_SPAWN.get()), List.copyOf(STATS.get()),
                key, now));
        while (STEPS.size() > UNDO) {
            STEPS.pollFirst();
        }
    }

    // Stored and sent to everyone, with how many steps back there are.
    static void saved(MinecraftServer server) {
        WorldSettings.store(FILE);
        PacketDistributor.sendToAllPlayers(new MobStepsPayload(STEPS.size()));
    }

    // A host's change from the settings window: whether `creatureId` spawns by itself in this world.
    public static void editSpawn(ServerPlayer player, String creatureId, boolean spawns) {
        EntityType<?> type = type(creatureId);
        if (!allowed(player) || type == null || spawns(type) == spawns) {
            return;
        }
        String id = id(type);
        List<String> lines = new ArrayList<>();
        for (String line : NO_SPAWN.get()) {
            if (!line.equals(id)) {
                lines.add(line);
            }
        }
        if (!spawns) {
            if (lines.size() >= MOST) {
                return;
            }
            lines.add(id);
        }
        remember(player, "");
        NO_SPAWN.set(lines);
        saved(player.server);
    }
}
