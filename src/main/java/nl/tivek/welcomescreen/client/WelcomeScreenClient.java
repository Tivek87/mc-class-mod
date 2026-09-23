package nl.tivek.welcomescreen.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.client.character.lantern.GreenLanternSuitLayer;
import nl.tivek.welcomescreen.client.config.SettingsScreen;

/**
 * Client-only start of the mod. Kept apart from WelcomeScreenMod, because a dedicated server cannot
 * load anything that mentions screens.
 */
@Mod(value = WelcomeScreenMod.MODID, dist = Dist.CLIENT)
public final class WelcomeScreenClient {
    public WelcomeScreenClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new SettingsScreen(parent));
        modEventBus.addListener(GreenLanternSuitLayer::onAddLayers);
    }
}
