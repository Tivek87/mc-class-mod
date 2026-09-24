package nl.tivek.multiversepowers.engine.fx;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The particles the powers send, gathered per player and sent together, in one packet per player each tick (see
 * {@link ParticlesPayload}), instead of one packet per particle per player. A single shockwave of light is easily a few
 * hundred particles, and every player near it used to get a packet for each; now he gets them all at once, and the
 * server sends a few packets a tick instead of thousands. What every player sees stays exactly the same.
 *
 * <p>What is gathered goes out at the start and at the end of every server tick (see
 * {@code engine.effect.Effects}), so nothing waits longer than the rest of the tick it was made in.
 */
public final class ParticleBatch {
    private static final Map<ServerPlayer, List<ParticlesPayload.Entry>> PENDING = new IdentityHashMap<>();

    private ParticleBatch() {
    }

    /** One particle packet's worth for this player (what the game's own particle packet would hold). */
    public static void add(ServerPlayer player, ParticleOptions options, boolean force, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
        PENDING.computeIfAbsent(player, key -> new ArrayList<>()).add(new ParticlesPayload.Entry(options, force, x, y,
                z, (float) dx, (float) dy, (float) dz, (float) speed, count));
    }

    /** Sends every player what was gathered for him. */
    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Map.Entry<ServerPlayer, List<ParticlesPayload.Entry>> pending : PENDING.entrySet()) {
            ServerPlayer player = pending.getKey();
            List<ParticlesPayload.Entry> entries = pending.getValue();
            if (player.hasDisconnected()) {
                continue;
            }
            for (int from = 0; from < entries.size(); from += ParticlesPayload.MAX_ENTRIES) {
                int to = Math.min(entries.size(), from + ParticlesPayload.MAX_ENTRIES);
                PacketDistributor.sendToPlayer(player, new ParticlesPayload(List.copyOf(entries.subList(from, to))));
            }
        }
        PENDING.clear();
    }

    /** Forgets what was not sent yet (the server stops). */
    public static void clear() {
        PENDING.clear();
    }
}
