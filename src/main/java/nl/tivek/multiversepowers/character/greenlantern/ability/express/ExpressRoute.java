package nl.tivek.multiversepowers.character.greenlantern.ability.express;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.LENGTH;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.RAIL_LIFT;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.TOP_SPEED;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.TRAIN_LENGTH;

abstract class ExpressRoute implements Effect {
    // The sharpest turn a tick (radians); in a sharp curve the train slows to TIGHT of its pace, so it bends tighter.
    private static final double MAX_TURN = 0.11;
    private static final double TIGHT = 0.55;
    // Longer than this (ticks) off to the side, a target is circled rather than caught: the train lets it be a while.
    private static final int ORBIT_TICKS = 24;
    private static final double LET_BE = 24.0;
    private static final double LEAD_TICKS = 16.0;
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
    private static final double KEEP = (TRAIN_LENGTH + 4.0) * SCALE;

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
    // Targets let be until the train has run this far (odometer), after it found it could only circle them.
    private final Map<Integer, Double> resting = new HashMap<>();
    @Nullable
    private LivingEntity target;
    private int chasing;
    private int wide;
    private double curve = 1.0;
    // Where the nose has been, by how far it had come: the rest of the train runs along it.
    private final ArrayDeque<double[]> trail = new ArrayDeque<>();
    boolean newTarget;

    ExpressRoute(ServerPlayer owner, CharacterAbility ability, Vec3 head, Vec3 way) {
        this.owner = owner;
        this.ability = ability;
        this.head = head;
        this.way = way;
        this.trail.add(new double[] { head.x, head.y, head.z, this.odometer });
    }

    // A point on the rails so far back from the nose; before the train has run that far, straight back from its start.
    Vec3 along(double back) {
        double wanted = this.odometer - back;
        double[] before = null;
        for (double[] point : this.trail) {
            if (point[3] >= wanted) {
                if (before == null) {
                    return new Vec3(point[0], point[1], point[2]).subtract(this.way.scale(point[3] - wanted));
                }
                double t = (wanted - before[3]) / Math.max(1.0E-9, point[3] - before[3]);
                return new Vec3(Mth.lerp(t, before[0], point[0]), Mth.lerp(t, before[1], point[1]),
                        Mth.lerp(t, before[2], point[2]));
            }
            before = point;
        }
        return this.head;
    }

    Vec3 right() {
        return new Vec3(-this.way.z, 0.0, this.way.x);
    }

    // One tick along the ground; false once the way on leaves the loaded world.
    boolean roll(ServerLevel level, boolean steer) {
        if (steer) {
            this.steer(level);
        } else {
            this.curve += (1.0 - this.curve) * 0.15;
        }
        double pace = this.speed * this.curve;
        double x = this.head.x + this.way.x * pace;
        double z = this.head.z + this.way.z * pace;
        BlockPos next = BlockPos.containing(x, this.head.y, z);
        if (!level.isLoaded(next) || !level.getWorldBorder().isWithinBounds(next)) {
            return false;
        }
        double rail = ground(level, x, z, this.head.y - RAIL_LIFT) + RAIL_LIFT;
        double y = this.head.y + Mth.clamp(rail - this.head.y, -(DROP * pace + 0.02), CLIMB * pace + 0.02);
        Vec3 was = this.head;
        this.head = new Vec3(x, y, z);
        this.odometer += this.head.distanceTo(was);
        this.trail.add(new double[] { this.head.x, this.head.y, this.head.z, this.odometer });
        while (this.trail.size() > 2 && this.trail.peekFirst()[3] < this.odometer - KEEP) {
            this.trail.removeFirst();
        }
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
        // Circled too long, or inside the circle the train turns in (it could only go round it): let it be a while.
        if (this.target != null && (this.wide > ORBIT_TICKS || !this.reachable(this.aim(this.target)))) {
            this.resting.put(this.target.getId(), this.odometer + LET_BE);
            this.target = null;
        }
        this.resting.values().removeIf(until -> until <= this.odometer);
        if (this.target == null) {
            this.target = this.pick(level);
            this.chasing = 0;
            this.wide = 0;
            this.newTarget = this.target != null;
        }
        if (this.target == null) {
            this.turning *= 0.8;
            this.curve += (1.0 - this.curve) * 0.15;
            return;
        }
        Vec3 to = this.aim(this.target).subtract(this.head);
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
        this.wide = Math.abs(angle) > 1.2 ? this.wide + 1 : 0;
        // Slows into a sharp curve and picks up again out of it.
        double want = 1.0 - (1.0 - TIGHT) * Mth.clamp((Math.abs(angle) - 0.35) / 0.9, 0.0, 1.0);
        this.curve += (want - this.curve) * (want < this.curve ? 0.3 : 0.12);
    }

    // Where to steer for: where the target will be by the time the train gets there, as far as it can tell.
    private Vec3 aim(LivingEntity living) {
        Vec3 at = living.position();
        double far = Math.sqrt(at.distanceToSqr(this.head.x, at.y, this.head.z));
        double ticks = Math.min(LEAD_TICKS, far / Math.max(0.2, this.speed));
        Vec3 moving = living.getDeltaMovement();
        return at.add(moving.x * ticks, 0.0, moving.z * ticks);
    }

    // Whether the train can reach a point at all: not inside either circle it turns in at its tightest.
    private boolean reachable(Vec3 point) {
        Vec3 to = point.subtract(this.head);
        double ahead = to.x * this.way.x + to.z * this.way.z;
        double aside = Math.abs(to.x * -this.way.z + to.z * this.way.x);
        double radius = Math.max(0.2, this.speed * TIGHT) / MAX_TURN;
        return ahead * ahead + aside * aside >= 2.0 * aside * radius * 0.92;
    }

    @Nullable
    private LivingEntity pick(ServerLevel level) {
        double reach = this.ability.value("rangeBlocks");
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.head, this.head).inflate(reach, 10.0, reach), entity -> GiantHands.fair(this.owner,
                        entity) && !this.hit.contains(entity.getId()) && !this.skipped.contains(entity.getId())
                        && !this.resting.containsKey(entity.getId()))) {
            Vec3 to = living.position().subtract(this.head);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > reach || !this.reachable(this.aim(living))) {
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
        Sounds.play(level, at, ExpressNoise.MACE_SMASH_GROUND, 1.2F, 0.8F);
        Sounds.play(level, at, ExpressNoise.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.6F);
        Sounds.play(level, at, ExpressNoise.ANVIL_LAND, 0.5F, 0.55F);
        Sounds.play(level, at, ExpressNoise.IRON_GOLEM_DAMAGE, 0.9F, 0.7F);
    }
}
