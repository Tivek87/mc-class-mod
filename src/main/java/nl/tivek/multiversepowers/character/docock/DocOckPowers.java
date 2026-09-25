package nl.tivek.multiversepowers.character.docock;

import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.Characters;

public final class DocOckPowers implements CharacterPowers {
    @Override
    public void enter(ServerPlayer player) {
        Characters.transformFlash(player, 0.9F);
        OctopusArms.armsOut(player, player.serverLevel());
    }

    @Override
    public void leave(ServerPlayer player) {
        OctopusArms.armsIn(player);
    }

    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        return OctopusArms.use(player, ability, on, data);
    }

    @Override
    public int ultimateLeft(ServerPlayer player) {
        return OctopusArms.ultimateLeft(player);
    }

    @Override
    public int stance(ServerPlayer player) {
        return OctopusArms.legCount(player);
    }

    @Override
    public int marks(ServerPlayer player) {
        return OctopusArms.markCount(player);
    }

    @Override
    public void clear() {
        OctopusArms.clear();
    }
}
