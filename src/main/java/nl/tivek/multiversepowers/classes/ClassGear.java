package nl.tivek.multiversepowers.classes;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies;

public final class ClassGear {
    private ClassGear() {
    }

    public static void apply(ServerPlayer player, PlayerClass playerClass) {
        Loadout loadout = ClassLoadouts.build(playerClass);

        for (Map.Entry<EquipmentSlot, ItemStack> entry : loadout.equipment().entrySet()) {
            player.setItemSlot(entry.getKey(), entry.getValue().copy());
        }
        for (ItemStack stack : loadout.inventory()) {
            player.getInventory().add(stack.copy());
        }
        player.inventoryMenu.broadcastChanges();

        Ceremonies.play(player, playerClass, true);
    }
}
