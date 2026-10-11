package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.joml.Vector3f;

// A block torn out of the world and drawn to a point: it rises off where it lay and spirals in, faster and faster,
// and is gone when it gets there. Every game works its path out from where it set off, where it goes and when, so it
// never lags behind; the server moves the point along when what pulls it moves. Drawn by
// client/render/entity/TornBlockRenderer, tumbling and stretched out as it falls in.
public final class TornBlock extends Entity {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<TornBlock>> TYPE = TYPES.register("torn_block",
            () -> EntityType.Builder.<TornBlock>of(TornBlock::new, MobCategory.MISC).sized(0.98F, 0.98F)
                    .clientTrackingRange(8).updateInterval(20).noSave().noSummon().fireImmune()
                    .build("torn_block"));
    private static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.defineId(TornBlock.class,
            EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Vector3f> FROM = SynchedEntityData.defineId(TornBlock.class,
            EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> TO = SynchedEntityData.defineId(TornBlock.class,
            EntityDataSerializers.VECTOR3);
    // The world's clock when it set off, and how many ticks it takes.
    private static final EntityDataAccessor<Integer> BORN = SynchedEntityData.defineId(TornBlock.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TICKS = SynchedEntityData.defineId(TornBlock.class,
            EntityDataSerializers.INT);
    // How high it rises off the ground before it falls in.
    private static final double LIFT = 1.8;
    private static final double TURNS = 2.6;

    public TornBlock(EntityType<? extends TornBlock> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    // Takes the block at `pos` out of the world and sends it to `to`, there in `ticks`. The caller has asked whether
    // it may be broken.
    public static TornBlock tear(ServerLevel level, BlockPos pos, BlockState state, Vec3 to, int ticks) {
        TornBlock block = new TornBlock(TYPE.get(), level);
        Vec3 from = Vec3.atCenterOf(pos);
        block.entityData.set(STATE, state);
        block.entityData.set(FROM, from.toVector3f());
        block.entityData.set(TO, to.toVector3f());
        block.entityData.set(BORN, (int) level.getGameTime());
        block.entityData.set(TICKS, Math.max(4, ticks));
        block.setPos(from.x, from.y, from.z);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.addFreshEntity(block);
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, state), from.x, from.y + 0.5, from.z, 10,
                0.3, 0.1, 0.3, 0.1);
        return block;
    }

    // What pulls it has moved: it falls in there now.
    public void aim(Vec3 to) {
        Vector3f now = this.entityData.get(TO);
        if (to.distanceToSqr(now.x(), now.y(), now.z()) > 0.01) {
            this.entityData.set(TO, to.toVector3f());
        }
    }

    public BlockState state() {
        return this.entityData.get(STATE);
    }

    // How far along its way it is, 0 as it sets off and 1 when it is there.
    public double run(float partialTick) {
        return Mth.clamp(this.age(partialTick) / this.entityData.get(TICKS), 0.0, 1.0);
    }

    public double age(float partialTick) {
        return this.level().getGameTime() - (long) this.entityData.get(BORN) + partialTick;
    }

    // Where it is `run` of the way along: up off the ground first, then round the point and in, ever faster.
    public Vec3 at(double run) {
        Vector3f from = this.entityData.get(FROM);
        Vector3f to = this.entityData.get(TO);
        Vec3 point = new Vec3(to.x(), to.y(), to.z());
        double fall = Math.pow(run, 1.5);
        double turn = (this.getId() & 1) == 0 ? TURNS : -TURNS;
        Vec3 off = new Vec3(from.x() - to.x(), from.y() - to.y(), from.z() - to.z());
        Vec3 spun = Vectors.spin(off, Vectors.UP, turn * fall).scale(1.0 - fall);
        double lift = LIFT * Math.sin(Math.PI * Math.min(1.0, run * 1.6)) * (1.0 - fall);
        return point.add(spun).add(0.0, lift, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, Blocks.STONE.defaultBlockState());
        builder.define(FROM, new Vector3f());
        builder.define(TO, new Vector3f());
        builder.define(BORN, 0);
        builder.define(TICKS, 20);
    }

    @Override
    public void tick() {
        super.tick();
        double run = this.run(0.0F);
        Vec3 at = this.at(run);
        this.setPos(at.x, at.y, at.z);
        if (!this.level().isClientSide && run >= 1.0) {
            if (this.level() instanceof ServerLevel level) {
                ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, this.state()), at.x, at.y, at.z,
                        6, 0.1, 0.1, 0.1, 0.05);
            }
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
