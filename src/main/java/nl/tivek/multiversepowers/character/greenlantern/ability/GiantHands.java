package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
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

/**
 * Giant Hands: Green Lantern waves his ring hand, and at every wave the ring's light shoots off to a creature out to
 * hurt him somewhere round him (within {@code radiusBlocks} every way, picked at random), and a giant hand of hard light
 * rises up out of the ground there in a cloud of dust. {@code hands} of them at every press (one by default), one after
 * another, never more than three up at once and the next only once the one before is halfway through, each doing one
 * of these to its creature, never what the one before it did, not even the last one of his press before (see
 * {@link HandPose}):
 * <ul>
 * <li>a smack: its open palm sweeps through the creature and swats it away (the ability's damage);</li>
 * <li>a grab: it closes its fingers on the creature, lifts it high and throws it away (0.6 of the damage);</li>
 * <li>the middle finger: it shoots up out of the ground right at the creature and launches it and everything round it
 * far away and high (2.5 of the damage), and then jabs the finger at it;</li>
 * <li>a slam: it slaps its open palm down flat on the creature and presses it flat against the ground (1.3 of the
 * damage, and slowed down a while);</li>
 * <li>a pound: it pounds the flat side of its fist down three times (0.55 of the damage every time);</li>
 * <li>a pair with an axe (see {@link HandDuo}): two portals of the ring's light burst open beyond the creature, and a
 * right hand (with the ring) and a left hand push out of them; the right snaps its fingers, the left makes the OK sign,
 * they roll round each other and a third portal lets out a giant axe of hard light; they grab it with both hands, heave
 * it up over the top and chop it down onto the creature (3 times the damage in the middle of the blow, half that at
 * its edge, and everything there flung far away and up), leave it stuck in the ground, give him a thumbs up and pull
 * back into their portals, and the axe breaks into pieces. It counts as one hand but comes alone (only while no other
 * hand is up, and none comes while it is), and only where there is room for it.</li>
 * </ul>
 * A hand stays where it came up, but turns after the creature nearest to it and reaches for that one, smoothly, as a
 * thing this big turns (a pair's axe only strikes a few blocks round where it was called). Whatever a hand hits flies
 * away from Green Lantern. Players are only struck where players may fight each other; his own pets, villagers and
 * animals never. Every hand sinks back into the ground when it is done (a pair pulls back into its portals); if he
 * stops being Green Lantern they break apart.
 */
public final class GiantHands implements Effect {
    /** How big the hands are, next to the size they are made at (see {@link HandPose}). */
    public static final double SCALE = 1.0;
    /**
     * How long his ring arm waves at every hand he calls, up and out and down again, in ticks: his ring hand does
     * nothing else meanwhile, and the next hand only comes once it is down (his arm as it is drawn keeps to it too).
     */
    public static final int WAVE_TICKS = 18;
    // How many hands may be up at once, how far through its life the newest must be before the next is called, and how
    // many creatures it tries in a tick to find one a hand can come up at.
    private static final int AT_ONCE = 3;
    private static final double NEXT_AFTER = 0.5;
    private static final int TRIES = 8;
    // The biggest creature a hand can close its fingers round.
    static final double GRAB_WIDE = 2.0;
    static final double GRAB_TALL = 3.2;
    // How often each move is picked, next to each other: smack, grab, middle finger, slam, pound, a pair with an axe.
    private static final int[] CHANCE = { 25, 22, 12, 22, 19, 24 };
    // The ways a pair is laid out, turned from the way from him to its creature, tried in turn until one has room.
    private static final double[] PAIR_TURNS = { 0.0, Math.PI * 0.5, -Math.PI * 0.5, Math.PI };
    // While no creature left is over ground a hand can come up out of (all thrown up in the air by the blows, or over a
    // drop), how long the hands still to come wait for one to come down before no more come, and how often they look
    // again meanwhile, in ticks.
    private static final int WAIT_TICKS = 80;
    private static final int LOOK_AGAIN = 5;
    // How much of the way a blow flings a creature must at least lead away from him (1: straight away from him), so
    // nothing a hand hits ever flies back at him.
    private static final double LEAST_AWAY = 0.3;

    private static final Map<UUID, GiantHands> ACTIVE = new HashMap<>();
    // What the hand each player called last did, so the next never does it again, not even at his next press.
    private static final Map<UUID, Integer> LAST_MOVES = new HashMap<>();
    // The creatures a hand holds right now, by entity id.
    static final Map<Integer, GiantHand> GRABBED = new HashMap<>();

    static {
        // What a hand holds counts as held for every other power too.
        HeldMobs.addHolder(entity -> GRABBED.containsKey(entity.getId()));
    }

    final ServerPlayer owner;
    final CharacterAbility ability;
    private final List<GiantHand> hands = new ArrayList<>();
    private final List<LivingEntity> targets;
    // The creatures no hand could come up at since the last one came (in the air, over a drop): tried again only once
    // every other creature has had its turn (and when every one is, again a little later; see WAIT_TICKS).
    private final Set<LivingEntity> missed = new HashSet<>();
    private final int count;
    private int called;
    // How many ticks the next hand has been waiting for a creature it can come up at.
    private int waited;
    private int lastMove;
    // The hand he called last: his arm waves for it.
    @Nullable
    private GiantHand latest;

    private GiantHands(ServerPlayer owner, CharacterAbility ability, List<LivingEntity> targets) {
        this.owner = owner;
        this.ability = ability;
        this.targets = targets;
        this.count = Math.max(1, ability.intValue("hands"));
        this.lastMove = LAST_MOVES.getOrDefault(owner.getUUID(), -1);
    }

    /**
     * The key: he waves his ring hand and the hands come, as long as the ring is free, can pay for it and there is a
     * creature out to hurt him within reach that a hand can come up at (not only ones in the air or over a drop).
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
        Collections.shuffle(found, new Random(owner.getRandom().nextLong()));
        GiantHands storm = new GiantHands(owner, ability, found);
        // The first hand comes right away; where none can come up at any of them, nothing is spent.
        if (!storm.call(level, Integer.MAX_VALUE)) {
            PowerRing.tell(owner, "hands_none");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        // The ring hand waves: the beam it pours out stops.
        LightBeam.stop(owner);
        ACTIVE.put(owner.getUUID(), storm);
        Effects.start(level, storm);
        PowerRing.tell(owner, "hands");
        storm.sound(level, owner.getEyePosition(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.3F);
        storm.sound(level, owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
        return true;
    }

    /** True while this player waves his ring hand to call a hand: the ring hand does nothing else meanwhile. */
    static boolean waving(ServerPlayer player) {
        GiantHands storm = ACTIVE.get(player.getUUID());
        return storm != null && storm.latest != null && storm.latest.t < WAVE_TICKS;
    }

    /** The server stops: every creature a hand holds is let go (the mobs get their own will back). */
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

    /**
     * Who a hand may strike: a creature that is out to hurt him (a monster, or anything that has him as its target), or
     * a player he may fight. Never his own pets, villagers or animals.
     */
    static boolean fair(ServerPlayer owner, LivingEntity living) {
        if (!PowerRing.canHit(owner, living) || living instanceof OwnableEntity pet && pet.getOwner() == owner) {
            return false;
        }
        return living instanceof Enemy || living instanceof Player || living instanceof Mob mob
                && mob.getTarget() == owner;
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
        // While his ring fist is up calling an air strike's plane, the next hand waits.
        if (fuels && this.called < this.count && !AirStrike.calling(this.owner) && this.ready()) {
            int before = this.called;
            boolean more = this.call(level, TRIES);
            if (this.called > before) {
                this.waited = 0;
            } else if (++this.waited > WAIT_TICKS || !more && this.targets.isEmpty()) {
                // Nothing a hand could come up at for so long, or no creature left at all: no more hands come.
                this.called = this.count;
            } else if (!more && this.waited % LOOK_AGAIN == 0) {
                // Every creature left is in the air (thrown up by a blow) or over a drop: once in a while they all
                // get another turn, as they may have come down again.
                this.missed.clear();
            }
        }
        this.hands.removeIf(hand -> hand.tick(level, fuels));
        if (this.hands.isEmpty() && (this.called >= this.count || !fuels)) {
            ACTIVE.remove(this.owner.getUUID(), this);
            return false;
        }
        return true;
    }

    /**
     * True when the next hand may be called: fewer than {@link #AT_ONCE} are up and no pair (it is up alone), and the
     * one called last is halfway through what it does, and his arm is down from waving at it.
     */
    private boolean ready() {
        if (this.hands.size() >= AT_ONCE || this.hands.stream().anyMatch(hand -> hand.move == HandPose.AXE)) {
            return false;
        }
        GiantHand newest = this.latest;
        return newest == null || newest.t >= Math.max(WAVE_TICKS, HandPose.life(newest.variant) * NEXT_AFTER);
    }

    /** True when the next call may be a pair: it comes alone, only while no other hand is up. */
    private boolean pairFits() {
        return this.hands.isEmpty();
    }

    /**
     * He waves his ring hand towards the next creature, and a hand comes up there. Creatures no hand is busy with go
     * first, so the hands are shared out; one no hand can come up at (in the air, over a drop) is passed over until
     * every other one has had its turn.
     *
     * @param tries how many creatures it may try this time
     * @return false when no hand came and there is no creature left to try: none can come up any more
     */
    private boolean call(ServerLevel level, int tries) {
        double reach = this.ability.value("radiusBlocks");
        this.targets.removeIf(living -> !living.isAlive() || living.level() != level
                || !fair(this.owner, living) || Math.abs(living.getX() - this.owner.getX()) > reach + 4.0
                || Math.abs(living.getY() - this.owner.getY()) > reach + 4.0
                || Math.abs(living.getZ() - this.owner.getZ()) > reach + 4.0);
        // Creatures that came within reach since, or turned on him, get their turn too.
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.owner.getBoundingBox().inflate(reach), entity -> fair(this.owner, entity))) {
            if (!this.targets.contains(living)) {
                this.targets.add(living);
            }
        }
        this.missed.retainAll(this.targets);
        // The ones no hand is busy with first, then the others, each in the order of their turns.
        List<LivingEntity> turns = new ArrayList<>();
        for (boolean free : new boolean[] { true, false }) {
            for (LivingEntity living : this.targets) {
                if (!this.missed.contains(living)
                        && this.hands.stream().noneMatch(hand -> hand.target == living) == free) {
                    turns.add(living);
                }
            }
        }
        for (LivingEntity target : turns.subList(0, Math.min(tries, turns.size()))) {
            // The next time it comes last, so every creature has a turn.
            this.targets.remove(target);
            this.targets.add(target);
            int move = this.pick(target, this.pairFits());
            GiantHand hand = this.spawn(level, target, move);
            if (hand == null && move == HandPose.AXE) {
                // No room for a pair there: a hand of its own instead.
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
            this.latest = hand;
            this.lastMove = move;
            LAST_MOVES.put(this.owner.getUUID(), move);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.5F);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            return true;
        }
        return turns.size() > tries;
    }

    /**
     * What the hand does to this creature: one of the moves at random, never the one before it twice in a row, and a
     * pair only when {@code pair}.
     */
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

    /** True when this move may be picked: not the one before, a grab only for what fits in a fist, a pair if it may. */
    private boolean may(int move, boolean grabbable, boolean pair) {
        return move != this.lastMove && (move != HandPose.GRAB || grabbable) && (move != HandPose.AXE || pair);
    }

    /**
     * A hand for this creature: it comes up out of the ground so that what it does sends the creature away from him,
     * on the ground under where it should come up (the other way round when a wall is in the way there).
     */
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
                // Beside it, sweeping its palm through it the way away from him.
                reach = Vectors.spin(away, Vectors.UP, side * Math.PI * 0.5);
                Vec3 right = reach.cross(Vectors.UP);
                variant = right.dot(away) > 0.0 ? move : move + HandPose.MOVES;
            }
            if (attempt > 0) {
                // A wall where it should come up: try the other side, then the ones across.
                reach = Vectors.spin(reach, Vectors.UP, Math.PI * (attempt == 1 ? 1.0 : attempt == 2 ? 0.5 : -0.5));
                if (move == HandPose.SMACK) {
                    Vec3 right = reach.cross(Vectors.UP);
                    variant = right.dot(away) > 0.0 ? move : move + HandPose.MOVES;
                }
            }
            Vec3 spot = target.position().subtract(reach.scale(HandPose.spot(move) * SCALE));
            Vec3 base = this.ground(level, spot, target.getY());
            if (base != null) {
                return new GiantHand(this, variant, base, target);
            }
        }
        return null;
    }

    /**
     * The top of the ground at this spot, near the height of the creature: null where it is solid all the way (inside a
     * wall) or there is no ground at all.
     */
    @Nullable
    private Vec3 ground(ServerLevel level, Vec3 spot, double near) {
        Vec3 from = new Vec3(spot.x, near + 3.0, spot.z);
        if (!level.isLoaded(BlockPos.containing(from))) {
            return null;
        }
        if (solid(level, from)) {
            // Under a low roof: look from just over the creature's feet instead.
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

    /**
     * A pair of hands with an axe for this creature: on the ground under it, laid out beyond it and facing him (the
     * flat way from him to it is {@code away}), or turned a quarter one way, the other or half round when there is no
     * room that way. Null when there is no room for it at all.
     */
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
                return new GiantHand(this, variant, base, target);
            }
        }
        return null;
    }

    /**
     * True when a pair laid out this way has room: its three portals, both its wrists and the axe's head are all in the
     * open as it comes out, grabs the axe, draws it free, holds it up and chops. Ground that is not loaded counts as no
     * room: it is never loaded to look.
     */
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

    /** True when this spot is loaded and nothing solid is there (over the top of the world counts as open). */
    private static boolean open(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        if (pos.getY() >= level.getMaxBuildHeight()) {
            return true;
        }
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** The middle of a pair's axe head, along its haft. */
    static Vec3 head(HandDuo duo) {
        return duo.axeEnd.add(duo.axeUp.scale(HandDuo.HEAD_AT * SCALE));
    }

    /** A spot kept within the reach of a pair's axe round where the pair was called (flat): the axe strikes there. */
    static Vec3 inReach(Vec3 base, Vec3 spot) {
        double dx = spot.x - base.x;
        double dz = spot.z - base.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double reach = HandDuo.REACH * SCALE;
        return flat <= reach ? spot : new Vec3(base.x + dx * reach / flat, spot.y, base.z + dz * reach / flat);
    }

    /**
     * The flat way a blow along {@code way} (flat, one long, or none) flings this creature: that way as far as it can,
     * but never back towards him. A hand that turned after its creature may swat or throw towards him; then the way is
     * tipped out away from him just enough that at least {@link #LEAST_AWAY} of it leads away (a blow straight at him,
     * or one with no way of its own, flings it straight away from him).
     */
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
