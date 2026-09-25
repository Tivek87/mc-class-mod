package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.fair;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.inReach;

/**
 * What every hand of the Giant Hands keeps and does, whatever its move (see {@link GiantHand}): where it came up
 * and what it reaches for, turning after the creature nearest to it, throwing up dust, striking creatures and
 * telling the players near it where it is.
 */
abstract class GiantHandBase {
    // How far round its base a hand looks for the creature nearest to it, and how much nearer another must be than
    // the one it is after before it turns to that one instead (so it never flickers between two), in blocks.
    private static final double HOMING = 10.0;
    private static final double SWITCH = 1.5;
    // A hand turns round its base after its creature as a thing this big turns: never faster than TURN radians a tick,
    // gathering or losing no more than TURN_GATHER of that speed a tick, and easing in over the last stretch (EASE:
    // the part of what is left it turns in a tick there). The spot it reaches for stays at least NEAREST of how far
    // from its base it came up (and never nearer than NEAREST_LEAST blocks), so a creature that runs past close by or
    // right over it never swings it round.
    private static final double TURN = 0.07;
    private static final double TURN_GATHER = 0.01;
    private static final double EASE = 0.2;
    private static final double NEAREST = 0.6;
    private static final double NEAREST_LEAST = 1.2;
    // How the spot a hand reaches for moves in and out along the way it faces, and how a pair's spot follows its
    // creature over the ground: never faster than FOLLOW blocks a tick, gathering or losing no more than GATHER of that
    // speed a tick; a pair's is pulled after it as by a spring (how hard, next to how far off it is), braked just
    // enough never to run past it.
    private static final double FOLLOW = 0.3;
    private static final double GATHER = 0.05;
    private static final double SPRING = 0.08;
    private static final double VIEW_RANGE = 128.0;

    // The Giant Hands that called it: who it strikes for, and with what.
    final GiantHands storm;
    private final int id = PowerRing.newId();
    final int variant;
    final int move;
    final Vec3 base;
    // The spot on the ground it reaches for, and the creature it is after. A hand keeps that spot as the way it
    // faces round its base (radians, see way) and how far out along it (blocks), each with how much it changed the
    // last tick; a pair moves it over the ground instead, by drift a tick.
    Vec3 aim;
    private double facing;
    private double turn;
    private double out;
    private double outSpeed;
    private Vec3 drift = Vec3.ZERO;
    LivingEntity target;
    @Nullable
    LivingEntity held;
    // What a slam presses flat.
    final List<LivingEntity> pressed = new ArrayList<>();
    int t;

    GiantHandBase(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        this.storm = storm;
        this.variant = variant;
        this.move = HandPose.move(variant);
        this.base = base;
        this.target = target;
        this.aim = this.within(new Vec3(target.getX(), base.y, target.getZ()));
        if (this.move != HandPose.AXE) {
            Vec3 to = this.aim.subtract(base);
            this.facing = to.x * to.x + to.z * to.z < 1.0E-8 ? 0.0 : Math.atan2(to.x, to.z);
            this.out = Math.max(this.nearest(), Math.sqrt(to.x * to.x + to.z * to.z));
            this.aim = base.add(way(this.facing).scale(this.out));
        }
    }

    /** How the hand stands now, out in the world. */
    HandPose.Place place() {
        return this.pose(this.t).place(this.base, this.aim.subtract(this.base), SCALE);
    }

    HandPose pose(double t) {
        return HandPose.at(this.variant, t, this.reach());
    }

    /** How far off along the ground the spot it reaches for is, at the size the hand is made at. */
    private double reach() {
        Vec3 to = this.aim.subtract(this.base);
        return Math.sqrt(to.x * to.x + to.z * to.z) / SCALE;
    }

    /**
     * It turns after the creature nearest to it (see {@link #after}), smoothly, as a thing this big turns: a hand
     * turns round its base towards it, gathering speed, gliding and slowing down in time to stop right facing it,
     * and reaches in or out to how far off it is. The nearer that creature is to its base the slower it turns
     * after it, so one that runs past close by or right over it never swings it round. With no creature left, or
     * while it strikes or holds something, it goes after nothing and glides to a stop. A pair's spot is pulled
     * over the ground after its creature instead (see {@link #homeSpot}).
     */
    void home(ServerLevel level) {
        if (this.move == HandPose.AXE) {
            this.homeSpot(level);
            return;
        }
        LivingEntity after = HandPose.locked(this.variant, this.t) ? null : this.after(level);
        double near = this.nearest();
        double off = 0.0;
        double most = TURN;
        double wanted = this.out;
        if (after != null) {
            double dx = after.getX() - this.base.x;
            double dz = after.getZ() - this.base.z;
            double far = Math.sqrt(dx * dx + dz * dz);
            off = Math.IEEEremainder(Math.atan2(dx, dz) - this.facing, Math.PI * 2.0);
            // Nearly behind it: it goes on round the way it already turns, never back and forth.
            if (off * this.turn < 0.0 && Math.abs(off) > Math.PI * 0.75) {
                off += Math.copySign(Math.PI * 2.0, this.turn);
            }
            // Close by its base the way to it means little: the closer, the slower it turns after it.
            most = TURN * Ease.smooth(far / near);
            wanted = Math.max(near, far);
        }
        this.turn = follow(this.turn, off, TURN_GATHER, most);
        this.facing = Math.IEEEremainder(this.facing + this.turn, Math.PI * 2.0);
        this.outSpeed = follow(this.outSpeed, wanted - this.out, GATHER, FOLLOW);
        this.out += this.outSpeed;
        if (this.out < near) {
            this.out = near;
            this.outSpeed = Math.max(0.0, this.outSpeed);
        }
        this.aim = this.base.add(way(this.facing).scale(this.out));
    }

    /**
     * A pair's spot on the ground is pulled after its creature as by a spring braked just enough never to run past
     * it: it gathers speed, glides and slows down as it gets there, and with no creature left it glides to a stop.
     * From the moment its axe goes up, the spot it strikes stays put.
     */
    private void homeSpot(ServerLevel level) {
        if (HandPose.locked(this.variant, this.t)) {
            this.drift = Vec3.ZERO;
            return;
        }
        LivingEntity after = this.after(level);
        Vec3 want = after == null ? this.aim : this.within(new Vec3(after.getX(), this.base.y, after.getZ()));
        Vec3 pull = want.subtract(this.aim).scale(SPRING).subtract(this.drift.scale(2.0 * Math.sqrt(SPRING)));
        double hard = pull.length();
        if (hard > GATHER) {
            pull = pull.scale(GATHER / hard);
        }
        Vec3 drift = this.drift.add(pull);
        double speed = drift.length();
        if (speed > FOLLOW) {
            drift = drift.scale(FOLLOW / speed);
        }
        Vec3 next = this.within(this.aim.add(drift));
        // How far it really moved: what a pair's reach held back is not kept for the next tick.
        this.drift = next.subtract(this.aim);
        this.aim = next;
    }

    /**
     * The creature it turns after: the one nearest to its base within its reach, but it keeps to the one it is
     * after until another is {@link #SWITCH} nearer, so it never flickers between two; null for none.
     */
    @Nullable
    private LivingEntity after(ServerLevel level) {
        double reach = HOMING * SCALE;
        boolean keeps = this.target.isAlive() && this.target.level() == level
                && fair(this.storm.owner, this.target) && this.flat(this.target) <= reach;
        LivingEntity nearest = keeps ? this.target : null;
        double best = keeps ? this.flat(this.target) - SWITCH * SCALE : reach;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.base, this.base).inflate(reach, reach * 0.6, reach),
                entity -> fair(this.storm.owner, entity))) {
            double distance = this.flat(living);
            if (living != this.target && distance < best) {
                best = distance;
                nearest = living;
            }
        }
        if (nearest != null) {
            this.target = nearest;
        }
        return nearest;
    }

    /** How near its base the spot a hand reaches for may come, in blocks. */
    private double nearest() {
        return Math.max(NEAREST_LEAST, NEAREST * HandPose.spot(this.move)) * SCALE;
    }

    /** For a pair, the spot kept within its axe's reach round where it was called; for a hand, as it is. */
    private Vec3 within(Vec3 spot) {
        return this.move == HandPose.AXE ? inReach(this.base, spot) : spot;
    }

    private double flat(LivingEntity living) {
        double dx = living.getX() - this.base.x;
        double dz = living.getZ() - this.base.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Bits of the ground and dust thrown up round its base. */
    void dust(ServerLevel level, int count) {
        BlockPos under = BlockPos.containing(this.base.x, this.base.y - 0.5, this.base.z);
        BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (ground != null && !ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), this.base.x,
                    this.base.y + 0.3, this.base.z, count, 1.1, 0.4, 1.1, 0.25);
        }
        ParticleFx.send(level, ParticleTypes.CLOUD, this.base.x, this.base.y + 0.5, this.base.z, count / 2, 1.3,
                0.4, 1.3, 0.05);
    }

    /** It lets go of what it holds, if anything: a mob gets its own will back. */
    void letGo() {
        if (this.held != null) {
            GRABBED.remove(this.held.getId(), this);
            if (this.held instanceof Mob mob) {
                HeldMobs.release(mob);
            }
            this.held = null;
        }
    }

    /** Bits of the ground and dust thrown up where a blow lands. */
    void dustAt(ServerLevel level, Vec3 at, int count) {
        this.dustAt(level, at, count, 1.0);
    }

    /** Bits of the ground and dust thrown up where a blow lands, {@code wide} blocks every way round it. */
    void dustAt(ServerLevel level, Vec3 at, int count, double wide) {
        BlockPos under = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (ground != null && !ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    count, wide, 0.2, wide, 0.2);
        }
        ParticleFx.send(level, ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, count / 3, wide, 0.2, wide, 0.06);
    }

    /** Every fair creature within {@code range} of its base. */
    List<LivingEntity> near(ServerLevel level, double range) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(this.base, this.base).inflate(range * SCALE),
                entity -> fair(this.storm.owner, entity));
    }

    /**
     * Strikes a creature: hurt, and thrown the way {@code away} (flat; never back towards him, see awayFromHim) and
     * up, as hard as the knockback setting says next to {@code out} and {@code up}.
     */
    void hit(ServerLevel level, LivingEntity living, double damage, Vec3 away, double out, double up) {
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(this.storm.owner), (float) damage);
        double knockback = this.storm.ability.value("knockback");
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        Vec3 flung = this.storm.awayFromHim(living, away);
        Vec3 push = flung.scale(out * knockback).add(0.0, up * Math.min(1.0, knockback), 0.0).scale(1.0 - resist);
        if (push.lengthSqr() > 1.0E-6) {
            living.setDeltaMovement(living.getDeltaMovement().add(push));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), living.getBoundingBox().getCenter(), 8,
                0.3, 0.05);
    }

    /** It is done, or broken off: it lets go of what it holds and is gone. */
    void end(ServerLevel level) {
        this.letGo();
        ConstructPayload.sendRemove(level, this.id, this.base);
    }

    void send(ServerLevel level) {
        PacketDistributor.sendToPlayersNear(level, null, this.base.x, this.base.y, this.base.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.storm.owner.getId(), this.base,
                        this.aim.subtract(this.base), (float) SCALE, 1.0F,
                        this.held == null ? 0.0F : LightBubble.caught(this.held.getId()), this.held != null,
                        ConstructPayload.HAND, this.variant, this.t, null));
    }

    /** The flat way a hand faces round its base, one long, for how far round it is turned (0: along +z). */
    private static Vec3 way(double facing) {
        return new Vec3(Math.sin(facing), 0.0, Math.cos(facing));
    }

    /**
     * A speed after one more tick of going after something {@code off} away: as fast as it may (never over
     * {@code most} a tick) without running past it, slowing down in time and easing in over the last stretch
     * ({@link #EASE}), and never gathering or losing more than {@code gather} of speed a tick.
     */
    private static double follow(double speed, double off, double gather, double most) {
        double far = Math.abs(off);
        double want = Math.copySign(Math.min(most, Math.min(Math.sqrt(1.6 * gather * far), EASE * far)), off);
        return speed + Mth.clamp(want - speed, -gather, gather);
    }
}
