package nl.tivek.multiversepowers.character.greenlantern.minion;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.mech.MechAssembly;

// A helper the Hard-Light Mech drops out of the hatch under its cockpit: a robot of hard light twice a player's
// height, a little stronger than a zombie. It goes after what is out to hurt its pilot (red to them) with three moves
// of its own (MinionMoves) and else keeps near the mech; it breaks into solid pieces when it dies, when its pilot
// leaves the mech or when it strays too far. Never saved, so a world saved while it was out loads without it.
public final class MechMinion extends PathfinderMob {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<MechMinion>> TYPE = TYPES.register("mech_helper",
            () -> EntityType.Builder.<MechMinion>of(MechMinion::new, MobCategory.MISC).sized(1.2F, 3.4F)
                    .eyeHeight(3.1F).clientTrackingRange(10).noSave().noSummon().fireImmune().build("mech_helper"));
    // Its pilot's entity id, what it aims its moves at, and the game time it dropped out of the hatch.
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AIMED = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BORN = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    // It breaks into pieces over this many ticks once dead.
    public static final int BREAK = 16;
    // Further than this from its pilot it breaks up.
    private static final double LEASH = 64.0;
    // The entity events that tell every game a move starts (MinionMoves' kinds after FIRST_EVENT).
    static final byte FIRST_EVENT = 100;

    @Nullable
    private UUID owner;
    private float damage = 5.0F;
    // In each game: the move it makes now and the tick it began (by its own count).
    private int move;
    private int moveAt;

    public MechMinion(EntityType<? extends MechMinion> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(MechMinion::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.1).build());
    }

    // Made by the mech for its pilot: as strong as the mech's settings say.
    public static MechMinion of(ServerLevel level, ServerPlayer pilot, double health, double damage) {
        MechMinion minion = new MechMinion(TYPE.get(), level);
        minion.owner = pilot.getUUID();
        minion.damage = (float) damage;
        minion.entityData.set(OWNER, pilot.getId());
        minion.entityData.set(BORN, (int) level.getGameTime());
        AttributeInstance most = minion.getAttribute(Attributes.MAX_HEALTH);
        if (most != null) {
            most.setBaseValue(Math.max(1.0, health));
        }
        minion.setHealth(minion.getMaxHealth());
        return minion;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, -1);
        builder.define(AIMED, -1);
        builder.define(BORN, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MinionMoves(this));
        this.goalSelector.addGoal(2, new MinionGoals.Follow(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new MinionGoals.Foes(this));
    }

    @Nullable
    public ServerPlayer pilot() {
        return this.owner == null || !(this.level() instanceof ServerLevel level) ? null
                : level.getServer().getPlayerList().getPlayer(this.owner);
    }

    public boolean ownedBy(Entity entity) {
        return entity.getUUID().equals(this.owner);
    }

    // The pilot's entity id, in every game.
    public int pilotId() {
        return this.entityData.get(OWNER);
    }

    // What it aims its moves at, in every game (-1 nothing).
    public int aimed() {
        return this.entityData.get(AIMED);
    }

    void aim(@Nullable LivingEntity target) {
        this.entityData.set(AIMED, target == null ? -1 : target.getId());
    }

    // How many ticks ago it dropped out of the hatch.
    public double since(float partialTick) {
        return this.level().getGameTime() - (long) this.entityData.get(BORN) + partialTick;
    }

    float damage() {
        return this.damage;
    }

    // The move it makes now in this game (MinionMoves' kinds, 0 none) and how many ticks it is into it.
    public int move() {
        return this.move;
    }

    public double moveAge(float partialTick) {
        return this.tickCount - this.moveAt + partialTick;
    }

    // Starts a move here and in every game that sees it.
    void start(int kind) {
        this.move = kind;
        this.moveAt = this.tickCount;
        this.level().broadcastEntityEvent(this, (byte) (FIRST_EVENT + kind));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id > FIRST_EVENT && id <= FIRST_EVENT + MinionMoves.KINDS) {
            this.move = id - FIRST_EVENT;
            this.moveAt = this.tickCount;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }
        ServerPlayer pilot = this.pilot();
        if (pilot == null || !MechAssembly.piloting(pilot) || pilot.distanceToSqr(this) > LEASH * LEASH) {
            this.kill();
        }
    }

    // Its pilot's own blows pass it by.
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        Entity by = source.getEntity();
        return by != null && this.ownedBy(by) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    // Dead, it breaks into pieces where it stands (drawn by every game) and is gone.
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= BREAK && !this.level().isClientSide() && !this.isRemoved()) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.5F, 1.4F);
    }
}
