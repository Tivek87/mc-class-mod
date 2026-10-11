package nl.tivek.multiversepowers.creature.rustbound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Rustbound: a gaunt zombie grown through with rusted scrap, plates of it riveted to its back, a heavy fist of it
// on its right arm, and in its chest a magnet core that glows. Scrap circles round it. Its core pulls whatever stands
// before it in (the more metal worn and held, the harder) and may tear a metal weapon or shield out of a hand; it flings
// its circling scrap, which leaves wounds that bleed; its fist knocks a raised shield aside. Wet (rain, water) its core
// only fizzles and pulls nothing.
public final class Rustbound extends MovingMonster {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Rustbound>> TYPE = TYPES.register("rustbound",
            () -> EntityType.Builder.<Rustbound>of(Rustbound::new, MobCategory.MONSTER).sized(0.75F, 2.05F)
                    .eyeHeight(1.75F).clientTrackingRange(10).build("rustbound"));
    public static final DeferredHolder<EntityType<?>, EntityType<RustShard>> SHARD = TYPES.register("rust_shard",
            () -> EntityType.Builder.<RustShard>of(RustShard::new, MobCategory.MISC).sized(0.3F, 0.3F)
                    .clientTrackingRange(6).updateInterval(2).build("rust_shard"));
    // What its core pulls on: armor, weapons and tools of iron, gold, chain or netherite, and the like.
    public static final TagKey<Item> METAL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "metal"));
    public static final int MOST_SHARDS = 6;
    public static final int CORE = 0x7FC8FF;
    // An iron golem is all metal.
    private static final int GOLEM_METAL = 8;
    private static final int REGROW = 50;
    private static final EntityDataAccessor<Integer> SHARDS = SynchedEntityData.defineId(Rustbound.class,
            EntityDataSerializers.INT);

    public Rustbound(EntityType<? extends Rustbound> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Rustbound::onAttributes);
        modEventBus.addListener(Rustbound::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.FOLLOW_RANGE, 32.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return RustboundMoves.KINDS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHARDS, MOST_SHARDS);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shards", this.shards());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shards")) {
            this.entityData.set(SHARDS, Math.max(0, Math.min(MOST_SHARDS, tag.getInt("Shards"))));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RustboundMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // How much scrap circles round it now.
    public int shards() {
        return this.entityData.get(SHARDS);
    }

    void useShards(int count) {
        this.entityData.set(SHARDS, Math.max(0, this.shards() - count));
    }

    // Wet, its core only fizzles.
    public boolean wet() {
        return this.isInWaterRainOrBubble();
    }

    // How many things of metal `living` wears and holds; an iron golem counts as all metal.
    public static int metal(LivingEntity living) {
        if (living instanceof IronGolem) {
            return GOLEM_METAL;
        }
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.BODY && living.getItemBySlot(slot).is(METAL)) {
                count++;
            }
        }
        return count;
    }

    // Where its core sits in the world now.
    public Vec3 core() {
        return this.position().add(MonsterBlows.ahead(this).scale(0.3)).add(0.0, 1.3, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
            return;
        }
        if (this.tickCount % REGROW == 0 && this.shards() < MOST_SHARDS && this.move() != RustboundMoves.SHRAPNEL) {
            this.entityData.set(SHARDS, this.shards() + 1);
        }
        Vec3 core = this.core();
        if (this.wet()) {
            if (this.tickCount % 6 == 0) {
                ParticleFx.send(level, ParticleTypes.SMOKE, core.x, core.y, core.z, 2, 0.08, 0.08, 0.08, 0.02);
                ParticleFx.send(level, ParticleTypes.CLOUD, core.x, core.y + 0.1, core.z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            if (this.tickCount % 40 == 0) {
                this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.6F);
            }
        } else if (this.tickCount % 14 == 0) {
            ParticleFx.send(level, ParticleFx.dust(CORE, 0.6F), core.x, core.y, core.z, 2, 0.12, 0.12, 0.12, 0.0);
        }
        // Flakes of rust fall from it.
        if (this.tickCount % 11 == 0) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.RED_SAND
                    .defaultBlockState()), this.getX(), this.getY() + 1.2, this.getZ(), 1, 0.25, 0.4, 0.25, 0.0);
        }
    }

    // Killed, its core goes out with a crack and the scrap round it falls.
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            Vec3 core = this.core();
            ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z, 24, 0.2, 0.2, 0.2, 0.3);
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                    core.x, core.y, core.z, 10 + this.shards() * 4, 0.6, 0.4, 0.6, 0.15);
            level.playSound(null, this.blockPosition(), SoundEvents.CHAIN_BREAK, this.getSoundSource(), 1.4F, 0.6F);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.3F, 0.8F);
        this.playSound(SoundEvents.CHAIN_STEP, 0.4F, 0.7F);
    }
}
