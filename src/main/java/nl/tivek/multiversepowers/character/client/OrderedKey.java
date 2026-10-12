package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

// One of the mod's keys in Options > Controls, all in one list in the order of their numbers (Ability 1 to 23, then the
// guide, the power wheel and the update manager); the game itself would sort them by name, Ability 10 before 2.
public class OrderedKey extends KeyMapping {
    private final int order;

    public OrderedKey(String name, InputConstants.Type type, int code, int order) {
        super(name, KeyConflictContext.IN_GAME, type, code, AbilityKeys.CATEGORY);
        this.order = order;
    }

    public int order() {
        return this.order;
    }

    @Override
    public int compareTo(KeyMapping other) {
        return other instanceof OrderedKey ordered ? Integer.compare(this.order, ordered.order)
                : super.compareTo(other);
    }
}
