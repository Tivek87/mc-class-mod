package nl.tivek.multiversepowers.spell.client;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// The glow bit lives in Entity's own flags; subclass it just to reach them.
abstract class GlowFlag extends Entity {
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = DATA_SHARED_FLAGS_ID;
    private static final int GLOWING_BIT = 1 << 6;

    private GlowFlag(EntityType<?> type, Level level) {
        super(type, level);
    }

    static boolean isGlowing(Entity entity) {
        return (entity.getEntityData().get(SHARED_FLAGS) & GLOWING_BIT) != 0;
    }

    // Client entities only: the server overwrites this again next sync.
    static void setGlowing(Entity entity, boolean glowing) {
        byte flags = entity.getEntityData().get(SHARED_FLAGS);
        byte updated = (byte) (glowing ? flags | GLOWING_BIT : flags & ~GLOWING_BIT);
        if (updated != flags) {
            entity.getEntityData().set(SHARED_FLAGS, updated);
        }
    }
}
