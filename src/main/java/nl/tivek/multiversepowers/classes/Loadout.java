package nl.tivek.multiversepowers.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record Loadout(Map<EquipmentSlot, ItemStack> equipment, List<ItemStack> inventory) {
    public List<ItemStack> allItems() {
        List<ItemStack> items = new ArrayList<>(this.equipment.values());
        items.addAll(this.inventory);
        return items;
    }
}
