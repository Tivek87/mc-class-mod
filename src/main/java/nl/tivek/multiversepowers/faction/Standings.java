package nl.tivek.multiversepowers.faction;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

public final class Standings {
    public static final int GRUDGE_HITS = 3;
    public static final long GRUDGE_TICKS = 6000L;

    public enum Relation {
        NONE,
        ALLY,
        ENEMY
    }

    public interface View {
        Relation relation(Player viewer, String team, String other);

        boolean grudge(Player viewer, UUID entity);

        boolean angry(Mob mob, Player viewer);

        @Nullable
        Player player(Player viewer, UUID id);
    }

    private Standings() {
    }

    public static Standing of(Player viewer, Entity entity, View view) {
        if (entity == viewer) {
            return Standing.FRIENDLY;
        }
        if (entity instanceof Player player) {
            Standing side = between(viewer, player, view);
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
        if (view.grudge(viewer, entity.getUUID()) || entity instanceof Enemy
                || entity instanceof Mob mob && view.angry(mob, viewer)) {
            return Standing.HOSTILE;
        }
        return Standing.NEUTRAL;
    }

    public static Standing between(Player viewer, Player other, View view) {
        PlayerTeam mine = viewer.getTeam();
        PlayerTeam theirs = other.getTeam();
        if (mine == null || theirs == null) {
            return Standing.NEUTRAL;
        }
        if (mine == theirs) {
            return Standing.FRIENDLY;
        }
        return switch (view.relation(viewer, mine.getName(), theirs.getName())) {
            case ALLY -> Standing.FRIENDLY;
            case ENEMY -> Standing.HOSTILE;
            case NONE -> Standing.NEUTRAL;
        };
    }

    public static String pair(String team, String other) {
        return team.compareTo(other) < 0 ? team + "|" + other : other + "|" + team;
    }
}
