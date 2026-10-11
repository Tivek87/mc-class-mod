package nl.tivek.multiversepowers.creature.screamer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;

// The Screamer: a starved, twitching zombie in rags, its teeth clicking, whose jaw unhinges down to its chest. Its
// screech (ScreamerMoves) fills a cone in front of it: shields stop working, heads spin and legs go slow, and three
// ferals claw their way up out of the ground round it. Whatever comes within 2 blocks it throws back 5 with a burst of
// its voice.
public final class Screamer extends MovingMonster {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Screamer>> TYPE = TYPES.register("screamer",
            () -> EntityType.Builder.<Screamer>of(Screamer::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .eyeHeight(1.7F).clientTrackingRange(10).build("screamer"));
    // How long a fit of teeth clicking comes round (ticks) and how long it lasts.
    private static final int CHATTER_EVERY = 70;
    static final int CHATTER = 14;
    // Its burst only throws: nothing is harmed by the explosion itself.
    static final ExplosionDamageCalculator HARMLESS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }
    };

    public Screamer(EntityType<? extends Screamer> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        Feral.register(modEventBus);
        modEventBus.addListener(Screamer::onAttributes);
        modEventBus.addListener(Screamer::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 36.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return ScreamerMoves.KINDS;
    }

    // How far into a fit of teeth clicking it is (ticks), -1 when it is not in one: every game works it out alike.
    public static float chatter(Screamer screamer, float age) {
        if (screamer.move() != 0) {
            return -1.0F;
        }
        float at = age + screamer.getId() * 37.0F;
        int round = Mth.floor(at / CHATTER_EVERY);
        if ((round * 7 + screamer.getId()) % 3 == 0) {
            return -1.0F;
        }
        float into = at - round * CHATTER_EVERY;
        return into < CHATTER ? into : -1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            float into = chatter(this, this.tickCount);
            if (into >= 0.0F && (int) into % 2 == 0) {
                this.level().playLocalSound(this.getX(), this.getEyeY(), this.getZ(), SoundEvents.BONE_BLOCK_HIT,
                        SoundSource.HOSTILE, 0.35F, 1.7F + this.random.nextFloat() * 0.3F, false);
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ScreamerMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F + this.random.nextFloat() * 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_VILLAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.2F);
    }
}
