package nl.tivek.multiversepowers.character.greenlantern;

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
    FLAMETHROWER("flamethrower");

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

    public boolean made() {
        return this == NONE || this == SWORD_SHIELD || this == ENERGY_WHIP || this == FLAMETHROWER;
    }

    public Component getDisplayName() {
        return Component.translatable(KEY + this.id);
    }

    @Nullable
    public Component getDescription() {
        return this.made() ? Component.translatable(KEY + this.id + ".about") : null;
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
