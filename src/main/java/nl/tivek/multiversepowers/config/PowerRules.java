package nl.tivek.multiversepowers.config;

import com.mojang.authlib.GameProfile;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerRules {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DAMAGE;
    public static final ModConfigSpec.DoubleValue COOLDOWNS;
    public static final ModConfigSpec.IntValue BREAK_BLOCKS;
    public static final ModConfigSpec.DoubleValue POWER_COST;
    public static final ModConfigSpec.IntValue HURT_PLAYERS;
    public static final ModConfigSpec.DoubleValue EFFECT_RANGE;
    public static final ModConfigSpec.DoubleValue KNOCKDOWN;
    public static final ModConfigSpec.IntValue SPELLS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> OWNERS;
    // The characters' ids (GameCharacter's own, which this file cannot load before its settings exist).
    public static final List<String> CHARACTER_IDS = List.of("green_lantern", "doc_ock", "thor");
    private static final Map<String, ModConfigSpec.IntValue> CHARACTERS = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "Rules for every power in this world, whatever character uses it.",
                "World settings: every world keeps its own copy of this file, in <world>/serverconfig/welcomescreen/.",
                "The same numbers can be changed in the game: Mods > this mod > Config > Server.").push("powers");
        DAMAGE = builder.comment("Multiplies every damage number of every ability (1 = as set per ability, 0 = no"
                + " damage)")
                .defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        COOLDOWNS = builder.comment("Multiplies the cooldown of every ability (1 = as set per ability, 0 = no"
                + " cooldowns)")
                .defineInRange("cooldownMultiplier", 1.0, 0.0, 10.0);
        BREAK_BLOCKS = builder.comment("Whether powers may break blocks (1 = yes, 0 = never)")
                .defineInRange("breakBlocks", 1, 0, 1);
        POWER_COST = builder.comment("Multiplies what every power costs of the ring's power (1 = as set per ability,"
                + " 0 = free; a full ring's flight lasts as much shorter)")
                .defineInRange("powerCostMultiplier", 1.0, 0.0, 10.0);
        HURT_PLAYERS = builder.comment("Whether powers may hurt other players, where the server allows PvP at all"
                + " (1 = yes, 0 = never)")
                .defineInRange("hurtPlayers", 1, 0, 1);
        EFFECT_RANGE = builder.comment("Multiplies how far away players still see the powers' particles (1 = 128"
                + " blocks for big effects, 32 for small ones)")
                .defineInRange("effectRangeMultiplier", 1.0, 0.25, 3.0);
        KNOCKDOWN = builder.comment("How long a creature thrown by a power stays down after it lands before it moves"
                + " again, in seconds (0 = it gets up at once)")
                .defineInRange("knockdownSeconds", 3.75, 0.0, 15.0);
        builder.pop();
        builder.comment("Which characters and spells can be chosen in this world. One switched off is taken away from"
                + " whoever is it.").push("characters");
        for (String id : CHARACTER_IDS) {
            CHARACTERS.put(id, builder.comment("Whether " + id + " can be chosen (1 = yes, 0 = no)")
                    .defineInRange(id, 1, 0, 1));
        }
        SPELLS = builder.comment("Whether spells can be cast (1 = yes, 0 = no)").defineInRange("spells", 1, 0, 1);
        builder.pop();
        builder.comment("Who may change the world settings in the game (Mods > this mod > Config > Server).",
                "In singleplayer and on a LAN world the host always may; operators may not unless they are listed.")
                .push("access");
        OWNERS = builder.comment("The owners of this world or server by player name (or UUID), for example"
                + " [\"Steve\", \"Alex\"]; only they may change these settings in the game. Only this file can change"
                + " the list.")
                .defineListAllowEmpty("owners", List.of(), () -> "", PowerRules::validOwner);
        builder.pop();
        SPEC = builder.build();
    }

    private PowerRules() {
    }

    public static double damage() {
        return SPEC.isLoaded() ? DAMAGE.get() : DAMAGE.getDefault();
    }

    private static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    private static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static double powerCost() {
        return get(POWER_COST);
    }

    public static boolean hurtPlayers() {
        return get(HURT_PLAYERS) != 0;
    }

    public static double effectRange() {
        return get(EFFECT_RANGE);
    }

    public static int knockdownTicks() {
        return (int) Math.round(get(KNOCKDOWN) * 20.0);
    }

    public static boolean spells() {
        return get(SPELLS) != 0;
    }

    public static boolean character(String id) {
        ModConfigSpec.IntValue value = CHARACTERS.get(id);
        return value == null || get(value) != 0;
    }

    public static Map<String, ModConfigSpec.IntValue> characters() {
        return Collections.unmodifiableMap(CHARACTERS);
    }

    public static double cooldowns() {
        return SPEC.isLoaded() ? COOLDOWNS.get() : COOLDOWNS.getDefault();
    }

    public static boolean breakBlocks() {
        return (SPEC.isLoaded() ? BREAK_BLOCKS.get() : BREAK_BLOCKS.getDefault()) != 0;
    }

    // Whether the owners list names this player, by name (any case) or by UUID.
    public static boolean isOwner(GameProfile profile) {
        if (!SPEC.isLoaded() || profile == null) {
            return false;
        }
        String id = profile.getId() == null ? "" : profile.getId().toString();
        for (String owner : OWNERS.get()) {
            String name = owner.trim();
            if (!name.isEmpty() && (name.equalsIgnoreCase(profile.getName()) || name.equalsIgnoreCase(id))) {
                return true;
            }
        }
        return false;
    }

    private static boolean validOwner(Object owner) {
        return owner instanceof String name && name.length() <= 36;
    }
}
