package nl.tivek.multiversepowers.character;

import net.minecraft.server.level.ServerPlayer;

public interface CharacterPowers {
    // The powers of a character that cannot be played yet: none.
    CharacterPowers NONE = new CharacterPowers() {
        @Override
        public void enter(ServerPlayer player) {
        }

        @Override
        public void leave(ServerPlayer player) {
        }

        @Override
        public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
            return false;
        }

        @Override
        public void clear() {
        }
    };

    void enter(ServerPlayer player);

    void leave(ServerPlayer player);

    boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data);

    default int ultimateLeft(ServerPlayer player) {
        return 0;
    }

    // Ticks an ability still waits on a cooldown of its own, apart from its slot's, sent along for the panel.
    default int waitLeft(ServerPlayer player, CharacterAbility ability) {
        return 0;
    }

    // While a power takes over every key (Green Lantern's mech), the slots' own cooldowns neither refuse a key nor
    // show: that power keeps its waits itself (waitLeft).
    default boolean ownsKeys(ServerPlayer player) {
        return false;
    }

    // A press that now only ends what its key keeps up (Green Lantern's dome) is never held back by the key's cooldown.
    default boolean endsOnly(ServerPlayer player, CharacterAbility ability) {
        return false;
    }

    default void showTo(ServerPlayer viewer, ServerPlayer target) {
    }

    // A power knocked the player down (PlayerKnockdowns): what keeps them up or holds something ends.
    default void knockedDown(ServerPlayer player) {
    }

    // Whether a power of theirs holds the player up in the air, asked as a knockdown ends it.
    default boolean flying(ServerPlayer player) {
        return false;
    }

    // Let go of a knockdown still in the air after it ended their flight: they fly on, free.
    default void flyAgain(ServerPlayer player) {
    }

    void clear();
}
