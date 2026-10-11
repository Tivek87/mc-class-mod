package nl.tivek.multiversepowers.content;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.greenlantern.summon.HardLight;
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;
import nl.tivek.multiversepowers.creature.lurker.Lurker;
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.creature.screamer.Feral;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.ritual.ArcaneTomeItem;
import nl.tivek.multiversepowers.schematic.SchematicWandItem;
import nl.tivek.multiversepowers.stage.StageBlocks;

// The mod's items and its own creative tab, which lists every block, creature and item it adds.
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MultiversePowers.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            MultiversePowers.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> HELPER_EGG = ITEMS.register("mech_helper_spawn_egg",
            () -> new DeferredSpawnEggItem(MechMinion.TYPE, 0x0E5A2A, 0x6CFF8E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BRUTE_EGG = ITEMS.register("zombie_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(ZombieBrute.TYPE, 0x3E5A34, 0x8A2E24, new Item.Properties()));
    public static final List<DeferredItem<DeferredSpawnEggItem>> MONSTER_EGGS = List.of(
            ITEMS.register("bloater_spawn_egg",
                    () -> new DeferredSpawnEggItem(Bloater.TYPE, 0x5E7A2E, 0xB6E14A, new Item.Properties())),
            ITEMS.register("lurker_spawn_egg",
                    () -> new DeferredSpawnEggItem(Lurker.TYPE, 0x1C1A1A, 0xE8E8E8, new Item.Properties())),
            ITEMS.register("screamer_spawn_egg",
                    () -> new DeferredSpawnEggItem(Screamer.TYPE, 0x6E6658, 0x8A1F1F, new Item.Properties())),
            ITEMS.register("feral_spawn_egg",
                    () -> new DeferredSpawnEggItem(Feral.TYPE, 0x4A5A3C, 0xC9C29F, new Item.Properties())),
            ITEMS.register("rustbound_spawn_egg",
                    () -> new DeferredSpawnEggItem(Rustbound.TYPE, 0x7A3E1E, 0x7FC8FF, new Item.Properties())),
            ITEMS.register("stalker_spawn_egg",
                    () -> new DeferredSpawnEggItem(Stalker.TYPE, 0x2B2622, 0xD8F2E4, new Item.Properties())),
            ITEMS.register("wizard_spawn_egg",
                    () -> new DeferredSpawnEggItem(WizardEntities.WIZARD, 0x2C2868, 0xD6AC42, new Item.Properties())),
            ITEMS.register("arcane_familiar_spawn_egg",
                    () -> new DeferredSpawnEggItem(WizardEntities.FAMILIAR, 0x3A1E66, 0xC9A2FF, new Item.Properties())));

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

    public static final List<DeferredItem<BlockItem>> LAMPS = StageBlocks.LAMPS.values().stream()
            .map(ITEMS::registerSimpleBlockItem).toList();

    public static final DeferredItem<ArcaneTomeItem> ARCANE_TOME = ITEMS.register("arcane_tome",
            () -> new ArcaneTomeItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<SchematicWandItem> SCHEMATIC_WAND = ITEMS.register("schematic_wand",
            () -> new SchematicWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("multiverse_powers",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + MultiversePowers.MODID))
                    .icon(() -> new ItemStack(HELPER_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HELPER_EGG.get());
                        output.accept(BRUTE_EGG.get());
                        MONSTER_EGGS.forEach(egg -> output.accept(egg.get()));
                        HARD_LIGHT_EGGS.forEach(egg -> output.accept(egg.get()));
                        LAMPS.forEach(lamp -> output.accept(lamp.get()));
                        output.accept(ARCANE_TOME.get());
                        output.accept(SCHEMATIC_WAND.get());
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
            event.accept(BRUTE_EGG.get());
            MONSTER_EGGS.forEach(egg -> event.accept(egg.get()));
            HARD_LIGHT_EGGS.forEach(egg -> event.accept(egg.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ARCANE_TOME.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            LAMPS.forEach(lamp -> event.accept(lamp.get()));
        }
        // An operator's tool, with the structure and command blocks.
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions()) {
            event.accept(SCHEMATIC_WAND.get());
        }
    }
}
