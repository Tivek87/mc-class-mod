package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

// A rocket of the Rocket Launcher: out of the tube and gathering speed, straight where he aimed; it bursts on the
// first creature or block in its way or at the end of its flight. A cluster rocket always splits, after SPLIT ticks or
// wherever it strikes first, into bomblets that fan out (back off what it struck) and then each curve onto a red
// creature near (Factions.hostile), or fall where they spread. A guided rocket flies where his crosshair points while
// he holds the button and bursts as he lets go. Every game draws them as the mech's missiles
// (ConstructPayload.MECH_MISSILE, its size the rocket's, the launcher's own long and thin).
final class HeavyRocket implements Effect {
    private static final double SPEED = 0.6;
    private static final double TOP_SPEED = 1.5;
    private static final double SPEED_UP = 0.05;
    private static final int LIFE = 100;
    private static final int SPLIT = 14;
    private static final int BOMBLETS = 5;
    private static final double BOMBLET_SPEED = 0.7;
    private static final double BOMBLET_TURN = 0.22;
    private static final double BOMBLET_FALL = 0.035;
    // Bomblets fly this long before they seek or burst on a creature, so they always spread first.
    private static final int ARMED = 4;
    private static final double SEEK = 10.0;
    private static final double BOMBLET_BLAST = 0.6;
    private static final double GUIDED_SPEED = 0.85;
    private static final double GUIDED_TURN = 0.14;
    private static final double GUIDED_RANGE = 96.0;
    private static final int GUIDED_LIFE = 200;
    private static final double BODY = 0.3;
    private static final double VIEW_RANGE = 128.0;
    private static final float ROCKET_SIZE = 0.4F;
    private static final float BOMBLET_SIZE = 0.15F;
    // Each player's guided rocket in flight.
    private static final Map<UUID, HeavyRocket> GUIDED = new HashMap<>();

    private enum Kind { ROCKET, CLUSTER, BOMBLET, GUIDED }

    private final ServerPlayer owner;
    private final Kind kind;
    private final double damage;
    private final double radius;
    private final int id = PowerRing.newId();
    @Nullable
    private final LivingEntity target;
    private Vec3 at;
    private Vec3 way;
    private double speed;
    private int age;
    private boolean released;

    private HeavyRocket(ServerPlayer owner, Kind kind, double damage, double radius, Vec3 at, Vec3 way,
            @Nullable LivingEntity target) {
        this.owner = owner;
        this.kind = kind;
        this.damage = damage;
        this.radius = radius;
        this.at = at;
        this.way = way;
        this.target = target;
        this.speed = kind == Kind.BOMBLET ? BOMBLET_SPEED : kind == Kind.GUIDED ? GUIDED_SPEED : SPEED;
    }

    static void fire(ServerLevel level, ServerPlayer owner, Vec3 mouth, Vec3 way, boolean cluster, double damage,
            double radius) {
        Effects.start(level, new HeavyRocket(owner, cluster ? Kind.CLUSTER : Kind.ROCKET, damage, radius, mouth, way,
                null));
    }

    // A rocket he steers with his crosshair while he holds on; one he still steers bursts first.
    static void guide(ServerLevel level, ServerPlayer owner, Vec3 mouth, Vec3 way, double damage, double radius) {
        letGo(owner);
        HeavyRocket rocket = new HeavyRocket(owner, Kind.GUIDED, damage, radius, mouth, way, null);
        GUIDED.put(owner.getUUID(), rocket);
        Effects.start(level, rocket);
    }

    // He let go of his guided rocket: it bursts where it is.
    static void letGo(ServerPlayer owner) {
        HeavyRocket rocket = GUIDED.remove(owner.getUUID());
        if (rocket != null) {
            rocket.released = true;
        }
    }

    static void clear() {
        GUIDED.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        if (this.owner.isRemoved()) {
            GUIDED.remove(this.owner.getUUID(), this);
            ConstructPayload.sendRemove(level, this.id, this.at);
            return false;
        }
        if (this.kind == Kind.GUIDED) {
            if (this.released || this.age >= GUIDED_LIFE || GUIDED.get(this.owner.getUUID()) != this
                    || !HeavyWeapon.equipped(this.owner)) {
                GUIDED.remove(this.owner.getUUID(), this);
                this.burst(level, this.at);
                return false;
            }
            Vec3 to = this.crosshair(level).subtract(this.at);
            if (to.lengthSqr() > 1.0E-4) {
                this.way = turned(this.way, to.normalize(), GUIDED_TURN);
            }
        } else if (this.kind == Kind.BOMBLET) {
            if (this.age < ARMED) {
                this.way = this.way.add(0.0, -BOMBLET_FALL, 0.0).normalize();
            } else if (this.target != null && this.target.isAlive()) {
                Vec3 to = this.target.getBoundingBox().getCenter().subtract(this.at);
                if (to.lengthSqr() > 1.0E-6) {
                    this.way = turned(this.way, to.normalize(), BOMBLET_TURN);
                }
            } else {
                this.way = this.way.add(0.0, -BOMBLET_FALL, 0.0).normalize();
            }
        } else {
            this.speed = Math.min(TOP_SPEED, this.speed + SPEED_UP);
        }
        Vec3 next = this.at.add(this.way.scale(this.speed));
        Vec3 hit = this.strikes(level, this.at, next, this.kind != Kind.BOMBLET || this.age >= ARMED);
        if (this.kind == Kind.CLUSTER && (hit != null || this.age >= SPLIT)) {
            this.split(level, hit == null ? next : hit, hit != null);
            return false;
        }
        boolean spent = this.kind != Kind.GUIDED && this.age >= LIFE;
        if (hit == null && (spent || !level.isLoaded(BlockPos.containing(next)))) {
            hit = next;
        }
        if (hit != null) {
            this.burst(level, hit);
            return false;
        }
        this.at = next;
        ParticleFx.send(level, ParticleTypes.SMOKE, next.x, next.y, next.z, 1, 0.05, 0.05, 0.05, 0.01);
        PacketDistributor.sendToPlayersNear(level, null, next.x, next.y, next.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), next, this.way, this.kind == Kind.BOMBLET
                        ? BOMBLET_SIZE : ROCKET_SIZE, 1.0F, 0.0F, false, ConstructPayload.MECH_MISSILE,
                        this.kind == Kind.BOMBLET ? 0 : ConstructPayload.SLIM_MISSILE, this.age, null));
        return true;
    }

    // The cluster rocket bursts open: its bomblets fan out, each after a red creature near where it opened, or down.
    // Opened against something (`struck`), they fan back out of it rather than into it.
    private void split(ServerLevel level, Vec3 struckAt, boolean struck) {
        Vec3 where = struck ? struckAt.subtract(this.way.scale(0.6)) : struckAt;
        ConstructPayload.sendRemove(level, this.id, where);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), where, 10, 0.2);
        Sounds.play(level, where, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.8F);
        Vec3[] across = Vectors.across(this.way);
        List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(where, where).inflate(SEEK), living -> living != this.owner && living.isAlive()
                        && Factions.hostile(this.owner, living) && PowerRing.canHit(this.owner, living));
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(where), b.distanceToSqr(where)));
        for (int i = 0; i < BOMBLETS; i++) {
            double turn = Mth.TWO_PI * i / BOMBLETS;
            Vec3 fan = across[0].scale(Math.cos(turn)).add(across[1].scale(Math.sin(turn)));
            Vec3 out = struck ? this.way.scale(-0.35).add(fan.scale(0.9)).add(0.0, 0.35, 0.0).normalize()
                    : this.way.add(fan.scale(0.55)).normalize();
            // Off the ground it struck, none dives straight back into it.
            if (struck && out.y < 0.2) {
                out = new Vec3(out.x, 0.2, out.z).normalize();
            }
            LivingEntity foe = foes.isEmpty() ? null : foes.get(i % foes.size());
            Effects.start(level, new HeavyRocket(this.owner, Kind.BOMBLET, this.damage, this.radius * BOMBLET_BLAST,
                    where, out, foe));
        }
    }

    // Where his crosshair rests, as far as a guided rocket can be steered.
    private Vec3 crosshair(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 end = eye.add(this.owner.getLookAngle().scale(GUIDED_RANGE));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    // `way` turned towards `to` (both of length 1) by `most` radians at the most.
    private static Vec3 turned(Vec3 way, Vec3 to, double most) {
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, way.dot(to))));
        if (angle <= most) {
            return to;
        }
        Vec3 axis = way.cross(to);
        if (axis.lengthSqr() < 1.0E-9) {
            axis = Vectors.across(way)[0];
        }
        return Vectors.spin(way, axis.normalize(), most).normalize();
    }

    // Where the rocket flying from `from` to `to` strikes the first block or creature the owner may hit, or null.
    @Nullable
    private Vec3 strikes(ServerLevel level, Vec3 from, Vec3 to, boolean creatures) {
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Vec3 best = block.getType() == HitResult.Type.MISS ? null : end;
        double bestFar = Double.MAX_VALUE;
        List<Entity> near = !creatures ? List.of() : level.getEntities(this.owner, new AABB(from, end).inflate(1.0),
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        && PowerRing.canHit(this.owner, living));
        for (Entity entity : near) {
            Optional<Vec3> on = entity.getBoundingBox().inflate(BODY).clip(from, end);
            if (on.isPresent() && from.distanceToSqr(on.get()) < bestFar) {
                bestFar = from.distanceToSqr(on.get());
                best = on.get();
            }
        }
        return best;
    }

    private void burst(ServerLevel level, Vec3 where) {
        GUIDED.remove(this.owner.getUUID(), this);
        boolean small = this.kind == Kind.BOMBLET;
        blast(level, this.owner, where, this.radius, this.damage, small ? 0.7 : 1.3, !small);
        PacketDistributor.sendToPlayersNear(level, null, where.x, where.y, where.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), where, this.way,
                        (float) this.radius * 0.7F, 1.0F, 0.0F, false, ConstructPayload.BLAST, AirStrike.SMALL_BLAST,
                        0, null));
        ConstructPayload.sendRemove(level, this.id, where);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), where, small ? 8 : 18, 0.3);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, where.x, where.y + 0.2, where.z, small ? 1 : 3, 0.5, 0.3, 0.5,
                0.0);
        Sounds.play(level, where, SoundEvents.GENERIC_EXPLODE.value(), small ? 1.4F : 3.0F,
                (small ? 1.3F : 0.9F) + 0.15F * level.random.nextFloat());
    }

    // Everything the owner may hit within `radius` of `where`, harder the nearer, thrown out and up; `down` knocks
    // the near ones down.
    static void blast(ServerLevel level, ServerPlayer owner, Vec3 where, double radius, double damage, double push,
            boolean down) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(where, where).inflate(radius),
                entity -> entity != owner && entity.isAlive() && PowerRing.canHit(owner, entity))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(where);
            double far = to.length();
            if (far > radius) {
                continue;
            }
            double near = 1.0 - far / radius;
            living.invulnerableTime = 0;
            if (damage > 0.0 && !living.hurt(level.damageSources().playerAttack(owner),
                    (float) (damage * (0.4 + 0.6 * near)))) {
                continue;
            }
            if (down && near > 0.35 && living.isAlive()) {
                Knockdowns.knock(living);
            }
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double strength = push * (0.4 + 0.6 * near);
            living.setDeltaMovement(new Vec3(out.x * strength, 0.3 + 0.4 * near, out.z * strength)
                    .scale(1.0 - resist));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
    }
}
