package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPath;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.BlockRules;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class GiantFist extends GiantFistSpots {
    private static final double MIN_SIZE = 1.0;
    private static final int APPEAR_TICKS = 3;
    private static final int STEP_TICKS = 10;
    private static final int HUM_TICKS = 6;
    private static final int FADE_TICKS = 6;
    private static final double SPEED = 1.3;
    private static final double JOIN = 1.3;
    private static final double MIN_JOIN = 1.5;
    private static final double NEAREST_JOIN = 1.0;
    private static final double HIT_MARGIN = 0.4;
    private static final double LIFT = 0.3;
    private static final double VIEW_RANGE = 128.0;
    private static final double FLOW = 0.3;

    private static final Map<UUID, GiantFist> HELD = new HashMap<>();

    private enum Phase {
        HOLD, FLY, FADE
    }

    private final int id = PowerRing.newId();
    private final float baseDamage;
    private final float fullDamage;
    private final double breakHardness;
    private final int maxBroken;
    private final double range;
    private final double knockback;
    private final double maxSize;
    private final int chargeTicks;
    private final float baseCost;
    private final float fullCost;
    private final Set<Integer> hit = new HashSet<>();
    private int broken;
    private Phase phase = Phase.HOLD;
    private int phaseAge;
    private int charged;
    private float pending;
    private boolean drained;
    private Vec3 offset;
    private double travelled;
    private ConstructPath path;
    private Vec3 onSight;
    private Vec3 push;

    private GiantFist(ServerPlayer owner, CharacterAbility ability) {
        super(owner);
        this.baseDamage = ability.getDamage();
        this.fullDamage = Math.max(this.baseDamage, (float) ability.value("fullChargeDamage"));
        this.breakHardness = ability.value("breakHardness");
        this.maxBroken = (int) Math.round(ability.value("maxBlocksBroken"));
        this.range = ability.value("rangeBlocks");
        this.knockback = ability.value("knockback");
        this.maxSize = Math.max(MIN_SIZE, ability.value("maxSize"));
        this.chargeTicks = Math.max(1, (int) Math.round(ability.value("chargeSeconds") * 20.0));
        this.baseCost = (float) ability.value("powerCost");
        this.fullCost = Math.max(this.baseCost, (float) ability.value("fullChargePowerCost"));
        this.pending = this.baseCost;
        this.facing = this.heldFacing();
        this.offset = this.spotOffset(this.spot, MIN_SIZE);
        this.center = this.worldPoint(this.offset);
    }

    public static boolean launch(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (PowerRing.power(owner) + 1.0E-4F < (float) ability.value("powerCost")) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        GiantFist fist = new GiantFist(owner, ability);
        HELD.put(owner.getUUID(), fist);
        Effects.start(level, fist);
        owner.swing(InteractionHand.MAIN_HAND, true);
        fist.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.7F);
        fist.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.3F);
        PowerRing.sync(owner);
        fist.send(level);
        return true;
    }

    public static boolean letGo(ServerPlayer owner) {
        return HELD.remove(owner.getUUID()) != null;
    }

    static boolean holding(ServerPlayer owner) {
        return HELD.containsKey(owner.getUUID());
    }

    public static float pending(ServerPlayer owner) {
        GiantFist fist = HELD.get(owner.getUUID());
        return fist == null ? 0.0F : fist.pending;
    }

    public static void clear() {
        HELD.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.phase != Phase.FADE && !PowerRing.fuels(this.owner, level)) {
            this.fall(level);
        }
        this.phaseAge++;
        switch (this.phase) {
            case HOLD -> this.hold(level);
            case FLY -> this.fly(level);
            case FADE -> {
                if (this.phaseAge >= FADE_TICKS) {
                    ConstructPayload.sendRemove(level, this.id, this.center);
                    return false;
                }
            }
        }
        this.send(level);
        return true;
    }

    private void hold(ServerLevel level) {
        boolean held = HELD.get(this.owner.getUUID()) == this;
        if (held) {
            this.charge(level);
        }
        this.facing = this.heldFacing();
        double size = this.grownSize();
        if (this.phaseAge % 2 == 1) {
            this.findRoom(level, size);
        }
        this.offset = this.offset.lerp(this.spotOffset(this.spot, size), FLOW);
        this.center = this.worldPoint(this.offset);
        if (!held) {
            this.shoot(level);
        }
    }

    private void charge(ServerLevel level) {
        if (this.charged >= this.chargeTicks) {
            return;
        }
        if (this.costAt(this.charged + 1) > PowerRing.power(this.owner) + 1.0E-4F) {
            if (!this.drained) {
                this.drained = true;
                PowerRing.tell(this.owner, "drained");
                this.sound(level, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
            }
            return;
        }
        this.charged++;
        if (this.charged >= this.chargeTicks) {
            this.sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 1.5F);
            this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), this.center, 30, 0.25);
        } else if (this.charged % HUM_TICKS == 1) {
            this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, (float) (0.6 + this.charge()));
        }
        float cost = this.costAt(this.charged);
        if (cost != this.pending) {
            this.pending = cost;
            PowerRing.sync(this.owner);
        }
    }

    private float costAt(int ticks) {
        if (ticks >= this.chargeTicks) {
            return this.fullCost;
        }
        double steps = (double) this.chargeTicks / STEP_TICKS;
        double done = Math.min(1.0, (ticks / STEP_TICKS) / steps);
        return (float) (this.baseCost + (this.fullCost - this.baseCost) * done);
    }

    private void shoot(ServerLevel level) {
        this.phase = Phase.FLY;
        this.phaseAge = 0;
        PowerRing.setPower(this.owner, PowerRing.power(this.owner) - this.costAt(this.charged));
        this.plan(level);
        this.owner.swing(InteractionHand.MAIN_HAND, true);
        float deeper = (float) (0.3 * this.charge());
        this.sound(level, SoundEvents.BREEZE_SHOOT, 1.0F, 0.7F - deeper);
        this.sound(level, SoundEvents.MACE_SMASH_AIR, 1.0F, 0.8F - deeper);
    }

    private void plan(ServerLevel level) {
        ConstructPath.Sight sight = this.sight();
        Vec3 offset = sight.local(this.center);
        double aside = Math.sqrt(offset.x * offset.x + offset.y * offset.y);
        double aimed = sight.local(this.aimedAt(level)).z - offset.z;
        double join = Math.max(NEAREST_JOIN, Math.min(aimed, Math.max(MIN_JOIN, JOIN * aside)));
        float pitch = this.owner.getXRot();
        double tilt = Math.toRadians(pitch - Mth.clamp(pitch, -MAX_HELD_PITCH, MAX_HELD_PITCH));
        this.path = ConstructPath.steered(offset, tilt, join, SPEED, this.range);
        this.facing = this.path.way(0.0, sight);
        this.onSight = this.path.onSight(0.0, sight);
        this.push = sight.forward();
    }

    private ConstructPath.Sight sight() {
        return ConstructPath.Sight.of(this.owner.getEyePosition(), this.owner.getYRot(), this.owner.getXRot());
    }

    private Vec3 aimedAt(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 end = eye.add(this.owner.getLookAngle().scale(this.range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.owner));
        Vec3 target = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB search = this.owner.getBoundingBox().expandTowards(target.subtract(eye)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(this.owner, eye, target, search,
                e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), eye.distanceToSqr(target));
        return entity != null ? entity.getEntity().getBoundingBox().getCenter() : target;
    }

    private void fly(ServerLevel level) {
        Vec3 from = this.center;
        // Derived only from phaseAge, so clients replay the exact same path from their own copy.
        this.travelled = this.path.travelled(this.phaseAge);
        ConstructPath.Sight sight = this.sight();
        Vec3 to = this.path.along(this.travelled, sight);
        this.facing = this.path.way(this.travelled, sight);
        Vec3 onSight = this.path.onSight(this.travelled, sight);
        if (onSight.distanceToSqr(this.onSight) > 1.0E-8) {
            this.push = onSight.subtract(this.onSight).normalize();
        }
        this.onSight = onSight;
        this.ram(level, from, to);
        this.smash(level, from, to);
        this.center = to;
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.4F), from, 3 + (int) this.grownSize(),
                0.3 * this.grownSize(), 0.0);
        if (this.travelled >= this.range - 1.0E-3) {
            this.fall(level);
        }
    }

    private void ram(ServerLevel level, Vec3 from, Vec3 to) {
        double size = this.grownSize();
        double radius = size * 0.5 + HIT_MARGIN;
        Vec3 knuckles = to.add(this.facing.scale(FRONT * size));
        AABB area = new AABB(from, knuckles).inflate(radius + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> PowerRing.canHit(this.owner, entity))) {
            if (this.hit.contains(target.getId())) {
                continue;
            }
            AABB body = target.getBoundingBox();
            if (!body.inflate(radius).contains(closest(from, knuckles, body.getCenter()))) {
                continue;
            }
            this.hit.add(target.getId());
            this.punch(level, target);
        }
    }

    private void smash(ServerLevel level, Vec3 from, Vec3 to) {
        if (this.breakHardness < 0.0 || this.broken >= this.maxBroken) {
            return;
        }
        double size = this.grownSize();
        double radius = size * 0.5;
        Vec3 knuckles = to.add(this.facing.scale(FRONT * size));
        AABB area = new AABB(from, knuckles).inflate(radius);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            if (this.broken >= this.maxBroken) {
                return;
            }
            Vec3 middle = pos.getCenter();
            if (closest(from, knuckles, middle).distanceToSqr(middle) > radius * radius || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > this.breakHardness
                    || !BlockRules.mayBreak(level, this.owner, pos, state)) {
                continue;
            }
            level.destroyBlock(pos, true, this.owner);
            this.broken++;
        }
    }

    private static Vec3 closest(Vec3 a, Vec3 b, Vec3 p) {
        Vec3 ab = b.subtract(a);
        double length = ab.lengthSqr();
        double t = length < 1.0E-9 ? 0.0 : Mth.clamp(p.subtract(a).dot(ab) / length, 0.0, 1.0);
        return a.add(ab.scale(t));
    }

    private void punch(ServerLevel level, LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(this.owner), this.damage());
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(new Vec3(this.push.x * this.knockback,
                Math.max(0.0, this.push.y * this.knockback) + LIFT, this.push.z * this.knockback)
                .scale(1.0 - resist));
        target.hasImpulse = true;
        target.hurtMarked = true;
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), at, 16, 0.3);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 10, 0.3, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private void fall(ServerLevel level) {
        boolean wasHeld = HELD.remove(this.owner.getUUID(), this);
        this.phase = Phase.FADE;
        this.phaseAge = 0;
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 1.2F), this.center, 24, 0.15);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.5F);
        if (wasHeld) {
            PowerRing.sync(this.owner);
        }
    }

    private double charge() {
        return (double) this.charged / this.chargeTicks;
    }

    private double grownSize() {
        return MIN_SIZE + (this.maxSize - MIN_SIZE) * this.charge();
    }

    private float damage() {
        return Mth.lerp((float) this.charge(), this.baseDamage, this.fullDamage);
    }

    private float size() {
        float size = (float) this.grownSize();
        return this.phase == Phase.FADE ? size * (1.0F + 0.15F * this.phaseAge / FADE_TICKS) : size;
    }

    private float solid() {
        return switch (this.phase) {
            case HOLD -> Math.min(1.0F, (float) this.phaseAge / APPEAR_TICKS);
            case FLY -> 1.0F;
            case FADE -> 1.0F - (float) this.phaseAge / FADE_TICKS;
        };
    }

    private void send(ServerLevel level) {
        // Held sends an eye-relative offset so clients hang it on their own copy of the player.
        boolean held = this.phase == Phase.HOLD;
        boolean flying = this.phase == Phase.FLY;
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), held ? this.offset : this.center, this.facing,
                        this.size(), this.solid(), (float) this.charge(), held, ConstructPayload.FIST, 0,
                        flying ? this.phaseAge : 0, flying ? this.path : null));
    }

}
