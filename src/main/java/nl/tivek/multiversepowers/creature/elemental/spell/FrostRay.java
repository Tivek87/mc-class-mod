package nl.tivek.multiversepowers.creature.elemental.spell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.BlazeTimeline;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import org.joml.Vector3f;

// The frost ray: a beam of cold light from the ice rod (or the staff) to the first thing in its way. The Blaze's beam
// follows its foe a little behind, so one who keeps moving can slip out of it; whoever stays in it long enough is hurt
// and frozen in place for three seconds. Water it touches freezes over into ice that melts again. Drawn by
// client/FrostRayRenderer from where its caster's rod is to the end the server last found.
public final class FrostRay extends Entity {
    public static final int WARM = 3;
    private static final int FROZEN_FOR = 60;
    private static final int PLAYER_HOLD = 6;
    private static final double FOLLOW = 0.22;
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(FrostRay.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(FrostRay.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(FrostRay.class,
            EntityDataSerializers.VECTOR3);

    private final Map<Integer, Integer> held = new HashMap<>();
    private final Set<Integer> frozen = new HashSet<>();
    private Vec3 way = Vec3.ZERO;
    private Vec3 endO = Vec3.ZERO;
    private float damage;

    public FrostRay(EntityType<? extends FrostRay> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void fire(ServerLevel level, LivingEntity caster, int life) {
        FrostRay ray = new FrostRay(ElementalBlaze.FROST_RAY.get(), level);
        Vec3 from = BlazeCasts.tip(caster, 1.0F);
        ray.setPos(from);
        ray.way = BlazeCasts.aim(caster, from, 0.0);
        ray.damage = BlazeCasts.damage(caster, Element.ICE);
        ray.entityData.set(CASTER, caster.getId());
        ray.entityData.set(LIFE, life);
        ray.entityData.set(END, from.add(ray.way).toVector3f());
        level.addFreshEntity(ray);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER, -1);
        builder.define(LIFE, BlazeTimeline.BEAM);
        builder.define(END, new Vector3f());
    }

    @Nullable
    public Entity caster() {
        return this.level().getEntity(this.entityData.get(CASTER));
    }

    public Vec3 end(float partialTick) {
        Vec3 end = new Vec3(this.entityData.get(END));
        return this.tickCount <= 1 ? end : this.endO.lerp(end, partialTick);
    }

    // How strong it burns now: thin as it starts, whole, then gone over its last ticks.
    public float strength(float partialTick) {
        float age = this.tickCount + partialTick;
        return Mth.clamp(age / WARM, 0.15F, 1.0F) * Mth.clamp((this.entityData.get(LIFE) - age) / 4.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.endO = new Vec3(this.entityData.get(END));
        super.tick();
        if (this.level().isClientSide) {
            this.flakes();
            return;
        }
        Entity caster = this.caster();
        if (!(caster instanceof LivingEntity living) || !living.isAlive() || this.tickCount >= this.entityData.get(LIFE)
                || caster instanceof ElementalBlaze blaze && BlazeTimeline.element(blaze.move()) != Element.ICE) {
            this.discard();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Vec3 from = BlazeCasts.tip(living, 1.0F);
        Vec3 aim = BlazeCasts.aim(living, from, 0.0);
        this.way = caster instanceof ElementalBlaze ? this.way.scale(1.0 - FOLLOW).add(aim.scale(FOLLOW)).normalize()
                : aim;
        this.setPos(from);
        Vec3 far = from.add(this.way.scale(BlazeCasts.RANGE));
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(from, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.SOURCE_ONLY, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
        LivingEntity struck = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(this, new AABB(from, end).inflate(1.0),
                entity -> BlazeCasts.hits(living, entity))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(from, end);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < best) {
                best = hit.get().distanceToSqr(from);
                struck = (LivingEntity) entity;
            }
        }
        if (struck != null) {
            end = from.add(this.way.scale(Math.sqrt(best)));
            this.chill(level, living, struck, end);
        } else if (wall.getType() == HitResult.Type.BLOCK) {
            this.freezeWater(level, wall.getBlockPos());
        }
        this.entityData.set(END, end.toVector3f());
        if (this.tickCount % 2 == 0) {
            ParticleFx.send(level, ParticleTypes.SNOWFLAKE, end.x, end.y, end.z, 3, 0.2, 0.2, 0.2, 0.03);
            ParticleFx.send(level, ParticleFx.dust(Element.ICE.light, 1.0F), end.x, end.y, end.z, 2, 0.15, 0.15, 0.15,
                    0.0);
        }
        if (this.tickCount % 8 == 0) {
            level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F,
                    1.4F + this.random.nextFloat() * 0.4F);
        }
    }

    // One more tick in the beam: once long enough, hurt and frozen.
    private void chill(ServerLevel level, LivingEntity caster, LivingEntity struck, Vec3 at) {
        int id = struck.getId();
        int ticks = this.held.merge(id, 1, Integer::sum);
        int needed = caster instanceof ElementalBlaze ? BlazeTimeline.FREEZE : PLAYER_HOLD;
        if (ticks < needed || this.frozen.contains(id)) {
            return;
        }
        this.frozen.add(id);
        if (!struck.hurt(BlazeCasts.source(this, caster, Element.ICE), this.damage)) {
            return;
        }
        struck.addEffect(new MobEffectInstance(Frozen.FROZEN, FROZEN_FOR, 0), caster);
        struck.setDeltaMovement(Vec3.ZERO);
        struck.hurtMarked = true;
        ParticleFx.send(level, ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 30, 0.4, 0.6, 0.4, 0.08);
        ParticleFx.send(level, ParticleFx.dust(0xFFFFFF, 1.2F), at.x, at.y, at.z, 14, 0.4, 0.6, 0.4, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 1.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0F, 0.9F);
    }

    // Still water the beam touches freezes over, as under frost-walking boots, and melts again.
    private void freezeWater(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos).getType() != Fluids.WATER || !level.getFluidState(pos).isSource()
                || !level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos).is(Blocks.WATER)) {
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
        level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(this.random, 60, 120));
    }

    // Flakes of frost along the beam.
    private void flakes() {
        Entity caster = this.caster();
        if (caster == null || this.random.nextInt(2) != 0) {
            return;
        }
        Vec3 from = BlazeCasts.tip(caster, 0.0F);
        // Not right at the staff of a player, where they fill the holder's view.
        Vec3 at = from.lerp(this.end(0.0F), caster instanceof ElementalBlaze ? this.random.nextDouble()
                : 0.25 + 0.75 * this.random.nextDouble());
        this.level().addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, (this.random.nextDouble() - 0.5) * 0.05,
                -0.02, (this.random.nextDouble() - 0.5) * 0.05);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
