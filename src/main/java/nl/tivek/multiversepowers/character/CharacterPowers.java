package nl.tivek.multiversepowers.character;

import net.minecraft.server.level.ServerPlayer;

public interface CharacterPowers {
    void enter(ServerPlayer player);

    void leave(ServerPlayer player);

    boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data);

    default int ultimateLeft(ServerPlayer player) {
        return 0;
    }

    default int stance(ServerPlayer player) {
        return 0;
    }

    default int marks(ServerPlayer player) {
        return 0;
    }

    default void showTo(ServerPlayer viewer, ServerPlayer target) {
    }

    void clear();
}
