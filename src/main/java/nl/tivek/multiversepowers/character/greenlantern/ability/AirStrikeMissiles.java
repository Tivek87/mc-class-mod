package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.BIG_BLAST;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.BIG_MISSILE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.IGNITE_EARLIEST;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.IGNITE_LATEST;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.JET_MISSILE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.MISSILE_NOSE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.MISSILE_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.SMALL_BLAST;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.SMALL_IGNITES;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.SMALL_MISSILE_SCALE;

/**
 * The missiles of the {@link AirStrike}: the big ones that drop out of the plane's hatch, and the plane's two jets with
 * the small ones they fire from under their wings.
 */
abstract class AirStrikeMissiles extends AirStrikeGuns {
    // The big missiles: how far their blast reaches, how far they look for a creature to home in on, and how long one
    // flies at most before it bursts by itself.
    private static final double MISSILE_BLAST = 3.2;
    private static final double MISSILE_REACH = 150.0;
    private static final int MISSILE_LIFE = 160;
    // The small missiles of the jets: how far their blast reaches, how hard it throws, and how far a jet looks for a
    // creature to fire at.
    private static final double SMALL_BLAST_REACH = 2.0;
    private static final double SMALL_KNOCKBACK = 0.5;
    private static final double JET_REACH = 72.0;
    // How far below a missile's blast the ground may lie for it to blow a small crater there, and how many of that
    // crater's blocks it hurls away.
    private static final double MISSILE_GROUND = 2.5;
    private static final int MISSILE_DEBRIS = 6;
    // Where a missile with nothing to find strikes: this far ahead of the spot under the plane, and at most this far to
    // the side of its way.
    private static final double AHEAD_NEAR = 14.0;
    private static final double AHEAD_FAR = 30.0;
    private static final double AHEAD_WIDE = 10.0;

    private final List<Missile> missiles = new ArrayList<>();
    // Which pylon of each jet fires next (0 the left one, 1 the right one).
    private final int[] jetPylons = new int[PlanePath.JETS];

    AirStrikeMissiles(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        super(owner, ability, path);
    }

    // ---- Missiles ----

    /**
     * One missile on its way. A big one drops out of the plane's hatch and falls a way with its motor dead, its nose
     * dipping into the way it falls; at a moment of its own its motor bursts into life and it homes in on the creature
     * out to hurt him nearest to it, or with none strikes the ground along the plane's way. A small one of a jet drops
     * off its pylon and fires its motor almost at once. Either bursts where its nose strikes: a creature, a block, or
     * wherever it is once it has flown long enough.
     */
    private final class Missile {
        private final int id = PowerRing.newId();
        private final boolean small;
        private final int variant;
        private final int fired;
        // The tick after it dropped that its motor fires.
        private final int ignites;
        // Where its middle is; how it moves while it falls; the way its nose points and its up; and how fast it flies
        // once its motor burns.
        private Vec3 at;
        private Vec3 falling;
        private Vec3 way;
        private Vec3 up;
        private double speed;
        @Nullable
        private LivingEntity target;
        // Where it goes with no creature to home in on, and where the creature it homed in on was last.
        @Nullable
        private Vec3 aim;
        @Nullable
        private Vec3 lastGoal;
        private boolean lit;
        @Nullable
        private LivingEntity struck;
        // Where the tip of its nose struck, once it has.
        @Nullable
        private Vec3 tip;

        /** @param state where it is, how it moves, its nose and its up as it is let go (see PlanePath.fall) */
        Missile(Vec3[] state, boolean small, int variant, int ignites, @Nullable LivingEntity target) {
            this.at = state[0];
            this.falling = state[1];
            this.way = state[2].normalize();
            this.up = state[3];
            this.small = small;
            this.variant = variant;
            this.ignites = ignites;
            this.target = target;
            this.fired = AirStrikeMissiles.this.age;
        }

        /** How far the tip of its nose is ahead of its middle, in blocks. */
        private double nose() {
            return MISSILE_NOSE * (this.small ? SMALL_MISSILE_SCALE : MISSILE_SCALE);
        }

        /**
         * Moves on; true once it has struck. Every client hears where it is every tick; on the tick it strikes, where
         * it was as its nose struck, so it is seen to get there before it bursts.
         */
        boolean step(ServerLevel level) {
            int since = AirStrikeMissiles.this.age - this.fired;
            Vec3 tipWas = this.at.add(this.way.scale(this.nose()));
            Vec3 next;
            if (since < this.ignites) {
                // Its motor still dead: it falls, and its nose dips slowly into the way it falls, as every client draws
                // it leaving the plane.
                Vec3[] fell = PlanePath.fall(new Vec3[] { this.at, this.falling, this.way, this.up }, this.small);
                next = fell[0];
                this.falling = fell[1];
                this.way = fell[2];
                this.up = fell[3];
            } else {
                if (!this.lit) {
                    this.ignite(level);
                }
                this.steer(level, since - this.ignites);
                next = this.at.add(this.way.scale(this.speed));
            }
            Vec3 hit = this.strikes(level, tipWas, next);
            boolean done = hit != null || since >= MISSILE_LIFE || !level.isLoaded(BlockPos.containing(next));
            if (done && hit == null) {
                return true;
            }
            this.at = hit != null ? hit : next;
            PacketDistributor.sendToPlayersNear(level, null, this.at.x, this.at.y, this.at.z, VIEW_RANGE,
                    new ConstructPayload(this.id, AirStrikeMissiles.this.owner.getId(), this.at, this.way, this.fired,
                            1.0F, this.ignites, false, ConstructPayload.MISSILE, this.variant, since, null));
            return done;
        }

        /**
         * Its motor bursts into life, with a flash and a roar: it looks for the creature out to hurt him nearest to it to
         * home in on.
         */
        private void ignite(ServerLevel level) {
            this.lit = true;
            this.speed = Math.max(0.4, this.falling.length());
            if (this.target == null || !this.target.isAlive()) {
                this.target = AirStrikeMissiles.this.nearest(level, this.at,
                        this.small ? JET_REACH * 1.3 : MISSILE_REACH);
            }
            if (this.target == null) {
                // Nothing to find: it strikes the ground a way ahead of the plane (a jet's missile, of where it points).
                Vec3 way = AirStrikeMissiles.this.path.way();
                Vec3 own = new Vec3(this.way.x, 0.0, this.way.z);
                if (this.small && own.lengthSqr() > 1.0E-6) {
                    way = own.normalize();
                }
                Vec3 right = way.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
                RandomSource random = AirStrikeMissiles.this.owner.getRandom();
                double ahead = Mth.lerp(random.nextDouble(), AHEAD_NEAR, AHEAD_FAR);
                double across = (random.nextDouble() * 2.0 - 1.0) * AHEAD_WIDE;
                this.aim = AirStrikeMissiles.this.ground(level, new Vec3(this.at.x, this.at.y, this.at.z)
                        .add(way.scale(ahead)).add(right.scale(across)));
            }
            AirStrikeMissiles.this.sound(level, this.at, SoundEvents.FIREWORK_ROCKET_LAUNCH, this.small ? 3.0F : 7.0F,
                    this.small ? 1.2F : 0.6F);
            AirStrikeMissiles.this.sound(level, this.at, SoundEvents.BLAZE_SHOOT, this.small ? 2.0F : 5.0F,
                    this.small ? 1.4F : 0.7F);
        }

        /**
         * Its nose swings round towards its creature (a little ahead of where it runs), faster the longer it has flown,
         * and it speeds up to its full speed.
         */
        private void steer(ServerLevel level, int burning) {
            if (this.target != null && (!this.target.isAlive() || this.target.level() != level)) {
                // What it homed in on is gone: the next creature out to hurt him nearest to it, or with none the ground
                // where the gone one was, so it never flies off into nothing.
                this.target = AirStrikeMissiles.this.nearest(level, this.at,
                        this.small ? JET_REACH * 1.3 : MISSILE_REACH);
                if (this.target == null && this.aim == null && this.lastGoal != null) {
                    this.aim = AirStrikeMissiles.this.ground(level, this.lastGoal);
                }
            }
            Vec3 goal = this.aim;
            if (this.target != null) {
                Vec3 middle = this.target.getBoundingBox().getCenter();
                double arrives = Math.min(10.0, middle.distanceTo(this.at) / Math.max(1.0, this.speed));
                goal = middle.add(this.target.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(arrives * 0.6));
                this.lastGoal = middle;
            }
            if (goal != null) {
                Vec3 want = goal.subtract(this.at);
                if (want.lengthSqr() > 1.0E-6) {
                    double turn = this.small ? 0.2 + 0.03 * burning : 0.1 + 0.018 * burning;
                    this.way = turnTowards(this.way, want.normalize(), turn);
                }
            }
            this.up = PlanePath.carried(this.up, this.way);
            this.speed = Math.min(this.small ? 4.2 : 3.6, this.speed + (this.small ? 0.45 : 0.3));
        }

        /**
         * Where its middle is as the tip of its nose strikes, on the way the tip goes from {@code tipWas} as its middle
         * goes on to {@code next}: the first creature it may hurt along it (or its creature, once it is close), or else
         * the first block; null when it flies on. It keeps where the tip struck.
         */
        @Nullable
        private Vec3 strikes(ServerLevel level, Vec3 tipWas, Vec3 next) {
            Vec3 tipTo = next.add(this.way.scale(this.nose()));
            double nearest = Double.MAX_VALUE;
            Vec3 on = null;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(tipWas, tipTo).inflate(1.5), AirStrikeMissiles.this::fair)) {
                Vec3 at = living.getBoundingBox().inflate(this.small ? 0.35 : 0.6).clip(tipWas, tipTo).orElse(null);
                if (at == null && living == this.target
                        && living.getBoundingBox().getCenter().distanceTo(tipTo) < (this.small ? 1.0 : 1.6)) {
                    at = tipTo;
                }
                if (at != null && at.distanceToSqr(tipWas) < nearest) {
                    nearest = at.distanceToSqr(tipWas);
                    on = at;
                    this.struck = living;
                }
            }
            BlockHitResult block = LoadedWorld.clip(level, new ClipContext(tipWas, tipTo, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (block.getType() != HitResult.Type.MISS && block.getLocation().distanceToSqr(tipWas) < nearest) {
                this.struck = null;
                on = block.getLocation().subtract(this.way.scale(0.15));
            }
            if (on == null) {
                return null;
            }
            this.tip = on;
            return on.subtract(this.way.scale(this.nose()));
        }

        /**
         * It bursts: a blast of light and fire where its nose struck (the missile itself breaks into solid pieces on
         * every client, see ClientConstructs). A big one blows a small crater out of the ground under it and hurls a
         * few of its blocks away; a small one only bursts.
         */
        void strike(ServerLevel level) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            if (this.small) {
                this.strikeSmall(level);
                return;
            }
            Vec3 tip = this.tip != null ? this.tip : this.at.add(this.way.scale(this.nose()));
            AirStrikeMissiles.this.blast(level, tip, MISSILE_BLAST,
                    AirStrikeMissiles.this.ability.value("missileDamage"), this.struck, 0.9);
            // On a creature it bursts at its middle: the crater goes into the ground under it, if that is near.
            BlockHitResult under = LoadedWorld.clip(level, new ClipContext(tip.add(0.0, 0.3, 0.0),
                    tip.subtract(0.0, MISSILE_GROUND, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    CollisionContext.empty()));
            Vec3 ground = under.getType() == HitResult.Type.MISS ? null : under.getLocation();
            if (ground != null) {
                AirStrikeMissiles.this.crater(level, ground,
                        AirStrikeMissiles.this.ability.value("missileCraterRadius"), MISSILE_DEBRIS);
            }
            Vec3 heart = ground == null ? tip : ground.add(0.0, 0.6, 0.0);
            PacketDistributor.sendToPlayersNear(level, null, heart.x, heart.y, heart.z, VIEW_RANGE,
                    new ConstructPayload(PowerRing.newId(), AirStrikeMissiles.this.owner.getId(), heart, this.way,
                            (float) MISSILE_BLAST, 1.0F, 0.0F, false, ConstructPayload.BLAST, BIG_BLAST, 0, null));
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), heart, 26, 0.35);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 2.4F), heart, 18, 0.22);
            ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, heart.x, heart.y + 0.3, heart.z, 1, 0.0, 0.0, 0.0,
                    0.0);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, heart.x, heart.y + 0.3, heart.z, 6, 0.9, 0.5, 0.9, 0.0);
            ParticleFx.send(level, ParticleTypes.FLAME, heart.x, heart.y + 0.3, heart.z, 24, 0.5, 0.3, 0.5, 0.12);
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, heart.x, heart.y + 0.5, heart.z, 18, 0.7, 0.4, 0.7, 0.05);
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, heart.x, heart.y + 0.4, heart.z, 6, 0.5, 0.2, 0.5,
                    0.02);
            AirStrikeMissiles.this.sound(level, heart, SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 1.0F);
            AirStrikeMissiles.this.sound(level, heart, SoundEvents.DRAGON_FIREBALL_EXPLODE, 2.0F, 1.2F);
            AirStrikeMissiles.this.sound(level, heart, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 1.0F);
        }

        /** A small missile of a jet bursts: a small blast of light and fire, and nothing blown out of the ground. */
        private void strikeSmall(ServerLevel level) {
            Vec3 heart = this.tip != null ? this.tip : this.at.add(this.way.scale(this.nose()));
            AirStrikeMissiles.this.blast(level, heart, SMALL_BLAST_REACH,
                    AirStrikeMissiles.this.ability.value("jetMissileDamage"), this.struck, SMALL_KNOCKBACK);
            PacketDistributor.sendToPlayersNear(level, null, heart.x, heart.y, heart.z, VIEW_RANGE,
                    new ConstructPayload(PowerRing.newId(), AirStrikeMissiles.this.owner.getId(), heart, this.way,
                            (float) SMALL_BLAST_REACH, 1.0F, 0.0F, false, ConstructPayload.BLAST, SMALL_BLAST, 0,
                            null));
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), heart, 14, 0.25);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, heart.x, heart.y + 0.2, heart.z, 2, 0.4, 0.3, 0.4, 0.0);
            ParticleFx.send(level, ParticleTypes.FLAME, heart.x, heart.y + 0.2, heart.z, 10, 0.3, 0.2, 0.3, 0.08);
            ParticleFx.send(level, ParticleTypes.SMOKE, heart.x, heart.y + 0.3, heart.z, 8, 0.4, 0.3, 0.4, 0.03);
            AirStrikeMissiles.this.sound(level, heart, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.5F);
            AirStrikeMissiles.this.sound(level, heart, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.5F);
        }
    }

    /** {@code way} swung towards {@code want} (both one long) by at most {@code angle} radians. */
    private static Vec3 turnTowards(Vec3 way, Vec3 want, double angle) {
        double cos = Mth.clamp(way.dot(want), -1.0, 1.0);
        double between = Math.acos(cos);
        if (between <= angle) {
            return want;
        }
        Vec3 axis = way.cross(want);
        if (axis.lengthSqr() < 1.0E-10) {
            axis = Math.abs(way.y) < 0.9 ? way.cross(new Vec3(0.0, 1.0, 0.0)) : way.cross(new Vec3(1.0, 0.0, 0.0));
        }
        return Vectors.spin(way, axis.normalize(), angle).normalize();
    }

    /**
     * The hatch in its belly is open: a big missile drops out of it, with the plane's own speed and a push down, its
     * motor to fire at a moment of its own, before it has fallen halfway to the ground under it.
     */
    void dropMissile(ServerLevel level) {
        Vec3[] state = this.path.dropsOut(this.age);
        Vec3 at = state[0];
        double high = Math.max(4.0, at.y - this.ground(level, at).y);
        // How many ticks it may fall with its motor dead: until it has fallen some way under halfway down.
        double reach = 0.45 * high;
        double push = PlanePath.DROP_PUSH;
        int latest = (int) Math.floor((-push + Math.sqrt(push * push + 2.0 * PlanePath.GRAVITY * reach))
                / PlanePath.GRAVITY);
        latest = Mth.clamp(latest, 4, IGNITE_LATEST);
        int earliest = Math.min(IGNITE_EARLIEST, latest);
        int ignites = earliest + this.owner.getRandom().nextInt(latest - earliest + 1);
        this.missiles.add(new Missile(state, false, BIG_MISSILE, ignites, null));
        this.sound(level, at, SoundEvents.IRON_TRAPDOOR_OPEN, 6.0F, 0.5F);
        this.sound(level, at, SoundEvents.BEACON_POWER_SELECT, 5.0F, 1.8F);
    }

    /**
     * The jets: each takes shape beside the plane out of its light, races round it and fires a small missile from one
     * pylon and then the other every {@code jetMissileTicks} at the creature out to hurt him nearest to it; once an
     * engine of the plane bursts they break away, break the sound barrier and are gone in a flash.
     */
    void jets(ServerLevel level) {
        int every = Math.max(4, this.ability.intValue("jetMissileTicks"));
        double fled = this.path.jetsFled(this.age);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!this.path.hasJet(k)) {
                continue;
            }
            int since = this.age - (int) this.path.jetFrom(k);
            Vec3 at = this.path.jetAt(k, this.age);
            if (since == 0) {
                this.sound(level, at, SoundEvents.BEACON_ACTIVATE, 6.0F, 1.3F);
                this.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 4.0F, 1.4F);
            }
            if (fled == PlanePath.JET_BOOM) {
                // The sound barrier breaks with a thunderclap, heard far and wide.
                this.sound(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 12.0F, 0.5F);
                this.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 10.0F, 1.5F);
            } else if (fled == PlanePath.JET_GONE) {
                this.sound(level, at, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 14.0F, 0.8F);
                this.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 10.0F, 0.6F);
            }
            int aiming = since - PlanePath.JET_GROWS - 10 - k * every / 2;
            if (fled >= 0.0 || aiming < 0 || aiming % every != 0) {
                continue;
            }
            LivingEntity target = this.nearest(level, at, JET_REACH);
            if (target == null) {
                continue;
            }
            int pylon = this.jetPylons[k];
            this.jetPylons[k] = 1 - pylon;
            Vec3[] state = this.path.firedOff(k, pylon, this.age);
            this.missiles.add(new Missile(state, true, JET_MISSILE + 2 * k + pylon, SMALL_IGNITES, target));
            this.sound(level, state[0], SoundEvents.FIREWORK_ROCKET_SHOOT, 4.0F, 1.3F);
        }
    }

    /** The creature out to hurt him nearest to {@code at}, within {@code reach}; null when there is none. */
    @Nullable
    private LivingEntity nearest(ServerLevel level, Vec3 at, double reach) {
        LivingEntity best = null;
        double bestDistance = reach * reach;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach),
                this::hostile)) {
            double distance = living.getBoundingBox().getCenter().distanceToSqr(at);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    void flyMissiles(ServerLevel level) {
        Iterator<Missile> all = this.missiles.iterator();
        while (all.hasNext()) {
            Missile missile = all.next();
            if (missile.step(level)) {
                missile.strike(level);
                all.remove();
            }
        }
    }

    /** Missiles still in flight break into solid pieces where they are (clients see to that when they are gone). */
    void dropFlying(ServerLevel level) {
        for (Missile missile : this.missiles) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(missile.id));
        }
        this.missiles.clear();
        this.bullets.clear();
    }
}
