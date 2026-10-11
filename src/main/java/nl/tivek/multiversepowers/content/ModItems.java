package nl.tivek.multiversepowers.content;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import nl.tivek.multiversepowers.character.greenlantern.summon.HardLight;

// The mod's items and its own creative tab, which lists every block, creature and item it adds.
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MultiversePowers.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            MultiversePowers.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> HELPER_EGG = ITEMS.register("mech_helper_spawn_egg",
            () -> new DeferredSpawnEggItem(MechMinion.TYPE, 0x0E5A2A, 0x6CFF8E, new Item.Properties()));

    // The hard-light creatures' eggs, a dark green shell spotted in each creature's own colour.
    private static final int HARD_LIGHT = 0x0E5A2A;
    public static final List<DeferredItem<DeferredSpawnEggItem>> HARD_LIGHT_EGGS = List.of(
            egg("hard_light_skeleton", HardLight.SKELETON, 0xCFE8D4),
            egg("hard_light_zombified_piglin", HardLight.PIGLIN, 0xEA9393),
            egg("hard_light_creeper", HardLight.CREEPER, 0x3FD95A),
            egg("hard_light_blaze", HardLight.BLAZE, 0xFFF87E),
            egg("hard_light_vindicator", HardLight.VINDICATOR, 0x959B9B),
            egg("hard_light_enderman", HardLight.ENDERMAN, 0x151515),
            egg("hard_light_iron_golem", HardLight.GOLEM, 0xD8CDBC));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("multiverse_powers",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + MultiversePowers.MODID))
                    .icon(() -> new ItemStack(HELPER_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HELPER_EGG.get());
                        HARD_LIGHT_EGGS.forEach(egg -> output.accept(egg.get()));
                    }).build());

    private static DeferredItem<DeferredSpawnEggItem> egg(String name,
            DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> type, int spots) {
        return ITEMS.register(name + "_spawn_egg",
                () -> new DeferredSpawnEggItem(type, HARD_LIGHT, spots, new Item.Properties()));
    }

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener(ModItems::onTabs);
    }

    // The spawn eggs also sit with the game's own spawn eggs.
    private static void onTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(HELPER_EGG.get());
            HARD_LIGHT_EGGS.forEach(egg -> event.accept(egg.get()));
        }
    }
}
