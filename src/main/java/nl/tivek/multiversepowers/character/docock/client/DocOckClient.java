package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.Gestures;

// Doctor Octopus in this game: he reads the mouse, space and shift himself (OctoControls), his tentacles show round the
// crosshair (OctoHud) and the ability guide knows his modes (OctoGuide).
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class DocOckClient {
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "octo_tentacles");

    public DocOckClient(IEventBus modEventBus) {
        modEventBus.addListener(DocOckClient::onRegisterLayers);
        Gestures.ownInput(GameCharacter.DOC_OCK);
        OctoGuide.register();
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, HUD, OctoHud::render);
    }
}
