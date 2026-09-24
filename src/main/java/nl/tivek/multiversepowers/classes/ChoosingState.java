package nl.tivek.multiversepowers.classes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Keeps a player out of the world while they pick a class: spectator mode makes them invisible
 * to other players, untouchable for mobs and immune to damage. The game mode they had before is
 * stored on the player, in the part of their saved data the game carries over when they respawn,
 * so it survives a disconnect or a death halfway through choosing.
 */
public final class ChoosingState {
    private static final String PREVIOUS_MODE_KEY = MultiversePowers.MODID + ":mode_before_choosing";

    private ChoosingState() {
    }

    public static void enter(ServerPlayer player) {
        CompoundTag data = saved(player);
        // Already hidden from an earlier login or life: keep the mode from the very first time.
        if (!data.contains(PREVIOUS_MODE_KEY)) {
            data.putString(PREVIOUS_MODE_KEY, player.gameMode.getGameModeForPlayer().getName());
        }
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void leave(ServerPlayer player) {
        CompoundTag data = saved(player);
        GameType fallback = player.server.getDefaultGameType();
        GameType previous = data.contains(PREVIOUS_MODE_KEY)
                ? GameType.byName(data.getString(PREVIOUS_MODE_KEY), fallback)
                : fallback;
        data.remove(PREVIOUS_MODE_KEY);
        player.setGameMode(previous);
    }

    /**
     * The part of the player's saved data that the game carries over when they respawn. A mode stored by an older
     * version outside it moves in the first time it is needed.
     */
    private static CompoundTag saved(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (data.contains(PREVIOUS_MODE_KEY)) {
            if (!persisted.contains(PREVIOUS_MODE_KEY)) {
                persisted.putString(PREVIOUS_MODE_KEY, data.getString(PREVIOUS_MODE_KEY));
            }
            data.remove(PREVIOUS_MODE_KEY);
        }
        return persisted;
    }
}
