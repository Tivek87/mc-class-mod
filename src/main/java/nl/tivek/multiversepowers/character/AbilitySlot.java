package nl.tivek.multiversepowers.character;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * The ability keys, the same for every character: ability 1 up to ability 11. A slot is only a
 * number, never a kind of ability, so every character is free to put anything in any slot. What key 1
 * does depends purely on who you are (see {@link GameCharacter}).
 */
public enum AbilitySlot {
    ABILITY_1,
    ABILITY_2,
    ABILITY_3,
    ABILITY_4,
    ABILITY_5,
    ABILITY_6,
    ABILITY_7,
    ABILITY_8,
    ABILITY_9,
    ABILITY_10,
    ABILITY_11;

    /** The number a player sees: 1 for the first slot. */
    public int number() {
        return this.ordinal() + 1;
    }

    /** "1": used in key names, in the config file and in translation keys. */
    public String getId() {
        return Integer.toString(this.number());
    }

    /** "Ability 1", used when a character has nothing in this slot. */
    public Component getDisplayName() {
        return Component.translatable("slot." + MultiversePowers.MODID + ".ability", this.number());
    }

    @Nullable
    public static AbilitySlot byIndex(int index) {
        AbilitySlot[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
