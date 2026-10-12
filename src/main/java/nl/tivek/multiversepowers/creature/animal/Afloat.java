package nl.tivek.multiversepowers.creature.animal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

// What the animals at home on the water share: lying on its surface at a line of their own, paddling about on it, and
// making for water from dry land now and then.
public final class Afloat {
    private Afloat() {
    }

    // Holds an animal up in water so the surface lies `line` blocks above its feet; each tick, on the server.
    public static void buoy(WildAnimal animal, double line) {
        if (!animal.isInWater()) {
            return;
        }
        double under = animal.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value()) - line;
        if (under > 0.0) {
            Vec3 motion = animal.getDeltaMovement();
            animal.setDeltaMovement(motion.x, Math.min(under * 0.4, 0.12), motion.z);
        }
    }

    // A block of water with air over it near the animal, as far as `drop` blocks below it; null when it finds none.
    @Nullable
    private static BlockPos surface(WildAnimal animal, int reach, int drop) {
        Level level = animal.level();
        RandomSource random = animal.getRandom();
        BlockPos at = animal.blockPosition();
        BlockPos.MutableBlockPos spot = new BlockPos.MutableBlockPos();
        for (int tries = 0; tries < 12; tries++) {
            int x = at.getX() + random.nextInt(2 * reach + 1) - reach;
            int z = at.getZ() + random.nextInt(2 * reach + 1) - reach;
            for (int y = at.getY() + 1; y >= at.getY() - drop - 1; y--) {
                spot.set(x, y, z);
                if (!level.isLoaded(spot)) {
                    break;
                }
                if (level.getFluidState(spot).is(FluidTags.WATER)) {
                    if (level.getBlockState(spot.above()).isAir()) {
                        return spot.immutable();
                    }
                    break;
                }
            }
        }
        return null;
    }

    // In the water, paddles to another spot on it now and then: the game's own strolls only ever pick dry ground.
    public static final class Paddle extends Goal {
        private final WildAnimal animal;
        private final int every;
        private final double speed;
        @Nullable
        private BlockPos spot;

        public Paddle(WildAnimal animal, int every, double speed) {
            this.animal = animal;
            this.every = every;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.animal.isInWater() || this.animal.getRandom().nextInt(reducedTickDelay(this.every)) != 0) {
                return false;
            }
            this.spot = surface(this.animal, 7, 0);
            return this.spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.animal.isInWater() && !this.animal.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos to = this.spot;
            if (to != null) {
                this.animal.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, this.speed);
            }
        }

        @Override
        public void stop() {
            this.animal.getNavigation().stop();
        }
    }

    // On dry land, makes for water near it now and then.
    public static final class Seek extends Goal {
        private final WildAnimal animal;
        private final int every;
        private final double speed;
        @Nullable
        private BlockPos spot;

        public Seek(WildAnimal animal, int every, double speed) {
            this.animal = animal;
            this.every = every;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.animal.isInWater() || !this.animal.onGround()
                    || this.animal.getRandom().nextInt(reducedTickDelay(this.every)) != 0) {
                return false;
            }
            this.spot = surface(this.animal, 10, 3);
            return this.spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.animal.isInWater() && !this.animal.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos to = this.spot;
            if (to != null) {
                this.animal.getNavigation().moveTo(to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, this.speed);
            }
        }
    }
}
