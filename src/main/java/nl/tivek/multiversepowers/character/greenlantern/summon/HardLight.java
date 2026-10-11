package nl.tivek.multiversepowers.character.greenlantern.summon;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;

// The hard-light creatures: each the game's own creature with all its moves, made of the ring's light (drawn so on
// every game, `client/HardLightLayer`). Out of the ring they fight for a Green Lantern (`Summons`); out of a spawn egg
// they live as the game's own do. How much health each has is a setting, given as it first comes into its world.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class HardLight {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Skeleton>> SKELETON = type("hard_light_skeleton",
            Skeleton::new, EntityType.SKELETON);
    public static final DeferredHolder<EntityType<?>, EntityType<ZombifiedPiglin>> PIGLIN = type(
            "hard_light_zombified_piglin", ZombifiedPiglin::new, EntityType.ZOMBIFIED_PIGLIN);
    public static final DeferredHolder<EntityType<?>, EntityType<Creeper>> CREEPER = type("hard_light_creeper",
            Creeper::new, EntityType.CREEPER);
    public static final DeferredHolder<EntityType<?>, EntityType<Blaze>> BLAZE = type("hard_light_blaze", Blaze::new,
            EntityType.BLAZE);
    public static final DeferredHolder<EntityType<?>, EntityType<Vindicator>> VINDICATOR = type(
            "hard_light_vindicator", Vindicator::new, EntityType.VINDICATOR);
    public static final DeferredHolder<EntityType<?>, EntityType<EnderMan>> ENDERMAN = type("hard_light_enderman",
            EnderMan::new, EntityType.ENDERMAN);
    public static final DeferredHolder<EntityType<?>, EntityType<IronGolem>> GOLEM = type("hard_light_iron_golem",
            IronGolem::new, EntityType.IRON_GOLEM);
    // Every kind in the order the settings list them, each with its health setting.
    public static final List<DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>>> ALL = List.of(SKELETON,
            PIGLIN, CREEPER, BLAZE, VINDICATOR, ENDERMAN, GOLEM);
    private static final String[] HEALTH = { "summonSkeletonHealth", "summonPiglinHealth", "summonCreeperHealth",
            "summonBlazeHealth", "summonVindicatorHealth", "summonEndermanHealth", "summonGolemHealth" };
    private static final String GIVEN = MultiversePowers.MODID + ":hard_light_health";

    private HardLight() {
    }

    // The game's own creature's size, fire and how far it is seen.
    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> type(String name,
            EntityType.EntityFactory<T> factory, EntityType<?> like) {
        return TYPES.register(name, () -> {
            EntityDimensions size = like.getDimensions();
            EntityType.Builder<T> builder = EntityType.Builder.of(factory, like.getCategory())
                    .sized(size.width(), size.height()).eyeHeight(size.eyeHeight())
                    .clientTrackingRange(like.clientTrackingRange());
            if (like.fireImmune()) {
                builder.fireImmune();
            }
            return builder.build(name);
        });
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(HardLight::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SKELETON.get(), AbstractSkeleton.createAttributes().build());
        event.put(PIGLIN.get(), ZombifiedPiglin.createAttributes().build());
        event.put(CREEPER.get(), Creeper.createAttributes().build());
        event.put(BLAZE.get(), Blaze.createAttributes().build());
        event.put(VINDICATOR.get(), Vindicator.createAttributes().build());
        event.put(ENDERMAN.get(), EnderMan.createAttributes().build());
        event.put(GOLEM.get(), IronGolem.createAttributes().build());
    }

    public static boolean is(EntityType<?> type) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> kind : ALL) {
            if (kind.get() == type) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)
                || mob.getPersistentData().getBoolean(GIVEN)) {
            return;
        }
        for (int k = 0; k < ALL.size(); k++) {
            if (ALL.get(k).get() == mob.getType()) {
                AttributeInstance most = mob.getAttribute(Attributes.MAX_HEALTH);
                if (most != null) {
                    most.setBaseValue(Math.max(1.0, GameCharacter.GREEN_LANTERN.byName("construct_wheel")
                            .value(HEALTH[k])));
                    mob.setHealth(mob.getMaxHealth());
                }
                mob.getPersistentData().putBoolean(GIVEN, true);
                return;
            }
        }
    }
}
