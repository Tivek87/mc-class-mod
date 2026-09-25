package nl.tivek.multiversepowers.engine.world;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

public final class ChunkPreloader {
    public static final int EVERY_TICKS = 10;
    // Keep this well above EVERY_TICKS, or a slow re-ask lets the ticket expire.
    private static final int KEEP_TICKS = 100;
    private static final TicketType<Integer> READY = TicketType.create("multiversepowers_ready", Integer::compareTo,
            KEEP_TICKS);
    // Ticket distance: 1 = loaded and ready to send; 2 = also wins over other work.
    private static final int READY_LEVEL = 1;
    private static final int URGENT_LEVEL = 2;
    private static final double URGENT_TICKS = 60.0;
    private static final double MAX_AHEAD = 512.0;
    private static final double STEP = 8.0;
    private static final int WIDE = 1;
    private static final int MAX_CHUNKS = 1200;

    private ChunkPreloader() {
    }

    public static void keep(ServerPlayer player, Vec3 velocity, double radius, double aheadSeconds) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double z = player.getZ();
        LongLinkedOpenHashSet urgent = new LongLinkedOpenHashSet();
        LongLinkedOpenHashSet ready = new LongLinkedOpenHashSet();
        urgent.add(ChunkPos.asLong(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)));
        double speed = velocity.horizontalDistance();
        double ahead = Math.min(MAX_AHEAD, speed * aheadSeconds * 20.0);
        if (ahead > 0.0 && speed > 1.0E-4) {
            double wx = velocity.x / speed;
            double wz = velocity.z / speed;
            double soon = speed * URGENT_TICKS;
            for (double along = 0.0; along <= ahead + STEP; along += STEP) {
                double at = Math.min(along, ahead);
                int ax = SectionPos.blockToSectionCoord(x + wx * at);
                int az = SectionPos.blockToSectionCoord(z + wz * at);
                for (int dx = -WIDE; dx <= WIDE; dx++) {
                    for (int dz = -WIDE; dz <= WIDE; dz++) {
                        (at <= soon ? urgent : ready).add(ChunkPos.asLong(ax + dx, az + dz));
                    }
                }
            }
        }
        int cx = SectionPos.blockToSectionCoord(x);
        int cz = SectionPos.blockToSectionCoord(z);
        int r = Mth.ceil(Math.max(0.0, radius) / 16.0);
        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring && dx * dx + dz * dz <= r * r) {
                        ready.add(ChunkPos.asLong(cx + dx, cz + dz));
                    }
                }
            }
        }
        ready.removeAll(urgent);
        int left = add(level, urgent, URGENT_LEVEL, player.getId(), MAX_CHUNKS);
        add(level, ready, READY_LEVEL, player.getId(), left);
    }

    private static int add(ServerLevel level, LongLinkedOpenHashSet chunks, int distance, int owner, int most) {
        ServerChunkCache cache = level.getChunkSource();
        WorldBorder border = level.getWorldBorder();
        int left = most;
        LongIterator all = chunks.iterator();
        while (all.hasNext() && left > 0) {
            ChunkPos chunk = new ChunkPos(all.nextLong());
            if (border.isWithinBounds(chunk)) {
                cache.addRegionTicket(READY, chunk, distance, owner);
                left--;
            }
        }
        return left;
    }
}
