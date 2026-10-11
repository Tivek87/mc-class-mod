package nl.tivek.multiversepowers.creature.bloater;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Bloater: a huge, swollen zombie dripping green bile, its trunk and back covered in boils that wobble and pulse.
// From afar it spits balls of acid slime (BloaterMoves); close by, or after a few spits, it shrieks, runs at what it
// hunts swelling and hissing, and bursts 3 seconds later, leaving a pool of poison. A blade that strikes it pops a boil,
// and the pus splashes back on whoever swung it.
public final class Bloater extends MovingMonster {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Bloater>> TYPE = TYPES.register("bloater",
            () -> EntityType.Builder.<Bloater>of(Bloater::new, MobCategory.MONSTER).sized(1.4F, 2.2F)
                    .eyeHeight(1.85F).clientTrackingRange(10).build("bloater"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloaterSpit>> SPIT = TYPES.register(
            "bloater_spit", () -> EntityType.Builder.<BloaterSpit>of(BloaterSpit::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F).clientTrackingRange(6).updateInterval(5).build("bloater_spit"));
    public static final int BOILS = 10;
    public static final int ACID = 0x7FCC33;
    // Where each boil sits on its trunk, in its model's pixels from its hips (x to its left, y down, z back), and how
    // big it is: on its belly and chest, its sides and its back.
    public static final float[][] BOIL_AT = { { -4.0F, -7.0F, -7.4F, 4.0F }, { 3.0F, -4.0F, -7.3F, 3.0F },
            { 5.0F, -9.5F, -7.2F, 4.0F }, { -2.0F, -2.5F, -7.1F, 3.0F }, { -4.0F, -14.5F, -5.3F, 3.0F },
            { 3.0F, -15.5F, -5.3F, 4.0F }, { 8.2F, -6.0F, -1.0F, 4.0F }, { -8.2F, -8.0F, 2.0F, 3.0F },
            { -3.0F, -9.0F, 6.2F, 4.0F }, { 4.0F, -14.0F, 5.2F, 3.0F } };
    // How high its hips stand, in blocks.
    private static final double HIPS = 11.0 / 16.0;
    private static final EntityDataAccessor<Integer> INTACT = SynchedEntityData.defineId(Bloater.class,
            EntityDataSerializers.INT);
    private static final double BURST = 5.0;
    // Nothing is harmed by the burst's own explosion: it only shakes and throws, the burst's harm is the Bloater's own.
    private static final ExplosionDamageCalculator HARMLESS = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }
    };

    private boolean burst;

    public Bloater(EntityType<? extends Bloater> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Bloater::onAttributes);
        modEventBus.addListener(Bloater::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 46.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 0.6).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return BloaterMoves.KINDS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INTACT, (1 << BOILS) - 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Boils", this.entityData.get(INTACT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Boils")) {
            this.entityData.set(INTACT, tag.getInt("Boils") & (1 << BOILS) - 1);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BloaterMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    // Whether boil `i` is still whole.
    public boolean boil(int i) {
        return (this.entityData.get(INTACT) >> i & 1) != 0;
    }

    // Where boil `i` sits in the world now.
    private Vec3 boilAt(int i) {
        double yaw = Math.toRadians(this.yBodyRot);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        float[] at = BOIL_AT[i];
        double right = -at[0] / 16.0;
        double ahead = -at[2] / 16.0;
        return this.position().add(-right * cos - ahead * sin, HIPS - at[1] / 16.0, -right * sin + ahead * cos);
    }

    // A blade striking it pops one of its boils, which splashes the one who swung it.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel level && source.getDirectEntity() instanceof LivingEntity hitter
                && source.getDirectEntity() == source.getEntity() && blade(hitter.getMainHandItem())) {
            this.pop(level, hitter);
        }
        return hurt;
    }

    private static boolean blade(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem;
    }

    private void pop(ServerLevel level, LivingEntity hitter) {
        int intact = this.entityData.get(INTACT);
        if (intact == 0) {
            return;
        }
        // The whole boil nearest the blow.
        int nearest = -1;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < BOILS; i++) {
            if ((intact >> i & 1) != 0) {
                double d = this.boilAt(i).distanceToSqr(hitter.getEyePosition());
                if (d < best) {
                    best = d;
                    nearest = i;
                }
            }
        }
        if (nearest < 0) {
            return;
        }
        this.entityData.set(INTACT, intact & ~(1 << nearest));
        Vec3 at = this.boilAt(nearest);
        Vec3 way = hitter.getEyePosition().subtract(at).normalize();
        ParticleFx.send(level, ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, 14, 0.15, 0.15, 0.15, 0.25);
        for (int k = 0; k < 10; k++) {
            ParticleFx.fly(level, ParticleFx.dust(ACID, 1.2F), at, way.add(ParticleFx.spread(0.35),
                    ParticleFx.spread(0.35), ParticleFx.spread(0.35)), 0.6);
        }
        level.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_SQUISH, this.getSoundSource(), 1.0F, 0.6F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, this.getSoundSource(),
                1.2F, 0.7F);
        if (hitter.distanceToSqr(this) < 25.0 && hitter.hurt(MonsterBlows.source(this, MonsterBlows.ACID), 2.0F)) {
            hitter.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), this);
        }
    }

    // It bursts where it stands: a blast of acid slime round it, everything near struck and thrown, the nearest
    // hardest, and a pool of poison left on the ground; nothing of it is left.
    void explode() {
        if (this.burst || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.burst = true;
        Vec3 at = this.position().add(0.0, 1.0, 0.0);
        level.explode(this, null, HARMLESS, at.x, at.y, at.z, 3.0F, false, Level.ExplosionInteraction.NONE);
        ParticleFx.at(level, ParticleTypes.EXPLOSION_EMITTER, at);
        ParticleFx.sphereOut(level, ParticleTypes.ITEM_SLIME, at, 70, 0.7);
        ParticleFx.sphereOut(level, ParticleFx.dust(ACID, 2.0F), at, 60, 0.5);
        ParticleFx.send(level, ParticleTypes.SNEEZE, at.x, at.y, at.z, 50, 1.6, 0.8, 1.6, 0.08);
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()),
                at.x, at.y, at.z, 40, 0.8, 0.8, 0.8, 0.3);
        level.playSound(null, this.blockPosition(), SoundEvents.SLIME_DEATH, this.getSoundSource(), 2.0F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.HONEY_BLOCK_BREAK, this.getSoundSource(), 2.0F, 0.6F);
        DamageSource blast = this.damageSources().explosion(this, this);
        for (LivingEntity living : MonsterBlows.foes(this, new AABB(at, at).inflate(BURST))) {
            Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(at);
            double far = to.length();
            if (far > BURST + living.getBbWidth() * 0.5) {
                continue;
            }
            double near = 1.0 - Mth.clamp(far / BURST, 0.0, 1.0);
            Vec3 out = to.horizontalDistanceSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(to.x, 0.0, to.z).normalize();
            if (MonsterBlows.hurt(living, blast, 4.0 + 14.0 * near, out.scale(0.4 + 1.4 * near).add(0.0, 0.35 + 0.4
                    * near, 0.0))) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this);
            }
        }
        this.pool(level, 3.5F, 120);
        this.discard();
    }

    // A pool of poison on the ground where it stands, lasting `ticks`: it slows and poisons whoever steps in.
    private void pool(ServerLevel level, float radius, int ticks) {
        AreaEffectCloud pool = new AreaEffectCloud(level, this.getX(), this.getY() + 0.1, this.getZ());
        pool.setOwner(this);
        pool.setRadius(radius);
        pool.setRadiusOnUse(0.0F);
        pool.setRadiusPerTick(-radius * 0.25F / ticks);
        pool.setDuration(ticks);
        pool.setWaitTime(0);
        pool.setPotionContents(new PotionContents(Optional.empty(), Optional.of(ACID), List.of(
                new MobEffectInstance(MobEffects.POISON, 60, 0), new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        40, 1))));
        level.addFreshEntity(pool);
    }

    // Killed, its boils burst: a smaller pool, no blast.
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.burst && this.level() instanceof ServerLevel level) {
            Vec3 at = this.position().add(0.0, 1.0, 0.0);
            ParticleFx.sphereOut(level, ParticleTypes.ITEM_SLIME, at, 30, 0.35);
            ParticleFx.send(level, ParticleTypes.SNEEZE, at.x, at.y, at.z, 20, 0.8, 0.5, 0.8, 0.05);
            level.playSound(null, this.blockPosition(), SoundEvents.SLIME_DEATH, this.getSoundSource(), 1.4F, 0.6F);
            this.pool(level, 2.0F, 60);
        }
    }

    // Spits a ball of acid slime from its mouth at `target`, arcing so it comes down on it.
    void spit(LivingEntity target) {
        Vec3 ahead = MonsterBlows.ahead(this);
        Vec3 mouth = this.position().add(ahead.scale(0.75)).add(0.0, this.getEyeHeight() - 0.35, 0.0);
        BloaterSpit spit = new BloaterSpit(this.level(), this, (float) MonsterBlows.damage(this));
        spit.setPos(mouth);
        Vec3 aim = target.getEyePosition().subtract(0.0, 0.4, 0.0).subtract(mouth);
        double flat = aim.horizontalDistance();
        spit.shoot(aim.x, aim.y + flat * 0.16, aim.z, 1.05F, 2.5F);
        this.level().addFreshEntity(spit);
        if (this.level() instanceof ServerLevel level) {
            ParticleFx.send(level, ParticleTypes.ITEM_SLIME, mouth.x, mouth.y, mouth.z, 8, 0.1, 0.1, 0.1, 0.12);
        }
        this.playSound(SoundEvents.LLAMA_SPIT, 1.2F, 0.55F);
        this.playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.7F);
    }

    // Hissing from its swollen boils.
    void hiss(ServerLevel level) {
        int i = this.random.nextInt(BOILS);
        if (!this.boil(i)) {
            return;
        }
        Vec3 at = this.boilAt(i);
        ParticleFx.send(level, ParticleTypes.SMOKE, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.03);
        ParticleFx.send(level, ParticleFx.dust(ACID, 0.8F), at.x, at.y, at.z, 3, 0.08, 0.08, 0.08, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
            return;
        }
        // Bile drips from it.
        if (this.tickCount % 9 == 0) {
            Vec3 at = this.boilAt(this.random.nextInt(BOILS));
            ParticleFx.send(level, ParticleTypes.ITEM_SLIME, at.x, at.y - 0.1, at.z, 1, 0.05, 0.0, 0.05, 0.0);
        }
        // A rush its moves let go of (it fell in water) still ends in the burst.
        if (this.move() == BloaterMoves.RUSH && this.moveAge(0.0F) > BloaterMoves.BLOW + 5) {
            this.explode();
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.35F, 0.6F);
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.4F, 0.7F);
    }
}
