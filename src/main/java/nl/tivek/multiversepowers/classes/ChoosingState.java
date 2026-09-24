package nl.tivek.multiversepowers.classes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Keeps a player out of the world while they pick a class: spectator mode makes them invisible
 * to other players, untouchable for mobs and immune to damage. The game mode they had before is
 * stored on the player, so it survives a disconnect halfway through choosing.
 */
public final class ChoosingState {
    private static final String PREVIOUS_MODE_KEY = MultiversePowers.MODID + ":mode_before_choosing";

    private ChoosingState() {
    }

    public static void enter(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        // Already hidden from an earlier login: keep the mode from the very first time.
        if (!data.contains(PREVIOUS_MODE_KEY)) {
            data.putString(PREVIOUS_MODE_KEY, player.gameMode.getGameModeForPlayer().getName());
        }
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void leave(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        GameType fallback = player.server.getDefaultGameType();
        GameType previous = data.contains(PREVIOUS_MODE_KEY)
                ? GameType.byName(data.getString(PREVIOUS_MODE_KEY), fallback)
                : fallback;
        data.remove(PREVIOUS_MODE_KEY);
        player.setGameMode(previous);
    }
}
