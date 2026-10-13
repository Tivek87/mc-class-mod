package nl.tivek.multiversepowers.content;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
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
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;
import nl.tivek.multiversepowers.creature.animal.duck.Duck;
import nl.tivek.multiversepowers.creature.animal.elephant.Elephant;
import nl.tivek.multiversepowers.creature.animal.giraffe.Giraffe;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;
import nl.tivek.multiversepowers.creature.animal.monkey.Monkey;
import nl.tivek.multiversepowers.creature.animal.moose.Moose;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementRod;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.creature.elemental.ElementalStaff;
import nl.tivek.multiversepowers.creature.lurker.Lurker;
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.creature.screamer.Feral;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.ritual.ArcaneTomeItem;
import nl.tivek.multiversepowers.faction.FactionBookItem;
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
            ITEMS.register("elemental_blaze_spawn_egg",
                    () -> new DeferredSpawnEggItem(ElementalBlaze.TYPE, 0xE8A33A, 0x7FD6FF, new Item.Properties())),
            ITEMS.register("wizard_spawn_egg",
                    () -> new DeferredSpawnEggItem(WizardEntities.WIZARD, 0x2C2868, 0xD6AC42, new Item.Properties())),
            ITEMS.register("arcane_familiar_spawn_egg",
                    () -> new DeferredSpawnEggItem(WizardEntities.FAMILIAR, 0x3A1E66, 0xC9A2FF, new Item.Properties())));

    public static final List<DeferredItem<DeferredSpawnEggItem>> ANIMAL_EGGS = List.of(
            ITEMS.register("brown_bear_spawn_egg",
                    () -> new DeferredSpawnEggItem(BrownBear.TYPE, 0x5C3D24, 0x2A1A10, new Item.Properties())),
            ITEMS.register("deer_spawn_egg",
                    () -> new DeferredSpawnEggItem(Deer.TYPE, 0x9A6436, 0xF1E9DA, new Item.Properties())),
            ITEMS.register("raccoon_spawn_egg",
                    () -> new DeferredSpawnEggItem(Raccoon.TYPE, 0x86807A, 0x1C1A19, new Item.Properties())),
            ITEMS.register("hedgehog_spawn_egg",
                    () -> new DeferredSpawnEggItem(Hedgehog.TYPE, 0x6B5A48, 0xD9C9A8, new Item.Properties())),
            ITEMS.register("owl_spawn_egg",
                    () -> new DeferredSpawnEggItem(Owl.TYPE, 0x7A5A3A, 0xE8DCC0, new Item.Properties())),
            ITEMS.register("squirrel_spawn_egg",
                    () -> new DeferredSpawnEggItem(Squirrel.TYPE, 0xB5562A, 0xF2EADC, new Item.Properties())),
            ITEMS.register("snake_spawn_egg",
                    () -> new DeferredSpawnEggItem(Snake.TYPE, 0x5E6B34, 0x1E1C16, new Item.Properties())),
            ITEMS.register("crab_spawn_egg",
                    () -> new DeferredSpawnEggItem(Crab.TYPE, 0xA8452A, 0xE9D2A6, new Item.Properties())),
            ITEMS.register("moose_spawn_egg",
                    () -> new DeferredSpawnEggItem(Moose.TYPE, 0x3D2B1E, 0xB9A68C, new Item.Properties())),
            ITEMS.register("lynx_spawn_egg",
                    () -> new DeferredSpawnEggItem(Lynx.TYPE, 0xB98A52, 0x2A211A, new Item.Properties())),
            ITEMS.register("monkey_spawn_egg",
                    () -> new DeferredSpawnEggItem(Monkey.TYPE, 0x221F1E, 0xEAD8AC, new Item.Properties())),
            ITEMS.register("elephant_spawn_egg",
                    () -> new DeferredSpawnEggItem(Elephant.TYPE, 0x807872, 0xECE2C4, new Item.Properties())),
            ITEMS.register("giraffe_spawn_egg",
                    () -> new DeferredSpawnEggItem(Giraffe.TYPE, 0xECD4A2, 0xA4602E, new Item.Properties())),
            ITEMS.register("duck_spawn_egg",
                    () -> new DeferredSpawnEggItem(Duck.TYPE, 0x8A6A48, 0x1F6A3A, new Item.Properties())),
            ITEMS.register("crocodile_spawn_egg",
                    () -> new DeferredSpawnEggItem(Crocodile.TYPE, 0x4A5632, 0xC9BE84, new Item.Properties())));

    // The wild animals' meat: venison from deer and moose, bear meat from bears, which raw may make you hungry as
    // raw chicken does, crab, and duck.
    public static final DeferredItem<Item> RAW_VENISON = ITEMS.registerSimpleItem("raw_venison",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_VENISON = ITEMS.registerSimpleItem("cooked_venison",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> RAW_BEAR_MEAT = ITEMS.registerSimpleItem("raw_bear_meat",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build()));
    public static final DeferredItem<Item> COOKED_BEAR_MEAT = ITEMS.registerSimpleItem("cooked_bear_meat",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> RAW_CRAB = ITEMS.registerSimpleItem("raw_crab",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()));
    public static final DeferredItem<Item> COOKED_CRAB = ITEMS.registerSimpleItem("cooked_crab",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()));
    public static final DeferredItem<Item> RAW_DUCK = ITEMS.registerSimpleItem("raw_duck",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build()));
    public static final DeferredItem<Item> COOKED_DUCK = ITEMS.registerSimpleItem("cooked_duck",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build()));
    public static final List<DeferredItem<Item>> MEATS = List.of(RAW_VENISON, COOKED_VENISON, RAW_BEAR_MEAT,
            COOKED_BEAR_MEAT, RAW_CRAB, COOKED_CRAB, RAW_DUCK, COOKED_DUCK);

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

    // The Elemental Blaze's rods, one per element, and the staff all five make.
    public static final List<DeferredItem<ElementRod>> ELEMENT_RODS = Arrays.stream(Element.ALL)
            .map(element -> ITEMS.register(element.rod, () -> new ElementRod(element, new Item.Properties()
                    .rarity(Rarity.RARE).fireResistant()))).toList();
    public static final DeferredItem<ElementalStaff> ELEMENTAL_STAFF = ITEMS.register("elemental_staff",
            () -> new ElementalStaff(new Item.Properties().stacksTo(1).durability(450).rarity(Rarity.EPIC)
                    .fireResistant()));

    public static final DeferredItem<FactionBookItem> FACTION_BOOK = ITEMS.register("faction_book",
            () -> new FactionBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<SchematicWandItem> SCHEMATIC_WAND = ITEMS.register("schematic_wand",
            () -> new SchematicWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("multiverse_powers",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup." + MultiversePowers.MODID))
                    .icon(() -> new ItemStack(HELPER_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(HELPER_EGG.get());
                        output.accept(BRUTE_EGG.get());
                        MONSTER_EGGS.forEach(egg -> output.accept(egg.get()));
                        ANIMAL_EGGS.forEach(egg -> output.accept(egg.get()));
                        MEATS.forEach(meat -> output.accept(meat.get()));
                        HARD_LIGHT_EGGS.forEach(egg -> output.accept(egg.get()));
                        LAMPS.forEach(lamp -> output.accept(lamp.get()));
                        output.accept(ARCANE_TOME.get());
                        ELEMENT_RODS.forEach(rod -> output.accept(rod.get()));
                        output.accept(ELEMENTAL_STAFF.get());
                        output.accept(FACTION_BOOK.get());
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
            ANIMAL_EGGS.forEach(egg -> event.accept(egg.get()));
            HARD_LIGHT_EGGS.forEach(egg -> event.accept(egg.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            MEATS.forEach(meat -> event.accept(meat.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ELEMENT_RODS.forEach(rod -> event.accept(rod.get()));
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ELEMENTAL_STAFF.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ARCANE_TOME.get());
            event.accept(FACTION_BOOK.get());
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
