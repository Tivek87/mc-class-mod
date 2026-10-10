package nl.tivek.multiversepowers;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.content.ModItems;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.killconfirm.KillConfirms;
import nl.tivek.multiversepowers.engine.entity.DeathBlows;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.Captives;
import nl.tivek.multiversepowers.engine.entity.Fatigue;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.entity.PlayerKnockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleBatch;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.network.ModNetwork;
import nl.tivek.multiversepowers.spell.SpellCasting;
import nl.tivek.multiversepowers.testfight.TestFight;
import org.slf4j.Logger;

@Mod(MultiversePowers.MODID)
public class MultiversePowers {
    public static final String MODID = "welcomescreen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MultiversePowers(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.warn("==================== UNFINISHED / WORK IN PROGRESS ====================");
        LOGGER.warn("Multiverse Powers is still being built: things may change, break or be missing.");
        modEventBus.addListener(ModNetwork::register);
        ThrownHammer.register(modEventBus);
        MechMinion.register(modEventBus);
        ModItems.register(modEventBus);
        ModConfigs.register(modContainer, modEventBus);
        PlayerKnockdowns.listen(Characters::knockedDown);
        PlayerKnockdowns.flight(Characters::flying, Characters::flyAgain);
        NeoForge.EVENT_BUS.addListener(MultiversePowers::onServerStopping);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        Effects.clear();
        ParticleBatch.clear();
        Ceremonies.clear();
        SpellCasting.clear(event.getServer());
        Characters.clear();
        Factions.clear();
        DeathStyles.clear();
        DeathBlows.clear();
        Fatigue.clear();
        Captives.clear();
        TestFight.clear();
        KillConfirms.clear();
        // Last: held mobs must not be saved with their AI switched off.
        HeldMobs.releaseAll();
        Knockdowns.clear();
        PlayerKnockdowns.clear();
    }
}
