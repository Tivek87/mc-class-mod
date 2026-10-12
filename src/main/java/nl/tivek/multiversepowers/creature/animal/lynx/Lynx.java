package nl.tivek.multiversepowers.creature.animal.lynx;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
import nl.tivek.multiversepowers.creature.animal.NeutralAnimal;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The lynx of the taiga: a long-legged cat with a bobbed, black-tipped tail, black tufts on its ears and a ruff round
// its face, tawny and spotted, or pale grey in its winter coat where it snows. It lives alone and keeps well away
// from players (not one who creeps up or holds out its food). Hit, it flattens its ears, hisses and claws back. At
// rest it sits, washes a paw, stretches with its front down and its rump up, and lies with its head raised.
public final class Lynx extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Lynx>> TYPE = TYPES.register("lynx",
            () -> EntityType.Builder.<Lynx>of(Lynx::new, MobCategory.CREATURE).sized(0.8F, 0.9F).eyeHeight(0.8F)
                    .clientTrackingRange(10).build("lynx"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "lynx_food"));
    public static final int TAWNY = 0;
    public static final int PALE = 1;
    public static final int SIT = 1;
    public static final int GROOM = 2;
    public static final int STRETCH = 3;
    public static final int LIE = 4;
    public static final int HISS = 5;
    public static final int SWIPE = 6;
    public static final int STRETCH_END = 44;
    public static final int HISS_END = 22;
    public static final int SWIPE_END = 12;
    // When the paw of a swipe lands.
    public static final int SWIPE_HIT = 5;
    private static final SoundEvent AMBIENT = Sounds.of("lynx.ambient");
    private static final SoundEvent HISSES = Sounds.of("lynx.hiss");
    private static final SoundEvent HURT = Sounds.of("lynx.hurt");
    private static final SoundEvent DEATH = Sounds.of("lynx.death");
    private static final SoundEvent SWIPES = Sounds.of("lynx.swipe");

    public Lynx(EntityType<? extends Lynx> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Lynx::onAttributes);
        modEventBus.addListener(Lynx::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 16.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildAnimal::checkWildSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return SWIPE;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case SIT, GROOM -> 10;
            case LIE -> 16;
            case SWIPE -> 2;
            default -> 6;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClawGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8,
                lynx -> lynx.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7.0F, 1.2, 1.7, this::shiesFrom));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), true));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 160).add(SIT, 100, 240, 3).add(GROOM, 60, 120, 2)
                .add(STRETCH, STRETCH_END, STRETCH_END, 1).add(LIE, 200, 500, 2));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(SWIPES, 0.8F, 1.1F);
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
        Lynx kitten = TYPE.get().create(level);
        if (kitten != null) {
            kitten.setVariant(other instanceof Lynx mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return kitten;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        this.setVariant(level.getBiome(this.blockPosition()).is(Tags.Biomes.IS_SNOWY) ? PALE : TAWNY);
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.15F)
                : group);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DEATH;
    }

    // A lynx's voice is deeper than an ocelot's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }

    // A cat walks without a sound.
    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
    }

    // Turns on whoever hit it: first, from a little way off, ears flat and hissing; then in with a paw.
    private final class ClawGoal extends MeleeAttackGoal {
        private int warning;
        private int swing;

        ClawGoal() {
            super(Lynx.this, 1.5, true);
        }

        @Override
        public boolean canUse() {
            return !Lynx.this.isBaby() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            this.warning = HISS_END;
            Lynx.this.getNavigation().stop();
            Lynx.this.start(HISS, HISS_END);
            Lynx.this.playSound(HISSES, 1.0F, 0.8F);
        }

        @Override
        public void tick() {
            LivingEntity target = Lynx.this.getTarget();
            if (this.warning > 0) {
                this.warning--;
                if (target != null) {
                    Lynx.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                return;
            }
            if (this.swing > 0 && --this.swing == 0 && target != null && Lynx.this.isWithinMeleeAttackRange(target)) {
                Lynx.this.doHurtTarget(target);
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = SWIPE_HIT;
                Lynx.this.start(SWIPE, SWIPE_END);
            }
        }

        @Override
        public void stop() {
            this.warning = 0;
            this.swing = 0;
            Lynx.this.end(HISS);
            super.stop();
        }
    }
}
