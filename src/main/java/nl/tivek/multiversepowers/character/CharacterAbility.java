package nl.tivek.multiversepowers.character;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.config.Unit;

public final class CharacterAbility {
    public enum Crouch {
        SAME,
        UNDO,
        ALTERNATE
    }

    // KEY is the slot's own key; the rest are the fixed gestures listed under Controls (scroll can be rebound).
    public enum Input {
        KEY,
        LEFT,
        RIGHT,
        SCROLL,
        SPACE
    }

    // PRESS and RELEASE: when a button with a hold version counts as a click. NEVER: only holding it does
    // anything. DOUBLE: two quick presses.
    public enum Tap {
        PRESS,
        RELEASE,
        NEVER,
        DOUBLE
    }

    // Some gestures do one thing on the ground and another in flight.
    public enum When {
        ALWAYS,
        GROUND,
        FLYING
    }

    public record Setting(String key, boolean whole, double value, double min, double max, Unit unit,
            @Nullable Group group, String comment, double[] was) {
    }

    public record Group(String id, String title) {
    }

    private final GameCharacter character;
    private final AbilitySlot slot;
    private final String id;
    private final List<Setting> settings = new ArrayList<>();
    @Nullable
    private Group group;
    private int defaultCooldown;
    private int[] oldCooldowns = new int[0];
    private double defaultDamage;
    private double[] oldDamages = new double[0];
    private boolean usesCooldown;
    private boolean usesDamage;
    private boolean held;
    private boolean clientOnly;
    private boolean placeholder;
    private Crouch crouch = Crouch.SAME;
    private Input input = Input.KEY;
    private When when = When.ALWAYS;
    private int holdTicks;
    private Tap tap = Tap.PRESS;

    CharacterAbility(GameCharacter character, AbilitySlot slot, String id) {
        this.character = character;
        this.slot = slot;
        this.id = id;
    }

    public CharacterAbility cooldown(int ticks) {
        this.defaultCooldown = ticks;
        this.usesCooldown = true;
        return this;
    }

    public CharacterAbility cooldownWas(int... oldTicks) {
        this.oldCooldowns = oldTicks;
        return this;
    }

    public CharacterAbility damageWas(double... oldHalfHearts) {
        this.oldDamages = oldHalfHearts;
        return this;
    }

    public CharacterAbility damage(double halfHearts) {
        this.defaultDamage = halfHearts;
        this.usesDamage = true;
        return this;
    }

    public CharacterAbility held() {
        this.held = true;
        return this;
    }

    public CharacterAbility clientOnly() {
        this.clientOnly = true;
        return this;
    }

    CharacterAbility placeholder() {
        this.placeholder = true;
        return this;
    }

    public boolean isPlaceholder() {
        return this.placeholder;
    }

    public CharacterAbility crouch(Crouch crouch) {
        this.crouch = crouch;
        return this;
    }

    public CharacterAbility input(Input input) {
        this.input = input;
        return this;
    }

    public CharacterAbility holdOnly(int ticks) {
        this.holdTicks = ticks;
        this.tap = Tap.NEVER;
        return this;
    }

    public CharacterAbility doubleTap() {
        this.tap = Tap.DOUBLE;
        return this;
    }

    public CharacterAbility when(When when) {
        this.when = when;
        return this;
    }

    public CharacterAbility holdVersion(int ticks, Tap tap) {
        this.holdTicks = ticks;
        this.tap = tap;
        return this;
    }

    public CharacterAbility setting(String key, double value, double min, double max, Unit unit, String comment) {
        this.settings.add(new Setting(key, false, value, min, max, unit, this.group, comment, new double[0]));
        return this;
    }

    public CharacterAbility settingInt(String key, int value, int min, int max, Unit unit, String comment) {
        this.settings.add(new Setting(key, true, value, min, max, unit, this.group, comment, new double[0]));
        return this;
    }

    public CharacterAbility group(String id, String title) {
        this.group = new Group(id, title);
        return this;
    }

    public CharacterAbility was(double... oldDefaults) {
        int last = this.settings.size() - 1;
        Setting setting = this.settings.get(last);
        this.settings.set(last, new Setting(setting.key(), setting.whole(), setting.value(), setting.min(),
                setting.max(), setting.unit(), setting.group(), setting.comment(), oldDefaults));
        return this;
    }

    public GameCharacter character() {
        return this.character;
    }

    public AbilitySlot slot() {
        return this.slot;
    }

    public String id() {
        return this.id;
    }

    public boolean isHeld() {
        return this.held;
    }

    public boolean isClientOnly() {
        return this.clientOnly;
    }

    public Crouch crouchDoes() {
        return this.crouch;
    }

    public Input input() {
        return this.input;
    }

    public When when() {
        return this.when;
    }

    // A mouse or space gesture, as opposed to the slot's own key.
    public boolean onGesture() {
        return this.input != Input.KEY;
    }

    public int holdTicks() {
        return this.holdTicks;
    }

    public Tap tapWhen() {
        return this.tap;
    }

    public List<Setting> settings() {
        return this.settings;
    }

    public boolean has(String key) {
        for (Setting setting : this.settings) {
            if (setting.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public int[] oldCooldowns() {
        return this.oldCooldowns;
    }

    public int defaultCooldown() {
        return this.defaultCooldown;
    }

    public double defaultDamage() {
        return this.defaultDamage;
    }

    public double[] oldDamages() {
        return this.oldDamages;
    }

    public boolean usesCooldown() {
        return this.usesCooldown;
    }

    public boolean usesDamage() {
        return this.usesDamage;
    }

    public int getCooldown() {
        return (int) Math.round(CharacterConfig.cooldown(this) * PowerRules.cooldowns());
    }

    public float getDamage() {
        return (float) (CharacterConfig.damage(this) * PowerRules.damage());
    }

    public double value(String key) {
        double value = CharacterConfig.value(this, key);
        for (Setting setting : this.settings) {
            if (setting.key().equals(key)) {
                return world(setting, value);
            }
        }
        return value;
    }

    // A setting as this world's power rules make it: damage times the damage multiplier, a cost of ring power
    // times the cost multiplier, a full ring's flight as much shorter.
    private static double world(Setting setting, double value) {
        String key = setting.key().toLowerCase(Locale.ROOT);
        return switch (setting.unit()) {
            case HALF_HEARTS, HALF_HEARTS_PER_SPEED -> value * PowerRules.damage();
            case POWER, POWER_PER_SECOND -> key.contains("cost") || key.contains("persecond")
                    ? value * PowerRules.powerCost() : value;
            case RING_SECONDS -> value / Math.max(1.0E-3, PowerRules.powerCost());
            default -> value;
        };
    }

    public int intValue(String key) {
        return (int) Math.round(this.value(key));
    }

    public Component getDisplayName() {
        if (this.placeholder) {
            return Component.translatable("ability." + MultiversePowers.MODID + ".placeholder");
        }
        return Component.translatable("ability." + MultiversePowers.MODID + "." + this.character.getId() + "."
                + this.id);
    }

    public String path() {
        return this.character.getId() + "." + this.id;
    }
}
