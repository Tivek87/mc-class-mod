package nl.tivek.multiversepowers.creature.screamer;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import nl.tivek.multiversepowers.creature.lurker.Crawler;
import nl.tivek.multiversepowers.creature.lurker.LurkerMoves;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A feral: a small, starved zombie that runs on all fours, quick and weak, slashing by turns with both hands. The
// Screamer's screech calls them up out of the ground round it (rise): for a second and a half they claw their way out,
// throwing up the ground, and can do nothing else. Drawn as a small Lurker (LurkerModel) in a skin of its own.
public final class Feral extends MovingMonster implements Crawler {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Feral>> TYPE = TYPES.register("feral",
            () -> EntityType.Builder.<Feral>of(Feral::new, MobCategory.MONSTER).sized(0.7F, 0.95F)
                    .eyeHeight(0.75F).clientTrackingRange(8).build("feral"));
    private static final EntityDataAccessor<Boolean> RISING = SynchedEntityData.defineId(Feral.class,
            EntityDataSerializers.BOOLEAN);
    public static final int RISE_END = 30;

    public Feral(EntityType<? extends Feral> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Feral::onAttributes);
        modEventBus.addListener(Feral::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // Calls one up out of the ground at `at`, hunting `target`.
    static void call(ServerLevel level, Vec3 at, float yaw, LivingEntity target) {
        Feral feral = TYPE.get().create(level);
        if (feral == null) {
            return;
        }
        feral.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        feral.yBodyRot = yaw;
        feral.yHeadRot = yaw;
        feral.entityData.set(RISING, true);
        feral.setTarget(target);
        level.addFreshEntity(feral);
        feral.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.6F);
    }

    @Override
    protected int kinds() {
        return LurkerMoves.KINDS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RISING, false);
    }

    @Override
    public boolean stalking() {
        return false;
    }

    // Counted from when this game first had it, as it rises from the moment it is made.
    @Override
    public float risen(float partialTick) {
        return this.entityData.get(RISING) ? Mth.clamp((this.tickCount + partialTick) / RISE_END, 0.0F, 1.0F) : 1.0F;
    }

    private boolean rising() {
        return this.entityData.get(RISING);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.rising();
    }

    @Override
    public boolean isPushable() {
        return !this.rising() && super.isPushable();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.rising() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        BlockState ground = level.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.2, this.getZ()));
        if (!ground.isAir() && this.tickCount % 3 == 0) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY()
                    + 0.1, this.getZ(), 6, 0.3, 0.05, 0.3, 0.15);
            if (this.tickCount % 9 == 0) {
                this.playSound(ground.getSoundType(level, this.blockPosition(), this).getBreakSound(), 0.6F, 0.7F);
            }
        }
        if (this.tickCount >= RISE_END) {
            this.entityData.set(RISING, false);
            this.playSound(SoundEvents.ZOMBIE_AMBIENT, 1.0F, 1.6F);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Slash(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + this.random.nextFloat() * 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.2F, 1.4F);
    }

    // It runs at what it hunts and slashes with one hand and the other by turns.
    private static final class Slash extends Goal {
        private static final double REACH = 1.2;
        private final Feral feral;
        private int kind;
        private int t;
        private int ready;
        private int repath;
        private boolean right;

        Slash(Feral feral) {
            this.feral = feral;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.feral.getTarget();
            return target != null && target.isAlive() && !this.feral.rising();
        }

        @Override
        public boolean canContinueToUse() {
            return this.kind != 0 || this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.feral.setAggressive(true);
        }

        @Override
        public void stop() {
            if (this.kind != 0) {
                this.feral.start(0);
            }
            this.kind = 0;
            this.feral.setAggressive(false);
            this.feral.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.feral.getTarget();
            if (target != null) {
                this.feral.getLookControl().setLookAt(target, 40.0F, 40.0F);
            }
            if (this.kind != 0) {
                this.t++;
                if (this.t == LurkerMoves.CLAW_HIT && target != null && MonsterBlows.gap(this.feral, target)
                        <= REACH + 0.6) {
                    Vec3 push = MonsterBlows.ahead(this.feral).scale(0.25).add(0.0, 0.15, 0.0);
                    MonsterBlows.hurt(target, this.feral.damageSources().mobAttack(this.feral), MonsterBlows.damage(
                            this.feral), push);
                    this.feral.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.8F);
                }
                if (this.t >= LurkerMoves.CLAW_END) {
                    this.kind = 0;
                    this.ready = this.feral.tickCount + 4;
                    this.feral.start(0);
                }
                return;
            }
            if (target == null) {
                return;
            }
            double far = MonsterBlows.gap(this.feral, target);
            if (far <= REACH && this.feral.tickCount >= this.ready) {
                this.right = !this.right;
                this.kind = this.right ? LurkerMoves.CLAW_RIGHT : LurkerMoves.CLAW_LEFT;
                this.t = 0;
                this.feral.getNavigation().stop();
                this.feral.start(this.kind);
            } else if (--this.repath <= 0) {
                this.repath = 4;
                this.feral.getNavigation().moveTo(target, 1.3);
            }
        }
    }
}
