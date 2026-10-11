package nl.tivek.multiversepowers.faction.mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettings;
import nl.tivek.multiversepowers.faction.Standing;

// How one kind of creature stands to another in this world, where the host set it apart from the game's own: a world
// setting, one line per pair, "<creature>><target>=friendly|neutral|hostile". The player is the target minecraft:player.
public final class MobRules {
    public static final String FILE = ModConfigs.file("mobs");
    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.ConfigValue<List<? extends String>> RELATIONS;
    private static final Pattern LINE = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9_./-]+>[a-z0-9_.-]+:[a-z0-9_./-]+=(friendly|neutral|hostile)");
    private static final int MOST = 20000;

    @Nullable
    private static List<? extends String> parsedFrom;
    private static Map<EntityType<?>, Map<EntityType<?>, Standing>> rows = Map.of();

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
        builder.pop();
        SPEC = builder.build();
    }

    private MobRules() {
    }

    // What this world sets `actor` to be toward `target`, or null where it keeps the game's own.
    @Nullable
    public static Standing set(EntityType<?> actor, EntityType<?> target) {
        Map<EntityType<?>, Standing> row = rows().get(actor);
        return row == null ? null : row.get(target);
    }

    // Every pair this world sets for `actor`.
    public static Map<EntityType<?>, Standing> row(EntityType<?> actor) {
        return rows().getOrDefault(actor, Map.of());
    }

    // Whether `actor` is set hostile toward anything, so it hunts.
    public static boolean hunts(EntityType<?> actor) {
        return row(actor).containsValue(Standing.HOSTILE);
    }

    private static synchronized Map<EntityType<?>, Map<EntityType<?>, Standing>> rows() {
        if (!SPEC.isLoaded()) {
            return Map.of();
        }
        List<? extends String> lines = RELATIONS.get();
        if (lines != parsedFrom) {
            parsedFrom = lines;
            rows = parse(lines);
        }
        return rows;
    }

    private static Map<EntityType<?>, Map<EntityType<?>, Standing>> parse(List<? extends String> lines) {
        Map<EntityType<?>, Map<EntityType<?>, Standing>> parsed = new HashMap<>();
        for (String line : lines) {
            int arrow = line.indexOf('>');
            int is = line.indexOf('=');
            if (arrow < 0 || is < arrow) {
                continue;
            }
            EntityType<?> actor = type(line.substring(0, arrow));
            EntityType<?> target = type(line.substring(arrow + 1, is));
            Standing standing = standing(line.substring(is + 1));
            // A creature of a mod no longer there keeps its line, but sets nothing.
            if (actor != null && target != null && standing != null) {
                parsed.computeIfAbsent(actor, key -> new HashMap<>()).put(target, standing);
            }
        }
        return parsed;
    }

    @Nullable
    public static EntityType<?> type(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
    }

    public static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
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

    private static boolean valid(Object line) {
        return line instanceof String text && text.length() <= 300 && LINE.matcher(text).matches();
    }

    // A host's change from the settings window: `standing` -1 puts the pair back to the game's own, and an empty
    // `target` puts every pair of `actor` back. One set to what the game does anyway is no line of its own.
    public static void edit(ServerPlayer player, String actorId, String targetId, int standing) {
        if (!WorldSettings.mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        EntityType<?> actor = type(actorId);
        EntityType<?> target = targetId.isEmpty() ? null : type(targetId);
        if (actor == null || !targetId.isEmpty() && target == null || standing >= Standing.values().length
                || !SPEC.isLoaded()) {
            return;
        }
        String prefix = id(actor) + ">" + (target == null ? "" : id(target) + "=");
        List<String> lines = new ArrayList<>();
        for (String line : RELATIONS.get()) {
            if (!line.startsWith(prefix)) {
                lines.add(line);
            }
        }
        if (target != null && standing >= 0) {
            Standing chosen = Standing.values()[standing];
            if (chosen != MobDefaults.of(player.serverLevel(), actor, target)) {
                lines.add(prefix + chosen.name().toLowerCase(Locale.ROOT));
            }
        }
        if (lines.size() > MOST || lines.equals(RELATIONS.get())) {
            return;
        }
        RELATIONS.set(lines);
        WorldSettings.store(FILE);
    }
}
