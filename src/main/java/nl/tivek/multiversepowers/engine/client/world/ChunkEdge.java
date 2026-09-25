package nl.tivek.multiversepowers.engine.client.world;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Vanilla freezes a player dead in an unloaded chunk instead of slowing him; this eases speed down before that edge.
public final class ChunkEdge {
    private static final double STEP = 2.0;

    private ChunkEdge() {
    }

    public static Vec3 cap(Level level, Vec3 from, Vec3 velocity, int lookTicks) {
        double speed = velocity.horizontalDistance();
        if (speed < 1.0E-4) {
            return velocity;
        }
        double wx = velocity.x / speed;
        double wz = velocity.z / speed;
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
