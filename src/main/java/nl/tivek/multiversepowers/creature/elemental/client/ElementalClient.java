package nl.tivek.multiversepowers.creature.elemental.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.content.ModItems;
import nl.tivek.multiversepowers.creature.elemental.BlazeTimeline;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.creature.elemental.ElementalStaff;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The Elemental Blaze's renderers and model, its spells', the moves the Mobs page plays on it, and the staff's look for
// the element it is set to.
public final class ElementalClient {
    private static final double OUT = 1.2;

    private ElementalClient() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ElementalBlaze.TYPE.get(), ElementalBlazeRenderer::new);
        event.registerEntityRenderer(ElementalBlaze.FROST_RAY.get(), FrostRayRenderer::new);
        event.registerEntityRenderer(ElementalBlaze.HEAVED_STONE.get(), HeavedStoneRenderer::new);
        event.registerEntityRenderer(ElementalBlaze.STORM_ARC.get(), StormArcRenderer::new);
        event.registerEntityRenderer(ElementalBlaze.SCORCH_BALL.get(), ScorchBallRenderer::new);
        for (Element element : Element.ALL) {
            int kind = BlazeTimeline.kind(element, false);
            MobPreview.move(ElementalBlaze.TYPE.get(), BlazeTimeline.length(kind) + 10, blaze -> blaze.play(kind),
                    blaze -> blaze.play(0));
        }
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ElementalBlazeRenderer.LAYER, ElementalBlazeModel::createLayer);
    }

    // Where a spell from `from` to `to` starts being drawn: in its own caster's first-person view a little way out,
    // or its near end fills their screen.
    static Vec3 start(Entity caster, Vec3 from, Vec3 to) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        return !own(caster) || length < 1.0E-3 ? from : from.add(way.scale(Math.min(OUT, length * 0.5) / length));
    }

    // Whether `caster` sees this through their own eyes.
    static boolean own(Entity caster) {
        Minecraft mc = Minecraft.getInstance();
        return caster == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.ELEMENTAL_STAFF.get(),
                ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "element"),
                (stack, level, holder, seed) -> ElementalStaff.element(stack).ordinal() / 4.0F));
    }
}
