package nl.tivek.multiversepowers.faction.mob;

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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.summon.Summons;
import nl.tivek.multiversepowers.faction.Standing;

// Holds creatures to `MobRules`: one set friendly never takes the target, one set neutral only when it was hit by it,
// and one set hostile hunts it (every creature carries a goal for that, idle until the world sets a hunt for its kind,
// and one that can strike but has no attack of its own a melee attack beside it). A target no longer allowed, the
// world's settings changed meanwhile, is let go.
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

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % 20 != 0 || !(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide()) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && forbidden(mob, target)) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    // A Green Lantern summon goes by its owner's fight, never by these rules.
    private static boolean forbidden(LivingEntity actor, LivingEntity target) {
        if (Summons.owned(actor)) {
            return false;
        }
        Standing set = MobRules.set(actor.getType(), target.getType());
        return set == Standing.FRIENDLY || set == Standing.NEUTRAL && actor.getLastHurtByMob() != target
                && target.getLastHurtMob() != actor && !(target instanceof Mob mob && mob.getTarget() == actor);
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
        return MobRules.set(mob.getType(), target.getType()) == Standing.HOSTILE;
    }

    private static final class Hunt extends NearestAttackableTargetGoal<LivingEntity> {
        private Hunt(Mob mob) {
            super(mob, LivingEntity.class, 10, true, false, target -> hostile(mob, target));
        }

        @Override
        public boolean canUse() {
            return MobRules.hunts(this.mob.getType()) && super.canUse();
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
            return target != null && hostile(this.mob, target) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && hostile(this.mob, target) && super.canContinueToUse();
        }
    }
}
