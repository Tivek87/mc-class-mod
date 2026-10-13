package nl.tivek.multiversepowers.faction.client;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.FactionStands;
import nl.tivek.multiversepowers.faction.net.FactionStatePayload;
import nl.tivek.multiversepowers.faction.net.MobFactionPayload;
import nl.tivek.multiversepowers.faction.MobFactions;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// What this game knows of its player and the factions: what they earned with each, the standings pinned for them,
// and the creatures with a faction or behaviour of their own that the server told of.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientFactions {
    private static final Map<String, Integer> EARNED = new HashMap<>();
    private static final Map<String, Standing> PINS = new HashMap<>();
    private static final Map<Integer, MobFactions.Own> MOBS = new HashMap<>();
    private static int steps;

    // Only this game's own player has a reputation here: anyone else counts as new to every faction.
    public static final FactionStands.Source SOURCE = new FactionStands.Source() {
        @Override
        public int earned(Player player, String faction) {
            return player == Minecraft.getInstance().player ? EARNED.getOrDefault(faction, 0) : 0;
        }

        @Override
        @Nullable
        public Standing pin(Player player, String faction) {
            return player == Minecraft.getInstance().player ? PINS.get(faction) : null;
        }
    };

    static {
        MobFactions.told = entity -> MOBS.get(entity.getId());
    }

    private ClientFactions() {
    }

    public static void state(FactionStatePayload payload) {
        EARNED.clear();
        EARNED.putAll(payload.earned());
        PINS.clear();
        payload.pins().forEach((faction, standing) -> {
            if (standing >= 0 && standing < Standing.values().length) {
                PINS.put(faction, Standing.values()[standing]);
            }
        });
        steps = payload.steps();
    }

    public static void mob(MobFactionPayload payload) {
        Standing stance = payload.stance() >= 0 && payload.stance() < Standing.values().length
                ? Standing.values()[payload.stance()] : null;
        if (payload.faction().isEmpty() && stance == null) {
            MOBS.remove(payload.entity());
        } else {
            MOBS.put(payload.entity(), new MobFactions.Own(payload.faction(), stance));
        }
    }

    public static int steps() {
        return steps;
    }

    // The player's reputation with `faction`, on its scale.
    public static int reputation(Faction faction) {
        return faction.reputation(FactionRules.reputation() ? EARNED.getOrDefault(faction.id(), 0) : 0);
    }

    // How `faction` stands to the player; null where each of its creatures does as the game has it.
    @Nullable
    public static Standing toward(Faction faction) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? faction.zone(0) : FactionStands.toward(faction, player, SOURCE);
    }

    public static boolean pinned(Faction faction) {
        return PINS.containsKey(faction.id());
    }

    // The faction the player is in, if the world has one by their team's name.
    @Nullable
    public static Faction mine() {
        LocalPlayer player = Minecraft.getInstance().player;
        PlayerTeam team = player == null ? null : player.getTeam();
        return team == null ? null : FactionRules.get(team.getName());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            MOBS.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EARNED.clear();
        PINS.clear();
        MOBS.clear();
        steps = 0;
    }
}
