package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPath;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class LightBolt implements Effect {
    private static final double SIZE = 0.38;
    private static final double REACH = 0.55;
    private static final double VIEW_RANGE = 128.0;
    private static final int FADE_TICKS = 3;

    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private final int id;
    private final ServerPlayer owner;
    private final float damage;
    private final double range;
    private Vec3 center;
    private final Vec3 step;
    private final Vec3 facing;
    private final ConstructPath path;
    private int age;
    private double travelled;
    private int fade = -1;

    private LightBolt(ServerPlayer owner, CharacterAbility ability, Vec3 from, Vec3 aim) {
        this.id = PowerRing.newId();
        this.owner = owner;
        this.damage = ability.getDamage();
        this.range = ability.value("rangeBlocks");
        this.center = from;
        Vec3 step = aim.scale(ability.value("speedBlocks")).add(Flight.velocity(owner));
        this.step = step.lengthSqr() < 1.0E-6 ? aim.scale(ability.value("speedBlocks")) : step;
        this.facing = this.step.normalize();
        this.path = ConstructPath.straight(from, this.facing, this.step.length(), this.range);
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability, boolean on, int data) {
        if (!on) {
            return LightBeam.stop(owner);
        }
        if ((data & Characters.HOLD) != 0) {
            return LightBeam.start(owner, level, ability);
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        LightBeam.charge(owner);
        return shoot(owner, level, ability);
    }

    private static boolean shoot(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (LightBeam.handsFull(owner) || Flight.descending(owner)) {
            return false;
        }
        long now = level.getGameTime();
        Long next = NEXT.get(owner.getUUID());
        if (next != null && now < next) {
            return false;
        }
        float cost = (float) ability.value("powerCost");
        if (PowerRing.power(owner) + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        NEXT.put(owner.getUUID(), now + Math.max(1, ability.intValue("shotTicks")));
        PowerRing.setPower(owner, PowerRing.power(owner) - cost);
        // In flight his own game has already carried him one more tick ahead than the server knows.
        Vec3 from = ringPoint(owner).add(Flight.velocity(owner));
        LightBolt bolt = new LightBolt(owner, ability, from, aim(owner, level, from, ability.value("rangeBlocks")));
        Effects.start(level, bolt);
        bolt.send(level);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F,
                1.8F);
        return true;
    }

    public static void clear() {
        NEXT.clear();
    }

    private static Vec3 ringPoint(ServerPlayer owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        return owner.getEyePosition().add(look.scale(0.8)).add(right.scale(0.32)).add(0.0, -0.18, 0.0);
    }

    private static Vec3 aim(ServerPlayer owner, ServerLevel level, Vec3 from, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 end = eye.add(owner.getLookAngle().scale(range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        Vec3 target = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        Vec3 way = target.subtract(from);
        return way.lengthSqr() < 1.0E-4 ? owner.getLookAngle() : way.normalize();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.fade >= 0) {
            this.fade++;
            if (this.fade >= FADE_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.center);
                return false;
            }
            this.send(level);
            return true;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            this.burst(level, false);
            this.send(level);
            return true;
        }
        Vec3 from = this.center;
        this.age++;
        // Counted from the ticks it has flown, the way every client counts it too.
        this.travelled = this.path.travelled(this.age);
        Vec3 to = this.path.along(this.travelled, null);
        this.center = to;
        ParticleFx.line(level, ParticleFx.dust(PowerRing.GREEN, 0.9F), from, to, 0.35);
        if (this.hitSomething(level, from, to) || this.travelled >= this.range - 1.0E-4) {
            this.burst(level, true);
        }
        this.send(level);
        return true;
    }

    private boolean hitSomething(ServerLevel level, Vec3 from, Vec3 to) {
        AABB search = new AABB(from, to).inflate(REACH);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, this.owner, from, to, search,
                entity -> entity instanceof LivingEntity && entity.isPickable()
                        && PowerRing.canHit(this.owner, entity));
        if (hit != null) {
            Entity target = hit.getEntity();
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), this.damage);
            this.center = target.getBoundingBox().getCenter();
            return true;
        }
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.owner));
        if (block.getType() != HitResult.Type.MISS) {
            this.center = block.getLocation();
            return true;
        }
        return false;
    }

    private void burst(ServerLevel level, boolean loud) {
        this.fade = 0;
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 1.3F), this.center, 14, 0.2);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), this.center, 4, 0.1, 0.0);
        if (loud) {
            level.playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    private void send(ServerLevel level) {
        float solid = this.fade < 0 ? 1.0F : 1.0F - (float) this.fade / FADE_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.center, this.facing, (float) SIZE, solid,
                        this.fade < 0 ? (float) this.step.length() : 0.0F, false, ConstructPayload.BOLT, 0, this.age,
                        this.fade < 0 ? this.path : null));
    }
}
