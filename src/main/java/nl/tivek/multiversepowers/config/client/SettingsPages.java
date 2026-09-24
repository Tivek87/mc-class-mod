package nl.tivek.multiversepowers.config.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterConfig;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.Unit;
import nl.tivek.multiversepowers.stamina.StaminaConfig;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

/**
 * What each settings page holds: the world settings (the stamina bar, and one page per character with a part for every
 * ability that has numbers) and your own client settings (see {@link ModConfigs}). Names and explanations come from the
 * translations; a setting without one falls back to its name in the file and the English explanation that stands next
 * to it there.
 */
public final class SettingsPages {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";

    private SettingsPages() {
    }

    /**
     * One page.
     *
     * @param color    the colour of its title
     * @param editable false while its settings can not be changed here, so nothing can be saved: a world page while no
     *                 world of your own is open (see {@link #worldEditable})
     * @param world    true for world settings, false for your own client settings
     * @param save     writes the page's settings file to disk
     */
    public record Page(Component title, int color, List<Section> sections, boolean editable, boolean world,
            Runnable save) {
    }

    /**
     * A part of a page: one ability, or the stamina bar.
     *
     * @param hint which key or button it sits on, or null
     */
    public record Section(Component title, @Nullable Component hint, List<Group> groups) {
    }

    /** Numbers that belong together; the first group of a section usually has no title. */
    public record Group(@Nullable Component title, List<ConfigNumber> numbers) {
    }

    /** Every page, in the order of the tabs: the stamina bar first, then every character, then your own settings. */
    public static List<Page> all() {
        List<Page> pages = new ArrayList<>();
        pages.add(stamina());
        for (GameCharacter character : GameCharacter.values()) {
            pages.add(character(character));
        }
        pages.add(client());
        return pages;
    }

    /**
     * World settings are the world's own: they can only be changed in a world this game runs itself (singleplayer, or a
     * LAN world you host), and are saved with that world. On someone else's server they are the server's, only shown;
     * with no world open there are none.
     */
    public static boolean worldEditable(ModConfigSpec spec) {
        return spec.isLoaded() && Minecraft.getInstance().hasSingleplayerServer();
    }

    /** The tab of this character's page (see {@link #all}). */
    public static int tab(GameCharacter character) {
        return 1 + character.ordinal();
    }

    // ---- The stamina bar ----

    public static Page stamina() {
        ModConfigSpec spec = StaminaConfig.SPEC;
        List<ConfigNumber> numbers = List.of(
                fromSpec(spec, "stamina", "maxStamina", StaminaConfig.MAX_STAMINA, Unit.STAMINA, 10.0),
                fromSpec(spec, "stamina", "sprintDrainPerTick", StaminaConfig.SPRINT_DRAIN, Unit.STAMINA_PER_TICK,
                        0.01),
                fromSpec(spec, "stamina", "jumpCost", StaminaConfig.JUMP_COST, Unit.STAMINA, 0.5),
                fromSpec(spec, "stamina", "regenPerTick", StaminaConfig.REGEN_RATE, Unit.STAMINA_PER_TICK, 0.05),
                fromSpec(spec, "stamina", "regenDelayTicks", StaminaConfig.REGEN_DELAY, Unit.TICKS, 5.0),
                fromSpec(spec, "stamina", "exhaustionRecoverThreshold", StaminaConfig.EXHAUSTION_THRESHOLD,
                        Unit.STAMINA, 1.0));
        Section section = new Section(Component.translatable(PREFIX + "stamina"), null,
                List.of(new Group(null, numbers)));
        return new Page(Component.translatable(PREFIX + "stamina"), 0xFFFF55, List.of(section), worldEditable(spec),
                true, () -> {
                    spec.save();
                    StaminaClient.onConfigUpdated();
                });
    }

    /**
     * A setting of its own file, with its limits and default read from the settings file's own description; named by
     * the translations under {@code page}.
     */
    private static ConfigNumber fromSpec(ModConfigSpec spec, String page, String key,
            ModConfigSpec.ConfigValue<? extends Number> value, Unit unit, double step) {
        ModConfigSpec.Range<?> range = value.getSpec().getRange();
        double min = range == null ? 0.0 : ((Number) range.getMin()).doubleValue();
        double max = range == null ? Double.MAX_VALUE : ((Number) range.getMax()).doubleValue();
        boolean whole = value instanceof ModConfigSpec.IntValue;
        String path = PREFIX + page + "." + key;
        // A file that is not open (world settings with no world open) shows what the mod has.
        return new ConfigNumber(Component.translatableWithFallback(path, key),
                Component.translatableWithFallback(path + ".desc", ""), unit, min, max, step, whole,
                value.getDefault().doubleValue(),
                () -> (spec.isLoaded() ? value.get() : value.getDefault()).doubleValue(), number -> set(value, number));
    }

    @SuppressWarnings("unchecked")
    private static void set(ModConfigSpec.ConfigValue<? extends Number> value, double number) {
        if (value instanceof ModConfigSpec.IntValue whole) {
            whole.set((int) Math.round(number));
        } else {
            ((ModConfigSpec.ConfigValue<Double>) value).set(number);
        }
    }

    // ---- A character ----

    public static Page character(GameCharacter character) {
        List<Section> sections = new ArrayList<>();
        for (CharacterAbility ability : character.abilities()) {
            List<ConfigNumber> general = new ArrayList<>();
            if (ability.usesCooldown()) {
                general.add(cooldown(ability));
            }
            if (ability.usesDamage()) {
                general.add(damage(ability));
            }
            // The ability's own settings, together per part of it, in the order the ability lists them.
            Map<String, Group> parts = new LinkedHashMap<>();
            for (CharacterAbility.Setting setting : ability.settings()) {
                CharacterAbility.Group part = setting.group();
                if (part == null) {
                    general.add(setting(ability, setting));
                    continue;
                }
                parts.computeIfAbsent(part.id(), id -> new Group(Component.translatableWithFallback(
                        PREFIX + "group." + id, part.title()), new ArrayList<>())).numbers()
                        .add(setting(ability, setting));
            }
            List<Group> groups = new ArrayList<>();
            if (!general.isEmpty()) {
                groups.add(new Group(null, general));
            }
            groups.addAll(parts.values());
            if (!groups.isEmpty()) {
                sections.add(new Section(ability.getDisplayName(), hint(ability), groups));
            }
        }
        return new Page(character.getDisplayName(), character.getColor(), sections,
                CharacterConfig.canEdit(character) && Minecraft.getInstance().hasSingleplayerServer(), true,
                () -> CharacterConfig.save(character));
    }

    // ---- Your own settings ----

    /** Your own client settings: what only you see and feel. */
    public static Page client() {
        ModConfigSpec spec = ClientSettings.SPEC;
        List<ConfigNumber> numbers = List.of(
                fromSpec(spec, "client", "cameraShake", ClientSettings.CAMERA_SHAKE, Unit.STRENGTH, 0.1),
                fromSpec(spec, "client", "ramGroundShake", ClientSettings.RAM_GROUND_SHAKE, Unit.STRENGTH, 0.1));
        Section view = new Section(Component.translatable(PREFIX + "client.view"), null,
                List.of(new Group(null, numbers)));
        return new Page(Component.translatable(PREFIX + "client"), 0x8FD3FF, List.of(view), spec.isLoaded(), false,
                spec::save);
    }

    private static ConfigNumber cooldown(CharacterAbility ability) {
        return new ConfigNumber(label(ability, "cooldown", Component.translatable(PREFIX + "cooldown")),
                label(ability, "cooldown.desc", Component.translatable(PREFIX + "cooldown.desc")), Unit.TICKS, 0.0,
                72000.0, 10.0, true, ability.defaultCooldown(), () -> CharacterConfig.cooldown(ability),
                ticks -> CharacterConfig.setCooldown(ability, (int) Math.round(ticks)));
    }

    private static ConfigNumber damage(CharacterAbility ability) {
        return new ConfigNumber(label(ability, "damage", Component.translatable(PREFIX + "damage")),
                label(ability, "damage.desc", Component.translatable(PREFIX + "damage.desc")), Unit.HALF_HEARTS,
                0.0, 2000.0, 1.0, false, ability.defaultDamage(), () -> CharacterConfig.damage(ability),
                halfHearts -> CharacterConfig.setDamage(ability, halfHearts));
    }

    private static ConfigNumber setting(CharacterAbility ability, CharacterAbility.Setting setting) {
        String key = setting.key();
        return new ConfigNumber(label(ability, key, Component.literal(key)),
                label(ability, key + ".desc", Component.literal(setting.comment())), setting.unit(), setting.min(),
                setting.max(), step(setting), setting.whole(), setting.value(),
                () -> CharacterConfig.value(ability, key), number -> CharacterConfig.setValue(ability, key, number));
    }

    /** This ability's own translation of {@code key}, or {@code fallback} when it has none. */
    private static Component label(CharacterAbility ability, String key, Component fallback) {
        String path = PREFIX + ability.path() + "." + key;
        return I18n.exists(path) ? Component.translatable(path) : fallback;
    }

    /** Which key or mouse button the ability sits on, as it is bound right now. */
    private static Component hint(CharacterAbility ability) {
        return switch (ability.mouseButton()) {
            case LEFT -> Component.translatable(PREFIX + "mouse.left");
            case RIGHT -> Component.translatable(PREFIX + "mouse.right");
            case NONE -> AbilityKeys.SLOTS[ability.slot().ordinal()].getTranslatedKeyMessage();
        };
    }

    /** How much - and + change a setting: a small step for small numbers, a bigger one for big numbers. */
    private static double step(CharacterAbility.Setting setting) {
        double size = Math.abs(setting.value());
        return switch (setting.unit()) {
            case HALF_HEARTS, BLOCKS_PER_SECOND, STAMINA -> 1.0;
            case HALF_HEARTS_PER_SPEED, SECONDS, HARDNESS -> 0.5;
            case TICKS -> setting.max() >= 1000.0 ? 10.0 : 1.0;
            case BLOCKS -> setting.max() > 20.0 ? 1.0 : 0.5;
            case BLOCKS_PER_TICK -> size >= 1.0 ? 0.1 : 0.05;
            case POWER -> size < 2.0 ? 0.1 : size < 20.0 ? 0.5 : 1.0;
            case POWER_PER_SECOND, STRENGTH -> 0.1;
            case RING_SECONDS -> 2.5;
            case PART_KEPT, STAMINA_PER_TICK, CHANCE -> 0.05;
            case BLOCK_COUNT -> 10.0;
            case COUNT -> 1.0;
        };
    }
}
