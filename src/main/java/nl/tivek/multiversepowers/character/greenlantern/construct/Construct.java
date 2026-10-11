package nl.tivek.multiversepowers.character.greenlantern.construct;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

public enum Construct {
    NONE("none"),
    SWORD_SHIELD("sword_shield"),
    ENERGY_WHIP("energy_whip"),
    BATTLEAXE("battleaxe"),
    CHAINSAW("chainsaw"),
    REVOLVERS("revolvers"),
    SHOTGUN("shotgun"),
    ARM_CANNON("arm_cannon"),
    MINIGUN("minigun"),
    ROCKET_LAUNCHER("rocket_launcher"),
    FLAMETHROWER("flamethrower"),
    SUMMON("summon");

    private static final String KEY = "construct." + MultiversePowers.MODID + ".";

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

    // Fights from afar: the only weapons the wheel forms in flight.
    public boolean ranged() {
        return switch (this) {
            case REVOLVERS, SHOTGUN, ARM_CANNON, MINIGUN, ROCKET_LAUNCHER, FLAMETHROWER -> true;
            default -> false;
        };
    }

    // Whether the wheel forms it now: never while it is shut, and in flight only what fights from afar (or summons).
    public boolean shut(boolean flying) {
        return this.locked() || flying && this != NONE && this != SUMMON && !this.ranged();
    }

    // Not held: picking it forms creatures that fight for him, and whatever he holds stays in his hands.
    public boolean summons() {
        return this == SUMMON;
    }

    // Shut for now: the energy whip is to be reworked or taken out (too glitchy, too heavy to draw).
    public boolean locked() {
        return this == ENERGY_WHIP;
    }

    public Component getDisplayName() {
        return Component.translatable(KEY + this.id);
    }

    public Component getDescription() {
        if (this.locked()) {
            return Component.translatable(KEY + "locked");
        }
        return Component.translatable(KEY + this.id + ".about");
    }

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
