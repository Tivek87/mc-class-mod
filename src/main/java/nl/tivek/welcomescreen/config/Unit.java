package nl.tivek.welcomescreen.config;

import java.math.BigDecimal;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * What a number in a settings file counts, so the settings screen can say what it means in plain words: 12 half
 * hearts is "6 hearts", 80 ticks is "4 s", 0.3 of a hit that gets through is "blocks 70%".
 */
public enum Unit {
    /** Damage, in half hearts. */
    HALF_HEARTS,
    /** Extra damage in half hearts for every block per tick of speed. */
    HALF_HEARTS_PER_SPEED,
    /** Time in ticks (20 ticks = 1 second). */
    TICKS,
    /** Time in seconds. */
    SECONDS,
    /** A distance in blocks. */
    BLOCKS,
    /** A speed in blocks per tick. */
    BLOCKS_PER_TICK,
    /** A speed in blocks per second. */
    BLOCKS_PER_SECOND,
    /** Ring power (a full ring holds 100). */
    POWER,
    /** Ring power a second. */
    POWER_PER_SECOND,
    /** Seconds a full ring lasts while it drains: shown as what it costs a second. */
    RING_SECONDS,
    /** The part of a hit that still gets through (0.3 = 30%): shown as how much is stopped. */
    PART_KEPT,
    /** Stamina. */
    STAMINA,
    /** Stamina a tick. */
    STAMINA_PER_TICK,
    /** How hard a block may be (dirt 0.5, stone 1.5, wood 2, ores 3, iron 5): shown as the hardest that breaks. */
    HARDNESS,
    /** A number of blocks. */
    BLOCK_COUNT,
    /** A strength without a unit of its own (a push, a throw): shown as a factor. */
    STRENGTH;

    private static final String PREFIX = "config." + WelcomeScreenMod.MODID + ".unit.";

    /** What {@code value} means, in plain words. */
    public Component describe(double value) {
        return switch (this) {
            case HALF_HEARTS -> value <= 0.0 ? key("no_damage") : key("hearts", number(value / 2.0));
            case HALF_HEARTS_PER_SPEED -> key("hearts_per_speed", number(value / 2.0));
            case TICKS -> value <= 0.0 ? key("instant") : key("seconds", number(value / 20.0));
            case SECONDS -> key("seconds", number(value));
            case BLOCKS -> key("blocks", number(value));
            case BLOCKS_PER_TICK -> key("blocks_per_second", number(value * 20.0));
            case BLOCKS_PER_SECOND -> key("blocks_per_second", number(value));
            case POWER -> value <= 0.0 ? key("free") : key("power", number(value));
            case POWER_PER_SECOND -> value <= 0.0 ? key("free") : key("power_per_second", number(value));
            case RING_SECONDS -> key("power_per_second", number(100.0 / Math.max(1.0E-3, value)));
            case PART_KEPT -> key("stops", number(Math.round((1.0 - value) * 100.0)));
            case STAMINA -> key("stamina", number(value));
            case STAMINA_PER_TICK -> key("stamina_per_second", number(value * 20.0));
            case HARDNESS -> value < 0.0 ? key("breaks_nothing") : key("breaks_up_to", key(material(value)));
            case BLOCK_COUNT -> key("block_count", number(value));
            case STRENGTH -> key("factor", number(value));
        };
    }

    /** The hardest common block that still breaks at this hardness. */
    private static String material(double hardness) {
        if (hardness >= 50.0) {
            return "material.obsidian";
        }
        if (hardness >= 5.0) {
            return "material.iron";
        }
        if (hardness >= 3.0) {
            return "material.ores";
        }
        if (hardness >= 2.0) {
            return "material.wood";
        }
        if (hardness >= 1.5) {
            return "material.stone";
        }
        return hardness >= 0.5 ? "material.dirt" : "material.leaves";
    }

    private static Component key(String name, Object... args) {
        return Component.translatable(PREFIX + name, args);
    }

    /** A number as short as it can be written: 2 instead of 2.00, 0.4 instead of 0.40, at most two decimals. */
    public static String number(double value) {
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0).stripTrailingZeros().toPlainString();
    }
}
