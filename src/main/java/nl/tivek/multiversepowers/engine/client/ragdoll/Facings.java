package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.math.Ease;

// A creature that got up faces the way it rose, drawn so until it walks or turns of its own; then it turns to its own
// way over TURN ticks, as it would turn anyway. Only what this player sees.
final class Facings {
    private static final int TURN = 10;
    // Moved this far (blocks) or turned this far (degrees) of its own since it stood, it turns to its own way.
    private static final double MOVED = 0.05;
    private static final float TURNED = 2.0F;
    // Drawn this near (degrees) its own way, it is left as it is.
    private static final float NEAR = 1.0F;

    private static final class Facing {
        private final LivingEntity entity;
        // How far (degrees) it is drawn turned from its own way, its own way and where it stood as it got up, and
        // ticks into turning to its own way (-1 before).
        private final float offset;
        private final float own;
        private final double x;
        private final double z;
        private int turning = -1;

        private Facing(LivingEntity entity, float offset) {
            this.entity = entity;
            this.offset = offset;
            this.own = entity.yBodyRot;
            this.x = entity.getX();
            this.z = entity.getZ();
        }
    }

    private static final Int2ObjectOpenHashMap<Facing> ALL = new Int2ObjectOpenHashMap<>();

    private Facings() {
    }

    // It stands, drawn facing `yaw` (the game's degrees).
    static void rose(LivingEntity entity, float yaw) {
        float offset = Mth.wrapDegrees(yaw - entity.yBodyRot);
        if (Math.abs(offset) > NEAR) {
            ALL.put(entity.getId(), new Facing(entity, offset));
        } else {
            ALL.remove(entity.getId());
        }
    }

    static void tick() {
        ALL.values().removeIf(facing -> {
            LivingEntity entity = facing.entity;
            if (entity.isRemoved() || !entity.isAlive()) {
                return true;
            }
            if (facing.turning < 0) {
                double dx = entity.getX() - facing.x;
                double dz = entity.getZ() - facing.z;
                if (dx * dx + dz * dz > MOVED * MOVED
                        || Math.abs(Mth.wrapDegrees(entity.yBodyRot - facing.own)) > TURNED) {
                    facing.turning = 0;
                }
                return false;
            }
            return ++facing.turning >= TURN;
        });
    }

    // How far (degrees) the creature is drawn turned from its own way now.
    static float offset(LivingEntity entity, float partialTick) {
        Facing facing = ALL.get(entity.getId());
        if (facing == null || facing.entity != entity) {
            return 0.0F;
        }
        if (facing.turning < 0) {
            return facing.offset;
        }
        return facing.offset * (1.0F - (float) Ease.smoother((facing.turning + partialTick) / TURN));
    }

    static void forget(int entity) {
        ALL.remove(entity);
    }

    static void clear() {
        ALL.clear();
    }
}
