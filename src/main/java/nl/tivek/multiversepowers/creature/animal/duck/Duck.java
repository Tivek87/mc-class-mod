package nl.tivek.multiversepowers.creature.animal.duck;

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
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.Afloat;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The duck of rivers, ponds and swamps, a mallard: the drake with his green head, white collar and chestnut breast, the
// hen mottled brown, their ducklings yellow and brown and following in a row. It waddles on land, makes for water
// and lies on it, paddling about, tipping up to dabble with its tail in the air, preening, stretching its wings and
// quacking. It walks away from a player who comes close (not one who creeps up or holds out its food); hurt, run at,
// or with a wolf, fox, lynx or crocodile near, it takes off, flies a little way and comes down again. It never falls
// hard: it flutters down.
public final class Duck extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Duck>> TYPE = TYPES.register("duck",
            () -> EntityType.Builder.<Duck>of(Duck::new, MobCategory.CREATURE).sized(0.45F, 0.5F).eyeHeight(0.42F)
                    .clientTrackingRange(10).build("duck"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "duck_food"));
    public static final int DRAKE = 0;
    public static final int HEN = 1;
    public static final int DABBLE = 1;
    public static final int PREEN = 2;
    public static final int STRETCH = 3;
    public static final int QUACK = 4;
    public static final int TUCK = 5;
    public static final int QUACK_END = 30;
    // How deep it lies in the water: the surface this far above its feet.
    private static final double FLOAT = 0.2;
    private static final SoundEvent QUACKS = Sounds.of("duck.quack");
    private static final SoundEvent AMBIENT = Sounds.of("duck.ambient");
    private static final SoundEvent HURT = Sounds.of("duck.hurt");
    private static final SoundEvent DEATH = Sounds.of("duck.death");
    private static final SoundEvent TAKEOFF = Sounds.of("duck.takeoff");
    private static final SoundEvent STEP = Sounds.of("duck.step");
    public float flap;
    public float oFlap;
    // How far its wings are spread, 0 folded to 1 in flight.
    public float spread;
    public float oSpread;
    // How far it lies afloat, 0 on land to 1 on the water.
    public float swim;
    public float oSwim;
    private int airborne;
    private int groundedUntil;

    public Duck(EntityType<? extends Duck> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.getNavigation().setCanFloat(true);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Duck::onAttributes);
        modEventBus.addListener(Duck::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 12.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Duck::checkDuckSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // On the grass, sand, gravel or mud of a bank, in daylight.
    private static boolean checkDuckSpawnRules(EntityType<Duck> type, LevelAccessor level, MobSpawnType spawn,
            BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL)
                || below.is(Blocks.MUD)) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected int kinds() {
        return TUCK;
    }

    @Override
    protected int fade(int kind) {
        return kind == TUCK ? 14 : kind == DABBLE ? 8 : 5;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FlightGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.0F, 1.0, 1.4, this::shiesFrom));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 120).afloat().add(DABBLE, 40, 90, 6, WildAnimal::isInWater)
                .add(PREEN, 50, 90, 2).add(STRETCH, 24, 36, 2).add(QUACK, QUACK_END, QUACK_END, 3)
                .add(TUCK, 200, 500, 1));
        this.goalSelector.addGoal(7, new Afloat.Paddle(this, 50, 1.4));
        this.goalSelector.addGoal(8, new Afloat.Seek(this, 300, 1.0));
        // Once on the water it stays a good while before it walks out again.
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                this.setInterval(Duck.this.isInWater() ? 700 : 120);
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    private boolean scaredOf(LivingEntity living) {
        if (living instanceof Wolf || living instanceof Fox || living instanceof Lynx
                || living instanceof Crocodile) {
            return true;
        }
        return living instanceof Player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)
                && living.isSprinting();
    }

    // Where what scares it into the air is: whoever just hurt it, a hunter near it, or a player running at it.
    @Nullable
    private Vec3 scare() {
        if (this.hurtTime > 0) {
            LivingEntity by = this.getLastHurtByMob();
            return by != null ? by.position() : this.position().subtract(this.getLookAngle());
        }
        if (this.tickCount % 5 != 0) {
            return null;
        }
        for (LivingEntity near : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(
                5.0, 2.0, 5.0), this::scaredOf)) {
            return near.position();
        }
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.oFlap = this.flap;
        this.oSpread = this.spread;
        this.oSwim = this.swim;
        boolean wet = this.isInWater();
        // A player's game only hears that it stands once it moves: look under it instead.
        boolean flying = !this.onGround() && !wet && !this.isPassenger()
                && this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.1, 0.0));
        this.airborne = flying ? this.airborne + 1 : 0;
        // A hop up a block is no flight: its wings open only once it has been off the ground a moment.
        this.spread = Mth.clamp(this.spread + (this.airborne > 3 ? 0.25F : -0.2F), 0.0F, 1.0F);
        this.swim = Mth.clamp(this.swim + (wet ? 0.15F : -0.15F), 0.0F, 1.0F);
        if (flying) {
            this.flap += 1.25F;
        }
        if (!this.level().isClientSide) {
            Afloat.buoy(this, FLOAT);
            Vec3 motion = this.getDeltaMovement();
            if (flying && motion.y < 0.0) {
                this.setDeltaMovement(motion.multiply(1.0, 0.6, 1.0));
            }
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.94F;
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == QUACK) {
            this.playSound(QUACKS, 1.0F, this.getVoicePitch());
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Duck duckling = TYPE.get().create(level);
        if (duckling != null) {
            duckling.setVariant(this.random.nextBoolean() ? DRAKE : HEN);
        }
        return duckling;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        this.setVariant(this.random.nextBoolean() ? DRAKE : HEN);
        return super.finalizeSpawn(level, difficulty, spawn, group == null
                ? new AgeableMob.AgeableMobGroupData(0.35F) : group);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.act() == TUCK ? null : AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
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
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.1F, 1.2F);
    }

    // Scared into the air: up and away from what scared it, level for a while, then down again. It flies on a
    // heading, not a path, turning aside from what it meets.
    private final class FlightGoal extends Goal {
        private int length;
        private int left;
        private float heading;

        FlightGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Duck duck = Duck.this;
            if (duck.isBaby() || duck.tickCount < duck.groundedUntil || duck.isLeashed()) {
                return false;
            }
            Vec3 from = duck.scare();
            if (from == null) {
                return false;
            }
            Vec3 away = duck.position().subtract(from);
            this.heading = (float) Mth.atan2(-away.x, away.z) + (duck.random.nextFloat() - 0.5F) * 1.0F;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.left > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Duck duck = Duck.this;
            this.length = 50 + duck.random.nextInt(40);
            this.left = this.length;
            duck.getNavigation().stop();
            duck.start(0, 0);
            duck.playSound(TAKEOFF, 0.7F, 1.3F);
            duck.playSound(AMBIENT, 1.0F, duck.getVoicePitch());
        }

        @Override
        public void tick() {
            Duck duck = Duck.this;
            int gone = this.length - this.left--;
            double up = gone < 14 ? 0.22 : this.left < 22 ? -0.1 : 0.0;
            if (duck.horizontalCollision) {
                this.heading += 0.6F;
                up = Math.max(up, 0.2);
            }
            double speed = 0.42 * Math.min(1.0, gone / 8.0);
            duck.setDeltaMovement(-Mth.sin(this.heading) * speed, up, Mth.cos(this.heading) * speed);
            float yaw = this.heading * Mth.RAD_TO_DEG;
            duck.setYRot(yaw);
            duck.yBodyRot = yaw;
            duck.yHeadRot = yaw;
            // Down again: it ends where it lands.
            if (this.left < 22 && (duck.onGround() || duck.isInWater())) {
                this.left = 0;
            }
        }

        @Override
        public void stop() {
            Duck.this.groundedUntil = Duck.this.tickCount + 80;
        }
    }
}
