package nl.tivek.multiversepowers.creature.wizard.spell;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import org.joml.Vector3f;

// The arcane beam, an ultimate of his third phase: braced, both hands on his staff, he lets go a torrent of violet light
// from the crystal that burns along wherever he looks. It starts as a thin aiming line (WARN), then roars out full; he
// turns after his foe only slowly (WizardCasts), so it sweeps after them and can be outrun sideways or ducked behind a
// wall, which stops it. Whoever it crosses is burned every quarter second. Drawn by client/fx/BeamRenderer from his
// drawn crystal to its end.
public final class ArcaneBeam extends Entity {
    public static final int WARN = 8;
    private static final double REACH = 32.0;
    private static final double WIDTH = 0.7;
    private static final int BURN = 5;
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(ArcaneBeam.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(ArcaneBeam.class,
            EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(ArcaneBeam.class,
            EntityDataSerializers.INT);
    private float damage;

    public ArcaneBeam(EntityType<? extends ArcaneBeam> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void fire(ServerLevel level, Wizard wizard, float damage, int life) {
        ArcaneBeam beam = new ArcaneBeam(WizardEntities.BEAM.get(), level);
        Vec3 from = wizard.staffTip();
        beam.setPos(from.x, from.y, from.z);
        beam.damage = damage;
        beam.entityData.set(CASTER, wizard.getId());
        beam.entityData.set(LIFE, life);
        beam.aim(level, wizard);
        level.addFreshEntity(beam);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.4F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER, -1);
        builder.define(END, new Vector3f());
        builder.define(LIFE, 40);
    }

    @Nullable
    public Entity caster() {
        return this.level().getEntity(this.entityData.get(CASTER));
    }

    public Vec3 end() {
        Vector3f end = this.entityData.get(END);
        return new Vec3(end.x, end.y, end.z);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    // How strong it burns now: a thin line while it warns, full, then dying out at its end.
    public float strength(float partialTick) {
        float age = this.tickCount + partialTick;
        if (age < WARN) {
            return 0.12F;
        }
        return Math.min(1.0F, (age - WARN) / 3.0F) * Math.min(1.0F, (this.life() - age) / 6.0F);
    }

    // From his crystal along where he looks, stopped by the first block.
    private void aim(ServerLevel level, Wizard wizard) {
        Vec3 from = wizard.staffTip();
        Vec3 to = from.add(wizard.getViewVector(1.0F).scale(REACH));
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, wizard));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        this.setPos(from.x, from.y, from.z);
        this.entityData.set(END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(this.caster() instanceof Wizard wizard) || !wizard.isAlive() || wizard.move() != WizardMove.BEAM
                || this.tickCount >= this.life()) {
            this.discard();
            return;
        }
        this.aim(level, wizard);
        Vec3 from = this.position();
        Vec3 end = this.end();
        if (this.tickCount >= WARN && this.tickCount % BURN == 0) {
            this.burn(wizard, from, end);
        }
        if (this.tickCount >= WARN) {
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 2.0F), end.x, end.y, end.z, 6, 0.3, 0.3, 0.3, 0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, end.x, end.y, end.z, 3, 0.1, 0.1, 0.1, 0.2);
            if (this.tickCount % 3 == 0) {
                ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, end.x, end.y, end.z, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (this.tickCount == WARN) {
            level.playSound(null, from.x, from.y, from.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.6F,
                    1.3F);
        }
        if (this.tickCount > WARN && this.tickCount % 10 == 0) {
            level.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 1.8F);
        }
    }

    // Burns whoever stands across it.
    private void burn(Wizard wizard, Vec3 from, Vec3 end) {
        Vec3 line = end.subtract(from);
        double length = line.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 way = line.scale(1.0 / length);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(from, end).inflate(WIDTH + 1.0))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double along = Math.max(0.0, Math.min(length, middle.subtract(from).dot(way)));
            double off = middle.distanceTo(from.add(way.scale(along)));
            if (off < WIDTH + living.getBbWidth() * 0.5 + living.getBbHeight() * 0.25) {
                if (living.hurt(Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage)) {
                    living.push(way.x * 0.15, 0.05, way.z * 0.15);
                }
            }
        }
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
        return distance < 160.0 * 160.0;
    }
}
