package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.Recharge;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.faction.Factions;

public final class GiantHands extends GiantHandPlaces implements Effect {
    public static final double SCALE = 1.0;
    public static final int WAVE_TICKS = 18;
    private static final int AT_ONCE = 5;
    private static final int TRIES = 8;
    static final double GRAB_WIDE = 2.0;
    static final double GRAB_TALL = 3.2;
    private static final int WAIT_TICKS = 80;
    private static final int LOOK_AGAIN = 5;
    private static final double LEAST_AWAY = 0.3;
    // How likely the ragdoll's throw is followed by a hand out of a portal catching the creature in the air.
    private static final double CATCH_CHANCE = 0.5;

    private static final Map<UUID, GiantHands> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_MOVES = new HashMap<>();
    static final Map<Integer, GiantHandBase> GRABBED = new HashMap<>();

    static {
        HeldMobs.addHolder(entity -> GRABBED.containsKey(entity.getId()));
    }

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
    @Nullable
    private GiantHand latest;

    private GiantHands(ServerPlayer owner, CharacterAbility ability, List<LivingEntity> targets) {
        super(owner);
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
        PowerRing.say(owner, "giant_hands");
        PowerRing.tell(owner, "hands");
        storm.sound(level, owner.getEyePosition(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.3F);
        storm.sound(level, owner.getEyePosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
        return true;
    }

    @Override
    GiantHands storm() {
        return this;
    }

    // Every hand up in a world, of every player: none may come up inside another.
    static List<GiantHand> handsIn(Level level) {
        List<GiantHand> all = new ArrayList<>();
        for (GiantHands storm : ACTIVE.values()) {
            if (storm.owner.level() == level) {
                all.addAll(storm.hands);
            }
        }
        return all;
    }

    public static boolean waving(ServerPlayer player) {
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

    public static boolean fair(ServerPlayer owner, LivingEntity living) {
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
            if (hand.partner instanceof GiantHand partner) {
                this.hands.add(partner);
            }
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
                || move == HandPose.EYE || move == HandPose.RINGCHAINS || move == HandPose.TEAR;
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

    static Vec3 head(HandDuo duo) {
        return duo.axeEnd.add(duo.axeUp.scale(HandDuo.HEAD_AT * SCALE));
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
