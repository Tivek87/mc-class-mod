package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.LENGTH;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.RAIL_LIFT;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.TOP_SPEED;

abstract class ExpressRoute implements Effect {
    private static final double MAX_TURN = 0.075;
    private static final double RUN_OUT = 12.0;
    private static final double FIRST_RUN = (LENGTH + 10.0) * SCALE;
    private static final int GIVE_UP_TICKS = 70;
    private static final double HALF_WIDTH = 1.7;
    private static final double NOSE_DEPTH = 3.0;
    private static final double REACH_UP = 5.0;
    private static final double CLIMB = 0.42;
    private static final double DROP = 0.55;
    private static final double LOOK_UP = 1.3;
    private static final double LOOK_DOWN = 14.0;
    private static final double AHEAD_BIAS = 10.0;

    final ServerPlayer owner;
    final CharacterAbility ability;
    Vec3 head;
    Vec3 way;
    double speed;
    double odometer;
    double turning;
    int rams;
    private double lastFound;
    private final Set<Integer> hit = new HashSet<>();
    private final Set<Integer> skipped = new HashSet<>();
    @Nullable
    private LivingEntity target;
    private int chasing;

    ExpressRoute(ServerPlayer owner, CharacterAbility ability, Vec3 head, Vec3 way) {
        this.owner = owner;
        this.ability = ability;
        this.head = head;
        this.way = way;
    }

    Vec3 right() {
        return new Vec3(-this.way.z, 0.0, this.way.x);
    }

    // One tick along the ground; false once the way on leaves the loaded world.
    boolean roll(ServerLevel level, boolean steer) {
        if (steer) {
            this.steer(level);
        }
        double x = this.head.x + this.way.x * this.speed;
        double z = this.head.z + this.way.z * this.speed;
        BlockPos next = BlockPos.containing(x, this.head.y, z);
        if (!level.isLoaded(next) || !level.getWorldBorder().isWithinBounds(next)) {
            return false;
        }
        double rail = ground(level, x, z, this.head.y - RAIL_LIFT) + RAIL_LIFT;
        double y = this.head.y + Mth.clamp(rail - this.head.y, -(DROP * this.speed + 0.02), CLIMB * this.speed + 0.02);
        Vec3 was = this.head;
        this.head = new Vec3(x, y, z);
        this.odometer += this.head.distanceTo(was);
        return true;
    }

    static double ground(ServerLevel level, double x, double z, double from) {
        Vec3 top = new Vec3(x, from + LOOK_UP, z);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(top, top.subtract(0.0, LOOK_UP + LOOK_DOWN, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            return from - LOOK_DOWN;
        }
        // Started inside the ground: climb towards the top of it, a step at a time.
        return hit.isInside() ? from + LOOK_UP : hit.getLocation().y;
    }

    boolean runDone() {
        return this.rams >= this.ability.intValue("mostRams") || this.odometer >= this.ability.value("runBlocks")
                || this.target == null && this.odometer - this.lastFound >= (this.rams == 0 ? FIRST_RUN : RUN_OUT);
    }

    private void steer(ServerLevel level) {
        if (this.target != null && (!this.fair(level, this.target) || this.hit.contains(this.target.getId()))) {
            this.target = null;
        }
        if (this.target != null && ++this.chasing > GIVE_UP_TICKS) {
            this.skipped.add(this.target.getId());
            this.target = null;
        }
        if (this.target == null) {
            this.target = this.pick(level);
            this.chasing = 0;
        }
        if (this.target == null) {
            this.turning *= 0.8;
            return;
        }
        Vec3 to = this.target.position().subtract(this.head);
        double length = Math.sqrt(to.x * to.x + to.z * to.z);
        if (length < 1.0E-3) {
            return;
        }
        double wantX = to.x / length;
        double wantZ = to.z / length;
        double angle = Math.atan2(this.way.z * wantX - this.way.x * wantZ, this.way.x * wantX + this.way.z * wantZ);
        double turn = Mth.clamp(angle, -MAX_TURN, MAX_TURN);
        this.way = Vectors.spin(this.way, Vectors.UP, turn).normalize();
        this.turning = this.turning * 0.8 + turn * 0.2;
    }

    @Nullable
    private LivingEntity pick(ServerLevel level) {
        double reach = this.ability.value("rangeBlocks");
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.head, this.head).inflate(reach, 10.0, reach), entity -> GiantHands.fair(this.owner,
                        entity) && !this.hit.contains(entity.getId()) && !this.skipped.contains(entity.getId()))) {
            Vec3 to = living.position().subtract(this.head);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > reach) {
                continue;
            }
            double ahead = flat < 1.0E-3 ? 1.0 : (to.x * this.way.x + to.z * this.way.z) / flat;
            double score = flat + (1.0 - ahead) * AHEAD_BIAS;
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }

    private boolean fair(ServerLevel level, LivingEntity living) {
        return living.isAlive() && living.level() == level && GiantHands.fair(this.owner, living);
    }

    void ram(ServerLevel level) {
        Vec3 right = this.right();
        double depth = NOSE_DEPTH * SCALE + this.speed;
        double half = HALF_WIDTH * SCALE;
        AABB area = new AABB(this.head, this.head.subtract(this.way.scale(depth))).inflate(half + 1.0, 1.5, half + 1.0)
                .expandTowards(0.0, REACH_UP * SCALE, 0.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> GiantHands.fair(this.owner, entity) && !this.hit.contains(entity.getId()))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(this.head);
            double along = to.x * this.way.x + to.z * this.way.z;
            double across = to.x * right.x + to.z * right.z;
            double width = living.getBbWidth() * 0.5;
            if (along > 0.4 + width || along < -depth - width || Math.abs(across) > half + width || to.y < -1.5
                    || to.y > REACH_UP * SCALE + living.getBbHeight() * 0.5) {
                continue;
            }
            this.smack(level, living, across >= 0.0 ? right : right.scale(-1.0));
        }
    }

    private void smack(ServerLevel level, LivingEntity living, Vec3 aside) {
        this.hit.add(living.getId());
        this.rams++;
        this.lastFound = this.odometer;
        if (living == this.target) {
            this.target = null;
        }
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(this.owner), this.ability.getDamage());
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double strength = this.ability.value("knockback") * (0.55 + 0.45 * Math.min(1.0, this.speed / TOP_SPEED));
        Vec3 push = this.way.scale(0.85).add(aside.scale(0.8)).normalize().scale(strength);
        living.setDeltaMovement(new Vec3(push.x, 0.5 + 0.18 * strength, push.z).scale(1.0 - resist));
        living.hasImpulse = true;
        living.hurtMarked = true;
        Vec3 at = living.getBoundingBox().getCenter();
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), at, 22, 0.35);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 14, 0.4, 0.4);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 6, 0.3, 0.08);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND, 1.2F, 0.8F);
        Sounds.play(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.6F);
        Sounds.play(level, at, SoundEvents.ANVIL_LAND, 0.5F, 0.55F);
        Sounds.play(level, at, SoundEvents.IRON_GOLEM_DAMAGE, 0.9F, 0.7F);
    }
}
