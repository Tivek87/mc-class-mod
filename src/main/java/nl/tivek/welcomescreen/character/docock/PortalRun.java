package nl.tivek.welcomescreen.character.docock;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.spell.HeldMobs;
import nl.tivek.welcomescreen.spell.RobotArm;
import nl.tivek.welcomescreen.spell.SpellFx;
import nl.tivek.welcomescreen.spell.SpellTargeting;

/**
 * The Portal ability of the Octopus Arms. One of your tentacles reaches into a portal that opens in
 * front of you, comes out of a second portal near the creature you aim at, chases it down, grabs it,
 * hauls it back through, and slams it out of a third portal high in the sky into the ground. Then it
 * comes back through the portals. Your other tentacles keep doing their own thing the whole time.
 */
final class PortalRun {
    /** How far you may aim to start the ability. */
    static final double RANGE = 24.0;

    // Everything is deliberately unhurried, so you can follow the whole trick with your eyes.
    private static final int SEARCH_TIME = 34;
    private static final int SPIKE_TIME = 16;
    private static final int THRUST_TIME = 18;
    private static final int PORTAL_OPEN = 52;
    private static final int SKY_OPEN = 44;
    private static final int CLOSE_TIME = 34;
    private static final int GRIP_TIME = 9;
    private static final int MAX_HUNT = 120;
    private static final int MAX_DRAG = 120;
    private static final int SLAM_WAIT = 10;

    private static final double DIVE_SPEED = 0.3;
    private static final double DIVE_ACCEL = 0.2;
    private static final double HUNT_SPEED = 1.15;
    private static final double STEERING = 0.3;
    private static final double DRAG_SPEED = 0.95;
    private static final double SLAM_SPEED = 1.2;
    private static final double SLAM_ACCEL = 0.25;
    // Never so fast that a watching client has to jump the creature instead of sliding it down.
    private static final double SLAM_MAX = 2.4;
    private static final double OUT_SPEED = 0.55;
    private static final double CANCEL_OUT_SPEED = 1.2;

    private static final double MAX_TRAIL = 60.0;
    private static final double PORTAL_A_DISTANCE = 3.2;
    private static final double RADIUS_A = 1.5;
    private static final double RADIUS_B = 1.8;
    private static final double RADIUS_C = 2.3;
    private static final double SKY_HEIGHT = 26.0;
    private static final double MIN_SKY = 7.0;
    private static final double SKY_RADIUS = 10.0;
    // Nothing of this ability may ever open further than this from the player who started it.
    private static final double MAX_FROM_CASTER = 28.0;
    // How far a tentacle starts behind the portal it comes out of, so no gap shows at the ring.
    private static final double PORTAL_DEPTH = 0.3;
    private static final double CLAW_OPEN = 0.7;

    private static final int CRACK = 0x1E2226;
    private static final int DARK = 0x3A3F46;
    private static final int HEAT = 0xFF8A3A;
    private static final Vec3 DOWN = new Vec3(0, -1, 0);

    /**
     * What the tentacle is doing right now. It looks around first, then the sharp point slides out
     * between its claws, then the thrusters fold out and light up; only then does it dive.
     */
    private enum Phase {
        SEARCH, SPIKE, THRUST, DIVE, HUNT, GRIP, DRAG, SLAM, RETRACT_OUT, RETRACT_BACK, CLOSE
    }

    private final ServerPlayer caster;
    private final LivingEntity target;
    private final Vec3 look;
    private Vec3 portalA;
    private final Gate gateA = new Gate(RADIUS_A, PORTAL_OPEN);
    private final Gate gateB = new Gate(RADIUS_B, PORTAL_OPEN);
    private final Gate gateC = new Gate(RADIUS_C, SKY_OPEN);
    // The far part of the tentacle: out of portal B or C, with its own id. The near part is the arm on
    // the player's back, which the rig draws.
    private final int farArm = RobotArm.newId();
    private final List<Vec3> trail = new ArrayList<>();
    private boolean farShown;

    private Phase phase = Phase.SEARCH;
    private int phaseAge = -1;
    // How far the sharp point and the thrusters are out (0 .. 1), for the client to draw.
    private double spike;
    private double thrust;
    private boolean dived;
    private double diveTravel;
    private double diveLength = 1.0;
    private Vec3 diveFrom = Vec3.ZERO;
    @Nullable
    private Gate exit;
    private double shown;
    private Vec3 velocity = new Vec3(0, 1, 0);
    private double retractSpeed = OUT_SPEED;
    private double groundC;
    private double clawOpen = CLAW_OPEN;
    private boolean held;
    private boolean clamped;
    private int cut;

    PortalRun(ServerPlayer caster, LivingEntity target) {
        this.caster = caster;
        this.target = target;
        this.look = caster.getLookAngle();
        this.portalA = spotInFront(caster, this.look);
    }

    /** Right in front of the player, or closer when a wall is in the way. */
    private static Vec3 spotInFront(ServerPlayer caster, Vec3 look) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult wall = caster.level().clip(new ClipContext(eye, eye.add(look.scale(PORTAL_A_DISTANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double distance = wall.getType() == HitResult.Type.MISS ? PORTAL_A_DISTANCE
                : Math.max(1.6, wall.getLocation().distanceTo(eye) - 0.6);
        return eye.add(look.scale(distance)).add(0, -0.2, 0);
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
        this.releaseTarget();
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
            SpellFx.at(level, ParticleTypes.ELECTRIC_SPARK, at);
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
        this.spike = smooth((this.phaseAge + 1.0) / SPIKE_TIME);
        this.clawOpen = CLAW_OPEN + 0.25 * this.spike;
        if (this.phaseAge % 4 == 0) {
            SpellFx.at(level, ParticleTypes.CRIT, this.searchPose());
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
        this.thrust = smooth((this.phaseAge + 1.0) / THRUST_TIME);
        Vec3 at = this.searchPose();
        if (this.thrust > 0.45) {
            SpellFx.cloud(level, ParticleTypes.FLAME, at, 2, 0.12, 0.02);
            SpellFx.cloud(level, ParticleTypes.SMOKE, at, 1, 0.1, 0.01);
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
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.portalA, 20, 0.35);
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.gateB.center, 24, 0.45);
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
        if (this.target instanceof Mob mob && !HeldMobs.hold(mob)) {
            return this.cancel();
        }
        this.held = true;
        Vec3 at = this.target.getBoundingBox().getCenter();
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 16, 0.4, 0.3);
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
        this.clawOpen = CLAW_OPEN + (closed - CLAW_OPEN) * smooth((this.phaseAge + 1.0) / GRIP_TIME);
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
        SpellFx.at(level, ParticleTypes.FLASH, this.gateB.center);
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, this.gateB.center, 24, 0.4);
        SpellFx.at(level, ParticleTypes.FLASH, c);
        SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, c, 30, 0.5);
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
            SpellFx.cloud(level, ParticleTypes.FLAME, at, 3, 0.2, 0.04);
            SpellFx.cloud(level, ParticleTypes.LARGE_SMOKE, at, 2, 0.2, 0.02);
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
        this.releaseTarget();
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

    // ---- The creature ----

    private boolean targetValid(ServerLevel level) {
        return this.target.isAlive() && !this.target.isRemoved() && this.target.level() == level;
    }

    private void placeTarget(Vec3 center) {
        this.placeTarget(center, false);
    }

    /**
     * Puts the creature where the claw is. Normally it is moved, not teleported: it keeps the speed it
     * is really going and everyone is told about it every tick, so other players see it slide along
     * instead of blinking from spot to spot. Only going through a portal is a hard jump.
     */
    private void placeTarget(Vec3 center, boolean hard) {
        double y = center.y - this.target.getBbHeight() / 2;
        Vec3 was = this.target.position();
        if (this.target instanceof ServerPlayer player) {
            player.connection.teleport(center.x, y, center.z, player.getYRot(), player.getXRot());
            player.connection.aboveGroundTickCount = 0;
        } else if (hard) {
            this.target.teleportTo(center.x, y, center.z);
            this.target.setDeltaMovement(Vec3.ZERO);
        } else {
            this.target.moveTo(center.x, y, center.z);
            this.target.setDeltaMovement(center.x - was.x, y - was.y, center.z - was.z);
            this.target.hurtMarked = true;
            this.target.hasImpulse = true;
        }
        this.target.resetFallDistance();
    }

    private void releaseTarget() {
        this.clamped = false;
        if (this.held) {
            this.held = false;
            // Let go standing still: the speed it was dragged at is not a throw.
            this.target.setDeltaMovement(Vec3.ZERO);
            this.target.hurtMarked = true;
            if (this.target instanceof Mob mob) {
                HeldMobs.release(mob);
            }
        }
    }

    private void impact(ServerLevel level) {
        Vec3 at = new Vec3(this.gateC.center.x, this.groundC, this.gateC.center.z);
        DamageSource source = level.damageSources().playerAttack(this.caster);
        this.releaseTarget();
        // Smashed dead: enough to get through any armour, but bosses can never be grabbed.
        this.target.invulnerableTime = 0;
        // Half the health of an Iron Golem by default; set it in the config file.
        this.target.hurt(source, OctoRig.damageOf("portal"));
        this.target.setDeltaMovement(0, 0.35, 0);

        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    90, 1.2, 0.3, 1.2, 0.45);
        }
        SpellFx.cloud(level, ParticleTypes.EXPLOSION, at.add(0, 0.5, 0), 4, 0.8, 0.0);
        SpellFx.shockwave(level, ParticleTypes.LARGE_SMOKE, at.add(0, 0.2, 0), 36, 0.4);
        SpellFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.3, 0), 44, 0.8);
        SpellFx.cloud(level, ParticleTypes.CRIT, at.add(0, 0.5, 0), 24, 0.6, 0.5);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + SpellFx.spread(0.3);
            double length = 2.2 + SpellFx.RANDOM.nextDouble() * 1.8;
            Vec3 end = at.add(Math.cos(angle) * length, 0.05, Math.sin(angle) * length);
            SpellFx.zigzag(level, SpellFx.dust(CRACK, 1.0F), at.add(0, 0.05, 0), end, 4, 0.35, 0.15);
        }
        SpellFx.ring(level, SpellFx.fade(HEAT, DARK, 1.8F), at.add(0, 0.15, 0), 1.6, 32, 0);
        sound(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.5F, 0.7F);
        sound(level, at, SoundEvents.ANVIL_LAND, 0.9F, 0.5F);
        sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.7F);
    }

    // ---- Where portals B and C go ----

    /** Portal B: near the creature, facing it, with open air between them; preferably in your view. */
    private void openEntry(ServerLevel level) {
        Vec3 goal = this.target.getBoundingBox().getCenter();
        Vec3 eye = this.caster.getEyePosition();
        Vec3 toCaster = new Vec3(eye.x - goal.x, 0, eye.z - goal.z);
        if (toCaster.lengthSqr() < 1.0E-4) {
            toCaster = new Vec3(-this.look.x, 0, -this.look.z);
        }
        toCaster = toCaster.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : toCaster.normalize();
        // The three portals are pulled apart, but never further than the creature itself is away:
        // otherwise a portal for a creature right next to you would open somewhere behind your back.
        double spread = Math.min(OctoRig.ability("portal").value("portalSpreadBlocks"),
                Math.max(2.0, goal.distanceTo(this.portalA) * 0.5));
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double angle : new double[] { 0.9, -0.9, 1.4, -1.4, 0.45, -0.45, 2.0, -2.0, 0.0 }) {
            for (double distance : new double[] { 7.0, 5.5, 4.0 }) {
                for (double height : new double[] { 2.0, 3.5, 1.0 }) {
                    Vec3 center = goal.add(turn(toCaster, angle).scale(distance)).add(0, height, 0);
                    Vec3 normal = goal.subtract(center).normalize();
                    if (!fits(level, center, normal, RADIUS_B, this.caster)
                            || !SpellTargeting.clearPath(level, center, goal, this.caster)) {
                        continue;
                    }
                    // Well away from the first portal: the three of them must never sit on one heap.
                    double fromA = center.distanceTo(this.portalA);
                    double score = (SpellTargeting.clearPath(level, eye, center, this.caster) ? 2.0 : 0.0)
                            + (fromA >= spread ? 2.5 : fromA / spread * 2.5) + Math.min(fromA, 24.0) * 0.12
                            - Math.abs(Math.abs(angle) - 0.9) * 0.5 - Math.abs(distance - 5.5) * 0.2
                            - Math.abs(height - 2.0) * 0.2;
                    if (score > bestScore) {
                        bestScore = score;
                        best = center;
                    }
                }
            }
        }
        if (best == null) {
            // Nothing fits around it: straight above the creature, on the far side from you.
            best = goal.add(toCaster.scale(-2.5)).add(0, 1.5, 0);
        }
        best = this.pushApart(best, this.portalA, spread);
        best = this.nearCaster(best);
        Vec3 normal = goal.subtract(best);
        this.gateB.open(level, best, normal.lengthSqr() < 1.0E-4 ? toCaster.scale(-1) : normal);
    }

    /** Moves {@code point} away from {@code other} until they are at least {@code apart} blocks apart. */
    private Vec3 pushApart(Vec3 point, Vec3 other, double apart) {
        Vec3 away = point.subtract(other);
        double distance = away.length();
        if (distance >= apart) {
            return point;
        }
        Vec3 direction = distance < 1.0E-4 ? new Vec3(0, 1, 0) : away.scale(1.0 / distance);
        return other.add(direction.scale(apart));
    }

    /**
     * A last check that nothing of this ability ever opens far away (at the world spawn, say): a spot
     * further than {@link #MAX_FROM_CASTER} is pulled back in to the player.
     */
    private Vec3 nearCaster(Vec3 point) {
        Vec3 eye = this.caster.getEyePosition();
        Vec3 away = point.subtract(eye);
        double distance = away.length();
        if (distance <= MAX_FROM_CASTER || distance < 1.0E-4) {
            return point;
        }
        return eye.add(away.scale(MAX_FROM_CASTER / distance));
    }

    /** Portal C: high in the sky near the creature, with open air down to the ground. */
    private void openSky(ServerLevel level) {
        Vec3 origin = this.target.position();
        Vec3 eye = this.caster.getEyePosition();
        Vec3 view = this.caster.getLookAngle();
        Vec3 best = null;
        double bestGround = origin.y;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = SpellFx.RANDOM.nextDouble() * Math.PI * 2;
            double distance = attempt == 0 ? 0.0 : 1.0 + SpellFx.RANDOM.nextDouble() * (SKY_RADIUS - 1.0);
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            double ground = SpellTargeting.floorBelow(level, BlockPos.containing(x, origin.y + 3, z));
            Vec3 floor = new Vec3(x, ground, z);
            double room = this.room(level, floor);
            if (room < MIN_SKY) {
                continue;
            }
            Vec3 portal = floor.add(0, room, 0);
            if (!fits(level, portal, DOWN, RADIUS_C * 0.8, this.caster)) {
                continue;
            }
            double facing = view.dot(portal.subtract(eye).normalize());
            // Far from the second portal as well, so the three rings are never close together.
            double fromB = Math.sqrt(portal.subtract(this.gateB.center).horizontalDistanceSqr());
            double score = room / SKY_HEIGHT + facing + Math.min(fromB, SKY_RADIUS) * 0.15
                    + (SpellTargeting.clearPath(level, eye, portal, this.caster) ? 1.0 : 0.0)
                    + (SpellTargeting.clearPath(level, eye, floor.add(0, 0.6, 0), this.caster) ? 0.5 : 0.0)
                    - distance * 0.03;
            if (score > bestScore) {
                bestScore = score;
                best = portal;
                bestGround = ground;
            }
        }
        if (best == null) {
            // No open sky anywhere near (a cave): as high as it goes right above the creature.
            Vec3 floor = new Vec3(origin.x, origin.y, origin.z);
            best = floor.add(0, Math.max(3.0, this.room(level, floor)), 0);
            bestGround = origin.y;
        }
        this.groundC = bestGround;
        this.gateC.open(level, this.nearCaster(best), DOWN);
    }

    private double room(ServerLevel level, Vec3 floor) {
        BlockHitResult hit = level.clip(new ClipContext(floor.add(0, 0.5, 0), floor.add(0, SKY_HEIGHT, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
        return hit.getType() == HitResult.Type.MISS ? SKY_HEIGHT : hit.getLocation().y - floor.y - 1.0;
    }

    // ---- Drawing the far part ----

    private Vec3 farTip() {
        List<Vec3> shown = RobotArm.firstPart(this.trail, this.shown);
        return shown.get(shown.size() - 1);
    }

    private void drawFar(ServerLevel level) {
        if (this.exit == null || this.shown <= PORTAL_DEPTH + 1.0E-3) {
            if (this.farShown) {
                this.farShown = false;
                RobotArm.remove(level, this.farArm);
            }
            return;
        }
        RobotArm.arm(this.farArm, RobotArm.firstPart(this.trail, this.shown)).claw(this.clawOpen)
                .holding(this.clamped ? this.target : null).cut(this.cut).tools(this.spike, this.thrust)
                .clip(this.exit.center, this.exit.normal.scale(-1)).send(level);
        this.farShown = true;
        if (this.thrust > 0.3) {
            // The flame trail behind the boosting tentacle.
            Vec3 at = this.farTip();
            SpellFx.cloud(level, ParticleTypes.FLAME, at, 2, 0.15, 0.02);
            if (SpellFx.chance(0.5)) {
                SpellFx.at(level, ParticleTypes.SMOKE, at);
            }
        }
    }

    // ---- Helpers ----

    private static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3 - 2 * t);
    }

    private static Vec3 turn(Vec3 v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** True when a portal of this size fits here without cutting into blocks. */
    private static boolean fits(ServerLevel level, Vec3 center, Vec3 normal, double radius, ServerPlayer caster) {
        if (!openSpace(level, center)) {
            return false;
        }
        Vec3[] b = SpellFx.basis(normal.normalize());
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            Vec3 rim = center.add(b[0].scale(Math.cos(a) * radius * 0.85))
                    .add(b[1].scale(Math.sin(a) * radius * 0.85));
            if (!openSpace(level, rim)) {
                return false;
            }
        }
        return true;
    }

    private static boolean openSpace(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** One tech portal: opens slowly, and shrinks shut backwards when told to. */
    private static final class Gate {
        private final int id = RobotArm.newId();
        private final double size;
        private final int openTime;
        private Vec3 center = Vec3.ZERO;
        private Vec3 normal = new Vec3(0, 1, 0);
        private int age = -1;
        private int closing = -1;
        private boolean gone;

        private Gate(double size, int openTime) {
            this.size = size;
            this.openTime = openTime;
        }

        private void open(ServerLevel level, Vec3 center, Vec3 normal) {
            this.center = center;
            this.normal = normal.normalize();
            this.age = 0;
            SpellFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, center, 24, 0.35);
            sound(level, center, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
            sound(level, center, SoundEvents.PISTON_EXTEND, 0.8F, 0.6F);
        }

        private boolean isOpen() {
            return this.age >= 0 && !this.gone;
        }

        /** Open far enough to go through: the energy field is (almost) all there. */
        private boolean ready() {
            return this.isOpen() && this.closing < 0 && this.age >= this.openTime * 0.9;
        }

        private void close() {
            if (this.isOpen() && this.closing < 0) {
                this.closing = 0;
            }
        }

        private void tick(ServerLevel level) {
            if (!this.isOpen()) {
                return;
            }
            double open;
            if (this.closing >= 0) {
                if (this.closing == 0) {
                    sound(level, this.center, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
                    sound(level, this.center, SoundEvents.PISTON_CONTRACT, 0.7F, 0.6F);
                }
                this.closing++;
                if (this.closing >= CLOSE_TIME) {
                    SpellFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.center, 14, 0.3, 0.1);
                    sound(level, this.center, SoundEvents.IRON_DOOR_CLOSE, 0.7F, 0.6F);
                    this.remove(level);
                    return;
                }
                open = Math.min(1.0, (double) this.age / this.openTime) * (1.0 - (double) this.closing / CLOSE_TIME);
            } else {
                open = Math.min(1.0, (this.age + 1.0) / this.openTime);
                this.openSounds(level);
            }
            this.age++;
            RobotArm.portal(level, this.id, this.center, this.normal, this.size, open);
        }

        /** Clanks while the ring assembles, a ticking lamp at a time, and a surge when the energy opens. */
        private void openSounds(ServerLevel level) {
            if (this.age > this.openTime) {
                return;
            }
            double p = (double) this.age / this.openTime;
            if (p < 0.45 && this.age % 4 == 0) {
                sound(level, this.center, SoundEvents.CHAIN_PLACE, 0.6F, 0.7F + (float) p);
            } else if (p >= 0.45 && p < 0.7 && this.age % 2 == 0) {
                sound(level, this.center, SoundEvents.STONE_BUTTON_CLICK_ON, 0.5F, 1.2F + (float) p);
            }
            if (this.age == (int) (this.openTime * 0.55)) {
                sound(level, this.center, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
                sound(level, this.center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8F, 1.2F);
            }
        }

        private void remove(ServerLevel level) {
            if (this.isOpen()) {
                this.gone = true;
                RobotArm.removePortal(level, this.id);
            }
        }
    }
}
