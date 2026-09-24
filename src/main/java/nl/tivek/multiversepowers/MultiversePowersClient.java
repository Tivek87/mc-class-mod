package nl.tivek.multiversepowers;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.tivek.multiversepowers.character.greenlantern.client.body.GreenLanternSuitLayer;
import nl.tivek.multiversepowers.config.client.SettingsScreen;

/**
 * Client-only start of the mod. Kept apart from MultiversePowers, because a dedicated server cannot
 * load anything that mentions screens.
 */
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class MultiversePowersClient {
    public MultiversePowersClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new SettingsScreen(parent));
        modEventBus.addListener(GreenLanternSuitLayer::onAddLayers);
    }
}
