package nl.tivek.multiversepowers.character.greenlantern.summon;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import nl.tivek.multiversepowers.faction.Factions;

// A summon's mind: its own moves stay (a skeleton's bow, a creeper's fuse, a golem's swing), but what it goes after
// is its owner's fight, and between fights it keeps near him, or round the place he told it to stay.
final class SummonGoals {

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
                    return owner != null && Factions.hostile(owner, living) && Summons.fair(owner, living)
                            && reaches(mob, living);
                }));
        if (mob instanceof PathfinderMob walker) {
            // Before its own attack, which it breaks off to go back to the marked place.
            mob.goalSelector.addGoal(0, new Hold(walker));
            mob.goalSelector.addGoal(5, new Follow(walker));
        }
    }

    // Whether `foe` is near enough the place its owner told it to stay to go after; anywhere while it follows him.
    private static boolean reaches(Mob mob, LivingEntity foe) {
        Summons.Anchor anchor = Summons.anchor(mob);
        return anchor == null || !Summons.outside(anchor, foe.position(), Summons.STAY + Summons.CHASE);
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
                    && (owner == null ? by.isAlive() : Summons.fair(owner, by)) && reaches(this.mob, by);
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
            if (by != null && owner.getLastHurtByMobTimestamp() != this.seenHurt && Summons.fair(owner, by)
                    && reaches(this.mob, by)) {
                this.foe = by;
                return true;
            }
            LivingEntity hit = owner.getLastHurtMob();
            if (hit != null && owner.getLastHurtMobTimestamp() != this.seenHit && Summons.fair(owner, hit)
                    && reaches(this.mob, hit)) {
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

    // Told to stay, it keeps within STAY of the marked place: strayed out of it, or chasing a foe further than CHASE
    // beyond it (let go), it walks back to the middle.
    private static final class Hold extends Goal {
        private final PathfinderMob mob;
        private int repath;

        Hold(PathfinderMob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Summons.Anchor anchor = Summons.anchor(this.mob);
            if (anchor == null) {
                return false;
            }
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                return Summons.outside(anchor, this.mob.position(), Summons.STAY + Summons.CHASE)
                        || !reaches(this.mob, target);
            }
            return Summons.outside(anchor, this.mob.position(), Summons.STAY);
        }

        @Override
        public boolean canContinueToUse() {
            Summons.Anchor anchor = Summons.anchor(this.mob);
            return anchor != null && this.mob.getTarget() == null && Summons.outside(anchor, this.mob.position(), 1.0);
        }

        @Override
        public void start() {
            this.mob.setTarget(null);
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            Summons.Anchor anchor = Summons.anchor(this.mob);
            if (anchor != null && --this.repath <= 0) {
                this.repath = 10;
                this.mob.getNavigation().moveTo(anchor.at().x, anchor.at().y, anchor.at().z, 1.15);
            }
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
            return owner != null && Summons.anchor(this.mob) == null && this.mob.getTarget() == null
                    && this.mob.distanceToSqr(owner) > Mth.square(Summons.value("summonFollowBlocks"));
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer owner = Summons.owner(this.mob);
            return owner != null && Summons.anchor(this.mob) == null && this.mob.getTarget() == null
                    && this.mob.distanceToSqr(owner) > 3.0 * 3.0;
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
            if (this.mob.distanceToSqr(owner) > Mth.square(Summons.value("summonTeleportBlocks"))
                    && owner.onGround()) {
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
