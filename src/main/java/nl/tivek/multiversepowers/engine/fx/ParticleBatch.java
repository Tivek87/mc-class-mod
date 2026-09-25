package nl.tivek.multiversepowers.engine.fx;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ParticleBatch {
    private static final Map<ServerPlayer, List<ParticlesPayload.Entry>> PENDING = new IdentityHashMap<>();

    private ParticleBatch() {
    }

    public static void add(ServerPlayer player, ParticleOptions options, boolean force, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
        PENDING.computeIfAbsent(player, key -> new ArrayList<>()).add(new ParticlesPayload.Entry(options, force, x, y,
                z, (float) dx, (float) dy, (float) dz, (float) speed, count));
    }

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

    public static void clear() {
        PENDING.clear();
    }
}
