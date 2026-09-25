package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;

public final class GreenLanternPowers implements CharacterPowers {
    @Override
    public void enter(ServerPlayer player) {
        Arrival.begin(player);
    }

    @Override
    public void leave(ServerPlayer player) {
        Arrival.end(player);
    }

    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        return PowerRing.use(player, ability, on, data);
    }

    @Override
    public int ultimateLeft(ServerPlayer player) {
        return AirStrike.left(player);
    }

    @Override
    public void showTo(ServerPlayer viewer, ServerPlayer target) {
        PowerRing.showTo(viewer, target);
    }

    @Override
    public void clear() {
        PowerRing.clear();
    }
}
