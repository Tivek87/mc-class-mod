package nl.tivek.multiversepowers.engine.effect;

import net.minecraft.server.level.ServerLevel;

/**
 * One running piece of a power (a charge-up, a trail, a lingering cloud, a construct on its way...), ticked by the
 * server every tick (see {@link Effects}) until it says it is done.
 */
@FunctionalInterface
public interface Effect {
    /**
     * @param age ticks since the effect started, 0 on the first call
     * @return false when the effect is finished and can be removed
     */
    boolean tick(ServerLevel level, int age);
}
