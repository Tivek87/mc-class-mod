package nl.tivek.multiversepowers.creature.wizard.abyss;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.field.FieldSpell;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The soul drain, his fourth phase: threads of the void's light reach from his open hand to up to three foes, and
// their life runs along them into him, healing him (never past where his phase began). A thread breaks when its foe
// gets out of his sight, or far enough away. Drawn by client/abyss/DrainRenderer.
public final class SoulDrain extends FieldSpell {
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(SoulDrain.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HELD = SynchedEntityData.defineId(SoulDrain.class,
            EntityDataSerializers.INT);
    private static final double REACH = 18.0;
    private static final int EVERY = 6;
    // Ticks out of his sight before the thread breaks.
    private static final int BLIND = 8;
    private static final int MOST = 3;
    private int life;
    private int blind;

    public SoulDrain(EntityType<? extends SoulDrain> type, Level level) {
        super(type, level);
    }

    // Threads to his nearest foes (his target first), lasting `life` ticks.
    public static void reach(ServerLevel level, Wizard wizard, @Nullable LivingEntity target, float damage,
            int life) {
        List<LivingEntity> foes = MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(REACH));
        foes.removeIf(living -> living == target || !Wizard.prey(living) || !wizard.hasLineOfSight(living));
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(wizard), b.distanceToSqr(wizard)));
        if (target != null && target.isAlive()) {
            foes.add(0, target);
        }
        for (int k = 0; k < Math.min(MOST, foes.size()); k++) {
            LivingEntity living = foes.get(k);
            SoulDrain drain = new SoulDrain(WizardEntities.DRAIN.get(), level);
            drain.setPos(living.getX(), living.getY(), living.getZ());
            drain.castBy(wizard, damage);
            drain.life = life;
            drain.entityData.set(CASTER, wizard.getId());
            drain.entityData.set(HELD, living.getId());
            level.addFreshEntity(drain);
        }
        level.playSound(null, wizard.getX(), wizard.getY() + 1.5, wizard.getZ(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    @Nullable
    public Entity caster() {
        return this.level().getEntity(this.entityData.get(CASTER));
    }

    @Nullable
    public Entity held() {
        return this.level().getEntity(this.entityData.get(HELD));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER, -1);
        builder.define(HELD, -1);
    }

    @Override
    public void tick() {
        super.tick();
        Entity held = this.held();
        if (held != null) {
            this.setPos(held.getX(), held.getY(), held.getZ());
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || !(held instanceof LivingEntity living) || !living.isAlive() || this.tickCount > this.life
                || living.distanceTo(wizard) > REACH) {
            this.discard();
            return;
        }
        Vec3 from = wizard.palm();
        Vec3 to = living.getBoundingBox().getCenter();
        boolean sees = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, wizard)).getType() == HitResult.Type.MISS;
        this.blind = sees ? 0 : this.blind + 1;
        if (this.blind > BLIND) {
            ParticleFx.send(level, ParticleTypes.SOUL, to.x, to.y, to.z, 8, 0.2, 0.3, 0.2, 0.02);
            this.discard();
            return;
        }
        if (sees && this.tickCount % EVERY == 0
                && living.hurt(Wizard.source(this, wizard, WizardEntities.VOID), this.damage)) {
            float top = wizard.phaseTop();
            if (wizard.getHealth() < top) {
                wizard.setHealth(Math.min(top, wizard.getHealth() + this.damage * 0.8F));
            }
            ParticleFx.line(level, ParticleTypes.SOUL, to, from, 0.8);
            if (this.tickCount % (EVERY * 3) == 0) {
                level.playSound(null, to.x, to.y, to.z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2F,
                        0.7F);
            }
        }
    }
}
