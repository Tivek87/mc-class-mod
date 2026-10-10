package nl.tivek.multiversepowers.character.greenlantern.minion;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.faction.Factions;

// What a mech's helper does between its moves: it picks the nearest creature red to its pilot, and with none it walks
// back to the mech's side. A wild one picks the nearest monster.
final class MinionGoals {
    private MinionGoals() {
    }

    // Its target, looked for again every half second: the nearest creature red to its pilot that it may hit, within
    // SEEK; one that died, turned another colour or got further than LOSE is let go.
    static final class Foes extends Goal {
        private static final double SEEK = 16.0;
        private static final double LOSE = 24.0;
        private static final int EVERY = 10;
        private final MechMinion minion;
        private int next;

        Foes(MechMinion minion) {
            this.minion = minion;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            if (--this.next > 0) {
                return;
            }
            this.next = EVERY;
            ServerPlayer pilot = this.minion.pilot();
            if (pilot == null && !this.minion.wild()) {
                this.minion.setTarget(null);
                return;
            }
            LivingEntity target = this.minion.getTarget();
            // A wild one keeps on whatever hurt it (HurtByTargetGoal) as long as it lives and stays near.
            if (target != null && (!target.isAlive() || target.distanceToSqr(this.minion) > LOSE * LOSE
                    || pilot != null && !fair(pilot, target))) {
                target = null;
            }
            if (target == null) {
                target = this.nearest(pilot);
            }
            this.minion.setTarget(target);
        }

        @Nullable
        private LivingEntity nearest(@Nullable ServerPlayer pilot) {
            LivingEntity best = null;
            double bestFar = SEEK * SEEK;
            for (LivingEntity living : this.minion.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(this.minion.blockPosition()).inflate(SEEK), living -> pilot == null ? monster(living)
                            : fair(pilot, living))) {
                double far = living.distanceToSqr(this.minion);
                if (far < bestFar) {
                    bestFar = far;
                    best = living;
                }
            }
            return best;
        }

        private boolean fair(ServerPlayer pilot, LivingEntity living) {
            return living != this.minion && living.isAlive() && PowerRing.canHit(pilot, living)
                    && Factions.hostile(pilot, living);
        }

        // What a wild one goes after, as an iron golem does: monsters, but not creepers.
        private boolean monster(LivingEntity living) {
            return living.isAlive() && living instanceof Enemy && !(living instanceof Creeper)
                    && !(living instanceof MechMinion);
        }
    }

    // With nothing to fight it keeps near the mech: further than NEAR from under its pilot it walks back, till CLOSE.
    static final class Follow extends Goal {
        private static final double NEAR = 9.0;
        private static final double CLOSE = 5.0;
        private static final int EVERY = 10;
        private final MechMinion minion;
        private int repath;

        Follow(MechMinion minion) {
            this.minion = minion;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.away(NEAR);
        }

        @Override
        public boolean canContinueToUse() {
            return this.away(CLOSE);
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.minion.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer pilot = this.minion.pilot();
            if (pilot != null && --this.repath <= 0) {
                this.repath = EVERY;
                this.minion.getNavigation().moveTo(pilot.getX(), this.minion.getY(), pilot.getZ(), 1.1);
            }
        }

        private boolean away(double far) {
            ServerPlayer pilot = this.minion.pilot();
            if (this.minion.getTarget() != null || pilot == null) {
                return false;
            }
            double dx = pilot.getX() - this.minion.getX();
            double dz = pilot.getZ() - this.minion.getZ();
            return dx * dx + dz * dz > far * far;
        }
    }
}
