package nl.tivek.multiversepowers.config;

import java.math.BigDecimal;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

public enum Unit {
    HALF_HEARTS,
    HALF_HEARTS_PER_SPEED,
    TICKS,
    SECONDS,
    BLOCKS,
    BLOCKS_PER_TICK,
    BLOCKS_PER_SECOND,
    POWER,
    POWER_PER_SECOND,
    RING_SECONDS,
    PART_KEPT,
    STAMINA,
    STAMINA_PER_TICK,
    HARDNESS,
    BLOCK_COUNT,
    COUNT,
    STRENGTH,
    CHANCE,
    SWITCH,
    MINUTES;

    private static final String PREFIX = "config." + MultiversePowers.MODID + ".unit.";

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
            case COUNT -> key("count", number(value));
            case STRENGTH -> key("factor", number(value));
            case CHANCE -> key("chance", number(Math.round(value * 100.0)));
            case SWITCH -> key(value >= 0.5 ? "on" : "off");
            case MINUTES -> value <= 0.0 ? key("never") : key("minutes", number(value));
        };
    }

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

    public static String number(double value) {
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0).stripTrailingZeros().toPlainString();
    }
}
