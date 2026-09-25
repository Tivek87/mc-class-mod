package nl.tivek.multiversepowers;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.tivek.multiversepowers.character.greenlantern.client.body.GreenLanternSuitLayer;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.update.client.UpdatePopup;

@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class MultiversePowersClient {
    public MultiversePowersClient(ModContainer container, IEventBus modEventBus) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientSettings.SPEC, ModConfigs.file("client"));
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new SettingsScreen(parent));
        modEventBus.addListener(GreenLanternSuitLayer::onAddLayers);
        modEventBus.addListener(UpdatePopup::onRegisterKeys);
    }
}
