package nl.tivek.multiversepowers.creature.animal.crocodile;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.Afloat;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.NeutralAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The crocodile of swamps and mangroves: long, low and armoured, olive above and pale below, with a long toothed
// snout and a heavy tail. On land it walks slowly on sprawled legs or lies on its belly basking with its jaws agape;
// in water it floats with only its eyes, nostrils and back above the surface and swims with its tail. It leaves
// players be until one walks up to it (not one who creeps): then it lifts its head, opens its jaws wide and hisses.
// Stay close or hit it and it lunges and bites, and keeps after you until you are well away.
public final class Crocodile extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Crocodile>> TYPE = TYPES.register("crocodile",
            () -> EntityType.Builder.<Crocodile>of(Crocodile::new, MobCategory.CREATURE).sized(1.3F, 0.6F)
                    .eyeHeight(0.45F).clientTrackingRange(10).build("crocodile"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "crocodile_food"));
    public static final int GAPE = 1;
    public static final int BITE = 2;
    public static final int BASK = 3;
    public static final int RISE = 4;
    public static final int BITE_END = 16;
    // When the jaws of a bite shut.
    public static final int BITE_HIT = 5;
    // It gathers itself between bites: slower than the game's one blow a second.
    private static final int BITE_EVERY = 36;
    // How deep it lies in the water: only its eyes and back above the surface.
    private static final double FLOAT = 0.42;
    private static final double REACH = 0.7;
    private static final SoundEvent HISS = Sounds.of("crocodile.hiss");
    private static final SoundEvent HURT = Sounds.of("crocodile.hurt");
    private static final SoundEvent DEATH = Sounds.of("crocodile.death");
    private static final SoundEvent BITES = Sounds.of("crocodile.bite");
    private static final SoundEvent STEP = Sounds.of("crocodile.step");
    // How far it lies afloat, 0 on land to 1 in the water.
    public float swim;
    public float oSwim;

    public Crocodile(EntityType<? extends Crocodile> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.getNavigation().setCanFloat(true);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Crocodile::onAttributes);
        modEventBus.addListener(Crocodile::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 14.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Crocodile::checkCrocodileSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // On the grass, mud or sand of a swamp's banks, in daylight.
    private static boolean checkCrocodileSpawnRules(EntityType<Crocodile> type, LevelAccessor level,
            MobSpawnType spawn, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(BlockTags.ANIMALS_SPAWNABLE_ON) || below.is(BlockTags.SAND) || below.is(Blocks.MUD))
                && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected int kinds() {
        return RISE;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case BITE -> 2;
            case BASK -> 20;
            default -> 8;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BiteGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.8, crocodile -> crocodile.isBaby()
                ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(3, new WarnGoal());
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new IdleActs(this, 220).add(BASK, 300, 800, 4,
                crocodile -> crocodile.level().isDay()).add(RISE, 60, 120, 2));
        this.goalSelector.addGoal(8, new Afloat.Paddle(this, 140, 1.0));
        this.goalSelector.addGoal(9, new Afloat.Seek(this, 400, 1.0));
        // In the water it stays a good while before it hauls out again.
        this.goalSelector.addGoal(10, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                this.setInterval(Crocodile.this.isInWater() ? 900 : 160);
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    // A player walking up in the open; one creeping past is let by.
    private boolean provokes(Entity entity) {
        return entity instanceof LivingEntity living && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)
                && !living.isDiscrete();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.oSwim = this.swim;
        this.swim = Mth.clamp(this.swim + (this.isInWater() ? 0.1F : -0.1F), 0.0F, 1.0F);
        if (!this.level().isClientSide) {
            Afloat.buoy(this, FLOAT);
        }
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.96F;
    }

    // Its jaws reach out well ahead of its body.
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(REACH, 0.0, REACH);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(BITES, 1.0F, 0.7F);
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
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.1F)
                : group);
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isBaby() ? 0 : 3 + this.random.nextInt(3);
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.15F, 0.7F);
    }

    private void hiss() {
        this.playSound(HISS, 1.0F, 0.45F + this.random.nextFloat() * 0.1F);
    }

    // Head up and jaws wide, hissing at a player who has come near. One who stays right by it is gone for; one who
    // keeps off is left be.
    private final class WarnGoal extends Goal {
        @Nullable
        private Player threat;
        private int nearSince;
        private int nextHiss;

        WarnGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Crocodile crocodile = Crocodile.this;
            if (crocodile.isBaby() || crocodile.getTarget() != null) {
                return false;
            }
            this.threat = crocodile.level().getNearestPlayer(crocodile.getX(), crocodile.getY(), crocodile.getZ(),
                    4.5, crocodile::provokes);
            return this.threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            Crocodile crocodile = Crocodile.this;
            return this.threat != null && this.threat.isAlive() && crocodile.provokes(this.threat)
                    && crocodile.distanceToSqr(this.threat) < 36.0 && crocodile.getTarget() == null
                    && crocodile.hurtTime == 0;
        }

        @Override
        public void start() {
            this.nearSince = -1;
            this.nextHiss = 0;
            Crocodile.this.getNavigation().stop();
            Crocodile.this.start(GAPE, 20 * 60);
        }

        @Override
        public void tick() {
            Crocodile crocodile = Crocodile.this;
            Player player = this.threat;
            if (player == null) {
                return;
            }
            crocodile.getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (crocodile.tickCount >= this.nextHiss) {
                crocodile.hiss();
                this.nextHiss = crocodile.tickCount + 44;
            }
            if (crocodile.distanceToSqr(player) > 7.5) {
                this.nearSince = -1;
            } else if (this.nearSince < 0) {
                this.nearSince = crocodile.tickCount;
            } else if (crocodile.tickCount - this.nearSince >= 30) {
                crocodile.setTarget(player);
            }
        }

        @Override
        public void stop() {
            Crocodile.this.end(GAPE);
            this.threat = null;
        }
    }

    // Goes for what it is set on, lunges and shuts its jaws on it; it lets a foe go that has got well away.
    private final class BiteGoal extends MeleeAttackGoal {
        private int swing;
        private int nextBite;

        BiteGoal() {
            super(Crocodile.this, 1.8, true);
        }

        @Override
        public boolean canUse() {
            return !Crocodile.this.isBaby() && super.canUse();
        }

        @Override
        public void tick() {
            Crocodile crocodile = Crocodile.this;
            LivingEntity target = crocodile.getTarget();
            if (this.swing > 0 && --this.swing == 0 && target != null && crocodile.isWithinMeleeAttackRange(target)) {
                crocodile.doHurtTarget(target);
            }
            if (target != null && crocodile.distanceToSqr(target) > 196.0) {
                crocodile.stopBeingAngry();
                return;
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && Crocodile.this.tickCount >= this.nextBite && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = BITE_HIT;
                this.nextBite = Crocodile.this.tickCount + BITE_EVERY;
                Crocodile.this.start(BITE, BITE_END);
            }
        }

        @Override
        public void stop() {
            this.swing = 0;
            super.stop();
        }
    }
}
