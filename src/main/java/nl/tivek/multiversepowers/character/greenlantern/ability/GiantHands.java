package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

public final class GiantHands implements Effect {
    public static final double SCALE = 1.0;
    public static final int WAVE_TICKS = 18;
    private static final int AT_ONCE = 5;
    private static final int TRIES = 8;
    static final double GRAB_WIDE = 2.0;
    static final double GRAB_TALL = 3.2;
    private static final int[] CHANCE = { 25, 22, 12, 22, 19, 24 };
    private static final double[] PAIR_TURNS = { 0.0, Math.PI * 0.5, -Math.PI * 0.5, Math.PI };
    private static final int WAIT_TICKS = 80;
    private static final int LOOK_AGAIN = 5;
    private static final double LEAST_AWAY = 0.3;

    private static final Map<UUID, GiantHands> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_MOVES = new HashMap<>();
    static final Map<Integer, GiantHand> GRABBED = new HashMap<>();

    static {
        HeldMobs.addHolder(entity -> GRABBED.containsKey(entity.getId()));
    }

    final ServerPlayer owner;
    final CharacterAbility ability;
    private final List<GiantHand> hands = new ArrayList<>();
    private final List<LivingEntity> targets;
    private final Set<LivingEntity> missed = new HashSet<>();
    private final int count;
    private final int every;
    private int called;
    private int waited;
    private int stuck;
    private int since;
    private int lastMove;
    private boolean crowded;
    @Nullable
    private List<GiantHand> taken;
    @Nullable
    private GiantHand latest;

    private GiantHands(ServerPlayer owner, CharacterAbility ability, List<LivingEntity> targets) {
        this.owner = owner;
        this.ability = ability;
        this.targets = targets;
        int fewest = Math.max(1, ability.intValue("fewestHands"));
        int most = Math.max(fewest, ability.intValue("mostHands"));
        this.count = fewest + owner.getRandom().nextInt(most - fewest + 1);
        this.every = Math.max(1, ability.intValue("handTicks"));
        this.lastMove = LAST_MOVES.getOrDefault(owner.getUUID(), -1);
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
        if (AirStrike.calling(owner)) {
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        List<LivingEntity> found = new ArrayList<>();
        double reach = ability.value("radiusBlocks");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                owner.getBoundingBox().inflate(reach), entity -> fair(owner, entity))) {
            found.add(living);
        }
        if (found.isEmpty()) {
            PowerRing.tell(owner, "hands_none");
            return false;
        }
        GiantHands storm = new GiantHands(owner, ability, found);
        if (!storm.call(level, Integer.MAX_VALUE)) {
            PowerRing.tell(owner, "hands_none");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        ACTIVE.put(owner.getUUID(), storm);
        Effects.start(level, storm);
        PowerRing.tell(owner, "hands");
        storm.sound(level, owner.getEyePosition(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.3F);
        storm.sound(level, owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
        return true;
    }

    static boolean waving(ServerPlayer player) {
        GiantHands storm = ACTIVE.get(player.getUUID());
        return storm != null && storm.latest != null && storm.latest.t < WAVE_TICKS;
    }

    public static void clear() {
        for (GiantHands storm : ACTIVE.values()) {
            for (GiantHand hand : storm.hands) {
                hand.letGo();
            }
        }
        ACTIVE.clear();
        LAST_MOVES.clear();
        GRABBED.clear();
    }

    static boolean fair(ServerPlayer owner, LivingEntity living) {
        return PowerRing.canHit(owner, living) && Factions.hostile(owner, living);
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            for (GiantHand hand : this.hands) {
                hand.end(level);
            }
            return false;
        }
        boolean fuels = PowerRing.fuels(this.owner, level);
        this.since++;
        if (fuels && this.called < this.count && !AirStrike.calling(this.owner) && this.ready()) {
            int before = this.called;
            boolean more = this.call(level, TRIES);
            // Waiting for room never gives up: the hands in the way always go in the end.
            if (this.called > before) {
                this.waited = 0;
                this.stuck = 0;
                this.crowded = false;
            } else if (!this.crowded && ++this.stuck > WAIT_TICKS || !more && this.targets.isEmpty()) {
                this.called = this.count;
            } else if (!more && ++this.waited % LOOK_AGAIN == 0) {
                this.missed.clear();
                this.crowded = false;
            }
        }
        this.hands.removeIf(hand -> hand.tick(level, fuels));
        if (this.hands.isEmpty() && (this.called >= this.count || !fuels)) {
            ACTIVE.remove(this.owner.getUUID(), this);
            return false;
        }
        return true;
    }

    private boolean ready() {
        return this.hands.size() < AT_ONCE && this.since >= this.every;
    }

    private int handsOn(LivingEntity living) {
        int on = 0;
        for (GiantHand hand : this.hands) {
            if (hand.target == living) {
                on++;
            }
        }
        return on;
    }

    private boolean call(ServerLevel level, int tries) {
        this.taken = null;
        double reach = this.ability.value("radiusBlocks");
        this.targets.removeIf(living -> !living.isAlive() || living.level() != level
                || !fair(this.owner, living) || Math.abs(living.getX() - this.owner.getX()) > reach + 4.0
                || Math.abs(living.getY() - this.owner.getY()) > reach + 4.0
                || Math.abs(living.getZ() - this.owner.getZ()) > reach + 4.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.owner.getBoundingBox().inflate(reach), entity -> fair(this.owner, entity))) {
            if (!this.targets.contains(living)) {
                this.targets.add(living);
            }
        }
        this.missed.retainAll(this.targets);
        List<LivingEntity> turns = new ArrayList<>();
        for (LivingEntity living : this.targets) {
            if (!this.missed.contains(living)) {
                turns.add(living);
            }
        }
        turns.sort(Comparator.comparingInt(this::handsOn)
                .thenComparingDouble(living -> living.distanceToSqr(this.owner)));
        for (LivingEntity target : turns.subList(0, Math.min(tries, turns.size()))) {
            int move = this.pick(target, true);
            GiantHand hand = this.spawn(level, target, move);
            if (hand == null && move == HandPose.AXE) {
                move = this.pick(target, false);
                hand = this.spawn(level, target, move);
            }
            if (hand == null) {
                this.missed.add(target);
                continue;
            }
            this.missed.clear();
            this.hands.add(hand);
            this.called++;
            this.since = 0;
            this.latest = hand;
            this.lastMove = move;
            LAST_MOVES.put(this.owner.getUUID(), move);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.5F);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            return true;
        }
        return turns.size() > tries;
    }

    private int pick(LivingEntity target, boolean pair) {
        RandomSource random = this.owner.getRandom();
        boolean grabbable = target.getBbWidth() <= GRAB_WIDE && target.getBbHeight() <= GRAB_TALL
                && !HeldMobs.isHeldByAnyone(target) && !LightBubble.trapped(target);
        int total = 0;
        for (int move = 0; move < HandPose.MOVES; move++) {
            if (this.may(move, grabbable, pair)) {
                total += CHANCE[move];
            }
        }
        int roll = random.nextInt(total);
        for (int move = 0; move < HandPose.MOVES; move++) {
            if (!this.may(move, grabbable, pair)) {
                continue;
            }
            roll -= CHANCE[move];
            if (roll < 0) {
                return move;
            }
        }
        return HandPose.SMACK;
    }

    private boolean may(int move, boolean grabbable, boolean pair) {
        return move != this.lastMove && (move != HandPose.GRAB || grabbable) && (move != HandPose.AXE || pair);
    }

    @Nullable
    private GiantHand spawn(ServerLevel level, LivingEntity target, int move) {
        Vec3 away = new Vec3(target.getX() - this.owner.getX(), 0.0, target.getZ() - this.owner.getZ());
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.owner.getYRot()) : away.normalize();
        if (move == HandPose.AXE) {
            return this.pairFor(level, target, away);
        }
        RandomSource random = this.owner.getRandom();
        double side = random.nextBoolean() ? 1.0 : -1.0;
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 reach = away;
            int variant = move;
            if (move == HandPose.SMACK) {
                reach = Vectors.spin(away, Vectors.UP, side * Math.PI * 0.5);
                Vec3 right = reach.cross(Vectors.UP);
                variant = right.dot(away) > 0.0 ? move : move + HandPose.MOVES;
            }
            if (attempt > 0) {
                reach = Vectors.spin(reach, Vectors.UP, Math.PI * (attempt == 1 ? 1.0 : attempt == 2 ? 0.5 : -0.5));
                if (move == HandPose.SMACK) {
                    Vec3 right = reach.cross(Vectors.UP);
                    variant = right.dot(away) > 0.0 ? move : move + HandPose.MOVES;
                }
            }
            Vec3 spot = target.position().subtract(reach.scale(HandPose.spot(move) * SCALE));
            Vec3 base = this.ground(level, spot, target.getY());
            if (base != null) {
                GiantHand hand = new GiantHand(this, variant, base, target);
                if (this.fits(hand)) {
                    return hand;
                }
            }
        }
        return null;
    }

    private boolean fits(GiantHand hand) {
        if (this.taken == null) {
            this.taken = new ArrayList<>();
            for (GiantHands storm : ACTIVE.values()) {
                if (storm.owner.level() == this.owner.level()) {
                    this.taken.addAll(storm.hands);
                }
            }
        }
        GiantHandRoom room = null;
        for (GiantHand other : this.taken) {
            if (!GiantHandRoom.near(hand, other)) {
                continue;
            }
            if (room == null) {
                room = GiantHandRoom.of(hand);
            }
            if (room.clashes(GiantHandRoom.of(other))) {
                this.crowded = true;
                return false;
            }
        }
        return true;
    }

    @Nullable
    private Vec3 ground(ServerLevel level, Vec3 spot, double near) {
        Vec3 from = new Vec3(spot.x, near + 3.0, spot.z);
        if (!level.isLoaded(BlockPos.containing(from))) {
            return null;
        }
        if (solid(level, from)) {
            from = new Vec3(spot.x, near + 1.2, spot.z);
            if (solid(level, from)) {
                return null;
            }
        }
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, from.subtract(0.0, 9.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private static boolean solid(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Nullable
    private GiantHand pairFor(ServerLevel level, LivingEntity target, Vec3 away) {
        Vec3 base = this.ground(level, target.position(), target.getY());
        if (base == null) {
            return null;
        }
        Vec3 aim = inReach(base, new Vec3(target.getX(), base.y, target.getZ()));
        for (double turn : PAIR_TURNS) {
            int variant = HandPose.axeVariant(Vectors.spin(away, Vectors.UP, turn));
            if (room(level, base, variant, aim)) {
                GiantHand pair = new GiantHand(this, variant, base, target);
                if (this.fits(pair)) {
                    return pair;
                }
            }
        }
        return null;
    }

    private static boolean room(ServerLevel level, Vec3 base, int variant, Vec3 aim) {
        for (int t : new int[] { HandDuo.OUT, HandDuo.GRAB, HandDuo.AXE_FREE, HandDuo.RAISED, HandDuo.IMPACT }) {
            HandDuo duo = HandDuo.at(base, variant, aim, t, SCALE);
            if (!open(level, duo.leftPortal.center()) || !open(level, duo.rightPortal.center())
                    || !open(level, duo.axePortal.center()) || !open(level, duo.leftPlace.wrist())
                    || !open(level, duo.rightPlace.wrist())) {
                return false;
            }
            // The head only counts once it is out of its portal: behind it, it is not there yet.
            if (duo.axeThere && !duo.axeCut && !open(level, head(duo))) {
                return false;
            }
        }
        return true;
    }

    private static boolean open(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        if (pos.getY() >= level.getMaxBuildHeight()) {
            return true;
        }
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    static Vec3 head(HandDuo duo) {
        return duo.axeEnd.add(duo.axeUp.scale(HandDuo.HEAD_AT * SCALE));
    }

    static Vec3 inReach(Vec3 base, Vec3 spot) {
        double dx = spot.x - base.x;
        double dz = spot.z - base.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double reach = HandDuo.REACH * SCALE;
        return flat <= reach ? spot : new Vec3(base.x + dx * reach / flat, spot.y, base.z + dz * reach / flat);
    }

    Vec3 awayFromHim(LivingEntity living, Vec3 way) {
        Vec3 off = new Vec3(living.getX() - this.owner.getX(), 0.0, living.getZ() - this.owner.getZ());
        if (off.lengthSqr() < 1.0E-4) {
            return way;
        }
        off = off.normalize();
        double along = way.dot(off);
        if (along >= LEAST_AWAY) {
            return way;
        }
        Vec3 flung = way.add(off.scale(LEAST_AWAY - along));
        return flung.lengthSqr() < 1.0E-6 ? off : flung.normalize();
    }

    void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
