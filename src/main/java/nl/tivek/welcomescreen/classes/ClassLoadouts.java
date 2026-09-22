package nl.tivek.welcomescreen.classes;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the starting gear for a class. Shared by both sides, so the selection screen shows
 * exactly what the server will hand out.
 */
public final class ClassLoadouts {
    private ClassLoadouts() {
    }

    // For now every class starts with the same single bread; class-specific gear comes later.
    public static Loadout build(PlayerClass playerClass) {
        return new Loadout(Map.of(), List.of(new ItemStack(Items.BREAD)));
    }
}
