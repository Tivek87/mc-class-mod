package nl.tivek.welcomescreen.spell;

import net.minecraft.server.level.ServerLevel;

/**
 * One running piece of a spell (a charge-up, a trail, a lingering cloud...), ticked by the server
 * every tick until it says it is done.
 */
@FunctionalInterface
public interface SpellEffect {
    /**
     * @param age ticks since the effect started, 0 on the first call
     * @return false when the effect is finished and can be removed
     */
    boolean tick(ServerLevel level, int age);
}
