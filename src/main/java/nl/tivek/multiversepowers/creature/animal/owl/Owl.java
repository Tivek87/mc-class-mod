package nl.tivek.multiversepowers.creature.animal.owl;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The owl of the woods: a brown tawny owl, and in snowy lands a white snowy owl. By day it flies up into a tree and
// sleeps there, eyes shut and feathers fluffed, until dusk or until a player walks up to it; by night it is awake,
// flying without a sound from perch to perch, turning its head far round, preening and hooting. It keeps its distance
// from players (not one who creeps up or holds out its food). Its flight is the game's own, as a parrot flies.
public final class Owl extends WildAnimal implements FlyingAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Owl>> TYPE = TYPES.register("owl",
            () -> EntityType.Builder.<Owl>of(Owl::new, MobCategory.CREATURE).sized(0.5F, 0.8F).eyeHeight(0.65F)
                    .clientTrackingRange(10).build("owl"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "owl_food"));
    public static final int TAWNY = 0;
    public static final int SNOWY = 1;
    public static final int SLEEP = 1;
    public static final int HOOT = 2;
    public static final int TURN = 3;
    public static final int PREEN = 4;
    public static final int HOOT_END = 46;
    // On a branch (leaves), where parrots and ocelots alone may spawn by the game's own rule, or on the ground.
    private static final SpawnPlacementType PERCH = (level, pos, type) -> level.getBlockState(pos.below())
            .is(BlockTags.LEAVES) ? level.getBlockState(pos).isAir()
                    : SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, type);
    private static final SoundEvent HOOTING = Sounds.of("owl.hoot");
    private static final SoundEvent HURT = Sounds.of("owl.hurt");
    private static final SoundEvent DEATH = Sounds.of("owl.death");
    public float flap;
    public float oFlap;
    // How far its wings are spread, 0 folded to 1 in flight.
    public float spread;
    public float oSpread;

    public Owl(EntityType<? extends Owl> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Owl::onAttributes);
        modEventBus.addListener(Owl::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.45).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), PERCH, Heightmap.Types.MOTION_BLOCKING, Owl::checkOwlSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean checkOwlSpawnRules(EntityType<Owl> type, LevelAccessor level, MobSpawnType spawn,
            BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS) || below.is(BlockTags.WOLVES_SPAWNABLE_ON))
                && Animal.isBrightEnoughToSpawn(level, pos);
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
    protected int kinds() {
        return PREEN;
    }

    @Override
    protected int fade(int kind) {
        return kind == SLEEP ? 16 : 6;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.0, 1.25, this::shiesFrom));
        this.goalSelector.addGoal(3, new RoostGoal());
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), true));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new IdleActs(this, 90).add(HOOT, HOOT_END, HOOT_END, 3,
                owl -> owl.level().isNight()).add(TURN, 50, 90, 3).add(PREEN, 50, 90, 2));
        this.goalSelector.addGoal(8, new PerchGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    public boolean isAsleep() {
        return this.act() == SLEEP;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.oFlap = this.flap;
        this.oSpread = this.spread;
        // A player's game only hears that it stands once it moves: look under it instead.
        boolean flying = !this.onGround() && !this.isPassenger()
                && this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.1, 0.0));
        this.spread = Mth.clamp(this.spread + (flying ? 0.2F : -0.15F), 0.0F, 1.0F);
        // Slow, deep beats, slower still while it glides down.
        Vec3 motion = this.getDeltaMovement();
        if (flying) {
            this.flap += motion.y < -0.05 ? 0.35F : 0.75F;
            // It glides: its fall held back most, its climb a little.
            this.setDeltaMovement(motion.multiply(1.0, motion.y < 0.0 ? 0.6 : 0.85, 1.0));
        }
    }

    // Twice a parrot's pace through the air.
    @Override
    protected float getFlyingSpeed() {
        return 0.04F;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == HOOT) {
            // A snowy owl's voice is deeper.
            this.playSound(HOOTING, 1.6F, (this.variant() == SNOWY ? 0.78F : 1.0F) * (this.isBaby() ? 1.4F : 1.0F)
                    + this.random.nextFloat() * 0.06F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Owl owlet = TYPE.get().create(level);
        if (owlet != null) {
            owlet.setVariant(other instanceof Owl mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return owlet;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        this.setVariant(level.getBiome(this.blockPosition()).is(Tags.Biomes.IS_SNOWY) ? SNOWY : TAWNY);
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.1F)
                : group);
    }

    // It hoots only as its own act, and flies without a sound.
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
    }

    private boolean onPerch() {
        BlockState below = this.level().getBlockState(this.getOnPos());
        return below.getBlock() instanceof LeavesBlock || below.is(BlockTags.LOGS);
    }

    // By day it sleeps where it has landed: at once on a branch, on the ground only when it has found none.
    private final class RoostGoal extends Goal {
        RoostGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        private boolean restful() {
            Owl owl = Owl.this;
            return owl.level().isDay() && owl.onGround() && !owl.isInWater() && owl.hurtTime == 0
                    && !owl.isInLove() && owl.level().getNearestPlayer(owl.getX(), owl.getY(), owl.getZ(), 4.0,
                            owl::shiesFromPlayer) == null;
        }

        @Override
        public boolean canUse() {
            return Owl.this.act() == 0 && this.restful()
                    && (Owl.this.onPerch() || Owl.this.random.nextInt(120) == 0);
        }

        @Override
        public boolean canContinueToUse() {
            return Owl.this.isAsleep() && this.restful();
        }

        @Override
        public void start() {
            Owl.this.getNavigation().stop();
            Owl.this.start(SLEEP, 24000);
        }

        @Override
        public void stop() {
            Owl.this.end(SLEEP);
        }
    }

    private boolean shiesFromPlayer(Entity entity) {
        return entity instanceof LivingEntity living && this.shiesFrom(living);
    }

    // Flies to another perch now and then: a branch near it if there is one, else anywhere near. By day, not yet
    // asleep, it looks for one much sooner.
    private final class PerchGoal extends WaterAvoidingRandomFlyingGoal {
        PerchGoal() {
            super(Owl.this, 1.0);
        }

        @Override
        public boolean canUse() {
            this.setInterval(Owl.this.level().isDay() ? 40 : 260);
            return super.canUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            Vec3 perch = Owl.this.random.nextInt(4) == 0 ? null : this.branch();
            return perch != null ? perch : super.getPosition();
        }

        @Nullable
        private Vec3 branch() {
            Level level = Owl.this.level();
            BlockPos at = Owl.this.blockPosition();
            BlockPos.MutableBlockPos spot = new BlockPos.MutableBlockPos();
            for (int tries = 0; tries < 24; tries++) {
                spot.set(at.getX() + Owl.this.random.nextInt(21) - 10, at.getY() + Owl.this.random.nextInt(13) - 4,
                        at.getZ() + Owl.this.random.nextInt(21) - 10);
                if (!level.isLoaded(spot) || !level.getBlockState(spot).isAir()
                        || !level.getBlockState(spot.above()).isAir()) {
                    continue;
                }
                BlockState below = level.getBlockState(spot.below());
                if (below.getBlock() instanceof LeavesBlock || below.is(BlockTags.LOGS)) {
                    return Vec3.atBottomCenterOf(spot);
                }
            }
            return null;
        }
    }
}
