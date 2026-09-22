package nl.tivek.welcomescreen.spell;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Mobs a spell is holding. While held, a mob's own AI and gravity are off (NoAI), so the spell alone
 * decides where it goes. Its old NoAI value is always put back, also when the server stops, and also
 * when the mob was saved while held (its chunk unloaded, or the game crashed): it then gets its AI back
 * the next time it is loaded.
 */
public final class HeldMobs {
    private static final String SAVED_TAG = "welcomescreen_held_noai";
    // Held mob -> whether it already had NoAI before (a mob from a spawn egg with NoAI stays that way).
    private static final Map<Mob, Boolean> HELD = new IdentityHashMap<>();

    private HeldMobs() {
    }

    public static boolean isHeld(Entity entity) {
        return entity instanceof Mob mob && HELD.containsKey(mob);
    }

    /** @return false when another spell already holds this mob */
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

    /** A mob loaded while no spell holds it, but saved while one did: give its AI back. */
    public static void restoreSaved(Entity entity) {
        if (entity instanceof Mob mob && !HELD.containsKey(mob) && mob.getPersistentData().contains(SAVED_TAG)) {
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
