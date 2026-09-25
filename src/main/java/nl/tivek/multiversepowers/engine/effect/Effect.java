package nl.tivek.multiversepowers.engine.effect;

import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface Effect {
    boolean tick(ServerLevel level, int age);
}
