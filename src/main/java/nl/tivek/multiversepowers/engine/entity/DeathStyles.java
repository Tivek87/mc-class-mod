package nl.tivek.multiversepowers.engine.entity;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// How a creature dies, when a power decides it: a power marks the creature just before it strikes, and if that blow
// kills it the players near it are told, so their games show that death instead of a body going limp.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class DeathStyles {
    public enum Style {
        // Burnt to ash on the spot: no body is left.
        ASH
    }

    // How many ticks a mark holds: a blow that kills later (burning on) no longer counts.
    private static final long FRESH = 10;
    private static final Map<LivingEntity, long[]> MARKS = new WeakHashMap<>();

    private DeathStyles() {
    }

    public static void mark(LivingEntity victim, Style style) {
        if (!victim.level().isClientSide()) {
            MARKS.put(victim, new long[] { style.ordinal(), victim.level().getGameTime() });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        long[] mark = MARKS.remove(victim);
        if (event.isCanceled() || mark == null || victim.level().isClientSide()
                || victim.level().getGameTime() - mark[1] > FRESH) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim, new DeathStylePayload(victim.getId(),
                (int) mark[0]));
    }

    public static void clear() {
        MARKS.clear();
    }
}
