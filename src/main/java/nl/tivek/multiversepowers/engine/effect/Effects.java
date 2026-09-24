package nl.tivek.multiversepowers.engine.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleBatch;

/**
 * Runs every {@link Effect} of every power: spells, the abilities of every character, anything that goes on for a
 * while. Each one is ticked once every server tick, in the order they were started, until it says it is done. An
 * effect lives in the level it was started in; once that level is gone, so is the effect.
 *
 * <p>What the powers send at the end of a tick goes out to every player together once they are all done, and their
 * particles in one packet per player (see {@link ParticleBatch}).
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Effects {
    private static final List<Running> ACTIVE = new ArrayList<>();
    // What powers gather over a tick and send once at its end (see atTickEnd).
    private static final List<Runnable> AT_TICK_END = new CopyOnWriteArrayList<>();

    private Effects() {
    }

    /**
     * Something a power gathers over a tick (a value that changes on every tick, told once instead of on every change)
     * and sends at the end of it, together with everything else that tick sends.
     */
    public static void atTickEnd(Runnable send) {
        AT_TICK_END.add(send);
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

    /**
     * The start of a server tick: the particles gathered since the end of the last one (a key a player pressed in
     * between) go out now (see {@link ParticleBatch}).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickStart(ServerTickEvent.Pre event) {
        ParticleBatch.flush();
    }

    /**
     * Before everything else at the end of a tick: whatever the powers send from here on waits, and goes out to every
     * player together once they are all done ({@link #onTickDone}), the way the game sends everything of its own in a
     * tick, instead of in a network write of its own for every packet.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickEnding(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.connection.suspendFlushing();
        }
    }

    /**
     * After everything else at the end of a tick: what the powers gathered over it (see {@link #atTickEnd}), this
     * tick's particles, and all that waited, go out.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTickDone(ServerTickEvent.Post event) {
        for (Runnable send : AT_TICK_END) {
            send.run();
        }
        ParticleBatch.flush();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.connection.resumeFlushing();
        }
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
