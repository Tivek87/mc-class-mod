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
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.config.Unit;
import nl.tivek.multiversepowers.stamina.StaminaConfig;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

public final class SettingsPages {
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";

    private SettingsPages() {
    }

    public record Page(Component title, int color, List<Section> sections, boolean editable, boolean world,
            Runnable save) {
    }

    public record Section(Component title, @Nullable Component hint, List<Group> groups) {
    }

    public record Group(@Nullable Component title, List<ConfigNumber> numbers) {
    }

    public static List<Page> clientPages() {
        return List.of(client());
    }

    public static List<Page> serverPages() {
        List<Page> pages = new ArrayList<>();
        pages.add(general());
        pages.add(stamina());
        for (GameCharacter character : GameCharacter.values()) {
            pages.add(character(character));
        }
        return pages;
    }

    // The host of this world, or an operator on a server: the server checks the same again.
    public static boolean serverEditable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hasSingleplayerServer() || minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    public static boolean worldEditable(ModConfigSpec spec) {
        return spec.isLoaded() && serverEditable();
    }

    public static final int STAMINA_TAB = 1;

    public static int tab(GameCharacter character) {
        return 2 + character.ordinal();
    }

    public static Page general() {
        ModConfigSpec spec = PowerRules.SPEC;
        String file = ModConfigs.file("general");
        List<ConfigNumber> numbers = List.of(
                fromSpec(spec, file, "general", "damageMultiplier", PowerRules.DAMAGE, Unit.STRENGTH, 0.1),
                fromSpec(spec, file, "general", "cooldownMultiplier", PowerRules.COOLDOWNS, Unit.STRENGTH, 0.1),
                fromSpec(spec, file, "general", "breakBlocks", PowerRules.BREAK_BLOCKS, Unit.SWITCH, 1.0));
        Section section = new Section(Component.translatable(PREFIX + "general.powers"), null,
                List.of(new Group(null, numbers)));
        return new Page(Component.translatable(PREFIX + "general"), 0x9DFF8A, List.of(section), worldEditable(spec),
                true, spec::save);
    }

    public static Page stamina() {
        ModConfigSpec spec = StaminaConfig.SPEC;
        String file = ModConfigs.file("stamina");
        List<ConfigNumber> numbers = List.of(
                fromSpec(spec, file, "stamina", "maxStamina", StaminaConfig.MAX_STAMINA, Unit.STAMINA, 10.0),
                fromSpec(spec, file, "stamina", "sprintDrainPerTick", StaminaConfig.SPRINT_DRAIN,
                        Unit.STAMINA_PER_TICK, 0.01),
                fromSpec(spec, file, "stamina", "jumpCost", StaminaConfig.JUMP_COST, Unit.STAMINA, 0.5),
                fromSpec(spec, file, "stamina", "regenPerTick", StaminaConfig.REGEN_RATE, Unit.STAMINA_PER_TICK,
                        0.05),
                fromSpec(spec, file, "stamina", "regenDelayTicks", StaminaConfig.REGEN_DELAY, Unit.TICKS, 5.0),
                fromSpec(spec, file, "stamina", "exhaustionRecoverThreshold", StaminaConfig.EXHAUSTION_THRESHOLD,
                        Unit.STAMINA, 1.0));
        Section section = new Section(Component.translatable(PREFIX + "stamina"), null,
                List.of(new Group(null, numbers)));
        return new Page(Component.translatable(PREFIX + "stamina"), 0xFFFF55, List.of(section), worldEditable(spec),
                true, () -> {
                    spec.save();
                    StaminaClient.onConfigUpdated();
                });
    }

    private static ConfigNumber fromSpec(ModConfigSpec spec, String file, String page, String key,
            ModConfigSpec.ConfigValue<? extends Number> value, Unit unit, double step) {
        ModConfigSpec.Range<?> range = value.getSpec().getRange();
        double min = range == null ? 0.0 : ((Number) range.getMin()).doubleValue();
        double max = range == null ? Double.MAX_VALUE : ((Number) range.getMax()).doubleValue();
        boolean whole = value instanceof ModConfigSpec.IntValue;
        String path = PREFIX + page + "." + key;
        return new ConfigNumber(Component.translatableWithFallback(path, key),
                Component.translatableWithFallback(path + ".desc", ""), unit, min, max, step, whole,
                value.getDefault().doubleValue(),
                () -> (spec.isLoaded() ? value.get() : value.getDefault()).doubleValue(), number -> set(value, number),
                file, value.getPath());
    }

    @SuppressWarnings("unchecked")
    private static void set(ModConfigSpec.ConfigValue<? extends Number> value, double number) {
        if (value instanceof ModConfigSpec.IntValue whole) {
            whole.set((int) Math.round(number));
        } else {
            ((ModConfigSpec.ConfigValue<Double>) value).set(number);
        }
    }

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
                CharacterConfig.canEdit(character) && serverEditable(), true, () -> CharacterConfig.save(character));
    }

    public static Page client() {
        ModConfigSpec spec = ClientSettings.SPEC;
        String file = ModConfigs.file("client");
        Section view = new Section(Component.translatable(PREFIX + "client.view"), null, List.of(new Group(null,
                List.of(fromSpec(spec, file, "client", "cameraShake", ClientSettings.CAMERA_SHAKE, Unit.STRENGTH, 0.1),
                        fromSpec(spec, file, "client", "ramGroundShake", ClientSettings.RAM_GROUND_SHAKE,
                                Unit.STRENGTH, 0.1)))));
        Section sound = new Section(Component.translatable(PREFIX + "client.sound"), null, List.of(new Group(null,
                List.of(fromSpec(spec, file, "client", "themeMusic", ClientSettings.THEME_MUSIC, Unit.SWITCH, 1.0)))));
        Section updates = new Section(Component.translatable(PREFIX + "client.updates"), null, List.of(new Group(null,
                List.of(fromSpec(spec, file, "client", "updateCheckMinutes", ClientSettings.UPDATE_CHECK,
                                Unit.MINUTES, 1.0),
                        fromSpec(spec, file, "client", "updatePopupSeconds", ClientSettings.UPDATE_POPUP,
                                Unit.SECONDS, 1.0)))));
        return new Page(Component.translatable(PREFIX + "client"), 0x8FD3FF, List.of(view, sound, updates),
                spec.isLoaded(), false, spec::save);
    }

    private static ConfigNumber cooldown(CharacterAbility ability) {
        return new ConfigNumber(label(ability, "cooldown", Component.translatable(PREFIX + "cooldown")),
                label(ability, "cooldown.desc", Component.translatable(PREFIX + "cooldown.desc")), Unit.TICKS, 0.0,
                72000.0, 10.0, true, ability.defaultCooldown(), () -> CharacterConfig.cooldown(ability),
                ticks -> CharacterConfig.setCooldown(ability, (int) Math.round(ticks)),
                ModConfigs.file(ability.character().getId()), CharacterConfig.path(ability, "cooldownTicks"));
    }

    private static ConfigNumber damage(CharacterAbility ability) {
        return new ConfigNumber(label(ability, "damage", Component.translatable(PREFIX + "damage")),
                label(ability, "damage.desc", Component.translatable(PREFIX + "damage.desc")), Unit.HALF_HEARTS,
                0.0, 2000.0, 1.0, false, ability.defaultDamage(), () -> CharacterConfig.damage(ability),
                halfHearts -> CharacterConfig.setDamage(ability, halfHearts),
                ModConfigs.file(ability.character().getId()), CharacterConfig.path(ability, "damage"));
    }

    private static ConfigNumber setting(CharacterAbility ability, CharacterAbility.Setting setting) {
        String key = setting.key();
        return new ConfigNumber(label(ability, key, Component.literal(key)),
                label(ability, key + ".desc", Component.literal(setting.comment())), setting.unit(), setting.min(),
                setting.max(), step(setting), setting.whole(), setting.value(),
                () -> CharacterConfig.value(ability, key), number -> CharacterConfig.setValue(ability, key, number),
                ModConfigs.file(ability.character().getId()), CharacterConfig.path(ability, key));
    }

    private static Component label(CharacterAbility ability, String key, Component fallback) {
        String path = PREFIX + ability.path() + "." + key;
        return I18n.exists(path) ? Component.translatable(path) : fallback;
    }

    private static Component hint(CharacterAbility ability) {
        return switch (ability.mouseButton()) {
            case LEFT -> Component.translatable(PREFIX + "mouse.left");
            case RIGHT -> Component.translatable(PREFIX + "mouse.right");
            case NONE -> AbilityKeys.SLOTS[ability.slot().ordinal()].getTranslatedKeyMessage();
        };
    }

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
            case COUNT, SWITCH, MINUTES -> 1.0;
        };
    }
}
