package nl.tivek.welcomescreen.server;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import nl.tivek.welcomescreen.classes.ClassLoadouts;
import nl.tivek.welcomescreen.classes.Loadout;
import nl.tivek.welcomescreen.classes.PlayerClass;

/**
 * Puts a class loadout on a player and fires the matching spawn effect.
 */
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

        ClassEffects.play(player, playerClass, true);
    }
}
