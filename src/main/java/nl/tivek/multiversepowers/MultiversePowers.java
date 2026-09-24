package nl.tivek.multiversepowers;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleBatch;
import nl.tivek.multiversepowers.network.ModNetwork;
import nl.tivek.multiversepowers.spell.SpellCasting;
import org.slf4j.Logger;

/**
 * The start of the mod, on the server and on every player's own game alike: everything the mod adds to the game is
 * registered here, and everything it keeps per server is forgotten here when the server stops. The part only a
 * player's own game has starts in {@link MultiversePowersClient}.
 */
@Mod(MultiversePowers.MODID)
public class MultiversePowers {
    /** The mod's id: it stays "welcomescreen", because worlds, settings files and keys already carry it. */
    public static final String MODID = "welcomescreen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MultiversePowers(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::register);
        // Every settings file of this mod, together in config/welcomescreen.
        ModConfigs.register(modContainer, modEventBus);
        // The config screen is registered in MultiversePowersClient: screen classes do not
        // exist on a server.
        NeoForge.EVENT_BUS.addListener(MultiversePowers::onServerStopping);
    }

    /**
     * The server stops: every power forgets what it was doing, in this order, before the world is saved. Every
     * ability, spell and character keeps its state per server in its own place; this is the one list of them all.
     */
    private static void onServerStopping(ServerStoppingEvent event) {
        Effects.clear();
        ParticleBatch.clear();
        Ceremonies.clear();
        SpellCasting.clear(event.getServer());
        Characters.clear();
        // Last: held mobs must not be saved with their AI switched off.
        HeldMobs.releaseAll();
    }
}
