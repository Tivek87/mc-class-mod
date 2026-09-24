package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordShield;

/**
 * The hard-light shapes Green Lantern can will into his hands, and the empty hands he starts with.
 *
 * <p>The first slot of the wheel holds a sword and a shield (see {@link SwordShield}). The other fifteen weapons have
 * their name and their picture already, but nothing to do yet: holding one does what empty hands do. Making one work is
 * the code that makes it do something, and the line in the language files that says how (see {@link #made}).
 *
 * <p>Every construct uses the mouse the same way, as the game itself does: left click attacks, with the
 * right hand (the one with the ring); right click defends, with the left hand.
 */
public enum Construct {
    /** Nothing in your hands: only the ring on your finger. This is what you start with. */
    NONE("none"),
    /** A sword in the ring hand and a shield on the other arm. */
    SWORD_SHIELD("sword_shield"),
    ENERGY_WHIP("energy_whip"),
    GAUNTLETS("gauntlets"),
    DAGGERS("daggers"),
    BATTLEAXE("battleaxe"),
    WAR_HAMMER("war_hammer"),
    HALBERD("halberd"),
    CHAINSAW("chainsaw"),
    REVOLVERS("revolvers"),
    SHOTGUN("shotgun"),
    SMGS("smgs"),
    ARM_CANNON("arm_cannon"),
    GRENADE_LAUNCHER("grenade_launcher"),
    MINIGUN("minigun"),
    ROCKET_LAUNCHER("rocket_launcher"),
    FLAMETHROWER("flamethrower");

    private static final String KEY = "construct." + MultiversePowers.MODID + ".";

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

    /** True for a construct that already does something; false for a weapon that only has its name and picture yet. */
    public boolean made() {
        return this == NONE || this == SWORD_SHIELD;
    }

    public Component getDisplayName() {
        return Component.translatable(KEY + this.id);
    }

    /**
     * The line under the name: what it does. Only a construct that is {@link #made} has one; the rest show their name
     * alone.
     */
    @Nullable
    public Component getDescription() {
        return this.made() ? Component.translatable(KEY + this.id + ".about") : null;
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
