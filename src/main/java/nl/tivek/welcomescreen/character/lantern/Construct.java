package nl.tivek.welcomescreen.character.lantern;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The hard-light shapes Green Lantern can will into his hands, and the empty hands he starts with.
 *
 * <p>The first slot of the wheel holds a sword and a shield (see {@link SwordShield}); the other fifteen are still
 * kept free and say placeholder, coming soon. Filling a slot in later is a name in the language files plus the code
 * that makes it do something.
 *
 * <p>Every construct uses the mouse the same way, as the game itself does: left click attacks, with the
 * right hand (the one with the ring); right click defends, with the left hand.
 */
public enum Construct {
    /** Nothing in your hands: only the ring on your finger. This is what you start with. */
    NONE("none"),
    /** A sword in the ring hand and a shield on the other arm. */
    SWORD_SHIELD("sword_shield"),
    SLOT_2("slot_2"),
    SLOT_3("slot_3"),
    SLOT_4("slot_4"),
    SLOT_5("slot_5"),
    SLOT_6("slot_6"),
    SLOT_7("slot_7"),
    SLOT_8("slot_8"),
    SLOT_9("slot_9"),
    SLOT_10("slot_10"),
    SLOT_11("slot_11"),
    SLOT_12("slot_12"),
    SLOT_13("slot_13"),
    SLOT_14("slot_14"),
    SLOT_15("slot_15"),
    SLOT_16("slot_16");

    private static final String KEY = "construct." + WelcomeScreenMod.MODID + ".";

    /** The sixteen slots around the wheel, clockwise from the top; the middle of the wheel is NONE. */
    public static final List<Construct> WHEEL = wheel();

    private final String id;

    Construct(String id) {
        this.id = id;
    }

    private static List<Construct> wheel() {
        List<Construct> slots = new ArrayList<>();
        for (Construct construct : values()) {
            if (construct != NONE) {
                slots.add(construct);
            }
        }
        return List.copyOf(slots);
    }

    public String getId() {
        return this.id;
    }

    /** Which slot of the wheel this is, 1 up to 16; 0 for empty hands. */
    public int number() {
        return this.ordinal();
    }

    /** True for a construct that really exists; false for a slot still kept free (a placeholder). */
    public boolean made() {
        return this == NONE || this == SWORD_SHIELD;
    }

    public Component getDisplayName() {
        return this.made() ? Component.translatable(KEY + this.id)
                : Component.translatable(KEY + "placeholder", this.number());
    }

    /** The line under the name: what it is, or that this slot is still to come. */
    public Component getDescription() {
        return Component.translatable(this.made() ? KEY + this.id + ".about" : KEY + "placeholder.about");
    }

    /** The construct with this number (its ordinal), or {@link #NONE}. */
    public static Construct byIndex(int index) {
        Construct[] all = values();
        return index >= 0 && index < all.length ? all[index] : NONE;
    }

    @Nullable
    public static Construct byId(String id) {
        for (Construct construct : values()) {
            if (construct.id.equals(id)) {
                return construct;
            }
        }
        return null;
    }
}
