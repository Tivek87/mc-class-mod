package nl.tivek.multiversepowers.creature.animal.flamingo;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

// Scared up: a run across the ground or the water with its wings beating, up and away from what scared it, level for
// a while, then down again. It flies on a heading, not a path, turning aside from what it meets; the flock near it goes
// up too.
final class FlamingoFlight extends Goal {
    private static final int RUN = 12;
    private final Flamingo f;
    private int length;
    private int left;
    private float heading;

    FlamingoFlight(Flamingo flamingo) {
        this.f = flamingo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Flamingo f = this.f;
        if (f.isBaby() || f.tickCount < f.groundedUntil || f.isLeashed()) {
            return false;
        }
        Vec3 from = f.scare();
        if (from == null) {
            return false;
        }
        Vec3 away = f.position().subtract(from);
        this.heading = (float) Mth.atan2(-away.x, away.z) + (f.getRandom().nextFloat() - 0.5F) * 0.8F;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.left > 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        Flamingo f = this.f;
        this.length = RUN + 60 + f.getRandom().nextInt(50);
        this.left = this.length;
        f.getNavigation().stop();
        f.start(0, 0);
        f.playSound(Flamingo.HONKS, 1.0F, f.getVoicePitch());
        Vec3 from = f.position().subtract(-Mth.sin(this.heading), 0.0, Mth.cos(this.heading));
        for (Flamingo near : f.level().getEntitiesOfClass(Flamingo.class, f.getBoundingBox().inflate(10.0, 4.0, 10.0),
                near -> near != f && !near.isBaby())) {
            near.alarm = from;
        }
    }

    @Override
    public void tick() {
        Flamingo f = this.f;
        int gone = this.length - this.left--;
        Vec3 motion = f.getDeltaMovement();
        double speed;
        double up;
        if (gone < RUN) {
            // The run-up: faster each stride, the wings opening and beating.
            speed = 0.12 + 0.02 * gone;
            up = motion.y;
            if (gone == RUN - 1) {
                f.playSound(Flamingo.TAKEOFF, 0.8F, 0.9F);
            }
            if (f.horizontalCollision && f.onGround()) {
                f.getJumpControl().jump();
            }
        } else {
            speed = 0.36;
            up = gone < RUN + 16 ? 0.16 : this.left < 26 ? -0.08 : 0.0;
            if (f.horizontalCollision) {
                this.heading += 0.6F;
                up = Math.max(up, 0.18);
            }
        }
        f.setDeltaMovement(-Mth.sin(this.heading) * speed, up, Mth.cos(this.heading) * speed);
        float yaw = this.heading * Mth.RAD_TO_DEG;
        f.setYRot(yaw);
        f.yBodyRot = yaw;
        f.yHeadRot = yaw;
        if (gone > RUN && this.left < 26 && (f.onGround() || f.isInWater())) {
            this.left = 0;
        }
    }

    @Override
    public void stop() {
        this.f.groundedUntil = this.f.tickCount + 100;
    }
}
