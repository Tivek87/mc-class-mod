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
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.network.ModNetwork;
import nl.tivek.multiversepowers.spell.SpellCasting;
import org.slf4j.Logger;

@Mod(MultiversePowers.MODID)
public class MultiversePowers {
    public static final String MODID = "welcomescreen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MultiversePowers(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetwork::register);
        ModConfigs.register(modContainer, modEventBus);
        NeoForge.EVENT_BUS.addListener(MultiversePowers::onServerStopping);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        Effects.clear();
        ParticleBatch.clear();
        Ceremonies.clear();
        SpellCasting.clear(event.getServer());
        Characters.clear();
        Factions.clear();
        // Last: held mobs must not be saved with their AI switched off.
        HeldMobs.releaseAll();
    }
}
