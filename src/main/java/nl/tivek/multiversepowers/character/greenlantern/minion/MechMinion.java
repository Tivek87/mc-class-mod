package nl.tivek.multiversepowers.character.greenlantern.minion;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.mech.MechAssembly;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A helper the Hard-Light Mech drops out of the hatch under its cockpit: a robot of hard light twice a player's
// height, a little stronger than a zombie. It goes after what is out to hurt its pilot (red to them) with three moves
// of its own (MinionMoves) and else keeps near the mech; it breaks into solid pieces when it dies, when its pilot
// leaves the mech or when it strays too far, and is never saved. One of no pilot (out of a spawn egg or summoned)
// guards where it is like an iron golem: it goes after monsters and whatever hurts it, and is saved with the world.
// Either cracks as it loses health (CRACKS), and an emerald mends one.
public final class MechMinion extends PathfinderMob {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<MechMinion>> TYPE = TYPES.register("mech_helper",
            () -> EntityType.Builder.<MechMinion>of(MechMinion::new, MobCategory.MISC).sized(1.2F, 3.4F)
                    .eyeHeight(3.1F).clientTrackingRange(10).fireImmune().build("mech_helper"));
    // Its pilot's entity id, what it aims its moves at, the game time it came, and how it came (HATCH or GROUND).
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AIMED = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BORN = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAME = SynchedEntityData.defineId(MechMinion.class,
            EntityDataSerializers.INT);
    public static final int GROUND = 0;
    public static final int HATCH = 1;
    // Below these parts of its health it shows its first, second and third crack.
    public static final float[] CRACKS = { 0.75F, 0.5F, 0.25F };
    private static final float MEND = 10.0F;
    private static final double WILD_HEALTH = 60.0;
    private static final float WILD_DAMAGE = 7.0F;
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
    // Loaded with its world: it does not come up out of the ground again.
    private boolean loaded;
    // In each game: how far off the ground it is, 0 standing to 1 in the air, eased, and the tick it last landed.
    private float air;
    private float airWas;
    private int landedAt = -1000;

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
        minion.entityData.set(CAME, HATCH);
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
        builder.define(BORN, Integer.MIN_VALUE);
        builder.define(CAME, GROUND);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MinionMoves(this));
        this.goalSelector.addGoal(2, new MinionGoals.Follow(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new MinionGoals.Foes(this));
    }

    // With no pilot it guards where it is.
    public boolean wild() {
        return this.owner == null;
    }

    // How it came: out of the mech's hatch, or up out of the ground (a spawn egg, a summon).
    public int came() {
        return this.entityData.get(CAME);
    }

    // How cracked it is: 0 whole, up to 3 nearly broken.
    public int cracks() {
        float part = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        int cracks = 0;
        for (float below : CRACKS) {
            if (part < below) {
                cracks++;
            }
        }
        return cracks;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        int was = this.cracks();
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.isAlive() && this.cracks() > was && this.level() instanceof ServerLevel level) {
            this.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.0F, 1.3F);
            this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.4F);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), this.position().add(0.0, 2.2, 0.0), 14,
                    0.5, 0.15);
        }
        return hurt;
    }

    // An emerald mends it a little, as iron mends an iron golem.
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.EMERALD) || this.getHealth() >= this.getMaxHealth()) {
            return super.mobInteract(player, hand);
        }
        float was = this.getHealth();
        this.heal(MEND);
        if (this.getHealth() == was) {
            return InteractionResult.PASS;
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.3F);
        held.consume(1, player);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // A wild one comes up out of the ground the moment it is first in a world (not when its world loads it again),
    // sturdier than a mech's.
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level().isClientSide() || this.loaded || this.entityData.get(BORN) != Integer.MIN_VALUE) {
            return;
        }
        this.entityData.set(BORN, (int) this.level().getGameTime());
        this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
        AttributeInstance most = this.getAttribute(Attributes.MAX_HEALTH);
        if (most != null && this.wild()) {
            most.setBaseValue(WILD_HEALTH);
            this.setHealth(this.getMaxHealth());
            this.damage = WILD_DAMAGE;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Punch", this.damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // /summon reads a tag too, one without what a saved one holds: that one still comes up out of the ground.
        this.loaded = tag.contains("Punch");
        if (this.loaded) {
            this.damage = tag.getFloat("Punch");
        }
    }

    // Only one of no pilot is saved with the world.
    @Override
    public boolean shouldBeSaved() {
        return this.wild() && super.shouldBeSaved();
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

    // How many ticks ago it came (a large number for one loaded with its world).
    public double since(float partialTick) {
        int born = this.entityData.get(BORN);
        return born == Integer.MIN_VALUE ? 1.0E6 : this.level().getGameTime() - (long) born + partialTick;
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

    // A client's onGround is only what the last move packet said: it looks for a block under its feet itself.
    private void feel() {
        this.airWas = this.air;
        boolean standing = !this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.2, 0.0));
        if (standing && this.air > 0.5F) {
            this.landedAt = this.tickCount;
        }
        this.air += ((standing ? 0.0F : 1.0F) - this.air) * (standing ? 0.6F : 0.25F);
    }

    public double air(float partialTick) {
        return Mth.lerp(partialTick, this.airWas, this.air);
    }

    // How many ticks ago it last came down on its feet in this game.
    public double landed(float partialTick) {
        return this.tickCount - this.landedAt + partialTick;
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
        if (this.level().isClientSide()) {
            this.feel();
            return;
        }
        if (!this.isAlive()) {
            return;
        }
        if (this.wild()) {
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
