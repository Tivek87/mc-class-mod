package nl.tivek.multiversepowers.engine.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.tivek.multiversepowers.config.PowerRules;

public final class BlockRules {
    private BlockRules() {
    }

    // Mirrors vanilla's own break checks, so a power never breaks what its owner could not.
    public static boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!PowerRules.breakBlocks() || !level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
