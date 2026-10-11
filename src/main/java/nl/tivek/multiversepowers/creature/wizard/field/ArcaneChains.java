package nl.tivek.multiversepowers.creature.wizard.field;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Arcane chains: a circle of runes lights up on the ground where his foe stands; a moment later (ERUPT) four chains of
// violet light burst out of it and fasten on whoever is still inside, holding them where they are (no walking, no
// jumping away) for two seconds or more, then shatter. Step out of the circle in time and they lash at nothing. Drawn
// by client/fx/ChainsRenderer.
public final class ArcaneChains extends FieldSpell {
    public static final int ERUPT = 14;
    public static final double RADIUS = 2.0;
    public static final int MOST = 3;
    private static final int MISSED = 12;
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] BOUND = new EntityDataAccessor[MOST];
    private static final EntityDataAccessor<Integer> HOLD = SynchedEntityData.defineId(ArcaneChains.class,
            EntityDataSerializers.INT);

    static {
        for (int i = 0; i < MOST; i++) {
            BOUND[i] = SynchedEntityData.defineId(ArcaneChains.class, EntityDataSerializers.INT);
        }
    }

    public ArcaneChains(EntityType<? extends ArcaneChains> type, Level level) {
        super(type, level);
    }

    public static void cast(ServerLevel level, Wizard wizard, Vec3 ground, float damage, int hold) {
        ArcaneChains chains = new ArcaneChains(WizardEntities.CHAINS.get(), level);
        chains.setPos(ground.x, ground.y, ground.z);
        chains.castBy(wizard, damage);
        chains.entityData.set(HOLD, hold);
        level.addFreshEntity(chains);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE,
                1.4F, 0.8F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        for (EntityDataAccessor<Integer> bound : BOUND) {
            builder.define(bound, -1);
        }
        builder.define(HOLD, 40);
    }

    public int hold() {
        return this.entityData.get(HOLD);
    }

    // The `i`th it holds, or null.
    @Nullable
    public Entity bound(int i) {
        int id = this.entityData.get(BOUND[i]);
        return id < 0 ? null : this.level().getEntity(id);
    }

    public boolean holdsAny() {
        for (EntityDataAccessor<Integer> bound : BOUND) {
            if (this.entityData.get(bound) >= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null) {
            this.shatter(level);
            return;
        }
        if (this.tickCount == ERUPT) {
            this.erupt(level, wizard);
        }
        if (this.tickCount > ERUPT) {
            this.hold(level);
        }
        int end = ERUPT + (this.holdsAny() ? this.hold() : MISSED);
        if (this.tickCount >= end) {
            this.shatter(level);
        }
    }

    private void erupt(ServerLevel level, Wizard wizard) {
        int count = 0;
        Vec3 middle = this.position();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(middle, middle).inflate(RADIUS, 2.5, RADIUS))) {
            double far = Math.hypot(living.getX() - middle.x, living.getZ() - middle.z);
            if (far > RADIUS + living.getBbWidth() * 0.5 || count >= MOST) {
                continue;
            }
            this.entityData.set(BOUND[count++], living.getId());
            living.hurt(Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.hold(), 7, false, false),
                    wizard);
        }
        ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.8F), middle.x, middle.y + 0.3, middle.z, 30, 1.0, 0.2,
                1.0, 0.0);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 1.6F);
    }

    // Whoever it holds is kept to the middle of the circle and on the ground.
    private void hold(ServerLevel level) {
        Vec3 middle = this.position();
        for (int i = 0; i < MOST; i++) {
            if (!(this.bound(i) instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            Vec3 off = living.position().subtract(middle);
            Vec3 flat = new Vec3(off.x, 0.0, off.z);
            boolean strayed = flat.length() > 0.6 || off.y > 0.5;
            if (strayed) {
                Vec3 back = flat.scale(-0.35).add(0.0, off.y > 0.5 ? -0.4 : 0.0, 0.0);
                living.setDeltaMovement(back);
                living.hurtMarked = true;
            }
            if (this.tickCount % 10 == 0) {
                Vec3 at = living.getBoundingBox().getCenter();
                ParticleFx.send(level, ParticleTypes.ENCHANT, at.x, at.y, at.z, 6, 0.3, 0.5, 0.3, 0.5);
            }
        }
    }

    private void shatter(ServerLevel level) {
        Vec3 at = this.position();
        if (this.tickCount > ERUPT) {
            ParticleFx.send(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHAIN)), at.x,
                    at.y + 1.0, at.z, 24, 0.8, 0.6, 0.8, 0.15);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.6F, 0.7F);
        }
        for (int i = 0; i < MOST; i++) {
            MobEffectInstance held = this.bound(i) instanceof LivingEntity living
                    ? living.getEffect(MobEffects.MOVEMENT_SLOWDOWN) : null;
            if (held != null && held.getAmplifier() == 7 && this.bound(i) instanceof LivingEntity living) {
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
        this.discard();
    }
}
