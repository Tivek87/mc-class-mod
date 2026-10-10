package nl.tivek.multiversepowers.character.greenlantern.minion;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// A mech's helper's three moves on its target, each with a wait of its own: a jab and a hook up close, a bolt of light
// from the cannon on its right forearm from further off, and a leap that slams down on it from 5 to 11 blocks away.
// Between them it walks at its target. Every hit lands as its pilot's own, on anyone they may hit.
public final class MinionMoves extends Goal {
    public static final int PUNCH = 1;
    public static final int CANNON = 2;
    public static final int LEAP = 3;
    public static final int SLAM = 4;
    static final int KINDS = 4;
    // The punches: the left jab lands at JAB, the right hook at HOOK; over at PUNCH_END.
    public static final int JAB = 5;
    public static final int HOOK = 11;
    public static final int PUNCH_END = 16;
    // The cannon charges till FIRE and comes down by CANNON_END.
    public static final int FIRE = 14;
    public static final int CANNON_END = 22;
    // The leap: crouched, it springs at SPRING; landing, it slams the ground (SLAM), over at SLAM_END.
    public static final int SPRING = 6;
    public static final int SLAM_END = 12;
    private static final int AIR_MOST = 40;
    private static final double REACH = 2.0;
    private static final double SHOT_FROM = 3.5;
    private static final double SHOT_MOST = 16.0;
    private static final double LEAP_FROM = 5.0;
    private static final double LEAP_MOST = 11.0;
    private static final double SLAM_RADIUS = 3.2;
    private static final int PUNCH_WAIT = 8;
    private static final int CANNON_WAIT = 40;
    private static final int LEAP_WAIT = 120;
    private static final int GLOW = 0xE4FFEA;

    private final MechMinion minion;
    private int kind;
    private int t;
    private int punchReady;
    private int cannonReady;
    private int leapReady;
    private int repath;

    MinionMoves(MechMinion minion) {
        this.minion = minion;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.minion.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.kind = 0;
        this.minion.aim(null);
        this.minion.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.minion.getTarget();
        ServerPlayer pilot = this.minion.pilot();
        if (target != null) {
            this.minion.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target, pilot);
            return;
        }
        if (target == null || pilot == null && !this.minion.wild()) {
            return;
        }
        this.minion.aim(target);
        int now = this.minion.tickCount;
        double far = this.gap(target);
        boolean seen = this.minion.getSensing().hasLineOfSight(target);
        if (far <= REACH && now >= this.punchReady) {
            this.begin(PUNCH);
        } else if (far >= LEAP_FROM && far <= LEAP_MOST && seen && now >= this.leapReady && this.minion.onGround()) {
            this.begin(LEAP);
        } else if (far >= SHOT_FROM && far <= SHOT_MOST && seen && now >= this.cannonReady) {
            this.begin(CANNON);
        } else if (far > REACH * 0.8 && --this.repath <= 0) {
            this.repath = 8;
            this.minion.getNavigation().moveTo(target, 1.15);
        }
    }

    // From its body's side to its target's.
    private double gap(LivingEntity target) {
        return Math.sqrt(this.minion.distanceToSqr(target)) - (target.getBbWidth() + this.minion.getBbWidth()) * 0.5;
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.minion.getNavigation().stop();
        this.minion.start(kind);
        ServerLevel level = (ServerLevel) this.minion.level();
        Vec3 at = this.minion.position().add(0.0, 2.6, 0.0);
        switch (kind) {
            case PUNCH -> Sounds.play(level, at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.3F);
            case CANNON -> Sounds.play(level, at, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.8F);
            case LEAP -> Sounds.play(level, at, SoundEvents.IRON_GOLEM_STEP, 1.5F, 0.6F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target, @Nullable ServerPlayer pilot) {
        ServerLevel level = (ServerLevel) this.minion.level();
        switch (this.kind) {
            case PUNCH -> {
                boolean lands = this.t == JAB || this.t == HOOK;
                if (lands && target != null && this.gap(target) <= REACH + 0.6) {
                    Vec3 away = this.flatWay(target).scale(this.t == HOOK ? 0.9 : 0.5).add(0.0, 0.3, 0.0);
                    this.hurt(pilot, target, this.minion.damage(), away);
                    Sounds.play(level, target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
                }
                if (this.t >= PUNCH_END) {
                    this.end();
                }
            }
            case CANNON -> {
                if (this.t == FIRE && target != null) {
                    this.shoot(level, pilot, target);
                }
                if (this.t >= CANNON_END) {
                    this.end();
                }
            }
            case LEAP -> {
                if (this.t == SPRING) {
                    this.spring(level, target);
                } else if (this.t > SPRING + 2 && this.minion.onGround() || this.t > SPRING + AIR_MOST) {
                    this.slam(level, pilot);
                    this.kind = SLAM;
                    this.t = 0;
                    this.minion.start(SLAM);
                }
            }
            case SLAM -> {
                if (this.t >= SLAM_END) {
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        int now = this.minion.tickCount;
        switch (this.kind) {
            case PUNCH -> this.punchReady = now + PUNCH_WAIT;
            case CANNON -> this.cannonReady = now + CANNON_WAIT;
            default -> this.leapReady = now + LEAP_WAIT;
        }
        this.kind = 0;
    }

    // The bolt: from the cannon's mouth straight at its target's middle, stopped by a wall.
    private void shoot(ServerLevel level, @Nullable ServerPlayer pilot, LivingEntity target) {
        Vec3 from = muzzle(this.minion);
        Vec3 to = target.getBoundingBox().getCenter();
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.minion));
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? wall.getLocation() : to;
        ParticleFx.line(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), from, end, 0.4);
        ParticleFx.cloud(level, ParticleFx.fade(GLOW, PowerRing.GREEN, 1.4F), end, 10, 0.3, 0.08);
        Sounds.play(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4F, 1.5F);
        if (!blocked) {
            Vec3 way = to.subtract(from).normalize();
            this.hurt(pilot, target, this.minion.damage() * 1.2, way.scale(0.7).add(0.0, 0.25, 0.0));
        }
    }

    // The cannon's mouth on its right forearm, held out at its target, in the world.
    public static Vec3 muzzle(MechMinion minion) {
        double yaw = Math.toRadians(minion.yBodyRot);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return minion.position().add(0.0, 2.75, 0.0).add(right.scale(1.05)).add(ahead.scale(2.0));
    }

    // Off the ground at its target, to come down on it: in the air a mob keeps 0.91 of its speed a tick, so over the
    // leap's 19 ticks or so it goes about 9 times what it set off with.
    private void spring(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 way = target == null ? this.minion.getLookAngle() : target.position().subtract(this.minion.position());
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        double far = flat.length();
        Vec3 push = far < 1.0E-3 ? Vec3.ZERO : flat.scale(Math.min(1.2, far / 9.0) / far);
        this.minion.setDeltaMovement(push.x, 0.75, push.z);
        this.minion.hasImpulse = true;
        Sounds.play(level, this.minion.position(), SoundEvents.IRON_GOLEM_STEP, 1.5F, 0.5F);
    }

    // Landed: both fists hammered into the ground, throwing everything round it up and away.
    private void slam(ServerLevel level, @Nullable ServerPlayer pilot) {
        Vec3 at = this.minion.position();
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND, 2.0F, 0.8F);
        Sounds.play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.4F);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at.add(0.0, 0.1, 0.0), 10, 0.35);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), at.add(0.0, 0.2, 0.0), 20, 0.5);
        if (pilot == null && !this.minion.wild()) {
            return;
        }
        LivingEntity target = this.minion.getTarget();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SLAM_RADIUS,
                2.0, SLAM_RADIUS), living -> living != this.minion && (pilot != null ? PowerRing.canHit(pilot, living)
                        : living == target || living instanceof Enemy && !(living instanceof MechMinion)))) {
            Vec3 to = living.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > SLAM_RADIUS + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            this.hurt(pilot, living, this.minion.damage() * 1.4, out.scale(0.8).add(0.0, 0.45, 0.0));
        }
    }

    // A hit as its pilot's own, or a wild one's own (which knocks back by itself): its own push is set after it.
    private void hurt(@Nullable ServerPlayer pilot, LivingEntity target, double damage, Vec3 push) {
        target.invulnerableTime = 0;
        DamageSources sources = this.minion.level().damageSources();
        if (!target.hurt(pilot != null ? sources.playerAttack(pilot) : sources.mobAttack(this.minion),
                (float) damage)) {
            return;
        }
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(push.scale(1.0 - resist));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private Vec3 flatWay(LivingEntity target) {
        Vec3 to = target.position().subtract(this.minion.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
    }
}
