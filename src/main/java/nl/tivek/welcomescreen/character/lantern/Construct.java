package nl.tivek.welcomescreen.character.lantern;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The hard-light shapes Green Lantern can will into his hands, and the empty hands he starts with.
 *
 * <p>None of them exist yet. The wheel keeps twelve slots free for them and every slot says the same
 * thing: placeholder, coming soon. Filling a slot in later is a name in the language files plus the code
 * that makes it do something.
 *
 * <p>Every construct uses the mouse the same way, as the game itself does: left click attacks, with the
 * right hand (the one with the ring); right click defends, with the left hand.
 */
public enum Construct {
    /** Nothing in your hands: only the ring on your finger. This is what you start with. */
    NONE("none"),
    SLOT_1("slot_1"),
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
    SLOT_12("slot_12");

    private static final String KEY = "construct." + WelcomeScreenMod.MODID + ".";

    /** The twelve slots around the wheel, clockwise from the top; the middle of the wheel is NONE. */
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

    /** Which slot of the wheel this is, 1 up to 12; 0 for empty hands. */
    public int number() {
        return this.ordinal();
    }

    public Component getDisplayName() {
        return this == NONE ? Component.translatable(KEY + "none")
                : Component.translatable(KEY + "placeholder", this.number());
    }

    /** The line under the name: what empty hands mean, or that this slot is still to come. */
    public Component getDescription() {
        return Component.translatable(this == NONE ? KEY + "none.about" : KEY + "placeholder.about");
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
