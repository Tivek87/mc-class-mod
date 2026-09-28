package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
        // A creature lying still after a throw is taken over as it was before it was thrown.
        boolean own = Knockdowns.ownNoAi(mob);
        Knockdowns.forget(mob);
        HELD.put(mob, own);
        mob.getPersistentData().putBoolean(SAVED_TAG, own);
        mob.setNoAi(true);
        mob.setDeltaMovement(Vec3.ZERO);
        tell(mob, true);
        return true;
    }

    public static void release(Mob mob) {
        Boolean wasNoAi = HELD.remove(mob);
        if (wasNoAi != null) {
            mob.setNoAi(wasNoAi);
            mob.getPersistentData().remove(SAVED_TAG);
            mob.resetFallDistance();
            tell(mob, false);
        }
    }

    private static void tell(Mob mob, boolean held) {
        if (!mob.level().isClientSide()) {
            PacketDistributor.sendToPlayersTrackingEntity(mob, new HeldPayload(mob.getId(), held));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && HELD.containsKey(mob)
                && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new HeldPayload(mob.getId(), true));
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
