package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.HashSet;
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
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Segments;

// One blow of a built mech on a left click, picked at random: the backhand sweep, the stomp, the two-fisted slam, or,
// with a creature out to hurt its pilot in reach, the throw: picked up, smashed into the ground twice and, still
// alive, flung where the pilot looks.
final class MechAttack {
    private static final SoundEvent WHOOSH = Sounds.of("mech.whoosh");
    private static final double PICK_RANGE = 10.0;
    private static final double SWEEP_RADIUS = 1.8;
    private static final double SWEEP_PUSH = 1.7;
    private static final double STOMP_RADIUS = 5.0;
    private static final double STOMP_PUSH = 1.3;
    private static final double SLAM_AHEAD = 5.0;
    private static final double SLAM_RADIUS = 6.5;
    private static final double SLAM_PUSH = 1.7;
    private static final double SMASH_RADIUS = 2.5;
    private static final double SMASH_PUSH = 0.8;
    private static final double FLING = 2.6;
    private static final double FLING_UP = 0.3;

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
    private final Set<Integer> struck = new HashSet<>();

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

    // `upright`: on the mech's ground spot, facing where its pilot looks.
    static MechAttack start(ServerPlayer owner, ServerLevel level, MechScript.Stage upright) {
        Pick near = nearest(owner, level, upright);
        int kinds = near != null ? MechAttacks.THROW : MechAttacks.SLAM;
        int kind = 1 + level.random.nextInt(kinds);
        Sounds.play(level, upright.point(0.0, 7.0, 1.0), WHOOSH, 2.0F, 0.7F + 0.2F * level.random.nextFloat());
        return new MechAttack(kind, kind == MechAttacks.THROW ? near : null);
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
        return new MechAttacks.Blow(this.kind, this.t, this.from, this.turn);
    }

    int packed() {
        return MechAttacks.pack(this.kind, this.t, this.from, this.turn);
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

    // One tick of the blow; false once it is over. `legs` is where the mech stands, `upright` on that spot facing
    // where its pilot looks.
    boolean tick(ServerLevel level, ServerPlayer owner, MechScript.Stage legs, MechScript.Stage upright,
            CharacterAbility ability) {
        this.t++;
        if (this.kind == MechAttacks.DROP) {
            return this.t < MechAttacks.DROP_TICKS;
        }
        MechAttacks.Body body = MechAttacks.body(this.blow());
        MechScript.Stage frame = MechAttacks.aimed(upright, body);
        MechScript.Stage torso = MechAttacks.torso(frame, body);
        switch (this.kind) {
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
            default -> {
            }
        }
        return this.t < MechAttacks.length(this.kind);
    }

    // The back of the hand sweeps through what stands in its way, once each, and throws it on the way the hand goes.
    private void sweep(ServerLevel level, ServerPlayer owner, MechScript.Stage frame, MechScript.Stage torso,
            double damage) {
        if (this.t < MechAttacks.SWEEP_FROM - 1 || this.t > MechAttacks.SWEEP_TO) {
            return;
        }
        MechMoves.Arm arm = MechAttacks.arm(this.blow(), true, frame, torso, null, MechMoves.arm(true, frame,
                MechScript.SETTLED));
        Vec3 hand = torso.point(arm.hand());
        Vec3 was = this.swept == null ? hand : this.swept;
        this.swept = hand;
        if (this.t == MechAttacks.SWEEP_FROM) {
            Sounds.play(level, hand, WHOOSH, 3.0F, 0.8F);
        }
        if (this.t < MechAttacks.SWEEP_FROM) {
            return;
        }
        Vec3 way = hand.subtract(was);
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        flat = flat.lengthSqr() < 1.0E-6 ? frame.ahead() : flat.normalize();
        double[] a0 = { was.x, was.y, was.z };
        double[] a1 = { hand.x, hand.y, hand.z };
        double[] out = new double[2];
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(was, hand)
                .inflate(SWEEP_RADIUS + 1.0), entity -> PowerRing.canHit(owner, entity)
                        && !this.struck.contains(entity.getId()))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double[] b = { middle.x, middle.y, middle.z };
            double reach = SWEEP_RADIUS + living.getBbWidth() * 0.5;
            if (Segments.closest(a0, a1, b, b, out) > reach * reach) {
                continue;
            }
            this.struck.add(living.getId());
            living.invulnerableTime = 0;
            if (damage > 0.0) {
                living.hurt(level.damageSources().playerAttack(owner), (float) damage);
            }
            Vec3 outward = new Vec3(middle.x - frame.base().x, 0.0, middle.z - frame.base().z);
            Vec3 away = flat.scale(0.75).add(outward.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                    : outward.normalize().scale(0.35)).normalize();
            push(living, away.scale(SWEEP_PUSH).add(0.0, 0.55, 0.0));
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), middle, 14, 0.4, 0.2);
            Sounds.play(level, middle, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6F, 0.6F);
        }
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

    private static void hit(ServerLevel level, ServerPlayer owner, LivingEntity living, double damage) {
        if (damage <= 0.0) {
            return;
        }
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(owner), (float) damage);
    }

    // A playerAttack knocks back by itself: the blow's own push is set after it.
    private static void push(LivingEntity living, Vec3 velocity) {
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        living.setDeltaMovement(velocity.scale(1.0 - resist));
        living.hasImpulse = true;
        living.hurtMarked = true;
    }
}
