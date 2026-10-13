package nl.tivek.multiversepowers.creature.elemental.spell;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import org.joml.Vector3f;

// Stone Heave: a block of the ground itself that the earth rod tears out of the world (BlazeCasts.heave): it grinds
// slowly up out of its hole, shaking and shedding dirt, then swings up until it hangs before its caster; then it is
// hurled, falling a little as it flies, and bursts on whatever it strikes, throwing it back. While it rises every game
// works out the same path from its hole and its caster, so it moves smoothly; drawn by client/HeavedStoneRenderer as
// its block.
public final class HeavedStone extends ThrowableProjectile {
    // How far to the side of a player theirs rises and hangs, clear of what they look at.
    public static final double BESIDE = 1.3;
    private static final double SPEED = 1.25;
    // The share of its lift spent working out of its hole, and how far over the hole's floor it is then.
    private static final double OUT = 0.45;
    private static final double CLEAR = 1.1;
    private static final EntityDataAccessor<BlockState> ROCK = SynchedEntityData.defineId(HeavedStone.class,
            EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Vector3f> GROUND = SynchedEntityData.defineId(HeavedStone.class,
            EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> LIFT = SynchedEntityData.defineId(HeavedStone.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOLDER = SynchedEntityData.defineId(HeavedStone.class,
            EntityDataSerializers.INT);

    private float damage = 7.0F;

    public HeavedStone(EntityType<? extends HeavedStone> type, Level level) {
        super(type, level);
    }

    public static void raise(ServerLevel level, LivingEntity caster, Vec3 ground, BlockState rock, int lift) {
        HeavedStone stone = new HeavedStone(ElementalBlaze.HEAVED_STONE.get(), level);
        stone.setOwner(caster);
        stone.setPos(ground);
        stone.damage = BlazeCasts.damage(caster, Element.EARTH);
        stone.entityData.set(ROCK, rock);
        stone.entityData.set(GROUND, ground.toVector3f());
        stone.entityData.set(LIFT, lift);
        stone.entityData.set(HOLDER, caster.getId());
        level.addFreshEntity(stone);
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, rock), ground.x, ground.y + 0.6, ground.z,
                24, 0.5, 0.2, 0.5, 0.15);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, ground.x, ground.y + 0.6, ground.z, 2, 0.3, 0.1, 0.3,
                0.01);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROCK, Blocks.COBBLESTONE.defaultBlockState());
        builder.define(GROUND, new Vector3f());
        builder.define(LIFT, 0);
        builder.define(HOLDER, -1);
    }

    public BlockState rock() {
        return this.entityData.get(ROCK);
    }

    // Whether it still rises in its caster's grip.
    public boolean rising() {
        return this.tickCount < this.entityData.get(LIFT);
    }

    @Nullable
    private Entity holder() {
        return this.level().getEntity(this.entityData.get(HOLDER));
    }

    // Where it hangs before its caster, ready to be thrown.
    private static Vec3 hold(Entity holder) {
        if (holder instanceof ElementalBlaze blaze) {
            return blaze.position().add(MonsterBlows.ahead(blaze).scale(1.2)).add(0.0, 2.2, 0.0);
        }
        Vec3 ahead = holder.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        return holder.getEyePosition().add(ahead.scale(1.8)).add(BlazeCasts.side(holder, ahead).scale(BESIDE))
                .add(0.0, 0.2, 0.0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        Entity holder = this.holder();
        int lift = this.entityData.get(LIFT);
        if (this.tickCount < lift && holder != null) {
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            this.tickCount++;
            Vec3 ground = new Vec3(this.entityData.get(GROUND));
            double s = Math.min(1.0, this.tickCount / (double) lift);
            Vec3 up;
            if (s < OUT) {
                // Slow at first, then faster, as a block wrenched loose; it shakes as it grinds against the hole.
                double p = s / OUT;
                double shake = 0.04 * (1.0 - p);
                up = ground.add(Math.sin(this.tickCount * 2.7) * shake, CLEAR * p * p,
                        Math.cos(this.tickCount * 3.1) * shake);
            } else {
                double p = (s - OUT) / (1.0 - OUT);
                double eased = p * p * (3.0 - 2.0 * p);
                up = ground.add(0.0, CLEAR, 0.0).lerp(hold(holder), eased).add(0.0, Math.sin(Math.PI * p) * 0.4,
                        0.0);
            }
            this.setPos(up);
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide) {
                this.crumble();
            } else if (this.tickCount == lift) {
                this.hurl(holder);
            }
            return;
        }
        if (this.tickCount < lift) {
            this.entityData.set(LIFT, 0);
        }
        super.tick();
        if (this.level().isClientSide && this.random.nextInt(2) == 0) {
            Vec3 at = this.position();
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, this.rock()), at.x, at.y
                    + 0.4, at.z, 0.0, 0.0, 0.0);
        }
        if (!this.level().isClientSide && this.tickCount > 200) {
            this.discard();
        }
    }

    private void hurl(Entity holder) {
        Vec3 from = this.position();
        Vec3 way = holder instanceof LivingEntity living ? BlazeCasts.aim(living, from, SPEED) : Vec3.ZERO;
        double far = holder instanceof LivingEntity living && living instanceof ElementalBlaze blaze
                && blaze.getTarget() != null ? blaze.getTarget().distanceTo(this) : 12.0;
        // Aimed a little high, for the fall on the way.
        this.shoot(way.x, way.y + far * 0.012, way.z, (float) SPEED, 0.5F);
        this.level().playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                1.4F, 0.5F);
        this.level().playSound(null, from.x, from.y, from.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    // Dirt and pebbles falling off it as it rises.
    private void crumble() {
        Vec3 at = this.position();
        for (int i = 0; i < 2; i++) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, this.rock()),
                    at.x + (this.random.nextDouble() - 0.5) * 0.7, at.y + this.random.nextDouble() * 0.7,
                    at.z + (this.random.nextDouble() - 0.5) * 0.7, 0.0, 0.0, 0.0);
        }
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, this.rock()), at.x, at.y + 0.3, at.z,
                    (this.random.nextDouble() - 0.5) * 0.2, 0.1, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return !this.rising() && super.canHitEntity(target) && BlazeCasts.hits(this.getOwner(), target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity living) {
            Vec3 way = this.getDeltaMovement().multiply(1.0, 0.0, 1.0);
            Vec3 push = (way.lengthSqr() < 1.0E-6 ? Vec3.ZERO : way.normalize().scale(0.9)).add(0.0, 0.35, 0.0);
            MonsterBlows.hurt(living, BlazeCasts.source(this, this.getOwner(), Element.EARTH), this.damage, push);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = result.getLocation();
            BlockParticleOption bits = new BlockParticleOption(ParticleTypes.BLOCK, this.rock());
            ParticleFx.send(level, bits, at.x, at.y + 0.2, at.z, 40, 0.4, 0.4, 0.4, 0.2);
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, this.rock()), at.x, at.y + 0.3,
                    at.z, 12, 0.6, 0.4, 0.6, 0.0);
            ParticleFx.send(level, ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 6, 0.3, 0.2, 0.3, 0.03);
            BlockPos pos = BlockPos.containing(at);
            level.playSound(null, pos, this.rock().getSoundType(level, pos, this).getBreakSound(),
                    SoundSource.HOSTILE, 1.6F, 0.6F);
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.5F, 1.5F);
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.put("Rock", NbtUtils.writeBlockState(this.rock()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        if (tag.contains("Rock")) {
            this.entityData.set(ROCK, NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK),
                    tag.getCompound("Rock")));
        }
    }
}
