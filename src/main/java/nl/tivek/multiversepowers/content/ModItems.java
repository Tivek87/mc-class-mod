package nl.tivek.multiversepowers.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;

// The mod's items and its own creative tab, which lists every block, creature and item it adds.
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MultiversePowers.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            MultiversePowers.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> HELPER_EGG = ITEMS.register("mech_helper_spawn_egg",
            () -> new DeferredSpawnEggItem(MechMinion.TYPE, 0x0E5A2A, 0x6CFF8E, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("multiverse_powers",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + MultiversePowers.MODID))
                    .icon(() -> new ItemStack(HELPER_EGG.get()))
                    .displayItems((parameters, output) -> output.accept(HELPER_EGG.get())).build());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener(ModItems::onTabs);
    }

    // The spawn egg also sits with the game's own spawn eggs.
    private static void onTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(HELPER_EGG.get());
        }
    }
}
