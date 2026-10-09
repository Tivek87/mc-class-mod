package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBeam;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Segments;

// One move of a built mech. A left click strikes the next blow of its combo: the straight right (or, with a creature
// out to hurt its pilot right at its feet, the stomp), the backhand sweep, then the two-fisted slam, or, with such a
// creature in reach, the throw: picked up, smashed into the ground twice and, still alive, flung where the pilot looks.
// A right click fires its eyes (MechBeams); the missile arm, the rocket boots, the dive and the spin are moves too.
final class MechAttack {
    private static final SoundEvent WHOOSH = Sounds.of("mech.whoosh");
    private static final double PICK_RANGE = 10.0;
    // A creature out to hurt the pilot this close to its feet (and no higher than this) gets the stomp, not the fist.
    private static final double UNDERFOOT = 4.5;
    private static final double UNDERFOOT_HIGH = 2.5;
    private static final double CROSS_RADIUS = 2.4;
    private static final double CROSS_PUSH = 2.1;
    private static final double SWEEP_RADIUS = 2.6;
    private static final double SWEEP_PUSH = 1.9;
    private static final double STOMP_RADIUS = 6.5;
    private static final double STOMP_PUSH = 1.5;
    private static final double SLAM_AHEAD = 6.0;
    private static final double SLAM_RADIUS = 8.5;
    private static final double SLAM_PUSH = 1.9;
    private static final double SMASH_RADIUS = 3.0;
    private static final double SMASH_PUSH = 0.9;
    private static final double FLING = 2.8;
    private static final double FLING_UP = 0.35;
    private static final double DIVE_AHEAD = 4.5;
    private static final double DIVE_RADIUS = 9.0;
    private static final double DIVE_PUSH = 2.2;
    private static final double SPIN_RADIUS = 2.4;
    private static final double SPIN_PUSH = 1.6;
    private static final double SPIN_SLAM_RADIUS = 3.5;
    private static final int SPIN_AGAIN = 6;
    // A dive still falling this long lands where it is.
    private static final int PLUNGE_MOST = 100;
    // The blow a click late in a combo's blow chains straight on to: from this many ticks before its end.
    private static final int CHAIN = 12;

    private int kind;
    private int t;
    private int from;
    private double turn;
    @Nullable
    private LivingEntity creature;
    private boolean holding;
    private boolean gripped;
    private Vec3 pinned = Vec3.ZERO;
    @Nullable
    private Vec3 swept;
    @Nullable
    private Vec3 sweptLeft;
    private final Set<Integer> struck = new HashSet<>();
    private final Map<Integer, Integer> spun = new HashMap<>();
    private boolean queued;
    private boolean held;
    private int fired;
    private int firedAt;
    private int plunged;
    private int powered;

    private MechAttack(int kind, @Nullable Pick pick) {
        this.kind = kind;
        if (pick != null) {
            this.creature = pick.creature();
            this.turn = pick.turn();
            this.pinned = pick.creature().position();
            if (pick.creature() instanceof Mob mob) {
                this.holding = HeldMobs.hold(mob);
            }
        }
    }

    private record Pick(LivingEntity creature, double turn) {
    }

    // The combo's blow `step` (0, 1, 2). `upright`: on the mech's ground spot, facing where its pilot looks.
    static MechAttack strike(ServerPlayer owner, ServerLevel level, MechScript.Stage upright, int step) {
        Pick near = step == 2 ? nearest(owner, level, upright) : null;
        int kind = switch (step) {
            case 0 -> underfoot(owner, level, upright) ? MechAttacks.STOMP : MechAttacks.CROSS;
            case 1 -> MechAttacks.SWEEP;
            default -> near != null ? MechAttacks.THROW : MechAttacks.SLAM;
        };
        Sounds.play(level, upright.point(0.0, 9.5, 1.0), WHOOSH, 2.0F, 0.7F + 0.2F * level.random.nextFloat());
        return new MechAttack(kind, near);
    }

    // A move of no blow of the combo: the eyes, the missile arm, the rocket boots, the dive or the spin.
    static MechAttack move(int kind) {
        MechAttack attack = new MechAttack(kind, null);
        attack.held = kind == MechAttacks.GLARE;
        return attack;
    }

    int kind() {
        return this.kind;
    }

    // Whether this is a blow of the combo, and whether a click now chains the next one on at its end.
    boolean combo() {
        return this.kind == MechAttacks.CROSS || this.kind == MechAttacks.STOMP || this.kind == MechAttacks.SWEEP
                || this.kind == MechAttacks.SLAM || this.kind == MechAttacks.THROW;
    }

    boolean chains() {
        return this.combo() && this.t >= MechAttacks.length(this.kind) - CHAIN;
    }

    void queue() {
        this.queued = true;
    }

    boolean queued() {
        return this.queued;
    }

    // Whether a creature out to hurt the pilot stands right at the mech's feet, in front of it.
    private static boolean underfoot(ServerPlayer owner, ServerLevel level, MechScript.Stage upright) {
        Vec3 from = upright.base();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from)
                .inflate(UNDERFOOT), living -> GiantHands.fair(owner, living))) {
            Vec3 local = upright.local(living.position());
            if (local.z > -1.0 && local.y < UNDERFOOT_HIGH && local.x * local.x + local.z * local.z
                    < UNDERFOOT * UNDERFOOT) {
                return true;
            }
        }
        return false;
    }

    // The nearest creature out to hurt the pilot that the right hand can reach, the body turned towards it and bent
    // down.
    @Nullable
    private static Pick nearest(ServerPlayer owner, ServerLevel level, MechScript.Stage upright) {
        Vec3 from = upright.base();
        Pick best = null;
        double bestFar = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(from, from).inflate(PICK_RANGE), living -> GiantHands.fair(owner, living)
                        && !HeldMobs.isHeldByAnyone(living) && !living.getType().is(Tags.EntityTypes.BOSSES))) {
            double far = living.position().distanceToSqr(from);
            if (far >= bestFar) {
                continue;
            }
            MechAttacks.Held held = held(living);
            double turn = MechAttacks.turnFor(upright, held.center());
            MechScript.Stage turned = MechAttacks.aimed(upright, new MechAttacks.Body(0.0, 0.0, 0.0, 0.0, turn));
            if (MechAttacks.reachable(turned, held)) {
                bestFar = far;
                best = new Pick(living, turn);
            }
        }
        return best;
    }

    private static MechAttacks.Held held(LivingEntity living) {
        return new MechAttacks.Held(living.getBoundingBox().getCenter(), living.getBbWidth() * 0.5,
                living.getBbHeight() * 0.5);
    }

    MechAttacks.Blow blow() {
        return new MechAttacks.Blow(this.kind, this.t, this.shownFrom(), this.turn);
    }

    int packed() {
        return MechAttacks.pack(this.kind, this.t, this.shownFrom(), this.turn);
    }

    private int shownFrom() {
        return this.kind == MechAttacks.AIM ? MechAttacks.salvos(this.fired, this.t - this.firedAt) : this.from;
    }

    // The torso as this blow bends and turns it over the upright one.
    MechScript.Stage torso(MechScript.Stage upright) {
        MechAttacks.Body body = MechAttacks.body(this.blow());
        return MechAttacks.torso(MechAttacks.aimed(upright, body), body);
    }

    @Nullable
    LivingEntity creature() {
        return this.creature != null && this.creature.isAlive() && !this.creature.isRemoved() ? this.creature : null;
    }

    boolean gripped() {
        return this.gripped && this.creature() != null;
    }

    // One tick of the move; false once it is over. `legs` is where the mech stands, `upright` on that spot facing
    // where its pilot looks, `grounded` whether its feet are on the ground.
    boolean tick(ServerLevel level, ServerPlayer owner, MechScript.Stage legs, MechScript.Stage upright,
            CharacterAbility ability, boolean grounded) {
        this.t++;
        if (this.kind == MechAttacks.DROP) {
            return this.t < MechAttacks.DROP_TICKS;
        }
        if (this.kind == MechAttacks.FLY) {
            this.fly(level, legs, ability, grounded);
        } else if (this.kind == MechAttacks.DIVE && this.t == MechAttacks.DIVE_LAND && !grounded
                && ++this.plunged < PLUNGE_MOST) {
            // Plunging, it waits at its plunge until it strikes the ground.
            this.t = MechAttacks.DIVE_PLUNGE;
        }
        MechAttacks.Body body = MechAttacks.body(this.blow());
        MechScript.Stage frame = MechAttacks.aimed(upright, body);
        MechScript.Stage torso = MechAttacks.torso(frame, body);
        switch (this.kind) {
            case MechAttacks.CROSS -> this.cross(level, owner, frame, torso, ability.value("mechCrossDamage"));
            case MechAttacks.SWEEP -> this.sweep(level, owner, frame, torso, ability.value("mechSweepDamage"));
            case MechAttacks.STOMP -> {
                if (this.t == MechAttacks.STOMP_HIT) {
                    Vec3 sole = legs.point(MechScript.ANKLE.x, 0.0, MechScript.ANKLE.z);
                    MechBlows.stomp(level, sole, true);
                    MechBlows.blast(level, owner, null, sole, STOMP_RADIUS, ability.value("mechStompBlowDamage"),
                            STOMP_PUSH);
                }
            }
            case MechAttacks.SLAM -> {
                if (this.t == MechAttacks.SLAM_HIT) {
                    Vec3 at = frame.point(0.0, 0.0, SLAM_AHEAD);
                    MechBlows.crash(level, at);
                    MechBlows.blast(level, owner, null, at, SLAM_RADIUS, ability.value("mechSlamDamage"), SLAM_PUSH);
                }
            }
            case MechAttacks.THROW -> {
                return this.carry(level, owner, frame, torso, ability);
            }
            case MechAttacks.EYE -> {
                if (this.t == MechAttacks.EYE_FIRE) {
                    MechBeams.eye(level, owner, torso, ability);
                }
            }
            case MechAttacks.GLARE -> this.glare(level, owner, torso, ability);
            case MechAttacks.AIM -> {
                // The last salvo's kick settled, the hand shuts.
                if (this.fired >= MechAttacks.SALVOS && this.t - this.firedAt >= MechAttacks.SALVO_KICK) {
                    this.close();
                }
            }
            case MechAttacks.DIVE -> {
                if (this.t == MechAttacks.DIVE_LAND) {
                    Vec3 at = legs.point(0.0, 0.0, DIVE_AHEAD);
                    MechBlows.crash(level, at);
                    MechBlows.stomp(level, legs.base(), true);
                    MechBlows.blast(level, owner, null, at, DIVE_RADIUS, ability.value("mechDiveDamage"), DIVE_PUSH);
                }
            }
            case MechAttacks.SPIN -> this.spin(level, owner, frame, torso, legs, ability.value("mechSpinDamage"));
            default -> {
            }
        }
        return this.t < MechAttacks.length(this.kind);
    }

    // The rocket boots: it springs up at the launch and flies while their thrust lasts (mechRocketSeconds); out of
    // it, or cut, it throws its arms out and falls; on the ground again, it lands.
    private void fly(ServerLevel level, MechScript.Stage legs, CharacterAbility ability, boolean grounded) {
        Vec3 feet = legs.base();
        if (this.t == MechAttacks.FLY_LAUNCH) {
            MechBlows.stomp(level, feet, true);
            Sounds.play(level, feet, SoundEvents.FIREWORK_ROCKET_LAUNCH, 4.0F, 0.5F);
            Sounds.play(level, feet, SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
        }
        int thrust = MechAttacks.FLY_LAUNCH + Math.min(MechAttacks.FLY_MOST,
                (int) Math.round(ability.value("mechRocketSeconds") * 20.0));
        if (this.t < MechAttacks.FLY_FALL - 6 && this.t >= thrust) {
            this.cut(level, feet);
        }
        if (this.t > MechAttacks.FLY_LAUNCH && this.t < MechAttacks.FLY_FALL - 6) {
            this.powered++;
            if (this.powered % 4 == 0) {
                Sounds.play(level, feet, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.5F, 0.4F + 0.1F * level.random
                        .nextFloat());
            }
        }
        if (this.t == MechAttacks.FLY_LAND && !grounded && ++this.plunged < PLUNGE_MOST) {
            this.t = MechAttacks.FLY_FALL;
        } else if (grounded && this.t > MechAttacks.FLY_LAUNCH + 8 && this.t < MechAttacks.FLY_LAND) {
            this.t = MechAttacks.FLY_LAND;
        }
        if (this.t == MechAttacks.FLY_LAND) {
            MechBlows.stomp(level, feet, true);
        }
    }

    // The rocket boots' thrust runs out, or its pilot cuts it: it falls.
    void cut(ServerLevel level, Vec3 feet) {
        if (this.kind == MechAttacks.FLY && this.t < MechAttacks.FLY_FALL - 6) {
            this.t = MechAttacks.FLY_FALL - 6;
            Sounds.play(level, feet, SoundEvents.FIRE_EXTINGUISH, 3.0F, 0.5F);
        }
    }

    // Whether it flies on its rocket boots with thrust left, high enough off the ground to dive.
    boolean flying() {
        return this.kind == MechAttacks.FLY && this.t >= MechAttacks.FLY_LAUNCH + 2 && this.t < MechAttacks.FLY_LAND;
    }

    // The held beam: it burns from GLARE_FIRE while the button is held, GLARE_MOST at most, paying for itself once a
    // second; let go or out of power, it dies away.
    private void glare(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability) {
        if (this.t < MechAttacks.GLARE_FIRE || this.t >= MechAttacks.GLARE_MOST) {
            return;
        }
        if (!this.held || !PowerRing.upkeep(owner, this.t - MechAttacks.GLARE_FIRE,
                ability.value("mechGlarePowerPerSecond"))) {
            this.t = MechAttacks.GLARE_MOST;
            return;
        }
        MechBeams.glare(level, owner, torso, ability, this.t - MechAttacks.GLARE_FIRE);
    }

    // The right button let go: a held beam dies away.
    void letGo() {
        this.held = false;
    }

    // A left click with the missile arm up: a salvo, once the hand is open and the last salvo's kick has settled.
    boolean fire(ServerLevel level, ServerPlayer owner, MechScript.Stage upright, CharacterAbility ability) {
        if (this.kind != MechAttacks.AIM || this.t < MechAttacks.AIM_OPEN || this.t >= MechAttacks.AIM_MOST
                || this.fired >= MechAttacks.SALVOS || this.fired > 0 && this.t - this.firedAt < MechAttacks.SALVO_KICK
                || !PowerRing.pay(owner, ability.value("mechMissilePowerCost"))) {
            return false;
        }
        MechScript.Stage torso = this.torso(upright);
        Vec3 target = MechBeam.aim(level, owner, MechBeam.AIM_RANGE);
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), true, upright, torso, null,
                MechMoves.arm(true, upright, MechScript.SETTLED), target);
        MechMissiles.salvo(level, owner, ability, torso.point(arm.hand()), torso.dir(arm.way()),
                torso.dir(arm.palm()).scale(-1.0), target);
        this.fired++;
        this.firedAt = this.t;
        return true;
    }

    // The missile arm lowered (R again, its salvos spent or its time up): the hand shuts and the arm comes down.
    void close() {
        if (this.kind == MechAttacks.AIM && this.t < MechAttacks.AIM_MOST) {
            this.t = MechAttacks.AIM_MOST;
        }
    }

    // The straight right: the fist drives through what stands in its way, once each, throwing it on ahead, and
    // strikes the ground under it.
    private void cross(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            double damage) {
        if (this.t < MechAttacks.CROSS_HIT - 3 || this.t > MechAttacks.CROSS_HIT + 1) {
            return;
        }
        Vec3 hand = this.fist(true, frame, torso);
        Vec3 was = this.swept == null ? hand : this.swept;
        this.swept = hand;
        if (this.t == MechAttacks.CROSS_HIT - 2) {
            Sounds.play(level, hand, WHOOSH, 3.0F, 0.7F);
        }
        if (this.t >= MechAttacks.CROSS_HIT - 2) {
            this.strike(level, owner, was, hand, CROSS_RADIUS, damage, frame, 0.0, CROSS_PUSH, 0.5, 0);
        }
        if (this.t == MechAttacks.CROSS_HIT) {
            MechBlows.stomp(level, new Vec3(hand.x, frame.base().y, hand.z), false);
        }
    }

    // The back of the hand sweeps through what stands in its way, once each, and throws it on the way the hand goes.
    private void sweep(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            double damage) {
        if (this.t < MechAttacks.SWEEP_FROM - 1 || this.t > MechAttacks.SWEEP_TO) {
            return;
        }
        Vec3 hand = this.fist(true, frame, torso);
        Vec3 was = this.swept == null ? hand : this.swept;
        this.swept = hand;
        if (this.t == MechAttacks.SWEEP_FROM) {
            Sounds.play(level, hand, WHOOSH, 3.0F, 0.8F);
        }
        if (this.t >= MechAttacks.SWEEP_FROM) {
            this.strike(level, owner, was, hand, SWEEP_RADIUS, damage, frame, 0.35, SWEEP_PUSH, 0.55, 0);
        }
    }

    // The spin: its knees strike the ground as its legs fold; then both fists swung round strike what they pass (each
    // creature again after SPIN_AGAIN ticks at the soonest), and each fist hammered down strikes the ground round it.
    private void spin(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            MechScript.Stage legs, double damage) {
        if (this.t == MechAttacks.SPIN_FROM) {
            for (int s = -1; s <= 1; s += 2) {
                MechBlows.stomp(level, legs.point(s * MechScript.KNEE.x, 0.0, 2.8), true);
            }
            MechBlows.blast(level, owner, null, legs.base(), STOMP_RADIUS, damage * 0.5, STOMP_PUSH);
        }
        if (this.t < MechAttacks.SPIN_FROM || this.t > MechAttacks.SPIN_TO) {
            return;
        }
        if (this.t % 7 == 0) {
            Sounds.play(level, torso.point(0.0, MechScript.SHOULDER.y, 0.0), WHOOSH, 3.0F,
                    0.6F + 0.15F * level.random.nextFloat());
        }
        for (int s = 0; s < 2; s++) {
            boolean right = s == 0;
            Vec3 hand = this.fist(right, frame, torso);
            Vec3 was = right ? this.swept : this.sweptLeft;
            if (right) {
                this.swept = hand;
            } else {
                this.sweptLeft = hand;
            }
            if (was != null) {
                this.strike(level, owner, was, hand, SPIN_RADIUS, damage, legs, 0.6, SPIN_PUSH, 0.45, SPIN_AGAIN);
            }
            if (MechAttacks.slams(this.t, right)) {
                Vec3 ground = new Vec3(hand.x, legs.base().y, hand.z);
                MechBlows.stomp(level, ground, false);
                MechBlows.blast(level, owner, null, ground, SPIN_SLAM_RADIUS, damage, 1.0);
            }
        }
    }

    // Where a fist of this move is, in the world.
    private Vec3 fist(boolean right, MechScript.Stage frame, MechScript.Stage torso) {
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), right, frame, torso, null, MechMoves.arm(right, frame,
                MechScript.SETTLED));
        return torso.point(arm.hand());
    }

    // What a fist moving from `was` to `hand` passes through, of what the owner may hit, is struck once (or again
    // after `again` ticks, when not 0) and thrown on the way the fist goes, `outward` of the way out from `middle`'s
    // spot as well, and up by `up`.
    private void strike(ServerLevel level, ServerPlayer owner, Vec3 was, Vec3 hand, double radius, double damage,
            MechScript.Stage middle, double outward, double push, double up, int again) {
        Vec3 way = hand.subtract(was);
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        flat = flat.lengthSqr() < 1.0E-6 ? middle.ahead() : flat.normalize();
        double[] a0 = { was.x, was.y, was.z };
        double[] a1 = { hand.x, hand.y, hand.z };
        double[] out = new double[2];
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(was, hand)
                .inflate(radius + 1.0), entity -> PowerRing.canHit(owner, entity) && this.mayStrike(entity, again))) {
            Vec3 at = living.getBoundingBox().getCenter();
            double[] b = { at.x, at.y, at.z };
            double reach = radius + living.getBbWidth() * 0.5;
            if (Segments.closest(a0, a1, b, b, out) > reach * reach) {
                continue;
            }
            this.struck.add(living.getId());
            this.spun.put(living.getId(), this.t);
            hit(level, owner, living, damage);
            Vec3 from = new Vec3(at.x - middle.base().x, 0.0, at.z - middle.base().z);
            Vec3 away = flat.scale(1.0 - outward).add(from.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                    : from.normalize().scale(outward)).normalize();
            push(living, away.scale(push).add(0.0, up, 0.0));
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), at, 14, 0.4, 0.2);
            Sounds.play(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6F, 0.6F);
        }
    }

    private boolean mayStrike(LivingEntity living, int again) {
        if (again <= 0) {
            return !this.struck.contains(living.getId());
        }
        Integer last = this.spun.get(living.getId());
        return last == null || this.t - last >= again;
    }

    // The throw: the creature waits pinned on its spot for the hand, rides in the fist from the grab on, is smashed
    // into the ground twice and flung at the release, the arm swinging on through. Gone before that, the arm lets go
    // and the mech stands back up.
    private boolean carry(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            CharacterAbility ability) {
        if (this.t > MechAttacks.RELEASE) {
            return this.t < MechAttacks.length(this.kind);
        }
        LivingEntity held = this.creature();
        if (held == null) {
            this.drop();
            return true;
        }
        if (this.t >= MechAttacks.GRAB && !this.gripped) {
            this.gripped = true;
            Sounds.play(level, held.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), 2.0F, 0.5F);
        }
        if (!this.gripped) {
            place(held, this.pinned);
            return true;
        }
        MechAttacks.Held box = held(held);
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), true, frame, torso, box, MechMoves.arm(true, frame,
                MechScript.SETTLED));
        Vec3 grip = torso.point(MechAttacks.grip(arm, box.halfWidth()));
        place(held, grip.subtract(0.0, box.halfHeight(), 0.0));
        if (this.t == MechAttacks.SMASH || this.t == MechAttacks.SMASH2) {
            Vec3 ground = new Vec3(grip.x, grip.y - box.halfHeight(), grip.z);
            MechBlows.stomp(level, ground, false);
            MechBlows.blast(level, owner, held, ground, SMASH_RADIUS, ability.value("mechSmashDamage") * 0.5,
                    SMASH_PUSH);
            hit(level, owner, held, ability.value("mechSmashDamage"));
            held.setDeltaMovement(Vec3.ZERO);
            if (!held.isAlive()) {
                this.drop();
            }
        }
        if (this.t == MechAttacks.RELEASE) {
            Vec3 look = owner.getLookAngle();
            this.let();
            hit(level, owner, held, ability.value("mechThrowDamage"));
            push(held, look.scale(FLING).add(0.0, FLING_UP, 0.0));
            Sounds.play(level, grip, WHOOSH, 3.0F, 1.1F);
            return true;
        }
        return this.t < MechAttacks.length(this.kind);
    }

    // The throw ends where it got to and the arm lets go.
    private void drop() {
        this.let();
        this.from = Math.min(this.t, MechAttacks.length(MechAttacks.THROW));
        this.kind = MechAttacks.DROP;
        this.t = 0;
    }

    private void let() {
        if (this.holding && this.creature instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.holding = false;
        this.gripped = false;
        this.creature = null;
    }

    void release() {
        this.let();
    }

    private static void place(LivingEntity living, Vec3 at) {
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        if (living instanceof ServerPlayer player) {
            player.teleportTo(at.x, at.y, at.z);
            player.connection.aboveGroundTickCount = 0;
        } else {
            living.setPos(at.x, at.y, at.z);
        }
    }

    static void hit(ServerLevel level, ServerPlayer owner, LivingEntity living, double damage) {
        if (damage <= 0.0) {
            return;
        }
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(owner), (float) damage);
    }

    // A playerAttack knocks back by itself: the blow's own push is set after it.
    static void push(LivingEntity living, Vec3 velocity) {
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        living.setDeltaMovement(velocity.scale(1.0 - resist));
        living.hasImpulse = true;
        living.hurtMarked = true;
    }
}
