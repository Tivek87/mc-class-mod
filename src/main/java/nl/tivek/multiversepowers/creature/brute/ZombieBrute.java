package nl.tivek.multiversepowers.creature.brute;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

// The Zombie Brute: a zombie a head taller than a player, more muscled, faster and harder to stop. Up close it
// smashes both fists down on what it hunts; further off it leaps and slams the ground where it lands, or roars and
// charges (BruteMoves). It burns in daylight like any zombie and comes out at night, rarely. Its numbers are its kind's
// line on the Mobs page.
public final class ZombieBrute extends Monster {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ZombieBrute>> TYPE = TYPES.register("zombie_brute",
            () -> EntityType.Builder.<ZombieBrute>of(ZombieBrute::new, MobCategory.MONSTER).sized(1.2F, 2.45F)
                    .eyeHeight(2.1F).clientTrackingRange(10).build("zombie_brute"));
    // The entity events that tell every game a move starts (BruteMoves' kinds after FIRST_EVENT).
    static final byte FIRST_EVENT = 100;

    // In each game: the move it makes now and the tick it began (by its own count).
    private int move;
    private int moveAt;

    public ZombieBrute(EntityType<? extends ZombieBrute> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(ZombieBrute::onAttributes);
        modEventBus.addListener(ZombieBrute::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 35.0).add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BruteMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // The move it makes now in this game (BruteMoves' kinds, 0 none) and how many ticks it is into it.
    public int move() {
        return this.move;
    }

    public double moveAge(float partialTick) {
        return this.tickCount - this.moveAt + partialTick;
    }

    // Starts a move here and in every game that sees it (0 ends it).
    void start(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
        this.level().broadcastEntityEvent(this, (byte) (FIRST_EVENT + kind));
    }

    // Shows a move in this game alone, as if the server had started it (one shown in a screen).
    public void play(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= FIRST_EVENT && id <= FIRST_EVENT + BruteMoves.KINDS) {
            this.move = id - FIRST_EVENT;
            this.moveAt = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    // Out in daylight with nothing on its head it catches fire, as a zombie does.
    @Override
    public void aiStep() {
        if (this.isAlive() && this.isSunBurnTick() && this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.igniteForSeconds(8.0F);
        }
        super.aiStep();
    }

    // Too heavy to be hurt by a short drop: it lands its own leaps.
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return super.causeFallDamage(distance - 4.0F, multiplier, source);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
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
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.4F, 0.6F);
        this.playSound(SoundEvents.RAVAGER_STEP, 0.35F, 1.1F);
    }
}
