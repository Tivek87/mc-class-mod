package nl.tivek.multiversepowers.character.docock;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * The Portal ability of the Octopus Arms. One of your tentacles reaches into a portal that opens in
 * front of you, comes out of a second portal near the creature you aim at, chases it down, grabs it,
 * hauls it back through, and slams it out of a third portal high in the sky into the ground. Then it
 * comes back through the portals. Your other tentacles keep doing their own thing the whole time.
 */
final class PortalRun extends PortalPlacing {
    /** How far you may aim to start the ability. */
    static final double RANGE = 24.0;

    PortalRun(ServerPlayer caster, LivingEntity target) {
        super(caster, target);
    }

    // ---- What the rig asks ----

    /** Where the tentacle on the back should have its tip this tick. */
    Vec3 tip() {
        if (this.dived) {
            return this.portalA;
        }
        if (this.phase == Phase.SEARCH || this.phase == Phase.SPIKE || this.phase == Phase.THRUST) {
            return this.searchPose();
        }
        double reach = this.diveLength < 1.0E-3 ? 1.0 : Math.min(1.0, this.diveTravel / this.diveLength);
        return this.diveFrom.lerp(this.portalA, reach);
    }

    /**
     * Before the dive the tentacle hangs over your shoulder and looks around, swinging slowly from
     * side to side, and steadies itself while the spike and the thrusters come out.
     */
    private Vec3 searchPose() {
        Vec3 eye = this.caster.getEyePosition();
        Vec3 forward = RobotArm.forward(this.caster);
        Vec3 right = RobotArm.right(this.caster);
        double calm = this.phase == Phase.SEARCH ? 1.0 : 0.25;
        double t = this.phaseAge * 0.16 + (this.phase == Phase.SEARCH ? 0.0 : 2.0);
        return eye.add(forward.scale(1.1 + 0.15 * Math.cos(t * 0.8)))
                .add(right.scale(Math.sin(t) * 1.15 * calm))
                .add(0, 0.85 + 0.25 * Math.cos(t * 1.7) * calm, 0);
    }

    /** How far the sharp point between the three claws is out (0 .. 1). */
    double spike() {
        return this.spike;
    }

    /** How far the thrusters are out and burning (0 .. 1). */
    double thrust() {
        return this.thrust;
    }

    /** How much tentacle lies beyond the portal, so its segments keep sliding along. */
    double tipOffset() {
        return this.dived ? Math.max(0.0, this.shown - PORTAL_DEPTH) : 0.0;
    }

    double claw() {
        return this.tipOffset() > 0.01 ? -1.0 : this.clawOpen;
    }

    /** The near part is cut off at portal A, so nothing pokes out the other side. */
    Vec3 clipPoint() {
        return this.portalA;
    }

    Vec3 clipNormal() {
        // Only once the tentacle is really going into the portal is it cut off at the ring.
        return this.phase.ordinal() < Phase.DIVE.ordinal() ? Vec3.ZERO : this.look;
    }

    boolean done() {
        return this.phase == Phase.CLOSE && !this.gateA.isOpen() && !this.gateB.isOpen() && !this.gateC.isOpen();
    }

    /** True once the tentacle is really on its way: its tip is then placed exactly, not glided. */
    boolean snap() {
        return this.phase.ordinal() >= Phase.DIVE.ordinal();
    }

    /** True while the tentacle is still out in the world (it may not go back to resting yet). */
    boolean busy() {
        return this.phase != Phase.CLOSE;
    }

    /** True while the claw has this creature. */
    boolean holds(Entity entity) {
        return this.held && this.target == entity;
    }

    // ---- Ticking ----

    /**
     * @param armTip where the rig currently draws the tip of the tentacle doing this run
     * @return false when everything is finished and cleaned up
     */
    boolean tick(ServerLevel level, Vec3 armTip) {
        if (this.caster.isRemoved() || !this.caster.isAlive() || this.caster.level() != level) {
            this.finish(level);
            return false;
        }
        this.phaseAge++;
        boolean running = switch (this.phase) {
            case SEARCH -> this.search(level);
            case SPIKE -> this.spikeOut(level);
            case THRUST -> this.thrustersOut(level, armTip);
            case DIVE -> this.dive(level);
            case HUNT -> this.hunt(level);
            case GRIP -> this.grip(level);
            case DRAG -> this.drag(level);
            case SLAM -> this.slam(level);
            case RETRACT_OUT -> this.retractOut(level);
            case RETRACT_BACK -> this.retractBack();
            case CLOSE -> !this.done();
        };
        this.gateA.tick(level);
        this.gateB.tick(level);
        this.gateC.tick(level);
        this.drawFar(level);
        if (!running) {
            this.finish(level);
            return false;
        }
        return true;
    }

    /** Everything of this run gone at once (the arms are folding in). */
    void stop(ServerLevel level) {
        this.finish(level);
    }

    private void next(Phase phase) {
        this.phase = phase;
        this.phaseAge = -1;
    }

    private void finish(ServerLevel level) {
        this.releaseTarget(true);
        RobotArm.remove(level, this.farArm);
        this.gateA.remove(level);
        this.gateB.remove(level);
        this.gateC.remove(level);
    }

    // ---- Phases ----

    /**
     * One tentacle stops what it was doing and searches: the claw swings around, the eye scans, and
     * the first portal starts building itself in front of you.
     */
    private boolean search(ServerLevel level) {
        if (this.phaseAge == 0) {
            this.portalA = spotInFront(this.caster, this.look);
            this.gateA.open(level, this.portalA, this.look.scale(-1));
            sound(level, this.searchPose(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.6F);
        }
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        Vec3 at = this.searchPose();
        if (this.phaseAge % 7 == 0) {
            sound(level, at, SoundEvents.NOTE_BLOCK_HAT.value(), 0.35F, 1.9F);
            ParticleFx.at(level, ParticleTypes.ELECTRIC_SPARK, at);
        }
        if (this.phaseAge >= SEARCH_TIME) {
            sound(level, at, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.8F);
            this.next(Phase.SPIKE);
        }
        return true;
    }

    /** The sharp point slides out of the middle of the claw, between the three fingers. */
    private boolean spikeOut(ServerLevel level) {
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        if (this.phaseAge == 0) {
            this.openEntry(level);
            sound(level, this.searchPose(), SoundEvents.PISTON_EXTEND, 0.9F, 1.5F);
        }
        this.spike = Ease.smooth((this.phaseAge + 1.0) / SPIKE_TIME);
        this.clawOpen = CLAW_OPEN + 0.25 * this.spike;
        if (this.phaseAge % 4 == 0) {
            ParticleFx.at(level, ParticleTypes.CRIT, this.searchPose());
        }
        if (this.phaseAge + 1 >= SPIKE_TIME) {
            this.spike = 1.0;
            sound(level, this.searchPose(), SoundEvents.ANVIL_LAND, 0.4F, 1.9F);
            this.next(Phase.THRUST);
        }
        return true;
    }

    /** The thrusters fold out of the tentacle and light up; then it dives, boosting. */
    private boolean thrustersOut(ServerLevel level, Vec3 armTip) {
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        if (this.phaseAge == 0) {
            sound(level, this.searchPose(), SoundEvents.IRON_TRAPDOOR_OPEN, 0.8F, 0.7F);
        }
        this.thrust = Ease.smooth((this.phaseAge + 1.0) / THRUST_TIME);
        Vec3 at = this.searchPose();
        if (this.thrust > 0.45) {
            ParticleFx.cloud(level, ParticleTypes.FLAME, at, 2, 0.12, 0.02);
            ParticleFx.cloud(level, ParticleTypes.SMOKE, at, 1, 0.1, 0.01);
            if (this.phaseAge % 5 == 0) {
                sound(level, at, SoundEvents.BLAZE_SHOOT, 0.5F, 1.4F);
            }
        }
        // Only really go once the thrusters burn and both portals stand open.
        if (this.phaseAge + 1 >= THRUST_TIME && this.gateA.ready() && this.gateB.ready()) {
            this.thrust = 1.0;
            this.diveFrom = armTip;
            this.diveLength = Math.max(0.5, armTip.distanceTo(this.portalA));
            this.diveTravel = 0;
            this.clawOpen = CLAW_OPEN;
            sound(level, armTip, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
            sound(level, armTip, SoundEvents.PISTON_EXTEND, 1.0F, 0.7F);
            this.next(Phase.DIVE);
        }
        return true;
    }

    /** The tentacle shoots into the portal, faster and faster, and comes out of the second one. */
    private boolean dive(ServerLevel level) {
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        this.diveTravel += Math.min(HUNT_SPEED, DIVE_SPEED + DIVE_ACCEL * this.phaseAge);
        if (this.diveTravel < this.diveLength) {
            return true;
        }
        this.dived = true;
        this.exit = this.gateB;
        Vec3 out = this.gateB.normal;
        this.trail.clear();
        this.trail.add(this.gateB.center.subtract(out.scale(PORTAL_DEPTH)));
        this.trail.add(this.gateB.center.add(out.scale(this.diveTravel - this.diveLength)));
        this.shown = RobotArm.length(this.trail);
        this.velocity = out;
        this.openSky(level);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.portalA, 20, 0.35);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.gateB.center, 24, 0.45);
        sound(level, this.gateB.center, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
        this.next(Phase.HUNT);
        return true;
    }

    /**
     * Out of the second portal, curving after the creature wherever it runs. It only hunts within its
     * own range (config: homingRangeBlocks, 30 by default); further than that it gives up and returns.
     */
    private boolean hunt(ServerLevel level) {
        double homing = OctoRig.ability("portal").value("homingRangeBlocks");
        if (!this.targetValid(level) || this.phaseAge > MAX_HUNT || this.shown > MAX_TRAIL
                || this.target.position().distanceTo(this.gateB.center) > homing) {
            return this.cancel();
        }
        Vec3 tip = this.trail.get(this.trail.size() - 1);
        Vec3 goal = this.target.getBoundingBox().getCenter();
        Vec3 toGoal = goal.subtract(tip);
        double distance = toGoal.length();
        Vec3 wanted = this.phaseAge < 3 || distance < 1.0E-4 ? this.gateB.normal : toGoal.scale(1.0 / distance);
        // The nearer it gets, the harder it turns. With one steady turning speed it sails past the
        // creature and then circles round it for ever without ever closing in.
        double steer = Math.min(1.0, Math.max(STEERING, STEERING + (1.0 - STEERING) * (1.0 - distance / 6.0)));
        this.velocity = this.velocity.scale(1.0 - steer).add(wanted.scale(steer)).normalize();
        Vec3 next = tip.add(this.velocity.scale(Math.min(HUNT_SPEED, Math.max(distance, 0.2))));
        this.trail.add(next);
        this.shown = RobotArm.length(this.trail);
        if (this.phaseAge % 5 == 0) {
            sound(level, next, SoundEvents.CHAIN_STEP, 0.8F, 0.7F);
        }
        // One step can take the claw straight past a small creature, so anything it passes within a
        // step of counts as caught.
        if (next.distanceTo(goal) <= Math.max(HUNT_SPEED, this.target.getBbWidth() * 0.7 + 0.6)) {
            return this.grab(level);
        }
        return true;
    }

    private boolean grab(ServerLevel level) {
        // Something else (a tentacle, another power) may have caught it during the hunt: a creature or a
        // player is only ever held once.
        if (HeldMobs.isHeldByAnyone(this.target) || (this.target instanceof Mob mob && !HeldMobs.hold(mob))) {
            return this.cancel();
        }
        this.held = true;
        Vec3 at = this.target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 16, 0.4, 0.3);
        sound(level, at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
        sound(level, at, SoundEvents.CHAIN_HIT, 1.2F, 0.6F);
        this.next(Phase.GRIP);
        return true;
    }

    /** The claw closes around it. */
    private boolean grip(ServerLevel level) {
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        Vec3 tip = this.trail.get(this.trail.size() - 1);
        Vec3 goal = this.target.getBoundingBox().getCenter();
        Vec3 next = tip.lerp(goal, 1.0 / Math.max(1, GRIP_TIME - this.phaseAge));
        if (next.distanceToSqr(tip) > 1.0E-4) {
            this.trail.add(next);
        }
        this.shown = RobotArm.length(this.trail);
        double closed = this.target.getBbWidth() * 0.5 + 0.2;
        this.clawOpen = CLAW_OPEN + (closed - CLAW_OPEN) * Ease.smooth((this.phaseAge + 1.0) / GRIP_TIME);
        if (this.phaseAge + 1 >= GRIP_TIME) {
            this.clamped = true;
            this.next(Phase.DRAG);
        }
        return true;
    }

    /** Hauled back along its own track into the portal. */
    private boolean drag(ServerLevel level) {
        if (!this.targetValid(level) || this.phaseAge > MAX_DRAG) {
            return this.cancel();
        }
        this.shown = Math.max(PORTAL_DEPTH, this.shown - DRAG_SPEED);
        this.placeTarget(this.farTip());
        if (this.phaseAge % 4 == 0) {
            sound(level, this.farTip(), SoundEvents.PISTON_CONTRACT, 0.6F, 1.3F);
        }
        if (this.shown <= PORTAL_DEPTH + 1.0E-3 && this.gateC.ready()) {
            this.throughSky(level);
        }
        return true;
    }

    private void throughSky(ServerLevel level) {
        this.gateB.close();
        this.exit = this.gateC;
        this.cut++;
        Vec3 c = this.gateC.center;
        this.trail.clear();
        this.trail.add(c.add(0, PORTAL_DEPTH, 0));
        this.trail.add(new Vec3(c.x, this.groundC - 2.0, c.z));
        this.shown = PORTAL_DEPTH;
        // Through the portal is the one real jump of the whole trick, so it is a hard teleport with a
        // flash on both sides. Everything after it slides.
        this.placeTarget(c, true);
        ParticleFx.at(level, ParticleTypes.FLASH, this.gateB.center);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.gateB.center, 24, 0.4);
        ParticleFx.at(level, ParticleTypes.FLASH, c);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, c, 30, 0.5);
        sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.8F);
        this.next(Phase.SLAM);
    }

    /** Straight down out of the sky portal, faster and faster, into the ground. */
    private boolean slam(ServerLevel level) {
        if (!this.targetValid(level)) {
            return this.cancel();
        }
        this.thrust = 1.0;
        if (this.phaseAge < SLAM_WAIT) {
            // Hanging out of the sky portal for a moment, thrusters howling, before it fires down.
            this.placeTarget(this.farTip());
            Vec3 at = this.farTip();
            ParticleFx.cloud(level, ParticleTypes.FLAME, at, 3, 0.2, 0.04);
            ParticleFx.cloud(level, ParticleTypes.LARGE_SMOKE, at, 2, 0.2, 0.02);
            if (this.phaseAge == 0) {
                sound(level, at, SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F);
            }
            return true;
        }
        this.shown += Math.min(SLAM_MAX, SLAM_SPEED + SLAM_ACCEL * (this.phaseAge - SLAM_WAIT));
        double half = this.target.getBbHeight() / 2;
        double lowest = this.gateC.center.y + PORTAL_DEPTH - (this.groundC + half);
        boolean landed = this.shown >= lowest;
        if (landed) {
            this.shown = Math.max(PORTAL_DEPTH, lowest);
        }
        this.placeTarget(this.farTip());
        if (landed) {
            this.impact(level);
            this.retractSpeed = OUT_SPEED;
            this.next(Phase.RETRACT_OUT);
        }
        return true;
    }

    /** Slowly back into the portal it came out of; the thrusters die down and the point slides in. */
    private boolean retractOut(ServerLevel level) {
        this.clawOpen += (CLAW_OPEN - this.clawOpen) * 0.1;
        this.thrust = Math.max(0.0, this.thrust - 0.06);
        this.spike = Math.max(0.0, this.spike - 0.05);
        this.shown -= this.retractSpeed;
        if (this.phaseAge % 10 == 0 && this.exit != null) {
            sound(level, this.exit.center, SoundEvents.CHAIN_STEP, 0.8F, 0.6F);
        }
        if (this.shown <= PORTAL_DEPTH) {
            this.exit = null;
            this.gateB.close();
            this.gateC.close();
            this.next(Phase.RETRACT_BACK);
        }
        return true;
    }

    /** Back out of the first portal; the tentacle is free again and goes back to its own work. */
    private boolean retractBack() {
        this.dived = false;
        this.thrust = Math.max(0.0, this.thrust - 0.08);
        this.spike = Math.max(0.0, this.spike - 0.08);
        this.diveTravel = Math.max(0.0, this.diveTravel - 0.6);
        if (this.diveTravel <= 0.0) {
            this.gateA.close();
            this.next(Phase.CLOSE);
        }
        return true;
    }

    private boolean cancel() {
        this.releaseTarget(true);
        this.gateC.close();
        if (this.phase.ordinal() < Phase.DIVE.ordinal()) {
            this.spike = 0;
            this.thrust = 0;
            this.gateA.close();
            this.gateB.close();
            this.next(Phase.CLOSE);
        } else if (this.phase == Phase.DIVE) {
            this.next(Phase.RETRACT_BACK);
        } else {
            this.retractSpeed = CANCEL_OUT_SPEED;
            this.next(Phase.RETRACT_OUT);
        }
        return true;
    }
}
