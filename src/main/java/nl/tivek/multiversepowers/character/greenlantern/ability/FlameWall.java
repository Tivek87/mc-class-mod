package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Factions;

public final class FlameWall implements Effect {
    public static final int LAY_TICKS = FlameMove.LAY_TO - FlameMove.LAY_FROM;
    public static final int RISE_TICKS = 3;
    public static final int FALL_TICKS = 10;
    public static final double AHEAD = 3.2;
    public static final double THICK = 1.1;
    public static final double HEIGHT = 2.6;
    private static final double VIEW_RANGE = 96.0;
    private static final int HURT_EVERY = 10;
    private static final double SHOVE = 0.55;

    private static final Map<UUID, FlameWall> WALLS = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final Vec3 center;
    private final Vec3 normal;
    private final Vec3 along;
    private final double width;
    private final int stand;
    private final Map<Integer, Double> sides = new HashMap<>();
    private final Map<Integer, Integer> hurtAt = new HashMap<>();
    private int age;
    private int falling = -1;

    private FlameWall(ServerPlayer owner, Vec3 center, Vec3 normal, CharacterAbility wheel) {
        this.owner = owner;
        this.center = center;
        this.normal = normal;
        this.along = new Vec3(normal.z, 0.0, -normal.x);
        this.width = wheel.value("wallWidth");
        this.stand = Math.max(1, (int) Math.round(wheel.value("wallSeconds") * 20.0));
    }

    @Nullable
    public static Vec3 base(Level level, Entity owner, Vec3 look) {
        Vec3 way = FlameHits.flat(look);
        Vec3 spot = owner.position().add(way.scale(AHEAD));
        for (int dy = 1; dy >= -4; dy--) {
            BlockPos pos = BlockPos.containing(spot.x, owner.getY() + dy, spot.z);
            BlockPos below = pos.below();
            if (!level.isLoaded(pos) || !level.isLoaded(below)) {
                return null;
            }
            VoxelShape under = level.getBlockState(below).getCollisionShape(level, below);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !under.isEmpty()) {
                return new Vec3(spot.x, below.getY() + under.max(Direction.Axis.Y), spot.z);
            }
        }
        return null;
    }

    public static void start(ServerLevel level, ServerPlayer owner, Vec3 base, Vec3 way, CharacterAbility wheel) {
        FlameWall wall = new FlameWall(owner, base, way, wheel);
        // A new wall takes over: the old one gutters out.
        WALLS.put(owner.getUUID(), wall);
        Effects.start(level, wall);
        wall.sound(SoundEvents.FIRECHARGE_USE, 0.8F, 1.0F);
        wall.send(level);
    }

    public static void clear() {
        WALLS.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.falling < 0 && (WALLS.get(this.owner.getUUID()) != this || !PowerRing.fuels(this.owner, level)
                || this.age >= LAY_TICKS + RISE_TICKS + this.stand)) {
            this.falling = 0;
            this.sound(SoundEvents.FIRE_EXTINGUISH, 0.7F, 0.8F);
        }
        this.age++;
        if (this.falling >= 0) {
            this.falling++;
            if (this.falling > FALL_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.center);
                WALLS.remove(this.owner.getUUID(), this);
                return false;
            }
            this.send(level);
            return true;
        }
        if (this.age <= LAY_TICKS) {
            double from = this.width * (this.age - 1.0) / LAY_TICKS;
            double to = this.width * this.age / LAY_TICKS;
            Vec3 start = this.center.add(this.along.scale(from - this.width * 0.5)).add(0.0, 0.15, 0.0);
            Vec3 end = this.center.add(this.along.scale(to - this.width * 0.5)).add(0.0, 0.15, 0.0);
            ParticleFx.line(level, ParticleFx.fade(0xD8FFE2, PowerRing.GREEN, 1.1F), start, end, 0.35);
        } else {
            this.hold(level, this.age == LAY_TICKS + 1);
        }
        if (this.age % 16 == 0) {
            this.sound(SoundEvents.FIRE_AMBIENT, 1.0F, 0.8F + 0.2F * this.owner.getRandom().nextFloat());
        }
        this.send(level);
        return true;
    }

    private void hold(ServerLevel level, boolean erupting) {
        CharacterAbility wheel = FlameHits.wheel();
        double half = this.width * 0.5;
        AABB zone = new AABB(this.center, this.center).inflate(half + 3.0, 0.0, half + 3.0)
                .expandTowards(0.0, HEIGHT + 0.5, 0.0).move(0.0, -1.0, 0.0);
        if (erupting) {
            this.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.5F);
            this.sound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
            this.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.35F, 1.7F);
            ParticleFx.line(level, ParticleFx.dust(0xE4FFEA, 1.6F), this.center.add(this.along.scale(-half))
                    .add(0.0, 1.2, 0.0), this.center.add(this.along.scale(half)).add(0.0, 1.2, 0.0), 0.3);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, zone, this::hostile)) {
            Vec3 rel = target.position().subtract(this.center);
            double d = rel.dot(this.normal);
            double s = rel.dot(this.along);
            double reachAcross = THICK * 0.5 + target.getBbWidth() * 0.5;
            if (Math.abs(s) > half + target.getBbWidth() * 0.5 || rel.y > HEIGHT || rel.y < -2.0) {
                continue;
            }
            if (Math.abs(d) > reachAcross) {
                this.sides.put(target.getId(), Math.signum(d));
                continue;
            }
            double side = this.sides.getOrDefault(target.getId(), d < 0.0 ? -1.0 : 1.0);
            Vec3 moving = target.getDeltaMovement();
            double into = moving.dot(this.normal);
            target.setDeltaMovement(moving.subtract(this.normal.scale(into)).add(this.normal.scale(side * SHOVE))
                    .add(0.0, erupting ? 0.45 : 0.08, 0.0));
            target.hasImpulse = true;
            target.hurtMarked = true;
            Integer last = this.hurtAt.get(target.getId());
            if (erupting || last == null || this.age - last >= HURT_EVERY) {
                this.hurtAt.put(target.getId(), this.age);
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(this.owner),
                        (float) (wheel.value("wallDamage") * (erupting ? 1.5 : 1.0)));
                FlameBurn.ignite(level, target);
            }
        }
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, zone.inflate(2.0))) {
            if (!FlameHits.burnsUp(this.owner, shot)) {
                continue;
            }
            Vec3 from = shot.position().subtract(this.center);
            Vec3 to = from.add(shot.getDeltaMovement());
            double d0 = from.dot(this.normal);
            double d1 = to.dot(this.normal);
            boolean crosses = Math.signum(d0) != Math.signum(d1) || Math.abs(d0) < THICK * 0.5
                    || Math.abs(d1) < THICK * 0.5;
            double s = to.dot(this.along);
            if (!crosses || Math.abs(s) > half || to.y < -0.5 || to.y > HEIGHT + 0.3) {
                continue;
            }
            FlameHits.burnUp(level, shot);
        }
    }

    private boolean hostile(LivingEntity living) {
        return PowerRing.canHit(this.owner, living) && Factions.hostile(this.owner, living);
    }

    private void send(ServerLevel level) {
        float solid = this.falling < 0 ? 1.0F : 1.0F - (float) this.falling / FALL_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.center, this.normal, (float) this.width, solid,
                        (float) HEIGHT, false, ConstructPayload.FLAME_WALL, 0, this.age, null));
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(null, this.center.x, this.center.y + 1.0, this.center.z, sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
