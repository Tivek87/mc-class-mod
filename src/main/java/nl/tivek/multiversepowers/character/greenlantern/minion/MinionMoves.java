package nl.tivek.multiversepowers.character.greenlantern.minion;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// A sentinel's six moves on its target, each with a wait of its own: a jab and a hook up close, a spin with its arms
// out when it is beset, a bolt of light from the cannon on its right forearm from further off, a shoulder charge that
// rams everything in its way, a leap that slams down on it from 5 to 11 blocks away, and a fist driven into the ground
// that sends spikes of hard light breaking out of it towards its target. Between them it walks at its target. Every
// hit lands as its pilot's own, on anyone they may hit.
public final class MinionMoves extends Goal {
    public static final int PUNCH = 1;
    public static final int CANNON = 2;
    public static final int LEAP = 3;
    public static final int SLAM = 4;
    public static final int SPIN = 5;
    public static final int CHARGE = 6;
    public static final int SPIKES = 7;
    static final int KINDS = 7;
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
    // The spin: arms out, it winds up till SPIN_WIND and whirls round twice, striking all round it at each of
    // SPIN_HITS; over at SPIN_END.
    public static final int SPIN_WIND = 6;
    public static final int[] SPIN_HITS = { 10, 15 };
    public static final int SPIN_END = 24;
    // The charge: shoulder down, it rushes from RUSH for RUSH_TICKS, ramming everything in its way; over at
    // CHARGE_END.
    public static final int RUSH = 7;
    public static final int RUSH_TICKS = 12;
    public static final int CHARGE_END = 30;
    // The spikes: its fist comes down into the ground at PLANT and a spike of hard light breaks out of the ground every
    // SPIKE_EVERY ticks, SPIKE_COUNT of them SPIKE_STEP apart along the way it faces, the first SPIKE_FROM ahead of
    // it; each stands SPIKE_STANDS ticks and sinks back before SPIKES_END.
    public static final int PLANT = 10;
    public static final int SPIKE_EVERY = 2;
    public static final int SPIKE_COUNT = 7;
    public static final double SPIKE_STEP = 1.6;
    public static final double SPIKE_FROM = 1.8;
    public static final int SPIKE_STANDS = 10;
    public static final int SPIKES_END = 38;
    private static final int AIR_MOST = 40;
    private static final double REACH = 2.0;
    private static final double SHOT_FROM = 3.5;
    private static final double SHOT_MOST = 16.0;
    private static final double LEAP_FROM = 5.0;
    private static final double LEAP_MOST = 11.0;
    private static final double SLAM_RADIUS = 3.2;
    private static final double SPIN_RADIUS = 3.4;
    private static final double CHARGE_FROM = 4.0;
    private static final double CHARGE_MOST = 10.0;
    private static final double RUSH_SPEED = 0.72;
    private static final double SPIKES_FROM = 4.5;
    private static final double SPIKES_MOST = SPIKE_FROM + SPIKE_STEP * (SPIKE_COUNT - 1);
    private static final int PUNCH_WAIT = 8;
    private static final int CANNON_WAIT = 40;
    private static final int LEAP_WAIT = 120;
    private static final int SPIN_WAIT = 70;
    private static final int CHARGE_WAIT = 100;
    private static final int SPIKES_WAIT = 90;
    private static final int GLOW = 0xE4FFEA;

    private final MechMinion minion;
    private int kind;
    private int t;
    private int punchReady;
    private int cannonReady;
    private int leapReady;
    private int spinReady;
    private int chargeReady;
    private int spikesReady;
    private int repath;
    private Vec3 rush = Vec3.ZERO;
    private final Set<LivingEntity> rammed = Collections.newSetFromMap(new IdentityHashMap<>());

    MinionMoves(MechMinion minion) {
        this.minion = minion;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.minion.getTarget();
        return target != null && target.isAlive() && this.minion.risen();
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
        // Charging or driving spikes along the ground it keeps the way it set off.
        boolean set = this.kind == CHARGE && this.t >= RUSH - 1 || this.kind == SPIKES && this.t >= PLANT - 2;
        if (target != null && !set) {
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
        boolean standing = this.minion.onGround();
        if (far <= SPIN_RADIUS && now >= this.spinReady
                && (this.beset(pilot) || far <= REACH && now < this.punchReady)) {
            this.begin(SPIN);
        } else if (far <= REACH && now >= this.punchReady) {
            this.begin(PUNCH);
        } else if (far >= CHARGE_FROM && far <= CHARGE_MOST && seen && now >= this.chargeReady && standing) {
            this.begin(CHARGE);
        } else if (far >= LEAP_FROM && far <= LEAP_MOST && seen && now >= this.leapReady && standing) {
            this.begin(LEAP);
        } else if (far >= SPIKES_FROM && far <= SPIKES_MOST && seen && now >= this.spikesReady && standing) {
            this.begin(SPIKES);
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
            case SPIN -> Sounds.play(level, at, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
            case CHARGE -> Sounds.play(level, at, SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 0.6F);
            case SPIKES -> Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.8F);
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
            case SPIN -> {
                for (int hit : SPIN_HITS) {
                    if (this.t == hit) {
                        this.whirl(level, pilot);
                    }
                }
                if (this.t >= SPIN_END) {
                    this.end();
                }
            }
            case CHARGE -> {
                this.charge(level, pilot, target);
                if (this.t >= CHARGE_END) {
                    this.end();
                }
            }
            case SPIKES -> {
                int k = (this.t - PLANT) / SPIKE_EVERY;
                if (this.t >= PLANT && (this.t - PLANT) % SPIKE_EVERY == 0 && k < SPIKE_COUNT) {
                    this.spike(level, pilot, k);
                }
                if (this.t >= SPIKES_END) {
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
            case SPIN -> this.spinReady = now + SPIN_WAIT;
            case CHARGE -> this.chargeReady = now + CHARGE_WAIT;
            case SPIKES -> this.spikesReady = now + SPIKES_WAIT;
            default -> this.leapReady = now + LEAP_WAIT;
        }
        this.rammed.clear();
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
        for (LivingEntity living : this.foes(level, pilot, new AABB(at, at).inflate(SLAM_RADIUS, 2.0, SLAM_RADIUS))) {
            Vec3 to = living.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > SLAM_RADIUS + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            this.hurt(pilot, living, this.minion.damage() * 1.4, out.scale(0.8).add(0.0, 0.45, 0.0));
        }
    }

    // Two or more it may hit within reach of its spin.
    private boolean beset(@Nullable ServerPlayer pilot) {
        Vec3 at = this.minion.position();
        return this.foes((ServerLevel) this.minion.level(), pilot, new AABB(at, at).inflate(SPIN_RADIUS, 2.0,
                SPIN_RADIUS)).size() >= 2;
    }

    // Everything in the box this sentinel may hit: anyone its pilot may, or for a wild one its target and monsters.
    private List<LivingEntity> foes(ServerLevel level, @Nullable ServerPlayer pilot, AABB box) {
        LivingEntity target = this.minion.getTarget();
        return level.getEntitiesOfClass(LivingEntity.class, box, living -> living != this.minion && living.isAlive()
                && (pilot != null ? PowerRing.canHit(pilot, living)
                        : living == target || living instanceof Enemy && !(living instanceof MechMinion)));
    }

    // Arms out it whirls: everything round it is struck and flung outward.
    private void whirl(ServerLevel level, @Nullable ServerPlayer pilot) {
        Vec3 at = this.minion.position();
        Sounds.play(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.7F);
        Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.2F);
        ParticleFx.ring(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), at.add(0.0, 2.2, 0.0), SPIN_RADIUS * 0.8, 18,
                0);
        if (pilot == null && !this.minion.wild()) {
            return;
        }
        for (LivingEntity living : this.foes(level, pilot, new AABB(at, at).inflate(SPIN_RADIUS, 3.0,
                SPIN_RADIUS))) {
            Vec3 to = living.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > SPIN_RADIUS + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            this.hurt(pilot, living, this.minion.damage() * 0.8, out.scale(0.8).add(0.0, 0.35, 0.0));
            ParticleFx.send(level, ParticleTypes.SWEEP_ATTACK, living.getX(), living.getY() + living.getBbHeight()
                    * 0.5, living.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // Shoulder down it rushes the way it set off, ramming everything in its way once, until it has run its course or
    // runs into a wall.
    private void charge(ServerLevel level, @Nullable ServerPlayer pilot, @Nullable LivingEntity target) {
        if (this.t == RUSH) {
            Vec3 way = target == null ? this.minion.getLookAngle() : target.position().subtract(this.minion.position());
            Vec3 flat = new Vec3(way.x, 0.0, way.z);
            this.rush = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-this.rush.x, this.rush.z));
            this.minion.setYRot(yaw);
            this.minion.yBodyRot = yaw;
            this.minion.yHeadRot = yaw;
            Sounds.play(level, this.minion.position(), SoundEvents.RAVAGER_ROAR, 0.6F, 1.6F);
        }
        if (this.t < RUSH || this.t >= RUSH + RUSH_TICKS || this.rush == Vec3.ZERO) {
            return;
        }
        if (this.t > RUSH + 1 && this.minion.horizontalCollision) {
            Sounds.play(level, this.minion.position(), SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 0.7F);
            ParticleFx.cloud(level, ParticleTypes.CLOUD, this.minion.position().add(this.rush).add(0.0, 1.5, 0.0), 8,
                    0.4, 0.05);
            this.rush = Vec3.ZERO;
            return;
        }
        this.minion.setDeltaMovement(this.rush.x * RUSH_SPEED, this.minion.getDeltaMovement().y,
                this.rush.z * RUSH_SPEED);
        this.minion.hasImpulse = true;
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), this.minion.position().add(0.0, 1.6, 0.0),
                3, 0.3, 0.02);
        if (pilot == null && !this.minion.wild()) {
            return;
        }
        Vec3 side = new Vec3(-this.rush.z, 0.0, this.rush.x);
        for (LivingEntity living : this.foes(level, pilot, this.minion.getBoundingBox().inflate(0.9, 0.2, 0.9)
                .move(this.rush.scale(0.6)))) {
            if (!this.rammed.add(living)) {
                continue;
            }
            double aside = Math.signum(living.position().subtract(this.minion.position()).dot(side));
            this.hurt(pilot, living, this.minion.damage() * 1.6, this.rush.scale(1.5).add(side.scale(0.4 * aside))
                    .add(0.0, 0.55, 0.0));
            Sounds.play(level, living.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
            Sounds.play(level, living.position(), SoundEvents.ANVIL_LAND, 0.5F, 1.6F);
        }
    }

    // A spike of hard light breaks out of the ground k steps along the way it faces, throwing up what stands on it.
    private void spike(ServerLevel level, @Nullable ServerPlayer pilot, int k) {
        Vec3 at = spikeAt(level, this.minion.position(), this.minion.yBodyRot, k);
        if (at == null) {
            return;
        }
        Sounds.play(level, at, SoundEvents.POINTED_DRIPSTONE_LAND, 1.2F, 0.6F);
        Sounds.play(level, at, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.2F, 0.8F + 0.05F * k);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), at.add(0.0, 0.4, 0.0), 8, 0.3, 0.05);
        if (pilot == null && !this.minion.wild()) {
            return;
        }
        double yaw = Math.toRadians(this.minion.yBodyRot);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        for (LivingEntity living : this.foes(level, pilot, new AABB(at.x - 1.0, at.y - 0.5, at.z - 1.0, at.x + 1.0,
                at.y + 2.4, at.z + 1.0))) {
            if (this.rammed.add(living)) {
                this.hurt(pilot, living, this.minion.damage() * 1.3, ahead.scale(0.25).add(0.0, 0.85, 0.0));
            }
        }
    }

    // Where spike k breaks out of the ground, for a sentinel standing at `from` facing `bodyYaw`: on the top of the
    // ground there, within a block or two up or down, or nowhere.
    @Nullable
    public static Vec3 spikeAt(Level level, Vec3 from, float bodyYaw, int k) {
        double yaw = Math.toRadians(bodyYaw);
        Vec3 at = from.add(-Math.sin(yaw) * (SPIKE_FROM + SPIKE_STEP * k), 0.0,
                Math.cos(yaw) * (SPIKE_FROM + SPIKE_STEP * k));
        BlockPos top = BlockPos.containing(at.x, from.y + 1.5, at.z);
        for (int down = 1; down <= 5; down++) {
            BlockPos below = top.below(down);
            if (!level.isLoaded(below)) {
                return null;
            }
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    && level.getBlockState(below.above()).getCollisionShape(level, below.above()).isEmpty()) {
                return new Vec3(at.x, below.getY() + 1.0, at.z);
            }
        }
        return null;
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
