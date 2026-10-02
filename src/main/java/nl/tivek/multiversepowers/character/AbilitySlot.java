package nl.tivek.multiversepowers.character;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

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
    ABILITY_11,
    ABILITY_12,
    // Slots past the twelfth have no key of their own: only abilities on a mouse, space or shift gesture use them.
    ABILITY_13,
    ABILITY_14,
    ABILITY_15,
    ABILITY_16,
    ABILITY_17,
    ABILITY_18,
    ABILITY_19,
    ABILITY_20,
    ABILITY_21,
    ABILITY_22,
    ABILITY_23,
    ABILITY_24,
    ABILITY_25,
    ABILITY_26,
    ABILITY_27,
    ABILITY_28,
    ABILITY_29,
    ABILITY_30,
    ABILITY_31,
    ABILITY_32;

    public int number() {
        return this.ordinal() + 1;
    }

    public boolean keyed() {
        return this.ordinal() < ABILITY_13.ordinal();
    }

    public String getId() {
        return Integer.toString(this.number());
    }

    public Component getDisplayName() {
        return Component.translatable("slot." + MultiversePowers.MODID + ".ability", this.number());
    }

    @Nullable
    public static AbilitySlot byIndex(int index) {
        AbilitySlot[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
