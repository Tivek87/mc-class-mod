package nl.tivek.multiversepowers.creature.wizard.spell;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// Chain lightning: a crackling arc leaps from the Wizard's crystal to his foe, and on from them to the next foe near
// them, and the next, each jump a little weaker, every one struck jolted and slowed a moment. Drawn as forking white-blue
// bolts (client/fx/ArcRenderer) between him and each in turn, a jump every other tick.
public final class ArcLightning extends Entity {
    public static final int MOST = 5;
    public static final int STEP = 2;
    public static final int LIFE = 16;
    private static final double JUMP = 6.5;
    private static final double WEAKER = 0.75;
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(ArcLightning.class,
            EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] STRUCK = new EntityDataAccessor[MOST];

    static {
        for (int i = 0; i < MOST; i++) {
            STRUCK[i] = SynchedEntityData.defineId(ArcLightning.class, EntityDataSerializers.INT);
        }
    }

    private float damage;

    public ArcLightning(EntityType<? extends ArcLightning> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // Strikes `target` and up to `jumps` more foes near each one before.
    public static void strike(ServerLevel level, Wizard wizard, LivingEntity target, float damage, int jumps) {
        strikeFrom(level, wizard, wizard, target, damage, jumps);
    }

    // As strike, the first arc leaping from `from` (his staff fighting on its own).
    public static void strikeFrom(ServerLevel level, Wizard wizard, Entity from, LivingEntity target, float damage,
            int jumps) {
        List<LivingEntity> chain = new ArrayList<>();
        chain.add(target);
        LivingEntity last = target;
        while (chain.size() <= Math.min(jumps, MOST - 1)) {
            LivingEntity next = null;
            double best = JUMP * JUMP;
            for (LivingEntity living : MonsterBlows.foes(wizard, last.getBoundingBox().inflate(JUMP))) {
                double far = living.distanceToSqr(last);
                if (!chain.contains(living) && far < best) {
                    best = far;
                    next = living;
                }
            }
            if (next == null) {
                break;
            }
            chain.add(next);
            last = next;
        }
        ArcLightning arc = new ArcLightning(WizardEntities.ARC.get(), level);
        arc.setPos(target.getX(), target.getY(), target.getZ());
        arc.damage = damage;
        arc.entityData.set(CASTER, from.getId());
        for (int i = 0; i < MOST; i++) {
            arc.entityData.set(STRUCK[i], i < chain.size() ? chain.get(i).getId() : -1);
        }
        level.addFreshEntity(arc);
        Vec3 at = from == wizard ? wizard.staffTip() : from.position();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.6F, 1.6F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER, -1);
        for (EntityDataAccessor<Integer> struck : STRUCK) {
            builder.define(struck, -1);
        }
    }

    @Nullable
    public Entity caster() {
        return this.level().getEntity(this.entityData.get(CASTER));
    }

    // The `i`th struck, or null.
    @Nullable
    public Entity struck(int i) {
        int id = this.entityData.get(STRUCK[i]);
        return id < 0 ? null : this.level().getEntity(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        if (this.tickCount % STEP != 0) {
            return;
        }
        int i = this.tickCount / STEP - 1;
        if (i < 0 || i >= MOST || !(this.struck(i) instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        Entity caster = this.caster();
        float harm = (float) (this.damage * Math.pow(WEAKER, i));
        if (living.hurt(Wizard.source(this, caster, WizardEntities.LIGHTNING), harm)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2), caster);
        }
        Vec3 at = living.getBoundingBox().getCenter();
        ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.3);
        ParticleFx.send(level, Wizard.dust(Wizard.STORM, 1.2F), at.x, at.y, at.z, 10, 0.3, 0.5, 0.3, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.5F,
                1.8F + i * 0.1F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
