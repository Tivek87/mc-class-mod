package nl.tivek.multiversepowers.creature.animal.crab;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.NeutralAnimal;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The crab of beaches and shores: a broad, flat, hard-shelled creature that scuttles sideways on eight legs, its eyes
// on stalks and two heavy claws before it. It lives on the sand and under the water alike, walking the sea floor (it
// does not swim, and breathes water through the `can_breathe_under_water` tag). It minds its own business: it raises
// and snaps its claws at a player who comes close, and pinches only one who hits it. At rest it picks at the sand
// with its claws, waves one in the air, and on sand digs itself in up to its eyes.
public final class Crab extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Crab>> TYPE = TYPES.register("crab",
            () -> EntityType.Builder.<Crab>of(Crab::new, MobCategory.CREATURE).sized(0.6F, 0.4F).eyeHeight(0.3F)
                    .clientTrackingRange(8).build("crab"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "crab_food"));
    public static final int WAVE = 1;
    public static final int SNAP = 2;
    public static final int BURROW = 3;
    public static final int FEED = 4;
    public static final int PINCH = 5;
    public static final int PINCH_END = 12;
    // When the claw of a pinch shuts.
    public static final int PINCH_HIT = 5;
    private static final SoundEvent HURT = Sounds.of("crab.hurt");
    private static final SoundEvent DEATH = Sounds.of("crab.death");
    private static final SoundEvent SNAPS = Sounds.of("crab.snap");
    private static final SoundEvent PINCHES = Sounds.of("crab.pinch");
    private static final SoundEvent STEP = Sounds.of("crab.step");

    public Crab(EntityType<? extends Crab> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Crab::onAttributes);
        modEventBus.addListener(Crab::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 10.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Crab::checkCrabSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // On the sand, gravel or mud of a shore, in daylight.
    private static boolean checkCrabSpawnRules(EntityType<Crab> type, LevelAccessor level, MobSpawnType spawn,
            BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL) || below.is(Blocks.MUD))
                && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected int kinds() {
        return PINCH;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case BURROW -> 18;
            case PINCH -> 3;
            default -> 6;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PinchGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6, crab -> crab.isBaby() ? DamageTypeTags.PANIC_CAUSES
                : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(5, new IdleActs(this, 140).inWater().add(FEED, 60, 140, 4).add(WAVE, 40, 70, 2)
                .add(SNAP, 40, 60, 5, Crab::playerNear).add(BURROW, 200, 500, 2, Crab::onSand));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    private static boolean onSand(WildAnimal crab) {
        return crab.level().getBlockState(crab.blockPosition().below()).is(BlockTags.SAND);
    }

    private static boolean playerNear(WildAnimal crab) {
        return crab.level().getNearestPlayer(crab.getX(), crab.getY(), crab.getZ(), 3.5,
                EntitySelector.NO_CREATIVE_OR_SPECTATOR) != null;
    }

    // Which way round it scuttles: its left side or its right side leading.
    public boolean leftward() {
        return this.getId() % 2 == 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.dig();
        } else if (this.act() == SNAP && this.tickCount % 9 == 0) {
            this.playSound(SNAPS, 0.5F, 1.5F + this.random.nextFloat() * 0.2F);
        }
    }

    // Sand thrown up while it works itself into the ground.
    private void dig() {
        double t = this.actAge(0.0F);
        if (this.act() != BURROW || t > 22 || this.tickCount % 2 != 0) {
            return;
        }
        BlockState ground = this.level().getBlockState(this.blockPosition().below());
        if (ground.isAir()) {
            return;
        }
        float a = this.random.nextFloat() * Mth.TWO_PI;
        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                this.getX() + Mth.cos(a) * 0.3, this.getY() + 0.1, this.getZ() + Mth.sin(a) * 0.3, 0.0, 0.05, 0.0);
    }

    // A crab does not swim: it walks the bottom, and the sea does not carry it off.
    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.9F;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(PINCHES, 0.7F, 1.2F);
        }
        return hit;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return TYPE.get().create(level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.2F)
                : group);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.06F, 1.6F);
    }

    // Goes for whoever hit it and shuts a claw on them.
    private final class PinchGoal extends MeleeAttackGoal {
        private int swing;

        PinchGoal() {
            super(Crab.this, 1.3, true);
        }

        @Override
        public boolean canUse() {
            return !Crab.this.isBaby() && super.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = Crab.this.getTarget();
            if (this.swing > 0 && --this.swing == 0 && target != null && Crab.this.isWithinMeleeAttackRange(target)) {
                Crab.this.doHurtTarget(target);
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = PINCH_HIT;
                Crab.this.start(PINCH, PINCH_END);
            }
        }

        @Override
        public void stop() {
            this.swing = 0;
            super.stop();
        }
    }
}
