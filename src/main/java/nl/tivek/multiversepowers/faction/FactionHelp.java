package nl.tivek.multiversepowers.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import nl.tivek.multiversepowers.faction.rules.FactionRules.Rule;

// A faction's creatures come to the aid of their own: when a member of a faction at peace with itself, a creature of
// a faction it is friendly to, or a player it is friendly with is struck, those near that can fight and have no fight
// of their own go after the attacker, unless that one is a friend too; and when a player of their own faction strikes
// something that fights, they go after that. `MobTargets` lets them keep that target.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class FactionHelp {
    private static final long CALL_EVERY = 20L;
    private static final long HELP_TICKS = 600L;
    private static final int MOST = 8;

    private record Aid(UUID target, long until) {
    }

    private static final Map<UUID, Aid> HELPING = new HashMap<>();
    private static final Map<UUID, Long> CALLED = new HashMap<>();

    private FactionHelp() {
    }

    // Whether `helper` is after `target` to help a friend.
    public static boolean helping(LivingEntity helper, LivingEntity target) {
        Aid aid = HELPING.get(helper.getUUID());
        return aid != null && aid.target().equals(target.getUUID())
                && helper.level().getGameTime() <= aid.until();
    }

    // Whether `helper` has been after `target` for a friend long enough: it lets go then.
    public static boolean done(Mob helper, LivingEntity target) {
        Aid aid = HELPING.get(helper.getUUID());
        if (aid == null || helper.level().getGameTime() <= aid.until() && aid.target().equals(target.getUUID())) {
            return false;
        }
        HELPING.remove(helper.getUUID());
        return aid.target().equals(target.getUUID());
    }

    // Asked as the blow comes in, so one a shield or armour stops calls for help as well.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onStruck(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        int range = Rule.HELP_RANGE.get();
        if (range <= 0 || event.getAmount() <= 0.0F || !(victim.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim
                || !attacker.isAlive() || attacker instanceof Player player && (player.isCreative()
                        || player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL)) {
            return;
        }
        long now = level.getGameTime();
        Long called = CALLED.get(victim.getUUID());
        if (called != null && now - called < CALL_EVERY && now >= called) {
            return;
        }
        if (CALLED.size() > 1024) {
            CALLED.clear();
            HELPING.values().removeIf(aid -> aid.until() < now);
        }
        CALLED.put(victim.getUUID(), now);
        Faction side = victim instanceof Mob ? MobFactions.of(victim) : null;
        if (side != null || victim instanceof Player) {
            call(level, victim, attacker, range, now, own -> victim instanceof Player player
                    ? FactionStands.toward(own, player, Reputation.SOURCE) == Standing.FRIENDLY
                    : FactionRules.relation(own.id(), side.id()) == Standing.FRIENDLY);
        }
        // A member's own fight: their faction joins in against what they strike, if it is something that fights.
        Faction band = attacker instanceof Player striker && fights(victim) ? Factions.of(striker) : null;
        if (band != null) {
            call(level, attacker, victim, range, now, own -> own.id().equals(band.id()));
        }
    }

    // Those near `friend` that stand by it (`stands`, asked of their faction) go after `foe`.
    private static void call(ServerLevel level, LivingEntity friend, LivingEntity foe, int range, long now,
            Predicate<Faction> stands) {
        List<Mob> near = level.getEntitiesOfClass(Mob.class, friend.getBoundingBox().inflate(range, range / 2.0, range),
                mob -> mob != friend && mob != foe && helps(mob, friend, foe, stands));
        for (int i = 0; i < near.size() && i < MOST; i++) {
            Mob helper = near.get(i);
            HELPING.put(helper.getUUID(), new Aid(foe.getUUID(), now + HELP_TICKS));
            helper.setTarget(foe);
            if (helper.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
                helper.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, foe);
            }
        }
    }

    private static boolean fights(LivingEntity entity) {
        return entity instanceof Player || entity instanceof RangedAttackMob
                || entity instanceof Mob && entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    // Seeing either of the two is enough: the friend it stands by, or the one that struck.
    private static boolean helps(Mob mob, LivingEntity friend, LivingEntity foe, Predicate<Faction> stands) {
        if (!mob.isAlive() || mob.isNoAi() || mob.getTarget() != null && mob.getTarget().isAlive() || !fights(mob)) {
            return false;
        }
        Faction own = MobFactions.of(mob);
        return own != null && own.help() && stands.test(own)
                && FactionStands.of(mob, foe, Reputation.SOURCE) != Standing.FRIENDLY && mob.canAttack(foe)
                && (mob.getSensing().hasLineOfSight(foe) || mob.getSensing().hasLineOfSight(friend));
    }

    static void clear() {
        HELPING.clear();
        CALLED.clear();
    }
}
