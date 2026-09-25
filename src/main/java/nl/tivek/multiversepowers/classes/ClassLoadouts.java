package nl.tivek.multiversepowers.classes;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ClassLoadouts {
    private ClassLoadouts() {
    }

    public static Loadout build(PlayerClass playerClass) {
        return new Loadout(Map.of(), List.of(new ItemStack(Items.BREAD)));
    }
}
