package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * Giant Fist: the ring makes a fist of hard light beside Green Lantern, on his right (the hand that
 * attacks). While he keeps the key down the ring charges it: it grows slowly from one block across up to
 * nearly six (in about 4 seconds by default), and every half second of it costs power. It keeps to a spot
 * around him that has room for it: on his right, and when that would put it through a wall or the ground,
 * higher up, above his head or on his left, flowing smoothly from one spot to the next while it keeps
 * charging. When he lets go the ring pays for it and it flies: it glides in onto his line of sight without
 * turning, and from then on he steers it with his eyes, its middle always right under his crosshair, further out
 * every tick wherever he looks. It rams every creature in its way (each one once: a heavy hit that throws it
 * far, and the bigger the fist the harder it hits), smashes the soft blocks it touches and goes straight through
 * everything harder, until the end of its range. Then it falls apart into green light.
 */
final class GiantFist implements SpellEffect {
    // The smallest fist, in blocks across: what a tap of the key gives.
    private static final double MIN_SIZE = 1.0;
    // Ticks it takes to fade in beside you. There is no build-up to watch: it is simply there.
    private static final int APPEAR_TICKS = 3;
    // The ring charges in steps of half a second, and every step costs power.
    private static final int STEP_TICKS = 10;
    // Ticks between two hums while it charges.
    private static final int HUM_TICKS = 6;
    // Ticks it takes to fall apart.
    private static final int FADE_TICKS = 6;
    // Blocks per tick in flight, out along your line of sight. It flies on from your eyes, so however fast you go
    // yourself, you never catch up with it.
    private static final double SPEED = 1.3;
    // Once let go it glides in onto your line of sight (see ConstructPath) over this many times as far as it hung
    // beside that line, and over at least MIN_JOIN blocks. What you aim at as you let go that is closer than that,
    // it is on the line by the time it gets there, but it never takes less than NEAREST_JOIN blocks to get onto it.
    private static final double JOIN = 1.3;
    private static final double MIN_JOIN = 1.5;
    private static final double NEAREST_JOIN = 1.0;
    // Its shape, as parts of how wide it is: how tall, from its middle to the front of the knuckles, and
    // from its middle to the back of the wrist (the forearm behind that is only a fading trail of light).
    private static final double HEIGHT = 0.65;
    private static final double FRONT = 0.5;
    private static final double BACK = 0.7;
    // A creature is hit when it comes within half the fist's width, and this much more, of its middle line.
    private static final double HIT_MARGIN = 0.4;
    // A creature that is hit flies this much upwards on top of the push.
    private static final double LIFT = 0.3;
    // Anyone this close sees it.
    private static final double VIEW_RANGE = 128.0;
    // While you charge it, it only tips this far up or down with your view, so it keeps to its spot.
    private static final float MAX_HELD_PITCH = 25.0F;
    // How much of the way to a new spot it covers each tick: it flows there instead of jumping.
    private static final double FLOW = 0.3;
    // A better spot that came free must stay free this many ticks before the fist moves back to it, so it
    // does not dart to and fro along a wall.
    private static final int RETURN_TICKS = 10;
    // How deep a block may poke into the fist before it is in the way: the fist's edges are only light.
    private static final double ROOM = 0.3;
    // Counting blocks in the way stops here; more than this is simply "very much in the way".
    private static final int MAX_COUNT = 4096;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    // The fist each Green Lantern charges right now, until they let go of the key.
    private static final Map<UUID, GiantFist> HELD = new HashMap<>();

    private enum Phase {
        HOLD, FLY, FADE
    }

    /** The spots around you where it can charge, the best one first. */
    private enum Spot {
        RIGHT, RIGHT_HIGH, ABOVE, LEFT, LEFT_HIGH
    }

    private static final Spot[] SPOTS = Spot.values();

    private final ServerPlayer owner;
    private final int id = PowerRing.newId();
    private final float baseDamage;
    private final float fullDamage;
    private final double breakHardness;
    private final int maxBroken;
    private final double range;
    private final double knockback;
    private final double maxSize;
    private final int chargeTicks;
    private final float baseCost;
    private final float fullCost;
    // Entity ids of everything it hit already: every creature is hit only once.
    private final Set<Integer> hit = new HashSet<>();
    // How many blocks it smashed so far.
    private int broken;
    private Phase phase = Phase.HOLD;
    private int phaseAge;
    // Ticks it has been charged: 0 = the smallest fist, chargeTicks = the biggest.
    private int charged;
    // What letting go right now would cost; the ring pays it when the fist flies.
    private float pending;
    // True once the ring could not pay for the next step, so that is said only once.
    private boolean drained;
    private Spot spot = Spot.RIGHT;
    // How long a better spot has been free (see RETURN_TICKS), in checks.
    private int waiting;
    // Where it hangs around your eyes: x to your right, y up, z ahead. It flows towards its spot.
    private Vec3 offset;
    private Vec3 center;
    private Vec3 facing;
    private double travelled;
    // The way it flies once let go: steered by your eyes (see ConstructPath). Clients get it too, and move the fist
    // along it by themselves.
    private ConstructPath path;
    // Where on your line of sight it was headed last tick, and the way that point moved since: what it hits is
    // thrown that way, on ahead, or aside when you swing it round with your view.
    private Vec3 onSight;
    private Vec3 push;

    private GiantFist(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.baseDamage = ability.getDamage();
        this.fullDamage = Math.max(this.baseDamage, (float) ability.value("fullChargeDamage"));
        this.breakHardness = ability.value("breakHardness");
        this.maxBroken = (int) Math.round(ability.value("maxBlocksBroken"));
        this.range = ability.value("rangeBlocks");
        this.knockback = ability.value("knockback");
        this.maxSize = Math.max(MIN_SIZE, ability.value("maxSize"));
        this.chargeTicks = Math.max(1, (int) Math.round(ability.value("chargeSeconds") * 20.0));
        this.baseCost = (float) ability.value("powerCost");
        this.fullCost = Math.max(this.baseCost, (float) ability.value("fullChargePowerCost"));
        this.pending = this.baseCost;
        this.facing = this.heldFacing();
        this.offset = this.spotOffset(this.spot, MIN_SIZE);
        this.center = this.worldPoint(this.offset);
    }

    /**
     * The key goes down: the fist appears beside you at once, as long as the ring can pay for the smallest
     * one and you are not recharging it.
     */
    static boolean launch(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (PowerRing.power(owner) + 1.0E-4F < (float) ability.value("powerCost")) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        GiantFist fist = new GiantFist(owner, ability);
        // The ring hand makes the fist: a beam it was pouring out stops.
        LightBeam.stop(owner);
        // A fist you still held (its key release got lost) lets go by itself: it sees it is not held any more.
        HELD.put(owner.getUUID(), fist);
        SpellCasting.start(level, fist);
        owner.swing(InteractionHand.MAIN_HAND, true);
        fist.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.7F);
        fist.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.3F);
        PowerRing.sync(owner);
        fist.send(level);
        return true;
    }

    /**
     * The key comes up: the fist flies off.
     *
     * @return true when there was a fist to let go of, so the cooldown starts now
     */
    static boolean letGo(ServerPlayer owner) {
        return HELD.remove(owner.getUUID()) != null;
    }

    /** True while this player charges a fist. */
    static boolean holding(ServerPlayer owner) {
        return HELD.containsKey(owner.getUUID());
    }

    /** What the fist this player charges would cost if they let go now; 0 when they charge none. */
    static float pending(ServerPlayer owner) {
        GiantFist fist = HELD.get(owner.getUUID());
        return fist == null ? 0.0F : fist.pending;
    }

    /** The server stops: nobody holds anything any more. */
    static void clear() {
        HELD.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        // Without the will behind it, a construct falls apart.
        if (this.phase != Phase.FADE && !PowerRing.fuels(this.owner, level)) {
            this.fall(level);
        }
        this.phaseAge++;
        switch (this.phase) {
            case HOLD -> this.hold(level);
            case FLY -> this.fly(level);
            case FADE -> {
                if (this.phaseAge >= FADE_TICKS) {
                    PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
                    return false;
                }
            }
        }
        this.send(level);
        return true;
    }

    /**
     * As long as you hold the key it charges beside you, in the best spot around you that has room for it.
     * Let go and it flies.
     */
    private void hold(ServerLevel level) {
        boolean held = HELD.get(this.owner.getUUID()) == this;
        // Crouch while you hold it and it simply falls apart: no shot, no cost and no cooldown either.
        if (held && this.owner.isShiftKeyDown()) {
            this.fall(level);
            return;
        }
        if (held) {
            this.charge(level);
        }
        this.facing = this.heldFacing();
        double size = this.grownSize();
        // Every other tick is plenty to look for room: the fist flows from spot to spot anyway.
        if (this.phaseAge % 2 == 1) {
            this.findRoom(level, size);
        }
        this.offset = this.offset.lerp(this.spotOffset(this.spot, size), FLOW);
        this.center = this.worldPoint(this.offset);
        if (!held) {
            this.shoot(level);
        }
    }

    /** One tick more charge, as long as the ring can pay for it: a hum that rises, and a chime once full. */
    private void charge(ServerLevel level) {
        if (this.charged >= this.chargeTicks) {
            return;
        }
        if (this.costAt(this.charged + 1) > PowerRing.power(this.owner) + 1.0E-4F) {
            if (!this.drained) {
                this.drained = true;
                PowerRing.tell(this.owner, "drained");
                this.sound(level, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
            }
            return;
        }
        this.charged++;
        if (this.charged >= this.chargeTicks) {
            this.sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 1.5F);
            this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
            SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.4F), this.center, 30, 0.25);
        } else if (this.charged % HUM_TICKS == 1) {
            this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, (float) (0.6 + this.charge()));
        }
        float cost = this.costAt(this.charged);
        if (cost != this.pending) {
            this.pending = cost;
            PowerRing.sync(this.owner);
        }
    }

    /**
     * What the fist costs after this many ticks of charging: the base cost, and an equal share of the rest for
     * every half second that is complete, so a full charge costs exactly the full cost.
     */
    private float costAt(int ticks) {
        if (ticks >= this.chargeTicks) {
            return this.fullCost;
        }
        double steps = (double) this.chargeTicks / STEP_TICKS;
        double done = Math.min(1.0, (ticks / STEP_TICKS) / steps);
        return (float) (this.baseCost + (this.fullCost - this.baseCost) * done);
    }

    /** The way it points while you charge it: where you look, but tipped no further than MAX_HELD_PITCH. */
    private Vec3 heldFacing() {
        return Vec3.directionFromRotation(Mth.clamp(this.owner.getXRot(), -MAX_HELD_PITCH, MAX_HELD_PITCH),
                this.owner.getYRot());
    }

    /**
     * Where a spot is for a fist this wide, around your eyes (x to your right, y up, z ahead). Beside you it
     * stands on the ground rather than sinking into it; the high spots hang from the height of your head up,
     * and above your head it floats a little ahead, so you still see it.
     */
    private Vec3 spotOffset(Spot spot, double size) {
        double half = HEIGHT * size * 0.5;
        // Far enough out and ahead that even its forearm stays clear of your eyes.
        double side = 1.0 + 0.62 * size;
        double ahead = 2.3 + 0.1 * size;
        double low = Math.max(-0.35, 0.2 - this.owner.getEyeHeight() + half);
        double high = Math.max(low, 0.2 + half);
        return switch (spot) {
            case RIGHT -> new Vec3(side, low, ahead);
            case RIGHT_HIGH -> new Vec3(side, high, ahead);
            case ABOVE -> new Vec3(0.0, 0.9 + 0.1 * size + half, 0.2 * size);
            case LEFT -> new Vec3(-side, low, ahead);
            case LEFT_HIGH -> new Vec3(-side, high, ahead);
        };
    }

    /** A point around your eyes out in the world (x to your right, y up, z ahead); it turns along with you. */
    private Vec3 worldPoint(Vec3 local) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.owner.getYRot());
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return this.owner.getEyePosition().add(right.scale(local.x)).add(0.0, local.y, 0.0)
                .add(ahead.scale(local.z));
    }

    /**
     * Picks the spot to charge in: the first one in {@link Spot}'s order with no block in the way, or else the
     * one with the fewest. A spot that is in the way is left at once; a better one that came free is only
     * taken back once it stays free a moment.
     */
    private void findRoom(ServerLevel level, double size) {
        int best = 0;
        int bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < SPOTS.length && bestCount > 0; i++) {
            int count = this.blocksIn(level, SPOTS[i], size, Math.min(bestCount, MAX_COUNT));
            if (count < bestCount) {
                best = i;
                bestCount = count;
            }
        }
        if (SPOTS[best] == this.spot) {
            this.waiting = 0;
            return;
        }
        int here = this.blocksIn(level, this.spot, size, MAX_COUNT);
        if (here == 0) {
            if (++this.waiting < RETURN_TICKS / 2) {
                return;
            }
        } else if (bestCount > 0 && here <= bestCount + 2 + bestCount / 2) {
            // Nowhere has room: only move for a spot that is clearly better, not for a block or two.
            this.waiting = 0;
            return;
        }
        this.waiting = 0;
        this.spot = SPOTS[best];
        this.sound(level, SoundEvents.PHANTOM_FLAP, 0.5F, 1.4F);
    }

    /**
     * How many solid blocks the fist would be in at that spot, counting no further than {@code limit}. Air and
     * everything you can walk through never count, and a block has to poke into it a little ({@link #ROOM}).
     */
    private int blocksIn(ServerLevel level, Spot spot, double size, int limit) {
        Vec3 forward = this.facing;
        Vec3 right = forward.cross(UP);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 up = right.cross(forward);
        double halfWidth = size * 0.5 + 0.5 - ROOM;
        double halfHeight = HEIGHT * size * 0.5 + 0.5 - ROOM;
        double halfDepth = (FRONT + BACK) * size * 0.5 + 0.5 - ROOM;
        // The fist reaches further back (to its wrist) than forward (to its knuckles).
        Vec3 middle = this.worldPoint(this.spotOffset(spot, size)).add(forward.scale((FRONT - BACK) * size * 0.5));
        double reachX = Math.abs(right.x) * halfWidth + Math.abs(up.x) * halfHeight + Math.abs(forward.x) * halfDepth;
        double reachY = Math.abs(right.y) * halfWidth + Math.abs(up.y) * halfHeight + Math.abs(forward.y) * halfDepth;
        double reachZ = Math.abs(right.z) * halfWidth + Math.abs(up.z) * halfHeight + Math.abs(forward.z) * halfDepth;
        int minX = Mth.floor(middle.x - reachX);
        int maxX = Mth.floor(middle.x + reachX);
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(middle.y - reachY));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(middle.y + reachY));
        int minZ = Mth.floor(middle.z - reachZ);
        int maxZ = Mth.floor(middle.z + reachZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                    // Most of the room around you is open air: whole sections of it are skipped at once.
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    for (int x = Math.max(minX, chunkX << 4); x <= Math.min(maxX, (chunkX << 4) + 15); x++) {
                        for (int y = Math.max(minY, sectionY << 4); y <= Math.min(maxY, (sectionY << 4) + 15); y++) {
                            for (int z = Math.max(minZ, chunkZ << 4); z <= Math.min(maxZ, (chunkZ << 4) + 15); z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state.isAir()) {
                                    continue;
                                }
                                double dx = x + 0.5 - middle.x;
                                double dy = y + 0.5 - middle.y;
                                double dz = z + 0.5 - middle.z;
                                if (Math.abs(dx * right.x + dy * right.y + dz * right.z) > halfWidth
                                        || Math.abs(dx * up.x + dy * up.y + dz * up.z) > halfHeight
                                        || Math.abs(dx * forward.x + dy * forward.y + dz * forward.z) > halfDepth) {
                                    continue;
                                }
                                if (!state.getCollisionShape(level, pos.set(x, y, z)).isEmpty() && ++count >= limit) {
                                    return count;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Off it goes, under your crosshair: it charges beside you, but you steer it with your eyes. The ring pays for it
     * now.
     */
    private void shoot(ServerLevel level) {
        this.phase = Phase.FLY;
        this.phaseAge = 0;
        PowerRing.setPower(this.owner, PowerRing.power(this.owner) - this.costAt(this.charged));
        this.plan(level);
        this.owner.swing(InteractionHand.MAIN_HAND, true);
        // A bigger fist sounds heavier.
        float deeper = (float) (0.3 * this.charge());
        this.sound(level, SoundEvents.BREEZE_SHOOT, 1.0F, 0.7F - deeper);
        this.sound(level, SoundEvents.MACE_SMASH_AIR, 1.0F, 0.8F - deeper);
    }

    /**
     * Works out the way it flies. It charges beside you, but the middle of the fist must go exactly where your
     * crosshair points: so it glides in onto your line of sight over its first few blocks, and from then on stays on
     * that line wherever you look, further out every tick. It points the way you look all the while, so it never
     * turns aside; only when you look further up or down than it tips while you charge it, it tips the rest of the
     * way as it glides in.
     */
    private void plan(ServerLevel level) {
        ConstructPath.Sight sight = this.sight();
        Vec3 offset = sight.local(this.center);
        double aside = Math.sqrt(offset.x * offset.x + offset.y * offset.y);
        double aimed = sight.local(this.aimedAt(level)).z - offset.z;
        double join = Math.max(NEAREST_JOIN, Math.min(aimed, Math.max(MIN_JOIN, JOIN * aside)));
        float pitch = this.owner.getXRot();
        double tilt = Math.toRadians(pitch - Mth.clamp(pitch, -MAX_HELD_PITCH, MAX_HELD_PITCH));
        this.path = ConstructPath.steered(offset, tilt, join, SPEED, this.range);
        this.facing = this.path.way(0.0, sight);
        this.onSight = this.path.onSight(0.0, sight);
        this.push = sight.forward();
    }

    /** Your eyes and the way you look right now: the fist on its way stays on your line of sight. */
    private ConstructPath.Sight sight() {
        return ConstructPath.Sight.of(this.owner.getEyePosition(), this.owner.getYRot(), this.owner.getXRot());
    }

    /** What you aim at: the creature in your sights, else the block, else the end of the range. */
    private Vec3 aimedAt(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 end = eye.add(this.owner.getLookAngle().scale(this.range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.owner));
        Vec3 target = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB search = this.owner.getBoundingBox().expandTowards(target.subtract(eye)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(this.owner, eye, target, search,
                e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), eye.distanceToSqr(target));
        return entity != null ? entity.getEntity().getBoundingBox().getCenter() : target;
    }

    /** Hard light: it smashes the soft blocks in its way and goes straight through everything else. */
    private void fly(ServerLevel level) {
        Vec3 from = this.center;
        // Counted from the ticks it has flown, the way every client counts it too.
        this.travelled = this.path.travelled(this.phaseAge);
        ConstructPath.Sight sight = this.sight();
        Vec3 to = this.path.along(this.travelled, sight);
        this.facing = this.path.way(this.travelled, sight);
        Vec3 onSight = this.path.onSight(this.travelled, sight);
        if (onSight.distanceToSqr(this.onSight) > 1.0E-8) {
            this.push = onSight.subtract(this.onSight).normalize();
        }
        this.onSight = onSight;
        this.ram(level, from, to);
        this.smash(level, from, to);
        this.center = to;
        SpellFx.cloud(level, SpellFx.dust(PowerRing.GREEN, 1.4F), from, 3 + (int) this.grownSize(),
                0.3 * this.grownSize(), 0.0);
        if (this.travelled >= this.range - 1.0E-3) {
            this.fall(level);
        }
    }

    /** Every creature the fist passed on its way from {@code from} to {@code to} takes the hit. */
    private void ram(ServerLevel level, Vec3 from, Vec3 to) {
        double size = this.grownSize();
        double radius = size * 0.5 + HIT_MARGIN;
        Vec3 knuckles = to.add(this.facing.scale(FRONT * size));
        AABB area = new AABB(from, knuckles).inflate(radius + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> PowerRing.canHit(this.owner, entity))) {
            if (this.hit.contains(target.getId())) {
                continue;
            }
            AABB body = target.getBoundingBox();
            if (!body.inflate(radius).contains(closest(from, knuckles, body.getCenter()))) {
                continue;
            }
            this.hit.add(target.getId());
            this.punch(level, target);
        }
    }

    /**
     * The blocks the fist smashes on its way from {@code from} to {@code to}: everything soft enough that it
     * touches breaks and drops what it would drop, up to {@code maxBlocksBroken} blocks for the whole flight.
     * Harder blocks, blocks that hold something (chests and the like) and blocks its owner is not allowed to
     * touch all stay where they are, and the fist goes straight through them.
     */
    private void smash(ServerLevel level, Vec3 from, Vec3 to) {
        if (this.breakHardness < 0.0 || this.broken >= this.maxBroken) {
            return;
        }
        double size = this.grownSize();
        double radius = size * 0.5;
        Vec3 knuckles = to.add(this.facing.scale(FRONT * size));
        AABB area = new AABB(from, knuckles).inflate(radius);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            if (this.broken >= this.maxBroken) {
                return;
            }
            Vec3 middle = pos.getCenter();
            if (closest(from, knuckles, middle).distanceToSqr(middle) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > this.breakHardness || !level.mayInteract(this.owner, pos)) {
                continue;
            }
            level.destroyBlock(pos, true, this.owner);
            this.broken++;
        }
    }

    /** The point on the line from {@code a} to {@code b} that is closest to {@code p}. */
    private static Vec3 closest(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double length = ab.lengthSqr();
        double t = length < 1.0E-9 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / length, 0.0, 1.0);
        return a.add(ab.scale(t));
    }

    /**
     * A heavy hit that counts as your attack, and a push the way the fist goes: straight on, or aside when you swing it
     * round.
     */
    private void punch(ServerLevel level, LivingEntity target) {
        // Hits in quick succession all land.
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(this.owner), this.damage());
        // Knockback resistance (netherite armour) still counts.
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(new Vec3(this.push.x * this.knockback,
                Math.max(0.0, this.push.y * this.knockback) + LIFT, this.push.z * this.knockback)
                .scale(1.0 - resist));
        target.hasImpulse = true;
        // Players move themselves on their own client, so they have to be told about the push.
        target.hurtMarked = true;
        Vec3 at = target.getBoundingBox().getCenter();
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.2F), at, 16, 0.3);
        SpellFx.cloud(level, ParticleTypes.CRIT, at, 10, 0.3, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    /** From now on it hits nothing and fades out, breaking up into green light. */
    private void fall(ServerLevel level) {
        boolean wasHeld = HELD.remove(this.owner.getUUID(), this);
        this.phase = Phase.FADE;
        this.phaseAge = 0;
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 1.2F), this.center, 24, 0.15);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.5F);
        // Nothing is paid for a fist that never flew; the HUD stops showing what it would have cost.
        if (wasHeld) {
            PowerRing.sync(this.owner);
        }
    }

    /** How far it is charged: 0 = not at all, 1 = as far as it goes. */
    private double charge() {
        return (double) this.charged / this.chargeTicks;
    }

    /** How wide it is, in blocks: from {@link #MIN_SIZE} up to the biggest fist as it charges. */
    private double grownSize() {
        return MIN_SIZE + (this.maxSize - MIN_SIZE) * this.charge();
    }

    /** What a hit does: the wider the fist, the harder, from the normal damage up to a full charge's. */
    private float damage() {
        return Mth.lerp((float) this.charge(), this.baseDamage, this.fullDamage);
    }

    /** How wide it is drawn: it swells a little while it falls apart. */
    private float size() {
        float size = (float) this.grownSize();
        return this.phase == Phase.FADE ? size * (1.0F + 0.15F * this.phaseAge / FADE_TICKS) : size;
    }

    /** How solid it is: it fades in beside you, and out again as it falls apart. */
    private float solid() {
        return switch (this.phase) {
            case HOLD -> Math.min(1.0F, (float) this.phaseAge / APPEAR_TICKS);
            case FLY -> 1.0F;
            case FADE -> 1.0F - (float) this.phaseAge / FADE_TICKS;
        };
    }

    private void send(ServerLevel level) {
        // While you hold it, where it hangs around your eyes goes out instead of a point in the world: every
        // client hangs it on you itself, so it keeps up with you however fast you turn or fly.
        boolean held = this.phase == Phase.HOLD;
        // On its way it goes out with its path and how long it has flown: every client moves it along that path by
        // its own clock (see ClientConstructs), so it glides instead of jumping from one update to the next.
        boolean flying = this.phase == Phase.FLY;
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), held ? this.offset : this.center, this.facing,
                        this.size(), this.solid(), (float) this.charge(), held, ConstructPayload.FIST, 0,
                        flying ? this.phaseAge : 0, flying ? this.path : null));
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.center.x, this.center.y, this.center.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
