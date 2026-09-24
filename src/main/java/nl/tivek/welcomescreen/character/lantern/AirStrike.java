package nl.tivek.welcomescreen.character.lantern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * Green Lantern's ultimate, the Air Strike. He throws his ring fist up at the sky and a pillar of light shoots out of
 * the ring; high over the battlefield, twice as high as a jet would fly, a big, slow gunship with four propellers grows
 * out of it, a construct like any other. It drones on in one straight line over the area he looked at (see
 * {@link PlanePath}) for {@code attackSeconds}:
 * <ul>
 * <li>its sensor under the nose scans the ground round it like the Ring Scan, half as far again, and marks every
 * creature out to hurt him for him, now and every few seconds after;</li>
 * <li>the two miniguns on its sides fire at what it marked in turn, one round each every {@code gunTicks}: many rounds
 * of hard light that spread wide, so not every one strikes;</li>
 * <li>its two missile launchers, one under each wing, fire a homing missile in turn every {@code missileTicks}, that
 * finds its creature {@code hitChance} of the time and bursts in a small blast either way.</li>
 * </ul>
 * Then, all at once, its nose drops and it plunges into the ground: a massive blast of green energy (the ability's
 * damage in the middle, half of it at the edge of {@code crashRadius}) that blows a crater out of the ground and hurls
 * its blocks up and away. Nothing it does ever hurts him, his pets, villagers or animals; other players only where
 * players may fight each other.
 *
 * <p>The plane, its guns, its rounds and its missiles are hard light shaped by his ring, solid like every construct;
 * the pillar, the scan and the blast are light. If he stops being Green Lantern the plane breaks apart in the air.
 */
public final class AirStrike implements SpellEffect {
    /** How long he holds his ring fist up to call the plane, in ticks: the ring does nothing else meanwhile. */
    public static final int CALL_TICKS = 34;
    /** How long the blast of the crash goes on after it, in ticks. */
    public static final int BLAST_TICKS = 90;
    /**
     * Where the miniguns turn on the sides of its body, in blocks at scale 1 from its middle: to its right (the left gun
     * at minus this), up, and ahead; and how long they are from there to the muzzle.
     */
    public static final double GUN_X = 3.2;
    public static final double GUN_Y = -0.6;
    public static final double GUN_Z = 11.0;
    public static final double GUN_LENGTH = 3.6;
    /** Where the missile launchers hang under its wings, in blocks from its middle (the left one at minus x). */
    public static final double LAUNCHER_X = 12.6;
    public static final double LAUNCHER_Y = 0.7;
    public static final double LAUNCHER_Z = 1.6;
    /**
     * Where the hubs of its propellers are, in blocks from its middle: the inner engines this far to either side, the
     * outer ones further out, both at this height and this far ahead. The right inner one is the one that bursts.
     */
    public static final double ENGINE_X = 8.6;
    public static final double ENGINE_OUTER_X = 16.4;
    public static final double ENGINE_Y = 2.35;
    public static final double ENGINE_Z = 8.0;
    /** Where the sensor ball under its nose is, that the scan shines out of. */
    public static final double SENSOR_Y = -3.25;
    public static final double SENSOR_Z = 16.0;
    /** How fast a round from the miniguns flies, in blocks per tick. */
    public static final double BULLET_SPEED = 7.0;
    /** How often its sensor scans the ground again, in ticks. */
    public static final int SCAN_EVERY = 100;
    /** How long a launcher takes to grow its next missile out of the light, in ticks. */
    public static final int RELOAD_TICKS = 26;
    // How high over his eyes it flies at most and at least (under a roof it flies lower, or not at all), how far he may
    // look for the middle of the area, and how far the middle is when he looks at nothing.
    private static final double HEIGHT = 84.0;
    private static final double LOWEST = 32.0;
    private static final double LOOK_REACH = 32.0;
    private static final double LOOK_AHEAD = 16.0;
    // How far above and below its sensor's line a creature may be to be marked, in blocks.
    private static final double SCAN_HIGH = 48.0;
    // Rounds: how far round the line of a round a creature still takes it, how far past what it aimed at a round flies
    // on to the ground, and how far the guns reach.
    private static final double BULLET_HIT = 0.3;
    private static final double BULLET_ON = 14.0;
    private static final double GUN_REACH = 150.0;
    // Missiles: how far their blast reaches, how far off the ones that miss strike, how fast they fly, and how far the
    // launchers reach.
    private static final double MISSILE_BLAST = 2.8;
    private static final double MISS_NEAR = 3.0;
    private static final double MISS_FAR = 6.0;
    private static final double MISSILE_SPEED = 2.6;
    private static final double MISSILE_REACH = 150.0;
    private static final double CRASH_KNOCKBACK = 2.6;
    private static final double VIEW_RANGE = 260.0;
    // How long before its nose drops one of its engines bursts, in ticks.
    private static final int FAILING = 8;

    private static final Map<UUID, AirStrike> ACTIVE = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final PlanePath path;
    private final List<Missile> missiles = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Scan> scans = new ArrayList<>();
    // The creatures its scans marked for him, by entity id, with the tick their mark runs out on.
    private final Map<Integer, Integer> marked = new HashMap<>();
    private int age;
    private boolean leftGun;
    private boolean leftLauncher;
    private boolean crashed;

    private AirStrike(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        this.owner = owner;
        this.ability = ability;
        this.path = path;
    }

    /**
     * The ultimate's key: he throws his ring fist up and calls the plane, as long as the ring is free, can pay for it and
     * there is room in the sky over him.
     *
     * @return true when it began
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            return false;
        }
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        double height = room(level, owner);
        if (height < LOWEST) {
            PowerRing.tell(owner, "no_sky");
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        // The ring hand goes up: the beam it pours out stops.
        LightBeam.stop(owner);
        AirStrike strike = new AirStrike(owner, ability, plan(level, owner, ability, height));
        ACTIVE.put(owner.getUUID(), strike);
        SpellCasting.start(level, strike);
        PowerRing.tell(owner, "air_strike");
        Vec3 eye = owner.getEyePosition();
        strike.sound(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F);
        strike.sound(level, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F, 0.5F);
        strike.sound(level, eye, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2F, 0.5F);
        strike.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
        strike.send(level);
        return true;
    }

    /** How high the plane may fly over him: as high as it goes, or less under a roof (less than LOWEST: no room). */
    private static double room(ServerLevel level, ServerPlayer owner) {
        Vec3 eye = owner.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(0.0, HEIGHT + 8.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        return hit.getType() == HitResult.Type.MISS ? HEIGHT : eye.distanceTo(hit.getLocation()) - 8.0;
    }

    /**
     * Where the plane flies: in one straight line the way he looks, right over the spot he looks at (or a way ahead of
     * him) halfway through its attack, taking shape at the top of the pillar of light. Where it plunges down is worked
     * out now too, from the ground that lies there.
     */
    private static PlanePath plan(ServerLevel level, ServerPlayer owner, CharacterAbility ability, double height) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(LOOK_REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 way = new Vec3(look.x, 0.0, look.z);
        way = way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
        Vec3 middle = hit.getType() == HitResult.Type.MISS ? owner.position().add(way.scale(LOOK_AHEAD))
                : hit.getLocation();
        int attack = Math.max(20, (int) Math.round(ability.value("attackSeconds") * 20.0));
        // Right over the middle halfway through its attack; if the pillar of light up to there is blocked, it takes shape
        // right over him instead.
        double before = PlanePath.SPEED * (PlanePath.FORM * 0.5 + attack * 0.5);
        Vec3 start = new Vec3(middle.x, eye.y + height, middle.z).subtract(way.scale(before));
        if (level.clip(new ClipContext(eye, start, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner))
                .getType() != HitResult.Type.MISS) {
            start = eye.add(0.0, height, 0.0);
        }
        // The dive ends on the ground under where a full one would end; if something stands in the way on the way down,
        // it strikes that instead.
        Vec3 ends = new PlanePath(start, way, height, attack, 1.0).diveEnd();
        BlockHitResult under = level.clip(new ClipContext(ends, ends.subtract(0.0, HEIGHT * 3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        double drop = under.getType() == HitResult.Type.MISS ? height + 1.6 : start.y - under.getLocation().y;
        drop = Math.max(8.0, drop);
        double end = 1.0;
        int steps = 48;
        Vec3 last = PlanePath.diving(start, way, attack, drop, 0.0);
        for (int k = 1; k <= steps; k++) {
            Vec3 next = PlanePath.diving(start, way, attack, drop, (double) k / steps);
            BlockHitResult strike = level.clip(new ClipContext(last, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (strike.getType() != HitResult.Type.MISS) {
                double part = last.distanceTo(strike.getLocation()) / Math.max(1.0E-6, last.distanceTo(next));
                end = Mth.clamp((k - 1 + part) / steps, 0.05, 1.0);
                break;
            }
            last = next;
        }
        // Rounded the way clients get it, so both work out the very same crash.
        return new PlanePath(start, way, drop, attack, Math.round(end * 100.0) / 100.0);
    }

    /** True while this player holds his ring fist up to call the plane: the ring hand does nothing else then. */
    static boolean calling(ServerPlayer player) {
        AirStrike strike = ACTIVE.get(player.getUUID());
        return strike != null && strike.age < CALL_TICKS;
    }

    /** How many ticks this player's air strike still goes on, or 0 when he has none: shown as his ultimate. */
    public static int left(ServerPlayer player) {
        AirStrike strike = ACTIVE.get(player.getUUID());
        return strike == null ? 0 : Math.max(0, (int) Math.ceil(strike.path.crashTick()) + BLAST_TICKS - strike.age);
    }

    /** The server stops: no air strike is going any more. */
    static void clear() {
        ACTIVE.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level) && !this.crashed) {
            // His will no longer holds it: it breaks apart in the air (clients see to that when it is gone).
            this.end(level);
            return false;
        }
        this.age++;
        double dive = this.path.diveTick();
        if (this.age == 8) {
            // Out of the pillar of light it starts to take shape.
            this.sound(level, this.path.at(this.age), SoundEvents.BEACON_ACTIVATE, 8.0F, 0.5F);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.BEACON_ACTIVATE, 1.0F, 0.7F);
        }
        if (this.age == PlanePath.FORM) {
            this.sound(level, this.path.at(this.age), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 10.0F, 0.5F);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.5F);
        }
        if (this.age >= PlanePath.FORM && this.age < dive) {
            int since = this.age - PlanePath.FORM;
            if (since % SCAN_EVERY == 0) {
                this.scan(level);
            }
            // The two guns fire in turn, each one round every gunTicks: two rounds in that time between them.
            int gunEvery = Math.max(2, this.ability.intValue("gunTicks"));
            if (since >= 12 && (since * 2) % gunEvery < 2) {
                this.fireGun(level);
            }
            int missileEvery = Math.max(4, this.ability.intValue("missileTicks"));
            if (since >= 20 && (since - 20) % missileEvery == 0) {
                this.fireMissile(level);
            }
        }
        if (this.age == (int) dive - FAILING) {
            // An engine bursts: it shudders, and a moment later its nose drops.
            Vec3 engine = this.path.point(this.age, ENGINE_X, ENGINE_Y, ENGINE_Z);
            this.sound(level, engine, SoundEvents.GENERIC_EXPLODE.value(), 8.0F, 0.8F);
            this.sound(level, engine, SoundEvents.AMETHYST_CLUSTER_BREAK, 8.0F, 0.5F);
            this.sound(level, engine, SoundEvents.WITHER_HURT, 6.0F, 0.5F);
        }
        if (this.age == (int) dive) {
            this.sound(level, this.path.at(this.age), SoundEvents.WITHER_DEATH, 5.0F, 1.4F);
            this.sound(level, this.path.at(this.age), SoundEvents.ELYTRA_FLYING, 8.0F, 0.6F);
        }
        this.flyMissiles(level);
        this.flyBullets(level);
        this.runScans(level);
        if (!this.crashed && this.age >= this.path.crashTick()) {
            this.crashed = true;
            this.crash(level);
        }
        if (this.age >= this.path.crashTick() + BLAST_TICKS) {
            this.end(level);
            return false;
        }
        // Through the blast as well: the plane is in pieces by then, but clients play it from its clock.
        this.send(level);
        return true;
    }

    // ---- The scan ----

    /** One scan of its sensor rolling out over the ground: every creature out to hurt him it passes gets marked. */
    private final class Scan {
        private final int id = PowerRing.newId();
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
            int since = AirStrike.this.age - this.began;
            double reached = Math.min(this.radius, RingScan.SPEED * since);
            int until = AirStrike.this.age + markTicks();
            AABB area = new AABB(this.center.x - reached, this.center.y - SCAN_HIGH, this.center.z - reached,
                    this.center.x + reached, this.center.y + SCAN_HIGH, this.center.z + reached);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, AirStrike.this::hostile)) {
                double dx = living.getX() - this.center.x;
                double dz = living.getZ() - this.center.z;
                if (dx * dx + dz * dz <= reached * reached) {
                    AirStrike.this.marked.merge(living.getId(), until, Math::max);
                }
            }
            boolean done = since > this.radius / RingScan.SPEED + RingScan.FADE;
            if (done) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            } else {
                PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z,
                        VIEW_RANGE, new ConstructPayload(this.id, AirStrike.this.owner.getId(), this.center,
                                AirStrike.this.path.way(), (float) this.radius, 1.0F, markTicks() / 20.0F, false,
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
    private void scan(ServerLevel level) {
        Vec3 sensor = this.path.point(this.age, 0.0, SENSOR_Y, SENSOR_Z);
        Vec3 center = this.ground(level, sensor);
        this.scans.add(new Scan(center, this.age, this.ability.value("scanBlocks")));
        this.sound(level, sensor, SoundEvents.CONDUIT_ACTIVATE, 8.0F, 1.2F);
        this.sound(level, center, SoundEvents.BEACON_POWER_SELECT, 3.0F, 1.9F);
        this.sound(level, this.owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.6F);
    }

    private void runScans(ServerLevel level) {
        this.scans.removeIf(scan -> scan.step(level));
        this.marked.values().removeIf(until -> until < this.age);
    }

    /**
     * One of the creatures its scans marked, within {@code reach} of {@code from}: from the side of the plane the gun or
     * launcher is on first (though it can still swing round under the body), nearer ones more often. Null when there
     * is none.
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

    /** One round on its way from a minigun to where it strikes, and the tick it gets there. */
    private record Bullet(Vec3 from, Vec3 to, int arrives) {
    }

    /** A round from one minigun and then the other, at a creature its scan marked; nothing when none is in reach. */
    private void fireGun(ServerLevel level) {
        this.leftGun = !this.leftGun;
        double side = this.leftGun ? -1.0 : 1.0;
        Vec3 pivot = this.path.point(this.age, GUN_X * side, GUN_Y, GUN_Z);
        LivingEntity target = this.pickMarked(level, pivot, GUN_REACH, side);
        if (target == null) {
            return;
        }
        // Low accuracy: the rounds spread wide round what they aim at.
        RandomSource random = this.owner.getRandom();
        double spread = this.ability.value("gunSpread");
        double angle = random.nextDouble() * Math.PI * 2.0;
        double far = Math.sqrt(random.nextDouble()) * spread;
        Vec3 aim = target.getBoundingBox().getCenter().add(Math.cos(angle) * far,
                (random.nextDouble() - 0.5) * target.getBbHeight() * 0.6, Math.sin(angle) * far);
        Vec3 way = aim.subtract(pivot).normalize();
        Vec3 muzzle = pivot.add(way.scale(GUN_LENGTH));
        // It flies on past what it aimed at until it strikes the ground, or a roof on the way.
        BlockHitResult block = level.clip(new ClipContext(muzzle, aim.add(way.scale(BULLET_ON)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        Vec3 to = block.getType() == HitResult.Type.MISS ? aim.add(way.scale(BULLET_ON)) : block.getLocation();
        double distance = muzzle.distanceTo(to);
        int travel = Math.max(2, (int) Math.ceil(distance / BULLET_SPEED));
        this.bullets.add(new Bullet(muzzle, to, this.age + travel));
        Vec3 middle = muzzle.lerp(to, 0.5);
        PacketDistributor.sendToPlayersNear(level, null, middle.x, middle.y, middle.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), to, muzzle.subtract(to).normalize(), 0.0F,
                        1.0F, (float) distance, false, ConstructPayload.BULLET, this.leftGun ? 0 : 1, 0, null));
        this.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 9.0F, 1.7F);
        this.sound(level, muzzle, SoundEvents.CHAIN_HIT, 6.0F, 0.6F);
    }

    /** Rounds that get where they were going strike: the first creature along their last stretch takes the hit. */
    private void flyBullets(ServerLevel level) {
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
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.2);
            } else {
                BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(way.scale(-0.1))));
                if (!ground.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z,
                            8, 0.15, 0.05, 0.15, 0.15);
                }
            }
            SpellFx.cloud(level, SpellFx.dust(PowerRing.BRIGHT, 0.9F), at, 4, 0.12, 0.05);
            this.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, 1.6F);
        }
    }

    // ---- Missiles ----

    /** One homing missile on its way from a launcher: it finds its creature, or strikes the ground a way off. */
    private final class Missile {
        private final int id = PowerRing.newId();
        private final Vec3 from;
        private final Vec3 bend;
        private final int fired;
        private final int flight;
        private final int side;
        @Nullable
        private final LivingEntity target;
        private final boolean homing;
        private Vec3 aim;
        private Vec3 at;

        Missile(Vec3 from, Vec3 bend, @Nullable LivingEntity target, boolean homing, Vec3 aim, int side) {
            this.from = from;
            this.bend = bend;
            this.fired = AirStrike.this.age;
            this.target = target;
            this.homing = homing;
            this.aim = aim;
            this.at = from;
            this.side = side;
            this.flight = Mth.clamp((int) Math.round(from.distanceTo(aim) / MISSILE_SPEED), 22, 56);
        }

        /** Moves on; true once it has struck. */
        boolean step(ServerLevel level) {
            if (this.homing && this.target != null && this.target.isAlive() && this.target.level() == level) {
                this.aim = this.target.getBoundingBox().getCenter();
            }
            double u = Math.min(1.0, (double) (AirStrike.this.age - this.fired) / this.flight);
            // Slow off the rail, then faster and faster.
            double s = Math.pow(u, 1.35);
            Vec3 next = this.from.scale((1.0 - s) * (1.0 - s)).add(this.bend.scale(2.0 * (1.0 - s) * s))
                    .add(this.aim.scale(s * s));
            Vec3 way = next.subtract(this.at);
            this.at = next;
            PacketDistributor.sendToPlayersNear(level, null, next.x, next.y, next.z, VIEW_RANGE,
                    new ConstructPayload(this.id, AirStrike.this.owner.getId(), next,
                            way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : way.normalize(), 1.0F, 1.0F,
                            0.0F, false, ConstructPayload.MISSILE, this.side, AirStrike.this.age - this.fired, null));
            return u >= 1.0;
        }

        /** It bursts: a small blast of light and fire where it struck. */
        void strike(ServerLevel level) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            AirStrike.this.blast(level, this.at, MISSILE_BLAST, AirStrike.this.ability.value("missileDamage"),
                    this.homing ? this.target : null, 0.7);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.8F), this.at, 26, 0.35);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 2.4F), this.at, 18, 0.22);
            level.sendParticles(ParticleTypes.EXPLOSION, this.at.x, this.at.y + 0.3, this.at.z, 3, 0.6, 0.4, 0.6, 0.0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.at.x, this.at.y + 0.3, this.at.z, 8, 0.5, 0.3, 0.5,
                    0.04);
            AirStrike.this.sound(level, this.at, SoundEvents.GENERIC_EXPLODE.value(), 2.2F, 1.2F);
            AirStrike.this.sound(level, this.at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 1.1F);
        }
    }

    /** A missile off one launcher and then the other, at a creature its scan marked; nothing when none is in reach. */
    private void fireMissile(ServerLevel level) {
        this.leftLauncher = !this.leftLauncher;
        double side = this.leftLauncher ? -1.0 : 1.0;
        Vec3 from = this.path.point(this.age, LAUNCHER_X * side, LAUNCHER_Y, LAUNCHER_Z);
        LivingEntity target = this.pickMarked(level, from, MISSILE_REACH, side);
        if (target == null) {
            // Nothing to fire at: this launcher keeps its missile for the next time.
            this.leftLauncher = !this.leftLauncher;
            return;
        }
        Vec3[] axes = this.path.axes(this.age);
        Vec3 bend = from.add(axes[2].scale(16.0)).add(axes[0].scale(side * 6.0)).add(0.0, -12.0, 0.0);
        RandomSource random = this.owner.getRandom();
        boolean homing = random.nextDouble() < this.ability.value("hitChance");
        Vec3 aim;
        if (homing) {
            aim = target.getBoundingBox().getCenter();
        } else {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double far = MISS_NEAR + random.nextDouble() * (MISS_FAR - MISS_NEAR);
            aim = this.ground(level, target.position().add(Math.cos(angle) * far, 0.0, Math.sin(angle) * far));
        }
        this.missiles.add(new Missile(from, bend, target, homing, aim, this.leftLauncher ? 0 : 1));
        this.sound(level, from, SoundEvents.FIREWORK_ROCKET_LAUNCH, 8.0F, 0.6F);
        this.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 5.0F, 1.8F);
    }

    private void flyMissiles(ServerLevel level) {
        Iterator<Missile> all = this.missiles.iterator();
        while (all.hasNext()) {
            Missile missile = all.next();
            if (missile.step(level)) {
                missile.strike(level);
                all.remove();
            }
        }
    }

    // ---- The crash ----

    /**
     * It plunges into the ground: a massive blast of green energy that blows a crater out of the ground and hurls its
     * blocks away, and the plane breaks into solid pieces.
     */
    private void crash(ServerLevel level) {
        Vec3 at = this.path.crash();
        double radius = this.ability.value("crashRadius");
        this.blast(level, at, radius, this.ability.getDamage(), null, CRASH_KNOCKBACK);
        this.crater(level, at);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.5, at.z, 6, 3.0, 1.5, 3.0, 0.0);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 2.0, at.z, 2, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 2.0, at.z, 60, 4.0, 2.0, 4.0, 0.08);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 3.0F), at.add(0.0, 1.0, 0.0), 120, 1.3);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 3.5F), at.add(0.0, 1.0, 0.0), 90, 0.8);
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.GREEN, 2.5F), at.add(0.0, 0.3, 0.0), 160, 1.9);
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.PALE, 2.0F), at.add(0.0, 0.6, 0.0), 120, 1.3);
        this.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 10.0F, 0.45F);
        this.sound(level, at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 8.0F, 0.5F);
        this.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, 8.0F, 0.5F);
        this.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 6.0F, 0.5F);
        this.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 6.0F, 0.4F);
        this.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 0.6F);
        // Heard wherever he stands: a rumble rolling out far over the land.
        this.sound(level, this.owner.getEyePosition(), SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.35F);
    }

    /**
     * The crater: a bowl blown out of the ground where it struck, rough at its rim. Some of its blocks are hurled up and
     * away and come down all round it; the rest are gone. Blocks harder than {@code breakHardness}, blocks that hold
     * something (chests and the like) and blocks he may not touch there all stay; so does water.
     */
    private void crater(ServerLevel level, Vec3 at) {
        double hardest = this.ability.value("breakHardness");
        double radius = this.ability.value("craterRadius");
        if (hardest < 0.0 || radius <= 0.0) {
            return;
        }
        double depth = radius * 0.62;
        BlockPos middle = BlockPos.containing(at.x, at.y - 0.5, at.z);
        int reach = (int) Math.ceil(radius + 1.0);
        List<BlockPos> blown = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                // Rough at its rim: every column reaches a little further or less far.
                double rough = 1.0 + 0.2 * (PlanePath.noise(middle.getX() + dx, middle.getZ() + dz, 17) - 0.5);
                for (int dy = -(int) Math.ceil(depth + 1.0); dy <= reach; dy++) {
                    double down = dy < 0 ? dy / depth : dy / (radius * 0.85);
                    double out = (dx * dx + dz * dz) / (radius * radius) + down * down;
                    if (out > rough * rough) {
                        continue;
                    }
                    BlockPos pos = middle.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
                        continue;
                    }
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0.0F || hardness > hardest || !level.mayInteract(this.owner, pos)) {
                        continue;
                    }
                    blown.add(pos);
                }
            }
        }
        // The ones hurled away are taken from near the top of the bowl, where the blast tears the ground open.
        RandomSource random = this.owner.getRandom();
        int debris = Math.max(0, this.ability.intValue("debrisBlocks"));
        List<BlockPos> hurled = new ArrayList<>();
        for (BlockPos pos : blown) {
            if (hurled.size() >= debris) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (pos.getY() >= middle.getY() - 2 && state.isCollisionShapeFullBlock(level, pos)
                    && random.nextDouble() < 0.35) {
                hurled.add(pos);
            }
        }
        for (BlockPos pos : blown) {
            if (!hurled.contains(pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (BlockPos pos : hurled) {
            FallingBlockEntity block = FallingBlockEntity.fall(level, pos, level.getBlockState(pos));
            block.dropItem = false;
            Vec3 out = new Vec3(pos.getX() + 0.5 - at.x, 0.0, pos.getZ() + 0.5 - at.z);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5)
                    : out.normalize();
            double speed = 0.45 + 0.55 * random.nextDouble();
            block.setDeltaMovement(out.x * speed, 0.7 + 0.8 * random.nextDouble(), out.z * speed);
            block.hurtMarked = true;
        }
    }

    /**
     * A blast of the ring's light at {@code at}: every creature within {@code radius} that is fair game is hurt, most
     * in the middle (half at the edge), and thrown away from it. {@code direct}, when it is there, takes the full
     * damage wherever it is: the creature a missile found.
     */
    private void blast(ServerLevel level, Vec3 at, double radius, double damage, @Nullable LivingEntity direct,
            double knockback) {
        AABB area = new AABB(at, at).inflate(radius + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this::fair)) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double distance = middle.distanceTo(at);
            boolean hit = target == direct;
            if (!hit && distance > radius + target.getBbWidth() * 0.5) {
                continue;
            }
            double near = hit ? 1.0 : 1.0 - 0.5 * Mth.clamp(distance / Math.max(0.1, radius), 0.0, 1.0);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * near));
            if (knockback > 0.0) {
                Vec3 away = new Vec3(middle.x - at.x, 0.0, middle.z - at.z);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                double push = knockback * near;
                target.setDeltaMovement(target.getDeltaMovement().add(new Vec3(away.x * push, 0.25 + 0.3 * push,
                        away.z * push).scale(1.0 - resist)));
                target.hasImpulse = true;
                target.hurtMarked = true;
            }
        }
    }

    /**
     * Who the air strike may hurt: a creature that is out to hurt him (a monster, or anything that has him as its
     * target), or a player he may fight. Never his own pets, villagers or animals.
     */
    private boolean fair(LivingEntity living) {
        if (!PowerRing.canHit(this.owner, living)) {
            return false;
        }
        if (living instanceof OwnableEntity pet && pet.getOwner() == this.owner) {
            return false;
        }
        return living instanceof Enemy || living instanceof Player
                || living instanceof Mob mob && mob.getTarget() == this.owner;
    }

    /** What its scan marks and its guns and missiles go for: a creature out to hurt him, never a player. */
    private boolean hostile(LivingEntity living) {
        return !(living instanceof Player) && this.fair(living);
    }

    /**
     * The top of whatever lies at this spot, looked for from the height the plane flies at straight down: the ground,
     * or a roof or a tree top standing on it. Where there is none, the spot at his own height.
     */
    private Vec3 ground(ServerLevel level, Vec3 at) {
        double top = this.path.start().y + 4.0;
        Vec3 from = new Vec3(at.x, top, at.z);
        BlockHitResult hit = level.clip(new ClipContext(from, from.subtract(0.0, HEIGHT * 3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? new Vec3(at.x, this.owner.getY(), at.z) : hit.getLocation();
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
        for (Missile missile : this.missiles) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(missile.id));
        }
        for (Scan scan : this.scans) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(scan.id));
        }
        this.missiles.clear();
        this.bullets.clear();
        this.scans.clear();
    }

    /** Where it flies, for clients to work it out by themselves (see {@link PlanePath}), and how long ago he called it. */
    private void send(ServerLevel level) {
        Vec3 at = this.path.at(Math.min(this.age, this.path.crashTick()));
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.path.start(), this.path.way(),
                        (float) this.path.drop(), 1.0F, (float) this.path.attack(), true, ConstructPayload.PLANE,
                        (int) Math.round(this.path.end() * 100.0), this.age, null));
    }

    private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
