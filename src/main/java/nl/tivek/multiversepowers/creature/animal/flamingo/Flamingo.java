package nl.tivek.multiversepowers.creature.animal.flamingo;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
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
import net.neoforged.neoforge.common.NeoForgeMod;
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

// The flamingo of warm shores and mangroves, in flocks: the American flamingo coral red, the greater flamingo pale
// pink with rose wings, both with black flight feathers and a bill bent sharply down, its tip black; their chicks grey
// down with a straight bill. It wades in the shallows on its long legs, keeps near its flock and stands on one leg to
// rest, its head laid on its back; it feeds with its head upside down in the water, sweeping it from side to side;
// preens, stretches a leg and a wing back together, honks with its neck up, and now and then one starts the flock's
// display, heads flagging and wings flashed open, and those near join in. It walks away from a player who comes close
// (not one who creeps up or holds out its food); hurt, run at, or with a hunter near, it runs across the ground or the
// water beating its wings and flies a short way, and the flock goes up with it.
public final class Flamingo extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Flamingo>> TYPE = TYPES.register("flamingo",
            () -> EntityType.Builder.<Flamingo>of(Flamingo::new, MobCategory.CREATURE).sized(0.7F, 1.8F)
                    .eyeHeight(1.65F).clientTrackingRange(10).build("flamingo"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "flamingo_food"));
    public static final int AMERICAN = 0;
    public static final int GREATER = 1;
    public static final int ONE_LEG = 1;
    public static final int FEED = 2;
    public static final int PREEN = 3;
    public static final int DISPLAY = 4;
    public static final int HONK = 5;
    public static final int STRETCH = 6;
    public static final int HONK_END = 58;
    public static final int DISPLAY_END = 80;
    // How big each is drawn against its design, which stands about two blocks tall.
    private static final float AMERICAN_SIZE = 0.85F;
    private static final float GREATER_SIZE = 0.92F;
    private static final float CHICK_SIZE = 0.46F;
    // Where the water lies on it, in the design's pixels above its feet: standing, no higher than the lower leg, just
    // under the joint halfway up; afloat where it cannot stand, just over its belly.
    private static final double KNEE = 6.3;
    private static final double CHICK_KNEE = 3.75;
    private static final double BELLY = 16.0;
    private static final double CHICK_BELLY = 10.0;
    static final SoundEvent HONKS = Sounds.of("flamingo.honk");
    private static final SoundEvent CHORUS = Sounds.of("flamingo.display");
    private static final SoundEvent AMBIENT = Sounds.of("flamingo.ambient");
    private static final SoundEvent HURT = Sounds.of("flamingo.hurt");
    private static final SoundEvent DEATH = Sounds.of("flamingo.death");
    static final SoundEvent TAKEOFF = Sounds.of("flamingo.takeoff");
    private static final SoundEvent STEP = Sounds.of("flamingo.step");
    public float flap;
    public float oFlap;
    // How far its wings are spread, 0 folded to 1 in flight.
    public float spread;
    public float oSpread;
    // How far it lies afloat, 0 standing to 1 floating.
    public float swim;
    public float oSwim;
    private int airborne;
    int groundedUntil;
    // Where something the flock fled from was, told by the flamingo that went up first.
    @Nullable
    Vec3 alarm;

    public Flamingo(EntityType<? extends Flamingo> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        this.getNavigation().setCanFloat(true);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Flamingo::onAttributes);
        modEventBus.addListener(Flamingo::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 16.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Flamingo::checkFlamingoSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // On the sand, mud or grass of a shore with water close by, in daylight.
    private static boolean checkFlamingoSpawnRules(EntityType<Flamingo> type, LevelAccessor level, MobSpawnType spawn,
            BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(BlockTags.SAND) || below.is(Blocks.MUD) || below.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
        return ground && isBrightEnoughToSpawn(level, pos) && (spawn != MobSpawnType.NATURAL
                && spawn != MobSpawnType.CHUNK_GENERATION || waterNear(level, pos));
    }

    private static boolean waterNear(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                for (int dy = -2; dy <= 0; dy++) {
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getFluidState(at).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Water a flamingo stands in: a block of it on firm ground, with air over it.
    static boolean shallow(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos.above()).isAir();
    }

    // How big this one is drawn against the design.
    public float size() {
        float kind = this.variant() == GREATER ? GREATER_SIZE : AMERICAN_SIZE;
        return this.isBaby() ? kind * CHICK_SIZE : kind;
    }

    // Too deep to stand in: more than a block of water over the ground under it, which it floats on instead.
    private boolean deep() {
        double surface = this.getY() + this.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value());
        BlockPos.MutableBlockPos at = this.blockPosition().mutable();
        for (int k = 0; k < 3 && this.level().getFluidState(at).is(FluidTags.WATER); k++) {
            at.move(Direction.DOWN);
        }
        return this.level().getFluidState(at).is(FluidTags.WATER) || surface - (at.getY() + 1) > 1.05;
    }

    public boolean wading() {
        return this.isInWater() && this.swim < 0.5F;
    }

    @Override
    protected int kinds() {
        return STRETCH;
    }

    @Override
    protected int fade(int kind) {
        return kind == ONE_LEG ? 16 : kind == FEED ? 12 : 8;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FlamingoFlight(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.0, 1.5, this::shiesFrom));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(5, new IdleActs(this, 100).inWater()
                .add(ONE_LEG, 240, 700, 4, a -> !((Flamingo) a).floating())
                .add(FEED, 80, 200, 8, a -> ((Flamingo) a).wading())
                .add(PREEN, 60, 110, 3, a -> !((Flamingo) a).floating())
                .add(DISPLAY, DISPLAY_END, DISPLAY_END, 1, a -> !a.isBaby() && !((Flamingo) a).floating())
                .add(HONK, HONK_END, HONK_END, 2)
                .add(STRETCH, 40, 50, 2, a -> !((Flamingo) a).floating()));
        this.goalSelector.addGoal(6, new Hold());
        this.goalSelector.addGoal(7, new Wade());
        this.goalSelector.addGoal(8, new Flock());
        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                // In the shallows Wade picks where it goes.
                return !Flamingo.this.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
    }

    private boolean floating() {
        return this.isInWater() && this.swim >= 0.5F;
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    private boolean scaredOf(LivingEntity living) {
        if (living instanceof Wolf || living instanceof Fox || living instanceof Ocelot || living instanceof Lynx
                || living instanceof Crocodile) {
            return true;
        }
        return living instanceof Player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)
                && living.isSprinting();
    }

    // Where what scares it into the air is: whoever just hurt it, a hunter near it, a player running at it, or what
    // the flock just fled from.
    @Nullable
    Vec3 scare() {
        if (this.hurtTime > 0) {
            LivingEntity by = this.getLastHurtByMob();
            return by != null ? by.position() : this.position().subtract(this.getLookAngle());
        }
        if (this.alarm != null) {
            Vec3 from = this.alarm;
            this.alarm = null;
            return from;
        }
        if (this.tickCount % 5 != 0) {
            return null;
        }
        for (LivingEntity near : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(
                6.0, 2.0, 6.0), this::scaredOf)) {
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
        // Its takeoff run, told by how fast it goes, which every game sees alike: wings half open and beating.
        double pace = Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo)
                + (this.getZ() - this.zo) * (this.getZ() - this.zo));
        boolean takeoff = !flying && pace > 0.24;
        float want = this.airborne > 3 ? 1.0F : takeoff ? 0.75F : 0.0F;
        this.spread = Mth.clamp(this.spread + Mth.clamp(want - this.spread, -0.15F, 0.2F), 0.0F, 1.0F);
        boolean afloat = wet && this.deep();
        this.swim = Mth.clamp(this.swim + (afloat ? 0.1F : -0.1F), 0.0F, 1.0F);
        if (flying || takeoff) {
            this.flap += takeoff ? 0.7F : 0.55F;
        }
        if (!this.level().isClientSide) {
            boolean chick = this.isBaby();
            double line = afloat ? (chick ? CHICK_BELLY : BELLY) : chick ? CHICK_KNEE : KNEE;
            Afloat.buoy(this, line * this.size() / 16.0);
            Vec3 motion = this.getDeltaMovement();
            if (flying && motion.y < 0.0) {
                this.setDeltaMovement(motion.multiply(1.0, 0.7, 1.0));
            }
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.9F;
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == HONK) {
            this.playSound(HONKS, 1.0F, this.getVoicePitch());
        } else if (kind == DISPLAY) {
            this.playSound(CHORUS, 1.0F, this.getVoicePitch());
            this.rally();
        }
    }

    // The flock's display spreads: those near that stand about take it up too.
    private void rally() {
        if (this.level().isClientSide) {
            return;
        }
        for (Flamingo near : this.level().getEntitiesOfClass(Flamingo.class, this.getBoundingBox().inflate(10.0, 3.0,
                10.0), f -> f != this && f.act() == 0 && !f.isBaby() && f.onGround() && f.spread == 0.0F)) {
            near.getNavigation().stop();
            near.join(DISPLAY);
        }
    }

    private void join(int kind) {
        super.start(kind, kind == DISPLAY ? DISPLAY_END : 0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Flamingo chick = TYPE.get().create(level);
        if (chick != null) {
            chick.setVariant(other instanceof Flamingo mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return chick;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawn, group == null
                ? new FlockData(this.random.nextInt(3) == 0 ? GREATER : AMERICAN) : group);
        this.setVariant(data instanceof FlockData flock ? flock.variant : this.random.nextInt(2));
        return data;
    }

    // One kind to a flock.
    private static final class FlockData extends AgeableMob.AgeableMobGroupData {
        private final int variant;

        FlockData(int variant) {
            super(0.2F);
            this.variant = variant;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.act() == ONE_LEG ? null : AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
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
        return 0.7F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.15F, 1.0F);
    }

    // Stands still through an act it was drawn into by another (the display), which no goal of its own started.
    private final class Hold extends Goal {
        Hold() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Flamingo.this.act() != 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Flamingo.this.act() != 0 && Flamingo.this.hurtTime == 0;
        }

        @Override
        public void start() {
            Flamingo.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            Flamingo.this.end(Flamingo.this.act());
        }
    }

    // Wades: by day it makes for the shallows near it now and then, and in them walks slowly from spot to spot.
    private final class Wade extends Goal {
        @Nullable
        private BlockPos spot;

        Wade() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Flamingo f = Flamingo.this;
            boolean wet = f.isInWater();
            int every = wet ? (f.floating() ? 10 : 160) : 360;
            if (f.getRandom().nextInt(reducedTickDelay(every)) != 0) {
                return false;
            }
            this.spot = this.pick(wet ? 6 : 12);
            return this.spot != null;
        }

        @Nullable
        private BlockPos pick(int reach) {
            Flamingo f = Flamingo.this;
            BlockPos at = f.blockPosition();
            for (int tries = 0; tries < 16; tries++) {
                BlockPos spot = at.offset(f.random.nextInt(2 * reach + 1) - reach, f.random.nextInt(5) - 2,
                        f.random.nextInt(2 * reach + 1) - reach);
                if (shallow(f.level(), spot)) {
                    return spot;
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return !Flamingo.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos to = this.spot;
            if (to != null) {
                Flamingo.this.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.75);
            }
        }
    }

    // Keeps with its flock: strayed from every other, it walks back to the nearest.
    private final class Flock extends Goal {
        @Nullable
        private Flamingo other;

        Flock() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Flamingo f = Flamingo.this;
            if (f.getRandom().nextInt(reducedTickDelay(80)) != 0) {
                return false;
            }
            Flamingo nearest = null;
            double best = Double.MAX_VALUE;
            for (Flamingo near : f.level().getEntitiesOfClass(Flamingo.class, f.getBoundingBox().inflate(24.0, 6.0,
                    24.0), near -> near != f)) {
                double d = near.distanceToSqr(f);
                if (d < best) {
                    best = d;
                    nearest = near;
                }
            }
            this.other = best > 49.0 ? nearest : null;
            return this.other != null;
        }

        @Override
        public boolean canContinueToUse() {
            Flamingo to = this.other;
            return to != null && to.isAlive() && Flamingo.this.distanceToSqr(to) > 9.0
                    && !Flamingo.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            Flamingo to = this.other;
            if (to != null) {
                Flamingo.this.getNavigation().moveTo(to, 1.0);
            }
        }

        @Override
        public void stop() {
            this.other = null;
        }
    }
}
