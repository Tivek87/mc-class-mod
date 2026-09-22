package nl.tivek.welcomescreen.character;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.config.CharacterConfig;

/**
 * One ability of one character, in one {@link AbilitySlot}. Everything about it is described here
 * once: which key slot it sits in, whether you hold that key down, what crouching does with it, and
 * its numbers. The numbers are only the defaults; what a player really gets comes from that
 * character's own config file (see {@link CharacterConfig}).
 */
public final class CharacterAbility {
    /** What crouching while you press this ability's key does. Every character decides for itself. */
    public enum Crouch {
        /** Crouching changes nothing. */
        SAME,
        /** The undo of this ability (let go, put it back down). Free: a cooldown never blocks it. */
        UNDO,
        /** Another version of the same ability, with the same cooldown. */
        ALTERNATE
    }

    /** The mouse button this ability hangs on instead of its own key: what the character always does. */
    public enum Mouse {
        /** Its own ability key, like every other ability. */
        NONE,
        /** Left click, the hand that attacks. */
        LEFT,
        /** Right click, the hand that defends. */
        RIGHT
    }

    /**
     * When the quick version of a mouse ability goes off (see {@link #holdVersion}): the moment the button goes
     * down, or once it comes up again before it was held long enough for the hold version.
     */
    public enum Tap {
        /** Right away: a shot should never wait for the button to come up. */
        PRESS,
        /** Once the button comes up: a switch must not flip when you only meant to hold the button. */
        RELEASE
    }

    /**
     * One extra setting of this ability in the config file, next to its cooldown and its damage.
     *
     * @param whole true for a whole number (a count), false for a number with decimals
     */
    public record Setting(String key, boolean whole, double value, double min, double max, String comment) {
    }

    private final GameCharacter character;
    private final AbilitySlot slot;
    private final String id;
    private final List<Setting> settings = new ArrayList<>();
    private int defaultCooldown;
    private double defaultDamage;
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

    // ---- Describing it (only while the characters are being built) ----

    /** Ticks before it can be used again; 20 ticks = 1 second. */
    public CharacterAbility cooldown(int ticks) {
        this.defaultCooldown = ticks;
        return this;
    }

    /** What its main hit does, in half hearts. */
    public CharacterAbility damage(double halfHearts) {
        this.defaultDamage = halfHearts;
        return this;
    }

    /** You hold this key down instead of pressing it once (it sends a start and a stop). */
    public CharacterAbility held() {
        this.held = true;
        return this;
    }

    /**
     * Your own game does this ability by itself: nothing about it is sent to the server, so it can only
     * change what you see. Used for the parts that are still only a menu.
     */
    public CharacterAbility clientOnly() {
        this.clientOnly = true;
        return this;
    }

    public CharacterAbility crouch(Crouch crouch) {
        this.crouch = crouch;
        return this;
    }

    /**
     * This ability hangs on a mouse button instead of on its own ability key: what the character does when
     * you are not doing anything else. While it runs, the mouse does nothing it normally would.
     */
    public CharacterAbility mouse(Mouse mouse) {
        this.mouse = mouse;
        return this;
    }

    /**
     * Click or hold, for an ability on a mouse button: a tap of the button does the quick version, and holding it
     * down {@code ticks} long does the hold version instead, for as long as the button then stays down. Every
     * mouse ability works this way, with empty hands and with every construct alike.
     *
     * @param ticks how long the button has to stay down for the hold version (20 ticks = 1 second)
     * @param tap   when the quick version goes off: as the button goes down, or as it comes up again
     */
    public CharacterAbility holdVersion(int ticks, Tap tap) {
        this.holdTicks = ticks;
        this.tap = tap;
        return this;
    }

    /** An extra setting of its own in the config file. */
    public CharacterAbility setting(String key, double value, double min, double max, String comment) {
        this.settings.add(new Setting(key, false, value, min, max, comment));
        return this;
    }

    /** An extra setting of its own that counts whole things (blocks, ticks). */
    public CharacterAbility settingInt(String key, int value, int min, int max, String comment) {
        this.settings.add(new Setting(key, true, value, min, max, comment));
        return this;
    }

    // ---- Asking about it ----

    public GameCharacter character() {
        return this.character;
    }

    public AbilitySlot slot() {
        return this.slot;
    }

    public String id() {
        return this.id;
    }

    /** True when you hold its key down instead of pressing it once. */
    public boolean isHeld() {
        return this.held;
    }

    /** True when your own game handles this ability and the server never hears about it. */
    public boolean isClientOnly() {
        return this.clientOnly;
    }

    public Crouch crouchDoes() {
        return this.crouch;
    }

    /** The mouse button this ability hangs on, or {@link Mouse#NONE} when it has its own key. */
    public Mouse mouseButton() {
        return this.mouse;
    }

    /** How long the button has to stay down for the hold version, in ticks; 0 when there is none. */
    public int holdTicks() {
        return this.holdTicks;
    }

    /** When the quick version of a mouse ability goes off. */
    public Tap tapWhen() {
        return this.tap;
    }

    public List<Setting> settings() {
        return this.settings;
    }

    /** True when this ability has a setting of that name (staminaCost, rangeBlocks, ...). */
    public boolean has(String key) {
        for (Setting setting : this.settings) {
            if (setting.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public int defaultCooldown() {
        return this.defaultCooldown;
    }

    public double defaultDamage() {
        return this.defaultDamage;
    }

    /** The cooldown from the config file, in ticks. */
    public int getCooldown() {
        return CharacterConfig.cooldown(this);
    }

    /** The damage from the config file, in half hearts. */
    public float getDamage() {
        return (float) CharacterConfig.damage(this);
    }

    /** One of its own settings from the config file. */
    public double value(String key) {
        return CharacterConfig.value(this, key);
    }

    /** One of its own whole-number settings from the config file. */
    public int intValue(String key) {
        return (int) Math.round(CharacterConfig.value(this, key));
    }

    public Component getDisplayName() {
        return Component.translatable("ability." + WelcomeScreenMod.MODID + "." + this.character.getId() + "."
                + this.id);
    }

    /** How this ability is named in the tables that keep cooldowns: "doc_ock.portal". */
    public String path() {
        return this.character.getId() + "." + this.id;
    }
}
