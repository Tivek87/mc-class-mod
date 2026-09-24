package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Collections;
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
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.BlockRules;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

/**
 * Green Lantern's ultimate, the Air Strike. He throws his ring fist up at the sky and a pillar of light shoots out of
 * the ring; high over the battlefield, some 55 blocks over his eyes, a big, slow gunship with four propellers grows out
 * of it, a construct like any other. It drones on in one straight line over the area he looked at (see
 * {@link PlanePath}) for {@code attackSeconds}, and it never stops firing:
 * <ul>
 * <li>its sensor under the nose scans the ground round it like the Ring Scan, half as far again, and marks every
 * creature out to hurt him for him, now and every few seconds after;</li>
 * <li>the two miniguns on its sides fire in turn, one round each every {@code gunTicks}: many rounds of hard light that
 * spread wide, so not every one strikes. Each swings smoothly round on its ball to what it marked (see
 * {@link PlanePath.Turret}) and keeps on it a while, every round leaving its barrel the way it points, so its fire walks
 * onto what it swings to; with nothing marked in reach they rake the ground along its way, their rounds walking to and
 * fro ahead of it;</li>
 * <li>every {@code missileTicks} the hatch in its belly opens and a big missile drops out of it; the hatch shuts behind
 * it. The missile falls a way, and at a moment of its own its motor bursts into life: it homes in on the creature out to
 * hurt him nearest to it (with none, it strikes the ground along the plane's way; when that creature dies first, the
 * next nearest, or the ground where it was) and bursts as its nose strikes, in a blast that blows a small crater out
 * of the ground;</li>
 * <li>two jets take shape beside it and race round it, fast, each firing a small homing missile from under its wings
 * every {@code jetMissileTicks} at the creature out to hurt him nearest to it, that bursts in a small blast.</li>
 * </ul>
 * Then one of its engines bursts: it shudders and struggles, its jets break away and race off so fast they break the
 * sound barrier and are gone in a flash, and its nose drops. It plunges into the ground, faster and faster, until the
 * first of its parts strikes: a massive blast of green energy (the ability's damage in the middle, half of it at the
 * edge of {@code crashRadius}) that blows a crater out of the ground and hurls its blocks up and away. Every crater is
 * blown out a little at a time over a few ticks, top first, so the server never stalls on it. Nothing it does ever
 * hurts him, his pets, villagers or animals; other players only where players may fight each other.
 *
 * <p>The plane, its jets, its guns, its rounds and its missiles are hard light shaped by his ring, solid like every
 * construct; the pillar, the scan and the blasts are light. If he stops being Green Lantern the plane, its jets and the
 * missiles still in flight break apart in the air.
 */
public final class AirStrike implements Effect {
    /** How long he holds his ring fist up to call the plane, in ticks: the ring does nothing else meanwhile. */
    public static final int CALL_TICKS = 34;
    /** How long the blast of the crash goes on after it, in ticks. */
    public static final int BLAST_TICKS = 90;
    /** What a {@link ConstructPayload#MISSILE} is: a big one out of the plane's hatch, or a small one of a jet. */
    public static final int BIG_MISSILE = 0;
    /** A small missile of jet k from the pylon on side s (0 left, 1 right): this plus 2k plus s. */
    public static final int JET_MISSILE = 2;
    /** What a {@link ConstructPayload#BLAST} is: the blast of a big missile, or the small one of a jet's missile. */
    public static final int BIG_BLAST = 0;
    public static final int SMALL_BLAST = 1;
    /**
     * Where the miniguns turn on the sides of its body, in blocks at scale 1 from its middle: to its right (the left gun
     * at minus this), up, and ahead; and how long they are from there to the muzzle.
     */
    public static final double GUN_X = 4.3;
    public static final double GUN_Y = -1.7;
    public static final double GUN_Z = 10.0;
    public static final double GUN_LENGTH = 6.4;
    /**
     * The bomb bay in its belly: its middle along the body and how long it is, in blocks at scale 1; and where a missile
     * hangs under the open hatch the moment it drops.
     */
    public static final double BAY_Z = -0.6;
    public static final double BAY_LENGTH = 8.6;
    public static final double DROP_Y = -3.35;
    /**
     * How big a big missile out of the hatch and a jet's small one are, next to the model of a missile; and how far the
     * tip of its nose is ahead of its middle in that model, in blocks at scale 1.
     */
    public static final double MISSILE_SCALE = 1.45;
    public static final double SMALL_MISSILE_SCALE = 0.62;
    public static final double MISSILE_NOSE = 2.3;
    /** Where the pylons under a jet's wings are, in blocks at the jet's scale 1 from its middle (the left one at -x). */
    public static final double PYLON_X = 3.3;
    public static final double PYLON_Y = -0.55;
    public static final double PYLON_Z = 0.2;
    /** How long a jet's pylon takes to grow its next missile out of the light, in ticks. */
    public static final int RELOAD_TICKS = 16;
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
    // How high over his eyes it flies at most and at least (under a roof it flies lower, or not at all), how far he may
    // look for the middle of the area, and how far the middle is when he looks at nothing.
    private static final double HEIGHT = 55.0;
    private static final double LOWEST = 21.0;
    private static final double LOOK_REACH = 32.0;
    private static final double LOOK_AHEAD = 16.0;
    // How far above and below its sensor's line a creature may be to be marked, in blocks.
    private static final double SCAN_HIGH = 48.0;
    // Rounds: how far round the line of a round a creature still takes it, how far past what it aimed at a round flies
    // on to the ground, and how far the guns reach.
    private static final double BULLET_HIT = 0.3;
    private static final double BULLET_ON = 14.0;
    private static final double GUN_REACH = 150.0;
    // How long a gun keeps firing at the creature it picked before it looks again, in ticks.
    private static final int GUN_KEEPS = 40;
    // The big missiles: how far their blast reaches, how far they look for a creature to home in on, and how long one
    // flies at most before it bursts by itself.
    private static final double MISSILE_BLAST = 3.2;
    private static final double MISSILE_REACH = 150.0;
    private static final int MISSILE_LIFE = 160;
    // A big missile drops out of the hatch (see PlanePath.dropsOut), falls, and fires its motor between these two ticks
    // after it dropped.
    private static final int IGNITE_EARLIEST = 9;
    /** The latest a big missile's motor fires, in ticks after it dropped out of the hatch. */
    public static final int IGNITE_LATEST = 24;
    // The small missiles of the jets: how far their blast reaches, how hard it throws, and how far a jet looks for a
    // creature to fire at.
    private static final double SMALL_BLAST_REACH = 2.0;
    private static final double SMALL_KNOCKBACK = 0.5;
    private static final double JET_REACH = 72.0;
    /** The tick after a jet fired its small missile that the missile's motor fires. */
    public static final int SMALL_IGNITES = 3;
    // How far below a missile's blast the ground may lie for it to blow a small crater there, and how many of that
    // crater's blocks it hurls away.
    private static final double MISSILE_GROUND = 2.5;
    private static final int MISSILE_DEBRIS = 6;
    // Where the guns rake the ground when nothing is marked: how far ahead of the spot under the plane, between these
    // two, and how far to its side, between these two.
    private static final double RAKE_NEAR = 6.0;
    private static final double RAKE_FAR = 20.0;
    private static final double RAKE_IN = 2.5;
    private static final double RAKE_OUT = 9.0;
    // Where a missile with nothing to find strikes: this far ahead of the spot under the plane, and at most this far to
    // the side of its way.
    private static final double AHEAD_NEAR = 14.0;
    private static final double AHEAD_FAR = 30.0;
    private static final double AHEAD_WIDE = 10.0;
    private static final double CRASH_KNOCKBACK = 2.8;
    private static final double VIEW_RANGE = 260.0;

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
    // How each minigun swings (0 the left one, 1 the right one), worked out as every client works it out.
    private final PlanePath.Turret[] turrets;
    // What each minigun fires at and until when it keeps on that one.
    private final LivingEntity[] gunTargets = new LivingEntity[2];
    private final int[] gunKeeps = new int[2];
    // The rounds the guns still owe: they fire as many per tick as their pace comes to, a whole one at a time.
    private double gunsOwe;
    // Which pylon of each jet fires next (0 the left one, 1 the right one).
    private final int[] jetPylons = new int[PlanePath.JETS];
    private int age;
    private boolean leftGun;
    private boolean crashed;

    private AirStrike(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        this.owner = owner;
        this.ability = ability;
        this.path = path;
        this.turrets = new PlanePath.Turret[] { new PlanePath.Turret(path, 0), new PlanePath.Turret(path, 1) };
    }

    /**
     * The ultimate's key: he throws his ring fist up and calls the plane, as long as the ring is free, can pay for it and
     * there is room in the sky over him.
     *
     * @return true when it began
     */
    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            return false;
        }
        if (Recharge.busy(owner)) {
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
        Effects.start(level, strike);
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
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(eye, eye.add(0.0, HEIGHT + 8.0, 0.0),
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
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(eye, eye.add(look.scale(LOOK_REACH)),
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
        if (LoadedWorld.clip(level, new ClipContext(eye, start, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner)).getType() != HitResult.Type.MISS) {
            start = eye.add(0.0, height, 0.0);
        }
        // The dive ends on the ground under where a full one would end; if something stands in the way on the way down,
        // it strikes that instead. It ends as the first of its parts that reach out furthest (its nose, a wingtip, a
        // propeller...) strikes, not its middle: nothing of it ever sinks into the ground before the crash.
        Vec3 ends = new PlanePath(start, way, height, attack, 1.0).diveEnd();
        BlockHitResult under = LoadedWorld.clip(level, new ClipContext(ends, ends.subtract(0.0, HEIGHT * 3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        double drop = under.getType() == HitResult.Type.MISS ? height + 1.6 : start.y - under.getLocation().y;
        drop = Math.max(8.0, drop);
        PlanePath full = new PlanePath(start, way, drop, attack, 1.0);
        double end = 1.0;
        int steps = PlanePath.DIVE;
        Vec3[] last = reaches(full, full.diveTick());
        for (int k = 1; k <= steps && end >= 1.0; k++) {
            Vec3[] next = reaches(full, full.diveTick() + k);
            for (int p = 0; p < next.length; p++) {
                BlockHitResult strike = LoadedWorld.clip(level, new ClipContext(last[p], next[p],
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
                if (strike.getType() != HitResult.Type.MISS) {
                    double part = last[p].distanceTo(strike.getLocation())
                            / Math.max(1.0E-6, last[p].distanceTo(next[p]));
                    end = Math.min(end, (k - 1 + part) / steps);
                }
            }
            last = next;
        }
        // Rounded down the way clients get it, so both work out the very same crash, and it strikes a hair before any
        // part would sink into the ground.
        return new PlanePath(start, way, drop, attack, Math.max(0.05, Math.floor(end * 100.0) / 100.0));
    }

    /** The parts of the plane that reach out furthest (see {@link PlanePath#REACHES}) on tick {@code t}. */
    private static Vec3[] reaches(PlanePath path, double t) {
        Vec3[] at = new Vec3[PlanePath.REACHES.length];
        for (int p = 0; p < at.length; p++) {
            double[] part = PlanePath.REACHES[p];
            at[p] = path.point(t, part[0], part[1], part[2]);
        }
        return at;
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
    public static void clear() {
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
            if (since % SCAN_EVERY == 0 && this.age < this.path.failTick()) {
                this.scan(level);
            }
            // The two guns fire in turn, each one round every gunTicks: two rounds in that time between them. They keep
            // on firing while it struggles, until its nose drops.
            if (since >= 12) {
                this.gunsOwe += 2.0 / Math.max(1.0, this.ability.value("gunTicks"));
                while (this.gunsOwe >= 1.0) {
                    this.gunsOwe -= 1.0;
                    this.fireGun(level);
                }
            }
            int missileEvery = Math.max(4, this.ability.intValue("missileTicks"));
            if (this.path.releases(this.age, missileEvery)) {
                this.dropMissile(level);
            }
            this.jets(level);
        }
        if (this.age == (int) Math.round(this.path.failTick())) {
            // An engine bursts: it shudders and struggles, and a while later its nose drops.
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
        } else if (this.crashed && this.age % 3 == 0) {
            this.smoulder(level);
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
    private void fireGun(ServerLevel level) {
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
            this.fired = AirStrike.this.age;
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
            int since = AirStrike.this.age - this.fired;
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
                    new ConstructPayload(this.id, AirStrike.this.owner.getId(), this.at, this.way, this.fired, 1.0F,
                            this.ignites, false, ConstructPayload.MISSILE, this.variant, since, null));
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
                this.target = AirStrike.this.nearest(level, this.at, this.small ? JET_REACH * 1.3 : MISSILE_REACH);
            }
            if (this.target == null && !this.small) {
                // Nothing to find: it strikes the ground a way ahead of the plane.
                Vec3 way = AirStrike.this.path.way();
                Vec3 right = way.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
                RandomSource random = AirStrike.this.owner.getRandom();
                double ahead = Mth.lerp(random.nextDouble(), AHEAD_NEAR, AHEAD_FAR);
                double across = (random.nextDouble() * 2.0 - 1.0) * AHEAD_WIDE;
                this.aim = AirStrike.this.ground(level, new Vec3(this.at.x, this.at.y, this.at.z)
                        .add(way.scale(ahead)).add(right.scale(across)));
            }
            AirStrike.this.sound(level, this.at, SoundEvents.FIREWORK_ROCKET_LAUNCH, this.small ? 3.0F : 7.0F,
                    this.small ? 1.2F : 0.6F);
            AirStrike.this.sound(level, this.at, SoundEvents.BLAZE_SHOOT, this.small ? 2.0F : 5.0F,
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
                this.target = AirStrike.this.nearest(level, this.at, this.small ? JET_REACH * 1.3 : MISSILE_REACH);
                if (this.target == null && this.aim == null && this.lastGoal != null) {
                    this.aim = AirStrike.this.ground(level, this.lastGoal);
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
                    new AABB(tipWas, tipTo).inflate(1.5), AirStrike.this::fair)) {
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
            AirStrike.this.blast(level, tip, MISSILE_BLAST, AirStrike.this.ability.value("missileDamage"),
                    this.struck, 0.9);
            // On a creature it bursts at its middle: the crater goes into the ground under it, if that is near.
            BlockHitResult under = LoadedWorld.clip(level, new ClipContext(tip.add(0.0, 0.3, 0.0),
                    tip.subtract(0.0, MISSILE_GROUND, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    CollisionContext.empty()));
            Vec3 ground = under.getType() == HitResult.Type.MISS ? null : under.getLocation();
            if (ground != null) {
                AirStrike.this.crater(level, ground, AirStrike.this.ability.value("missileCraterRadius"),
                        MISSILE_DEBRIS);
            }
            Vec3 heart = ground == null ? tip : ground.add(0.0, 0.6, 0.0);
            PacketDistributor.sendToPlayersNear(level, null, heart.x, heart.y, heart.z, VIEW_RANGE,
                    new ConstructPayload(PowerRing.newId(), AirStrike.this.owner.getId(), heart, this.way,
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
            AirStrike.this.sound(level, heart, SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 1.0F);
            AirStrike.this.sound(level, heart, SoundEvents.DRAGON_FIREBALL_EXPLODE, 2.0F, 1.2F);
            AirStrike.this.sound(level, heart, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 1.0F);
        }

        /** A small missile of a jet bursts: a small blast of light and fire, and nothing blown out of the ground. */
        private void strikeSmall(ServerLevel level) {
            Vec3 heart = this.tip != null ? this.tip : this.at.add(this.way.scale(this.nose()));
            AirStrike.this.blast(level, heart, SMALL_BLAST_REACH, AirStrike.this.ability.value("jetMissileDamage"),
                    this.struck, SMALL_KNOCKBACK);
            PacketDistributor.sendToPlayersNear(level, null, heart.x, heart.y, heart.z, VIEW_RANGE,
                    new ConstructPayload(PowerRing.newId(), AirStrike.this.owner.getId(), heart, this.way,
                            (float) SMALL_BLAST_REACH, 1.0F, 0.0F, false, ConstructPayload.BLAST, SMALL_BLAST, 0,
                            null));
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), heart, 14, 0.25);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, heart.x, heart.y + 0.2, heart.z, 2, 0.4, 0.3, 0.4, 0.0);
            ParticleFx.send(level, ParticleTypes.FLAME, heart.x, heart.y + 0.2, heart.z, 10, 0.3, 0.2, 0.3, 0.08);
            ParticleFx.send(level, ParticleTypes.SMOKE, heart.x, heart.y + 0.3, heart.z, 8, 0.4, 0.3, 0.4, 0.03);
            AirStrike.this.sound(level, heart, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.5F);
            AirStrike.this.sound(level, heart, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.5F);
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
    private void dropMissile(ServerLevel level) {
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
    private void jets(ServerLevel level) {
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
        // It struck a hair before its part touched: the crater goes into the ground right under that.
        BlockHitResult under = LoadedWorld.clip(level, new ClipContext(at.add(0.0, 0.5, 0.0),
                at.subtract(0.0, 6.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        Vec3 ground = under.getType() == HitResult.Type.MISS ? at : under.getLocation();
        this.crater(level, ground, this.ability.value("craterRadius"), this.ability.intValue("debrisBlocks"));
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1.5, at.z, 10, 4.0, 1.7, 4.0, 0.0);
        ParticleFx.send(level, ParticleTypes.FLASH, at.x, at.y + 2.0, at.z, 2, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 2.0, at.z, 90, 4.6, 2.3, 4.6, 0.11);
        ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y + 1.0, at.z, 100, 3.5, 1.2, 3.5, 0.38);
        ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 34, 2.9, 0.6, 2.9, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 3.0F), at.add(0.0, 1.0, 0.0), 130, 1.45);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 3.5F), at.add(0.0, 1.0, 0.0), 100, 0.9);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 2.5F), at.add(0.0, 0.3, 0.0), 170, 2.15);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.0F), at.add(0.0, 0.6, 0.0), 130, 1.5);
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
     * After the crash its crater smoulders while the blast dies down: columns of smoke climb out of it, fires lick at it
     * and a haze of smoke hangs over it.
     */
    private void smoulder(ServerLevel level) {
        Vec3 at = this.path.crash();
        double fade = 1.0 - (this.age - this.path.crashTick()) / BLAST_TICKS;
        if (fade <= 0.0) {
            return;
        }
        RandomSource random = this.owner.getRandom();
        double radius = Math.max(2.0, this.ability.value("craterRadius"));
        for (int k = 0; k < 2; k++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double far = Math.sqrt(random.nextDouble()) * radius * 0.8;
            double x = at.x + Math.cos(angle) * far;
            double z = at.z + Math.sin(angle) * far;
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, at.y - 0.5, z, 0, 0.0, 1.0, 0.0, 0.07);
            ParticleFx.send(level, ParticleTypes.FLAME, x, at.y - 0.3, z, 4, 0.5, 0.3, 0.5, 0.04);
        }
        ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 2 + (int) (6.0 * fade), radius * 0.5,
                0.8, radius * 0.5, 0.05);
        if (random.nextDouble() < 0.4 * fade) {
            ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y, at.z, 3, radius * 0.4, 0.2, radius * 0.4, 0.0);
        }
    }

    /**
     * A crater of {@code radius}: a bowl blown out of the ground at {@code at}, rough at its rim. Up to {@code debris} of
     * its blocks are hurled up and away and come down all round it; the rest are gone. Blocks harder than
     * {@code breakHardness}, blocks that hold something (chests and the like) and blocks he may not touch there all
     * stay; so does water. It is blown out a little at a time over the next few ticks, top first (see {@link Crater}),
     * so the server never stalls on it.
     */
    private void crater(ServerLevel level, Vec3 at, double radius, int debris) {
        double hardest = this.ability.value("breakHardness");
        if (hardest < 0.0 || radius <= 0.0) {
            return;
        }
        Crater crater = new Crater(this.owner, at, radius, debris, hardest);
        if (crater.tick(level, 0)) {
            Effects.start(level, crater);
        }
    }

    /**
     * A crater being blown out of the ground: a bowl of {@code radius} round {@code at}, rough at its rim, reaching up
     * over the ground too, through what stands on it. Each tick it gets through only so much of it, the top first, so
     * however big it is the server never stalls on it. Of the blocks near the top of the bowl, where the blast tears
     * the ground open, some are hurled up and away from all round it (up to {@code debris}); the rest are gone.
     */
    private static final class Crater implements Effect {
        // How much it may do in one tick: looking at a spot costs 1, one it may not blow away 3, blowing a block away
        // 12 and hurling one 30.
        private static final int WORK = 2400;
        private final ServerPlayer owner;
        private final Vec3 at;
        private final int top;
        private final double hardest;
        private final double hurlChance;
        private final RandomSource random;
        // Every spot of the bowl, the highest first (in a random order within each layer), and how far it has got.
        private final List<BlockPos> spots = new ArrayList<>();
        private int next;
        private int hurls;

        Crater(ServerPlayer owner, Vec3 at, double radius, int debris, double hardest) {
            this.owner = owner;
            this.at = at;
            this.hardest = hardest;
            this.random = owner.getRandom();
            this.hurls = Math.max(0, debris);
            // About as many blocks lie in the top three layers of the bowl as in three discs as wide as it.
            this.hurlChance = Math.min(1.0, this.hurls / Math.max(1.0, 3.0 * Math.PI * radius * radius));
            double depth = radius * 0.62;
            BlockPos middle = BlockPos.containing(at.x, at.y - 0.5, at.z);
            this.top = middle.getY() - 2;
            int reach = (int) Math.ceil(radius + 1.0);
            for (int dy = reach; dy >= -(int) Math.ceil(depth + 1.0); dy--) {
                int layer = this.spots.size();
                double down = dy < 0 ? dy / depth : dy / (radius * 0.85);
                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        // Rough at its rim: every column reaches a little further or less far.
                        double rough = 1.0 + 0.2 * (Noise.of(middle.getX() + dx, middle.getZ() + dz, 17) - 0.5);
                        double out = (dx * dx + dz * dz) / (radius * radius) + down * down;
                        if (out <= rough * rough) {
                            this.spots.add(middle.offset(dx, dy, dz));
                        }
                    }
                }
                for (int i = this.spots.size() - 1; i > layer; i--) {
                    Collections.swap(this.spots, i, layer + this.random.nextInt(i - layer + 1));
                }
            }
        }

        @Override
        public boolean tick(ServerLevel level, int age) {
            int work = 0;
            while (this.next < this.spots.size() && work < WORK) {
                BlockPos pos = this.spots.get(this.next++);
                work++;
                // Where nobody has the world loaded (he flew off), nothing is blown away.
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
                    continue;
                }
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0.0F || hardness > this.hardest || !BlockRules.mayBreak(level, this.owner, pos, state)) {
                    work += 2;
                    continue;
                }
                if (this.hurls > 0 && pos.getY() >= this.top && state.isCollisionShapeFullBlock(level, pos)
                        && this.random.nextDouble() < this.hurlChance) {
                    this.hurls--;
                    work += 29;
                    this.hurl(level, pos, state);
                } else {
                    work += 11;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            return this.next < this.spots.size();
        }

        /** A block of the bowl hurled up and away from its middle, to come down somewhere round it. */
        private void hurl(ServerLevel level, BlockPos pos, BlockState state) {
            FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
            block.dropItem = false;
            Vec3 out = new Vec3(pos.getX() + 0.5 - this.at.x, 0.0, pos.getZ() + 0.5 - this.at.z);
            out = out.lengthSqr() < 1.0E-4
                    ? new Vec3(this.random.nextDouble() - 0.5, 0.0, this.random.nextDouble() - 0.5)
                    : out.normalize();
            double speed = 0.45 + 0.55 * this.random.nextDouble();
            block.setDeltaMovement(out.x * speed, 0.7 + 0.8 * this.random.nextDouble(), out.z * speed);
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
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, from.subtract(0.0, HEIGHT * 3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? new Vec3(at.x, this.owner.getY(), at.z) : hit.getLocation();
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
        // Missiles still in flight break into solid pieces where they are (clients see to that when they are gone).
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
