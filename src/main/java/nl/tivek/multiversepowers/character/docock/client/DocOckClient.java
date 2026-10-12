package nl.tivek.multiversepowers.character.docock.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.Gestures;

// Doctor Octopus in this game: he reads the mouse, space and shift himself (OctoControls), his tentacles show in his
// crosshair (OctoCrosshair) and the ability guide knows his modes (OctoGuide).
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class DocOckClient {
    public DocOckClient() {
        Gestures.ownInput(GameCharacter.DOC_OCK);
        OctoGuide.register();
    }
}
