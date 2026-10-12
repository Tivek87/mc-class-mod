package nl.tivek.multiversepowers;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.greenlantern.summon.HardLight;
import nl.tivek.multiversepowers.character.greenlantern.summon.Summons;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies;
import nl.tivek.multiversepowers.command.ShortCommandRegistry;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.transfer.SettingsTransfer;
import nl.tivek.multiversepowers.content.ModItems;
import nl.tivek.multiversepowers.creature.Bleeding;
import nl.tivek.multiversepowers.creature.animal.WildAnimals;
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;
import nl.tivek.multiversepowers.creature.lurker.Lurker;
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.ritual.WizardRitual;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Captives;
import nl.tivek.multiversepowers.engine.entity.DeathBlows;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.Fatigue;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.entity.PlayerKnockdowns;
import nl.tivek.multiversepowers.engine.entity.TimeFreeze;
import nl.tivek.multiversepowers.engine.entity.TornBlock;
import nl.tivek.multiversepowers.engine.fx.ParticleBatch;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.faction.mob.MobDefaults;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.galactus.GalactusCommand;
import nl.tivek.multiversepowers.killconfirm.KillConfirms;
import nl.tivek.multiversepowers.network.ModNetwork;
import nl.tivek.multiversepowers.schematic.SchematicServer;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprints;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;
import nl.tivek.multiversepowers.space.MoonGenerator;
import nl.tivek.multiversepowers.space.SpaceTravel;
import nl.tivek.multiversepowers.spell.SpellCasting;
import nl.tivek.multiversepowers.stage.StageBlocks;
import nl.tivek.multiversepowers.stage.show.LightShow;
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
        TornBlock.register(modEventBus);
        MechMinion.register(modEventBus);
        ZombieBrute.register(modEventBus);
        Bleeding.register(modEventBus);
        Bloater.register(modEventBus);
        Lurker.register(modEventBus);
        Screamer.register(modEventBus);
        Rustbound.register(modEventBus);
        Stalker.register(modEventBus);
        WildAnimals.register(modEventBus);
        Wizard.register(modEventBus);
        HardLight.register(modEventBus);
        StageBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        MoonGenerator.register(modEventBus);
        SpaceTravel.register();
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
        Blueprints.clear();
        Factions.clear();
        MobDefaults.clear();
        ShortCommandRegistry.clear();
        Summons.clear();
        WizardRitual.clear();
        WizardStaff.clear();
        GalactusCommand.clear();
        SpaceTravel.clear();
        MobRules.forget();
        DeathStyles.clear();
        DeathBlows.clear();
        Fatigue.clear();
        Captives.clear();
        TestFight.clear();
        KillConfirms.clear();
        SettingsTransfer.clear();
        SchematicPastes.clear();
        SchematicServer.clear();
        LightShow.stopAll(event.getServer());
        // Last: held mobs must not be saved with their AI switched off.
        TimeFreeze.thawAll();
        HeldMobs.releaseAll();
        Knockdowns.clear();
        PlayerKnockdowns.clear();
    }
}
