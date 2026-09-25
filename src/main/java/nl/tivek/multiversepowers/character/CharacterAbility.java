package nl.tivek.multiversepowers.character;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.Unit;

public final class CharacterAbility {
    public enum Crouch {
        SAME,
        UNDO,
        ALTERNATE
    }

    public enum Mouse {
        NONE,
        LEFT,
        RIGHT
    }

    public enum Tap {
        PRESS,
        RELEASE
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
    private Crouch crouch = Crouch.SAME;
    private Mouse mouse = Mouse.NONE;
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

    public CharacterAbility crouch(Crouch crouch) {
        this.crouch = crouch;
        return this;
    }

    public CharacterAbility mouse(Mouse mouse) {
        this.mouse = mouse;
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

    public Mouse mouseButton() {
        return this.mouse;
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
        return CharacterConfig.cooldown(this);
    }

    public float getDamage() {
        return (float) CharacterConfig.damage(this);
    }

    public double value(String key) {
        return CharacterConfig.value(this, key);
    }

    public int intValue(String key) {
        return (int) Math.round(CharacterConfig.value(this, key));
    }

    public Component getDisplayName() {
        return Component.translatable("ability." + MultiversePowers.MODID + "." + this.character.getId() + "."
                + this.id);
    }

    public String path() {
        return this.character.getId() + "." + this.id;
    }
}
