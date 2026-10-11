package nl.tivek.multiversepowers.creature.brute;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.mob.MobRules;

// The brute's moves on what it hunts, each with a wait of its own: up close it raises both fists and SMASHes them
// down; further off it crouches and LEAPs at it, SLAMming the ground where it lands; or it ROARs and CHARGEs in a
// straight line, ramming all in its way, STUNNED a moment if it runs into a wall. Every game draws each move from its
// start (ZombieBrute.start) on the same timeline.
public final class BruteMoves extends Goal {
    public static final int SMASH = 1;
    public static final int LEAP = 2;
    public static final int SLAM = 3;
    public static final int ROAR = 4;
    public static final int CHARGE = 5;
    public static final int STUNNED = 6;
    public static final int KINDS = 6;
    // The smash: fists overhead, down at HITS, over at SMASH_END.
    public static final int HITS = 12;
    public static final int SMASH_END = 24;
    // The leap: crouched until SPRING, then in the air until it lands; the slam after lasts SLAM_END.
    public static final int SPRING = 10;
    public static final int SLAM_END = 18;
    // The roar lasts ROAR_END, the charge at most RUN, a stun STUN.
    public static final int ROAR_END = 16;
    public static final int RUN = 26;
    public static final int STUN = 40;
    private static final int AIR_MOST = 40;
    private static final double REACH = 2.2;
    private static final double SMASH_REACH = 3.0;
    private static final double LEAP_FROM = 5.0;
    private static final double LEAP_MOST = 13.0;
    private static final double CHARGE_FROM = 7.0;
    private static final double CHARGE_MOST = 18.0;
    private static final double RUN_SPEED = 0.6;
    private static final double SLAM_RADIUS = 4.0;
    private static final int SMASH_WAIT = 14;
    private static final int LEAP_WAIT = 120;
    private static final int CHARGE_WAIT = 180;

    private final ZombieBrute brute;
    private int kind;
    private int t;
    private int smashReady;
    private int leapReady = 40;
    private int chargeReady = 80;
    private int repath;
    private Vec3 rush = Vec3.ZERO;
    private final Set<LivingEntity> rammed = Collections.newSetFromMap(new IdentityHashMap<>());

    BruteMoves(ZombieBrute brute) {
        this.brute = brute;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.brute.getTarget();
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
    public void start() {
        this.brute.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0) {
            this.brute.start(0);
        }
        this.kind = 0;
        this.rammed.clear();
        this.brute.setAggressive(false);
        this.brute.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.brute.getTarget();
        // Charging it keeps the way it set off; stunned it looks nowhere.
        boolean set = this.kind == CHARGE || this.kind == STUNNED || this.kind == LEAP && this.t >= SPRING;
        if (target != null && !set) {
            this.brute.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        int now = this.brute.tickCount;
        double far = this.gap(target);
        boolean seen = this.brute.getSensing().hasLineOfSight(target);
        boolean standing = this.brute.onGround();
        if (far <= REACH && now >= this.smashReady) {
            this.begin(SMASH);
        } else if (far >= CHARGE_FROM && far <= CHARGE_MOST && seen && standing && now >= this.chargeReady) {
            this.begin(ROAR);
        } else if (far >= LEAP_FROM && far <= LEAP_MOST && seen && standing && now >= this.leapReady) {
            this.begin(LEAP);
        } else if (far > REACH * 0.8 && --this.repath <= 0) {
            this.repath = 6;
            this.brute.getNavigation().moveTo(target, 1.0);
        }
    }

    // From its body's side to its target's.
    private double gap(LivingEntity target) {
        return Math.sqrt(this.brute.distanceToSqr(target)) - (target.getBbWidth() + this.brute.getBbWidth()) * 0.5;
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.brute.getNavigation().stop();
        this.brute.start(kind);
        ServerLevel level = (ServerLevel) this.brute.level();
        switch (kind) {
            case SMASH -> this.brute.playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6F, 0.6F);
            case LEAP -> this.brute.playSound(SoundEvents.RAVAGER_STEP, 1.2F, 0.6F);
            case ROAR -> this.brute.playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.7F);
            case SLAM -> level.playSound(null, this.brute.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    this.brute.getSoundSource(), 1.0F, 0.6F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        ServerLevel level = (ServerLevel) this.brute.level();
        switch (this.kind) {
            case SMASH -> {
                if (this.t == HITS) {
                    this.smash(level, target);
                }
                if (this.t >= SMASH_END) {
                    this.end();
                }
            }
            case LEAP -> {
                if (this.t == SPRING) {
                    this.spring(target);
                } else if (this.t > SPRING + 2 && this.brute.onGround() || this.t > SPRING + AIR_MOST) {
                    this.begin(SLAM);
                    this.slam(level);
                }
            }
            case SLAM -> {
                if (this.t >= SLAM_END) {
                    this.end();
                }
            }
            case ROAR -> {
                if (this.t >= ROAR_END) {
                    this.set(target);
                    this.begin(CHARGE);
                }
            }
            case CHARGE -> this.charge(level);
            case STUNNED -> {
                if (this.t >= STUN) {
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        int now = this.brute.tickCount;
        switch (this.kind) {
            case SMASH -> this.smashReady = now + SMASH_WAIT;
            case SLAM, LEAP -> this.leapReady = now + LEAP_WAIT;
            default -> this.chargeReady = now + CHARGE_WAIT;
        }
        this.smashReady = Math.max(this.smashReady, now + SMASH_WAIT / 2);
        this.rammed.clear();
        this.kind = 0;
        this.brute.start(0);
    }

    // Both fists come down in front of it: what stands there is struck down and flung on along the way it faces.
    private void smash(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 ahead = this.ahead();
        Vec3 at = this.brute.position().add(ahead.scale(1.6));
        this.dust(level, at, 14, 0.8);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ANVIL_LAND, this.brute.getSoundSource(), 0.6F,
                0.5F);
        for (LivingEntity living : this.foes(level, new AABB(at, at).inflate(SMASH_REACH * 0.6, 2.0, SMASH_REACH
                * 0.6))) {
            Vec3 to = living.position().subtract(this.brute.position());
            if (to.horizontalDistance() > SMASH_REACH + living.getBbWidth() * 0.5 || to.dot(ahead) < -0.2) {
                continue;
            }
            this.hurt(living, this.damage(), ahead.scale(0.9).add(0.0, 0.35, 0.0));
        }
        if (target != null && target.isAlive() && this.gap(target) <= REACH + 0.6) {
            this.hurt(target, this.damage(), ahead.scale(0.9).add(0.0, 0.35, 0.0));
        }
    }

    // Up at its target in an arc that comes down where it stands.
    private void spring(@Nullable LivingEntity target) {
        Vec3 to = target == null ? this.ahead().scale(6.0) : target.position().subtract(this.brute.position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        double up = 0.85;
        double airtime = 2.0 * up / 0.08;
        // The air slows it 9% a tick: the speed whose slowing run still ends at its target.
        double speed = Math.min(1.4, flat * 0.09 / (1.0 - Math.pow(0.91, airtime)));
        Vec3 way = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
        this.brute.setDeltaMovement(way.x * speed, up, way.z * speed);
        this.brute.hasImpulse = true;
        this.brute.playSound(SoundEvents.RAVAGER_ATTACK, 1.2F, 0.6F);
    }

    // Landing it pounds the ground: everything round it is struck, the nearest hardest, and thrown outward.
    private void slam(ServerLevel level) {
        Vec3 at = this.brute.position();
        this.dust(level, at, 40, SLAM_RADIUS * 0.6);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (LivingEntity living : this.foes(level, new AABB(at, at).inflate(SLAM_RADIUS, 2.0, SLAM_RADIUS))) {
            Vec3 to = living.position().subtract(at);
            double flat = to.horizontalDistance();
            if (flat > SLAM_RADIUS + living.getBbWidth() * 0.5) {
                continue;
            }
            double near = 1.0 - 0.5 * Mth.clamp(flat / SLAM_RADIUS, 0.0, 1.0);
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            this.hurt(living, this.damage() * 1.25 * near, out.scale(1.0 * near).add(0.0, 0.55, 0.0));
        }
    }

    private void set(@Nullable LivingEntity target) {
        Vec3 way = target == null ? this.ahead() : target.position().subtract(this.brute.position());
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        this.rush = flat.lengthSqr() < 1.0E-6 ? this.ahead() : flat.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-this.rush.x, this.rush.z));
        this.brute.setYRot(yaw);
        this.brute.yBodyRot = yaw;
        this.brute.yHeadRot = yaw;
    }

    // Head down it runs the way it set off, ramming everything in its way once, until it has run its course or runs
    // into a wall, which stuns it.
    private void charge(ServerLevel level) {
        if (this.t > 2 && this.brute.horizontalCollision) {
            Vec3 at = this.brute.position().add(this.rush.scale(0.9)).add(0.0, 1.4, 0.0);
            level.playSound(null, BlockPos.containing(at), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    this.brute.getSoundSource(), 1.4F, 0.6F);
            ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 10, 0.5, 0.05);
            this.begin(STUNNED);
            return;
        }
        if (this.t >= RUN) {
            this.end();
            return;
        }
        this.brute.setDeltaMovement(this.rush.x * RUN_SPEED, this.brute.getDeltaMovement().y, this.rush.z
                * RUN_SPEED);
        this.brute.hasImpulse = true;
        if (this.t % 2 == 0) {
            this.dust(level, this.brute.position(), 3, 0.4);
        }
        Vec3 side = new Vec3(-this.rush.z, 0.0, this.rush.x);
        for (LivingEntity living : this.foes(level, this.brute.getBoundingBox().inflate(0.6, 0.2, 0.6)
                .move(this.rush.scale(0.6)))) {
            if (!this.rammed.add(living)) {
                continue;
            }
            double aside = Math.signum(living.position().subtract(this.brute.position()).dot(side));
            this.hurt(living, this.damage() * 1.4, this.rush.scale(1.6).add(side.scale(0.5 * aside))
                    .add(0.0, 0.6, 0.0));
            level.playSound(null, living.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    this.brute.getSoundSource(), 1.4F, 0.6F);
        }
    }

    // Who its blows land on: what it hunts, players, and creatures that are no monster, as the Mobs page sets them.
    private List<LivingEntity> foes(ServerLevel level, AABB box) {
        LivingEntity target = this.brute.getTarget();
        return level.getEntitiesOfClass(LivingEntity.class, box, living -> {
            if (living == this.brute || !living.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)) {
                return false;
            }
            if (living == target) {
                return true;
            }
            Standing set = MobRules.set(this.brute.getType(), living.getType());
            if (set != null) {
                return set == Standing.HOSTILE;
            }
            return living instanceof Player || !(living instanceof Enemy);
        });
    }

    private void hurt(LivingEntity target, double damage, Vec3 push) {
        if (!target.hurt(this.brute.damageSources().mobAttack(this.brute), (float) damage)) {
            return;
        }
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(push.scale(1.0 - resist));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private double damage() {
        return this.brute.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private Vec3 ahead() {
        double yaw = Math.toRadians(this.brute.yBodyRot);
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    // The ground's own dust thrown up round `at`.
    private void dust(ServerLevel level, Vec3 at, int count, double spread) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.2, at.z);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            return;
        }
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, count,
                spread, 0.1, spread, 0.15);
    }
}
