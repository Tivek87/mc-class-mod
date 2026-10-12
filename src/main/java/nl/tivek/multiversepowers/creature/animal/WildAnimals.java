package nl.tivek.multiversepowers.creature.animal;

import net.neoforged.bus.api.IEventBus;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;
import nl.tivek.multiversepowers.creature.animal.moose.Moose;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;

// Every wild animal of the mod, registered in one place.
public final class WildAnimals {
    private WildAnimals() {
    }

    public static void register(IEventBus modEventBus) {
        BrownBear.register(modEventBus);
        Deer.register(modEventBus);
        Raccoon.register(modEventBus);
        Hedgehog.register(modEventBus);
        Owl.register(modEventBus);
        Squirrel.register(modEventBus);
        Snake.register(modEventBus);
        Crab.register(modEventBus);
        Moose.register(modEventBus);
    }
}
