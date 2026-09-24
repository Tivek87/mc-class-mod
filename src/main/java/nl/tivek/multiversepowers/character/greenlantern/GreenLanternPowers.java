package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;

/** Green Lantern for the character system: his power ring (see {@link PowerRing}). */
public final class GreenLanternPowers implements CharacterPowers {
    /** The ring comes for him from afar and dresses him in the uniform, with light and sound of its own. */
    @Override
    public void enter(ServerPlayer player) {
        Arrival.begin(player);
    }

    /**
     * His constructs fall apart by themselves once he is no longer Green Lantern; so does a ring still on its way to
     * him.
     */
    @Override
    public void leave(ServerPlayer player) {
        Arrival.end(player);
    }

    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        return PowerRing.use(player, ability, on, data);
    }

    /** His ultimate is the Air Strike. */
    @Override
    public int ultimateLeft(ServerPlayer player) {
        return AirStrike.left(player);
    }

    /** They learn how brightly his ring glows and what he does with it. */
    @Override
    public void showTo(ServerPlayer viewer, ServerPlayer target) {
        PowerRing.showTo(viewer, target);
    }

    @Override
    public void clear() {
        PowerRing.clear();
    }
}
