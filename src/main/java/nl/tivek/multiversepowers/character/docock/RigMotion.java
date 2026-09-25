package nl.tivek.multiversepowers.character.docock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.target.Targeting;

abstract class RigMotion extends RigState {
    RigMotion(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    void spreadTips() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < this.arms.length; i++) {
                for (int j = i + 1; j < this.arms.length; j++) {
                    this.pushApart(this.arms[i], this.arms[j]);
                }
            }
        }
    }

    void tickAims() {
        for (Arm arm : this.arms) {
            Vec3 wanted = arm.aim.lengthSqr() > 1.0E-6 ? arm.aim.normalize() : this.naturalAim(arm);
            if (arm.aimShown.lengthSqr() < 1.0E-6) {
                arm.aimShown = wanted;
                continue;
            }
            Vec3 next = arm.aimShown.add(wanted.subtract(arm.aimShown).scale(AIM_EASE));
            arm.aimShown = next.lengthSqr() < 1.0E-6 ? wanted : next.normalize();
        }
    }

    private Vec3 naturalAim(Arm arm) {
        Vec3 mount = this.mount(arm.index);
        Vec3 out = arm.tip.subtract(this.elbow(arm, mount));
        return out.lengthSqr() < 1.0E-6 ? this.forward() : out.normalize();
    }

    Vec3 elbow(Arm arm, Vec3 mount) {
        double reach = mount.distanceTo(arm.tip);
        double bow = arm.upper ? Math.min(1.5, 0.35 + reach * 0.12) : Math.min(1.5, 0.9 + reach * 0.1);
        return arm.tip.add(mount.subtract(arm.tip).scale(0.35)).add(0, bow, 0);
    }

    private void pushApart(Arm a, Arm b) {
        if (!mayBeNudged(a) || !mayBeNudged(b)) {
            return;
        }
        Vec3 between = b.tip.subtract(a.tip);
        double distance = between.length();
        if (distance >= TIP_APART) {
            return;
        }
        Vec3 push = distance < 1.0E-4
                ? this.right().scale(TIP_APART * 0.3)
                : between.scale((TIP_APART - distance) * 0.3 / distance);
        a.tip = a.tip.subtract(push);
        b.tip = b.tip.add(push);
    }

    private static boolean mayBeNudged(Arm arm) {
        return (arm.job == Job.REST || arm.job == Job.CARRY) && !arm.leg && arm.grip == null
                && arm.foot == null && arm.step < 0;
    }

    void glide(Arm arm, Vec3 goal, double follow, double blendTo) {
        if (follow >= 0.999) {
            arm.speed = goal.subtract(arm.tip);
            arm.tip = goal;
        } else {
            arm.speed = arm.speed.add(goal.subtract(arm.tip).scale(follow * SPRING)).scale(DAMPING);
            double fast = arm.speed.length();
            if (fast > MAX_STEP) {
                arm.speed = arm.speed.scale(MAX_STEP / fast);
            }
            arm.tip = arm.tip.add(arm.speed);
        }
        arm.blend += Mth.clamp(blendTo - arm.blend, -0.12, 0.12);
    }

    void toRest(Arm arm) {
        arm.job = arm.load != null ? Job.CARRY : Job.REST;
        arm.age = 0;
        arm.target = null;
        arm.struckAt = -1;
        arm.delay = 0;
        arm.tipOffset = 0;
        arm.clipNormal = Vec3.ZERO;
        arm.spike = 0;
        arm.thrust = 0;
        arm.aim = Vec3.ZERO;
        double fast = arm.speed.length();
        if (fast > 0.5) {
            arm.speed = arm.speed.scale(0.5 / fast);
        }
    }

    void walk(ServerLevel level, Arm arm) {
        Vec3 wanted = this.footSpot(level, arm);
        if (wanted == null) {
            arm.foot = null;
            arm.step = -1;
            Vec3 hang = this.mount(arm.index)
                    .add(this.right().scale(arm.side * (arm.upper ? 0.9 : 1.4)))
                    .add(0, arm.upper ? -1.15 : -1.95, 0)
                    .subtract(this.forward().scale(arm.upper ? 0.05 : 0.5));
            this.glide(arm, hang, 0.3, 0);
            arm.claw += (0.45 - arm.claw) * 0.14;
            arm.aim = new Vec3(0, -1, 0);
            return;
        }
        if (arm.step >= 0) {
            double t = (arm.step + 1.0) / STEP_TIME;
            Vec3 was = arm.tip;
            arm.tip = arm.stepFrom.lerp(arm.stepTo, Ease.smoother(t)).add(0, Math.sin(Math.PI * t) * 0.55, 0);
            arm.speed = arm.tip.subtract(was);
            arm.step++;
            if (t >= 1.0) {
                arm.foot = arm.stepTo;
                arm.step = -1;
                this.sound(arm.foot, SoundEvents.IRON_GOLEM_STEP, 0.3F, 1.7F);
                ParticleFx.cloud(level, ParticleTypes.POOF, arm.foot.add(0, 0.05, 0), 2, 0.1, 0.01);
            }
        } else {
            double behind = arm.foot == null ? Double.MAX_VALUE
                    : Math.max(horizontal(arm.foot, wanted), Math.abs(arm.foot.y - wanted.y) * 1.3);
            int stepping = 0;
            int legs = 0;
            for (Arm other : this.arms) {
                if (other.leg) {
                    legs++;
                    if (other != arm && other.step >= 0) {
                        stepping++;
                    }
                }
            }
            boolean mayStep = stepping < Math.max(1, legs / 2) || behind > STEP_AFTER * 1.8;
            if (behind > STEP_AFTER && mayStep) {
                arm.stepFrom = arm.tip;
                arm.stepTo = wanted;
                arm.step = 0;
            } else if (arm.foot != null) {
                arm.tip = arm.foot;
                arm.speed = Vec3.ZERO;
            }
        }
        arm.blend += Mth.clamp(1.0 - arm.blend, -0.12, 0.12);
        arm.claw += (0.4 - arm.claw) * 0.14;
        arm.aim = new Vec3(0, -1, 0);
    }

    @Nullable
    private Vec3 footSpot(ServerLevel level, Arm arm) {
        int legs = Math.max(1, this.legCount());
        Vec3 motion = this.caster.position().subtract(this.caster.xo, this.caster.yo, this.caster.zo);
        double ahead = (arm.upper ? 1.0 : -1.0) * (legs >= 3 ? 0.62 : 0.18);
        Vec3 around = this.right().scale(arm.side).add(this.forward().scale(ahead)).normalize();
        double reach = 4.0;
        Vec3 spot = this.caster.position().add(around.scale(LEG_SPREAD))
                .subtract(this.forward().scale(0.15)).add(motion.x * reach, 0, motion.z * reach);
        double ground = Targeting.floorBelow(level, BlockPos.containing(spot.x, this.caster.getY() + 1.5, spot.z));
        if (this.caster.getY() - ground > LEG_DROP || ground - this.caster.getY() > 1.6) {
            return null;
        }
        return new Vec3(spot.x, ground, spot.z);
    }

    void assignLegs() {
        int wanted = this.climbing || this.folding ? 0 : STANCES[this.stance];
        int slot = 0;
        for (int i = this.arms.length - 1; i >= 0; i--) {
            Arm arm = this.arms[i];
            boolean canWalk = slot < wanted && arm.held == null && arm.load == null && arm.job == Job.REST;
            if (canWalk) {
                arm.leg = true;
                slot++;
            } else if (arm.leg) {
                arm.leg = false;
                arm.foot = null;
                arm.step = -1;
            }
        }
    }

    boolean onLegs(ServerLevel level) {
        return this.legCount() > 0
                && !level.noBlockCollision(this.caster, this.caster.getBoundingBox().expandTowards(0, -LEG_DROP, 0));
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
