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

/**
 * Makes the world ready for a player who moves faster than the game makes and sends it by itself: the chunks round him
 * and along the way he goes are made (generated), loaded and kept ready to be sent, before he gets there. The game
 * stops a player dead at the edge of the chunks his own game has, until the next one comes in; this keeps that edge far
 * ahead of him.
 *
 * <p>The game itself only works on a few chunks at a time for a player, nearest first and all round him alike; the chunks
 * asked for here go to all of the server's world makers at once, the way ahead first. Every call keeps them a few
 * seconds; what is no longer asked for is let go by itself after that, so a player who stops, turns, lands, leaves or goes
 * to another world needs nothing undone, and two players never let go of each other's chunks.
 */
public final class ChunkPreloader {
    /** How often a mover should ask again, in ticks: well within the time a chunk is kept. */
    public static final int EVERY_TICKS = 10;
    // How long a chunk stays ready after it was last asked for, in ticks.
    private static final int KEEP_TICKS = 100;
    private static final TicketType<Integer> READY = TicketType.create("multiversepowers_ready", Integer::compareTo,
            KEEP_TICKS);
    // How ready a chunk is kept, as the game counts it (from the chunk itself outwards): 1 is loaded and ready to be
    // sent; 2, for the way just ahead, also comes first when the server has more to make than it can.
    private static final int READY_LEVEL = 1;
    private static final int URGENT_LEVEL = 2;
    // The way just ahead: the next few seconds of it, in ticks.
    private static final double URGENT_TICKS = 60.0;
    // How far ahead at most, in blocks, in steps of half a chunk; how many chunks either side of the way; and the most
    // chunks one mover keeps at once.
    private static final double MAX_AHEAD = 512.0;
    private static final double STEP = 8.0;
    private static final int WIDE = 1;
    private static final int MAX_CHUNKS = 1200;

    private ChunkPreloader() {
    }

    /**
     * Keeps the world ready round this player and ahead of him, for a few seconds; call it again every
     * {@link #EVERY_TICKS} ticks for as long as he moves fast.
     *
     * @param velocity     how he moves, in blocks per tick: the way ahead follows it
     * @param radius       how far round him, in blocks (0 for only the way ahead)
     * @param aheadSeconds how many seconds of moving ahead of him, at his speed (at most {@link #MAX_AHEAD} blocks)
     */
    public static void keep(ServerPlayer player, Vec3 velocity, double radius, double aheadSeconds) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double z = player.getZ();
        LongLinkedOpenHashSet urgent = new LongLinkedOpenHashSet();
        LongLinkedOpenHashSet ready = new LongLinkedOpenHashSet();
        // The chunk he is in, and the way ahead: that is where he will be soonest.
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
        // Then round him, nearest first.
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

    /** Keeps these chunks ready, at most {@code most} of them inside the world border; how many more may follow. */
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
