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

public final class AirStrike extends AirStrikeMissiles {
    public static final int CALL_TICKS = 34;
    public static final int BLAST_TICKS = 90;
    public static final int BIG_MISSILE = 0;
    public static final int JET_MISSILE = 2;
    public static final int BIG_BLAST = 0;
    public static final int SMALL_BLAST = 1;
    public static final double GUN_X = 4.3;
    public static final double GUN_Y = -1.7;
    public static final double GUN_Z = 10.0;
    public static final double GUN_LENGTH = 6.4;
    public static final double BAY_Z = -0.6;
    public static final double BAY_LENGTH = 8.6;
    public static final double DROP_Y = -3.35;
    public static final double MISSILE_SCALE = 1.45;
    public static final double SMALL_MISSILE_SCALE = 0.62;
    public static final double MISSILE_NOSE = 2.3;
    public static final double PYLON_X = 3.3;
    public static final double PYLON_Y = -0.55;
    public static final double PYLON_Z = 0.2;
    public static final int RELOAD_TICKS = 16;
    public static final double ENGINE_X = 8.6;
    public static final double ENGINE_OUTER_X = 16.4;
    public static final double ENGINE_Y = 2.35;
    public static final double ENGINE_Z = 8.0;
    public static final double SENSOR_Y = -3.25;
    public static final double SENSOR_Z = 16.0;
    public static final double BULLET_SPEED = 7.0;
    public static final int SCAN_EVERY = 100;
    static final double HEIGHT = 55.0;
    private static final double LOWEST = 21.0;
    private static final double LOOK_REACH = 32.0;
    private static final double LOOK_AHEAD = 16.0;
    private static final int CLEAR_STEPS = 4;
    static final int IGNITE_EARLIEST = 9;
    public static final int IGNITE_LATEST = 24;
    public static final int SMALL_IGNITES = 3;

    private static final Map<UUID, AirStrike> ACTIVE = new HashMap<>();

    private final int id = PowerRing.newId();
    private boolean crashed;

    private AirStrike(ServerPlayer owner, CharacterAbility ability, PlanePath path) {
        super(owner, ability, path);
    }

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

    private static double room(ServerLevel level, ServerPlayer owner) {
        Vec3 eye = owner.getEyePosition();
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(eye, eye.add(0.0, HEIGHT + 8.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        return hit.getType() == HitResult.Type.MISS ? HEIGHT : eye.distanceTo(hit.getLocation()) - 8.0;
    }

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
        double before = PlanePath.SPEED * (PlanePath.FORM * 0.5 + attack * 0.5);
        Vec3 start = new Vec3(middle.x, eye.y + height, middle.z).subtract(way.scale(before));
        if (LoadedWorld.clip(level, new ClipContext(eye, start, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner)).getType() != HitResult.Type.MISS) {
            start = eye.add(0.0, height, 0.0);
        }
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
        // Rounded down the way clients get it, so both work out the very same crash.
        return new PlanePath(start, way, drop, attack, Math.max(0.05, Math.floor(end * 100.0) / 100.0));
    }

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

    private static Vec3[] reaches(PlanePath path, double t) {
        Vec3[] at = new Vec3[PlanePath.REACHES.length];
        for (int p = 0; p < at.length; p++) {
            double[] part = PlanePath.REACHES[p];
            at[p] = path.point(t, part[0], part[1], part[2]);
        }
        return at;
    }

    static boolean calling(ServerPlayer player) {
        AirStrike strike = ACTIVE.get(player.getUUID());
        return strike != null && strike.age < CALL_TICKS;
    }

    public static int left(ServerPlayer player) {
        AirStrike strike = ACTIVE.get(player.getUUID());
        return strike == null ? 0 : Math.max(0, (int) Math.ceil(strike.path.crashTick()) + BLAST_TICKS - strike.age);
    }

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
                this.end(level);
                return false;
            }
            this.dropFlying(level);
        }
        this.age++;
        double dive = this.path.diveTick();
        if (this.age == 8) {
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

    private void send(ServerLevel level) {
        Vec3 at = this.path.at(Math.min(this.age, this.path.crashTick()));
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.path.start(), this.path.way(),
                        (float) this.path.drop(), 1.0F, (float) this.path.attack(), true, ConstructPayload.PLANE,
                        (int) Math.round(this.path.end() * 100.0), this.age, null));
    }
}
