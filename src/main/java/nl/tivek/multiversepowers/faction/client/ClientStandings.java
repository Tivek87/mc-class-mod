package nl.tivek.multiversepowers.faction.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.Standings;
import nl.tivek.multiversepowers.faction.StandingsPayload;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientStandings {
    private static final Set<String> ALLIES = new HashSet<>();
    private static final Set<String> ENEMIES = new HashSet<>();
    private static final Map<UUID, Long> GRUDGES = new HashMap<>();

    private static final Standings.View VIEW = new Standings.View() {
        @Override
        public Standings.Relation relation(Player viewer, String team, String other) {
            String pair = Standings.pair(team, other);
            return ALLIES.contains(pair) ? Standings.Relation.ALLY
                    : ENEMIES.contains(pair) ? Standings.Relation.ENEMY : Standings.Relation.NONE;
        }

        @Override
        public boolean grudge(Player viewer, UUID entity) {
            Long until = GRUDGES.get(entity);
            return until != null && until >= viewer.level().getGameTime();
        }

        @Override
        public boolean angry(Mob mob, Player viewer) {
            return mob.isAggressive();
        }

        @Override
        @Nullable
        public Player player(Player viewer, UUID id) {
            return viewer.level().getPlayerByUUID(id);
        }
    };

    private ClientStandings() {
    }

    public static void update(StandingsPayload payload) {
        ALLIES.clear();
        ALLIES.addAll(payload.allies());
        ENEMIES.clear();
        ENEMIES.addAll(payload.enemies());
        GRUDGES.clear();
        for (int i = 0; i < payload.grudges().size() && i < payload.until().size(); i++) {
            GRUDGES.put(payload.grudges().get(i), payload.until().get(i));
        }
    }

    public static Standing of(Entity entity) {
        LocalPlayer self = Minecraft.getInstance().player;
        return self == null ? Standing.NEUTRAL : Standings.of(self, entity, VIEW);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ALLIES.clear();
        ENEMIES.clear();
        GRUDGES.clear();
    }
}
