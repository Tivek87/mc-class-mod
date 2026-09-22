package nl.tivek.welcomescreen.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The gear that belongs to one class. Built the same way on both sides, so the
 * selection screen can show exactly what the server will hand out.
 *
 * @param equipment worn items, per slot
 * @param inventory items that go into the backpack
 */
public record Loadout(Map<EquipmentSlot, ItemStack> equipment, List<ItemStack> inventory) {
    /** Every item of this loadout, worn gear first, for showing on the selection screen. */
    public List<ItemStack> allItems() {
        List<ItemStack> items = new ArrayList<>(this.equipment.values());
        items.addAll(this.inventory);
        return items;
    }
}
