package nl.tivek.multiversepowers.engine.client.world;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a player that his own game moves quickly from running into a chunk it does not have yet. The game does not move
 * a player at all while he stands in such a chunk: he would stop dead in the air until it comes in, then jump on. Instead
 * his speed along the ground is eased down before the edge of what is loaded, so he slows smoothly, and picks up again
 * the moment the world has caught up. Only what is needed is taken off, and going up and down stays free.
 *
 * <p>The server makes the world ready ahead of fast movers (see {@code engine.world.ChunkPreloader}), so this is only
 * the last safety.
 */
public final class ChunkEdge {
    // How far apart the checks along the way are, in blocks.
    private static final double STEP = 2.0;

    private ChunkEdge() {
    }

    /**
     * The speed a mover may keep, in blocks per tick: {@code velocity}, its part along the ground eased down so that the
     * loaded world before him lasts at least {@code lookTicks} ticks at that speed.
     */
    public static Vec3 cap(Level level, Vec3 from, Vec3 velocity, int lookTicks) {
        double speed = velocity.horizontalDistance();
        if (speed < 1.0E-4) {
            return velocity;
        }
        double wx = velocity.x / speed;
        double wz = velocity.z / speed;
        // One step past what this speed covers in lookTicks: the first spot along the way whose chunk is missing.
        double reach = speed * lookTicks + STEP;
        double free = -1.0;
        for (double along = STEP; along <= reach; along += STEP) {
            if (!level.hasChunk(SectionPos.blockToSectionCoord(from.x + wx * along),
                    SectionPos.blockToSectionCoord(from.z + wz * along))) {
                free = along - STEP;
                break;
            }
        }
        if (free < 0.0) {
            return velocity;
        }
        double allowed = free / lookTicks;
        if (allowed >= speed) {
            return velocity;
        }
        double scale = allowed / speed;
        return new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
    }
}
