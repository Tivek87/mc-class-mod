package nl.tivek.multiversepowers.character.greenlantern.summon;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import nl.tivek.multiversepowers.faction.Factions;

// A summon's mind: its own moves stay (a skeleton's bow, a creeper's fuse, a golem's swing), but what it goes after
// is its owner's fight, and between fights it keeps near him.
final class SummonGoals {
    // Past this it walks back to its owner, past FAR it steps to his side at once.
    private static final double NEAR = 7.0;
    private static final double FAR = 24.0;

    private SummonGoals() {
    }

    static void take(Mob mob) {
        mob.targetSelector.removeAllGoals(goal -> true);
        // An enderman lifting and setting down blocks: a summon leaves the world alone.
        mob.goalSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().contains("Block"));
        mob.targetSelector.addGoal(1, new Retaliate(mob));
        mob.targetSelector.addGoal(2, new Guard(mob));
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false,
                living -> {
                    ServerPlayer owner = Summons.owner(mob);
                    return owner != null && Factions.hostile(owner, living) && Summons.fair(owner, living);
                }));
        if (mob instanceof PathfinderMob walker) {
            mob.goalSelector.addGoal(5, new Follow(walker));
        }
    }

    // Strikes back at what hurt it, unless that was its own side.
    private static final class Retaliate extends TargetGoal {
        private int seen;

        Retaliate(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity by = this.mob.getLastHurtByMob();
            ServerPlayer owner = Summons.owner(this.mob);
            return by != null && this.mob.getLastHurtByMobTimestamp() != this.seen
                    && (owner == null ? by.isAlive() : Summons.fair(owner, by));
        }

        @Override
        public void start() {
            this.mob.setTarget(this.mob.getLastHurtByMob());
            this.seen = this.mob.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    // Turns on what hurt its owner, or on what its owner struck.
    private static final class Guard extends TargetGoal {
        private LivingEntity foe;
        private int seenHurt;
        private int seenHit;

        Guard(Mob mob) {
            super(mob, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = Summons.owner(this.mob);
            if (owner == null) {
                return false;
            }
            LivingEntity by = owner.getLastHurtByMob();
            if (by != null && owner.getLastHurtByMobTimestamp() != this.seenHurt && Summons.fair(owner, by)) {
                this.foe = by;
                return true;
            }
            LivingEntity hit = owner.getLastHurtMob();
            if (hit != null && owner.getLastHurtMobTimestamp() != this.seenHit && Summons.fair(owner, hit)) {
                this.foe = hit;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            ServerPlayer owner = Summons.owner(this.mob);
            if (owner != null) {
                this.seenHurt = owner.getLastHurtByMobTimestamp();
                this.seenHit = owner.getLastHurtMobTimestamp();
            }
            this.mob.setTarget(this.foe);
            super.start();
        }
    }

    // Between fights it walks after its owner, and steps to his side when he is far.
    private static final class Follow extends Goal {
        private final PathfinderMob mob;
        private int repath;

        Follow(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = Summons.owner(this.mob);
            return owner != null && this.mob.getTarget() == null && this.mob.distanceToSqr(owner) > NEAR * NEAR;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = Summons.owner(this.mob);
            return owner != null && this.mob.getTarget() == null && this.mob.distanceToSqr(owner) > 3.0 * 3.0;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer owner = Summons.owner(this.mob);
            if (owner == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(owner, 10.0F, this.mob.getMaxHeadXRot());
            if (this.mob.distanceToSqr(owner) > FAR * FAR && owner.onGround()) {
                this.mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                this.mob.getNavigation().stop();
                return;
            }
            if (--this.repath <= 0) {
                this.repath = 10;
                this.mob.getNavigation().moveTo(owner, 1.15);
            }
        }
    }
}
