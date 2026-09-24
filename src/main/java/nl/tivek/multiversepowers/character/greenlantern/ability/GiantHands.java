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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

/**
 * Giant Hands: Green Lantern waves his ring hand this way and that, and at every wave the ring's light shoots off to a
 * creature out to hurt him somewhere round him (within {@code radiusBlocks} every way, picked at random), and a giant
 * hand of hard light rises up out of the ground there in a cloud of dust. One after another, {@code hands} of them,
 * never more than three up at once (a pair counts as two) and the next only once the one before is halfway through,
 * each doing one of these to its creature (see {@link HandPose}):
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
 * back into their portals, and the axe breaks into pieces. It counts as two hands, and only comes where there is room
 * for it.</li>
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
    // How far round its base a hand looks for the creature nearest to it, and how much nearer another must be than
    // the one it is after before it turns to that one instead (so it never flickers between two), in blocks.
    private static final double HOMING = 10.0;
    private static final double SWITCH = 1.5;
    // A hand turns round its base after its creature as a thing this big turns: never faster than TURN radians a tick,
    // gathering or losing no more than TURN_GATHER of that speed a tick, and easing in over the last stretch (EASE:
    // the part of what is left it turns in a tick there). The spot it reaches for stays at least NEAREST of how far
    // from its base it came up (and never nearer than NEAREST_LEAST blocks), so a creature that runs past close by or
    // right over it never swings it round.
    private static final double TURN = 0.07;
    private static final double TURN_GATHER = 0.01;
    private static final double EASE = 0.2;
    private static final double NEAREST = 0.6;
    private static final double NEAREST_LEAST = 1.2;
    // How the spot a hand reaches for moves in and out along the way it faces, and how a pair's spot follows its
    // creature over the ground: never faster than FOLLOW blocks a tick, gathering or losing no more than GATHER of that
    // speed a tick; a pair's is pulled after it as by a spring (how hard, next to how far off it is), braked just
    // enough never to run past it.
    private static final double FOLLOW = 0.3;
    private static final double GATHER = 0.05;
    private static final double SPRING = 0.08;
    // The biggest creature a hand can close its fingers round.
    private static final double GRAB_WIDE = 2.0;
    private static final double GRAB_TALL = 3.2;
    // How often each move is picked, next to each other: smack, grab, middle finger, slam, pound, a pair with an axe.
    private static final int[] CHANCE = { 25, 22, 12, 22, 19, 24 };
    // The ways a pair is laid out, turned from the way from him to its creature, tried in turn until one has room.
    private static final double[] PAIR_TURNS = { 0.0, Math.PI * 0.5, -Math.PI * 0.5, Math.PI };
    // A pair's axe chopped into the ground: how far round the middle of its blade it strikes, flat, in blocks, what
    // share of the damage that does in the middle and at the edge, and how hard it throws, away and up (next to the
    // knockback setting).
    private static final double AXE_REACH = 6.0;
    private static final double AXE_MIDDLE = 3.0;
    private static final double AXE_EDGE = 1.5;
    private static final double AXE_OUT = 2.4;
    private static final double AXE_UP = 1.4;
    // How many ticks before its blow the axe is heard coming down.
    private static final int CHOP_HEARD = 5;
    // A middle finger bursting out of the ground: how far round where it comes up it launches what is there, flat, in
    // blocks, what share of the damage that does, and how hard it throws, away and up (next to the knockback setting).
    private static final double BURST_REACH = 4.5;
    private static final double BURST_DAMAGE = 2.5;
    private static final double BURST_OUT = 1.8;
    private static final double BURST_UP = 1.7;
    // How long a creature slapped flat stays slowed down, in ticks.
    private static final int FLAT_TICKS = 50;
    private static final double VIEW_RANGE = 128.0;

    private static final Map<UUID, GiantHands> ACTIVE = new HashMap<>();
    // The creatures a hand holds right now, by entity id.
    private static final Map<Integer, Hand> GRABBED = new HashMap<>();

    static {
        // What a hand holds counts as held for every other power too.
        HeldMobs.addHolder(entity -> GRABBED.containsKey(entity.getId()));
    }

    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final List<Hand> hands = new ArrayList<>();
    private final List<LivingEntity> targets;
    // The creatures no hand could come up at since the last one came (in the air, over a drop): tried again only once
    // every other creature has had its turn.
    private final Set<LivingEntity> missed = new HashSet<>();
    private final int count;
    private int called;
    private int lastMove = -1;
    // The hand he called last: his arm waves for it.
    @Nullable
    private Hand latest;

    private GiantHands(ServerPlayer owner, CharacterAbility ability, List<LivingEntity> targets) {
        this.owner = owner;
        this.ability = ability;
        this.targets = targets;
        this.count = Math.max(1, ability.intValue("hands"));
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
            for (Hand hand : storm.hands) {
                hand.letGo();
            }
        }
        ACTIVE.clear();
        GRABBED.clear();
    }

    /**
     * Who a hand may strike: a creature that is out to hurt him (a monster, or anything that has him as its target), or
     * a player he may fight. Never his own pets, villagers or animals.
     */
    private static boolean fair(ServerPlayer owner, LivingEntity living) {
        if (!PowerRing.canHit(owner, living) || living instanceof OwnableEntity pet && pet.getOwner() == owner) {
            return false;
        }
        return living instanceof Enemy || living instanceof Player || living instanceof Mob mob
                && mob.getTarget() == owner;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            for (Hand hand : this.hands) {
                hand.end(level);
            }
            return false;
        }
        boolean fuels = PowerRing.fuels(this.owner, level);
        // While his ring fist is up calling an air strike's plane, the next hand waits.
        if (fuels && this.called < this.count && !AirStrike.calling(this.owner) && this.ready()) {
            if (!this.call(level, TRIES)) {
                // Nothing left that a hand can come up at: no more hands come.
                this.called = this.count;
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
     * True when the next hand may be called: fewer than {@link #AT_ONCE} are up (a pair counts as two), and the one
     * called last is halfway through what it does, and his arm is down from waving at it.
     */
    private boolean ready() {
        if (this.up() >= AT_ONCE) {
            return false;
        }
        Hand newest = this.latest;
        return newest == null || newest.t >= Math.max(WAVE_TICKS, HandPose.life(newest.variant) * NEXT_AFTER);
    }

    /** How many hands are up now: a pair counts as two. */
    private int up() {
        int up = 0;
        for (Hand hand : this.hands) {
            up += weight(hand.move);
        }
        return up;
    }

    /** How many hands a move counts as: two for the pair with the axe, else one. */
    private static int weight(int move) {
        return move == HandPose.AXE ? 2 : 1;
    }

    /**
     * True when the next call may be a pair: two of the hands are still to come, and with it there are no more than
     * {@link #AT_ONCE} up.
     */
    private boolean pairFits() {
        return this.count - this.called >= 2 && this.up() + weight(HandPose.AXE) <= AT_ONCE;
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
            Hand hand = this.spawn(level, target, move);
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
            this.called += weight(move);
            this.latest = hand;
            this.lastMove = move;
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
    private Hand spawn(ServerLevel level, LivingEntity target, int move) {
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
                return new Hand(variant, base, target);
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
    private Hand pairFor(ServerLevel level, LivingEntity target, Vec3 away) {
        Vec3 base = this.ground(level, target.position(), target.getY());
        if (base == null) {
            return null;
        }
        Vec3 aim = inReach(base, new Vec3(target.getX(), base.y, target.getZ()));
        for (double turn : PAIR_TURNS) {
            int variant = HandPose.axeVariant(Vectors.spin(away, Vectors.UP, turn));
            if (room(level, base, variant, aim)) {
                return new Hand(variant, base, target);
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
    private static Vec3 head(HandDuo duo) {
        return duo.axeEnd.add(duo.axeUp.scale(HandDuo.HEAD_AT * SCALE));
    }

    /** A spot kept within the reach of a pair's axe round where the pair was called (flat): the axe strikes there. */
    private static Vec3 inReach(Vec3 base, Vec3 spot) {
        double dx = spot.x - base.x;
        double dz = spot.z - base.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double reach = HandDuo.REACH * SCALE;
        return flat <= reach ? spot : new Vec3(base.x + dx * reach / flat, spot.y, base.z + dz * reach / flat);
    }

    /**
     * One hand of hard light (or a pair of them with an axe), from the moment the ring's light shoots off to its spot
     * until it has sunk away.
     */
    private final class Hand {
        private final int id = PowerRing.newId();
        private final int variant;
        private final int move;
        private final Vec3 base;
        // The spot on the ground it reaches for, and the creature it is after. A hand keeps that spot as the way it
        // faces round its base (radians, see way) and how far out along it (blocks), each with how much it changed the
        // last tick; a pair moves it over the ground instead, by drift a tick.
        private Vec3 aim;
        private double facing;
        private double turn;
        private double out;
        private double outSpeed;
        private Vec3 drift = Vec3.ZERO;
        private LivingEntity target;
        @Nullable
        private LivingEntity held;
        // What a slam presses flat.
        private final List<LivingEntity> pressed = new ArrayList<>();
        private int t;

        Hand(int variant, Vec3 base, LivingEntity target) {
            this.variant = variant;
            this.move = HandPose.move(variant);
            this.base = base;
            this.target = target;
            this.aim = this.within(new Vec3(target.getX(), base.y, target.getZ()));
            if (this.move != HandPose.AXE) {
                Vec3 to = this.aim.subtract(base);
                this.facing = to.x * to.x + to.z * to.z < 1.0E-8 ? 0.0 : Math.atan2(to.x, to.z);
                this.out = Math.max(this.nearest(), Math.sqrt(to.x * to.x + to.z * to.z));
                this.aim = base.add(way(this.facing).scale(this.out));
            }
        }

        /** One tick; true once it is done. */
        boolean tick(ServerLevel level, boolean fuels) {
            this.t++;
            if (!fuels) {
                this.end(level);
                return true;
            }
            this.home(level);
            if (this.t == HandPose.ARRIVES && this.move != HandPose.AXE) {
                this.burstOut(level);
            }
            switch (this.move) {
                case HandPose.SMACK -> {
                    if (this.t == HandPose.SMACK_HITS) {
                        this.smack(level);
                    }
                }
                case HandPose.GRAB -> this.grab(level);
                case HandPose.FINGER -> {
                    if (this.t == HandPose.FINGER_BURSTS) {
                        this.burst(level);
                    }
                    if (this.t == HandPose.FINGER_UP) {
                        Vec3 tip = this.place().at(new Vec3(-0.38, 6.3, 0.0));
                        ParticleFx.send(level, ParticleTypes.ANGRY_VILLAGER, tip.x, tip.y + 0.5, tip.z, 4, 0.6, 0.3,
                                0.6, 0.0);
                        GiantHands.this.sound(level, tip, SoundEvents.VILLAGER_NO, 1.6F, 0.5F);
                        GiantHands.this.sound(level, tip, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.7F);
                    }
                }
                case HandPose.SLAM -> this.slam(level);
                case HandPose.AXE -> this.pair(level);
                default -> {
                    for (int hit : HandPose.POUND_HITS) {
                        if (this.t == hit) {
                            this.pound(level);
                        }
                    }
                }
            }
            if (this.t == HandPose.sinks(this.variant) && this.move != HandPose.AXE) {
                // It sinks back into the ground.
                GiantHands.this.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.6F);
                this.dust(level, 12);
            }
            if (this.t >= HandPose.life(this.variant)) {
                this.end(level);
                return true;
            }
            this.send(level);
            return false;
        }

        /** How the hand stands now, out in the world. */
        HandPose.Place place() {
            return this.pose(this.t).place(this.base, this.aim.subtract(this.base), SCALE);
        }

        private HandPose pose(double t) {
            return HandPose.at(this.variant, t, this.reach());
        }

        /** How far off along the ground the spot it reaches for is, at the size the hand is made at. */
        private double reach() {
            Vec3 to = this.aim.subtract(this.base);
            return Math.sqrt(to.x * to.x + to.z * to.z) / SCALE;
        }

        /**
         * It turns after the creature nearest to it (see {@link #after}), smoothly, as a thing this big turns: a hand
         * turns round its base towards it, gathering speed, gliding and slowing down in time to stop right facing it,
         * and reaches in or out to how far off it is. The nearer that creature is to its base the slower it turns
         * after it, so one that runs past close by or right over it never swings it round. With no creature left, or
         * while it strikes or holds something, it goes after nothing and glides to a stop. A pair's spot is pulled
         * over the ground after its creature instead (see {@link #homeSpot}).
         */
        private void home(ServerLevel level) {
            if (this.move == HandPose.AXE) {
                this.homeSpot(level);
                return;
            }
            LivingEntity after = HandPose.locked(this.variant, this.t) ? null : this.after(level);
            double near = this.nearest();
            double off = 0.0;
            double most = TURN;
            double wanted = this.out;
            if (after != null) {
                double dx = after.getX() - this.base.x;
                double dz = after.getZ() - this.base.z;
                double far = Math.sqrt(dx * dx + dz * dz);
                off = Math.IEEEremainder(Math.atan2(dx, dz) - this.facing, Math.PI * 2.0);
                // Nearly behind it: it goes on round the way it already turns, never back and forth.
                if (off * this.turn < 0.0 && Math.abs(off) > Math.PI * 0.75) {
                    off += Math.copySign(Math.PI * 2.0, this.turn);
                }
                // Close by its base the way to it means little: the closer, the slower it turns after it.
                most = TURN * Ease.smooth(far / near);
                wanted = Math.max(near, far);
            }
            this.turn = follow(this.turn, off, TURN_GATHER, most);
            this.facing = Math.IEEEremainder(this.facing + this.turn, Math.PI * 2.0);
            this.outSpeed = follow(this.outSpeed, wanted - this.out, GATHER, FOLLOW);
            this.out += this.outSpeed;
            if (this.out < near) {
                this.out = near;
                this.outSpeed = Math.max(0.0, this.outSpeed);
            }
            this.aim = this.base.add(way(this.facing).scale(this.out));
        }

        /**
         * A pair's spot on the ground is pulled after its creature as by a spring braked just enough never to run past
         * it: it gathers speed, glides and slows down as it gets there, and with no creature left it glides to a stop.
         * From the moment its axe goes up, the spot it strikes stays put.
         */
        private void homeSpot(ServerLevel level) {
            if (HandPose.locked(this.variant, this.t)) {
                this.drift = Vec3.ZERO;
                return;
            }
            LivingEntity after = this.after(level);
            Vec3 want = after == null ? this.aim : this.within(new Vec3(after.getX(), this.base.y, after.getZ()));
            Vec3 pull = want.subtract(this.aim).scale(SPRING).subtract(this.drift.scale(2.0 * Math.sqrt(SPRING)));
            double hard = pull.length();
            if (hard > GATHER) {
                pull = pull.scale(GATHER / hard);
            }
            Vec3 drift = this.drift.add(pull);
            double speed = drift.length();
            if (speed > FOLLOW) {
                drift = drift.scale(FOLLOW / speed);
            }
            Vec3 next = this.within(this.aim.add(drift));
            // How far it really moved: what a pair's reach held back is not kept for the next tick.
            this.drift = next.subtract(this.aim);
            this.aim = next;
        }

        /**
         * The creature it turns after: the one nearest to its base within its reach, but it keeps to the one it is
         * after until another is {@link #SWITCH} nearer, so it never flickers between two; null for none.
         */
        @Nullable
        private LivingEntity after(ServerLevel level) {
            double reach = HOMING * SCALE;
            boolean keeps = this.target.isAlive() && this.target.level() == level
                    && fair(GiantHands.this.owner, this.target) && this.flat(this.target) <= reach;
            LivingEntity nearest = keeps ? this.target : null;
            double best = keeps ? this.flat(this.target) - SWITCH * SCALE : reach;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(this.base, this.base).inflate(reach, reach * 0.6, reach),
                    entity -> fair(GiantHands.this.owner, entity))) {
                double distance = this.flat(living);
                if (living != this.target && distance < best) {
                    best = distance;
                    nearest = living;
                }
            }
            if (nearest != null) {
                this.target = nearest;
            }
            return nearest;
        }

        /** How near its base the spot a hand reaches for may come, in blocks. */
        private double nearest() {
            return Math.max(NEAREST_LEAST, NEAREST * HandPose.spot(this.move)) * SCALE;
        }

        /** For a pair, the spot kept within its axe's reach round where it was called; for a hand, as it is. */
        private Vec3 within(Vec3 spot) {
            return this.move == HandPose.AXE ? inReach(this.base, spot) : spot;
        }

        private double flat(LivingEntity living) {
            double dx = living.getX() - this.base.x;
            double dz = living.getZ() - this.base.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** It bursts up out of the ground in a cloud of dust and bits of the ground. */
        private void burstOut(ServerLevel level) {
            this.dust(level, 36);
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.base.x, this.base.y + 0.4, this.base.z, 5,
                    1.2, 0.3, 1.2, 0.015);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.4F), this.base.add(0.0, 0.2, 0.0), 24, 0.45);
            GiantHands.this.sound(level, this.base, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 1.5F);
            GiantHands.this.sound(level, this.base, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
            GiantHands.this.sound(level, this.base, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.7F);
        }

        /**
         * The middle finger bursting out of the ground, its fist right behind it: everything fair round it is launched
         * far away from it and high up.
         */
        private void burst(ServerLevel level) {
            Vec3 ahead = this.aim.subtract(this.base);
            ahead = new Vec3(ahead.x, 0.0, ahead.z);
            ahead = ahead.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
            double reach = BURST_REACH * SCALE;
            for (LivingEntity living : this.near(level, BURST_REACH + 3.0)) {
                Vec3 to = living.position().subtract(this.base);
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                if (flat.length() > reach + living.getBbWidth() * 0.5 || to.y < -2.5 || to.y > 5.0 * SCALE) {
                    continue;
                }
                Vec3 away = flat.lengthSqr() < 1.0E-4 ? ahead : flat.normalize();
                this.hit(level, living, GiantHands.this.ability.getDamage() * BURST_DAMAGE, away, BURST_OUT, BURST_UP);
                ParticleFx.send(level, ParticleTypes.EXPLOSION, living.getX(), living.getY() + 0.6, living.getZ(), 1,
                        0.0, 0.0, 0.0, 0.0);
            }
            Vec3 fist = this.base.add(0.0, 0.3, 0.0);
            this.dust(level, 60);
            ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, fist.x, fist.y, fist.z, 1, 0.0, 0.0, 0.0, 0.0);
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, fist.x, fist.y + 0.4, fist.z, 10, 1.6, 0.5, 1.6,
                    0.03);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.8F), fist, 48, 0.9);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), fist.add(0.0, 1.5, 0.0), 28, 0.55);
            GiantHands.this.sound(level, fist, SoundEvents.GENERIC_EXPLODE.value(), 2.4F, 0.7F);
            GiantHands.this.sound(level, fist, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.4F, 0.5F);
            GiantHands.this.sound(level, fist, SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
            GiantHands.this.sound(level, fist, SoundEvents.ROOTED_DIRT_BREAK, 2.4F, 0.4F);
        }

        /** Bits of the ground and dust thrown up round its base. */
        private void dust(ServerLevel level, int count) {
            BlockPos under = BlockPos.containing(this.base.x, this.base.y - 0.5, this.base.z);
            BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
            if (ground != null && !ground.isAir()) {
                ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), this.base.x,
                        this.base.y + 0.3, this.base.z, count, 1.1, 0.4, 1.1, 0.25);
            }
            ParticleFx.send(level, ParticleTypes.CLOUD, this.base.x, this.base.y + 0.5, this.base.z, count / 2, 1.3,
                    0.4, 1.3, 0.05);
        }

        /**
         * The smack: everything fair its palm sweeps through is swatted away the way the palm goes, and a little up.
         */
        private void smack(ServerLevel level) {
            List<Vec3> path = new ArrayList<>();
            double from = this.t - HandPose.SWING_TICKS * 0.6;
            for (double at = from; at <= this.t + HandPose.SWING_TICKS * 0.25; at += 0.5) {
                path.add(this.pose(at).place(this.base, this.aim.subtract(this.base), SCALE).at(HandPose.PALM));
            }
            HandPose.Place now = this.place();
            Vec3 swat = new Vec3(now.forward().x, 0.0, now.forward().z);
            swat = swat.lengthSqr() < 1.0E-6 ? now.forward() : swat.normalize();
            double reach = 2.3 * SCALE;
            for (LivingEntity living : this.near(level, 12.0)) {
                Vec3 middle = living.getBoundingBox().getCenter();
                if (distance(path, middle) > reach + living.getBbWidth() * 0.5) {
                    continue;
                }
                this.hit(level, living, GiantHands.this.ability.getDamage(), swat, 1.2, 0.55);
            }
            Vec3 palm = now.at(HandPose.PALM);
            GiantHands.this.sound(level, palm, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.6F);
            GiantHands.this.sound(level, palm, SoundEvents.ANVIL_LAND, 0.8F, 1.6F);
            GiantHands.this.sound(level, palm, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 0.7F);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), palm, 16, 0.3);
        }

        /**
         * The grab: its fingers close on its creature (if it is still there, before the palm), it holds it in its fist as
         * it lifts it, and lets go as it throws, flinging it away from him.
         */
        private void grab(ServerLevel level) {
            HandPose.Place place = this.place();
            Vec3 grip = place.at(HandPose.GRIP);
            if (this.t == HandPose.GRAB_CATCHES) {
                LivingEntity caught = null;
                double best = 2.4 * SCALE;
                for (LivingEntity living : this.near(level, 6.0)) {
                    double distance = living.getBoundingBox().getCenter().distanceTo(grip);
                    if (distance < best && living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                            && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                        best = distance;
                        caught = living;
                    }
                }
                if (caught != null) {
                    this.held = caught;
                    GRABBED.put(caught.getId(), this);
                    if (caught instanceof Mob mob) {
                        HeldMobs.hold(mob);
                    }
                    GiantHands.this.sound(level, grip, SoundEvents.AMETHYST_BLOCK_PLACE, 2.0F, 0.6F);
                    GiantHands.this.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.6F, 0.7F);
                }
            }
            if (this.held == null) {
                return;
            }
            if (!this.held.isAlive() || this.held.level() != level) {
                this.letGo();
                return;
            }
            if (this.t < HandPose.GRAB_THROWS) {
                this.hold(grip);
                return;
            }
            // Thrown: away from him, the way the hand swings, and up.
            LivingEntity thrown = this.held;
            this.letGo();
            Vec3 away = this.aim.subtract(this.base);
            away = new Vec3(away.x, 0.0, away.z);
            away = away.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
            this.hit(level, thrown, GiantHands.this.ability.getDamage() * 0.6, away, 1.0, 0.9);
            GiantHands.this.sound(level, grip, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
            GiantHands.this.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.2F);
        }

        /** Keeps what it holds in its fist, still. */
        private void hold(Vec3 grip) {
            LivingEntity living = this.held;
            double y = grip.y - living.getBbHeight() * 0.5;
            living.setDeltaMovement(Vec3.ZERO);
            living.resetFallDistance();
            if (living instanceof ServerPlayer player) {
                player.teleportTo(grip.x, y, grip.z);
                player.connection.aboveGroundTickCount = 0;
            } else {
                living.setPos(grip.x, y, grip.z);
            }
        }

        /** It lets go of what it holds, if anything: a mob gets its own will back. */
        void letGo() {
            if (this.held != null) {
                GRABBED.remove(this.held.getId(), this);
                if (this.held instanceof Mob mob) {
                    HeldMobs.release(mob);
                }
                this.held = null;
            }
        }

        /**
         * The slam: everything fair under its hand as it lands flat is struck, pressed flat against the ground and slowed
         * down; while the hand presses down it stays there.
         */
        private void slam(ServerLevel level) {
            if (this.t == HandPose.SLAM_HITS) {
                HandPose.Place place = this.place();
                Vec3 along = new Vec3(place.up().x, 0.0, place.up().z);
                along = along.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : along.normalize();
                Vec3 across = along.cross(Vectors.UP);
                for (LivingEntity living : this.near(level, 10.0)) {
                    Vec3 to = living.position().subtract(place.wrist());
                    double wide = 2.0 * SCALE + living.getBbWidth() * 0.5;
                    if (to.dot(along) < -0.4 || to.dot(along) > 6.6 * SCALE || Math.abs(to.dot(across)) > wide
                            || to.y > 1.6 * SCALE || to.y < -2.5) {
                        continue;
                    }
                    this.hit(level, living, GiantHands.this.ability.getDamage() * 1.3, along, 0.0, 0.0);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FLAT_TICKS, 3),
                            GiantHands.this.owner);
                    this.pressed.add(living);
                }
                Vec3 palm = place.at(HandPose.PALM);
                ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), this.base.add(along.scale(3.0))
                        .add(0.0, 0.2, 0.0), 40, 0.6);
                this.dustAt(level, this.base.add(along.scale(3.0)), 30);
                GiantHands.this.sound(level, palm, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.2F);
                GiantHands.this.sound(level, palm, SoundEvents.SLIME_SQUISH, 2.0F, 0.5F);
                GiantHands.this.sound(level, palm, SoundEvents.ANVIL_LAND, 0.9F, 0.8F);
            }
            if (this.t > HandPose.SLAM_HITS && this.t <= HandPose.SLAM_PRESSES) {
                for (LivingEntity living : this.pressed) {
                    if (living.isAlive()) {
                        living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
                        living.hurtMarked = true;
                    }
                }
            }
        }

        /** One blow of a pound: everything fair round where the flat of its fist strikes the ground is knocked up. */
        private void pound(ServerLevel level) {
            Vec3 strike = this.place().at(HandPose.FIST);
            double reach = 2.8 * SCALE;
            for (LivingEntity living : this.near(level, 12.0)) {
                Vec3 to = living.position().subtract(strike);
                double flat = Math.sqrt(to.x * to.x + to.z * to.z);
                if (flat > reach + living.getBbWidth() * 0.5 || Math.abs(to.y) > 2.5) {
                    continue;
                }
                Vec3 out = new Vec3(to.x, 0.0, to.z);
                out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
                this.hit(level, living, GiantHands.this.ability.getDamage() * 0.55, out, 0.35, 0.5);
            }
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.5F), strike.add(0.0, 0.2, 0.0), 32, 0.5);
            this.dustAt(level, strike, 22);
            GiantHands.this.sound(level, strike, SoundEvents.ANVIL_LAND, 1.2F, 0.6F);
            GiantHands.this.sound(level, strike, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.6F);
        }

        /** Where the pair and its axe are now (see {@link HandDuo}). */
        private HandDuo duo() {
            return HandDuo.at(this.base, this.variant, this.aim, this.t, SCALE);
        }

        /**
         * The pair with the axe: what is heard and seen at each moment of it (see {@link HandDuo}); its blow is
         * {@link #chop}.
         */
        private void pair(ServerLevel level) {
            int t = this.t;
            if (t == HandDuo.ARRIVES) {
                // The ring's light reaches its spot: the two side portals burst open.
                HandDuo duo = this.duo();
                this.opens(level, duo.leftPortal, 1.5F);
                this.opens(level, duo.rightPortal, 1.7F);
            }
            if (t == HandDuo.OUT) {
                // Both hands are all the way out, and hang there humming.
                HandDuo duo = this.duo();
                for (HandPose.Place hand : List.of(duo.leftPlace, duo.rightPlace)) {
                    Vec3 wrist = hand.wrist();
                    ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), wrist, 8, 0.6, 0.03);
                    GiantHands.this.sound(level, wrist, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.3F);
                }
            }
            if (t == HandDuo.SNAP) {
                // The right hand snaps its fingers: a crisp click and sparks between thumb and middle finger.
                Vec3 snap = this.duo().rightPlace.at(HandDuo.SNAP_AT);
                ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, snap, 12, 0.3);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), snap, 14, 0.22);
                GiantHands.this.sound(level, snap, SoundEvents.WOODEN_BUTTON_CLICK_ON, 2.4F, 1.9F);
                GiantHands.this.sound(level, snap, SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 1.5F);
            }
            if (t == HandDuo.OK) {
                // The left hand makes the OK sign: a bright ding and a sparkle in the ring of thumb and finger.
                Vec3 ok = this.duo().leftPlace.at(HandDuo.OK_AT);
                ParticleFx.cloud(level, ParticleTypes.END_ROD, ok, 6, 0.2, 0.02);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), ok, 12, 0.12);
                GiantHands.this.sound(level, ok, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.8F, 1.6F);
                GiantHands.this.sound(level, ok, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.9F);
            }
            if (t == HandDuo.AXE_OPENS) {
                // The hands fling apart and the third portal bursts open beyond them.
                this.opens(level, this.duo().axePortal, 0.8F);
            }
            if (t == HandDuo.GRAB) {
                // Both fists close on the haft with a heavy clank.
                HandDuo duo = this.duo();
                for (double along : new double[] { HandDuo.GRIP_LOW, HandDuo.GRIP_HIGH }) {
                    Vec3 grip = duo.axeEnd.add(duo.axeUp.scale(along * SCALE));
                    ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), grip, 12, 0.25);
                    GiantHands.this.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.8F, 0.6F);
                }
                Vec3 middle = duo.axeEnd.add(duo.axeUp.scale((HandDuo.GRIP_LOW + HandDuo.GRIP_HIGH) * 0.5 * SCALE));
                GiantHands.this.sound(level, middle, SoundEvents.ANVIL_PLACE, 1.4F, 0.6F);
            }
            if (t == HandDuo.AXE_FREE) {
                // Its head comes free with a ring of steel, and its portal snaps shut behind it.
                HandDuo duo = this.duo();
                Vec3 head = head(duo);
                this.shuts(level, duo.axePortal, 1.2F);
                GiantHands.this.sound(level, head, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 1.3F);
                GiantHands.this.sound(level, head, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 0.7F);
            }
            if (t == HandDuo.RAISED) {
                // Heaved up over the top: it hangs there a beat, humming with its weight.
                Vec3 head = head(this.duo());
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.PALE, 1.2F), head, 10, 0.8, 0.02);
                GiantHands.this.sound(level, head, SoundEvents.ENDER_DRAGON_FLAP, 1.8F, 0.6F);
                GiantHands.this.sound(level, head, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
            }
            if (t == Math.max(HandDuo.RAISED + 1, HandDuo.IMPACT - CHOP_HEARD)) {
                // It comes down.
                Vec3 head = head(this.duo());
                GiantHands.this.sound(level, head, SoundEvents.PLAYER_ATTACK_SWEEP, 2.4F, 0.5F);
                GiantHands.this.sound(level, head, SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.9F);
            }
            if (t == HandDuo.IMPACT) {
                this.chop(level);
            }
            if (t == HandDuo.THUMBS) {
                // Both hands give him a thumbs up: a cheerful ding, two notes, and a twinkle over each thumb.
                HandDuo duo = this.duo();
                HandPose.Place[] both = { duo.leftPlace, duo.rightPlace };
                float[] notes = { 1.19F, 1.5F };
                for (int i = 0; i < both.length; i++) {
                    Vec3 thumb = both[i].at(HandDuo.FIST_HOLE).add(0.0, 2.4 * SCALE, 0.0);
                    ParticleFx.cloud(level, ParticleTypes.HAPPY_VILLAGER, thumb, 8, 0.5, 0.0);
                    ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), thumb, 10, 0.15);
                    GiantHands.this.sound(level, thumb, SoundEvents.NOTE_BLOCK_BELL.value(), 1.6F, notes[i]);
                }
                Vec3 between = duo.leftPlace.wrist().lerp(duo.rightPlace.wrist(), 0.5);
                GiantHands.this.sound(level, between, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
            }
            if (t == HandDuo.RETRACT) {
                // They pull back into their portals.
                HandDuo duo = this.duo();
                GiantHands.this.sound(level, duo.leftPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.6F);
                GiantHands.this.sound(level, duo.rightPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.7F);
            }
            if (t == HandDuo.HANDS_GONE) {
                // The hands are gone and their portals pop shut.
                HandDuo duo = this.duo();
                this.shuts(level, duo.leftPortal, 1.6F);
                this.shuts(level, duo.rightPortal, 1.8F);
            }
            if (t == HandDuo.AXE_BREAKS) {
                this.breakAxe(level);
            }
        }

        /** A portal of the ring's light bursting open: light flung off its rim, a hum, a whoosh and a chime. */
        private void opens(ServerLevel level, HandDuo.Portal portal, float pitch) {
            Vec3 middle = portal.center();
            this.rim(level, portal, false);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 8, portal.radius() * 0.3, 0.06);
            GiantHands.this.sound(level, middle, SoundEvents.BEACON_ACTIVATE, 1.6F, pitch);
            GiantHands.this.sound(level, middle, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, pitch * 0.9F);
            GiantHands.this.sound(level, middle, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, pitch * 0.7F);
        }

        /** A portal of the ring's light snapping shut: its light sucked in off its rim, and a pop. */
        private void shuts(ServerLevel level, HandDuo.Portal portal, float pitch) {
            Vec3 middle = portal.center();
            this.rim(level, portal, true);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), middle, 8, 0.3, 0.05);
            GiantHands.this.sound(level, middle, SoundEvents.ITEM_PICKUP, 1.6F, pitch * 0.35F);
            GiantHands.this.sound(level, middle, SoundEvents.BEACON_DEACTIVATE, 1.2F, pitch);
        }

        /** Light flung off the rim of a portal at its full size: out as it bursts open, in as it snaps shut. */
        private void rim(ServerLevel level, HandDuo.Portal portal, boolean in) {
            ParticleOptions light = ParticleFx.dust(PowerRing.BRIGHT, 1.3F);
            int points = Math.max(12, (int) Math.round(portal.radius() * 10.0));
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points;
                Vec3 out = portal.a().scale(Math.cos(angle)).add(portal.b().scale(Math.sin(angle)));
                Vec3 at = portal.center().add(out.scale(portal.radius()));
                ParticleFx.fly(level, light, at, in ? out.scale(-1.0) : out, in ? 0.3 : 0.4);
            }
        }

        /**
         * The axe's blow: everything fair round where the middle of its blade bites into the ground is hurt, three
         * times the damage right there and half that at the edge of the blow, and flung far away from it and up.
         */
        private void chop(ServerLevel level) {
            Vec3 edge = HandDuo.strike(this.base, this.variant, this.aim, SCALE);
            Vec3 middle = new Vec3(edge.x, this.base.y, edge.z);
            // Right in the middle: straight away from him (the way the pair is laid out may be turned to find room).
            Vec3 ahead = new Vec3(middle.x - GiantHands.this.owner.getX(), 0.0,
                    middle.z - GiantHands.this.owner.getZ());
            ahead = ahead.lengthSqr() < 1.0E-4 ? HandPose.axeWay(this.variant) : ahead.normalize();
            double reach = AXE_REACH * SCALE;
            double damage = GiantHands.this.ability.getDamage();
            for (LivingEntity living : this.near(level, HandDuo.REACH + AXE_REACH + 3.0)) {
                Vec3 to = living.position().subtract(middle);
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                double out = flat.length();
                if (out > reach + living.getBbWidth() * 0.5 || to.y < -2.5 || to.y > 4.0 * SCALE) {
                    continue;
                }
                double share = Mth.lerp(Math.min(1.0, out / reach), AXE_MIDDLE, AXE_EDGE);
                Vec3 away = out < 1.0E-2 ? ahead : flat.scale(1.0 / out);
                this.hit(level, living, damage * share, away, AXE_OUT, AXE_UP);
                ParticleFx.send(level, ParticleTypes.EXPLOSION, living.getX(), living.getY() + 0.6, living.getZ(), 1,
                        0.0, 0.0, 0.0, 0.0);
            }
            Vec3 low = middle.add(0.0, 0.2, 0.0);
            this.dustAt(level, middle, 70, 2.2);
            ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, middle.x, middle.y + 0.4, middle.z, 1, 0.0, 0.0,
                    0.0, 0.0);
            ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, middle.x, middle.y + 0.4, middle.z, 14, 2.2,
                    0.5, 2.2, 0.03);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.0F), low, 64, 1.1);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), low, 40, 0.6);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle.add(0.0, 1.2, 0.0), 30, 0.5);
            // Cracks of light running out over the ground from the blade.
            ParticleOptions crack = ParticleFx.dust(PowerRing.BRIGHT, 1.1F);
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2.0 * (i + 0.2 + ParticleFx.RANDOM.nextDouble() * 0.6) / 6.0;
                Vec3 end = low.add(Math.cos(angle) * reach * 0.8, 0.0, Math.sin(angle) * reach * 0.8);
                ParticleFx.zigzag(level, crack, low, end, 5, 0.6, 0.45);
            }
            GiantHands.this.sound(level, middle, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.55F);
            GiantHands.this.sound(level, middle, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.4F, 0.6F);
            GiantHands.this.sound(level, middle, SoundEvents.ANVIL_LAND, 1.6F, 0.45F);
            GiantHands.this.sound(level, middle, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2.0F, 0.6F);
            GiantHands.this.sound(level, middle, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.6F, 0.5F);
            GiantHands.this.sound(level, middle, SoundEvents.ROOTED_DIRT_BREAK, 2.4F, 0.4F);
        }

        /** The axe left in the ground breaks into solid pieces: chips of hard light fly, and the ground crumbles. */
        private void breakAxe(ServerLevel level) {
            HandDuo duo = this.duo();
            ParticleOptions chips = new BlockParticleOption(ParticleTypes.BLOCK,
                    Blocks.EMERALD_BLOCK.defaultBlockState());
            ParticleOptions light = ParticleFx.dust(PowerRing.BRIGHT, 1.2F);
            for (int i = 0; i <= 4; i++) {
                Vec3 at = duo.axeEnd.add(duo.axeUp.scale(HandDuo.AXE_LENGTH * SCALE * i / 4.0));
                ParticleFx.send(level, chips, at.x, at.y, at.z, 10, 0.5, 0.5, 0.5, 0.15);
                ParticleFx.cloud(level, light, at, 5, 0.5, 0.05);
            }
            Vec3 head = head(duo);
            this.dustAt(level, new Vec3(head.x, this.base.y, head.z), 18);
            Vec3 middle = duo.axeEnd.add(duo.axeUp.scale(HandDuo.AXE_LENGTH * 0.5 * SCALE));
            GiantHands.this.sound(level, middle, SoundEvents.AMETHYST_BLOCK_BREAK, 2.0F, 0.6F);
            GiantHands.this.sound(level, middle, SoundEvents.GLASS_BREAK, 1.6F, 0.7F);
            GiantHands.this.sound(level, head, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.8F);
        }

        /** Bits of the ground and dust thrown up where a blow lands. */
        private void dustAt(ServerLevel level, Vec3 at, int count) {
            this.dustAt(level, at, count, 1.0);
        }

        /** Bits of the ground and dust thrown up where a blow lands, {@code wide} blocks every way round it. */
        private void dustAt(ServerLevel level, Vec3 at, int count, double wide) {
            BlockPos under = BlockPos.containing(at.x, at.y - 0.5, at.z);
            BlockState ground = level.isLoaded(under) ? level.getBlockState(under) : null;
            if (ground != null && !ground.isAir()) {
                ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                        count, wide, 0.2, wide, 0.2);
            }
            ParticleFx.send(level, ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, count / 3, wide, 0.2, wide, 0.06);
        }

        /** Every fair creature within {@code range} of its base. */
        private List<LivingEntity> near(ServerLevel level, double range) {
            return level.getEntitiesOfClass(LivingEntity.class, new AABB(this.base, this.base).inflate(range * SCALE),
                    entity -> fair(GiantHands.this.owner, entity));
        }

        /**
         * Strikes a creature: hurt, and thrown the way {@code away} (flat) and up, as hard as the knockback setting says
         * next to {@code out} and {@code up}.
         */
        private void hit(ServerLevel level, LivingEntity living, double damage, Vec3 away, double out, double up) {
            living.invulnerableTime = 0;
            living.hurt(level.damageSources().playerAttack(GiantHands.this.owner), (float) damage);
            double knockback = GiantHands.this.ability.value("knockback");
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 push = away.scale(out * knockback).add(0.0, up * Math.min(1.0, knockback), 0.0).scale(1.0 - resist);
            if (push.lengthSqr() > 1.0E-6) {
                living.setDeltaMovement(living.getDeltaMovement().add(push));
                living.hasImpulse = true;
                living.hurtMarked = true;
            }
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), living.getBoundingBox().getCenter(), 8,
                    0.3, 0.05);
        }

        /** It is done, or broken off: it lets go of what it holds and is gone. */
        void end(ServerLevel level) {
            this.letGo();
            ConstructPayload.sendRemove(level, this.id, this.base);
        }

        private void send(ServerLevel level) {
            PacketDistributor.sendToPlayersNear(level, null, this.base.x, this.base.y, this.base.z, VIEW_RANGE,
                    new ConstructPayload(this.id, GiantHands.this.owner.getId(), this.base,
                            this.aim.subtract(this.base), (float) SCALE, 1.0F,
                            this.held == null ? -1.0F : this.held.getId(), false, ConstructPayload.HAND, this.variant,
                            this.t, null));
        }
    }

    /** The flat way a hand faces round its base, one long, for how far round it is turned (0: along +z). */
    private static Vec3 way(double facing) {
        return new Vec3(Math.sin(facing), 0.0, Math.cos(facing));
    }

    /**
     * A speed after one more tick of going after something {@code off} away: as fast as it may (never over
     * {@code most} a tick) without running past it, slowing down in time and easing in over the last stretch
     * ({@link #EASE}), and never gathering or losing more than {@code gather} of speed a tick.
     */
    private static double follow(double speed, double off, double gather, double most) {
        double far = Math.abs(off);
        double want = Math.copySign(Math.min(most, Math.min(Math.sqrt(1.6 * gather * far), EASE * far)), off);
        return speed + Mth.clamp(want - speed, -gather, gather);
    }

    /** How far {@code point} is from the nearest point of a path of points. */
    private static double distance(List<Vec3> path, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 a = path.get(i);
            Vec3 ab = path.get(i + 1).subtract(a);
            double length = ab.lengthSqr();
            double u = length < 1.0E-9 ? 0.0 : Mth.clamp(point.subtract(a).dot(ab) / length, 0.0, 1.0);
            best = Math.min(best, a.add(ab.scale(u)).distanceTo(point));
        }
        return best;
    }

    private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
