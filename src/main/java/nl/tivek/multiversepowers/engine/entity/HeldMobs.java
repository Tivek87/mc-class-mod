package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class HeldMobs {
    private static final String SAVED_TAG = "welcomescreen_held_noai";
    private static final Map<Mob, Boolean> HELD = new IdentityHashMap<>();
    private static final List<Predicate<Entity>> HOLDERS = new ArrayList<>();

    private HeldMobs() {
    }

    public static boolean isHeld(Entity entity) {
        return entity instanceof Mob mob && HELD.containsKey(mob);
    }

    public static boolean isHeldByAnyone(Entity entity) {
        if (isHeld(entity)) {
            return true;
        }
        for (Predicate<Entity> holder : HOLDERS) {
            if (holder.test(entity)) {
                return true;
            }
        }
        return false;
    }

    public static void addHolder(Predicate<Entity> holder) {
        HOLDERS.add(holder);
    }

    public static boolean hold(Mob mob) {
        if (HELD.containsKey(mob)) {
            return false;
        }
        HELD.put(mob, mob.isNoAi());
        mob.getPersistentData().putBoolean(SAVED_TAG, mob.isNoAi());
        mob.setNoAi(true);
        mob.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    public static void release(Mob mob) {
        Boolean wasNoAi = HELD.remove(mob);
        if (wasNoAi != null) {
            mob.setNoAi(wasNoAi);
            mob.getPersistentData().remove(SAVED_TAG);
            mob.resetFallDistance();
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && !HELD.containsKey(mob)
                && mob.getPersistentData().contains(SAVED_TAG)) {
            mob.setNoAi(mob.getPersistentData().getBoolean(SAVED_TAG));
            mob.getPersistentData().remove(SAVED_TAG);
        }
    }

    public static void releaseAll() {
        for (Mob mob : new ArrayList<>(HELD.keySet())) {
            release(mob);
        }
    }
}
