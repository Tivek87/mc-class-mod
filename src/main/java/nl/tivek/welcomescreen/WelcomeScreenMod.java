package nl.tivek.welcomescreen;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import nl.tivek.welcomescreen.config.ModConfigs;
import nl.tivek.welcomescreen.network.ModNetwork;

@Mod(WelcomeScreenMod.MODID)
public class WelcomeScreenMod {
    public static final String MODID = "welcomescreen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WelcomeScreenMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::register);
        nl.tivek.welcomescreen.init.ModItems.register(modEventBus);
        nl.tivek.welcomescreen.init.ModEffects.register(modEventBus);
        // Every settings file of this mod, together in config/welcomescreen.
        ModConfigs.register(modContainer);
        // The config screen is registered in WelcomeScreenClient: screen classes do not
        // exist on a server.
    }
}
