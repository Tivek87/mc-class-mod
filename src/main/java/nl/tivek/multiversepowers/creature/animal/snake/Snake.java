package nl.tivek.multiversepowers.creature.animal.snake;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The snake of grass, woods and swamp: an olive green grass snake with a yellow collar, or a brown adder with a dark
// zigzag down its back. It slithers off about its own business; a player walking up to it (not one who creeps) makes
// it coil, raise its head and hiss. Stay close and it strikes once, then makes off; hit it and it strikes back. Only
// the adder's bite poisons. Left alone long enough, a warning snake gives way and leaves. At rest it basks in the
// sun or lifts its head to look about, tongue flicking.
public final class Snake extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Snake>> TYPE = TYPES.register("snake",
            () -> EntityType.Builder.<Snake>of(Snake::new, MobCategory.CREATURE).sized(0.5F, 0.25F)
                    .eyeHeight(0.15F).clientTrackingRange(8).build("snake"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "snake_food"));
    public static final int GRASS = 0;
    public static final int ADDER = 1;
    public static final int COIL = 1;
    public static final int STRIKE = 2;
    public static final int BASK = 3;
    public static final int PERISCOPE = 4;
    public static final int STRIKE_END = 12;
    // When the head of a strike reaches what it struck at.
    public static final int STRIKE_HIT = 4;
    private static final SoundEvent HISS = Sounds.of("snake.hiss");
    private static final SoundEvent HURT = Sounds.of("snake.hurt");
    private static final SoundEvent DEATH = Sounds.of("snake.death");
    private static final SoundEvent BITE = Sounds.of("snake.bite");
    private static final SoundEvent STEP = Sounds.of("snake.step");
    private int retreatUntil;

    public Snake(EntityType<? extends Snake> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Snake::onAttributes);
        modEventBus.addListener(Snake::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 8.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return PERISCOPE;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case STRIKE -> 2;
            case BASK -> 14;
            default -> 7;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BiteGoal());
        this.goalSelector.addGoal(2, new RetreatGoal());
        this.goalSelector.addGoal(3, new WarnGoal());
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Hedgehog.class, 6.0F, 1.2, 1.5));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(7, new IdleActs(this, 200).add(BASK, 200, 500, 3,
                snake -> snake.level().isDay()).add(PERISCOPE, 50, 90, 2));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // A player walking up in the open; one creeping past is let by.
    private boolean provokes(Entity entity) {
        return entity instanceof LivingEntity living && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)
                && !living.isDiscrete();
    }

    // Only the adder is venomous: its bite poisons for longer the harder the world is.
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(BITE, 0.7F, 1.3F);
            Difficulty difficulty = this.level().getDifficulty();
            int seconds = difficulty == Difficulty.HARD ? 10 : difficulty == Difficulty.NORMAL ? 6 : 4;
            if (this.variant() == ADDER && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0), this);
            }
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
        Snake young = TYPE.get().create(level);
        if (young != null) {
            young.setVariant(other instanceof Snake mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return young;
    }

    // Grass snakes keep to the wet lowlands, adders to taiga, hills and dry grass; elsewhere either.
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        Holder<Biome> biome = level.getBiome(this.blockPosition());
        int adder = biome.is(Tags.Biomes.IS_SWAMP) ? 0
                : biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_HILL) || biome.is(BiomeTags.IS_SAVANNA) ? 8
                        : 4;
        this.setVariant(this.random.nextInt(10) < adder ? ADDER : GRASS);
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.1F)
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
        this.playSound(STEP, 0.08F, 0.7F);
    }

    private void hiss() {
        this.playSound(HISS, 0.7F, 0.65F + this.random.nextFloat() * 0.15F);
    }

    // Coiled with its head up, hissing at a player who has come near. One who stays right by it is struck at; one
    // who keeps off is left be, and after a while it gives way itself.
    private final class WarnGoal extends Goal {
        @Nullable
        private Player threat;
        private int since;
        private int nearSince;
        private int nextHiss;

        WarnGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Nullable
        private Player near(double reach) {
            Snake snake = Snake.this;
            return snake.level().getNearestPlayer(snake.getX(), snake.getY(), snake.getZ(), reach, snake::provokes);
        }

        @Override
        public boolean canUse() {
            Snake snake = Snake.this;
            if (snake.isBaby() || snake.getTarget() != null || snake.tickCount < snake.retreatUntil
                    || !snake.onGround() || snake.isInWater()) {
                return false;
            }
            this.threat = this.near(3.5);
            return this.threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            Snake snake = Snake.this;
            return this.threat != null && this.threat.isAlive() && snake.provokes(this.threat)
                    && snake.distanceToSqr(this.threat) < 25.0 && snake.getTarget() == null && snake.hurtTime == 0
                    && snake.tickCount < this.since + 160;
        }

        @Override
        public void start() {
            Snake snake = Snake.this;
            this.since = snake.tickCount;
            this.nearSince = -1;
            this.nextHiss = 0;
            snake.getNavigation().stop();
            snake.start(COIL, 400);
        }

        @Override
        public void tick() {
            Snake snake = Snake.this;
            Player player = this.threat;
            if (player == null) {
                return;
            }
            snake.getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (snake.tickCount >= this.nextHiss) {
                snake.hiss();
                this.nextHiss = snake.tickCount + 36;
            }
            if (snake.distanceToSqr(player) > 4.0) {
                this.nearSince = -1;
            } else if (this.nearSince < 0) {
                this.nearSince = snake.tickCount;
            } else if (snake.tickCount - this.nearSince >= 24) {
                snake.setTarget(player);
            }
        }

        @Override
        public void stop() {
            Snake snake = Snake.this;
            snake.end(COIL);
            // Warned long enough with no answer: it gives way.
            if (snake.getTarget() == null && snake.tickCount >= this.since + 160) {
                snake.retreatUntil = snake.tickCount + 80;
            }
            this.threat = null;
        }
    }

    // Goes for what it is set on and strikes once, then lets it be and makes off.
    private final class BiteGoal extends MeleeAttackGoal {
        private int swing;

        BiteGoal() {
            super(Snake.this, 1.4, true);
        }

        @Override
        public boolean canUse() {
            return !Snake.this.isBaby() && super.canUse();
        }

        @Override
        public void tick() {
            Snake snake = Snake.this;
            LivingEntity target = snake.getTarget();
            if (this.swing > 0 && --this.swing == 0 && target != null && snake.isWithinMeleeAttackRange(target)
                    && snake.doHurtTarget(target)) {
                snake.retreatUntil = snake.tickCount + 100;
                snake.setTarget(null);
                return;
            }
            if (target != null && snake.distanceToSqr(target) > 49.0) {
                snake.setTarget(null);
                return;
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = STRIKE_HIT;
                Snake.this.start(STRIKE, STRIKE_END);
                Snake.this.hiss();
            }
        }

        @Override
        public void stop() {
            this.swing = 0;
            super.stop();
        }
    }

    // Makes off after a bite or a long warning; a young snake from anything that hurts it.
    private final class RetreatGoal extends PanicGoal {
        RetreatGoal() {
            super(Snake.this, 1.6, snake -> snake.isBaby() ? DamageTypeTags.PANIC_CAUSES
                    : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
        }

        @Override
        protected boolean shouldPanic() {
            return Snake.this.tickCount < Snake.this.retreatUntil || super.shouldPanic();
        }
    }
}
