package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;

// A player a power knocks down, or a blow or blast throws hard, goes limp as a creature does (Knockdowns): in every
// game near, their own seen from outside too, the body flies, lies 3 seconds where it lands and gets up. Their own
// game moves them and holds back their keys meanwhile (Downed); here they only count as down, refused every power, and
// the games near are told when they land and how long they lie. In water or lava they are let go at once, so they can
// swim out.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class PlayerKnockdowns {
    private static final Map<UUID, Down> DOWNED = new HashMap<>();
    private static final List<Consumer<ServerPlayer>> LISTENERS = new ArrayList<>();

    private static final class Down {
        int age;
        int thrown;
        int landed = -1;
    }

    private PlayerKnockdowns() {
    }

    // Told of each player knocked down, so what their powers keep up (flight, a raised shield) ends.
    public static void listen(Consumer<ServerPlayer> listener) {
        LISTENERS.add(listener);
    }

    public static boolean isDown(ServerPlayer player) {
        return DOWNED.containsKey(player.getUUID());
    }

    static void knock(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.isInWater()
                || player.isInLava()) {
            return;
        }
        player.stopRiding();
        player.stopFallFlying();
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        for (Consumer<ServerPlayer> listener : LISTENERS) {
            listener.accept(player);
        }
        Down down = DOWNED.get(player.getUUID());
        if (down != null) {
            down.thrown = down.age;
            down.landed = -1;
            tell(player, Knockdowns.FLYING);
            return;
        }
        Down mine = new Down();
        DOWNED.put(player.getUUID(), mine);
        tell(player, Knockdowns.FLYING);
        Effects.start(player.serverLevel(), (level, age) -> {
            if (DOWNED.get(player.getUUID()) != mine) {
                return false;
            }
            if (!player.isAlive() || player.isRemoved() || player.level() != level || player.isSpectator()
                    || player.isCreative() || player.isInWater() || player.isInLava()) {
                up(player, mine);
                return false;
            }
            mine.age = age;
            if (mine.landed < 0) {
                int flight = age - mine.thrown;
                if (flight > 1 && grounded(player) || flight > Knockdowns.LONGEST_FLIGHT) {
                    mine.landed = age;
                    tell(player, Knockdowns.DOWN);
                }
                return true;
            }
            if (age - mine.landed >= Knockdowns.DOWN) {
                up(player, mine);
                return false;
            }
            return true;
        });
    }

    // The server's onGround() of a player runs a tick ahead of their game: what lies right under their box says more.
    private static boolean grounded(ServerPlayer player) {
        return !player.level().noCollision(player, player.getBoundingBox().move(0.0, -0.06, 0.0));
    }

    private static void up(ServerPlayer player, Down down) {
        if (DOWNED.remove(player.getUUID(), down)) {
            tell(player, 0);
        }
    }

    private static void tell(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new KnockdownPayload(player.getId(), ticks));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            Down down = DOWNED.get(target.getUUID());
            if (down != null) {
                int left = down.landed < 0 ? Knockdowns.FLYING
                        : Math.max(1, Knockdowns.DOWN - (down.age - down.landed));
                PacketDistributor.sendToPlayer(viewer, new KnockdownPayload(target.getId(), left));
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DOWNED.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        DOWNED.clear();
    }
}
