package nl.tivek.multiversepowers.killconfirm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// Whoever kills something is told so, whatever did it (a sword, an arrow, a power, a fall after their blow), and their
// game marks the kill at the crosshair (client.KillMarker); each blow of theirs that lands is told too, at most once a
// tick, for the crosshair to flash.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class KillConfirms {
    // The tick each player was last told of a hit.
    private static final Map<UUID, Integer> TOLD = new HashMap<>();

    private KillConfirms() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim instanceof ArmorStand || event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim
                || victim.isDeadOrDying()) {
            return;
        }
        int now = attacker.server.getTickCount();
        Integer last = TOLD.put(attacker.getUUID(), now);
        if (last == null || last != now) {
            PacketDistributor.sendToPlayer(attacker, KillConfirmPayload.HIT);
        }
    }

    public static void clear() {
        TOLD.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || victim.level().isClientSide() || victim instanceof ArmorStand) {
            return;
        }
        ServerPlayer killer = killer(event.getSource(), victim);
        if (killer != null && killer != victim) {
            PacketDistributor.sendToPlayer(killer, KillConfirmPayload.KILL);
        }
    }

    // Who struck the blow, else who gets the kill (the one who hit it last, for a fall or a burn that followed).
    @Nullable
    private static ServerPlayer killer(DamageSource source, LivingEntity victim) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return victim.getKillCredit() instanceof ServerPlayer player ? player : null;
    }
}
