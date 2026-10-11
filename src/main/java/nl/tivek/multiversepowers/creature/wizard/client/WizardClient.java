package nl.tivek.multiversepowers.creature.wizard.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.client.field.BlizzardRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.ChainsRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.RiftRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.RuneRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.WarpRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.WellRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.ArcRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.BeamRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.FamiliarRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.MeteorRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.OrbRenderer;

// Registers how the Wizard, his images and his spells are drawn.
public final class WizardClient {
    private WizardClient() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WizardEntities.WIZARD.get(), context -> new WizardRenderer<Wizard>(context,
                0.6F));
        event.registerEntityRenderer(WizardEntities.DECOY.get(), context -> new WizardRenderer<WizardDecoy>(context,
                0.0F));
        event.registerEntityRenderer(WizardEntities.FAMILIAR.get(), FamiliarRenderer::new);
        event.registerEntityRenderer(WizardEntities.BOLT.get(), context -> new OrbRenderer<>(context, Wizard.VIOLET,
                0.55F, 0.9F));
        event.registerEntityRenderer(WizardEntities.SPARK.get(), context -> new OrbRenderer<>(context, Wizard.VIOLET,
                0.3F, 0.6F));
        event.registerEntityRenderer(WizardEntities.FIREBALL.get(), context -> new OrbRenderer<>(context, Wizard.FIRE,
                0.9F, 0.7F));
        event.registerEntityRenderer(WizardEntities.STAR.get(), context -> new OrbRenderer<>(context,
                Wizard.STARLIGHT, 0.8F, 2.2F));
        event.registerEntityRenderer(WizardEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(WizardEntities.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(WizardEntities.BLIZZARD.get(), BlizzardRenderer::new);
        event.registerEntityRenderer(WizardEntities.CHAINS.get(), ChainsRenderer::new);
        event.registerEntityRenderer(WizardEntities.RUNE.get(), RuneRenderer::new);
        event.registerEntityRenderer(WizardEntities.WELL.get(), WellRenderer::new);
        event.registerEntityRenderer(WizardEntities.WARP.get(), WarpRenderer::new);
        event.registerEntityRenderer(WizardEntities.ARC.get(), ArcRenderer::new);
        event.registerEntityRenderer(WizardEntities.BEAM.get(), BeamRenderer::new);
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WizardRenderer.LAYER, WizardModel::createLayer);
        event.registerLayerDefinition(StaffLayer.LAYER, StaffLayer::createLayer);
    }
}
