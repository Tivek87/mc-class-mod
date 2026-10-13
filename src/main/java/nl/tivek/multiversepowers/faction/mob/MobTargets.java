package nl.tivek.multiversepowers.faction.mob;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.summon.Summons;
import nl.tivek.multiversepowers.faction.FactionHelp;
import nl.tivek.multiversepowers.faction.FactionStands;
import nl.tivek.multiversepowers.faction.Factions;
import nl.tivek.multiversepowers.faction.Reputation;
import nl.tivek.multiversepowers.faction.Standing;

// Holds creatures to how they stand (`FactionStands`: their faction, a player's reputation with it, the Mobs page):
// one that is friendly never takes the target, not even struck by it by accident (its anger and the grudge in its
// memory are let go too) and does it no harm, one that is neutral never hunts it unless provoked, and one that is
// hostile hunts it (every creature carries a goal for that, idle until something sets a hunt for its kind or faction,
// and one that can strike but has no attack of its own a melee attack beside it). A target no longer allowed, the
// world's settings or a reputation changed meanwhile, is let go. One helping a friend (`FactionHelp`) keeps its target.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class MobTargets {
    private MobTargets() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity actor = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || actor.level().isClientSide()) {
            return;
        }
        if (forbidden(actor, target)) {
            event.setCanceled(true);
        }
    }

    // A creature the world keeps from spawning by itself does not come as the world spawns creatures, and one with a
    // spawn chance comes only that share of the times.
    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        MobSpawnType how = event.getSpawnType();
        boolean byItself = how == MobSpawnType.NATURAL || how == MobSpawnType.CHUNK_GENERATION
                || how == MobSpawnType.STRUCTURE || how == MobSpawnType.PATROL || how == MobSpawnType.REINFORCEMENT
                || how == MobSpawnType.JOCKEY;
        EntityType<?> type = event.getEntity().getType();
        if (byItself && (!MobRules.spawns(type) || event.getEntity().getRandom().nextDouble() * 100.0
                >= MobStats.value(type, MobStats.Stat.SPAWN_CHANCE))) {
            event.setSpawnCancelled(true);
        }
    }

    // Every tick: a creature run by its brain (a piglin, a hoglin) can take a target past the event. A friend's blow is
    // forgiven here, after the creature's own goals ran: vanilla's goals still read who hurt it while they start.
    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        LivingEntity hurtBy = mob.getLastHurtByMob();
        if (hurtBy != null && friendly(mob, hurtBy)) {
            calm(mob, hurtBy);
        }
        if (mob instanceof NeutralMob neutral && neutral.getPersistentAngerTarget() != null
                && mob.level() instanceof ServerLevel level
                && level.getEntity(neutral.getPersistentAngerTarget()) instanceof LivingEntity angry
                && friendly(mob, angry)) {
            calm(mob, angry);
        }
        LivingEntity target = mob.getTarget();
        if (target != null && FactionHelp.done(mob, target)) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return;
        }
        if (target != null && forbidden(mob, target)) {
            if (friendly(mob, target)) {
                calm(mob, target);
            }
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    // How `actor` stands to `target` where the world says, else null.
    @Nullable
    private static Standing stand(LivingEntity actor, LivingEntity target) {
        return actor instanceof Mob mob ? FactionStands.of(mob, target, Reputation.SOURCE)
                : MobRules.set(actor.getType(), target.getType());
    }

    private static boolean friendly(Mob mob, LivingEntity target) {
        return !Summons.owned(mob) && stand(mob, target) == Standing.FRIENDLY;
    }

    // A friend does no harm: not by a blast, a touch or an arrow gone astray either.
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Mob mob && mob != event.getEntity()
                && !mob.level().isClientSide() && friendly(mob, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // A friend that struck it by accident: no anger, no grudge, nothing that would call it back to the fight.
    private static void calm(Mob mob, LivingEntity friend) {
        if (mob.getLastHurtByMob() == friend) {
            mob.setLastHurtByMob(null);
        }
        if (mob instanceof NeutralMob neutral && friend.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.setPersistentAngerTarget(null);
            neutral.setRemainingPersistentAngerTime(0);
        }
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT)
                && mob.getBrain().getMemory(MemoryModuleType.ANGRY_AT).filter(friend.getUUID()::equals).isPresent()) {
            mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    // A Green Lantern summon goes by its owner's fight, never by these rules. Asked too as a creature looks for
    // someone to attack or to run from (`mixin/TargetingConditionsMixin`). Neutral holds back a creature that would
    // hunt the target by its nature until it is provoked; one that is neutral by nature (an enderman, a wolf, a
    // golem) keeps its own triggers.
    public static boolean forbidden(LivingEntity actor, LivingEntity target) {
        if (Summons.owned(actor)) {
            return false;
        }
        Standing set = stand(actor, target);
        return set == Standing.FRIENDLY || set == Standing.NEUTRAL && hunter(actor, target)
                && actor.getLastHurtByMob() != target && target.getLastHurtMob() != actor
                && !(target instanceof Mob mob && mob.getTarget() == actor) && !FactionHelp.helping(actor, target);
    }

    // Whether `actor` attacks `target`'s kind on sight by its nature: the game's own target goals.
    private static boolean hunter(LivingEntity actor, LivingEntity target) {
        return actor.level() instanceof ServerLevel level
                && MobDefaults.of(level, actor.getType(), target.getType()) == Standing.HOSTILE;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || Summons.owned(mob)) {
            return;
        }
        for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof Hunt) {
                return;
            }
        }
        mob.targetSelector.addGoal(3, new Hunt(mob));
        if (mob instanceof PathfinderMob walker && !(mob instanceof RangedAttackMob)
                && mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                && mob.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof MeleeAttackGoal)) {
            mob.goalSelector.addGoal(2, new Strike(walker));
        }
    }

    private static boolean hostile(Mob mob, LivingEntity target) {
        return stand(mob, target) == Standing.HOSTILE;
    }

    // Whether `mob` may strike `target`: set to hunt it, or after it to help a friend.
    private static boolean after(Mob mob, LivingEntity target) {
        return hostile(mob, target) || FactionHelp.helping(mob, target);
    }

    private static final class Hunt extends NearestAttackableTargetGoal<LivingEntity> {
        private Hunt(Mob mob) {
            super(mob, LivingEntity.class, 10, true, false, target -> hostile(mob, target) && !hunter(mob, target));
        }

        @Override
        public boolean canUse() {
            return (MobRules.hunts(this.mob.getType()) || Factions.hunts(this.mob)) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && hostile(this.mob, target) && super.canContinueToUse();
        }

        // Let go as the world no longer sets it hostile, it holds no grudge of its own from the hunt either.
        @Override
        public void stop() {
            LivingEntity target = this.mob.getTarget();
            if (target != null && !hostile(this.mob, target) && this.mob instanceof NeutralMob neutral) {
                neutral.stopBeingAngry();
            }
            super.stop();
        }
    }

    private static final class Strike extends MeleeAttackGoal {
        private Strike(PathfinderMob mob) {
            super(mob, 1.2, true);
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && after(this.mob, target) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && after(this.mob, target) && super.canContinueToUse();
        }
    }
}
