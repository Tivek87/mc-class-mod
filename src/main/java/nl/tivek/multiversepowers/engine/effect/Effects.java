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

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Effects {
    private static final List<Running> ACTIVE = new ArrayList<>();
    private static final List<Runnable> AT_TICK_END = new CopyOnWriteArrayList<>();

    private Effects() {
    }

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

    public static void start(ServerLevel level, Effect effect) {
        ACTIVE.add(new Running(level.dimension(), effect));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickStart(ServerTickEvent.Pre event) {
        ParticleBatch.flush();
    }

    // HIGHEST here, LOWEST on onTickDone: everything else must run and queue its packets in between.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickEnding(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.connection.suspendFlushing();
        }
    }

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
        // Snapshot: an effect may start another while it ticks, which must not run until next tick.
        Running[] current = ACTIVE.toArray(new Running[0]);
        for (Running running : current) {
            ServerLevel level = server.getLevel(running.dimension);
            running.done = level == null || !tick(running, level);
            running.age++;
        }
        ACTIVE.removeIf(running -> running.done);
    }

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
