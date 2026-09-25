package nl.tivek.multiversepowers.character.docock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.target.Targeting;

final class OctoRig extends RigGround implements Effect {
    OctoRig(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    void fold() {
        if (!this.folding) {
            this.folding = true;
            this.marks.clear();
            this.dropLoads(this.home);
            this.letGo(true);
            this.setBlocking(false);
            this.endRampage();
            this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 1.0F, 0.6F);
            OctopusArms.sync(this.caster);
        }
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        this.age = age;
        if (this.caster.isRemoved() || !this.caster.isAlive() || this.caster.level() != level
                || this.caster.isSpectator()) {
            this.shutDown(level);
            return false;
        }
        if (this.climbing && !this.nearSurface(level)) {
            this.climbing = false;
        }
        // The tentacles carry the player through the air; the server must not see that as flying.
        if (this.climbing || this.onLegs(level)) {
            this.caster.connection.aboveGroundTickCount = 0;
        }
        if (this.climbing) {
            this.caster.resetFallDistance();
        }
        if (this.folding) {
            this.unfold -= 1.0 / FOLD;
            if (this.unfold <= 0.0) {
                this.shutDown(level);
                return false;
            }
        } else {
            this.unfold = Math.min(1.0, this.unfold + 1.0 / UNFOLD);
        }
        this.assignLegs();
        this.tickMarks(level);
        this.tickPortal(level);
        this.tickHold(level);
        this.tickSlam(level);
        this.tickHeldSlam();
        this.tickRampage(level);
        for (Arm arm : this.arms) {
            this.tickArm(level, arm);
        }
        this.spreadTips();
        this.tickAims();
        this.tickLegSpeed();
        this.tickShield(level);
        this.draw(level);
        int legs = this.legCount();
        if (legs != this.syncedLegs || this.marks.size() != this.syncedMarks) {
            this.syncedLegs = legs;
            this.syncedMarks = this.marks.size();
            OctopusArms.sync(this.caster);
        }
        return true;
    }

    void shutDown(ServerLevel level) {
        this.letGo(true);
        this.dropMarks();
        this.dropLoads(level);
        if (this.portal != null) {
            this.portal.stop(level);
            this.portal = null;
        }
        for (Arm arm : this.arms) {
            RobotArm.remove(level, arm.id);
            if (arm.farId >= 0) {
                RobotArm.remove(level, arm.farId);
                arm.farId = -1;
            }
        }
        if (this.shieldId >= 0) {
            RobotArm.removePortal(level, this.shieldId);
            this.shieldId = -1;
        }
        OctopusArms.removed(this.caster, this);
    }

    private void tickArm(ServerLevel level, Arm arm) {
        arm.age++;
        Vec3 goal;
        double follow;
        double blendTo;
        Job job = arm.job;
        if (job == Job.STRIKE && arm.delay > 0) {
            arm.delay--;
            job = Job.REST;
        }
        switch (job) {
            case STRIKE -> {
                LivingEntity target = arm.target;
                if (target == null || !target.isAlive() || target.level() != level) {
                    this.toRest(arm);
                    return;
                }
                goal = target.getBoundingBox().getCenter();
                follow = 0.7;
                blendTo = 1;
                if (arm.struckAt < 0) {
                    arm.claw = 0.6;
                    double reach = 1.0 + target.getBbWidth() * 0.5;
                    if (arm.tip.distanceTo(goal) <= reach || arm.age > STRIKE_MAX) {
                        this.deliver(level, arm, target);
                        arm.struckAt = arm.age;
                        arm.claw = 0.12;
                    }
                } else if (arm.age - arm.struckAt >= 2) {
                    this.toRest(arm);
                    return;
                }
            }
            case REACH -> {
                LivingEntity target = arm.target;
                if (target == null || !target.isAlive() || target.level() != level || arm.age > GRAB_MAX) {
                    this.toRest(arm);
                    return;
                }
                Vec3 center = target.getBoundingBox().getCenter();
                Vec3 toGoal = center.subtract(arm.tip);
                double distance = toGoal.length();
                goal = distance > GRAB_SPEED ? arm.tip.add(toGoal.scale(GRAB_SPEED / distance)) : center;
                follow = 1.0;
                blendTo = 1;
                arm.claw = 0.7;
                if (arm.tip.distanceTo(center) <= 0.9 + target.getBbWidth() * 0.5) {
                    this.seize(level, arm, target);
                }
            }
            case HOLD -> {
                goal = arm.held != null ? arm.held.getBoundingBox().getCenter() : arm.tip;
                follow = 1.0;
                blendTo = 1;
                arm.claw = arm.held != null ? arm.held.getBbWidth() * 0.5 + 0.25 : CLAW_REST;
            }
            case PORTAL -> {
                PortalRun run = this.portal;
                if (run == null) {
                    this.toRest(arm);
                    return;
                }
                goal = run.tip();
                follow = run.snap() ? 1.0 : 0.4;
                blendTo = run.snap() ? 1.0 : 0.6;
                arm.claw = run.claw();
                arm.tipOffset = run.tipOffset();
                arm.clipPoint = run.clipPoint();
                arm.clipNormal = run.clipNormal();
                arm.spike = run.spike();
                arm.thrust = run.thrust();
            }
            case BURROW -> {
                this.tickDig(level, arm);
                return;
            }
            case CARRY -> {
                goal = this.carryPose(arm);
                follow = 0.5;
                blendTo = 0.2;
                arm.claw = 0.55;
                if (arm.load == null) {
                    this.toRest(arm);
                    return;
                }
            }
            case RISE -> {
                goal = this.caster.position().add(0, 3.3, 0)
                        .add(this.right().scale(arm.side * (arm.upper ? 0.9 : 1.7)))
                        .add(this.forward().scale(arm.upper ? 0.5 : -0.5));
                follow = 0.4;
                blendTo = 0;
                arm.claw = 0.75;
            }
            case SMASH -> {
                goal = arm.spot;
                follow = 0.85;
                blendTo = 1;
                arm.claw = 0.5;
                if (arm.age > 10) {
                    this.toRest(arm);
                    return;
                }
            }
            case PLANT -> {
                goal = arm.spot;
                follow = 0.9;
                blendTo = 1;
                arm.claw = 0.2;
                if (arm.age > PLANT_TIME) {
                    this.toRest(arm);
                    return;
                }
            }
            default -> {
                if (this.climbing) {
                    goal = this.climbPose(level, arm);
                    follow = 0.45;
                    blendTo = 0.35;
                    arm.claw = 0.18;
                    arm.aim = Vec3.atLowerCornerOf(this.climbFace.getNormal()).scale(-1);
                } else if (arm.leg) {
                    this.walk(level, arm);
                    return;
                } else if (this.isBlocking()) {
                    goal = this.blockPose(arm);
                    follow = 0.55;
                    blendTo = 0;
                    arm.claw = 0.1;
                    arm.aim = this.forward();
                } else {
                    goal = this.restPose(arm);
                    follow = 0.4;
                    blendTo = 0;
                    arm.claw += (CLAW_REST - arm.claw) * 0.14;
                }
                arm.foot = null;
                arm.step = -1;
                if (!this.climbing) {
                    arm.grip = null;
                }
            }
        }
        if (job == Job.STRIKE || job == Job.REACH || job == Job.HOLD) {
            arm.aim = goal.subtract(arm.tip);
        }
        this.glide(arm, goal, follow, blendTo);
    }

    boolean dash(ServerLevel level) {
        Vec3 input = this.forward().scale(this.caster.zza).subtract(this.right().scale(this.caster.xxa));
        Vec3 way = input.lengthSqr() > 1.0E-4 ? input.normalize() : this.caster.getLookAngle();
        this.caster.setDeltaMovement(way.scale(ability("dash").value("speed")).add(0, DASH_LIFT, 0));
        this.caster.hurtMarked = true;
        OctopusArms.safeFall(this.caster, 80);
        Vec3 flat = new Vec3(way.x, 0, way.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? this.forward() : flat.normalize();
        Vec3 behind = this.caster.position().subtract(push.scale(0.9));
        double ground = Targeting.floorBelow(level,
                BlockPos.containing(behind.x, this.caster.getY() + 0.5, behind.z));
        for (Arm arm : this.arms) {
            if (arm.free()) {
                arm.job = Job.PLANT;
                arm.age = 0;
                arm.spot = new Vec3(behind.x, Math.max(ground, this.caster.getY() - 2.5), behind.z)
                        .add(this.right().scale(arm.side * (arm.upper ? 0.5 : 0.9)));
            }
        }
        ParticleFx.cloud(level, ParticleTypes.POOF, this.caster.position(), 12, 0.4, 0.05);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, this.caster.position(), 6, 0.3, 0.1);
        this.sound(this.caster.position(), SoundEvents.BREEZE_JUMP, 1.0F, 0.8F);
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.6F);
        return true;
    }

    private void tickLegSpeed() {
        boolean running = this.legCount() > 0 && this.caster.isSprinting() && !this.climbing;
        if (running == this.legsRunning) {
            return;
        }
        this.legsRunning = running;
        OctopusArms.modifier(this.caster, Attributes.MOVEMENT_SPEED, OctopusArms.LEG_RUN_ID, LEG_RUN_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, running);
    }

    boolean cycleStance(boolean back) {
        this.stance = Math.floorMod(this.stance + (back ? -1 : 1), STANCES.length);
        int legs = STANCES[this.stance];
        this.sound(this.caster.position(), legs == 0 ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND,
                0.9F, legs == 0 ? 1.2F : 0.8F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + (legs == 0 ? ".stance.feet"
                        : ".stance.legs"), legs, this.arms.length - legs),
                true);
        OctopusArms.sync(this.caster);
        return true;
    }

    void setBlocking(boolean on) {
        on = on && !this.folding;
        if (on == this.blocking) {
            return;
        }
        this.blocking = on;
        OctopusArms.modifier(this.caster, Attributes.MOVEMENT_SPEED, OctopusArms.BLOCKING_ID, -0.4,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on);
        this.sound(this.caster.position(), on ? SoundEvents.SHIELD_BLOCK : SoundEvents.IRON_TRAPDOOR_OPEN, 0.7F,
                on ? 1.4F : 0.9F);
    }

    private void tickShield(ServerLevel level) {
        double wanted = this.isBlocking() ? 1.0 : 0.0;
        this.shieldOpen += Mth.clamp(wanted - this.shieldOpen, -1.0 / SHIELD_TIME, 1.0 / SHIELD_TIME);
        if (this.shieldOpen <= 0.001) {
            if (this.shieldId >= 0) {
                RobotArm.removePortal(level, this.shieldId);
                this.shieldId = -1;
            }
            return;
        }
        if (this.shieldId < 0) {
            this.shieldId = RobotArm.newId();
        }
        Vec3 look = this.caster.getLookAngle();
        Vec3 center = this.caster.getEyePosition().add(look.scale(1.05)).add(0, -0.15, 0);
        RobotArm.shield(level, this.shieldId, center, look, SHIELD_SIZE, Ease.smoother(this.shieldOpen));
    }

    void blocked(ServerLevel level, Vec3 from) {
        this.lastBlocked = this.age;
        Vec3 eye = this.caster.getEyePosition();
        Vec3 at = eye.add(from.subtract(eye).normalize().scale(1.05));
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.25);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 6, 0.2, 0.2);
        this.sound(at, SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.5F, 1.7F);
    }

    void setClimbing(boolean on, Direction face) {
        on = on && !this.folding && this.nearSurface(this.caster.serverLevel());
        if (on && !this.climbing) {
            this.sound(this.caster.position(), SoundEvents.CHAIN_PLACE, 0.8F, 1.1F);
        }
        this.climbing = on;
        this.climbFace = face;
    }

    private boolean nearSurface(ServerLevel level) {
        AABB box = this.caster.getBoundingBox();
        AABB around = new AABB(box.minX - HOLD_REACH, box.minY, box.minZ - HOLD_REACH,
                box.maxX + HOLD_REACH, box.maxY + HOLD_REACH, box.maxZ + HOLD_REACH);
        return !level.noBlockCollision(this.caster, around);
    }

    boolean startPortal(ServerLevel level) {
        if (this.portal != null || this.searchedJustNow("portal")) {
            return false;
        }
        Arm arm = this.freeArm();
        if (arm == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + MultiversePowers.MODID + ".busy"), true);
            return false;
        }
        LivingEntity target = Targeting.aimLiving(this.caster, level, PortalRun.RANGE);
        if (target == null) {
            Targeting.noTarget(this.caster);
            return this.foundNothing("portal");
        }
        this.portal = new PortalRun(this.caster, target);
        this.portalArm = arm.index;
        arm.job = Job.PORTAL;
        arm.age = 0;
        this.sound(this.caster.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
        return true;
    }

    private void tickPortal(ServerLevel level) {
        PortalRun run = this.portal;
        if (run == null) {
            return;
        }
        Arm arm = this.arms[this.portalArm];
        boolean running = run.tick(level, arm.tip);
        if (arm.job == Job.PORTAL && !run.busy()) {
            this.toRest(arm);
        }
        if (!running) {
            this.portal = null;
        }
    }
}
