package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightDome;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.mech.MechAssembly;

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
    public int waitLeft(ServerPlayer player, CharacterAbility ability) {
        return ability.id().equals("mech") ? MechAssembly.waitLeft(player) : 0;
    }

    @Override
    public void showTo(ServerPlayer viewer, ServerPlayer target) {
        PowerRing.showTo(viewer, target);
    }

    @Override
    public void knockedDown(ServerPlayer player) {
        Flight.stop(player);
        LightShield.stop(player);
        LightDome.lower(player);
        LightBeam.stop(player);
    }

    @Override
    public boolean flying(ServerPlayer player) {
        return Flight.flying(player);
    }

    @Override
    public void flyAgain(ServerPlayer player) {
        Flight.resume(player);
    }

    @Override
    public void clear() {
        PowerRing.clear();
    }
}
