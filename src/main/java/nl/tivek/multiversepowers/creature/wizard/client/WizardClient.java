package nl.tivek.multiversepowers.creature.wizard.client;

import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.client.abyss.DrainRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.abyss.SupernovaRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.abyss.TendrilsRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.abyss.VoidOrbRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.BlizzardRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.ChainsRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.InfernoRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.NovaRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.RiftRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.RuneRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.TornadoRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.WarpRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.field.WellRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.ArcRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.BeamRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.CrystalRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.FamiliarRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.LanceRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.MeteorRenderer;
import nl.tivek.multiversepowers.creature.wizard.client.fx.OrbRenderer;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

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
        for (EntityType<? extends MovingMonster> type : List.<EntityType<? extends MovingMonster>>of(
                WizardEntities.WIZARD.get(), WizardEntities.DECOY.get())) {
            for (int kind : new int[] { WizardMove.STRIKE, WizardMove.BOLT, WizardMove.BLIZZARD }) {
                MobPreview.move(type, WizardMove.length(kind) + 8, w -> w.play(kind), w -> w.play(0));
            }
        }
        MobPreview.move(WizardEntities.FAMILIAR.get(), 40, f -> f.setIsCharging(true), f -> f.setIsCharging(false));
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
        event.registerEntityRenderer(WizardEntities.CRYSTAL.get(), CrystalRenderer::new);
        event.registerEntityRenderer(WizardEntities.ORB.get(), context -> new OrbRenderer<>(context, Wizard.VIOLET,
                0.45F, 0.8F));
        event.registerEntityRenderer(WizardEntities.SHARD.get(), context -> new OrbRenderer<>(context,
                Wizard.STARLIGHT, 0.35F, 1.4F));
        event.registerEntityRenderer(WizardEntities.LANCE.get(), LanceRenderer::new);
        event.registerEntityRenderer(WizardEntities.NOVA.get(), NovaRenderer::new);
        event.registerEntityRenderer(WizardEntities.TORNADO.get(), TornadoRenderer::new);
        event.registerEntityRenderer(WizardEntities.RING_OF_FIRE.get(), InfernoRenderer::new);
        event.registerEntityRenderer(WizardEntities.TENDRILS.get(), TendrilsRenderer::new);
        event.registerEntityRenderer(WizardEntities.VOID_ORB.get(), VoidOrbRenderer::new);
        event.registerEntityRenderer(WizardEntities.DRAIN.get(), DrainRenderer::new);
        event.registerEntityRenderer(WizardEntities.SUPERNOVA.get(), SupernovaRenderer::new);
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WizardRenderer.LAYER, WizardModel::createLayer);
        event.registerLayerDefinition(StaffLayer.LAYER, StaffLayer::createLayer);
    }
}
