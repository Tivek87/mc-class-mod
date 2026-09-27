package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

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
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.fair;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.inReach;

abstract class GiantHandBase {
    private static final double HOMING = 10.0;
    private static final double SWITCH = 1.5;
    private static final double TURN = 0.07;
    private static final double TURN_GATHER = 0.01;
    private static final double EASE = 0.2;
    private static final double NEAREST = 0.6;
    private static final double NEAREST_LEAST = 1.2;
    private static final double FOLLOW = 0.3;
    private static final double GATHER = 0.05;
    private static final double SPRING = 0.08;
    // Even as it strikes a hand keeps turning after its creature, this much as fast and never to another one, so a
    // creature that moves on is still hit.
    private static final double LATE = 0.5;
    private static final double VIEW_RANGE = 128.0;
    private static final int PORTAL_SETTLES = 6;

    final GiantHands storm;
    private final int id = PowerRing.newId();
    final int variant;
    final int move;
    // Only a portal hand's base moves: its portal glides after the creature until the hand is committed.
    Vec3 base;
    Vec3 aim;
    private double facing;
    private double turn;
    private double out;
    private double outSpeed;
    private Vec3 drift = Vec3.ZERO;
    LivingEntity target;
    @Nullable
    LivingEntity held;
    final List<LivingEntity> pressed = new ArrayList<>();
    int t;
    @Nullable
    GiantHandRoom room;
    @Nullable
    private HandPose.Place placed;
    private int placedAt = -1;
    @Nullable
    private Vec3 placedBase;
    @Nullable
    private Vec3 placedAim;

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

    // A portal hand faces the way it is given, not towards the creature: its portal is placed round that.
    GiantHandBase(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        this(storm, variant, base, target);
        this.facing = Math.atan2(facing.x, facing.z);
        this.out = this.nearest();
        this.aim = base.add(way(this.facing).scale(this.out));
    }

    // Worked out once a tick: a move asks for it several times, and base and aim are only ever replaced, not changed.
    HandPose.Place place() {
        if (this.placed == null || this.placedAt != this.t || this.placedBase != this.base
                || this.placedAim != this.aim) {
            this.placed = this.pose(this.t).place(this.base, this.aim.subtract(this.base), SCALE);
            this.placedAt = this.t;
            this.placedBase = this.base;
            this.placedAim = this.aim;
        }
        return this.placed;
    }

    int id() {
        return this.id;
    }

    HandPose pose(double t) {
        return HandPose.at(this.variant, t, this.reach());
    }

    private double reach() {
        Vec3 to = this.aim.subtract(this.base);
        return Math.sqrt(to.x * to.x + to.z * to.z) / SCALE;
    }

    void home(ServerLevel level) {
        if (this.move == HandPose.AXE) {
            this.homeSpot(level);
            return;
        }
        if (HandPose.portal(this.variant)) {
            this.homePortal(level);
            return;
        }
        boolean locked = HandPose.locked(this.variant, this.t);
        LivingEntity after = locked ? this.kept(level) : this.after(level);
        double near = this.nearest();
        double off = 0.0;
        double most = TURN;
        double wanted = this.out;
        if (after != null) {
            double dx = after.getX() - this.base.x;
            double dz = after.getZ() - this.base.z;
            double far = Math.sqrt(dx * dx + dz * dz);
            // A hand out of a wall stays square to its wall: it only reaches further or less far.
            off = HandPose.wall(this.variant) ? 0.0
                    : Math.IEEEremainder(Math.atan2(dx, dz) - this.facing, Math.PI * 2.0);
            // Nearly behind it: it goes on round the way it already turns, never back and forth.
            if (off * this.turn < 0.0 && Math.abs(off) > Math.PI * 0.75) {
                off += Math.copySign(Math.PI * 2.0, this.turn);
            }
            most = TURN * Ease.smooth(far / near) * (locked ? LATE : 1.0);
            wanted = Math.max(near, far);
        }
        this.turn = follow(this.turn, off, TURN_GATHER, most);
        this.outSpeed = follow(this.outSpeed, wanted - this.out, GATHER, locked ? FOLLOW * LATE : FOLLOW);
        if (this.turn == 0.0 && this.outSpeed == 0.0) {
            return;
        }
        double facing = Math.IEEEremainder(this.facing + this.turn, Math.PI * 2.0);
        double out = Math.max(near, this.out + this.outSpeed);
        Vec3 aim = this.base.add(way(facing).scale(out));
        // Turning after the creature never swings the hand into a wall: it waits there instead.
        Vec3 root = HandPose.rootNormal(this.variant, aim.subtract(this.base));
        Vec3 reach = aim.subtract(this.base);
        double floor = Double.NEGATIVE_INFINITY;
        if (!GiantHandSpots.clearAt(level, this, this.t + 1, reach, this.base, root, floor)
                || !GiantHandSpots.clearAt(level, this, this.t + 4, reach, this.base, root, floor)) {
            this.turn = 0.0;
            this.outSpeed = 0.0;
            return;
        }
        this.facing = facing;
        if (out <= near) {
            this.outSpeed = Math.max(0.0, this.outSpeed);
        }
        this.out = out;
        this.aim = aim;
    }

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
        this.drift = next.subtract(this.aim);
        this.aim = next;
    }

    // The portal glides after the creature, the hand still turned the same way, until the moment it acts draws near.
    private void homePortal(ServerLevel level) {
        // The catch keeps right under its falling creature until the moment it closes on it.
        if (this.move == HandPose.CATCH) {
            if (this.t < HandPose.CATCH_CATCHES && this.held == null && this.target.isAlive()) {
                Vec3 next = GiantHands.portalFor(this.target, this.variant, way(this.facing));
                this.aim = this.aim.add(next.subtract(this.base));
                this.base = next;
                this.room = null;
            }
            return;
        }
        int stops = HandPose.firstAct(this.variant) - PORTAL_SETTLES;
        if (this.t >= stops || this.held != null) {
            this.drift = Vec3.ZERO;
            return;
        }
        LivingEntity after = this.after(level);
        if (after == null) {
            return;
        }
        Vec3 want = GiantHands.portalFor(after, this.variant, way(this.facing));
        Vec3 pull = want.subtract(this.base).scale(SPRING).subtract(this.drift.scale(2.0 * Math.sqrt(SPRING)));
        double hard = pull.length();
        if (hard > GATHER) {
            pull = pull.scale(GATHER / hard);
        }
        Vec3 drift = this.drift.add(pull);
        double speed = drift.length();
        if (speed > FOLLOW) {
            drift = drift.scale(FOLLOW / speed);
        }
        Vec3 next = this.base.add(drift);
        if (!GiantHands.open(level, next)) {
            this.drift = Vec3.ZERO;
            return;
        }
        this.drift = drift;
        this.base = next;
        this.aim = this.base.add(way(this.facing).scale(this.out));
        this.room = null;
    }

    @Nullable
    private LivingEntity after(ServerLevel level) {
        double reach = HOMING * SCALE;
        LivingEntity nearest = this.kept(level);
        double best = nearest != null ? this.flat(nearest) - SWITCH * SCALE : reach;
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

    // Its own creature, while it is still there to reach for.
    @Nullable
    private LivingEntity kept(ServerLevel level) {
        return this.target.isAlive() && this.target.level() == level && fair(this.storm.owner, this.target)
                && this.flat(this.target) <= HOMING * SCALE ? this.target : null;
    }

    private double nearest() {
        return Math.max(NEAREST_LEAST, NEAREST * HandPose.spot(this.move)) * SCALE;
    }

    private Vec3 within(Vec3 spot) {
        return this.move == HandPose.AXE ? inReach(this.base, spot) : spot;
    }

    private double flat(LivingEntity living) {
        double dx = living.getX() - this.base.x;
        double dz = living.getZ() - this.base.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    void dust(ServerLevel level, int count) {
        // The block the hand broke out of: under it, or behind it in a wall.
        BlockPos under = BlockPos.containing(this.base.subtract(HandPose.rootNormal(this.variant,
                this.aim.subtract(this.base)).scale(0.5)));
        BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (ground != null && !ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), this.base.x,
                    this.base.y + 0.3, this.base.z, count, 1.1, 0.4, 1.1, 0.25);
        }
        ParticleFx.send(level, ParticleTypes.CLOUD, this.base.x, this.base.y + 0.5, this.base.z, count / 2, 1.3,
                0.4, 1.3, 0.05);
    }

    void letGo() {
        if (this.held != null) {
            GRABBED.remove(this.held.getId(), this);
            if (this.held instanceof Mob mob) {
                HeldMobs.release(mob);
            }
            this.held = null;
        }
    }

    void dustAt(ServerLevel level, Vec3 at, int count) {
        this.dustAt(level, at, count, 1.0);
    }

    void dustAt(ServerLevel level, Vec3 at, int count, double wide) {
        BlockPos under = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (ground != null && !ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    count, wide, 0.2, wide, 0.2);
        }
        ParticleFx.send(level, ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, count / 3, wide, 0.2, wide, 0.06);
    }

    List<LivingEntity> near(ServerLevel level, double range) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(this.base, this.base).inflate(range * SCALE),
                entity -> fair(this.storm.owner, entity));
    }

    void hit(ServerLevel level, LivingEntity living, double damage, Vec3 away, double out, double up) {
        // Hits in quick succession all land.
        living.invulnerableTime = 0;
        String hand = HandPose.HANDS[HandPose.settingsOf(this.move)];
        living.hurt(level.damageSources().playerAttack(this.storm.owner),
                (float) (damage * this.storm.ability.value(hand + "Damage")));
        this.push(living, away, out, up);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), living.getBoundingBox().getCenter(), 8,
                0.3, 0.05);
    }

    void push(LivingEntity living, Vec3 away, double out, double up) {
        String hand = HandPose.HANDS[HandPose.settingsOf(this.move)];
        double knockback = this.storm.ability.value("knockback") * this.storm.ability.value(hand + "Knockback");
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        Vec3 flung = this.storm.awayFromHim(living, away);
        Vec3 push = flung.scale(out * knockback).add(0.0, up * Math.min(1.0, knockback), 0.0).scale(1.0 - resist);
        if (push.lengthSqr() > 1.0E-6) {
            living.setDeltaMovement(living.getDeltaMovement().add(push));
            living.hasImpulse = true;
            // Players move themselves on their own client, so they have to be told about the push.
            living.hurtMarked = true;
        }
    }

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

    private static Vec3 way(double facing) {
        return new Vec3(Math.sin(facing), 0.0, Math.cos(facing));
    }

    private static double follow(double speed, double off, double gather, double most) {
        double far = Math.abs(off);
        double want = Math.copySign(Math.min(most, Math.min(Math.sqrt(1.6 * gather * far), EASE * far)), off);
        return speed + Mth.clamp(want - speed, -gather, gather);
    }
}
