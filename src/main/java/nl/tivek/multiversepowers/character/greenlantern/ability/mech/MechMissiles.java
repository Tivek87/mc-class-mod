package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// One missile of the missile arm, fired by a left click out of the next full tube of the pod on the back of the right
// hand. It leaps out of its tube, then curves round onto where the pilot aimed and bursts there, or on the first
// creature or block in its way. Every game draws it where the server says (ConstructPayload.MECH_MISSILE) and its
// burst as the air strike's.
final class MechMissiles implements Effect {
    private static final double LAUNCH_SPEED = 0.75;
    private static final double TOP_SPEED = 2.0;
    private static final double SPEED_UP = 0.14;
    // Free of the pod for FREE ticks, then turning at most TURN radians a tick onto where the pilot aimed.
    private static final int FREE = 3;
    private static final double TURN = 0.24;
    private static final int LIFE = 80;
    private static final double BLAST = 3.2;
    private static final double PUSH = 0.9;
    private static final double BODY = 0.3;
    private static final double VIEW_RANGE = 128.0;

    private final ServerPlayer owner;
    private final double damage;
    private final int id = PowerRing.newId();
    private final Vec3 goal;
    private Vec3 at;
    private Vec3 way;
    private double speed = LAUNCH_SPEED;
    private int age;

    private MechMissiles(ServerPlayer owner, double damage, Vec3 at, Vec3 way, Vec3 goal) {
        this.owner = owner;
        this.damage = damage;
        this.at = at;
        this.way = way;
        this.goal = goal;
    }

    // `tube`: which of the pod's tubes it leaves (MechAttacks.TUBE_X); `hand`: the middle of the right palm; `way` along
    // its fingers; `back` out of the back of the hand, where the pod stands; `target` what the pilot's crosshair rests
    // on.
    static void fire(ServerLevel level, ServerPlayer owner, CharacterAbility ability, int tube, Vec3 hand, Vec3 way,
            Vec3 back, Vec3 target) {
        Vec3 across = way.cross(back).normalize();
        double x = MechAttacks.TUBE_X[tube];
        double out = -MechAttacks.TUBE_Z[tube];
        Vec3 mouth = hand.add(way.scale(0.7)).add(back.scale(out)).add(across.scale(x));
        Vec3 leaving = way.add(back.scale(0.35 + 0.58 * (out - 0.48))).add(across.scale(0.47 * x)).normalize();
        Sounds.play(level, mouth, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.5F, 0.8F + 0.1F * level.random.nextFloat());
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), mouth, 6, 0.2, 0.1);
        Effects.start(level, new MechMissiles(owner, ability.value("mechMissileDamage"), mouth, leaving, target));
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        if (this.owner.isRemoved()) {
            ConstructPayload.sendRemove(level, this.id, this.at);
            return false;
        }
        if (this.age == 1) {
            Sounds.play(level, this.at, SoundEvents.FIREWORK_ROCKET_SHOOT, 1.6F, 1.1F + 0.2F * level.random
                    .nextFloat());
        }
        this.speed = Math.min(TOP_SPEED, this.speed + SPEED_UP);
        if (this.age >= FREE) {
            Vec3 to = this.goal.subtract(this.at);
            if (to.lengthSqr() > 1.0E-6) {
                this.way = turned(this.way, to.normalize(), TURN);
            }
        }
        Vec3 next = this.at.add(this.way.scale(this.speed));
        Vec3 hit = this.strikes(level, this.at, next);
        if (hit == null && this.at.distanceTo(this.goal) <= this.speed) {
            hit = this.goal;
        }
        if (hit == null && (this.age >= LIFE || !level.isLoaded(BlockPos.containing(next)))) {
            hit = next;
        }
        if (hit != null) {
            this.burst(level, hit);
            return false;
        }
        this.at = next;
        ParticleFx.send(level, ParticleTypes.SMOKE, next.x, next.y, next.z, 1, 0.05, 0.05, 0.05, 0.01);
        PacketDistributor.sendToPlayersNear(level, null, next.x, next.y, next.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), next, this.way, 1.0F, 1.0F, 0.0F, false,
                        ConstructPayload.MECH_MISSILE, 0, this.age, null));
        return true;
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

    // Where the missile flying from `from` to `to` strikes the first block or creature the owner may hit, or null.
    @Nullable
    private Vec3 strikes(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Vec3 best = block.getType() == HitResult.Type.MISS ? null : end;
        double bestFar = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(this.owner, new AABB(from, end).inflate(1.0),
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        && PowerRing.canHit(this.owner, living))) {
            Optional<Vec3> on = entity.getBoundingBox().inflate(BODY).clip(from, end);
            if (on.isPresent() && from.distanceToSqr(on.get()) < bestFar) {
                bestFar = from.distanceToSqr(on.get());
                best = on.get();
            }
        }
        return best;
    }

    private void burst(ServerLevel level, Vec3 at) {
        MechBlows.blast(level, this.owner, null, at, BLAST, this.damage, PUSH);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), at, this.way, (float) BLAST * 0.7F,
                        1.0F, 0.0F, false, ConstructPayload.BLAST, AirStrike.SMALL_BLAST, 0, null));
        ConstructPayload.sendRemove(level, this.id, at);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), at, 14, 0.25);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, at.x, at.y + 0.2, at.z, 2, 0.4, 0.3, 0.4, 0.0);
        Sounds.play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.2F + 0.2F * level.random.nextFloat());
    }
}
