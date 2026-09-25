package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.BULLET_SPEED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.GUN_LENGTH;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.SENSOR_Y;
import static nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike.SENSOR_Z;

/**
 * The plane's sensor and its two miniguns: the scans that mark the creatures out to hurt him, and the rounds the guns
 * fire at them (or rake the ground with while nothing is marked). Part of the {@link AirStrike}.
 */
abstract class AirStrikeGuns extends AirStrikeBlasts {
    // How far above and below its sensor's line a creature may be to be marked, in blocks.
    private static final double SCAN_HIGH = 48.0;
    // Rounds: how far round the line of a round a creature still takes it, how far past what it aimed at a round flies
    // on to the ground, and how far the guns reach.
    private static final double BULLET_HIT = 0.3;
    private static final double BULLET_ON = 14.0;
    private static final double GUN_REACH = 150.0;
    // How long a gun keeps firing at the creature it picked before it looks again, in ticks.
    private static final int GUN_KEEPS = 40;
    // Where the guns rake the ground when nothing is marked: how far ahead of the spot under the plane, between these
    // two, and how far to its side, between these two.
    private static final double RAKE_NEAR = 6.0;
    private static final double RAKE_FAR = 20.0;
    private static final double RAKE_IN = 2.5;
    private static final double RAKE_OUT = 9.0;

    final List<Bullet> bullets = new ArrayList<>();
    final List<Scan> scans = new ArrayList<>();
    // The creatures its scans marked for him, by entity id, with the tick their mark runs out on.
    private final Map<Integer, Integer> marked = new HashMap<>();
    // How each minigun swings (0 the left one, 1 the right one), worked out as every client works it out.
    private final PlanePath.Turret[] turrets;
    // What each minigun fires at and until when it keeps on that one.
    private final LivingEntity[] gunTargets = new LivingEntity[2];
    private final int[] gunKeeps = new int[2];
    // The rounds the guns still owe: they fire as many per tick as their pace comes to, a whole one at a time.
    double gunsOwe;
    private boolean leftGun;

    AirStrikeGuns(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        super(owner, ability, path);
        this.turrets = new PlanePath.Turret[] { new PlanePath.Turret(path, 0), new PlanePath.Turret(path, 1) };
    }

    // ---- The scan ----

    /** One scan of its sensor rolling out over the ground: every creature out to hurt him it passes gets marked. */
    final class Scan {
        final int id = PowerRing.newId();
        private final Vec3 center;
        private final int began;
        private final double radius;

        Scan(Vec3 center, int began, double radius) {
            this.center = center;
            this.began = began;
            this.radius = radius;
        }

        /** Rolls on; true once it is done. */
        boolean step(ServerLevel level) {
            int since = AirStrikeGuns.this.age - this.began;
            double reached = Math.min(this.radius, RingScan.SPEED * since);
            int until = AirStrikeGuns.this.age + markTicks();
            AABB area = new AABB(this.center.x - reached, this.center.y - SCAN_HIGH, this.center.z - reached,
                    this.center.x + reached, this.center.y + SCAN_HIGH, this.center.z + reached);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                    AirStrikeGuns.this::hostile)) {
                double dx = living.getX() - this.center.x;
                double dz = living.getZ() - this.center.z;
                if (dx * dx + dz * dz <= reached * reached) {
                    AirStrikeGuns.this.marked.merge(living.getId(), until, Math::max);
                }
            }
            boolean done = since > this.radius / RingScan.SPEED + RingScan.FADE;
            if (done) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            } else {
                PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z,
                        VIEW_RANGE, new ConstructPayload(this.id, AirStrikeGuns.this.owner.getId(), this.center,
                                AirStrikeGuns.this.path.way(), (float) this.radius, 1.0F, markTicks() / 20.0F, false,
                                ConstructPayload.SCAN, ConstructPayload.SCAN_HOSTILE, since, null));
            }
            return done;
        }
    }

    /** How long what its scan marks stays marked: as long as the Ring Scan marks what it finds. */
    private static int markTicks() {
        CharacterAbility scan = GameCharacter.GREEN_LANTERN.byName("ring_scan");
        return (int) Math.round((scan == null ? 21.0 : scan.value("markSeconds")) * 20.0);
    }

    /** Its sensor scans the ground right under it, with a sound everyone near can hear. */
    void scan(ServerLevel level) {
        Vec3 sensor = this.path.point(this.age, 0.0, SENSOR_Y, SENSOR_Z);
        Vec3 center = this.ground(level, sensor);
        this.scans.add(new Scan(center, this.age, this.ability.value("scanBlocks")));
        this.sound(level, sensor, SoundEvents.CONDUIT_ACTIVATE, 8.0F, 1.2F);
        this.sound(level, center, SoundEvents.BEACON_POWER_SELECT, 3.0F, 1.9F);
        this.sound(level, this.owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.6F);
    }

    void runScans(ServerLevel level) {
        this.scans.removeIf(scan -> scan.step(level));
        this.marked.values().removeIf(until -> until < this.age);
    }

    /**
     * One of the creatures its scans marked, within {@code reach} of {@code from}: from the side of the plane the gun
     * is on first (though it can still swing round under the body), nearer ones more often. Null when there is none.
     *
     * @param side 1 for its right side, -1 for its left
     */
    @Nullable
    private LivingEntity pickMarked(ServerLevel level, Vec3 from, double reach, double side) {
        Vec3 right = this.path.axes(this.age)[0];
        RandomSource random = this.owner.getRandom();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (int entity : this.marked.keySet()) {
            if (!(level.getEntity(entity) instanceof LivingEntity living) || !this.hostile(living)) {
                continue;
            }
            Vec3 at = living.getBoundingBox().getCenter();
            double distance = at.distanceTo(from);
            if (distance > reach) {
                continue;
            }
            double across = at.subtract(from).dot(right) * side;
            // A little chance in it, so a crowd is shared out instead of the nearest one taking everything.
            double score = distance * (0.6 + 0.8 * random.nextDouble()) * (across >= -2.0 ? 1.0 : 2.5);
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }

    // ---- The miniguns ----

    /**
     * One round on its way from a minigun to where it strikes, the tick it gets there, and whether it strikes nothing
     * at all (it flies off into the air).
     */
    private record Bullet(Vec3 from, Vec3 to, int arrives, boolean air) {
    }

    /**
     * A round from one minigun and then the other, out of its barrel the way it points right now (see
     * {@link PlanePath.Turret}), spread wide round that. Every round also tells the gun where to swing to next: to a
     * creature its scan marked when one is in reach, and otherwise to the ground along its way (see {@link #rake}). A
     * gun keeps on the creature it picked a while, so it swings round to it once and stays on it, its rounds walking
     * onto it as it swings, instead of jumping from one to the next.
     */
    void fireGun(ServerLevel level) {
        this.leftGun = !this.leftGun;
        int gun = this.leftGun ? 0 : 1;
        double side = this.leftGun ? -1.0 : 1.0;
        Vec3 pivot = this.path.pivot(gun, this.age);
        Vec3 barrel = this.turrets[gun].aim(this.age);
        Vec3 muzzle = pivot.add(barrel.scale(GUN_LENGTH));
        LivingEntity target = this.gunTargets[gun];
        if (target == null || this.age >= this.gunKeeps[gun] || !target.isAlive() || target.level() != level
                || !this.marked.containsKey(target.getId()) || !this.hostile(target)
                || target.getBoundingBox().getCenter().distanceTo(pivot) > GUN_REACH) {
            target = this.pickMarked(level, pivot, GUN_REACH, side);
            this.gunTargets[gun] = target;
            this.gunKeeps[gun] = this.age + GUN_KEEPS;
        }
        Vec3 goal = target == null ? this.rake(level, side) : target.getBoundingBox().getCenter();
        Vec3 next = this.path.gunGoal(gun, this.age, goal);
        this.turrets[gun].fired(this.age, next);
        // Low accuracy: the rounds spread wide round where the barrel points, as far round as gunSpread blocks out at
        // what it fires at.
        RandomSource random = this.owner.getRandom();
        double spread = this.ability.value("gunSpread");
        double angle = random.nextDouble() * Math.PI * 2.0;
        double off = Math.sqrt(random.nextDouble()) * spread;
        Vec3[] across = Vectors.across(barrel);
        Vec3 way = barrel.scale(Math.max(8.0, goal.distanceTo(muzzle))).add(across[0].scale(Math.cos(angle) * off))
                .add(across[1].scale(Math.sin(angle) * off)).normalize();
        // It flies on until it strikes the ground, a roof or a creature on the way; with none of those in reach it
        // flies off into the air.
        Vec3 end = muzzle.add(way.scale(GUN_REACH));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(muzzle, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, CollisionContext.empty()));
        Vec3 to = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        boolean air = block.getType() == HitResult.Type.MISS;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(muzzle, to).inflate(1.0),
                this::fair)) {
            Vec3 on = living.getBoundingBox().inflate(BULLET_HIT).clip(muzzle, to).orElse(null);
            if (on != null) {
                to = on;
                air = false;
            }
        }
        double distance = muzzle.distanceTo(to);
        int travel = Math.max(2, (int) Math.ceil(distance / BULLET_SPEED));
        this.bullets.add(new Bullet(muzzle, to, this.age + travel, air));
        Vec3 middle = muzzle.lerp(to, 0.5);
        PacketDistributor.sendToPlayersNear(level, null, middle.x, middle.y, middle.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), to, next, travel, 1.0F, this.age, air,
                        ConstructPayload.BULLET, gun, 0, null));
        this.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 9.0F, 1.7F);
        this.sound(level, muzzle, SoundEvents.CHAIN_HIT, 6.0F, 0.6F);
    }

    /**
     * Where a gun with nothing marked to fire at rakes the ground: ahead of the plane on its own side, the spot walking
     * to and fro across its way and nearer and further, so its rounds stitch lines over the ground as it drones on.
     *
     * @param side 1 for its right side, -1 for its left
     */
    private Vec3 rake(ServerLevel level, double side) {
        Vec3 way = this.path.way();
        Vec3 right = way.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        double phase = side > 0.0 ? 0.0 : 1.9;
        double ahead = Mth.lerp(0.5 + 0.5 * Math.sin(this.age * 0.05 + phase), RAKE_NEAR, RAKE_FAR);
        double across = side * Mth.lerp(0.5 + 0.5 * Math.sin(this.age * 0.13 + phase * 1.7), RAKE_IN, RAKE_OUT);
        return this.ground(level, this.path.at(this.age).add(way.scale(ahead)).add(right.scale(across)));
    }

    /** Rounds that get where they were going strike: the first creature along their last stretch takes the hit. */
    void flyBullets(ServerLevel level) {
        Iterator<Bullet> all = this.bullets.iterator();
        while (all.hasNext()) {
            Bullet bullet = all.next();
            if (this.age < bullet.arrives()) {
                continue;
            }
            all.remove();
            Vec3 way = bullet.to().subtract(bullet.from()).normalize();
            Vec3 from = bullet.to().subtract(way.scale(BULLET_ON + 4.0));
            LivingEntity struck = null;
            double nearest = Double.MAX_VALUE;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(from, bullet.to()).inflate(1.0), this::fair)) {
                Vec3 on = living.getBoundingBox().inflate(BULLET_HIT).clip(from, bullet.to()).orElse(null);
                if (on != null && on.distanceToSqr(from) < nearest) {
                    nearest = on.distanceToSqr(from);
                    struck = living;
                }
            }
            Vec3 at = bullet.to();
            if (struck != null) {
                at = from.add(way.scale(Math.sqrt(nearest)));
                struck.invulnerableTime = 0;
                struck.hurt(level.damageSources().playerAttack(this.owner), (float) this.ability.value("gunDamage"));
                ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.2);
            } else if (bullet.air()) {
                // It struck nothing: it flies off into the air, with nothing to splash on.
                continue;
            } else {
                BlockPos spot = BlockPos.containing(at.subtract(way.scale(-0.1)));
                BlockState ground = level.isLoaded(spot) ? level.getBlockState(spot) : Blocks.AIR.defaultBlockState();
                if (!ground.isAir()) {
                    ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z,
                            8, 0.15, 0.05, 0.15, 0.15);
                }
            }
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), at, 4, 0.12, 0.05);
            this.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, 1.6F);
        }
    }
}
