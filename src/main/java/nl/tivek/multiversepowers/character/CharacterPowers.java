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

    // Ticks an ability still waits on a cooldown of its own, apart from its slot's, sent along for the panel.
    default int waitLeft(ServerPlayer player, CharacterAbility ability) {
        return 0;
    }

    default void showTo(ServerPlayer viewer, ServerPlayer target) {
    }

    // A power knocked the player down (PlayerKnockdowns): what keeps them up or holds something ends.
    default void knockedDown(ServerPlayer player) {
    }

    void clear();
}
