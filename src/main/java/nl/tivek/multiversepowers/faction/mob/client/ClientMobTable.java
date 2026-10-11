package nl.tivek.multiversepowers.faction.mob.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.mob.MobTablePayload;

// The server's `MobTablePayload`, for the Mobs page: every creature and the game's own standings between them.
public final class ClientMobTable {
    @Nullable
    private static MobTablePayload table;
    private static List<EntityType<?>> types = List.of();

    private ClientMobTable() {
    }

    public static void accept(MobTablePayload payload) {
        List<EntityType<?>> known = new ArrayList<>();
        for (String id : payload.types()) {
            known.add(MobRules.type(id));
        }
        types = known;
        table = payload;
    }

    public static boolean ready() {
        return table != null;
    }

    public static void forget() {
        table = null;
        types = List.of();
    }

    // Every creature, in the server's order; one this game does not know is null.
    static List<EntityType<?>> types() {
        return types;
    }

    // The game's own standing of creature `actor` (an index of `types`) toward `target` (the player for -1).
    static Standing of(int actor, int target) {
        return table == null ? Standing.FRIENDLY : Standing.values()[table.standings()[table.at(actor, target + 1)]];
    }
}
