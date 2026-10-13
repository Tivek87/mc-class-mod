package nl.tivek.multiversepowers.faction;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

public final class Standings {
    public static final int GRUDGE_HITS = 1;
    public static final long GRUDGE_TICKS = 6000L;

    public interface View {
        boolean grudge(Player viewer, UUID entity);

        boolean angry(Mob mob, Player viewer);

        @Nullable
        Player player(Player viewer, UUID id);

        // How `mob` stands to the viewer by its faction and the Mobs page (`FactionStands`), or null.
        @Nullable
        Standing stand(Mob mob, Player viewer);
    }

    private Standings() {
    }

    public static Standing of(Player viewer, Entity entity, View view) {
        if (entity == viewer) {
            return Standing.FRIENDLY;
        }
        if (entity instanceof Player player) {
            Standing side = between(viewer, player);
            return side == Standing.NEUTRAL && view.grudge(viewer, player.getUUID()) ? Standing.HOSTILE : side;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            if (pet.getOwnerUUID().equals(viewer.getUUID())) {
                return Standing.FRIENDLY;
            }
            Player owner = view.player(viewer, pet.getOwnerUUID());
            Standing side = owner == null ? Standing.NEUTRAL : of(viewer, owner, view);
            if (side != Standing.NEUTRAL) {
                return side;
            }
        }
        Standing stand = entity instanceof Mob mob ? view.stand(mob, viewer) : null;
        if (view.grudge(viewer, entity.getUUID()) || stand == Standing.HOSTILE
                || entity instanceof Mob mob && view.angry(mob, viewer)) {
            return Standing.HOSTILE;
        }
        if (stand != null) {
            return stand;
        }
        return entity instanceof Enemy ? Standing.HOSTILE : Standing.NEUTRAL;
    }

    // Two players by their factions: hostile where either faction is to the other, friendly where either is.
    public static Standing between(Player viewer, Player other) {
        PlayerTeam mine = viewer.getTeam();
        PlayerTeam theirs = other.getTeam();
        if (mine == null || theirs == null) {
            return Standing.NEUTRAL;
        }
        if (mine == theirs) {
            return Standing.FRIENDLY;
        }
        Standing to = FactionRules.relation(mine.getName(), theirs.getName());
        Standing from = FactionRules.relation(theirs.getName(), mine.getName());
        if (to == Standing.HOSTILE || from == Standing.HOSTILE) {
            return Standing.HOSTILE;
        }
        return to == Standing.FRIENDLY || from == Standing.FRIENDLY ? Standing.FRIENDLY : Standing.NEUTRAL;
    }
}
