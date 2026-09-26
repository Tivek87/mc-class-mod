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
    ABILITY_12;

    public int number() {
        return this.ordinal() + 1;
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
