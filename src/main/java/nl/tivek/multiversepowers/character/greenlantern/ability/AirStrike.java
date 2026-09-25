package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effects;
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
public final class AirStrike extends AirStrikeMissiles {
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
    static final double HEIGHT = 55.0;
    private static final double LOWEST = 21.0;
    private static final double LOOK_REACH = 32.0;
    private static final double LOOK_AHEAD = 16.0;
    // How many times along one tick of its dive a part of the plane that is inside something looks again for where it
    // comes out in the clear (see diveStrikes).
    private static final int CLEAR_STEPS = 4;
    // A big missile drops out of the hatch (see PlanePath.dropsOut), falls, and fires its motor between these two ticks
    // after it dropped.
    static final int IGNITE_EARLIEST = 9;
    /** The latest a big missile's motor fires, in ticks after it dropped out of the hatch. */
    public static final int IGNITE_LATEST = 24;
    /** The tick after a jet fired its small missile that the missile's motor fires. */
    public static final int SMALL_IGNITES = 3;

    private static final Map<UUID, AirStrike> ACTIVE = new HashMap<>();

    private final int id = PowerRing.newId();
    private boolean crashed;

    private AirStrike(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        super(owner, ability, path);
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
        if (GiantHands.waving(owner)) {
            // The ring hand waves a giant hand up: it does nothing else meanwhile.
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
                double part = diveStrikes(level, last[p], next[p]);
                if (part >= 0.0) {
                    end = Math.min(end, (k - 1 + part) / steps);
                }
            }
            last = next;
        }
        // Rounded down the way clients get it, so both work out the very same crash, and it strikes a hair before any
        // part would sink into the ground.
        return new PlanePath(start, way, drop, attack, Math.max(0.05, Math.floor(end * 100.0) / 100.0));
    }

    /**
     * How far along its way from {@code from} to {@code to} a part of the diving plane strikes something (0 to 1), or
     * -1 when it strikes nothing. A part already inside something (a wingtip through a treetop as it drones on)
     * strikes only what it meets once it is out in the clear again, looked for from a little further on each time.
     */
    private static double diveStrikes(ServerLevel level, Vec3 from, Vec3 to) {
        double length = Math.max(1.0E-6, from.distanceTo(to));
        for (int s = 0; s < CLEAR_STEPS; s++) {
            Vec3 start = from.lerp(to, (double) s / CLEAR_STEPS);
            BlockHitResult strike = LoadedWorld.clip(level, new ClipContext(start, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (strike.getType() == HitResult.Type.MISS) {
                return -1.0;
            }
            if (!strike.isInside()) {
                return from.distanceTo(strike.getLocation()) / length;
            }
        }
        return -1.0;
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
        if (!PowerRing.fuels(this.owner, level)) {
            if (!this.crashed) {
                // His will no longer holds it: it breaks apart in the air (clients see to that when it is gone).
                this.end(level);
                return false;
            }
            // Gone after the crash: what still flies breaks up where it is and hurts no one in his name.
            this.dropFlying(level);
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

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
        this.dropFlying(level);
        for (Scan scan : this.scans) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(scan.id));
        }
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
}
