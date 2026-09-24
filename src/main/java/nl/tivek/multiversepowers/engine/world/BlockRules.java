package nl.tivek.multiversepowers.engine.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Whether a power may break a block for a player, asked the way the game asks it when he breaks one by hand: spawn
 * protection and the world border, adventure mode, and every mod that guards land (claims, protected areas) through
 * the game's block break event. On a server a power then never breaks what its owner could not break himself.
 */
public final class BlockRules {
    private BlockRules() {
    }

    /** True when {@code player}'s power may break this block (in this state) here. */
    public static boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
}
