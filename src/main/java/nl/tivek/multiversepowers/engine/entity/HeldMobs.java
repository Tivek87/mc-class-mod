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

/**
 * Mobs a power is holding. While held, a mob's own AI and gravity are off (NoAI), so the power alone
 * decides where it goes. Its old NoAI value is always put back, also when the server stops, and also
 * when the mob was saved while held (its chunk unloaded, or the game crashed): it then gets its AI back
 * the next time it is loaded.
 *
 * <p>A power with a grip of its own (Doctor Octopus's claws also hold players) tells what it holds through
 * {@link #addHolder}, so {@link #isHeldByAnyone} knows about every grip in the mod.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class HeldMobs {
    private static final String SAVED_TAG = "welcomescreen_held_noai";
    // Held mob -> whether it already had NoAI before (a mob from a spawn egg with NoAI stays that way).
    private static final Map<Mob, Boolean> HELD = new IdentityHashMap<>();
    // The grips of powers that hold things their own way.
    private static final List<Predicate<Entity>> HOLDERS = new ArrayList<>();

    private HeldMobs() {
    }

    /** True while a power holds this mob here (see {@link #hold}). */
    public static boolean isHeld(Entity entity) {
        return entity instanceof Mob mob && HELD.containsKey(mob);
    }

    /** True while anything holds it: here, or in the grip of a power that holds things its own way. */
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

    /** A grip of a power's own: whatever it says it holds counts as held for every other power too. */
    public static void addHolder(Predicate<Entity> holder) {
        HOLDERS.add(holder);
    }

    /** @return false when another power already holds this mob */
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

    /** A mob loaded while no power holds it, but saved while one did: give its AI back. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && !HELD.containsKey(mob)
                && mob.getPersistentData().contains(SAVED_TAG)) {
            mob.setNoAi(mob.getPersistentData().getBoolean(SAVED_TAG));
            mob.getPersistentData().remove(SAVED_TAG);
        }
    }

    /** Every held mob gets its own AI back (the server stops: before the world is saved). */
    public static void releaseAll() {
        for (Mob mob : new ArrayList<>(HELD.keySet())) {
            release(mob);
        }
    }
}
