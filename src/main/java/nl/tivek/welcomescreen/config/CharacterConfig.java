package nl.tivek.welcomescreen.config;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;

/**
 * The settings of every character: one file per character, in the mod's own config folder (see
 * {@link ModConfigs}). Inside a file every ability has its own section with its cooldown, its damage
 * and its own settings:
 *
 * <pre>
 * [abilities.portal]
 *     cooldownTicks = 400
 *     damage = 50.0
 *     homingRangeBlocks = 30.0
 * </pre>
 *
 * Everything in the files is made from what the characters themselves say their abilities are (see
 * CharacterAbility), so a new character brings its own file and its own settings along by itself.
 */
public final class CharacterConfig {
    /**
     * The version of the mod's defaults. Raise it whenever a default changes, and tell the setting what it was
     * before with {@link CharacterAbility#was}: files that still hold the old number then take the new one.
     */
    private static final int DEFAULTS_VERSION = 8;

    private static final Map<GameCharacter, ModConfigSpec> SPECS = new EnumMap<>(GameCharacter.class);
    private static final Map<GameCharacter, ModConfigSpec.IntValue> VERSIONS = new EnumMap<>(GameCharacter.class);
    // Keyed by "doc_ock.portal", and by "doc_ock.portal.homingRangeBlocks" for the settings.
    private static final Map<String, ModConfigSpec.IntValue> COOLDOWNS = new HashMap<>();
    private static final Map<String, ModConfigSpec.DoubleValue> DAMAGE = new HashMap<>();
    private static final Map<String, ModConfigSpec.ConfigValue<? extends Number>> SETTINGS = new HashMap<>();

    static {
        for (GameCharacter character : GameCharacter.values()) {
            SPECS.put(character, build(character));
        }
    }

    private CharacterConfig() {
    }

    /** Gives every character its own file in the mod's config folder. */
    static void register(ModContainer container, IEventBus modEventBus) {
        for (Map.Entry<GameCharacter, ModConfigSpec> entry : SPECS.entrySet()) {
            container.registerConfig(ModConfig.Type.COMMON, entry.getValue(),
                    ModConfigs.file(entry.getKey().getId()));
        }
        modEventBus.addListener(ModConfigEvent.Loading.class, CharacterConfig::onLoad);
        modEventBus.addListener(ModConfigEvent.Reloading.class, CharacterConfig::onLoad);
    }

    private static ModConfigSpec build(GameCharacter character) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VERSIONS.put(character, builder.comment("Kept up to date by the mod, leave it as it is: which of the mod's"
                + " defaults this file has taken over. A setting you never changed follows the mod when its default"
                + " changes; one you did change stays yours.")
                .defineInRange("defaultsVersion", 0, 0, Integer.MAX_VALUE));
        builder.comment("The abilities of " + character.getId() + ", one section per ability.",
                "Cooldowns are in ticks (20 ticks = 1 second), damage is in half hearts (an Iron Golem has 100),",
                "ring power is out of the 100 a full ring holds. The same numbers can be changed in the game:",
                "Mods > this mod > Config.")
                .push("abilities");
        if (character.abilities().isEmpty()) {
            builder.comment("This character has no abilities yet.").define("none", true);
        }
        for (CharacterAbility ability : character.abilities()) {
            builder.comment("Ability " + ability.slot().number()
                    + ": the key \"Ability " + ability.slot().number() + "\" in Options > Controls")
                    .push(ability.id());
            if (ability.usesCooldown()) {
                COOLDOWNS.put(ability.path(), builder.comment("Cooldown in ticks (20 = 1 second)")
                        .defineInRange("cooldownTicks", ability.defaultCooldown(), 0, 72000));
            }
            if (ability.usesDamage()) {
                DAMAGE.put(ability.path(), builder.comment("Damage in half hearts (0 = this ability does none)")
                        .defineInRange("damage", ability.defaultDamage(), 0.0, 2000.0));
            }
            for (CharacterAbility.Setting setting : ability.settings()) {
                // Every part of the ability says which part it is: "[Light Beam (hold ...)] Damage ...".
                String comment = setting.group() == null ? setting.comment()
                        : "[" + setting.group().title() + "] " + setting.comment();
                SETTINGS.put(ability.path() + "." + setting.key(), setting.whole()
                        ? builder.comment(comment).defineInRange(setting.key(),
                                (int) setting.value(), (int) setting.min(), (int) setting.max())
                        : builder.comment(comment).defineInRange(setting.key(),
                                setting.value(), setting.min(), setting.max()));
            }
            builder.pop();
        }
        builder.pop();
        return builder.build();
    }

    /**
     * A character's file has been read (or read again after it changed on disk). Once for every new version of
     * the defaults: every setting that still holds one of its old defaults was never changed by hand, so it takes
     * the new default, and the file remembers it is up to date.
     */
    private static void onLoad(ModConfigEvent event) {
        for (Map.Entry<GameCharacter, ModConfigSpec> entry : SPECS.entrySet()) {
            ModConfigSpec spec = entry.getValue();
            if (event.getConfig().getSpec() != spec || !spec.isLoaded()) {
                continue;
            }
            ModConfigSpec.IntValue version = VERSIONS.get(entry.getKey());
            if (version.get() >= DEFAULTS_VERSION) {
                return;
            }
            for (CharacterAbility ability : entry.getKey().abilities()) {
                ModConfigSpec.IntValue cooldown = COOLDOWNS.get(ability.path());
                if (cooldown != null) {
                    for (int old : ability.oldCooldowns()) {
                        if (cooldown.get() == old) {
                            cooldown.set(ability.defaultCooldown());
                        }
                    }
                }
                for (CharacterAbility.Setting setting : ability.settings()) {
                    ModConfigSpec.ConfigValue<? extends Number> value = SETTINGS.get(ability.path() + "."
                            + setting.key());
                    if (value != null && wasDefault(value.get().doubleValue(), setting.was())) {
                        setValue(ability, setting.key(), setting.value());
                    }
                }
            }
            version.set(DEFAULTS_VERSION);
            spec.save();
            return;
        }
    }

    private static boolean wasDefault(double value, double[] oldDefaults) {
        for (double old : oldDefaults) {
            if (Math.abs(value - old) < 1.0E-6) {
                return true;
            }
        }
        return false;
    }

    /** True once this character's file has been read; before that the defaults are used. */
    private static boolean loaded(CharacterAbility ability) {
        ModConfigSpec spec = SPECS.get(ability.character());
        return spec != null && spec.isLoaded();
    }

    /** The ability's cooldown in ticks, from its file. */
    public static int cooldown(CharacterAbility ability) {
        ModConfigSpec.IntValue value = COOLDOWNS.get(ability.path());
        return value == null || !loaded(ability) ? ability.defaultCooldown() : value.get();
    }

    /** The ability's damage in half hearts, from its file. */
    public static double damage(CharacterAbility ability) {
        ModConfigSpec.DoubleValue value = DAMAGE.get(ability.path());
        return value == null || !loaded(ability) ? ability.defaultDamage() : value.get();
    }

    /** One of the ability's own settings, from its file. */
    public static double value(CharacterAbility ability, String key) {
        ModConfigSpec.ConfigValue<? extends Number> value = SETTINGS.get(ability.path() + "." + key);
        if (value == null || !loaded(ability)) {
            for (CharacterAbility.Setting setting : ability.settings()) {
                if (setting.key().equals(key)) {
                    return setting.value();
                }
            }
            throw new IllegalArgumentException(ability.path() + " has no setting named " + key);
        }
        return value.get().doubleValue();
    }

    // ---- Changing it from the settings screen ----

    /** True while this character's file can be written to (it has been read in). */
    public static boolean canEdit(GameCharacter character) {
        ModConfigSpec spec = SPECS.get(character);
        return spec != null && spec.isLoaded();
    }

    /** Puts a new cooldown in the file, in ticks. Does nothing while the file is not read yet. */
    public static void setCooldown(CharacterAbility ability, int ticks) {
        ModConfigSpec.IntValue value = COOLDOWNS.get(ability.path());
        if (value != null && loaded(ability)) {
            value.set(ticks);
        }
    }

    /** Puts a new damage in the file, in half hearts. */
    public static void setDamage(CharacterAbility ability, double halfHearts) {
        ModConfigSpec.DoubleValue value = DAMAGE.get(ability.path());
        if (value != null && loaded(ability)) {
            value.set(halfHearts);
        }
    }

    /** Puts a new number in one of the ability's own settings. */
    @SuppressWarnings("unchecked")
    public static void setValue(CharacterAbility ability, String key, double number) {
        ModConfigSpec.ConfigValue<? extends Number> value = SETTINGS.get(ability.path() + "." + key);
        if (value == null || !loaded(ability)) {
            return;
        }
        if (value instanceof ModConfigSpec.IntValue whole) {
            whole.set((int) Math.round(number));
        } else {
            ((ModConfigSpec.ConfigValue<Double>) value).set(number);
        }
    }

    /** Writes this character's file to disk, after changes from the settings screen. */
    public static void save(GameCharacter character) {
        ModConfigSpec spec = SPECS.get(character);
        if (spec != null && spec.isLoaded()) {
            spec.save();
        }
    }
}
