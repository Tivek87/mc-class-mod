package nl.tivek.multiversepowers.engine.effect;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Runs every {@link Effect} of every power: spells, the abilities of every character, anything that goes on for a
 * while. Each one is ticked once every server tick, in the order they were started, until it says it is done. An
 * effect lives in the level it was started in; once that level is gone, so is the effect.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Effects {
    private static final List<Running> ACTIVE = new ArrayList<>();

    private Effects() {
    }

    private static final class Running {
        private final ResourceKey<Level> dimension;
        private final Effect effect;
        private int age;
        private boolean done;

        private Running(ResourceKey<Level> dimension, Effect effect) {
            this.dimension = dimension;
            this.effect = effect;
        }
    }

    /** Starts an effect in this level; its first tick runs on the next server tick. */
    public static void start(ServerLevel level, Effect effect) {
        ACTIVE.add(new Running(level.dimension(), effect));
    }

    /** Forgets every effect that is still running (the server stops). */
    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        // The ones running now; an effect may start new ones while it ticks (the poison vial turns into the cloud),
        // and those begin on the next tick.
        Running[] current = ACTIVE.toArray(new Running[0]);
        for (Running running : current) {
            ServerLevel level = server.getLevel(running.dimension);
            running.done = level == null || !tick(running, level);
            running.age++;
        }
        ACTIVE.removeIf(running -> running.done);
    }

    /**
     * One tick of one effect. One that breaks is stopped on its own, with the error in the log, so it cannot take the
     * whole server down with it; while developing it still crashes, so the error is never missed.
     */
    private static boolean tick(Running running, ServerLevel level) {
        if (!FMLEnvironment.production) {
            return running.effect.tick(level, running.age);
        }
        try {
            return running.effect.tick(level, running.age);
        } catch (RuntimeException error) {
            MultiversePowers.LOGGER.error("An effect ({}) broke on tick {} and was stopped",
                    running.effect.getClass().getName(), running.age, error);
            return false;
        }
    }
}
