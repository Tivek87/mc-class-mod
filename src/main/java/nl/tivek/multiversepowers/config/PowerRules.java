package nl.tivek.multiversepowers.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PowerRules {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DAMAGE;
    public static final ModConfigSpec.DoubleValue COOLDOWNS;
    public static final ModConfigSpec.IntValue BREAK_BLOCKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Rules for every power in this world, whatever character uses it.",
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
        builder.pop();
        SPEC = builder.build();
    }

    private PowerRules() {
    }

    public static double damage() {
        return SPEC.isLoaded() ? DAMAGE.get() : DAMAGE.getDefault();
    }

    public static double cooldowns() {
        return SPEC.isLoaded() ? COOLDOWNS.get() : COOLDOWNS.getDefault();
    }

    public static boolean breakBlocks() {
        return (SPEC.isLoaded() ? BREAK_BLOCKS.get() : BREAK_BLOCKS.getDefault()) != 0;
    }
}
