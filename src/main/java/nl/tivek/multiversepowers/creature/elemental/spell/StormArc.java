package nl.tivek.multiversepowers.creature.elemental.spell;

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
import nl.tivek.multiversepowers.creature.elemental.BlazeCasts;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import org.joml.Vector3f;

// Chain Lightning: a bolt from the storm rod (or the staff) to its foe, leaping on from each one struck to the nearest
// other foe within reach, up to three times, a little weaker each leap; whoever it strikes is left weakened for five
// seconds. With no one in its way it cracks into the ground where it was aimed. Drawn by client/StormArcRenderer, a
// leap every other tick.
public final class StormArc extends Entity {
    public static final int MOST = 4;
    public static final int STEP = 2;
    public static final int LIFE = 16;
    private static final double JUMP = 6.0;
    private static final double WEAKER = 0.75;
    private static final int WEAKENED = 100;
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(StormArc.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> MISS = SynchedEntityData.defineId(StormArc.class,
            EntityDataSerializers.VECTOR3);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] STRUCK = new EntityDataAccessor[MOST];

    static {
        for (int i = 0; i < MOST; i++) {
            STRUCK[i] = SynchedEntityData.defineId(StormArc.class, EntityDataSerializers.INT);
        }
    }

    private float damage;

    public StormArc(EntityType<? extends StormArc> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void strike(ServerLevel level, LivingEntity caster, @Nullable LivingEntity first, Vec3 miss,
            float damage) {
        List<LivingEntity> chain = new ArrayList<>();
        if (first != null) {
            chain.add(first);
            LivingEntity last = first;
            while (chain.size() < MOST) {
                LivingEntity next = null;
                double best = JUMP * JUMP;
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, last.getBoundingBox()
                        .inflate(JUMP), living -> BlazeCasts.seeks(caster, living))) {
                    double far = living.distanceToSqr(last);
                    if (!chain.contains(living) && far < best && living.hasLineOfSight(last)) {
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
        }
        StormArc arc = new StormArc(ElementalBlaze.STORM_ARC.get(), level);
        Vec3 at = first != null ? first.position() : miss;
        arc.setPos(at.x, at.y, at.z);
        arc.damage = damage;
        arc.entityData.set(CASTER, caster.getId());
        arc.entityData.set(MISS, miss.toVector3f());
        for (int i = 0; i < MOST; i++) {
            arc.entityData.set(STRUCK[i], i < chain.size() ? chain.get(i).getId() : -1);
        }
        level.addFreshEntity(arc);
        Vec3 from = BlazeCasts.tip(caster, 1.0F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.6F,
                1.5F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.7F,
                1.6F);
        if (first == null) {
            ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, miss.x, miss.y, miss.z, 20, 0.3, 0.2, 0.3, 0.3);
            ParticleFx.send(level, ParticleTypes.SMOKE, miss.x, miss.y, miss.z, 6, 0.2, 0.1, 0.2, 0.02);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER, -1);
        builder.define(MISS, new Vector3f());
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

    // Where the bolt cracks into the ground when it struck no one.
    public Vec3 miss() {
        return new Vec3(this.entityData.get(MISS));
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
        Entity before = i > 0 ? this.struck(i - 1) : caster;
        Vec3 from = before == null ? this.position() : before.getBoundingBox().getCenter();
        if (living.hurt(BlazeCasts.source(this, caster, Element.STORM, from), harm)) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKENED, 0), caster);
        }
        Vec3 at = living.getBoundingBox().getCenter();
        ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 24, 0.3, 0.5, 0.3, 0.3);
        ParticleFx.send(level, ParticleFx.dust(Element.STORM.light, 1.2F), at.x, at.y, at.z, 10, 0.3, 0.5, 0.3, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.4F,
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
