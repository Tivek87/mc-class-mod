package nl.tivek.multiversepowers.stage;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

// The stage lights: a block per kind, all sharing one block entity that holds each lamp's own settings.
public final class StageBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MultiversePowers.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, MultiversePowers.MODID);

    public static final Map<LampKind, DeferredBlock<StageLightBlock>> LAMPS = new EnumMap<>(LampKind.class);

    static {
        for (LampKind kind : LampKind.values()) {
            LAMPS.put(kind, BLOCKS.register(kind.id(), () -> new StageLightBlock(kind, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(state -> state.getValue(StageLightBlock.LIGHT)))));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StageLightBlockEntity>> LAMP =
            BLOCK_ENTITIES.register("stage_light", () -> BlockEntityType.Builder.of(StageLightBlockEntity::new,
                    LAMPS.values().stream().map(DeferredBlock::get).toArray(StageLightBlock[]::new)).build(null));

    private StageBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
