package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.List;
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
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// A ball of plasma from the arm cannon: straight where he aimed, bursting on the first creature or block in its way or
// at the end of its flight. Every game draws its flight itself from the cannon's shot (GunTracers); only the burst
// is sent.
final class HeavyPlasma implements Effect {
    // Blocks a tick, the same on the client (GunTracers.PLASMA_SPEED).
    static final double SPEED = 2.4;
    private static final int LIFE = 40;
    private static final double BODY = 0.25;
    private static final double VIEW_RANGE = 128.0;

    private final ServerPlayer owner;
    private final double damage;
    private final double radius;
    private final Vec3 way;
    private Vec3 at;
    private int age;

    private HeavyPlasma(ServerPlayer owner, Vec3 at, Vec3 way, double damage, double radius) {
        this.owner = owner;
        this.at = at;
        this.way = way;
        this.damage = damage;
        this.radius = radius;
    }

    static void fire(ServerLevel level, ServerPlayer owner, Vec3 mouth, Vec3 way, double damage, double radius) {
        Effects.start(level, new HeavyPlasma(owner, mouth, way, damage, radius));
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        if (this.owner.isRemoved()) {
            return false;
        }
        Vec3 next = this.at.add(this.way.scale(SPEED));
        Vec3 hit = this.strikes(level, this.at, next);
        if (hit == null && (this.age >= LIFE || !level.isLoaded(BlockPos.containing(next)))) {
            hit = next;
        }
        if (hit != null) {
            this.burst(level, hit);
            return false;
        }
        this.at = next;
        return true;
    }

    @Nullable
    private Vec3 strikes(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Vec3 best = block.getType() == HitResult.Type.MISS ? null : end;
        double bestFar = Double.MAX_VALUE;
        List<Entity> near = level.getEntities(this.owner, new AABB(from, end).inflate(1.0),
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
        boolean big = this.radius > 2.2;
        HeavyRocket.blast(level, this.owner, where, this.radius, this.damage, big ? 1.4 : 0.7, big);
        PacketDistributor.sendToPlayersNear(level, null, where.x, where.y, where.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), where, this.way,
                        (float) this.radius * 0.7F, 1.0F, 0.0F, false, ConstructPayload.BLAST, AirStrike.SMALL_BLAST,
                        0, null));
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), where, big ? 24 : 10, big ? 0.45 : 0.25);
        if (big) {
            ParticleFx.send(level, ParticleTypes.EXPLOSION, where.x, where.y + 0.2, where.z, 2, 0.4, 0.3, 0.4, 0.0);
        }
        Sounds.play(level, where, SoundEvents.GENERIC_EXPLODE.value(), big ? 2.6F : 1.0F,
                (big ? 1.0F : 1.6F) + 0.1F * level.random.nextFloat());
        Sounds.play(level, where, SoundEvents.AMETHYST_CLUSTER_BREAK, big ? 1.4F : 0.8F, big ? 0.6F : 1.2F);
    }
}
