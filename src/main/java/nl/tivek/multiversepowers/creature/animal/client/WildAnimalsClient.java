package nl.tivek.multiversepowers.creature.animal.client;

import net.neoforged.bus.api.IEventBus;
import nl.tivek.multiversepowers.creature.animal.bear.client.BearRenderer;
import nl.tivek.multiversepowers.creature.animal.crab.client.CrabRenderer;
import nl.tivek.multiversepowers.creature.animal.deer.client.DeerRenderer;
import nl.tivek.multiversepowers.creature.animal.hedgehog.client.HedgehogRenderer;
import nl.tivek.multiversepowers.creature.animal.moose.client.MooseRenderer;
import nl.tivek.multiversepowers.creature.animal.owl.client.OwlRenderer;
import nl.tivek.multiversepowers.creature.animal.raccoon.client.RaccoonRenderer;
import nl.tivek.multiversepowers.creature.animal.snake.client.SnakeRenderer;
import nl.tivek.multiversepowers.creature.animal.squirrel.client.SquirrelRenderer;

// Every wild animal's renderer and model, registered in one place.
public final class WildAnimalsClient {
    private WildAnimalsClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BearRenderer::onRegisterRenderers);
        modEventBus.addListener(BearRenderer::onRegisterLayers);
        modEventBus.addListener(DeerRenderer::onRegisterRenderers);
        modEventBus.addListener(DeerRenderer::onRegisterLayers);
        modEventBus.addListener(RaccoonRenderer::onRegisterRenderers);
        modEventBus.addListener(RaccoonRenderer::onRegisterLayers);
        modEventBus.addListener(HedgehogRenderer::onRegisterRenderers);
        modEventBus.addListener(HedgehogRenderer::onRegisterLayers);
        modEventBus.addListener(OwlRenderer::onRegisterRenderers);
        modEventBus.addListener(OwlRenderer::onRegisterLayers);
        modEventBus.addListener(SquirrelRenderer::onRegisterRenderers);
        modEventBus.addListener(SquirrelRenderer::onRegisterLayers);
        modEventBus.addListener(SnakeRenderer::onRegisterRenderers);
        modEventBus.addListener(SnakeRenderer::onRegisterLayers);
        modEventBus.addListener(CrabRenderer::onRegisterRenderers);
        modEventBus.addListener(CrabRenderer::onRegisterLayers);
        modEventBus.addListener(MooseRenderer::onRegisterRenderers);
        modEventBus.addListener(MooseRenderer::onRegisterLayers);
    }
}
