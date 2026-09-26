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
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
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
    private static final double[] PAIR_TURNS = { 0.0, Math.PI * 0.5, -Math.PI * 0.5, Math.PI };
    private static final int WAIT_TICKS = 80;
    private static final int LOOK_AGAIN = 5;
    private static final double LEAST_AWAY = 0.3;
    // Hands that may come out of a wall next to the creature instead of the ground, and how often when one is there.
    private static final Set<Integer> WALLED = Set.of(HandPose.SMACK, HandPose.GRAB, HandPose.FINGER, HandPose.SLAM,
            HandPose.POUND, HandPose.SNAP, HandPose.RAKE, HandPose.RAGDOLL, HandPose.RINGBEAM, HandPose.SCOOP,
            HandPose.EYE, HandPose.MEGAPHONE);
    // Hands that act from afar and so may stand higher or lower than the creature.
    private static final Set<Integer> FAR = Set.of(HandPose.RINGBEAM, HandPose.EYE, HandPose.MEGAPHONE);
    private static final double WALL_CHANCE = 0.6;
    private static final double WALL_NEAR = 4.5;
    private static final int GROUND_TURNS = 8;
    // How far above the creature a hand out of a wall stands at most, so low walls serve too.
    private static final double WALL_HIGH = 4.0;
    // How far out of its ground or wall a hand looks for its creature from.
    private static final double SEES_FROM = 1.5;
    // How likely the ragdoll's throw is followed by a hand out of a portal catching the creature in the air.
    private static final double CATCH_CHANCE = 0.5;

    private static final Map<UUID, GiantHands> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_MOVES = new HashMap<>();
    static final Map<Integer, GiantHandBase> GRABBED = new HashMap<>();

    static {
        HeldMobs.addHolder(entity -> GRABBED.containsKey(entity.getId()));
    }

    final ServerPlayer owner;
    final CharacterAbility ability;
    private final List<GiantHand> hands = new ArrayList<>();
    // Hands a hand calls up itself while the others are being ticked (the ragdoll's catch).
    private final List<GiantHand> coming = new ArrayList<>();
    private final int[] made = new int[HandPose.MOVES];
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
        this.hands.addAll(this.coming);
        this.coming.clear();
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
        if (!this.anyLeft()) {
            this.called = this.count;
            return false;
        }
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
            GiantHand hand = move < 0 ? null : this.spawn(level, target, move);
            if (hand == null && move == HandPose.AXE) {
                move = this.pick(target, false);
                hand = move < 0 ? null : this.spawn(level, target, move);
            }
            if (hand == null) {
                this.missed.add(target);
                continue;
            }
            this.missed.clear();
            this.hands.add(hand);
            this.made[move]++;
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
        int move = this.pick(target, pair, true);
        return move >= 0 ? move : this.pick(target, pair, false);
    }

    private int pick(LivingEntity target, boolean pair, boolean fresh) {
        boolean grabbable = target.getBbWidth() <= GRAB_WIDE && target.getBbHeight() <= GRAB_TALL
                && !HeldMobs.isHeldByAnyone(target) && !LightBubble.trapped(target);
        double[] chances = new double[HandPose.MOVES];
        double total = 0.0;
        for (int move = 0; move < HandPose.MOVES; move++) {
            if (this.may(move, grabbable, pair, fresh)) {
                chances[move] = this.chance(move);
                total += chances[move];
            }
        }
        if (total <= 0.0) {
            return -1;
        }
        double roll = this.owner.getRandom().nextDouble() * total;
        int last = -1;
        for (int move = 0; move < HandPose.MOVES; move++) {
            if (chances[move] <= 0.0) {
                continue;
            }
            last = move;
            roll -= chances[move];
            if (roll < 0.0) {
                return move;
            }
        }
        return last;
    }

    private boolean may(int move, boolean grabbable, boolean pair, boolean fresh) {
        boolean holds = move == HandPose.GRAB || move == HandPose.PINCH || move == HandPose.DRAG
                || move == HandPose.RAGDOLL || move == HandPose.SWALLOW || move == HandPose.RINGHOLD
                || move == HandPose.EYE;
        return HandPose.pickable(move) && (!fresh || move != this.lastMove) && this.made[move] < this.most(move)
                && (!holds || grabbable) && (move != HandPose.AXE || pair);
    }

    private boolean anyLeft() {
        for (int move = 0; move < HandPose.MOVES; move++) {
            if (HandPose.pickable(move) && this.made[move] < this.most(move) && this.chance(move) > 0.0) {
                return true;
            }
        }
        return false;
    }

    private double chance(int move) {
        return Math.max(0.0, this.ability.value(HandPose.HANDS[move] + "Chance"));
    }

    private int most(int move) {
        return this.ability.intValue(HandPose.HANDS[move] + "Most");
    }

    @Nullable
    private GiantHand spawn(ServerLevel level, LivingEntity target, int move) {
        Vec3 away = new Vec3(target.getX() - this.owner.getX(), 0.0, target.getZ() - this.owner.getZ());
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.owner.getYRot()) : away.normalize();
        if (move == HandPose.AXE) {
            return this.pairFor(level, target, away);
        }
        if (HandPose.portal(move)) {
            return this.portalHand(level, target, move, away);
        }
        RandomSource random = this.owner.getRandom();
        int extra = move == HandPose.RAGDOLL ? random.nextInt(4) | random.nextInt(1024) << 2 : 0;
        if (WALLED.contains(move) && random.nextDouble() < WALL_CHANCE) {
            GiantHand walled = this.wallHand(level, target, move, extra);
            if (walled != null) {
                return walled;
            }
        }
        GiantHand grounded = this.groundHand(level, target, move, extra, away);
        if (grounded != null || !WALLED.contains(move)) {
            return grounded;
        }
        // No room on the ground: a wall next to it will do, whatever the chance said.
        return this.wallHand(level, target, move, extra);
    }

    // The first spot round the creature where the ground is level and the whole move stays clear of blocks: the way
    // the move likes best first, then turned further and further round.
    @Nullable
    private GiantHand groundHand(ServerLevel level, LivingEntity target, int move, int extra, Vec3 away) {
        double side = this.owner.getRandom().nextBoolean() ? 1.0 : -1.0;
        // A scoop and the evil eye come up on the far side, facing the caster's way; a ring beam and a megaphone stand
        // off to one side and fire across.
        Vec3 liked = move == HandPose.SCOOP || move == HandPose.EYE ? away.scale(-1.0)
                : move == HandPose.RINGBEAM || move == HandPose.MEGAPHONE || move == HandPose.SMACK
                        ? Vectors.spin(away, Vectors.UP, side * Math.PI * 0.5) : away;
        for (int k = 0; k < GROUND_TURNS; k++) {
            int step = (k + 1) / 2;
            double turn = (k % 2 == 1 ? side : -side) * step * Math.PI * 2.0 / GROUND_TURNS;
            Vec3 reach = Vectors.spin(liked, Vectors.UP, turn);
            int variant = HandPose.variant(move, move == HandPose.SNAP && side < 0.0, false, extra);
            if (move == HandPose.SMACK) {
                variant = reach.cross(Vectors.UP).dot(away) > 0.0 ? move : move + HandPose.MOVES;
            }
            Vec3 spot = target.position().subtract(reach.scale(HandPose.spot(move) * SCALE));
            Vec3 base = GiantHandSpots.ground(level, spot, target.getY());
            if (base == null || !GiantHandSpots.standing(level, base, target.getY(), FAR.contains(move))
                    || !GiantHandSpots.sees(level, base.add(0.0, SEES_FROM, 0.0), target)) {
                continue;
            }
            GiantHand hand = new GiantHand(this, variant, base, target);
            if (GiantHandSpots.clear(level, hand) && this.fits(hand)) {
                return hand;
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

    // A wall right beside the creature: the hand comes out of it instead of the ground, the wall as its ground, so it
    // stands spot above the creature on the wall as it would stand spot away from it on the ground.
    @Nullable
    private GiantHand wallHand(ServerLevel level, LivingEntity target, int move, int extra) {
        Vec3 middle = target.getBoundingBox().getCenter();
        GiantHand best = null;
        double nearest = WALL_NEAR;
        for (int k = 0; k < 8; k++) {
            Vec3 way = Vectors.spin(new Vec3(0.0, 0.0, 1.0), Vectors.UP, k * Math.PI * 0.25);
            BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(middle, middle.add(way.scale(WALL_NEAR)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS || hit.getDirection().getAxis().isVertical()) {
                continue;
            }
            double far = hit.getLocation().distanceTo(middle);
            if (far >= nearest) {
                continue;
            }
            Vec3 out = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            double high = Math.min(HandPose.spot(move), WALL_HIGH) * SCALE;
            Vec3 base = hit.getLocation().add(0.0, high, 0.0);
            boolean wall = true;
            for (double up = 0.0; up <= high + 1.0 && wall; up += 1.0) {
                Vec3 at = hit.getLocation().add(0.0, up, 0.0);
                wall = GiantHandSpots.solid(level, at.subtract(out.scale(0.3))) && open(level, at.add(out.scale(0.7)));
            }
            if (!wall || !GiantHandSpots.sees(level, base.add(out.scale(SEES_FROM)), target)) {
                continue;
            }
            GiantHand hand = new GiantHand(this, HandPose.variant(move, false, true, extra), base, target, out);
            if (GiantHandSpots.clear(level, hand) && this.fits(hand)) {
                best = hand;
                nearest = far;
            }
        }
        return best;
    }

    @Nullable
    private GiantHand pairFor(ServerLevel level, LivingEntity target, Vec3 away) {
        Vec3 base = GiantHandSpots.ground(level, target.position(), target.getY());
        if (base == null) {
            return null;
        }
        Vec3 aim = inReach(base, new Vec3(target.getX(), base.y, target.getZ()));
        for (double turn : PAIR_TURNS) {
            int variant = HandPose.axeVariant(Vectors.spin(away, Vectors.UP, turn));
            if (GiantHandSpots.room(level, base, variant, aim)) {
                GiantHand pair = new GiantHand(this, variant, base, target);
                if (this.fits(pair)) {
                    return pair;
                }
            }
        }
        return null;
    }

    // A portal opens beside the creature for a flick (on the caster's side, so it flies away from him) or over it for
    // a pinch; failing that, turned the other ways round it.
    @Nullable
    private GiantHand portalHand(ServerLevel level, LivingEntity target, int move, Vec3 away) {
        // A drag reaches back from the far side, so the creature is dragged away from the caster, never at him.
        boolean drag = move == HandPose.DRAG;
        for (double turn : PAIR_TURNS) {
            if (drag && turn == Math.PI) {
                continue;
            }
            Vec3 facing = Vectors.spin(drag ? away.scale(-1.0) : away, Vectors.UP, turn);
            Vec3 base = portalFor(target, move, facing);
            if (GiantHandSpots.portalRoom(level, target, move, base, facing)) {
                GiantHand hand = new GiantHand(this, move, base, target, facing);
                if ((HandGroup.is(move) || GiantHandSpots.clear(level, hand)) && this.fits(hand)) {
                    return hand;
                }
            }
        }
        return null;
    }

    static Vec3 portalFor(LivingEntity target, int variant, Vec3 facing) {
        int move = HandPose.move(variant);
        Vec3 spot = move == HandPose.HAMMER ? target.position() : target.getBoundingBox().getCenter();
        Vec3 base = spot.subtract(HandPose.workOffset(variant, facing, SCALE));
        // The swallow's portal lies on the ground the creature stands on.
        return move == HandPose.SWALLOW ? new Vec3(base.x, target.getY() + 0.05, base.z) : base;
    }

    // The ragdoll's throw: half the time a hand out of a portal comes up under the falling creature and catches it.
    void catchFalling(ServerLevel level, LivingEntity living) {
        if (this.owner.getRandom().nextDouble() >= CATCH_CHANCE || !living.isAlive()) {
            return;
        }
        Vec3 away = new Vec3(living.getX() - this.owner.getX(), 0.0, living.getZ() - this.owner.getZ());
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
        Vec3 base = portalFor(living, HandPose.CATCH, away);
        if (open(level, base)) {
            this.coming.add(new GiantHand(this, HandPose.CATCH, base, living, away));
        }
    }

    static boolean open(ServerLevel level, Vec3 at) {
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
