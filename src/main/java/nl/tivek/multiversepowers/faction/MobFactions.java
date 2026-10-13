package nl.tivek.multiversepowers.faction;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.summon.Summons;
import nl.tivek.multiversepowers.faction.net.MobFactionPayload;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// The faction and the behaviour one creature has of its own, apart from its kind: saved with it, and told to every
// player who sees it. A creature someone owns (a pet, a summon) is in no faction: it goes by its owner.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class MobFactions {
    // A creature kept out of every faction, its kind's too.
    public static final String NONE = "-";
    private static final String FACTION = MultiversePowers.MODID + "_faction";
    private static final String STANCE = MultiversePowers.MODID + "_stance";

    // `faction`: "" for its kind's own. `stance`: how it stands to players, whatever its faction does.
    public record Own(String faction, @Nullable Standing stance) {
    }

    // What the server told this game of a creature, set by the client.
    public static Function<Entity, Own> told = entity -> null;

    private MobFactions() {
    }

    @Nullable
    public static Own own(Entity entity) {
        if (entity.level().isClientSide()) {
            return told.apply(entity);
        }
        if (!free(entity)) {
            return new Own(NONE, null);
        }
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(FACTION) && !data.contains(STANCE)) {
            return null;
        }
        return new Own(data.getString(FACTION), FactionTable.standing(data.getString(STANCE)));
    }

    // Whether the server lets `entity` go by factions at all: no one's pet or summon.
    public static boolean free(Entity entity) {
        return !Summons.owned(entity) && !(entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null);
    }

    // The faction `living` is in: its own, else its kind's.
    @Nullable
    public static Faction of(LivingEntity living) {
        Own own = own(living);
        if (own != null && !own.faction().isEmpty()) {
            return own.faction().equals(NONE) ? null : FactionRules.get(own.faction());
        }
        return FactionRules.of(living.getType());
    }

    // How it stands to players by itself, or null where its faction decides.
    @Nullable
    public static Standing stance(Entity entity) {
        Own own = own(entity);
        return own == null ? null : own.stance();
    }

    // `faction`: an id, `NONE`, or "" for its kind's own again.
    public static void faction(Mob mob, String faction) {
        if (faction.isEmpty()) {
            mob.getPersistentData().remove(FACTION);
        } else {
            mob.getPersistentData().putString(FACTION, faction);
        }
        changed(mob);
    }

    public static void stance(Mob mob, @Nullable Standing stance) {
        if (stance == null) {
            mob.getPersistentData().remove(STANCE);
        } else {
            mob.getPersistentData().putString(STANCE, FactionTable.name(stance));
        }
        changed(mob);
    }

    private static void changed(Mob mob) {
        mob.setTarget(null);
        PacketDistributor.sendToPlayersTrackingEntity(mob, payload(mob, own(mob)));
    }

    private static MobFactionPayload payload(Entity entity, @Nullable Own own) {
        return new MobFactionPayload(entity.getId(), own == null ? "" : own.faction(),
                own == null || own.stance() == null ? -1 : own.stance().ordinal());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Own own = event.getTarget() instanceof Mob ? own(event.getTarget()) : null;
        if (own != null && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, payload(event.getTarget(), own));
        }
    }
}
