package nl.tivek.multiversepowers.character;

import net.minecraft.server.level.ServerPlayer;

/**
 * What one character's own code does, for the character system ({@link Characters}). Everything else is the same for
 * every character and done there once: who is who, the ability keys and their cooldowns, the panel, and turning back
 * on death or logout. A new character is one entry in {@link GameCharacter} (with its abilities and their numbers)
 * and one of these.
 */
public interface CharacterPowers {
    /** The player just became this character: bring out what they have (four arms, a ring on its way...). */
    void enter(ServerPlayer player);

    /** The player stops being this character (another pick, dying, logging out): put it all away again. */
    void leave(ServerPlayer player);

    /**
     * One of this character's abilities, whichever key slot it sits in. The character system has already looked up
     * which ability the key means and checked its cooldown.
     *
     * @param on   for a key you hold down: true as it goes down, false as it comes up again
     * @param data the bits a key press carries (see {@link Characters#SNEAKING} and the ones after it)
     * @return true when the ability really ran, so its cooldown should start
     */
    boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data);

    /** Ticks left of this character's ultimate while it runs (the panel counts it down), 0 while none runs. */
    default int ultimateLeft(ServerPlayer player) {
        return 0;
    }

    /**
     * How the character stands or moves right now, as a number its own client understands (Doctor Octopus: how many
     * tentacles he walks on); 0 when that means nothing for this character.
     */
    default int stance(ServerPlayer player) {
        return 0;
    }

    /** How many creatures the player has marked for one of their abilities (Doctor Octopus's Ground Strike). */
    default int marks(ServerPlayer player) {
        return 0;
    }

    /** Someone starts seeing a player who is this character: tell them what they need to draw him. */
    default void showTo(ServerPlayer viewer, ServerPlayer target) {
    }

    /** The server stops: forget everything, for every player. */
    void clear();
}
