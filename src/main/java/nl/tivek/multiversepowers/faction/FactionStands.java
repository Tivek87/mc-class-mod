package nl.tivek.multiversepowers.faction;

import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// How one creature stands to another or to a player, the one rule for server and client. Toward a player, most
// its own first: the creature's own behaviour, what the player is to its faction (a member, a standing pinned for
// them, how its faction stands to theirs, a reputation that crossed a threshold), the Mobs page's line for its kind,
// then its faction's attitude. Toward a creature: the Mobs page's line for the two kinds, how its faction stands to
// the other's, then the Mobs page's wider lines. Null wherever nothing says: the game's own then.
public final class FactionStands {
    // What a player earned with a faction and a standing pinned for them, as this side knows them.
    public interface Source {
        int earned(Player player, String faction);

        @Nullable
        Standing pin(Player player, String faction);
    }

    private FactionStands() {
    }

    @Nullable
    public static Standing of(Mob actor, LivingEntity target, Source source) {
        if (target instanceof Player player) {
            Standing own = MobFactions.stance(actor);
            if (own != null) {
                return own;
            }
            Faction faction = MobFactions.of(actor);
            Standing personal = faction == null ? null : personal(faction, player, source);
            if (personal != null) {
                return personal;
            }
            Standing line = MobRules.set(actor.getType(), EntityType.PLAYER);
            return line != null || faction == null ? line : faction.zone(0);
        }
        Standing line = MobRules.line(actor.getType(), target.getType());
        if (line != null) {
            return line;
        }
        Faction mine = MobFactions.of(actor);
        Faction theirs = mine != null && target instanceof Mob ? MobFactions.of(target) : null;
        Standing relation = theirs == null ? null : FactionRules.relation(mine.id(), theirs.id());
        return relation != null ? relation : MobRules.wider(actor.getType(), target.getType());
    }

    // What `player` is to `faction` beyond its attitude to anyone, or null.
    @Nullable
    public static Standing personal(Faction faction, Player player, Source source) {
        PlayerTeam team = player.getTeam();
        String own = team == null ? null : team.getName();
        return personal(faction, own, source.pin(player, faction.id()),
                own == null ? null : FactionRules.relation(faction.id(), own),
                FactionRules.reputation() ? source.earned(player, faction.id()) : 0);
    }

    @Nullable
    static Standing personal(Faction faction, @Nullable String team, @Nullable Standing pin,
            @Nullable Standing toTeam, int earned) {
        if (faction.id().equals(team)) {
            return Standing.FRIENDLY;
        }
        if (pin != null) {
            return pin;
        }
        if (toTeam != null) {
            return toTeam;
        }
        Standing now = faction.zone(earned);
        return now != faction.zone(0) ? now : null;
    }

    // How `faction` stands to `player` all told: null where each of its creatures does as the game has it.
    @Nullable
    public static Standing toward(Faction faction, Player player, Source source) {
        Standing personal = personal(faction, player, source);
        return personal != null ? personal : faction.zone(0);
    }
}
