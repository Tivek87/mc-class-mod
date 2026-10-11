package nl.tivek.multiversepowers.creature.wizard;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;
import org.joml.Vector3f;

// The Wizard: an old mage in a long robe and a pointed hat, a staff crowned with a glowing crystal in his right hand. A
// boss with a bar of his own (client/WizardBar), only ever brought into the world by his egg, fought in three phases:
// the Archmage on his feet; at two thirds of his health he rises into the air (ASCEND), the Ascended, floating from then
// on; at a third the magic breaks loose round him (UNBIND), the Unbound. Each phase he knows stronger spells and casts
// harder (WizardMove, WizardMoves). While a phase takes him he cannot be harmed; caught out among his mirror images he
// is stunned and takes half again.
public final class Wizard extends MovingMonster {
    // His colours: the arcane violet, the fire, the frost, the stars' white-blue, gravity's deep blue, time's gold
    // and the lightning's white-blue.
    public static final int VIOLET = 0xB46CFF;
    public static final int FIRE = 0xFF8A2A;
    public static final int ICE = 0xA8E6FF;
    public static final int STARLIGHT = 0xE6F0FF;
    public static final int GRAVITY = 0x6E6BFF;
    public static final int TIME = 0xFFD27A;
    public static final int STORM = 0x9FDCFF;
    // The light of each phase: violet, a cold blue, a burning gold.
    private static final int[] PHASE_LIGHT = { VIOLET, VIOLET, 0x62C6FF, 0xFFB23C };
    // How far above the ground he floats from his second phase.
    public static final double HOVER = 0.8;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Wizard.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> BAR = SynchedEntityData.defineId(Wizard.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final ResourceLocation PHASE_SPEED = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "wizard_phase_speed");
    private static final EntityDimensions FLOATING = EntityDimensions.scalable(0.6F, 1.95F + (float) HOVER)
            .withEyeHeight(1.62F + (float) HOVER);

    private final ServerBossEvent bar = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    // Set in registerGoals, which runs before this class's own fields are set.
    private WizardMoves moves;
    // Where his staff's crystal and his left hand are in the world, as this game last drew him.
    @Nullable
    private Vec3 crystal;
    @Nullable
    private Vec3 leftHand;
    // Until when time runs fast for him (field/TimeWarp).
    private int hastedUntil;

    public Wizard(EntityType<? extends Wizard> type, Level level) {
        super(type, level);
        this.xpReward = 400;
        this.entityData.set(BAR, Optional.of(this.bar.getId()));
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        WizardEntities.register(modEventBus);
    }

    // The harm of one of his spells, struck by `by` (the spell itself or him) on his behalf.
    public static DamageSource source(Entity by, @Nullable Entity owner, ResourceKey<DamageType> type) {
        return new DamageSource(by.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), by, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(BAR, Optional.empty());
    }

    @Override
    protected int kinds() {
        return WizardMove.KINDS;
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.moves = new WizardMoves(this);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, this.moves);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // His phase, 1 to 3.
    public int phase() {
        return this.entityData.get(PHASE);
    }

    void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(PHASE_SPEED);
            if (phase > 1) {
                speed.addPermanentModifier(new AttributeModifier(PHASE_SPEED, 0.15 * (phase - 1),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        this.bar.setColor(phase == 1 ? BossEvent.BossBarColor.PURPLE : phase == 2 ? BossEvent.BossBarColor.BLUE
                : BossEvent.BossBarColor.YELLOW);
        this.refreshDimensions();
    }

    // The light of his phase: his crystal, his runes and his bar.
    public static int light(int phase) {
        return PHASE_LIGHT[Mth.clamp(phase, 1, 3)];
    }

    @Nullable
    public UUID barId() {
        return this.entityData.get(BAR).orElse(null);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.phase() > 1 ? FLOATING : super.getDefaultDimensions(pose);
    }

    // How high above the ground he floats now.
    public double hover() {
        return this.phase() > 1 ? HOVER : 0.0;
    }

    // Changing phase (or stunned): no harm reaches him while a phase takes him.
    public boolean shielded() {
        return this.move() == WizardMove.ASCEND || this.move() == WizardMove.UNBIND;
    }

    boolean hasted() {
        return this.tickCount < this.hastedUntil;
    }

    public void haste(int ticks) {
        this.hastedUntil = Math.max(this.hastedUntil, this.tickCount + ticks);
    }

    // How hard a move strikes now: his attack times its share, harder each phase.
    public float harm(int kind) {
        return (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * WizardMove.harm(kind)
                * WizardMove.power(this.phase()));
    }

    // A point on his body: to his right, up from his feet and ahead of him, in blocks.
    public Vec3 at(double right, double up, double ahead) {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        return this.position().add(-cos * right - sin * ahead, up + this.hover(), -sin * right + cos * ahead);
    }

    // Where his spells leave from: the crystal of the staff in his right hand, and his left hand.
    public Vec3 staffTip() {
        return this.at(0.45, 2.25, 0.35);
    }

    public Vec3 palm() {
        return this.at(-0.45, 1.35, 0.55);
    }

    // Where his crystal was last drawn (where it should be, before he is first drawn).
    public Vec3 crystal() {
        return this.crystal == null ? this.staffTip() : this.crystal;
    }

    public void drawnAt(Vec3 crystal, Vec3 leftHand) {
        this.crystal = crystal;
        this.leftHand = leftHand;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            WizardSparks.of(this, this.phase(), this.crystal == null ? this.staffTip() : this.crystal,
                    this.leftHand == null ? this.palm() : this.leftHand);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        // His own spells, decoys and familiars never harm him, nor do walls he is moved through among his mirrors.
        if (by == this || by instanceof WizardDecoy || by instanceof ArcaneFamiliar
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (this.shielded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!this.level().isClientSide && this.tickCount % 4 == 0) {
                this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.6F);
            }
            return false;
        }
        if (this.moves != null && !this.level().isClientSide) {
            this.moves.struck(by);
        }
        float taken = this.move() == WizardMove.STUNNED ? amount * 1.5F : amount;
        boolean hit = super.hurt(source, taken);
        if (hit && this.isAlive() && this.moves != null && this.phase() < 3) {
            float edge = this.getMaxHealth() * (this.phase() == 1 ? 2.0F / 3.0F : 1.0F / 3.0F);
            if (this.getHealth() <= edge) {
                // A phase is never skipped: the blow that breaks it stops at its edge.
                this.setHealth(edge);
                this.moves.phaseUp();
            }
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bar.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", this.phase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) {
            this.setPhase(Mth.clamp(tag.getInt("Phase"), 1, 3));
        }
        if (this.hasCustomName()) {
            this.bar.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + this.random.nextFloat() * 0.1F;
    }

    // Dust in one of his colours.
    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F,
                (rgb & 255) / 255.0F), size);
    }
}
