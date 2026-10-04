package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.effect.Effects;

// A player a power knocks down, or a blow or blast throws hard, goes limp as a creature does (Knockdowns): in every
// game near, their own seen from outside too, the body flies and lies where it lands, limp at most LIMP ticks from the
// blow, then gets up quickly. Let go still in the air, they fall on as themselves, or fly on if the knockdown ended
// their flight. Their own game moves them and holds back their keys meanwhile (Downed); here they only count as down,
// refused every power, and the games near are told when they land and how long they lie. In water or lava they are
// let go at once, so they can swim out.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class PlayerKnockdowns {
    // Limp at most this long from the blow: 1 second.
    public static final int LIMP = 20;
    // How long a player's body takes to get up in the games near (Ragdolls), and stands before they are let go.
    public static final int RISE = 24;
    public static final int MARGIN = 5;
    // On the ground at least this long, however late they came down.
    private static final int LIES_LEAST = 4;
    // Up again, a player is not knocked down anew for this long: no lying limp blow after blow.
    private static final int SPARED = 60;
    private static final Map<UUID, Down> DOWNED = new HashMap<>();
    private static final Map<UUID, Long> UP_AT = new HashMap<>();
    private static final List<Consumer<ServerPlayer>> LISTENERS = new ArrayList<>();
    private static final List<Predicate<ServerPlayer>> FLYING = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> FLY_AGAIN = new ArrayList<>();

    private static final class Down {
        int age;
        int thrown;
        int landed = -1;
        int free;
        boolean flew;
    }

    private PlayerKnockdowns() {
    }

    // Told of each player knocked down, so what their powers keep up (flight, a raised shield) ends.
    public static void listen(Consumer<ServerPlayer> listener) {
        LISTENERS.add(listener);
    }

    // Asked as a knockdown starts whether a power holds the player up in the air, and told when one let go of still in
    // the air had: they fly on.
    public static void flight(Predicate<ServerPlayer> flying, Consumer<ServerPlayer> again) {
        FLYING.add(flying);
        FLY_AGAIN.add(again);
    }

    public static boolean isDown(ServerPlayer player) {
        return DOWNED.containsKey(player.getUUID());
    }

    // Only while the world lets players go limp (playerKnockdown), so every game shows what the server does.
    static void knock(ServerPlayer player) {
        if (!PowerRules.playerKnockdown() || !player.isAlive() || player.isSpectator() || player.isCreative()
                || player.isInWater() || player.isInLava()) {
            return;
        }
        Long up = UP_AT.get(player.getUUID());
        if (up != null && player.level().getGameTime() - up < SPARED && !DOWNED.containsKey(player.getUUID())) {
            return;
        }
        boolean flying = false;
        for (Predicate<ServerPlayer> flies : FLYING) {
            flying |= flies.test(player);
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
            down.flew |= flying;
            tell(player, Knockdowns.FLYING);
            return;
        }
        Down mine = new Down();
        mine.flew = flying;
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
            // Held by a power (carried, gripped), the knockdown waits: it counts from when they are let go.
            if (HeldMobs.isHeldByAnyone(player)) {
                if (mine.landed >= 0) {
                    tell(player, Knockdowns.FLYING);
                }
                mine.thrown = age;
                mine.landed = -1;
                return true;
            }
            if (mine.landed < 0) {
                int flight = age - mine.thrown;
                if (flight > 1 && grounded(player)) {
                    mine.landed = age;
                    mine.free = Math.max(mine.thrown + LIMP, age + LIES_LEAST) + RISE + MARGIN;
                    tell(player, mine.free - age);
                } else if (flight >= LIMP) {
                    // Still in the air when the knockdown is over: they fall on as themselves, or fly on.
                    up(player, mine);
                    if (mine.flew) {
                        FLY_AGAIN.forEach(again -> again.accept(player));
                    }
                    return false;
                }
                return true;
            }
            if (age >= mine.free) {
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
            UP_AT.put(player.getUUID(), player.level().getGameTime());
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
                int left = down.landed < 0 ? Knockdowns.FLYING : Math.max(1, down.free - down.age);
                PacketDistributor.sendToPlayer(viewer, new KnockdownPayload(target.getId(), left));
            }
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DOWNED.remove(event.getEntity().getUUID());
        UP_AT.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        DOWNED.clear();
        UP_AT.clear();
    }
}
