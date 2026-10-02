package nl.tivek.multiversepowers;

import net.minecraft.client.model.PlayerModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.body.arm.LanternArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.pose.LanternBody;
import nl.tivek.multiversepowers.character.greenlantern.client.body.pose.MechPilot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.suit.GreenLanternSuitLayer;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.Clapped;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.Flattened;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.thor.client.ThrownHammerRenderer;
import nl.tivek.multiversepowers.character.thor.client.pose.ThorHammerLayer;
import nl.tivek.multiversepowers.character.thor.client.pose.ThorPoses;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.config.client.ConfigChoiceScreen;
import nl.tivek.multiversepowers.engine.client.pose.BodyTurns;
import nl.tivek.multiversepowers.engine.client.pose.Poses;
import nl.tivek.multiversepowers.engine.client.pose.Tired;
import nl.tivek.multiversepowers.engine.client.stagger.Reactions;
import nl.tivek.multiversepowers.engine.client.ragdoll.RagdollProfiles;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.fx.Lens;
import nl.tivek.multiversepowers.spell.client.ClientClaps;
import nl.tivek.multiversepowers.update.client.UpdatePopup;

@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class MultiversePowersClient {
    public MultiversePowersClient(ModContainer container, IEventBus modEventBus) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientSettings.SPEC, ModConfigs.file("client"));
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new ConfigChoiceScreen(parent));
        modEventBus.addListener(GreenLanternSuitLayer::onAddLayers);
        modEventBus.addListener(ThorHammerLayer::onAddLayers);
        modEventBus.addListener(ThorHammerLayer::onRegisterModels);
        modEventBus.addListener(ThrownHammerRenderer::onRegisterRenderers);
        modEventBus.addListener(UpdatePopup::onRegisterKeys);
        modEventBus.addListener(Lens::onRegisterShaders);
        modEventBus.addListener(MechPainter::onClientSetup);
        modEventBus.addListener(RagdollProfiles::onRegisterReloadListeners);
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && SwordArms.lean(player, entity));
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && FlameArms.lean(player, entity));
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && WhipArms.lean(player, entity));
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && ClientClaps.pose(player, entity));
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && MechPilot.pose(player, entity));
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && LanternBody.pose(player, entity));
        Poses.layer(Poses.Stage.CREATURE, HandVictims::pose);
        Poses.layer(Poses.Stage.CREATURE, Tired::pose);
        Poses.layer(Poses.Stage.CREATURE, Reactions::pose);
        BodyTurns.add(LanternArms::turnBody);
        BodyTurns.add(ThorPoses::turn);
        Poses.layer(Poses.Stage.MODEL, (model, entity, partialTick) -> model instanceof PlayerModel<?> player
                && ThorPoses.pose(player, entity));
        // A creature Green Lantern squashes, claps, strings up or crushes under the mech is posed by that power, not
        // limp.
        Ragdolls.claim(entity -> HandVictims.has(entity.getId()) || Flattened.has(entity.getId())
                || Clapped.has(entity.getId()) || ClientConstructs.mechVictim(entity.getId(), 0.0F) != null);
    }
}
