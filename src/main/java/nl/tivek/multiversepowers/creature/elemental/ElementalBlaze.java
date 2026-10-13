package nl.tivek.multiversepowers.creature.elemental;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.elemental.spell.FrostRay;
import nl.tivek.multiversepowers.creature.elemental.spell.Frozen;
import nl.tivek.multiversepowers.creature.elemental.spell.HeavedStone;
import nl.tivek.multiversepowers.creature.elemental.spell.ScorchBall;
import nl.tivek.multiversepowers.creature.elemental.spell.StormArc;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Elemental Blaze: a rare mini boss of the caves, a blaze whose fifteen rods each carry one of five elements
// (earth, fire, ice, wind, storm). It floats at a distance and casts one element after another, the rod of each
// swinging to the front first (BlazeMoves, BlazeCasts); its rods turn part of every blow aside (the Rod Ward); at half
// health it awakens into its second phase and from then on casts two elements one right after the other (the Elemental
// Surge). Its boss bar takes the colour of the element in use. Killed, it drops one of its rods. In every other way a
// blaze: its sounds, water and snowballs hurting it, fire not; no Blaze by class, as that one smokes too thickly to see
// its rods and climbs by itself.
public final class ElementalBlaze extends Monster {
    private static final float SNOWBALL = 3.0F;
    private static final float AIR_PACE = 0.05F;
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalBlaze>> TYPE = TYPES.register(
            "elemental_blaze", () -> EntityType.Builder.<ElementalBlaze>of(ElementalBlaze::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(10).build("elemental_blaze"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostRay>> FROST_RAY = TYPES.register("frost_ray",
            () -> EntityType.Builder.<FrostRay>of(FrostRay::new, MobCategory.MISC).sized(0.2F, 0.2F).noSave()
                    .clientTrackingRange(8).updateInterval(1).build("frost_ray"));
    public static final DeferredHolder<EntityType<?>, EntityType<HeavedStone>> HEAVED_STONE = TYPES.register(
            "heaved_stone", () -> EntityType.Builder.<HeavedStone>of(HeavedStone::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F).clientTrackingRange(8).updateInterval(1).build("heaved_stone"));
    public static final DeferredHolder<EntityType<?>, EntityType<ScorchBall>> SCORCH_BALL = TYPES.register(
            "scorch_ball", () -> EntityType.Builder.<ScorchBall>of(ScorchBall::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2).build("scorch_ball"));
    public static final DeferredHolder<EntityType<?>, EntityType<StormArc>> STORM_ARC = TYPES.register("storm_arc",
            () -> EntityType.Builder.<StormArc>of(StormArc::new, MobCategory.MISC).sized(0.2F, 0.2F).noSave()
                    .clientTrackingRange(8).updateInterval(1).build("storm_arc"));
    // Its rods turn this share of a blow aside.
    private static final float WARD = 0.2F;
    private static final double BAR_RANGE = 32.0;
    private static final int FIGHT_MEMORY = 200;
    private static final byte FIRST_EVENT = Byte.MIN_VALUE;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(ElementalBlaze.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementalBlaze.class,
            EntityDataSerializers.INT);

    private final ServerBossEvent bar = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_6);
    private final float[] rod = new float[6];
    private int move;
    private int moveAt;
    private int awokeAt = -1;
    private int foughtAt = -FIGHT_MEMORY;
    private boolean awakenDue;

    public ElementalBlaze(EntityType<? extends ElementalBlaze> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        Frozen.register(modEventBus);
        modEventBus.addListener(ElementalBlaze::onAttributes);
        modEventBus.addListener(ElementalBlaze::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 72.0)
                .add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 9.6).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FLYING_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ElementalBlaze::maySpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // Only deep in a cave, in the dark, where there is room to float, and never near another of its kind: rare.
    private static boolean maySpawn(EntityType<ElementalBlaze> type, ServerLevelAccessor level, MobSpawnType reason,
            BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.TRIAL_SPAWNER) {
            return true;
        }
        if (pos.getY() > 32 || level.canSeeSky(pos) || random.nextInt(3) != 0) {
            return false;
        }
        for (int up = 0; up < 4; up++) {
            if (!level.getBlockState(pos.above(up)).getCollisionShape(level, pos.above(up)).isEmpty()) {
                return false;
            }
        }
        return level.getEntitiesOfClass(ElementalBlaze.class, new AABB(pos).inflate(64.0)).isEmpty();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BlazeMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(ELEMENT, Element.FIRE.ordinal());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) {
            this.awokeAt = this.phase() >= 2 ? this.tickCount : -1;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", this.phase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getInt("Phase") >= 2) {
            this.entityData.set(PHASE, 2);
        }
        this.bar.setName(this.getDisplayName());
        // Loading puts its weight back, and it would sink until it next flies somewhere.
        this.setNoGravity(true);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bar.setName(this.getDisplayName());
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    // The element it uses now, or last used.
    public Element element() {
        return Element.of(this.entityData.get(ELEMENT));
    }

    // The move it makes now in this game (0 none) and how many ticks it is into it.
    public int move() {
        return this.move;
    }

    public float moveAge(float partialTick) {
        return this.tickCount - this.moveAt + partialTick;
    }

    // Starts a move here and in every game that sees it (0 ends it).
    void start(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
        Element element = BlazeTimeline.element(kind);
        if (element != null) {
            this.entityData.set(ELEMENT, element.ordinal());
            this.bar.setColor(element.bar);
        }
        this.level().broadcastEntityEvent(this, (byte) (FIRST_EVENT + kind));
    }

    // Shows a move in this game alone (one drawn in a screen).
    public void play(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
        Element element = BlazeTimeline.element(kind);
        if (element != null) {
            this.entityData.set(ELEMENT, element.ordinal());
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= FIRST_EVENT && id <= FIRST_EVENT + BlazeTimeline.KINDS) {
            this.move = id - FIRST_EVENT;
            this.moveAt = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    // How its rods stand at this moment, for the model and the particles they shed.
    public RodOrbit.State orbit(float partialTick) {
        float age = this.tickCount + partialTick;
        float awake = this.awokeAt < 0 ? -1.0F : age - this.awokeAt;
        float dying = this.deathTime > 0 ? this.deathTime + partialTick : 0.0F;
        float hurt = this.hurtTime > 0 ? Mth.clamp((this.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F) : 0.0F;
        return new RodOrbit.State(age, this.move, this.moveAge(partialTick), awake, dying, hurt);
    }

    // Where the leading rod of its cast is in the world: what its spells fly from.
    public Vec3 emitter(float partialTick) {
        RodOrbit.place(this.orbit(partialTick), RodOrbit.leader(this.element()), this.rod);
        Vec3 at = this.getPosition(partialTick);
        return RodOrbit.world(this.rod, at, Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot));
    }

    boolean awakenDue() {
        return this.awakenDue;
    }

    void awaken() {
        this.awakenDue = false;
        this.entityData.set(PHASE, 2);
        this.start(BlazeTimeline.AWAKEN);
    }

    boolean awakening() {
        return this.move == BlazeTimeline.AWAKEN && this.moveAge(0.0F) < BlazeTimeline.BURST;
    }

    // The Rod Ward: its rods turn a share of every blow aside, sparking where they catch it; while it awakens nothing
    // gets through.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean always = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!always && this.awakening()) {
            if (this.level() instanceof ServerLevel level && this.tickCount % 4 == 0) {
                this.sparks(level, source, 6);
            }
            return false;
        }
        float blow = source.getDirectEntity() instanceof Snowball ? Math.max(amount, SNOWBALL) : amount;
        float dealt = always ? blow : blow * (1.0F - WARD);
        boolean hit = super.hurt(source, dealt);
        if (hit && !always && this.level() instanceof ServerLevel level) {
            this.sparks(level, source, 10);
            this.foughtAt = this.tickCount;
            if (this.phase() < 2 && this.getHealth() <= this.getMaxHealth() * 0.5F && this.isAlive()) {
                this.awakenDue = true;
            }
        }
        return hit;
    }

    private void sparks(ServerLevel level, DamageSource source, int count) {
        Vec3 from = source.getSourcePosition();
        Vec3 middle = this.getBoundingBox().getCenter();
        Vec3 at = from == null ? middle : middle.add(from.subtract(middle).normalize().scale(0.6));
        Element element = Element.of(this.random.nextInt(Element.ALL.length));
        ParticleFx.send(level, ParticleFx.dust(element.light, 0.9F), at.x, at.y, at.z, count, 0.2, 0.2, 0.2, 0.0);
        ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count / 2, 0.15, 0.15, 0.15, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, this.getSoundSource(), 0.9F,
                0.7F + this.random.nextFloat() * 0.3F);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(Frozen.FROZEN) && super.canBeAffected(effect);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.isAlive()) {
            this.shed();
        }
    }

    // Its rods shed their elements as they circle: the rod of the element it casts much more.
    private void shed() {
        RodOrbit.State state = this.orbit(0.0F);
        Element casting = BlazeTimeline.element(this.move);
        for (int i = 0; i < RodOrbit.RODS; i++) {
            Element own = RodOrbit.element(i);
            float glow = RodOrbit.glow(state, own, RodOrbit.ring(i));
            if (this.random.nextFloat() > 0.04F + 0.35F * glow) {
                continue;
            }
            RodOrbit.place(state, i, this.rod);
            Vec3 at = RodOrbit.world(this.rod, this.position(), this.yBodyRot);
            this.level().addParticle(own.particle(), at.x + (this.random.nextDouble() - 0.5) * 0.15, at.y
                    - 0.2 + (this.random.nextDouble() - 0.5) * 0.3, at.z + (this.random.nextDouble() - 0.5) * 0.15,
                    0.0, own == Element.FIRE ? 0.02 : 0.0, 0.0);
            if (own == casting && glow > 0.5F && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleFx.dust(own.light, 1.1F), at.x, at.y - 0.2, at.z, 0.0, 0.0, 0.0);
            }
        }
        if (this.awokeAt >= 0 && this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.6), this.getRandomY(),
                    this.getRandomZ(0.6), 0.0, 0.03, 0.0);
        }
        if (this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.3), this.getY() + this.getBbHeight()
                    + 0.1, this.getRandomZ(0.3), 0.0, 0.02, 0.0);
        }
        if (this.random.nextInt(24) == 0 && !this.isSilent()) {
            this.level().playLocalSound(this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5,
                    SoundEvents.BLAZE_BURN, this.getSoundSource(), 1.0F + this.random.nextFloat(),
                    this.random.nextFloat() * 0.7F + 0.3F, false);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 10 != 0) {
            return;
        }
        this.bar.setProgress(this.getHealth() / this.getMaxHealth());
        boolean fighting = this.getTarget() != null || this.tickCount - this.foughtAt < FIGHT_MEMORY;
        Set<ServerPlayer> near = new HashSet<>();
        if (fighting && this.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(this) < BAR_RANGE * BAR_RANGE && !player.isSpectator()
                        && (player == this.getTarget() || this.hasLineOfSight(player))) {
                    near.add(player);
                }
            }
        }
        for (ServerPlayer player : Set.copyOf(this.bar.getPlayers())) {
            if (!near.contains(player)) {
                this.bar.removePlayer(player);
            }
        }
        near.forEach(this.bar::addPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bar.removeAllPlayers();
    }

    // Killed, its rods burst apart in all five of their elements.
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = this.getBoundingBox().getCenter();
            for (Element element : Element.ALL) {
                ParticleFx.sphereOut(level, ParticleFx.dust(element.light, 1.3F), at, 14, 0.25);
                ParticleFx.send(level, element.particle(), at.x, at.y, at.z, 10, 0.5, 0.6, 0.5, 0.05);
            }
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 0.4, 0.5, 0.4, 0.04);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 0.8F,
                    1.4F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, this.getSoundSource(), 1.5F,
                    0.6F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + this.random.nextFloat() * 0.1F;
    }

    // The game's own pace in the air (0.02) leaves a flyer drifting at a walk.
    @Override
    protected float getFlyingSpeed() {
        return AIR_PACE;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }
}
