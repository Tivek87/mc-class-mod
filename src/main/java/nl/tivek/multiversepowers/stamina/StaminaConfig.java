package nl.tivek.multiversepowers.stamina;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StaminaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MAX_STAMINA;
    public static final ModConfigSpec.DoubleValue SPRINT_DRAIN;
    public static final ModConfigSpec.DoubleValue JUMP_COST;
    public static final ModConfigSpec.DoubleValue REGEN_RATE;
    public static final ModConfigSpec.IntValue REGEN_DELAY;
    public static final ModConfigSpec.DoubleValue EXHAUSTION_THRESHOLD;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Stamina System Configuration").push("stamina");

        MAX_STAMINA = builder.comment("Maximum / standard stamina (default: 100.0)")
                .defineInRange("maxStamina", 100.0, 10.0, 1000.0);

        SPRINT_DRAIN = builder.comment("Stamina drained per tick while sprinting (default: 0.2, twice as fast as 0.1)")
                .defineInRange("sprintDrainPerTick", 0.2, 0.01, 10.0);

        JUMP_COST = builder.comment("Stamina cost per jump (default: 3.2)")
                .defineInRange("jumpCost", 3.2, 0.0, 100.0);

        REGEN_RATE = builder.comment("Stamina regenerated per tick while resting (default: 0.6)")
                .defineInRange("regenPerTick", 0.6, 0.05, 10.0);

        REGEN_DELAY = builder.comment("Delay in ticks before regeneration starts (default: 20 ticks = 1 second)")
                .defineInRange("regenDelayTicks", 20, 0, 200);

        EXHAUSTION_THRESHOLD = builder.comment("Stamina needed to recover from exhaustion (default: 10.0 = 1 icon)")
                .defineInRange("exhaustionRecoverThreshold", 10.0, 1.0, 500.0);

        builder.pop();
        SPEC = builder.build();
    }

    private StaminaConfig() {
    }

    public static float getMaxStamina() {
        return MAX_STAMINA.get().floatValue();
    }

    public static float getSprintDrain() {
        return SPRINT_DRAIN.get().floatValue();
    }

    public static float getJumpCost() {
        return JUMP_COST.get().floatValue();
    }

    public static float getRegenRate() {
        return REGEN_RATE.get().floatValue();
    }

    public static int getRegenDelay() {
        return REGEN_DELAY.get();
    }

    public static float getExhaustionThreshold() {
        return EXHAUSTION_THRESHOLD.get().floatValue();
    }
}

